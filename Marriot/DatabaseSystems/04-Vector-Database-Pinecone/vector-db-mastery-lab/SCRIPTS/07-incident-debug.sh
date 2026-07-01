#!/usr/bin/env bash
set -euo pipefail

echo "Incident: retrieval quality dropped after an embedding-model change."
echo "Check order: query vector, collection dimension, metric, metadata filters, topK, seeded model version, and golden-set recall."
echo "Run: bash SCRIPTS/06-evaluation.sh"