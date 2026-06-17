# Object-Oriented Design - Mentorship Track

> Goal: build strong low-level design intuition with interview-ready depth for modeling objects, responsibilities, relationships, and change-friendly code.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `2.1 Object-Oriented Design`.
- We will follow the same learning style used in the HLD mentorship notes.
- We will add each subtopic as a full architect-level learning unit.
- For LLD topics, we will include both Java and Python examples wherever code helps: Java for enterprise-style OOD structure, and Python for compact simulation of the same concept.
- We will include interview-style answers that connect the code back to design decisions.

---

## Roadmap for This Sheet

1. SOLID principles applied
2. Encapsulation and abstraction
3. Composition vs inheritance
4. Immutability
5. Cohesion and coupling

---

## Code Example Convention

- Section 14 uses Java to show class structure, interfaces, records, domain methods, and enterprise-style LLD design.
- Section 15 uses Python to simulate the same concept compactly so the behavior is easy to run mentally.
- Code comments will call out the exact LLD concept being applied where that makes the example easier to map mentally.
- If a future LLD topic is mostly conceptual, we will still add code only where it clarifies the design instead of forcing artificial examples.

---

# Topic 1: SOLID Principles Applied

> Track: 2.1 Object-Oriented Design
> Scope: responsibility boundaries, extension points, substitutable types, interface design, dependency inversion, and practical class design

---

## 1. Intuition

Think of a hotel operation.

- The receptionist checks guests in.
- The pricing desk calculates room rates.
- The housekeeping team prepares rooms.
- The payment team charges cards.
- The manager coordinates policy, but does not personally do every job.

If one person tries to do every task, the hotel becomes fragile. A small policy change can break check-in, billing, housekeeping, and reporting at the same time.

SOLID is a set of object-oriented design principles that helps classes behave more like well-run teams.

Short memory trick:
- one class, one reason to change
- extend behavior without rewriting stable code
- child types must behave like their parent contract
- small interfaces beat fat interfaces
- depend on abstractions, not concrete details

---

## 2. Definition

- Definition: SOLID is a group of five object-oriented design principles that help code remain understandable, testable, extensible, and resilient to change.
- Category: Low-level design principles and maintainability framework
- Core idea: Put responsibilities behind clear contracts so new behavior can be added with minimal damage to existing behavior.

The five principles:
- Single Responsibility Principle: a class should have one primary reason to change.
- Open/Closed Principle: code should be open for extension but closed for modification.
- Liskov Substitution Principle: subtypes must honor the behavior expected from their base types.
- Interface Segregation Principle: clients should not depend on methods they do not use.
- Dependency Inversion Principle: high-level policy should depend on abstractions, not low-level concrete details.

Interview shortcut:
- SOLID is not about adding patterns everywhere
- SOLID is about controlling change
- the goal is flexible code where flexibility is actually needed

---

## 3. Why It Exists

Large systems change constantly.

Examples:
- pricing rules change by season
- payments add a new provider
- notifications move from email-only to email plus SMS
- booking cancellation policy changes by hotel chain
- reporting needs new fields but should not affect checkout

Without SOLID thinking:
- one class becomes a large procedural script
- adding one feature breaks unrelated behavior
- tests become hard because everything depends on concrete systems
- teams copy-paste instead of extending cleanly
- interfaces become bloated and force clients to implement useless methods
- inheritance hierarchies become fragile and surprising

SOLID exists because software changes faster than the first design expects.

The principles help us design code so the likely axis of change is isolated. If payment providers change often, hide them behind a payment interface. If pricing rules change often, model them as replaceable policies. If a class has too many reasons to change, split responsibilities.

---

## 4. Reality

### SOLID is common in:

- Java backend services
- Spring Boot applications
- domain-driven service layers
- payment and booking workflows
- testable business logic
- plugin-like systems
- systems with multiple providers or policies

### Common places it shows up

- services depending on repository interfaces
- strategy objects for pricing or discount rules
- adapters for third-party providers
- small interfaces for read vs write behavior
- domain objects with focused responsibility
- constructor injection instead of creating dependencies inside classes

### Real-world architecture truth

SOLID is useful when it controls real change. It becomes noise when applied mechanically.

Bad SOLID looks like:
- an interface for every class without a reason
- five tiny classes for a simple two-line operation
- abstract factories for behavior that never varies
- inheritance used only to share two fields
- indirection that makes code harder to read than the problem itself

Good SOLID looks like:
- clear responsibility boundaries
- easy unit tests
- low-impact provider changes
- business rules that can evolve without rewriting orchestration code
- dependencies that can be replaced in tests or production

---

## 5. How It Works

At a high level:

1. Identify the responsibility of each class.
2. Identify the things that are likely to change.
3. Put changing behavior behind abstractions or policies.
4. Keep interfaces focused on what clients actually need.
5. Use composition and dependency injection to assemble behavior.
6. Make sure derived types honor the base contract if inheritance is used.
7. Add tests around contracts and important workflows.

### Single Responsibility flow

- Separate orchestration, validation, calculation, persistence, and notification when they change for different reasons.
- A booking service may coordinate the booking flow, but pricing, payment, and notification should usually live elsewhere.

SRP answers:
- why would this class change?

### Open/Closed flow

- Stable orchestration code calls a policy or strategy interface.
- New behavior is added by creating a new implementation.
- Existing tested orchestration does not need risky modification.

OCP answers:
- can we add a new variant without editing the stable core?

### Liskov Substitution flow

- A subtype must support the promises made by its parent type.
- If a child throws unsupported-operation errors for normal parent behavior, the hierarchy is probably wrong.

LSP answers:
- can this subtype safely stand in for the base type?

### Interface Segregation flow

- Split broad interfaces into role-specific interfaces.
- A read-only client should not depend on write methods.
- An email notifier should not be forced to implement SMS-specific behavior.

ISP answers:
- does this client depend only on what it actually uses?

### Dependency Inversion flow

- High-level services depend on interfaces.
- Infrastructure classes implement those interfaces.
- Construction is handled outside the business logic, often by dependency injection.

DIP answers:
- does business policy depend on details, or do details plug into policy?

### Failure path

- If abstractions are chosen too early, the code becomes over-engineered.
- If abstractions are ignored too long, the code becomes rigid and hard to test.
- If inheritance violates behavior contracts, callers get surprising runtime failures.

### Recovery path

- Refactor around actual change points.
- Extract interfaces from real consumers, not imagination.
- Replace fragile inheritance with composition.
- Add contract tests for important abstractions.

---

## 6. What Problem It Solves

- Primary problem solved: keeps object-oriented code maintainable as requirements change.
- Secondary benefits: better testability, clearer ownership, lower coupling, safer extensions, easier provider swaps, and fewer large classes.
- Systems impact: changes LLD from class dumping into responsibility-driven modeling.

This topic solves three practical problems:
- how do we prevent one class from becoming a monster?
- how do we add new behavior without rewriting stable code?
- how do we keep business logic testable and independent from infrastructure details?

---

## 7. When to Rely on It

Use SOLID thinking when:
- the codebase will evolve over time
- multiple business rules or providers exist
- classes are becoming large or hard to test
- dependencies need to be mocked or replaced
- different teams own different behaviors
- new variants are expected, such as new payment providers or discount rules

Especially valuable for:
- booking workflows
- payment processing
- pricing engines
- notification systems
- order management
- inventory systems
- domain services in Java or Spring Boot

Strong interviewer keywords:
- responsibility
- extension point
- strategy
- dependency injection
- substitutability
- interface segregation
- provider abstraction
- testability

---

## 8. When Not to Use It

Do not apply SOLID mechanically.

Be careful when:
- the code is a small one-off script
- the behavior is stable and unlikely to vary
- abstractions hide more than they clarify
- every class gets an interface only for ceremony
- a simple data transformation is split into many tiny objects

Avoid these patterns:
- creating interfaces before there is a real second implementation or test need
- using inheritance to avoid small duplication
- making every method public for future flexibility
- splitting responsibilities so far that the flow becomes impossible to follow
- treating SOLID as a checklist instead of design judgment

Better framing:
- start simple
- watch where change repeats
- extract boundaries where they reduce real friction
- design for known and likely variation, not every imaginary future

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Applied SOLID principles | Improve maintainability, testability, extensibility, and responsibility clarity | Can create unnecessary abstraction, class explosion, and harder navigation if applied without judgment |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Flexibility vs simplicity:
  abstractions make change safer, but too many abstractions make code harder to read.
- Testability vs ceremony:
  dependency inversion helps testing, but interfaces without meaningful boundaries add noise.
