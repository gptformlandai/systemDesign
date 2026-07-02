# Spring Boot Data Access Beyond JPA, JdbcClient, jOOQ, R2DBC, And NoSQL Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: choose the right data access style instead of forcing every problem through JPA.

---

## 1. Intuition

JPA is a powerful automatic transmission. It is great for domain entities and normal CRUD,
but sometimes you need manual control: raw SQL for performance, jOOQ for type-safe SQL,
R2DBC for reactive I/O, Redis for fast key-value access, MongoDB for document aggregates,
or Elasticsearch for search.

---

## 2. Definition

- Definition: Data access beyond JPA means selecting Spring-supported persistence tools
  based on query shape, consistency needs, latency, scalability, and operational fit.
- Category: persistence architecture.
- Core idea: model the data access problem first, then choose the abstraction.

---

## 3. Why It Exists

JPA is not always the best fit because:

- complex reports can produce ugly JPQL
- high-volume batch writes may need direct JDBC
- read-heavy projections may be better as SQL views or query models
- reactive stacks need non-blocking drivers end to end
- search is not a relational database problem
- cache/session/state access often belongs in Redis
- document aggregates may not need relational joins

---

## 4. Reality

Common Spring Boot data stack choices:

| Need | Good Fit |
|---|---|
| Domain CRUD with relationships | Spring Data JPA |
| Simple direct SQL | JdbcTemplate or JdbcClient |
| Complex type-safe SQL | jOOQ |
| Reactive relational I/O | Spring Data R2DBC |
| Key-value/cache/rate state | Redis |
| Flexible document aggregate | MongoDB |
| Search, autocomplete, relevance | Elasticsearch/OpenSearch |
| Graph relationships | Neo4j |
| Very high write-scale wide rows | Cassandra |

---

## 5. How It Works

Decision flow:

1. Identify the consistency invariant.
2. Identify access pattern: point lookup, join, search, aggregate, stream, report, graph.
3. Identify concurrency model: blocking MVC, virtual threads, or reactive.
4. Choose the storage system and Spring abstraction.
5. Define transaction boundaries.
6. Add migrations or schema management where relevant.
7. Test with the real engine through Testcontainers.
8. Observe latency, pool usage, query plans, and error rates.

Failure path:

- choose JPA for analytics report -> slow query and entity explosion
- choose R2DBC with blocking libraries -> event loop starvation
- choose MongoDB but need cross-document ACID joins -> complex consistency
- choose Redis as source of truth -> data loss risk if not configured carefully

Recovery path:

- move report to JdbcClient/jOOQ projection
- isolate blocking calls on bounded scheduler or return to MVC
- add outbox/projection for search or document views
- put true invariants in the database that owns the write model

---

## 6. What Problem It Solves

- Primary problem solved: mismatch between persistence abstraction and workload.
- Secondary benefits: better performance, clearer SQL, cleaner read models, safer scaling.
- Systems impact: fewer hidden ORM traps and better ownership of data correctness.

---

## 7. When To Rely On It

Use non-JPA tools when:

- queries are SQL-first and projection-heavy
- batch operations need predictable JDBC behavior
- you need type-safe SQL generation
- the full app is reactive and drivers are non-blocking
- data shape is document/search/cache/graph oriented
- the interviewer asks when JPA is not enough

---

## 8. When Not To Use It

Do not abandon JPA just because it has learning curve:

- normal CRUD with transactions and relationships is often excellent with JPA
- direct SQL everywhere can duplicate mapping logic
- R2DBC without reactive discipline is worse than MVC
- NoSQL does not remove data modeling; it changes the trade-offs
- polyglot persistence adds operational cost

Use one database and one abstraction until the workload proves otherwise.

---

## 9. Pros And Cons

