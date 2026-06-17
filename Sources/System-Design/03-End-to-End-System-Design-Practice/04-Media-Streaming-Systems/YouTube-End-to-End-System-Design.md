# YouTube - End-to-End System Design

> Goal: practice one complete E2E media platform problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for video-heavy E2E problems.
- Start broad with requirements and scale, then zoom into upload, encoding, storage, CDN delivery, recommendations, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For YouTube-style systems, optimize upload durability, asynchronous video processing, low-latency playback start, CDN efficiency, and personalized discovery.

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

| Layer | Interview signal | YouTube system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | upload, watch, search/discovery, recommendations, comments/likes out of scope unless asked |
| HLD | Can design scalable media systems | upload service, processing pipeline, object storage, metadata, CDN, playback service, recommender |
| LLD | Can model maintainable components | `Video`, `UploadSession`, `TranscodeJob`, `Rendition`, `PlaybackManifest`, `WatchEvent` |
| Machine coding | Can implement critical path | multipart upload, job state machine, manifest selection, cache-aware playback URL |
| Traffic spikes | Can protect production | viral videos, creator upload bursts, breaking news, CDN hot keys, recommendation fanout |
| Global scale | Can reason across regions | geo-routing, CDN hierarchy, regional rights/policy, async processing, multi-region metadata |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Creators can upload large videos reliably.
- System stores original videos and generates multiple playback renditions.
- Viewers can play videos with adaptive bitrate streaming.
- Video metadata includes title, description, tags, duration, thumbnails, visibility, and owner.
- Users can browse home feed, search, and recommended videos.
- Track watch events for analytics, recommendations, and creator dashboards.
- Support CDN-backed global video delivery.

Optional requirements to clarify:

- Are likes, comments, subscriptions, and notifications in scope?
- Do we need live streaming or only video-on-demand?
- Are copyright checks, moderation, and safety review in scope?
- Are private/unlisted videos required?
- Should offline download be supported?
- Do we need monetization/ad insertion?

Out of scope unless interviewer asks:

- Full search ranking internals.
- Full ML model training platform.
- Full copyright matching implementation.
- Full ad auction and billing system.

## 1.2 Non-Functional Requirements

Playback:

- Low startup latency, usually under a few seconds.
- Smooth playback with minimal buffering.
- Adaptive quality based on bandwidth and device.
- Very high availability for watch path.

Upload and processing:

- Resumable uploads for large files and unstable networks.
- Durable original storage.
- Asynchronous processing because encoding is CPU-heavy.
- Progress and failure visibility for creators.

Discovery:

- Personalized recommendations with freshness.
- Search and feed reads should be low latency.
- Watch events should be captured at very high write volume.

## 1.3 Constraints

- Raw videos are huge and cannot be served directly from application servers.
- Encoding is expensive and should be decoupled from upload.
- Popular videos create extreme read skew.
- Video quality ladder depends on device, codec, resolution, and network.
- Recommendation data is eventually consistent.
- Metadata correctness matters more than immediate analytics freshness.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 2 billion |
| DAU | 500 million |
| Video uploads/day | 5 million |
| Watch sessions/day | 5 billion |
| Average uploaded video size | 500 MB |
| Average playback bitrate | 2-5 Mbps |
| Peak watch QPS | millions of segment requests/sec |
| Playback availability target | 99.99%+ |
| Metadata API target | p95 under 100 ms |

## 1.5 Capacity Math

Back-of-the-envelope:

- Upload ingest: `5M uploads/day * 500 MB` is about `2.5 PB/day` of originals before replication and renditions.
- If each video creates 5-8 encoded renditions, derived storage can exceed original storage.
- Segment request volume is much higher than watch-start QPS because each player fetches many small chunks.
- CDN offload must be very high; even a small origin miss rate can create huge origin load.
- Watch events are append-heavy and should go to a stream, not directly to transactional databases.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Video segment duration | 2-6 seconds |
| Manifest size | KB-scale |
| Thumbnail size | tens to hundreds of KB |
| Encoding job time | seconds to hours depending length/resolution |
| CDN cache TTL | minutes to days depending asset type |
| Watch event lag tolerance | seconds to minutes |

## 1.6 Clarifying Questions To Ask

- Is this mostly video-on-demand, live streaming, or both?
- What upload size and duration limits should we assume?
- What playback devices are supported?
- What consistency is required after upload publish?
- Are recommendations in scope as online serving only, or also offline training?
- Do videos have regional visibility or policy restrictions?

