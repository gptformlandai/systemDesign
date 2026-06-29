# AWS Networking: VPC, ALB, Route 53, and CloudFront Gold Sheet

> Track: AWS Interview Track — Foundations
> Goal: design secure, scalable, multi-AZ AWS network architectures and explain every component with production reasoning.

---

## 0. How To Read This

Beginner focus:
- VPC, public vs private subnets
- Security groups vs NACLs
- ALB, target groups
- Route 53 basics

Intermediate focus:
- NAT Gateway, VPC endpoints
- ALB routing rules, listener rules
- Health checks, connection draining
- Route 53 routing policies

Senior / MAANG focus:
- VPC peering vs Transit Gateway vs PrivateLink
- ALB vs NLB vs API Gateway decision
- CloudFront behaviors, OAC, Lambda@Edge
- NAT Gateway cost vs PrivateLink trade-off
- Hybrid connectivity (Direct Connect, Site-to-Site VPN)

---

# Topic 1: VPC — Virtual Private Cloud

## 1. Intuition

A VPC is your private data center inside AWS.

Without a VPC, all AWS resources would be on a shared public network. VPC gives you isolation, routing control, firewall rules, and the ability to connect AWS services to on-premises networks.

## 2. VPC Core Concepts

| Concept | Meaning |
|---|---|
| VPC | isolated network with CIDR block (e.g., 10.0.0.0/16) |
| Subnet | subdivision of VPC CIDR assigned to one AZ |
| Internet Gateway | enables internet access for public subnets |
| NAT Gateway | lets private subnet instances initiate outbound internet access |
| Route Table | rules controlling where traffic goes from a subnet |
| Security Group | stateful instance-level firewall |
| NACL | stateless subnet-level firewall |
| VPC Endpoint | private connection to AWS services without internet |
| Elastic IP | static public IP attached to an instance |

## 3. Subnet Design

Production standard — 3 subnet tiers per AZ:

```text
VPC: 10.0.0.0/16

Public Subnets (ALB, NAT Gateway, Bastion)
  10.0.1.0/24  — AZ-a
  10.0.2.0/24  — AZ-b
  10.0.3.0/24  — AZ-c

Private App Subnets (EC2, ECS tasks, Lambda)
  10.0.11.0/24 — AZ-a
  10.0.12.0/24 — AZ-b
  10.0.13.0/24 — AZ-c

Private Data Subnets (RDS, ElastiCache)
  10.0.21.0/24 — AZ-a
  10.0.22.0/24 — AZ-b
  10.0.23.0/24 — AZ-c
```

Rules:
- ALB goes in public subnets, backend in private subnets
- Databases never in public subnets
- Only explicitly public resources get public subnet placement

## 4. Security Groups vs NACLs

| Feature | Security Group | NACL |
|---|---|---|
| Level | instance / ENI | subnet |
| State | stateful (return traffic automatic) | stateless (must allow both directions) |
| Rules | allow only | allow and deny |
| Default | deny all inbound, allow all outbound | allow all |
| Evaluation | all rules evaluated | rules evaluated in number order |

Interview trap:

```text
NACLs are stateless. If you allow inbound port 443, you also must allow the ephemeral return
ports (1024-65535) outbound, or the TCP response will be dropped. Security groups handle this
automatically.
```

Production practice:

```text
Use security groups as the primary control layer.
Use NACLs only for subnet-level explicit denies (e.g., block a known bad CIDR range).
```

## 5. NAT Gateway

NAT Gateway lets private subnet resources initiate outbound internet calls (e.g., AWS API calls, package downloads) without having a public IP.

Architecture:

```text
Private subnet instance
-> Private route table: 0.0.0.0/0 -> NAT Gateway in public subnet
-> NAT Gateway -> Internet Gateway -> Internet
```

Cost trap:

```text
NAT Gateway charges per GB of data processed (~$0.045/GB in us-east-1).
High-volume S3, DynamoDB, or ECR traffic through NAT = expensive.

Solution: VPC Gateway Endpoints for S3 and DynamoDB (free, no NAT transit).
Solution: VPC Interface Endpoints (PrivateLink) for other services (hourly + per-GB, but
cheaper than NAT for high volumes).
```

## 6. VPC Endpoints

| Type | What It Does | Services |
|---|---|---|
| Gateway Endpoint | route table entry, free | S3, DynamoDB |
| Interface Endpoint | ENI with private IP, hourly + per-GB | ECR, Secrets Manager, SQS, SNS, etc. |

