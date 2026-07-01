# VectorDB Embeddings and Similarity Metrics - Gold Sheet

> Track File #2 of 30 - Group 01: Starter Path
> For: backend/search/GenAI interviews | Level: beginner | Mode: embeddings, dimensions, metrics

## 1. Embedding

An embedding is a numeric representation of meaning produced by a model.

```text
"reset my password" -> [0.12, -0.08, ...]
```

Similar meaning should produce nearby vectors.

---

## 2. Dimension

Dimension is vector length.

Examples:

- 384
- 768
- 1024
- 1536
- 3072

The vector DB index/collection dimension must match the embedding model dimension. A dimension mismatch is a common production bug during model migration.

---

## 3. Similarity Metrics

| Metric | Best Mental Model | Notes |
|---|---|---|
| cosine | angle similarity | common for normalized semantic embeddings |
| dot product | magnitude plus direction | common with models trained for inner product |
| euclidean | physical distance | useful when model expects L2 distance |

Do not choose metric randomly. Follow the embedding model recommendation.

---

## 4. Normalization

Normalization scales vectors to unit length.

```text
cosine similarity becomes similar to dot product when vectors are normalized
```

If the model or vendor already normalizes vectors, avoid double-transforming without understanding the effect.

---

## 5. Model Versioning

Store embedding model version in metadata:

```json
{
  "embedding_model": "text-embedding-model-v3",
  "embedding_version": "2026-07-01"
}
```

Why:

- old and new embeddings may not be comparable
- reindexing needs rollout tracking
- evaluation needs reproducibility

---

## 6. Interview Summary

```text
An embedding maps content into a fixed-dimensional vector space. The vector DB index dimension and similarity metric must match the embedding model. I would store model version metadata because embedding upgrades often require re-embedding and careful evaluation before switching traffic.
```

---

## 7. Revision Notes

- One-line summary: Embedding model choice defines dimension, metric, and retrieval quality.
- Three keywords: dimension, metric, version.
- One trap: mixing embeddings from incompatible models in one index.