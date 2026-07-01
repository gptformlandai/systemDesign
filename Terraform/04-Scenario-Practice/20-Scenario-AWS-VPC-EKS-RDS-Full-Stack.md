# 20. Scenario: AWS VPC + EKS + RDS Full Stack

## Architecture Overview

```text
VPC (10.0.0.0/16)
├── Public subnets (10.0.1.0/24, 10.0.2.0/24, 10.0.3.0/24)
│   └── NAT Gateway (for private subnet outbound)
│   └── Internet Gateway
├── Private subnets (10.0.11.0/24, 10.0.12.0/24, 10.0.13.0/24)
│   └── EKS Node Group (EC2 instances)
│   └── RDS Subnet Group
└── RDS private subnet (10.0.21.0/24, 10.0.22.0/24)

EKS Cluster
├── Managed Node Group (t3.large, min:2, max:10)
├── OIDC Provider (for IRSA)
└── Add-ons: CoreDNS, kube-proxy, VPC CNI

RDS PostgreSQL
├── Multi-AZ
├── Subnet Group (private subnets)
└── Security Group (only EKS nodes can connect on 5432)
```

---

## Project Structure

```text
stack/
├── versions.tf          ← provider versions + backend
├── variables.tf         ← all input variables
├── main.tf              ← calls vpc, eks, rds modules
├── outputs.tf           ← expose key values
├── locals.tf            ← computed values
├── data.tf              ← AZs, AMI, caller identity
└── modules/
    ├── vpc/
    ├── eks/
    └── rds/
```

---

## versions.tf

```hcl
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.24"
    }
  }

  backend "s3" {
    bucket         = "mycompany-terraform-state"
    key            = "services/myapp/prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
```

---

## data.tf

```hcl
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
```

---

## locals.tf

```hcl
locals {
  name_prefix = "${var.project}-${var.environment}"
  account_id  = data.aws_caller_identity.current.account_id
  region      = data.aws_region.current.name

  # Take first 3 AZs
  azs = slice(data.aws_availability_zones.available.names, 0, 3)

  # CIDR blocks
  vpc_cidr         = "10.0.0.0/16"
  public_cidrs     = [for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i + 1)]
  private_cidrs    = [for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i + 11)]
  db_subnet_cidrs  = [for i, az in local.azs : cidrsubnet(local.vpc_cidr, 8, i + 21)]
}
```

---

## modules/vpc/main.tf (Key Resources)

```hcl
resource "aws_vpc" "this" {
  cidr_block           = var.cidr_block
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = { Name = var.name }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-igw" }
}

resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
  tags = {
    Name                     = "${var.name}-public-${count.index + 1}"
    "kubernetes.io/role/elb" = "1"   # required for AWS Load Balancer Controller
  }
}

resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  tags = {
    Name                              = "${var.name}-private-${count.index + 1}"
    "kubernetes.io/role/internal-elb" = "1"
  }
}

resource "aws_eip" "nat" {
  count  = var.enable_nat_gateway ? 1 : 0
  domain = "vpc"
  tags   = { Name = "${var.name}-nat-eip" }
}

resource "aws_nat_gateway" "this" {
  count         = var.enable_nat_gateway ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public[0].id
  depends_on    = [aws_internet_gateway.this]
  tags          = { Name = "${var.name}-nat" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }
  tags = { Name = "${var.name}-public-rt" }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id
  dynamic "route" {
    for_each = aws_nat_gateway.this
    content {
      cidr_block     = "0.0.0.0/0"
      nat_gateway_id = route.value.id
    }
  }
  tags = { Name = "${var.name}-private-rt" }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
```

---

## modules/eks/main.tf (Key Resources)

