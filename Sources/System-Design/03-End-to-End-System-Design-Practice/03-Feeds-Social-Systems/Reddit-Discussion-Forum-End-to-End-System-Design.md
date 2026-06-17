# Reddit / Discussion Forum - End-to-End System Design

> Goal: design a discussion-forum feed with communities, posts, vote-based ranking, comment trees, caching, consistency, fanout/pull trade-offs, and timeline storage.

---

## How To Use This File

- Use this when the problem says Reddit, discussion forum, community feed, subreddit feed, Hacker News-like ranking, or threaded comments.
- Reddit is not a pure social graph feed. The main feed sources are communities, subscriptions, votes, comments, recency, moderation, and recommendations.
- Focus on ranking modes such as hot, new, top, rising, and personalized home.
- Keep post feed, comment tree, search, moderation tools, chat, and notifications separate unless asked.

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

| Layer | Interview signal | Reddit/forum focus |
|---|---|---|
| Problem understanding | Can clarify community/feed scope | subreddits, home feed, voting, comments, moderation |
| HLD | Can design ranked community feeds | post service, community service, vote service, ranking, cache, comment tree |
| LLD | Can model forum domain | `Post`, `Community`, `Vote`, `Comment`, `FeedSortStrategy` |
| Machine coding | Can implement core path | create post, subscribe, vote, rank feed, paginate comments |
| Traffic spikes | Can handle viral threads | hot posts, comment storms, vote storms, cache stampede |
| Billion users | Can scale globally | partitioned communities, ranking caches, vote aggregation, CDN, moderation pipeline |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- User can create a post in a community/subreddit.
- User can subscribe/unsubscribe to communities.
- User can fetch home feed from subscribed communities.
- User can fetch a community feed.
- Feed supports sort modes: hot, new, top, rising, controversial if in scope.
- User can upvote/downvote posts and comments.
- User can comment and reply in a nested thread.
- Moderators can remove posts/comments or lock threads.
- Feed supports cursor-based pagination.

Optional requirements to clarify:

- Is feed personalized or only community-based?
- Are anonymous/guest reads supported?
- How deep can comment nesting go?
- Are awards, flair, crossposts, saved posts, and reports in scope?
- Should votes be visible immediately?
- Is real-time comment updating required?

Out of scope unless asked:

- Full search indexing.
- Full moderation dashboard.
- Full recommendation ML system.
- Chat/direct messaging.
- Full anti-abuse system internals.

## 1.2 Non-Functional Requirements

Feed reads:

- Low latency for home feed and community feed.
- High availability because public reads can be very large.
- Support multiple sort orders efficiently.
- Cache hot community feeds and hot posts.

Writes and aggregation:

- Durable post/comment creation.
- Vote writes should be idempotent per user and target.
- Vote score aggregation can be eventually consistent.
- Ranking should refresh periodically or incrementally.

Moderation and correctness:

- Removed/deleted/locked/private-community content must not render to ineligible users.
- User vote state should be accurate for the viewer.
- Public counts can be eventually consistent.
- Comment tree should handle deleted parents gracefully.

## 1.3 Constraints

- Reddit is community-centric, not mostly follower-centric.
- Some communities and threads become extremely hot.
- A hot post can receive thousands of comments and votes per second.
- Ranking depends on time decay and vote/comment velocity.
- Comment trees can be very large and deeply nested.
- Vote counts are hot keys and should not be updated as one global row without protection.
- Moderation state can change after a post was cached.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| Daily active users | 200 million |
| Communities | 10 million |
| Posts/day | 50 million |
| Comments/day | 500 million |
| Votes/day | 5 billion |
| Feed reads/day | 100 billion item reads |
| Hot community QPS | millions during major events |
| Feed page size | 25 posts |

Back-of-the-envelope:

- Votes dominate write volume, so vote ingestion and aggregation need event streams/sharded counters.
- Community feeds can be precomputed per sort mode for hot communities.
- Home feed for subscribed communities is often a pull/merge of community-ranked feeds, not a pure per-user fanout.
- Hot comments need pagination and ranking, not full thread loading at once.

## 1.5 Clarifying Questions To Ask

- Are we designing home feed, subreddit feed, or both?
- Which sort modes are required?
- Should home feed be personalized or simply merged from subscriptions?
- Are nested comments in scope?
- Are private/restricted communities required?
- How fresh should votes and ranking be?
- What fallback is acceptable if ranking aggregation is delayed?

