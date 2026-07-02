# Microservices Local Dev, Docker Compose, And First System Gold Sheet

> Track: Microservices Interview Track - Setup Layer  
> Goal: take a beginner from "I know the pattern names" to running and debugging a small local microservice system.

---

## 1. Intuition

Microservices are not only code split into folders. They are several processes, each with its
own runtime, data, network path, logs, health, configuration, and failure behavior.

Local development is the practice field:

```text
services + databases + broker + config + logs + tests + seed data + debug commands
```

If a learner cannot run a small local version, production microservices will feel like magic.

---

## 2. Definition

- Definition: local microservices development is a reproducible environment where multiple
  services, dependencies, test data, contracts, and observability tools can run together.
- Category: developer experience, platform readiness, integration testing.
- Core idea: every service should be understandable alone, but the system must also be
  testable as a flow.

---

## 3. Why It Exists

Without a local setup strategy, teams lose time to:

- "works on my machine" runtime drift
- manual startup order
- unknown ports
- missing environment variables
- fake data that does not match real contracts
- local services accidentally using shared staging resources
- no way to reproduce async failures
- no trace/log correlation during development

Strong interview line:

```text
Microservices need a local golden path because the complexity is not only in each service.
It is in the interactions, dependencies, contracts, and failure paths.
```

---

## 4. Local System Minimum

A useful first local system has:

| Piece | Example |
|---|---|
| Entry point | API Gateway or BFF |
| Core service | Booking Service |
| Dependency service | Payment Service |
| Async side effect | Notification worker |
| Database | Postgres or SQLite for learning |
| Broker | Kafka, RabbitMQ, Redis Streams, or in-memory queue for learning |
| Contracts | OpenAPI, protobuf, JSON schema, or documented examples |
| Observability | request ID, logs, simple metrics |
| Tests | smoke test, contract test, failure scenario |

For early learning, a lightweight simulation is fine. The important part is seeing these
behaviors:

- one request enters the system
- one service calls another service
- one local transaction saves state
- an outbox event is created
- an async worker processes the event
- retry/idempotency avoids duplicate side effects
- logs carry the same request ID

---

## 5. Recommended Local Stack

### Beginner

Use one script or Docker Compose with minimal services:

```text
gateway -> booking -> payment
booking -> outbox -> notification worker
```

Use SQLite or an embedded/in-memory broker if dependency setup would distract from concepts.

### Intermediate

Use Docker Compose:

- `booking-service`
- `payment-service`
- `notification-service`
- Postgres
- Kafka or RabbitMQ
- local observability collector if available

### Senior

Use a local Kubernetes option only when needed:

- kind
- minikube
- k3d

Add:

- service discovery
- probes
- graceful shutdown
- local OpenTelemetry collector
- contract test verification
- dependency health dashboards

---

## 6. Folder Structure

Suggested structure:

```text
microservices-mastery-lab/
  README.md
  gateway/
  booking-service/
  payment-service/
  notification-worker/
  contracts/
  scripts/
  docker-compose.yml
  test-data/
  runbooks/
```

Even if the first lab is a single script, teach the learner to think in service boundaries:

```text
Gateway concerns       = routing, auth edge, request ID
Booking concerns       = lifecycle, idempotency, outbox
Payment concerns       = provider interaction, idempotency, audit
Notification concerns  = async delivery, retry, DLQ
```

---

## 7. Environment Variables

Every service should document:

| Variable | Example | Purpose |
|---|---|---|
| `SERVICE_NAME` | `booking-service` | logs/traces |
| `PORT` | `8081` | local HTTP port |
| `DATABASE_URL` | `postgres://...` | data store |
| `BROKER_URL` | `localhost:9092` | async events |
| `PAYMENT_URL` | `http://payment:8082` | dependency |
| `LOG_LEVEL` | `INFO` | debugging |
| `REQUEST_TIMEOUT_MS` | `300` | resilience |

Rules:

- do not hardcode secrets
- use `.env.example`, not `.env` with real values
- keep local and production config separate
- validate required config at startup
- fail readiness if critical config is missing

---

## 8. Startup Order

Bad local setup:

```text
Start service A manually, then B, then DB, then broker, then hope.
```

Better local setup:

```text
dependencies -> migrations -> services -> smoke test -> seed data -> dashboards
```

Startup checklist:

1. Start database and broker.
2. Run migrations.
3. Start services with known ports.
4. Verify `/health/live` and `/health/ready`.
5. Run one happy-path request.
6. Check logs by request ID.
7. Run one duplicate request.
8. Run one dependency failure drill.

---

## 9. Debugging Locally

What to inspect:

| Symptom | First Check |
|---|---|
| API returns 500 | gateway logs and downstream status |
| service cannot call another service | port/DNS/config |
| duplicate booking | idempotency table |
| event not delivered | outbox table and relay logs |
| consumer repeats side effect | processed event table |
| slow request | dependency latency and timeout |
| service starts but gets no traffic | readiness/proxy route |

Local debugging commands conceptually:

```text
curl gateway endpoint
inspect service logs by request ID
query idempotency/outbox tables
force duplicate idempotency key
stop dependency and observe timeout behavior
replay outbox event
```

---

## 10. Contract-First Local Development

Local dev should not mean "whatever JSON worked today."

Contracts to keep:

- OpenAPI for HTTP APIs
- protobuf for gRPC
- event schema for messages
- example requests/responses
- error shape examples

Contract checklist:

1. Can a consumer generate or validate a client?
2. Are required fields explicit?
3. Are error codes documented?
4. Are new fields backward-compatible?
5. Can the contract run in CI?

---

## 11. Test Data

Good seed data is small but realistic.

Example:

| Entity | Test Data |
|---|---|
| User | `guest-1`, `guest-2` |
| Hotel | `hotel-nyc-1` |
| Room | `deluxe-king` |
| Inventory | one remaining room for a date |
| Payment | success card, timeout card, declined card |

Failure-friendly test data:

- payment timeout
- payment declined
- duplicate idempotency key
- notification provider failure
- outbox event with retry count
- tenant-specific data

---

## 12. Observability In Local Dev

Minimum local observability:

- request ID generated at gateway
- request ID propagated to services
- structured logs
- dependency latency in logs
- outbox age
- retry count
- simple health endpoint

Example log fields:

```json
{
  "service": "booking-service",
  "requestId": "req-123",
  "bookingId": "B1",
  "event": "booking_confirmed",
  "latencyMs": 83,
  "status": "success"
}
```

Strong interview line:

```text
I add local request IDs early because distributed debugging habits should be learned before
production, not during an incident.
```

---

## 13. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| One giant local service | hides distributed behavior | run at least gateway + two services |
| Shared local database for every service | teaches wrong ownership | separate schema/tables per service |
| No duplicate request test | misses retry reality | test same idempotency key twice |
| No dependency failure test | false confidence | stop payment service and verify timeout |
| No seed data | every learner invents state | provide deterministic examples |
| Local uses staging dependencies | unsafe and flaky | local mocks/containers |
| Only happy path works | not production learning | include failure drills |

---

## 14. Mini Program / Simulation

This track includes a runnable local capstone:

```text
Marriot/Microservices/microservices-mastery-lab/
```

Run:

```bash
python3 booking_platform_simulation.py
```

Then:

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-key-1' \
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120}'
```

Learning target:

- see request IDs
- see service-to-service HTTP call
- see idempotency
- see outbox event
- see async notification worker
- replay the same request safely

---

## 15. Strong Closing Answer

```text
For local microservices development, I want a golden path that starts dependencies, runs
migrations, starts services, seeds data, and gives me a smoke test plus failure drills. I
keep service boundaries visible even locally: each service owns its config, data, logs,
health checks, and contracts. The goal is not to perfectly reproduce production, but to
reproduce the important interactions and failure modes safely.
```

