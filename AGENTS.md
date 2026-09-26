# AGENTS.md — Contributor runbook

Read [ARCHITECTURE.md](ARCHITECTURE.md), [PLAN.md](PLAN.md), [ORCHESTRATION.md](ORCHESTRATION.md)
before changing anything. Rules authority is [docs/DESIGN.md](docs/DESIGN.md); player
facing rules live in [docs/RULES.md](docs/RULES.md) and must agree with it.

## Toolchain

- Java 17 + Maven 3.9 (`mvn test`, `mvn -q compile`). Portable installs documented in
  ORCHESTRATION.md. Set `JAVA_HOME` + `PATH` per shell — env does not persist.
- Python 3.13 (stdlib only) for `tools/history/`.

## Build / test / run

```sh
mvn test                                                              # gate: all green
mvn -q compile exec:java -Dexec.mainClass=com.chris.cardgame.Main -Dexec.args="auto 42 KNIGHT"
```

PS note: `-Dexec.*` args are mangled by PowerShell parsing, so run java
directly: `mvn -q compile`, then `java -cp target/classes;<jackson jars>`
`com.chris.cardgame.Main <args>` (jars live under `$HOME/.m2/repository`,
`com/fasterxml/jackson/core/*/*/*.jar`).

## Phase 2 (web UI) deltas
- `Main serve [port]` runs the embedded web UI (default 8080). Browser app lives
  in `src/main/resources/web/` — vanilla JS/CSS, no build step, no external
  requests (offline-friendly). Verify with `WebServerTest` + an HTTP smoke.
- Frontend style: no AI-slop defaults (see taste: no purple gradients, no
  Inter-everywhere, no 3-card grids, no emoji icons); fantasy ink/parchment
  look; respect `prefers-reduced-motion`.

## Conventions

- Engine packages (`model`, `data`, `combat`, `ai`, `map`, `loot`, `run`) never import
  `cli` or `web`. UI code may import engines, never the reverse.
- All content is data: cards/enemies/relics/events/companions live in
  `src/main/resources/data/*.json` and load via Jackson. Code changes for content
  changes are a smell — extend the data fields instead.
- Seeded RNG everywhere (`SplittableRandom`). New randomness must take the run/battle
  RNG, never `new Random()` — determinism is tested.
- Numbers live in one place: multipliers in `DamageCalc`, economy in the JSON/run
  classes, balance history in DESIGN.md. Update all three together.
- Payments validate BEFORE spending (gold/dust/shards/deck-space). Regression-test
  every new transaction.
- Tests are JUnit 5 + AssertJ, colocated in `src/test/java/com/chris/cardgame/`.
  Pin exact numbers for combat math; pin collection sizes for content.

## Skills

Follow `.agents/skills/royan-*/SKILL.md` for the area you touch:
`royan-design` (content/balance), `royan-qa` (gates before commit),
`royan-history` (backdated-commit runbook).