Strong interview framing:

> I will design YouTube as two major paths: an asynchronous upload-processing path that creates streamable renditions, and a latency-sensitive playback path that serves manifests and chunks through CDN. Recommendations and analytics consume events asynchronously and should not block playback.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Upload flow:
Creator -> API Gateway -> Upload Service -> Object Store originals
        -> Upload finalized event -> Processing Queue
        -> Transcode Workers -> Rendition Store + Thumbnail Store
        -> Metadata publish -> Search/Recommendation indexes

Playback flow:
Viewer -> API Gateway -> Playback Service
       -> entitlement/policy check -> Manifest Service
       -> CDN signed URLs -> Player fetches video chunks from CDN
       -> Watch events -> Analytics/Recommender streams
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
            +-------------------------------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Upload Service        |        | Playback Service     |
| resumable multipart   |        | manifest + policy    |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Object Storage        |        | CDN / Edge Cache     |
| originals/renditions  |        | chunks/thumbnails    |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Processing Stream     |        | Watch Event Stream   |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Transcode Workers     |        | Analytics + Recs     |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+
| Metadata DB + Indexes |
+-----------------------+
```

Request flow for upload:

1. Creator initiates upload and receives an `uploadSessionId`.
2. Client uploads chunks directly to object storage using signed part URLs.
3. Upload Service finalizes the upload after verifying parts and checksum.
4. A `video.upload.finalized` event is published.
5. Processing workers validate, transcode, generate thumbnails, and create manifests.
6. Metadata changes from `PROCESSING` to `READY`.
7. Search and recommendation indexes receive update events.

Request flow for playback:

1. Viewer opens watch page for `videoId`.
2. Playback Service checks visibility, policy, device, and region.
3. Manifest Service returns an HLS/DASH manifest with signed CDN chunk URLs.
4. Player starts with a safe bitrate and adapts based on measured throughput.
5. CDN serves chunks from edge cache; misses go to regional/origin storage.
6. Player emits watch events for progress, buffering, quality switches, and completion.

## 2.2 APIs

### Start Upload

```http
POST /v1/videos/uploads
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "fileName": "system-design.mp4",
  "fileSizeBytes": 734003200,
  "contentType": "video/mp4",
  "checksum": "sha256:..."
}
```

Response:

```json
{
  "uploadSessionId": "upl-123",
  "videoId": "vid-987",
  "partSizeBytes": 8388608,
  "expiresAt": "2026-06-17T12:30:00Z"
}
```

### Get Part URL

```http
POST /v1/videos/uploads/upl-123/parts/17/url
```

```json
{
  "uploadUrl": "https://object-store.example/upload/...",
  "partNumber": 17,
  "expiresInSeconds": 900
}
```

### Finalize Upload

```http
POST /v1/videos/uploads/upl-123/finalize
```

```json
{
  "parts": [
    {"partNumber": 1, "etag": "etag-1"},
    {"partNumber": 2, "etag": "etag-2"}
  ]
}
```

### Get Playback Manifest

```http
GET /v1/videos/{videoId}/playback?device=ios&network=wifi
Authorization: Bearer <token>
```

Response:

```json
{
  "videoId": "vid-987",
  "manifestUrl": "https://cdn.example/vid-987/master.m3u8?sig=...",
  "expiresInSeconds": 300,
  "availableQualities": ["360p", "720p", "1080p"]
}
```

### Record Watch Event

```http
POST /v1/watch-events
```

```json
{
  "videoId": "vid-987",
  "userId": "u-123",
  "sessionId": "sess-555",
  "positionMs": 120000,
  "eventType": "PROGRESS",
  "quality": "720p",
  "bufferMs": 80
}
```

Important API points:

- Upload APIs are control-plane APIs; bytes should flow directly to object storage.
- Playback URLs should be signed and short-lived.
- Watch events are high-volume and should be batched by clients.
- Manifest and chunk APIs should be cache-friendly.

## 2.3 Core Components

Think of YouTube as four connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Upload plane | sessions, chunks, finalize, validation | durable ingest without app-server byte bottleneck |
| Processing plane | transcode, thumbnail, manifest, moderation hooks | convert originals into streamable assets |
| Playback plane | policy, manifest, CDN URLs, adaptive streaming | low startup time and low buffering |
| Discovery plane | watch events, search index, recommendations | help users find relevant videos |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| API Gateway | auth, rate limits, routing | video bytes | request QPS |
| Upload Service | upload session state, signed part URLs | transcoding | upload sessions |
| Object Store | originals, chunks, thumbnails | metadata truth | bytes and requests |
| Processing Queue | job decoupling and retry | video business rules | backlog and partition count |
| Transcode Workers | renditions, thumbnails, manifests | viewer authz | CPU/GPU encoding capacity |
| Metadata Service | video state and metadata | serving video chunks | metadata QPS |
| Playback Service | policy checks, manifest selection | raw chunk serving | watch starts |
| CDN | cached video segments and thumbnails | canonical metadata | edge traffic |
| Watch Event Pipeline | event ingest and stream processing | synchronous playback | event volume |
| Recommendation Service | candidate/ranking serving | video storage | feed QPS |

### Upload Service

Why it exists:

- Large video uploads need resumability and retry safety.
- Application servers should not proxy multi-GB payloads.
- Uploads need state: initiated, parts uploaded, finalized, processing, ready, failed.

Core responsibilities:

- Create upload sessions and video IDs.
- Issue signed URLs for chunk uploads.
- Track part numbers, etags, sizes, and checksums.
- Finalize multipart object in storage.
- Publish processing jobs.
- Enforce upload quota and file validation.

Failure behavior:

- Client retry uses same upload session and part number.
- Expired sessions are cleaned by background jobs.
- Finalize is idempotent: duplicate finalize returns the same video state.

Interview signal:

> Upload Service manages the control plane for resumable uploads; object storage handles the data plane for bytes.

### Video Processing Pipeline

Why it exists:

- Raw uploads are not optimal for streaming.
- Different devices and networks need different codecs, bitrates, and resolutions.
- CPU-heavy encoding must be asynchronous.

Core responsibilities:

- Validate container and codec.
- Extract metadata like duration, resolution, audio tracks.
- Generate thumbnail candidates.
- Transcode into an adaptive bitrate ladder.
- Segment renditions into small chunks.
- Generate HLS/DASH manifests.
- Publish `video.ready` or `video.failed` events.

State machine:

```text
CREATED -> UPLOADING -> UPLOADED -> PROCESSING -> READY
                                  -> FAILED_RETRYABLE
                                  -> FAILED_PERMANENT
