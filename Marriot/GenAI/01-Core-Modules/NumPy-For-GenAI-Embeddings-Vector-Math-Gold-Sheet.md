# NumPy for GenAI Embeddings & Vector Math - Gold Sheet

> **GenAI Python Support Stack - File 2 of 5**
> For: all learners | Level: beginner to production | Mode: understand the math behind retrieval

---

## 1. Why NumPy Matters In GenAI

GenAI apps use vectors everywhere:

- text embeddings
- image embeddings
- semantic search
- RAG retrieval
- reranking features
- clustering similar documents
- deduplication
- recommendation-like scoring
- eval metrics

NumPy gives fast array operations for vector math.

In RAG, the core retrieval question is often:

```text
Which document vectors are closest to the query vector?
```

NumPy helps answer that efficiently.

---

## 2. Mental Model

An embedding is a list of numbers representing meaning.

```python
query_embedding = [0.12, -0.45, 0.88, ...]
document_embedding = [0.10, -0.40, 0.91, ...]
```

NumPy turns lists into arrays and performs math across the whole array.

```python
import numpy as np

query = np.array([0.12, -0.45, 0.88], dtype=np.float32)
doc = np.array([0.10, -0.40, 0.91], dtype=np.float32)

print(query.shape)  # (3,)
```

For many documents:

```python
docs = np.array([
    [0.10, -0.40, 0.91],
    [0.80, 0.20, -0.10],
    [0.11, -0.46, 0.86],
], dtype=np.float32)

print(docs.shape)  # (3, 3) -> 3 docs, 3 dimensions
```

---

## 3. Java Developer Bridge

| Java Concept | NumPy Equivalent |
|---|---|
| `double[]` | 1D `np.ndarray` |
| `double[][]` | 2D `np.ndarray` |
| for-loop over arrays | vectorized operation |
| Apache Commons Math / ND4J | NumPy for core array math |
| primitive array memory efficiency | NumPy compact numeric arrays |
| Java Stream map/reduce | NumPy broadcasting/reductions |

Key difference: NumPy arrays are fixed-type and vectorized. Python lists are generic object containers.

---

## 4. Python List vs NumPy Array

Python list:

```python
values = [1.0, 2.0, 3.0]
print(values * 2)  # [1.0, 2.0, 3.0, 1.0, 2.0, 3.0]
```

NumPy array:

```python
import numpy as np

values = np.array([1.0, 2.0, 3.0])
print(values * 2)  # [2. 4. 6.]
```

A list repeats. An array performs numeric multiplication.

This matters for embeddings because you want math, not list behavior.

---

## 5. Vector Similarity Basics

### Dot Product

```python
import numpy as np

a = np.array([1, 2, 3], dtype=np.float32)
b = np.array([4, 5, 6], dtype=np.float32)

print(np.dot(a, b))  # 32.0
```

Dot product is large when vectors point in a similar direction and have large magnitude.

### Vector Norm

```python
print(np.linalg.norm(a))  # length/magnitude
```

### Cosine Similarity

Cosine similarity measures direction, not magnitude.

```python
def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    denominator = np.linalg.norm(a) * np.linalg.norm(b)
    if denominator == 0:
        return 0.0
    return float(np.dot(a, b) / denominator)

print(cosine_similarity(a, b))
```

Range:

```text
1.0   -> same direction
0.0   -> unrelated / orthogonal
-1.0  -> opposite direction
```

For many embedding models, cosine similarity is the default retrieval score.

---

## 6. Normalize Vectors For Fast Search

If vectors are normalized to unit length, cosine similarity becomes a dot product.

```python
def normalize(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    norms = np.where(norms == 0, 1, norms)
    return matrix / norms

query = np.array([[0.12, -0.45, 0.88]], dtype=np.float32)
docs = np.array([
    [0.10, -0.40, 0.91],
    [0.80, 0.20, -0.10],
    [0.11, -0.46, 0.86],
], dtype=np.float32)

query_norm = normalize(query)
docs_norm = normalize(docs)

scores = docs_norm @ query_norm.T
print(scores.ravel())
```

`@` is matrix multiplication.

Shape check:

```text
docs_norm:  (num_docs, dimensions)
query_norm: (1, dimensions)
query_norm.T: (dimensions, 1)
scores:     (num_docs, 1)
```

---

## 7. Top-K Retrieval With NumPy

```python
import numpy as np

scores = np.array([0.82, 0.15, 0.91, 0.44, 0.73])
top_k = 3

indices = np.argpartition(scores, -top_k)[-top_k:]
indices = indices[np.argsort(scores[indices])[::-1]]

print(indices)          # indexes of top 3 scores
print(scores[indices])  # sorted top scores
```

Why not just sort everything?

- full sort: O(n log n)
- top-k partition: roughly O(n) plus sorting k items

For large retrieval sets, top-k matters.

---

## 8. Mini RAG Retrieval Example

