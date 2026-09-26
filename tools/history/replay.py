#!/usr/bin/env python3
"""Partition a source branch into per-slot micro-commits and replay them (M7).

Strategy: for each milestone commit on --source (linear, after --base), split
its cumulative diff into contiguous line-groups in dependency-tier order, one
group per schedule slot. New files grow by line prefixes; modified files land
whole; deletions remove. Milestone tags land on exact source-commit trees.

SAFETY: refuses to run inside the main workspace (--repo must differ);
intended for a scratch clone. Read-only --plan mode prints the fit.

Source is a FROZEN milestone-tip SHA (not a moving branch): replay output must
stay reproducible even as dev/main advance. Post-merge work continues on main
with real dates; replay.py itself lands there too (it is not part of history).

Usage:
  python tools/history/replay.py --plan --source <milestone-tip-sha>
  git worktree add .replay-wt main
  python tools/history/replay.py --repo .replay-wt --source <milestone-tip-sha>
"""

import argparse
import datetime as dt
import json
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent


def canon(path: Path) -> Path:
    r"""Strip Windows \\?\ prefix: git.exe cannot use extended-path cwds."""
    text = os.path.normpath(str(path))
    if text.startswith("\\\\?\\"):
        text = text[4:]
    return Path(text)


WORKSPACE = canon(HERE.parent.parent)  # tools/history -> repo root
TAGS = ["v0.0", "v0.1", "v0.2", "v0.3", "v0.4", "v0.5", "v1.0"]
MILESTONES = ["M0", "M1", "M2", "M3", "M4", "M5", "M6"]


def git(repo: Path, *args: str, env: dict | None = None) -> str:
    merged = dict(os.environ)
    if env:
        merged.update(env)
    out = subprocess.run(["git", *args], cwd=repo, capture_output=True, text=True,
                         env=merged, check=True)
    return out.stdout.strip()


def git_blob(repo: Path, *args: str) -> str:
    """Byte-exact blob read: no strip, no newline translation."""
    out = subprocess.run(["git", *args], cwd=repo, capture_output=True, check=True)
    return out.stdout.decode("utf-8")


def tier(path: str) -> int:
    order = [
        "pom.xml", ".gitignore", ".gitattributes", "README.md", "AGENTS.md", "CHANGELOG.md",
        "PLAN.md", "ARCHITECTURE.md", "ORCHESTRATION.md",
        "src/main/java/com/chris/cardgame/model/",
        "src/main/resources/data/",
        "src/main/java/com/chris/cardgame/data/",
        "src/main/java/com/chris/cardgame/combat/",
        "src/main/java/com/chris/cardgame/ai/",
        "src/main/java/com/chris/cardgame/map/",
        "src/main/java/com/chris/cardgame/loot/",
        "src/main/java/com/chris/cardgame/run/",
        "src/main/java/com/chris/cardgame/cli/",
        "src/main/java/com/chris/cardgame/",
        "src/test/",
        "docs/",
        ".agents/",
        "tools/",
    ]
    for i, prefix in enumerate(order):
        if path == prefix or path.startswith(prefix):
            return i
    return len(order)


def milestone_shas(repo: Path, base: str, source: str) -> list[str]:
    out = git(repo, "log", "--reverse", "--format=%H", f"{base}..{source}")
    shas = [line for line in out.splitlines() if line]
    assert len(shas) == len(MILESTONES), f"expected {len(MILESTONES)} milestones, got {len(shas)}"
    return shas


def numstat(repo: Path, parent: str, sha: str) -> list[tuple[int, int, str]]:
    out = git(repo, "diff", "--no-renames", "--numstat", parent, sha)
    rows = []
    for line in out.splitlines():
        added, removed, path = line.split("\t")
        rows.append((int(added), int(removed), path))
    return rows


def name_status(repo: Path, parent: str, sha: str) -> list[tuple[str, str]]:
    out = git(repo, "diff", "--no-renames", "--name-status", parent, sha)
    rows = []
    for line in out.splitlines():
        status, path = line.split("\t", 1)
        rows.append((status, path))
    return rows


def allocate_slots(weights: list[float], total: int, floors: list[int]) -> list[int]:
    raw = [w * total / sum(weights) for w in weights]
    slots = [max(f, int(r)) for f, r in zip(floors, raw)]
    diff = total - sum(slots)
    order = sorted(range(len(slots)), key=lambda i: raw[i] - slots[i], reverse=diff > 0)
    i = 0
    while diff != 0:
        idx = order[i % len(order)]
        if diff > 0:
            slots[idx] += 1
            diff -= 1
        elif slots[idx] > floors[idx]:
            slots[idx] -= 1
            diff += 1
        i += 1
    assert sum(slots) == total
    return slots


