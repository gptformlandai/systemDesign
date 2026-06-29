# AWS Scenario Drill Bank

> Track: AWS Interview Track — Practice Upgrade
> Goal: develop structured responses to open-ended design and debugging scenarios using the AWS Interview Architecture Template.

---

## Answer Template (9-Step Framework)

For every design question:

1. Clarify requirements (functional, non-functional, scale)
2. Estimate scale (RPS, data volume, users)
3. Define the high-level architecture (3-4 boxes)
4. Drill into each component (service choice + reasoning)
5. Handle failures (what breaks, how to recover)
6. Security (least privilege, encryption, network isolation)
7. Observability (metrics, logs, traces, alarms)
8. Cost optimization (right-sizing, savings plans, endpoints)
9. Trade-offs (what you chose vs alternatives)

---

# Category 1: System Design Scenarios

---

## Scenario 1: E-Commerce Order Processing

**Interview Prompt:**
"Design the backend for an e-commerce platform that processes 10,000 orders per day, supports peak traffic of 500 RPS during flash sales, and must not lose any order."

**Key Requirements To Clarify:**
- Can orders be eventually processed or must they be synchronous (immediate confirmation)?
- What downstream systems must be notified? (inventory, email, analytics)
- What is the failure tolerance for each step?
- Cross-region or single region?

**Strong Architecture:**

```text
Frontend (CloudFront + S3 for SPA)
  -> API Gateway (throttling: 500 RPS limit per stage)
  -> Lambda: validate and publish order to SQS FIFO (MessageGroupId = orderId)
  -> SQS FIFO Queue: OrderQueue.fifo (deduplication window 5 minutes)
  -> Lambda (OrderProcessor): consumes from SQS
     - Write order to Aurora (primary region)
     - Publish to SNS topic: order-events
  -> SNS fan-out:
     - SQS: InventoryQueue -> InventoryService Lambda (reserve stock)
     - SQS: NotificationQueue -> EmailService Lambda (SES)
     - SQS: AnalyticsQueue -> AnalyticsService Lambda (Firehose → S3)
```

**Failure Handling:**
- SQS FIFO with DLQ: failed orders go to DLQ after 3 retries
- CloudWatch alarm: DLQ > 0 → PagerDuty
- SQS visibility timeout: 120s (longer than order processing time)
- Lambda destinations: on failure → SNS alert

**Security:**
- API Gateway Cognito authorizer: authenticated users only
- Lambda task roles: least privilege per function
- RDS in private subnet: security group from Lambda only
- SQS resource policy: restrict to account

**Observability:**
- EMF metrics: OrdersPlaced, OrderProcessingDuration
- X-Ray tracing across Lambda chain
- CloudWatch Logs Insights: query failed orders by error type

---

## Scenario 2: URL Shortener At Scale

**Interview Prompt:**
"Design a URL shortener like bit.ly that handles 10,000 writes/second and 100,000 reads/second globally."

**Key Requirements To Clarify:**
- How long are shortened URLs valid?
- Is there analytics on click counts?
- Can the same long URL produce multiple short codes?
- Custom short codes or random?

**Strong Architecture:**

```text
Global:
  Route 53 latency routing -> us-east-1, eu-west-1

Write path (10,000 writes/sec):
  ALB -> ECS Fargate (URLShortener service)
  -> Generate 7-character base62 code (ID from DynamoDB atomic counter or UUID)
  -> Write to DynamoDB: {shortCode: "abc1234", longUrl: "...", createdAt, expiry, userId}
  -> Return short URL

Read path (100,000 reads/sec) - ultra-low latency:
  CloudFront -> Lambda@Edge (Viewer Request)
    -> Check ElastiCache Redis for shortCode -> longUrl
    -> If miss: query DynamoDB -> cache with TTL = expiry date
    -> Return 301/302 redirect to longUrl

Click analytics:
  Lambda@Edge logs click event
  -> Kinesis Data Firehose -> S3 (Parquet format)
  -> Athena for analytics
```

