# Runbook: High P99 Latency

## Symptoms

- vector search p99 spikes
- RAG endpoint times out
- reranker cost or latency rises
- one tenant is much slower than others

## Confirm

1. Break down latency by embedding, vector query, filter, rerank, source fetch, and LLM.
2. Check topK and reranker depth.
3. Check filter selectivity.
4. Check hot tenant or namespace.
5. Check index/collection health.
6. Check recent deploys or reindexing.

## Mitigate

- lower topK or reranker depth
- add timeout/fallback
- isolate hot tenant
- cache frequent queries
- pause noncritical ingestion if it affects serving

## Durable Fix

- capacity model and replicas
- partition hot corpora
- tune ANN/search parameters
- improve filters and schema
- budget each retrieval stage

## Interview Summary

```text
High vector retrieval p99 must be split by stage: embedding, ANN search, metadata filters, reranker, source fetch, and downstream LLM.
```