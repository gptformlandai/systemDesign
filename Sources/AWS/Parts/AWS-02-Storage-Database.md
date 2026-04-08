# AWS Interview Notes - Part 2: Storage + Database

> Covers: S3, EBS, EFS, RDS, DynamoDB, Aurora, ElastiCache. This is the part interviewers use to test whether you understand persistence, scale, durability, consistency, performance bottlenecks, and cost trade-offs.

---

# Table of Contents

1. [How to Think About AWS Storage](#1-how-to-think-about-aws-storage)
2. [S3](#2-s3)
3. [EBS](#3-ebs)
4. [EFS](#4-efs)
5. [RDS](#5-rds)
6. [Aurora](#6-aurora)
7. [DynamoDB](#7-dynamodb)
8. [ElastiCache](#8-elasticache)
9. [High-Value Comparisons](#9-high-value-comparisons)
10. [Architecture Patterns](#10-architecture-patterns)
11. [Common Interview Traps](#11-common-interview-traps)
12. [Rapid Revision Sheet](#12-rapid-revision-sheet)

---

# 1. How to Think About AWS Storage

Core interview question:

- Is this block storage, file storage, or object storage?
- Is the access pattern transactional, analytical, cache-heavy, or append-only?
- Do we need SQL joins and strict schema, or massive scale with simple access patterns?
- Do we need low latency, high durability, shared access, or cheap archival?

Simple mental map:

| Need | Service |
|---|---|
| object storage | S3 |
| block volume for one instance | EBS |
| shared file system | EFS |
| relational DB | RDS / Aurora |
| NoSQL key-value/document | DynamoDB |
| in-memory cache | ElastiCache |

---

# 2. S3

## 2.1 What It Is

Amazon S3 is durable object storage. You store objects in buckets, not files in directories and not blocks on disks.

Key characteristics:

- virtually unlimited scale
- very high durability
- not a block device
- not a POSIX file system

## 2.2 Best Use Cases

- static website assets
- backups
- logs
- data lake storage
- media
- document storage
- event-driven processing with object-created notifications

## 2.3 Important Concepts

- Bucket
- Object
- Key
- Versioning
- Lifecycle rules
- Storage classes
- Pre-signed URLs
- Multipart upload

## 2.4 Storage Classes

- Standard: frequent access
- Standard-IA: infrequent access
- One Zone-IA: lower cost, lower resilience
- Glacier tiers: archive

Interview angle:

Know lifecycle transitions and retrieval trade-offs.

## 2.5 Security

- bucket policies
- IAM policies
- Block Public Access
- SSE-S3 / SSE-KMS
- pre-signed URLs for controlled client upload/download

## 2.6 S3 Performance and Design

- use prefix design only when necessary for organization; S3 scales well
- use multipart upload for large files
- do not use S3 like a low-latency transactional database

## 2.7 Interviewer Favorites

- Why S3 instead of EFS?
- How do you securely let users upload files?
- How do lifecycle policies reduce cost?
- How do you serve private content globally?

## 2.8 Strong Answer

```
For user-uploaded documents, I would store files in S3, keep metadata in a database, expose uploads using pre-signed URLs, enable versioning, encrypt at rest, and serve downloads through CloudFront if low-latency global access matters.
```

---

# 3. EBS

## 3.1 What It Is

Amazon EBS is network-attached block storage for EC2.

Think:

- behaves like a disk volume
- good for databases and file systems running on one instance
- tied to an AZ

## 3.2 Best Use Cases

- boot volumes
- relational DB on EC2
- application data requiring block semantics
- transactional workloads

## 3.3 Important Properties

- persistent beyond instance stop/start
- snapshots to S3
- provisioned capacity and performance characteristics
- typically attached to one instance at a time

## 3.4 Volume Types

- `gp3`: general purpose SSD, common default
- `io1/io2`: high IOPS, critical databases
- `st1/sc1`: throughput-optimized HDD / cold HDD for niche cases

## 3.5 Interviewer Angle

- EBS is not shared file storage like EFS
- EBS is AZ-scoped
- snapshot strategy matters for backup and restore

---

# 4. EFS

## 4.1 What It Is

Amazon EFS is managed shared file storage for Linux workloads.

Think:

- NFS-style shared file system
- multiple instances can mount it
- elastic capacity

## 4.2 Best Use Cases

- shared content repositories
- ML/data processing needing shared files
- legacy apps needing shared filesystem semantics
- container workloads needing RWX-style storage

## 4.3 Trade-off

EFS is easier for shared access, but usually slower and more expensive than local or block storage for some patterns.

## 4.4 Interviewer Comparison

| Service | Access model | Typical fit |
|---|---|---|
| EBS | block, usually single-instance | DB or single-instance filesystem |
| EFS | shared file system | multi-instance shared files |
| S3 | object | assets, backup, data lake |

---

# 5. RDS

## 5.1 What It Is

Amazon RDS is managed relational database service. AWS handles backups, patching, monitoring hooks, and failover features for supported engines.

Common engines:

- MySQL
- PostgreSQL
- MariaDB
- Oracle
- SQL Server

## 5.2 When to Use It

Use RDS when:

- you need SQL
- you need ACID transactions
- joins matter
- schema is relational
- operational overhead should be lower than self-managed DBs

## 5.3 Multi-AZ

Multi-AZ means:

- synchronous standby replica in another AZ
- failover for high availability
- not primarily a read-scaling feature

This distinction is asked often.

## 5.4 Read Replicas

Read replicas are for:

- read scaling
- offloading analytics/reporting style queries
- sometimes disaster recovery patterns

Read replicas are not the same as Multi-AZ standby.

## 5.5 Backups

- automated backups with retention
- manual snapshots
- point-in-time recovery

## 5.6 Interviewer Hot Points

- Multi-AZ vs read replica
- why RDS over DynamoDB
- what happens during failover
- how to tune connections and indexes

## 5.7 Strong Answer

```
For an order management system requiring transactions, joins, and referential integrity, I would start with RDS PostgreSQL in Multi-AZ mode, add read replicas only if read traffic justifies it, and use connection pooling plus caching before scaling blindly.
```

---

# 6. Aurora

## 6.1 What It Is

Aurora is AWS's cloud-optimized relational database compatible with MySQL and PostgreSQL.

It separates compute and storage more aggressively than standard RDS engines and is built for higher performance and managed resilience.

## 6.2 Why Aurora Exists

It addresses common pain points of traditional relational databases in the cloud:

- better scalability
- faster failover
- managed storage replication
- improved performance profile

## 6.3 Key Properties

- storage auto-scales
- six-way storage replication across three AZs
- reader endpoints for replicas
- writer endpoint for primary

## 6.4 Aurora vs RDS

Aurora is still under the RDS family but has a different architecture and operational profile.

Interview-safe summary:

```
Choose Aurora when you want managed relational databases with stronger scalability and availability characteristics than standard RDS engines, and your cost/engine compatibility trade-offs make sense.
```

## 6.5 Aurora Interview Traps

- saying Aurora is "just RDS MySQL"
- confusing reader endpoint with HA standby
- not discussing cost

---

# 7. DynamoDB

## 7.1 What It Is

Amazon DynamoDB is a fully managed NoSQL database optimized for key-value and document workloads with very high scale and low-latency access.

## 7.2 Best Use Cases

- session stores
- user profiles
- shopping carts
- event metadata
- high-scale request-driven systems
- workloads where access patterns are known up front

## 7.3 Data Modeling Mindset

This is the interview separator.

In relational design you model entities first.
In DynamoDB you model access patterns first.

Ask:

- What queries must be fast?
- What is the partition key?
- Do I need sort-key range queries?
- Do I need GSIs?

## 7.4 Core Concepts

- partition key
- sort key
- item
- GSI
- LSI
- provisioned vs on-demand capacity
- TTL
- streams

## 7.5 Partition Key Design

Bad partition key design creates hot partitions.

Senior candidates mention:

- write distribution
- traffic skew
- sharding keys if necessary

## 7.6 Consistency

- eventual consistency by default for many reads
- strongly consistent reads available in some cases

## 7.7 DynamoDB Strengths

- scale
- predictable low latency
- fully managed
- no server patching

## 7.8 DynamoDB Weaknesses

- no rich joins
- data modeling is harder if access patterns are unclear
- ad hoc querying is limited compared to SQL

## 7.9 Interviewer Favorites

- RDS vs DynamoDB
- how to avoid hot partitions
- when to use GSIs
- how streams help event-driven architecture

---

# 8. ElastiCache

## 8.1 What It Is

ElastiCache is managed in-memory caching, commonly Redis or Memcached.

## 8.2 Use Cases

- read caching
- session storage
- rate limiting
- leaderboard/ranking
- distributed locks with care
- pub/sub or lightweight ephemeral coordination in some designs

## 8.3 Redis vs Memcached

- Redis: richer data structures, persistence options, more common
- Memcached: simpler distributed cache, less feature-rich

## 8.4 Caching Patterns

- cache-aside
- write-through
- write-behind

Interviewers usually want you to know cache-aside.

## 8.5 Cache Risks

- stale data
- cache stampede
- inconsistent invalidation
- overusing cache to hide bad schema/query design

## 8.6 Strong Answer

```
I use ElastiCache to reduce database read pressure for hot keys and expensive queries, but I treat cache invalidation and TTL strategy as first-class design concerns because cache consistency bugs are application bugs.
```

---

# 9. Redshift (Surendra will likely probe this)

## 9.1 What It Is

Amazon Redshift is a fully managed data warehouse optimized for analytical queries on large datasets.

Key characteristics:

- columnar storage
- massively parallel processing (MPP)
- SQL-based analytics
- optimized for OLAP, not OLTP

## 9.2 When to Use Redshift

```
Use Redshift when:
  ✓ You need to analyze terabytes/petabytes of data
  ✓ Queries are analytical (aggregations, joins across large tables)
  ✓ Workload is batch/BI reports, not real-time transactions
  ✓ You need SQL interface for analysts/BI tools

Do NOT use Redshift when:
  ✗ You need sub-millisecond transactional responses
  ✗ Data is small (< 100GB) — RDS is simpler
  ✗ Access pattern is key-value lookups
```

## 9.3 Redshift vs RDS vs DynamoDB

| Aspect | RDS | DynamoDB | Redshift |
|---|---|---|---|
| Workload | OLTP | Key-value/Document | OLAP |
| Query style | Transactional SQL | Access by key | Analytical SQL |
| Scale | Vertical + Read Replicas | Horizontal | MPP cluster |
| Latency | Low (milliseconds) | Very low | Higher (seconds+) |
| Best for | App backend | High-scale lookups | BI/Analytics |

## 9.4 Key Concepts

- **Cluster**: leader node + compute nodes
- **Distribution style**: how data is spread across nodes (KEY, EVEN, ALL)
- **Sort keys**: optimize range queries
- **Redshift Spectrum**: query S3 data directly without loading
- **Materialized views**: pre-computed results for expensive queries
- **Concurrency Scaling**: auto-add capacity for burst queries

## 9.5 Distribution Style Decision

```
KEY distribution:
  When you frequently join on a column, distribute by that column
  Both tables joined will have matching rows on same node

EVEN distribution:
  Default. Spreads data evenly. Good when no clear join pattern.

ALL distribution:
  Copies entire table to every node. Only for small dimension tables.
```

## 9.6 Interview Gold Answer

```
"For our BI reporting layer, I'd use Redshift as the analytics warehouse.
Transactional data flows from RDS/DynamoDB via ETL jobs into Redshift.
I'd design distribution keys based on common join patterns, use sort keys
for date-range queries, and leverage Redshift Spectrum for cold data in S3
without loading it into the cluster."
```

---

# 10. Athena

## 10.1 What It Is

Amazon Athena is a serverless query service that runs SQL directly against data in S3.

No infrastructure to manage. Pay per query.

## 10.2 When to Use Athena

```
Use Athena when:
  ✓ Ad-hoc queries on S3 data lake
  ✓ Log analysis (CloudTrail, ALB logs, VPC Flow Logs)
  ✓ Exploratory analytics without loading data
  ✓ Cost-sensitive — no standing infrastructure

Use Redshift instead when:
  ✗ Complex, repeated queries with many joins
  ✗ Sub-second dashboard performance needed
  ✗ High concurrency requirements
```

## 10.3 Data Formats

Athena works best with columnar formats:

- **Parquet**: columnar, compressed, best performance
- **ORC**: columnar, Hive-optimized
- JSON, CSV: supported but slower and costlier

## 10.4 Partitioning

Critical for performance and cost:

```
s3://my-bucket/logs/year=2026/month=03/day=30/

Query with partition filter:
  SELECT * FROM logs WHERE year='2026' AND month='03'
  -> Only scans that folder, not entire bucket
```

## 10.5 Athena vs Redshift

| Aspect | Athena | Redshift |
|---|---|---|
| Infrastructure | Serverless | Cluster |
| Pricing | Per TB scanned | Hourly + storage |
| Latency | Seconds-minutes | Faster for complex | 
| Best for | Ad-hoc, infrequent | Repeated, complex |
| Concurrency | Limited | Better with scaling |

## 10.6 Interview Answer

```
"For ad-hoc analysis of CloudTrail logs, I'd use Athena directly on S3.
But for production dashboards with complex joins, I'd load data into
Redshift where I can optimize distribution and sort keys."
```

---

# 11. RDS Proxy

## 11.1 What It Is

RDS Proxy is a managed database proxy that sits between your application and RDS/Aurora.

## 11.2 Why It Exists

```
Problem: Lambda + RDS = connection explosion
  Each Lambda invocation opens a new DB connection
  100 concurrent Lambdas = 100 connections
  RDS has connection limits (varies by instance size)
  Result: connection exhaustion, errors

Solution: RDS Proxy
  Proxy maintains a connection pool to the database
  Lambda connects to proxy (fast, reuses connections)
  Proxy multiplexes to actual DB connections
```

## 11.3 Benefits

- **Connection pooling**: reduces DB connection pressure
- **Failover handling**: faster failover, app doesn't need reconnect logic
- **IAM authentication**: integrate with IRSA/IAM for credential-free access
- **TLS enforcement**: can enforce encrypted connections

## 11.4 When to Use

```
✓ Serverless + RDS (Lambda, Fargate with many short tasks)
✓ High connection churn applications
✓ Want IAM-based DB authentication
✓ Need faster failover during Multi-AZ switch

✗ Overkill for low-connection steady-state apps on EC2/ECS
```

## 11.5 Interview Gold Answer

```
"For Lambda functions accessing RDS, I'd put RDS Proxy in front to solve
connection pooling. Without it, we'd hit connection limits under load.
Proxy also speeds up Multi-AZ failover and supports IAM authentication."
```

---

# 12. Data Lake Architecture (High-Value for BI Interviews)

## 12.1 What Is a Data Lake

A data lake is a centralized repository for storing structured and unstructured data at any scale, typically on S3.

## 12.2 Modern AWS Data Lake Pattern

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DATA SOURCES                                │
│  RDS | DynamoDB | Kinesis | APIs | SaaS | On-prem | IoT            │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         INGESTION                                   │
│  AWS Glue | Kinesis Firehose | DMS | Lambda | Step Functions        │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    S3 DATA LAKE (ZONES)                             │
│                                                                     │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐               │
│  │   RAW       │   │  PROCESSED  │   │  CURATED    │               │
│  │  (Bronze)   │──►│   (Silver)  │──►│   (Gold)    │               │
│  │  as-is data │   │  cleaned    │   │  analytics  │               │
│  └─────────────┘   └─────────────┘   └─────────────┘               │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        CONSUMPTION                                  │
│  Athena (ad-hoc) | Redshift (BI) | QuickSight | SageMaker (ML)     │
└─────────────────────────────────────────────────────────────────────┘
```

## 12.3 Zone Definitions

```
Raw/Bronze Zone:
  ✦ Data as-is from sources
  ✦ No transformation
  ✦ Immutable, append-only
  ✦ For audit trail and reprocessing

Processed/Silver Zone:
  ✦ Cleaned, validated, normalized
  ✦ Schema enforced
  ✦ Format converted (JSON → Parquet)
  ✦ PII masked if needed

Curated/Gold Zone:
  ✦ Business-ready aggregations
  ✦ Pre-joined, denormalized for analytics
  ✦ Exposed to BI tools
```

## 12.4 AWS Glue Role

- **Glue Crawler**: auto-discover schema, populate Glue Data Catalog
- **Glue Data Catalog**: metadata store (like Hive metastore)
- **Glue ETL**: serverless Spark jobs for transformation
- **Glue Studio**: visual ETL designer

## 12.5 Interview Gold Answer

```
"For our analytics platform, I'd design a data lake on S3 with bronze/silver/gold
zones. Raw data lands in bronze, Glue ETL transforms to Parquet in silver,
and business aggregations go to gold. Athena queries silver for exploration,
Redshift Spectrum or direct load for production BI dashboards."
```

---

# 13. High-Value Comparisons

## 9.1 S3 vs EBS vs EFS

| Service | Type | Shared? | Typical fit |
|---|---|---|---|
| S3 | object | via API, not mounted disk semantics | assets, backups, logs |
| EBS | block | typically one instance | database disks, boot volumes |
| EFS | file | yes | shared filesystem |

## 9.2 RDS vs Aurora vs DynamoDB

| Need | Best fit |
|---|---|
| SQL, joins, conventional relational workloads | RDS |
| relational plus stronger cloud-native scaling/resilience | Aurora |
| massive scale key-value/document with access-pattern design | DynamoDB |

## 9.3 ElastiCache vs Database

Cache is not your source of truth unless the system is explicitly designed that way.

Use cache to accelerate reads, absorb bursts, and reduce DB load.

---

# 10. Architecture Patterns

## 10.1 File Upload Platform

```
Client
  ->
API issues pre-signed URL
  ->
S3 stores object
  ->
metadata stored in RDS/DynamoDB
  ->
CloudFront serves downloads
```

## 10.2 Classic Transactional App

```
App tier
  ->
RDS PostgreSQL Multi-AZ
  ->
ElastiCache Redis for hot reads
  ->
S3 for documents and backups
```

## 10.3 Event-Driven High-Scale Platform

```
API / Lambda / services
  ->
DynamoDB for request-time lookups
  ->
S3 for immutable objects
  ->
ElastiCache for hot keys
```

---

# 11. Common Interview Traps

## Trap 1

"S3 is a file system."

Correct:

No. It is object storage with API access semantics.

## Trap 2

"RDS Multi-AZ scales reads."

Correct:

No. Multi-AZ primarily improves availability. Read replicas scale reads.

## Trap 3

"DynamoDB is always better because it scales more."

Correct:

Scale is not the only criterion. Query flexibility, transactions, schema relationships, and developer productivity matter.

## Trap 4

"Cache will fix a slow system."

Correct:

Sometimes. But poor data modeling, missing indexes, and bad query patterns must still be fixed.

## Trap 5

"EFS can replace S3 for everything."

Correct:

No. Shared filesystem and object store solve different problems.

---

# 16. Rapid Revision Sheet

## One-Line Definitions

- `S3`: durable object storage
- `EBS`: block storage for EC2
- `EFS`: shared managed file system
- `RDS`: managed relational database
- `Aurora`: cloud-optimized relational database in the RDS family
- `DynamoDB`: managed NoSQL key-value/document DB
- `ElastiCache`: managed in-memory caching
- `Redshift`: columnar data warehouse for analytics
- `Athena`: serverless SQL on S3
- `RDS Proxy`: connection pooling for serverless + RDS
- `Glue`: serverless ETL and data catalog

## Questions You Must Be Able to Answer

- Why RDS over DynamoDB?
- Why Aurora over standard RDS?
- Why S3 over EFS?
- When do read replicas help?
- How do you design a good DynamoDB partition key?
- When Redshift vs Athena?
- Why RDS Proxy for Lambda + RDS?
- What are data lake zones (bronze/silver/gold)?
- How does Glue fit in a data pipeline?

## Decision Quick Reference

```
Transactional SQL workload?            → RDS / Aurora
Key-value at massive scale?            → DynamoDB
Analytical warehouse?                  → Redshift
Ad-hoc queries on S3?                  → Athena
Lambda + RDS connection issues?        → RDS Proxy
ETL and data catalog?                  → Glue
Cache hot reads?                       → ElastiCache
```

## Gold Standard Sentence

```
My storage and database choices depend on data model (relational vs NoSQL vs object),
access pattern (transactional vs analytical vs key-based), scale requirements,
and whether the workload is OLTP or OLAP. For analytics, I design data lakes
with proper zones and use the right query engine for each use case.
```
- When should cache-aside be used?

## Gold Standard Sentence

```
I choose AWS persistence services by matching the data model, access pattern, consistency needs, latency target, and operational burden rather than by chasing whichever service scales the most on paper.
```

