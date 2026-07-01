# Linux Pro Gap Fill: Production Readiness Checklist

> Track File #30 of 30 - Group 06: Practice Upgrade
> For: final review | Level: pro | Mode: readiness checklist and rubric

## 1. Staff-Level Linux Review Questions

1. What service or workload runs on this host?
2. Which user runs each service?
3. What ports are listening and why?
4. What filesystems and mounts are required?
5. What happens if disk bytes or inodes fill?
6. What are the CPU/memory limits or capacity assumptions?
7. What logs prove health or failure?
8. What metrics and alerts exist?
9. What is the patching and reboot plan?
10. What is the backup and restore plan?
11. What secrets exist and how are they protected?
12. What sudo access exists and who owns it?
13. What firewall/security group rules are open?
14. What container cgroup limits or namespaces matter?
15. What runbook handles common failure modes?
16. What rollback exists for config changes?

---

## 2. Red Flags

- service runs as root without reason
- no logs or logs not retained
- no disk/inode alerts
- no rollback for config changes
- broad sudo access
- password SSH exposed to internet
- unpatched kernel/packages
- cron scripts with no logs
- destructive scripts with no dry-run
- no runbook for common incidents

---

## 3. Scoring Rubric

| Score | Meaning |
|---:|---|
| 0 | knows isolated commands only |
| 1 | can inspect files/processes but weak on interpretation |
| 2 | can debug common issues with guidance |
| 3 | can troubleshoot services, network, storage, permissions, and resources independently |
| 4 | can operate production Linux with runbooks, automation, security, incident response, and prevention |

---

## 4. Final Interview Answer

```text
I approach Linux production readiness by checking service ownership, permissions, ports, systemd units, logs, metrics, disk/inodes, CPU/memory, networking, patching, security, backups, automation, and runbooks. A Linux system is ready when failures are observable, mitigations are safe, and operations are repeatable.
```

---

## 5. Revision Notes

- One-line summary: Pro Linux means safe production operation, not just command recall.
- Three keywords: observable, secure, repeatable.
- One trap: passing an interview with command trivia but failing production because no runbook or rollback exists.