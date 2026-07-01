    # MongoDB Active Recall Question Bank - Gold Sheet

    > **Track File #25 of 28 - Group 06: Practice Upgrade**
    > For: backend/database/system design interviews | Level: MAANG interview readiness | Mode: answer from memory, no notes

    This sheet builds:
    - Foundation, intermediate, advanced, and MAANG recall prompts
- Question tiers by topic
- Answer references mapped to modular sheets

    How to use this:
    - Read the mental model first.
    - Practice the commands and examples in `mongosh` or a driver.
    - Say the interview answers out loud in 30-90 seconds.
    - Revisit the anti-patterns before designing production schemas.

    ---
## 1. How to Use This Sheet

Rules:

1. Cover the answer references and answer out loud.
2. Mark each answer: cold, partial, or blank.
3. Revisit blank answers the next day.
4. Revisit partial answers in three days.
5. A topic is mastered only when you can explain the tradeoff, not just the definition.

Difficulty tiers:

- Foundation: must answer instantly.
- Intermediate: answer with examples.
- Advanced: include failure modes and performance implications.
- MAANG: defend the design under scale, skew, and failure.

## 2. Foundation Recall

### 2-A Document Model

1. What is MongoDB in one sentence?
2. What is the difference between a database, collection, document, and field?
3. Why does MongoDB use BSON instead of plain JSON?
4. What is `_id` and why is it indexed?
5. What is ObjectId and what does it roughly contain?
6. What is the 16 MB document limit and why does it matter?
7. What does schema-flexible mean? Why is schema-less a dangerous phrase?
8. What is an embedded document?
9. What is an array field?
10. How does MongoDB map to SQL concepts?

Answer reference: MongoDB-Core-Architecture-BSON-ObjectId-Gold-Sheet.md

### 2-B CRUD and Querying

1. When do you use `insertOne` vs `insertMany`?
2. What is ordered vs unordered bulk insert?
3. What is projection and why does it matter?
4. What is the problem with deep `skip` pagination?
5. How do `$set`, `$unset`, `$inc`, `$push`, `$pull`, and `$addToSet` differ?
6. What is an upsert?
7. What is dot notation?
8. Why does `$elemMatch` matter for arrays of objects?
9. Why can regex be dangerous for performance?
10. What is the difference between missing field and `null`?

Answer reference: MongoDB-CRUD-Operations-Deep-Dive-Gold-Sheet.md and MongoDB-Query-Language-Operators-Gold-Sheet.md

## 3. Intermediate Recall

### 3-A Schema Design

1. What does it mean to design around query patterns?
2. When should you embed?
3. When should you reference?
4. What is the subset pattern?
5. What is the bucket pattern?
6. What is the outlier pattern?
7. What is the computed pattern?
8. What is the extended reference pattern?
9. What is the attribute pattern?
10. What is the CQRS-style read model pattern?

Answer reference: MongoDB-Data-Modeling-Schema-Design-MAANG-Master-Sheet.md

### 3-B Indexing

1. What is an index?
2. Why do indexes speed reads but slow writes?
3. What are cardinality and selectivity?
4. What is a covered query?
5. What is the compound index prefix rule?
6. What is the ESR rule?
7. What is the difference between `IXSCAN` and `COLLSCAN`?
8. What do `keysExamined`, `docsExamined`, and `nReturned` tell you?
9. When is a partial index useful?
10. Why is over-indexing bad?

Answer reference: MongoDB-Indexing-Explain-ESR-MAANG-Master-Sheet.md

### 3-C Aggregation

1. What is the pipeline mental model?
2. Why should `$match` usually come early?
3. When does `$unwind` increase cardinality?
4. When is `$lookup` acceptable?
5. Why can `$group` be dangerous on high-cardinality data?
6. What does `$facet` help with?
7. When should you use `$merge`?
8. What does `$setWindowFields` enable?
9. What is `allowDiskUse` and why is it not a free lunch?
10. How would you build a sales report pipeline?

Answer reference: MongoDB-Aggregation-Framework-MAANG-Master-Sheet.md

## 4. Advanced Recall

### 4-A Transactions and Consistency

1. What is single-document atomicity?
2. When do you need a multi-document transaction?
3. What is a session?
4. What is read concern?
5. What is write concern?
6. What is read preference?
7. What are retryable writes?
8. What is causal consistency?
9. Why are long transactions risky?
10. When is schema redesign better than a transaction?

Answer reference: MongoDB-Transactions-Consistency-MAANG-Master-Sheet.md

### 4-B Replication

1. What is a replica set?
2. What does the primary do?
3. What does a secondary do?
4. What is the oplog?
5. What happens during an election?
6. Why does majority write concern matter?
7. What is replication lag?
8. How can stale reads happen?
9. What is rollback after failover?
10. When would you read from secondaries?

Answer reference: MongoDB-Replication-High-Availability-MAANG-Master-Sheet.md

### 4-C Sharding

1. What is sharding?
2. What does `mongos` do?
3. What do config servers store?
4. What is a chunk?
5. What is the balancer?
6. What makes a good shard key?
7. Why is timestamp-only often a bad shard key?
8. Why can tenantId-only be bad in multi-tenant SaaS?
9. What is scatter-gather?
10. What is zone sharding?

Answer reference: MongoDB-Sharding-Distributed-Scale-MAANG-Master-Sheet.md

## 5. Production Recall

1. How do you debug a slow query?
2. What metrics belong on a MongoDB dashboard?
3. How do you design secure MongoDB access?
4. What is RPO vs RTO?
5. How do you test backups?
6. How do change streams use the oplog?
7. What is a resume token?
8. When are time-series collections useful?
9. When should you use Atlas Search instead of text index?
10. When is MongoDB Vector Search enough for RAG?

Answer references: performance, security, backup, change streams, time-series, search/vector sheets.

## 6. MAANG System Design Recall

1. Design a product catalog in MongoDB. Include schema, indexes, search, and scaling.
2. Design shopping cart storage. Explain why cart can be a single document and where it breaks.
3. Design order management with payment and inventory consistency.
4. Design chat message storage for large conversations.
5. Design audit logs with retention and query patterns.
6. Design IoT telemetry ingestion and aggregation.
7. Design RAG chunk storage with ACL-safe vector search.
8. Design multi-tenant SaaS. Choose indexes and shard key.
9. Design real-time analytics with raw events and materialized summaries.
10. Design social feed storage and explain fanout tradeoffs.

Answer reference: MongoDB-System-Design-Case-Studies-MAANG-Master-Sheet.md

## 7. Debugging Prompts

1. A query returns 20 docs but examines 2 million. What do you inspect?
2. A write-heavy collection slowed down after adding indexes. Why?
3. A dashboard aggregation times out. What are your options?
4. A secondary is 20 minutes behind. What could cause it?
5. A sharded cluster has one hot shard. What do you check?
6. Product reviews are stored inside product documents and some products approach 16 MB. Fix the schema.
7. A `$lookup` API has p99 latency spikes. How do you redesign?
8. Users see stale order status after payment. What consistency/read preference issue might exist?
9. A change stream consumer reprocesses events after restart. How do you make it safe?
10. A RAG app leaks documents across tenants. What filters and tests are missing?

Answer reference: MongoDB-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md
