# Debugging Mastery Track — IntelliJ · VS Code · PyCharm

From zero to production-grade debugging across Java (IntelliJ), JavaScript/Node.js/Python (VS Code), and Python (PyCharm). Every sheet focuses on real debug workflows, keyboard shortcuts, multithreading/concurrency debugging, and scenario-based walkthroughs.

```text
observe symptom -> set targeted breakpoint -> step through execution -> inspect state
-> identify root cause -> fix -> verify no regression
```

---

## 1. Track Structure

| Group | Folder | IDE / Language |
|---:|---|---|
| 1 | `01-IntelliJ-Java` | IntelliJ IDEA — Java debugging |
| 2 | `02-VSCode-Frontend-NodeJS` | VS Code — Node.js / JS / Python debugging |
| 3 | `03-PyCharm-Python` | PyCharm — Python debugging |
| 4 | `04-Cross-IDE-Debugging` | Common debugging mental models across IntelliJ, VS Code, and PyCharm |
| 5 | `05-Concurrency-Threading` | Concurrency deep dive across all three |
| 6 | `06-Scenario-Practice` | Full scenario walkthroughs |
| 7 | `07-Practice-Upgrade` | Drills, recall, production readiness |
| Lab | `debugging-mastery-lab` | Cheatsheets, examples, scripts |

---

## 2. IntelliJ Java Path

| Sheet | File | What It Builds |
|---:|---|---|
| 1 | [01-IntelliJ-Java/01-IntelliJ-Debugger-Setup-Breakpoints-Debug-Config-HotSheet.md](01-IntelliJ-Java/01-IntelliJ-Debugger-Setup-Breakpoints-Debug-Config-HotSheet.md) | Run/Debug config, line breakpoints, debug toolbar navigation |
| 2 | [01-IntelliJ-Java/02-IntelliJ-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md](01-IntelliJ-Java/02-IntelliJ-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md) | All debug shortcuts: step, resume, evaluate, watches, frames |
| 3 | [01-IntelliJ-Java/03-IntelliJ-Java-Advanced-Breakpoints-Conditional-Watchpoints.md](01-IntelliJ-Java/03-IntelliJ-Java-Advanced-Breakpoints-Conditional-Watchpoints.md) | Conditional, method, field watch, exception, log breakpoints |
| 4 | [01-IntelliJ-Java/04-IntelliJ-Java-Threads-View-Multithreading-Debug.md](01-IntelliJ-Java/04-IntelliJ-Java-Threads-View-Multithreading-Debug.md) | Threads panel, frame inspection, thread suspension, deadlock detection |
| 5 | [01-IntelliJ-Java/05-IntelliJ-Java-Remote-Debug-JVM-Attach-Docker.md](01-IntelliJ-Java/05-IntelliJ-Java-Remote-Debug-JVM-Attach-Docker.md) | Remote JVM debug, JDWP, Docker/K8s port forward attach |
| 6 | [01-IntelliJ-Java/06-IntelliJ-Java-Spring-Boot-HotSwap-LiveReload-Debug.md](01-IntelliJ-Java/06-IntelliJ-Java-Spring-Boot-HotSwap-LiveReload-Debug.md) | Spring Boot debug, hot swap, actuator integration, profile-specific debug |
| 7 | [01-IntelliJ-Java/07-IntelliJ-Java-Memory-Heap-Profiler-OOM-Debug.md](01-IntelliJ-Java/07-IntelliJ-Java-Memory-Heap-Profiler-OOM-Debug.md) | Heap dumps, memory leak detection, profiler, OOM debugging |

---

## 3. VS Code Path

| Sheet | File | What It Builds |
|---:|---|---|
| 8 | [02-VSCode-Frontend-NodeJS/08-VSCode-Debugger-Setup-LaunchJSON-Config-HotSheet.md](02-VSCode-Frontend-NodeJS/08-VSCode-Debugger-Setup-LaunchJSON-Config-HotSheet.md) | launch.json, debug configurations, debug toolbar |
| 9 | [02-VSCode-Frontend-NodeJS/09-VSCode-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md](02-VSCode-Frontend-NodeJS/09-VSCode-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md) | All VS Code debug shortcuts (macOS + Windows/Linux) |
| 10 | [02-VSCode-Frontend-NodeJS/10-VSCode-NodeJS-Debug-Express-Async-EventLoop.md](02-VSCode-Frontend-NodeJS/10-VSCode-NodeJS-Debug-Express-Async-EventLoop.md) | Node.js debug: Express routes, async/await, callbacks, event loop |
| 11 | [02-VSCode-Frontend-NodeJS/11-VSCode-JavaScript-TypeScript-Browser-Debug.md](02-VSCode-Frontend-NodeJS/11-VSCode-JavaScript-TypeScript-Browser-Debug.md) | Browser debug via Chrome DevTools, source maps, React/TS debug |
| 12 | [02-VSCode-Frontend-NodeJS/12-VSCode-Python-Debug-Interpreter-Django-FastAPI.md](02-VSCode-Frontend-NodeJS/12-VSCode-Python-Debug-Interpreter-Django-FastAPI.md) | Python debug in VS Code: interpreter config, Django, FastAPI, pytest |
| 13 | [02-VSCode-Frontend-NodeJS/13-VSCode-NodeJS-Async-Concurrency-Thread-Debug.md](02-VSCode-Frontend-NodeJS/13-VSCode-NodeJS-Async-Concurrency-Thread-Debug.md) | Async stack traces, promise chains, Worker threads, cluster debug |
| 14 | [02-VSCode-Frontend-NodeJS/14-VSCode-Remote-Debug-Docker-SSH-Containers.md](02-VSCode-Frontend-NodeJS/14-VSCode-Remote-Debug-Docker-SSH-Containers.md) | Remote debug via Docker, SSH tunnel, devcontainer, attach to process |