```python
import numpy as np

chunks = [
    "Python uses reference counting and cyclic GC.",
    "FastAPI uses Pydantic for request validation.",
    "Kafka partitions distribute messages across brokers.",
    "Cosine similarity compares embedding direction.",
]

# Toy embeddings for demo only. Real embeddings come from an embedding model.
doc_embeddings = np.array([
    [0.9, 0.1, 0.1],
    [0.8, 0.2, 0.2],
    [0.1, 0.9, 0.1],
    [0.2, 0.1, 0.9],
], dtype=np.float32)

query_embedding = np.array([[0.85, 0.15, 0.15]], dtype=np.float32)

def normalize(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    norms = np.where(norms == 0, 1, norms)
    return matrix / norms

docs_norm = normalize(doc_embeddings)
query_norm = normalize(query_embedding)

scores = (docs_norm @ query_norm.T).ravel()
top_indices = np.argsort(scores)[::-1][:2]

for index in top_indices:
    print(f"score={scores[index]:.3f} chunk={chunks[index]}")
```

Expected shape of result:

```text
score=... chunk=Python uses reference counting and cyclic GC.
score=... chunk=FastAPI uses Pydantic for request validation.
```

---

## 9. Batch Embedding Shape Discipline

Most embedding APIs return:

```text
list[list[float]]
```

Convert once:

```python
embeddings = np.array(provider_response_embeddings, dtype=np.float32)
```

Validate shape:

```python
if embeddings.ndim != 2:
    raise ValueError("Expected 2D embedding matrix")

num_items, dimensions = embeddings.shape
print(num_items, dimensions)
```

Common dimensions:

```text
384, 768, 1024, 1536, 3072
```

Do not hard-code a dimension unless you own the embedding model config.

---

## 10. Similarity Metrics

| Metric | Formula Idea | Use Case |
|---|---|---|
| cosine similarity | angle/direction | common text embedding retrieval |
| dot product | magnitude + direction | when model trained for dot-product scoring |
| Euclidean distance | geometric distance | clustering, some vector indexes |
| Manhattan distance | absolute coordinate distance | less common in text embeddings |

Important: Use the metric your embedding model/vector DB expects.

---

## 11. Performance Rules

Do:

- use `float32` for embeddings unless you need `float64`
- normalize vectors once and cache normalized form
- batch operations instead of Python loops
- check shapes before matrix multiplication
- use vector DB / ANN index for large-scale search

Avoid:

- computing similarity in pure Python loops for large datasets
- mixing dimensions from different embedding models
- comparing unnormalized vectors with cosine logic
- silently accepting zero vectors
- storing huge embeddings in Pandas cells for heavy math

---

## 12. Common NumPy Traps In GenAI

| Trap | Symptom | Fix |
|---|---|---|
| Wrong shape `(dim,)` vs `(1, dim)` | matrix multiplication errors | reshape query to 2D |
| Zero vector | division by zero / NaN | guard norm with `np.where` |
| Mixed embedding dimensions | ValueError in array creation or matmul | validate dimension per model |
| Python list multiplication | repeats list instead of numeric multiply | convert to `np.array` |
| `float64` by default | double memory usage | use `dtype=np.float32` |
| Full sort for top-k | slow retrieval | use `argpartition` or vector DB |
| Ignoring model metric | poor retrieval quality | follow embedding model docs |

---

## 13. Mini-Lab: Build Top-K Search

Create `lab_numpy_topk.py`:

```python
import numpy as np

rng = np.random.default_rng(seed=42)
num_docs = 1000
dimensions = 128

docs = rng.normal(size=(num_docs, dimensions)).astype(np.float32)
query = rng.normal(size=(1, dimensions)).astype(np.float32)

def normalize(matrix: np.ndarray) -> np.ndarray:
    norms = np.linalg.norm(matrix, axis=1, keepdims=True)
    norms = np.where(norms == 0, 1, norms)
    return matrix / norms

docs = normalize(docs)
query = normalize(query)

scores = (docs @ query.T).ravel()
top_k = 5
indices = np.argpartition(scores, -top_k)[-top_k:]
indices = indices[np.argsort(scores[indices])[::-1]]

print("Top scores:")
for index in indices:
    print(index, round(float(scores[index]), 4))
```

Challenge:

- Change `num_docs` to 1,000,000.
- Time full sort vs `argpartition`.
- Observe why approximate vector indexes exist.

---

## 14. Hot Interview Q&A

**Q1: Why does NumPy matter for embeddings?**
> Embeddings are numeric vectors. NumPy lets us normalize vectors, compute cosine similarity, batch matrix operations, and perform top-k retrieval efficiently.

**Q2: What is cosine similarity?**
> It is the dot product of two vectors divided by the product of their magnitudes. It measures direction similarity, which is why it is common for semantic text embeddings.

**Q3: Why normalize embeddings?**
> Once vectors are unit length, cosine similarity becomes a simple dot product. That makes scoring faster and easier to batch.

**Q4: Why use `float32`?**
> Embeddings rarely need `float64` precision. `float32` uses half the memory and is usually what embedding/vector systems expect.

**Q5: When is NumPy not enough?**
> For millions or billions of vectors, brute-force NumPy search is too slow or memory-heavy. Use a vector database or approximate nearest neighbor index such as FAISS, HNSW, Milvus, Pinecone, Weaviate, or pgvector.

---

## 15. Final Revision Checklist

- [ ] Can convert embedding lists to `np.ndarray`
- [ ] Can explain array shape `(num_docs, dimensions)`
- [ ] Can compute dot product and vector norm
- [ ] Can implement cosine similarity
- [ ] Can normalize vectors safely with zero-vector guard
- [ ] Can compute top-k scores with `argpartition`
- [ ] Can explain why vector DBs are needed at scale
- [ ] Can avoid list multiplication and dtype traps
- [ ] Can explain NumPy to a Java developer using primitive array/vector math mapping
