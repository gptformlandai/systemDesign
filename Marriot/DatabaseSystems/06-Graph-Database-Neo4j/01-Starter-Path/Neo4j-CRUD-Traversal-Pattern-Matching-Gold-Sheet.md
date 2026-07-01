# Neo4j CRUD, Traversal, and Pattern Matching - Gold Sheet

> Track File #4 of 30 - Group 01: Starter Path
> For: backend/data interviews | Level: beginner | Mode: CRUD plus traversal basics

This sheet builds:
- Core Cypher CRUD operations
- Relationship traversal basics
- The difference between lookup and traversal

---

## 1. Create And Merge

`CREATE` always creates. `MERGE` finds or creates a pattern.

```cypher
CREATE (:User {userId: 'u1', name: 'Asha'});

MERGE (u:User {userId: 'u1'})
ON CREATE SET u.createdAt = datetime()
ON MATCH SET u.lastSeenAt = datetime();
```

Use `MERGE` with constraints for idempotent imports.

---

## 2. Match And Traverse

```cypher
MATCH (u:User {userId: 'u1'})-[:BOUGHT]->(p:Product)
RETURN p.productId, p.name;
```

This query starts at an indexed user anchor, then traverses outgoing `BOUGHT` relationships.

---

## 3. Update And Delete

```cypher
MATCH (u:User {userId: 'u1'})
SET u.status = 'ACTIVE';

MATCH (u:User {userId: 'u1'})-[r:BOUGHT]->(:Product {productId: 'p1'})
DELETE r;

MATCH (u:User {userId: 'u1'})
DETACH DELETE u;
```

`DETACH DELETE` removes a node and its relationships. Use carefully.

---

## 4. Path Patterns

```cypher
MATCH path = (u:User {userId: 'u1'})-[:FRIEND_OF*1..2]->(candidate:User)
RETURN candidate.userId, length(path) AS hops;
```

Always bound variable-length paths unless there is a proven reason not to.

---

## 5. Strong Answer

Question:

> What is the basic Cypher pattern for graph traversal?

Strong answer:

```text
I start from a selective node, usually found by a unique constraint or index, then traverse typed relationships with a bounded pattern. For example, find a user by userId, traverse BOUGHT or FRIEND_OF relationships, and return connected nodes or paths. I avoid starting from all nodes or using unbounded variable-length paths because that creates fan-out and slow queries.
```

---

## 6. Revision Notes

- One-line summary: Cypher queries should start from selective anchors and traverse meaningful relationships.
- Three keywords: MATCH, MERGE, path.
- One interview trap: unbounded `*` traversals.
- Memory trick: anchor first, traverse second.