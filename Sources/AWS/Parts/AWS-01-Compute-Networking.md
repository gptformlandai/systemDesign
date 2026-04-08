# AWS Interview Notes - Part 1: Compute + Networking

> Covers: EC2, ECS, EKS, Lambda, VPC, ELB, Route 53, CloudFront, API Gateway. Written for backend/platform interviews where the interviewer expects trade-offs, production reasoning, and architecture clarity rather than surface-level definitions.

---

# Table of Contents

1. [How to Think About AWS Compute](#1-how-to-think-about-aws-compute)
2. [EC2](#2-ec2)
3. [ECS](#3-ecs)
4. [EKS](#4-eks)
5. [Lambda](#5-lambda)
6. [VPC](#6-vpc)
7. [Elastic Load Balancing](#7-elastic-load-balancing)
8. [Route 53](#8-route-53)
9. [CloudFront](#9-cloudfront)
10. [API Gateway](#10-api-gateway)
11. [High-Value Comparisons](#11-high-value-comparisons)
12. [Architecture Walkthroughs](#12-architecture-walkthroughs)
13. [Common Interview Traps](#13-common-interview-traps)
14. [Rapid Revision Sheet](#14-rapid-revision-sheet)

---

# 1. How to Think About AWS Compute

Interviewers rarely care that you can list services. They care whether you can answer:

- Where should this workload run?
- How does traffic reach it?
- How does it scale?
- What fails if one AZ goes down?
- How do you secure east-west and north-south traffic?
- Why did you choose container orchestration vs serverless vs virtual machines?

The clean mental model:

| Need | AWS choice |
|---|---|
| Full OS control, custom runtime, legacy app | EC2 |
| Containers without managing Kubernetes | ECS |
| Containers with K8s portability/ecosystem | EKS |
| Event-driven short-lived functions | Lambda |
| Network isolation and routing | VPC |
| Traffic distribution | ELB |
| DNS and routing policies | Route 53 |
| Edge caching and CDN | CloudFront |
| Managed API front door | API Gateway |

The senior-level answer is always a trade-off answer, not a single-service answer.

---

# 2. EC2

## 2.1 What It Is

Amazon EC2 is a virtual machine service. You get compute instances with CPU, memory, network, and attached storage. You manage the OS, patching, runtime, scaling logic, and usually the application lifecycle.

## 2.2 When to Use It

Use EC2 when:

- you need full machine control
- you have legacy apps not container-ready
- you need specific agents, kernels, drivers, or custom networking behavior
- you want predictable long-running servers
- you are running self-managed databases, build agents, or stateful middleware

Avoid defaulting to EC2 when the team actually wants less operational overhead.

## 2.3 Instance Types

Common families:

- `t` series: burstable, cheap, dev/test, low steady load
- `m` series: general purpose
- `c` series: compute optimized
- `r` series: memory optimized
- `i` series: storage optimized
- `g`/`p` series: GPU

Interview angle:

- Do not pick `t` instances for sustained CPU-heavy production services unless you understand CPU credits.
- Right-size based on workload profile, not habit.

## 2.4 Pricing Models

- On-Demand: flexible, highest price
- Reserved Instances / Savings Plans: commit usage, cheaper
- Spot: very cheap, can be interrupted
- Dedicated Hosts/Instances: compliance or licensing use cases

Good answer:

```
Stateless background workers can use Spot with interruption handling.
Critical low-latency APIs usually run On-Demand or Savings Plans-backed capacity.
```

## 2.5 Auto Scaling

EC2 Auto Scaling Groups provide:

- desired capacity
- min/max capacity
- health checks
- scaling policies
- multi-AZ distribution

Typical triggers:

- CPU
- memory via CloudWatch custom metrics
- request count per target
- queue depth

## 2.6 Placement and Resilience

- Spread across multiple AZs
- Use launch templates
- Put instances behind a load balancer
- Bake AMIs or use bootstrapping carefully

## 2.7 Interviewer Hot Points

- Difference between instance store and EBS
- Why private subnets for app servers
- How patching is handled
- Why immutable deployments are better than in-place changes
- How to survive instance loss

## 2.8 Common Mistakes

- Running a single EC2 instance in one AZ for production
- SSH-ing into servers and changing them manually
- Storing important data on ephemeral instance store
- Using security groups too broadly

---

# 3. ECS

## 3.1 What It Is

Amazon ECS is a managed container orchestration service. It runs containers without requiring you to manage Kubernetes control-plane complexity.

Two launch types:

- `EC2 launch type`: you manage the EC2 nodes
- `Fargate launch type`: serverless containers, no node management

## 3.2 Core Concepts

- Cluster: logical grouping of capacity
- Task Definition: container spec, CPU/memory, ports, env vars, IAM role
- Task: running instance of a task definition
- Service: ensures desired number of tasks stays running

## 3.3 Why Teams Pick ECS

- simpler than EKS
- tightly integrated with AWS IAM, ALB, CloudWatch
- good for teams that want containers without Kubernetes expertise

## 3.4 ECS with Fargate

Best when:

- you want less ops
- workloads are stateless
- cluster management is not core competency
- startup time and cost model are acceptable

Trade-off:

- less low-level control than EC2-backed containers
- sometimes more expensive at scale than optimized EC2 fleets

## 3.5 ECS Interview Talking Points

- ECS vs EKS
- Fargate vs EC2 launch type
- task role vs instance role
- service auto scaling
- ALB path-based routing to ECS services

## 3.6 Strong Answer Example

```
For a Java microservice platform on AWS where we want containers but not Kubernetes operational overhead, ECS on Fargate is often the pragmatic default. We get service discovery, ALB integration, IAM task roles, and autoscaling with a lower control-plane burden.
```

---

# 4. EKS

## 4.1 What It Is

Amazon EKS is AWS-managed Kubernetes. AWS manages the Kubernetes control plane; you still manage worker nodes or use Fargate for pods.

## 4.2 When to Use It

Choose EKS when:

- the org is standardized on Kubernetes
- you need K8s APIs, CRDs, operators, Helm, service mesh, or ecosystem tools
- portability matters
- platform engineering already has K8s maturity

Do not choose EKS just because "Kubernetes is popular."

## 4.3 Operational Reality

Even though EKS is managed, you still own a lot:

- worker nodes or Fargate profiles
- cluster add-ons
- networking model
- ingress
- observability
- cost control
- pod security
- upgrade strategy

## 4.4 Node Groups

- Managed Node Groups: AWS-managed lifecycle for worker groups
- Self-managed nodes: more control, more operational burden
- Fargate on EKS: serverless pods for some workloads

## 4.5 EKS Networking

EKS uses VPC networking. Pods get VPC IPs through the AWS VPC CNI.

Implications:

- IP planning matters
- subnet exhaustion is a real problem
- security groups and network design need thought

## 4.6 What Interviewers Probe

- Why EKS over ECS?
- How ingress works
- How secrets/config are handled
- How rolling updates and autoscaling work
- How you secure the cluster
- How you observe and debug workloads

## 4.7 Senior-Level Trade-off

```
EKS gives maximum flexibility and ecosystem depth, but it is not lower effort than ECS. It is the right answer when Kubernetes itself is a requirement, not when containers alone are the requirement.
```

---

# 5. Lambda

## 5.1 What It Is

AWS Lambda is serverless compute. You upload code or container images, and AWS runs functions in response to events.

## 5.2 Best Use Cases

- API backends with moderate latency sensitivity
- event-driven processing
- file processing
- cron jobs
- queue consumers
- integration glue logic

## 5.3 When Lambda Is a Bad Fit

- very long-running workloads
- low-latency workloads sensitive to cold starts
- heavy stateful processing
- applications needing stable connections or specialized system-level control

## 5.4 Key Concepts

- Invocation types: synchronous, asynchronous, poll-based
- Concurrency: number of parallel executions
- Reserved concurrency: hard limit/protection
- Provisioned concurrency: reduce cold-start impact
- Timeout and memory settings affect performance

## 5.5 Cold Starts

Cold starts matter more for:

- JVM functions
- VPC-attached Lambdas if badly configured
- low-latency APIs

Mitigations:

- provisioned concurrency
- lighter runtime/package
- reduce initialization cost

## 5.6 Lambda and VPC

Lambda can run inside a VPC to access private resources, but that adds networking considerations and historically increased cold-start sensitivity.

## 5.7 Interviewer Favorites

- Lambda vs ECS/Fargate
- idempotency for async processing
- retries and DLQs
- limits and timeouts
- handling fan-out events

---

# 6. VPC

## 6.1 What It Is

Amazon VPC is your logically isolated network in AWS. It defines IP ranges, subnets, routing, gateways, and traffic controls.

Think of it as the network boundary inside which your AWS resources communicate.

## 6.2 Core Building Blocks

- VPC CIDR
- Subnets
- Route tables
- Internet Gateway
- NAT Gateway
- Security Groups
- Network ACLs
- VPC Endpoints

## 6.3 Public vs Private Subnets

Public subnet:

- route to Internet Gateway
- used for public-facing ALBs, bastions if unavoidable

Private subnet:

- no direct internet route
- used for app servers, ECS tasks, EKS nodes, RDS

Strong pattern:

```
Internet -> ALB in public subnets -> app tier in private subnets -> DB in private subnets
```

## 6.4 Security Groups vs NACLs

Security Groups:

- stateful
- attached to ENIs/resources
- allow rules only
- primary control in most architectures

NACLs:

- stateless
- attached at subnet level
- allow and deny rules
- lower-level coarse control

Interview answer:

```
Use security groups for resource-level traffic control.
Use NACLs sparingly when subnet-level deny logic is needed.
```

## 6.5 NAT Gateway

Purpose:

- allows private-subnet resources to access the internet outbound
- blocks inbound internet-initiated traffic

Example:

- private EC2 instance downloads patches through NAT Gateway

## 6.6 VPC Endpoints

Avoid public internet path for AWS services.

Two broad types:

- Gateway Endpoint: S3, DynamoDB
- Interface Endpoint: powered by PrivateLink for many AWS services

Great answer:

```
For private workloads talking to S3 or Secrets Manager, I prefer VPC endpoints to reduce exposure and avoid unnecessary NAT traffic.
```

## 6.7 Multi-AZ Network Design

- use at least two AZs
- create public/private subnets in each AZ
- route traffic locally where possible
- remember NAT Gateway is AZ-scoped for resilience design

## 6.8 VPC Interview Traps

- thinking private subnet means no outbound internet ever
- confusing security groups with NACLs
- ignoring CIDR planning
- putting databases in public subnets

---

# 7. Elastic Load Balancing

AWS offers multiple load balancers.

## 7.1 Application Load Balancer (ALB)

Layer 7 HTTP/HTTPS load balancer.

Use it for:

- host-based routing
- path-based routing
- HTTP header rules
- web apps, APIs, microservices
- ECS/EKS ingress patterns

## 7.2 Network Load Balancer (NLB)

Layer 4 TCP/UDP load balancer.

Use it for:

- very high performance
- static IP needs
- non-HTTP protocols
- TLS passthrough style use cases

## 7.3 Gateway Load Balancer

Used for inserting virtual appliances such as network firewalls.

Less commonly expected in general interviews unless the role is network-heavy.

## 7.4 Health Checks

Critical concept:

- load balancer only routes to healthy targets
- your health endpoint should reflect readiness, not just process alive

## 7.5 Interviewer Comparison

| Service | Layer | Best for |
|---|---|---|
| ALB | L7 | HTTP APIs, path/host routing |
| NLB | L4 | TCP/UDP, extreme performance, static IP |
| GWLB | specialized | network appliances |

## 7.6 Common Mistakes

- using NLB when advanced HTTP routing is required
- weak health checks
- single-AZ target groups

---

# 8. Route 53

## 8.1 What It Is

Route 53 is AWS DNS and traffic-routing service.

It provides:

- domain registration
- hosted zones
- DNS records
- health checks
- routing policies

## 8.2 Important Routing Policies

- Simple: one resource
- Weighted: split traffic by percentage
- Latency-based: route to lowest-latency region
- Failover: primary/secondary based on health
- Geolocation/Geoproximity: route by user geography

## 8.3 Interview Use Cases

- blue-green cutover with weighted records
- DR with failover routing
- multi-region active-active with latency routing

## 8.4 What Senior Candidates Mention

- DNS is not instant because of TTL
- Route 53 helps steer traffic but does not make an app multi-region by itself

---

# 9. CloudFront

## 9.1 What It Is

CloudFront is AWS CDN. It caches content at edge locations close to users.

## 9.2 Best Use Cases

- static asset delivery
- global APIs
- media/content distribution
- caching public or semi-public responses
- fronting S3 or ALB origins

## 9.3 Key Concepts

- Edge locations
- Origin
- Cache behavior
- TTLs
- Signed URLs / signed cookies
- Origin Access Control for S3

## 9.4 Strong Interview Points

- CloudFront reduces latency and origin load
- Dynamic content can still benefit through TLS termination and edge routing
- Cache invalidation is expensive relative to versioned asset strategy

## 9.5 Security Angle

- keep S3 private and let only CloudFront access it
- use WAF with CloudFront for edge protection

---

# 10. API Gateway

## 10.1 What It Is

Amazon API Gateway is a managed service for publishing APIs at scale. It handles request intake, routing, throttling, auth integration, and observability.

## 10.2 API Gateway Works Well For

- serverless APIs backed by Lambda
- lightweight service front doors
- request validation and throttling
- partner APIs

## 10.3 Common Features

- authentication/authorization integration
- request/response transformation
- usage plans and API keys
- throttling
- stage variables
- custom domains

## 10.4 REST vs HTTP API

Broad interview-safe answer:

- HTTP APIs are simpler and cheaper for many common use cases
- REST APIs have broader feature depth in some scenarios

## 10.5 API Gateway vs ALB

Use API Gateway when:

- you need managed API concerns such as throttling, usage plans, authorizers, request validation
- Lambda is the backend

Use ALB when:

- you are routing to containers or instances over HTTP
- you want simpler, cheaper L7 load balancing without full API-management features

---

# 11. High-Value Comparisons

## 11.1 EC2 vs ECS vs EKS vs Lambda

| Service | You manage | Best when |
|---|---|---|
| EC2 | OS, runtime, scaling, patching | full control or legacy apps |
| ECS | app containers, some cluster choices | AWS-native containers with low ops |
| EKS | K8s workloads and platform ops | Kubernetes ecosystem required |
| Lambda | function code only | event-driven or bursty serverless |

## 11.2 ALB vs API Gateway

| Need | Better fit |
|---|---|
| Container/instance HTTP routing | ALB |
| managed API features and serverless front door | API Gateway |

## 11.3 ECS vs EKS

| Question | ECS | EKS |
|---|---|---|
| Operational simplicity | better | worse |
| Kubernetes portability | low | high |
| AWS-native ease | high | medium |
| ecosystem flexibility | medium | high |

---

# 12. Architecture Walkthroughs

## 12.1 Standard Web App

```
Users
  ->
Route 53
  ->
CloudFront
  ->
ALB
  ->
ECS / EKS / EC2 app tier in private subnets
  ->
RDS / ElastiCache / S3
```

Why it is strong:

- edge caching
- TLS termination
- multi-AZ resilience
- private app and data tiers

## 12.2 Serverless API

```
Client
  ->
Route 53
  ->
API Gateway
  ->
Lambda
  ->
DynamoDB / SQS / EventBridge
```

Why it is strong:

- no servers
- event-driven
- easy burst handling

## 12.3 Kubernetes Platform

```
Route 53
  ->
ALB Ingress
  ->
EKS services across multiple AZs
  ->
RDS, S3, internal services
```

Mention:

- IAM roles for service accounts
- autoscaling
- observability
- subnet/IP planning

---

# 13. Common Interview Traps

## Trap 1

"We need containers, so EKS."

Correct thinking:

Containers do not automatically imply Kubernetes. ECS is often enough.

## Trap 2

"Private subnet means no internet access."

Correct thinking:

Private subnet means no direct route from the internet. Outbound access can still happen via NAT Gateway or VPC endpoints.

## Trap 3

"ALB and API Gateway are the same."

Correct thinking:

ALB is load balancing. API Gateway is managed API front-door functionality.

## Trap 4

"Lambda is always cheaper."

Correct thinking:

Not for high-throughput steady-state workloads where containers or EC2 may be more cost-efficient.

## Trap 5

"Multi-AZ means multi-region."

Correct thinking:

No. Multi-AZ improves regional resilience. Multi-region is a bigger architecture decision.

## Trap 6

"VPC Peering is how we connect everything."

Correct thinking:

VPC Peering is point-to-point and doesn't scale. Transit Gateway is the hub-spoke model for many VPCs.

---

# 14. Global Accelerator

## 14.1 What It Is

AWS Global Accelerator provides static anycast IP addresses that route traffic to optimal AWS endpoints via the AWS global network backbone.

Unlike CloudFront which is a CDN (caches content), Global Accelerator is a **network accelerator** (routes packets faster).

## 14.2 When to Use Global Accelerator

```
Use Global Accelerator when:
  ✦ You need static IP addresses (whitelisting, compliance)
  ✦ Non-HTTP protocols: TCP, UDP, gRPC, gaming, IoT
  ✦ Low-latency global routing WITHOUT caching
  ✦ Instant regional failover (health-check based)
  ✦ Multi-region active-active with single entry point

Use CloudFront when:
  ✦ HTTP/HTTPS traffic
  ✦ Content needs caching (static assets, API responses)
  ✦ Web applications, media delivery
  ✦ WAF integration at edge
```

## 14.3 How It Works

```
                      ┌─────────────────────────────────────┐
                      │     2 Static Anycast IPs            │
                      │     (Global Accelerator)            │
                      └───────────────┬─────────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
  AWS Edge Location          AWS Edge Location           AWS Edge Location
   (closest to user)                                         
        │
        │  AWS Private Backbone (not public internet)
        │
        ▼
  ┌──────────────────────────────────────────────────────────────────┐
  │  Regional Endpoint Groups                                        │
  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐           │
  │  │ us-east-1   │    │ eu-west-1   │    │ ap-south-1  │           │
  │  │ ALB/NLB/EC2 │    │ ALB/NLB/EC2 │    │ ALB/NLB/EC2 │           │
  │  │ weight: 50% │    │ weight: 30% │    │ weight: 20% │           │
  │  └─────────────┘    └─────────────┘    └─────────────┘           │
  └──────────────────────────────────────────────────────────────────┘
```

## 14.4 Global Accelerator vs CloudFront

| Aspect | Global Accelerator | CloudFront |
|---|---|---|
| Layer | L4 (TCP/UDP) | L7 (HTTP/HTTPS) |
| Caching | No | Yes |
| Static IPs | Yes (2 anycast IPs) | No (DNS-based) |
| Protocol support | Any TCP/UDP | HTTP/HTTPS only |
| Use case | Low-latency routing, failover | Content delivery, caching |
| WAF | No | Yes |
| Price model | Fixed + data transfer | Request + data transfer |

## 14.5 Interview Gold Answer

```
"For a global gRPC service that needs static IPs and instant failover, 
I'd use Global Accelerator. For a web app serving static assets globally, 
I'd use CloudFront. They solve different problems — routing vs caching."
```

---

# 15. VPC Connectivity — Transit Gateway, Peering, PrivateLink

This is a **high-value architect topic**. Sushree (SA-certified) will likely probe here.

## 15.1 The Problem

How do you connect:
- Multiple VPCs in the same region?
- VPCs across regions?
- VPCs to on-premises data centers?
- Your services to other AWS accounts without exposing to internet?

## 15.2 VPC Peering

```
What it is:
  Direct network connection between two VPCs.
  Traffic stays on AWS backbone, never hits internet.

How it works:
  VPC-A <----peering----> VPC-B
  Both must accept the peering request.
  Route tables must be updated in both VPCs.

Limitations:
  ✗ NOT transitive: A↔B and B↔C does NOT mean A can talk to C
  ✗ CIDR ranges must not overlap
  ✗ Doesn't scale well (n VPCs = n(n-1)/2 peerings)
  ✗ Cross-region peering has data transfer costs

When to use:
  ✓ Simple two-VPC connectivity
  ✓ Low number of VPCs
  ✓ Direct, high-bandwidth communication needed
```

## 15.3 Transit Gateway

```
What it is:
  Regional network hub that connects VPCs and on-premises networks.
  Hub-and-spoke model — all VPCs connect to the TGW, not to each other.

How it works:
                    ┌──────────────────┐
                    │  Transit Gateway │
                    └────────┬─────────┘
            ┌────────────────┼────────────────┐
            │                │                │
         VPC-A            VPC-B            VPC-C
                            │
                     VPN / Direct Connect
                            │
                       On-Premises

Benefits:
  ✓ Transitive routing (A can reach C through TGW)
  ✓ Scales to thousands of VPCs
  ✓ Centralized routing tables
  ✓ Supports VPN and Direct Connect attachments
  ✓ Cross-region peering via TGW peering

When to use:
  ✓ 10+ VPCs that need interconnection
  ✓ Hub-and-spoke topology
  ✓ Hybrid cloud (AWS + on-prem)
  ✓ Centralized egress/ingress through shared services VPC
```

## 15.4 AWS PrivateLink

```
What it is:
  Expose a service from your VPC to other VPCs/accounts 
  WITHOUT VPC peering, WITHOUT public internet.
  Traffic stays entirely within AWS network.

How it works:
  ┌─────────────────────────────────────────────────────────────┐
  │ Service Provider VPC (Account A)                            │
  │                                                             │
  │   ┌─────────────┐                                           │
  │   │  Your App   │ ← NLB required (or GWLB)                  │
  │   │  (ECS/EC2)  │                                           │
  │   └──────┬──────┘                                           │
  │          │                                                  │
  │   ┌──────▼──────┐                                           │
  │   │ VPC Endpoint│ ← Endpoint Service                        │
  │   │   Service   │                                           │
  │   └─────────────┘                                           │
  └─────────────────────────────────────────────────────────────┘
                         │
              PrivateLink (AWS backbone)
                         │
  ┌─────────────────────────────────────────────────────────────┐
  │ Consumer VPC (Account B)                                    │
  │                                                             │
  │   ┌─────────────┐                                           │
  │   │ Interface   │ ← Gets private IP in consumer VPC         │
  │   │ Endpoint    │                                           │
  │   └──────┬──────┘                                           │
  │          │                                                  │
  │   ┌──────▼──────┐                                           │
  │   │ Consumer    │                                           │
  │   │ Application │                                           │
  │   └─────────────┘                                           │
  └─────────────────────────────────────────────────────────────┘

When to use:
  ✓ SaaS provider exposing service to customers' VPCs
  ✓ Shared services across accounts without full VPC connectivity
  ✓ Compliance: no data over internet, no CIDR overlap concerns
  ✓ Accessing AWS services privately (S3, DynamoDB, Secrets Manager)

Key points:
  → Consumer gets a private IP (ENI) in their VPC
  → No CIDR overlap issues
  → Unidirectional: consumer initiates to provider
  → Provider controls who can connect (allowlist)
```

## 15.5 Comparison Matrix

| Aspect | VPC Peering | Transit Gateway | PrivateLink |
|---|---|---|---|
| Topology | Point-to-point | Hub-and-spoke | Service-to-consumer |
| Transitivity | No | Yes | N/A (unidirectional) |
| Scale | Low (mesh explosion) | High (1000s of VPCs) | High |
| CIDR overlap | Not allowed | Not allowed | Allowed |
| Cross-account | Yes | Yes | Yes |
| Cross-region | Yes (with cost) | Yes (TGW peering) | No |
| On-prem connectivity | No | Yes (VPN/DX) | No |
| Use case | Simple 2-VPC | Enterprise multi-VPC | Expose service privately |

## 15.6 Interview Decision Tree

```
"How do I connect VPCs?"

  Is it just 2-3 VPCs directly communicating?
    → VPC Peering

  Is it many VPCs that all need to talk to each other + on-prem?
    → Transit Gateway

  Am I exposing a SERVICE to consumers without giving full network access?
    → PrivateLink

  Do I need private access to AWS services like S3?
    → VPC Endpoints (Gateway for S3/DynamoDB, Interface for others)
```

---

# 16. IRSA — IAM Roles for Service Accounts (EKS)

If Sushree asks about EKS + IAM, this is **the key topic**.

## 16.1 The Problem

In Kubernetes, pods need to call AWS APIs (S3, DynamoDB, Secrets Manager, etc.).

Old approach: Attach IAM role to EC2 worker node.

```
Problem with node-level IAM:
  Every pod on that node gets the same permissions.
  Pod A (needs S3) and Pod B (needs DynamoDB) both get S3 + DynamoDB.
  This violates least privilege.
```

## 16.2 IRSA Solution

IRSA = IAM Roles for Service Accounts

```
How it works:
  1. Create an IAM role with specific permissions
  2. Create a Kubernetes ServiceAccount
  3. Annotate the ServiceAccount with the IAM role ARN
  4. Pod uses that ServiceAccount
  5. AWS SDK in the pod automatically assumes the role via OIDC

  Pod → K8s ServiceAccount → OIDC → IAM Role → AWS Resources

Result:
  ✓ Pod-level IAM permissions
  ✓ Least privilege per workload
  ✓ No credentials stored in pod
  ✓ Automatic credential rotation
```

## 16.3 Setup Flow

```yaml
# 1. Create IAM role with trust policy for EKS OIDC provider
#    Trust policy allows the specific ServiceAccount to assume this role

# 2. Create Kubernetes ServiceAccount with annotation
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app-sa
  namespace: production
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789:role/my-app-role

# 3. Use ServiceAccount in Pod/Deployment
spec:
  serviceAccountName: my-app-sa
  containers:
    - name: my-app
      image: my-app:latest
      # AWS SDK automatically uses the IRSA credentials
```

## 16.4 Interview Gold Answer

```
"For EKS workloads accessing AWS services, I use IRSA to provide 
pod-level IAM permissions. Each microservice gets its own ServiceAccount 
annotated with a specific IAM role, following least privilege. The pod 
gets temporary credentials via OIDC — no hardcoded keys, automatic rotation."
```

## 16.5 Alternative: EKS Pod Identity (newer)

```
AWS introduced EKS Pod Identity as a simpler alternative to IRSA.
- No OIDC provider setup needed
- Uses EKS Pod Identity Agent (DaemonSet)
- Simpler IAM trust policy

If asked: "I'm aware of EKS Pod Identity as the newer approach, 
but IRSA is still widely used and I'm comfortable with both."
```

---

# 17. Cost Awareness (Sushree + Surendra will appreciate this)

Surendra has a FinOps certification. Cost awareness is a differentiator.

## 17.1 Cross-AZ Data Transfer

```
THE HIDDEN COST MOST DEVELOPERS MISS:

  Same AZ:     Free (within VPC)
  Cross-AZ:    $0.01/GB each direction ($0.02 round trip)
  Cross-Region: $0.02/GB+ (varies by region)
  Internet:    $0.09/GB out (first 10TB)

Example:
  Service A (AZ-a) → Service B (AZ-b): 100GB/day
  Cost: 100GB × $0.02 × 30 days = $60/month just for one service pair
  
  10 services doing this = $600/month in hidden data transfer

Mitigation strategies:
  ✓ Co-locate chatty services in same AZ (but reduces HA)
  ✓ Use caching to reduce cross-AZ calls
  ✓ Compress payloads
  ✓ Batch requests instead of chatty calls
  ✓ Use VPC endpoints to avoid NAT Gateway data charges
```

## 17.2 NAT Gateway Costs

```
NAT Gateway pricing:
  $0.045/hour (~$32/month just to exist)
  + $0.045/GB processed

For high-traffic private workloads:
  1TB/month through NAT = $45 processing + $32 hourly = $77/month PER AZ

Cost-saving alternatives:
  ✓ VPC Endpoints for AWS services (S3, DynamoDB, Secrets Manager)
      → No NAT Gateway needed for those calls
  ✓ NAT Instance (self-managed, cheaper, less resilient)
  ✓ Single NAT Gateway if HA is not critical (dev/test)
```

## 17.3 Load Balancer Costs

```
ALB: $0.0225/hour + LCU charges
NLB: $0.0225/hour + LCU charges (but different LCU calculation)

Hidden cost: too many ALBs
  10 ALBs = $162/month just in hourly charges

Solution:
  Consolidate with path-based routing on fewer ALBs
  Use K8s Ingress to share one ALB across services
```

## 17.4 Lambda vs Fargate vs EC2 Cost Crossover

```
Rule of thumb:

  Low traffic / bursty     → Lambda cheapest
  Medium steady traffic    → Fargate often cheaper
  High steady traffic      → EC2 with Reserved/Savings Plans cheapest

Crossover point (rough):
  Lambda becomes expensive around 1M+ requests/day with moderate duration
  
Interview answer:
  "I'd start with Lambda for the event-driven workload, monitor costs,
   and consider moving to Fargate if we see sustained high utilization
   where Lambda's per-request pricing becomes expensive."
```

## 17.5 Interview Cost Question Pattern

```
Q: "How would you optimize costs for this architecture?"

Strong answer structure:
  1. Right-size compute (check utilization, downsize over-provisioned)
  2. Reserved capacity / Savings Plans for predictable workloads
  3. Spot for fault-tolerant workloads
  4. VPC Endpoints to avoid NAT charges
  5. Consolidate load balancers
  6. Review data transfer patterns (cross-AZ, cross-region)
  7. S3 lifecycle policies and intelligent tiering
  8. Turn off dev/test resources after hours
```

---

# 18. Rapid Revision Sheet

## Services in One Line

- `EC2`: virtual machines with maximum control
- `ECS`: AWS-managed container orchestration
- `EKS`: AWS-managed Kubernetes
- `Lambda`: event-driven serverless compute
- `VPC`: isolated network boundary
- `ALB`: L7 HTTP/HTTPS load balancer
- `NLB`: L4 TCP/UDP load balancer
- `Route 53`: DNS and traffic routing
- `CloudFront`: CDN and edge caching (L7, HTTP)
- `Global Accelerator`: network routing with static IPs (L4, TCP/UDP)
- `API Gateway`: managed API front door
- `Transit Gateway`: hub for connecting multiple VPCs and on-prem
- `PrivateLink`: expose services privately across accounts/VPCs
- `IRSA`: pod-level IAM for EKS workloads

## Questions You Must Be Able to Answer

- Why not ECS instead of EKS?
- Why private subnets for app and DB tiers?
- Why ALB instead of NLB?
- Why API Gateway instead of ALB?
- How would you make this multi-AZ?
- How would you reduce latency for global users?
- How would you expose an internal service securely?
- When Transit Gateway vs VPC Peering vs PrivateLink?
- How do EKS pods get IAM permissions? (IRSA)
- What are hidden costs in AWS architectures?
- When Global Accelerator vs CloudFront?

## Decision Quick Reference

```
Containers but don't need K8s ecosystem?    → ECS
Need K8s APIs, CRDs, Helm, operators?       → EKS
Event-driven, bursty, short-lived?          → Lambda
Full OS control, legacy, compliance?        → EC2

HTTP routing, path/host based?              → ALB
TCP/UDP, static IP, extreme perf?           → NLB
gRPC global routing, instant failover?      → Global Accelerator
HTTP caching at edge?                       → CloudFront

2-3 VPCs direct connection?                 → VPC Peering
Many VPCs + on-prem hub-spoke?              → Transit Gateway
Expose service without network peering?     → PrivateLink
```

## Gold Standard Sentence

```
My AWS compute choice depends on how much runtime control we need, 
how much operational burden the team can absorb, and whether the 
architecture is request-driven, event-driven, or container-platform 
oriented. For networking, I consider traffic patterns, security 
boundaries, cost implications, and whether we need caching, routing, 
or private connectivity.
```

