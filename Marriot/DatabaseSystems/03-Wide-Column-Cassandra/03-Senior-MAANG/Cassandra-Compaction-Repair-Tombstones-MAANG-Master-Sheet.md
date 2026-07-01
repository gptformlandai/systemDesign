# Cassandra Compaction, Repair, and Tombstones - MAANG Master Sheet

> Track File #12 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: storage maintenance, deletes, repair, operational risk

This sheet builds:
- Compaction strategy selection
- Tombstone and TTL failure modes
- Anti-entropy repair and production runbook thinking

---

## 1. Why Compaction Exists

Cassandra writes immutable SSTables. Compaction merges SSTables, drops overwritten data when safe, and reduces read amplification.

```text
many SSTables + old versions + tombstones -> compaction -> fewer cleaner SSTables
```

Cost:

- disk IO
- CPU
- temporary disk space
- p99 latency impact if overloaded

---

## 2. Compaction Strategies

| Strategy | Best Fit | Watch Out |
|---|---|---|
| SizeTieredCompactionStrategy | write-heavy general workloads | read amplification, disk spikes |
| LeveledCompactionStrategy | read-heavy workloads needing predictable reads | higher write amplification |
| TimeWindowCompactionStrategy | time-series with TTL/windowed writes | late-arriving writes and wrong windows |

Interview rule:

```text
Choose compaction from workload shape: write-heavy, read-heavy, or time-windowed TTL data.
```

---

## 3. Tombstones

Deletes and TTL expiry create tombstones. Cassandra must keep tombstones long enough to prevent deleted data from coming back from stale replicas.

Tombstone sources:

- DELETE statements
- TTL expiry
- overwrites of collection elements
- range deletes

Tombstone risk:

```text
Reads scan live data plus tombstones, so tombstone-heavy partitions can cause p99 spikes and failures.
```

---

## 4. gc_grace_seconds

`gc_grace_seconds` controls how long tombstones are retained before eligible removal.

Why it matters:

- if removed too early, deleted data can reappear from unrepaired replicas
- if too high for delete-heavy TTL workloads, tombstone pressure grows

Senior answer:

```text
I would tune gc_grace_seconds only with a repair strategy and failure assumptions, not as a random tombstone cleanup knob.
```

---

## 5. Repair

Repair reconciles data between replicas.

Repair matters for:

- anti-entropy correctness
- tombstone safety
- long-lived replica divergence
- replacing failed nodes safely

Operational cautions:

- repairs consume network/disk/CPU
- large repairs can destabilize clusters
- incremental/full repair strategy depends on version and tooling
- monitor repair age and failures

---

## 6. Tombstone Storm Runbook

Symptoms:

- p99 read latency spikes
- tombstone warnings
- read timeouts
- high compaction backlog
- disk IO saturation

Response:

1. Identify table and query.
2. Check partition size and tombstone metrics.
3. Inspect TTL/delete pattern.
4. Reduce hot reads or route around affected partitions.
5. Review compaction strategy and repair safety.
6. Redesign table if delete-heavy access pattern is structurally bad.

---

## 7. Strong Answer

Question:

> Why are tombstones dangerous in Cassandra?

Strong answer:

```text
Tombstones represent deletes or expired TTL data. Cassandra must keep them for a grace period so deletes can be propagated to replicas. Reads may need to scan tombstones across SSTables, which increases read amplification and p99 latency. Tombstones are especially dangerous in wide partitions, TTL-heavy workloads, and range deletes. The fix is usually modeling and retention discipline, not just lowering gc_grace_seconds.
```

---

## 8. Revision Notes

- One-line summary: Compaction cleans SSTables; tombstones and repair decide delete safety and read cost.
- Three keywords: STCS, TWCS, tombstone.
- One interview trap: lowering gc_grace_seconds without repair guarantees.
- Memory trick: deletes are writes that keep haunting reads until compaction can safely remove them.