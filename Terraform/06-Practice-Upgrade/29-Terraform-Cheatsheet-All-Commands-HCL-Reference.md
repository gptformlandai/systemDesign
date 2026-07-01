# 29. Terraform Cheatsheet: All Commands + HCL Reference

---

## CLI Commands Quick Reference

### Initialization

```bash
terraform init                          # download providers, init backend, install modules
terraform init -upgrade                 # upgrade providers to latest matching constraint
terraform init -migrate-state          # migrate state to new backend
terraform init -reconfigure            # force backend reconfiguration without migration
terraform init -backend=false          # skip backend initialization
terraform init -backend-config=file.hcl # partial backend config from file
```

### Plan

```bash
terraform plan                         # compute diff; read-only
terraform plan -out=tfplan             # save plan to file
terraform plan -destroy                # plan a destroy operation
terraform plan -target=aws_vpc.main    # plan only specified resource
terraform plan -var="env=prod"         # pass variable
terraform plan -var-file=prod.tfvars   # pass variable file
terraform plan -refresh=false          # skip state refresh (faster, less accurate)
terraform plan -refresh=true           # full refresh (default)
terraform plan -refresh-only           # only update state from actual APIs
terraform plan -replace=aws_instance.web  # force replacement of resource
terraform plan -compact-warnings       # compact warning output
terraform plan -no-color               # no ANSI color codes (for CI logs)
terraform plan -detailed-exitcode      # exit code 2 if changes present
terraform plan -generate-config-out=gen.tf  # generate HCL for import blocks (TF 1.5+)
```

### Apply

```bash
terraform apply                        # interactive apply (shows plan, confirms)
terraform apply tfplan                 # apply a saved plan file
terraform apply -auto-approve          # skip confirmation
terraform apply -target=aws_vpc.main   # partial apply
terraform apply -var="env=prod"        # pass variable
terraform apply -refresh-only          # accept drift into state (no resource changes)
terraform apply -replace=aws_instance.web  # force replace specific resource
```

### Destroy

```bash
terraform destroy                      # destroy all managed resources
terraform destroy -auto-approve        # skip confirmation
terraform destroy -target=aws_instance.web  # destroy specific resource
```

### State Commands

```bash
terraform state list                   # list all resources in state
terraform state show aws_vpc.main      # show resource attributes from state
terraform state mv <source> <dest>     # rename resource address in state
terraform state rm <address>           # remove resource from state (no destroy)
terraform state pull                   # download state from remote backend to stdout
terraform state push <file>            # upload local state to remote backend (DANGEROUS)
terraform state replace-provider <old> <new>  # replace provider source in state
```

### Import

```bash
terraform import <address> <id>        # import existing resource into state
# e.g.:
terraform import aws_instance.web i-0abc123def456789
terraform import 'aws_subnet.this["public-1"]' subnet-0abc1111
terraform import module.vpc.aws_vpc.this vpc-0abc1234
```

### Utilities

```bash
terraform validate                     # syntax + type check (no API calls)
terraform validate -json               # JSON output for tooling
terraform fmt                          # format .tf files
terraform fmt -recursive               # format recursively
terraform fmt -check                   # check only; exit 1 if formatting needed
terraform fmt -diff                    # show diff
terraform show                         # show current state (human-readable)
terraform show tfplan                  # show saved plan
terraform show -json tfplan            # show plan as JSON
terraform output                       # show all outputs
terraform output vpc_id                # show specific output
terraform output -json                 # all outputs as JSON
terraform output -raw vpc_id           # raw value (no quotes)
terraform console                      # interactive HCL expression REPL
terraform graph                        # output dependency graph in DOT format
terraform graph | dot -Tsvg > graph.svg
terraform version                      # show Terraform version
terraform providers                    # show providers used in config
terraform providers lock -platform=linux_amd64  # update lock file for specific platform
terraform force-unlock <LOCK_ID>       # release a stale state lock
```

### Workspace Commands

```bash
terraform workspace list               # list all workspaces
terraform workspace new staging        # create workspace
terraform workspace select prod        # switch workspace
terraform workspace show               # show current workspace
terraform workspace delete old-feature # delete workspace
```

### Test Commands (TF 1.6+)

```bash
terraform test                         # run all .tftest.hcl files
terraform test -filter=tests/unit.tftest.hcl  # run specific test file
terraform test -verbose                # verbose output
```

---

## HCL Block Reference

### resource

```hcl
resource "<TYPE>" "<NAME>" {
  # provider arguments
  
  # Meta-arguments (available on all resources):
  count       = <number>
  for_each    = <map or set>
  depends_on  = [<resource_references>]
  provider    = <provider_alias>
  
  lifecycle {
    create_before_destroy = bool
    prevent_destroy       = bool
    ignore_changes        = [attr1, attr2]
    replace_triggered_by  = [resource_ref]
  }
}
```

### data

```hcl
data "<TYPE>" "<NAME>" {
  # filter arguments
}
# Reference: data.<TYPE>.<NAME>.<ATTRIBUTE>
```

### variable

```hcl
variable "<NAME>" {
  type        = <type>
  description = "string"
  default     = <value>
  sensitive   = bool
  
  validation {
    condition     = <expression returning bool>
    error_message = "string"
  }
}
# Reference: var.<NAME>
```

