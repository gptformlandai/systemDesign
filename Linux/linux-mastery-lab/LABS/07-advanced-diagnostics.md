# Lab 07: Advanced Diagnostics

Goal: learn when and how to use deeper Linux diagnostic tools safely.

---

## Run

```bash
bash SCRIPTS/06-advanced-diagnostics-safe.sh ssh
```

Optionally pass a PID you are allowed to inspect:

```bash
bash SCRIPTS/06-advanced-diagnostics-safe.sh ssh 1234
```

---

## Explain

- Which advanced tools are installed?
- What are your shell limits?
- Do service limits differ from shell limits?
- Which kernel tunables are visible?
- If a PID was provided, how many file descriptors are open?

---

## When To Use Advanced Tools

| Symptom | Tool |
|---|---|
| app cannot find config but logs are vague | `strace` |
| disk remains full after deleting file | `lsof +L1` |
| dependency timeout is unclear | scoped `tcpdump` |
| CPU hot path is unknown | `perf` |
| too many open files | `ulimit`, `systemctl show`, `/proc/PID/limits` |
| crash without useful app log | `coredumpctl` |

---

## Completion Gate

- You can explain what `strace`, `lsof`, `tcpdump`, `perf`, `ulimit`, `sysctl`, and `coredumpctl` prove.
- You can name the production risk of each tool.
- You can scope a diagnostic command before running it.