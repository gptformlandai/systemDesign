# Google Search - End-to-End System Design

> Goal: design a web-scale search system from crawling to indexing, ranking, query serving, freshness, spam handling, and global scale.

---

## How To Use This File

- Use this when the interview asks for Google Search, web search, large-scale crawler, search ranking, or internet-scale indexing.
- Start with crawl -> parse -> index -> rank -> serve, then add freshness, spam/safety, query understanding, caching, and global replication.
- Keep one idea sharp: web search is mostly an offline/nearline indexing and ranking pipeline with a very fast online query serving layer.
- In interviews, do not claim you will rank the whole web at request time. Candidate retrieval and multi-stage ranking are the key.

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

| Layer | Interview signal | Google Search focus |
|---|---|---|
| Problem understanding | Can scope web search | crawl pages, index documents, answer queries, rank results |
| HLD | Can split pipelines | crawler, parser, indexer, link graph, ranking, query serving |
| LLD | Can model core entities | `Document`, `TermPosting`, `Shard`, `Query`, `RankedResult` |
| Machine coding | Can implement toy search | inverted index, TF-IDF/BM25-like scoring, PageRank intuition |
| Traffic spikes | Can protect serving | query cache, hot query handling, replica fanout, degraded ranking |
| Global scale | Can reason freshness and regions | regional serving, distributed index, crawl priority, replication |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Discover web pages by crawling links and sitemaps.
- Fetch pages while respecting robots rules and crawl politeness.
- Parse page content, links, title, metadata, language, and canonical URL.
- Deduplicate near-identical pages.
- Build an inverted index from terms to documents.
- Build link graph signals.
- Accept user search queries.
- Retrieve relevant candidate documents.
- Rank results by textual relevance, link authority, freshness, quality, and user context.
- Return top results with title, URL, snippet, and metadata.
- Support result freshness for news/trending pages.
- Support safe-search, spam filtering, and removal requests.

Optional requirements to clarify:

- Is the corpus the open web, a company intranet, or a fixed set of documents?
- Do we need images, videos, news, maps, or shopping verticals?
- Is personalization required?
- Are ads part of the result page?
- Do we need typo correction and query suggestions?
- Do we need multilingual search?
- What freshness SLA is expected?

Out of scope unless interviewer asks:

- Full web browser rendering engine.
- Full ML model training platform internals.
- Full ads auction.
- Full legal/content moderation workflow.
- Complete natural-language question answering.

## 1.2 Non-Functional Requirements

Latency:

- Query response should be fast, commonly p95 under a few hundred milliseconds.
- Online serving cannot scan all documents.
- Hot queries should benefit from caching.

Availability:

- Search should remain available even if crawling/indexing is delayed.
- Query serving should use replicas across regions.
- Partial shard failure should degrade rather than fail the entire query when possible.

Quality:

- Results should be relevant, high-quality, safe, and fresh enough.
- Spam and low-quality pages should be demoted.
- Duplicates and near-duplicates should be collapsed.

Scalability:

- Corpus can be billions to trillions of pages.
- Queries can be millions per second globally.
- Crawling and indexing are continuous.

Compliance/safety:

- Respect robots directives.
- Support content removals.
- Enforce safe-search and policy filters.
- Keep audit logs for removal and policy decisions.

## 1.3 Constraints

- The web is huge and constantly changing.
- Many pages are duplicates, spam, or low quality.
- Fetching pages too aggressively can harm websites.
- Query patterns are highly skewed; a small number of queries are very hot.
- Ranking must be multi-stage because scoring the entire corpus online is impossible.
- Index updates need freshness without breaking serving stability.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Indexed pages | 100B+ |
| Crawled pages/day | billions |
| Queries/day | 10B+ |
| Peak queries/sec | millions globally |
| Average query terms | 2 to 5 |
| Result page size | top 10 to 50 |
| Freshness SLA | minutes for news, hours/days for long-tail pages |
| Availability target | 99.99%+ query serving |

Back-of-the-envelope:

- `10B queries/day` is about `115K/sec` average globally.
- Peak can be many times average.
- The inverted index is much larger than memory for one machine.
- Serving must shard the index and query many shards in parallel.

## 1.5 Clarifying Questions To Ask

