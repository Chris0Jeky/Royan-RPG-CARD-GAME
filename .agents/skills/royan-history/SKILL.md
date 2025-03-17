# royan-history — Backdated-commit runbook

Use for the July-2024 incremental history (M7). Read bundled:git first if available.

## Contract

- Existing 18 commits (to 2024-06-25) are NEVER rewritten.
- New history is APPENDED to `main` with backdated author/committer dates from
  2024-07-01, ~4 days/week, ~30 commits/week, random days/times, strictly increasing.
- Final tree must equal the source branch tree exactly (`git diff` empty).

## Tools (`tools/history/`, stdlib-only Python)

- `schedule.py` — seeded slot generator. Verifies its own output (counts, bands,
  monotonicity) and writes `schedule.json`.
- `replay.py` — partitions the source diff into one chunk per slot (dependency order:
  scaffold → model → data → engine → content → tests → docs), applies each chunk on
  `history-replay` from `main`, commits with the slot's dates. Milestone tags land on
  exact source-commit trees (green by construction).
- `verify.py` — histogram (days/week, commits/week), monotonicity, tag checks
  (`mvn test` at each tag), final-tree equality.

## Procedure

1. `python tools/history/schedule.py` → inspect bands.
2. `python tools/history/replay.py --source dev --dest history-replay` (slow: ~3500 commits).
3. `python tools/history/verify.py --branch history-replay` → all checks pass.
4. Fast-forward `main`, tag `v0.0`…`v1.0`, re-verify on `main`.
5. NEVER `push --force`. Push only when asked.
