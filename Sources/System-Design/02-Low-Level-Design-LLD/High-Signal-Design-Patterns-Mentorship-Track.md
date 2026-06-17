# High-Signal Design Patterns - Mentorship Track

> Goal: build practical low-level design pattern intuition with interview-ready clarity, confusion-free comparisons, and examples that show when each pattern actually earns its place.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `2.2 High-Signal Design Patterns`.
- We will learn patterns as design vocabulary, not as decoration.
- Every pattern will answer: what confusion it removes, when it helps, when it hurts, and how to explain it in interviews.
- For LLD topics, Section 14 uses Java for enterprise-style object design, and Section 15 uses Python for compact runnable intuition.
- Code comments call out the exact LLD concept being applied where that makes the pattern easier to remember.

---

## Roadmap for This Sheet

### Creational Patterns

1. Factory
2. Builder
3. Singleton and why it is dangerous

### Structural Patterns

4. Adapter
5. Decorator
6. Facade
7. Proxy

### Behavioral Patterns

8. Strategy
9. Observer
10. State
11. Command

---

## Pattern Decision Map

| If the design problem is... | Pattern to consider | Why |
|---|---|---|
| I need to choose which concrete object to create | Factory | Hides creation decisions behind a stable method |
| I need to create an object with many optional fields and invariants | Builder | Makes construction readable and safe |
| I need exactly one shared instance | Singleton, carefully | Useful for immutable or stateless shared resources, dangerous for mutable global state |
| I need to use an incompatible external or legacy API | Adapter | Converts one interface into another |
| I need to add behavior without changing the original object | Decorator | Wraps the same interface with extra behavior |
| I need to simplify a messy subsystem behind one clean API | Facade | Gives clients one high-level entry point |
| I need access control, lazy loading, caching, or remote boundary behavior | Proxy | Controls access to another object through the same interface |
| I need to swap algorithms | Strategy | Injects interchangeable behavior |
| I need many listeners to react to an event | Observer | Publishes change notifications to subscribers |
| I need behavior to change with object lifecycle state | State | Moves state-specific behavior into state objects |
| I need to store, queue, retry, audit, or undo an action | Command | Turns a request into an object |

---

## Confusion Map

| Common confusion | Clean distinction |
|---|---|
| Factory vs Strategy | Factory creates an object. Strategy uses an object to perform an algorithm. |
| Factory vs Builder | Factory chooses a concrete type. Builder assembles one complex object step by step. |
| Builder vs Constructor | Constructor is fine for small required data. Builder helps when optional fields and validation make construction noisy. |
| Singleton vs Dependency Injection singleton | Singleton mixes single instance with global access. DI singleton controls lifecycle while still allowing testable injection. |
| Adapter vs Facade | Adapter changes an incompatible interface into the one you need. Facade simplifies a subsystem that may already be compatible. |
| Decorator vs Proxy | Decorator adds responsibilities. Proxy controls access, lifecycle, location, or expensive work. |
| Decorator vs Inheritance | Decorator composes behavior at runtime. Inheritance fixes behavior at class definition time. |
| Strategy vs State | Strategy is usually selected from outside. State changes internally as the object moves through a lifecycle. |
| Observer vs Message Queue | Observer is usually in-process notification. A queue is durable, distributed, and operationally heavier. |
| Command vs Strategy | Command represents an action/request. Strategy represents an algorithm/policy. |

---

# Topic 1: Factory

> Track: 2.2 High-Signal Design Patterns
> Scope: object creation, concrete type selection, dependency isolation, and extension-friendly construction

---

## 1. Intuition

Think of a hotel front desk.

A guest asks for a room. The guest should not know whether the hotel creates a standard room booking, suite booking, corporate booking, or loyalty booking behind the scenes. The front desk chooses the right booking flow based on the request.

Factory is the front desk for object creation.

Short memory trick:
- caller asks for a capability
- factory chooses the concrete class
- caller uses the returned interface

---

## 2. Definition

- Definition: Factory is a creational pattern that centralizes object creation logic and returns objects through a stable abstraction.
- Category: Creational design pattern
- Core idea: Keep callers independent from the concrete classes they use.

Factory can appear as:
- a simple static factory method
- a factory class
- an abstract factory for families of related objects
- a DI container provider method

Interview shortcut:
- Factory is not about making every `new` disappear
- Factory is about hiding creation decisions when those decisions vary

---

## 3. Why It Exists

Without a factory, creation logic spreads across the codebase.

Bad signs:
- many `if channel == "email"` checks across services
- every caller knows concrete class names
- adding one provider requires editing many files
- tests must construct deep dependency graphs manually

Factory exists because creation is often a separate responsibility from usage.

Example:
- checkout should send a notification
- checkout should not know how to build `EmailNotifier`, `SmsNotifier`, or `PushNotifier`

---

## 4. Reality

Factories appear in real systems as:

- payment provider selection: Stripe, Adyen, PayPal
- notification channel selection: email, SMS, push, WhatsApp
- parser selection: JSON, Avro, Protobuf, CSV
- storage client creation: S3, GCS, Azure Blob
- shipping provider selection: FedEx, UPS, DHL
- feature-specific service creation in plugin architectures

Framework examples:
- Spring bean factories
- Java `Calendar.getInstance()` style factory methods
- Python factory functions that return strategy implementations
- ORM session factories

---

## 5. How It Works

Typical flow:

1. Define an interface the caller actually needs.
2. Implement multiple concrete classes behind that interface.
3. Put selection logic in one factory.
4. Caller requests an implementation using a small input such as type, region, tenant, or config.
5. Caller uses the returned abstraction without knowing the concrete type.
6. New implementation is added by updating factory registration, not the caller logic.

Important design point:
- if the caller immediately checks the returned concrete type, the factory did not help

---

## 6. What Problem It Solves

- Primary problem solved: scattered concrete object creation.
- Secondary benefits: testability, simpler callers, centralized provider selection, easier extension.
- Systems impact: reduces blast radius when new providers or variants are added.

Factory is especially useful when creation depends on:
- runtime configuration
- tenant type
- geography
- channel
- feature flag
- external provider availability

---

## 7. When to Rely on It

Use Factory when:

- there are multiple concrete implementations of the same role
- caller should not know concrete class names
- creation logic has branching
- construction requires provider-specific setup
- object choice is driven by config, request type, tenant, or region
- you expect more variants later

Interview trigger words:
- provider selection
- channel selection
- plugin
- parser
- gateway
- driver
- multiple implementations

---

## 8. When Not to Use It

Avoid Factory when:

- there is only one concrete class and no realistic variation
- the constructor is simple and clear
- the factory would only wrap `new Something()` with no extra value
- the factory becomes a giant switch with business logic inside
- the factory hides dependencies so much that tests become unclear

Use plain constructors when object creation is obvious.

Use dependency injection when the object should be selected at application startup rather than per request.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Centralizes creation decisions | Can become a large switch statement |
| Decouples callers from concrete classes | Adds indirection for simple cases |
| Supports new variants cleanly | Can hide important dependencies |
| Improves testability through interfaces | Poor naming can make code harder to trace |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: callers become simpler and depend on abstractions.
- Give up: direct visibility into which class is created at the call site.
- Complexity impact: low to medium, depending on number of variants.
- Testing impact: easier if factory returns interfaces, harder if factory has hidden global config.

### Common Mistakes

- Mistake: create a factory for every class.
- Why it is wrong: it adds ceremony without solving variation.
- Better approach: use constructors until creation varies.

- Mistake: put business workflow inside the factory.
- Why it is wrong: factory should create objects, not run use cases.
- Better approach: keep orchestration in services and creation in factories.

- Mistake: return concrete types.
- Why it is wrong: callers stay coupled to implementation details.
- Better approach: return the narrow interface the caller needs.

---

## 11. Key Numbers

Pattern heuristics:

- One implementation: constructor is usually enough.
- Two implementations: factory may be useful if selection logic is not local.
- Three or more implementations: factory usually becomes valuable.
- More than 5 to 7 variants: consider registration map instead of a long switch.
- More than one selection dimension: consider composing factories or using configuration-driven registration.

Memory number:
- Factory pays for itself when object choice changes more often than the caller workflow.

---

## 12. Failure Modes

- Missing mapping: caller asks for `sms`, factory has no SMS implementation.
- Wrong implementation: region or tenant config routes to the wrong provider.
- Factory bloat: creation logic becomes mixed with validation, authorization, and workflow.
- Hidden dependencies: factory creates concrete classes with real external clients during tests.
- Runtime surprise: errors appear only when a rare type is requested.

Mitigations:
- validate supported types at startup
- keep factory mappings explicit
- use tests for every registered type
- inject dependencies into factory instead of constructing everything inside it

---

## 13. Scenario

- Product / system: Hotel booking notification service
- Requirement: send confirmations through email, SMS, or push based on user preference
- Good design: `NotificationFactory` returns a `NotificationSender` abstraction
- Why this pattern fits: checkout should not know provider-specific classes
- What would go wrong without it: every caller would duplicate channel branching

---

## 14. Java Code Sample

### Notification sender factory

```java
interface NotificationSender {
    void send(String userId, String message);
}

class EmailSender implements NotificationSender {
    public void send(String userId, String message) {
        System.out.println("email to " + userId + ": " + message);
    }
}

class SmsSender implements NotificationSender {
    public void send(String userId, String message) {
        System.out.println("sms to " + userId + ": " + message);
    }
}

class PushSender implements NotificationSender {
    public void send(String userId, String message) {
        System.out.println("push to " + userId + ": " + message);
    }
}

class NotificationFactory {
    public NotificationSender create(String channel) {
        // LLD concept: Factory owns concrete type selection; callers depend on NotificationSender.
        return switch (channel) {
            case "email" -> new EmailSender();
            case "sms" -> new SmsSender();
            case "push" -> new PushSender();
            default -> throw new IllegalArgumentException("unsupported channel: " + channel);
        };
    }
}

class BookingConfirmationService {
    private final NotificationFactory notificationFactory;

    BookingConfirmationService(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    public void confirm(String userId, String preferredChannel) {
        NotificationSender sender = notificationFactory.create(preferredChannel);
        // LLD concept: service uses the abstraction and stays unaware of concrete sender classes.
        sender.send(userId, "booking confirmed");
    }
}
```

Key idea:
- factory isolates creation, while the caller only uses the interface

---

## 15. Python Mini Program / Simulation

This mini program shows provider selection without spreading `if` checks everywhere.

```python
from typing import Protocol


class PaymentProcessor(Protocol):
    def charge(self, user_id: str, amount: int) -> str:
        pass


class StripeProcessor:
    def charge(self, user_id: str, amount: int) -> str:
        return f"stripe charged {user_id}: {amount}"


class PaypalProcessor:
    def charge(self, user_id: str, amount: int) -> str:
        return f"paypal charged {user_id}: {amount}"


class PaymentProcessorFactory:
    def __init__(self) -> None:
        # LLD concept: registration map avoids a long chain of conditionals as variants grow.
        self._processors: dict[str, PaymentProcessor] = {
            "stripe": StripeProcessor(),
            "paypal": PaypalProcessor(),
        }

    def create(self, provider: str) -> PaymentProcessor:
        if provider not in self._processors:
            raise ValueError(f"unsupported provider: {provider}")
        # LLD concept: caller receives the common contract, not the concrete provider class.
        return self._processors[provider]


def checkout(provider: str) -> None:
    factory = PaymentProcessorFactory()
    processor = factory.create(provider)
    print(processor.charge("user-1", 250))


if __name__ == "__main__":
    checkout("stripe")
    checkout("paypal")
```

What this demonstrates:
- creation choice is centralized
- checkout does not branch by provider
- new providers can be registered without changing checkout flow

---

## 16. Practical Question

> You are designing a notification system that supports email, SMS, push, and future channels. How would you use Factory, and what trade-offs would you consider?

---

## 17. Strong Answer

1. I would define a `NotificationSender` interface with a `send` method.
2. Each channel would implement that interface.
3. A factory would select the sender based on channel or user preference.
4. Checkout and booking services would depend only on `NotificationSender`.
5. This makes adding a new channel localized to registration and implementation.
6. The trade-off is indirection, so I would avoid this if there is only one channel.
7. I would validate supported channels at startup to avoid runtime surprises.

