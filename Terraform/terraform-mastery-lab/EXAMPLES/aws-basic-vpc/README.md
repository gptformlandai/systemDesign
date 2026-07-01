# Example: AWS Basic VPC

A minimal working Terraform configuration that creates a VPC with public and private subnets.

---

## What This Creates

- 1 VPC (`10.0.0.0/16`)
- 2 public subnets (different AZs)
- 2 private subnets (different AZs)
- 1 Internet Gateway
- 1 NAT Gateway (in first public subnet)
- Route tables + associations

---

## Usage

```bash
terraform init
terraform plan
terraform apply
terraform destroy  # when done (incurs cost while running)
```

---

## Files

```
aws-basic-vpc/
├── main.tf          ← resources
├── variables.tf     ← inputs
├── outputs.tf       ← outputs
└── versions.tf      ← provider + version constraints
```
