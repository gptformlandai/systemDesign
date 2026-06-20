# Java 8+ Concepts Interview Prep

Target: Marriott Tech Accelerator / Intervue Java backend round.

This sheet covers Java 8+ concepts that are commonly asked in medium-level Java interviews.

Focus order for your interview:
1. Java 8 features first.
2. Functional programming concepts.
3. Optional, default methods, Date-Time API.
4. CompletableFuture and concurrency basics.
5. Java 17 and Java 21 LTS awareness.
6. Latest GA / preview feature awareness without overclaiming.

---

## 1. Interview Priority Meter

| Concept | Priority | Why It Matters |
|---|---:|---|
| Lambda expressions | Very high | Base of Java 8 functional style |
| Functional interfaces | Very high | Used in streams, callbacks, predicates |
| Method references | High | Clean Java 8 syntax |
| Streams | Very high | Already covered deeply in separate sheet |
| Optional | Very high | Null handling and API design |
| Default/static interface methods | Very high | Common Java 8 theory question |
| Date-Time API | High | Replaced old `Date`/`Calendar` pain |
| CompletableFuture | High | Async programming and backend calls |
| Map enhancements | High | `computeIfAbsent`, `merge`, `getOrDefault` |
| Collectors | High | Grouping, mapping, counting, toMap |
| Parallel streams | Medium | Asked for judgment and pitfalls |
| Base64 | Medium | Practical utility |
| StringJoiner | Low-medium | Sometimes asked with collectors |
| Java 9-11 features | Medium | Modules, factory methods, `var`, HTTP client |
| Java 14-17 features | High | Switch expressions, text blocks, records, sealed classes |
| Java 21 features | High | Virtual threads, pattern matching for switch, sequenced collections |
| Java 25 features | Medium | Latest LTS awareness: compact source files, scoped values, stream gatherers |
| JDK 26 preview awareness | Low-medium | Latest RC/EA awareness only; do not present as common production baseline |

---

## 2. Java 8 Big Picture

### What Changed In Java 8?

Java 8 made Java more functional and expressive.

Main additions:
- Lambda expressions
- Functional interfaces
- Method references
- Streams API
- Optional
- Default and static methods in interfaces
- New Date-Time API
- CompletableFuture
- Map enhancements like `computeIfAbsent`, `merge`, `getOrDefault`
- Base64 API

### Interview Answer

```text
Java 8 introduced functional programming support into Java through lambdas,
functional interfaces, streams, Optional, default methods, a better Date-Time API,
and CompletableFuture for asynchronous programming.
```

---

## 3. Lambda Expressions

### Definition

A lambda expression is a short way to represent an implementation of a functional interface.

### Syntax

```java
(parameters) -> expression
```

or:

```java
(parameters) -> {
    statements;
}
```

### Before Java 8: Anonymous Class

```java
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Task running");
    }
};

new Thread(task).start();
```

### Java 8: Lambda

```java
Runnable task = () -> System.out.println("Task running");

new Thread(task).start();
```

### Example: Sorting Before And After Java 8

Before:

```java
List<String> names = Arrays.asList("Rahul", "Aravind", "Priya");

Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.compareTo(b);
    }
});
```

After:

```java
List<String> names = Arrays.asList("Rahul", "Aravind", "Priya");

names.sort((a, b) -> a.compareTo(b));
```

Cleaner:

```java
names.sort(String::compareTo);
```

### Why Lambdas Exist

They reduce boilerplate when passing behavior as data.

Common use cases:
- Sorting
- Filtering
- Callbacks
- Stream operations
- Thread tasks
- Event handlers

### Important Rule: Effectively Final Variables

Local variables used inside lambdas must be final or effectively final.

Valid:

```java
int threshold = 100;

List<Integer> result = Arrays.asList(50, 120, 200)
    .stream()
    .filter(num -> num > threshold)
    .collect(Collectors.toList());
```

Invalid:

```java
int threshold = 100;

threshold = 150;

List<Integer> result = Arrays.asList(50, 120, 200)
    .stream()
    .filter(num -> num > threshold)
    .collect(Collectors.toList());
```

Why invalid?

```text
The local variable threshold is modified after assignment, so it is not effectively final.
```

### Common Interview Question

Can lambda access instance variables?

Yes.

```java
class Example {
    private int threshold = 100;

    public List<Integer> filter(List<Integer> numbers) {
        return numbers.stream()
            .filter(num -> num > threshold)
            .collect(Collectors.toList());
    }
}
```

Instance variables can be accessed and modified, but be careful with thread safety.

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| What is lambda? | Short implementation of a functional interface |
| Can lambda exist without functional interface? | No, it needs a target functional interface |
| What is effectively final? | A local variable not declared final but not modified after assignment |
| Lambda vs anonymous class? | Lambda is concise and does not create a separate named class style; `this` behaves differently |
| Can lambda throw checked exception? | Only if functional interface method declares it |

### Lambda `this` Trap

In an anonymous class, `this` refers to the anonymous class object.

In a lambda, `this` refers to the enclosing object.

```java
class LambdaThisExample {
    private String name = "Outer";

    public void test() {
        Runnable runnable = () -> System.out.println(this.name);
        runnable.run();
    }
}
```

Output:

```text
Outer
```

---

## 4. Functional Interfaces

### Definition

A functional interface is an interface with exactly one abstract method.

It can have:
- One abstract method
- Any number of default methods
- Any number of static methods
- Methods inherited from `Object`

### Example

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
}
```

Usage:

```java
Calculator add = (a, b) -> a + b;
Calculator multiply = (a, b) -> a * b;

System.out.println(add.calculate(10, 5));      // 15
System.out.println(multiply.calculate(10, 5)); // 50
```

### Why `@FunctionalInterface`?

It is optional but recommended.

It tells the compiler:

```text
This interface should have only one abstract method.
```

If someone adds another abstract method, compilation fails.

### Built-In Functional Interfaces

| Interface | Method | Input | Output | Use Case |
|---|---|---|---|---|
| `Predicate<T>` | `test(T t)` | T | boolean | Filtering |
| `Function<T,R>` | `apply(T t)` | T | R | Transformation |
| `Consumer<T>` | `accept(T t)` | T | void | Side effect |
| `Supplier<T>` | `get()` | none | T | Object/value creation |
| `BiFunction<T,U,R>` | `apply(T,U)` | T,U | R | Two-input transformation |
| `UnaryOperator<T>` | `apply(T)` | T | T | Same-type transformation |
| `BinaryOperator<T>` | `apply(T,T)` | T,T | T | Same-type combination |

### Predicate Example

```java
Predicate<Integer> isEven = num -> num % 2 == 0;

System.out.println(isEven.test(10)); // true
System.out.println(isEven.test(7));  // false
```

With stream:

```java
List<Integer> evenNumbers = Arrays.asList(1, 2, 3, 4, 5, 6)
    .stream()
    .filter(num -> num % 2 == 0)
    .collect(Collectors.toList());
```

### Function Example

```java
Function<String, Integer> lengthFunction = str -> str.length();

System.out.println(lengthFunction.apply("Java")); // 4
```

With stream:

```java
List<Integer> lengths = Arrays.asList("Java", "Spring", "Kafka")
    .stream()
    .map(String::length)
    .collect(Collectors.toList());
```

### Consumer Example

```java
Consumer<String> printer = value -> System.out.println(value);

printer.accept("Hello");
```

With stream:

```java
Arrays.asList("A", "B", "C")
    .forEach(System.out::println);
```

### Supplier Example

```java
Supplier<String> tokenSupplier = () -> UUID.randomUUID().toString();

System.out.println(tokenSupplier.get());
```

### BiFunction Example

```java
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;

System.out.println(add.apply(10, 20)); // 30
```

### UnaryOperator Example

```java
UnaryOperator<String> trimAndUpper = value -> value.trim().toUpperCase();

System.out.println(trimAndUpper.apply(" java ")); // JAVA
```

### BinaryOperator Example

```java
BinaryOperator<Integer> max = (a, b) -> a > b ? a : b;

System.out.println(max.apply(10, 20)); // 20
```

### Predicate Chaining

```java
Predicate<String> startsWithA = name -> name.startsWith("A");
Predicate<String> lengthGreaterThanFive = name -> name.length() > 5;

Predicate<String> rule = startsWithA.and(lengthGreaterThanFive);

System.out.println(rule.test("Aravind")); // true
System.out.println(rule.test("Anil"));    // false
```

Other methods:

```java
and()
or()
negate()
```

### Function Chaining

```java
Function<String, String> trim = String::trim;
Function<String, String> upper = String::toUpperCase;

Function<String, String> trimThenUpper = trim.andThen(upper);

System.out.println(trimThenUpper.apply(" java ")); // JAVA
```

Difference:

```java
f.andThen(g) // first f, then g
f.compose(g) // first g, then f
```

### Mini Program: Validation Rules With Predicate

```java
import java.util.function.Predicate;

public class UserValidationExample {
    static class User {
        String name;
        int age;
        String email;

        User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }

    public static void main(String[] args) {
        Predicate<User> validAge = user -> user.age >= 18;
        Predicate<User> validEmail = user -> user.email != null && user.email.contains("@");
        Predicate<User> validName = user -> user.name != null && !user.name.trim().isEmpty();

        Predicate<User> validUser = validAge.and(validEmail).and(validName);

        User user = new User("Aravind", 29, "aravind@example.com");

        System.out.println(validUser.test(user)); // true
    }
}
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| What is functional interface? | Interface with exactly one abstract method |
| Why use `@FunctionalInterface`? | Compile-time safety |
| Can it have default methods? | Yes |
| Can it have static methods? | Yes |
| Is `Comparator` functional interface? | Yes, because it has one abstract method `compare` |
| Is `Runnable` functional interface? | Yes, `run()` |
| Difference between Predicate and Function? | Predicate returns boolean; Function returns transformed value |

---

## 5. Method References

### Definition

Method reference is a shorter syntax for a lambda that only calls an existing method.

### Types Of Method References

| Type | Syntax | Example |
|---|---|---|
| Static method | `ClassName::staticMethod` | `Integer::parseInt` |
| Instance method of object | `object::instanceMethod` | `System.out::println` |
| Instance method of arbitrary object | `ClassName::instanceMethod` | `String::toUpperCase` |
| Constructor reference | `ClassName::new` | `ArrayList::new` |

