# Java Modern LTS 17, 21, 25 FAANG Master Sheet

Target: Java interviews where the candidate must show modern language awareness without overclaiming preview features.

This sheet covers:
- Java 8 baseline
- Java 11 awareness
- Java 17 LTS
- Java 21 LTS
- Java 25 LTS
- Java 26/27 awareness safety
- Records, sealed classes, pattern matching
- Virtual threads, sequenced collections
- Scoped values, structured concurrency
- How to discuss preview/incubator features safely

---

## 1. Mental Model

Modern Java is not one feature.

It is a shift in three areas:

```text
Language clarity
    -> records, switch expressions, pattern matching, sealed classes

Runtime scalability
    -> virtual threads, modern GC, profiling, compact headers

Developer productivity
    -> text blocks, HTTP client, collection factories, better APIs
```

Strong interview line:

```text
I separate stable LTS features from preview/incubator features. I can discuss the latest
direction of Java, but production adoption depends on the exact JDK, framework support,
and team policy.
```

---

## 2. Interview Priority Meter

| Feature Area | Priority | Why It Matters |
|---|---:|---|
| Java 8 lambdas/streams | Very high | Backend baseline |
| Optional | Very high | Null handling judgment |
| Java 11 HTTP client/string methods | Medium-high | Common LTS awareness |
| Records | Very high | DTO/modeling |
| Sealed classes | High | Domain modeling |
| Pattern matching | High | Cleaner type handling |
| Switch expressions | High | Cleaner branching |
| Text blocks | Medium-high | SQL/JSON readability |
| Virtual threads | Very high | Modern concurrency |
| Sequenced collections | High | Java 21 API awareness |
| Scoped values | Medium-high | Context propagation |
| Structured concurrency | Medium-high | Modern concurrency design |
| Java 25 LTS awareness | High | Latest LTS maturity |
| Preview safety | Very high | Senior judgment |

---

## 3. Java Version Timeline For Interviews

| Version | Interview Role |
|---|---|
| Java 8 | Functional programming baseline |
| Java 11 | Common older LTS, HTTP client and API improvements |
| Java 17 | Widely used LTS, records/sealed classes/pattern matching awareness |
| Java 21 | Modern LTS, virtual threads and sequenced collections |
| Java 25 | Newer LTS, latest LTS awareness |
| Java 26+ | Feature-release / latest-awareness only unless project uses it |

Safe wording:

```text
My production experience depends on the project JDK, but I understand the stable LTS
features and can identify which newer features are preview or incubator.
```

---

## 4. Java 8 Foundation

Must know:

- Lambda expressions
- Functional interfaces
- Method references
- Streams
- Optional
- Default/static interface methods
- Date-Time API
- CompletableFuture
- Map enhancements

Strong answer:

```text
Java 8 added functional programming support through lambdas, functional interfaces,
streams, Optional, default methods, Date-Time API, and CompletableFuture.
```

Code:

```java
List<String> names = List.of("Aravind", "Rahul", "Anil");

List<String> result = names.stream()
    .filter(name -> name.startsWith("A"))
    .map(String::toUpperCase)
    .toList();
```

Interview caution:

```text
If the project is Java 8, use collect(Collectors.toList()) instead of stream().toList().
```

---

## 5. Java 11 Awareness

Useful features:

- Standard HTTP Client
- `String.isBlank()`
- `String.lines()`
- `String.strip()`
- `String.repeat()`
- `Files.readString()`
- `Files.writeString()`
- Local variable syntax for lambda parameters

HTTP client:

```java
import java.net.URI;
import java.net.http.*;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://example.com"))
    .GET()
    .build();

HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
);
```

`isBlank` vs `isEmpty`:

```java
"   ".isEmpty(); // false
"   ".isBlank(); // true
```

`strip` vs `trim`:

```text
strip is Unicode-aware. trim is older and removes characters up to U+0020.
```

---

## 6. Java 14/15 Awareness

Switch expressions:

```java
String label = switch (status) {
    case "NEW" -> "Created";
    case "PAID" -> "Completed";
    case "CANCELLED" -> "Stopped";
    default -> "Unknown";
};
```

Text blocks:

```java
String json = """
    {
      "name": "Aravind",
      "role": "Java Developer"
    }
    """;
```

