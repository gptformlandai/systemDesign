# Lab 07: Authorized RAG Retrieval

Goal: practice tenant and ACL filtering before document chunks reach an LLM.

---

## Run

```bash
bash SCRIPTS/reset-lab.sh
bash SCRIPTS/run-request.sh SCRIPTS/08-rag-acl-search.sh
```

---

## What To Observe

- The authorized query uses `tenant_id` and `acl_ids` filters.
- The same text query returns different visibility for different ACLs.
- The search result includes source metadata that could become a citation.

---

## Explain Out Loud

```text
Why must tenant and ACL filters run before retrieval and reranking?
```

Strong answer:

```text
Filtering after retrieval can leak snippets, counts, metadata, or chunks into the LLM context. Authorized retrieval means the candidate set is filtered before lexical, vector, or hybrid ranking is used downstream.
```

---

## Completion Gate

- You can explain tenant filters.
- You can explain ACL filters.
- You can name RAG leak tests and stale-permission tests.