#!/usr/bin/env python3
"""Verify the backdated history on a branch (M7).

Checks:
  1. commit count and date range match schedule.json (author dates)
  2. ~4 days/week, ~30 commits/week bands
  3. strictly increasing author dates from 2024-07-01 onward
  4. final tree equals the source branch tree
  5. milestone tags exist (tag green-checks run separately via mvn)

Usage: python tools/history/verify.py --branch history-replay --source dev
"""

import argparse
import datetime as dt
import json
import subprocess
import sys
from pathlib import Path

START = dt.date(2024, 7, 1)


def git(*args: str) -> str:
    out = subprocess.run(["git", *args], capture_output=True, text=True, check=True)
    return out.stdout.strip()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--branch", required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--schedule", type=Path,
                        default=Path(__file__).with_name("schedule.json"))
    parser.add_argument("--tags", nargs="*", default=["v0.0", "v0.1", "v0.2", "v0.3",
                                                      "v0.4", "v0.5", "v1.0"])
    args = parser.parse_args()

    schedule = json.loads(args.schedule.read_text(encoding="utf-8"))
    log = git("log", args.branch, "--format=%aI", "--reverse",
              "--since", START.isoformat())
