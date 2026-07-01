# Lab 08: mTLS And Authz Review

## Scenario

`checkout-api` calls `payment-api` to capture payment.

## Task

Answer:

1. What proves the workload identity?
2. What proves the end-user or application identity?
3. Which method needs per-method authorization?
4. Which resource needs resource-level authorization?
5. Which metadata must be redacted?
6. How are certificates rotated?
7. What does `UNAUTHENTICATED` mean here?
8. What does `PERMISSION_DENIED` mean here?

## Done When

You can separate mTLS, token validation, and authorization policy.