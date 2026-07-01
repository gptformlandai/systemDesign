#!/bin/bash
# provider-lock-update.sh
# Updates .terraform.lock.hcl for all common platforms.
# Run this after changing provider version constraints.
# Commit the updated lock file to git.

set -euo pipefail

TERRAFORM="/usr/bin/terraform"
if [[ ! -x "$TERRAFORM" ]]; then
  TERRAFORM="$(command -v terraform 2>/dev/null || true)"
fi

if [[ -z "$TERRAFORM" ]]; then
  echo "ERROR: terraform not found" >&2
  exit 1
fi

echo "=== Provider Lock File Update ==="
echo "Terraform: $TERRAFORM"
echo ""

# Initialize to ensure providers are downloaded
echo "Running terraform init..."
"$TERRAFORM" init -upgrade -no-color

echo ""
echo "Updating lock file for all platforms..."

"$TERRAFORM" providers lock \
  -platform=linux_amd64 \
  -platform=linux_arm64 \
  -platform=darwin_amd64 \
  -platform=darwin_arm64 \
  -platform=windows_amd64

echo ""
echo "Lock file updated: .terraform.lock.hcl"
echo ""
echo "Next steps:"
echo "  1. Review the changes: git diff .terraform.lock.hcl"
echo "  2. Commit: git add .terraform.lock.hcl && git commit -m 'chore: update provider lock file'"
