# GitHub Actions Backend CI and Testing Gold Sheet

> Goal: design backend CI pipelines that are fast, reliable, secure, and production-relevant for Java/Spring Boot, Node, Python, Go, and .NET services.

---

## 0. How To Read This

Beginner focus:

- checkout code
- setup language
- install dependencies
- run tests
- upload reports

Intermediate focus:

- service containers
- integration tests
- coverage
- test artifacts
- matrix builds
- dependency caching

Senior focus:

- contract tests
- monorepo services
- flaky test isolation
- quality gates
- CI speed/cost
- production-like test dependencies

---

# Topic 1: Backend CI and Testing

---

## 1. Intuition

Backend CI is a safety gate before code joins the main branch.

It asks:

```text
Does it compile?
Do unit tests pass?
Do integration tests pass?
Does it work with real dependencies?
Is coverage acceptable?
Did we break contracts?
Can we produce a deployable artifact?
```

Beginner explanation:

A backend CI workflow automatically builds and tests backend code on every pull request or push so broken code does not reach main or production.

---

## 2. Definition

- Definition: Backend CI is an automated workflow that validates backend code through compile/build, tests, dependency checks, reports, and artifacts.
- Category: Continuous Integration
- Core idea: give fast, trustworthy feedback before merge.

---

## 3. Why It Exists

Without backend CI:

- broken code reaches main
- tests are skipped locally
- integration problems are caught late
- deployment artifacts are inconsistent
- code quality depends on memory
- releases become risky

CI makes quality repeatable.

---

## 4. Reality

Backend CI commonly includes:

- Java/Spring Boot tests
- Maven/Gradle cache
- Node API tests
- Python pytest
- Go tests
- .NET tests
- Postgres/Redis/Kafka service containers
- coverage reports
- dependency scanning
- Docker image build
- test report upload

Senior expectation:

CI should be fast enough for developers and strong enough to protect production.

---

## 5. How It Works

### Part A: CI Flow

```text
pull_request opened
-> checkout code
-> setup runtime
-> restore dependency cache
-> install dependencies
-> compile/build
-> run unit tests
-> run integration/contract tests
-> upload reports/artifacts
-> mark PR pass/fail
```

### Part B: Java / Spring Boot CI

```yaml
name: Java Backend CI

on:
  pull_request:
    paths:
      - "backend/**"
      - ".github/workflows/backend-ci.yml"

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: app
          POSTGRES_PASSWORD: app
          POSTGRES_DB: app_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - run: ./mvnw -B verify
        working-directory: backend
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: java-test-reports
          path: backend/target/surefire-reports/
```

### Part C: Gradle CI

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: "21"
    cache: gradle

- run: ./gradlew test
```

### Part D: Node Backend CI

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: "22"
    cache: npm
    cache-dependency-path: package-lock.json

- run: npm ci
- run: npm test
```

### Part E: Python Backend CI

```yaml
- uses: actions/setup-python@v5
  with:
    python-version: "3.12"
    cache: pip

- run: pip install -r requirements.txt
- run: pytest --junitxml=reports/pytest.xml
```

### Part F: Go Backend CI

```yaml
- uses: actions/setup-go@v5
  with:
    go-version: "1.22"
    cache: true

- run: go test ./...
```

### Part G: .NET Backend CI

```yaml
- uses: actions/setup-dotnet@v4
  with:
    dotnet-version: "8.0.x"

- run: dotnet restore
- run: dotnet test --logger trx
```

### Part H: Service Containers

Use service containers when tests need real dependencies:

- Postgres
- MySQL
- Redis
- Kafka
- LocalStack
- Elasticsearch

Good use:

```text
integration tests validate repository layer against real Postgres
```

Bad use:

```text
every tiny unit test waits for a full environment
```

### Part I: Unit vs Integration vs Contract Tests

| Test Type | Purpose | CI Placement |
|---|---|---|
| Unit | fast logic validation | every PR |
| Integration | validate real dependencies | every PR or merge depending cost |
| Contract | producer/consumer API/event compatibility | every PR for service boundaries |
| E2E | validate full user flow | scheduled, pre-release, or critical PRs |

### Part J: Test Reports and Coverage

Always upload useful debugging outputs:

- JUnit XML
- Surefire reports
- coverage report
- failed test logs
- screenshots/traces for E2E

Use coverage as a signal, not a religion.

Senior answer:

> I care more about meaningful coverage of risky behavior than chasing a vanity percentage.

### Part K: Flaky Tests

Flaky tests are production-risk signals.

Bad practice:

- rerun forever
- ignore failed tests
- mark everything optional

Better practice:

- quarantine with owner
- track flake rate
- fix root cause
- separate unstable external dependencies
- use deterministic test data

---

## 6. What Problem It Solves

- Primary problem solved: prevent broken backend code from merging
- Secondary benefits: faster feedback, artifact consistency, regression detection
- Systems impact: improves release confidence and developer velocity

---

## 7. When To Rely On It

