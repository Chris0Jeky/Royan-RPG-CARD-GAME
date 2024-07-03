# PLAN — Royan RPG Card Game

> Live tracker. Update after every milestone. Survives compaction: read this + ARCHITECTURE.md + ORCHESTRATION.md to resume.

## Goal
Playable RPG card game ("Royan" roguelike deckbuilder, Java 17 + Maven + CLI) + backdated history from 2024-07-01 (~4 days/week, ~30 commits/week, random days/times, strictly increasing).

## Pitch (swarm synthesis, locked)
Sky-isles mercenary Guild Captain. Hub → 3-act branching map → lane combat (hero + companions vs enemies, front/back rows, telegraphed intents) → pick-1-of-3 loot/XP → deck + tavern upgrades → 2-phase boss. 3 heroes, 90 cards, 20 relics, 24 events, gold/dust/shards economy, levels 1–10 with boons. ~8-min runs, ~45-min campaign.

## Milestones
| # | Milestone | Status |
|---|-----------|--------|
| M0 | Green build: pom.xml (Java 17, JUnit5, AssertJ, Jackson), fix Player/Deck/Flow/Declaration, 7 files compile, smoke test | pending |
| M1 | Combat slice: Strike/Guard/Trick/Power, energy/draw/intents, front/back rows, 1 hero + 12-card deck, JSON loader | pending |
| M2 | Run loop: 3-act map gen, pick-1-of-3 loot, XP boons, rest/shop/event/boss nodes | pending |
| M3 | Collection/economy: 90 cards, dust 3:1, gold/dust/shards, 20 relics, shrines, tavern | pending |
| M4 | Enemies/content: AI intents (4 behaviors), 2-phase bosses, 24 events, companions | pending |
| M5 | Campaign balance: 8-min/45-min pacing, 3 starter heroes, save/persistence | pending |
| M6 | Docs + evolved system: README, RULES, DEVLOG, QA, AGENTS.md, `.agents/skills/royan-*` | pending |
| M7 | History replay: schedule Jul-2024→present ~30/wk on `history-replay`, verify histogram + monotonicity + tests at HEAD + spot mid-history, merge to main, tag v0.1..v1.0 | pending |
