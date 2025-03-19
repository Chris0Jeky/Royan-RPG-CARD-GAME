# royan-qa — Gates before commit

Use before marking any milestone or release done.

## Gates

1. `mvn test` — all green, no skips. A failing test is the requirement, not the test's bug.
2. Engine changes: exact-number suites updated first (`CombatEngine`, `BattleArts`,
   `BossPhase`, `EnemyAi`). Explain-then-update on expectation changes.
3. Content changes: size pins updated (`DataLoader`, `RelicEconomy`, `NarrativeEvent`).
4. Loop changes: run suites + fuzz campaign green (`RunEngine`, `BattlePlaythrough`,
   `CampaignPlaythrough`, `InteractiveLoop`, `SaveLoad`).
5. Balance-affecting changes: 30-seed oracle per hero within 3–28 wins; pins current.
6. Docs agree: DESIGN (authority) ↔ RULES (players) ↔ QA ↔ skill budgets.

## Fuzz oracle

`InteractiveLoopTest.fullScriptedCampaignTerminates` (2400 prompts) must pass —
it covers every interactive path including invalid-input recovery. "Script exhausted"
means a prompt loop regressed; fix the loop.

## Manual spot checks (release only)

Play one act per hero, `quit`→`continue` round-trip, `auto` demo completes.
See docs/QA.md release checklist.
