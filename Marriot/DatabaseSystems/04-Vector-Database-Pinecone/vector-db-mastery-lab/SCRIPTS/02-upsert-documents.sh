#!/usr/bin/env bash
set -euo pipefail

base_url="${QDRANT_URL:-http://localhost:6333}"
collection="${VECTOR_COLLECTION:-vector_lab}"

curl -fsS -X PUT "$base_url/collections/$collection/points?wait=true" \
  -H 'Content-Type: application/json' \
  -d '{
    "points": [
      {
        "id": 1,
        "vector": [0.10, 0.90, 0.10, 0.20],
        "payload": {
          "tenant_id": "t1",
          "acl_group": "support",
          "doc_type": "policy",
          "doc_id": "password-policy",
          "chunk_id": "password-policy#1",
          "title": "Password Reset Policy",
          "text": "Users can reset passwords from account settings after MFA verification.",
          "source": "kb/password-policy",
          "embedding_model": "demo-4d-v1"
        }
      },
      {
        "id": 2,
        "vector": [0.12, 0.86, 0.11, 0.18],
        "payload": {
          "tenant_id": "t1",
          "acl_group": "support",
          "doc_type": "runbook",
          "doc_id": "login-runbook",
          "chunk_id": "login-runbook#1",
          "title": "Login Incident Runbook",
          "text": "Check identity provider health, MFA service, and session token errors.",
          "source": "runbooks/login",
          "embedding_model": "demo-4d-v1"
        }
      },
      {
        "id": 3,
        "vector": [0.80, 0.10, 0.20, 0.10],
        "payload": {
          "tenant_id": "t1",
          "acl_group": "commerce",
          "doc_type": "product",
          "doc_id": "shoe-arch-support",
          "chunk_id": "shoe-arch-support#1",
          "title": "Arch Support Walking Shoe",
          "text": "Comfortable walking shoe with arch support for long standing shifts.",
          "source": "catalog/shoe-arch-support",
          "embedding_model": "demo-4d-v1"
        }
      },
      {
        "id": 4,
        "vector": [0.75, 0.15, 0.25, 0.12],
        "payload": {
          "tenant_id": "t2",
          "acl_group": "commerce",
          "doc_type": "product",
          "doc_id": "shoe-private-catalog",
          "chunk_id": "shoe-private-catalog#1",
          "title": "Private Tenant Shoe Catalog",
          "text": "Tenant two private catalog content should never appear for tenant one.",
          "source": "catalog/private-tenant-two",
          "embedding_model": "demo-4d-v1"
        }
      },
      {
        "id": 5,
        "vector": [0.15, 0.20, 0.92, 0.25],
        "payload": {
          "tenant_id": "t1",
          "acl_group": "security",
          "doc_type": "risk",
          "doc_id": "risk-similarity",
          "chunk_id": "risk-similarity#1",
          "title": "Risk Similarity Pattern",
          "text": "Shared device, unusual velocity, and repeated failed payments can indicate risk similarity.",
          "source": "risk/patterns",
          "embedding_model": "demo-4d-v1"
        }
      }
    ]
  }'

echo "Seeded sample vectors into $collection"