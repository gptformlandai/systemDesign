# 29. Datadog Mini Projects and Portfolio Mastery

## Project 1: Full-Stack Java Spring Boot With Complete Observability

**Goal**: Instrument a Spring Boot application end-to-end with metrics, APM, logs, and a live dashboard.

### What To Build

```text
Application: simple order processing REST API
  POST /orders -> create order
  GET /orders/{id} -> fetch order
  GET /orders/history?userId=xxx -> fetch user's orders

Observability stack:
  - Datadog Agent in Docker
  - dd-java-agent for APM
  - JSON logging with trace ID injection via Logback
  - DogStatsD custom business metrics (orders created, payment failures)
  - Dashboard with backend APM widgets
  - One SLO for checkout API availability
  - One monitor for error rate
```

### Implementation Steps

```text
1. Create Spring Boot project with JDBC, Redis, and web dependencies.

2. Add Logback JSON configuration with MDC trace fields.

3. Run with dd-java-agent:
   -javaagent:dd-java-agent.jar
   -Ddd.service=order-api -Ddd.env=dev -Ddd.version=1.0.0 -Ddd.logs.injection=true

4. Add custom business metrics:
   @Service
   public class OrderService {
     private final StatsDClient statsd = new NonBlockingStatsDClientBuilder()
       .prefix("orders").hostname("localhost").port(8125).build();
   
     public Order createOrder(OrderRequest req) {
       statsd.incrementCounter("created", "env:dev", "service:order-api");
       // business logic
     }
   }

5. Add manual span for payment processing:
   @Trace(operationName = "payment.process", resourceName = "PaymentService.charge")
   public PaymentResult charge(String orderId, BigDecimal amount) { ... }

6. Build Datadog dashboard:
   - Timeseries: requests/sec, error rate, p99 latency
   - Query Value: current error rate with color threshold
   - Top List: slowest endpoints
   - Log Stream: recent errors with trace links

7. Create SLO: 99.9% of checkout requests succeed.

8. Create monitor: alert if error rate > 2% for 5 minutes.
```

**Portfolio Output**: GitHub repo with instrumented Spring Boot app, docker-compose.yml with Datadog Agent, and screenshot of the dashboard.

---

## Project 2: Node.js + Python Microservices With Cross-Service Tracing

**Goal**: Show a complete distributed trace across two services in different languages.

### Architecture

```text
frontend-api (Node.js / Express)
  POST /checkout
    -> calls product-service (Python / FastAPI) to validate stock
    -> calls payment-service (Node.js) to charge
    -> returns combined result

All three services instrumented with Datadog APM.
Complete end-to-end trace visible in one flame graph.
Trace IDs injected into logs across all three services.
```

### Key Implementation Points

```text
Node.js (frontend-api):
  const tracer = require('dd-trace').init({ logInjection: true })
  const axios = require('axios')  // auto-instrumented

Python (product-service):
  ddtrace.patch_all()
  # FastAPI auto-instrumented

Node.js (payment-service):
  require('./tracer')  // tracer.js first
  const express = require('express')

Context propagation:
  axios (Node.js) auto-injects x-datadog-trace-id and traceparent headers
  httpx (Python) auto-instrumented by ddtrace.patch_all()
  All services read both Datadog and W3C headers
```

**Portfolio Output**: docker-compose with all three services + Datadog Agent, Postman collection, screenshot of multi-service flame graph.

---

## Project 3: OpenTelemetry Java App Sending To Datadog

**Goal**: Build a Java app using OTel SDK (not dd-java-agent) and route to Datadog via OTLP.

### What To Build

```text
Java Spring Boot app using:
  - OTel Java SDK for instrumentation
  - OTel Java auto-instrumentation agent (opentelemetry-javaagent.jar)
  - OTLP gRPC exporter to Datadog Agent
  - W3C TraceContext propagation

Demonstrates:
  - Vendor-neutral instrumentation
  - Same traces visible in Datadog as with dd-java-agent
  - Resource attributes: service.name, service.version, deployment.environment
```

