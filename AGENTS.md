# AGENTS.md — Contributor runbook

Read [ARCHITECTURE.md](ARCHITECTURE.md), [PLAN.md](PLAN.md), [ORCHESTRATION.md](ORCHESTRATION.md)
before changing anything. Rules authority is [docs/DESIGN.md](docs/DESIGN.md); player
facing rules live in [docs/RULES.md](docs/RULES.md) and must agree with it.

## Toolchain

- Java 17 + Maven 3.9 (`mvn test`, `mvn -q compile`). Portable installs documented in
  ORCHESTRATION.md. Set `JAVA_HOME` + `PATH` per shell — env does not persist.
