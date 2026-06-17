# News Feed - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For News Feed specifically, design for read-heavy fanout, freshness, and ranking quality under extreme scale.

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

| Layer | Interview signal | News feed focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | post creation, follow graph, feed retrieval, ranking, pagination |
| HLD | Can design scalable systems | write service, fanout pipeline, feed store, cache, ranking service |
| LLD | Can model maintainable components | `Post`, `FeedItem`, `FollowEdge`, `FanoutWorker`, `RankingStrategy` |
| Machine coding | Can implement critical path | create post, fanout distribution, fetch feed, cursor pagination |
| Traffic spikes | Can protect production | celebrity posts, read storms, async backpressure, degraded ranking |
| Billion users | Can reason at global scale | hybrid fanout, graph partitioning, multi-tier cache, region-aware feed serving |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- User can create posts.
- User can follow and unfollow other users.
- User can fetch a personalized home feed.
- Feed should show recent and relevant posts from followed accounts.
- Support pagination for continuous scrolling.
- Support basic actions: like, comment count preview, repost/share marker.

Optional requirements to clarify:

- Is feed purely chronological or ranked?
- Should ads/promoted posts be included?
- How fresh must feed updates be?
- Should private accounts be supported?
- Is offline feed prefetch needed?
- Should blocked/muted users be filtered?

Out of scope unless interviewer asks:

- Full recommendation graph for "people you may know".
- Full ML model training pipeline.
- Content moderation review tooling UX.

## 1.2 Non-Functional Requirements

Feed read path:

- Very low latency for feed fetch.
- Very high throughput for scroll requests.
- High availability because feed is a core product surface.
- Freshness target defined by product, such as near real-time to a few seconds.

Write/fanout path:

- Durable post write.
- Reliable fanout to followers with backpressure handling.
- Eventual consistency acceptable for delivery order in many cases.

Ranking/quality path:

- Ranking should be stable and explainable enough for production debugging.
- Degraded mode should serve chronological feed if ranking stack is unhealthy.

## 1.3 Constraints

- Users can have highly skewed follower counts, including celebrity accounts.
- Read/write ratio is heavily read-dominant.
- A single hot post can create global read spikes.
- Feed storage can grow very quickly if naive fanout is used.
- Ranking must balance quality with latency budget.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Users | 1 billion registered users |
| Daily active users | 300 million |
| Posts/day | 500 million |
| Feed reads/day | 200 billion feed item fetches |
| Avg follows/user | 300 |
| Celebrity accounts | up to 100M followers |
| Read/write ratio | often greater than 100:1 |
| Feed read latency target | p99 under 200 ms end-to-end |

Back-of-the-envelope:

- `500M posts/day` is about `5,787 posts/sec` average global write rate, with large peaks.
- `200B feed item fetches/day` can translate to multi-million QPS for home feed endpoints.
- If each feed item metadata is about `300 bytes`, storing only recent windows still requires very large storage.
- Full fanout-on-write for celebrity posts can create enormous queue and storage amplification.

## 1.5 Clarifying Questions To Ask

- Timeline mode: strict chronological, ranked, or hybrid?
- Freshness SLA: seconds, tens of seconds, or minutes?
- Do we require read-your-write for author's own post in feed?
- Retention window: how far back does home feed fetch by default?
- Can we show partial feed in degraded mode?
- Is cross-region consistency strict or eventual?

Strong interview framing:

> I will design a hybrid feed system: durable post writes, asynchronous fanout for normal users, special handling for celebrity accounts, and fast read path through feed caches and precomputed candidate windows.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Post create flow:
Client
  -> API Gateway
  -> Post Service
  -> Post Store
  -> Event Stream (post-created)
  -> Fanout Workers
  -> Feed Store / Inbox Store
  -> Cache invalidation

Feed read flow:
Client
  -> API Gateway
  -> Feed Query Service
  -> Feed Cache / Feed Store
  -> Ranking Service
  -> Hydration Service (author/post metadata)
  -> Response with cursor
```

Recommended architecture:

```text
                         +----------------------+
                         |  Recommendation / ML |
                         +----------+-----------+
                                    |
Client                              v
  |                        +----------------------+
  v                        | Ranking Feature Store |
+------------+             +-----------+----------+
| API Gateway|                         |
+-----+------+                         v
      |                    +-----------------------+
      +------------------->| Feed Query Service    |
      |                    +-----------+-----------+
      |                                |
      v                                v
+------------+               +---------------------+
| Post Service|              | Feed Cache (Redis)  |
+-----+------+               +----------+----------+
      |                                 |
      v                                 v
+------------------+          +---------------------+
| Post Store       |          | Feed Store / Inbox  |
+--------+---------+          +----------+----------+
         |                               ^
         v                               |
