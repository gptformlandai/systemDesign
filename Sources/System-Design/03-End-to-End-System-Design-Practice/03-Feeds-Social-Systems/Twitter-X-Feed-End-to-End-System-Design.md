# Twitter (X) Feed - End-to-End System Design

> Goal: design a high-scale microblogging feed with home timeline generation, celebrity fanout, ranking, caching, consistency, timeline storage, and real-time freshness.

---

## How To Use This File

- Use this guide when the interview problem says Twitter, X, microblog feed, home timeline, or timeline generation.
- Focus on short posts, huge celebrity follower skew, retweets/reposts, replies, real-time freshness, and read-heavy timeline serving.
- The core lesson is the same feed problem with a sharper fanout challenge: normal accounts can push, celebrity accounts must be handled carefully.
- In interviews, explain home timeline and user timeline separately.

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

| Layer | Interview signal | Twitter/X focus |
|---|---|---|
| Problem understanding | Can clarify timeline scope | home timeline, user timeline, follow graph, reposts, replies |
| HLD | Can design scalable timeline service | post service, graph service, fanout, timeline store, mixer, ranking |
| LLD | Can model clean components | `Tweet`, `FollowEdge`, `TimelineEntry`, `FanoutStrategy`, `RankingStrategy` |
| Machine coding | Can implement core flow | post tweet, follow, fanout, read timeline, cursor pagination |
| Traffic spikes | Can handle celebrities/news | viral tweets, breaking news, hot hashtags, timeline refresh storm |
| Billion users | Can reason globally | hybrid fanout, active-user fanout, regional caches, timeline compaction |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- User can create a short post/tweet with text and optional media references.
- User can follow and unfollow another user.
- User can fetch a personalized home timeline.
- User can fetch a user's profile timeline.
- User can repost/retweet, reply, like, bookmark, mute, block, and report.
- Timeline supports cursor-based pagination.
- Timeline should include content from followed users and optionally ranked recommendations.
- Timeline must hide deleted, blocked, muted, or restricted content.

Optional requirements to clarify:

- Is the home timeline chronological, ranked, or a mix?
- Do replies appear in the home timeline?
- Are recommended posts from non-followed users allowed?
- Are ads, trends, spaces/live audio, or communities included?
- Do we need push updates through WebSocket/SSE, or only pull refresh?
- Is search/trending hashtag design in scope?

Out of scope unless asked:

- Full search indexing and ranking.
- Complete ad auction platform.
- Full media transcoding service.
- Full abuse/moderation workflow.

## 1.2 Non-Functional Requirements

Timeline read path:

- Very low latency for first page and pull-to-refresh.
- High availability because home timeline is the main read surface.
- Stable cursor pagination.
- Good cache efficiency for active users.

Post/fanout path:

- Durable tweet creation before fanout.
- Asynchronous fanout to follower timelines.
- Idempotent fanout writes under retries.
- Celebrity-aware fanout to avoid write explosions.

Freshness and consistency:

- Normal posts should appear in followers' home timelines within seconds.
- Celebrity posts may be merged at read time.
- Like/repost counts can be eventually consistent.
- Deletes, blocks, and mutes must be enforced before rendering.

## 1.3 Constraints

- Follower distribution is extremely skewed.
- Breaking news can cause many reads, reposts, and refreshes in seconds.
- Many users follow hundreds or thousands of accounts.
- Retweets can multiply content visibility without duplicating canonical tweets.
- Timeline storage grows quickly if old entries are never compacted.
- Ranking needs freshness, social relevance, and quality without blowing latency.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| Daily active users | 300 million |
| Tweets/posts/day | 500 million |
| Timeline reads/day | 150 billion |
| Avg following/user | 400 |
| Celebrity followers | 100M+ |
| Feed page size | 20 to 50 entries |
| Home timeline p99 | under 200 ms |

Back-of-the-envelope:

- `500M posts/day` is about `5,787 posts/sec` average before spikes.
- Fanout to 400 followers means `200B` timeline-entry writes/day for normal accounts.
- A single 100M-follower account cannot be fully pushed synchronously.
- Most timeline reads should avoid scanning the full follow graph live.

## 1.5 Clarifying Questions To Ask

