# AWS Enterprise Hands-On Build: GenAI Social + Streaming + Messaging Platform

> Role model: Principal AWS Solutions Architect.  
> System: GenAI-powered Social + Streaming + Messaging Platform.  
> Product shape: Instagram + YouTube + TikTok + WhatsApp.  
> Goal: learn AWS by building a production-style, billion-user system.

---

## How To Use This Document

- Build in small slices.
- Use AWS Console first to understand what each resource does.
- Convert the same design to Terraform once the click path is clear.
- Do not deploy everything at once.
- Keep every service tagged with:
  - `Project = genai-social-platform`
  - `Environment = dev`
  - `Owner = your-name`
  - `CostCenter = learning`

Important:

```text
Use https://console.aws.amazon.com for AWS Console.
Do not use retail amazon.com.
```

Production warning:

```text
This is a learning blueprint for an enterprise-grade architecture.
This is conceptual -- here's what you must verify before production:
- account strategy and SCPs
- region availability for each AWS service
- service quotas
- security review
- data classification
- privacy/compliance requirements
- cost budgets
- disaster recovery requirements
- load testing results
```

---

# Index

| Section | What to use it for | Main outcome |
|---|---|---|
| [1. System Overview](#1-system-overview) | Understand product scope, scale, architecture, and major flows | Big-picture map of the platform |
| [2. AWS Service Selection](#2-aws-service-selection) | Learn why each AWS service is chosen | Service decision-making muscle |
| [3. Networking](#3-networking) | Design VPC, subnets, routing, endpoints, and security groups | Secure network foundation |
| [4. Step-By-Step Deployment Using AWS Console](#4-step-by-step-deployment-using-aws-console) | Build the platform foundations manually first | Console-level AWS understanding |
| [5. Infrastructure As Code: Terraform](#5-infrastructure-as-code-terraform) | Convert the same architecture into repeatable IaC | Terraform foundation for dev environment |
| [6. Application Flow End-To-End](#6-application-flow-end-to-end) | Trace upload, watch, and messaging flows | Request/async path clarity |
| [7. Scaling To Billions](#7-scaling-to-billions) | Reason about compute, data, CDN, shards, and replicas | Billion-user scaling checklist |
| [8. Caching Strategy](#8-caching-strategy) | Decide where Redis and CloudFront fit | Cache placement and invalidation rules |
| [9. Queues And Streaming](#9-queues-and-streaming) | Choose SQS, SNS, MSK, and Kinesis correctly | Async processing and event-streaming map |
| [10. GenAI Integration](#10-genai-integration) | Add recommendations, vectors, Bedrock, SageMaker, and RAG | Safe GenAI architecture patterns |
| [11. CI/CD Pipeline](#11-cicd-pipeline) | Plan AWS-native or GitHub Actions deployment | Repeatable release flow |
| [12. Observability](#12-observability) | Add logs, metrics, alarms, and tracing | Production debugging toolkit |
| [13. Security](#13-security) | Apply IAM, encryption, API security, and GenAI safety | Security baseline |
| [14. Cost Optimization](#14-cost-optimization) | Control compute, CDN, storage, database, and GenAI spend | Cost-aware architecture decisions |
| [15. How To Practice](#15-how-to-practice) | Follow the 10-week hands-on build path | Practical learning roadmap |
| [Official Source Notes](#official-source-notes) | Jump to AWS and Terraform references | Verification links |

Fast jump map:

| Goal | Start here |
|---|---|
| I want the architecture story | [1. System Overview](#1-system-overview) -> [2. AWS Service Selection](#2-aws-service-selection) |
| I want to build resources in console | [3. Networking](#3-networking) -> [4. Step-By-Step Deployment Using AWS Console](#4-step-by-step-deployment-using-aws-console) |
| I want Terraform | [5. Infrastructure As Code: Terraform](#5-infrastructure-as-code-terraform) |
| I want application flows | [6. Application Flow End-To-End](#6-application-flow-end-to-end) |
| I want scaling and caching | [7. Scaling To Billions](#7-scaling-to-billions) -> [8. Caching Strategy](#8-caching-strategy) |
| I want async/event systems | [9. Queues And Streaming](#9-queues-and-streaming) |
| I want GenAI on AWS | [10. GenAI Integration](#10-genai-integration) |
| I want production readiness | [11. CI/CD Pipeline](#11-cicd-pipeline) -> [12. Observability](#12-observability) -> [13. Security](#13-security) -> [14. Cost Optimization](#14-cost-optimization) |
| I want a weekly plan | [15. How To Practice](#15-how-to-practice) |

---

# 1. System Overview

## 1.1 What The App Does

- Users create accounts and profiles.
- Users upload:
  - photos
  - short videos
  - long videos
  - reels/stories
- Users watch video feeds.
- Users like, comment, follow, share, and save.
- Users chat in real time.
- Creators livestream or publish recorded content.
- GenAI powers:
  - recommendations
  - captions
  - moderation
  - semantic search
  - creator assistant
  - support chatbot
  - content summarization

## 1.2 Major User Flows

- Signup/login:
  - Frontend -> Cognito -> JWT -> backend APIs.
- Upload video:
  - Frontend -> API -> pre-signed S3 URL -> S3 -> SQS/EventBridge -> processing -> metadata DB.
- Watch feed:
  - Frontend -> CloudFront -> API -> recommendation service -> metadata DB/cache -> CloudFront video URLs.
- Chat:
  - Frontend WebSocket -> API Gateway WebSocket -> messaging service -> DynamoDB/MSK/SQS -> push notifications.
- Search:
  - Frontend -> search API -> OpenSearch -> ranked results.
- GenAI recommendation:
  - events -> Kinesis/MSK -> feature pipelines -> embeddings -> vector/search/index -> ranked feed.

## 1.3 Scale Assumptions

- Registered users:
  - 1 billion.
- Daily active users:
  - 250 million to 400 million.
- Peak concurrent users:
  - 20 million to 50 million.
- Feed read RPS:
  - 1 million+ at peak globally.
- Messaging events:
  - 5 million+ messages/second globally at extreme scale.
- Video uploads:
  - 5 million to 20 million per day.
- Video storage:
  - petabytes to exabytes over time.
- CDN bandwidth:
  - terabits/second globally.
- Search index:
  - billions of posts/videos/profiles.
- Observability:
  - billions of logs/events per day.

## 1.4 High-Level Architecture

```text
Users
  -> Route 53
  -> CloudFront
  -> S3 frontend / ALB / API Gateway
  -> ECS services / Lambda functions
  -> DynamoDB / Aurora / OpenSearch / ElastiCache
  -> S3 media storage
  -> SQS / SNS / MSK / Kinesis
  -> Bedrock / SageMaker AI
  -> CloudWatch / X-Ray / CloudTrail
```

## Example Request/Response

Request:

```http
POST /v1/videos/upload-intent
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "fileName": "trip.mp4",
  "contentType": "video/mp4",
  "sizeBytes": 734003200
}
```

Response:

```json
{
  "uploadId": "upl_01HYEXAMPLE",
  "videoId": "vid_01HYEXAMPLE",
  "uploadUrl": "https://s3-presigned-url",
  "objectKey": "raw/user-123/vid_01HYEXAMPLE/original.mp4",
  "expiresInSeconds": 900
}
```

## Expected System Behavior Under Load

- CloudFront absorbs static/video delivery.
- APIs autoscale horizontally.
- Uploads do not pass through app servers.
- SQS buffers processing spikes.
- DynamoDB handles high-write metadata paths.
- Redis handles hot feed/profile/session reads.
- Kinesis/MSK absorbs event streams.
- GenAI workloads are throttled and budgeted.

## Debugging Strategy

- Start from the user symptom:
  - upload failed
  - feed slow
  - chat delayed
  - video buffering
- Check:
  - CloudFront metrics
  - ALB/API Gateway 4xx/5xx
  - ECS/Lambda logs
  - SQS queue depth
  - DynamoDB throttles
  - Redis evictions
  - OpenSearch latency
  - Bedrock/SageMaker errors

## Hands-On Task

- Draw this architecture on paper.
- Mark every sync path and async path.
- Mark which services are public and which are private.

## Common Mistakes

- Sending video bytes through backend API.
- Putting database in public subnet.
- Using one database for all access patterns.
- Ignoring CDN costs and cache behavior.
- Running GenAI calls without budgets.

## Deep Dive Explanation

- Billion-scale systems are not one giant app.
- They are many specialized systems:
  - identity
  - feed
  - media
  - messaging
  - search
  - recommendations
  - moderation
  - notifications
  - analytics
- AWS service selection should follow workload shape, not hype.

---

# 2. AWS Service Selection

## 2.1 Frontend Hosting: S3 vs Amplify vs EC2

Chosen:

- S3 + CloudFront for production SPA/static frontend.

Why:

- S3 is durable object storage.
- CloudFront gives global caching.
- Frontend is static build output.
- No server patching.
- Cheap and scalable.

Alternatives:

- Amplify:
  - good for fast full-stack app hosting and previews.
  - less ideal when platform team wants custom enterprise controls.
- EC2:
  - possible but unnecessary for static files.
  - adds OS, patching, scaling, and server management.

Decision:

```text
React/Vite/Next static export -> S3 private bucket -> CloudFront OAC -> Route 53.
```

## 2.2 CDN: CloudFront

Chosen:

- CloudFront.

Why:

- Edge caching for images/videos/static assets.
- Reduces origin load.
- Supports signed URLs/cookies for private content.
- Integrates with S3, ALB, API Gateway, WAF, ACM.
- Origin Access Control protects private S3 origins.

Alternative:

- Third-party CDN.

Why CloudFront:

- Native AWS integration and IAM/WAF/S3/OAC support.

## 2.3 DNS: Route 53

Chosen:

- Route 53.

Why:

- Hosted zones.
- Alias records for CloudFront/ALB/API Gateway.
- Health checks.
- Weighted/failover/latency routing.

## 2.4 Authentication: Cognito

Chosen for learning:

- Cognito User Pools.

Why:

- Managed signup/login.
- JWT tokens.
- MFA support.
- Social/federated login support.
- Works with API Gateway and backend JWT validation.

Alternatives:

- Auth0/Okta:
  - enterprise identity provider.
- Custom auth:
  - high control but heavy security burden.

Decision:

```text
Cognito for user auth in learning/prototype.
Enterprise may federate Cognito or backend auth with corporate IdP.
```

## 2.5 API Layer: API Gateway vs ALB

Use API Gateway for:

- serverless APIs.
- WebSocket APIs for chat.
- request throttling.
- auth integration.
- usage plans.
- edge-friendly API front door.

Use ALB for:

- ECS/EKS/EC2 services.
- high-throughput HTTP APIs.
- path/host routing.
- lower per-request cost at very high steady RPS.

Chosen:

- ALB for main feed/media/backend microservices on ECS.
- API Gateway WebSocket for real-time messaging gateway.
- API Gateway HTTP API for lightweight Lambda workflows.

## 2.6 Compute: Lambda vs ECS vs EC2

Lambda:

- Best for:
  - event handlers
  - upload intent
  - lightweight processing
  - scheduled jobs
  - S3/SQS triggers
- Avoid for:
  - long-running video transcoding
  - very high steady JVM APIs with cold-start concerns

ECS Fargate:

- Best for:
  - backend APIs
  - feed service
  - user service
  - media metadata service
  - notification workers
- Chosen default for app services.

EC2:

- Best for:
  - custom compute
  - GPU workers
  - specialized agents
  - cost-optimized steady workloads

Decision:

```text
ECS for core APIs.
Lambda for glue/event tasks.
EC2/GPU/SageMaker for heavy media/ML workloads where needed.
```

## 2.7 Database: DynamoDB vs RDS vs Aurora

DynamoDB:

- Best for:
  - high-scale key-value access
  - user timelines
  - chat messages by conversation
  - likes/follows counters/events
  - notification state
- Chosen for massive scale hot paths.

Aurora/RDS:

- Best for:
  - relational transactional workflows
  - payments
  - creator monetization
  - admin systems
  - reporting metadata with SQL needs

Decision:

```text
DynamoDB for social/messaging high-scale access patterns.
Aurora PostgreSQL for relational domains requiring SQL transactions.
```

## 2.8 Caching: ElastiCache Redis

Chosen:

- ElastiCache Redis.

Use for:

- hot profiles
- feed cache
- session cache
- rate-limit counters
- trending content
- online presence
- precomputed recommendation lists

Avoid:

- source of truth for business-critical data.

## 2.9 Messaging: SQS vs SNS vs MSK

SQS:

- async work queue.
- upload processing jobs.
- email jobs.
- moderation jobs.

SNS:

- fan-out event broadcast.
- one event to many queues.

MSK/Kafka:

- high-throughput event log.
- feed events.
- analytics pipeline.
- recommendation events.
- chat/event stream at very large scale.

Decision:

```text
SQS for task queues.
SNS + SQS for fan-out.
MSK for durable event streaming and replay at platform scale.
```

## 2.10 Streaming: Kinesis

Chosen for AWS-native event ingestion:

- Kinesis Data Streams for clickstream/watch events.
- Kinesis Firehose for delivery to S3/OpenSearch/analytics.

When Kinesis over MSK:

- AWS-native stream.
- lower operational overhead.
- direct AWS integrations.

When MSK over Kinesis:

- Kafka ecosystem required.
- portability.
- complex stream processing.

## 2.11 Search: OpenSearch

Chosen:

- OpenSearch.

Use for:

- full-text search over videos/users/posts.
- hashtag search.
- semantic search with vector indexes.
- moderation search.

Alternative:

- DynamoDB queries:
  - not full-text.
- Aurora:
  - SQL search is not enough at this scale.

## 2.12 Storage: S3

Chosen:

- S3 for media objects.

Use for:

- raw uploads.
- transcoded video renditions.
- thumbnails.
- profile pictures.
- logs.
- ML training datasets.
- RAG documents.

## 2.13 AI / GenAI: Bedrock vs SageMaker AI

Bedrock:

- managed foundation models.
- text/image/embedding models.
- RAG with Knowledge Bases.
- Agents.
- Guardrails.
- Prompt Management.

SageMaker AI:

- train/customize/deploy your own models.
- real-time/serverless/async/batch inference.
- pipelines and model registry.

Decision:

```text
Bedrock for GenAI app features and embeddings.
SageMaker AI for custom recommendation/moderation models.
```

## 2.14 Notifications: SNS / SES

SNS:

- mobile push fanout.
- SMS in some cases.
- system notifications.

SES:

- email notifications.
- verification emails.
- creator reports.

## 2.15 Observability: CloudWatch / X-Ray

CloudWatch:

- metrics.
- logs.
- alarms.
- dashboards.

X-Ray:

- distributed tracing.
- request path debugging.

CloudTrail:

- audit who changed what.

## Example Request/Response

Request:

```http
GET /v1/feed?cursor=abc
Authorization: Bearer <jwt>
```

Response:

```json
{
  "items": [
    {
      "postId": "post_123",
      "creatorId": "user_456",
      "mediaUrl": "https://cdn.example.com/videos/vid_123/720p.m3u8",
      "caption": "Mountain trip",
      "rankScore": 0.982
    }
  ],
  "nextCursor": "def"
}
```

## Expected System Behavior Under Load

- CloudFront handles repeated media requests.
- Redis serves hot feed pages.
- DynamoDB scales high-volume reads/writes.
- ECS services scale by CPU/RPS.
- Queues absorb spikes.

## Debugging Strategy

- If feed slow:
  - check CloudFront cache hit ratio.
  - check ALB target response time.
  - check Redis hit rate.
  - check DynamoDB throttles.
  - check recommendation service latency.

## Hands-On Task

- Create a table with each service and why it exists.
- Mark which services are sync vs async.
- Mark source-of-truth data stores.

## Common Mistakes

- Choosing Lambda for every backend API.
- Choosing EKS before ECS is understood.
- Using S3 as a database.
- Using Redis as source of truth.
- Using one queue for all background work.

## Deep Dive Explanation

- AWS service selection is about operational properties:
  - latency
  - throughput
  - durability
  - consistency
  - cost
  - team skill
  - failure mode

---

# 3. Networking

## 3.1 Target Network Design

Use one VPC per environment for the learning build:

```text
VPC: 10.0.0.0/16

Public subnets:
  10.0.0.0/20   us-east-1a
  10.0.16.0/20  us-east-1b

Private app subnets:
  10.0.32.0/20  us-east-1a
  10.0.48.0/20  us-east-1b

Private data subnets:
  10.0.64.0/20  us-east-1a
  10.0.80.0/20  us-east-1b
```

## 3.2 Public vs Private Subnet

Public subnet:

- route table has `0.0.0.0/0 -> Internet Gateway`.
- use for:
  - ALB
  - NAT Gateway

Private subnet:

- no direct Internet Gateway route.
- outbound internet goes through NAT Gateway.
- AWS service calls can use VPC endpoints.
- use for:
  - ECS tasks
  - EC2 app servers
  - RDS
  - Redis
  - internal services

## 3.3 Internet Gateway

Use for:

- internet inbound/outbound for public subnets.

Impact:

- attaching IGW alone does not make everything public.
- subnet route table and public IP still matter.

## 3.4 NAT Gateway

Use for:

- private subnet outbound internet.

Impact:

- private ECS tasks can pull external dependencies or call third-party APIs.
- NAT has hourly and data processing cost.

## 3.5 VPC Endpoints

Use for:

- private access to AWS services.

Examples:

- S3 Gateway Endpoint.
- DynamoDB Gateway Endpoint.
- ECR Interface Endpoint.
- CloudWatch Logs Interface Endpoint.
- Secrets Manager Interface Endpoint.

Impact:

- reduces NAT dependency.
- improves private traffic posture.

## 3.6 Security Groups

Use as stateful resource firewall.

Example:

```text
ALB SG:
  inbound 443 from 0.0.0.0/0

ECS Service SG:
  inbound 8080 from ALB SG only

RDS SG:
  inbound 5432 from ECS Service SG only
```

## 3.7 NACLs

Use as subnet-level stateless rules.

Default:

- keep simple.
- rely primarily on security groups.

Use NACLs for:

- explicit subnet-level deny.
- compliance boundary.
- emergency blocking.

## 3.8 Traffic Flow

User to API:

```text
User
  -> Route 53
  -> CloudFront
  -> ALB or API Gateway
  -> ECS service / Lambda
  -> DynamoDB / Aurora / Redis
```

User to video:

```text
User
  -> Route 53
  -> CloudFront
  -> S3 origin
  -> cached video segment
```

Backend to S3:

```text
ECS private subnet
  -> S3 Gateway Endpoint
  -> S3 bucket
```

Backend to external provider:

```text
ECS private subnet
  -> NAT Gateway
  -> Internet
  -> provider API
```

## Example Request/Response

Request:

```http
GET /v1/profiles/user_123
Host: api.example.com
```

Expected network path:

```text
Route 53 -> CloudFront -> ALB -> ECS profile-service -> Redis/DynamoDB
```

Response:

```json
{
  "userId": "user_123",
  "displayName": "Asha",
  "followers": 1200345
}
```

## Expected System Behavior Under Load

- CloudFront reduces edge latency.
- ALB distributes to many ECS tasks.
- private services scale horizontally.
- NAT is not used for S3 if endpoint exists.

## Debugging Strategy

- Check Route 53 record.
- Check CloudFront origin status.
- Check ALB target health.
- Check ECS service health.
- Check security groups.
- Check route tables.
- Check NACLs.
- Check VPC Flow Logs.

## Hands-On Task

- Create a VPC with:
  - 2 public subnets.
  - 2 private app subnets.
  - 2 private data subnets.
  - Internet Gateway.
  - NAT Gateway.
  - S3 Gateway Endpoint.

## Common Mistakes

- Putting ECS tasks in public subnets.
- Missing route from private subnet to NAT.
- Missing S3 endpoint and paying unnecessary NAT cost.
- Opening DB security group to `0.0.0.0/0`.

## Deep Dive Explanation

- Networking is not only connectivity.
- It is blast radius, security, cost, and failure isolation.
- Subnet placement controls how traffic can enter and leave.

---

# 4. Step-By-Step Deployment Using AWS Console

## 4.1 VPC

Console steps:

- Go to `AWS Console -> Search "VPC"`.
- Click `Your VPCs`.
- Click `Create VPC`.
- Choose `VPC and more`.
- Set:
  - Name: `genai-social-dev`
  - IPv4 CIDR: `10.0.0.0/16`
  - Availability Zones: `2`
  - Public subnets: `2`
  - Private subnets: `4`
  - NAT gateways: `1 per AZ` for stronger resilience, `1` for learning cost control
  - VPC endpoints: select S3 Gateway Endpoint if available in wizard
- Click `Create VPC`.

What each step impacts:

- CIDR:
  - defines total private IP space.
- AZ count:
  - defines failure isolation.
- Public subnets:
  - allow ALB/NAT to face internet.
- Private subnets:
  - keep app/data hidden.
- NAT:
  - gives private subnets outbound internet.
- Endpoint:
  - keeps S3 traffic private.

Hands-on task:

- Create the VPC.
- Name route tables clearly:
  - `public-rt`
  - `private-app-rt-a`
  - `private-app-rt-b`

Common mistakes:

- CIDR overlap.
- one subnet only.
- private subnet accidentally using IGW route.

Deep dive explanation:

- VPC is the private network boundary.
- Route tables decide whether a subnet is public/private.

## 4.2 EC2

Console steps:

- Go to `EC2 -> Instances -> Launch instances`.
- Name: `learning-bastion` or `test-private-app`.
- Choose AMI:
  - Amazon Linux 2023.
- Choose instance type:
  - `t3.micro` or `t4g.micro` for learning.
- Network settings -> Edit:
  - VPC: `genai-social-dev`
  - Subnet:
    - public subnet for bastion/testing only.
    - private app subnet for app server.
  - Auto-assign public IP:
    - enable only for public test instance.
  - Security group:
    - SSH only from your IP for public test.
    - app port only from ALB for private app.
- Add IAM role:
  - SSM role for Session Manager.
- Launch.

What each step impacts:

- VPC/subnet:
  - decides network placement and AZ.
- public IP:
  - decides direct internet reachability.
- security group:
  - decides allowed traffic.
- IAM role:
  - decides AWS API permissions from instance.

Hands-on task:

- Launch one EC2 in a public subnet.
- Launch one EC2 in a private subnet.
- Verify only the public one can be reached directly.

Common mistakes:

- Trying to attach a subnet after launch.
- Opening SSH to everyone.
- putting production app in public subnet.

Deep dive explanation:

- EC2 primary subnet is selected at launch.
- To move to another subnet, relaunch using AMI/template.

## 4.3 ECS

Console steps:

- Go to `ECS -> Clusters -> Create cluster`.
- Name: `genai-social-dev`.
- Infrastructure:
  - choose Fargate.
- Create cluster.
- Go to `Task definitions -> Create`.
- Add container:
  - image: your ECR image.
  - port: `8080`.
  - logs: CloudWatch.
  - secrets: from Secrets Manager.
- Go to cluster -> `Services -> Create`.
- Choose:
  - launch type: Fargate.
  - desired tasks: `2`.
  - private app subnets.
  - service security group.
  - attach to ALB target group.

What each step impacts:

- cluster:
  - logical container platform.
- task definition:
  - deployable container contract.
- service:
  - desired running replicas.
- private subnets:
  - no direct internet inbound.
- ALB:
  - public entry point.

Hands-on task:

- Deploy a simple `/health` Spring Boot or mock container to ECS.
- Attach to ALB.
- Hit ALB DNS name.

Common mistakes:

- task role vs execution role confusion.
- missing CloudWatch logs.
- health check path wrong.

Deep dive explanation:

- ECS Service is the self-healing unit.
- Task Definition is the immutable versioned deployment config.

## 4.4 Lambda

Console steps:

- Go to `Lambda -> Functions -> Create function`.
- Choose `Author from scratch`.
- Name: `create-upload-intent`.
- Runtime: Python/Node/Java.
- Execution role:
  - allow S3 pre-signed URL creation.
  - allow DynamoDB metadata write.
- Add environment variables:
  - `UPLOAD_BUCKET`
  - `VIDEO_TABLE`
- Add trigger:
  - API Gateway HTTP API.

What each step impacts:

- runtime:
  - execution environment.
- role:
  - AWS permissions.
- env vars:
  - runtime config.
- trigger:
  - event source.

Hands-on task:

- Create Lambda that returns `{ "status": "ok" }`.
- Add API Gateway trigger.
- Test URL.

Common mistakes:

- role too broad.
- timeout too low.
- logging secrets.

Deep dive explanation:

- Lambda is excellent for event glue.
- It is not the default for all high-RPS backend APIs.

## 4.5 API Gateway

HTTP API steps:

- Go to `API Gateway -> Create API`.
- Choose `HTTP API`.
- Add integration:
  - Lambda `create-upload-intent`.
- Add route:
  - `POST /v1/videos/upload-intent`.
- Deploy stage:
  - `$default` or `dev`.
- Enable CORS for frontend domain.

WebSocket API steps:

- Go to `API Gateway -> Create API`.
- Choose `WebSocket API`.
- Route selection expression:
  - `$request.body.action`.
- Create routes:
  - `$connect`
  - `$disconnect`
  - `sendMessage`
- Integrate routes with Lambda.

What each step impacts:

- route:
  - maps HTTP/WebSocket request to backend.
- integration:
  - target compute.
- stage:
  - deployed environment.
- CORS:
  - browser access control.

Hands-on task:

- Build HTTP API for upload intent.
- Build WebSocket API with `$connect` and `sendMessage`.

Common mistakes:

- CORS missing.
- auth missing.
- no throttling.

Deep dive explanation:

- HTTP APIs are simpler and cheaper than REST APIs for many workloads.
- WebSocket API is useful for chat gateway, but fanout/state must be externalized.

## 4.6 DynamoDB

Console steps:

- Go to `DynamoDB -> Tables -> Create table`.
- Table: `Videos`.
- Partition key:
  - `videoId`.
- Add GSI:
  - `creatorId-createdAt-index`.
- Enable:
  - point-in-time recovery.
  - streams if event processing needed.
- Capacity:
  - on-demand for learning/unpredictable traffic.

Chat table:

- Table: `Messages`.
- Partition key:
  - `conversationId`.
- Sort key:
  - `messageCreatedAt`.

What each step impacts:

- partition key:
  - data distribution and lookup path.
- sort key:
  - ordered reads inside partition.
- GSI:
  - alternate query path.
- PITR:
  - recovery.

Hands-on task:

- Create `Videos` and `Messages` tables.
- Insert sample items.
- Query by partition key.

Common mistakes:

- using scans for feed.
- bad hot partition key.
- creating GSI without access pattern.

Deep dive explanation:

- DynamoDB design starts from queries, not entities.

## 4.7 S3

Console steps:

- Go to `S3 -> Buckets -> Create bucket`.
- Buckets:
  - `genai-social-frontend-dev`
  - `genai-social-raw-video-dev`
  - `genai-social-processed-video-dev`
- Enable:
  - Block Public Access.
  - default encryption.
  - versioning for critical buckets.
  - lifecycle rules.
- Configure CORS for upload bucket if browser uses pre-signed upload.

What each step impacts:

- bucket:
  - object namespace.
- block public access:
  - prevents accidental public exposure.
- encryption:
  - protects data at rest.
- lifecycle:
  - controls storage cost.
- CORS:
  - allows browser upload.

Hands-on task:

- Create raw video bucket.
- Upload sample file.
- Generate a pre-signed URL from CLI/SDK.

Common mistakes:

- public media bucket without intent.
- no lifecycle for raw uploads.
- storing metadata only in S3 key.

Deep dive explanation:

- S3 stores objects.
- DynamoDB/Aurora stores searchable metadata.

## 4.8 CloudFront

Console steps:

- Go to `CloudFront -> Distributions -> Create distribution`.
- Origin:
  - S3 frontend bucket or S3 processed video bucket.
- Origin access:
  - choose Origin Access Control.
  - choose `Sign requests`.
- Viewer protocol:
  - redirect HTTP to HTTPS.
- Cache policy:
  - optimized caching for static/media.
- WAF:
  - attach Web ACL for public app.
- Alternate domain:
  - `app.example.com`.
- Certificate:
  - ACM certificate in `us-east-1`.

What each step impacts:

- origin:
  - source of content.
- OAC:
  - keeps S3 private and lets CloudFront sign origin requests.
- cache policy:
  - TTL and cache key behavior.
- viewer protocol:
  - HTTPS enforcement.
- WAF:
  - L7 protection.

Hands-on task:

- Create private S3 frontend bucket.
- Create CloudFront distribution with OAC.
- Add S3 bucket policy allowing only that distribution.

Common mistakes:

- using public S3 bucket.
- missing SPA error mapping.
- ACM cert in wrong region for CloudFront.

Deep dive explanation:

- CloudFront should be the public edge.
- S3 origin should remain private.
- AWS recommends OAC over legacy OAI for S3 origins.

## 4.9 Cognito

Console steps:

- Go to `Cognito -> User pools -> Create user pool`.
- Choose sign-in:
  - email.
- Configure:
  - password policy.
  - MFA optional/required.
  - app client.
  - callback/logout URLs.
- Create domain if using Hosted UI.

What each step impacts:

- user pool:
  - user directory.
- app client:
  - frontend/backend OAuth client identity.
- token settings:
  - access/refresh behavior.

Hands-on task:

- Create user pool.
- Create test user.
- Sign in and inspect JWT.

Common mistakes:

- not validating JWT audience/issuer.
- storing tokens insecurely.
- confusing Cognito users with IAM users.

Deep dive explanation:

- Cognito authenticates app users.
- IAM authorizes AWS resource access.

## 4.10 SQS / SNS

SQS steps:

- Go to `SQS -> Queues -> Create queue`.
- Create:
  - `video-processing-q`.
  - `video-processing-dlq`.
- Set:
  - visibility timeout.
  - retention.
  - redrive policy to DLQ.

SNS steps:

- Go to `SNS -> Topics -> Create topic`.
- Create:
  - `video-events`.
- Subscribe:
  - SQS queues for processing/moderation/analytics.

What each step impacts:

- SQS:
  - durable task buffer.
- DLQ:
  - failed message isolation.
- SNS:
  - fanout event distribution.

Hands-on task:

- Publish SNS event.
- Verify SQS subscriber receives it.

Common mistakes:

- no DLQ.
- visibility timeout too short.
- consumers not idempotent.

Deep dive explanation:

- SNS tells many systems something happened.
- SQS lets one worker group process work reliably.

## 4.11 Kinesis

Console steps:

- Go to `Kinesis -> Data streams -> Create data stream`.
- Name:
  - `watch-events-dev`.
- Capacity:
  - on-demand for learning.
- Retention:
  - default or extended if replay needed.

What each step impacts:

- stream:
  - ordered event ingestion.
- capacity:
  - throughput model.
- retention:
  - replay window.

Hands-on task:

- Send sample `VideoWatched` event.
- Consume using Lambda or simple CLI consumer.

Common mistakes:

- wrong partition key causing hot shard.
- using Kinesis for simple tasks better suited to SQS.

Deep dive explanation:

- Kinesis is an event stream.
- SQS is a work queue.

---

# 5. Infrastructure As Code: Terraform

## 5.1 Terraform Scope

This Terraform builds a learning-grade AWS foundation:

- VPC.
- subnets.
- security groups.
- S3 frontend/media buckets.
- CloudFront with OAC.
- Lambda.
- API Gateway HTTP API.
- DynamoDB tables.
- SQS/SNS.
- Kinesis stream.
- ALB skeleton.

This is conceptual -- here's what you must verify before production:

- ACM certificate ARN.
- Route 53 hosted zone.
- Lambda ZIP path and runtime code.
- ECS image URI and task definition.
- WAF rules.
- KMS key policies.
- Terraform remote state backend.
- provider version lock.
- separate dev/stage/prod workspaces or directories.

## 5.2 Terraform: Provider And Locals

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project" {
  type    = string
  default = "genai-social"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "domain_name" {
  type    = string
  default = "app.example.com"
}

variable "acm_certificate_arn" {
  type        = string
  description = "ACM certificate ARN in us-east-1 for CloudFront."
}

locals {
  name = "${var.project}-${var.environment}"

  tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
```

## 5.3 Terraform: VPC, Subnets, NAT, S3 Endpoint

```hcl
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.tags, { Name = local.name })
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = merge(local.tags, { Name = "${local.name}-igw" })
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(aws_vpc.main.cidr_block, 4, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = merge(local.tags, { Name = "${local.name}-public-${count.index + 1}" })
}

resource "aws_subnet" "private_app" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 4, count.index + 2)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(local.tags, { Name = "${local.name}-private-app-${count.index + 1}" })
}

resource "aws_subnet" "private_data" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 4, count.index + 4)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(local.tags, { Name = "${local.name}-private-data-${count.index + 1}" })
}

resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = merge(local.tags, { Name = "${local.name}-nat-eip" })
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = merge(local.tags, { Name = "${local.name}-nat" })

  depends_on = [aws_internet_gateway.main]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(local.tags, { Name = "${local.name}-public-rt" })
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private_app" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = merge(local.tags, { Name = "${local.name}-private-app-rt" })
}

resource "aws_route_table_association" "private_app" {
  count          = length(aws_subnet.private_app)
  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app.id
}

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.private_app.id]

  tags = merge(local.tags, { Name = "${local.name}-s3-endpoint" })
}
```

## 5.4 Terraform: Security Groups

```hcl
resource "aws_security_group" "alb" {
  name        = "${local.name}-alb-sg"
  description = "Public ALB security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "app" {
  name        = "${local.name}-app-sg"
  description = "Private app services"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}
```

## 5.5 Terraform: S3 + CloudFront With OAC

```hcl
resource "aws_s3_bucket" "frontend" {
  bucket = "${local.name}-frontend-${data.aws_caller_identity.current.account_id}"
  tags   = local.tags
}

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "${local.name}-frontend-oac"
  description                       = "OAC for private S3 frontend bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  comment             = "${local.name} frontend"
  default_root_object = "index.html"

  aliases = [var.domain_name]

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "frontend-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  default_cache_behavior {
    target_origin_id       = "frontend-s3"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }
  }

  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = var.acm_certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  tags = local.tags
}

data "aws_iam_policy_document" "frontend_bucket_policy" {
  statement {
    sid     = "AllowCloudFrontReadOnly"
    actions = ["s3:GetObject"]

    resources = ["${aws_s3_bucket.frontend.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_bucket_policy.json
}
```

## 5.6 Terraform: DynamoDB

```hcl
resource "aws_dynamodb_table" "videos" {
  name         = "${local.name}-videos"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "videoId"

  attribute {
    name = "videoId"
    type = "S"
  }

  attribute {
    name = "creatorId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "creatorId-createdAt-index"
    hash_key        = "creatorId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "messages" {
  name         = "${local.name}-messages"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "conversationId"
  range_key    = "messageCreatedAt"

  attribute {
    name = "conversationId"
    type = "S"
  }

  attribute {
    name = "messageCreatedAt"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.tags
}
```

## 5.7 Terraform: Lambda + API Gateway HTTP API

```hcl
resource "aws_iam_role" "lambda_exec" {
  name = "${local.name}-lambda-exec"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = local.tags
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_lambda_function" "upload_intent" {
  function_name = "${local.name}-upload-intent"
  role          = aws_iam_role.lambda_exec.arn
  handler       = "index.handler"
  runtime       = "python3.12"
  filename      = "lambda/upload_intent.zip"
  timeout       = 10
  memory_size   = 256

  environment {
    variables = {
      VIDEO_TABLE = aws_dynamodb_table.videos.name
    }
  }

  tags = local.tags
}

resource "aws_apigatewayv2_api" "http" {
  name          = "${local.name}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_methods = ["GET", "POST", "OPTIONS"]
    allow_origins = ["https://${var.domain_name}"]
    allow_headers = ["authorization", "content-type"]
  }

  tags = local.tags
}

resource "aws_apigatewayv2_integration" "upload_intent" {
  api_id                 = aws_apigatewayv2_api.http.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.upload_intent.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "upload_intent" {
  api_id    = aws_apigatewayv2_api.http.id
  route_key = "POST /v1/videos/upload-intent"
  target    = "integrations/${aws_apigatewayv2_integration.upload_intent.id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "api_upload_intent" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.upload_intent.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http.execution_arn}/*/*"
}
```

## 5.8 Terraform: SQS, SNS, Kinesis

```hcl
resource "aws_sqs_queue" "video_processing_dlq" {
  name                      = "${local.name}-video-processing-dlq"
  message_retention_seconds = 1209600
  tags                      = local.tags
}

resource "aws_sqs_queue" "video_processing" {
  name                       = "${local.name}-video-processing"
  visibility_timeout_seconds = 300
  message_retention_seconds  = 345600

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.video_processing_dlq.arn
    maxReceiveCount     = 3
  })

  tags = local.tags
}

resource "aws_sns_topic" "video_events" {
  name = "${local.name}-video-events"
  tags = local.tags
}

resource "aws_sns_topic_subscription" "video_processing" {
  topic_arn = aws_sns_topic.video_events.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.video_processing.arn
}

resource "aws_kinesis_stream" "watch_events" {
  name        = "${local.name}-watch-events"
  shard_count = 1

  stream_mode_details {
    stream_mode = "PROVISIONED"
  }

  tags = local.tags
}
```

## 5.9 Terraform: ALB Skeleton

```hcl
resource "aws_lb" "api" {
  name               = "${local.name}-api-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = local.tags
}

resource "aws_lb_target_group" "api" {
  name        = "${local.name}-api-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = local.tags
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.api.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}
```

## Example Request/Response

Terraform command:

```bash
terraform init
terraform plan
terraform apply
```

Expected behavior:

```text
VPC, subnets, gateway, buckets, CloudFront, DynamoDB, API Gateway,
Lambda, queues, stream, and ALB skeleton are created.
```

## Debugging Strategy

- If Terraform fails:
  - check IAM permissions.
  - check region.
  - check bucket name uniqueness.
  - check ACM cert region for CloudFront.
  - check Lambda ZIP file exists.

## Hands-On Task

- Put Terraform in `infra/dev`.
- Run `terraform plan`.
- Create only network first.
- Add S3 + CloudFront next.
- Add API + Lambda after that.

## Common Mistakes

- no remote state.
- hardcoding secrets in Terraform.
- public S3 bucket.
- missing CloudFront bucket policy.
- using one Terraform state for all environments.

## Deep Dive Explanation

- Console teaches what resources do.
- Terraform makes those resources repeatable.
- Production teams review Terraform plans before applying.

---

# 6. Application Flow End-To-End

## 6.1 User Uploads Video

Flow:

```text
Frontend
  -> CloudFront
  -> API Gateway HTTP API
  -> Lambda upload-intent
  -> DynamoDB video metadata row
  -> S3 pre-signed upload URL
  -> Browser uploads video to S3
  -> S3 event / EventBridge
  -> SQS video-processing queue
  -> ECS worker / MediaConvert
  -> processed S3 bucket
  -> DynamoDB update
  -> SNS notification
```

Example request:

```json
{
  "fileName": "dance.mp4",
  "contentType": "video/mp4",
  "sizeBytes": 104857600
}
```

Example response:

```json
{
  "videoId": "vid_123",
  "uploadUrl": "https://s3-presigned-url",
  "status": "UPLOAD_PENDING"
}
```

Expected behavior under load:

- API creates intent quickly.
- S3 handles upload bandwidth.
- SQS buffers processing.
- workers scale separately.

Debugging:

- S3 object exists?
- SQS message created?
- worker logs?
- DLQ has messages?
- DynamoDB status updated?

## 6.2 User Watches Video

Flow:

```text
Frontend
  -> CloudFront
  -> cached HLS/DASH video segments
```

Metadata:

```text
Frontend
  -> ALB/API
  -> video metadata service
  -> Redis hot cache
  -> DynamoDB fallback
```

Expected behavior:

- cache hit for popular videos.
- origin protected from repeated requests.

Debugging:

- CloudFront cache hit ratio.
- S3 403 from OAC policy.
- video segment missing.
- origin latency.

## 6.3 Messaging

Flow:

```text
Client WebSocket
  -> API Gateway WebSocket
  -> Lambda/ECS messaging gateway
  -> DynamoDB Messages table
  -> SQS delivery queue
  -> connected recipient delivery
  -> SNS mobile push if offline
```

Example request:

```json
{
  "action": "sendMessage",
  "conversationId": "conv_123",
  "clientMessageId": "client_abc",
  "body": "hello"
}
```

Example response:

```json
{
  "messageId": "msg_789",
  "status": "ACCEPTED",
  "serverTimestamp": "2026-06-18T10:15:30Z"
}
```

Expected behavior:

- sender gets accepted quickly.
- offline users get push notification.
- duplicate client message IDs are idempotent.

Debugging:

- WebSocket connection stored?
- DynamoDB write succeeded?
- delivery queue depth?
- push provider errors?

## Hands-On Task

- Implement upload intent Lambda.
- Store video metadata in DynamoDB.
- Send message to SQS after upload.

## Common Mistakes

- processing video inside request thread.
- not using idempotency keys.
- not tracking video processing status.

## Deep Dive Explanation

- The user-facing request should only do critical work.
- Heavy work moves to queues/workers.

---

# 7. Scaling To Billions

## 7.1 Compute Scaling

ECS:

- scale by:
  - CPU.
  - memory.
  - ALB request count per target.
  - custom queue depth.

Lambda:

- scales automatically.
- use reserved concurrency for blast-radius control.
- use provisioned concurrency for cold-start sensitive paths.

EC2:

- Auto Scaling Groups.
- Spot for fault-tolerant workers.
- Savings Plans for steady baseline.

## 7.2 DynamoDB Scaling

- On-demand for unpredictable workloads.
- Provisioned + autoscaling for predictable extreme scale.
- partition key must distribute traffic.
- use write sharding for hot keys.
- use GSIs carefully.

## 7.3 CDN Scaling

- cache static assets.
- cache video segments.
- use separate cache policies for:
  - frontend assets.
  - thumbnails.
  - video segments.
  - API responses only when safe.

## 7.4 Sharding

Shard by:

- user ID.
- conversation ID.
- creator ID.
- geography.
- content ID.

Avoid:

- one global hot partition.

## 7.5 Read Replicas

Use Aurora/RDS read replicas for:

- reporting.
- read-heavy relational queries.
- admin dashboards.

Do not use read replicas for:

- read-after-write critical paths unless consistency lag is handled.

## Example Request/Response

Request:

```http
GET /v1/feed?cursor=high-scale-page
```

Under load:

```text
Redis hit -> 20 ms
DynamoDB fallback -> 50 ms
recommendation service -> 100 ms+
```

## Debugging Strategy

- check throttling.
- check hot partitions.
- check ECS desired vs running count.
- check queue age.
- check cache hit ratio.

## Hands-On Task

- Enable DynamoDB on-demand.
- Add CloudWatch alarm for throttled requests.
- Add ECS autoscaling policy.

## Common Mistakes

- scaling compute while DB is bottleneck.
- not testing hot keys.
- ignoring account/service quotas.

## Deep Dive Explanation

- Billion scale is mostly partitioning and caching.
- Autoscaling cannot fix bad data modeling.

---

# 8. Caching Strategy

## 8.1 Redis Cache Locations

Use Redis for:

- profile cache.
- creator metadata.
- feed page cache.
- trending content.
- rate limits.
- online presence.
- short-lived recommendation result cache.

## 8.2 CloudFront Cache Locations

Use CloudFront for:

- static frontend.
- thumbnails.
- video manifests.
- video segments.
- public profile images.

## 8.3 Cache Invalidation

Strategies:

- TTL.
- write-through invalidation.
- event-driven invalidation.
- versioned object keys.

Best practice for media:

```text
Use versioned S3 object keys.
Avoid mass CloudFront invalidations.
```

## Example

Cache key:

```text
feed:user_123:v5:page_1
```

Expected behavior:

- hot feed page served from Redis.
- refresh after TTL or new event.

Debugging:

- check Redis hit rate.
- check evictions.
- check stale data complaints.
- check TTL.

## Hands-On Task

- Create ElastiCache Redis.
- Cache one profile response.
- Add TTL.

## Common Mistakes

- infinite TTL.
- cache as source of truth.
- no invalidation plan.
- caching personalized private data at CDN incorrectly.

## Deep Dive Explanation

- Cache improves latency by accepting controlled staleness.
- Never cache without knowing invalidation and privacy rules.

---

# 9. Queues And Streaming

## 9.1 When To Use SQS

Use SQS when:

- one worker group processes tasks.
- task must be retried.
- task can go to DLQ.
- ordering is not global.

Examples:

- video processing job.
- moderation job.
- email job.

## 9.2 When To Use SNS

Use SNS when:

- one event fans out to many subscribers.

Example:

```text
VideoUploaded -> processing queue, moderation queue, analytics queue
```

## 9.3 When To Use MSK/Kafka

Use MSK when:

- many services need replayable event log.
- Kafka ecosystem is required.
- event ordering by key matters.
- stream processing needs Kafka semantics.

Examples:

- social graph events.
- feed ranking events.
- message events.
- watch events.

## 9.4 When To Use Kinesis

Use Kinesis when:

- AWS-native stream ingestion is enough.
- clickstream/watch analytics.
- Firehose delivery to S3/OpenSearch.

## Example Event

```json
{
  "eventType": "VideoWatched",
  "userId": "user_123",
  "videoId": "vid_456",
  "watchMs": 184000,
  "timestamp": "2026-06-18T10:15:30Z"
}
```

Expected behavior under load:

- events are partitioned by user/video.
- consumers lag but do not drop events.
- DLQs catch poison task messages.

Debugging:

- SQS queue age.
- Kinesis iterator age.
- MSK consumer lag.
- DLQ count.
- producer error rate.

## Hands-On Task

- Create SQS queue with DLQ.
- Create SNS topic and subscribe SQS.
- Create Kinesis stream and put a sample event.

## Common Mistakes

- using SNS alone for critical delivery.
- no DLQ.
- non-idempotent consumers.
- wrong Kinesis partition key.

## Deep Dive Explanation

- Queues distribute work.
- Streams record events.
- Pub/sub broadcasts facts.

---

# 10. GenAI Integration

## 10.1 Content Recommendation

Inputs:

- watch history.
- likes.
- follows.
- skips.
- comments.
- video embeddings.
- creator embeddings.

AWS services:

- Kinesis/MSK for events.
- S3 data lake for training data.
- SageMaker AI for custom ranking models.
- Bedrock embeddings for semantic features.
- DynamoDB/OpenSearch for serving features/results.

## 10.2 Vector Search

Use:

- OpenSearch vector engine.
- Aurora pgvector for smaller relational vector needs.
- Bedrock Knowledge Bases for managed RAG.

Use cases:

- semantic video search.
- similar creator search.
- support chatbot retrieval.
- moderation evidence search.

## 10.3 Bedrock Usage

Use Bedrock for:

- caption generation.
- comment summarization.
- creator assistant.
- support chatbot.
- embeddings.
- moderation assist.
- RAG over internal docs.

## 10.4 RAG Pipeline

Flow:

```text
S3 docs
  -> chunking
  -> embeddings
  -> vector store
  -> retrieve relevant chunks
  -> Bedrock model
  -> answer with citations
```

## 10.5 Safety

Use:

- Bedrock Guardrails.
- PII redaction.
- prompt injection tests.
- output grounding.
- user authorization before retrieval.

## Example Request/Response

Request:

```json
{
  "query": "Find videos like this mountain biking clip",
  "seedVideoId": "vid_123"
}
```

Response:

```json
{
  "results": [
    {
      "videoId": "vid_987",
      "reason": "Similar sport, terrain, pacing, and creator cluster"
    }
  ]
}
```

Expected behavior under load:

- embeddings precomputed async.
- online request uses vector/search index.
- expensive GenAI calls rate-limited.

Debugging:

- embedding job status.
- vector index freshness.
- Bedrock throttles.
- prompt version.
- guardrail blocks.

## Hands-On Task

- Use Bedrock playground for one caption prompt.
- Create a small Knowledge Base over 5 docs.
- Ask questions and inspect citations.

## Common Mistakes

- calling LLM for every feed item online.
- no eval dataset.
- no guardrails.
- no cost budget.
- RAG without authorization filtering.

## Deep Dive Explanation

- GenAI should not sit directly in every hot path.
- Precompute where possible.
- Use retrieval and ranking for online latency.

---

# 11. CI/CD Pipeline

## 11.1 AWS-Native Pipeline

Use:

- CodePipeline.
- CodeBuild.
- ECR.
- ECS deploy.
- Lambda deploy.

Console steps:

- Go to `CodePipeline -> Create pipeline`.
- Source:
  - GitHub/CodeCommit.
- Build:
  - CodeBuild project.
- Deploy:
  - ECS service or Lambda.

## 11.2 BuildSpec Example

```yaml
version: 0.2

phases:
  pre_build:
    commands:
      - aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_URI
      - IMAGE_TAG=$CODEBUILD_RESOLVED_SOURCE_VERSION
  build:
    commands:
      - docker build -t $ECR_URI:$IMAGE_TAG .
  post_build:
    commands:
      - docker push $ECR_URI:$IMAGE_TAG
      - printf '[{"name":"api","imageUri":"%s"}]' $ECR_URI:$IMAGE_TAG > imagedefinitions.json
artifacts:
  files:
    - imagedefinitions.json
```

## 11.3 GitHub Actions Alternative

- Use GitHub OIDC to assume AWS role.
- Build Docker image.
- Push ECR.
- Update ECS service.
- Deploy Terraform through approved workflow.

Expected behavior:

- every deploy tied to Git commit.
- rollback uses previous image/task definition.
- prod deploy requires approval.

Debugging:

- check CodeBuild logs.
- check ECR image exists.
- check ECS deployment events.
- check ALB target health.

## Hands-On Task

- Create CodeBuild project.
- Build and push Docker image to ECR.
- Manually update ECS service to use new image.

## Common Mistakes

- storing AWS keys in CI.
- using `latest` tag only.
- no rollback plan.
- DB migration not backward-compatible.

## Deep Dive Explanation

- CI/CD is not only automation.
- It is release safety, auditability, rollback, and repeatability.

---

# 12. Observability

## 12.1 CloudWatch Logs

Use for:

- ECS app logs.
- Lambda logs.
- API Gateway logs.
- worker logs.

Log fields:

- requestId.
- userId hash.
- route.
- latencyMs.
- statusCode.
- errorCode.
- dependency latency.

## 12.2 Metrics

Track:

- ALB 5xx.
- API latency.
- ECS CPU/memory.
- Lambda errors/throttles.
- DynamoDB throttles.
- Redis evictions.
- SQS age/DLQ.
- Kinesis iterator age.
- OpenSearch latency.
- Bedrock token usage/error rate.

## 12.3 Alerts

Alarm on:

- user-facing 5xx.
- p95 latency.
- DLQ messages.
- queue age.
- low ECS running task count.
- DynamoDB throttles.
- high cache evictions.
- CloudFront origin errors.

## 12.4 X-Ray

Use for:

- tracing request across:
  - API Gateway.
  - Lambda.
  - ECS services.
  - DynamoDB.
  - external calls.

## Example Debug

Symptom:

```text
Feed page p95 latency increased from 150 ms to 900 ms.
```

Check:

- ALB target response time.
- ECS CPU/memory.
- Redis hit rate.
- DynamoDB latency/throttles.
- recommendation service latency.
- X-Ray trace segments.

## Hands-On Task

- Create CloudWatch dashboard.
- Add ALB, Lambda, DynamoDB, SQS widgets.
- Create alarm on DLQ messages.

## Common Mistakes

- logging secrets.
- no correlation ID.
- alerting only on CPU.
- no dashboard for async queues.

## Deep Dive Explanation

- Logs tell what happened.
- Metrics tell how much/how often.
- Traces tell where time went.
- CloudTrail tells who changed infrastructure.

---

# 13. Security

## 13.1 IAM Roles

Use:

- ECS task role.
- Lambda execution role.
- EC2 instance profile.
- GitHub/CodeBuild deploy role.

Rule:

```text
No static AWS keys in code, Docker, GitHub, or laptop scripts.
```

## 13.2 Least Privilege

Example:

- upload Lambda:
  - `s3:PutObject` to raw video bucket prefix.
  - `dynamodb:PutItem` to Videos table.
- feed service:
  - `dynamodb:Query` specific table/index.
  - Redis access through network only.

## 13.3 Encryption

Use:

- S3 SSE-S3 or SSE-KMS.
- DynamoDB encryption.
- RDS encryption.
- EBS encryption.
- TLS everywhere.
- Secrets Manager for secrets.

## 13.4 API Security

Use:

- Cognito JWT validation.
- WAF on CloudFront/ALB/API Gateway.
- rate limits.
- request size limits.
- schema validation.
- idempotency keys.

## 13.5 GenAI Security

Use:

- Bedrock Guardrails.
- no sensitive prompt logs.
- retrieval authorization.
- prompt injection tests.
- PII filters.

## Example Request

```http
POST /v1/messages
Authorization: Bearer <jwt>
Idempotency-Key: client-msg-123
```

Expected behavior:

- JWT validated.
- user permission checked.
- duplicate idempotency key returns same result.

Debugging:

- check API Gateway auth result.
- check backend authorization logs.
- check IAM deny in CloudTrail.
- check WAF logs.

## Hands-On Task

- Create IAM role for Lambda.
- Attach only S3/DynamoDB actions needed.
- Test denied action intentionally.

## Common Mistakes

- `AdministratorAccess` for app.
- public S3 bucket.
- hardcoded secrets.
- missing auth on WebSocket routes.

## Deep Dive Explanation

- IAM protects AWS resources.
- Cognito/JWT protects application users.
- Security groups protect network paths.
- KMS protects data keys.

---

# 14. Cost Optimization

## 14.1 Compute

Lambda:

- good for bursty/event tasks.
- expensive for high steady long-running compute.

ECS Fargate:

- good default for app services.
- pay for vCPU/memory while running.

EC2:

- cheaper for steady large workloads with good utilization.
- more operational burden.

## 14.2 CDN Savings

CloudFront:

- reduces origin requests.
- reduces S3/API load.
- improves global latency.

Cost controls:

- cache long-lived immutable assets.
- use lifecycle policies.
- compress assets.
- avoid unnecessary invalidations.

## 14.3 Storage

Use:

- S3 lifecycle.
- Intelligent-Tiering where access unknown.
- Glacier classes for archives.
- delete raw uploads after processing if policy allows.

## 14.4 Database

Use:

- DynamoDB on-demand for unpredictable learning workloads.
- provisioned/autoscaling for predictable high-scale workloads.
- Redis cache to reduce hot DB reads.
- OpenSearch sizing reviews.

## 14.5 GenAI

Control:

- token budget.
- prompt length.
- model choice.
- caching for deterministic outputs.
- batch/offline embedding generation.
- usage tags/inference profiles.

## Expected Behavior Under Load

- CloudFront reduces origin bill.
- queues smooth spikes.
- autoscaling adds capacity only when needed.

Debugging:

- Cost Explorer by service.
- group by usage type.
- check NAT Gateway bytes.
- check CloudWatch log ingestion.
- check Bedrock usage.

## Hands-On Task

- Create AWS Budget for $25.
- Add Cost Explorer saved view grouped by service.
- Add tags to all Terraform resources.

## Common Mistakes

- NAT Gateway surprise costs.
- CloudWatch log retention unlimited.
- always-on dev endpoints.
- high-token GenAI prompts.
- no lifecycle rules.

## Deep Dive Explanation

- Cost is an architecture signal.
- Expensive systems often reveal bad routing, caching, or data design.

---

# 15. How To Practice

## Week 1: Foundation

Build:

- VPC.
- public/private subnets.
- S3 frontend bucket.
- CloudFront distribution.

Deliverable:

- static frontend reachable through CloudFront.

## Week 2: Auth And API

Build:

- Cognito User Pool.
- API Gateway HTTP API.
- Lambda upload intent.
- DynamoDB Videos table.

Deliverable:

- authenticated upload intent API.

## Week 3: Media Upload Pipeline

Build:

- raw video S3 bucket.
- pre-signed upload.
- SQS queue.
- processing worker mock.

Deliverable:

- upload video -> metadata status changes from `UPLOADED` to `PROCESSING`.

## Week 4: Feed And Cache

Build:

- ECS feed service.
- ALB.
- Redis cache.
- DynamoDB query path.

Deliverable:

- feed API returns cached results.

## Week 5: Messaging

Build:

- API Gateway WebSocket.
- Messages DynamoDB table.
- sendMessage Lambda.
- SNS push placeholder.

Deliverable:

- two browser clients exchange messages.

## Week 6: Search

Build:

- OpenSearch domain/serverless collection.
- index sample videos.
- search API.

Deliverable:

- search videos by caption/hashtag.

## Week 7: Streaming Events

Build:

- Kinesis watch-events stream.
- producer from backend.
- consumer Lambda.
- S3 analytics sink.

Deliverable:

- watch events land in analytics bucket.

## Week 8: GenAI

Build:

- Bedrock prompt for captions.
- Knowledge Base over support docs.
- simple RAG chatbot.

Deliverable:

- chatbot answers from docs with citations.

## Week 9: CI/CD

Build:

- CodeBuild/CodePipeline or GitHub Actions OIDC.
- deploy Lambda and ECS.

Deliverable:

- push to main triggers dev deployment.

## Week 10: Observability + Security + Cost

Build:

- CloudWatch dashboard.
- alarms.
- WAF.
- budget.
- IAM least privilege review.

Deliverable:

- production-readiness checklist.

## Final Capstone

Demo:

- signup.
- upload video.
- process event.
- show feed.
- send message.
- search content.
- ask GenAI chatbot.
- show CloudWatch dashboard.

## Hands-On Task

- Start with Week 1 today.
- Do not skip VPC.
- Keep screenshots/notes of every resource you create.

## Common Mistakes

- trying to build all services in one weekend.
- skipping observability until the end.
- ignoring cleanup.
- not setting budgets.

## Deep Dive Explanation

- Real AWS learning comes from:
  - clicking.
  - breaking.
  - reading logs.
  - fixing permissions.
  - converting clicks into IaC.

---

# Official Source Notes

- CloudFront Origin Access Control for private S3 origins: <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html>
- API Gateway WebSocket APIs: <https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-websocket-api.html>
- Amazon Bedrock user guide: <https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html>
- Bedrock Knowledge Bases: <https://docs.aws.amazon.com/bedrock/latest/userguide/knowledge-base.html>
- Bedrock Agents: <https://docs.aws.amazon.com/bedrock/latest/userguide/agents.html>
- Bedrock Guardrails: <https://docs.aws.amazon.com/bedrock/latest/userguide/guardrails.html>
- SageMaker AI: <https://docs.aws.amazon.com/sagemaker/latest/dg/whatis.html>
- Terraform AWS provider CloudFront distribution resource: <https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/cloudfront_distribution>
- Terraform AWS provider API Gateway v2 API resource: <https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/apigatewayv2_api>

