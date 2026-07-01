# Architecture Comparison Anti-Patterns, Debugging, and Decision Traps - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
> For: senior interview/debugging rounds | Level: senior | Mode: anti-patterns and recovery

## 1. Common Decision Anti-Patterns

| Anti-Pattern | Why It Fails | Better Approach |
|---|---|---|
| one database for everything | ignores specialized access patterns | source of truth plus derived stores |
| one new database for every feature | operational sprawl | add only when access pattern justifies it |
| cache as source of truth | data loss/stale state | cache is derived unless explicitly durable |
| search as transaction store | weak correctness | search as derived index |
| vector DB for exact lookup | unnecessary complexity | primary-key database or search |
| graph DB for simple joins | overkill | SQL joins or precomputed projection |
| Cassandra for ad hoc queries | wrong model | query-driven wide-column design |
| dual writes without recovery | inconsistent stores | outbox/CDC/replay |

---

## 2. Debugging Bad Database Choices

Symptoms:

- p99 spikes under one access pattern
- stale search/vector/cache results
- cross-shard transactions fail
- hot partition or hot shard
- impossible ad hoc query
- missing audit trail
- high operational cost

Debug order:

```text
workflow -> access pattern -> source of truth -> derived stores -> consistency need -> sync path -> SLO -> failure mode
```

---

## 3. Interview Recovery Phrases

Use these when you need to correct direction:

```text
I would not make that the source of truth; I would use it as a derived index.
```

```text
That choice works if the access pattern is bounded. If ad hoc querying is required, I would reconsider.
```

```text
The main risk is freshness, so I would add CDC, lag monitoring, and a rebuild path.
```

---

## 4. Strong Interview Answer

```text
The biggest architecture comparison mistake is optimizing for one property while ignoring correctness and operations. I would identify the workflow, source of truth, access pattern, consistency need, sync path, SLO, failure mode, and operational owner before choosing or adding a datastore.
```

---

## 5. Revision Notes

- One-line summary: Bad database choices usually hide a misunderstood access pattern or ownership boundary.
- Three keywords: source, sync, SLO.
- One trap: adding a derived store without a rebuild path.