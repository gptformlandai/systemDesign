# Debugging Mastery Sheet System

## What Debugging Mastery Means

Debugging mastery is not about memorizing how to click buttons. It is about:

- Knowing exactly which breakpoint type catches a specific class of bug
- Understanding how the IDE represents runtime state (call stack, variable scope, thread frames)
- Recognizing when a bug is actually environment drift: wrong interpreter, JDK, Node version, working directory, env var, classpath, or source map
- Attaching safely to remote processes through the correct protocol, port, and source path mapping
- Debugging concurrency problems: seeing deadlocks in the Threads panel, reading thread dumps, and comparing repeated snapshots
- Debugging async code without getting lost in callback chains or coroutine frames
- Debugging production without pausing the process: logs, metrics, traces, profiles, dumps, deployment markers, and incident timelines
- Knowing when the bug is not code: Kubernetes state, network path, database locks, queue lag, browser cache, OS signals, or unsafe operational change

---

## The Five Debugger Modes

```text
Local debug:    IDE starts and controls the process directly
Remote debug:   IDE attaches to a running process via debug protocol
Test debug:     IDE runs a test class/function in debug mode (JUnit, pytest, Jest)
Observed debug: production investigation through logs, metrics, traces, profiles, dumps
Incident debug: mitigation-first debugging during active customer impact
```

Every IDE covered in this track supports the first three. Senior production debugging requires the last two.

---

## Cross-IDE Bridge

Use [04-Cross-IDE-Debugging](04-Cross-IDE-Debugging) when the debugging problem is bigger than one IDE.

| File | Use When |
|---|---|
| [X01-Cross-IDE-Debugging-Mental-Model-Breakpoints-State-CallStack.md](04-Cross-IDE-Debugging/X01-Cross-IDE-Debugging-Mental-Model-Breakpoints-State-CallStack.md) | you need the universal debugger model: pause, inspect state, read stack, step with a hypothesis |
| [X02-Cross-IDE-Environment-Config-Interpreter-Classpath-SourceMaps.md](04-Cross-IDE-Debugging/X02-Cross-IDE-Environment-Config-Interpreter-Classpath-SourceMaps.md) | code works in terminal but fails in the IDE, or breakpoints/debug behavior differ by runtime config |
| [X03-Cross-IDE-Remote-Attach-Docker-SSH-Ports.md](04-Cross-IDE-Debugging/X03-Cross-IDE-Remote-Attach-Docker-SSH-Ports.md) | attaching IntelliJ, VS Code, or PyCharm to a running process in Docker, SSH, or a remote machine |
| [X04-Cross-IDE-Threads-Async-Concurrency-Triage-Playbook.md](04-Cross-IDE-Debugging/X04-Cross-IDE-Threads-Async-Concurrency-Triage-Playbook.md) | debugging hangs, deadlocks, event-loop stalls, race conditions, promises, threads, or coroutines |

---

## Production Systems Bridge

Use [08-Production-Systems-Debugging](08-Production-Systems-Debugging) when the issue crosses runtime boundaries.

| File | Use When |
|---|---|
| [32-Production-Debugging-Observability-Logs-Metrics-Traces-Gold-Sheet.md](08-Production-Systems-Debugging/32-Production-Debugging-Observability-Logs-Metrics-Traces-Gold-Sheet.md) | you cannot attach a debugger and must use logs, metrics, traces, profiles, and deploy metadata |
| [33-Distributed-Systems-Debugging-Microservices-Traces-Retries-Gold-Sheet.md](08-Production-Systems-Debugging/33-Distributed-Systems-Debugging-Microservices-Traces-Retries-Gold-Sheet.md) | failure crosses service boundaries, retries, timeouts, circuit breakers, or async edges |
| [34-Kubernetes-Debugging-Pods-CrashLoop-OOMKilled-DNS-Probes-Gold-Sheet.md](08-Production-Systems-Debugging/34-Kubernetes-Debugging-Pods-CrashLoop-OOMKilled-DNS-Probes-Gold-Sheet.md) | pods crash, fail probes, get OOMKilled, lose DNS/endpoints, or need ephemeral debug containers |
| [35-Network-HTTP-Debugging-DNS-TLS-CORS-Timeouts-Gold-Sheet.md](08-Production-Systems-Debugging/35-Network-HTTP-Debugging-DNS-TLS-CORS-Timeouts-Gold-Sheet.md) | DNS, TCP, TLS, CORS, proxy, connection, or HTTP status behavior is suspicious |
| [36-Database-Debugging-Slow-Queries-Locks-Transactions-Pools-Gold-Sheet.md](08-Production-Systems-Debugging/36-Database-Debugging-Slow-Queries-Locks-Transactions-Pools-Gold-Sheet.md) | slow queries, lock waits, transaction issues, DB pool exhaustion, or migration regressions appear |
| [37-Browser-DevTools-Performance-Memory-Network-Frontend-Debugging-Gold-Sheet.md](08-Production-Systems-Debugging/37-Browser-DevTools-Performance-Memory-Network-Frontend-Debugging-Gold-Sheet.md) | frontend issue is performance, memory, network waterfall, cache/storage, service worker, or rendering |
| [38-Native-OS-Debugging-Core-Dumps-Strace-Perf-EBPF-Gold-Sheet.md](08-Production-Systems-Debugging/38-Native-OS-Debugging-Core-Dumps-Strace-Perf-EBPF-Gold-Sheet.md) | process crashes, hangs in syscalls, leaks file descriptors, or needs OS-level evidence |
| [39-Advanced-Runtime-Profiling-JVM-Python-NodeJS-JFR-Tracemalloc-DiagnosticReports.md](08-Production-Systems-Debugging/39-Advanced-Runtime-Profiling-JVM-Python-NodeJS-JFR-Tracemalloc-DiagnosticReports.md) | runtime CPU, memory, GC, event-loop, or allocation behavior needs profiler evidence |
| [40-Messaging-Queue-Debugging-Kafka-SQS-RabbitMQ-Celery-DLQ-Gold-Sheet.md](08-Production-Systems-Debugging/40-Messaging-Queue-Debugging-Kafka-SQS-RabbitMQ-Celery-DLQ-Gold-Sheet.md) | async messages are delayed, duplicated, dead-lettered, retried, or lost |
| [41-Safe-Debugging-Incident-RCA-Secrets-PII-Runbooks-MAANG-Sheet.md](08-Production-Systems-Debugging/41-Safe-Debugging-Incident-RCA-Secrets-PII-Runbooks-MAANG-Sheet.md) | debugging action itself may affect availability, expose data, or alter evidence |

