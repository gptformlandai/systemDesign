# VectorDB Multi-Tenancy, ACL, and Security - Gold Sheet

> Track File #12 of 30 - Group 03: Senior / MAANG
> For: backend/security/GenAI interviews | Level: senior | Mode: tenant isolation, ACLs, permission leaks

## 1. Why Security Is Hard

Vector search retrieves semantically similar content. Without strict filters, it can retrieve content the user should never see.

Security bugs include:

- cross-tenant results
- stale ACL metadata
- deleted document still retrievable
- embeddings revealing sensitive text in logs
- reranker receiving unauthorized candidates

---

## 2. Isolation Choices

| Strategy | Strength | Risk |
|---|---|---|
| index per tenant | strongest isolation | high ops/cost for many tenants |
| namespace per tenant | good logical isolation | namespace sprawl |
| metadata tenant filter | flexible | filter mistakes can leak data |
| hybrid | large tenants isolated, small tenants filtered | more routing complexity |

---

## 3. ACL Query Rule

```text
tenant and ACL filters must be part of retrieval before candidates leave the vector DB/search layer
```

Do not retrieve broad candidates and filter later in the application if unauthorized content can leak through logs, traces, rerankers, or metrics.

---

## 4. Security Checklist

- encrypt data at rest and in transit
- avoid logging raw sensitive chunks
- store tenant_id and ACL metadata
- propagate permission updates and deletes
- test deny cases in golden evaluation
- keep audit trails for ingestion and access
- restrict admin/API keys

---

## 5. Interview Summary

```text
For multi-tenant vector search, I would choose index, namespace, metadata-filter, or hybrid isolation based on tenant size, risk, and cost. Tenant and ACL filters must be enforced during retrieval, not after reranking. I would also test permission leaks, delete propagation, encryption, audit logs, and stale ACL failure modes.
```

---

## 6. Revision Notes

- One-line summary: Vector retrieval must be authorized before candidates leave retrieval.
- Three keywords: tenant, ACL, leak.
- One trap: sending unauthorized candidates to a reranker.