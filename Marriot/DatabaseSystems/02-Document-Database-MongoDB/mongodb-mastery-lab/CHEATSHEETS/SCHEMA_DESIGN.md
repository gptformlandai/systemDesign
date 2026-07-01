# Schema Design Cheat Sheet

## First Question

Do not start with collections. Start with access patterns:

- What does the API read by ID?
- What does the API list and sort?
- What changes together?
- What must be atomic?
- What grows forever?
- What is duplicated safely?
- What needs indexes?

## Embed When

| Condition | Example |
|---|---|
| Data is read together | Order with line items |
| One-to-one or one-to-few | User profile addresses |
| Child data is bounded | Cart items with max limit |
| Same lifecycle | Shipping address snapshot in order |
| Atomic update needed | Cart item count and total |

## Reference When

| Condition | Example |
|---|---|
| Child grows forever | Product reviews |
| Many-to-many | Users and teams |
| Child is large | Attachments metadata vs file storage |
| Child updates independently | Product inventory |
| Shared entity | Customer referenced by orders |

## Patterns

| Pattern | Use |
|---|---|
| Embedded document | Data read together |
| Reference | Independent growth/lifecycle |
| Subset | Recent/top child preview in parent |
| Bucket | Many time-ordered events |
| Outlier | Rare extreme documents split out |
| Computed | Precomputed counts/totals |
| Extended reference | Reference plus display snapshot |
| Attribute | Flexible searchable attributes |
| Polymorphic | Shared collection with type field |
| Tree/materialized path | Hierarchies |
| Pre-aggregation | Dashboards and reports |
| Event sourcing | Immutable state transitions |
| CQRS read model | Separate write and read shapes |

## Red Flags

- Unbounded arrays.
- Documents approaching 16 MB.
- Many `$lookup` joins on hot paths.
- Fields with inconsistent types.
- No tenant field in multi-tenant data.
- No schema version during evolution.
- Data duplicated without an update strategy.
