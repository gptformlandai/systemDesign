# VectorDB Records, Namespaces, Metadata, and CRUD - Gold Sheet

> Track File #4 of 30 - Group 01: Starter Path
> For: backend/search/GenAI interviews | Level: beginner | Mode: records, metadata, CRUD

## 1. Vector Record Shape

```json
{
  "id": "doc1#chunk3",
  "vector": [0.12, -0.03, 0.98],
  "metadata": {
    "tenant_id": "t1",
    "doc_id": "doc1",
    "chunk_id": "chunk3",
    "source": "handbook",
    "acl": ["support", "admin"],
    "embedding_model": "demo-model"
  }
}
```

The ID should be stable and deterministic when idempotent upserts matter.

---

## 2. CRUD Operations

| Operation | Purpose |
|---|---|
| upsert | create or replace vector record |
| query/search | retrieve nearest vectors |
| fetch | get records by ID |
| delete | remove stale or unauthorized records |
| update metadata | change filterable fields without recomputing vector, if supported |

---

## 3. Namespace Strategy

Common choices:

- namespace per tenant
- namespace per environment
- namespace per embedding version
- metadata field per tenant

Tradeoff:

```text
Namespaces simplify isolation but can fragment data. Metadata filters are flexible but require strict access-control discipline.
```

---

## 4. Metadata Design Rules

- include tenant and ACL fields
- include source and document IDs
- include timestamps and embedding version
- include language/category/type fields used in filters
- avoid huge metadata blobs
- avoid fields that cannot be indexed or filtered efficiently

---

## 5. Interview Summary

```text
A vector DB record should have a stable ID, vector, and metadata that supports tenant isolation, filtering, provenance, freshness, and model versioning. CRUD must be idempotent because ingestion pipelines commonly retry.
```

---

## 6. Revision Notes

- One-line summary: Metadata is not decoration; it is retrieval control.
- Three keywords: ID, namespace, metadata.
- One trap: storing vectors without enough provenance to debug retrieval.