# AWS Senior Architecture: Advanced Networking and FinOps Gold Sheet

> Track: AWS Interview Track — Senior Architecture
> Goal: design complex hybrid and multi-account network topologies with Transit Gateway, PrivateLink, and Direct Connect, and control cloud costs with FinOps practices.

---

## 0. How To Read This

Beginner focus:
- Transit Gateway concept
- Direct Connect vs VPN
- NAT Gateway cost

Intermediate focus:
- Transit Gateway route tables and attachments
- PrivateLink for internal services
- Route 53 Resolver rules for hybrid DNS
- Cost Explorer and Savings Plans

Senior / MAANG focus:
- Transit Gateway multi-account hub-and-spoke
- PrivateLink vs VPC Peering vs Transit Gateway decision
- Direct Connect bandwidth and resilience planning
- Hybrid DNS full architecture (on-prem → Route 53 and back)
- Data transfer cost analysis and reduction
- FinOps practices: cost allocation, chargeback, rightsizing

---

# Topic 1: Transit Gateway

## 1. Intuition

Transit Gateway is a network transit hub that connects VPCs, VPNs, and Direct Connect gateways in a hub-and-spoke topology.

Without Transit Gateway (VPC peering at scale):

```text
VPC A <-> VPC B
VPC A <-> VPC C
VPC A <-> VPC D
VPC B <-> VPC C
VPC B <-> VPC D
VPC C <-> VPC D

n*(n-1)/2 peering connections = 6 for 4 VPCs = scales poorly
Each connection must be manually configured, no transitive routing
```

With Transit Gateway:

```text
VPC A \
VPC B  -> Transit Gateway -> VPN / Direct Connect / other accounts
VPC C /
VPC D /

n attachments = 4 for 4 VPCs = linear scaling
Transitive routing: VPC A can reach VPC B through TGW
```

## 2. Transit Gateway Components

