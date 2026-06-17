# Search Engine ElasticSearch-Like - End-to-End System Design

> Goal: design a distributed search engine that supports document ingestion, analyzers, inverted indexes, shards, replicas, near-real-time search, ranking, filters, aggregations, and cluster operations.

---

## How To Use This File

- Use this when the interview asks for ElasticSearch-like search, internal search platform, document indexing service, log search, product search, or full-text search.
- Start with documents and inverted index, then add analyzers, index segments, shards, replicas, refresh, query execution, ranking, filters, and aggregations.
- Keep one idea sharp: a search engine is an index-first database optimized for retrieval by terms and filters, not a row-store that scans documents.
- In interviews, distinguish full web search from a bounded search engine cluster that indexes a known corpus.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Search engine focus |
|---|---|---|
| Problem understanding | Can define searchable corpus | index documents, update/delete, query, filter, rank, aggregate |
| HLD | Can design distributed index | coordinator, primary shards, replicas, segment store, query fanout |
| LLD | Can model indexing internals | `Document`, `Analyzer`, `Posting`, `Segment`, `Shard`, `QueryPlan` |
| Machine coding | Can implement toy engine | analyzer, inverted index, boolean query, BM25-like score |
| Traffic spikes | Can protect cluster | hot shards, bulk ingest, query cache, circuit breakers |
| Scale | Can reason cluster ops | shard sizing, replication, rebalancing, refresh/merge tradeoffs |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create indexes with mappings/settings.
- Index documents.
- Update and delete documents.
- Search documents by full-text query.
- Filter documents by structured fields.
- Rank results by relevance.
- Support pagination.
- Support sorting by field.
- Support aggregations/facets.
- Support near-real-time search after document ingest.
- Support multiple shards and replicas.
- Support cluster scaling and shard rebalancing.

Optional requirements to clarify:

- Is this for product search, log search, user search, document search, or general search?
- Do we need fuzzy search, synonyms, stemming, or autocomplete?
- Do we need vector search/semantic search?
- Do we need multi-tenant indexes?
- What consistency is expected after writes?
- Are aggregations required?
- Are nested documents required?

Out of scope unless interviewer asks:

- Full web crawler.
- Full ML ranking platform.
- Full SQL engine.
- Full security/tenant billing platform.

## 1.2 Non-Functional Requirements

Latency:

- Search should respond quickly, often p95 under 100 to 300 ms depending corpus/query.
- Term and filter queries should avoid full scans.
- Aggregations may be slower but should be bounded.

Throughput:

- Support high document ingestion rate.
- Support high query concurrency.
- Support bulk indexing.

Availability:

- Replicas should serve reads if primary shard fails.
- Cluster should continue with degraded replication if possible.
- Index refresh/merge should not block serving for too long.

Consistency:

- Search is near-real-time, not necessarily immediately consistent.
- Document GET by ID can be stronger than search visibility.
- Updates/deletes must eventually reflect in search.

Operability:

- Support shard placement, rebalancing, snapshots, metrics, and backpressure.

## 1.3 Constraints

- Inverted indexes are optimized for search, but updates are expensive compared with appending.
- Index segments are often immutable, so deletes/updates are represented as tombstones plus new versions.
- Too many small segments hurt query performance.
- Too many shards increase coordination overhead.
- Very large shards are hard to move and recover.
- Aggregations can consume significant CPU/memory.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Documents | 1B+ |
| Index size | 10 TB+ |
| Ingest rate | 100K docs/sec peak |
| Query rate | 50K queries/sec |
| Shards | hundreds to thousands |
| Replication factor | 2 to 3 |
| Refresh interval | 1 to 30 seconds |
| P95 search latency | 100 to 300 ms typical target |

Back-of-the-envelope:

- `1B docs` cannot live on one machine comfortably.
- Index must be partitioned into shards.
- Each shard can be searched independently.
- Coordinator merges top-K results from many shards.

## 1.5 Clarifying Questions To Ask

