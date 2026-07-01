# 24. Datadog Interview Q&A — Beginner To MAANG

## Level 1: Beginner Questions

**Q: What is Datadog?**

Datadog is a cloud-scale SaaS observability platform that unifies metrics, logs, traces, RUM, synthetic tests, and security signals under a common tag-based correlation model. You install an agent on each host, instrument applications with APM libraries, and all signals flow to the Datadog intake endpoint via HTTPS.

---

**Q: What are the three pillars of observability and how does Datadog implement them?**

Metrics (what is wrong), logs (what happened), and traces (where and why). Datadog implements all three: metrics via the agent and DogStatsD; logs via agent tailing, Docker/Lambda forwarders, and log pipelines; traces via APM libraries (dd-java-agent, dd-trace, ddtrace). All three share a common tag vocabulary for correlation.

---

**Q: What is Unified Service Tagging?**

UST is the practice of applying three standard tags — `env`, `service`, and `version` — consistently to every signal (metrics, logs, traces) from a service. When all three match across signals, Datadog can correlate a metric spike to an error log to a specific trace without manual search.

---

**Q: What is DogStatsD?**

DogStatsD is a metrics aggregation service built into the Datadog Agent. Applications send custom metric data via UDP (port 8125) in StatsD format. The agent batches and forwards metrics to Datadog. Supports counter, gauge, histogram, distribution, timer, and set metric types.

---

## Level 2: Intermediate Questions

**Q: Explain how dd-java-agent instruments a Spring Boot application.**

The dd-java-agent is a JVM javaagent attached at startup via `-javaagent:/path/to/dd-java-agent.jar`. It uses bytecode instrumentation (via Byte Buddy) to intercept calls to Spring MVC, JDBC, HTTP clients (RestTemplate, OkHttp), Kafka producers/consumers, and Redis clients. It creates spans for each intercepted call automatically. No application code changes are required. Custom spans can be added with the `@Trace` annotation or the OpenTracing API.

---

**Q: What are spanID, traceID, and parentID?**

A traceID is a 128-bit identifier shared by all spans in one distributed trace. A spanID is a 64-bit identifier for one specific unit of work (one service call, DB query, etc). A parentID is the spanID of the calling span, forming the parent-child tree. The root span has no parentID. All spans propagate the traceID across service boundaries via HTTP headers (x-datadog-trace-id or W3C traceparent).

---

**Q: How does log correlation with traces work?**

The tracing library (dd-java-agent, dd-trace, ddtrace) injects `dd.trace_id` and `dd.span_id` into the logging context (Java MDC, Python log record, Node.js log fields) automatically when `DD_LOGS_INJECTION=true`. The application log JSON includes these fields. In Log Explorer, clicking the `dd.trace_id` value reveals a "View related trace" button that jumps directly to the flame graph for that trace.

---

**Q: What is the difference between histogram and distribution metrics?**

Histogram metrics compute percentiles (p50/p75/p95/p99) per-agent and then aggregate them, which can distort p99 across many hosts. Distribution metrics send all raw values to Datadog, which computes global server-side percentiles across all agents simultaneously. Distribution metrics provide accurate p99 across 1000 hosts; histogram aggregation does not.

---

## Level 3: Practitioner Questions

**Q: How do you build an SLO for a REST API service?**

Create a metric-based SLO. Numerator: count of 2xx responses (`sum:trace.http.request.hits{env:prod,service:orders,http.status_code:2*}`). Denominator: count of all requests. Set a 30-day 99.9% target. This gives 43 minutes of monthly error budget. Configure burn rate alerts: fast burn (14.4x over 5m/1h windows) for P1 pages, slow burn (6x over 30m/6h windows) for P2 tickets.

---

**Q: How do you detect and fix alert fatigue?**

Audit monitor history: any monitor that triggered more than 5 times this week with no action taken is noisy. Fixes: extend evaluation window (1m → 15m), raise thresholds (70% → 85%), add recovery threshold, replace static thresholds with anomaly detection for seasonal metrics, and replace multiple single-signal monitors with composite monitors that require two conditions simultaneously.

---

**Q: How do you monitor Kubernetes with Datadog?**

Deploy the agent as a DaemonSet (one pod per node) and the Cluster Agent as a Deployment. Use Helm for configuration. Enable autodiscovery via pod annotations for per-container log collection and checks. Use Unified Service Tagging labels (`tags.datadoghq.com/env`, `tags.datadoghq.com/service`, `tags.datadoghq.com/version`) on pod specs. The Cluster Agent provides the external metrics API for HPA and can auto-inject APM agents via the Admission Controller.

---

## Level 4: Senior/MAANG Questions

**Q: How would you design a Datadog observability strategy for a 50-microservice platform?**

Start with Unified Service Tagging enforced at deployment (CI gate: reject deployments missing required tags). Use the Cluster Agent with Admission Controller for automatic APM injection. Standardize on W3C TraceContext propagation across all services. Define a global tag taxonomy: env, service, version, team, region. Implement SLOs for all tier-1 services. Create per-team dashboards using template variables. Configure log exclusion filters (drop debug/health-check logs). Set up burn rate alerts instead of raw metric thresholds. Archive all logs to S3 for compliance.

---

**Q: Explain how you debug a broken distributed trace in a mixed Java/Node.js environment.**

Open the "disconnected" trace entry span and check if a parent context was extracted (look for sampling priority tag). Open the source service exit span and check if trace headers were injected. If headers are present but not understood, the issue is propagation format mismatch: dd-trace (Node.js) defaults to Datadog headers while OTel Java defaults to W3C TraceContext. Fix by configuring both services to use W3C TraceContext. If headers are absent from the outbound call, the HTTP client is not instrumented — add manual injection or verify the tracing library is initialized before the HTTP client is constructed.

---

**Q: How would you control Datadog costs at scale?**

Four levers: host count (right-size; only install agents where needed), APM hosts (same), log indexing volume (exclusion filters for debug/health-check logs; archive everything to S3), and custom metric cardinality (prohibit user_id/request_id as metric tags; use tag governance with mandatory review for new tags; monitor `datadog.estimated_usage.metrics.custom`). Enable Usage Attribution by team tag for per-team chargebacks. Review usage reports weekly and set monitors on the estimated usage metrics themselves.

---

**Q: How do you propagate trace context through a Kafka message?**

The Kafka auto-instrumentation in dd-trace/dd-java-agent handles inject/extract automatically for supported Kafka clients. For manual or OTel-based setups: producer side injects the current span context into Kafka message headers using the TextMapPropagator with a custom header carrier. Consumer side extracts the context from the record headers, creates a new root span with the extracted context as parent (asChildOf or using context.extract()). This links consumer processing spans to the producer trace, forming a continuous trace across the async boundary.

---

**Q: What is cardinality explosion and how do you prevent it?**

Cardinality explosion occurs when high-cardinality values are used as metric tags. Each unique tag combination creates a separate time series. Example: adding user_id (1M users) to a metric creates 1 million time series for that single metric. Cost scales linearly. Prevention: enforce a tag allowlist via governance policy; prohibit user_id, request_id, session_id, and IP address as metric tags; use Datadog estimated usage monitors to detect sudden cardinality growth; require team review for any new metric tag with cardinality above 100.
