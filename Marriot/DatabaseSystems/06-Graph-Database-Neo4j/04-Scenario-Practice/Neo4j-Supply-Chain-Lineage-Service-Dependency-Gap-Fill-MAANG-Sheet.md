# Neo4j Supply Chain, Lineage, and Service Dependency Graph - Gap Fill MAANG Sheet

> Track File #21 of 30 - Group 04: Scenario Practice
> For: backend/platform/data/system design interviews | Level: senior / MAANG | Mode: dependency graph, blast radius, lineage, risk propagation

This sheet fills three related scenario families:

- supply-chain risk propagation
- service dependency blast radius
- data lineage impact analysis

All three ask the same graph question:

```text
If this node changes or fails, what connected things are impacted?
```

---

## 1. Supply Chain Graph

Model:

```text
(:Supplier)-[:SUPPLIES]->(:Part)-[:USED_IN]->(:Product)
(:Supplier)-[:LOCATED_IN]->(:Region)
(:Product)-[:SOLD_IN]->(:Market)
```

Use cases:

- vendor outage impact
- part recall blast radius
- regulatory region exposure
- alternative supplier search

---

## 2. Service Dependency Graph

Model:

```text
(:Service)-[:CALLS]->(:Service)
(:Service)-[:DEPENDS_ON]->(:Database)
(:Service)-[:PUBLISHES_TO]->(:Queue)
(:Service)-[:OWNED_BY]->(:Team)
```

Use cases:

- outage blast radius
- deployment risk
- owner lookup
- incident routing

---

## 3. Data Lineage Graph

Model:

```text
(:Dataset)-[:GENERATED_BY]->(:Job)
(:Job)-[:READS_FROM]->(:Dataset)
(:Dashboard)-[:USES]->(:Dataset)
(:Column)-[:DERIVED_FROM]->(:Column)
```

Use cases:

- schema-change impact
- compliance lineage
- stale data investigation
- dashboard trust scoring

---

## 4. Blast-Radius Query Shape

```cypher
MATCH path = (start:Service {serviceId: $serviceId})<-[:CALLS|DEPENDS_ON*1..3]-(impacted)
RETURN impacted, length(path) AS distance
ORDER BY distance
LIMIT 100;
```

Production notes:

- direction must match impact semantics
- depth must be bounded
- relationship types should be explicit
- stale topology is a serious risk
- high-degree shared infrastructure nodes need careful treatment

---

## 5. Freshness And Ownership

Dependency graphs go stale quickly unless fed by platform events.

Sources:

- CI/CD deployment metadata
- service registry
- OpenTelemetry traces
- infrastructure inventory
- data catalog events
- vendor/part master data

Track:

- last observed dependency time
- confidence/source
- owner/team
- criticality
- environment

---

## 6. Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| wrong blast radius | stale dependency graph | freshness alerts and reconciliation |
| runaway traversal | unbounded dependency path | depth and relationship-type limits |
| noisy shared node | common infra dependency | classify and filter shared services |
| missing owner | incomplete service catalog | ownership constraint/reconciliation |
| misleading lineage | missing column-level edge | confidence/provenance and review |

---

## 7. Strong Interview Answer

```text
For supply-chain, service-dependency, or lineage graphs, I would model entities and dependency relationships in the direction that answers impact questions. Queries start from a changed or failed node, traverse explicit dependency relationships with bounded depth, return impacted nodes with distance and ownership, and include source/provenance timestamps. I would monitor graph freshness because stale dependencies can make incident response or compliance answers dangerously wrong.
```

---

## 8. Revision Notes

- One-line summary: Dependency graphs answer blast-radius and lineage questions only when direction, freshness, and bounds are correct.
- Three keywords: dependency, lineage, blast radius.
- One interview trap: drawing dependencies without defining impact direction.
- Memory trick: ask what breaks if this node disappears.