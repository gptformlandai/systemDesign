# VectorDB Mini Projects Portfolio

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: portfolio and interview discussion | Level: beginner to pro | Mode: project specs

Each project should include embedding model, schema, metadata filters, retrieval flow, evaluation, scaling concerns, security concerns, and tradeoffs.

---

## 1. Enterprise RAG Search

Build:

- document chunks
- tenant and ACL metadata
- citation fields
- retrieval evaluation

Discuss:

- permission leaks
- stale content
- reranking

---

## 2. Semantic Product Search

Build:

- product embeddings
- catalog filters
- inventory and region metadata
- business reranking

Discuss:

- conversion metrics
- exact term fallback
- personalization

---

## 3. Support Knowledge Base Chatbot

Build:

- support article chunks
- version and publish status filters
- answer citations
- escalation path

Discuss:

- outdated articles
- no-answer policy
- support feedback loop

---

## 4. Image Similarity Search

Build:

- image embeddings
- asset metadata
- safety/license filters
- object storage references

Discuss:

- multimodal embeddings
- CDN/object-store separation
- rights and safety controls

---

## 5. Fraud Similarity Explorer

Build:

- risk-profile embeddings
- nearest risky profiles
- investigator notes
- false-positive tracking

Discuss:

- similarity is not proof
- drift
- human review

---

## Portfolio Scoring

| Area | What To Prove |
|---|---|
| embeddings | model, dimension, metric, version are clear |
| schema | stable IDs and metadata support retrieval |
| security | tenant and ACL filters are retrieval-time |
| quality | golden queries and metrics exist |
| operations | freshness, deletes, p99, cost are covered |
| tradeoffs | knows when vector DB is wrong |

MAANG-ready portfolio:

```text
At least 4 projects can be explained end-to-end in 10 minutes each with follow-up answers.
```