- Should home timeline be ranked or strictly chronological?
- Are tweets from non-followed users included?
- Do retweets and replies appear by default?
- How fresh should celebrity posts be?
- How much history does home timeline store?
- What is the fallback when ranking is unavailable?
- Are mutes/blocks/privacy in scope?

Strong interview framing:

> I will design a hybrid timeline system: tweets are canonical, normal authors push lightweight timeline entries to followers, celebrity authors are pulled or partially faned out, the home timeline merges candidates, ranks them, filters visibility, hydrates metadata, and returns an opaque cursor.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Tweet create flow:
Client -> API Gateway -> Tweet Service -> Tweet Store
       -> Outbox/Event Stream: tweet.created
       -> Fanout Service -> Home Timeline Store
       -> Cache invalidation

Home timeline read flow:
Client -> API Gateway -> Timeline Service
       -> Timeline Cache / Home Timeline Store
       -> Pull Candidate Source for celebrities/recommendations
       -> Ranking Service
       -> Visibility Filter
       -> Hydration Service
       -> Response with cursor
```

Recommended architecture:

```text
                          +---------------------+
                          | Ranking Service     |
                          +----------+----------+
                                     |
Client                               v
  |                       +----------------------+
  v                       | Timeline Service     |
+-------------+           +----------+-----------+
| API Gateway |                      |
+------+------+                      v
       |                   +----------------------+
       |                   | Timeline Cache       |
       |                   +----------+-----------+
       v                              |
+--------------+                      v
| Tweet Service|            +---------------------+
+------+-------+            | Home Timeline Store |
       |                    +----------+----------+
       v                               ^
+--------------+                       |
| Tweet Store  |                       |
+------+-------+                       |
       |                               |
       v                               |
+--------------+             +---------+----------+
| Event Stream |------------>| Fanout Workers     |
+------+-------+             +---------+----------+
       |                               |
       v                               v
+--------------+             +--------------------+
| User Timeline|             | Follow Graph       |
| Store        |             | Service            |
+--------------+             +--------------------+
```

Request flow for home timeline:

1. Client calls `GET /v1/timeline/home?cursor=...&limit=20`.
2. Gateway authenticates and rate-limits refresh behavior.
3. Timeline Service fetches recent inbox entries from cache or timeline store.
4. It merges pull candidates from followed celebrity accounts and recommendations if enabled.
5. Visibility Filter removes deleted, blocked, muted, restricted, or already-hidden posts.
6. Ranking Service scores and orders a bounded candidate set.
7. Hydration Service batch-fetches tweet body, author preview, media, and counters.
8. Response returns timeline items and an opaque cursor.

## 2.2 APIs

### Create Tweet

```http
POST /v1/tweets
Authorization: Bearer <token>
Idempotency-Key: tweet-123
Content-Type: application/json

