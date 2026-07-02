# Spring Boot Modulith, Domain Events, And Module Boundaries Platinum Sheet

> Track: Spring Boot Interview Track - MAANG Platinum Scenarios  
> Goal: design modular Spring Boot systems that can grow before splitting into microservices.

---

## 1. Intuition

A modulith is a disciplined monolith. It keeps one deployable application, but the code is
divided into clear business modules with rules about who can call whom. It is often the
best middle path between a tangled monolith and premature microservices.

---

## 2. Definition

- Definition: Spring Modulith helps structure a Spring Boot application into explicit
  application modules, verify boundaries, publish domain events, and document module
  relationships.
- Category: architecture, modularity, domain design.
- Core idea: modularity is a design property, not a deployment count.

---

## 3. Why It Exists

Teams split into microservices too early because the monolith is messy. But the real
problem is often poor boundaries, not one deployable.

Modulith thinking exists to:

- keep transactional simplicity where useful
- prevent package-level spaghetti
- make domain ownership visible
- test architecture boundaries
- use events without distributed complexity too soon
- create a safer future path to microservices

---

## 4. Reality

Good candidates for modules in a hotel platform:

- booking
- inventory
- pricing
- payment
- customer
- notification
- loyalty
- settlement

Each module owns its domain model and exposes a small API. Other modules should not reach
into its internals or tables casually.

---

## 5. How It Works

1. Organize packages by business capability.
2. Treat each top-level package as an application module.
3. Keep module internals package-private where possible.
4. Expose only intentional interfaces or events.
5. Use domain events for cross-module side effects.
6. Test module boundaries.
7. Document dependencies.
8. Use outbox/integration events if events must leave the process.

Failure path:

- payment reaches directly into booking repositories
- notification reads booking tables directly
- circular module dependencies appear
- all modules share a common "util" domain model
- in-process events are mistaken for durable integration events

Recovery path:

- introduce module API interfaces
- move shared concepts to small value objects
- publish domain event from owner module
- add module boundary tests
- use outbox before leaving process

---

## 6. What Problem It Solves

- Primary problem solved: big Spring Boot applications becoming unstructured and hard to
  change.
- Secondary benefits: clearer ownership, easier testing, better future service extraction.
- Systems impact: improves maintainability without adding distributed-system failure modes.

---

## 7. When To Rely On It

Use modulith thinking when:

- one Spring Boot service has multiple business capabilities
- microservices feel too expensive
- teams need architecture boundaries
- transactions across modules are still valuable
- you want domain events without Kafka for every local side effect
- interviewers ask monolith vs microservices trade-offs

---

## 8. When Not To Use It

Do not use a modulith as an excuse for one giant deployment forever:

- independent teams may need separate release cadence
- scaling needs may differ dramatically
- regulatory isolation may require separate deployables
- data ownership may need hard network boundaries
- a module may have a very different runtime profile

When these pressures are real and persistent, split a well-designed module into a service.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Simpler deployment than microservices | One deployable can still be large |
| Local transactions are easier | Requires discipline to preserve boundaries |
| Clear module ownership | Runtime scaling is shared |
| Good stepping stone to services | In-process events are not durable by default |
| Boundary tests catch erosion | Teams must agree on package/API rules |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Gain: modularity without network cost.
- Give up: independent deployment per module.
- Latency: in-process calls are fast.
- Consistency: local transactions remain possible.
- Complexity: less than microservices, more discipline than a casual monolith.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Calling repositories across modules | Violates ownership | Expose module service/API |
| Shared mutable domain model | Couples modules | Use value objects/events |
| Circular dependencies | Modules cannot evolve independently | Define dependency direction |
| In-process event for external reliability | Event lost if process rolls back/crashes | Use outbox for integration event |
| Splitting to microservices before boundaries | Creates distributed mess | Modularize first |
| Only package cleanup, no tests | Boundaries erode | Add module verification tests |

---

## 11. Key Numbers

Reasoning heuristics:

- Start considering modules when one app has 3+ clear business capabilities.
- Keep module public surface small: fewer public types are easier to govern.
- Avoid circular dependencies entirely.
- Use in-process domain events for local side effects.
- Use outbox/Kafka/Pulsar for cross-process durability.
- Split service only when deployment, scaling, ownership, or isolation pressure justifies it.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| Change causes many regressions | Fragile codebase | Module boundaries unclear | Define and test modules |
| Circular dependencies | Build/design pain | Bad ownership direction | Refactor APIs/events |
| Lost notification | Missing side effect | In-process event after crash | Outbox for durable event |
| Slow release | Entire app deploys together | Module needs independent release | Extract service when justified |
| Data leak | Module reads another module tables | Ownership bypass | Module API and DB ownership |
| Common package grows | Hidden coupling | Dumping ground utilities | Move concepts to owning module |

---

## 13. Scenario

- Product/system: hotel booking platform.
- Why this concept fits: booking, inventory, payment, notification, and settlement can
  remain in one deployable while still having strong boundaries.
- What would go wrong without it: the code becomes a tangled monolith or premature
  microservices create network and data consistency problems.

---

## 14. Code Sample

Package sketch:

```text
com.example.hotel
  HotelApplication
  booking
    BookingService
    BookingRepository
    BookingCreatedEvent
    internal
      BookingEntity
  payment
    PaymentService
    PaymentAuthorizedEvent
  notification
    BookingNotificationListener
  settlement
    SettlementJob
```

Domain event sketch:

```java
package com.example.hotel.booking;

import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class BookingService {

    private final BookingRepository repository;
    private final ApplicationEventPublisher events;

    BookingService(BookingRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    BookingId create(CreateBookingCommand command) {
        Booking booking = repository.save(Booking.create(command));
        events.publishEvent(new BookingCreatedEvent(booking.id(), Instant.now()));
        return booking.id();
    }
}
```

---

## 15. Mini Program / Simulation

```python
dependencies = {
    "booking": {"inventory", "payment"},
    "payment": set(),
    "notification": {"booking"},
    "inventory": set(),
}


def has_cycle(graph):
    visiting, visited = set(), set()

    def dfs(node):
        if node in visiting:
            return True
        if node in visited:
            return False
        visiting.add(node)
        for nxt in graph.get(node, set()):
            if dfs(nxt):
                return True
        visiting.remove(node)
        visited.add(node)
        return False

    return any(dfs(node) for node in graph)


print("cycle?", has_cycle(dependencies))
```

---

## 16. Practical Question

> You are building a hotel platform. Would you start with microservices or a modular
> Spring Boot monolith, and how would you keep boundaries clean?

---

## 17. Strong Answer

I would usually start with a modular Spring Boot application unless independent deployment,
scaling, or ownership pressure is already clear. I would split packages by business module:
booking, inventory, payment, notification, settlement. Each module owns its data model and
exposes a small API or domain events. Other modules cannot call its repositories directly.
I would add architecture/module tests so boundaries do not erode. In-process domain events
are fine for local side effects, but anything that must survive process failure or notify
another service should use an outbox and durable messaging. If one module later needs its
own deployment lifecycle, the clean boundary makes extraction far safer.

---

## 18. Revision Notes

- One-line summary: a modulith gives microservice-like boundaries without distributed
  runtime cost.
- Three keywords: boundary, ownership, event.
- One interview trap: in-process events are not the same as durable integration events.
- One memory trick: modularize before you microservice.

