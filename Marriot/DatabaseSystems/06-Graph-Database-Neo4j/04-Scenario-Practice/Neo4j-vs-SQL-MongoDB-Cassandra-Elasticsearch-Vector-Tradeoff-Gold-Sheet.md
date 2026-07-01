# Neo4j vs SQL, MongoDB, Cassandra, Elasticsearch, Vector DBs, and RDF - Tradeoff Gold Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: backend/data/system design interviews | Level: senior | Mode: tool selection and tradeoffs

This sheet builds:
- Clear Neo4j comparison language
- When graph database is right or wrong
- MAANG-level database selection judgment

---

## 1. Neo4j vs PostgreSQL

| Neo4j | PostgreSQL |
|---|---|
| relationship traversal | relational source of truth |
| graph paths and patterns | joins, constraints, SQL analytics |
| natural recursive dependencies | strong transactional reporting patterns |
| graph algorithms and connected queries | mature relational ecosystem |

Use together:

```text
PostgreSQL stores transactional truth; Neo4j serves relationship-heavy graph queries.
```

---

## 2. Neo4j vs MongoDB

MongoDB stores document aggregates. Neo4j stores connected entities and relationships.

Choose MongoDB for document-centric CRUD. Choose Neo4j when relationships and paths are central.

---

## 3. Neo4j vs Cassandra

Cassandra is optimized for high-scale predictable query tables. Neo4j is optimized for relationship traversal.

Choose Cassandra for massive write/read access patterns. Choose Neo4j for connected questions.

---

## 4. Neo4j vs Elasticsearch

Elasticsearch is search/retrieval over indexed documents. Neo4j is graph traversal over connected entities.

Use together:

```text
Elasticsearch finds candidate documents/entities; Neo4j expands relationship context.
```

---

## 5. Neo4j vs Vector Databases

Vector DBs find semantically similar embeddings. Neo4j explains explicit relationships.

GraphRAG often combines both:

```text
vector similarity -> candidate chunks/entities -> graph traversal -> grounded answer
```

---

## 6. Neo4j vs RDF/Triplestores

RDF/triplestores are strong for semantic web, ontologies, standards, SPARQL, and linked data. Neo4j is often strong for property graph development, Cypher, app integration, and operational graph workloads.

Choose based on standards, ontology depth, team skill, query style, and integration needs.

---

## 7. Strong Answer

```text
I choose Neo4j when the core workload is relationship traversal, path explanation, graph algorithms, or knowledge graph interaction. I avoid it for simple CRUD, warehouse analytics, or predictable partitioned lookups. In many architectures, Neo4j is a derived graph projection from SQL/Kafka/search/vector systems rather than the only database.
```

---

## 8. Revision Notes

- One-line summary: Neo4j is a graph traversal engine, not a universal database replacement.
- Three keywords: traversal, source of truth, alternative.
- One interview trap: saying graph DB is better because joins are bad.
- Memory trick: choose Neo4j when the path is the product.