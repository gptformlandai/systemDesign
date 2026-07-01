# Neo4j Knowledge Graph and Entity Resolution - MAANG Master Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: backend/data/GenAI interviews | Level: senior / MAANG | Mode: knowledge graphs, identity, provenance

This sheet builds:
- Entity-resolution design
- Knowledge graph quality controls
- GraphRAG-ready modeling

---

## 1. Requirements

- represent entities, aliases, facts, documents, and sources
- merge duplicate entities safely
- keep provenance and confidence
- support graph traversal and retrieval
- prevent bad merges from corrupting answers

---

## 2. Model

```text
(:Document)-[:HAS_CHUNK]->(:Chunk)
(:Chunk)-[:MENTIONS {confidence: 0.91}]->(:Entity)
(:Entity)-[:ALIAS_OF]->(:Entity)
(:Fact)-[:SUBJECT]->(:Entity)
(:Fact)-[:OBJECT]->(:Entity)
(:Fact)-[:SUPPORTED_BY]->(:Source)
```

---

## 3. Entity Resolution Pipeline

```text
extract entities -> normalize candidates -> match by rules/embedding -> score confidence -> merge or link -> retain provenance -> review high-risk cases
```

---

## 4. Quality Metrics

- duplicate rate
- false merge rate
- unresolved alias rate
- provenance completeness
- stale fact rate
- citation correctness
- retrieval recall

---

## 5. Strong Answer

```text
I would model a knowledge graph around entities, facts, documents, chunks, sources, aliases, and provenance. Entity resolution should not blindly merge; it needs confidence scores, source evidence, review thresholds, and rollback paths. For GraphRAG, retrieval should combine text/vector search with graph expansion and return authorized, cited context paths.
```

---

## 6. Revision Notes

- One-line summary: Knowledge graph quality depends on identity, provenance, and rollback discipline.
- Three keywords: entity, alias, provenance.
- One interview trap: merging entities without confidence and source evidence.
- Memory trick: a bad merge poisons every path after it.