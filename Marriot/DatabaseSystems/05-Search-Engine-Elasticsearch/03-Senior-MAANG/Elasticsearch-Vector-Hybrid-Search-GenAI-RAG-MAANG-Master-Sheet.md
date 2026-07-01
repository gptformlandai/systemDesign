# Elasticsearch Vector, Hybrid Search, GenAI, and RAG - MAANG Master Sheet

> Track File #15 of 27 - Group 03: Senior / MAANG
> For: backend/search/GenAI/system design interviews | Level: senior / MAANG | Mode: vector search, hybrid retrieval, RAG safety

This sheet builds:
- Dense vector and kNN mental model
- Hybrid lexical + semantic retrieval
- RAG metadata, ACL, reranking, and evaluation

---

## 1. Vector Search Mental Model

Vector search finds documents whose embeddings are close to the query embedding.

```text
text/document -> embedding model -> dense vector -> kNN search -> candidate chunks
```

Use for:

- semantic document retrieval
- recommendations
- FAQ/support search
- RAG chunk retrieval
- similar product/document discovery

---

## 2. Hybrid Search

Hybrid search combines lexical and vector retrieval.

Why:

- lexical search is strong for exact terms, SKUs, names, rare keywords
- vector search is strong for semantic similarity
- hybrid often improves recall and precision together

Common flow:

```text
metadata filters -> lexical query + vector query -> candidate merge -> rerank -> answer/use results
```

---

## 3. RAG Document Design

Fields:

- `chunk_id`
- `document_id`
- `tenant_id`
- `acl_ids`
- `source_uri`
- `title`
- `body_chunk`
- `embedding`
- `language`
- `version`
- `indexed_at`

Rule:

```text
Tenant and ACL filters must apply before retrieval results reach the LLM.
```

---

## 4. Evaluation

Track:

- retrieval recall@k
- answer groundedness
- hallucination rate
- source citation quality
- latency
- stale embeddings
- ACL leak tests
- zero-result and low-confidence fallback

---

## 5. Strong Answer

Question:

> How would you use Elasticsearch for RAG?

Strong answer:

```text
I would index document chunks with metadata, ACL fields, lexical fields, and dense vectors. Retrieval would apply tenant and ACL filters first, then use hybrid lexical plus vector search to get candidates, optionally rerank them, and pass only authorized chunks to the LLM. I would track recall@k, groundedness, latency, stale embeddings, and ACL leak tests. If vector workload or scale exceeds Elasticsearch's strengths, I would compare a specialized vector database.
```

---

## 6. Revision Notes

- One-line summary: Elasticsearch can support hybrid RAG when metadata, ACLs, vectors, and evaluation are designed together.
- Three keywords: vector, hybrid, ACL.
- One interview trap: retrieving chunks first and applying security after.
- Memory trick: RAG retrieval is search plus safety plus evaluation.