+------------------+          +----------+----------+
| Event Stream     |--------->| Fanout Workers      |
| (Kafka/Pulsar)   |          +---------------------+
+------------------+
```

Request flow for feed fetch:

1. User calls `GET /v1/feed/home?cursor=...`.
2. Feed service checks user feed cache.
3. On hit, fetches candidate IDs quickly.
4. On miss, reads feed inbox store or runs on-read merge path.
5. Ranking service scores top candidates with latency budget.
6. Hydration service fetches post/author preview metadata.
7. Service returns feed items and next cursor.

## 2.2 APIs

### Create Post

```http
POST /v1/posts
Authorization: Bearer <token>
Idempotency-Key: 6c3d...
Content-Type: application/json

{
  "text": "Launching a new feature today!",
  "media": ["media-123"],
  "visibility": "PUBLIC"
}
```

Response:

```json
{
  "postId": "p_87322",
  "authorId": "u_42",
  "createdAt": "2026-06-17T10:30:00Z",
  "status": "PUBLISHED"
}
```

### Follow User

```http
POST /v1/follows
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetUserId": "u_84"
}
```

### Get Home Feed

```http
GET /v1/feed/home?cursor=eyJ0cyI6IjIwMjYtMDYtMTdUMTA6MDA6MDBaIiwiaWQiOiJwXzk5In0=&limit=20
Authorization: Bearer <token>
```

Response:

```json
{
  "items": [
    {"postId": "p_87322", "authorId": "u_84", "score": 0.92, "createdAt": "2026-06-17T10:30:00Z"}
  ],
  "nextCursor": "eyJ0cyI6IjIwMjYtMDYtMTdUMDk6NTk6MDBaIiwiaWQiOiJwXzgxIn0="
}
```

### Get User Timeline

```http
GET /v1/users/{userId}/posts?cursor=...&limit=20
```

### Delete Post

```http
DELETE /v1/posts/{postId}
Authorization: Bearer <token>
```

## 2.3 Core Components

Think of News Feed as two cooperating systems:

| Plane | What it handles | Main goal |
|---|---|---|
| Write/fanout plane | post creation, follow graph lookup, inbox materialization | prepare feed candidates before users scroll |
| Read/ranking plane | fetch candidates, rank, hydrate, filter, paginate | return a fast and relevant feed page |
| Background plane | engagement signals, cache invalidation, analytics, moderation | improve quality without blocking reads/writes |

The key interview idea is hybrid fanout. Normal authors can push post IDs into follower inboxes at write time. Celebrity authors should not synchronously fan out to millions of users; their posts are merged at read time or handled by special fanout lanes.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Gateway | auth, request shaping, anti-scraping limits | feed ranking logic | API QPS |
| Post Service | canonical post creation/deletion/edit events | follower fanout loops | post write QPS |
| Follow Graph Service | follower/following edges and batch reads | post content storage | edge count and graph reads |
| Fanout Service / Workers | post-ID distribution into inboxes | ranking final feed page | event volume and follower count |
| Feed Query Service | fetch candidates, filter, paginate response | training ranking model | feed read QPS |
| Ranking Service | score and order candidates | store canonical posts | candidate count and feature reads |
| Hydration Service | batch fetch post/author/social preview fields | decide candidate source | batch read QPS |
| Feed Cache | hot feed pages and candidate windows | canonical post truth | cache hit rate |
| Event Stream | durable post/follow/engagement events | business decisions | event throughput and replay |
| Moderation/Visibility Filter | delete/block/private/mute checks | feed storage mechanics | policy check QPS |

### Gateway / Edge Layer

Why it exists:

- Feed endpoints receive huge scroll traffic.
- Clients can aggressively refresh or scrape feeds.
- Feed reads and writes need authentication, rate limits, and request shaping.

Core responsibilities:

- Authenticate users and attach identity context.
- Rate-limit abusive feed reads, post writes, and follow churn.
- Route to nearest healthy region.
- Enforce max page size and request validation.
- Protect the backend from bots and scraping.

Failure behavior:

- If read traffic spikes, gateway can reduce max page size or apply stricter rate limits.
- If a user/device is abusive, throttle that caller without affecting global feed service.

Interview signal:

> Gateway protects the feed surface from abusive read amplification and keeps downstream services focused on feed logic.

### Post Service

Why it exists:

- Posts are canonical content objects.
- Fanout, ranking, search, and notifications should react to post events, not own post creation themselves.

Core responsibilities:

- Validate and create posts.
- Store canonical post content and metadata.
- Enforce author permissions and visibility.
- Emit `post.created`, `post.deleted`, and `post.updated` events.
- Handle soft-delete/tombstone state transitions.

What it should avoid:

- Do not synchronously write the post into every follower feed.
- Do not embed ranking logic into post creation.
- Do not duplicate full post body into every feed inbox item.

Failure behavior:

- If fanout pipeline is down, post creation can still persist and fanout later from durable event log.
- If post store write fails, no fanout event should be emitted.
- If delete occurs after fanout, read-time filtering must still hide the post.

Interview signal:

> Post Service owns canonical content. Everything else consumes post events.

### Follow Graph Service

Why it exists:

- Fanout needs to know who follows an author.
- Feed reads sometimes need to merge posts from followed authors.
- Follow/unfollow state affects what feed candidates are valid.

Core responsibilities:

- Store forward edge: `follower -> followed`.
- Store reverse edge: `followed -> followers` for fanout batches.
- Support follow/unfollow with idempotency.
- Support block/mute/private-account visibility metadata.
- Provide paginated follower batches for fanout workers.

Scaling notes:

- Partition graph edges by user ID.
- Celebrity reverse edges can be extremely large; batch them and process through worker shards.
- Cache hot follow lists carefully because follow/unfollow changes affect correctness.

Failure behavior:

- If graph service is slow, fanout can lag because events are durable.
- If follow state changes after fanout, Feed Query Service must re-check visibility/membership during read.

Interview signal:

> Follow Graph Service is not the feed itself; it is the relationship source used by fanout and read-time candidate generation.

### Fanout Service / Workers

Why it exists:

- Precomputing feed candidates makes read path fast.
- Feed reads are usually far more frequent than post writes.

Core responsibilities:

- Consume `post.created` events.
- Decide fanout strategy by author follower count and product policy.
- Push post IDs into follower inboxes for normal authors.
- Skip or defer celebrity fanout into pull/merge strategy.
- Retry failed inbox writes idempotently.
- Send poison events to DLQ.

Fanout strategy:

| Author type | Recommended strategy | Why |
|---|---|---|
| normal user | fanout on write | fast feed reads, manageable follower count |
| medium creator | async fanout with batching | controls queue pressure |
| celebrity/channel | fanout on read or hybrid | avoids enormous write amplification |

Failure behavior:

- Duplicate event: inbox write is idempotent by `(userId, postId)`.
- Worker lag: feed may be slightly stale but still available.
- Celebrity spike: route to special queue or read-time merge path.

Interview signal:

> Fanout workers trade write cost for read speed. Hybrid fanout prevents celebrity accounts from destroying the write path.

### Feed Query Service

Why it exists:

- It owns the user-facing feed read API.
- It combines candidate retrieval, filtering, ranking, hydration, and pagination.

Core responsibilities:

- Fetch feed candidates from inbox/cache or read-time merge source.
- Filter deleted, private, blocked, muted, or unavailable posts.
- Call Ranking Service within latency budget.
- Batch hydrate post and author metadata.
- Return stable cursor-based pagination.
- Degrade gracefully when ranking/hydration dependencies are slow.

Read flow:

```text
Client -> Feed Query Service -> feed cache/inbox -> visibility filter -> ranking -> hydration -> response + nextCursor
```

Failure behavior:

- Ranking slow: return chronological or cached ranked page.
- Hydration partial failure: return minimal cards or skip unavailable posts.
- Feed inbox stale: filter at read time before rendering.

Interview signal:

> Feed Query Service is the composition layer. It should not own post storage, graph storage, or ranking internals.

### Ranking Service

Why it exists:

- Chronological feeds are simple, but large products usually need relevance.
- Ranking improves quality using recency, affinity, engagement, freshness, and policy signals.

Core responsibilities:

- Score candidate posts under strict latency budget.
- Use features such as author affinity, recency, engagement, media type, and freshness.
- Apply policy rules such as downranking unsafe or low-quality content.
- Support fallback chronological ranking.
- Log ranking decisions/features for debugging where allowed.

Important boundary:

- Ranking orders candidates; it should not decide whether a private/deleted post is legally visible. Visibility filtering must be enforced separately.

Failure behavior:

- Feature store unavailable: use cached/basic features.
- Ranking model unavailable: fallback to chronological feed.
- Ranking latency high: cap candidates or use simpler strategy.

Interview signal:

> Ranking is quality-critical but not availability-critical. Feed should still work in degraded chronological mode.

### Hydration Service

Why it exists:

- Feed inboxes should store compact candidate references, not full duplicated post objects.
- The final response needs post text/media preview, author display name, counts, and viewer-specific flags.

Core responsibilities:

- Batch fetch posts by ID.
- Batch fetch author/profile preview metadata.
- Attach like/comment/repost counts or cached counters.
- Avoid N+1 reads.
- Hide or skip deleted/unavailable posts.

Failure behavior:

- Missing post: skip item and optionally clean stale inbox reference.
- Author metadata slow: show minimal fallback fields if product allows.
- Counter service slow: omit secondary counters rather than fail feed page.

Interview signal:

> Hydration turns candidate IDs into displayable feed cards and must be batched to keep latency bounded.

### Storage and Cache Components

Why they exist:

- Feed has different data shapes: posts, graph edges, inbox candidates, ranking features, counters, and caches.

Storage responsibilities:

| Store | Owns | Access pattern |
|---|---|---|
| Post Store | canonical post objects | write by author, batch read by post ID |
| Feed Inbox Store | per-user candidate post IDs | append and cursor read by user |
| Follow Graph Store | follower/following edges | adjacency reads and batch reverse reads |
| Ranking Feature Store | model features/signals | low-latency feature lookup |
| Feed Cache | hot first page/candidate windows | read-heavy, short TTL |

Cache rules:

- Cache first feed page briefly.
- Cache hydrated post cards separately.
- Invalidate or filter deleted/private posts at read time.
- Avoid long cache TTLs for users with rapidly changing follow/privacy state.

Interview signal:

> Split stores by access pattern. Feed inbox is not the post store; it is a fast candidate list.

### Event Stream and Backpressure Layer

Why it exists:

- Post, follow, delete, engagement, and ranking-update events need durable async processing.
- Fanout can lag, but it should not lose events.

Core responsibilities:

- Persist `post.created`, `post.deleted`, `follow.created`, `follow.deleted`, and engagement events.
- Let fanout workers replay events after failure.
- Track lag by queue/partition/author type.
- Send poison events to DLQ.
- Support backpressure and autoscaling.

Failure behavior:

- Worker crash: resume from stream offset.
- Queue lag: feed freshness degrades but service remains available.
- Poison event: isolate in DLQ instead of blocking partition forever.

Interview signal:

> Event stream is the safety net that lets feed fanout be asynchronous, replayable, and backpressure-aware.

### How The Components Work Together

Post create path:

```text
Gateway -> Post Service -> Post Store -> Event Stream -> Fanout Workers -> Feed Inbox Store -> Cache invalidation
```

Feed read path:

```text
Gateway -> Feed Query Service -> Feed Cache/Inbox -> Visibility Filter -> Ranking Service -> Hydration -> Response
```

One-stop interview answer:

> I design News Feed with durable post writes, follow graph storage, async fanout for normal users, read-time merge for celebrities, fast feed inbox reads, ranking within a strict latency budget, and read-time visibility filtering. Ranking and hydration can degrade, but deleted/private/blocked content must not leak.

## 2.4 Data Layer

### Storage Choices

| Data type | Candidate storage | Why |
|---|---|---|
| Post content | wide-column/document store | high write throughput, flexible fields |
| Follow graph | graph or KV adjacency lists | efficient follower batch reads |
| Feed inbox | KV/time-series sorted sets | fast retrieval by user and recency |
| Cache | Redis/Memcached | low-latency hot reads |
| Analytics | stream plus OLAP | decoupled high-volume processing |

### Example Logical Schemas

Post record:

```json
{
  "postId": "p_87322",
  "authorId": "u_84",
  "createdAt": "2026-06-17T10:30:00Z",
  "status": "PUBLISHED",
  "text": "Launching a new feature today!",
  "media": ["media-123"],
  "visibility": "PUBLIC"
}
```

Follow edge:

```text
PK: follower_id
SK: followed_id
metadata: created_at, edge_state
```

Feed inbox item:

```text
PK: user_id
SK: created_at desc + post_id
fields: author_id, post_id, source_type, preliminary_score
```

### Partitioning

- Post store partition by `author_id` or `post_id` hash.
- Feed inbox partition by `user_id`.
- Follow graph partition by `follower_id` and optional reverse index by `followed_id`.

### Replication

- Multi-AZ replication for all core stores.
- Cross-region replication for read availability.
- Eventual cross-region consistency acceptable for most feed freshness requirements.

## 2.5 Scalability

### Fanout Strategies

| Strategy | Description | Pros | Cons |
|---|---|---|---|
| Fanout on write | push post ID to followers at publish time | fast reads | expensive for high-follower accounts |
| Fanout on read | build feed candidates at read time | cheaper writes | expensive reads and higher latency |
| Hybrid | normal users write-fanout, celebrities read-fanout | balanced | added complexity |

Recommended:

- Hybrid fanout.
- Threshold-based routing: if follower count exceeds threshold, switch to read-time merge for that author.

### Stateless Services

- Post, fanout orchestrator, and feed query services remain stateless.
- State lives in stores and event streams.

### Partition Management

- Use logical partitions and rebalance gradually.
- Avoid per-user hotspots by spreading celebrity follower processing across worker shards.

## 2.6 Performance

### Caching Strategy

| Cache layer | What to cache | TTL |
|---|---|---:|
| Feed page cache | top feed page per user | short, such as 10-60s |
| Post hydration cache | post preview cards | medium |
| Author profile cache | display metadata | medium |
| Negative cache | unavailable/deleted post IDs | short |

### Pagination

- Use cursor-based pagination, not offset.
- Cursor should include stable ordering keys, such as timestamp plus post ID.

### Hydration Efficiency

- Batch post and author metadata fetch.
- Avoid N+1 reads.
- Use compact payloads for list view.

## 2.7 Async Systems

Use event streams for:

- post-created
- post-deleted
- follow-created
- follow-deleted
- engagement events (like/comment/share)
- feed-invalidation events

Backpressure handling:

- Consumer groups with lag monitoring.
- Retry with dead-letter topic for poison messages.
- Dynamic worker autoscaling by queue lag.
- Shed non-critical enrichment before core fanout.

## 2.8 Reliability

### Retry and Idempotency

- Post create uses idempotency key for client retries.
- Fanout worker updates should be idempotent by `(user_id, post_id)`.
- Exactly-once is hard; design for at-least-once with deduplication.

### Circuit Breakers

- If ranking service is slow, fallback to chronological feed.
- If hydration service is degraded, return minimal post cards.
- If analytics pipeline is down, keep feed serving alive.

### Failover

- Regional failover for read APIs.
- Post writes can be region-home based with replication.
- Use degraded mode during partial regional outages.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Feed ordering | pure chronological | ranked feed | chronology is simple; ranking improves engagement but adds complexity |
| Fanout model | write fanout | read fanout | write fanout improves reads but expensive for celebrities |
| Consistency | strict ordering | eventual ordering | strict ordering hurts scale; eventual improves throughput |
| Storage | one feed store | split inbox and post stores | split supports scale and isolation but more joins/hydration |
| Cache TTL | short | long | short freshness vs long efficiency |

---

# 3. Low-Level Design

LLD goal:

> Model feed generation as a clean split between post creation, follow graph reads, fanout/write-side preparation, feed query/read-side assembly, and ranking.

Simple rule:

- Entities represent feed state.
- Services coordinate user actions.
- Strategies choose ranking/fanout behavior.
- Repositories hide graph, post, and feed-store details.

Starter map:

| LLD question | News feed answer |
|---|---|
| What is the canonical content object? | `Post` |
| What represents the relationship graph? | `FollowEdge` |
| What is stored in a user's feed inbox? | `FeedItem`, usually a post reference, not a full post copy |
| What is the write-side service? | `FanoutService`, which distributes post IDs after post creation |
| What is the read-side service? | `FeedQueryService`, which fetches, filters, ranks, hydrates, and paginates |
| What changes behavior at runtime? | `RankingStrategy` and fanout strategy |

Beginner-friendly design order:

1. Model the canonical `Post` separately from `FeedItem`.
2. Model `FollowEdge` so you know who should receive candidates.
3. Design `FanoutService` for write-time distribution.
4. Design `FeedQueryService` for read-time assembly.
5. Add `RankingStrategy` so chronological and personalized ranking are swappable.
6. Add read-time filters for deleted, private, blocked, muted, or stale feed items.

Interview sentence:

> In LLD, I will separate canonical post storage from per-user feed candidates. The feed query path fetches candidate IDs, filters visibility, ranks them, hydrates post details, and returns a cursor.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `User` | identity and feed preferences | blocked/private rules must be respected |
| `Post` | canonical post content and visibility | deleted/private posts should not render |
| `FollowEdge` | follower-to-creator relationship | inactive edges should not produce feed candidates |
| `FeedItem` | user inbox candidate reference | can point to a post, but does not own post content |
| `FeedCursor` | pagination position | cursor must be tamper-resistant and monotonic |
| `FeedQuery` | request filters and limit | limit must be bounded |
| `RankingContext` | user/device/time features for scoring | ranking must not mutate canonical post state |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `PostService` | create/delete posts and publish post events | compute every follower feed synchronously |
| `FollowService` | maintain follow/unfollow relationships | rank feed items |
| `FanoutService` | distribute post references to follower inboxes | store full duplicated post bodies |
| `FeedQueryService` | fetch candidates, rank, hydrate, paginate | own ranking math directly |
| `RankingService` | score candidate posts | perform storage writes |
| `FeedRepository` | read/write feed candidates | own canonical post content |
| `PostRepository` | store canonical posts | know follower graph internals |
| `FollowGraphRepository` | read forward/reverse follow graph | decide ranking order |

Core flow:

```text
Post create: save post -> publish event -> fanout references -> user inbox candidates
Feed read: fetch candidates -> filter visibility -> rank -> hydrate posts -> return cursor
```

## 3.2 OOP Fundamentals

Encapsulation:

- `FeedCursor` owns encode/decode rules.
- `FeedItem` owns visibility/filter checks.

Abstraction:

- `RankingStrategy` interface allows multiple scoring approaches.
- Repository interfaces hide storage implementations.

Polymorphism:

- `ChronologicalRankingStrategy` and `PersonalizedRankingStrategy`.

Composition:

- `FeedQueryService` composes candidate source, ranking strategy, and hydration modules.

## 3.3 SOLID Principles

| Principle | Application |
|---|---|
| SRP | fanout worker only handles distribution, not ranking |
| OCP | add new ranking strategy without rewriting query service |
| LSP | all ranking strategies produce consistent scored outputs |
| ISP | separate read and write repository interfaces |
| DIP | services depend on interfaces, not concrete DB clients |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | ranking algorithm selection | switch chronological/personalized logic cleanly |
| Factory | create ranking strategy by user tier/context | keep wiring outside feed query logic |
| Observer/Event | post-created event fanout | decouple posting from feed materialization |
| Builder | feed response assembly | attach optional enrichments without messy constructors |
| Proxy | cache-backed repository wrappers | add cache without changing repository contract |

## 3.5 UML / Diagrams

### Class Diagram

```text
+------------------+        +----------------------+
| FeedQueryService |------->| CandidateSource      |
+------------------+        +----------------------+
| getHomeFeed()    |------->| RankingStrategy      |
+--------+---------+        +----------------------+
         |
         v
