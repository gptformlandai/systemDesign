# Instagram / Facebook Feed - End-to-End System Design

> Goal: design a social media feed that supports media-heavy posts, social graph fanout, personalized ranking, fast scrolling, cache efficiency, and correct privacy handling.

---

## How To Use This File

- Use this as the baseline social-graph feed design.
- Focus on the repeated interview themes: feed generation, fanout, ranking, caching, consistency, and timeline storage.
- Think of Instagram/Facebook feed as a read-heavy product surface built on top of posts, media, relationships, engagement, and ranking.
- In interviews, start with the simple chronological feed, then evolve it to a ranked hybrid fanout feed.

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

| Layer | Interview signal | Instagram / Facebook focus |
|---|---|---|
| Problem understanding | Can clarify exact feed scope | home feed, profile feed, media posts, stories/reels optional |
| HLD | Can split write/read/ranking/media planes | post store, fanout workers, feed store, ranking, CDN |
| LLD | Can model maintainable feed objects | `Post`, `MediaAsset`, `FeedItem`, `FollowEdge`, `RankingStrategy` |
| Machine coding | Can implement core path | post create, fanout, fetch feed, paginate, rank simply |
| Traffic spikes | Can handle viral posts | celebrity fanout, cache hot media, degrade ranking safely |
| Billion users | Can scale globally | hybrid fanout, regional feed serving, graph partitions, CDN edge |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- User can create posts with text, images, videos, or carousel media.
- User can follow/unfollow another user, page, group, or creator.
- User can fetch a personalized home feed.
- User can fetch a user's profile timeline.
- Feed should support pagination for infinite scroll.
- Feed should show ranked content from followed accounts and optionally recommended content.
- User can like, comment, share, save, hide, mute, or report a feed item.

Optional requirements to clarify:

- Is the feed only from followed accounts, or can it include recommendations?
- Are stories, reels, marketplace, groups, and ads included?
- Is ranking ML-based or rule-based for the interview?
- Should profile timeline be strictly chronological?
- Should private accounts and blocked users be supported?
- Should feed update in real time or only on refresh?

Out of scope unless asked:

- Full ad auction design.
- Full ML training platform.
- Full media upload/transcoding system, except the pieces required for feed rendering.
- Moderation reviewer tooling UX.

## 1.2 Non-Functional Requirements

Read path:

- Low latency for `GET /feed/home`, especially first page.
- High availability because feed is the main product surface.
- Stable pagination without duplicate or missing items during scrolling.
- Graceful degraded feed if ranking or feature store is unhealthy.

Write path:

- Durable post creation before feed fanout.
- Asynchronous fanout so posting does not block on follower count.
- Idempotent event processing to handle retries safely.

Correctness path:

- Visibility, privacy, block, mute, and deletion rules must be enforced at read time.
- Engagement counters can be eventually consistent.
- Ranking can be eventually consistent, but should not show forbidden content.

## 1.3 Constraints

- Follower counts are highly skewed; celebrities can have hundreds of millions of followers.
- Feed reads are much more frequent than post writes.
- Media bytes are large, so feed service should return metadata and CDN URLs, not raw bytes.
- Ranking needs many signals but has a strict latency budget.
- Cached feed items can become stale after deletes, privacy changes, or blocks.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Registered users | 3 billion |
| Daily active users | 1 billion |
| Posts/day | 800 million |
| Feed reads/day | 500 billion item reads |
| Average follows/user | 400 |
| Hot creator followers | 100M+ |
| Feed page size | 20 items |
| First page p99 target | under 200 ms |

Back-of-the-envelope:

- `800M posts/day` is about `9,260 posts/sec` on average, with peaks much higher.
- If a normal user has 400 followers, fanout-on-write creates about 400 inbox writes per post.
- If a celebrity has 100M followers, fanout-on-write for one post is not acceptable synchronously.
- First-page feed should usually come from cache or a precomputed candidate window.

## 1.5 Clarifying Questions To Ask

