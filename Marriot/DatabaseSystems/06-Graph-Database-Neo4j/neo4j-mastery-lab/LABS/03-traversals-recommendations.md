# Lab 03: Traversals And Recommendations

Goal: practice bounded traversals and recommendation patterns.

---

## Run

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/04-recommendations.cypher
```

---

## What To Observe

- product recommendation uses shared buyers
- friend recommendation uses two-hop follows
- queries exclude already connected nodes
- `LIMIT` and bounded hops keep the exercise safe

---

## Explain Out Loud

```text
When should recommendations be precomputed instead of calculated online?
```

---

## Completion Gate

- You can explain two-hop traversal.
- You can explain mutual/shared scoring.
- You can name fan-out and supernode risks.