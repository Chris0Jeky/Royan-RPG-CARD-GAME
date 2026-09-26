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
- 2026-09-26 ~22:15 UTC: M2 DONE on dev: map/loot/run packages, full campaign loop (Main runs 3-act campaign), 38/38 green incl. pinned seed-6 victory. Balance: enemy HP cut, hero 80, elite double-draft, War Paint scaling boon. Lesson: parallel write_file to same new dir races (os 183) — pre-create dirs. Next: M3 collection/economy (90 cards, relics, dust/shards, tavern).
- 2026-09-26 ~22:30 UTC: M3 DONE on dev: 90 cards (24/class+18 neutral), 20 relics, AoE/multi-hit/energy arts, card-aspect advantage, dust/shards economy, Tavern node, shrine events, shop relics, 3 hero starters selectable. 56/56 green. Balance note: Ranger/Runemage overperform (20/20, 19/20 vs Knight 11/20) — M5 tuning. Next: M4 enemies/content (4 AI behaviors, 2-phase bosses, 24 events, companions).
- 2026-09-26 ~22:45 UTC: M4 DONE on dev: ai/EnemyAi (4 behaviors), 10 enemies + 3 two-phase bosses, 24 narrative events (costed choices, no-repeat/act), 5 companions (Striker/Guardian/Medic, max 2, tavern/event recruits). 78/78 green. Balance alert: K 2/30, R 6/30, M 27/30. Next: M5 campaign balance + save/persistence (also interactive CLI? decide at M5).
- 2026-09-26 ~23:15 UTC: M5 DONE on dev: balance locked (K 11/30, R 22/30, M 8/30; per-class HP, aspect-shift bosses, duelist+assassin, pool retune); InteractiveLoop (play/continue/auto, validated prompts, scripted-input tested incl. full fuzz campaign); JSON autosave + resume + save/load tests. 88/88 green. Note: multi-line stdin pipes to java hang under PowerShell managed shell (single-line pipe OK) — test evidence via ScriptedInput instead. Next: M6 docs + evolved system (README, RULES, QA, AGENTS.md, royan-* skills).
- 2026-09-26 ~23:30 UTC: M6 DONE on dev: README/RULES/QA/DEVLOG/CHANGELOG/AGENTS.md, royan-{design,qa,history} skills, exec-maven-plugin (verified via --% on PS), schedule.py + schedule.json (117w/3504 slots, bands hold) + verify.py. 88/88 green. Next: M7 replay.py + execute (~3500 commits), tags, merge to main. Strategy: partition dev-diff into per-slot chunks in dependency order; milestone tags on exact dev-commit trees. Load bundled:git before executing.
- 2026-09-26: M7 DONE on main: replay.py (frozen source dev@b38f1e8; new-file line-prefix atoms + whole-file M atoms, weight-proportional contiguous groups; fixed strip/newline + repair-ordering bugs) → history-replay 3504 commits (M0:86 M1:205 M2:187 M3:154 M4:236 M5:190 M6:2446), tags v0.0..v1.0 on exact trees, verify OK pre/post merge, ff-merged main (=v1.0 tip 45eff108). 88/88 HEAD, 78/78 v0.4 spot. verify.py gained --base + merge-base tag checks; history skill procedure corrected. Post-merge tooling committed with real dates; dev/history-replay branches retained (delete only if asked). Next: M8 final QA + handoff.
- 2026-09-26: M8 DONE on main: release QA green (88/88 final HEAD; auto K42 full-campaign defeat at Act3 boss, R3/M1 pinned victories via CLI; quit/save/continue round-trip with valid JSON save, save dir cleaned). Balance oracle NOT rerun (no content/balance changes since M5 lock). Docs agree (no number changes since M6 pass). CHANGELOG Unreleased + DEVLOG M7/M8 entries written. v1.0 tag on green main. Goal complete.

- 2026-09-27: Phase 2 W1 DONE on `feature/web-ui-foundation`: embedded web UI (web/GameSession+WebServer, `serve` mode, browser combat screen: hero select/cards/intents/log), WebServerTest 8/8 headless, 96/96 green, live serve smoke OK (state/new-battle/end-turn), plus RunEngine→cli decouple (new run/AutoBattle, GameLoop adapter, engine/UI import gate). Auto K42 oracle unchanged (Act3 defeat). Swarm synthesis reconciled (confirms JVM-native UI; history-risk item carried). PS `-Dexec.*` args mangle — java -cp recipe in AGENTS.md. Next: merge W1, then W2 full run in browser.

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