### Static Method Reference

Lambda:

```java
Function<String, Integer> parser = value -> Integer.parseInt(value);
```

Method reference:

```java
Function<String, Integer> parser = Integer::parseInt;
```

### Instance Method Of Existing Object

```java
Consumer<String> printer = System.out::println;

printer.accept("Hello Java 8");
```

### Instance Method Of Arbitrary Object

```java
List<String> upper = Arrays.asList("java", "spring")
    .stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());
```

Equivalent lambda:

```java
.map(str -> str.toUpperCase())
```

### Constructor Reference

```java
Supplier<List<String>> listSupplier = ArrayList::new;

List<String> list = listSupplier.get();
```

### Mini Program: Convert Strings To Integers

```java
import java.util.*;
import java.util.stream.Collectors;

public class MethodReferenceExample {
    public static void main(String[] args) {
        List<String> values = Arrays.asList("10", "20", "30");

        List<Integer> numbers = values.stream()
            .map(Integer::parseInt)
            .collect(Collectors.toList());

        System.out.println(numbers); // [10, 20, 30]
    }
}
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| What is method reference? | Shorthand for lambda calling an existing method |
| Is method reference faster than lambda? | Usually not meaningfully; it is mainly readability |
| When not to use it? | When lambda has extra logic |
| Example of constructor reference? | `Employee::new`, `ArrayList::new` |

---

## 6. Streams Recap

Streams are covered deeply in:

```text
Marriot/Java/Java-Streams-Interview-Prep.md
```

### Most Asked Stream Concepts

| Concept | Must Know |
|---|---|
| Lazy execution | Intermediate operations run only after terminal operation |
| map vs flatMap | One-to-one vs one-to-many flattening |
| filter | Keeps matching elements |
| collect | Converts stream into List, Set, Map, grouped result |
| groupingBy | Group data by key |
| toMap | Convert stream to map; handle duplicate keys |
| reduce | Combine into single result |
| parallelStream | Use carefully; avoid shared mutable state |

### Quick Example

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

### Hot Interview Line

```text
A stream is single-use, lazy, and does not store data.
```

---

## 7. Optional

### Definition

`Optional<T>` is a container object that may or may not contain a non-null value.

It was introduced to reduce direct null handling and make absence explicit.

### Creating Optional

```java
Optional<String> name = Optional.of("Aravind");
```

Use `ofNullable` when value may be null:

```java
String value = null;
Optional<String> optional = Optional.ofNullable(value);
```

Avoid:

```java
Optional.of(value); // Throws NullPointerException if value is null
```

### Basic Usage

```java
Optional<String> name = Optional.ofNullable("Aravind");

name.ifPresent(System.out::println);
```

### `orElse`

```java
String result = Optional.ofNullable(null)
    .orElse("Default");

System.out.println(result); // Default
```

### `orElseGet`

```java
String result = Optional.ofNullable(null)
    .orElseGet(() -> "Default");
```

### `orElseThrow`

```java
String result = Optional.ofNullable(null)
    .orElseThrow(() -> new IllegalArgumentException("Value missing"));
```

### `orElse` vs `orElseGet` Trap

```java
String value = Optional.of("Actual")
    .orElse(getDefaultValue());
```

`getDefaultValue()` is called even though Optional has value.

Better:

```java
String value = Optional.of("Actual")
    .orElseGet(() -> getDefaultValue());
```

`getDefaultValue()` is called only when Optional is empty.

### Optional With Map

```java
Optional<String> upper = Optional.ofNullable("java")
    .map(String::toUpperCase);

System.out.println(upper.orElse("NA")); // JAVA
```

### Optional With Filter

```java
Optional<String> validName = Optional.ofNullable("Aravind")
    .filter(name -> name.length() > 3);
```

### Optional With FlatMap

Use `flatMap` when mapper already returns Optional.

```java
class User {
    private Address address;

    Optional<Address> getAddress() {
        return Optional.ofNullable(address);
    }
}

class Address {
    private String city;

    Optional<String> getCity() {
        return Optional.ofNullable(city);
    }
}
```

```java
Optional<String> city = Optional.ofNullable(user)
    .flatMap(User::getAddress)
    .flatMap(Address::getCity);
```

### Common Wrong Usage

Avoid:

```java
if (optional.isPresent()) {
    System.out.println(optional.get());
}
```

Better:

```java
optional.ifPresent(System.out::println);
```

Or:

```java
String value = optional.orElse("Default");
```

### Should We Use Optional For Fields?

Usually no.

Avoid:

```java
class Employee {
    private Optional<String> name;
}
```

Better:

```java
class Employee {
    private String name;

    public Optional<String> getNameOptional() {
        return Optional.ofNullable(name);
    }
}
```

### Should We Use Optional For Method Parameters?

Usually no.

Avoid:

```java
public void process(Optional<String> name) {
}
```

Better:

```java
public void process(String name) {
}
```

or overload methods if needed.

### Best Use Case

Optional is best as a return type when a value may be absent.

```java
public Optional<Employee> findById(int id) {
    return employees.stream()
        .filter(emp -> emp.getId() == id)
        .findFirst();
}
```

### Mini Program: Optional In Service Method

```java
import java.util.*;

public class OptionalServiceExample {
    static class Employee {
        int id;
        String name;

        Employee(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static Optional<Employee> findById(List<Employee> employees, int id) {
        return employees.stream()
            .filter(emp -> emp.id == id)
            .findFirst();
    }

    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee(1, "Aravind"),
            new Employee(2, "Rahul")
        );

        Employee employee = findById(employees, 3)
            .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        System.out.println(employee.name);
    }
}
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| What is Optional? | Container that represents value present or absent |
| Why Optional? | Avoid null checks and make absence explicit |
| `of` vs `ofNullable`? | `of` rejects null; `ofNullable` allows null |
| `orElse` vs `orElseGet`? | `orElse` evaluates eagerly; `orElseGet` evaluates lazily |
| Should Optional be field? | Usually no |
| Should Optional be parameter? | Usually no |
| Best Optional usage? | Return type for possibly missing value |

---

## 8. Default And Static Methods In Interfaces

### Why Introduced?

Java 8 added default methods mainly to evolve existing interfaces without breaking implementations.

Example:
- Java added methods to collection interfaces.
- Existing classes did not have to implement all new methods immediately.

### Default Method

```java
interface Vehicle {
    void start();

    default void stop() {
        System.out.println("Vehicle stopped");
    }
}
```

Implementation:

```java
class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }
}
```

Usage:

```java
Vehicle car = new Car();
car.start();
car.stop();
```

### Static Method In Interface

```java
interface VehicleUtils {
    static boolean isValidSpeed(int speed) {
        return speed >= 0 && speed <= 200;
    }
}
```

Usage:

```java
boolean valid = VehicleUtils.isValidSpeed(100);
```

### Conflict Case: Two Interfaces With Same Default Method

```java
interface A {
    default void show() {
        System.out.println("A");
    }
}

interface B {
    default void show() {
        System.out.println("B");
    }
}

class C implements A, B {
    @Override
    public void show() {
        A.super.show();
    }
}
```

Why override is required?

```text
The compiler cannot decide whether to use A's show or B's show.
```

### Class Wins Over Interface

```java
class Parent {
    public void show() {
        System.out.println("Parent");
    }
}

interface Printable {
    default void show() {
        System.out.println("Printable");
    }
}

class Child extends Parent implements Printable {
}
```

Calling:

```java
new Child().show();
```

Output:

```text
Parent
```

Rule:

```text
Class method wins over interface default method.
```

### Interface Static Method Is Not Inherited

```java
interface MyInterface {
    static void utility() {
        System.out.println("Utility");
    }
}
```

Call like:

```java
MyInterface.utility();
```

Not like:

```java
new SomeImplementation().utility();
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| Why default methods? | To evolve interfaces without breaking existing classes |
| Can default method be overridden? | Yes |
| Can interface have static method? | Yes |
| Are static interface methods inherited? | No, call using interface name |
| What if two interfaces have same default method? | Implementing class must override |
| What wins: class method or interface default? | Class method wins |

---

## 9. Date-Time API

### Why New Date-Time API?

Old APIs had issues:
- `java.util.Date` was mutable.
- `Calendar` was verbose and confusing.
- Month indexing was error-prone.
- Time zone handling was painful.

Java 8 introduced the `java.time` package.

### Important Classes

| Class | Use |
|---|---|
| `LocalDate` | Date without time |
| `LocalTime` | Time without date |
| `LocalDateTime` | Date and time without timezone |
| `ZonedDateTime` | Date and time with timezone |
| `Instant` | Machine timestamp in UTC |
| `Duration` | Time-based amount |
| `Period` | Date-based amount |
| `DateTimeFormatter` | Format/parse date-time |

### LocalDate Example

```java
LocalDate today = LocalDate.now();
LocalDate interviewDate = LocalDate.of(2026, 4, 27);

System.out.println(today);
System.out.println(interviewDate);
```

### LocalDateTime Example

```java
LocalDateTime now = LocalDateTime.now();

System.out.println(now);
```

### ZonedDateTime Example

```java
ZonedDateTime indiaTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
ZonedDateTime utcTime = ZonedDateTime.now(ZoneId.of("UTC"));

System.out.println(indiaTime);
System.out.println(utcTime);
```

### Instant Example

```java
Instant now = Instant.now();

System.out.println(now);
```

Good for:
- Audit timestamps
- Event time
- Logs
- Database timestamps

### Period vs Duration

`Period` is date-based:

```java
LocalDate start = LocalDate.of(2026, 4, 25);
LocalDate end = LocalDate.of(2026, 4, 27);

Period period = Period.between(start, end);

System.out.println(period.getDays()); // 2
```

`Duration` is time-based:

```java
LocalDateTime start = LocalDateTime.of(2026, 4, 25, 10, 0);
LocalDateTime end = LocalDateTime.of(2026, 4, 25, 12, 30);

Duration duration = Duration.between(start, end);

System.out.println(duration.toMinutes()); // 150
```

### Formatting

```java
LocalDate date = LocalDate.of(2026, 4, 27);

DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

String formatted = date.format(formatter);

System.out.println(formatted); // 27-04-2026
```

### Parsing

```java
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

LocalDate date = LocalDate.parse("27-04-2026", formatter);

System.out.println(date); // 2026-04-27
```

### Mini Program: Hotel Check-In / Check-Out Nights

```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class HotelStayExample {
    public static void main(String[] args) {
        LocalDate checkIn = LocalDate.of(2026, 4, 25);
        LocalDate checkOut = LocalDate.of(2026, 4, 28);

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        System.out.println("Nights: " + nights); // Nights: 3
    }
}
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| Why java.time? | Immutable, clearer, thread-safe date-time API |
| LocalDate vs LocalDateTime? | Date only vs date plus time |
| LocalDateTime vs ZonedDateTime? | Without timezone vs with timezone |
| Instant use? | Machine timestamp, usually UTC |
| Period vs Duration? | Date amount vs time amount |
| Is DateTimeFormatter thread-safe? | Yes, unlike old `SimpleDateFormat` |

---

## 10. CompletableFuture

### Definition

`CompletableFuture` is used for asynchronous, non-blocking-style computation.

It implements:

```java
Future
CompletionStage
```

### Why It Exists

Old `Future` limitations:
- Could not easily chain tasks.
- Could not easily combine multiple async results.
- Error handling was awkward.
- `get()` blocks.

`CompletableFuture` adds:
- Chaining
- Combining
- Exception handling
- Async execution
- Manual completion

### Basic Async Example

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
});

String result = future.join();

System.out.println(result); // Hello
```

### `runAsync` vs `supplyAsync`

| Method | Returns |
|---|---|
| `runAsync` | `CompletableFuture<Void>` |
| `supplyAsync` | `CompletableFuture<T>` |

`runAsync`:

```java
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Sending email");
});
```

`supplyAsync`:

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Booking confirmed";
});
```

### thenApply

Transforms result.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "java")
    .thenApply(String::toUpperCase);

System.out.println(future.join()); // JAVA
```

### thenAccept

Consumes result, returns `Void`.

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Booking confirmed")
    .thenAccept(message -> System.out.println(message));

future.join();
```

### thenRun

Runs next task without using previous result.

```java
CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "Booking confirmed")
    .thenRun(() -> System.out.println("Audit event logged"));
```

### thenCompose

Flattens dependent async calls.

Use when second async call depends on first result.

```java
CompletableFuture<User> userFuture = getUser("u1")
    .thenCompose(user -> getLoyaltyProfile(user.id));
```

Simple complete example:

```java
import java.util.concurrent.CompletableFuture;

public class ThenComposeExample {
    static class User {
        String id;

        User(String id) {
            this.id = id;
        }
    }

    static class LoyaltyProfile {
        String userId;

        LoyaltyProfile(String userId) {
            this.userId = userId;
        }
    }

    static CompletableFuture<User> getUser(String id) {
        return CompletableFuture.supplyAsync(() -> new User(id));
    }

    static CompletableFuture<LoyaltyProfile> getLoyaltyProfile(String userId) {
        return CompletableFuture.supplyAsync(() -> new LoyaltyProfile(userId));
    }

    public static void main(String[] args) {
        CompletableFuture<LoyaltyProfile> future = getUser("u1")
            .thenCompose(user -> getLoyaltyProfile(user.id));

        System.out.println(future.join().userId);
    }
}
```

### thenCombine

Combines two independent async calls.

```java
CompletableFuture<Integer> priceFuture = CompletableFuture.supplyAsync(() -> 1000);
CompletableFuture<Integer> taxFuture = CompletableFuture.supplyAsync(() -> 180);

CompletableFuture<Integer> totalFuture = priceFuture.thenCombine(
    taxFuture,
    (price, tax) -> price + tax
);

System.out.println(totalFuture.join()); // 1180
```

### allOf

Waits for all futures.

```java
CompletableFuture<String> hotel = CompletableFuture.supplyAsync(() -> "Hotel");
CompletableFuture<String> flight = CompletableFuture.supplyAsync(() -> "Flight");
CompletableFuture<String> cab = CompletableFuture.supplyAsync(() -> "Cab");

CompletableFuture<Void> all = CompletableFuture.allOf(hotel, flight, cab);

all.join();

List<String> results = Arrays.asList(
    hotel.join(),
    flight.join(),
    cab.join()
);

System.out.println(results);
```

### anyOf

Completes when any future completes.

```java
CompletableFuture<String> source1 = CompletableFuture.supplyAsync(() -> "Source1");
CompletableFuture<String> source2 = CompletableFuture.supplyAsync(() -> "Source2");

CompletableFuture<Object> fastest = CompletableFuture.anyOf(source1, source2);

System.out.println(fastest.join());
```

### Exception Handling: exceptionally

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Service failed");
}).exceptionally(ex -> "Fallback response");

System.out.println(future.join()); // Fallback response
```

### Exception Handling: handle

`handle` receives both result and exception.

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    if (true) {
        throw new RuntimeException("Failed");
    }
    return "Success";
}).handle((result, ex) -> {
    if (ex != null) {
        return "Fallback";
    }
    return result;
});

System.out.println(future.join()); // Fallback
```

### `get()` vs `join()`

| Method | Exception Style |
|---|---|
| `get()` | Throws checked exceptions |
| `join()` | Throws unchecked `CompletionException` |

### Custom Executor

By default, async methods use common ForkJoinPool.

For backend applications, prefer custom executor for control.

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Result";
}, executor);

System.out.println(future.join());

executor.shutdown();
```

### Mini Program: Parallel Price Aggregation

```java
import java.util.concurrent.*;

public class PriceAggregationExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        CompletableFuture<Integer> roomPrice = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return 5000;
        }, executor);

        CompletableFuture<Integer> tax = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return 900;
        }, executor);

        CompletableFuture<Integer> discount = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return 500;
        }, executor);

        CompletableFuture<Integer> finalPrice = roomPrice
            .thenCombine(tax, Integer::sum)
            .thenCombine(discount, (amount, discountAmount) -> amount - discountAmount);

        System.out.println("Final price: " + finalPrice.join()); // 5400

        executor.shutdown();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }
}
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| Future vs CompletableFuture? | CompletableFuture supports chaining, combining, and better exception handling |
| runAsync vs supplyAsync? | `runAsync` returns no value; `supplyAsync` returns a value |
| thenApply vs thenCompose? | `thenApply` transforms; `thenCompose` flattens dependent async future |
| thenCompose vs thenCombine? | Compose is dependent; combine is independent |
| allOf vs anyOf? | Wait all vs first completed |
| get vs join? | checked vs unchecked exception style |
| Default executor? | Common ForkJoinPool |
| Production caution? | Use custom executor for blocking IO or controlled thread usage |

---

## 11. Map Enhancements In Java 8

Java 8 added useful default methods to `Map`.

These are very common in interviews and production code.

### getOrDefault

```java
Map<String, Integer> count = new HashMap<>();

int javaCount = count.getOrDefault("java", 0);
```

Useful for frequency counting:

```java
Map<String, Integer> frequency = new HashMap<>();

for (String word : Arrays.asList("java", "spring", "java")) {
    frequency.put(word, frequency.getOrDefault(word, 0) + 1);
}

System.out.println(frequency); // {spring=1, java=2}
```

### putIfAbsent

```java
Map<String, List<String>> map = new HashMap<>();

map.putIfAbsent("Engineering", new ArrayList<>());
map.get("Engineering").add("Aravind");
```

### computeIfAbsent

Cleaner:

```java
Map<String, List<String>> namesByDepartment = new HashMap<>();

namesByDepartment
    .computeIfAbsent("Engineering", key -> new ArrayList<>())
    .add("Aravind");
```

### computeIfPresent

```java
Map<String, Integer> stock = new HashMap<>();
stock.put("room-101", 5);

stock.computeIfPresent("room-101", (key, value) -> value - 1);

System.out.println(stock.get("room-101")); // 4
```

### compute

```java
Map<String, Integer> frequency = new HashMap<>();

frequency.compute("java", (key, oldValue) -> oldValue == null ? 1 : oldValue + 1);
```

### merge

Best for counting:

```java
Map<String, Integer> frequency = new HashMap<>();

for (String word : Arrays.asList("java", "spring", "java")) {
    frequency.merge(word, 1, Integer::sum);
}

System.out.println(frequency); // {spring=1, java=2}
```

### forEach

```java
Map<String, Integer> frequency = new HashMap<>();
frequency.put("java", 2);
frequency.put("spring", 1);

frequency.forEach((key, value) -> System.out.println(key + " -> " + value));
```

### replaceAll

```java
Map<String, Integer> prices = new HashMap<>();
prices.put("room", 5000);
prices.put("tax", 900);

prices.replaceAll((key, value) -> value + 100);

System.out.println(prices);
```

### Hot Interview Points

| Method | Use |
|---|---|
| `getOrDefault` | Read with default |
| `putIfAbsent` | Put only if key missing |
| `computeIfAbsent` | Create value lazily if key missing |
| `computeIfPresent` | Update only existing key |
| `compute` | General remapping |
| `merge` | Combine old and new value |
| `forEach` | Iterate map |
| `replaceAll` | Update all values |

### Common Interview Question

Frequency count using `merge`:

```java
public class FrequencyWithMerge {
    public static void main(String[] args) {
        List<String> words = Arrays.asList("java", "spring", "java", "kafka");
        Map<String, Integer> frequency = new HashMap<>();

        words.forEach(word -> frequency.merge(word, 1, Integer::sum));

        System.out.println(frequency);
    }
}
```

Frequency count using `compute`:

```java
words.forEach(word ->
    frequency.compute(word, (key, oldValue) -> oldValue == null ? 1 : oldValue + 1)
);
```

Interview answer:

```text
For simple counting, merge is cleaner. compute is more flexible when the update logic
needs both key and old value.
```

---

## 12. Collectors

Collectors are heavily used with streams.

### toList

```java
List<String> names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### toSet

```java
Set<String> departments = employees.stream()
    .map(Employee::getDepartment)
    .collect(Collectors.toSet());
```

### joining

```java
String names = employees.stream()
    .map(Employee::getName)
    .collect(Collectors.joining(", "));
