# Recommendation System - End-to-End System Design

> Goal: design a generic recommendation platform from event collection to candidate generation, feature stores, ranking, exploration, feedback loops, and large-scale serving.

---

## How To Use This File

- Use this when the interview asks for recommendations, personalized feed, similar products, people you may know, movies/music/videos to recommend, or ranking systems.
- Start with what is being recommended and what success means, then split the system into event collection, offline training, candidate generation, ranking, serving, and feedback.
- Keep one idea sharp: recommendation systems use multi-stage retrieval. Generate a manageable candidate set first, then rank it.
- In interviews, avoid jumping straight to ML. Explain data, features, freshness, safety, serving latency, and feedback loops.

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

| Layer | Interview signal | Recommendation focus |
|---|---|---|
| Problem understanding | Can define goal and surface | recommend products/videos/friends/posts, optimize clicks/watch/purchase |
| HLD | Can separate offline and online | event pipeline, feature store, candidate generation, ranking, serving |
| LLD | Can model recommendation entities | `User`, `Item`, `Event`, `FeatureVector`, `Candidate`, `RankedItem` |
| Machine coding | Can implement simple ranker | collaborative filtering, content score, weighted ranking |
| Traffic spikes | Can protect serving | cached candidates, model fallback, feature-store degradation |
| Scale | Can reason feedback loops | embeddings, approximate nearest neighbor, exploration, cold start |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Recommend items to a user.
- Support multiple recommendation surfaces, such as home feed, similar items, trending, cart recommendations, or notifications.
- Collect user events: impression, click, view, watch time, like, share, purchase, skip, hide.
- Collect item metadata: category, text, image/video/audio features, creator/seller, price, tags.
- Generate candidate items from multiple sources.
- Rank candidates for a user/context.
- Filter unsafe, unavailable, duplicate, already-seen, or blocked items.
- Support pagination/session-based serving.
- Log impressions and outcomes for training and evaluation.
- Support cold-start users and cold-start items.

Optional requirements to clarify:

- What domain is this: e-commerce, video, music, social, jobs, news?
- What metric matters most: click, purchase, watch time, retention, revenue, diversity?
- Should recommendations be real-time or refreshed periodically?
- Are ads/sponsored items included?
- Is user privacy/regional compliance required?
- Are explanations needed, such as "because you watched..."?
- Do we need exploration of new items?

Out of scope unless interviewer asks:

- Full deep-learning model architecture.
- Full feature engineering platform.
- Full A/B testing platform implementation.
- Full moderation platform.

## 1.2 Non-Functional Requirements

Latency:

- Online recommendation serving should be low latency.
- Candidate generation and ranking must complete within the page/feed SLA.
- Heavy training and embedding computation should happen offline/nearline.

Availability:

- Recommendation serving should degrade gracefully.
- If personalization is down, return trending/popular/fallback items.
- Do not block core product flows on non-critical recommendations.

Quality:

- Results should be relevant, diverse, fresh, and safe.
- Avoid repetitive recommendations.
- Avoid recommending unavailable/deleted/private items.

Scalability:

- Event volume can be huge.
- Candidate generation across millions/billions of items requires indexes.
- Ranking features must be available quickly.

Safety/privacy:

- Respect blocked users, private content, age/region restrictions.
- Avoid leaking sensitive user behavior.
- Apply policy filters before serving.

## 1.3 Constraints

- Scoring every item online is impossible.
- User interests change quickly.
- Event data can be delayed, duplicated, or noisy.
- Feedback loops can over-amplify popular items.
- New users/items have little historical data.
- ML model quality depends on feature freshness and logging quality.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Users | 500M+ |
| Items | 100M to 1B+ |
| Events/day | 10B+ |
| Recommendation requests/day | 5B+ |
| Peak requests/sec | 500K+ globally |
| Candidates generated/request | 500 to 5000 |
| Final returned items | 10 to 100 |
| P95 serving latency | 100 to 300 ms depending surface |

Back-of-the-envelope:

- `5B requests/day` is about `58K/sec` average.
- Event volume is usually larger than request volume.
- Online system should score hundreds/thousands of candidates, not all items.
- Offline/nearline systems precompute embeddings, features, and candidate pools.

## 1.5 Clarifying Questions To Ask

- What are we recommending?
- What user action defines success?
- Is this a feed, carousel, similar-items module, or notification?
- How fresh must recommendations be?
- How much personalization is required?
- Do we need diversity/exploration constraints?
- Are safety, privacy, or business rules strict?
- What latency budget does the surface have?

