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
