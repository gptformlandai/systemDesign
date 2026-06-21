# GitHub Actions Platform Observability Cost Operating Model Gold Sheet

> Track: GitHub Actions Interview Track - Senior / MAANG Platform Layer

Goal: operate GitHub Actions as a CI/CD platform: reliability, observability, cost, runner capacity, workflow ownership, golden paths, policy, migrations, incident response, and continuous improvement.

---

## 0. How To Read This

Use this after runners/governance, caching/performance, reusable workflows, and production operations.

Platform mental model:

```text
workflow catalog -> owners -> runners/capacity -> metrics/logs -> cost -> policy -> support -> continuous improvement
```

Interview rule:

```text
At company scale, GitHub Actions is not just YAML. It is a developer platform with SLOs, cost, security, support, and governance.
```

---

# Topic 1: Actions Platform Operations

## 1. Intuition

For one repo, Actions is a CI tool.

For hundreds of repos, Actions becomes a platform.

A platform team must answer:

- Are workflows reliable?
- Are PR checks fast enough?
- Are runners available?
- Are teams using secure patterns?
- Which workflows cost the most?
- Who owns failures?
- Which templates are approved?
- How do we migrate old workflows safely?
- How do we support incidents?

---

## 2. Definition

- Definition: GitHub Actions platform operations is the practice of managing CI/CD workflows, runners, templates, policies, metrics, cost, support, and incident response across many repositories and teams.
- Category: developer platform engineering and CI/CD operations.
- Core idea: make secure, fast, reliable delivery the default path for all teams.

---

## 3. Why It Exists

Without an operating model:

- every team copies different YAML
- insecure patterns spread
- PR checks become slow/flaky
- runner queues block delivery
- costs rise without ownership
- incidents have no clear owner
- migrations take months
- required checks become unreliable
- deploy workflows bypass approvals
- platform teams become manual support bottlenecks

---

## 4. Platform Ownership Model

Each workflow should have:

- owning team
- business/service context
- trigger type
- required checks status
- deployment authority
- runner type
- secret/OIDC usage
- average duration
- failure rate
- cost estimate
- support channel
- last reviewed date

Workflow catalog example:

```yaml
workflow: service-ci.yml
repo: payments-api
owner: payments-platform
purpose: pull request validation
required_check: true
runner: github-hosted-linux
secrets: none
cloud_role: none
slo: p95_under_12_min
support: '#payments-ci'
```

---

## 5. CI/CD SLIs And SLOs

Useful SLIs:

| SLI | Meaning |
|---|---|
| workflow success rate | percent of runs that pass without retry |
| flaky failure rate | failures that pass on rerun without code change |
| p50/p95 duration | developer wait time |
| queue time | runner capacity pressure |
| required check availability | ability to merge PRs |
| deployment success rate | deploy reliability |
| rollback time | recovery speed |
| runner utilization | capacity efficiency |
| cache hit rate | performance health |
| cost per repo/team | financial accountability |

Example SLOs:

```text
95 percent of required PR checks complete within 15 minutes.
99 percent of production deployment workflows are available during business hours.
Runner queue p95 stays under 2 minutes for standard PR checks.
```

---

## 6. Dashboard Design

Platform dashboard panels:

- workflow run volume by repo/team
- success/failure rate
- flaky rerun rate
- p50/p95 workflow duration
- queue time by runner label/group
- runner utilization and autoscaling lag
- top costly workflows
- cache hit/miss rate
- artifact storage growth
- deployment frequency/success/failure
- OIDC/secrets usage inventory
- required check failures blocking merge
- self-hosted runner offline count

Strong answer:

```text
I separate developer-experience metrics like PR wait time from production-delivery metrics like deployment success and rollback time.
```

---

## 7. Cost Model

Cost drivers:

- runner minutes
- larger runners
- self-hosted infrastructure
- artifact storage
- cache storage
- repeated flaky reruns
- large matrix builds
- preview environments
- scheduled workflows
- Docker builds without layer caching

Cost controls:

- path filters
- affected builds in monorepos
- concurrency cancellation for superseded PR runs
- right-sized matrix
- cache strategy
- artifact retention policy
- preview TTL cleanup
- larger runners only for workloads that benefit
- team-level reporting

Cost interview line:

```text
I reduce CI cost by removing wasted work first: canceled stale runs, narrowed triggers, path filters, cache strategy, and flaky test reduction.
```

---

## 8. Runner Capacity Management

Capacity questions:

- Which jobs require GitHub-hosted vs self-hosted?
- Which labels are overloaded?
- Is queue time from capacity or label mismatch?
- Are heavy jobs blocking lightweight PR checks?
- Are deployment runners isolated?
- Are self-hosted runners ephemeral?
- Are runners patched and monitored?

Controls:

- runner groups by trust level
- labels by capability
- autoscaling pools
- ARC for Kubernetes-based scaling
- separate PR, release, and deployment runner pools
- peak-hour scheduled workflow limits
- queue alerts

---

## 9. Golden Paths

A golden path is an approved reusable workflow/template that teams can adopt safely.

Examples:

- backend PR CI
- frontend PR CI
- Docker build and scan
- Terraform plan/apply
- Kubernetes deploy
- release creation
- OIDC cloud login
- security scanning

Good golden path qualities:

- versioned
- documented inputs/outputs
- secure defaults
- least-privilege permissions
- clear ownership
- migration guide
- changelog
- deprecation policy

---

## 10. Workflow Standardization

Standardize:

- checkout depth policy
- permissions defaults
- artifact naming
- cache key patterns
- test report upload
- release artifact metadata
- OIDC role assumptions
- deployment environments
- concurrency groups
- reusable workflow versioning
- runner labels
- notification patterns

Avoid:

```text
Everyone copy-pastes an old workflow and edits it until it works.
```

Better:

```text
Provide reusable workflows and templates with secure defaults, then let teams override only the safe extension points.
```

---

## 11. Policy And Guardrails

Guardrails can include:

- required branch checks
- restricted workflow permissions
- allowed actions policy
- CODEOWNERS for workflow files
- environment protection rules
- required OIDC over static cloud secrets
- disallow self-hosted runners for public fork PRs
- approved runner groups
- artifact retention defaults
- required security scans for release workflows

Strong answer:

```text
I prefer guardrails that make the secure path easy and the dangerous path explicit, reviewed, and auditable.
```

---

## 12. Support Model

Platform support should define:

- escalation channel
- workflow owner responsibility
- common runbooks
- office hours
- incident severity levels
- templates for bug reports
- self-service docs
- migration support
- known issue dashboard

Support request template:

```text
repo:
workflow:
run url:
event:
runner label:
failure step:
first failed time:
recent workflow change:
blocking merge/deploy: yes/no
```

---

## 13. Incident Response

Common platform incidents:

- required checks unavailable
- runner fleet unavailable
- self-hosted runner compromised
- broad workflow secret leaked
- OIDC role assumption failing
- artifact storage outage
- GitHub Actions service degradation
- deploy workflow bug affects many repos
- reusable workflow breaking change

Incident response flow:

1. Scope affected repos/workflows.
2. Identify if merge/deploy is blocked.
3. Check GitHub status and runner fleet health.
4. Apply workaround or rollback reusable workflow version.
5. Communicate impact and ETA.
6. Preserve audit logs and run URLs.
7. Add prevention action.

---

## 14. Migration Strategy

Migrating many repos to standard workflows:

1. Inventory current workflows.
2. Identify high-risk patterns.
3. Define target golden paths.
4. Pilot with a few teams.
5. Add migration guide and examples.
6. Automate PRs where safe.
7. Track adoption metrics.
8. Deprecate old templates gradually.
9. Enforce policy after adoption runway.

Caution:

```text
Do not flip strict org-wide policy before teams have a working replacement path.
```

---

## 15. Flaky Workflow Program

Flakiness harms trust in CI.

Track:

- rerun success rate
- flaky test files
- flaky infrastructure steps
- dependency download failures
- timeout patterns
- service container health failures
- runner image regressions

Fixes:

- isolate tests
- improve test data
- add service readiness checks
- reduce network dependency
- pin unstable tool versions
- quarantine flaky tests temporarily with owner/SLA
- measure flake burn-down

---

## 16. Platform Maturity Levels

| Level | Behavior |
|---|---|
| 1 | teams hand-write YAML independently |
| 2 | some shared examples/templates |
| 3 | reusable workflows with secure defaults |
| 4 | metrics, ownership, support, and cost reporting |
| 5 | policy-as-guardrails, golden paths, SLOs, incident process, continuous improvement |

MAANG-ready answer should describe level 4-5 practices.

---

## 17. Common Mistakes

| Mistake | Better Approach |
|---|---|
| no workflow ownership | owner metadata and support channel |
| only track pass/fail | track duration, queue, flake, cost, deploy health |
| optimize cache before fixing wasted jobs | cancel stale runs and narrow triggers first |
| one runner label for everything | labels by capability and trust level |
| shared workflow breaking all repos | versioned reusable workflows and rollback plan |
| no artifact retention policy | retention by artifact type and compliance need |
| strict policy before migration path | golden path first, enforcement later |
| platform support only through DMs | run URLs, templates, support channel, runbooks |

---

## 18. Scenario

Prompt:

```text
A company has 300 repos. PR checks are slow, runner queues spike daily, CI costs keep rising, and every team uses different workflow patterns. What do you do?
```

Strong answer:

```text
I would first build visibility: workflow inventory, owners, duration, queue time, failure/flake rate, runner labels, and cost by team. Then I would attack waste with concurrency cancellation, path filters, matrix tuning, cache hygiene, and scheduled workflow shaping. In parallel I would create versioned reusable workflow golden paths with least-privilege permissions and standard artifact/cache patterns. Runner capacity would be split by trust and workload, with autoscaling for overloaded labels. Finally I would add support runbooks, adoption metrics, and phased policy enforcement so teams migrate without being blocked.
```

---

## 19. Revision Notes

- Actions at scale is a developer platform.
- Track workflow ownership, SLOs, queue time, flake rate, and cost.
- Golden paths reduce copy-paste YAML and insecure drift.
- Runner labels and groups should reflect capability and trust.
- Required checks need reliability, not just enforcement.
- Cost optimization starts with wasted work and flaky reruns.
- Migrations need a target path before strict enforcement.
- Platform support requires runbooks, run URLs, owners, and communication.

---

## 20. Official Source Notes

- GitHub Actions usage limits and billing: https://docs.github.com/en/billing/managing-billing-for-github-actions
- GitHub Actions monitoring and troubleshooting: https://docs.github.com/en/actions/monitoring-and-troubleshooting-workflows
- GitHub-hosted runners: https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners
- Self-hosted runners: https://docs.github.com/en/actions/hosting-your-own-runners
- Reusable workflows: https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows
