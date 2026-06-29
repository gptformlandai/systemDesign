# AWS Interview Scoring Rubrics

> Track: AWS Interview Track — Practice Upgrade
> Goal: objectively measure answer quality across six dimensions. Use for self-scoring or partner scoring.

---

## Master Rubric (6 Dimensions × 5 Points Each = 30 Points Total)

For every design or debugging answer, score each dimension independently.

---

### Dimension 1: Service Selection Accuracy (0-5)

| Score | Description |
|---|---|
| 5 | Chose exactly the right service for each requirement. Named specific tiers (e.g., Aurora vs RDS, HTTP API vs REST API, FIFO vs Standard SQS). Explained WHY each service was chosen. |
| 4 | Correct services with mostly accurate reasoning. Minor imprecision (e.g., said "SQS" but didn't specify Standard vs FIFO when it matters). |
| 3 | Core services correct, but missing 1-2 components (e.g., forgot DLQ, forgot S3 Gateway Endpoint). |
| 2 | Several services misidentified or used where they aren't appropriate. |
| 1 | Major service errors (e.g., S3 for real-time lookup, Lambda for 1-hour jobs). |
| 0 | Unable to select appropriate services. |

---

### Dimension 2: Failure Mode Coverage (0-5)

| Score | Description |
|---|---|
| 5 | Proactively identified 3+ failure modes and mitigation for each. Named specific configurations (DLQ with maxReceiveCount=3, Multi-AZ standby, circuit breaker pattern). |
| 4 | Identified 2 failure modes with actionable mitigation. One mode missed or vague. |
| 3 | Mentioned 1-2 failure modes but at surface level ("add a DLQ" without explaining the config or monitoring). |
| 2 | Only mentioned failure modes when prompted. Vague ("add retry logic"). |
| 1 | Single failure mode mentioned without detail. |
| 0 | No failure modes discussed. |

---

### Dimension 3: Security Coverage (0-5)

| Score | Description |
|---|---|
| 5 | Covered: network isolation (private subnets), least privilege IAM (specific policies not wildcards), encryption at rest and in transit, secrets management (Secrets Manager not env vars), audit trail (CloudTrail). |
| 4 | Covered 4 of 5 security areas. No major gaps. |
| 3 | Covered 3 areas. Missing 2 (often: forgets audit trail or forgets transit encryption). |
| 2 | Only mentioned "security groups" and "IAM". No depth. |
| 1 | Security mentioned as afterthought ("and we'd add security"). |
| 0 | No security discussion. |

---

### Dimension 4: Observability Depth (0-5)

| Score | Description |
|---|---|
| 5 | Named specific metrics (not just "CloudWatch"), log groups, trace approach, AND described how you'd detect and alert on failure conditions. Mentioned EMF or specific metric names. |
| 4 | Named CloudWatch + X-Ray with some specifics. Alarms described. 1 area shallow. |
| 3 | Mentioned CloudWatch metrics and logs. No tracing. No alarm thresholds. |
| 2 | "We'd add CloudWatch." No specifics on what to monitor or alert on. |
| 1 | Observability mentioned only when directly asked. |
| 0 | No observability mentioned. |

---

### Dimension 5: Cost Awareness (0-5)

| Score | Description |
|---|---|
| 5 | Mentioned cost trade-offs for at least 2 choices. Named specific cost drivers (NAT Gateway per GB, Lambda per ms, cross-AZ data transfer). Suggested optimizations (S3 endpoint, Savings Plans, Spot). |
| 4 | Cost mentioned for 1-2 key decisions. Named Savings Plans or Spot without full analysis. |
| 3 | Brief cost mention ("Fargate is more expensive than Lambda for low traffic"). |
| 2 | Cost mentioned when prompted only. |
| 1 | Acknowledged cost exists but no specifics. |
| 0 | No cost consideration. |

---

### Dimension 6: Communication Clarity (0-5)

| Score | Description |
|---|---|
| 5 | Structured answer (clarify → design → deep dive → failure → security → observability). No rambling. Used concrete examples and numbers. Confident but not dismissive of trade-offs. |
| 4 | Well-structured. Minor tangent or one confusing explanation. |
| 3 | Generally clear but disorganized in 1-2 sections. Some undefined jargon. |
| 2 | Struggled to organize thoughts. Answer jumped around. Hard to follow. |
| 1 | Very difficult to follow. Contradicted earlier statements. |
| 0 | Unable to communicate answer clearly. |

---

## Score Interpretation

| Total Score (out of 30) | Rating |
|---|---|
| 27-30 | Exceptional: MAANG L6/L7 depth. Offer-ready. |
| 23-26 | Strong: L5-ready with minor polish needed. |
| 18-22 | Solid L4: study specific weak dimensions. |
| 13-17 | Developing: 2-3 more weeks of study needed. |
| < 13 | Foundational gaps: return to Gold Sheets for failing dimensions. |

---

# Per-Topic Scoring Checklists

Use these checklists to evaluate specific question types.

---

## Checklist: VPC Design Question

**Question type:** "Design a VPC for a 3-tier application."

| Checkpoint | Did You Cover? |
|---|---|
| 3 subnet tiers (public/private-app/private-data) | ☐ |
| Multi-AZ (min 2, ideally 3) | ☐ |
| Internet Gateway + NAT Gateway | ☐ |
| NAT Gateway per AZ (not shared) | ☐ |
| Security groups (chained, stateful) | ☐ |
| Mentioned NACLs or explained why not using them | ☐ |
| S3/DynamoDB Gateway Endpoints (cost) | ☐ |
| Private subnets for EC2/ECS and RDS | ☐ |
| ALB in public subnet | ☐ |
| No RDS in public subnet | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: Lambda Architecture Question

**Question type:** "Design a serverless API" or "Why is the Lambda slow?"

| Checkpoint | Did You Cover? |
|---|---|
| Cold start explanation (init phase, runtime, package size) | ☐ |
| Provisioned concurrency vs reserved concurrency | ☐ |
| Lambda in VPC trade-offs (if VPC needed) | ☐ |
| VPC Interface Endpoints for AWS services | ☐ |
| SQS event source with ReportBatchItemFailures | ☐ |
| Secrets from Secrets Manager (not env vars) | ☐ |
| X-Ray tracing | ☐ |
| DLQ / Lambda Destination for async failures | ☐ |
| API Gateway: REST vs HTTP API decision | ☐ |
| Throttling and rate limiting | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: DynamoDB Design Question

| Checkpoint | Did You Cover? |
|---|---|
| Access patterns identified first | ☐ |
| Partition key has high cardinality (distributes load) | ☐ |
| Sort key enables range queries | ☐ |
| Hot partition problem addressed | ☐ |
| GSI for alternative access patterns | ☐ |
| On-demand vs provisioned decision | ☐ |
| DAX consideration (if read-heavy) | ☐ |
| DynamoDB Streams for event-driven | ☐ |
| TTL for temporary data | ☐ |
| Single-table design consideration | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: Security Question

| Checkpoint | Did You Cover? |
|---|---|
| IAM roles (not users) for services | ☐ |
| Least privilege policies (specific ARNs, not *) | ☐ |
| Encryption at rest (KMS CMK) | ☐ |
| Encryption in transit (TLS everywhere) | ☐ |
| Secrets Manager for credentials | ☐ |
| VPC network isolation | ☐ |
| CloudTrail for audit | ☐ |
| IAM policy evaluation order | ☐ |
| SCPs for organizational guardrails | ☐ |
| Permission boundaries where applicable | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: Multi-Region / DR Question

| Checkpoint | Did You Cover? |
|---|---|
| Defined RTO and RPO requirements first | ☐ |
| Named correct DR pattern (backup / pilot / warm / active-active) | ☐ |
| Database DR (Aurora Global or DynamoDB Global Tables) | ☐ |
| DNS failover (Route 53 health checks or Global Accelerator) | ☐ |
| Secrets available in DR region | ☐ |
| ECR images available in DR region | ☐ |
| ACM certificates in DR region | ☐ |
| Mentioned DR testing frequency | ☐ |
| Single points of failure eliminated | ☐ |
| Cost trade-off of warm standby vs active-active | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: Messaging / Event-Driven Question

| Checkpoint | Did You Cover? |
|---|---|
| SQS vs SNS vs EventBridge decision | ☐ |
| Fan-out pattern (SNS → SQS) explained | ☐ |
| DLQ with maxReceiveCount | ☐ |
| CloudWatch alarm on DLQ depth | ☐ |
| Visibility timeout > processing time | ☐ |
| ReportBatchItemFailures for Lambda | ☐ |
| Idempotency (how duplicates are handled) | ☐ |
| Message ordering requirements addressed | ☐ |
| FIFO vs Standard decision | ☐ |
| Large message pattern (> 256 KB) | ☐ |

**Score: ___ / 10 checkpoints**

---

## Checklist: GenAI / LLM Design Question

| Checkpoint | Did You Cover? |
|---|---|
| Bedrock vs SageMaker decision justified | ☐ |
| Model selection reasoning (cost/quality/latency) | ☐ |
| RAG architecture (embed → search → inject → generate) | ☐ |
| Chunking strategy trade-offs | ☐ |
| Guardrails (content filter, PII, grounding) | ☐ |
| Token cost tracking and optimization | ☐ |
| Prompt versioning | ☐ |
| Model evaluation before production | ☐ |
| Progressive rollout for model upgrades | ☐ |
| Observability (latency, cost, quality scores) | ☐ |

**Score: ___ / 10 checkpoints**

---

# Tracking Sheet

## Dimension Averages (track over time)

| Date | Service Selection | Failure Modes | Security | Observability | Cost | Communication | Total |
|---|---|---|---|---|---|---|---|
| | | | | | | | /30 |
| | | | | | | | /30 |
| | | | | | | | /30 |
| | | | | | | | /30 |
| | | | | | | | /30 |

**Goal: maintain ≥ 23/30 across 3 consecutive sessions before the interview.**

## Topic Checklist Averages

| Date | VPC | Lambda | DynamoDB | Security | Multi-Region | Messaging | GenAI |
|---|---|---|---|---|---|---|---|
| | /10 | /10 | /10 | /10 | /10 | /10 | /10 |
| | /10 | /10 | /10 | /10 | /10 | /10 | /10 |

**Goal: ≥ 8/10 in each topic checklist.**
