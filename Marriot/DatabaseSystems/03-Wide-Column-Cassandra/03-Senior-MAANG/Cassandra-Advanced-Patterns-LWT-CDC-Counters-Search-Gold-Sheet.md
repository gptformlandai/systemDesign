# Cassandra Advanced Patterns: LWT, CDC, Counters, and Search - Gold Sheet

> Track File #15 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: advanced features, caveats, alternatives

This sheet builds:
- When to use Lightweight Transactions
- CDC, counters, and search integration caveats
- MAANG-level feature boundary judgment

---

## 1. Lightweight Transactions

LWT uses Paxos-style coordination for compare-and-set semantics.

Example:

```sql
INSERT INTO user_by_email (email, user_id)
VALUES ('asha@example.com', 'u1')
IF NOT EXISTS;
```

Use when:

- uniqueness must be enforced
- compare-and-set is needed
- contention is low
- latency cost is acceptable

Avoid when:

- high-throughput hot path
- heavily contended key
- using it to compensate for bad modeling

---

## 2. Counters

Cassandra counters are specialized and have operational caveats.

Use carefully for:

- approximate or operational counters where semantics are acceptable

Prefer alternatives when:

- exact financial/accounting counters are needed
- idempotent retries are difficult
- counters are highly contended

Alternative:

```text
Write events to Cassandra/Kafka, aggregate with stream processing, store materialized counts separately.
```

---

## 3. CDC

Change Data Capture can expose mutations for downstream processing, but implementation and operational behavior are version/platform-specific.

Use CDC for:

- event pipelines
- audit/outbox-like propagation
- downstream cache/search updates

Watch:

- disk pressure
- consumer lag
- ordering expectations
- replay/idempotency
- platform support and tooling maturity

---

## 4. Search Integration

Cassandra is not a full-text search engine.

For search use:

- Elasticsearch/OpenSearch
- Solr-based platforms
- managed search integrations

Pattern:

```text
Cassandra stores source/query tables.
Search index serves text/filter search.
Events/CDC/outbox keep search index updated.
```

---

## 5. Guardrails

Use advanced features only after naming:

- version/platform support
- failure mode
- latency impact
- operational monitoring
- fallback/rebuild plan
- simpler alternative rejected

---

## 6. Strong Answer

Question:

> When would you use LWT in Cassandra?

Strong answer:

```text
I would use LWT for low-contention compare-and-set cases such as claiming a unique email or idempotency key. I would avoid it for high-throughput hot paths because it requires extra coordination and increases latency. If the business invariant can be handled by deterministic IDs, idempotent writes, or a different system with stronger transaction semantics, I would prefer that.
```

---

## 7. Revision Notes

- One-line summary: Cassandra advanced features are useful but must be bounded by latency, correctness, and operations.
- Three keywords: LWT, CDC, counters.
- One interview trap: using LWT as a general transaction replacement.
- Memory trick: advanced feature = name the price before the benefit.