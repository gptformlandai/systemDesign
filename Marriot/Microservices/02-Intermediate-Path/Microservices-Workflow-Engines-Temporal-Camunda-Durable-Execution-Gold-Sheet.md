# Microservices Workflow Engines, Temporal, Camunda, And Durable Execution Gold Sheet

> Track: Microservices Interview Track - Intermediate Path  
> Goal: understand when a workflow engine is better than hand-rolled Saga logic, and how it fails.

---

## 1. Intuition

A Saga coordinates a business journey. A workflow engine gives that journey durable memory.

```text
normal code forgets when the process crashes
workflow code resumes because state/history is durable
```

Booking example:

```text
reserve inventory -> authorize payment -> confirm booking -> notify guest -> award points
```

If the workflow takes minutes, waits on external systems, needs retries, or has manual steps,
a durable workflow engine can be a better fit than a pile of retry tables and schedulers.

---

## 2. Definition

- Definition: a workflow engine coordinates long-running business processes by storing
  workflow state/history and executing steps with retries, timers, compensation, and visibility.
- Category: orchestration, Saga implementation, durable execution.
- Core idea: workflow state survives process crashes and restarts.

---

## 3. Hand-Rolled Saga vs Workflow Engine

| Area | Hand-Rolled Saga | Workflow Engine |
|---|---|---|
| State | service-owned tables | workflow history/state |
| Timers | custom scheduler | built-in durable timers |
| Retries | custom retry table/job | activity retry policies |
| Visibility | build dashboards | engine UI/history |
| Versioning | custom | engine-specific versioning model |
| Operations | simpler stack | new platform to operate |
| Best for | simple workflows | complex long-running workflows |

Strong line:

```text
I do not choose a workflow engine because it sounds advanced. I choose it when durable
timers, retries, state visibility, and workflow recovery justify the operational cost.
```

---

## 4. When To Use A Workflow Engine

Good fit:

- many workflow steps
- long-running process
- timers and deadlines matter
- external systems are flaky
- manual approval exists
- compensation is complex
- state visibility matters
- workflow must resume after crash
- retry policies differ by step
- business wants workflow progress tracking

Hotel examples:

- booking confirmation with payment unknown states
- refund workflow with approval
- group booking requiring hotel confirmation
- partner onboarding workflow
- loyalty dispute resolution

---

## 5. When Not To Use One

Avoid when:

- workflow is only 2-3 simple steps
- local transaction plus outbox is enough
- team cannot operate the engine
- workflow engine becomes a shared domain dumping ground
- every service has to depend on one central workflow team
- latency overhead is unacceptable for a simple request

Alternative:

```text
local transaction + outbox + idempotent consumers + simple reconciliation job
```

---

## 6. Core Concepts

| Concept | Meaning |
|---|---|
| Workflow | durable business process definition |
| Activity/task | side-effecting step such as payment call |
| Timer | durable wait/deadline |
| Retry policy | engine-managed retry behavior |
| Compensation | undo or repair step after failure |
| Workflow history | recorded events used to recover/replay |
| Worker | process that executes workflow/activity code |
| Signal/message | external input to running workflow |
| Query | read workflow state |
| Versioning | safe evolution of workflow code |

---

## 7. Durable Execution Flow

```text
1. Client starts workflow with business ID.
2. Engine records workflow started.
3. Worker executes first deterministic workflow step.
4. Workflow schedules activity.
5. Activity calls external service.
6. Engine records result or failure.
7. Retry/timer/next step is scheduled durably.
8. If worker crashes, another worker resumes from history.
9. Workflow completes, fails, or waits for manual action.
```

Important:

- activities can have side effects
- workflow decision code must be deterministic in many engines
- external calls should be in activities, not workflow decision logic
- activities must be idempotent because retries happen

---

## 8. Booking Workflow Example

```text
Start BookingWorkflow
  -> reserve inventory
  -> authorize payment
  -> confirm booking
  -> publish BookingConfirmed
  -> send notification
  -> award loyalty points
```

Failure handling:

