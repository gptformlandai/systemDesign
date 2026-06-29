# AWS Senior Architecture: Multi-Region DR and Resilience Gold Sheet

> Track: AWS Interview Track — Senior Architecture
> Goal: design resilient multi-region architectures, choose the right DR strategy for given RTO/RPO requirements, and articulate trade-offs at MAANG depth.

---

## 0. How To Read This

Beginner focus:
- RTO and RPO definitions
- Backup and restore basics
- Pilot light concept

Intermediate focus:
- Warm standby pattern
- Aurora Global Database for DR
- Route 53 failover routing
- DynamoDB global tables

Senior / MAANG focus:
- Active-active vs active-passive trade-offs
- RPO/RTO for each AWS service
- Cross-region dependency mapping
- Chaos engineering for resilience validation
- Global Accelerator for network-level HA
- Cost vs resilience optimization
- Regional service availability failures (not just instance failures)

---

# Topic 1: RPO, RTO, and DR Tiers

## 1. Definitions

| Term | Meaning | Example |
|---|---|---|
| RTO | Recovery Time Objective: how long until the system is back online | RTO = 30 minutes |
| RPO | Recovery Point Objective: how much data can you lose | RPO = 5 minutes (lose up to 5 minutes of data) |
| MTTR | Mean Time to Recover | average time across incidents |
| MTBF | Mean Time Between Failures | frequency of incidents |

Interview tip:

```text
RTO = time = downtime tolerance
RPO = data = data loss tolerance

Lower RTO and RPO = more expensive architecture.
The business defines requirements; the architect picks the pattern.
```

## 2. Four DR Strategies

| Strategy | RTO | RPO | Cost | Description |
|---|---|---|---|---|
| Backup & Restore | hours | hours | lowest | restore from backups in new region |
| Pilot Light | 10-30 min | minutes | low | minimal standby runs critical components |
| Warm Standby | minutes | seconds | medium | scaled-down but functional copy |
| Active-Active | seconds | zero | highest | traffic distributed, no failover needed |

## 3. Backup and Restore

Lowest cost DR tier:

```text
What runs in DR region:
  - Nothing (zero cost when not in failure)
  - S3 CRR copies data to DR region
  - RDS automated snapshots copied to DR region
  - EBS snapshots copied to DR region
  - AMIs copied to DR region

On disaster:
  1. Restore RDS from latest snapshot (~30 min)
  2. Launch EC2 from copied AMI
  3. Update Route 53 to DR region
  4. Manual configuration and validation

Use for: dev/test workloads, data archiving, non-critical internal tools
```

## 4. Pilot Light

Critical data replicated, minimal infrastructure pre-deployed:

```text
What runs in DR region:
  - Aurora Global Database (passive secondary cluster)
  - EC2 AMIs ready (not running)
  - ECS task definitions ready (no running tasks)
  - Route 53 failover record (health check on primary)

On disaster:
  1. Promote Aurora secondary to primary (~1 minute)
  2. Scale up EC2/ECS to full capacity (5-10 minutes)
  3. Route 53 health check fails primary -> routes to DR (DNS TTL minutes)

Use for: internal business apps, non-public-facing services
```

## 5. Warm Standby

Fully functional but scaled down, ready to scale:

```text
What runs in DR region:
  - Aurora Global Database (passive secondary, can handle reads)
  - ECS service running with 1 task (scaled down from production 10)
  - ALB ready
  - Route 53 failover record

On disaster:
  1. Promote Aurora secondary (~1 minute)
  2. Scale ECS service from 1 → 10 tasks (~2-3 minutes)
  3. Route 53 fails over automatically via health check

RTO: 2-5 minutes
RPO: seconds (Aurora Global replication < 1 second)

Use for: customer-facing applications, SLA-driven services
```

## 6. Active-Active Multi-Region

Traffic routed to multiple regions simultaneously:

```text
Production:
  Route 53 latency routing: 50% → us-east-1, 50% → eu-west-1
  (or weighted routing for even split)

Database:
  DynamoDB Global Tables (multi-master, any region can write)
  Aurora Global Database (writes to one primary, route reads to nearest)

Sessions:
  ElastiCache Global Datastore OR stateless sessions (JWT)

On single region failure:
  Route 53 health check fails unhealthy region
  All traffic routes to healthy region (no manual steps)
  No "failover" — region simply leaves rotation

RTO: seconds (DNS failover)
RPO: near-zero for DynamoDB; <1s for Aurora Global

Use for: highest-tier services, global consumer apps, financial systems
```