Strong interview framing:

> I will design recommendations as a multi-stage system. Offline and streaming pipelines collect events and build features. Online serving generates candidates from several sources, filters them, ranks a small set, applies business/safety rules, and logs feedback for continuous improvement.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Clients
  |
  v
Recommendation API
  |
  +--> User Profile Service
  +--> Candidate Generation Service
  |      +--> Collaborative Filtering Index
  |      +--> Embedding ANN Index
  |      +--> Trending/Popular Index
  |      +--> Graph Candidate Service
  |
  +--> Feature Store
  +--> Ranking Service
  +--> Filtering/Policy Service
  +--> Diversity/Blending Service
  +--> Impression Logger

Event Pipeline:
  Clients -> Event Collector -> Stream -> Feature Updates -> Data Lake
                                      -> Model Training -> Model Registry
                                      -> Embedding Builder -> ANN Index
```

Online serving path:

```text
User request
  -> fetch user/context features
  -> generate candidates from multiple sources
  -> dedupe/filter candidates
  -> score/rank top candidates
  -> apply diversity/business rules
  -> return recommendations
  -> log impressions
```

Offline/nearline path:

```text
Events -> data lake -> training data -> models/embeddings -> candidate indexes -> serving
```

## 2.2 APIs

Get recommendations:

```http
POST /v1/recommendations

{
  "userId": "user_1",
  "surface": "HOME_FEED",
  "context": {
    "locale": "en-US",
    "device": "mobile",
    "timeOfDay": "evening"
  },
  "limit": 20,
  "cursor": "session_cursor"
}
```

Response:

```json
{
  "items": [
    {
      "itemId": "item_1",
      "score": 0.93,
      "reason": "similar_to_recent_activity"
    }
  ],
  "nextCursor": "cursor_2"
}
```

Record event:

```http
POST /v1/recommendation-events

{
  "userId": "user_1",
  "itemId": "item_1",
  "eventType": "CLICK",
  "surface": "HOME_FEED",
  "requestId": "req_1",
  "position": 3,
  "timestamp": "2026-06-17T10:00:00Z"
}
```

Similar items:

```http
GET /v1/items/{itemId}/similar?limit=20
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| Event Collector | collects impressions/actions |
| Stream Processor | cleans, dedupes, aggregates recent events |
| Data Lake | stores long-term training data |
| Feature Store | serves offline and online features |
| Candidate Generation | retrieves possible items |
| ANN Index | nearest-neighbor lookup over embeddings |
| Graph Candidate Service | friends-of-friends, co-engagement graph |
| Ranking Service | scores candidates |
| Filter/Policy Service | removes unsafe/unavailable/seen items |
| Diversity/Blending | balances sources and avoids repetition |
| Model Registry | versions and deploys models |
| Experiment Service | A/B tests models and strategies |

### Candidate Generation

Candidate sources:

| Source | Example |
|---|---|
| collaborative filtering | users like you liked these |
| content-based | items similar to what user consumed |
| graph-based | friends liked, people you may know |
| trending/popular | popular in region/category |
| fresh items | new uploads/products |
| business candidates | sponsored, inventory goals, promotions |
| continue/revisit | unfinished video/course/cart item |

Why multiple sources:

- One source misses good candidates.
- Cold-start requires fallback sources.
- Blending improves diversity.
- Business/safety constraints differ by source.

### Ranking

Ranking inputs:

- user features: interests, demographics if allowed, recent behavior
- item features: category, quality, freshness, popularity
- context features: device, time, location, network
- interaction features: user-item similarity, creator affinity, price sensitivity
- business/safety features: availability, policy risk, promotion

Ranking stages:

```text
Candidate generation: 500 to 5000 items
Light ranker: top 500
Heavy ranker: top 100
Reranker/diversifier: final 20
```

### Filtering

Hard filters:

- item deleted/unavailable
- private/restricted content
- blocked creator/user
- already purchased when not useful
- already seen too many times
- unsafe or policy-violating
- region/age restrictions

Soft constraints:

- diversity by category/creator
- freshness boost
- exploration quota
- sponsored item spacing

### Feedback Loop

Events feed future recommendations:

```text
impression -> click/watch/purchase/skip -> feature updates -> retraining -> new model/index
```

Important:

- Log impressions, not just clicks.
- Without impressions, you cannot tell what the user ignored.
- Avoid training only on positive feedback because it creates bias.

