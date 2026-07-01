# Debugging Mastery Lab

## What This Lab Is

This lab provides runnable examples, quick-reference cheatsheets, operational runbooks, and utility scripts for the Debugging Mastery track. Everything here is designed to be used alongside your IDE — not just read.

---

## Lab Structure

```text
debugging-mastery-lab/
  README.md                           <- this file
  LEARNING_PATH.md                    <- suggested order of progression
  CHEATSHEETS/
    intellij-shortcuts-cheatsheet.md
    vscode-shortcuts-cheatsheet.md
    pycharm-shortcuts-cheatsheet.md
  EXAMPLES/
    java-deadlock-example/            <- runnable Java deadlock demo
    nodejs-async-debug-example/       <- runnable Node.js async bug demo
    python-threading-example/         <- runnable Python race condition demo
  SCRIPTS/
    java-thread-dump.sh               <- captures jstack thread dump
    python-spy-snapshot.sh            <- captures py-spy dump
    node-inspect-attach.sh            <- starts Node with inspector and opens Chrome
  RUNBOOKS/
    runbook-java-deadlock.md
    runbook-memory-leak.md
    runbook-async-hang.md
```

---

## Quick Start

### Java Deadlock Demo

```bash
cd EXAMPLES/java-deadlock-example
javac DeadlockDemo.java
java DeadlockDemo
# Application hangs — open IntelliJ debugger and attach to the PID
```

### Node.js Async Bug Demo

```bash
cd EXAMPLES/nodejs-async-debug-example
npm install
node server.js
# In another terminal: curl http://localhost:3000/orders/ORD-FAIL
```

### Python Race Condition Demo

```bash
cd EXAMPLES/python-threading-example
python3 order_processor.py
# Observe non-deterministic output
```

---

## Prerequisites

- Java 17+, Maven or Gradle
- Node.js 18+
- Python 3.11+
- IntelliJ IDEA (Community or Ultimate)
- VS Code with Python and JavaScript Debug extensions
- PyCharm Community or Professional
