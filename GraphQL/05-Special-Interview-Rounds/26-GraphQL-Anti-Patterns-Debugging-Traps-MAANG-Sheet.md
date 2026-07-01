# GraphQL Anti-Patterns And Debugging Traps - MAANG Sheet

> Track File #26 of 30 - Group 05: Special Interview Rounds
> For: production maturity | Level: senior | Mode: traps and safer alternatives

## 1. Anti-Patterns

| Anti-Pattern | Why It Is Bad | Safer Approach |
|---|---|---|
| expose database tables directly | leaks implementation and weakens contract | model product/domain concepts |
| unbounded list fields | latency and memory risk | pagination and max page size |
| global DataLoader cache | cross-user/tenant leak risk | request-scoped loaders |
| auth only at top-level Query | nested data leaks | field/object-level authorization |
| no operation naming | poor observability | named operations or persisted IDs |
| arbitrary public queries | abuse/performance risk | complexity limits and persisted queries |
| remove fields directly | breaks clients | deprecate and monitor usage |
| endpoint-only metrics | hides slow operations | operation/resolver metrics |

---

## 2. Debugging Traps

- GraphQL can return HTTP 200 with errors.
- `data` can be partial when `errors` exists.
- non-null fields can null a parent object.
- nested resolvers can bypass top-level checks.
- client cache may show stale data after server fix.
- federation failures may be composition or runtime query-plan issues.
- disabling introspection does not replace authorization.

---

## 3. Interview Recovery Phrase

```text
I would identify the schema field and operation first, then trace validation, resolver execution, data-source calls, auth context, cache scope, and error path.
```

---

## 4. Revision Notes

- One-line summary: GraphQL traps come from flexible field selection without enough guardrails.
- Three keywords: bounded, scoped, observable.
- One trap: believing GraphQL solves backend performance automatically.