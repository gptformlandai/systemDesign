# Observability & Operations - Mentorship Track

> Goal: build strong intuition and interview-ready depth for measuring system behavior, debugging production issues, and operating services safely at scale.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `1.9 Observability & Operations`.
- We will follow the same learning style used in the asynchronous-system and reliability notes.
- We will add topics one by one in a repeatable architect-level structure.
- We will include code samples, mini programs, and interview-style answers.

---

## Roadmap for This Sheet

1. Metrics, logs, distributed tracing
2. Liveness vs readiness probes
3. SLIs, SLOs, SLAs
4. Canary deployments
5. Blue-green deployments

---

# Topic 1: Metrics, Logs, and Distributed Tracing

> Track: 1.9 Observability & Operations
> Scope: telemetry signals, latency distributions, correlation IDs, trace spans, and production debugging

---

## 1. Intuition

Think of observability like diagnosing a patient in a busy hospital.

- Metrics are the vital signs.
- Logs are the detailed notes written during treatment.
- Traces show the patient's full journey through departments.

If the patient is unstable, the vital signs tell you that something is wrong. The notes tell you what happened. The journey tells you where time was spent and where the issue started.

In distributed systems, the same idea applies. You need to know:
- is the system healthy?
- what exactly happened?
- where did the request slow down or fail?

Short memory trick:
- metrics tell you something is wrong
- logs tell you what happened
- traces tell you where it happened

---

## 2. Definition

- Definition: Observability is the ability to understand the internal behavior of a system by analyzing the telemetry it emits.
- Category: Operational visibility and production-debugging discipline
- Core idea: Metrics summarize system behavior over time, logs capture detailed events, and distributed traces connect one request across multiple components.

Classic interview framing:
- metrics, logs, and traces are the three core signals
- modern observability may also include profiling and events, but the three-signal model is the right baseline

---

## 3. Why It Exists

Production systems fail in partial, messy ways.

Examples:
- one dependency becomes slow but does not fully fail
- only one endpoint has elevated error rate
- one tenant experiences timeouts while the rest of the fleet looks healthy
- one request crosses six services and the bottleneck is not obvious

Without observability, incident response becomes guesswork.

Teams need observability because they must:
- detect failures quickly
- localize the failing component
- understand why the failure happened
- validate whether a release made things better or worse
- measure user-impacting reliability over time

Simple uptime checks are not enough.

A service can be "up" while users still experience:
- high latency
- partial failures
- queue buildup
- stale data
- degraded dependencies

Observability exists because real systems need evidence, not intuition alone.

---

## 4. Reality

### Metrics, logs, and tracing are common in:

- microservices platforms
- payment systems
- e-commerce checkouts
- search and recommendation stacks
- data pipelines
- Kubernetes-based applications
- multi-region SaaS platforms

### Common tooling examples

- Metrics: Prometheus, CloudWatch, Datadog, Grafana
- Logs: ELK, OpenSearch, Loki, Splunk, CloudWatch Logs
- Tracing: OpenTelemetry, Jaeger, Zipkin, Tempo, X-Ray

### Real-world architecture truth

Collecting telemetry is easy. Making it useful is hard.

Teams often fail because they have:
- too many dashboards and no clear signal
- logs without structure
- metrics with bad labels
- traces that break at service boundaries
- alerts that page constantly but explain nothing

Another important truth:
- observability should not depend on human memory

If the system only becomes diagnosable after an engineer manually adds a temporary log line during an incident, the instrumentation is too weak.

---

## 5. How It Works

At a high level:

1. A request enters the system.
2. The service creates or propagates a trace ID and span context.
3. The service emits metrics for request rate, errors, and latency.
4. The service writes structured logs with request context.
5. The service creates trace spans around internal work and downstream calls.
6. Telemetry is exported to storage and visualization systems.
7. Operators use dashboards, alerts, logs, and traces to investigate behavior.

### Metrics flow

- Counters track totals such as requests, errors, retries, or queue events.
- Gauges track current values such as queue depth, CPU, memory, or active connections.
- Histograms or summaries track latency distributions so teams can reason about p95 and p99.

Metrics answer questions like:
- Is traffic up or down?
- Is error rate increasing?
- Is latency getting worse?

### Logs flow

- The service writes event records when meaningful things happen.
- Good logs are structured, searchable, and contextual.
- Important fields often include service name, trace ID, request ID, endpoint, dependency, status, and error code.

Logs answer questions like:
- What exactly failed?
- Which order, user flow, or dependency was involved?
- What exception or business event occurred?

### Distributed tracing flow

- One request gets a trace ID.
- Each service call creates a child span.
- Spans record duration, status, metadata, and parent-child relationships.
- Trace context is propagated across HTTP, gRPC, messaging, or async boundaries.

Tracing answers questions like:
- Which hop was slow?
- Did latency come from the database, cache, or external API?
- How did this request move across services?

### Failure path

- If the telemetry backend is slow or unavailable, the application should fail open.
- Telemetry export should be buffered, batched, sampled, or dropped before it blocks user requests.
- Observability failure should not become application failure.

### Recovery path

- Exporters retry or reconnect.
- Buffered telemetry drains when the backend recovers.
- Dashboards and alerts resume, and traces become queryable again.

---

## 6. What Problem It Solves

- Primary problem solved: detects, localizes, and explains production failures and performance regressions
- Secondary benefits: capacity planning, release verification, trend analysis, faster incident response, and stronger postmortems
- Systems impact: changes operations from guess-and-check debugging to evidence-driven diagnosis

Observability is what lets a team answer:
- what is broken?
- where is it broken?
- how bad is it?
- is it getting worse?

---

## 7. When to Rely on It

Use strong observability when:
- the system is customer-facing
- multiple services participate in one user request
- latency and error budgets matter
- the team runs on-call rotations
- production changes must be validated safely
- dependencies can fail partially or intermittently

Especially important for:
- microservices
- asynchronous pipelines
- payment flows
- search systems
- SaaS control planes
- high-traffic APIs

Strong interviewer keywords:
- p99 latency
- root cause
- request path
- correlation ID
- dependency bottleneck
- noisy alerting
- incident triage

---

## 8. When Not to Use It

Do not instrument blindly.

Be careful when:
- adding high-cardinality metric labels such as user ID or order ID
- logging secrets, tokens, passwords, or sensitive PII
- tracing every successful request at full fidelity in very high-volume systems
- turning debug logging on globally in a hot code path

Also avoid this misunderstanding:
- more telemetry is not automatically better telemetry

Poor observability can create:
- high cost
- storage pressure
- query slowdown
- alert fatigue
- harder debugging because signal is buried in noise

Better framing:
- use metrics for aggregate health
- use logs for rich event detail
- use traces for request-path analysis
- choose sampling, retention, and cardinality deliberately

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Metrics, logs, and tracing | Fast detection, strong drill-down debugging, SLO measurement, and better release confidence | Storage cost, instrumentation effort, noisy signals if poorly designed, and cardinality or retention trade-offs |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Detail vs cost:
  richer telemetry improves diagnosis but increases ingest, storage, and query cost.
- Aggregation vs precision:
  metrics are cheap and fast to query, but logs and traces hold deeper detail.
- Full-fidelity tracing vs overhead:
  tracing every request helps debugging, but sampling is often needed at scale.
- Flexibility vs safety:
  free-form logs are easy to write, but structured logs are much easier to query reliably.