{
  "text": "shipping the new timeline design",
  "mediaIds": ["m_900"],
  "replyToTweetId": null
}
```

Response:

```json
{
  "tweetId": "t_1001",
  "authorId": "u_42",
  "status": "PUBLISHED",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

### Get Home Timeline

```http
GET /v1/timeline/home?cursor=abc&limit=20
Authorization: Bearer <token>
```

Response:

```json
{
  "items": [
    {
      "timelineEntryId": "te_1",
      "tweetId": "t_1001",
      "authorId": "u_42",
      "score": 0.91,
      "entryType": "TWEET",
      "createdAt": "2026-06-17T12:00:00Z"
    }
  ],
  "nextCursor": "def"
}
```

### Repost

```http
POST /v1/tweets/{tweetId}/repost
Authorization: Bearer <token>
Idempotency-Key: repost-123
```

### Follow User

```http
POST /v1/follows
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetUserId": "u_99"
}
```

## 2.3 Core Components

Think of Twitter/X feed as four storage views over the same canonical content:

| View | What it stores | Why it exists |
|---|---|---|
| Tweet store | canonical tweet body and lifecycle | single source of truth |
| User timeline | tweets by one author | profile page and pull merge |
| Home timeline | candidate entries for one viewer | fast home reads |
| Engagement/feature store | counters and behavioral features | ranking and product metrics |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| API Gateway | auth, rate limits, refresh shaping | timeline ranking | request QPS |
| Tweet Service | tweet lifecycle and durable events | follower fanout loops | write QPS |
| Follow Graph Service | follow/unfollow edges | tweet content | edge reads and celebrity reverse edges |
| Fanout Service | timeline-entry distribution | final feed ranking | follower count skew |
| Home Timeline Store | per-viewer candidate windows | canonical tweet text | read/write amplification |
| User Timeline Store | tweets by author | follower inboxes | author profile reads and celebrity pull |
| Timeline Service | fetch, merge, paginate | model training | feed read QPS |
| Ranking Service | score candidates | storage writes | feature latency |
| Hydration Service | batch tweet/author/media/counter fetch | fanout decisions | metadata QPS |
| Visibility Service | block/mute/delete/safety checks | ranking order | correctness |

### Feed Generation: Push vs Pull

| Strategy | Description | Twitter/X fit |
|---|---|---|
| Push | write timeline entry into each follower's home timeline | excellent for normal authors |
| Pull | read recent tweets from followed authors during timeline fetch | useful for celebrities and cold users |
| Hybrid | push normal authors, pull celebrity authors, merge at read time | recommended |

Celebrity rule:

> The larger the follower count, the more dangerous fanout-on-write becomes. Move huge accounts to pull, partial fanout, or active-follower-only fanout.

### Fanout Service

Responsibilities:

- Consume tweet-created and repost-created events.
- Fetch follower batches from Follow Graph Service.
- Decide fanout strategy by author tier.
- Write compact timeline entries to Home Timeline Store.
- Deduplicate using `(viewerId, tweetId, sourceAction)`.
- Track lag and retry failed batches.

Fanout entry should contain:

```text
viewer_id
tweet_id
author_id
source_actor_id
entry_type: TWEET | REPOST | REPLY | RECOMMENDATION
created_at
fanout_at
base_score_hint
```

### Timeline Service

Responsibilities:

- Fetch precomputed inbox candidates.
- Pull recent tweets from followed celebrity accounts.
- Merge reposts, recommendations, and promoted entries if in scope.
- Bound candidate set before ranking.
- Enforce cursor pagination.
- Call visibility, ranking, and hydration.

Timeline merge example:

```text
home_candidates =
  pushed home timeline entries
  + pulled recent celebrity tweets
  + pulled recent high-quality recommendations
  + eligible promoted entries
```

### Ranking Service

Typical ranking signals:

| Signal | Examples |
|---|---|
| recency | tweet age, freshness window |
| social affinity | interactions with author, follows, replies, DMs if available |
| engagement | repost velocity, likes, replies, quote posts |
| content quality | spam score, media type, text quality |
| topic interest | hashtags, embeddings, communities |
| negative feedback | mutes, hides, reports, not-interested |
| diversity | avoid too many tweets from same author/topic |

Fallback:

- If ranking is slow, use chronological order over a cached candidate window.
- If feature store is slow, use default feature values and a cheaper model.

### Timeline Storage

Home timeline table:

```text
Partition key: viewer_id
Sort key: rank_bucket or created_at DESC + tweet_id
Value: compact TimelineEntry
TTL/compaction: keep recent home window, rebuild older history from author timelines if needed
```

User timeline table:

```text
Partition key: author_id
Sort key: created_at DESC + tweet_id
Value: tweet_id and light metadata
```

Storage rule:

- Home timeline stores references, not full tweets.
- User timeline is the source for profile reads and celebrity pull.
- Tweet store remains canonical truth.

### Caching Strategy

| Cache | Key | Value | TTL |
|---|---|---|---|
| Home first page | `home:{viewerId}:first` | ranked tweet IDs | seconds |
| Timeline candidates | `home_candidates:{viewerId}` | candidate IDs | minutes |
| Tweet hydration | `tweet:{tweetId}` | tweet preview | minutes |
| Author preview | `user:{authorId}` | handle, avatar, verification | minutes |
| Engagement counters | `tweet_counts:{tweetId}` | likes/reposts/replies | seconds |
| Celebrity recent tweets | `celebrity:{authorId}:recent` | recent tweet IDs | seconds to minutes |

Important cache rule:

> A cached timeline is only a candidate list; visibility and tweet status still need final checks.

### Consistency Model

| Data | Consistency target | Reason |
|---|---|---|
| tweet creation | strong canonical write | avoid lost posts |
| user profile timeline | read-your-write | author expects immediate post |
| home timeline fanout | eventual | async fanout is acceptable |
| repost/like counters | eventual | counters can lag |
| delete/block/mute | read-time enforced | correctness and safety |
| ranking score | eventual | quality can be refreshed |

One-stop interview answer:

> I separate canonical tweets from timeline entries. Normal authors are pushed to follower home timelines, celebrity authors are pulled or partially faned out, the timeline read path merges candidates, filters visibility, ranks within a bounded latency budget, hydrates metadata in batches, and returns an opaque cursor.

---

# 3. Low-Level Design

LLD goal:

> Model tweets, follows, timeline entries, fanout, ranking, visibility, and hydration as separate pieces so the core timeline flow is easy to reason about.

Simple rule:

- `Tweet` is canonical content.
- `TimelineEntry` is a viewer-specific candidate.
- `UserTimeline` stores author history.
- `HomeTimeline` stores viewer feed candidates.
- `FanoutStrategy` chooses push or pull behavior.

Starter map:

| LLD question | Twitter/X answer |
|---|---|
| Canonical content | `Tweet` |
| Relationship graph | `FollowEdge` |
| Viewer feed row | `TimelineEntry` |
| Author profile source | `UserTimeline` |
| Write-side service | `FanoutService` |
| Read-side service | `TimelineService` |
| Ranking abstraction | `RankingStrategy` |
| Fanout abstraction | `FanoutStrategy` |
| Correctness gate | `VisibilityPolicy` |

Beginner-friendly design order:

1. Model `Tweet` as the source of truth.
2. Model `FollowEdge` for follower relationships.
3. Model `TimelineEntry` as a reference to a tweet.
4. Write `TweetService.createTweet()` to persist and publish an event.
5. Write `FanoutService` to append timeline entries for normal authors.
6. Write `TimelineService.getHomeTimeline()` to fetch, merge, rank, filter, and hydrate.
7. Add celebrity pull source for huge authors.
8. Add idempotency and cursor pagination.

Interview sentence:

> In LLD, I will keep Tweet, UserTimeline, and HomeTimeline separate: tweet creation writes canonical content, fanout creates lightweight home entries, and timeline reads merge pushed entries with pulled celebrity tweets before ranking and visibility filtering.

## 3.1 Object Modelling

| Entity | Owns | Key invariant |
|---|---|---|
| `Tweet` | canonical text/media/reply state | deleted tweets must not render |
| `FollowEdge` | follower-to-author relationship | inactive edges should not create candidates |
| `TimelineEntry` | viewer-specific candidate reference | unique by `(viewerId, tweetId, sourceActorId)` |
| `Repost` | source actor amplifying tweet | should not duplicate tweet body |
| `TimelineCursor` | pagination state | opaque and tamper-resistant |
| `RankingContext` | viewer/session features | no canonical mutation |
| `VisibilityPolicy` | block/mute/delete/safety checks | final gate before rendering |

Core services:

| Service | Responsibility |
|---|---|
| `TweetService` | create/delete tweets and publish events |
| `FollowService` | maintain follow graph |
| `FanoutService` | distribute timeline entries |
| `TimelineService` | assemble home timeline |
| `RankingService` | score candidates |
| `HydrationService` | batch fetch tweet, author, counters |
| `VisibilityService` | enforce block/mute/delete rules |

## 3.2 Class Sketch

```java
interface FanoutStrategy {
    FanoutMode choose(String authorId, long followerCount);
}

interface TimelineRepository {
    void append(String viewerId, TimelineEntry entry);
    List<TimelineEntry> readHome(String viewerId, TimelineCursor cursor, int limit);
}

interface RankingStrategy {
    List<ScoredTimelineEntry> rank(String viewerId, List<TimelineEntry> entries, RankingContext context);
}

interface VisibilityPolicy {
    boolean canRender(String viewerId, Tweet tweet, AuthorState authorState);
}
```

## 3.3 Sequence Diagram

```text
Client -> TweetService: createTweet(authorId, text)
TweetService -> TweetStore: save(tweet)
TweetService -> EventStream: publish(tweet.created)
FanoutWorker -> FollowGraph: getFollowers(authorId)
FanoutWorker -> TimelineStore: append(viewerId, timelineEntry)

Client -> TimelineService: getHome(viewerId, cursor)
TimelineService -> TimelineStore: readHome(viewerId, cursor)
TimelineService -> CelebritySource: pullRecent(viewerId)
TimelineService -> VisibilityService: filter(entries)
TimelineService -> RankingService: rank(entries)
TimelineService -> HydrationService: hydrate(entries)
TimelineService --> Client: items + nextCursor
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | fanout mode and ranking mode |
| Observer/Event | tweet-created triggers fanout |
| Factory | choose ranking strategy by feed type |
| Adapter | hide timeline store and cache clients |
| Decorator | cache-backed timeline repository |
| State | tweet lifecycle: published, restricted, deleted |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| user follows nobody | return onboarding suggestions or recommendations |
| celebrity tweets | pull or active-user partial fanout |
| duplicate tweet create retry | idempotency key |
| duplicate fanout worker retry | unique timeline-entry key |
| tweet deleted after fanout | tombstone and read-time filter |
| viewer mutes author | visibility filter removes entries |
| retweet of deleted tweet | hide retweet container or show unavailable state |
| ranking timeout | chronological fallback |
| cursor points to deleted entry | skip and continue until page fills or limit reached |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
timeline/
  domain/
    Tweet.java
    FollowEdge.java
    TimelineEntry.java
    TimelineCursor.java
  service/
    TweetService.java
    FollowService.java
    FanoutService.java
    TimelineService.java
    VisibilityService.java
  ranking/
    RankingStrategy.java
    ChronologicalRankingStrategy.java
  repository/
    TweetRepository.java
    TimelineRepository.java
    FollowGraphRepository.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import defaultdict, deque
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class Tweet:
    tweet_id: str
    author_id: str
    text: str
    created_at: float
    deleted: bool = False


class TimelineApp:
    def __init__(self) -> None:
        self.followers: dict[str, set[str]] = defaultdict(set)
        self.tweets: dict[str, Tweet] = {}
        self.home: dict[str, deque[str]] = defaultdict(deque)
        self.blocked: set[tuple[str, str]] = set()

    def follow(self, follower_id: str, author_id: str) -> None:
        self.followers[author_id].add(follower_id)

    def create_tweet(self, author_id: str, text: str) -> Tweet:
        tweet = Tweet(f"t_{len(self.tweets) + 1}", author_id, text, time())
        self.tweets[tweet.tweet_id] = tweet
        for follower_id in self.followers[author_id]:
            self.home[follower_id].appendleft(tweet.tweet_id)
        self.home[author_id].appendleft(tweet.tweet_id)
        return tweet

    def get_home(self, viewer_id: str, limit: int = 20) -> list[Tweet]:
        result: list[Tweet] = []
        for tweet_id in self.home[viewer_id]:
            tweet = self.tweets.get(tweet_id)
            if not tweet or tweet.deleted:
                continue
            if (viewer_id, tweet.author_id) in self.blocked:
                continue
            result.append(tweet)
            if len(result) == limit:
                break
        return result


app = TimelineApp()
app.follow("u2", "u1")
app.create_tweet("u1", "hello timeline")
print([tweet.text for tweet in app.get_home("u2")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| author followers | `Map<AuthorId, Set<UserId>>` |
| tweet store | `Map<TweetId, Tweet>` |
| home timeline | `Map<UserId, Deque<TweetId>>` |
| author timeline | `Map<AuthorId, Deque<TweetId>>` |
| blocked pairs | `Set<(viewerId, authorId)>` |
| fanout queue | `Queue<TweetCreatedEvent>` |

## 4.4 Concurrency

Risks:

- Duplicate timeline entries after event retry.
- Follow/unfollow race with fanout.
- Delete racing with timeline read.
- Hot celebrity partitions.

Mitigations:

- Idempotent insert keyed by `(viewerId, tweetId, sourceActorId)`.
- Versioned follow edges.
- Soft delete and read-time filtering.
- Partition celebrity fanout separately.
- Use outbox pattern for tweet-created events.

## 4.5 Performance Optimization

- Precompute active users' home timelines.
- Keep home timeline entries compact.
- Use pull source for celebrities.
- Cache first page and hydrated tweet previews.
- Batch hydration of tweet/user/counter data.
- Bound ranking candidate count.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Breaking News Spike

Problem:

- Many users refresh timelines, many authors post, and a few tweets become extremely hot.

Handling:

- Throttle refresh frequency per user/device.
- Serve cached first page for short windows.
- Reduce ranking feature budget.
- Use hot-tweet cache for hydration and counters.
- Prioritize timeline fanout for active users.

## 5.2 Celebrity Tweet Fanout

Handling:

- Do not push to all followers synchronously.
- Use pull-on-read or active-follower fanout.
- Split fanout partitions by follower shard.
- Isolate celebrity fanout workers from normal-user fanout.
- Monitor fanout lag by author tier.

## 5.3 Engagement Counter Storm

Handling:

- Append likes/reposts/replies to event stream.
- Use sharded counters for hot tweets.
- Update displayed counts eventually.
- Feed ranking consumes aggregated velocity features.

## 5.4 Ranking Degradation

Handling:

- Fall back to chronological timeline over cached candidates.
- Use cached/default feature values.
- Reduce candidate count and disable expensive diversity passes.
- Keep visibility filtering on.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| tweets | `tweetId` or `(authorId, time)` | profile timeline needs author/time access |
| home timeline | `viewerId` | feed read is per viewer |
| user timeline | `authorId` | profile page and celebrity pull |
| follow graph forward | `followerId` | following list |
| follow graph reverse | `authorId` | fanout recipients |
| engagement events | `tweetId + time` | hot tweet aggregation |

## 6.2 Active User Optimization

- Fan out only to recently active followers for huge authors.
- Rebuild cold users' timelines when they return.
- Keep smaller candidate windows for inactive users.
- Prioritize active-region cache warming.

## 6.3 Multi-Region Strategy

- Serve timeline reads from the viewer's home region.
- Replicate public tweet metadata globally.
- Keep media on CDN.
- Accept eventual cross-region home timeline delivery.
- Replicate deletes/blocks/mutes with high priority.

## 6.4 Timeline Compaction

- Keep recent home timeline window hot.
- TTL old home entries.
- Rebuild older pages from user timelines and search/archive storage if needed.
- Compact duplicate retweets or repeated recommendation entries.

## 6.5 Observability

Track:

- Home timeline latency p50/p95/p99.
- Cache hit rate.
- Fanout lag by author tier.
- Ranking timeout rate.
- Celebrity pull latency.
- Hydration batch size and latency.
- Duplicate timeline-entry rate.
- Visibility-filter drop rate.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify home timeline vs user timeline.
2. Estimate post volume, read volume, and follower skew.
3. Store canonical tweets and author timelines.
4. Add home timeline fanout for normal authors.
5. Add celebrity pull/hybrid path.
6. Add ranking, visibility filtering, hydration, and cursor pagination.
7. Add caching and consistency rules.
8. Discuss breaking-news spikes and billion-user scaling.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| fanout | push | pull | hybrid |
| timeline order | chronological | ranked | ranked with chronological fallback |
| storage | full tweet copies | tweet references | references |
| consistency | strong timeline | eventual timeline | strong tweet/delete, eventual fanout |
| celebrities | normal fanout | special handling | pull or active-user fanout |

## 7.3 Common Mistakes

- Synchronously fan out celebrity tweets to every follower.
- Store full tweet body in every home timeline row.
- Ignore user timeline as a separate access pattern.
- Trust cached timeline entries after delete/block/mute.
- Rank an unbounded candidate set online.
- Update hot engagement counters with one global row.

## 7.4 Strong Closing

> Twitter/X timeline design is a hybrid fanout problem. Canonical tweets and user timelines are durable truth, home timelines are precomputed candidate references, celebrity posts are pulled or partially faned out, and the read path filters, ranks, hydrates, and paginates under a strict latency budget.

---

# 8. Fast Recall Rules

- Tweet store is canonical truth.
- User timeline is author history.
- Home timeline is viewer candidate storage.
- Push normal authors.
- Pull or partially fan out celebrities.
- Store references, not full tweets.
- Rank bounded candidates.
- Filter delete/block/mute at read time.
- Cache first page and hydration data.
- Use sharded counters for hot engagement.
