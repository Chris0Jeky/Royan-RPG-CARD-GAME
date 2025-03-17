# royan-qa — Gates before commit

Use before marking any milestone or release done.

## Gates

1. `mvn test` — all green, no skips. A failing test is the requirement, not the test's bug.
2. Engine changes: exact-number suites updated first (`CombatEngine`, `BattleArts`,
   `BossPhase`, `EnemyAi`). Explain-then-update on expectation changes.
