# Debugging Mastery: Learning Path

## Stage 1: IDE Foundations (Days 1-3)

Goal: Be comfortable with the debug toolbar, breakpoints, and variable inspection in your primary IDE.

1. Read Sheet 01 (IntelliJ Setup) or Sheet 08 (VS Code Setup) or Sheet 15 (PyCharm Setup)
2. Open the corresponding EXAMPLE project and step through it
3. Practice: set a conditional breakpoint, a log breakpoint, and an exception breakpoint
4. Drill: do the shortcut speed drill from Sheet 30 for your IDE

---

## Stage 2: Keyboard Mastery (Days 4-5)

Goal: Zero hesitation on the 7 core shortcuts.

1. Read the Shortcuts Gold Sheet for your IDE (02, 09, or 16)
2. Do the Speed Drill section from Sheet 30 three times with answers covered
3. Practice the Evaluate Expression patterns on the EXAMPLE projects

---

## Stage 3: Advanced Breakpoints (Day 6)

Goal: Know when to use each breakpoint type and choose the right one reflexively.

1. Read Sheet 03 (IntelliJ Advanced Breakpoints) or Sheet 08 (VS Code setup/breakpoints) or X01 (Cross-IDE breakpoint model)
2. Set up each breakpoint type in the Java or Node.js example
3. Understand: conditional, exception, log, watchpoint, hit count

---

## Stage 4: Cross-IDE Environment Debugging (Day 7)

Goal: Recognize when the IDE is running a different process/config than your terminal.

1. Read X02 (Cross-IDE Environment Debugging)
2. For Java, compare Project SDK/JDK, working directory, VM options, and Spring profile
3. For Node/JS, compare `launch.json`, `cwd`, `env`, source maps, and dev server URL
4. For Python, compare interpreter, venv, `PYTHONPATH`, working directory, and framework settings
5. Practice: print runtime identity from the example app and confirm IDE vs terminal match

---

## Stage 5: Remote And Container Debug (Days 8-9)

Goal: Attach a debugger to a Docker container.

1. Read X03 (Cross-IDE Remote Attach)
2. Read Sheet 05 (Java Remote) or Sheet 14 (VS Code Docker) or Sheet 19 (PyCharm Docker)
3. Follow the RUNBOOK for your language
4. Start the example app in Docker and attach the IDE debugger

---

## Stage 6: Concurrency Fundamentals (Days 10-12)

Goal: Read a thread dump and identify deadlocks and stuck threads.

1. Read X04 (Cross-IDE Threads, Async, And Concurrency Triage)
2. Read Sheet 21 (Java Deadlock) and Sheet 25 (Thread Dumps)
3. Run the deadlock EXAMPLE
4. Capture jstack output using SCRIPTS/java-thread-dump.sh
5. Read Sheet 23 (Python GIL/Threading) and Sheet 24 (Node Event Loop)
6. Understand which language has which concurrency model

---

## Stage 7: Concurrency Deep Dive (Days 13-15)

Goal: Explain all five concurrency anti-patterns and their fixes.

1. Read Sheets 21-26
2. Run Scenarios 27, 28, 29 (the full walkthroughs)
3. Drill: Concurrency questions from Sheet 30 (Q31-Q40)

---

## Stage 8: Memory And Performance (Day 16)

Goal: Capture and analyze a heap dump.

1. Read Sheet 07 (Java Memory/OOM)
2. Run an OOM-causing app (see EXAMPLES/java-deadlock-example README for OOM variant)
3. Open the .hprof in IntelliJ and identify the leak
4. Read Sheet 31 (Production Readiness)

---

## Stage 9: Production Readiness (Day 17)

Goal: Know what you can and cannot do in production.

1. Read Sheet 31 (Production Readiness Checklist)
2. Verify all JVM flags, error handlers, and observability hooks are in your apps
3. Complete the full Active Recall drill (Sheet 30)

---

## Stage 10: Production Systems Debugging (Days 18-23)

Goal: Debug real systems when an IDE breakpoint is not available or not safe.

1. Read Sheet 32 for the production observability workflow: logs, metrics, traces, profiles, dumps, deploy markers
2. Read Sheet 33 and practice explaining a microservice timeout with retries, timeouts, and circuit breakers
3. Read Sheet 34 and rehearse `kubectl describe`, `logs --previous`, events, probes, OOMKilled, and ephemeral debug containers
4. Read Sheet 35 and practice DNS/TCP/TLS/HTTP/CORS isolation with `dig`, `nc`, `curl -v`, and `openssl s_client`
5. Read Sheet 36 and practice separating slow query, lock wait, pool exhaustion, and N+1 query symptoms
6. Read Sheet 37 and record one browser Performance profile for a slow frontend interaction

---

## Stage 11: Advanced Profiling, Queues, And Incident Safety (Days 24-27)

Goal: Handle hard production failures and keep debugging safe.

1. Read Sheet 38 for OS-level debugging: signals, core dumps, lsof, strace/dtruss, perf, eBPF
2. Read Sheet 39 for JVM/Python/Node runtime profiling: JFR, async-profiler, tracemalloc, diagnostic reports
3. Read Sheet 40 for queue debugging: Kafka/SQS/RabbitMQ/Celery lag, DLQ, poison messages, idempotency
4. Read Sheet 41 for safe debugging, incident response, RCA, secrets, PII, and dump handling
5. Re-run Sheet 30 Q41-Q60 until answers are instant
6. Re-score yourself with Sheet 31

---

## Interview Preparation Mode (Final 2 Days)

1. Re-read all "Interview Sound Bite" sections (one per sheet)
2. Time yourself: can you explain a Java deadlock investigation in 90 seconds?
3. Time yourself: can you explain async Promise leaks in Node.js in 90 seconds?
4. Time yourself: can you explain a production outage investigation using metrics, traces, logs, deployment markers, and mitigation?
5. Time yourself: can you explain CrashLoopBackOff, OOMKilled, DNS/TLS/CORS, DB lock waits, and queue DLQs?
6. Do the Speed Drill (Sheet 30) until all shortcuts are instant
