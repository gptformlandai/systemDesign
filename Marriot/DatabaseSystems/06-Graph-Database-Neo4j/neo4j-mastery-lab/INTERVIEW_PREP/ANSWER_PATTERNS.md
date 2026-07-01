# Neo4j Answer Patterns

## Design Answer

```text
I start from the relationship-heavy domain question. For <use case>, I would model <labels> connected by <relationship types>, with relationship properties for <connection facts>. Queries start from <indexed anchor>, traverse <bounded paths>, apply <filters/security>, and return <result>. I would validate PROFILE, monitor <latency/fan-out/hot nodes/sync lag>, and use <alternative> if the workload is not relationship-centric.
```

## Debugging Answer

```text
I start with the exact Cypher and parameters. Then I inspect EXPLAIN/PROFILE for anchor usage, label scans, row multiplication, db hits, Cartesian products, variable-length paths, returned payload, and high-degree nodes. Durable fixes may be indexes, query rewrites, subquery limits, or graph model changes.
```

## Tradeoff Answer

```text
Neo4j gives natural relationship traversal, path explanation, graph algorithms, and knowledge graph modeling. The cost is graph modeling discipline, fan-out control, operational memory/storage tuning, sync quality, and knowing when SQL/search/vector/analytics systems are better.
```

## Permission Graph Answer

```text
I model users, groups, roles, permissions, and resources with explicit relationships for membership, grants, resource scope, and deny rules. The query starts from indexed user and resource anchors, enforces tenant filters, checks deny precedence, traverses bounded group inheritance, and returns the path that explains the decision. I would cache carefully, but track stale-permission risk and invalidate on membership or role changes.
```

## Dependency And Lineage Answer

```text
I model services, infrastructure, datasets, jobs, dashboards, suppliers, parts, and products in the direction that answers impact questions. Blast-radius and lineage queries start from the changed or failed node, traverse explicit relationships with bounded depth, return impacted nodes with distance and ownership, and include provenance/freshness metadata. The main failure mode is stale or directionally wrong dependency data.
```