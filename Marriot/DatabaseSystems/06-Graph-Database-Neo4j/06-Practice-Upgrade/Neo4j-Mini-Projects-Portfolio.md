# Neo4j Mini Projects Portfolio

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: backend/data/system design interviews | Level: beginner to pro | Mode: portfolio projects and interview discussion

Each project should include requirements, graph model, constraints, sample data, Cypher queries, traversal limits, scaling concerns, security concerns, and interview talking points.

---

## 1. Social Recommendation Engine

Build:

- `User`, `Content`, `Product`
- `FOLLOWS`, `LIKED`, `BOUGHT`, `BLOCKED`
- two-hop recommendation queries

Discuss:

- celebrity nodes
- privacy/block filters
- precomputation vs online traversal

---

## 2. Fraud Ring Detection

Build:

- `Account`, `Device`, `Email`, `Card`, `IpAddress`, `RiskEvent`
- shared signal traversals
- investigator query examples

Discuss:

- false positives
- signal weighting
- streaming updates

---

## 3. Knowledge Graph / GraphRAG

Build:

- `Document`, `Chunk`, `Entity`, `Source`, `Fact`
- chunk-to-entity and fact provenance relationships
- retrieval and graph expansion queries

Discuss:

- entity resolution quality
- citation correctness
- permission filtering

---

## 4. Access Control Graph

Build:

- `User`, `Group`, `Role`, `Resource`
- `MEMBER_OF`, `HAS_ROLE`, `CAN_ACCESS`, `DENIED`

Discuss:

- deny precedence
- inherited permissions
- tenant isolation

---

## 5. Service Dependency Graph

Build:

- `Service`, `Database`, `Queue`, `Endpoint`
- `DEPENDS_ON`, `CALLS`, `PUBLISHES_TO`, `CONSUMES_FROM`

Discuss:

- outage blast radius
- deployment risk
- stale topology data

---

## 6. Supply Chain / Data Lineage Impact Graph

Build:

- `Supplier`, `Part`, `Product`, `Region`, `Dataset`, `Job`, `Dashboard`
- `SUPPLIES`, `USED_IN`, `LOCATED_IN`, `READS_FROM`, `WRITES_TO`, `USES`

Discuss:

- recall blast radius
- schema-change impact
- freshness and provenance quality

---

## Portfolio Scoring

| Area | What To Prove |
|---|---|
| use case | relationship-heavy question is clear |
| model | labels and relationship types are defensible |
| traversal | starting anchors and depth bounds are named |
| integrity | constraints protect identity |
| performance | PROFILE/fan-out/supernode risks covered |
| security | tenant/path/relationship access covered |
| alternatives | knows when Neo4j is wrong |

MAANG-ready portfolio:

```text
At least 4 projects can be explained end-to-end in 10 minutes each with follow-up answers.
```