Strong interview framing:

> I will design Reddit as a community-centric ranking system: posts and comments are canonical, votes are ingested idempotently and aggregated asynchronously, community feeds are ranked by sort strategy and cached, home feed pulls/merges subscribed community candidates, and moderation/visibility filters run before rendering.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Create post flow:
Client -> API Gateway -> Post Service -> Post Store
       -> Event Stream: post.created
       -> Community Feed Index / Ranking Queue
       -> Cache invalidation

Vote flow:
Client -> API Gateway -> Vote Service -> Vote Store
       -> Event Stream: vote.changed
       -> Vote Aggregator -> Score Store / Ranking Features
       -> Feed Rank Refresh

Feed read flow:
Client -> API Gateway -> Feed Service
       -> Feed Cache / Community Feed Index
       -> Home Merge Service for subscriptions
       -> Moderation/Visibility Filter
       -> Hydration Service
       -> Response with cursor

Comment flow:
Client -> API Gateway -> Comment Service -> Comment Store
       -> Event Stream: comment.created
       -> Comment Count Aggregator / Thread Cache Invalidation
```

Recommended architecture:

```text
                         +----------------------+
                         | Feed Ranking Service |
                         +----------+-----------+
                                    |
Client                              v
  |                       +----------------------+
  v                       | Feed Service         |
+-------------+           +----------+-----------+
| API Gateway |                      |
+------+------+                      v
       |                   +----------------------+
       |                   | Feed Cache / Index   |
       |                   +----------+-----------+
       v                              |
+--------------+                      v
| Post Service |            +---------------------+
+------+-------+            | Hydration Service   |
       |                    +----------+----------+
       v                               |
+--------------+                      v
| Post Store   |            +---------------------+
+------+-------+            | Moderation Filter   |
       |                    +---------------------+
       v
+--------------+            +---------------------+
| Event Stream |<-----------| Vote Service        |
+------+-------+            +---------------------+
       |
       v
+--------------+            +---------------------+
| Aggregators  |----------->| Score / Feature     |
| votes/comments|           | Stores              |
+--------------+            +---------------------+
```

## 2.2 APIs

### Create Post

```http
POST /v1/communities/{communityId}/posts
Authorization: Bearer <token>
Idempotency-Key: post-123
Content-Type: application/json

{
  "title": "How would you design a ranking cache?",
  "body": "Looking for trade-offs between freshness and latency.",
  "postType": "TEXT",
  "tags": ["system-design"]
}
```

Response:

```json
{
  "postId": "p_9001",
  "communityId": "c_systemdesign",
  "authorId": "u_42",
  "status": "PUBLISHED",
  "createdAt": "2026-06-17T10:00:00Z"
}
```

### Get Community Feed

```http
GET /v1/communities/{communityId}/feed?sort=hot&cursor=abc&limit=25
Authorization: Bearer <token>
```

Response:

```json
{
  "items": [
    {
      "postId": "p_9001",
      "title": "How would you design a ranking cache?",
      "score": 382,
      "commentCount": 47,
      "rankScore": 0.93,
      "viewerVote": "UP"
    }
  ],
  "nextCursor": "def"
}
```

### Vote On Post

```http
PUT /v1/posts/{postId}/vote
Authorization: Bearer <token>
Content-Type: application/json

{
  "vote": "UP"
}
```

### Create Comment

```http
POST /v1/posts/{postId}/comments
Authorization: Bearer <token>
Idempotency-Key: comment-123
Content-Type: application/json

