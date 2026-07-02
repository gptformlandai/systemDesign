# Kafka Connect Operations And Connector Development Gold Sheet

> Track: Kafka Interview Track - Advanced Streaming
> Goal: operate Kafka Connect and understand when connector development, converters, SMTs, offsets, and task management matter.

---

## 1. Why This Sheet Exists

The existing Streams/Connect/CDC sheet explains when to use Connect.

This sheet fills production Connect depth:

- workers
- connectors
- tasks
- converters
- SMTs
- offset storage
- connector lifecycle
- task failures
- custom connector development
- upgrade and rollback

---

## 2. Mental Model

Kafka Connect is a framework for moving data into and out of Kafka.

```text
source system
  -> source connector
  -> source tasks
  -> converter
  -> Kafka topic

Kafka topic
  -> sink connector
  -> sink tasks
  -> converter
  -> target system
```

Connect is not for heavy business workflows. It is for integration plumbing.

---

## 3. Core Components

| Component | Meaning |
|---|---|
| worker | JVM process running Connect framework |
| connector | logical integration config |
| task | parallel unit of connector work |
| converter | converts Connect data to bytes and back |
| transform | lightweight per-record transformation |
| offset storage | tracks source positions |
| config/status storage | internal Connect metadata |

Distributed mode:
Multiple workers form a Connect cluster. Connect balances connector tasks across workers.

---

## 4. Source vs Sink

Source connector:

```text
database/files/SaaS
  -> Kafka
```

Examples:
- Debezium CDC
- JDBC source
- file/source connector
- cloud storage source

Sink connector:

```text
Kafka
  -> database/search/lake/SaaS
```

Examples:
- Elasticsearch sink
- JDBC sink
- S3/object storage sink
- Snowflake/warehouse sink

---

## 5. Converters

Converters control serialization format.

Common:
- Avro converter
- Protobuf converter
- JSON Schema converter
- JSON converter
- String/ByteArray converter

Rules:
- Use Schema Registry-backed formats for multi-team contracts.
- Avoid schemaless JSON for critical shared pipelines unless governance accepts it.
- Keep key converter and value converter explicit.
- Test converter compatibility before rollout.

Trap:
SMTs transform Connect records before converter serialization, not arbitrary post-serialization bytes.

---

## 6. Single Message Transforms

SMTs are lightweight transforms.

Good uses:
- rename field
- drop field
- insert metadata
- route by field
- unwrap Debezium envelope

Bad uses:
- API calls
- joins
- stateful enrichment
- complex business logic
- high-cost transformations

Rule:
If transformation needs state or business logic, use Kafka Streams or an application service.

---

## 7. Connector REST API

List connectors:

```bash
curl http://connect:8083/connectors
```

Get connector config:

```bash
curl http://connect:8083/connectors/orders-jdbc-sink/config
```

Status:

```bash
curl http://connect:8083/connectors/orders-jdbc-sink/status
```

Restart failed task:

```bash
curl -X POST http://connect:8083/connectors/orders-jdbc-sink/tasks/0/restart
```

Pause/resume:

```bash
curl -X PUT http://connect:8083/connectors/orders-jdbc-sink/pause
curl -X PUT http://connect:8083/connectors/orders-jdbc-sink/resume
```

Production rule:
Put connector config in version control. Do not only mutate through ad hoc REST calls.

---

## 8. Connector Failure Triage

Triage order:

```text
1. Connector status.
2. Task status.
3. Worker logs.
4. Source/target system health.
5. Schema/converter errors.
6. DLQ/errors topic.
7. Offset progress.
8. Recent config or credential changes.
```

Common failures:
- target rejects record
- schema incompatible
- bad credentials
- network timeout
- poison record
- connector task stuck
- worker rebalance
- target throttling

---

## 9. DLQ For Connect

Sink connectors can route bad records to DLQ depending on connector/framework config.

DLQ record should preserve:
- original topic
- partition
- offset
- key
- value if allowed
- error class/message
- connector name
- task id

Governance:
DLQs often contain sensitive broken payloads. Restrict ACLs and retention.

---

## 10. Offset Storage

Source connector offsets track source progress.

Examples:
- database binlog position
- file offset
- timestamp/cursor
- API page token

Offset risks:
- resetting source offset can duplicate or skip data
- connector migration must preserve offsets
- failed offset flush can replay
- source retention may expire before connector catches up

Interview line:
Connect offsets are as important as consumer offsets, but they represent source-system position.

---

## 11. Custom Connector Development

Build a custom connector only when:
- no maintained connector exists
- source/target API is proprietary
- connector logic is reusable across teams
- operational ownership is clear

Avoid custom connector when:
- a normal service consumer/producer is simpler
- business logic is complex
- retries/transactions require app-specific workflow
- team cannot maintain connector lifecycle

Development concepts:
- `SourceConnector` / `SinkConnector`
- task classes
- config definition and validation
- offset management
- retry and error handling
- schema/converter compatibility
- tests with mock source/target

---

## 12. Upgrade And Rollback

Connector upgrades need:
- compatibility notes
- plugin version pinning
- config diff
- schema impact review
- target/source compatibility check
- rollback plugin available
- offset safety review

Safe rollout:

```text
1. Test in staging with production-like data.
2. Deploy plugin to workers.
3. Pause connector if needed.
4. Update config in version control.
5. Resume and monitor task status.
6. Verify source/target counts and lag.
```

---

## 13. Connect Metrics

Track:
- connector status
- task status
- records read/written
- source lag
- sink write latency
- error rate
- DLQ count
- offset commit/flush latency
- worker rebalance count
- target throttling

Alert on:
- failed task
- no progress
- DLQ spike
- source lag approaching source retention
- sink latency spike

---

## 14. Failure Modes

| Failure | User/System Impact | Mitigation |
|---|---|---|
| sink task fails | target stale | restart, DLQ, fix target |
| source offset expires | data gap | increase source retention, recover from backup |
| bad SMT | schema/data corruption | CI test connector configs |
| wrong converter | unreadable records | explicit converter tests |
| worker rebalance loop | unstable tasks | inspect worker health/config |
| target throttling | lag grows | pause, rate limit, scale target |

---

## 15. Strong Interview Answer

```text
Kafka Connect is best for source/sink integration, not complex business logic. I
operate it by watching connector and task status, worker logs, offsets, DLQs,
schema/converter errors, and source or target system health. SMTs are fine for
small stateless changes, but stateful enrichment belongs in Streams or an app. For
custom connectors I need clear ownership, offset safety, retry behavior, tests,
and upgrade/rollback plans.
```

---

## 16. Revision Notes

- One-line summary: Connect moves data; apps and Streams own business logic.
- Three keywords: worker, task, converter.
- One interview trap: Using SMTs for complex stateful transformations.
- Memory trick: Connector is the plan; tasks do the work.

---

## 17. Official Source Notes

- Kafka Connect overview: https://kafka.apache.org/43/kafka-connect/overview/
- Kafka Connect user guide: https://kafka.apache.org/43/kafka-connect/userguide/
- Kafka Connect development guide: https://kafka.apache.org/43/kafka-connect/devguide/
