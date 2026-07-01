    # MongoDB Interview Prep Q&A - MAANG Sheet

    > **Track File #24 of 28 - Group 05: Special Interview Rounds**
    > For: backend/database/system design interviews | Level: beginner through MAANG interview | Mode: questions, strong answers, tradeoffs, interviewer insight

    This sheet builds:
    - Beginner, intermediate, advanced, and MAANG questions
- Concise answers with examples and tradeoffs
- System design prompts

Original master-map sections included here:
- 28. MongoDB Interview Prep

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 28. MongoDB Interview Prep

### Beginner Questions

| Question | Answer | Example | Tradeoff | Interviewer Insight |
|---|---|---|---|---|
| What is MongoDB? | A document database storing BSON documents. | User profile document. | Flexible schema needs discipline. | They want more than "NoSQL". |
| What is a collection? | Group of documents. | `users` collection. | No enforced table schema by default. | Know SQL mapping. |
| What is BSON? | Binary JSON-like format with richer types. | Date, ObjectId. | Not human JSON exactly. | Types matter for indexes. |
| What is ObjectId? | Common unique `_id` value with timestamp-like component. | `_id: ObjectId(...)`. | Leaks creation time. | Good for distributed ID generation. |
| What is embedding? | Storing related data inside same document. | Order with line items. | Can bloat if unbounded. | Tie to atomicity and read locality. |

### Intermediate Questions

| Question | Strong Answer |
|---|---|
| Embedding vs referencing? | Embed bounded data read/updated together; reference large, shared, independently changing data. |
| How do indexes work? | They reduce scans by storing sorted keys, but add write/storage/memory cost. |
| What is aggregation? | Pipeline for transforming/grouping/joining documents. `$match` early, watch memory. |
| What is a replica set? | Primary plus secondaries with oplog replication and elections. |
| What are transactions? | ACID multi-document operations via sessions; use when schema cannot make it single-document. |
| What are change streams? | Resumable database change feed built on oplog. |

### Advanced Questions

#### How do you choose a shard key?

Concise answer: choose a high-cardinality, evenly distributed, stable key that appears in common queries and avoids write hotspots.

Example: for orders, `{ tenantId, orderId }` targets tenant/order lookups. For time-series events, avoid timestamp-only and consider device or tenant plus bucket.

Tradeoff: hashed keys distribute writes but hurt range locality. Range keys support range queries but can hotspot.

Interviewer insight: they expect you to mention query isolation, distribution, cardinality, and hotspots.

#### How do you debug a slow query?

Answer:

1. Capture exact query shape.
2. Run `explain("executionStats")`.
3. Check `IXSCAN` vs `COLLSCAN`.
4. Compare keys/docs examined to returned.
5. Validate compound index order with ESR.
6. Check sort stage and projection.
7. Consider schema redesign or preaggregation.

Example:

```javascript
db.orders.find({ tenantId: "t1", status: "PAID" }).sort({ createdAt: -1 }).explain("executionStats")
```

Tradeoff: adding an index helps reads but slows writes.

#### What is replication lag and why does it matter?

Replication lag is delay between primary write and secondary application. It causes stale secondary reads and can affect failover readiness.

Fixes: improve secondary resources, reduce write bursts, review indexes, monitor oplog window, avoid heavy operations on secondaries.

#### How do read concern and write concern differ?

Read concern controls what data a read can observe. Write concern controls when a write is acknowledged. Majority write concern improves durability across failover; majority read concern avoids reading data that may roll back.

#### MongoDB vs PostgreSQL?

MongoDB is better for aggregate-oriented, JSON-like, flexible operational data. PostgreSQL is better for relational integrity, complex joins, and SQL reporting.

### MAANG-Level Questions

| Prompt | Answer Shape |
|---|---|
| Design scalable chat storage | Message collection, conversation index, cursor pagination, shard by conversation/bucket, read receipts separate, handle hot groups |
| Design audit logging | Append-only logs, tenant/time indexes, TTL/archive, majority writes for critical audit, immutable model |
| Design global catalog | Product docs, Atlas Search, regional replication/sharding, cache hot products, eventual search consistency |
| Design multi-region SaaS | Tenant-aware shard/zone, data residency, majority writes tradeoff, regional reads, failover plan |
| Choose shard key under traffic | Discuss cardinality, distribution, query targeting, write hotspots, tenant skew |
| Debug slow query | Explain plan, indexes, cardinality, sort, schema redesign, precompute |
| Fix bad schema | Identify unbounded arrays/joins, propose embed/reference/subset/bucket pattern |
| Migrate SQL model to MongoDB | Start from access patterns, aggregate roots, duplicate stable fields, preserve transactions where needed |
| Design RAG memory store | Chunks, embeddings, metadata, ACL filters, vector index, re-embedding, conversation memory |

---

---
