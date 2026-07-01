# Elasticsearch Mastery Lab

A practical beginner-to-pro Elasticsearch lab for backend engineers, search engineers, GenAI/RAG builders, production debugging, and MAANG-style system design rounds.

This lab is designed to be used alongside the modular Elasticsearch track. It includes Docker Compose, curl helper scripts, mappings, sample data, guided labs, projects, cheatsheets, interview prep, and production runbooks.

---

## Suggested Local Setup

Prerequisites:

- Docker Desktop
- curl
- optional jq for prettier JSON inspection

Start Elasticsearch and seed the lab:

```bash
docker compose up -d
bash SCRIPTS/wait-for-elasticsearch.sh
bash SCRIPTS/run-request.sh SCRIPTS/01-create-indices.sh
bash SCRIPTS/run-request.sh SCRIPTS/02-seed-data.sh
bash SCRIPTS/run-request.sh SCRIPTS/03-search-queries.sh
```

Reset everything from scratch:

```bash
bash SCRIPTS/reset-lab.sh
```

Open Kibana at:

```text
http://localhost:5601
```

Elasticsearch is exposed at:

```text
http://localhost:9200
```

Security is disabled only for local learning. Production designs must include auth, TLS, roles, API keys, network controls, and audit posture.

---

## Repository-Style Learning Areas

```text
elasticsearch-mastery-lab/
  README.md
  LEARNING_PATH.md
  docker-compose.yml
  SCRIPTS/
    00-cluster-health.sh
    01-create-indices.sh
    02-seed-data.sh
    03-search-queries.sh
    04-aggregations.sh
    05-analyze.sh
    06-operations-inspection.sh
    07-alias-reindex-migration.sh
    08-rag-acl-search.sh
    09-geo-place-search.sh
    run-request.sh
    reset-lab.sh
    wait-for-elasticsearch.sh
  LABS/
    01-index-search-basics.md
    02-mappings-analyzers.md
    03-facets-aggregations.md
    04-relevance-debugging.md
    05-operations-incident-drills.md
    06-zero-downtime-reindex-aliases.md
    07-authorized-rag-retrieval.md
    08-autocomplete-geospatial-search.md
  PROJECTS/
    01-product-search-engine.md
    02-log-analytics-platform.md
    03-rag-document-retrieval.md
  CHEATSHEETS/
    REST.md
    QUERY_DSL.md
    OPERATIONS.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    SLOW_SEARCH.md
    MAPPING_EXPLOSION.md
    HOT_SHARD.md
    STALE_RESULTS.md
```

---

## First Session

1. Run `bash SCRIPTS/reset-lab.sh`.
2. Open [LABS/01-index-search-basics.md](LABS/01-index-search-basics.md).
3. Run [SCRIPTS/03-search-queries.sh](SCRIPTS/03-search-queries.sh).
4. Run [SCRIPTS/04-aggregations.sh](SCRIPTS/04-aggregations.sh).
5. Run [SCRIPTS/05-analyze.sh](SCRIPTS/05-analyze.sh).
6. Explain why `title` has both `text` and `keyword`/autocomplete behavior.
7. Open [RUNBOOKS/SLOW_SEARCH.md](RUNBOOKS/SLOW_SEARCH.md) and rehearse the incident response.

Gap-fill sessions:

1. Run [SCRIPTS/07-alias-reindex-migration.sh](SCRIPTS/07-alias-reindex-migration.sh), then read [LABS/06-zero-downtime-reindex-aliases.md](LABS/06-zero-downtime-reindex-aliases.md).
2. Run [SCRIPTS/08-rag-acl-search.sh](SCRIPTS/08-rag-acl-search.sh), then read [LABS/07-authorized-rag-retrieval.md](LABS/07-authorized-rag-retrieval.md).
3. Run [SCRIPTS/09-geo-place-search.sh](SCRIPTS/09-geo-place-search.sh), then read [LABS/08-autocomplete-geospatial-search.md](LABS/08-autocomplete-geospatial-search.md).
4. Open [RUNBOOKS/HOT_SHARD.md](RUNBOOKS/HOT_SHARD.md) and rehearse skew detection.

---

## Suggested Practice Loop

```text
run script -> inspect result -> explain mapping/analyzer -> name relevance tradeoff -> name failure mode -> answer interview prompt
```

Elasticsearch sticks when every query is tied to user intent, every field has a mapping reason, and every search path has a relevance and operations story.