# Microservices Active Recall Question Bank

> Track: Microservices Interview Track - Group 5 Practice Upgrade  
> Mode: answer from memory, then verify in source sheets.

Goal: convert reading into recall. Do not read passively.

---

## 1. How To Use This Bank

Rules:

1. Answer out loud before checking notes.
2. Keep most answers under 90 seconds.
3. For design questions, draw a quick flow.
4. Mark each answer as Green, Yellow, or Red.
5. Revisit Red after 24 hours and again after 7 days.
6. A topic is mastered only when you can answer with a definition, trade-off, failure mode,
   and production example.

Difficulty tiers:

| Tier | Meaning |
|---|---|
| Foundation | must answer without hesitation |
| Intermediate | explain mechanics and trade-offs |
| Senior | reason through failure, scale, security, and operations |
| Platinum | defend architecture under follow-up pressure |

---

## 2. Distributed Systems Foundations

### Foundation

1. Why is a microservice call not the same as a method call?
2. What are the five major costs introduced by microservices?
3. When is a modular monolith better than microservices?
4. What does database ownership mean?
5. What is a distributed monolith?
6. Why is fan-out dangerous for p99 latency?
7. What is the difference between strong and eventual consistency?
8. What does CAP theorem actually say in plain language?
9. Why do microservices require correlation IDs?
10. What makes a service boundary real?

### Intermediate

1. Explain how service ownership differs from code ownership.
2. Give three examples of operations where eventual consistency is acceptable.
3. Give three examples where strong consistency is required.
4. Why can retries create outages?
5. Explain latency budgeting for a checkout flow.
6. What should a service dashboard include?
7. Why is a shared database sometimes accepted temporarily but still risky?
8. How do network failure and process failure differ?
9. What does independent deployability require beyond separate repositories?
10. How do team boundaries influence service boundaries?

### Senior

1. A design has 12 services but every feature requires all teams to coordinate. Diagnose it.
2. A page calls 15 downstream services synchronously. What can go wrong and how do you fix it?
3. A team wants active-active inventory in two regions. What questions do you ask first?
4. Explain why microservices are a socio-technical architecture.
5. How would you decide whether to split Payment Service from a monolith?

---

## 3. Service Decomposition And DDD

### Foundation

1. What is a business capability?
2. What is a bounded context?
3. Why is service-per-table a bad split?
4. What is an anti-corruption layer?
5. What is a domain event?
6. What is an ownership matrix?
7. What is semantic coupling?
8. Why should strong invariants influence boundaries?
9. What does "same word, different meaning" reveal in DDD?
10. When should you not split a service?

### Intermediate

1. Split a hotel booking platform into services and justify each boundary.
2. Explain why Search and Availability may have different definitions of "Room".
3. What data should Booking Service own vs Availability Service own?
4. How do runtime, data, deployment, semantic, and team coupling differ?
5. Give an example of a good modular-monolith-first answer.
6. How do you identify a boundary smell?
7. What makes a service too small?
8. What makes a service too large?
9. How do domain events support loose coupling?
10. Why should Reporting not own operational business truth?

### Senior

1. An interviewer proposes UserService, BookingService, PaymentService, RoomService, and StatusService. Critique the split.
2. A service owns no data and only forwards calls. Is it a microservice? Explain.
3. A capability needs one atomic transaction with another service for every operation. What does that imply?
4. How do you migrate a module into a service safely?
5. How do you prevent a shared library from becoming hidden semantic coupling?

---

## 4. Communication And API Contracts

### Foundation

1. REST vs gRPC: when would you choose each?
2. Synchronous vs asynchronous communication: what is the difference?
3. What is an API Gateway?
4. What is a BFF?
5. What is service discovery?
6. What is load balancing?
7. What is API versioning?
8. What is backward compatibility?
9. What is contract testing?
10. What is a correlation ID?

### Intermediate

1. Why are events good for fan-out?
2. Why are events harder to debug?
3. Explain fan-out and tail latency.
4. What changes are safe in a REST response?
5. What changes are breaking in an API contract?
6. How does consumer-driven contract testing work?
7. What is schema evolution?
8. Why should events be named in past tense?
9. How do you version an event without breaking consumers?
10. What belongs in a gateway and what does not?

### Senior

1. A provider removes a field and all provider tests pass, but a consumer breaks. What was missing?
2. A mobile app needs a custom response shape. Gateway, BFF, or domain service change?
3. A service has a 100 ms budget and calls five downstream services. How do you reason about it?
4. How would you roll out a breaking API change safely?
5. How do operational contracts differ from schema contracts?

---

## 5. Data Consistency, Saga, Outbox, Idempotency

### Foundation

