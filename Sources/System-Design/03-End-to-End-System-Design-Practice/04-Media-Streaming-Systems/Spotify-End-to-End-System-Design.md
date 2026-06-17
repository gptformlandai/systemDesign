# Spotify - End-to-End System Design

> Goal: practice one complete E2E audio streaming problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for audio streaming and music discovery systems.
- Start broad with requirements and scale, then zoom into catalog, rights, audio encoding, chunk delivery, CDN caching, playback sessions, playlists, recommendations, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Spotify-style systems, optimize low startup latency, continuous playback, playlist/library correctness, rights filtering, CDN efficiency, offline support, and personalized discovery.

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

| Layer | Interview signal | Spotify system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | music playback, podcasts, playlists, search, library, recommendations, offline |
| HLD | Can design scalable streaming systems | catalog, rights, playback service, audio store, CDN, playlist service, recommender |
| LLD | Can model maintainable components | `Track`, `AudioAsset`, `Playlist`, `PlaybackSession`, `Queue`, `ListeningEvent` |
| Machine coding | Can implement critical path | check rights, select audio file, return stream chunks, update playlist/progress |
| Traffic spikes | Can protect production | album releases, viral tracks, playlist refreshes, cache misses, event floods |
| Global scale | Can reason across regions | regional rights, edge caching, offline downloads, high-volume telemetry |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can search and browse tracks, albums, artists, podcasts, and playlists.
- Users can play audio with low startup latency.
- System enforces subscription, region, and rights restrictions.
- Users can create, edit, and share playlists.
- Users can like/save tracks and albums to their library.
- System supports personalized recommendations and generated playlists.
- System records listening events for recommendations, royalty reporting, and analytics.
- Premium users may download tracks/playlists for offline playback if in scope.

Optional requirements to clarify:

- Are podcasts and audiobooks in scope, or music only?
- Do we support free/ad-supported users?
- Should playback support cross-device handoff?
- Is real-time collaborative playlist editing in scope?
- Do we need lyrics, credits, and social activity?
- Are royalty accounting and artist dashboards in scope?

Out of scope unless interviewer asks:

- Full ad serving and auction logic.
- Full payment/subscription system.
- Full music licensing contract management.
- Full ML training platform internals.
- Full social graph implementation.

## 1.2 Non-Functional Requirements

Playback:

- Very low audio startup latency.
- Smooth continuous playback with small buffers.
- High availability for play/pause/next and stream URL generation.
- Device-aware audio quality selection.

Catalog and rights:

- Correct regional availability and subscription gating.
- Fast search and browse reads.
- Catalog updates should propagate quickly but can be eventually indexed.

Playlist/library:

- Playlist edits should be durable and ordered.
- Library operations should be low latency.
- Collaborative edits require conflict handling if in scope.

Recommendations:

- Personalized home and generated playlists can be eventually consistent.
- Listening events must be captured at very high write volume.

## 1.3 Constraints

- Audio files are smaller than videos, but request volume and session duration are huge.
- Regional music rights change over time.
- Playlists can become very large and highly followed.
- Listening events are high-cardinality and append-heavy.
- Offline downloads require secure local entitlements and periodic renewal.
- Recommendations must balance personalization, freshness, diversity, and licensing.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 1 billion |
| DAU | 300 million |
| Peak concurrent listeners | 80 million |
| Tracks in catalog | 100 million+ |
| Listening sessions/day | billions |
| Average audio bitrate | 96-320 Kbps |
| Segment duration | 2-10 seconds |
| Playlist edits/day | hundreds of millions |
| Playback availability target | 99.99%+ |

## 1.5 Capacity Math

Back-of-the-envelope:

- `80M concurrent listeners * 160 Kbps` is about `12.8 Tbps` edge delivery at peak.
- With 5-second segments, `80M / 5 = 16M segment requests/sec` at peak.
- Audio bandwidth is lower than video, but concurrency and track-switch frequency are very high.
- Listening events can be multiple events per track: start, progress, skip, complete, like, add-to-playlist.
- Playlist reads are highly skewed: popular editorial/generated playlists can become hot objects.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Audio segment duration | 2-10 seconds |
| Track metadata size | KB-scale |
| Track audio file | few MB to tens of MB |
| Playlist page size | 50-100 tracks |
| Progress update interval | 10-30 seconds or track boundary |
| Recommendation freshness | minutes to daily depending surface |

## 1.6 Clarifying Questions To Ask

- Music only, or podcasts/audiobooks too?
- Free tier with ads, premium tier, or both?
- Is offline download required?
- Are collaborative playlists required?
- What consistency is expected after playlist edits?
- Are regional licensing and explicit-content filters in scope?

