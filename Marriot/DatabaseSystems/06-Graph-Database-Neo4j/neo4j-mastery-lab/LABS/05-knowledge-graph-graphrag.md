# Lab 05: Knowledge Graph And GraphRAG

Goal: retrieve chunks, entities, and provenance paths.

---

## Run

```bash
bash SCRIPTS/run-cypher.sh SCRIPTS/06-knowledge-graph-graphrag.cypher
```

---

## What To Observe

- full-text search finds candidate chunks
- chunks mention entities
- entities connect to related entities
- documents provide source context

---

## Explain Out Loud

```text
How does a graph improve RAG beyond vector similarity?
```

Strong answer:

```text
The graph adds explicit entities, relationships, provenance, and paths. Vector search finds semantically similar text, while graph expansion explains how entities and facts connect.
```

---

## Completion Gate

- You can explain chunk-to-entity modeling.
- You can explain provenance.
- You can name GraphRAG leak and stale-entity tests.