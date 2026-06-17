# Log Storage System - End-to-End System Design

> Goal: practice one complete E2E log storage and query problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for observability, logging, and append-heavy storage systems.
- Start broad with requirements and scale, then zoom into ingestion, buffering, partitioning, segment storage, indexing, replication, query, retention, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For log storage systems, optimize high-throughput writes, durable append, time-based partitioning, efficient search, retention cost, tenant isolation, and graceful degradation under spikes.

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

| Layer | Interview signal | Log storage focus |
|---|---|---|
| Problem understanding | Can clarify log use case | ingest logs, query by time/service/level/text, retention, alerts |
| HLD | Can design append-heavy infra | collectors, ingestion gateway, stream buffer, segment writer, indexer, query service |
| LLD | Can model maintainable components | `LogRecord`, `LogStream`, `Segment`, `IndexBlock`, `RetentionPolicy`, `QueryPlan` |
| Machine coding | Can implement critical path | append logs, rotate segment, query by time, apply retention |
| Traffic spikes | Can protect production | incident log storm, noisy tenant, query bursts, index backlog |
| Global scale | Can reason across regions | tenant partitioning, replication, tiered storage, federated query |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Applications/agents can ingest log events.
- Logs have timestamp, tenant, service, host, severity, message, and labels.
- Users can query logs by time range and filters.
- Support full-text or substring search if in scope.
- Support retention policies by tenant/service.
- Support high-throughput append writes.
- Support dashboards/alerts consuming logs or derived metrics.
- Support export/archive if required.

Optional requirements to clarify:

- Is this like CloudWatch Logs, Loki, Elasticsearch, or Kafka-backed log lake?
- Do we need full-text search or label/time search only?
- What retention duration and query latency are expected?
- Are logs immutable?
- Do we support multi-tenant isolation and quotas?
- Do alerts need real-time stream processing?

Out of scope unless interviewer asks:

- Full metrics/tracing platform.
- Full SIEM/security analytics.
- Full query language implementation.
- Full agent deployment/management.

## 1.2 Non-Functional Requirements

Ingestion:

- Very high write throughput.
- Backpressure under overload.
- At-least-once ingestion with dedup where needed.
- Durable buffering before storage.

Query:

- Time-range queries should be efficient.
- Common filters should avoid scanning all data.
- Query fanout must be bounded per tenant.

Storage:

- Cheap long-term retention.
- Compression and tiering.
- Replication for durability.
- Clear consistency expectations for newly ingested logs.

## 1.3 Constraints

- Logs are append-heavy and time-ordered.
- Log volume can spike during incidents.
- Full-text indexing every log is expensive.
- High-cardinality labels can explode indexes.
- Tenants can be noisy neighbors.
- Querying cold logs is slower and more expensive.
- Late/out-of-order logs are common.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Tenants | millions |
| Ingest volume | PB/day at large scale |
| Events/sec | millions to tens of millions |
| Avg log size | 200 bytes-2 KB |
| Retention | 7-365 days by plan |
| Query latency hot data | p95 under 1-5 seconds |
| Replication factor | 2-3 |
| Availability target | 99.9%-99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- `10M events/sec * 500 bytes` is `5 GB/sec`, about `432 TB/day` before replication and indexes.
- Replication factor 3 turns `432 TB/day` into `1.3 PB/day` physical writes.
- Indexes can be 10%-100%+ of raw size depending full-text/labels.
- Compression can reduce log storage significantly, often 3x-10x depending data.
- Query cost is dominated by bytes scanned and partitions touched.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Segment size | 128 MB-1 GB compressed |
| Ingest buffer retention | hours to days |
| Hot storage retention | hours to days |
| Warm/cold retention | days to years |
| Query page size | thousands of log lines |
| Index granularity | segment/block level |

## 1.6 Clarifying Questions To Ask

- Do users need full-text search or mostly labels/time filters?
- How fresh must logs be after ingestion?
- What retention and compliance requirements apply?
- Are logs tenant-isolated?
- Should ingestion drop logs under overload or block clients?
- Are alerts derived from raw logs in real time?

