# Runbook: Low Recall

## Symptoms

- expected document missing from topK
- RAG answer lacks evidence
- user success drops
- golden-set recall regresses

## Confirm

1. Reproduce exact query and filters.
2. Check embedding model and dimension.
3. Check chunking and source coverage.
4. Check topK and score threshold.
5. Check metadata filters are not too strict.
6. Check reindex completion and stale vectors.
7. Check reranker suppressing relevant candidates.

## Mitigate

- increase topK temporarily
- rollback chunking/model change
- disable faulty reranker change
- replay failed ingestion

## Durable Fix

- improve chunking
- tune hybrid retrieval
- build better golden set
- version embeddings and run canaries

## Interview Summary

```text
Low recall is debugged from query, embeddings, chunks, filters, topK, freshness, and reranking before blaming the vector database.
```