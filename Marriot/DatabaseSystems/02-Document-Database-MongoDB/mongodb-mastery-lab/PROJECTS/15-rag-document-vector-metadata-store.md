# Project 15: RAG Document and Vector Metadata Store

Difficulty: Pro

Build a RAG metadata store with source documents, chunks, embeddings, ACL filters, ingestion jobs, citations, re-embedding, and deletion handling.

---

## Goal

Practice GenAI-oriented schema design, vector-search metadata, tenant and ACL filtering, chunk lineage, ingestion workflows, and MongoDB vs specialized vector database tradeoffs.

---

## Schema Design

Store source document metadata separately from chunks. Each chunk includes tenant, ACL, source lineage, metadata filters, embedding model, and content hash.

```javascript
// sourceDocuments
{
  _id: 'src_1001',
  tenantId: 'tenant_ai',
  title: 'Refund Policy',
  sourceType: 'pdf',
  uri: 's3://tenant_ai/policies/refund.pdf',
  acl: { users: ['usr_1001'], groups: ['support'] },
  status: 'INDEXED',
  version: 4,
  contentHash: 'sha256:sourcehash',
  createdAt: ISODate('2026-07-01T09:00:00Z'),
  updatedAt: ISODate('2026-07-01T10:00:00Z')
}

// ragChunks
{
  _id: 'src_1001:chunk_0001',
  tenantId: 'tenant_ai',
  sourceDocumentId: 'src_1001',
  chunkId: 'chunk_0001',
  text: 'Refunds are processed within five business days...',
  embedding: [0.012, -0.08, 0.44],
  embeddingModel: 'text-embedding-3-large',
  acl: { users: ['usr_1001'], groups: ['support'] },
  metadata: { product: 'payments', tags: ['refunds'], page: 12 },
  contentHash: 'sha256:chunkhash',
  createdAt: ISODate('2026-07-01T10:00:00Z')
}
```

---

## Sample Data

```javascript
db.sourceDocuments.insertOne({ _id: 'src_1001', tenantId: 'tenant_ai', title: 'Refund Policy', sourceType: 'pdf', uri: 's3://tenant_ai/policies/refund.pdf', acl: { users: ['usr_1001'], groups: ['support'] }, status: 'INDEXED', version: 4, contentHash: 'sha256:sourcehash', createdAt: new Date(), updatedAt: new Date() })

db.ragChunks.insertMany([
  { _id: 'src_1001:chunk_0001', tenantId: 'tenant_ai', sourceDocumentId: 'src_1001', chunkId: 'chunk_0001', text: 'Refunds are processed within five business days.', embedding: [0.012, -0.08, 0.44], embeddingModel: 'text-embedding-3-large', acl: { users: ['usr_1001'], groups: ['support'] }, metadata: { product: 'payments', tags: ['refunds'], page: 12 }, contentHash: 'sha256:chunk1', createdAt: new Date() },
  { _id: 'src_1001:chunk_0002', tenantId: 'tenant_ai', sourceDocumentId: 'src_1001', chunkId: 'chunk_0002', text: 'Escalations require manager approval.', embedding: [0.022, -0.01, 0.39], embeddingModel: 'text-embedding-3-large', acl: { users: ['usr_1001'], groups: ['support'] }, metadata: { product: 'payments', tags: ['escalation'], page: 13 }, contentHash: 'sha256:chunk2', createdAt: new Date() }
])
```

---

## CRUD Operations

Create ingestion job:

```javascript
db.ingestionJobs.insertOne({ _id: 'job_1001', tenantId: 'tenant_ai', sourceDocumentId: 'src_1001', status: 'PENDING', embeddingModel: 'text-embedding-3-large', createdAt: new Date(), updatedAt: new Date() })
```

Insert chunk:

```javascript
db.ragChunks.insertOne({ _id: 'src_1001:chunk_0003', tenantId: 'tenant_ai', sourceDocumentId: 'src_1001', chunkId: 'chunk_0003', text: 'Refund requests must include the original order number.', embedding: [0.01, -0.02, 0.31], embeddingModel: 'text-embedding-3-large', acl: { users: ['usr_1001'], groups: ['support'] }, metadata: { product: 'payments', tags: ['refunds'], page: 14 }, contentHash: 'sha256:chunk3', createdAt: new Date() })
```