- Should the feed be chronological, ranked, or hybrid?
- Are recommendations allowed in the home feed?
- What is the freshness expectation: instant, seconds, or minutes?
- Do private accounts require approval before feed visibility?
- Are ads in scope?
- How far back should the home feed store candidates?
- What is more important: freshness, ranking quality, or latency?

Strong interview framing:

> I will design a hybrid social feed: posts are written durably, normal authors are faned out asynchronously to follower inboxes, huge creators are merged at read time, ranking orders candidates under a latency budget, and read-time visibility checks protect privacy even if caches are stale.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Post create flow:
Client -> API Gateway -> Post Service -> Post Store
       -> Media Metadata Store
       -> Event Stream: post.created
       -> Fanout Workers -> Home Feed Store / Inbox Store
       -> Cache invalidation

Feed read flow:
Client -> API Gateway -> Feed Query Service
       -> Feed Cache / Feed Store / Pull Candidate Source
       -> Ranking Service -> Hydration Service
       -> Visibility Filter -> Response with media CDN URLs and cursor
```

Recommended architecture:

```text
                         +-----------------------+
                         | Ranking / Feature API |
                         +-----------+-----------+
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
       |                               |
       v                               v
+--------------+             +---------------------+
| Post Service |             | Hydration Service   |
+------+-------+             +----------+----------+
       |                                |
       v                                v
+---------------+             +---------------------+
| Post Store    |             | Post/User/Media DB  |
+------+--------+             +----------+----------+
       |                                |
       v                                v
+---------------+             +---------------------+
| Event Stream  |-----------> | Fanout Workers      |
+---------------+             +---------------------+
       |
       v
+---------------+
| Media Pipeline|
| CDN / Object  |
+---------------+
```

## 2.2 APIs

### Create Post

```http
POST /v1/posts
Authorization: Bearer <token>
Idempotency-Key: post-create-123
Content-Type: application/json

{
  "text": "Weekend hike",
  "mediaAssetIds": ["m_101", "m_102"],
  "visibility": "FOLLOWERS"
}
```

Response:

```json
{
  "postId": "p_5001",
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
      "feedItemId": "fi_1",
      "postId": "p_5001",
      "authorId": "u_10",
      "score": 0.94,
      "media": [{"type": "IMAGE", "cdnUrl": "https://cdn.example/m_101"}],
      "viewerState": {"liked": false, "saved": false}
    }
  ],
  "nextCursor": "def"
}
```

### Record Engagement

```http
POST /v1/feed/items/{feedItemId}/engagement
Authorization: Bearer <token>
Content-Type: application/json

