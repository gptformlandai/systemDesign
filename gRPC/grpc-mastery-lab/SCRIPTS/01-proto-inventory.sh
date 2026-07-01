#!/usr/bin/env bash
set -euo pipefail

root="${1:-.}"

printf 'gRPC proto inventory for: %s\n' "$root"
printf '\nProto files:\n'
/usr/bin/find "$root" -type f -name '*.proto' -print | /usr/bin/sort

printf '\nPackages:\n'
/usr/bin/find "$root" -type f -name '*.proto' -print | while IFS= read -r file; do
  /usr/bin/awk -v f="$file" '/^package[[:space:]]+/ { gsub(";", "", $2); print f ": " $2 }' "$file"
done

printf '\nServices:\n'
/usr/bin/find "$root" -type f -name '*.proto' -print | while IFS= read -r file; do
  /usr/bin/awk -v f="$file" '/^service[[:space:]]+/ { print f ": " $2 }' "$file"
done