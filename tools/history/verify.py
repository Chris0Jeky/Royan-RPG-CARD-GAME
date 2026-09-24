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
