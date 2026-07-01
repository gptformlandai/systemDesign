# 08. Redis Transactions: MULTI/EXEC/WATCH, Pipelining

## Goal

Use Redis transactions and pipelining to atomically combine commands and batch network round-trips.

---

## MULTI/EXEC

MULTI starts a transaction block. Commands are queued and executed atomically when EXEC is called.

```bash
MULTI
INCR orders:count
INCR revenue:daily:2026-07-01
LPUSH recent:orders "order:5001"
EXEC
```

Behavior:

- all queued commands execute in order
- no other client can interleave commands between MULTI and EXEC
- if the connection drops before EXEC, the transaction is discarded
- syntax errors during queuing cause the whole transaction to be discarded
- runtime errors in one command do not abort the rest

The last point is important: Redis does not roll back on runtime errors in EXEC.

---

## DISCARD

Cancel a pending transaction.

```bash
MULTI
SET key1 val1
DISCARD
```

---

## WATCH: Optimistic Locking

WATCH marks keys to observe. If any watched key changes before EXEC, the transaction is aborted.

```bash
WATCH inventory:item:5001

count = GET inventory:item:5001

MULTI
SET inventory:item:5001 (count - 1)
EXEC
# Returns nil if watched key changed -> retry
```

This is optimistic locking: read-compare-write without holding a lock. Retry on conflict.

Use WATCH when:

- multiple clients may update the same key concurrently
- you want check-and-set semantics
- contention is expected to be low

Avoid WATCH under high contention: retries accumulate and throughput drops. Use Lua scripts for higher-contention atomic operations.

---

## Pipelining

Pipelining batches multiple commands in a single network round-trip. It is not a transaction.

```text
without pipeline: send cmd1 -> wait for reply -> send cmd2 -> wait -> ...
with pipeline: send cmd1, cmd2, cmd3 -> wait once -> get all replies
```

Pipelining is a client-side optimization and does not guarantee atomicity.

Use pipelining for:

- bulk reads or writes where atomicity is not required
- reducing network latency in high-latency environments
- batch initialization and loading

---

## MULTI vs Lua vs Pipeline

| Tool | Atomic | Can Read Mid-Transaction | Network RTTs |
|---|---|---|---|
| MULTI/EXEC | yes | no | 2 (MULTI + EXEC) |
| Lua (EVAL) | yes | yes | 1 |
| pipeline | no | no | 1 (batch) |
| WATCH+MULTI | conditional | before WATCH only | 3+ with retry |

Use Lua scripts when you need to read a value and make a decision inside an atomic operation.

---

## Interview Sound Bite

Redis transactions with MULTI/EXEC execute atomically but do not roll back on runtime errors. WATCH provides optimistic locking for check-and-set patterns. Pipelining reduces round-trips but does not provide atomicity. Lua scripting is the preferred tool when atomic read-compute-write is needed.
