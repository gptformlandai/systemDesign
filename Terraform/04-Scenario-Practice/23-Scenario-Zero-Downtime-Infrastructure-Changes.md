# 23. Scenario: Zero-Downtime Infrastructure Changes

## The Core Challenge

```text
Some Terraform changes require destroy → create (replacement).
If a resource is in the critical path (load balancer, database, compute),
destroying it first causes downtime.

Resources that commonly need replacement:
  - EC2 instances (AMI change, instance type change in some cases)
  - Launch templates / Launch configurations
  - RDS parameter groups
  - ACM certificates
  - Security group rules (in some configurations)
  - ALB target groups (when attributes that force replacement change)
```

---

## Pattern 1: create_before_destroy

Tell Terraform to provision the replacement FIRST, then tear down the old resource.

```hcl
resource "aws_launch_template" "app" {
  name_prefix   = "app-lt-"   # name_prefix = unique names on each replace
  image_id      = var.ami_id
  instance_type = var.instance_type

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "app" {
  name                = "${aws_launch_template.app.name}-asg"
  desired_capacity    = 3
  min_size            = 2
  max_size            = 10
  vpc_zone_identifier = var.private_subnet_ids

  launch_template {
    id      = aws_launch_template.app.id
    version = "$Latest"
  }

  lifecycle {
    create_before_destroy = true
  }
}
```

```text
How it works:
  Old: launch_template v1, ASG pointing to v1
  Change: new AMI
  Terraform plan: -/+ replace aws_launch_template.app
  
  With create_before_destroy:
    1. Create new launch template v2
    2. Update ASG to point to v2 (rolling update)
    3. Terminate old launch template v1
    Result: ASG never points to a deleted launch template
```

---

## Pattern 2: Blue/Green With ALB Target Groups

```hcl
variable "active_color" {
  type    = string
  default = "blue"    # toggle to "green" for next deploy
}

# Blue target group
resource "aws_lb_target_group" "blue" {
  name     = "${var.app_name}-blue"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc_id

  health_check {
    path                = "/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 10
  }
}

# Green target group
resource "aws_lb_target_group" "green" {
  name     = "${var.app_name}-green"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = var.vpc_id

  health_check {
    path                = "/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 10
  }
}

# ALB listener: points to active color
resource "aws_lb_listener_rule" "app" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = var.active_color == "blue" ? aws_lb_target_group.blue.arn : aws_lb_target_group.green.arn
  }

  condition {
    path_pattern {
      values = ["/api/*"]
    }
  }
}
```

```text
Blue/Green deploy process:
  1. Active = blue; blue ASG running v1
  2. Deploy v2 to green ASG; green TG health checks pass
  3. Change var.active_color = "green"
  4. terraform apply → ALB now sends traffic to green (v2)
  5. Monitor; if healthy, drain blue
  6. Next deploy: green is active, deploy to blue

Zero downtime because:
  - Traffic shifted atomically at the ALB listener
  - New version health-checked before receiving production traffic
  - Instant rollback: change active_color back to "blue" + apply
```

---

## Pattern 3: Rolling Update With replace_triggered_by

```hcl
resource "aws_launch_template" "app" {
  name_prefix   = "app-"
  image_id      = var.ami_id
  instance_type = var.instance_type
  user_data     = base64encode(templatefile("scripts/init.sh", { env = var.environment }))

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_autoscaling_group" "app" {
  name_prefix         = "app-asg-"
  desired_capacity    = var.asg_desired
  min_size            = var.asg_min
  max_size            = var.asg_max
  vpc_zone_identifier = var.private_subnet_ids
  target_group_arns   = [aws_lb_target_group.app.arn]

  launch_template {
    id      = aws_launch_template.app.id
    version = aws_launch_template.app.latest_version
  }

  instance_refresh {
    strategy = "Rolling"
    preferences {
      min_healthy_percentage = 90    # keep 90% healthy during rollout
      instance_warmup        = 60    # wait 60s before declaring instance healthy
    }
  }

  lifecycle {
    create_before_destroy = true

    # Replace ASG (triggering instance refresh) when launch template changes
    replace_triggered_by = [
      aws_launch_template.app.latest_version
    ]
  }
}
```

---

## Pattern 4: RDS Zero-Downtime Parameter Changes

```hcl
resource "aws_db_parameter_group" "postgres15" {
  name   = "${var.identifier}-params-v2"   # new name for new group
  family = "postgres15"

  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_db_instance" "main" {
  identifier       = var.identifier
  parameter_group_name = aws_db_parameter_group.postgres15.name

  apply_immediately = false    # false = apply during maintenance window (no immediate downtime)

  lifecycle {
    create_before_destroy = true
    prevent_destroy       = true
    ignore_changes        = [password]
  }
}
```

---

## Pattern 5: ACM Certificate Rotation

```hcl
resource "aws_acm_certificate" "api" {
  domain_name               = "api.${var.domain}"
  subject_alternative_names = ["*.api.${var.domain}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true   # new cert before old is deleted
  }
}

resource "aws_acm_certificate_validation" "api" {
  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.app.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.api.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
```

---

## name vs name_prefix

```text
A key zero-downtime technique: use name_prefix instead of name.

name = "my-lt"          → fails if the resource already exists
name_prefix = "my-lt-"  → AWS generates "my-lt-abc123" — unique every time

This is critical for create_before_destroy:
  - Old resource: my-lt-abc123
  - New resource: my-lt-def456
  Both can exist simultaneously until Terraform deletes the old one.

Resources that support name_prefix:
  aws_launch_template, aws_iam_role, aws_iam_policy,
  aws_security_group, aws_db_parameter_group, aws_alb_target_group
```

---

## Dangerous Changes (Require Careful Planning)

```text
Changes that FORCE replacement (destroy + create):
  - aws_instance: ami, availability_zone, subnet_id
  - aws_db_instance: engine, identifier, allocated_storage reduction
  - aws_eks_cluster: name, version downgrade

Mitigation:
  1. create_before_destroy in lifecycle block
  2. Blue/green pattern
  3. Schedule during low-traffic window
  4. Test in staging first
  5. Plan carefully: read the force-replacement note in plan output

Look for this in plan output:
  # aws_db_instance.main must be replaced
  -/+ resource "aws_db_instance" "main" {
       # (forces replacement)
```

---

## Interview Sound Bite

Zero-downtime Terraform changes rely on three patterns: `create_before_destroy = true` in the lifecycle block tells Terraform to create the replacement resource before destroying the old one; `name_prefix` instead of `name` lets both old and new resources coexist with unique names during replacement; blue/green with ALB target groups allows traffic-shifting at the load balancer atomically while both versions are running. The `instance_refresh` block on ASGs triggers rolling instance replacement when the launch template version changes. For RDS, parameter group changes should use `apply_immediately = false` to apply during the maintenance window. Always read the plan output for `-/+` (force replacement) symbols before applying.
