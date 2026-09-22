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
    return weeks


def generate(seed: int = SEED) -> list:
    rng = random.Random(seed)
    slots: list[dt.datetime] = []
    for monday in monday_weeks(START, END):
        days = sorted(rng.sample(range(7), DAYS_PER_WEEK))
        counts = [COMMITS_PER_WEEK // DAYS_PER_WEEK] * DAYS_PER_WEEK
        for i in rng.sample(range(DAYS_PER_WEEK), COMMITS_PER_WEEK % DAYS_PER_WEEK):
            counts[i] += 1
        # jitter +-1 while keeping the weekly total at 30
        for _ in range(2):
            a, b = rng.sample(range(DAYS_PER_WEEK), 2)
            if counts[a] > 5:
                counts[a] -= 1
                counts[b] += 1
        for day_offset, count in zip(days, counts):
            day = monday + dt.timedelta(days=int(day_offset))
            if day > END:
                continue
            minutes = sorted(rng.sample(range(DAY_START_MIN, DAY_END_MIN), count))
            for minute in minutes:
                slots.append(dt.datetime.combine(day, dt.time(minute // 60, minute % 60)))
    slots.sort()
    # enforce strict increase (sample() is unique per day; sort keeps global order)
    for prev, cur in zip(slots, slots[1:]):
        assert cur > prev, f"non-monotonic slots: {prev} {cur}"
    return slots


def self_check(slots: list) -> None:
    assert slots, "empty schedule"
    assert slots[0].date() >= START and slots[-1].date() <= END
    by_week: dict = {}
    for slot in slots:
        monday = slot.date() - dt.timedelta(days=slot.date().weekday())
        by_week.setdefault(monday, []).append(slot)
    weeks = len(by_week)
    assert weeks >= 100, f"too few weeks: {weeks}"
    total = len(slots)
    avg_per_week = total / weeks
    assert 29.0 <= avg_per_week <= 30.0, f"avg/week {avg_per_week}"
    day_counts = [len({s.date() for s in v}) for v in by_week.values()]
    assert min(day_counts) >= 3 and max(day_counts) <= 4, "day band violated"
    assert abs(sum(day_counts) / weeks - 4.0) < 0.05, "avg days/week off"
    print(f"weeks={weeks} commits={total} avg/week={avg_per_week:.2f} "
          f"avg-days/week={sum(day_counts) / weeks:.2f} "
          f"first={slots[0]} last={slots[-1]}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int, default=SEED)
    parser.add_argument("--out", type=Path, default=Path(__file__).with_name("schedule.json"))
    args = parser.parse_args()
    slots = generate(args.seed)
    self_check(slots)
    payload = [{"slot": i, "date": s.isoformat(timespec="minutes")} for i, s in enumerate(slots)]
