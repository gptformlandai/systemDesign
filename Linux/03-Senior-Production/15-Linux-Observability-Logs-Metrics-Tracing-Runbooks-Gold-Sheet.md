# Linux Observability: Logs, Metrics, Tracing Mindset, and Runbooks - Gold Sheet

> Track File #15 of 30 - Group 03: Senior Production
> For: on-call and SRE practice | Level: senior | Mode: evidence gathering

## 1. Core Idea

Linux observability turns symptoms into evidence.

```text
logs + metrics + process state + kernel signals + service state + recent changes = incident picture
```

---

## 2. Evidence Sources

| Source | What It Shows |
|---|---|
| `journalctl` | systemd and service logs |
| `/var/log` | distro/app logs |
| `dmesg` | kernel ring buffer: OOM, disk, driver, network events |
| `top`, `free`, `df`, `ss` | live resource and network state |
| `/proc` | process and kernel pseudo-files |
| metrics agent | CPU, memory, disk, network over time |
| tracing/profiling | deeper latency and CPU path evidence |

---

## 3. Commands

```bash
journalctl -u my-service --since "30 minutes ago"
journalctl -p err -b
dmesg -T | tail -100
top
free -h
df -h
ss -ltnp
systemctl --failed
```

---

## 4. Runbook Shape

```text
symptom -> scope -> impact -> evidence -> hypothesis -> mitigation -> validation -> prevention
```

Good runbooks include:

- commands to collect evidence
- expected healthy output
- safe mitigations
- rollback steps
- escalation criteria
- post-incident prevention

---

## 5. Production Failure Modes

- logs rotated away before incident review
- only average CPU monitored, p99 latency missed
- no alert on disk/inodes
- no service-level health check
- metrics exist but no runbook says what to do
- noisy logs hide the causal error

---

## 6. Interview Summary

```text
For Linux observability, I combine service logs, journal logs, kernel messages, resource metrics, process state, network state, and recent changes. A good runbook tells responders what to check, what healthy looks like, how to mitigate safely, and how to validate recovery.
```

---

## 7. Revision Notes

- One-line summary: Observability is evidence plus action.
- Three keywords: logs, metrics, runbook.
- One trap: collecting logs without knowing what decision the evidence supports.