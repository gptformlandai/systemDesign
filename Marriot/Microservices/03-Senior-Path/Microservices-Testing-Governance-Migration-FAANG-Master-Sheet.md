# Microservices Testing Governance Migration FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- microservice testing pyramid
- unit, integration, component, contract, E2E tests
- consumer-driven contracts
- test data strategy
- CI/CD gates
- schema compatibility checks
- migration from monolith
- strangler fig
- anti-corruption layer
- service ownership
- governance without blocking teams

Goal:

```text
After reading this sheet, you should be able to explain how to test independently deployed
services, prevent breaking changes, migrate from a monolith safely, and govern microservices
without destroying team autonomy.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | unit/integration/E2E testing |
| Intermediate | contract testing, test pyramid, CI gates |
| Senior | migration strategy, schema compatibility, ownership |
| FAANG-ready | governance, platform standards, safe evolution at scale |

Must-say line:

```text
Microservices need fewer brittle end-to-end tests and more focused contract, component,
and integration tests that support independent deployments.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Testing pyramid | Very high | Quality strategy |
| Contract testing | Very high | Independent deploy safety |
| Component tests | High | Service confidence |
| Integration tests | High | Dependency behavior |
| E2E tests | High | Critical path validation |
| Test data | Medium-high | Reliability |
| CI/CD gates | High | Safe rollout |
| Schema compatibility | High | Event/API safety |
| Strangler fig | High | Monolith migration |
| Anti-corruption layer | High | Legacy isolation |
| Ownership | Very high | Team boundaries |
| Governance | High | Scale without chaos |

---

# 2. Microservice Testing Pyramid

```text
          few E2E tests
       contract/component tests
    integration tests
unit tests
          many
```

Goal:
- many fast unit tests
- focused service/component tests
- contract tests between services
- limited E2E for critical flows

Strong answer:

```text
I avoid relying only on end-to-end tests because they are slow and flaky. Contract and
component tests give faster confidence for independent deployments.
```

---

# 3. Unit Tests

Use for:
- business rules
- validation
- mappers
- pure domain logic
- state transitions

Example:

```text
Booking cannot move from CANCELLED to CONFIRMED.
```

Strong answer:

```text
Unit tests should cover domain logic without needing network, database, or broker.
```

---

# 4. Integration Tests

Integration tests verify service with real dependencies or realistic substitutes.

Examples:
- database repository with PostgreSQL container
- Kafka producer/consumer with broker container
- Redis cache behavior
- HTTP client against fake server

Strong answer:

```text
Integration tests should verify real integration points where mocks would hide important
behavior, such as SQL dialect, broker serialization, or HTTP error handling.
```

---

# 5. Component Tests

Component test runs one service as a unit.

Example:

```text
Booking Service running with real DB container and mocked external Payment/Inventory APIs.
```

Verifies:
- HTTP API
- service logic
- database
- migrations
- serialization
- security filters

Strong answer:

```text
Component tests give confidence in one service without needing the entire microservice
environment.
```

---

# 6. Contract Testing

Contract testing verifies provider and consumer agree.

Consumer-driven flow:

```text
Consumer records expected request/response.
Provider verifies it satisfies contract.
CI blocks breaking provider changes.
```

Use for:
- REST APIs
- gRPC contracts
- event schemas
- message payload compatibility

Strong answer:

```text
Contract tests are essential because microservices deploy independently. They catch breaking
changes before runtime.
```

---

# 7. E2E Tests

End-to-end tests validate full business flows.

Examples:
- create booking
- authorize payment
- confirm booking
- send notification

Use sparingly because:
- slow
- flaky
- hard to debug
- many services involved
- environment setup costly

Strong answer:

```text
I keep E2E tests for the most critical user journeys and rely on lower-level tests for most
coverage.
```

---

# 8. Test Data Strategy

Problems:
- shared test data creates flaky tests
- tests depend on execution order
- test services mutate same records
- old events pollute topics

