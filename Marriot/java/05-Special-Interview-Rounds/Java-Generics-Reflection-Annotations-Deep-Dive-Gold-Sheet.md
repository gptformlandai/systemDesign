# Java Generics, Reflection, And Annotations Deep Dive Gold Sheet

Target: Java interviews where the interviewer moves beyond basic syntax into framework-level understanding.

This sheet covers:
- Generics
- Type erasure
- Wildcards
- PECS
- Bounded type parameters
- Generic methods
- Reflection
- Annotations
- Runtime retention
- Dynamic proxies
- Spring-style usage

---

## 1. Mental Model

Generics help at compile time.

Reflection helps inspect and use code at runtime.

Annotations attach metadata to code.

Frameworks combine all three:

```text
Generics
    -> type-safe APIs

Annotations
    -> metadata like @Service, @Transactional, @Autowired

Reflection / proxies
    -> runtime scanning, object creation, dependency injection, AOP
```

Strong interview line:

```text
Generics give compile-time type safety, annotations provide metadata, and reflection lets
frameworks inspect and act on classes at runtime.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Type erasure | Very high | Core generics trap |
| Wildcards | Very high | API design |
| PECS | Very high | Producer/consumer collections |
| Bounded generics | High | Reusable type-safe code |
| Generic methods | High | Common utility pattern |
| Raw types | High | Legacy trap |
| Reflection basics | High | Framework awareness |
| Annotation retention | High | Runtime scanning |
| Dynamic proxy | Medium-high | Spring AOP/JDK proxy |
| Reflection downsides | High | Senior judgment |

---

## 3. Why Generics Exist

Before generics:

```java
List list = new ArrayList();
list.add("java");

String value = (String) list.get(0);
```

Problems:

- Manual casts.
- Runtime ClassCastException.
- Less readable APIs.

With generics:

```java
List<String> list = new ArrayList<>();
list.add("java");

String value = list.get(0);
```

Strong answer:

```text
Generics provide compile-time type safety and reduce casts. They make APIs clearer by
expressing what type a collection or class works with.
```

---

## 4. Type Erasure

Java generics are implemented mostly through type erasure.

Meaning:

```text
Generic type information is checked at compile time and mostly removed at runtime.
```

Example:

```java
List<String> names = new ArrayList<>();
List<Integer> numbers = new ArrayList<>();

System.out.println(names.getClass() == numbers.getClass()); // true
```

Why:

```text
Both are ArrayList at runtime.
```

Interview answer:

```text
Type erasure means Java uses generics for compile-time safety, but runtime usually sees
raw classes like List and ArrayList. This preserves backward compatibility with pre-generics Java.
```

---

## 5. Type Erasure Consequences

You cannot do:

```java
if (list instanceof List<String>) {
}
```

You cannot do:

```java
new T();
```

You cannot create generic arrays safely:

```java
T[] array = new T[10];
```

You cannot overload only by generic type:

```java
void process(List<String> values) {}
void process(List<Integer> values) {} // compilation error after erasure
```

Trap:

```text
At runtime, List<String> and List<Integer> erase to the same raw List type.
```

---

## 6. Raw Types

Raw type:

```java
List list = new ArrayList();
```

Problem:

```java
List<String> names = new ArrayList<>();
List raw = names;
raw.add(100);

String value = names.get(0); // ClassCastException later
```

Strong answer:

```text
Raw types bypass generic type safety and can introduce runtime ClassCastException. Avoid
raw types except when interacting with old legacy APIs.
```

---

## 7. Unbounded Wildcard

`List<?>` means list of unknown type.

Example:

```java
void printAll(List<?> values) {
    for (Object value : values) {
        System.out.println(value);
    }
}
```

You can read as Object.

You generally cannot add non-null values:

```java
values.add("x"); // not allowed
```

Why:

```text
The actual list could be List<Integer>, List<String>, or any other type.
```

---

## 8. Upper Bounded Wildcard

`? extends Number`

Meaning:

```text
Some unknown subtype of Number.
```

Example:

```java
double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number number : numbers) {
        total += number.doubleValue();
    }
    return total;
}
```

Can pass:

```java
List<Integer>
List<Double>
List<Long>
```

Can read as Number.

Cannot safely add Integer/Double because actual list type is unknown.

Trap:

```text
extends is good for reading/producing values.
```

---

## 9. Lower Bounded Wildcard

`? super Integer`

Meaning:

```text
Some unknown supertype of Integer.
```

Example:

```java
void addNumbers(List<? super Integer> values) {
    values.add(1);
    values.add(2);
}
```

Can pass:

```java
List<Integer>
List<Number>
List<Object>
```

Can add Integer.

Reading returns Object safely.

Trap:

```text
super is good for consuming values.
```

---

## 10. PECS Rule

PECS:

```text
Producer Extends, Consumer Super.
```

If a collection produces values for you to read:

```java
List<? extends Number>
```

If a collection consumes values you add:

```java
List<? super Integer>
```

Strong answer:

```text
Use extends when the collection is a producer and I mainly read from it. Use super when
the collection is a consumer and I mainly write into it.
```

---

## 11. PECS Example: Copy

Code:

```java
static <T> void copy(List<? extends T> source, List<? super T> destination) {
    for (T item : source) {
        destination.add(item);
    }
}
```

Explanation:

```text
source produces T, so extends. destination consumes T, so super.
```

Interview line:

```text
This is the cleanest PECS example.
```

---

## 12. Bounded Type Parameter

Example:

```java
class Box<T extends Number> {
    private final T value;

