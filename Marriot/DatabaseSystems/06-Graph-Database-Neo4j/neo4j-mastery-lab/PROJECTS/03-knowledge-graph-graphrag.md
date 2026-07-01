# Project 03: Knowledge Graph And GraphRAG

Goal: build a graph-backed retrieval system for GenAI answers.

---

## Requirements

- connect documents, chunks, entities, facts, and sources
- retrieve chunks by text/vector candidate search
- expand entity neighborhood
- return citations and provenance
- enforce tenant/ACL filters

---

## Graph Model

```text
(:Document)-[:HAS_CHUNK]->(:Chunk)
(:Chunk)-[:MENTIONS]->(:Entity)
(:Fact)-[:SUPPORTED_BY]->(:Source)
(:Entity)-[:RELATED_TO]->(:Entity)
```

---

## Interview Talking Points

- entity resolution quality
- provenance and citations
- vector + graph hybrid retrieval
- permission filters before expansion