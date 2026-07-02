# 44. Datadog Network, Mobile RUM, Platform Governance, As Code

## Goal

Cover the remaining platform maturity areas: network monitoring, mobile RUM, governance, RBAC, auditability, and managing Datadog resources as code.

---

## Mental Model

Datadog at small scale is a tool.

Datadog at enterprise scale is a platform.

```text
observability signals
  + ownership
  + governance
  + security
  + automation
  + as-code management
  = reliable operating system for engineering
```

---

## Network Performance Monitoring

Network Performance Monitoring (NPM) helps answer:

- Which services communicate?
- Is latency caused by network or application code?
- Is traffic cross-AZ or cross-region?
- Are retransmits or packet loss rising?
- Which dependency is consuming bandwidth?

Important signals:

```text
TCP latency
TCP retransmits
connection count
bytes sent/received
source/destination service
source/destination IP/subnet/AZ/region
```

---

## Network Device Monitoring

Network Device Monitoring is for infrastructure such as:

```text
routers
switches
firewalls
load balancers
VPN devices
on-prem appliances
```

Common data sources:

```text
SNMP metrics
NetFlow/sFlow/IPFIX flow data
device status
interface throughput
packet drops
errors
```

Use it when hybrid/on-prem networks matter to application reliability.

---

## Network Debug Scenario

```text
Symptom:
  checkout-service p99 latency increased, but DB query spans are normal.

Check:
  APM shows downstream call to payment-service slow.
  NPM shows high retransmits between AZ-a and AZ-c.
  Infrastructure shows no CPU issue.

Conclusion:
  Network path degradation, not application code.
```

APM tells you which dependency is slow. NPM tells you whether the network path is unhealthy.

---

## Mobile RUM

Mobile RUM extends user monitoring to iOS, Android, React Native, and similar mobile clients.

Track:

```text
app startup time
screen load time
crashes
mobile errors
network requests
resource timing
user actions
session replay where supported
device model
OS version
app version
carrier/network type
```

Mobile observability must split by app version and OS version because rollout and platform behavior vary heavily.

---

## Mobile RUM Debug Scenario

```text
Symptom:
  Checkout failures increased only for Android users.

Filters:
  application:mobile-app
  os.name:Android
  app.version:6.8.1
  view.name:Checkout

Finding:
  crash grouped in Error Tracking after version 6.8.1 release.

Action:
  halt staged rollout, patch null handling, release 6.8.2.
```

---

## Product Analytics

RUM and Product Analytics help connect technical performance to user behavior:

```text
conversion funnel
checkout drop-off
rage clicks
frustration signals
feature adoption
page/screen performance
user journey
```

Use this carefully: product analytics should not replace privacy review.

---

## Datadog Governance

Enterprise Datadog needs rules:

```text
RBAC roles
SAML/SSO
SCIM user lifecycle
API/application key rotation
audit trail
restricted data access
team ownership
monitor ownership
dashboard ownership
tag policy
cost policy
```

The same governance mindset used for cloud platforms applies to Datadog.

---

## RBAC Pattern

| Role | Permissions |
|---|---|
| Viewer | read dashboards, monitors, notebooks |
| Service Owner | manage own service monitors/dashboards |
| Observability Platform Admin | manage integrations, pipelines, org settings |
| Security Admin | security rules, sensitive data, audit views |
| Incident Commander | manage incidents and response workflow |

Avoid giving every engineer full admin rights.

---

## Datadog As Code

Manage critical resources through code:

```text
monitors
dashboards
SLOs
synthetic tests
logs indexes
pipelines
rum applications
roles
teams
integrations
```

Common tools:

```text
Terraform provider
Datadog API
CI/CD validation
policy-as-code checks
```

---

## Terraform Monitor Example

```hcl
resource "datadog_monitor" "orders_high_error_rate" {
  name = "orders-service high error rate"
  type = "query alert"

  query = "sum(last_5m):sum:trace.http.request.errors{service:orders-service,env:production}.as_count() / sum:trace.http.request.hits{service:orders-service,env:production}.as_count() > 0.02"

  message = <<EOT
High error rate for orders-service.
@pagerduty-checkout-platform
Runbook: https://wiki.company.com/runbooks/orders-service
EOT

  tags = [
    "service:orders-service",
    "team:checkout",
    "env:production",
    "managed_by:terraform"
  ]
}
```

---

## As-Code Quality Rules

Before merging Datadog config:

- Monitor has owner/team/service/env tags.
- Alert has runbook link.
- Pager route is correct.
- Query has scoped tags.
- SLO has clear numerator/denominator.
- Dashboard has template variables.
- Synthetic tests use secure credentials.
- No secret appears in Terraform state or variables.

---

## Platform Anti-Patterns

| Anti-Pattern | Impact | Fix |
|---|---|---|
| ClickOps monitors | Drift and no review | Terraform/API |
| No tag policy | Broken routing and cost attribution | mandatory tags |
| Everyone admin | security risk | RBAC |
| No audit trail review | changes untraceable | audit monitors |
| No ownership | dashboards/monitors rot | team ownership |
| No config promotion | prod changes untested | dev/stage/prod workflow |

---

## Practical Question

> Your company has 50 teams using Datadog. Monitors are noisy, dashboards are duplicated, and nobody knows who owns half the resources. What platform changes would you make?

---

## Strong Answer

I would treat Datadog as an internal platform. First, enforce mandatory tags such as service, team, env, owner, and managed_by. Second, move critical monitors, SLOs, dashboards, synthetics, and pipelines into Terraform or API-managed repositories with code review. Third, define RBAC roles so teams can manage their services without global admin access.

Then I would connect resources to Software Catalog ownership, use scorecards for production readiness, monitor Datadog usage and audit logs, and create templates for common service dashboards and SLOs. This reduces drift, improves routing, controls cost, and makes observability maintainable across many teams.

---

## Interview Sound Bite

Enterprise Datadog maturity is governance plus observability. The platform should have RBAC, SSO/SCIM, audit trail, tag policy, resource ownership, Terraform/API management, network visibility, mobile RUM, and service catalog standards.