```

### counting

```java
long count = employees.stream()
    .collect(Collectors.counting());
```

Usually simpler:

```java
long count = employees.stream().count();
```

### groupingBy

```java
Map<String, List<Employee>> byDepartment = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment));
```

### groupingBy + counting

```java
Map<String, Long> countByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

### mapping

```java
Map<String, List<String>> namesByDepartment = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));
```

### averagingInt

```java
Map<String, Double> avgSalary = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.averagingInt(Employee::getSalary)
    ));
```

### toMap With Merge Function

```java
Map<String, Employee> highestPaidByDepartment = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDepartment,
        Function.identity(),
        (e1, e2) -> e1.getSalary() >= e2.getSalary() ? e1 : e2
    ));
```

### Hot Interview Points

| Question | Strong Answer |
|---|---|
| What is collector? | A terminal operation helper that accumulates stream elements |
| groupingBy use? | Groups elements by key |
| mapping collector use? | Transforms elements inside grouping |
| toMap duplicate key issue? | Throws exception unless merge function is provided |
| joining use? | Concatenate strings |

---

## 13. Parallel Streams

### Definition

Parallel stream splits stream processing across multiple threads using the common ForkJoinPool.

```java
long count = employees.parallelStream()
    .filter(emp -> emp.getSalary() > 100000)
    .count();
```

### When It Can Help

Good fit:
- Large in-memory data
- CPU-heavy operations
- Independent processing
- No shared mutable state

### When Not To Use

Avoid for:
- Small collections
- Blocking DB calls
- Blocking HTTP calls
- Shared mutable state
- Order-sensitive processing
- Code running in application server where common pool contention matters

### Bad Example

```java
List<String> names = new ArrayList<>();

employees.parallelStream()
    .forEach(emp -> names.add(emp.getName()));
```

Problem:

```text
ArrayList is not thread-safe.
```

Correct:

```java
List<String> names = employees.parallelStream()
    .map(Employee::getName)
    .collect(Collectors.toList());
```

### Hot Interview Answer

```text
Parallel streams are not automatically faster. They are useful for large, CPU-bound,
independent tasks. For blocking IO or shared mutable state, I avoid them and use a proper
executor or async model.
```

---

## 14. Base64 API

### Why It Matters

Java 8 introduced standard Base64 support.

Useful for:
- Encoding tokens
- Basic auth headers
- Binary-to-text conversion

### Encode

```java
String input = "username:password";

String encoded = Base64.getEncoder()
    .encodeToString(input.getBytes(StandardCharsets.UTF_8));

System.out.println(encoded);
```

### Decode

```java
byte[] decodedBytes = Base64.getDecoder().decode(encoded);

String decoded = new String(decodedBytes, StandardCharsets.UTF_8);

System.out.println(decoded);
```

### Complete Program

```java
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Example {
    public static void main(String[] args) {
        String input = "aravind:secret";

        String encoded = Base64.getEncoder()
            .encodeToString(input.getBytes(StandardCharsets.UTF_8));

        String decoded = new String(
            Base64.getDecoder().decode(encoded),
            StandardCharsets.UTF_8
        );

        System.out.println(encoded);
        System.out.println(decoded);
    }
}
```

### Interview Point

```text
Base64 is encoding, not encryption.
```

---

## 15. StringJoiner

### Definition

`StringJoiner` helps join strings with delimiter, prefix, and suffix.

### Example

```java
StringJoiner joiner = new StringJoiner(", ", "[", "]");

joiner.add("Java");
joiner.add("Spring");
joiner.add("Kafka");

System.out.println(joiner.toString()); // [Java, Spring, Kafka]
```

### Similar Stream Version

```java
String result = Arrays.asList("Java", "Spring", "Kafka")
    .stream()
    .collect(Collectors.joining(", ", "[", "]"));

System.out.println(result); // [Java, Spring, Kafka]
```

### Interview Point

```text
StringJoiner is useful for delimiter-based string construction. In streams, Collectors.joining
uses a similar idea.
```

---

## 16. Repeatable Annotations And Type Annotations

These are lower priority, but sometimes asked as "what else came in Java 8?"

### Repeatable Annotations

Before Java 8, applying same annotation multiple times required a container annotation.

Java 8 added `@Repeatable`.

```java
import java.lang.annotation.*;

@Repeatable(Roles.class)
@interface Role {
    String value();
}

@interface Roles {
    Role[] value();
}

@Role("ADMIN")
@Role("USER")
class AccountService {
}
```

### Type Annotations

Java 8 allowed annotations in more type-use locations.

Example:

```java
List<@NonNull String> names;
```

This is mostly useful with static analysis tools.

### Interview Priority

Low.

Know the names, but do not spend too much preparation time here.

---

## 17. Metaspace Instead Of PermGen

### What Changed?

Java 8 removed PermGen and introduced Metaspace.

### Before Java 8

Class metadata was stored in PermGen, which had fixed JVM memory limits.

### Java 8+

Class metadata is stored in Metaspace, which uses native memory by default.

### Interview Answer

```text
Java 8 removed PermGen and replaced it with Metaspace. Metaspace stores class metadata
in native memory and can grow dynamically, though it can still be limited using JVM flags.
```

### Hot Points

| Topic | Answer |
|---|---|
| PermGen removed in? | Java 8 |
| Replacement? | Metaspace |
| Stores what? | Class metadata |
| Memory area? | Native memory |
| Can it still OOM? | Yes, if class metadata grows too much |

---

## 18. Java 9 To Java 25+ Modern Java Awareness

This section is for "Java 8 plus" awareness.

For this Marriott / Intervue round:
- Java 8 concepts are still the highest priority.
- Java 17 and Java 21 are the most valuable modern Java LTS topics.
- Java 25 is useful as latest LTS awareness.
- JDK 26 is worth knowing only as release-candidate / early-access awareness, not as a normal production baseline.

### Version Priority Table

| Version | Interview Priority | Must Know Features |
|---|---:|---|
| Java 9 | Medium | Modules, collection factory methods, private interface methods |
| Java 10 | Medium | `var` local variable type inference |
| Java 11 | High | String methods, HTTP Client, LTS status |
| Java 14 | Medium | Switch expressions |
| Java 15 | Medium | Text blocks |
| Java 16 | High | Records, pattern matching for `instanceof` |
| Java 17 | Very high | LTS, sealed classes, strong encapsulation |
| Java 21 | Very high | LTS, virtual threads, sequenced collections, record patterns, pattern matching for switch |
| Java 22-24 | Medium | Unnamed variables, FFM API, stream gatherers, class-file API |
| Java 25 | Medium-high | Latest LTS awareness, scoped values, compact source files, module imports |
| JDK 26 | Low-medium | RC/EA awareness: HTTP/3, structured concurrency preview |

### What To Say In Interview

```text
I am strongest with Java 8 production features like streams, lambdas, Optional, and
CompletableFuture. I also keep track of modern Java, especially Java 17 and Java 21 LTS
features such as records, sealed classes, pattern matching, virtual threads, and sequenced
collections. For very new features like Java 25 or JDK 26, I treat preview features carefully
and do not assume they are production baseline unless the project uses that JDK.
```

---

## 18.1 Java 9 Features

### Collection Factory Methods

Java 9 introduced convenient factory methods for small immutable collections.

```java
List<String> names = List.of("Java", "Spring", "Kafka");
Set<String> skills = Set.of("Java", "AWS");
Map<Integer, String> idToName = Map.of(
    1, "Aravind",
    2, "Rahul"
);
```

Important:

```text
These collections are immutable and do not allow null elements.
```

This throws `UnsupportedOperationException`:

```java
names.add("Docker");
```

This throws `NullPointerException`:

```java
List.of("Java", null);
```

### Map.ofEntries For Larger Maps

`Map.of` supports limited key-value pairs. For larger maps, use `Map.ofEntries`.

```java
Map<Integer, String> status = Map.ofEntries(
    Map.entry(100, "Continue"),
    Map.entry(200, "OK"),
    Map.entry(201, "Created"),
    Map.entry(400, "Bad Request"),
    Map.entry(404, "Not Found"),
    Map.entry(500, "Server Error")
);
```

### Private Methods In Interfaces

Java 8 allowed default methods.

Java 9 allowed private helper methods inside interfaces.

```java
interface Logger {
    default void info(String message) {
        log("INFO", message);
    }

    default void error(String message) {
        log("ERROR", message);
    }

    private void log(String level, String message) {
        System.out.println(level + ": " + message);
    }
}
```

Why?

```text
To share common code between default methods without exposing it publicly.
```

### Optional Improvements

```java
optional.ifPresentOrElse(
    value -> System.out.println(value),
    () -> System.out.println("Value missing")
);
```

```java
Stream<String> stream = optional.stream();
```

Useful stream pattern:

```java
List<Optional<String>> optionals = Arrays.asList(
    Optional.of("Java"),
    Optional.empty(),
    Optional.of("Spring")
);

List<String> values = optionals.stream()
    .flatMap(Optional::stream)
    .collect(Collectors.toList());
```

Output:

```text
[Java, Spring]
```

### Stream Improvements

`takeWhile`:

```java
List<Integer> result = Stream.of(1, 2, 3, 4, 1, 2)
    .takeWhile(num -> num < 4)
    .collect(Collectors.toList());
```

Output:

```text
[1, 2, 3]
```

`dropWhile`:

```java
List<Integer> result = Stream.of(1, 2, 3, 4, 1, 2)
    .dropWhile(num -> num < 4)
    .collect(Collectors.toList());
```

Output:

```text
[4, 1, 2]
```

Important:

```text
takeWhile and dropWhile are order-sensitive. They stop based on the first element that
breaks the condition, not all matching elements like filter.
```

### Module System

Java 9 introduced the Java Platform Module System.

Example `module-info.java`:

```java
module com.example.booking {
    requires java.sql;
    exports com.example.booking.api;
}
```

Interview answer:

```text
The module system helps create stronger boundaries between packages and explicit dependencies.
It is especially useful for large applications, libraries, and JDK internals encapsulation.
```

### JShell

JShell is an interactive Java REPL.

Useful for:
- Trying small Java snippets.
- Testing API behavior quickly.
- Learning language features.

