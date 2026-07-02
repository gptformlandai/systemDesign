# Microservices Interview Track Index

Target: beginner-to-pro backend, distributed systems, cloud, platform, and FAANG-level system
design preparation.

This folder is a complete microservices learning track. The goal is to understand not only
patterns, but when to use them, when to avoid them, how they fail in production, how to run a
small system locally, and how to explain the trade-offs clearly in interviews.

The upgraded track now contains:

- setup and local development first-mile
- concept sheets for understanding
- implementation sheets for mechanics
- senior sheets for production ownership
- platinum sheets for architecture review and platform engineering
- a runnable local capstone lab
- practice upgrade sheets for recall, labs, mocks, scoring, and roadmaps

---

## 1. Recommended Study Order

| Order | Document | Why This Order |
|---:|---|---|
| 0 | `00-Setup/Microservices-Local-Dev-Docker-Compose-First-System-Gold-Sheet.md` | Gives beginners a local mental model for services, ports, config, logs, idempotency, outbox, and failure drills |
| 1 | `01-Starter-Path/Microservices-Distributed-Systems-Foundations-Gold-Sheet.md` | Builds the distributed-systems mental model before pattern vocabulary |
| 2 | `01-Starter-Path/Microservice-Design-Patterns-Interview-Master-Sheet.md` | Core pattern map and interview vocabulary |
| 3 | `01-Starter-Path/Microservices-Hotel-Booking-End-to-End-Walkthrough-Gold-Sheet.md` | Connects all patterns to one concrete capstone flow |
| 4 | `01-Starter-Path/Microservices-Communication-API-Contracts-FAANG-Master-Sheet.md` | REST/gRPC/events, API Gateway, BFF, API contracts |
| 5 | `02-Intermediate-Path/Microservices-API-Management-Partner-APIs-Webhooks-Gold-Sheet.md` | API product lifecycle, partner onboarding, quotas, developer portal, signed webhooks |
| 6 | `02-Intermediate-Path/Microservices-Data-Consistency-Transactions-FAANG-Master-Sheet.md` | DB ownership, joins, saga, outbox, consistency |
| 7 | `02-Intermediate-Path/Microservices-Saga-Outbox-Idempotency-Implementation-Gold-Sheet.md` | Tables, flows, retries, relay, idempotent consumers, workflow recovery |
| 8 | `02-Intermediate-Path/Microservices-Workflow-Engines-Temporal-Camunda-Durable-Execution-Gold-Sheet.md` | Durable execution, workflow engines, activity idempotency, workflow versioning |
| 9 | `02-Intermediate-Path/Microservices-Event-Driven-Kafka-Messaging-FAANG-Master-Sheet.md` | Kafka, ordering, consumer lag, DLQ, schemas |
| 10 | `02-Intermediate-Path/Microservices-Contracts-Testing-Schema-Evolution-Implementation-Gold-Sheet.md` | OpenAPI, contract tests, schema evolution, CDC implementation depth |
| 11 | `02-Intermediate-Path/Microservices-Resilience-Scalability-Capacity-FAANG-Master-Sheet.md` | timeout, retry, circuit breaker, backpressure, scaling math |
| 12 | `03-Senior-Path/Microservices-Security-Zero-Trust-FAANG-Master-Sheet.md` | OAuth2, JWT, mTLS, service identity, secrets |
| 13 | `03-Senior-Path/Microservices-Security-Service-Identity-Policy-Secrets-Deep-Dive-Gold-Sheet.md` | JWKS, policy-as-code, tenant isolation, audit, rotation incidents |
| 14 | `03-Senior-Path/Microservices-Observability-Operations-MultiRegion-FAANG-Master-Sheet.md` | logs, metrics, traces, incidents, DR, multi-region |
| 15 | `03-Senior-Path/Microservices-Observability-SLO-OpenTelemetry-Deep-Dive-Gold-Sheet.md` | OpenTelemetry, SLOs, burn-rate alerts, dashboards, chaos, load, cost |
| 16 | `03-Senior-Path/Microservices-Cloud-Managed-Architecture-AWS-EKS-ECS-Serverless-Gold-Sheet.md` | Cloud runtime choices, EKS/ECS/serverless, managed messaging, cloud failure modes |
| 17 | `03-Senior-Path/Microservices-FinOps-Cost-Capacity-Unit-Economics-Gold-Sheet.md` | Cost per booking/request, right-sizing, retry cost, observability cost, unit economics |
| 18 | `03-Senior-Path/Microservices-Privacy-Compliance-Data-Lifecycle-Gold-Sheet.md` | PII lifecycle, deletion, data residency, PCI boundaries, audit evidence |
| 19 | `03-Senior-Path/Microservices-Testing-Governance-Migration-FAANG-Master-Sheet.md` | contract tests, rollout, strangler, governance |
| 20 | `04-FAANG-Platinum-Path/Microservices-DDD-Service-Decomposition-Boundaries-Platinum-Sheet.md` | service boundaries, DDD, data/team ownership |
| 21 | `04-FAANG-Platinum-Path/Microservices-Production-Debugging-Incident-Playbook-Platinum-Sheet.md` | incident response, production debugging, RCA |
| 22 | `04-FAANG-Platinum-Path/Microservices-Kubernetes-Service-Mesh-Platform-Readiness-Platinum-Sheet.md` | Kubernetes runtime, probes, mesh, platform readiness |
| 23 | `04-FAANG-Platinum-Path/Microservices-Kubernetes-Advanced-Operations-Platinum-Sheet.md` | CPU throttling, OOM, PDBs, HPA limits, mesh retry amplification |
| 24 | `04-FAANG-Platinum-Path/Microservices-Platform-Engineering-Golden-Path-Platinum-Sheet.md` | Golden paths, service catalog, platform guardrails, maturity model |
| 25 | `04-FAANG-Platinum-Path/Microservices-Architecture-Review-Capstone-Case-Studies-Platinum-Sheet.md` | owner-level architecture review and case-study defense |
| 26 | `microservices-mastery-lab/README.md` | Runnable local capstone lab with gateway, booking, payment, idempotency, outbox, and worker |
| 27 | `05-Practice-Upgrade/Microservices-Active-Recall-Question-Bank.md` | Topic-by-topic retrieval practice |
| 28 | `05-Practice-Upgrade/Microservices-Scenario-Drill-Bank.md` | Interview scenario drills across design, incidents, security, platform |
| 29 | `05-Practice-Upgrade/Microservices-Design-Mini-Labs.md` | Hands-on design labs for outbox, idempotency, SLOs, contracts, Kubernetes, API management, privacy, and cost |
| 30 | `05-Practice-Upgrade/Microservices-Mock-Interview-Scripts.md` | Timed mock rounds with follow-up pressure |
| 31 | `05-Practice-Upgrade/Microservices-Interview-Scoring-Rubrics.md` | 1-5 rubrics for every major skill |
| 32 | `05-Practice-Upgrade/Microservices-2-Week-4-Week-Mastery-Roadmaps.md` | Realistic fast-track and mastery study plans |

