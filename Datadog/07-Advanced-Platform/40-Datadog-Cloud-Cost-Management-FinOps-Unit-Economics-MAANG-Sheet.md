# 40. Datadog Cloud Cost Management: FinOps, Tag Rules, Unit Economics

## Goal

Understand Cloud Cost Management as a FinOps layer that correlates cloud spend with services, teams, infrastructure usage, deployments, and reliability.

---

## Mental Model

Datadog usage cost answers:

```text
How much are we spending on Datadog?
```

Cloud Cost Management answers:

```text
How much cloud spend does each service/team/product generate, and why?
```

One controls observability spend. The other controls AWS/GCP/Azure spend.

---

## Why It Exists

Cloud bills are hard to understand because:

- Resources are shared.
- Tags are missing or inconsistent.
- Autoscaling changes cost daily.
- Data transfer is invisible until the bill arrives.
- Engineers lack cost visibility during design.
- Finance sees spend but not technical cause.

Cloud Cost Management connects cost to engineering context.

---

## Core Concepts

| Concept | Meaning |
|---|---|
| Cost ingestion | Import cloud billing data |
| Cost allocation | Assign spend to team/service/env/product |
| Tag rules | Normalize or require cost tags |
| Cost monitors | Alert when spend exceeds expectation |
| Unit economics | Cost per request/order/user/job |
| Cost correlation | Compare spend with infra metrics and deployments |

---

## Mandatory Cost Tags

```text
team:checkout
service:orders-service
env:production
product:commerce
cost_center:cc-1042
region:us-east-1
owner:checkout-platform
```

Cost tags should match observability tags. If `service` means one thing in APM and another in billing, FinOps reporting breaks.

---

## Cloud Cost Views

| View | Question |
|---|---|
| Cost by service | Which service is most expensive? |
| Cost by team | Which team owns the spend? |
| Cost by product | Which business unit drives cost? |
| Cost by region | Is one region abnormal? |
| Cost by resource | Which resource spiked? |
| Cost by tag coverage | Which resources are untagged? |

---

## Cost Monitor Examples

### Daily Spend Spike

```text
Alert:
  team:checkout daily cloud cost > 30% above 14-day baseline
```

### Untagged Resource Growth

```text
Alert:
  untagged cost > 5% of total spend
```

### Service Unit Cost Regression

```text
Alert:
  cost_per_order for service:orders-service > $0.015 for 3 days
```

---

## Unit Economics

Unit economics ties spend to business volume.

```text
cost_per_order =
  monthly cloud cost for checkout services
  /
  monthly completed orders

cost_per_1000_requests =
  service infrastructure cost
  /
  request_count * 1000
```

In Datadog, combine cloud cost with metrics such as:

```text
orders.completed
trace.request.hits
api.requests
jobs.processed
rum.sessions
```

---

## Investigation Workflow

```text
Alert:
  payments-service cost increased 45% week over week.

Step 1: Split cost by resource type.
  NAT Gateway data transfer increased.

Step 2: Split by region and AZ.
  us-east-1 only.

Step 3: Correlate with deployment events.
  version 3.2.0 deployed same day.

Step 4: Check APM dependency map.
  service now calls external fraud API through NAT per request.

Step 5: Fix architecture.
  add VPC endpoint, cache result, batch calls, or move dependency path.
```

---

## FinOps Governance

Use policy, not heroics:

```text
1. Require cost tags at provisioning time.
2. Block production resources with missing owner/team tags.
3. Review top cost movers weekly.
4. Add cost monitors for critical services.
5. Include cost impact in design reviews.
6. Tie service catalog ownership to cost ownership.
7. Track unit cost, not only total cost.
```

---

## Cost And Reliability Trade-Offs

| Decision | Cost Impact | Reliability Impact |
|---|---|---|
| More replicas | Higher compute | Better availability/latency |
| Multi-region active-active | Much higher | Better resilience |
| Lower log retention | Lower | Less forensic history |
| Bigger DB instance | Higher | Lower query latency |
| Aggressive autoscaling | Lower idle cost | Possible cold capacity |

Senior answer: cost is a design constraint, not the only objective.

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Only finance sees cost | Engineers cannot fix causes | Put cost in engineering dashboards |
| Missing tags tolerated | Spend becomes unowned | Enforce tag policy |
| Tracking total cost only | Growth hides inefficiency | Track unit cost |
| Blind cost cutting | Reliability suffers | Compare cost with SLOs |
| No deployment correlation | Regressions hidden | Overlay deploys on cost charts |

---

## Practical Question

> Cloud spend increased 35% this month, but traffic increased only 5%. How would you investigate with Datadog?

---

## Strong Answer

I would start by splitting cost by team, service, region, and resource type to find the largest mover. Then I would check tag coverage to make sure spend is allocated correctly. For the suspicious service, I would correlate cost change with deployments, infrastructure metrics, APM dependency changes, request volume, and error/retry rates.

If total cost rose faster than traffic, I would calculate unit cost, such as cost per request or cost per order. Then I would identify whether the cause is over-provisioning, data transfer, retry storms, inefficient code, expensive storage, or architecture changes. The fix should preserve SLOs while reducing waste.

---

## Interview Sound Bite

Datadog Cloud Cost Management connects cloud spend to engineering context. The mature FinOps move is to tag by service/team/env, monitor cost anomalies, correlate spend with deployments and usage, and optimize unit cost without breaking reliability.
