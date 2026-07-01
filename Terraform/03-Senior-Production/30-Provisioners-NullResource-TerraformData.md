# 30. Provisioners, null_resource, and terraform_data

## What Are Provisioners?

Provisioners execute scripts or commands **after** a resource is created. They are Terraform's escape hatch for things providers don't support — bootstrapping, configuration, notifications.

```text
When to use:
  - Run a bootstrap script on a new EC2 instance (cloud-init is better, but provisioners work)
  - Invoke an external API or CLI after resource creation
  - Copy a file to a newly provisioned server

When NOT to use:
  - For configuration management (use Ansible, Chef, SSM, or cloud-init)
  - For anything that can be done with a provider resource
  - As your primary infra delivery mechanism

Terraform's official stance: provisioners are a "last resort."
If a provider resource exists for what you need, use it.
```

---

## local-exec Provisioner

Runs a command on the **local machine** (where Terraform is running — your laptop or CI runner).

```hcl
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = "t3.micro"

  provisioner "local-exec" {
    command = "echo 'Instance ${self.public_ip} is ready' >> inventory.txt"
  }

  provisioner "local-exec" {
    command = "aws ssm put-parameter --name /prod/instance-ip --value ${self.public_ip} --type String --overwrite"
  }
}
```

### When destroy

```hcl
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = "t3.micro"

  # Runs on terraform destroy (before resource is deleted)
  provisioner "local-exec" {
    when    = destroy
    command = "echo 'Removing ${self.id} from service registry'"
  }
}
```

### Environment variables and interpreter

```hcl
provisioner "local-exec" {
  command     = "python3 notify.py"
  working_dir = "${path.module}/scripts"
  interpreter = ["/usr/bin/python3", "-c"]
  environment = {
    INSTANCE_ID  = self.id
    REGION       = var.aws_region
    SLACK_HOOK   = var.slack_webhook_url
  }
}
```

---

## remote-exec Provisioner

Runs commands **on the remote resource** (via SSH or WinRM). Requires network access from the Terraform runner to the target.

```hcl
resource "aws_instance" "web" {
  ami           = data.aws_ami.app.id
  instance_type = "t3.micro"
  key_name      = aws_key_pair.deployer.key_name

  vpc_security_group_ids = [aws_security_group.ssh_access.id]
  subnet_id              = var.public_subnet_id

  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file("~/.ssh/deployer_key")
    host        = self.public_ip
    timeout     = "2m"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo yum update -y",
      "sudo yum install -y nginx",
      "sudo systemctl enable --now nginx",
    ]
  }
}
```

---

## file Provisioner

Copies a file or directory from the local machine to the remote resource.

```hcl
resource "aws_instance" "web" {
  # ...

  connection {
    type        = "ssh"
    user        = "ec2-user"
    private_key = file("~/.ssh/key")
    host        = self.public_ip
  }

  # Copy single file
  provisioner "file" {
    source      = "scripts/setup.sh"
    destination = "/tmp/setup.sh"
  }

  # Copy directory
  provisioner "file" {
    source      = "config/"
    destination = "/etc/app/"
  }

  # Inline content
  provisioner "file" {
    content     = templatefile("templates/nginx.conf.tftpl", { domain = var.domain })
    destination = "/etc/nginx/nginx.conf"
  }
}
```

---

## on_failure Behavior

```hcl
provisioner "local-exec" {
  command    = "some-flaky-command"
  on_failure = continue   # ignore errors (default: fail)
}

# Default: on_failure = fail
# If a provisioner fails, the resource is marked tainted (will be recreated on next apply)
# Use on_failure = continue only for non-critical side effects
```

---

## Provisioner Lifecycle Issues

```text
Provisioners only run at CREATE time (unless when = destroy).
If the provisioner script fails and the resource is tainted:
  → Next apply will DESTROY the resource and CREATE it again
  → The provisioner runs again on the new resource

Provisioners are NOT idempotent by design.
This is why cloud-init, SSM Run Command, or Ansible are better alternatives:
  - They can run on existing instances
  - They are idempotent (running twice has the same result as running once)
  - They don't require network access from the Terraform runner
```

---

## null_resource (Deprecated in TF 1.4+)

`null_resource` is a resource with no real infrastructure. It is used purely as a hook to run provisioners or to trigger re-runs.

```hcl
# Old pattern: null_resource to run a script when a file changes
resource "null_resource" "run_migrations" {
  triggers = {
    migration_hash = filemd5("${path.module}/migrations/v2.sql")
    cluster_arn    = aws_rds_cluster.main.arn
  }

  provisioner "local-exec" {
    command = "psql ${aws_rds_cluster.main.endpoint} -f migrations/v2.sql"
    environment = {
      PGPASSWORD = var.db_password
    }
  }
}
```