- What is the document size and total corpus size?
- What query types are required?
- How fresh must search results be after writes?
- Are updates frequent or mostly append-only?
- Do we need strict ordering/pagination beyond top-K?
- Are aggregations/facets required?
- What availability and replication targets are expected?

Strong interview framing:

> I will design this as a distributed inverted-index service. Writes go to primary shards and segment builders. Search requests fan out to shard replicas, each shard returns local top-K results, and a coordinator merges results globally.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Clients
  |
  v
Search API / Coordinator
  |
  +--> Cluster Metadata Service
  +--> Index Management Service
  +--> Query Planner
  +--> Shard Router
          |
          +--> Shard 0 primary + replicas
          +--> Shard 1 primary + replicas
          +--> Shard N primary + replicas

Each Shard:
  Write Buffer
  Transaction Log
  Analyzer
  Segment Builder
  Immutable Segments
  Delete Tombstones
  Query Executor
  Segment Merger
```

Write path:

```text
Client -> Coordinator -> primary shard -> translog -> memory buffer -> replica -> ack
Refresh -> memory buffer becomes searchable segment
Merge -> small segments compacted into larger segments
```

Read path:

```text
Client -> Coordinator -> query planner -> shard fanout -> local top-K -> merge -> result fetch -> response
```

## 2.2 APIs

Create index:

```http
PUT /v1/indexes/products

{
  "settings": {
    "shards": 12,
    "replicas": 2,
    "refreshIntervalSeconds": 5
  },
  "mappings": {
    "title": {"type": "text", "analyzer": "english"},
    "brand": {"type": "keyword"},
    "price": {"type": "long"},
    "category": {"type": "keyword"}
  }
}
```

Index document:

```http
PUT /v1/indexes/products/documents/sku_1

{
  "title": "Wireless Noise Cancelling Headphones",
  "brand": "Acme",
  "price": 12999,
  "category": "electronics"
}
```

Search:

```http
POST /v1/indexes/products/search

{
  "query": {
    "match": {"title": "wireless headphones"}
  },
  "filter": {
    "term": {"category": "electronics"}
  },
  "sort": ["_score", {"price": "asc"}],
  "from": 0,
  "size": 10,
  "aggs": {
    "brands": {"terms": {"field": "brand"}}
  }
}
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| Coordinator | receives requests, fans out, merges results |
| Cluster Metadata | index settings, shard allocation, node health |
| Analyzer | tokenization, lowercasing, stemming, synonyms |
| Primary Shard | owns writes for shard |
| Replica Shard | serves reads and provides failover |
| Translog | durable write-ahead log |
| Segment Builder | builds immutable index segments |
| Segment Merger | compacts segments and applies tombstones |
| Query Planner | translates DSL to executable query plan |
| Query Executor | runs term/filter/range queries per shard |
| Aggregation Engine | computes facets/aggregations |

### Analyzer

Analyzer pipeline:

```text
input text
  -> character filters
  -> tokenizer
  -> lowercase
  -> stopword removal
  -> stemming/synonyms
  -> terms
```

Example:

```text
"Wireless Running Shoes!"
  -> ["wireless", "run", "shoe"]
```

Why analyzer matters:

- Index-time analyzer and query-time analyzer should usually be compatible.
- Bad analyzers cause missing or noisy results.
- Keyword fields should not be tokenized like text fields.

### Inverted Index

```text
term -> postings list
posting = (docId, termFrequency, positions, fieldNorms)
```

Structured fields:

- keyword terms for exact filters
- numeric/range indexes for price/time
- doc values/columnar structures for sorting and aggregations

### Segments

Search engines often use immutable segments:

```text
segment_1: docs 1..10000
segment_2: docs 10001..20000
delete bitmap: marks deleted docs
```

Benefits:

- Immutable files are easy to cache and search.
- Writes append new segments.
- Replicas can copy segment files.

Costs:

- Deletes are tombstones until merge.
- Too many segments increase query work.
- Merge consumes IO/CPU.

### Query Execution

Boolean query example:

```text
must: title contains "wireless"
filter: category = electronics
sort: score desc
```

Execution:

1. Analyze query terms.
2. Fetch postings for terms.
3. Intersect/filter doc ID sets.
4. Score matching docs.
5. Maintain local top-K heap.
6. Return doc IDs and scores to coordinator.
7. Coordinator merges shard top-K.
8. Fetch stored fields/source for final hits.

## 2.4 Data Layer

Storage layout:

| Data | Storage |
|---|---|
| index metadata | strongly consistent metadata store |
| document source | segment stored fields or document store |
| inverted index | segment files |
| translog | local durable log replicated to replicas |
| delete tombstones | segment delete bitmaps |
| doc values | columnar files for sort/aggs |
| snapshots | object storage |

Core metadata:

```text
Index(name, settings, mappings, createdAt)
Shard(indexName, shardId, primaryNode, replicaNodes, state)
Segment(segmentId, shardId, generation, docCount, deletedCount)
Document(_id, routingKey, version, source)
```

## 2.5 Scalability

Shard routing:

```text
shardId = hash(routingKey or documentId) % numberOfPrimaryShards
```

Read scaling:

- Add replicas.
- Cache filters and hot queries.
- Route search to least-loaded replica.
- Use circuit breakers for expensive queries.

Write scaling:

- Increase primary shards for write parallelism.
- Use bulk indexing.
- Tune refresh interval.
- Throttle merges.
- Use routing keys to avoid random fanout when possible.

Shard sizing rules of thumb:

- Too small: too many shards, high coordination overhead.
- Too large: slow recovery and rebalancing.
- Practical shard size depends on workload, often tens of GB as a starting discussion point.

## 2.6 Caching

Useful caches:

- query result cache for repeated exact queries
- filter bitset cache
- field data/doc values cache
- OS page cache for segment files
- request-level aggregation cache

Cache cautions:

- High-cardinality queries may not benefit.
- Personalized queries reduce cache hit rate.
- Refresh/invalidation affects cache usefulness.
- Large aggregations can blow memory.

## 2.7 Async Systems

Internal async work:

- refresh: makes buffered writes searchable
- flush: commits translog to stable segments
- merge: compacts small segments
- replication: sync primary writes to replicas
- recovery: rebuild shard after node failure
- snapshot: copy segment files to object storage
- rebalancing: move shards across nodes

Events:

| Event | Consumers |
|---|---|
| DocumentIndexed | replica, metrics |
| SegmentRefreshed | query cache invalidation |
| SegmentMerged | old segment cleanup |
| ShardFailed | cluster allocator |
| NodeJoined | rebalancer |
| SnapshotCompleted | operations |

## 2.8 Safety And Failure Handling

| Failure | Mitigation |
|---|---|
| primary shard fails | promote replica |
| replica lag | route reads to healthy replica |
| node disk full | shard relocation, write block |
| heavy query | timeout, circuit breaker, max result window |
| merge overload | throttle merges/indexing |
| translog corruption | recover from replica/snapshot if needed |
| bad mapping change | reject incompatible update |
| hot shard | custom routing, split index, rebalance |

## 2.9 Observability

Metrics:

- query QPS and latency
- ingest QPS and latency
- refresh latency
- merge time and backlog
- segment count per shard
- heap/memory usage
- disk usage
- cache hit rate
- shard failures
- replica lag
- rejected queries/writes

Logs/traces:

- `indexName`
- `shardId`
- `nodeId`
- `queryId`
- `segmentGeneration`
- `tookMs`
- `timedOut`

Alerts:

- disk above threshold
- heap pressure
- shard unassigned
- query latency spike
- merge backlog
- indexing rejection spike

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| frequent refresh | fresher search | more small segments and overhead |
| slower refresh | better ingest throughput | stale search results |
| many shards | parallelism | coordination overhead |
| few shards | simpler and efficient | harder to scale/rebalance |
| replicas | read scale and HA | storage/write amplification |
| strict consistency | predictable reads | slower writes |
| near-real-time search | high throughput | recent writes may not appear immediately |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
IndexDefinition
Mapping
Analyzer
Token
Document
Posting
Segment
Shard
Replica
Query
QueryPlan
SearchHit
AggregationResult
ClusterState
```

## 3.2 OOP Fundamentals

Encapsulation:

- `Analyzer` owns tokenization rules.
- `Segment` owns immutable postings and doc values.
- `Shard` owns segments, refresh, search, and write routing.
- `Coordinator` owns fanout and merge.

Polymorphism:

- `Query` types: term, match, bool, range, prefix.
- `Analyzer` types: standard, keyword, language-specific.
- `Scorer` types: TF-IDF, BM25-like, custom field boost.

Composition:

- `SearchService` composes coordinator, metadata, shard clients, scorer, and result fetcher.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Strategy | analyzer and scoring |
| Composite | boolean query tree |
| Repository | metadata/document access |
| Builder | query plan construction |
| Observer | cluster state changes |
| Circuit Breaker | expensive query protection |

## 3.4 Sequence Diagram

Index document:

```text
Client
  -> Coordinator: index(index, id, document)
  -> Metadata: resolve shard
  -> PrimaryShard: append translog
  -> Analyzer: tokenize fields
  -> MemoryBuffer: add postings
  -> ReplicaShard: replicate operation
  -> Client: ack
```

Search:

```text
Client
  -> Coordinator: search(query)
  -> QueryPlanner: build plan
  -> ShardRouter: select replicas
  -> Shards: local top-K
  -> Coordinator: merge top-K
  -> Shards/Store: fetch fields
  -> Client: hits and aggs
```

## 3.5 Edge Cases

- Document updated before previous version is refreshed.
- Delete arrives for document in old segment.
- Query uses field that mapping does not contain.
- User asks for page 100000.
- Aggregation over high-cardinality field.
- Node fails during primary write.
- Replica is stale.
- Refresh creates too many segments.
- Hot tenant causes shard imbalance.
- Synonym update requires reindex.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
analysis/
  analyzer.py
index/
  inverted_index.py
  segment.py
query/
  query.py
  scorer.py
services/
  search_service.py
```

## 4.2 Core Logic Implementation

Tiny search engine with analyzers, filters, and scoring:

```python
from collections import Counter, defaultdict
from dataclasses import dataclass
import math
import re


class Analyzer:
    def analyze(self, text: str) -> list[str]:
        return re.findall(r"[a-z0-9]+", text.lower())


@dataclass(frozen=True)
class Document:
    doc_id: str
    fields: dict[str, object]


@dataclass(frozen=True)
class Hit:
    doc_id: str
    score: float
    source: dict[str, object]


class InvertedIndex:
    def __init__(self, analyzer: Analyzer) -> None:
        self.analyzer = analyzer
        self.documents: dict[str, Document] = {}
        self.text_index: dict[str, dict[str, int]] = defaultdict(dict)
        self.keyword_index: dict[tuple[str, str], set[str]] = defaultdict(set)

    def index(self, doc: Document) -> None:
        self.documents[doc.doc_id] = doc

        text = " ".join(
            str(value)
            for value in doc.fields.values()
            if isinstance(value, str)
        )
        counts = Counter(self.analyzer.analyze(text))
        for term, count in counts.items():
            self.text_index[term][doc.doc_id] = count

        for field, value in doc.fields.items():
            if isinstance(value, str):
                self.keyword_index[(field, value.lower())].add(doc.doc_id)

    def search(self, text_query: str, filters: dict[str, str], top_k: int = 10) -> list[Hit]:
        terms = self.analyzer.analyze(text_query)
        candidates: set[str] | None = None
        scores: dict[str, float] = defaultdict(float)
        total_docs = max(len(self.documents), 1)

        for term in terms:
            postings = self.text_index.get(term, {})
            term_docs = set(postings.keys())
            candidates = term_docs if candidates is None else candidates | term_docs
            idf = math.log((total_docs + 1) / (len(postings) + 1)) + 1
            for doc_id, tf in postings.items():
                scores[doc_id] += tf * idf

        if candidates is None:
            candidates = set(self.documents.keys())

        for field, value in filters.items():
            allowed = self.keyword_index.get((field, value.lower()), set())
            candidates &= allowed

        ranked = sorted(candidates, key=lambda doc_id: scores[doc_id], reverse=True)
        return [
            Hit(doc_id=doc_id, score=scores[doc_id], source=self.documents[doc_id].fields)
            for doc_id in ranked[:top_k]
        ]
```

