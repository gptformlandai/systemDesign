# Lab 06: Query Plan Debugging

Goal: practice EXPLAIN and PROFILE interpretation.

---

## Run

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/07-query-plan-debugging.cypher
```

---

## What To Observe

- indexed anchor queries should be selective
- PROFILE shows actual work for the query
- disconnected patterns can create Cartesian products

---

## Explain Out Loud

```text
What are rows and db hits telling you in a Neo4j profile?
```

---

## Completion Gate

- You can explain EXPLAIN vs PROFILE.
- You can identify a Cartesian product shape.
- You can name at least three query tuning fixes.