| Option | Pros | Cons |
|---|---|---|
| JPA | rich domain mapping, transactions, repositories | N+1, hidden SQL, flush/lazy traps |
| JdbcClient | explicit SQL, simple, fast | manual mapping, less domain abstraction |
| jOOQ | type-safe SQL, great for complex queries | code generation and licensing choices |
| R2DBC | non-blocking relational I/O | fewer ecosystem assumptions, reactive complexity |
| Redis | very low latency | memory cost, eviction/consistency risks |
| MongoDB | document aggregate flexibility | join/reporting limitations |
| Elasticsearch | search relevance | eventual consistency, not source of truth |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- JPA optimizes developer productivity for domain data.
- JDBC/jOOQ optimize predictability and SQL control.
- R2DBC optimizes non-blocking concurrency if the whole stack cooperates.
- NoSQL optimizes specific access patterns while giving up relational strengths.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Using JPA entities as report DTOs | Loads too much state | Use projection query |
| Using R2DBC with JDBC driver calls | Blocks event loop | Keep reactive end to end |
| Treating Elasticsearch as source of truth | Index can be stale | Use DB as source, index as projection |
| Putting every lookup in Redis | Cache invalidation becomes business logic | Cache only clear hot paths |
| Multiple databases for a simple app | Operational overkill | Start with relational DB |
| No Testcontainers for database-specific SQL | H2 misses real behavior | Test against real engine |

---

## 11. Key Numbers

Approximate reasoning numbers:

- Most OLTP service queries should target low tens of milliseconds at the database layer.
- N+1 can turn 1 query into 101 queries for 100 rows.
- Redis point lookups are commonly sub-millisecond to low-millisecond inside a region.
- Search indexes are often eventually consistent by seconds, depending on pipeline.
- Database pool size is usually much smaller than request concurrency.
- Reactive stacks do not remove database capacity limits.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| Slow endpoint | p99 spike | ORM query explosion | Fetch plan/projection/query plan |
| Stale search result | User sees old data | Async index lag | Show eventual consistency or sync critical reads |
| Pool exhaustion | Requests timeout | Too many blocking DB calls | Tune query, pool, concurrency |
| Event loop blocked | Reactive app stalls | Blocking library in reactive path | Remove/block isolate |
| Duplicate write | Data inconsistency | Missing unique constraint | DB constraint plus idempotency |
| Cache wrong data | Bad business result | Weak key/invalidation | Include tenant/version and invalidate carefully |

---

## 13. Scenario

- Product/system: hotel booking platform.
- Why this concept fits: booking writes use JPA and constraints, price search may use
  Elasticsearch, rate cache may use Redis, reporting may use jOOQ/JdbcClient.
- What would go wrong without it: JPA would be forced into search, reporting, and cache
  problems where it is not the best abstraction.

---

## 14. Code Sample

JdbcClient projection:

```java
package com.example.booking.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class BookingReportRepository {

    private final JdbcClient jdbc;

    BookingReportRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    BookingRevenue revenueFor(LocalDate day) {
        return jdbc.sql("""
                select count(*) as booking_count, coalesce(sum(total_amount), 0) as revenue
                from bookings
                where check_in_date = :day
                """)
            .param("day", day)
            .query((rs, rowNum) -> new BookingRevenue(
                rs.getLong("booking_count"),
                rs.getBigDecimal("revenue")))
            .single();
    }

    record BookingRevenue(long bookingCount, BigDecimal revenue) {
    }
}
```

---

## 15. Mini Program / Simulation

```python
def choose_data_access(workload):
    if workload == "domain-crud":
        return "Spring Data JPA"
    if workload == "complex-sql-report":
        return "jOOQ or JdbcClient"
    if workload == "reactive-relational":
        return "Spring Data R2DBC"
    if workload == "search":
        return "Elasticsearch/OpenSearch projection"
    if workload == "hot-key-cache":
        return "Redis"
    return "Start with JPA and prove the need to change"


for item in ["domain-crud", "complex-sql-report", "search", "unknown"]:
    print(item, "=>", choose_data_access(item))
```

---

## 16. Practical Question

> You are designing hotel search and booking. Which data access technologies would you
> choose for booking writes, price lookup, search, and reporting?

---

## 17. Strong Answer

I would keep booking writes in a relational database with JPA because the booking invariant
needs transactions, constraints, and locking. For reporting or complex SQL projections I
would use JdbcClient or jOOQ instead of loading entity graphs. For hot price lookups I may
use Redis as a cache, with keys that include hotel, dates, guests, currency, and tenant.
For text search and filtering at scale I would use Elasticsearch/OpenSearch as a projection,
not the source of truth. I would test each store with Testcontainers, observe query latency
and pool metrics, and document where eventual consistency is acceptable.

---

## 18. Revision Notes

- One-line summary: JPA is a default for domain CRUD, not a universal data access answer.
- Three keywords: workload, invariant, projection.
- One interview trap: reactive database access only helps if the full request path is
  non-blocking and the database is still sized correctly.
- One memory trick: entities for writes, SQL for reports, cache for hot keys, search for search.

