#!/bin/bash
# slo-burn-calculator.sh
# Calculate SLO error budget and burn rate from the command line.
# Usage: ./slo-burn-calculator.sh <slo_percent> <window_days> [consumed_minutes] [burn_rate]

set -euo pipefail

SLO_PERCENT="${1:-99.9}"
WINDOW_DAYS="${2:-30}"
CONSUMED_MIN="${3:-}"
BURN_RATE="${4:-}"

echo "=== SLO Burn Rate Calculator ==="
echo ""

# Calculate total window in minutes.
WINDOW_MIN=$(echo "$WINDOW_DAYS * 24 * 60" | bc)

# Calculate error budget.
ERROR_RATE=$(echo "scale=6; 1 - ($SLO_PERCENT / 100)" | bc)
ERROR_BUDGET_MIN=$(echo "scale=3; $WINDOW_MIN * $ERROR_RATE" | bc)

echo "SLO Target:       ${SLO_PERCENT}%"
echo "Window:           ${WINDOW_DAYS} days (${WINDOW_MIN} minutes)"
echo "Error budget:     ${ERROR_BUDGET_MIN} minutes"
echo ""

if [ -n "$CONSUMED_MIN" ]; then
  REMAINING=$(echo "scale=2; $ERROR_BUDGET_MIN - $CONSUMED_MIN" | bc)
  PCT_CONSUMED=$(echo "scale=1; ($CONSUMED_MIN / $ERROR_BUDGET_MIN) * 100" | bc)
  echo "Consumed:         ${CONSUMED_MIN} minutes (${PCT_CONSUMED}%)"
  echo "Remaining:        ${REMAINING} minutes"
  echo ""
fi

if [ -n "$BURN_RATE" ]; then
  BUDGET_FOR_CALC="${CONSUMED_MIN:-$ERROR_BUDGET_MIN}"
  if [ -n "$CONSUMED_MIN" ]; then
    REMAINING_FOR_CALC=$(echo "scale=2; $ERROR_BUDGET_MIN - $CONSUMED_MIN" | bc)
  else
    REMAINING_FOR_CALC="$ERROR_BUDGET_MIN"
  fi
  HOURS_TO_EXHAUST=$(echo "scale=1; ($REMAINING_FOR_CALC / $BURN_RATE / ($ERROR_BUDGET_MIN / $WINDOW_MIN / 60))" | bc 2>/dev/null || echo "N/A")
  echo "Current burn rate:  ${BURN_RATE}x"
  echo "At this rate, remaining budget exhausted in: approx ${HOURS_TO_EXHAUST} hours"
  echo ""
fi

echo "--- Burn Rate Thresholds (Google SRE multi-window) ---"
echo "Fast burn (page now):     > 14.4x (budget gone in 2 days)"
echo "Slow burn (ticket):       > 6x    (budget gone in 5 days)"
echo "Crawling burn (low-pri):  > 3x    (budget gone in 10 days)"
echo ""

echo "--- Common SLO Error Budgets ---"
for target in 99.0 99.5 99.9 99.95 99.99; do
  rate=$(echo "scale=6; 1 - ($target / 100)" | bc)
  budget=$(echo "scale=1; 43200 * $rate" | bc)
  echo "  ${target}% -> ${budget} min/month"
done
