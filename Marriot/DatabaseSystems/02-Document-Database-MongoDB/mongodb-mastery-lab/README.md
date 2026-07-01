# MongoDB Mastery Lab

A complete beginner-to-pro MongoDB learning repository for backend engineers, production systems, GenAI/RAG apps, microservices, and MAANG-style database/system design interviews.

This repo is designed to be used, not only read. It includes a Docker MongoDB lab, seed data, mongosh scripts, Node.js examples, Python examples, Java Spring Boot examples, performance exercises, schema design case studies, interview prep, and mini projects.

---

## Repository Tree

```text
mongodb-mastery-lab/
  README.md
  LEARNING_PATH.md
  CONTRIBUTING.md
  GLOSSARY.md
  docker-compose.yml
  .gitignore
  CHEATSHEETS/
    CRUD.md
    QUERY_OPERATORS.md
    UPDATE_OPERATORS.md
    AGGREGATION.md
    INDEXING.md
    SCHEMA_DESIGN.md
    TRANSACTIONS_REPLICATION_SHARDING.md
    PERFORMANCE.md
    SECURITY.md
  NOTES/
    01-mongodb-fundamentals.md
    02-document-model-bson-objectid.md
    03-crud-query-language.md
    04-schema-design-patterns.md
    05-indexing-explain-query-planner.md
    06-aggregation-framework.md
    07-transactions-consistency.md
    08-replication-sharding.md
    09-change-streams-time-series-search-vector.md
    10-production-operations.md
  EXAMPLES/
    mongosh/
      01-crud.js
      02-query-operators.js
      03-indexing-explain.js
      04-aggregation.js
      05-transactions.js
    nodejs/
      package.json
      .env.example
      src/db.js
      src/crud.js
      src/aggregation.js
      src/change-stream.js
    python/
      requirements.txt
      crud.py
      aggregation.py
      change_stream.py
    java-spring-boot/
      pom.xml
      src/main/java/com/example/mongodbmastery/MongoMasteryApplication.java
      src/main/java/com/example/mongodbmastery/order/OrderDocument.java
      src/main/java/com/example/mongodbmastery/order/OrderRepository.java
      src/main/java/com/example/mongodbmastery/order/OrderController.java
      src/main/resources/application.yml
  PROJECTS/
    01-user-profile-service.md
    02-blog-platform.md
    03-ecommerce-catalog.md
    04-shopping-cart.md
    05-order-system.md
    06-chat-app.md
    07-notification-engine.md
    08-audit-log-platform.md
    09-iot-telemetry.md
    10-analytics-dashboard.md
    11-multi-tenant-saas.md
    12-document-management-system.md
    13-change-stream-event-pipeline.md
    14-time-series-metrics-store.md
    15-rag-document-vector-metadata-store.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
    MOCK_INTERVIEWS.md
    MONGODB_100_INTERVIEW_DRILLS.md
    MAANG_DEEP_DIVE_QA.md
  SYSTEM_DESIGN/
    PROMPTS.md
    CASE_STUDIES.md
    SCHEMA_DESIGN_CASE_STUDIES.md
    SHARDING_DESIGN.md
    MAANG_MONGODB_ARCHITECTURE_PLAYBOOK.md
  PERFORMANCE/
    INDEXING_EXERCISES.md
    AGGREGATION_EXERCISES.md
    PERFORMANCE_TUNING_EXERCISES.md
    EXPLAIN_PLAYBOOK.md
    MAANG_PRODUCTION_INCIDENT_PLAYBOOK.md
  SECURITY/
    SECURITY_CHECKLIST.md
    BACKUP_RESTORE_DR.md
  SCRIPTS/
    00-init-user.js
    01-seed-data.js
    create-indexes.js
    aggregation-labs.js
    performance-labs.js
    run-mongosh.sh
    reset-lab.sh
```

---

## Quick Start

Prerequisites:

- Docker Desktop
- `mongosh` for local shell access
- Node.js 20+ for Node examples
- Python 3.11+ for Python examples
- Java 17+ and Maven for Spring Boot examples

Start MongoDB:

```bash
docker compose up -d
```

Connect as the app user:

```bash
mongosh "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true"
```

Connect as root:

```bash
mongosh "mongodb://root:rootpass@localhost:27017/admin?replicaSet=rs0&directConnection=true"
```

Run a lab script:

