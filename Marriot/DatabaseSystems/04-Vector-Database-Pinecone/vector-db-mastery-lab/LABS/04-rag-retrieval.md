# Lab 04: RAG Retrieval

Goal: retrieve authorized document chunks for a RAG-style answer.

---

## Run

```bash
bash SCRIPTS/05-rag-retrieval.sh
```

---

## What To Observe

- query vector matches password policy content
- tenant and ACL filters restrict candidates
- returned payload includes title, text, source, and chunk ID
- source metadata supports citations

---

## Explain Out Loud

```text
What extra stages would production RAG add after this query?
```

Strong answer:

```text
Production RAG may add query rewrite, hybrid retrieval, reranking, context packing, citation formatting, groundedness checks, and answer evaluation.
```

---

## Completion Gate

- You can explain RAG candidate retrieval.
- You can explain citation metadata.
- You can name freshness and permission risks.