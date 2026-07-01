# Runbook: Embedding Dimension Mismatch

## Symptoms

- upsert fails with dimension error
- query fails after model upgrade
- retrieval quality drops after mixed embeddings
- new index has no useful results

## Confirm

1. Check collection/index dimension.
2. Check embedding model output dimension.
3. Check model version metadata.
4. Check whether old and new vectors are mixed.
5. Check query embedding model.

## Mitigate

- stop writes from incompatible model
- route queries to matching index
- rollback model change
- create versioned index/namespace

## Durable Fix

- store embedding_model and embedding_version metadata
- validate dimension before upsert
- side-by-side reindex for model upgrades
- canary and golden-set evaluation before traffic switch

## Interview Summary

```text
Embedding model changes are index migrations. Dimension, metric, and vector-space compatibility must be verified before rollout.
```