### Java 9 Hot Interview Points

| Question | Strong Answer |
|---|---|
| Are `List.of` collections mutable? | No, they are immutable |
| Can `List.of` contain null? | No, it throws `NullPointerException` |
| Why private interface methods? | To share code between default methods |
| `takeWhile` vs `filter`? | `takeWhile` stops at first failure; `filter` checks all elements |
| What is module system? | Explicit module dependencies and stronger encapsulation |

---

## 18.2 Java 10 Feature

### `var` Local Variable Type Inference

```java
var name = "Aravind";
var count = 10;
var list = new ArrayList<String>();
```

Important:

```text
var is local variable type inference. It is not dynamic typing.
```

The compiler still knows exact types:

```java
String name = "Aravind";
```

and:

```java
var name = "Aravind";
```

both produce a statically typed local variable.

### Invalid `var` Examples

Cannot infer from null:

```java
var value = null;
```

Cannot use for fields:

```java
class User {
    var name = "Aravind";
}
```

Cannot use for method return type:

```java
public var getName() {
    return "Aravind";
}
```

### Good Use

```java
var employeesByDepartment = new HashMap<String, List<String>>();
```

### Bad Use

```java
var data = getData();
```

If `getData()` is not clear, readability suffers.

### Interview Answer

```text
var improves readability when the right-hand side makes the type obvious. It is still
compile-time static typing, not JavaScript-style dynamic typing.
```

---

## 18.3 Java 11 Features

Java 11 is an LTS release and is still common in many enterprise backend systems.

### String Methods

```java
" ".isBlank();                 // true
" Java ".strip();              // "Java"
"Java\nSpring".lines();        // Stream<String>
"ha".repeat(3);                // hahaha
```

### `isBlank` vs `isEmpty`

```java
"".isEmpty();      // true
" ".isEmpty();     // false
" ".isBlank();     // true
"\t\n".isBlank();  // true
```

Interview answer:

```text
isEmpty checks length zero. isBlank checks whether the string is empty or only whitespace.
```

### `strip` vs `trim`

```java
String value = " Java ";

System.out.println(value.trim());
System.out.println(value.strip());
```

Interview answer:

```text
trim is older and removes characters less than or equal to U+0020. strip is Unicode-aware
and preferred for modern whitespace handling.
```

### HTTP Client

Java 11 standardized the new HTTP Client.

```java
HttpClient client = HttpClient.newHttpClient();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://example.com"))
    .GET()
    .build();

HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
);

System.out.println(response.body());
```

### Async HTTP Call

```java
HttpClient client = HttpClient.newHttpClient();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://example.com"))
    .GET()
    .build();

CompletableFuture<HttpResponse<String>> future = client.sendAsync(
    request,
    HttpResponse.BodyHandlers.ofString()
);

future.thenApply(HttpResponse::body)
    .thenAccept(System.out::println)
    .join();
```

### Local-Variable Syntax For Lambda Parameters

```java
BiFunction<Integer, Integer, Integer> add = (var a, var b) -> a + b;
```

Why useful?

```text
It allows annotations on lambda parameters while keeping inferred types.
```

### Java 11 Hot Interview Points

| Question | Strong Answer |
|---|---|
| Java 11 LTS? | Yes |
| `isBlank` vs `isEmpty`? | Whitespace-aware vs length check |
| `strip` vs `trim`? | Unicode-aware vs older trimming |
| HTTP Client supports async? | Yes, via `sendAsync` and CompletableFuture |

---

## 18.4 Java 12 To Java 15 Features

These are useful because many later LTS features passed through these versions.

### Java 14: Switch Expressions

Old switch statement:

```java
String type = "GOLD";
int discount;

switch (type) {
    case "GOLD":
        discount = 20;
        break;
    case "SILVER":
        discount = 10;
        break;
    default:
        discount = 0;
}
```

Switch expression:

```java
String type = "GOLD";

int discount = switch (type) {
    case "GOLD" -> 20;
    case "SILVER" -> 10;
    default -> 0;
};
```

### Switch With `yield`

```java
int discount = switch (type) {
    case "GOLD" -> {
        System.out.println("Premium customer");
        yield 20;
    }
    case "SILVER" -> 10;
    default -> 0;
};
```

Interview answer:

```text
Switch expressions reduce boilerplate and avoid accidental fall-through. They can return
a value and use arrow labels or yield for block cases.
```

### Java 15: Text Blocks

```java
String json = """
    {
      "name": "Aravind",
      "role": "Developer"
    }
    """;
```

Useful for:
- JSON
- SQL
- HTML
- Multi-line logs
- Test payloads

SQL example:

```java
String sql = """
    SELECT id, name, status
    FROM booking
    WHERE status = ?
    ORDER BY created_at DESC
    """;
```

Interview answer:

```text
Text blocks make multi-line strings readable without heavy escaping and concatenation.
They are useful for JSON, SQL, and test data.
```

---

## 18.5 Java 16 And Java 17 LTS Features

Java 17 is an LTS release and very important for backend interviews.

### Pattern Matching For `instanceof`

Old:

```java
if (obj instanceof String) {
    String value = (String) obj;
    System.out.println(value.toUpperCase());
}
```

Modern:

```java
if (obj instanceof String value) {
    System.out.println(value.toUpperCase());
}
```

Why?

```text
It removes boilerplate casting after instanceof checks.
```

### Records

Records reduce boilerplate for immutable data carriers.

```java
record EmployeeDto(int id, String name, String department) {
}
```

The compiler generates:
- Constructor
- Accessors like `id()`, `name()`
- `equals`
- `hashCode`
- `toString`

Usage:

```java
EmployeeDto dto = new EmployeeDto(1, "Aravind", "Engineering");

System.out.println(dto.name());
System.out.println(dto);
```

### Record With Compact Constructor

```java
record BookingRequest(String hotelId, LocalDate checkIn, LocalDate checkOut) {
    BookingRequest {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId is required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkout must be after checkin");
        }
    }
}
```

### When To Use Records

Good for:
- DTOs
- API responses
- Request objects
- Immutable projections
- Composite map keys

Avoid for:
- JPA entities
- Mutable domain objects
- Classes with complex identity/lifecycle

### Sealed Classes

Sealed classes restrict which classes can extend or implement them.

```java
sealed interface Payment permits CardPayment, UpiPayment, WalletPayment {
}

final class CardPayment implements Payment {
}

final class UpiPayment implements Payment {
}

final class WalletPayment implements Payment {
}
```

Permitted subclasses must be one of:
- `final`
- `sealed`
- `non-sealed`

Example:

```java
sealed class BookingEvent permits BookingCreated, BookingCancelled {
}

final class BookingCreated extends BookingEvent {
}

final class BookingCancelled extends BookingEvent {
}
```

### Sealed Class Interview Answer

```text
Sealed classes let us model a fixed set of allowed subtypes. They are useful when the domain
has known variants, like payment methods or booking events, and we want controlled inheritance.
```

### Strong Encapsulation Of JDK Internals

Java 17 strongly encapsulated JDK internals.

Interview-safe answer:

```text
Modern Java discourages depending on internal JDK APIs like sun.misc classes. Code should use
standard supported APIs because internal APIs are strongly encapsulated and may break.
```

### Java 17 Hot Interview Points

| Question | Strong Answer |
|---|---|
| Java 17 LTS? | Yes |
| Why records? | Immutable data carrier with less boilerplate |
| Record getter naming? | `name()`, not `getName()` |
| Can record have methods? | Yes |
| Good record use case? | DTOs and immutable request/response models |
| Bad record use case? | JPA entities and mutable lifecycle objects |
| Why sealed classes? | Restrict allowed subclasses |
| `instanceof` pattern matching benefit? | Avoid explicit cast after type check |

### Mini Program: Payment Modeling With Sealed Interface

```java
sealed interface Payment permits CardPayment, UpiPayment {
    int amount();
}

record CardPayment(String cardNumber, int amount) implements Payment {
}

record UpiPayment(String upiId, int amount) implements Payment {
}

public class PaymentExample {
    public static void main(String[] args) {
        Payment payment = new CardPayment("4111111111111111", 1000);

        if (payment instanceof CardPayment card) {
            System.out.println("Card payment: " + card.amount());
        }
    }
}
```

---

## 18.6 Java 18 To Java 20 Features

These are lower priority, but useful awareness.

### UTF-8 By Default

Java 18 made UTF-8 the default charset for standard Java APIs.

Interview answer:

```text
UTF-8 by default makes behavior more predictable across operating systems and environments.
Still, for important file/network code, I prefer explicitly passing StandardCharsets.UTF_8.
```

Example:

```java
String content = Files.readString(path, StandardCharsets.UTF_8);
```

### Simple Web Server

Java 18 added a simple command-line web server.

Example:

```text
jwebserver -p 8080
```

Use case:

```text
Quickly serving static files during development or testing.
```

### Lower Priority

For this interview, do not spend too much time here.

Know:
- UTF-8 by default
- Simple web server
- These are not core Java backend interview topics.

---

## 18.7 Java 21 LTS Features

Java 21 is an LTS release and the most important modern Java version after Java 17.

### Virtual Threads

Virtual threads are lightweight threads designed to simplify high-concurrency applications.

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        System.out.println("Running in virtual thread");
    });
}
```

### Why Virtual Threads Matter

Traditional platform threads are expensive.

Virtual threads are lightweight and allow a thread-per-task style for blocking workloads.

Good fit:
- Blocking HTTP calls
- Blocking DB calls
- High-concurrency request handling
- Thread-per-request style workloads

Not a magic fit:
- CPU-bound work
- Code with heavy synchronized pinning
- Work requiring limited external resources like DB connection pool size

### Important Virtual Thread Interview Answer

```text
Virtual threads are lightweight threads useful for high-concurrency blocking workloads.
They simplify thread-per-task programming, but they do not make CPU-bound code faster and
they do not remove the need to size downstream resources like DB connection pools.
```

### Platform Thread vs Virtual Thread

| Topic | Platform Thread | Virtual Thread |
|---|---|---|
| Backed by OS thread | Yes | Not one-to-one permanently |
| Cost | Expensive | Lightweight |
| Best for | General threading | Many blocking tasks |
| CPU-bound speedup | Limited by cores | Still limited by cores |
| Introduced final | Long-standing | Java 21 |

### Sequenced Collections

Java 21 introduced common APIs for collections with defined encounter order.

Examples of APIs:

```java
getFirst()
getLast()
reversed()
```

Example:

```java
SequencedCollection<String> names = new ArrayList<>(
    List.of("Aravind", "Rahul", "Priya")
);