- Rich dimensions vs cardinality risk:
  labels help analysis until they explode the metric store.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using user IDs as metric labels | Creates unbounded cardinality and can overwhelm the metrics backend | Keep labels bounded; use logs or traces for request-specific identity |
| Relying only on averages | Averages hide tail latency and user pain | Track p50, p95, and p99 latency |
| Writing unstructured logs | Hard to filter, correlate, and automate | Use structured logs with stable fields |
| Forgetting trace propagation across services | Distributed traces break into disconnected fragments | Standardize context propagation using OpenTelemetry or consistent middleware |
| Making telemetry export part of the critical path | Observability outages can harm application availability | Fail open and export asynchronously |
| Logging sensitive data | Creates compliance, security, and privacy risk | Redact or avoid secrets and sensitive identifiers |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Rate, errors, duration:
  the RED method is a strong baseline for request-driven systems.
- p50, p95, p99 latency:
  p95 and p99 are often more useful than averages for user-facing systems.
- Error rate:
  teams often alert on sustained error percentages, not just absolute failures.
- Trace sampling rate:
  high-volume success traffic may be sampled, while error traces are often kept at a much higher rate.
- Log retention:
  hot searchable retention may be days to weeks, while colder archival retention may be much longer.
- Alert evaluation window:
  often a few minutes for urgent regressions and longer for slower-burn saturation issues.
- Cardinality budget:
  metric dimensions should stay bounded and predictable.

Interview shorthand:
- RED metrics, p99, sampling, retention, cardinality, correlation ID

---

## 12. Failure Modes

### High-cardinality metrics

Problem:
- Labels such as session ID, order ID, or user ID create too many unique time series.

User impact:
- dashboards slow down, monitoring cost spikes, and useful metrics become harder to query during incidents

Mitigation:
- keep labels bounded
- move unique identifiers into logs and traces
- review metric cardinality regularly

### Broken trace propagation

Problem:
- One or more services fail to forward trace context.

User impact:
- the slow request is visible only as disconnected fragments, so root cause analysis gets much harder

Mitigation:
- standardize middleware
- use automatic instrumentation where possible
- test trace continuity across sync and async boundaries

### Noisy alerting

Problem:
- alerts fire on low-value signals or transient noise.

User impact:
- operators learn to ignore pages, and real incidents are detected late

Mitigation:
- alert on actionable symptoms
- tie alerts to user impact or SLO risk
- tune thresholds and suppression windows

### Telemetry backend outage

Problem:
- logging, metrics, or tracing infrastructure becomes degraded or unavailable.

User impact:
- teams lose visibility during an incident, which slows diagnosis

Mitigation:
- fail open in the application
- batch and buffer exporters
- monitor the observability stack itself

---

## 13. Scenario

- Product / system: Checkout platform with API gateway, checkout service, payment service, inventory service, and PostgreSQL
- Requirement:
  on-call engineers must quickly diagnose intermittent checkout latency spikes and payment failures
- Good design:
  RED metrics on every service and dependency, structured logs with trace IDs and order IDs, and distributed traces across API, payment, inventory, and database calls
- Why this concept fits:
  the failure path crosses service boundaries, so no single service log is enough
- What would go wrong without it:
  engineers would know checkout is slow, but not whether the cause is payment latency, database saturation, or queue buildup

---

## 14. Code Sample

### Emitting all three signals from one request path

```java
public OrderResponse placeOrder(OrderRequest request) {
    Span span = tracer.spanBuilder("checkout.place_order").startSpan();
    long startNanos = System.nanoTime();

    try (Scope ignored = span.makeCurrent()) {
        String traceId = span.getSpanContext().getTraceId();

        logger.info("event=place_order_start traceId={} orderId={}",
                traceId,
                request.orderId());

        OrderResponse response = paymentClient.charge(request);

        meterRegistry.counter("checkout.requests.total", "outcome", "success").increment();
        return response;
    } catch (Exception ex) {
        meterRegistry.counter("checkout.requests.total", "outcome", "error").increment();
        span.recordException(ex);
        span.setStatus(StatusCode.ERROR);

        logger.error("event=place_order_failed traceId={} orderId={} message={}",
                span.getSpanContext().getTraceId(),
                request.orderId(),
                ex.getMessage());
        throw ex;
    } finally {
        meterRegistry.timer("checkout.request.latency")
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        span.end();
    }
}
```

Key idea:
- one request should emit aggregate health signals, detailed event records, and end-to-end request context

---

## 15. Mini Program / Simulation

This mini program shows one request producing logs, metrics, and a simple trace.

