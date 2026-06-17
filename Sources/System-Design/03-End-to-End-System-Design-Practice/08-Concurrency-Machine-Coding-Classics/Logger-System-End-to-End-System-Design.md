# Logger System - End-to-End System Design

> Goal: practice one complete machine-coding + LLD classic from problem understanding to thread-safe implementation, async logging, rotation, and production-scale thinking.

---

## How To Use This File

- Use this when the interview asks for logger, logging framework, async logger, appenders, or observability basics.
- Start with a simple thread-safe logger, then evolve to log levels, formatters, appenders, async queue, file rotation, and shutdown flushing.
- Keep one idea sharp: logging should be safe under concurrency and should not become the bottleneck or corrupt output.
- In interviews, explain levels, appenders, formatting, synchronization, async buffering, backpressure, and failure behavior.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Logger focus |
|---|---|---|
| Problem understanding | Can define logging contract | levels, message, context, destinations |
| HLD | Can design extensible logger | logger, formatter, appenders, async queue |
| LLD | Can model components cleanly | `LogEvent`, `LogLevel`, `Logger`, `Appender`, `Formatter`, `AsyncLogger` |
| Machine coding | Can implement critical path | thread-safe log call, async append, flush, rotation |
| Traffic spikes | Can protect app | bounded queue, drop policy, sampling, backpressure |
| Scale | Can evolve design | centralized collection, structured logs, correlation IDs |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Support log levels: DEBUG, INFO, WARN, ERROR, FATAL.
- Log message with timestamp, level, logger name, thread name, and context.
- Support multiple appenders: console, file, remote.
- Support formatters: plain text and JSON/structured.
- Support level filtering.
- Support thread-safe logging from many threads.
- Support flush and shutdown.

Optional requirements to clarify:

- Should logging be synchronous or asynchronous?
- Is file rotation required?
- Should logs include correlation/request IDs?
- What happens when the async queue is full?
- Should appenders fail independently?
- Are dynamic log level changes required?

Out of scope unless asked:

- Full centralized log storage backend.
- Full distributed tracing system.
- Full SIEM/security analytics.
- Full OpenTelemetry implementation.

## 1.2 Non-Functional Requirements

Correctness:

- Log lines should not be interleaved/corrupted.
- A log event should go to all configured appenders or report appender failures.
- Shutdown should flush important pending logs.

Thread safety:

- Multiple threads can call `log()` concurrently.
- Appenders that share resources must synchronize.
- Async queue must be thread-safe.

Performance:

- Avoid heavy work on hot application path.
- Support async logging for high throughput.
- Bound memory used by pending logs.

Reliability:

- Logging failure should not crash business logic unless policy says so.
- Critical logs should not be silently lost.

## 1.3 Constraints

- File writes are slower than memory operations.
- Remote logging can be slow/unavailable.
- Logging inside locks can amplify contention.
- Unbounded async queues can cause memory pressure.
- Dropping logs may hide incidents.
- Synchronous logging can increase request latency.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---:|
| App threads | 10-1000 |
| Log events/sec | thousands to millions |
| Log line size | 100 bytes-10 KB |
| Async queue capacity | bounded |
| File rotation size | 100 MB-1 GB |
| Flush interval | seconds |

## 1.5 Capacity Math

Back-of-the-envelope:

- `100K logs/sec * 500 bytes` is `50 MB/sec` of log output.
- A queue of 1M events at 500 bytes is 500 MB before object overhead.
- Remote logging latency must not happen inside request critical path.
- JSON logs are larger but easier to parse downstream.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Console log | slow and blocking |
| File append | fast with buffering, slower on fsync |
| Remote append | network-dependent |
| Queue drop threshold | app-specific |
| Flush on shutdown | bounded timeout |

## 1.6 Clarifying Questions To Ask

