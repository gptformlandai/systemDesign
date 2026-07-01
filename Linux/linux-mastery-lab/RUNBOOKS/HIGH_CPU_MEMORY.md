# Runbook: High CPU Or Memory

## Symptoms

- high load
- slow response
- OOMKilled process
- swap usage spike

## Confirm

```bash
uptime
top
ps aux --sort=-%cpu | head
ps aux --sort=-%mem | head
free -h
vmstat 1 5
dmesg -T | grep -i -E 'oom|killed process'
```

If the basic evidence is inconclusive:

```bash
pidstat 1 5
perf top
systemctl show SERVICE --property=LimitNOFILE,LimitNPROC,TasksMax
cat /proc/PID/limits
```

## Mitigate

- rollback recent change
- scale out or shift traffic
- restart leaking process after evidence collection
- raise limit or tune memory use
- rate limit overload traffic

## Prevent

- CPU/memory alerts
- profiling
- memory leak tests
- capacity planning
- container resource limit review