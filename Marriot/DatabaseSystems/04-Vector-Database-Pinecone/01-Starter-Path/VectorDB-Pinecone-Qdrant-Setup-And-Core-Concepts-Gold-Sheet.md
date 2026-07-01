# VectorDB Pinecone, Qdrant Setup, and Core Concepts - Gold Sheet

> Track File #3 of 30 - Group 01: Starter Path
> For: backend/search/GenAI interviews | Level: beginner | Mode: Pinecone reference, Qdrant lab concepts

## 1. Why Pinecone Plus Qdrant

| System | Best Role In This Track |
|---|---|
| Pinecone | managed production reference and interview vocabulary |
| Qdrant | local Docker lab and open-source hands-on practice |

The concepts transfer:

```text
Pinecone index ~= Qdrant collection
Pinecone metadata ~= Qdrant payload
Pinecone namespace ~= Qdrant collection/tenant strategy or payload partition
```

---

## 2. Core Concepts

| Concept | Meaning |
|---|---|
| index/collection | container for vectors of one dimension and metric |
| vector | embedding array |
| point/record | ID plus vector plus metadata/payload |
| namespace | logical partition, often tenant or environment |
| metadata filter | structured filter applied during retrieval |
| topK | number of candidates returned |

---

## 3. Local Lab Setup

The lab uses Qdrant because it runs with Docker:

```bash
docker compose up -d
```

The production answers should still mention Pinecone when the interviewer expects a managed vector DB.

---

## 4. First Design Choice

Before creating an index/collection, decide:

- embedding model
- vector dimension
- similarity metric
- record ID strategy
- tenant/isolation strategy
- metadata fields
- expected QPS and topK

---

## 5. Interview Summary

```text
In Pinecone I would create an index with the dimension and metric required by the embedding model, then write records with IDs, vectors, and metadata. In local practice I can use Qdrant collections with the same dimension/metric concepts. The important design decisions are embedding model, schema, tenant isolation, metadata filters, topK, and evaluation.
```

---

## 6. Revision Notes

- One-line summary: Index/collection design starts with model dimension and metric.
- Three keywords: index, collection, namespace.
- One trap: creating one index before knowing the embedding model.