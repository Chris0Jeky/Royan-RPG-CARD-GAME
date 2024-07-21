# ORCHESTRATION — Royan RPG Card Game

> Live tracker. How work is organized across sessions/agents. Read after PLAN.md to resume.

## Lanes (this session)
- `swarm-plan` (Workflow, 8 agents): DONE. Synthesis adopted for pitch + Java/Maven stack + M0–M5; its history advice (plain incremental commits) REJECTED in favor of backdated schedule replay (user requirement).
- `parent`: trackers, stack arbitration, toolchain. Next: portable JDK 17 + Maven 3.9 install → M0 via worker swarm.

## Session log (append; compaction-safe)
- 2026-09-26 ~14:00 UTC: goal set. Launched planning workflow (5→8 agents). Scouted repo: 18 commits to 2024-06-25 on `main`, remote `origin` = github.com/Chris0Jeky/Royan-RPG-CARD-GAME. Java prototype broken (Player ctor, Deck stubs, Player.java:9). No JDK on PATH; Node 24 + Python 3.13 present; network OK (Adoptium API reachable).
- 2026-09-26 ~14:20 UTC: swarm synthesis received (complete, no unresolved). Stack reversed to Java 17 + Maven + JUnit5 + Jackson + CLI. Trackers rewritten to swarm layout. JDK/Maven download starting.
- 2026-09-26 ~21:00 UTC: portable Temurin 17.0.20.1 + Maven 3.9.9 verified under `$env:TEMP\royan-tools`. M0 DONE on `dev@ce312ea`: Maven layout, fixed Player/Deck/Flow/Declaration/Mechanics, SmokeTest 4/4 green. Next: M1 combat slice (model/combat packages, JSON card loader, energy/draw/intents, front/back rows).
- 2026-09-26 ~22:05 UTC: M1 DONE on dev: model/data/combat/cli packages, 18 cards + 4 enemies JSON, DESIGN.md locked, 21/21 tests green, Main demo plays (victory turn 3). Next: M2 run loop (map gen, loot pick-1-of-3, XP boons, nodes).

## Toolchain (portable, outside repo — OneDrive-safe)
- Root: `$env:TEMP\royan-tools` (local disk, survives sessions; re-download if missing).
- `jdk17/` from `https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse`
- `maven/` from `https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip`
- Use: `$env:JAVA_HOME="$env:TEMP\royan-tools\jdk17"; $env:Path="$env:JAVA_HOME\bin;$env:TEMP\royan-tools\maven\bin;$env:Path"`
- `.gitignore` must cover `target/`, `out/`, `.tools/` (if ever used in-repo).

## History strategy (locked)
- Span: 2024-07-01 → present (~117 weeks → ~3510 slots at 30/wk). Seeded PRNG; 4 distinct days/week (weighted, not always weekends); 6–9 commits/day jitter; times 09:00–23:30, strictly increasing.
- Replay on branch `history-replay` from `main@0b09260`: order construction into micro-steps (scaffold → model → combat → loop → economy → content → balance → docs → tests interleaved); 1+ commits per slot via `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE`.
- Verify: histogram + monotonicity (`tools/history/verify.py`) + `mvn test` at HEAD + spot-check mid-history builds + tags v0.1..v1.0 per milestone, then merge to main.
- NEVER `push --force`, never rewrite the 18 existing commits. Push only if user asks (default: local).

## Evolving system (instructions/skills)
- M6: `AGENTS.md` (build/test/commit runbook) + `.agents/skills/royan-{design,qa,history}/SKILL.md` (balance rules, playthrough gates, replay runbook). Update when rules change.
- This file + PLAN.md are the compaction bridge.

## Resume checklist
1. Read PLAN.md, ARCHITECTURE.md, this file.
2. `git log --oneline -5; git status --short; git branch -a`
3. Ensure `$env:TEMP\royan-tools\jdk17\bin\java -version` works; else re-download.
4. Continue at first non-complete milestone in PLAN.md.
