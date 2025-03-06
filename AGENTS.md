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

## Conventions

- Engine packages (`model`, `data`, `combat`, `ai`, `map`, `loot`, `run`) never import
