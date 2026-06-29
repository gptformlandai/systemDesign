# Kubernetes EKS Production Architecture Gold Sheet

> Track: K8s Interview Track — Phase 7: Production Architecture
> Goal: Design and operate production-grade EKS clusters — Karpenter, IRSA, managed node groups, multi-AZ, networking — as a Staff Engineer or Platform Lead would.

---

## 0. How To Read This

Beginner focus:
- What EKS manages vs what you manage
- Managed node groups basics
- EKS addons

Intermediate focus:
- IRSA for AWS service access
- VPC CNI and pod networking on AWS
- Cluster Autoscaler vs Karpenter
- EKS-specific logging and monitoring

Senior / MAANG focus:
- Multi-AZ HA design: control plane, nodes, storage
- Karpenter NodePool design for cost optimization
- EKS with private API endpoint and VPN/DirectConnect
- EKS Anywhere and EKS on Outposts
- EKS upgrade strategy and zero-downtime

---

# Topic 1: EKS Architecture Overview

## 1. What AWS Manages vs What You Manage

```text
AWS manages (EKS control plane):
  ✅ API server (multi-AZ, auto-scaling)
  ✅ etcd cluster (3-node, 5-node SLA)
  ✅ Controller manager
  ✅ Scheduler
  ✅ Control plane upgrades
  ✅ Control plane HA (across 3 AZs)

You manage (data plane):
  Worker nodes (EC2 instances)
  Node upgrades (AMI, K8s version)
  Node networking (VPC CNI)
  Add-ons (CoreDNS, kube-proxy, EBS CSI driver)
  Application workloads
  RBAC, Network Policies, Pod Security
```

## 2. EKS Architecture Diagram

```text
AWS Account
├── EKS Control Plane (AWS-managed, multi-AZ)
│   ├── API Server (endpoint: https://xxx.gr7.us-east-1.eks.amazonaws.com)
│   ├── etcd
│   └── Scheduler + Controller Manager
│
└── Your VPC
    ├── us-east-1a:
    │   ├── Managed Node Group: m5.xlarge (3 nodes)
    │   └── Karpenter nodes
    ├── us-east-1b:
    │   ├── Managed Node Group: m5.xlarge (3 nodes)
    │   └── Karpenter nodes
    └── us-east-1c:
        ├── Managed Node Group: m5.xlarge (2 nodes)
        └── Karpenter nodes
```

## 3. EKS Cluster Creation (eksctl)

```bash
# cluster.yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: prod-cluster
  region: us-east-1
  version: "1.29"

iam:
  withOIDC: true    # MUST be enabled for IRSA

vpc:
  id: vpc-12345
  subnets:
    private:
      us-east-1a: {id: subnet-aaa}
      us-east-1b: {id: subnet-bbb}
      us-east-1c: {id: subnet-ccc}
    public:
      us-east-1a: {id: subnet-ddd}
      us-east-1b: {id: subnet-eee}
      us-east-1c: {id: subnet-fff}

managedNodeGroups:
  - name: system-nodes
    instanceType: m5.large
    minSize: 3
    maxSize: 6
    desiredCapacity: 3
    privateNetworking: true    # nodes in private subnets
    subnets:
      - subnet-aaa
      - subnet-bbb
      - subnet-ccc
    labels:
      role: system
    taints:
      - key: CriticalAddonsOnly
        effect: NoSchedule

addons:
  - name: vpc-cni
    version: latest
    attachPolicyARNs:
      - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
  - name: coredns
    version: latest
  - name: kube-proxy
    version: latest
  - name: aws-ebs-csi-driver
    version: latest
    wellKnownPolicies:
      ebsCSIController: true
```

---

# Topic 2: VPC CNI (AWS Pod Networking)

## 1. How VPC CNI Works

```text
AWS VPC CNI: pods get real VPC IP addresses (no overlay).

Node: m5.xlarge (ENIs: 4, IPs per ENI: 15 → max 58 pods, minus 3 for node itself)

Each pod:
  - Gets its own VPC IP from node's secondary IP range
  - Appears in VPC routing table as a real host
  - Can be security-group protected
  - No encapsulation overhead (better performance than overlay)

Tradeoff:
  + Better performance (native VPC routing)
  + Integration with Security Groups
  - IP exhaustion: large pod counts need large VPC CIDR
  - Max pods per node limited by ENI count and IPs per ENI
```

## 2. Increase Pod Density (Prefix Delegation)

```text
Problem: m5.xlarge supports only 58 pods by default.

Prefix Delegation:
  Instead of assigning individual IPs, assign /28 CIDR prefixes to ENIs.
  Each /28 = 16 IPs → more pods per node.
  m5.xlarge with prefix delegation: 4 ENIs * 15 prefixes * 16 IPs = 960 pods (theoretical)
```

```bash
kubectl set env daemonset aws-node -n kube-system ENABLE_PREFIX_DELEGATION=true
kubectl set env daemonset aws-node -n kube-system WARM_PREFIX_TARGET=1
```

## 3. Security Groups for Pods (SGP)

```text
Without SGP: all pods on a node share the node's Security Group.
With SGP: each pod gets its own Security Group (or group from SG pool).

Use case:
  payment-service pods: allow inbound 8080 only from api-gateway SG
  db pods: allow inbound 5432 only from payment-service SG
  Admin access: allow 22 only from bastion SG
```

```yaml
apiVersion: vpcresources.k8s.aws/v1beta1
kind: SecurityGroupPolicy
metadata:
  name: payment-service-sgp
  namespace: prod
spec:
  podSelector:
    matchLabels:
      app: payment-service
  securityGroups:
    groupIds:
      - sg-payment-service-id
```

---

# Topic 3: IRSA (IAM Roles for Service Accounts)

## 1. Full IRSA Setup

```bash
# 1. OIDC provider already enabled via eksctl withOIDC: true

# 2. Create IAM policy
aws iam create-policy \
  --policy-name payment-service-policy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject"],
      "Resource": "arn:aws:s3:::payment-receipts-bucket/*"
    }, {
      "Effect": "Allow",
      "Action": ["sqs:ReceiveMessage", "sqs:DeleteMessage"],
      "Resource": "arn:aws:sqs:us-east-1:123456:payment-queue"
    }]
  }'

# 3. Create ServiceAccount with IRSA annotation
eksctl create iamserviceaccount \
  --cluster=prod-cluster \
  --namespace=prod \
  --name=payment-service-sa \
  --attach-policy-arn=arn:aws:iam::123456:policy/payment-service-policy \
  --approve \
  --override-existing-serviceaccounts
```

Result in cluster:
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: payment-service-sa
  namespace: prod
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456:role/eksctl-prod-cluster-addon-iamserviceaccount-Role1-...
```

---

# Topic 4: Karpenter on EKS

## 1. Karpenter vs Cluster Autoscaler

```text
Cluster Autoscaler:
  Works with Auto Scaling Groups (ASGs)
  Must pre-configure node types and sizes
  Scale-out: 2-5 minutes (ASG launch template + bootstrap)
  
Karpenter:
  Directly calls EC2 Fleet API
  Chooses optimal instance type per pod requirements
  Scale-out: 30-60 seconds
  Consolidation: replaces expensive instances with cheaper ones
  Handles spot interruptions: cordons and replaces nodes automatically
```

## 2. Karpenter Setup

```bash
# Install Karpenter via Helm
helm install karpenter oci://public.ecr.aws/karpenter/karpenter \
  --version v0.36 \
  --namespace kube-system \
  --set settings.clusterName=prod-cluster \
  --set settings.interruptionQueue=prod-karpenter-interruption \
  --set controller.resources.requests.cpu=1 \
  --set controller.resources.requests.memory=1Gi
```

## 3. NodePool Configuration

```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: general-purpose
spec:
  template:
    metadata:
      labels:
        node-type: general
    spec:
      nodeClassRef:
        group: karpenter.k8s.aws
        kind: EC2NodeClass
        name: default
      requirements:
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["spot", "on-demand"]
        - key: node.kubernetes.io/instance-type
          operator: In
          values:
            - m5.large
            - m5.xlarge
            - m5.2xlarge
            - m6i.large
            - m6i.xlarge
            - m6i.2xlarge
            - m7i.large
            - m7i.xlarge
        - key: topology.kubernetes.io/zone
          operator: In
          values: ["us-east-1a", "us-east-1b", "us-east-1c"]
      expireAfter: 720h    # replace nodes after 30 days (security + drift)
  disruption:
    consolidationPolicy: WhenUnderutilized
    consolidateAfter: 1m    # consolidate after 1 minute of underutilization
  limits:
    cpu: 1000               # max 1000 vCPUs in this NodePool
    memory: 4000Gi

---
apiVersion: karpenter.k8s.aws/v1
kind: EC2NodeClass
metadata:
  name: default
spec:
  amiFamily: AL2023         # Amazon Linux 2023
  role: karpenter-node-role
  subnetSelectorTerms:
    - tags:
        karpenter.sh/discovery: prod-cluster
  securityGroupSelectorTerms:
    - tags:
        karpenter.sh/discovery: prod-cluster
  blockDeviceMappings:
    - deviceName: /dev/xvda
      ebs:
        volumeSize: 100Gi
        volumeType: gp3
        iops: 3000
        encrypted: true
```

## 4. Spot + On-Demand Blend Strategy

```yaml
# Prioritize spot; fall back to on-demand
requirements:
  - key: karpenter.sh/capacity-type
    operator: In
    values: ["spot"]       # try spot first

# Separate NodePool for critical pods (on-demand only)
---
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: critical-on-demand
spec:
  template:
    spec:
      requirements:
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["on-demand"]   # always on-demand for critical workloads
      taints:
        - key: critical
          effect: NoSchedule
```

---

# Topic 5: EKS Logging and Monitoring

## 1. Control Plane Logs

```text
EKS provides control plane log types (send to CloudWatch):
  api              — API server audit logs
  audit            — audit log for all K8s API calls
  authenticator    — authentication logs
  controllerManager — controller manager logs
  scheduler        — scheduler decisions

Enable:
  eksctl utils update-cluster-logging --cluster=prod-cluster --enable-types=all
```

## 2. Node and Pod Logs (Fluent Bit)

```text
EKS add-on: Amazon CloudWatch Observability (Fluent Bit)
  Replaces manual Fluent Bit DaemonSet
  Sends:
    - Container logs to CloudWatch Logs
    - Container metrics to CloudWatch Metrics
    - Node metrics from node-exporter to CloudWatch
```

## 3. EKS Container Insights

```text
Container Insights = CloudWatch metrics for EKS:
  - Per-namespace CPU/memory usage
  - Per-pod metrics
  - Cluster-level metrics

Prometheus + Grafana (alternative):
  More flexible, cheaper for high-cardinality metrics
  Better alerting customization
  Use AMP (Amazon Managed Prometheus) + AMG (Amazon Managed Grafana)
```

---

# Topic 6: EKS High Availability Design

## 1. HA Checklist

```text
Worker nodes:
  ✅ Nodes spread across 3 AZs
  ✅ TopologySpreadConstraints on critical Deployments
  ✅ PodDisruptionBudget on all production workloads
  ✅ Karpenter NodePool spread across 3 AZs

Storage:
  ✅ EBS: WaitForFirstConsumer (zone-aware provisioning)
  ✅ EFS: multi-AZ (for shared storage)
  ✅ Regular EBS volume snapshots

Networking:
  ✅ Ingress: AWS ALB in at least 2 AZs
  ✅ Cross-zone load balancing enabled on NLB
  ✅ Private subnets for nodes; public subnets only for LBs

Control plane:
  ✅ EKS control plane: managed by AWS, multi-AZ (handled)

Application:
  ✅ Replicas >= 3 per critical service
  ✅ Readiness probes prevent traffic to unready pods
  ✅ PreStop hook for graceful drain on pod termination
```

## 2. EKS Endpoint Access

```text
Public endpoint (default):
  kubectl from anywhere → EKS API server
  Node bootstrap → EKS API server
  Simple, but API server accessible from internet

Private endpoint:
  kubectl from VPC only (VPN or bastion required)
  Node bootstrap within VPC
  More secure; no API server exposure to internet

Recommended production:
  Public endpoint: yes, but restrict access CIDRs
  Private endpoint: yes (nodes use private endpoint)
  CIDR allowlist: [your corporate IP range]
```

---

# Topic 7: Revision Notes

- EKS: AWS manages control plane; you manage data plane (nodes, addons, RBAC, networking)
- VPC CNI: pods get real VPC IPs; no overlay; limited by ENI + IPs per node
- Prefix Delegation: increases pods per node by assigning /28 prefixes to ENIs
- Security Groups for Pods: per-pod SG for fine-grained AWS security group policies
- IRSA: per-SA AWS IAM role via OIDC; use `eksctl create iamserviceaccount`
- Karpenter: direct EC2 Fleet provisioning; faster than Cluster Autoscaler; consolidation support
- EKS NodePool: specify instance families, capacity type (spot/on-demand), AZ spread
- Control plane logs: api, audit, scheduler, controllerManager → CloudWatch
- HA: 3 AZ spread + TopologySpreadConstraints + PDB + WaitForFirstConsumer storage

## Official Source Notes

- EKS: <https://docs.aws.amazon.com/eks/latest/userguide/>
- Karpenter: <https://karpenter.sh/docs/>
- VPC CNI: <https://github.com/aws/amazon-vpc-cni-k8s>
- IRSA: <https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html>
- EKS Best Practices: <https://aws.github.io/aws-eks-best-practices/>
