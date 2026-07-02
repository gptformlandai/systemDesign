# AWS Architect 05: Advanced Networking, FinOps, and Data Platform Real-World Guide

> Goal: handle the architecture topics that show up when systems grow: many VPCs, private service access, hybrid networking, DNS, hidden AWS costs, and data platform choices.

---

# Index

| Section | Focus |
|---|---|
| [0. Real Situation](#0-real-situation) | Real Situation |
| [1. Advanced Networking Mental Model](#1-advanced-networking-mental-model) | Advanced Networking Mental Model |
| [2. Console Build: Transit Gateway](#2-console-build-transit-gateway) | Console Build: Transit Gateway |
| [3. Console Build: PrivateLink](#3-console-build-privatelink) | Console Build: PrivateLink |
| [4. Console Build: VPC Endpoints To Reduce NAT Cost](#4-console-build-vpc-endpoints-to-reduce-nat-cost) | Console Build: VPC Endpoints To Reduce NAT Cost |
| [5. Hybrid DNS With Route 53 Resolver](#5-hybrid-dns-with-route-53-resolver) | Hybrid DNS With Route 53 Resolver |
| [6. Console Build: Site-to-Site VPN Data Tunnel](#6-console-build-site-to-site-vpn-data-tunnel) | Console Build: Site-to-Site VPN Data Tunnel |
| [7. Console Build: Direct Connect](#7-console-build-direct-connect) | Console Build: Direct Connect |
| [8. FinOps Mental Model](#8-finops-mental-model) | FinOps Mental Model |
| [9. Console Build: Budgets And Alerts](#9-console-build-budgets-and-alerts) | Console Build: Budgets And Alerts |
| [10. Console Build: Cost Explorer Investigation](#10-console-build-cost-explorer-investigation) | Console Build: Cost Explorer Investigation |
| [11. Data Platform Decision](#11-data-platform-decision) | Data Platform Decision |
| [12. Console Build: S3 Data Lake Zones](#12-console-build-s3-data-lake-zones) | Console Build: S3 Data Lake Zones |
| [13. GenAI Data Platform Scenario: RAG Over Enterprise Docs](#13-genai-data-platform-scenario-rag-over-enterprise-docs) | GenAI Data Platform Scenario: RAG Over Enterprise Docs |
| [14. Production Checklist](#14-production-checklist) | Production Checklist |
| [15. Interview Question](#15-interview-question) | Interview Question |
| [16. Strong Answer](#16-strong-answer) | Strong Answer |
| [17. Revision Notes](#17-revision-notes) | Revision Notes |
| [18. Official Source Notes](#18-official-source-notes) | Official Source Notes |

---

## 0. Real Situation

Your AWS setup grew from one app to many teams:

```text
20 VPCs
multiple accounts
shared services
data lake
analytics team
GenAI document search
on-prem network connection
large NAT Gateway bill
cross-AZ data transfer bill
teams asking for private APIs
```

Architect answer:

```text
You need a network architecture, cost model, and data platform strategy.
Not just more VPC peering.
```

---

## 1. Advanced Networking Mental Model

```text
VPC Peering:
  point-to-point private connectivity.

Transit Gateway:
  hub-and-spoke routing across many VPCs/on-prem networks.

PrivateLink:
  private service exposure without full network connectivity.

VPC Sharing:
  central network account owns subnets; workload accounts deploy into them.

Direct Connect:
  dedicated private connection from on-prem to AWS.

Site-to-Site VPN:
  encrypted tunnel over internet.

Route 53 Resolver:
  hybrid DNS between AWS and on-prem.
```

---

## 2. Console Build: Transit Gateway

### When You Need It

You have many VPCs:

```text
app-dev
app-prod
shared-services
data-platform
security-tools
on-prem network
```

VPC peering becomes messy.

### Console Path

```text
AWS Console -> Search "Transit Gateway" -> Transit gateways -> Create transit gateway
```

Choose:

```text
name
description
ASN
DNS support enabled
VPN ECMP support if needed
auto accept shared attachments disabled for tighter control
```

### What Each Click Changes

```text
Transit Gateway:
  creates central routing hub.

ASN:
  BGP identity for hybrid routing.

DNS support:
  enables DNS resolution support across attached VPCs.

Auto accept:
  controls whether attachments are automatically accepted.
```

### Attach VPCs

```text
Transit Gateway -> Transit gateway attachments -> Create attachment
```

Choose:

```text
attachment type: VPC
VPC ID
subnets in each AZ
```

### What This Changes

It connects the selected VPC to the Transit Gateway.

Then you must update route tables:

```text
VPC route table -> route destination CIDR -> target Transit Gateway
Transit Gateway route table -> route destination CIDR -> target attachment
```

### What Can Go Wrong

Overly broad routing.

Bad:

```text
Every VPC can talk to every other VPC.
```

Better:

```text
Separate TGW route tables:
  prod
  nonprod
  shared-services
  inspection
```

---

## 3. Console Build: PrivateLink

### When You Need It

A producer service in one account/VPC needs to be privately consumed by other accounts/VPCs.

You do not want:

```text
full VPC-to-VPC routing
overlapping CIDR problems
exposing service publicly
```

### Console Path: Producer Side

```text
EC2 -> Load Balancers -> Create Network Load Balancer
VPC -> Endpoint services -> Create endpoint service
```

Choose:

```text
NLB
acceptance required
allowed principals
```

### Console Path: Consumer Side

```text
VPC -> Endpoints -> Create endpoint
```

Choose:

```text
endpoint type: Endpoint services that use NLBs
service name from producer
VPC/subnets/security group
private DNS if configured
```

### What Each Click Changes

```text
Producer endpoint service:
  publishes private service entry point.

Allowed principals:
  controls which AWS accounts can create endpoints.

Consumer VPC endpoint:
  creates ENIs in consumer subnets.

Security group:
  controls which clients in consumer VPC can call the endpoint.
```

### Why It Matters

PrivateLink gives private service access without opening the whole network.

### What Can Go Wrong

Debugging is often split between:

```text
consumer security group
endpoint policy
producer NLB health
allowed principals
DNS name
```

---

## 4. Console Build: VPC Endpoints To Reduce NAT Cost

### Real Situation

Private ECS tasks call S3, ECR, CloudWatch Logs, Secrets Manager, and STS through NAT Gateway.

NAT bill explodes.

### Console Path

```text
VPC -> Endpoints -> Create endpoint
```

Choose:

```text
Gateway endpoint:
  S3
  DynamoDB

Interface endpoint:
  ECR API
  ECR Docker
  CloudWatch Logs
  Secrets Manager
  STS
  Bedrock runtime where supported/needed
```

### What Each Click Changes

```text
Gateway endpoint:
  adds route table entries for S3/DynamoDB private access.

Interface endpoint:
  creates private ENIs with private IPs for AWS service access.

Endpoint policy:
  limits which resources/actions can be used through endpoint.
```

### Why It Matters

Traffic to AWS services can avoid NAT Gateway and stay on AWS private network paths.

### What Can Go Wrong

Interface endpoints cost hourly plus data processing.

Architect move:

```text
Compare NAT cost vs endpoint cost.
Use gateway endpoints for S3/DynamoDB almost by default in private VPCs.
Use interface endpoints for high-volume or security-sensitive service calls.
```

---

## 5. Hybrid DNS With Route 53 Resolver

### Scenario

AWS services need to resolve `corp.internal`, and on-prem apps need to resolve private AWS names.

### Console Path

```text
Route 53 -> Resolver -> Inbound endpoints -> Create inbound endpoint
Route 53 -> Resolver -> Outbound endpoints -> Create outbound endpoint
Route 53 -> Resolver rules -> Create rule
```

### What Each Click Changes

```text
Inbound endpoint:
  lets on-prem DNS forward queries into AWS.

Outbound endpoint:
  lets AWS forward queries to on-prem DNS.

Resolver rule:
  says which domain suffix should be forwarded where.
```

### What Can Go Wrong

DNS loops.

Architect move:

```text
Document domain ownership.
Keep forwarding rules specific.
Monitor resolver query logs for loops/failures.
```

---

## 6. Console Build: Site-to-Site VPN Data Tunnel

### When You Need It

You need an encrypted tunnel between AWS and an on-prem/customer network over the public internet.

Example:

```text
AWS private subnet app -> VPN tunnel -> on-prem database/API
on-prem users -> VPN tunnel -> internal ALB in AWS
```

### Console Path

```text
VPC -> Customer gateways -> Create customer gateway
VPC -> Virtual private gateways -> Create virtual private gateway
VPC -> Virtual private gateways -> Attach to VPC
VPC -> Site-to-Site VPN connections -> Create VPN connection
```

If connecting many VPCs:

```text
Transit Gateway -> Transit gateway attachments -> Create VPN attachment
```

### What Each Click Changes

```text
Customer gateway:
  defines the on-prem VPN device public IP and routing mode.

Virtual private gateway:
  AWS-side VPN gateway for one VPC.

Transit Gateway VPN attachment:
  AWS-side VPN gateway when many VPCs need shared hybrid connectivity.

VPN connection:
  creates two redundant IPsec tunnels.

Static routes or BGP:
  tells AWS and on-prem which CIDRs are reachable.

Download configuration:
  gives vendor-specific tunnel config for the on-prem device.
```

### Route Table Impact

After the VPN exists, update routing:

```text
AWS private subnet route table:
  destination = on-prem CIDR
  target = virtual private gateway or transit gateway

On-prem router/firewall:
  destination = AWS VPC CIDR
  target = VPN tunnel
```

### What Can Go Wrong

```text
tunnel is up but routes are missing
AWS CIDR overlaps with on-prem CIDR
security groups block private traffic
on-prem firewall blocks return path
only one tunnel configured
BGP route not advertised/accepted
```

### Production Check

```text
Both tunnels are up.
CloudWatch alarms exist for tunnel down.
Routes exist both ways.
No CIDR overlap.
Security groups/NACLs/firewalls allow required ports.
Failover between tunnels tested.
```

---

## 7. Console Build: Direct Connect

### When You Need It

You need a dedicated private network path from on-prem/data center/colo to AWS.

Use cases:

```text
large data transfer
predictable latency
hybrid enterprise apps
regulated network connectivity
more stable path than internet VPN
```

### Console Path

```text
Direct Connect -> Connections -> Create connection
Direct Connect -> Virtual interfaces -> Create virtual interface
```

### What Each Click Changes

```text
Connection:
  creates/request a physical or hosted dedicated network connection.

Location:
  Direct Connect facility where connectivity terminates.

Port speed:
  bandwidth of physical connection.

Private virtual interface:
  reaches private VPC IPs through VGW/DX gateway.

Transit virtual interface:
  reaches Transit Gateway through Direct Connect gateway.

Public virtual interface:
  reaches AWS public services, not private VPC IPs.

BGP peer:
  exchanges routes between on-prem and AWS.
```

### Important Security Note

```text
Direct Connect is private but not encrypted by default.
Use application TLS or VPN over Direct Connect if encryption is required.
```

### Production Check

```text
Redundant Direct Connect links if workload is critical.
Backup VPN path exists if required.
BGP routes are correct.
Route tables point to the right gateway.
Security groups still enforce least privilege.
Failover tested.
```

---

## 8. FinOps Mental Model

AWS cost problems usually come from:

```text
idle resources
overprovisioned compute
NAT Gateway data processing
cross-AZ data transfer
cross-region data transfer
unbounded logs
expensive storage class mismatch
uncontrolled model inference tokens
large vector indexes
always-on GPU endpoints
```

Architect rule:

```text
Every architecture diagram should have a cost story.
```

---

## 9. Console Build: Budgets And Alerts

### Console Path

```text
AWS Console -> Billing and Cost Management -> Budgets -> Create budget
```

Choose:

```text
Cost budget
monthly
filter by account, tag, service, or usage type
alert at 50, 80, 100 percent
email/SNS recipients
```

### What Each Click Changes

```text
Filter:
  defines which spend is tracked.

Threshold:
  when alert fires.

SNS/email:
  who gets notified.
```

### GenAI Budget Example

```text
Service filter: Amazon Bedrock
Tag filter: project = support-chatbot
Alert:
  50 percent to team
  80 percent to team + manager
  100 percent to platform/on-call
```

### What Can Go Wrong

Budgets alert after spend happens.

Architect move:

```text
Use budgets plus application rate limits, quotas, tags, and dashboards.
```

---

## 10. Console Build: Cost Explorer Investigation

### Console Path

```text
Billing and Cost Management -> Cost Explorer -> New cost and usage report view
```

Group by:

```text
Service
Linked account
Usage type
Region
Tag
```

### What Each Click Shows

```text
Service:
  which AWS service costs most.

Linked account:
  which team/account owns spend.

Usage type:
  what kind of cost within a service.

Region:
  unexpected regional spend.

Tag:
  project/team/environment cost ownership.
```

### Real Debug: NAT Cost Spike

Look for:

```text
EC2-Other
NatGateway-Bytes
DataTransfer
```

Fix:

```text
add S3/DynamoDB gateway endpoints
add ECR/Logs/Secrets interface endpoints if justified
reduce cross-AZ traffic
keep chatty services in same AZ where safe
```

---

## 11. Data Platform Decision

| Need | AWS Choice |
|---|---|
| Durable object lake | S3 |
| Catalog and ETL | Glue |
| SQL on S3 | Athena |
| Warehouse analytics | Redshift / Redshift Serverless |
| Governed lake permissions | Lake Formation |
| Big data processing | EMR / Glue jobs |
| Search/log analytics | OpenSearch |
| Streaming ingestion | Kinesis / MSK |
| Vector search for RAG | OpenSearch Serverless / Aurora pgvector / managed vector store |

Architect rule:

```text
Do not put everything in Redshift.
Do not query raw JSON forever.
Use storage formats, partitions, catalogs, and governance.
```

---

## 12. Console Build: S3 Data Lake Zones

### Console Path

```text
S3 -> Create bucket
```

Create buckets or prefixes:

```text
raw/
clean/
curated/
analytics/
archive/
```

Enable:

```text
Block Public Access
SSE-KMS encryption
versioning where needed
lifecycle rules
access logging if required
```

### What Each Click Changes

```text
raw:
  immutable landing data.

clean:
  validated and standardized data.

curated:
  business-ready datasets.

analytics:
  query-optimized data.

lifecycle:
  moves old data to cheaper storage.
```

### What Can Go Wrong

No partition strategy.

Bad:

```text
s3://lake/orders/all-orders.json
```

Better:

```text
s3://lake/curated/orders/year=2026/month=06/day=17/
```

---

## 13. GenAI Data Platform Scenario: RAG Over Enterprise Docs

### Requirement

Build a chatbot over internal PDFs, tickets, architecture docs, and runbooks.

### Data Architecture

```text
S3 source bucket
  -> document classification
  -> ingestion job
  -> chunking
  -> embedding model
  -> vector store
  -> Bedrock Knowledge Base or custom RAG service
  -> app with auth-aware retrieval
```

### Console Steps

```text
S3 -> Create bucket -> encryption + block public access
Macie -> Enable scan for sensitive docs
Bedrock -> Knowledge Bases -> Create knowledge base
Choose data source: S3
Choose embedding model
Choose vector store
Sync data source
```

### What Each Click Changes

```text
S3 bucket:
  source of documents.

Macie scan:
  discovers sensitive/regulated content before ingestion.

Knowledge Base:
  managed RAG retrieval layer.

Embedding model:
  converts chunks into vectors.

Vector store:
  stores searchable embeddings.

Sync:
  ingests documents into retrieval index.
```

### What Can Go Wrong

User authorization is ignored.

Example:

```text
All employees can ask questions over HR/legal/security documents.
```

Fix:

```text
separate knowledge bases by sensitivity
metadata filters
document-level ACLs in custom RAG
PII guardrails
prompt/response logging with redaction
```

---

## 14. Production Checklist

- VPC-to-VPC connectivity uses intentional pattern
- Transit Gateway route tables segment environments
- PrivateLink used for private service exposure when full routing is not needed
- VPC endpoints reduce NAT/security exposure where justified
- hybrid DNS rules documented
- Direct Connect/VPN designed with redundancy
- budgets configured by account/project/service
- cost allocation tags enforced
- NAT/data transfer/logging costs reviewed
- GenAI token costs tracked by app/team
- data lake zones defined
- Glue catalog/Lake Formation considered for governance
- vector store cost and retention reviewed
- RAG data classification done before ingestion

---

## 15. Interview Question

> Your AWS bill doubled and the company also wants to connect 15 VPCs and build a RAG app. What do you investigate?

---

## 16. Strong Answer

I would split the problem into network, cost, and data architecture. For networking, I would avoid full-mesh VPC peering and evaluate Transit Gateway for hub-and-spoke routing, with route table segmentation for prod/nonprod/shared services. If teams only need to consume a private service, I would use PrivateLink instead of full network connectivity.

For cost, I would use Cost Explorer grouped by service, account, usage type, region, and tags. I would specifically check NAT Gateway processing, cross-AZ transfer, logs, idle compute, and GenAI model inference usage. I would add budgets and cost allocation tags.

For the RAG app, I would store source documents in encrypted S3, classify them with Macie if sensitive, ingest into a Bedrock Knowledge Base or custom vector store, and enforce authorization-aware retrieval and guardrails.

---

## 17. Revision Notes

- One-line summary: advanced AWS architecture is routing, cost, and data governance at scale.
- Three keywords: Transit Gateway, PrivateLink, FinOps.
- One interview trap: connecting everything to everything.
- Memory trick: "Route intentionally, tag everything, classify data before AI."

---

## 18. Official Source Notes

- AWS Organizations supports account-level cost, security, and governance boundaries: <https://docs.aws.amazon.com/organizations/latest/userguide/orgs_introduction.html>
- Bedrock Knowledge Bases support RAG over proprietary data and vector stores: <https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html>
- Bedrock inference profiles support usage and cost tracking with tags: <https://docs.aws.amazon.com/bedrock/latest/userguide/inference-profiles.html>
