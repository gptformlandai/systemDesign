# Java Deadlock Example

## What This Demonstrates

Two threads transfer money between bank accounts, acquiring locks in opposite orders. Within 100ms, both threads are stuck waiting for each other — a textbook deadlock.

## Files

- `DeadlockDemo.java` — the buggy version (deadlocks)
- `DeadlockFixed.java` — the fix (lock ordering)

## Run The Buggy Version

```bash
javac DeadlockDemo.java
java DeadlockDemo
# The application hangs after printing the start message.
# No crash, no error, no output.
```

## Debug With IntelliJ

1. Open this folder in IntelliJ.
2. Run `DeadlockDemo` in debug mode (Ctrl+D).
3. Wait for the hang (~100ms).
4. Open the Threads panel in the Debug window.
5. Observe `transfer-A-to-B` and `transfer-B-to-A` in BLOCKED state.
6. Click each to read the lock wait chain.
7. Run → Dump Threads to see the "Found one Java-level deadlock" output.

## Run The Fixed Version

```bash
javac DeadlockFixed.java
java DeadlockFixed
# Prints: Transfer complete. A=1000.0 B=1000.0
```
