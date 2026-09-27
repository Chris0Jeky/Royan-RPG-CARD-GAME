#!/usr/bin/env python3
"""Playtest bot gate: drive a FULL seeded campaign to victory/defeat via API.

Boots `Main serve` headless, starts a campaign with /api/new-run, then
loops on the snapshot screen until the campaign ends:

  map     -> choose-node (first option)
  battle  -> play first playable card, else end-turn
  levelup -> choose-boon 0
  draft   -> choose-draft 0 (server handles a full deck)
  shop    -> shop-leave
  tavern  -> tavern-leave
  event   -> event-choose (first affordable choice)

Asserts the campaign TERMINATES with victory or defeat inside the action
cap (no hang, no exception). Prints a Result line either way.
Stdlib only.
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from qalib import (Api, QAFail, battle_action, boot_server, die, in_battle,
                   is_over, java_exe, main_classpath, stop_server,
                   wait_for_state)


def post_ok(api, path, body, what):
    status, snap = api.post(path, body)
    if status != 200:
        raise QAFail("%s rejected: %s" % (what, snap.get("error")))
    return snap


def step(api, snap, verbose):
    """Advance one campaign screen; return the new snapshot."""
    screen = snap.get("screen")
    if screen == "map":
        options = (snap.get("map") or {}).get("options", [])
        if not options:
            raise QAFail("map has no options (act %s)"
                         % (snap.get("run") or {}).get("act"))
        node = options[0]
        if verbose:
            nodes = {(n.get("id")): n.get("type")
                     for n in (snap.get("map") or {}).get("nodes", [])}
            print("  map: sail to %s [%s]" % (node, nodes.get(node)),
                  flush=True)
        return post_ok(api, "/api/choose-node", {"id": node},
                       "choose-node %s" % node)
    if screen == "battle":
        snap2, braced = battle_action(api, snap)
        if verbose and braced:
            print("  battle: brace (turn %s)" % snap.get("turn"), flush=True)
        return snap2
    if screen == "levelup":
        offer = (snap.get("levelup") or {}).get("offer", [])
        if not offer:
            raise QAFail("levelup with no offer")
        if verbose:
            print("  levelup: take %s" % offer[0].get("name"), flush=True)
        return post_ok(api, "/api/choose-boon", {"index": 0}, "choose-boon 0")
    if screen == "draft":
        options = (snap.get("draft") or {}).get("options", [])
        if not options:
            raise QAFail("draft with no options")
        if verbose:
            print("  draft: take %s" % options[0].get("name"), flush=True)
        return post_ok(api, "/api/choose-draft", {"index": 0}, "choose-draft 0")
    if screen == "shop":
        if verbose:
            print("  shop: leave", flush=True)
        return post_ok(api, "/api/shop-leave", {}, "shop-leave")
    if screen == "tavern":
        if verbose:
            print("  tavern: leave", flush=True)
        return post_ok(api, "/api/tavern-leave", {}, "tavern-leave")
    if screen == "event":
        choices = (snap.get("event") or {}).get("choices", [])
        pick = next((c["index"] for c in choices if c.get("affordable")), None)
        if pick is None:
            raise QAFail("event '%s' has no affordable choice"
                         % (snap.get("event") or {}).get("title"))
        if verbose:
            print("  event: choose %d (%s)"
                  % (pick, choices[pick].get("text", "")[:60]), flush=True)
        return post_ok(api, "/api/event-choose", {"index": pick},
                       "event-choose %d" % pick)
    raise QAFail("unknown screen %r (phase %r)" % (screen, snap.get("phase")))


def main():
    args = argparse.ArgumentParser(description="full-campaign playtest bot")
    args.add_argument("--port", type=int, default=0, help="serve port (0 = free)")
    args.add_argument("--hero", default="KNIGHT")
    args.add_argument("--seed", type=int, default=4,
                      help="pinned-victory seed for the CLI auto demo")
    args.add_argument("--cap", type=int, default=600,
                      help="max campaign actions before declaring a hang")
    args.add_argument("--battle-turn-cap", type=int, default=120,
                      help="max turns in one battle before declaring a stall")
    args.add_argument("-q", "--quiet", action="store_true")
    opts = args.parse_args()

    proc = None
    try:
        proc, base = boot_server(
            [java_exe(), "-Djava.awt.headless=true", "-cp", main_classpath(),
             "com.chris.cardgame.Main", "serve", str(opts.port)])
        print("server: %s" % base, flush=True)
        api = Api(base)
        wait_for_state(api)

        snap = post_ok(api, "/api/new-run",
                       {"heroClass": opts.hero, "seed": opts.seed},
                       "new-run %s seed %d" % (opts.hero, opts.seed))
        print("campaign: %s seed %d" % (opts.hero, opts.seed), flush=True)

        actions = 0
        battles = 0
        in_fight = False
        while not is_over(snap):
            if snap.get("screen") == "battle":
                if not in_fight:
                    battles += 1
                    in_fight = True
                if (snap.get("turn") or 0) > opts.battle_turn_cap:
                    raise QAFail("battle stall: turn %s in one fight"
                                 % snap.get("turn"))
            elif in_fight:
                in_fight = False
            snap = step(api, snap, verbose=not opts.quiet)
            actions += 1
            if actions >= opts.cap:
                raise QAFail("campaign did not terminate within %d actions "
                             "(screen=%r)" % (opts.cap, snap.get("screen")))

        run = snap.get("run") or {}
        result = "victory" if snap.get("victory") else "defeat"
        print("Result: %s, hero=%s, seed=%d, acts=%s, level=%s, deck=%s, "
              "gold=%s, battles=%d, actions=%d"
              % (result, opts.hero, opts.seed, run.get("act"),
                 run.get("level"), run.get("deckSize"), run.get("gold"),
                 battles, actions), flush=True)
        print("PASS: playtest bot (%s seed %d -> %s, %d actions)"
              % (opts.hero, opts.seed, result, actions))
        return 0
    except QAFail as err:
        return die(str(err))
    finally:
        if proc is not None:
            stop_server(proc)


if __name__ == "__main__":
    sys.exit(main())