- Are we indexing the entire web or a smaller corpus?
- What result types are required: pages only or also images/videos/news?
- What freshness target matters most?
- What ranking sophistication is expected?
- Should personalization/location be included?
- How strict are removal and safety requirements?
- What latency and availability targets are expected?

Strong interview framing:

> I will design web search as two main systems: an offline/nearline pipeline that crawls, parses, deduplicates, and builds indexes; and an online serving path that retrieves candidates from index shards and ranks them with fast multi-stage ranking.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Web
  |
  v
URL Frontier
  |
  v
Crawler Workers
  |
  v
Fetch Store / Raw Page Store
  |
  v
Parser + Extractor
  |
  +--> Content Store
  +--> Link Graph Builder
  +--> Deduplication Service
  +--> Indexing Pipeline
             |
             v
        Inverted Index
             |
             v
       Index Shards + Replicas

User Query
  -> Query Frontend
  -> Query Understanding
  -> Index Shard Fanout
  -> Candidate Merge
  -> Ranking Service
  -> Snippet Service
  -> Results Page
```

Two big flows:

```text
Offline/nearline: crawl -> parse -> dedupe -> index -> rank signals -> publish shards
Online: query -> retrieve candidates -> rank -> filter -> render
```

## 2.2 APIs

Search API:

```http
GET /v1/search?q=distributed+cache&page=1&safe=true&locale=en-US
```

Response:

```json
{
  "query": "distributed cache",
  "results": [
    {
      "url": "https://example.com/cache",
      "title": "Distributed Cache Explained",
      "snippet": "A distributed cache stores frequently accessed data...",
      "score": 0.98
    }
  ],
  "nextPageToken": "token_2"
}
```

Crawler control APIs:

```http
POST /v1/crawl/seeds
POST /v1/crawl/priorities
GET /v1/crawl/status?host=example.com
```

Removal/safety APIs:

```http
POST /v1/removals
POST /v1/safety/blocklist
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| URL Frontier | prioritizes URLs to crawl |
| Crawler Worker | fetches pages politely |
| Robots/Policy Service | enforces crawl permissions |
| Parser | extracts text, links, metadata |
| Canonicalizer | normalizes URLs |
| Deduplication Service | removes duplicate/near-duplicate pages |
| Link Graph Builder | computes link-based authority signals |
| Indexer | builds inverted index and document metadata |
| Query Frontend | receives user query and handles cache/auth/context |
| Query Understanding | tokenize, spell-correct, rewrite, detect intent |
| Index Serving | retrieves candidate documents |
| Ranker | scores and orders candidates |
| Snippet Service | builds result snippets |
| Safety/Spam Filter | filters unsafe or spam results |

### Crawling

Crawler goals:

- Discover URLs.
- Fetch pages.
- Respect robots rules.
- Avoid overloading websites.
- Prioritize important/fresh pages.

URL frontier priorities:

| Signal | Meaning |
|---|---|
| page authority | important pages crawled more often |
| change frequency | frequently updated pages crawled more often |
| freshness need | news/trending pages get higher priority |
| host politeness | limit per-host request rate |
| sitemap hint | site-provided URL and update metadata |

### Indexing

Inverted index:

```text
term -> list of postings
posting = (docId, termFrequency, positions, fields, qualitySignals)
```

Example:

```text
"cache" -> [(doc7, tf=3, pos=[4, 17, 93]), (doc9, tf=1, pos=[12])]
```

Important fields:

- title
- headings
- body
- anchor text
- URL tokens
- metadata

### Query Serving

Online query path:

1. Normalize query.
2. Tokenize and rewrite query.
3. Check result cache.
4. Fan out query to index shards.
5. Each shard returns top candidates.
6. Merge candidates.
7. Apply ranking model.
8. Apply safety/spam/removal filters.
9. Generate snippets.
10. Return top results.

### Ranking

Ranking signals:

- term relevance (TF-IDF/BM25-like)
- field match: title match is stronger than body match
- link authority/PageRank-like signals
- freshness
- page quality
- spam score
- user location/language
- click/engagement signals
- query intent

Multi-stage ranking:

```text
Stage 1: retrieve thousands from inverted index
Stage 2: lightweight rank top few thousand
Stage 3: heavier rank top few hundred
Stage 4: final filters, snippets, diversity
```

## 2.4 Data Layer