The `triggers` map forces re-run when any value changes. This is how you make provisioners re-run on subsequent applies.

---

## terraform_data (Terraform 1.4+ — Replaces null_resource)

`terraform_data` is the successor to `null_resource`. Built into Terraform core, no provider needed.

```hcl
# New pattern: terraform_data
resource "terraform_data" "run_migrations" {
  triggers_replace = [
    filemd5("${path.module}/migrations/v2.sql"),
    aws_rds_cluster.main.endpoint,
  ]

  provisioner "local-exec" {
    command = "psql ${aws_rds_cluster.main.endpoint} -f migrations/v2.sql"
  }
}

# terraform_data can also store values across applies
resource "terraform_data" "cluster_bootstrap" {
  input = {
    cluster_name = var.cluster_name
    region       = var.aws_region
  }

  lifecycle {
    replace_triggered_by = [aws_eks_cluster.main]
  }

  provisioner "local-exec" {
    command = "aws eks update-kubeconfig --name ${self.input.cluster_name} --region ${self.input.region}"
  }
}
```

---

## null_resource vs terraform_data

| Feature | null_resource | terraform_data |
|---|---|---|
| Available in | Terraform + `hashicorp/null` provider | TF 1.4+ core (no provider needed) |
| Trigger mechanism | `triggers` map | `triggers_replace` list |
| Store values | No | Yes (`input` attribute, accessible as `self.output`) |
| Status | Deprecated | Recommended |

```hcl
# Upgrade path: replace this
resource "null_resource" "example" {
  triggers = { hash = filemd5("script.sh") }
  provisioner "local-exec" { command = "bash script.sh" }
}

# With this
resource "terraform_data" "example" {
  triggers_replace = [filemd5("script.sh")]
  provisioner "local-exec" { command = "bash script.sh" }
}
```

---

## Common Production Patterns

### Pattern 1: Notify Slack After Deploy

```hcl
resource "terraform_data" "notify_deploy" {
  triggers_replace = [var.app_version]

  provisioner "local-exec" {
    command = <<-EOT
      curl -X POST -H 'Content-type: application/json' \
        --data '{"text":"Deployed ${var.app_name} v${var.app_version} to ${var.environment}"}' \
        "${var.slack_webhook}"
    EOT
  }
}
```

### Pattern 2: Update kubeconfig After EKS Creation

```hcl
resource "terraform_data" "update_kubeconfig" {
  triggers_replace = [aws_eks_cluster.main.endpoint]

  provisioner "local-exec" {
    command = "aws eks update-kubeconfig --name ${aws_eks_cluster.main.name} --region ${var.aws_region}"
  }

  depends_on = [aws_eks_cluster.main]
}
```

### Pattern 3: Run Database Migrations

```hcl
resource "terraform_data" "db_migrate" {
  # Runs when migration files change or DB endpoint changes
  triggers_replace = [
    filemd5("${path.module}/migrations.sql"),
    aws_db_instance.main.address,
  ]

  provisioner "local-exec" {
    command = "flyway -url=jdbc:postgresql://${aws_db_instance.main.address}/app migrate"
    environment = {
      FLYWAY_PASSWORD = var.db_password
    }
  }
}
```

---

## Better Alternatives To Provisioners

```text
Instead of remote-exec:
  → AWS: Use user_data (cloud-init) on EC2 launch
  → AWS: Use SSM Run Command (no network access from Terraform needed)
  → Kubernetes: Use init containers or Jobs

Instead of local-exec:
  → CI/CD: Run scripts as separate pipeline steps after terraform apply
  → AWS: Use EventBridge + Lambda triggered by CloudTrail on resource creation
  → For kubeconfig: Use the Kubernetes/Helm provider directly

Key principle:
  If it runs ONCE at creation time: provisioner is acceptable.
  If it needs to run multiple times or idempotently: use a proper config tool.
```

---

## Interview Sound Bite

Provisioners (`local-exec`, `remote-exec`, `file`) are Terraform's last resort for bootstrapping — they run commands at resource creation but are not idempotent, not re-runnable on existing resources, and fail silently by tainting the resource for recreation. `null_resource` (deprecated) and its successor `terraform_data` (TF 1.4+) provide a lifecycle hook without real infrastructure — useful for running scripts when input values change, driven by `triggers_replace`. In production, prefer cloud-init for EC2 bootstrapping, SSM Run Command for runtime commands, and keep Terraform focused on infrastructure provisioning rather than configuration management.
