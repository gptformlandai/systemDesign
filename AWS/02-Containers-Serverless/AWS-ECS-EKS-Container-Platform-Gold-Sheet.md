# AWS Containers: ECS and EKS Gold Sheet

> Track: AWS Interview Track — Containers and Serverless
> Goal: choose between ECS and EKS confidently, deploy production container workloads, and explain trade-offs at MAANG depth.

---

## 0. How To Read This

Beginner focus:
- ECS task definition, service, cluster
- ECS Fargate vs EC2 launch type
- ECR — container registry

Intermediate focus:
- ECS task IAM roles, secrets injection
- ECS service auto scaling
- EKS node groups vs Fargate profiles
- kubectl basics, namespaces, deployments

Senior / MAANG focus:
- IRSA (IAM Roles for Service Accounts) in EKS
- EKS Cluster Autoscaler vs Karpenter
- HPA / VPA in Kubernetes
- EKS managed node groups vs self-managed
- ECS vs EKS decision framework
- Container image security scanning (ECR + Inspector)
- Multi-tenant cluster design

---

# Topic 1: ECS — Elastic Container Service

## 1. Intuition

ECS is AWS's managed container orchestration service.

You tell ECS what containers to run, how much CPU/memory they need, and how many copies. ECS handles scheduling, placement, health checking, and replacing failed containers.

Mental model:

```text
Task Definition = blueprint (container image, CPU, memory, env vars, ports, IAM role)
Task = one running instance of a task definition
Service = maintains N healthy tasks (like a deployment + ReplicaSet in Kubernetes)
Cluster = logical grouping of tasks and services
```

## 2. Launch Types: Fargate vs EC2

| Feature | Fargate | EC2 Launch Type |
|---|---|---|
| Server management | AWS manages | you manage EC2 instances |
| Scaling granularity | per-task CPU/RAM | instance-level |
| Startup speed | ~30-60 seconds | faster if instance is warm |
| Cost model | per task vCPU + memory per second | per EC2 instance-hour |
| Use case | stateless apps, variable load | GPU workloads, custom instance types |
| Spot support | yes (Fargate Spot) | yes |

Interview line:

```text
I use Fargate by default. It removes EC2 patching, capacity management, and AMI management.
I use EC2 launch type when I need GPU instances, custom AMIs, or need instance store.
```

## 3. Task Definition

```json
{
  "family": "payment-service",
  "taskRoleArn": "arn:aws:iam::123456789012:role/payment-service-task-role",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecs-execution-role",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [{
    "name": "payment-service",
    "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/payment-service:v1.2.3",
    "portMappings": [{"containerPort": 8080}],
    "environment": [{"name": "SPRING_PROFILES_ACTIVE", "value": "prod"}],
    "secrets": [
      {"name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:us-east-1:...:secret:payment-db-pass"}
    ],
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/payment-service",
        "awslogs-region": "us-east-1",
        "awslogs-stream-prefix": "ecs"
      }
    }
  }]
}
```

Key fields:
- `taskRoleArn`: permissions the container uses to call AWS services (S3, SQS, etc.)
- `executionRoleArn`: permissions ECS uses to pull image, write logs, read secrets
- `secrets`: inject from Secrets Manager or SSM Parameter Store at runtime

## 4. IAM Roles In ECS

Two separate roles:

| Role | Who Uses It | What It Grants |
|---|---|---|
| Task Execution Role | ECS agent | pull ECR image, write CloudWatch logs, read Secrets Manager secrets |
| Task Role | container app | S3 access, SQS send/receive, DynamoDB reads, etc. |

Interview trap:

```text
The task role and execution role are different.
If the app cannot write to S3, check the task role.
If the container fails to start (image pull fails, log driver error), check the execution role.
```

## 5. ECS Service Auto Scaling

Policies:
- Target Tracking: maintain metric at target value
- Step Scaling: alarm-triggered incremental changes
- Scheduled: set desired count at known times

Useful metrics:
- `ECSServiceAverageCPUUtilization`
- `ECSServiceAverageMemoryUtilization`
- ALB `RequestCountPerTarget`

Scale-in protection: mark task as scale-in-protected if processing a long job.

## 6. ECS Networking (awsvpc Mode)

With `awsvpc` network mode (required for Fargate):
- each task gets its own elastic network interface (ENI)
- task gets its own private IP in the subnet
- security groups apply at task level (not host level)

This makes ECS security groups work like VM-level controls:

```text
Security group: payment-service-sg
  Inbound: port 8080 from alb-sg
  Outbound: port 5432 to rds-sg, port 443 to 0.0.0.0/0 (HTTPS APIs)
```

## 7. ECS Service Discovery And Load Balancing

Two patterns:

1. ALB with target group (most common):
   - ECS service registers tasks with ALB target group
   - ALB routes traffic to healthy tasks
   - supports rolling updates with health check gates

2. AWS Cloud Map (service discovery):
   - tasks register private DNS entries
   - other services resolve by name
   - useful for east-west service-to-service without ALB

---

# Topic 2: ECR — Elastic Container Registry

## 1. What ECR Does

ECR is a private Docker container registry managed by AWS.

Benefits over Docker Hub:
- stays inside AWS network (no internet egress for image pulls)
- IAM-controlled access
- vulnerability scanning with Inspector v2
- lifecycle policies to clean up old images

## 2. Key ECR Operations

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Tag and push
docker tag my-app:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-app:v1.0.0
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-app:v1.0.0
```

## 3. ECR Lifecycle Policy

Auto-delete old images:

```json
{
  "rules": [{
    "rulePriority": 1,
    "description": "Keep only 10 tagged images",
    "selection": {
      "tagStatus": "tagged",
      "tagPatternList": ["v*"],
      "countType": "imageCountMoreThan",
      "countNumber": 10
    },
    "action": {"type": "expire"}
  }]
}
```

---

# Topic 3: EKS — Elastic Kubernetes Service

## 1. Intuition

EKS is AWS-managed Kubernetes. AWS manages the control plane (API server, etcd, scheduler). You manage or choose managed node groups for the data plane.

Use EKS when:
- team already knows Kubernetes
- you need Kubernetes ecosystem (Helm, Argo, service mesh, custom operators)
- you want cloud-portable workloads
- complex scheduling, custom resource definitions, or multi-tenancy matter

## 2. EKS vs ECS Decision Framework

| Factor | Favor ECS | Favor EKS |
|---|---|---|
| Team Kubernetes experience | low | high |
| Kubernetes portability | not needed | needed |
| Custom scheduler/operators | no | yes |
| Helm chart ecosystem | not needed | helpful |
| Operational simplicity | priority | acceptable complexity |
| AWS-native integration depth | highest | good (with add-ons) |

## 3. EKS Node Options

| Option | What It Is | When To Use |
|---|---|---|
| Managed Node Groups | AWS creates/manages EC2, handles AMI updates | default choice for most workloads |
| Self-Managed Node Groups | you create EC2 ASG, manage AMIs | custom kernels, custom agents |
| Fargate Profiles | pods run on Fargate (no EC2 nodes) | burstable workloads, no node management |
| Karpenter | open-source just-in-time node provisioner | faster and more flexible than Cluster Autoscaler |

## 4. IRSA — IAM Roles For Service Accounts

IRSA lets Kubernetes pods assume IAM roles without static credentials.

How it works:

```text
EKS creates an OIDC provider endpoint for the cluster
-> Kubernetes service account annotated with IAM role ARN
-> Pod uses service account
-> Pod receives temporary credentials via OIDC token exchange
-> AWS validates token against cluster's OIDC endpoint
```

Setup:

```bash
# Create IAM role for service account
eksctl create iamserviceaccount \
  --cluster my-cluster \
  --namespace default \
  --name payment-service \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess \
  --approve
```

Kubernetes YAML:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payment-service
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/payment-service-role
```

Interview trap:

```text
Without IRSA, apps in EKS use the EC2 instance profile — which means all pods on that node
share the same IAM permissions. IRSA gives pod-level IAM isolation, which is the security
best practice for EKS.
```

## 5. Cluster Autoscaler vs Karpenter

| Feature | Cluster Autoscaler | Karpenter |
|---|---|---|
| Provisioning speed | 2-5 minutes | 30-60 seconds |
| Node flexibility | scales existing node groups | provisions any instance type |
| Cost optimization | manual instance type selection | automatically picks cheapest |
| AWS-native | AWS add-on version | AWS-sponsored open source |
| Recommended for | existing ASG-based setups | new clusters, cost efficiency |

## 6. HPA And VPA

Horizontal Pod Autoscaler (HPA):
- scales replica count based on CPU/memory or custom metrics
- requires metrics-server
- works for stateless workloads

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
```

Vertical Pod Autoscaler (VPA):
- adjusts CPU/memory requests for pods
- requires pod restart (cannot adjust live)
- use in Recommendation mode to right-size, not Auto mode in prod without care

## 7. EKS Networking

AWS VPC CNI (default):
- each pod gets a VPC IP from the node's subnet
- no overlay network, full VPC routing
- security groups can apply to pods (Security Groups for Pods)

Common add-ons:
- CoreDNS: in-cluster DNS
- kube-proxy: service networking rules
- AWS Load Balancer Controller: provisions ALB/NLB for Kubernetes Ingress/Services

## 8. EKS Security Best Practices

- Use IRSA, never static credentials
- Apply pod security standards (Baseline or Restricted)
- Enable control plane logging to CloudWatch
- Use private endpoint for API server in production
- Scan images with ECR + Inspector before deployment
- Apply network policies (Calico or VPC CNI Network Policy)
- Keep managed node groups updated (use EKS managed patching)

## 9. Common Mistakes

| Mistake | Better Approach |
|---|---|
| Share one IAM role for all ECS tasks | one task role per service, least privilege |
| Store secrets in environment variables directly | use Secrets Manager / SSM secret injection |
| EKS without IRSA | configure IRSA so pods have isolated IAM scope |
| Skip ECR lifecycle policies | old images accumulate, increase storage cost |
| Use latest tag in task definitions | use immutable versioned tags for reproducible deploys |
| Public ECR without auth | ECR private registry, IAM-controlled |
| Skip container resource limits in EKS | no limits = noisy neighbor kills other pods |

## 10. Interview Scenarios

**Scenario**: "How would you deploy a multi-service Spring Boot app on ECS?"

Strong answer:

```text
Each service gets its own task definition with its service's ECR image, task role (least
privilege for that service's AWS access), and secrets injected from Secrets Manager.
Each service runs as an ECS Service behind an ALB target group. Services are in private
subnets. ALB is in public subnets. CloudWatch Container Insights provides metrics and logs.
Service auto scaling uses target tracking on ALB RequestCountPerTarget. I use CodePipeline
or GitHub Actions to build, scan, push to ECR, and update ECS services with rolling updates.
```

**Scenario**: "When would you choose EKS over ECS?"

Strong answer:

```text
I choose EKS when the team has Kubernetes expertise and the workload benefits from the
Kubernetes ecosystem — Helm charts, Argo CD, service mesh, custom operators, or CRDs.
I choose ECS when I want simpler AWS-native operations with less Kubernetes abstraction.
ECS is easier to operate for teams without K8s experience. For new greenfield projects with
no existing K8s dependency, ECS Fargate is often the better default.
```

## 11. Revision Notes

- ECS: task definition → service → cluster; task role ≠ execution role
- Fargate: no EC2 to manage; EC2 launch type: more control, GPU support
- ECS secrets injection: reference from Secrets Manager ARN in task definition
- EKS: IRSA replaces instance profile for pod-level IAM; configure via OIDC + service account annotation
- Cluster Autoscaler: slower; Karpenter: faster, flexible, preferred for new clusters
- HPA scales replicas; VPA scales resource requests (needs restart)
- Security: IRSA, image scanning, pod security standards, private API endpoint

## 12. Official Source Notes

- ECS: <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html>
- EKS: <https://docs.aws.amazon.com/eks/latest/userguide/what-is-eks.html>
- IRSA: <https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html>
- Karpenter: <https://karpenter.sh/>
- ECR: <https://docs.aws.amazon.com/AmazonECR/latest/userguide/what-is-ecr.html>