---

## 18. Revision Notes

- One-line summary: Factory centralizes concrete object creation behind an abstraction.
- Three keywords: creation, selection, abstraction
- One interview trap: using Factory when a plain constructor is enough
- One memory trick: Factory creates, Strategy behaves

---

# Topic 2: Builder

> Track: 2.2 High-Signal Design Patterns
> Scope: complex construction, optional fields, readability, validation, and immutable objects

---

## 1. Intuition

Think of booking a custom vacation package.

You choose hotel, dates, room type, breakfast, airport pickup, coupon, loyalty points, and special requests. A giant constructor call with all of these values would be hard to read and easy to misuse.

Builder is a guided assembly process for complex objects.

Short memory trick:
- constructor is a single form
- builder is a checklist
- final `build()` validates the object

---

## 2. Definition

- Definition: Builder is a creational pattern that constructs complex objects step by step, usually with readable methods and final validation.
- Category: Creational design pattern
- Core idea: Separate object assembly from the final immutable object.

Builder is most useful when:
- many fields are optional
- fields have dependencies
- object should be immutable after creation
- constructor overloads become confusing

---

## 3. Why It Exists

Without Builder, complex construction often becomes unreadable.

Problem example:

```java
new BookingSearch("NYC", "2026-07-01", "2026-07-05", 2, 1, true, false, null, "GOLD", 50);
```

Confusions:
- What does `true` mean?
- What does `false` mean?
- Is `null` safe?
- Are dates valid?
- Are optional values in the right order?

Builder exists to make construction self-documenting.

---

## 4. Reality

Builder appears in:

- Java `StringBuilder`
- HTTP client builders
- request builders
- query builders
- test data builders
- configuration objects
- immutable DTO construction
- SDK clients, such as cloud clients and payment clients

Common enterprise example:
- build a `SearchRequest` with required dates and optional filters

---

## 5. How It Works

Typical flow:

1. Caller starts with a builder.
2. Caller sets required and optional values using named methods.
3. Builder stores temporary mutable construction state.
4. Caller invokes `build()`.
5. Builder validates required fields and cross-field rules.
6. Builder returns the final object, often immutable.

Important design point:
- the builder may be mutable, but the object it creates should usually be stable

---

## 6. What Problem It Solves

- Primary problem solved: confusing construction of complex objects.
- Secondary benefits: readable code, fewer constructor overloads, centralized validation.
- Systems impact: reduces bugs caused by wrong parameter order and missing validation.

Builder is especially useful for:
- request objects
- configuration objects
- query criteria
- domain objects with optional metadata
- tests that need clear setup

---

## 7. When to Rely on It

Use Builder when:

- an object has many optional fields
- constructor has more than 4 to 5 parameters
- boolean parameters make calls unreadable
- object creation has validation rules
- you want immutable objects with readable creation
- tests repeatedly create similar objects with small differences

Interview trigger words:
- many optional fields
- constructor overloads
- readable object creation
- immutable request
- fluent API

---

## 8. When Not to Use It

Avoid Builder when:

- object has only 2 or 3 obvious fields
- construction has no optional data
- a record or dataclass is already clear
- builder duplicates validation in another layer
- the builder allows invalid partially built objects to escape

Use a constructor, static factory, record, or dataclass for simple value objects.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Improves readability | Adds extra class or code |
| Handles optional fields well | Can be overkill for simple objects |
| Centralizes validation | Bad builders can still allow invalid state |
| Works well with immutability | Fluent APIs can hide expensive work |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: construction becomes explicit and readable.
- Give up: more code than a simple constructor.
- Complexity impact: low for useful builders, noisy for unnecessary builders.
- Testing impact: test setup becomes clearer if builder defaults are sensible.

### Common Mistakes

- Mistake: builder for every DTO.
- Why it is wrong: simple objects do not need construction machinery.
- Better approach: use builder only when construction confusion exists.

- Mistake: no validation in `build()`.
- Why it is wrong: builder becomes only a verbose setter bag.
- Better approach: validate required fields and cross-field constraints in one place.

- Mistake: reusable builder accidentally carries old state.
- Why it is wrong: one build can leak fields into the next object.
- Better approach: use a fresh builder per object or provide clear reset behavior.

---

## 11. Key Numbers

Pattern heuristics:

- 1 to 3 fields: constructor or record is usually enough.
- 4 to 5 fields: consider builder if fields are optional or same type.
- 2 or more boolean constructor parameters: builder likely improves readability.
- 2 or more constructor overloads with optional values: builder is often cleaner.
- Cross-field validation: `build()` is a good home for it.

Memory number:
- Builder helps when the call site needs names more than the constructor needs brevity.

---

## 12. Failure Modes

- Missing required field: object built without destination or dates.
- Invalid combination: checkout date before check-in date.
- Mutable final object: builder creates an object that callers can still mutate freely.
- State leakage: builder reused and carries values from a previous build.
- Too much logic: builder starts running workflows instead of constructing data.

Mitigations:
- validate in `build()`
- keep final object immutable where possible
- avoid reusing builder instances
- keep builder focused on construction

---

## 13. Scenario

- Product / system: Hotel search request builder
- Requirement: build a search request with destination, dates, guests, room type, breakfast, loyalty tier, and optional coupon
- Good design: builder names each option and validates dates in `build()`
- Why this pattern fits: the object has optional fields and cross-field rules
- What would go wrong without it: constructor calls become unreadable and fragile

---

## 14. Java Code Sample

### Booking search request builder

```java
import java.time.LocalDate;

public final class BookingSearchRequest {
    private final String destination;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final boolean breakfastIncluded;
    private final String loyaltyTier;

    private BookingSearchRequest(Builder builder) {
        this.destination = builder.destination;
        this.checkIn = builder.checkIn;
        this.checkOut = builder.checkOut;
        this.guests = builder.guests;
        this.breakfastIncluded = builder.breakfastIncluded;
        this.loyaltyTier = builder.loyaltyTier;
    }

    public static Builder builder(String destination, LocalDate checkIn, LocalDate checkOut) {
        // LLD concept: required fields are explicit at builder creation time.
        return new Builder(destination, checkIn, checkOut);
    }

    public static final class Builder {
        private final String destination;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private int guests = 1;
        private boolean breakfastIncluded;
        private String loyaltyTier = "STANDARD";

        private Builder(String destination, LocalDate checkIn, LocalDate checkOut) {
            this.destination = destination;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
        }

        public Builder guests(int guests) {
            this.guests = guests;
            return this;
        }

        public Builder includeBreakfast() {
            // LLD concept: named method removes boolean-parameter confusion at call sites.
            this.breakfastIncluded = true;
            return this;
        }

        public Builder loyaltyTier(String loyaltyTier) {
            this.loyaltyTier = loyaltyTier;
            return this;
        }

        public BookingSearchRequest build() {
            // LLD concept: builder centralizes cross-field validation before object creation.
            if (!checkOut.isAfter(checkIn)) {
                throw new IllegalArgumentException("checkout must be after checkin");
            }
            if (guests <= 0) {
                throw new IllegalArgumentException("guests must be positive");
            }
            return new BookingSearchRequest(this);
        }
    }
}
```

Key idea:
- the builder makes construction readable while the final object stays controlled

---

## 15. Python Mini Program / Simulation

This mini program shows a builder replacing a confusing long constructor.

```python
from dataclasses import dataclass
from datetime import date


@dataclass(frozen=True)
class BookingSearchRequest:
    destination: str
    check_in: date
    check_out: date
    guests: int
    breakfast: bool
    loyalty_tier: str


class BookingSearchBuilder:
    def __init__(self, destination: str, check_in: date, check_out: date) -> None:
        # LLD concept: required construction data is separated from optional choices.
        self.destination = destination
        self.check_in = check_in
        self.check_out = check_out
        self.guests = 1
        self.breakfast = False
        self.loyalty_tier = "STANDARD"

    def for_guests(self, guests: int) -> "BookingSearchBuilder":
        self.guests = guests
        return self

    def with_breakfast(self) -> "BookingSearchBuilder":
        # LLD concept: fluent method gives meaning to an optional boolean setting.
        self.breakfast = True
        return self

    def with_loyalty_tier(self, tier: str) -> "BookingSearchBuilder":
        self.loyalty_tier = tier
        return self

    def build(self) -> BookingSearchRequest:
        # LLD concept: build is the single gate that prevents invalid final objects.
        if self.check_out <= self.check_in:
            raise ValueError("checkout must be after checkin")
        if self.guests <= 0:
            raise ValueError("guests must be positive")
        return BookingSearchRequest(
            self.destination,
            self.check_in,
            self.check_out,
            self.guests,
            self.breakfast,
            self.loyalty_tier,
        )


def main() -> None:
    request = (
        BookingSearchBuilder("NYC", date(2026, 7, 1), date(2026, 7, 5))
        .for_guests(2)
        .with_breakfast()
        .with_loyalty_tier("GOLD")
        .build()
    )
    print(request)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- optional values are named
- validation is centralized
- final request is immutable

---

## 16. Practical Question

> You are designing a hotel search API request object with many optional filters. Would you use Builder?

---

## 17. Strong Answer

1. I would use Builder if the request has many optional filters or validation rules.
2. Required fields such as destination and dates should be explicit.
3. Optional fields such as breakfast, room type, and loyalty tier should have named builder methods.
4. The final `build()` method should validate date ranges and guest count.
5. I would return an immutable request object.
6. If the request has only a few simple fields, I would prefer a constructor or record.

---

## 18. Revision Notes

- One-line summary: Builder makes complex object construction readable and safe.
- Three keywords: optional fields, validation, readability
- One interview trap: using Builder for tiny objects with no construction confusion
- One memory trick: Builder assembles, Factory selects

---

# Topic 3: Singleton and Why It Is Dangerous

> Track: 2.2 High-Signal Design Patterns
> Scope: single instance, global access, hidden state, testability, dependency injection, and safer alternatives

---

## 1. Intuition

Think of a hotel having one master emergency switchboard.

Having one switchboard can be useful. But if every department secretly reaches into it and changes settings, the hotel becomes unpredictable.

Singleton is useful when one shared instance is truly needed. It becomes dangerous when it turns into global mutable state.

Short memory trick:
- one instance can be fine
- global access is the danger
- mutable singleton is the real risk

---

## 2. Definition

- Definition: Singleton is a creational pattern that ensures a class has only one instance and provides a global access point to it.
- Category: Creational design pattern
- Core idea: Control instance count, usually to one.

Important nuance:
- single instance is a lifecycle decision
- global access is a coupling decision
- the dangerous part is often global access plus mutable state

Safer modern interpretation:
- prefer dependency injection with singleton lifecycle managed by the container

---

## 3. Why It Exists

Singleton was created for cases where multiple instances are wasteful, incorrect, or confusing.

Examples:
- application configuration snapshot
- stateless formatter registry
- shared metrics registry
- process-wide logger
- expensive shared client pool managed once

But it is often misused to avoid passing dependencies.

Bad reason:
- "I do not want to inject this dependency, so I will make it globally available."

Good reason:
- "There must be exactly one immutable application configuration instance in this process."

---

## 4. Reality

In production systems, Singleton-like lifecycle exists everywhere:

- Spring singleton beans
- application config objects
- connection pools
- logger instances
- metrics registries
- feature flag clients
- serializer registries

But high-quality code usually avoids direct global singleton access. Instead, the framework creates one instance and injects it where needed.

Interview maturity:
- say Singleton is risky
- explain why
- propose DI-managed singleton when appropriate

---

## 5. How It Works

Classic Singleton flow:

1. Constructor is private.
2. Class stores one static instance.
3. Caller asks for instance through a static method.
4. Same instance is returned every time.

Modern safer flow:

1. Application creates one instance at startup.
2. Dependency injection container owns lifecycle.
3. Services receive the dependency through constructors.
4. Tests can replace it with a fake.

Important design point:
- single lifecycle does not require global access

---

## 6. What Problem It Solves

- Primary problem solved: controls duplicate creation of a shared resource.
- Secondary benefits: shared configuration, consistent registry, simpler lifecycle for expensive resources.
- Systems impact: can reduce resource waste, but can increase coupling and test fragility.

Singleton solves instance count.

It does not solve:
- dependency management
- configuration design
- thread safety by default
- test isolation

---

## 7. When to Rely on It

Use Singleton carefully when:

- the object is immutable after startup
- the object is stateless
- the resource is genuinely process-wide
- duplicate instances would be wrong or expensive
- lifecycle is managed by a DI container
- tests can replace the dependency

Good candidates:
- immutable app configuration
- metrics registry
- logger facade
- shared connection pool object

---

## 8. When Not to Use It

Avoid Singleton when:

- object stores request-specific state
- object is mutable and accessed by many threads
- tests need different instances
- you only want convenience
- it hides dependencies
- it makes ordering of initialization complicated

Prefer:
- constructor injection
- dependency injection lifecycle scopes
- immutable config objects
- explicit context objects

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Controls duplicate instances | Creates hidden global coupling |
| Useful for immutable shared resources | Makes tests harder if globally accessed |
| Can simplify resource lifecycle | Can hide mutable shared state |
| Common in frameworks | Can cause thread-safety issues |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: one shared instance and simpler lifecycle for true shared resources.
- Give up: flexibility if global access is hardcoded.
- Complexity impact: deceptively low at first, often high later.
- Testing impact: can be painful unless dependency can be replaced.

### Common Mistakes

- Mistake: store user session data in a singleton.
- Why it is wrong: users leak state into each other.
- Better approach: use request/session-scoped objects.

- Mistake: use Singleton to avoid dependency injection.
- Why it is wrong: dependencies become invisible and tests become brittle.
- Better approach: inject a single instance through constructors.

- Mistake: mutable singleton with no synchronization.
- Why it is wrong: concurrent calls can corrupt shared state.
- Better approach: immutable object or thread-safe design.

---

## 11. Key Numbers

Pattern heuristics:

- One per process: acceptable for immutable config or registry.
- One per request: not a singleton.
- One per tenant: not a singleton, use scoped dependency or keyed registry.
- Mutable shared fields: treat as concurrency risk immediately.
- Tests requiring reset: strong smell that singleton is hurting design.

Memory number:
- Singleton is safest when state changes zero times after startup.

---

## 12. Failure Modes

- Test pollution: one test changes singleton state and another test fails.
- Race condition: multiple threads mutate shared state.
- Initialization order bug: singleton reads config before config is loaded.
- Hidden dependency: class looks simple but secretly reaches global state.
- Multi-tenant leakage: tenant A config appears in tenant B request.

Mitigations:
- make singleton immutable
- inject it instead of globally fetching it
- avoid request-specific data
- reset only in test harness if absolutely needed
- use DI scopes for tenant or request-specific resources

---

## 13. Scenario

- Product / system: Feature flag configuration snapshot
- Requirement: all services in one process read the same immutable startup configuration
- Good design: application creates one immutable `FeatureFlagConfig` and injects it
- Why this pattern fits: duplicate mutable copies would create inconsistent reads
- What would go wrong without caution: global mutable flags could change during tests or requests unpredictably

---

## 14. Java Code Sample

### Safer DI-managed singleton style

```java
import java.util.Map;

