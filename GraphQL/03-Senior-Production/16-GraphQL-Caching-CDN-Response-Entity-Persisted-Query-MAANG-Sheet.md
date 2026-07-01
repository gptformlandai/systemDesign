# GraphQL Caching: CDN, Response, Entity, Persisted Query - MAANG Sheet

> Track File #16 of 30 - Group 03: Senior Production
> For: caching strategy | Level: senior | Mode: GraphQL caching

## 1. Core Idea

GraphQL caching is harder than REST because many operations share one endpoint and response shape varies by query.

```text
operation identity + variables + auth scope + entity identity + freshness rule = cache key strategy
```

---

## 2. Cache Layers

| Layer | Use Case |
|---|---|
| client normalized cache | UI entity reuse |
| request-scoped DataLoader cache | dedupe loads during one request |
| server response cache | cache full operation response when safe |
| entity/object cache | cache backend objects by ID/scope |
| persisted query/CDN cache | cache known public or semi-public operations |

---

## 3. Key Design Questions

- Is data user-specific or public?
- What variables affect response?
- What auth scope affects response?
- How is invalidation triggered?
- Can stale data be tolerated?
- Is operation persisted and named?

---

## 4. Failure Modes

- shared cache leaks private data
- response cache ignores variables or auth
- client normalized cache has unstable IDs
- CDN cannot cache because POST/headers are uncontrolled
- stale data survives mutation without invalidation

---

## 5. Interview Summary

```text
GraphQL caching needs explicit operation identity, variables, auth scope, entity IDs, and freshness policy. I combine client normalized cache, request-scoped loaders, server/entity caches, persisted queries, and CDN caching only where privacy and invalidation are safe.
```

---

## 6. Revision Notes

- One-line summary: GraphQL caching is cache-key and auth-scope design.
- Three keywords: operation identity, auth scope, entity cache.
- One trap: caching full GraphQL responses without including user/tenant scope.