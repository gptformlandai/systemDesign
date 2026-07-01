# Linux Performance: CPU, Memory, and I/O Troubleshooting - MAANG Sheet

> Track File #12 of 30 - Group 03: Senior Production
> For: production performance interviews | Level: senior | Mode: saturation analysis

## 1. Core Idea

Performance debugging starts by identifying the saturated resource.

```text
latency or errors -> CPU, memory, disk I/O, network, locks, downstream dependency
```

Do not guess the bottleneck from the symptom alone.

---

## 2. Command Map

| Resource | Commands |
|---|---|
| CPU | `top`, `mpstat`, `pidstat`, `uptime` |
| memory | `free -h`, `vmstat`, `cat /proc/meminfo`, `dmesg | grep -i oom` |
| disk I/O | `iostat`, `iotop`, `df -h`, `du`, `lsof` |
| process | `ps`, `top -H`, `pidstat`, `/proc/<pid>` |
| system trend | `sar` if available |

---

## 3. Key Concepts

| Concept | Meaning |
|---|---|
| load average | runnable or uninterruptible tasks over time |
| CPU user/system/iowait | CPU used by app, kernel, or waiting on I/O |
| RSS | resident memory used by process |
| swap | disk-backed memory, can destroy latency when overused |
| OOM killer | kernel kills process when memory pressure is severe |
| iowait | CPU waiting for disk I/O completion |

---

## 4. Debug Flow

```text
impact -> top resource -> top process -> recent change -> logs -> limits -> mitigation -> prevention
```

Useful commands:

```bash
uptime
top -o %CPU
free -h
vmstat 1 5
df -h
df -ih
dmesg -T | grep -i -E 'oom|killed process'
```

---

## 5. Production Failure Modes

- high CPU from tight loop, expensive query, bad deploy, crypto/compression, log storm
- memory leak or cache growth triggers OOM killer
- disk full blocks writes and service startup
- high iowait from slow disk or noisy neighbor
- too many open files prevents new connections
- CPU throttling from container limits

---

## 6. Interview Summary

```text
For Linux performance incidents, I first identify the saturated resource with top, uptime, free, vmstat, iostat, df, and logs. Then I find the responsible process or dependency, mitigate safely, and prevent recurrence with limits, alerts, capacity changes, profiling, or rollback.
```

---

## 7. Revision Notes

- One-line summary: Performance debugging is resource saturation evidence.
- Three keywords: CPU, memory, I/O.
- One trap: treating high load average as only CPU, even though uninterruptible I/O wait can raise load too.