**DynamoDB Key Design:**
- PK: shortCode (high cardinality, random base62 = even distribution)
- GSI: userId-createdAt for "my shortened URLs" view
- TTL attribute: automatic expiry

**Read Optimization:**
- CloudFront + Lambda@Edge for global 301 redirect
- ElastiCache Redis: LRU eviction, cache popular codes
- DynamoDB on-demand: handles traffic spikes without pre-provisioning

---

## Scenario 3: Real-Time Chat Application

**Interview Prompt:**
"Design a real-time chat system like Slack for 1 million concurrent users."

**Key Requirements:**
- Message persistence required?
- Group chats or 1:1 only?
- Read receipts?
- File sharing?

**Strong Architecture:**

```text
WebSocket connections:
  API Gateway WebSocket API -> Lambda (connect/disconnect/message)
  Connection registry: DynamoDB {connectionId, userId, roomId, ttl}

Message flow:
  User sends message
  -> Lambda publishes to SNS topic: room-{roomId}-messages
  -> SNS fan-out to:
     - DynamoDB: persist message
     - Lambda: push to all connected users in room (fetch connectionIds from DDB, push via API GW WebSocket)

Presence:
  User connects -> write to DynamoDB with TTL 30s
  Heartbeat every 20s refreshes TTL
  Missed heartbeat = connection considered stale

File sharing:
  S3 presigned POST -> user uploads directly
  S3 event -> Lambda publishes message with S3 URL
  CloudFront signed URL for secure file download

Search:
  New messages -> EventBridge -> Lambda -> OpenSearch Serverless indexing
```

---

## Scenario 4: Multi-Tenant SaaS Platform

**Interview Prompt:**
"You're building a B2B SaaS platform. How do you isolate tenant data?"

**Strong Architecture:**

```text
Option 1: Silo (one account per tenant — maximum isolation)
  Each tenant = dedicated AWS account
  Workloads fully isolated
  Data never co-mingles
  Best for: enterprise, compliance-heavy, large tenants

Option 2: Bridge (shared infra, separate databases)
  Shared ECS cluster, shared ALB
  Separate RDS database per tenant (or separate schema)
  DynamoDB: tenant-prefix on all partition keys
  Best for: mid-size tenants, balance cost/isolation

Option 3: Pool (fully shared)
  Shared everything with tenant-ID in every record
  Row-level security in RDS PostgreSQL
  DynamoDB: PK includes tenantId prefix
  Best for: small tenants, lowest cost

Security in pool model:
  JWT with tenantId claim
  API Gateway Lambda authorizer: extract tenantId, validate claim
  Every DynamoDB query must include tenantId in key condition
  Row-Level Security in RDS: every query filtered by current_setting('app.tenant_id')
```

---

# Category 2: Debugging and Incident Scenarios

---

## Scenario 5: Lambda Timeout Spike

**Prompt:**
"Users report API timeouts. Lambda functions that took 200ms are now taking 30 seconds. What do you do?"

**Investigation Approach:**

```text
Step 1: Check CloudWatch Lambda metrics
  - Duration P99: confirm 30s spikes
  - Throttles: are functions being throttled?
  - Errors: timeout vs other errors?
  - ConcurrentExecutions: near limit?

Step 2: X-Ray traces
  - Open ServiceLens
  - Filter: duration > 5000ms
  - Drill into slow trace: which subsegment is slow?

Step 3: Common causes
  - DB connection: Lambda hitting RDS max_connections (add RDS Proxy)
  - External API: third-party timeout (add circuit breaker)
  - Cold starts: provisioned concurrency
  - Memory pressure: increase memory (more CPU allocated)
  - VPC NAT issue: NAT Gateway overloaded or missing route

Step 4: Check CloudWatch Logs Insights
  filter @type = "REPORT" | stats percentile(Duration, 99) by bin(5min)
  Correlate spike time with deployments

Step 5: Fix
  - DB connections: add RDS Proxy
  - External timeout: circuit breaker with fallback
  - Lambda memory: increase (power tune)
  - Cold start: provisioned concurrency
```

