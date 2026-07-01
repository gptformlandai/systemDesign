#!/usr/bin/env bash
set -euo pipefail

cat <<'TEXT'
Scenario: ecommerce marketplace datastore choice

Workflow                         Choice                         Reason
orders/payments                  PostgreSQL/MySQL               transactions, constraints, audit
product catalog source            MongoDB or PostgreSQL          document aggregate or relational model
product search                    Elasticsearch/OpenSearch       relevance, facets, filters
recommendations                   Vector DB + ranking service    semantic similarity and personalization
cart/session hot reads             Redis + durable source         low latency, rebuildable derived state
product images                    Object storage + CDN           durable blobs and delivery
analytics                         Warehouse/Lakehouse            OLAP scans and reporting

Decision rule:
  Keep money and inventory correctness in a transactional source of truth.
  Use search/cache/vector/analytics as derived stores with CDC, freshness SLOs, and rebuild paths.
TEXT