```

Failure behavior:

- Retry transient worker failures with exponential backoff.
- Mark permanent failures when file is corrupt or unsupported.
- Keep original upload for retry/debug until retention policy removes it.

### Encoding, Chunking, And Streaming Protocols

Core idea:

- Encode one original into multiple renditions.
- Split each rendition into short segments.
- Serve a manifest that tells the player which segments exist.
- Player switches quality based on bandwidth and buffer health.

Typical adaptive bitrate ladder:

| Quality | Resolution | Approx bitrate |
|---|---:|---:|
| 144p | 256x144 | 100-300 Kbps |
| 360p | 640x360 | 500-1000 Kbps |
| 720p | 1280x720 | 2-4 Mbps |
| 1080p | 1920x1080 | 4-8 Mbps |
| 4K | 3840x2160 | 15-25+ Mbps |

Protocols:

| Protocol | Use | Notes |
|---|---|---|
| HLS | broad device support | manifest + segmented media |
| MPEG-DASH | adaptive streaming standard | common for web/Android-style players |
| CMAF | chunk format convergence | helps reuse chunks across HLS/DASH |

Interview signal:

> We do not stream one giant MP4 from origin. We serve manifests and small cached chunks through CDN so the player can adapt quality and recover from network changes.

### Playback Service

Why it exists:

- Playback needs policy, visibility, region, device, and freshness checks before returning a manifest.
- Chunk serving should stay at CDN; app services should not serve bytes.

Core responsibilities:

- Check video state is `READY`.
- Enforce visibility: public, unlisted, private.
- Enforce region/policy/age restrictions if needed.
- Choose manifest variant based on device and codecs.
- Issue signed CDN URLs or signed manifest URLs.
- Emit watch-start event.

Failure behavior:

- If metadata cache misses, read from metadata DB.
- If one CDN provider/region degrades, route to another edge/origin path.
- If recommendation service is down, playback still works.

### CDN And Caching

Why it exists:

- Video delivery is bandwidth-heavy and read-skewed.
- Popular chunks should be served close to viewers.
- Origin cannot handle all video segment requests directly.

Cache layers:

| Layer | Stores | TTL |
|---|---|---:|
| Edge CDN | hot video chunks, thumbnails, manifests | minutes to days |
| Regional cache | warm chunks and origin shield | hours to days |
| Origin/object store | canonical renditions and originals | durable retention |
| Service cache | video metadata, visibility, manifest metadata | seconds to minutes |

Hot-video strategy:

- Pre-warm CDN for trending or newly published high-subscriber videos.
- Use origin shield to collapse many edge misses into fewer origin reads.
- Cache immutable chunks aggressively.
- Version manifests and chunks by content hash or rendition version.

### Recommendation System

Why it exists:

- A video platform has too much content for manual discovery.
- Personalized ranking drives watch time and user satisfaction.

Serving architecture:

```text
User opens home
  -> Recommendation API
  -> Candidate generation (subscriptions, similar videos, trending, search history)
  -> Feature fetch (user/video/context features)
  -> Ranking model
  -> Filtering policy and diversity
  -> Response cache for short TTL
