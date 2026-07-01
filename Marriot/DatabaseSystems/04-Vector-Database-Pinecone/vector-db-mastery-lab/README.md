# VectorDB Mastery Lab

A practical beginner-to-pro vector database lab for backend engineers, GenAI/RAG builders, search engineers, production owners, and MAANG-style system design rounds.

This lab uses Qdrant locally because it is open-source and Docker-friendly. The track still teaches Pinecone as the managed-production reference, and every lab maps cleanly to Pinecone concepts such as index, namespace, metadata, upsert, query, filter, and delete.

---

## Suggested Local Setup

Prerequisites:

- Docker Desktop
- bash
- curl

Start Qdrant and seed the lab:

```bash
docker compose up -d
bash SCRIPTS/wait-for-qdrant.sh
bash SCRIPTS/reset-lab.sh
```

Run a similarity search:

```bash
bash SCRIPTS/03-vector-search.sh
```

Run an authorized RAG retrieval:

```bash
bash SCRIPTS/05-rag-retrieval.sh
```

Open Qdrant dashboard/API at:

```text
http://localhost:6333/dashboard
```

---

## Repository-Style Learning Areas

```text
vector-db-mastery-lab/
  README.md
  LEARNING_PATH.md
  docker-compose.yml
  SCRIPTS/
    01-create-collection.sh
    02-upsert-documents.sh
    03-vector-search.sh
    04-metadata-filter-search.sh
    05-rag-retrieval.sh
    06-evaluation.sh
    07-incident-debug.sh
    08-hybrid-fusion-demo.sh
    reset-lab.sh
    wait-for-qdrant.sh
  LABS/
    01-first-collection-upsert.md
    02-similarity-search.md
    03-metadata-filtering-acl.md
    04-rag-retrieval.md
    05-evaluation.md
    06-incident-debugging.md
    07-hybrid-fusion-rerank.md
  PROJECTS/
    01-enterprise-rag.md
    02-semantic-product-search.md
    03-support-chatbot.md
    04-multimodal-search.md
  CHEATSHEETS/
    API.md
    MODELING.md
    OPERATIONS.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    LOW_RECALL.md
    HIGH_P99_LATENCY.md
    STALE_VECTORS.md
    METADATA_ACL_LEAK.md
    EMBEDDING_DIMENSION_MISMATCH.md
```

---

## First Session

1. Run `bash SCRIPTS/reset-lab.sh`.
2. Open [LABS/01-first-collection-upsert.md](LABS/01-first-collection-upsert.md).
3. Run [SCRIPTS/03-vector-search.sh](SCRIPTS/03-vector-search.sh).
4. Run [SCRIPTS/04-metadata-filter-search.sh](SCRIPTS/04-metadata-filter-search.sh).
5. Run [SCRIPTS/05-rag-retrieval.sh](SCRIPTS/05-rag-retrieval.sh).
6. Open [RUNBOOKS/LOW_RECALL.md](RUNBOOKS/LOW_RECALL.md) and rehearse the debug flow.

---

## Suggested Practice Loop

```text
run query -> inspect payload -> explain metadata filter -> name topK tradeoff -> name evaluation metric -> answer interview prompt
```

Vector DBs become clear when every vector has a source, every query has filters, and every quality claim has an evaluation check.