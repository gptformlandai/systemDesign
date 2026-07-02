# 42. Datadog Incident Management, On-Call, Workflow Automation, Watchdog, Bits AI

## Goal

Understand Datadog's service management layer: incidents, on-call routing, automated workflows, event correlation, Watchdog insights, and AI-assisted investigation.

---

## Mental Model

Observability finds symptoms.

Incident management coordinates humans.

Automation performs repeatable actions.

AI/correlation surfaces likely context.

```text
monitor fires -> incident declared -> owner paged -> evidence gathered -> mitigation -> postmortem -> prevention
```

---

## Incident Object

An incident should capture:

```text
title
severity
state
incident commander
owning team
affected services
customer impact
timeline
monitors involved
dashboards/traces/logs
Slack/PagerDuty links
postmortem
follow-up tasks
```

The incident object is the shared truth during response.

---

## Severity Model

| Severity | Meaning | Example |
|---|---|---|
| SEV-1 | Major customer/business outage | checkout unavailable globally |
| SEV-2 | Significant degraded service | payment latency high in one region |
| SEV-3 | Partial/minor impact | one async worker delayed |
| SEV-4 | Internal issue/no customer impact | dashboard broken |

Severity should be based on impact, not how scary the alert looks.

---

## Incident Lifecycle

```text
1. Detection
   monitor, SLO burn, Watchdog, customer report

2. Declaration
   create incident, set severity, assign commander

3. Triage
   identify impacted services, users, and owners

4. Mitigation
   rollback, scale, failover, config change, disable feature

5. Resolution
   confirm metrics/SLO/user impact recovered

6. Postmortem
   timeline, root cause, contributing factors, action items

7. Prevention
   monitor, test, runbook, architecture change, ownership fix
```

---

## On-Call Routing

Good routing depends on tags:

```text
service:orders-service
team:checkout
env:production
severity:high
tier:critical
```

Routing rules:

```text
if team:checkout and env:production -> checkout primary on-call
if security signal critical -> security incident channel
if cloud account prod-main -> platform cloud on-call
```

---

## Workflow Automation Examples

| Trigger | Automation |
|---|---|
| High CPU on Kubernetes service | capture pod list, restart count, top containers |
| SLO burn alert | create incident, attach SLO, notify owner |
| Lambda DLQ growth | fetch queue metrics, post runbook |
| Security signal critical | create case, enrich IP, notify security |
| Deployment regression | create rollback checklist |

Automation should gather evidence and perform safe actions. Destructive remediation needs guardrails.

---

## Watchdog And Event Correlation

Watchdog-style insights help surface anomalies and correlations:

```text
latency anomaly started after deployment
error spike correlated with one availability zone
DB connection saturation linked to checkout failures
new exception appeared in version 2.4.1
```

Use these as leads, not proof. Confirm with metrics, logs, traces, and timeline.

---

## Bits AI Usage Pattern

AI-assisted investigation can help:

- summarize incident context
- find relevant dashboards
- explain monitor history
- suggest likely related events
- draft postmortem notes
- answer "what changed?"

But the responder owns the conclusion. AI output needs verification.

---

## Incident Evidence Template

```text
Incident:
  title:
  severity:
  commander:
  services:
  start time:
  detected by:

Impact:
  users affected:
  regions affected:
  SLO/error budget impact:

Evidence:
  monitor:
  dashboard:
  trace:
  logs:
  deployment:
  dependency:

Mitigation:
  action taken:
  time recovered:
  verification metric:

Follow-ups:
  prevention:
  owner:
  due date:
```

---

## Postmortem Quality Bar

A good postmortem includes:

- customer impact in plain language
- exact timeline
- detection gap
- root cause and contributing factors
- what worked
- what failed
- action items with owners and dates
- monitor/SLO/runbook updates

Avoid blame. Focus on system improvement.

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Incident starts in random chat | Timeline gets lost | Declare incident object early |
| No commander | Everyone investigates, nobody coordinates | Assign commander |
| Paging by monitor name only | Wrong team paged | Route by service/team tags |
| Postmortem without action owners | No prevention | Assign dated follow-ups |
| Trusting AI summary blindly | Wrong RCA risk | Verify with evidence |

---

## Practical Question

> A p99 latency SLO burn alert fires for checkout. Explain your Datadog incident workflow.

---

## Strong Answer

I would declare an incident from the SLO burn alert, set severity based on customer impact, and assign an incident commander. The incident would attach the affected service, SLO, monitor, dashboard, and current timeline. On-call routing should page the owning team from `service` and `team` tags.

Then I would use APM, logs, dashboards, deployment events, Watchdog insights, and dependency maps to identify what changed and where latency is coming from. Workflow Automation can gather standard evidence like recent deploys, slow traces, pod restarts, DB metrics, and linked runbooks. After mitigation, I would verify recovery with the SLO and user-facing metrics, then write a postmortem with action items.

---

## Interview Sound Bite

Datadog Incident Management turns observability signals into coordinated response. The mature workflow is detection, declaration, ownership, evidence, mitigation, verification, postmortem, and prevention, all tied together by service/team tags.
