# AWS Active Recall Question Bank

> Track: AWS Interview Track — Practice Upgrade
> Goal: Test knowledge with active retrieval across all 25 AWS Gold Sheets. Answer before reading the hint.

---

## How To Use This File

1. Cover the answer section
2. Read the question aloud
3. Answer in full sentences (or bullet points for lists)
4. Uncover and compare
5. Mark: ✅ solid, ⚠️ partial, ❌ missed
6. Revisit ❌ items before every interview

---

# Section 0: AWS CLI and Developer Tooling

**Q0.1.** Before running a write command in AWS CLI, what seven things should you verify?

> Hint: account, region, profile/role, resource, change, verification, rollback/cleanup

**Q0.2.** What command proves which AWS account and principal your CLI is currently using?

> Hint: STS caller identity

**Q0.3.** For human access in a company AWS environment, why is IAM Identity Center better than IAM user access keys?

> Hint: central login, MFA, short-lived credentials, account assignments

**Q0.4.** What is the purpose of `--query`, and what language does it use?

> Hint: client-side output filtering and reshaping

**Q0.5.** Why should you use service-side filters before `--query` when inspecting large AWS accounts?

> Hint: reduce API payload before client-side reshaping

**Q0.6.** What is the difference between `aws s3` and `aws s3api`?

> Hint: friendly high-level commands vs raw S3 API control

**Q0.7.** What are AWS CLI waiters, and why do production scripts use them?

> Hint: poll until resource reaches state; avoid racing async operations

**Q0.8.** How should GitHub Actions authenticate to AWS in a production-grade pipeline?

> Hint: OIDC assume-role, not static access keys

**Q0.9.** What are the first commands you run when the CLI seems to use the wrong account?

> Hint: configure list, env AWS vars, STS identity

**Q0.10.** What does `--dry-run` prove, and what does it not prove?

> Hint: IAM permission check for supported actions; does not validate every runtime dependency

---

# Section 1: Compute — EC2 and Auto Scaling

**Q1.** What is the difference between a Savings Plan and a Reserved Instance? When do you choose each?

> Hint: flexibility vs discount depth; compute vs instance family

**Q2.** What placement group type would you use for a tightly coupled HPC job that needs low-latency networking between instances?

> Hint: three types: Cluster, Spread, Partition

**Q3.** A web application is consistently at 70% CPU during business hours but near 0% overnight. What Auto Scaling policy is most cost-efficient?

> Hint: target tracking vs scheduled

**Q4.** What is the lifecycle hook in Auto Scaling and why would you use it?

> Hint: pauses instance at launch/termination to run custom logic

**Q5.** What instance type family would you use for an in-memory data processing job requiring 768 GB RAM?

> Hint: r family (memory optimized)

---

# Section 2: Networking — VPC and Load Balancing

**Q6.** Why are security groups called stateful while NACLs are called stateless? What is the practical difference?

> Hint: return traffic; ephemeral ports

**Q7.** You have 10 VPCs across 5 accounts that all need to communicate. What is better: VPC peering or Transit Gateway?

> Hint: peering doesn't scale, TGW is hub-and-spoke

**Q8.** What is the difference between a VPC Gateway Endpoint and a VPC Interface Endpoint? When is each free?

> Hint: S3 and DynamoDB; PrivateLink

**Q9.** Your ALB shows healthy backend targets but users report 502 errors. What do you check first?

> Hint: backend application returning invalid response, not ALB issue

**Q10.** What Route 53 routing policy would you use to test a new version with 5% of traffic?

> Hint: weighted routing

---

# Section 3: Containers — ECS and EKS

**Q11.** What is the difference between the ECS task role and the ECS execution role?

> Hint: who uses it; what for

**Q12.** What is IRSA and why is it better than EC2 instance profiles for EKS workloads?

> Hint: pod-level IAM vs node-level; OIDC token exchange

**Q13.** When would you choose ECS over EKS for a container workload?

> Hint: operational simplicity, no K8s ecosystem needed

**Q14.** What is Karpenter and how does it differ from the Kubernetes Cluster Autoscaler?

> Hint: speed, flexibility, instance type selection

**Q15.** A Lambda function in a VPC needs to call Secrets Manager. Why might this fail, and how do you fix it?

> Hint: NAT Gateway not configured; VPC Interface Endpoint for Secrets Manager

---

# Section 4: Serverless — Lambda and API Gateway

**Q16.** What causes Lambda cold starts? Name three ways to reduce cold start latency.

> Hint: init phase; provisioned concurrency, SnapStart, smaller package

**Q17.** What is the difference between reserved concurrency and provisioned concurrency in Lambda?

> Hint: cap vs pre-warm

**Q18.** A Lambda function processing SQS messages fails on one message in a batch of 10. What happens without ReportBatchItemFailures? What happens with it?

> Hint: full batch retry vs selective retry

**Q19.** When would you use REST API Gateway instead of HTTP API Gateway?

> Hint: usage plans, caching, request validation, API keys

**Q20.** Your Lambda function is slow at 128 MB. What AWS tool helps you find the optimal memory setting for cost and performance?