System.out.println(names.getFirst()); // Aravind
System.out.println(names.getLast());  // Priya

SequencedCollection<String> reversed = names.reversed();
System.out.println(reversed); // [Priya, Rahul, Aravind]
```

Interview answer:

```text
Sequenced collections provide a common way to work with first, last, and reversed order
for ordered collections, instead of using collection-specific APIs.
```

### Pattern Matching For Switch

Java 21 finalized pattern matching for switch.

```java
static String describe(Object value) {
    return switch (value) {
        case Integer i -> "Integer: " + i;
        case String s -> "String: " + s;
        case null -> "Null value";
        default -> "Unknown";
    };
}
```

Why useful?

```text
It makes type-based branching more expressive and safer than long instanceof chains.
```

### Record Patterns

Record patterns allow destructuring records.

```java
record Point(int x, int y) {
}
```

```java
static void print(Object obj) {
    if (obj instanceof Point(int x, int y)) {
        System.out.println("x=" + x + ", y=" + y);
    }
}
```

### Record Patterns With Switch

```java
record CardPayment(String cardNumber, int amount) {
}

record UpiPayment(String upiId, int amount) {
}

static String describe(Object payment) {
    return switch (payment) {
        case CardPayment(String card, int amount) -> "Card amount: " + amount;
        case UpiPayment(String upi, int amount) -> "UPI amount: " + amount;
        default -> "Unknown payment";
    };
}
```

### Generational ZGC

Java 21 added Generational ZGC.

Interview-safe answer:

```text
Generational ZGC improves ZGC by separating young and old objects, which helps reduce GC
overhead while keeping low-pause behavior.
```

For this interview, keep this as awareness only.

### Structured Concurrency Preview

Structured concurrency was preview in Java 21.

Concept:

```text
Treat related concurrent tasks as one unit of work, so cancellation, failure, and joining
are easier to reason about.
```

Do not overclaim it as final in Java 21.

### Scoped Values Preview

Scoped values were preview in Java 21 and finalized later in Java 25.

Concept:

```text
Scoped values provide a safer way to share immutable context within a bounded execution scope,
especially with virtual threads.
```

### Java 21 Hot Interview Points

| Question | Strong Answer |
|---|---|
| Java 21 LTS? | Yes |
| Most famous Java 21 feature? | Virtual threads |
| Virtual threads good for? | High-concurrency blocking workloads |
| Virtual threads bad for? | CPU-bound acceleration and uncontrolled downstream load |
| What are sequenced collections? | Common first/last/reversed APIs for ordered collections |
| Pattern matching for switch? | Type-safe switch over object shapes/types |
| Record patterns? | Destructure records directly in pattern matching |
| Structured concurrency final in 21? | No, preview |
| Scoped values final in 21? | No, preview |

---

## 18.8 Java 22 To Java 24 Features

These are modern awareness topics. They are less likely than Java 17/21, but useful if interviewer asks "what's new in latest Java?"

### Java 22: Unnamed Variables And Patterns

Use `_` for values you intentionally do not use.

```java
try {
    process();
} catch (Exception _) {
    System.out.println("Processing failed");
}
```

Pattern example:

```java
if (obj instanceof Map.Entry<?, ?> entry) {
    System.out.println(entry.getKey());
}
```

Core idea:

```text
Unnamed variables make it clear that a variable is intentionally unused.
```

### Java 22: Foreign Function And Memory API

The Foreign Function and Memory API became final in Java 22.

Interview-safe answer:

```text
The Foreign Function and Memory API lets Java programs call native code and access memory
outside the JVM in a safer, more modern way than JNI for many use cases.
```

For most backend interviews, this is awareness only.

### Java 23: Markdown Documentation Comments

JavaDoc can use Markdown-style documentation comments.

Example:

```java
/// Returns the booking status.
///
/// Example:
/// ```java
/// service.getStatus("B1");
/// ```
String getStatus(String bookingId) {
    return "CONFIRMED";
}
```

Useful for cleaner API docs.

### Java 24: Stream Gatherers

Stream Gatherers let developers define custom intermediate stream operations.

Interview-safe answer:

```text
Stream Gatherers extend the Stream API by allowing custom intermediate operations,
especially useful for patterns not easily expressed with existing map/filter/flatMap.
```

Simple conceptual example:

```text
Custom windowing, batching, or stateful stream transformations.
```

For this interview:
- Know the concept.
- Do not spend coding practice time on it.

### Java 24: Class-File API

Java 24 added a standard API for reading, writing, and transforming Java class files.

Interview-safe answer:

```text
The Class-File API is mainly useful for tools, frameworks, and bytecode processing.
It is not a day-to-day Spring Boot application feature.
```

### Java 24: Security Manager Permanently Disabled

Interview-safe answer:

```text
The Security Manager has been deprecated for removal and is now permanently disabled.
Modern Java security relies more on OS/container boundaries, library controls, and platform
security practices.
```

### Java 24 Hot Interview Points

| Feature | Interview Priority | What To Say |
|---|---:|---|
| Stream Gatherers | Medium | Custom intermediate stream operations |
| Class-File API | Low-medium | Useful for tools and bytecode frameworks |
| FFM API | Medium | Native interop and off-heap memory |
| Security Manager disabled | Medium | Old security model removed/disabled |

---

## 18.9 Java 25 LTS Features

Java 25 is a newer LTS release from most vendors.

Use this section as awareness. For most enterprise interviews, Java 17 and 21 are still more likely to be asked deeply.

### Compact Source Files And Instance Main Methods

Java 25 finalized compact source files and instance main methods.

Traditional:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

Compact style:

```java
void main() {
    System.out.println("Hello");
}
```

Interview answer:

```text
Compact source files and instance main methods reduce ceremony for small programs,
scripts, demos, and learning. They are not meant to replace normal structured application
code in large Spring Boot services.
```

### Module Import Declarations

Java 25 added module import declarations.

Concept:

```java
import module java.base;
```

Interview answer:

```text
Module import declarations let source files import the exported packages of a module more
conveniently. It is mainly useful in modular code and compact source examples.
```

### Flexible Constructor Bodies

Java 25 relaxed constructor rules so some statements can appear before explicit constructor invocation in controlled ways.

Conceptual example:

```java
class Booking {
    private final String id;

    Booking(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id;
    }
}
```

Interview-safe answer:

```text
Flexible constructor bodies improve constructor readability and validation patterns, while
still preserving object initialization safety rules.
```

### Scoped Values

Scoped values became a final feature in Java 25.

They are an alternative to many ThreadLocal use cases.

Concept:

```text
Share immutable context within a bounded execution scope.
```

Typical use cases:
- Request ID
- Tenant ID
- Security context
- Correlation ID

Interview answer:

```text
Scoped values are useful for passing immutable context through a bounded call scope,
especially with virtual threads. They are safer and easier to reason about than ThreadLocal
for many context propagation cases.
```

### Structured Concurrency Still Preview

Structured concurrency is still a preview feature in Java 25.

Interview answer:

```text
Structured concurrency is promising for managing related concurrent tasks as one unit, but
since it is still preview, I would be careful before using it in production unless the team
explicitly accepts preview features.
```

### Stable Values Preview

Stable values are a preview feature in Java 25.

Concept:

```text
They support values that are initialized at most once and then treated as stable, helping
with safe lazy initialization patterns.
```

### Key Derivation Function API

Java 25 added a Key Derivation Function API.

Interview-safe answer:

```text
The KDF API provides standard support for deriving cryptographic keys, which is useful in
security-sensitive systems. For normal backend work, I would rely on platform libraries and
security team guidance.
```

### Compact Object Headers

Java 25 includes compact object headers.

Interview-safe answer:

```text
Compact object headers reduce object header size, improving memory footprint for applications
with many objects. This is mostly JVM/runtime awareness rather than everyday coding syntax.
```

### Generational Shenandoah

Java 25 includes Generational Shenandoah.

Interview-safe answer:

```text
Generational Shenandoah improves the Shenandoah low-pause garbage collector by separating
young and old objects, which can improve throughput and memory behavior.
```

### Java 25 Hot Interview Points

| Feature | Status | Interview Priority | What To Say |
|---|---|---:|---|
| Compact source files | Final | Medium | Less ceremony for small Java programs |
| Module import declarations | Final | Low-medium | Easier module-level imports |
| Flexible constructor bodies | Final | Medium | Cleaner validation/init before constructor chaining |
| Scoped values | Final | Medium-high | Immutable scoped context, useful with virtual threads |
| Structured concurrency | Preview | Medium | Promising but preview |
| Stable values | Preview | Low-medium | Safe lazy initialization concept |
| Stream Gatherers | Java 24 final | Medium | Custom intermediate stream ops |

---

## 18.10 JDK 26 Release-Candidate / Early-Access Awareness

As of this sheet update, official OpenJDK pages show JDK 26 as release-candidate / early-access material, while JDK 25 is the latest GA line visible in the OpenJDK GA archive.

Use this only as awareness.

### HTTP/3 For HTTP Client API

JDK 26 targets HTTP/3 support for Java's HTTP Client API.

Interview-safe answer:

```text
HTTP/3 support in the Java HTTP Client is a modern networking enhancement. For production,
I would check JDK support, server support, proxy/load-balancer compatibility, and operational
observability before relying on it.
```

### Structured Concurrency Preview Continues

JDK 26 includes another preview of structured concurrency.

Strong answer:

```text
Structured concurrency is still evolving. I understand the model, but I would avoid claiming
it as a stable production feature unless the project is explicitly using preview APIs.
```

### Primitive Types In Patterns Preview

JDK 26 continues preview work around primitive types in patterns, `instanceof`, and `switch`.

Strong answer:

```text
This extends pattern matching to work more naturally with primitive types, but since it is
preview, I would treat it as future-facing language awareness.
```

### JDK 26 Hot Interview Point

```text
If asked about the latest Java, mention Java 25 as the latest LTS/GA awareness point and
JDK 26 as release-candidate or early-access awareness, not as something you assume in
enterprise production.
```

---

## 18.11 Modern Java Feature Grouping For Interview Recall

### Language Syntax Improvements

| Feature | Version | Why It Matters |
|---|---:|---|
| `var` | 10 | Less verbose local variables |
| Switch expressions | 14 | Cleaner switch returning values |
| Text blocks | 15 | Cleaner multi-line strings |
| Records | 16 | Immutable data carriers |
| Pattern matching for `instanceof` | 16 | Avoid manual casts |
| Sealed classes | 17 | Controlled inheritance |
| Pattern matching for switch | 21 | Safer type-based switch |
| Record patterns | 21 | Destructure records |
| Compact source files | 25 | Less ceremony for small programs |

### Library And Runtime Improvements

| Feature | Version | Why It Matters |
|---|---:|---|
| HTTP Client | 11 | Modern sync/async HTTP client |
| UTF-8 by default | 18 | Predictable charset behavior |
| Virtual threads | 21 | High-concurrency blocking workloads |
| Sequenced collections | 21 | Common first/last/reversed APIs |
| FFM API | 22 | Native interop |
| Stream Gatherers | 24 | Custom stream intermediate operations |
| Scoped values | 25 | Scoped immutable context |

### JVM / Operations Awareness

| Feature | Version | Why It Matters |
|---|---:|---|
| Helpful NullPointerExceptions | 14 | Easier debugging |
| Strong encapsulation | 17 | Avoid internal JDK APIs |
| Generational ZGC | 21 | Low-pause GC improvements |
| Security Manager disabled | 24 | Old security mechanism gone |
| Compact object headers | 25 | Memory footprint optimization |

---

## 18.12 Modern Java Mini Programs

### Program 1: Records + Validation

```java
import java.time.LocalDate;

