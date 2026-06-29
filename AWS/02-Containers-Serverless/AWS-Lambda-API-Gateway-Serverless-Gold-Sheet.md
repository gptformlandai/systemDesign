# AWS Serverless: Lambda and API Gateway Gold Sheet

> Track: AWS Interview Track — Containers and Serverless
> Goal: understand Lambda deeply enough to design correct serverless architectures and avoid production pitfalls.

---

## 0. How To Read This

Beginner focus:
- Lambda triggers, execution role, timeout
- API Gateway basics, proxy integration
- Lambda pricing model

Intermediate focus:
- Cold start, provisioned concurrency
- Lambda concurrency limits and throttling
- Lambda layers, environment variables, secrets
- API Gateway REST vs HTTP API

Senior / MAANG focus:
- Lambda power tuning, memory vs cost vs latency
- Concurrency reservation vs provisioned concurrency
- Lambda in VPC trade-offs
- API Gateway throttling, usage plans, caching
- Lambda destinations vs SQS DLQ
- Event source mappings (SQS, Kinesis, DynamoDB Streams)

---

# Topic 1: Lambda

## 1. Intuition

Lambda runs code in response to events without you managing servers.

You upload a function, configure a trigger, and Lambda scales from zero to thousands of concurrent executions automatically.

Mental model:

```text
Event source -> Lambda trigger -> function invocation -> result/destination
```

Lambda billing = execution duration (per millisecond) + number of requests.

## 2. Lambda Execution Model

Every invocation runs inside an execution environment (micro-VM):

```text
Cold start: new environment initialized (100ms-2s depending on runtime and function size)
  -> download and initialize runtime
  -> load function code
  -> run initialization code (outside handler)
  -> run handler code

Warm invocation: existing environment reused
  -> run handler code only (no init overhead)
```

Cold start factors:
- runtime (Java worst, Python/Node.js better, SnapStart for Java)
- function package size (larger = slower init)
- VPC attachment (adds ~100ms for ENI provisioning, improved with VPC improvements)
- init code outside the handler (minimize heavy logic at init time)

## 3. Concurrency

