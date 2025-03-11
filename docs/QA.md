# QA — Gates, oracles, release checklist

## Test gates (every change)

```sh
mvn test          # must be green: 88 tests across 17 suites (M5)
```

- Engine suites (`combat`, `BattleArts`, `BossPhase`, `EnemyAi`) pin exact numbers —
  multipliers, cover, phases, AI tables. Touching combat math means updating these first.
- Content suites (`DataLoader`, `LootXp`, `RelicEconomy`, `NarrativeEvent`) pin collection
  sizes: 90 cards (24/24/24/18), 12 enemies (3 bosses), 20 relics, 24 events, 5 companions.
- Run suites (`RunEngine`, `BattlePlaythrough`, `CampaignPlaythrough`, `InteractiveLoop`,
  `SaveLoad`) pin the loop: drafts, shops, taverns, events, scripted + fuzz campaigns,
  save round-trips, quit/resume.
- Pinned victories: Knight seed 4, Ranger seed 3, Runemage seed 1. If a balance change
  breaks a pin, re-probe (below) and move the pin deliberately — never delete it.

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
- [ ] Milestone tag on green main (`v0.1` … `v1.0`)
