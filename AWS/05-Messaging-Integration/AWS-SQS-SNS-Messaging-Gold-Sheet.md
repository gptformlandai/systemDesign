# AWS Messaging: SQS and SNS Gold Sheet

> Track: AWS Interview Track — Messaging and Integration
> Goal: design correct asynchronous messaging architectures with SQS and SNS and explain ordering, fan-out, and failure handling.

---

## 0. How To Read This

Beginner focus:
- SQS Standard vs FIFO
- SNS topics and subscriptions
- Basic fan-out pattern (SNS → SQS)

Intermediate focus:
- Visibility timeout, DLQ configuration
- SQS long polling vs short polling
- SNS message filtering policies
- SQS FIFO throughput limits

Senior / MAANG focus:
- SQS with Lambda event source mapping (partial batch failure)
- SNS FIFO with SQS FIFO fan-out
- SQS delay queues vs message timers
- Poison message handling (DLQ + alarm + manual review)
- Large message pattern (S3 + SQS pointer)
- SQS + DynamoDB idempotency pattern
- SNS Extended Client Library for >256 KB messages

---

# Topic 1: Amazon SQS — Simple Queue Service

## 1. Intuition

SQS decouples producers from consumers. Producers write messages, consumers read and process them at their own pace.

Mental model:

```text
Producer -> [SQS Queue] -> Consumer

Producer and Consumer are fully decoupled:
- consumer can be down, messages wait
- producer can burst, messages buffer
- multiple consumers can process in parallel
```

## 2. SQS Standard vs FIFO

| Feature | Standard Queue | FIFO Queue |
|---|---|---|
| Ordering | at-least-once, best-effort ordering | exactly-once processing, strict FIFO per message group |
| Delivery | at-least-once (duplicate possible) | exactly-once in-order |
| Throughput | unlimited | 300 messages/sec (3,000 with batching) |
| Deduplication | not built-in | built-in (MessageDeduplicationId, 5-min window) |
| Name | any name | must end in `.fifo` |
| Use case | high throughput, order not critical | order matters, financial, inventory |

SQS Standard duplicate risk:

```text
Standard queue guarantees at-least-once delivery.
A message may be delivered twice (rare but happens).
Consumers must be idempotent: processing the same message twice produces the same result.
```

FIFO message groups:

```text
MessageGroupId = "order-123"
Messages with the same MessageGroupId are delivered in order.
Messages with different MessageGroupId are processed in parallel.
FIFO queue can have thousands of message groups.
```

## 3. Visibility Timeout

When a consumer reads a message:
- message becomes invisible to other consumers for `VisibilityTimeout` seconds
- consumer has that time to process and delete the message
- if consumer fails and does not delete: message reappears and can be reprocessed

Default: 30 seconds. Set longer than your maximum processing time.

Change timeout for a specific message while processing:

```python
# Extend visibility if processing will take longer
sqs.change_message_visibility(
    QueueUrl=queue_url,
    ReceiptHandle=receipt_handle,
    VisibilityTimeout=120  # extend by 2 minutes
)
```

## 4. Dead Letter Queue (DLQ)

DLQ receives messages that fail to process after `maxReceiveCount` attempts:

```text
Main Queue -> consumer fails -> message returns -> consumer fails again (up to maxReceiveCount)
-> message moved to DLQ

DLQ Config:
  maxReceiveCount: 3 (retry 3 times before DLQ)
  DLQ must be same type as source (FIFO DLQ for FIFO source)
```

DLQ best practices:

```text
1. Always configure a DLQ for production queues
2. Set CloudWatch alarm on DLQ ApproximateNumberOfMessages > 0
3. Log failed messages from DLQ with enough context to diagnose
4. Create manual review process: fix root cause, then move messages back
5. Set DLQ retention to max (14 days) to avoid losing messages
```

Move messages from DLQ back to source (dead-letter queue redrive):

```bash
aws sqs start-message-move-task \
  --source-arn arn:aws:sqs:us-east-1:...:my-queue-dlq \
  --destination-arn arn:aws:sqs:us-east-1:...:my-queue
```