{
  "parentCommentId": "comment_55",
  "body": "One trick is separating candidate cache from hydration cache."
}
```

## 2.3 Core Components

Reddit has two ranking layers:

| Layer | What is ranked | Examples |
|---|---|---|
| Post feed ranking | posts inside a community or home feed | hot, new, top, rising |
| Comment ranking | comments inside a post thread | best, top, new, controversial |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| API Gateway | auth, rate limits, request validation | ranking formulas | QPS and abuse |
| Community Service | community metadata, membership, rules | post body | community count |
| Post Service | canonical post lifecycle | vote aggregation | post write QPS |
| Comment Service | comment tree writes/reads | post feed ranking | hot thread reads/writes |
| Vote Service | idempotent user votes | final feed ranking alone | vote write QPS |
| Vote Aggregator | scores and velocity features | raw post content | hot counters |
| Feed Ranking Service | hot/new/top/rising lists | vote write truth | ranking refresh volume |
| Feed Service | read/merge/paginate feeds | canonical moderation decisions | read QPS |
| Home Merge Service | merge subscribed community feeds | community ranking formulas | subscription fan-in |
| Moderation Filter | removed/private/locked/NSFW checks | ranking order | correctness |
| Hydration Service | post/comment/user/community previews | candidate source | batch read QPS |
| Cache Layer | hot feed pages, post previews, comment pages | canonical truth | cache stampede |

### Feed Generation Strategy

Reddit is usually pull/merge and ranking-cache heavy:

| Feed | Generation style | Notes |
|---|---|---|
| community hot feed | precomputed ranked list | cache by community and sort |
| community new feed | append by created time | simple time index |
| community top feed | score/time-window index | daily/weekly/all-time variants |
| home feed | merge subscribed community ranked lists | user-specific subscription fan-in |
| popular/all feed | global/regional ranked list | heavily cached |

Push fanout is less central than in Instagram/Twitter because users subscribe to communities and pull ranked lists. However, for active users or notification-like home feed caching, the system may precompute recent home candidates.

### Ranking Algorithms

Common sort modes:

| Sort | Inputs | Mental model |
|---|---|---|
| new | creation time | newest first |
| top | net/upvote score within time window | highest voted |
| hot | score plus time decay | good and recent |
| rising | recent vote/comment velocity | gaining momentum |
| controversial | many upvotes and downvotes | disagreement |
| best comments | score, confidence, author/trust signals | useful discussion first |

Simple hot score intuition:

```text
hot_score = log10(max(abs(upvotes - downvotes), 1))
            + sign(upvotes - downvotes) * age_factor
            + comment_velocity_boost
            - moderation_penalty