1. Why should each service own its database?
2. Why are distributed transactions hard?
3. What is two-phase commit?
4. What is a saga?
5. What is choreography saga?
6. What is orchestration saga?
7. What is the outbox pattern?
8. What is idempotency?
9. What is a compensating action?
10. What is a read model?

### Intermediate

1. Explain create-booking flow with PENDING status.
2. What does outbox protect against?
3. Why does outbox not remove the need for idempotent consumers?
4. What table would you use to track processed events?
5. Why can payment timeout mean unknown instead of failed?
6. How do you prevent duplicate booking creation?
7. How do you prevent duplicate payment authorization?
8. How do you design a DLQ replay safely?
9. What is CDC, and when would Debezium help?
10. Why is compensation not the same as rollback?

### Senior

1. The outbox relay publishes to Kafka and crashes before marking the row published. What happens?
2. A consumer commits offset before processing and then crashes. What happens?
3. Compensation to release inventory fails. What should the system do?
4. A workflow has 12 steps and timers. Would you consider Temporal/Camunda? Why?
5. How do you decide between orchestration and choreography for booking/payment?

---

## 6. Event-Driven And Kafka

### Foundation

1. What is a Kafka topic?
2. What is a partition?
3. What is an offset?
4. What is a consumer group?
5. What does Kafka ordering guarantee?
6. What is at-least-once delivery?
7. What is a DLQ?
8. What is consumer lag?
9. What is a poison message?
10. What is replay?

### Intermediate

1. Why does key choice affect ordering?
2. How do you preserve per-booking order?
3. Why can one hot key hurt throughput?
4. When should you commit offsets?
5. Why is exactly-once often misunderstood?
6. What is schema registry?
7. How do retry topics differ from immediate retry?
8. What metrics show consumer health?
9. How do you handle a poison message?
10. How do you design event payloads?

### Senior

1. Consumer lag grows. Walk through the debug path.
2. A topic has 6 partitions and 20 consumers in one group. What happens?
3. One partition has huge lag while others are fine. Diagnose it.
4. A replay updates old read models incorrectly. What was missing?
5. How do you prevent duplicate side effects during replay?

---

## 7. Resilience And Scalability

### Foundation

1. What is timeout?
2. What is retry?
3. What is backoff and jitter?
4. What is circuit breaker?
5. What is bulkhead?
6. What is rate limiting?
7. What is load shedding?
8. What is graceful degradation?
9. What is backpressure?
10. What is cascading failure?

### Intermediate

1. Timeout vs retry: how do they work together?
2. Retry vs circuit breaker: how are they different?
3. Why can retry storms happen?
4. What is a retry budget?
5. How do you size a connection pool?
6. What is p99 latency?
7. How do you reason about capacity planning for QPS?
8. How do you protect a downstream dependency?
9. How do you handle non-critical feature failure?
10. How do you choose autoscaling metrics?

### Senior

1. Payment Service is slow and upstream retries triple traffic. What do you do?
2. DB connection pool is exhausted. What dashboards do you check?
3. A service scales pods but throughput does not improve. What bottlenecks do you inspect?
4. How does backpressure differ for HTTP and Kafka consumers?
5. How do you define a safe fallback?

---

## 8. Observability And Operations

### Foundation

1. Logs vs metrics vs traces?
2. What is distributed tracing?
3. What is a span?
4. What is a health check?
5. Liveness vs readiness?
6. What is a runbook?
7. What is incident response?
8. What is rollback?
9. What is RTO?
10. What is RPO?

### Intermediate

1. What is OpenTelemetry?
2. What should a booking dashboard include?
3. What is an SLI?
4. What is an SLO?
5. What is error budget?
6. What is burn-rate alerting?
7. Why is consumer lag count not always enough?
8. What is outbox age?
9. Why should deploys be annotated on dashboards?
10. How do you write useful structured logs?

### Senior

1. Checkout p99 jumps from 300 ms to 6 seconds. Debug it.
2. Error rate spikes after deploy. What do you check first?
3. Region outage occurs. How do you communicate and mitigate?
4. Kafka lag is growing but consumer CPU is low. Diagnose.
5. What is the difference between user-impacting SLO and pod health?

---

## 9. Security And Zero Trust

### Foundation

1. Authentication vs authorization?
2. Why is gateway auth not enough?
3. What is JWT?
4. What is OAuth2 scope?
5. What is mTLS?
6. What is service identity?
7. What is secrets management?
8. What is least privilege?
9. What is network policy?
10. What is audit logging?

### Intermediate

1. What must be validated in a JWT?
2. What is JWKS and why does key rotation matter?
3. What is SPIFFE/SPIRE style workload identity?
4. What can service mesh secure?
5. What is policy-as-code?
6. Where would OPA help?
7. How do you rotate secrets safely?
8. How do you prevent tenant data leaks?
9. What should never be logged?
10. What actions need audit logs?

### Senior

