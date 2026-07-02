# 31. Datadog Software Catalog, Internal Developer Portal, Service Ownership, Scorecards

## Goal

Understand how Datadog Software Catalog turns raw telemetry into an operating model for service ownership, reliability standards, dependency visibility, and platform governance.

---

## Mental Model

Classic observability answers:

```text
What is broken?
Where is it slow?
Which trace/log/metric proves it?
```

Software Catalog answers:

```text
Who owns this service?
What does it depend on?
Does it meet production standards?
Which team should be paged?
Is it reliable, secure, and cost-aware?
```

APM shows runtime behavior. Software Catalog turns runtime behavior into a living service inventory.

---

## Why It Exists

In small systems, everyone knows who owns every service. In a 100-service organization, ownership becomes tribal knowledge.

Without a catalog:

- Alerts go to the wrong team.
- Services exist without owners.
- Critical dependencies are discovered during incidents.
- Reliability standards are enforced manually.
- New engineers cannot understand the system map quickly.

Software Catalog makes ownership and standards queryable.

---

## Core Objects

| Object | Meaning |
|---|---|
| Entity | A service, API, datastore, queue, library, or component represented in the catalog |
| Service | Runtime application unit such as `orders-api` or `payment-worker` |
| Owner | Team, Slack channel, PagerDuty service, or responsible group |
| Dependency | Upstream/downstream relationship detected from telemetry or declared manually |
| Scorecard | Rules that measure whether a service meets engineering standards |
| Definition | Metadata file or imported record describing the entity |

---

## Entity Definition Pattern

Store entity metadata close to code, usually in a repo-owned file.

```yaml
apiVersion: v3
kind: service
metadata:
  name: orders-service
  displayName: Orders Service
  tags:
    - env:production
    - team:checkout
spec:
  owner: checkout-platform
  lifecycle: production
  tier: critical
  type: web
  languages:
    - java
  contacts:
    slack: "#team-checkout-alerts"
    pagerduty: checkout-platform-primary
  links:
    repo: https://github.com/company/orders-service
    runbook: https://wiki.company.com/runbooks/orders-service
    dashboard: https://app.datadoghq.com/dashboard/orders
```

Interview point: catalog metadata should be version-controlled, reviewed, and owned by the same team that owns the service.

---

## How Services Enter The Catalog

```text
1. Datadog detects services from APM, USM, RUM, logs, or infrastructure telemetry.
2. The service appears with runtime metadata such as service/env/version.
3. The team declares ownership and business metadata in an entity definition.
4. Datadog enriches the service with dependencies, SLOs, monitors, security findings, and cost data.
5. Scorecards evaluate whether the service meets production standards.
```

Detected telemetry is not enough. A mature platform adds declared ownership.

---

## Scorecards

Scorecards convert engineering standards into visible checks.

| Standard | Example Check |
|---|---|
| Ownership | Service has `owner`, Slack channel, and PagerDuty route |
| Observability | Service has APM, logs, dashboard, and trace/log correlation |
| Reliability | Service has at least one SLO and burn-rate monitor |
| Security | Service has no critical vulnerabilities or exposed secrets |
| Cost | Service has mandatory tags for cost attribution |
| Runbooks | Service has a linked incident runbook |
| Deployment | Service emits version tags and deployment events |

---

## Production Service Scorecard Example

```text
Production Readiness Scorecard

Required:
  - owner is present
  - team tag is present
  - service/env/version tags are present
  - SLO exists for availability or latency
  - at least one dashboard exists
  - at least one monitor routes to owning team
  - runbook link exists
  - critical security findings = 0
  - last deployment visible in Datadog

Warning:
  - no synthetic test for public endpoint
  - no RUM correlation for frontend service
  - custom metric cardinality above team budget
```

---

## Service Catalog Views

| View | Use |
|---|---|
| Ownership | Find team, contact, repository, runbook |
| Reliability | See SLOs, monitors, incidents, error rate |
| Performance | See latency, throughput, dependencies |
| Security | See vulnerabilities, posture, API findings |
| Cost | Attribute cloud and Datadog spend |
| Delivery | See deployments, CI health, version freshness |

This is why the catalog is an internal developer portal, not just a list of services.

---

## Service Ownership Operating Model

Use this ownership hierarchy:

```text
service -> owning team -> escalation policy -> runbook -> dashboard -> SLO -> source repo
```

Minimum ownership fields:

```text
service:orders-service
team:checkout
owner:checkout-platform
pagerduty:checkout-platform-primary
slack:#team-checkout-alerts
repo:github.com/company/orders-service
runbook:wiki/runbooks/orders-service
```

---

## Dependency Mapping

Software Catalog becomes powerful when paired with APM and USM:

```text
frontend-web
  -> api-gateway
      -> orders-service
          -> payments-service
          -> inventory-service
          -> orders-db
          -> order-events-kafka
```

During an incident, the dependency graph answers:

- Who is upstream and impacted?
- Who is downstream and likely causing failure?
- Which team owns the dependency?
- Which SLO is burning?
- Which runbook should the responder open?

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Relying only on auto-detected services | Ownership remains missing | Add entity definitions in repos |
| Using inconsistent service names | APM, logs, monitors, catalog fragment | Standardize `DD_SERVICE` |
| Treating catalog as documentation only | It becomes stale | Connect it to telemetry and scorecards |
| No scorecards | Standards stay subjective | Measure standards automatically |
| No lifecycle field | Deprecated services look production-critical | Track experimental, staging, production, deprecated |

---

## Practical Question

> Your company has 200 microservices. During incidents, alerts often route to the wrong team and nobody knows who owns internal dependencies. How would you use Datadog Software Catalog?

---

## Strong Answer

I would use Software Catalog as the ownership layer on top of observability. APM and USM would auto-detect services and dependencies, but every production service must also define an entity file in its source repo with owner, team, lifecycle, tier, Slack, PagerDuty, runbook, repository, and dashboard links.

Then I would create scorecards for production readiness: unified service tags, SLO present, monitor routing present, runbook present, critical security findings zero, and deployment visibility enabled. During incidents, responders can pivot from a failing trace or service map node directly to the owning team and runbook. This reduces MTTR because ownership and dependency context are available at the exact point of investigation.

Trade-off: it requires metadata governance. The fix is to make catalog definitions part of CI checks and service onboarding.

---

## Interview Sound Bite

Software Catalog is the control plane for service ownership. Metrics, logs, and traces show behavior; the catalog shows who owns the behavior, what the service depends on, which standards it meets, and who should respond when it fails.