- Sync or async logger?
- Which appenders are needed?
- Should logs be structured JSON?
- What is the queue full policy?
- Should file rotation be size-based, time-based, or both?
- Should app continue if logging fails?

Strong interview framing:

> I will design a thread-safe logger with log levels, formatters, and appenders. For performance, I will support an async logger backed by a bounded blocking queue and a worker thread. Appenders serialize writes safely, file appender supports rotation, and shutdown flushes pending logs.

---

# 2. High-Level Design

## 2.1 Architecture

Synchronous version:

```text
Application Thread
  -> Logger.log()
  -> Level Filter
  -> Formatter
  -> Appenders
```

Asynchronous version:

```text
Application Threads
  -> AsyncLogger.log()
  -> Bounded Queue
  -> Logger Worker Thread
  -> Formatter
  -> Console/File/Remote Appenders
```

Core flow:

1. Caller calls `logger.info("message")`.
2. Logger checks if level is enabled.
3. Logger builds `LogEvent`.
4. Sync logger writes to appenders immediately.
5. Async logger enqueues event.
6. Worker drains queue and writes to appenders.
7. Shutdown flushes queue and appenders.

## 2.2 APIs

```java
interface Logger {
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message, Throwable error);
    boolean isEnabled(LogLevel level);
    void flush();
    void shutdown();
}

interface Appender {
    void append(LogEvent event);
    void flush();
    void close();
}

interface Formatter {
    String format(LogEvent event);
}
```

Config:

```json
{
  "level": "INFO",
  "async": true,
  "queueCapacity": 100000,
  "appenders": [
    {"type": "console"},
    {"type": "file", "path": "app.log", "maxBytes": 104857600}
  ]
}
```

Important API points:

- Level check should happen before expensive formatting.
- Async logger should not allocate huge objects unnecessarily.
- Appender failures should be isolated.
- Shutdown should be explicit.

## 2.3 Core Components

Think of Logger System as five planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Event plane | log event data | structured context |
| Filtering plane | log levels and rules | reduce noise |
| Formatting plane | text/JSON formatting | readable/parseable output |
| Appender plane | console/file/remote output | durable/visible logs |
| Async plane | queue and worker | reduce caller latency |

### Component Responsibility Map

| Component | Owns | Does not own |
|---|---|---|
| `Logger` | level check and event creation | file rotation details |
| `AsyncLogger` | queue and worker handoff | output formatting internals |
| `Formatter` | string/JSON representation | filtering |
| `Appender` | output destination | log event creation |
| `FileAppender` | file writes and rotation | remote transport |
| `LogContext` | correlation/request metadata | appender flushing |

### Thread Safety

Shared state:

- Logger level/config.
- Appender file handle.
- Async queue.
- Shutdown flag.

Rules:

- Appender writes to same file must be synchronized or single-threaded through worker.
- Async queue handles multi-producer safety.
- Dynamic config changes need volatile/atomic reference.
- Shutdown must stop accepting or define best-effort behavior.

Race-condition trap:

```text
Thread A writes "error one\n"
Thread B writes "error two\n"

Without appender synchronization, output can become:
errerror two
or one
```

### Async Logging

Benefits:

- Low caller latency.
- Batch writes.
- Isolate slow appenders.

Costs:

- Logs can be lost if process crashes before flush.
- Queue can fill.
- Shutdown becomes important.

Queue full policies:

| Policy | Behavior | Use |
|---|---|---|
| block | caller waits | critical logs |
| drop debug/info | preserve app latency | high-volume services |
| drop newest | reject current event | telemetry-style logs |
| drop oldest | keep recent logs | latest-state debugging |
| sync fallback | caller writes directly | preserve logs at latency cost |

### File Rotation

Why it exists:

- Log files should not grow forever.

Rotation policies:

- Size-based: rotate after N bytes.
- Time-based: rotate hourly/daily.
- Hybrid: size or time.

Safe rotation:

- Hold file appender lock.
- Flush current file.
- Rename/close current file.
- Open new file.
- Optionally compress old file asynchronously.

## 2.4 Data Layer

Log event:

```json
{
  "timestamp": "2026-06-17T12:00:00Z",
  "level": "ERROR",
  "loggerName": "payment-service",
  "threadName": "worker-7",
  "message": "payment provider timeout",
  "context": {"requestId": "req-1", "tenantId": "t-1"}
}
```

Data structures:

| Need | Data structure |
|---|---|
| appenders | list of appenders |
| async buffer | bounded blocking queue |
| logger config | immutable config object / atomic reference |
| MDC/context | thread-local map |
| file rotation | current file metadata |

## 2.5 Scalability

### Single Process Scaling

- Use async logger.
- Batch writes in worker.
- Separate appenders by speed.
- Drop/sampling policy for low-level logs.
- Avoid expensive formatting when disabled.

### Distributed Evolution

```text
Application logger -> local file/stdout -> agent -> centralized log storage
```

Production approach:

- App writes structured logs to stdout/file.
- Agent ships logs to central system.
- Central log storage handles indexing/retention.

## 2.6 Performance

Optimization rules:

- Check level before string formatting.
- Use async queue for slow appenders.
- Batch flush.
- Avoid logging inside hot locks.
- Keep context small.
- Prefer structured logs for downstream search.

Latency:

```text
sync logging latency = format + appender write
async logging latency = enqueue + possible wait/drop
```

## 2.7 Async Systems

Async logger is a producer-consumer system:

- Application threads are producers.
- Log queue is bounded buffer.
- Logger worker is consumer.
- Appenders are output sinks.

Reliability upgrades:

- bounded queue
- flush on shutdown
- drop counters
- fallback appender
- local disk buffer for remote logging

## 2.8 Safety And Failure Handling

Failure modes:

| Failure | Handling |
|---|---|
| file appender fails | fallback console/stderr and increment error metric |
| remote appender slow | async queue and circuit breaker |
| queue full | configured drop/block policy |
| formatter throws | catch and emit safe fallback message |
| shutdown with pending logs | drain with timeout |
| process crash | accept possible loss unless sync/fsync enabled |

Important:

- Logger should generally not throw into business code.
- Fatal logging may need synchronous flush.
- Appender failures should be visible.

## 2.9 Observability

Track:

- logs emitted by level
- queue depth
- dropped log count
- appender write latency
- appender failures
- flush duration
- file rotation count
- bytes written

Alerts:

- dropped ERROR logs
- queue full for sustained time
- file appender failures
- remote appender backlog grows

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| logging | synchronous | asynchronous | durability simplicity vs caller latency |
| queue | bounded | unbounded | memory safety vs possible drops/blocks |
| format | plain text | JSON | readability vs machine parsing |
| failure | throw | swallow/report | correctness pressure vs app availability |
| rotation | size-based | time-based | file control vs operational predictability |
| flush | every log | batched | durability vs throughput |

Interview framing:

> I would design the logger with levels, formatters, and appenders, and make appenders thread-safe. For production performance, I use async logging with a bounded queue, explicit drop/block policy, and flush-on-shutdown.

---

# 3. Low-Level Design

LLD goal:

> Model Logger System around log events, levels, filters, formatters, appenders, async queue, worker lifecycle, and file rotation.

Simple rules:

- Check level before formatting.
- Build immutable log event.
- Appender writes are thread-safe.
- Async queue is bounded.
- Shutdown flushes or reports dropped logs.

## 3.1 Object Modelling

| Entity | Owns | Key invariant |
|---|---|---|
| `LogEvent` | timestamp, level, message, context | immutable |
| `LogLevel` | severity ordering | filter compares correctly |
| `Logger` | level filtering and event creation | does not corrupt appenders |
| `Formatter` | event serialization | no side effects |
| `Appender` | output sink | append is thread-safe or single-worker |
| `AsyncLogger` | queue and worker | bounded memory |
| `FileRotationPolicy` | rotation decision | file does not grow unbounded |

