# 36. Datadog CI Visibility, Test Optimization, DORA, Software Delivery

## Goal

Understand how Datadog connects build pipelines, tests, deployments, and production telemetry into one software delivery view.

---

## Mental Model

Observability usually starts after deploy.

CI Visibility starts before deploy.

```text
code commit -> CI pipeline -> tests -> artifact -> deployment -> production health
```

The goal is to trace software delivery from commit to customer impact.

---

## Why It Exists

Engineering teams need to know:

- Which pipeline is slow?
- Which test is flaky?
- Which commit broke the build?
- Did the latest deploy increase errors?
- Which services deploy frequently but safely?
- Which teams have long lead time or high change failure rate?

CI/CD data becomes more useful when correlated with service/env/version telemetry.

---

## CI Visibility Signals

| Signal | Example |
|---|---|
| Pipeline duration | build took 18 minutes |
| Job duration | integration tests took 11 minutes |
| Test duration | `PaymentServiceTest` took 92 seconds |
| Test failures | flaky checkout test failed 7 times this week |
| Queue time | runner wait time increased |
| Commit metadata | author, branch, SHA |
| Deployment markers | version 2.4.1 deployed to prod |

---

## Delivery Data Flow

```text
CI provider
  -> Datadog CI Visibility integration / datadog-ci
  -> pipeline and test spans
  -> Datadog CI Explorer
  -> Deployment events tagged with service/env/version
  -> APM/RUM/SLO correlation after deploy
```

---

## Required Tag Discipline

The same version identity must connect CI and runtime:

```text
git.commit.sha:abc123
service:orders-service
env:production
version:2.4.1
team:checkout
repository:orders-service
branch:main
```

Without consistent versioning, deploy correlation becomes guesswork.

---

## DORA Metrics

| Metric | Meaning | Datadog Data |
|---|---|---|
| Deployment frequency | How often production changes ship | deployment events |
| Lead time for changes | Commit to production time | commit + deploy metadata |
| Change failure rate | Deploys causing incidents/rollback | deploy + monitor/incident data |
| MTTR | Time to recover after failure | incident and monitor resolution |

DORA metrics are not vanity metrics. They reveal delivery reliability.

---

## Test Optimization Concepts

| Concept | Why It Matters |
|---|---|
| Flaky test detection | Prevents random pipeline failures |
| Slow test identification | Reduces build time |
| Test impact analysis | Runs only tests affected by code changes |
| Failure clustering | Groups repeated failures by cause |
| Quarantine policy | Prevents known flaky tests from blocking all work forever |

---

## Monitor Examples

### Pipeline Duration Regression

```text
Alert:
  p95 pipeline duration for repo:orders-service > 20 minutes
  for 3 consecutive runs
```

### Flaky Test Spike

```text
Alert:
  flaky test count by repository > 10 in 24 hours
```

### Production Deploy Regression

```text
Alert:
  deployment version:2.4.1
  AND service error rate increased 3x within 30 minutes
```

---

## GitHub Actions Example

```yaml
name: build

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    env:
      DD_SERVICE: orders-service
      DD_ENV: ci
      DD_TAGS: team:checkout,repository:orders-service
    steps:
      - uses: actions/checkout@v4
      - name: Install Datadog CI CLI
        run: npm install -g @datadog/datadog-ci
      - name: Run tests with CI Visibility
        run: datadog-ci junit upload ./test-results/*.xml
```

Exact setup varies by language, test framework, and CI provider.

---

## Deployment Tracking Pattern

```text
1. CI builds artifact with git SHA.
2. Release process assigns version.
3. Deployment event is sent to Datadog.
4. Runtime services emit DD_VERSION.
5. Dashboards and APM show version overlay.
6. Error Tracking identifies issues first seen after version.
7. Incident response can roll back exact version.
```

---

## Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| CI data not tagged with service/team | Cannot attribute failures | Add service/team/repo tags |
| Runtime version missing | Cannot connect deploy to errors | Set `DD_VERSION` |
| Measuring only build success | Misses slow/flaky pipeline pain | Track duration, queue, flakes |
| DORA used to punish teams | Creates gaming | Use for system improvement |
| No deploy markers | Incident timeline unclear | Emit deployment events |

---

## Practical Question

> A team says deployments are risky and slow, but nobody has data. How would you use Datadog to improve software delivery?

---

## Strong Answer

I would enable CI Visibility for the team's repositories and ensure pipeline, job, and test data are tagged by service, team, repository, branch, and commit SHA. Then I would publish dashboards for pipeline duration, flaky tests, failed jobs, and queue time.

For production, I would send deployment events and set `DD_VERSION` in runtime services so APM, Error Tracking, SLOs, and incidents can correlate failures to deploys. Then I would track DORA metrics: deployment frequency, lead time, change failure rate, and MTTR. The goal is not to blame teams; it is to identify bottlenecks like slow tests, unreliable environments, risky deploy batches, and weak rollback practices.

---

## Interview Sound Bite

CI Visibility connects commit, pipeline, test, deployment, and production health. The mature pattern is: tag builds and services consistently, emit deploy markers, then correlate new errors, SLO burn, and incidents to the exact version that shipped.
