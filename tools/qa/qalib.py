"""Shared helpers for the Royan QA scripts (stdlib only).

Every script in tools/qa boots a headless game server as a subprocess and
drives it over HTTP with urllib. This module holds the boot/poll/API
logic so the four gates stay consistent.
"""

import json
import os
import queue
import shutil
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class QAFail(Exception):
    """A gate failure with a human-readable message (exit nonzero)."""


def java_exe():
    """Resolve the java launcher: JAVA_HOME first, then PATH."""
    home = os.environ.get("JAVA_HOME")
    if home:
        cand = os.path.join(home, "bin", "java.exe" if os.name == "nt" else "java")
        if os.path.isfile(cand):
            return cand
    found = shutil.which("java")
    if not found:
        raise QAFail("no java on PATH and JAVA_HOME unset/invalid")
    return found


def find_jackson_jars():
    """Locate jackson jars under ~/.m2 (databind + transitive deps)."""
    m2 = os.path.join(os.path.expanduser("~"), ".m2", "repository",
                      "com", "fasterxml", "jackson", "core")
    jars = []
    for group in ("jackson-databind", "jackson-core", "jackson-annotations"):
        gdir = os.path.join(m2, group)
        if not os.path.isdir(gdir):
            continue
        for version in sorted(os.listdir(gdir)):
            jar = os.path.join(gdir, version, "%s-%s.jar" % (group, version))
            if os.path.isfile(jar):
                jars.append(jar)
    return jars


def ensure_compiled():
    """Make sure target/classes exists, compiling if needed. Returns classes dir."""
    classes = os.path.join(ROOT, "target", "classes")
    if os.path.isfile(os.path.join(classes, "com", "chris", "cardgame", "Main.class")):
        return classes
    mvn = shutil.which("mvn")
    if not mvn:
        raise QAFail("target/classes missing and no mvn on PATH to build it")
    print("target/classes missing; running mvn -q compile ...", flush=True)
    proc = subprocess.run([mvn, "-q", "compile"], cwd=ROOT,
                          shell=(os.name == "nt"))
    if proc.returncode != 0:
        raise QAFail("mvn -q compile failed (exit %d)" % proc.returncode)
    return classes


def main_classpath():
    """Classpath for running com.chris.cardgame.Main from the repo tree."""
    classes = ensure_compiled()
    jars = find_jackson_jars()
    if not jars:
        raise QAFail("no jackson jars under ~/.m2; run mvn compile first")
    return classes + os.pathsep + os.pathsep.join(jars)


def boot_server(argv, timeout=30.0):
    """Start the server subprocess, parse its printed URL, return (proc, base).

    argv is the full command line ending in ['serve', '<port>'] with port 0.
    Reads stdout on a reader thread so a slow JVM start cannot hang us.
    """
    proc = subprocess.Popen(
        argv, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, bufsize=1)
    lines = queue.Queue()

    def reader():
        try:
            for line in proc.stdout:
                lines.put(line)
        finally:
            lines.put(None)

    thread = threading.Thread(target=reader, daemon=True)
    thread.start()
    deadline = time.time() + timeout
    seen = []
    try:
        while time.time() < deadline:
            if proc.poll() is not None:
                raise QAFail("server exited during boot (code %d):\n%s"
                             % (proc.returncode, "".join(seen[-20:])))
            try:
                line = lines.get(timeout=1.0)
            except queue.Empty:
                continue
            if line is None:
                break
            seen.append(line)
            marker = "http://localhost:"
            at = line.find(marker)
            if at >= 0:
                url = line[at:].split()[0].rstrip("/")
                # Localhost resolves to ::1 first on Windows while the game
                # server binds IPv4 127.0.0.1, so each new connection stalls
                # ~2s on fallback. Pin IPv4: same server, no per-hit stall.
                return proc, url.replace("http://localhost:", "http://127.0.0.1:")
        raise QAFail("server did not print its URL within %ds:\n%s"
                     % (timeout, "".join(seen[-20:])))
    except Exception:
        stop_server(proc)
        raise


def stop_server(proc):
    """Terminate a booted server; never raises."""
    try:
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=10)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait(timeout=10)
    except Exception:
        pass


class Api:
    """Minimal JSON client for the game API (urllib, stdlib only)."""

    def __init__(self, base):
        self.base = base

    def _read(self, req):
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                return resp.status, json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as err:
            try:
                body = json.loads(err.read().decode("utf-8"))
            except Exception:
                body = {"error": "HTTP %d (unparseable body)" % err.code}
            return err.code, body
        except (urllib.error.URLError, TimeoutError, ConnectionError, OSError) as err:
            raise QAFail("HTTP request failed: %s" % err)

    def get(self, path):
        return self._read(urllib.request.Request(self.base + path, method="GET"))

    def post(self, path, body=None):
        data = json.dumps(body or {}).encode("utf-8")
        req = urllib.request.Request(
            self.base + path, data=data, method="POST",
            headers={"Content-Type": "application/json"})
        return self._read(req)


def wait_for_state(api, timeout=30.0):
    """Poll /api/state until the server answers; return the snapshot."""
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        try:
            status, snap = api.get("/api/state")
        except QAFail as err:
            last = str(err)
            time.sleep(0.5)
            continue
        if status == 200:
            return snap
        last = "HTTP %d: %s" % (status, snap.get("error"))
        time.sleep(0.5)
    raise QAFail("server never served /api/state: %s" % last)


def first_alive(enemies):
    """Index of the first living foe, or -1 when the field is empty."""
    for foe in enemies:
        if foe.get("alive", True) and foe.get("hp", 1) > 0:
            return foe.get("index", 0)
    return -1


def battle_action(api, snap):
    """Play the first playable card, else end the turn. Returns new snapshot.

    Works for both skirmish (phase battle) and campaign (screen battle)
    snapshots: hand/enemies sit at top level in both.
    """
    hand = snap.get("hand", [])
    target = first_alive(snap.get("enemies", []))
    for card in hand:
        if card.get("playable"):
            aim = target if card.get("needsTarget") else -1
            status, snap2 = api.post("/api/play",
                                     {"hand": card["index"], "target": aim})
            if status == 200:
                return snap2, False
            # Defensive: a card the UI calls playable can still be
            # rejected (stale snapshot); bracing always works instead.
            break
    status, snap2 = api.post("/api/end-turn", {})
    if status != 200:
        raise QAFail("end-turn rejected: %s" % snap2.get("error"))
    return snap2, True


def in_battle(snap):
    return snap.get("phase") == "battle" or snap.get("screen") == "battle"


def is_over(snap):
    return snap.get("phase") == "over" or snap.get("over") is True


def die(message):
    print("FAIL: %s" % message, file=sys.stderr)
    return 1
