# Search Autocomplete - End-to-End System Design

> Goal: design a low-latency autocomplete system that returns useful top suggestions as users type prefixes, using prefix indexes, ranking, query logs, freshness pipelines, filtering, caching, and personalization where needed.

---

## How To Use This File

- Use this when the interview problem says autocomplete, typeahead, search suggestions, prefix search, query suggestions, or instant search box.
- Start with a simple Trie and top-K suggestions, then evolve to distributed indexes, ranking, personalization, caching, and freshness.
- Keep the distinction clear: autocomplete suggests queries/entities before full search results are fetched.
- In interviews, explain why precomputing top suggestions per prefix is often better than ranking every candidate at request time.

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

| Layer | Interview signal | Autocomplete focus |
|---|---|---|
| Problem understanding | Can define suggestion type | query suggestions, product names, users, places, hashtags |
| HLD | Can separate read and index pipelines | query API, prefix index, ranking, logs, index builder, cache |
| LLD | Can model Trie/index cleanly | `TrieNode`, `Suggestion`, `PrefixIndex`, `RankingStrategy`, `IndexBuilder` |
| Machine coding | Can implement top-K prefix lookup | insert terms, maintain suggestions, search prefix |
| Traffic spikes | Can protect low-latency reads | cache hot prefixes, degrade personalization, precompute top prefixes |
| Billion users | Can scale globally | sharding, replication, regional indexes, freshness trade-offs |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Return autocomplete suggestions for a typed prefix.
- Rank suggestions by popularity, relevance, freshness, and optional personalization.
- Support top-K results, such as top 5 or top 10 suggestions.
- Support fast response as user types each character.
- Update suggestions based on query logs, clicks, trending terms, and catalog changes.
- Filter unsafe, blocked, deleted, or low-quality suggestions.
- Support multiple languages/locales if required.
- Support typo tolerance if required.
- Emit metrics for impressions, clicks, latency, and zero-result prefixes.

Suggestion types to clarify:

- query suggestions: `iphone 16 case`, `java interview questions`,
- entity suggestions: users, products, restaurants, places,
- mixed suggestions: query + entity + category,
- command suggestions: IDE/search tool commands.

Optional requirements to clarify:

- Is matching only prefix-based or also fuzzy?
- Should suggestions be personalized by user/location/history?
- Should suggestions include trending queries in real time?
- Should there be profanity, policy, or safety filtering?
- Is the corpus small enough for one machine or massive/global?
- Do we need instant updates when an item is created/deleted?

Out of scope unless asked:

- Full search ranking and document retrieval.
- Full ad auction system.
- Full recommendation system.
- Full typo-correction ML pipeline.
- Full natural-language query understanding.

## 1.2 Non-Functional Requirements

Latency:

- Very low read latency, often p99 under 50 ms end-to-end.
- User sees suggestions after every keystroke, so tail latency matters.
- Hot prefixes should be served from memory/cache.

Availability:

- Autocomplete should degrade gracefully.
- It is usually better to return popular generic suggestions than timeout.
- Index serving should continue if indexing pipeline is delayed.

Quality:

- Suggestions should be relevant and safe.
- Ranking should avoid spam and offensive terms.
- Deleted/private items should not appear.
- Fresh/trending suggestions should appear within the required freshness SLA.

## 1.3 Constraints

- Prefix queries are extremely frequent for short prefixes like `a`, `i`, `s`.
- Short prefixes have huge candidate sets.
- Every keystroke can generate a request unless clients debounce.
- Ranking all candidates at request time is too expensive.
- Personalization increases cache fragmentation.
- Global consistency is less important than latency for most suggestions.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Daily active users | 500M |
| Peak autocomplete QPS | 2M requests/sec |
| Suggestions per request | top 10 |
| Unique suggestion terms | 1B |
| Hot prefixes | top 1M prefixes receive most traffic |
| Latency target | p99 under 50 ms |
| Freshness target | minutes for trending queries, seconds for critical entity delete |
| Index replicas | many per region |

