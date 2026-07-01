# Cassandra Microservices and Event-Driven Production Patterns - Gold Sheet

> Track File #17 of 25 - Group 04: Scenario Practice
> For: backend/database/system design interviews | Level: senior | Mode: service ownership, events, idempotency, production patterns

This sheet builds:
- Cassandra in microservice architectures
- Event ingestion and read-model patterns
- Idempotency and consistency guardrails

---

## 1. Good Microservice Fits

Cassandra fits services that own high-volume, predictable access patterns:

- notification delivery history
- audit log service
- session/token store
- event read model service
- activity feed timeline
- IoT ingestion service
- chat message history
- metrics/write-heavy observability service

It is less suitable for services dominated by relational constraints, joins, and evolving ad hoc queries.

---

## 2. Database Per Service

If a service owns Cassandra tables, it should also own:

- table schema and migrations
- access patterns
- consistency levels
- retention and TTL
- repair/backup expectations with platform team
- write idempotency
- downstream event contracts

Do not share Cassandra tables casually across services.

---

## 3. Event Read Model Pattern

```text
source service event
-> Kafka/topic/outbox
-> consumer writes Cassandra query table
-> API reads Cassandra by primary-key-shaped query
```

Use when:

- read path must be fast and predictable
- source of truth lives elsewhere
- eventual consistency is acceptable

Need:

- idempotent writes
- replay handling
- dead-letter/retry path
- schema versioning
- reconciliation job

---

## 4. Idempotent Write Pattern

Use deterministic primary keys:

```sql
CREATE TABLE processed_events_by_consumer (
  consumer_name text,
  event_id text,
  processed_at timestamp,
  PRIMARY KEY (consumer_name, event_id)
);
```

Then write read models with stable keys so retries do not duplicate facts.

---

## 5. Outbox And Cassandra

Cassandra can store event read models, but if a relational system is the source of truth, the outbox often belongs next to that relational write.

Interview maturity:

```text
I would not force Cassandra to be the transaction boundary for a relational source-of-truth service. I would use outbox/CDC from the source and populate Cassandra as a denormalized read model.
```

---

## 6. Strong Answer

Question:

> How would Cassandra fit in microservices?

Strong answer:

```text
Cassandra fits a microservice when the service has high-volume predictable queries, such as notification history or audit events. I would design tables around API access patterns, use idempotent writes from events, document consistency levels, and handle replay/reconciliation. I would avoid sharing tables across services and avoid using Cassandra for relational transaction workflows that need joins or strict cross-entity constraints.
```

---

## 7. Revision Notes

- One-line summary: Cassandra works well as a service-owned high-scale query/read model store.
- Three keywords: read model, idempotency, replay.
- One interview trap: treating Cassandra as the source-of-truth transaction engine for every service.
- Memory trick: event in, query table out.