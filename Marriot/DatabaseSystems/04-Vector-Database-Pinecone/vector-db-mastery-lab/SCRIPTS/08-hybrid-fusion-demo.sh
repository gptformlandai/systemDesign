#!/usr/bin/env bash
set -euo pipefail

cat <<'TEXT'
Hybrid retrieval demo: reciprocal rank fusion

User query:
  login error MFA code not working

Dense vector ranking:
  1. login-runbook
  2. password-policy
  3. risk-similarity

Sparse/BM25 ranking:
  1. login-runbook
  2. password-policy
  3. shoe-arch-support

Reciprocal rank fusion idea:
  score(doc) = 1 / (k + dense_rank) + 1 / (k + sparse_rank)

With k = 60:
  login-runbook     = 1/61 + 1/61 = 0.03279
  password-policy   = 1/62 + 1/62 = 0.03226
  risk-similarity   = 1/63 + 0    = 0.01587
  shoe-arch-support = 0    + 1/63 = 0.01587

Final fused ranking:
  1. login-runbook
  2. password-policy
  3. risk-similarity or shoe-arch-support tie, resolved by business rule or reranker

Interview takeaway:
  Hybrid search helps when dense retrieval captures meaning and sparse retrieval captures exact terms, IDs, error codes, product names, or policy language.
TEXT