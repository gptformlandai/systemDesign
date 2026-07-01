# Neo4j Permission Graph and Access Control - Gap Fill MAANG Sheet

> Track File #20 of 30 - Group 04: Scenario Practice
> For: backend/security/system design interviews | Level: senior / MAANG | Mode: permission graph, RBAC, inherited access, deny rules

This sheet fills a high-value graph scenario that interviewers often use to test graph-modeling maturity:

```text
Can this user access this resource, and why?
```

---

## 1. Requirements

- users can belong to groups
- groups can inherit from other groups
- groups or roles grant access to resources
- explicit deny can override allow
- tenant boundaries must be enforced
- answer must explain the access path
- permission checks need very low latency

Typical SLO:

```text
permission check p99 < 50 ms to 100 ms, depending on cache and graph depth
```

---

## 2. Graph Model

```text
(:User)-[:MEMBER_OF]->(:Group)
(:Group)-[:MEMBER_OF]->(:Group)
(:Group)-[:HAS_ROLE]->(:Role)
(:Role)-[:GRANTS]->(:Permission)
(:Permission)-[:APPLIES_TO]->(:Resource)
(:User)-[:DENIED]->(:Resource)
```

Useful properties:

- `tenantId`
- `resourceId`
- `action`
- `scope`
- `source`
- `expiresAt`

---

## 3. Access Query Shape

```cypher
MATCH (u:User {userId: $userId, tenantId: $tenantId})
MATCH (r:Resource {resourceId: $resourceId, tenantId: $tenantId})
OPTIONAL MATCH denyPath = (u)-[:DENIED]->(r)
WITH u, r, denyPath
WHERE denyPath IS NULL
MATCH allowPath = (u)-[:MEMBER_OF*1..3]->(:Group)-[:HAS_ROLE]->(:Role)-[:GRANTS]->(p:Permission)-[:APPLIES_TO]->(r)
WHERE p.action = $action
RETURN allowPath
LIMIT 1;
```

Notes:

- start from indexed user and resource anchors
- bound group inheritance depth
- enforce tenant filter in every anchor
- handle deny precedence before allow
- return the path for explainability

---

## 4. Caching Strategy

Permission checks are often hot-path queries.

Options:

- cache final decisions briefly
- cache compiled permission sets per user/resource scope
- invalidate on membership or role changes
- keep Neo4j as explainable source for complex paths

Risk:

```text
Permission caches are dangerous without clear invalidation and stale-access SLOs.
```

---

## 5. Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| access leak | tenant filter missing | mandatory server-side query builder and tests |
| deny ignored | allow query runs first | deny precedence model and tests |
| slow check | deep group hierarchy | depth limit, cache, flatten/precompute |
| stale access | membership sync lag | freshness SLO and invalidation |
| path confusion | generic relationship types | explicit role/permission/resource model |

---

## 6. Strong Interview Answer

```text
For a permission graph, I would model users, groups, roles, permissions, and resources with explicit relationships such as MEMBER_OF, HAS_ROLE, GRANTS, APPLIES_TO, and DENIED. Permission queries start from indexed user and resource anchors, enforce tenant filters, check deny precedence, and traverse bounded group inheritance paths. For low latency, I would cache decisions carefully with invalidation, but keep Neo4j as the explainable source for complex access paths.
```

---

## 7. Revision Notes

- One-line summary: Permission graphs need bounded inheritance, tenant filters, deny precedence, and explainable access paths.
- Three keywords: RBAC, deny, path.
- One interview trap: applying tenant/security filters after traversal.
- Memory trick: permission graph answer must say both yes/no and why.