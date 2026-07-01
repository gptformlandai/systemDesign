# Cassandra Application Development with Java, Spring, and Python - Gold Sheet

> Track File #8 of 25 - Group 02: Intermediate Backend
> For: backend/database/system design interviews | Level: intermediate | Mode: drivers, repositories, retries, paging, idempotency

This sheet builds:
- Practical driver usage principles
- Java/Spring/Python integration patterns
- Production-safe retry, paging, timeout, and idempotency thinking

---

## 1. Application Rules

- Reuse one driver session/client per process.
- Use prepared statements for hot paths.
- Bind parameters instead of string-concatenating CQL.
- Design repositories around query-shaped tables.
- Set timeouts, consistency levels, and idempotency intentionally.
- Page large result sets.
- Avoid driver retries for non-idempotent mutations unless explicitly safe.

---

## 2. Java Driver Shape

```java
CqlSession session = CqlSession.builder()
    .withKeyspace("app")
    .build();

PreparedStatement statement = session.prepare(
    "SELECT * FROM messages_by_room_day " +
    "WHERE room_id = ? AND bucket_day = ? LIMIT ?"
);

BoundStatement bound = statement.bind("room-1", LocalDate.parse("2026-07-01"), 50)
    .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM);

ResultSet rows = session.execute(bound);
```

Interview point:

```text
Prepared statements improve safety and performance, but they do not fix bad table design.
```

---

## 3. Spring Data Cassandra

Use Spring Data Cassandra for simple repository patterns, but keep schema design explicit.

Good for:

- mapping table rows to domain objects
- simple repositories
- integration with Spring Boot configuration

Be careful with:

- generated queries that do not match primary keys
- treating Cassandra repositories like JPA repositories
- hiding consistency and paging behavior

Interview trap:

```text
Do not say Cassandra is like JPA with a different driver. The data model is fundamentally query-shaped.
```

---

## 4. Python Driver Shape

```python
from cassandra.cluster import Cluster
from cassandra.query import ConsistencyLevel

cluster = Cluster(["127.0.0.1"])
session = cluster.connect("app")

statement = session.prepare("""
SELECT * FROM messages_by_room_day
WHERE room_id = ? AND bucket_day = ?
LIMIT ?
""")
statement.consistency_level = ConsistencyLevel.LOCAL_QUORUM

rows = session.execute(statement, ("room-1", "2026-07-01", 50))
for row in rows:
    print(row.message_ts, row.body)
```

---

## 5. Paging

Cassandra result paging avoids pulling too many rows at once.

Rules:

- page within bounded partitions
- do not expose raw paging state as a long-lived public contract without care
- combine paging with clustering-order queries
- avoid pagination that jumps arbitrarily across the cluster

---

## 6. Retries And Idempotency

Retries can create duplicate side effects if writes are not idempotent.

Safer patterns:

- deterministic IDs
- idempotency keys
- upserts with stable primary keys
- event deduplication tables
- compare-and-set only when required

Bad pattern:

```text
Retry a non-idempotent counter-like write blindly after timeout.
```

---

## 7. Strong Answer

Question:

> What makes Cassandra application integration different from relational integration?

Strong answer:

```text
The repository methods must mirror query-shaped tables. I use prepared statements, explicit consistency levels, bounded paging, and idempotent write design. I avoid JPA-style dynamic queries because Cassandra needs primary-key-shaped access. Driver retries and timeouts must be chosen carefully because a timeout does not always mean the write failed.
```

---

## 8. Revision Notes

- One-line summary: Cassandra app code must preserve query modeling, consistency, paging, and idempotency decisions.
- Three keywords: prepared statement, paging, idempotency.
- One interview trap: treating Spring Data Cassandra like JPA.
- Memory trick: repository method name should look like the table's access pattern.