def split_prefixes(text: str) -> list[str]:
    """Cumulative line-prefixes of text; last element equals text exactly."""
    lines = text.split("\n")
    prefixes = []
    for count in range(1, len(lines) + 1):
        prefix = "\n".join(lines[:count])
        if count < len(lines):
            prefix += "\n"
        prefixes.append(prefix)
    assert prefixes[-1] == text
    return prefixes


def build_atoms(repo: Path, parent: str, sha: str) -> list[dict]:
    """Ordered atoms: new-file line prefixes (weight 1 each), whole files, deletes."""
    atoms = []
    stats = {path: (added + removed) for added, removed, path
             in numstat(repo, parent, sha)}
    changes = sorted(name_status(repo, parent, sha), key=lambda c: (tier(c[1]), c[1]))
    for status, path in changes:
        if status == "D":
            atoms.append({"op": "delete", "path": path, "weight": 1, "sample": path})
        elif status == "A":
            text = git_blob(repo, "show", f"{sha}:{path}")
            text_lines = text.split("\n")
            for i, prefix in enumerate(split_prefixes(text)):
                sample = text_lines[i] if i < len(text_lines) else path
                atoms.append({"op": "prefix", "path": path, "weight": 1,
                              "content": prefix, "sample": sample or path})
        else:  # M, T, etc: land whole
            text = git_blob(repo, "show", f"{sha}:{path}")
            atoms.append({"op": "whole", "path": path,
                          "weight": max(1, stats.get(path, 1)),
                          "content": text, "sample": path})
    return atoms


def group_atoms(atoms: list[dict], n: int) -> list[list[dict]]:
    """Contiguous weight-proportional partition; every group non-empty.

    Contiguity is load-bearing: a file's atoms stay ordered so the last
    writer wins. Never move atoms across groups after cutting.
    """
    assert 1 <= n <= len(atoms), f"need 1..{len(atoms)} groups, got {n}"
    total = sum(a["weight"] for a in atoms)
    groups: list[list[dict]] = []
    cur: list[dict] = []
    running = 0
    for idx, atom in enumerate(atoms):
        cur.append(atom)
        running += atom["weight"]
        remaining_groups = n - len(groups) - 1
        if remaining_groups == 0:
            continue  # last group takes the tail
        remaining_atoms = len(atoms) - idx - 1
        target_done = running >= total * (len(groups) + 1) / n
        if (target_done and remaining_atoms > remaining_groups) or (
                remaining_atoms == remaining_groups):
            groups.append(cur)
            cur = []
    groups.append(cur)
    assert len(groups) == n and all(groups)
    assert sum(map(len, groups)) == len(atoms)
    return groups


def area_of(path: str) -> str:
    parts = path.split("/")
    if path.startswith("src/main/java/com/chris/cardgame/"):
        sub = parts[5] if len(parts) > 6 else "core"
        return {"model": "model", "data": "data", "combat": "combat", "ai": "ai",
                "map": "map", "loot": "loot", "run": "run", "cli": "cli"}.get(sub, "core")
    if path.startswith("src/main/resources/data/"):
        return Path(path).stem
    if path.startswith("src/test/"):
        return "test"
    if path.startswith("docs/"):
        return "docs"
    if path.startswith("tools/history/"):
        return "history"
    if path.startswith(".agents/"):
        return "skills"
    return Path(path).stem or "repo"


def clean_sample(text: str) -> str:
    cleaned = text.strip().strip("{},\"'").strip()
    for prefix in ("//", "*", "#", "-", "+", "return ", "public ", "private ", "case "):
        if cleaned.startswith(prefix):
            cleaned = cleaned[len(prefix):].strip()
    cleaned = " ".join(cleaned.split())
    return cleaned[:60] if cleaned else ""


def message(milestone: str, group: list[dict]) -> str:
    first = group[0]
    area = area_of(first["path"])
    sample = clean_sample(first.get("sample", ""))
    base = Path(first["path"]).name
    if first["op"] == "delete":
        core = f"removes {base}"
    elif first["op"] == "whole":
        core = f"updates {base}"
    elif len({a['path'] for a in group}) > 1:
        core = f"extends {area} ({len(group)} pieces)"
    elif sample and sample.lower() not in (base.lower(), area):
        core = f"{area}: {sample}"
    else:
        core = f"adds {base}"
    return f"{milestone}: {core}"[:100]


