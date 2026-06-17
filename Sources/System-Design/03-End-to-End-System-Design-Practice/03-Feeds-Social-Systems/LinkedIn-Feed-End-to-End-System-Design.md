# LinkedIn Feed - End-to-End System Design

> Goal: design a professional-network feed with connection updates, creator/company posts, job/news signals, ranking, caching, consistency, fanout, and timeline storage.

---

## How To Use This File

- Use this when the problem says LinkedIn feed, professional social network, connection feed, company feed, or career/news feed.
- LinkedIn is not only a friend/follow feed; it must balance professional relevance, network distance, trust, freshness, and content diversity.
- The core design is a hybrid feed system: push normal connection posts, pull high-follower creators/pages/news, rank with professional signals, and filter trust/safety at read time.
- Keep feed serving separate from search, messaging, recruiter workflows, and full job recommendation systems unless the interviewer expands scope.

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

| Layer | Interview signal | LinkedIn focus |
|---|---|---|
| Problem understanding | Can clarify professional feed scope | connections, follows, companies, jobs/news optional, comments/reactions |
| HLD | Can split feed planes | post service, graph service, fanout, feed store, ranking, trust, hydration |
| LLD | Can model clean components | `ProfessionalPost`, `ConnectionEdge`, `FeedCandidate`, `RankingStrategy`, `VisibilityPolicy` |
| Machine coding | Can implement core flow | connect/follow, create post, fanout, fetch ranked feed, paginate |
| Traffic spikes | Can protect production | viral company posts, layoff/news events, influencer posts, read storms |
| Billion users | Can scale globally | hybrid fanout, graph partitioning, multi-tier caches, regional serving |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Member can create a post with text, links, images, videos, polls, documents, or article references.
- Member can connect with another member.
- Member can follow creators, companies, schools, groups, hashtags, or newsletters.
- Member can fetch a personalized home feed.
- Feed can include posts from direct connections, followed entities, second-degree interactions, companies, groups, and optionally jobs/news/promoted content.
- Member can react, comment, repost, hide, unfollow, report, or mark not interested.
- Feed supports cursor-based pagination.
- Feed must enforce visibility, block, privacy, moderation, and trust constraints.

Optional requirements to clarify:

- Is the feed only from connections/follows, or can it include recommendations?
- Are job recommendations and ads in scope?
- Are comments that connections reacted to included?
- Should feed ranking optimize for engagement, professional relevance, or freshness?
- Is profile/company page timeline in scope?
- Are groups/newsletters/events included?

Out of scope unless asked:

- Full job matching and recruiter search.
- Full ads auction platform.
- Full notification system.
- Complete moderation review tooling.
- Messaging and inbox.

## 1.2 Non-Functional Requirements

Feed read path:

- Low latency for first page and scrolling.
- High availability because feed is a core engagement surface.
- Stable cursor pagination.
- Graceful degradation to cached or chronological feed when ranking is unhealthy.

Write/fanout path:

- Durable post creation.
- Asynchronous fanout so posting does not block on connection/follower count.
- Idempotent fanout and event processing.
- Backpressure handling for influencers, companies, and major news events.

Correctness and trust:

- Privacy, block, moderation, spam, and professional trust checks must run before rendering.
- Reactions/comments counts can be eventually consistent.
- Feed delivery and ranking order can be eventually consistent.
- Author's own post should be visible on their profile immediately.

## 1.3 Constraints

- Professional graph has multiple relationship types: connection, follow, company follow, group membership, hashtag follow, school/employer affinity.
- Ranking must avoid low-quality engagement bait because professional trust matters.
- Feed reads are much more frequent than post writes.
- Influencers and companies can have huge follower counts.
- Content visibility can depend on network distance and author settings.
- Feed should avoid too many posts from one company/person/topic.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Registered members | 1 billion |
| Daily active members | 200 million |
| Posts/day | 100 million |
| Feed item reads/day | 50 billion |
| Average connections/member | 300 to 800 |
| Followed companies/creators | 50 to 300 per member |
| Large creator/company followers | 10M to 100M+ |
| Feed page size | 20 items |
| First page p99 target | under 200 ms |

Back-of-the-envelope:

- `100M posts/day` is about `1,157 posts/sec` average, but business/news cycles create peaks.
- If every post faned out to 500 recipients, that is `50B` candidate writes/day before recommendations and replication.
- Company/influencer posts need special handling because full fanout can be expensive.
- Feed ranking should operate on bounded candidate windows, not the entire professional graph.

## 1.5 Clarifying Questions To Ask

- Is the feed ranked or chronological?
- Are second-degree interactions included?
- Are company posts, jobs, ads, or newsletters included?
- What is more important: professional relevance, engagement, freshness, or trust?
- How fresh should a new connection post be?
- How strict are privacy and network-distance visibility rules?
- Can we serve a stale cached feed in degraded mode?

Strong interview framing:

> I will design LinkedIn Feed as a hybrid professional feed: canonical posts are stored once, normal members push lightweight candidates to connections/followers, large creators and companies are pulled or partially faned out, ranking uses professional graph and engagement signals, and read-time trust/visibility checks protect correctness.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Post create flow:
Client -> API Gateway -> Post Service -> Post Store
       -> Event Stream: post.created
       -> Fanout Planner -> Fanout Workers
       -> Feed Candidate Store / Member Inbox
       -> Cache invalidation

Feed read flow:
Client -> API Gateway -> Feed Query Service
       -> Feed Cache / Candidate Store
       -> Pull Sources for companies/creators/recommendations
       -> Ranking Service
       -> Trust and Visibility Filter
       -> Hydration Service
       -> Response with cursor

Engagement flow:
Client -> Engagement Service -> Event Stream
       -> Counter Aggregator / Feature Pipeline / Ranking Feature Store
```

Recommended architecture:

```text
                          +----------------------+
                          | Ranking Service      |
                          +----------+-----------+
                                     |
Client                               v
  |                        +----------------------+
  v                        | Feed Query Service   |
+-------------+            +----------+-----------+
| API Gateway |                       |
+------+------+                       v
       |                    +----------------------+
       |                    | Feed Cache / Inbox   |
       |                    +----------+-----------+
       v                               |
+--------------+                       v
| Post Service |             +---------------------+
+------+-------+             | Hydration Service   |
       |                     +----------+----------+
       v                                |
+--------------+                       v
| Post Store   |             +---------------------+
+------+-------+             | Trust/Visibility    |
       |                     +---------------------+
       v
+--------------+             +---------------------+
| Event Stream |-----------> | Fanout Workers      |
+------+-------+             +----------+----------+
       |                                |
       v                                v
+--------------+             +---------------------+
| Feature      |             | Professional Graph  |
| Pipeline     |             | Service             |
+--------------+             +---------------------+
```

## 2.2 APIs

### Create Post

```http
POST /v1/posts
Authorization: Bearer <token>
Idempotency-Key: post-123
Content-Type: application/json

{
  "text": "We are hiring backend engineers for a distributed systems team.",
  "mediaIds": [],
  "visibility": "CONNECTIONS",
  "topics": ["hiring", "backend", "distributed-systems"]
}
```

Response:

```json
{
  "postId": "p_7001",
  "authorId": "m_42",
  "status": "PUBLISHED",
  "createdAt": "2026-06-17T10:00:00Z"
}
```

### Get Home Feed

```http
GET /v1/feed/home?cursor=abc&limit=20
Authorization: Bearer <token>
```

Response:

```json
{
  "items": [
    {
      "postId": "p_7001",
      "authorId": "m_42",
      "score": 0.89,
      "reason": "connection_post",
      "socialContext": "3 connections work at this company",
      "createdAt": "2026-06-17T10:00:00Z"
    }
  ],
  "nextCursor": "def"
}
```

### Connect Or Follow

```http
POST /v1/graph/follow
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetId": "company_99",
  "targetType": "COMPANY"
}
```

### Record Feed Action

```http
POST /v1/feed/actions
Authorization: Bearer <token>
Content-Type: application/json

