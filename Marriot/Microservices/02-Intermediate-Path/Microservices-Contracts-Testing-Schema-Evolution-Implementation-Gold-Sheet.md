# Microservices Contracts Testing Schema Evolution Implementation Gold Sheet

> Track: Microservices Interview Track - Group 2 Intermediate Path  
> Goal: make independent deployment safe by designing compatible APIs, event schemas, and tests.

Read after communication/API contracts and testing/governance sheets.

---

## 1. Why This Matters

Microservices fail when teams deploy independently but contracts evolve unsafely.

Common production break:

```text
Provider removes or renames a field.
Consumer deploys later or not at all.
Runtime traffic starts failing even though provider tests passed.
```

Strong answer:

```text
Independent deployment requires backward-compatible contracts, consumer-driven tests,
schema compatibility checks, and rollout discipline.
```

---

## 2. Contract Types

| Contract | Example | Risk |
|---|---|---|
| HTTP API contract | OpenAPI spec | breaking response/request shape |
| gRPC contract | protobuf service definition | incompatible field changes |
| Event contract | JSON/Avro/Protobuf schema | consumer deserialization failure |
| Semantic contract | meaning of status or field | consumers behave incorrectly |
| Operational contract | SLO, timeout, rate limit | hidden runtime dependency risk |

Senior point:

```text
A schema can be compatible while the meaning changes in a breaking way. Semantic contracts
must be documented and tested with scenarios.
```

---

## 3. Backward Compatibility Rules For REST

Safe changes:

- add optional response field
- add new endpoint
- add optional request field with default behavior
- support new enum value only if consumers tolerate unknowns
- add pagination metadata without removing old fields

Breaking changes:

- remove response field
- rename field
- change type from string to number
- make optional request field required
- change error code meaning
- change default sort/order unexpectedly

Interview answer:

```text
For public or cross-team APIs, I prefer additive changes, deprecation windows, contract tests,
and versioning only when compatibility cannot be preserved.
```

---

## 4. REST Versioning Decision

| Situation | Better Choice |
|---|---|
| Add optional field | no new version |
| Add new endpoint behavior | new endpoint or optional parameter |
| Major incompatible model change | new version |
| One consumer needs custom shape | BFF or consumer-specific endpoint |
| Internal temporary migration | compatibility period + cleanup plan |

Versioning styles:

| Style | Example | Notes |
|---|---|---|
| URI | `/v2/bookings/{id}` | simple, visible |
| Header | `Accept: application/vnd.company.v2+json` | cleaner URI, more tooling needed |
| Field-level | optional fields | best for additive changes |

Trap:

```text
Versioning does not remove migration work. It creates multiple contracts to support.
```

---

## 5. Consumer-Driven Contract Testing

Consumer-driven contract testing records what a consumer expects.

Flow:

```text
1. Consumer defines expected request/response interaction.
2. Provider verifies it can satisfy those interactions.
3. CI blocks provider changes that break active consumers.
```

Typical tool:

```text
Pact
```

Good for:

- provider/consumer HTTP APIs
- independent deploy safety
- preventing accidental response shape breaks

Not enough for:

- full business workflows
- performance
- authorization edge cases
- database behavior

Strong answer:

```text
Contract tests do not replace integration tests. They protect API compatibility between
services so each service can deploy independently with confidence.
```

---

## 6. Component Tests

A component test runs one service with realistic local dependencies.

Example:

```text
Booking Service
  + real PostgreSQL container
  + real migrations
  + fake Payment server
  + fake Availability server
  + test Kafka or embedded broker if needed
```

Useful tools:

- Testcontainers for databases, Kafka, Redis
- WireMock for HTTP dependencies
- Pact for provider/consumer contracts
- LocalStack for cloud dependency behavior where useful

Strong answer:

```text
I use component tests to verify one service end to end without needing the entire distributed
system. It catches wiring, serialization, migrations, and security filter issues that unit
tests miss.
```

---

## 7. Event Schema Evolution

For events, prefer explicit schemas.

Common options:

- Avro with Schema Registry
- Protobuf
- JSON Schema
- versioned JSON with compatibility tests

Safe event changes:

- add optional field with default
- add new event type
- keep old fields until consumers migrate
- preserve event meaning

Dangerous changes:

- remove field consumers use
- change field type
- change enum semantics
- reuse event name for a different fact
- send command-like events that ask consumers to do hidden work

Strong answer:

```text
Events are long-lived integration contracts. I treat them as published facts, version them
carefully, and check compatibility before deployment.
```

---

## 8. Protobuf Evolution Rules

