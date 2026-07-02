# 28. Datadog Practical Drills and Hands-On Practice

## Drill 1: Agent Setup And First Metric

**Goal**: Install agent locally (Docker), verify it reports, send a custom metric.

```bash
# Run Datadog Agent in Docker.
docker run -d --name datadog-agent \
  -e DD_API_KEY=YOUR_API_KEY \
  -e DD_SITE=datadoghq.com \
  -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC=true \
  -e DD_APM_ENABLED=true \
  -e DD_APM_NON_LOCAL_TRAFFIC=true \
  -p 8125:8125/udp \
  -p 8126:8126/tcp \
  gcr.io/datadoghq/agent:7

# Verify agent is healthy.
docker exec datadog-agent datadog-agent status

# Send a test custom metric via UDP.
echo "myapp.test.counter:1|c|#env:dev,service:myapp" | nc -u -w1 localhost 8125
echo "myapp.test.gauge:42.5|g|#env:dev,service:myapp" | nc -u -w1 localhost 8125

# Verify in Datadog Metrics Explorer:
# Search for myapp.test.counter
# Should appear within 15-30 seconds.
```

**Verify**: Metric appears in Metrics Explorer within 30 seconds.

---

## Drill 2: Java Spring Boot APM

**Goal**: Add dd-java-agent to a Spring Boot app and see traces in Datadog.

```bash
# Download agent.
curl -Lo dd-java-agent.jar 'https://dtdg.co/latest-java-tracer'

# Run Spring Boot app with agent.
java \
  -javaagent:./dd-java-agent.jar \
  -Ddd.service=my-spring-app \
  -Ddd.env=dev \
  -Ddd.version=1.0.0 \
  -Ddd.agent.host=localhost \
  -Ddd.trace.agent.port=8126 \
  -Ddd.logs.injection=true \
  -Ddd.trace.sample.rate=1.0 \
  -jar myapp.jar

# Make a request to any endpoint.
curl http://localhost:8080/api/orders

# Check Datadog APM -> Services -> my-spring-app
# Should see the request as a trace.
```

**Verify**: Trace appears in APM → Traces with correct service name and resource.

---

## Drill 3: Python FastAPI APM

**Goal**: Instrument FastAPI with ddtrace and see traces with log correlation.

```python
# app.py
import ddtrace
ddtrace.patch_all()

from fastapi import FastAPI
import logging
import json

# JSON logging with trace injection.
logging.basicConfig(
    format='%(message)s',
    level=logging.INFO
)

app = FastAPI()

@app.get("/orders/{order_id}")
async def get_order(order_id: str):
    span = ddtrace.tracer.current_span()
    log_entry = {
        "message": f"Getting order {order_id}",
        "service": "orders-api",
        "level": "INFO",
        "dd.trace_id": str(span.trace_id) if span else None,
        "dd.span_id": str(span.span_id) if span else None,
    }
    logging.info(json.dumps(log_entry))
    return {"order_id": order_id, "status": "PENDING"}
```

```bash
DD_SERVICE=orders-api \
DD_ENV=dev \
DD_VERSION=1.0.0 \
DD_AGENT_HOST=localhost \
DD_LOGS_INJECTION=true \
ddtrace-run uvicorn app:app --port 8000

# Make a request.
curl http://localhost:8000/orders/ORD-001

# Check logs: should contain dd.trace_id.
# Check APM: should show trace for GET /orders/{order_id}.
```

---

## Drill 4: Write A Metric Query

**Practice**: Write queries for these requirements.