## 3.2 OOP Fundamentals

Encapsulation:

- `FileAppender` hides file handle and rotation lock.
- `AsyncLogger` hides worker thread and queue.
- `Formatter` hides text/JSON details.

Abstraction:

- `Appender` interface supports console/file/remote.
- `Formatter` interface supports plain/JSON.

Composition:

- Logger composes filters, formatter, and appenders.

## 3.3 SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | formatter only formats |
| Open/Closed | add appender without changing logger |
| Liskov Substitution | any appender supports append/flush/close |
| Interface Segregation | separate logger/appender/formatter APIs |
| Dependency Inversion | logger depends on appender/formatter interfaces |

## 3.4 Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Strategy | formatter and drop policy | configurable behavior |
| Composite | multiple appenders | fan out event |
| Producer-Consumer | async logger queue | decouple caller from output |
| Decorator | add filtering/context/timing | cross-cutting |
| Singleton | global logger registry | common but use carefully |

## 3.5 Sequence Diagram

```text
AppThread -> Logger: info(message)
Logger -> Logger: isEnabled(INFO)
Logger -> LogEvent: create immutable event
Logger -> AsyncQueue: offer event
LoggerWorker -> AsyncQueue: take event
LoggerWorker -> Formatter: format
LoggerWorker -> Appenders: append
```

## 3.6 Class Design

```java
interface Logger {
    void log(LogLevel level, String message, Throwable error);
}

interface Appender {
    void append(LogEvent event);
    void flush();
    void close();
}

interface Formatter {
    String format(LogEvent event);
}
```

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| level disabled | return before formatting |
| appender throws | catch, count, fallback |
| async queue full | block/drop/caller-runs by policy |
| shutdown while producers log | reject or best-effort based on policy |
| file rotation races with append | rotate under appender lock |
| formatter fails | safe fallback format |
| recursive logging during appender error | avoid infinite recursion |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
logger/
  LogLevel.java
  LogEvent.java
  Logger.java
  AsyncLogger.java
  Formatter.java
  TextFormatter.java
  JsonFormatter.java
  Appender.java
  ConsoleAppender.java
  FileAppender.java
  DropPolicy.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import IntEnum
from queue import Full, Queue
from threading import Event, Lock, Thread
from typing import Optional


class LogLevel(IntEnum):
    DEBUG = 10
    INFO = 20
    WARN = 30
    ERROR = 40
    FATAL = 50


@dataclass(frozen=True)
class LogEvent:
    timestamp: str
    level: LogLevel
    logger_name: str
    message: str
    error: Optional[BaseException] = None


class TextFormatter:
    def format(self, event: LogEvent) -> str:
        base = f"{event.timestamp} {event.level.name} {event.logger_name} - {event.message}"
        if event.error:
            return f"{base} error={event.error}"
        return base


class ConsoleAppender:
    def __init__(self) -> None:
        self.lock = Lock()

    def append(self, line: str) -> None:
        with self.lock:
            print(line)

    def flush(self) -> None:
        pass