+------------------+        +----------------------+
| FeedRepository   |<-------| FanoutWorker         |
+------------------+        +----------------------+
| getCandidates()  |        | processPostEvent()   |
+------------------+        +----------------------+
```

### Sequence Diagram - Get Home Feed

```text
Client -> FeedQueryService: getHomeFeed(userId, cursor)
FeedQueryService -> FeedCache: get(userId, cursor)
FeedCache --> FeedQueryService: miss
FeedQueryService -> FeedRepository: getCandidates(userId, cursor, limit)
FeedRepository --> FeedQueryService: candidatePostIds
FeedQueryService -> RankingService: score(userId, candidatePostIds)
RankingService --> FeedQueryService: rankedPostIds
FeedQueryService -> PostRepository: batchGet(rankedPostIds)
FeedQueryService --> Client: feedItems + nextCursor
```

## 3.6 Class Design

```java
interface RankingStrategy {
    List<ScoredPost> rank(String userId, List<CandidatePost> candidates, RankingContext context);
}

interface FeedRepository {
    List<CandidatePost> getCandidates(String userId, FeedCursor cursor, int limit);
    void appendToInbox(String userId, String postId, long createdAtEpochMs);
}

interface FanoutService {
    void distributePost(PostCreatedEvent event);
}
```

## 3.7 Data Handling

Machine-coding data structures:

| Need | Data structure |
|---|---|
| follow graph | `Map<UserId, Set<UserId>>` |
| posts | `Map<PostId, Post>` |
| user inbox | `Map<UserId, Deque<FeedItem>>` |
| dedup set | `Set<(userId, postId)>` |
| fanout queue | `Queue<PostCreatedEvent>` |

Production:

- Follow graph store with forward and reverse indexes.
- Feed inbox store for precomputed candidates.
- Post store for canonical post objects.
- Ranking feature store for scoring signals.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| user follows nobody | return fallback recommendations or empty state |
| user follows too many accounts | cap read fan-in, rely on precomputed inbox candidates |
| post deleted after fanout | filter during hydration and optionally clean inbox later |
| privacy change after fanout | enforce visibility at read time, not only fanout time |
| duplicate fanout event | dedup by `(userId, postId)` or idempotent inbox append |
| cursor tampering | sign/encode cursor and reject invalid values |
| celebrity post fanout spike | hybrid fanout: push to normal users, pull for huge accounts |
| ranking service slow | degrade to chronological or cached ranking fallback |

Interview rule:

> News feed LLD is about separating write-side fanout from read-side ranking/hydration, while keeping visibility checks correct even when cached feed items are stale.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested structure:

```text
newsfeed/
  domain/
    Post.java
    FeedItem.java
    FollowEdge.java
    FeedCursor.java
  service/
    PostService.java
    FollowService.java
    FanoutService.java
    FeedQueryService.java
  ranking/
    RankingStrategy.java
    ChronologicalRankingStrategy.java
  repository/
    PostRepository.java
    FeedRepository.java
    FollowGraphRepository.java
  infra/
    EventBusPublisher.java
    InMemoryQueue.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import defaultdict, deque
