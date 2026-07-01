# VectorDB Hands-On Exercises and Runnable Mini Labs

> Track File #27 of 30 - Group 06: Practice Upgrade
> For: hands-on practice | Level: beginner to pro | Mode: local Qdrant labs, Pinecone mental model

Use these with the `vector-db-mastery-lab` folder.

---

## Lab 1: First Collection And Upsert

Tasks:

- start Qdrant with Docker Compose
- create a collection with vector dimension 4
- upsert sample document chunks
- inspect payload/metadata

---

## Lab 2: Similarity Search

Tasks:

- run nearest-neighbor search
- change query vector
- observe score changes
- explain topK

---

## Lab 3: Metadata Filters

Tasks:

- filter by tenant
- filter by ACL group
- filter by document type
- explain why post-filtering can leak data

---

## Lab 4: RAG Retrieval

Tasks:

- search support/document chunks
- return source and citation metadata
- explain context packing
- identify stale or unauthorized result risks

---

## Lab 5: Hybrid Search Reasoning

Tasks:

- run [../vector-db-mastery-lab/SCRIPTS/08-hybrid-fusion-demo.sh](../vector-db-mastery-lab/SCRIPTS/08-hybrid-fusion-demo.sh)
- compare vector search with exact keyword needs
- explain where BM25/sparse retrieval would help
- design a dense+sparse fusion plan
- explain where reranking fits and why reranker depth needs a latency budget

---

## Lab 6: Evaluation

Tasks:

- define golden queries
- calculate recall@3 manually
- identify missing expected documents
- propose chunking/filter changes

---

## Lab 7: Incident Drill

Scenario:

```text
RAG answers became worse after an embedding-model upgrade.
```

Answer:

- check embedding version metadata
- compare old/new golden-set recall
- check dimension/metric compatibility
- check reindex completion
- canary or rollback

---

## Completion Gate

You finish these labs only when you can explain:

- why each metadata field exists
- what each query filters on
- what topK changes
- what could leak private data
- what metric proves retrieval quality