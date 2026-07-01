# Neo4j Installation, Browser, and Cypher Basics - Gold Sheet

> Track File #3 of 30 - Group 01: Starter Path
> For: backend/data interviews | Level: beginner | Mode: local setup and first commands

This sheet builds:
- Local Neo4j workflow
- Browser and cypher-shell basics
- First Cypher commands for inspection and learning

---

## 1. Local Setup Mental Model

Neo4j has two common local surfaces:

- HTTP Browser UI on port `7474`
- Bolt database protocol on port `7687`

The runnable lab uses Docker Compose and `cypher-shell` inside the container.

---

## 2. First Commands

```cypher
RETURN 1 AS ok;
MATCH (n) RETURN count(n) AS nodes;
MATCH ()-[r]->() RETURN type(r), count(r) ORDER BY count(r) DESC;
```

---

## 3. Create And Query

```cypher
CREATE (:Person {personId: 'p1', name: 'Maya'});
CREATE (:Person {personId: 'p2', name: 'Ravi'});

MATCH (a:Person {personId: 'p1'}), (b:Person {personId: 'p2'})
CREATE (a)-[:KNOWS {since: date('2026-01-01')}]->(b);

MATCH (a:Person)-[:KNOWS]->(b:Person)
RETURN a.name, b.name;
```

---

## 4. Browser vs cypher-shell

| Tool | Use |
|---|---|
| Neo4j Browser | visual exploration, ad hoc Cypher, graph view |
| cypher-shell | scripts, automation, repeatable labs |
| drivers | application integration |

---

## 5. Strong Answer

Question:

> How would you start learning Neo4j practically?

Strong answer:

```text
I would run Neo4j locally, create a small graph, add constraints, and query it with Cypher. I would use Browser for visual inspection and cypher-shell for repeatable scripts. The first goal is to understand how pattern matching traverses relationships, then move to indexed anchors, query plans, and bounded traversals.
```

---

## 6. Revision Notes

- One-line summary: Neo4j learning starts by drawing and querying a small graph.
- Three keywords: Browser, Bolt, Cypher.
- One interview trap: learning UI clicks without understanding graph patterns.
- Memory trick: create two nodes, connect them, then traverse.