Strong interview framing:

> I will design Spotify as a low-latency audio playback system with a catalog/rights control plane, CDN-backed chunk delivery, durable playlist and library services, and an event-driven recommendation pipeline. Playback and rights correctness are critical; recommendations and analytics can be eventually consistent.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Playback flow:
Client -> API Gateway -> Playback Service
       -> account/profile/rights check
       -> Audio Manifest Service
       -> signed CDN URLs
       -> player fetches audio chunks
       -> listening events stream

Playlist flow:
Client -> Playlist Service
       -> validate ownership/collaboration
       -> append ordered playlist mutation
       -> update playlist read model/cache

Recommendation flow:
Listening events + library + playlists
       -> stream/batch feature pipelines
       -> candidate generation/ranking
       -> Home/Radio/Discover APIs
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
            +------------------+-------------------+--------------------+
            |                  |                   |                    |
            v                  v                   v                    v
+----------------+   +----------------+   +----------------+   +----------------+
| Playback Svc   |   | Catalog/Rights |   | Playlist Svc   |   | Home/Recs Svc  |
+-------+--------+   +--------+-------+   +-------+--------+   +-------+--------+
        |                     |                   |                    |
        v                     v                   v                    v
+----------------+   +----------------+   +----------------+   +----------------+
| Manifest Svc   |   | Catalog DB     |   | Playlist Store |   | Feature Store  |
+-------+--------+   | Search Index   |   +----------------+   +----------------+
        |            +----------------+            |                    |
        v                                          v                    v
+----------------+                         +----------------+   +----------------+
| CDN / Edge     |                         | Event Stream   |-->| Analytics/Recs |
| audio chunks   |                         | mutations/listens |  pipelines     |
+-------+--------+                         +----------------+   +----------------+
        |
        v
