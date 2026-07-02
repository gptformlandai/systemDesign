# Microservices Cloud Managed Architecture AWS EKS ECS Serverless Gold Sheet

> Track: Microservices Interview Track - Senior Path  
> Goal: map microservices design decisions to cloud-managed runtime choices without becoming tool-first.

---

## 1. Intuition

Cloud microservices are the same architecture problems with managed building blocks:

```text
runtime + network + data + messaging + identity + observability + deployment + cost
```

The senior move is not "use Kubernetes" or "use serverless." The senior move is:

```text
match workload shape to platform constraints
```

---

## 2. Definition

- Definition: cloud-managed microservices architecture uses managed compute, messaging,
  databases, networking, identity, observability, and deployment services to run independent
  business services.
- Category: cloud architecture, platform engineering, production operations.
- Core idea: managed services reduce undifferentiated operations but introduce constraints,
  integration semantics, quotas, and cost trade-offs.

---

## 3. Runtime Options

| Runtime | Good For | Watch Out |
|---|---|---|
| Kubernetes/EKS | many services, platform control, portability | operational complexity |
| ECS/Fargate | container services with less Kubernetes overhead | AWS-specific model |
| Lambda | event-driven/bursty workloads | cold starts, limits, local dev, connection reuse |
| App Runner/managed app platform | simpler web APIs | less low-level control |
| Batch/managed jobs | async heavy jobs | queueing and job lifecycle |
| Step Functions/workflow service | durable cloud workflow | state machine limits/cost/model |

Interview line:

```text
I choose runtime based on workload duration, traffic shape, dependency needs, team maturity,
operational control, and cost.
```

---

## 4. Hotel Booking Mapping

| Capability | Possible Cloud Building Block |
|---|---|
| API Gateway/BFF | API Gateway, ALB, CloudFront, container gateway |
| Booking API | EKS/ECS/Fargate service |
| Payment integration | container service or Lambda with strict idempotency |
| Notification | SQS/SNS/EventBridge + worker/Lambda |
| Booking workflow | Step Functions, Temporal on containers, or Saga service |
| Events | MSK/Kafka, SNS/SQS, EventBridge |
| Database | RDS/Aurora, DynamoDB, service-owned store |
| Secrets | Secrets Manager, Parameter Store, external secret operator |
| Observability | CloudWatch, X-Ray/OpenTelemetry, vendor APM |
| Deployment | CodePipeline/GitHub Actions/ArgoCD/managed rollout |

Do not copy this blindly. Use it as a decision map.

---

## 5. Kubernetes vs Serverless

| Question | Kubernetes Often Wins | Serverless Often Wins |
|---|---|---|
| Long-running service? | yes | sometimes poor fit |
| Bursty event workload? | possible | strong fit |
| Need custom sidecars/agents? | yes | limited |
| Need many protocols? | yes | limited by platform |
| Team has platform maturity? | yes | less required |
| Need scale-to-zero? | not typical | yes |
| Need predictable low latency? | strong if warm | cold starts matter |
| Need simple ops for small team? | maybe too much | often better |

Strong answer:

```text
Kubernetes gives control and consistency. Serverless gives operational simplicity and elastic
scale for event workloads. Both still need contracts, idempotency, observability, and cost
controls.
```

---

## 6. Managed Messaging Choices

| Need | Candidate |
|---|---|
| event stream, replay, ordering by key | Kafka/MSK |
| simple queue with retry/DLQ | SQS |
| fan-out pub/sub | SNS |
| event bus and SaaS/service routing | EventBridge |
| workflow state and retries | Step Functions or workflow engine |

Decision questions:

1. Do consumers need replay?
2. Is ordering required?
3. Is fan-out required?
4. Is the payload a command, event, or notification?
5. What is the retry/DLQ behavior?
6. What is the maximum retention?
7. What are throughput and cost limits?

---

## 7. Data Ownership In Cloud

Cloud does not remove the database-per-service rule.

Good:

```text
Booking Service owns booking DB.
Payment Service owns payment audit DB.
Availability Service owns inventory DB.
Reporting gets data through events/read models/warehouse.
```