| Component | Description |
|---|---|
| Attachment | connection between TGW and a VPC, VPN, Direct Connect GW, or another TGW |
| Route Table | routing rules for attachments |
| Route | destination CIDR → attachment |
| Association | attachment → route table (determines which routes attachment uses) |
| Propagation | attachment → route table (automatically adds attachment's CIDR to route table) |

## 3. Route Table Design

By default, all attachments share one route table (full mesh). For segmentation, create multiple route tables:

Segmented design:

```text
Production Route Table:
  Propagated: Prod VPCs, Shared Services VPC, On-Prem (DX/VPN)
  Not propagated: Dev VPCs, Sandbox VPCs
  Dev cannot reach Prod

Dev Route Table:
  Propagated: Dev VPCs, Shared Services VPC
  Not propagated: Prod VPCs, On-Prem

Shared Services Route Table:
  Propagated: All VPCs (so Shared Services can be reached by all)
```

## 4. Multi-Account Transit Gateway

Share Transit Gateway across accounts using Resource Access Manager (RAM):

```text
Network Account creates TGW -> shares via RAM to all accounts in organization
Member accounts create VPC attachments to shared TGW
Member accounts update route tables to send inter-VPC traffic to TGW

RAM share:
  aws ram create-resource-share \
    --name tgw-share \
    --resource-arns arn:aws:ec2:us-east-1:NETWORK_ACCOUNT:transit-gateway/tgw-xxx \
    --principals arn:aws:organizations::MANAGEMENT:organization/o-xxx
```

## 5. TGW Costs

```text
Attachment: $0.05/hour per attachment (VPC, VPN, or DX connection)
Data processing: $0.02/GB processed by TGW

Cost example for 10 VPCs + 1 DX attachment:
  11 attachments × $0.05 × 720 hours = $396/month
  Plus data transfer cost

Optimization:
  Use VPC Gateway Endpoints for S3/DynamoDB (free, bypass TGW)
  Use Interface Endpoints for high-volume service traffic (cheaper than TGW + NAT)
```

---

# Topic 2: PrivateLink

## 1. Intuition

PrivateLink exposes a single service from one VPC to consumers in other VPCs or accounts without full VPC peering:

```text
Provider VPC:
  Service runs behind NLB (Network Load Balancer)
  Creates VPC Endpoint Service on the NLB
  Allows specific consumers to connect

Consumer VPC:
  Creates Interface Endpoint pointing to provider's Endpoint Service
  Gets private IP in consumer's subnet
  Connects to service via private IP — no internet, no peering
```

## 2. PrivateLink vs VPC Peering vs Transit Gateway

| Feature | VPC Peering | Transit Gateway | PrivateLink |
|---|---|---|---|
| What connects | two entire VPCs | many VPCs + on-prem | one service |
| Transitive routing | no | yes | n/a (single service) |
| IP overlap allowed | no | no | yes (NLB absorbs) |
| Direction | bidirectional | bidirectional | one-directional |
| Cross-account | yes | yes | yes |
| Use case | small number of VPCs, any traffic | many VPCs, any traffic | expose one service |

PrivateLink advantage: overlapping CIDRs are OK because consumers connect to the NLB's private IP in their own subnet, not to the provider VPC range.

## 3. AWS Services Via PrivateLink

Most AWS services can be accessed via Interface VPC Endpoints (PrivateLink):
- Secrets Manager, SSM, ECR, SQS, SNS, CloudWatch, KMS, etc.

```text
Without VPC endpoint:
  Lambda in private subnet -> NAT Gateway -> Internet -> Secrets Manager

With Interface Endpoint:
  Lambda in private subnet -> Interface Endpoint (private IP in same subnet) -> Secrets Manager
  No internet. No NAT. Cheaper at high volume.
```

---

# Topic 3: Hybrid Connectivity

## 1. AWS Direct Connect vs Site-to-Site VPN

| Feature | Direct Connect | Site-to-Site VPN |
|---|---|---|
| Path | dedicated fiber from on-prem to AWS PoP | IPSec over internet |
| Bandwidth | 50 Mbps to 100 Gbps | up to 1.25 Gbps per tunnel |
| Latency | consistent, low | internet-dependent, variable |
| Reliability | high (dedicated) | depends on internet |
| Cost | port hours + data transfer (no internet data out costs) | per VPN connection-hour |
| Setup time | weeks to months (physical circuit) | minutes to hours |
| HA | requires redundant circuits | two tunnels per VPN (automatic) |

Use Direct Connect when:
- consistent bandwidth > 1 Gbps
- latency-sensitive workloads (databases, real-time)
- large data transfer volumes (cheaper than internet egress at scale)
- compliance requires private connectivity

Use VPN when:
- backup for Direct Connect
- smaller offices
- temporary connectivity
- fast setup required

## 2. Direct Connect Resilience

Direct Connect Single Point of Failure risks:

```text
Level 1 (low resilience):
  One Direct Connect circuit, one location, one device

Level 2 (recommended for most):
  Two Direct Connect circuits, same DX location, different devices
  + Site-to-Site VPN as failover (active-passive)

Level 3 (high availability):
  Two Direct Connect circuits, two DX locations, different routers
  Survives location-level failure

Level 4 (maximum):
  Two DX locations + two redundant circuits each = 4 circuits
  Used for critical financial/government workloads
```

## 3. Hybrid DNS

Two DNS resolution needs in hybrid environments:

### On-Premises → AWS

On-prem resolves `api.internal.company.com` → points to Route 53 private hosted zone:

```text
Route 53 Resolver Inbound Endpoint:
  - Two ENIs in your VPC (one per AZ)
  - On-prem DNS server forwards *.internal.company.com to these ENI IPs
  - Route 53 resolves and returns private hosted zone records
```

### AWS → On-Premises

AWS resources resolve `db.corp.company.com` → points to on-prem DNS:

```text
Route 53 Resolver Outbound Endpoint:
  - Two ENIs in your VPC
  - Resolver Rule: *.corp.company.com -> forward to on-prem DNS server IPs via DX/VPN
  - EC2 instances resolve on-prem DNS names through this endpoint
```

Combined architecture:

```text
On-Prem DNS -> (query for *.internal) -> Route 53 Inbound Endpoint -> Route 53 PHZ
Route 53 Resolver -> (query for *.corp) -> Outbound Endpoint -> On-Prem DNS
```

---

# Topic 4: FinOps

## 1. AWS Cost Model

```text
Compute: EC2 on-demand, Lambda per-ms, ECS/EKS (pay for EC2 or Fargate)
Storage: S3 per-GB-month, EBS per-GB-month, RDS per-GB
Data transfer:
  Inbound: free
  Same region, same AZ: free
  Same region, cross-AZ: $0.01/GB each way
  Cross-region: $0.02-$0.09/GB
  Internet egress: $0.09-$0.09/GB (varies by region)

Cross-AZ data transfer is often an overlooked cost.
```

## 2. NAT Gateway Cost

NAT Gateway:
- $0.045/hour per NAT Gateway
- $0.045/GB processed

High traffic through NAT Gateway is expensive. Fix:

```text
Traffic that can bypass NAT:
  S3 -> use S3 Gateway Endpoint (free)
  DynamoDB -> use DynamoDB Gateway Endpoint (free)
  ECR, SSM, Secrets Manager -> use Interface Endpoints ($0.01/hour + $0.01/GB, often cheaper)
  ECS image pulls from ECR -> ECR Interface Endpoint (avoid NAT for image pull costs)
```

## 3. Data Transfer Cost Reduction

| Cost Source | Reduction Strategy |
|---|---|
| Cross-AZ replication | pin high-volume traffic to same AZ when possible |
| EC2 → S3 | use S3 Gateway Endpoint |
| EC2 → DynamoDB | use DynamoDB Gateway Endpoint |
| Lambda → AWS services | use VPC Interface Endpoints if in VPC |
| Cross-region replication | compress data before transfer |
| Internet egress | use CloudFront (data from CloudFront edge cheaper than direct egress) |

## 4. Savings Plans vs Reserved Instances

| Tool | Flexibility | Discount | Commitment |
|---|---|---|---|
| Compute Savings Plans | any EC2, Lambda, Fargate, any region | up to 66% | 1 or 3 years, $/hour |
| EC2 Instance Savings Plans | specific family, specific region | up to 72% | 1 or 3 years |
| Reserved Instances | specific instance type, region, AZ | up to 72% | 1 or 3 years, upfront options |
| RDS Reserved Instances | specific DB engine, instance type | up to 69% | 1 or 3 years |

Compute Savings Plans: start here. Most flexible.
EC2 Instance Savings Plans: more discount for committed workloads in one region.
Reserved Instances for RDS, ElastiCache, Redshift (not covered by Savings Plans).

## 5. Spot Instances

Spot = unused EC2 capacity at up to 90% discount. AWS can reclaim with 2-minute notice.

Use Spot for:
- batch processing, data analysis jobs
- CI/CD build agents
- EMR workloads
- stateless app tier with graceful interrupt handling (save progress, terminate cleanly)

Do NOT use Spot for:
- stateful applications (databases)
- workloads that cannot be interrupted

Mixed instances policy (ASG):

```text
On-Demand base: 2 instances (always on)
On-Demand percentage: 20% of scale-up
Spot percentage: 80% of scale-up

Multiple instance types in pool: m5.xlarge, m5a.xlarge, m4.xlarge
  (reduces interruption probability — many pools, not just one)
```

## 6. Cost Tagging Strategy

```text
Required tags (enforced by SCP):
  Team: payments | orders | platform
  Environment: dev | staging | prod
  CostCenter: CC-123
  Service: payment-api | order-processor

Tag policies via Organizations:
  Enforce consistent tag values across all accounts
  Report on untagged resources
  Block resource creation without required tags (SCP condition)
```

## 7. Common Mistakes

| Mistake | Better Approach |
|---|---|
| VPC peering at scale (full mesh) | Transit Gateway for hub-and-spoke |
| NAT Gateway for all traffic including S3 | S3/DynamoDB Gateway Endpoints (free) |
| Single Direct Connect circuit for critical workloads | two circuits, two locations |
| No DNS Resolver rules for hybrid | Route 53 Resolver inbound + outbound endpoints |
| Spot in stateful workloads | Spot only for stateless, interrupt-tolerant workloads |
| No Savings Plans analysis | Run Cost Explorer Savings Plans recommendations monthly |
| Untagged resources | SCP to require tags; tag policies to enforce values |
| Cross-AZ replication of large datasets | pin to same AZ when tolerable, or use S3 endpoints |

## 8. Interview Scenario

**Scenario**: "Design the network architecture for 20 VPCs across 5 AWS accounts with on-prem connectivity."

Strong answer:

```text
Network Account owns the hub:
  Transit Gateway (TGW) shared via RAM to all 5 accounts

Segmentation via TGW route tables:
  Prod route table: prod VPCs + Shared Services + DX/VPN
  Dev route table: dev VPCs + Shared Services
  Sandbox route table: sandbox VPCs only (isolated)

Hybrid connectivity:
  Direct Connect (1 Gbps) with two circuits for HA
  Site-to-Site VPN as hot failover for DX
  Both terminate in Network Account TGW

DNS:
  Route 53 Resolver Inbound Endpoints in Network Account
    On-prem DNS: forward *.internal.company.com -> Inbound Endpoint IPs
  Route 53 Resolver Outbound Endpoints
    Resolver Rule: *.corp.company.com -> On-Prem DNS servers

Shared Services VPC:
  ECR, internal APIs, artifact repos
  Interface Endpoints for ECR, SSM, Secrets Manager (avoid NAT)

Cost optimization:
  S3 Gateway Endpoints in all VPCs (free)
  DynamoDB Gateway Endpoints in all VPCs (free)
  Single NAT Gateway per AZ (not per VPC — consolidate through TGW)
  Spot instances for dev EC2 workloads (70% discount)
  Compute Savings Plans for consistent prod workloads
```

## 9. Revision Notes

- Transit Gateway: hub-and-spoke, transitive routing, route tables for segmentation
- TGW cost: $0.05/attachment/hour + $0.02/GB processed
- PrivateLink: expose one service; IP overlap OK; one-directional
- Direct Connect: consistent, high bandwidth; requires redundant circuits for HA
- VPN: fast setup; internet-dependent; use as DX failover
- Hybrid DNS: Route 53 Resolver Inbound (on-prem → AWS) + Outbound (AWS → on-prem)
- NAT Gateway expensive at scale: use Gateway Endpoints for S3/DynamoDB (free)
- Savings Plans: Compute most flexible; RI for RDS, ElastiCache, Redshift
- Spot: 90% discount; stateless, interruptible workloads only; mixed instance pools

## 10. Official Source Notes

- Transit Gateway: <https://docs.aws.amazon.com/vpc/latest/tgw/what-is-transit-gateway.html>
- PrivateLink: <https://docs.aws.amazon.com/vpc/latest/privatelink/what-is-privatelink.html>
- Direct Connect: <https://docs.aws.amazon.com/directconnect/latest/UserGuide/Welcome.html>
- Route 53 Resolver: <https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/resolver.html>
- AWS Savings Plans: <https://docs.aws.amazon.com/savingsplans/latest/userguide/what-is-savings-plans.html>
- Cost Explorer: <https://docs.aws.amazon.com/cost-management/latest/userguide/ce-what-is.html>
