# ARCHITECTURE — Royan RPG Card Game

> Live tracker. Module map + data model. Update when structure changes.

## Stack (locked 2026-09-26, swarm synthesis)
- **Java 17 LTS** (Temurin portable, `file://` reproducible; see ORCHESTRATION.md for path) + **Maven 3.9**.
- Tests: **JUnit5 + AssertJ** (`mvn test`). JSON data: **Jackson**.
- UI v1: **console CLI** (no engine/UI deps in engine). Browser UI is a post-v1 option, not planned.
- History tooling: Python 3.13 scripts in `tools/history/`.
- Why Java over static-web: preserves the repo's 18-commit continuity and the swarm's converged design; CLI is headlessly QA-able; JDK installs portably.