public final class FeatureFlagConfig {
    private final Map<String, Boolean> flags;

    public FeatureFlagConfig(Map<String, Boolean> flags) {
        // LLD concept: immutable snapshot avoids mutable global state danger.
        this.flags = Map.copyOf(flags);
    }

    public boolean enabled(String flagName) {
        return flags.getOrDefault(flagName, false);
    }
}

class CheckoutService {
    private final FeatureFlagConfig featureFlags;

    CheckoutService(FeatureFlagConfig featureFlags) {
        // LLD concept: dependency is injected, so tests can provide a fake config.
        this.featureFlags = featureFlags;
    }

    public void checkout() {
        if (featureFlags.enabled("new-payment-flow")) {
            System.out.println("new flow");
        } else {
            System.out.println("old flow");
        }
    }
}
```

Key idea:
- one shared instance can be fine, but global mutable access is what makes Singleton dangerous

---

## 15. Python Mini Program / Simulation

This mini program shows the safer version: one immutable config passed explicitly.

```python
from dataclasses import dataclass
from types import MappingProxyType
from typing import Mapping


@dataclass(frozen=True)
class FeatureFlagConfig:
    flags: Mapping[str, bool]

    @staticmethod
    def from_dict(flags: dict[str, bool]) -> "FeatureFlagConfig":
        # LLD concept: make a read-only snapshot instead of exposing mutable global state.
        return FeatureFlagConfig(MappingProxyType(dict(flags)))

    def enabled(self, name: str) -> bool:
        return self.flags.get(name, False)


class CheckoutService:
    def __init__(self, config: FeatureFlagConfig) -> None:
        # LLD concept: injected singleton lifecycle stays testable and explicit.
        self.config = config

    def checkout(self) -> str:
        return "new flow" if self.config.enabled("new-payment-flow") else "old flow"


def main() -> None:
    config = FeatureFlagConfig.from_dict({"new-payment-flow": True})
    service = CheckoutService(config)
    print(service.checkout())


if __name__ == "__main__":
    main()
```

What this demonstrates:
- single instance behavior without global lookup
- immutable config prevents accidental mutation
- dependency remains replaceable in tests

---

## 16. Practical Question

> Your interviewer asks whether you would use Singleton for a configuration service. How do you answer safely?

---

## 17. Strong Answer

1. I would avoid classic global mutable Singleton.
2. If configuration is immutable after startup, a single shared instance is reasonable.
3. I would let the DI container manage it as a singleton-scoped dependency.
4. Services should receive it through constructors instead of calling a global accessor.
5. This keeps lifecycle efficient while preserving testability.
6. If configuration changes dynamically, I would use a thread-safe refresh model or config provider abstraction.

---

## 18. Revision Notes

- One-line summary: Singleton controls one instance, but global mutable access is dangerous.
- Three keywords: lifecycle, global state, injection
- One interview trap: defending Singleton without mentioning testability and hidden coupling
- One memory trick: one instance is fine; invisible dependency is the problem

---

# Topic 4: Adapter

> Track: 2.2 High-Signal Design Patterns
> Scope: interface conversion, legacy integration, third-party SDK isolation, and domain boundary protection

---

## 1. Intuition

Think of a travel adapter plug.

Your laptop charger works, the wall socket works, but their shapes do not match. The adapter does not change electricity. It changes the interface.

Adapter does the same in code.

Short memory trick:
- existing thing works
- interface does not match
- adapter translates

---

## 2. Definition

- Definition: Adapter is a structural pattern that converts one interface into another interface expected by the client.
- Category: Structural design pattern
- Core idea: Let incompatible classes work together without changing either side deeply.

Adapter is often used at boundaries:
- external API to domain interface
- legacy system to new service interface
- third-party SDK to internal contract

---

## 3. Why It Exists

Real systems rarely start from a clean slate.

Problems:
- third-party SDK has awkward method names
- legacy service returns different data shape
- domain code should not depend on vendor classes
- provider API changes should not ripple everywhere

Adapter exists to keep external weirdness at the boundary.

Without Adapter:
- vendor-specific code leaks into business logic
- swapping providers becomes painful
- tests must understand external SDK objects

---

## 4. Reality

Adapters appear in:

- payment gateways wrapping Stripe, Adyen, PayPal
- inventory adapters wrapping legacy hotel inventory APIs
- storage adapters wrapping S3, GCS, and local filesystem
- analytics adapters wrapping vendor event SDKs
- identity adapters wrapping OAuth providers
- database adapters wrapping old schemas

Architecture phrase:
- Adapter is common in hexagonal architecture as an inbound or outbound adapter.

---

## 5. How It Works

Typical flow:

1. Client defines or depends on a clean internal interface.
2. External system exposes a different interface.
3. Adapter implements the internal interface.
4. Adapter calls the external API internally.
5. Adapter translates request and response models.
6. Client stays isolated from external details.

Important design point:
- Adapter should translate, not become a full business workflow

---

## 6. What Problem It Solves

- Primary problem solved: incompatible interfaces between client and existing system.
- Secondary benefits: vendor isolation, easier testing, cleaner domain code.
- Systems impact: reduces migration cost when external systems change.

Adapter protects the core domain from:
- awkward names
- vendor DTOs
- legacy schemas
- provider-specific exceptions
- protocol differences

---

## 7. When to Rely on It

Use Adapter when:

- integrating with third-party SDKs
- migrating from legacy systems
- internal model differs from external model
- you want provider-specific code in one place
- you need to test domain logic without vendor SDKs

Interview trigger words:
- legacy integration
- third-party provider
- incompatible API
- wrapper around SDK
- anti-corruption layer

---

## 8. When Not to Use It

Avoid Adapter when:

- interfaces already match cleanly
- adapter only renames a method with no boundary value
- adapter hides important failure behavior
- adapter becomes a dumping ground for business rules
- you control both sides and can design a clean shared interface

Use direct dependency when there is no meaningful mismatch.

Use Facade when the problem is subsystem simplification, not interface incompatibility.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Isolates external APIs | Adds another layer to trace |
| Keeps domain clean | Can hide provider-specific limitations |
| Helps migrations | Mapping code can become verbose |
| Improves testability | Bad adapters can leak external models anyway |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: clean internal contract and provider isolation.
- Give up: extra mapping code.
- Complexity impact: low for one provider, medium when supporting many providers.
- Failure impact: adapter must translate errors carefully.

### Common Mistakes

- Mistake: return vendor DTOs from adapter.
- Why it is wrong: external model leaks into the domain.
- Better approach: map to internal domain DTOs.

- Mistake: put business decisions in adapter.
- Why it is wrong: adapter becomes hard to reuse and test.
- Better approach: keep business logic in services or domain objects.

- Mistake: swallow provider errors.
- Why it is wrong: callers cannot respond correctly.
- Better approach: translate external errors into meaningful internal exceptions or results.

---

## 11. Key Numbers

Pattern heuristics:

- One external provider with awkward API: adapter is useful.
- Two or more providers for same capability: adapter or gateway interface is usually necessary.
- More than 3 fields mapped repeatedly: centralize mapping in adapter.
- Provider SDK used in more than one domain service: strong smell that adapter is missing.

Memory number:
- If vendor types cross more than one boundary, add an adapter.

---

## 12. Failure Modes

- Mapping bug: external field is mapped to wrong internal field.
- Semantic mismatch: provider status `SETTLED` does not equal internal status `CONFIRMED`.
- Error loss: adapter converts all provider errors into generic failure.
- Retry confusion: adapter hides whether an operation is safe to retry.
- Provider leakage: internal services start depending on vendor-specific values.

Mitigations:
- test mapping explicitly
- document semantic translations
- preserve meaningful error categories
- keep vendor objects inside adapter package

---

## 13. Scenario

- Product / system: Payment integration for hotel checkout
- Requirement: internal checkout expects `PaymentGateway.charge`, but vendor SDK exposes `createPaymentIntent`
- Good design: `StripePaymentAdapter` implements internal `PaymentGateway`
- Why this pattern fits: vendor interface does not match domain interface
- What would go wrong without it: checkout becomes coupled to Stripe-specific SDK classes

---

## 14. Java Code Sample

### Payment adapter around vendor SDK

```java
record PaymentRequest(String userId, int amountInCents) {
}

record PaymentResult(boolean approved, String reference) {
}

interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);
}

class StripeSdkClient {
    String createPaymentIntent(String customerId, int cents) {
        return "stripe-payment-intent-123";
    }
}

class StripePaymentAdapter implements PaymentGateway {
    private final StripeSdkClient stripeSdkClient;

    StripePaymentAdapter(StripeSdkClient stripeSdkClient) {
        this.stripeSdkClient = stripeSdkClient;
    }

    public PaymentResult charge(PaymentRequest request) {
        // LLD concept: Adapter translates internal request shape to vendor SDK call.
        String paymentIntentId = stripeSdkClient.createPaymentIntent(request.userId(), request.amountInCents());
        // LLD concept: Adapter returns internal result shape, not the vendor SDK object.
        return new PaymentResult(true, paymentIntentId);
    }
}
```

Key idea:
- adapter keeps checkout code independent from vendor method names and DTOs

---

## 15. Python Mini Program / Simulation

This mini program shows a legacy inventory API being adapted to a clean domain interface.

```python
from typing import Protocol