```text
Requirement 1: Average error rate for orders-service in production over last 1 hour.
Answer:
  avg:trace.http.request.errors{env:production,service:orders-service}.as_rate()
  / avg:trace.http.request.hits{env:production,service:orders-service}.as_rate()
  * 100

Requirement 2: P99 latency per service in production.
Answer:
  p99:trace.http.request.duration{env:production} by {service}

Requirement 3: Container restart count per pod for checkout deployment.
Answer:
  sum:kubernetes.containers.restarts{kube_deployment:checkout-worker,kube_namespace:production} by {pod_name}

Requirement 4: Top 5 slowest endpoints by average latency.
Answer:
  top(avg:trace.http.request.duration{env:production,service:orders}, 5, 'mean', 'desc')
```

---

## Drill 5: Create A Monitor

**Practice**: Write the configuration for this monitor.

```text
Requirement:
  Alert when orders-service error rate exceeds 2% in production for more than 5 minutes.
  Warning at 1%.
  Recover when below 0.5%.
  Notify #platform-alerts Slack channel.
  Include current value and threshold in message.

Configuration:
  Type: Metric Monitor
  
  Query:
    avg(last_5m):
      100 * sum:trace.http.request.errors{env:production,service:orders-service}.as_rate()
      / sum:trace.http.request.hits{env:production,service:orders-service}.as_rate()
  
  Alert threshold: > 2
  Warning threshold: > 1
  Recovery threshold: < 0.5
  
  Notify no data: 10 minutes
  Re-notify: 30 minutes
  
  Message:
    ## Orders Service Error Rate {{#is_alert}}CRITICAL{{/is_alert}}{{#is_warning}}WARNING{{/is_warning}}
    
    Current rate: {{value}}% (threshold: {{threshold}}%)
    
    APM: https://app.datadoghq.com/apm/service/orders-service
    
    @slack-platform-alerts
```

---

## Drill 6: SLO Math

**Practice**: Calculate error budget and burn rate values.

```text
Problem 1: Service has 99.95% SLO over 30 days. How many minutes of error budget?
Answer: 43200 * (1 - 0.9995) = 43200 * 0.0005 = 21.6 minutes

Problem 2: Service is at 14.4x burn rate. When will budget be exhausted?
  Assume 99.9% SLO, 30-day window. 43.2 minutes total budget.
Answer: 43.2 / 14.4 = 3 hours

Problem 3: In 2 hours, 15 minutes of budget was consumed. What is the burn rate?
  (2 hours = 120 minutes total time; 15 minutes consumed; normal = 43.2/43200 per minute)
Answer: actual_rate = 15 / 120 = 0.125 min/min
        allowed_rate = 43.2 / 43200 = 0.001 min/min
        burn_rate = 0.125 / 0.001 = 125x burn rate (extreme)
```

---

## Drill 7: Broken Trace Diagnosis

**Practice**: Given these symptoms, identify the root cause.

```text
Symptom A:
  cart-service traces appear correctly in APM.
  checkout-service traces appear as SEPARATE root spans (no parent from cart-service).
  cart-service is using dd-trace (Node.js).
  checkout-service is using OTel Java SDK.

Root cause: Propagation format mismatch.
Fix: configure both to use W3C TraceContext, or configure dd-trace to also inject traceparent.

Symptom B:
  Application logs contain dd.trace_id field.
  APM shows traces.
  But Log Explorer shows no "view related trace" button.

Root cause: service/env/version mismatch between logs and traces.
Fix: ensure logs and traces use identical service, env, version tag values.

Symptom C:
  dd-trace is imported in Node.js app.
  Zero traces appear in Datadog.
  App is running in Docker, agent is running in separate container.

Root cause: DD_APM_NON_LOCAL_TRAFFIC=true not set on agent.
Fix: set DD_APM_NON_LOCAL_TRAFFIC=true and DD_AGENT_HOST=datadog-agent-container-name.
```

---

## Drill 8: Software Catalog Entity And Scorecard

**Practice**: Write the minimum metadata for a production service and define scorecard checks.

