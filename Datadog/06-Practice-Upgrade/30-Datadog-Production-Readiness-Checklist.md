# 30. Datadog Production Readiness Checklist

Score yourself: each item is worth 1 point.
35+ / 45 = production ready.
40+ / 45 = MAANG ready.
45 / 45 = Datadog SME level.

---

## Section 1: Foundations (9 points)

- [ ] Can explain the three observability pillars and what question each answers.
- [ ] Can name the five standard fields required in a correlated Datadog JSON log.
- [ ] Can install and configure the Datadog Agent on a bare metal host, Docker container, and Kubernetes cluster.
- [ ] Can explain the difference between count, gauge, histogram, and distribution metric types.
- [ ] Can write a DogStatsD counter and gauge metric from the command line.
- [ ] Can create a log processing pipeline in Datadog with at least one Grok parser.
- [ ] Can configure log exclusion filters to reduce indexing cost.
- [ ] Can explain what Unified Service Tagging is and why it is required.
- [ ] Can verify the agent is healthy and receiving data from the CLI.

---

## Section 2: APM And Tracing (10 points)

- [ ] Can explain traceID, spanID, parentID, root span, and entry span.
- [ ] Can attach dd-java-agent to a Spring Boot application with all five required environment variables.
- [ ] Can create a manual custom span in Java using the @Trace annotation and the OpenTracing API.
- [ ] Can initialize dd-trace in a Node.js application as the first require().
- [ ] Can instrument a FastAPI Python application with ddtrace using ddtrace-run.
- [ ] Can explain head-based sampling and configure a sampling rate per service.
- [ ] Can filter traces in Trace Explorer using duration, service, resource, and error filters.
- [ ] Can explain what the flame graph shows and how to identify the bottleneck span.
- [ ] Can explain how the service map works and what edge color/thickness means.
- [ ] Can explain context propagation and the difference between Datadog headers and W3C TraceContext.

---

## Section 3: OpenTelemetry (5 points)

- [ ] Can configure the Datadog Agent to accept OTLP on ports 4317 and 4318.
- [ ] Can set up OTel Java SDK with TracerProvider, BatchSpanProcessor, and OTLPGrpcSpanExporter.
- [ ] Can set up OTel Python SDK and instrument FastAPI with auto-instrumentation.
- [ ] Can set up OTel Node.js SDK with NodeSDK and OTLP exporter.
- [ ] Can explain W3C TraceContext traceparent format and propagation chain.

---

## Section 4: Log Correlation (5 points)

- [ ] Can configure Logback JSON encoder to include dd.trace_id from MDC in Java.
- [ ] Can configure Python logging with ddtrace.contrib.logging to inject trace IDs.
- [ ] Can configure dd-trace Node.js with logInjection=true for Winston/Pino.
- [ ] Can demonstrate clicking from a log entry to its trace in Datadog Log Explorer.
- [ ] Can explain why JSON logging is required (not plain text) for log correlation.

---

## Section 5: Dashboards And Monitors (6 points)

- [ ] Can build a dashboard with at least five widget types and template variables for env and service.
- [ ] Can write a metric query for error rate as a percentage of total requests.
- [ ] Can write a metric query for p99 latency grouped by endpoint.
- [ ] Can create a metric monitor with alert, warning, and recovery thresholds.
- [ ] Can create a composite monitor combining error rate AND latency conditions.
- [ ] Can configure a monitor notification message with Slack and PagerDuty routing.

---

## Section 6: SLOs And Error Budgets (4 points)

- [ ] Can create a metric-based SLO with numerator and denominator queries.
- [ ] Can calculate the monthly error budget in minutes for any SLO target.
- [ ] Can calculate burn rate and answer "at this burn rate, when is budget exhausted?"
- [ ] Can configure multi-window multi-burn-rate alerts (fast burn and slow burn).

---

## Section 7: Kubernetes And Infrastructure (4 points)

- [ ] Can install Datadog on Kubernetes via Helm with DaemonSet agent and Cluster Agent.
- [ ] Can configure pod annotations for autodiscovery log collection.
- [ ] Can write a monitor for container restart count and pod memory usage.
- [ ] Can apply Unified Service Tagging labels to a Kubernetes Deployment spec.

---

## Section 8: Production Scenarios (6 points)

- [ ] Can walk through a high latency investigation using APM flame graph and infrastructure metrics.
- [ ] Can correlate a log error spike to a trace and identify the downstream dependency failure.
- [ ] Can debug an OOMKilled container using Live Containers and memory usage metrics.
- [ ] Can identify an N+1 query problem from a flame graph showing many identical DB spans.
- [ ] Can design a composite alert strategy to reduce alert fatigue from 50 alerts/day to under 10.
- [ ] Can describe the full incident response workflow for a SLO burn rate alert.

---

## Scoring Rubric

| Score | Meaning |
|---|---|
| 30-34 | Strong foundations. Ready for junior SRE/DevOps Datadog roles. |
| 35-39 | Production ready. Ready for mid-level SRE/backend engineer roles with Datadog on the stack. |
| 40-44 | Senior level. Can own observability strategy for a team of 5-10 services. |
| 45 | MAANG level. Can design and own the observability platform for a 50+ service organization. |

---

## Gap Fill Recommendations

If missing Section 2 items: spend one day building a Java Spring Boot + dd-java-agent app.

If missing Section 3 items: build the OTel project from Project 3 in sheet 29.

If missing Section 5 items: build the dashboard from Project 1 and write one composite monitor.

If missing Section 8 items: simulate each scenario in the datadog-mastery-lab environments.
