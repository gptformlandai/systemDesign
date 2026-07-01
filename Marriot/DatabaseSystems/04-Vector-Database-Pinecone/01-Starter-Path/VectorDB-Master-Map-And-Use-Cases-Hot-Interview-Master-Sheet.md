# VectorDB Master Map and Use Cases - Hot Interview Master Sheet

> Track File #1 of 30 - Group 01: Starter Path
> For: backend/search/GenAI interviews | Level: beginner | Mode: mental model, fit, vocabulary

## 1. Core Idea

A vector database stores embeddings and retrieves the nearest items by semantic similarity.

```text
text/image/audio/code -> embedding model -> vector -> vector DB -> nearest neighbors
```

It is useful when exact keyword matching is not enough and the product needs meaning-based retrieval.

---

## 2. What A Vector DB Stores

| Part | Meaning |
|---|---|
| ID | stable record identity |
| vector | numeric embedding array |
| metadata | tenant, ACL, category, timestamp, source, language |
| payload/text | optional chunk text or reference to object storage |

Pinecone calls the container an index. Qdrant calls it a collection. The interview concept is the same: vectors plus metadata plus query APIs.

---

## 3. High-Value Use Cases

| Use Case | Why Vector DB Fits |
|---|---|
| RAG document retrieval | semantic chunks and citations |
| semantic product search | user intent differs from exact catalog words |
| recommendations | similar items/users/content |
| support chatbot | retrieve relevant policy/KB chunks |
| fraud/risk similarity | similar behavior or identity patterns |
| image/audio search | multimodal embedding lookup |
| code search | semantic search over functions and docs |

---

## 4. When Not To Use It

- exact lookup by primary key
- transactional source of truth
- heavy joins or reporting
- strict relational constraints
- pure keyword ranking with rich analyzers
- workloads where recall cannot be approximate

---

## 5. Interview Summary

```text
I would use a vector database when the core problem is semantic similarity retrieval. I would store embeddings with stable IDs and metadata, query with a query embedding, filter by tenant/ACL/product constraints, retrieve topK candidates, optionally rerank, and evaluate recall and groundedness. I would not use it as the primary transactional database.
```

---

## 6. Revision Notes

- One-line summary: Vector DBs retrieve nearest embeddings, not exact records.
- Three keywords: embedding, similarity, topK.
- One trap: treating vector search quality as automatic.