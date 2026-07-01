# Elasticsearch Cloud, Kubernetes, Testing, and Operations - Gold Sheet

> Track File #16 of 27 - Group 03: Senior / MAANG
> For: backend/search/system design interviews | Level: senior | Mode: managed platforms, Kubernetes, testing, upgrades

This sheet builds:
- Elastic Cloud and Kubernetes tradeoffs
- Testcontainers and integration testing approach
- Upgrade, capacity, and operations maturity

---

## 1. Managed Elasticsearch

Managed services reduce operational load, but they do not remove search design responsibility.

Check:

- supported version and feature set
- security defaults
- snapshot/restore behavior
- scaling limits
- hot/warm/cold tiers
- slow logs and metrics access
- vector search support
- pricing for storage, compute, egress, and replicas

---

## 2. Kubernetes Caution

Elasticsearch can run on Kubernetes with mature operators, but stateful search clusters need careful operations.

Evaluate:

- persistent volume performance
- pod disruption budgets
- anti-affinity across zones
- JVM/heap and memory limits
- snapshot repository
- shard allocation awareness
- rolling upgrade behavior
- operator maturity

Strong answer:

```text
I would not run Elasticsearch on Kubernetes casually. I would require storage, operator, backup, upgrade, and observability maturity before accepting that design.
```

---

## 3. Testing Strategy

| Test Type | Purpose |
|---|---|
| unit tests | query construction and field allowlists |
| integration tests | real mappings, queries, aggregations |
| relevance tests | golden query set and expected ranking |
| sync tests | source-to-index lag and idempotency |
| load tests | p99 latency, bulk indexing, shard behavior |
| security tests | tenant/ACL leakage prevention |

Testcontainers is useful for verifying mappings, queries, and repository/search API behavior.

---

## 4. Upgrade And Migration Discipline

Safer rollout:

1. Check breaking changes.
2. Snapshot before risky operations.
3. Validate mappings/templates/ILM in staging.
4. Roll nodes carefully or use managed upgrade path.
5. Monitor indexing/search latency, errors, and shard allocation.
6. Keep rollback/rebuild plan.

---

## 5. Strong Answer

Question:

> How would you test Elasticsearch-backed search?

Strong answer:

```text
I would unit test query construction, use integration tests against real Elasticsearch for mappings and query DSL, and maintain a golden relevance test set for important user queries. I would also test sync lag, bulk retry behavior, tenant/ACL filters, and load-test p99 latency because correctness in search includes relevance, freshness, security, and performance.
```

---

## 6. Revision Notes

- One-line summary: Managed/cloud/Kubernetes choices do not replace mappings, relevance tests, sync tests, and operations discipline.
- Three keywords: managed, Testcontainers, relevance tests.
- One interview trap: testing only that the API returns any hits.
- Memory trick: search tests must prove quality, not just syntax.