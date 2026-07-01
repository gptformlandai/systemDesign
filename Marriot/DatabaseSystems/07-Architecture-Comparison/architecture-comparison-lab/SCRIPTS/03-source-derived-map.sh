#!/usr/bin/env bash
set -euo pipefail

cat <<'TEXT'
Source of truth vs derived store quick map

Canonical source of truth:
  - order payment state
  - inventory reservation
  - account permissions
  - document ownership metadata
  - ledger entries

Common derived stores:
  - Redis cache
  - Elasticsearch/OpenSearch index
  - Vector DB index
  - Neo4j projection
  - analytics warehouse table
  - materialized read model

Production rule:
  Derived stores must be rebuildable, monitored for freshness, and fed by CDC/events/replay.
TEXT