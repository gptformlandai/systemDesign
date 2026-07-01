# 11. Proto Evolution, Compatibility, Governance

## Goal

Design protobuf contracts that survive independent client and server deployments.

```text
field-number discipline + compatibility rules + review gates + generated artifacts = safe evolution
```

---

## Compatibility Principle

Distributed systems rarely upgrade every client and server at the same time. A proto change must be safe while old and new versions coexist.

Safe changes usually include:

- adding a new field with a new field number
- adding a new RPC method
- adding a new message type
- reserving deleted field numbers and names
- adding enum values if clients tolerate unknown values correctly

Dangerous changes include:

- reusing field numbers
- changing field meaning or unit
- changing scalar type incompatibly
- removing fields without reservation
- changing method request/response shapes without versioning
- renaming packages in a way that breaks generated clients

---

## Field Evolution Example

```proto
message Invoice {
  reserved 4;
  reserved "legacy_status";

  string invoice_id = 1;
  int64 amount_cents = 2;
  string currency = 3;
  InvoiceState state = 5;
}
```

Deleted field `4` is reserved so a future developer cannot accidentally reuse it for a different meaning.

---

## Versioning Choices

| Strategy | Use When |
|---|---|
| additive fields in same package | normal compatible evolution |
| new method | behavior changes but service remains same version |
| new package version like `orders.v2` | incompatible contract or semantic rewrite |
| gateway translation | external API shape differs from internal RPC |

Avoid creating `v2` for every small change. Version when the compatibility contract truly changes.

---

## Governance Gates

Production proto repositories should have:

- ownership for each package/service
- style/lint rules
- breaking-change checks against main/released schemas
- review from service owners and major client owners
- generated artifact publishing
- changelog/release notes for clients
- compatibility tests for critical methods

---

## Buf Example Gate

```bash
buf lint
buf breaking --against '.git#branch=main'
buf generate
```

The exact tool can vary. The principle should not: schema compatibility must be automated.

---

## Interview Sound Bite

Proto evolution is about independent deployment safety. I protect field numbers, reserve removed fields, prefer additive changes, use package versions for incompatible changes, and enforce lint plus breaking-change checks in CI so old clients and new servers can coexist.