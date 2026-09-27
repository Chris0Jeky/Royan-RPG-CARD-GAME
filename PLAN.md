# PLAN — Royan RPG Card Game

> Live tracker. Update after every milestone. Survives compaction: read this + ARCHITECTURE.md + ORCHESTRATION.md to resume.

## Goal
Playable RPG card game ("Royan" roguelike deckbuilder, Java 17 + Maven + CLI) + backdated history from 2024-07-01 (~4 days/week, ~30 commits/week, random days/times, strictly increasing).

## Pitch (swarm synthesis, locked)
Sky-isles mercenary Guild Captain. Hub → 3-act branching map → lane combat (hero + companions vs enemies, front/back rows, telegraphed intents) → pick-1-of-3 loot/XP → deck + tavern upgrades → 2-phase boss. 3 heroes, 90 cards, 20 relics, 24 events, gold/dust/shards economy, levels 1–10 with boons. ~8-min runs, ~45-min campaign.

## Milestones
| # | Milestone | Status |
|---|-----------|--------|
| M0 | Green build: pom.xml (Java 17, JUnit5, AssertJ, Jackson), fix Player/Deck/Flow/Declaration, 7 files compile, smoke test | done (`dev@ce312ea`, 4/4 green) |
| M1 | Combat slice: Strike/Guard/Trick/Power, energy/draw/intents, front/back rows, 1 hero + 12-card deck, JSON loader | done (dev, 21/21 green, demo victory in 3 turns) |
| M2 | Run loop: 3-act map gen, pick-1-of-3 loot, XP boons, rest/shop/event/boss nodes | done (dev, 38/38 green, seed 6 wins campaign) |
| M3 | Collection/economy: 90 cards, gold/dust/shards, 20 relics, shrines, tavern | done (dev, 56/56 green; K 11/20, R 20/20, M 19/20 — tune at M5) |
| M4 | Enemies/content: AI intents (4 behaviors), 2-phase bosses, 24 events, companions | done (dev, 78/78 green; K 2/30, R 6/30, M 27/30 — rebalance at M5) |
| M5 | Campaign balance: 8-min/45-min pacing, 3 starter heroes, save/persistence | done (dev, 88/88 green; K 11/30, R 22/30, M 8/30; interactive CLI + autosave) |
| M6 | Docs + evolved system: README, RULES, DEVLOG, QA, AGENTS.md, `.agents/skills/royan-*` | done (dev, 88/88 green; schedule 3504 slots verified; exec path verified) |
| M7 | History replay: schedule Jul-2024→present ~30/wk on `history-replay`, verify histogram + monotonicity + tests at HEAD + spot mid-history, merge to main, tag v0.0..v1.0 | done (main: 3504/3504 slots, 117w, 29.95/wk, 3.99d/wk; tags v0.0..v1.0 on exact milestone trees; 88/88 HEAD, 78/78 v0.4 spot; source frozen at dev@b38f1e8) |
| M8 | Final QA pass + handoff | done (main: 88/88 final HEAD; auto K42 to Act3-boss defeat, R3 + M1 victories; quit/save/continue round-trip green; verify OK pinned v1.0; tree clean) |

## Verify gates (each milestone)
- `mvn -q test` green; CLI boots and can complete a scripted playthrough (`cli-smoke`).
- No engine→CLI imports (ArchUnit-style package check or grep gate in CI script).

## Phase 2 — Web UX overhaul (goal 2026-09-27; real dates, feature branches)
Stack decision (evidence: engine/UI split, zero new deps via JDK HttpServer +
Jackson): embedded Java web server + dependency-free browser frontend (vanilla
JS/CSS, inline SVG, WebAudio). CLI stays fully working.
| # | Milestone | Status |
|---|-----------|--------|
| W1 | Web foundation: `serve` mode, combat JSON API, playable browser combat screen (hero select → battle → play/end → victory), WebServerTest headless | done (96/96 green, serve smoke OK, auto K42 unchanged; merged to main) |
| W2 | Full run in browser: map, drafts, shop/tavern/event, level boons, save/resume | done (101/101 green, serve smoke OK; merged to main) |
| W3 | Feel pass: animations, damage numbers, screen shake (reduced-motion safe), WebAudio SFX + music, card/enemy art | done (103/103 green, art/audio smoke OK; merged to main) |
| W4 | RPG depth: hero stories, companion banter, event chains, run chronicle, bestiary/deck codex | done (109/109 green, codex smoke OK; merged to main) |
| W5 | Polish + rebalance for the new UX, visual QA suite, docs refresh, release v2.0 | done (109/109 green, probe = M5 lock, Edge QA 0 errors, docs refreshed; merged to main, tagged v2.0) |

W-gates: all M-gates plus `WebServerTest` green, browser smoke (serve + play
one battle over HTTP), no engine→`cli`/`web` imports.

## History verify (M7)
- `python tools/history/verify.py`: slots from 2024-07-01, ≈4 days/wk, ≈30 commits/wk, strictly increasing, author dates match slots.

## Decision log
- 2026-09-26: swarm overrode parent static-web sketch → Java 17 + Maven + CLI (continuity, headless QA). Parent accepted after confirming portable JDK download works.
- 2026-09-26: history = appended backdated commits (no rewrite of existing 18); replay branch + merge + tags.