## 2.4 Data Layer

Storage choices:

| Data | Storage |
|---|---|
| raw events | append-only event log + data lake |
| user profile | KV/document store |
| item metadata | document/relational store |
| online features | low-latency feature store |
| offline features | data lake/warehouse |
| embeddings | vector/ANN index |
| candidate lists | KV store/cache |
| model artifacts | model registry/object storage |
| experiment assignments | KV store |

Core entities:

```text
User(userId, locale, preferences, privacySettings)
Item(itemId, type, metadata, status, createdAt, qualityScore)
Event(eventId, userId, itemId, type, surface, requestId, position, timestamp)
FeatureVector(entityId, featureNames, values, version)
Candidate(itemId, source, rawScore)
RankedItem(itemId, finalScore, reasons, rankPosition)
Model(modelId, version, features, createdAt)
```

## 2.5 Scalability

Serving scale:

- Cache precomputed user candidates.
- Cache popular/trending lists by region/category.
- Use approximate nearest neighbor indexes for embeddings.
- Limit candidate count.
- Batch feature fetches.
- Use model serving replicas.

Event scale:

- Use append-only event streams.
- Partition by user ID or item ID depending aggregation.
- Deduplicate client retries by event ID.
- Use stream processors for near-real-time features.

Feature scale:

- Store hot online features in low-latency KV.
- Precompute expensive features offline.
- Keep feature versions compatible with model versions.

## 2.6 Caching

Good cache candidates:

- trending items by region/category
- popular fallback lists
- user candidate pools
- item embeddings
- item metadata
- similar-item results
- experiment assignments

Cache cautions:

- Personalized ranked lists can go stale quickly.
- Recommending deleted/unsafe items is unacceptable, so apply serving filters.
- Session-level recommendations may need anti-repeat memory.

## 2.7 Async Systems

Events:

| Event | Consumers |
|---|---|
| ImpressionLogged | analytics/training |
| ClickLogged | feature update |
| PurchaseLogged | user profile, training |
| ItemCreated | candidate indexes, cold-start pipeline |
| ItemDeleted | cache/index invalidation |
| UserPreferenceChanged | profile update |
| ModelPublished | serving deployment |

Pipelines:

- real-time feature updates for recent user behavior
- batch training for long-term models
- embedding generation for users/items
- ANN index build and publish
- offline evaluation and A/B testing

## 2.8 Safety And Failure Handling

| Failure | Mitigation |
|---|---|
| feature store down | fallback to cached features/trending |
| ranking model slow | use lightweight ranker |
| candidate source down | blend remaining sources |
| ANN index stale | serve previous index version |
| event pipeline delayed | continue serving with older features |
| item deleted after caching | final serving filter |
| model bad rollout | rollback model version |
| feedback loop amplifies bad item | policy/quality caps, exploration limits |

## 2.9 Observability

Serving metrics:

- recommendation QPS
- p50/p95/p99 latency
- candidate generation latency
- feature fetch latency
- ranking latency
- fallback rate
- empty result rate

Quality metrics:

- click-through rate
- conversion/watch completion
- dwell time
- hide/skip/report rate
- diversity
- freshness
- long-term retention

Pipeline metrics:

- event ingestion lag
- feature freshness
- training job success
- model deployment status
- ANN index build lag

Logs/traces:

- `requestId`
- `userId`
- `surface`
- `candidateSources`
- `modelVersion`
- `featureVersion`
- `experimentId`
- `returnedItemIds`

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| precomputed recommendations | low latency | less fresh |
| online ranking | personalized and fresh | higher latency/cost |
| collaborative filtering | strong behavior signal | cold-start issues |
| content-based | handles new items better | can be repetitive |
| popularity fallback | robust and simple | not personalized |
| exploration | discovers interests/items | may reduce immediate CTR |
| heavy ML model | better quality | operational complexity |
| simple heuristic ranker | explainable and cheap | lower ceiling |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
User
Item
RecommendationRequest
UserEvent
FeatureVector
Candidate
CandidateSource
RankingModel
RankedItem
FilterPolicy
ExperimentAssignment
RecommendationResponse
```

## 3.2 OOP Fundamentals

Encapsulation:

- `CandidateSource` owns retrieval from one source.
- `RankingModel` owns scoring.
- `FilterPolicy` owns hard exclusion rules.
- `RecommendationService` owns orchestration.

Polymorphism:

- `CandidateSource`: collaborative, content, graph, trending, fresh.
- `RankingModel`: heuristic, linear model, ML model wrapper.
- `FilterPolicy`: availability, safety, seen-item, region.

Composition:

- `RecommendationService` composes sources, feature store, ranker, filters, diversifier, and logger.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Strategy | candidate source and ranking model |
| Pipeline | candidate -> filter -> rank -> rerank |
| Composite | combine multiple filters |
| Adapter | model serving backend |
| Repository | feature/item/user storage |
| Observer | event-driven feature updates |

## 3.4 Sequence Diagram

Online serving:

```text
Client
  -> RecommendationService: recommend(user, surface)
  -> UserProfile: fetch profile
  -> CandidateSources: get candidates
  -> FilterPolicy: remove invalid items
  -> FeatureStore: batch fetch features
  -> RankingModel: score candidates
  -> Diversifier: final ordering
  -> ImpressionLogger: log returned items
  -> Client: recommendations
