# Module 10 — Spring Boot & Enterprise Concurrency (The Deep Dive)

> **Welcome to the Final Module!** 🎓 This is where all the concurrency theory meets the Spring framework that most of us use daily. You'll know exactly why `@Async` sometimes runs synchronously, how to configure a production executor, and when to pick WebFlux vs Virtual Threads.

---

## 📑 Table of Contents

- [🎯 What You'll Master](#-what-youll-master)
- [🗺️ The Journey Ahead](#️-the-journey-ahead)
- [Part 1: @Async — The AOP Proxy Trap Every Senior Should Know Cold](#part-1-async--the-aop-proxy-trap-every-senior-should-know-cold)
  - [1.1 The Office Intercom Analogy](#-the-analogy-the-office-intercom-system)
  - [1.2 How Spring Implements @Async](#️-how-spring-implements-async)
  - [1.3 The Self-Invocation Trap](#-the-self-invocation-trap)
  - [1.4 Private/Final Methods](#-privatefinal-methods)
  - [1.5 Exception Loss](#-exception-loss)
  - [1.6 Default Executor](#-default-executor)
  - [1.7 Already Async Callers](#-already-async-callers)
- [Part 2: ThreadPoolTaskExecutor — Production Configuration](#part-2-threadpooltaskexecutor--production-configuration)
  - [2.1 The Seven Parameters](#-the-seven-parameters)
  - [2.2 CallerRunsPolicy](#-callerrunspolicy)
  - [2.3 Graceful Shutdown](#-graceful-shutdown)
  - [2.4 TaskDecorator](#-taskdecorator)
- [Part 3: Context Propagation](#part-3-context-propagation)
  - [3.1 The Problem](#-the-problem)
  - [3.2 MDC Propagation](#-mdc-propagation)
  - [3.3 SecurityContext Propagation](#-securitycontext-propagation)
  - [3.4 RequestAttributes Propagation](#-requestattributes-propagation)
  - [3.5 Micrometer ContextSnapshot](#-micrometer-contextsnapshot)
- [Part 4: WebFlux vs Spring MVC + Virtual Threads](#part-4-webflux-vs-spring-mvc--virtual-threads)
  - [4.1 The Decision Matrix](#-the-decision-matrix)
  - [4.2 Migration Honesty](#-migration-honesty)
  - [4.3 Common WebFlux Mistakes](#-common-webflux-mistakes)
- [Part 5: Production War Stories](#part-5-production-war-stories)
- [Part 6: Interview Traps & Self-Check](#part-6-interview-traps--self-check)
- [🎓 The Course Complete!](#-the-course-complete)

---

## 🎯 What You'll Master

By the end of this module, you'll be able to:
- Explain and fix the `@Async` self-invocation trap
- Configure a production-ready `ThreadPoolTaskExecutor`
- Propagate MDC, SecurityContext, and trace IDs across async boundaries
- Make the WebFlux vs Spring MVC + Virtual Threads decision
- Avoid the traps that trip up most Spring developers

---

## 🗺️ The Journey Ahead

```
                 ┌──────────────────────────────────────────────┐
                 │      MODULE 10 - SPRING CONCURRENCY          │
                 │   "Where Theory Meets the Real Framework"    │
                 └──────────────────────────────────────────────┘
                                    │
    ┌───────────┬──────────┬───────┴────────┬──────────┬───────────┐
    │           │          │                │          │           │
    ▼           ▼          ▼                ▼          ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐     ┌─────────┐ ┌────────┐ ┌─────────┐
│ 10.1  │  │ 10.2   │  │ 10.3   │     │  10.4   │ │ War    │ │  The    │
│@Async │  │Task    │  │Context │     │ WebFlux │ │Stories │ │ Course  │
│ Traps │  │Executor│  │Propaga-│     │ vs VT   │ │        │ │Complete!│
│       │  │Config  │  │tion    │     │         │ │        │ │         │
└───────┘  └────────┘  └────────┘     └─────────┘ └────────┘ └─────────┘
```

---

# Part 1: @Async — The AOP Proxy Trap Every Senior Should Know Cold

## 🏢 The Analogy: The Office Intercom System

```
The @Async Analogy:
═══════════════════════════════════════════════════════════════

A @Async method is like delegating a task to a coworker
via the OFFICE INTERCOM SYSTEM.

┌─────────────────────────────────────────────────────────────┐
│                        YOUR OFFICE                          │
│                                                             │
│   ┌─────────┐                         ┌─────────────────┐  │
│   │   YOU   │ ──── "this.method()" ──►│  YOUR METHOD    │  │
│   │         │      (direct call)      │  (runs HERE,    │  │
│   │         │                         │   synchronously)│  │
│   └─────────┘                         └─────────────────┘  │
│                                                             │
│   The intercom NEVER HEARS direct calls!                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                     OUTSIDE YOUR OFFICE                     │
│                                                             │
│   ┌─────────┐      ┌──────────┐      ┌─────────────────┐  │
│   │ CALLER  │ ───► │ INTERCOM │ ───► │  YOUR METHOD    │  │
│   │(another │      │ (PROXY)  │      │  (runs on       │  │
│   │ bean)   │      │          │      │   another       │  │
│   └─────────┘      └──────────┘      │   thread!)      │  │
│                                       └─────────────────┘  │
│                                                             │
│   External calls GO THROUGH the intercom (proxy)!          │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚙️ How Spring Implements @Async

```
@Async Under the Hood:
═══════════════════════════════════════════════════════════════

Step 1: You annotate a method
┌─────────────────────────────────────────────────────────────┐
│  @Async                                                     │
│  public void sendEmail(String to) { ... }                  │
└─────────────────────────────────────────────────────────────┘

Step 2: Spring wraps your bean in a PROXY
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    PROXY                             │   │
│  │  ┌───────────────────────────────────────────────┐  │   │
│  │  │              YOUR ACTUAL BEAN                  │  │   │
│  │  │                                                │  │   │
│  │  │  sendEmail() { ... }                          │  │   │
│  │  │                                                │  │   │
│  │  └───────────────────────────────────────────────┘  │   │
│  │                                                      │   │
│  │  sendEmail() {                                      │   │
│  │      // Intercept!                                  │   │
│  │      executor.submit(() -> actualBean.sendEmail()); │   │
│  │      return immediately;                            │   │
│  │  }                                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Step 3: External callers get the PROXY, not the bean
Step 4: Proxy intercepts, hands to TaskExecutor
Step 5: Returns immediately (or returns CompletableFuture)
```

### Enable It

```java
@Configuration
@EnableAsync  // Turns on the proxy machinery
public class AsyncConfig {}
```

---

## 🚨 Trap 1: Self-Invocation (THE BIG ONE!)

```java
@Service
public class ReportService {

    public void generateAll() {
        for (long id : ids) {
            sendReport(id);  // BUG! this.sendReport() → SYNCHRONOUS!
        }
    }

    @Async
    public void sendReport(long id) { 
        // This should run async, but it doesn't!
    }
}
```

### Why It Fails

```
Self-Invocation Problem:
═══════════════════════════════════════════════════════════════

When you call this.sendReport(id):

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  generateAll() {                                           │
│      sendReport(id);  // Compiles to: this.sendReport(id)  │
│  }                                                          │
│                                                             │
│  "this" = the ACTUAL BEAN, not the proxy!                  │
│                                                             │
│  The proxy is NEVER involved!                              │
│  The @Async interceptor is NEVER triggered!                │
│  The code runs SYNCHRONOUSLY on the calling thread!        │
│                                                             │
│  ╔═══════════════════════════════════════════════════════╗ │
│  ║  The code COMPILES, RUNS, and SILENTLY BLOCKS!        ║ │
│  ║  No error, no warning, just wrong behavior!           ║ │
│  ╚═══════════════════════════════════════════════════════╝ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### The Fixes (Ranked by Cleanliness)

```
Fix 1: Self-Injection (Quick but Smelly)
═══════════════════════════════════════════════════════════════

@Service
public class ReportService {
    
    @Autowired 
    private ReportService self;  // Inject yourself!
    
    public void generateAll() {
        for (long id : ids) {
            self.sendReport(id);  // Goes through the PROXY!
        }
    }
    
    @Async
    public void sendReport(long id) { ... }
}

Works, but self-injection is a code smell.
```

```
Fix 2: Split into Two Beans (RECOMMENDED!)
═══════════════════════════════════════════════════════════════

@Service
public class ReportOrchestrator {
    
    @Autowired 
    private ReportSender sender;  // Separate bean!
    
    public void generateAll() {
        for (long id : ids) {
            sender.sendReport(id);  // External call → proxy!
        }
    }
}

@Service
public class ReportSender {
    
    @Async("appTaskExecutor")
    public CompletableFuture<Void> sendReport(long id) { 
        // Actually runs async!
        return CompletableFuture.completedFuture(null);
    }
}

Cleaner architecture! No self-injection smell!
```

```
Fix 3: AspectJ Weaving (Overkill)
═══════════════════════════════════════════════════════════════

@EnableAsync(mode = AdviceMode.ASPECTJ)
+ load-time weaving configuration

Weaves the aspect directly into the bytecode.
Self-invocation works!

But: Complex setup, operational overhead.
Rarely worth it. Use Fix 2 instead.
```

### The Interview One-Liner

> *"`@Async` works through a Spring proxy, so any call routed via `this.` bypasses it and runs synchronously. Fix by going through the proxy — either self-injection or splitting responsibilities into two beans."*

---

## 🚨 Trap 2: @Async on Private or Final Methods

```
Visibility Requirements:
═══════════════════════════════════════════════════════════════

CGLIB Proxy (default):
┌─────────────────────────────────────────────────────────────┐
│  Can only proxy: public, protected, package-private        │
│  CANNOT proxy: private, final                              │
│                                                             │
│  @Async                                                     │
│  private void send() { }  // SILENT NO-OP!                 │
│                                                             │
│  @Async                                                     │
│  public final void send() { }  // SILENT NO-OP!            │
│                                                             │
│  No warning at startup! Just doesn't work!                 │
└─────────────────────────────────────────────────────────────┘

JDK Dynamic Proxy (interface-based):
┌─────────────────────────────────────────────────────────────┐
│  Can only proxy: interface methods                         │
│  The method must be declared in an interface               │
└─────────────────────────────────────────────────────────────┘

RULE: @Async methods must be PUBLIC and NON-FINAL!
```

---

## 🚨 Trap 3: Return Type & Exception Loss

```
What Happens to Exceptions?
═══════════════════════════════════════════════════════════════

┌─────────────────┬───────────────────────────────────────────┐
│   Return Type   │           What Happens on Error           │
├─────────────────┼───────────────────────────────────────────┤
│ void            │ Exception goes to global                  │
│                 │ AsyncUncaughtExceptionHandler             │
│                 │ (you MUST register one!)                  │
├─────────────────┼───────────────────────────────────────────┤
│ Future<T>       │ Exception stored in the future            │
│ CompletableFuture│ Caller sees it on .get()/.join()        │
└─────────────────┴───────────────────────────────────────────┘

THE TRAP:
┌─────────────────────────────────────────────────────────────┐
│  @Async                                                     │
│  public void sendEmail() {                                 │
│      throw new RuntimeException("SMTP failed!");           │
│  }                                                          │
│                                                             │
│  If you don't register a handler:                          │
│  • Exception is SILENTLY LOGGED                            │
│  • And LOST!                                               │
│  • No one knows it failed!                                 │
└─────────────────────────────────────────────────────────────┘
```

### Register a Handler!

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Async void failure in {}#{}: {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                ex.getMessage(), ex);
            // Alert, metric, whatever you need!
        };
    }
}
```

---

## 🚨 Trap 4: The Default Executor

```
The Default Executor Problem:
═══════════════════════════════════════════════════════════════

If you don't specify an executor:

OLD Spring versions:
┌─────────────────────────────────────────────────────────────┐
│  Used SimpleAsyncTaskExecutor                              │
│  Creates a NEW THREAD per task!                            │
│  UNBOUNDED!                                                │
│  Under load: 10,000 threads → OOM!                        │
└─────────────────────────────────────────────────────────────┘

Spring Boot 3.x:
┌─────────────────────────────────────────────────────────────┐
│  Looks for bean named "taskExecutor" or                    │
│  "applicationTaskExecutor"                                 │
│  Better, but still not YOUR configuration!                 │
└─────────────────────────────────────────────────────────────┘

ALWAYS name and inject your own:

@Async("appTaskExecutor")  // Explicit!
public CompletableFuture<Report> render(long id) { ... }
```

---

## 🚨 Trap 5: Using @Async When Already Async

```
Don't @Async Everything!
═══════════════════════════════════════════════════════════════

If your service is on WebFlux:
┌─────────────────────────────────────────────────────────────┐
│  You're ALREADY async!                                     │
│  Adding @Async just adds thread-hopping overhead           │
│  And BREAKS context propagation!                           │
└─────────────────────────────────────────────────────────────┘

If you're on Virtual Threads:
┌─────────────────────────────────────────────────────────────┐
│  You're ALREADY on a lightweight thread!                   │
│  @Async adds unnecessary complexity                        │
└─────────────────────────────────────────────────────────────┘

USE @Async WHEN:
• A SYNC caller needs to fire-and-forget
• A SYNC caller needs to fork parallel work
• You want to offload to a specific pool

DON'T USE @Async WHEN:
• You're already in a reactive context
• You're already on a virtual thread
• Every method "just because"
```

---

# Part 2: ThreadPoolTaskExecutor — The Production Configuration

## 🔧 Spring Wrapper vs Raw ThreadPoolExecutor

```
ThreadPoolTaskExecutor:
═══════════════════════════════════════════════════════════════

ThreadPoolTaskExecutor is a Spring bean-friendly wrapper
around java.util.concurrent.ThreadPoolExecutor.

Same parameters, PLUS:
• Lifecycle hooks integrated into Spring context
• TaskDecorator support
• Graceful shutdown knobs
• Easy metrics binding
```

---

## 🏭 Production-Shaped Bean (Every Line Deliberate!)

```java
@Bean("appTaskExecutor")
public ThreadPoolTaskExecutor taskExecutor(MeterRegistry metrics) {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    
    // Core and max pool size
    int cores = Runtime.getRuntime().availableProcessors();
    exec.setCorePoolSize(cores);
    exec.setMaxPoolSize(cores * 4);
    
    // BOUNDED queue - no memory blowup!
    exec.setQueueCapacity(1000);
    
    // Thread lifecycle
    exec.setKeepAliveSeconds(60);
    exec.setAllowCoreThreadTimeOut(true);
    
    // Readable thread dumps!
    exec.setThreadNamePrefix("app-async-");
    
    // Real backpressure - the ONLY good rejection policy!
    exec.setRejectedExecutionHandler(new CallerRunsPolicy());
    
    // Graceful shutdown - drain queue before killing
    exec.setWaitForTasksToCompleteOnShutdown(true);
    exec.setAwaitTerminationSeconds(30);
    
    // Context propagation (MDC, Security, etc.)
    exec.setTaskDecorator(new ContextCopyingDecorator());
    
    exec.initialize();
    
    // Metrics - pool utilization + queue depth for free!
    new ExecutorServiceMetrics(
        exec.getThreadPoolExecutor(), 
        "app-async", 
        List.of()
    ).bindTo(metrics);
    
    return exec;
}
```

### Defending Each Line in the Interview

```
Line-by-Line Defense:
═══════════════════════════════════════════════════════════════

setCorePoolSize(cores):
┌─────────────────────────────────────────────────────────────┐
│  Start with CPU count as baseline                          │
│  These threads are always alive                            │
│  Adjust based on workload (IO-bound = more)               │
└─────────────────────────────────────────────────────────────┘

setMaxPoolSize(cores * 4):
┌─────────────────────────────────────────────────────────────┐
│  Allow burst capacity                                      │
│  Only used when queue is FULL                             │
│  4x is a reasonable starting point for mixed workloads    │
└─────────────────────────────────────────────────────────────┘

setQueueCapacity(1000):
┌─────────────────────────────────────────────────────────────┐
│  BOUNDED! This is critical!                                │
│  Unbounded queue = memory blowup under load               │
│  1000 is a starting point - tune based on monitoring      │
└─────────────────────────────────────────────────────────────┘

setRejectedExecutionHandler(new CallerRunsPolicy()):
┌─────────────────────────────────────────────────────────────┐
│  The ONLY rejection policy that implements backpressure!   │
│                                                             │
│  When queue is full:                                       │
│  • AbortPolicy: throws exception (caller crashes)         │
│  • DiscardPolicy: silently drops (data loss!)             │
│  • DiscardOldestPolicy: drops oldest (data loss!)         │
│  • CallerRunsPolicy: caller executes the task itself!     │
│                                                             │
│  CallerRunsPolicy slows down the submitter naturally!     │
│  True backpressure!                                        │
└─────────────────────────────────────────────────────────────┘

setWaitForTasksToCompleteOnShutdown(true):
┌─────────────────────────────────────────────────────────────┐
│  On shutdown, DRAIN the queue first                        │
│  Don't just kill in-flight tasks                          │
│  Prevents orphaned work                                    │
└─────────────────────────────────────────────────────────────┘

setAwaitTerminationSeconds(30):
┌─────────────────────────────────────────────────────────────┐
│  Wait up to 30s for tasks to complete                     │
│  After that, shutdownNow() interrupts stragglers          │
│  Align with K8s terminationGracePeriodSeconds!            │
└─────────────────────────────────────────────────────────────┘

setThreadNamePrefix("app-async-"):
┌─────────────────────────────────────────────────────────────┐
│  Thread dumps become READABLE!                             │
│  "app-async-1" vs "pool-3-thread-7"                       │
│  You'll thank yourself at 3 AM                            │
└─────────────────────────────────────────────────────────────┘

setTaskDecorator(new ContextCopyingDecorator()):
┌─────────────────────────────────────────────────────────────┐
│  Propagates MDC, SecurityContext, RequestAttributes       │
│  Without this, logs lose trace IDs!                       │
│  See Part 3 for details                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛑 Graceful Shutdown Lifecycle

```
Shutdown Sequence:
═══════════════════════════════════════════════════════════════

When Spring closes the context:

Step 1: stop()
┌─────────────────────────────────────────────────────────────┐
│  Pool refuses new tasks (via shutdown())                   │
│  Tasks already in queue continue                          │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
Step 2: If waitForTasksToCompleteOnShutdown = true
┌─────────────────────────────────────────────────────────────┐
│  Drains the queue                                          │
│  Waits for in-flight tasks to complete                    │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
Step 3: After awaitTerminationSeconds
┌─────────────────────────────────────────────────────────────┐
│  Calls shutdownNow()                                       │
│  Interrupts any stragglers                                │
└─────────────────────────────────────────────────────────────┘
           │
           ▼
Step 4: Container waits
┌─────────────────────────────────────────────────────────────┐
│  Until all SmartLifecycle beans stop                      │
└─────────────────────────────────────────────────────────────┘

IMPORTANT: Align with K8s!
spring.lifecycle.timeout-per-shutdown-phase=30s
Should match terminationGracePeriodSeconds in your deployment!
```

---

## ❓ Why Not Just Use @EnableAsync Defaults?

```
The Problem with Defaults:
═══════════════════════════════════════════════════════════════

Old Spring versions:
┌─────────────────────────────────────────────────────────────┐
│  Default = SimpleAsyncTaskExecutor                         │
│  Creates NEW THREAD per task!                              │
│  UNBOUNDED!                                                │
│  CATASTROPHIC under load!                                  │
└─────────────────────────────────────────────────────────────┘

Newer Spring Boot:
┌─────────────────────────────────────────────────────────────┐
│  Uses application-wide shared TaskExecutor                 │
│  Better, but:                                              │
│  • Not YOUR configuration                                  │
│  • No metrics                                              │
│  • No context propagation                                  │
│  • No graceful shutdown tuning                            │
└─────────────────────────────────────────────────────────────┘

THE RULE:
┌─────────────────────────────────────────────────────────────┐
│  EXPLICIT IS ALWAYS BETTER!                                │
│                                                             │
│  • Own your pool                                           │
│  • Observe it (metrics!)                                   │
│  • Size it deliberately                                    │
│  • Configure shutdown properly                             │
└─────────────────────────────────────────────────────────────┘
```

---

# Part 3: Context Propagation Across Async Boundaries

## 🎯 The Problem

```
The Context Problem:
═══════════════════════════════════════════════════════════════

Every request carries INVISIBLE thread-local context:

┌─────────────────────────────────────────────────────────────┐
│  MDC (SLF4J Mapped Diagnostic Context)                     │
│  • traceId                                                 │
│  • spanId                                                  │
│  • requestId                                               │
│  • userId                                                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  SecurityContextHolder                                     │
│  • The authenticated principal                             │
│  • Roles and permissions                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  RequestContextHolder                                      │
│  • Current HttpServletRequest                              │
│  • Request attributes                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Micrometer / OpenTelemetry Context                        │
│  • Trace spans                                             │
│  • Baggage                                                 │
└─────────────────────────────────────────────────────────────┘
```

```
What Happens on @Async:
═══════════════════════════════════════════════════════════════

Request Thread                    Pool Thread
─────────────────                 ───────────────
MDC: {traceId: "abc-123"}         MDC: {} (EMPTY!)
Security: User("alice")           Security: null (ANONYMOUS!)
Request: HttpServletRequest       Request: null

@Async method executes on pool thread...

╔═══════════════════════════════════════════════════════════╗
║  NONE of the context comes along!                         ║
║                                                            ║
║  • Logs lose their trace IDs                              ║
║  • Downstream calls fail auth checks                      ║
║  • Distributed traces BREAK                               ║
╚═══════════════════════════════════════════════════════════╝
```

---

## 🔧 The Clean Fix: TaskDecorator

```
TaskDecorator Pattern:
═══════════════════════════════════════════════════════════════

Spring's TaskDecorator wraps every submitted Runnable
BEFORE it's queued.

1. CAPTURE context on the submitting thread
2. RESTORE context on the worker thread
3. CLEAN UP in finally (critical for pool threads!)
```

### The Implementation

```java
public class ContextCopyingDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        // CAPTURE on submitting thread
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        SecurityContext security = SecurityContextHolder.getContext();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return () -> {
            // Save previous context (for pool thread reuse)
            Map<String, String> prevMdc = MDC.getCopyOfContextMap();
            SecurityContext prevSec = SecurityContextHolder.getContext();
            RequestAttributes prevAttr = RequestContextHolder.getRequestAttributes();
            
            try {
                // RESTORE captured context
                if (mdc != null) MDC.setContextMap(mdc); 
                else MDC.clear();
                SecurityContextHolder.setContext(security);
                RequestContextHolder.setRequestAttributes(attributes);
                
                // Run the actual task
                task.run();
                
            } finally {
                // CLEAN UP - restore previous context!
                if (prevMdc != null) MDC.setContextMap(prevMdc); 
                else MDC.clear();
                SecurityContextHolder.setContext(prevSec);
                RequestContextHolder.setRequestAttributes(prevAttr);
            }
        };
    }
}
```

### Wire It In

```java
exec.setTaskDecorator(new ContextCopyingDecorator());
```

### Why `finally` Restore Matters

```
The Tenant Leak Bug:
═══════════════════════════════════════════════════════════════

Without finally restore:

Pool Thread processes Tenant A's request
  → Sets TenantContext to "A"
  → Finishes
  → DOESN'T CLEAR

Pool Thread processes Tenant B's request
  → TenantContext is STILL "A"!
  → Writes to WRONG database shard!
  → Customer data leak!

This is a REAL production bug that happens!
Always restore in finally!
```

---

## 🆕 Modern Alternative: Micrometer ContextSnapshot

```java
// Micrometer 1.10+ unified API
exec.setTaskDecorator(task ->
    ContextSnapshotFactory.builder()
        .build()
        .captureAll()
        .wrap(task)
);
```

Works across:
- ExecutorService
- Reactor
- Virtual Threads

If you're on Spring Boot 3.x+ with Micrometer 1.10+, use this!

---

## ❓ Virtual Threads — Do You Still Need This?

```
VTs and Context:
═══════════════════════════════════════════════════════════════

YES, you still need context propagation!

Even Virtual Threads are still Thread objects.
ThreadLocal (including MDC and SecurityContext) is still per-thread.

Only ScopedValue (Module 7) makes context inheritance
automatic across VTs launched inside a StructuredTaskScope.

Until Spring's context types migrate to ScopedValue,
you still need TaskDecorator / ContextSnapshot on VT executors!
```

---

# Part 4: WebFlux vs Spring MVC + Virtual Threads

## 📊 The 2026 Decision Matrix

```
┌─────────────────────┬─────────────────────────┬─────────────────────────┐
│     Dimension       │  Spring MVC + VTs       │    Spring WebFlux       │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Programming model   │ Imperative, blocking    │ Reactive (Mono, Flux)   │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Throughput (CRUD)   │ Excellent               │ Excellent               │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Native backpressure │ ❌ (manual Semaphore)   │ ✅ request(n) protocol  │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Streaming           │ Adequate                │ Excellent               │
│ (SSE, WebSocket)    │                         │                         │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Debuggability       │ Native, familiar        │ Async fragments         │
│                     │                         │ Needs Reactor operators │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Library ecosystem   │ Everything works!       │ Needs reactive drivers  │
│                     │ JDBC, JMS, REST clients │ (R2DBC, WebClient)      │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Learning curve      │ Zero                    │ Steep                   │
├─────────────────────┼─────────────────────────┼─────────────────────────┤
│ Best for            │ Request/response CRUD   │ Streaming, event        │
│                     │ Most microservices      │ processing, gateways    │
└─────────────────────┴─────────────────────────┴─────────────────────────┘
```

---

## 🎯 The 2026 Default

```
For TYPICAL Microservices (request/response, DB + downstream calls):
═══════════════════════════════════════════════════════════════

Spring Boot 3.x
+ spring.threads.virtual.enabled=true
+ Spring MVC
+ JDBC/JPA

Keep your imperative code!
Gate downstreams with Semaphore (Module 7).

ONE PROPERTY FLAG = immediate scalability win!
```

```
For STREAMING Pipelines (real-time feeds, SSE, WebSocket, gateway):
═══════════════════════════════════════════════════════════════

Spring WebFlux

Backpressure is a first-class concern.
Reactor's operator model is the right tool.
```

---

## 🔄 The Migration Honesty

```
MVC → WebFlux Migration:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│  This is a WHOLE-APP REWRITE!                              │
│                                                             │
│  Every layer must be reactive-aware:                       │
│  • Controllers                                             │
│  • Services                                                │
│  • Repositories                                            │
│  • Filters                                                 │
│  • Exception handlers                                      │
│                                                             │
│  .block() in a reactive chain = P0 OUTAGE!                │
└─────────────────────────────────────────────────────────────┘

MVC → MVC + Virtual Threads:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│  ONE PROPERTY FLAG!                                        │
│                                                             │
│  spring.threads.virtual.enabled=true                       │
│                                                             │
│  Plus: audit downstream resource usage                     │
│  (Semaphore gates for connection pools)                   │
│                                                             │
│  IMMEDIATE scalability win!                                │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Common WebFlux Mistakes

```
WebFlux Mistakes to Name:
═══════════════════════════════════════════════════════════════

Mistake 1: .block() inside a reactive chain
┌─────────────────────────────────────────────────────────────┐
│  Mono.just(data)                                           │
│      .map(d -> legacyService.process(d))  // .block() inside!│
│                                                             │
│  Occupies an event-loop thread!                            │
│  Reactor has only 8 event-loop threads!                    │
│  8 concurrent .block() calls = TOTAL STALL!               │
└─────────────────────────────────────────────────────────────┘

Mistake 2: JDBC/JPA without subscribeOn
┌─────────────────────────────────────────────────────────────┐
│  Mono.fromCallable(() -> jdbcTemplate.query(...))          │
│                                                             │
│  This BLOCKS the event loop!                               │
│                                                             │
│  FIX:                                                       │
│  Mono.fromCallable(() -> jdbcTemplate.query(...))          │
│      .subscribeOn(Schedulers.boundedElastic())             │
└─────────────────────────────────────────────────────────────┘

Mistake 3: subscribeOn vs publishOn confusion
┌─────────────────────────────────────────────────────────────┐
│  subscribeOn: affects the SUBSCRIPTION (upstream)          │
│  publishOn: affects DOWNSTREAM operators                   │
│                                                             │
│  Mix them up = unpredictable threading!                   │
└─────────────────────────────────────────────────────────────┘

Mistake 4: Not carrying context
┌─────────────────────────────────────────────────────────────┐
│  Reactor uses Context, NOT ThreadLocal!                    │
│  MDC doesn't propagate automatically!                      │
│  Traces break!                                             │
│                                                             │
│  Use Reactor's Context or Micrometer ContextPropagation   │
└─────────────────────────────────────────────────────────────┘
```

---

# Part 5: Production War Stories

## 💥 War Story 1: The @Async That Ran on the Servlet Thread

**Setup:** Team wrote `orchestrator.send(id)` inside a loop calling `this::send`.

**What Happened:**
```
Every "async" batch actually blocked the request thread for 8 seconds.
Users saw 30-second timeouts.
Logs showed all work happening on "http-nio-8080-exec-42".

The @Async annotation was there, but it did NOTHING!
```

**Fix:** Split into two beans. Batch fanned out on the pool for real.

---

## 💥 War Story 2: The SimpleAsyncTaskExecutor OOM

**Setup:** Legacy Spring app had `@EnableAsync` with no custom executor.

**What Happened:**
```
SimpleAsyncTaskExecutor (default in that version) created
one NEW thread per task.

Downstream slowdown → tasks piled up → 12,000 threads spawned!

Native memory (thread stacks × 1 MB) exceeded RSS budget.
JVM killed by OOM-killer.
```

**Fix:** Replaced default with a bounded `ThreadPoolTaskExecutor`.

---

## 💥 War Story 3: The Tenant-Context Leak on Pool Threads

**Setup:** Payment service had multi-tenant `TenantContext` (a ThreadLocal). No TaskDecorator.

**What Happened:**
```
Worker finished Tenant A's request without clearing context.
Next request for Tenant B inherited A's tenant.
Wrote to the WRONG database shard!

Discovered only after a customer support ticket:
"Why can I see another company's data?"
```

**Fix:** TaskDecorator with restore-in-finally. Added audit log check.

---

## 💥 War Story 4: The MDC That Vanished Mid-Request

**Setup:** Log-searching for trace ID `abc-123` returned only half the expected lines.

**What Happened:**
```
Mid-request @Async fanout on a pool with no TaskDecorator.
MDC didn't propagate to the pool threads.
Trace was silently broken.

Half the request's work was invisible in logs!
```

**Fix:** TaskDecorator on every executor bean. Standardized filter that puts traceId in MDC on entry.

---

## 💥 War Story 5: The WebFlux .block() Incident

**Setup:** WebFlux service called a legacy library that internally used `.block()`.

**What Happened:**
```
Under low load: fine.
Under 500 rps: Reactor's 8 event-loop threads all sat in .block() waits.

Total stall!
p99 latency went from 20ms to 30 SECONDS.
```

**Fix:** Wrapped legacy call in `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`.

---

## 💥 War Story 6: The Graceful Shutdown That Wasn't

**Setup:** K8s sent SIGTERM. Spring context started closing.

**What Happened:**
```
Pool didn't have setWaitForTasksToCompleteOnShutdown(true).
In-flight tasks were interrupted mid-DB-transaction.
Orphaned rows in the database!
```

**Fix:** 
- Enabled the flag
- Aligned `spring.lifecycle.timeout-per-shutdown-phase` with K8s `terminationGracePeriodSeconds`

---

# Part 6: Interview Traps & L5 Answers

## 🎯 Quick Reference Table

| Trap | Bad Answer | L5 Answer |
|------|------------|-----------|
| "Why isn't my @Async running async?" | "It should be." | "Self-invocation — `this.method()` bypasses the proxy. Fix by going through the proxy: self-injection or splitting the bean." |
| "Best default executor for @Async?" | "Spring's default." | "Never the default. Register a bounded ThreadPoolTaskExecutor with CallerRunsPolicy, named prefix, graceful shutdown, and a TaskDecorator." |
| "How to propagate MDC to @Async?" | "It's automatic." | "It isn't. Use a TaskDecorator (or Micrometer ContextSnapshot) that captures MDC/SecurityContext on submitter and restores in finally on worker." |
| "Where do @Async void exceptions go?" | "Bubble up." | "Nowhere by default — swallowed. Register an AsyncUncaughtExceptionHandler, or return CompletableFuture and handle on .get()." |
| "MVC vs WebFlux today?" | "WebFlux, it's newer." | "For request/response CRUD, MVC + virtual threads is the 2026 default. WebFlux for streaming and true backpressure." |
| "SimpleAsyncTaskExecutor OK for prod?" | "Sure." | "No — historically created new thread per task, unbounded. Always configure a bounded pool bean." |
| "Can I put @Async on a private method?" | "Yes." | "No — CGLIB can't proxy private/final. Silent no-op." |

---

## 🔧 L5-Grade Code Examples

### Example 1: Complete @Async Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean("appTaskExecutor")
    public ThreadPoolTaskExecutor appTaskExecutor(MeterRegistry metrics) {
        var exec = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        exec.setCorePoolSize(cores);
        exec.setMaxPoolSize(cores * 4);
        exec.setQueueCapacity(1000);
        exec.setKeepAliveSeconds(60);
        exec.setAllowCoreThreadTimeOut(true);
        exec.setThreadNamePrefix("app-async-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(30);
        exec.setTaskDecorator(new ContextCopyingDecorator());
        exec.initialize();
        new ExecutorServiceMetrics(exec.getThreadPoolExecutor(), "app-async", List.of())
            .bindTo(metrics);
        return exec;
    }

    @Override 
    public Executor getAsyncExecutor() { 
        return appTaskExecutor(null); 
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        var log = LoggerFactory.getLogger(AsyncConfig.class);
        return (ex, method, params) ->
            log.error("async void failure in {}#{}: {}",
                method.getDeclaringClass().getSimpleName(), 
                method.getName(),
                ex.getMessage(), ex);
    }
}
```

### Example 2: Fixing Self-Invocation

```java
// BAD - self-invocation bypasses proxy!
@Service
public class Orchestrator {
    @Async("appTaskExecutor")
    public CompletableFuture<Void> send(long id) { ... }

    public void run() {
        ids.forEach(this::send);  // SYNC! Bypasses proxy!
    }
}

// GOOD - separate beans!
@Service
public class Orchestrator {
    @Autowired private Sender sender;

    public void run() {
        List<CompletableFuture<Void>> futures = ids.stream()
            .map(sender::send)
            .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
}

@Service
public class Sender {
    @Async("appTaskExecutor")
    public CompletableFuture<Void> send(long id) { 
        // Actually runs async!
        return CompletableFuture.completedFuture(null); 
    }
}
```

### Example 3: Enabling Virtual Threads

```properties
# application.properties
spring.threads.virtual.enabled=true
```

```java
// Gate downstream resources!
@Bean
public Semaphore hikariGate(DataSourceProperties props) {
    return new Semaphore(props.getHikari().getMaximumPoolSize());
}

@Service
public class UserService {
    @Autowired Semaphore hikariGate;
    @Autowired JdbcTemplate jdbc;

    public User load(long id) throws InterruptedException {
        hikariGate.acquire();
        try { 
            return jdbc.queryForObject(...); 
        } finally { 
            hikariGate.release(); 
        }
    }
}
```

**Say in the interview:** *"VTs make the servlet layer scale, but downstream Hikari is unchanged. The Semaphore gates VT concurrency to whatever the DB can actually handle."*

### Example 4: WebFlux JDBC Done Correctly

```java
// WRONG - blocks event loop!
public Mono<User> loadUser(long id) {
    return Mono.fromCallable(() -> jdbc.queryForObject(SQL, mapper, id));
}

// CORRECT - offload to bounded elastic!
public Mono<User> loadUser(long id) {
    return Mono.fromCallable(() -> jdbc.queryForObject(SQL, mapper, id))
               .subscribeOn(Schedulers.boundedElastic());
}
```

---

## 🎯 Self-Check Questions

1. Why does `this.asyncMethod()` run synchronously? Two fixes.
2. What visibility must an `@Async` method have? Why?
3. Where does an exception in an `@Async void` go by default? How do you catch it?
4. List 5 non-default settings you'd put on a production `ThreadPoolTaskExecutor`.
5. Which rejection policy gives real backpressure? What's the tradeoff?
6. How do you propagate MDC + SecurityContext to an async pool thread?
7. Why is `SimpleAsyncTaskExecutor` dangerous in production?
8. Give the 2026 decision rule for MVC + VT vs WebFlux.
9. Why is `.block()` in WebFlux a P0?
10. Enabling virtual threads in Spring Boot — what property, and what must you audit afterwards?

---

## 🎓 The Spring Concurrency Mantras

> *"@Async works through a proxy. this.method() bypasses it. Split your beans!"*

> *"Never use the default executor. Own your pool, observe it, size it deliberately."*

> *"TaskDecorator: capture on submit, restore on execute, clean up in finally."*

> *"MVC + Virtual Threads for CRUD. WebFlux for streaming. That's the 2026 default."*

> *"CallerRunsPolicy is the only rejection policy that implements true backpressure."*

---

# 🏁 Course Complete!

## 🎉 Congratulations!

You've now covered the **entire L5 concurrency curriculum**:

```
The Complete Journey:
═══════════════════════════════════════════════════════════════

Module 1:  OS & Memory Foundations
           Process/thread, IPC, cache, JMM
           
Module 2:  Locks & AQS
           synchronized, ReentrantLock, AQS internals
           
Module 3:  Lock-Free & Atomics
           CAS, ABA, LongAdder, VarHandle
           
Module 4:  Machine Coding Pack
           BBQ, thread pool, LRU, rate limiter
           
Module 5:  Concurrent Collections
           CHM, blocking queues, CoW, SkipListMap
           
Module 6:  Thread Pools & Async
           TPE sizing, ForkJoin, CompletableFuture
           
Module 7:  Virtual Threads / Loom
           M:N, pinning, downstream exhaustion, ScopedValue
           
Module 8:  Distributed Concurrency
           Redlock, fencing tokens, MVCC, isolation
           
Module 8B: Coordination, Consensus & Events
           Raft, Sagas, Kafka EOS, Outbox pattern
           
Module 8C: Advanced Distributed Appendix
           Spanner, HLC, consistent hashing, CRDTs
           
Module 9:  Production Diagnostics
           Thread dumps, JFR, async-profiler
           
Module 10: Spring Concurrency
           @Async traps, MDC propagation, MVC+VT vs WebFlux
```

---

## 📚 Suggested Review Cadence

```
DAILY (30 min):
┌─────────────────────────────────────────────────────────────┐
│  One machine-coding problem from Module 4                  │
│  From scratch, no notes                                    │
│  BoundedBlockingQueue, ThreadPool, LRU, RateLimiter       │
└─────────────────────────────────────────────────────────────┘

WEEKLY:
┌─────────────────────────────────────────────────────────────┐
│  Redraw one module's mind map on paper                    │
│  Answer all its self-check questions ALOUD                │
│  Teaching is the best learning!                           │
└─────────────────────────────────────────────────────────────┘

BI-WEEKLY:
┌─────────────────────────────────────────────────────────────┐
│  Pick a war story from any module                         │
│  Defend the fix as if in an interview                     │
│  "What happened? Why? How did you fix it?"               │
└─────────────────────────────────────────────────────────────┘

BEFORE THE LOOP:
┌─────────────────────────────────────────────────────────────┐
│  Speed-run the mind maps for:                             │
│  • Module 1 (OS & Memory)                                 │
│  • Module 2 (Locks & AQS)                                 │
│  • Module 3 (Lock-Free)                                   │
│  • Module 6 (Thread Pools)                                │
│  • Module 9 (Diagnostics)                                 │
│                                                             │
│  These are your FUNDAMENTALS!                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Final Words

You've now got the material an **L5 loop actually asks for** — no more and no less.

The difference between knowing this material and *owning* it is practice:
- Write the code
- Draw the diagrams
- Explain it out loud
- Debug real issues

**Good luck! You've got this!** 🚀

---

*"Concurrency is not about threads. It's about correctly managing shared mutable state. Everything else is just implementation details."* 🧵