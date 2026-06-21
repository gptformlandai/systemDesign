# Microservices Distributed Systems Foundations Gold Sheet

> Track: Microservices Interview Track - Group 1 Starter Path  
> Goal: make microservices feel like distributed systems, not just smaller Spring Boot apps.

Use this before the pattern master sheet if you are new to microservices.

---

## 1. The Core Reframe

A microservice call is not a method call.

A method call usually fails because of code bugs or local process state.
A service call can fail because of network latency, timeout, DNS, deployment, load balancer,
serialization, authentication, connection pool limits, version mismatch, downstream slowness,
or partial regional outage.

Strong answer:

```text
Microservices turn local code boundaries into network boundaries. That gives independent
ownership and scaling, but every interaction now needs latency control, failure handling,
contract compatibility, observability, and security.
```

---

## 2. Method Call vs Network Call

| Dimension | Method Call | Microservice Call |
|---|---|---|
| Location | same process | different process or node |
| Failure | exception/null/bug | timeout, 5xx, DNS, TLS, auth, overload |
| Latency | nanoseconds/microseconds | milliseconds to seconds |
| Contract | compiler often helps | API/event contract must be versioned |
| Debugging | stack trace | logs, metrics, traces, correlation ID |
| Security | process boundary | identity, token, mTLS, authorization |
| Rollout | same deploy unit | independent versions coexist |

Interview trap:

```text
Designing service calls as if they were cheap local calls creates chatty APIs, high p99
latency, cascading failures, and distributed monoliths.
```

---

## 3. Why Microservices Exist

Microservices are useful when a system needs:

- independent team ownership
- independent deployments
- independent scaling
- separate data ownership
- clear failure boundaries
- different technology/runtime choices when justified
- faster evolution of separate business capabilities

They are not automatically better than monoliths.

Strong answer:

```text
I choose microservices when organizational and domain boundaries justify the distributed
systems cost. For a small product or tightly coupled domain, I prefer a modular monolith
until boundaries and scale pressure become clear.
```

---

## 4. Monolith, Modular Monolith, Microservices

| Architecture | Best Use | Main Risk |
|---|---|---|
| Monolith | small teams, simple domain, fast early delivery | grows into tangled codebase |
| Modular monolith | clear internal modules, one deployable | modules may leak boundaries |
| Microservices | independent ownership and scale | distributed system complexity |

Modular monolith first is often the mature answer.

```text
If the domain boundary is unclear, start with a modular monolith. Split later when the
module has stable ownership, independent scaling needs, and a clear data boundary.
```

---

## 5. The Five Costs Of Microservices

Every microservice split adds these costs:

1. Network cost: latency and partial failure.
2. Data cost: no simple joins or single transaction across services.
3. Operational cost: more deployments, dashboards, alerts, runbooks.
4. Testing cost: contract compatibility and integration confidence.
5. Ownership cost: teams must own services end to end.

Good interview close:

```text
Microservices move complexity from code structure into operations, data consistency, and
team coordination. The split is worth it only when those trade-offs buy real autonomy.
```

---

## 6. Essential Runtime Mental Model

A production microservice is not just application code.

```text
Client
  -> DNS / CDN / Load balancer
  -> API Gateway / Ingress
  -> Service pod/container
  -> Database / cache / broker
  -> Logs, metrics, traces
  -> Deployment, rollback, scaling, secrets, config
```

Runtime checklist:

| Concern | Question |
|---|---|
| Routing | How does traffic find healthy instances? |
| Health | What is liveness vs readiness? |
| Scaling | What metric drives replicas? |
| Config | How are environment differences handled? |
| Secrets | Where are credentials stored and rotated? |
| Observability | How do we debug one request across services? |
| Rollback | How do we safely return to a previous version? |

---

## 7. Latency Budget Basics

Latency is additive.

Example checkout budget:

```text
Gateway routing:        20 ms
Booking service logic:  40 ms
Availability call:     120 ms
Payment auth call:     250 ms
Database writes:        80 ms
Safety margin:          90 ms
Total budget:          600 ms
```

Fan-out makes p99 worse.

```text
If one page calls 10 downstream services, the page latency is often dominated by the slowest
call, not the average call.
```

Starter answer:

```text
For user-facing flows, I avoid unnecessary fan-out, set per-hop timeouts, track p95/p99,
and move non-critical side effects to async events.
```

---

## 8. Basic Failure Types