from dataclasses import dataclass
from typing import Deque
import time


@dataclass(frozen=True)
class Post:
    post_id: str
    author_id: str
    text: str
    created_at: float


class InMemoryNewsFeed:
    def __init__(self) -> None:
        self.following: dict[str, set[str]] = defaultdict(set)
        self.posts: dict[str, Post] = {}
        self.inbox: dict[str, Deque[str]] = defaultdict(deque)

    def follow(self, follower: str, followed: str) -> None:
        self.following[follower].add(followed)

    def create_post(self, author_id: str, text: str) -> Post:
        post_id = f"p_{len(self.posts) + 1}"
        post = Post(post_id=post_id, author_id=author_id, text=text, created_at=time.time())
        self.posts[post_id] = post

        # Fanout on write for normal users in this demo.
        for follower, followed_set in self.following.items():
            if author_id in followed_set:
                self.inbox[follower].appendleft(post_id)
        self.inbox[author_id].appendleft(post_id)
        return post

    def get_feed(self, user_id: str, limit: int = 20) -> list[Post]:
        post_ids = list(self.inbox[user_id])[:limit]
        return [self.posts[pid] for pid in post_ids if pid in self.posts]


nf = InMemoryNewsFeed()
nf.follow("u2", "u1")
nf.create_post("u1", "hello world")
print([post.text for post in nf.get_feed("u2")])
```

## 4.3 Data Structures

| Structure | Why |
|---|---|
| hash maps | O(1) lookup for posts and users |
| deque | efficient appendleft/pop for feed windows |
| priority queue | optional ranking merge by score or timestamp |
| set | dedup follow edges and feed IDs |
| queue/stream | asynchronous fanout processing |

## 4.4 Concurrency

Concurrency risks:

- Duplicate fanout due to retries.
- Follow/unfollow race during fanout.
- Post deletion race with feed read.
- Hot partition under celebrity post.

Mitigations:

- Idempotent fanout writes keyed by `(user_id, post_id)`.
- Event versioning and last-write-wins policy for follow edges.
- Soft delete with tombstones for post lifecycle.
- Partitioned fanout queues plus consumer autoscaling.

## 4.5 Performance Optimization

Complexity perspective:

- Fanout on write cost roughly scales with follower count.
- Feed fetch cost depends on cache hit and candidate volume.
- Ranking complexity must fit strict latency budget.

Optimizations:

- Hybrid fanout.
- Multi-tier caching for feed pages and hydration.
- Candidate truncation before ranking.
- Batched metadata hydration.
- Async precompute for active users.

## 4.6 Error Handling

| Error | Behavior |
|---|---|
| invalid cursor | `400 Bad Request` |
| unauthorized feed access | `401/403` |
| ranking timeout | fallback chronological response |
| fanout backlog high | serve stale cache + degrade freshness |
| deleted post in inbox | skip item during hydration |
| follow graph temporary failure | return best-effort cached feed |

## 4.7 Testing Thinking

Unit tests:

- follow/unfollow behavior.
- post creation and self-inbox insertion.
- fanout distribution correctness.
- cursor pagination stability.
- ranking fallback logic.

Concurrency tests:

- duplicate event processing does not duplicate feed item.
- unfollow during fanout behaves deterministically.
- parallel post creation order handling.

Load tests:

- hot celebrity post.
- large follow graph fetch.
- cache outage fallback.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| celebrity post spike | 50M followers receive one post | fanout queue explosion |
| read storm | push notification drives massive app opens | feed read saturation |
| bot scraping | automated feed crawling | cache/DB abuse |
| ranking dependency outage | model or feature service down | high latency or errors |
| regional outage | failover to one region | concentrated overload |

## 5.2 Immediate Response

1. Protect API edge with rate limiting and bot controls.
2. Prioritize feed read availability over deep ranking quality.
3. Switch to cached/chronological feed when ranking is degraded.
4. Increase cache TTL briefly for hot feed slices.
5. Apply backpressure and worker autoscale for fanout queues.
6. For celebrity posts, bypass write-fanout and use read-time merge.
7. Shed non-critical enrichments such as low-priority analytics tags.

## 5.3 Celebrity Post Strategy

Problem:

```text
Fanout-on-write for a user with tens of millions of followers is too expensive.
```

Strategy:

- Mark author as high-fanout account.
- Do not push post into every follower inbox immediately.
- At read time, merge celebrity timeline posts into user feed candidates.
- Cache merged results for short durations.

## 5.4 Degradation Policy

Protect in order:

1. Feed endpoint availability.
2. Basic chronological relevance.
3. Freshness.
4. Rich ranking quality.
5. Secondary analytics.

Allowed degradation:

- Fallback to chronological ranking.
- Return smaller page size.
- Increase cursor page cache TTL.
- Delay low-priority fanout and analytics updates.

Not allowed:

- Lose durable posts.
- Serve blocked/private content incorrectly.
- Let ranking outage take down feed serving.

## 5.5 Spike Interview Answer

> For abnormal spikes, I first classify the event: celebrity fanout spike, read storm, scraping attack, or dependency outage. For celebrity spikes, I switch to hybrid fanout and avoid pushing to all followers on write. For read storms, I serve cached feed windows and degrade ranking to chronological fallback. I protect core feed APIs first, keep post durability intact, apply queue backpressure, and shed non-critical enrichments.

---

# 6. Scaling To A Billion Users

## 6.1 Architecture At Billion Scale

```text
Global DNS/Anycast
  -> regional API gateways
  -> feed query fleets
  -> regional feed cache
  -> inbox/feed stores and post stores
  -> async event stream backbone
  -> ranking feature systems