```

The exact formula can vary; the interview focus is trade-off reasoning:

- top can get stale,
- new can be low quality,
- hot balances freshness and quality,
- rising finds momentum,
- personalized home blends community rankings with viewer interests.

### Vote And Score Aggregation

Vote write path:

1. User sends vote change.
2. Vote Service validates user/community/post eligibility.
3. Vote Store upserts `(userId, targetId) -> vote`.
4. Vote event is published with old vote and new vote.
5. Aggregator updates sharded counters.
6. Ranking features are refreshed.
7. Feed caches are invalidated or lazily refreshed.

Vote consistency rule:

> The viewer's own vote state should be correct, while public aggregate score can lag slightly.

### Comment Tree Storage

Comment tree row:

```text
post_id
comment_id
parent_comment_id
path_or_depth
author_id
created_at
status
score
```

Common approaches:

- adjacency list: simple parent pointer, needs recursive/tree fetch,
- materialized path: easy subtree reads, harder moves,
- nested set: efficient subtree but complex writes,
- hybrid: parent pointer plus sorted child indexes.

Recommended interview choice:

- Store parent pointer and maintain child indexes per parent.
- Paginate top-level comments and child replies.
- Cache hot comment pages.
- Keep deleted parent placeholders if replies remain.

### Timeline / Feed Storage

| Store | Key | Value | Purpose |
|---|---|---|---|
| community post index | `(communityId, createdAt DESC)` | post IDs | new feed |
| community hot index | `(communityId, hotScore DESC)` | post IDs | hot feed |
| community top index | `(communityId, timeWindow, score DESC)` | post IDs | top feed |
| home candidate cache | `(userId, sort, cursor)` | merged post IDs | personalized home |
| comment child index | `(postId, parentCommentId, rank)` | child comment IDs | comment pagination |
| vote store | `(userId, targetId)` | current vote | idempotent votes |

### Caching Strategy

| Cache | Key | Value | TTL |
|---|---|---|---|
| hot community feed | `(communityId, sort, page)` | post IDs | seconds to minutes |
| popular/all feed | `(region, sort, page)` | post IDs | seconds |
| home feed page | `(userId, sort, cursor)` | merged post IDs | short |
| post preview | `post:{postId}` | title/body preview/counts | minutes |
| comment page | `(postId, parentId, sort, page)` | comment IDs | seconds to minutes |
| vote state | `(userId, postId)` | viewer vote | short/read-through |
| community metadata | `community:{communityId}` | rules/name/status | minutes |

Cache safety rule:

> Cached ranked lists are candidate lists. Removed/private/blocked/NSFW content still needs a final eligibility check.

### Consistency Model

| Data | Consistency target | Why |
|---|---|---|
| post creation | strong canonical write | avoid lost posts |
| comment creation | strong canonical write | discussion correctness |
| viewer's own vote | read-your-write | user expects vote state accuracy |
| public vote score | eventual | hot counters need async aggregation |
| feed ranking | eventual | ranked lists can refresh periodically |
| moderation removal | read-time enforced | removed content must not render |
| comment count | eventual | high write volume |

One-stop interview answer:

> Reddit is a community-centric ranking system. I store posts/comments canonically, upsert votes idempotently, aggregate scores asynchronously, maintain ranked community indexes for hot/new/top/rising, merge subscribed community candidates for home feed, cache hot ranked lists, and apply moderation/visibility filters before hydration.

---

# 3. Low-Level Design

LLD goal:

> Model communities, posts, votes, comments, ranking strategies, feed sources, and moderation filters as separate components.

Simple rule:

- `Post` is discussion starter truth.
- `Comment` forms a tree.
- `Vote` is one user's current vote for one target.
- `ScoreAggregate` powers ranking.
- `FeedSortStrategy` chooses ordering.
- `ModerationPolicy` decides visibility.

Starter map:

| LLD question | Reddit/forum answer |
|---|---|
| Main content object | `Post` |
| Community object | `Community` |
| Thread object | `Comment` with parent-child relation |
| Vote object | `Vote` keyed by `(userId, targetId)` |
| Ranking abstraction | `FeedSortStrategy` |
| Feed source | `CommunityFeedSource`, `HomeFeedSource` |
| Correctness gate | `ModerationPolicy`, `VisibilityPolicy` |
| Async aggregation | `VoteAggregator`, `CommentCountAggregator` |

Beginner-friendly design order:

1. Model `Community` and `Post`.
2. Model `Comment` as a tree using `parentCommentId`.
3. Model `Vote` as current user intent, not just a counter increment.
4. Model `ScoreAggregate` so public score can be updated asynchronously.
5. Add `FeedSortStrategy` for hot, new, top, and rising.
6. Build `CommunityFeedService` for community pages.
7. Build `HomeFeedService` to merge subscribed communities.
8. Add moderation and visibility filters before response.

Interview sentence:

> In LLD, I will separate posts, comments, votes, score aggregates, ranking strategies, and moderation filters so votes can be written safely and feeds can be ranked/cached without losing visibility correctness.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `Community` | metadata, rules, privacy, moderators | private communities require membership |
| `Post` | canonical discussion post | removed/deleted posts should not render normally |
| `Comment` | comment body and parent relation | replies can survive deleted parent placeholder |
| `Vote` | user vote on post/comment | one active vote per `(userId, targetId)` |
| `ScoreAggregate` | up/down/net/velocity fields | can lag raw votes |
| `FeedCursor` | pagination state | opaque and sort-specific |
| `FeedSortStrategy` | ranking behavior | strategies must produce deterministic order |
| `ModerationPolicy` | visibility and status checks | final gate before response |

Core services:

| Service | Responsibility |
|---|---|
| `CommunityService` | community metadata and subscriptions |
| `PostService` | create/delete/moderate posts |
| `CommentService` | create/read comment trees |
| `VoteService` | validate and upsert votes |
| `VoteAggregator` | update score aggregates |
| `FeedRankingService` | maintain ranked lists |
| `FeedService` | fetch/merge/filter/hydrate feeds |
| `ModerationService` | enforce removal, lock, NSFW, private rules |

## 3.2 Class Sketch

```java
interface FeedSortStrategy {
    List<PostCandidate> sort(List<PostCandidate> candidates, SortContext context);
}

interface VoteRepository {
    Vote upsertVote(String userId, String targetId, VoteValue newValue);
    Vote getVote(String userId, String targetId);
}

interface ModerationPolicy {
    boolean canShowPost(String viewerId, Post post, Community community);
    boolean canShowComment(String viewerId, Comment comment, Post post);
}

interface CommentRepository {
    List<Comment> getChildren(String postId, String parentCommentId, CommentCursor cursor, int limit);
}
```

## 3.3 Sequence Diagram

```text
Client -> FeedService: getCommunityFeed(communityId, sort, cursor)
FeedService -> FeedIndex: getRankedPostIds(communityId, sort, cursor)
FeedService -> PostRepository: batchGet(postIds)
FeedService -> ModerationService: filter(posts)
FeedService -> VoteRepository: batchGetViewerVotes(userId, postIds)
FeedService -> HydrationService: attach counts/community/user previews
FeedService --> Client: posts + nextCursor

