# AWS Database: RDS and Aurora Gold Sheet

> Track: AWS Interview Track — Storage and Database
> Goal: choose the right relational database option, design for high availability, and explain Aurora's architecture advantages over standard RDS.

---

## 0. How To Read This

Beginner focus:
- RDS engines, Multi-AZ vs single-AZ
- Read replicas
- Automated backups and snapshots

Intermediate focus:
- RDS vs Aurora comparison
- Aurora storage layer architecture
- Connection pooling (RDS Proxy)
- Parameter groups, option groups
- Encryption, IAM authentication

Senior / MAANG focus:
- Aurora Serverless v2 scaling behavior
- Aurora Global Database RPO/RTO
- Aurora storage volume durability model
- Performance Insights and wait event analysis
- RDS Proxy for Lambda connection pooling
- Maintenance windows and zero-downtime upgrades
- Aurora custom endpoints and reader endpoint routing

---

# Topic 1: Amazon RDS

## 1. Intuition

RDS is a managed relational database service. AWS handles provisioning, patching, backups, and multi-AZ failover. You manage the schema, queries, and application logic.

Supported engines:
- MySQL
- PostgreSQL
- MariaDB
- Oracle
- SQL Server
- Aurora MySQL / Aurora PostgreSQL (different service, covered separately)

## 2. RDS vs Self-Managed Database On EC2

| Feature | RDS | Self-Managed EC2 |
|---|---|---|
| OS/patch management | AWS | you |
| Storage auto-scaling | yes (up to 65 TB) | manual |
| Multi-AZ failover | built-in (click) | build yourself |
| Automated backups | yes | build yourself |
| Read replicas | built-in | build yourself |
| Custom OS config | no | yes |
| Cost | higher per CPU | potentially lower (with ops overhead) |

Interview line:

```text
RDS is almost always the right choice for relational workloads on AWS. Self-managed on EC2 is
justified only when you need a database engine or version not supported by RDS, need custom
OS-level config, or have unique licensing requirements.
```

## 3. Multi-AZ vs Single-AZ

| Feature | Single-AZ | Multi-AZ |
|---|---|---|
| Standby instance | no | yes (synchronous replication) |
| Automatic failover | no | yes (~30-60 seconds) |
| Standby serves reads | no (standby is passive) | no (only for failover) |
| Cost | lower | ~2x |
| Use case | dev/test, non-critical | all production workloads |

Important trap:

```text
Multi-AZ standby does NOT serve read traffic. It is a hot standby for failover only.
For read scaling, use Read Replicas.
```

## 4. Read Replicas

Read replicas use asynchronous replication from the primary:

```text
Use for:
- offload read-heavy workloads (reporting, analytics)
- geographic distribution (create cross-region replica)
- promote to standalone primary if needed

Replication lag:
- usually seconds, but can be higher under heavy write load
- do not use for reads requiring strong consistency
```

Promotion:
- promote a read replica to a standalone primary (irrevocable, no replication back)
- used for migration or DR

Cross-region read replica:
- provides data in another region for DR or low-latency reads
- replication is asynchronous across regions
- can be promoted if primary region fails (manual process)

## 5. RDS Proxy

RDS Proxy maintains a pool of connections to RDS and shares them across Lambda/app connections:

```text
Problem: Lambda can have thousands of concurrent invocations, each opening a DB connection.
RDS has a connection limit (max_connections).
Without proxy: connection exhaustion or OOMKilled.

RDS Proxy:
- maintains a warm pool of long-lived connections to RDS
- Lambda connects to Proxy endpoint (fast, multiplexed)
- Proxy multiplexes many app connections into fewer DB connections
```

Use RDS Proxy when:
- Lambda connects to RDS
- Any app with connection churn (microservices, serverless)
- Connection count is approaching RDS limits

## 6. RDS Encryption And Security

Encryption at rest:
- enabled at creation time using KMS (cannot be enabled post-creation without snapshot restore)
- encrypts storage, automated backups, snapshots, read replicas

Encryption in transit:
- SSL/TLS for client-to-RDS connections
- enforce with `rds.force_ssl=1` parameter group setting

