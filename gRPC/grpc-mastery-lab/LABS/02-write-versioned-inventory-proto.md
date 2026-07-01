# Lab 02: Write A Versioned Inventory Proto

## Task

Design `inventory.v1.InventoryService` with:

- `GetItem`
- `ListItems`
- `ReserveItem`
- request and response messages
- `InventoryStatus` enum with `INVENTORY_STATUS_UNSPECIFIED = 0`
- idempotency key for `ReserveItem`

## Review Checklist

- [ ] Package name includes version.
- [ ] Field numbers are stable and not reused.
- [ ] Side-effect method has idempotency plan.
- [ ] Large list response has pagination or streaming decision.
- [ ] Enum zero is unspecified.

## Done When

You can explain why each method is unary or streaming and how the contract can evolve safely.