Back-of-the-envelope:

- If every user types 10 characters and the client sends every keystroke, autocomplete traffic can exceed search-submit traffic by an order of magnitude.
- Precomputing top suggestions for popular prefixes reduces request-time CPU.
- Cache hit rate is high for short popular prefixes but lower for personalized or long-tail prefixes.

## 1.5 Clarifying Questions To Ask

- Are suggestions based on historical queries, searchable entities, or both?
- What is the minimum prefix length?
- Should the system support typo tolerance?
- How fresh must suggestions be?
- Are suggestions personalized?
- What languages/locales are supported?
- What safety/moderation rules apply?
- Should deleted/private items disappear immediately?

Strong interview framing:

> I will design autocomplete as a read-optimized prefix lookup system. Offline and streaming pipelines build a prefix index with precomputed top suggestions, the query API serves from memory/cache with ranking and filtering, and updates flow through an index builder so reads remain fast while quality improves over time.

---

# 2. High-Level Design

## 2.1 Architecture

Read path:

```text
Client Search Box
  -> debounce and prefix normalization
  -> Autocomplete API
  -> Hot Prefix Cache
  -> Prefix Index Service
  -> Ranking / Filtering / Personalization
  -> top-K suggestions
```

Write/indexing path:

```text
Search logs / Click logs / Catalog updates / Moderation updates
  -> Event Stream
  -> Aggregation and Quality Scoring
  -> Index Builder
  -> Prefix Index Store
  -> Index Serving Replicas
```

Recommended architecture:

```text
                    Read Path

Client -> API Gateway -> Autocomplete Service -> Hot Prefix Cache
                                      |                 |
                                      | miss            v
                                      v          +-------------+
                              Prefix Index      | Suggestion   |
                              Serving Fleet     | Cache        |
                                      |          +-------------+
                                      v
                              Ranking/Filtering
                                      |
                                      v
                                  Client

                    Indexing Path

Query Logs -> Stream -> Aggregator -> Ranking Features -> Index Builder
Catalog Updates -----> Stream ------> Moderation Filter -> Index Publisher
                                                        -> Serving Replicas
```

## 2.2 APIs

### Get Suggestions

```http
GET /v1/autocomplete?prefix=iph&limit=10&locale=en-US&userId=u123
```

Response:

```json
{
  "prefix": "iph",
  "suggestions": [
    {"text": "iphone 16", "type": "query", "score": 0.98},
    {"text": "iphone 16 case", "type": "query", "score": 0.94},
    {"text": "iphone charger", "type": "query", "score": 0.91}
  ],
  "source": "prefix-index",
  "indexVersion": "idx_20260617_1200"
}
```

### Track Impression/Click

```http
POST /v1/autocomplete/events
Content-Type: application/json

{
  "requestId": "req_123",
  "userId": "u123",
  "prefix": "iph",
  "shown": ["iphone 16", "iphone 16 case"],
  "clicked": "iphone 16 case",
  "timestampMs": 1781690400000
}
```

### Update Entity Suggestion

```http
POST /internal/autocomplete/entities/upsert
Content-Type: application/json

{
  "entityId": "product_123",
  "text": "iphone 16 pro case",
  "type": "product",
  "locale": "en-US",
  "qualityScore": 0.87,
  "visibility": "PUBLIC"
}
```

## 2.3 Core Components

Think of Search Autocomplete as two systems:

| System | Main job | Optimized for |
|---|---|---|
| serving system | answer prefix requests | latency and availability |
| indexing system | build/update prefix suggestions | quality and freshness |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| Client Search Box | debounce, cancellation, minimum prefix | ranking truth | keystroke volume |
| API Gateway | auth, coarse rate limits | suggestion ranking | request QPS |
| Autocomplete Service | normalize prefix, fetch candidates, apply ranking/filtering | full search retrieval | p99 latency |
| Hot Prefix Cache | cached top suggestions | source of truth | hot prefixes |
| Prefix Index Service | prefix to candidate lookup | query-log aggregation | memory and shard count |
| Suggestion Store | canonical suggestion metadata | per-request ranking alone | corpus size |
| Ranking Service | score and order candidates | unsafe content policy alone | feature lookup latency |
| Filtering/Moderation | remove blocked/deleted/private suggestions | ranking popularity | safety and correctness |
| Query Log Pipeline | impressions/clicks/searches | serving direct requests | event volume |
| Index Builder | build trie/FST/top-K lists | request path serving | freshness and build cost |
| Index Publisher | distribute index versions | candidate scoring | rollout safety |

### Prefix Index Options

| Index | How it works | Pros | Cons | Best for |
|---|---|---|---|---|
| Trie | character tree with prefix nodes | easy to explain and implement | memory-heavy at massive scale | machine coding, moderate corpus |
| Compressed Trie/Radix Tree | compress common chains | lower memory | more complex | large prefix sets |
| FST | finite-state transducer | very compact and fast | harder to build/explain | production search systems |
| Prefix hash map | map prefix to top suggestions | very fast reads | expensive storage for all prefixes | hot prefix cache/precompute |
| Search engine edge n-grams | index generated prefixes | integrates with search infra | tuning complexity | existing Elasticsearch/OpenSearch stack |

Interview default:

- Start with Trie for clarity.
- Store top-K suggestions at each node.
- For production, mention compressed trie/FST or prefix-to-topK maps for hot prefixes.

### Ranking Signals

| Signal | Meaning |
|---|---|
| query frequency | how often users search this term |
| click-through rate | how often suggestion is clicked |
| conversion/action rate | downstream success |
| recency/trending | recent spike in interest |
| locale/location | regional relevance |
| personalization | user history and preferences |
| quality/safety score | spam/offensive filtering |
| business score | optional promoted categories or products |

Simple scoring idea:

```text
score = popularity_weight * log(query_count)
      + click_weight * ctr
      + freshness_weight * trend_score
      + personalization_weight * user_affinity
      - penalty_weight * spam_or_safety_penalty
```

### Precompute vs Rank At Request Time

| Approach | Pros | Cons |
|---|---|---|
| precompute top-K per prefix | very fast reads | more index storage and rebuild cost |
| rank all candidates at request time | flexible | too slow for short prefixes |
| hybrid | fast base list plus light reranking | common practical choice |

Recommended:

- Precompute top suggestions per prefix for popular/global ranking.
- Apply lightweight filtering and personalization at request time.
- Use cache for hot prefixes and local in-memory index for serving.

### Freshness Model

Freshness needs vary:

| Update type | Freshness expectation | Handling |
|---|---|---|
| deleted/private item | seconds or immediate | tombstone/filter overlay |
| trending query | seconds to minutes | streaming delta index |
| daily popularity shift | hours | batch rebuild |
| new product/entity | minutes | incremental index update |
| typo model update | hours/days | offline rebuild |

One-stop interview answer:

> I would serve autocomplete from an in-memory prefix index with precomputed top suggestions per prefix. Query logs and catalog updates flow through batch and streaming pipelines into an index builder. The read path normalizes the prefix, checks hot-prefix cache, retrieves candidates from the prefix index, applies safety filtering and lightweight reranking, then returns top-K suggestions within a tight latency budget.

---

# 3. Low-Level Design

LLD goal:

> Model autocomplete around a prefix index and suggestion ranking, keeping insertion, lookup, and top-K maintenance clear.

Simple rule:

- `Suggestion` is the thing shown.
- `TrieNode` represents a prefix.
- `PrefixIndex` supports insert and search.
- `RankingStrategy` orders candidates.
- `AutocompleteService` handles normalization, filtering, and response.

Starter map:

