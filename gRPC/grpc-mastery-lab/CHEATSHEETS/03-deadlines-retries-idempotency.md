# Deadlines, Retries, Idempotency Cheatsheet

## Deadline Rules

- Every client RPC needs a deadline.
- Downstream calls receive sub-budgets.
- Servers should stop work after cancellation.
- Proxy timeouts should align with app deadlines.

## Retry Rules

- Retry only bounded attempts.
- Use backoff and jitter where appropriate.
- Do not retry invalid requests.
- Do not blindly retry side-effecting calls.

## Idempotency Rules

- Use client-generated idempotency keys for side effects.
- Store operation result by key.
- Return the prior result for duplicate safe requests.
- Detect duplicate key with different payload.

## Hedging Rule

Use hedging only for safe/idempotent methods with strict attempt caps and monitoring.