record BookingRequest(String hotelId, LocalDate checkIn, LocalDate checkOut) {
    BookingRequest {
        if (hotelId == null || hotelId.isBlank()) {
            throw new IllegalArgumentException("hotelId is required");
        }
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Invalid stay dates");
        }
    }
}

public class BookingRequestExample {
    public static void main(String[] args) {
        BookingRequest request = new BookingRequest(
            "HOTEL-1",
            LocalDate.of(2026, 4, 25),
            LocalDate.of(2026, 4, 28)
        );

        System.out.println(request);
    }
}
```

### Program 2: Pattern Matching For `instanceof`

```java
public class PatternInstanceofExample {
    public static void main(String[] args) {
        Object value = "marriott";

        if (value instanceof String text) {
            System.out.println(text.toUpperCase());
        }
    }
}
```

### Program 3: Sealed Types + Records

```java
sealed interface BookingEvent permits BookingCreated, BookingCancelled {
}

record BookingCreated(String bookingId) implements BookingEvent {
}

record BookingCancelled(String bookingId, String reason) implements BookingEvent {
}

public class BookingEventExample {
    public static void main(String[] args) {
        BookingEvent event = new BookingCreated("B1");

        if (event instanceof BookingCreated created) {
            System.out.println("Created: " + created.bookingId());
        }
    }
}
```

### Program 4: Java 21 Pattern Matching For Switch

```java
public class PatternSwitchExample {
    static String describe(Object value) {
        return switch (value) {
            case null -> "null";
            case String text -> "String: " + text;
            case Integer number -> "Integer: " + number;
            default -> "Unknown";
        };
    }

    public static void main(String[] args) {
        System.out.println(describe("booking"));
        System.out.println(describe(100));
        System.out.println(describe(null));
    }
}
```

### Program 5: Virtual Threads

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadExample {
    public static void main(String[] args) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 5; i++) {
                int taskId = i;
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " running on " + Thread.currentThread());
                });
            }
        }
    }
}
```

### Program 6: Sequenced Collections

```java
import java.util.*;

public class SequencedCollectionExample {
    public static void main(String[] args) {
        SequencedCollection<String> names = new ArrayList<>(
            List.of("Aravind", "Rahul", "Priya")
        );

        System.out.println(names.getFirst());
        System.out.println(names.getLast());
        System.out.println(names.reversed());
    }
}
```

### Program 7: Java 25 Compact Source File

This style is for small programs and demos.

```java
void main() {
    System.out.println("Hello from compact Java");
}
```

For production Spring Boot projects, normal class-based structure is still expected.

---

## 18.13 Modern Java Common Mistakes

| Mistake | Why It Is Wrong | Better Answer |
|---|---|---|
| Saying `var` is dynamic typing | Java remains statically typed | Say compiler infers local type |
| Using records for JPA entities blindly | JPA often needs no-arg constructor/proxies/mutability | Use records mainly for DTOs |
| Saying virtual threads make CPU code faster | CPU-bound work is still core-limited | Virtual threads help blocking concurrency |
| Ignoring DB pool limits with virtual threads | More threads can overwhelm downstream systems | Still control DB/HTTP concurrency |
| Saying structured concurrency is final in Java 21/25 | It is preview in those versions | Say it is promising but preview |
| Treating `List.of` like `ArrayList` | It is immutable and null-hostile | Use `new ArrayList<>(List.of(...))` if mutable needed |
| Overusing latest syntax in old codebase | Team may run Java 8/11/17 | Match project runtime |
| Claiming JDK 26 features as normal production features | It is RC/EA awareness in official pages checked | Present as latest awareness only |

---

## 18.14 Modern Java Rapid Revision

### Java 17 Must-Say Lines

```text
Java 17 is an LTS release. Important features include records, sealed classes,
pattern matching for instanceof, text blocks, switch expressions, and stronger
encapsulation of JDK internals.
```

### Java 21 Must-Say Lines

```text
Java 21 is an LTS release. Important features include virtual threads, sequenced
collections, pattern matching for switch, record patterns, and Generational ZGC.
```

### Java 25 Must-Say Lines

```text
Java 25 is a newer LTS release. Important awareness topics include compact source files,
module import declarations, flexible constructor bodies, scoped values, and runtime
improvements like compact object headers and Generational Shenandoah.
```

### Latest Feature Safety Line

```text
For latest Java features, I separate final LTS features from preview/incubator features.
I am comfortable discussing preview APIs, but I would not use them in production without
explicit team and build support.
```

---

## 19. Hot Interview Questions And Answers

### Q1. What are the main features of Java 8?

```text
Lambda expressions, functional interfaces, streams, Optional, default and static methods
in interfaces, new Date-Time API, CompletableFuture, Base64, and Map enhancements.
```

### Q2. What is a lambda expression?

```text
A lambda expression is a concise implementation of a functional interface. It lets us pass
behavior as a method argument.
```

### Q3. What is a functional interface?

```text
An interface with exactly one abstract method. It can have default and static methods.
```

### Q4. Can a functional interface have multiple default methods?

```text
Yes. The restriction is only on abstract methods.
```

### Q5. Why was Optional introduced?

```text
To represent optional return values explicitly and reduce direct null checks.
```

### Q6. Difference between Optional.of and Optional.ofNullable?

```text
Optional.of throws NullPointerException for null. Optional.ofNullable allows null and returns
Optional.empty.
```

### Q7. Difference between orElse and orElseGet?

```text
orElse evaluates the default value eagerly. orElseGet invokes the supplier lazily only when
Optional is empty.
```

### Q8. Why default methods in interfaces?

```text
To add new methods to existing interfaces without breaking implementing classes.
```

### Q9. What happens if two interfaces have same default method?

```text
The implementing class must override the method and resolve the conflict.
```

### Q10. Difference between map and flatMap?

```text
map transforms one element into one result. flatMap transforms one element into a stream of
results and flattens nested streams.
```

### Q11. What is CompletableFuture?

```text
CompletableFuture is an asynchronous computation API that supports chaining, combining,
manual completion, and exception handling.
```

### Q12. thenApply vs thenCompose?

```text
thenApply transforms a value. thenCompose is used when transformation itself returns another
CompletableFuture and we want to flatten it.
```

### Q13. thenCompose vs thenCombine?

```text
thenCompose is for dependent async calls. thenCombine is for combining two independent async
results.
```

### Q14. Why new Date-Time API?

```text
The old Date and Calendar APIs were mutable and hard to use. java.time is immutable,
thread-safe, clearer, and better for timezone handling.
```

### Q15. Period vs Duration?

```text
Period is date-based, like days/months/years. Duration is time-based, like seconds/minutes/hours.
```

### Q16. What changed from PermGen to Metaspace?

```text
Java 8 removed PermGen and introduced Metaspace for class metadata. Metaspace uses native
memory and can grow dynamically.
```

### Q17. Is parallel stream always faster?

```text
No. It helps only for large CPU-bound independent tasks. It can hurt with small data,
blocking IO, ordering, or shared mutable state.
```

### Q18. What is `var`?

```text
var is local variable type inference. Java remains statically typed; compiler infers the type.
```

### Q19. What are records?

```text
Records are compact immutable data carriers that automatically provide constructor, accessors,
equals, hashCode, and toString.
```

### Q20. What are virtual threads?

```text
Virtual threads are lightweight threads useful for high-concurrency blocking workloads.
They simplify thread-per-task style without requiring huge platform thread pools.
```

### Q21. Are virtual threads always faster?

```text
No. They improve scalability for blocking workloads, but CPU-bound work is still limited
by available CPU cores. They also do not remove limits like DB connection pool size.
```

### Q22. What are sealed classes?

```text
Sealed classes restrict which classes can extend or implement a type. They are useful when
the domain has a fixed set of known subtypes, such as payment methods or booking events.
```

### Q23. What are records best used for?

```text
Records are best for immutable data carriers like DTOs, request/response models, and
projection objects. I would avoid them for JPA entities with mutable lifecycle requirements.
```

### Q24. What is pattern matching for `instanceof`?

```text
It combines type check and cast. If the object matches the type, Java creates a typed
variable directly, avoiding manual casting.
```

### Q25. What is pattern matching for switch?

```text
It allows switch to branch based on object type and structure, making type-based logic
cleaner than long instanceof chains.
```

