# DEVLOG — Milestone journal

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
