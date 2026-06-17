# TikTok Video Feed and Recommendations - End-to-End System Design

> Goal: design a short-video feed with recommendation-heavy ranking, video delivery, candidate generation, caching, consistency, watch-event pipelines, and large-scale personalization.

---

## How To Use This File

- Use this when the interview problem says TikTok, Reels, Shorts, For You feed, video recommendations, or infinite video feed.
- The core challenge is different from a pure social-follow feed: ranking and recommendation quality dominate.
- Fanout still exists for the Following feed, but the For You feed is mostly pull/recommendation-based.
- Keep video serving separate from feed serving: feed returns metadata and CDN playback URLs; video bytes come from object storage/CDN.

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

| Layer | Interview signal | TikTok focus |
|---|---|---|
| Problem understanding | Can separate feed types | For You feed, Following feed, profile feed, video upload, watch events |
| HLD | Can design recommendation serving | candidate generation, ranking, feature store, video metadata, CDN |
| LLD | Can model recommendation feed | `Video`, `FeedCandidate`, `WatchEvent`, `RankingStrategy`, `RecommendationSource` |
| Machine coding | Can implement simplified feed | upload video metadata, record watch, rank candidates, cursor pagination |
| Traffic spikes | Can handle viral videos | CDN hot objects, watch-event flood, ranking feature pressure |
| Billion users | Can scale personalization | multi-stage ranking, caches, offline features, regional CDN, event streaming |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Creator can upload a short video.
- User can fetch a personalized For You feed.
- User can fetch a Following feed.
- User can like, comment, share, save, follow, skip, report, and mark not interested.
- System records watch events such as impressions, watch duration, completion, replay, like, share, and skip.
- Feed supports cursor/session-based infinite scrolling.
- Video playback should use CDN URLs and adaptive bitrate metadata.
- Unsafe, deleted, private, or geo-restricted videos must not render.

Optional requirements to clarify:

- Is upload/transcoding in scope or only feed serving?
- Is the feed personalized by ML or by simple heuristics?
- Are ads/promoted videos included?
- Should users see only public videos?
- How fresh should newly uploaded videos appear?
- Is cold-start handling for new users/videos in scope?

Out of scope unless asked:

- Full deep-learning training platform internals.
- Complete video transcoding pipeline implementation.
- Full live streaming system.
- Full moderation review console.

## 1.2 Non-Functional Requirements

Read path:

- Very low latency between swipes; next videos should be prefetched.
- High availability for feed and playback metadata.
- Feed response should contain enough video metadata for smooth playback.
- Ranking should degrade safely if feature stores or models are slow.

Video delivery path:

- Video bytes should be served from CDN, not feed services.
- Hot videos should be cached close to users.
- Multiple video qualities should be available for device/network conditions.

Data and ranking path:

- Watch events must be collected at huge scale.
- Ranking features can be eventually consistent.
- Moderation/delete/safety decisions must be enforced before serving.
- Cold-start new videos need exploration traffic.

## 1.3 Constraints

- For You feed is recommendation-heavy, not just follow graph fanout.
- Watch events are extremely high-volume because every impression generates events.
- Hot videos create CDN and metadata hot keys.
- Ranking must be multi-stage because scoring millions of videos online is impossible.
- User interest changes quickly, so session-level signals matter.
- Safety and moderation must prevent harmful or restricted content from reaching the feed.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Registered users | 2 billion |
| Daily active users | 800 million |
| Videos uploaded/day | 100 million |
| Feed video impressions/day | 500 billion |
| Watch events/day | 2 to 5 trillion |
| Feed page/batch size | 10 to 20 videos |
| Next-video latency target | p99 under 100 ms for metadata |
| Video playback startup | as low as possible, usually under 1 to 2 seconds |

Back-of-the-envelope:

- `500B impressions/day` is about `5.8M impressions/sec` average globally.
- If each impression creates multiple watch events, event stream volume is enormous.
- Online ranking cannot scan all videos; it needs candidate generation and staged ranking.
- CDN cache hit rate is critical because video bytes dominate bandwidth cost.

## 1.5 Clarifying Questions To Ask