IAM database authentication:
- authenticate to MySQL/PostgreSQL RDS using IAM tokens instead of username/password
- token is valid 15 minutes, automatically rotated by IAM
- no static DB password in application

RDS database in private subnet:
- NEVER expose RDS publicly
- security group: allow only app servers / Lambda on specific port

## 7. Automated Backups And Snapshots

| Type | What | Retention | Restore |
|---|---|---|---|
| Automated backups | daily full + continuous transaction log | 1-35 days | point-in-time to any second |
| Manual snapshot | user-triggered, any time | indefinite | restore to new instance |
| Snapshot copy | cross-region copy of snapshot | indefinite | DR and region migration |

Point-in-time restore:
- restore to any second within backup retention window
- creates a new DB instance (not in-place)

## 8. RDS Storage Auto Scaling

Enable storage auto scaling to avoid running out of disk:

```text
Set maximum storage threshold (e.g., 1 TB)
RDS auto-scales in 10 GB increments when:
  - free storage < 10% for 5 continuous minutes
  - or < 10 GB free
  - 6 hours since last auto-scale

Storage can only increase, not decrease.
```

---

# Topic 2: Amazon Aurora

## 1. What Makes Aurora Different

Aurora is AWS's cloud-native relational database engine, API-compatible with MySQL and PostgreSQL, but built differently.

The key architectural difference:

```text
Standard RDS:
  Primary instance + EBS volume
  Replica syncs from primary across network
  Replica failover = few minutes (promote standby)

Aurora:
  Storage is a distributed, fault-tolerant cluster volume (6 copies across 3 AZs)
  Primary + up to 15 read replicas all READ from the SAME storage volume
  No replication lag for reads (replicas read the same pages, nearly synchronous)
  Replica failover = ~30 seconds (flip endpoint, no storage sync)
```

## 2. Aurora Storage Architecture

Aurora separates compute from storage:

```text
Aurora Storage Volume:
- spans 3 AZs, 2 copies per AZ = 6 total copies
- tolerates loss of 2 copies for reads, 3 for writes
- auto-heals corrupted data
- grows automatically in 10 GB increments up to 128 TB
- shared by primary and all replicas (no per-instance storage)
```

Benefits:
- faster failover (no storage sync needed)
- read replicas have near-zero replication lag
- storage is more durable than a single EBS volume
- cheaper (pay for actual storage used, not pre-provisioned volume)

## 3. Aurora vs RDS Comparison

| Feature | RDS MySQL/PostgreSQL | Aurora |
|---|---|---|
| Storage | per-instance EBS | shared distributed volume |
| Read replicas | async, can lag | near-synchronous reads from shared storage |
| Failover | 1-2 minutes | ~30 seconds |
| Read endpoints | each replica has own endpoint | Aurora provides cluster reader endpoint (round-robin) |
| Max replicas | 5 | 15 |
| Auto storage grow | yes (limited) | yes (up to 128 TB automatically) |
| Cost | lower | 20-30% higher, often worth it for availability |
| Compatibility | native engine | MySQL 8.x / PostgreSQL 15.x compatible |

## 4. Aurora Endpoints

| Endpoint | Routes To | Use Case |
|---|---|---|
| Cluster Endpoint | current primary instance | write operations |
| Reader Endpoint | load-balanced across all replicas | read-only queries |
| Instance Endpoints | specific instance | maintenance, targeted troubleshooting |
| Custom Endpoints | subset of instances you define | analytics on dedicated replicas |

Pattern: 

```text
Write path: app -> cluster endpoint (primary)
Read path: app -> reader endpoint (all replicas, round-robin)
Analytics: app -> custom endpoint (dedicated analytics replicas with larger instance type)
```

## 5. Aurora Serverless V2

Aurora Serverless v2 scales Aurora compute capacity in fine-grained ACU (Aurora Capacity Units) increments:

```text
Scale from: 0.5 ACU (minimum)
Scale to: 128 ACU (maximum)
Granularity: 0.5 ACU increments
Scale speed: seconds, not minutes

1 ACU = ~2 GB RAM + proportional CPU

Billing: per ACU-second (only pay for what you use)
```

Use Aurora Serverless v2 when:
- variable or unpredictable database load
- dev/test environments (scale to 0 after idle period)
- bursty traffic patterns
- want to avoid over-provisioning

Caution:
- Serverless v2 does not scale to zero by default (0.5 ACU minimum active)
- Serverless v1 scaled to zero but had warm-up delay (deprecated in favor of v2)

## 6. Aurora Global Database

Multi-region Aurora cluster:

```text
Primary Region: write operations, 1 primary cluster
Secondary Regions: up to 5 read-only regions

Replication lag: < 1 second (storage-level physical replication)
RPO: < 1 second
RTO: < 1 minute (promote secondary to primary)

Use for:
- global low-latency reads
- cross-region DR with RPO/RTO better than standard CRR
```

## 7. Performance Insights

Performance Insights shows database load by:
- Wait events (what is the database waiting for?)
- SQL statements (which queries are slowest?)
- Hosts, users, applications

Key wait events to watch:
- `io/table/sql/handler` — table locks, index issues
- `db/cpu` — CPU-bound queries
- `io/file/innodb/innodb_log_file` — redo log writes
- `lock/table/sql/handler` — contention

## 8. RDS Proxy For Aurora

Same as RDS, RDS Proxy is critical for Lambda → Aurora connections:

```text
Lambda concurrency surge -> all connect to RDS Proxy
RDS Proxy maintains connection pool to Aurora cluster
Aurora only sees a small number of long-lived Proxy connections
```

## 9. Common Mistakes

| Mistake | Better Approach |
|---|---|
| RDS in public subnet | put RDS in private data subnet, security group from app only |
|Use Multi-AZ standby for read traffic | standby is for failover only; use read replicas |
| Lambda opens new DB connection per invocation | use RDS Proxy to multiplex connections |
| Aurora reader endpoint for analytics hitting all replicas | use custom endpoint with dedicated analytics replicas |
| Enable encryption after creation | enable at creation; post-creation requires snapshot restore |
| No Performance Insights | enable Performance Insights for production query analysis |
| Max_connections set to default | size max_connections per instance class; use RDS Proxy to avoid exhaustion |

## 10. Interview Scenario

**Scenario**: "When would you choose Aurora over RDS MySQL?"

Strong answer:

```text
I choose Aurora when:
1. High availability matters: Aurora failover takes ~30 seconds vs 1-2 minutes for RDS.
2. Read scaling matters: up to 15 replicas with near-zero lag vs 5 for RDS with async lag.
3. Variable load: Aurora Serverless v2 scales in 0.5 ACU increments without node bouncing.
4. Long-term cost: Aurora's pay-for-storage model is cheaper than pre-provisioned EBS at scale.
5. Global distribution: Aurora Global Database gives <1s replication lag across regions.

I would choose RDS MySQL when the workload is simple, cost is critical, or the team needs
a specific MySQL/PostgreSQL feature not yet available on the Aurora-compatible version.
```

## 11. Revision Notes

- RDS Multi-AZ: synchronous standby, automatic failover, NOT for reads
- Read Replicas: async, for read scaling, can promote; cross-region for DR
- RDS Proxy: critical for Lambda → RDS, prevents connection exhaustion
- Aurora storage: 6 copies across 3 AZs, shared by primary + replicas, auto-grows
- Aurora failover: ~30 seconds vs RDS ~60-120 seconds
- Aurora Serverless v2: per-ACU-second billing, scales in 0.5 ACU steps
- Aurora Global DB: RPO < 1 second, RTO < 1 minute for cross-region DR
- Always private subnet, always encryption at creation, always Performance Insights

## 12. Official Source Notes

- RDS: <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Welcome.html>
- Aurora: <https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/CHAP_AuroraOverview.html>
- Aurora Serverless v2: <https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-serverless-v2.html>
- Aurora Global Database: <https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database.html>
- RDS Proxy: <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy.html>