Always create S3 and DynamoDB Gateway Endpoints to avoid NAT costs.

## 7. VPC Peering vs Transit Gateway vs PrivateLink

| Option | Best For | Key Limitation |
|---|---|---|
| VPC Peering | 2-3 VPCs, same or cross-account, transitive routing NOT allowed | does not scale beyond ~10 VPCs |
| Transit Gateway | many VPCs, cross-account, hub-and-spoke | per-attachment + per-GB cost |
| PrivateLink | expose one service privately to consumers without full VPC access | one-directional, single service exposure |

Interview line:

```text
VPC peering is fine for a few VPCs. At scale, Transit Gateway becomes the hub for all
VPC-to-VPC routing. PrivateLink is for securely exposing one specific service across
account or VPC boundaries without peering the full networks.
```

---

# Topic 2: ALB — Application Load Balancer

## 1. What ALB Does

ALB distributes HTTP/HTTPS/gRPC traffic to targets based on routing rules.

Operates at Layer 7 (HTTP). Makes routing decisions based on:
- host header (host-based routing)
- URL path (path-based routing)
- HTTP headers and query strings
- source IP

## 2. Core ALB Concepts

| Concept | Meaning |
|---|---|
| Listener | port + protocol (80, 443) that ALB listens on |
| Listener Rule | condition + action (forward, redirect, fixed response) |
| Target Group | group of targets with health check |
| Target | EC2 instance, ECS task, Lambda, IP address |
| Weighted Target Group | send % traffic to different target groups (blue-green/canary) |

## 3. ALB vs NLB vs API Gateway

| Service | Protocol | Use Case |
|---|---|---|
| ALB | HTTP/HTTPS/gRPC, Layer 7 | web apps, container services, path routing |
| NLB | TCP/UDP/TLS, Layer 4 | low latency, static IP, IoT, non-HTTP protocols |
| API Gateway | HTTP/REST/WebSocket | managed API, Lambda integration, throttling, auth |

Interview answer:

```text
I use ALB for standard web apps. NLB when I need a static IP, extreme low latency, or
non-HTTP protocols. API Gateway when I want managed throttling, request validation, Lambda
proxy, or API key management without running a server.
```

## 4. HTTPS And SSL/TLS

ALB terminates TLS. Certificate options:
- ACM (AWS Certificate Manager) — free, auto-renews, attach directly to ALB
- Server Name Indication (SNI) — multiple certificates on one ALB listener

Redirect HTTP to HTTPS:

```text
Listener Rule: HTTP 80 -> redirect to HTTPS 443
```

## 5. Connection Draining (Deregistration Delay)

When a target deregisters (deployment, scale-in):
- ALB stops sending new requests to that target
- allows in-flight requests to complete for `deregistration_delay.timeout_seconds`
- default: 300 seconds

Set shorter for fast-deploys or long for stateful apps.

## 6. Health Checks

ALB health check config:

```text
Protocol: HTTP
Path: /health
Threshold: 3 healthy checks required
Interval: 30 seconds
Timeout: 5 seconds
```

Production rules:
- use a lightweight dedicated health endpoint (not the main API path)
- health endpoint should verify DB connectivity, cache connectivity
- do NOT use health endpoints that require authentication (bypasses auth check)

---

# Topic 3: Route 53

## 1. Routing Policies

| Policy | Behavior | Use Case |
|---|---|---|
| Simple | one record, one value | single endpoint |
| Weighted | route X% to record A, Y% to record B | canary deployments, blue-green |
| Latency | route to region with lowest latency | global apps |
| Failover | primary + failover endpoint | active-passive DR |
| Geolocation | route by user country/continent | data residency, localized content |
| Geoproximity | route by geographic distance with bias | fine-grained global routing |
| Multi-value | return up to 8 healthy records | basic load distribution |

## 2. Health Checks And Failover

Route 53 can check endpoint health and remove unhealthy records:

```text
Primary: ALB in us-east-1
Secondary: ALB in eu-west-1

If us-east-1 health check fails -> Route 53 routes to eu-west-1
When us-east-1 recovers -> traffic returns
```

DNS TTL trade-off:

```text
Low TTL (60s): faster failover, but more Route 53 queries (cost)
High TTL (300s): slower failover, caches hold stale records longer
```

## 3. Private Hosted Zones

