# GraphQL Performance, Complexity, Depth, Persisted Queries - MAANG Sheet

> Track File #12 of 30 - Group 03: Senior Production
> For: performance and abuse prevention | Level: senior | Mode: query cost control

## 1. Core Idea

GraphQL lets clients choose field shape, so production systems need cost controls.

```text
operation shape -> resolver fanout -> data-source cost -> latency/load -> guardrails
```

---

## 2. Performance Controls

| Control | Purpose |
|---|---|
| DataLoader | batch and request-cache data loads |
| max depth | prevent deeply nested operations |
| complexity scoring | estimate field cost |
| max page size | avoid unbounded list fields |
| persisted queries | allow known operations and reduce payload |
| timeout/deadline | protect servers and upstreams |
| resolver tracing | identify slow fields |

---

## 3. Persisted Queries

Persisted queries store operation text by hash or registry ID.

Benefits:

- smaller requests
- operation allowlisting
- easier caching
- less attack surface from arbitrary queries
- operation-level telemetry

---

## 4. Failure Modes

- nested query causes resolver explosion
- query fetches huge unbounded lists
- expensive field hidden inside common operation
- DataLoader missing or scoped incorrectly
- persisted query registry out of sync with clients

---

## 5. Interview Summary

```text
For GraphQL performance, I combine DataLoader, bounded lists, complexity/depth limits, timeouts, persisted queries, resolver tracing, and operation telemetry. I optimize based on resolver and data-source evidence, not just HTTP latency.
```

---

## 6. Revision Notes

- One-line summary: GraphQL performance is controlled by operation cost and resolver fanout.
- Three keywords: complexity, DataLoader, persisted queries.
- One trap: adding GraphQL without operation limits and allowing arbitrary expensive queries.