## 5. Long Polling vs Short Polling

| Mode | Behavior | Cost |
|---|---|---|
| Short polling | returns immediately, even if queue is empty | many empty responses = more API calls = more cost |
| Long polling | waits up to 20 seconds for a message to arrive | fewer API calls, lower cost |

Always use long polling for consumers:

```python
# Long polling: wait up to 20 seconds
response = sqs.receive_message(
    QueueUrl=queue_url,
    MaxNumberOfMessages=10,
    WaitTimeSeconds=20  # long polling
)
```

## 6. SQS With Lambda

Lambda event source mapping polls SQS automatically:

```text
Lambda polls SQS -> receives batch -> invokes function with batch
Function processes batch -> function returns success -> Lambda deletes messages
Function throws exception -> Lambda retries full batch (problem: duplicates)

Fix: ReportBatchItemFailures
```

SQS event source with partial batch failure:

```python
def lambda_handler(event, context):
    failed_ids = []
    
    for record in event['Records']:
        try:
            process(record['body'])
        except Exception as e:
            failed_ids.append({'itemIdentifier': record['messageId']})
    
    return {'batchItemFailures': failed_ids}
```

Lambda Event Source Mapping config:

```json
{
  "EventSourceArn": "arn:aws:sqs:...:my-queue",
  "BatchSize": 10,
  "MaximumBatchingWindowInSeconds": 5,
  "FunctionResponseTypes": ["ReportBatchItemFailures"]
}
```

## 7. Large Message Pattern

SQS has a 256 KB message size limit.

For large payloads (up to 2 GB):

```text
Producer:
  1. Upload payload to S3
  2. Send SQS message with S3 reference: {bucket: "...", key: "...", size: "..."}

Consumer:
  1. Receive SQS message
  2. Read payload from S3
  3. Process
  4. Delete S3 object
  5. Delete SQS message

AWS SDK: Extended Client Library for Java/Python handles this automatically
```

## 8. SQS Key Numbers

| Setting | Value |
|---|---|
| Max message size | 256 KB |
| Max retention period | 14 days |
| Default retention | 4 days |
| Max visibility timeout | 12 hours |
| Max long polling wait | 20 seconds |
| Max batch size (receive) | 10 messages |
| FIFO throughput | 300 msg/sec base, 3,000 with batching |

---

# Topic 2: Amazon SNS — Simple Notification Service

## 1. Intuition

SNS is a pub/sub service. A producer publishes to a topic. All subscribers receive the message.

SNS is push-based. Subscribers receive messages immediately without polling.

## 2. SNS Subscribers

| Subscriber Type | Delivery |
|---|---|
| SQS | message pushed to SQS queue |
| Lambda | Lambda invoked with message |
| HTTP/HTTPS | HTTP POST to your endpoint |
| Email/Email-JSON | email to address |
| SMS | text to phone number |
| Kinesis Firehose | stream to S3, Redshift, etc. |
| Mobile Push (APNS, GCM) | push to mobile device |

## 3. SNS Fan-Out Pattern

SNS → multiple SQS queues is the most important SNS pattern:

```text
OrderService publishes to SNS topic: order-events

SQS Queue: inventory-service (subscriber)
SQS Queue: notification-service (subscriber)
SQS Queue: analytics-service (subscriber)

All three receive the same order event.
Each processes independently at its own pace.
Failure in one does not affect others.
```

Benefits:
- decouples producers from all downstream consumers
- consumers added without changing producer
- each consumer has its own DLQ for failure isolation

## 4. SNS Message Filtering

Subscriptions can filter messages by message attributes:

```json
{
  "eventType": ["ORDER_PLACED", "ORDER_CANCELLED"],
  "region": [{"prefix": "us-"}]
}
```

Without filtering: all subscribers receive all messages.
With filtering: subscriber only receives messages matching their policy.

Message attribute on publish:

```json
{
  "eventType": {
    "DataType": "String",
    "StringValue": "ORDER_PLACED"
  }
}
```

