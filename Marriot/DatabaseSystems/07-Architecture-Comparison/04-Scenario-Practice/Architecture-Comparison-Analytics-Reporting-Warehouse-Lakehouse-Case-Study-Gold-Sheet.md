# Architecture Comparison Analytics, Reporting, Warehouse, and Lakehouse Case Study - Gold Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: analytics/data platform interviews | Level: senior | Mode: OLTP vs OLAP

## 1. Workloads

- operational dashboard
- executive reporting
- ad hoc analytics
- ML feature generation
- historical retention
- batch and streaming pipelines

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| OLTP state | SQL/MongoDB/Cassandra by access pattern | application correctness |
| analytical queries | warehouse/lakehouse | columnar scans, joins, aggregations |
| raw historical data | object storage/data lake | cheap durable retention |
| streaming aggregates | stream processor plus OLAP store | near-real-time dashboards |
| search over logs/docs | Elasticsearch/OpenSearch | text search and filters |

---

## 3. OLTP vs OLAP

| OLTP | OLAP |
|---|---|
| low-latency transactions | large scans and aggregations |
| current operational state | historical analysis |
| normalized or access-pattern modeled | denormalized/star/columnar models |
| strict correctness path | reporting and decision support |

---

## 4. Production Risks

- running heavy reports on OLTP database
- stale dashboards without freshness labels
- schema drift in pipelines
- unbounded warehouse cost
- PII leakage into analytics
- no backfill/replay plan

---

## 5. Strong Interview Answer

```text
I would keep OLTP workloads in application databases and move analytical workloads into a warehouse or lakehouse through CDC, ETL, or event streams. Object storage can hold raw historical data cheaply, while the warehouse serves reporting and ad hoc analytics. The key concerns are freshness, schema evolution, cost controls, privacy, and backfill/replay.
```

---

## 6. Revision Notes

- One-line summary: Do not make OLTP databases carry heavy analytics forever.
- Three keywords: OLTP, OLAP, freshness.
- One trap: dashboards with no data freshness or lineage.