- Reuse vs clarity:
  inheritance can reuse behavior, but composition often keeps responsibilities clearer.
- Local speed vs long-term change:
  directly coding against a concrete class is quick, but costly if the dependency later changes.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Treating SRP as one method per class | SRP is about one reason to change, not tiny classes for everything | Group behavior by cohesive responsibility |
| Adding interfaces for every implementation | It creates ceremony without flexibility | Extract interfaces at real boundaries or test seams |
| Violating LSP with unsupported methods | Callers cannot trust the base type contract | Redesign the hierarchy or split the abstraction |
| Fat service interfaces | Clients depend on operations they do not need | Create role-specific interfaces |
| Newing dependencies inside business logic | Code becomes hard to test and replace | Inject abstractions through constructors |
| Overusing inheritance | Base classes become fragile and subclasses inherit unwanted behavior | Prefer composition for behavior reuse |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Class size:
  if a class keeps growing past a few focused responsibilities, inspect SRP.
- Constructor dependencies:
  many dependencies can indicate orchestration overload or poor boundary design.
- Interface methods:
  broad interfaces with many unrelated methods often violate ISP.
- Change frequency:
  if a class changes for unrelated features repeatedly, split responsibilities.
- Test setup complexity:
  if testing one business rule requires many unrelated mocks, coupling is likely too high.
- Implementations per abstraction:
  one implementation can still be valid at a boundary, but many single-use interfaces may indicate over-design.

Interview shorthand:
- one reason to change, extend safely, respect contracts, keep interfaces small, depend on abstractions

---

## 12. Failure Modes

### God service

Problem:
- `BookingService` validates input, calculates pricing, writes database rows, charges payment, sends email, and creates reports.

User impact:
- every change risks breaking unrelated behavior, and tests become large and fragile

Mitigation:
- split pricing, payment, persistence, notification, and reporting into focused collaborators
- keep orchestration clear but not overloaded

### Fake abstraction

Problem:
- Every class has an interface even though there is no variation, no external boundary, and no testing benefit.

User impact:
- code navigation becomes noisy and simple changes touch many files

Mitigation:
- introduce abstractions at meaningful seams such as external providers, repositories, policies, and strategies

### Broken subtype

Problem:
- A subclass implements a parent method by throwing `UnsupportedOperationException`.

User impact:
- callers using the parent type fail unexpectedly at runtime

Mitigation:
- split the interface or replace the inheritance hierarchy with composition

### Concrete dependency trap

Problem:
- Business logic directly creates a third-party payment SDK client.

User impact:
- tests require real infrastructure and provider changes affect business code

Mitigation:
- depend on a `PaymentGateway` abstraction and implement provider-specific adapters separately

---

## 13. Scenario

- Product / system: Hotel booking checkout service
- Requirement:
  support multiple payment providers, seasonal pricing rules, booking notifications, and cancellation policies without turning checkout into a single massive class
- Good design:
  keep checkout orchestration in one service, model pricing and cancellation as policies, hide payment providers behind a gateway interface, keep notification channels separate, and inject dependencies through constructors
- Why this concept fits:
  the system has multiple change axes: pricing, payment, notification, persistence, and policy
- What would go wrong without it:
  adding a new payment provider or discount rule would require editing fragile checkout logic and retesting unrelated flows

---

## 14. Java Code Sample

### SOLID-friendly booking checkout skeleton

```java
import java.math.BigDecimal;

public record BookingRequest(String userId, String hotelId, int nights) {
}

public record Price(BigDecimal amount, String currency) {
}

public record PaymentResult(boolean approved, String reference) {
}

// LLD concept: focused contracts create extension points without coupling checkout to concrete implementations.
interface PricingPolicy {
    Price calculate(BookingRequest request);
}

interface PaymentGateway {
    PaymentResult charge(String userId, Price price);
}

interface BookingRepository {
    void saveConfirmedBooking(BookingRequest request, Price price, String paymentReference);
}

interface BookingNotifier {
    void sendConfirmation(String userId, String hotelId);
}

public class BookingCheckoutService {

  // LLD concept: dependency inversion; checkout depends on abstractions, not SQL, email, or provider SDK classes.
    private final PricingPolicy pricingPolicy;
    private final PaymentGateway paymentGateway;
    private final BookingRepository bookingRepository;
    private final BookingNotifier bookingNotifier;

    public BookingCheckoutService(
            PricingPolicy pricingPolicy,
            PaymentGateway paymentGateway,
            BookingRepository bookingRepository,
            BookingNotifier bookingNotifier) {
        this.pricingPolicy = pricingPolicy;
        this.paymentGateway = paymentGateway;
        this.bookingRepository = bookingRepository;
        this.bookingNotifier = bookingNotifier;
    }

    public void checkout(BookingRequest request) {
      // LLD concept: high-level orchestration delegates specialized responsibilities to cohesive collaborators.
        Price price = pricingPolicy.calculate(request);
        PaymentResult payment = paymentGateway.charge(request.userId(), price);

        if (!payment.approved()) {
            throw new IllegalStateException("payment declined");
        }

        bookingRepository.saveConfirmedBooking(request, price, payment.reference());
        bookingNotifier.sendConfirmation(request.userId(), request.hotelId());
    }
}
```

Key idea:
- the checkout service coordinates the use case, while pricing, payment, persistence, and notification each have focused responsibilities and replaceable contracts

---

## 15. Python Mini Program / Simulation

This mini program shows open/closed behavior by adding pricing policies without changing checkout flow.

