    # MongoDB Anti-Patterns, Internals and Debugging - MAANG Sheet

    > **Track File #23 of 28 - Group 05: Special Interview Rounds**
    > For: backend/database/system design interviews | Level: advanced interview/debugging | Mode: what breaks, why it breaks, how to fix it

    This sheet builds:
    - MongoDB anti-patterns and fixes
- WiredTiger, journaling, checkpoints, cache
- Read/write/replication/sharding paths

Original master-map sections included here:
- 24. MongoDB Anti-Patterns
- 25. MongoDB Internals

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 24. MongoDB Anti-Patterns

| Anti-Pattern | Why Bad | Fix |
|---|---|---|
| Using MongoDB like SQL | Too many collections and joins | Model aggregates around reads |
| Huge unbounded arrays | 16 MB limit and slow updates | Child collection or bucket pattern |
| Missing indexes | Collection scans | Index hot filters and sorts |
| Over-indexing | Slow writes, memory pressure | Keep indexes tied to queries |
| Deep `skip` pagination | Scans skipped records | Cursor pagination |
| Large documents near limit | Fragile updates and network cost | Split data |
| Too many `$lookup` joins | Latency/memory blowups | Embed summaries or read models |
| Storing unrelated data together | Bad locality and document bloat | Separate aggregates |
| Bad shard key | Hotspots and scatter-gather | Choose high-cardinality query-aligned key |
| Hot partitions | One shard receives most writes | Add distribution component/buckets |
| Excessive transactions | Latency/conflicts | Redesign aggregate or saga |
| Ignoring write concern | Data loss on failover | Use majority for critical writes |
| Ignoring schema validation | Inconsistent data | Add validators and app schema checks |
| Arbitrary user queries | Slow or abusive queries | Query allowlist and limits |
| Not monitoring slow queries | Production surprises | Profiler, alerts, explain review |
| Heavy relational reporting | Complex and slow | Use warehouse/Postgres/read models |

Examples:

Bad unbounded comments:

```javascript
{ _id: "post1", comments: [/* grows forever */] }
```

Better:

```javascript
{ _id: "post1", commentCount: 1200, recentComments: [] }
{ _id: "c1", postId: "post1", text: "...", createdAt: ISODate("...") }
```

Bad dynamic query endpoint:

```javascript
// User can send any filter to database
app.post("/search", (req, res) => collection.find(req.body).toArray())
```

Better:

- allowlist fields
- cap limits
- require tenant filter
- reject regex without prefix
- enforce max time
- log query shapes

---

---

## 25. MongoDB Internals

### WiredTiger Storage Engine

WiredTiger is MongoDB's default storage engine. It provides document storage, indexes, compression, caching, journaling integration, and concurrency control.

### Document Storage

Documents are stored as BSON. Collections and indexes are persisted in storage files managed by the engine.

Document updates may be in-place if size allows, or may require moving/rewriting internal records depending on storage behavior.

### Journaling

Journaling records changes so MongoDB can recover after crash. Durability depends on write concern, journaling, and replication settings.

### Checkpoints

WiredTiger periodically creates checkpoints: durable consistent points on disk.

Crash recovery uses journal plus checkpoint state.

### Cache Behavior

WiredTiger uses an internal cache. Performance suffers when working set and indexes do not fit memory and disk I/O becomes dominant.

Watch:

- cache eviction pressure
- dirty bytes
- page faults / disk reads
- index size vs memory

### Compression

MongoDB can compress collection and index data, reducing storage at CPU cost. Compression is usually beneficial for document workloads.

### Locking Model

Modern MongoDB uses fine-grained concurrency control. Still, operations can contend on documents, indexes, collection metadata, and storage resources.

High contention example: many writers updating one counter document.

Fix: shard counters, bucket writes, or use event aggregation.

### Oplog

The oplog is a capped collection of operations used by replica set secondaries and change streams.

Oplog window matters: if a secondary is down longer than the oplog retains history, it may need resync.

### Query Planner

The query planner evaluates candidate indexes and plans, chooses a winning plan, and may cache it. Data distribution changes can affect plan quality.

### B-Tree Concept

MongoDB indexes are conceptually B-tree-like sorted structures. They support equality, range scans, and ordered traversal.

### Write Path

Simplified:

1. Driver sends write to primary.
2. MongoDB validates command and document.
3. Storage engine updates collection and indexes.
4. Operation is written to oplog for replication.
5. Journal/checkpoint durability occurs according to settings.
6. Write concern determines when client gets acknowledgment.
7. Secondaries replicate from oplog.

### Read Path

Simplified:

1. Driver sends query to selected node.
2. Query planner chooses plan.
3. Index scan or collection scan finds candidates.
4. Documents are fetched if needed.
5. Filter/projection/sort/aggregation stages run.
6. Results stream back to client cursor.

### Replication Path

1. Primary writes operation to oplog.
2. Secondary pulls/applies oplog entries.
3. Secondary acknowledges progress.
4. Majority commit point advances when majority has operation.

### Sharding Path

1. Client sends query to `mongos`.
2. `mongos` checks metadata from config servers.
3. If shard key target known, routes to specific shard.
4. Otherwise broadcasts to multiple shards.
5. Merges results and returns to client.

---

---
