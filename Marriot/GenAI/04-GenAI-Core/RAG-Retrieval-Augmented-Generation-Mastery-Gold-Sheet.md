# RAG - Retrieval Augmented Generation Mastery Gold Sheet

> **GenAI Mastery Track - Core File 2**
> For: all learners | Level: beginner to production | Mode: build reliable retrieval-grounded LLM systems

---

## 1. What RAG Is

RAG means **Retrieval Augmented Generation**.

Instead of asking the model to answer from memory alone, the system retrieves relevant external context and gives it to the model.

```text
user question
  -> rewrite/understand query
  -> retrieve relevant documents/chunks
  -> build prompt with context
  -> LLM generates grounded answer
  -> return answer with citations
```

### Strong Answer

> RAG combines search with generation. Retrieval brings relevant facts into the context window; generation turns those facts into a natural-language answer. It reduces hallucination and enables private/current knowledge, but quality depends heavily on chunking, metadata, retrieval, ranking, prompting, and evals.

---

## 2. When To Use RAG

Use RAG when:

- knowledge changes frequently
- data is private/internal
- answers need citations
- model does not know domain-specific facts
- you need cheaper updates than fine-tuning
- you need traceability to source documents

Do not use RAG when:

- task is pure transformation of user input
- answer does not require external knowledge
- deterministic database query is better
- source data is low quality or untrusted
- latency budget cannot handle retrieval

---

## 3. RAG Pipeline

```text
Offline ingestion:
  documents -> parse -> clean -> chunk -> enrich metadata -> embed -> store

Online query:
  user question -> query rewrite -> embed query -> retrieve -> rerank -> prompt -> generate -> validate -> cite/evaluate
```

Separate offline and online concerns.

### Offline Ingestion

- load documents
- extract text
- normalize formatting
- split into chunks
- compute metadata
- create embeddings
- write to vector store

### Online Query

- understand user intent
- retrieve candidate chunks
- rerank if needed
- build context
- generate answer
- validate output
- log trace and metrics

---

## 4. Document Loading

Documents may come from:

- PDFs
- HTML pages
- Markdown files
- Confluence/SharePoint/Google Drive
- databases
- tickets
- source code repos
- APIs

### Loading Rules

- preserve source path/URL
- preserve page/section headings
- store last modified time
- compute content hash
- capture permissions/tenant info
- record parser version

### Metadata Example

```python
from pydantic import BaseModel

class DocumentMetadata(BaseModel):
    document_id: str
    source_uri: str
    source_type: str
    content_hash: str
    last_modified: str | None = None
    access_group: str | None = None
```

Metadata is not decoration. It is how you debug retrieval and enforce security.

---

## 5. Chunking

Chunking splits documents into retrieval-sized pieces.

Bad chunking causes bad RAG.

### Chunking Strategies

| Strategy | Use Case |
|---|---|
| fixed token window | simple baseline |
| sliding window overlap | preserve context across boundaries |
| heading-aware chunking | Markdown/docs/wiki pages |
| semantic chunking | split by meaning/topic |
| code-aware chunking | functions/classes/modules |
| table-aware chunking | financial/analytics documents |

### Chunk Size

Typical starting point:

```text
300-800 tokens per chunk
10-20 percent overlap
```

But tune by evals, not vibes.

### Chunk Metadata

```python
class Chunk(BaseModel):
    chunk_id: str
    document_id: str
    text: str
    heading_path: list[str]
    chunk_index: int
    token_count: int
    content_hash: str
```

---

## 6. Embeddings

An embedding maps text to a vector.

```text
"What is the refund policy?" -> [0.12, -0.04, 0.88, ...]
```

Similar meaning should produce nearby vectors.

### Embedding Rules

- use same embedding model for documents and queries
- store embedding model name/version
- normalize if your similarity metric expects it
- do not mix vector dimensions
- re-embed when chunking or embedding model changes

### Batch Embedding

Batch document embeddings for throughput and cost efficiency.

```python
async def embed_documents(texts: list[str]) -> list[list[float]]:
    # provider call here
    ...
```

---

## 7. Vector Stores

Vector store responsibilities:

- store vectors
- store metadata
- run similarity search
- filter by metadata
- sometimes hybrid search

Options:

| Store | Best For |
|---|---|
| NumPy/FAISS local | learning/prototyping |
| Chroma | local apps/dev |
| pgvector | Postgres-centric apps |
| Pinecone | managed vector search |
| Weaviate/Milvus | scalable vector search |
| Elasticsearch/OpenSearch | hybrid keyword + vector search |

### Production Rule

Use metadata filters for tenant/security boundaries. Do not retrieve unauthorized chunks and hope the model ignores them.

---

## 8. Retrieval Techniques

### Dense Retrieval

Embedding similarity.

Good for semantic matches:

```text
question: "How do I change my password?"
chunk: "Reset your account credentials from profile settings."
```

### Sparse Retrieval

Keyword/BM25 search.

Good for exact terms:

- error codes
- product IDs
- class names
- legal phrases

### Hybrid Retrieval

Combine dense + sparse.

Often best in production.

```text
score = alpha * dense_score + beta * sparse_score
```

### Reranking

Retrieve many candidates, then rerank with a stronger model.

```text
vector search top 50 -> reranker top 5 -> LLM context
```

