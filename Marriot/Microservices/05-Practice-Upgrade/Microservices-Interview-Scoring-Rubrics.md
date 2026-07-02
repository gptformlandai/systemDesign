# Microservices Interview Scoring Rubrics

> Track: Microservices Interview Track - Group 5 Practice Upgrade  
> Goal: score answers honestly and identify what to improve before interviews.

Use this after mock interviews, scenario drills, and mini-labs.

---

## 1. Global 1 To 5 Scale

| Score | Quality |
|---:|---|
| 1 | vague, memorized, incorrect, pattern-name answer |
| 2 | basic definition, misses trade-offs and failure modes |
| 3 | correct concept and simple example, limited production maturity |
| 4 | senior-ready: mechanics, trade-offs, failures, observability, security |
| 5 | FAANG/platinum: owner-level design, migration, testing, operations, cost, follow-up depth |

Rule:

```text
A 5 is not a longer answer. It is a clearer answer that includes the right constraints and
trade-offs under pressure.
```

---

## 2. Concept Explanation Rubric

Use for questions like:

```text
What is saga? What is outbox? What is circuit breaker? What is service mesh?
```

| Score | Description |
|---:|---|
| 1 | cannot define it or gives wrong definition |
| 2 | definition only |
| 3 | definition + example + basic trade-off |
| 4 | explains why it exists, how it works, failure mode, when not to use |
| 5 | connects to production operations, testing, observability, migration, and alternatives |

Checklist for score 4+:

- crisp definition
- problem it solves
- mechanism/flow
- example from hotel booking
- failure mode
- trade-off
- strong closing line

---

## 3. Service Boundary Rubric

Use for decomposition questions.

| Score | Description |
|---:|---|
| 1 | splits by tables/entities only |
| 2 | names common services but cannot defend boundaries |
| 3 | uses business capabilities and basic data ownership |
| 4 | defends boundaries with invariants, teams, coupling, and transaction needs |
| 5 | also discusses modular monolith first, migration path, ownership matrix, and boundary smells |

Score 5 answer includes:

- business capability
- bounded context
- owned data
- invariant
- team ownership
- deployment independence
- coupling analysis
- when not to split

Red flags:

- RoomService as generic CRUD service without domain ownership
- shared database as final design
- every feature crosses every service
- no owner/on-call thinking

---

## 4. Data Consistency Rubric

Use for saga/outbox/idempotency/double-booking questions.

| Score | Description |
|---:|---|
| 1 | says "use Kafka" or "eventual consistency" vaguely |
| 2 | knows saga but not local transaction boundaries |
| 3 | explains saga and compensation at high level |
| 4 | includes owning service, local transaction, outbox, idempotency, DLQ, compensation failure |
| 5 | includes tables, retries, unknown payment state, reconciliation, monitoring, manual repair |

Score 4+ must mention:

- strong consistency inside owning service
- no cross-service DB writes
- saga style choice
- outbox for reliable publish
- idempotent commands and consumers
- duplicate/retry handling
- compensation failure path

---

## 5. Communication And Contract Rubric

Use for REST/gRPC/events/API versioning questions.

| Score | Description |
|---:|---|
| 1 | chooses technology by popularity |
| 2 | basic REST/gRPC/events definitions |
| 3 | chooses based on immediate response vs async fan-out |
| 4 | includes latency, failure, schema, compatibility, contract tests, observability |
| 5 | includes rollout/deprecation, semantic compatibility, BFF/API composition, operational contracts |

Score 5 answer includes:

- sync vs async decision
- user-critical path vs side effect
- backward compatibility
- consumer-driven contracts
- schema evolution
- fan-out control
- versioning trade-off

---

## 6. Kafka/Event-Driven Rubric

| Score | Description |
|---:|---|
| 1 | knows Kafka name only |
| 2 | basic topic/partition/consumer group definitions |
| 3 | explains ordering, offset, at-least-once |
| 4 | handles lag, retries, DLQ, schema registry, idempotent consumers |
| 5 | adds key design, hot partitions, replay, event age SLO, operational dashboards |

Score 4+ must mention:

- order within partition only
- key choice as correctness decision
- offset commit timing
- at-least-once duplicates
- DLQ and replay safety
- consumer lag debug path

