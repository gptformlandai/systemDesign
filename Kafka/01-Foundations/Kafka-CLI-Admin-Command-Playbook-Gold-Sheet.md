# Kafka CLI And Admin Command Playbook Gold Sheet

> Track: Kafka Interview Track - Foundations / Operations Bridge
> Goal: know the Kafka command-line tools well enough to inspect, debug, and operate Kafka safely.

---

## 1. Why This Sheet Exists

Kafka interviews often sound conceptual, but production Kafka work often starts with:

```text
What topic is this?
Which group is lagging?
Which partition is hot?
Who has access?
What config changed?
Can we replay safely?
```

The CLI tools help answer those questions.

Rule:
Use CLI commands to inspect first. Mutating commands need dry runs, peer review, and rollback plans.

---

## 2. Command Safety Levels

| Level | Examples | Risk |
|---|---|---|
| Read-only | describe topics, describe groups, list configs | low |
| Scoped mutation | create topic, alter config, reset dev offsets | medium |
| Cluster mutation | reassignment, ACL changes, broker config changes | high |
| Destructive | delete topic, force offset reset in prod | very high |

Production habit:
Paste the exact command into the incident timeline or change ticket.

---

## 3. Topic Commands

List topics:

```bash
kafka-topics.sh \
  --bootstrap-server broker1:9092 \
  --list
```

Describe topic:

```bash
kafka-topics.sh \
  --bootstrap-server broker1:9092 \
  --describe \
  --topic orders.created.v1
```

Create topic:

```bash
kafka-topics.sh \
  --bootstrap-server broker1:9092 \
  --create \
  --topic orders.created.v1 \
  --partitions 12 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000
```

Checklist before creating:
- owner
- schema
- key
- partitions
- retention
- cleanup policy
- ACLs
- DLQ/retry topic
- expected throughput

---

## 4. Topic Config Commands

Describe topic configs:

```bash
kafka-configs.sh \
  --bootstrap-server broker1:9092 \
  --entity-type topics \
  --entity-name orders.created.v1 \
  --describe
```

Alter topic retention:

```bash
kafka-configs.sh \
  --bootstrap-server broker1:9092 \
  --entity-type topics \
  --entity-name orders.created.v1 \
  --alter \
  --add-config retention.ms=1209600000
```

Remove topic override:

```bash
kafka-configs.sh \
  --bootstrap-server broker1:9092 \
  --entity-type topics \
  --entity-name orders.created.v1 \
  --alter \
  --delete-config retention.ms
```

Production trap:
Changing retention can delete data sooner than expected. Confirm replay/audit requirements first.

---

## 5. Consumer Group Commands

List groups:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --list
```

Describe group:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --describe \
  --group payment-risk-consumer
```

Useful interpretation:

```text
all partitions lagging:
  capacity, bad deploy, downstream slowness, broker issue

one partition lagging:
  hot key, poison record, partition-specific downstream issue

no active members but committed offsets exist:
  app is down or scaled to zero
```

---

## 6. Offset Reset Commands

Dry run to earliest:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --group payment-risk-replay \
  --topic payments.authorized.v1 \
  --reset-offsets \
  --to-earliest \
  --dry-run
```

Reset to timestamp:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --group payment-risk-replay \
  --topic payments.authorized.v1 \
  --reset-offsets \
  --to-datetime 2026-07-02T10:00:00.000 \
  --dry-run
```

Execute:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --group payment-risk-replay \
  --topic payments.authorized.v1 \
  --reset-offsets \
  --to-datetime 2026-07-02T10:00:00.000 \
  --execute
```

Safe reset rules:
- use dry run first
- prefer replay group over production group
- confirm idempotency
- rate limit replay
- announce downstream blast radius

---

## 7. Console Producer And Consumer

Producer with keys:

```bash
kafka-console-producer.sh \
  --bootstrap-server broker1:9092 \
  --topic test.orders \
  --property parse.key=true \
  --property key.separator=:
```

Consumer with key and metadata:

```bash
kafka-console-consumer.sh \
  --bootstrap-server broker1:9092 \
  --topic test.orders \
  --from-beginning \
  --property print.key=true \
  --property print.partition=true \
  --property print.offset=true \
  --property key.separator=:
```

Production warning:
Avoid console-consuming sensitive production topics unless access is approved and output is handled securely.

---

## 8. ACL Commands

List ACLs for topic:

```bash
kafka-acls.sh \
  --bootstrap-server broker1:9092 \
  --list \
  --topic payments.authorized.v1
```

Grant producer write:

```bash
kafka-acls.sh \
  --bootstrap-server broker1:9092 \
  --add \
  --allow-principal User:order-service \
  --operation Write \
  --operation Describe \
  --topic orders.created.v1
```

Grant consumer read:

```bash
kafka-acls.sh \
  --bootstrap-server broker1:9092 \
  --add \
  --allow-principal User:risk-service \
  --operation Read \
  --operation Describe \
  --topic payments.authorized.v1 \
  --group risk-service-group
```

Rule:
Grant topic and group access together for consumers. Avoid wildcard ACLs for business services.

---

## 9. Storage And Metadata Commands

KRaft storage formatting is normally handled by deployment automation.

Know conceptually:

```bash
kafka-storage.sh random-uuid
kafka-storage.sh format --config server.properties --cluster-id <cluster-id>
```

Production warning:
Do not run storage format on an existing broker log directory unless you are intentionally destroying/reinitializing local metadata. This is not a normal incident command.

---

## 10. Log Inspection

`kafka-dump-log.sh` can inspect local log segment files on brokers.

Use cases:
- debugging record batches
- verifying log corruption suspicions
- learning log internals

Risk:
It requires broker filesystem access and should be used carefully, usually by platform engineers.

---

## 11. Partition Reassignment

Partition reassignment moves replicas across brokers.

Use for:
- broker decommission
- disk balancing
- rack placement correction
- hot broker mitigation

Production rules:
- throttle reassignment
- move in small batches
- monitor ISR and request latency
- avoid during controller/broker incidents
- keep rollback plan

Interview sentence:
Reassignment is cluster surgery; do it slowly and watch replication health.

---

## 12. Incident Command Checklist

For lag:

```text
1. Describe consumer group.
2. Identify topic/partition lag shape.
3. Check app deploy and processing errors.
4. Check downstream DB/API latency.
5. Check rebalance rate and max.poll interval.
6. Check DLQ/retry topic.
```

For broker health:

```text
1. Describe affected topic.
2. Check under-replicated/offline partitions.
3. Check disk/network/CPU.
4. Check controller stability.
5. Pause large admin operations.
```

For security failure:

```text
1. Identify principal.
2. Confirm topic/group operation.
3. List ACLs or provider IAM/RBAC.
4. Check certificate/token expiry.
5. Apply least-privilege fix.
```

---

## 13. Strong Interview Answer

```text
I use Kafka CLI tools first for inspection: describe topics, describe consumer
groups, inspect configs, and list ACLs. Mutating commands like offset reset,
retention changes, ACL updates, and partition reassignment need dry runs, owner
approval, and monitoring. For production replay I prefer a separate consumer group
with idempotent downstream writes rather than casually changing the live group's
offsets.
```

---

## 14. Revision Notes

- One-line summary: Kafka CLI is for inspection first and careful mutation second.
- Three keywords: describe, dry-run, owner.
- One interview trap: Resetting production offsets without idempotency.
- Memory trick: Topics, groups, configs, ACLs, offsets, reassignment.

---

## 15. Official Source Notes

- Apache Kafka basic operations: https://kafka.apache.org/43/operations/basic-operations/
- Apache Kafka authorization and ACLs: https://kafka.apache.org/43/security/authorization-and-acls/
