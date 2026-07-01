# 28. gRPC Hands-On Exercises And RPC Drills

## Drill 1: Design A Proto

Create a versioned proto package for `inventory.v1.InventoryService` with:

- `GetItem`
- `ListItems`
- `ReserveItem`
- request/response messages
- enum with `UNSPECIFIED = 0`
- idempotency key for `ReserveItem`

Review for field-number safety and future evolution.

---

## Drill 2: Status Mapping

For each failure, choose a status and client behavior.

| Failure | Status | Client Behavior |
|---|---|---|
| item id malformed | | |
| item not found | | |
| caller lacks tenant access | | |
| reservation conflict | | |
| inventory database unavailable | | |
| request deadline expired | | |
| server panic | | |

---

## Drill 3: grpcurl Smoke Plan

Write grpcurl commands to:

- list services
- describe one service
- call unary method with JSON request
- pass auth metadata
- call using local proto when reflection is disabled

---

## Drill 4: Deadline Budget

Design a deadline budget for:

```text
checkout-api -> order-api -> payment-api -> payment-processor
```

Given a user-facing budget of 900 ms, assign budgets to each hop and explain what happens when payment processing exceeds its budget.

---

## Drill 5: Streaming Contract

Design a server-streaming `WatchInventory` RPC.

Document:

- event ordering
- resume token
- duplicate handling
- max stream lifetime
- heartbeat behavior
- cancellation
- rate limiting

---

## Drill 6: Proto Evolution Review

Given a proto diff, identify whether it is safe:

- add `string display_name = 5;`
- remove `string old_name = 2;` without reserve
- change `int64 amount_cents = 3;` to `double amount = 3;`
- add enum value `PAYMENT_STATUS_REFUNDED = 4;`
- reuse deleted field `7` for `risk_score`

---

## Drill 7: Incident RCA

Write a mini RCA for `DEADLINE_EXCEEDED` spike:

- symptom
- impact
- timeline
- evidence
- root cause
- mitigation
- prevention

---

## Drill 8: Security Review

Review a gRPC service and answer:

- Is TLS or mTLS enabled?
- What identity does the server trust?
- Which metadata carries user/app credentials?
- Where is authorization enforced?
- Are tokens redacted from logs?
- How are certs rotated?

---

## Completion Criteria

You complete this file when you can write contracts, map statuses, design deadlines, explain streaming, review compatibility, and produce an incident RCA without looking up the answer.