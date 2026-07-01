# Lab 06: Incident Debugging

Goal: rehearse vector retrieval incident response.

---

## Run

```bash
bash SCRIPTS/07-incident-debug.sh
```

---

## Scenario

```text
Retrieval quality dropped after an embedding-model upgrade.
```

---

## Debug Checklist

1. Did query embedding and stored vectors use compatible models?
2. Did dimension and metric stay correct?
3. Did all documents reindex?
4. Did metadata filters change?
5. Did reranker behavior change?
6. Did golden-set recall regress?
7. Is rollback available?

---

## Completion Gate

- You can explain embedding version metadata.
- You can explain model migration risk.
- You can name rollback and canary steps.