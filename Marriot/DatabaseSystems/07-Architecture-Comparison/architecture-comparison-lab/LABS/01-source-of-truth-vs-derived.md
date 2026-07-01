# Lab 01: Source Of Truth vs Derived Stores

Goal: classify data stores by ownership responsibility.

---

## Run

```bash
bash SCRIPTS/03-source-derived-map.sh
```

---

## Drill

Classify each item:

| Data | Source Or Derived? | Why |
|---|---|---|
| payment ledger | source | canonical audit state |
| product search index | derived | rebuilt from catalog source |
| vector RAG index | derived | rebuilt from documents and embeddings |
| Redis cache | derived | rebuilt from durable source |
| warehouse table | derived | fed by ETL/CDC |

---

## Completion Gate

- You can identify canonical owner.
- You can name derived-store freshness risk.
- You can explain rebuild path.