# ORCHESTRATION — Royan RPG Card Game

> Live tracker. How work is organized across sessions/agents. Read after PLAN.md to resume.

## Lanes (this session)
- `swarm-plan` (Workflow, 8 agents): DONE. Synthesis adopted for pitch + Java/Maven stack + M0–M5; its history advice (plain incremental commits) REJECTED in favor of backdated schedule replay (user requirement).
- `parent`: trackers, stack arbitration, toolchain. Next: portable JDK 17 + Maven 3.9 install → M0 via worker swarm.

## Session log (append; compaction-safe)
- 2026-09-26 ~14:00 UTC: goal set. Launched planning workflow (5→8 agents). Scouted repo: 18 commits to 2024-06-25 on `main`, remote `origin` = github.com/Chris0Jeky/Royan-RPG-CARD-GAME. Java prototype broken (Player ctor, Deck stubs, Player.java:9). No JDK on PATH; Node 24 + Python 3.13 present; network OK (Adoptium API reachable).
- 2026-09-26 ~14:20 UTC: swarm synthesis received (complete, no unresolved). Stack reversed to Java 17 + Maven + JUnit5 + Jackson + CLI. Trackers rewritten to swarm layout. JDK/Maven download starting.

## Toolchain (portable, outside repo — OneDrive-safe)
- Root: `$env:TEMP\royan-tools` (local disk, survives sessions; re-download if missing).
- `jdk17/` from `https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse`
- `maven/` from `https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip`
- Use: `$env:JAVA_HOME="$env:TEMP\royan-tools\jdk17"; $env:Path="$env:JAVA_HOME\bin;$env:TEMP\royan-tools\maven\bin;$env:Path"`
- `.gitignore` must cover `target/`, `out/`, `.tools/` (if ever used in-repo).

## History strategy (locked)
