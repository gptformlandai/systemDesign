# Lab 07: Hybrid Fusion And Rerank

Goal: practice how dense and sparse retrieval can be fused before reranking.

---

## Run

```bash
bash SCRIPTS/08-hybrid-fusion-demo.sh
```

---

## What To Observe

- dense ranking captures semantic similarity
- sparse ranking captures exact terms like `login`, `MFA`, and error language
- reciprocal rank fusion combines ranks without needing raw score calibration
- a reranker or business rule can break close ties

---

## Explain Out Loud

```text
Why is score calibration hard in hybrid search?
```

Strong answer:

```text
Dense vector scores and sparse lexical scores come from different scoring systems, so raw scores may not be directly comparable. Rank-based fusion methods like reciprocal rank fusion avoid some calibration problems by combining result positions instead of raw scores.
```

---

## Production Notes

- apply tenant and ACL filters before fusion/reranking
- measure quality lift against a golden set
- cap reranker depth to protect p99 latency and cost
- keep fallback behavior if sparse or dense retrieval fails

---

## Completion Gate

- You can explain dense vs sparse retrieval.
- You can explain reciprocal rank fusion.
- You can explain why reranker depth needs a latency budget.