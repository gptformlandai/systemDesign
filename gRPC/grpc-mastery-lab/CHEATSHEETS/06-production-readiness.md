# Production Readiness Cheatsheet

## Contract

- versioned package
- safe field numbers
- reserved deleted fields
- CI lint and breaking checks
- generated artifacts versioned

## Runtime

- deadlines
- cancellation
- status mapping
- bounded retries
- idempotency
- streaming backpressure

## Security

- TLS/mTLS
- token validation
- per-method authz
- cert rotation
- metadata redaction

## Operations

- health checks
- service discovery
- load balancing
- proxy timeout alignment
- connection draining
- method metrics/traces/logs
- runbooks