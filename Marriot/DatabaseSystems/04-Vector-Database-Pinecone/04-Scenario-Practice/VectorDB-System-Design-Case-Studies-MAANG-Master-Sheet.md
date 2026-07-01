# VectorDB System Design Case Studies - MAANG Master Sheet

> Track File #22 of 30 - Group 04: Scenario Practice
> For: senior system design interviews | Level: MAANG | Mode: case studies

## 1. Enterprise RAG

Documents, chunks, embeddings, tenant/ACL filters, reranking, citations, freshness, evaluation.

## 2. Semantic Product Search

Product embeddings, catalog filters, inventory, personalization, business rerank, conversion metrics.

## 3. Support Chatbot

KB chunks, version/region filters, escalation, stale content tests, feedback loop.

## 4. Code Search

Function/doc embeddings, repo permissions, language filters, symbol lookup hybrid search.

## 5. Similar Recommendations

Item/user/session embeddings, diversity, freshness, offline/online metrics.

## 6. Fraud Similarity

Profile embeddings, nearest risky patterns, false-positive controls, investigator workflow.

## 7. Multimodal Search

Image/text embeddings, safety/license filters, object storage, CDN, reranking.

## 8. Legal Discovery

Strict ACLs, citations, audit logs, exact term hybrid search, defensible evaluation.

## 9. Observability Runbook Search

Incident text embeddings, service filters, recency weighting, runbook citations.

## 10. Personal Memory App

User namespace, privacy, deletes, local-first option, source provenance.

## 11. Healthcare Knowledge Search

PHI controls, audit, high precision, human review, data residency.

## 12. Feature Store Similarity

Feature vectors, model version, drift checks, offline/online parity.

---

## Interview Pattern

```text
For any vector DB case, define embedding source, record schema, metadata filters, index/metric, retrieval/rerank flow, evaluation, freshness, security, scale, and alternatives.
```

---

## Revision Notes

- One-line summary: Vector DB system design repeats the same retrieval architecture under different product constraints.
- Three keywords: schema, evaluation, safety.
- One trap: skipping evaluation and security in scenario answers.