Storage choices:

| Data | Storage |
|---|---|
| raw fetched pages | object storage |
| parsed documents | document store |
| URL frontier | distributed priority queue / KV |
| robots metadata | cache + durable store |
| link graph | graph processing store / distributed files |
| inverted index | shard files + serving replicas |
| document metadata | KV/document store |
| query logs | append-only event log + data lake |
| ranking features | feature store |
| removal/safety lists | strongly consistent config store |

Core entities:

```text
URLRecord(url, normalizedUrl, host, priority, lastFetchedAt, nextFetchAt)
Document(docId, canonicalUrl, title, language, contentHash, qualityScore)
Posting(term, docId, tf, positions, fields)
LinkEdge(sourceDocId, targetDocId, anchorText)
QueryLog(query, userContext, resultIds, clicks, timestamp)
RemovalRule(urlPattern, reason, status)
```

## 2.5 Scalability

Index sharding options:

| Sharding | How it works | Tradeoff |
|---|---|---|
| by document | each shard owns subset of docs | query fans out to many shards |
| by term | each shard owns subset of terms | multi-term queries need term fanout |
| hybrid | partition by docs plus specialized indexes | more complex |

Common serving approach:

- Document-partitioned shards.
- Query sent to many shards.
- Each shard returns top-K local results.
- Aggregator merges global top-K.

Replication:

- Multiple replicas per shard.
- Query router picks healthy/nearby replica.
- Index versions are published atomically.

Crawler scaling:

- Partition URL frontier by host/domain.
- Enforce per-host politeness.
- Use many crawler workers.
- Store raw pages in object storage.

## 2.6 Caching

Cache candidates:

- hot query result pages
- query rewrites/spell corrections
- document metadata
- snippets for hot results
- robots rules
- DNS/host metadata

Cache rules:

- Hot queries should be served from cache.
- Fresh/news queries may need lower TTL.
- Removal/safety updates must invalidate affected cached results.
- Query cache should include locale/safe-search/context dimensions.

## 2.7 Async Systems

Events:

| Event | Consumers |
|---|---|
| URLDiscovered | frontier |
| PageFetched | parser |
| DocumentParsed | dedupe/index/link graph |
| DocumentUpdated | index builder |
| IndexPublished | serving replicas |
| QueryServed | analytics/ranking training |
| ResultClicked | ranking training |
| RemovalRequested | safety/index invalidation |

Reliability patterns:

- Crawling and indexing can be retried.
- Index publish should be versioned.
- Serving should know which index version it uses.
- Query logs feed offline ranking training.

## 2.8 Safety And Failure Handling

| Failure | Mitigation |
|---|---|
| crawler fetch fails | retry with backoff, lower priority |
| website rate limit | respect politeness and slow down |
| parser bug | quarantine document batch |
| bad index build | do not publish failed index version |
| shard timeout | return partial result or query replica |
| ranking service slow | use simpler ranker fallback |
| removal request | invalidate caches and update serving filters |
| spam wave | demote via spam signals and policy filters |

## 2.9 Observability

Crawler metrics:

- fetch success rate
- fetch latency
- per-host request rate
- crawl backlog
- duplicate rate

Index metrics:

- documents indexed/hour
- index build latency
- index size
- publish success/failure
- freshness lag

Serving metrics:

- query QPS
- p50/p95/p99 latency
- shard timeout rate
- result cache hit rate
- zero-result rate
- click-through rate
- unsafe/spam filter count

Logs/traces:

- `queryId`
- `indexVersion`
- `shardIds`
- `candidateCount`
- `rankerVersion`
- `resultIds`

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| fresh index updates | timely results | complex serving/versioning |
| batch index builds | stable and efficient | stale results |
| query-time heavy ranking | high quality | high latency/cost |
| multi-stage ranking | scalable | more complex pipeline |
| aggressive crawling | fresher index | can overload sites |
| conservative crawling | polite and cheaper | stale content |
| cache hot queries | low latency | invalidation complexity |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
URLRecord
Document
ParsedDocument
Posting
InvertedIndexShard
Query
QueryPlan
SearchCandidate
RankedResult
Snippet
CrawlerWorker
IndexBuilder
RankingStrategy
```

## 3.2 OOP Fundamentals

Encapsulation:

- `URLFrontier` owns crawl priority and politeness.
- `InvertedIndexShard` owns term lookup for one shard.
- `RankingService` owns scoring logic.

Polymorphism:

- `RankingStrategy` can be BM25-only, link-aware, or ML-based.
- `Parser` can vary by content type.
- `QueryRewriter` can support synonyms, spell correction, or locale-specific logic.

Composition:

- `SearchService` composes query understanding, shard client, ranker, filter, and snippet builder.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Strategy | ranking algorithms and query rewriting |
| Pipeline | crawl/parse/index stages |
| Adapter | fetching different content protocols |
| Repository | document/index metadata access |
| Builder | query plan and result response |
| Circuit Breaker | shard/ranker failures |

## 3.4 Sequence Diagram

Query serving:

```text
User
  -> QueryFrontend: search(q)
  -> QueryCache: lookup(q, context)
  -> QueryUnderstanding: tokenize/rewrite
  -> IndexRouter: fanout to shard replicas
  -> Shards: local top-K candidates
  -> Aggregator: merge candidates
  -> Ranker: final ranking
  -> Filter: safety/removal/spam
  -> SnippetService: snippets
  -> User: results
```

Crawl/index:

```text
URLFrontier
  -> CrawlerWorker: next URL
  -> Web: fetch page
  -> RawStore: save page
  -> Parser: extract text/links
  -> Deduper: canonical document
  -> Indexer: postings
  -> IndexPublisher: new shard version
```

## 3.5 Edge Cases

- Duplicate URLs with different query params.
- Infinite calendars or generated pages.
- Web pages blocked by robots rules.
- Pages with spam keyword stuffing.
- Query with rare terms and no results.
- Query with ambiguous intent.
- Shard times out.
- Removal request arrives after result is cached.
- Fresh news page needs rapid indexing.
- Language mismatch between query and document.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
models/
  document.py
  posting.py
  query.py
index/
  inverted_index.py
ranking/
  bm25_ranker.py
  pagerank.py
services/
  search_service.py
```

## 4.2 Core Logic Implementation

Toy inverted index with term scoring:

```python
from collections import Counter, defaultdict
from dataclasses import dataclass
import math
import re


def tokenize(text: str) -> list[str]:
    return re.findall(r"[a-z0-9]+", text.lower())


@dataclass(frozen=True)
class Document:
    doc_id: int
    title: str
    body: str
    page_rank: float = 1.0


@dataclass(frozen=True)
class SearchResult:
    doc_id: int
    title: str
    score: float


class MiniSearchIndex:
    def __init__(self) -> None:
        self.documents: dict[int, Document] = {}
        self.index: dict[str, dict[int, int]] = defaultdict(dict)
        self.doc_lengths: dict[int, int] = {}

    def add_document(self, doc: Document) -> None:
        terms = tokenize(doc.title + " " + doc.body)
        counts = Counter(terms)
        self.documents[doc.doc_id] = doc
        self.doc_lengths[doc.doc_id] = len(terms)

        for term, count in counts.items():
            self.index[term][doc.doc_id] = count

    def search(self, query: str, top_k: int = 10) -> list[SearchResult]:
        query_terms = tokenize(query)
        scores: dict[int, float] = defaultdict(float)
        total_docs = max(len(self.documents), 1)

        for term in query_terms:
            postings = self.index.get(term, {})
            if not postings:
                continue

            idf = math.log((total_docs + 1) / (len(postings) + 1)) + 1
            for doc_id, tf in postings.items():
                doc = self.documents[doc_id]
                title_boost = 2.0 if term in tokenize(doc.title) else 1.0
                scores[doc_id] += tf * idf * title_boost * doc.page_rank

        ranked = sorted(scores.items(), key=lambda item: item[1], reverse=True)
        return [
            SearchResult(doc_id=doc_id, title=self.documents[doc_id].title, score=score)
            for doc_id, score in ranked[:top_k]
        ]
```

What this demonstrates:

- Terms map to postings lists.
- Query retrieves candidate documents by term.
- Scoring combines term frequency, inverse document frequency, title boost, and link authority.
- Real systems add BM25, field normalization, learning-to-rank, freshness, spam, and personalization.

## 4.3 Mini PageRank Intuition

