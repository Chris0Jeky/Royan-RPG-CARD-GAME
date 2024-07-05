# ARCHITECTURE — Royan RPG Card Game

> Live tracker. Module map + data model. Update when structure changes.

## Stack (locked 2026-09-26, swarm synthesis)
- **Java 17 LTS** (Temurin portable, `file://` reproducible; see ORCHESTRATION.md for path) + **Maven 3.9**.
- Tests: **JUnit5 + AssertJ** (`mvn test`). JSON data: **Jackson**.
- UI v1: **console CLI** (no engine/UI deps in engine). Browser UI is a post-v1 option, not planned.
- History tooling: Python 3.13 scripts in `tools/history/`.
- Why Java over static-web: preserves the repo's 18-commit continuity and the swarm's converged design; CLI is headlessly QA-able; JDK installs portably.

## Layout (target, Maven)
```
pom.xml                       Java 17, JUnit5, AssertJ, Jackson
src/main/java/com/chris/cardgame/
  model/        Card, Deck, Hero, Player, Enemy, Relic, CardType, Rarity, Effect
  combat/       CombatEngine, TurnState, DamageCalc, Intent, Advantage
  map/          MapGen, MapNode, Act (3-act branching: 15/18/22 nodes)
  loot/         LootGen (pick-1-of-3), XpCurve, Economy (gold/dust/shards)
  ai/           EnemyAi (aggro/turtle/burst/trickster), BossPhases
  cli/          Main, GameLoop, Render (text), Input, SaveStore
src/main/resources/data/  cards.json, relics.json, events.json, maps/
