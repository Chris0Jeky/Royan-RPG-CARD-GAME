# DEVLOG — Milestone journal

## M5 — Balance lock, interactive CLI, autosave (Jan 2025)
- Per-class HP (Knight 90 / Ranger 80 / Runemage 72); bosses shift aspect in phase 2;
  added duelist + assassin; pools retuned. Final auto-rates: K 11/30, R 22/30, M 8/30.
- `play` (interactive campaign), `continue` (JSON autosave resume), `auto` (demo).
  ScriptedInput drives the same loop in tests, including a full fuzz campaign.
- Fixed payment bugs: shop/tavern validate before spending (regression-tested).