```yaml
apiVersion: v3
kind: service
metadata:
  name: orders-service
  displayName: Orders Service
  tags:
    - service:orders-service
    - team:checkout
    - env:production
spec:
  owner: checkout-platform
  lifecycle: production
  tier: critical
  type: web
  contacts:
    slack: "#team-checkout-alerts"
    pagerduty: checkout-platform-primary
  links:
    repo: https://github.com/company/orders-service
    runbook: https://wiki.company.com/runbooks/orders-service
    dashboard: https://app.datadoghq.com/dashboard/orders
```

```text
Scorecard checks:
  - owner exists
  - service/env/version tags exist
  - SLO exists
  - monitor routes to owner
  - runbook link exists
  - no critical security findings
  - deployment events visible
```

---

## Drill 9: Profiler And Error Tracking Diagnosis

**Practice**: Given the evidence, identify the tool and root-cause path.

```text
Symptom A:
  CPU is 95%.
  APM shows slow application span but no slow downstream dependency.
  Regression started after version 2.4.1.

Tool: Continuous Profiler.
Action:
  compare CPU profiles for version 2.4.0 vs 2.4.1.
  find widest new frame.
  fix the expensive method.

Symptom B:
  20,000 Java exceptions appear after deploy.
  Many logs are duplicates.
  Need one actionable issue.

Tool: Error Tracking.
Action:
  group exceptions into issues.
  filter env:production service:orders-service.
  sort by new/high-impact issue.
  inspect first_seen, suspected commit, stack trace, impacted requests.
```

---

## Drill 10: Observability Pipelines Routing Plan

**Practice**: Design a pipeline for this requirement.

```text
Requirement:
  Reduce log cost by 40%.
  Redact email/token/card data before logs leave the VPC.
  Preserve complete logs for compliance.
  Keep all errors and security audit logs in Datadog.

Answer:
  app logs -> Observability Pipelines Worker
    -> redact email/token/card fields
    -> enrich service/team/env/account tags
    -> drop health check logs
    -> sample INFO logs at 10%
    -> keep ERROR and security logs at 100%
    -> send operational stream to Datadog
    -> send full stream to S3 archive
    -> monitor Worker drops, delivery errors, buffer usage, input/output rate
```

---

## Drill 11: Serverless And Data Stream Delay

**Practice**: Diagnose this event-driven incident.

```text
Symptom:
  Users report delayed order confirmation.
  orders-api p95 is normal.
  Lambda error rate is low.
  SQS age of oldest message is 18 minutes.
  DLQ depth is increasing.

Likely area:
  async processing path, not the synchronous API.

Check:
  - Lambda duration, throttles, timeouts, memory
  - queue age and visible messages
  - DLQ message samples
  - retry count and visibility timeout
  - consumer version/deployment
  - downstream dependency latency

Datadog tools:
  Serverless Monitoring + Data Streams Monitoring + APM for consumer code.
```

---

## Drill 12: Cloud Cost And LLM Cost Spike

**Practice**: Explain investigation steps.

```text
Symptom:
  AI support chatbot cost tripled.
  Traffic increased only 10%.
  Release introduced prompt template v7.

Steps:
  1. Split LLM cost by service, model, prompt.template, version, customer_tier.
  2. Compare prompt_tokens and completion_tokens before/after v7.
  3. Inspect RAG retrieval span count and document sizes.
  4. Check model/provider changes and retry/rate-limit behavior.
  5. Verify PII redaction and prompt logging policy.
  6. Roll back prompt template or cap retrieval/context/token budget.
```

---

## Interview Sound Bite

Practical drills reinforce what theory alone cannot: the exact init order for dd-trace (Node.js first import), the command-line syntax for dd-java-agent (-javaagent with -Ddd.service), the query structure for error rate formulas, the math behind SLO burn rate calculations, and the operating judgment behind Software Catalog, Profiler, Error Tracking, Observability Pipelines, Serverless, Data Streams, Cloud Cost, and LLM Observability. Drilling these until they are automatic is what separates a candidate who "knows Datadog" from one who can implement and operate it correctly.
