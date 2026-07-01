# VectorDB Chunking, Metadata, and Schema Design - Gold Sheet

> Track File #6 of 30 - Group 02: Intermediate Backend
> For: backend/GenAI/search interviews | Level: intermediate | Mode: chunking, schema, filters

## 1. Why Chunking Matters

RAG often searches chunks, not entire documents.

Bad chunking causes:

- missing context
- irrelevant results
- broken citations
- duplicate answers
- high token cost

---

## 2. Chunking Strategies

| Strategy | Use When | Risk |
|---|---|---|
| fixed-size chunks | quick baseline | splits meaning badly |
| paragraph/section chunks | structured docs | uneven length |
| semantic chunks | high-quality retrieval | more complex pipeline |
| parent-child chunks | need citations plus broad context | extra lookup step |

---

## 3. Metadata Schema

Essential fields:

- tenant_id
- acl/group IDs
- document_id
- chunk_id
- source URI
- title/section
- language
- created_at/updated_at
- embedding_model/version
- content_hash

---

## 4. Chunk Size Tradeoff

```text
small chunks -> precise retrieval, less context
large chunks -> more context, more noise
```

Start with a measurable baseline, then tune against golden queries.

---

## 5. Interview Summary

```text
Vector schema design starts from retrieval questions. I would chunk documents by semantic boundaries where possible, store metadata for tenant, ACL, source, version, and freshness, and evaluate chunk size using recall and answer quality. Chunking is a retrieval-quality decision, not only a preprocessing detail.
```

---

## 6. Revision Notes

- One-line summary: Bad chunks make good vector search look bad.
- Three keywords: chunk, metadata, provenance.
- One trap: losing source and ACL metadata during chunk creation.