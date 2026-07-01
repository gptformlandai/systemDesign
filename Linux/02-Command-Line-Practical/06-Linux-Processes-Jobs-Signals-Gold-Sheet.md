# Linux Processes, Jobs, and Signals - Gold Sheet

> Track File #6 of 30 - Group 02: Command-Line Practical
> For: production debugging | Level: intermediate | Mode: process control

## 1. Core Idea

A process is a running program with a PID, parent PID, user, memory, CPU time, open files, environment, and signal handlers.

```text
program on disk -> process in memory -> scheduled by kernel -> observed by tools
```

---

## 2. Process Commands

```bash
ps -ef
ps aux --sort=-%cpu | head
top
htop                 # if installed
pgrep -af nginx
pidof sshd
kill -TERM 1234
kill -KILL 1234
jobs
fg
bg
nohup command &
```

---

## 3. Signal Map

| Signal | Meaning |
|---|---|
| `SIGTERM` | ask process to stop gracefully |
| `SIGKILL` | force kill, cannot be caught |
| `SIGHUP` | terminal hangup, often used to reload config |
| `SIGINT` | interrupt, usually Ctrl-C |
| `SIGSTOP` | pause process |
| `SIGCONT` | resume process |

Prefer `SIGTERM` before `SIGKILL` unless the process is truly stuck.

---

## 4. Production Checks

| Symptom | Check |
|---|---|
| service not responding | process exists, port listening, logs |
| high CPU | top process, thread dump, recent deploy |
| memory growth | RSS, heap, OOM logs |
| zombie process | parent process not reaping |
| too many files | open file count, limits |

Useful commands:

```bash
lsof -p 1234
ls -l /proc/1234/fd | wc -l
cat /proc/1234/status
```

---

## 5. Failure Modes

- killing the parent or wrong PID
- using `kill -9` before collecting evidence
- process restarted by systemd immediately after manual kill
- background job exits when shell closes
- missing resource limits like open files or max processes

---

## 6. Interview Summary

```text
I debug processes by checking PID, parent, user, CPU, memory, open files, environment, logs, and listening ports. I prefer graceful shutdown with SIGTERM and reserve SIGKILL for stuck processes after collecting enough evidence.
```

---

## 7. Revision Notes

- One-line summary: A Linux process is the unit of running work.
- Three keywords: PID, signal, resource.
- One trap: killing a process without knowing whether a supervisor will restart it.