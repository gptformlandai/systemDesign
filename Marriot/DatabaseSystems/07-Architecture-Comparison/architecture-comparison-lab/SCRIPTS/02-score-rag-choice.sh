#!/usr/bin/env bash
set -euo pipefail

cat <<'TEXT'
Scenario: enterprise RAG datastore choice

Workflow                         Choice                         Reason
raw documents                     Object storage/document DB     authoritative content
document metadata and ACL          PostgreSQL/MongoDB             ownership, permissions, lifecycle
keyword retrieval                  Elasticsearch/OpenSearch       exact terms, filters, BM25
semantic retrieval                 Pinecone/Qdrant VectorDB       embedding similarity
entity/path expansion              Neo4j                          relationships and provenance paths
evaluation and analytics           Warehouse/Lakehouse            offline quality and usage analysis

Decision rule:
  Search, vector, and graph stores are derived retrieval indexes.
  Tenant/ACL filters must apply during retrieval before reranking or LLM context packing.
TEXT