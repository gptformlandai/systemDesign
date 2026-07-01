# Elasticsearch ILM, Data Streams, Rollover, and Lifecycle - Gold Sheet

> Track File #12 of 27 - Group 03: Senior / MAANG
> For: backend/search/system design interviews | Level: senior | Mode: index lifecycle, retention, time-series operations

This sheet builds:
- Index lifecycle management
- Aliases, rollover, and data streams
- Retention and snapshot thinking

---

## 1. Why Lifecycle Exists

Indexes grow. Without lifecycle control, shards become too large, retention becomes manual, and operations become risky.

Lifecycle questions:

- How fast does data arrive?
- How long is it searched frequently?
- When can it move to cheaper storage?
- When should it be deleted?
- What is the snapshot policy?

---

## 2. Rollover Pattern

Use aliases to write to the current index and roll over when size/age thresholds are reached.

```text
logs-write -> logs-000003
logs-read  -> logs-*
```

Rollover can be based on:

- max primary shard size
- index age
- document count

---

## 3. Data Streams

Data streams are useful for append-only time-series data such as logs, metrics, traces, and events.

They provide:

- backing indices
- write index management
- lifecycle integration
- time-based data model around `@timestamp`

Use for:

- logs
- security events
- metrics-like events
- append-only observability data

Avoid for:

- update-heavy entity search
- product catalogs with frequent partial updates

---

## 4. Hot/Warm/Cold Thinking

| Tier | Purpose |
|---|---|
| hot | active indexing and frequent search |
| warm | less frequent search, lower cost |
| cold/frozen | rare access and retention |
| delete | remove after retention |

Interview maturity:

```text
Search retention is cost and latency design. Not all data deserves hot-tier storage forever.
```

---

## 5. Strong Answer

Question:

> How would you manage log retention in Elasticsearch?

Strong answer:

```text
I would use data streams or rollover-backed indices with ILM. New logs go to the current write backing index, then roll over based on shard size or age. ILM moves older data to cheaper tiers and deletes it after retention. I would align this with query SLOs, storage cost, snapshot policy, and compliance requirements.
```

---

## 6. Revision Notes

- One-line summary: ILM keeps growing search data bounded, tiered, and disposable by policy.
- Three keywords: rollover, data stream, retention.
- One interview trap: one forever-growing logs index.
- Memory trick: hot data should age out before it burns the cluster.