# Data Engineering Good-To-Know (Interview Guide for 7+ Years)

> Goal: Not deep implementation, but clear understanding of where each concept is useful, what problem it solves, and how to explain trade-offs in interviews.

---

## 1) ETL vs ELT

### ETL (Extract -> Transform -> Load)

You transform data before loading it into the target system.

Use ETL when:
- strict schema/quality is required before storage
- destination storage is expensive or has limited compute
- compliance requires filtering/masking before landing

Problems ETL solves:
- prevents bad/raw data from entering curated systems
- centralizes transformation logic in controlled pipelines

Trade-offs:
- slower onboarding of new data sources
- less flexibility for future unknown analytics

### ELT (Extract -> Load -> Transform)

You load raw data first, transform later in warehouse/lakehouse.

Use ELT when:
- you want fast ingestion of many sources
- cloud data platform has scalable compute (Snowflake/BigQuery/Databricks)
- teams want to reuse same raw data for different analytics

Problems ELT solves:
- faster time-to-ingest
- supports multiple downstream use cases from same raw data

Trade-offs:
- raw data governance becomes critical
- storage and compute cost control is mandatory

### 7+ Years interview framing

"In older on-prem stacks ETL was common due to compute/storage constraints. In cloud-native analytics, ELT is often preferred for agility. I choose based on compliance, latency, and cost model."

---

## 2) Batch vs Stream Processing

### Batch processing

Data is processed in chunks at intervals (every 15 min/hour/day).

Typical tools:
- Spark jobs
- Databricks workflows
- scheduled Airflow pipelines

Use batch when:
- reports are periodic, not real-time
- volume is high but latency requirement is relaxed
- reprocessing historical windows is common

Problems batch solves:
- cost-efficient large-scale transformations
- deterministic backfills and reconciliation

### Stream processing

Data is processed continuously as events arrive.

Typical tools:
- Kafka Streams
- Flink
- Kinesis + consumers

Use stream when:
- fraud detection, monitoring, personalization, alerts
- near real-time dashboards
- event-driven architecture needs low latency

Problems stream solves:
- reduces decision latency from hours to seconds
- enables reactive systems

Trade-offs (important in interviews):
- stream adds operational complexity (ordering, retries, duplicates)
- exactly-once guarantees are harder and costlier

### 7+ Years interview framing

"I use batch by default when business SLA allows it; I use streaming only when the latency benefit justifies additional complexity and cost."

---

## 3) Change Data Capture (CDC)

CDC captures INSERT/UPDATE/DELETE changes from source databases and publishes them downstream.

Common flow:
- DB transaction log/binlog/WAL -> CDC connector (e.g., Debezium) -> Kafka/Kinesis -> lake/warehouse

Where CDC is handy:
- near real-time replication to analytics
- keeping search index/cache in sync
- reducing heavy full table extracts

Problems CDC solves:
- avoids full reloads for large tables
- lowers source DB read pressure
- improves freshness for downstream analytics

Key concerns:
- schema evolution
- out-of-order events
- delete handling and idempotency

### Interview line

"CDC is my go-to for scalable incremental ingestion when full extracts become expensive or too slow."

---

## 4) Data Lake vs Data Warehouse vs Lakehouse

### Data Lake

Stores raw/semi-structured/structured data cheaply (S3/ADLS/GCS).

Best for:
- raw ingestion at scale
- long-term historical retention
- data science exploration

Risk:
- can become a "data swamp" without governance/metadata

### Data Warehouse

Curated, structured analytics store optimized for BI SQL.

Best for:
- consistent metrics and dashboards
- governed business reporting
- high-performance SQL analytics

Risk:
- rigid modeling can slow new use cases

### Lakehouse

Combines lake storage economics with warehouse-like reliability and performance.

Best for:
- one platform for BI + ML + engineering
- ACID tables on lake (Delta/Iceberg/Hudi)
- reducing duplicated platforms

### Quick chooser

- Need cheapest raw landing zone: Lake
- Need governed BI and stable KPIs: Warehouse
- Need both with fewer silos: Lakehouse

---

## 5) Medallion Architecture (Bronze -> Silver -> Gold)

### Bronze (raw)

As-ingested data, minimal changes.

Purpose:
- traceability and replay
- source-of-truth copy

### Silver (cleaned/conformed)

Deduplicated, standardized, validated, joined core entities.

Purpose:
- reliable reusable datasets
- consistent definitions across teams

### Gold (business-ready)

Aggregated/domain-specific tables for dashboards and KPIs.

Purpose:
- fast business consumption
- simplified access for analysts