{
  "action": "LIKE",
  "postId": "p_5001"
}
```

### Hide Or Mute

```http
POST /v1/feed/preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "action": "MUTE_AUTHOR",
  "authorId": "u_10"
}
```

## 2.3 Core Components

Think of this system as five cooperating planes:

| Plane | Owns | Main goal |
|---|---|---|
| Write plane | post creation, media metadata, durable events | never lose content |
| Fanout plane | follower lookup, inbox writes, backpressure | prepare feed candidates cheaply |
| Read plane | candidate fetch, pagination, hydration | return fast pages |
| Ranking plane | feature reads, scoring, reordering | improve relevance within latency budget |
| Governance plane | privacy, deletes, blocks, moderation | prevent forbidden content from rendering |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| API Gateway | auth, rate limits, routing, max page sizes | feed ranking logic | QPS and bot traffic |
| Post Service | canonical post lifecycle | follower fanout loops | write QPS |
| Media Service | media metadata, upload completion, CDN URLs | feed ranking | object size and CDN traffic |
| Follow Graph Service | follower/following edges | post content | graph read volume |
| Fanout Workers | write post references into inboxes | final ranking page | event volume and follower skew |
| Feed Query Service | candidate fetch, filtering, pagination | model training | read QPS |
| Ranking Service | score candidates | canonical post writes | feature latency |
| Hydration Service | batch post/user/media enrichment | candidate source | batch read pressure |
| Feed Store | home timeline candidate windows | full post body | storage amplification |
| Visibility Service | privacy, block, mute, delete checks | feed storage | correctness under stale caches |

### Feed Generation Strategy

| Strategy | How it works | Best for | Weakness |
|---|---|---|---|
| Push / fanout-on-write | write post ID into follower inboxes when post is created | normal users, fast reads | terrible for huge creators |
| Pull / fanout-on-read | read followed authors' posts during feed request | celebrities, fresh merge | expensive if user follows many accounts |
| Hybrid | push normal authors, pull huge creators and recommendations | most real feeds | more complex ranking and merge logic |

Recommended choice:

- Use push fanout for normal accounts.
- Use pull or partial fanout for celebrities/pages with huge follower counts.
- Store a bounded home feed candidate window per user.
- Always enforce visibility at read time.

### Ranking System

Ranking pipeline:

1. Collect candidates from inbox, recent followed creators, recommendations, and ads if in scope.
2. Remove blocked, muted, deleted, private, and unsafe content.
3. Fetch features: author affinity, recency, media type, engagement velocity, dwell time, negative feedback.
4. Score candidates using ranking model or weighted heuristic.
5. Blend content types to avoid one author/type dominating.
6. Return ranked page with cursor.

Beginner mental model:

> Fanout decides what could appear. Ranking decides what should appear first. Visibility decides what must never appear.

### Timeline Storage

| Timeline | Stores | Access pattern |
|---|---|---|
| Author timeline | posts by one author | profile page, pull merge for celebrity authors |
| Home inbox timeline | candidate post IDs for one viewer | fast home feed reads |
| Media timeline metadata | media asset state and CDN references | hydration |
| Engagement timeline | likes/comments/shares events | ranking features and counters |

Storage rule:

- Do not duplicate full post bodies into every feed inbox.
- Store compact references: `(viewer_id, post_id, author_id, created_at, fanout_source, base_score)`.
- Hydrate the latest post/media/user state at read time.

## 2.4 Data Model

```sql
posts(
  post_id primary key,
  author_id,
  text,
  visibility,
  status,
  created_at,
  updated_at
)

media_assets(
  media_id primary key,
  owner_id,
  post_id,
  media_type,
  object_key,
  cdn_url,
  processing_status
)

follow_edges(
  follower_id,
  followed_id,
  status,
  created_at,
  primary key (follower_id, followed_id)
)

home_feed_items(
  viewer_id,
  sort_bucket,
  post_id,
  author_id,
  created_at,
  fanout_source,
  base_score,
  primary key (viewer_id, sort_bucket, created_at, post_id)
)