Use cases:

- JSON test payloads
- SQL strings
- HTML templates
- Documentation snippets

Interview line:

```text
Switch expressions reduce fall-through bugs and text blocks improve readability for
multi-line strings.
```

---

## 7. Java 17 LTS

High-value features:

- Records
- Sealed classes
- Pattern matching for `instanceof`
- Text blocks
- Switch expressions
- Strong encapsulation of JDK internals

Java 17 answer:

```text
Java 17 is an important LTS release. The features I would highlight are records for compact
immutable data carriers, sealed classes for restricted hierarchies, pattern matching for
instanceof, text blocks, and switch expressions.
```

---

## 8. Records

Records are compact immutable data carriers.

Example:

```java
public record UserDto(String id, String name) {}
```

Compiler provides:

- Constructor
- Accessors
- `equals`
- `hashCode`
- `toString`

Compact constructor:

```java
public record Money(String currency, int amount) {
    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative");
        }
    }
}
```

Use for:

- DTOs
- Request/response models
- Projections
- Immutable value-like objects

Avoid for:

- JPA entities with mutable lifecycle/proxies
- Complex domain objects with identity mutation
- Objects needing custom inheritance

Strong answer:

```text
Records reduce boilerplate for immutable data carriers, but I would not blindly use them
for JPA entities because persistence frameworks often expect no-arg constructors, proxies,
and mutable lifecycle behavior.
```

---

## 9. Pattern Matching For instanceof

Before:

```java
if (obj instanceof String) {
    String value = (String) obj;
    System.out.println(value.length());
}
```

After:

```java
if (obj instanceof String value) {
    System.out.println(value.length());
}
```

Why:

```text
Combines type check, cast, and variable binding.
```

Interview line:

```text
Pattern matching reduces boilerplate and makes type checks safer and clearer.
```

---

## 10. Sealed Classes

Sealed types restrict who can extend or implement them.

Example:

```java
sealed interface Payment permits CardPayment, UpiPayment, WalletPayment {
}

record CardPayment(String cardNumber) implements Payment {
}

record UpiPayment(String upiId) implements Payment {
}

record WalletPayment(String walletId) implements Payment {
}
```

Use when:

- Domain has known subtypes.
- You want compiler-enforced hierarchy control.
- Pattern matching/switch can be exhaustive.

Avoid when:

- External teams need open extension.
- Plugin ecosystem needs unknown future implementations.

Strong answer:

```text
Sealed classes are useful for modeling closed domains, like known payment types or event
types. They improve safety by making valid subtypes explicit.
```

---

## 11. Java 21 LTS

High-value features:

- Virtual threads
- Sequenced collections
- Pattern matching for switch
- Record patterns
- Generational ZGC

Java 21 answer:

```text
Java 21 is a major LTS because virtual threads make blocking concurrency much more scalable.
It also adds sequenced collections and continues the modern pattern-matching direction.
```

---

## 12. Virtual Threads

Basic example:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> callBlockingService());
}
```

Strong answer:

```text
Virtual threads are lightweight threads for high-concurrency blocking workloads. They let
us keep simple blocking code without tying every request to an expensive OS thread.
```

Production caveat:

```text
They do not make CPU-bound work faster and they do not remove DB connection pool or remote
API limits.
```

Deep-dive file:

```text
Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md
```

---

## 13. Sequenced Collections

Sequenced collections provide common APIs for collections with encounter order.

Useful methods:

- `getFirst()`
- `getLast()`
- `addFirst()`
- `addLast()`
- `removeFirst()`
- `removeLast()`
- `reversed()`

Example:

```java
SequencedCollection<String> names = new ArrayList<>();
names.add("A");
names.add("B");

System.out.println(names.getFirst());
System.out.println(names.getLast());
```

Interview line:

```text
Sequenced collections standardize first, last, and reversed operations for ordered collections.
```

---

## 14. Pattern Matching For Switch

Example:

```java
String result = switch (payment) {
    case CardPayment card -> "card";
    case UpiPayment upi -> "upi";
    case WalletPayment wallet -> "wallet";
};
```

Why it matters:

```text
It replaces long instanceof chains with clearer type-based branching.
```

With sealed classes:

```text
The compiler can know all possible subtypes and help with exhaustive handling.
```

---

## 15. Record Patterns

Record patterns allow destructuring record values in pattern matching contexts.

Conceptual example:

```java
record Point(int x, int y) {}