class InventoryGateway(Protocol):
    def rooms_available(self, hotel_id: str) -> int:
        pass


class LegacyInventoryApi:
    def fetch_room_count(self, property_code: str) -> dict[str, int]:
        return {"availableRoomCount": 5}


class LegacyInventoryAdapter:
    def __init__(self, legacy_api: LegacyInventoryApi) -> None:
        self.legacy_api = legacy_api

    def rooms_available(self, hotel_id: str) -> int:
        # LLD concept: Adapter converts the clean domain call into the legacy API call.
        response = self.legacy_api.fetch_room_count(property_code=hotel_id)
        # LLD concept: Adapter maps legacy response shape into the internal contract.
        return response["availableRoomCount"]


def show_inventory(gateway: InventoryGateway) -> None:
    print(gateway.rooms_available("hotel-7"))


if __name__ == "__main__":
    show_inventory(LegacyInventoryAdapter(LegacyInventoryApi()))
```

What this demonstrates:
- domain code uses `InventoryGateway`
- legacy naming stays inside adapter
- response mapping is centralized

---

## 16. Practical Question

> You are integrating a new payment provider whose SDK does not match your existing checkout interface. How would Adapter help?

---

## 17. Strong Answer

1. I would keep checkout dependent on an internal `PaymentGateway` interface.
2. I would create a provider-specific adapter, such as `StripePaymentAdapter`.
3. The adapter would translate internal request objects into SDK calls.
4. It would map provider responses and errors back into internal result types.
5. This isolates vendor code and makes future provider changes easier.
6. I would ensure the adapter does not contain checkout business rules.

---

## 18. Revision Notes

- One-line summary: Adapter converts an incompatible external interface into the interface your code expects.
- Three keywords: conversion, boundary, compatibility
- One interview trap: confusing Adapter with Facade
- One memory trick: Adapter changes shape; Facade hides complexity

---

# Topic 5: Decorator

> Track: 2.2 High-Signal Design Patterns
> Scope: runtime behavior extension, wrapping, composition, cross-cutting behavior, and avoiding subclass explosion

---

## 1. Intuition

Think of adding options to a hotel room.

You start with a standard room. Then you add breakfast, ocean view, late checkout, and loyalty discount. You should not need a subclass for every combination.

Decorator wraps an object with extra behavior while keeping the same interface.

Short memory trick:
- same interface
- wrapped object
- extra behavior before or after

---

## 2. Definition

- Definition: Decorator is a structural pattern that dynamically adds responsibilities to an object by wrapping it with another object that implements the same interface.
- Category: Structural design pattern
- Core idea: Prefer composition over subclassing for optional behavior layers.

Decorator is useful when behavior combinations are flexible.

Examples:
- base price plus tax plus discount
- notifier plus retry plus metrics
- stream plus buffering plus compression

---

## 3. Why It Exists

Inheritance creates too many classes when behavior combinations multiply.

Without Decorator:
- `BreakfastRoom`
- `BreakfastOceanViewRoom`
- `BreakfastOceanViewLateCheckoutRoom`
- `DiscountedBreakfastOceanViewLateCheckoutRoom`

This is subclass explosion.

Decorator exists so behavior can be stacked at runtime.

---

## 4. Reality

Decorator appears in:

- Java I/O streams
- middleware chains
- request filters
- retry wrappers
- caching wrappers
- logging wrappers
- metrics instrumentation
- price calculation layers
- authorization wrappers

Production examples:
- wrap a payment gateway with retry
- wrap repository with cache
- wrap notifier with metrics and fallback

---

## 5. How It Works

Typical flow:

1. Define a component interface.
2. Implement a base component.
3. Create decorators that implement the same interface.
4. Each decorator stores another component.
5. Decorator calls wrapped component and adds behavior.
6. Client uses the final wrapped object as the same interface.

Important design point:
- decorator should preserve the contract of the wrapped object

---

## 6. What Problem It Solves

- Primary problem solved: adding optional behavior without subclass explosion.
- Secondary benefits: runtime composition, open/closed design, reusable cross-cutting layers.
- Systems impact: flexible feature assembly with localized changes.

Decorator is especially useful when:
- behavior can be layered
- order matters
- feature combinations vary by tenant or config

---

## 7. When to Rely on It

Use Decorator when:

- you need optional behavior around a core object
- behavior can be added before or after the main call
- you want many combinations without many subclasses
- wrappers should be transparent to the caller
- you are adding cross-cutting concerns like logging, metrics, retry, caching

Interview trigger words:
- add behavior dynamically
- wrapper
- avoid subclass explosion
- same interface
- middleware

---

## 8. When Not to Use It

Avoid Decorator when:

- the behavior fundamentally changes the contract
- wrapper order becomes too confusing
- you need a simpler direct class
- the decoration hides important side effects
- there are only one or two fixed variants

Use Strategy when the goal is swapping algorithms.

Use Proxy when the goal is access control, lazy loading, or remote boundary behavior.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Avoids subclass explosion | Wrapper chains can be hard to debug |
| Adds behavior at runtime | Order of decorators can matter |
| Keeps same interface | Too many small classes can feel noisy |
| Supports open/closed principle | Side effects may become less obvious |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: behavior composition without modifying core class.
- Give up: simple linear call stack becomes layered.
- Complexity impact: low with few decorators, higher with deep chains.
- Performance impact: each wrapper adds a method call and possible work.

### Common Mistakes

- Mistake: decorator changes return semantics unexpectedly.
- Why it is wrong: caller expects same component contract.
- Better approach: preserve interface behavior and document added effects.

- Mistake: use decorator when a simple helper function would do.
- Why it is wrong: adds object structure without real composition need.
- Better approach: use decorator for reusable optional layers.

- Mistake: order-sensitive decorators without tests.
- Why it is wrong: tax then discount may differ from discount then tax.
- Better approach: test important wrapper orders explicitly.

---

## 11. Key Numbers

Pattern heuristics:

- Two optional behaviors: decorator may help if combinations vary.
- Three or more independent optional behaviors: decorator often prevents subclass explosion.
- More than 4 to 5 wrappers in one chain: debugability becomes a concern.
- Any wrapper with external I/O: consider timeout and failure behavior.

Memory number:
- If combinations grow multiplicatively, composition beats inheritance.

---

## 12. Failure Modes

- Wrong order: discount and tax produce unexpected price.
- Broken contract: decorator returns incompatible result.
- Hidden latency: metrics, cache, or retry wrapper adds delay.
- Exception handling confusion: wrapper catches errors the caller needed.
- Debugging difficulty: stack trace passes through many wrappers.

Mitigations:
- keep decorators small
- document ordering assumptions
- test chain composition
- expose wrapper chain in configuration or logs when useful

---

## 13. Scenario

- Product / system: Hotel room price calculator
- Requirement: start with base price, optionally add breakfast, tax, and loyalty discount
- Good design: each option is a decorator over `PriceCalculator`
- Why this pattern fits: options can be combined differently per booking
- What would go wrong without it: subclasses would multiply for every feature combination

---

## 14. Java Code Sample

### Price calculator decorators

```java
interface PriceCalculator {
    int priceFor(int nights);
}

class BaseRoomPrice implements PriceCalculator {
    public int priceFor(int nights) {
        return nights * 150;
    }
}

class BreakfastDecorator implements PriceCalculator {
    private final PriceCalculator wrapped;

    BreakfastDecorator(PriceCalculator wrapped) {
        // LLD concept: Decorator stores the same interface it implements.
        this.wrapped = wrapped;
    }

    public int priceFor(int nights) {
        // LLD concept: extra behavior is layered around the wrapped object.
        return wrapped.priceFor(nights) + (nights * 20);
    }
}

class LoyaltyDiscountDecorator implements PriceCalculator {
    private final PriceCalculator wrapped;

    LoyaltyDiscountDecorator(PriceCalculator wrapped) {
        this.wrapped = wrapped;
    }

    public int priceFor(int nights) {
        int original = wrapped.priceFor(nights);
        return (int) (original * 0.90);
    }
}

class PricingDemo {
    public static void main(String[] args) {
        PriceCalculator calculator = new LoyaltyDiscountDecorator(
                new BreakfastDecorator(new BaseRoomPrice())
        );
        System.out.println(calculator.priceFor(2));
    }
}
```

Key idea:
- decorators keep the same interface while adding optional behavior layers

---

## 15. Python Mini Program / Simulation

This mini program shows runtime composition of pricing behavior.

```python
from typing import Protocol


class PriceCalculator(Protocol):
    def price_for(self, nights: int) -> int:
        pass


class BaseRoomPrice:
    def price_for(self, nights: int) -> int:
        return nights * 150


class BreakfastDecorator:
    def __init__(self, wrapped: PriceCalculator) -> None:
        # LLD concept: decorator wraps the same interface it exposes.
        self.wrapped = wrapped

    def price_for(self, nights: int) -> int:
        return self.wrapped.price_for(nights) + nights * 20


class LoyaltyDiscountDecorator:
    def __init__(self, wrapped: PriceCalculator) -> None:
        self.wrapped = wrapped

    def price_for(self, nights: int) -> int:
        # LLD concept: decorator adds behavior without modifying BaseRoomPrice.
        return int(self.wrapped.price_for(nights) * 0.9)


def main() -> None:
    calculator: PriceCalculator = LoyaltyDiscountDecorator(BreakfastDecorator(BaseRoomPrice()))
    print(calculator.price_for(2))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- behavior is layered at runtime
- caller still sees one `PriceCalculator`
- no subclass is needed for every combination

---

## 16. Practical Question

> You are designing room pricing with optional breakfast, tax, coupon, and loyalty discount. Why might Decorator be better than inheritance?

---

## 17. Strong Answer

1. I would use Decorator if pricing features can be combined independently.
2. I would define a `PriceCalculator` interface.
3. Base pricing would implement it, and each optional feature would wrap it.
4. This avoids subclass explosion for every combination.
5. The trade-off is wrapper ordering, so I would test important combinations.
6. If pricing is just one selected algorithm, Strategy might be simpler.

---

## 18. Revision Notes

- One-line summary: Decorator adds optional behavior by wrapping the same interface.
- Three keywords: wrapper, same interface, composition
- One interview trap: confusing Decorator with Proxy
- One memory trick: Decorator adds; Proxy controls

---

# Topic 6: Facade

> Track: 2.2 High-Signal Design Patterns
> Scope: subsystem simplification, workflow boundary, client-facing API design, and reducing call-site complexity

---

## 1. Intuition

Think of a hotel concierge.

You ask for a complete airport pickup, dinner reservation, and room upgrade. The concierge talks to transport, restaurant, billing, and room allocation. You do not call each department yourself.

Facade is the concierge of a subsystem.

Short memory trick:
- many subsystem calls
- one simple entry point
- hides orchestration complexity

---

## 2. Definition

- Definition: Facade is a structural pattern that provides a simplified high-level interface over a complex subsystem.
- Category: Structural design pattern
- Core idea: Make a subsystem easier to use by exposing one clean API.

Facade does not necessarily change interfaces like Adapter.

Facade hides complexity.

---

## 3. Why It Exists

Without Facade, clients must understand too much.

Bad call-site symptoms:
- call inventory service
- call pricing service
- call payment service
- call booking repository
- call notification service
- handle ordering and rollback manually

Every client becomes an expert in subsystem internals.

Facade exists to give clients a simpler contract.

---

## 4. Reality

Facades appear in:

- checkout services
- travel booking APIs
- report generation services
- SDK client classes
- payment orchestration entry points
- account onboarding services
- file export services

Architecture examples:
- service layer facade over domain services
- API facade over multiple internal services
- SDK facade over many low-level endpoints

---

## 5. How It Works

Typical flow:

1. Subsystem has several collaborating classes.
2. Facade receives a high-level request.
3. Facade calls subsystem components in correct order.
4. Facade handles simple coordination and error translation.
5. Client receives a clean response.
6. Subsystem remains available for advanced internal usage if needed.

