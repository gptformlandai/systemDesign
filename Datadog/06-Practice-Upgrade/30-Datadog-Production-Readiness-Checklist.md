# 30. Datadog Production Readiness Checklist

Score yourself: each item is worth 1 point.
60+ / 75 = production ready.
68+ / 75 = MAANG ready.
75 / 75 = Datadog platform SME level.

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

## Section 9: Advanced Datadog Platform Products (16 points)

- [ ] Can define a Software Catalog entity with owner, lifecycle, tier, contacts, runbook, repository, and dashboard links.
- [ ] Can design a service scorecard for ownership, SLOs, monitors, runbooks, security, cost tags, and deployment visibility.
- [ ] Can explain Universal Service Monitoring, when to use it before APM, and what visibility it cannot provide.
- [ ] Can use Continuous Profiler to compare CPU, wall-time, allocation, or lock profiles across versions.
- [ ] Can safely design a Dynamic Instrumentation probe with scope, condition, expiration, and PII controls.
- [ ] Can use Error Tracking to group repeated exceptions, identify first-seen version, suspected commit, and impacted users.
- [ ] Can connect CI Visibility data to pipeline duration, flaky tests, deployment markers, and DORA metrics.
- [ ] Can design an Observability Pipelines Worker flow for redaction, enrichment, sampling, routing, archiving, and dual shipping.
- [ ] Can monitor Lambda cold starts, timeouts, memory, throttles, DLQs, Step Functions, and async event source lag.
- [ ] Can debug Kafka/SQS/Kinesis/RabbitMQ pipeline delay using Data Streams Monitoring topology, lag, latency, and payload size.
- [ ] Can distinguish Datadog usage cost control from Cloud Cost Management and explain unit economics.
- [ ] Can prioritize cloud spend by service/team/env, deployment correlation, resource type, and cost per business unit.
- [ ] Can explain App/API Protection, API posture, credential stuffing, account takeover, and runtime attack detection.
- [ ] Can explain Code Security coverage across SAST, SCA, IAST, IaC scanning, secret scanning, and SBOM.
- [ ] Can define LLM observability metrics for token cost, model latency, prompt template version, RAG retrieval, tool calls, and safety.
- [ ] Can explain when network monitoring or mobile RUM is needed beyond backend APM.

---

## Section 10: Platform Governance And Operations (10 points)

- [ ] Can route alerts and incidents by service/team/env/tier tags instead of monitor names alone.
- [ ] Can run an incident using declaration, severity, commander, affected services, evidence, mitigation, verification, and postmortem.
- [ ] Can use Workflow Automation to gather incident evidence without unsafe automatic remediation.
- [ ] Can explain Watchdog/Event correlation and how to verify AI-assisted incident summaries before trusting them.
- [ ] Can define RBAC roles for viewers, service owners, platform admins, security admins, and incident commanders.
- [ ] Can explain SSO, SCIM, API/application key rotation, restricted data access, and audit trail.
- [ ] Can manage monitors, dashboards, SLOs, synthetics, pipelines, and roles using Terraform/API as code.
- [ ] Can write quality rules for as-code Datadog resources: owner tags, runbook, scoped query, pager route, and no secrets.
- [ ] Can connect Software Catalog ownership to incident routing, security findings, cost attribution, and scorecards.
- [ ] Can design a Datadog platform operating model for 50+ teams with templates, governance, cost controls, and lifecycle standards.

---

## Scoring Rubric

| Score | Meaning |
|---|---|
| 45-54 | Strong foundations. Ready for junior SRE/DevOps Datadog roles. |
| 55-59 | Strong production observability. Ready for mid-level SRE/backend roles with Datadog on the stack. |
| 60-67 | Production ready. Can own Datadog instrumentation, dashboards, monitors, SLOs, and incident workflows for a team. |
| 68-74 | MAANG ready. Can design observability, service ownership, delivery correlation, security, cost, and incident response across many services. |
| 75 | Datadog platform SME level. Can design and govern Datadog as an enterprise engineering platform for 50+ teams. |

---

## Gap Fill Recommendations

If missing Section 2 items: spend one day building a Java Spring Boot + dd-java-agent app.

If missing Section 3 items: build the OTel project from Project 3 in sheet 29.

If missing Section 5 items: build the dashboard from Project 1 and write one composite monitor.

If missing Section 8 items: simulate each scenario in the datadog-mastery-lab environments.

If missing Section 9 items: work through the `07-Advanced-Platform` sheets in order and map each product to one real production use case.

If missing Section 10 items: create a small platform-as-code repository with one dashboard, one monitor, one SLO, one synthetic test, one team tag policy, and one incident workflow template.
