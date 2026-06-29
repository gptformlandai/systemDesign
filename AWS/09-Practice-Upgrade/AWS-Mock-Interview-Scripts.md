# AWS Mock Interview Scripts

> Track: AWS Interview Track — Practice Upgrade
> Goal: simulate MAANG-level AWS interviews with timed rounds, realistic probing questions, and self-evaluation criteria.

---

## How To Use This File

1. Use a timer for each segment
2. Speak your answers aloud (not just mentally)
3. Record yourself for the first 2-3 rounds
4. Score yourself or have a practice partner score you
5. Repeat weak rounds until comfortable

---

# Round 1: AWS Fundamentals (25 Minutes)

## Setup
- Timer: 25 minutes total
- Mode: solo (answer aloud) or with a partner
- Target: MAANG L5-equivalent depth

## Question Set

**[3 min] Question 1: VPC Architecture**

*Interviewer:* "Walk me through the VPC architecture you'd design for a production three-tier web application. Include subnets, routing, security groups, and internet access."

*Expected coverage:*
- 3 AZs minimum
- 3 subnet tiers: public (ALB, NAT GW), private-app (ECS/EC2), private-data (RDS)
- Internet Gateway, NAT Gateway per AZ
- Security groups: ALB SG → App SG → RDS SG (chained, least privilege)
- NACLs: use sparingly, only for explicit deny

*Probe questions if answer is shallow:*
- "What's the difference between stateful and stateless in this context?"
- "What happens to return traffic if NACL blocks ephemeral ports?"
- "Why put a NAT Gateway in the public subnet?"

---

**[3 min] Question 2: IAM Policy Evaluation**

*Interviewer:* "I have an IAM user with AdministratorAccess, but they can't perform an action in a member account in my AWS Organization. What are all the reasons this could happen?"

*Expected coverage:*
- SCP at Root, OU, or account level blocking the action
- Permission boundary on the user capping max permissions
- Resource-based policy on the resource denying the user
- Explicit Deny anywhere in the chain

*Probe questions:*
- "SCPs apply to the management account too, right?"
  - Answer: NO. SCPs do not apply to the management account.
- "If both SCP and IAM policy allow the action, is that sufficient?"
  - Answer: yes for IAM users/roles; resource policy may also need to allow for cross-account

---

**[4 min] Question 3: Lambda Architecture**

*Interviewer:* "Your Lambda-based API has P99 latency of 4 seconds but P50 is 150ms. What is happening and how do you fix it?"

*Expected coverage:*
- Cold starts (P99 = cold start tail; P50 = warm invocations)
- Fix options: provisioned concurrency, SnapStart (Java), reduce package size, init code outside handler
- X-Ray to confirm cold start duration
- Lambda Insights for init_duration metric

*Probe questions:*
- "What is SnapStart and when can't you use it?"
  - Java 11+ only on Lambda
- "Provisioned concurrency is expensive. How do you know how many to provision?"
  - P99 concurrency from CloudWatch; scale down with AppAutoScaling scheduled actions

---

**[5 min] Question 4: Database Design**

*Interviewer:* "An e-commerce application needs to support: get order by ID, get all orders for a user, get orders by date range per user, and get all pending orders across all users. Design the DynamoDB table."

*Expected coverage:*
- PK: orderId, SK: metadata → get order by ID
- GSI 1: PK=userId, SK=createdAt → all orders for user + date range (begins_with or between)
- GSI 2 (for pending): PK=status, SK=createdAt → all pending orders (sparse index; status only set when pending)
- Single-table: orderId as PK, SK=METADATA for order item

*Probe questions:*
- "Why is status as PK potentially problematic for a GSI?"
  - Low cardinality → hot partition if millions of orders with status=PENDING
  - Better: status#date partition, or use ElasticSearch for admin queries

---

**[5 min] Question 5: Distributed System Design**