// Pattern matching support depends on exact JDK feature level.
// if (obj instanceof Point(int x, int y)) {
//     System.out.println(x + y);
// }
```

Interview safety:

```text
Mention record patterns as part of modern pattern matching, but verify exact stability
and syntax in the JDK used by the project.
```

---

## 16. Java 25 LTS Awareness

Java 25 is a newer LTS release in the modern Java line.

Useful awareness areas:

- Scoped Values
- Module Import Declarations
- Compact Source Files and Instance Main Methods
- Flexible Constructor Bodies
- Key Derivation Function API
- Compact Object Headers
- Generational Shenandoah
- Structured Concurrency preview
- Stable Values preview
- Vector API incubator

Strong answer:

```text
For Java 25, I would present it as latest LTS awareness. I would separate final features
from preview/incubator APIs before recommending production use.
```

---

## 17. Scoped Values

Scoped values provide bounded immutable context sharing.

Use cases:

- Request ID
- Tenant ID
- User/security context
- Trace context

Why they matter:

```text
They are a safer modern direction for context propagation, especially with virtual threads,
compared with mutable ThreadLocal-heavy designs.
```

Interview safety:

```text
Check exact JDK status and syntax. Some related APIs changed across preview rounds.
```

---

## 18. Structured Concurrency

Structured concurrency groups related tasks under a scope.

Concept:

```text
Start subtasks together, wait together, cancel together, fail together.
```

Why:

- Easier cancellation.
- Better error handling.
- Clear task lifetime.
- Cleaner request-scoped concurrency.

Strong answer:

```text
Structured concurrency is about making concurrent subtasks behave like a single unit of
work. It improves reasoning around failure and cancellation, but I would verify whether
the API is preview in the project's JDK.
```

---

## 19. Compact Source Files And Instance Main

This makes small Java programs easier for beginners and scripts.

Conceptual shape:

```java
void main() {
    System.out.println("Hello Java");
}
```

Interview priority:

```text
Good to know, but less important than records, virtual threads, pattern matching, and JVM depth.
```

---

## 20. Module Import Declarations

Module import declarations reduce boilerplate by importing exported packages from a module.

Interview priority:

```text
Awareness topic. Useful to mention only if discussing Java 25 language convenience.
```

Do not oversell it as a daily backend feature unless your project uses modules heavily.

---

## 21. Flexible Constructor Bodies

Flexible constructor bodies relax some restrictions around statements before constructor invocation.

Why it matters:

```text
It improves constructor validation and setup patterns, but it is not usually the first
feature to discuss in backend interviews.
```

Safe line:

```text
I know it as a modern language improvement, but records, virtual threads, pattern matching,
and GC/runtime features are more important for backend interviews.
```

---

## 22. Compact Object Headers

Compact object headers reduce object header size.

Why it matters:

- Lower memory footprint.
- Better cache efficiency.
- Important for object-heavy Java applications.

Interview line:

```text
Compact object headers are runtime-level improvements aimed at reducing memory overhead
for Java objects.
```

This is a JVM awareness topic, not something most application developers directly code against.

---

## 23. Generational ZGC And Generational Shenandoah

Modern low-latency collectors are evolving with generational behavior.

Why:

```text
Most objects die young, so generational collection can improve efficiency while preserving
low-pause goals.
```

Interview answer:

```text
For low-latency Java services, I would consider collectors like ZGC or Shenandoah based
on JDK version, heap size, pause targets, CPU overhead, and production measurements.
```

---

## 24. Java 26 / 27 Awareness

Use latest feature releases as awareness, not as default production baseline.

Safe answer:

```text
I track newer JDK feature releases, but in interviews I separate latest awareness from
production recommendation. Most production teams standardize on LTS versions like 17, 21,
or 25 depending on maturity and support.
```

Examples of newer direction:

- HTTP/3 for HTTP Client API
- Continued structured concurrency previews
- Primitive types in patterns
- Post-quantum TLS/key exchange work

Important:

```text
Always verify exact GA/EA and preview status from OpenJDK or the vendor distribution before
claiming a feature is production-ready.
```

---

## 25. Preview, Incubator, Experimental

| Status | Meaning |
|---|---|
| Final | Stable production feature |
| Preview | Fully specified but not final; can change |
| Incubator | Early API/module, may change significantly |
| Experimental | JVM feature for experimentation |
| Early access | Build before GA; not normal production baseline |

Strong answer:

```text
I can discuss preview and incubator features, but I would not recommend them for production
unless the team explicitly accepts the risk, enables build flags, and has an upgrade plan.
```

---

## 26. What To Say In Interviews

If asked:

> Which Java version features do you know?

Answer:

```text
I use Java 8 features like lambdas, streams, Optional, and CompletableFuture. From Java 17,
I know records, sealed classes, text blocks, switch expressions, and pattern matching for
instanceof. From Java 21, I would highlight virtual threads, sequenced collections, and
pattern matching improvements. For Java 25, I track it as a newer LTS and separate stable
features from preview/incubator APIs before recommending production use.
```

If asked:

> Would you upgrade from Java 8 to Java 21 or 25?

Answer:

```text
I would evaluate dependency compatibility, build tooling, container base images, framework
version, GC behavior, performance, and test coverage. The upgrade can bring better APIs,
records, virtual threads, modern GC, and security updates, but it should be done with
compatibility testing and observability.
```

---

## 27. Mini Program: Modern Domain Modeling

```java
sealed interface BookingEvent permits BookingCreated, BookingCancelled, PaymentCompleted {
}