Client -> VoteService: vote(userId, postId, UP)
VoteService -> VoteRepository: upsert current vote
VoteService -> EventStream: publish vote.changed
VoteAggregator -> ScoreStore: update aggregate
RankingService -> FeedIndex: refresh affected ranking entries
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | hot/new/top/rising sort modes |
| Observer/Event | vote/comment events update aggregates |
| Chain of Responsibility | moderation and visibility filters |
| Adapter | cache/feed-index/store clients |
| Decorator | cache-backed feed repositories |
| State | post/comment lifecycle: published, removed, deleted, locked |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| user changes upvote to downvote | compute delta from old vote to new vote |
| duplicate vote request | idempotent upsert by `(userId, targetId)` |
| post removed after cached ranking | moderation filter removes it at read time |
| private community | membership check before feed response |
| deleted comment with replies | show deleted placeholder if children remain |
| locked thread | allow reads but reject new comments |
| huge comment thread | paginate top-level and child comments |
| ranking cache stale | serve slightly stale list with final filtering |
| hot post score key | sharded counters and async aggregation |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
redditforum/
  domain/
    Community.java
    Post.java
    Comment.java
    Vote.java
    ScoreAggregate.java
    FeedCursor.java
  service/
    CommunityService.java
    PostService.java
    CommentService.java
    VoteService.java
    FeedService.java
    ModerationService.java
  ranking/
    FeedSortStrategy.java
    HotSortStrategy.java
    NewSortStrategy.java
    TopSortStrategy.java
  repository/
    PostRepository.java
    CommentRepository.java
    VoteRepository.java
    FeedIndexRepository.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import defaultdict
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class Post:
    post_id: str
    community_id: str
    author_id: str
    title: str
    created_at: float
    status: str = "PUBLISHED"


class ForumFeed:
    def __init__(self) -> None:
        self.posts: dict[str, Post] = {}
        self.community_posts: dict[str, list[str]] = defaultdict(list)
        self.votes: dict[tuple[str, str], int] = {}
        self.scores: dict[str, int] = defaultdict(int)

    def create_post(self, community_id: str, author_id: str, title: str) -> Post:
        post = Post(f"p_{len(self.posts) + 1}", community_id, author_id, title, time())
        self.posts[post.post_id] = post
        self.community_posts[community_id].append(post.post_id)
        return post

    def vote(self, user_id: str, post_id: str, value: int) -> None:
        key = (user_id, post_id)
        old_value = self.votes.get(key, 0)
        self.votes[key] = value
        self.scores[post_id] += value - old_value

    def get_feed(self, community_id: str, sort: str = "hot", limit: int = 25) -> list[Post]:
        post_ids = self.community_posts[community_id]
        posts = [self.posts[post_id] for post_id in post_ids if self.posts[post_id].status == "PUBLISHED"]
        if sort == "new":
            posts.sort(key=lambda post: post.created_at, reverse=True)
        else:
            posts.sort(key=lambda post: (self.scores[post.post_id], post.created_at), reverse=True)
        return posts[:limit]