Important design point:
- Facade should simplify access, not become an enormous god class

---

## 6. What Problem It Solves

- Primary problem solved: clients knowing too much about subsystem complexity.
- Secondary benefits: cleaner APIs, fewer duplicated workflows, lower coupling.
- Systems impact: reduces repeated orchestration and makes call sites easier to evolve.

Facade is especially useful when:
- multiple steps must happen in order
- many clients repeat the same call sequence
- external callers need a stable high-level API

---

## 7. When to Rely on It

Use Facade when:

- subsystem has many classes or steps
- clients repeat the same workflow
- you need a stable API over changing internals
- you want to hide complexity from external teams
- you are designing SDK or service-layer entry points

Interview trigger words:
- simplify subsystem
- one API for many operations
- hide complexity
- orchestration entry point
- client should not know internals

---

## 8. When Not to Use It

Avoid Facade when:

- subsystem is already simple
- facade becomes a god object
- clients need fine-grained control
- facade hides too many failures
- facade duplicates logic from domain services

Use Adapter when the issue is interface incompatibility.

Use a domain service when the main issue is business behavior, not subsystem simplification.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Simplifies client code | Can become a god class |
| Reduces duplicated call sequences | May hide important details |
| Provides stable boundary | Can limit advanced use cases |
| Easier onboarding for callers | Adds another abstraction layer |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: easier client interaction and fewer duplicated workflows.
- Give up: some direct control over subsystem calls.
- Complexity impact: lower at call sites, extra responsibility in facade.
- Failure impact: facade must decide what errors to expose and how.

### Common Mistakes

- Mistake: put all system logic into one facade.
- Why it is wrong: facade becomes a god class.
- Better approach: facade coordinates focused subsystem services.

- Mistake: hide all errors behind a generic message.
- Why it is wrong: clients cannot recover or retry correctly.
- Better approach: expose meaningful failure categories.

- Mistake: confuse Facade with Adapter.
- Why it is wrong: Adapter solves interface mismatch; Facade solves complexity.
- Better approach: choose based on the actual problem.

---

## 11. Key Numbers

Pattern heuristics:

- More than 3 repeated subsystem calls at many call sites: facade may help.
- More than 2 clients duplicating the same workflow: consider facade.
- More than 7 to 10 methods on facade: check if it is becoming too broad.
- More than one business domain inside facade: split by use case.

Memory number:
- Facade is useful when caller knowledge of internals becomes the problem.

---

## 12. Failure Modes

- God facade: one class knows everything.
- Hidden failures: clients cannot distinguish payment failure from inventory failure.
- Over-simplification: facade blocks valid advanced use cases.
- Tight coupling: facade directly depends on too many low-level details.
- Transaction confusion: facade calls multiple services without clear compensation.

Mitigations:
- keep facade use-case focused
- expose meaningful result objects
- delegate real work to cohesive services
- document ordering and failure behavior

---

## 13. Scenario

- Product / system: Hotel checkout API
- Requirement: reserve inventory, calculate price, charge payment, save booking, and notify user
- Good design: `CheckoutFacade` provides one `checkout` method over the subsystem
- Why this pattern fits: clients should not orchestrate internal booking steps
- What would go wrong without it: each client duplicates the workflow and handles errors inconsistently

---

## 14. Java Code Sample

### Checkout facade over subsystem services

```java
record CheckoutRequest(String userId, String hotelId, int nights) {
}

record CheckoutResult(String bookingId) {
}

class InventoryService {
    void reserve(String hotelId, int nights) {
        System.out.println("reserved inventory");
    }
}

class PricingService {
    int price(CheckoutRequest request) {
        return request.nights() * 150;
    }
}

class PaymentService {
    String charge(String userId, int amount) {
        return "payment-ref-1";
    }
}

class BookingRepository {
    String save(CheckoutRequest request, String paymentReference) {
        return "booking-1";
    }
}

class CheckoutFacade {
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    CheckoutFacade(
            InventoryService inventoryService,
            PricingService pricingService,
            PaymentService paymentService,
            BookingRepository bookingRepository) {
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
    }

    public CheckoutResult checkout(CheckoutRequest request) {
        // LLD concept: Facade gives clients one high-level operation over several subsystem calls.
        inventoryService.reserve(request.hotelId(), request.nights());
        int amount = pricingService.price(request);
        String paymentReference = paymentService.charge(request.userId(), amount);
        String bookingId = bookingRepository.save(request, paymentReference);
        return new CheckoutResult(bookingId);
    }
}
```

Key idea:
- facade hides subsystem ordering from clients while delegating actual work

---

## 15. Python Mini Program / Simulation

This mini program shows one clean call over several internal services.

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class CheckoutRequest:
    user_id: str
    hotel_id: str
    nights: int


class InventoryService:
    def reserve(self, request: CheckoutRequest) -> None:
        print(f"reserved {request.hotel_id}")


class PricingService:
    def price(self, request: CheckoutRequest) -> int:
        return request.nights * 150


class PaymentService:
    def charge(self, user_id: str, amount: int) -> str:
        print(f"charged {user_id}: {amount}")
        return "payment-ref-1"


class CheckoutFacade:
    def __init__(self) -> None:
        self.inventory = InventoryService()
        self.pricing = PricingService()
        self.payments = PaymentService()

    def checkout(self, request: CheckoutRequest) -> str:
        # LLD concept: Facade hides the multi-step subsystem workflow behind one method.
        self.inventory.reserve(request)
        amount = self.pricing.price(request)
        payment_reference = self.payments.charge(request.user_id, amount)
        return f"booking confirmed with {payment_reference}"


def main() -> None:
    facade = CheckoutFacade()
    print(facade.checkout(CheckoutRequest("user-1", "hotel-7", 2)))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- client calls one method
- subsystem details stay behind the facade
- internal services remain focused

---

## 16. Practical Question

> You are designing checkout where mobile, web, and partner APIs all need the same booking workflow. Would Facade help?

---

## 17. Strong Answer

1. I would use a checkout facade to expose one high-level checkout operation.
2. The facade would coordinate inventory, pricing, payment, booking persistence, and notification.
3. Clients would not duplicate subsystem call ordering.
4. Internal services remain separate and cohesive.
5. The trade-off is that the facade can become too large, so I would keep it use-case focused.
6. I would expose meaningful errors for payment, inventory, and validation failures.

---

## 18. Revision Notes

- One-line summary: Facade gives a simple high-level API over a complex subsystem.
- Three keywords: simplify, subsystem, entry point
- One interview trap: turning facade into a god class
- One memory trick: Facade is a concierge, not every department

---

# Topic 7: Proxy

> Track: 2.2 High-Signal Design Patterns
> Scope: access control, lazy loading, caching, remote calls, protection, and same-interface indirection

---

## 1. Intuition

Think of a hotel security desk.

You want to access a VIP floor. The security desk uses the same request language: "open access for this guest." But it checks permission before forwarding the request.

Proxy stands between caller and real object to control access or lifecycle.

Short memory trick:
- same interface
- controls access
- forwards to real object

---

## 2. Definition

- Definition: Proxy is a structural pattern that provides a substitute object controlling access to another object through the same interface.
- Category: Structural design pattern
- Core idea: Add control around an object without changing the caller interface.

Types of proxy:
- protection proxy: authorization checks
- virtual proxy: lazy loading
- caching proxy: cache reads
- remote proxy: represents object in another process
- logging proxy: tracks calls, often similar to decorator

---

## 3. Why It Exists

Sometimes direct access is unsafe, expensive, or impossible.

Problems:
- document should be read only by authorized users
- image should load only when displayed
- remote service call should look local to caller
- expensive object should be created lazily
- repeated reads should be cached

Proxy exists to protect the real object or delay its work.

---

## 4. Reality

Proxies appear in:

- Spring AOP proxies
- ORM lazy-loading proxies
- remote service client stubs
- API gateway authorization wrappers
- cache wrappers around repositories
- file access protection
- image lazy loading in UI systems

Difference from Decorator:
- Decorator adds behavior
- Proxy controls access or lifecycle

---

## 5. How It Works

Typical flow:

1. Define a subject interface.
2. Real object implements the interface.
3. Proxy also implements the same interface.
4. Caller receives proxy instead of real object.
5. Proxy checks, caches, delays, or forwards.
6. Real object is called only when proxy allows it.

Important design point:
- proxy should be transparent unless control behavior intentionally changes outcome

---

## 6. What Problem It Solves

- Primary problem solved: controlled access to another object.
- Secondary benefits: lazy loading, caching, authorization, remote abstraction.
- Systems impact: protects expensive or sensitive resources behind stable interfaces.

Proxy is especially useful when:
- real object is expensive
- access requires permission
- real object lives remotely
- repeated calls can be cached

---

## 7. When to Rely on It

Use Proxy when:

- you need authorization before access
- you need lazy initialization
- you need cache in front of expensive reads
- you need to represent remote resource locally
- you need to control lifecycle of the real object

Interview trigger words:
- access control
- lazy loading
- remote object
- cache wrapper
- placeholder
- protection

---

## 8. When Not to Use It

Avoid Proxy when:

- direct access is simple and safe
- proxy hides expensive remote calls too much
- proxy adds surprising behavior
- proxy caches data without clear invalidation
- access control belongs at a different layer

Use Decorator when the main goal is optional added responsibility.

Use Adapter when the interface is incompatible.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Controls access cleanly | Adds indirection |
| Supports lazy loading | Can hide latency |
| Enables caching | Cache invalidation can be hard |
| Keeps same interface | Can surprise callers if behavior differs |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: control without changing caller code.
- Give up: direct visibility into when real object is called.
- Complexity impact: low for simple protection, higher for remote or caching proxies.
- Performance impact: can improve or hurt depending on cache and checks.

### Common Mistakes

- Mistake: proxy hides remote network latency completely.
- Why it is wrong: caller may assume local cheap call.
- Better approach: name remote clients clearly and expose timeouts.

- Mistake: caching proxy without invalidation strategy.
- Why it is wrong: stale data can become correctness bug.
- Better approach: define TTL, invalidation, or consistency expectations.

- Mistake: proxy performs business workflow.
- Why it is wrong: proxy should control access, not own business process.
- Better approach: keep workflow in services.

---

## 11. Key Numbers

Pattern heuristics:

- Expensive object creation above tens of milliseconds: virtual proxy may help.
- Repeated read-heavy call with stable data: caching proxy may help.
- Security-sensitive object: protection proxy can centralize checks.
- Remote call hidden by proxy: always think timeout, retry, and circuit breaker.

Memory number:
- Proxy is justified when direct access needs a gate.

---

## 12. Failure Modes

- Authorization bug: proxy permits access incorrectly.
- Stale cache: proxy returns old data.
- Hidden remote failure: caller sees normal method but network fails.
- Lazy load spike: many proxies load real objects at once.
- Inconsistent behavior: proxy and real object do not honor same interface contract.

Mitigations:
- test proxy and real object contract
- define cache TTL and invalidation
- expose remote failure categories
- keep permission checks explicit and auditable

---

## 13. Scenario

- Product / system: Document access service for booking invoices
- Requirement: users can download invoices only for bookings they own
- Good design: `AuthorizedInvoiceProxy` checks ownership before calling real invoice store
- Why this pattern fits: access to real object must be controlled
- What would go wrong without it: callers might bypass authorization and read sensitive documents

---

## 14. Java Code Sample

### Authorization proxy for invoice access

```java
interface InvoiceStore {
    String downloadInvoice(String userId, String bookingId);
}

class RealInvoiceStore implements InvoiceStore {
    public String downloadInvoice(String userId, String bookingId) {
        return "invoice-pdf-bytes-for-" + bookingId;
    }
}

class BookingAccessPolicy {
    boolean canAccess(String userId, String bookingId) {
        return bookingId.startsWith(userId + "-");
    }
}

class AuthorizedInvoiceProxy implements InvoiceStore {
    private final InvoiceStore realStore;
    private final BookingAccessPolicy accessPolicy;

    AuthorizedInvoiceProxy(InvoiceStore realStore, BookingAccessPolicy accessPolicy) {
        // LLD concept: Proxy implements the same interface and wraps the real object.
        this.realStore = realStore;
        this.accessPolicy = accessPolicy;
    }

    public String downloadInvoice(String userId, String bookingId) {
        // LLD concept: Proxy controls access before delegating to the real object.
        if (!accessPolicy.canAccess(userId, bookingId)) {
            throw new SecurityException("not allowed");
        }
        return realStore.downloadInvoice(userId, bookingId);
    }
}
```