Use backend CI for:

- every pull request
- main branch protection
- pre-deployment validation
- dependency updates
- service contract changes
- database migration checks

Interviewer keywords:

- PR checks
- quality gate
- integration test
- service container
- contract test
- flaky tests
- backend deployment

---

## 8. When Not To Overload CI

Avoid putting every possible test in PR CI when:

- feedback becomes too slow
- tests depend on unstable external systems
- full E2E suite takes too long
- cost is excessive

Use layers:

```text
PR: fast lint/unit/build/smoke
main: integration/contract/security
nightly: heavy E2E/performance
pre-prod: deployment validation
```

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects main branch | Can become slow |
| Catches regressions early | Flaky tests reduce trust |
| Produces consistent artifacts | Service containers add setup time |
| Supports branch protection | Poor cache strategy wastes minutes |
| Documents build process | YAML duplication can drift |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More tests:
  More confidence, slower feedback.
- Real dependencies:
  Better realism, more setup and flakiness risk.
- Matrix builds:
  More compatibility coverage, more cost.
- Required checks:
  Stronger quality gate, but broken CI blocks all developers.

### Common Mistakes

- Mistake: "CI only runs unit tests."
  Why it is wrong: backend issues often happen at integration boundaries.
  Better approach: add targeted integration and contract tests.

- Mistake: "CI calls shared staging DB."
  Why it is wrong: tests become flaky and interfere with each other.
  Better approach: use isolated service containers or ephemeral environments.

- Mistake: "Upload no reports."
  Why it is wrong: failures become hard to debug.
  Better approach: upload test reports and logs on failure.

- Mistake: "Rerun flaky tests until green."
  Why it is wrong: it hides quality problems.
  Better approach: track and fix flakes.

---

## 11. Key Numbers

Useful targets:

- PR CI common target: under 10 minutes for normal changes
- Unit tests: fastest layer, usually every PR
- Integration tests: targeted and parallelized where possible
- Heavy E2E/performance: scheduled or pre-release
- Cache hit rate: should be high for common branches

Exact values depend on repo size and business risk.

---

## 12. Failure Modes

### Service Container Not Ready

Cause:

- tests start before DB/Redis/Kafka is healthy

Fix:

- health checks
- retry connection logic
- explicit wait step when needed

### Slow CI

Cause:

- no cache
- huge matrix
- too many integration tests
- serial jobs

Fix:

- cache dependencies
- split jobs
- path filters
- affected builds
- parallel test execution

### Flaky Integration Test

Cause:

- shared data
- race condition
- external dependency
- timing assumption

Fix:

- isolate state
- deterministic data
- service containers
- remove sleeps where possible

### Test Report Missing

Cause:

- artifact upload only runs on success

Fix:

```yaml
if: always()
```

---

## 13. Scenario

- Product / system: Spring Boot order service
- Why this concept fits: PRs must validate service logic, database queries, and API contracts
- What would go wrong without it: broken migrations or repository logic could reach main and fail deployment

---

## 14. Code Sample

Java CI with Postgres and reports:

```yaml
name: Order Service CI

on:
  pull_request:
    paths:
      - "services/order/**"

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: order
          POSTGRES_PASSWORD: order
          POSTGRES_DB: order_test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"
          cache: maven

      - run: ./mvnw -B verify
        working-directory: services/order

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: order-service-test-reports
          path: services/order/target/surefire-reports/
```

---

## 15. Mini Program / Simulation

CI test layering:

```python
tests = [
    ("unit", 2, "every PR"),
    ("integration", 8, "every PR for touched service"),
    ("contract", 5, "every PR for API/event changes"),
    ("e2e", 45, "nightly or pre-release"),
]

for name, minutes, when in tests:
    print(f"{name}: {minutes} min -> {when}")
```

---

## 16. Practical Question

> Design a GitHub Actions CI pipeline for a Spring Boot service that uses Postgres and publishes events.

---

## 17. Strong Answer

I would run fast PR checks on every pull request: compile, unit tests, formatting/lint if used, and targeted integration tests. For Postgres, I would use a service container with a health check instead of a shared database. For event contracts, I would add contract tests or schema compatibility checks.

I would cache Maven/Gradle dependencies, upload JUnit reports with `if: always()`, and make the workflow required in branch protection. Heavy E2E or performance tests would run nightly or before release so normal PR feedback stays fast.

The workflow would run with minimal `contents: read` permission and no production secrets.

---

## 18. Revision Notes

- One-line summary: Backend CI validates code, dependencies, contracts, and artifacts before merge.
- Three keywords: tests, service containers, reports
- One interview trap: PR CI should not depend on shared mutable staging databases.
- One memory trick: unit is speed, integration is realism, contract is boundary safety.

---

## 19. Official Source Notes

- Service containers: <https://docs.github.com/en/actions/use-cases-and-examples/using-containerized-services/about-service-containers>
- Dependency caching: <https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows>
- Artifact storage: <https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts>

