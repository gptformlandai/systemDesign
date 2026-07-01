# Runbook: Metadata ACL Leak

## Symptoms

- user sees cross-tenant result
- denied document reaches reranker
- logs include unauthorized chunk
- golden ACL test fails

## Confirm

1. Inspect exact query filter.
2. Check tenant and ACL metadata on returned records.
3. Check whether reranker received unauthorized candidates.
4. Check stale permission metadata.
5. Check logs/traces for leaked content.

## Mitigate

- disable affected retrieval route
- force strict tenant filter
- remove leaked records from index
- rotate exposed logs if needed

## Durable Fix

- server-side query builder for filters
- mandatory ACL regression tests
- delete/permission event replay
- avoid logging raw retrieved chunks
- isolate high-risk tenants

## Interview Summary

```text
ACL filters must be enforced during retrieval. Post-filtering can leak candidates through rerankers, logs, traces, and metrics.
```