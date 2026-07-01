# Cassandra Primary Keys, Indexes, and Materialized Views - Gold Sheet

> Track File #6 of 25 - Group 02: Intermediate Backend
> For: backend/database/system design interviews | Level: intermediate | Mode: key design, index cautions, query alternatives

This sheet builds:
- Correct primary-key reasoning
- Why secondary indexes are not a magic fix
- When materialized views, SAI, search, or extra tables are better or worse

---

## 1. Primary Key Rules

Primary key design controls data distribution and query support.

```sql
PRIMARY KEY ((tenant_id, bucket_day), created_at, event_id)
```

| Part | Query Rule |
|---|---|
| Partition key | Usually must be provided to target data efficiently |
| First clustering column | Can be restricted after partition key |
| Later clustering columns | Usually require earlier clustering restrictions or valid range order |

Example valid query:

```sql
SELECT *
FROM events_by_tenant_day
WHERE tenant_id = 't1'
  AND bucket_day = '2026-07-01'
  AND created_at >= '2026-07-01T10:00:00Z'
  AND created_at < '2026-07-01T11:00:00Z';
```

---

## 2. Secondary Indexes

Secondary indexes can help for low-volume or carefully selected queries, but they are dangerous for high-cardinality, high-volume, or broad cluster searches.

Bad fit:

```text
Find all orders by status = PAID across billions of rows.
```

Better fit:

```text
Small table, low-cardinality lookup, or operational query where latency is not a hot path.
```

Professional default:

```text
Prefer a query table over a secondary index for production hot paths.
```

---

## 3. SAI And SASI Caution

Storage-Attached Indexing can be useful on supported Cassandra/platform versions, but it is not a replacement for primary-key modeling.

Use SAI carefully for:

- narrower filtering inside a known partition strategy
- operationally acceptable lookup workloads
- platforms where version, limits, and monitoring are understood

Avoid using SAI as:

- a join replacement
- a general ad hoc analytics engine
- a way to skip access-pattern modeling

SASI is historically experimental/deprecated in many setups; mention it only with version/platform context.

---

## 4. Materialized Views

Materialized views maintain another table shape automatically, but production teams are cautious because operational behavior and edge cases can be subtle.

Safer default:

```text
Own denormalized tables in application/write pipeline unless your platform/version and operations team explicitly support materialized views for the use case.
```

Manual denormalization example:

```sql
CREATE TABLE user_by_email (
  email text PRIMARY KEY,
  user_id text,
  name text
);

CREATE TABLE user_by_id (
  user_id text PRIMARY KEY,
  email text,
  name text
);
```

Application writes both tables idempotently.

---

## 5. ALLOW FILTERING Decision

`ALLOW FILTERING` can be acceptable in tiny admin/debug cases, but it is usually a sign the table does not match the query.

Decision rule:

```text
If the query is user-facing, frequent, or large-scale, redesign the table.
If it is one-off admin/debug and bounded, document the risk.
```

---

## 6. Choosing The Right Alternative

| Need | Better Option |
|---|---|
| Query by another access pattern | New denormalized Cassandra table |
| Full-text search | Elasticsearch/OpenSearch/Solr/search platform |
| Analytics across huge data | Spark/Trino/OLAP/data lake |
| Strict uniqueness | LWT or external coordination, used sparingly |
| Many ad hoc filters | Different database or search/indexing system |

---

## 7. Strong Answer

Question:

> Why not just add indexes in Cassandra?

Strong answer:

```text
Cassandra indexes are not the same as relational indexes. The main performance model is still partition-key-based query routing. For production hot paths, I prefer a denormalized table shaped for that query because it gives predictable partition targeting and p99 behavior. I would consider secondary indexes or SAI only when the data size, selectivity, version support, and operational monitoring make the risk acceptable.
```

---

## 8. Revision Notes

- One-line summary: Primary-key modeling beats indexes for Cassandra hot paths.
- Three keywords: primary key, SAI, materialized view.
- One interview trap: treating indexes like PostgreSQL B-trees.
- Memory trick: if the index hides a bad partition strategy, the table is still bad.