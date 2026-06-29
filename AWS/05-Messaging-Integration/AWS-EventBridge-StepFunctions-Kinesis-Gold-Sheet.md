# AWS Integration: EventBridge, Step Functions, and Kinesis Gold Sheet

> Track: AWS Interview Track — Messaging and Integration
> Goal: design event-driven architectures with EventBridge, orchestrate workflows with Step Functions, and stream real-time data with Kinesis.

---

## 0. How To Read This

Beginner focus:
- EventBridge events, rules, targets
- Step Functions state types
- Kinesis Data Streams basics

Intermediate focus:
- EventBridge pattern matching
- Step Functions retry/catch, error handling
- Kinesis shards, partition keys, consumer fanout
- EventBridge Scheduler

Senior / MAANG focus:
- EventBridge Pipes (filtering + enrichment + routing)
- Step Functions Express vs Standard workflows
- Kinesis enhanced fan-out
- Kinesis vs SQS vs Kafka trade-offs
- Step Functions integration with Lambda, DynamoDB, ECS, Bedrock
- Event replay and archive in EventBridge

---

# Topic 1: Amazon EventBridge

## 1. Intuition

EventBridge is a serverless event bus that routes events from sources to targets based on rules.

It replaces CloudWatch Events and adds SaaS integrations, custom event buses, schema registry, event archive, and Pipes.

```text
Event Source -> EventBridge Bus -> Rules -> Target(s)

Rules evaluate event patterns and route matching events to one or more targets.
```

## 2. Event Buses

| Bus Type | Source | Use Case |
|---|---|---|
| Default | AWS services, CloudTrail | react to AWS events (EC2 state change, S3 upload) |
| Custom | your apps, microservices | event-driven microservices within your account |
| Partner | SaaS providers (Stripe, Salesforce, Zendesk) | react to SaaS events directly in AWS |

## 3. Event Structure

```json
{
  "version": "0",
  "id": "event-id",
  "source": "com.mycompany.orders",
  "detail-type": "OrderPlaced",
  "account": "123456789012",
  "region": "us-east-1",
  "time": "2025-01-15T10:30:00Z",
  "detail": {
    "orderId": "o-123",
    "customerId": "c-456",
    "amount": 99.99,
    "items": [...]
  }
}
```

## 4. Event Pattern Matching

EventBridge rules match on event fields:

Simple match:

```json
{
  "source": ["com.mycompany.orders"],
  "detail-type": ["OrderPlaced"]
}
```

With conditions:

```json
{
  "source": ["com.mycompany.orders"],
  "detail-type": ["OrderPlaced"],
  "detail": {
    "amount": [{"numeric": [">", 100]}],
    "region": ["us-east-1", "eu-west-1"]
  }
}
```

Content filtering operators:
- `prefix` — string starts with
- `numeric` — number comparison
- `exists` — field exists
- `anything-but` — NOT in list
- `equals-ignore-case` — case-insensitive string match

## 5. Targets

One rule can have up to 5 targets:

| Target | Example Use |
|---|---|
| Lambda | invoke function with event payload |
| SQS | enqueue event for async processing |
| SNS | fan out to multiple subscribers |
| Step Functions | start workflow execution |
| ECS task | run a container task |
| API Gateway | HTTP POST to REST API |
| Kinesis | stream event data |
| Event bus (another account) | cross-account routing |
| Bedrock | trigger AI workflow |

## 6. Event Archive And Replay

Archive events to replay later:

```text
Archive: store all events matching a filter (e.g., all order events) for 30 days
Replay: send archived events back to the bus as if they just happened

Use case:
- Add a new service that needs to process past events
- Bug fix: replay events a faulty consumer mishandled
- Test new consumer against production event history
```

## 7. EventBridge Scheduler

Serverless cron/rate-based scheduling (replaced CloudWatch Events rules for scheduling):

```text
Schedule: rate(5 minutes) or cron(0 9 * * ? *)
Target: Lambda, Step Functions, any API destination

Benefits over CloudWatch Events:
- up to 100 million schedules
- time-zone aware
- one-time schedules (future timestamp)
- flexible windows (delivery within X minutes of target time)
```

## 8. EventBridge Pipes

Pipes connect a source to a target with optional filtering and enrichment:

```text
Source (SQS, Kinesis, DynamoDB Streams, Kafka)
-> Filtering (optional, filter events before enrichment)
-> Enrichment (optional: Lambda, Step Functions, API call)
-> Target (EventBridge bus, SQS, SNS, Lambda, HTTP endpoint)

Use case:
  DynamoDB Streams -> Pipe -> filter only INSERT events -> Lambda enrichment -> SQS target
  Kinesis -> Pipe -> filter by field -> EventBridge custom bus -> multiple rules and targets
```

---

# Topic 2: AWS Step Functions

