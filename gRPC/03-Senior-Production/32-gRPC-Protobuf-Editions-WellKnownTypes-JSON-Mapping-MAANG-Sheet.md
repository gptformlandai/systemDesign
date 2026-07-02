# 32. Protobuf Editions, Well-Known Types, JSON Mapping, And API Design

## Goal

Move from "I can write proto3 messages" to "I can design long-lived protobuf APIs that survive language differences, JSON gateways, schema evolution, and independent deploys."

```text
field number discipline + presence semantics + well-known types + JSON mapping + API style = durable protobuf contract
```

---

## 1. Proto3 vs Protobuf Editions

Proto3 is still common, but Protobuf Editions add a newer way to express language features with edition-level feature settings.

```proto
edition = "2023";

package payments.v1;

message CapturePaymentRequest {
  string payment_id = 1;
  int64 amount_cents = 2;
}
```

Senior interview point:

```text
Do not treat syntax choice as cosmetic. Presence, default behavior, generated
APIs, JSON behavior, and migration strategy can differ by syntax/edition and
language runtime.
```

Migration stance:

- Do not rewrite a healthy proto3 estate just to chase Editions.
- Use Editions deliberately for new APIs when language/tool support is ready.
- Add compatibility tests before migrating generated client SDKs.
- Document presence semantics in API contracts.

---

## 2. Field Presence

Presence answers:

```text
Can the receiver tell the difference between "field was not set" and
"field was set to the default value"?
```

Why it matters:

- PATCH/update methods.
- Partial responses.
- Business values where zero/empty is meaningful.
- Audit trails and client intent.

Example:

```proto
syntax = "proto3";

message UpdateInventoryRequest {
  string sku = 1;
  optional int32 available_quantity = 2;
}
```

Without presence:

```text
available_quantity = 0

Could mean:
  client intentionally set stock to zero
  OR field was not sent
```

With `optional`, server logic can check whether the field was present.

---

## 3. Well-Known Types

Use well-known types when the meaning is standard and cross-language behavior matters.

| Type | Use |
|---|---|
| `google.protobuf.Timestamp` | absolute point in time |
| `google.protobuf.Duration` | elapsed time or timeout |
| `google.protobuf.FieldMask` | partial read/update paths |
| `google.protobuf.Empty` | request/response has no fields |
| `google.protobuf.Struct` | dynamic JSON-like object, use sparingly |
| `google.protobuf.Any` | embedded arbitrary typed message, use sparingly |
| `google.rpc.Status` | rich error model outside raw transport |

Bad:

```proto
string created_at = 1; // timezone, format, and parsing ambiguity
int64 timeout_ms = 2;  // unit embedded in name, harder to evolve
```

Better:

```proto
google.protobuf.Timestamp created_at = 1;
google.protobuf.Duration timeout = 2;
```

---

## 4. `Any`, `Struct`, And Extension Points

`Any` and `Struct` are escape hatches, not default design tools.

Use `Any` when:

- You control a small plugin ecosystem.
- The embedded type URL is documented.
- Consumers know how to unpack allowed types.
- Unknown types have a safe fallback path.

Use `Struct` when:

- You genuinely need JSON-like extension data.
- The field is not core business logic.
- You cap size and document allowed keys.

Avoid:

```proto
message PaymentEvent {
  string event_type = 1;
  google.protobuf.Struct payload = 2; // turns contract into loose JSON
}
```

Prefer:

```proto
message PaymentEvent {
  string event_id = 1;
  oneof event {
    PaymentCaptured captured = 2;
    PaymentFailed failed = 3;
  }
}
```

---

## 5. JSON Mapping And Gateways

Many systems expose protobuf APIs through JSON transcoding or gRPC-Web. That means JSON mapping becomes part of the API contract.

Watch for:

- `int64` values may map to JSON strings.
- Field names use lowerCamelCase by default in JSON.
- Default values may be omitted.
- Unknown fields can behave differently across JSON vs binary.
- Enums have string names in JSON by default.

Design implication:

```text
If external clients consume JSON, test both binary protobuf and JSON mapping.
Do not assume a proto-safe change is automatically safe for JSON clients.
```

---

## 6. API Shape Rules

### Pagination

```proto
message ListOrdersRequest {
  string customer_id = 1;
  int32 page_size = 2;
  string page_token = 3;
}

message ListOrdersResponse {
  repeated Order orders = 1;
  string next_page_token = 2;
}
```

Use server streaming only when the caller needs a live stream or very large sequential output with backpressure.

### Batch RPCs

Batch APIs are useful when clients would otherwise do N small RPCs.

```proto
rpc BatchGetOrders(BatchGetOrdersRequest) returns (BatchGetOrdersResponse);
```

Rules:

- Bound batch size.
- Preserve per-item status if partial success is allowed.
- Avoid hiding huge fan-out behind one innocent-looking RPC.

### Long-Running Operations

For long work:

```text
Start operation -> return operation id -> poll/get operation -> optional cancel
```

Do not keep unary RPCs open for minutes unless the system is intentionally designed for it.

---

## 7. Compatibility Traps

| Change | Safe? | Why |
|---|---|---|
| Add new field number | Usually safe | Old clients ignore unknown fields |
| Reuse deleted field number | Unsafe | Old data can deserialize incorrectly |
| Change field type | Usually unsafe | Wire interpretation can break |
| Rename field only | Binary safe, JSON risky | JSON clients see names |
| Add enum value | Usually safe | Old clients need unknown handling |
| Reorder fields | Binary safe | But do not churn generated diffs casually |
| Move field into `oneof` | Risky | Presence and wire behavior change |
| Add required field | Unsafe | Breaks old clients |

---

## 8. Interview Scenario

> A team says, "We use protobuf, so our API is automatically backward compatible." How do you respond?

Good answer:

```text
Protobuf gives you tools for compatibility, not automatic safety. Field numbers
are the durable wire contract, so we never reuse them and we reserve removed
numbers and names. We keep changes additive, test JSON mapping when gateways are
used, document field presence for updates, use well-known types for time and
field masks, and enforce lint plus breaking checks in CI. Old clients and new
servers are never upgraded at exactly the same time, so compatibility must be
designed and tested.
```

---

## Senior Sound Bite

Protobuf maturity is about schema lifecycle, not just syntax. I design with stable field numbers, explicit presence where intent matters, well-known types for common semantics, cautious JSON mapping, bounded pagination/batch shapes, and CI gates so independently deployed clients and servers keep working.

## Official Source Notes

- Protobuf Editions: <https://protobuf.dev/programming-guides/editions/>
- Proto3 guide: <https://protobuf.dev/programming-guides/proto3/>
- Proto best practices: <https://protobuf.dev/best-practices/dos-donts/>
- Field masks: <https://protobuf.dev/reference/protobuf/google.protobuf/#field-mask>

