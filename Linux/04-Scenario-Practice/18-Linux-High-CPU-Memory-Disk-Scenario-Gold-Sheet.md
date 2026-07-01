# Linux High CPU, Memory, and Disk Scenario - Gold Sheet

> Track File #18 of 30 - Group 04: Scenario Practice
> For: production incident drills | Level: intermediate to senior | Mode: resource saturation

## 1. Scenario

```text
An application is slow and alerts show high host load.
```

Do not assume CPU. Prove which resource is saturated.

---

## 2. Triage Commands

```bash
uptime
top
ps aux --sort=-%cpu | head
ps aux --sort=-%mem | head
free -h
vmstat 1 5
df -h
df -ih
dmesg -T | grep -i -E 'oom|killed process|error'
```

---

## 3. Interpretation

| Evidence | Meaning |
|---|---|
| high CPU user | app work, loop, traffic, expensive code |
| high CPU system | kernel work, syscalls, network, disk pressure |
| high iowait | disk/storage bottleneck |
| low free memory plus OOM | memory pressure or leak |
| disk full | writes fail, services may crash |
| inode full | cannot create files despite free bytes |

---

## 4. Mitigation

- rollback recent deploy
- restart leaking process after evidence collection
- scale out or shift traffic
- clean/rotate logs safely
- increase disk or memory capacity
- reduce traffic or rate-limit noisy clients

---

## 5. Interview Summary

```text
For high-load Linux incidents, I identify whether CPU, memory, disk bytes, inodes, disk I/O, or a downstream dependency is the actual bottleneck. I use uptime, top, ps, free, vmstat, df, dmesg, and logs, then mitigate safely and add alerts or capacity fixes.
```

---

## 6. Revision Notes

- One-line summary: High load is a symptom, not a root cause.
- Three keywords: saturation, evidence, mitigation.
- One trap: deleting files without checking whether processes still hold them open.