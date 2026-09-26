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

1. `python tools/history/schedule.py` → inspect bands in `schedule.json`.
2. Freeze the source: note the milestone-tip SHA (`git rev-parse dev`).
3. `python tools/history/replay.py --plan --source <sha>` → inspect per-milestone counts.
4. `git worktree add .replay-wt main`, then
   `python tools/history/replay.py --repo .replay-wt --source <sha>`
   (slow: ~3500 commits; tags `v0.0`…`v1.0` land during the run).
5. `python tools/history/verify.py --branch history-replay --source <sha> --base main`
   → all checks pass. `mvn test` at HEAD; spot-check one tag in a spare worktree.
6. Fast-forward `main` to `history-replay`, remove the scratch worktree,
   re-verify pinned at the tag: `--branch v1.0 --source <sha> --base main`.
7. NEVER `push --force`. Push only when asked.