```python
import time
from collections import Counter, defaultdict
from uuid import uuid4


metrics = Counter()
latencies = defaultdict(list)


def log(level: str, event: str, **fields) -> None:
    record = {"level": level, "event": event, **fields}
    print(record)


class Span:
    def __init__(self, trace_id: str, name: str, parent: str | None = None) -> None:
        self.trace_id = trace_id
        self.name = name
        self.parent = parent
        self.start = time.perf_counter()
        log("INFO", "span_start", traceId=trace_id, span=name, parent=parent)

    def finish(self, error: str | None = None) -> None:
        duration_ms = round((time.perf_counter() - self.start) * 1000, 2)
        metrics[f"{self.name}.count"] += 1
        latencies[self.name].append(duration_ms)

        if error:
            metrics[f"{self.name}.errors"] += 1

        log(
            "ERROR" if error else "INFO",
            "span_finish",
            traceId=self.trace_id,
            span=self.name,
            durationMs=duration_ms,
            error=error,
        )


def inventory_call(trace_id: str) -> None:
    span = Span(trace_id, "inventory.reserve", parent="checkout.place_order")
    time.sleep(0.02)
    span.finish()


def payment_call(trace_id: str) -> None:
    span = Span(trace_id, "payment.charge", parent="checkout.place_order")
    time.sleep(0.08)
    span.finish(error="gateway_timeout")


def place_order(order_id: str) -> None:
    trace_id = str(uuid4())
    root = Span(trace_id, "checkout.place_order")

    log("INFO", "request_start", traceId=trace_id, orderId=order_id)
    metrics["checkout.requests.total"] += 1

    inventory_call(trace_id)
    payment_call(trace_id)

    metrics["checkout.requests.error"] += 1
    log("ERROR", "request_failed", traceId=trace_id, orderId=order_id, reason="gateway_timeout")
    root.finish(error="gateway_timeout")


def main() -> None:
    place_order("order-123")

    print("\nMetrics summary")
    for name, value in metrics.items():
        print(name, value)

    print("\nLatency summary")
    for name, values in latencies.items():
        print(name, values)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- the same trace ID ties together events across spans
- metrics summarize what happened
- logs hold contextual detail for the failing request
- traces expose where latency and failure occurred

---

## 16. Practical Question

> You are designing a checkout platform with an API gateway, checkout service, payment service, inventory service, and database. How would you instrument metrics, logs, and distributed tracing so the on-call team can diagnose a p99 latency spike quickly?

---

## 17. Strong Answer

I would absolutely instrument all three signals, because a latency spike across multiple services is exactly the kind of problem that one signal alone explains poorly.

I would start with metrics using the RED model at each service boundary: request rate, error rate, and latency distributions by endpoint and by dependency. That gives the team fast detection and lets them see whether the spike is isolated to checkout, payment, database access, or a specific external dependency.

Then I would add structured logs with stable fields such as service name, trace ID, request ID, endpoint, dependency, status code, and business-safe identifiers like order ID. I would avoid putting unbounded identifiers into metrics, and I would avoid logging secrets or sensitive payloads.

For distributed tracing, I would propagate trace context end to end using standard instrumentation such as OpenTelemetry. Each service hop and dependency call would produce spans so the team can see whether time is being spent in application logic, a database query, a remote payment call, or queue wait.

At scale, I would sample successful traces but keep a much higher rate for errors and slow requests. If the system were still small and mostly monolithic, I might start with strong metrics and structured logs first, then add full tracing once request paths become more distributed.

---

## 18. Revision Notes

- One-line summary: Observability turns production debugging from guesswork into evidence by combining metrics for trends, logs for events, and traces for request flow.
- Three keywords: RED, correlation ID, spans
- One interview trap: putting unbounded values like user ID into metric labels
- One memory trick: metrics tell you something is wrong, logs tell you what happened, traces tell you where it happened

---

# Topic 2: Liveness vs Readiness Probes

> Track: 1.9 Observability & Operations
> Scope: health signaling, container lifecycle, traffic routing, restart behavior, and rollout safety

---

## 1. Intuition

Think of a restaurant kitchen.

- Liveness asks: is the kitchen staff still functioning at all, or has the kitchen effectively stopped working?
- Readiness asks: even if the kitchen is alive, is it ready to accept new orders right now?

A kitchen may be alive but temporarily not ready because it is still opening, restocking, or cleaning after a problem. In that case, you should not shut the building down. You should just stop seating new customers there for a while.

That is the core difference:
- liveness decides whether the process should be restarted
- readiness decides whether traffic should be sent to it

Short memory trick:
- liveness = restart me if I am stuck
- readiness = do not send me traffic yet

---

## 2. Definition

- Definition: A liveness probe checks whether an application is still running in a recoverable state or has become stuck badly enough that a restart is appropriate.
- Definition: A readiness probe checks whether an application is currently able to serve traffic safely.
- Category: Health signaling and workload orchestration mechanism
- Core idea: Separate "should this instance be restarted?" from "should this instance receive traffic?"

Interview shortcut:
- liveness protects against hung or wedged processes
- readiness protects users from being routed to instances that are not prepared to serve

---

## 3. Why It Exists

Distributed systems do not fail in just one way.

An instance may be:
- totally wedged because of deadlock or event-loop starvation
- still booting and warming caches
- draining during rollout
- temporarily disconnected from a required dependency
- overloaded and intentionally refusing new work

If you treat all of those states the same way, bad things happen.

Without separate probes:
- traffic may hit cold or half-started instances
- orchestrators may restart healthy-but-not-ready instances unnecessarily
- rolling deployments may route users to pods before startup is complete
- dependency blips may trigger destructive restart loops

These probes exist because the platform needs different responses for different failure modes:
- restart a truly broken process
- remove a temporarily unready instance from load balancing

---

## 4. Reality

### Liveness and readiness probes are common in:

- Kubernetes deployments
- service mesh environments
- ECS and containerized platforms with health checks
- API services behind load balancers
- Spring Boot, Node.js, Go, and gRPC services
- systems with rolling deployments or autoscaling

### Real-world architecture truth

Readiness usually matters more often than liveness.

Many incidents come from traffic being sent to instances that are:
- still starting
- still warming caches
- still establishing database pools
- intentionally draining for deployment

Another important truth:
- liveness probes are often overused

Teams frequently point liveness at remote dependencies like the database or Redis. That is usually a mistake. If the database is down, restarting every pod rarely fixes the real issue. It often makes the outage worse by causing restart storms.

Operationally mature systems often use:
- readiness for dependency availability and traffic acceptance
- liveness for local process health only
- startup probes for slow boot paths so liveness does not kill the app too early

---

## 5. How It Works

At a high level:

1. The orchestrator starts the application container.
2. The platform periodically calls the configured health endpoints or checks.
3. If the readiness probe passes, the instance is added to the traffic-serving pool.
4. If the readiness probe fails later, the instance stays running but is removed from traffic.
5. If the liveness probe fails repeatedly, the platform restarts the container.

### Liveness flow

- The check should verify that the process is not fundamentally stuck.
- Typical signals are local process health, deadlock detection, event-loop responsiveness, or critical in-process state.
- On repeated failure, the orchestrator restarts the container.

Liveness answers:
- is this instance so unhealthy that restart is the correct recovery action?

### Readiness flow

- The check verifies whether the instance can serve requests correctly right now.
- Typical signals include startup completion, config loaded, listener active, dependency pool available, or drain mode disabled.
- On failure, the instance is removed from service discovery or load-balancer targets, but it is not restarted solely because of readiness failure.

Readiness answers:
- should this instance receive user traffic at this moment?

### Why startup probes matter

- Some services are slow to start.
- Without a startup probe, aggressive liveness checks may kill the service before it has fully initialized.
- A startup probe gives the application time to boot before liveness enforcement becomes strict.

### Failure path

- If a pod is alive but not ready, traffic should shift away from it.
- If a pod is deadlocked or unresponsive locally, liveness failure should trigger restart.
- If both probes point to the same expensive remote check, the system can flap badly.

### Recovery path

- Once readiness succeeds again, the instance re-enters the traffic pool.
- Once the application restarts and becomes healthy, liveness passes and readiness eventually follows.

---

## 6. What Problem It Solves

- Primary problem solved by liveness: detects instances that are stuck badly enough that restart is the right recovery action
- Primary problem solved by readiness: prevents traffic from being sent to instances that cannot safely serve requests yet or anymore
- Secondary benefits: safer rollouts, cleaner draining, lower user-visible error rates, and better autoscaling behavior
- Systems impact: separates traffic-routing health from restart health so the platform can respond proportionally to failure

This topic solves two different questions:
- should this instance be restarted?
- should this instance get traffic?

---

## 7. When to Rely on It

Use liveness and readiness probes when:
- the service runs under an orchestrator such as Kubernetes
- instances take time to warm up before serving safely
- rolling deploys must avoid sending traffic too early
- dependencies can degrade temporarily without requiring restart
- drain and shutdown behavior matters

Readiness is especially valuable for:
- startup warmup
- cache loading
- connection-pool establishment
- maintenance mode
- deployment draining
- brownout or overload protection

Liveness is especially valuable for:
- deadlocks
- event-loop stalls
- fatal internal state corruption
- workers that stop making forward progress

Strong interviewer keywords:
- rolling update
- draining
- warmup
- restart loop
- health endpoint
- service endpoints
- startup probe

---

## 8. When Not to Use It

Do not use probes carelessly.

Avoid these patterns:
- pointing liveness at a remote dependency that commonly has transient failures
- making probe handlers expensive or slow
- using the exact same logic for readiness and liveness by default
- returning ready before the application can really serve traffic
- adding deep correctness checks that make the probe itself a bottleneck

Also be careful when:
- the application is so simple that a trivial process check is enough
- the platform already has a different health-routing mechanism and duplicated logic would create confusion

Better framing:
- liveness should be local, cheap, and restart-oriented
- readiness should reflect traffic-serving ability, not theoretical perfection

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Liveness and readiness probes | Safer traffic routing, fewer bad rollouts, automatic recovery from stuck processes, and clearer instance state | Misconfiguration can cause restart storms, traffic flapping, false failures, and dependency-coupled outages |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Safety vs sensitivity:
  stricter probes catch issues faster, but overly sensitive thresholds can flap under normal jitter.
- Fast failover vs stability:
  removing pods quickly protects users, but too-fast reactions can shrink capacity during brief dependency hiccups.
- Rich health logic vs simplicity:
  more checks improve confidence, but complicated probe code becomes slower and harder to trust.
- Restart automation vs incident amplification:
  automatic restart is helpful for a wedged process, but harmful when the root problem is external.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using the database in liveness | DB outages should usually stop traffic, not force every pod to restart | Put DB dependency in readiness, keep liveness local |
| Returning ready before warmup completes | Users hit cold instances and see failures or latency spikes | Gate readiness on actual startup completion |
| Reusing the same endpoint for both probes | The platform cannot distinguish restart-worthy failure from traffic-unready state | Model the two decisions separately |
| Probe intervals too aggressive | Normal spikes or GC pauses can trigger false failures | Tune timeout, period, and thresholds to application behavior |
| No startup probe for slow boots | Liveness kills the process before startup completes | Use startup probe to protect long initialization |
| Readiness tied to every optional dependency | One non-critical enrichment service can take the whole pod out of rotation | Only gate on dependencies required for core traffic |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- periodSeconds:
  how often the platform checks health
- timeoutSeconds:
  how long the probe waits before counting failure
- failureThreshold:
  how many consecutive failures trigger the platform action
- successThreshold:
  useful for readiness in some environments before re-entering service
- initialDelaySeconds:
  startup delay before probing begins
- startupProbe window:
  total boot grace is roughly `failureThreshold * periodSeconds`
- drain time:
  should align with readiness removal, in-flight request completion, and termination grace

Interview shorthand:
- cheap local liveness, dependency-aware readiness, startup probe, failureThreshold, drain safety

---

## 12. Failure Modes

### Restart storm

Problem:
- Liveness depends on a remote system like the database. The database has a short outage, so every pod starts failing liveness and restarting.

User impact:
- outage becomes wider and recovery is slower because healthy application processes keep getting killed

Mitigation:
- keep liveness local
- move remote dependency checks to readiness
- use startup probes for slow initialization

### Traffic to cold pods

Problem:
- Readiness returns success before caches, model files, or connection pools are ready.

User impact:
- new pods receive traffic too early, causing latency spikes and rollout errors

Mitigation:
- gate readiness on true serving ability
- test rollout behavior under cold start

### Readiness flapping

Problem:
- The readiness signal toggles rapidly because thresholds are too tight or a dependency is unstable.

User impact:
- pods repeatedly enter and leave service, creating uneven capacity and unpredictable latency

Mitigation:
- tune thresholds
- add hysteresis where supported
- avoid coupling readiness to noisy optional dependencies

### Slow-start kill loop

Problem:
- The app needs 45 seconds to initialize, but liveness starts failing after 10 seconds.

User impact:
- the pod never becomes healthy, so deploys stall and capacity drops

Mitigation:
- use startup probe
- align probe timings with real boot behavior

---

## 13. Scenario

- Product / system: Checkout API running on Kubernetes
- Requirement:
  new pods must not receive traffic until configuration is loaded, caches are warmed, and the database pool is usable; truly stuck pods should restart automatically
- Good design:
  a lightweight local liveness probe for JVM forward progress, a readiness probe that stays false during warmup or drain mode, and a startup probe that protects long initialization
- Why this concept fits:
  the platform needs different actions for "not ready yet" and "must be restarted"
- What would go wrong without it:
  either users would hit half-initialized pods, or transient dependency issues would trigger destructive restart loops

---

## 14. Code Sample

### Separate live and ready decisions

```java
@RestController
public class HealthController {

