# VectorDB Metadata Filtering, Hybrid Sparse, and Dense Search - Gold Sheet

> Track File #8 of 30 - Group 02: Intermediate Backend
> For: backend/search/GenAI interviews | Level: intermediate | Mode: filters, hybrid search, sparse+dense

## 1. Why Metadata Filters Matter

Vector similarity alone does not know:

- tenant permissions
- language
- product category
- document freshness
- region
- user entitlement

Metadata filters make retrieval safe and product-specific.

---

## 2. Filter Examples

```json
{
  "tenant_id": "t1",
  "language": "en",
  "doc_type": "policy",
  "status": "published"
}
```

Security filters must be applied before results leave the retrieval layer.

---

## 3. Dense vs Sparse

| Retrieval | Strength |
|---|---|
| dense vector | semantic meaning |
| sparse/BM25 | exact words, IDs, rare terms |
| hybrid | combines semantic and lexical signals |

Hybrid search is often stronger for production RAG and product search.

---

## 4. Filter Selectivity

Highly selective filters can reduce candidate pools and hurt recall.

Design question:

```text
Should we partition by tenant, filter by tenant, or maintain separate indexes?
```

Answer depends on tenant size, QPS, isolation, cost, and operations.

---

## 5. Interview Summary

```text
Metadata filters are essential for tenant isolation, ACLs, freshness, language, category, and product constraints. Dense search captures semantic meaning, sparse search captures exact lexical signals, and hybrid retrieval often improves production quality. I would test filter selectivity because filters can improve safety but reduce recall.
```

---

## 6. Revision Notes

- One-line summary: Filters make vector retrieval safe and useful.
- Three keywords: metadata, ACL, hybrid.
- One trap: applying permission checks after retrieval instead of during retrieval.