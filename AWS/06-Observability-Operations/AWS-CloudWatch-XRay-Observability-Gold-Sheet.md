# AWS Observability: CloudWatch and X-Ray Gold Sheet

> Track: AWS Interview Track — Observability and Operations
> Goal: design complete observability for AWS workloads using metrics, logs, traces, and alarms.

---

## 0. How To Read This

Beginner focus:
- CloudWatch metrics and alarms
- CloudWatch Logs basics
- X-Ray trace concept

Intermediate focus:
- CloudWatch metric filters, log insights queries
- X-Ray segments, subsegments, sampling
- Container Insights, Lambda Insights
- Custom metrics (EMF format)

Senior / MAANG focus:
- Anomaly detection alarms
- CloudWatch Synthetics (canaries)
- ServiceLens integration of traces + metrics
- Composite alarms
- Metric math and dashboard design
- CloudWatch Logs subscription filters for streaming
- X-Ray trace analysis for latency outliers

---

# Topic 1: CloudWatch Metrics

## 1. Intuition

CloudWatch Metrics stores time-series data about AWS resource performance.

Every AWS service publishes metrics automatically. You add custom metrics for application-level indicators.

Structure:

```text
Namespace: AWS/EC2 (built-in) or MyApp/Orders (custom)
Metric Name: CPUUtilization, OrdersPlaced, PaymentLatency
Dimensions: InstanceId=i-1234, Environment=prod

A unique metric = namespace + metric name + all dimensions
```

## 2. Built-In Metrics By Service

| Service | Key Metrics |
|---|---|
| EC2 | CPUUtilization, NetworkIn/Out, DiskReadOps/WriteOps |
| ALB | RequestCount, TargetResponseTime, HTTPCode_Target_5XX_Count |
| Lambda | Invocations, Errors, Duration, Throttles, ConcurrentExecutions |
| SQS | NumberOfMessagesSent, ApproximateNumberOfMessages, NumberOfMessagesDeleted |
| RDS | CPUUtilization, DatabaseConnections, FreeableMemory, ReadLatency, WriteLatency |
| DynamoDB | ConsumedReadCapacityUnits, SystemErrors, SuccessfulRequestLatency |
| ECS | CpuUtilized, MemoryUtilized, RunningTaskCount |

## 3. Custom Metrics

Publish custom business metrics from your application:

```python
import boto3

cloudwatch = boto3.client('cloudwatch')

cloudwatch.put_metric_data(
    Namespace='MyApp/Orders',
    MetricData=[
        {
            'MetricName': 'OrdersPlaced',
            'Dimensions': [
                {'Name': 'Environment', 'Value': 'prod'},
                {'Name': 'Region', 'Value': 'us-east-1'}
            ],
            'Value': 1,
            'Unit': 'Count'
        }
    ]
)
```

Embedded Metric Format (EMF) — faster, no extra SDK call:

```python
import json

def log_metric(metric_name, value, unit='Count'):
    print(json.dumps({
        "_aws": {
            "Timestamp": int(time.time() * 1000),
            "CloudWatchMetrics": [{
                "Namespace": "MyApp/Orders",
                "Dimensions": [["Environment"]],
                "Metrics": [{"Name": metric_name, "Unit": unit}]
            }]
        },
        "Environment": "prod",
        metric_name: value
    }))

# Lambda: print to stdout -> CloudWatch Logs -> EMF parser -> metric automatically created
log_metric("OrdersPlaced", 1)
log_metric("PaymentLatency", 250, "Milliseconds")
```

EMF advantages:
- no extra API call (metric extracted from logs by CloudWatch agent)
- same structured log line creates both log and metric
- zero additional Lambda overhead

## 4. CloudWatch Alarms

| Alarm Type | How It Works | Use Case |
|---|---|---|
| Static threshold | value > or < fixed number | CPU > 80%, error rate > 5% |
| Anomaly detection | ML detects deviation from normal band | detect unusual traffic patterns |
| Composite alarm | combines multiple alarms with AND/OR | fire only if both CPU high AND latency high |
| Metric math alarm | alarm on computed expression | error_rate = errors/requests > 0.01 |

Alarm states:
- OK: metric within threshold
- ALARM: metric outside threshold
- INSUFFICIENT_DATA: not enough data points

Alarm actions:
- SNS notification (email, SMS, Lambda, Slack via webhook)
- Auto Scaling action (scale in/out)
- EC2 action (stop, reboot, recover)
- Systems Manager OpsCenter (create OpsItem)

Multi-period alarm:

```text
Alarm condition: CPUUtilization > 80% for 3 out of 5 consecutive 5-minute periods

This prevents single-spike false alerts.
```

## 5. Anomaly Detection

CloudWatch learns metric patterns from historical data:

```text
Band = expected range based on day-of-week, time-of-day patterns
Alarm fires when: metric falls outside the expected band

Use for:
- request rate (expect lower traffic at 3 AM)
- latency (expect slightly higher on Monday mornings)
- error rate deviation from normal

More reliable than static thresholds for metrics with natural cycles.
```

## 6. Composite Alarms

Reduce alert noise by combining alarms:

```json
AlarmRule: "ALARM(high-cpu) AND ALARM(high-latency)"
```

Fire only when BOTH conditions are true. Prevents paging on CPU spike that doesn't affect latency.

---

# Topic 2: CloudWatch Logs

## 1. Core Concepts

| Concept | Meaning |
|---|---|
| Log Group | container for log streams (e.g., /aws/lambda/payment-service) |
| Log Stream | sequence of log events from one source (one Lambda instance, one EC2 instance) |
| Log Event | one line with timestamp and message |
| Retention | configurable per log group (1 day to 10 years, or never expire) |

Log naming convention:

```text
Lambda: /aws/lambda/{function-name}  (automatic)
ECS: /ecs/{cluster}/{service}  (configure in task definition)
EC2: /ec2/{instance-id}/application  (CloudWatch agent)
Custom apps: /myapp/{environment}/{service}
```

## 2. CloudWatch Logs Insights

SQL-like query language for log analysis:

```text
# Count errors by type
fields @timestamp, @message
| filter @message like /ERROR/
| stats count(*) as errorCount by errorType
| sort errorCount desc
| limit 20

# P90 latency of Lambda function
filter @type = "REPORT"
| stats avg(Duration), percentile(Duration, 90) as p90, max(Duration) as maxDuration
by bin(5min)

# Find 5xx errors
filter @message like /HTTP 5/
| fields @timestamp, requestId, @message
| sort @timestamp desc
```

## 3. Metric Filters

Extract metrics from log patterns:

```text
Filter Pattern: [timestamp, requestId, level="ERROR", ...]
Metric Name: ErrorCount
Namespace: MyApp/Payments
Value: 1

Every log line matching the pattern increments the metric by 1.
Alarm on ErrorCount > 10 per minute -> PagerDuty notification.
```

## 4. Log Subscription Filters

Stream logs in real-time to:
- Lambda (process and analyze in real-time)
- Kinesis Data Streams (high-volume streaming)
- Kinesis Firehose (deliver to S3 or OpenSearch)

Use for:
- centralized logging to OpenSearch for search
- real-time log alerting via Lambda
- cross-account log aggregation

## 5. CloudWatch Synthetics

Canary scripts that run on a schedule to test endpoints:

```text
Canary: puppeteer/Node.js script that checks:
  - /health endpoint returns 200
  - login flow completes in <2 seconds
  - key user journey works end-to-end

Runs: every minute
Alarm: SuccessPercent < 100 -> alert

Use for:
- proactive alerting before users notice
- SLA proof (uptime measurement)
- external endpoint monitoring
```

---

# Topic 3: AWS X-Ray

## 1. Intuition

X-Ray provides distributed tracing. When a request flows through multiple services (API Gateway → Lambda → RDS → external API), X-Ray shows the end-to-end path with latency breakdown.

Without tracing:
```text
User reports: "checkout is slow"
You: which service is slow? don't know.
```

With X-Ray:
```text
User reports: "checkout is slow"
X-Ray trace: API GW (5ms) -> Lambda init (50ms) -> payment-service (15ms) -> 
             PaymentGateway HTTP (450ms) <- here!
```

## 2. Core Concepts

| Concept | Meaning |
|---|---|
| Trace | end-to-end record of a request across all services |
| Segment | one service's contribution to a trace |
| Subsegment | operations within a segment (DB query, HTTP call) |
| Annotations | key-value pairs indexed for filtering traces |
| Metadata | non-indexed context data on segments |
| Service Map | visual graph of service dependencies |
| Sampling | only record a % of requests (avoid cost/performance overhead) |

## 3. X-Ray Sampling

Sampling controls what % of requests are traced:

Default rule:
- first request each second per host: always trace
- 5% of subsequent requests

Custom sampling rule:

```json
{
  "RuleName": "HighValueOrders",
  "Priority": 1,
  "FixedRate": 0.10,
  "ReservoirSize": 10,
  "ResourceARN": "*",
  "ServiceName": "payment-service",
  "URLPath": "/api/checkout*",
  "HTTPMethod": "POST"
}
```

This traces 10% of checkout POST requests (plus 10 per second guaranteed).

## 4. X-Ray With Lambda

Enable X-Ray active tracing in Lambda:

```yaml
# SAM / CloudFormation
Properties:
  TracingConfig:
    Mode: Active
```