```python
from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class BookingRequest:
    hotel_id: str
    nights: int
    base_rate: int


class PricingPolicy(Protocol):
  # LLD concept: Checkout depends on this abstraction, so pricing variants can be added safely.
    def calculate(self, request: BookingRequest) -> int:
        pass


class StandardPricing:
    def calculate(self, request: BookingRequest) -> int:
        return request.nights * request.base_rate


class HolidayPricing:
    def calculate(self, request: BookingRequest) -> int:
        return int(request.nights * request.base_rate * 1.25)


class Checkout:
    def __init__(self, pricing_policy: PricingPolicy) -> None:
    # LLD concept: behavior is injected instead of hardcoded, keeping Checkout closed for modification.
        self.pricing_policy = pricing_policy

    def quote(self, request: BookingRequest) -> int:
    # LLD concept: Checkout delegates pricing instead of owning every pricing rule.
        return self.pricing_policy.calculate(request)


def main() -> None:
    request = BookingRequest("hotel-7", nights=3, base_rate=100)

    for policy in [StandardPricing(), HolidayPricing()]:
        checkout = Checkout(policy)
        print(f"{policy.__class__.__name__}: {checkout.quote(request)}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- checkout depends on a pricing abstraction
- new pricing variants do not require rewriting checkout
- strategy-style design is useful when behavior varies by policy
- SOLID should clarify a real variation point

---

## 16. Practical Question

> You are designing the low-level classes for a hotel booking checkout flow. The system must support multiple pricing rules, payment providers, notification channels, and cancellation policies. How would you apply SOLID principles without over-engineering?

---

## 17. Strong Answer

I would start by identifying the reasons the code is likely to change. Pricing rules, payment providers, notification channels, persistence, and cancellation policy can change independently, so I would not put all of that logic into one large checkout class. The checkout service can orchestrate the flow, but it should delegate calculation, payment, storage, and notification to focused collaborators.

For open/closed design, I would put variable behavior such as pricing and cancellation behind policy or strategy interfaces. New seasonal pricing or refund rules can be added as new implementations instead of editing stable checkout code. For dependency inversion, checkout should depend on interfaces like `PaymentGateway`, not directly on a Stripe or Adyen SDK client. Provider-specific code belongs in adapters.

I would also avoid making interfaces too broad. A notification sender should not be forced to implement unrelated methods for every channel. And I would avoid inheritance unless subtypes truly satisfy the base contract. The goal is not to create the maximum number of classes; it is to isolate real change points so the design remains understandable and testable.

---

## 18. Revision Notes

- One-line summary: SOLID helps object-oriented code stay maintainable by separating responsibilities, hiding variation behind contracts, and depending on abstractions where change is expected.
- Three keywords: responsibility, extension, abstraction
- One interview trap: applying SOLID mechanically and creating needless interfaces or class explosion
- One memory trick: design classes like focused hotel teams, not one person doing every job

---

# Topic 2: Encapsulation and Abstraction

> Track: 2.1 Object-Oriented Design
> Scope: state protection, invariant enforcement, public APIs, domain methods, implementation hiding, and model clarity

---

## 1. Intuition

Think of a hotel room safe.

- A guest can open the safe using the correct code.
- The guest does not need to know how the lock mechanism works internally.
- The hotel should not let anyone directly move gears inside the safe.
- The safe exposes a small set of meaningful actions: open, close, lock, unlock.

That is encapsulation and abstraction together.

Encapsulation protects the inside.

Abstraction presents the useful outside.

Short memory trick:
- encapsulation hides state and protects rules
- abstraction hides complexity and exposes intent

In LLD, a good class should not be just a bag of public fields. It should protect its invariants and expose meaningful operations.

---

## 2. Definition

- Definition: Encapsulation is the practice of keeping object state and implementation details private and allowing access only through controlled methods.
- Definition: Abstraction is the practice of exposing the essential behavior of an object while hiding unnecessary implementation details.
- Category: Object modeling and class-boundary design
- Core idea: Objects should protect their own validity and give callers a clean contract instead of exposing internal mechanics.

Interview shortcut:
- encapsulation protects data and invariants
- abstraction simplifies usage
- getters and setters alone do not mean good encapsulation

---

## 3. Why It Exists

Objects often have rules that must always remain true.

Examples:
- a booking cannot have a checkout date before check-in
- a payment cannot be captured before it is authorized
- room inventory cannot go below zero
- a cancellation may be allowed only before a deadline
- a confirmed booking should not move back to draft accidentally

Without encapsulation:
- any caller can mutate object state incorrectly
- rules are duplicated across many services
- bugs appear because invalid states become possible
- later changes require searching every direct field mutation

Without abstraction:
- callers need to know too many implementation details
- class users become tightly coupled to internal structure
- changing storage, calculation, or validation logic becomes risky
- code reads like mechanical manipulation instead of business behavior

These ideas exist because objects should own their own consistency.

The design goal is not to hide everything for secrecy. The design goal is to expose only the operations that make sense and keep invalid states hard to create.

---

## 4. Reality

### Encapsulation and abstraction are common in:

- domain models
- value objects
- aggregate roots
- service APIs
- SDK clients
- repositories
- payment and booking workflows
- state machines

### Common examples

- `Booking.confirm()` instead of `booking.status = CONFIRMED`
- `Inventory.reserve(quantity)` instead of `inventory.available -= quantity`
- `Money.add(other)` instead of manually adding amounts and currencies everywhere
- `Payment.capture()` instead of mutating payment fields directly

### Real-world architecture truth

Private fields alone do not guarantee good encapsulation.

If a class has private fields but exposes setters for everything, callers can still put it into invalid states.

Another important truth:
- abstraction should be stable at the level callers care about

For example, callers should ask a `PricingPolicy` for a price. They should not know whether pricing came from a table, API, cache, seasonal rule, or machine-learning model unless that detail matters to the caller.

---

## 5. How It Works

At a high level:

1. Identify the object's valid states and invariants.
2. Make fields private where possible.
3. Expose behavior methods that preserve invariants.
4. Avoid generic setters for state transitions with business meaning.
5. Keep implementation details behind interfaces or domain methods.
6. Return safe views or immutable values when exposing collections or internal data.
7. Let callers depend on what the object does, not how it does it.

### Encapsulation flow

- Object owns its state.
- Constructor or factory validates required fields.
- Methods enforce business rules before mutation.
- Invalid transitions are rejected close to the data.

Encapsulation answers:
- can this object protect itself from invalid state?

### Abstraction flow

- Public methods describe meaningful operations.
- Internal algorithms, data structures, provider details, and calculations remain hidden.
- Callers interact with the simplest useful contract.

Abstraction answers:
- can callers use this object without knowing its internal machinery?

### Failure path

- Public setters allow invalid state.
- Internal lists are returned and mutated by callers.
- Domain rules leak into controllers and services.
- Callers depend on implementation details and break when internals change.

### Recovery path

- Move rules into domain methods.
- Replace setters with intention-revealing operations.
- Return immutable snapshots or copies.
- Hide provider or storage details behind small interfaces.

---

## 6. What Problem It Solves

- Primary problem solved: prevents invalid object state and hides unnecessary implementation detail from callers.
- Secondary benefits: clearer APIs, fewer duplicated rules, safer refactoring, stronger domain modeling, and easier testing.
- Systems impact: turns objects from passive data containers into responsible domain concepts.

This topic solves three practical problems:
- how do we keep objects valid?
- how do we stop callers from depending on internals?
- how do we make code read in terms of business actions rather than field mutation?

---

## 7. When to Rely on It

Use strong encapsulation and abstraction when:
- objects have important invariants
- state transitions have business meaning
- multiple callers may modify the same concept
- internal implementation may change
- collections or mutable fields are exposed
- the object models real domain behavior

Especially valuable for:
- booking lifecycle
- inventory reservation
- payment authorization and capture
- user account status
- cart and order totals
- room availability
- cancellation and refund policies

Strong interviewer keywords:
- invariant
- state transition
- intention-revealing method
- information hiding
- domain method
- public contract
- implementation detail

---

## 8. When Not to Use It

Do not turn every simple data carrier into a heavy domain object.

Be careful when:
- the object is a DTO used only for serialization
- the data has no behavior or invariant
- extra abstraction hides straightforward code
- methods become vague wrappers around simple getters
- a domain object starts depending on infrastructure details

Avoid these patterns:
- exposing setters for every field by default
- returning mutable internal collections
- putting validation only in controllers while domain objects remain unsafe
- making all fields public for convenience
- using abstraction names so generic they hide meaning, such as `Manager` or `Processor`

Better framing:
- use rich behavior for domain objects
- use simple DTOs at boundaries when appropriate
- protect invariants where they live
- expose methods that match domain language

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Encapsulation and abstraction | Keep state valid, reduce coupling to internals, clarify public APIs, and make refactoring safer | Can add boilerplate or hide simple behavior if overused in plain data-transfer cases |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Rich domain model vs simple DTOs:
  rich models enforce rules well, but DTOs are simpler for serialization and API boundaries.
- Information hiding vs observability:
  hiding internals protects design, but diagnostics may still need safe snapshots or events.
- Strict invariants vs flexibility:
  strict rules prevent invalid states, but migrations and imports may need controlled bypass paths.
- Abstraction vs transparency:
  abstraction simplifies callers, but too much abstraction can make behavior hard to trace.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Public fields everywhere | Any caller can break invariants | Use private fields and behavior methods |
| Setters for every field | Private fields become cosmetic only | Replace setters with intention-revealing operations |
| Returning mutable lists | External code can mutate internal state silently | Return immutable views or copies |
| Anemic domain model | Rules scatter across services and controllers | Put core invariants near the domain object |
| Over-abstracting DTOs | Simple API data becomes hard to map and debug | Keep DTOs simple and domain models expressive |
| Leaking implementation details | Callers break when internals change | Expose stable public contracts |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Public setters:
  if most fields have public setters, inspect whether invariants are actually protected.
- Mutable collections:
  if a getter returns a mutable internal collection, expect accidental state corruption.
- Domain methods:
  important state changes should usually have named methods such as `confirm`, `cancel`, `reserve`, or `capture`.
- Validation location:
  rules that define object validity should live close to the object, not only at the edge.
- Constructor size:
  too many raw parameters may indicate missing value objects or factories.

Interview shorthand:
- hide state, expose behavior, protect invariants, speak domain language

---

## 12. Failure Modes

### Invalid booking state

Problem:
- Callers directly set booking dates and status fields.

User impact:
- bookings can become confirmed with invalid dates or impossible transitions

Mitigation:
- validate dates in constructors or factories
- use methods such as `confirm()` and `cancel()` for transitions

### Mutable collection leak

Problem:
- `getRooms()` returns the internal room list.

User impact:
- external code can remove rooms without inventory checks

Mitigation:
- return immutable copies or read-only views
- provide controlled methods such as `addRoom` or `reserveRoom`

### Abstraction leak

Problem:
- Checkout code knows which SQL table or external pricing API is used.

User impact:
- pricing implementation changes force unrelated checkout changes

Mitigation:
- hide details behind `PricingPolicy` or `PriceCalculator`

### Rules scattered everywhere

Problem:
- Every service checks cancellation deadline differently.

User impact:
- users see inconsistent behavior across APIs

Mitigation:
- centralize the cancellation rule in a policy or domain method

---

## 13. Scenario

- Product / system: Hotel booking domain model
- Requirement:
  bookings should never be confirmed with invalid dates, cancelled bookings should not be paid again, and callers should not mutate room reservations directly
- Good design:
  make booking fields private, validate construction, expose domain methods like `confirm`, `cancel`, and `reserveRoom`, and return immutable views for internal collections
- Why this concept fits:
  booking has real invariants and state transitions that should be protected consistently
- What would go wrong without it:
  invalid bookings would appear because many services could mutate the same fields differently

---

## 14. Java Code Sample

### Encapsulating booking state transitions

```java
import java.time.LocalDate;

