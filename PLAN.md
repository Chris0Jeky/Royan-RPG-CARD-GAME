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
