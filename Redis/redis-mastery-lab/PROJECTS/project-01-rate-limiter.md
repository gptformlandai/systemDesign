# Project 01: Rate Limiter Service

## Objective

Build a reusable rate-limiting middleware backed by Redis using a sliding window algorithm.

## Requirements

- Per-user, per-action rate limiting
- Sliding window via sorted set + Lua script
- Response headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
- Configurable limit and window per route
- Graceful degradation: when Redis is down, fail open (allow requests)

## Key Redis Patterns Used

- Sorted set: `ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`, `PEXPIRE`
- Lua `EVAL` for atomicity
- Key pattern: `rate:{action}:{userId}`

## Implementation Notes

Lua script receives KEYS[1], ARGV[1] (limit), ARGV[2] (window_ms), ARGV[3] (now_ms).

Load script at startup with `SCRIPT LOAD` and use `EVALSHA` in hot path.

On Redis connection error: log warning, increment a bypass metric, return allowed response.

## Test Scenarios

1. Send 5 requests within 10-second window with limit=3. Verify first 3 allowed, last 2 denied.
2. Wait 11 seconds. Verify next request is allowed (window expired).
3. Stop Redis. Verify service returns allowed responses (fail open).
4. Restart Redis. Verify rate limiting resumes.

## Interview Value

Demonstrates: Lua atomicity, sorted-set sliding window, fail-open design, EVALSHA optimization.
