# 17. Scenario: Design Order And Payment gRPC Services

## Prompt

Design service-to-service APIs for an order service calling a payment service.

The system must support creating orders, authorizing payments, capturing payments, and checking payment status.

---

## Good Contract Shape

```proto
syntax = "proto3";

package payments.v1;

service PaymentService {
  rpc AuthorizePayment(AuthorizePaymentRequest) returns (AuthorizePaymentResponse);
  rpc CapturePayment(CapturePaymentRequest) returns (CapturePaymentResponse);
  rpc GetPayment(GetPaymentRequest) returns (GetPaymentResponse);
}

message AuthorizePaymentRequest {
  string idempotency_key = 1;
  string order_id = 2;
  int64 amount_cents = 3;
  string currency = 4;
}
```

---

## Design Decisions

| Area | Decision |
|---|---|
| idempotency | required for side-effecting payment calls |
| status codes | validation uses `INVALID_ARGUMENT`; duplicate key returns prior result or `ALREADY_EXISTS` by contract |
| deadline | order service sets bounded deadline for payment RPC |
| auth | mTLS for service identity plus method authorization |
| observability | trace links order request to payment attempt |
| schema evolution | additive fields only; reserve removed fields |

---

## Failure Handling

| Failure | Response |
|---|---|
| invalid amount | `INVALID_ARGUMENT` |
| duplicate idempotency key with same payload | return stored result |
| duplicate idempotency key with different payload | `ABORTED` or `FAILED_PRECONDITION` by documented policy |
| payment processor unavailable | `UNAVAILABLE` if transient |
| deadline exceeded | `DEADLINE_EXCEEDED`; order workflow moves to retry/reconciliation |

---

## Interview Answer Structure

1. Define service boundaries and proto packages.
2. Choose unary RPCs for bounded commands/queries.
3. Add idempotency keys for payment side effects.
4. Map domain failures to canonical status codes.
5. Set caller deadlines and propagate trace metadata.
6. Secure with mTLS and per-method authorization.
7. Add contract tests and breaking-change gates.

---

## Senior Add-On

Discuss reconciliation. Payments can succeed after the caller times out. The order service should not assume timeout means failure. It needs idempotent retry, status query, and async reconciliation with processor events.