Metadata-filtered retrieval candidate query:

```javascript
db.ragChunks.find({ tenantId: 'tenant_ai', 'acl.groups': 'support', 'metadata.tags': 'refunds', embeddingModel: 'text-embedding-3-large' }).limit(20)
```

Delete source and chunks:

```javascript
db.sourceDocuments.updateOne({ tenantId: 'tenant_ai', _id: 'src_1001' }, { $set: { status: 'DELETED', updatedAt: new Date() } })
db.ragChunks.deleteMany({ tenantId: 'tenant_ai', sourceDocumentId: 'src_1001' })
```

---

## Indexes

```javascript
db.sourceDocuments.createIndex({ tenantId: 1, status: 1, updatedAt: -1 })
db.sourceDocuments.createIndex({ tenantId: 1, contentHash: 1 }, { unique: true })
db.ragChunks.createIndex({ tenantId: 1, sourceDocumentId: 1, chunkId: 1 }, { unique: true })
db.ragChunks.createIndex({ tenantId: 1, 'metadata.tags': 1, embeddingModel: 1 })
db.ragChunks.createIndex({ tenantId: 1, 'acl.groups': 1 })
db.ingestionJobs.createIndex({ tenantId: 1, status: 1, createdAt: 1 })
```

Vector index note: create an Atlas Vector Search index on `ragChunks.embedding` with metadata filter fields such as `tenantId`, `acl.groups`, `metadata.tags`, and `embeddingModel`.

---

## Aggregation Queries

Chunks by source status:

```javascript
db.ragChunks.aggregate([
  { $match: { tenantId: 'tenant_ai' } },
  { $group: { _id: '$sourceDocumentId', chunks: { $sum: 1 }, model: { $first: '$embeddingModel' } } },
  { $lookup: { from: 'sourceDocuments', localField: '_id', foreignField: '_id', as: 'source' } },
  { $unwind: '$source' },
  { $group: { _id: '$source.status', sources: { $sum: 1 }, chunks: { $sum: '$chunks' } } }
])
```

Ingestion job latency:

```javascript
db.ingestionJobs.aggregate([
  { $match: { tenantId: 'tenant_ai', status: 'COMPLETED' } },
  { $project: { durationMs: { $dateDiff: { startDate: '$createdAt', endDate: '$updatedAt', unit: 'millisecond' } } } },
  { $group: { _id: null, jobs: { $sum: 1 }, avgDurationMs: { $avg: '$durationMs' }, maxDurationMs: { $max: '$durationMs' } } }
])
```

---

## Performance Considerations

- Keep chunk text size predictable.
- Filter by tenant and ACL before or during vector retrieval.
- Track embedding model version for selective re-embedding.
- Avoid large unbounded metadata maps that cannot be indexed well.

---

## Scaling Considerations

- Shard by `{ tenantId: 1, sourceDocumentId: 1 }` for source lifecycle operations.
- Use async ingestion workers for parsing, chunking, embedding, and indexing.
- Use object storage for original documents.
- Consider a specialized vector DB for massive vector-only workloads or advanced ANN tuning.

---

## Security Considerations

- Enforce tenant and ACL filters before retrieval reaches the LLM prompt.
- Delete chunks when source access is revoked or source is deleted.
- Avoid storing secrets in chunk text.
- Return citations for auditability.
- Log retrieval decisions without exposing restricted text.

---

## Optional API Layer

- `POST /rag/sources`
- `POST /rag/sources/{sourceDocumentId}/ingest`
- `GET /rag/sources/{sourceDocumentId}/chunks`
- `POST /rag/search`
- `DELETE /rag/sources/{sourceDocumentId}`

---

## Interview Discussion Points

- Why separate source documents and chunks?
- How do ACL filters interact with vector search?
- What happens when an embedding model changes?
- MongoDB Vector Search vs specialized vector DB?
- How do you prevent deleted or restricted documents from being retrieved?
