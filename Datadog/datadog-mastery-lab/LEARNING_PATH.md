# Datadog Mastery Lab — Learning Path

## Week 1: Foundations

| Day | Activity | Sheet | Lab |
|---|---|---|---|
| Mon | Datadog mental model and agent architecture | Sheet 01 | Lab 01 (agent setup) |
| Tue | Install agent on Docker, send first metric | Sheet 02 | Lab 01 continued |
| Wed | Metric types, DogStatsD, custom metrics | Sheet 03 | Lab 01 (custom metrics) |
| Thu | Log collection, pipelines, Grok parsing | Sheet 04 | Lab 06 (log setup) |
| Fri | Practice: active recall drills Level 1 | Sheet 27 | n/a |

**Week 1 Goal**: Agent is running, custom metric appears in Metrics Explorer, log pipeline parses one JSON log correctly.

---

## Week 2: APM And Instrumentation

| Day | Activity | Sheet | Lab |
|---|---|---|---|
| Mon | APM concepts: traceID, spanID, sampling | Sheet 05 | Trace Explorer exploration |
| Tue | Java dd-java-agent with Spring Boot | Sheet 06 | Lab 02 (Java APM) |
| Wed | Node.js dd-trace with Express | Sheet 07 | Lab 03 (Node.js APM) |
| Thu | Python ddtrace with FastAPI | Sheet 07 | Lab 04 (Python APM) |
| Fri | Log correlation: inject trace IDs in all three | Sheet 09 | Lab 06 (log correlation) |

**Week 2 Goal**: Spring Boot app, Express app, and FastAPI app all sending traces. Flame graphs visible. Log entries include dd.trace_id and link to traces.

---

## Week 3: OpenTelemetry And Dashboards

| Day | Activity | Sheet | Lab |
|---|---|---|---|
| Mon | OpenTelemetry Java SDK with OTLP to Datadog | Sheet 08 | Lab 05 (OTel Java) |
| Tue | OpenTelemetry Python and Node.js SDK | Sheet 08 | Lab 05 continued |
| Wed | Build full-stack dashboard: APM + RUM + infra | Sheet 10 | Lab 07 (dashboard) |
| Thu | Monitors: threshold, anomaly, composite | Sheet 11 | Lab 08 (monitors) |
| Fri | SLOs, error budgets, burn rate alerts | Sheet 12 | Lab 08 continued |

**Week 3 Goal**: OTel Java app sending to Datadog via OTLP. Full-stack dashboard built. One SLO with burn rate alerts configured.

---

## Week 4: Senior Production And Scenarios

| Day | Activity | Sheet | Lab |
|---|---|---|---|
| Mon | Kubernetes monitoring with Helm + Cluster Agent | Sheet 13 | Lab 09 (Kubernetes) |
| Tue | Security: CSPM, SIEM, AWS integration | Sheet 14 | Review only |
| Wed | RUM, Synthetic tests, Core Web Vitals | Sheet 15 | Lab 07 (RUM widgets) |
| Thu | Cost, cardinality, tagging strategy | Sheet 16 | Audit existing metrics |
| Fri | Scenario drills: sheets 17-23 | Sheets 17-23 | Lab 10 (incident simulation) |

**Week 4 Goal**: Kubernetes deployment monitored. SLO dashboard live. Can walk through each scenario without looking at notes.

---

## Week 5: Interview Preparation

| Day | Activity | Sheet | Lab |
|---|---|---|---|
| Mon | Q&A review: beginner to MAANG | Sheet 24 | n/a |
| Tue | Query language cheatsheet practice | Sheet 25 | Write 10 queries from memory |
| Wed | Anti-patterns review and common bugs | Sheet 26 | Debug a broken trace |
| Thu | Portfolio project completion | Sheet 29 | Project 01 or 02 |
| Fri | Production readiness checklist scoring | Sheet 30 | Score self, identify gaps |

**Week 5 Goal**: Score 40+ on production readiness checklist. All six anti-patterns understood from memory. One portfolio project on GitHub.

---

## Daily Study Pattern

```text
10 minutes: review yesterday's active recall (Sheet 27)
30 minutes: read the day's assigned sheet
20 minutes: lab or practice exercise
10 minutes: write interview answer for the day's key concept in your own words
```

---

## Lab Priority For Interviews

If short on time, do these five labs only:

1. Lab 02 (Java APM): most commonly asked in interviews.
2. Lab 06 (log correlation): second most commonly asked.
3. Lab 08 (monitors + SLOs): third most commonly asked.
4. Lab 05 (OTel): shows vendor-neutral knowledge.
5. Lab 09 (Kubernetes): required for any cloud/SRE role.
