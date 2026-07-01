# Project 03: RAG Document Retrieval

Goal: design Elasticsearch-backed document retrieval for GenAI/RAG.

---

## Requirements

- Search document chunks by lexical text.
- Filter by tenant and ACL before retrieval.
- Support future vector/hybrid search.
- Return source metadata for citations.

---

## Index

```text
rag-chunks-v1
```

Fields:

- `chunk_id`
- `document_id`
- `tenant_id`
- `acl_ids`
- `title`
- `body_chunk`
- `source_uri`
- `indexed_at`

---

## Interview Talking Points

- ACL filters must happen before retrieval
- hybrid search can combine lexical and vector retrieval
- stale embeddings and source versions must be tracked
- evaluate recall@k and groundedness