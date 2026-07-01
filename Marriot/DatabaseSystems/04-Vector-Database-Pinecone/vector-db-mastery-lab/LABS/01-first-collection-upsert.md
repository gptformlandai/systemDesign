# Lab 01: First Collection And Upsert

Goal: create a vector collection and seed records with vectors plus payload metadata.

---

## Run

```bash
docker compose up -d
bash SCRIPTS/wait-for-qdrant.sh
bash SCRIPTS/reset-lab.sh
```

---

## What To Observe

- collection dimension is `4`
- distance metric is `Cosine`
- payload contains tenant, ACL, source, doc ID, chunk ID, and embedding model
- deterministic IDs make upserts idempotent

---

## Explain Out Loud

```text
How would this map to Pinecone?
```

Strong answer:

```text
Qdrant collection maps to a Pinecone index conceptually. Qdrant payload maps to Pinecone metadata. The same design decisions are dimension, metric, stable IDs, metadata fields, filters, and evaluation.
```

---

## Completion Gate

- You can explain collection/index dimension.
- You can explain why metadata fields exist.
- You can explain idempotent upsert.