### Q26. What are sequenced collections?

```text
Sequenced collections provide common APIs like getFirst, getLast, and reversed for
collections that have a defined encounter order.
```

### Q27. What are text blocks?

```text
Text blocks are multi-line string literals. They make JSON, SQL, HTML, and test payloads
more readable without heavy escaping or string concatenation.
```

### Q28. What are switch expressions?

```text
Switch expressions let switch return a value and reduce boilerplate. Arrow cases avoid
accidental fall-through, and block cases can use yield.
```

### Q29. What is the difference between `isBlank` and `isEmpty`?

```text
isEmpty checks whether string length is zero. isBlank checks whether the string is empty
or contains only whitespace.
```

### Q30. What is the difference between `trim` and `strip`?

```text
trim is the older method and removes characters up to U+0020. strip is Unicode-aware and
preferred for modern whitespace handling.
```

### Q31. What are scoped values?

```text
Scoped values allow immutable context to be shared within a bounded execution scope.
They are useful for request IDs, tenant IDs, and correlation IDs, especially with virtual
threads.
```

### Q32. What is structured concurrency?

```text
Structured concurrency treats related concurrent tasks as one unit of work, making failure,
cancellation, and joining easier to reason about. In Java 21 and Java 25, it is still a
preview feature.
```

### Q33. What are stream gatherers?

```text
Stream gatherers allow custom intermediate stream operations, useful for patterns like
windowing, batching, or stateful transformations that are awkward with only map/filter.
```

### Q34. What is new in Java 25 that is useful to know?

```text
Java 25 is a newer LTS release. Useful awareness topics include compact source files,
module import declarations, flexible constructor bodies, scoped values, and runtime
improvements like compact object headers and Generational Shenandoah.
```

### Q35. How do you talk about very latest Java features safely?

```text
I separate final LTS features from preview or incubator features. I can discuss preview
features, but I would not use them in production unless the team explicitly enables and
accepts preview APIs.
```

---

## 20. Sample Programs To Practice

### Program 1: Functional Interface Calculator

```java
@FunctionalInterface
interface Operation {
    int apply(int a, int b);
}

public class CalculatorExample {
    public static void main(String[] args) {
        Operation add = (a, b) -> a + b;
        Operation subtract = (a, b) -> a - b;
        Operation multiply = (a, b) -> a * b;

        System.out.println(add.apply(10, 5));      // 15
        System.out.println(subtract.apply(10, 5)); // 5
        System.out.println(multiply.apply(10, 5)); // 50
    }
}
```

### Program 2: Predicate-Based Filtering

```java
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PredicateFilterExample {
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Aravind", "Anil", "Rahul", "Priya");

        Predicate<String> startsWithA = name -> name.startsWith("A");
        Predicate<String> lengthGreaterThanFour = name -> name.length() > 4;

        List<String> result = names.stream()
            .filter(startsWithA.and(lengthGreaterThanFour))
            .collect(Collectors.toList());

        System.out.println(result); // [Aravind]
    }
}
```

### Program 3: Optional Safe Lookup

```java
import java.util.*;

public class OptionalLookupExample {
    static class User {
        int id;
        String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static Optional<User> findUser(List<User> users, int id) {
        return users.stream()
            .filter(user -> user.id == id)
            .findFirst();
    }

    public static void main(String[] args) {
        List<User> users = Arrays.asList(
            new User(1, "Aravind"),
            new User(2, "Rahul")
        );

        String name = findUser(users, 3)
            .map(user -> user.name)
            .orElse("Guest");

        System.out.println(name); // Guest
    }
}
```

### Program 4: Date-Time Booking Nights

```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BookingNightsExample {
    public static void main(String[] args) {
        LocalDate checkIn = LocalDate.of(2026, 4, 25);
        LocalDate checkOut = LocalDate.of(2026, 4, 28);

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights <= 0) {
            throw new IllegalArgumentException("Checkout must be after checkin");
        }

        System.out.println("Nights: " + nights);
    }
}
```

### Program 5: Map Merge Frequency Count

```java
import java.util.*;

public class MapMergeFrequencyExample {
    public static void main(String[] args) {
        List<String> words = Arrays.asList("java", "spring", "java", "kafka", "spring");

        Map<String, Integer> frequency = new HashMap<>();

        words.forEach(word -> frequency.merge(word, 1, Integer::sum));

        System.out.println(frequency);
    }
}
```

### Program 6: CompletableFuture Aggregation

```java
import java.util.concurrent.*;

public class CompletableFutureAggregationExample {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        CompletableFuture<Integer> room = CompletableFuture.supplyAsync(() -> 5000, executor);
        CompletableFuture<Integer> tax = CompletableFuture.supplyAsync(() -> 900, executor);
        CompletableFuture<Integer> discount = CompletableFuture.supplyAsync(() -> 500, executor);

        CompletableFuture<Integer> total = room
            .thenCombine(tax, Integer::sum)
            .thenCombine(discount, (amount, discountAmount) -> amount - discountAmount);

        System.out.println(total.join()); // 5400

        executor.shutdown();
    }
}
```

---

## 21. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Calling `Optional.get()` directly | Throws if empty | Use `orElse`, `orElseGet`, `orElseThrow` |
| Using `Optional.of(null)` | Throws NPE | Use `Optional.ofNullable` |
| Using parallel stream for DB calls | Can block common pool | Use controlled executor or async IO |
| Mutating external list in stream | Side effects, unsafe in parallel | Use `map/filter/collect` |
| Forgetting terminal stream operation | Pipeline never executes | Add `collect`, `count`, `forEach`, etc. |
| Using `toMap` with duplicate keys | Throws exception | Provide merge function |
| Wrong identity in reduce | Incorrect result | Use proper identity like `0` for sum, `1` for product |
| Treating Base64 as encryption | It is reversible encoding | Use encryption/hashing for security |
| Using old `Date` for new code | Mutable and clumsy | Use `java.time` |
| Assuming `var` is dynamic typing | Java remains static | Compiler infers local type |
| Using records for JPA entities blindly | Records are immutable and can conflict with entity lifecycle/proxies | Use records mostly for DTOs |
| Saying virtual threads make CPU code faster | CPU-bound work is still limited by cores | Use virtual threads for blocking concurrency |
| Ignoring downstream limits with virtual threads | DB pools and APIs can still be overloaded | Control concurrency and resource pools |
| Claiming preview APIs as production baseline | Preview APIs can change | Say "preview" clearly |
| Treating latest JDK syntax as always available | Runtime may be Java 8/11/17 | Match project JDK version |

---

## 22. Final Rapid Revision

### Java 8 Must-Say Lines

```text
Lambda is a concise implementation of a functional interface.
```

```text
Functional interface has exactly one abstract method.
```

```text
Streams are lazy and execute only after terminal operation.
```

```text
Optional should mainly be used as a return type for possibly absent values.
```

```text
Default methods allow interfaces to evolve without breaking existing implementations.
```

```text
java.time is immutable and thread-safe compared to old Date and Calendar APIs.
```

```text
CompletableFuture is used for async chaining, combining, and error handling.
```

```text
Parallel streams are not always faster; use them only for suitable CPU-bound independent work.
```

### Modern Java Must-Say Lines

```text
Java 17 is an LTS release with important features like records, sealed classes,
pattern matching for instanceof, text blocks, switch expressions, and stronger JDK
internal encapsulation.
```

```text
Java 21 is an LTS release with virtual threads, sequenced collections, pattern matching
for switch, record patterns, and Generational ZGC.
```

```text
Java 25 is a newer LTS release; I know it mainly as awareness for compact source files,
module import declarations, flexible constructor bodies, scoped values, and JVM/runtime
improvements.
```

```text
I separate final LTS features from preview/incubator features before recommending them
for production use.
```

### One-Hour Revision Order

1. Lambda and functional interface.
2. Predicate, Function, Consumer, Supplier.
3. Optional traps.
4. Default method conflict rules.
5. Date-Time API classes.
6. CompletableFuture methods.
7. Map enhancements.
8. Records, sealed classes, and pattern matching.
9. Virtual threads and sequenced collections.
10. Latest Java awareness: Java 25 final vs preview features.

### Fifteen Must-Code Snippets

1. Custom functional interface calculator.
2. Predicate chaining.
3. Function `andThen` and `compose`.
4. Optional lookup with `orElseThrow`.
5. Default method conflict resolution.
6. Date formatting and parsing.
7. Count frequency using `merge`.
8. Group by using collectors.
9. CompletableFuture `thenCombine`.
10. CompletableFuture exception fallback.
11. Record DTO with compact constructor validation.
12. Pattern matching for `instanceof`.
13. Sealed interface with record implementations.
14. Pattern matching for switch.
15. Virtual thread executor example.

---

## 23. How To Answer If Asked "How Much Java 8+ Have You Used?"

```text
I use Java 8 features regularly in backend development, especially lambdas, streams,
Optional, functional interfaces, method references, Date-Time API, and Map enhancements.
For service-level code, I use streams for readable transformations and grouping, Optional
for safe return values, and CompletableFuture when independent calls can be composed
asynchronously. I also understand modern Java features from Java 17 and Java 21, such as
records, sealed classes, pattern matching, and virtual threads. I am careful not to overuse
streams when a normal loop is clearer, and I separate stable LTS features from preview APIs
before recommending them for production.
```

This answer is balanced and practical.

---

## 24. Official Source Notes

Use official OpenJDK pages when refreshing this sheet:

- JDK 17 project page: `https://openjdk.org/projects/jdk/17/`
- JDK 17 JEPs since JDK 11: `https://openjdk.org/projects/jdk/17/jeps-since-jdk-11`
- JDK 21 project page: `https://openjdk.org/projects/jdk/21/`
- JDK 21 JEPs since JDK 17: `https://openjdk.org/projects/jdk/21/jeps-since-jdk-17`
- JDK 25 project page: `https://openjdk.org/projects/jdk/25/`
- JDK 25 JEPs since JDK 21: `https://openjdk.org/projects/jdk/25/jeps-since-jdk-21`
- OpenJDK GA archive: `https://jdk.java.net/archive/`
- JDK 26 page for release-candidate / early-access awareness: `https://openjdk.org/projects/jdk/26/`