- Are we designing For You feed, Following feed, or both?
- Is ranking ML-based or rule-based for the interview?
- Are watch events required in real time?
- How do we handle new users and new videos?
- Are privacy, region, age, and moderation constraints required?
- Are ads/promoted videos included?
- Do we need offline download or only streaming?

Strong interview framing:

> I will design TikTok as a recommendation-serving system: video metadata and playback URLs are stored separately from video bytes, candidates come from multiple recommendation sources, a multi-stage ranker scores a bounded set, safety filters run before serving, and watch events feed back into feature pipelines asynchronously.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Video upload flow:
Client -> API Gateway -> Upload Service -> Object Storage
       -> Transcoding Pipeline -> Video Metadata Store
       -> Moderation/Safety Pipeline
       -> Event Stream: video.ready
       -> Candidate Index / Feature Pipeline

For You feed read flow:
Client -> API Gateway -> Feed Service
       -> Candidate Generation Service
       -> Feature Store
       -> Ranking Service
       -> Safety/Eligibility Filter
       -> Hydration Service
       -> Response with playback metadata + CDN URLs

Watch event flow:
Client -> Event Collector -> Event Stream
       -> Real-time Aggregation
       -> Feature Store / Analytics / Model Training
```

Recommended architecture:

```text
                         +----------------------+
                         | Candidate Generation |
                         +----------+-----------+
                                    |
Client                              v
  |                       +----------------------+
  v                       | Feed Service         |
+-------------+           +----------+-----------+
| API Gateway |                      |
+------+------+                      v
       |                   +----------------------+
       |                   | Ranking Service      |
       |                   +----------+-----------+
       |                              |
       v                              v
+---------------+           +---------------------+
| Upload Service|           | Feature Store       |
+------+--------+           +----------+----------+
       |                               |
       v                               v
+---------------+           +---------------------+
| Object Store  |           | Video Metadata Store|
+------+--------+           +----------+----------+
       |                               |
       v                               v
+---------------+           +---------------------+
| Transcoding   |---------->| CDN / Playback URLs |
+---------------+           +---------------------+

Client Watch Events -> Event Collector -> Event Stream -> Feature Pipeline
```

## 2.2 APIs

### Create Upload Session

```http
POST /v1/videos/upload-sessions
Authorization: Bearer <token>
Content-Type: application/json

{
  "fileName": "clip.mp4",
  "contentType": "video/mp4",
  "sizeBytes": 73400320
}
```

Response:

```json
{
  "uploadSessionId": "us_101",
  "uploadUrl": "https://upload.example/us_101",
  "expiresAt": "2026-06-17T10:30:00Z"
}
```

### Publish Video Metadata

```http
POST /v1/videos
Authorization: Bearer <token>
Idempotency-Key: video-publish-123
Content-Type: application/json

{
  "uploadSessionId": "us_101",
  "caption": "first system design sketch",
  "hashtags": ["systemdesign", "learning"],
  "visibility": "PUBLIC"
}
```

Response:

```json
{
  "videoId": "v_9001",
  "status": "PROCESSING",
  "createdAt": "2026-06-17T10:00:00Z"
}
```

### Get For You Feed

```http
GET /v1/feed/for-you?cursor=session_abc&limit=10
Authorization: Bearer <token>
```

Response:

```json
{
  "items": [
    {
      "videoId": "v_9001",
      "creatorId": "u_42",
      "score": 0.97,
      "playback": {
        "hlsUrl": "https://cdn.example/v_9001/master.m3u8",
        "thumbnailUrl": "https://cdn.example/v_9001/thumb.jpg"
      },
      "reason": "high_completion_probability"
    }
  ],
  "nextCursor": "session_def"
}
```

### Record Watch Event

```http
POST /v1/events/watch
Authorization: Bearer <token>
Content-Type: application/json