record BookingCreated(String bookingId, String userId) implements BookingEvent {
}

record BookingCancelled(String bookingId, String reason) implements BookingEvent {
}

record PaymentCompleted(String bookingId, int amount) implements BookingEvent {
}

class EventHandler {
    String describe(BookingEvent event) {
        return switch (event) {
            case BookingCreated created -> "Created " + created.bookingId();
            case BookingCancelled cancelled -> "Cancelled " + cancelled.bookingId();
            case PaymentCompleted paid -> "Paid " + paid.amount();
        };
    }
}
```

Why this is strong:

```text
Records model immutable event data. Sealed interface controls valid event types. Pattern
matching switch gives clear exhaustive handling.
```

---

## 28. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Saying `var` is dynamic typing | Java remains statically typed | Say compiler infers local type |
| Using records for all entities | JPA/proxies/mutation issues | Use records mainly for DTOs/value carriers |
| Claiming virtual threads speed up CPU | CPU still limited by cores | Use for blocking IO scalability |
| Ignoring DB pool with virtual threads | Bottleneck moves to pool | Tune downstream limits |
| Treating preview as final | API can change | State preview clearly |
| Using newest syntax in Java 8 project | Won't compile | Match project JDK |
| Overusing streams | Can hurt readability | Use loops when clearer |
| Overusing pattern matching for simple code | Can feel clever | Prefer clarity |

---

## 29. Rapid Revision

Must-say lines:

```text
Java 8 made Java functional with lambdas, streams, Optional, and functional interfaces.
```

```text
Java 17 LTS is important for records, sealed classes, text blocks, and pattern matching.
```

```text
Java 21 LTS is important for virtual threads and sequenced collections.
```

```text
Java 25 is a newer LTS; discuss it with stable-vs-preview caution.
```

```text
Virtual threads help blocking IO scalability, not CPU speed.
```

```text
Records are great for immutable data carriers, not automatically for JPA entities.
```

---

## 30. Official Source Notes

Use official sources when refreshing:

- OpenJDK JDK 17: `https://openjdk.org/projects/jdk/17/`
- OpenJDK JDK 21: `https://openjdk.org/projects/jdk/21/`
- OpenJDK JDK 25: `https://openjdk.org/projects/jdk/25/`
- OpenJDK JDK 26: `https://openjdk.org/projects/jdk/26/`
- OpenJDK JDK 27: `https://openjdk.org/projects/jdk/27/`
- GA and early-access builds: `https://jdk.java.net/`

Final safety answer:

```text
For modern Java, I speak confidently about stable LTS features and carefully label preview,
incubator, experimental, or early-access features. That keeps the answer accurate and senior.
```
