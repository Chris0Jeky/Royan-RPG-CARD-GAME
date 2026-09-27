#!/usr/bin/env python3
"""Browser QA gate: serve the web UI, check the page + headless DOM.

Floor (always): boots `Main serve` headless, fetches `/` over HTTP and
asserts static markers, checks /api/state serves the select screen.

Ceiling (when Edge/Chrome is found): runs it headless with --dump-dom
--virtual-time-budget=6000 --enable-logging=stderr and asserts the
JS-rendered select screen (Knight/Ranger/Runemage hero picks, codex,
tutorial, settings, how-to markers) plus ZERO console errors
(no ERROR:CONSOLE / Uncaught lines).

Without a browser the ceiling is reported as SKIP (exit 0 with an
honest SKIP note), never a fake pass.
Stdlib only.
"""

import argparse
import os
import shutil
import subprocess
import sys
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from qalib import (Api, QAFail, boot_server, die, java_exe, main_classpath,
                   stop_server, wait_for_state)

STATIC_MARKERS = [
    "Royan",            # <title>
    "select-screen",
    "btn-codex",
    "btn-settings",
    "btn-howto",
    "tutorial-screen",
]

# data-hero buttons only exist after app.js renders the select screen,
# so they prove the JS actually ran (not just static HTML served).
DOM_MARKERS = [
    'data-hero="KNIGHT"',
    'data-hero="RANGER"',
    'data-hero="RUNEMAGE"',
    "Knight",
    "Ranger",
    "Runemage",
    "codex",
    "tutorial",
    "settings",
    "howto",
]

ERROR_NEEDLES = ["ERROR:CONSOLE", "Uncaught"]


def find_browser():
    """Locate Edge/Chrome: PATH first, then well-known install paths."""
    for name in ("msedge", "chrome", "chromium", "google-chrome",
                 "google-chrome-stable"):
        found = shutil.which(name)
        if found:
            return found
    if os.name == "nt":
        programs = [os.environ.get("ProgramFiles", r"C:\Program Files"),
                    os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)"),
                    os.path.join(os.environ.get("LocalAppData", ""), "Programs")]
        rels = [os.path.join("Microsoft", "Edge", "Application", "msedge.exe"),
                os.path.join("Google", "Chrome", "Application", "chrome.exe")]
        for base in programs:
            for rel in rels:
                cand = os.path.join(base, rel)
                if os.path.isfile(cand):
                    return cand
        local = os.environ.get("LocalAppData", "")
        cand = os.path.join(local, "Google", "Chrome", "Application", "chrome.exe")
        if os.path.isfile(cand):
            return cand
    else:
        for cand in ("/usr/bin/google-chrome", "/usr/bin/chromium",
                     "/usr/bin/chromium-browser", "/snap/bin/chromium"):
            if os.path.isfile(cand):
                return cand
    return None


def check_floor(base, api):
    try:
        with urllib.request.urlopen(base + "/", timeout=15) as resp:
            html = resp.read().decode("utf-8")
            ctype = resp.headers.get("Content-Type", "")
    except Exception as err:
        raise QAFail("GET / failed: %s" % err)
    missing = [m for m in STATIC_MARKERS if m not in html]
    if missing:
        raise QAFail("GET / missing static markers: %s" % missing)
    print("floor: GET / ok (%d bytes, %s)" % (len(html), ctype), flush=True)

    snap = wait_for_state(api)
    if snap.get("phase") != "select":
        raise QAFail("expected select phase, got %r" % snap.get("phase"))
    print("floor: /api/state select ok", flush=True)


def check_ceiling(browser, base):
    cmd = [browser, "--headless", "--disable-gpu", "--no-first-run",
           "--dump-dom", "--virtual-time-budget=6000",
           "--enable-logging=stderr", base + "/"]
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    except subprocess.TimeoutExpired:
        raise QAFail("headless browser timed out after 120s")
    dom = proc.stdout or ""
    log = proc.stderr or ""
    missing = [m for m in DOM_MARKERS if m not in dom]
    if missing:
        raise QAFail("dump-dom missing select-screen markers: %s "
                     "(dom %d bytes)" % (missing, len(dom)))
    errors = [line.strip() for line in log.splitlines()
              if any(n in line for n in ERROR_NEEDLES)]
    if errors:
        shown = "\n  ".join(errors[:10])
        raise QAFail("console errors in headless render (%d):\n  %s"
                     % (len(errors), shown))
    print("ceiling: dump-dom ok (%d bytes, 0 console errors)" % len(dom),
          flush=True)


def main():
    args = argparse.ArgumentParser(description="browser QA gate")
    args.add_argument("--port", type=int, default=0, help="serve port (0 = free)")
    args.add_argument("--browser", default=None,
                      help="browser binary (default: auto-detect; "
                           "'none' forces the API floor only)")
    opts = args.parse_args()

    proc = None
    try:
        classpath = main_classpath()
        proc, base = boot_server(
            [java_exe(), "-Djava.awt.headless=true", "-cp", classpath,
             "com.chris.cardgame.Main", "serve", str(opts.port)])
        print("server: %s" % base, flush=True)
        api = Api(base)

        check_floor(base, api)

        if opts.browser == "none":
            browser = None
        elif opts.browser:
            browser = opts.browser
            if not os.path.isfile(browser):
                raise QAFail("browser not found: %s" % browser)
        else:
            browser = find_browser()
        if browser is None:
            print("SKIP: no Edge/Chrome found; API floor only "
                  "(ceiling not verified)", flush=True)
            print("PASS: browser QA (floor only, ceiling SKIPPED)")
            return 0
        print("browser: %s" % browser, flush=True)
        check_ceiling(browser, base)
        print("PASS: browser QA (floor + headless DOM, 0 console errors)")
        return 0
    except QAFail as err:
        return die(str(err))
    finally:
        if proc is not None:
            stop_server(proc)


if __name__ == "__main__":
    sys.exit(main())
