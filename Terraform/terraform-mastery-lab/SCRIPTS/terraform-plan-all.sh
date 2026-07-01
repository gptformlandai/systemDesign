#!/bin/bash
# terraform-plan-all.sh
# Runs terraform plan in all environment directories under a given root.
# Usage: ./terraform-plan-all.sh [base_path]
# Example: ./terraform-plan-all.sh ./environments

set -euo pipefail

BASE_PATH="${1:-./environments}"
PLAN_EXIT_CODES=()
PLAN_DIRS=()
TERRAFORM="/usr/bin/terraform"

if [[ ! -x "$TERRAFORM" ]]; then
  TERRAFORM="$(command -v terraform 2>/dev/null || true)"
fi

if [[ -z "$TERRAFORM" ]]; then
  echo "ERROR: terraform not found on PATH or at /usr/bin/terraform" >&2
  exit 1
fi

echo "=== Terraform Plan All ==="
echo "Base path: $BASE_PATH"
echo "Terraform: $TERRAFORM"
echo ""

# Find all directories containing a versions.tf or main.tf (root modules)
while IFS= read -r -d '' dir; do
  dir_path="$(dirname "$dir")"
  
  echo "--- Planning: $dir_path ---"
  PLAN_DIRS+=("$dir_path")
  
  pushd "$dir_path" > /dev/null
  
  "$TERRAFORM" init -input=false -no-color 2>&1 | tail -5
  
  set +e
  "$TERRAFORM" plan \
    -input=false \
    -no-color \
    -detailed-exitcode \
    -out="${dir_path//\//-}.tfplan" \
    2>&1
  exit_code=$?
  set -e
  
  PLAN_EXIT_CODES+=("$exit_code")
  
  case $exit_code in
    0) echo "  [OK] No changes" ;;
    1) echo "  [ERROR] Plan failed" ;;
    2) echo "  [CHANGES] Changes present" ;;
  esac
  
  popd > /dev/null
  echo ""

done < <(/usr/bin/find "$BASE_PATH" -name "versions.tf" -print0 2>/dev/null)

# Summary
echo "=== Summary ==="
for i in "${!PLAN_DIRS[@]}"; do
  dir="${PLAN_DIRS[$i]}"
  code="${PLAN_EXIT_CODES[$i]}"
  case $code in
    0) label="NO CHANGES" ;;
    1) label="ERROR      " ;;
    2) label="CHANGES    " ;;
    *) label="UNKNOWN    " ;;
  esac
  echo "  $label  $dir"
done

# Exit non-zero if any plan errored (exit code 1)
for code in "${PLAN_EXIT_CODES[@]}"; do
  if [[ "$code" -eq 1 ]]; then
    echo ""
    echo "One or more plans failed. Review output above."
    exit 1
  fi
done

echo ""
echo "All plans completed."
