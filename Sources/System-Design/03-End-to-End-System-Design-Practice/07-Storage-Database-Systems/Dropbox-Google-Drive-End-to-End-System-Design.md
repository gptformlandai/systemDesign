# Dropbox / Google Drive - End-to-End System Design

> Goal: practice one complete E2E cloud drive and file sync problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for file sync, collaboration, and cloud drive systems.
- Start broad with requirements and scale, then zoom into upload/download, metadata, sync engine, change feed, conflict resolution, sharding, replication, consistency, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Dropbox/Google Drive-style systems, optimize durable object storage, metadata correctness, efficient client sync, sharing permissions, version history, and offline conflict handling.

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

| Layer | Interview signal | Dropbox/Google Drive focus |
|---|---|---|
| Problem understanding | Can clarify product scope | upload, download, folders, sync, share, versioning, offline changes |
| HLD | Can design storage + sync systems | metadata service, block store, upload sessions, change log, sync service, sharing |
| LLD | Can model maintainable components | `FileNode`, `FileVersion`, `Block`, `SyncCursor`, `SharePolicy`, `ChangeEvent` |
| Machine coding | Can implement critical path | chunk upload, commit version, list changes, detect conflict |
| Traffic spikes | Can protect production | hot shared file, client reconnect storms, mass folder restore, upload bursts |
| Global scale | Can reason across regions | metadata sharding, object replication, change-log partitioning, client locality |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can upload files and folders.
- Users can download files.
- Desktop/mobile clients can sync local changes to cloud.
- Clients can fetch cloud changes since last sync cursor.
- Support file version history and restore.
- Support folders, move/rename/delete.
- Support sharing files/folders with permissions.
- Support resumable/chunked upload for large files.
- Support conflict handling when offline edits race.

Optional requirements to clarify:

- Is real-time collaborative editing in scope, or only file sync?
- Should we support block-level delta sync?
- Do we need global deduplication or per-user deduplication?
- Are comments, previews, and search in scope?
- How long should version history be retained?
- Are enterprise audit/compliance controls required?

Out of scope unless interviewer asks:

- Full Google Docs collaborative editing.
- Full malware/DLP scanning internals.
- Full search ranking and OCR pipeline.
- Full billing/subscription system.

## 1.2 Non-Functional Requirements

Durability:

- Extremely high durability for file contents.
- Metadata operations must be reliable and auditable.
- Version history should protect against accidental overwrite/delete.

Sync correctness:

- Clients should converge after offline edits.
- Change feed should be ordered per user/namespace.
- Conflict handling must be deterministic and user-visible.

Performance:

- Fast metadata/listing operations.
- Efficient large-file upload/download.
- Avoid re-uploading unchanged blocks when possible.

Security:

- Strong authn/authz.
- Encryption in transit and at rest.
- Permission checks on every file/folder access.

## 1.3 Constraints

- File bytes and metadata have different storage/query patterns.
- Clients can be offline for long periods.
- Multiple devices can edit the same file.
- Shared folders create multi-user namespace effects.
- Renames/moves are metadata operations, not object rewrites.
- Block dedup saves bandwidth/storage but complicates security/accounting.
- Change logs can grow very large.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 1 billion |
| DAU | 300 million |
| Files stored | trillions |
| Uploads/day | billions |
| Average file size | MB-scale with heavy skew |
| Large file size | GB-scale |
| Sync clients online | hundreds of millions |
| Metadata API target | p95 under 100-200 ms |
| Object durability | 11-nines-style target |

## 1.5 Capacity Math

Back-of-the-envelope:

- Billions of uploads/day mean object storage ingest is PB/day-scale at large companies.
- If a 100 MB file is chunked into 4 MB blocks, one file has 25 block records.
- Sync traffic can exceed upload traffic because many clients poll/subscribe for changes.
- Metadata rows can outnumber files because every file has versions, blocks, permissions, and change events.
- Hot shared files/folders create read amplification in metadata and download paths.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Block/chunk size | 4-16 MB |
| Sync page size | 100-1000 changes |
| Upload session TTL | hours to days |
| Version retention | days to indefinite by plan |
| Change log retention | enough for offline client recovery, then snapshot |
| Metadata replication | multi-AZ strongly consistent for writes |

