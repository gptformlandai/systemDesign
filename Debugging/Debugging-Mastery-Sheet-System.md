# Debugging Mastery Sheet System

## What Debugging Mastery Means

Debugging mastery is not about memorizing how to click buttons. It is about:

- Knowing exactly which breakpoint type catches a specific class of bug
- Understanding how the IDE represents runtime state (call stack, variable scope, thread frames)
- Debugging concurrency problems: seeing deadlocks in the Threads panel, reading thread dumps
- Attaching to remote processes (Docker, K8s, production-like environments)
- Debugging async code without getting lost in callback chains or coroutine frames

---

## The Three Debugger Modes

```text
Local debug:    IDE starts and controls the process directly
Remote debug:   IDE attaches to a running process via debug protocol
Test debug:     IDE runs a test class/function in debug mode (JUnit, pytest, Jest)
```

Every IDE covered in this track supports all three.

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
  Suspend all threads except the one of interest (Thread.suspend in IntelliJ)
  Read the thread dump to see what each thread is waiting for
  Use atomic operations and proper synchronization to fix
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
  Python -> pydevd (debugger stub) -> IDE
  Remote: python -m debugpy --listen 0.0.0.0:5678 app.py
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