{
  "postId": "p_7001",
  "action": "NOT_INTERESTED"
}
```

## 2.3 Core Components

LinkedIn Feed is a blend of social graph, professional graph, content quality, and business context.

| Plane | Owns | Main goal |
|---|---|---|
| Content plane | posts, articles, media, lifecycle | store professional content safely |
| Graph plane | connections, follows, companies, groups, topics | decide candidate eligibility |
| Fanout plane | candidate distribution | prepare likely feed items |
| Ranking plane | professional relevance and quality | order feed well |
| Trust plane | spam, moderation, privacy, block | protect feed quality and correctness |
| Feature plane | reactions, comments, dwell, profile/company signals | improve ranking over time |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| API Gateway | auth, rate limits, request validation | feed ranking | QPS and abuse |
| Post Service | canonical post lifecycle | follower fanout loops | write QPS |
| Professional Graph Service | connections, follows, company/group/topic edges | post body storage | graph size and skew |
| Fanout Planner | push/pull decision | ranking final page | author/entity follower count |
| Fanout Workers | candidate writes | canonical post truth | event backlog |
| Feed Candidate Store | member feed inbox entries | full post body | storage amplification |
| Feed Query Service | candidate fetch/merge/pagination | model training | read QPS |
| Ranking Service | professional relevance scoring | visibility rules alone | feature latency |
| Trust/Visibility Service | privacy, block, spam, policy checks | ranking order | correctness |
| Hydration Service | post/author/company/social-context metadata | candidate storage | batch read QPS |
| Feature Pipeline | engagement and professional features | synchronous response | event throughput |

### Feed Generation: Push vs Pull

| Source | Strategy | Why |
|---|---|---|
| direct connection post | push on write | high relevance and fast reads |
| normal followed creator | push on write | manageable follower count |
| large influencer/company | pull or active-member fanout | avoids huge write amplification |
| group/company/topic recommendation | pull/rank on read | depends on viewer context |
| second-degree engagement | pull or generated candidate | too broad to fanout blindly |
| jobs/news/promoted content | specialized candidate source | separate ranking/business rules |

Recommended choice:

- Use push for direct connections and normal creators.
- Use pull or partial fanout for high-follower companies/influencers.
- Use recommendation sources for second-degree and topic content.
- Store compact candidates and hydrate details at read time.

### Ranking Service

LinkedIn ranking cares about professional relevance, not just clicks.

Ranking signals:

| Signal family | Examples |
|---|---|
| graph proximity | 1st-degree, 2nd-degree, same company, same school, same industry |
| professional interest | followed topics, skills, job function, industry |
| author affinity | profile views, messages, reactions, comments, past follows |
| content quality | spam score, originality, link quality, language, media type |
| engagement quality | meaningful comments, saves, dwell time, reshares |
| freshness | post age, trend velocity |
| negative feedback | hides, reports, unfollows, low dwell |
| diversity | avoid too many jobs, companies, or same author |

Fallback:

- Chronological feed from connection/follow candidates.
- Cached ranked first page.
- Lightweight heuristic scoring if feature store is slow.

### Timeline Storage

| Timeline/store | Key | Value | Purpose |
|---|---|---|---|
| author timeline | `(authorId, createdAt DESC)` | post IDs | profile and pull source |
| member home inbox | `(memberId, candidateTime DESC)` | feed candidate references | fast feed reads |
| company/page timeline | `(companyId, createdAt DESC)` | post IDs | page profile and pull source |
| group/topic candidate store | `(topicId, time/rank)` | post IDs | recommendations |
| impression/action history | `(memberId, time)` | shown/actioned post IDs | dedup and feedback |

Feed candidate should store:

```text
member_id
post_id
author_id
source_type: CONNECTION | FOLLOW | COMPANY | GROUP | TOPIC | RECOMMENDATION
candidate_time
base_score_hint
fanout_version
```

Do not store:

- full post body,
- full comment tree,
- full author/company profile,
- permanent final ranking score.

### Caching Strategy

| Cache | Key | Value | TTL |
|---|---|---|---|
| feed first page | `feed:first:{memberId}` | ranked post IDs | seconds to minutes |
| candidate window | `feed:candidates:{memberId}` | candidate post IDs | minutes |
| post preview | `post:{postId}` | post text/media/link preview | minutes |
| author preview | `member:{memberId}` | name, title, photo | minutes to hours |
| company preview | `company:{companyId}` | name, logo, industry | minutes to hours |
| social context | `(memberId, postId)` | why shown/context | short |
| counters | `post:counters:{postId}` | reactions/comments/reposts | seconds |

Cache safety rule:

> Cached candidates are not final truth; visibility, privacy, and trust filters still run before rendering.

### Consistency Model

| Data | Consistency target | Why |
|---|---|---|
| post creation | strong canonical write | avoid lost posts |
| author profile timeline | read-your-write | author expects immediate visibility |
| home feed fanout | eventual | async delivery acceptable |
| connection/follow change | eventual for candidate store | read-time checks can correct stale candidates |
| privacy/block/delete/moderation | read-time enforced | correctness and trust |
| reactions/comments counters | eventual | high-volume counters |
| ranking features | eventual | feature pipeline can lag |

One-stop interview answer:

> LinkedIn Feed is a professional hybrid feed. I store posts once, push direct-connection and normal-creator candidates, pull large companies/influencers and recommendations at read time, rank with professional graph and quality signals, enforce trust/privacy before rendering, hydrate post/member/company metadata in batches, and cache short-lived candidate windows for fast scrolling.

---

# 3. Low-Level Design

LLD goal:

> Model professional feed generation as canonical posts plus graph relationships, feed candidates, ranking, visibility, trust, hydration, and engagement feedback.

Simple rule:

- `ProfessionalPost` is the canonical post.
- `ProfessionalGraphEdge` explains relationships.
- `FeedCandidate` says a post may appear for a member.
- `RankingStrategy` orders candidates.
- `VisibilityPolicy` and `TrustPolicy` decide what must not render.

Starter map:

| LLD question | LinkedIn answer |
|---|---|
| Canonical content object | `ProfessionalPost` |
| Relationship object | `ProfessionalGraphEdge` |
| Feed inbox row | `FeedCandidate` |
| Write-side workflow | `PostService.createPost()` then `FanoutService.distribute()` |
| Read-side workflow | `FeedQueryService.getHomeFeed()` |
| Ranking abstraction | `RankingStrategy` |
| Correctness gates | `VisibilityPolicy`, `TrustPolicy` |
| Async feedback | `EngagementEvent` pipeline |

Beginner-friendly design order:

1. Model `ProfessionalPost` with author, visibility, lifecycle, and topics.
2. Model graph edges: connection, follow, company follow, group membership, block, mute.
3. Model `FeedCandidate` as a post reference for one viewer.
4. Build `FanoutService` to distribute normal connection/follow posts.
5. Build pull candidate sources for companies, creators, topics, and recommendations.
6. Build `FeedQueryService` to merge, filter, rank, hydrate, and paginate.
7. Add trust and visibility filters as the final safety gates.

Interview sentence:

> In LLD, I will keep professional posts, graph edges, feed candidates, ranking, trust filtering, and hydration separate so candidate generation can be asynchronous while final rendering remains correct and relevant.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `ProfessionalPost` | canonical post metadata and lifecycle | hidden/deleted/restricted posts must not render |
| `ProfessionalGraphEdge` | connection, follow, company, group, topic relation | inactive edges should not create future candidates |
| `FeedCandidate` | viewer-specific post reference | unique by `(memberId, postId, sourceType)` |
| `FeedCursor` | pagination position | opaque and tamper-resistant |
| `RankingContext` | member/session/request features | request-scoped |
| `EngagementEvent` | reaction/comment/share/hide signal | idempotent by event ID |
| `VisibilityPolicy` | privacy and block checks | final gate before response |
| `TrustPolicy` | spam/moderation/professional quality checks | final gate before response |

Core services:

| Service | Responsibility |
|---|---|
| `PostService` | create/edit/delete posts and publish events |
| `GraphService` | maintain professional relationships |
| `FanoutService` | append candidates to member inboxes |
| `CandidateMergeService` | combine push and pull sources |
| `FeedQueryService` | orchestrate feed read |
| `RankingService` | score candidates |
| `TrustVisibilityService` | filter forbidden content |
| `HydrationService` | fetch post/member/company/social context |

## 3.2 Class Sketch

```java
interface CandidateSource {
    List<FeedCandidate> getCandidates(String memberId, FeedCursor cursor, int limit);
}