### Problems this pattern solves

- separates ingestion concerns from business logic
- improves data quality progressively
- supports auditability and reproducibility

### 7+ Years interview framing

"I keep transformations layered: Bronze for lineage, Silver for quality, Gold for KPI-ready consumption. This reduces coupling and improves trust in metrics."

---

## 6) How APIs Feed Lakes/Warehouses

Typical ingestion patterns:
- event APIs/webhooks -> message bus -> lake/warehouse
- periodic API pulls -> batch loads
- bulk exports (CSV/Parquet) -> object storage -> transform
- operational DB -> CDC -> analytics

Where each fits:
- webhook/events: low latency, incremental
- scheduled pulls: simple external integrations
- bulk export: legacy systems or vendor constraints

Common problems solved:
- integrating SaaS platforms (CRM, billing, support tools)
- centralizing fragmented operational data

Important 7+ Years point:

"API ingestion needs strong retry, backoff, idempotency, and schema-drift handling. Reliability matters more than just successful first load."

---

## 7) Spark/Databricks Awareness (What They Solve)

### Spark (engine)

Distributed compute engine for large-scale batch/stream processing.

What it solves:
- parallel processing over huge datasets
- large joins/aggregations that single-node systems cannot handle
- unified API for ETL and ML pipelines

### Databricks (platform)

Managed platform around Spark/lakehouse operations.

What it solves:
- managed cluster orchestration
- collaborative notebooks/workflows
- Delta Lake reliability features
- easier productionization vs self-managing Spark infra

When handy:
- enterprise-scale pipelines
- multi-team data platform standardization

Interview phrase:

"Spark is the distributed processing engine; Databricks is the managed platform that reduces operational burden and speeds delivery."

---

## 8) FinOps Basics (Cloud Cost Optimization)

FinOps is operating cloud spend with engineering + finance accountability.

### Core principles

- visibility: clear cost by team/product/pipeline
- optimization: right-size compute/storage
- accountability: owners for spend and unit economics

### Data-platform cost levers

- use tiered storage lifecycle policies
- optimize file sizes/partitioning to reduce scan cost
- avoid over-clustering/always-on compute
- schedule jobs for off-peak where possible
- enforce auto-termination for dev clusters
- use incremental processing instead of full refresh
- prune stale tables and duplicate datasets

### Metrics leaders care about

- cost per TB processed
- cost per dashboard/query family
- cost per data product/domain
- freshness SLA achieved per dollar spent

### Interview framing (very useful)

"I balance performance with cost by defaulting to incremental pipelines, enforcing storage lifecycle policies, and tagging spend by domain so optimization decisions are measurable."

---

## 9) Decision Matrix (Fast Interview Answers)

### ETL or ELT?
- regulated sensitive data before landing -> ETL
- cloud analytics agility and reuse -> ELT

### Batch or Stream?
- hourly/daily SLA -> Batch
- sub-minute SLA -> Stream

### Lake, Warehouse, Lakehouse?
- raw cheap storage -> Lake
- curated BI at scale -> Warehouse
- unified analytics + ML with open table formats -> Lakehouse

### Full load or CDC?
- small tables/simple pipelines -> Full load
- large operational tables with frequent changes -> CDC

---

## 10) End-to-End Story You Can Tell in Interview

"In a typical enterprise setup, operational systems emit data via APIs and CDC. Raw data lands in Bronze for replay and lineage. Silver standardizes and validates entities across domains. Gold serves business KPIs and BI models. We use batch for most domain reporting and introduce streaming only where low latency directly impacts business outcomes. Spark/Databricks handle scale and orchestration. FinOps practices ensure the platform remains cost-efficient by favoring incremental processing, storage lifecycle controls, and cost ownership per domain."

---

## 11) What to Say If Asked "Where Have You Used This?"

"I have used these patterns in cloud modernization and analytics platforms where we integrated APIs + CDC pipelines, layered data into Bronze/Silver/Gold, and delivered governed BI models. The key value was reducing data latency, improving trust in metrics, and controlling cloud costs through FinOps practices."

---

## 12) Last-Minute 60-Second Revision

- ETL: transform before load; ELT: load raw then transform.
- Batch: cheaper and simpler; Stream: lower latency, more complexity.
- CDC: incremental DB changes, avoids full reloads.
- Lake: raw cheap storage; Warehouse: curated BI; Lakehouse: both.
- Medallion: Bronze raw, Silver clean, Gold business.
- Spark scales compute; Databricks simplifies Spark operations.
- FinOps: visibility + optimization + accountability for cloud spend.