## 5. SNS FIFO

SNS FIFO with SQS FIFO:

```text
Use when:
- fan-out AND strict message ordering required
- financial transactions (debit + credit events in order)

SNS FIFO -> SQS FIFO 1 (subscribed, filtered)
         -> SQS FIFO 2 (subscribed, different filter)

Each SQS FIFO queue receives messages in order.
SNS FIFO throughput: same as SQS FIFO limits.
```

## 6. SNS vs SQS vs EventBridge

| Feature | SQS | SNS | EventBridge |
|---|---|---|---|
| Pattern | queue (pull) | topic (push) | event bus (rule-based routing) |
| Fan-out | no (one consumer) | yes | yes |
| Filtering | no (all messages) | message attribute filter | pattern matching on event body |
| Targets | consumer polls | subscriptions | many AWS services, APIs, SaaS |
| Replay | no (after visibility) | no | yes (event archive + replay) |
| Schema registry | no | no | yes |
| FIFO | yes | yes | no |
| Dead-letter | DLQ | DLQ per subscription | dead-letter queue per rule |

Interview decision:

```text
Use SQS: point-to-point decoupling, worker queue pattern, rate limiting consumption
Use SNS: fan-out to multiple consumers without coordination
Use EventBridge: event-driven routing across multiple services, SaaS integrations, complex patterns
```

## 7. Common Mistakes

| Mistake | Better Approach |
|---|---|
| No DLQ on production SQS queue | always configure DLQ + alarm |
| Visibility timeout shorter than processing time | set visibility timeout > max processing time |
| No idempotency for Standard queue consumers | all SQS consumers must handle duplicate delivery |
| SQS batch without ReportBatchItemFailures | enable for Lambda; prevents full batch retry on partial failures |
| Short polling (default) | use WaitTimeSeconds=20 for long polling |
| SNS without SQS buffer | direct SNS → Lambda loses messages if Lambda throttled; add SQS |
| No message filtering on SNS | use filtering to reduce downstream processing, lower cost |

## 8. Interview Scenario

**Scenario**: "Design the messaging layer for an e-commerce order system where placing an order must trigger inventory reservation, email notification, and analytics."

Strong answer:

```text
When an order is placed:
1. OrderService publishes to SNS topic: arn:aws:sns:...:order-events

SNS fans out to:
- SQS Queue: inventory-events -> InventoryService Lambda (reserves stock)
- SQS Queue: notification-events -> NotificationService Lambda (sends email via SES)
- SQS Queue: analytics-events -> AnalyticsService Lambda (writes to data lake)

Each queue has:
- Visibility timeout: 120 seconds (enough for processing)
- DLQ: dedicated DLQ per service
- CloudWatch alarm: DLQ > 0 -> PagerDuty alert

SNS message filtering: InventoryService only receives ORDER_PLACED, not ORDER_SHIPPED.
NotificationService receives ORDER_PLACED, ORDER_SHIPPED, ORDER_CANCELLED.

All consumers are idempotent: use orderID as idempotency key in DynamoDB.
```

## 9. Revision Notes

- SQS Standard: unlimited throughput, at-least-once (idempotent consumers required)
- SQS FIFO: exactly-once, strict order per MessageGroupId, 300 msg/sec base
- Visibility timeout: always set longer than max processing time
- DLQ: always configure; alarm on DLQ queue depth; 14-day retention
- Long polling: WaitTimeSeconds=20; reduces cost, latency
- Lambda + SQS: always use ReportBatchItemFailures for partial batch success
- SNS fan-out: one SNS topic → multiple SQS queues; decouples all downstream services
- SNS filtering: subscriptions can filter by message attributes

## 10. Official Source Notes

- SQS: <https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/welcome.html>
- SQS FIFO: <https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html>
- SNS: <https://docs.aws.amazon.com/sns/latest/dg/welcome.html>
- SNS message filtering: <https://docs.aws.amazon.com/sns/latest/dg/sns-message-filtering.html>
- SQS DLQ: <https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html>
