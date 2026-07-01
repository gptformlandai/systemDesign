# VectorDB Multimodal Image and Audio Search - Gold Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: search/ML/product interviews | Level: senior | Mode: multimodal retrieval

## 1. Core Idea

Multimodal embeddings map different media into vector spaces.

Examples:

- image to image search
- text to image search
- audio clip similarity
- video frame retrieval
- product visual search

---

## 2. Design Choices

- embedding model per modality
- whether text and images share a vector space
- metadata filters for safety, region, rights, and category
- thumbnail/source storage outside vector DB
- reranking with domain-specific models

---

## 3. Record Metadata

- asset_id
- modality
- source URI
- rights/license
- tenant/category
- safety labels
- embedding model version
- capture timestamp

---

## 4. Failure Modes

| Failure | Fix |
|---|---|
| wrong modality match | separate indexes or modality filters |
| unsafe result | safety metadata filters |
| rights violation | license filters |
| poor visual relevance | better embeddings/reranker |
| high storage cost | compression and lifecycle policy |

---

## 5. Interview Summary

```text
For multimodal vector search, I would choose embeddings that support the query and asset modality, store metadata for rights, safety, category, source, and model version, and retrieve with filters before reranking. The vector DB stores embeddings and metadata, while media assets usually live in object storage or a CDN.
```

---

## 6. Revision Notes

- One-line summary: Multimodal search retrieves semantic neighbors across media types.
- Three keywords: modality, rights, safety.
- One trap: storing large media blobs in the vector DB.