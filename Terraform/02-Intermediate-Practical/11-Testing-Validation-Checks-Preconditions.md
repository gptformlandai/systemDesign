# 11. Testing, Validation, Checks, Preconditions

## Validation Hierarchy

```text
Level 1: terraform validate
  → Syntax and type checking, no API calls
  
Level 2: Variable validation blocks
  → Check inputs before any resource is created
  
Level 3: Preconditions / postconditions (TF 1.2+)
  → Check resource state before/after creation
  
Level 4: check blocks (TF 1.5+)
  → Ongoing assertions that report but don't fail apply
  
Level 5: terraform test framework (TF 1.6+)
  → Write test files (.tftest.hcl), run with real or mock providers
  
Level 6: Terratest (Go-based integration tests)
  → Full infrastructure lifecycle: plan → apply → validate → destroy
```

---

## terraform validate

```bash
# Check HCL syntax, type compatibility, and internal references
terraform validate

# Output (success):
# Success! The configuration is valid.

# Output (failure):
# Error: Unsupported argument
#   on main.tf line 12, in resource "aws_instance" "web":
#   12:   instance_typo = "t3.micro"

# JSON output for tooling
terraform validate -json
```

---

## Variable Validation Blocks

```hcl
variable "environment" {
  type        = string
  description = "Deployment environment"
  
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "instance_type" {
  type = string
  
  validation {
    condition     = can(regex("^t[23]\\.", var.instance_type))
    error_message = "instance_type must start with t2. or t3."
  }
}

variable "vpc_cidr" {
  type = string
  
  validation {
    condition     = can(cidrnetmask(var.vpc_cidr))
    error_message = "vpc_cidr must be a valid CIDR block like 10.0.0.0/16."
  }
}

variable "port" {
  type = number
  
  validation {
    condition     = var.port >= 1 && var.port <= 65535
    error_message = "port must be between 1 and 65535."
  }
}
```

---

## Preconditions (Terraform 1.2+)

Preconditions run before a resource is created/updated/read. If the condition fails, the plan/apply errors immediately.

```hcl
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = var.instance_type
  
  lifecycle {
    precondition {
      condition     = data.aws_ami.app.architecture == "x86_64"
      error_message = "The selected AMI must be x86_64 architecture."
    }
    
    precondition {
      condition     = var.environment != "prod" || var.instance_type != "t3.micro"
      error_message = "Production environment must not use t3.micro — too small."
    }
  }
}
```

Preconditions on data sources:

```hcl
data "aws_ami" "app" {
  most_recent = true
  owners      = ["self"]
  
  filter {
    name   = "tag:Environment"
    values = [var.environment]
  }
  
  lifecycle {
    postcondition {
      condition     = self.state == "available"
      error_message = "The AMI is not in available state."
    }
  }
}
```

---

## Postconditions (Terraform 1.2+)

Postconditions run after a resource is created/updated. They verify the resulting state meets expectations.

```hcl
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = var.instance_type
  subnet_id     = var.subnet_id
  
  lifecycle {
    postcondition {
      condition     = self.public_ip != ""
      error_message = "EC2 instance did not receive a public IP. Check subnet config."
    }
  }
}

resource "aws_acm_certificate" "api" {
  domain_name       = "api.${var.domain}"
  validation_method = "DNS"
  
  lifecycle {
    postcondition {
      condition     = self.status == "ISSUED" || self.status == "PENDING_VALIDATION"
      error_message = "ACM certificate is in unexpected status: ${self.status}"
    }
  }
}
```

---

## check Blocks (Terraform 1.5+)

Check blocks define ongoing assertions. Unlike preconditions, a failing check does NOT fail the apply — it posts a warning. Good for health-check assertions.

```hcl
check "website_health" {
  data "http" "api_health" {
    url = "https://api.${var.domain}/health"
  }
  
  assert {
    condition     = data.http.api_health.status_code == 200
    error_message = "API health check returned status ${data.http.api_health.status_code}"
  }
}

check "rds_available" {
  assert {
    condition     = aws_db_instance.main.status == "available"
    error_message = "RDS instance is not available: ${aws_db_instance.main.status}"
  }
}
```

---

## terraform test Framework (TF 1.6+)

Create `.tftest.hcl` files to define test runs.

```text
tests/
  unit.tftest.hcl
  integration.tftest.hcl
```

```hcl
# tests/unit.tftest.hcl
variables {
  environment    = "test"
  instance_type  = "t3.micro"
  name_prefix    = "test"
}

run "verify_naming" {
  command = plan    # plan only, no apply

  assert {
    condition     = aws_instance.web.tags["Environment"] == "test"
    error_message = "Environment tag must be set to 'test'"
  }

  assert {
    condition     = startswith(aws_instance.web.tags["Name"], "test-")
    error_message = "Name tag must start with name_prefix"
  }
}

run "verify_instance_type" {
  command = plan

  assert {
    condition     = aws_instance.web.instance_type == "t3.micro"
    error_message = "Dev environment should use t3.micro"
  }
}
```

```hcl
# tests/integration.tftest.hcl (runs apply → asserts → auto-destroys)
run "full_deploy" {
  command = apply   # real infrastructure created + destroyed after test

  assert {
    condition     = aws_instance.web.public_ip != ""
    error_message = "Web instance must have a public IP"
  }
  
  assert {
    condition     = aws_vpc.main.enable_dns_support == true
    error_message = "VPC must have DNS support enabled"
  }
}
```

```bash
# Run all tests
terraform test

# Run specific test file
terraform test -filter=tests/unit.tftest.hcl

# Verbose output
terraform test -verbose
```

### Mock Providers (TF 1.7+)

```hcl
# tests/mocked.tftest.hcl
mock_provider "aws" {
  mock_resource "aws_instance" {
    defaults = {
      id        = "i-mock1234"
      public_ip = "192.168.1.1"
    }
  }
}

run "test_with_mock" {
  command = apply   # applies against mock provider (no real AWS calls)

  assert {
    condition     = aws_instance.web.id == "i-mock1234"
    error_message = "Expected mock instance ID"
  }
}
```

---

## Terratest (Go-Based Integration Testing)

```go
// test/vpc_test.go
package test

import (
    "testing"
    "github.com/gruntwork-io/terratest/modules/terraform"
    "github.com/stretchr/testify/assert"
)

func TestVpcCreation(t *testing.T) {
    opts := &terraform.Options{
        TerraformDir: "../examples/vpc",
        Vars: map[string]interface{}{
            "environment": "test",
        },
    }
    
    defer terraform.Destroy(t, opts)        // always clean up
    terraform.InitAndApply(t, opts)         // apply
    
    vpcId := terraform.Output(t, opts, "vpc_id")
    assert.NotEmpty(t, vpcId)
    
    subnetIds := terraform.OutputList(t, opts, "private_subnet_ids")
    assert.Equal(t, 3, len(subnetIds))
}
```

---

## Interview Sound Bite

Terraform has a five-layer validation stack: `terraform validate` checks syntax/types with no API calls; variable validation blocks check inputs before resource creation; preconditions check assumptions about data sources or resource attributes before creating the resource; postconditions verify the created resource meets expectations; check blocks run ongoing assertions that warn without blocking apply. The `terraform test` framework (TF 1.6+) uses `.tftest.hcl` files with `run` blocks that can run `plan` (fast, no infrastructure) or `apply` (real resources, auto-destroyed). TF 1.7+ adds mock providers to test without any cloud API calls. Terratest is the Go-based alternative for full end-to-end integration testing.
