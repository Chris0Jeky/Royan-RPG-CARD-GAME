# Royan RPG Card Game

A roguelike deckbuilder about a sky-isles mercenary Guild Captain. Draft a war-band,
build a deck, cross a 3-act branching map, and kill the Sky-Tyrant's captains —
three two-phase bosses with aspect-shifting second forms.

- 3 heroes (Knight / Ranger / Runemage), 90 cards, 12 enemies, 20 relics, 24 events, 5 companions
- Lane combat with front/back rows, telegraphed intents, and an aspect triangle
- Full RPG layer: XP levels 1–10, boons, gold/dust/shards economy, shops, taverns, shrines
- Playable in the terminal: `play` (interactive), `continue` (autosave resume), `auto` (demo)

## Quickstart

Requires Java 17+ and Maven 3.9+. No other runtime dependencies (offline-safe after
the first Maven pull).

```sh
mvn test                                   # build + 88 tests
mvn -q compile exec:java -Dexec.mainClass=com.chris.cardgame.Main -Dexec.args="play"
```

(PowerShell: insert `--%` before the `-D` flags, e.g.
`mvn -q compile exec:java --% "-Dexec.mainClass=com.chris.cardgame.Main" "-Dexec.args=play"`.)

Then pick a captain and type commands (`play <card> [foe]`, `end`, `quit` saves).
Other modes:

```sh
... -Dexec.args="play ranger"              # hero + random seed
... -Dexec.args="play knight 42"           # hero + fixed seed (reproducible)
... -Dexec.args="continue"                 # resume .royan-save/save.json
... -Dexec.args="auto 42 KNIGHT"           # scripted demo campaign
```

No JDK handy? A portable Temurin 17 + Maven install works — see
[ORCHESTRATION.md](ORCHESTRATION.md#toolchain-portable-outside-repo--onedrive-safe).

## Docs

- [docs/RULES.md](docs/RULES.md) — how to play (players start here)
- [docs/DESIGN.md](docs/DESIGN.md) — locked rules authority + balance log
- [docs/QA.md](docs/QA.md) — test gates, balance oracle, release checklist
- [docs/DEVLOG.md](docs/DEVLOG.md) — milestone journal
- [CHANGELOG.md](CHANGELOG.md) — per-release changes
- [ARCHITECTURE.md](ARCHITECTURE.md), [PLAN.md](PLAN.md), [ORCHESTRATION.md](ORCHESTRATION.md) — live build trackers
- [AGENTS.md](AGENTS.md) — contributor/agent runbook

## Project shape

```
src/main/java/com/chris/cardgame/{model,data,combat,ai,map,loot,run,cli}/
src/main/resources/data/{cards,enemies,relics,events,companions}.json
src/test/java/...            # JUnit 5 + AssertJ suites
tools/history/               # backdated-commit schedule / replay / verify scripts
.agents/skills/royan-*/      # evolved project skills (design, qa, history)
```
