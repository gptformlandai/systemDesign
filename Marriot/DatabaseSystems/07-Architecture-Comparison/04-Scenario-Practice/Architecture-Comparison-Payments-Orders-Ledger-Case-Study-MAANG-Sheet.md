# Architecture Comparison Payments, Orders, and Ledger Case Study - MAANG Sheet

> Track File #22 of 30 - Group 04: Scenario Practice
> For: payments/order system design interviews | Level: MAANG | Mode: consistency, ledger, audit

## 1. Workloads

- create order
- authorize payment
- capture/refund
- reserve inventory
- maintain ledger
- reconcile with external processor
- audit history

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| order state | SQL/PostgreSQL | transactional workflow and constraints |
| ledger | relational append-only ledger | auditability and consistency |
| idempotency keys | SQL/Redis with durable backing | duplicate request protection |
| events | Kafka/outbox | downstream notification and reconciliation |
| search/reporting | Elasticsearch/warehouse | derived views only |
| cache | Redis | read optimization, not canonical money state |

---

## 3. Production Risks

- double charge
- lost refund
- inconsistent order/payment state
- inventory oversell
- missing idempotency
- reconciliation drift
- mutable ledger entries

---

## 4. Strong Interview Answer

```text
For payments and ledgers, I would prefer a relational transactional source of truth with idempotency keys, strong constraints, append-only ledger entries, audit trails, and reconciliation jobs. Search, cache, and analytics are derived. I would not use an eventually consistent search, vector, graph, or cache system as the canonical money state.
```

---

## 5. Revision Notes

- One-line summary: Money workflows prioritize correctness and audit over trendy storage choices.
- Three keywords: transaction, ledger, idempotency.
- One trap: using cache or search as canonical payment state.