## 1. Intuition

Step Functions orchestrates workflows as sequences of steps (states) with branching, error handling, retries, and waits.

Use Step Functions when:
- you need to coordinate multiple Lambda functions or AWS services
- you need human approval steps
- you need wait (pause and resume) behavior
- you want visibility into workflow execution history
- you need complex retry logic across different steps

Do NOT use for simple Lambda → Lambda chaining (just call the next function in code).

## 2. State Types

| State | Purpose | Example |
|---|---|---|
| Task | invoke Lambda, call AWS SDK, call HTTP endpoint | payment processing |
| Choice | branch based on condition | if amount > 100 -> approve; else -> auto-approve |
| Wait | pause for duration or until timestamp | wait 24 hours |
| Parallel | run branches in parallel, wait for all | send email AND update inventory in parallel |
| Map | iterate over items in array | process each order item |
| Pass | pass input to output (testing, transformation) | inject test data |
| Succeed | end workflow successfully | order complete |
| Fail | end workflow with error | unrecoverable error |

## 3. Standard vs Express Workflows

| Feature | Standard Workflow | Express Workflow |
|---|---|---|
| Execution time | up to 1 year | up to 5 minutes |
| Execution history | yes (90 days in console) | no (must log to CloudWatch) |
| Pricing | per state transition | per execution duration + requests |
| Execution rate | 2,000/sec | 100,000/sec |
| Guarantees | exactly-once | at-least-once |
| Use case | long-running business processes | high-volume, short-lived |

Interview answer:

```text
Standard Workflow for business processes: order fulfillment, loan approval, user onboarding.
Express Workflow for high-throughput event processing: IoT data pipeline, real-time fraud check.
```

## 4. Error Handling: Retry And Catch

Retry failed task automatically:

```json
{
  "Type": "Task",
  "Resource": "arn:aws:lambda:...:PaymentProcessor",
  "Retry": [
    {
      "ErrorEquals": ["Lambda.ServiceException", "Lambda.TooManyRequestsException"],
      "IntervalSeconds": 2,
      "MaxAttempts": 3,
      "BackoffRate": 2,
      "JitterStrategy": "FULL"
    }
  ],
  "Catch": [
    {
      "ErrorEquals": ["PaymentDeclinedException"],
      "Next": "SendPaymentFailureNotification"
    },
    {
      "ErrorEquals": ["States.ALL"],
      "Next": "HandleGenericFailure"
    }
  ]
}
```

Retry configuration:
- `IntervalSeconds`: wait before first retry
- `MaxAttempts`: max retry count
- `BackoffRate`: exponential factor (2 = double each time)
- `JitterStrategy`: FULL adds randomness to prevent thundering herd

## 5. AWS SDK Integration (Without Lambda)

Step Functions can call AWS SDK directly (no Lambda needed):

```json
{
  "Type": "Task",
  "Resource": "arn:aws:states:::dynamodb:putItem",
  "Parameters": {
    "TableName": "Orders",
    "Item": {
      "OrderId": {"S.$": "$.orderId"},
      "Status": {"S": "PROCESSING"}
    }
  }
}
```

Supported: DynamoDB, S3, SQS, SNS, ECS, Bedrock, SageMaker, and 200+ more services.

Use SDK integration for simple AWS service calls to avoid Lambda overhead.

## 6. Human Approval Pattern

Step Functions with SQS for human approval:

```text
Step 1: SendApprovalEmailTask (Lambda sends email with approval/rejection link)
Step 2: WaitForApproval state (waits for callback token)
Step 3: Human clicks approve -> callback API sends task success with token
Step 4: WorkflowContinues

Callback token:
  $$.Task.Token injected into task
  Lambda embeds token in approval link
  API handler calls: sfn:SendTaskSuccess(taskToken, output) or sfn:SendTaskFailure
```

---

# Topic 3: Amazon Kinesis

## 1. Kinesis Family

| Service | What It Does |
|---|---|
| Kinesis Data Streams | real-time streaming, retain and replay records |
| Kinesis Data Firehose | managed delivery to S3, Redshift, OpenSearch |
| Kinesis Data Analytics | SQL or Flink queries on streams |
| Kinesis Video Streams | video streaming |

## 2. Kinesis Data Streams

Concepts:

| Concept | Meaning |
|---|---|
| Stream | collection of shards |
| Shard | one unit of capacity (1 MB/s write, 2 MB/s read) |
| Partition Key | determines which shard the record goes to (hash-based) |
| Sequence Number | unique identifier within a shard, in order |
| Retention | 24 hours default, up to 365 days |
| Consumer | reads from shards |

Capacity planning:

```text
Required shards = max(
  ceil(write MB/s / 1 MB),
  ceil(read MB/s / 2 MB),
  ceil(records/s / 1000)
)
```

## 3. Enhanced Fan-Out