1. An internal caller bypasses the gateway and cancels a booking. What failed?
2. Secret rotation causes Payment Service outage. Debug and prevent it.
3. Token signing key rotates and some requests fail. Diagnose.
4. A service accepts JWT with wrong audience. Why is that dangerous?
5. How do you reduce blast radius after a service compromise?

---

## 10. Kubernetes And Platform

### Foundation

1. What is a pod?
2. What is a deployment?
3. What is a Kubernetes Service?
4. What is ingress/gateway?
5. Startup vs readiness vs liveness probe?
6. What is HPA?
7. What is a ConfigMap?
8. What is a Secret?
9. What is service mesh?
10. What is sidecar pattern?

### Intermediate

1. Why should liveness not depend on DB availability?
2. What happens during graceful shutdown?
3. What are CPU requests and limits?
4. What causes CPU throttling?
5. What causes OOMKilled?
6. What is a PodDisruptionBudget?
7. What is topology spread?
8. How does cluster autoscaler relate to HPA?
9. Why can mesh retries be dangerous?
10. What metrics are required for canary?

### Senior

1. p99 rises but CPU average looks fine. How do you check throttling?
2. HPA scales consumers but lag stays high. Why?
3. Readiness is wrong during rollout. What user impact can happen?
4. Node drain removes too many pods. What was missing?
5. Secret config typo reaches production. How should rollout catch it?

---

## 11. API, Workflow, Cloud, Cost, Privacy, And Platform

### Foundation

1. What is the difference between API Gateway and API management?
2. Why do partner write APIs need idempotency keys?
3. What is a webhook?
4. What is a workflow engine?
5. What is cost per request?
6. What is PII?
7. What is a service catalog?
8. What is a golden path?
9. What is the difference between container and serverless runtime?
10. What is data retention?

### Intermediate

1. How do you design signed webhook delivery?
2. How do you handle webhook retries and DLQ?
3. When is Temporal/Camunda better than hand-rolled Saga?
4. Why must workflow activities be idempotent?
5. How do you choose between Kafka/MSK and SQS/SNS/EventBridge?
6. What causes microservice cost to grow faster than traffic?
7. How do logs/traces become a cost problem?
8. How do you delete user data from read models and search indexes?
9. What belongs in a service catalog?
10. What guardrails should a platform automate?

### Senior

1. A partner creates duplicate bookings after retries. Design the fix.
2. A webhook partner is down for 6 hours. What state, metrics, and replay controls do you need?
3. A 12-step workflow waits 48 hours for supplier confirmation. Workflow engine or Saga service?
4. Old workflows are running while workflow code changes. What can go wrong?
5. Booking traffic grows 3x and cost grows 10x. What do you investigate?
6. A user requests account deletion. How do you handle service DBs, events, caches, search, analytics, and backups?
7. Data residency prevents failover to one region. How does that affect DR design?
8. Company has 200 services and inconsistent deployment/logging/security. What platform strategy do you propose?
9. How do you separate platform responsibility from application service responsibility?
10. What is a good escape hatch for golden path exceptions?

---

## 12. Platinum Architecture Review

Answer these as 2-minute drills:

1. Design hotel booking checkout.
2. Extract Payment Service from monolith.
3. Design search read model for hotel inventory.
4. Design active-passive multi-region booking.
5. Decide whether inventory can be active-active.
6. Design loyalty points ledger.
7. Fix API Gateway that became a domain monolith.
8. Migrate from shared database to database per service.
9. Add contract testing to independent deployments.
10. Design incident response for checkout outage.
11. Design partner API and webhook delivery.
12. Design cost controls for a 10x traffic spike.
13. Design privacy deletion lifecycle.
14. Design platform golden path for a new service.

For each answer, include:

```text
boundary, data owner, communication, consistency, failure handling, observability, security,
privacy, cost, platform ownership, testing, deployment/migration, trade-off
```

---

## 13. Final Readiness Gate

You are ready when you can do all of this without notes:

1. Draw hotel booking architecture in 5 minutes.
2. Explain saga/outbox/idempotency in one integrated answer.
3. Debug p99 latency spike using metrics/traces/logs.
4. Defend service boundaries against service-per-table split.
5. Explain how to roll out a breaking API change safely.
6. Explain Kafka lag, DLQ, replay, and idempotent consumers.
7. Explain gateway auth vs service-level authorization.
8. Explain Kubernetes probe and graceful shutdown failure modes.
9. Provide a migration plan from monolith to microservices.
10. Score an architecture using the platinum rubric.
11. Run the local capstone and explain the request/outbox flow.
12. Design partner APIs and signed webhooks.
13. Decide when to use a workflow engine.
14. Explain cost per booking and top cost drivers.
15. Design user deletion across microservice data stores.
16. Explain golden path and service catalog strategy.
