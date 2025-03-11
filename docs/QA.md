# QA — Gates, oracles, release checklist

## Test gates (every change)

```sh
mvn test          # must be green: 88 tests across 17 suites (M5)
```

- Engine suites (`combat`, `BattleArts`, `BossPhase`, `EnemyAi`) pin exact numbers —
  multipliers, cover, phases, AI tables. Touching combat math means updating these first.
- Content suites (`DataLoader`, `LootXp`, `RelicEconomy`, `NarrativeEvent`) pin collection