```

## 6.2 Hybrid Fanout As Default

For billion users, pure strategies are insufficient:

- Pure fanout-on-write is too expensive for celebrities.
- Pure fanout-on-read is too expensive for all users.

Recommended:

- Fanout-on-write for low/medium follower accounts.
- Fanout-on-read for high-follower accounts.
- Dynamic threshold by follower count and traffic behavior.

## 6.3 Graph and Feed Storage Scaling

- Partition follow graph by follower and reverse lookup by followed.
- Partition inbox store by user ID.
- Keep only recent candidate windows in hot store.
- Archive older feed references to cheaper storage.
- Use separate stores for posts vs feed pointers.

## 6.4 Read Path Scaling

- Multi-tier caches for feed pages and hydration data.
- Cursor pagination to avoid expensive offsets.
- Candidate generation and ranking with strict time budgets.
- Request coalescing for hot users/pages.

## 6.5 Write Path Scaling

- Durable post writes with replicated log/store.
- Async fanout workers with partitioned queues.
- Idempotent delivery semantics.
- Backpressure when queue lag increases.

## 6.6 Multi-Region Considerations

- Serve feed reads from nearest region.
- Replicate posts and graph updates across regions asynchronously.
- Use region-home writes if needed for conflict simplicity.
- Accept eventual cross-region feed ordering in many cases.

## 6.7 Billion-User Capacity Signals

| Layer | Key signal |
|---|---|
| edge/gateway | request rate, bot traffic, WAF blocks |
| feed cache | hit ratio, eviction pressure |
| fanout pipeline | consumer lag, retry rate, DLQ volume |
| ranking | p95/p99 latency, fallback rate |
| hydration | batch size, fanout N+1 misses |
| stores | hot partitions, throttling, replication lag |

## 6.8 Billion-User Interview Answer

> At billion-user scale, I would run a hybrid feed design: write-fanout for regular users, read-fanout for celebrities, and strict separation of post durability from feed distribution. Reads are served through regional feed query fleets with heavy caching and cursor pagination. Ranking is time-bounded with chronological fallback. Fanout and analytics stay asynchronous with lag-based autoscaling and backpressure so spikes do not break core feed availability.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

Use this answer shape:

```text
I will first clarify feed semantics, freshness, and ranking expectations.
Then I will estimate post write rate, feed read QPS, and follower skew.
For architecture, I use post store, follow graph, fanout pipeline, feed inbox store, feed cache, ranking service, and hydration service.
For scale, I choose hybrid fanout: write-fanout for regular users and read-fanout for celebrities.
For reliability, ranking and analytics are non-blocking dependencies with fallback.
For LLD, I model Post, FollowEdge, FeedItem, FanoutWorker, and RankingStrategy interfaces.
For spikes, I protect feed availability first and degrade ranking quality before failing requests.
```

---

# 8. Fast Recall Rules

- News feed is read-heavy but write bursts matter.
- Follower distribution is skewed; celebrity strategy is mandatory.
- Hybrid fanout is the practical large-scale default.
- Feed fetch should use cursor pagination, not offset pagination.
- Ranking must have strict latency budgets and fallback mode.
- Cache feed windows and hydrate in batches to avoid N+1 reads.
- Keep posts durable even if fanout is delayed.
- Use idempotent fanout writes to tolerate retries.
- Backpressure and queue lag monitoring are mandatory.
- Never let ranking or analytics outage take down feed serving.