{
  "videoId": "v_9001",
  "eventType": "WATCH_PROGRESS",
  "watchMs": 12000,
  "videoDurationMs": 18000,
  "sessionId": "s_123"
}
```

## 2.3 Core Components

TikTok feed has two major systems: video delivery and recommendation serving.

| Plane | Owns | Main goal |
|---|---|---|
| Video ingestion plane | upload, object storage, transcoding, metadata | make videos playable |
| Recommendation plane | candidate generation, ranking, features | choose the next best videos |
| Event plane | impressions, watch time, likes, skips | learn user preferences |
| Safety plane | moderation, age/region/privacy filtering | prevent ineligible content from serving |
| Delivery plane | CDN, playback URLs, prefetching | make playback fast and cheap |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| API Gateway | auth, rate limits, request shaping | ranking decisions | feed and event QPS |
| Upload Service | upload sessions and publish intent | transcoding internals | upload QPS |
| Object Storage | raw and transcoded video bytes | feed ranking | bandwidth and durability |
| Transcoding Pipeline | bitrate variants, thumbnails, status | user ranking | CPU/GPU jobs |
| Video Metadata Store | captions, creator, status, playback refs | video bytes | metadata reads |
| Candidate Generation | retrieve possible videos | final ordering | index size and recall |
| Ranking Service | score candidates | canonical video writes | feature/model latency |
| Feature Store | user/video/session features | event collection durability | hot feature reads |
| Safety Filter | moderation, privacy, region, age checks | ranking score | correctness |
| Event Collector | watch/engagement events | ranking response | event volume |
| CDN | video bytes and thumbnails | feed candidate logic | cache hit and bandwidth |

### Feed Generation Strategy

TikTok has multiple feed types:

| Feed | Generation style | Notes |
|---|---|---|
| For You | pull/recommendation-based | main feed; ranking dominates |
| Following | hybrid fanout or pull from followed creators | closer to social feed |
| Profile | pull from creator's video timeline | mostly chronological |
| Hashtag/sound | pull from inverted indexes | ranked by relevance/popularity |

For You candidate sources:

- videos similar to recent completions,
- videos from creators the user engages with,
- trending videos in region/language,
- fresh videos needing exploration,
- videos from followed creators,
- topic/hashtag/sound-based candidates,
- diversity/exploration candidates.

### Multi-Stage Ranking

Online ranking should not score the full corpus.

```text
Stage 1: candidate generation
  thousands of possible videos from many sources

Stage 2: lightweight filtering
  remove seen, unsafe, blocked, wrong-region, already skipped too often

Stage 3: coarse ranking
  cheap model reduces thousands to hundreds

Stage 4: heavy ranking
  expensive model scores top candidates

Stage 5: re-ranking
  diversity, freshness, creator caps, exploration, ads if in scope