*Interviewer:* "Design the messaging architecture for a notification system: payment confirmation emails, push notifications, and SMS. It must handle 50,000 notifications per second."

*Expected coverage:*
- SNS topic: payment-events
- SQS queues per channel: email-queue, push-queue, sms-queue
- Lambda consumers per queue (parallel processing)
- DLQ per queue
- SES for email (50,000 TPS verified domain sending)
- SNS for push notifications (mobile)
- SNS for SMS (check per-country SMS limits)
- Idempotency: userId+orderId+channel as deduplication key

*Probe:*
- "What if SES has sending limits?"
  - Warm up sending rates; request limit increase; use SES v2 dedicated IP pools

---

**[5 min] Question 6: Multi-Region DR**

*Interviewer:* "A banking platform requires RPO < 5 seconds and RTO < 5 minutes. Design the AWS architecture."

*Expected coverage:*
- Active-passive warm standby or active-active
- Aurora Global Database (< 1s replication lag)
- DynamoDB Global Tables (if using NoSQL)
- Global Accelerator for fast failover (30s vs Route 53 1-5 min)
- Secrets Manager multi-region secret replication
- ECR in both regions
- Quarterly DR drills with actual failover

*Probe:*
- "How do you handle Aurora promotion during failover?"
  - `failover-global-cluster` API call; endpoint updates automatically
  - Application needs to handle brief read-only window during promotion

---

## Self-Evaluation Checklist — Round 1

| Criteria | Yes | Partial | No |
|---|---|---|---|
| Named correct services with reasoning |  |  |  |
| Addressed failure modes |  |  |  |
| Covered security (least privilege, encryption) |  |  |  |
| Mentioned cost trade-offs |  |  |  |
| Handled probe questions without panic |  |  |  |
| Did not need to look up answers |  |  |  |

---

# Round 2: Architecture Design (30 Minutes)

## Setup
- Timer: 30 minutes total
- One large design question (open-ended)
- Expected depth: whiteboard-level design

## Design Prompt

*Interviewer:* "Design AWS infrastructure for a ride-sharing platform similar to Uber. The system needs to track driver locations in real-time (every 5 seconds), match riders to drivers, process payments, and handle 100,000 concurrent rides globally."

**[5 min] Clarify requirements:**
- How many drivers active simultaneously? (say 500,000 globally)
- Consistency requirements for payments? (strong)
- Location update frequency? (every 5 seconds per driver)
- Latency for matching? (< 1 second)

**[5 min] High-level components:**
```
Location tracking → Matching → Booking → Payment → Notifications
```

**[10 min] Deep dive each component:**

Location tracking:
- Driver mobile sends location every 5s
- API Gateway WebSocket → Lambda → write to ElastiCache Redis Geo (GEOADD)
- Redis Geo allows radius search: GEORADIUSBYMEMBER "drivers" lat lon 5 km
- DynamoDB: persist location history (TTL 24h, analytics)
- Kinesis Data Streams: stream for analytics, ML model training

Matching:
- Rider requests ride → Lambda reads Redis Geo → find nearest N drivers
- Step Functions: offer to Driver 1 → 10s timeout → offer to Driver 2 → etc.
- DynamoDB: write booking record with ACID transaction (reserve driver)

Payment:
- Stripe integration (not AWS-native)
- Lambda calls Stripe API
- Idempotency key: rideId to prevent double charge
- SQS for async payment processing

Notifications:
- SNS mobile push for driver and rider
- EventBridge: ride-events → multiple consumers

Multi-region:
- Route 53 latency routing: us-east-1, eu-west-1, ap-southeast-1
- ElastiCache Global Datastore for driver locations
- DynamoDB Global Tables for ride records
- Aurora Global Database for payment records (strong consistency needed)

**[10 min] Failure modes + observability:**
- Driver goes offline: location TTL in Redis expires after 60s → marked unavailable
- Lambda matching timeout: Step Functions Catch → try next driver → eventually fail ride
- Payment failure: DLQ → retry → compensate (cancel booking, notify rider)
- X-Ray tracing end-to-end
- CloudWatch: GEORADIUSBYMEMBER latency, match rate, payment success rate