```

Offline training:

```text
EventStream
  -> DataLake: raw events
  -> FeatureJobs: build features
  -> TrainingJob: train model
  -> Evaluation: validate
  -> ModelRegistry: publish
  -> Serving: load model version
```

## 3.5 Edge Cases

- New user has no history.
- New item has no interactions.
- User has consumed all popular items.
- Candidate source returns duplicates.
- User blocks a creator after candidates are cached.
- Item becomes unavailable after ranking.
- Event logging misses impressions.
- Model over-recommends one category.
- Recommendation creates a popularity feedback loop.
- User context changes within session.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
models/
  user.py
  item.py
  event.py
recommendation/
  candidate_sources.py
  filters.py
  ranker.py
  service.py
```

## 4.2 Core Logic Implementation

Simple multi-source recommendation engine:

```python
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class Item:
    item_id: str
    category: str
    popularity: float
    quality: float
    available: bool = True


@dataclass(frozen=True)
class UserProfile:
    user_id: str
    preferred_categories: set[str]
    seen_item_ids: set[str]


@dataclass(frozen=True)
class Candidate:
    item: Item
    source: str
    source_score: float


@dataclass(frozen=True)
class RankedItem:
    item_id: str
    score: float
    reason: str


class CandidateSource(Protocol):
    def candidates(self, user: UserProfile) -> list[Candidate]:
        ...


class TrendingSource:
    def __init__(self, items: list[Item]) -> None:
        self.items = items

    def candidates(self, user: UserProfile) -> list[Candidate]:
        return [
            Candidate(item=item, source="trending", source_score=item.popularity)
            for item in self.items
        ]


class CategorySource:
    def __init__(self, items: list[Item]) -> None:
        self.items = items

    def candidates(self, user: UserProfile) -> list[Candidate]:
        return [
            Candidate(item=item, source="category", source_score=1.0)
            for item in self.items
            if item.category in user.preferred_categories
        ]


class RecommendationService:
    def __init__(self, sources: list[CandidateSource]) -> None:
        self.sources = sources

    def recommend(self, user: UserProfile, limit: int) -> list[RankedItem]:
        by_item_id: dict[str, Candidate] = {}

        for source in self.sources:
            for candidate in source.candidates(user):
                existing = by_item_id.get(candidate.item.item_id)
                if existing is None or candidate.source_score > existing.source_score:
                    by_item_id[candidate.item.item_id] = candidate

        ranked: list[RankedItem] = []
        for candidate in by_item_id.values():
            item = candidate.item
            if not item.available:
                continue
            if item.item_id in user.seen_item_ids:
                continue

            category_boost = 1.5 if item.category in user.preferred_categories else 1.0
            score = (
                0.45 * candidate.source_score
                + 0.35 * item.quality
                + 0.20 * item.popularity
            ) * category_boost

            ranked.append(RankedItem(item.item_id, score, candidate.source))

        ranked.sort(key=lambda item: item.score, reverse=True)
        return ranked[:limit]
```

What this demonstrates:

- Multiple candidate sources feed one service.
- Candidates are deduplicated.
- Hard filters remove unavailable/seen items.
- Ranking combines source score, quality, popularity, and user preference.
- Real systems replace simple scoring with feature stores and model serving.

## 4.3 Collaborative Filtering Intuition

Simple item-to-item co-occurrence:

```python
from collections import defaultdict


def similar_items(user_histories: list[list[str]]) -> dict[str, list[tuple[str, int]]]:
    counts: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

    for history in user_histories:
        unique_items = list(set(history))
        for item_a in unique_items:
            for item_b in unique_items:
                if item_a != item_b:
                    counts[item_a][item_b] += 1

    return {
        item: sorted(related.items(), key=lambda pair: pair[1], reverse=True)
        for item, related in counts.items()
    }
```

Interview explanation:

- If many users interact with A and B together, B can be recommended to users who liked A.
- This is a classic collaborative filtering signal.
- It struggles with new items and popularity bias, so combine it with content-based and exploration sources.

## 4.4 Testing Thinking

Test cases:

- Cold user receives trending fallback.
- Preferred category boosts relevant items.
- Seen items are filtered.
- Unavailable/deleted items are filtered.
- Duplicate candidates are deduped.
- Ranking order is deterministic for same inputs.
- Candidate-source failure still returns fallback results.
- Impression logging includes request ID and positions.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Viral item causes candidate hot key.
- App open spike after notification.
- Feature store latency spike.
- Model serving latency spike.
- Event pipeline lag.
- New content flood.
- Bad model rollout.

## 5.2 Immediate Response

- Serve cached candidate pools.
- Fall back to trending/popular lists.
- Disable expensive candidate sources.
- Use lightweight ranker.
- Batch feature fetches.
- Roll back bad model version.
- Keep safety filters active.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| feature store down | cached features or popularity fallback |
| ANN index down | use trending/category/graph sources |
| ranker slow | heuristic ranker |
| event pipeline delayed | serve older features |
| item metadata slow | skip unknown items |
| policy service slow | fail closed for risky items |

## 5.4 Spike Interview Answer

> During a recommendation spike, I would keep serving responsive by using cached candidates, fallback lists, and a simpler ranker. I would degrade personalization before violating safety filters. Event delays affect future quality, but the online path can continue with older features and model versions.

---

# 6. Global Scale

## 6.1 Regional Serving

```text
Global traffic
  -> regional recommendation API
  -> regional feature cache
  -> regional candidate indexes
  -> model serving replicas
```

Principles:

- Keep online serving close to users.
- Replicate model artifacts and candidate indexes by region.
- Use regional trending lists.
- Keep privacy and residency constraints in mind.
- Send events to regional streams and global/offline lake if allowed.

## 6.2 Embeddings And ANN

Embedding flow:

```text
events + metadata
  -> training
  -> user/item embeddings
  -> approximate nearest neighbor index
  -> candidate generation
```

Why ANN:

- Exact nearest-neighbor over millions/billions of items is too slow.
- ANN trades a little recall for much faster candidate retrieval.

## 6.3 Exploration And Cold Start

Cold-start strategies:

- new user: ask preferences, use location/trending/popular
- new item: content-based features, creator/category boost, exploration traffic
- sparse domain: use editorial/business rules

Exploration:

- Reserve a small percentage of slots for uncertain/new items.
- Measure outcomes carefully.
- Prevent low-quality items from overexposure.

## 6.4 Interview Answer

> At global scale, I would precompute embeddings and candidate pools offline/nearline, replicate candidate indexes and models regionally, and keep online serving to candidate retrieval plus ranking. The system falls back to trending/popular items when personalization is unavailable, and it logs impressions/actions to close the feedback loop.

---

# 7. Final Interview Playbook

Start with:

> Recommendations are multi-stage. We cannot score every item online, so we generate candidates from multiple sources, filter them, rank a smaller set, diversify the final list, and log feedback.

Then cover:

1. Recommendation goal and success metric.
2. Event collection and impression logging.
3. Feature store and offline/nearline pipelines.
4. Candidate generation sources.
5. Ranking and reranking.
6. Filters, safety, diversity, and business rules.
7. Cold start and exploration.
8. Fallbacks, spikes, and global serving.

Common traps:

- Scoring all items online.
- Ignoring impression logging.
- Optimizing only clicks and hurting long-term quality.
- Forgetting cold start.
- Forgetting safety/privacy filters.
- Treating model training as the whole system.

---

# 8. Fast Recall Rules

- Recommendation = candidates -> filters -> rank -> diversify -> log.
- Generate hundreds/thousands, rank tens/hundreds.
- Events and impressions are the fuel.
- Feature freshness affects quality.
- Cold start needs fallback and content signals.
- Collaborative filtering is strong but biased toward known items.
- ANN retrieves embedding neighbors efficiently.
- Exploration prevents the system from getting stuck.
- Degrade personalization, not safety.
- Always log what was shown, not only what was clicked.

