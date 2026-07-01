# MongoDB Mastery Lab Learning Path

This path turns MongoDB from a tool you can use into a database you can design, operate, debug, and defend in interviews.

---

## Stage 1: Starter Foundations

Goal: become comfortable with the document model and shell operations.

Read:

- `NOTES/01-mongodb-fundamentals.md`
- `NOTES/02-document-model-bson-objectid.md`
- `NOTES/03-crud-query-language.md`
- `CHEATSHEETS/CRUD.md`
- `CHEATSHEETS/QUERY_OPERATORS.md`

Run:

```bash
docker compose up -d
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/01-crud.js
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/02-query-operators.js
```

Success criteria:

- You can explain database, collection, document, field, BSON, ObjectId, and `_id`.
- You can insert, query, update, and delete nested documents.
- You can query arrays and nested objects without guessing syntax.

Common mistakes:

- Treating MongoDB as schema-less.
- Storing arbitrary inconsistent JSON.
- Forgetting projections and limits.

---

## Stage 2: Intermediate Backend Modeling

Goal: design MongoDB schemas for real APIs.

Read:

- `NOTES/04-schema-design-patterns.md`
- `CHEATSHEETS/SCHEMA_DESIGN.md`
- `PROJECTS/01-user-profile-service.md`
- `PROJECTS/02-blog-platform.md`
- `PROJECTS/03-ecommerce-catalog.md`
- `PROJECTS/04-shopping-cart.md`
- `PROJECTS/05-order-system.md`

Run:

```bash
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/03-indexing-explain.js
```

Practice:

- Model user profiles, product catalogs, orders, carts, reviews, and audit logs.
- Decide embed vs reference for each relationship.
- Identify bounded and unbounded arrays.

Success criteria:

- You can explain why order line items are embedded.
- You can explain why product reviews should be a separate collection plus summary fields.
- You can design tenant-safe documents with `tenantId` included consistently.

---

## Stage 3: Indexing and Aggregation

Goal: make reads and analytics fast enough for production APIs.

Read:

- `NOTES/05-indexing-explain-query-planner.md`
- `NOTES/06-aggregation-framework.md`
- `CHEATSHEETS/INDEXING.md`
- `CHEATSHEETS/AGGREGATION.md`
- `PERFORMANCE/EXPLAIN_PLAYBOOK.md`

Run:

```bash
bash SCRIPTS/run-mongosh.sh SCRIPTS/create-indexes.js
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/04-aggregation.js
bash SCRIPTS/run-mongosh.sh SCRIPTS/aggregation-labs.js
```

Practice:

- Create compound indexes with ESR.
- Compare explain plans before and after indexes.
- Build sales, dashboard, and faceted-search aggregations.

Success criteria:

- You can read `IXSCAN`, `COLLSCAN`, `FETCH`, `SORT`, `keysExamined`, `docsExamined`, and `nReturned`.
- You can explain why over-indexing slows writes.
- You can move `$match` early and avoid huge `$lookup` and `$group` stages.

---

## Stage 4: Application Integration

Goal: connect MongoDB from real backend stacks.

Run Node.js:

```bash
cd EXAMPLES/nodejs
npm install
cp .env.example .env
npm run crud
npm run aggregation
```

Run Python:

```bash
cd EXAMPLES/python
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python crud.py
python aggregation.py
```

Run Spring Boot:

```bash
cd EXAMPLES/java-spring-boot
mvn spring-boot:run
```

Success criteria:

- You reuse one MongoDB client per process.
- You understand connection pools.
- You handle duplicate key errors.
- You keep database access behind repositories/services.

---

## Stage 5: Senior Production MongoDB

Goal: understand correctness, failure, scale, and operations.

Read:

- `NOTES/07-transactions-consistency.md`
- `NOTES/08-replication-sharding.md`
- `NOTES/10-production-operations.md`
- `CHEATSHEETS/TRANSACTIONS_REPLICATION_SHARDING.md`
- `PERFORMANCE/PERFORMANCE_TUNING_EXERCISES.md`
- `PERFORMANCE/MAANG_PRODUCTION_INCIDENT_PLAYBOOK.md`
- `SECURITY/SECURITY_CHECKLIST.md`
- `SECURITY/BACKUP_RESTORE_DR.md`

Run:

```bash
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/05-transactions.js
bash SCRIPTS/run-mongosh.sh SCRIPTS/performance-labs.js
```

Success criteria:

- You can explain single-document atomicity and when transactions are needed.
- You can explain majority write concern, stale secondary reads, and replication lag.
- You can choose shard keys for orders, chat messages, logs, and IoT events.
- You can design backups with RPO and RTO.

---

## Stage 6: GenAI, Search, and System Design

Goal: use MongoDB in modern architectures.

Read:

- `NOTES/09-change-streams-time-series-search-vector.md`
- `PROJECTS/13-change-stream-event-pipeline.md`
- `PROJECTS/14-time-series-metrics-store.md`
- `PROJECTS/15-rag-document-vector-metadata-store.md`
- `SYSTEM_DESIGN/PROMPTS.md`
- `SYSTEM_DESIGN/CASE_STUDIES.md`
- `SYSTEM_DESIGN/SHARDING_DESIGN.md`
- `SYSTEM_DESIGN/MAANG_MONGODB_ARCHITECTURE_PLAYBOOK.md`

Practice:

- Design a RAG chunk schema with metadata and ACL filters.
- Design a change-stream notification pipeline.
- Design a multi-tenant SaaS database.
- Design a globally scalable order system.

Success criteria:

- You can explain when MongoDB Vector Search is enough and when a specialized vector DB may be better.
- You can enforce tenant and ACL filters before retrieval reaches an LLM.
- You can explain schema, indexes, scaling, consistency, and failure modes for each design.

---

## Stage 7: Interview Readiness

Read:

- `INTERVIEW_PREP/QUESTIONS.md`
- `INTERVIEW_PREP/ANSWER_PATTERNS.md`
- `INTERVIEW_PREP/MOCK_INTERVIEWS.md`
- `INTERVIEW_PREP/MONGODB_100_INTERVIEW_DRILLS.md`
- `INTERVIEW_PREP/MAANG_DEEP_DIVE_QA.md`

Readiness gate:

- Explain MongoDB vs PostgreSQL clearly.
- Design an order schema and indexes.
- Optimize a slow query using `explain()`.
- Choose a shard key under traffic assumptions.
- Debug replication lag.
- Fix a schema with unbounded arrays.
- Design a RAG memory store with metadata filters.
- Answer tradeoffs, not just definitions.

MAANG deep-dive gate:

- Defend an embed vs reference decision under data growth.
- Choose and reject shard keys with skew and query-routing reasoning.
- Explain read concern, write concern, read preference, replication lag, and read-your-write behavior.
- Explain when transaction overhead is worth it and when schema redesign is better.
- Design multi-tenant SaaS, chat, audit logs, large-scale event ingestion, real-time dashboards, and RAG metadata stores.
- Compare MongoDB and PostgreSQL without turning it into a generic NoSQL vs SQL answer.
- Walk through a production p99 latency or stale-read incident from alert to postmortem.
