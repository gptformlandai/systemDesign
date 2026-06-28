# Kafka Advanced API and Platform Concepts Gold Sheet

> Goal: cover the Kafka API and platform knobs that often appear in senior interviews after the interviewer is satisfied with producer, partition, consumer group, and delivery guarantees.

---

## 0. How To Read This

Beginner focus:

- headers
- partitions
- consumer offset
- topic metadata

Intermediate focus:

- `subscribe` vs `assign`
- `seek`
- `pause` / `resume`
- `commitSync` / `commitAsync`
- AdminClient
- cooperative rebalancing

Senior focus:

- static membership
- rack awareness
- leader imbalance
- partition reassignment
- MirrorMaker / cross-cluster replication
- managed Kafka caveats
- operational safety for admin APIs

---

# Topic 1: Kafka Advanced API and Platform Concepts

---

## 1. Intuition

The normal Kafka story is:

```text
producer -> topic -> partition -> consumer group
```

Advanced Kafka is about controlling the edges:

- how producers annotate and route records
- how consumers recover, replay, pause, and rebalance
- how platform teams inspect and change cluster state
- how clusters survive placement, region, and operations issues

Beginner explanation:

Kafka's basic APIs move data. Kafka's advanced APIs control recovery, replay, admin operations, and production safety.

---

## 2. Definition

- Definition: Advanced Kafka APIs and platform concepts are the tools used to control client behavior, inspect cluster metadata, manage topics/configs, and operate Kafka safely at scale.
- Category: Kafka client/platform operations
- Core idea: use advanced knobs only when they solve a clear correctness, performance, or operational problem.

---

## 3. Why It Exists

At small scale, simple Kafka usage works:

```text
send()
poll()
commit()
```

At production scale, you need more control:

- replay from a timestamp
- pause one partition while others continue
- avoid full stop-the-world rebalances
- keep the same consumer identity across restarts
- create and inspect topics safely
- move partitions off hot brokers
- route replicas across racks
- replicate topics across clusters

Advanced APIs exist for those moments.

---

## 4. Reality

These concepts appear in:

- senior backend interviews
- platform engineering interviews
- Kafka incident debugging
- data infrastructure roles
- fintech/order/payment pipelines
- managed Kafka migrations
- cross-region disaster recovery

They are not the first Kafka topics to learn, but they are common follow-ups once your foundation is clear.

---

## 5. How It Works

### Part A: Producer Headers

Kafka records can carry headers.

Use headers for metadata such as:

- trace ID
- correlation ID
- event source
- schema hint
- retry count
- original topic/partition/offset for DLQ records
- tenant ID when security model allows it

Do not use headers for:

- core business data
- fields consumers must query heavily
- large payloads
- secrets

Interview sentence:

> Headers are good for operational metadata. The value payload remains the business event contract.

### Part B: Producer Interceptors

Producer interceptors can observe or modify records before send and observe acknowledgments after send.

Use carefully for:

- metrics
- tracing
- adding standard headers
- lightweight audit metadata

Avoid for:

- heavy business logic
- remote API calls
- expensive transformations
- changing event semantics invisibly

Trap:

If important event behavior is hidden inside interceptors, debugging becomes painful.

### Part C: Producer Backpressure Knobs

Important configs:

| Config | Why It Matters |
|---|---|
| `buffer.memory` | local memory used for unsent records |
| `max.block.ms` | how long `send()` can block waiting for metadata/buffer |
| `delivery.timeout.ms` | max total time to report send success/failure |
| `request.timeout.ms` | request-level timeout |
| `batch.size` | upper bound for producer batches |
| `linger.ms` | wait time to build better batches |
| `compression.type` | throughput/storage improvement |

Senior framing:

```text
batching improves throughput
linger can add latency
compression saves network/disk but costs CPU
buffer pressure means producers are faster than Kafka can accept
```

### Part D: `subscribe` vs `assign`

`subscribe`:

- consumer joins a group
- Kafka handles partition assignment
- rebalances happen
- common for scalable service consumers

`assign`:

- application manually assigns partitions
- no group rebalancing for that consumer assignment
- useful for special tools, replay jobs, debugging, or custom control

Rule:

Use `subscribe` for normal applications. Use `assign` only when you intentionally want manual partition control.