class AsyncLogger:
    def __init__(self, name: str, level: LogLevel, capacity: int = 1000) -> None:
        self.name = name
        self.level = level
        self.formatter = TextFormatter()
        self.appender = ConsoleAppender()
        self.queue: Queue[LogEvent] = Queue(maxsize=capacity)
        self.stopped = Event()
        self.worker = Thread(target=self._run, daemon=True)
        self.worker.start()

    def log(self, level: LogLevel, message: str, error: Optional[BaseException] = None) -> None:
        if level < self.level:
            return
        event = LogEvent(
            timestamp=datetime.now(timezone.utc).isoformat(),
            level=level,
            logger_name=self.name,
            message=message,
            error=error,
        )
        try:
            self.queue.put_nowait(event)
        except Full:
            if level >= LogLevel.ERROR:
                self.appender.append(self.formatter.format(event))

    def info(self, message: str) -> None:
        self.log(LogLevel.INFO, message)

    def error(self, message: str, error: Optional[BaseException] = None) -> None:
        self.log(LogLevel.ERROR, message, error)

    def _run(self) -> None:
        while not self.stopped.is_set() or not self.queue.empty():
            try:
                event = self.queue.get(timeout=0.1)
            except Exception:
                continue
            try:
                self.appender.append(self.formatter.format(event))
            finally:
                self.queue.task_done()

    def shutdown(self) -> None:
        self.stopped.set()
        self.queue.join()
        self.worker.join(timeout=1)
        self.appender.flush()
```

## 4.3 Concurrency Checklist

- Log event is immutable.
- Queue is thread-safe.
- Appender write is synchronized.
- Level check happens before enqueue.
- Worker catches appender/formatter errors.
- Shutdown drains queue.
- Queue full policy is explicit.

## 4.4 Testing Thinking

Tests:

- Disabled level does not append.
- Multiple threads logging do not interleave lines.
- Async logger drains on shutdown.
- Queue full policy handles ERROR differently if configured.
- Appender failure does not crash application.
- File rotation does not lose current line.

Stress tests:

- Many threads logging.
- Very small queue capacity.
- Slow appender.
- Shutdown while logging.
- Large messages.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike | Risk |
|---|---|
| error storm | async queue fills |
| slow disk | appender blocks |
| remote collector down | backlog grows |
| debug enabled accidentally | massive log volume |
| recursive appender error | infinite logging loop |

## 5.2 Immediate Response

- Drop or sample low-level logs.
- Preserve ERROR/FATAL with sync fallback if needed.
- Rate-limit repetitive messages.
- Circuit-break remote appender.
- Alert on dropped logs.
- Rotate/compress files.

## 5.3 Degradation Policy

Protect:

1. Application availability.
2. ERROR/FATAL logs.
3. Bounded memory.
4. INFO/DEBUG logs.

Do not:

- Let logging crash core business code.
- Let logging allocate unbounded memory.
- Hide appender failures completely.
- Log recursively forever.

## 5.4 Spike Interview Answer

> Under log storms, async logging protects request latency but must be bounded. I drop or sample low-level logs first, preserve high-severity logs where possible, and expose dropped-log metrics so operators know logging is degraded.

---

# 6. Scaling Beyond One Process

## 6.1 Production Logging Architecture

```text
Application logger
  -> stdout/file
  -> node agent/sidecar
  -> durable log pipeline
  -> log storage/search
```

## 6.2 Distributed Additions

- Structured JSON logs.
- Correlation/request IDs.
- Log shipping agent.
- Central log storage.
- Retention policies.
- Redaction of secrets/PII.
- Dashboards and alerts.

## 6.3 Interview Answer

> The machine-coding logger writes safely inside one process. At production scale, apps emit structured logs to local files/stdout, an agent ships them to durable log storage, and the logger still needs bounded async behavior so it does not hurt the application.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

```text
I will clarify levels, appenders, sync vs async, formatting, queue full policy, file rotation, and shutdown.
I will model LogEvent, Logger, Formatter, and Appender.
I check level before formatting.
For thread safety, appender writes are synchronized or done by one async worker.
For performance, async logger uses a bounded queue.
For overload, I define drop/block/sync fallback policy.
Shutdown drains and flushes.
```

---

# 8. Fast Recall Rules

- Logger = level filter + event + formatter + appender.
- Check level before formatting.
- Log event should be immutable.
- Appenders must be thread-safe.
- Async logger is producer-consumer.
- Async queue must be bounded.
- Define queue-full policy.
- Do not let logger crash business logic.
- File rotation needs synchronization.
- Shutdown should flush pending important logs.
