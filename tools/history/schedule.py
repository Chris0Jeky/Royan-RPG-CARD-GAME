#!/usr/bin/env python3
"""Seeded backdated-commit schedule generator (M7).

Emits one strictly-increasing datetime slot per commit, from START (a Monday)
to the Monday of END week: ~4 random days/week, ~30 commits/week, random times
09:00-23:30. Deterministic for a fixed SEED.

Usage: python tools/history/schedule.py [--seed N] [--out FILE]
Writes schedule.json (list of {"slot": i, "date": ISO8601}) next to this file
by default, after self-verifying bands and monotonicity.
"""

import argparse
import datetime as dt
import json
import random
import sys
from pathlib import Path

START = dt.date(2024, 7, 1)  # a Monday
END = dt.date(2026, 9, 26)
SEED = 20240701
DAYS_PER_WEEK = 4
COMMITS_PER_WEEK = 30
DAY_START_MIN = 9 * 60
DAY_END_MIN = 23 * 60 + 30


def monday_weeks(start: dt.date, end: dt.date):
    weeks = []
    day = start
    while day <= end:
        weeks.append(day)
        day += dt.timedelta(days=7)
