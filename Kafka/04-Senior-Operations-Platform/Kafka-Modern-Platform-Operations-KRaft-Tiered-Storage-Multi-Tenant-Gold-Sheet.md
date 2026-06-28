# Kafka Modern Platform Operations KRaft Tiered Storage Multi-Tenant Gold Sheet

> Track: Kafka Interview Track - Senior / MAANG Platform Layer

Target: senior backend, platform, and MAANG interviews where Kafka is discussed as a shared production platform, not only a producer-consumer library.

This sheet fills the modern platform layer: KRaft operations, controller quorum, tiered storage, managed Kafka caveats, quotas, rack awareness, partition reassignment, cross-cluster DR, capacity planning, and platform runbooks.

---

## 0. How To Read This

Use this after the broker internals, operations, and advanced API sheets.

Mental model:

```text
cluster metadata -> controller quorum -> broker capacity -> topic placement -> client behavior -> tenant governance -> incident runbook
```

Senior answer shape:

```text
I separate Kafka application behavior from platform behavior. Producers and consumers create traffic, but the platform owner must protect metadata health, broker capacity, replica placement, tenant limits, security boundaries, and recovery paths.
```

---

# Topic 1: Modern Kafka Platform Operations

## 1. Intuition

A Kafka cluster is like a shared highway system:

- brokers are roads
- partitions are lanes
- controllers manage the traffic map
- producers and consumers are vehicles
- quotas are speed limits
- replication is lane redundancy
- monitoring is traffic control

A single bad tenant, topic, partition key, or reassignment can hurt everyone.

---

## 2. Definition

- Definition: Kafka platform operations is the discipline of running Kafka as a reliable shared service across teams, workloads, topics, clusters, and failure domains.
- Category: distributed data platform operations.
- Core idea: keep metadata, storage, networking, broker capacity, security, and tenant behavior healthy together.

---

## 3. Why It Exists

Kafka is often central infrastructure.

Without platform discipline:

- one hot partition causes one broker to burn
- one tenant floods brokers and hurts others
- bad partition reassignment overloads the cluster
- missing rack awareness reduces zone-failure resilience
- schema/security changes break consumers
- replay jobs overwhelm downstream systems
- DR plans fail when offsets, schemas, and consumer state are forgotten

---

## 4. Modern Platform Map

A senior Kafka platform answer should cover:

| Layer | Questions |
|---|---|
| Metadata | Is KRaft/controller quorum healthy? |
| Broker | CPU, disk, network, page cache, request latency? |
| Partition placement | Are leaders and replicas balanced across brokers/racks? |
| Storage | Retention, compaction, segment size, tiered storage behavior? |
| Clients | Producer errors, consumer lag, rebalance rate, retry storms? |
| Tenants | ACLs, quotas, topic naming, ownership, cost controls? |
| Contracts | Schema compatibility, event naming, PII classification? |
| DR | RPO/RTO, replication, failover, replay, offset strategy? |

---

## 5. KRaft Operations

KRaft replaces ZooKeeper with Kafka-managed metadata quorum.

Core components:

- controller quorum stores cluster metadata
- brokers serve partition traffic
- controllers elect active controller
- metadata changes are replicated through the quorum

Production considerations:

- use 3 or 5 controllers depending on scale and fault tolerance
- keep controller quorum stable and monitored
- separate broker/controller roles for critical clusters when appropriate
- alert on controller election churn
- protect metadata log disk and network
- avoid noisy admin automation during incidents

Strong answer:

```text
In modern Kafka, metadata health is a first-class dependency. If the KRaft quorum is unhealthy,
operations like topic creation, partition reassignment, and leader election can be affected even
if some brokers are still serving data.
```

---

## 6. KRaft Failure Signals

Watch:

- active controller changes
- controller quorum unavailable
- metadata propagation delay
- failed topic/partition admin operations
- broker unable to register or receive metadata
- controller CPU/disk/network saturation

Runbook:

1. Confirm controller quorum health.
2. Check active controller stability.
3. Check broker registration and metadata errors.
4. Pause non-urgent admin automation.
5. Avoid large reassignments until metadata health recovers.
6. Restore failed controller/broker nodes carefully.

---

## 7. Tiered Storage

Tiered storage moves older log segments to remote/object storage while keeping recent hot data local.