---

## Scenario 6: SQS Message Build-Up

**Prompt:**
"ApproximateNumberOfMessages in your SQS queue is growing from 1,000 to 50,000 over 30 minutes. What happened and what do you do?"

**Investigation:**

```text
Immediate check:
  1. Are consumers running? (Lambda errors, ECS task count)
  2. Consumer errors in CloudWatch? DLQ messages?
  3. Lambda Throttles? (concurrency limit reached)
  4. Visibility timeout expired? (messages reappearing)

Common causes:
  A) Lambda concurrency limit: 
     all 1,000 concurrent executions consumed by other functions
     Fix: increase reserved concurrency for this queue's Lambda
  
  B) Consumer crashing immediately:
     Lambda DLQ growing
     Fix: check Lambda error logs, fix application bug, redrive DLQ
  
  C) Message processing too slow (visibility timeout expiring):
     Messages returning to queue faster than processed
     Fix: increase visibility timeout; or parallelize processing

  D) Downstream dependency down (DB, API):
     Consumer succeeds reading but fails writing → message returns
     Fix: fix downstream; messages wait safely in queue

Actions:
  1. Scale up Lambda concurrency or ECS consumers
  2. Check and fix application errors
  3. Alert stays active until queue depth drops to 0
  4. After resolution: review consumer throughput vs max queue depth
```

---

## Scenario 7: S3 Access Denied Suddenly

**Prompt:**
"A Lambda function that successfully read from S3 yesterday is now getting AccessDeniedException. Nothing changed in the code. What happened?"

**Investigation:**

```text
Most common causes:

1. IAM role detached or modified
   Check: CloudTrail for iam:DetachRolePolicy, iam:PutRolePolicy events
   Fix: re-attach correct policy

2. SCP added at organization level
   Check: SCPs in Organizations management account
   Fix: update SCP or request change

3. S3 bucket policy changed
   Check: CloudTrail for s3:PutBucketPolicy
   Fix: restore previous policy

4. KMS key policy changed (if bucket uses SSE-KMS)
   Check: CloudTrail for kms:PutKeyPolicy
   Fix: restore key policy to allow Lambda role to Decrypt

5. VPC endpoint policy change (if Lambda in VPC using S3 via endpoint)
   Check: VPC endpoint policy
   Fix: restore endpoint policy

Process:
  aws cloudtrail lookup-events \
    --lookup-attributes AttributeKey=ResourceName,AttributeValue=my-bucket \
    --start-time 2025-01-14T00:00:00

  Filter for PutBucketPolicy, PutBucketAcl events
  Find who changed what and when
  Restore or update to fix
```

---

## Scenario 8: DynamoDB ProvisionedThroughputExceededException

**Prompt:**
"Your DynamoDB table starts throwing ProvisionedThroughputExceededException at peak hours. Provisioned WCUs = 1,000. How do you diagnose and fix?"

**Diagnosis:**

```text
CloudWatch metrics:
  ConsumedWriteCapacityUnits: is actual consumption reaching 1,000?
  WriteThrottleEvents: confirms throttling occurring
  SystemErrors: rule out system issues

DynamoDB contributor insights:
  Shows which partition keys are consuming the most capacity
  Hot key identified: userId = "admin-user-123" consuming 40% of capacity

CloudWatch Logs Insights on DDB:
  Check if a specific code path is hammering one partition key
```

**Fixes:**

```text
Option 1: Switch to On-Demand capacity
  No more provisioning; AWS scales automatically
  Cost increases for sustained high traffic; good for bursty

Option 2: Increase provisioned capacity + Enable Auto Scaling
  Set target utilization to 70%
  Auto scales up during peaks, down during valleys

Option 3: Fix hot partition key (root cause)
  If hot key is a structural problem:
  - Implement write sharding: suffix PK with random 0-9
  - Use DynamoDB DAX for read hot keys
  - Redesign access pattern if possible

Option 4: Application-level retry with backoff
  AWS SDK default: exponential backoff handles transient throttling
  Ensure SDK retry is configured (default is 3 retries with backoff)
```

