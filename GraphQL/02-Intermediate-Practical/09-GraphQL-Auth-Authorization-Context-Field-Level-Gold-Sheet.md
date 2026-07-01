# GraphQL Auth, Authorization, Context, Field-Level Access - Gold Sheet

> Track File #9 of 30 - Group 02: Intermediate Practical
> For: API security | Level: intermediate | Mode: auth and authz

## 1. Core Idea

GraphQL authorization must be enforced at every field/object/data access path, not only at the top-level operation.

```text
request token -> context user -> resolver authz -> data-source constraints -> field result
```

---

## 2. Auth Layers

| Layer | Responsibility |
|---|---|
| authentication | prove who the caller is |
| operation authorization | can caller execute this operation? |
| object authorization | can caller access this object? |
| field authorization | can caller see this field? |
| data-source authorization | enforce tenant/user constraints near data |

---

## 3. Context Pattern

Context commonly carries:

- user identity
- tenant/account ID
- roles/permissions
- request ID
- loaders
- service clients

Do not trust client-supplied fields for tenant/user scope when the server already knows the identity from auth.

---

## 4. Failure Modes

- top-level query checks auth, nested field leaks sensitive data
- list resolver returns objects caller should not see
- DataLoader cache ignores auth scope
- introspection reveals private schema unintentionally
- error messages disclose object existence

---

## 5. Interview Summary

```text
In GraphQL, I treat auth as field and object authorization, not just endpoint access. Context carries identity, resolvers enforce policy, data sources apply tenant constraints, and loaders/cache must be scoped to the authenticated request.
```

---

## 6. Revision Notes

- One-line summary: GraphQL auth must follow the field tree.
- Three keywords: context, field auth, tenant scope.
- One trap: securing only Query and Mutation fields while nested object fields leak data.