Strong interview framing:

> I will design log storage as an append-heavy pipeline: agents send logs to ingestion gateways, logs are buffered in a durable stream, segment writers create compressed immutable files, indexers build time/label/text indexes, and query service fans out only to relevant segments. Retention and tiering control cost.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Ingestion flow:
Agent -> Ingestion Gateway
      -> auth/quota/validation
      -> durable stream buffer
      -> segment writers
      -> hot object storage / local segment store
      -> index builders
      -> catalog metadata

Query flow:
User -> Query API
     -> auth/tenant limits
     -> query planner reads catalog/index
     -> fetch relevant segments
     -> filter/decompress/merge/sort
     -> paginated response
```

Recommended architecture:

```text
Apps/Agents
  |
  v
+-----------------------+
| Ingestion Gateway     |
| auth/quota/batching   |
+-----------+-----------+
            |
            v
+-----------------------+
| Durable Stream Buffer |
| partitioned           |
+-----------+-----------+
            |
            +----------------------+----------------------+
            |                      |                      |
            v                      v                      v
+----------------+       +----------------+       +----------------+
| Segment Writer |       | Index Builder  |       | Alert Pipeline |
| compress/store |       | labels/text    |       | stream rules   |
+-------+--------+       +-------+--------+       +----------------+
        |                        |
        v                        v
+----------------+       +----------------+
| Object Storage |       | Index Store    |
| hot/warm/cold  |       +-------+--------+
+-------+--------+               |
        |                        v
        +--------------->+----------------+
                         | Catalog Store  |
                         +-------+--------+
                                 |
                                 v
                         +----------------+
                         | Query Service  |
                         +----------------+
