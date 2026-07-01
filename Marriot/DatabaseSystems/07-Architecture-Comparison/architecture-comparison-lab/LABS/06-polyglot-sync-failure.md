# Lab 06: Polyglot Sync Failure

Goal: debug a stale derived-store incident.

---

## Scenario

```text
The product price changed in the catalog database, but search results and cache still show the old price.
```

---

## Debug Checklist

1. Confirm source-of-truth price.
2. Check outbox/CDC event emitted.
3. Check search index consumer lag.
4. Check cache invalidation path.
5. Check retries and dead-letter queues.
6. Rebuild affected derived records if needed.
7. Add freshness alert or regression test.

---

## Completion Gate

- You can explain dual-write risk.
- You can explain CDC lag.
- You can explain rebuild and replay.