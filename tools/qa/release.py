#!/usr/bin/env python3
"""Release gate: run every QA gate in order, stamp RELEASE READY or NOT READY.

Gates:
  1. mvn test                    (full JUnit suite, must be green)
  2. artifact-smoke.py           (fat jar boots, skirmish completes, codex ok)
  3. browser-qa.py               (page markers + headless DOM, 0 console errors)
  4. playtest-bot.py             (full seeded campaign terminates)

Setup between gates 1 and 2: `mvn -q package -DskipTests` (tests already
ran in gate 1; the jar is what gate 2 boots).

Prints per-gate PASS/FAIL plus a final RELEASE READY / RELEASE NOT READY
stamp. Exits nonzero unless every gate passes. Never tags by itself.
Stdlib only.
"""

import glob
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))


def run(name, cmd):
    print("=" * 60, flush=True)
    print("gate: %s\n$ %s" % (name, " ".join(cmd)), flush=True)
    print("=" * 60, flush=True)
    # Windows: mvn is a .cmd shim, so go through the shell there.
    proc = subprocess.run(cmd, cwd=ROOT, shell=(os.name == "nt"))
    ok = proc.returncode == 0
    print("gate %s: %s (exit %d)" % (name, "PASS" if ok else "FAIL",
                                     proc.returncode), flush=True)
    return ok


def main():
    python = sys.executable
    results = []

    results.append(("mvn test", run("mvn test", ["mvn", "test"])))

    jar = None
    if results[-1][1]:
        ok = run("package (setup)", ["mvn", "-q", "package", "-DskipTests"])
        cands = sorted(glob.glob(os.path.join(ROOT, "target", "royan-*.jar")),
                       key=os.path.getmtime)
        cands = [c for c in cands if os.path.basename(c).startswith("royan-")
                 and "-shaded" not in c and "-sources" not in c
                 and "-javadoc" not in c]
        jar = cands[-1] if cands else None
        if not ok or not jar:
            print("package setup failed; artifact-smoke cannot run", flush=True)
            results.append(("artifact-smoke", False))
        else:
            results.append(("artifact-smoke",
                            run("artifact-smoke",
                                [python, os.path.join(HERE, "artifact-smoke.py"),
                                 "--jar", jar])))
    else:
        results.append(("artifact-smoke", False))

    results.append(("browser-qa",
                    run("browser-qa",
                        [python, os.path.join(HERE, "browser-qa.py")])))
    results.append(("playtest-bot",
                    run("playtest-bot",
                        [python, os.path.join(HERE, "playtest-bot.py"), "-q"])))

    print("=" * 60, flush=True)
    print("GATE SUMMARY", flush=True)
    for name, ok in results:
        print("  [%s] %s" % ("PASS" if ok else "FAIL", name), flush=True)
    print("=" * 60, flush=True)
    if all(ok for _, ok in results):
        print("RELEASE READY", flush=True)
        return 0
    print("RELEASE NOT READY", flush=True)
    return 1


if __name__ == "__main__":
    sys.exit(main())
