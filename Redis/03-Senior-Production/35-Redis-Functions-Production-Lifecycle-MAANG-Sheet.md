# 35. Redis Functions: Production Lifecycle, FCALL, And Script Governance

## Goal

Move from ad-hoc Lua scripts to production-governed server-side logic. Redis Functions are persistent, named, introspectable libraries that can replace fragile EVAL/EVALSHA deployment patterns.

```text
Lua script -> function library -> versioned rollout -> ACL -> observability -> rollback
```

---

## 1. EVAL vs Functions

| Capability | EVAL/EVALSHA | Redis Functions |
|---|---|---|
| deployment | sent by client or loaded into script cache | loaded as named library |
| persistence | script cache is not an application deployment system | functions survive restarts |
| invocation | EVAL or EVALSHA | FCALL or FCALL_RO |
| discoverability | SCRIPT EXISTS by SHA | FUNCTION LIST |
| lifecycle | client-owned | server-owned |
| governance | harder to audit | easier to version and review |

Use EVAL for quick experiments and small app-owned scripts. Prefer Functions when the logic is shared, long-lived, or production-critical.

---

## 2. Function Library Example

Library source:

```lua
#!lua name=rate_limiter_v1

redis.register_function('allow_fixed_window', function(keys, args)
  local key = keys[1]
  local limit = tonumber(args[1])
  local ttl_seconds = tonumber(args[2])

  local count = redis.call('INCR', key)
  if count == 1 then
    redis.call('EXPIRE', key, ttl_seconds)
  end

  if count <= limit then
    return {1, count}
  end

  return {0, count}
end)
```

Load:

```bash
FUNCTION LOAD "$(cat rate_limiter_v1.lua)"
```

Call:

```bash
FCALL allow_fixed_window 1 rate:user:1001:minute 100 60
```

Return convention:

```text
{allowed, current_count}
```

---

## 3. Read-Only Functions

Use read-only functions when logic only reads data:

```bash
FCALL_RO get_profile_summary 1 user:1001
```

Why it matters:

- communicates intent
- safer with read-only replicas where supported
- easier ACL reasoning
- reduces accidental writes in helper logic

Rule:

```text
If the function mutates data, use FCALL. If it only reads, use FCALL_RO.
```

---

## 4. Function Design Rules

| Rule | Why |
|---|---|
| pass all keys in KEYS | Cluster routing and command correctness |
| pass values/options in ARGV | separates key names from parameters |
| keep runtime short | Redis command execution is serialized |
| no unbounded loops over large keys | avoids blocking every client |
| return structured results | clients can handle success/failure cleanly |
| version library names | safe rollout and rollback |
| avoid hidden cross-key access | breaks Cluster and makes code hard to audit |

Bad:

```lua
-- Key is built inside the function from args only.
local key = 'user:' .. args[1]
return redis.call('GET', key)
```

Better:

```lua
-- Key is passed explicitly.
return redis.call('GET', keys[1])
```

---

## 5. Cluster Considerations

Redis Cluster requires keys used by a function call to belong to the same slot.

Good:

```bash
FCALL transfer_points 2 acct:{1001}:points acct:{1001}:ledger 50
```

Bad:

```bash
FCALL transfer_points 2 acct:1001:points acct:2002:points 50
# CROSSSLOT risk if keys hash to different slots.
```

Use hash tags when a function must touch multiple keys atomically:

```text
order:{5001}:state
order:{5001}:events
order:{5001}:lock
```

---

## 6. Deployment Lifecycle

Recommended lifecycle:

1. Store function library source in git.
2. Code review it like application code.
3. Unit test against local Redis.
4. Load into staging Redis.
5. Run correctness tests and latency tests.
6. Load new version into production under a versioned library/name.
7. Shift clients to call new function.
8. Monitor errors and latency.
9. Delete old version only after rollback window expires.

Avoid in-place replacement as the first move. Versioned rollout is safer:

```text
allow_fixed_window_v1 -> allow_fixed_window_v2
```

---

## 7. Rollback Strategy

Safe rollback:

```text
Keep old function loaded. Switch clients back to old function name.
```

Risky rollback:

```text
Delete old library before proving the new one under production load.
```

Function rollback must consider data shape:

- Did the new function write new keys?
- Did it change value format?
- Did it change TTL behavior?
- Did it change idempotency semantics?
- Can old code read new data?

---

## 8. Observability

Monitor:

```bash
FUNCTION LIST
INFO commandstats
INFO latencystats
SLOWLOG GET 25
LATENCY LATEST
```

Application metrics:

- function call count
- function error count
- timeout count
- p50/p95/p99 latency
- return-code distribution
- fallback count

Common function incident:

```text
A function loops over a large collection and blocks Redis. SLOWLOG shows FCALL. The fix is to bound work, chunk with SCAN-style iteration in the app, or redesign data structures.
```

---

## 9. Security And ACL

Production controls:

- only admin/deploy role can load/delete functions
- app role can call approved functions
- functions should access only key patterns the app owns
- avoid functions that wrap dangerous commands
- review ACL logs after deployment

Ask:

```text
If this function is abused, what keys can it read, write, or delete?
```

---

## 10. When Not To Use Functions

Avoid Functions when:

- logic is long-running
- logic requires external network calls
- logic needs heavy CPU or complex parsing
- logic crosses many Cluster slots
- application code is clearer and fast enough
- you need independent deploys from Redis operations

Use Functions when:

- atomic read-modify-write matters
- multiple apps share the same logic
- script cache misses are causing fragility
- a small server-side decision saves many round trips
- the function can be bounded and reviewed

---

## 11. Interview Scenario

> Your application uses 12 Lua scripts loaded by each service on startup. During deploys, some calls fail with NOSCRIPT. How would you fix it?

Strong answer:

```text
I would first make the client resilient by falling back from EVALSHA to SCRIPT LOAD plus retry. Then I would move stable shared scripts into Redis Functions. Functions are named, persisted, and discoverable, so deployment is not tied to every client process warming the script cache. I would version function names, load the new library in staging, canary production callers, monitor FCALL latency/errors, and keep the old function loaded for rollback.
```

---

## 12. Revision Notes

- One-line summary: Redis Functions turn Lua from client-managed snippets into governed server-side libraries.
- Three keywords: FCALL, versioning, bounded.
- One interview trap: treating Functions as a place for heavy business logic.
- One memory trick: Functions are for atomic short decisions, not background jobs.

---

## 13. Official Source Notes

- Redis Functions introduction: <https://redis.io/docs/latest/develop/programmability/functions-intro/>
- Redis Lua scripting docs: <https://redis.io/docs/latest/develop/programmability/eval-intro/>
- Redis programmability docs: <https://redis.io/docs/latest/develop/programmability/>