## 1.6 Clarifying Questions To Ask

- Is this personal cloud drive, team drive, or enterprise file sync?
- Do we need block-level delta sync or whole-file upload?
- What consistency is expected after upload/rename/share?
- How should conflicts be represented to users?
- Do shared folder changes appear in each member's namespace?
- How long can clients be offline and still sync incrementally?

Strong interview framing:

> I will design Dropbox/Google Drive as separate metadata, block storage, and sync planes. File bytes are immutable blocks in object storage; metadata tracks file tree and versions; a per-namespace change log lets clients sync after a cursor; conflicts create new versions rather than silently overwriting data.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Upload/sync flow:
Client -> Sync API -> Upload Session Service
       -> block hash check
       -> signed block upload URLs
       -> Object/Block Store
       -> Commit Version API
       -> Metadata DB transaction
       -> Change Log event

Download/sync flow:
Client -> Sync API: list changes after cursor
       -> Metadata Service returns changed file versions
       -> Client downloads missing blocks through signed URLs/CDN
```

Recommended architecture:

```text
Clients
  |
  v
+-----------------------+
| API Gateway + Auth    |
+-----------+-----------+
            |
            +----------------------+----------------------+
            |                      |                      |
            v                      v                      v
+----------------+       +----------------+       +----------------+
| Sync Service   |       | Metadata Svc   |       | Upload Svc     |
| cursors/deltas |       | file tree/ver  |       | block sessions |
+-------+--------+       +-------+--------+       +-------+--------+
        |                        |                        |
        v                        v                        v
+----------------+       +----------------+       +----------------+
| Change Log     |       | Metadata DB    |       | Block/Object   |
| per namespace  |       | sharded        |       | Store + CDN    |
+----------------+       +-------+--------+       +----------------+
                                 |
                                 v
                        +----------------+
                        | Sharing/Authz  |
                        +----------------+
