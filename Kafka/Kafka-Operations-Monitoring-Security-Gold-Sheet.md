# Kafka Operations, Monitoring, and Security Gold Sheet

> Goal: debug Kafka like a production engineer: identify lag, broker trouble, ISR shrink, bad deploys, security gaps, and recovery paths.

---

## 0. How To Read This

Beginner focus:

- consumer lag
- broker down
- disk full
- retry
- DLQ

Intermediate focus:

- under-replicated partitions
- offline partitions
- ISR shrink
- rebalance
- quotas
- TLS/SASL/ACLs

Senior focus:

- incident triage
- KRaft quorum health
- multi-tenant isolation
- cross-region recovery
- schema incident recovery
- capacity planning
- security operations

---

# Topic 1: Kafka Operations, Monitoring, and Security

---

## 1. Intuition

Kafka operations are like air traffic control.

The planes are still flying even when one screen turns yellow, but you need to know which warning means:

- traffic is delayed
- a runway is down
- radar is unstable
- a plane is carrying invalid cargo
- someone unauthorized is trying to enter

Beginner explanation:

Kafka production health is mostly about lag, partition availability, replication health, disk usage, request latency, and client errors. If you monitor those well, most incidents become explainable instead of mysterious.

---

## 2. Definition

- Definition: Kafka operations cover the monitoring, alerting, security, capacity planning, incident response, and recovery practices needed to run Kafka reliably.
- Category: Distributed system operations
- Core idea: protect the log, keep consumers within replay windows, and catch replication/security issues before data is lost or delayed.

---

## 3. Why It Exists

Kafka can fail partially:

- one broker down
- one partition hot
- one consumer group lagging
- one producer timing out
- one schema breaks consumers
- one connector stuck
- one disk filling
- one controller unstable

Because Kafka is distributed, "cluster is up" is not enough.

You need to ask:

```text
Are partitions online?
Are replicas in sync?
Are producers succeeding?
Are consumers keeping up?
Are disks safe?
Are controllers stable?
Are schemas and ACLs correct?
```

---

## 4. Reality

Operational Kafka knowledge matters in:

- platform teams
- fintech/event-ledger systems
- data infrastructure
- high-volume microservices
- CDC platforms
- search indexing pipelines
- notification systems
- fraud/ML streams

Senior interviews often include Kafka failure scenarios because they reveal whether you understand distributed behavior beyond happy-path APIs.

---

## 5. How It Works

### Part A: Golden Signals

Track these first:

| Area | What To Watch |
|---|---|
| Availability | offline partitions, controller health, broker count |
| Replication | under-replicated partitions, ISR shrink, replica lag |
| Producers | error rate, request latency, timeout rate, batch size, retry rate |
| Consumers | lag, commit rate, rebalance rate, processing error rate |
| Storage | disk usage, log growth, retention pressure |
| Network | bytes in/out, request queue time, connection errors |
| Security | auth failures, denied ACLs, certificate expiry |
| Data quality | schema errors, DLQ count, poison records |

### Part B: Consumer Lag

Lag means:

```text
latest broker offset - committed consumer offset
```

Lag can mean:

- consumer is slow
- downstream dependency is slow
- rebalance storm
- poison record
- partition is hot
- consumer has crashed
- partition count is too low for parallelism

Do not only ask "what is total lag?" Ask:

```text
Which group?
Which topic?
Which partition?
Is lag increasing or decreasing?
What changed recently?
```

### Part C: Under-Replicated Partitions

A partition is under-replicated when followers are not fully caught up with leader.

Risk:

- reduced fault tolerance
- potential write failures if ISR drops below minimum
- higher recovery risk if leader fails

Immediate checks:

- broker health
- network
- disk I/O
- replica fetcher lag
- recent broker restart
- leader imbalance

### Part D: Offline Partitions

Offline partition means no leader is available.

This is serious.

User impact:

- producers cannot write
- consumers cannot read
- availability is broken for that partition

Immediate checks:

- which brokers hosted replicas
- are any ISR replicas alive
- controller logs
- recent reassignment
- disk failures

### Part E: Rebalance Storm

Symptoms:

- consumers repeatedly join/leave
- throughput drops
- lag grows
- duplicate processing increases

Causes:

- long processing blocks `poll()`
- `max.poll.interval.ms` exceeded
- unstable network
- deployment rolling too aggressively
- slow startup
- bad consumer health checks

Fixes:

- shorten processing per poll
- pause/resume partitions for slow work
- tune `max.poll.interval.ms`
- use cooperative rebalancing where appropriate
- deploy gradually

### Part F: DLQ and Retry Operations

DLQ is not a trash bin. It is an operational queue.

DLQ record should include:

- original topic
- partition
- offset
- key
- payload
- headers
- exception
- consumer group
- timestamp
- attempt count

Runbook:

1. Alert on DLQ count or rate.
2. Identify root cause.
3. Fix schema/code/data issue.
4. Replay if safe.
5. Mark discarded records with business approval.

### Part G: Capacity Planning

Plan for:

- write throughput
- read throughput
- message size
- retention days
- replication factor
- compression ratio
- consumer parallelism
- broker disk
- broker network
- broker CPU

Storage estimate:

```text
raw_daily = messages_per_day * average_message_size
replicated_daily = raw_daily * replication_factor
compressed_daily = replicated_daily / compression_ratio
retention_storage = compressed_daily * retention_days
```

Add headroom:

- broker failure
- replay
- traffic spikes
- compaction overhead
- reassignments

### Part H: Security

Kafka security has three big layers:

| Layer | Purpose |
|---|---|
| Encryption | protect traffic with TLS/SSL |
| Authentication | prove identity with TLS certs, SASL/SCRAM, SASL/OAUTHBEARER, Kerberos, etc. |
| Authorization | allow/deny operations with ACLs |

ACL examples:

```text
order-service can Write to order-events
payment-service can Read from order-events as group payment-service
analytics-service can Read from order-events as group analytics
```

Production principle:

```text
least privilege per service principal
```

### Part I: Multi-Tenancy and Quotas

If many teams share Kafka:

- use naming conventions
- enforce ACLs
- set producer/consumer quotas
- track ownership
- isolate critical topics
- limit topic creation
- define retention defaults
- monitor noisy tenants

### Part J: Cross-Region and Disaster Recovery

Kafka DR choices:

| Pattern | Use |
|---|---|
| Backup/archive | compliance and recovery history |
| Mirror topics to another cluster | regional failover/read locality |
| Active-passive | simpler failover, lower conflict risk |
| Active-active | complex, conflict/idempotency design required |

Cross-region warning:

- latency increases
- ordering expectations can break
- duplicate events are common during failover
- consumer offsets may not map cleanly unless planned

---

## 6. What Problem It Solves

- Primary problem solved: keeping Kafka reliable under partial failure and growth
- Secondary benefits: faster incident recovery, better security, predictable capacity
- Systems impact: prevents silent data loss, prolonged lag, and unauthorized data access

---

## 7. When To Rely On Operational Patterns

Use strong monitoring when:

- Kafka carries business-critical events
- multiple teams depend on topics
- replay windows matter
- cross-region recovery matters
- consumers write to external systems

Use quotas when:

- Kafka is shared by many teams
- one producer can overload brokers
- platform team needs blast-radius control

Use strict ACLs when:

- topics contain sensitive data
- compliance matters
- production is multi-tenant

---

## 8. When Not To Overbuild

For small learning/dev clusters:

- simple auth may be enough
- one broker may be acceptable locally
- no DR needed

For production:

- do not skip auth
- do not skip disk alerts
- do not skip lag alerts
- do not skip replication health alerts

Senior maturity is choosing operational rigor based on business risk.

---

## 9. Pros and Cons

| Practice | Pros | Cons |
|---|---|---|
| Strict ACLs | safer multi-team access | more admin work |
| TLS everywhere | protects data in transit | CPU/config/cert management |
| Quotas | protects cluster from noisy clients | can throttle during spikes |
| Long retention | more replay safety | higher storage cost |
| Cross-region mirroring | DR and locality | lag, duplicates, operational complexity |
| DLQ | isolates poison records | needs ownership and replay tooling |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- High durability:
  More replicas and stricter ISR settings, but higher cost and potential write unavailability during failures.
- High availability:
  Looser settings may keep writes flowing, but can raise data loss risk.
- Long retention:
  Better replay, but larger disks and longer recovery operations.
- Strict security:
  Safer access, but more certificate/ACL management.
- Multi-tenancy:
  Better platform efficiency, but requires quotas and governance.

### Common Mistakes

- Mistake: "Consumer lag is one number."
  Why it is wrong: lag must be checked per group/topic/partition and trend.
  Better approach: identify the partition and rate of change.

- Mistake: "DLQ means we can ignore bad records."
  Why it is wrong: DLQ is delayed work.
  Better approach: assign ownership and replay/discard process.

- Mistake: "All partitions online means cluster is healthy."
  Why it is wrong: ISR may be shrinking, lag may be growing, or disks may be near full.
  Better approach: monitor multiple health dimensions.

- Mistake: "Give all services wildcard ACLs."
  Why it is wrong: one compromised service can read/write everything.
  Better approach: least privilege per topic and group.

---

## 11. Key Metrics

Important metrics and alerts:

| Metric | Why It Matters |
|---|---|
| Offline partition count | no leader means unavailable partition |
| Under-replicated partitions | reduced fault tolerance |
| ISR shrink/expand rate | replication instability |
| Consumer lag per partition | processing delay and hot partitions |
| Producer request latency | broker/network pressure |
| Producer error/timeout rate | write path failure |
| Broker disk usage | data loss/outage risk if full |
| Network bytes in/out | capacity and traffic spikes |
| Request queue time | broker saturation |
| Rebalance rate | consumer group instability |
| DLQ count/rate | data quality or code issue |
| Auth failure rate | security/config issue |
| Certificate expiry | future outage risk |
| Controller/quorum health | metadata and leadership stability |

Alerting rule:

```text
alert on customer impact or future customer impact,
not only on raw metric noise
```

---

## 12. Failure Modes And Runbooks

### Case 1: Consumer Lag Growing

Check:

- which group/topic/partition
- lag trend
- consumer instance health
- processing latency
- downstream dependency latency
- DLQ/errors
- recent deploy
- hot partition

Actions:

- scale consumers up to partition count
- fix downstream slowness
- pause/retry poison records
- increase partitions only with ordering review
- rollback bad deploy

### Case 2: Under-Replicated Partitions

Check:

- broker down?
- disk saturated?
- network issue?
- replica fetcher lag?
- recent partition reassignment?

Actions:

- restore broker health
- reduce broker pressure
- rebalance leaders if needed
- avoid risky maintenance until ISR recovers

### Case 3: Offline Partitions

Check:

- leader unavailable
- ISR replicas unavailable
- controller logs
- broker logs
- disk corruption/failure

Actions:

- restore brokers with replicas
- validate leader election
- avoid unclean leader election unless business accepts data loss
- communicate impact by topic/partition

### Case 4: Disk Usage Critical

Check:

- topic retention
- largest topics
- unexpected producer spike
- compaction lag
- stale test topics

Actions:

- expand disk
- reduce retention with approval
- throttle abusive producers
- delete unused topics carefully
- move partitions if needed

### Case 5: Schema Break

Check:

- producer deploy
- schema registry compatibility
- deserialization errors
- DLQ records

Actions:

- rollback producer
- restore compatible schema
- fix consumer if needed
- replay DLQ after validation

### Case 6: Security Access Failure

Check:

- principal identity
- ACL for topic/group/cluster resource
- listener/SASL/TLS config
- certificate expiry
- recent rotation

Actions:

- fix ACL least-privilege
- rotate or restore certificate
- validate service principal
- avoid wildcard grants as a shortcut

---

## 13. Scenario

- Product / system: Shared Kafka platform for 30 microservice teams
- Why this concept fits: many producers/consumers need reliability, isolation, security, and incident clarity
- What would go wrong without it: one team can overload brokers, unauthorized services can read sensitive events, lag can silently exceed retention, and replication problems can become data loss

---

## 14. Code Sample

Example ACL commands:

```bash
kafka-acls.sh \
  --bootstrap-server broker1:9092 \
  --add \
  --allow-principal User:payment-service \
  --operation Read \
  --topic order-events \
  --group payment-service
```

```bash
kafka-acls.sh \
  --bootstrap-server broker1:9092 \
  --add \
  --allow-principal User:order-service \
  --operation Write \
  --topic order-events
```

Example lag check:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server broker1:9092 \
  --describe \
  --group payment-service
```

---

## 15. Mini Program / Simulation

This simple lag trend classifier helps interview explanation.

```python
def classify_lag(previous_lag, current_lag, consumer_rate, producer_rate):
    if current_lag == 0:
        return "healthy"
    if current_lag > previous_lag and producer_rate > consumer_rate:
        return "falling behind: consumer slower than producer"
    if current_lag < previous_lag:
        return "catching up"
    if current_lag == previous_lag:
        return "stable lag: check SLA and retention window"
    return "needs more context"


def main():
    print(classify_lag(1000, 1500, consumer_rate=500, producer_rate=700))
    print(classify_lag(1000, 700, consumer_rate=900, producer_rate=500))
    print(classify_lag(1000, 1000, consumer_rate=500, producer_rate=500))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> A Kafka consumer group is lagging badly after a deploy. How would you debug it?

---

## 17. Strong Answer

I would first scope the lag: group, topic, partition, and trend. If only one partition is lagging, I suspect hot key or poison record. If all partitions lag, I suspect consumer capacity, downstream dependency, or bad deploy.

Then I would check consumer logs, processing latency, error rate, rebalance rate, and DLQ. I would verify whether `max.poll.interval.ms` is being exceeded or whether the app is stuck on slow DB/API calls.

If it is a bad deploy, I would rollback. If it is downstream slowness, I would throttle, pause partitions, or move to retry topics. If capacity is low, I would scale consumers up to partition count. If a poison record blocks progress, I would isolate it to DLQ with topic/partition/offset and replay after fixing.

Finally, I would verify recovery by watching lag decrease, commit rate normalize, DLQ stop growing, and rebalance rate stabilize.

---

## 18. Revision Notes

- One-line summary: Kafka operations are lag, replication, partitions, disks, controllers, clients, DLQs, and security.
- Three keywords: lag, ISR, ACL
- One interview trap: "cluster up" does not mean "data safe and consumers healthy."
- One memory trick: availability asks "can I read/write?", durability asks "how many safe copies?", operations asks "will this stay true tomorrow?"

---

## 19. Official Source Notes

- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Apache Kafka KRaft docs: <https://kafka.apache.org/43/operations/kraft/>
- Apache Kafka security overview: <https://kafka.apache.org/43/security/security-overview/>
- Apache Kafka ACL docs: <https://kafka.apache.org/43/security/authorization-and-acls/>