---

## 2. Level-Wise Learning Plan

### Setup Layer

Focus:

- local service topology
- ports and config
- request ID propagation
- idempotency keys
- outbox and async worker basics
- seed data and failure drills
- local debugging habits

Setup goal:

```text
I can run a small local microservice system, send a request through it, replay a duplicate
request safely, and explain the logs, database rows, and outbox event.
```

Setup file:

| File | What It Builds |
|---|---|
| `00-Setup/Microservices-Local-Dev-Docker-Compose-First-System-Gold-Sheet.md` | first-mile local development and debugging mindset |

### Starter Path

Focus:

- microservices as distributed systems
- monolith vs modular monolith vs microservices
- method call vs network call
- service decomposition
- sync vs async communication
- API Gateway and BFF
- database per service
- timeout, retry, circuit breaker
- basic logs, metrics, health checks
- hotel booking capstone walkthrough

Starter goal:

```text
I can explain what microservices are, when not to use them, how services communicate,
why each service owns its data, and how one hotel booking request flows through the system.
```

Starter files:

| File | What It Builds |
|---|---|
| `01-Starter-Path/Microservices-Distributed-Systems-Foundations-Gold-Sheet.md` | network, failure, latency, ownership mental model |
| `01-Starter-Path/Microservice-Design-Patterns-Interview-Master-Sheet.md` | complete pattern vocabulary |
| `01-Starter-Path/Microservices-Hotel-Booking-End-to-End-Walkthrough-Gold-Sheet.md` | concrete capstone flow |
| `01-Starter-Path/Microservices-Communication-API-Contracts-FAANG-Master-Sheet.md` | communication choices and contracts |

