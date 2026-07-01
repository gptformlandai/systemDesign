# Neo4j Pro Gap Fill: Capacity, Traversal, SLOs, and Design Review

> Track File #30 of 30 - Group 06: Practice Upgrade
> For: backend/data/system design interviews | Level: pro / MAANG | Mode: final gap fill and staff-level design review

This sheet fills advanced gaps that separate a good Neo4j learner from a production-ready graph designer:

- traversal budget
- fan-out and hot-node review
- capacity worksheet
- graph quality metrics
- freshness and sync SLOs
- security/path review
- final design-review checklist

---

## 1. Pro Mental Model

```text
pro Neo4j design = domain question + graph model + indexed anchors + bounded traversal + query plan + SLO + incident plan
```

If your design cannot explain path meaning, fan-out, identity quality, and operational failure behavior, it is not finished.

---

## 2. Traversal Budget Worksheet

| Input | Example |
|---|---|
| start node selectivity | one user by `userId` |
| relationship types | `FOLLOWS`, `BOUGHT`, `USES_DEVICE` |
| max depth | 2 hops |
| average degree | 50 follows |
| high-degree risk | celebrity user or shared IP |
| filters | tenant, status, time range, block list |
| result size | top 20 |
| latency SLO | p99 < 300 ms |

Rough fan-out intuition:

```text
possible expansions ~= degree^depth before filters and limits
```

---

## 3. Capacity Questions

- How many nodes by label?
- How many relationships by type?
- What are high-degree node distributions?
- What are read/write QPS targets?
- What are average and p99 traversal depths?
- What is data freshness requirement?
- What is backup/restore target?
- What is memory/page-cache requirement?

---

## 4. Graph Quality Metrics

Track:

- duplicate entity rate
- orphan node rate
- invalid relationship rate
- stale relationship rate
- unknown source/provenance rate
- false merge rate
- traversal zero-result rate
- recommendation click/accept rate
- fraud false positive/negative rate

---

## 5. SLO And Alert Design

| Graph Path | SLO | Alerts |
|---|---|---|
| user recommendation | p99 < 300 ms | latency, fan-out, zero results, supernode hit rate |
| fraud investigation | p99 < 1 s | slow query, hot node, stale signal, failed imports |
| permission check | p99 < 50 ms | latency, deny mismatch, cache staleness |
| GraphRAG retrieval | p99 < 700 ms | latency, permission leak tests, stale entity rate |

Minimum dashboard:

- query latency by endpoint
- slow queries
- transaction failures/retries
- heap/page cache
- disk and transaction logs
- lock waits/deadlocks
- import/sync lag
- node/relationship counts by type
- high-degree node distribution

---

## 6. Staff-Level Design Review

Ask these before approving a design:

### Domain And Model

- What relationship-heavy question are we answering?
- What labels and relationship types exist?
- Which relationship properties matter?
- What is the source of truth?

### Traversal And Performance

- What are the top 5 Cypher queries?
- Where does each query start?
- What indexes/constraints support anchors?
- What is max depth and expected fan-out?
- What does PROFILE show?

### Scale And Operations

- Are there supernodes?
- Which workloads are online vs batch?
- What clustering/read scaling is required?
- What backup/restore plan exists?
- What dashboards and runbooks exist?

### Security And Quality

- How are tenant/path permissions enforced?
- What relationship data is sensitive?
- How is entity resolution validated?
- How are stale relationships detected?

---

## 7. Staff-Level Interview Answer Template

```text
I would use Neo4j for <relationship-heavy use case>, while keeping <source system> as source of truth if needed. The graph model has <labels> and <relationship types>, with constraints on <identity fields> and indexes for <anchors>. The core traversal starts from <anchor>, follows <bounded paths>, applies <filters>, and returns <result>. I would validate PROFILE, monitor <latency/fan-out/hot nodes/sync lag>, and reject Neo4j for <unsupported workload> by using <alternative>.
```

---

## 8. Revision Notes

- One-line summary: Pro Neo4j design proves graph meaning, bounded traversal, quality, security, and operations.
- Three keywords: traversal budget, hot node, SLO.
- One interview trap: drawing the graph but not budgeting fan-out.
- Memory trick: a graph design is not done until every path has a limit.