---

## Breakpoint Taxonomy (All IDEs)

| Breakpoint Type | When To Use |
|---|---|
| Line breakpoint | Pause execution at a specific line |
| Conditional breakpoint | Pause only when expression is true (e.g., `userId == 42`) |
| Method entry/exit | Pause when a method is called or returns |
| Field watchpoint | Pause when a field is read or written (Java only) |
| Exception breakpoint | Pause when a specific exception is thrown (before stack unwind) |
| Log breakpoint | Print a message and continue without pausing |
| Hit count breakpoint | Pause after N hits (useful for loop bugs at iteration 1000) |

---

## Call Stack Anatomy

```text
Frame 0 (top/current):  method where execution is paused RIGHT NOW
Frame 1:                the method that called Frame 0
Frame 2:                the method that called Frame 1
...
Frame N (bottom):       main() or thread entry point

In multithreaded code: each thread has its own independent call stack.
```

When debugging: walk up the call stack frames to understand context, not just the current line.

---

## Variable Inspection Rules

```text
Local variables:  only visible in the current method scope
Instance fields:  accessible via 'this' in the Variables panel
Static fields:    accessible via the class in the Variables panel
Closures:         captured variables from outer scope (JS/Python)
Coroutine state:  frame locals of a suspended coroutine
```

---

## Concurrency Debugging Mental Model

```text
Thread-safe code:
  outcome is the same regardless of thread scheduling

Broken concurrent code:
  outcome changes depending on who runs first (race condition)
  two threads wait for each other forever (deadlock)
  one thread never gets CPU time (starvation)
  threads make progress but never finish their combined goal (livelock)

Debug approach:
  Use debugger thread controls to suspend/resume only the thread of interest
  Read the thread dump to see what each thread is waiting for
  Use atomic operations and proper synchronization to fix
```

---

## Production Debugging Mental Model

```text
Production-safe debugging:
  observe first
  preserve evidence
  mitigate active impact
  avoid unsafe debug access
  correlate signals
  verify recovery
  prevent recurrence

Production-unsafe debugging:
  attach public debug port
  dump sensitive memory without controls
  restart before evidence
  enable global DEBUG logs
  redrive DLQ blindly
  run packet capture without approval
```

Production debugging is a control loop:

```text
symptom -> blast radius -> time window -> changed thing -> failing boundary
-> evidence -> mitigation -> verification -> RCA -> prevention
```

---

## IDE Debug Protocol Stack

```text
Java (IntelliJ):
  JVM -> JDWP (Java Debug Wire Protocol) -> IntelliJ debugger
  Remote: -agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=n

Node.js (VS Code):
  Node.js -> V8 Inspector Protocol (CDP) -> VS Code debugger
  Remote: node --inspect=0.0.0.0:9229 server.js

Python (PyCharm/VS Code):
  Python -> debugpy or pydevd (debugger stub) -> IDE
  Remote: python -m debugpy --listen 0.0.0.0:5678 app.py

Production:
  Runtime -> logs/metrics/traces/profiles/dumps/events -> observability + incident workflow
  Safe tools: jstack, py-spy, JFR, heap snapshots, kubectl logs/describe/events, curl -v, EXPLAIN
```

---

## Shortcut Memory System

Learn shortcuts in this order per IDE:

1. **F8** (Step Over) and **F7** (Step Into) — navigate the call
2. **F9** (Resume) — continue to next breakpoint
3. **Evaluate Expression** — test hypotheses without changing code
4. **Add Watch** — pin a variable to track across frames
5. **Suspend/Resume Thread** — for concurrency debugging

These five cover 90% of daily debugging.
