#!/usr/bin/env bash
set -euo pipefail

docker compose down -v
docker compose up -d
bash SCRIPTS/wait-for-neo4j.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/01-schema.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/02-seed-data.cypher

echo "Neo4j mastery lab reset and seeded."