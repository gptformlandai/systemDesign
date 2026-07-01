# VectorDB Vendor Architecture: Pinecone, Qdrant, Weaviate, Milvus - Gold Sheet

> Track File #16 of 30 - Group 03: Senior / MAANG
> For: senior system design interviews | Level: senior | Mode: vendor tradeoffs

## 1. Vendor Map

| System | Strong Fit |
|---|---|
| Pinecone | managed vector DB, fast production adoption, low ops burden |
| Qdrant | open-source, Docker-friendly, strong payload filtering, practical labs |
| Weaviate | vector DB with schema/objects and hybrid search focus |
| Milvus | large-scale open-source vector platform, more operational complexity |
| pgvector | smaller workloads inside PostgreSQL, simple stack |
| Elasticsearch/OpenSearch | hybrid lexical/vector search with existing search infrastructure |
| MongoDB Atlas Vector Search | vector search near document data |

---

## 2. Choice Questions

Ask:

- managed or self-hosted?
- corpus size and QPS?
- pure vector or hybrid lexical/vector?
- metadata filter complexity?
- tenant isolation needs?
- operational team strength?
- latency and recall targets?
- data residency/security constraints?

---

## 3. Pinecone Answer

```text
I would choose Pinecone when the team wants a managed vector database with minimal ops burden, high availability, and fast production adoption. I would still design schema, namespaces, metadata filters, evaluation, freshness, and cost controls carefully because managed does not remove retrieval architecture responsibility.
```

---

## 4. Qdrant Answer

```text
I would choose Qdrant when I want open-source control, local development, payload filtering, and operational flexibility. The tradeoff is that my team owns deployment, upgrades, scaling, backups, and incident response.
```

---

## 5. Interview Summary

```text
Pinecone is strong for managed vector DB adoption, Qdrant is strong for practical open-source control, Weaviate is strong for object/schema and hybrid retrieval patterns, Milvus is strong for large-scale vector infrastructure, pgvector is strong when PostgreSQL simplicity is enough, and Elasticsearch/OpenSearch are strong when lexical search is equally important.
```

---

## 6. Revision Notes

- One-line summary: Vendor choice is ops model plus retrieval requirements.
- Three keywords: managed, hybrid, scale.
- One trap: picking a vector DB without evaluating filters, freshness, and ops.