```

Ranking signals:

| Signal family | Examples |
|---|---|
| watch behavior | completion rate, watch time, rewatch, skip speed |
| engagement | likes, comments, shares, saves, follows |
| similarity | embeddings, topics, sounds, hashtags |
| freshness | upload age, trend velocity |
| creator quality | trust, consistency, follower relation |
| negative feedback | reports, not interested, fast skips |
| context | device, network, region, language, session state |

### Timeline / Feed Session Storage

TikTok often serves a ranked session queue rather than a permanent per-user inbox.

Session feed row:

```text
session_id
viewer_id
rank_position
video_id
score
candidate_source
served_at
seen_status
```

Why store session feed state:

- avoid showing duplicates within a session,
- support cursor pagination,
- support event attribution from impressions to rank position,
- allow auditing of why a video was shown.

### Caching Strategy

| Cache | Key | Value | TTL |
|---|---|---|---|
| CDN video cache | video segment URL | video bytes | long/versioned |
| thumbnail cache | thumbnail URL | image bytes | long/versioned |
| feed session cache | `(viewerId, sessionId)` | ranked video IDs | minutes |
| video metadata cache | `video:{videoId}` | caption, creator, playback refs | minutes |
| creator cache | `creator:{creatorId}` | profile preview | minutes |
| feature cache | `features:{viewerId}` | recent user/session features | seconds to minutes |
| trending cache | `(region, topic)` | hot video IDs | seconds to minutes |

Cache safety rule:

> CDN can cache video bytes aggressively, but feed candidates must still pass safety and eligibility checks before being returned.

### Consistency Model

| Data | Consistency target | Why |
|---|---|---|
| upload session | strong | avoid lost uploads |
| video publish status | strong for creator response | clear lifecycle |
| transcoding readiness | eventual | video becomes playable after processing |
| For You ranking | eventual | features and models can lag |
| watch events | at-least-once/eventual | high-volume pipeline |
| safety/delete/moderation | read-time enforced | must prevent serving unsafe content |
| counters | eventual | hot videos need async aggregation |

One-stop interview answer:

> TikTok's For You feed is not primarily fanout-on-write; it is multi-stage recommendation serving. I generate candidates from many sources, filter unsafe/seen videos, rank a bounded set using user/video/session features, return playback metadata and CDN URLs, record watch events asynchronously, and feed those events back into feature stores and model training.

---

# 3. Low-Level Design

LLD goal:

> Model video feed serving as candidate generation, ranking, safety filtering, hydration, and watch-event feedback.

Simple rule:

- `Video` is canonical metadata.
- `PlaybackAsset` points to CDN/object storage.
- `FeedCandidate` is a possible video for one viewer.
- `FeedSession` tracks what was served.
- `WatchEvent` teaches the system what happened.

Starter map:

| LLD question | TikTok answer |
|---|---|
| Canonical content object | `Video` |
| Video delivery object | `PlaybackAsset` |
| Feed row object | `FeedCandidate` or `RankedVideo` |
| User feedback object | `WatchEvent` |
| Candidate abstraction | `RecommendationSource` |
| Ranking abstraction | `RankingStrategy` or `Ranker` |
| Safety gate | `EligibilityPolicy` |
| Cursor/session object | `FeedSession` and `FeedCursor` |

Beginner-friendly design order:

1. Model `Video` and `PlaybackAsset` separately.
2. Model `WatchEvent` because TikTok learns heavily from user behavior.
3. Model `RecommendationSource` for candidate generation.
4. Model `RankingService` to score a bounded candidate list.
5. Model `EligibilityPolicy` to remove unsafe, deleted, private, or already-seen videos.
6. Model `FeedSession` so pagination and event attribution are stable.
7. Add caching and prefetching for smooth swipes.

Interview sentence:

> In LLD, I will design For You feed as a ranked session queue: recommendation sources produce candidates, eligibility filters remove invalid videos, ranking orders the rest, hydration adds playback metadata, and watch events update future features asynchronously.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `Video` | canonical video metadata | only READY and eligible videos can serve |
| `PlaybackAsset` | HLS/DASH URLs, thumbnails, bitrate variants | points to processed CDN-ready assets |
| `FeedCandidate` | possible video for viewer | includes source and base score |
| `RankedVideo` | final ordered result | must have rank position for event attribution |
| `FeedSession` | served queue and cursor state | should prevent repeats in one session |
| `WatchEvent` | impression, watch, skip, like, share | should be idempotent or deduplicated |
| `EligibilityPolicy` | safety/privacy/region checks | final gate before rendering |

Core services:

| Service | Responsibility |
|---|---|
| `VideoService` | publish video metadata and lifecycle |
| `CandidateGenerationService` | collect candidates from multiple sources |
| `RankingService` | score and order candidates |
| `EligibilityService` | filter invalid candidates |
| `HydrationService` | fetch video/creator/playback metadata |
| `FeedSessionService` | store served rank queue and cursor |
| `WatchEventService` | collect and publish watch events |

## 3.2 Class Sketch

```java
interface RecommendationSource {
    List<FeedCandidate> getCandidates(String viewerId, RecommendationContext context, int limit);
}

interface Ranker {
    List<RankedVideo> rank(String viewerId, List<FeedCandidate> candidates, RankingContext context);
}

interface EligibilityPolicy {
    boolean canServe(String viewerId, Video video, ViewerContext context);
}

