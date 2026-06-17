# Netflix - End-to-End System Design

> Goal: practice one complete E2E premium video streaming problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for subscription video-on-demand systems.
- Start broad with requirements and scale, then zoom into catalog ingestion, encoding, storage, CDN delivery, playback authorization, DRM, personalization, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Netflix-style systems, optimize playback reliability, low startup latency, device compatibility, CDN efficiency, rights enforcement, and personalized discovery.

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

| Layer | Interview signal | Netflix system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | subscription VOD, catalog, profiles, playback, recommendations, continue watching |
| HLD | Can design scalable streaming systems | catalog service, encoding pipeline, manifest service, DRM/license service, CDN, personalization |
| LLD | Can model maintainable components | `Title`, `Asset`, `EncodingProfile`, `PlaybackSession`, `Entitlement`, `ViewingProgress` |
| Machine coding | Can implement critical path | authorize playback, select manifest, issue signed CDN URLs, record progress |
| Traffic spikes | Can protect production | new release surges, regional prime-time peaks, CDN misses, entitlement provider issues |
| Global scale | Can reason across regions | edge delivery, regional catalog rights, multi-device playback, telemetry streams |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can browse a personalized catalog.
- Users can search titles and view title details.
- Users can start playback for available titles.
- System enforces subscription, profile, maturity, region, and licensing restrictions.
- Player receives an adaptive bitrate manifest and plays chunks from CDN.
- System supports DRM license acquisition for protected content.
- System stores viewing progress and supports continue watching.
- System records playback quality telemetry for monitoring and recommendations.

Optional requirements to clarify:

- Are downloads/offline viewing in scope?
- Do we support live events or only VOD?
- Are multiple profiles per account required?
- Do we need household/device limits?
- Should we include billing/subscription lifecycle?
- Should recommendations include offline model training or only online serving?

Out of scope unless interviewer asks:

- Full billing and payments.
- Full ML training pipeline.
- Studio contract management internals.
- Full ad-supported plan logic.
- Full live-streaming low-latency pipeline.

## 1.2 Non-Functional Requirements

Playback:

- Very low playback start failure rate.
- Low startup latency and minimal rebuffering.
- High availability during regional peak hours.
- Adaptive bitrate across devices and networks.

Rights and security:

- Strict entitlement, maturity, and regional rights enforcement.
- DRM support for protected premium content.
- Short-lived signed URLs and licenses.

Catalog and personalization:

- Low-latency home page and title detail reads.
- Personalization can be eventually consistent.
- Viewing progress should be durable and monotonic per profile/title.

## 1.3 Constraints

- Content is professionally ingested, not uploaded by every user.
- Catalog rights vary by country, plan, profile maturity, and time window.
- Video assets are huge and must be served from CDN/edge.
- DRM license service is on the playback critical path.
- Device capability matrix is complex: codec, resolution, HDR, audio, DRM support.
- Evening release windows can create predictable but massive traffic spikes.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered profiles | 500 million |
| DAU | 150 million |
| Peak concurrent streams | 50 million |
| Catalog titles | 100K-1M depending regions/assets |
| Average playback bitrate | 3-8 Mbps |
| Segment duration | 2-6 seconds |
| Playback start target | under 2 seconds p95 |
| Playback availability target | 99.99%+ |
| Progress write QPS | millions/sec globally at peak |

## 1.5 Capacity Math

Back-of-the-envelope:

- `50M concurrent streams * 5 Mbps` is `250 Tbps` of edge delivery at peak.
- Segment requests/sec can be huge: with 4-second segments, `50M / 4 = 12.5M segment requests/sec`.
- Playback start QPS is much lower than segment QPS, but it is more latency-sensitive because entitlement, manifest, and license are involved.
- Viewing progress writes should be batched/throttled because players can emit frequent updates.
- CDN hit ratio is critical; a 1% origin miss rate at this scale is still enormous origin traffic.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Segment duration | 2-6 seconds |
| Manifest response | KB-scale |
| DRM license TTL | minutes to hours depending policy |
| Progress update interval | 10-60 seconds or milestone-based |
| Telemetry lag tolerance | seconds to minutes |
| Catalog cache TTL | seconds to minutes for policy-sensitive views |

## 1.6 Clarifying Questions To Ask

- Are we designing playback only, or catalog plus recommendations too?
- Is DRM mandatory for every title?
- Do regional licensing restrictions apply?
- How many devices/profiles per account?
- What consistency is required for continue watching?
- Are offline downloads in scope?

Strong interview framing:

> I will design Netflix as a premium VOD system with a strict playback control plane and a CDN-heavy data plane. The critical path is entitlement, device-aware manifest selection, DRM license, and CDN chunk delivery; recommendations, telemetry, and progress updates are asynchronous or eventually consistent where possible.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Catalog browse flow:
Client -> API Gateway -> Profile/Auth
       -> Home/Recommendation Service
       -> Catalog Service + Rights Filter
       -> ranked rows/title cards

Playback flow:
Client -> Playback Service
       -> Entitlement/Rights/Profile check
       -> Manifest Service
       -> DRM License Service
       -> signed CDN manifest/chunks
       -> Player fetches segments from CDN
       -> Telemetry + progress streams

Content ingestion flow:
Studio assets -> Ingestion Service -> Object Store originals
              -> Encoding Pipeline -> Renditions/Manifests
              -> QC/Policy -> Catalog publish
```

Recommended architecture:

```text
Client Apps
  |
  v
+-----------------------+
| API Gateway + Auth    |
+-----------+-----------+
            |
            +-------------------------+-------------------------+
            |                         |                         |
            v                         v                         v
+-----------------------+   +----------------------+   +----------------------+
| Catalog/Home Service  |   | Playback Service     |   | Progress Service     |
| recs + rights filter  |   | sessions/manifests   |   | continue watching    |
+-----------+-----------+   +----------+-----------+   +----------+-----------+
            |                          |                          |
            v                          v                          v
+-----------------------+   +----------------------+   +----------------------+
| Catalog DB + Search   |   | Manifest + DRM Svc   |   | Progress Store       |
+-----------------------+   +----------+-----------+   +----------------------+
                                       |
                                       v
                            +----------------------+
                            | CDN / Edge Cache     |
                            | segments/subtitles   |
                            +----------+-----------+
                                       |
                                       v
                            +----------------------+
                            | Origin Object Store  |
                            +----------------------+

