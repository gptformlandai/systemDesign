# Datadog Mastery Track - Beginner To Pro Index

Datadog is a cloud-scale observability and security platform. It unifies metrics, logs, traces, real user monitoring, synthetic tests, and SLOs into a single correlated view. The goal of this track is to teach Datadog from first-agent-install to MAANG-level production incident resolution.

```text
use case -> signal type (metric/log/trace) -> instrumentation -> collection -> correlation -> alert -> incident response -> postmortem
```

Use this track if:

- You want beginner-to-pro Datadog confidence across metrics, APM, logs, and dashboards.
- You want to instrument Java, Node.js, and Python applications with Datadog APM and OpenTelemetry.
- You want to understand trace correlation, spanID/traceID injection into logs, and distributed tracing across microservices.
- You want MAANG-level answers on SLOs, monitors, alert fatigue, and production incident resolution.
- You want modern Datadog platform depth: Software Catalog, USM, Profiler, Dynamic Instrumentation, Error Tracking, CI Visibility, Observability Pipelines, Serverless, Data Streams, Cloud Cost, App/API Security, Incident Management, LLM Observability, and governance.
- You want hands-on labs, runbooks, portfolio projects, and scenario drills.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
what am I observing -> which signal fits (metric/log/trace/RUM) -> how do I instrument it -> how do I collect it -> how do I correlate signals -> how do I alert on it -> how do I resolve incidents with it
```

Datadog mastery is not about knowing the UI. It is about instrumenting systems correctly, designing tag strategies, correlating signals across services, and resolving production incidents faster than without observability.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | Datadog mental model, agent setup, metrics, logs collection |
| 2 | `02-Intermediate-Practical` | APM tracing, Java/Node/Python instrumentation, OpenTelemetry, log correlation, dashboards |
| 3 | `03-Senior-Production` | Monitors/alerts, SLOs, Kubernetes, security, RUM/Synthetic, cost/tagging |
| 4 | `04-Scenario-Practice` | High latency, log spikes, OOMKilled, slow queries, alert fatigue, SLO burn, broken traces |
| 5 | `05-Special-Interview-Rounds` | Q&A, query/command cheatsheet, anti-patterns |
| 6 | `06-Practice-Upgrade` | Active recall, drills, mini projects, production readiness checklist |
| 7 | `07-Advanced-Platform` | Software Catalog, USM, Profiler, Dynamic Instrumentation, Error Tracking, CI/CD, Observability Pipelines, Serverless, Data Streams, FinOps, Security, Incidents, LLM Observability, Governance |
| Lab | `datadog-mastery-lab` | Examples, scripts, labs, projects, cheatsheets, interview prep, runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-Datadog-Mental-Model-Observability-Pillars-Platform-Overview-Hot-Sheet.md](01-Foundations/01-Datadog-Mental-Model-Observability-Pillars-Platform-Overview-Hot-Sheet.md) | what Datadog is, three pillars (metrics/logs/traces), agent architecture, tagging |
| 2 | [01-Foundations/02-Datadog-Agent-Install-Config-API-Key-Host-Tagging-Gold-Sheet.md](01-Foundations/02-Datadog-Agent-Install-Config-API-Key-Host-Tagging-Gold-Sheet.md) | agent install, datadog.yaml, API key, DogStatsD, process/network agents |
| 3 | [01-Foundations/03-Datadog-Metrics-Types-DogStatsD-Custom-Metrics-Queries-Gold-Sheet.md](01-Foundations/03-Datadog-Metrics-Types-DogStatsD-Custom-Metrics-Queries-Gold-Sheet.md) | metric types, DogStatsD, custom metrics, tag-based queries, metric aggregations |
| 4 | [01-Foundations/04-Datadog-Logs-Collection-Pipelines-Parsing-Facets-Gold-Sheet.md](01-Foundations/04-Datadog-Logs-Collection-Pipelines-Parsing-Facets-Gold-Sheet.md) | log collection, pipelines, Grok parsing, facets, log archives, exclusion filters |

Foundation target:

- You understand the three observability pillars and how Datadog unifies them.
- You can install and configure the Datadog Agent on any host.
- You can send custom metrics and parse logs with pipelines.

---

## 4. Intermediate Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Practical/05-Datadog-APM-Tracing-Concepts-SpanID-TraceID-ServiceMap-Gold-Sheet.md](02-Intermediate-Practical/05-Datadog-APM-Tracing-Concepts-SpanID-TraceID-ServiceMap-Gold-Sheet.md) | distributed tracing, spanID/traceID/parentID, sampling, Trace Explorer, service map |
| 6 | [02-Intermediate-Practical/06-Datadog-APM-Java-Instrumentation-dd-java-agent-Spring-Boot-Gold-Sheet.md](02-Intermediate-Practical/06-Datadog-APM-Java-Instrumentation-dd-java-agent-Spring-Boot-Gold-Sheet.md) | dd-java-agent.jar, auto/manual instrumentation, Spring Boot/Quarkus, trace config |
| 7 | [02-Intermediate-Practical/07-Datadog-APM-NodeJS-Python-Instrumentation-Express-Flask-FastAPI-Gold-Sheet.md](02-Intermediate-Practical/07-Datadog-APM-NodeJS-Python-Instrumentation-Express-Flask-FastAPI-Gold-Sheet.md) | dd-trace Node.js, ddtrace Python, Express/Flask/FastAPI auto-instrumentation |
| 8 | [02-Intermediate-Practical/08-Datadog-OpenTelemetry-Java-Python-JS-OTLP-Integration-Gold-Sheet.md](02-Intermediate-Practical/08-Datadog-OpenTelemetry-Java-Python-JS-OTLP-Integration-Gold-Sheet.md) | OTel SDK (Java/Python/JS), OTLP exporter, TracerProvider, context propagation, W3C TraceContext |
| 9 | [02-Intermediate-Practical/09-Datadog-Log-Correlation-TraceID-Injection-Java-Python-NodeJS-Gold-Sheet.md](02-Intermediate-Practical/09-Datadog-Log-Correlation-TraceID-Injection-Java-Python-NodeJS-Gold-Sheet.md) | dd.trace_id injection, Logback/Log4j2 MDC, Python logging, Winston/Pino, JSON log format |
| 10 | [02-Intermediate-Practical/10-Datadog-Dashboards-Widgets-Frontend-Backend-RUM-APM-Gold-Sheet.md](02-Intermediate-Practical/10-Datadog-Dashboards-Widgets-Frontend-Backend-RUM-APM-Gold-Sheet.md) | dashboard creation, widget types, frontend RUM metrics, backend APM metrics, SLO widgets |

Practical target:

- You can instrument any Java, Node.js, or Python app for distributed tracing.
- You can correlate traces with logs using injected trace/span IDs.
- You can build dashboards covering both frontend and backend health.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-Datadog-Monitors-Alerts-Notification-Channels-Composite-MAANG-Sheet.md](03-Senior-Production/11-Datadog-Monitors-Alerts-Notification-Channels-Composite-MAANG-Sheet.md) | monitor types, threshold/anomaly/forecast, alert routing, PagerDuty/Slack |
| 12 | [03-Senior-Production/12-Datadog-SLOs-Error-Budgets-Burn-Rate-Alerts-MAANG-Sheet.md](03-Senior-Production/12-Datadog-SLOs-Error-Budgets-Burn-Rate-Alerts-MAANG-Sheet.md) | SLO types, error budget math, burn rate alerts, SLO dashboards |
| 13 | [03-Senior-Production/13-Datadog-Kubernetes-Container-Monitoring-Cluster-Agent-MAANG-Sheet.md](03-Senior-Production/13-Datadog-Kubernetes-Container-Monitoring-Cluster-Agent-MAANG-Sheet.md) | K8s DaemonSet agent, Cluster Agent, autodiscovery, Live Containers, pod/node metrics |
| 14 | [03-Senior-Production/14-Datadog-Security-CSPM-Cloud-Integrations-AWS-GCP-Azure-MAANG-Sheet.md](03-Senior-Production/14-Datadog-Security-CSPM-Cloud-Integrations-AWS-GCP-Azure-MAANG-Sheet.md) | Cloud SIEM, CSPM, threat detection, AWS/GCP/Azure integrations, audit trail |
| 15 | [03-Senior-Production/15-Datadog-RUM-Synthetic-Frontend-Performance-CoreWebVitals-Gold-Sheet.md](03-Senior-Production/15-Datadog-RUM-Synthetic-Frontend-Performance-CoreWebVitals-Gold-Sheet.md) | RUM SDK, session replay, Core Web Vitals, synthetic browser/API tests, apdex |
| 16 | [03-Senior-Production/16-Datadog-Cost-Tagging-Strategy-Cardinality-Retention-MAANG-Sheet.md](03-Senior-Production/16-Datadog-Cost-Tagging-Strategy-Cardinality-Retention-MAANG-Sheet.md) | custom metric cardinality, log exclusion, span retention, tag governance, cost attribution |

Senior target:

- You can design monitors, SLOs, and error budgets for production services.
- You can operate Datadog across Kubernetes clusters and cloud environments.
- You can manage cost through tag strategy, cardinality control, and retention policies.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-Scenario-High-Latency-APM-Flamegraph-Investigation-MAANG-Sheet.md](04-Scenario-Practice/17-Scenario-High-Latency-APM-Flamegraph-Investigation-MAANG-Sheet.md) | p99 latency spike, flame graph, span analysis, service dependency map |
| 18 | [04-Scenario-Practice/18-Scenario-Log-Error-Spike-Trace-Correlation-Root-Cause-MAANG-Sheet.md](04-Scenario-Practice/18-Scenario-Log-Error-Spike-Trace-Correlation-Root-Cause-MAANG-Sheet.md) | log error surge, pivot to traces, root cause via spanID/traceID |
| 19 | [04-Scenario-Practice/19-Scenario-Kubernetes-OOMKilled-CrashLoop-Container-Debug-MAANG-Sheet.md](04-Scenario-Practice/19-Scenario-Kubernetes-OOMKilled-CrashLoop-Container-Debug-MAANG-Sheet.md) | OOMKilled pods, CrashLoopBackOff, container memory debug with Datadog |
| 20 | [04-Scenario-Practice/20-Scenario-Database-Slow-Query-APM-DBM-NPlus1-MAANG-Sheet.md](04-Scenario-Practice/20-Scenario-Database-Slow-Query-APM-DBM-NPlus1-MAANG-Sheet.md) | Database Monitoring, slow query traces, N+1 detection, explain plan via DBM |
| 21 | [04-Scenario-Practice/21-Scenario-Alert-Fatigue-Monitor-Tuning-Composite-Alerts-MAANG-Sheet.md](04-Scenario-Practice/21-Scenario-Alert-Fatigue-Monitor-Tuning-Composite-Alerts-MAANG-Sheet.md) | alert fatigue, noise reduction, composite monitors, routing and suppression |
| 22 | [04-Scenario-Practice/22-Scenario-SLO-Burn-Rate-Error-Budget-Exhaustion-MAANG-Sheet.md](04-Scenario-Practice/22-Scenario-SLO-Burn-Rate-Error-Budget-Exhaustion-MAANG-Sheet.md) | burn rate alert, error budget exhaustion, SLO breach response |
| 23 | [04-Scenario-Practice/23-Scenario-Broken-Trace-Context-Propagation-Debug-MAANG-Sheet.md](04-Scenario-Practice/23-Scenario-Broken-Trace-Context-Propagation-Debug-MAANG-Sheet.md) | broken distributed traces, missing spanID/traceID, context propagation debugging |

Scenario target:

- You can resolve production incidents using Datadog signal correlation.
- You can debug broken traces, alert fatigue, SLO breaches, and Kubernetes failures.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-Datadog-Interview-QnA-Beginner-to-MAANG-Sheet.md](05-Special-Interview-Rounds/24-Datadog-Interview-QnA-Beginner-to-MAANG-Sheet.md) | Datadog Q&A from beginner to MAANG |
| 25 | [05-Special-Interview-Rounds/25-Datadog-Query-Language-Metrics-Logs-APM-Cheatsheet.md](05-Special-Interview-Rounds/25-Datadog-Query-Language-Metrics-Logs-APM-Cheatsheet.md) | metric/log/APM query syntax, DQL, aggregations |
| 26 | [05-Special-Interview-Rounds/26-Datadog-Anti-Patterns-Common-Bugs-Debugging-Traps-Sheet.md](05-Special-Interview-Rounds/26-Datadog-Anti-Patterns-Common-Bugs-Debugging-Traps-Sheet.md) | instrumentation failures, cardinality explosions, broken traces |

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-Datadog-Active-Recall-Spaced-Repetition-Drills.md](06-Practice-Upgrade/27-Datadog-Active-Recall-Spaced-Repetition-Drills.md) | recall prompts across beginner to MAANG |
| 28 | [06-Practice-Upgrade/28-Datadog-Practical-Drills-Hands-On-Practice.md](06-Practice-Upgrade/28-Datadog-Practical-Drills-Hands-On-Practice.md) | configuration and query drills |
| 29 | [06-Practice-Upgrade/29-Datadog-Mini-Projects-Portfolio-Mastery.md](06-Practice-Upgrade/29-Datadog-Mini-Projects-Portfolio-Mastery.md) | portfolio-ready Datadog projects |
| 30 | [06-Practice-Upgrade/30-Datadog-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-Datadog-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |

---

## 9. Advanced Platform Path

| Order | File | What It Builds |
|---:|---|---|
| 31 | [07-Advanced-Platform/31-Datadog-Software-Catalog-IDP-Service-Ownership-Scorecards-MAANG-Sheet.md](07-Advanced-Platform/31-Datadog-Software-Catalog-IDP-Service-Ownership-Scorecards-MAANG-Sheet.md) | Software Catalog, service ownership, entity definitions, scorecards, internal developer portal |
| 32 | [07-Advanced-Platform/32-Datadog-Universal-Service-Monitoring-USM-Code-Free-Service-Discovery-Gold-Sheet.md](07-Advanced-Platform/32-Datadog-Universal-Service-Monitoring-USM-Code-Free-Service-Discovery-Gold-Sheet.md) | USM, code-free service discovery, baseline RED metrics, APM rollout prioritization |
| 33 | [07-Advanced-Platform/33-Datadog-Continuous-Profiler-Code-Hotspots-Production-Performance-MAANG-Sheet.md](07-Advanced-Platform/33-Datadog-Continuous-Profiler-Code-Hotspots-Production-Performance-MAANG-Sheet.md) | Continuous Profiler, CPU/wall/allocation/lock profiles, code hotspots, version comparison |
| 34 | [07-Advanced-Platform/34-Datadog-Dynamic-Instrumentation-Live-Debugging-Probes-MAANG-Sheet.md](07-Advanced-Platform/34-Datadog-Dynamic-Instrumentation-Live-Debugging-Probes-MAANG-Sheet.md) | live debugging, dynamic logs, metric probes, snapshots, RBAC and safety |
| 35 | [07-Advanced-Platform/35-Datadog-Error-Tracking-Issue-Grouping-Exception-Replay-Gold-Sheet.md](07-Advanced-Platform/35-Datadog-Error-Tracking-Issue-Grouping-Exception-Replay-Gold-Sheet.md) | Error Tracking, grouped issues, suspected commits, impact, exception replay, frontend/backend triage |
| 36 | [07-Advanced-Platform/36-Datadog-CI-Visibility-Test-Optimization-DORA-Software-Delivery-Gold-Sheet.md](07-Advanced-Platform/36-Datadog-CI-Visibility-Test-Optimization-DORA-Software-Delivery-Gold-Sheet.md) | CI Visibility, flaky tests, pipeline traces, deployment correlation, DORA metrics |
| 37 | [07-Advanced-Platform/37-Datadog-Observability-Pipelines-Worker-Redaction-Dual-Shipping-MAANG-Sheet.md](07-Advanced-Platform/37-Datadog-Observability-Pipelines-Worker-Redaction-Dual-Shipping-MAANG-Sheet.md) | Observability Pipelines Worker, redaction, enrichment, sampling, routing, archives, dual shipping |
| 38 | [07-Advanced-Platform/38-Datadog-Serverless-Lambda-Extension-Forwarder-Step-Functions-Gold-Sheet.md](07-Advanced-Platform/38-Datadog-Serverless-Lambda-Extension-Forwarder-Step-Functions-Gold-Sheet.md) | serverless monitoring, Lambda Extension/Forwarder, cold starts, timeouts, Step Functions, async queues |
| 39 | [07-Advanced-Platform/39-Datadog-Data-Streams-Monitoring-Kafka-SQS-Kinesis-RabbitMQ-Gold-Sheet.md](07-Advanced-Platform/39-Datadog-Data-Streams-Monitoring-Kafka-SQS-Kinesis-RabbitMQ-Gold-Sheet.md) | Data Streams Monitoring, Kafka/SQS/Kinesis/RabbitMQ topology, end-to-end latency, consumer lag |
| 40 | [07-Advanced-Platform/40-Datadog-Cloud-Cost-Management-FinOps-Unit-Economics-MAANG-Sheet.md](07-Advanced-Platform/40-Datadog-Cloud-Cost-Management-FinOps-Unit-Economics-MAANG-Sheet.md) | Cloud Cost Management, FinOps, tag rules, cost monitors, unit economics, service/team spend |
| 41 | [07-Advanced-Platform/41-Datadog-Application-API-Protection-Code-Security-DevSecOps-MAANG-Sheet.md](07-Advanced-Platform/41-Datadog-Application-API-Protection-Code-Security-DevSecOps-MAANG-Sheet.md) | App/API Protection, API posture, runtime vulnerabilities, Code Security, SAST/SCA/IAST/IaC/secrets |
| 42 | [07-Advanced-Platform/42-Datadog-Incident-Management-On-Call-Workflow-Automation-Watchdog-BitsAI-Gold-Sheet.md](07-Advanced-Platform/42-Datadog-Incident-Management-On-Call-Workflow-Automation-Watchdog-BitsAI-Gold-Sheet.md) | Incident Management, On-Call, Workflow Automation, Watchdog/Event correlation, Bits AI usage |
| 43 | [07-Advanced-Platform/43-Datadog-LLM-Observability-AI-RAG-Token-Cost-Safety-Gold-Sheet.md](07-Advanced-Platform/43-Datadog-LLM-Observability-AI-RAG-Token-Cost-Safety-Gold-Sheet.md) | LLM Observability, RAG traces, model latency, token cost, prompt versions, safety signals |
| 44 | [07-Advanced-Platform/44-Datadog-Network-Mobile-RUM-Platform-Governance-As-Code-MAANG-Sheet.md](07-Advanced-Platform/44-Datadog-Network-Mobile-RUM-Platform-Governance-As-Code-MAANG-Sheet.md) | Network monitoring, mobile RUM, product analytics, RBAC, audit, Terraform/API as code |

Advanced target:

- You can use Datadog as a full engineering platform, not only an observability UI.
- You can connect ownership, delivery, runtime behavior, cost, security, incidents, and governance.
- You can explain where each Datadog product fits and when it is the wrong tool.

---

## 10. Datadog Mastery Lab

- [datadog-mastery-lab/README.md](datadog-mastery-lab/README.md)
- [datadog-mastery-lab/LEARNING_PATH.md](datadog-mastery-lab/LEARNING_PATH.md)

Lab covers: Java/Node/Python instrumentation examples, OTel setup examples, log correlation examples, diagnostic scripts, labs, projects, cheatsheets, interview prep, and runbooks.

---

## 11. Interview Answer Pattern

For Datadog design and debugging answers, use this shape:

```text
1. Signal: which observability signal (metric/log/trace/RUM) answers this question?
2. Instrumentation: how is data collected (agent/SDK/OTel/integration)?
3. Correlation: how are signals linked (trace_id in logs, service/env/version tags)?
4. Alert: what monitor or SLO detects this condition?
5. Investigation: what Datadog view (Trace Explorer/Log Explorer/Dashboard/Service Map) surfaces the root cause?
6. Resolution: what action fixes it and what metric/log/trace confirms the fix?
7. Prevention: what monitor, tag, or architecture change prevents recurrence?
```

---

## 12. Recommended Study Orders

### 2-Week Practical Path

1. Foundations 1-4.
2. APM + instrumentation 5-9.
3. Scenarios 17-23.
4. Query cheatsheet and interview Q&A.

### 4-Week Pro Path

1. Week 1: mental model, agent, metrics, logs.
2. Week 2: APM, Java/Node/Python, OTel, log correlation, dashboards.
3. Week 3: monitors, SLOs, Kubernetes, security, RUM, cost.
4. Week 4: scenarios, runbooks, projects, interview practice.

### 6-Week Platform Mastery Path

1. Week 1: foundations, agent, metrics, logs, tagging.
2. Week 2: APM, language instrumentation, OTel, log correlation.
3. Week 3: dashboards, monitors, SLOs, Kubernetes, RUM, security.
4. Week 4: scenario practice and production incident drills.
5. Week 5: Software Catalog, USM, Profiler, Dynamic Instrumentation, Error Tracking.
6. Week 6: CI Visibility, Observability Pipelines, Serverless, Data Streams, Cloud Cost, Incident Management, LLM Observability, Governance.

### Production Operator Path

1. Learn agent setup, metric collection, and log pipelines.
2. Practice APM instrumentation on a real app.
3. Build SLOs and monitors for your team's services.
4. Write RCA notes after each incident scenario.

---

## 13. Readiness Gate

You are Datadog interview-ready when you can do all of this without notes:

- Explain the three observability pillars and how Datadog unifies them with correlation.
- Install and configure the Datadog Agent on a host, Docker container, and Kubernetes cluster.
- Instrument a Java app with dd-java-agent and manually create custom spans.
- Instrument a Node.js app with dd-trace and a Python app with ddtrace.
- Set up OpenTelemetry SDK for Java, Python, and JS and route via OTLP to Datadog.
- Inject trace_id and span_id into application logs for Java, Python, and Node.js.
- Explain spanID, traceID, parentID, and how they form a distributed trace.
- Build a dashboard with frontend RUM metrics and backend APM service metrics.
- Create monitors with threshold, anomaly, and composite conditions.
- Define an SLO, calculate error budget, and configure burn rate alerts.
- Debug a production latency incident using APM flame graphs and service maps.
- Correlate a log error spike to a specific trace and span.
- Debug broken distributed traces caused by missing context propagation.
- Explain cardinality explosion and how to prevent it with tag governance.
- Use Software Catalog to connect service ownership, scorecards, runbooks, SLOs, and dependencies.
- Explain when to use USM before APM and what visibility USM cannot provide.
- Debug high CPU or allocation regressions with Continuous Profiler.
- Add safe Dynamic Instrumentation probes for production-only bugs.
- Use Error Tracking to group repeated exceptions into deploy-linked issues.
- Connect CI pipeline/test failures and deployment markers to production regressions.
- Design an Observability Pipelines Worker flow for redaction, sampling, archiving, and dual shipping.
- Monitor serverless cold starts, timeouts, throttles, DLQs, Step Functions, and async queue lag.
- Debug Kafka/SQS/Kinesis delays with Data Streams Monitoring.
- Correlate cloud cost with service/team tags, deployments, infrastructure usage, and unit economics.
- Explain App/API Protection, Code Security, runtime vulnerability prioritization, and DevSecOps workflow.
- Run an incident with Datadog Incident Management, On-Call, Workflow Automation, Watchdog/Event correlation, and verified AI assistance.
- Define LLM observability metrics for latency, errors, token cost, prompt versions, RAG retrieval, tool calls, and safety.
- Govern Datadog as a platform using RBAC, SSO/SCIM, audit trail, tag policy, Terraform/API as code, and resource ownership.
