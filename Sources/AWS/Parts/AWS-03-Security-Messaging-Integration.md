# AWS Interview Notes - Part 3: Security + Messaging + Integration

> Covers: IAM, KMS, Cognito, SQS, SNS, EventBridge, Step Functions. This part is where interviewers check whether you can build secure systems, decouple services correctly, and reason about async workflows without creating operational chaos.

---

# Table of Contents

1. [How to Think About Security and Integration](#1-how-to-think-about-security-and-integration)
2. [IAM](#2-iam)
3. [KMS](#3-kms)
4. [Cognito](#4-cognito)
5. [SQS](#5-sqs)
6. [SNS](#6-sns)
7. [EventBridge](#7-eventbridge)
8. [Step Functions](#8-step-functions)
9. [High-Value Comparisons](#9-high-value-comparisons)
10. [Architecture Patterns](#10-architecture-patterns)
11. [Common Interview Traps](#11-common-interview-traps)
12. [Rapid Revision Sheet](#12-rapid-revision-sheet)

---

# 1. How to Think About Security and Integration

Security and integration questions usually test whether you understand:

- identity vs authentication vs authorization
- encryption at rest vs encryption in transit
- point-to-point coupling vs pub/sub
- queue vs event bus vs workflow orchestration
- retries, idempotency, dead-letter handling

Good architects reduce coupling while keeping failure modes understandable.

---

# 2. IAM

## 2.1 What It Is

AWS IAM controls who can do what on which resource.

This is the foundational AWS security service.

## 2.2 Key Concepts

- User: long-term identity, usually avoid for apps
- Group: collection of users
- Role: assumed identity, primary mechanism for workloads
- Policy: JSON permissions document

## 2.3 Golden Rule

Prefer roles over static credentials.

Examples:

- EC2 instance role
- ECS task role
- Lambda execution role
- EKS IAM role for service accounts

## 2.4 Policy Evaluation

Important principle:

- explicit deny beats allow
- if no allow matches, access is denied

## 2.5 Least Privilege

Interviewers expect you to say:

- scope permissions narrowly
- avoid `*` wherever possible
- separate human access from workload access
- rotate away from static access keys

## 2.6 Common IAM Use Cases

- service accessing S3 bucket
- Lambda reading Secrets Manager value
- ECS task publishing to SQS

## 2.7 Interview Trap

"Attach admin policy and move on."

That is not an engineering answer. It is a security failure.

---

# 3. KMS

## 3.1 What It Is

AWS KMS manages cryptographic keys used for encryption.

It commonly supports:

- encrypting S3 objects
- encrypting RDS storage
- encrypting EBS volumes
- envelope encryption patterns

## 3.2 Core Concepts

- CMK/KMS key
- customer managed vs AWS managed keys
- key policies
- grants
- envelope encryption

## 3.3 Envelope Encryption

High-value interview topic:

1. data is encrypted with a data key
2. data key is encrypted by a KMS key
3. encrypted data and encrypted data key are stored

Why it matters:

- scalable
- efficient
- centralizes master key control

## 3.4 Key Rotation and Access

- customer managed keys offer stronger control
- IAM and key policies both matter
- audit access with CloudTrail

## 3.5 What Interviewers Ask

- difference between AWS managed and customer managed keys
- when to use KMS directly vs let an AWS service integrate with it
- how encryption access is controlled

---

# 4. Secrets Manager vs Parameter Store

## 4.1 The Question You Will Be Asked

"Why use Secrets Manager instead of Parameter Store?"

This is a very common interview question.

## 4.2 Comparison

| Aspect | Secrets Manager | Parameter Store |
|---|---|---|
| Primary purpose | Secrets (DB creds, API keys) | Config values + secrets |
| Automatic rotation | Yes (built-in for RDS, etc.) | No |
| Cost | $0.40/secret/month + API calls | Free tier + lower cost |
| Max size | 64KB | 8KB (standard), 10KB (advanced) |
| Versioning | Yes | Yes |
| Cross-account | Yes | Yes |
| KMS encryption | Always | Optional SecureString |

## 4.3 When to Use Each

```
Use Secrets Manager when:
  ✓ Credentials need automatic rotation (RDS, Redshift)
  ✓ Secrets are shared across accounts
  ✓ Compliance requires managed rotation
  ✓ Cost is not the primary concern

Use Parameter Store when:
  ✓ Storing configuration (feature flags, endpoints)
  ✓ Budget-conscious secret storage without rotation
  ✓ Simple key-value config for apps
  ✓ Already in SSM ecosystem
```

## 4.4 Interview Gold Answer

```
"For database credentials that need rotation, I use Secrets Manager—it has
built-in rotation for RDS. For application config values and non-rotating
secrets, Parameter Store is simpler and cheaper. Both integrate with IAM
for access control and KMS for encryption."
```

---

# 5. Cognito

## 4.1 What It Is

Amazon Cognito is managed identity for application users.

It helps with:

- sign-up/sign-in
- user directories
- token issuance
- federation with external identity providers

## 4.2 Key Pieces

- User Pools: authentication, users, tokens
- Identity Pools: temporary AWS credentials for client access scenarios

## 4.3 Where It Fits

Use Cognito when your application needs end-user identity and you do not want to build auth from scratch.

## 4.4 Interviewer Angle

Do not confuse:

- IAM: AWS resource access control
- Cognito: end-user identity and authentication for applications

## 4.5 Strong Answer

```
If I need end-user login for a web app and token-based authentication without building a full auth system, Cognito User Pools is a reasonable managed choice. IAM roles solve workload authorization in AWS, not end-user login.
```

---

# 5. SQS

## 5.1 What It Is

Amazon SQS is a managed message queue.

Core value:

- decouples producers from consumers
- buffers spikes
- improves resilience

## 5.2 Queue Types

- Standard Queue: at-least-once delivery, best-effort ordering
- FIFO Queue: ordering and deduplication guarantees with throughput trade-offs

## 5.3 Important Concepts

- visibility timeout
- long polling
- dead-letter queue
- redrive
- message retention

## 5.4 What Interviewers Care About

- at-least-once means consumers must be idempotent
- visibility timeout must exceed processing time reasonably
- DLQ design matters

## 5.5 Common Use Cases

- async order processing
- background jobs
- buffering traffic spikes
- Lambda event source

## 5.6 Strong Answer

```
I use SQS when I want temporal decoupling between services. The producer should not care whether the consumer is temporarily slow or down, as long as the message is durably queued.
```

---

# 6. SNS

## 6.1 What It Is

Amazon SNS is a pub/sub notification service.

It fans out a message to multiple subscribers.

## 6.2 Best Use Cases

- fan-out to multiple SQS queues
- push notifications
- email/SMS notifications
- broadcasting events to multiple consumers

## 6.3 SNS vs SQS

- SNS pushes to subscribers
- SQS stores messages for polling consumers

Very common architecture:

```
Publisher -> SNS topic -> multiple SQS queues -> independent consumers
```

This is one of the strongest basic integration patterns in AWS.

## 6.4 Interviewer Hot Points

- why SNS plus SQS is better than one service calling five other services directly
- how each consumer can retry independently

---

# 7. EventBridge

## 7.1 What It Is

Amazon EventBridge is an event bus for routing events between AWS services, SaaS systems, and your applications.

## 7.2 When to Use It

Use EventBridge when:

- you are routing events by rules
- many producers and consumers exist
- you want event-driven architecture with loose coupling
- AWS service events are part of the design

## 7.3 Core Model

- producer emits event
- event lands on bus
- rules match event pattern
- targets receive event

## 7.4 EventBridge vs SNS

SNS is simpler broadcast pub/sub.
EventBridge is richer event routing and event-bus style integration.

Interview-safe distinction:

```
SNS is excellent for fan-out notifications.
EventBridge is stronger when routing decisions depend on event content and multiple rule-based targets.
```

## 7.5 Good Use Cases

- order-created event routed differently by order type
- AWS service events triggering remediation
- SaaS integrations

---

# 8. Step Functions

## 8.1 What It Is

AWS Step Functions is a workflow orchestration service.

Use it when a process has multiple steps, branching, retries, and error handling.

## 8.2 Best Use Cases

- business workflows
- ETL pipelines
- approval flows
- saga-like orchestration
- multi-step serverless processing

## 8.3 Why It Matters

It externalizes orchestration logic instead of hiding it in code scattered across services.

## 8.4 Strong Features

- explicit state transitions
- retries
- catch/fallback handling
- parallel branches
- auditability of workflow execution

## 8.5 Step Functions vs Simple Chained Lambdas

Interview answer:

```
If the workflow has multiple steps, conditional branches, retries, and compensation handling, Step Functions is preferable to hand-rolled orchestration because it makes state and failure paths explicit.
```

## 8.6 Step Functions and Saga Pattern

This is a strong answer in system design interviews:

- choreography: services react to events themselves
- orchestration: central coordinator drives the flow

Step Functions supports orchestration-style workflows.

---

# 10. Kinesis (for Surendra's data streaming interest)

## 10.1 What It Is

Amazon Kinesis is a family of services for real-time data streaming.

## 10.2 Kinesis Components

```
Kinesis Data Streams:
  ✦ Real-time ingestion and processing
  ✦ You manage consumers (Lambda, EC2, ECS)
  ✦ Retention: 1-365 days
  ✦ Shard-based scaling

Kinesis Data Firehose:
  ✦ Fully managed delivery to destinations
  ✦ S3, Redshift, Elasticsearch, Splunk
  ✦ Near real-time (buffers 60s-900s)
  ✦ Zero administration

Kinesis Data Analytics:
  ✦ SQL or Flink on streaming data
  ✦ Real-time transformations
```

## 10.3 Kinesis vs SQS vs Kafka

| Aspect | Kinesis | SQS | MSK (Kafka) |
|---|---|---|---|
| Model | Stream (ordered, replay) | Queue (consume & delete) | Stream |
| Ordering | Per shard | FIFO only | Per partition |
| Replay | Yes (retention window) | No | Yes |
| Consumer model | Multiple consumers read same data | One consumer per message | Multiple |
| Management | Managed | Fully managed | Semi-managed |
| Best for | Real-time analytics | Async tasks, decoupling | High-throughput streaming |

## 10.4 When to Use Kinesis

```
✓ Real-time analytics (clickstream, IoT, logs)
✓ Multiple consumers need same data stream
✓ Need to replay data within retention window
✓ Event sourcing patterns
✗ Simple async task processing (use SQS)
✗ Need infinite retention (use Kafka/MSK or S3)
```

## 10.5 Architecture Pattern: Log Analytics

```
Application logs
  → CloudWatch Logs
  → Subscription filter
  → Kinesis Firehose
  → S3 (Parquet, partitioned)
  → Athena / Redshift Spectrum
```

## 10.6 Interview Gold Answer

```
"For real-time clickstream analytics, I'd use Kinesis Data Streams
with Lambda consumers for sub-second processing, and Kinesis Firehose
for buffered delivery to S3 for batch analytics. If I just need
async task processing without replay, SQS is simpler."
```

---

# 11. WAF (Web Application Firewall)

## 11.1 What It Is

AWS WAF protects web applications from common web exploits.

It can be attached to:
- CloudFront
- ALB
- API Gateway
- AppSync

## 11.2 What WAF Protects Against

```
✓ SQL injection
✓ Cross-site scripting (XSS)
✓ Bad bots and scrapers
✓ Geographic blocking
✓ Rate limiting (request throttling)
✓ IP reputation filtering
```

## 11.3 WAF Components

- **Web ACL**: container for rules
- **Rules**: match conditions + action (allow/block/count)
- **Rule groups**: reusable sets of rules
- **Managed rules**: AWS or marketplace rule sets

## 11.4 Common Interview Pattern

```
Q: "How would you protect your API from attacks?"

A: "I'd put WAF on the ALB or API Gateway with:
   - AWS Managed Rules for common threats (SQL injection, XSS)
   - Rate-based rules to prevent DDoS/abuse
   - IP set rules for known bad actors
   - Geo restrictions if business requires
   And use Shield Standard (free) for network-layer DDoS."
```

## 11.5 WAF vs Security Groups vs NACLs

| Layer | Service | What it does |
|---|---|---|
| L3-4 | Security Groups | Instance-level stateful firewall |
| L3-4 | NACLs | Subnet-level stateless firewall |
| L7 | WAF | Application-layer HTTP rule filtering |
| L3-4 | Shield | DDoS protection |

---

# 12. High-Value Comparisons

## 9.1 IAM vs Cognito

| Service | Main purpose |
|---|---|
| IAM | AWS resource authorization |
| Cognito | end-user identity/authentication |

## 9.2 SQS vs SNS vs EventBridge

| Need | Best fit |
|---|---|
| durable queue with consumers pulling | SQS |
| simple fan-out pub/sub | SNS |
| event-bus routing with rule matching | EventBridge |

## 9.3 EventBridge vs Step Functions

| Need | Best fit |
|---|---|
| route events to targets | EventBridge |
| manage multi-step workflow with state | Step Functions |

## 9.4 SNS + SQS Pattern

Excellent when:

- one event has multiple downstream consumers
- each consumer needs isolated retry/failure behavior

---

# 10. Architecture Patterns

## 10.1 Order Processing

```
API
  ->
Order service
  ->
SNS topic
  ->
inventory queue -> inventory consumer
payment queue   -> payment consumer
email queue     -> notification consumer
```

Why strong:

- decoupled fan-out
- each downstream service scales independently
- failures isolated by queue

## 10.2 Event Routing Platform

```
Producer services
  ->
EventBridge bus
  ->
rules by event type
  ->
Lambda / Step Functions / SQS / other targets
```

Why strong:

- rule-based event routing
- central event backbone

## 10.3 Secure File Processing

```
User authenticates via Cognito
  ->
upload to S3
  ->
event triggers SQS or EventBridge
  ->
processor with IAM role
  ->
data encrypted using KMS-backed keys
```

---

# 11. Common Interview Traps

## Trap 1

"IAM is for app user login."

Correct:

Usually no. IAM governs AWS identities and permissions. Cognito handles end-user authentication for apps.

## Trap 2

"SQS guarantees exactly once."

Correct:

Standard queues are at-least-once. Design idempotent consumers.

## Trap 3

"SNS replaces queues."

Correct:

SNS is fan-out pub/sub. It does not replace durable per-consumer buffering the way SQS does.

## Trap 4

"EventBridge is just another queue."

Correct:

It is an event-routing bus, not a simple queue.

## Trap 5

"Encryption at rest means only KMS exists."

Correct:

KMS manages keys and integrates with services. The end-to-end security story includes IAM, policies, network controls, audit logs, and application design.

---

# 15. Rapid Revision Sheet

## One-Line Definitions

- `IAM`: who can do what in AWS
- `KMS`: key management for encryption
- `Secrets Manager`: managed secrets with rotation
- `Parameter Store`: config and secrets storage
- `Cognito`: app user authentication/identity
- `SQS`: durable queue
- `SNS`: fan-out pub/sub
- `EventBridge`: event bus with rule-based routing
- `Step Functions`: workflow orchestration
- `Kinesis`: real-time data streaming
- `WAF`: L7 web application firewall

## Questions You Must Be Able to Answer

- Why role-based access over static keys?
- When do you use SNS plus SQS together?
- When is EventBridge better than SNS?
- Why must SQS consumers be idempotent?
- When is Step Functions preferable to Lambda chaining?
- How does KMS fit into encryption at rest?
- Secrets Manager vs Parameter Store?
- Kinesis vs SQS for streaming?
- Where does WAF fit in the security stack?

## Decision Quick Reference

```
Need credential rotation?               → Secrets Manager
Simple config + budget-conscious?       → Parameter Store
Real-time stream with replay?           → Kinesis
Async task queue?                       → SQS
Fan-out to multiple consumers?          → SNS + SQS
Event routing by content?               → EventBridge
Multi-step workflow with state?         → Step Functions
L7 attack protection?                   → WAF
```

## Gold Standard Sentence

```
I separate security concerns into identity, authorization, and encryption,
and I separate integration concerns into queues, pub/sub, event routing,
streaming, and workflow orchestration so each failure mode stays explicit
and manageable.
```

