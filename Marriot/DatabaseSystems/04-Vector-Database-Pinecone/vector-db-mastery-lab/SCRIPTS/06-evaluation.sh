#!/usr/bin/env bash
set -euo pipefail

echo "Golden query: password reset help"
echo "Expected relevant doc_id: password-policy"
bash SCRIPTS/05-rag-retrieval.sh

echo
echo "Manual evaluation drill: verify password-policy appears in top results."
echo "If it does, recall@2 for this one-query golden set is 1.0."