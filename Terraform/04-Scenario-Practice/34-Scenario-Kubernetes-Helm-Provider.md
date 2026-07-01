# 34. Kubernetes and Helm Provider: Managing Cluster Resources with Terraform

## Overview

After provisioning an EKS/GKE/AKS cluster with Terraform, you need to deploy workloads and cluster add-ons. Two providers enable this from within Terraform:

```text
kubernetes provider  → manage K8s resources: Namespace, ConfigMap, ServiceAccount,
                       Secret, Deployment, Service, RBAC rules
                       
helm provider        → manage Helm chart releases: cert-manager, external-dns,
                       cluster-autoscaler, ingress-nginx, KEDA, etc.
```

---

## The Core Challenge: Provider Initialization Order

The Kubernetes/Helm provider needs the cluster endpoint and auth credentials — which don't exist until the cluster is created. This creates a **chicken-and-egg problem**.

```text
Problem:
  Terraform evaluates ALL provider blocks before planning resources.
  The kubernetes provider block references aws_eks_cluster.main.endpoint
  → But aws_eks_cluster.main doesn't exist until AFTER plan+apply!

Solutions:
  1. Two-phase apply: apply cluster first, then apply K8s resources
  2. Use data sources: read cluster from existing state (for separate root modules)
  3. Single root module with depends_on on provider-using resources
```

---

## Provider Configuration

### EKS (Recommended Pattern)

```hcl
# versions.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.14"
    }
  }
}

# providers.tf
provider "aws" {
  region = var.aws_region
}

# EKS cluster data — used to configure kubernetes and helm providers
data "aws_eks_cluster" "main" {
  name       = aws_eks_cluster.main.name
  depends_on = [aws_eks_cluster.main]
}

data "aws_eks_cluster_auth" "main" {
  name       = aws_eks_cluster.main.name
  depends_on = [aws_eks_cluster.main]
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.main.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.main.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.main.token
}

provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.main.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.main.certificate_authority[0].data)
    token                  = data.aws_eks_cluster_auth.main.token
  }
}
```

---

## Kubernetes Provider Resources

### Namespace

```hcl
resource "kubernetes_namespace_v1" "app" {
  metadata {
    name = "myapp"
    labels = {
      environment = var.environment
      managed-by  = "terraform"
    }
  }
}
```

### ServiceAccount With IRSA (EKS Pod Identity)

```hcl
# IRSA = IAM Roles for Service Accounts
resource "kubernetes_service_account_v1" "app" {
  metadata {
    name      = "myapp"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.app.arn
    }
  }
}
```

### ConfigMap

```hcl
resource "kubernetes_config_map_v1" "app_config" {
  metadata {
    name      = "myapp-config"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
  }

  data = {
    APP_ENV       = var.environment
    LOG_LEVEL     = "info"
    DATABASE_HOST = aws_db_instance.main.address
    REDIS_URL     = "redis://${aws_elasticache_cluster.main.cache_nodes[0].address}:6379"
  }
}
```

### Secret (With AWS SSM Source)

```hcl
data "aws_ssm_parameter" "db_password" {
  name            = "/prod/myapp/db-password"
  with_decryption = true
}

resource "kubernetes_secret_v1" "db_credentials" {
  metadata {
    name      = "db-credentials"
    namespace = kubernetes_namespace_v1.app.metadata[0].name
  }

  type = "Opaque"

  data = {
    DB_PASSWORD = data.aws_ssm_parameter.db_password.value
    DB_USER     = "myapp"
  }
}
```

---

## Helm Provider: Installing Cluster Add-ons

### cert-manager

```hcl
resource "helm_release" "cert_manager" {
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = "v1.14.5"
  namespace        = "cert-manager"
  create_namespace = true

  set {
    name  = "installCRDs"
    value = "true"
  }

  set {
    name  = "global.leaderElection.namespace"
    value = "cert-manager"
  }

  depends_on = [aws_eks_node_group.main]  # wait for nodes, not just cluster
}
```

### external-dns

```hcl
resource "helm_release" "external_dns" {
  name             = "external-dns"
  repository       = "https://kubernetes-sigs.github.io/external-dns/"
  chart            = "external-dns"
  version          = "1.14.4"
  namespace        = "external-dns"
  create_namespace = true

  values = [
    yamlencode({
      provider = "aws"
      aws = {
        region          = var.aws_region
        zoneType        = "public"
        evaluateTargetHealth = true
      }
      serviceAccount = {
        annotations = {
          "eks.amazonaws.com/role-arn" = aws_iam_role.external_dns.arn
        }
      }
      txtOwnerId = "my-cluster-${var.environment}"
    })
  ]

  depends_on = [helm_release.cert_manager]
}
```

### cluster-autoscaler

```hcl
resource "helm_release" "cluster_autoscaler" {
  name             = "cluster-autoscaler"
  repository       = "https://kubernetes.github.io/autoscaler"
  chart            = "cluster-autoscaler"
  version          = "9.36.0"
  namespace        = "kube-system"

  set {
    name  = "autoDiscovery.clusterName"
    value = aws_eks_cluster.main.name
  }

  set {
    name  = "awsRegion"
    value = var.aws_region
  }

  set {
    name  = "rbac.serviceAccount.annotations.eks\\.amazonaws\\.com/role-arn"
    value = aws_iam_role.cluster_autoscaler.arn
  }

  depends_on = [aws_eks_node_group.main]
}
```

### ingress-nginx

```hcl
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = "4.10.1"
  namespace        = "ingress-nginx"
  create_namespace = true

  values = [
    yamlencode({
      controller = {
        replicaCount = 2
        service = {
          annotations = {
            "service.beta.kubernetes.io/aws-load-balancer-type" = "nlb"
          }
        }
        metrics = {
          enabled        = true
          serviceMonitor = { enabled = var.prometheus_enabled }
        }
      }
    })
  ]

  depends_on = [aws_eks_node_group.main]
}
```