### Intermediate Path

Add:

- partner API lifecycle and webhooks
- API keys, OAuth2/mTLS partner identity, quotas, developer portal
- Saga
- Outbox
- Idempotency
- durable workflow engine trade-offs
- DLQ and replay
- Kafka partitioning, ordering, lag, rebalancing
- CQRS/read models
- API compatibility and contract testing
- schema evolution and schema registry
- CDC/Debezium and outbox relay thinking
- timeout/retry/circuit breaker/backpressure/load shedding
- capacity and retry-amplification reasoning

Intermediate goal:

```text
I can design a realistic microservice workflow that handles failures, retries, duplicate
messages, eventual consistency, contract compatibility, API consumers, webhooks, workflow
state, and safe deployment.
```

Intermediate files:

| File | What It Builds |
|---|---|
| `02-Intermediate-Path/Microservices-API-Management-Partner-APIs-Webhooks-Gold-Sheet.md` | API product lifecycle and webhook reliability |
| `02-Intermediate-Path/Microservices-Data-Consistency-Transactions-FAANG-Master-Sheet.md` | consistency patterns and workflow correctness |
| `02-Intermediate-Path/Microservices-Saga-Outbox-Idempotency-Implementation-Gold-Sheet.md` | implementation-level reliability details |
| `02-Intermediate-Path/Microservices-Workflow-Engines-Temporal-Camunda-Durable-Execution-Gold-Sheet.md` | durable workflow judgment |
| `02-Intermediate-Path/Microservices-Event-Driven-Kafka-Messaging-FAANG-Master-Sheet.md` | event-driven Kafka design and operations |
| `02-Intermediate-Path/Microservices-Contracts-Testing-Schema-Evolution-Implementation-Gold-Sheet.md` | contract/schema/CDC/testing implementation depth |
| `02-Intermediate-Path/Microservices-Resilience-Scalability-Capacity-FAANG-Master-Sheet.md` | resilience and scaling judgment |

### Senior Path

Add:

- service boundaries and bounded contexts
- schema evolution governance
- OpenTelemetry and trace propagation
- SLI/SLO/error budget/burn-rate alerts
- incident response and runbooks
- multi-region DR and RTO/RPO
- cloud-managed runtime decisions
- FinOps and unit economics
- privacy/compliance data lifecycle
- zero trust, service identity, mTLS
- JWT/JWKS validation and key rotation
- secrets rotation and tenant isolation
- contract/component/integration testing strategy
- strangler migration and service ownership

Senior goal:

```text
I can reason about correctness, scale, security, observability, cloud runtime choice,
privacy, cost, and operational ownership across many independently deployed services.
```

Senior files:

| File | What It Builds |
|---|---|
| `03-Senior-Path/Microservices-Security-Zero-Trust-FAANG-Master-Sheet.md` | zero-trust security foundation |
| `03-Senior-Path/Microservices-Security-Service-Identity-Policy-Secrets-Deep-Dive-Gold-Sheet.md` | service identity, policy, secrets, audit depth |
| `03-Senior-Path/Microservices-Observability-Operations-MultiRegion-FAANG-Master-Sheet.md` | operations, incidents, DR, multi-region |
| `03-Senior-Path/Microservices-Observability-SLO-OpenTelemetry-Deep-Dive-Gold-Sheet.md` | instrumentation, SLOs, dashboards, burn alerts |
| `03-Senior-Path/Microservices-Cloud-Managed-Architecture-AWS-EKS-ECS-Serverless-Gold-Sheet.md` | cloud-managed architecture judgment |
| `03-Senior-Path/Microservices-FinOps-Cost-Capacity-Unit-Economics-Gold-Sheet.md` | cost-aware production ownership |
| `03-Senior-Path/Microservices-Privacy-Compliance-Data-Lifecycle-Gold-Sheet.md` | privacy, compliance, deletion, residency, audit |
| `03-Senior-Path/Microservices-Testing-Governance-Migration-FAANG-Master-Sheet.md` | testing, governance, migration |

### FAANG / Platinum Path

Add:

- DDD decomposition and service boundary scoring
- ownership matrix and boundary smells
- production incident playbooks
- Kubernetes probes, shutdown, resource sizing, autoscaling
- service mesh readiness and retry policy ownership
- CPU throttling, OOM, PDBs, topology spread, rollout metrics
- platform engineering golden paths
- service catalog, developer portal, maturity model
- full case studies and architecture review scorecards
- multi-region and migration trade-off defense

FAANG goal:

```text
I can design microservices like an owner: clear boundaries, safe data flows, resilient
communication, observable operations, secure identity, controlled deployment risk, cost
visibility, platform guardrails, and defensible migration paths.
```

Platinum files:

| File | What It Builds |
|---|---|
| `04-FAANG-Platinum-Path/Microservices-DDD-Service-Decomposition-Boundaries-Platinum-Sheet.md` | boundary and DDD judgment |
| `04-FAANG-Platinum-Path/Microservices-Production-Debugging-Incident-Playbook-Platinum-Sheet.md` | incident/debugging confidence |
| `04-FAANG-Platinum-Path/Microservices-Kubernetes-Service-Mesh-Platform-Readiness-Platinum-Sheet.md` | runtime/platform readiness |
| `04-FAANG-Platinum-Path/Microservices-Kubernetes-Advanced-Operations-Platinum-Sheet.md` | advanced Kubernetes failure modes |
| `04-FAANG-Platinum-Path/Microservices-Platform-Engineering-Golden-Path-Platinum-Sheet.md` | platform engineering and golden path strategy |
| `04-FAANG-Platinum-Path/Microservices-Architecture-Review-Capstone-Case-Studies-Platinum-Sheet.md` | architecture owner-level review |

### Runnable Capstone Lab

Use this after the starter and intermediate sheets.

| File | What It Builds |
|---|---|
| `microservices-mastery-lab/README.md` | how to run and learn from the local capstone |
| `microservices-mastery-lab/booking_platform_simulation.py` | runnable gateway, booking, payment, outbox, worker simulation |

Lab goal:

```text
I can run a local booking flow, observe request ID propagation, test idempotency replay,
simulate payment timeout, and explain why outbox and async side effects exist.
```

### Practice Upgrade Path

Use these after or alongside the concept sheets. They convert the track from passive reading
into active recall, design drills, timed mocks, and measurable readiness.

Practice goal:

```text
I can answer from memory, draw the architecture, run the local lab, debug incidents, defend
trade-offs, and score my readiness honestly before the interview.
```

Practice files:

| File | What It Builds |
|---|---|
| `05-Practice-Upgrade/Microservices-Active-Recall-Question-Bank.md` | topic-by-topic recall questions |
| `05-Practice-Upgrade/Microservices-Scenario-Drill-Bank.md` | scenario-based design/debug/security/platform drills |
| `05-Practice-Upgrade/Microservices-Design-Mini-Labs.md` | hands-on diagrams, SQL tables, dashboards, runbooks, privacy/cost/platform drills |
| `05-Practice-Upgrade/Microservices-Mock-Interview-Scripts.md` | timed mock rounds with follow-ups |
| `05-Practice-Upgrade/Microservices-Interview-Scoring-Rubrics.md` | 1-5 rubrics for every major skill |
| `05-Practice-Upgrade/Microservices-2-Week-4-Week-Mastery-Roadmaps.md` | study plans and readiness gates |

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
- Partner API / Webhook Delivery
- Platform / Service Catalog

End-to-end flow:

```text
Search hotel -> check availability -> create pending booking -> reserve inventory
-> authorize payment -> confirm booking -> publish event -> notify guest -> award points
-> send partner webhook -> update reporting/read models
```

Patterns used:

| Problem | Pattern |
|---|---|
| Service boundaries | bounded context / business capability |
| External entry point | API Gateway / BFF |
| Partner integration | API management / developer portal / quotas |
| Immediate checks | sync REST/gRPC |
| Side effects | async events |
| Cross-service booking workflow | Saga or workflow engine |
| Reliable event publishing | Outbox / CDC |
| Duplicate retries | idempotency keys |
| Read-heavy search | CQRS / materialized view |
| Downstream failure | timeout, retry, circuit breaker, bulkhead |
| Poison messages | DLQ |
| Request debugging | correlation ID, logs, traces |
| User-impacting reliability | SLI, SLO, error budget, burn-rate alert |
| Service-to-service trust | mTLS / workload identity / network policy |
| Privacy and compliance | data minimization / retention / audit |
| Cost ownership | cost per booking / cost per request |
| Safe rollout | canary / blue-green / contract testing |
| Platform scaling | golden path / service catalog / guardrails |
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
7. Add security and privacy.
8. Mention testing and deployment safety.
9. Mention cost and platform ownership where relevant.
10. Mention trade-offs and an alternative.
```

Example:

```text
For booking, I keep inventory ownership in Availability Service and booking lifecycle in
Booking Service. The user-facing reserve call can be synchronous, but downstream notification,
loyalty, partner webhooks, and reporting are async. Since the workflow spans services, I use
Saga or a workflow engine depending complexity, Outbox for reliable events, idempotency keys
for retries, traces/logs/metrics to debug failures, service-level authorization, data
minimization for events, cost tags, and contract tests/canary rollout to deploy safely.
```

---

## 5. Gold Standard Audit Rubric

| Quality Bar | What It Means |
|---|---|
| Beginner clarity | Can explain the pattern in simple words |
| Local confidence | Can run and debug a small local system |
| Design maturity | Can choose it based on requirements |
| Failure awareness | Can explain how it fails |
| Data correctness | Can protect business invariants |
| Communication clarity | Can choose REST/gRPC/events/webhooks correctly |
| Observability | Can debug it in production |
| Security | Can protect identity, secrets, and access |
| Privacy/compliance | Can handle PII, retention, deletion, residency, and audit |
| Scalability | Can reason about limits and growth |
| Cost | Can reason about unit economics and cost drivers |
| Testing | Can verify contracts and workflows |
| Migration | Can evolve safely from current state |
| Platform readiness | Can explain Kubernetes/runtime/platform behavior |
| Practice readiness | Can answer under timed follow-up pressure |

Gold rule:

```text
A topic is gold-level only when the learner can explain definition, runtime flow, trade-offs,
failure modes, debugging, testing, security, privacy, migration path, cost impact, and at
least one concrete hotel booking example.
```

---

## 6. Adjacent Tracks

Use these tracks for deeper implementation:

| Track | Use It For |
|---|---|
| `Kafka/Kafka-Interview-Track-Index.md` | Kafka internals, schema registry, transactions, operations |
| `gRPC/gRPC-Mastery-Track-Index.md` | protobuf, deadlines, streaming, gRPC deployment/security |
| `GraphQL/GraphQL-Mastery-Track-Index.md` | GraphQL gateway, BFF, federation, schema governance |
| `Docker/Docker-Mastery-Track-Index.md` | Dockerfiles, Compose, image security, local containers |
| `K8-e2e/K8s-Interview-Track-Index.md` | Kubernetes fundamentals, operations, service mesh |
| `AWS/AWS-Interview-Track-Index.md` | AWS API Gateway, Lambda, ECS/EKS, messaging, observability |
| `Terraform/Terraform-Mastery-Track-Index.md` | infrastructure as code for platform/runtime |
| `Datadog/Datadog-Mastery-Track-Index.md` | observability, APM, SLOs, dashboards, cost/cardinality |
| `Marriot/SpringBoot/Spring-Boot-Interview-Track-Index.md` | Spring Boot service implementation and Spring Cloud |

---

## 7. Final Completeness Statement

This microservices track now covers:

- local development and first runnable system
- distributed-systems foundations
- communication
- API management and webhooks
- database ownership
- consistency
- saga/outbox/idempotency implementation
- durable workflow engine judgment
- messaging and Kafka
- contract testing and schema evolution
- CDC/Debezium and workflow-engine awareness
- resilience
- scale and capacity reasoning
- cloud-managed architecture decisions
- FinOps and unit economics
- privacy, compliance, deletion, residency, and audit
- security and zero trust
- service identity, JWT/JWKS, policy, secrets, tenant isolation
- observability, OpenTelemetry, SLOs, burn-rate alerts
- deployment
- testing
- migration
- governance
- multi-region design
- DDD service decomposition
- production incident debugging
- Kubernetes and service mesh readiness
- advanced Kubernetes operations
- platform engineering golden paths
- architecture review case studies
- runnable capstone lab
- active recall
- scenario drills
- design mini-labs
- mock interviews
- scoring rubrics
- 2-week and 4-week mastery roadmaps

Final standard:

```text
The track is no longer only a concept overview. It is now a full learner guide: read,
run, retrieve, design, debug, defend, score, and retest.
```
