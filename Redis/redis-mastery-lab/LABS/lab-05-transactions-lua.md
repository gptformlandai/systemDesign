# Lab 05: Transactions And Lua Scripting

## Objective

Practice MULTI/EXEC, WATCH optimistic locking, and Lua atomic scripts.

## Exercises

### Exercise 1: MULTI/EXEC Basic

```bash
MULTI
INCR orders:count
SET last:order:time "1720000000"
LPUSH recent:orders "order:5001"
EXEC
# Expected: [1, OK, 1]
```

### Exercise 2: DISCARD Cancels Transaction

```bash
MULTI
SET key1 "value1"
SET key2 "value2"
DISCARD
# Expected: OK (transaction cancelled)

EXISTS key1
# Expected: 0 (was never set)
```

### Exercise 3: WATCH Abort

```bash
# Terminal 1:
SET watched:stock 100
WATCH watched:stock

# Terminal 2 (simulate concurrent write):
SET watched:stock 50

# Terminal 1 continues:
MULTI
DECRBY watched:stock 10
EXEC
# Expected: (nil) — transaction aborted because watched:stock changed
```

### Exercise 4: WATCH Success

```bash
SET account:1001:balance 500
WATCH account:1001:balance
# No other client modifies it.
MULTI
DECRBY account:1001:balance 50
EXEC
# Expected: [450] — transaction succeeded
GET account:1001:balance
# Expected: "450"
```

### Exercise 5: Lua Conditional Set

```bash
EVAL "
  local current = tonumber(redis.call('GET', KEYS[1]))
  if current == nil then current = 0 end
  if current < tonumber(ARGV[1]) then
    return redis.call('INCR', KEYS[1])
  end
  return -1
" 1 counter:lab:limit 3
# Run 4 times. First 3 return 1,2,3. Fourth returns -1.
```

### Exercise 6: Lua Atomic Lock Release

```bash
# Acquire lock.
SET lock:lab:job "my-worker-uuid" NX PX 10000

# Release only if we own it.
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  end
  return 0
" 1 lock:lab:job "my-worker-uuid"
# Expected: 1 (deleted)

# Try again: should return 0 (already gone).
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  end
  return 0
" 1 lock:lab:job "my-worker-uuid"
# Expected: 0
```

## Reflection

- When does EXEC return nil?
- Why must lock release use Lua instead of GET + DEL?
- What happens if an error occurs inside a MULTI/EXEC block on one command?