    private final AtomicBoolean eventLoopResponsive = new AtomicBoolean(true);
    private final AtomicBoolean cacheWarmed = new AtomicBoolean(false);
    private final AtomicBoolean drainMode = new AtomicBoolean(false);
    private final DataSourceHealth dataSourceHealth;

    public HealthController(DataSourceHealth dataSourceHealth) {
        this.dataSourceHealth = dataSourceHealth;
    }

    @GetMapping("/health/live")
    public ResponseEntity<String> liveness() {
        if (!eventLoopResponsive.get()) {
            return ResponseEntity.status(500).body("stuck");
        }
        return ResponseEntity.ok("alive");
    }

    @GetMapping("/health/ready")
    public ResponseEntity<String> readiness() {
        boolean ready = cacheWarmed.get()
                && !drainMode.get()
                && dataSourceHealth.canServeRequests();

        if (!ready) {
            return ResponseEntity.status(503).body("not-ready");
        }
        return ResponseEntity.ok("ready");
    }
}
```

Key idea:
- liveness checks whether restart is appropriate
- readiness checks whether traffic is appropriate
- they are related, but they are not the same question

---

## 15. Mini Program / Simulation

This mini program shows the difference between removing a pod from traffic and restarting it.

```python
class Pod:
    def __init__(self) -> None:
        self.started = False
        self.cache_warmed = False
        self.db_available = True
        self.deadlocked = False
        self.restart_count = 0

    def liveness(self) -> bool:
        return not self.deadlocked

    def readiness(self) -> bool:
        return self.started and self.cache_warmed and self.db_available

    def restart(self) -> None:
        self.restart_count += 1
        print(f"restart #{self.restart_count}")
        self.started = False
        self.cache_warmed = False
        self.deadlocked = False


def route_traffic(pod: Pod) -> None:
    if pod.readiness():
        print("router: sending traffic to pod")
    else:
        print("router: pod removed from traffic")


def kubelet_check(pod: Pod) -> None:
    if pod.liveness():
        print("kubelet: pod is live")
    else:
        print("kubelet: pod failed liveness, restarting")
        pod.restart()


def main() -> None:
    pod = Pod()

    print("booting")
    pod.started = True
    route_traffic(pod)
    kubelet_check(pod)

    print("warming cache")
    pod.cache_warmed = True
    route_traffic(pod)

    print("database outage")
    pod.db_available = False
    route_traffic(pod)
    kubelet_check(pod)

    print("database recovers")
    pod.db_available = True
    route_traffic(pod)

    print("event loop deadlocks")
    pod.deadlocked = True
    route_traffic(pod)
    kubelet_check(pod)
    route_traffic(pod)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- a pod can be alive but not ready
- readiness failure removes traffic without forcing restart
- liveness failure triggers restart because forward progress is gone

---

## 16. Practical Question

> You are deploying a checkout service on Kubernetes. The service takes 30 seconds to warm caches and build database pools, and sometimes a downstream database has short-lived connectivity issues. How would you design liveness and readiness probes so rollouts stay safe without causing restart storms?

---

## 17. Strong Answer

I would explicitly separate restart health from traffic health. For liveness, I would keep the check local and cheap, focused on whether the process is making forward progress. I would not make liveness depend on the database or another remote dependency, because that turns an external outage into a restart storm.

For readiness, I would gate traffic on whether the service can actually handle requests right now. That would include startup completion, cache warmup, and the database pool being usable if the database is truly part of the core request path. If the service is draining during a rollout, readiness should also go false so the load balancer stops sending new traffic.

Because the service needs about 30 seconds to initialize, I would also add a startup probe or equivalent grace window so liveness does not kill the process during normal boot. I would tune probe intervals and thresholds based on real startup time and dependency jitter rather than defaults. The main principle is simple: not-ready pods should stop receiving traffic, but only truly stuck pods should be restarted.

