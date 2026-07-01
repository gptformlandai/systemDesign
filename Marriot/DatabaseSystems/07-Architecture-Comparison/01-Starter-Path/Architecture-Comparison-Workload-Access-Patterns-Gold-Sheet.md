# Architecture Comparison Workload Access Patterns - Gold Sheet

> Track File #3 of 30 - Group 01: Starter Path
> For: system design interviews | Level: beginner | Mode: access pattern recognition

## 1. Why Access Patterns Matter

A database should be selected around how the application reads and writes data.

```text
same data, different query pattern -> different best datastore
```

---

## 2. Access Pattern Map

| Access Pattern | Strong Candidates |
|---|---|
| primary-key lookup | SQL, MongoDB, Cassandra, key-value store |
| transaction with constraints | PostgreSQL/MySQL |
| flexible JSON aggregate read | MongoDB/document store |
| huge append writes by partition key | Cassandra/wide-column |
| full-text search | Elasticsearch/OpenSearch |
| semantic similarity | Vector DB/Pinecone/Qdrant |
| relationship traversal | Neo4j/graph DB |
| hot cached read | Redis/cache |
| object/blob fetch | S3/object storage |
| metrics over time | time-series store |
| dashboard analytics | warehouse/lakehouse |

---

## 3. Access Pattern Questions

Ask:

- What is the exact query?
- What fields are filtered or sorted?
- Is the query bounded?
- Is the result set small or large?
- What is the write rate?
- What is the required freshness?
- Is the workload OLTP, search, graph, vector, streaming, or analytics?

---

## 4. Interview Summary

```text
I would first identify the access pattern: point lookup, transaction, range scan, full-text search, vector similarity, graph traversal, hot cache read, blob fetch, time-series query, or analytics scan. Then I would choose a datastore optimized for that pattern and name tradeoffs for consistency, scale, cost, and operations.
```

---

## 5. Revision Notes

- One-line summary: Access pattern is the quickest path to a good datastore decision.
- Three keywords: query, bound, freshness.
- One trap: saying “NoSQL scales” without naming the query.