# 03 - Data CAP Sharding Transactions

> Goal: choose the right database, consistency model, replication, sharding, transaction pattern, and derived store for each part of a request lifecycle.

---

## 1. Intuition

Data architecture is not "SQL vs NoSQL."

It is:

```text
What promise must this data keep under concurrency, traffic, failure, distance, and time?
```

Examples:

- Product search can be stale.
- Product images can be immutable.
- Inventory display can be approximate.
- Inventory reservation must be controlled.
- Payment state must be idempotent and auditable.
- Ledger entries must be strictly correct.
- Analytics can be delayed.

Beginner line:

```text
Pick databases by access pattern and correctness requirement, not by trend. Strong consistency is
expensive but necessary for money and critical invariants. Eventual consistency is powerful when
staleness is acceptable and reconciliation exists.
```

---

## 2. CAP And PACELC

CAP says that during a network partition, a distributed system must choose how it behaves:

| Choice | Meaning |
|---|---|
| Consistency | reject/delay operations rather than return possibly stale/conflicting data |
| Availability | keep accepting operations even if replicas may diverge |
| Partition tolerance | unavoidable in distributed systems |

PACELC extends the thinking:

```text
If Partition happens: choose Availability or Consistency.
Else during normal operation: choose Latency or Consistency.
```

Interview correction:

```text
CAP is not a database label you memorize. It is a failure-mode decision per operation and topology.
```

---

## 3. Consistency Models

| Model | Meaning | Fit |
|---|---|---|
| strong consistency | latest committed value visible | ledger, reservation, permissions |
| linearizability | operations appear instant and globally ordered | locks, account transfer, leader writes |
| serializability | transactions equivalent to serial order | finance, complex invariants |
| snapshot isolation | reads stable snapshot | many OLTP systems |
| read-your-writes | user sees own updates | profile/cart/settings |
| monotonic reads | user does not go backward | feeds, dashboards |
| causal consistency | causally related writes seen in order | collaboration/social |
| eventual consistency | replicas converge eventually | search, recommendations, counters |
| bounded staleness | stale within known limit | globally replicated reads |

Wrong option:

```text
Use eventual consistency for permission checks because it scales better.
```

What fails:

```text
A revoked user may continue accessing sensitive data until replicas converge.
```

Better:

```text
Use strong or read-through authoritative permission checks for sensitive access, with cache invalidation
or very short TTL only if risk is acceptable.
```

---

## 4. Database Families

| Store | Examples | Best Fit | Weakness |
|---|---|---|---|
| relational OLTP | PostgreSQL, MySQL, Aurora | transactions, joins, orders, payments | horizontal write scale needs design |
| distributed SQL | Spanner, CockroachDB, YugabyteDB | SQL + distributed transactions | latency/operational complexity |
| key-value | DynamoDB, FoundationDB KV | high-scale key access | query flexibility |
| wide-column | Cassandra, ScyllaDB, Bigtable | high write/read scale by partition key | query-model constraints |
| document | MongoDB, Couchbase | aggregate documents, flexible schema | cross-document invariants harder |
| cache | Redis, Memcached | hot reads, counters, rate limits | not primary durable truth by default |
| search | Elasticsearch, OpenSearch, Solr | full-text/filter/search | eventual derived index |
| graph | Neo4j, Neptune | relationship traversal | partitioning/global scale complexity |
| object storage | S3, GCS | blobs, images, logs, exports | not OLTP query engine |
| warehouse/lakehouse | Snowflake, BigQuery, Redshift, Databricks | analytics | not request-path OLTP |
| time-series | M3, Prometheus, InfluxDB | metrics/telemetry | not business transactions |

Decision rule:

```text
Source of truth should optimize correctness and write model. Derived stores should optimize reads,
search, analytics, or caching.
```

---

## 5. Source Of Truth vs Derived Stores

Source of truth:

- orders table
- payment intent table
- ledger entries
- user account table
- inventory reservation table

Derived stores:

- Redis product cache
- Elasticsearch search index
- recommendation features
- analytics warehouse
- materialized read model
- CDN cached page

Wrong option:

```text
Let Elasticsearch be the source of truth for orders because it searches well.
```

What fails:

```text
Search indexes are eventually consistent, optimized for retrieval, and not ideal for transactional
order state, idempotency, or audit correctness.
```

Better:

```text
Use OLTP source of truth for orders and replicate/stream to Elasticsearch for search.
```

---

## 6. Replication

Replication types:

| Type | Use | Trade-off |
|---|---|---|
| leader-follower | common OLTP scaling | follower lag |
| multi-leader | multi-region writes | conflict resolution |
| leaderless/quorum | high availability | read/write quorum tuning |
| synchronous replication | low data loss | higher write latency |
| asynchronous replication | lower latency | possible data loss/lag |
| log shipping/CDC | derived systems | eventual propagation |