Bad:

```text
All services share one Aurora database and write each other's tables.
```

Cloud-specific concerns:

- cross-region replication
- backup/restore
- encryption/KMS keys
- IAM/service access
- read replicas and stale reads
- connection pool behavior in serverless
- data residency
- cost per read/write/storage

---

## 8. Networking And Identity

Cloud microservices need:

- VPC/subnet design
- private service-to-service traffic
- ingress/egress control
- security groups/network policies
- workload identity/IAM roles
- mTLS where justified
- secret rotation
- audit trails

Interview line:

```text
I avoid static shared credentials where workload identity or short-lived credentials are
available.
```

---

## 9. Observability

Cloud-managed systems need unified signals across:

- API Gateway/edge
- service runtime
- database
- queue/broker
- workflow engine
- downstream providers
- deployment events

Minimum dashboard:

- request rate
- p95/p99 latency
- error rate
- saturation
- queue age/lag
- DLQ count
- dependency latency
- cost signal
- version/deploy annotation

---

## 10. Deployment And Rollback

Safe cloud rollout:

1. Build immutable artifact.
2. Scan image/package.
3. Run tests and contract gates.
4. Deploy canary or progressive rollout.
5. Separate metrics by version.
6. Abort on SLO burn or version-specific errors.
7. Roll back code.
8. Roll forward data migrations only with expand-contract discipline.

Serverless note:

```text
Lambda version/alias traffic shifting canary is powerful, but dependencies, secrets, and
event source mappings still need safe rollout.
```

---

## 11. Quotas And Limits

Managed services have limits:

- request size
- payload size
- execution duration
- concurrency
- connection count
- topic/queue throughput
- rule count
- API rate limits
- cross-region replication lag

Senior answer:

```text
For managed services, I always check quotas and failure semantics before choosing the tool.
The happy path is easy; the limits define production behavior.
```

---

## 12. Cloud Failure Modes

| Failure | User Impact | Mitigation |
|---|---|---|
| function cold start | latency spike | provisioned/warm capacity, choose containers |
| queue backlog | stale async side effects | age alerts, scaling, DLQ |
| DB connection storm | API failure | pooling/proxy, concurrency limits |
| region outage | partial/full outage | DR plan, RTO/RPO, failover drills |
| IAM misconfiguration | auth failures | least privilege tests and rollback |
| managed service quota hit | throttling | quota monitoring and pre-approval |
| event rule misroute | lost side effect | contract tests and audit |

---

## 13. Interview Question

> Design the hotel booking platform on AWS using managed services. Would you use EKS, ECS, Lambda, or a mix?

Strong answer:

```text
I would not force one runtime everywhere. Booking and Payment are long-running APIs with
strict idempotency and observability, so containers on ECS/EKS/Fargate can be a good fit.
Notification side effects can use a queue and workers or Lambda. For simple event routing,
SNS/SQS or EventBridge can work; for replay/order-heavy streams, Kafka/MSK is better. Booking
workflow can be a Saga service, Step Functions, or Temporal depending complexity. Each
service still owns its data, uses least-privilege identity, emits traces/logs/metrics, and
rolls out with canary plus rollback.
```

---

## 14. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| "serverless means no ops" | still needs limits, observability, security | design operations explicitly |
| one runtime for all workloads | poor fit for some services | match workload to runtime |
| shared database because managed DB is easy | destroys ownership | service-owned data |
| ignore quotas | surprise throttling | quota review and alerts |
| no cost model | managed costs grow quietly | unit economics |
| no local testing story | slow feedback | local mocks plus integration env |
| no rollback for events | bad events persist | versioned schemas and replay plan |

---

## 15. Strong Closing Answer

```text
Cloud-managed microservices are still microservices. Managed compute, queues, workflows, and
databases reduce some operations, but I still design around data ownership, idempotency,
contracts, failure modes, observability, security, quotas, cost, and rollback. I choose EKS,
ECS, Lambda, or managed messaging based on workload shape and team maturity, not hype.
```

