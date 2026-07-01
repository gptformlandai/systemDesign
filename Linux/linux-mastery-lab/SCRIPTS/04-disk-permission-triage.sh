#!/usr/bin/env bash
set -euo pipefail

target=${1:-.}

printf '## Disk Usage\n'
df -h 2>/dev/null || true

printf '\n## Inode Usage\n'
df -ih 2>/dev/null || true

printf '\n## Largest Entries Under %s\n' "$target"
du -sh "$target"/* 2>/dev/null | sort -h | tail -20 || true

printf '\n## Path Permissions For %s\n' "$target"
namei -l "$target" 2>/dev/null || true

printf '\n## Stat For %s\n' "$target"
stat "$target" 2>/dev/null || true