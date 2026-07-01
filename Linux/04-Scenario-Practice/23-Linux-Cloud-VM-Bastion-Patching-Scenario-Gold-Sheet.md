# Linux Cloud VM, Bastion, and Patching Scenario - Gold Sheet

> Track File #23 of 30 - Group 04: Scenario Practice
> For: cloud operations interviews | Level: intermediate to senior | Mode: VM lifecycle

## 1. Scenario

```text
You own a Linux VM fleet behind a bastion and need to patch safely.
```

Cloud Linux work combines OS skill with network, identity, maintenance windows, and rollback planning.

---

## 2. Patching Flow

```text
inventory -> risk classify -> backup/snapshot -> patch staging -> patch canary -> validate -> roll fleet -> reboot if needed -> monitor
```

Commands:

```bash
uname -a
uptime
apt list --upgradable 2>/dev/null || true
dnf check-update || true
systemctl --failed
last reboot
```

---

## 3. Bastion Checks

- SSH key/cert access
- least-privilege groups
- audit logging
- source IP restrictions
- no direct public SSH to private workloads when avoidable
- session recording if required

---

## 4. Production Risks

- kernel update requires reboot
- patch breaks dependency or config
- reboot fails due to bad fstab/service startup
- security group blocks access after change
- no snapshot or rollback
- patching all hosts at once causes outage

---

## 5. Interview Summary

```text
For Linux cloud VM patching, I inventory hosts, patch staging first, use canaries, take backups or snapshots, plan reboots, validate services after patching, and roll gradually. Bastion access should be audited, least-privilege, and restricted by network policy.
```

---

## 6. Revision Notes

- One-line summary: Production patching is staged change management, not one command.
- Three keywords: canary, reboot, rollback.
- One trap: applying kernel updates without reboot planning and service validation.