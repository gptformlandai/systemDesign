# Architecture Comparison Observability, Logs, Metrics, and Time-Series Case Study - Gold Sheet

> Track File #20 of 30 - Group 04: Scenario Practice
> For: observability/platform interviews | Level: senior | Mode: logs, metrics, traces, retention

## 1. Workloads

- ingest logs at high volume
- search logs by text/fields
- query metrics by time range
- trace service dependencies
- alert on SLOs
- retain cold data cheaply

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| log search | Elasticsearch/OpenSearch or log platform | text and field search |
| metrics | time-series DB/Prometheus-style storage | range queries and rollups |
| traces | tracing backend plus object/cold storage | spans and service graph |
| cold archive | object storage | low-cost retention |
| analytics | warehouse/lakehouse | long-term reporting |
| service dependency graph | graph projection | blast radius and ownership |

---

## 3. Production Risks

- hot shards/current time bucket
- unbounded cardinality labels
- high retention cost
- slow incident search
- missing trace sampling strategy
- alert noise

---

## 4. Strong Interview Answer

```text
For observability, I would separate logs, metrics, traces, and cold archives. Logs need search indexing, metrics need time-series storage and rollups, traces need span storage and sampling, and old raw data can move to object storage. The key production tradeoffs are cardinality, retention, hot shards, query latency, storage cost, and alert reliability.
```

---

## 5. Revision Notes

- One-line summary: Observability storage is shaped by time, cardinality, search, and retention cost.
- Three keywords: logs, metrics, retention.
- One trap: keeping all logs in hot searchable storage forever.