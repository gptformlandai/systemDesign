    # MongoDB vs SQL Deep Tradeoff Analysis - Gold Sheet

    > **Track File #22 of 28 - Group 04: Scenario Practice**
    > For: backend/database/system design interviews | Level: system design tradeoff readiness | Mode: database choice, normalization, reporting, hybrid architecture

    This sheet builds:
    - MongoDB vs SQL/PostgreSQL tradeoffs
- Normalization vs denormalization
- Hybrid architecture patterns

Original master-map sections included here:
- 23. MongoDB vs SQL: Deep Tradeoff Analysis

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 23. MongoDB vs SQL: Deep Tradeoff Analysis

### Normalization vs Denormalization

SQL normalization reduces duplication and preserves relational consistency. MongoDB denormalization improves read locality and developer ergonomics.

| Topic | SQL Bias | MongoDB Bias |
|---|---|---|
| Data shape | Normalized entities | Aggregates/documents |
| Joins | Runtime joins | Embed/reference/duplicate |
| Integrity | Foreign keys | Application/service invariants |
| Schema change | Migrations | Versioned documents/evolution |
| Read performance | Join/index tuning | Document locality/index tuning |
| Write complexity | Less duplication | Update duplicated copies |

### Joins vs Embedding/Referencing

SQL joins are powerful for flexible querying. MongoDB embedding is powerful for predictable aggregate reads.

MongoDB `$lookup` exists, but if most hot paths require multi-collection joins, reconsider the model or database choice.

### Schema Migrations vs Schema Evolution

MongoDB allows rolling schema evolution:

- app reads old and new fields
- new writes use new shape
- background backfill
- validator tightened later

SQL migrations provide stronger central shape but can be operationally heavier for large tables.

### ACID Expectations

PostgreSQL is excellent for multi-row, relational ACID workflows. MongoDB is excellent for single-document atomic aggregates and supports multi-document transactions when needed.

### OLTP vs OLAP

MongoDB is primarily OLTP/operational. For large analytics, export to OLAP systems or maintain preaggregates.

### Reporting Complexity

If business users need arbitrary SQL reports across many entities, MongoDB can become painful. Use PostgreSQL or a warehouse for reporting.

### Developer Velocity

MongoDB can speed development when app data is naturally JSON-like and access patterns are understood. It can slow teams down when schema discipline is absent.

### When PostgreSQL Is Better

- strict relational integrity
- complex joins across many entities
- ad hoc reporting
- mature SQL analytics
- financial ledger with strong constraints
- relational model is stable and central

### When MongoDB Is Better

- document/aggregate reads dominate
- schema varies by entity subtype
- product catalog/content/profile data
- high write ingestion with flexible metadata
- app-owned microservice database
- JSON-native developer workflow
- Atlas Search/vector integration useful

### Hybrid Architecture Patterns

| Pattern | Example |
|---|---|
| MongoDB operational + warehouse analytics | Orders in MongoDB, events to Snowflake |
| PostgreSQL core ledger + MongoDB profile/catalog | Money in Postgres, product data in MongoDB |
| MongoDB source + Elasticsearch search index | Product source in MongoDB, search in ES |
| MongoDB RAG metadata + object storage documents | Chunks in MongoDB, PDFs in S3 |
| Service-specific databases | Order service MongoDB, billing service Postgres |

---

---