| Concept | Meaning |
|---|---|
| Concurrent executions | number of function instances running simultaneously |
| Account-level limit | default 1,000 concurrent executions per region |
| Reserved concurrency | cap for one function (guarantees it doesn't starve others AND limits its max) |
| Provisioned concurrency | pre-initialized environments that eliminate cold starts |

Throttling behavior:

```text
If concurrency limit is reached:
  Synchronous invocation: 429 TooManyRequests error
  Asynchronous invocation: Lambda retries internally for up to 6 hours
  Event source mapping (SQS): Lambda stops polling, records stay in queue
```

## 4. Provisioned Concurrency

Pre-warms N execution environments so they are ready with zero cold start:

```text
Use provisioned concurrency when:
- API latency SLA requires <50ms response time
- Cold start tail latency is unacceptable
- Traffic patterns are predictable enough to pre-warm
- Java-based functions with heavy frameworks (Spring Boot on Lambda)

Cost:
- Provisioned concurrency = charged per hour (even if not invoked)
- On-demand invocations above provisioned = normal pricing
```

Auto scaling for provisioned concurrency:

```text
Application Auto Scaling can scale provisioned concurrency based on schedule or utilization.
Scale up before traffic spikes, scale down during off-hours.
```

## 5. Lambda SnapStart (Java)

Lambda SnapStart for Java reduces cold starts by:
- taking a snapshot of the initialized execution environment after init
- restoring from snapshot on cold start instead of reinitializing

Reduces Java cold start from 2-10 seconds to ~200-500ms.

Not available for all runtimes — Java 11+ on Lambda.

## 6. Lambda In VPC

Lambda in VPC lets functions access:
- RDS in private subnets
- ElastiCache in private subnets
- internal services not exposed publicly

Trade-offs:

```text
Pro: access to VPC resources (RDS, ElastiCache, internal services)
Con: function needs ENI in the subnet; NAT Gateway required for internet access
Con: increased cold start (historically; improved in 2020+ with hyperplane ENIs)
```

Architecture:

```text
Lambda in private subnet
-> RDS in private data subnet (same VPC)
-> Internet access: Lambda -> NAT Gateway -> Internet Gateway -> Internet
-> AWS services: Lambda -> VPC Interface Endpoint (no NAT)
```

## 7. Lambda Layers

Layers share code across functions:

```text
Use layers for:
- shared utilities and libraries
- custom runtimes
- dependency packages (avoid including in every function package)
- security/compliance libraries

A function can use up to 5 layers.
Layer ARN is immutable per version.
```

## 8. Environment Variables And Secrets

Environment variables:

```text
Use for: non-sensitive config (region, log level, feature flags)
Encrypted at rest with Lambda's KMS key (or your CMK)
Visible in Lambda console unless you use Secrets Manager
```

For secrets:

```text
Preferred: reference Secrets Manager at runtime
  -> fetch secret in function initialization code (outside handler)
  -> cache the value in the execution environment
  -> add cache TTL (re-fetch periodically to pick up rotations)

Do NOT: hardcode secrets in environment variables for sensitive values
```

## 9. Lambda Destinations And DLQ

Destinations route the result of async invocations:

| Destination Type | When | Target |
|---|---|---|
| On Success | async success | SQS, SNS, Lambda, EventBridge |
| On Failure | async failure after all retries | SQS, SNS, Lambda, EventBridge |

DLQ (Dead Letter Queue):
- only for asynchronous invocations
- sends failed invocations to SQS or SNS
- Lambda Destinations (on failure) is the preferred newer mechanism

## 10. Event Source Mappings

Lambda can poll event sources:

| Source | Behavior |
|---|---|
| SQS Standard | Lambda polls, processes up to 10 messages per batch (or 10,000 with batch window) |
| SQS FIFO | Lambda processes one message group at a time (ordering preserved) |
| Kinesis Data Streams | Lambda reads from shard, processes records in order per shard |
| DynamoDB Streams | Lambda processes change events in order per partition key |
| MSK / Kafka | Lambda reads from Kafka topic partitions |

SQS batch settings:

```text
BatchSize: 1-10,000 (larger = fewer invocations, more efficient)
MaximumBatchingWindowInSeconds: wait up to N seconds to fill batch
FunctionResponseTypes: ReportBatchItemFailures (allows partial batch success)
```

ReportBatchItemFailures — critical for SQS:

```text
Without it: if any message in a batch fails, the entire batch is retried (duplicates)
With it: return failed messageIds only; Lambda retries only those messages
```

---

# Topic 2: API Gateway

## 1. REST API vs HTTP API vs WebSocket API

| Feature | REST API | HTTP API | WebSocket API |
|---|---|---|---|
| Latency | higher | lower (~60% lower) | bidirectional |
| Cost | higher | lower (~70% cheaper) | based on connection time |
| Features | full (usage plans, WAF, caching) | simpler | persistent connections |
| Auth | Cognito, Lambda, IAM, API keys | JWT, Lambda, IAM | Lambda, IAM |
| Caching | yes (per stage) | no | no |

When to use:

```text
HTTP API: default for simple Lambda proxy, JWT auth, lower cost
REST API: when you need request validation, response mapping, per-method caching, API keys
WebSocket API: real-time bidirectional (chat, live updates, streaming)
```

## 2. Integration Types

| Type | What It Does | Use Case |
|---|---|---|
| Lambda Proxy (AWS_PROXY) | pass entire request to Lambda, Lambda controls response | most common for Lambda backends |
| HTTP Proxy | proxy request to HTTP backend | forward to ALB or any HTTP endpoint |
| AWS Service | integrate with AWS service directly | SQS SendMessage, S3 GetObject without Lambda |
| Mock | return a static response | testing, CORS preflight |

## 3. Throttling

| Level | Config | Default |
|---|---|---|
| Account/Region | burst: 5,000 RPS, steady: 10,000 RPS | shared across all APIs |
| API Stage | set per-stage rate and burst | override per stage |
| Method | set per-method rate and burst | fine-grained per endpoint |
| Usage Plan | limit by API key | control per-customer throughput |

429 Too Many Requests is returned when throttled.

## 4. API Gateway Caching

REST API caching per stage:
- cache TTL: 0 to 3600 seconds (default 300s)
- cache size: 0.5 GB to 237 GB
- cache key: full URI + headers + query strings (configurable)
- encrypted at rest

When useful:

```text
Cache expensive GET operations that return the same result within the TTL window.
Not useful for POST/PUT/DELETE or personalized user responses.
```

## 5. Request Validation And Mapping

REST API can validate requests before Lambda invocation:
- required query parameters
- required headers
- request body against a JSON Schema model

Reduces Lambda invocations for invalid requests.

Mapping templates (VTL):
- transform request body before Lambda
- transform Lambda response before returning to client
- less needed with Lambda Proxy integration

## 6. API Keys And Usage Plans

For external API monetization or rate limiting per consumer:

```text
Usage Plan:
  Throttle: 100 RPS per key
  Quota: 10,000 requests per day per key

API Key:
  Customer A gets key with plan A
  Customer B gets key with plan B
```

## 7. Lambda Cold Start Impact On API Response

For latency-sensitive APIs:

```text
p50 latency = warm invocations (fast)
p99 latency = cold starts (potentially 2-5x slower)

Mitigation:
1. Use provisioned concurrency for critical APIs
2. Use Lambda SnapStart for Java
3. Minimize function package size
4. Move heavy init code outside handler
5. Use HTTP API instead of REST API (lower overhead)
6. Use shorter VPC ENI provisioning with Hyperplane
```

## 8. Lambda Power Tuning

Memory controls both RAM and proportional CPU allocation:

```text
128 MB = least RAM, slowest CPU
10,240 MB = maximum RAM, maximum CPU

Increasing memory often reduces execution duration enough to lower total cost.
A 1,024 MB function that runs in 200ms may be cheaper than a 128 MB function
that takes 1,500ms to do the same work.
```

Use AWS Lambda Power Tuning (open-source Step Functions state machine) to find the optimal memory setting for cost or performance.

## 9. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Heavy framework init inside handler | move DB connections and config to outside handler |
| No provisioned concurrency for latency-critical API | provision or SnapStart for Java |
| Lambda timeout set to 15 minutes by default | set realistic timeout matching expected work |
| No DLQ/destination for async failures | configure Lambda Destination on failure |
| Process SQS batch without ReportBatchItemFailures | enable partial batch success to avoid duplicates |
| Lambda in VPC without VPC endpoints | add VPC endpoints to avoid NAT costs for AWS service calls |
| No throttling on API Gateway | set stage-level rate limit to protect Lambda concurrency |

## 10. Interview Scenario

**Scenario**: "An API endpoint using Lambda is slow for the first user of the morning. Why and how do you fix it?"

Strong answer:

```text
That is a cold start. Lambda shuts down idle execution environments. The first request
initializes a new one, causing 1-5 seconds of latency for Java, ~200ms for Node/Python.

Fixes:
1. Provisioned concurrency: pre-warm N environments. No cold start for those requests.
2. Lambda SnapStart for Java: snapshot after init, restore from snapshot on cold start.
3. Reduce function package size: smaller packages initialize faster.
4. Move DB connections and heavy init outside the handler: amortize across many invocations.
5. If cold start is infrequent and acceptable: keep as-is, not every API needs P99 guarantees.
```

## 11. Revision Notes

- Lambda: pay per millisecond + requests; cold start affects p99 latency
- Provisioned concurrency: eliminates cold starts, but costs even when idle
- SnapStart: Java-specific, snapshots init state — reduces Java cold start by 80%+
- Reserved concurrency: both a cap and a guarantee
- API Gateway: HTTP API for simple/cheap; REST API for validation/caching/usage plans
- SQS + Lambda: always use ReportBatchItemFailures for partial batch retries
- Lambda in VPC: add VPC Interface Endpoints for AWS services to avoid NAT cost

## 12. Official Source Notes

- Lambda: <https://docs.aws.amazon.com/lambda/latest/dg/welcome.html>
- Lambda concurrency: <https://docs.aws.amazon.com/lambda/latest/dg/configuration-concurrency.html>
- Lambda SnapStart: <https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html>
- API Gateway: <https://docs.aws.amazon.com/apigateway/latest/developerguide/welcome.html>
- Lambda Power Tuning: <https://github.com/alexcasalboni/aws-lambda-power-tuning>
