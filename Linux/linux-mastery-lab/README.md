# Linux Mastery Lab

A hands-on lab for beginner-to-pro Linux mastery.

This lab is built around safe diagnostics, production-style scenarios, interview practice, and runbooks. Run scripts on a Linux host, VM, container, or cloud instance where you have permission.

---

## Quick Start

Run a system snapshot:

```bash
bash SCRIPTS/01-system-snapshot.sh
```

Run process/resource triage:

```bash
bash SCRIPTS/02-process-resource-triage.sh
```

Run network triage:

```bash
bash SCRIPTS/03-network-service-triage.sh example.com https://example.com
```

Run disk and permission triage:

```bash
bash SCRIPTS/04-disk-permission-triage.sh /var/log
```

Run service log triage:

```bash
bash SCRIPTS/05-systemd-log-triage.sh ssh
```

Run advanced diagnostics readiness checks:

```bash
bash SCRIPTS/06-advanced-diagnostics-safe.sh ssh
```

---

## Lab Layout

```text
linux-mastery-lab/
  README.md
  LEARNING_PATH.md
  SCRIPTS/
    01-system-snapshot.sh
    02-process-resource-triage.sh
    03-network-service-triage.sh
    04-disk-permission-triage.sh
    05-systemd-log-triage.sh
    06-advanced-diagnostics-safe.sh
  LABS/
    01-system-snapshot.md
    02-service-debugging.md
    03-resource-saturation.md
    04-network-debugging.md
    05-permission-debugging.md
    06-container-host-debugging.md
    07-advanced-diagnostics.md
  PROJECTS/
    01-linux-health-snapshot-script.md
    02-service-debugging-playbook.md
    03-log-triage-pipeline.md
    04-network-diagnosis-playbook.md
    05-secure-linux-baseline.md
  CHEATSHEETS/
    COMMAND_MAP.md
    DEBUG_FLOW.md
    SAFETY_RULES.md
  INTERVIEW_PREP/
    QUESTIONS.md
    ANSWER_PATTERNS.md
  RUNBOOKS/
    SERVICE_DOWN.md
    HIGH_CPU_MEMORY.md
    DISK_FULL.md
    NETWORK_FAILURE.md
    PERMISSION_DENIED.md
    PATCHING_REBOOT.md
    FILE_DESCRIPTOR_LIMITS.md
```

---

## Practice Loop

```text
symptom -> command evidence -> OS layer -> likely cause -> safe mitigation -> validation -> prevention
```