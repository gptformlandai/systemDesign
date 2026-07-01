# GraphQL Authorization Data Leak Scenario - MAANG Sheet

> Track File #19 of 30 - Group 04: Scenario Practice
> For: security incident interviews | Level: senior | Mode: auth leak response

## 1. Scenario

```text
A user can see fields on an object they do not own through a nested GraphQL selection.
```

Goal: stop exposure, identify resolver path, and prevent recurrence.

---

## 2. Debug Flow

```text
operation -> path -> resolver -> context user/tenant -> data-source constraints -> cache/loader scope
```

Evidence:

- operation document and variables
- error/data path
- user/tenant identity
- resolver logs/traces
- DataLoader cache key
- data-source query filters

---

## 3. Likely Causes

- top-level resolver checks auth but nested field does not
- object list is filtered incorrectly
- DataLoader cache is global or missing tenant scope
- error response reveals object existence
- federation subgraph assumes gateway already authorized field

---

## 4. Mitigation

- disable or hide exposed field if needed
- patch resolver and data-source tenant constraints
- purge unsafe cache
- add auth regression tests for nested selections
- review similar fields

---

## 5. Interview Summary

```text
For a GraphQL data leak, I trace the exact operation path, resolver, context identity, data-source filter, and cache scope. I patch field/object authorization, purge unsafe caches, add regression tests, and review related schema fields.
```

---

## 6. Revision Notes

- One-line summary: GraphQL data leaks often hide in nested fields and loader scope.
- Three keywords: path, context, tenant.
- One trap: assuming gateway auth is enough for every subgraph and nested field.