# Lab 03: RAG Search, Vector, and Graph

Goal: compare retrieval stores in a RAG architecture.

---

## Run

```bash
bash SCRIPTS/02-score-rag-choice.sh
```

---

## Explain Out Loud

```text
Why might production RAG use search, vector, and graph stores together?
```

Strong answer:

```text
Search handles exact terms, IDs, and lexical ranking. Vector search handles semantic similarity. Graph stores handle entity relationships, provenance, and path expansion. All are derived from source documents and metadata.
```

---

## Completion Gate

- You can explain search vs vector vs graph.
- You can explain ACL retrieval-time filters.
- You can name citation and freshness risks.