---

## 4. PyCharm Python Path

| Sheet | File | What It Builds |
|---:|---|---|
| 15 | [03-PyCharm-Python/15-PyCharm-Debugger-Setup-Breakpoints-Debug-Config.md](03-PyCharm-Python/15-PyCharm-Debugger-Setup-Breakpoints-Debug-Config.md) | PyCharm debug setup, run configs, line/exception breakpoints |
| 16 | [03-PyCharm-Python/16-PyCharm-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md](03-PyCharm-Python/16-PyCharm-Keyboard-Shortcuts-Debug-Complete-Gold-Sheet.md) | All PyCharm debug shortcuts (macOS + Windows/Linux) |
| 17 | [03-PyCharm-Python/17-PyCharm-Python-Threading-GIL-Concurrency-Debug.md](03-PyCharm-Python/17-PyCharm-Python-Threading-GIL-Concurrency-Debug.md) | threading module debug, GIL explanation, concurrent.futures, thread panel |
| 18 | [03-PyCharm-Python/18-PyCharm-Python-AsyncIO-Async-Await-Event-Loop-Debug.md](03-PyCharm-Python/18-PyCharm-Python-AsyncIO-Async-Await-Event-Loop-Debug.md) | asyncio debug, async breakpoints, coroutine step-through |
| 19 | [03-PyCharm-Python/19-PyCharm-Remote-Debug-Docker-SSH-pydevd.md](03-PyCharm-Python/19-PyCharm-Remote-Debug-Docker-SSH-pydevd.md) | Remote debug with pydevd, SSH interpreter, Docker container debug |
| 20 | [03-PyCharm-Python/20-PyCharm-Python-Django-FastAPI-Pytest-Debug.md](03-PyCharm-Python/20-PyCharm-Python-Django-FastAPI-Pytest-Debug.md) | Django debug server, FastAPI/uvicorn debug, pytest breakpoints |

---

## 5. Cross-IDE Debugging Bridge

Use this bridge when the problem is not tied to one IDE. It teaches the debugger mental model, environment drift checks, remote attach rules, and cross-language concurrency triage.

| Sheet | File | What It Builds |
|---:|---|---|
| X1 | [04-Cross-IDE-Debugging/X01-Cross-IDE-Debugging-Mental-Model-Breakpoints-State-CallStack.md](04-Cross-IDE-Debugging/X01-Cross-IDE-Debugging-Mental-Model-Breakpoints-State-CallStack.md) | universal debugger mental model: breakpoints, call stack, variables, stepping |
| X2 | [04-Cross-IDE-Debugging/X02-Cross-IDE-Environment-Config-Interpreter-Classpath-SourceMaps.md](04-Cross-IDE-Debugging/X02-Cross-IDE-Environment-Config-Interpreter-Classpath-SourceMaps.md) | environment/config mismatch debugging across Java, Node/JS, and Python |
| X3 | [04-Cross-IDE-Debugging/X03-Cross-IDE-Remote-Attach-Docker-SSH-Ports.md](04-Cross-IDE-Debugging/X03-Cross-IDE-Remote-Attach-Docker-SSH-Ports.md) | remote attach through JDWP, Node inspector, debugpy/pydevd, Docker, SSH, and ports |
| X4 | [04-Cross-IDE-Debugging/X04-Cross-IDE-Threads-Async-Concurrency-Triage-Playbook.md](04-Cross-IDE-Debugging/X04-Cross-IDE-Threads-Async-Concurrency-Triage-Playbook.md) | cross-language triage for threads, async, event loops, deadlocks, hangs, and timing bugs |

---

## 6. Concurrency And Threading Deep Dive

