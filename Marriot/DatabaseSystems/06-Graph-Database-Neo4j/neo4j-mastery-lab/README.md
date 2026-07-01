# Neo4j Mastery Lab

A practical beginner-to-pro Neo4j lab for backend engineers, graph database learners, fraud/risk engineers, knowledge graph builders, GenAI/GraphRAG builders, production debugging, and MAANG-style system design rounds.

This lab is designed to be used alongside the modular Neo4j track. It includes Docker Compose, cypher-shell helper scripts, constraints, sample graph data, guided labs, projects, cheatsheets, interview prep, and production runbooks.

---

## Suggested Local Setup

Prerequisites:

- Docker Desktop
- bash

Start Neo4j and seed the lab:

```bash
docker compose up -d
bash SCRIPTS/wait-for-neo4j.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/01-schema.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/02-seed-data.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/03-basic-traversals.cypher
```

Reset everything from scratch:

```bash
bash SCRIPTS/reset-lab.sh
```

Open Neo4j Browser at:

```text
http://localhost:7474
```

Bolt is exposed at:

```text
bolt://localhost:7687
```

Authentication is disabled only for local learning. Production designs must include auth, roles, TLS, private network access, secret management, and audit posture.

---

## Repository-Style Learning Areas

```text
neo4j-mastery-lab/
  README.md
  LEARNING_PATH.md
  docker-compose.yml
  SCRIPTS/
    00-health.cypher
    01-schema.cypher
    02-seed-data.cypher
    03-basic-traversals.cypher
    04-recommendations.cypher
    05-fraud-identity.cypher
    06-knowledge-graph-graphrag.cypher
    07-query-plan-debugging.cypher
    08-permission-graph.cypher
    09-dependency-lineage.cypher
    run-cypher.sh
    reset-lab.sh
    wait-for-neo4j.sh
  LABS/
    01-graph-basics.md
    02-modeling-constraints.md
    03-traversals-recommendations.md
    04-fraud-identity.md
    05-knowledge-graph-graphrag.md
    06-query-plan-debugging.md
    07-operations-incident-drills.md
    08-permission-graph-access-control.md
    09-dependency-lineage-blast-radius.md
  PROJECTS/
    01-social-recommendation-engine.md
    02-fraud-ring-detection.md
    03-knowledge-graph-graphrag.md
    04-service-dependency-graph.md
  CHEATSHEETS/
    CYPHER.md
    MODELING.md
    OPERATIONS.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    SLOW_TRAVERSAL.md
    CARTESIAN_PRODUCT.md
    HOT_NODE.md
    LOCK_CONTENTION.md
    STALE_GRAPH_PROJECTION.md
```

---

## First Session

1. Run `bash SCRIPTS/reset-lab.sh`.
2. Open [LABS/01-graph-basics.md](LABS/01-graph-basics.md).
3. Run [SCRIPTS/03-basic-traversals.cypher](SCRIPTS/03-basic-traversals.cypher) through `bash SCRIPTS/run-cypher.sh`.
4. Run [SCRIPTS/04-recommendations.cypher](SCRIPTS/04-recommendations.cypher).
5. Run [SCRIPTS/05-fraud-identity.cypher](SCRIPTS/05-fraud-identity.cypher).
6. Explain why constraints protect graph identity.
7. Open [RUNBOOKS/SLOW_TRAVERSAL.md](RUNBOOKS/SLOW_TRAVERSAL.md) and rehearse the incident response.

---

## Suggested Practice Loop

```text
run script -> inspect graph/path -> explain model choice -> name traversal budget -> name failure mode -> answer interview prompt
```

Neo4j sticks when every relationship has meaning, every traversal starts from an anchor, and every path has a boundary.