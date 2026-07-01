# VectorDB Upsert, Query, Delete API, Python, and Node - Gold Sheet

> Track File #7 of 30 - Group 02: Intermediate Backend
> For: backend interviews | Level: intermediate | Mode: API design, batching, retries

## 1. API Flow

```text
load source -> chunk -> embed -> upsert -> query -> rerank -> return cited results
```

Vector DB APIs are usually simple. Production correctness is not simple.

---

## 2. Upsert Rules

- use deterministic IDs
- batch writes
- retry transient failures
- store embedding model version
- include content hash
- make deletes explicit
- measure ingestion lag

---

## 3. Query Rules

- embed query with compatible model
- include tenant/ACL metadata filters
- choose topK intentionally
- include score thresholds carefully
- rerank when topK quality matters
- return provenance and source IDs

---

## 4. Delete Rules

Deletes are critical for:

- GDPR/right-to-delete
- document removal
- permission revocation
- source-system correction
- embedding model rollback

Soft deletes in source systems must propagate to the vector index.

---

## 5. Python Pseudocode

```python
record = {
    "id": "doc1#chunk1",
    "vector": embed(text),
    "metadata": {"tenant_id": "t1", "doc_id": "doc1", "acl": ["support"]},
}
vector_db.upsert([record])

results = vector_db.query(
    vector=embed("how do I reset my password?"),
    top_k=10,
    filter={"tenant_id": "t1", "acl": {"$contains": "support"}},
)
```

---

## 6. Interview Summary

```text
I would treat vector DB writes as an idempotent ingestion pipeline: deterministic IDs, batching, retries, content hashes, model version metadata, and delete propagation. Queries must include compatible query embeddings, tenant/ACL filters, topK/rerank choices, and provenance.
```

---

## 7. Revision Notes

- One-line summary: Simple APIs still need production ingestion discipline.
- Three keywords: upsert, query, delete.
- One trap: forgetting delete propagation and serving removed content.