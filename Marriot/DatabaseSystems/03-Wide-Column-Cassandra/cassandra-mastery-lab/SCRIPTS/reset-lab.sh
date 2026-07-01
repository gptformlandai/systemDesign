#!/usr/bin/env bash
set -euo pipefail

docker compose down -v
docker compose up -d
bash SCRIPTS/wait-for-cassandra.sh
bash SCRIPTS/run-cqlsh.sh SCRIPTS/00-create-keyspace.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/01-schema.cql
bash SCRIPTS/run-cqlsh.sh SCRIPTS/02-seed-data.cql

echo "Cassandra mastery lab reset and seeded."