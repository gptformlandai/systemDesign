# Elasticsearch Mastery Lab Learning Path

This path turns Elasticsearch from a tool you can query into a search system you can design, operate, debug, and defend in interviews.

---

## Stage 1: Starter Foundations

Read:

- `../01-Starter-Path/Elasticsearch-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md`
- `../01-Starter-Path/Elasticsearch-Core-Architecture-Index-Document-Shard-Gold-Sheet.md`
- `../01-Starter-Path/Elasticsearch-Installation-Tools-REST-Kibana-Gold-Sheet.md`
- `../01-Starter-Path/Elasticsearch-Indexing-CRUD-Search-Basics-Gold-Sheet.md`

Run:

```bash
docker compose up -d
bash SCRIPTS/wait-for-elasticsearch.sh
bash SCRIPTS/run-request.sh SCRIPTS/01-create-indices.sh
bash SCRIPTS/run-request.sh SCRIPTS/02-seed-data.sh
```

Lab:

- [LABS/01-index-search-basics.md](LABS/01-index-search-basics.md)
- [CHEATSHEETS/REST.md](CHEATSHEETS/REST.md)

---

## Stage 2: Mappings, Query DSL, And Facets

Read:

- `../02-Intermediate-Backend/Elasticsearch-Mappings-Analyzers-Text-Keyword-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/Elasticsearch-Query-DSL-Filters-Bool-Pagination-Gold-Sheet.md`
- `../02-Intermediate-Backend/Elasticsearch-Aggregations-Facets-Analytics-Gold-Sheet.md`
- `../02-Intermediate-Backend/Elasticsearch-Advanced-Query-DSL-Nested-Geo-Collapse-Runtime-Gap-Fill-Gold-Sheet.md`

Lab:

- [LABS/02-mappings-analyzers.md](LABS/02-mappings-analyzers.md)
- [LABS/03-facets-aggregations.md](LABS/03-facets-aggregations.md)
- [CHEATSHEETS/QUERY_DSL.md](CHEATSHEETS/QUERY_DSL.md)

---

## Stage 3: Relevance, Sync, And App Integration

Read:

- `../02-Intermediate-Backend/Elasticsearch-Ingest-Pipelines-Sync-CDC-Gold-Sheet.md`
- `../02-Intermediate-Backend/Elasticsearch-Relevance-Scoring-Synonyms-Highlighting-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/Elasticsearch-Application-Development-Java-Python-Node-Gold-Sheet.md`

Lab:

- [LABS/04-relevance-debugging.md](LABS/04-relevance-debugging.md)
- [LABS/06-zero-downtime-reindex-aliases.md](LABS/06-zero-downtime-reindex-aliases.md)
- [INTERVIEW_PREP/ANSWER_PATTERNS.md](INTERVIEW_PREP/ANSWER_PATTERNS.md)

---

## Stage 4: Senior Production Elasticsearch

Read:

- `../03-Senior-MAANG/Elasticsearch-Shards-Replicas-Cluster-Scaling-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Elasticsearch-ILM-Data-Streams-Rollover-Lifecycle-Gold-Sheet.md`
- `../03-Senior-MAANG/Elasticsearch-Performance-Tuning-Observability-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Elasticsearch-Security-Backup-Disaster-Recovery-Gold-Sheet.md`

Lab:

- [LABS/05-operations-incident-drills.md](LABS/05-operations-incident-drills.md)
- [RUNBOOKS/SLOW_SEARCH.md](RUNBOOKS/SLOW_SEARCH.md)
- [RUNBOOKS/MAPPING_EXPLOSION.md](RUNBOOKS/MAPPING_EXPLOSION.md)
- [RUNBOOKS/HOT_SHARD.md](RUNBOOKS/HOT_SHARD.md)
- [RUNBOOKS/STALE_RESULTS.md](RUNBOOKS/STALE_RESULTS.md)
- [CHEATSHEETS/OPERATIONS.md](CHEATSHEETS/OPERATIONS.md)

---

## Stage 5: Advanced And Scenario Design

Read:

- `../03-Senior-MAANG/Elasticsearch-Vector-Hybrid-Search-GenAI-RAG-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Elasticsearch-Cloud-Kubernetes-Testing-Operations-Gold-Sheet.md`
- `../04-Scenario-Practice/Elasticsearch-Ecommerce-Product-Search-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/Elasticsearch-Logs-Observability-Security-Analytics-Gold-Sheet.md`
- `../04-Scenario-Practice/Elasticsearch-System-Design-Case-Studies-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/Elasticsearch-Autocomplete-Geospatial-Search-Gap-Fill-MAANG-Sheet.md`
- `../04-Scenario-Practice/Elasticsearch-Multi-Tenant-ACL-RAG-Gap-Fill-MAANG-Sheet.md`

Lab:

- [LABS/07-authorized-rag-retrieval.md](LABS/07-authorized-rag-retrieval.md)
- [LABS/08-autocomplete-geospatial-search.md](LABS/08-autocomplete-geospatial-search.md)

Projects:

- [PROJECTS/01-product-search-engine.md](PROJECTS/01-product-search-engine.md)
- [PROJECTS/02-log-analytics-platform.md](PROJECTS/02-log-analytics-platform.md)
- [PROJECTS/03-rag-document-retrieval.md](PROJECTS/03-rag-document-retrieval.md)

---

## Stage 6: Interview Readiness

Read:

- `../05-Special-Interview-Rounds/Elasticsearch-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md`
- `../05-Special-Interview-Rounds/Elasticsearch-Interview-Prep-QA-MAANG-Sheet.md`
- `../06-Practice-Upgrade/Elasticsearch-Active-Recall-Question-Bank.md`
- `../06-Practice-Upgrade/Elasticsearch-Hands-On-Exercises-And-Runnable-Mini-Labs.md`
- `../06-Practice-Upgrade/Elasticsearch-Mini-Projects-Portfolio.md`
- `../06-Practice-Upgrade/Elasticsearch-Cheat-Sheets-Roadmap-Golden-Rules.md`
- `../06-Practice-Upgrade/Elasticsearch-Pro-Gap-Fill-Capacity-Relevance-SLO-Design-Review.md`

MAANG deep-dive gate:

- Defend mappings, analyzers, and query DSL choices.
- Explain relevance, sync freshness, shard sizing, ILM, and SLOs.
- Debug slow search, mapping explosion, high heap, hot shards, stale results, and disk watermarks.
- Compare Elasticsearch with SQL, MongoDB, Cassandra, OpenSearch, Solr, Kafka, and vector databases.
- Design at least 3 portfolio projects end to end.