interface RankingStrategy {
    List<ScoredCandidate> rank(String memberId, List<FeedCandidate> candidates, RankingContext context);
}

interface VisibilityPolicy {
    boolean canView(String memberId, ProfessionalPost post, GraphSnapshot graphSnapshot);
}

interface TrustPolicy {
    boolean isAllowed(ProfessionalPost post, MemberContext viewerContext);
}
```

## 3.3 Sequence Diagram

```text
Client -> FeedQueryService: getHomeFeed(memberId, cursor)
FeedQueryService -> FeedCandidateStore: getInboxCandidates(memberId)
FeedQueryService -> CompanySource: pullCompanyCandidates(memberId)
FeedQueryService -> TopicSource: pullTopicCandidates(memberId)
FeedQueryService -> TrustVisibilityService: filter(candidates)
FeedQueryService -> RankingService: rank(candidates)
FeedQueryService -> HydrationService: hydrate(postIds)
FeedQueryService --> Client: feed items + next cursor
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | ranking and fanout policies |
| Observer/Event | post-created event triggers fanout and features |
| Factory | choose candidate sources for feed type |
| Chain of Responsibility | trust and visibility filters |
| Adapter | graph, post, feature, cache clients |
| Decorator | cache-backed repositories |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| member has no connections | use onboarding topics, follows, jobs/news recommendations |
| company post goes viral | pull or active-member partial fanout |
| connection removed after fanout | read-time visibility/graph check filters stale candidate |
| member blocks author | filter and invalidate candidate cache |
| post deleted after cache | tombstone check removes it |
| low-quality/spam post | trust policy blocks or downranks |
| ranking timeout | chronological connection/follow fallback |
| duplicate fanout event | idempotent insert by `(memberId, postId, sourceType)` |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
linkedinfeed/
  domain/
    ProfessionalPost.java
    ProfessionalGraphEdge.java
    FeedCandidate.java
    FeedCursor.java
    EngagementEvent.java
  service/
    PostService.java
    GraphService.java
    FanoutService.java
    FeedQueryService.java
    TrustVisibilityService.java
  ranking/
    RankingStrategy.java
    ChronologicalRankingStrategy.java
    ProfessionalRelevanceRankingStrategy.java
  repository/
    PostRepository.java
    GraphRepository.java
    FeedCandidateRepository.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import defaultdict, deque
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class ProfessionalPost:
    post_id: str
    author_id: str
    text: str
    topics: set[str]
    created_at: float
    status: str = "PUBLISHED"