engagement_events(
  event_id primary key,
  viewer_id,
  post_id,
  action,
  created_at
)
```

Partitioning:

- `posts`: partition by `author_id` or `post_id` depending on access pattern.
- `follow_edges`: maintain both forward and reverse indexes.
- `home_feed_items`: partition by `viewer_id`.
- `engagement_events`: partition by `post_id` and time for aggregation.

## 2.5 Caching Strategy

| Cache | Key | Value | TTL |
|---|---|---|---|
| CDN media cache | media URL | image/video bytes | long, versioned |
| Feed first page cache | `feed:user_id:first_page` | ranked item IDs | short |
| Candidate window cache | `candidate:user_id` | recent post IDs | minutes |
| Hydration cache | `post:post_id`, `user:user_id` | metadata | minutes |
| Engagement counter cache | `count:post_id` | likes/comments | seconds to minutes |
| Follow graph cache | `following:user_id` | followed IDs | short and invalidated |

Important cache rules:

- Cache media aggressively because assets are immutable or versioned.
- Cache feed pages briefly because ranking and visibility can change.
- Cache candidate IDs more safely than fully hydrated items.
- Apply read-time visibility filters after cache reads.

## 2.6 Consistency Model

| Data | Consistency target | Why |
|---|---|---|
| Post creation | strong for author write response | user must not lose post |
| Media upload completion | strong enough before publish | avoid broken feed media |
| Home feed delivery | eventual | fanout can lag |
| Likes/comments counts | eventual | counters can be delayed |
| Delete/privacy/block | read-time strong enforcement | forbidden content must not render |
| Ranking order | eventual | quality can improve over time |

Key rule:

> Feed storage is a cache of candidates, not the final source of truth for visibility.

## 2.7 Failure Modes

| Failure | User impact | Mitigation |
|---|---|---|
| Fanout backlog | followers see post late | durable event log, lag monitoring, priority lanes |
| Ranking service down | less personalized feed | fallback chronological/cached ranking |
| Feature store slow | higher latency | time budget, partial features, defaults |
| Media CDN miss | slow image/video load | multi-CDN, prewarming for hot media |
| Post deleted after cache | stale candidate | read-time status check filters it |
| Celebrity post storm | worker overload | hybrid pull, rate-limited fanout, batch writes |

---

# 3. Low-Level Design

LLD goal:

> Model a feed as durable posts plus lightweight candidate references, then keep fanout, ranking, hydration, and visibility as separate components.

Simple rule:

- Domain objects represent content and relationships.
- Services coordinate workflows.
- Strategies decide ranking and fanout behavior.
- Repositories hide storage and cache details.

Starter map:

| LLD question | Instagram / Facebook answer |
|---|---|
| Canonical content object | `Post` |
| Media object | `MediaAsset` |
| Relationship object | `FollowEdge` |
| Home feed candidate | `FeedItem` |
| Write-side workflow | `PostService.createPost()` then `FanoutService.distribute()` |
| Read-side workflow | `FeedQueryService.getHomeFeed()` |
| Runtime strategy | `FanoutStrategy`, `RankingStrategy`, `VisibilityPolicy` |
| Atomic operation | create post and emit event through outbox/event log |

Beginner-friendly design order:

1. Model `Post` and `MediaAsset` as canonical data.
2. Model `FollowEdge` so fanout can find recipients.
3. Model `FeedItem` as a reference to a post, not as a full copy.
4. Design `PostService` for durable post creation.
5. Design `FanoutService` for asynchronous candidate distribution.
6. Design `FeedQueryService` to fetch, filter, rank, hydrate, and paginate.
7. Add `VisibilityPolicy` so cached/stale candidates do not leak private content.

Interview sentence:

> In LLD, I will keep posts and media canonical, store only lightweight feed candidates per viewer, fan out asynchronously for normal creators, pull huge creators at read time, and always apply visibility checks before returning a ranked page.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `User` | identity, account state, preferences | blocked/muted/private rules must be respected |
| `Post` | canonical content metadata | only published posts can render |
| `MediaAsset` | image/video metadata and CDN location | feed renders only processed media |
| `FollowEdge` | relationship from follower to followed | inactive edge should not create candidates |
| `FeedItem` | viewer-specific candidate reference | does not own full post body |
| `FeedCursor` | pagination position | must be opaque and tamper-resistant |
| `EngagementEvent` | like/comment/share/save/hide signal | idempotent per action request |

Core services:

| Service | Responsibility |
|---|---|
| `PostService` | create, edit, delete, publish post events |
| `MediaService` | validate processed media and provide CDN URLs |
| `FollowService` | maintain follow graph |
| `FanoutService` | distribute post references to recipients |
| `FeedQueryService` | assemble feed page |
| `RankingService` | score candidates |
| `HydrationService` | batch fetch post, media, author, viewer state |
| `VisibilityService` | enforce privacy/block/mute/delete/moderation |

## 3.2 Class Sketch

```java
interface FanoutStrategy {
    FanoutPlan plan(Post post, AuthorStats stats);
}

interface RankingStrategy {
    List<ScoredFeedItem> rank(String viewerId, List<FeedItem> candidates, RankingContext context);
}

interface VisibilityPolicy {
    boolean canView(String viewerId, Post post, User author);
}