---

# Topic 2: AWS Services For Multi-Region

## 1. Aurora Global Database

```text
Primary region: read + write
Secondary regions (up to 5): read-only

Replication: storage-level, physical replication
Lag: < 1 second (typically milliseconds)
RPO: < 1 second
RTO: < 1 minute (managed failover or manual promotion)

Managed Failover (Aurora 3.x):
  aws rds failover-global-cluster \
    --global-cluster-identifier my-global-cluster \
    --target-db-cluster-identifier my-secondary-cluster
  
  Automatically promotes secondary and updates cluster endpoints.
```

## 2. DynamoDB Global Tables

```text
Multi-master: write to any region, replicated to all
Conflict resolution: last-write-wins (based on timestamp)

Setup:
  Create table with same name in all regions
  Enable Global Tables, add regions

RPO: near-zero (sub-second replication)
RTO: zero (all regions already accepting writes)

Considerations:
  - Last-write-wins can cause conflicts in concurrent writes
  - Design items to be naturally idempotent
  - Consider vector clocks or conditional writes for conflict-sensitive data
```

## 3. S3 Cross-Region Replication (CRR)

```text
Replicates new objects from source to destination region

Configuration:
  - Versioning required on both source and destination
  - Replication not retroactive (only new objects after enabling)
  - Replication Time Control (RTC): 99.99% of objects replicated within 15 minutes
  - Delete markers: not replicated by default (configure explicitly)

RPO: minutes (with RTC enabled, 15-minute SLA)
```

## 4. Route 53 Health Checks And Failover

```text
Health Check types:
  - Endpoint health check (HTTP/HTTPS/TCP)
  - CloudWatch alarm (if alarm fires, mark endpoint unhealthy)
  - Calculated health check (combines multiple checks)

Failover routing:
  Primary record: weight=100, health check attached
  Secondary record: weight=0, failover
  
  When primary health check fails:
    Route 53 stops serving primary A record
    Traffic shifts to secondary record

DNS TTL trade-off:
  Low TTL (60s): faster failover (old records expire in 60s)
  High TTL (300s): slower failover, but fewer Route 53 queries
  
Production: 60-120s TTL for critical failover records
```

## 5. Global Accelerator

```text
Two static Anycast IP addresses routed via AWS global backbone

Benefits vs Route 53 failover:
  - No DNS propagation delay (failover in seconds, not minutes)
  - Client connects to nearest AWS PoP, AWS backbone routes to healthy region
  - Automatic failover based on endpoint health (30 seconds)
  - Supports TCP/UDP (non-HTTP), unlike Route 53 HTTP-only health checks

Use Global Accelerator when:
  - Fastest possible failover (30s vs 1-5 min with Route 53 TTL)
  - Non-HTTP traffic (gaming, IoT, custom TCP)
  - Client IPs need to be preserved (Route 53 loses original client IP)

Global Accelerator vs CloudFront:
  - CloudFront: HTTP, caching, content delivery, WAF
  - Global Accelerator: any TCP/UDP, network routing, health-based failover
```

---

# Topic 3: Resilience Engineering

## 1. Designing For Regional Failure

A region going down is rare but happens. Design so a region failure does not require human intervention:

```text
Checklist:
[ ] Route 53 health checks on all endpoints
[ ] All config and secrets available in DR region (SSM Parameter Store replicated, Secrets Manager multi-region)
[ ] Container images in ECR in DR region (or use pull-through cache)
[ ] ACM certificates in DR region
[ ] CloudFront distributions pointing to DR origins
[ ] RDS / Aurora snapshots or global database in DR region
[ ] All IAM roles and policies work in DR region (IAM is global)
[ ] VPC and subnet configuration matches in DR region
[ ] Run DR test at least quarterly
```

## 2. Single Points Of Failure Checklist

For each component, ask: "if this fails, does the application fail?"

| Component | Single Failure Eliminated By |
|---|---|
| EC2 single instance | ASG with min=2 across 2+ AZs |
| RDS single-AZ | Multi-AZ standby |
| ALB in one AZ | ALB spans all AZs by default |
| NAT Gateway in one AZ | NAT Gateway per AZ |
| ElastiCache single node | Multi-AZ with automatic failover |
| Lambda (single region) | Lambda is HA within region; cross-region needs Route 53 |
| S3 (regional) | S3 is multi-AZ by default; use CRR for region-level DR |

## 3. Chaos Engineering

Proactively inject failures to validate resilience:

```text
What to test:
  - AZ failure: terminate all instances in one AZ, verify ASG replaces in other AZs
  - DB failover: trigger RDS Multi-AZ failover, measure reconnect time
  - Network failure: block traffic from one subnet, verify routing adjusts
  - Latency injection: introduce latency to downstream service, verify timeouts and circuit breakers
  - Region failover: block Route 53 primary health check, verify DR routing works

AWS Fault Injection Service (FIS):
  - AWS-managed chaos experiments
  - Supports EC2, ECS, EKS, Lambda, RDS, Aurora
  - Rollback on alarm (safe guard)

Run in staging first. Document expected vs actual behavior.
DR test: actually fail over to DR region, validate full stack, fail back.
```

## 4. RTO/RPO Target By Tier

| Tier | RTO Target | RPO Target | DR Pattern |
|---|---|---|---|
| Tier 1 (revenue-critical) | < 1 minute | < 1 second | Active-Active |
| Tier 2 (customer-facing) | < 15 minutes | < 1 minute | Warm Standby |
| Tier 3 (internal business) | < 4 hours | < 15 minutes | Pilot Light |
| Tier 4 (dev/test, non-critical) | < 24 hours | < 1 hour | Backup & Restore |

## 5. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Test DR only on paper, never actually fail over | run quarterly DR drills with actual failover |
| Forget to replicate secrets/parameters to DR region | use Secrets Manager multi-region or SSM cross-region copy |
| DNS TTL too long for failover | set TTL to 60-120s for critical failover records |
| Aurora Global Database without testing promotion | test promotion procedure quarterly |
| All subnets in same AZ (cost saving) | always spread across 2+ AZs in production |
| Single NAT Gateway for all AZs | one NAT Gateway per AZ for AZ-level HA |
| No health check on Route 53 primary | without health check, Route 53 never fails over |
| Active-active with RDS (no multi-master) | Aurora Global with primary write routing, or DynamoDB Global Tables |

## 6. Interview Scenario

**Scenario**: "Design a multi-region architecture for a financial trading platform with RPO < 1 second and RTO < 30 seconds."

Strong answer:

```text
Active-active with two regions: us-east-1 (primary) and eu-west-1 (secondary).

Frontend:
  CloudFront global CDN -> React app from S3 with CRR

Application:
  Global Accelerator: two static IPs, routes to nearest healthy region
  ECS Fargate in both regions, behind ALBs
  GA endpoints: both ALBs, health check on /health

Database:
  DynamoDB Global Tables: multi-master, write to either region
  For relational: Aurora Global Database
    - Writes to primary region (us-east-1)
    - Reads from nearest region
    - Failover: managed global cluster failover < 1 minute

Sessions:
  JWT stateless (no session store needed)
  Or: ElastiCache Global Datastore (Redis cross-region)

Configuration:
  Secrets Manager with multi-region secret replication
  AWS AppConfig for feature flags (deployed to both regions)

Failover:
  GA detects endpoint unhealthy in 30 seconds
  Shifts all traffic to healthy region
  Aurora Global: promote secondary (< 1 minute, but this means brief read-only)
  DynamoDB: no action needed (already multi-master)

RPO achieved: DynamoDB < 1 second replication; Aurora < 1 second
RTO achieved: GA failover < 30 seconds (network); Aurora promotion < 1 minute
  -> True RTO: 30s for network; 60s if Aurora promotion needed
  -> With DynamoDB: 30s
```

## 7. Revision Notes

- RTO = downtime tolerance; RPO = data loss tolerance
- 4 tiers: backup/restore → pilot light → warm standby → active-active
- Aurora Global: RPO < 1s, RTO < 1 min; DynamoDB Global Tables: near-zero RPO, zero RTO
- Route 53 failover: requires health checks; TTL determines failover speed
- Global Accelerator: faster failover (30s), static IPs, TCP/UDP, client IP preserved
- Single NAT per AZ; spread ASG across 3 AZs; always Multi-AZ for RDS
- DR test: actually fail over quarterly; use AWS Fault Injection Service
- Chaos engineering: validate assumptions before production incidents do

## 8. Official Source Notes

- AWS DR strategies: <https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-options-in-the-cloud.html>
- Aurora Global Database: <https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database.html>
- DynamoDB Global Tables: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GlobalTables.html>
- Route 53 failover: <https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/routing-policy-failover.html>
- Global Accelerator: <https://docs.aws.amazon.com/global-accelerator/latest/dg/what-is-global-accelerator.html>
- AWS FIS: <https://docs.aws.amazon.com/fis/latest/userguide/what-is.html>