Why it exists:

- longer retention without keeping all data on broker disks
- lower local storage pressure
- better replay economics for historical data
- separation of hot and cold storage

Trade-offs:

| Benefit | Cost |
|---|---|
| longer retention | remote read latency |
| lower broker disk pressure | operational dependency on remote storage |
| cheaper historical replay | platform/version-specific behavior |
| easier storage scaling | more metrics and failure modes |

Interview line:

```text
Tiered storage changes storage economics, but it does not make replay free. Historical reads may
be slower and depend on remote storage health, so I would validate replay SLAs.
```

---

## 8. Tiered Storage Runbook

Questions during design:

1. Which topics need long retention?
2. What is hot retention vs remote retention?
3. How often do consumers replay old data?
4. Is replay latency acceptable from remote storage?
5. How is remote storage secured and monitored?
6. What happens if remote storage is unavailable?
7. How does compaction interact with remote segments in the target platform?

Incident symptoms:

- old offset replay is slow
- remote read errors
- unexpected broker disk growth
- retention does not behave as expected
- object storage access/permission failure

---

## 9. Managed Kafka Caveats

Managed Kafka platforms are excellent, but they hide and constrain parts of the platform.

Ask:

- Which Kafka version and features are supported?
- Is KRaft exposed or abstracted?
- Is tiered storage available?
- Are ACLs native Kafka ACLs or provider IAM/RBAC?
- Are quotas configurable?
- Can you run custom connectors?
- How are upgrades handled?
- What metrics/logs are exposed?
- What is the failover/DR model?
- What are partition/topic limits?

Strong answer:

```text
On managed Kafka, I avoid assuming upstream behavior. I check provider limits for quotas,
ACLs, tiered storage, metrics, connector support, and cross-region replication.
```

---

## 10. Multi-Tenant Kafka Platform

Multi-tenant Kafka means many teams share one platform.

Controls:

- topic naming standards
- ownership metadata
- ACLs per service principal
- producer and consumer quotas
- partition limits per tenant
- schema subject ownership
- retention and compaction policies
- PII classification
- cost attribution
- onboarding/offboarding process

Topic naming example:

```text
<domain>.<entity>.<event-type>.v<major>
orders.booking.created.v1
payments.payment.authorized.v1
```

Tenant guardrail:

```text
No team should create unlimited topics, unlimited partitions, wildcard ACLs, or unbounded retention without review.
```

---

## 11. Quotas

Quotas protect shared clusters from noisy tenants.

Common quota types:

- producer byte rate
- consumer byte rate
- request rate
- connection/client quotas depending on platform

Use quotas when:

- teams share one cluster
- one workload can spike suddenly
- replay jobs can saturate brokers
- cost attribution matters
- platform stability matters more than one tenant's burst

Trap:

```text
Quotas can become hidden throttling. Monitor quota violations and communicate limits to teams.
```

---

## 12. Rack Awareness And Failure Domains

Rack awareness places replicas across failure domains.

Failure domains can be:

- rack
- availability zone
- node pool
- cloud region in cross-cluster designs

Goal:

```text
One rack or zone failure should not take all replicas for the same partition.
```

Checklist:

1. Brokers have correct rack/zone metadata.
2. Replication factor is high enough.
3. Min ISR matches durability/availability goals.
4. Leader distribution is balanced.
5. Partition reassignment preserves placement rules.

---

## 13. Partition Reassignment And Rebalancing

Partition reassignment moves replicas between brokers.

Use it for:

- adding brokers
- removing brokers
- disk imbalance
- hot broker mitigation
- rack placement correction
- topic expansion cleanup

Risks:

- network saturation
- disk I/O spike
- under-replicated partitions during movement
- longer recovery if too many partitions move
- leader imbalance after reassignment

Runbook:

1. Measure broker CPU/network/disk before movement.
2. Move partitions in batches.
3. Throttle reassignment bandwidth.
4. Watch under-replicated partitions and request latency.
5. Verify leader balance after movement.
6. Roll back or pause if cluster health degrades.

---

## 14. Cross-Cluster DR

DR questions:

| Question | Why It Matters |
|---|---|
| RPO | how much data loss is acceptable |
| RTO | how quickly service must recover |
| active-passive vs active-active | conflict and duplicate complexity |
| offsets | consumers must resume intentionally |
| schema registry | schemas must be available in DR |
| ACLs/secrets | clients need access in failover region |
| topic config | retention/partitions/compaction must match |

Common options:

- MirrorMaker 2 style replication
- managed provider cluster linking
- application dual-write only with strong justification
- outbox replay from source database for some domains

Trap:

```text
Replicating messages is not a full DR plan. You also need schemas, ACLs, offsets, clients,
DNS/routing, downstream idempotency, and runbooks.
```

---

## 15. Active-Active Trap

Active-active Kafka across regions is hard because:

- ordering is local to partition in one cluster
- conflict resolution is application-specific
- duplicate events are likely
- bidirectional replication can loop without controls
- latency affects producer/consumer expectations

Strong answer:

```text
I prefer active-passive or regional ownership by key/domain unless the business truly needs
active-active. Then I design idempotency, conflict resolution, and ownership rules explicitly.
```

---

## 16. Capacity Planning Formula

Start with rough estimates:

```text
producer_bytes_per_day = messages_per_second * avg_message_bytes * 86400
replicated_bytes_per_day = producer_bytes_per_day * replication_factor
retained_bytes = replicated_bytes_per_day * retention_days
compressed_bytes = retained_bytes / compression_ratio
```

Also estimate:

- partitions per broker
- leader partitions per broker
- network in/out
- consumer fan-out
- page cache pressure
- compaction workload
- replay bandwidth
- controller metadata scale

Interview line:

```text
Kafka capacity is not only raw storage. Replication, fan-out, retention, compaction, network,
page cache, and replay behavior all matter.
```

---

## 17. Platform Dashboard

Useful panels:

- offline partitions
- under-replicated partitions
- active controller changes
- produce/fetch request latency
- broker network in/out
- broker disk usage
- partition count per broker
- leader imbalance
- consumer lag by group/topic
- rebalance rate
- failed produce requests
- quota violations
- auth/ACL failures
- schema registry errors
- connector task failures
- reassignment progress
- tiered storage remote read errors if enabled

---

## 18. Common Platform Mistakes

| Mistake | Better Approach |
|---|---|
| Let every team self-create unlimited topics | governance and ownership workflow |
| Add partitions without key/order review | check ordering, consumer scaling, future imbalance |
| Reassign too many partitions at once | throttle and batch movement |
| Use wildcard ACLs | least-privilege service principals |
| Ignore quota violations | tune limits or fix noisy client behavior |
| Treat DR as only message replication | include schemas, ACLs, offsets, clients, runbooks |
| Assume managed Kafka equals upstream Kafka | check provider feature boundaries |
| Enable long retention without cost model | estimate storage, replay, and tiered-storage behavior |

---

## 19. Scenario

Prompt:

```text
Your company wants one shared Kafka platform for 40 teams. How do you make it safe?
```

Strong answer:

```text
I would define topic naming and ownership, require schema compatibility, use service principals
with least-privilege ACLs, set producer/consumer quotas, enforce retention defaults, classify
PII topics, monitor lag/ISR/offline partitions/quota violations, and create a controlled topic
creation workflow. For resilience, I would use replication factor 3, rack awareness, balanced
leaders, KRaft health monitoring, and DR runbooks that include schemas, ACLs, offsets, and
client routing. I would avoid letting one tenant create unlimited partitions or run unbounded
replay jobs without review.
```

---

## 20. Revision Notes

- KRaft metadata health is production-critical.
- Tiered storage improves retention economics but changes replay behavior.
- Managed Kafka needs provider-specific feature checks.
- Multi-tenant Kafka needs ACLs, quotas, ownership, schema governance, and cost controls.
- Partition reassignment must be throttled and monitored.
- DR requires messages plus schemas, ACLs, offsets, clients, and downstream idempotency.
- Capacity planning includes storage, replication, fan-out, network, partitions, and replay.

---

## 21. Official Source Notes

- Apache Kafka KRaft docs: https://kafka.apache.org/43/operations/kraft/
- Apache Kafka operations docs: https://kafka.apache.org/43/operations/ops/
- Apache Kafka monitoring docs: https://kafka.apache.org/43/operations/monitoring/
- Apache Kafka security docs: https://kafka.apache.org/43/security/
