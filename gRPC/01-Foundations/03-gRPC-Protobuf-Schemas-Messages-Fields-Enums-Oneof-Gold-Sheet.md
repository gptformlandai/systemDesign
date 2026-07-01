# 03. Protocol Buffers: Schemas, Messages, Fields, Enums, Oneof

## Goal

Learn protobuf as a long-lived API contract, not as a temporary DTO format.

```text
message name + field names + stable field numbers + compatible evolution = safe wire contract
```

---

## Message Basics

```proto
syntax = "proto3";

package payments.v1;

message Payment {
  string payment_id = 1;
  int64 amount_cents = 2;
  string currency = 3;
  PaymentStatus status = 4;
}

enum PaymentStatus {
  PAYMENT_STATUS_UNSPECIFIED = 0;
  PAYMENT_STATUS_PENDING = 1;
  PAYMENT_STATUS_CAPTURED = 2;
  PAYMENT_STATUS_FAILED = 3;
}
```

Field number `1`, `2`, `3`, and `4` are the wire identity. Renaming a field is usually less dangerous than reusing a field number incorrectly.

---

## Field Number Rules

| Rule | Reason |
|---|---|
| never reuse deleted field numbers | old clients can decode wrong meaning |
| reserve deleted field numbers and names | prevents accidental reuse |
| add new optional fields instead of changing meaning | old clients ignore unknown fields |
| keep field meanings stable | same wire data must mean same thing |
| avoid required fields in distributed APIs | independent deployments need tolerance |

Example:

```proto
message Customer {
  reserved 3, 7;
  reserved "legacy_tier", "old_region";

  string customer_id = 1;
  string display_name = 2;
  string region = 4;
}
```

---

## Enum Rules

Always include an unspecified zero value.

```proto
enum RiskLevel {
  RISK_LEVEL_UNSPECIFIED = 0;
  RISK_LEVEL_LOW = 1;
  RISK_LEVEL_MEDIUM = 2;
  RISK_LEVEL_HIGH = 3;
}
```

Why: zero is the default value in proto3. A meaningful zero value can hide missing data.

---

## Oneof

Use `oneof` when exactly one of several shapes is valid.

```proto
message NotificationTarget {
  oneof target {
    string email = 1;
    string phone = 2;
    string user_id = 3;
  }
}
```

Production caution: changing fields inside a `oneof` requires compatibility review. Unknown fields and oneof presence can surprise older clients.

---

## Wrapper And Optional Presence

Proto3 defaults can make absent and zero values look similar. Use presence-aware fields when the distinction matters.

Examples:

- `optional int32 retry_count = 1;`
- wrapper types in older ecosystems
- explicit `has_x` style fields when needed for product semantics

---

## Common Schema Anti-Patterns

| Anti-Pattern | Better Approach |
|---|---|
| `map<string, string> metadata` for everything | typed fields for core business data |
| generic `payload` bytes | explicit message shapes |
| reusing field numbers | reserve numbers and names |
| changing units silently | add a new field with clear unit name |
| enum zero means success | use `UNSPECIFIED` as zero |

---

## Interview Sound Bite

In protobuf, field numbers are the durable wire contract. Safe evolution means adding fields, reserving deleted numbers/names, keeping meanings stable, using enum zero as unspecified, and automating compatibility checks with tooling like Buf.