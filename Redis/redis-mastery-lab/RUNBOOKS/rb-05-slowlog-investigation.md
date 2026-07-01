# Runbook 05: Slowlog Investigation

## When To Use

- Application latency is elevated
- P99 Redis response times are above normal
- SLOWLOG LEN is growing

## Steps

1. Check current slowlog threshold.

```bash
redis-cli CONFIG GET slowlog-log-slower-than
# Default: 10000 (10ms in microseconds)
```

2. Get slowlog entries.

```bash
redis-cli SLOWLOG GET 25
```

3. Parse entries. Format per entry:
- ID
- Timestamp (unix)
- Duration (microseconds)
- Command + arguments

4. Group by command to find patterns.

```bash
redis-cli SLOWLOG GET 100 | grep -E "^\d+\)" | head -50
```

5. Common culprits and fixes.

| Command | Fix |
|---|---|
| KEYS | Replace with SCAN |
| SMEMBERS | Replace with SSCAN or restructure |
| HGETALL on large hash | Replace with HSCAN or targeted HGET |
| LRANGE 0 -1 | Add COUNT limit |
| Lua EVAL | Profile script, reduce iterations |
| SORT | Cache SORT results with STORE |

6. If Lua script is slow.

```bash
redis-cli SLOWLOG GET 10 | grep EVAL
# Inspect script body and optimize
```

7. Reset slowlog after investigation.

```bash
redis-cli SLOWLOG RESET
```

## Prevention

- Audit SLOWLOG after every load test
- Alert if SLOWLOG LEN > 50 since last reset
- Ban KEYS from application code (code review gate)
- Limit LRANGE/SMEMBERS/HGETALL results with COUNT bounds
