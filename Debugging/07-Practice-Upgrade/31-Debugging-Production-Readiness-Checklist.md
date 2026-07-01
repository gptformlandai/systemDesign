# 31. Debugging Production Readiness Checklist

## Purpose

Before deploying to production, before a senior interview, before on-call rotation — verify that every item on this list is addressed. These are the things experienced engineers look for when a production system is debugged blindly under pressure.

---

## Java / Spring Boot Readiness

### JVM Startup Flags

```bash
# Production-safe JVM flags:
-Xms2g                                          # pre-allocate heap (avoid resize pause)
-Xmx2g                                          # max heap = same as initial (fixed size)
-XX:+UseG1GC                                    # G1 garbage collector (predictable pauses)
-XX:MaxGCPauseMillis=200                        # target max GC pause
-XX:+HeapDumpOnOutOfMemoryError                 # auto capture on OOM
-XX:HeapDumpPath=/var/log/app/heap-$(date +%s).hprof
-XX:+ExitOnOutOfMemoryError                     # restart process on OOM (let K8s restart it)
-Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
-Djava.security.egd=file:/dev/./urandom         # faster SecureRandom (Docker)
```

### What NOT To Enable In Production

```text
NEVER in production:
  -agentlib:jdwp=...          (JDWP gives full JVM control to anyone who connects)
  
Use instead:
  jstack <PID>                 (thread dump, read-only)
  jmap -dump:live,...          (heap dump, read-only)
  /actuator/threaddump         (thread dump via HTTP, behind auth)
  /actuator/heapdump           (heap dump via HTTP, behind auth)
```

### Actuator Security

```yaml
# application-prod.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,threaddump,heapdump
  endpoint:
    health:
      show-details: when-authorized   # not always
  server:
    port: 8081    # separate port for actuator, protected by VPN/firewall
```

---

## Thread Pool Sizing

```java
// Too small: requests queue up, latency increases.
// Too large: memory pressure, context switching overhead.

// Rule of thumb:
//   I/O bound: threads = cores * (1 + wait_time / compute_time)
//   CPU bound: threads = cores + 1

// Spring Boot Tomcat threads:
server.tomcat.threads.max=200        # default: 200
server.tomcat.accept-count=100       # queue before rejecting

// Spring Boot async executor:
@Configuration
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

---

## Python / FastAPI / Django Production Readiness

### Enable Proper Logging

```python
# settings.py / logging config
import logging
import sys

LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'json': {
            'format': '{"time": "%(asctime)s", "level": "%(levelname)s", "logger": "%(name)s", "message": "%(message)s"}'
        }
    },
    'handlers': {
        'console': {
            'class': 'logging.StreamHandler',
            'stream': sys.stdout,
            'formatter': 'json'
        }
    },
    'root': {
        'handlers': ['console'],
        'level': 'WARNING'   # production: WARNING or ERROR
    },
    'loggers': {
        'myapp': {
            'handlers': ['console'],
            'level': 'INFO',
            'propagate': False
        }
    }
}
```

### asyncio Production Settings

```python
# Detect slow callbacks.
asyncio.get_event_loop().slow_callback_duration = 0.1  # 100ms threshold
# Or via:
PYTHONASYNCIODEBUG=1  # development only
```

### Never In Python Production Debug

```text
NEVER leave in production code:
  breakpoint()
  import pdb; pdb.set_trace()
  debugpy.wait_for_client()     (hangs startup forever)
  debugpy.listen()              (opens debug port without auth)
```

---

## Node.js Production Readiness

### Error Handling

```javascript
// Every async Express route must catch errors.
// Use a wrapper or express-async-errors.
const asyncHandler = fn => (req, res, next) =>
    Promise.resolve(fn(req, res, next)).catch(next);

// Register global handlers.
process.on('unhandledRejection', (reason, promise) => {
    logger.error('Unhandled rejection', { reason: reason?.message, stack: reason?.stack });
    // Do NOT call process.exit() here in production — let PM2/K8s decide.
});

process.on('uncaughtException', (err) => {
    logger.error('Uncaught exception', { error: err.message, stack: err.stack });
    process.exit(1);  // MUST exit after uncaughtException — state is corrupt.
});
```

### Never In Node.js Production

```text
NEVER in production:
  --inspect or --inspect-brk     (exposes CDP debug port)
  node --inspect server.js       (anyone on the network can attach)
  debugger;                      (hardcoded breakpoint — may pause production)
```

---

## Concurrency Pre-Production Checklist

### Java

- [ ] All shared mutable state is protected by synchronized, Lock, or Atomic classes
- [ ] No thread pool sharing between independent components (separate pools for web, async, scheduled)
- [ ] ThreadLocal values are cleaned up in finally blocks (especially in thread pool workers)
- [ ] No unbounded queues (set queue capacity on ExecutorService to prevent OOM)
- [ ] Deadlock impossible? Lock ordering verified or single-lock design used

### Python

- [ ] All threads writing to shared state use threading.Lock
- [ ] asyncio event loop is not blocked by synchronous code (use `run_in_executor` for blocking calls)
- [ ] Multiprocessing workers handle exceptions and terminate cleanly
- [ ] No `threading.Thread(daemon=True)` for critical operations (daemon threads die when main thread exits)

### Node.js

- [ ] No synchronous operations in request handlers (file I/O, crypto, JSON.parse on large data)
- [ ] Worker threads used for CPU-bound tasks
- [ ] All Promises have `.catch()` or are `await`-ed inside `try/catch`
- [ ] Event emitters cleaned up to prevent listener leaks (`emitter.removeListener()`)

---

## Observability: Minimum For Production Debug

```text
You cannot use a debugger in production. You must plan for:

1. Structured Logs (JSON format)
   - Request ID in every log line (correlation ID)
   - Error logs include stack traces
   - Log level changeable at runtime (actuator /loggers, structlog, dynamic log level)

2. Metrics
   - JVM: heap used, GC pause time, thread count, active connections
   - Application: request rate, error rate, p99 latency
   - Business: orders per second, payment failures

3. Thread Dumps On Demand
   - Java: /actuator/threaddump OR jstack <PID>
   - Python: py-spy dump --pid (no restart, no code change)
   - Node.js: kill -USR1 <pid> to enable inspector, or clinic.js

4. Heap Dumps On Demand
   - Java: /actuator/heapdump OR jmap -dump:live
   - Python: memory-profiler, tracemalloc
   - Node.js: Chrome DevTools heap snapshot via inspector
```

---

## Final Interview Sound Bite

Production debugging readiness means: structured JSON logs with correlation IDs, metrics covering error rate and thread/memory health, and the ability to take thread dumps and heap dumps without restarting. JVM flags for production must include `-XX:+HeapDumpOnOutOfMemoryError` to capture the moment of failure automatically. JDWP must never be exposed in production — it gives full JVM control. For Python, `py-spy` enables sampling profiler output on live processes without code changes. For Node.js, `kill -USR1 <pid>` enables the V8 inspector on a running process. The concurrency checklist: every shared state has a lock, every async function has error handling, no unbounded queues, no ThreadLocal leaks, and lock ordering prevents deadlocks.
