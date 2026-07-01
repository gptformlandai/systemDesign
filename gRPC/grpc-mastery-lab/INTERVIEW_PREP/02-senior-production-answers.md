# Senior Production Answers

## How Do You Make gRPC Production Ready?

Use safe proto evolution, generated-code hygiene, deadlines, cancellation, canonical status mapping, retries only for idempotent methods, mTLS/authz, method-level observability, health checks, load balancing, and deployment runbooks.

## How Do You Handle Schema Evolution?

Prefer additive changes, reserve removed fields, never reuse field numbers, use new package versions for incompatible changes, and enforce lint/breaking checks in CI.

## How Do You Handle Resilience?

Set deadlines first. Then use bounded retries with backoff only for safe/idempotent methods. Use idempotency keys for side effects and monitor retry amplification.

## How Do You Handle Security?

Use TLS/mTLS for transport and workload identity, validate JWT/OAuth metadata where needed, enforce method/resource authorization, rotate certs, and redact sensitive metadata.