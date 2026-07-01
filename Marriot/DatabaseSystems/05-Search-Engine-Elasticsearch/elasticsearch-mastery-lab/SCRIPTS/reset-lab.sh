#!/usr/bin/env bash
set -euo pipefail

docker compose down -v
docker compose up -d
bash SCRIPTS/wait-for-elasticsearch.sh
bash SCRIPTS/run-request.sh SCRIPTS/00-cluster-health.sh
bash SCRIPTS/run-request.sh SCRIPTS/01-create-indices.sh
bash SCRIPTS/run-request.sh SCRIPTS/02-seed-data.sh

echo "Elasticsearch mastery lab reset and seeded."