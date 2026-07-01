# Neo4j Constraints, Indexes, Full-Text, and Vector Search - Gold Sheet

> Track File #7 of 30 - Group 02: Intermediate Backend
> For: backend/data/GenAI interviews | Level: intermediate | Mode: lookup anchors, integrity, search, vector retrieval

This sheet builds:
- Constraint and index basics
- Why indexed anchors matter for traversal
- Full-text and vector index positioning

---

## 1. Constraints Protect Identity

Use uniqueness constraints for natural or system identifiers.

```cypher
CREATE CONSTRAINT user_id_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.userId IS UNIQUE;

CREATE CONSTRAINT product_id_unique IF NOT EXISTS
FOR (p:Product) REQUIRE p.productId IS UNIQUE;
```

Why it matters:

- prevents duplicate identity nodes
- makes `MERGE` reliable
- supports fast lookup anchors

---

## 2. Indexes Support Starting Points

Traversals should start from indexed anchors.

```cypher
CREATE INDEX account_status_index IF NOT EXISTS
FOR (a:Account) ON (a.status);
```

Good:

```cypher
MATCH (u:User {userId: $userId})-[:FRIEND_OF*1..2]->(candidate:User)
RETURN candidate;
```

Bad:

```cypher
MATCH (u:User)-[:FRIEND_OF*1..2]->(candidate:User)
WHERE u.userId = $userId
RETURN candidate;
```

The planner may still optimize some cases, but the modeling habit should be anchor-first.

---

## 3. Full-Text Indexes

Full-text indexes help search text properties before graph traversal.

Use cases:

- find document chunks by text
- search names/descriptions
- start entity resolution from approximate text hits

Conceptual flow:

```text
text search -> candidate nodes -> graph expansion -> ranking/explanation
```

---

## 4. Vector Indexes

Neo4j supports vector search for embeddings in modern versions.

GraphRAG flow:

```text
embedding search -> candidate chunks/entities -> graph neighborhood expansion -> grounded answer context
```

Use vector search when semantic similarity matters. Use graph traversal when explicit relationships matter. Hybrid GraphRAG often uses both.

---

## 5. Strong Answer

```text
I use constraints to protect node identity and indexes to make query starting points selective. In Neo4j, indexes do not replace traversal design; they help find anchors before relationship expansion. Full-text and vector indexes are useful for search and GraphRAG entry points, but the graph still needs bounded traversal and permission-aware filtering.
```

---

## 6. Revision Notes

- One-line summary: Constraints protect identity; indexes find anchors before traversal.
- Three keywords: constraint, anchor, vector.
- One interview trap: thinking indexes speed every relationship expansion automatically.
- Memory trick: index the starting node, then traverse.