### Key Config

```bash
java \
  -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=orders-otel-demo \
  -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
  -Dotel.resource.attributes=env=dev,version=1.0.0 \
  -Dotel.propagators=tracecontext,baggage \
  -jar orders-service.jar
```

**Portfolio Output**: Comparison screenshot — same endpoint, same traffic, OTel vs dd-java-agent trace views side-by-side.

---

## Project 4: Kubernetes Deployment With Datadog Monitoring

**Goal**: Deploy a containerized application on Kind/Minikube with full Datadog Kubernetes monitoring.

### What To Build

```text
Kind cluster with:
  - Datadog Agent DaemonSet (via Helm)
  - Cluster Agent
  - Sample application with Kubernetes manifest including UST labels
  - Autodiscovery annotations for log collection

Dashboard showing:
  - Pod restart count per deployment
  - Container CPU and memory utilization
  - Node resource usage heatmap
  - Live containers view
  - Pod status by namespace
```

### Helm Install For Kind

```bash
kind create cluster --name datadog-demo

helm install datadog datadog/datadog \
  --namespace datadog --create-namespace \
  --set datadog.apiKey=YOUR_API_KEY \
  --set datadog.clusterName=datadog-demo \
  --set datadog.apm.portEnabled=true \
  --set datadog.logs.enabled=true \
  --set datadog.logs.containerCollectAll=true \
  --set clusterAgent.enabled=true
```

**Portfolio Output**: Kind cluster setup script, Helm values.yaml, Kubernetes manifests for sample app with UST labels.

---

## Project 5: SLO Dashboard And Incident Runbook

**Goal**: Create a production-grade SLO dashboard with error budget tracking and a written incident runbook.

### SLO Dashboard Components

```text
Section 1: Service Health Overview
  - Request rate (last 24h)
  - Error rate with threshold line
  - P99 latency with SLA target line

Section 2: SLO Status
  - 7-day SLO widget
  - 30-day SLO widget
  - Error budget remaining (time format)
  - Burn rate trend (last 24h)

Section 3: Incident History
  - Log stream filtered to ERROR for last 24h
  - Deployment markers on metric graphs
  - SLO breach annotations

Section 4: Synthetic Test Results
  - API test uptime for /health
  - Browser test pass rate for checkout flow
```

### Incident Runbook Template

```text
# Runbook: Orders Service Error Rate Elevated

## Severity: P1 (SLO at risk)

## Trigger
Monitor: "Orders Service Error Rate > 2%"
Burn Rate alert: > 14.4x

## Initial Response (T+0 to T+5 minutes)
1. Acknowledge PagerDuty incident.
2. Open APM Service page for orders-service.
3. Check: is this a deployment? (APM Deployments view)
4. Check: what is the top error message? (Trace Explorer, sort by error count)

## Investigation (T+5 to T+15 minutes)
5. Open the slowest/erroring trace in Trace Explorer.
6. Identify the failing span (DB/HTTP/Kafka).
7. Check downstream service APM pages.
8. Check infrastructure metrics for the failing dependency.

## Mitigation
If deployment-related: kubectl rollout undo deployment/orders-service -n production
If dependency-related: enable circuit breaker / increase timeout

## Recovery Confirmation
Monitor auto-resolves.
SLO burn rate drops below 1.0x.
Error rate < 0.5%.

## Post-Incident
RCA document within 24 hours.
SLO status reported to stakeholders.
```

**Portfolio Output**: Dashboard JSON (exportable from Datadog), written runbook as Markdown file, Terraform resource for the monitor.

---

## Interview Sound Bite

Portfolio projects demonstrate practical Datadog mastery better than any certification. Five projects cover the full stack: Java APM instrumentation, multi-language cross-service tracing, OpenTelemetry portability, Kubernetes monitoring, and SLO-driven incident response. Each project produces a shareable artifact (GitHub repo, dashboard JSON, Helm values, runbook) that answers the interview question "show me something you built with Datadog."
