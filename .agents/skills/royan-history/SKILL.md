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