interface FeedSessionRepository {
    void saveServedVideos(String sessionId, List<RankedVideo> videos);
    boolean alreadyServed(String sessionId, String videoId);
}
```

## 3.3 Sequence Diagram

```text
Client -> FeedService: getForYouFeed(viewerId, cursor)
FeedService -> CandidateGenerationService: getCandidates(viewerId)
CandidateGenerationService -> RecommendationSource*: fetch candidates
FeedService -> EligibilityService: filter(candidates)
FeedService -> RankingService: rank(filteredCandidates)
FeedService -> HydrationService: hydrate(videoIds)
FeedService -> FeedSessionRepository: save served rank positions
FeedService --> Client: videos + playback metadata + cursor
Client -> WatchEventService: record watch event
WatchEventService -> EventStream: publish(watch event)
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | ranking model and candidate source behavior |
| Chain of Responsibility | safety and eligibility filters |
| Factory | choose recommendation source mix by user state |
| Observer/Event | watch events update feature pipelines |
| Adapter | wrap feature store/model-serving clients |
| State | video lifecycle: uploaded, processing, ready, blocked, deleted |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| new user | use region/language/trending/onboarding interests |
| new video | exploration bucket and limited test traffic |
| video deleted after ranking | final eligibility check before response |
| user already saw video | session and history filter removes it |
| model timeout | fallback to trending/following/heuristic ranking |
| CDN URL expired | hydrate fresh playback URL |
| watch event duplicated | dedup by event ID or `(sessionId, videoId, eventType, timestamp bucket)` |
| unsafe content flagged | remove from indexes and filter at read time |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
tiktokfeed/
  domain/
    Video.java
    PlaybackAsset.java
    FeedCandidate.java
    RankedVideo.java
    WatchEvent.java
    FeedSession.java
  service/
    FeedService.java
    CandidateGenerationService.java
    RankingService.java
    EligibilityService.java
    WatchEventService.java
  recommendation/
    RecommendationSource.java
    TrendingSource.java
    FollowingSource.java
    SimilarVideoSource.java
  repository/
    VideoRepository.java
    FeedSessionRepository.java
    WatchEventRepository.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from time import time


@dataclass(frozen=True)
class Video:
    video_id: str
    creator_id: str
    caption: str
    tags: set[str]
    created_at: float
    status: str = "READY"


@dataclass(frozen=True)
class WatchEvent:
    viewer_id: str
    video_id: str
    watch_ms: int
    completed: bool


class SimpleVideoFeed:
    def __init__(self) -> None:
        self.videos: dict[str, Video] = {}
        self.watch_history: dict[str, set[str]] = {}
        self.likes: dict[str, int] = {}

    def publish_video(self, creator_id: str, caption: str, tags: set[str]) -> Video:
        video = Video(f"v_{len(self.videos) + 1}", creator_id, caption, tags, time())
        self.videos[video.video_id] = video
        self.likes[video.video_id] = 0
        return video

    def like(self, video_id: str) -> None:
        self.likes[video_id] = self.likes.get(video_id, 0) + 1

    def record_watch(self, event: WatchEvent) -> None:
        self.watch_history.setdefault(event.viewer_id, set()).add(event.video_id)

    def get_for_you(self, viewer_id: str, limit: int = 10) -> list[Video]:
        seen = self.watch_history.get(viewer_id, set())
        candidates = [video for video in self.videos.values() if video.status == "READY" and video.video_id not in seen]
        candidates.sort(key=lambda video: (self.likes.get(video.video_id, 0), video.created_at), reverse=True)
        return candidates[:limit]


feed = SimpleVideoFeed()
feed.publish_video("u1", "ranking basics", {"systemdesign"})
feed.publish_video("u2", "cache tips", {"backend"})
print([video.caption for video in feed.get_for_you("viewer-1")])
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| video metadata | `Map<VideoId, Video>` |
| seen history | `Map<UserId, Set<VideoId>>` |
| feed session | `Map<SessionId, List<RankedVideo>>` |
| candidate dedup | `Set<VideoId>` |
| ranking top K | priority queue or sorted bounded list |
| watch events | append-only queue/stream |

## 4.4 Concurrency

- Video publish should use idempotency keys.
- Watch events should be accepted at least once and deduplicated downstream.
- Ranking should use immutable snapshots of features for one request.
- Feed session writes should be idempotent by `(sessionId, videoId, rankPosition)`.
- Hot counters should be sharded.

## 4.5 Performance Optimization

- Use multi-stage ranking instead of scoring all videos.
- Prefetch next video metadata and first video segments.
- Cache hot video metadata and thumbnails.
- Keep feature fetches batched.
- Store served session queue to avoid duplicates.
- Use CDN for video bytes and thumbnails.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Viral Video

Problem:

- A video suddenly receives massive impressions, watch events, likes, comments, shares, and CDN requests.