| Sheet | File | What It Builds |
|---:|---|---|
| 21 | [05-Concurrency-Threading/21-Java-Concurrency-Debug-DeadLock-RaceCondition-Synchronized.md](05-Concurrency-Threading/21-Java-Concurrency-Debug-DeadLock-RaceCondition-Synchronized.md) | Java thread dump, deadlock detection, synchronized, volatile, jstack |
| 22 | [05-Concurrency-Threading/22-Java-Virtual-Threads-Loom-Debug-ExecutorService.md](05-Concurrency-Threading/22-Java-Virtual-Threads-Loom-Debug-ExecutorService.md) | Project Loom virtual threads, ForkJoinPool, CompletableFuture debug |
| 23 | [05-Concurrency-Threading/23-Python-GIL-Threading-Multiprocessing-AsyncIO-Debug.md](05-Concurrency-Threading/23-Python-GIL-Threading-Multiprocessing-AsyncIO-Debug.md) | Python GIL impact, threading vs multiprocessing, asyncio event loop debug |
| 24 | [05-Concurrency-Threading/24-NodeJS-EventLoop-Worker-Threads-Cluster-Debug.md](05-Concurrency-Threading/24-NodeJS-EventLoop-Worker-Threads-Cluster-Debug.md) | Node.js event loop phases, Worker threads, libuv, cluster debug |
| 25 | [05-Concurrency-Threading/25-Thread-Dump-Analysis-jstack-pySpy-NodeInspect.md](05-Concurrency-Threading/25-Thread-Dump-Analysis-jstack-pySpy-NodeInspect.md) | jstack, py-spy, node --inspect, reading thread dumps for deadlocks |
| 26 | [05-Concurrency-Threading/26-Concurrency-Anti-Patterns-Bugs-All-Languages.md](05-Concurrency-Threading/26-Concurrency-Anti-Patterns-Bugs-All-Languages.md) | Race conditions, deadlocks, starvation, livelocks across Java/Python/Node |

---

## 7. Scenario Practice

| Sheet | File | What It Builds |
|---:|---|---|
| 27 | [06-Scenario-Practice/27-Scenario-Java-Deadlock-IntelliJ-Debug-Walkthrough.md](06-Scenario-Practice/27-Scenario-Java-Deadlock-IntelliJ-Debug-Walkthrough.md) | Full deadlock scenario: detect, locate, resolve in IntelliJ |
| 28 | [06-Scenario-Practice/28-Scenario-NodeJS-Async-Promise-Leak-VSCode-Debug.md](06-Scenario-Practice/28-Scenario-NodeJS-Async-Promise-Leak-VSCode-Debug.md) | Async promise rejection / memory leak debug in VS Code |
| 29 | [06-Scenario-Practice/29-Scenario-Python-Race-Condition-Thread-PyCharm-Debug.md](06-Scenario-Practice/29-Scenario-Python-Race-Condition-Thread-PyCharm-Debug.md) | Python threading race condition: reproduce, debug, fix in PyCharm |

---

## 8. Practice Upgrade

| Sheet | File | What It Builds |
|---:|---|---|
| 30 | [07-Practice-Upgrade/30-Debugging-Active-Recall-Drills-All-IDEs.md](07-Practice-Upgrade/30-Debugging-Active-Recall-Drills-All-IDEs.md) | Active recall Q&A across all three IDEs and concurrency |
| 31 | [07-Practice-Upgrade/31-Debugging-Production-Readiness-Checklist.md](07-Practice-Upgrade/31-Debugging-Production-Readiness-Checklist.md) | Self-assessment checklist with scoring rubric |

---

## 9. Lab

- [debugging-mastery-lab/README.md](debugging-mastery-lab/README.md)
- [debugging-mastery-lab/CHEATSHEETS/](debugging-mastery-lab/CHEATSHEETS/)
- [debugging-mastery-lab/EXAMPLES/](debugging-mastery-lab/EXAMPLES/)

---

## 10. Core Debugging Mental Model

```text
1. REPRODUCE: make the bug happen reliably (understand the trigger)
2. ISOLATE:   narrow down where it happens (module, function, line)
3. INSPECT:   examine state at that point (variables, call stack, threads)
4. HYPOTHESIZE: form a theory about why
5. VERIFY:    test the theory with targeted breakpoints or changes
6. FIX:       change the code
7. CONFIRM:   run the original reproduction case and confirm the fix
8. PREVENT:   add test that catches this regression
```

---

## 11. IDE Selection Guide

| Scenario | Best IDE |
|---|---|
| Java Spring Boot / backend API | IntelliJ IDEA |
| Node.js Express / Fastify API | VS Code |
| React / TypeScript frontend | VS Code + Chrome DevTools |
| Python FastAPI / Django | PyCharm OR VS Code |
| Python data science / Jupyter | VS Code with Jupyter extension |
| Java microservices in Docker | IntelliJ Remote Debug or VS Code |
| Python in Docker/K8s | PyCharm Professional or VS Code devcontainer |
| Multithreaded Java | IntelliJ Threads view + jstack |
| Python asyncio | PyCharm async debug or VS Code |
| Node.js Worker threads | VS Code + node --inspect |
