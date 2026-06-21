# Kafka Interview Scoring Rubrics

> Track: Kafka Interview Track - Practice Upgrade  
> Goal: make Kafka readiness measurable.

Use this after every scenario, mock, or mini-lab.

---

## 1. Score Scale

| Score | Meaning |
|---|---|
| 1 | fragmented recall, unsafe claims, little production reasoning |
| 2 | basic concept recall but weak failure/trade-off explanation |
| 3 | solid mid-level answer with some senior gaps |
| 4 | strong senior answer with correct boundaries and operations |
| 5 | MAANG-level answer: precise, structured, production-aware, trade-off fluent |

Passing target:

- mid-level: mostly 3s
- senior backend: mostly 4s
- MAANG senior/platform: consistent 4s with multiple 5s

---

## 2. Universal Kafka Rubric

| Dimension | 1 | 3 | 5 |
|---|---|---|---|
| Data flow | cannot explain producer-to-consumer path | explains topics, partitions, groups | explains leaders, offsets, groups, fan-out, ordering boundary clearly |
| Correctness | overclaims guarantees | explains at-least-once and idempotency | precisely names duplicate/loss windows and mitigation |
| Design | chooses topics/keys vaguely | reasonable topic/key design | ties topics, keys, partitions, schema, retention, and consumers to requirements |
| Operations | little monitoring | mentions lag/errors | gives triage flow, metrics, mitigation, and prevention |
| Security | ignores sensitive data | mentions ACLs | covers classification, PII minimization, ACLs, retention, DLQ/replay audit |
| Trade-offs | one-size answer | names some pros/cons | explains alternatives and why chosen design fits constraints |

---

## 3. Fundamentals Rubric

5-point answer includes:

- Kafka as partitioned durable log
- topic, partition, offset, broker, leader
- consumer group assignment
- fan-out via groups
- ordering per partition only
- scaling limited by partitions in a group
- rebalances and lag explained

Common deductions:

| Issue | Deduct |
|---|---|
| says Kafka is just a queue | -1 |
| misses per-partition ordering boundary | -2 |
| confuses topic and partition | -1 |
| says all consumers in same group read all messages | -2 |

---

## 4. Producer Rubric

5-point answer includes:

- `acks=all` and `min.insync.replicas`
- idempotence and retries
- delivery timeout
- batching/compression trade-offs
- key/partition strategy
- producer metrics
- timeout ambiguity after broker write

Common deductions:

| Issue | Deduct |
|---|---|
| uses `acks=0` for critical data without naming loss risk | -2 |
| ignores idempotence | -1 |
| says retries are always safe | -1 |
| no key strategy | -1 |

---

## 5. Consumer Rubric

5-point answer includes:

- poll loop
- manual commits for controlled processing
- commit after successful side effect for at-least-once
- idempotent consumer design
- rebalance handling
- pause/resume/seek when appropriate
- lag and error monitoring

Common deductions:

| Issue | Deduct |
|---|---|
| commits before side effect while claiming no loss | -2 |
| ignores duplicate window | -2 |
| no idempotency mechanism | -1 |
| no rebalance awareness | -1 |

---

## 6. Exactly-Once Rubric

5-point answer includes:

- distinguishes idempotent producer from transactions
- explains transactional consume-transform-produce
- covers `transactional.id`, fencing, `read_committed`
- states external side effects are outside Kafka transaction boundary
- proposes outbox/inbox/idempotency for DB/API effects

Common deductions:

| Issue | Deduct |
|---|---|
| says Kafka makes everything exactly-once | -3 |
| misses external side effects boundary | -2 |
| cannot explain fencing | -1 |
| ignores idempotent consumer | -1 |

---

## 7. Schema And Event Design Rubric

5-point answer includes:

- event envelope and event id
- schema registry
- compatibility mode
- safe additive changes
- semantic compatibility
- PII minimization
- versioning/deprecation strategy

Common deductions:

| Issue | Deduct |
|---|---|
| no schema registry for shared production events | -1 |
| treats type compatibility as full safety | -1 |
| includes raw PII broadly | -2 |
| no evolution plan | -1 |

---

## 8. Topic Design Rubric

5-point answer includes:

- domain-aligned topics
- key chosen from ordering/access needs
- partition count based on throughput and growth
- retention/compaction decision
- DLQ/retry topics
- hot-partition risk
- storage estimate

Common deductions:

| Issue | Deduct |
|---|---|
| key choice unrelated to ordering | -1 |
| says increase partitions has no side effects | -2 |
| uses compaction for event history without explaining loss of history | -1 |
| no retention decision | -1 |

---

## 9. Operations Rubric

5-point answer includes:

- symptom and blast radius
- topic/group/partition view
- producer, consumer, broker, and downstream metrics
- likely hypotheses
- mitigation plan
- prevention actions
- runbook clarity

Common deductions:

| Issue | Deduct |
|---|---|
| only says scale consumers | -1 |
| ignores partition-level lag | -1 |
| ignores downstream dependency | -1 |
| no mitigation order | -1 |
| no prevention/alerting | -1 |

---

## 10. Streams Connect CDC Rubric

5-point answer includes:

- chooses Streams, Connect, CDC, or plain app appropriately
- explains outbox for DB consistency
- explains state stores/changelogs for Streams
- handles connector failures and DLQs
- covers schema and idempotency

Common deductions:

| Issue | Deduct |
|---|---|
| uses CDC as public event contract without caution | -1 |
| ignores state recovery | -1 |
| cannot explain outbox | -2 |
| no connector operational story | -1 |

---

## 11. Platform Rubric

5-point answer includes:

- KRaft/controller quorum awareness
- capacity and partition planning
- quota model
- rack awareness/failure domains
- partition reassignment runbook
- tiered storage trade-offs
- managed Kafka caveats
- DR test plan

Common deductions:

| Issue | Deduct |
|---|---|
| ignores metadata/controller health | -1 |
| no noisy-neighbor controls | -1 |
| treats tiered storage as free infinite storage | -1 |
| active-active without conflict/idempotency plan | -2 |

---

## 12. Security Governance Rubric

5-point answer includes:

- topic owner and data classification
- PII minimization
- least-privilege ACLs
- schema governance
- DLQ access/retention control
- replay approval and audit
- tenant isolation beyond tenant id field

Common deductions:

| Issue | Deduct |
|---|---|
| raw sensitive data in broad topic | -2 |
| wildcard ACLs for services | -2 |
| ignores DLQ sensitivity | -1 |
| tenant_id used as only isolation | -2 |
| no replay governance | -1 |

---

## 13. Capstone Rubric

A 5-point MAANG capstone answer includes:

1. Requirements: scale, latency, correctness, compliance, DR.
2. Event model: domains, schemas, ownership, versioning.
3. Topic design: keys, partitions, retention, compaction, DLQ.
4. Correctness: outbox, idempotency, transactions where useful.
5. Processing: consumers, Streams, Connect, CDC choices.
6. Operations: lag, broker health, schema, DLQ, replay dashboards.
7. Security: ACLs, PII, tenant isolation, audit.
8. Reliability: broker failure, DR, failover, replay, backfill.
9. Trade-offs: alternative designs and limits.
10. Communication: clear structure and no guarantee overclaims.

---

## 14. Readiness Matrix

| Area | Target Score |
|---|---|
| Fundamentals | 5 |
| Producer/consumer delivery | 4-5 |
| Exactly-once/idempotency | 4-5 |
| Schema/topic design | 4-5 |
| Operations/debugging | 4-5 |
| Streams/Connect/CDC | 4 |
| Platform operations | 4 |
| Security/governance | 4 |
| Capstone design | 4-5 |

Final readiness rule:

```text
You are not ready for senior Kafka interviews until you can explain what Kafka guarantees,
what the application must still guarantee, and how operations/security validate those claims.
```
