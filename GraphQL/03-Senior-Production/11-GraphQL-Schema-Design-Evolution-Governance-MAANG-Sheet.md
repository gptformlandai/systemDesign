# GraphQL Schema Design, Evolution, Governance - MAANG Sheet

> Track File #11 of 30 - Group 03: Senior Production
> For: API architecture interviews | Level: senior | Mode: schema governance

## 1. Core Idea

The GraphQL schema is a product contract. Production teams need rules for naming, ownership, compatibility, deprecation, and review.

```text
schema field -> client dependency -> compatibility rule -> rollout/deprecation -> governance evidence
```

---

## 2. Design Rules

- model business concepts, not database tables directly
- name fields consistently
- prefer additive changes
- deprecate before removing
- avoid exposing internal implementation details
- define ownership for types and fields
- review nullability carefully
- track client operation usage before breaking changes

---

## 3. Evolution Strategy

| Change | Compatibility |
|---|---|
| add nullable field | usually safe |
| add enum value | can break strict clients |
| add required input field | breaking |
| remove field | breaking |
| change field type | breaking |
| make nullable field non-null | can be breaking |

---

## 4. Production Controls

- schema registry
- breaking-change checks
- operation safelisting or telemetry
- deprecation policy
- schema review checklist
- owner metadata
- changelog for clients

---

## 5. Interview Summary

```text
I treat a GraphQL schema as a durable product contract. I prefer additive changes, use deprecation and telemetry before removals, run breaking-change checks in CI, assign schema ownership, and design nullability and naming for long-term client safety.
```

---

## 6. Revision Notes

- One-line summary: Schema governance prevents client breakage and API drift.
- Three keywords: compatibility, deprecation, ownership.
- One trap: assuming GraphQL removes API versioning needs without schema evolution discipline.