```

Common stages:

| Stage | Purpose | Latency sensitivity |
|---|---|---|
| Candidate generation | find hundreds/thousands of possible videos | medium |
| Ranking | order candidates by predicted engagement/satisfaction | high |
| Filtering | remove watched, blocked, unsafe, unavailable videos | high |
| Exploration | include fresh/long-tail content | medium |

Failure behavior:

- If ranking model fails, fall back to subscriptions/trending.
- If feature store is stale, use cached/default features.
- Recommendation failures should not affect direct video playback.

## 2.4 Data Layer

### Core Data Models

Video metadata:

```json
{
  "videoId": "vid-987",
  "ownerId": "u-123",
  "title": "System Design Basics",
  "description": "Intro to scalable systems",
  "visibility": "PUBLIC",
  "state": "READY",
  "durationMs": 720000,
  "createdAt": "2026-06-17T12:00:00Z",
  "publishedAt": "2026-06-17T12:20:00Z"
}
```

Rendition metadata:

```json
{
  "videoId": "vid-987",
  "renditionId": "rend-720p-h264",
  "codec": "h264",
  "resolution": "1280x720",
  "bitrateKbps": 3000,
  "segmentDurationSec": 4,
  "manifestPath": "videos/vid-987/hls/master.m3u8"
}
```

Watch event:

```json
{
  "eventId": "evt-1",
  "videoId": "vid-987",
  "userId": "u-123",
  "sessionId": "sess-555",
  "eventType": "PROGRESS",
  "positionMs": 120000,
  "createdAt": "2026-06-17T12:25:00Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Video originals | object storage | large immutable blobs |
| Encoded chunks | object storage + CDN | immutable, cacheable, high bandwidth |
| Video metadata | relational or document DB | query by ID/owner/state with correctness |
| Processing jobs | queue/stream + job DB | retries, state, worker coordination |
| Watch events | event stream + data lake | append-heavy analytics |
| Search index | inverted index/search engine | text and metadata search |
| Recommendation features | feature store/KV | low-latency ranking features |

Relational-style tables:

```sql
videos(video_id PK, owner_id, title, visibility, state, duration_ms, created_at, published_at)
video_renditions(video_id, rendition_id, codec, resolution, bitrate_kbps, manifest_path, state)
upload_sessions(upload_session_id PK, video_id, owner_id, state, expires_at, checksum)
upload_parts(upload_session_id, part_number, etag, size_bytes)
```

Important indexes:

- `videos(owner_id, created_at DESC)` for creator studio.
- `videos(state, published_at DESC)` for recent public content.
- `video_renditions(video_id, codec, resolution)` for manifest generation.
- `upload_parts(upload_session_id, part_number)` for finalize validation.

### Partitioning

- Partition object storage by hashed object key, not by raw video ID prefix.
- Partition metadata by `videoId`; add secondary indexes for owner queries.
- Partition watch events by `videoId` or `userId` depending pipeline.
- Partition processing jobs by `videoId` to keep job steps ordered per video.

### Replication And Consistency

- Original and rendition objects need high durability across zones.
- Metadata publish should be strongly consistent for video state transitions.
- Search and recommendation indexes can lag behind publish.
- Watch analytics and creator dashboards can be eventually consistent.

## 2.5 Scalability

### Horizontal Scaling

- Upload Service scales by upload session QPS.
- Transcode workers scale by queue depth and compute utilization.
- Playback Service scales by watch-start QPS.
- CDN scales video segment delivery by geography and popularity.
- Recommendation API scales by feed request QPS and model cost.

### Hot Content Strategy

- CDN pre-warming for predicted viral videos.
- Origin shield to avoid cache stampede.
- Immutable chunk URLs so cache invalidation is rare.
- Short TTL for manifests if availability/policy can change.
- Separate hot metadata cache from cold metadata reads.

### Large Creator Upload Bursts

- Quotas and per-creator rate limits.
- Upload admission control during processing backlog.
- Priority queues for important content classes if product requires it.
- Progress UI reads job state from processing metadata.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Watch page metadata fetch | 20-80 ms |
| Playback manifest generation | 20-100 ms |
| CDN first segment fetch | 50-500 ms depending network |
| Player startup target | 1-3 seconds |
| Recommendation API | 100-300 ms |

### Optimization Rules

- Keep playback path small: authz/policy, manifest, CDN URL.
- Serve chunks and thumbnails from CDN, not app servers.
- Use adaptive bitrate to trade quality for smoothness.
- Batch watch events on client.
- Cache immutable assets long; cache policy-sensitive data short.

## 2.7 Async Systems

Use streams for:

- Upload finalized events.
- Transcode job events.
- Thumbnail generation events.
- Moderation/copyright scan events.
- Search indexing events.
- Watch events.
- Recommendation feature updates.

Queue notes:

- Processing workers need idempotent job steps.
- Poison videos go to DLQ with reason.
- Job state must survive worker crashes.
- Backpressure should delay processing, not corrupt metadata.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated upload and metadata mutation APIs.
- Signed upload URLs with expiration and scoped object paths.
- Signed playback URLs for private/unlisted/restricted videos.
- Encryption in transit and at rest.
- Malware/content validation hooks after upload.

Privacy and policy:

- Visibility checks before manifest issuance.
- Region and age restrictions at playback.
- Private videos should not leak through CDN cache keys.
- Watch history is sensitive and should have retention/deletion controls.

Abuse controls:

- Upload quotas and rate limits.
- Spam and bot detection on watch events.
- Content safety/moderation pipeline.
- Copyright matching pipeline, if in scope.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Upload | upload success rate, finalize latency, abandoned sessions |
| Processing | queue lag, transcode duration, failed jobs, retry count |
| Playback | startup latency, rebuffer ratio, CDN hit rate, manifest error rate |
| CDN | edge hit ratio, origin egress, hot object misses, regional errors |
| Recommendations | API latency, fallback rate, click/watch quality metrics |
| Events | watch event ingest lag, dropped events, duplicate rate |

Alerts:

- CDN hit ratio drops sharply.
- Origin egress spikes unexpectedly.
- Transcode backlog exceeds SLO.
- Playback manifest 5xx rises.
- Watch event stream lag grows.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Upload path | app server proxy | direct signed object upload | simpler auth vs massive server bandwidth cost |
| Encoding | precompute all renditions | encode on demand | low playback latency vs storage/compute cost |
| Delivery | CDN chunks | origin streaming | global scale vs cache complexity |
| Quality | adaptive bitrate | fixed quality | smooth playback vs encoding ladder complexity |
| Recommendations | real-time ranking | cached feed | freshness/personalization vs latency/cost |
| Metadata consistency | strong for publish state | eventual everywhere | correctness vs simpler scaling |

Interview framing:

> I would keep upload and processing asynchronous, serve immutable chunks through CDN, keep playback control-plane APIs fast, and treat analytics/recommendations as eventually consistent side pipelines.

---

# 3. Low-Level Design

LLD goal:

> Model YouTube around durable video metadata, resumable upload sessions, asynchronous processing jobs, immutable renditions, playback manifests, and append-only watch events.

Simple rules:

- Do not put video bytes inside metadata objects.
- Do not let playback depend on recommendation availability.
- Make processing jobs idempotent because workers retry.
- Version renditions and manifests so cached chunks remain immutable.

Starter map:

| LLD question | YouTube answer |
|---|---|
| What is durable? | original video, renditions, metadata, upload sessions, processing state |
| What is ephemeral? | signed URLs, playback sessions, recommendation caches |
| What is expensive? | encoding, CDN misses, high-cardinality watch events |
| What is eventually consistent? | analytics, recommendations, search indexing |
| What must be correct? | ownership, visibility, video state, object paths |

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Video` | owner, title, visibility, state | only `READY` public videos are playable |
| `UploadSession` | part state, checksum, expiration | finalize is idempotent |
| `UploadPart` | part number, etag, size | parts belong to one session |
| `TranscodeJob` | job state, attempts, target profile | job step can retry safely |
| `Rendition` | codec, resolution, bitrate, path | chunks are immutable for a version |
| `PlaybackManifest` | rendition list and segment paths | references ready renditions only |
| `WatchEvent` | playback telemetry | append-only and deduplicated by event ID if needed |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `UploadService` | session lifecycle and finalize | transcode bytes inline |
| `ProcessingService` | job orchestration | serve viewer playback |
| `MetadataService` | video state and ownership | store large blobs |
| `PlaybackService` | policy check and manifest selection | deliver segments directly |
| `RecommendationService` | candidates and ranking | block direct playback |
| `EventIngestService` | watch telemetry ingestion | mutate video truth synchronously |

## 3.2 OOP Fundamentals

Encapsulation:

- `UploadSession` owns valid state transitions.
- `Video` owns publishability checks.
- `TranscodeJob` owns retry and terminal-state rules.

Abstraction:

- `ObjectStorageClient` hides cloud/object-store details.
- `ManifestGenerator` hides HLS/DASH differences.
- `RecommendationRanker` hides model implementation.

Polymorphism:

- Different `EncodingProfile` implementations for mobile, HD, and 4K.
- Different `ManifestGenerator` implementations for HLS and DASH.
- Different `RecommendationStrategy` implementations for home, related, and shorts-style feeds.

Composition:

- `PlaybackService` composes metadata repository, policy checker, manifest repository, and URL signer.

## 3.3 SOLID Principles

| Principle | YouTube application |
|---|---|
| Single Responsibility | `UploadService` manages upload state only |
| Open/Closed | add AV1 profile without rewriting upload flow |
| Liskov Substitution | any storage adapter must preserve put/get/sign contract |
| Interface Segregation | separate upload, playback, metadata, event ingest ports |
| Dependency Inversion | services depend on `VideoRepository`, not a concrete DB driver |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| State | upload/video/job lifecycle | explicit valid transitions |
| Strategy | encoding ladders and recommendation strategies | swap behavior by device/product surface |
| Factory | manifest generator creation | create HLS/DASH generators cleanly |
| Observer/Event Publisher | upload finalized to processing | decouple slow jobs from request path |
| Decorator | metrics/retry/rate-limit wrappers | cross-cutting behavior |

## 3.5 UML / Diagrams

### Upload Sequence

```text
Client -> UploadService: startUpload(metadata)
UploadService -> VideoRepository: create video PROCESSING placeholder
UploadService -> ObjectStorage: create multipart upload
UploadService -> Client: uploadSessionId + part config
Client -> ObjectStorage: upload parts with signed URLs
Client -> UploadService: finalize(parts)
UploadService -> ObjectStorage: complete multipart upload
UploadService -> JobQueue: publish video.upload.finalized
Worker -> ProcessingService: transcode and create renditions
ProcessingService -> VideoRepository: mark READY
```

### Playback Sequence

```text
Client -> PlaybackService: getPlayback(videoId)
PlaybackService -> MetadataRepository: fetch video
PlaybackService -> PolicyService: check visibility/region/device
PlaybackService -> ManifestRepository: fetch ready manifest
PlaybackService -> UrlSigner: sign manifest/chunk URLs
PlaybackService -> Client: playback manifest URL
Client -> CDN: fetch manifest and chunks
Client -> EventIngest: batch watch events
```

## 3.6 Class Design

Interfaces:

```java
interface VideoRepository {
    Video get(String videoId);
    void save(Video video);
    void updateState(String videoId, VideoState state);
}

interface UploadSessionRepository {
    UploadSession get(String uploadSessionId);
    void save(UploadSession session);
}

interface ObjectStorageClient {
    String createSignedPartUrl(String objectKey, int partNumber);
    void completeMultipartUpload(String objectKey, List<UploadPart> parts);
}

interface ManifestGenerator {
    PlaybackManifest generate(String videoId, List<Rendition> renditions);
}

interface EventPublisher {
    void publish(String topic, Object event);
}
```

Design notes:

- `finalizeUpload()` must be idempotent.
- `markReady()` should happen only after required renditions and manifest exist.
- Playback should reject videos not in `READY` state.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| duplicate finalize request | return existing video processing state |
| part uploaded twice | latest valid etag for part wins or reject mismatch |
| corrupt video | mark failed permanent and show creator error |
| transcode worker crashes | retry from durable job state |
| manifest exists but one chunk missing | do not mark rendition ready until validation passes |
| private video CDN cache leak risk | signed URLs and cache key includes authorization-safe token/version |
| viral video after publish | pre-warm CDN and use origin shield |
| recommendation service down | fallback to trending/subscriptions |
| watch events delayed | playback unaffected; analytics catches up |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
youtube/
  domain/
    Video.java
    UploadSession.java
    TranscodeJob.java
    Rendition.java
  service/
    UploadService.java
    ProcessingService.java
    PlaybackService.java
  port/
    VideoRepository.java
    ObjectStorageClient.java
    JobQueue.java
    UrlSigner.java
  adapter/
    InMemoryVideoRepository.java
    InMemoryObjectStorage.java
  app/
    YouTubeDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List


class VideoState(str, Enum):
    CREATED = "CREATED"
    UPLOADING = "UPLOADING"
    PROCESSING = "PROCESSING"
    READY = "READY"
    FAILED = "FAILED"


@dataclass
class UploadPart:
    part_number: int
    etag: str
    size_bytes: int


@dataclass
class Video:
    video_id: str
    owner_id: str
    title: str
    state: VideoState = VideoState.CREATED
    renditions: List[str] = field(default_factory=list)


@dataclass
class UploadSession:
    session_id: str
    video_id: str
    owner_id: str
    object_key: str
    parts: Dict[int, UploadPart] = field(default_factory=dict)
    finalized: bool = False


class InMemoryYouTube:
    def __init__(self) -> None:
        self.videos: Dict[str, Video] = {}
        self.sessions: Dict[str, UploadSession] = {}
        self.jobs: List[str] = []

    def start_upload(self, owner_id: str, title: str) -> UploadSession:
        video_id = f"vid-{len(self.videos) + 1}"
        session_id = f"upl-{len(self.sessions) + 1}"
        self.videos[video_id] = Video(video_id=video_id, owner_id=owner_id, title=title, state=VideoState.UPLOADING)
        session = UploadSession(
            session_id=session_id,
            video_id=video_id,
            owner_id=owner_id,
            object_key=f"originals/{video_id}",
        )
        self.sessions[session_id] = session
        return session

    def record_part(self, session_id: str, part_number: int, etag: str, size_bytes: int) -> None:
        session = self.sessions[session_id]
        if session.finalized:
            raise ValueError("upload already finalized")
        session.parts[part_number] = UploadPart(part_number, etag, size_bytes)

    def finalize_upload(self, session_id: str) -> Video:
        session = self.sessions[session_id]
        video = self.videos[session.video_id]
        if session.finalized:
            return video
        if not session.parts:
            raise ValueError("cannot finalize upload with no parts")
        session.finalized = True
        video.state = VideoState.PROCESSING
        self.jobs.append(video.video_id)
        return video

    def complete_processing(self, video_id: str, renditions: List[str]) -> Video:
        video = self.videos[video_id]
        if video.state != VideoState.PROCESSING:
            raise ValueError("video is not processing")
        video.renditions = renditions
        video.state = VideoState.READY
        return video

    def get_playback_manifest(self, video_id: str) -> List[str]:
        video = self.videos[video_id]
        if video.state != VideoState.READY:
            raise ValueError("video is not ready")
        return video.renditions


yt = InMemoryYouTube()
session = yt.start_upload("u1", "System Design")
yt.record_part(session.session_id, 1, "etag-1", 8_000_000)
video = yt.finalize_upload(session.session_id)
yt.complete_processing(video.video_id, ["360p.m3u8", "720p.m3u8"])
print(yt.get_playback_manifest(video.video_id))
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[videoId -> Video]` | metadata lookup |
| `dict[uploadSessionId -> UploadSession]` | resumable upload state |
| `dict[videoId -> list[Rendition]]` | manifest generation |
| `queue[videoId]` | async processing jobs |
| `append-only list[WatchEvent]` | telemetry simulation |

## 4.4 Concurrency

High-signal concurrency issues:

- Duplicate finalize calls from client retries.
- Multiple workers picking the same transcode job.
- Metadata state racing with search index updates.
- Watch event duplicates from client retry.

Handling strategy:

- Idempotent finalize keyed by `uploadSessionId`.
- Job leasing with heartbeat and retry timeout.
- State machine with compare-and-set transitions.
- Event IDs or session-position dedup for watch telemetry.

## 4.5 Testing Thinking

Unit tests:

- Upload session creation.
- Part recording and overwrite behavior.
- Finalize is idempotent.
- Playback rejects non-ready video.
- Processing marks video ready only with renditions.

Load tests:

- Massive watch-start QPS.
- CDN cache miss storm.
- Transcode backlog under upload surge.
- Watch event stream ingestion.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Viral video | celebrity/news upload | CDN hot object and origin egress spike |
| Upload burst | many creators upload after event | processing queue backlog |
| Breaking news | regional traffic surge | edge saturation and hot metadata |
| Bot watch spam | fake views | event pipeline pollution |
| CDN outage | edge provider degradation | origin overload and playback errors |

## 5.2 Immediate Spike Response

1. Protect playback path first: manifests, chunks, CDN routing.
2. Use CDN origin shield and request coalescing for hot chunks.
3. Pre-warm or pin viral content at edge.
4. Apply upload admission control if processing backlog is dangerous.
5. Degrade recommendations to cached/trending fallback.
6. Batch or sample low-priority analytics events.
7. Rate-limit suspicious watch or upload patterns.

## 5.3 Degradation Policy

Protect in this order:

1. Existing video playback.
2. Upload finalization and durable originals.
3. Basic search/watch page metadata.
4. Recommendations.
5. Creator analytics freshness.
6. Non-critical experiments and enrichments.

Not allowed:

- Lose finalized uploaded originals.
- Serve private videos publicly.
- Mark videos ready before chunks/manifests are valid.
- Let CDN misses collapse object storage.

## 5.4 Spike Interview Answer

> During spikes I protect playback and durable upload state first. Viral content is handled with CDN pre-warming, origin shield, immutable chunks, and hot metadata caching. Upload processing can lag safely because it is asynchronous, while recommendations and analytics can degrade to cached or sampled modes.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global DNS/Anycast
  -> nearest API and playback region
  -> CDN edge for chunks/thumbnails
  -> regional origin shield
  -> object storage origins
  -> metadata and event pipelines replicated by region
```

## 6.2 Multi-Region Strategy

- Route playback to nearest healthy edge.
- Keep video chunks immutable and globally cacheable.
- Replicate metadata for low-latency reads.
- Keep ownership/publish state transitions strongly controlled.
- Process uploads in creator/home region, then replicate renditions.
- Use regional policy filters for availability and compliance.

## 6.3 Storage And Retention

- Originals in durable object storage, possibly colder after processing.
- Popular renditions in hot storage/CDN.
- Rarely watched videos can stay in cheaper storage with lazy cache fill.
- Watch events go to data lake with retention and privacy controls.
- Deleted/private videos require metadata change plus CDN invalidation/versioning.

## 6.4 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Upload | signed direct upload, partitioned sessions |
| Processing | queue-based workers, codec-specific pools |
| Metadata | sharded by video ID, cached by watch traffic |
| CDN | edge caching, origin shield, pre-warming |
| Playback | stateless services, regional routing |
| Events | partitioned streams, batch consumers |
| Recommendations | offline training + online ranking + fallbacks |

## 6.5 Global Interview Answer

> I would scale YouTube by separating the write-heavy upload pipeline from the read-heavy playback path. Uploads become durable originals and async transcode jobs; playback serves signed manifests and immutable chunks from CDN; recommendations and analytics consume watch events asynchronously. The critical global levers are CDN hit ratio, processing backlog, metadata correctness, and graceful degradation of discovery features.

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
I will clarify VOD vs live, upload size, playback devices, visibility, and recommendation scope.
I will estimate upload bytes/day, segment request rate, CDN hit ratio, and watch event volume.
HLD separates upload control plane, object storage, processing pipeline, metadata, playback service, CDN, and recommendation pipeline.
For chunking, I will use adaptive bitrate renditions with HLS/DASH manifests and small immutable segments.
For reliability, uploads are resumable, processing is retryable, playback is CDN-backed, and analytics is async.
For spikes, I protect playback, pre-warm hot content, use origin shield, and degrade recommendations/analytics first.
For global scale, I route viewers to edge, replicate metadata, and keep video chunks cacheable and versioned.
```

---

# 8. Fast Recall Rules

- Upload bytes should go directly to object storage using signed URLs.
- Processing is asynchronous because encoding is CPU-heavy.
- Store originals separately from encoded renditions.
- Playback returns manifests; CDN serves chunks.
- Use HLS/DASH with adaptive bitrate streaming.
- Chunks should be immutable and cacheable.
- CDN hit ratio is a first-class SLO.
- Watch events feed analytics and recommendations asynchronously.
- Recommendations should never block direct playback.
- Protect private video access with policy checks and signed URLs.