Key idea:
- proxy looks like the real object to the caller but adds a gate before access

---

## 15. Python Mini Program / Simulation

This mini program shows lazy loading with a virtual proxy.

```python
from typing import Protocol


class Image(Protocol):
    def display(self) -> None:
        pass


class RealImage:
    def __init__(self, file_name: str) -> None:
        print(f"loading large file {file_name}")
        self.file_name = file_name

    def display(self) -> None:
        print(f"displaying {self.file_name}")


class LazyImageProxy:
    def __init__(self, file_name: str) -> None:
        self.file_name = file_name
        self._real_image: RealImage | None = None

    def display(self) -> None:
        # LLD concept: Proxy delays expensive object creation until the real work is needed.
        if self._real_image is None:
            self._real_image = RealImage(self.file_name)
        self._real_image.display()


def main() -> None:
    image: Image = LazyImageProxy("hotel-room.png")
    print("proxy created, image not loaded yet")
    image.display()
    image.display()


if __name__ == "__main__":
    main()
```

What this demonstrates:
- proxy and real object share the same interface
- real object is created lazily
- caller does not manage lifecycle manually

---

## 16. Practical Question

> You are designing invoice downloads with authorization checks. Would you use Proxy, Decorator, or Adapter?

---

## 17. Strong Answer

1. I would use Proxy because the main goal is controlled access to the real invoice store.
2. The proxy and real store would implement the same interface.
3. The proxy would check user authorization before forwarding the request.
4. This keeps authorization near the protected resource.
5. Decorator would fit better if I were adding optional behavior like logging.
6. Adapter would fit if the invoice store interface were incompatible.

---

## 18. Revision Notes

- One-line summary: Proxy controls access to another object through the same interface.
- Three keywords: gate, lazy, access
- One interview trap: confusing Proxy with Decorator
- One memory trick: Proxy controls; Decorator adds

---

# Topic 8: Strategy

> Track: 2.2 High-Signal Design Patterns
> Scope: interchangeable algorithms, policy injection, open/closed design, and avoiding conditional-heavy logic

---

## 1. Intuition

Think of choosing a route to the airport.

You can choose fastest route, cheapest route, scenic route, or avoid-tolls route. The destination is the same, but the algorithm changes.

Strategy is a replaceable algorithm.

Short memory trick:
- same goal
- different algorithms
- chosen from outside

---

## 2. Definition

- Definition: Strategy is a behavioral pattern that defines a family of algorithms, encapsulates each one, and makes them interchangeable.
- Category: Behavioral design pattern
- Core idea: Replace conditional algorithm selection with injected behavior.

Strategy is about how work is done.

Factory may create a strategy, but Strategy performs the behavior.

---

## 3. Why It Exists

Without Strategy, algorithm branching often piles up in one class.

Bad signs:
- `if loyaltyTier == GOLD`
- `else if season == PEAK`
- `else if channel == PARTNER`
- giant pricing method grows forever

Strategy exists to put each algorithm in its own class.

This supports open/closed principle:
- add a new strategy without editing the core context class

---

## 4. Reality

Strategy appears in:

- pricing policies
- routing algorithms
- ranking algorithms
- discount calculations
- retry policies
- compression algorithms
- payment fee calculators
- fraud scoring models

Framework examples:
- comparator strategies
- pluggable authentication providers
- retry policy objects

---

## 5. How It Works

Typical flow:

1. Define a strategy interface.
2. Implement each algorithm as a separate strategy.
3. Context class receives a strategy.
4. Context delegates algorithm-specific work to strategy.
5. Caller or factory chooses strategy based on config or request.
6. New algorithms are added as new strategy classes.

Important design point:
- context should not inspect the concrete strategy type

---

## 6. What Problem It Solves

- Primary problem solved: algorithm variation inside a stable workflow.
- Secondary benefits: open/closed design, easier tests, cleaner responsibilities.
- Systems impact: allows business rules to change without rewriting orchestration.

Strategy is especially useful when:
- there are multiple ways to calculate, route, rank, retry, or validate
- algorithm choice is config-driven
- new algorithms are expected

---

## 7. When to Rely on It

Use Strategy when:

- algorithm varies independently from the object using it
- conditional logic is mostly choosing behavior
- each algorithm can be tested separately
- you need runtime selection
- you want to add algorithms without changing context

Interview trigger words:
- pluggable algorithm
- pricing policy
- ranking strategy
- routing strategy
- retry policy
- discount calculation

---

## 8. When Not to Use It

Avoid Strategy when:

- there is only one simple algorithm
- branches are tiny and unlikely to grow
- behavior depends on object lifecycle state, which may be State pattern
- strategy objects need too much context and become tightly coupled
- the abstraction name is too vague, such as `Handler` for everything

Use simple conditional logic when variation is small and stable.

Use State when behavior changes because the object itself changes state.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Replaces large conditionals | Adds more classes |
| Supports open/closed principle | Strategy selection still needs a place |
| Makes algorithms testable | Too many tiny strategies can fragment logic |
| Enables runtime behavior changes | Poor strategy boundaries can leak context |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: algorithm isolation and extensibility.
- Give up: direct inline visibility of all branches.
- Complexity impact: good when algorithms are real, noisy when branches are trivial.
- Testing impact: easier unit tests for each algorithm.

### Common Mistakes

- Mistake: use Strategy for two one-line branches.
- Why it is wrong: abstraction costs more than it saves.
- Better approach: use Strategy when algorithms are meaningful or growing.

- Mistake: context still checks concrete strategy type.
- Why it is wrong: defeats polymorphism.
- Better approach: strategy interface should expose the needed behavior.

- Mistake: confuse Strategy with State.
- Why it is wrong: strategy is usually selected externally; state changes internally.
- Better approach: identify who controls behavior selection.

---

## 11. Key Numbers

Pattern heuristics:

- One algorithm: no strategy needed.
- Two algorithms: strategy helps if they are meaningful or likely to grow.
- Three or more algorithms: strategy is usually cleaner than conditionals.
- Algorithm over 10 to 15 lines: separate strategy improves readability.
- New algorithm expected per tenant or business rule: strategy is strong fit.

Memory number:
- Strategy pays off when behavior changes more often than workflow.

---

## 12. Failure Modes

- Wrong strategy selected: user gets wrong price or route.
- Strategy needs too much context: abstraction boundary is weak.
- Hidden side effects: strategy mutates state unexpectedly.
- Duplicate logic: strategies copy common calculations.
- Inconsistent contracts: one strategy returns values in different units.

Mitigations:
- use clear interface contracts
- keep strategy inputs explicit
- test every strategy with shared contract tests
- centralize common helper logic if needed

---

## 13. Scenario

- Product / system: Hotel pricing engine
- Requirement: support standard pricing, peak-season pricing, and loyalty pricing
- Good design: `PricingStrategy` interface with separate implementations
- Why this pattern fits: pricing algorithm varies while checkout flow stays stable
- What would go wrong without it: checkout accumulates pricing conditionals

---

## 14. Java Code Sample

### Pricing strategy injection

```java
record BookingRequest(int nights, int baseRate, String loyaltyTier) {
}

interface PricingStrategy {
    int calculate(BookingRequest request);
}

class StandardPricingStrategy implements PricingStrategy {
    public int calculate(BookingRequest request) {
        return request.nights() * request.baseRate();
    }
}

class PeakSeasonPricingStrategy implements PricingStrategy {
    public int calculate(BookingRequest request) {
        return (int) (request.nights() * request.baseRate() * 1.30);
    }
}

class CheckoutQuoteService {
    private final PricingStrategy pricingStrategy;

    CheckoutQuoteService(PricingStrategy pricingStrategy) {
        // LLD concept: Strategy is injected so the workflow does not hardcode the algorithm.
        this.pricingStrategy = pricingStrategy;
    }

    public int quote(BookingRequest request) {
        // LLD concept: context delegates the variable algorithm to the strategy.
        return pricingStrategy.calculate(request);
    }
}
```

Key idea:
- checkout flow stays stable while pricing algorithms are replaceable

---

## 15. Python Mini Program / Simulation

This mini program shows the same checkout flow using different pricing algorithms.

```python
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class BookingRequest:
    nights: int
    base_rate: int
    loyalty_tier: str


class PricingStrategy(Protocol):
    def calculate(self, request: BookingRequest) -> int:
        pass


class StandardPricing:
    def calculate(self, request: BookingRequest) -> int:
        return request.nights * request.base_rate


class LoyaltyPricing:
    def calculate(self, request: BookingRequest) -> int:
        subtotal = request.nights * request.base_rate
        return int(subtotal * 0.85) if request.loyalty_tier == "GOLD" else subtotal


class QuoteService:
    def __init__(self, strategy: PricingStrategy) -> None:
        # LLD concept: strategy is selected from outside the context.
        self.strategy = strategy

    def quote(self, request: BookingRequest) -> int:
        return self.strategy.calculate(request)


def main() -> None:
    request = BookingRequest(nights=2, base_rate=200, loyalty_tier="GOLD")
    for strategy in [StandardPricing(), LoyaltyPricing()]:
        service = QuoteService(strategy)
        print(strategy.__class__.__name__, service.quote(request))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- algorithms are interchangeable
- context does not branch by pricing type
- strategy differs from factory because strategy performs behavior

---

## 16. Practical Question

> You are designing a pricing engine with normal, holiday, partner, and loyalty pricing. How would Strategy help?

---

## 17. Strong Answer

1. I would define a `PricingStrategy` interface.
2. Each pricing algorithm would live in its own implementation.
3. Checkout would receive a strategy and delegate price calculation.
4. A factory or configuration layer could select the right strategy.
5. This keeps checkout closed for modification when new pricing rules are added.
6. If there is only one pricing algorithm, I would avoid Strategy until variation appears.

---

## 18. Revision Notes

- One-line summary: Strategy makes algorithms interchangeable behind one contract.
- Three keywords: algorithm, policy, interchangeable
- One interview trap: confusing Strategy with State
- One memory trick: Strategy is selected; State transitions

---

# Topic 9: Observer

> Track: 2.2 High-Signal Design Patterns
> Scope: event notification, one-to-many dependencies, in-process publish-subscribe, listener management, and side-effect isolation

---

## 1. Intuition

Think of a hotel announcement board.

When a booking is confirmed, housekeeping, billing, loyalty, and notification teams may all need to react. The booking service should not manually know every team forever.

Observer lets interested listeners subscribe to events.

Short memory trick:
- subject changes
- observers listen
- notification fans out

---

## 2. Definition

- Definition: Observer is a behavioral pattern where an object publishes events to a list of dependent observers that react to changes.
- Category: Behavioral design pattern
- Core idea: Decouple the event producer from the event consumers.

Observer is usually in-process and synchronous unless designed otherwise.

Distributed event streams are related but operationally different.

---

## 3. Why It Exists

Without Observer, the event producer becomes coupled to every reaction.

Bad signs:
- booking service directly calls email, loyalty, analytics, housekeeping, audit, and recommendation services
- adding one side effect requires editing the core booking flow
- tests for booking confirmation must mock every side effect

Observer exists to let side effects subscribe without bloating the producer.

---

## 4. Reality

Observer appears in:

- UI event listeners
- domain event handlers
- notification listeners
- cache invalidation hooks
- analytics event hooks
- in-process event buses
- file watchers
- reactive streams at small scale

Distributed equivalents:
- Kafka topics
- message queues
- webhooks

But those add durability, retries, and infrastructure beyond classic Observer.

---

## 5. How It Works

Typical flow:

1. Define event object.
2. Define observer/listener interface.
3. Subject maintains list of observers.
4. Observers subscribe or are registered at startup.
5. Subject publishes event when something happens.
6. Observers react independently.

Important design point:
- observer failures must not accidentally break the core flow unless that is intentional

---

## 6. What Problem It Solves

- Primary problem solved: producer coupled to many side-effect consumers.
- Secondary benefits: extensibility, cleaner core flow, pluggable reactions.
- Systems impact: makes new reactions easier to add without changing core producer logic.

Observer is useful when:
- many components care about the same event
- event producer should not know all consumers
- reactions can be added independently

---

## 7. When to Rely on It

Use Observer when:

- one event has multiple reactions
- producer should not depend on consumer classes
- listeners can be added or removed
- side effects are secondary to the main event
- event fanout is local or lightweight

Interview trigger words:
- event listeners
- notify subscribers
- one-to-many
- domain events
- UI callbacks
- publish-subscribe

---

## 8. When Not to Use It

Avoid classic Observer when:

- events must be durable across process crashes
- consumers are distributed services
- event order and retry matter strongly
- observer failure must be isolated operationally
- flow becomes impossible to trace

Use message queue or event stream when durability, replay, and cross-service delivery are required.

Use direct method call when there is exactly one required synchronous dependency.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Decouples producer from consumers | Can make flow harder to trace |
| Easy to add reactions | Listener failures can be tricky |
| Supports one-to-many notification | Ordering may be unclear |
| Keeps core service smaller | Can cause memory leaks if listeners are not removed |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: extensibility and producer decoupling.
- Give up: direct visibility of all side effects from producer code.
- Complexity impact: low in-process, higher with async event buses.
- Failure impact: listener exception policy must be explicit.

### Common Mistakes

- Mistake: use Observer for required transaction steps.
- Why it is wrong: required steps should be explicit in workflow.
- Better approach: use observer for side effects, not mandatory core invariants.

- Mistake: no listener failure handling.
- Why it is wrong: one bad observer can break all notifications.
- Better approach: catch, log, isolate, or define fail-fast policy.

- Mistake: confuse Observer with Kafka.
- Why it is wrong: Kafka provides durability and distributed delivery; Observer does not by default.
- Better approach: choose based on reliability needs.

---

## 11. Key Numbers

Pattern heuristics:

- One consumer: direct call may be simpler.
- Two or more optional consumers: Observer becomes useful.
- More than 5 to 7 listeners: tracing and ordering need attention.
- Cross-process consumers: consider queue or event stream.
- Required delivery: classic Observer is not enough unless explicitly persisted.

Memory number:
- Observer is local fanout, not durable messaging by default.

---

## 12. Failure Modes

- Listener exception stops later listeners.
- Hidden side effects make debugging hard.
- Listener memory leak from forgotten unsubscribe.
- Event storm causes performance issues.
- Ordering dependency appears accidentally between listeners.

Mitigations:
- define listener failure policy
- keep events small and clear
- avoid observer ordering assumptions
- unregister dynamic listeners
- use durable messaging for critical cross-service events

---

## 13. Scenario

- Product / system: Booking confirmation domain event
- Requirement: after booking is confirmed, send email, update loyalty points, and track analytics
- Good design: booking service publishes `BookingConfirmedEvent`; observers react
- Why this pattern fits: multiple side effects care about the same event
- What would go wrong without it: booking service grows with every new side effect

---

## 14. Java Code Sample

### Booking event publisher with observers

```java
import java.util.ArrayList;
import java.util.List;