---

## Self-Evaluation — Round 2

| Criteria | Score (1-5) |
|---|---|
| Correctly identified scale requirements |  |
| Component selection appropriate to requirements |  |
| Failure modes addressed |  |
| Security built in (not afterthought) |  |
| Observability plan |  |
| Cost-aware choices |  |
| Communication clarity |  |
| **Total** | **/35** |

Target: ≥ 25/35

---

# Round 3: Security Deep Dive (20 Minutes)

## Question Set

**[5 min] IAM and Cross-Account**

*Prompt:* "Walk me through how you'd implement least-privilege access for a team of 20 developers who need access to 5 AWS accounts (dev, staging, prod-A, prod-B, prod-C)."

*Expected:*
- IAM Identity Center (SSO) with Okta or AD as IdP
- Permission Sets: Developer (dev accounts), Readonly (prod accounts), OnCall (prod+write, time-limited)
- No IAM users, no static keys
- SCPs restricting prod accounts (no TerminateInstances without MFA condition)
- Session logging with CloudTrail

---

**[5 min] Encryption**

*Prompt:* "Explain how to ensure all sensitive data is encrypted end-to-end in an AWS microservices architecture. Cover transit and at rest."

*Expected:*
- In transit: TLS everywhere (ACM certs on ALB; enforce TLS in RDS parameter group; Secrets Manager over HTTPS)
- At rest: KMS CMK for RDS, S3 (SSE-KMS), EBS, DynamoDB, SQS
- Secrets Manager with rotation for DB passwords (not in environment variables)
- Application-level encryption for extra-sensitive fields (PII) using envelope encryption
- mTLS between services for service-to-service auth (ACM Private CA)

---

**[5 min] WAF and DDoS**

*Prompt:* "You're launching a public API that will be featured in a major news article. How do you protect it from traffic spikes and potential attacks?"

*Expected:*
- CloudFront in front (absorbs volumetric at edge, Shield Standard)
- WAF on CloudFront: AWSManagedRulesCommonRuleSet, rate-based rule (1000 req/5min per IP)
- API Gateway: stage-level throttling (1000 RPS burst), usage plans for known consumers
- Lambda concurrency limit: reserved concurrency to protect downstream services
- Shield Advanced consideration (if $3K/month justified)
- Auto Scaling behind ALB to handle legitimate traffic spikes

---

**[5 min] Incident Response**

*Prompt:* "At 2 AM, GuardDuty fires a high-severity finding: UnauthorizedAccess:IAMUser/InstanceCredentialExfiltration. An EC2 instance credential was used from an external IP. What do you do?"

*Expected:*
1. Triage: confirm the GuardDuty finding (actor IP, instance ID, what API calls made)
2. Contain: attach SCP or security group to block outbound from instance
3. Revoke: use IAM `CreateServiceSpecificCredential` date trick or terminate instance to revoke credentials (temporary STS credentials are associated with instance role)
4. Investigate: CloudTrail for all API calls made with the stolen credential
5. Remediate: identify how credential was exfiltrated (SSRF vulnerability? cURL to metadata endpoint?)
6. Rotate: if any damage done (S3 objects read, secrets accessed)
7. Document: security incident report

---

# Round 4: Behavioral + AWS Trade-Offs (15 Minutes)

## Question Set

**[3 min] When did you have to make a tough technical trade-off?**

Prepare an example using STAR:
- Situation: high-traffic API, latency vs cost
- Task: chose between provisioned concurrency ($$$) vs cold start (latency)
- Action: analyzed P99 impact, used provisioned for peak hours only via AppAutoScaling scheduled
- Result: 80% reduction in P99 latency, 40% reduction in provisioned concurrency cost vs 24/7 provisioning