Lambda automatically creates a trace segment. Use X-Ray SDK to add subsegments:

```python
from aws_xray_sdk.core import xray_recorder
from aws_xray_sdk.core import patch_all

patch_all()  # auto-instrument boto3, requests, etc.

@xray_recorder.capture('payment-processing')
def process_payment(order_id, amount):
    # this becomes a subsegment
    xray_recorder.put_annotation('orderId', order_id)
    xray_recorder.put_annotation('amount', str(amount))
    # ... payment logic
```

## 5. ServiceLens

ServiceLens in CloudWatch combines:
- X-Ray traces
- CloudWatch metrics
- CloudWatch Logs

Into a unified view:
- Service map with health indicators
- Drill from service to traces
- Drill from trace to logs

Requires X-Ray + Container Insights or Lambda Insights enabled.

## 6. Container Insights

Container Insights collects metrics and logs from ECS and EKS:

```text
Metrics collected:
  ECS: CpuUtilized, MemoryUtilized, NetworkRxBytes, NetworkTxBytes, StorageReadBytes
  EKS: node_cpu_utilization, pod_memory_utilization, container_restarts

Enable for ECS:
  ecs create-cluster --configuration executeCommandConfiguration ... \
    --settings name=containerInsights,value=enabled

Enable for EKS:
  Deploy Container Insights add-on (CloudWatch agent + Fluent Bit)
```

## 7. Lambda Insights

Lambda Insights provides enhanced metrics not in standard Lambda metrics:

```text
Additional metrics:
  init_duration, cold_start count
  memory_utilization
  disk_utilization
  network bytes

Enable via Lambda layer:
  Attach AWS-provided Lambda Insights layer to function
```

## 8. Common Mistakes

| Mistake | Better Approach |
|---|---|
| No log retention policy | set retention per log group (avoid 30-day billing default) |
| Alert on every metric in isolation | use composite alarms to reduce noise |
| X-Ray sampling at 100% in production | use default or custom sampling; 100% adds overhead and cost |
| No custom metrics for business KPIs | use EMF to emit business metrics from application code |
| Static threshold alarms on cyclical metrics | use anomaly detection for traffic/error rate |
| No CloudWatch Synthetics | add canary probes for external availability monitoring |
| Logs with no structure (plain strings) | use structured JSON logs for Logs Insights queries |

## 9. Interview Scenario

**Scenario**: "Walk me through how you'd troubleshoot a Lambda-based API where users report intermittent slow responses."

Strong answer:

```text
1. CloudWatch Lambda metrics:
   - Duration: look at p99 vs p50 (large gap = cold starts or outlier errors)
   - Throttles: check if concurrency limit is causing queuing
   - Errors: check error rate and correlate with duration spikes

2. Lambda Insights:
   - init_duration: high values confirm cold starts
   - memory_utilization: if near max, might be OOM pressure

3. X-Ray traces:
   - Open ServiceLens -> payment-service
   - Filter by response_time > 1000ms
   - Look at trace breakdown: which subsegment is slow?
     - If Lambda init is large: provisioned concurrency or SnapStart
     - If DB call is large: RDS Proxy connection pool exhaustion? slow query?
     - If external HTTP is large: third-party timeout? retry storm?

4. CloudWatch Logs Insights:
   filter @type = "REPORT"
   | stats percentile(Duration, 99) by bin(5min)
   Correlate p99 spike times with deployments, traffic changes

5. Fix:
   - Cold start: provisioned concurrency or SnapStart
   - DB connection: add RDS Proxy
   - Slow query: Performance Insights for slow query log
   - External timeout: add circuit breaker, reduce timeout, add fallback
```

## 10. Revision Notes

- CloudWatch Metrics: namespace + metric + dimensions; 1-minute resolution default
- Custom metrics: EMF format — extract from logs automatically, no extra API call
- Alarms: static | anomaly detection | composite; use composite to reduce noise
- Logs Insights: SQL-like, query across log groups, patterns, time-range filtering
- Metric filters: extract metric from log pattern; good for error rate, latency histograms
- X-Ray: distributed tracing; trace → segment → subsegment; annotations for filter
- Sampling: never 100% in prod; custom rules for high-value paths
- Container Insights: ECS/EKS metrics; Lambda Insights: enhanced Lambda metrics
- ServiceLens: combined metrics + traces + logs view in CloudWatch console

## 11. Official Source Notes

- CloudWatch Metrics: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/working_with_metrics.html>
- CloudWatch Logs Insights: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AnalyzingLogData.html>
- CloudWatch Synthetics: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Synthetics_Canaries.html>
- X-Ray: <https://docs.aws.amazon.com/xray/latest/devguide/aws-xray.html>
- Container Insights: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights.html>
- EMF: <https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format.html>
