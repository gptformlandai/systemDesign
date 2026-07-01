# Linux Automation: Shell Scripting, Cron, systemd Timers, and Ansible - Gold Sheet

> Track File #16 of 30 - Group 03: Senior Production
> For: production automation | Level: senior | Mode: safe repeatability

## 1. Core Idea

Automation turns manual Linux operations into repeatable, reviewable, safer workflows.

```text
manual command -> script/runbook -> idempotent automation -> monitored scheduled job
```

---

## 2. Automation Options

| Tool | Use |
|---|---|
| shell script | local workflow, diagnostics, glue logic |
| cron | simple scheduled command |
| systemd timer | scheduled job with systemd logs/dependencies |
| Ansible | remote configuration and orchestration |
| cloud-init | boot-time instance initialization |
| CI/CD job | controlled deployment/maintenance automation |

---

## 3. Safe Shell Script Practices

```bash
#!/usr/bin/env bash
set -euo pipefail

target_dir=${1:?usage: script TARGET_DIR}

if [[ ! -d "$target_dir" ]]; then
  echo "missing directory: $target_dir" >&2
  exit 1
fi

du -sh "$target_dir"
```

Rules:

- quote variables
- validate inputs
- log actions
- avoid destructive defaults
- support dry-run when possible
- make scripts idempotent
- use absolute paths in cron/systemd jobs

---

## 4. Cron vs systemd Timer

| Cron | systemd Timer |
|---|---|
| simple and widely known | stronger logs and dependency model |
| limited environment | integrates with journal/systemctl |
| easy for user jobs | better for production service jobs |

---

## 5. Production Failure Modes

- script works manually but fails under cron PATH
- unquoted variable expands dangerously
- job overlaps with previous run
- no logs or alerting on failure
- script deletes wrong directory
- automation changes too many hosts at once
- no rollback for config change

---

## 6. Interview Summary

```text
For Linux automation, I make commands repeatable, idempotent, logged, monitored, and safe by validating inputs, quoting variables, avoiding destructive defaults, using dry-run when possible, and choosing cron, systemd timers, Ansible, or CI/CD based on scope and operational needs.
```

---

## 7. Revision Notes

- One-line summary: Production automation must be repeatable, safe, and observable.
- Three keywords: idempotent, scheduled, monitored.
- One trap: running a destructive script across hosts without dry-run, scope limit, or rollback.