| LLD question | Autocomplete answer |
|---|---|
| Main data structure | `TrieNode` or compressed trie |
| Suggestion object | `Suggestion` |
| Lookup service | `PrefixIndex` |
| Ranking abstraction | `RankingStrategy` |
| Serving orchestrator | `AutocompleteService` |
| Build component | `IndexBuilder` |
| Filter component | `SuggestionFilter` |
| Cache component | `SuggestionCache` |

Beginner-friendly design order:

1. Model `Suggestion` with text, score, type, locale, and visibility.
2. Build a Trie where each node has children and top suggestions.
3. Insert suggestion text into the Trie.
4. Maintain top-K suggestions at every visited node.
5. Search by prefix and return node top-K.
6. Add normalization for case, spaces, punctuation, and locale.
7. Add filtering for deleted/private/blocked suggestions.
8. Add cache for hot prefixes.

Interview sentence:

> In LLD, I will use a Trie-style prefix index where each node stores the best top-K suggestions for that prefix, so lookup is O(length of prefix) plus small top-K processing instead of scanning all terms.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `Suggestion` | text shown to user plus metadata | text must be normalized consistently |
| `TrieNode` | children and top suggestions for prefix | top list sorted by score |
| `PrefixIndex` | insert and lookup operations | lookup cost depends on prefix length |
| `RankingStrategy` | score and compare suggestions | deterministic for same features/version |
| `SuggestionFilter` | remove unsafe/unavailable results | safety filters apply before response |
| `IndexBuilder` | build index from logs/catalog | publish immutable index version |
| `AutocompleteService` | read path orchestration | returns fast fallback if index unavailable |
| `SuggestionCache` | cache hot prefix responses | TTL/version-aware |

Core services:

| Service | Responsibility |
|---|---|
| `Normalizer` | lowercase, trim, locale handling |
| `PrefixIndexService` | serve candidate suggestions |
| `RankingService` | rerank candidates |
| `ModerationService` | provide blocked/tombstone terms |
| `QueryLogConsumer` | consume impressions/clicks/search logs |
| `IndexPublisher` | publish new index snapshots |

## 3.2 Class Sketch

```java
final class Suggestion {
    private final String text;
    private final String type;
    private final double score;
    private final String locale;
}

final class TrieNode {
    private final Map<Character, TrieNode> children = new HashMap<>();
    private final PriorityQueue<Suggestion> topSuggestions;
}

interface PrefixIndex {
    void insert(Suggestion suggestion);
    List<Suggestion> search(String prefix, int limit);
}
```

## 3.3 Sequence Diagram

```text
Client -> AutocompleteAPI: prefix=iph
AutocompleteAPI -> Normalizer: normalize prefix
AutocompleteAPI -> SuggestionCache: get(iph,en-US)
SuggestionCache --> AutocompleteAPI: miss
AutocompleteAPI -> PrefixIndexService: lookup prefix
PrefixIndexService --> AutocompleteAPI: candidates
AutocompleteAPI -> SuggestionFilter: remove blocked/deleted/private
AutocompleteAPI -> RankingService: lightweight rerank
AutocompleteAPI -> SuggestionCache: cache response
AutocompleteAPI --> Client: top-K suggestions
```

Indexing sequence:

```text
Search/Click Logs -> Stream -> Aggregator -> Feature Builder
Catalog Updates -> Stream -> Entity Processor -> Feature Builder
Feature Builder -> IndexBuilder: scored suggestions
IndexBuilder -> IndexPublisher: immutable index version
IndexPublisher -> Serving Replicas: load new index
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | ranking algorithms, matching modes |
| Adapter | index backends: trie, FST, search engine |
| Snapshot | immutable index versions in serving fleet |
| Decorator | metrics/tracing around lookup |
| Factory | choose index by locale/type |
| Cache-Aside | hot prefix cache |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| empty prefix | return trending suggestions or nothing based on product choice |
| one-character prefix | serve from cache/precomputed list only |
| deleted entity still in index | tombstone filter overlay blocks it immediately |
| offensive query becomes popular | moderation filter removes it before index publish and response |
| user types quickly | client debounce and cancel older requests |
| no prefix match | fallback to trending or return empty list |
| multilingual text | locale-aware normalization and separate indexes |
| typo in prefix | optional fuzzy lookup or spell-correction layer |
| personalized ranking unavailable | return generic global ranking |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
autocomplete/
  domain/
    Suggestion.java
    AutocompleteRequest.java
    AutocompleteResponse.java
  index/
    TrieNode.java
    PrefixIndex.java
    TriePrefixIndex.java
  ranking/
    RankingStrategy.java
    PopularityRankingStrategy.java
  service/
    AutocompleteService.java
    Normalizer.java
    SuggestionFilter.java
  cache/
    SuggestionCache.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Suggestion:
    text: str
    score: float


class TrieNode:
    def __init__(self) -> None:
        self.children: dict[str, TrieNode] = {}
        self.top: list[Suggestion] = []


class TrieAutocomplete:
    def __init__(self, top_size: int = 3) -> None:
        self.root = TrieNode()
        self.top_size = top_size

    def insert(self, suggestion: Suggestion) -> None:
        node = self.root
        normalized = self._normalize(suggestion.text)
        for char in normalized:
            node = node.children.setdefault(char, TrieNode())
            self._add_to_top(node, suggestion)

    def search(self, prefix: str) -> list[Suggestion]:
        node = self.root
        for char in self._normalize(prefix):
            if char not in node.children:
                return []
            node = node.children[char]
        return node.top

    def _add_to_top(self, node: TrieNode, suggestion: Suggestion) -> None:
        by_text = {item.text: item for item in node.top}
        by_text[suggestion.text] = suggestion
        node.top = sorted(by_text.values(), key=lambda item: (-item.score, item.text))[:self.top_size]

    def _normalize(self, value: str) -> str:
        return " ".join(value.lower().strip().split())


autocomplete = TrieAutocomplete(top_size=3)
autocomplete.insert(Suggestion("iphone 16", 100))
autocomplete.insert(Suggestion("iphone 16 case", 95))
autocomplete.insert(Suggestion("iphone charger", 90))
autocomplete.insert(Suggestion("ipad", 80))

print([item.text for item in autocomplete.search("iph")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| prefix lookup | Trie, compressed trie, FST |
| top suggestions | bounded sorted list or min-heap |
| hot prefix cache | LRU/TTL cache |
| tombstones | hash set or bloom filter plus source of truth |
| locale indexes | map from locale to index snapshot |
| query logs | append-only event stream |
| popularity counts | key-value counters or OLAP aggregates |

## 4.4 Concurrency

- Serving indexes should be immutable snapshots.
- Load new index versions with atomic pointer swap.
- Avoid mutating Trie nodes while serving requests.
- Use separate builder process for index creation.
- Cache updates should be thread-safe.
- Event ingestion should be idempotent where possible.

## 4.5 Performance Optimization

- Precompute top-K suggestions per prefix.
- Cache hot prefixes in memory and CDN/edge if public-safe.
- Debounce client requests.
- Limit minimum prefix length.
- Use compressed trie/FST for memory efficiency.
- Keep personalization lightweight.
- Use tombstone overlays to avoid full index rebuild for deletes.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Keystroke QPS Spike

Problem:

- Autocomplete QPS explodes because every keystroke creates a request.

Handling:

- Client debounce, such as 100 to 300 ms.
- Cancel stale in-flight requests.
- Enforce minimum prefix length.
- Cache short hot prefixes aggressively.
- Return generic suggestions if personalization service is slow.
- Apply rate limits by user/IP/session.

## 5.2 Hot Prefix Spike

Problem:

- A prefix like `i`, `a`, or a trending event becomes extremely hot.

Handling:

- Serve from hot-prefix cache.
- Precompute short-prefix suggestions.
- Replicate hot prefix entries across shards.
- Avoid fanout to many shards for short prefixes.
- Apply request coalescing for cache misses.

## 5.3 Bad Or Unsafe Suggestion Trend

Handling:

- Moderation filter in both indexing and serving path.
- Real-time blocklist/tombstone overlay.
- Alert on sudden score spikes for sensitive terms.
- Human review for high-impact suggestions if product requires it.

## 5.4 Indexing Pipeline Delay

Handling:

- Continue serving last good index.
- Show index freshness in metrics.
- Use streaming delta index for urgent updates.
- Prioritize delete/tombstone events over ranking updates.
- Fall back to cached generic suggestions.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| prefix index | locale + first few characters | balances read traffic |
| hot prefix cache | prefix + locale | replicate very hot prefixes |
| suggestion metadata | suggestion ID/text hash | source details |
| query logs | region + time | event ingestion scale |
| popularity aggregates | suggestion/prefix + window | ranking features |
| tombstones | suggestion ID/text | fast serving overlay |

## 6.2 Sharding Strategies

Options:

| Strategy | Pros | Cons |
|---|---|---|
| shard by first character | simple | skew for popular letters |
| shard by first two/three chars | better distribution | short-prefix fanout issue |
| shard by hash of prefix | balanced | prefix traversal harder |
| replicate full hot index | fastest reads | memory cost |
| split by locale/type | improves relevance | more indexes to operate |

Recommended:

- Keep hot short prefixes replicated.
- Shard long-tail prefixes by prefix range or hash.
- Maintain separate indexes by locale/type where quality requires.
- Use immutable index versions and atomic rollout.

## 6.3 Multi-Region Strategy

- Serve reads regionally from local replicas.
- Build global index offline and publish to all regions.
- Use regional trending overlays for local freshness.
- Keep safety/tombstone updates globally fast.
- Allow ranking differences by locale/region.

## 6.4 Observability

Track:

- prefix lookup latency,
- cache hit rate,
- top prefixes by QPS,
- zero-result prefix rate,
- click-through rate,
- suggestion acceptance rate,
- index version and freshness,
- moderation/tombstone filter count,
- personalization fallback count,
- shard QPS and memory,
- client debounce effectiveness.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify suggestion source and matching rules.
2. Start with Trie and top-K suggestions at each node.
3. Split read path and indexing path.
4. Add ranking from query logs, clicks, recency, and safety.
5. Add cache for hot prefixes.
6. Add immutable index snapshots and publisher.
7. Add freshness overlays for trends and deletes.
8. Scale with sharding, replication, regional indexes, and observability.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| index | Trie | FST/compressed trie | Trie for explanation, FST/compressed for production |
| ranking | precompute | request-time rank | precompute base top-K, light rerank online |
| freshness | batch only | streaming updates | hybrid batch plus streaming delta |
| personalization | fully personalized | generic | generic base plus lightweight personalization |
| consistency | immediate global | eventual | eventual except deletes/safety tombstones |

## 7.3 Common Mistakes

- Scanning the full corpus for every prefix.
- Ranking all candidates at request time for short prefixes.
- Ignoring hot prefixes.
- Forgetting client debounce.
- Returning deleted/private/unsafe suggestions.
- Over-personalizing and destroying cache hit rate.
- Not separating indexing from serving.

## 7.4 Strong Closing

> Autocomplete should be a read-optimized prefix-serving system. I would precompute top-K suggestions per prefix in an immutable index, serve hot prefixes from cache, apply safety filtering and lightweight reranking online, and update the index through batch plus streaming pipelines so reads stay fast while ranking remains fresh.

---

# 8. Fast Recall Rules

- Autocomplete = prefix lookup plus ranking.
- Trie is the beginner-friendly data structure.
- Store top-K at each prefix node for fast lookup.
- Production often uses compressed trie, FST, or prefix maps.
- Short prefixes are hot and expensive.
- Precompute base rankings; rerank lightly online.
- Separate serving path from indexing path.
- Use query logs, clicks, recency, and safety signals.
- Deletes and unsafe terms need fast tombstone/filter overlays.
- Cache hot prefixes and debounce client requests.