Read strategy:

| Strategy | Fit | Risk |
|---|---|---|
| read from leader | freshest | leader load/latency |
| read from follower | scale reads | stale reads |
| read quorum | tunable consistency | higher latency |
| session stickiness | read-your-writes | routing complexity |

Wrong option:

```text
After user updates bank transfer limit, immediately read from any async replica for authorization.
```

What fails:

```text
Replica lag may allow operations using stale limits.
```

Better:

```text
Use leader/strong read for security-critical authorization changes or enforce version checks.
```

---

## 7. Sharding And Partitioning

Sharding means splitting data across nodes.

Partition key choices:

| Key | Fit | Risk |
|---|---|---|
| userId | user-scoped data | celebrity/hot user |
| tenantId | SaaS tenant isolation | hot enterprise tenant |
| orderId hash | order lookups | cross-user queries need indexes |
| SKU/productId | inventory/catalog | flash sale hot key |
| region | data locality | cross-region workflows |
| time bucket | logs/events | hot current bucket |
| composite key | access-pattern fit | more complex |

Sharding strategies:

| Strategy | Meaning |
|---|---|
| range sharding | contiguous ranges |
| hash sharding | distribute by hash |
| consistent hashing | reduce remap during node changes |
| directory-based | lookup service maps key to shard |
| geo-sharding | keep data near region |
| tenant sharding | isolate tenants |

When sharding is required:

- single-node write capacity exceeded
- storage no longer fits
- read replicas are not enough
- hot domains need isolation
- regional data residency
- operational blast-radius reduction

When not required:

- MVP/small data
- vertical scale/read replicas enough
- complexity cost exceeds benefit
- queries need global joins and data volume is manageable

Wrong option:

```text
Shard early by random orderId before understanding access patterns.
```

What fails:

```text
User order history, merchant views, reconciliation, and support queries scatter across shards.
```

Better:

```text
Choose shard keys from dominant access patterns, capacity needs, and business boundaries.
```

---

## 8. Hot Partitions

Hot partition examples:

- one viral product during flash sale
- one tenant with massive traffic
- current timestamp bucket
- celebrity user profile
- global counter

Mitigations:

| Technique | Use |
|---|---|
| key salting | split hot key across buckets |
| write sharding | distribute writes, aggregate later |
| per-SKU queue | serialize hot inventory operations |
| local cache | reduce repeated reads |
| adaptive capacity | managed DB feature |
| admission control | reject excess load |
| precomputed counters | avoid write-per-read |

Wrong option:

```text
Use SKU as the only partition key for flash-sale inventory writes with no hot-key strategy.
```

What fails:

```text
The hottest product overloads one partition, causing throttling or high latency exactly when traffic peaks.
```

Better:

```text
Use reservation queues, conditional writes with admission control, or sharded counters with a final
strong reservation boundary.
```

---

## 9. Transactions And Isolation

ACID:

| Property | Meaning |
|---|---|
| Atomicity | all or nothing |
| Consistency | invariants preserved |
| Isolation | concurrent transactions behave safely |
| Durability | committed data survives failure |

Isolation levels:

| Level | Prevents | Still Allows |
|---|---|---|
| read committed | dirty reads | non-repeatable reads, phantoms |
| repeatable read | non-repeatable reads | some phantom/write-skew depending DB |
| serializable | most anomalies | higher contention/retries |

Concurrency controls:

| Technique | Use |
|---|---|
| row locks | serialize updates to specific rows |
| optimistic locking | version check on update |
| compare-and-swap | conditional write |
| unique constraint | prevent duplicates |
| serializable transaction | complex invariant |
| advisory lock | app-level mutual exclusion |

Wrong option:

```text
Check inventory count in one query, then update later without lock/version condition.
```

What fails:

```text
Two requests can both see available inventory and oversell.
```

Better:

```sql
UPDATE inventory
SET available = available - 1, reserved = reserved + 1
WHERE sku_id = $1 AND available > 0;
```

Then check affected row count.

---

## 10. Ledger Modeling For Finance

Finance source of truth should be immutable ledger entries.

Double-entry rule:

```text
Every movement has balanced debit and credit entries. Sum must equal zero for the transaction.
```

Minimal model:

```text
ledger_transaction
  id
  idempotency_key
  status
  created_at

ledger_entry
  id
  transaction_id
  account_id
  direction
  amount
  currency
  created_at
```

Rules:

- append-only entries
- no destructive updates
- idempotency unique constraint
- balanced transaction check
- audit every state transition
- reconcile with external systems

Wrong option:

```text
Store only current account balance and update it in place.
```

What fails:

```text
No audit trail, hard reconciliation, impossible to explain historical balance, and corruption may
be unrecoverable.
```

Better:

```text
Use immutable ledger entries and derive balance from ledger or from a transactionally maintained
balance projection.
```

---

## 11. SQL vs NoSQL For E-Commerce

E-commerce data split:

| Domain | Recommended Store | Why |
|---|---|---|
| product catalog source | relational/document | structured seller/product management |
| product search | Elasticsearch/OpenSearch | full-text/filter/ranking |
| product images | object storage + CDN | blob delivery |
| cart | key-value/document | user-scoped fast access |
| inventory reservation | relational or strongly consistent KV | conditional updates |
| orders | relational/distributed SQL | state machine, transactions |
| payments | relational/distributed SQL | idempotency, audit |
| events | Kafka/Pulsar | async propagation |
| analytics | warehouse/lake | reporting |

Wrong option:

```text
Use only Elasticsearch for product catalog, inventory, and checkout.
```

What fails:

```text
Search is good, but transactional updates, inventory correctness, and order/payment audit need a
stronger source-of-truth model.
```

Better:

```text
Use OLTP catalog/inventory/order stores and publish derived search documents to Elasticsearch.
```

---

## 12. SQL vs NoSQL For Finance

Finance data split:

| Domain | Recommended Store | Why |
|---|---|---|
| ledger | relational/distributed SQL | ACID, constraints, audit |
| idempotency | same transactional DB or strongly consistent KV | duplicate prevention |
| account metadata | relational | ownership/permissions |
| fraud features | streaming/feature store | async risk scoring |
| notifications | queue/event stream | post-commit side effects |
| analytics | warehouse/lake | reporting |
| logs/audit archive | object storage | retention |

Wrong option:

```text
Use eventually consistent Cassandra writes for the core ledger with last-write-wins conflict handling.
```

What fails:

```text
Concurrent updates/conflicts can lose financial truth. Last-write-wins is unacceptable for money.
```

Better:

```text
Use ACID ledger transactions or a database with proven strict consistency semantics for the ledger.
```

---

## 13. Multi-Region Data Choices

| Pattern | Fit | Trade-off |
|---|---|---|
| active-passive | finance writes in primary | failover time |
| active-active reads | global e-commerce browse | stale reads |
| active-active writes | local low latency writes | conflict complexity |
| home-region per user/account | locality with ownership | cross-region transfer complexity |
| globally consistent DB | strict semantics across regions | higher latency/cost |
| event replication | derived cross-region state | lag and replay handling |

E-commerce:

```text
Use active-active reads for catalog/search, regional caches, async replication, and a clear home
region or strong local boundary for checkout/order.
```

Finance:

```text
Prefer primary write region, account home region, or globally consistent database. Do not accept
conflicting writes for the same money/account without strict ordering.
```

---

## 14. CAP Choice - E-Commerce Availability

Availability-first paths:

- home page
- product catalog
- search
- recommendations
- reviews summary
- cart display with merge

Consistency-first paths inside commerce:

- inventory reservation
- payment authorization/capture
- order state transition
- refund state transition

Architect answer:

```text
I choose AP/eventual consistency for read-heavy discovery and derived data because stale product
information is tolerable for a short time and availability drives conversion. I choose stronger
consistency for checkout-critical writes because oversell, duplicate charge, and invalid orders are
business failures.
```

---

## 15. CAP Choice - Finance Strict Consistency

Consistency-first paths:

- ledger writes
- balance-affecting transfers
- account ownership/authorization
- transaction status
- limits and risk blocks

Availability approach:

- keep read-only account history available when safe
- allow "pending" states
- use degraded mode for non-critical features
- reject/delay money movement if strict correctness unavailable

Architect answer:

```text
For finance, I choose consistency over availability for money movement. If a partition prevents the
system from proving account state and writing balanced ledger entries, I would return pending or
reject rather than risk double spend or incorrect balances.
```

---

## 16. Interview Answer Template

```text
I classify data by correctness requirement. Public product discovery can use caches, search indexes,
replicas, and eventual consistency. Checkout inventory reservation needs conditional writes or a
transactional boundary. Payments need idempotent state machines and audit records. A finance ledger
needs immutable double-entry records in an ACID store. I shard only after access patterns and capacity
justify it, and I choose shard keys around user/account/order/SKU access patterns. Derived stores are
rebuilt from source-of-truth events, not treated as authoritative.
```

---

## 17. Revision Notes

- One-line summary: Correctness and access pattern choose the data store, not fashion.
- Three keywords: source-of-truth, shard key, isolation.
- One interview trap: one database for every access pattern or one NoSQL store for every correctness need.
- Memory trick: source of truth protects invariants; derived stores serve speed.

