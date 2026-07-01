# Lab 04: Deadline Budget Drill

## Scenario

```text
mobile app -> checkout-api -> order-api -> payment-api -> payment-processor
```

The user-facing budget is 900 ms.

## Task

Create a budget table:

| Hop | Budget | Reason |
|---|---:|---|
| mobile to checkout | | |
| checkout to order | | |
| order to payment | | |
| payment to processor | | |

## Questions

1. Where should each deadline be set?
2. What happens if payment times out after the processor succeeds?
3. Where does idempotency matter?
4. Which spans prove where the budget was spent?

## Done When

You can explain `DEADLINE_EXCEEDED` without guessing.