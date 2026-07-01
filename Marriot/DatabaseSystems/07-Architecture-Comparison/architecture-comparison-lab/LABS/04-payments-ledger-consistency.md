# Lab 04: Payments Ledger Consistency

Goal: rehearse datastore choices for money movement.

---

## Scenario

```text
A customer retries checkout three times during a payment processor timeout.
```

---

## Required Design Points

- relational transactional source of truth
- idempotency key
- append-only ledger entries
- payment state machine
- reconciliation job
- derived search/analytics only for reads

---

## Completion Gate

- You can explain why strong consistency matters.
- You can explain idempotency.
- You can explain why cache/search are not canonical money state.