---

## 7. Resilience And Scale Rubric

| Score | Description |
|---:|---|
| 1 | says "add more servers" |
| 2 | knows timeout/retry/circuit breaker definitions |
| 3 | applies basic patterns to downstream failure |
| 4 | includes retry budgets, backoff/jitter, bulkheads, load shedding, p99, saturation |
| 5 | performs capacity reasoning, detects bottlenecks, avoids retry storms, ties to SLO/cost |

Score 5 answer includes:

- p95/p99 thinking
- dependency budget
- connection pool limits
- downstream quota
- retry amplification
- backpressure strategy
- autoscaling limitations

---

## 8. Observability And Incident Rubric

| Score | Description |
|---:|---|
| 1 | says "check logs" only |
| 2 | knows logs/metrics/traces definitions |
| 3 | uses metrics and traces for simple debugging |
| 4 | follows incident flow with SLOs, dashboards, runbooks, deploy annotations |
| 5 | includes burn-rate alerts, async metrics, mitigation first, postmortem quality, cost/privacy |

Score 4+ must mention:

- user-impacting metric
- scope/region/version
- metrics detect, traces locate, logs explain
- saturation and dependency metrics
- rollback/canary
- runbook and incident roles

---

## 9. Security Rubric

| Score | Description |
|---:|---|
| 1 | says "use JWT" only |
| 2 | distinguishes auth/authz |
| 3 | explains gateway auth and service permissions |
| 4 | includes zero trust, service identity, mTLS, secrets, network policy, audit |
| 5 | adds JWKS rotation, tenant isolation, policy-as-code, incident response, compliance/PII |

Score 5 answer includes:

- gateway is not enough
- JWT validation details
- service-to-service identity
- business authorization
- least privilege
- secret rotation
- audit logs
- tenant isolation
- data protection

---

## 10. Kubernetes/Platform Rubric

| Score | Description |
|---:|---|
| 1 | knows pod/deployment names only |
| 2 | explains basic pod/service/deployment |
| 3 | explains probes, config, autoscaling basics |
| 4 | includes graceful shutdown, requests/limits, PDB, canary, service mesh trade-offs |
| 5 | debugs CPU throttling/OOM/HPA limits, topology spread, mesh retry amplification, rollout metrics |

Score 4+ must mention:

- readiness vs liveness
- graceful shutdown
- resource sizing
- autoscaling by bottleneck
- version-labeled canary metrics
- platform vs application ownership

---

## 11. API Management And Webhook Rubric

Use for partner API, external API, API lifecycle, and webhook questions.

| Score | Description |
|---:|---|
| 1 | exposes internal service directly |
| 2 | mentions gateway and API key only |
| 3 | includes docs, auth, and basic rate limits |
| 4 | includes quotas, developer portal, idempotency, analytics, deprecation, signed webhooks |
| 5 | adds partner onboarding, sandbox, support lifecycle, webhook DLQ/retry, SLA, abuse/cost controls |

Score 4+ must mention:

- API as product surface
- consumer onboarding
- authentication plus authorization
- quotas and rate limits
- idempotency for writes
- signed webhooks with event IDs
- versioning and deprecation
- partner-specific observability

---

## 12. Workflow Engine Rubric

Use for Temporal/Camunda/durable execution questions.

| Score | Description |
|---:|---|
| 1 | says "use Temporal" without reason |
| 2 | knows workflow engine coordinates steps |
| 3 | compares choreography vs orchestration |
| 4 | includes durable timers, retries, activity idempotency, state visibility, compensation |
| 5 | adds workflow versioning, replay risk, stuck workflow alerts, worker scaling, domain ownership boundaries |

Score 4+ must mention:

- why hand-rolled Saga may be enough
- workflow engine operational cost
- side effects in activities
- idempotency keys per activity
- workflow state/history
- versioning of long-running workflows

---

## 13. Cloud, Cost, Privacy, And Platform Rubric

Use for senior ownership questions that go beyond core patterns.

