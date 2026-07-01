# VectorDB Support Knowledge Base Chatbot - Gold Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: GenAI/support system interviews | Level: intermediate to senior | Mode: support KB retrieval

## 1. Requirements

- retrieve relevant support articles
- respect product/version/region
- cite article and section
- avoid outdated policy
- escalate when confidence is low
- support feedback loop

---

## 2. Metadata

Important filters:

- product
- version
- region
- language
- customer tier
- publish_status
- effective_date
- article_owner

---

## 3. Retrieval Flow

```text
customer question -> classify product/version -> vector search with filters -> rerank -> answer or escalate
```

---

## 4. Quality Controls

- golden support questions
- no-answer threshold
- freshness checks
- citation audit
- human feedback labels
- regression tests before content releases

---

## 5. Interview Summary

```text
For a support KB chatbot, I would chunk articles by sections, store product/version/region/language/status metadata, retrieve with filters, rerank results, cite sources, and escalate when confidence is low. Evaluation should include answer success, citation correctness, stale article rate, and support deflection quality.
```

---

## 6. Revision Notes

- One-line summary: Support RAG needs version-aware and status-aware retrieval.
- Three keywords: version, citation, escalation.
- One trap: serving draft or outdated articles.