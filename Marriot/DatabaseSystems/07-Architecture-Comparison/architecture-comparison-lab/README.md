# Architecture Comparison Lab

A practical beginner-to-pro lab for choosing datastores in system design interviews and production architecture reviews.

This lab is script-and-drill based. It does not need Docker because the goal is to practice decision-making, tradeoff scoring, source-of-truth boundaries, and production failure analysis across SQL, MongoDB, Cassandra, Elasticsearch, VectorDB, Neo4j, Redis, object storage, time-series stores, and warehouses.

---

## Quick Start

Run the ecommerce decision drill:

```bash
bash SCRIPTS/01-score-ecommerce-choice.sh
```

Run the RAG decision drill:

```bash
bash SCRIPTS/02-score-rag-choice.sh
```

Build a generic interview answer:

```bash
bash SCRIPTS/04-interview-answer-builder.sh
```

---

## Repository-Style Learning Areas

```text
architecture-comparison-lab/
  README.md
  LEARNING_PATH.md
  SCRIPTS/
    01-score-ecommerce-choice.sh
    02-score-rag-choice.sh
    03-source-derived-map.sh
    04-interview-answer-builder.sh
  LABS/
    01-source-of-truth-vs-derived.md
    02-ecommerce-decision-matrix.md
    03-rag-search-vector-graph.md
    04-payments-ledger-consistency.md
    05-observability-storage.md
    06-polyglot-sync-failure.md
  PROJECTS/
    01-marketplace-data-architecture.md
    02-enterprise-rag-data-architecture.md
    03-chat-feed-storage-architecture.md
    04-fraud-risk-data-platform.md
    05-observability-data-platform.md
  CHEATSHEETS/
    DECISION_MATRIX.md
    PROS_CONS.md
    INTERVIEW_PATTERNS.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    STALE_DERIVED_STORE.md
    BAD_DATABASE_CHOICE.md
    DUAL_WRITE_INCONSISTENCY.md
    HOT_PARTITION_OR_SHARD.md
    CACHE_STALENESS.md
```

---

## Practice Loop

```text
scenario -> access pattern -> source of truth -> derived stores -> sync path -> failure mode -> final interview answer
```

Architecture comparison becomes sharp when every database choice has a reason and every reason has a tradeoff.