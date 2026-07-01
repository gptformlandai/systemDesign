# Lab 09: Dependency, Lineage, And Blast Radius

Goal: practice service dependency and data lineage impact analysis.

---

## Run

```bash
bash SCRIPTS/reset-lab.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/09-dependency-lineage.cypher
```

---

## What To Observe

- service dependencies use explicit `CALLS` and `DEPENDS_ON` relationships
- traversal direction defines the impact question
- data lineage connects datasets, jobs, and dashboards
- both traversals are bounded and return impact context

---

## Explain Out Loud

```text
Why can stale dependency data make incident response dangerous?
```

Strong answer:

```text
Incident response depends on correct ownership and blast-radius paths. If dependency or lineage data is stale, the system can page the wrong team, miss impacted services, or approve risky changes.
```

---

## Completion Gate

- You can explain dependency direction.
- You can explain blast-radius traversal.
- You can name freshness and ownership metrics.