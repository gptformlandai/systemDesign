# Neo4j System Design Case Studies - MAANG Master Sheet

> Track File #22 of 30 - Group 04: Scenario Practice
> For: backend/data/system design interviews | Level: senior / MAANG | Mode: case studies, graph modeling, tradeoffs

This sheet builds:
- 12 Neo4j system design cases
- Graph model, traversal, constraints, performance, and operations thinking
- Follow-up-ready architecture language

---

## Case Study Template

```text
requirements -> graph model -> constraints/indexes -> traversal/query -> freshness/sync -> scale/ops -> failure modes -> alternatives
```

---

## 1. Social Recommendations

Users, follows, blocks, likes, content. Bound two-hop traversal or precompute scores.

## 2. Fraud Ring Detection

Accounts connected to devices/cards/emails/IPs. Use bounded shared-signal traversals and risk weights.

## 3. Permission Graph

Users, groups, roles, resources, inherited permissions. Enforce tenant and deny rules carefully.

## 4. Supply Chain Risk

Vendors, parts, products, routes, regions. Traverse dependencies and blast radius.

## 5. Service Dependency Graph

Services, databases, queues, APIs. Analyze impact of outages and deployment risk.

## 6. Network Topology

Routers, hosts, links, subnets. Pathfinding and blast-radius analysis.

## 7. Knowledge Graph Search

Documents, chunks, entities, facts, sources. Combine search and graph traversal.

## 8. GraphRAG

Question to entity/vector retrieval, graph expansion, provenance, citations, permissions.

## 9. Entity Resolution

Aliases, duplicate candidates, merge confidence, source evidence, rollback.

## 10. Product Recommendation

Users, products, categories, purchases, views, similarities, precomputed score edges.

## 11. Financial Transaction Graph

Accounts, transfers, merchants, devices. Detect suspicious loops and shared signals.

## 12. Data Lineage

Datasets, jobs, tables, columns, dashboards. Trace impact and compliance lineage.

---

## Strong Case Study Answer

```text
I would choose Neo4j when the product question depends on connected paths, not just filtering records. I would model entities and relationship types from the core traversals, add constraints for identity, add indexes for query anchors, bound path depth and fan-out, validate EXPLAIN/PROFILE, and define freshness and operational SLOs. If the graph is only used for reporting or simple lookup, I would prefer SQL/search/analytics stores and possibly derive a graph projection.
```

---

## Revision Notes

- One-line summary: Neo4j system design is won by naming graph model, traversal boundary, and failure modes.
- Three keywords: model, traversal, alternative.
- One interview trap: drawing a graph without naming query anchors.
- Memory trick: every graph design needs a path and a stop condition.