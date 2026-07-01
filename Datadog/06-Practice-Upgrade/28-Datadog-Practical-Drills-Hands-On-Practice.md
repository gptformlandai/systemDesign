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

## Interview Sound Bite

Practical drills reinforce what theory alone cannot: the exact init order for dd-trace (Node.js first import), the command-line syntax for dd-java-agent (-javaagent with -Ddd.service), the query structure for error rate formulas, and the math behind SLO burn rate calculations. Drilling these until they are automatic is what separates a candidate who "knows Datadog" from one who can implement it correctly on the first try.