```bash
mongosh "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true" SCRIPTS/create-indexes.js
mongosh "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true" SCRIPTS/aggregation-labs.js
mongosh "mongodb://app:app_password@localhost:27017/mongodb_mastery?authSource=mongodb_mastery&replicaSet=rs0&directConnection=true" SCRIPTS/performance-labs.js
```

The Docker setup initializes a single-node replica set named `rs0`, so transaction and change-stream examples are runnable locally.

Reset the lab from scratch:

```bash
bash SCRIPTS/reset-lab.sh
```

---

## What You Will Build

You will learn MongoDB through four loops:

1. Read the notes and cheatsheets.
2. Run the scripts against seeded data.
3. Complete indexing, aggregation, performance, schema, and security exercises.
4. Build mini projects and answer interview/system design prompts.

Core datasets included:

- users and profiles
- products, variants, and reviews
- orders and order items
- audit logs
- IoT metrics
- RAG document chunks
- notifications

---

## Hands-On Project Track

The `PROJECTS/` folder now contains 15 beginner-to-pro MongoDB projects. Every project includes goal, schema design, sample data, CRUD operations, indexes, aggregation queries, performance considerations, scaling considerations, security considerations, optional API layer, and interview discussion points.

Project sequence:

1. User profile service
2. Blog platform
3. E-commerce catalog
4. Shopping cart
5. Order system
6. Chat app
7. Notification engine
8. Audit log platform
9. IoT telemetry
10. Analytics dashboard
11. Multi-tenant SaaS
12. Document management system
13. Change stream event pipeline
14. Time-series metrics store
15. RAG document/vector metadata store

---

## Learning Outcomes

By the end, you should be able to:

- Model MongoDB documents around access patterns.
- Choose embedding, referencing, and hybrid patterns.
- Write CRUD and query operators fluently.
- Build aggregation pipelines for real reports.
- Design indexes with ESR and validate using `explain()`.
- Explain transactions, read concern, write concern, and retryable writes.
- Explain replica sets, elections, oplog, lag, and failover.
- Choose shard keys for multi-tenant, chat, IoT, and order systems.
- Secure MongoDB with auth, roles, TLS concepts, backups, and secret hygiene.
- Use MongoDB for GenAI/RAG metadata and vector-search-friendly designs.
- Defend MongoDB choices in MAANG-style system design interviews.
- Explain MAANG-level tradeoffs for shard keys, hot partitions, consistency, lag, transaction overhead, multi-tenant SaaS, chat, audit logging, event ingestion, real-time dashboards, RAG metadata stores, MongoDB vs PostgreSQL, and production incidents.

---

## MAANG Interview Deep Dive

For senior backend and system design interview preparation, use these files after the core notes:

- `INTERVIEW_PREP/MONGODB_100_INTERVIEW_DRILLS.md`: 100 categorized drills with concise answers, examples, traps, follow-ups, system design prompts, debugging scenarios, performance tuning, sharding, transactions, and schema design questions.
- `INTERVIEW_PREP/MAANG_DEEP_DIVE_QA.md`: 20 deep-dive interview cards with expected answers, diagrams, and tradeoff tables.
- `SYSTEM_DESIGN/MAANG_MONGODB_ARCHITECTURE_PLAYBOOK.md`: design-board playbook for multi-tenant SaaS, chat, audit logs, event ingestion, dashboards, RAG metadata stores, shard keys, indexes, and MongoDB vs PostgreSQL tradeoffs.
- `PERFORMANCE/MAANG_PRODUCTION_INCIDENT_PLAYBOOK.md`: production debugging playbook for slow queries, index regressions, replication lag, write concern, transaction conflicts, hot shards, aggregation spills, connection pools, dashboard drift, and RAG retrieval incidents.

---

## Suggested First Session

1. Start Docker with `docker compose up -d`.
2. Read `LEARNING_PATH.md`.
3. Run `EXAMPLES/mongosh/01-crud.js`.
4. Run `SCRIPTS/create-indexes.js`.
5. Run one aggregation from `EXAMPLES/mongosh/04-aggregation.js`.
6. Open `PERFORMANCE/EXPLAIN_PLAYBOOK.md` and compare explain plans.

---

## Safety Notes

This repo uses local demo credentials only:

```text
root / rootpass
app / app_password
```

Do not reuse these credentials in real environments. Production MongoDB must use least-privilege users, TLS, network restrictions, secret management, backups, monitoring, and tested disaster recovery.
