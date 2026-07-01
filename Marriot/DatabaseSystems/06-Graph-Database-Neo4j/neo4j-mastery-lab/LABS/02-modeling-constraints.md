# Lab 02: Modeling And Constraints

Goal: understand identity, constraints, and idempotent writes.

---

## Run

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/01-schema.cypher
```

---

## Exercise

Inspect constraints in Browser or cypher-shell:

```cypher
SHOW CONSTRAINTS;
```

Explain:

- why `User.userId` is unique
- why `Account.accountId` is unique
- why duplicate `Entity` nodes poison a knowledge graph
- why `MERGE` needs stable IDs

---

## Completion Gate

- You can explain constraints vs indexes.
- You can explain why identity quality matters.
- You can describe idempotent graph import.