# Elasticsearch Logs, Observability, and Security Analytics - Gold Sheet

> Track File #18 of 27 - Group 04: Scenario Practice
> For: backend/search/operations interviews | Level: senior | Mode: logs, data streams, retention, dashboards

This sheet builds:
- Log/event search architecture
- Data streams and ILM for observability
- Security analytics and operational tradeoffs

---

## 1. Use Cases

- search application logs
- count errors over time
- correlate trace IDs
- security event investigation
- dashboard service health
- alert on suspicious activity

---

## 2. Document Shape

```json
{
  "@timestamp": "2026-07-01T10:00:00Z",
  "service": "checkout",
  "level": "ERROR",
  "trace_id": "abc123",
  "tenant_id": "t1",
  "message": "Payment authorization failed",
  "status_code": 502,
  "duration_ms": 842
}
```

---

## 3. Design Choices

- data streams for append-only logs/events
- ILM for hot/warm/cold/delete lifecycle
- keyword fields for service, level, trace ID, tenant
- text field for message
- date histogram aggregations for dashboards
- snapshots for retention/DR requirements

---

## 4. Risks

- high ingest volume can overwhelm indexing
- mapping explosion from arbitrary log fields
- expensive dashboards can hurt user search
- retention cost grows fast
- PII leakage in logs creates security risk

---

## 5. Strong Answer

```text
For logs, I would use data streams with `@timestamp`, controlled mappings, and ILM. Logs are append-only, so rollover and lifecycle tiers keep shard sizes and retention bounded. I would index structured fields as keywords/numerics and messages as text. Dashboards use date histograms and terms aggregations, with guardrails on high-cardinality fields. I would monitor ingest lag, rejected writes, disk watermarks, and query latency.
```

---

## 6. Revision Notes

- One-line summary: Logs need data streams, controlled mappings, lifecycle, and cost discipline.
- Three keywords: data streams, ILM, mapping explosion.
- One interview trap: unlimited log retention in hot storage.
- Memory trick: observability data grows every second, so lifecycle is the design.