| Failure | Example | Protection |
|---|---|---|
| Slow dependency | Payment takes 5 seconds | timeout, circuit breaker |
| Down dependency | Loyalty unavailable | async retry, degrade |
| Duplicate request | client retries booking | idempotency key |
| Lost event | publish fails after DB commit | outbox |
| Poison message | consumer always crashes | bounded retry, DLQ |
| Hot partition | one booking key overloaded | better partition strategy |
| Contract break | provider removes field | compatibility tests |
| Bad deploy | new version 5xx | canary, rollback |

Memory line:

```text
In microservices, partial failure is normal design input, not an exception case.
```

---

## 9. Consistency Basics

Strong consistency:

```text
After a write, all readers see the latest value immediately.
```

Eventual consistency:

```text
After a write, different services may temporarily see old state, but they converge later.
```

Microservice rule:

```text
Keep strong consistency inside one service boundary. Use eventual consistency, sagas,
outbox, idempotency, and read models across service boundaries.
```

Hotel booking example:

| Requirement | Consistency Need |
|---|---|
| Do not sell same room twice | strong inside Availability Service |
| Send confirmation email | eventual consistency acceptable |
| Award loyalty points | eventual consistency acceptable |
| Show booking confirmation | read-your-writes UX expected |

---

## 10. CAP Theorem For Interviews

Do not overuse CAP. Use it carefully.

Plain version:

```text
When a network partition happens, a distributed system must choose between availability
and consistency for the affected operation.
```

Interview maturity:

```text
CAP is not a generic excuse to ignore correctness. For each operation, I decide which
business invariant must be protected and where stale reads are acceptable.
```

Example:

| Operation | Preference |
|---|---|
| Confirm booking inventory | consistency over availability |
| Show recommendation list | availability over freshness |
| Send marketing email | availability/eventual processing |
| Payment capture | correctness and auditability |

---

## 11. Service Ownership Basics

A real microservice has one accountable owner.

Owner responsibilities:

- API and event contracts
- database schema
- uptime and SLOs
- dashboard and alerts
- deployment and rollback
- incident response
- security and secrets
- documentation and deprecation

Strong answer:

```text
A service boundary is not real if no team owns its data, deploys it independently, and is
accountable for its production behavior.
```

---

## 12. First Design Checklist

Before splitting a service, ask:

1. What business capability is this?
2. Which data does it own?
3. Which operation must be strongly consistent?
4. Which work can be async?
5. What happens if a dependency is slow or down?
6. How will we observe one request end to end?
7. Who owns it in production?
8. Can it be deployed independently?

If the answers are unclear, avoid splitting yet.

---

## 13. Starter Anti-Patterns

| Anti-Pattern | Why It Hurts |
|---|---|
| Service per table | creates chatty, anemic services |
| Shared database | destroys autonomy and ownership |
| Sync call chain for everything | high latency and cascading failure |
| No timeouts | threads/connections get stuck |
| Retry everything | retry storms and duplicate side effects |
| Gateway owns business logic | gateway becomes new monolith |
| No correlation ID | impossible request debugging |
| One giant E2E test suite | slow, flaky confidence |

---

## 14. Beginner To Interview Answer Pattern

Use this when asked any broad microservice question:

```text
1. Start with the business boundary.
2. State data ownership.
3. Choose sync or async communication.
4. Add failure handling.
5. Add observability.
6. Add security.
7. Mention trade-off and when not to use it.
```

Example:

```text
For booking, I would keep inventory rules in Availability Service and booking lifecycle in
Booking Service. Immediate reserve/check calls can be synchronous, while notification and
loyalty updates can be events. I would use timeouts, idempotency, outbox, tracing, service
identity, and contract tests because the design crosses network and deployment boundaries.
```

---

## 15. Active Recall Check

Answer without notes:

1. Why is a service call not the same as a method call?
2. When is a modular monolith better than microservices?
3. What are the five costs of microservices?
4. Why does fan-out hurt p99 latency?
5. Where should strong consistency live?
6. What does database ownership mean?
7. Why is a shared database a distributed monolith smell?
8. What is the first thing you add to debug cross-service requests?
9. Why can retries be dangerous?
10. What makes a service boundary real?

---

## 16. Strong Closing Answer

```text
Microservices are a socio-technical architecture. They work when business boundaries,
data ownership, deployment independence, and team ownership align. They fail when teams
split code without accepting distributed systems responsibilities: latency, partial failure,
consistency, observability, testing, and security.
```