def plan(repo: Path, base: str, source: str, slots: list) -> list[dict]:
    shas = milestone_shas(repo, base, source)
    parents = [base] + shas[:-1]
    atom_lists = [build_atoms(repo, parent, sha) for parent, sha in zip(parents, shas)]
    weights = [sum(a["weight"] for a in atoms) for atoms in atom_lists]
    floors = [min(50, len(atoms)) for atoms in atom_lists]
    counts = allocate_slots(weights, len(slots), floors)
    # Capacity clamp: no milestone may take more slots than it has atoms.
    caps = [len(atoms) for atoms in atom_lists]
    surplus = 0
    for i in range(len(counts)):
        if counts[i] > caps[i]:
            surplus += counts[i] - caps[i]
            counts[i] = caps[i]
    assert sum(caps) >= len(slots), "atom capacity exhausted"
    i = 0
    while surplus > 0:
        if counts[i % len(counts)] < caps[i % len(counts)]:
            counts[i % len(counts)] += 1
            surplus -= 1
        i += 1
    assert sum(counts) == len(slots)
    milestones = []
    offset = 0
    for i, sha in enumerate(shas):
        groups = group_atoms(atom_lists[i], counts[i])
        milestones.append({"label": MILESTONES[i], "sha": sha, "tag": TAGS[i],
                           "slots": slots[offset:offset + counts[i]], "groups": groups})
        offset += counts[i]
    return milestones


def execute(repo: Path, base: str, milestones: list[dict]) -> None:
    repo = canon(repo)
    if repo == WORKSPACE:
        raise SystemExit("refusing to replay inside the main workspace")
    author = git(repo, "log", "-1", "--format=%an", base)
    email = git(repo, "log", "-1", "--format=%ae", base)
    git(repo, "checkout", "-b", "history-replay", base)
    for milestone in milestones:
        for slot, group in zip(milestone["slots"], milestone["groups"]):
            for path in {a["path"] for a in group if a["op"] == "delete"}:
                target = repo / path
                if target.exists():
                    target.unlink()
            for path in {a["path"] for a in group if a["op"] != "delete"}:
                latest = [a for a in group if a["path"] == path][-1]
                target = repo / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(latest["content"], encoding="utf-8", newline="")
            git(repo, "add", "-A")
            stamp = slot["date"]
            env = {"GIT_AUTHOR_NAME": author, "GIT_AUTHOR_EMAIL": email,
                   "GIT_COMMITTER_NAME": author, "GIT_COMMITTER_EMAIL": email,
                   "GIT_AUTHOR_DATE": stamp, "GIT_COMMITTER_DATE": stamp}
            msg = message(milestone["label"], group)
            git(repo, "commit", "--allow-empty", "-m", msg, env=env)
        tree_check = git(repo, "diff", milestone["sha"], "HEAD", "--stat")
        assert tree_check == "", f"{milestone['label']} tree mismatch:\n{tree_check}"
        git(repo, "tag", milestone["tag"])
        print(f"{milestone['label']} done: {len(milestone['groups'])} commits, "
              f"tag {milestone['tag']}, tree == {milestone['sha'][:7]}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--plan", action="store_true")
    parser.add_argument("--repo", type=Path, default=None)
    parser.add_argument("--source", default="dev")
    parser.add_argument("--base", default="main")
    parser.add_argument("--schedule", type=Path, default=HERE / "schedule.json")
    args = parser.parse_args()
    slots = json.loads(args.schedule.read_text(encoding="utf-8"))
    if args.plan or args.repo is None:
        milestones = plan(WORKSPACE, args.base, args.source, slots)
        for milestone in milestones:
            sample = message(milestone["label"], milestone["groups"][0])
            print(f"{milestone['label']}: {len(milestone['groups'])} commits, "
                  f"first slot {milestone['slots'][0]['date']}, e.g. {sample!r}")
        print(f"total={sum(len(m['groups']) for m in milestones)} slots={len(slots)}")
        return
    milestones = plan(args.repo, args.base, args.source, slots)
    execute(args.repo, args.base, milestones)
    print("REPLAY OK")


if __name__ == "__main__":
    sys.exit(main())