    Box(T value) {
        this.value = value;
    }

    double doubleValue() {
        return value.doubleValue();
    }
}
```

Meaning:

```text
T must be Number or a subtype of Number.
```

Use when:

- Generic class needs methods from a parent type.
- You want type safety plus constraints.

---

## 13. Multiple Bounds

Example:

```java
class Repository<T extends Entity & Auditable> {
    void save(T entity) {
        entity.validate();
        entity.audit();
    }
}
```

Rule:

```text
Class bound must come first, then interfaces.
```

Example:

```java
T extends SomeClass & InterfaceA & InterfaceB
```

---

## 14. Generic Method

Example:

```java
static <T> T first(List<T> values) {
    if (values.isEmpty()) {
        throw new IllegalArgumentException("empty list");
    }
    return values.get(0);
}
```

Usage:

```java
String name = first(List.of("A", "B"));
Integer number = first(List.of(1, 2));
```

Interview line:

```text
Generic methods allow type-safe reusable behavior without making the whole class generic.
```

---

## 15. Generic Class vs Generic Method

| Generic Class | Generic Method |
|---|---|
| Type belongs to object/class | Type belongs to one method |
| Useful when class stores T | Useful for utility behavior |
| `class Box<T>` | `<T> T first(List<T>)` |

Rule:

```text
If only one method needs the type parameter, prefer a generic method.
```

---

## 16. Reflection Basics

Reflection lets code inspect classes, fields, methods, constructors, and annotations at runtime.

Example:

```java
Class<?> clazz = Class.forName("com.example.User");

for (Method method : clazz.getDeclaredMethods()) {
    System.out.println(method.getName());
}
```

Use cases:

- Framework scanning
- Dependency injection
- Serialization/deserialization
- ORM mapping
- Testing tools
- Annotation processing

Strong answer:

```text
Reflection allows runtime inspection and invocation. Frameworks use it to create objects,
read annotations, inject dependencies, and map data.
```

---

## 17. Create Object With Reflection

Example:

```java
Constructor<User> constructor = User.class.getDeclaredConstructor(String.class);
User user = constructor.newInstance("u1");
```

If private constructor:

```java
constructor.setAccessible(true);
```

Caution:

```text
setAccessible bypasses normal access control and may be restricted by modules/security policies.
```

---

## 18. Invoke Method With Reflection

Example:

```java
Method method = User.class.getDeclaredMethod("name");
String name = (String) method.invoke(user);
```

Downsides:

- Slower than direct calls.
- Less compile-time safety.
- Exceptions move to runtime.
- Can break encapsulation.
- Harder with modules/native image.

Interview line:

```text
Reflection is powerful for frameworks but should be used carefully in application code.
```

---

## 19. Annotation Basics

Annotation:

```java
@interface Audited {
}
```

Usage:

```java
@Audited
class PaymentService {
}
```

Annotations are metadata.

They do nothing by themselves.

Something must read them:

- Compiler
- Annotation processor
- Runtime reflection
- Framework

Trap:

```text
Annotations are markers/instructions; behavior comes from code that processes them.
```

---

## 20. Annotation Retention

Retention controls how long annotation metadata is kept.

| Retention | Meaning |
|---|---|
| SOURCE | Available only in source code |
| CLASS | Stored in class file, not necessarily runtime-visible |
| RUNTIME | Available through reflection at runtime |

Example:

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface Audited {
}
```