class LinkedInFeed:
    def __init__(self) -> None:
        self.connections: dict[str, set[str]] = defaultdict(set)
        self.posts: dict[str, ProfessionalPost] = {}
        self.inbox: dict[str, deque[str]] = defaultdict(deque)
        self.hidden: set[tuple[str, str]] = set()

    def connect(self, member_a: str, member_b: str) -> None:
        self.connections[member_a].add(member_b)
        self.connections[member_b].add(member_a)

    def create_post(self, author_id: str, text: str, topics: set[str]) -> ProfessionalPost:
        post = ProfessionalPost(f"p_{len(self.posts) + 1}", author_id, text, topics, time())
        self.posts[post.post_id] = post
        for member_id in self.connections[author_id]:
            self.inbox[member_id].appendleft(post.post_id)
        self.inbox[author_id].appendleft(post.post_id)
        return post

    def get_feed(self, member_id: str, limit: int = 20) -> list[ProfessionalPost]:
        result: list[ProfessionalPost] = []
        for post_id in self.inbox[member_id]:
            post = self.posts.get(post_id)
            if not post or post.status != "PUBLISHED":
                continue
            if (member_id, post.post_id) in self.hidden:
                continue
            result.append(post)
            if len(result) == limit:
                break
        return result


feed = LinkedInFeed()
feed.connect("m1", "m2")
feed.create_post("m1", "Hiring backend engineers", {"hiring", "backend"})
print([post.text for post in feed.get_feed("m2")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| member connections | `Map<MemberId, Set<MemberId>>` |
| reverse followers | `Map<EntityId, Set<MemberId>>` |
| post store | `Map<PostId, ProfessionalPost>` |
| feed inbox | `Map<MemberId, Deque<PostId>>` |
| hidden/dedup set | `Set<(memberId, postId)>` |
| ranking top K | priority queue or bounded sorted list |
| fanout events | queue/stream |

## 4.4 Concurrency

- Use idempotency key for post create.
- Use outbox/event log so post creation and event publishing do not diverge.
- Use unique candidate key `(memberId, postId, sourceType)`.
- Treat graph changes as versioned events.
- Use read-time visibility checks for delete/block/privacy races.
- Use sharded counters for viral posts.

## 4.5 Performance Optimization

- Keep feed candidates compact.
- Cache first page and candidate windows.
- Batch hydrate post/member/company metadata.
- Bound ranking candidate count.
- Pull large companies/influencers at read time.
- Precompute professional affinity features offline.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Viral Company Or Influencer Post

Problem:

- A company layoff/update, hiring post, or influencer post can create huge reads, comments, reposts, and candidate fanout.

Handling:

- Classify high-follower entities.
- Use pull or active-member partial fanout.
- Cache post/company/author hydration data.
- Shard engagement counters.
- Isolate high-follower fanout queues.
- Apply comment/reaction write backpressure.

## 5.2 Business News Read Storm

Handling:

- Serve cached first page for short windows.
- Reduce ranking feature budget.
- Lower page size temporarily if needed.
- Prefer connection/follow candidates over expensive recommendations.
- Keep trust and visibility filters enabled.

## 5.3 Feature Store Or Ranking Degradation

Handling:

- Fall back to chronological connection/follow feed.
- Use cached professional-affinity features.
- Disable expensive second-degree recommendation sources.
- Return fewer but safe feed items.

## 5.4 Engagement Storm

Handling:

- Write reactions/comments/reposts to event stream.
- Aggregate counters asynchronously.
- Use sharded counters for hot posts.
- Delay non-critical ranking feature updates under pressure.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| posts | `postId` or `(authorId, time)` | direct lookup and profile timeline |
| member home inbox | `memberId` | per-member feed read |
| graph forward edges | `memberId` | connections/follows lookup |
| graph reverse edges | `entityId` plus shards | fanout recipients |
| engagement events | `postId + time` | hot post aggregation |
| feature store | `memberId`, `postId`, feature family | ranking reads |

## 6.2 Multi-Region Strategy

- Serve members from a home region.
- Replicate public post metadata and company/member previews globally.
- Use regional feed caches.
- Accept eventual feed freshness across regions.
- Replicate delete/block/moderation events with high priority.

## 6.3 Storage Economics

- Store candidate references, not full posts.
- Cap member inbox windows.
- TTL old feed candidates.
- Rebuild cold users lazily.
- Pull huge creators and companies on read.
- Archive old engagement events to analytics storage.

## 6.4 Observability

Track:

- feed p99 latency,
- feed cache hit rate,
- fanout lag by author/entity tier,
- ranking timeout rate,
- trust-filter drop rate,
- stale candidate rate,
- hydration batch latency,
- duplicate feed candidate rate,
- engagement counter lag.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify feed inventory: connections, follows, companies, groups, jobs, ads.
2. Estimate read/write volume and graph skew.
3. Design canonical post store and professional graph.
4. Add hybrid fanout into member feed candidates.
5. Add ranking with professional relevance signals.
6. Add trust/visibility filtering and hydration.
7. Add caching, consistency, and timeline storage.
8. Discuss viral business/news spikes and billion-user scaling.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| feed generation | push | pull | hybrid |
| ranking | chronological | professional relevance | relevance with fallback |
| storage | full post copies | candidate references | references |
| consistency | strong everywhere | mixed | strong post/privacy, eventual feed/counters |
| candidates | only graph | graph + recommendations | graph plus controlled recommendations |

## 7.3 Common Mistakes

- Treat LinkedIn exactly like a casual social feed.
- Ignore professional trust and spam quality.
- Fan out huge company/influencer posts synchronously.
- Store full post body in every feed row.
- Skip read-time privacy/block/moderation checks.
- Rank unbounded second-degree candidate sets.
- Hydrate each feed item one by one.

## 7.4 Strong Closing

> LinkedIn Feed is a professional hybrid feed: push close-network candidates, pull large entities and recommendations, rank by professional relevance and quality, enforce trust/visibility at read time, and cache compact candidate windows for low-latency scrolling.

---

# 8. Fast Recall Rules

- LinkedIn feed = posts + professional graph + hybrid fanout + ranking + trust + hydration.
- Push direct connection and normal creator posts.
- Pull companies, influencers, topics, and recommendations when fanout is too expensive.
- Rank by professional relevance, not just engagement.
- Store feed candidate references, not full posts.
- Trust/visibility checks are final gates.
- Engagement counters and ranking features are eventually consistent.
- Cache first page, candidates, previews, counters, and features separately.
- Keep author/company timelines separate from home feed.
- Degrade to chronological connection/follow feed when ranking fails.