Route 53 private hosted zones resolve DNS inside VPCs:

```text
api.internal -> ALB private IP
db.internal -> RDS private endpoint
```

Use for:
- internal service discovery
- predictable internal hostnames
- multi-account DNS with Route 53 Resolver rules

---

# Topic 4: CloudFront

## 1. What CloudFront Does

CloudFront is a global CDN with 400+ edge locations.

It caches content close to users and provides:
- lower latency for static assets
- reduced origin load
- DDoS protection via AWS Shield (standard is free)
- HTTPS termination at edge
- Lambda@Edge / CloudFront Functions for edge logic

## 2. Origins

CloudFront can front:
- S3 buckets (for static websites or media)
- ALB (for dynamic app traffic)
- API Gateway
- Custom HTTP origin (any HTTP server)

## 3. Origin Access Control (OAC)

For S3 origins, use OAC to prevent direct bucket access:

```text
CloudFront OAC:
  S3 bucket policy grants access to CloudFront distribution principal only
  S3 bucket blocks all public access
  Users can ONLY access S3 content through CloudFront
```

OAC replaced the older OAI (Origin Access Identity) and is the current best practice.

## 4. Cache Behaviors

Behaviors route different paths to different origins with different cache settings:

```text
Default behavior: /* -> S3 origin (static assets, cached)
/api/* -> ALB origin (dynamic, no cache or short TTL)
/images/* -> S3 origin (long cache TTL, 1 year)
```

Cache policies control:
- TTL (min, max, default)
- what headers/cookies/query strings affect the cache key

## 5. Lambda@Edge And CloudFront Functions

| Tool | Runtime | Location | Use Case |
|---|---|---|---|
| CloudFront Functions | JS subset | edge (ultra-fast) | URL rewrites, header manipulation, simple auth checks |
| Lambda@Edge | Node.js/Python | regional (4 events) | complex auth, A/B testing, personalization |

Lambda@Edge events:
- Viewer Request (before cache check)
- Origin Request (cache miss, before origin)
- Origin Response (from origin, before caching)
- Viewer Response (after cache, before user)

## 6. Signed URLs And Signed Cookies

Control access to private content:

| Method | Use When |
|---|---|
| Signed URL | restrict one file per URL (e.g., one download link) |
| Signed Cookie | restrict access to multiple files (e.g., video playlist) |

Use cases:
- paid content behind authentication
- time-limited download links
- user-specific private media

## 7. Common CloudFront Patterns

Pattern 1: Static SPA + API

```text
CloudFront
├── /* -> S3 (React build, long TTL, OAC)
└── /api/* -> ALB (no cache, dynamic)
```

Pattern 2: Video streaming

```text
CloudFront
├── /videos/* -> S3 (HLS segments, long TTL, signed cookies)
└── /api/* -> ALB (playlist generation, auth check)
```

## 8. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Expose S3 bucket publicly, skip CloudFront | use CloudFront + OAC, block direct S3 access |
| Cache /api/* responses | set Cache-Control: no-store or min TTL=0 for dynamic responses |
| Default CloudFront HTTP (no HTTPS) | always redirect HTTP to HTTPS, use ACM |
| One behavior for all paths | separate static vs dynamic with behaviors |
| Skip WAF on CloudFront | attach WAF web ACL to CloudFront for edge protection |

## 9. Revision Notes

- VPC: always 3 subnet tiers (public/private-app/private-data), multi-AZ
- Security groups are stateful; NACLs are stateless — do not forget ephemeral return ports
- NAT Gateway costs per GB: use S3/DynamoDB Gateway Endpoints to avoid NAT for AWS services
- ALB: Layer 7, path/host routing, weighted TG for canary; NLB: Layer 4, static IP
- Route 53 failover: primary + secondary health check, TTL matters for failover speed
- CloudFront: always OAC for S3, behaviors for routing, signed URLs for private content
- Transit Gateway for multi-VPC hub-and-spoke; PrivateLink for single-service private exposure

## 10. Official Source Notes

- VPC: <https://docs.aws.amazon.com/vpc/latest/userguide/what-is-amazon-vpc.html>
- ALB: <https://docs.aws.amazon.com/elasticloadbalancing/latest/application/introduction.html>
- Route 53: <https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/Welcome.html>
- CloudFront: <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Introduction.html>
- VPC Endpoints: <https://docs.aws.amazon.com/vpc/latest/privatelink/vpc-endpoints.html>
