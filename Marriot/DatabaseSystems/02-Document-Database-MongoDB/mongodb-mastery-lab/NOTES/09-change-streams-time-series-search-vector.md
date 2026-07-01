# 09. Change Streams, Time-Series, Search, and Vector Search

## Change Streams

Change streams let applications subscribe to inserts, updates, deletes, and replacements. They are built on the oplog and are resumable with resume tokens.

Use cases:

- notifications
- cache invalidation
- audit pipelines
- search index sync
- event-driven processing
- materialized views

Important rules:

- Process events idempotently.
- Persist resume tokens after successful processing.
- Rebuild derived state if stream history expires.
- Use an outbox pattern when event contracts and external publishing guarantees matter.

## Time-Series Collections

Use time-series collections for telemetry, IoT, metrics, monitoring, and financial ticks.

```javascript
db.createCollection('deviceMetrics', {
  timeseries: {
    timeField: 'ts',
    metaField: 'metadata',
    granularity: 'seconds'
  },
  expireAfterSeconds: 60 * 60 * 24 * 30
})
```

Query by metadata plus time range.

## Text Search vs Atlas Search

MongoDB text index is basic keyword search.

Atlas Search is Lucene-backed and supports:

- analyzers
- autocomplete
- fuzzy search
- relevance scoring
- facets
- highlighting
- synonyms

Use Atlas Search for real product/content search experiences.

## Vector Search and RAG

Vector search finds semantically similar embeddings.

RAG flow:

```text
user query -> embedding -> vector search with metadata filters -> retrieved chunks -> LLM answer
```

RAG chunk schema:

```javascript
{
  tenantId: 't1',
  sourceDocumentId: 'doc-123',
  chunkId: 'doc-123:0007',
  text: '...',
  embedding: [0.012, -0.044],
  metadata: {
    title: 'MongoDB Guide',
    page: 12,
    tags: ['mongodb'],
    acl: ['team-db']
  },
  embeddingModel: 'text-embedding-model',
  createdAt: new Date()
}
```

Security rule: tenant and ACL filters must be enforced during retrieval, before text reaches the LLM.

## When MongoDB Vector Search Is Enough

- You already use Atlas.
- You need operational metadata with embeddings.
- Workload is app-scale.
- Tenant filters and document metadata matter.

## When Specialized Vector DB May Be Better

- very large vector-only workload
- very high vector QPS
- specialized ANN tuning requirements
- independent search/vector platform team
