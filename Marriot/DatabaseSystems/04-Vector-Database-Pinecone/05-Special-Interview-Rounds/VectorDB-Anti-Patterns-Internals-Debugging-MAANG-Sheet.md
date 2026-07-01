# VectorDB Anti-Patterns, Internals, and Debugging - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
> For: senior debugging/interview rounds | Level: senior | Mode: failure modes and fixes

## 1. Common Anti-Patterns

| Anti-Pattern | Why It Fails | Better Approach |
|---|---|---|
| one giant chunk per document | noisy retrieval | semantic/section chunks |
| no metadata | cannot filter, secure, or debug | tenant, ACL, source, version fields |
| post-retrieval ACL filtering | leaks candidates | retrieval-time authorization filters |
| no golden set | no quality control | offline evaluation before rollout |
| mixed embedding models | incomparable vectors | versioned indexes/namespaces |
| topK too small | missed evidence | tune topK with recall tests |
| topK too large | latency and token cost | rerank and context budget |

---

## 2. Low Recall Debugging

Check:

1. query text and embedding model
2. embedding dimension and metric
3. chunking quality
4. metadata filters too strict
5. topK too low
6. ANN recall settings
7. stale or missing vectors
8. reranker suppressing correct results

---

## 3. Slow Query Debugging

Check:

- topK and reranker depth
- filter selectivity
- hot tenant or namespace
- high dimension/corpus size
- index health
- recent deploys or reindexing
- embedding API latency

---

## 4. Security Debugging

Check:

- tenant filter included in every query
- ACL metadata is current
- deleted documents are removed
- reranker sees only authorized candidates
- logs do not expose sensitive chunks

---

## 5. Strong Interview Answer

```text
When vector retrieval quality drops, I would not blindly tune ANN settings. I would start from the exact query, filters, embedding model, chunking, topK, index version, reranker, and freshness pipeline, then compare against a golden set. Vector search incidents are often schema, chunking, filter, freshness, or evaluation problems rather than only database problems.
```

---

## 6. Revision Notes

- One-line summary: Most vector DB failures are retrieval design failures, not only database failures.
- Three keywords: chunking, filters, evaluation.
- One trap: tuning ANN while ACL filters or chunks are wrong.