+----------------+
| Audio Object   |
| Store          |
+----------------+
```

Request flow for playback:

1. Client requests playback for a track, episode, playlist, or queue item.
2. Playback Service validates account, subscription, explicit filter, and device.
3. Catalog/Rights Service checks regional availability and content state.
4. Manifest Service selects compatible audio codec/quality and returns chunk URLs.
5. Player fetches chunks from CDN and buffers upcoming segments.
6. Client emits listening events for start, progress, skip, complete, and quality.

Request flow for playlist edit:

1. Client sends playlist mutation with `clientMutationId`.
2. Playlist Service validates owner/collaborator permissions.
3. Service appends mutation to durable log or updates versioned playlist row.
4. Read model/cache is updated.
5. Playlist update event feeds recommendations and notifications.

## 2.2 APIs

### Search

```http
GET /v1/search?q=daft&type=track,artist,playlist&market=US
Authorization: Bearer <token>
```

### Start Playback

```http
POST /v1/playback-sessions
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "userId": "u-1",
  "deviceId": "dev-9",
  "context": {"type": "playlist", "id": "pl-123"},
  "trackId": "trk-456",
  "requestedQuality": "HIGH"
}
```

Response:

```json
{
  "playbackSessionId": "pbs-1",
  "trackId": "trk-456",
  "manifestUrl": "https://cdn.example/audio/trk-456/manifest.json?sig=...",
  "expiresInSeconds": 300,
  "nextTrackHints": ["trk-789", "trk-222"]
}
```

### Get Audio Manifest

```http
GET /v1/tracks/{trackId}/manifest?device=ios&quality=HIGH
```

Response:

```json
{
  "trackId": "trk-456",
  "codec": "ogg",
  "bitrateKbps": 320,
  "segments": [
    {"index": 0, "url": "https://cdn.example/audio/trk-456/0.seg?sig=...", "durationMs": 5000},
    {"index": 1, "url": "https://cdn.example/audio/trk-456/1.seg?sig=...", "durationMs": 5000}
  ]
}
```

### Playlist Mutation

```http
POST /v1/playlists/{playlistId}/mutations
```

```json
{
  "clientMutationId": "mut-abc",
  "operation": "ADD_TRACK",
  "trackId": "trk-456",
  "position": 12
}
```

### Listening Event

```http
POST /v1/listening-events
```

```json
{
  "eventId": "evt-1",
  "playbackSessionId": "pbs-1",
  "userId": "u-1",
  "trackId": "trk-456",
  "eventType": "SKIP",
  "positionMs": 42000,
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Important API points:

- Playback APIs are control plane; audio chunks come from CDN.
- Playlist mutations need idempotency through `clientMutationId`.
- Search and home results must be rights-filtered by market/user context.
- Listening events should be batched and append-only.

## 2.3 Core Components

Think of Spotify as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Catalog plane | tracks, albums, artists, podcasts, metadata | accurate searchable catalog |
| Playback plane | session, rights, quality, queue, manifest | low-latency continuous listening |
| Delivery plane | audio chunks, CDN, object storage | efficient global streaming |
| User collection plane | playlists, library, follows | durable user intent |
| Discovery plane | recommendations, radio, generated playlists | personalized listening |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| API Gateway | auth, routing, rate limits | audio bytes | request QPS |
| Catalog Service | track/album/artist metadata | playlist order | catalog reads |
| Rights Service | market/subscription/content restrictions | audio encoding | entitlement QPS |
| Playback Service | playback session and queue control | playlist mutation truth | playback starts/actions |
| Manifest Service | audio asset selection and signed URLs | rights rules | manifest QPS |
| CDN/Edge | audio chunk delivery | catalog truth | segment traffic |
| Playlist Service | playlist mutations and read models | audio chunks | playlist writes/reads |
| Library Service | saved tracks/albums/follows | recommendation ranking | library operations |
| Recommendation Service | home, radio, generated playlists | rights source of truth | home/feed QPS |
| Event Pipeline | listening and mutation events | synchronous playback | event volume |

### Catalog And Rights Service

Why it exists:

- The same track may be available in one country and unavailable in another.
- Search/home/playback must filter content by rights and explicit settings.

Core responsibilities:

- Store tracks, albums, artists, podcast episodes, artwork, duration, ISRC/IDs.
- Store audio asset metadata by codec/quality.
- Apply regional availability and subscription rules.
- Expose low-latency metadata to playback, search, and recommendations.

Failure behavior:

- Playback should fail closed if rights cannot be checked.
- Search/home can use cached rights-filtered results briefly.
- Catalog index lag is acceptable after new releases, but playback state must be correct.

Interview signal:

> Catalog says what the content is; Rights Service says whether this user can play it now.

### Playback Service

Why it exists:

- Playback is more than fetching a file: it includes queue context, rights, quality, device, and telemetry setup.

Core responsibilities:

- Start playback sessions.
- Validate account, subscription, explicit filter, and device.
- Resolve context: album, playlist, artist radio, podcast, queue.
- Check track rights.
- Request manifest for compatible audio quality.
- Return next-track hints for gapless or low-latency transition.
- Record playback start event.

Failure behavior:

- If a track becomes unavailable, skip to next playable track.
- If high-quality asset unavailable, fall back to lower quality.
- If recommendations fail for radio/autoplay, continue current queue where possible.

Interview signal:

> Playback Service coordinates the control plane; CDN serves the audio chunks.

### Audio Encoding, Chunking, And Streaming

Core idea:

- Store each track in multiple codecs/bitrates.
- Split audio into small chunks or support byte-range chunking.
- Player buffers current and upcoming chunks.
- Quality can be selected by subscription, device, and network.

Typical quality ladder:

| Quality | Approx bitrate | Use |
|---|---:|---|
| LOW | 24-64 Kbps | poor network/data saver |
| NORMAL | 96-160 Kbps | default mobile |
| HIGH | 256-320 Kbps | premium/high quality |
| LOSSLESS | higher | optional product tier if in scope |

Common formats:

| Format | Use |
|---|---|
| Ogg/Vorbis | efficient music streaming |
| AAC | broad device support |
| Opus | speech/podcast or modern clients |
| FLAC/lossless | high-fidelity tier if required |

Interview signal:

> Audio streaming has less bandwidth than video, but segment QPS and session continuity are massive. The system should prefetch next chunks/tracks and keep playback independent from discovery services.

### CDN And Caching

Why it exists:

- Popular tracks and playlists create heavy read skew.
- Edge delivery reduces startup latency and origin bandwidth.
- Audio chunks are immutable and highly cacheable.

Cache layers:

| Layer | Stores | TTL |
|---|---|---:|
| Edge CDN | hot audio chunks, artwork | hours to days |
| Regional cache | warm chunks and origin shield | hours to days |
| Origin object store | canonical audio assets | durable retention |
| Service cache | track metadata, rights hints, playlist pages | seconds to minutes |
| Client cache | recently played tracks, offline downloads | product-policy dependent |

Hot content strategy:

- Pre-warm new major releases and editorial playlists.
- Cache artwork and metadata separately from audio.
- Use immutable asset paths with versioning.
- Use origin shield to avoid cache miss storms.

### Playlist Service

Why it exists:

- Playlists are user-authored durable ordered collections.
- Edits can happen from multiple devices and collaborators.

Core responsibilities:

- Create, rename, delete, and share playlists.
- Add/remove/reorder tracks.
- Enforce owner/collaborator permissions.
- Maintain playlist version.
- Provide paginated playlist reads.
- Emit playlist mutation events.

Consistency choices:

- Single-user playlist edits can be strongly consistent.
- Collaborative playlist edits may use optimistic concurrency and version checks.
- Read caches should invalidate by playlist version.

Failure behavior:

- Duplicate mutation with same `clientMutationId` returns same result.
- Conflicting reorder can fail with version mismatch and ask client to refresh.

### Recommendations

Why it exists:

- Music discovery is central: home, radio, mixes, autoplay, similar artists, generated playlists.

Serving architecture:

```text
Home request
  -> profile/context fetch
  -> candidate generation from history, follows, playlists, trends
  -> feature fetch
  -> ranking
  -> rights/explicit filtering
  -> diversity and freshness
  -> response cache
```

Signals:

- Starts, skips, completes, repeats.
- Saves, likes, playlist adds.
- Search queries.
- Followed artists and genres.
- Time of day/device/context.
- Similar users and collaborative filtering.

Failure behavior:

- Fall back to cached generated playlists, followed artists, or editorial/trending.
- Recommendation failure should not interrupt active playback.

### Offline Downloads

Why it exists:

- Premium users may want playback without network.
- Offline files must still respect subscription and rights.

Core responsibilities:

- Authorize download by user/track/market.
- Store encrypted assets on device.
- Renew offline entitlement periodically.
- Remove or disable expired/unavailable content.

Failure behavior:

- If renewal fails after grace period, offline playback is disabled.
- Rights changes should apply on next entitlement refresh.

## 2.4 Data Layer

### Core Data Models

Track:

```json
{
  "trackId": "trk-456",
  "albumId": "alb-1",
  "artistIds": ["art-1"],
  "name": "Example Track",
  "durationMs": 210000,
  "explicit": false,
  "state": "AVAILABLE"
}
```

Audio asset:

```json
{
  "trackId": "trk-456",
  "assetId": "asset-ogg-320",
  "codec": "ogg",
  "bitrateKbps": 320,
  "segmentDurationMs": 5000,
  "objectPath": "audio/trk-456/ogg/320/"
}
```

Playlist:

```json
{
  "playlistId": "pl-123",
  "ownerId": "u-1",
  "name": "System Design Focus",
  "visibility": "PUBLIC",
  "version": 42,
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

Playlist item:

```json
{
  "playlistId": "pl-123",
  "position": 12,
  "trackId": "trk-456",
  "addedBy": "u-1",
  "addedAt": "2026-06-17T12:00:00Z"
}
```

Listening event:

```json
{
  "eventId": "evt-1",
  "userId": "u-1",
  "trackId": "trk-456",
  "eventType": "COMPLETE",
  "positionMs": 210000,
  "createdAt": "2026-06-17T12:04:00Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Track/album/artist metadata | document/relational DB | rich structured catalog |
| Search index | search engine | text search and autocomplete |
| Audio chunks | object storage + CDN | immutable high-read assets |
| Rights rules | relational/rule store | correctness and auditability |
| Playlists | relational/wide-column + mutation log | ordered durable collections |
| Library saves/follows | KV/wide-column | user-centric reads |
| Playback sessions | Redis/KV TTL store | short-lived state |
| Listening events | event stream + data lake | append-heavy analytics/recs |
| Recommendation features | feature store | low-latency ranking |

Relational-style tables:

```sql
tracks(track_id PK, album_id, name, duration_ms, explicit, state)
track_artists(track_id, artist_id)
audio_assets(track_id, asset_id, codec, bitrate_kbps, object_path)
track_availability(track_id, country, starts_at, ends_at, subscription_tier)
playlists(playlist_id PK, owner_id, name, visibility, version, updated_at)
playlist_items(playlist_id, position, track_id, added_by, added_at)
user_library(user_id, item_type, item_id, saved_at)
```

Important indexes:

- `tracks(name)` through search index for search/autocomplete.
- `audio_assets(track_id, codec, bitrate_kbps)` for manifest selection.
- `track_availability(track_id, country)` for rights checks.
- `playlist_items(playlist_id, position)` for paginated playlist reads.
- `user_library(user_id, saved_at DESC)` for library pages.

### Partitioning

- Partition track metadata by `trackId`.
- Partition playlist items by `playlistId`.
- Partition user library by `userId`.
- Partition listening events by `userId` for user features or `trackId` for track analytics.
- Partition audio object paths by hashed key to avoid hot prefixes.

### Replication And Consistency

- Rights checks for playback should be correct or fail closed.
- Playlist edits should be read-after-write consistent for the editing user.
- Search and recommendations can lag behind catalog changes.
- Listening events are eventually processed.
- Audio chunks are immutable and globally cacheable.

## 2.5 Scalability

### Horizontal Scaling

- Playback Service scales by playback starts and control commands.
- Manifest Service scales by track-change QPS and cache misses.
- CDN scales by segment request rate.
- Playlist Service scales by playlist reads/writes.
- Event pipeline scales by listening event volume.
- Recommendation Service scales by home/radio/autoplay requests.

### Hot Track And Playlist Strategy

- Pre-warm edge caches for major releases.
- Cache immutable audio segments aggressively.
- Cache popular playlist pages by version.
- Use origin shield for hot audio asset misses.
- Keep rights-sensitive cache entries short-lived or context-specific.

### Playlist Scaling

- Store playlist items in pages/chunks for large playlists.
- Use playlist version for optimistic concurrency and cache invalidation.
- For highly followed playlists, fan out updates through cache invalidation rather than per-user writes.
- Generated playlists can be materialized per user periodically.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Search/home API | 50-250 ms |
| Start playback | 50-200 ms |
| Manifest lookup | 10-80 ms |
| First audio chunk from CDN | 30-300 ms |
| Playlist edit | 20-150 ms |
| Listening event ingest | async, low client overhead |

### Optimization Rules

- Prefetch next track and first chunks before current track ends.
- Cache manifests for common codec/quality combinations.
- Keep playback independent from recommendations.
- Batch listening events.
- Use paginated playlist reads.
- Use immutable audio asset paths for long CDN TTL.

## 2.7 Async Systems

Use streams for:

- Listening events.
- Playlist mutations.
- Library save/follow events.
- Catalog publish events.
- Rights update events.
- Recommendation feature updates.
- Royalty/accounting events if in scope.

Queue notes:

- Consumers must be idempotent.
- Listening events may arrive late or duplicated.
- Playlist mutation events should preserve order per playlist.
- Rights update events should invalidate affected caches.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated APIs for playback, playlists, library, and downloads.
- Signed stream URLs with expiration.
- Encrypted offline downloads.
- Entitlement checks for premium quality/offline playback.
- Encryption in transit and at rest.

Privacy:

- Listening history is sensitive personal data.
- Private playlists and libraries must not leak through search/recs.
- Support deletion/export of listening history if required.
- Be careful with friend/social activity visibility.

Abuse controls:

- Rate limit playback starts, playlist mutations, and search scraping.
- Detect fake streams or bot listening for royalty abuse.
- Prevent playlist spam and malicious collaborative edits.
- Validate clients for offline entitlement renewal.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Playback | start latency, start failure rate, chunk error rate, skip due to unavailable track |
| CDN | edge hit ratio, origin egress, hot asset misses, regional errors |
| Catalog/Rights | rights check latency, unavailable mismatch, catalog index lag |
| Playlist | mutation latency, conflict rate, stale cache rate |
| Recommendations | home latency, fallback rate, generated playlist freshness |
| Events | listening event ingest lag, duplicate rate, dropped event rate |
| Offline | entitlement renewal failure, expired download attempts |

Alerts:

- Playback start failures spike.
- CDN hit ratio drops or origin egress spikes.
- Rights check errors increase.
- Playlist mutation conflicts or latency surge.
- Listening event stream lag grows.
- Recommendation fallback rate jumps.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Audio delivery | CDN chunks | app-server streaming | scale/latency vs cache/key complexity |
| Quality | pre-encoded bitrates | on-demand transcode | low playback latency vs storage cost |
| Playlist consistency | strong versioned writes | eventual conflict merge | user trust vs collaboration flexibility |
| Rights cache | short-lived cache | live check every time | latency/availability vs correctness |
| Recommendations | real-time ranking | precomputed mixes | freshness vs cost/latency |
| Listening events | capture everything | sample events | analytics quality vs pipeline cost |

Interview framing:

> I would keep playback fast and CDN-backed, enforce rights before stream URL issuance, store playlists as durable ordered collections, and use listening events asynchronously for recommendations, analytics, and royalty systems.

---

# 3. Low-Level Design

LLD goal:

> Model Spotify around catalog tracks, audio assets, rights rules, playback sessions, queue context, playlists, library state, and listening events.

Simple rules:

- Do not serve audio bytes from business services.
- Do not let recommendations block active playback.
- Playlist mutations need ordering, idempotency, and versioning.
- Rights checks must happen before playback URL issuance.

Starter map:

| LLD question | Spotify answer |
|---|---|
| What is durable? | catalog, audio assets, playlists, library, listening events |
| What is ephemeral? | playback sessions, queue hints, signed URLs |
| What is hot? | popular tracks, playlists, home recommendations |
| What is eventually consistent? | recommendations, analytics, search indexing |
| What must be correct? | rights, private playlists, playlist mutation order |

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Track` | metadata and state | unavailable tracks cannot be played |
| `AudioAsset` | codec, bitrate, object path | must match requested quality/device |
| `RightsRule` | country/tier/time availability | must pass before playback |
| `PlaybackSession` | user/device/current context | expires and uses signed URLs |
| `Queue` | current and next tracks | should skip unavailable tracks |
| `Playlist` | owner, visibility, version | mutations change version |
| `PlaylistItem` | ordered track entry | order is stable within version |
| `ListeningEvent` | playback telemetry | append-only and retry-safe |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `CatalogService` | track/artist/album metadata | playlist ordering |
| `RightsService` | availability checks | recommendation ranking |
| `PlaybackService` | session and queue orchestration | deliver chunks directly |
| `ManifestService` | choose audio asset and URLs | mutate playlists |
| `PlaylistService` | playlist writes/reads | enforce track rights alone |
| `LibraryService` | saves/follows | stream audio |
| `RecommendationService` | home/radio/mixes | final playback entitlement |
| `EventIngestService` | listening events | synchronous user commands |

## 3.2 OOP Fundamentals

Encapsulation:

- `Playlist` owns versioned mutation rules.
- `RightsRule` owns availability checks.
- `Queue` owns next playable track selection.

Abstraction:

- `AudioStorageClient` hides object store/CDN details.
- `RightsChecker` hides licensing rule storage.
- `RecommendationProvider` hides ranking/model implementation.

Polymorphism:

- Different playback contexts: album, playlist, radio, podcast.
- Different audio selectors for free, premium, high-quality, offline.
- Different recommendation strategies for home, radio, and generated mixes.

Composition:

- `PlaybackService` composes catalog, rights, manifest, queue, and event publisher.

## 3.3 SOLID Principles

| Principle | Spotify application |
|---|---|
| Single Responsibility | `PlaylistService` manages playlist state only |
| Open/Closed | add lossless quality without rewriting rights/playback flow |
| Liskov Substitution | any `AudioUrlSigner` must produce expiring playable URLs |
| Interface Segregation | separate playback, playlist, catalog, library, event APIs |
| Dependency Inversion | services depend on repositories/checkers, not concrete databases |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | audio quality and recommendation strategy | choose behavior by tier/context |
| Command | playlist mutations | durable, idempotent edit operations |
| State | playback session lifecycle | active, paused, expired |
| Observer/Event Publisher | listening events and playlist mutations | decouple analytics/recs |
| Decorator | metrics, rate limiting, retry wrappers | cross-cutting concerns |

## 3.5 UML / Diagrams

### Playback Sequence

```text
Client -> PlaybackService: startPlayback(context, trackId)
PlaybackService -> CatalogService: fetch track
PlaybackService -> RightsService: canPlay(user, track, country)
PlaybackService -> QueueService: resolve queue context
PlaybackService -> ManifestService: choose asset and sign URLs
PlaybackService -> SessionStore: create playback session
PlaybackService -> Client: manifest + next track hints
Client -> CDN: fetch audio chunks
Client -> EventIngest: listening events
```

### Playlist Mutation Sequence

```text
Client -> PlaylistService: mutate(playlistId, clientMutationId, op)
PlaylistService -> PermissionService: validate owner/collaborator
PlaylistService -> IdempotencyStore: check mutation
PlaylistService -> PlaylistRepository: apply mutation if version matches
PlaylistService -> EventStream: publish playlist.mutated
PlaylistService -> Client: new playlist version
```

## 3.6 Class Design

Interfaces:

```java
interface RightsChecker {
    boolean canPlay(User user, Track track, String country);
}

interface AudioAssetSelector {
    AudioAsset select(Track track, Device device, Quality requestedQuality);
}

interface PlaylistRepository {
    Playlist get(String playlistId);
    Playlist applyMutation(String playlistId, PlaylistMutation mutation, long expectedVersion);
}

interface PlaybackSessionStore {
    PlaybackSession create(User user, Device device, PlaybackContext context);
}

interface ListeningEventPublisher {
    void publish(ListeningEvent event);
}
```

Design notes:

- `applyMutation()` should be idempotent with `clientMutationId`.
- `select()` should consider subscription tier, device, and network.
- `canPlay()` should be called at stream URL issuance time.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| track unavailable in region | skip or show unavailable based on context |
| duplicate playlist add retry | idempotency returns same playlist version |
| collaborative edits conflict | optimistic version check and refresh |
| user goes offline | use encrypted downloaded assets if entitlement valid |
| high-quality asset missing | fall back to normal quality |
| recommendation service down | use cached mixes/library/followed artists |
| listening events duplicated | dedup by event ID or tolerate in aggregation |
| private playlist shared accidentally | enforce visibility on every read/share |
| CDN miss storm on new album | pre-warm and origin shield |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
spotify/
  domain/
    Track.java
    AudioAsset.java
    Playlist.java
    PlaylistMutation.java
    PlaybackSession.java
    ListeningEvent.java
  service/
    PlaybackService.java
    PlaylistService.java
    CatalogService.java
    RightsService.java
    RecommendationService.java
  port/
    TrackRepository.java
    PlaylistRepository.java
    SessionStore.java
    EventPublisher.java
    UrlSigner.java
  adapter/
    InMemoryTrackRepository.java
    InMemoryPlaylistRepository.java
  app/
    SpotifyDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass, field
from typing import Dict, List, Set


@dataclass
class Track:
    track_id: str
    name: str
    countries: Set[str]
    assets_by_quality: Dict[str, str]
    explicit: bool = False


@dataclass
class User:
    user_id: str
    country: str
    premium: bool
    allow_explicit: bool = True


@dataclass
class Playlist:
    playlist_id: str
    owner_id: str
    tracks: List[str] = field(default_factory=list)
    version: int = 0
    applied_mutations: Set[str] = field(default_factory=set)


class InMemorySpotify:
    def __init__(self) -> None:
        self.tracks: Dict[str, Track] = {}
        self.playlists: Dict[str, Playlist] = {}

    def add_track(self, track: Track) -> None:
        self.tracks[track.track_id] = track

    def create_playlist(self, playlist_id: str, owner_id: str) -> Playlist:
        playlist = Playlist(playlist_id=playlist_id, owner_id=owner_id)
        self.playlists[playlist_id] = playlist
        return playlist

    def add_track_to_playlist(self, playlist_id: str, user_id: str, track_id: str, mutation_id: str) -> Playlist:
        playlist = self.playlists[playlist_id]
        if playlist.owner_id != user_id:
            raise ValueError("not playlist owner")
        if mutation_id in playlist.applied_mutations:
            return playlist
        if track_id not in self.tracks:
            raise ValueError("track not found")
        playlist.tracks.append(track_id)
        playlist.version += 1
        playlist.applied_mutations.add(mutation_id)
        return playlist

    def start_playback(self, user: User, track_id: str, requested_quality: str) -> str:
        track = self.tracks[track_id]
        if user.country not in track.countries:
            raise ValueError("track unavailable in country")
        if track.explicit and not user.allow_explicit:
            raise ValueError("explicit content blocked")
        quality = requested_quality
        if requested_quality == "HIGH" and not user.premium:
            quality = "NORMAL"
        asset = track.assets_by_quality.get(quality) or track.assets_by_quality["NORMAL"]
        return f"https://cdn.example/{asset}?sig=short-lived"


app = InMemorySpotify()
app.add_track(Track("t1", "System Design Song", {"US", "IN"}, {"NORMAL": "t1/normal", "HIGH": "t1/high"}))
app.create_playlist("p1", "u1")
app.add_track_to_playlist("p1", "u1", "t1", "mut-1")
url = app.start_playback(User("u1", "US", premium=True), "t1", "HIGH")
print(url)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[trackId -> Track]` | catalog lookup |
| `dict[playlistId -> Playlist]` | playlist state |
| `list[trackId]` | ordered playlist items |
| `set[mutationId]` | idempotent playlist mutations |
| `dict[(userId,trackId) -> state]` | library/offline entitlement simulation |
| `append-only list[ListeningEvent]` | telemetry simulation |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent playlist edits from multiple devices.
- Duplicate mutation retries.
- Track rights changing while playback cache is warm.
- Listening events arriving late or duplicated.

Handling strategy:

- Playlist version and optimistic concurrency.
- Idempotency key per playlist mutation.
- Short TTL rights cache and playback-time checks.
- Event dedup or idempotent aggregations.

## 4.5 Testing Thinking

Unit tests:

- Non-owner cannot mutate playlist.
- Duplicate mutation does not add track twice.
- Region-unavailable track cannot play.
- Free user is downgraded from high quality.
- Explicit filter blocks explicit tracks.

Load tests:

- Major album release playback spike.
- Popular playlist read traffic.
- Playlist mutation burst.
- Listening event stream ingestion.
- CDN miss storm on new tracks.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Album release | global midnight release | hot audio chunks and metadata |
| Viral track | social trend | CDN hot keys and event spikes |
| Generated playlist refresh | weekly personalized playlists | recommendation/cache pressure |
| Playlist edit storm | collaborative playlist activity | write conflicts and cache churn |
| CDN outage | edge degradation | playback failures and origin overload |

## 5.2 Immediate Spike Response

1. Protect playback start and audio chunk delivery.
2. Pre-warm major releases and popular playlist assets.
3. Use origin shield and immutable asset paths.
4. Degrade home recommendations to cached mixes/editorial rows.
5. Batch or sample lower-priority listening telemetry if needed.
6. Rate-limit abusive playlist/search/playback clients.
7. Keep rights checks fast and fail closed.

## 5.3 Degradation Policy

Protect in this order:

1. Current audio playback and next-track prefetch.
2. New playback starts and rights checks.
3. Playlist/library writes.
4. Search and basic catalog.
5. Home/recommendations.
6. Analytics freshness.
7. Social/activity features.

Not allowed:

- Serve tracks without valid rights.
- Leak private playlists.
- Corrupt playlist order.
- Let CDN miss storm overload origin storage.

## 5.4 Spike Interview Answer

> During spikes I protect playback and rights first. Hot tracks are handled with CDN pre-warming, immutable chunks, origin shield, and metadata caching. Playlist and library writes remain durable, while recommendations and analytics can degrade to cached or delayed modes.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global routing
  -> regional API/playback services
  -> nearby CDN edge for audio chunks/artwork
  -> regional origin shield
  -> durable audio object storage
  -> replicated catalog, playlist, and event systems
```

## 6.2 Multi-Region Strategy

- Route playback control to nearby healthy region.
- Serve chunks from nearest CDN edge.
- Replicate catalog metadata globally with regional rights filters.
- Partition playlists and libraries by user/playlist owner.
- Process listening events regionally, aggregate globally.
- Support offline entitlement renewal from regional services.

## 6.3 Storage And Retention

- Master audio files in durable object storage.
- Encoded audio assets in hot/warm object storage and CDN.
- Artwork cached aggressively.
- Playlist mutations retained for audit/rebuild if needed.
- Listening events retained according to analytics, royalty, and privacy policy.
- Offline entitlements expire and require periodic renewal.

## 6.4 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Playback | stateless services, cache manifests, prefetch next track |
| CDN | edge caching, pre-warming, origin shield |
| Catalog/Rights | replicated reads, short-TTL rights cache |
| Playlist | playlist-partitioned writes, versioned pages |
| Library | user-partitioned KV/wide-column store |
| Events | partitioned streams, idempotent consumers |
| Recommendations | offline features + online ranking + cached fallbacks |
| Offline | encrypted assets and renewable entitlements |

## 6.5 Global Interview Answer

> I would scale Spotify by separating playback control from audio delivery. Playback checks rights, resolves context, selects a compatible audio asset, and returns signed CDN URLs. Audio chunks are immutable and edge-cached. Playlists and library state are durable user-data systems, while listening events asynchronously power recommendations, analytics, and royalty reporting.

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
I will clarify music vs podcasts, free vs premium, offline support, playlists, rights, and recommendation scope.
I will estimate concurrent listeners, segment requests/sec, CDN bandwidth, playlist writes, and listening event volume.
HLD includes catalog/rights, playback service, manifest service, audio object storage, CDN, playlist/library services, event pipeline, and recommendations.
For streaming, I use pre-encoded audio assets, small chunks or byte ranges, signed URLs, and CDN caching.
For correctness, rights checks and private playlist permissions are enforced; recommendations and analytics are async.
For spikes, I pre-warm releases, protect CDN/origin, cache hot playlists, and degrade recommendations first.
For global scale, I route playback regionally, serve chunks from edge, partition user data, and process listening events asynchronously.
```

---

# 8. Fast Recall Rules

- Spotify is audio-first: lower bandwidth than video, but massive concurrency and segment QPS.
- Playback control plane checks rights and returns signed CDN URLs.
- CDN serves audio chunks; business services do not stream bytes.
- Tracks have multiple encoded assets by codec/quality.
- Playlists are ordered, versioned, durable collections.
- Playlist mutations need idempotency.
- Listening events power recommendations, analytics, and royalty systems.
- Recommendations should not interrupt active playback.
- Offline downloads need encrypted assets and renewable entitlement.
- Rights checks and private playlist access must fail closed.