| Mode | Read Throughput | Latency | Cost |
|---|---|---|---|
| Standard Consumer (GetRecords) | shared 2 MB/s per shard | up to 200ms | free |
| Enhanced Fan-Out (SubscribeToShard) | 2 MB/s per consumer per shard | ~70ms | extra per shard-hour |

Use enhanced fan-out when:
- multiple consumers read the same stream
- each needs full 2 MB/s throughput
- low latency matters

## 4. Kinesis vs SQS vs Kafka

| Feature | Kinesis Data Streams | SQS | Kafka |
|---|---|---|---|
| Ordering | per shard (partition) | best-effort (Standard), FIFO | per partition |
| Consumer groups | yes (different starting points) | each message one consumer | yes |
| Replay | yes (retention period) | no (once consumed+deleted) | yes (offset management) |
| Retention | up to 365 days | up to 14 days | configurable (unlimited on tiered storage) |
| Throughput | 1 MB/s per shard | unlimited | very high |
| Partitioning | partition key → shard | none (Standard) | partition key |
| Self-hosted | no | no | yes (MSK managed) |
| Use case | real-time analytics, log streaming, event replay | task queues, decoupling | high-scale event streaming, Kafka ecosystem |

## 5. Kinesis Data Firehose

Fully managed stream delivery — no consumer code needed:

```text
Sources:
  Kinesis Data Streams, direct PUT, MSK, EventBridge, IoT Core

Destinations:
  S3, Amazon Redshift, Amazon OpenSearch, HTTP endpoint, Splunk, Datadog

Processing:
  Lambda function for transformation before delivery
  Data format conversion (JSON → Parquet, JSON → ORC)

Buffering:
  Buffer by size (1-128 MB) or by time (60-900 seconds)
  Delivers when either threshold met
```

Use Firehose for:
- streaming logs from Lambda / EC2 → S3 for archiving
- streaming events → Redshift for analytics
- streaming → OpenSearch for real-time search

## 6. Common Mistakes

| Mistake | Better Approach |
|---|---|
| EventBridge rule with target = Lambda (no retry) | Lambda failed invocations → EventBridge sends to DLQ if configured |
| Step Functions calling Lambda for simple DynamoDB writes | use SDK integration directly |
| Kinesis shard count guessed at creation | calculate from throughput, use UpdateShardCount to scale |
| Same partition key for all Kinesis records | hot shard: spread with high-cardinality partition key |
| Kinesis standard consumer with many consumers | use enhanced fan-out for multiple high-throughput consumers |
| Step Functions Standard for high-volume, fast workflows | use Express Workflow for >100k executions/second |
| No event archive on EventBridge | enable archive for event replay capability |

## 7. Interview Scenario

**Scenario**: "Design a real-time fraud detection pipeline."

Strong answer:

```text
Transaction events published to Kinesis Data Streams (partition key = accountId)
  -> Shard per accountId: all transactions for account in order

Two consumers:
1. Real-time fraud check: Lambda with enhanced fan-out
   -> reads in <70ms per record
   -> calls SageMaker real-time endpoint for ML fraud score
   -> if fraud_score > 0.9: publishes fraud alert to EventBridge
   
2. EventBridge rule on fraud alert:
   -> target 1: Step Functions workflow (freeze account, notify compliance, send alert)
   -> target 2: SQS queue -> manual review team

3. Kinesis Data Firehose (separate consumer)
   -> buffers records, converts to Parquet
   -> delivers to S3 data lake every 5 minutes
   -> Athena queries for daily fraud analysis reports

Replay: if fraud model is updated, replay last 7 days of Kinesis events
through the new model via S3 batch or Kinesis stream re-processing.
```

## 8. Revision Notes

- EventBridge: route by pattern; custom/default/partner buses; archive for replay
- EventBridge Pipes: filter → enrich → route for single-source-to-single-target pipeline
- EventBridge Scheduler: serverless cron, 100M schedules, timezone-aware
- Step Functions: Standard for long-running business workflows; Express for high-volume short
- Step Functions retry: exponential backoff with jitter; Catch for error routing
- Kinesis: 1 MB/s write + 2 MB/s read per shard; partition key determines shard
- Enhanced fan-out: 2 MB/s per consumer per shard vs shared 2 MB/s for standard
- Firehose: managed delivery to S3/Redshift/OpenSearch, no consumer code

## 9. Official Source Notes

- EventBridge: <https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html>
- Step Functions: <https://docs.aws.amazon.com/step-functions/latest/dg/welcome.html>
- Kinesis Data Streams: <https://docs.aws.amazon.com/streams/latest/dev/introduction.html>
- Kinesis Firehose: <https://docs.aws.amazon.com/firehose/latest/dev/what-is-this-service.html>
- EventBridge Pipes: <https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-pipes.html>
