# 29. gRPC Mini Projects Portfolio

## Project 1: Greeter Plus

Build a simple unary gRPC service with:

- versioned proto package
- unary method
- generated server/client
- grpcurl smoke command
- deadline on client call
- status mapping for invalid input

Portfolio proof:

- proto file
- server implementation
- client call
- grpcurl output
- README explaining the RPC lifecycle

---

## Project 2: Inventory Service

Build an inventory service with:

- `GetItem`
- `ListItems`
- `ReserveItem`
- idempotency key for reservation
- method-level metrics
- contract tests
- Buf lint/breaking check if tooling is available

Portfolio proof:

- safe proto evolution notes
- status-code mapping table
- integration tests
- incident notes for invalid request and not found

---

## Project 3: Streaming Event Watcher

Build a server-streaming API with:

- resume token
- heartbeat event
- cancellation handling
- bounded buffer
- slow-consumer test
- reconnect example

Portfolio proof:

- streaming protocol documentation
- load/slow consumer notes
- cancellation test evidence

---

## Project 4: Secure Payment RPC

Build a payment-style API with:

- mTLS or local TLS simulation
- auth metadata validation
- per-method authorization
- idempotent capture call
- deadline and retry policy notes
- audit-safe logs

Portfolio proof:

- security design document
- auth failure test cases
- idempotency behavior demo
- redaction checklist

---

## Project 5: gRPC On Kubernetes Design Pack

Create a design-only or working deployment pack with:

- Deployment and Service manifest
- readiness probe notes
- graceful shutdown plan
- Envoy/mesh timeout alignment notes
- gRPC health check plan
- dashboard/SLO sketch

Portfolio proof:

- architecture diagram or markdown flow
- timeout table
- incident runbook
- rollout checklist

---

## Project 6: Runnable Java And Go Payment Service

Build a runnable payment service using the blueprint in [37-gRPC-Runnable-Java-Go-Lab-Guide.md](37-gRPC-Runnable-Java-Go-Lab-Guide.md).

Required:

- shared `payments.v1.PaymentService` proto
- Java or Go server
- Java or Go client with deadline
- `GetPayment`, `CapturePayment`, and `WatchPayments`
- idempotency key behavior
- grpcurl smoke tests
- health and local reflection
- status-code contract tests
- Docker or compose run path

Portfolio proof:

- run commands
- grpcurl output
- failing request mapped to `INVALID_ARGUMENT`
- duplicate capture demo
- stream cancellation demo
- readiness/shutdown notes

---

## Portfolio Review Rubric

| Area | Evidence |
|---|---|
| contract design | clear proto, field-number safety, versioning |
| implementation | generated stubs used cleanly, runnable server/client exists |
| reliability | deadlines, cancellation, retries/idempotency |
| security | TLS/mTLS/auth metadata/authz |
| observability | metrics, traces, logs, SLOs |
| operations | grpcurl, runbooks, incident response |

Complete at least three projects for strong interview signal. Complete Project 6 for the strongest practical signal.