---

## 18. Revision Notes

- One-line summary: Liveness decides whether an instance should restart, while readiness decides whether it should receive traffic.
- Three keywords: restart, traffic, startup
- One interview trap: using a remote dependency in liveness and triggering restart storms
- One memory trick: alive is not the same as ready

---

# Topic 3: SLIs, SLOs, and SLAs

> Track: 1.9 Observability & Operations
> Scope: user-centric reliability measurement, target setting, error budgets, alerting, and external service commitments

---

## 1. Intuition

Think of an airline.

- The SLI is the measured fact: how many flights actually departed on time this month.
- The SLO is the internal target: we want 99.5% of flights to depart within 15 minutes of schedule.
- The SLA is the external promise: if we fail badly enough, customers may get compensation or refunds.

That is the relationship:
- SLI tells you what is happening
- SLO tells you what you are aiming for
- SLA tells customers what you are contractually promising

Short memory trick:
- indicator = measurement
- objective = target
- agreement = contract

---

## 2. Definition

- Definition: An SLI, or service level indicator, is a quantitative measure of service behavior such as availability, latency, correctness, or freshness.
- Definition: An SLO, or service level objective, is the target value or acceptable threshold for one or more SLIs over a defined time window.
- Definition: An SLA, or service level agreement, is an external commitment to customers or partners that often includes consequences if the commitment is missed.
- Category: Reliability measurement and service-governance framework
- Core idea: Measure user-visible quality, define a target, and separate internal engineering goals from external contractual promises.

Interview shortcut:
- SLI = what you measure
- SLO = what you target
- SLA = what you promise

---

## 3. Why It Exists

Teams need a precise way to answer a deceptively simple question:
- is the service reliable enough for users?

Without SLI and SLO thinking, teams fall into bad patterns such as:
- chasing infrastructure metrics like CPU without knowing whether users are actually impacted
- treating uptime alone as the whole reliability story
- arguing about priorities without a shared reliability target
- alerting on noise instead of real user harm

SLAs exist because businesses also need a customer-facing reliability contract.

Without the distinction between SLO and SLA:
- engineering may over-promise externally
- product teams may not know how much reliability investment is enough
- incident response may lack a clear severity lens
- release decisions may ignore reliability debt until customers complain

These concepts exist to connect operations, engineering, and business language around one measurable reality.

---

## 4. Reality

### SLIs, SLOs, and SLAs are common in:

- public APIs
- SaaS platforms
- payments and checkout systems
- search and booking platforms
- internal platform teams serving many other teams
- regulated or enterprise products with contractual reliability expectations

### Common SLI categories

- availability: did the request succeed?
- latency: was the request fast enough?
- correctness: was the response valid?
- freshness: is the data recent enough?
- durability: was the data preserved?

### Real-world architecture truth

Good teams do not create dozens of vanity SLOs.

They pick a small number of user-meaningful journeys such as:
- search results returned successfully
- booking confirmed successfully
- payment processed within latency target
- streaming event delivered within freshness window

Another important truth:
- internal SLOs are usually stricter than external SLAs

If the public SLA is tighter than the internal engineering target, the team is effectively promising customers more than it is managing internally. That is backwards.

Also:
- uptime alone is often a weak SLI

A service can be technically up while users still suffer from:
- slow responses
- stale data
- partial failures
- broken write paths

---

## 5. How It Works

At a high level:

1. Pick an important user journey such as search, booking, or payment.
2. Define what counts as a good event and a bad event.
3. Emit telemetry that can measure those events accurately.
4. Compute the SLI over a fixed window such as 5 minutes, 1 hour, or 30 days.
5. Compare the measured SLI against the SLO target.
6. Track remaining error budget and use it to guide alerting and release decisions.
7. Define any customer-facing SLA separately, usually with looser terms and explicit remedies.

### SLI flow

- Choose the behavior that matters to users.
- Define numerator and denominator clearly.
- Example:
  good booking confirmations / total valid booking attempts

SLIs answer:
- how is the service actually behaving for users?

### SLO flow

- Choose a target and a time window.
- Example:
  99.95% of booking confirmations succeed over a rolling 30-day window
- The allowed failure fraction becomes the error budget.

SLOs answer:
- what reliability level are we trying to maintain?
- how much failure can we tolerate before we should slow down or change behavior?

### SLA flow

- Translate customer expectations into a formal commitment.
- Include scope, exclusions, measurement method, reporting window, and consequences.
- Example:
  monthly API availability of 99.9%, otherwise service credits apply

SLAs answer:
- what are we promising externally, and what happens if we miss it?

### Error budget idea

- If the SLO is 99.9%, the error budget is 0.1% over the window.
- Teams can spend that budget on failures, risky releases, or known instability.
- If the budget burns too quickly, the team should reduce risk and prioritize reliability work.

### Failure path

- If the SLI drops below target, the error budget is consumed.
- Alerting should focus on meaningful burn rate, not every small blip.
- Teams may pause releases, shift traffic, roll back changes, or fix the highest-impact dependency.

### Recovery path

- Stabilize the failing user journey.
- Confirm the SLI has recovered.
- Slow the burn rate back to normal.
- Use postmortems to refine the SLI definition, SLO target, or alert policy if they were poorly chosen.

---

## 6. What Problem It Solves

- Primary problem solved: gives teams a user-centered, measurable way to define and manage reliability
- Secondary benefits: better alerting, clearer prioritization, healthier release decisions, and stronger business alignment
- Systems impact: shifts operations from vague uptime discussions to explicit reliability targets with measurable error budgets

This topic answers three different questions:
- what are users actually experiencing?
- what target should engineering hold itself to?
- what promise is the business making outside the company?

---

## 7. When to Rely on It

Use SLIs and SLOs when:
- the service is user-facing or mission-critical
- multiple teams need a shared reliability target
- on-call engineers need clear alerting based on user impact
- release velocity must be balanced against stability
- reliability trade-offs need a quantitative framework

Use SLAs when:
- you have enterprise customers or contractual uptime obligations
- your API or platform is sold as a managed service
- the business needs formal terms around reliability and remedies

Especially valuable for:
- booking platforms
- payment systems
- search APIs
- internal platform teams
- high-volume microservices
- multi-tenant SaaS systems

Strong interviewer keywords:
- error budget
- rolling window
- burn rate
- user journey
- availability target
- latency objective
- contractual commitment

---

## 8. When Not to Use It

Do not create SLOs blindly.

Be careful when:
- the metric is not user-visible or business-relevant
- the measurement is too noisy or poorly instrumented to trust
- the service is small and internal enough that lightweight health checks are sufficient
- teams are choosing targets without operational history or baseline data

Avoid these patterns:
- defining an SLO on CPU usage instead of user experience
- setting dozens of overlapping SLOs nobody acts on
- making the target 100% with no realistic error budget
- using the SLA as the primary engineering target

Better framing:
- choose a few meaningful SLIs
- make SLOs actionable
- keep SLAs external and conservative

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| SLIs, SLOs, and SLAs | Create shared reliability language, improve alerting, and make release risk measurable | Poor metric choice leads to false confidence, target setting can become political, and contractual promises add business risk |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Precision vs simplicity:
  a richer SLI captures reality better, but it is harder to measure and explain.
- Ambition vs sustainability:
  stricter SLOs improve user experience, but demand more engineering investment and slower change velocity.