final class FeedQueryService {
    private final FeedRepository feedRepository;
    private final CandidateSource celebritySource;
    private final RankingStrategy rankingStrategy;
    private final HydrationService hydrationService;
    private final VisibilityPolicy visibilityPolicy;
}
```

## 3.3 Sequence Diagram

```text
Client -> FeedQueryService: getHomeFeed(viewerId, cursor)
FeedQueryService -> FeedRepository: getInboxCandidates(viewerId, cursor)
FeedQueryService -> CelebrityCandidateSource: pullRecent(viewerId)
FeedQueryService -> HydrationService: batchHydrate(postIds)
FeedQueryService -> VisibilityService: filter(viewerId, hydratedPosts)
FeedQueryService -> RankingService: rank(viewerId, visibleCandidates)
FeedQueryService --> Client: ranked items + next cursor
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | ranking and fanout policies |
| Observer/Event | post-created event triggers fanout |
| Factory | choose ranking strategy by feed type |
| Adapter | hide Redis/Cassandra/Elastic clients behind repositories |
| Decorator | add cache around repository calls |
| State | post lifecycle: draft, processing, published, deleted |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| user follows nobody | show onboarding recommendations or empty feed |
| media processing not complete | keep post unpublished or hide media preview |
| duplicate post create retry | use idempotency key |
| duplicate fanout event | idempotent insert by `(viewerId, postId)` |
| author goes private | read-time visibility check filters old candidates |
| viewer blocks author | filter during feed read and invalidate cache |
| deleted post still in inbox | tombstone check during hydration |
| ranking timeout | fallback to chronological cached candidates |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
socialfeed/
  domain/
    Post.java
    MediaAsset.java
    FollowEdge.java
    FeedItem.java
    FeedCursor.java
  service/
    PostService.java
    FanoutService.java
    FeedQueryService.java
    VisibilityService.java
  ranking/
    RankingStrategy.java
    ChronologicalRankingStrategy.java
    WeightedRankingStrategy.java
  repository/
    PostRepository.java
    FeedRepository.java
    FollowGraphRepository.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import defaultdict, deque
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class Post:
    post_id: str
    author_id: str
    text: str
    created_at: float
    status: str = "PUBLISHED"


class SocialFeed:
    def __init__(self) -> None:
        self.followers: dict[str, set[str]] = defaultdict(set)
        self.posts: dict[str, Post] = {}
        self.inbox: dict[str, deque[str]] = defaultdict(deque)
        self.blocked: set[tuple[str, str]] = set()

    def follow(self, follower_id: str, author_id: str) -> None:
        self.followers[author_id].add(follower_id)

    def block(self, viewer_id: str, author_id: str) -> None:
        self.blocked.add((viewer_id, author_id))

    def create_post(self, author_id: str, text: str) -> Post:
        post = Post(f"p_{len(self.posts) + 1}", author_id, text, time())
        self.posts[post.post_id] = post
        for follower_id in self.followers[author_id]:
            self.inbox[follower_id].appendleft(post.post_id)
        self.inbox[author_id].appendleft(post.post_id)
        return post

    def get_feed(self, viewer_id: str, limit: int = 20) -> list[Post]:
        result: list[Post] = []
        for post_id in self.inbox[viewer_id]:
            post = self.posts.get(post_id)
            if not post or post.status != "PUBLISHED":
                continue
            if (viewer_id, post.author_id) in self.blocked:
                continue
            result.append(post)
            if len(result) == limit:
                break
        return result