```

Request flow for file update:

1. Client detects local file change.
2. Client splits file into blocks and computes hashes.
3. Client asks Upload Service which blocks already exist.
4. Client uploads missing blocks using signed URLs.
5. Client commits a new file version with block list and parent revision.
6. Metadata Service checks permissions and expected parent revision.
7. If parent revision matches, commit new version.
8. If parent revision conflicts, create conflict copy/version.
9. Change Log emits `file.version.created`.
10. Other clients sync after cursor and download missing blocks.

## 2.2 APIs

### Start Upload Session

```http
POST /v1/upload-sessions
Authorization: Bearer <token>
```

```json
{
  "namespaceId": "ns-1",
  "path": "/docs/design.pdf",
  "fileSizeBytes": 104857600,
  "blockHashes": ["h1", "h2", "h3"]
}
```

### Commit File Version

```http
POST /v1/files/{fileId}/versions
Idempotency-Key: commit-abc
```

```json
{
  "namespaceId": "ns-1",
  "path": "/docs/design.pdf",
  "baseRevision": 41,
  "blocks": [
    {"blockHash": "h1", "sizeBytes": 4194304},
    {"blockHash": "h2", "sizeBytes": 4194304}
  ]
}
```

### List Changes

```http
GET /v1/namespaces/{namespaceId}/changes?cursor=chg-123&limit=500
Authorization: Bearer <token>
```

### Get Download URLs

```http
POST /v1/files/{fileId}/download-urls
```

```json
{
  "versionId": "ver-9",
  "blockHashes": ["h1", "h2"]
}
```

### Share Folder

```http
POST /v1/folders/{folderId}/shares
```

```json
{
  "principal": "user@example.com",
  "role": "EDITOR"
}
```

Important API points:

- Upload bytes should not flow through metadata services.
- Commit version is the atomic metadata boundary.
- List changes uses durable cursors.
- Sharing and download URLs require authorization checks.

## 2.3 Core Components

Think of Dropbox/Drive as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Metadata plane | files, folders, versions, namespace tree | correctness and fast listing |
| Block plane | immutable chunks, hashes, object storage | durable efficient file bytes |
| Sync plane | change feed, cursors, conflict detection | client convergence |
| Sharing plane | ACLs, links, team folders | permission correctness |
| Async plane | previews, search, scanning, lifecycle | enrich without blocking writes |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Upload Service | sessions, block existence, signed URLs | file tree commits | upload QPS |
| Block Store | immutable blocks and object paths | user permissions | bytes and requests |
| Metadata Service | file tree, versions, moves, deletes | raw upload bytes | metadata QPS |
| Sync Service | change cursors and delta API | storage durability | online clients |
| Change Log | ordered namespace events | permission decisions | events/namespace |
| Sharing Service | ACLs and shared links | block storage | authz checks |
| Search/Preview Workers | async indexes/previews | commit path truth | backlog |
| Lifecycle Service | retention, compaction, GC | sync cursor correctness | storage age |

### Metadata Service

Why it exists:

- File tree operations need transactional correctness.
- Rename/move/share/delete are metadata changes.

Core responsibilities:

- Store file/folder nodes.
- Store file versions and current head.
- Enforce namespace/path uniqueness.
- Validate base revision for conflict detection.
- Emit ordered change events.
- Support listing and path lookup.

Failure behavior:

- If metadata commit fails, uploaded blocks remain unreferenced and GC later cleans them.
- If change log publish fails after metadata commit, outbox republishes.

Interview signal:

> The version commit is the source of truth; uploaded blocks are not visible until metadata commit succeeds.

### Block Store And Deduplication

Core idea:

- Files are split into immutable blocks.
- Blocks are addressed by content hash.
- File version points to an ordered list of block hashes.

Benefits:

- Resumable upload.
- Reuse unchanged blocks.
- Efficient sync across devices.
- Potential dedup.

Trade-offs:

- More metadata rows.
- Hash verification needed.
- Cross-user dedup can leak information if not designed carefully.
- Garbage collection must know which versions reference blocks.

### Sync Service And Change Feed

Why it exists:

- Clients need to catch up after offline periods.
- Polling the entire file tree is too expensive.

Core responsibilities:

- Maintain per-namespace ordered change events.
- Return changes after cursor.
- Support cursor expiry and full resync fallback.
- Include file create/update/delete/move/share events.
- Let clients hydrate missing metadata and blocks.

Consistency model:

- Read-your-writes for user's committed metadata.
- Eventual propagation to other devices.
- Ordered changes within a namespace.

### Conflict Resolution

Common conflict:

- Device A and Device B both edit version 41 offline.
- Device A commits version 42.
- Device B tries to commit with base revision 41.

Handling:

- Metadata Service detects base revision mismatch.
- Create conflict copy or branch version.
- Notify clients.
- Never silently overwrite the winner.

Interview signal:

> The system should preserve both versions and make conflict visible rather than choosing data loss.

### Sharing And Permissions

Types:

- Owner.
- Viewer/editor ACL.
- Shared link.
- Team/shared folder.
- Expiring link.

Important rules:

- Every metadata read/write checks authorization.
- Download URL issuance checks authorization.
- Shared folder changes appear in member sync feeds.
- Permission changes should invalidate cached access.

## 2.4 Data Layer

### Core Data Models

File node:

```json
{
  "fileId": "file-1",
  "namespaceId": "ns-1",
  "parentFolderId": "folder-1",
  "name": "design.pdf",
  "type": "FILE",
  "currentVersionId": "ver-9",
  "state": "ACTIVE"
}
```

File version:

```json
{
  "versionId": "ver-9",
  "fileId": "file-1",
  "revision": 42,
  "sizeBytes": 104857600,
  "blocks": ["h1", "h2", "h3"],
  "createdBy": "u-1",
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Change event:

```json
{
  "namespaceId": "ns-1",
  "changeSequence": 88001,
  "eventType": "FILE_VERSION_CREATED",
  "fileId": "file-1",
  "versionId": "ver-9"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| File/folder metadata | relational/distributed SQL | transactions and path uniqueness |
| File versions | wide-column/relational | append-heavy version history |
| Blocks | object storage | immutable durable blobs |
| Block refs | metadata DB/wide-column | GC and dedup accounting |
| Change log | ordered stream/table | sync cursor |
| ACLs/share links | relational/KV | authorization correctness |
| Search index | search engine | async discovery |

Relational-style tables:

```sql
file_nodes(namespace_id, file_id, parent_id, name, type, current_version_id, state)
file_versions(file_id, version_id, revision, size_bytes, created_by, created_at)
version_blocks(version_id, block_index, block_hash, size_bytes)
namespace_changes(namespace_id, change_sequence, event_type, file_id, version_id, created_at)
acl_entries(resource_id, principal_id, role, state)
```

Important indexes:

- `file_nodes(namespace_id, parent_id, name)` for path lookup/listing.
- `file_versions(file_id, revision DESC)` for history.
- `namespace_changes(namespace_id, change_sequence)` for sync.
- `version_blocks(block_hash)` for ref counting/GC.

### Partitioning

- Partition metadata by `namespaceId`.
- Partition change log by `namespaceId`.
- Partition blocks by hash prefix/object key.
- Large enterprise namespaces may require folder/subtree partitioning.
- Hot shared folders may need dedicated metadata shards.

### Replication And Consistency

- Metadata writes need strong consistency within a namespace/subtree.
- Object blocks need high durability, often multi-AZ and cross-region replication.
- Change log must match committed metadata through outbox/transactional publish.
- Search/previews can be eventually consistent.

## 2.5 Scalability

### Horizontal Scaling

- Upload Service scales by upload session QPS.
- Block Store scales by object requests and bytes.
- Metadata Service scales by namespace partitions.
- Sync Service scales by online client count and change reads.
- Search/preview workers scale by backlog.

### Hot Shared Folder Strategy

- Cache read-only metadata carefully.
- Use namespace/folder partitioning for large teams.
- Batch sync notifications.
- Rate-limit runaway clients.
- Use backpressure on mass restore/move operations.

## 2.6 Performance

### Caching Strategy

| Cache | Stores | TTL |
|---|---|---:|
| metadata cache | file/folder nodes | short |
| block existence cache | block hash present? | minutes |
| share permission cache | ACL decisions | short with invalidation |
| CDN | hot downloads/previews | minutes to days |
| client cache | local files/metadata | durable local state |

### Latency Budget Example

| Stage | Target |
|---|---:|
| Metadata lookup/list | 20-100 ms |
| Start upload session | 50-200 ms |
| Commit version | 50-300 ms |
| List changes | 50-300 ms |
| Download signed URLs | 50-200 ms |

### Optimization Rules

- Upload only missing blocks.
- Keep metadata commit small.
- Use cursor-based sync, not full tree scans.
- Cache static file previews/downloads at CDN.
- Use batching for client sync.

## 2.7 Async Systems

Use streams for:

- file version created
- file moved/renamed/deleted
- folder shared/unshared
- block uploaded
- preview generation requested
- search indexing requested
- malware/DLP scan requested
- lifecycle/GC requested

Queue notes:

- Consumers are idempotent.
- Search and previews can lag.
- GC must not delete referenced blocks.
- Sync change log should be tied to metadata commit.

## 2.8 Security, Privacy, And Compliance

Security:

- Authenticated APIs.
- Authorization on metadata, share, and download URL issuance.
- Encryption in transit and at rest.
- Optional customer-managed keys for enterprise.
- Signed URLs with short expiration.

Privacy/compliance:

- File contents and names are sensitive.
- Audit sharing and admin actions.
- Retention/legal hold can prevent hard delete.
- GDPR-style delete/export if required.

Abuse controls:

- Rate-limit sync clients.
- Detect malware/spam sharing links.
- Prevent public link abuse.
- Quota enforcement for storage and bandwidth.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Metadata | commit latency, conflict rate, transaction failures |
| Sync | cursor lag, full-resync rate, client error rate |
| Blocks | upload success, download latency, orphan block growth |
| Sharing | authz latency, permission denied anomalies |
| Storage | replication lag, durability errors, GC backlog |
| Async | preview/search lag, scan backlog |

Alerts:

- Metadata commit failures rise.
- Change log lag grows.
- Object store errors spike.
- Orphan block growth exceeds threshold.
- Permission cache invalidation failures occur.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Sync granularity | whole file | block-level | simplicity vs bandwidth efficiency |
| Metadata DB | strongly consistent | eventually consistent | correctness vs write scalability |
| Conflict handling | overwrite latest | preserve conflict copy | simplicity vs data safety |
| Dedup | global | per-user/per-namespace | storage savings vs privacy risk |
| Change feed | per namespace | global stream | easy sync vs global ordering simplicity |
| Deletes | soft delete/versioned | hard delete | recovery/compliance vs storage cost |

Interview framing:

> I would store immutable file blocks separately from strongly consistent metadata. Clients sync using a namespace change log, commits use base revisions for conflict detection, and conflict copies preserve data instead of overwriting.

---

# 3. Low-Level Design

LLD goal:

> Model Dropbox/Drive around file nodes, versions, immutable blocks, namespace changes, sync cursors, ACLs, and conflict-safe commits.

Simple rules:

- Uploaded blocks are not visible until metadata commit.
- File version points to ordered immutable blocks.
- Client cursor advances through namespace change log.
- Conflicts create versions/copies, not silent overwrites.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Namespace` | user's/team's file tree | change sequence is ordered |
| `FileNode` | file/folder identity and parent | path unique within parent |
| `FileVersion` | revision and block list | immutable after commit |
| `Block` | hash, size, object key | immutable content |
| `SyncCursor` | last seen change sequence | moves forward only |
| `SharePolicy` | ACL/link permissions | checked on every access |
| `ChangeEvent` | ordered namespace mutation | replayable for sync |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `UploadService` | block upload sessions | mutate file tree directly |
| `MetadataService` | file tree and versions | store raw bytes |
| `SyncService` | change cursor reads | decide ACLs alone |
| `SharingService` | ACL/link checks | generate file versions |
| `BlockGcService` | remove unreferenced blocks | delete live versions |

## 3.2 OOP Fundamentals

Encapsulation:

- `FileVersion` owns block order.
- `SyncCursor` owns monotonic cursor advancement.
- `SharePolicy` owns access decisions.

Abstraction:

- `BlockStore` hides object storage.
- `MetadataRepository` hides DB partitioning.
- `ChangeLog` hides stream/table implementation.

Polymorphism:

- Different conflict strategies: conflict copy, branch, reject.
- Different storage tiers: hot, warm, cold/archive.

Composition:

- `CommitService` composes block verifier, metadata repository, ACL service, change log, and event publisher.

## 3.3 SOLID Principles

| Principle | Dropbox/Drive application |
|---|---|
| Single Responsibility | `SyncService` only serves deltas/cursors |
| Open/Closed | add storage tier without rewriting metadata commit |
| Liskov Substitution | any `BlockStore` preserves put/get/hash contract |
| Interface Segregation | separate upload, metadata, sync, sharing APIs |
| Dependency Inversion | core services depend on repository/storage interfaces |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Command | commit/move/delete/share operations | idempotency and audit |
| Observer/Event Publisher | metadata changes to async workers | search/preview/scan |
| Strategy | conflict handling and tiering | choose behavior by product/policy |
| State | upload session lifecycle | active, committed, expired |
| Decorator | authz/audit/rate-limit wrappers | cross-cutting controls |

## 3.5 UML / Diagrams

### Commit Version Sequence

```text
Client -> UploadService: upload missing blocks
Client -> MetadataService: commitVersion(baseRevision, blocks)
MetadataService -> SharingService: canWrite
MetadataService -> BlockStore: verify blocks exist
MetadataService -> MetadataDB: transaction create version/update head
MetadataService -> ChangeLog: append namespace change
MetadataService -> Client: committed version or conflict
```

### Sync Sequence

```text
Client -> SyncService: listChanges(cursor)
SyncService -> ChangeLog: read after sequence
SyncService -> MetadataService: hydrate changed nodes
SyncService -> Client: changes + next cursor
Client -> BlockStore/CDN: download missing blocks
```

## 3.6 Class Design

Interfaces:

```java
interface BlockStore {
    boolean exists(String blockHash);
    SignedUrl signedUploadUrl(String blockHash);
    SignedUrl signedDownloadUrl(String blockHash);
}

interface MetadataRepository {
    CommitResult commitVersion(CommitCommand command);
    List<FileNode> listFolder(String namespaceId, String folderId, Cursor cursor);
}

interface ChangeLog {
    long append(String namespaceId, ChangeEvent event);
    List<ChangeEvent> readAfter(String namespaceId, long sequence, int limit);
}

interface AccessControl {
    boolean canRead(String userId, String resourceId);
    boolean canWrite(String userId, String resourceId);
}
```

Design notes:

- `commitVersion()` should be transactional with the change event/outbox.
- Block verification prevents committing references to missing blocks.
- Sync reads must be authorization-aware for shared namespaces.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| blocks uploaded but commit fails | orphan block GC later |
| two offline edits commit same base revision | first wins, second creates conflict copy |
| client cursor too old | full resync or snapshot-based catch-up |
| share revoked while URL cached | short URL TTL and permission recheck |
| move folder with many children | metadata operation with subtree semantics/events |
| file deleted then edited by offline client | create conflict/restore flow |
| block GC races with old version | ref-count/mark-sweep with retention guard |
| metadata commit succeeds but event publish fails | outbox republish |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
drive/
  domain/
    FileNode.java
    FileVersion.java
    Block.java
    ChangeEvent.java
    SyncCursor.java
  service/
    UploadService.java
    MetadataService.java
    SyncService.java
    SharingService.java
  port/
    BlockStore.java
    MetadataRepository.java
    ChangeLog.java
    AccessControl.java
  adapter/
    InMemoryBlockStore.java
    InMemoryMetadataRepository.java
  app/
    DriveDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from typing import Dict, List


@dataclass(frozen=True)
class FileVersion:
    version_id: str
    revision: int
    blocks: List[str]


@dataclass
class FileNode:
    file_id: str
    name: str
    current_revision: int
    versions: List[FileVersion]


class InMemoryDrive:
    def __init__(self) -> None:
        self.blocks: set[str] = set()
        self.files: Dict[str, FileNode] = {}
        self.changes: List[dict] = []

    def upload_block(self, block_hash: str) -> None:
        self.blocks.add(block_hash)

    def create_file(self, file_id: str, name: str) -> None:
        self.files[file_id] = FileNode(file_id, name, 0, [])

    def commit_version(self, file_id: str, base_revision: int, blocks: List[str]) -> FileVersion:
        missing = [b for b in blocks if b not in self.blocks]
        if missing:
            raise ValueError(f"missing blocks: {missing}")
        node = self.files[file_id]
        if base_revision != node.current_revision:
            conflict_id = f"{file_id}-conflict-{len(node.versions) + 1}"
            self.create_file(conflict_id, node.name + " (conflict)")
            return self.commit_version(conflict_id, 0, blocks)
        next_revision = node.current_revision + 1
        version = FileVersion(f"{file_id}-v{next_revision}", next_revision, blocks)
        node.versions.append(version)
        node.current_revision = next_revision
        self.changes.append({"fileId": file_id, "revision": next_revision})
        return version

    def list_changes(self, after_index: int) -> tuple[list[dict], int]:
        return self.changes[after_index:], len(self.changes)


drive = InMemoryDrive()
drive.create_file("f1", "notes.txt")
drive.upload_block("h1")
v1 = drive.commit_version("f1", 0, ["h1"])
print(v1.revision)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[fileId -> FileNode]` | metadata |
| `dict[versionId -> list[blockHash]]` | version block list |
| `set[blockHash]` | uploaded blocks |
| `list[ChangeEvent]` | sync change log |
| `dict[resourceId -> ACL]` | sharing |

## 4.4 Concurrency

High-signal concurrency issues:

- Two devices commit same file revision.
- Share revoked while client syncs.
- Move/delete races with file update.
- Block GC races with version restore.

Handling strategy:

- Compare base revision on commit.
- Transactional metadata update and change event.
- Permission check at commit/read/download URL time.
- GC uses ref-count/mark-sweep and retention windows.

## 4.5 Testing Thinking

Unit tests:

- Missing block cannot be committed.
- Version commit advances revision.
- Stale base revision creates conflict.
- List changes returns events after cursor.
- Revoked user cannot download.

Load tests:

- Many clients syncing same shared folder.
- Mass upload burst.
- Large folder rename/restore.
- Change log catch-up after offline period.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Client reconnect storm | outage recovery | sync API/change log overload |
| Hot shared file | viral public link | CDN/object read spike |
| Mass folder restore | ransomware recovery | metadata/change-event flood |
| Upload burst | backup client rollout | object ingest pressure |
| Permission change storm | enterprise sharing update | cache invalidation pressure |

## 5.2 Immediate Spike Response

1. Protect metadata commits and change log.
2. Rate-limit runaway clients.
3. Serve downloads through CDN and signed URLs.
4. Batch sync changes and notifications.
5. Queue large restore/move jobs.
6. Degrade search/preview freshness before sync correctness.
7. Prioritize authz correctness.

## 5.3 Degradation Policy

Protect in this order:

1. Metadata correctness and authz.
2. Block durability.
3. Sync cursor/change feed.
4. Upload/download availability.
5. Search/previews.
6. Notifications.

Not allowed:

- Lose committed versions.
- Grant access after permission revocation beyond token TTL/policy.
- Corrupt namespace ordering.
- Delete referenced blocks.

## 5.4 Spike Interview Answer

> During spikes I protect metadata commits, authorization, and change-log correctness. Downloads can lean on CDN, previews/search can lag, and heavy namespace operations can become queued jobs. Sync must remain replayable and conflict-safe.

---

# 6. Scaling To Global Users

## 6.1 Global Architecture

```text
Global routing
  -> regional upload/download edges
  -> metadata owner shard by namespace
  -> durable object/block storage
  -> namespace change logs
  -> async search/preview/scan/lifecycle pipelines
```

## 6.2 Multi-Region Strategy

- Route object upload/download to nearest region.
- Keep metadata writes for a namespace in owner region/shard.
- Replicate blocks cross-region for durability/read locality.
- Replicate metadata for disaster recovery and read replicas.
- Use client sync to repair after failover.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Upload | signed direct block uploads |
| Block store | hash-partitioned object storage |
| Metadata | namespace-sharded transactional store |
| Change log | namespace-partitioned ordered log |
| Sync | cursor reads, batching, backoff |
| Sharing | ACL cache with invalidation |
| Search/preview | async workers and indexes |
| Lifecycle | retention, tiering, GC |

## 6.4 Global Interview Answer

> I would scale Dropbox/Drive by separating immutable block storage from transactional metadata. Namespaces are sharded, change logs power sync, blocks are replicated and CDN-served, and clients converge through cursors and conflict-safe version commits.

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
I will clarify file sync vs simple storage, block-level sync, sharing, versioning, conflicts, retention, and offline duration.
I will estimate upload bytes/day, metadata writes, block count, sync QPS, change-log growth, and hot shared-file traffic.
HLD includes Upload Service, Block Store, Metadata Service, Sync Service, Change Log, Sharing, CDN, and async scan/search/preview workers.
I keep blocks immutable and metadata transactional.
Clients commit using base revision and sync using namespace cursor.
Conflict handling preserves data with conflict copies/versions.
For global scale, I shard by namespace and replicate blocks widely.
```

---

# 8. Fast Recall Rules

- Dropbox/Drive is file storage plus sync.
- Blocks are immutable; metadata points to blocks.
- Uploaded blocks are invisible until version commit.
- Namespace change log powers sync.
- Base revision detects conflicts.
- Preserve both versions on conflict.
- Metadata needs strong consistency per namespace/subtree.
- Search/previews/scans are async.
- Shared folder permissions affect sync and download URLs.
- GC must never delete referenced blocks.
