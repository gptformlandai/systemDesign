# Lab 02: Ecommerce Decision Matrix

Goal: practice marketplace datastore choices.

---

## Run

```bash
bash SCRIPTS/01-score-ecommerce-choice.sh
```

---

## Explain Out Loud

```text
Why should orders/payments not live only in Elasticsearch or Redis?
```

Strong answer:

```text
Orders and payments need transactional correctness, constraints, audit, idempotency, and reconciliation. Elasticsearch and Redis are useful derived read systems, not canonical money state.
```

---

## Completion Gate

- You can split source and derived stores.
- You can explain search freshness.
- You can name inventory consistency risk.