Ingestion/Encoding:
Studio assets -> Ingest -> Processing Queue -> Encode/QC Workers -> Asset Store -> Catalog Publish
```

Request flow for playback:

1. Client requests playback for `titleId`, `profileId`, and device capabilities.
2. Playback Service validates session and subscription.
3. Entitlement Service checks region, license window, maturity rating, plan, and device policy.
4. Manifest Service selects compatible renditions, audio tracks, subtitles, and DRM metadata.
5. DRM License Service issues a license or token for the playback session.
6. Client receives manifest URL, license URL/token, and telemetry session ID.
7. Player fetches segments from CDN and emits progress/quality telemetry.

Request flow for catalog browse:

1. Client opens home page.
2. Home Service fetches candidate rows from Recommendation Service.
3. Catalog Service filters titles by region, plan, maturity, and availability.
4. Ranking service orders items and returns title cards.
5. Response is cached briefly per profile/context.

## 2.2 APIs

### Home Page

```http
GET /v1/profiles/{profileId}/home?device=tv&country=US
Authorization: Bearer <token>
```

Response:

```json
{
  "profileId": "prof-1",
  "rows": [
    {
      "rowId": "continue-watching",
      "title": "Continue Watching",
      "items": [{"titleId": "t-100", "progressMs": 1800000}]
    }
  ]
}
```

### Title Detail

```http
GET /v1/titles/{titleId}?profileId=prof-1
```

### Start Playback

```http
POST /v1/playback-sessions
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "profileId": "prof-1",
  "titleId": "t-100",
  "device": {
    "type": "tv",
    "supportedCodecs": ["h264", "hevc"],
    "drm": "widevine",
    "maxResolution": "4k"
  }
}
```

Response:

```json
{
  "playbackSessionId": "pbs-777",
  "manifestUrl": "https://cdn.example/t-100/master.mpd?sig=...",
  "licenseUrl": "https://api.example/v1/drm/licenses/pbs-777",
  "telemetryToken": "tel-abc",
  "expiresInSeconds": 300
}
```

### DRM License

```http
POST /v1/drm/licenses/{playbackSessionId}
Authorization: Bearer <token>
```

### Update Progress

```http
PUT /v1/profiles/{profileId}/progress/{titleId}
```

```json
{
  "playbackSessionId": "pbs-777",
  "positionMs": 1840000,
  "durationMs": 3600000,
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

### Playback Telemetry

```http
POST /v1/playback-telemetry
```

```json
{
  "playbackSessionId": "pbs-777",
  "eventType": "REBUFFER",
  "positionMs": 1850000,
  "quality": "1080p",
  "bufferMs": 120
}
```

Important API points:

- Start playback is control plane; segment bytes go through CDN.
- Playback session tokens should be short-lived.
- DRM license issuance is separate but tied to entitlement.
- Progress writes should be monotonic and retry-safe.
- Telemetry should be batched and never block playback.

## 2.3 Core Components

Think of Netflix as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Catalog plane | titles, artwork, metadata, regional availability | accurate browsable catalog |
| Playback control plane | entitlement, manifest, DRM, playback sessions | secure playback start |
| Delivery plane | CDN edge, segments, subtitles, audio tracks | high-quality streaming |
| Personalization plane | home rows, ranking, continue watching | relevant discovery |
| Ingestion plane | studio assets, encoding, QC, publish | high-quality prepared assets |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| API Gateway | auth, routing, rate limits | media chunks | request QPS |
| Catalog Service | title metadata and availability | playback license issuing | catalog reads |
| Rights Service | region/license/maturity/plan rules | segment delivery | entitlement QPS |
| Playback Service | playback session orchestration | CDN caching | playback starts |
| Manifest Service | device-aware manifest selection | user subscription state | manifest QPS |
| DRM License Service | license/token issuance | recommendations | license QPS |
| CDN/Edge | media segment delivery | catalog truth | segment traffic |
| Progress Service | continue watching state | raw telemetry analysis | progress writes |
| Recommendation Service | personalized rows/ranking | rights truth | home requests |
| Encoding Pipeline | renditions, subtitles, QC | user playback sessions | asset processing jobs |

### Catalog And Rights Service

Why it exists:

- Not every title is available to every user.
- Rights vary by region, time, plan, maturity rating, and device capability.
- Home/search results must filter unavailable titles.

Core responsibilities:

- Store title metadata, seasons, episodes, artwork, genres, and availability windows.
- Apply regional rights and maturity filters.
- Provide catalog details to home/search/playback services.
- Emit catalog update events to caches and search indexes.

Failure behavior:

- If recommendation service is down, catalog can still serve fallback rows.
- If rights checks are unavailable, fail closed for playback and use cached allowed catalog for browse where safe.

Interview signal:

> Catalog tells what exists; Rights Service tells whether this profile can see or play it now.

### Playback Service

Why it exists:

- Playback start combines subscription, profile, rights, device, manifest, DRM, and telemetry setup.
- It must stay fast and reliable.

Core responsibilities:

- Create playback session.
- Validate account/profile/subscription state.
- Call Rights Service for title availability.
- Select compatible manifest through Manifest Service.
- Attach DRM license metadata.
- Return signed manifest/CDN URLs.
- Emit playback-start event.

Failure behavior:

- Retry safe dependencies with short timeouts.
- Use cached entitlement for brief periods only where policy allows.
- If manifest variant is unavailable, fall back to lower quality.

Interview signal:

> Playback Service is the control plane for starting playback; the CDN is the data plane for video bytes.

### Manifest, Encoding, And Streaming Protocols

Core idea:

- Prepare many encodes ahead of time.
- Store chunks for different codecs, resolutions, audio tracks, subtitles, and HDR formats.
- Return only the variants a device can play.

Typical assets:

| Asset | Purpose |
|---|---|
| video renditions | multiple bitrates/resolutions/codecs |
| audio tracks | languages, surround formats |
| subtitles/captions | accessibility and localization |
| trick-play thumbnails | preview while seeking |
| manifests | HLS/DASH/CMAF playlists |

Device-aware manifest selection:

- Device max resolution.
- Supported codecs: H.264, HEVC, AV1 depending support.
- DRM system support.
- HDR/SDR capability.
- Audio format support.
- Network quality and plan limits.

Interview signal:

> For Netflix-style playback, manifest generation is not just a list of chunks; it is a policy and device-compatibility decision.

### DRM License Service

Why it exists:

- Premium content must be protected from unauthorized playback.
- The player needs a license/key to decrypt protected segments.

Core responsibilities:

- Verify playback session and entitlement.
- Issue short-lived licenses/tokens.
- Bind license to device/session where required.
- Enforce expiration and output restrictions if needed.
- Log security-relevant license events.

Failure behavior:

- License service degradation can block playback start, so it needs high availability.
- Use regional redundancy and carefully scoped caching where allowed.
- Fail closed if entitlement cannot be verified.

Interview signal:

> DRM is on the critical path for protected playback, so it must be highly available, but it should still fail closed for rights/security violations.

### CDN And Edge Delivery

Why it exists:

- Streaming is bandwidth-heavy and predictable at peak.
- Segments should be served close to users.
- Origin/object storage should be shielded from direct high-volume traffic.

Cache strategy:

| Layer | Stores | TTL |
|---|---|---:|
| Edge CDN | hot segments, subtitles, artwork | hours to days |
| Regional cache/origin shield | warm segments | hours to days |
| Origin object store | canonical assets | long-term durable |
| Service cache | catalog, rights hints, manifests | seconds to minutes |

Important points:

- Segments are immutable and cacheable.
- Manifests may be signed and short-lived.
- Popular releases can be pre-positioned at edge.
- Regional edges reduce startup latency and rebuffering.

### Personalization And Recommendations

Why it exists:

- Catalog is large and users need relevant rows.
- Personalization should account for profile history, context, availability, and diversity.

Serving stages:

```text
Home request
  -> fetch profile/context
  -> candidate generation
  -> rights/catalog filtering
  -> rank rows/items
  -> apply diversity/business rules
  -> return cached response
```

Signals:

- Watch history and completion.
- Search and browse interactions.
- My List / likes/dislikes.
- Similar profiles.
- Time of day and device.
- Regional trends.

Failure behavior:

- Fallback to popular/trending/continue-watching.
- Use cached rows for short TTL.
- Playback remains independent from recommendation availability.

### Viewing Progress

Why it exists:

- Continue watching is a core user experience.
- Progress writes are frequent and can arrive out of order from retries/devices.

Core responsibilities:

- Store latest position per profile/title.
- Use monotonic update rules with timestamps/session sequence.
- Remove from continue-watching when completed.
- Support cross-device resume.

Failure behavior:

- Lost progress event is tolerable; next event repairs it.
- Out-of-order updates should not move progress backward unless user intentionally seeks.

## 2.4 Data Layer

### Core Data Models

Title:

```json
{
  "titleId": "t-100",
  "type": "MOVIE",
  "name": "Example Movie",
  "maturityRating": "PG-13",
  "genres": ["Drama"],
  "runtimeMs": 7200000,
  "state": "PUBLISHED"
}
```

Availability:

```json
{
  "titleId": "t-100",
  "country": "US",
  "startsAt": "2026-01-01T00:00:00Z",
  "endsAt": "2026-12-31T23:59:59Z",
  "plans": ["standard", "premium"]
}
```

Asset rendition:

```json
{
  "titleId": "t-100",
  "assetId": "asset-4k-hevc",
  "codec": "hevc",
  "resolution": "3840x2160",
  "bitrateKbps": 16000,
  "drmRequired": true,
  "manifestPath": "titles/t-100/dash/manifest.mpd"
}
```

Viewing progress:

```json
{
  "profileId": "prof-1",
  "titleId": "t-100",
  "positionMs": 1840000,
  "durationMs": 7200000,
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Catalog metadata | document/relational DB | title detail and structured relationships |
| Rights windows | relational/rule store | correctness and auditable policy |
| Media assets | object storage + CDN | immutable high-bandwidth segments |
| Progress | KV/wide-column store | profile/title point lookup and high writes |
| Playback sessions | Redis/KV TTL store | short-lived session state |
| Telemetry | event stream + data lake | append-heavy analytics |
| Recommendations | feature store + cache | low-latency home serving |

Relational-style tables:

```sql
titles(title_id PK, type, name, maturity_rating, runtime_ms, state)
title_assets(title_id, asset_id, codec, resolution, bitrate_kbps, manifest_path, drm_required)
title_availability(title_id, country, starts_at, ends_at, plan)
profiles(profile_id PK, account_id, maturity_limit, language)
viewing_progress(profile_id, title_id, position_ms, updated_at)
```

Important indexes:

- `title_availability(country, starts_at, ends_at)` for regional filtering.
- `title_assets(title_id, codec, resolution)` for manifest selection.
- `viewing_progress(profile_id, updated_at DESC)` for continue watching.
- `titles(state, type)` for catalog indexing.

### Partitioning

- Partition media assets by hashed object path and distribute through CDN.
- Partition catalog reads by `titleId`, with denormalized title cards for browse.
- Partition progress by `profileId` because continue-watching is profile-centric.
- Partition telemetry by playback session or title depending consumer needs.
- Partition recommendations by profile for cache locality.

### Replication And Consistency

- Playback entitlement should be strongly checked or safely cached with tight TTL.
- Catalog browse can tolerate brief staleness.
- Progress is eventually consistent across devices but should converge quickly.
- Telemetry is append-only and eventually processed.
- Media segments are immutable and globally replicated/cached.

## 2.5 Scalability

### Horizontal Scaling

- Playback Service scales by playback starts.
- Manifest Service scales by manifest requests and cache misses.
- License Service scales by protected playback sessions.
- CDN scales by segment traffic.
- Progress Service scales by write QPS.
- Recommendation Service scales by home/detail page reads.

### Prime-Time Scaling

- Pre-position popular new releases near expected audiences.
- Warm catalog and manifest caches before release.
- Use regional capacity planning based on evening viewing curves.
- Keep progress/telemetry writes async and buffered.

### Hot Title Strategy

- Immutable segments with high cache TTL.
- Origin shield for cache miss collapse.
- Precomputed manifests for common device classes.
- Short TTL entitlement checks separate from long TTL media chunks.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Home page API | 100-300 ms |
| Start playback control plane | 100-500 ms |
| Manifest fetch | 20-100 ms |
| DRM license | 50-300 ms |
| First segment from CDN | 50-500 ms |
| Playback startup | under 2 seconds p95 |

### Optimization Rules

- Cache title cards and artwork heavily.
- Precompute common manifests by device class.
- Keep entitlement checks fast with indexed rules and bounded calls.
- Serve segments from edge and avoid origin fetches.
- Batch progress and telemetry updates.
- Use fallback rows when recommendation latency exceeds budget.

## 2.7 Async Systems

Use streams for:

- Catalog publish events.
- Asset encoding completion.
- Playback start events.
- Progress update events.
- Playback telemetry events.
- Recommendation feature updates.
- CDN log ingestion.

Queue notes:

- Encoding jobs are durable and retryable.
- Progress consumers should handle out-of-order updates.
- Telemetry consumers should be idempotent.
- Catalog publish should fan out to search, cache invalidation, and recommendation features.

## 2.8 Security, Privacy, And Abuse

Security:

- Strong authentication for account/profile APIs.
- Entitlement and rights checks before playback session creation.
- DRM licenses tied to playback session/device where needed.
- Signed manifest and segment URLs.
- Encryption in transit and at rest.

Privacy:

- Viewing history is sensitive personal data.
- Profiles may have separate maturity and personalization settings.
- Support deletion/export of viewing history if required.
- Avoid leaking regional title availability through unauthorized APIs.

Abuse controls:

- Device/session limits per account if product requires.
- Rate limit license requests and playback starts.
- Detect credential sharing or suspicious token reuse if in scope.
- Protect APIs from scraping catalog metadata.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Playback | start success rate, startup latency, rebuffer ratio, playback error rate |
| DRM | license latency, license failure rate, entitlement mismatch |
| CDN | edge hit ratio, origin egress, segment 4xx/5xx, regional saturation |
| Catalog | home API latency, title detail errors, rights filter failures |
| Progress | write latency, stale continue-watching rate, out-of-order drops |
| Encoding | job duration, QC failure rate, backlog |
| Recommendations | latency, fallback rate, row engagement |

Alerts:

- Playback start failures exceed SLO.
- DRM license failures spike.
- CDN hit ratio drops or origin egress spikes.
- Progress write lag grows.
- Catalog rights service errors increase.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Assets | pre-encode all variants | encode on demand | smooth playback vs storage/compute cost |
| Entitlement | strict live check | cached entitlement | correctness/security vs latency/availability |
| CDN | own/partner edge | direct origin | control/performance vs operational complexity |
| Manifest | precompute by device class | generate dynamically | low latency vs flexibility |
| Progress | every second write | batched updates | precision vs write load |
| Recs | personalized real-time | cached/fallback rows | relevance vs latency/reliability |

Interview framing:

> I would keep media segments immutable and edge-cached, make playback start a fast but strict control-plane flow, and move telemetry, progress aggregation, and recommendations to asynchronous paths with graceful fallbacks.

---

# 3. Low-Level Design

LLD goal:

> Model Netflix around catalog titles, rights rules, prepared assets, playback sessions, DRM licenses, and viewing progress.

Simple rules:

- Do not put segment bytes behind application services.
- Do not serve playback before entitlement succeeds.
- Keep progress updates monotonic and retry-safe.
- Keep recommendations independent from direct playback.

Starter map:

| LLD question | Netflix answer |
|---|---|
| What is durable? | catalog, rights rules, encoded assets, progress |
| What is ephemeral? | playback sessions, signed URLs, DRM license tokens |
| What is critical path? | entitlement, manifest, DRM, first segment |
| What can be stale? | recommendations, some catalog rows, telemetry |
| What must fail closed? | rights, subscription, maturity, DRM authorization |

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Account` | subscription and device limits | inactive account cannot start playback |
| `Profile` | maturity limit, language, preferences | profile filters catalog/playback |
| `Title` | metadata and publish state | unpublished title is not playable |
| `AvailabilityRule` | region/time/plan constraints | must be satisfied for playback |
| `AssetRendition` | codec/resolution/bitrate/path | must match device capability |
| `PlaybackSession` | profile/title/device/session TTL | created only after entitlement |
| `DrmLicense` | license state and expiration | tied to valid playback session |
| `ViewingProgress` | position per profile/title | should not move backward accidentally |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `CatalogService` | title and artwork data | issue DRM licenses |
| `RightsService` | availability and entitlement checks | rank recommendations |
| `PlaybackService` | create playback sessions | serve media segments |
| `ManifestService` | select compatible assets | check subscription itself |
| `LicenseService` | issue DRM license | decide catalog ranking |
| `ProgressService` | continue-watching state | ingest all telemetry analytics |
| `RecommendationService` | home rows and ranking | enforce final playback rights |

## 3.2 OOP Fundamentals

Encapsulation:

- `AvailabilityRule` owns whether it matches a playback context.
- `PlaybackSession` owns expiration and session validity.
- `ViewingProgress` owns monotonic update logic.

Abstraction:

- `RightsPolicy` hides plan/region/maturity rules.
- `ManifestSelector` hides device compatibility logic.
- `LicenseProvider` hides DRM vendor protocol details.

Polymorphism:

- Different `RightsPolicy` implementations for region, plan, maturity, and device.
- Different `ManifestSelector` implementations for HLS and DASH.
- Different `RecommendationStrategy` implementations for home, search, and similar titles.

Composition:

- `PlaybackService` composes account repository, rights service, manifest service, license service, and session store.

## 3.3 SOLID Principles

| Principle | Netflix application |
|---|---|
| Single Responsibility | `ProgressService` only owns resume state |
| Open/Closed | add new codec/device policy without rewriting playback |
| Liskov Substitution | any `LicenseProvider` must satisfy issue/renew contract |
| Interface Segregation | separate catalog, playback, progress, telemetry APIs |
| Dependency Inversion | playback depends on `RightsChecker`, not concrete rules DB |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | rights policies and manifest selection | choose behavior by context |
| Chain of Responsibility | entitlement checks | subscription -> region -> maturity -> device |
| State | playback session lifecycle | started, active, expired, revoked |
| Factory | DRM provider/license adapter | isolate provider-specific details |
| Observer/Event Publisher | progress/telemetry updates | decouple playback from analytics |

## 3.5 UML / Diagrams

### Playback Sequence

```text
Client -> PlaybackService: startPlayback(profileId, titleId, device)
PlaybackService -> AccountService: validate subscription
PlaybackService -> RightsService: check availability
PlaybackService -> ManifestService: select device-compatible manifest
PlaybackService -> SessionStore: create playback session
PlaybackService -> LicenseService: create license challenge metadata
PlaybackService -> Client: manifest URL + license URL
Client -> LicenseService: request DRM license
Client -> CDN: fetch manifest and segments
Client -> ProgressService: update progress
Client -> TelemetryIngest: send quality events
```

### Catalog Home Sequence

```text
Client -> HomeService: getHome(profileId)
HomeService -> RecommendationService: get candidates
HomeService -> CatalogService: fetch title cards
HomeService -> RightsService: filter unavailable titles
HomeService -> RankingService: rank/diversify rows
HomeService -> Client: home rows
```

## 3.6 Class Design

Interfaces:

```java
interface RightsChecker {
    boolean canPlay(Profile profile, Title title, PlaybackContext context);
}

interface ManifestSelector {
    PlaybackManifest select(Title title, DeviceCapabilities device);
}

interface PlaybackSessionStore {
    PlaybackSession create(Profile profile, Title title, DeviceCapabilities device);
    Optional<PlaybackSession> get(String playbackSessionId);
}

interface LicenseProvider {
    DrmLicense issue(PlaybackSession session, LicenseChallenge challenge);
}

interface ProgressRepository {
    void update(ViewingProgress progress);
    Optional<ViewingProgress> get(String profileId, String titleId);
}
```

Design notes:

- `canPlay()` should be deterministic and auditable.
- `select()` should return only compatible, ready assets.
- Progress update should handle retries and out-of-order timestamps.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| title removed during browse | playback entitlement re-check rejects start |
| user changes region | rights evaluated at playback start |
| account expires mid-session | session expires or renewal fails based on policy |
| device lacks 4K support | manifest selector returns lower compatible variant |
| DRM license service slow | timeout and retry; fail playback safely if unavailable |
| progress update arrives old | ignore if older than stored update unless seek semantics say otherwise |
| CDN edge misses hot release | origin shield and pre-positioning |
| recommendation down | fallback home rows |
| telemetry stream lag | playback unaffected |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
netflix/
  domain/
    Account.java
    Profile.java
    Title.java
    AvailabilityRule.java
    PlaybackSession.java
    ViewingProgress.java
  service/
    CatalogService.java
    RightsService.java
    PlaybackService.java
    ManifestService.java
    ProgressService.java
  port/
    TitleRepository.java
    SessionStore.java
    LicenseProvider.java
    ProgressRepository.java
  adapter/
    InMemoryTitleRepository.java
    InMemorySessionStore.java
  app/
    NetflixDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Dict, List


@dataclass
class Profile:
    profile_id: str
    country: str
    max_maturity: int
    active_subscription: bool


@dataclass
class Title:
    title_id: str
    name: str
    maturity: int
    countries: set[str]
    manifest_by_codec: Dict[str, str]


@dataclass
class Device:
    device_id: str
    codecs: List[str]
    max_resolution: str


@dataclass
class PlaybackSession:
    session_id: str
    profile_id: str
    title_id: str
    manifest_url: str
    expires_at: datetime


class InMemoryNetflix:
    def __init__(self) -> None:
        self.titles: Dict[str, Title] = {}
        self.sessions: Dict[str, PlaybackSession] = {}
        self.progress: Dict[tuple[str, str], int] = {}

    def add_title(self, title: Title) -> None:
        self.titles[title.title_id] = title

    def start_playback(self, profile: Profile, title_id: str, device: Device) -> PlaybackSession:
        if not profile.active_subscription:
            raise ValueError("inactive subscription")
        title = self.titles[title_id]
        if profile.country not in title.countries:
            raise ValueError("title unavailable in region")
        if title.maturity > profile.max_maturity:
            raise ValueError("blocked by maturity profile")

        manifest = None
        for codec in device.codecs:
            if codec in title.manifest_by_codec:
                manifest = title.manifest_by_codec[codec]
                break
        if manifest is None:
            raise ValueError("no compatible manifest")

        session_id = f"pbs-{len(self.sessions) + 1}"
        session = PlaybackSession(
            session_id=session_id,
            profile_id=profile.profile_id,
            title_id=title_id,
            manifest_url=f"https://cdn.example/{manifest}?sig=short-lived",
            expires_at=datetime.now(timezone.utc) + timedelta(minutes=5),
        )
        self.sessions[session_id] = session
        return session

    def update_progress(self, profile_id: str, title_id: str, position_ms: int) -> None:
        key = (profile_id, title_id)
        self.progress[key] = max(self.progress.get(key, 0), position_ms)


app = InMemoryNetflix()
app.add_title(Title("t1", "Architecture Movie", 13, {"US", "IN"}, {"h264": "t1/hls.m3u8", "hevc": "t1/dash.mpd"}))
profile = Profile("p1", "US", max_maturity=16, active_subscription=True)
device = Device("tv1", ["hevc", "h264"], "4k")
session = app.start_playback(profile, "t1", device)
app.update_progress("p1", "t1", 120_000)
print(session.manifest_url)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[titleId -> Title]` | title lookup |
| `dict[profileId -> Profile]` | profile settings |
| `dict[playbackSessionId -> PlaybackSession]` | short-lived sessions |
| `dict[(profileId,titleId) -> position]` | progress store |
| `dict[country -> set[titleId]]` | availability filtering |

## 4.4 Concurrency

High-signal concurrency issues:

- Progress updates from multiple devices.
- Duplicate start playback requests.
- Rights changes racing with cached catalog rows.
- License requests retried by player.

Handling strategy:

- Progress uses monotonic max with timestamp/session rules.
- Playback sessions are idempotent only if client supplies request ID.
- Playback re-checks rights even if browse cache is stale.
- License issuance validates session and can dedup by challenge/request ID.

## 4.5 Testing Thinking

Unit tests:

- Inactive subscriptions cannot start playback.
- Region-restricted titles are blocked.
- Maturity profile is enforced.
- Manifest selection respects device codecs.
- Progress does not move backward on retry.

Load tests:

- New-release playback start surge.
- DRM license QPS spike.
- CDN cache miss storm.
- Progress write burst during prime time.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| New release surge | major show drops | playback start and CDN edge pressure |
| Prime-time regional peak | evening viewing | regional capacity exhaustion |
| CDN cache miss storm | bad cache key/purge | origin overload |
| DRM provider degradation | license latency spike | playback start failures |
| Recommendation outage | home page dependency failure | browse latency and empty rows |

## 5.2 Immediate Spike Response

1. Protect playback start and segment delivery.
2. Route users to healthy CDN edges and origin shields.
3. Pre-position high-demand assets before known releases.
4. Use cached/precomputed manifests for common device classes.
5. Degrade home personalization to cached popular rows.
6. Batch progress and telemetry writes.
7. Apply request shaping for abusive or retry-loop clients.

## 5.3 Degradation Policy

Protect in this order:

1. Existing playback segment delivery.
2. New playback start authorization and manifest.
3. DRM license availability.
4. Basic catalog/title details.
5. Continue-watching updates.
6. Recommendations and experiments.
7. Fine-grained telemetry.

Not allowed:

- Bypass entitlement or maturity restrictions.
- Serve unavailable regional content.
- Let CDN miss storm collapse origins.
- Corrupt progress state with backward updates.

## 5.4 Spike Interview Answer

> During spikes I protect playback first: CDN health, manifest generation, entitlement, and DRM. Known release spikes are handled by pre-positioning assets, warming caches, and precomputing manifests. Recommendations, progress precision, and telemetry can degrade, but rights enforcement and playback security cannot.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global routing
  -> regional API/playback services
  -> nearby CDN edge for segments
  -> regional origin shield
  -> durable origin object storage
  -> replicated catalog/progress/event systems
```

## 6.2 Multi-Region Strategy

- Serve segments from the closest healthy edge.
- Replicate immutable assets broadly based on predicted demand.
- Keep catalog metadata region-aware.
- Evaluate rights using viewer context at playback start.
- Store progress close to profile region, replicate for roaming.
- Keep telemetry regional first, aggregate globally later.

## 6.3 Storage And Retention

- Source mezzanine files in durable object storage.
- Encoded renditions in hot/warm object storage and edge caches.
- Subtitles/artwork cached aggressively.
- Playback sessions expire quickly.
- Progress retained as product requires.
- Telemetry retained according to analytics and privacy policy.

## 6.4 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Catalog | replicated read models, rights-aware filters |
| Playback | stateless regional services, bounded dependency calls |
| Manifest | precomputed common manifests, cache by device class |
| DRM | regional redundancy, short-lived licenses |
| CDN | pre-positioning, origin shield, edge health routing |
| Progress | profile-partitioned KV/wide-column store |
| Telemetry | partitioned streams and batch consumers |
| Recommendations | cached rows and fallback strategies |

## 6.5 Global Interview Answer

> I would scale Netflix around a strict but fast playback control plane and an edge-heavy delivery plane. Catalog and recommendations help users discover titles, but playback starts only after entitlement, device-compatible manifest selection, and DRM setup. Segments are immutable and CDN-cached, progress is monotonic and eventually consistent, and telemetry feeds operations and recommendations asynchronously.

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

Use this answer flow:

```text
I will clarify VOD vs live, DRM, regional rights, profiles, device support, and recommendation scope.
I will estimate concurrent streams, segment requests/sec, CDN bandwidth, playback start QPS, and progress writes.
HLD includes catalog/rights, playback service, manifest service, DRM, CDN edge, progress, telemetry, recommendations, and encoding pipeline.
For streaming, I use adaptive bitrate manifests with immutable chunks served from CDN.
For correctness, entitlement and DRM fail closed; catalog/recommendations can be cached or stale.
For spikes, I pre-position assets, warm manifests, protect CDN/origin, and degrade personalization first.
For global scale, I use regional playback services, edge delivery, region-aware catalog rights, and async telemetry.
```

---

# 8. Fast Recall Rules

- Netflix-style systems are playback reliability and rights-enforcement heavy.
- Segment bytes come from CDN, not application servers.
- Playback start requires subscription, region, maturity, device, manifest, and DRM checks.
- Manifests are device-aware and policy-aware.
- Segments are immutable and aggressively cached.
- Rights and DRM fail closed.
- Progress is profile/title state and should be monotonic.
- Recommendations can degrade without breaking playback.
- Prime-time and new-release spikes should be planned and pre-warmed.
- CDN hit ratio and startup failure rate are first-class metrics.
