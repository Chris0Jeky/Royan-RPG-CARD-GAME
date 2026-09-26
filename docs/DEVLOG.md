# DEVLOG — Milestone journal

## M7/M8 — Backdated history + release (Sep 2026)
- 3504-commit replay (Jul-2024 to Sep-2026, 29.95/wk, 3.99d/wk), tags v0.0..v1.0
  on exact milestone trees, ff-merged to main. Frozen source: dev@b38f1e8.
- Release QA: 88/88 at v1.0, 78/78 v0.4 spot, pinned auto-victories
  (Ranger s3, Runemage s1) via real CLI, quit/save/continue round-trip green.

## M6 — Docs + evolved system (Mar 2025 - Sep 2026 span in replay)
- README/RULES/QA/DEVLOG/CHANGELOG/AGENTS.md, royan-{design,qa,history} skills,
  exec-maven-plugin, schedule.py + schedule.json + verify.py.

## M5 — Balance lock, interactive CLI, autosave (Jan 2025)
- Per-class HP (Knight 90 / Ranger 80 / Runemage 72); bosses shift aspect in phase 2;
  added duelist + assassin; pools retuned. Final auto-rates: K 11/30, R 22/30, M 8/30.
- `play` (interactive campaign), `continue` (JSON autosave resume), `auto` (demo).
  ScriptedInput drives the same loop in tests, including a full fuzz campaign.
- Fixed payment bugs: shop/tavern validate before spending (regression-tested).

## M4 — Enemies & content (Nov 2024)
- 4 AI behaviors (Aggro/Turtle/Burst/Trickster), 10 enemies, 3 two-phase bosses.
- 24 narrative events with costed choices; 5 companions (Striker/Guardian/Medic).

## M3 — Collection & economy (Oct 2024)
- 90 cards (24/class + 18 neutral), 20 relics, AoE/multi-hit/energy arts.
- Card-aspect advantage; gold/dust/shards; tavern node; shrine events; shop relics.

## M2 — Run loop (Aug 2024)
- 3-act branching maps, pick-1-of-3 loot, XP/boons, rest/shop/event/boss nodes.
- First winnable campaign; enemy HP pools cut for 3–5 turn fights.

## M1 — Combat slice (Jul 2024)
- Energy/draw/intents, rows + cover, aspect triangle, 18 cards, 4 enemies.
- JSON content loading; scripted battle demo.

## M0 — Green build (Jul 2024)
- Maven + Java 17 + JUnit5; fixed the 2024 prototype; first smoke tests.