record BookingConfirmedEvent(String bookingId, String userId) {
}

interface BookingEventListener {
    void onBookingConfirmed(BookingConfirmedEvent event);
}

class EmailListener implements BookingEventListener {
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        System.out.println("email sent to " + event.userId());
    }
}

class LoyaltyListener implements BookingEventListener {
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        System.out.println("loyalty points added for " + event.userId());
    }
}

class BookingEventPublisher {
    private final List<BookingEventListener> listeners = new ArrayList<>();

    public void register(BookingEventListener listener) {
        // LLD concept: observers are registered without changing publisher logic.
        listeners.add(listener);
    }

    public void publish(BookingConfirmedEvent event) {
        for (BookingEventListener listener : listeners) {
            // LLD concept: publisher fans out the event to all observers through one contract.
            listener.onBookingConfirmed(event);
        }
    }
}
```

Key idea:
- producer publishes an event, observers decide how to react

---

## 15. Python Mini Program / Simulation

This mini program shows listeners subscribing to a booking event.

```python
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class BookingConfirmedEvent:
    booking_id: str
    user_id: str


class BookingListener(Protocol):
    def handle(self, event: BookingConfirmedEvent) -> None:
        pass


class EmailListener:
    def handle(self, event: BookingConfirmedEvent) -> None:
        print(f"email sent to {event.user_id}")


class AnalyticsListener:
    def handle(self, event: BookingConfirmedEvent) -> None:
        print(f"analytics tracked for {event.booking_id}")


class EventPublisher:
    def __init__(self) -> None:
        self.listeners: list[BookingListener] = []

    def register(self, listener: BookingListener) -> None:
        # LLD concept: new observers can be added without modifying the publisher.
        self.listeners.append(listener)

    def publish(self, event: BookingConfirmedEvent) -> None:
        for listener in self.listeners:
            # LLD concept: one event fans out to many independent reactions.
            listener.handle(event)


def main() -> None:
    publisher = EventPublisher()
    publisher.register(EmailListener())
    publisher.register(AnalyticsListener())
    publisher.publish(BookingConfirmedEvent("booking-1", "user-1"))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- producer does not know concrete listener details
- adding a listener does not edit the producer
- this is in-process notification, not durable messaging

---

## 16. Practical Question

> Booking confirmation should trigger email, loyalty, analytics, and audit side effects. Would you use Observer or a message queue?

---

## 17. Strong Answer

1. If all reactions are in-process and non-critical, Observer is enough.
2. I would publish a `BookingConfirmedEvent` and register listeners.
3. This keeps booking flow decoupled from optional side effects.
4. I would define listener failure policy clearly.
5. If reactions are cross-service, durable, or need retries, I would use a queue or event stream.
6. I would not use Observer for mandatory transaction steps that must succeed before confirmation.

---

## 18. Revision Notes

- One-line summary: Observer lets many subscribers react to an event without coupling the producer to each one.
- Three keywords: event, listener, fanout
- One interview trap: treating Observer as durable messaging
- One memory trick: Observer notifies; queue persists

---

# Topic 10: State

> Track: 2.2 High-Signal Design Patterns
> Scope: lifecycle-driven behavior, state transitions, transition rules, and replacing conditional state machines with polymorphism

---

## 1. Intuition

Think of a booking lifecycle.

A draft booking can be confirmed. A confirmed booking can be cancelled. A completed booking cannot be cancelled. The same action means different things depending on current state.

State pattern lets the object change behavior when its internal state changes.

Short memory trick:
- object has lifecycle
- behavior depends on state
- state object handles the action

---

## 2. Definition

- Definition: State is a behavioral pattern that allows an object to change its behavior when its internal state changes by delegating behavior to state objects.
- Category: Behavioral design pattern
- Core idea: Move state-specific behavior out of large conditionals and into separate state classes.

State differs from Strategy:
- Strategy is often chosen from outside
- State is controlled by the object lifecycle

---

## 3. Why It Exists

Without State, lifecycle logic becomes a condition-heavy method.

Bad signs:
- `if status == DRAFT` in many methods
- every new status requires editing many conditionals
- invalid transitions are handled inconsistently
- behavior is scattered across services

State exists to centralize behavior for each lifecycle state.

---

## 4. Reality

State appears in:

- booking lifecycle
- order lifecycle
- payment authorization lifecycle
- document workflow
- ticket workflow
- connection states
- game character states
- approval workflows

Common states:
- draft
- pending
- confirmed
- cancelled
- completed
- failed

---

## 5. How It Works

Typical flow:

1. Define a state interface with actions.
2. Implement one class per state.
3. Context object stores current state.
4. Context delegates actions to current state.
5. State object validates action and transitions context when allowed.
6. Invalid transitions fail in the state that rejects them.

Important design point:
- state transition rules belong near state behavior

---

## 6. What Problem It Solves

- Primary problem solved: complex behavior that changes by lifecycle state.
- Secondary benefits: clearer transitions, fewer scattered conditionals, better extensibility.
- Systems impact: reduces bugs in workflows with strict allowed transitions.

State is useful when:
- object behavior changes by status
- transitions have rules
- status conditionals appear in many methods

---

## 7. When to Rely on It

Use State when:

- lifecycle has multiple states
- each state handles actions differently
- invalid transitions matter
- conditionals are repeated across methods
- new states may be added

Interview trigger words:
- lifecycle
- status transitions
- workflow
- state machine
- invalid transition
- behavior depends on current status

---

## 8. When Not to Use It

Avoid State when:

- there are only 2 simple states
- behavior is just display text
- a simple enum and validation method is enough
- state classes would be mostly empty
- state transitions are better represented in a workflow engine

Use enum plus switch for small, stable state machines.

Use workflow engine for long-running distributed workflows with retries and timers.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Removes repeated state conditionals | Adds classes |
| Localizes transition rules | Can be overkill for simple states |
| Makes invalid transitions explicit | State object references context |
| Easier to add state-specific behavior | Harder to see full state machine in one place |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: clearer state-specific behavior and transition rules.
- Give up: single-file visibility of all logic.
- Complexity impact: useful for rich lifecycle, noisy for simple status.
- Testing impact: easier to test each state transition.

### Common Mistakes

- Mistake: create State classes for simple labels.
- Why it is wrong: no behavior is being polymorphically changed.
- Better approach: use enum.

- Mistake: allow state objects to mutate unrelated context fields.
- Why it is wrong: state objects become too powerful.
- Better approach: expose narrow transition methods on context.

- Mistake: confuse State with Strategy.
- Why it is wrong: State changes through lifecycle; Strategy is usually selected externally.
- Better approach: ask who controls behavior selection.

---

## 11. Key Numbers

Pattern heuristics:

- Two simple states: enum is usually enough.
- Three or more states with different behavior: consider State.
- Same `if status` repeated in 3 or more methods: strong State smell.
- More than 5 states: draw a state diagram before coding.
- Long-running distributed transitions: consider workflow engine or saga.

Memory number:
- State helps when status changes behavior, not just data.

---

## 12. Failure Modes

- Missing transition: state does not handle a valid action.
- Invalid transition allowed: cancelled booking becomes confirmed.
- State explosion: too many tiny states with little behavior.
- Hidden transition: state changes without audit or domain event.
- Persistence mismatch: stored status does not map to state class correctly.

Mitigations:
- draw state diagram
- test every transition
- centralize state restoration from persistence
- emit domain events for important transitions

---

## 13. Scenario

- Product / system: Hotel booking lifecycle
- Requirement: draft bookings can confirm, confirmed bookings can cancel, completed bookings cannot cancel
- Good design: booking delegates actions to current state object
- Why this pattern fits: behavior depends on lifecycle state
- What would go wrong without it: state conditionals appear everywhere and invalid transitions slip in

---

## 14. Java Code Sample

### Booking lifecycle with state objects

```java
interface BookingState {
    void confirm(Booking booking);
    void cancel(Booking booking);
    String name();
}

class DraftState implements BookingState {
    public void confirm(Booking booking) {
        // LLD concept: state object owns the transition rule for draft -> confirmed.
        booking.transitionTo(new ConfirmedState());
    }

    public void cancel(Booking booking) {
        booking.transitionTo(new CancelledState());
    }

    public String name() {
        return "DRAFT";
    }
}

class ConfirmedState implements BookingState {
    public void confirm(Booking booking) {
        throw new IllegalStateException("already confirmed");
    }

    public void cancel(Booking booking) {
        // LLD concept: confirmed state defines its own valid cancellation behavior.
        booking.transitionTo(new CancelledState());
    }

    public String name() {
        return "CONFIRMED";
    }
}

class CancelledState implements BookingState {
    public void confirm(Booking booking) {
        throw new IllegalStateException("cancelled booking cannot be confirmed");
    }

    public void cancel(Booking booking) {
        System.out.println("already cancelled");
    }

    public String name() {
        return "CANCELLED";
    }
}

class Booking {
    private BookingState state = new DraftState();

    public void confirm() {
        // LLD concept: context delegates behavior to current state instead of switching on status.
        state.confirm(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    void transitionTo(BookingState nextState) {
        this.state = nextState;
    }

    public String status() {
        return state.name();
    }
}
```