- Fast alerting vs alert fatigue:
  very sensitive thresholds detect problems quickly, but noisy policies burn out the on-call team.
- External trust vs business exposure:
  a stronger SLA can help sales, but increases financial and reputational downside when incidents happen.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using infrastructure metrics as the primary SLI | CPU, memory, or pod count are weak proxies for user experience | Define SLIs around success, latency, freshness, or correctness of real requests |
| Setting the SLO to 100% | Real systems need room for partial failure and controlled risk-taking | Use a realistic target and manage the error budget explicitly |
| Making the SLA tighter than the SLO | The business promises more than engineering is targeting internally | Keep the internal SLO stricter than the external SLA |
| Not defining good and bad events clearly | Teams compute different numbers from the same service | Document numerator, denominator, exclusions, and window precisely |
| Alerting on every breach instantly | Small transient dips create noise without business action | Alert on burn rate and sustained user impact |
| Choosing too many SLOs | Nobody remembers them or changes behavior because of them | Focus on a few critical user journeys |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- 99.9% availability:
  about 43.2 minutes of monthly unavailability
- 99.95% availability:
  about 21.6 minutes of monthly unavailability
- 99.99% availability:
  about 4.32 minutes of monthly unavailability
- Error budget:
  if the SLO is 99.9%, the error budget is 0.1% over the chosen window
- Common windows:
  28-day or 30-day rolling windows are common because they smooth noise and align with operational review cycles
- Latency SLO example:
  95% of search requests under 300 ms or 99% under 800 ms, depending on the product and user expectation
- Burn rate:
  current bad-event rate divided by the allowed bad-event rate; high burn rate means the team will exhaust the error budget quickly

Interview shorthand:
- indicator, objective, agreement, rolling 30 days, error budget, burn rate

---

## 12. Failure Modes

### Wrong SLI definition

Problem:
- The team measures load balancer success codes, but users actually fail later in the booking workflow.

User impact:
- dashboards look healthy while customers still cannot complete the journey

Mitigation:
- define the SLI at the point closest to user-visible success
- use end-to-end journey metrics for critical flows

### Unrealistic SLO

Problem:
- Leadership chooses 99.999% reliability without funding the engineering work required to support it.

User impact:
- teams miss targets continuously, alerts become normal, and the SLO stops guiding decisions

Mitigation:
- baseline real performance first
- set targets that are ambitious but achievable
- raise the target only when the system and organization are ready

### Burn-rate blindness

Problem:
- The team watches the monthly SLI but ignores the fact that the budget is being exhausted rapidly in the last hour.

User impact:
- incidents escalate before anyone responds because the average still looks acceptable for a while

Mitigation:
- alert on short-window and long-window burn rates
- combine immediate detection with slower trend monitoring

### SLA mismatch

Problem:
- The public SLA counts only total uptime, while the internal SLO is based on user-facing latency and correctness.

User impact:
- customers feel pain that is not reflected in the formal commitment

Mitigation:
- align SLA language with what customers actually experience
- keep internal SLOs more detailed than the public contract

---

## 13. Scenario

- Product / system: Hotel search and booking platform with search, pricing, reservation, and payment services
- Requirement:
  search should stay fast during peak traffic, and booking confirmation should remain highly reliable because failed bookings directly affect revenue
- Good design:
  define separate SLIs for search latency and booking success, set 30-day SLOs with explicit error budgets, and keep the public partner SLA looser than the internal engineering targets
- Why this concept fits:
  the team needs a shared reliability language that maps directly to user experience and business impact
- What would go wrong without it:
  teams would debate reliability based on infrastructure symptoms rather than measured customer-facing outcomes

---

## 14. Code Sample

### Evaluating an availability and latency SLO from service counters

```java
public record SloStatus(
        double availabilitySli,
        double latencySli,
        double errorBudgetRemaining,
        boolean withinObjective) {
}

public class BookingSloEvaluator {

    private static final double AVAILABILITY_TARGET = 0.999;
    private static final double LATENCY_TARGET = 0.95;

    public SloStatus evaluate(long totalRequests, long failedRequests, long fastRequests) {
        if (totalRequests == 0) {
            return new SloStatus(1.0, 1.0, 1.0, true);
        }

        double availabilitySli = 1.0 - ((double) failedRequests / totalRequests);
        double latencySli = (double) fastRequests / totalRequests;
        double errorBudgetRemaining = Math.max(0.0, (availabilitySli - AVAILABILITY_TARGET) / (1.0 - AVAILABILITY_TARGET));
        boolean withinObjective = availabilitySli >= AVAILABILITY_TARGET && latencySli >= LATENCY_TARGET;

        return new SloStatus(availabilitySli, latencySli, errorBudgetRemaining, withinObjective);
    }
}
```

Key idea:
- measure real request outcomes first, then compare them against explicit objectives instead of guessing from infrastructure noise

---

## 15. Mini Program / Simulation

This mini program simulates request results and shows the difference between measured behavior, an internal target, and an external promise.

