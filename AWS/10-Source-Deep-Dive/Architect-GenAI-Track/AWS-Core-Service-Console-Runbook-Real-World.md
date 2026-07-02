# AWS Core Service Console Runbook: Real-World Click Paths and Impact

> Goal: know where to click in AWS Console, what each setting creates, and how it affects production behavior.

---

# Index

| Section | Focus |
|---|---|
| [0. How To Use This Runbook](#0-how-to-use-this-runbook) | How To Use This Runbook |
| [1. EC2: Launch A Server](#1-ec2-launch-a-server) | EC2: Launch A Server |
| [2. VPC: Create Network Boundary](#2-vpc-create-network-boundary) | VPC: Create Network Boundary |
| [3. Security Group: Allow Traffic](#3-security-group-allow-traffic) | Security Group: Allow Traffic |
| [4. ALB: Public Entry To Backend](#4-alb-public-entry-to-backend) | ALB: Public Entry To Backend |
| [5. ECS Fargate: Deploy Container Service](#5-ecs-fargate-deploy-container-service) | ECS Fargate: Deploy Container Service |
| [6. EKS: Kubernetes On AWS](#6-eks-kubernetes-on-aws) | EKS: Kubernetes On AWS |
| [7. Lambda: Event Function](#7-lambda-event-function) | Lambda: Event Function |
| [8. S3: Store Objects](#8-s3-store-objects) | S3: Store Objects |
| [9. RDS: Relational Database](#9-rds-relational-database) | RDS: Relational Database |
| [10. DynamoDB: Key-Value / Document Store](#10-dynamodb-key-value-document-store) | DynamoDB: Key-Value / Document Store |
| [11. SQS: Queue Work](#11-sqs-queue-work) | SQS: Queue Work |
| [12. SNS: Fan-Out Events](#12-sns-fan-out-events) | SNS: Fan-Out Events |
| [13. EventBridge: Event Routing](#13-eventbridge-event-routing) | EventBridge: Event Routing |
| [14. CloudWatch: Observe The System](#14-cloudwatch-observe-the-system) | CloudWatch: Observe The System |
| [15. CloudTrail: Audit Actions](#15-cloudtrail-audit-actions) | CloudTrail: Audit Actions |
| [16. Secrets Manager: Store Secrets](#16-secrets-manager-store-secrets) | Secrets Manager: Store Secrets |
| [17. Bedrock: Enable GenAI Model](#17-bedrock-enable-genai-model) | Bedrock: Enable GenAI Model |
| [18. Final Production Click Rule](#18-final-production-click-rule) | Final Production Click Rule |

---

## 0. How To Use This Runbook

Use this when you know the service but feel stuck in the UI.

Format:

```text
Console path
  -> what to click
  -> what it changes
  -> what can go wrong
  -> production check
```

Use:

```text
https://console.aws.amazon.com
```

not retail `amazon.com`.

---

## 1. EC2: Launch A Server

### Console Path

```text
AWS Console -> Search "EC2" -> Instances -> Launch instances
```

### Key Clicks And Impact

```text
Name:
  human-readable resource identity.

AMI:
  operating system and pre-baked software base.

Instance type:
  CPU/memory/network capacity and cost.

Key pair:
  SSH access path. Avoid SSH-heavy production ops when possible.

Network settings:
  VPC, subnet, public IP, security group.

Storage:
  EBS root volume size/type/encryption.

IAM instance profile:
  AWS permissions available to apps/agents on instance.

User data:
  bootstrap script run at first launch.
```

### What Can Go Wrong

```text
public IP on app server
security group open to 0.0.0.0/0 on SSH
no IAM role so apps use static keys
data stored only on instance disk
manual changes not captured in AMI/IaC
```

### Production Check

```text
EC2 in private subnet behind ALB.
SSM Session Manager instead of SSH where possible.
IAM role scoped to exact needs.
EBS encrypted.
CloudWatch agent/logging configured.
```

### Subnet Placement Clarification

```text
An EC2 instance's primary subnet is selected at launch.
You do not attach a subnet later the way you attach EBS storage.
```

Console path:

```text
EC2 -> Instances -> Launch instances -> Network settings -> Edit
```

Impact:

```text
VPC:
  selects the isolated network.

Subnet:
  selects the AZ and public/private segment.

Auto-assign public IP:
  decides whether the instance can get direct public internet identity.

Security group:
  decides allowed traffic.
```

If launched in the wrong subnet:

```text
Create AMI or use launch template and relaunch in the correct subnet.
For special cases, attach a secondary ENI in another subnet in the same AZ.
You cannot move the primary ENI to another subnet/AZ on the same running instance.
```

---

## 2. VPC: Create Network Boundary

### Console Path

```text
AWS Console -> Search "VPC" -> Your VPCs -> Create VPC
```

Choose:

```text
VPC and more
number of AZs
public subnets
private subnets
NAT gateways
VPC endpoints
```

### Key Clicks And Impact

```text
CIDR:
  IP range for the network. Hard to change later.

AZ count:
  resilience boundary.

Public subnet:
  route to Internet Gateway.

Private subnet:
  no direct internet inbound route.

NAT Gateway:
  outbound internet for private subnets.

VPC endpoints:
  private access to AWS services, can reduce NAT use.
```

### What Can Go Wrong

```text
CIDR overlaps with other VPC/on-prem
single-AZ design
private subnet missing route to NAT/endpoints
one NAT for multi-AZ production
```

### Production Check

```text
At least 2 AZs.
ALB public, app private, DB private.
Route tables intentionally designed.
S3 gateway endpoint enabled for private workloads.
```

---

## 3. Security Group: Allow Traffic

### Console Path

```text
EC2 -> Security Groups -> Create security group
```

### Key Clicks And Impact

```text
Inbound rule:
  who can initiate traffic into resource.

Outbound rule:
  where resource can initiate traffic.

Source as security group:
  allows traffic from resources with that SG, not from fixed IPs.
```

### Example

```text
ALB SG:
  inbound 443 from 0.0.0.0/0

Backend SG:
  inbound 8080 from ALB SG only

RDS SG:
  inbound 5432 from Backend SG only
```

### What Can Go Wrong

```text
RDS open to internet
SSH open to everyone
backend public when it should only trust ALB
```

---

## 4. ALB: Public Entry To Backend

### Console Path

```text
EC2 -> Load Balancers -> Create load balancer -> Application Load Balancer
```

### Key Clicks And Impact

```text
Internet-facing:
  public entry point.

Internal:
  private load balancer inside VPC.

Listeners:
  ports/protocols accepted by ALB.

Certificate:
  TLS termination through ACM.

Target group:
  backend instances/tasks receiving traffic.

Health check:
  decides which targets are safe for traffic.
```

### What Can Go Wrong

```text
health check points to wrong path
backend SG does not allow ALB SG
ALB in one subnet/AZ only
HTTP without HTTPS redirect
```

### Production Check

```text
HTTPS listener.
ACM certificate.
Targets in multiple AZs.
Health check matches readiness, not just process alive.
```

---

## 5. ECS Fargate: Deploy Container Service

### Console Path

```text
AWS Console -> Search "ECS" -> Clusters -> Create cluster
ECS -> Task definitions -> Create new task definition
ECS -> Cluster -> Services -> Create
```

### Key Clicks And Impact

```text
Cluster:
  logical home for services/tasks.

Task definition:
  container image, CPU, memory, ports, env, secrets, logs.

Execution role:
  ECS pulls image and writes logs.

Task role:
  app inside container calls AWS services.

Service:
  keeps desired number of tasks running.

Networking:
  subnets and security groups for tasks.

Load balancer:
  routes user traffic to tasks.
```

### What Can Go Wrong

```text
confusing execution role and task role
tasks in public subnets unnecessarily
no CloudWatch log driver
secrets stored as plain env vars
health check start period too short
```

### Production Check

```text
private subnets
ALB target group
Secrets Manager injection
CloudWatch logs
deployment circuit breaker
autoscaling
```

---

## 6. EKS: Kubernetes On AWS

### Console Path

```text
AWS Console -> Search "EKS" -> Clusters -> Create cluster
```

### Key Clicks And Impact

```text
Cluster IAM role:
  EKS control plane permissions.

VPC/subnets:
  where control plane connects and worker nodes run.

Endpoint access:
  public/private Kubernetes API access.

Node group:
  EC2 worker capacity.

Add-ons:
  VPC CNI, CoreDNS, kube-proxy, EBS CSI, etc.
```

### What Can Go Wrong

```text
no IP planning for pods
public endpoint too open
nodes over-permissioned
no IRSA/pod identity
missing cluster autoscaler/Karpenter strategy
```

### Production Check

```text
private workloads
IRSA or EKS Pod Identity
managed node groups or Fargate profiles
ALB Ingress Controller
observability add-ons
network policy strategy if needed
```

---

## 7. Lambda: Event Function

### Console Path

```text
AWS Console -> Search "Lambda" -> Functions -> Create function
```

### Key Clicks And Impact

```text
Runtime:
  language/runtime environment.

Execution role:
  permissions function has.

Memory:
  CPU and cost scale with memory.

Timeout:
  max runtime duration.

Trigger:
  event source such as API Gateway, SQS, S3, EventBridge.

VPC:
  required only if function needs private VPC resources.
```

### What Can Go Wrong

```text
using Lambda for long-running main API
VPC Lambda without NAT/endpoints
timeout too low
execution role too broad
no DLQ/on-failure destination for async processing
```

---

## 8. S3: Store Objects

### Console Path

```text
AWS Console -> Search "S3" -> Buckets -> Create bucket
```

### Key Clicks And Impact

```text
Bucket name:
  global unique name.

Region:
  where objects live.

Block Public Access:
  prevents accidental public exposure.

Versioning:
  keeps old object versions.

Encryption:
  protects at rest.

Lifecycle:
  moves/deletes old objects based on rules.
```

### What Can Go Wrong

```text
public bucket
no versioning for critical docs
using S3 as relational DB
no lifecycle policy for logs
```

### Production Check

```text
Block Public Access on.
SSE-S3 or SSE-KMS.
Versioning for important data.
Lifecycle rules.
Access through IAM/presigned URLs/CloudFront as appropriate.
```

---

## 9. RDS: Relational Database

### Console Path

```text
AWS Console -> Search "RDS" -> Databases -> Create database
```

### Key Clicks And Impact

```text
Engine:
  PostgreSQL, MySQL, Aurora, etc.

Template:
  dev/test vs production defaults.

Multi-AZ:
  availability/failover.

Storage autoscaling:
  prevents storage-full incidents.

Credentials:
  DB auth setup; prefer Secrets Manager integration where available.

VPC/subnet group:
  network placement.

Public access:
  should usually be No.

Backup retention:
  PITR window.

Deletion protection:
  prevents accidental deletion.
```

### What Can Go Wrong

```text
public RDS
no backups
no Multi-AZ for production
security group too broad
no Performance Insights
```

---

## 10. DynamoDB: Key-Value / Document Store

### Console Path

```text
AWS Console -> Search "DynamoDB" -> Tables -> Create table
```

### Key Clicks And Impact

```text
Partition key:
  determines data distribution and primary access path.

Sort key:
  enables ordered/range queries within partition.

Capacity mode:
  on-demand vs provisioned.

PITR:
  point-in-time restore.

Global secondary index:
  alternate query pattern.

Streams:
  change events for async processing.
```

### What Can Go Wrong

```text
bad partition key creates hot partition
using scan instead of query
adding GSIs without cost awareness
modeling relational queries in DynamoDB
```

---

## 11. SQS: Queue Work

### Console Path

```text
AWS Console -> Search "SQS" -> Queues -> Create queue
```

### Key Clicks And Impact

```text
Standard vs FIFO:
  throughput/ordering trade-off.

Visibility timeout:
  time message is hidden while consumer processes.

Message retention:
  how long unprocessed messages remain.

DLQ:
  where poison messages go.

Redrive policy:
  max receive count before DLQ.
```

### What Can Go Wrong

```text
visibility timeout shorter than processing time
no DLQ
consumer not idempotent
FIFO used when ordering not needed
```

---

## 12. SNS: Fan-Out Events

### Console Path

```text
AWS Console -> Search "SNS" -> Topics -> Create topic
```

### Key Clicks And Impact

```text
Topic type:
  standard or FIFO.

Subscription:
  endpoint receiving published messages.

SQS subscription:
  durable fan-out pattern.

Filter policy:
  subscriber receives only matching messages.
```

### Production Check

```text
Use SNS -> SQS for critical consumers.
Avoid direct HTTP subscription for critical events unless retry/dead-letter behavior is designed.
```

---

## 13. EventBridge: Event Routing

### Console Path

```text
AWS Console -> Search "EventBridge" -> Event buses -> Rules -> Create rule
```

### Key Clicks And Impact

```text
Event bus:
  event domain/boundary.

Event pattern:
  content-based routing rule.

Target:
  Lambda, SQS, Step Functions, API destination, etc.

Retry/DLQ:
  failure handling for target delivery.
```

### What Can Go Wrong

```text
event pattern too broad
no DLQ
target role missing permissions
schema not versioned
```

---

## 14. CloudWatch: Observe The System

### Console Path

```text
AWS Console -> Search "CloudWatch"
```

Use:

```text
Metrics
Logs
Alarms
Dashboards
Logs Insights
```

### Key Clicks And Impact

```text
Alarm:
  watches metric and sends notification/action.

Dashboard:
  shared operational view.

Logs Insights:
  query logs during debugging.

Metric filter:
  converts log pattern into metric.
```

### Production Check

```text
Alarm on symptoms:
  5xx, latency, queue age, DLQ count, saturation.

Not only causes:
  CPU alone is not enough.
```

---

## 15. CloudTrail: Audit Actions

### Console Path

```text
AWS Console -> Search "CloudTrail" -> Event history
```

For organization trail:

```text
CloudTrail -> Trails -> Create trail -> Apply to all accounts
```

### Key Clicks And Impact

```text
Event history:
  recent API activity lookup.

Trail:
  durable delivery to S3/CloudWatch.

Organization trail:
  captures events across accounts.

Log file validation:
  helps detect tampering.
```

### Production Check

```text
Organization trail to log archive account.
Multi-region enabled.
Restricted deletion.
```

---

## 16. Secrets Manager: Store Secrets

### Console Path

```text
AWS Console -> Search "Secrets Manager" -> Store a new secret
```

### Key Clicks And Impact

```text
Secret type:
  database credentials, API key, custom secret.

KMS key:
  encryption key.

Rotation:
  automatic secret rotation where supported.

Resource permissions:
  who can read/manage secret.
```

### What Can Go Wrong

```text
secret value logged
wildcard read access
secret baked into Docker image
rotation enabled but app cannot handle change
```

---

## 17. Bedrock: Enable GenAI Model

### Console Path

```text
AWS Console -> Search "Bedrock" -> Model access -> Modify model access
```

### Key Clicks And Impact

```text
Provider/model selection:
  enables specific foundation model invocation in account/region.

Approval:
  controls what apps can use.
```

### Production Check

```text
Only approved models.
Budgets and tags.
Guardrails.
Prompt/eval process.
```

---

## 18. Final Production Click Rule

Before clicking `Create`, `Save`, `Deploy`, or `Delete`, ask:

```text
What resource changes?
Who gets access?
What network path opens?
What cost starts?
What logs/audit prove it?
What is rollback?
Is this managed by IaC?
```