| Area | 3 | 4 | 5 |
|---|---|---|---|
| Cloud runtime | names EKS/Lambda | chooses runtime by workload | includes quotas, cold starts, IAM, observability, cost |
| Managed messaging | names Kafka/SQS | maps replay/order/fan-out | includes retention, DLQ, cost, schema, failure semantics |
| FinOps | mentions cost | tracks service cost | defines cost per booking/request and optimizes without breaking SLO |
| Privacy | says encrypt PII | classifies/minimizes data | handles deletion, derived stores, residency, audit evidence |
| Platform | says standardize | uses templates/catalog | designs golden path, guardrails, maturity model, escape hatch |

Score 5 answer includes:

- workload-to-runtime decision
- data residency and privacy impact
- cost tags and unit economics
- platform vs app ownership
- service catalog and automated guardrails
- trade-offs and when not to add platform complexity

---

## 14. Full System Design Rubric

Use for hotel booking platform capstone.

| Area | 1 | 3 | 5 |
|---|---|---|---|
| Requirements | jumps to design | basic functional/non-functional | clarifies scale, correctness, regions, SLOs |
| Boundaries | entity split | common service list | capability + invariant + team ownership |
| Data | shared DB | basic DB per service | ownership, read models, migration/reconciliation |
| Workflow | vague calls | saga mentioned | full saga/outbox/idempotency/compensation |
| Events | Kafka named | topics and consumers | schema, keys, lag, DLQ, replay, idempotency |
| Resilience | retries | timeouts/circuit breaker | budgets, backpressure, degradation, capacity |
| Observability | logs | metrics/traces | SLO, dashboards, runbooks, async health, incidents |
| Security | auth only | JWT/gateway | zero trust, service auth, secrets, audit, tenant |
| Privacy | ignored | basic PII masking | deletion lifecycle, derived stores, residency, audit evidence |
| Cost | ignored | basic capacity | unit economics, cost tags, retry/fan-out/observability cost |
| Cloud runtime | vague cloud | chooses containers/serverless | maps workload to runtime, quotas, IAM, rollout, observability |
| API management | gateway only | docs/rate limit | partner lifecycle, quotas, webhooks, analytics, deprecation |
| Platform | teams figure it out | service catalog | golden path, guardrails, maturity, ownership split |
| Deployment | deploy services | canary/rollback | compatibility gates, migrations, version metrics |
| Trade-offs | none | some trade-offs | clear alternatives and when not to use patterns |

Passing senior score:

```text
Mostly 4s, no area below 3.
```

FAANG/platinum score:

```text
Multiple 5s, especially in boundaries, data consistency, failure handling, and operations.
```

---

## 15. Readiness Gates

### Starter Ready

You can:

- explain microservices vs monolith
- define database per service
- explain REST vs events
- name gateway, service discovery, circuit breaker
- draw simple hotel booking flow
- run the local capstone happy path

### Intermediate Ready

You can:

- design saga/outbox/idempotency
- design partner API idempotency and webhook retry
- decide when workflow engine is useful
- explain Kafka partitioning and lag
- handle retries/DLQ/replay
- evolve API/event contracts safely
- explain testing pyramid and contract tests

### Senior Ready

You can:

- debug p99 incidents
- define SLOs and dashboards
- secure internal services with zero trust
- choose cloud runtime based on workload
- explain cost per booking/request
- design privacy deletion lifecycle
- design migration from monolith
- explain Kubernetes runtime failures

### Platinum Ready

You can:

- defend service boundaries under critique
- handle multi-region consistency trade-offs
- review architecture with scoring rubric
- design rollback/migration/reconciliation paths
- connect every design choice to ownership and operations
- design platform golden path and guardrails

---

## 16. Common Score Inflation Mistakes

Do not give yourself a 4 or 5 if:

- you named a pattern but could not explain failure handling
- you skipped data ownership
- you skipped observability
- you skipped security for sensitive flows
- you skipped privacy for PII flows
- you skipped cost for high-scale flows
- you skipped API lifecycle for external consumers
- you skipped workflow versioning for long-running workflows
- you skipped platform ownership for many-service organizations
- you ignored deployment/migration risk
- you could not answer follow-up questions
- you did not mention trade-offs

Memory line:

```text
Senior answers are measured by failure handling, not by pattern vocabulary.
```