---

## EKS Managed Add-ons (AWS Native, Alternative to Helm)

For AWS-native add-ons, use `aws_eks_addon` instead of Helm:

```hcl
# VPC CNI — managed by AWS, auto-updates
resource "aws_eks_addon" "vpc_cni" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "vpc-cni"
  addon_version            = "v1.18.1-eksbuild.1"
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "PRESERVE"

  configuration_values = jsonencode({
    env = {
      ENABLE_PREFIX_DELEGATION = "true"
      WARM_PREFIX_TARGET       = "1"
    }
  })
}

resource "aws_eks_addon" "coredns" {
  cluster_name  = aws_eks_cluster.main.name
  addon_name    = "coredns"
  addon_version = "v1.11.1-eksbuild.9"
  depends_on    = [aws_eks_node_group.main]
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name  = aws_eks_cluster.main.name
  addon_name    = "kube-proxy"
  addon_version = "v1.29.3-eksbuild.2"
}

resource "aws_eks_addon" "pod_identity_agent" {
  cluster_name  = aws_eks_cluster.main.name
  addon_name    = "eks-pod-identity-agent"
  addon_version = "v1.3.0-eksbuild.1"
}
```

---

## Managing aws-auth ConfigMap

The `aws-auth` ConfigMap in `kube-system` controls RBAC access for AWS IAM roles/users.

```hcl
resource "kubernetes_config_map_v1" "aws_auth" {
  metadata {
    name      = "aws-auth"
    namespace = "kube-system"
  }

  data = {
    mapRoles = yamlencode([
      {
        rolearn  = aws_iam_role.eks_node_group.arn
        username = "system:node:{{EC2PrivateDNSName}}"
        groups   = ["system:bootstrappers", "system:nodes"]
      },
      {
        rolearn  = "arn:aws:iam::${var.account_id}:role/DevTeamRole"
        username = "dev-team"
        groups   = ["dev-access"]
      },
    ])
    mapUsers = yamlencode([
      {
        userarn  = "arn:aws:iam::${var.account_id}:user/ci-bot"
        username = "ci-bot"
        groups   = ["system:masters"]
      }
    ])
  }

  depends_on = [aws_eks_cluster.main]
}

# NOTE: For EKS 1.29+ with Access Entries (preferred over aws-auth ConfigMap):
resource "aws_eks_access_entry" "dev_team" {
  cluster_name  = aws_eks_cluster.main.name
  principal_arn = "arn:aws:iam::${var.account_id}:role/DevTeamRole"
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "dev_team" {
  cluster_name  = aws_eks_cluster.main.name
  principal_arn = "arn:aws:iam::${var.account_id}:role/DevTeamRole"
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSViewPolicy"
  access_scope {
    type       = "namespace"
    namespaces = ["myapp", "dev"]
  }
}
```

---

## Common Pitfall: Provider Dependency Cycle

```text
Error you'll see:
  Error: Provider configuration not present
  
  A provider configuration block is required for all operations.
  To use hashicorp/kubernetes, please add a provider configuration.

Cause:
  The kubernetes provider block references resource outputs that
  don't exist yet (e.g., aws_eks_cluster.main.endpoint before cluster is created).

Fix:
  Use depends_on on resources that use the kubernetes provider:

resource "kubernetes_namespace_v1" "app" {
  metadata { name = "myapp" }
  depends_on = [
    aws_eks_cluster.main,
    aws_eks_node_group.main,
  ]
}

  OR: Split into two separate root modules:
    - Module 1: EKS cluster (apply first)
    - Module 2: K8s resources (reads cluster from data source)
```

---

## Terraform vs Helm vs ArgoCD: When To Use What

```text
Terraform (kubernetes + helm provider):
  ✓ Cluster add-ons with complex IAM/IRSA (cert-manager, external-dns, cluster-autoscaler)
  ✓ Infrastructure-level K8s resources (RBAC, namespaces, service accounts with IAM)
  ✓ One-time cluster configuration
  ✗ Application deployments (use ArgoCD/Flux instead — they handle rollouts, rollbacks)
  ✗ Frequent deploys (Terraform is not designed for deployment pipelines)

ArgoCD / Flux (GitOps):
  ✓ Application deployments (Deployments, Services, Ingresses)
  ✓ Continuous reconciliation (drift detection for K8s resources)
  ✓ Rollback, rollout, sync strategies
  ✗ Creating the cluster itself or AWS resources

helmfile (standalone):
  ✓ Managing many Helm releases without Terraform
  ✓ Value files per environment
  ✗ Does not manage AWS resources
  
Best practice in production:
  Terraform → provision EKS cluster + cluster-level add-ons (via Helm provider)
  ArgoCD → deploy all application Helm charts (continuous GitOps)
```

---

## Interview Sound Bite

The Kubernetes and Helm providers let Terraform manage K8s resources and Helm releases after cluster creation, but they introduce a dependency ordering problem: the provider config needs the cluster endpoint, which doesn't exist until the cluster is applied. The solution is `depends_on` on K8s resources and using `data "aws_eks_cluster"` (which defers resolution to apply time). In practice, use the Helm provider for cluster infrastructure add-ons with IAM dependencies (cert-manager, external-dns, cluster-autoscaler via IRSA), and use EKS managed add-ons (`aws_eks_addon`) for AWS-native components like vpc-cni and coredns. Application deployments belong in ArgoCD or Flux — not Terraform — because GitOps tools handle rollouts and continuous reconciliation that Terraform's stateful apply model is not designed for.