```

Request flow for ingestion:

1. Agent batches log events.
2. Ingestion Gateway authenticates tenant/API key.
3. Gateway validates schema, timestamp bounds, and size limits.
4. Gateway applies tenant quota/backpressure.
5. Events are appended to durable stream partitions.
6. Segment writers consume stream and write compressed immutable segments.
7. Catalog records segment time range, tenant, stream, labels, object path.
8. Index builders create label/text indexes.
9. Query Service can discover and scan segments.

## 2.2 APIs

### Ingest Logs

```http
POST /v1/logs/ingest
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "tenantId": "t-1",
  "stream": "payments-api",
  "records": [
    {
      "timestamp": "2026-06-17T12:00:00Z",
      "level": "ERROR",
      "message": "payment provider timeout",
      "labels": {"service": "payments", "host": "pod-1"}
    }
  ]
}
```

### Query Logs

```http
POST /v1/logs/query
Authorization: Bearer <token>
```

```json
{
  "tenantId": "t-1",
  "start": "2026-06-17T11:00:00Z",
  "end": "2026-06-17T12:00:00Z",
  "filter": "service=payments level=ERROR",
  "contains": "timeout",
  "limit": 1000
}
```

### Tail Logs

```http
GET /v1/logs/tail?tenantId=t-1&stream=payments-api
Authorization: Bearer <token>
```

Important API points:

- Ingest API should support batching and compression.
- Query API must enforce tenant limits.
- Tail can read from stream buffer/hot segments.
- Timestamps can be late/out of order; ingestion time and event time both matter.

## 2.3 Core Components

Think of log storage as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Ingestion plane | agents, gateways, validation, quotas | accept high-volume logs safely |
| Buffer plane | durable stream, partition ordering | absorb spikes and replay |
| Storage plane | compressed immutable segments | cheap durable retention |
| Index plane | time/label/text indexes | reduce query scan |
| Query plane | planning, fanout, filtering, merge | efficient retrieval |
| Lifecycle plane | retention, compaction, tiering | cost control |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Agent/Collector | batching and retry | storage indexing | app log volume |
| Ingestion Gateway | auth, quotas, validation | long-term storage | ingest QPS |
| Stream Buffer | durable ordered events | query planning | partitions/bytes |
| Segment Writer | compression and segment files | user query auth | write throughput |
| Catalog Store | segment metadata | raw log line truth | segment count |
| Index Builder | label/text index | ingestion auth | index backlog |
| Query Planner | choose segments/indexes | ingest writes | query QPS |
| Retention Manager | delete/tier old data | live ingestion | data age/volume |

### Ingestion Gateway

Why it exists:

- Clients are untrusted/noisy.
- Logs arrive in bursts.
- The storage backend needs normalized batches.

Core responsibilities:

- Authenticate tenant.
- Validate event size/schema/timestamp.
- Apply tenant quotas and rate limits.
- Compress/decompress batches.
- Append to stream buffer.
- Return ack after durable buffer append.

Failure behavior:

- If stream unavailable, reject or apply backpressure.
- If tenant exceeds quota, throttle/drop according to policy.
- Client retries may create duplicates; use event ID if dedup required.

Interview signal:

> Acknowledging at durable buffer append gives ingestion durability before expensive indexing/storage finishes.

### Segment Writer And Storage Format

Core idea:

- Consume logs from stream.
- Group by tenant/stream/time partition.
- Sort or bucket by timestamp if needed.
- Compress into immutable segment files.
- Write segment metadata to catalog.

Segment metadata:

- Tenant ID.
- Stream/service.
- Time range min/max.
- Label summaries.
- Object path.
- Compression codec.
- Record count and size.

Storage format:

- Row-oriented for raw log retrieval.
- Blocked compressed format for skipping.
- Optional columnar extraction for common fields.

### Indexing

Index types:

| Index | Purpose | Cost |
|---|---|---|
| Time index | segment time pruning | low |
| Label index | service/level/env filters | medium |
| Bloom filter | maybe contains term/key | low/medium |
| Inverted index | full-text search | high |
| Trigram/ngram | substring search | high |

Design choice:

- If product is Loki-like, index labels and scan compressed chunks.
- If product is Elasticsearch-like, build richer inverted indexes.

Interview signal:

> Index only what queries need; full-text indexing everything is powerful but expensive.

### Query Service

Query steps:

1. Authenticate and authorize tenant.
2. Parse query/time range.
3. Use catalog and indexes to identify candidate segments.
4. Enforce query budget and max fanout.
5. Fetch segments from hot/warm/cold storage.
6. Decompress blocks and apply filters.
7. Merge results by timestamp.
8. Return page/cursor.

Failure behavior:

- Partial results may be returned with warnings if product allows.
- Cold storage queries can be slower.
- Expensive queries can be rejected or queued.

### Retention And Tiering

Why it exists:

- Logs grow without bound.
- Most logs are rarely queried after a short window.

Tiers:

| Tier | Stores | Query speed |
|---|---|---|
| Hot | recent segments/indexes on SSD/object hot tier | fast |
| Warm | compressed object storage | moderate |
| Cold | archive storage | slow |

Retention:

- Per tenant/service policy.
- Delete indexes and data after retention.
- Legal hold can override deletion.

## 2.4 Data Layer

### Core Data Models

Log record:

```json
{
  "tenantId": "t-1",
  "stream": "payments-api",
  "timestamp": "2026-06-17T12:00:00Z",
  "ingestedAt": "2026-06-17T12:00:01Z",
  "level": "ERROR",
  "message": "payment provider timeout",
  "labels": {"service": "payments", "host": "pod-1"}
}
```

Segment:

```json
{
  "segmentId": "seg-1",
  "tenantId": "t-1",
  "stream": "payments-api",
  "minTimestamp": "2026-06-17T12:00:00Z",
  "maxTimestamp": "2026-06-17T12:05:00Z",
  "objectPath": "logs/t-1/payments/seg-1.zst",
  "recordCount": 100000,
  "compressedBytes": 52428800
}
```

Index block:

```json
{
  "segmentId": "seg-1",
  "indexType": "LABEL",
  "key": "level",
  "value": "ERROR",
  "blockOffsets": [0, 4, 8]
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Ingest buffer | Kafka/Pulsar-style stream | durable replay/backpressure |
| Segments | object storage / distributed file store | cheap immutable data |
| Catalog | relational/distributed metadata DB | segment discovery |
| Label index | KV/search index | query pruning |
| Full-text index | search engine/inverted index | text search |
| Hot cache | SSD/cache | recent query speed |
| Query results | cache | repeated dashboard queries |

Relational-style catalog tables:

```sql
segments(tenant_id, stream, segment_id, min_ts, max_ts, object_path, bytes, record_count)
segment_labels(tenant_id, label_key, label_value, segment_id, min_ts, max_ts)
retention_policies(tenant_id, stream, retention_days, tier_policy)
ingest_offsets(partition_id, offset, segment_id)
```

Important indexes:

- `segments(tenant_id, stream, min_ts, max_ts)` for time pruning.
- `segment_labels(tenant_id, label_key, label_value, min_ts)` for label filters.
- `retention_policies(tenant_id, stream)` for lifecycle.

### Partitioning

- Partition ingest stream by tenant/stream/hash.
- Partition segment files by tenant/stream/time.
- Partition catalog by tenant and time bucket.
- Partition indexes by tenant/label/time.
- Isolate noisy tenants into dedicated partitions.

### Replication And Consistency

- Durable stream replication protects recent ingest.
- Segment storage replication protects long-term logs.
- Catalog update should happen after segment write succeeds.
- Newly ingested logs may be queryable from stream before segment/index is ready.
- Query freshness can be eventual, with tail path for recent logs.

## 2.5 Scalability

### Horizontal Scaling

- Add ingestion gateways for write QPS.
- Add stream partitions for throughput.
- Add segment writers for backlog.
- Add index builders for index lag.
- Add query workers for fanout.
- Partition by tenant/time to isolate load.

### Query Scaling

- Plan queries using time/label indexes.
- Push filters down to segment scan.
- Cache common dashboard queries.
- Limit query time range/fanout.
- Queue expensive cold queries.

### Noisy Tenant Strategy

- Per-tenant ingest quotas.
- Per-tenant query budgets.
- Dedicated partitions for large tenants.
- Drop/debug sampling policies where allowed.
- Cardinality limits on labels.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Ingest gateway validation | 1-20 ms |
| Durable stream append | 5-100 ms |
| Segment availability | seconds to minutes |
| Hot query | 1-5 seconds |
| Cold query | seconds to minutes |
| Tail logs | sub-second to few seconds |

### Optimization Rules

- Batch and compress ingestion.
- Use time partitioning everywhere.
- Keep high-cardinality labels out of global indexes.
- Use Bloom filters to skip segments.
- Store hot recent logs separately for fast tail/query.
- Compact small segments.

## 2.7 Async Systems

Use streams/jobs for:

- segment writing
- index building
- alert evaluation
- retention deletion
- tier migration
- compaction
- query cache warming
- dead-letter malformed events

Queue notes:

- Ingest buffer absorbs spikes.
- Segment/index consumers must be idempotent.
- Catalog writes should be idempotent by segment ID.
- Retention must not delete under legal hold.

## 2.8 Security, Privacy, And Compliance

Security:

- Tenant API keys/tokens.
- TLS ingestion/query.
- Encryption at rest for segments/indexes.
- Access controls by tenant/project/service.

Privacy/compliance:

- Logs may contain PII/secrets.
- Support redaction/scrubbing at ingestion if required.
- Retention policy enforcement.
- Audit query access.
- Legal hold support.

Abuse controls:

- Limit event size.
- Limit label cardinality.
- Per-tenant ingest and query quotas.
- Prevent wildcard queries from scanning unlimited data.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Ingest | accepted events/sec, rejected events, ingest latency |
| Buffer | stream lag, partition skew, replay backlog |
| Storage | segment write failures, compression ratio, bytes/day |
| Index | indexing lag, index size, high-cardinality labels |
| Query | query latency, bytes scanned, segments touched |
| Retention | deletion lag, tiering lag, legal hold skips |

Alerts:

- Ingest rejection spikes.
- Stream lag grows.
- Segment writer failures rise.
- Query bytes scanned explode.
- Storage growth exceeds forecast.
- Retention deletion fails.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Search | full-text index | label/time scan | query power vs index cost |
| Storage | hot SSD | object storage | speed vs cost |
| Freshness | query stream directly | wait for segments/index | fresh tail vs architecture complexity |
| Ingestion | reject over quota | drop/sample | correctness vs protection |
| Partition | by tenant/time | by stream/hash | isolation vs balance |
| Consistency | immediate queryable | eventual index | freshness vs throughput |

Interview framing:

> I would design log storage as a durable append pipeline with stream buffering, immutable compressed segments, metadata/catalog indexes, and query fanout bounded by time and label indexes. Retention and tiering are first-class because storage cost dominates.

---

# 3. Low-Level Design

LLD goal:

> Model log storage around log records, streams, segments, catalog metadata, index blocks, query plans, retention policies, and tenant quotas.

Simple rules:

- Ingest is append-only.
- Segments are immutable after sealed.
- Catalog tells query service where to look.
- Indexes reduce scan but may lag.
- Retention deletes by policy and legal hold.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `LogRecord` | timestamp, labels, message | immutable after ingest |
| `LogStream` | tenant/service stream identity | partitions logs |
| `Segment` | compressed log block and time range | immutable when sealed |
| `SegmentCatalog` | segment metadata | discoverable by query |
| `IndexBlock` | label/text summaries | references existing segment |
| `QueryPlan` | selected segments and filters | bounded by tenant budget |
| `RetentionPolicy` | delete/tier rules | enforced by lifecycle job |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `IngestionService` | validate and append logs | run expensive queries |
| `SegmentWriter` | create immutable segments | enforce user query auth |
| `IndexService` | build indexes | mutate raw segment content |
| `QueryService` | plan/scan/filter | accept raw agent traffic |
| `RetentionService` | delete/tier old logs | serve user queries directly |

## 3.2 OOP Fundamentals

Encapsulation:

- `Segment` owns append-until-sealed lifecycle.
- `QueryPlan` owns budget and segment selection.
- `RetentionPolicy` owns expiry decision.

Abstraction:

- `SegmentStore` hides object storage.
- `IndexStore` hides label/text index backend.
- `StreamBuffer` hides Kafka/Pulsar details.

Polymorphism:

- Different index strategies: label-only, inverted, bloom.
- Different retention strategies: delete, tier, legal hold.

Composition:

- `QueryService` composes catalog, index store, segment store, scanner, and quota service.

## 3.3 SOLID Principles

| Principle | Log storage application |
|---|---|
| Single Responsibility | `SegmentWriter` only writes/seals segments |
| Open/Closed | add index type without rewriting ingestion |
| Liskov Substitution | any `SegmentStore` preserves put/get contract |
| Interface Segregation | separate ingest, query, index, lifecycle APIs |
| Dependency Inversion | services depend on store/index interfaces |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Producer-Consumer | ingest stream to segment/index workers | buffer spikes |
| Strategy | index and retention policies | choose by tenant/use case |
| Builder | query plan construction | compose filters/budgets |
| Iterator | paginated query results | stream large results |
| Observer/Event Publisher | segment sealed to index/retention | decouple pipelines |

## 3.5 UML / Diagrams

### Ingest Sequence

```text
Agent -> IngestionGateway: batch logs
IngestionGateway -> QuotaService: check tenant budget
IngestionGateway -> StreamBuffer: append records
StreamBuffer -> SegmentWriter: consume partition
SegmentWriter -> SegmentStore: write compressed segment
SegmentWriter -> Catalog: register segment
Catalog -> IndexService: segment sealed event
IndexService -> IndexStore: write label/text index
```

### Query Sequence

```text
User -> QueryService: query(time, filters)
QueryService -> Authz: can query tenant
QueryService -> Catalog/IndexStore: find candidate segments
QueryService -> SegmentStore: fetch segments
QueryService -> Scanner: decompress/filter
QueryService -> User: page results + cursor
```

## 3.6 Class Design

Interfaces:

```java
interface StreamBuffer {
    AppendResult append(List<LogRecord> records);
    List<LogRecord> read(String partition, long offset, int limit);
}

interface SegmentStore {
    void put(Segment segment, byte[] compressedBytes);
    byte[] get(String objectPath);
}

interface CatalogStore {
    void register(SegmentMetadata metadata);
    List<SegmentMetadata> find(QueryWindow window, LabelFilter filter);
}

interface QueryPlanner {
    QueryPlan plan(LogQuery query, TenantBudget budget);
}
```

Design notes:

- `register()` must be idempotent by segment ID.
- `plan()` should enforce query limits before scanning.
- Segment scanner should stream results to avoid loading huge files into memory.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| late log arrives | store by event time with ingestion-time fallback |
| segment written but catalog update fails | retry idempotent registration |
| index lags | query scans using catalog/time with warning or slower path |
| tenant exceeds quota | throttle, reject, or sample by policy |
| high-cardinality label | reject label or stop indexing it |
| query touches too much data | reject/queue/require narrower time range |
| retention races with query | snapshot candidate segment list or return partial with retry |
| duplicate ingest retry | dedup by event ID if product requires |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
logstorage/
  domain/
    LogRecord.java
    Segment.java
    SegmentMetadata.java
    IndexBlock.java
    QueryPlan.java
    RetentionPolicy.java
  service/
    IngestionService.java
    SegmentWriter.java
    IndexService.java
    QueryService.java
    RetentionService.java
  port/
    StreamBuffer.java
    SegmentStore.java
    CatalogStore.java
    IndexStore.java
  adapter/
    InMemoryStreamBuffer.java
    InMemorySegmentStore.java
  app/
    LogStorageDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from typing import Dict, List


@dataclass(frozen=True)
class LogRecord:
    tenant: str
    stream: str
    timestamp: int
    level: str
    message: str


@dataclass
class Segment:
    segment_id: str
    tenant: str
    stream: str
    min_ts: int
    max_ts: int
    records: List[LogRecord]


class InMemoryLogStorage:
    def __init__(self, segment_size: int = 3) -> None:
        self.segment_size = segment_size
        self.open_records: Dict[tuple[str, str], List[LogRecord]] = {}
        self.segments: List[Segment] = []

    def ingest(self, record: LogRecord) -> None:
        key = (record.tenant, record.stream)
        bucket = self.open_records.setdefault(key, [])
        bucket.append(record)
        if len(bucket) >= self.segment_size:
            self._seal(record.tenant, record.stream)

    def _seal(self, tenant: str, stream: str) -> None:
        key = (tenant, stream)
        records = self.open_records.get(key, [])
        if not records:
            return
        segment = Segment(
            segment_id=f"seg-{len(self.segments) + 1}",
            tenant=tenant,
            stream=stream,
            min_ts=min(r.timestamp for r in records),
            max_ts=max(r.timestamp for r in records),
            records=list(records),
        )
        self.segments.append(segment)
        self.open_records[key] = []

    def query(self, tenant: str, stream: str, start: int, end: int, contains: str = "") -> List[LogRecord]:
        results: List[LogRecord] = []
        for segment in self.segments:
            if segment.tenant != tenant or segment.stream != stream:
                continue
            if segment.max_ts < start or segment.min_ts > end:
                continue
            for record in segment.records:
                if start <= record.timestamp <= end and contains in record.message:
                    results.append(record)
        return sorted(results, key=lambda r: r.timestamp)


store = InMemoryLogStorage(segment_size=2)
store.ingest(LogRecord("t1", "api", 1, "INFO", "started"))
store.ingest(LogRecord("t1", "api", 2, "ERROR", "timeout"))
print(store.query("t1", "api", 1, 3, "timeout")[0].level)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `list[LogRecord]` | ingest buffer/segment records |
| `list[Segment]` | sealed segments |
| `dict[(tenant,stream,time) -> segments]` | catalog |
| `dict[label -> segmentIds]` | label index |
| `priorityQueue(retainUntil, segmentId)` | retention deletion |

## 4.4 Concurrency

High-signal concurrency issues:

- Multiple writers sealing same stream/time bucket.
- Segment write succeeds but catalog update fails.
- Query reads while retention deletes.
- Indexer processes same segment twice.

Handling strategy:

- Single writer per partition or segment lease.
- Idempotent segment IDs and catalog registration.
- Retention tombstones/grace period.
- Idempotent index writes by segment ID.

## 4.5 Testing Thinking

Unit tests:

- Ingest creates segments after threshold.
- Query prunes by time range.
- Query filters by tenant/stream/message.
- Duplicate segment registration is idempotent.
- Retention removes expired segments only.

Load tests:

- Incident log storm.
- Noisy tenant ingest spike.
- Query scanning large time range.
- Index backlog recovery.
- Cold storage query.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Incident log storm | service outage logs errors | ingest and stream lag |
| Noisy tenant | debug logging enabled | noisy neighbor impact |
| Query storm | incident investigation | query workers/storage overloaded |
| Index backlog | indexer down/restarted | stale search results |
| Retention failure | delete jobs stuck | runaway storage cost |

## 5.2 Immediate Spike Response

1. Protect ingestion durability and tenant isolation.
2. Apply per-tenant quotas/backpressure.
3. Increase stream partitions/writers for hot tenants.
4. Degrade full-text search before raw ingest.
5. Queue or reject expensive broad queries.
6. Sample/drop debug logs if tenant policy allows.
7. Prioritize retention and storage safety.

## 5.3 Degradation Policy

Protect in this order:

1. Durable ingest buffer.
2. Tenant isolation/quotas.
3. Segment writes and catalog.
4. Recent log tail.
5. Label/time query.
6. Full-text search.
7. Cold historical queries.

Not allowed:

- Let one tenant take down all ingestion.
- Silently exceed retention/compliance rules.
- Corrupt segment catalog.
- Lose acknowledged logs unless product explicitly allows sampling/dropping.

## 5.4 Spike Interview Answer

> During log storms I protect the durable ingest path and tenant isolation first. Indexing and broad queries can lag or be throttled. Segments remain immutable and replayable from the buffer, and retention/tiering controls cost.

---

# 6. Scaling To Global Log Volumes

## 6.1 Global Architecture

```text
Regional agents
  -> regional ingest gateways
  -> regional durable streams
  -> segment writers
  -> object storage tiers
  -> catalog/index stores
  -> federated query service
```

## 6.2 Multi-Region Strategy

- Ingest logs near source region.
- Keep tenant data residency rules.
- Replicate critical logs cross-region if needed.
- Query can federate across regions for global tenants.
- Cold archives can live in cheaper object storage.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Ingestion | regional gateways and batching |
| Buffer | partitioned durable stream |
| Segment storage | object storage by tenant/time |
| Catalog | tenant/time sharded metadata |
| Index | label/text partitions |
| Query | fanout workers and budgets |
| Retention | tier/delete lifecycle jobs |
| Isolation | quotas, cells, noisy tenant controls |

## 6.4 Global Interview Answer

> I would scale log storage by making ingestion append-only and partitioned, buffering through a durable stream, writing compressed immutable segments to object storage, and using catalog/index metadata to bound query fanout. Retention and tenant quotas are core design features, not afterthoughts.

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
I will clarify ingest volume, query types, full-text needs, freshness, retention, tenant isolation, and alerting scope.
I will estimate events/sec, bytes/day, replication, compression, index size, query fanout, and retention cost.
HLD includes agents, ingest gateways, stream buffer, segment writers, object storage, catalog, indexes, query service, retention, and alert pipeline.
I acknowledge ingestion after durable stream append.
Segments are immutable and compressed.
Indexes/catalog reduce query scan but may lag.
Tenant quotas and retention protect the system and cost.
```

---

# 8. Fast Recall Rules

- Log storage is append-heavy.
- Durable stream absorbs ingest spikes.
- Segments are immutable compressed files.
- Catalog maps tenant/time/stream to segments.
- Indexes reduce scan but cost storage and lag.
- Full-text search is expensive.
- High-cardinality labels hurt.
- Retention/tiering is core to cost control.
- Query fanout must be bounded.
- Tenant isolation prevents noisy neighbor collapse.
