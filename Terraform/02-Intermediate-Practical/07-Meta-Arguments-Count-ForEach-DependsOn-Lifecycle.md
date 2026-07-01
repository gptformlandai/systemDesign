# 07. Meta-Arguments: count, for_each, depends_on, lifecycle

## Meta-Arguments Overview

Meta-arguments are special arguments accepted by every resource or module block. They control how Terraform manages the resource, independent of the provider.

```text
count          → create N copies of the resource
for_each       → create one resource per item in a map or set
depends_on     → explicit ordering when implicit dependency is missing
lifecycle      → control create/destroy behavior and change detection
provider       → assign a specific provider alias
```

---

## count

```hcl
resource "aws_instance" "web" {
  count         = 3
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.micro"
  
  tags = {
    Name = "web-server-${count.index}"  # 0, 1, 2
  }
}

# References with count use list index notation:
aws_instance.web[0].id
aws_instance.web[1].public_ip
aws_instance.web[*].id      # splat: all IDs as a list

# Conditional resource (count 0 or 1)
resource "aws_eip" "nat" {
  count  = var.enable_nat_gateway ? 1 : 0
  domain = "vpc"
}

# Reference a conditional resource (check length first)
output "nat_eip" {
  value = length(aws_eip.nat) > 0 ? aws_eip.nat[0].public_ip : null
}
```

### count Problem: Index-Based Identity

```text
count creates resources identified by INDEX, not by name.
If you remove element 0 from a list of 3:
  [a, b, c]  →  [b, c]
  
Terraform sees:
  index 0 changed:  a → b  (update)
  index 1 changed:  b → c  (update)
  index 2 deleted          (delete)

This causes unnecessary updates to surviving resources.
Use for_each when resources have meaningful names.
```

---

## for_each

```hcl
# for_each with a map — each.key and each.value are available
variable "subnets" {
  type = map(object({
    cidr = string
    az   = string
  }))
  default = {
    public-1  = { cidr = "10.0.1.0/24", az = "us-east-1a" }
    public-2  = { cidr = "10.0.2.0/24", az = "us-east-1b" }
    private-1 = { cidr = "10.0.11.0/24", az = "us-east-1a" }
  }
}

resource "aws_subnet" "this" {
  for_each = var.subnets

  vpc_id            = aws_vpc.main.id
  cidr_block        = each.value.cidr
  availability_zone = each.value.az
  
  tags = {
    Name = each.key  # "public-1", "public-2", "private-1"
  }
}

# Reference: map key notation
aws_subnet.this["public-1"].id
aws_subnet.this["private-1"].id

# All IDs as a map
{ for k, v in aws_subnet.this : k => v.id }

# for_each with a set of strings
resource "aws_iam_user" "team" {
  for_each = toset(["alice", "bob", "carol"])
  name     = each.value
}

aws_iam_user.team["alice"].arn
```

### for_each vs count

| Situation | Use |
|---|---|
| Fixed number of identical resources | `count` |
| Resources with distinct names/config | `for_each` |
| Conditional resource (0 or 1) | `count = var.enable ? 1 : 0` |
| Set of strings → one resource each | `for_each = toset(...)` |
| Map → one resource per entry | `for_each = var.config_map` |

```text
Why for_each is better for named resources:
  If you have for_each = { "alice", "bob", "carol" }
  and remove "alice", only aws_iam_user.team["alice"] is destroyed.
  "bob" and "carol" are untouched.

  With count: removing index 0 shifts all indices → needless updates.
```

---

## depends_on

```hcl
# Terraform usually builds an implicit dependency graph from references.
# Use depends_on only when the dependency is not expressed through references.

resource "aws_iam_role" "ec2_role" {
  name = "ec2-role"
  # ...
}

resource "aws_iam_role_policy_attachment" "ec2_policy" {
  role       = aws_iam_role.ec2_role.name  # ← implicit dependency created here
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ReadOnlyAccess"
}

# depends_on needed when the relationship is side-effectful, not a reference:
# e.g., an S3 bucket policy must exist before EC2 accesses the bucket,
# but the EC2 resource doesn't reference the policy directly.
resource "aws_instance" "app" {
  ami           = "ami-0c55b159cbfafe1f0"
  instance_type = "t3.micro"
  
  depends_on = [
    aws_iam_role_policy_attachment.ec2_policy   # wait for policy before instance
  ]
}
```

---

## lifecycle Block

```hcl
resource "aws_db_instance" "main" {
  identifier        = "prod-db"
  engine            = "postgres"
  instance_class    = "db.t3.medium"
  allocated_storage = 100

  lifecycle {
    # 1. Create the replacement before destroying the old resource
    # Useful for zero-downtime replacements (ALB target groups, etc.)
    create_before_destroy = true

    # 2. Block terraform destroy for this resource
    # Raises an error if destroy is attempted — protects critical resources
    prevent_destroy = true

    # 3. Ignore out-of-band changes to specific attributes
    # Terraform won't treat these changes as drift needing correction
    ignore_changes = [
      tags["LastModified"],   # ignore a specific tag
      engine_version,          # ignore automated minor version upgrades
    ]

    # 4. Replace the resource when another resource changes (Terraform 1.2+)
    replace_triggered_by = [
      aws_launch_template.app.id   # replace instances when launch template changes
    ]
  }
}
```

### create_before_destroy In Detail

```text
Default behavior:
  Terraform destroys the old resource THEN creates the new one.
  → Downtime window between destroy and create.

create_before_destroy = true:
  Terraform creates the new resource FIRST, then destroys the old one.
  → Zero downtime, but both versions coexist briefly.
  → Requires the resource to support having two versions at once.
  
Common use cases:
  - aws_launch_template (create new template version, swap ASG, delete old)
  - aws_acm_certificate (provision new cert, update listener, delete old)
  - aws_alb_target_group (new target group, update listener rule, delete old)
  
Watch out: create_before_destroy propagates!
  Any resource that depends on a create_before_destroy resource
  must also have create_before_destroy = true (or will error).
```

### prevent_destroy Guard

```hcl
resource "aws_s3_bucket" "critical_data" {
  bucket = "company-critical-prod-data"

  lifecycle {
    prevent_destroy = true  # terraform destroy raises error: "prevent_destroy is set"
  }
}

# To destroy: first remove prevent_destroy from HCL, then apply, then destroy.
```

---

## provider Meta-Argument

```hcl
# Use a non-default provider alias for a specific resource
resource "aws_s3_bucket" "eu_archive" {
  provider = aws.eu_west   # use the eu-west-1 aliased provider
  bucket   = "eu-archive-bucket"
}
```

---

## Interview Sound Bite

`count` creates N copies of a resource identified by integer index; `for_each` creates one resource per key in a map or set, identified by the key. For_each is almost always better for named resources because removing an item only destroys that key — count-based removals shift indices and cause unnecessary updates. `depends_on` creates explicit ordering when the dependency isn't a reference in HCL (e.g., a policy must exist before an EC2 can access a bucket, but the EC2 block doesn't reference the policy). `lifecycle { create_before_destroy = true }` ensures zero-downtime replacements by creating the new resource before deleting the old one. `prevent_destroy = true` is a safety net for stateful resources like RDS databases and S3 buckets containing production data.