| Failure | Workflow Action |
|---|---|
| inventory unavailable | fail booking cleanly |
| payment timeout | retry or mark PAYMENT_UNKNOWN |
| payment declined | release inventory |
| confirm booking fails | retry, then manual repair |
| notification fails | retry async, do not fail booking |
| loyalty fails | retry side effect or DLQ |

---

## 9. Activity Idempotency

Workflow engines retry activities. Retried activities must not duplicate side effects.

Examples:

| Activity | Idempotency Key |
|---|---|
| reserve inventory | booking ID |
| authorize payment | payment request ID |
| confirm booking | booking ID |
| send email | event ID |
| award loyalty | ledger operation ID |

Rule:

```text
Every side-effecting activity needs a stable business idempotency key.
```

---

## 10. Workflow Versioning

Changing workflow code is dangerous because old workflows may still be running.

Common safe strategies:

- keep old workflow path for running instances
- version workflow definitions
- use engine-supported versioning APIs
- add new activities compatibly
- avoid changing meaning of existing history
- migrate only with tested strategy

Interview trap:

```text
"Just deploy new workflow code" can break replay of old workflow histories.
```

---

## 11. Workflow Observability

Track:

- workflows started/completed/failed
- workflow age
- step/activity latency
- activity retry count
- timer count
- stuck workflow count
- compensation count
- manual intervention count
- worker queue backlog
- worker failures

For booking:

```text
show bookings stuck in PAYMENT_UNKNOWN longer than threshold
```

---

## 12. Workflow Engine Failure Modes

| Failure | Impact | Mitigation |
|---|---|---|
| worker down | workflows stop progressing | autoscale workers, alerts |
| engine unavailable | cannot schedule progress | HA engine, retry clients |
| activity not idempotent | duplicate side effects | business idempotency keys |
| bad workflow version | replay failures | versioning discipline |
| engine becomes domain monolith | team coupling | domain logic remains in services |
| massive workflow history | slow replay | continue-as-new/archival strategy |
| hidden stuck workflows | business state frozen | age alerts and dashboards |

---

## 13. Workflow Engine vs Message Choreography

Choreography:

```text
BookingCreated -> InventoryReserved -> PaymentAuthorized -> BookingConfirmed
```

Pros:

- loose service coupling
- simple for small flows
- natural event-driven model

Cons:

- hard to see whole workflow
- event spaghetti
- compensation can be scattered

Orchestration:

```text
BookingWorkflow commands each step and stores progress
```

Pros:

- clear workflow state
- easier timers/retries
- easier visibility

Cons:

- central coordinator risk
- engine operational cost
- workflow versioning complexity

---

## 14. Interview Question

> A booking workflow has 12 steps, waits up to 48 hours for supplier confirmation, and must retry payment and notification safely. Would you use a workflow engine?

Strong answer:

```text
I would consider a workflow engine because this is long-running, timer-heavy, and needs
durable progress visibility. I would keep domain ownership inside services: Inventory owns
inventory, Payment owns payment audit, Booking owns booking lifecycle. Workflow activities
would call those services using idempotency keys. I would monitor stuck workflows, activity
retries, worker backlog, and compensation. If the flow were only a few simple steps, I would
prefer local transaction, outbox, and idempotent consumers.
```

---

## 15. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Use engine for every workflow | platform overkill | reserve for complex long-running flows |
| Put all domain logic in workflow | shared business monolith | services own rules |
| Non-idempotent activity | duplicate charge/booking | stable idempotency keys |
| Ignore workflow code versioning | replay failures | explicit version strategy |
| No stuck workflow alert | silent business outage | workflow age dashboards |
| Treat timeout as failure | external result may be unknown | reconcile unknown states |

---

## 16. Strong Closing Answer

```text
A workflow engine is useful when Saga coordination becomes long-running, timer-heavy, and
hard to recover manually. It gives durable state, retries, timers, and visibility, but it adds
platform and versioning complexity. I use it for complex workflows while keeping business
ownership in services and making every side-effecting activity idempotent.
```

