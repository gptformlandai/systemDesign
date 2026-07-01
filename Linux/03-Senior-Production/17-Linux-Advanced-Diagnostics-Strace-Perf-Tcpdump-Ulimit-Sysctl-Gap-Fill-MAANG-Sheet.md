# Linux Advanced Diagnostics: strace, perf, tcpdump, ulimit, sysctl - MAANG Gap Fill

> Gap Fill Appendix - Group 03: Senior Production
> For: senior Linux/SRE interviews | Level: pro / MAANG | Mode: deep diagnostics and safe evidence gathering

## 1. Why This Sheet Exists

Basic Linux debugging tools answer the first question: what resource or service looks unhealthy?

Advanced diagnostic tools answer the harder question:

```text
what is the process or kernel actually doing right now?
```

Use these tools when `top`, `ps`, `journalctl`, `curl`, `ss`, `df`, and basic logs show symptoms but not root cause.

---

## 2. Tool Map

| Tool | Best At | Production Risk |
|---|---|---|
| `strace` | system calls, file/path errors, permission failures, network syscall behavior | overhead, noisy output, sensitive arguments |
| `lsof` | open files, sockets, deleted files held open, port ownership | large output, permission limits |
| `tcpdump` | packet-level evidence, DNS/TCP/TLS path proof | sensitive payload capture, high-volume files |
| `perf` | CPU hotspots, kernel/user stack sampling | overhead, permissions, symbol setup |
| `ulimit` | process resource limits: files, processes, core dumps | shell-local confusion, service limits may differ |
| `sysctl` | kernel tunables and runtime parameters | unsafe tuning can destabilize host |
| `coredumpctl` | crash evidence and core dump inspection | disk usage and sensitive memory contents |

---

## 3. Safe Production Rules

1. Start with read-only tools.
2. Scope by PID, port, interface, host, or time window.
3. Avoid broad packet captures on busy interfaces.
4. Avoid changing `sysctl` values during an incident unless rollback is clear.
5. Do not leave large traces or captures on disk.
6. Treat traces, core dumps, and packet captures as sensitive data.
7. Record the command, time, host, and reason.

---

## 4. `strace` Patterns

Attach to a running process:

```bash
strace -f -p PID -o /tmp/trace.out
```

Trace a command:

```bash
strace -f -o /tmp/trace.out command args
```

Common findings:

| Error | Meaning |
|---|---|
| `ENOENT` | file/path not found |
| `EACCES` | permission denied |
| `ECONNREFUSED` | remote/local port refused connection |
| `ETIMEDOUT` | network timeout |
| repeated `openat` | config/library lookup issue |

---

## 5. `lsof` Patterns

```bash
lsof -p PID
lsof -i -P -n
lsof +L1
```

Use it for:

- proving which process owns a port
- finding deleted files still consuming disk
- checking open file count
- seeing files/sockets a process depends on

---

## 6. `tcpdump` Patterns

Capture a small scoped sample:

```bash
tcpdump -i eth0 host 10.0.0.5 and port 443 -c 100 -w /tmp/sample.pcap
```

Use it to answer:

- do packets leave the host?
- does the remote reply?
- is DNS returning expected records?
- is there retransmission or reset behavior?

Do not capture broadly on production interfaces unless incident severity justifies it and data handling is approved.

---

## 7. `perf` Patterns

```bash
perf top
perf record -g -p PID -- sleep 30
perf report
```

Use it when CPU is high but logs do not explain why. It helps identify hot functions, kernel paths, and CPU-heavy loops.

---

## 8. Limits And Tunables

Check shell limits:

```bash
ulimit -a
ulimit -n
```

Check service limits:

```bash
systemctl show SERVICE --property=LimitNOFILE,LimitNPROC,TasksMax
```

Check selected kernel tunables:

```bash
sysctl fs.file-max
sysctl net.core.somaxconn
sysctl vm.swappiness
```

Common trap:

```text
ulimit in your shell is not necessarily the limit for a systemd service.
```

---

## 9. Core Dumps

```bash
coredumpctl list
coredumpctl info PID
```

Core dumps can explain crashes, but they may contain secrets or user data. Handle them like sensitive production artifacts.

---

## 10. Interview Summary

```text
When basic Linux checks show symptoms but not root cause, I use advanced diagnostics carefully: strace for syscalls, lsof for open files and sockets, tcpdump for packet evidence, perf for CPU hotspots, ulimit/systemd limits for resource ceilings, sysctl for kernel tunables, and coredumpctl for crashes. I scope every command tightly and treat captures/traces as sensitive data.
```

---

## 11. Revision Notes

- One-line summary: Advanced tools prove process and kernel behavior directly.
- Three keywords: syscall, packet, limit.
- One trap: changing kernel tunables or capturing packets broadly before scoping the problem.