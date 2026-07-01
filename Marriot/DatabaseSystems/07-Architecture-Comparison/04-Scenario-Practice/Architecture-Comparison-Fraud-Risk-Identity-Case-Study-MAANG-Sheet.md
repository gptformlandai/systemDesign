# Architecture Comparison Fraud, Risk, and Identity Case Study - MAANG Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: fraud/risk/security interviews | Level: MAANG | Mode: graph, streaming, vector, features

## 1. Workloads

- ingest risk events
- detect shared devices/cards/emails
- query identity graph
- retrieve similar risky profiles
- compute features for models
- investigator search and audit

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| risk events | Kafka plus Cassandra/object storage | high-volume append and replay |
| account state | SQL/MongoDB | profile and workflow correctness |
| identity relationships | Neo4j/graph DB | shared devices and fraud rings |
| similarity search | Vector DB | similar behavior/profile retrieval |
| investigation search | Elasticsearch | text/field search over cases/events |
| model features | feature store/warehouse | training and online/offline parity |

---

## 3. Production Risks

- false positives
- stale identity graph
- privacy/compliance violations
- hot risk entities
- model drift
- investigator explanation gaps

---

## 4. Strong Interview Answer

```text
For fraud and identity, I would combine event streams, source account data, a graph projection for relationship traversal, search for investigation, vector similarity for behavioral neighbors, and warehouse/feature-store pipelines for model features. Vector and graph outputs should be risk signals, not final proof. False positives, freshness, privacy, audit, and explainability are central.
```

---

## 5. Revision Notes

- One-line summary: Fraud architecture is event-driven, graph-aware, search-friendly, and model-supported.
- Three keywords: graph, similarity, audit.
- One trap: using a black-box similarity score as the only fraud reason.