Safe practices:

- never reuse field numbers
- reserve removed field numbers and names
- add new optional fields
- use stable semantic names
- avoid changing field type
- tolerate unknown fields

Example:

```proto
message BookingConfirmed {
  string booking_id = 1;
  string user_id = 2;
  string hotel_id = 3;
  string confirmed_at = 4;

  reserved 5;
  reserved "legacy_status";
}
```

Strong answer:

```text
With Protobuf, field numbers are the wire contract. I do not reuse numbers after removal
because old serialized data or clients can interpret the field incorrectly.
```

---

## 9. Avro And Schema Registry Basics

Schema Registry stores schemas and enforces compatibility.

Compatibility modes:

| Mode | Meaning |
|---|---|
| backward | new reader can read old data |
| forward | old reader can read new data |
| full | both backward and forward |
| none | no compatibility enforcement |

Interview answer:

```text
For event-driven systems, Schema Registry helps prevent producers from publishing schemas
that active consumers cannot read.
```

---

## 10. CDC And Debezium

CDC captures database changes and publishes them as events.

Tool example:

```text
Debezium reads database transaction logs and emits change events.
```

Good for:

- migration pipelines
- read model updates
- analytics feeds
- outbox event relay from DB log

Risks:

- leaking internal table schema as public contract
- noisy low-level events instead of domain events
- ordering and replay considerations
- operational dependency on connector health

Strong answer:

```text
CDC is powerful, but I avoid exposing raw table changes as domain contracts. For business
integration, I prefer intentional domain events or an outbox table consumed by CDC.
```

---

## 11. API Compatibility CI Gate

Provider pipeline should check:

1. unit tests
2. component tests
3. OpenAPI compatibility against previous version
4. provider verification for active Pact contracts
5. security tests for auth/authorization behavior
6. smoke tests after deploy
7. canary metrics before full rollout

Strong answer:

```text
I want breaking contract changes to fail in CI, not after independent consumers receive
runtime traffic.
```

---

## 12. Event Compatibility CI Gate

Producer pipeline should check:

1. schema is valid
2. schema is compatible with registry policy
3. required fields have defaults or migration plan
4. event names stay semantic and past tense
5. sample payloads deserialize in consumer tests
6. poison-message behavior is tested

Example event review questions:

- Is this a fact that already happened?
- Who consumes it today?
- Who might consume it later?
- Is the event too low-level?
- Can consumers ignore unknown fields?
- What is the replay behavior?

---

## 13. Test Data Strategy

Microservices tests need stable test data.

Approaches:

| Approach | Use When |
|---|---|
| test builders | unit/component tests |
| seeded containers | DB integration tests |
| fake provider fixtures | component tests |
| contract examples | API provider verification |
| synthetic E2E users | critical flow smoke tests |

Avoid:

- tests depending on shared mutable staging data
- test order dependencies
- production-like data without masking
- one giant test environment as the only confidence source

---

## 14. E2E Test Scope

Use few E2E tests for critical flows:

- create booking
- cancel booking
- payment failure path
- login/auth flow
- search-to-booking smoke path

Do not test every edge case through E2E.

Strong answer:

```text
E2E tests are useful for confidence in critical paths but they are slow and flaky as the
main strategy. Most confidence should come from unit, component, integration, and contract
tests.
```

---

## 15. Breaking Change Migration Plan

When breaking change is unavoidable:

```text
1. Add new contract without removing old one.
2. Deploy provider supporting both.
3. Move consumers one by one.
4. Monitor usage of old contract.
5. Announce deprecation deadline.
6. Remove old contract only after usage is zero or explicitly accepted.
```

Strong answer:

```text
I do not rely on coordinated big-bang deployment. I design expand-and-contract migrations
so old and new consumers can coexist safely.
```

---

## 16. Common Interview Traps

| Trap | Better Answer |
|---|---|
| "Just version every API" | prefer compatibility; version when necessary |
| "Contract tests replace integration tests" | they cover compatibility, not behavior/performance |
| "Events are internal" | consumers make events external contracts |
| "JSON is flexible, so safe" | flexible schemas still break semantics |
| "E2E proves everything" | E2E is slow, flaky, and poor at fault isolation |
| "Schema compatibility handles all changes" | semantic compatibility also matters |

---

## 17. Strong Closing Answer

```text
For independent microservice deployments, I make contracts explicit and compatible. REST and
gRPC APIs need additive changes, deprecation windows, and consumer-driven contract tests.
Events need schema compatibility, stable semantics, and replay-safe consumers. CI should
block incompatible changes before traffic reaches production.
```