### output

```hcl
output "<NAME>" {
  value       = <expression>
  description = "string"
  sensitive   = bool
  depends_on  = [<resource_references>]
}
```

### locals

```hcl
locals {
  <NAME> = <expression>
}
# Reference: local.<NAME>
```

### module

```hcl
module "<NAME>" {
  source  = "<source_string>"
  version = "<constraint>"    # for registry modules only
  
  # Input variables
  <var_name> = <value>
  
  # Meta-arguments:
  count      = <number>
  for_each   = <map or set>
  depends_on = [<references>]
  providers  = { <provider_type> = <provider_config> }
}
# Reference: module.<NAME>.<output_name>
```

### terraform

```hcl
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    <name> = {
      source  = "<hostname>/<namespace>/<type>"
      version = "<constraint>"
      configuration_aliases = [<alias_references>]  # for module provider aliases
    }
  }
  
  backend "<TYPE>" {
    # backend-specific arguments
  }
  
  # OR for TFC:
  cloud {
    organization = "org-name"
    workspaces {
      name = "workspace-name"
      # OR:
      tags = ["tag1"]
    }
  }
}
```

### moved

```hcl
moved {
  from = <old_address>
  to   = <new_address>
}
```

### import (TF 1.5+)

```hcl
import {
  to = <resource_address>
  id = "<real_resource_id>"
}
```

### check (TF 1.5+)

```hcl
check "<NAME>" {
  data "<type>" "<name>" {
    # optional data source for check
  }
  assert {
    condition     = <bool expression>
    error_message = "string"
  }
}
```

---

## Version Constraint Operators

| Operator | Meaning | Example |
|---|---|---|
| `= 1.5.0` | Exact version only | `= 5.1.0` |
| `!= 1.5.0` | Exclude version | `!= 5.0.0` |
| `>= 1.5.0` | Minimum version | `>= 5.0` |
| `<= 1.5.0` | Maximum version | `<= 5.9` |
| `~> 5.0` | `>= 5.0, < 6.0` | Minor updates only |
| `~> 5.1.0` | `>= 5.1.0, < 5.2.0` | Patch updates only |

---

## Variable Types Reference

```hcl
type = string
type = number
type = bool
type = list(string)
type = set(string)
type = map(string)
type = map(number)
type = any
type = object({
  name    = string
  count   = number
  enabled = optional(bool, true)  # TF 1.3+
})
type = tuple([string, number, bool])
```

---

## Common Functions Quick Reference

| Function | What it does |
|---|---|
| `merge(m1, m2)` | Merge maps |
| `concat(l1, l2)` | Concat lists |
| `flatten([l1, l2])` | Flatten nested lists |
| `toset(list)` | Convert list to set (unique, unordered) |
| `tolist(set)` | Convert set to sorted list |
| `tomap({...})` | Convert to map |
| `keys(map)` | Map keys as list |
| `values(map)` | Map values as list |
| `length(x)` | Length of string/list/map |
| `contains(list, val)` | Check if list contains value |
| `element(list, idx)` | Get list element (wraps around) |
| `index(list, val)` | Get index of value |
| `lookup(map, key, default)` | Map lookup with default |
| `zipmap(keys, values)` | Create map from keys and values lists |
| `try(expr, fallback)` | Return first non-error value |
| `can(expr)` | Return bool: does expr evaluate? |
| `coalesce(v1, v2, ...)` | Return first non-null, non-empty value |
| `cidrsubnet(cidr, bits, num)` | Compute subnet CIDR |
| `cidrhost(cidr, hostnum)` | Compute host IP in subnet |
| `format(fmt, args...)` | String formatting |
| `formatdate(fmt, time)` | Format timestamp |
| `base64encode(str)` | Base64 encode |
| `base64decode(str)` | Base64 decode |
| `jsonencode(val)` | Encode value as JSON string |
| `jsondecode(str)` | Decode JSON string to value |
| `file(path)` | Read file contents |
| `templatefile(path, vars)` | Render template with variables |
| `upper/lower(str)` | String case |
| `trimprefix/trimsuffix(str, pre)` | Remove prefix/suffix |
| `split(sep, str)` | Split string to list |
| `join(sep, list)` | Join list to string |

---

## Environment Variables Reference

| Variable | Purpose |
|---|---|
| `TF_VAR_<name>` | Set input variable value |
| `TF_LOG` | Log level: TRACE, DEBUG, INFO, WARN, ERROR |
| `TF_LOG_PATH` | Write logs to file |
| `TF_INPUT=false` | Disable interactive prompts |
| `TF_CLI_ARGS` | Default CLI arguments for all commands |
| `TF_CLI_ARGS_plan` | Default args for plan specifically |
| `TF_DATA_DIR` | Override .terraform/ directory location |
| `TF_WORKSPACE` | Default workspace to select |
| `TF_TOKEN_<hostname>` | Terraform Cloud/Enterprise API token |
| `CHECKPOINT_DISABLE=1` | Disable HashiCorp telemetry |
| `AWS_ACCESS_KEY_ID` | AWS credentials (prefer OIDC) |
| `AWS_SECRET_ACCESS_KEY` | AWS credentials (prefer OIDC) |
| `AWS_PROFILE` | AWS CLI profile to use |
| `AWS_REGION` | Default AWS region |