> Hint: AWS Lambda Power Tuning

---

# Section 5: Storage — S3

**Q21.** What is the difference between S3 Standard-IA and S3 Glacier Instant Retrieval? When is S3 Standard-IA a cost trap?

> Hint: retrieval latency; per-retrieval fee on frequent access

**Q22.** How do you give a user time-limited access to download a specific S3 object without making the bucket public?

> Hint: presigned URL

**Q23.** What is OAC and why is it preferred over OAI for S3 and CloudFront?

> Hint: newer, supports all S3 operations, more secure

**Q24.** A bucket has versioning enabled. A developer runs `aws s3 rm s3://bucket/important.json`. Is the file recoverable?

> Hint: delete marker created; version still exists

**Q25.** What is CloudFront cache behavior and when would you create multiple behaviors for one distribution?

> Hint: different origins or cache settings for different URL patterns

---

# Section 6: Database — RDS, Aurora, DynamoDB

**Q26.** What is the key architectural difference between Aurora and standard RDS that makes Aurora failover faster?

> Hint: shared distributed storage volume; no storage sync needed

**Q27.** Why can't you enable RDS encryption on an existing unencrypted database?

> Hint: must be done at creation; workaround is snapshot + restore

**Q28.** What is RDS Proxy and when is it critical?

> Hint: connection pooling; Lambda → RDS connection exhaustion

**Q29.** What is the hot partition problem in DynamoDB? Give an example of a bad partition key design.

> Hint: all writes go to one partition; date as partition key

**Q30.** What is DynamoDB single-table design? What must you know before designing the table?

> Hint: all entity types in one table; access patterns must be defined upfront

---

# Section 7: Caching — ElastiCache

**Q31.** When should you use cache-aside vs write-through caching strategy?

> Hint: cache-aside for reads; write-through for frequently read data

**Q32.** How do you implement a rate limiter using Redis? What data structure?

> Hint: INCR with EXPIRE; or sorted sets for sliding window

**Q33.** When would you choose Redis over Memcached for ElastiCache?

> Hint: persistence, replication, complex data structures, pub/sub

**Q34.** A Redis key holds a 50 MB object. What are the risks and what should you do instead?

> Hint: memory pressure; eviction; serialize + compress or store in S3

---

# Section 8: Security — IAM

**Q35.** Explain AWS IAM policy evaluation order in one minute.

> Hint: explicit deny → SCP → resource policy → permission boundary → identity policy → implicit deny

**Q36.** What is a permission boundary and how does it differ from a regular IAM policy?

> Hint: caps maximum, does not grant; effective = intersection of identity + boundary

**Q37.** What is iam:PassRole and why must it be scoped carefully?

> Hint: privilege escalation risk; always restrict to specific role ARNs

**Q38.** How does cross-account S3 access work? What two things must both allow the access?

> Hint: identity policy in Account A AND bucket policy in Account B

**Q39.** What is an SCP and what happens if an SCP denies an action but the IAM policy allows it?

> Hint: SCP wins; restricts max permissions

---

# Section 9: Security — Secrets, Encryption, Auth

**Q40.** Explain envelope encryption in one minute.

> Hint: CMK wraps DEK; DEK encrypts data; DEK stored encrypted alongside data

**Q41.** When would you use Secrets Manager vs SSM Parameter Store?

> Hint: rotation, cost, size; secrets = SM, config = SSM

**Q42.** What is the ViaService KMS condition and when do you use it?

> Hint: restrict CMK to be usable only through specific AWS services

**Q43.** A Cognito User Pool issues an ID token. Your backend API receives it. What must you validate?

> Hint: signature, expiry, iss, aud, token_use

**Q44.** When should you enable WAF in Count mode instead of Block mode?

> Hint: new rules; validate no false positives before blocking

---

# Section 10: Messaging

**Q45.** What is the SQS visibility timeout and what happens if it expires before the consumer deletes the message?

> Hint: message becomes visible again; reprocessed

**Q46.** Why must SQS Standard queue consumers be idempotent?

> Hint: at-least-once delivery; message may arrive twice

**Q47.** What is the SNS fan-out pattern? Draw the architecture.

> Hint: one SNS topic → multiple SQS queues → independent consumers

**Q48.** When would you choose Kinesis Data Streams over SQS?

> Hint: ordering, replay, multiple consumers with independent offsets

**Q49.** What is EventBridge Pipes and when is it useful?

> Hint: single source → filter → enrich → single target pipeline

---

# Section 11: Observability

**Q50.** What is the Embedded Metric Format (EMF) and why is it better than calling PutMetricData directly from Lambda?

> Hint: extract from logs, no API call, free

**Q51.** What is a composite alarm in CloudWatch and why do you need it?

> Hint: combine alarms; reduce noise; only alert when multiple conditions are true

**Q52.** What is X-Ray sampling and why is 100% sampling bad in production?

> Hint: overhead and cost; use default sampling or custom rules

**Q53.** What is CloudWatch Synthetics and what does it detect that a CloudWatch alarm cannot?

> Hint: external user journey monitoring; detects user-visible failures before metrics catch them

---