```python
from dataclasses import dataclass


@dataclass
class Event:
    success: bool
    latency_ms: int


def compute_sli(events: list[Event], latency_threshold_ms: int) -> tuple[float, float]:
    total = len(events)
    success_count = sum(1 for event in events if event.success)
    fast_count = sum(1 for event in events if event.success and event.latency_ms <= latency_threshold_ms)

    availability_sli = success_count / total
    latency_sli = fast_count / total
    return availability_sli, latency_sli


def main() -> None:
    events = [
        Event(True, 180),
        Event(True, 220),
        Event(True, 260),
        Event(False, 900),
        Event(True, 320),
        Event(True, 190),
        Event(False, 700),
        Event(True, 210),
        Event(True, 280),
        Event(True, 240),
    ]

    availability_sli, latency_sli = compute_sli(events, latency_threshold_ms=300)

    availability_slo = 0.90
    latency_slo = 0.80
    availability_sla = 0.85

    print(f"availability SLI: {availability_sli:.2%}")
    print(f"latency SLI:      {latency_sli:.2%}")
    print(f"meets SLO:        {availability_sli >= availability_slo and latency_sli >= latency_slo}")
    print(f"meets SLA:        {availability_sli >= availability_sla}")
    print(f"error budget used: {(1 - availability_sli) / (1 - availability_slo):.2f}x of allowed budget")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- the SLI is the measured result from actual events
- the SLO is the internal threshold the team is trying to maintain
- the SLA is a separate external promise that may be looser than the SLO
- the error budget shows how quickly reliability risk is being consumed

---

## 16. Practical Question

> You are designing a hotel booking platform. Search traffic is high volume, and booking confirmation failures are revenue critical. How would you define SLIs, SLOs, and any public SLA so the team can alert properly and make sane release decisions?

---

## 17. Strong Answer

I would start from the user journeys, not from server metrics. For this system, I would define at least two core SLIs: booking confirmation success rate and search latency. Those are directly tied to customer experience and revenue.

Then I would set SLOs over a rolling window, for example 99.95% successful booking confirmations over 30 days and a latency objective such as 95% of search requests under a chosen threshold. From those SLOs, I would derive error budgets and use burn-rate based alerting so the team is paged when the budget is being consumed fast, not just when a graph wiggles briefly.

If the platform has external customers or partners, I would define an SLA separately. That SLA would usually be looser and simpler than the internal SLO, with clear scope and remedies such as credits. Internally, the SLO should be the tool that governs engineering behavior: if the error budget is healthy, the team can move faster; if it is being burned aggressively, releases should slow down until reliability recovers.

---

## 18. Revision Notes

- One-line summary: SLI measures actual service quality, SLO sets the target, and SLA defines the external promise.
- Three keywords: error budget, burn rate, user journey
- One interview trap: using infrastructure health as the primary SLI instead of user-visible outcomes
- One memory trick: measure, target, promise

---

# Topic 4: Canary Deployments

> Track: 1.9 Observability & Operations
> Scope: progressive traffic shifting, guardrail metrics, rollback safety, cohort selection, and release risk reduction

---

## 1. Intuition

Think of introducing a new menu item in a large hotel restaurant.

- You do not serve it to every guest in every dining room at once.
- You first offer it to a small table section, watch how it performs, check for complaints, and confirm the kitchen can handle it.
- If the result is good, you expand to more tables.
- If the result is bad, you stop quickly before the whole restaurant is affected.

That is what a canary deployment does.

- It sends a small portion of production traffic to the new version.
- It compares the new version against the stable one using real signals.
- It expands only if the new version behaves safely.

Short memory trick:
- small traffic first
- watch real production behavior
- promote or rollback fast

---

## 2. Definition

- Definition: A canary deployment is a release strategy in which a new version is exposed to a small subset of production traffic before wider rollout.
- Category: Progressive delivery and deployment risk-control mechanism
- Core idea: Limit blast radius by validating a new version under real traffic with measurable guardrails before sending it to everyone.

Interview shortcut:
- canary means gradual exposure in production
- rollout decisions depend on observed health, not just on successful deployment

---

## 3. Why It Exists

Many failures only appear under real production conditions.

Examples:
- a dependency behaves differently under true production concurrency
- one endpoint has a serialization bug that tests missed
- a JVM memory pattern looks fine in staging but fails with production data shape
- one downstream partner API has a subtle compatibility issue with the new version

If you do an all-at-once rollout, the whole fleet inherits the risk immediately.

Without canary deployments:
- one bad release can become a full outage
- rollback happens only after broad customer impact
- operators lose the chance to compare old and new versions side by side
- deployments become high-stress events instead of controlled experiments

Canary exists because production validation should happen gradually, not all at once.

---

## 4. Reality

### Canary deployments are common in:

- Kubernetes-based microservices
- service mesh environments such as Istio or Linkerd
- API gateways and load balancers that support weighted routing
- high-traffic SaaS platforms
- mobile and web backends with frequent releases
- teams practicing progressive delivery

### Common implementation mechanisms

- weighted traffic split at the load balancer
- service mesh routing rules
- ingress controller annotations
- header-based or cookie-based cohort routing
- feature-flag platforms combined with version routing

### Real-world architecture truth

Canary without observability is mostly theater.

If the team cannot compare:
- error rate
- latency distributions
- saturation
- business success metrics

then the rollout is gradual, but not actually safe.

Another important truth:
- canary is strongest when traffic volume is high enough to make results meaningful

If the service gets very low traffic, a 1% canary may produce too little signal. In that case, the team may need longer bake times, synthetic checks, or a different rollout strategy.

Also:
- stateless services are easier to canary than schema-coupled or stateful changes

If a release changes storage format or request semantics incompatibly, routing only some traffic to the new version may not be enough on its own.

---

## 5. How It Works

At a high level:

1. Deploy the new version alongside the stable version.
2. Route a small percentage of traffic, such as 1% or 5%, to the new version.
3. Compare canary behavior against baseline guardrails such as error rate, p95 latency, resource saturation, and key business outcomes.
4. Keep the canary running for a bake window long enough to observe real behavior.
5. If the metrics stay healthy, increase traffic gradually.
6. If the metrics regress, stop promotion and roll back quickly.
7. Once confidence is high, promote the new version to 100% and retire the old one.

### Traffic-routing flow

- Two versions run at the same time.
- Routing logic decides what percentage or cohort reaches the canary.
- Cohorts may be random, geography-based, tenant-based, or employee-only.

Canary routing answers:
- who should receive the new version first?
- how much risk are we exposing at this step?

### Guardrail flow

- Measure request success rate, latency, dependency errors, CPU or memory trends, and business KPIs.
- Compare new version and stable version over the same period.
- Use thresholds or automated analysis to decide promote, hold, or rollback.

Guardrails answer:
- is the new version actually safe under real traffic?

### Promotion flow

- Start with a small percentage.
- Increase gradually only after each bake period passes.
- Typical steps may look like 1% -> 5% -> 25% -> 50% -> 100%.

Promotion answers:
- how fast can we expand without hiding regressions?

### Failure path

- If error rate or latency worsens materially, stop increasing traffic.
- If the regression is clear, shift traffic back to the stable version.
- If the result is ambiguous, hold the rollout, gather more signal, and investigate before promoting.

### Recovery path

- Route traffic back to the stable release.
- preserve logs, metrics, and traces for comparison
- fix the defect and rerun the canary when ready

---

## 6. What Problem It Solves

- Primary problem solved: reduces the blast radius of bad releases by validating the new version under limited production exposure
- Secondary benefits: safer experimentation, faster rollback, better release confidence, and clearer version-to-version comparison
- Systems impact: turns deployment from a binary switch into a controlled, observable progression

This topic solves three practical problems:
- how do we test production behavior safely?
- how do we detect regressions before the whole fleet is affected?
- how do we release frequently without treating every deploy like a gamble?

---

## 7. When to Rely on It

Use canary deployments when:
- the service is user-facing and regressions are expensive
- the team deploys frequently
- observability is strong enough to compare versions meaningfully
- the system can run old and new versions side by side
- traffic volume is high enough to give the canary real signal

Especially valuable for:
- booking and payment services
- search and recommendation APIs
- gateway or edge services
- high-throughput Spring Boot microservices
- systems where rollback speed matters more than absolute deployment simplicity

Strong interviewer keywords:
- progressive delivery
- weighted routing
- guardrail metrics
- blast radius
- bake time
- rollback threshold
- automated promotion

---

## 8. When Not to Use It

Canary is not always the right tool.

Be careful when:
- the system gets too little traffic to evaluate a small canary confidently
- the change is strongly coupled to a non-backward-compatible schema migration
- running two versions in parallel is too costly or operationally complex
- the service is internal, low risk, and simple enough that rolling deployment is sufficient

Avoid these patterns:
- declaring success after only a few requests
- routing randomly when session stickiness or cache locality matters
- ignoring business metrics and watching only CPU or memory
- canarying a stateful protocol change without compatibility planning

Better framing:
- use canary when real production signal can guide rollout decisions
- use blue-green when full-environment swap and instant cutover are more important
- use feature flags when the risk is more about behavior than binary version routing

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Canary deployments | Limit blast radius, enable measured promotion, and provide real production validation before full rollout | Need strong observability, can be statistically weak at low traffic, and add routing plus operational complexity |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Safety vs speed:
  slower, staged rollout reduces risk, but delays full release.
- Signal quality vs simplicity:
  richer comparisons improve confidence, but require better instrumentation and analysis.
- Cost vs confidence:
  running two versions simultaneously costs more compute, but gives safer rollout behavior.
- Automation vs operator judgment:
  automated rollback is fast and consistent, but poor thresholds can make the pipeline too sensitive or too blind.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using only infrastructure metrics | The canary may look healthy on CPU while users are seeing booking failures | Include user-facing SLIs and business KPIs in the rollout guardrails |
| Promoting too quickly | Short bake time can hide slow memory leaks, cache churn, or time-based failures | Tune rollout steps and bake windows to the service behavior |
| Ignoring cohort bias | Employee traffic or one geography may not represent the full workload | Choose cohorts deliberately and understand what they do not cover |
| Canarying incompatible schema changes carelessly | Old and new versions may conflict on reads or writes | Use backward-compatible migrations or decouple rollout stages |
| Comparing absolute metrics without baseline | Traffic shape can vary over time, making raw values misleading | Compare canary against the stable version over the same window |
| Treating any small blip as failure | Random noise can trigger unnecessary rollback | Define clear thresholds and use enough sample volume before deciding |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Initial canary size:
  often 1% to 5% for high-traffic systems
- Promotion steps:
  common progressions are 1% -> 5% -> 25% -> 50% -> 100% or similar staged ramps
- Bake window:
  often 10 to 30 minutes for high-volume APIs, but much longer for low-traffic or slow-burn failure modes
- Rollback threshold:
  commonly based on relative error-rate increase, p95 or p99 latency degradation, or business KPI drop
- Sample sufficiency:
  the canary should receive enough requests to make comparison meaningful before promotion
- Parallel version cost:
  temporarily higher compute and memory footprint is expected because both versions run concurrently

Interview shorthand:
- small percentage, bake, compare to baseline, promote gradually, rollback fast

---

## 12. Failure Modes

### Silent low-volume failure

Problem:
- The canary receives too little traffic, so a serious bug does not appear during the initial stage.

User impact:
- the issue reaches more users later when traffic is increased

Mitigation:
- extend bake time
- increase representative traffic carefully
- supplement with synthetic or targeted test traffic

### Bad cohort selection

Problem:
- The canary serves only internal users, but external customer traffic has a different usage pattern.

User impact:
- rollout looks healthy until broader exposure reveals real regressions

Mitigation:
- choose cohorts that reflect real workload characteristics
- understand which user segments are and are not covered

### Metric dilution

Problem:
- Operators watch fleet-wide dashboards instead of isolating canary metrics.

User impact:
- canary regressions are buried under healthy baseline traffic

Mitigation:
- tag telemetry by version
- compare canary versus stable directly
- build rollout-specific dashboards

### Data compatibility failure

Problem:
- The canary writes data in a format the stable version cannot read correctly.

User impact:
- rollback does not fully restore behavior because mixed-version data is already present

Mitigation:
- design schema and event changes for backward and forward compatibility
- separate data migration from traffic migration when needed

---

## 13. Scenario

- Product / system: Hotel search and pricing service running on Kubernetes behind an API gateway
- Requirement:
  release a new pricing engine safely during peak booking hours without risking widespread bad prices or slow search responses
- Good design:
  route 1% of traffic to the new version, compare canary and stable versions on search latency, pricing error rate, and booking conversion impact, then promote gradually only if guardrails stay healthy
- Why this concept fits:
  the pricing logic is business-critical, and the team needs real production validation with limited exposure
- What would go wrong without it:
  a pricing bug or latency regression could affect the whole customer base immediately

---

## 14. Code Sample

### Evaluating whether to promote or roll back a canary

```java
public record RolloutSnapshot(long totalRequests, long failedRequests, long p95LatencyMs) {
    public double errorRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) failedRequests / totalRequests;
    }
}

