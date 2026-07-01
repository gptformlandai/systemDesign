#!/bin/bash
# 05-rate-limiter-template.sh
# Demonstrate sliding-window rate limiter using redis-cli EVAL.
# Usage: ./05-rate-limiter-template.sh [user_id] [limit] [window_seconds]

set -euo pipefail

USER_ID="${1:-user:1001}"
LIMIT="${2:-5}"
WINDOW_MS=$(( ${3:-10} * 1000 ))
KEY="rate:${USER_ID}:api"

LUA_SCRIPT='
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local window_start = now - window_ms
redis.call("ZREMRANGEBYSCORE", key, "-inf", window_start)
local count = redis.call("ZCARD", key)
if count < limit then
  redis.call("ZADD", key, now, now .. math.random())
  redis.call("PEXPIRE", key, window_ms)
  return 1
end
return 0
'

echo "Rate limiter test: key=${KEY} limit=${LIMIT} window=${WINDOW_MS}ms"
echo ""

for i in $(seq 1 8); do
  NOW=$(date +%s000)
  RESULT=$(redis-cli EVAL "${LUA_SCRIPT}" 1 "${KEY}" "${LIMIT}" "${WINDOW_MS}" "${NOW}")
  if [ "${RESULT}" = "1" ]; then
    echo "Request ${i}: ALLOWED"
  else
    echo "Request ${i}: DENIED (limit ${LIMIT} reached)"
  fi
done

echo ""
echo "Current window entries: $(redis-cli ZCARD "${KEY}")"
