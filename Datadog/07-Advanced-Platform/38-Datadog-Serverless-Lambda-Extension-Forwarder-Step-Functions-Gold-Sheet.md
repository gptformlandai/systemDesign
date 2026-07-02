# 38. Datadog Serverless Monitoring: Lambda, Extension, Forwarder, Step Functions

## Goal

Understand how Datadog monitors serverless workloads across AWS Lambda, Step Functions, Google Cloud Run, Azure App Service, and similar managed runtimes.

---

## Mental Model

On hosts and Kubernetes, the Datadog Agent lives near the workload.

In serverless, there may be no host to install on.

```text
serverless function -> extension/layer/forwarder/cloud integration -> Datadog
```

The challenge is collecting metrics, logs, traces, cold starts, and errors from short-lived execution environments.

---

## Why Serverless Needs Special Monitoring

Serverless failures are different:

- Cold starts increase latency.
- Timeouts kill execution abruptly.
- Memory exhaustion may not produce normal app logs.
- Cost depends on duration, memory, invocations, and retries.
- Event sources hide failure paths.
- A workflow can fail across multiple functions.

---

## AWS Lambda Collection Options

| Option | Purpose | Notes |
|---|---|---|
| Cloud integration | CloudWatch metrics and metadata | baseline service metrics |
| Datadog Forwarder | Ships CloudWatch logs to Datadog | common legacy pattern |
| Datadog Lambda Extension | Sends telemetry directly from Lambda runtime | lower-latency richer collection |
| Datadog tracing library | Captures function traces and downstream calls | language-specific |
| Lambda layer | Packages Datadog library/extension | easier rollout |

Modern setups often prefer the Lambda Extension plus language tracing where supported.

---

## Key Lambda Signals

| Signal | Meaning |
|---|---|
| Invocations | Function execution count |
| Errors | Failed executions |
| Duration | Execution time |
| Cold starts | New runtime initialization |
| Timeouts | Function hit max duration |
| Out of memory | Memory limit exceeded |
| Throttles | Concurrency or account limit hit |
| Iterator age | Stream event processing lag |
| Estimated cost | Cost driven by duration and memory |

---

## Lambda Investigation Workflow

```text
Alert:
  checkout-authorizer Lambda p95 duration > 2s

Step 1: Check duration, error, timeout, memory, cold start metrics.
Step 2: Split by function version/alias.
Step 3: Inspect traces for downstream calls.
Step 4: Check logs around slow invocations.
Step 5: Verify memory utilization and CPU-bound behavior.
Step 6: Check concurrency and throttles.
Step 7: Compare before/after deployment.
```

---

## Cold Start Debugging

```text
Symptoms:
  p95 latency jumps but p50 is normal.
  cold_start:true traces are much slower.

Common causes:
  large package size
  heavy initialization code
  VPC ENI overhead
  low provisioned concurrency
  slow dependency initialization

Fixes:
  reduce package size
  lazy-load dependencies
  use provisioned concurrency for critical paths
  keep initialization outside hot request path carefully
```

---

## Step Functions Monitoring

Step Functions are distributed workflows. Monitor:

```text
executions started
executions succeeded
executions failed
execution duration
state transition failures
Lambda task errors
retry counts
DLQ messages
```

Trace the workflow as:

```text
API Gateway -> Lambda A -> Step Function -> Lambda B -> SQS -> Lambda C
```

The hard part is preserving correlation across async boundaries.

---

## Async Event Sources

| Source | Failure Risk | Monitor |
|---|---|---|
| SQS | backlog, DLQ growth | queue depth, age of oldest message |
| Kinesis | shard lag | iterator age |
| SNS | delivery failure | failed notifications |
| EventBridge | rule misrouting | failed invocations |
| DynamoDB Streams | lag and retries | iterator age, errors |

Serverless observability must include the event source, not just the function.

---

## Tagging Pattern

```text
service:checkout-authorizer
env:production
version:2026.07.03.1
team:checkout
functionname:checkout-authorizer
aws_account:prod-main
region:us-east-1
```

Without `service`, `env`, and `version`, serverless traces and logs become hard to correlate.

---

## Cost Monitoring

Serverless cost is often hidden until usage spikes.

```text
Cost drivers:
  invocations
  duration
  memory size
  provisioned concurrency
  retries
  event source fan-out

Monitor:
  duration by version
  error retries
  memory headroom
  provisioned concurrency utilization
```

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Monitoring only Lambda errors | Misses timeouts, throttles, lag | Monitor full serverless path |
| No trace propagation across async | Broken workflow view | Preserve context in message attributes |
| Ignoring cold starts | p95/p99 latency unexplained | Tag and compare cold vs warm |
| No version tag | Cannot identify bad deploy | Set version/alias tags |
| Forgetting DLQs | Failed events disappear | Monitor DLQ depth and age |

---

## Practical Question

> A checkout flow uses API Gateway, Lambda, SQS, and Step Functions. Users report intermittent slow orders, but function error rate is low. How do you debug?

---

## Strong Answer

I would look beyond function error rate. First I would check p95/p99 Lambda duration, cold starts, timeouts, throttles, and memory. Then I would inspect SQS queue age and DLQ depth because async backlog can make users wait even when Lambda errors are low. For Step Functions, I would check execution duration, failed states, retries, and task-level errors.

I would use distributed traces where possible and preserve context across message attributes so the request can be followed from API Gateway to Lambda to queue to workflow. I would split by version/alias to identify deploy regressions and compare cold versus warm invocations for latency.

---

## Interview Sound Bite

Serverless monitoring is about more than Lambda errors. You need cold starts, duration, timeouts, memory, throttles, async queue lag, DLQs, workflow state failures, trace propagation, and cost by function/version.