feed = SocialFeed()
feed.follow("u2", "u1")
feed.create_post("u1", "new photo")
print([post.text for post in feed.get_feed("u2")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| author to followers | `Map<AuthorId, Set<UserId>>` |
| user inbox | `Map<UserId, Deque<PostId>>` |
| canonical posts | `Map<PostId, Post>` |
| visibility blocks | `Set<(viewerId, authorId)>` |
| ranking candidates | bounded list or priority queue |
| fanout events | queue or stream |

## 4.4 Concurrency

Risks:

- Duplicate inbox entries after worker retry.
- Post delete racing with feed read.
- Follow/unfollow racing with fanout.
- Engagement counter increments lost under high write rate.

Mitigations:

- Idempotent feed item insert keyed by `(viewer_id, post_id)`.
- Soft delete posts and filter by status.
- Read-time visibility check as final gate.
- Atomic counters or sharded counters for engagement.

## 4.5 Performance Optimization

- Keep feed item small.
- Cap per-user inbox window.
- Batch hydrate posts and authors.
- Cache first page and candidate windows.
- Use hybrid fanout for high-follower authors.
- Use CDN for all media bytes.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Viral Creator Post

Problem:

- A celebrity post can create huge fanout pressure, notification pressure, media CDN pressure, and engagement write pressure.

Handling:

- Classify author as high-fanout.
- Avoid full synchronous push to all followers.
- Put post into celebrity pull source.
- Prewarm CDN for media.
- Rate-limit engagement writes and aggregate counters asynchronously.
- Prioritize active followers before cold followers if partial fanout is used.

## 5.2 Read Storm

Problem:

- App open, outage recovery, or viral content can cause first-page feed QPS spike.

Handling:

- Serve cached first page where possible.
- Reduce ranking feature budget.
- Return smaller page sizes temporarily.
- Degrade to chronological feed from candidate window.
- Use request coalescing for cache misses.

## 5.3 Engagement Storm

Problem:

- Likes/comments/shares on one post can overload counters and ranking features.

Handling:

- Write engagement events to stream.
- Use sharded counters.
- Update visible counts eventually.
- Feed ranking consumes aggregated velocity features with delay.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| posts | `author_id` or `post_id` | author timeline and post lookup patterns differ |
| home feed items | `viewer_id` | feed reads are per viewer |
| follow graph forward | `follower_id` | get following list |
| follow graph reverse | `followed_id` | fanout recipient batches |
| engagement events | `post_id + time` | aggregation and hot-post handling |

## 6.2 Multi-Region Design

- Route users to nearest home region for feed reads.
- Store user feed inbox in home region.
- Replicate public posts and media metadata across regions.
- Use CDN for media globally.
- Accept eventual cross-region feed freshness for most social content.
- Keep privacy/block decisions available in read region.

## 6.3 Storage Economics

Storage can explode if every post is copied to every follower.

Controls:

- Store post references, not post bodies.
- Cap home feed inbox to recent candidate window.
- Use TTL or compaction for old feed items.
- Pull high-fanout authors at read time.
- Archive old engagement events into analytics storage.

## 6.4 Observability

Track:

- Feed first-page p50/p95/p99 latency.
- Feed cache hit rate.
- Fanout lag by author tier.
- Ranking timeout rate.
- Hydration batch latency.
- Visibility filter drop rate.
- CDN media error rate.
- Duplicate feed item rate.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify feed type: following-only, ranked, recommendations, ads.
2. Estimate read/write ratio and follower skew.
3. Start with post store, follow graph, and simple chronological feed.
4. Add fanout-on-write for normal authors.
5. Add pull path for celebrities.
6. Add ranking, hydration, and visibility filtering.
7. Add caching, timeline storage, and consistency rules.
8. Discuss spikes and billion-user scaling.

## 7.2 Strong Trade-Off Statements

- Push fanout makes reads fast but writes expensive.
- Pull fanout keeps writes cheap but makes reads expensive.
- Hybrid fanout is the practical choice for skewed social graphs.
- Feed inbox should store candidate references, not full content.
- Ranking quality must fit latency budget.
- Read-time visibility is mandatory because feed caches can be stale.

## 7.3 Common Mistakes

| Mistake | Better answer |
|---|---|
| synchronously fan out every post | asynchronous hybrid fanout |
| store full post body in every feed row | store compact post references |
| trust cache for visibility | re-check privacy/block/delete at read time |
| rank all historical posts | rank bounded candidate windows |
| ignore media delivery | use object storage and CDN |

---

# 8. Fast Recall Rules

- Social feed = post store + graph + fanout + feed store + ranking + hydration + visibility.
- Push fanout is good for normal users.
- Pull fanout is good for celebrities and recommendations.
- Hybrid fanout is the real answer.
- Timeline storage should hold post IDs, not full posts.
- Ranking decides order, not eligibility.
- Visibility filtering is the final safety gate.
- CDN handles media bytes; feed service handles metadata.
- Engagement counters are eventually consistent.
- Degraded mode can serve chronological cached candidates.