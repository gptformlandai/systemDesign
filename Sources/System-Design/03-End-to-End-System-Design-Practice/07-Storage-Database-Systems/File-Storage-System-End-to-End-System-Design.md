# File Storage System - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For File Storage systems, optimize durability, upload/download performance, metadata correctness, and lifecycle cost controls.

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

| Layer | Interview signal | File storage system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | upload/download, folders, sharing, versioning, metadata, retention |
| HLD | Can design scalable systems | object storage, metadata service, upload sessions, CDN, authz, lifecycle jobs |
| LLD | Can model maintainable components | `FileObject`, `UploadSession`, `Chunk`, `Metadata`, `AccessPolicy` |
| Machine coding | Can implement critical path | initiate upload, chunk upload, finalize, fetch metadata, signed URL |
| Traffic spikes | Can protect production | large upload surges, popular-download hot objects, cache stampede |
| Billion users | Can reason at global scale | multi-region object replicas, partitioned metadata, CDN edge strategy |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- User can upload files of varying sizes.
- User can download files securely.
- Support resumable multipart uploads.
- Store file metadata (name, owner, size, checksum, content type, tags).
- Support folder-like logical organization.
- Support sharing with permissions and expiring links.
- Support file versioning and soft delete.

Optional requirements to clarify:

- Max file size and per-user storage quota?
- Is cross-region upload/download needed?
- Is server-side encryption mandatory?
- Should antivirus/malware scanning be inline or async?
- Is deduplication (global or per-user) required?
- Is collaborative editing out of scope?

Out of scope unless interviewer asks:

- Real-time document editing conflicts.
- Full data-loss prevention stack.
- Full content search indexing pipeline internals.

## 1.2 Non-Functional Requirements

Durability and reliability:

- Very high durability for file objects.
- High availability for metadata and download path.
- Resumable upload robustness on flaky networks.

Performance:

- Efficient large-file uploads via multipart/chunking.
- Low-latency metadata operations.
- Fast global downloads via CDN/edge.

Security/compliance:

- Authn/authz on object and folder scopes.
- Encryption in transit and at rest.
- Audit logs for sensitive operations.

## 1.3 Constraints

- Object payload and metadata have different access patterns.
- Large files need chunking and parallel upload.
- Hot objects can create read amplification.
- Region and compliance rules may restrict data placement.
- Background lifecycle tasks (tiering, cleanup) are unavoidable.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| DAU | 200 million |
| Uploads/day | 3 billion objects/day |
| Downloads/day | 20 billion object reads/day |
| Average object size | 5 MB (high skew) |
| Hot object skew | top 1% objects get majority of reads |
| Durability target | 11 nines-style object durability |
| Availability target | 99.99% metadata APIs |

Back-of-the-envelope:

- `3B uploads/day * 5 MB` means enormous ingest volume with skew.
- Multipart uploads can massively increase request count.
- Download traffic is highly cacheable but can spike on hot content.
- Metadata growth and index strategy are as important as blob durability.

## 1.5 Clarifying Questions To Ask

- Are we building user cloud drive, media store, or internal blob store?
- Required consistency for metadata reads after upload finalize?
- What are retention and deletion semantics?
- Is legal hold/version retention in scope?
- How should shared links be authorized and expired?

Strong interview framing:

> I will separate object payload storage from metadata, use multipart resumable uploads, signed URL access, and asynchronous lifecycle pipelines for scan, tiering, and cleanup while preserving strong metadata correctness.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Upload flow:
Client
  -> API Gateway
  -> Upload Session Service
  -> Signed part URLs
  -> Object Store part uploads
  -> Finalize API
  -> Metadata commit
  -> Async scan/index events

Download flow:
Client
  -> API Gateway / CDN
  -> Authorization check
  -> Signed object URL or proxy stream
  -> CDN/Object Store
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
            +------------------------+
            |                        |
            v                        v
+-----------------------+   +----------------------+
| Upload Session Svc    |   | Metadata Service     |
+-----------+-----------+   +----------+-----------+
            |                          |
            v                          v
+-----------------------+   +----------------------+
| Object Store          |   | Metadata DB + Cache  |
| (multipart objects)   |   +----------------------+
+-----------+-----------+
            |
            v
+-----------------------+   +----------------------+
| Event Stream          |-->| Async Workers        |
| (scan/index/lifecycle)|   | scan, index, tiering |
+-----------------------+   +----------------------+
            |
            v
+-----------------------+
| CDN / Edge Delivery   |
+-----------------------+
```

Request flow for multipart upload:

1. Client calls `POST /uploads/init` with file metadata.
2. Service creates upload session and returns signed URLs for parts.
3. Client uploads parts directly to object store.
4. Client calls finalize with uploaded part list and checksum.
5. Service validates parts and commits metadata atomically.
6. Async events trigger malware scan/indexing/thumbnailing.

## 2.2 APIs

### Initiate Upload

```http
POST /v1/files/uploads/init
Authorization: Bearer <token>
Content-Type: application/json

{
  "fileName": "design.pdf",
  "size": 52428800,
  "contentType": "application/pdf",
  "checksum": "sha256:..."
}
```

Response:

```json
{
  "uploadId": "up-1289",
  "objectKey": "u42/2026/06/design.pdf",
  "partSize": 8388608,
  "signedPartUrls": ["https://..."]
}
```

### Finalize Upload

```http
POST /v1/files/uploads/{uploadId}/complete
Authorization: Bearer <token>
Content-Type: application/json

{
  "parts": [{"partNumber": 1, "etag": "abc"}],
  "finalChecksum": "sha256:..."
}
```

### Get File Metadata

```http
GET /v1/files/{fileId}
Authorization: Bearer <token>
```

### Get Download URL

```http
POST /v1/files/{fileId}/download-url
Authorization: Bearer <token>
Content-Type: application/json

