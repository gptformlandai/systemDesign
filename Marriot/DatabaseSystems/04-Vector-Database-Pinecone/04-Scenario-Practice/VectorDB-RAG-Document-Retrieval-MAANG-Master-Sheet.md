# VectorDB RAG Document Retrieval - MAANG Master Sheet

> Track File #17 of 30 - Group 04: Scenario Practice
> For: GenAI/system design interviews | Level: senior | Mode: enterprise RAG retrieval

## 1. Problem

Design retrieval for an enterprise assistant that answers from internal documents.

Requirements:

- tenant isolation
- ACL filters
- citations
- freshness
- low hallucination risk
- evaluation before rollout

---

## 2. Model

```text
Document -> chunks -> embeddings -> vector DB records -> retrieval -> rerank -> context -> answer
```

Record metadata:

- tenant_id
- acl groups
- document_id
- chunk_id
- source URL
- updated_at
- embedding_model
- content_hash

---

## 3. Retrieval Flow

```text
query -> query embedding -> tenant/ACL filter -> topK vector search -> rerank -> fetch source snippets -> answer with citations
```

---

## 4. Failure Modes

| Failure | Fix |
|---|---|
| irrelevant chunks | improve chunking, hybrid search, reranking |
| permission leak | retrieval-time ACL filters and leak tests |
| stale content | delete/update propagation metrics |
| missing citations | store source/chunk metadata |
| hallucination | groundedness eval and refusal policy |

---

## 5. Interview Summary

```text
For enterprise RAG, I would store chunk embeddings with tenant, ACL, source, freshness, and embedding-version metadata. Retrieval would apply authorization filters before candidate return, use topK plus reranking, return citations, and be evaluated with recall, groundedness, citation correctness, stale-content rate, and permission leak tests.
```

---

## 6. Revision Notes

- One-line summary: Enterprise RAG is authorized retrieval plus evaluation, not just vector search.
- Three keywords: ACL, citation, groundedness.
- One trap: retrieving first and filtering permissions later.