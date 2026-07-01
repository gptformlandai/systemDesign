# MongoDB Interview Questions

## Beginner

1. What is MongoDB?
2. What is a collection?
3. What is BSON?
4. What is ObjectId?
5. What is `_id`?
6. What is an embedded document?
7. What is a flexible schema?
8. What is the difference between MongoDB and SQL?
9. What is `insertOne` vs `insertMany`?
10. What is projection?

## Intermediate

1. When do you embed vs reference?
2. What is the subset pattern?
3. What is the bucket pattern?
4. What is an index?
5. Why do indexes slow writes?
6. What is the ESR rule?
7. What is a covered query?
8. What is the aggregation framework?
9. When is `$lookup` acceptable?
10. Why is deep `skip` pagination bad?

## Advanced

1. What is single-document atomicity?
2. When do you need transactions?
3. What is read concern?
4. What is write concern?
5. What is read preference?
6. What is a replica set?
7. What is the oplog?
8. What is replication lag?
9. What is sharding?
10. How do you choose a shard key?

## MAANG-Level

1. Design scalable chat storage in MongoDB.
2. Design a product catalog with search and filters.
3. Design an audit log system with retention.
4. Design a multi-tenant SaaS schema and shard key.
5. Design IoT telemetry ingestion.
6. Debug a slow query returning 20 docs but scanning 2 million.
7. Fix a schema with product reviews embedded in product documents.
8. Explain a replication lag incident.
9. Design a RAG document chunk store with vector search and ACL filters.
10. Compare MongoDB, PostgreSQL, Elasticsearch, Cassandra, and DynamoDB for a system design prompt.

For full expected answers, diagrams, and tradeoff tables, use `INTERVIEW_PREP/MAANG_DEEP_DIVE_QA.md`.

## MAANG Deep-Dive Drills

1. Your order API returns 20 rows but scans 2 million documents. Explain how you debug it and what index you would add.
2. A tenant with 70% of traffic makes one shard hot. Defend a new shard key or tenant isolation strategy.
3. A chat room has 10 million messages. Explain why embedding messages inside the conversation document fails.
4. Payment succeeds, but the user sees the old order status. Explain replication lag, read preference, and read concern/write concern choices.
5. Checkout transactions begin timing out under peak traffic. Explain transaction overhead and redesign options.
6. A dashboard aggregation spills to disk and times out. Explain pipeline, index, and materialized-summary fixes.
7. Audit logs must support compliance search and retention. Design schema, indexes, immutability controls, and failure handling.
8. An event ingestion pipeline receives 100k events/sec. Choose write model, index budget, rollups, backpressure, and archive path.
9. A RAG app leaks answers from restricted documents. Explain metadata, tenant, ACL, and vector-search filtering fixes.
10. The interviewer asks why not PostgreSQL. Give a balanced workload-driven answer, not a database popularity answer.

## Strong Follow-Up Topics

For every answer, be ready to discuss:

- latency
- throughput
- consistency
- failure modes
- index cost
- schema evolution
- backup/restore
- security
- sharding and tenant skew

## Full Drill Sheet

For a senior-backend-ready 100-question practice sheet with concise answers, examples, traps, follow-ups, diagrams, system design prompts, debugging scenarios, performance tuning, sharding, transactions, and schema design questions, use `INTERVIEW_PREP/MONGODB_100_INTERVIEW_DRILLS.md`.
