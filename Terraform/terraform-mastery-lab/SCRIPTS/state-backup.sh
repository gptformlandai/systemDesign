#!/bin/bash
# state-backup.sh
# Pull Terraform state from a remote backend and save a timestamped backup.
# Usage: ./state-backup.sh [output_dir]
# Run from within a Terraform configuration directory.

set -euo pipefail

OUTPUT_DIR="${1:-./state-backups}"
TIMESTAMP="$(/bin/date +%Y%m%d-%H%M%S)"
BACKUP_FILE="${OUTPUT_DIR}/state-backup-${TIMESTAMP}.json"

TERRAFORM="/usr/bin/terraform"
if [[ ! -x "$TERRAFORM" ]]; then
  TERRAFORM="$(command -v terraform 2>/dev/null || true)"
fi

if [[ -z "$TERRAFORM" ]]; then
  echo "ERROR: terraform not found" >&2
  exit 1
fi

# Verify this is a Terraform directory
if [[ ! -f "versions.tf" && ! -f "main.tf" ]]; then
  echo "ERROR: Run this script from a Terraform configuration directory" >&2
  exit 1
fi

# Create output directory
/bin/mkdir -p "$OUTPUT_DIR"

echo "=== Terraform State Backup ==="
echo "Pulling state..."

"$TERRAFORM" state pull > "$BACKUP_FILE"

# Validate the backup is valid JSON
if /usr/bin/python3 -m json.tool "$BACKUP_FILE" > /dev/null 2>&1; then
  RESOURCE_COUNT="$(/usr/bin/python3 -c "
import json, sys
with open('$BACKUP_FILE') as f:
    state = json.load(f)
print(len(state.get('resources', [])))
")"
  echo "Backup saved: $BACKUP_FILE"
  echo "Resources in backup: $RESOURCE_COUNT"
else
  echo "ERROR: State backup is not valid JSON. Something went wrong." >&2
  /bin/rm -f "$BACKUP_FILE"
  exit 1
fi

# Keep only the last 10 backups
BACKUP_COUNT="$(/usr/bin/find "$OUTPUT_DIR" -name 'state-backup-*.json' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
if [[ "$BACKUP_COUNT" -gt 10 ]]; then
  echo "Removing old backups (keeping last 10)..."
  /usr/bin/find "$OUTPUT_DIR" -name 'state-backup-*.json' \
    | /usr/bin/sort \
    | head -n $(( BACKUP_COUNT - 10 )) \
    | xargs /bin/rm -f
fi

echo "Done. Backup count in ${OUTPUT_DIR}: $(/usr/bin/find "$OUTPUT_DIR" -name 'state-backup-*.json' | /usr/bin/wc -l | /usr/bin/tr -d ' ')"