{ "expiresInSeconds": 300 }
```

### Soft Delete File

```http
DELETE /v1/files/{fileId}
Authorization: Bearer <token>
```

Important points:

- Upload complete should be idempotent.
- Download URLs should be signed and short-lived.
- Metadata and ACL checks must precede download issuance.

## 2.3 Core Components

Think of file storage as three cooperating planes:

| Plane | What it handles | Examples |
|---|---|---|
| Control plane | decisions and metadata | auth, upload sessions, file metadata, ACLs, folder listing |
| Data plane | bytes moving in/out | multipart upload parts, object store reads, CDN downloads |
| Background plane | work that should not block users | malware scan, thumbnailing, indexing, tiering, cleanup, audit |

This separation is the most important mental model. The API servers should not carry giant file bytes unless there is a strong reason. They should validate, authorize, create sessions, issue signed URLs, commit metadata, and let object storage/CDN move bytes at scale.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| API Gateway + Auth | request validation, authn/authz entry point, rate limits | file bytes, durable metadata | request QPS |
| Upload Session Service | multipart lifecycle, signed part URLs, finalize workflow | long-term metadata ownership | active uploads and finalize QPS |
| Metadata Service | file records, versions, folder listing, object state | raw object bytes | metadata reads/writes |
| Object Storage Layer | durable bytes, multipart objects, replication, checksums | user-facing permissions | storage volume and byte throughput |
| Access Control Service | permissions, share links, ACL inheritance, policy checks | object persistence | authz checks/sec |
| Share Link Service | external scoped access, token hash, expiry, revocation | raw permission model ownership | share-link creation/use QPS |
| Quota/Billing Service | storage limits, reservations, usage counters, billing dimensions | file byte storage | tenant/user usage volume |
| CDN/Edge Layer | global read acceleration, range caching, origin shielding | canonical metadata truth | download traffic and hot-object skew |
| Event Stream | durable async handoff | business decisions | event volume and replay needs |
| Async Workers | scan, index, thumbnail, lifecycle, audit fanout | synchronous upload acceptance | queue lag and job cost |
| Lifecycle/Retention Service | tiering, retention, legal hold, cleanup | immediate upload/download decisions | object count and policy volume |
| Observability/Audit | traces, metrics, audit logs, integrity alarms | business mutation ownership | event and telemetry volume |

### API Gateway + Auth Layer

Why it exists:

- It is the front door for all client requests.
- It prevents invalid, abusive, or unauthorized requests from reaching expensive backend systems.
- It gives one place to apply authentication, coarse authorization, throttling, WAF rules, request size limits, and tenant/user quotas.

Key responsibilities:

- Verify user/session tokens.
- Validate request shape and enforce max file size policy before upload starts.
- Apply per-user, per-tenant, and per-IP rate limits.
- Route metadata requests to metadata services and upload requests to upload session services.
- Attach request IDs, tenant IDs, and trace context for downstream observability.

What it should avoid:

- Do not stream huge file payloads through the gateway for normal uploads/downloads.
- Do not make final storage decisions by itself.
- Do not keep upload session state in memory.

Scaling and failure notes:

- Scale horizontally because it is stateless.
- Use admission control during upload storms.
- If downstream metadata is slow, return fast, clear errors instead of letting connections pile up.

Interview signal:

> The gateway protects the control plane. It authenticates, validates, rate-limits, and routes, but large file bytes should normally bypass it through signed object-store URLs.

### Upload Session Service

Why it exists:

- Large files cannot reliably be uploaded as one request.
- Mobile and browser clients need resumability after network failures.
- Backend API servers should not hold long-lived upload streams.

Core responsibilities:

- Create an upload session for a planned file upload.
- Decide part size based on file size and policy.
- Create or reserve the target object key.
- Generate short-lived signed URLs for each part.
- Track session status: `IN_PROGRESS`, `COMPLETING`, `COMPLETED`, `EXPIRED`, `ABORTED`.
- Validate final part list, ETags/checksums, ownership, and expiry.
- Complete multipart upload in object storage.
- Commit final metadata only after the object is durably completed.

Important data it owns:

- `uploadId`
- `ownerId`
- `objectKey`
- expected size/content type/checksum
- part size and uploaded part metadata
- expiry timestamp
- current status/version

Idempotency rules:

- `initUpload` can be idempotent with a client upload key if product needs retry-safe init.
- `uploadPart` is naturally retryable by `(uploadId, partNumber)`.
- `completeUpload` must be idempotent. If the object was already completed, return the same final file metadata.

Failure modes:

| Failure | Handling |
|---|---|
| client disconnects mid-upload | session remains resumable until expiry |
| part upload retry | overwrite or re-confirm same part number safely |
| finalize called twice | return canonical completed result |
| finalize checksum mismatch | reject completion and keep/abort session by policy |
| object-store complete succeeds but metadata commit fails | repair job detects orphan object and retries metadata commit or quarantines object |

Scaling notes:

- Keep service stateless; store session state in DB/KV.
- Use short-lived signed URLs to reduce server involvement.
- Expire incomplete uploads with cleanup jobs to prevent storage leaks.

Interview signal:

> Upload Session Service is the coordinator for resumable multipart upload. It does not store file bytes; it coordinates direct upload to object storage and commits metadata only after durable object completion.

### Metadata Service

Why it exists:

- Object storage is excellent for bytes but weak for product queries like folders, owners, versions, shares, search filters, and soft deletes.
- Metadata is the control-plane source of truth for what a file means to the product.

Core responsibilities:

- Store file metadata: owner, name, size, MIME type, checksum, storage key, created/updated timestamps.
- Track logical folder/path relationships.
- Track file state: `UPLOADING`, `ACTIVE`, `QUARANTINED`, `SOFT_DELETED`, `PURGED`.
- Manage versions and point the current file to the active version.
- Support list APIs with cursor pagination.
- Support rename/move operations without moving the raw object bytes when possible.
- Expose metadata for download authorization and signed URL generation.

Important design decision:

- Metadata and object bytes should not be committed independently without a recovery story.
- The usual rule is: object upload completes first, then metadata commit makes it visible.
- If metadata commit fails after object completion, an orphan cleanup/repair job must reconcile it.

Metadata consistency expectations:

| Operation | Recommended consistency |
|---|---|
| read after upload finalize | strong or read-your-write |
| rename/move | strong for owner view |
| listing large folders | cursor-based, may use indexed eventual projections for aggregates |
| search index | eventual |
| thumbnail availability | eventual |

Scaling notes:

- Partition by tenant/user and file ID hash.
- Use separate indexes for listing by parent folder and created time.
- Avoid unbounded folder scans; always paginate.
- Cache hot metadata briefly, but invalidate or version-check after writes.

Failure modes:

- Metadata row exists but object is missing: mark inconsistent and repair from object inventory or fail safely.
- Object exists but metadata missing: orphan cleanup or delayed metadata commit retry.
- Stale cache after permission change: use short TTL/version checks for ACL-sensitive records.

Interview signal:

> Metadata Service is the product source of truth. Object storage stores bytes; metadata stores ownership, path, version, state, ACL references, and the pointer to the object key.

### Object Storage Layer

Why it exists:

- File payloads need extremely high durability, large capacity, and efficient byte-range access.
- Object storage scales better for blobs than a relational DB or application server disk.

Core responsibilities:

- Store raw file bytes as immutable or versioned objects.
- Support multipart upload and completion.
- Maintain object checksums/ETags.
- Replicate objects within a region and optionally across regions.
- Support lifecycle tiering to hot/warm/cold storage.
- Support range reads for video, PDFs, archives, and resumable downloads.

What it does not decide:

- Whether a user is allowed to read a file.
- Whether a file is visible in a folder.
- Whether a file should be shown after malware scan.

Object key strategy:

```text
tenantId/userId/date/randomOrContentHash/version
```

Good object keys should:

- Avoid hot prefixes.
- Avoid exposing sensitive user data directly.
- Be stable once metadata points to them.
- Support versioning and lifecycle policies.

Durability and integrity:

- Use replication/erasure coding depending storage backend.
- Store checksums and verify during finalize.
- Run periodic object inventory and integrity checks.
- Keep deleted object tombstones or version markers if recovery is required.

Failure modes:

| Failure | Handling |
|---|---|
| upload part missing | reject finalize |
| object store region degraded | route uploads/downloads to allowed replica region |
| checksum mismatch | quarantine/reject object |
| lifecycle move partially fails | retry idempotently and keep metadata state unchanged until confirmed |

Interview signal:

> Object storage is the durable byte plane. It should be optimized for large payloads, multipart upload, replication, range reads, and lifecycle tiering, while metadata and ACL decisions remain outside it.

### Access Control Service

Why it exists:

- File storage is dangerous if permissions are treated as an afterthought.
- Every metadata read, upload finalize, share action, and download URL issuance must be authorized.

Core responsibilities:

- Evaluate owner, folder, team, tenant, and share-link permissions.
- Support roles such as `OWNER`, `EDITOR`, `VIEWER`, `COMMENTER` if product needs them.
- Validate expiring share links by hashed token.
- Enforce revocation and blocklist rules.
- Log sensitive access decisions for audit.

Permission models:

| Model | Description | Best fit |
|---|---|---|
| owner-only | only owner accesses file | personal storage/simple apps |
| ACL per file | explicit users/groups on each file | sharing products |
| inherited folder ACL | permissions flow from parent folder | drive-like systems |
| signed link | bearer token grants scoped access until expiry | external sharing/downloads |
| policy/RBAC/ABAC | rules based on roles, tenant, attributes | enterprise/compliance systems |

Important security rule:

- A signed URL should be issued only after authorization.
- The URL should be scoped to one object/version, one operation, and a short expiry.
- If immediate revocation is required, use short TTLs or proxy downloads through an auth-checking layer.

Failure modes:

- ACL cache stale after revoke: keep TTL short and version ACL records.
- Share token leaked: short expiry, token hashing, download limits, optional password/device checks.
- Folder inheritance bug: record effective ACL version and test move/rename cases heavily.

Interview signal:

> Access Control Service is in the critical path before metadata exposure and signed URL generation. It protects data privacy even when CDN/object storage can technically serve the bytes.

### Share Link Service

Why it exists:

- Sharing is a product workflow, not just an ACL check.
- External users may need temporary access without becoming permanent collaborators.
- Link leakage risk must be bounded through expiry, scope, and revocation.

Core responsibilities:

- Generate high-entropy share tokens and store only token hashes.
- Bind a link to file ID, version policy, permission scope, owner, expiry, and optional password/device rules.
- Validate link expiry, revocation, file state, and scope before download URL generation.
- Support link revoke and audit events.
- Optionally enforce download limits or domain restrictions.

Important design choice:

| Choice | Meaning | Trade-off |
|---|---|---|
| link points to latest file | recipient always sees latest version | easier collaboration, harder rollback semantics |
| link points to immutable version | recipient sees exact shared version | safer auditability, may surprise users expecting latest |

Failure modes:

- Token leaked: limit blast radius using expiry, token hashing, scope, and revocation.
- File deleted/quarantined: link must stop working even if token is valid.
- Link cache stale after revoke: use short TTL and versioned link records.

Interview signal:

> Share Link Service controls external access with scoped, expiring, revocable tokens. It should still call Access Control and respect file state before issuing signed URLs.

### Quota and Billing Service

Why it exists:

- Storage looks infinite to users, but it has real cost.
- Uploads should not let one user or tenant exceed plan limits.
- Billing needs accurate usage by storage class, tenant, region, and time.

Core responsibilities:

- Check available quota before upload session creation.
- Reserve expected bytes during upload init.
- Commit actual bytes after successful finalize.
- Release reservation when upload aborts/expires.
- Decrement or defer usage release on delete depending soft-delete retention policy.
- Track usage by hot/warm/cold storage class and region.

Quota flow:

```text
initUpload -> reserve expected bytes
completeUpload -> commit actual bytes and release difference
abort/expire -> release reservation
softDelete -> usage may remain until purge, depending product policy
purge -> release billable bytes
```

Scaling notes:

- Large tenants can create hot quota counters.
- Use sharded counters and periodic reconciliation from metadata/object inventory.
- Keep quota checks fast on upload init, but reconcile drift asynchronously.

Failure modes:

- Quota service unavailable: fail closed for free/unknown tenants or allow limited grace for trusted enterprise tenants.
- Counter drift: reconciliation job repairs usage counters.
- Upload final size differs from expected size: commit actual bytes and adjust reservation.

Interview signal:

> Quota/Billing Service makes the storage system economically safe. It turns uploads, deletes, lifecycle tiering, and retention into controlled usage accounting.

### CDN / Edge Delivery Layer

Why it exists:

- Downloads dominate cost and traffic for many file systems.
- A small number of hot files can overwhelm origin object storage.
- Global users need lower latency than a single region can provide.

Core responsibilities:

- Cache popular object bytes and byte ranges near users.
- Reduce origin egress and origin request load.
- Support range requests for large files/media.
- Apply TLS, basic edge protections, and sometimes signed-cookie/signed-URL validation.

What should be cached:

- Public or share-link files with safe TTL.
- Immutable file versions.
- Thumbnails and previews.
- Range chunks for large media/files.

What should be cached carefully:

- Private files with short-lived signed URLs.
- Files with fast revocation requirements.
- Files in `QUARANTINED` or scan-pending state.

Hot object strategy:

- Use origin shielding so many edge misses collapse into one regional origin miss.
- Use request coalescing for simultaneous cache misses.
- Prewarm known viral/large campaign objects.
- Cache immutable versioned keys longer than mutable logical paths.

Failure modes:

| Failure | Handling |
|---|---|
| CDN purge creates origin spike | origin shield + rate limiting + prewarm |
| stale object after delete | short TTL, versioned keys, purge on critical delete |
| range-request abuse | rate limit and validate request ranges |
| origin down | serve stale-if-error only when security/product policy allows |

Interview signal:

> CDN is not just a performance optimization; it is the primary defense against hot download amplification and global latency.

### Event Stream / Queue Layer

Why it exists:

- Upload completion triggers many follow-up tasks, but most should not block the user.
- Events provide durable handoff, replay, and backpressure boundaries.

Events to publish:

- `upload.completed`
- `file.deleted`
- `file.permission_changed`
- `file.version_created`
- `scan.completed`
- `lifecycle.transition_requested`
- `object.integrity_failed`

Design requirements:

- Events should include stable IDs and versions.
- Consumers must be idempotent.
- DLQ is required for poison events.
- Queue lag must be observable by workload type.

Example event shape:

```json
{
  "eventId": "evt-991",
  "type": "upload.completed",
  "fileId": "f-991",
  "version": 3,
  "objectKey": "u42/2026/06/design.pdf",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Interview signal:

> The event stream turns upload completion into a reliable workflow without making scan, index, thumbnail, audit, and lifecycle work part of the synchronous request.

### Async Workers

Why they exist:

- Some work is expensive, slow, or dependency-heavy.
- Users should not wait for thumbnails, malware scan, search indexing, or tiering in the upload response unless product policy requires it.

Major worker categories:

| Worker | Job | User-visible impact |
|---|---|---|
| malware scan | validate file safety | file may stay `QUARANTINED` until clear |
| thumbnail/preview | generate image/PDF/video previews | preview appears later |
| search indexer | index file name/text/tags | search result appears later |
| audit logger | durable audit trail | compliance/forensics |
| lifecycle worker | tier/delete/retain objects | cost and compliance |
| integrity checker | compare metadata vs object inventory | repair and alerting |

Key rules:

- Workers must be idempotent by `(jobType, fileId, version)`.
- Worker output should update metadata with version checks.
- Slow workers should not block uploads/downloads unless the file is unsafe to expose.
- Heavy pipelines need backpressure and priority isolation.

Failure modes:

- Scan backlog: keep file in restricted/quarantined state and show processing status.
- Thumbnail failure: do not fail file availability; mark preview failed and retry later.
- Index backlog: search freshness degrades, but direct file access still works.
- Lifecycle bug: use dry-run, guardrails, canaries, and recovery windows before purge.

Interview signal:

> Async workers make the file product rich without making the core upload/download path fragile.

### Lifecycle and Retention Service

Why it exists:

- At scale, storage cost can dominate architecture cost.
- Files move through hot, warm, cold, archived, deleted, and legal-hold states.
- Compliance requires deterministic retention and deletion behavior.

Core responsibilities:

- Move old/infrequent files to cheaper storage classes.
- Enforce retention expiration and legal hold exceptions.
- Clean incomplete multipart uploads.
- Purge soft-deleted files after recovery window.
- Produce auditable deletion/tiering records.

Lifecycle examples:

| Policy | Example |
|---|---|
| incomplete upload cleanup | abort sessions older than 24 hours |
| soft delete window | recoverable for 30 days |
| cold tier transition | move files not read for 90 days |
| legal hold | prevent purge until hold released |
| version pruning | keep last N versions or versions newer than X days |

Safety rules:

- Never hard-delete based only on a cache value.
- Use metadata state + retention policy + object confirmation.
- Run lifecycle jobs with dry-run/canary mode for high-risk policies.
- Keep audit events for every permanent delete.

Interview signal:

> Lifecycle is where file storage becomes economically viable and compliant. It must be automated, observable, and guarded because mistakes can delete real customer data.

### Observability and Audit

Why it exists:

- File systems fail in subtle ways: stale permissions, orphan objects, scan backlog, hot prefixes, partial uploads, and CDN origin surges.
- Without observability, correctness and cost issues appear late.

Core metrics:

- Upload init/finalize latency and error rate.
- Multipart completion failure rate.
- Download p95/p99 and CDN cache hit ratio.
- Metadata DB latency and hot partitions.
- ACL deny/allow counts and suspicious access patterns.
- Async queue lag by job type.
- Object/metadata mismatch count.
- Storage growth by tenant/class/region.

Audit events:

- file uploaded
- file downloaded/shared
- permission changed
- share link created/revoked
- file deleted/restored/purged
- lifecycle transition executed
- admin access performed

Interview signal:

> Observability and audit are not add-ons. They are how you prove durability, privacy, compliance, and operational health in a file storage platform.

### How The Components Work Together

Upload path summary:

```text
Gateway authenticates -> Upload Session Service creates session -> client uploads parts to Object Store -> finalize verifies parts/checksum -> Metadata Service commits ACTIVE/QUARANTINED record -> Event Stream triggers scan/index/thumbnail/lifecycle workers
```

Download path summary:

```text
Gateway authenticates -> Metadata Service loads file/version -> Access Control Service authorizes -> Download URL service signs scoped URL -> CDN/Object Store serves bytes -> audit event emitted asynchronously
```

Delete path summary:

```text
Gateway authenticates -> Access Control authorizes delete -> Metadata state becomes SOFT_DELETED -> cache/CDN invalidation event published -> Lifecycle Service purges object later after retention window
```

One-stop interview answer:

> I split file storage into metadata/control plane, object byte/data plane, and async background plane. Metadata and ACL correctness stay in strongly controlled services. Large file bytes move directly through object storage and CDN using signed URLs. Async workers handle scan, preview, indexing, audit, tiering, and cleanup. This gives durability, scale, security, and cost control without overloading the synchronous APIs.

## 2.4 Data Layer

### Core Data Models

File metadata:

```json
{
  "fileId": "f-991",
  "ownerId": "u-42",
  "objectKey": "u42/2026/06/design.pdf",
  "fileName": "design.pdf",
  "size": 52428800,
  "contentType": "application/pdf",
  "checksum": "sha256:...",
  "version": 3,
  "state": "ACTIVE"
}
```

Upload session:

```json
{
  "uploadId": "up-1289",
  "ownerId": "u-42",
  "objectKey": "u42/2026/06/design.pdf",
  "partSize": 8388608,
  "status": "IN_PROGRESS",
  "expiresAt": "2026-06-17T13:00:00Z"
}
```

Share link:

```json
{
  "shareId": "s-771",
  "fileId": "f-991",
  "tokenHash": "...",
  "permission": "READ",
  "expiresAt": "2026-06-18T12:00:00Z"
}
```

### Store Choices

| Data type | Candidate store | Why |
|---|---|---|
| Object payload | object storage | high durability and scale |
| Metadata/ACL | relational or document DB | strong query and constraints |
| Upload sessions | KV/relational | frequent state updates |
| Share tokens | KV with TTL | fast validation |
| Async events | stream/queue | decoupled processing |

### Partitioning

- Partition metadata by owner/tenant and file ID hash.
- Partition object keys by prefix strategy to avoid hotspots.
- Separate hot and cold object index paths.

### Replication

- Multi-AZ replication for metadata DB.
- Cross-region replication or copy for objects based on policy.
- Strong backups and object integrity checks.

## 2.5 Scalability

### Horizontal Scaling

- Gateway and metadata services are stateless.
- Upload session workers scale by active multipart sessions.
- Async pipelines scale by queue lag.

### Hot Object Handling

- CDN edge caching for popular files.
- Origin shield and request coalescing.
- Signed URL with cache-key controls.

### Namespace and Listing Scale

- Use cursor-based listing.
- Avoid expensive deep folder scans synchronously.
- Precompute aggregate folder stats where needed.

## 2.6 Performance

### Caching Strategy

| Cache layer | What it stores | TTL |
|---|---|---:|
| Metadata cache | file metadata hot records | short |
| ACL cache | permission check results | short |
| Share token cache | validated token checks | short |
| CDN edge cache | hot object bytes/ranges | policy-based |

### Latency Budget Example

| Stage | Target |
|---|---:|
| Metadata lookup + authz | 10-40 ms |
| Signed URL generation | 5-20 ms |
| CDN/object first byte | geo and size dependent |
| Upload finalize commit | 20-100 ms |

### Optimization Rules

- Direct-to-object-store uploads minimize API server load.
- Use multipart parallel uploads for large files.
- Prefer edge delivery over origin downloads.

## 2.7 Async Systems

Use streams/queues for:

- upload-completed events
- malware scanning results
- thumbnail/transcode jobs
- lifecycle tiering/deletion
- audit and analytics events

Queue design notes:

- At-least-once worker processing with idempotent job handlers.
- DLQ for repeated scan/transcode failures.
- Backpressure on heavy media pipelines.

## 2.8 Reliability

### Retry and Idempotency

- Upload part retries are safe by part number + ETag.
- Finalize endpoint must be idempotent.
- Download URL generation is safe and repeatable.

### Circuit Breakers and Fallbacks

- If scan pipeline is delayed, file may remain quarantined state.
- If metadata cache fails, fallback to DB with stricter rate control.
- If one region is degraded, route reads to replica where policy allows.

### Failover

- Multi-region object replication for critical classes.
- Metadata DB failover with strict RPO/RTO targets.
- Restore through metadata backups + object inventory validation.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Upload path | proxy through API | direct-to-object-store | simpler control vs massive scalability |
| Consistency | strong metadata consistency | eventual everywhere | correctness vs latency/availability |
| Storage class | always hot tier | lifecycle tiering | latency vs cost |
| Sharing model | ACL-only | signed links + ACL | stricter control vs flexibility |
| Versioning | disabled | enabled | lower cost vs recoverability/audit |

Interview framing:

> I would keep metadata correctness strong, push byte transfer to object storage + CDN, and rely on async pipelines for scan/index/lifecycle so the core upload/download path stays resilient and scalable.

---

# 3. Low-Level Design

LLD goal:

> Model file storage as a split between upload session state, durable file metadata, object-store byte operations, access policy, sharing, scanning, and lifecycle cleanup.

Simple rule:

- `UploadSession` owns incomplete upload progress.
- `FileObject`/`FileVersion` own visible file metadata.
- `ObjectStoreClient` owns byte movement.
- Access, sharing, scan, and lifecycle rules stay explicit because mistakes can expose or delete real data.

Starter map:

| LLD question | File storage answer |
|---|---|
| What represents the logical file? | `FileObject` |
| What represents immutable bytes? | `FileVersion` pointing to an object key |
| What tracks resumable upload? | `UploadSession` and `UploadPart` |
| What moves bytes? | `ObjectStoreClient` |
| What protects access? | `AccessPolicy` and `ShareLink` |
| What controls deletion/tiering? | `LifecycleRule` |

Beginner-friendly design order:

1. Model `UploadSession` first, because large files are uploaded in parts.
2. Model `FileObject` separately from `FileVersion`; the logical file can have many versions.
3. Model `ObjectStoreClient` as a port/interface so S3/GCS/local storage can be swapped.
4. Model `AccessPolicy` and `ShareLink` before generating download URLs.
5. Make finalize idempotent and commit metadata only after object completion.
6. Add lifecycle and scan states so unsafe/deleted/incomplete files are not exposed.

Interview sentence:

> In LLD, I will separate upload session state, file metadata, immutable object versions, access policy, and object-store byte operations. The final file becomes visible only after multipart completion and metadata commit are safe.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `FileObject` | logical file identity, owner, folder, current version, state | private/deleted/quarantined files must not be served incorrectly |
| `FileVersion` | immutable object key, size, checksum, content type | version points to one durable object payload |
| `UploadSession` | multipart progress, expiry, finalization state | incomplete uploads are not visible files |
| `UploadPart` | part number, size, ETag/checksum | finalize requires all expected valid parts |
| `AccessPolicy` | ACL/role/share inheritance rules | authorization fails closed for private data |
| `ShareLink` | token hash, scope, expiry, revocation state | expired/revoked links cannot generate download URLs |
| `LifecycleRule` | retention, tiering, purge policy | destructive purge must respect retention/legal hold |
| `ScanResult` | malware/processing status | unsafe files remain restricted/quarantined |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `UploadService` | create session, sign part URLs, validate complete upload | store raw file bytes in app memory |
| `MetadataService` | create/read/update file metadata and versions | decide low-level object-store mechanics |
| `AccessControlService` | evaluate owner/ACL/share permissions | cache forever after permission changes |
| `ShareService` | create/revoke scoped external links | bypass file state checks |
| `LifecycleService` | tier/archive/purge files safely | hard-delete without retention checks |
| `ScanService` | scan and mark clean/quarantined | block unrelated metadata reads |
| `ObjectStoreClient` | multipart upload, signed URLs, range reads | decide product visibility rules |

Core flow:

```text
Upload: init session -> client uploads parts -> complete object -> commit metadata -> publish scan/lifecycle events
Download: load metadata -> authorize -> sign URL -> CDN/object store serves bytes
Delete: mark metadata soft-deleted -> revoke/purge cache -> lifecycle purges later
```

## 3.2 OOP Fundamentals

Encapsulation:

- `UploadSession` owns part-state and completion rules.
- `FileObject` owns state transitions (`UPLOADING -> ACTIVE -> DELETED`).
- `AccessPolicy` owns permission checks and inheritance behavior.

Abstraction:

- `ObjectStoreClient` hides storage provider details.
- `MetadataRepository` hides DB specifics.

Polymorphism:

- Different storage backends or regional adapters behind common interface.
- Different lifecycle policies by tenant/tier.

Composition over inheritance:

- `UploadService` composes checksum validator, session repo, object-store client, and metadata writer.

## 3.3 SOLID Principles

| Principle | File storage application |
|---|---|
| Single Responsibility | `ScanService` only manages malware scan workflow |
| Open/Closed | add new storage adapter without changing metadata service core |
| Liskov Substitution | all object store adapters satisfy upload/download contract |
| Interface Segregation | split upload, metadata, sharing, and lifecycle interfaces |
| Dependency Inversion | core services depend on repository/client abstractions |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | storage class/lifecycle policy selection | runtime flexibility |
| Factory | object-store adapter creation | provider isolation |
| State | upload and file lifecycle states | transition correctness |
| Observer/Event Publisher | upload-complete events to async processors | decoupling |
| Decorator | authz/metrics/tracing around repositories | cross-cutting concerns |

## 3.5 UML / Diagrams

### Class Diagram

```text
+---------------------+      +----------------------+
| UploadService       |----->| UploadSessionRepo    |
| +initUpload()       |      +----------------------+
| +completeUpload()   |
+----------+----------+
           |
           +-------> +----------------------+
           |         | ObjectStoreClient    |
           |         | +putPart()           |
           |         | +completeMultipart() |
           |         +----------------------+
           |
           +-------> +----------------------+
                     | MetadataRepository   |
                     +----------------------+
```

### Sequence Diagram - Multipart Upload

```text
Client -> UploadService: initUpload(fileName, size, checksum)
UploadService -> UploadSessionRepo: create session
UploadService -> ObjectStoreClient: create multipart upload
UploadService -> Client: uploadId + signed part URLs
Client -> ObjectStoreClient: upload part 1..N
Client -> UploadService: completeUpload(uploadId, parts)
UploadService -> ObjectStoreClient: complete multipart
UploadService -> MetadataRepository: save file metadata
UploadService -> EventBus: publish upload.completed
```

## 3.6 Class Design

Interfaces:

```java
interface UploadSessionRepository {
    UploadSession create(UploadSession session);
    Optional<UploadSession> findById(String uploadId);
    void updateStatus(String uploadId, UploadStatus status);
}

interface ObjectStoreClient {
    String initiateMultipart(String objectKey);
    String signPartUploadUrl(String objectKey, int partNumber, long expiresInSec);
    void completeMultipart(String objectKey, List<PartETag> parts);
    String generateSignedDownloadUrl(String objectKey, long expiresInSec);
}

interface MetadataRepository {
    FileMetadata save(FileMetadata metadata);
    Optional<FileMetadata> findById(String fileId);
}
```

Design notes:

- Finalize operation should verify part list + checksum.
- Metadata commit should happen only after object commit success.
- Share links should reference immutable file version when needed.

## 3.7 Data Handling

Machine-coding version:

- `ConcurrentHashMap<String, UploadSession>` for sessions.
- `ConcurrentHashMap<String, FileMetadata>` for metadata.
- `ConcurrentHashMap<String, Set<String>>` for file ACLs.
- Queue for async scan/lifecycle events.

Production version:

- Durable metadata DB with indexes.
- Object storage for payload bytes.
- Event stream for async processors.
- Cache layers for metadata and ACL.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| duplicate finalize after timeout | finalize is idempotent and returns canonical metadata |
| missing/reordered upload part | reject completion until required ETags/checksums match |
| checksum mismatch | reject/quarantine object and do not mark file active |
| session expires mid-upload | block finalize and cleanup orphan parts later |
| delete/move races with finalize | use version/state checks and transaction boundaries |
| share link expires during URL generation | deny before signed URL creation |
| scan fails or marks suspicious | keep file `PENDING_SCAN`/`QUARANTINED` by policy |
| object exists but metadata commit failed | repair job completes metadata or quarantines/deletes orphan |
| metadata points to missing object | mark inconsistent, fail safely, trigger repair/audit |
| CDN stale after delete/revoke | short TTL/purge/versioned keys for sensitive content |

Production rule of thumb:

> A file storage system should prefer delayed visibility over incorrect visibility. It is acceptable for a thumbnail or search index to arrive late; it is not acceptable to expose private, deleted, corrupted, or unscanned data incorrectly.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
file_storage/
  domain/
    FileMetadata.java
    UploadSession.java
    AccessGrant.java
  service/
    UploadService.java
    MetadataService.java
    AccessService.java
    ShareService.java
    LifecycleService.java
  port/
    ObjectStoreClient.java
    MetadataRepository.java
    UploadSessionRepository.java
  adapter/
    S3LikeObjectStoreClient.java
    InMemoryMetadataRepository.java
    InMemoryUploadSessionRepository.java
  app/
    FileStorageDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from collections import defaultdict
from datetime import datetime, timedelta, timezone


@dataclass
class UploadSession:
    upload_id: str
    owner_id: str
    object_key: str
    status: str
    expires_at: datetime


class InMemoryFileStorage:
    def __init__(self) -> None:
        self.sessions: dict[str, UploadSession] = {}
        self.objects: dict[str, bytes] = {}
        self.metadata: dict[str, dict] = {}
        self.parts: dict[str, dict[int, bytes]] = defaultdict(dict)

    def init_upload(self, owner_id: str, file_name: str) -> UploadSession:
        upload_id = f"up_{len(self.sessions) + 1}"
        object_key = f"{owner_id}/{file_name}"
        session = UploadSession(
            upload_id=upload_id,
            owner_id=owner_id,
            object_key=object_key,
            status="IN_PROGRESS",
            expires_at=datetime.now(timezone.utc) + timedelta(hours=1),
        )
        self.sessions[upload_id] = session
        return session

    def upload_part(self, upload_id: str, part_number: int, data: bytes) -> None:
        self.parts[upload_id][part_number] = data

    def complete_upload(self, upload_id: str) -> str:
        session = self.sessions[upload_id]
        if session.status != "IN_PROGRESS":
            return session.object_key
        assembled = b"".join(self.parts[upload_id][p] for p in sorted(self.parts[upload_id]))
        self.objects[session.object_key] = assembled
        self.metadata[session.object_key] = {
            "ownerId": session.owner_id,
            "size": len(assembled),
            "createdAt": datetime.now(timezone.utc).isoformat(),
        }
        session.status = "COMPLETED"
        return session.object_key


store = InMemoryFileStorage()
s = store.init_upload("u42", "notes.txt")
store.upload_part(s.upload_id, 1, b"hello ")
store.upload_part(s.upload_id, 2, b"world")
key = store.complete_upload(s.upload_id)
print(key, store.metadata[key]["size"])
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[uploadId -> UploadSession]` | resumable upload tracking |
| `dict[(uploadId, partNo) -> bytes/meta]` | multipart assembly |
| `dict[fileId -> metadata]` | metadata reads/listing |
| ACL map/set | permission checks |
| queue/stream | scan/index/lifecycle jobs |

## 4.4 Concurrency

High-signal concurrency issues:

- Parallel part uploads finishing out of order.
- Duplicate finalize requests.
- Metadata update while lifecycle job moves storage tier.
- Concurrent share revoke and download URL generation.

Handling strategy:

- Idempotent finalize endpoint.
- Optimistic locking/version in metadata rows.
- Atomic ACL updates with short cache TTL.
- Job idempotency keys for async workers.

## 4.5 Performance Optimization

Time complexity (conceptual):

- Metadata lookup near `O(1)` by key.
- Listing complexity depends on index/prefix design.
- Multipart assembly cost proportional to file size and part count.

Optimization rules:

- Use direct upload/download data path where possible.
- Use CDN for hot download traffic.
- Keep metadata payload small and indexed.

## 4.6 Error Handling

| Error | Response |
|---|---|
| invalid upload session | `404`/`410` |
| expired upload session | `409` with restart guidance |
| checksum mismatch | upload reject + cleanup |
| unauthorized access | `403` |
| object missing after metadata exists | mark inconsistent and repair workflow |

## 4.7 Testing Thinking

Unit tests:

- Multipart init/upload/finalize flow.
- Idempotent finalize behavior.
- ACL/share link validation logic.
- Soft delete and version retrieval.

Concurrency tests:

- Out-of-order part uploads.
- Parallel finalize requests.
- ACL update and download race.

Load tests:

- Large file upload storms.
- Hot object download spikes with cache misses.
- Lifecycle tiering backlog scenarios.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| upload storm | campaign users upload media simultaneously | session + origin write pressure |
| hot download storm | one viral file goes global | origin read saturation |
| cache miss wave | CDN purge or edge restart | sudden backend load |
| async backlog surge | scan/transcode queues spike | delayed file availability |
| abusive traffic | large bogus uploads | storage and bandwidth abuse |

## 5.2 Immediate Spike Response

1. Enforce upload quotas and adaptive rate limits.
2. Prioritize metadata and auth paths over non-critical batch jobs.
3. Extend CDN TTL and enable origin shielding for hot objects.
4. Autoscale upload/session and async worker fleets.
5. Apply backpressure on expensive scan/transcode pipelines.
6. Trigger abuse detection and block malicious clients.
7. Protect metadata DB with circuit breakers and cache fallback.

## 5.3 Hot Object Strategy

For viral downloads:

- Prewarm CDN edges for known hot objects.
- Use range caching for partial content clients.
- Coalesce simultaneous origin misses for same object.
- Serve stale-if-error when policy permits.

## 5.4 Degradation Policy

Protect in this order:

1. Metadata correctness and access control.
2. Core upload/download availability.
3. Durability guarantees.
4. Async enrichments (thumbnails/index freshness).
5. Secondary analytics and non-critical listing enrichments.

Allowed degradation:

- Delay thumbnails and search indexing.
- Slow folder aggregate stats updates.
- Temporarily reduce listing sort/filter richness.

Not allowed:

- Serving unauthorized data.
- Committing metadata without durable object availability.
- Silent data loss.

## 5.5 Spike Interview Answer

> During spikes I keep metadata + auth correctness first, shift heavy byte delivery to CDN, and degrade non-critical async enrichments before core upload/download paths. Hot-object controls and abuse throttles protect origin systems.

---

# 6. Scaling To A Billion Users

## 6.1 Global Architecture

For billion users:

```text
Global ingress and edge routing
  -> regional metadata + auth clusters
  -> regional object storage endpoints
  -> CDN global edge distribution
  -> async global pipelines for scan/index/lifecycle
```

## 6.2 Partitioning Strategy

- Partition metadata by tenant/user and file hash.
- Partition async jobs by object key hash and workload type.
- Keep object key design balanced for storage backend partitions.

## 6.3 Multi-Region Strategy

- Region-local uploads for lower latency.
- Cross-region replication policies by file class.
- Region failover for metadata and signed URL issuance.
- Compliance-aware data residency routing.

## 6.4 Storage Lifecycle at Scale

- Hot tier for recent/frequent files.
- Warm/cold archival for infrequent objects.
- Automated retention expiration and legal-hold exceptions.

## 6.5 Cost Controls

- Lifecycle tiering policies.
- Dedup/content-addressing where feasible and compliant.
- Cache hit optimization to reduce origin egress.
- Intelligent scan/index priorities.

## 6.6 Billion-User Capacity Plan

| Layer | Scaling plan |
|---|---|
| API/Gateway | stateless autoscale with rate limiting |
| Metadata DB | partitioned strong-indexed clusters |
| Object store | horizontally elastic, replicated durability |
| CDN | edge caching + origin shield |
| Async workers | lag-based autoscaling by workload type |
| Access service | low-latency token/ACL checks with cache |
| Observability | upload success, download p95, cache hit, async lag, integrity mismatch rate |

## 6.7 Billion-User Interview Answer

> At billion-user scale, file systems must separate metadata from object payload, push data transfer to object storage/CDN, and keep strict access control and durability semantics. Async pipelines handle scan/index/lifecycle while core upload/download stays reliable.

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
I start by clarifying file size profile, consistency requirements, and sharing/security model.
Then I estimate upload/download throughput and hot-object skew.
I design with metadata service, upload sessions, object storage, signed URLs, CDN, and async processing pipelines.
I keep metadata correctness strong and object bytes in storage/CDN path.
I enforce idempotent finalize flow and strict ACL checks.
For spikes, I prioritize auth/metadata, use CDN hot-object controls, and degrade async enrichments first.
At billion scale, I use partitioned metadata, region-aware storage, and lifecycle cost controls.
```

---

# 8. Fast Recall Rules

- Separate metadata and object payload planes.
- Use multipart resumable upload for large files.
- Keep upload finalize idempotent.
- Signed URLs should be short-lived and scoped.
- CDN is mandatory for global hot downloads.
- Never compromise ACL correctness for speed.
- Metadata commit must follow durable object commit.
- Async scan/index/lifecycle pipelines are first-class.
- Plan for hot-object skew and cache stampedes.
- Durability + integrity checks are non-negotiable.