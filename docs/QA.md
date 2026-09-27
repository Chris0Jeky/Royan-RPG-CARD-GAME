# QA — Gates, oracles, release checklist

## Test gates (every change)

```sh
mvn test          # must be green: 109 tests across 18 suites (W5)
```

- Engine suites (`combat`, `BattleArts`, `BossPhase`, `EnemyAi`) pin exact numbers —
  multipliers, cover, phases, AI tables. Touching combat math means updating these first.
- Content suites (`DataLoader`, `LootXp`, `RelicEconomy`, `NarrativeEvent`) pin collection
  sizes: 90 cards (24/24/24/18), 12 enemies (3 bosses), 20 relics, 28 events, 5 companions,
  3 heroes.
- Run suites (`RunEngine`, `BattlePlaythrough`, `CampaignPlaythrough`, `InteractiveLoop`,
  `SaveLoad`) pin the loop: drafts, shops, taverns, events, scripted + fuzz campaigns,
  save round-trips, quit/resume.
- Pinned victories: Knight seed 4, Ranger seed 3, Runemage seed 1. If a balance change
  breaks a pin, re-probe (below) and move the pin deliberately — never delete it.
- Web suites (`WebServerTest`, 18 tests): all run endpoints headless, shop/tavern
  transactions, save round-trip, codex sizes, art content-types.

## Web QA (after frontend changes)

1. `node --check` on `src/main/resources/web/app.js` and `audio.js`.
2. `WebServerTest` green (headless endpoint coverage).
3. Live serve smoke: `serve` on a scratch port, drive state via the API
   (`new-run`, `choose-node`), then Edge/Chrome headless `--dump-dom` with
   `--virtual-time-budget=6000 --enable-logging=stderr` and assert zero
   `ERROR:CONSOLE`/`Uncaught` lines plus real content markers per screen
   (heroes on select, node types on map, hand + foes + End Turn in battle).
   W5 reference: select/map/battle all rendered, 0 console errors.

## Balance oracle (after content/balance changes)

```sh
# 30 seeds per hero via the auto demo; expect roughly K 11/30, R 22/30, M 8/30 (M5 lock)
```

- Every hero must win ≥3/30 (winnable) and ≤28/30 (not trivial) on seeds 1–30.
- Investigate before tuning: find WHERE deaths happen (act/boss/fight), not just counts.
- Data changes reshuffle RNG streams, so seed-by-seed before/after comparisons are
  confounded — compare distributions, and prefer structural fixes (aspect coverage,
  HP pools, fight length) over number nudges. See DESIGN.md balance log.

## Fuzz oracle (after loop changes)

`InteractiveLoopTest.fullScriptedCampaignTerminates` runs a 2400-prompt fuzz campaign
through every interactive path (including invalid-input recovery). If it fails with
"script exhausted", some prompt loops without consuming progress — fix the loop,
not the script length.

## Manual QA (release)

1. `play` each hero for ≥1 act: prompts render, invalid input re-prompts, `quit` saves.
2. `continue` resumes mid-act with deck/gold/companions intact.
3. `auto` demo completes and prints a Result line.
4. Save file is valid JSON, deleted after victory/defeat.

## Release checklist

- [ ] `mvn test` green
- [ ] Balance oracle within bands, pins current
- [ ] README/RULES/DESIGN agree on numbers (costs, heals, multipliers)
- [ ] CHANGELOG + DEVLOG entries for the release
- [ ] Milestone tag on green main (`v2.0`; never move `v0.0`..`v1.0`)