public class Booking {

  // LLD concept: private state prevents callers from bypassing booking invariants.
    private final String bookingId;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private BookingStatus status;

    public Booking(String bookingId, LocalDate checkIn, LocalDate checkOut) {
    // LLD concept: constructor enforces the invariant before the object can be used.
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkout must be after checkin");
        }
        this.bookingId = bookingId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = BookingStatus.DRAFT;
    }

    public void confirm() {
      // LLD concept: state changes happen through domain methods, not public setters.
        if (status != BookingStatus.DRAFT) {
            throw new IllegalStateException("only draft bookings can be confirmed");
        }
        status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        if (status == BookingStatus.COMPLETED) {
            throw new IllegalStateException("completed booking cannot be cancelled");
        }
        status = BookingStatus.CANCELLED;
    }

    public BookingStatus status() {
        return status;
    }
}

enum BookingStatus {
    DRAFT,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
```

Key idea:
- callers do not directly set status; they ask the booking to perform a meaningful transition that enforces rules

---

## 15. Python Mini Program / Simulation

This mini program shows the difference between direct mutation and behavior-based state changes.

```python
from dataclasses import dataclass
from datetime import date


class Booking:
    def __init__(self, booking_id: str, check_in: date, check_out: date) -> None:
    # LLD concept: construction protects the date invariant at the object boundary.
        if check_out <= check_in:
            raise ValueError("checkout must be after checkin")
        self._booking_id = booking_id
        self._check_in = check_in
        self._check_out = check_out
        self._status = "draft"

    def confirm(self) -> None:
        # LLD concept: callers ask for a valid transition instead of mutating _status directly.
        if self._status != "draft":
            raise ValueError("only draft bookings can be confirmed")
        self._status = "confirmed"

    def cancel(self) -> None:
        if self._status == "completed":
            raise ValueError("completed booking cannot be cancelled")
        self._status = "cancelled"

