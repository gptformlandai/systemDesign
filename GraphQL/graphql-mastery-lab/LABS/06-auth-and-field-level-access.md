# Lab 06: Auth And Field-Level Access

## Goal

Practice field and object authorization.

## Scenario

`User.email` should be visible only to the viewer or admins.

## Resolver Policy

```text
if context.user.id == parent.id -> allow
if context.user.role == ADMIN -> allow
otherwise -> return null or authorization error based on schema contract
```

## Checklist

- top-level resolver auth
- nested field auth
- object ownership check
- tenant data-source filter
- request-scoped loader cache key
- safe error message

## Interview Takeaway

```text
GraphQL authorization must follow nested field execution and data-source access, not just the top-level operation.
```