```python
def pagerank(graph: dict[str, list[str]], iterations: int = 20, damping: float = 0.85) -> dict[str, float]:
    pages = list(graph.keys())
    n = len(pages)
    ranks = {page: 1.0 / n for page in pages}

    for _ in range(iterations):
        next_ranks = {page: (1.0 - damping) / n for page in pages}
        for page, outgoing in graph.items():
            if not outgoing:
                continue
            share = ranks[page] / len(outgoing)
            for target in outgoing:
                if target in next_ranks:
                    next_ranks[target] += damping * share
        ranks = next_ranks

    return ranks
```

Interview explanation:

- A page is important if important pages link to it.
- Links act like votes, but votes from high-quality pages count more.
- Modern ranking has many more signals, but PageRank is the classic link-graph intuition.

## 4.4 Testing Thinking

Test cases:

- Query term returns matching documents.
- Title match ranks higher than body-only match.
- Rare term has stronger signal than common term.
- Spam/blocked document is filtered.
- Duplicate documents are collapsed.
- Query cache returns same result for same index version.
- Shard timeout triggers fallback behavior.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Breaking news query spike.
- Celebrity/event query spike.
- Bot scraping search results.
- Shard hot spots from common terms.
- Ranking model latency spike.
- Index publish causes replica pressure.

## 5.2 Immediate Response

- Cache hot query results.
- Rate limit abusive clients.
- Serve simpler ranking for overloaded path.
- Use shard replicas and hedged requests.
- Limit deep pagination.
- Protect snippet/rich-result generation.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| ranking slow | use lightweight ranker |
| snippet service slow | return title/URL with simple snippet |
| shard timeout | merge partial results if acceptable |
| freshness pipeline delayed | serve last stable index |
| cache invalidation lag | apply removal filter at serving time |

## 5.4 Spike Interview Answer

> During a hot query spike, I would serve cached results for the query and context, use replicas for shard fanout, and degrade expensive ranking or snippet features first. I would not bypass safety/removal filters. For breaking news, I would rely on a freshness index that can be merged with the main index.

---

# 6. Global Scale

## 6.1 Regional Serving

```text
Global Traffic Manager
  -> nearest Search Frontend
  -> regional query cache
  -> regional index replicas
  -> regional ranking/snippet services
```

Principles:

- Serve queries close to users.
- Replicate index shards by region.
- Keep removal/safety controls globally enforced.
- Use locale/language-specific ranking.
- Keep query logs region-aware for privacy and compliance.

## 6.2 Freshness Architecture

Common pattern:

```text
Main index: large, stable, batch-published
Fresh index: small, frequently updated
Query serving: search both, merge results
```

Why:

- Rebuilding the whole index continuously is expensive.
- Fresh content needs fast visibility.
- A small fresh index can be updated every few minutes.

## 6.3 Interview Answer

> At global scale, I would replicate index serving across regions and route users to nearby replicas. The crawler and index pipeline continuously publish versioned indexes. For freshness, I would maintain a small near-real-time index merged with the main index. Query serving fans out to shards, merges local top-K results, runs multi-stage ranking, and applies safety/removal filters before returning.

---

# 7. Final Interview Playbook

Start with:

> Web search has two planes: an offline/nearline crawl-index pipeline and an online query-serving path. The online path retrieves candidates from an inverted index and ranks only a manageable candidate set.

Then cover:

1. Requirements and scale.
2. Crawler and URL frontier.
3. Parsing, deduplication, and link graph.
4. Inverted index.
5. Query serving and shard fanout.
6. Multi-stage ranking.
7. Freshness index.
8. Caching, safety, failures, and global scale.

Common traps:

- Scanning all documents at query time.
- Ignoring crawler politeness.
- Forgetting duplicate/spam pages.
- Forgetting index versioning.
- Treating ranking as only keyword matching.
- Ignoring removal/safety cache invalidation.

---

# 8. Fast Recall Rules

- Search is crawl -> parse -> index -> rank -> serve.
- Inverted index maps terms to postings.
- Query serving retrieves candidates, not the whole web.
- Ranking is multi-stage.
- PageRank is link authority intuition, not the whole ranker.
- Fresh content usually uses a fresh index plus main index.
- Hot queries need caching.
- Shards need replicas and timeouts.
- Removal and safety filters must survive cache/index delays.
- Query logs improve ranking over time.