**[3 min] Kinesis vs SQS vs Kafka — when do you choose each?**

Expected:
- SQS: simple queue, point-to-point, no replay needed, worker pattern
- Kinesis: ordered per shard, replay, real-time analytics, multiple consumers with independent positions
- Kafka: Kinesis at 10x scale, Kafka ecosystem (Kafka Connect, ksqlDB), self-hosted control

**[4 min] Tell me about a production incident you caused or responded to.**

STAR format with:
- What was the technical root cause
- How you detected it (observability)
- How you communicated during the incident
- Blameless postmortem learnings
- What you changed afterward (prevention)

**[5 min] How do you decide between Lambda and ECS Fargate for a new microservice?**

Expected factors:
- Duration: > 15 minutes = ECS (Lambda max is 15 min)
- Execution pattern: event-driven = Lambda; always-on = Fargate
- Cold start: latency sensitive = Fargate or provisioned Lambda
- Team: existing Lambda skills or Fargate skills?
- Cost: high constant traffic = Fargate (per-second, no cold start overhead)
          low/sporadic = Lambda (zero cost when idle)
- Complexity: Lambda is simpler to deploy; Fargate more powerful for complex networking

---

# Round 5: 45-Minute Full Loop Simulation

## Setup
- Set a single 45-minute timer
- Read the prompt, then talk through the design as if presenting to a whiteboard
- After 45 minutes: score yourself on all criteria

## Prompt

*"Design the AWS infrastructure for a healthcare data platform that:*
- *Ingests patient records from 50 hospitals via API (FHIR standard)*
- *Stores and queries records at scale (10M patients, 1B events)*
- *Provides a real-time dashboard for clinicians*
- *Meets HIPAA compliance requirements*
- *Allows ML models to run on de-identified data"*

**Expected Coverage:**

1. HIPAA compliance:
   - BAA with AWS
   - Encryption at rest: S3 SSE-KMS, RDS KMS, DynamoDB CMK
   - Encryption in transit: TLS everywhere
   - CloudTrail for all data access audit
   - VPC for all compute (no public EC2)
   - Secrets Manager for credentials
   - Config conformance pack: HIPAA

2. Ingestion:
   - API Gateway (FHIR API) + Lambda → SQS → Lambda (validation) → S3 (raw) + DynamoDB (indexed)
   - Kinesis for high-volume streaming
   - AWS HealthLake for FHIR-native storage (consideration)

3. Storage:
   - S3 data lake (raw FHIR records, Parquet for analytics)
   - DynamoDB (patient index, real-time lookups)
   - OpenSearch Serverless (FHIR search queries)

4. Real-time dashboard:
   - CloudFront + React SPA
   - Cognito for clinician authentication (SAML with hospital IdP)
   - API Gateway + Lambda → DynamoDB/OpenSearch
   - WebSocket for real-time updates

5. ML on de-identified data:
   - AWS Glue ETL: de-identification Lambda (remove PII: names, DOB, SSN → replace with synthetic)
   - Separate S3 bucket for de-identified data (different KMS key, different VPC endpoint)
   - SageMaker training on de-identified dataset
   - Model deployed to SageMaker real-time endpoint

6. Multi-account structure:
   - Production account (ePHI data)
   - ML sandbox account (de-identified data only, SCP blocks access to ePHI)
   - Separate KMS CMKs per environment
   - CloudTrail centralized to Log Archive account

---

## Final Score Sheet

| Section | Max Points | Your Score |
|---|---|---|
| Round 1: Fundamentals | 30 | |
| Round 2: Design | 35 | |
| Round 3: Security | 20 | |
| Round 4: Behavioral | 15 | |
| **Total** | **100** | |

**Scoring Guide:**
- 90-100: Ready to interview at top-tier companies
- 75-89: Strong candidate, address remaining gaps
- 60-74: Good foundation, 1-2 more weeks of practice
- < 60: Return to Gold Sheets for weak topics, then re-drill
