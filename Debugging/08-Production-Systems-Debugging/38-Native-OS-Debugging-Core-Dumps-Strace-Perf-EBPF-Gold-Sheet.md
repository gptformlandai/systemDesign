# 38. Native And OS Debugging: Core Dumps, strace, lsof, perf, eBPF

## Goal

Debug process and operating-system-level failures: crashes, signals, file descriptor leaks, syscalls, CPU hotspots, blocked I/O, and kernel/user boundary issues.

---

## When You Need OS-Level Debugging

Use OS tools when:

- process crashes without app stack trace
- container exits with signal
- app hangs but language debugger is unavailable
- CPU high but app-level profiler is unclear
- file descriptors or sockets leak
- network calls never return
- native library segfaults
- JVM/Python/Node process blocks in native code

---

## Signal Cheat Sheet

| Signal | Meaning |
|---|---|
| SIGKILL 9 | killed immediately, cannot catch |
| SIGTERM 15 | graceful termination request |
| SIGSEGV 11 | invalid memory access |
| SIGABRT 6 | process aborted itself |
| SIGQUIT 3 | quit; JVM prints thread dump |
| SIGUSR1 | app/runtime-specific; Node enables inspector |

Kubernetes exit code:

```text
exit code 137 = 128 + 9  = SIGKILL, often OOMKilled
exit code 143 = 128 + 15 = SIGTERM, graceful shutdown request
```

---

## Core Dump Workflow

```bash
ulimit -c unlimited
cat /proc/sys/kernel/core_pattern
```

When a native crash happens:

```bash
gdb /path/to/binary core.<pid>
(gdb) bt
(gdb) thread apply all bt
```

For JVM native crash:

```text
hs_err_pid<pid>.log
problematic frame
native library name
JVM flags
thread at crash
```

Common causes:

- JNI bug
- native dependency crash
- incompatible library version
- memory corruption
- unsafe off-heap access

---

## lsof: File Descriptor Debugging

```bash
lsof -p <pid>
lsof -i -P -n
lsof -i :8080
```

Use for:

- port already in use
- socket leak
- too many open files
- deleted file still held open
- log file descriptor not released

Symptom:

```text
EMFILE: too many open files
```

Check:

```bash
ulimit -n
lsof -p <pid> | wc -l
```

---

## strace / dtruss

Linux:

```bash
strace -p <pid>
strace -f -p <pid>
strace -tt -T -p <pid>
```

macOS rough equivalent:

```bash
sudo dtruss -p <pid>
```

Use to answer:

- What syscall is the process stuck in?
- Is it reading a file?
- Is it waiting on network?
- Is it repeatedly failing permission checks?
- Is it opening too many files?

Example interpretation:

```text
connect(...) = -1 ECONNREFUSED
read(...)    blocks for seconds
open(...)    = -1 ENOENT
```

---

## perf And CPU Hotspots

Linux:

```bash
perf top -p <pid>
perf record -F 99 -p <pid> -- sleep 30
perf report
```

Use when CPU is high and app-level profiler is not enough.

Look for:

- hot native function
- crypto/compression hotspot
- regex engine
- JSON parser
- GC/native runtime
- spin loop

---

## eBPF / bpftrace Basics

eBPF observes kernel/user behavior with low overhead.

Examples:

```bash
# Count syscalls by process.
bpftrace -e 'tracepoint:raw_syscalls:sys_enter { @[comm] = count(); }'

# Show TCP connect attempts.
bpftrace -e 'tracepoint:syscalls:sys_enter_connect { printf("%s connect\\n", comm); }'
```

Use when:

- you cannot modify app code
- you need system-wide visibility
- issue crosses process/container boundary

Treat eBPF as an advanced production tool; test scripts before using them broadly.

---

## Container Gotchas

Inside containers:

- shell tools may be missing
- PID namespace changes process visibility
- container may not allow ptrace
- core dumps may write to unexpected location
- securityContext may block debugging

Options:

- ephemeral debug container
- privileged debug node/pod if approved
- host-level tools
- runtime-provided dumps/profiles

---

## Practical Question

> A service container exits randomly with code 139 and no application logs. How do you debug?

---

## Strong Answer

Exit code 139 is usually SIGSEGV, so I would suspect a native crash rather than a normal application exception. I would check container events, kernel logs if available, core dump configuration, and any runtime crash file such as JVM `hs_err_pid` logs. If I can get a core dump, I would open it with `gdb` or `lldb` and inspect the crashing thread backtrace.

If the app is Java/Python/Node, I would check native extensions, JNI libraries, compression/crypto/database drivers, or runtime version mismatches. I would also compare image versions and recent dependency changes.

---

## Interview Sound Bite

OS debugging starts when application evidence ends. Signals explain exits, `lsof` explains file/socket leaks, `strace` explains syscall waits, core dumps explain native crashes, and `perf`/eBPF explain CPU and kernel-level behavior.
