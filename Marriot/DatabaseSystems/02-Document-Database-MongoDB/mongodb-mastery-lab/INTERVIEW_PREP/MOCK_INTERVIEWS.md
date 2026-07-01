# MongoDB Mock Interviews

## Mock 1: Backend CRUD and Querying

Duration: 30 minutes.

Prompts:

1. Explain MongoDB document model.
2. Write a query for orders by tenant/status/date.
3. Update inventory atomically for reservation.
4. Explain projection and cursor pagination.
5. Identify indexes for the above queries.

Scoring:

- 1: syntax only
- 3: correct syntax and basic reasoning
- 5: production tradeoffs and explain-plan awareness

## Mock 2: Schema Design

Duration: 45 minutes.

Prompt: Design an e-commerce catalog with products, variants, reviews, search filters, and inventory.

Expected discussion:

- product document shape
- variants embedded if bounded
- reviews separate with subset pattern
- attribute pattern
- SKU unique index
- category/price/brand indexes
- Atlas Search for text/autocomplete

## Mock 3: Performance Debugging

Duration: 45 minutes.

Prompt: An endpoint became slow. It lists paid orders for a tenant sorted by newest first.

Expected answer:

- capture query shape
- run explain
- identify `COLLSCAN`/`SORT`
- create `{ tenantId: 1, status: 1, createdAt: -1 }`
- re-run explain
- monitor production

## Mock 4: Distributed Systems

Duration: 60 minutes.

Prompts:

1. Explain replica set failover.
2. Explain majority write concern.
3. Choose a shard key for orders.
4. Explain scatter-gather.
5. Handle tenant skew.

## Mock 5: GenAI/RAG

Duration: 45 minutes.

Prompt: Design a MongoDB-backed RAG metadata and vector search store.

Expected discussion:

- source documents and chunks
- embedding model/version
- tenant and ACL filters
- vector index
- hybrid search
- re-embedding
- deletion and source updates
- evaluation set