Strategies:
- isolated test tenants
- generated unique IDs
- database cleanup
- ephemeral environments
- containerized dependencies
- contract fixtures
- deterministic clocks

Senior line:

```text
Flaky test data is an architecture smell in CI.
```

---

# 9. CI/CD Quality Gates

Common gates:
- compile
- unit tests
- static analysis
- dependency vulnerability scan
- API contract verification
- schema compatibility check
- integration tests
- migration validation
- container build
- deployment smoke test
- canary analysis

Strong answer:

```text
The CI pipeline should catch breaking contracts, schema changes, and migration failures
before deployment.
```

---

# 10. API Compatibility Gate

Check:
- OpenAPI diff
- removed fields
- changed required fields
- changed status codes
- removed endpoints
- changed enum behavior

Allowed:
- new optional fields
- new endpoint
- additive enum only if consumers tolerate unknowns

---

# 11. Event Schema Compatibility Gate

Check:
- old consumers can read new events
- new consumers can read old events if replay needed
- required fields not removed
- enum changes safe
- schema registry compatibility mode

Strong answer:

```text
Event schema checks are important because events may be replayed long after the producer
code changed.
```

---

# 12. Deployment Verification

After deploy:
- health check passes
- smoke tests pass
- error rate stable
- latency stable
- no DLQ spike
- consumer lag stable
- dependency errors stable
- canary compares well against baseline

Rollback if:
- error budget burn too high
- canary worse than baseline
- critical business metric drops
- security issue found

---

# 13. Migration From Monolith

Do not split everything at once.

Steps:
1. Identify business capability.
2. Create clear module boundary in monolith.
3. Extract API boundary.
4. Move data ownership carefully.
5. Add anti-corruption layer.
6. Route traffic gradually.
7. Retire old path.

Strong answer:

```text
I prefer modularizing first, then extracting services where independent deployment or scaling
justifies the complexity.
```

---

# 14. Strangler Fig Pattern

Strangler pattern gradually replaces legacy functionality.

Flow:

```text
route old traffic to monolith
route one capability to new service
validate behavior
increase traffic
remove old code
```

Use for:
- monolith migration
- legacy rewrite
- risky domain extraction

Strong answer:

```text
Strangler fig reduces migration risk by replacing one capability at a time instead of doing
a big-bang rewrite.
```

---

# 15. Anti-Corruption Layer

ACL protects new model from legacy model.

Example:

```text
Legacy reservation status "X9" maps to new BookingStatus.CANCELLED_BY_SYSTEM
```

Benefits:
- isolates weird legacy concepts
- avoids leaking old schema
- supports gradual migration
- keeps new domain clean

Strong answer:

```text
Anti-corruption layer translates between old and new models so the new service does not
inherit legacy design problems.
```

---

# 16. Data Migration Strategy

Patterns:
- big bang, risky
- dual write, risky unless controlled
- outbox/CDC replication
- backfill then switch
- read old/write new
- write both/read new
- expand-contract

Safe migration flow:

```text
create new schema
backfill old data
dual-read compare
route small traffic
switch writes
monitor
retire old path
```

---

# 17. Ownership Model

Each service needs:
- owning team
- clear API contract
- data ownership
- on-call ownership
- SLO ownership
- runbook
- dashboard
- security owner
- dependency map

Strong answer:

```text
Microservices without ownership become operational debt. Every service needs a team that
owns code, data, SLOs, incidents, and contracts.
```

---

# 18. Governance Without Killing Autonomy

Governance should standardize:
- observability fields
- auth patterns
- deployment checks
- API compatibility
- incident process
- secrets management
- logging standards
- platform libraries

Governance should not:
- require central approval for every small code change
- force one data store for every service
- block team ownership
- create architecture theater

Strong answer:

```text
Good governance provides paved roads and safety checks while preserving team autonomy.
```

---

# 19. Service Catalog

Service catalog tracks:
- owner
- repo
- API docs
- dashboard
- runbook
- SLO
- dependencies
- on-call rotation
- data classification
- lifecycle status

Why:

```text
At scale, teams need to discover who owns what and what depends on what.
```

---

# 20. Dependency Management

Problems:
- hidden dependencies
- circular service calls
- synchronous call chains
- old API versions
- owner unknown

Controls:
- dependency map
- contract tests
- deprecation policy
- API version lifecycle
- dashboard per dependency
- architecture review for new sync dependency

---

# 21. Deprecation Policy

Good deprecation process:
1. Announce old API/event deprecation.
2. Identify consumers.
3. Provide migration guide.
4. Monitor usage.
5. Set removal date.
6. Block new usage.
7. Remove only after consumers migrate.

Strong answer:

```text
Microservices need deprecation discipline because unknown consumers can break when old APIs
or event fields disappear.
```

---

# 22. Production Scenario: Extract Payment From Monolith

Plan:
1. Identify payment boundary in monolith.
2. Define Payment Service API.
3. Add ACL between monolith and payment provider model.
4. Create Payment Service database.
5. Backfill payment data.
6. Route small traffic through new service.
7. Use contract tests with Booking Service.
8. Monitor authorization success, latency, errors.
9. Gradually shift traffic.
10. Retire old monolith payment module.

Strong answer:

```text
I would not rewrite payment in one big cutover. I would use strangler fig, contract tests,
backfill, gradual traffic routing, and clear rollback points.
```

---

# 23. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| Only E2E tests | slow/flaky | test pyramid |
| No contract tests | breaking changes | consumer-driven contracts |
| Shared test environment only | flaky | isolated/env-per-PR where possible |
| Big-bang rewrite | high risk | strangler fig |
| Leak legacy model | new system polluted | anti-corruption layer |
| No service owner | incident confusion | ownership registry |
| No deprecation policy | surprise breakage | lifecycle management |
| Governance by meetings only | slow and inconsistent | automated checks |
| Unknown dependencies | outage surprises | service catalog |

---

# 24. Hot Interview Questions

### Q1. How do you test microservices?

```text
Use a pyramid: unit tests, integration/component tests, contract tests, and a small number
of critical E2E tests.
```

### Q2. Why contract testing?

```text
It protects independent deployments by verifying provider and consumer expectations.
```

### Q3. How do you migrate from monolith?

```text
Use strangler fig: extract one business capability at a time, route traffic gradually, and
retire old code safely.
```

### Q4. What is anti-corruption layer?

```text
A translation layer that prevents legacy model concepts from leaking into the new service.
```

### Q5. What is good governance?

```text
Automated safety checks, standards, and platform support that preserve team autonomy.
```

---

# 25. Final Rapid Revision

| Need | Concept |
|---|---|
| Fast logic validation | unit tests |
| Real dependency behavior | integration tests |
| One service confidence | component tests |
| Provider/consumer safety | contract tests |
| Critical full journey | E2E tests |
| Safe monolith migration | strangler fig |
| Legacy translation | anti-corruption layer |
| API removal safety | deprecation policy |
| Ownership clarity | service catalog |
| Scaled standards | governance |

---

# 26. Strong Closing Answer

If interviewer asks:

```text
How do you test and govern microservices?
```

Say:

```text
I use a microservice testing pyramid: many unit tests, focused integration and component
tests, consumer-driven contract tests for service boundaries, and a small number of critical
E2E tests. CI should verify contracts, schema compatibility, migrations, and security checks.
For migration, I prefer strangler fig with anti-corruption layers and gradual traffic shifting.
At scale, every service needs clear ownership, SLOs, runbooks, dashboards, and a service
catalog.
```

---

# 27. Official Source Notes

Useful references:

- Martin Fowler on Microservice Testing: https://martinfowler.com/articles/microservice-testing/
- Pact Documentation: https://docs.pact.io/
- Spring Cloud Contract: https://docs.spring.io/spring-cloud-contract/reference/
- Strangler Fig Application: https://martinfowler.com/bliki/StranglerFigApplication.html
- CNCF TAG App Delivery: https://tag-app-delivery.cncf.io/

