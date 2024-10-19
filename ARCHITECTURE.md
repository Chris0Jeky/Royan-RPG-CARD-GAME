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
  model/        CardDef (hits/aoe/energy arts), EnemyDef, Combatant (plating, baseStrength, turn-1 stats),
              CardType, Aspect, Rarity, HeroClass, Row, RelicDef, RelicEffect (+ legacy Card/Deck/Player at root)
  data/         CardLoader (90), EnemyLoader, RelicLoader (20) (Jackson JSON from resources/data)
  combat/       CombatEngine, CombatState, DamageCalc, Intent/IntentKind
  map/          MapGen, ActMap, MapNode, NodeType (layered DAG, 15/18/22 nodes)
  loot/         EncounterGen, LootGen (pick-1-of-3), XpCurve, Boon
  run/          RunState (gold/dust/shards/relics/deck-thinning), RunEngine, Events (6 incl. shrines),
              Shop (cards + relics), Tavern (heal/removal/relic trade)
  map/          MapGen, MapNode, Act (3-act branching: 15/18/22 nodes)
  loot/         LootGen (pick-1-of-3), XpCurve, Economy (gold/dust/shards)
  ai/           EnemyAi (aggro/turtle/burst/trickster), BossPhases
  cli/          Main, GameLoop, Render (text), Input, SaveStore
src/main/resources/data/  cards.json, relics.json, events.json, maps/
src/test/java/...         engine/combat/loot/ai/cli-smoke suites
docs/           DESIGN.md, RULES.md, DEVLOG.md, QA.md
tools/history/  schedule.py, replay.py, verify.py
.agents/skills/ royan-design, royan-qa, royan-history (M6)
src-legacy/     original 7-file prototype (moved at M0, reference only)
```

## Data model (v1, from swarm pitch "Royan")
- Card: `{id, name, cost(0-3), type(Strike|Guard|Trick|Power|Curse), atk, def, effect, rarity, heroClass, flavor}` — 90 cards (30/hero × Knight/Ranger/Runemage).
- Deck: start 12, max 30, max 3 copies; draw 4/turn, 3 energy/turn.
- Hero/Player: `{name, class, hp, maxHp, xp, level(1-10, pick-1-of-3 boons), gold, dust, shards, deck, relics[], quests[], nodeId}`.
- Enemy: `{id, name, hp, atk, def, behavior, intents[], lootTable, xp}`; lane combat hero+0–2 companions vs 1–3, front/back rows, telegraphed intents.
- Encounters: hub → 3-act map (rest/shop/event/boss nodes) → combat → loot/XP → deck+tavern → 2-phase boss. ~8-min runs, ~45-min campaign. 20 relics, 24 narrative events.
- Advantage triangle: Might→Guile→Focus→Might.
- Save: versioned JSON file under `~/.royan/` (or CWD `.royan-save/` for tests); seeded RNG (SplittableRandom) for reproducible playthroughs.

## Invariants
- `model/combat/map/loot/ai` have zero CLI imports → pure unit-testable.
- All content data lives in `src/main/resources/data/*.json`; Java loads via Jackson at boot.
- Deterministic RNG seed per run; playthrough tests pin seeds.
- Existing 18 commits (to 2024-06-25) are NEVER rewritten; backdated history is appended with old dates.