```hcl
resource "aws_eks_cluster" "this" {
  name     = var.cluster_name
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = var.subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = true    # false for fully private cluster
    public_access_cidrs     = var.cluster_access_cidrs
    security_group_ids      = [aws_security_group.cluster.id]
  }

  access_config {
    authentication_mode = "API_AND_CONFIG_MAP"
  }

  encryption_config {
    resources = ["secrets"]
    provider {
      key_arn = aws_kms_key.eks.arn
    }
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster_AmazonEKSClusterPolicy
  ]
}

resource "aws_eks_node_group" "workers" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.cluster_name}-workers"
  node_role_arn   = aws_iam_role.node_group.arn
  subnet_ids      = var.subnet_ids
  instance_types  = var.instance_types

  scaling_config {
    desired_size = var.desired_size
    min_size     = var.min_size
    max_size     = var.max_size
  }

  update_config {
    max_unavailable = 1
  }

  labels = {
    role = "worker"
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_AmazonEKSWorkerNodePolicy,
    aws_iam_role_policy_attachment.node_AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.node_AmazonEC2ContainerRegistryReadOnly
  ]
}

# OIDC provider for IRSA
data "tls_certificate" "eks" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
}
```

---

## modules/rds/main.tf (Key Resources)

```hcl
resource "aws_db_subnet_group" "this" {
  name       = "${var.identifier}-subnet-group"
  subnet_ids = var.subnet_ids
  tags       = { Name = "${var.identifier}-subnet-group" }
}

resource "aws_security_group" "rds" {
  name   = "${var.identifier}-rds-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_node_security_group_id]
    description     = "PostgreSQL access from EKS nodes only"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "this" {
  identifier             = var.identifier
  engine                 = "postgres"
  engine_version         = var.engine_version
  instance_class         = var.instance_class
  allocated_storage      = var.allocated_storage
  max_allocated_storage  = var.max_allocated_storage
  storage_type           = "gp3"
  storage_encrypted      = true
  kms_key_id             = aws_kms_key.rds.arn

  db_name  = var.db_name
  username = var.master_username
  password = var.master_password    # sensitive variable; better: use random_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az               = var.multi_az
  publicly_accessible    = false
  deletion_protection    = var.enable_deletion_protection
  skip_final_snapshot    = !var.enable_deletion_protection
  final_snapshot_identifier = "${var.identifier}-final-snapshot"

  backup_retention_period = var.backup_retention_days
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  performance_insights_enabled = true

  lifecycle {
    prevent_destroy = true
    ignore_changes  = [password]   # prevent drift from external password rotation
  }
}
```

---

## main.tf (Root Module Composition)

```hcl
module "vpc" {
  source = "./modules/vpc"
  name               = local.name_prefix
  cidr_block         = local.vpc_cidr
  availability_zones = local.azs
  public_cidrs       = local.public_cidrs
  private_cidrs      = local.private_cidrs
  enable_nat_gateway = true
}

module "eks" {
  source             = "./modules/eks"
  cluster_name       = local.name_prefix
  kubernetes_version = var.kubernetes_version
  subnet_ids         = module.vpc.private_subnet_ids
  vpc_id             = module.vpc.vpc_id
  instance_types     = var.eks_instance_types
  desired_size       = var.eks_desired_size
  min_size           = var.eks_min_size
  max_size           = var.eks_max_size
}

module "rds" {
  source                       = "./modules/rds"
  identifier                   = "${local.name_prefix}-db"
  subnet_ids                   = module.vpc.private_subnet_ids
  vpc_id                       = module.vpc.vpc_id
  eks_node_security_group_id   = module.eks.node_security_group_id
  master_username              = var.db_master_username
  master_password              = var.db_master_password
  multi_az                     = true
  enable_deletion_protection   = true
}
```

---

## Interview Sound Bite

A full AWS stack in Terraform follows a layered composition pattern: VPC module creates the network foundation (VPC, subnets, IGW, NAT, route tables), EKS module creates the Kubernetes cluster (cluster role, node group role, OIDC provider for IRSA), and RDS module creates the database (subnet group, security group restricting 5432 access to EKS nodes only). Root module wires them together: `module.vpc.private_subnet_ids` goes into both `module.eks.subnet_ids` and `module.rds.subnet_ids`. Key production requirements: EKS secrets encryption with KMS, RDS storage encryption, multi-AZ RDS, `prevent_destroy = true` on RDS, `ignore_changes = [password]` to allow external rotation without drift.
