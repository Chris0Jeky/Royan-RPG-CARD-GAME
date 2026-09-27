#!/usr/bin/env python3
"""Artifact smoke gate: boot the packaged fat jar, finish a skirmish via API.

Builds nothing itself: pass --jar (default: newest target/royan-*.jar).
Boots `java -Djava.awt.headless=true -jar <jar> serve <port>` as a
subprocess, polls /api/state, drives new-battle -> play/end-turn until
the skirmish ends, checks /api/codex, kills the server.

Exit 0 on PASS, nonzero with a clear message on any failure.
Stdlib only.
"""

import argparse
import glob
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from qalib import (Api, QAFail, ROOT, battle_action, boot_server, die,
                   in_battle, is_over, java_exe, stop_server, wait_for_state)


def newest_jar():
    cands = sorted(glob.glob(os.path.join(ROOT, "target", "royan-*.jar")),
                   key=os.path.getmtime)
    # The shade plugin replaces the main artifact in place; ignore the
    # original- stub, the -shaded side copy, and -sources/-javadoc.
    cands = [c for c in cands if os.path.basename(c).startswith("royan-")
             and "-shaded" not in c and "-sources" not in c and "-javadoc" not in c]
    return cands[-1] if cands else None


def main():
    args = argparse.ArgumentParser(description="fat-jar artifact smoke test")
    args.add_argument("--jar", default=None, help="fat jar path")
    args.add_argument("--port", type=int, default=0, help="serve port (0 = free)")
    args.add_argument("--hero", default="KNIGHT")
    args.add_argument("--seed", type=int, default=7)
    args.add_argument("--turn-cap", type=int, default=200,
                      help="max end-turns before the battle is declared stuck")
    opts = args.parse_args()

    jar = opts.jar or newest_jar()
    if not jar or not os.path.isfile(jar):
        return die("no fat jar found (ran mvn package? tried target/royan-*.jar)")
    print("jar: %s" % jar, flush=True)

    proc = None
    try:
        proc, base = boot_server(
            [java_exe(), "-Djava.awt.headless=true", "-jar", jar,
             "serve", str(opts.port)])
        print("server: %s" % base, flush=True)
        api = Api(base)

        snap = wait_for_state(api)
        if snap.get("phase") != "select":
            raise QAFail("expected select screen, got phase=%r" % snap.get("phase"))
        heroes = [h.get("id") for h in snap.get("heroes", [])]
        for want in ("KNIGHT", "RANGER", "RUNEMAGE"):
            if want not in heroes:
                raise QAFail("select screen missing hero %s (got %s)" % (want, heroes))
        print("select: heroes %s" % ",".join(heroes), flush=True)

        status, codex = api.get("/api/codex")
        if status != 200:
            raise QAFail("/api/codex rejected: %s" % codex.get("error"))
        sizes = {k: len(codex.get(k, []))
                 for k in ("heroes", "enemies", "cards", "relics", "companions")}
        if sizes["heroes"] != 3 or sizes["cards"] < 90 or sizes["enemies"] < 12:
            raise QAFail("codex sizes look wrong: %s" % sizes)
        print("codex: %s" % sizes, flush=True)

        status, snap = api.post("/api/new-battle",
                                {"heroClass": opts.hero, "seed": opts.seed})
        if status != 200:
            raise QAFail("new-battle rejected: %s" % snap.get("error"))
        if not in_battle(snap):
            raise QAFail("new-battle did not enter battle (phase=%r)"
                         % snap.get("phase"))
        print("battle: %s seed %d, %d foe(s)"
              % (opts.hero, opts.seed, len(snap.get("enemies", []))), flush=True)

        braces = 0
        actions = 0
        while not is_over(snap):
            if not in_battle(snap):
                raise QAFail("skirmish left battle early (phase=%r)" % snap.get("phase"))
            snap, braced = battle_action(api, snap)
            actions += 1
            braces += 1 if braced else 0
            if braces > opts.turn_cap:
                raise QAFail("battle stuck: %d end-turns without a result" % braces)
            if actions > opts.turn_cap * 10:
                raise QAFail("battle stuck: %d actions without a result" % actions)

        result = "victory" if snap.get("victory") else "defeat"
        print("Result: %s in %d turns (%d actions)"
              % (result, snap.get("turn"), actions), flush=True)
        print("PASS: artifact smoke (%s, %s)" % (os.path.basename(jar), result))
        return 0
    except QAFail as err:
        return die(str(err))
    finally:
        if proc is not None:
            stop_server(proc)


if __name__ == "__main__":
    sys.exit(main())
