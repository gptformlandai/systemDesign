# Neo4j Cypher Querying: MATCH, WHERE, RETURN - MAANG Master Sheet

> Track File #5 of 30 - Group 02: Intermediate Backend
> For: backend/data/system design interviews | Level: intermediate | Mode: Cypher query fluency and pattern reasoning

This sheet builds:
- Practical Cypher fluency
- Query pattern composition
- Common follow-ups: optional matches, aggregation, UNWIND, subqueries

---

## 1. Query Shape

```cypher
MATCH (start:Label {id: $id})-[:REL_TYPE]->(next:Label)
WHERE next.status = 'ACTIVE'
RETURN next
ORDER BY next.score DESC
LIMIT 20;
```

Think:

```text
anchor -> pattern -> filter -> aggregate/project -> limit
```

---

## 2. OPTIONAL MATCH

Use `OPTIONAL MATCH` when a relationship may not exist.

```cypher
MATCH (u:User {userId: $userId})
OPTIONAL MATCH (u)-[:HAS_PROFILE]->(profile:Profile)
RETURN u, profile;
```

Trap: optional expansions can multiply rows if not scoped carefully.

---

## 3. Aggregation

```cypher
MATCH (u:User)-[:BOUGHT]->(p:Product)
RETURN p.category AS category, count(*) AS purchases
ORDER BY purchases DESC;
```

Cypher groups by all non-aggregated return expressions.

---

## 4. UNWIND For Batch Inputs

```cypher
UNWIND $events AS event
MERGE (u:User {userId: event.userId})
MERGE (p:Product {productId: event.productId})
MERGE (u)-[:BOUGHT {orderId: event.orderId}]->(p);
```

Use `UNWIND` for batch writes and parameterized lists.

---

## 5. Subqueries

Subqueries isolate logic and control row multiplication.

```cypher
MATCH (u:User {userId: $userId})
CALL {
  WITH u
  MATCH (u)-[:BOUGHT]->(p:Product)<-[:BOUGHT]-(other:User)
  RETURN other, count(*) AS shared
  ORDER BY shared DESC
  LIMIT 10
}
RETURN other.userId, shared;
```

---

## 6. Strong Answer

Question:

> How do you write production-safe Cypher?

Strong answer:

```text
I start from indexed anchors, use explicit labels and relationship types, bound variable-length paths, avoid accidental Cartesian products, parameterize inputs, and inspect EXPLAIN/PROFILE for cardinality and db hits. For multi-step logic, I use subqueries to control row scope and limits.
```

---

## 7. Revision Notes

- One-line summary: Good Cypher makes the traversal path explicit and bounded.
- Three keywords: MATCH, UNWIND, PROFILE.
- One interview trap: matching disconnected patterns that create Cartesian products.
- Memory trick: every Cypher row can multiply; control scope.