Handling:

- CDN caches video segments close to users.
- Prewarm or promote hot video objects across regions.
- Shard engagement counters.
- Aggregate watch events asynchronously.
- Protect ranking feature store from hot-key reads with cached aggregates.

## 5.2 Watch Event Flood

Problem:

- Every impression generates events, and viral sessions multiply event volume.

Handling:

- Use high-throughput event collectors.
- Batch client events.
- Use durable streams with partitioning by `videoId` or `viewerId`.
- Apply backpressure and sampling for non-critical analytics.
- Preserve critical events such as impressions, completions, reports, and not-interested.

## 5.3 Ranking Service Degradation

Handling:

- Fallback to trending plus following plus recent popular videos.
- Reduce candidate source count.
- Use cached user embeddings/features.
- Skip expensive re-ranking passes.
- Keep safety filtering active.

## 5.4 CDN Or Playback Issues

Handling:

- Multi-CDN failover.
- Return lower bitrate variants.
- Refresh playback URLs during hydration.
- Detect regional playback errors and route around bad CDN POPs.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| video metadata | `videoId` | direct hydration lookup |
| creator video timeline | `creatorId + time` | profile feed |
| watch events | `viewerId` or `videoId + time` | user features vs video aggregates |
| feed sessions | `viewerId` or `sessionId` | pagination and dedup |
| feature store | `viewerId`, `videoId`, feature family | ranking latency |
| candidate indexes | topic, embedding cluster, region | retrieval recall |

## 6.2 Multi-Region Strategy

- Serve feed from nearest region with replicated video metadata.
- Serve video bytes through CDN and object storage replication.
- Keep user features close to user's home region.
- Use region/language candidate pools.
- Replicate moderation/delete decisions quickly.

## 6.3 Recommendation Architecture

Offline:

- Train embeddings and ranking models.
- Build candidate indexes by topic, sound, creator, embedding cluster, region, freshness.
- Precompute user and video features.

Online:

- Fetch candidates from multiple sources.
- Apply request/session filters.
- Score top candidates.
- Re-rank for diversity and exploration.
- Store served session queue.

## 6.4 Observability

Track:

- feed metadata latency,
- video playback startup time,
- CDN cache hit rate,
- ranking timeout rate,
- candidate source latency,
- watch-event ingestion lag,
- duplicate/repeated video rate,
- safety-filter drop rate,
- model fallback rate,
- completion/skip metrics by cohort.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify For You vs Following feed.
2. Separate video upload/playback from feed ranking.
3. Design video metadata, object storage, transcoding, and CDN at a high level.
4. Design candidate generation from multiple sources.
5. Add multi-stage ranking and eligibility filtering.
6. Add watch-event ingestion and feature feedback loop.
7. Add caching, consistency, and session storage.
8. Discuss viral video spikes and billion-user scaling.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| feed generation | following fanout | recommendation pull | recommendation pull for For You, hybrid for Following |
| ranking | single-stage | multi-stage | multi-stage |
| video serving | app servers | CDN | CDN |
| watch events | synchronous processing | async stream | async stream |
| consistency | strong ranking | eventual features | eventual features, strong safety |

## 7.3 Common Mistakes

- Treating TikTok like a simple follower timeline only.
- Serving video bytes through feed service.
- Scoring every video online.
- Ignoring watch-event scale.
- Showing cached videos without final safety checks.
- Forgetting session dedup, causing repeated videos.

## 7.4 Strong Closing

> TikTok is primarily a recommendation-serving system. The feed service creates a ranked session queue from multiple candidate sources, filters unsafe or already-seen videos, hydrates playback metadata, uses CDN for bytes, and records watch events asynchronously so future rankings improve.

---

# 8. Fast Recall Rules

- For You feed is recommendation pull, not pure social fanout.
- Following feed can use hybrid fanout.
- Video bytes live in object storage/CDN.
- Feed returns metadata and playback URLs.
- Use multi-stage ranking.
- Watch events are the feedback loop.
- Store feed session state to avoid repeats.
- Cache video metadata, features, trending candidates, and CDN segments separately.
- Safety filtering is mandatory before serving.
- Viral videos stress CDN, counters, events, and feature stores.