---

# Category 3: Architecture Trade-Off Scenarios

---

## Scenario 9: Monolith Migration to Microservices

**Prompt:**
"Your team is migrating a Spring Boot monolith to microservices on ECS. What would you do first and how would you approach the migration?"

**Strong Answer:**

```text
Strangler Fig pattern — incrementally extract services:

Phase 1: Containerize the monolith
  - Docker image of existing monolith
  - Deploy on ECS Fargate (no code change)
  - ALB in front for routing
  - Benefit: operational experience with ECS before splitting

Phase 2: Extract by bounded context (domain-driven)
  - Identify highest-value or highest-change-rate service to extract first
  - Example: extract Payment Service (clearest domain boundary)
  - ALB path routing: /api/payments/* -> PaymentService; /* -> Monolith
  - Monolith database still shared (anti-pattern but safe first step)

Phase 3: Database decomposition
  - Identify Payment Service tables
  - Create separate Aurora cluster for PaymentService
  - Dual-write period: write to both DBs during transition
  - Cut over reads to new DB, validate
  - Remove old tables from monolith DB

Phase 4: Service-to-service communication
  - Synchronous: HTTP/REST or gRPC with service discovery (ALB or Cloud Map)
  - Asynchronous: SNS/SQS for events (preferred for loose coupling)
  - Use correlation IDs in all calls for distributed tracing

Guardrails:
  - API contracts defined (OpenAPI specs)
  - Contract testing (Pact)
  - Feature flags for cutover
  - Observability from day one (X-Ray, EMF metrics)
```

---

## Scenario 10: Cost Reduction Without Downtime

**Prompt:**
"Your AWS bill increased 40% last quarter. How do you investigate and reduce it without downtime?"

**Strong Answer:**

```text
Step 1: Cost Explorer analysis
  Filter by service, account, tag
  Identify top 3 cost drivers (usually EC2, NAT Gateway, data transfer)

Step 2: Right-size EC2
  AWS Compute Optimizer recommendations
  Enable CloudWatch detailed monitoring for 2 weeks
  Identify instances at <20% average CPU -> downsize
  Use Spot instances for dev/test (70% savings)

Step 3: Savings Plans analysis
  Cost Explorer Savings Plans recommendations
  Purchase Compute Savings Plans for committed baseline workloads
  Keep 20% on-demand for flexibility

Step 4: NAT Gateway cost
  Check CloudWatch NAT Gateway BytesOut
  Add S3 Gateway Endpoint (free, removes S3 traffic from NAT)
  Add DynamoDB Gateway Endpoint (free)
  For ECR: Interface Endpoint often cheaper than NAT at scale

Step 5: Identify wasted resources
  Unattached EBS volumes (gp2 with no instance)
  Old EBS snapshots (lifecycle policy)
  Idle load balancers
  Oversized RDS (reduce instance class or switch to Aurora Serverless v2)

Step 6: Data transfer
  Cross-AZ data: co-locate frequently communicating services in same AZ
  Internet egress: use CloudFront (cheaper egress from edge)

Step 7: Storage tiers
  S3 Intelligent-Tiering for mixed access buckets
  Lifecycle rules: Standard -> IA -> Glacier for logs/backups
```

---

# Scoring Guide

For each scenario response, assess:

| Criteria | Score |
|---|---|
| Identified the right questions to clarify | /2 |
| Named correct AWS services with reasoning | /3 |
| Addressed failure modes | /2 |
| Addressed security | /1 |
| Addressed observability | /1 |
| Mentioned cost trade-offs | /1 |
| **Total** | **/10** |

Target: ≥ 7/10 per scenario before the interview.
