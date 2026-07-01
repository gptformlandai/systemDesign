# Elasticsearch Anti-Patterns, Internals, and Debugging - MAANG Sheet

> Track File #21 of 27 - Group 05: Special Interview Rounds
> For: backend/search/system design interviews | Level: senior / MAANG | Mode: traps, fixes, production debugging

This sheet builds:
- Elasticsearch anti-pattern recognition
- Production debugging playbooks
- Interview follow-up confidence

---

## 1. Top Anti-Patterns

| Anti-Pattern | Why It Fails | Better Approach |
|---|---|---|
| using Elasticsearch as source of truth | weak transactional model for primary data | sync from source DB/event stream |
| dynamic mapping everywhere | wrong types and mapping explosion | explicit mappings/templates |
| deep pagination with `from` | memory and shard coordination cost | `search_after` + PIT |
| wildcard/regex on hot path | expensive query execution | n-grams, prefixes, controlled fields |
| aggregating on `text` | fielddata/heap risk or failure | keyword/doc values fields |
| one giant time index | huge shards and hard retention | data streams/rollover/ILM |
| too many tiny shards | heap/cluster-state overhead | shard sizing and rollover |
| no alias strategy | unsafe mapping migrations | versioned indices + aliases |
| applying ACL after retrieval | data leak risk | filter before retrieval |

---

## 2. Debug: Mapping Explosion

Symptoms:

- cluster-state growth
- mapping update pressure
- high heap
- index failures from field limits

Fixes:

- explicit mappings
- dynamic templates
- flattened fields for arbitrary metadata
- reject arbitrary JSON keys
- isolate noisy tenants/sources

---

## 3. Debug: Hot Shard

Symptoms:

- one shard/node has high CPU or latency
- tenant or routing key dominates
- uneven disk/search/write pressure

Fixes:

- routing redesign
- index split by tenant tier or time
- rollover to new shard count
- throttle noisy workload

---

## 4. Debug: Stale Search Results

Causes:

- refresh delay
- sync lag
- failed bulk item
- alias points to old index
- source event missing

Fixes:

- inspect sync pipeline and failed items
- define freshness SLO
- verify alias and document version
- use source-of-truth fallback where needed

---

## 5. Strong Debugging Answer

```text
I start with the exact query and index alias. Then I inspect mappings, shard fan-out, slow logs, profile output, heap/GC, search or indexing rejections, merge pressure, disk watermarks, and sync lag. If the issue is structural, such as mapping explosion or bad shard design, the durable fix is reindexing with a corrected mapping or index strategy, not only adding nodes.
```

---

## 6. Revision Notes

- One-line summary: Elasticsearch anti-patterns are usually mapping, query, shard, sync, or security-model failures.
- Three keywords: mapping explosion, deep pagination, hot shard.
- One interview trap: treating more nodes as the first fix.
- Memory trick: find the query, then the index, then the shard.