### Part E: `seek`

`seek` moves a consumer to a specific offset.

Use cases:

- replay from known offset
- replay from timestamp after resolving to offsets
- debugging a specific bad record
- rebuilding a derived store
- controlled backfill

Risk:

- reprocessing can trigger duplicate side effects
- seeking past records can skip work

Safe replay checklist:

1. Identify topic, partition, offset or timestamp.
2. Confirm side effects are idempotent.
3. Use a separate consumer group when needed.
4. Limit replay scope.
5. Monitor downstream impact.

### Part F: `pause` and `resume`

`pause` stops fetching from selected partitions without leaving the group.

Use when:

- one partition has slow downstream work
- you need backpressure per partition
- retry delay should not block all partitions
- external dependency is temporarily slow

`resume` restarts fetching.

Trap:

Pausing forever is silent lag. Always track paused partitions and timeout policies.

### Part G: `commitSync` vs `commitAsync`

`commitSync`:

- blocks until broker responds
- easier correctness reasoning
- slower

`commitAsync`:

- non-blocking
- higher throughput
- callback must handle failures carefully
- older async commit responses can arrive after newer ones

Common pattern:

- use async commits during normal processing
- use sync commit on shutdown or partition revoke when needed

For beginner/intermediate interviews, manual `commitSync` after processing is the safest explanation.

### Part H: Cooperative Rebalancing

Classic eager rebalancing can revoke many partitions at once.

Cooperative rebalancing tries to reduce disruption by moving partitions incrementally.

Why it matters:

- less pause during deploys
- fewer duplicate windows
- smoother scaling
- better for stateful consumers

Still true:

- rebalances can happen
- consumers must be idempotent
- partition revocation still needs careful handling

### Part I: Static Membership

Static membership gives a consumer instance a stable identity.

Why useful:

- rolling restart does not always look like a brand-new member
- fewer unnecessary rebalances
- stateful consumers recover more smoothly

Risk:

- duplicate identity across two live instances is dangerous
- deployment automation must assign identities carefully

Interview sentence:

> Static membership reduces rebalance churn, but it requires disciplined instance identity management.

### Part J: AdminClient

AdminClient is for cluster and metadata operations.

Common uses:

- create topics
- describe topics
- describe configs
- alter configs
- list consumer groups
- describe consumer groups
- inspect offsets
- manage ACLs depending on tooling/setup
- trigger or inspect administrative workflows

Senior warning:

Admin APIs can change production behavior. They need guardrails, audit logs, least privilege, and often platform ownership.

Do not let random services create unlimited topics in production without policy.

### Part K: Rack Awareness

Rack awareness tries to place replicas across different failure domains.

Failure domains can be:

- rack
- availability zone
- data center room
- cloud zone

Why it matters:

If all replicas for a partition sit in the same rack/AZ, one rack/AZ failure can take out all copies.

Interview sentence:

> Replication factor protects only if replicas are placed across independent failure domains.

### Part L: Leader Imbalance and Preferred Leaders

Every partition has one leader.

If too many leaders sit on one broker:

- that broker handles more produce/fetch traffic
- latency increases
- other brokers are underused

Operational fix:

- preferred leader election
- partition reassignment
- broker balancing tooling
- capacity planning

Trap:

Replication distributes copies. Leadership distribution controls traffic load.

### Part M: Partition Reassignment

Partition reassignment moves replicas between brokers.

Use cases:

- add brokers
- remove brokers
- fix disk imbalance
- fix broker hot spots
- move data away from failing hardware

Risk:

- high network and disk I/O
- replication lag
- ISR shrink
- produce/fetch latency

Safe approach:

- throttle reassignment
- move in batches
- monitor ISR, disk, network, and latency
- avoid peak traffic windows

### Part N: Cross-Cluster Replication

Cross-cluster replication is used for:

- disaster recovery
- regional locality
- migrations
- data sharing

Common tooling:

- MirrorMaker-style replication
- managed Kafka replication features
- custom replication services in some companies

Senior caveats:

- offsets may not map cleanly without tooling
- duplicates can happen during failover
- ordering across regions is hard
- active-active requires conflict handling
- security and schema registry must be planned too

