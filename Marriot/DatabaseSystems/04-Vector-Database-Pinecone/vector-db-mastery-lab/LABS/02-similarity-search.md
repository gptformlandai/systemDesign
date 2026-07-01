# Lab 02: Similarity Search

Goal: run nearest-neighbor vector search and interpret topK results.

---

## Run

```bash
bash SCRIPTS/03-vector-search.sh
```

---

## What To Observe

- similar vectors return higher scores
- topK controls candidate count
- payload metadata explains source and chunk
- raw vector scores still need product interpretation

---

## Explain Out Loud

```text
Why does semantic similarity not automatically mean answer correctness?
```

Strong answer:

```text
Nearest vectors are only candidate evidence. Correctness still depends on chunk quality, source authority, permissions, freshness, reranking, and answer grounding.
```

---

## Completion Gate

- You can explain topK.
- You can explain similarity score limits.
- You can name why reranking may be needed.