# GraphQL Security: Auth, Introspection, Rate Limits, Injection - MAANG Sheet

> Track File #13 of 30 - Group 03: Senior Production
> For: API security interviews | Level: senior | Mode: secure GraphQL

## 1. Core Idea

GraphQL security must account for flexible query shape, nested resolver execution, and field-level data exposure.

```text
identity -> authorization -> query cost -> resolver/data-source constraints -> safe response
```

---

## 2. Controls

| Risk | Control |
|---|---|
| data leak | field/object-level authorization |
| expensive query | depth/complexity limits, persisted queries |
| brute force | rate limits by identity and operation |
| introspection exposure | environment-specific policy |
| injection | parameterized data-source calls and input validation |
| noisy errors | stable error codes, no sensitive internals |
| cross-tenant cache leak | request/tenant-scoped loaders and cache keys |

---

## 3. Introspection Policy

Options:

- enable in dev and internal trusted tools
- restrict in public production
- publish schema via registry instead
- combine with authentication and allowlisted operations

---

## 4. Failure Modes

- nested resolver bypasses auth
- DataLoader leaks across tenants
- introspection exposes private fields
- SQL/NoSQL query built from unvalidated args
- rate limiting only by HTTP endpoint ignores operation cost

---

## 5. Interview Summary

```text
GraphQL security requires identity-aware field/object authorization, query cost controls, scoped caches/loaders, validated inputs, safe errors, introspection policy, and operation-aware rate limiting.
```

---

## 6. Revision Notes

- One-line summary: GraphQL security follows the field tree and operation cost.
- Three keywords: authz, complexity, introspection.
- One trap: protecting `/graphql` once and assuming all nested fields are safe.