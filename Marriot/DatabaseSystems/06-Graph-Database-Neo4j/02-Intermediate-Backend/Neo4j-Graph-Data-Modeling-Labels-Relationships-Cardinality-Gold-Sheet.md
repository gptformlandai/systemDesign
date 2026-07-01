# Neo4j Graph Data Modeling: Labels, Relationships, and Cardinality - Gold Sheet

> Track File #6 of 30 - Group 02: Intermediate Backend
> For: backend/data/system design interviews | Level: intermediate | Mode: graph modeling, access patterns, cardinality

This sheet builds:
- How to model graphs from questions
- Cardinality and fan-out awareness
- Supernode and relationship semantics judgment

---

## 1. Start From Questions

Bad start:

```text
We have users, orders, products, and devices. Let's convert tables to nodes.
```

Better start:

```text
What connected question must be answered quickly?
```

Examples:

- Which accounts share devices with known fraud accounts?
- Which products are recommended because similar users bought them?
- Which services are impacted if this database fails?
- Which documents support this answer path?

---

## 2. Relationship Type Design

Prefer meaningful relationship types:

```text
(:User)-[:USED_DEVICE]->(:Device)
(:Account)-[:TRANSFERRED_TO]->(:Account)
(:Service)-[:DEPENDS_ON]->(:Service)
(:Chunk)-[:MENTIONS]->(:Entity)
```

Avoid generic `RELATED_TO` unless there is a strong ontology reason.

---

## 3. Cardinality And Fan-Out

Ask for every relationship:

- one-to-one?
- one-to-many?
- many-to-many?
- can one node have millions of relationships?
- will traversal start or end at high-degree nodes?

Supernode risk:

```text
One node with huge relationship degree can dominate traversal cost and lock/write contention.
```

---

## 4. Relationship Properties

Use relationship properties for connection-specific facts:

```cypher
(:User)-[:LOGGED_IN_FROM {firstSeen: date(), lastSeen: date(), count: 23}]->(:Device)
```

Do not create extra nodes for every event unless event identity or traversal is needed.

---

## 5. Model Review Checklist

- What are the top 5 queries?
- What is the starting anchor for each query?
- Which relationships are traversed and how deep?
- Which nodes can become supernodes?
- What constraints protect identity?
- What indexes support anchors?
- What data is source of truth?
- What is the freshness SLO?

---

## 6. Strong Answer

```text
I model Neo4j from traversal requirements, not table structure. Labels identify entity types, relationship types encode domain meaning, direction improves readability, and relationship properties store facts about the connection. Then I review cardinality and fan-out to avoid supernodes and unbounded traversal. The model is only good if the key Cypher queries start from selective anchors and stay bounded.
```

---

## 7. Revision Notes

- One-line summary: Graph modeling is access-pattern design for connected questions.
- Three keywords: relationship type, cardinality, supernode.
- One interview trap: table-to-node translation without traversal review.
- Memory trick: model the question, not the schema dump.