What this demonstrates:

- Analyzer converts raw text to searchable terms.
- Inverted index maps terms to document IDs and term frequencies.
- Keyword index supports exact filters.
- Query computes candidate set and scores.
- Real systems add segments, replicas, BM25, range indexes, aggregations, and cluster routing.

## 4.3 Testing Thinking

Test cases:

- Search returns docs containing query term.
- Filter restricts results to matching field.
- Common term has lower IDF than rare term.
- Update replaces document version.
- Delete removes document from visible results.
- Query against missing field returns clear error or zero results.
- Deep pagination is rejected or limited.
- Expensive aggregation is bounded.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Bulk ingest spike.
- Dashboard/log-search query storm.
- Expensive wildcard query.
- Aggregation over high-cardinality field.
- Hot shard from bad routing key.
- Node failure causing replica recovery load.

## 5.2 Immediate Response

- Apply write backpressure.
- Increase refresh interval during bulk ingest.
- Reject or timeout expensive queries.
- Use query and filter cache.
- Route reads to replicas.
- Rebalance hot shards.
- Limit result window and aggregation cardinality.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| ingest overload | throttle bulk writes |
| query overload | reject heavy queries first |
| replica unavailable | serve from remaining replicas |
| high heap pressure | circuit-break aggregations |
| merge backlog | slow indexing/refresh |
| disk pressure | block writes and alert |

## 5.4 Spike Interview Answer

> During a spike, I would protect the cluster with backpressure and circuit breakers. For ingest spikes, I would use bulk APIs, slower refresh, and merge throttling. For query spikes, I would cache filters/results, reject expensive queries, cap pagination, and route to replicas. Search availability is preserved by replicas and shard-level timeouts.

---

# 6. Scaling Beyond One Cluster

## 6.1 Multi-Cluster Patterns

Use cases:

- region-local search
- disaster recovery
- read-only follower clusters
- tenant isolation
- large log-search deployments

Patterns:

```text
Primary ingest cluster
  -> replicated/follower search clusters
  -> regional query endpoints
```

## 6.2 Operational Concerns

- Snapshot and restore indexes.
- Rebalance shards as nodes join/leave.
- Monitor shard sizes.
- Avoid oversharding.
- Use index lifecycle policies for time-series/log data.
- Roll over indexes by time or size.

## 6.3 Interview Answer

> At scale, I would partition documents into primary shards with replicas. Writes go to primaries and are replicated. Search fans out to shard replicas and the coordinator merges top-K. Segments are immutable and made visible through refresh, while background merges compact them. For high availability, I would use replica promotion, snapshots, and careful shard sizing.

---

# 7. Final Interview Playbook

Start with:

> An ElasticSearch-like engine is a distributed inverted-index database. Writes build searchable segments; reads fan out to shards and merge ranked results.

Then cover:

1. Documents, mappings, and analyzers.
2. Inverted index and postings.
3. Primary shards and replicas.
4. Write path: translog, buffer, refresh, segment.
5. Read path: query plan, shard fanout, local top-K, merge.
6. Filters, scoring, aggregations, pagination.
7. Failures, hot shards, and operational tradeoffs.

Common traps:

- Treating full-text search as SQL scan.
- Forgetting analyzer mismatch.
- Forgetting refresh delay.
- Creating too many shards.
- Ignoring segment merge cost.
- Allowing unbounded deep pagination or aggregations.

---

# 8. Fast Recall Rules

- Search engine = analyzer + inverted index + shards + replicas.
- Terms map to postings lists.
- Segments are often immutable.
- Refresh makes recent writes searchable.
- Merge compacts old segments and tombstones.
- Search fans out to shards and merges top-K.
- Replicas provide read scale and failover.
- Many tiny shards are bad; giant shards are bad too.
- Expensive queries need circuit breakers.
- Near-real-time means search may lag writes.