Key idea:
- each state class owns the behavior and transition rules for that state

---

## 15. Python Mini Program / Simulation

This mini program shows how state removes repeated `if status` checks.

```python
from __future__ import annotations
from typing import Protocol


class BookingState(Protocol):
    name: str

    def confirm(self, booking: "Booking") -> None:
        pass

    def cancel(self, booking: "Booking") -> None:
        pass


class DraftState:
    name = "draft"

    def confirm(self, booking: "Booking") -> None:
        # LLD concept: draft state owns the valid confirm transition.
        booking.transition_to(ConfirmedState())

    def cancel(self, booking: "Booking") -> None:
        booking.transition_to(CancelledState())


class ConfirmedState:
    name = "confirmed"

    def confirm(self, booking: "Booking") -> None:
        raise ValueError("already confirmed")

    def cancel(self, booking: "Booking") -> None:
        booking.transition_to(CancelledState())


class CancelledState:
    name = "cancelled"

    def confirm(self, booking: "Booking") -> None:
        raise ValueError("cancelled booking cannot be confirmed")

    def cancel(self, booking: "Booking") -> None:
        print("already cancelled")


class Booking:
    def __init__(self) -> None:
        self.state: BookingState = DraftState()

    def transition_to(self, state: BookingState) -> None:
        self.state = state

    def confirm(self) -> None:
        # LLD concept: behavior changes because the internal state object changes.
        self.state.confirm(self)

    def cancel(self) -> None:
        self.state.cancel(self)


def main() -> None:
    booking = Booking()
    print(booking.state.name)
    booking.confirm()
    print(booking.state.name)
    booking.cancel()
    print(booking.state.name)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- no repeated status switch in `Booking`
- each state controls its valid actions
- state differs from strategy because it transitions internally

---

## 16. Practical Question

> You are designing booking status transitions. When would you use State instead of a simple enum?

---

## 17. Strong Answer

1. I would start with enum if states are simple labels.
2. I would use State if behavior differs significantly by status.
3. Each state class would own allowed actions and invalid transitions.
4. The booking object would delegate actions to its current state.
5. This reduces scattered `if status` checks.
6. I would draw a state diagram and test every transition.

---

## 18. Revision Notes

- One-line summary: State moves lifecycle-specific behavior into state objects.
- Three keywords: lifecycle, transition, status behavior
- One interview trap: using State for simple enums with no behavior
- One memory trick: Strategy is chosen; State evolves

---

# Topic 11: Command

> Track: 2.2 High-Signal Design Patterns
> Scope: action objects, queues, retries, undo, auditability, delayed execution, and request encapsulation

---

## 1. Intuition

Think of a hotel work order.

Instead of verbally asking maintenance to fix room 504, you create a work order: action, room, priority, requester, timestamp. The work order can be queued, retried, assigned, audited, or cancelled.

Command turns a request into an object.

Short memory trick:
- action as object
- execute later
- queue, retry, undo, audit

---

## 2. Definition

- Definition: Command is a behavioral pattern that encapsulates a request or action as an object, allowing it to be queued, logged, retried, undone, or executed later.
- Category: Behavioral design pattern
- Core idea: Separate the object that asks for work from the object that performs it.

Command is common wherever operations need lifecycle.

Examples:
- job queue tasks
- UI button actions
- undoable editor actions
- booking operations
- payment retry commands

---

## 3. Why It Exists

Direct method calls are simple, but they disappear after execution.

Sometimes an action needs to be:
- stored
- retried
- audited
- scheduled
- undone
- authorized
- batched
- sent to a queue

Command exists when an action needs identity and lifecycle.

---

## 4. Reality

Command appears in:

- background jobs
- task queues
- UI actions
- undo/redo stacks
- workflow engines
- CQRS command handlers
- payment capture commands
- booking cancellation commands
- migration scripts with rollback commands

Architecture connection:
- Commands are central in CQRS, where commands represent intent to change state.

---

## 5. How It Works

Typical flow:

1. Define a command interface with `execute`.
2. Create concrete command objects with required data.
3. Invoker receives command without knowing details.
4. Invoker executes, queues, logs, or retries the command.
5. Receiver performs actual business operation.
6. Optional undo or compensation can be attached.

Important design point:
- command should represent intent, not hide random business logic

---

## 6. What Problem It Solves

- Primary problem solved: actions need to be represented as first-class objects.
- Secondary benefits: queueing, retry, audit, undo, scheduling, decoupling invoker from receiver.
- Systems impact: supports reliable operation workflows and delayed execution.

Command is especially useful when:
- action must survive beyond the call stack
- action needs metadata
- action may be retried or undone

---

## 7. When to Rely on It

Use Command when:

- you need to queue work
- you need undo/redo
- you need audit logs of requested actions
- you need retry or delayed execution
- UI buttons should trigger reusable actions
- caller should not know receiver details

Interview trigger words:
- job queue
- task
- undo
- retry
- audit action
- delayed execution
- command handler

---

## 8. When Not to Use It

Avoid Command when:

- a direct method call is enough
- action has no lifecycle beyond immediate execution
- commands become anemic wrappers with no value
- too many tiny commands fragment simple logic
- consistency requires one synchronous transaction and no queueing

Use direct service method call for simple immediate operations.

Use Strategy when you are swapping algorithms rather than storing actions.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Makes actions queueable | Adds object overhead |
| Supports retry and audit | Can create many classes |
| Decouples invoker from receiver | Overuse makes simple flow indirect |
| Enables undo/redo | Command serialization can be tricky |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: action lifecycle and decoupling.
- Give up: simplicity of direct call.
- Complexity impact: useful for queues and undo, noisy for simple CRUD.
- Reliability impact: command can be persisted and retried if designed carefully.

### Common Mistakes

- Mistake: command object has no meaningful data or lifecycle.
- Why it is wrong: direct call would be clearer.
- Better approach: use Command when action needs identity.

- Mistake: command executes non-idempotent work without safeguards.
- Why it is wrong: retries can duplicate side effects.
- Better approach: include idempotency key or execution tracking.

- Mistake: command contains too much infrastructure code.
- Why it is wrong: domain intent gets buried.
- Better approach: command captures intent; handler coordinates execution.

---

## 11. Key Numbers

Pattern heuristics:

- Immediate one-off call: no command needed.
- Action needs retry, audit, undo, or queue: command is strong fit.
- More than 2 execution modes, such as now, later, retry: command helps.
- Distributed retries: require idempotency key.
- Long-running commands: include status tracking.

Memory number:
- Command is useful when the action needs a life after the method call.

---

## 12. Failure Modes

- Duplicate execution: command retried and charges twice.
- Lost command: queued action not persisted.
- Stale command data: command executes after context changed.
- Missing authorization: command executed by wrong actor.
- Undo impossible: command did not capture enough inverse data.

Mitigations:
- idempotency keys
- durable storage for important commands
- authorization at command handling time
- command status tracking
- explicit compensation or undo data

---

## 13. Scenario

- Product / system: Booking cancellation workflow
- Requirement: cancellation requests may be queued, retried, audited, and compensated
- Good design: `CancelBookingCommand` captures request intent and is executed by a command handler
- Why this pattern fits: the action needs identity, metadata, retry, and audit
- What would go wrong without it: cancellation is only a direct call and cannot be reliably retried or audited

---

## 14. Java Code Sample

### Booking command with command bus

```java
interface Command {
    void execute();
}

class BookingService {
    void cancelBooking(String bookingId, String reason) {
        System.out.println("cancelled " + bookingId + " because " + reason);
    }
}

class CancelBookingCommand implements Command {
    private final BookingService bookingService;
    private final String bookingId;
    private final String reason;

    CancelBookingCommand(BookingService bookingService, String bookingId, String reason) {
        // LLD concept: Command captures all data needed to execute the action later.
        this.bookingService = bookingService;
        this.bookingId = bookingId;
        this.reason = reason;
    }

    public void execute() {
        // LLD concept: invoker does not need to know receiver method details.
        bookingService.cancelBooking(bookingId, reason);
    }
}

class CommandBus {
    public void dispatch(Command command) {
        System.out.println("audit: command dispatched");
        command.execute();
    }
}
```

Key idea:
- command object carries action intent so it can be dispatched, logged, queued, or retried

---

## 15. Python Mini Program / Simulation

This mini program shows commands being queued and executed later.

```python
from dataclasses import dataclass
from typing import Protocol


class Command(Protocol):
    def execute(self) -> None:
        pass


class BookingService:
    def cancel(self, booking_id: str, reason: str) -> None:
        print(f"cancelled {booking_id}: {reason}")


@dataclass(frozen=True)
class CancelBookingCommand:
    booking_service: BookingService
    booking_id: str
    reason: str
    idempotency_key: str

    def execute(self) -> None:
        # LLD concept: command object represents an executable action with metadata.
        self.booking_service.cancel(self.booking_id, self.reason)


class CommandQueue:
    def __init__(self) -> None:
        self._commands: list[Command] = []

    def enqueue(self, command: Command) -> None:
        # LLD concept: invoker stores the command without knowing concrete receiver logic.
        self._commands.append(command)

    def drain(self) -> None:
        while self._commands:
            command = self._commands.pop(0)
            command.execute()


def main() -> None:
    queue = CommandQueue()
    service = BookingService()
    queue.enqueue(CancelBookingCommand(service, "booking-1", "guest request", "cancel-booking-1"))
    queue.drain()


if __name__ == "__main__":
    main()
```

What this demonstrates:
- action is represented as data plus behavior
- invoker can queue command without knowing receiver details
- idempotency metadata can travel with the action

---

## 16. Practical Question

> You are designing a cancellation workflow that needs retry and audit. Would you use Command?

---

## 17. Strong Answer

1. I would use Command because cancellation needs identity beyond a direct method call.
2. A `CancelBookingCommand` would contain booking id, reason, actor, and idempotency key.
3. A command handler or bus could execute, log, queue, or retry it.
4. This decouples the invoker from the receiver.
5. The trade-off is extra structure, so I would avoid it for simple immediate calls.
6. For retries, I would make execution idempotent to avoid duplicate cancellation side effects.

---

## 18. Revision Notes

- One-line summary: Command turns an action into an object so it can be queued, retried, logged, or undone.
- Three keywords: action, queue, execute
- One interview trap: using Command when a direct service call is clearer
- One memory trick: Command is a work order

---

## Final Interview Comparison Sheet

| Pattern | Best one-line interview explanation | Do not confuse with |
|---|---|---|
| Factory | Creates the right concrete object behind an abstraction | Strategy, which performs an algorithm |
| Builder | Assembles complex objects with readable steps and validation | Factory, which selects a type |
| Singleton | Ensures one shared instance, but can create dangerous global state | DI singleton lifecycle |
| Adapter | Converts an incompatible interface into the expected one | Facade, which simplifies a subsystem |
| Decorator | Adds behavior by wrapping the same interface | Proxy, which controls access |
| Facade | Provides one simple API over many subsystem calls | Adapter, which translates shape |
| Proxy | Controls access, lifecycle, cache, or remote boundary | Decorator, which adds optional behavior |
| Strategy | Swaps algorithms behind one contract | State, which changes through lifecycle |
| Observer | Notifies many listeners about an event | Durable message queue |
| State | Moves lifecycle-specific behavior into state objects | Strategy, selected externally |
| Command | Turns a request into an executable object | Strategy, which is an algorithm |

---

## Fast Recall Rules

- If the problem is object creation, think Factory or Builder.
- If the problem is too many optional constructor values, think Builder.
- If the problem is global mutable convenience, be suspicious of Singleton.
- If the problem is incompatible interface, think Adapter.
- If the problem is too many subsystem calls, think Facade.
- If the problem is optional layers around the same object, think Decorator.
- If the problem is access control or lazy loading, think Proxy.
- If the problem is interchangeable algorithm, think Strategy.
- If the problem is one event and many reactions, think Observer.
- If the problem is lifecycle-specific behavior, think State.
- If the problem is action needs queue, retry, audit, or undo, think Command.