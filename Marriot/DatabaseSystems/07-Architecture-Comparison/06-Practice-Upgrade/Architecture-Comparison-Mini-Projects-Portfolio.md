# Architecture Comparison Mini Projects Portfolio

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: portfolio and interview discussion | Level: beginner to pro | Mode: project specs

Each project should include requirements, source of truth, derived stores, data sync, failure modes, SLOs, cost, security, and alternatives.

---

## 1. Ecommerce Data Architecture

Build a design for:

- orders and payments
- product catalog
- search
- recommendations
- analytics

Discuss:

- transactional correctness
- cache invalidation
- search freshness
- warehouse pipeline

---

## 2. Enterprise RAG Data Architecture

Build a design for:

- source documents
- metadata and permissions
- search index
- vector index
- optional knowledge graph
- evaluation logs

Discuss:

- ACL propagation
- citations
- stale embeddings
- derived-store rebuild

---

## 3. Chat And Feed Storage

Build a design for:

- conversations
- messages
- timelines
- unread counters
- notifications
- search

Discuss:

- fanout strategy
- hot users
- ordering
- cache drift

---

## 4. Fraud And Identity Platform

Build a design for:

- event stream
- account source data
- identity graph
- similarity search
- investigator search
- analytics/features

Discuss:

- false positives
- privacy
- graph freshness
- model drift

---

## 5. Observability Data Platform

Build a design for:

- logs
- metrics
- traces
- alerts
- cold archive
- dashboards

Discuss:

- retention cost
- cardinality
- hot shards
- SLO alert reliability

---

## Portfolio Scoring

| Area | What To Prove |
|---|---|
| requirement | product and correctness needs are clear |
| access pattern | query shapes are named |
| source of truth | canonical owner is explicit |
| derived stores | cache/search/vector/graph/analytics are justified |
| sync | CDC/events/rebuild path exists |
| operations | SLOs, DR, security, cost covered |
| alternatives | rejected options are explained |

MAANG-ready portfolio:

```text
At least 4 projects can be explained end-to-end in 10 minutes each with follow-up answers.
```