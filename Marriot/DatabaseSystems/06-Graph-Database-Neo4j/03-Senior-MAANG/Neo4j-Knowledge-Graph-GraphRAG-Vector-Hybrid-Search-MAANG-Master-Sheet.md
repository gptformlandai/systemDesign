# Neo4j Knowledge Graph, GraphRAG, Vector, and Hybrid Search - MAANG Master Sheet

> Track File #15 of 30 - Group 03: Senior / MAANG
> For: backend/data/GenAI interviews | Level: senior / MAANG | Mode: knowledge graphs, retrieval, entity grounding

This sheet builds:
- Knowledge graph mental model
- GraphRAG retrieval design
- Vector + graph hybrid reasoning

---

## 1. Knowledge Graph Mental Model

A knowledge graph connects entities, concepts, documents, facts, and provenance.

```text
(:Document)-[:HAS_CHUNK]->(:Chunk)-[:MENTIONS]->(:Entity)
(:Entity)-[:RELATED_TO]->(:Entity)
(:Fact)-[:SUPPORTED_BY]->(:Source)
```

The graph answers:

- what entities are connected?
- why are they connected?
- which source supports the connection?
- what path explains the answer?

---

## 2. GraphRAG Flow

```text
user question -> entity/vector/text retrieval -> graph expansion -> permission/provenance filtering -> ranked context -> LLM answer with citations
```

GraphRAG improves RAG when relationships and provenance matter.

---

## 3. Hybrid Retrieval

Use both:

- vector search for semantic similarity
- graph traversal for explicit relationships and lineage
- full-text search for exact terms and names
- metadata filters for tenant/security

Do not pass unauthorized graph neighborhoods into the LLM.

---

## 4. Entity Resolution

Knowledge graphs live or die by identity quality.

Risks:

- duplicate entities
- wrong merges
- stale aliases
- missing provenance
- conflicting facts

Controls:

- uniqueness constraints
- source provenance
- confidence scores
- human review for high-impact merges
- rollback/versioning strategy

---

## 5. Strong Answer

```text
For GraphRAG, I would model documents, chunks, entities, facts, sources, and relationships with provenance. Retrieval can combine vector search, full-text search, and graph expansion from matched entities. I would apply tenant and ACL filters before expansion, rank context by relevance and source quality, and evaluate recall, groundedness, citation correctness, stale entities, and permission leaks.
```

---

## 6. Revision Notes

- One-line summary: GraphRAG adds entity relationships and provenance to retrieval.
- Three keywords: entity, provenance, groundedness.
- One interview trap: building GraphRAG without entity resolution quality checks.
- Memory trick: vector finds similar text; graph explains connected facts.