# Neo4j Graph Model: Nodes, Relationships, and Properties - Gold Sheet

> Track File #2 of 30 - Group 01: Starter Path
> For: backend/data/system design interviews | Level: beginner | Mode: graph primitives and modeling intuition

This sheet builds:
- Native graph model vocabulary
- How labels, relationship types, direction, and properties fit together
- Basic modeling judgment for interviews

---

## 1. Graph Building Blocks

| Concept | Meaning | Example |
|---|---|---|
| node | entity | `(:User)`, `(:Product)`, `(:Account)` |
| label | node category | `:User`, `:Merchant`, `:Device` |
| relationship | typed connection | `[:BOUGHT]`, `[:USES]`, `[:KNOWS]` |
| direction | traversal meaning | `(:User)-[:BOUGHT]->(:Product)` |
| property | key/value data | `{userId: "u1", risk: 0.82}` |
| path | connected sequence | user to device to account |

---

## 2. Relationship Direction

Direction should match domain meaning.

```cypher
(:Customer)-[:PLACED]->(:Order)
(:Order)-[:CONTAINS]->(:Product)
(:Person)-[:WORKS_FOR]->(:Company)
```

You can traverse either direction in Cypher, but direction improves readability and modeling discipline.

---

## 3. Properties On Nodes vs Relationships

Put stable entity attributes on nodes:

```cypher
(:User {userId: 'u1', status: 'ACTIVE'})
```

Put relationship-specific facts on relationships:

```cypher
(:User)-[:BOUGHT {quantity: 2, at: datetime()}]->(:Product)
```

Rule:

```text
If the value describes the connection, put it on the relationship.
```

---

## 4. Labels And Relationship Types

Labels help classify and index nodes. Relationship types define traversal semantics.

Bad:

```text
(:Thing)-[:RELATED_TO]->(:Thing)
```

Better:

```text
(:User)-[:USES_DEVICE]->(:Device)
(:Account)-[:PAID_WITH]->(:Card)
(:Service)-[:DEPENDS_ON]->(:Service)
```

Specific relationships make queries readable and bounded.

---

## 5. Strong Answer

Question:

> How do you model data in Neo4j?

Strong answer:

```text
I start from the domain questions and traversals. Entities become nodes with labels and properties. Meaningful connections become typed, directed relationships, with relationship properties when facts belong to the connection. Then I add constraints and indexes for lookup anchors, and I test the model against the most important traversals to avoid vague relationship types, hot nodes, and unbounded fan-out.
```

---

## 6. Revision Notes

- One-line summary: A graph model is a domain model optimized for traversals.
- Three keywords: label, relationship type, path.
- One interview trap: modeling every join table as a node.
- Memory trick: entities are nodes; verbs are relationships.