# Lab 07: Proto Evolution Review

## Task

Classify each change as safe or unsafe.

| Change | Safe? | Why |
|---|---|---|
| add `string display_name = 5;` | | |
| remove field `2` without reserving it | | |
| change `int64 amount_cents = 3;` to `double amount = 3;` | | |
| add enum value `PAYMENT_STATUS_REFUNDED = 4;` | | |
| reuse deleted field `7` for `risk_score` | | |
| add new method `GetPaymentHistory` | | |

## Done When

You can describe why protobuf compatibility problems can produce wrong data with `OK` status.