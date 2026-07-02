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

## Level 5: Advanced Platform Recall

**What problem does Software Catalog solve that APM alone does not?**

> APM shows runtime behavior. Software Catalog shows ownership, lifecycle, tier, contacts, runbooks, dependencies, scorecards, and standards for each service.

---

**When would you use Universal Service Monitoring before APM?**

> When you need fast service discovery and basic request/error/latency visibility without code changes, especially for legacy or uninstrumented services. Use APM later for flame graphs, custom spans, and code-level debugging.

---

**What is the difference between APM and Continuous Profiler?**

> APM shows request spans and dependency timing. Continuous Profiler shows which code paths consume CPU, wall time, memory allocations, heap, or locks while the application runs.

---

**What safety checks should you apply before adding a Dynamic Instrumentation probe in production?**

> Scope it narrowly, add a condition, avoid PII/secrets, set expiration, control rate/sampling, verify RBAC, and remove it after investigation.

---

**Why is Error Tracking better than alerting on every exception log?**

> It groups repeated exceptions into issues, shows first-seen time, impacted users/requests, suspected commits, service/env/version context, and issue lifecycle.

---

**What tags connect CI Visibility to production regressions?**

> service, env, version, team, repository, branch, and git.commit.sha. Runtime must also emit `DD_VERSION` so deploys can correlate with APM, Error Tracking, SLOs, and incidents.

---

**What is the main difference between Datadog log pipelines and Observability Pipelines?**

> Datadog log pipelines process logs after they reach Datadog. Observability Pipelines process telemetry inside your infrastructure before routing, enabling pre-ingestion redaction, enrichment, sampling, archiving, and dual shipping.

---

**Name five serverless signals that matter beyond Lambda error count.**

> Cold starts, duration, timeouts, memory/OOM, throttles, async queue lag, DLQ depth, Step Functions failures, retries, and cost.

---

**What does Data Streams Monitoring show that normal HTTP APM may miss?**

> Producer-to-consumer topology, end-to-end event latency, consumer lag, backlog, payload size, and faulty edges across Kafka/SQS/Kinesis/RabbitMQ-style pipelines.

---

**What is unit cost in Cloud Cost Management?**

> Cost normalized by business volume, such as cost per order, cost per 1,000 requests, cost per job processed, or cost per active user.

---

**How should runtime vulnerabilities be prioritized?**

> By severity, exploitability, internet exposure, service criticality, whether the vulnerable code is actually running or called, data sensitivity, and service owner.

---

**What makes an LLM observability trace different from a normal API trace?**

> It includes prompt assembly, retrieval, reranking, model calls, token counts, tool calls, safety checks, prompt template version, model/provider tags, and quality/cost signals.

---

**What is the Datadog platform-as-code rule of thumb?**

> Critical monitors, SLOs, dashboards, synthetics, pipelines, roles, and integrations should be version-controlled with owner tags, runbooks, scoped queries, pager routes, and no secrets.

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
| Software Catalog | ownership, scorecards, runbooks, dependencies |
| USM | code-free service discovery and RED metrics |
| Profiler | code-level CPU/wall/memory/lock hotspots |
| Dynamic Instrumentation | temporary live probes without redeploy |
| Error Tracking | grouped issues from repeated exceptions |
| CI Visibility | pipeline/test/deploy telemetry |
| Observability Pipelines | pre-ingestion processing and routing |
| Serverless traps | cold starts, throttles, timeouts, DLQ, lag |
| Data Streams | async pipeline latency and consumer lag |
| Cloud Cost | service/team spend and unit economics |
| App/API Protection | runtime attack and API abuse detection |
| Code Security | SAST/SCA/IAST/IaC/secrets/SBOM |
| LLM Observability | token cost, prompt/model/RAG/tool/safety signals |
| Governance | RBAC, SSO/SCIM, audit, Terraform/API, tag policy |