    @property
    def status(self) -> str:
      # LLD concept: expose a read-only view of state, not a general-purpose setter.
        return self._status


def main() -> None:
    booking = Booking("b-1", date(2026, 7, 1), date(2026, 7, 5))
    print(booking.status)
    booking.confirm()
    print(booking.status)
    booking.cancel()
    print(booking.status)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- construction validates required invariants
- state changes happen through named methods
- callers see behavior, not raw internal state
- invalid transitions fail close to the domain object

---

## 16. Practical Question

> You are designing a booking domain model. How would you use encapsulation and abstraction to prevent invalid booking states while keeping the API easy for other developers to use?

---

## 17. Strong Answer

I would model booking as a domain object that owns its important invariants. Dates should be validated during construction or through a factory so a booking cannot exist with checkout before check-in. Status should not be changed through a generic setter. Instead, I would expose intention-revealing methods like `confirm`, `cancel`, or `complete`, and each method would enforce the allowed transition rules.

For abstraction, callers should not need to know how the booking stores status internally or how cancellation rules are calculated. They should ask the booking or a cancellation policy whether the operation is allowed. If the booking contains internal collections such as room reservations, I would avoid returning mutable references and instead expose controlled methods or immutable views.

The key idea is that the public API should speak the domain language and make invalid states difficult to create. DTOs at API boundaries can stay simple, but the domain model should protect business rules.

---

## 18. Revision Notes

- One-line summary: Encapsulation protects object state and invariants, while abstraction exposes a clean contract that hides unnecessary implementation detail.
- Three keywords: invariants, behavior, contract
- One interview trap: using private fields plus public setters and calling it encapsulation
- One memory trick: lock the safe, expose the keypad

---

# Topic 3: Composition vs Inheritance

> Track: 2.1 Object-Oriented Design
> Scope: code reuse, behavior reuse, type hierarchies, substitutability, capabilities, strategies, and flexible object assembly

---

## 1. Intuition

Think of hotel rooms.

You could create a giant inheritance tree:
- `Room`
- `DeluxeRoom`
- `DeluxeRoomWithBreakfast`
- `DeluxeRoomWithBreakfastAndOceanView`
- `SuiteWithBreakfastAndLateCheckout`

This becomes messy fast because features combine in many ways.

A better design is often composition:
- a room has a pricing policy
- a room has amenities
- a room has cancellation policy
- a room has housekeeping rules

Inheritance says:
- this object is a specialized kind of another object

Composition says:
- this object is built from smaller behaviors or parts

Short memory trick:
- inheritance is `is-a`
- composition is `has-a` or `uses-a`
- composition is usually safer when behaviors vary independently

---

## 2. Definition

- Definition: Inheritance is an object-oriented mechanism where one class derives from another and inherits its structure or behavior.
- Definition: Composition is a design technique where a class is built by containing or delegating to other objects.
- Category: Object relationship and behavior-reuse design
- Core idea: Use inheritance for true substitutable type relationships and composition for flexible combinations of behavior.

Interview shortcut:
- prefer composition when features vary independently
- use inheritance only when the child can safely replace the parent
- inheritance couples subclasses to base-class decisions
- composition lets behavior be swapped, tested, and combined

---

## 3. Why It Exists

Object-oriented systems need reuse and specialization.

The early temptation is to reuse code through inheritance. That works in some cases, but becomes dangerous when the domain varies across multiple dimensions.

Examples:
- rooms vary by size, view, breakfast, cancellation, loyalty discount, and accessibility
- notifications vary by channel, template, language, and retry behavior
- payments vary by provider, region, fraud rules, and capture mode

If each variation becomes a subclass, the hierarchy explodes.

Without composition:
- subclasses multiply rapidly
- base classes become fragile
- optional behavior is forced into parent types
- changes to the base class accidentally affect all children
- testing one behavior requires constructing deep class hierarchies

Composition exists because many behaviors are not true type relationships. They are capabilities, policies, or collaborators.

Inheritance still has a place, but it should describe a stable `is-a` relationship, not just a desire to reuse two methods.

---

## 4. Reality

### Composition is common in:

- strategy pattern
- decorator pattern
- dependency injection
- policy-based domain design
- service classes composed from repositories, gateways, and validators
- notification systems
- pricing and discount engines
- payment workflows

### Inheritance is common in:

- framework base classes
- exception hierarchies
- simple stable domain taxonomies
- abstract templates with controlled variation points
- test fixtures or shared base test classes, used carefully

### Real-world architecture truth

Most business behavior changes across multiple axes.

That means composition usually handles change better than inheritance.

Example:
- `EmailNotification` and `SmsNotification` may share the concept of sending a message.
- Retry policy, template rendering, localization, and delivery provider can vary independently.
- Composition lets each part vary without creating subclasses for every combination.

Another important truth:
- inheritance exposes subclasses to base-class internals and lifecycle decisions

That is powerful when intentional, but risky when used casually.

---

## 5. How It Works

At a high level:

1. Ask whether the relationship is truly `is-a` or mostly `has-a`.
2. Check whether the child can replace the parent without surprising callers.
3. Identify independent behavior dimensions.
4. Model variable behavior as strategies, policies, capabilities, or collaborators.
5. Inject or assemble those collaborators into the object that needs them.
6. Keep inheritance shallow and stable when used.
7. Favor composition when new combinations are expected.

### Inheritance flow

- Parent defines common contract or behavior.
- Child extends or customizes behavior.
- Callers can use the child through the parent type safely.

Inheritance answers:
- is this child truly a valid kind of the parent?

### Composition flow

- Main object owns or references smaller behavior objects.
- Behavior is delegated to collaborators.
- New combinations can be created by plugging in different collaborators.

Composition answers:
- which behavior does this object use to complete its job?

### Failure path

- Deep hierarchies become hard to understand.
- Base-class changes break subclasses.
- Subclasses inherit behavior they do not want.
- Optional behavior creates unsupported methods.

### Recovery path

- Extract variable behavior into interfaces.
- Replace inheritance branches with composed strategies.
- Keep base classes small and stable if inheritance remains.
- Use delegation for optional capabilities.

---

## 6. What Problem It Solves

- Primary problem solved: helps choose the right relationship model for reuse and variation.
- Secondary benefits: fewer fragile hierarchies, better testability, easier behavior combinations, and clearer domain modeling.
- Systems impact: shifts LLD from rigid class trees to flexible behavior assembly.

This topic solves three practical problems:
- how do we reuse behavior without creating fragile inheritance trees?
- how do we model independent feature combinations?
- how do we avoid subclasses that violate parent expectations?

---

## 7. When to Rely on It

Use composition when:
- behavior varies independently
- features can be combined in many ways
- runtime swapping is useful
- dependencies need mocking or replacement
- the relationship is `has-a` or `uses-a`

Use inheritance when:
- the relationship is truly `is-a`
- the base contract is stable
- subtypes are safely substitutable
- shared behavior is fundamental to the type hierarchy

Especially valuable for:
- pricing policies
- notification channels
- payment providers
- validation rules
- room amenities
- cancellation policies
- retry and backoff behavior

Strong interviewer keywords:
- `is-a`
- `has-a`
- strategy
- delegation
- substitutability
- class explosion
- fragile base class
- capability

---

## 8. When Not to Use It

Do not blindly replace every inheritance relationship with composition.

Be careful when:
- the type hierarchy is small, stable, and naturally substitutable
- composition adds too many tiny objects without real variation
- delegation only forwards every method without simplifying anything
- framework requirements expect inheritance

Avoid these patterns:
- inheriting only to reuse code
- deep hierarchies for independent feature combinations
- base classes with many protected fields subclasses manipulate freely
- parent methods that subclasses cannot support
- composition objects named vaguely, such as `Helper` or `Util`, with no domain meaning

Better framing:
- inheritance for stable taxonomy
- composition for behavior variation
- delegation for capabilities
- shallow hierarchies when inheritance is used

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Inheritance | Simple reuse for stable `is-a` relationships and can express clear taxonomies | Can create fragile base classes, deep hierarchies, and Liskov violations |
| Composition | Flexible, testable, supports independent behavior combinations, and reduces hierarchy complexity | Can add more objects and delegation if overused |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Reuse vs coupling:
  inheritance reuses behavior directly, but couples subclasses to base-class decisions.
- Flexibility vs object count:
  composition enables combinations, but may introduce more small classes.
- Compile-time structure vs runtime assembly:
  inheritance fixes behavior in a class hierarchy, while composition can assemble behavior at runtime.
- Simplicity vs future variation:
  inheritance may be simpler today, but composition adapts better when behavior axes grow.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Inheriting for code reuse only | The subtype may not be a true kind of parent | Extract shared behavior into a collaborator |
| Deep room hierarchy | Features combine independently and explode subclasses | Compose room with amenities, pricing, and policies |
| Parent class with optional methods | Some children cannot support the contract | Split interfaces or use capabilities |
| Over-composition with vague helpers | The design becomes harder to read | Name collaborators by domain responsibility |
| Base class knows too much | Changes ripple through all subclasses | Keep base classes small or replace with composition |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Inheritance depth:
  more than two or three levels in business code deserves scrutiny.
- Variation axes:
  if behavior varies across more than one dimension, composition is usually safer.
- Subclass count:
  rapidly growing subclasses often signal class explosion.
- Unsupported methods:
  even one normal parent method unsupported by a child is a design smell.
- Test friction:
  if testing one behavior requires a deep hierarchy, extract a collaborator.

Interview shorthand:
- inherit stable identity, compose variable behavior

---

## 12. Failure Modes

### Class explosion

Problem:
- Room types multiply for every combination of view, breakfast, cancellation, and discount.

User impact:
- adding one feature creates many new subclasses

Mitigation:
- model amenities, pricing, and cancellation as composed policies or capabilities

### Fragile base class

Problem:
- A change in `BaseNotification` breaks email, SMS, and push notifications.

User impact:
- unrelated channels fail because they depend on base-class internals

Mitigation:
- move shared behavior into small collaborators
- keep channel implementations behind a common interface

### Liskov violation

Problem:
- `ReadOnlyBookingRepository` extends `BookingRepository` but throws errors for `save`.

User impact:
- callers using the parent contract fail unexpectedly

Mitigation:
- split read and write interfaces
- compose services with the capability they need

### Over-delegation

Problem:
- Every class only forwards calls to another class with no meaningful responsibility.

User impact:
- code becomes harder to follow without gaining flexibility

Mitigation:
- keep composition around real behavior boundaries
- merge objects that do not represent useful responsibilities

---

## 13. Scenario

- Product / system: Hotel room pricing and booking options
- Requirement:
  rooms may have breakfast, ocean view, loyalty discounts, refundable or non-refundable cancellation, and seasonal pricing
- Good design:
  keep `Room` as the core entity and compose it with pricing policy, cancellation policy, and amenity set instead of creating subclasses for every combination
- Why this concept fits:
  room behavior varies across independent dimensions
- What would go wrong without it:
  inheritance would create many brittle subclasses and make new combinations expensive

---

## 14. Java Code Sample

### Composing room behavior with policies

```java
import java.math.BigDecimal;
import java.util.Set;

// LLD concept: Room composes policies instead of subclassing every feature combination.
public record Room(String roomId, Set<String> amenities, PricingPolicy pricingPolicy, CancellationPolicy cancellationPolicy) {
    public BigDecimal priceFor(int nights) {
    // LLD concept: delegation lets pricing vary independently from Room identity.
        return pricingPolicy.priceFor(nights, amenities);
    }

    public boolean canCancel(int hoursBeforeCheckIn) {
    // LLD concept: cancellation is another replaceable capability, not another subclass branch.
        return cancellationPolicy.canCancel(hoursBeforeCheckIn);
    }
}

// LLD concept: small policy contracts make independent behavior swappable.
interface PricingPolicy {
    BigDecimal priceFor(int nights, Set<String> amenities);
}

interface CancellationPolicy {
    boolean canCancel(int hoursBeforeCheckIn);
}

class SeasonalPricing implements PricingPolicy {
    public BigDecimal priceFor(int nights, Set<String> amenities) {
        BigDecimal base = new BigDecimal("150").multiply(BigDecimal.valueOf(nights));
        if (amenities.contains("ocean-view")) {
            base = base.add(new BigDecimal("75"));
        }
        return base;
    }
}

class FlexibleCancellation implements CancellationPolicy {
    public boolean canCancel(int hoursBeforeCheckIn) {
        return hoursBeforeCheckIn >= 24;
    }
}
```

Key idea:
- `Room` does not need a subclass for every feature combination; policies and amenities carry variable behavior

---

## 15. Python Mini Program / Simulation

This mini program shows how behavior can be assembled through composition.

```python
from dataclasses import dataclass
from typing import Protocol


class PricingPolicy(Protocol):
  # LLD concept: pricing behavior is a replaceable capability of Room.
    def price(self, nights: int) -> int:
        pass


class StandardPricing:
    def price(self, nights: int) -> int:
        return nights * 120


class PeakSeasonPricing:
    def price(self, nights: int) -> int:
        return nights * 180


@dataclass(frozen=True)
class Room:
    room_id: str
    amenities: tuple[str, ...]
  # LLD concept: composition; Room has pricing behavior instead of inheriting a pricing subclass.
    pricing_policy: PricingPolicy

    def quote(self, nights: int) -> int:
    # LLD concept: combine composed behavior and local data without creating subclass explosion.
        amenity_fee = 30 if "breakfast" in self.amenities else 0
        return self.pricing_policy.price(nights) + amenity_fee


def main() -> None:
    rooms = [
        Room("standard-1", (), StandardPricing()),
        Room("suite-1", ("breakfast", "ocean-view"), PeakSeasonPricing()),
    ]

    for room in rooms:
        print(f"{room.room_id}: {room.quote(2)}")


if __name__ == "__main__":
    main()
```

What this demonstrates:
- room identity and pricing behavior are separate
- pricing can change without subclassing room
- amenities combine naturally
- composition avoids subclass explosion

---

## 16. Practical Question

> You are designing hotel room types with amenities, pricing rules, and cancellation policies. Would you use inheritance or composition, and why?

---

## 17. Strong Answer

I would avoid a deep inheritance hierarchy for room combinations. A room can vary by amenities, pricing policy, cancellation policy, loyalty rules, and seasonal behavior. Those are independent axes of change, so inheritance would likely create class explosion, such as `SuiteWithBreakfastAndOceanViewAndFlexibleCancellation`.

I would keep inheritance only if there is a stable type relationship where a subtype can safely replace the parent. For example, a small stable hierarchy may be acceptable for broad room categories if the behavior contract is consistent. But pricing, cancellation, and amenities are better modeled through composition.

The design I would prefer is a `Room` object composed with `PricingPolicy`, `CancellationPolicy`, and a set of amenities. That lets us add a new pricing rule or cancellation option without rewriting the room hierarchy. It is easier to test, easier to combine, and safer when requirements grow.

---

## 18. Revision Notes

- One-line summary: Use inheritance for stable substitutable `is-a` relationships and composition for flexible behavior that varies independently.
- Three keywords: `is-a`, `has-a`, strategy
- One interview trap: inheriting only to reuse code and accidentally creating a fragile hierarchy
- One memory trick: inherit identity, compose behavior

---

# Topic 4: Immutability

> Track: 2.1 Object-Oriented Design
> Scope: value objects, defensive copies, thread safety, state transitions, records, shared references, and mutation control

---

## 1. Intuition

Think of a printed hotel receipt.

- Once issued, the receipt should not silently change.
- If a correction is needed, the hotel issues an adjustment or a new receipt.
- Everyone looking at the old receipt sees the same facts.

That is the feeling of immutability.

An immutable object is created once and then does not change. If a different value is needed, create a new object.

Short memory trick:
- mutable object: same object, changing state
- immutable object: new object, new state

In LLD, immutability makes code easier to reason about because callers do not need to worry that someone else changed the object behind their back.

---

## 2. Definition

- Definition: Immutability is a design property where an object's observable state cannot change after construction.
- Category: Object state management and correctness technique
- Core idea: Represent stable values as objects that cannot be modified, and model changes by creating new values or explicit state transitions elsewhere.

Interview shortcut:
- immutable objects are safer to share
- immutable value objects are easier to test
- immutability reduces accidental side effects
- immutable does not mean every object in the system must never change

---

## 3. Why It Exists

Mutable shared state is one of the easiest ways to create subtle bugs.

Examples:
- one service changes a `Money` amount after it was used for payment calculation
- a caller modifies a date range after availability was checked
- a list returned from an object is changed by outside code
- two threads share a mutable object and see inconsistent state
- a cached object is mutated and corrupts future reads

Without immutability:
- data can change unexpectedly
- reasoning about state over time becomes harder
- defensive copying is often forgotten
- concurrency bugs become more likely
- objects used as map keys can break if their fields change

Immutability exists because many domain concepts are values, not living entities.

Examples:
- money
- date range
- address
- email
- room type
- price quote
- booking snapshot

These should usually be immutable because their identity is their value.

---

## 4. Reality

### Immutability is common in:

- Java records
- value objects
- DTOs and event payloads
- configuration objects
- command objects
- snapshots
- map keys and set elements
- concurrent systems

### Common examples

- `Money(amount, currency)`
- `DateRange(checkIn, checkOut)`
- `GuestId(value)`
- `EmailAddress(value)`
- `PriceQuote(total, taxes, expiresAt)`
- `BookingConfirmedEvent(...)`

### Real-world architecture truth

Immutable value objects pair well with mutable entities.

For example:
- `Booking` may be a mutable entity with lifecycle transitions.
- `Money`, `DateRange`, and `GuestId` inside it can be immutable values.

Another important truth:
- final fields are not enough if the object contains mutable references

If an immutable class stores a mutable list and returns it directly, callers can still mutate its contents. True immutability needs defensive copies or immutable collections.

---

## 5. How It Works

At a high level:

1. Identify values that should not change after creation.
2. Make fields private and final where the language supports it.
3. Validate all invariants during construction.
4. Do not expose setters.
5. Copy mutable inputs before storing them.
6. Return immutable views or copies for collections.
7. For changes, return a new object with the updated value.

### Value object flow

- Construct the object with all required values.
- Validate invariants immediately.
- Use equality based on value rather than object identity.
- Share freely because state cannot change.

Value object answers:
- is this concept defined by its values rather than a lifecycle identity?

### Defensive-copy flow

- Copy mutable constructor inputs.
- Store the copy internally.
- Return copies or immutable views from accessors.

Defensive copying answers:
- can external code mutate this object through a reference it already has?

### State-change flow

- For immutable values, create a new object with the new value.
- For mutable entities, keep mutation controlled through domain methods.

State-change answers:
- should this change create a new value or transition an entity?

### Failure path

- A supposedly immutable object exposes a mutable list.
- An object used as a hash key changes after insertion.
- A cached object is modified by one caller and affects another.
- State changes happen silently through shared references.

### Recovery path

- Convert simple values to immutable value objects.
- Use records or final fields where appropriate.
- Add defensive copies for mutable inputs.
- Separate immutable values from mutable lifecycle entities.

---

## 6. What Problem It Solves

- Primary problem solved: reduces accidental state changes and makes values safer to share, compare, cache, and pass across boundaries.
- Secondary benefits: easier testing, safer concurrency, clearer domain models, fewer defensive checks, and more predictable behavior.
- Systems impact: makes LLD designs more reliable by minimizing uncontrolled mutation.

This topic solves three practical problems:
- how do we stop objects from changing unexpectedly?
- how do we make values safe to share across code paths?
- how do we reduce mutation-related bugs in concurrent or cached systems?

---

## 7. When to Rely on It

Use immutability when:
- modeling value objects
- objects are shared across threads
- objects are cached
- objects are used as keys in maps or sets
- data represents a snapshot or event
- construction can validate all required fields upfront
- state changes should be explicit and traceable

Especially valuable for:
- money and currency
- date ranges
- IDs and typed identifiers
- request commands
- domain events
- configuration
- price quotes
- immutable DTOs

Strong interviewer keywords:
- value object
- final fields
- defensive copy
- no setters
- thread safety
- snapshot
- side effects
- safe sharing

---

## 8. When Not to Use It

Do not force immutability where lifecycle mutation is the core behavior.

Be careful when:
- the object is a long-lived entity with real state transitions
- copying very large structures creates performance pressure
- persistence frameworks require mutable constructors or setters
- mutation is local, controlled, and simpler
- object creation rate becomes a measurable bottleneck

Avoid these patterns:
- making an entity immutable but then adding confusing `withStatus` chains everywhere
- assuming final fields protect mutable collections
- using immutable wrappers while nested objects remain mutable
- copying large graphs unnecessarily
- treating immutability as a substitute for domain modeling

Better framing:
- immutable values
- controlled mutable entities
- immutable snapshots and events
- mutation only behind intention-revealing methods

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Immutability | Reduces side effects, improves thread safety, simplifies testing, and makes values safe to share | Can require object copying, may not fit lifecycle entities, and needs care with nested mutable data |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Safety vs allocation:
  new objects avoid mutation bugs, but may allocate more memory.
- Simplicity vs lifecycle modeling:
  immutable values are simple, but mutable entities may better model real workflows.
- Defensive copying vs performance:
  copies protect state, but large collections need careful design.
- Framework convenience vs domain correctness:
  some frameworks prefer mutable objects, but domain models may still benefit from immutability.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Final fields with mutable list | The reference is final but the list contents can change | Use immutable collections or defensive copies |
| Public setters on value objects | The value can change after validation | Validate in constructor and remove setters |
| Mutable map keys | Changing fields can break hash-based lookup | Use immutable key objects |
| Sharing mutable cached objects | One caller can corrupt data for others | Cache immutable snapshots |
| Making every entity immutable | Real lifecycle transitions become awkward | Use immutable values inside controlled mutable entities |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Value object fields:
  should usually be assigned once and validated at creation.
- Collection exposure:
  zero direct mutable collection leaks is the goal for immutable objects.
- Equality:
  value objects should compare based on values, not object identity.
- Thread safety:
  immutable objects are naturally safe to share after construction.
- Mutation points:
  fewer mutation points means easier reasoning and simpler tests.

Interview shorthand:
- immutable values, mutable entities, defensive copies, no surprise changes

---

## 12. Failure Modes

### Mutable value object

Problem:
- `Money` has setters for amount and currency.

User impact:
- payment calculations can change after validation or authorization

Mitigation:
- make `Money` immutable and validate amount plus currency during construction

### Collection leak

Problem:
- `BookingSnapshot` returns a mutable internal list of rooms.

User impact:
- callers can alter historical snapshot data

Mitigation:
- copy input lists and return immutable lists

### Hash key corruption

Problem:
- A mutable `DateRange` is used as a map key and later modified.

User impact:
- cached availability lookup fails or returns inconsistent results

Mitigation:
- use immutable keys for maps and sets

### Hidden nested mutability

Problem:
- An immutable outer object contains mutable nested objects.

User impact:
- callers mutate nested state and bypass outer-object guarantees

Mitigation:
- make nested values immutable or deep-copy when needed

---

## 13. Scenario

- Product / system: Hotel pricing and booking snapshot model
- Requirement:
  once a price quote is shown to the user, the quote should not change unexpectedly while checkout continues
- Good design:
  model `Money`, `DateRange`, and `PriceQuote` as immutable values; create a new quote when recalculation is needed; keep booking lifecycle changes explicit in the booking entity
- Why this concept fits:
  pricing and checkout logic require stable values and predictable comparisons
- What would go wrong without it:
  a shared mutable quote could change between display, payment authorization, and confirmation

---

## 14. Java Code Sample

### Immutable money and price quote

```java
import java.math.BigDecimal;
import java.time.Instant;

public record Money(BigDecimal amount, String currency) {
    public Money {
    // LLD concept: immutable value objects validate all invariants at creation time.
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException("currency mismatch");
        }
      // LLD concept: return a new value instead of mutating the existing Money instance.
        return new Money(amount.add(other.amount()), currency);
    }
}

public record PriceQuote(String quoteId, Money total, Instant expiresAt) {
    public boolean expiredAt(Instant now) {
      // LLD concept: behavior can live on immutable values without changing their state.
        return !expiresAt.isAfter(now);
    }
}
```

Key idea:
- adding money returns a new `Money` object; the original values remain stable and safe to share

---

## 15. Python Mini Program / Simulation

This mini program shows immutable value replacement instead of mutation.

```python
from dataclasses import dataclass, replace


@dataclass(frozen=True)
class Money:
  # LLD concept: frozen dataclass models a value object with no post-construction mutation.
    amount: int
    currency: str

    def add(self, other: "Money") -> "Money":
        if self.currency != other.currency:
            raise ValueError("currency mismatch")
    # LLD concept: create a new Money value instead of changing self.amount.
        return Money(self.amount + other.amount, self.currency)


@dataclass(frozen=True)
class PriceQuote:
    quote_id: str
    total: Money
    version: int


def main() -> None:
    original = PriceQuote("q-1", Money(300, "USD"), 1)
  # LLD concept: replacing creates a new quote version while the original snapshot stays stable.
    updated = replace(original, total=original.total.add(Money(25, "USD")), version=2)

    print(original)
    print(updated)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- the original quote remains unchanged
- changes create a new value
- immutable objects are easy to compare and log
- side effects are reduced

---

## 16. Practical Question

> You are designing price quotes and booking snapshots for a hotel booking checkout flow. Which objects would you make immutable, and how would you handle changes?

---

## 17. Strong Answer

I would make value objects immutable: `Money`, `DateRange`, `GuestId`, `RoomId`, `PriceQuote`, and booking snapshots. These objects are defined by their values, and they are often shared between pricing, checkout, payment, and confirmation flows. If they can change silently, it becomes hard to reason about what the user saw versus what the system charged.

For changes, I would create new values. For example, recalculating a quote should produce a new `PriceQuote` with a new version or expiry, not mutate the old one. A booking entity itself may still be mutable because it has a lifecycle, but its transitions should be controlled through methods like `confirm` or `cancel`.

I would also protect collections through defensive copies or immutable collections. The key distinction is that values should be immutable, while entities may mutate through explicit domain behavior.

---

## 18. Revision Notes

- One-line summary: Immutability makes values safe to share by preventing observable state changes after construction.
- Three keywords: value object, defensive copy, no setters
- One interview trap: using final fields but exposing mutable nested collections
- One memory trick: issue a new receipt, do not rewrite the old one

---

# Topic 5: Cohesion and Coupling

> Track: 2.1 Object-Oriented Design
> Scope: class responsibility quality, dependency strength, module boundaries, change impact, god classes, feature envy, and maintainable collaboration

---

## 1. Intuition

Think of departments in a hotel.

High cohesion means each department has a focused purpose.

- Housekeeping handles room readiness.
- Front desk handles check-in and guest coordination.
- Finance handles payments and invoices.
- Maintenance handles repairs.

Low coupling means departments coordinate through clear handoffs instead of everyone depending on everyone else's internal checklist.

If housekeeping needs to know how finance calculates tax before cleaning a room, the hotel is badly coupled.

Short memory trick:
- cohesion: do the things inside this class belong together?
- coupling: how much does this class know about other classes?

Good OOD aims for high cohesion and low coupling.

---

## 2. Definition

- Definition: Cohesion describes how closely related the responsibilities inside a class, module, or component are.
- Definition: Coupling describes how strongly one class, module, or component depends on another.
- Category: Design quality, maintainability, and modularity
- Core idea: Put related behavior together and keep dependencies between objects narrow, explicit, and stable.

Interview shortcut:
- high cohesion means focused responsibility
- low coupling means fewer fragile dependencies
- the goal is not zero coupling; the goal is appropriate coupling through stable contracts

---

## 3. Why It Exists

Large codebases become hard to change when responsibilities are scattered or dependencies are tangled.

Without cohesion:
- classes do unrelated work
- changes for one feature risk breaking another feature
- naming becomes vague, such as `BookingManager` or `CommonUtil`
- tests require unrelated setup
- ownership is unclear

Without coupling control:
- one class knows too much about another's internals
- implementation changes ripple across many files
- tests become slow and brittle
- modules cannot evolve independently
- circular dependencies appear

Cohesion and coupling exist as design lenses.

They help answer:
- should this method live here?
- should this class know about that detail?
- is this dependency stable enough?
- will this change spread across the codebase?

---

## 4. Reality

### Cohesion and coupling appear in:

- class design
- package design
- service-layer design
- module boundaries
- domain-driven design aggregates
- controller-service-repository layering
- microservice boundaries
- testability discussions

### Common high-cohesion examples

- `Money` handles money-specific validation and operations
- `Booking` handles booking lifecycle transitions
- `PricingPolicy` calculates prices
- `PaymentGateway` hides payment-provider interaction
- `InventoryService` reserves and releases room inventory

### Common high-coupling examples

- controller directly builds SQL queries
- domain object imports HTTP clients
- checkout service knows payment provider response internals
- class reads another object's private state through many getters
- every service depends on a shared giant utility class

### Real-world architecture truth

Low coupling does not mean no dependencies.

Objects need to collaborate. The goal is to make collaboration intentional through stable, narrow contracts.

Another important truth:
- high cohesion often reduces coupling naturally

When responsibilities are focused, classes need fewer unrelated dependencies.

---

## 5. How It Works

At a high level:

1. Identify what each class is responsible for.
2. Check whether its methods and fields support that responsibility.
3. Move unrelated behavior to more appropriate classes.
4. Replace knowledge of internals with method calls or interfaces.
5. Keep dependency direction aligned with business flow.
6. Reduce broad shared utilities and vague managers.
7. Test whether changes stay localized.

### Cohesion flow

- A class name should predict its methods.
- Methods should operate on the class's core data or responsibility.
- If half the methods use one set of fields and half use another, the class may be two classes.

Cohesion answers:
- do these responsibilities belong together?

### Coupling flow

- A class depends only on what it needs.
- Dependencies are passed through contracts where variation exists.
- Internal details stay hidden behind public methods.
- Changes in one implementation should not force unrelated callers to change.

Coupling answers:
- how much does this class need to know about the other class?

### Boundary flow

- Controllers handle transport concerns.
- Services coordinate use cases.
- Domain objects protect rules.
- Repositories handle persistence.
- Gateways handle external systems.

Boundary design answers:
- is each layer doing the kind of work it owns?

### Failure path

- Classes become god objects.
- Shared utility classes become dumping grounds.
- Circular dependencies appear.
- Tests need large fixture graphs.
- Small changes ripple through many modules.

### Recovery path

- Split classes by reason to change.
- Move behavior to the data it uses most.
- Introduce narrow interfaces around external details.
- Replace global utilities with domain-specific services or value objects.
- Add tests before refactoring heavily coupled code.

---

## 6. What Problem It Solves

- Primary problem solved: keeps classes and modules understandable by grouping related behavior and limiting unnecessary dependency knowledge.
- Secondary benefits: easier testing, smaller change impact, clearer ownership, better naming, and safer refactoring.
- Systems impact: makes LLD designs evolve through local changes instead of cross-codebase edits.

This topic solves three practical problems:
- how do we know whether a class is doing too much?
- how do we prevent dependency tangles?
- how do we keep changes localized?

---

## 7. When to Rely on It

Use cohesion and coupling analysis when:
- a class is hard to name clearly
- a class has many unrelated methods
- tests require many mocks
- changes spread across many files
- dependencies form cycles
- a service imports too many infrastructure details
- multiple teams are stepping on the same code

Especially valuable for:
- checkout services
- booking lifecycle modules
- pricing engines
- notification systems
- payment integrations
- repository and gateway boundaries
- large service classes in Spring Boot

Strong interviewer keywords:
- high cohesion
- low coupling
- god class
- feature envy
- dependency direction
- stable contract
- change locality
- circular dependency

---

## 8. When Not to Use It

Do not chase low coupling so aggressively that collaboration disappears.

Be careful when:
- splitting a small cohesive flow makes it harder to read
- interfaces are added only to reduce coupling metrics, not real risk
- classes become tiny wrappers with no responsibility
- avoiding dependencies causes duplicate logic
- shared domain language is fragmented across too many objects

Avoid these patterns:
- one giant `Util` class used by everything
- one `Manager` class coordinating every domain concern
- moving methods away from the data they use most
- creating abstractions that leak all implementation details anyway
- circular dependencies between packages or modules

Better framing:
- keep related behavior together
- hide implementation details
- depend on stable contracts
- accept necessary coupling, reject accidental coupling

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| High cohesion and low coupling | Improve readability, testability, change locality, and module ownership | Can lead to over-splitting or excessive interfaces if pursued mechanically |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Locality vs separation:
  keeping related logic together improves readability, but too much togetherness becomes a god class.
- Low coupling vs duplication:
  reducing dependencies is good, but avoiding all dependencies can duplicate business rules.
- Interface boundaries vs simplicity:
  interfaces reduce coupling at real seams, but unnecessary interfaces add navigation cost.
- Layering vs domain behavior:
  strict layers help structure, but rules should still live near the domain concepts they protect.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| God class | Too many responsibilities change for different reasons | Split by cohesive domain responsibility |
| Feature envy | A method mostly uses another object's data | Move behavior closer to the data or expose a domain method |
| Giant utility class | Unrelated behavior becomes globally coupled | Create focused value objects or domain services |
| Circular dependencies | Modules cannot evolve or test independently | Fix dependency direction and extract stable contracts |
| Over-splitting | The flow becomes harder to understand | Keep small behavior together when it changes together |
| Infrastructure leak | Business logic depends on SQL, HTTP, or SDK details | Hide details behind repositories or gateways |

---

## 11. Key Numbers

These are practical heuristics, not universal laws.

- Constructor dependencies:
  many unrelated dependencies often suggest low cohesion.
- Reasons to change:
  more than one unrelated reason to change suggests a split.
- Method-field usage:
  methods that use different field groups may indicate multiple responsibilities.
- Change radius:
  a small feature touching many modules may indicate high coupling.
- Test fixture size:
  a unit test that needs many unrelated mocks often reveals coupling.
- Package cycles:
  circular dependencies are strong warning signs.

Interview shorthand:
- related inside, narrow outside, stable contracts between

---

## 12. Failure Modes

### God checkout service

Problem:
- Checkout handles pricing, inventory, payment, notification, auditing, persistence, and response formatting.

User impact:
- every change risks the full checkout path and tests become fragile

Mitigation:
- keep checkout as orchestration and move pricing, inventory, payment, and notification into focused collaborators

### Feature envy

Problem:
- `BookingService` repeatedly pulls fields from `Booking` to decide whether cancellation is allowed.

User impact:
- booking rules are scattered and duplicated

Mitigation:
- move cancellation behavior into `Booking` or a `CancellationPolicy`

### Utility dumping ground

Problem:
- `BookingUtils` contains date logic, money formatting, validation, pricing, and email helpers.

User impact:
- unrelated modules become coupled to a shared unstable class

Mitigation:
- extract cohesive value objects and domain services such as `DateRange`, `Money`, and `PricingPolicy`

### Circular package dependency

Problem:
- booking depends on payment, payment depends on booking internals, and both packages import each other.

User impact:
- builds, tests, and refactors become painful

Mitigation:
- define stable contracts or domain events between packages
- align dependency direction with use-case flow

---

## 13. Scenario

- Product / system: Hotel booking checkout module
- Requirement:
  calculate price, reserve inventory, charge payment, persist booking, and send confirmation while keeping code maintainable
- Good design:
  use a cohesive checkout orchestration service that depends on focused collaborators: `PricingService`, `InventoryService`, `PaymentGateway`, `BookingRepository`, and `NotificationSender`
- Why this concept fits:
  checkout has several responsibilities that must collaborate but should not collapse into one class
- What would go wrong without it:
  the checkout service would become a god class and every feature would create wide change impact

---

## 14. Java Code Sample

### Cohesive checkout orchestration with narrow dependencies

```java
public class CheckoutService {