public enum RolloutDecision {
    PROMOTE,
    HOLD,
    ROLLBACK
}

public class CanaryGuardrails {

    private static final double MAX_ERROR_RATE_DELTA = 0.005;
    private static final long MAX_P95_LATENCY_DELTA_MS = 75;
    private static final long MIN_REQUESTS_FOR_DECISION = 500;

    public RolloutDecision evaluate(RolloutSnapshot stable, RolloutSnapshot canary) {
        if (canary.totalRequests() < MIN_REQUESTS_FOR_DECISION) {
            return RolloutDecision.HOLD;
        }

        boolean errorRegression = canary.errorRate() - stable.errorRate() > MAX_ERROR_RATE_DELTA;
        boolean latencyRegression = canary.p95LatencyMs() - stable.p95LatencyMs() > MAX_P95_LATENCY_DELTA_MS;

        if (errorRegression || latencyRegression) {
            return RolloutDecision.ROLLBACK;
        }

        return RolloutDecision.PROMOTE;
    }
}
```

Key idea:
- do not promote a canary just because deployment succeeded; promote only when real production guardrails stay within acceptable bounds

---

## 15. Mini Program / Simulation

This mini program simulates a canary rollout with staged traffic increases and automatic rollback when latency regresses too much.

```python
from dataclasses import dataclass


@dataclass
class Snapshot:
    requests: int
    errors: int
    p95_latency_ms: int

    def error_rate(self) -> float:
        return self.errors / self.requests


def decide(stable: Snapshot, canary: Snapshot) -> str:
    if canary.requests < 500:
        return "hold"

    if canary.error_rate() - stable.error_rate() > 0.005:
        return "rollback"

    if canary.p95_latency_ms - stable.p95_latency_ms > 75:
        return "rollback"

    return "promote"


def main() -> None:
    stable = Snapshot(requests=10000, errors=20, p95_latency_ms=220)

    rollout = [
        (1, Snapshot(requests=600, errors=1, p95_latency_ms=230)),
        (5, Snapshot(requests=900, errors=2, p95_latency_ms=240)),
        (25, Snapshot(requests=3000, errors=18, p95_latency_ms=340)),
    ]

    for percent, canary in rollout:
        decision = decide(stable, canary)
        print(
            f"stage={percent}% errorRate={canary.error_rate():.2%} "
            f"p95={canary.p95_latency_ms}ms decision={decision}"
        )

        if decision == "rollback":
            print("traffic returned to stable version")
            break


if __name__ == "__main__":
    main()
```

What this demonstrates:
- rollout happens in stages, not all at once
- canary traffic is evaluated against the stable baseline
- latency or error regressions should stop promotion quickly
- rollback is a normal control path, not a failure of the process

---

## 16. Practical Question

> You are releasing a new search and pricing service for a hotel booking platform. How would you design a canary deployment strategy so you can detect bad prices or latency regressions quickly without risking the whole customer base?

---

## 17. Strong Answer

I would deploy the new version beside the stable one and start with a very small percentage of live traffic, usually something like 1% or 5% depending on request volume. I would make sure telemetry is tagged by version so I can compare the canary directly against the stable baseline rather than looking only at fleet-wide averages.

The guardrails would include both technical and business signals: search error rate, p95 or p99 latency, downstream dependency failures, and a business metric such as booking conversion or pricing validation errors. If those metrics stay within thresholds for a defined bake window, I would gradually increase traffic. If they regress materially, I would stop promotion and route traffic back to the stable version immediately.

I would also check whether the release includes schema or contract changes. If it does, I would make those changes backward compatible first, because canary only works cleanly when old and new versions can coexist safely. The key principle is simple: use small real traffic, compare against a stable baseline, and let observability drive promotion or rollback.

---

## 18. Revision Notes

- One-line summary: Canary deployment reduces rollout risk by sending small production traffic to a new version, measuring real behavior, and promoting only if guardrails stay healthy.
- Three keywords: weighted routing, bake time, rollback
- One interview trap: calling it a canary rollout without version-isolated metrics or a clear rollback threshold
- One memory trick: small traffic, compare, expand