# GraphQL Schema Evolution Breaking Change Scenario - Gold Sheet

> Track File #20 of 30 - Group 04: Scenario Practice
> For: schema governance interviews | Level: intermediate to senior | Mode: compatibility

## 1. Scenario

```text
A team wants to rename a field used by several mobile and web clients.
```

Goal: evolve the schema without breaking active clients.

---

## 2. Safe Flow

```text
add new field -> mark old field deprecated -> monitor operation usage -> migrate clients -> remove after policy window
```

---

## 3. Required Evidence

- schema registry diff
- client operation usage
- owner of affected clients
- deprecation reason/date
- compatibility check in CI
- rollout and removal plan

---

## 4. Common Breaking Changes

- remove field/type/enum value used by clients
- rename field
- change field type
- add required input field
- change nullability unexpectedly
- change resolver semantics without schema change

---

## 5. Interview Summary

```text
For GraphQL schema evolution, I prefer additive fields, deprecate old fields with reason, track real client operation usage, migrate clients, run breaking-change checks in CI, and remove only after an agreed policy window.
```

---

## 6. Revision Notes

- One-line summary: GraphQL schema changes need telemetry-backed deprecation discipline.
- Three keywords: additive, deprecated, usage.
- One trap: renaming a field directly because GraphQL has no URL version.