forum = ForumFeed()
post = forum.create_post("systemdesign", "u1", "How do ranking caches work?")
forum.vote("u2", post.post_id, 1)
print([item.title for item in forum.get_feed("systemdesign")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| posts | `Map<PostId, Post>` |
| community post index | `Map<CommunityId, List<PostId>>` |
| votes | `Map<(UserId, TargetId), VoteValue>` |
| scores | `Map<TargetId, ScoreAggregate>` |
| comment children | `Map<(PostId, ParentCommentId), List<CommentId>>` |
| ranked feed | sorted set or priority index |
| hot queues | stream/queue for ranking refresh |

## 4.4 Concurrency

- Use idempotency keys for post/comment creation.
- Vote changes should be atomic per `(userId, targetId)`.
- Public score updates can be async but must apply correct delta.
- Comment creation should validate parent/thread state.
- Moderation changes should create tombstones or status updates.
- Hot counters should be sharded and aggregated.

## 4.5 Performance Optimization

- Precompute hot/top/rising ranked lists for active communities.
- Cache hot feed pages and hot comment pages.
- Batch hydrate post/user/community/vote-state data.
- Use sharded vote counters.
- Paginate comments aggressively.
- Use CDN for media/link previews.
- Use lazy refresh of ranking caches when exact freshness is not needed.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Viral Post

Problem:

- A post reaches the front page and receives huge reads, votes, comments, and share traffic.

Handling:

- Cache post preview and first comment pages.
- Use sharded vote counters.
- Batch comment count updates.
- Protect ranking refresh with debounce/coalescing.
- Use stale-while-revalidate for ranked feed pages.

## 5.2 Comment Storm

Problem:

- A live event thread receives many comments per second.

Handling:

- Partition comments by post and parent bucket.
- Paginate comment reads.
- Cache top-level comments separately from deep replies.
- Use async comment count aggregation.
- Allow moderators to lock thread under abuse.

## 5.3 Vote Storm

Handling:

- Upsert vote state idempotently.
- Emit vote delta events.
- Aggregate with sharded counters.
- Refresh ranking periodically instead of on every vote.
- Rate-limit suspicious voting behavior.

## 5.4 Cache Stampede

Handling:

- Request coalescing for hot feed pages.
- Stale-while-revalidate.
- Per-community cache warming for hot/rising lists.
- TTL jitter.
- Circuit breakers around hydration dependencies.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| posts | `communityId + time` or `postId` | community feeds and direct lookup |
| comments | `postId` plus shard/bucket | hot thread scaling |
| votes | `targetId` plus shard or `userId` | aggregation vs user vote lookup |
| community feeds | `communityId + sort` | ranked list serving |
| home feed cache | `userId` | personalized merge result |
| subscriptions | `userId` | home feed source list |

## 6.2 Multi-Region Strategy

- Serve public reads from nearby regions and caches.
- Keep writes routed to primary shard/region for the community or user.
- Replicate post/comment metadata for read availability.
- Replicate moderation removals with high priority.
- Use CDN for media assets and link preview images.

## 6.3 Ranking At Scale

- Keep per-community ranked indexes.
- Refresh hot/rising lists frequently for active communities.
- Refresh quiet communities less often.
- Precompute global popular feeds by region/language.
- Merge subscribed community candidates for home feed with a bounded fan-in.
- Cache merged home pages briefly.

## 6.4 Observability

Track:

- feed p99 latency,
- cache hit rate by community/sort,
- vote ingestion lag,
- score aggregation lag,
- ranking refresh lag,
- comment write/read latency,
- moderation-filter drops,
- hot key detection,
- stale cache serve rate.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify home feed vs community feed.
2. Clarify sort modes: hot, new, top, rising.
3. Design community, post, vote, and comment models.
4. Explain idempotent votes and async score aggregation.
5. Design ranked community feed indexes.
6. Design home feed as a merge of subscribed community candidates.
7. Add comment tree pagination and caching.
8. Add moderation/visibility filtering, spike handling, and billion-user scale.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| home feed | per-user push | pull/merge communities | pull/merge with cache |
| vote counts | sync exact | async aggregate | async aggregate with user vote read-your-write |
| ranking | compute on read | precompute ranked lists | precompute hot communities, lazy for cold |
| comments | load full tree | paginated tree | paginated tree |
| cache | long TTL | short/stale-while-revalidate | short with final moderation filter |

## 7.3 Common Mistakes

- Treating Reddit like only a follower fanout feed.
- Updating one global score counter for hot posts without sharding.
- Recomputing hot ranking on every vote synchronously.
- Loading entire comment trees.
- Trusting cached ranked lists after moderation removal.
- Forgetting that user vote state and public score have different consistency needs.

## 7.4 Strong Closing

> Reddit is a community-ranking system. Posts and comments are canonical, votes are idempotent user state, public scores are aggregated asynchronously, ranked community lists are cached, home feed merges subscribed community candidates, and final moderation/visibility checks protect correctness.

---

# 8. Fast Recall Rules

- Reddit feed is community-centric, not follower-centric.
- Home feed usually pulls/merges subscribed community ranked lists.
- Community feeds need hot/new/top/rising indexes.
- Votes are idempotent state per user and target.
- Public score aggregation is eventual.
- Hot posts need sharded counters.
- Comments are trees and must be paginated.
- Cached ranked lists are only candidate lists.
- Moderation/visibility filters run before rendering.
- Use stale-while-revalidate for hot feed caches.