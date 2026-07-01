# Lab 05: Operations Incident Drills

Goal: rehearse production debugging language for Elasticsearch incidents.

---

## Drill 1: Slow Search

Symptom:

```text
Product search p99 increases after a release.
```

Check:

- query DSL diff
- index alias
- shard fan-out
- slow logs/profile
- deep pagination
- high-cardinality aggregations
- heap/GC
- thread-pool rejections

---

## Drill 2: Mapping Explosion

Symptom:

```text
New log source adds thousands of dynamic fields and cluster stability degrades.
```

Check:

- mapping field count
- cluster state
- dynamic templates
- source payload shape

---

## Drill 3: Stale Results

Symptom:

```text
Product price changed in the database but search still shows old price.
```

Check:

- source event emitted
- bulk indexing result
- failed items
- refresh/freshness SLO
- alias points to expected index

---

## Completion Gate

For each incident, explain:

1. Most likely index/query.
2. Evidence to gather.
3. Immediate mitigation.
4. Durable design or operations fix.
5. Alert that should catch it next time.