# Runbook: Slow Search

## Symptoms

- p99 search latency spike
- search thread-pool rejections
- slow logs show expensive queries
- user-facing search timeouts

## Confirm

1. Identify endpoint and query DSL.
2. Identify index alias and physical indices.
3. Check shard fan-out.
4. Check slow logs/profile API.
5. Check deep pagination, wildcard/regex, scripts, or high-cardinality aggregations.
6. Check heap, GC, disk watermarks, and hot shards.

## Mitigate

- roll back query template
- reduce page size
- disable expensive facets temporarily
- route analytics away from user-facing search
- add cache/fallback for critical path

## Durable Fix

- mapping/query redesign
- alias/reindex with better fields
- shard/ILM redesign
- relevance and performance guardrails

## Interview Summary

```text
Slow search is debugged from query and index evidence first, then shard and node metrics.
```