# GraphQL Observability: Tracing, Metrics, Errors, SLOs - Gold Sheet

> Track File #14 of 30 - Group 03: Senior Production
> For: production operations | Level: senior | Mode: observability

## 1. Core Idea

GraphQL observability must explain operation-level and resolver-level behavior.

```text
operation name/hash -> validation -> resolver spans -> data-source calls -> errors -> SLO impact
```

---

## 2. Signals

| Signal | Why It Matters |
|---|---|
| operation name/hash | identify workload |
| query complexity/depth | estimate cost |
| resolver latency | find slow fields |
| resolver error rate | isolate broken fields |
| data-source call count | detect N+1 |
| cache hit rate | evaluate client/server cache |
| null/error path | understand client impact |

---

## 3. Error Classification

- validation errors
- authentication errors
- authorization errors
- domain/business errors
- upstream dependency errors
- timeout/deadline errors
- internal server errors

Use stable error codes in `extensions` where appropriate.

---

## 4. SLO View

GraphQL SLOs should consider:

- operation latency by operation name/hash
- critical operation success rate
- partial error rate
- slow-field budget
- downstream dependency health

---

## 5. Interview Summary

```text
For GraphQL observability, I track operation names/hashes, validation failures, resolver spans, data-source fanout, error paths/codes, cache behavior, and SLOs for critical operations rather than only endpoint-level HTTP metrics.
```

---

## 6. Revision Notes

- One-line summary: `/graphql` latency is not enough; observe operation and resolver paths.
- Three keywords: operation hash, resolver span, error path.
- One trap: aggregating every GraphQL request under one endpoint metric and losing the real slow operation.