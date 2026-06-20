# Microservices Interview Track Index

Target: starter, intermediate, senior, and FAANG-level backend/system design preparation.

This folder is a complete microservices learning track. The goal is to understand not only
patterns, but when to use them, when to avoid them, how they fail in production, and how to
explain them clearly in interviews.

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 1 | `01-Starter-Path/Microservice-Design-Patterns-Interview-Master-Sheet.md` | Core pattern map and interview vocabulary |
| 2 | `01-Starter-Path/Microservices-Communication-API-Contracts-FAANG-Master-Sheet.md` | REST/gRPC/events, API versioning, contracts |
| 3 | `02-Intermediate-Path/Microservices-Data-Consistency-Transactions-FAANG-Master-Sheet.md` | DB ownership, joins, saga, outbox, consistency |
| 4 | `02-Intermediate-Path/Microservices-Event-Driven-Kafka-Messaging-FAANG-Master-Sheet.md` | Kafka, ordering, consumer lag, DLQ, schemas |
| 5 | `02-Intermediate-Path/Microservices-Resilience-Scalability-Capacity-FAANG-Master-Sheet.md` | timeout, retry, circuit breaker, scaling math |
| 6 | `03-Senior-Path/Microservices-Security-Zero-Trust-FAANG-Master-Sheet.md` | OAuth2, mTLS, service identity, secrets |
| 7 | `03-Senior-Path/Microservices-Observability-Operations-MultiRegion-FAANG-Master-Sheet.md` | logs, metrics, traces, incidents, DR, multi-region |
| 8 | `03-Senior-Path/Microservices-Testing-Governance-Migration-FAANG-Master-Sheet.md` | contract tests, rollout, strangler, governance |
| 9 | `04-FAANG-Platinum-Path/Microservices-DDD-Service-Decomposition-Boundaries-Platinum-Sheet.md` | service boundaries, DDD, data/team ownership |
| 10 | `04-FAANG-Platinum-Path/Microservices-Production-Debugging-Incident-Playbook-Platinum-Sheet.md` | incident response, production debugging, RCA |
| 11 | `04-FAANG-Platinum-Path/Microservices-Kubernetes-Service-Mesh-Platform-Readiness-Platinum-Sheet.md` | Kubernetes runtime, probes, mesh, platform readiness |

---

## 2. Level-Wise Learning Plan

### Starter Path

Focus:
- monolith vs microservices
- service decomposition
- sync vs async communication
- API Gateway
- database per service
- timeout, retry, circuit breaker
- basic logs, metrics, health checks

Starter goal:

```text
I can explain what microservices are, when not to use them, how services communicate,
and why each service should own its data.
```

### Intermediate Path

Add:
- Saga
- Outbox
- Idempotency
- DLQ
- CQRS/read models
- API versioning
- contract testing
- blue-green/canary deployment

Intermediate goal:

```text
I can design a realistic microservice workflow that handles failures, retries, eventual
consistency, and safe deployment.
```

### Senior Path

Add:
- service boundaries and bounded contexts
- DDD decomposition and ownership matrices
- schema evolution
- Kafka partitioning and ordering
- consumer lag and rebalancing
- read-your-writes UX
- compensation failure handling
- multi-region DR
- service identity and zero trust
- production debugging
- Kubernetes runtime behavior and rollout safety

Senior goal:

```text
I can reason about correctness, scale, security, observability, and operational ownership
across many independently deployed services.
```

### FAANG-Ready Path

For every topic, practice:
- why this pattern exists
- what breaks without it
- when not to use it
- failure modes
- latency, throughput, consistency, cost trade-offs
- testing strategy
- migration path
- strong 2-minute explanation

FAANG goal:

```text
I can design microservices like an owner: clear boundaries, safe data flows, resilient
communication, observable operations, secure identity, and controlled deployment risk.
```

---

## 3. End-To-End Capstone System

Use one capstone across the whole track:

```text
Hotel Booking Platform
```

Core services:
- Search Service
- Availability Service
- Booking Service
- Payment Service
- Notification Service
- Loyalty Service
- Pricing Service
- Reporting Service
- Identity/Auth Service

End-to-end flow:

```text
Search hotel -> check availability -> create pending booking -> reserve inventory
-> authorize payment -> confirm booking -> publish event -> notify guest -> award points
```

Patterns used:

| Problem | Pattern |
|---|---|
| Service boundaries | bounded context / business capability |
| External entry point | API Gateway / BFF |
| Immediate checks | sync REST/gRPC |
| Side effects | async events |
| Cross-service booking workflow | Saga |
| Reliable event publishing | Outbox / CDC |
| Duplicate retries | idempotency keys |
| Read-heavy search | CQRS / materialized view |
| Downstream failure | timeout, retry, circuit breaker, bulkhead |
| Poison messages | DLQ |
| Request debugging | correlation ID, logs, traces |
| Safe rollout | canary / blue-green / contract testing |
| Multi-region continuity | RTO/RPO, failover, data replication |

---

## 4. Interview Answer Formula

Use this formula for almost every microservices answer:

```text
1. State the business boundary or requirement.
2. Choose sync or async communication.
3. Explain data ownership.
4. Explain consistency model.
5. Add failure handling.
6. Add observability.
7. Add security.
8. Mention trade-offs and an alternative.
```

Example:

```text
For booking, I keep inventory ownership in Availability Service and booking lifecycle in
Booking Service. The user-facing reserve call can be synchronous, but downstream notification
and loyalty are async. Since the workflow spans services, I use Saga with compensation,
Outbox for reliable events, idempotency keys for retries, and traces/logs/metrics to debug
failures.
```

---

## 5. Gold Standard Audit Rubric

| Quality Bar | What It Means |
|---|---|
| Beginner clarity | Can explain the pattern in simple words |
| Design maturity | Can choose it based on requirements |
| Failure awareness | Can explain how it fails |
| Data correctness | Can protect business invariants |
| Communication clarity | Can choose REST/gRPC/events correctly |
| Observability | Can debug it in production |
| Security | Can protect identity, secrets, and access |
| Scalability | Can reason about limits and growth |
| Testing | Can verify contracts and workflows |
| Migration | Can evolve safely from current state |

Gold rule:

```text
A topic is gold-level only when the learner can explain definition, runtime flow, trade-offs,
failure modes, debugging, testing, security, and migration path.
```

---

## 6. Platinum Supplements

Use these after the core eight sheets:

| Supplement | Why It Matters |
|---|---|
| `04-FAANG-Platinum-Path/Microservices-DDD-Service-Decomposition-Boundaries-Platinum-Sheet.md` | Prevents wrong service splits and distributed monoliths |
| `04-FAANG-Platinum-Path/Microservices-Production-Debugging-Incident-Playbook-Platinum-Sheet.md` | Builds senior-level incident/debugging confidence |
| `04-FAANG-Platinum-Path/Microservices-Kubernetes-Service-Mesh-Platform-Readiness-Platinum-Sheet.md` | Connects design to real production runtime |

Platinum rule:

```text
A microservice answer becomes senior only when you can defend boundaries, runtime behavior,
failure handling, observability, security, and operational ownership.
```

---

## 7. Final Completeness Statement

This microservices track covers:
- communication
- database ownership
- consistency
- messaging
- resilience
- scale
- security
- observability
- deployment
- testing
- migration
- governance
- multi-region design
- DDD service decomposition
- production incident debugging
- Kubernetes and service mesh readiness

Optional future deep dives:
- event sourcing implementation details
- cloud-specific AWS/Azure/GCP microservice stacks

These are optional because the current track now covers the high-frequency interview surface
for backend, system design, senior ownership, and production operations rounds.