Reranking improves precision but adds latency/cost.

---

## 9. Prompt Construction For RAG

Template:

```text
You are a helpful assistant. Answer using only the provided context.
If the context does not contain the answer, say you do not know.
Cite sources using [source_id].

Question:
{question}

Context:
{retrieved_chunks}

Answer:
```

### Context Formatting

```text
[source: doc-123 chunk-4]
Password resets expire after 15 minutes.

[source: doc-456 chunk-2]
Admins can force reset from the user management page.
```

### Rules

- clearly delimit context
- include source IDs
- tell model what to do when context is insufficient
- keep context concise
- do not include untrusted instructions as instructions

---

## 10. Citations

Citations are not automatic truth.

A model can cite a source that does not support the answer.

### Citation Quality Checks

- cited chunk exists
- cited chunk was actually provided
- answer claim appears in cited chunk
- citation is not just vaguely related
- source is authorized for user

### Strong Answer

> I treat citations as generated output that must be checked. RAG should log which chunks were retrieved and which chunks were cited, then eval should verify citation support.

---

## 11. RAG Evaluation

RAG eval has multiple layers:

| Metric | Question |
|---|---|
| retrieval recall | did we retrieve the needed source? |
| retrieval precision | were retrieved chunks relevant? |
| answer correctness | is final answer correct? |
| groundedness | is answer supported by context? |
| citation accuracy | do citations support claims? |
| refusal correctness | did model say unknown when needed? |
| latency/cost | is it shippable? |

### Minimum Eval Record

```python
from pydantic import BaseModel

class RagEvalRecord(BaseModel):
    question_id: str
    question: str
    expected_source_ids: list[str]
    retrieved_source_ids: list[str]
    answer: str
    cited_source_ids: list[str]
    passed: bool
    failure_reason: str | None = None
```

---

## 12. RAG Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| no relevant chunks retrieved | bad query/chunking/embedding | query rewrite, better chunking, hybrid search |
| too many irrelevant chunks | weak retriever | metadata filters, reranker |
| answer ignores context | prompt issue or context overload | stricter prompt, reduce context |
| hallucinated answer | missing context or no refusal rule | allow unknown, groundedness check |
| wrong citation | model cites loosely | citation verification eval |
| tenant data leak | missing metadata filter | enforce auth filter before retrieval |
| high latency | reranker/too many chunks | cache, reduce top_k, async batching |
| stale answer | old index | incremental reindexing and freshness metadata |

---

## 13. Security In RAG

Key concerns:

- prompt injection inside documents
- unauthorized document retrieval
- sensitive data leakage
- stale or poisoned documents
- malicious HTML/PDF content
- over-broad source connectors

### Security Rules

- enforce ACL filters at retrieval time
- sanitize/parse documents safely
- treat retrieved context as untrusted data
- never place secrets in context
- log source IDs for audit
- support document deletion/reindexing

---

## 14. RAG Architecture

```text
api/
  rag_routes.py
services/
  rag_service.py
retrieval/
  loader.py
  chunker.py
  embedder.py
  vector_store.py
  reranker.py
  prompt_builder.py
  citation_checker.py
evals/
  rag_eval_runner.py
  datasets.py
```

Keep components separate so you can improve retrieval without rewriting API code.

---

## 15. Java Developer Bridge

| Java/Search Concept | RAG Equivalent |
|---|---|
| Elasticsearch query | sparse retrieval / hybrid retrieval |
| Repository lookup | retriever/vector store call |
| DTO metadata | chunk metadata schema |
| Batch job | ingestion pipeline |
| Search relevance tuning | retrieval/reranking evals |
| Access control filter | metadata filter before vector search |
| Integration test dataset | RAG golden eval set |

Key shift: RAG is not just "put docs in a vector DB." It is a full search-quality system plus generation.

---

## 16. Hot Interview Q&A

**Q1: What is RAG?**
> RAG retrieves relevant external context and gives it to an LLM so the answer can be grounded in private/current source material.

**Q2: Why not just fine-tune?**
> Fine-tuning changes model behavior/style but is not ideal for frequently changing factual knowledge or citations. RAG is better for dynamic private knowledge that must be traceable.

**Q3: What is the hardest part of RAG?**
> Retrieval quality. If the right context is not retrieved, the model cannot answer correctly. Chunking, metadata, hybrid search, reranking, and evals matter more than the final prompt alone.

**Q4: How do you evaluate RAG?**
> Measure retrieval recall/precision, answer correctness, groundedness, citation accuracy, refusal correctness, latency, and cost on a golden dataset.

**Q5: How do you prevent data leaks in RAG?**
> Enforce access-control metadata filters before retrieval, not after generation. Never retrieve unauthorized chunks into model context.

---

## 17. Final Revision Checklist

- [ ] Can explain offline ingestion vs online query path
- [ ] Can choose chunking strategy and chunk size
- [ ] Can explain embeddings and vector store role
- [ ] Can compare dense, sparse, hybrid retrieval
- [ ] Can explain reranking trade-offs
- [ ] Can write a RAG prompt with citations and unknown behavior
- [ ] Can list RAG evaluation metrics
- [ ] Can diagnose common RAG failures
- [ ] Can explain RAG security and ACL filtering
- [ ] Can map RAG concepts to Java/search/backend equivalents
