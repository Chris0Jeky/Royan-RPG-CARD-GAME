# Changelog

## Unreleased
- Web UI (W1): `serve` mode with embedded server + browser combat screen
  (hero select, click-to-play cards, intents, combat log); CLI unchanged.
- Web UI (W2): full campaign in browser — act map, loot drafts, shop, tavern,
  events, level boons, suspend/continue; quick-skirmish mode kept.
- Web UI (W3): feel pass — 34 game-icons portraits (CC-BY), damage numbers,
  hit flashes, screen shake, turn banners, synthesized SFX + ambient music.
- Web UI (W4): RPG depth — hero backstories, companion banter, 2 event chains
  (28 events), run chronicle/saga, bestiary + deck codex.

## Unreleased (history)
- History tooling: `replay.py` (frozen-source micro-commit replay), `verify.py`
  `--base` scoping + merge-base tag checks, corrected history-skill runbook.
- `main` carries the full backdated history (3504 commits, Jul-2024 to Sep-2026,
  tags `v0.0` to `v1.0`); post-merge work uses real dates.

## v1.0 — First playable campaign
- 3 heroes, 90 cards, 12 enemies, 3 two-phase bosses, 20 relics, 24 events, 5 companions.
- Interactive terminal play with autosave/resume; scripted demo mode.
- 88 tests green.

## v0.5 — Balance + human play (M5)
- Class HP split, aspect-shifting bosses, encounter retune.
- InteractiveLoop, SaveStore, ScriptedInput fuzz coverage.

## v0.4 — Enemies & content (M4)
- 4 AI behaviors, boss phases, narrative events, companions.

## v0.3 — Collection & economy (M3)
- Full 90-card collection, relics, dust/shards, tavern.

## v0.2 — Run loop (M2)
- 3-act maps, loot drafts, XP/boons, node types.

## v0.1 — Combat slice (M1)
- Turn engine, aspects, rows, JSON cards, first demo battle.

## v0.0 — Green build (M0)
- Maven build, prototype fixes, smoke tests.