### Part O: Managed Kafka Caveats

Managed Kafka platforms can differ in:

- available configs
- quotas
- tiered storage behavior
- monitoring names
- ACL model
- networking model
- upgrade schedule
- supported Kafka version

Interview sentence:

> I would design using Kafka fundamentals, then verify exact knobs against the managed provider.

### Part P: Version-Specific Features

Some Kafka features are version-dependent.

Examples:

- KRaft metadata mode
- transaction protocol improvements
- tiered storage behavior
- share-consumer style capabilities

Rule:

Do not overclaim a feature unless you know the Kafka version and platform support.

---

## 6. What Problem It Solves

- Primary problem solved: controlled recovery, admin safety, and production-grade Kafka operations
- Secondary benefits: smoother deploys, safer replay, better placement, lower rebalance pain
- Systems impact: lets Kafka be run as a platform rather than only a library dependency

---

## 7. When To Rely On It

Use advanced consumer APIs when:

- you need targeted replay
- one partition needs backpressure
- replay should not affect normal consumer groups
- you are building repair/backfill tools

Use AdminClient when:

- you are building platform automation
- topic/config/group metadata must be inspected safely
- topic creation is governed

Use rack awareness and reassignment when:

- Kafka is business-critical
- multiple failure domains exist
- broker load is uneven
- cluster capacity is changing

Use cross-cluster replication when:

- RPO/RTO requires regional recovery
- migration needs low downtime
- another region needs a copy of events

---

## 8. When Not To Use It

Avoid advanced APIs when:

- normal consumer group behavior is enough
- replay can be done through a new consumer group
- operational risk is higher than benefit
- team does not have runbooks

Avoid AdminClient from business services when:

- topic creation should be governed
- service has no reason to change cluster configs
- least privilege matters

Avoid manual `assign` when:

- you actually need group scaling
- you want automatic rebalancing
- multiple instances should share partitions dynamically

---

## 9. Pros and Cons

| Concept | Pros | Cons |
|---|---|---|
| `seek` | precise replay | duplicate or skipped side effects if misused |
| `pause/resume` | partition-level backpressure | silent lag if not monitored |
| static membership | fewer rebalances | identity management complexity |
| cooperative rebalancing | smoother movement | still requires idempotent processing |
| AdminClient | automation and visibility | dangerous without guardrails |
| rack awareness | better failure isolation | placement/config complexity |
| cross-cluster replication | DR/migration | lag, duplicates, offset mapping complexity |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More control:
  Better recovery and operations, but more ways to make mistakes.
- Manual replay:
  Powerful for repair, but duplicate side effects must be handled.
- Static membership:
  Less rebalance churn, but deployment identity must be correct.
- Rack awareness:
  Better fault tolerance, but needs real failure-domain mapping.
- Cross-cluster replication:
  Better DR, but harder consistency and failover.

### Common Mistakes

- Mistake: "Use `assign` because it feels simpler."
  Why it is wrong: you lose group-managed scaling and rebalancing.
  Better approach: use `subscribe` for normal app consumers.

- Mistake: "Seek and replay in production without checking idempotency."
  Why it is wrong: duplicate side effects can corrupt business state.
  Better approach: replay with a plan, idempotency, and limited scope.

- Mistake: "Pause partitions and forget them."
  Why it is wrong: lag silently grows.
  Better approach: monitor paused partitions and resume with timeout policy.

- Mistake: "Let every service create topics."
  Why it is wrong: topic sprawl, bad configs, and security issues follow.
  Better approach: governed topic creation through platform automation.

- Mistake: "Replication factor 3 is enough even if all replicas are in one rack."
  Why it is wrong: one rack failure can still take all replicas.
  Better approach: place replicas across failure domains.

---

## 11. Key Numbers

Use these as reasoning anchors:

- Consumer group parallelism for one topic is capped by partition count.
- Static membership is valuable when restarts are frequent and rebalance cost is high.
- Replay batch size should be small enough to avoid overwhelming downstream systems.
- Reassignment should be throttled when cluster traffic is high.
- Cross-region replication lag must be compared against RPO.
- Paused partitions should have alerting if pause duration exceeds expected retry window.

---

## 12. Failure Modes

### Bad Replay

What fails:

- operator seeks consumer to old offset
- side effects are not idempotent

Impact:

- duplicate payments, emails, inventory updates, or analytics rows

Mitigation:

- replay with separate group
- idempotency keys
- dry-run mode where possible
- limit topic/partition/time window

### Forgotten Paused Partition

What fails:

- partition paused due to downstream issue
- no one resumes it

Impact:

- lag grows
- retention window may be missed

Mitigation:

- metric for paused partitions
- max pause duration
- alerting and ownership

### Unsafe Admin Automation

What fails:

- service creates topics with wrong partition count, retention, or replication

Impact:

- hot spots, data loss risk, cost spike

Mitigation:

- platform-owned topic templates
- config validation
- least-privilege AdminClient permissions
- audit logs

### Rack Failure With Bad Placement

What fails:

- all replicas for some partitions are in same failure domain

Impact:

- partitions unavailable or data at risk

Mitigation:

- rack awareness
- placement audits
- reassignment

### Cross-Region Failover Duplicate

What fails:

- failover reads replicated topic
- some events were already processed in old region

Impact:

- duplicate business effects

Mitigation:

- idempotent consumers
- event IDs
- failover runbook
- offset translation strategy

---

## 13. Scenario

- Product / system: Payment event platform
- Why this concept fits: payment pipelines need controlled replay, low rebalance disruption, safe admin operations, and strong failure-domain placement
- What would go wrong without it: a careless replay can double-process payments, a forgotten pause can miss retention, and bad replica placement can turn one zone failure into topic unavailability

---

## 14. Code Sample

AdminClient example: create a topic with explicit production-minded settings.

```java
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TopicAdminExample {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", "broker1:9092,broker2:9092,broker3:9092");

        try (AdminClient admin = AdminClient.create(props)) {
            NewTopic topic = new NewTopic("payment-events", 24, (short) 3)
                    .configs(Map.of(
                            "min.insync.replicas", "2",
                            "retention.ms", "604800000"
                    ));

            admin.createTopics(List.of(topic)).all().get();
        }
    }
}
```

Interview explanation:

- partitions are chosen intentionally
- replication factor is explicit
- `min.insync.replicas` supports durable producer writes
- retention is documented at creation
- in real production, this should be behind platform guardrails

---

## 15. Mini Program / Simulation

This simulation shows why rack-aware placement matters.

```python
def survives_rack_failure(replica_racks, failed_rack):
    surviving = [rack for rack in replica_racks if rack != failed_rack]
    return len(surviving) > 0


def main():
    bad_placement = ["rack-a", "rack-a", "rack-a"]
    good_placement = ["rack-a", "rack-b", "rack-c"]

    print("bad placement survives rack-a:", survives_rack_failure(bad_placement, "rack-a"))
    print("good placement survives rack-a:", survives_rack_failure(good_placement, "rack-a"))


if __name__ == "__main__":
    main()
```

Takeaway:

Replication count matters, but placement decides whether replicas survive the same failure.

---

## 16. Practical Question

> A consumer needs to replay one hour of payment events after a bug fix, but normal processing must continue. How would you do it safely?

---

## 17. Strong Answer

I would avoid disrupting the main consumer group. I would create a separate replay consumer group or controlled repair job, identify the exact topic, partitions, and timestamp range, and use offset lookup or `seek` to replay only the required window.

Before replay, I would confirm the downstream side effects are idempotent using `paymentId` or `eventId`. If side effects are risky, I would run in dry-run mode or write to a staging output first. I would throttle replay so it does not overload downstream systems and monitor lag, errors, DLQ, and duplicate-detection metrics.

I would not reset the main consumer group casually because that can duplicate or skip production work.

---

## 18. Revision Notes

- One-line summary: Advanced Kafka APIs give control over replay, backpressure, admin automation, placement, and failover, but they require guardrails.
- Three keywords: `seek`, AdminClient, rack awareness
- One interview trap: manual control is powerful, not automatically safer.
- One memory trick: advanced Kafka is about controlled recovery, not fancy APIs.

---

## 19. Official Source Notes

- Apache Kafka 4.3 documentation: <https://kafka.apache.org/43/>
- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka operations docs: <https://kafka.apache.org/43/operations/monitoring/>