Interview line:

```text
If a framework must read an annotation at runtime using reflection, retention must be RUNTIME.
```

---

## 21. Annotation Target

Target controls where annotation can be used.

Example:

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@interface Audited {
}
```

Common targets:

- TYPE
- METHOD
- FIELD
- PARAMETER
- CONSTRUCTOR

---

## 22. Annotation With Values

Example:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Retryable {
    int attempts() default 3;
    long delayMillis() default 100;
}
```

Usage:

```java
@Retryable(attempts = 5, delayMillis = 200)
void callRemoteService() {
}
```

Reading:

```java
Retryable retryable = method.getAnnotation(Retryable.class);
int attempts = retryable.attempts();
```

---

## 23. Dynamic Proxy

JDK dynamic proxy works with interfaces.

Example:

```java
interface PaymentService {
    void pay();
}

PaymentService proxy = (PaymentService) Proxy.newProxyInstance(
    PaymentService.class.getClassLoader(),
    new Class<?>[] {PaymentService.class},
    (obj, method, args) -> {
        System.out.println("before");
        Object result = method.invoke(new RealPaymentService(), args);
        System.out.println("after");
        return result;
    }
);
```

Use cases:

- Logging
- Transactions
- Security
- Metrics
- Retry

Spring mapping:

```text
Spring AOP often uses proxies to add behavior around method calls, such as @Transactional.
```

---

## 24. JDK Proxy vs CGLIB Proxy

| JDK Dynamic Proxy | CGLIB-Style Proxy |
|---|---|
| Interface-based | Class subclass-based |
| Built into JDK | Uses bytecode generation library/framework support |
| Proxy implements interface | Proxy extends concrete class |
| Cannot proxy final classes/methods via subclassing | Final restrictions matter |

Interview line:

```text
JDK proxies need interfaces. Class-based proxies subclass concrete classes, so final classes
or final methods can be a problem.
```

---

## 25. Spring-Style Example

Annotation:

```java
@Service
class BookingService {
    @Transactional
    void bookRoom() {
    }
}
```

What conceptually happens:

```text
1. Spring scans classes.
2. It finds annotations.
3. It creates bean definitions.
4. It creates objects.
5. It may create proxies.
6. Calls through proxy can apply transaction logic.
```

Trap:

```text
Self-invocation may bypass proxy behavior because the call does not go through the proxy.
```

---

## 26. Reflection And Native Image Caution

GraalVM Native Image uses static analysis.

Problem:

```text
Reflection, dynamic proxies, resources, and dynamic class loading may need metadata because
the native-image builder must know reachable code at build time.
```

Strong answer:

```text
Reflection-heavy frameworks need native-image support or reachability metadata when building
GraalVM native executables.
```

---

## 27. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Using raw List | Loses type safety | Use `List<T>` |
| Thinking generics exist fully at runtime | Type erasure removes most info | Know erasure |
| Adding to `List<? extends Number>` | Actual subtype unknown | Read only |
| Reading precise type from `List<? super Integer>` | Only Object is safe | Use for writes |
| Forgetting RUNTIME retention | Framework cannot read annotation at runtime | Add `@Retention(RUNTIME)` |
| Thinking annotation creates behavior | It is metadata only | Processor/framework must act |
| Using reflection everywhere | Slow, unsafe, brittle | Prefer direct code unless framework/tooling need |
| Ignoring proxy self-invocation | AOP may not apply | Call through proxy or refactor boundary |

---

## 28. FAANG-Level Question

Question:

> Explain how Spring uses annotations and reflection.

Strong answer:

```text
Annotations like @Service or @Transactional are metadata. Spring scans classes, reads
annotations using reflection and classpath scanning, creates bean definitions, instantiates
objects, injects dependencies, and may wrap beans in proxies. The proxy can apply cross-cutting
behavior such as transactions, security, caching, or metrics around method calls.
```

---

## 29. Rapid Revision

Must-say lines:

```text
Generics give compile-time type safety; type erasure removes most generic info at runtime.
```

```text
PECS means Producer Extends, Consumer Super.
```

```text
Annotations are metadata; processors or frameworks give them behavior.
```

```text
Runtime reflection requires RUNTIME annotation retention.
```

```text
Reflection is powerful but weaker in compile-time safety and performance.
```

```text
JDK dynamic proxies are interface-based; class-based proxies subclass concrete classes.
```
