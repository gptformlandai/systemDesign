# 27. Datadog Active Recall and Spaced Repetition Drills

## How To Use This Sheet

Read each question. Answer without looking. Then check the answer.

Set a schedule:
- Day 1: read all questions
- Day 3: repeat Level 1
- Day 7: repeat Level 1 + 2
- Day 14: repeat all levels

---

## Level 1: Foundations Recall

**What are the three observability pillars and what question does each answer?**

> Metrics: is something wrong? Logs: what happened? Traces: where and why?

---

**Name the five mandatory fields in a Datadog-correlated JSON log.**

> dd.trace_id, dd.span_id, dd.service, dd.env, dd.version

---

**What environment variable enables trace ID injection into Java logs via dd-java-agent?**

> DD_LOGS_INJECTION=true

---

**What port does the Datadog APM Agent listen on? What port does DogStatsD listen on?**

> APM: 8126 (TCP). DogStatsD: 8125 (UDP).

---

**What three tags must be on every service for Unified Service Tagging?**

> env, service, version

---

**What is the difference between a count metric and a gauge metric?**

> Count: sum of occurrences in a flush interval (events, requests). Gauge: current point-in-time value (queue depth, memory, CPU%).

---

## Level 2: APM And Tracing Recall

**What is the difference between traceID and spanID?**

> traceID: unique to the entire request journey, shared across all spans. spanID: unique to one unit of work within the trace.

---

**What HTTP header carries the traceID in W3C TraceContext format?**

> traceparent

---

**What does head-based sampling mean in Datadog APM?**

> The sampling decision is made at the root span (entry point). All downstream spans in that trace inherit the same keep/drop decision.

---

**Name three auto-instrumented frameworks for dd-java-agent.**

> Spring MVC, JDBC, RestTemplate/OkHttp, Kafka, Redis (Jedis/Lettuce), gRPC — any three.

---

**What command installs the Datadog Python tracing library?**

> pip install ddtrace

---

**What is the init pattern difference between dd-trace (Node.js) and ddtrace (Python)?**

> Node.js: require('dd-trace').init() must be the first require(). Python: either ddtrace-run python app.py or patch_all() at the top of main module.

---

## Level 3: Senior Concepts Recall

**Define error budget and write the formula for a 99.9% SLO over 30 days.**

> Error budget = time × (1 - SLO target). 43200 min × 0.001 = 43.2 minutes.

---

**What does a burn rate of 14.4 mean?**

> The error budget is being consumed 14.4 times faster than the allowed rate. At this rate, the 30-day budget is exhausted in 30 / 14.4 = ~2 days.

---

**What two Kubernetes components does Datadog deploy and what does each do?**

> DaemonSet agent: one per node, collects node/container metrics, logs, and APM traces. Cluster Agent: one per cluster, aggregates cluster metrics, provides external metrics API, runs Admission Controller.

---

**What causes cardinality explosion in Datadog metrics?**

> Using high-cardinality values (user IDs, request IDs, IP addresses) as metric tags. Each unique tag combination creates a separate time series.

---

**Name two methods to reduce Datadog log indexing costs.**

> Exclusion filters (drop debug/health-check logs before indexing), log archives to S3 (cheap storage for rehydration).

---

## Level 4: MAANG-Level Recall

**Why does a composite monitor require fewer PagerDuty pages than two individual monitors?**

> It fires only when both conditions are simultaneously true. A single metric spike (error rate OR latency, not both) does not page. This eliminates false positives where only one signal is briefly elevated.

---

**What OTel port does the Datadog Agent listen on for gRPC OTLP? For HTTP OTLP?**

> gRPC: 4317. HTTP: 4318.

---

**How do you propagate trace context across a Kafka message boundary?**

> Producer: inject current span context into Kafka message headers using TextMapPropagator. Consumer: extract span context from record headers, create child span with extracted context as parent.

---

**What is the most common cause of a broken distributed trace in a mixed Java/Node.js environment?**

> Propagation format mismatch: dd-trace (Node.js) defaults to Datadog headers; OTel Java defaults to W3C TraceContext. Neither reads the other's format. Fix: standardize all services on W3C TraceContext propagation.

---

**What is the purpose of span retention filters vs agent-side sampling?**

> Agent-side sampling controls which traces are sent to Datadog (ingestion). Retention filters control which ingested spans are stored long-term (post-ingestion). They are independent controls. Errors can always be kept via retention filter even if the overall sampling rate is 10%.

---

## Flash Card Summary

| Concept | Answer |
|---|---|
| Three pillars | Metrics / Logs / Traces |
| UST tags | env, service, version |
| APM port | 8126 |
| DogStatsD port | 8125 |
| OTLP gRPC port | 4317 |
| OTLP HTTP port | 4318 |
| Log injection env var | DD_LOGS_INJECTION=true |
| Trace header (W3C) | traceparent |
| Trace header (Datadog) | x-datadog-trace-id |
| 99.9% SLO 30d budget | 43.2 minutes |
| Fast burn rate | 14.4x |
| Cardinality trap | user_id / request_id as metric tag |
| K8s DaemonSet purpose | per-node metrics, logs, traces |
| K8s Cluster Agent purpose | cluster metrics, external metrics API, Admission Controller |