  // LLD concept: each dependency is a focused collaborator, keeping CheckoutService cohesive as orchestration.
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final PaymentGateway paymentGateway;
    private final BookingRepository bookingRepository;
    private final NotificationSender notificationSender;

    public CheckoutService(
            PricingService pricingService,
            InventoryService inventoryService,
            PaymentGateway paymentGateway,
            BookingRepository bookingRepository,
            NotificationSender notificationSender) {
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        this.paymentGateway = paymentGateway;
        this.bookingRepository = bookingRepository;
        this.notificationSender = notificationSender;
    }

    public void checkout(BookingRequest request) {
      // LLD concept: narrow handoffs reduce coupling; checkout does not know each collaborator's internals.
        Price price = pricingService.price(request);
        inventoryService.reserve(request.hotelId(), request.nights());
        PaymentResult payment = paymentGateway.charge(request.userId(), price);
        bookingRepository.saveConfirmed(request, price, payment.reference());
        notificationSender.sendBookingConfirmation(request.userId());
    }
}
```

Key idea:
- checkout remains cohesive as orchestration, while each collaborator owns a focused responsibility behind a narrow contract

---

## 15. Python Mini Program / Simulation

This mini program shows how a god function can be split into cohesive collaborators.

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class BookingRequest:
    user_id: str
    hotel_id: str
    nights: int


class PricingService:
    def price(self, request: BookingRequest) -> int:
    # LLD concept: pricing responsibility stays cohesive inside PricingService.
        return request.nights * 150


class InventoryService:
    def reserve(self, request: BookingRequest) -> None:
    # LLD concept: inventory behavior is isolated from checkout orchestration.
        print(f"reserved room for {request.hotel_id}")


class PaymentGateway:
    def charge(self, user_id: str, amount: int) -> str:
    # LLD concept: payment integration is hidden behind a gateway boundary.
        print(f"charged {user_id}: {amount}")
        return "payment-ref-1"


class CheckoutService:
    def __init__(self, pricing: PricingService, inventory: InventoryService, payments: PaymentGateway) -> None:
    # LLD concept: explicit dependencies show collaboration without mixing responsibilities.
        self.pricing = pricing
        self.inventory = inventory
        self.payments = payments

    def checkout(self, request: BookingRequest) -> None:
    # LLD concept: CheckoutService coordinates the workflow but delegates detailed work.
        amount = self.pricing.price(request)
        self.inventory.reserve(request)
        reference = self.payments.charge(request.user_id, amount)
        print(f"confirmed booking with {reference}")


def main() -> None:
    service = CheckoutService(PricingService(), InventoryService(), PaymentGateway())
    service.checkout(BookingRequest("user-1", "hotel-7", 2))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- collaborators have focused responsibilities
- checkout coordinates without knowing internal details
- dependencies are explicit
- change impact is smaller when behavior lives in the right place

---

## 16. Practical Question

> You are reviewing an LLD design where `BookingManager` handles pricing, inventory, payment, notification, database writes, and response formatting. How would you improve cohesion and reduce coupling?

---

## 17. Strong Answer

I would first identify the reasons `BookingManager` changes. Pricing rules, inventory reservation, payment provider logic, persistence, notifications, and response formatting are different responsibilities. Keeping them in one class creates low cohesion and high change risk.

I would keep a checkout or booking application service as orchestration, but extract focused collaborators. Pricing should move to `PricingService` or `PricingPolicy`, inventory to `InventoryService`, provider-specific payment code behind `PaymentGateway`, database writes behind `BookingRepository`, and notifications behind `NotificationSender`. The controller should handle request and response mapping rather than domain decisions.

To reduce coupling, each dependency should expose a narrow contract. Checkout should not know SQL details, payment-provider response internals, or email-template mechanics. I would also watch for feature envy: if a method mostly pulls data from `Booking`, the behavior may belong on `Booking` or a domain policy. The goal is high cohesion inside each class and stable, limited knowledge between classes.

---

## 18. Revision Notes

- One-line summary: Cohesion measures whether responsibilities inside a class belong together, while coupling measures how much that class depends on other classes or details.
- Three keywords: focus, dependency, change radius
- One interview trap: splitting everything into tiny classes but still leaking implementation details everywhere
- One memory trick: focused departments, clean handoffs