# Section 12: Operations — CloudTrail, Config, SSM

**Q54.** What is the difference between CloudTrail management events and data events? When do you enable data events?

> Hint: control plane vs data plane; S3 GetObject, Lambda Invoke

**Q55.** Why is Session Manager better than SSH + bastion host for EC2 access?

> Hint: no keys, no bastion to maintain, full audit log, IAM-controlled

**Q56.** What is an AWS Config conformance pack?

> Hint: bundle of Config rules mapped to a compliance framework (CIS, PCI, HIPAA)

**Q57.** How does AWS Config differ from CloudTrail?

> Hint: Config = state and compliance; CloudTrail = API call history

---

# Section 13: Senior Architecture — IaC and Release

**Q58.** Why should you never put static AWS access keys in GitHub Secrets for CI/CD?

> Hint: OIDC is better; no static keys; GitHub OIDC → STS assume role

**Q59.** What is the difference between blue-green and canary deployment?

> Hint: all-or-nothing switch vs gradual traffic shift

**Q60.** What is feature flag deployment and how does it enable safer releases than traditional deployment?

> Hint: deploy code but don't activate; rollback = flag flip; no redeploy

**Q61.** What is Terraform state drift and how do you detect it?

> Hint: `terraform plan` shows what changed; drift = real infra ≠ Terraform state

---

# Section 14: DR and Resilience

**Q62.** Define RTO and RPO. Which one is about time, which is about data?

> Hint: RTO = downtime; RPO = data loss

**Q63.** What is the difference between pilot light and warm standby DR strategies?

> Hint: pilot light: data replicated, minimal compute; warm standby: scaled-down running copy

**Q64.** Why use Global Accelerator instead of Route 53 for fastest failover?

> Hint: no DNS propagation delay; 30-second failover vs 1-5 minutes

**Q65.** What is Aurora Global Database's typical RPO and RTO?

> Hint: RPO < 1 second; RTO < 1 minute (managed failover)

---

# Section 15: Landing Zone and Governance

**Q66.** What is the purpose of the Management Account in AWS Organizations?

> Hint: billing, SCPs, account creation ONLY; never deploy workloads

**Q67.** An SCP denies an action but you have AdministratorAccess IAM policy. Can you perform the action?

> Hint: no; SCP restricts max permissions regardless of IAM policy

**Q68.** What is AWS Control Tower Account Factory?

> Hint: automated account provisioning with baseline guardrails applied

**Q69.** Why is IAM Identity Center preferred over creating IAM users for each developer?

> Hint: SSO, federated, no static keys, temporary credentials, single management point

---

# Section 16: Advanced Networking and FinOps

**Q70.** What is the key limitation of VPC peering that Transit Gateway solves?

> Hint: peering not transitive; TGW allows transitive routing

**Q71.** When would you choose PrivateLink over VPC peering?

> Hint: expose single service; overlapping CIDRs OK; one-directional

**Q72.** Name two strategies to reduce NAT Gateway data processing costs.

> Hint: S3 Gateway Endpoint; DynamoDB Gateway Endpoint; Interface Endpoints for ECR/SSM/SM

**Q73.** What is the hybrid DNS architecture for AWS + on-premises?

> Hint: Route 53 Resolver Inbound Endpoint (on-prem → AWS) + Outbound Endpoint (AWS → on-prem)

---

# Section 17: GenAI — Bedrock and SageMaker

**Q74.** What is RAG and why does it improve LLM responses?

> Hint: retrieval-augmented generation; injects relevant context from knowledge base

**Q75.** What is a Bedrock Guardrail and what types of protection does it offer?

> Hint: content filter, PII masking, topic denial, grounding check

**Q76.** When would you use SageMaker for an LLM workload instead of Bedrock?

> Hint: fine-tuning on private data, specific hardware, model not on Bedrock

**Q77.** What is prompt versioning and why does it matter for LLMOps?

> Hint: prompts as code; independent deployment; rollback without code redeploy

**Q78.** What is the difference between Kinesis Data Streams and Kinesis Data Firehose?

> Hint: Streams: custom consumers, replay, order; Firehose: managed delivery to S3/Redshift/OpenSearch

---

# Quick Score Tracker

| Section | Questions | Solid ✅ | Partial ⚠️ | Missed ❌ |
|---|---|---|---|---|
| 1: Compute | 1-5 | | | |
| 2: Networking | 6-10 | | | |
| 3: Containers | 11-15 | | | |
| 4: Serverless | 16-20 | | | |
| 5: Storage | 21-25 | | | |
| 6: Database | 26-30 | | | |
| 7: Cache | 31-34 | | | |
| 8: IAM | 35-39 | | | |
| 9: Secrets/Auth | 40-44 | | | |
| 10: Messaging | 45-49 | | | |
| 11: Observability | 50-53 | | | |
| 12: Operations | 54-57 | | | |
| 13: Release | 58-61 | | | |
| 14: DR | 62-65 | | | |
| 15: Governance | 66-69 | | | |
| 16: Networking/FinOps | 70-73 | | | |
| 17: GenAI | 74-78 | | | |

Target: ≥ 70% solid before the interview.
