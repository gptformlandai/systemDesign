# Java Platform, Tooling, Testing, And Security FAANG Master Sheet

Target: final Java platform layer for FAANG-style interviews and production readiness.

This sheet covers:
- Classpath vs module path
- JPMS / Java Platform Module System
- JAR, WAR, executable JAR
- Maven and Gradle basics
- Dependency conflicts and version alignment
- Unit, integration, contract, and performance testing
- JUnit, Mockito, Testcontainers, JMH
- GraalVM Native Image
- JNI and Foreign Function & Memory API awareness
- Java security basics
- SecureRandom, hashing, encryption, TLS, secrets
- Supply-chain hygiene

---

## 1. Mental Model

Java is more than syntax and JVM internals.

A production Java engineer must understand:

```text
Language
    -> classes, generics, collections, concurrency

Runtime
    -> JVM, GC, JIT, diagnostics

Platform
    -> modules, classpath, packaging, build tools, dependencies

Engineering
    -> testing, benchmarking, security, deployment
```

Strong interview line:

```text
Java maturity means knowing how code is compiled, packaged, tested, secured, diagnosed,
and shipped, not only how to write classes and methods.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Classpath | Very high | Runtime dependency loading |
| Module path / JPMS | Medium-high | Modern Java platform awareness |
| JAR packaging | Very high | Daily backend deployment |
| Maven/Gradle | Very high | Build and dependency management |
| Dependency conflicts | Very high | Real production issue |
| JUnit | Very high | Testing baseline |
| Mockito | High | Unit test isolation |
| Testcontainers | Medium-high | Real integration tests |
| JMH | High | Correct benchmarking |
| GraalVM Native Image | High | Modern deployment option |
| JNI / FFM | Medium | Native interop awareness |
| Java security basics | High | Avoid dangerous mistakes |
| Supply chain | High | Dependency hygiene |

---

## 3. Classpath

Classpath tells the JVM and compiler where to find classes and resources.

Example:

```text
javac -cp lib/a.jar:lib/b.jar App.java
java -cp .:lib/a.jar:lib/b.jar App
```

On Windows, the separator is usually `;`.

Classpath can contain:

- Directories
- JAR files
- Wildcards like `lib/*`

Common issue:

```text
ClassNotFoundException or NoClassDefFoundError often means the class was missing from the
runtime classpath or failed during initialization.
```

Strong answer:

```text
Classpath is the legacy mechanism for locating classes and resources. If a dependency is
available during compilation but missing at runtime, the app can fail with class loading errors.
```

---

## 4. ClassNotFoundException vs NoClassDefFoundError

| Error | Meaning |
|---|---|
| ClassNotFoundException | Code explicitly tried to load a class by name and could not find it |
| NoClassDefFoundError | Class was available earlier or expected by bytecode, but missing/failing at runtime |

Example:

```java
Class.forName("com.example.Driver");
```

If missing:

```text
ClassNotFoundException
```

Runtime dependency missing:

```text
NoClassDefFoundError
```

Interview line:

```text
ClassNotFoundException is checked and usually comes from reflective loading. NoClassDefFoundError
is an Error from runtime linkage/class loading failure.
```

---

## 5. JPMS / Java Modules

JPMS means Java Platform Module System.

It was introduced in Java 9.

Core file:

```java
module com.example.booking {
    requires java.sql;
    exports com.example.booking.api;
}
```

Module benefits:

- Strong encapsulation
- Explicit dependencies
- Reliable configuration
- Smaller runtime images with `jlink`

Module terms:

| Term | Meaning |
|---|---|
| module | Named unit of code |
| requires | Declares dependency |
| exports | Makes package visible to other modules |
| opens | Allows reflective access |
| module path | Where modules are found |

Strong answer:

```text
JPMS makes dependencies and exported packages explicit. It improves encapsulation compared
with classpath, but many enterprise apps still use classpath because frameworks and legacy
dependencies are not always fully modular.
```

---

## 6. Classpath vs Module Path

| Classpath | Module Path |
|---|---|
| Legacy | Java 9+ module system |
| No strong boundaries | Explicit module boundaries |
| Can hide dependency issues | More reliable configuration |
| Easier with legacy libs | Better encapsulation |
| Split packages can happen | Split packages are restricted |

Interview line:

```text
Classpath is simpler and common. Module path gives stronger structure but requires the
application and dependencies to be module-friendly.
```

---

## 7. JAR, WAR, And Executable JAR

JAR:

```text
Java Archive containing classes/resources.
```

WAR:

```text
Web Application Archive traditionally deployed to servlet containers.
```

Executable JAR:

```text
JAR with a main class or framework launcher, often used by Spring Boot services.
```

Manifest example:

```text
Main-Class: com.example.App
```

Run:

```text
java -jar app.jar
```

Strong answer:

```text
Modern backend Java services are commonly shipped as executable JARs in containers, while
WAR deployment is more common in older app-server models.
```

---

## 8. Maven

Maven uses convention over configuration.

Standard layout:

```text
src/main/java
src/main/resources
src/test/java
src/test/resources
pom.xml
```

Dependency:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.1.0</version>
    <scope>test</scope>
</dependency>
```

Common commands:

```text
mvn clean test
mvn clean package
mvn dependency:tree
```

Scopes:

| Scope | Meaning |
|---|---|
| compile | Needed at compile and runtime |
| provided | Compile-time, provided by runtime/container |
| runtime | Needed only at runtime |
| test | Needed only for tests |

Interview line:

```text
Maven gives a standard lifecycle, dependency management, and reproducible builds through
pom.xml.
```

---

## 9. Gradle

Gradle is flexible and task-based.

Example:

```groovy
plugins {
    id 'java'
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:6.1.0'
}

test {
    useJUnitPlatform()
}
```

Common commands:

```text
./gradlew test
./gradlew build
./gradlew dependencies
```

Maven vs Gradle:

| Maven | Gradle |
|---|---|
| XML, convention-driven | Groovy/Kotlin DSL, flexible |
| Predictable lifecycle | Powerful task graph |
| Very common in enterprises | Strong for multi-module/custom builds |

Strong answer:

```text
Both Maven and Gradle solve build and dependency management. Maven is more convention-heavy;
Gradle is more programmable and can be faster/flexible in complex builds.
```

---

## 10. Dependency Conflicts

Common problems:

- Two libraries require different versions of same dependency.
- Runtime dependency missing.
- Binary incompatibility.
- Duplicate classes.
- Vulnerable transitive dependency.

Maven inspection:

```text
mvn dependency:tree
```

Gradle inspection:

```text
./gradlew dependencies
./gradlew dependencyInsight --dependency guava
```

Strong answer:

```text
For dependency conflicts, I inspect the dependency tree, identify the transitive path,
align versions using dependency management or constraints, and run tests to catch binary
compatibility issues.
```

---

## 11. Semantic Versioning Reality

Semantic versioning idea:

```text
MAJOR.MINOR.PATCH
```

But in real projects:

- Not every library follows SemVer perfectly.
- Minor upgrades can break behavior.
- Transitive dependencies can change unexpectedly.
- Security patches can require urgent upgrades.

Production rule:

```text
Lock versions, review changelogs, run tests, scan vulnerabilities, and deploy gradually.
```

---

## 12. JUnit

JUnit is the standard Java testing framework family.

Basic test:

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculatorTest {
    @Test
    void addsNumbers() {
        Calculator calculator = new Calculator();

        assertEquals(5, calculator.add(2, 3));
    }
}
```

Parameterized test:

```java
@ParameterizedTest
@CsvSource({
    "2,3,5",
    "10,5,15"
})
void addsNumbers(int a, int b, int expected) {
    assertEquals(expected, new Calculator().add(a, b));
}
```

Strong answer:

```text
Unit tests should be fast, deterministic, focused on behavior, and cover normal, boundary,
and failure cases.
```

---

## 13. Mockito

Mockito creates test doubles for dependencies.

Example:

```java
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    @Test
    void returnsOrder() {
        OrderRepository repository = mock(OrderRepository.class);
        when(repository.findById("o1")).thenReturn(new Order("o1"));

        OrderService service = new OrderService(repository);

        assertEquals("o1", service.getOrder("o1").id());
        verify(repository).findById("o1");
    }
}
```

Use mocks for:

- External dependencies
- Slow dependencies
- Failure scenarios

Avoid:

- Mocking value objects
- Mocking everything
- Over-verifying implementation details

Interview line:

```text
Mocks are useful for isolating behavior, but too many mocks can make tests brittle and
coupled to implementation instead of behavior.
```

---

## 14. Testcontainers

Testcontainers runs real dependencies in containers during tests.

Use cases:

- PostgreSQL integration tests
- Kafka integration tests
- Redis integration tests
- LocalStack/AWS-like tests

Why:

```text
It catches integration problems mocks cannot catch, such as SQL syntax, migrations,
serialization, and broker behavior.
```

Interview line:

```text
I use unit tests for business logic and containerized integration tests for real dependency
behavior when mocks would hide important risks.
```

---

## 15. Testing Pyramid For Java Backend

| Test Type | Purpose |
|---|---|
| Unit | Fast logic validation |
| Slice/component | Framework layer testing |
| Integration | Real DB/broker/cache behavior |
| Contract | Producer/consumer API compatibility |
| End-to-end | Critical user journey |
| Performance | Latency/throughput under load |

Strong answer:

```text
I want many fast unit tests, enough integration tests for confidence, contract tests for
service boundaries, and limited end-to-end tests for critical paths.
```

---

## 16. JMH Benchmarking

JMH means Java Microbenchmark Harness.

Why normal timing is bad:

```java
long start = System.nanoTime();
method();
long end = System.nanoTime();
```

This can be misleading because of:

- JIT warm-up
- Dead code elimination
- Constant folding
- GC noise
- CPU frequency changes
- Small sample size

JMH handles:

- Warm-up
- Measurement iterations
- Forks
- Blackholes
- Benchmark modes

Conceptual example:

```java
@Benchmark
public int sum() {
    int total = 0;
    for (int i = 0; i < 1000; i++) {
        total += i;
    }
    return total;
}
```

Strong answer:

```text
For Java microbenchmarks, I use JMH because it accounts for JVM warm-up and JIT effects
that make naive timing unreliable.
```

---

## 17. GraalVM Native Image

GraalVM Native Image compiles Java ahead-of-time into a native executable.

Benefits:

- Very fast startup.
- Lower memory footprint for some workloads.
- No JIT warm-up.
- Good for CLI tools, serverless, small services, and fast-scaling containers.

Trade-offs:

- Longer build time.
- Reflection/dynamic proxies need configuration or metadata.
- Closed-world assumption.
- Some libraries need native-image compatibility work.
- Peak throughput may differ from HotSpot JIT workloads.

Strong answer:

```text
Native Image is useful when startup time and memory footprint matter, but I would verify
library compatibility, reflection usage, build complexity, and runtime performance before
choosing it for a service.
```

Native Image mental model:

```text
HotSpot JVM:
    bytecode -> runtime profiling -> JIT optimization

Native Image:
    static analysis at build time -> native executable -> fast startup
```

---

## 18. Closed-World Assumption

Native Image analyzes reachable code at build time.

Problem:

```text
Reflection, dynamic proxies, resources, JNI, and dynamic class loading may not be visible
to static analysis automatically.
```

Mitigation:

- Reachability metadata.
- Framework support.
- Native-image tracing agent.
- Explicit resource/reflection configuration.

Interview line:

```text
The closed-world assumption is why some dynamic Java patterns need extra metadata for
GraalVM Native Image.
```

---

## 19. JNI And FFM API Awareness

JNI:

```text
Legacy Java Native Interface for calling native code.
```

Foreign Function & Memory API:

```text
Modern API direction for calling native functions and safely accessing off-heap memory.
```

Use cases:

- Native libraries
- High-performance computing
- System integrations
- Existing C libraries

Risks:

- Memory safety
- Platform-specific behavior
- Harder debugging
- Deployment complexity

Strong answer:

```text
Native interop is powerful but should be isolated behind a small Java abstraction because
it weakens portability and memory-safety guarantees.
```

---

## 20. Java Security Basics

Security basics every Java backend developer should know:

- Do not deserialize untrusted data.
- Do not log secrets.
- Do not hardcode secrets.
- Use TLS for network calls.
- Validate inputs at boundaries.
- Use parameterized SQL.
- Use strong password hashing.
- Use SecureRandom for security tokens.
- Keep dependencies patched.
- Prefer framework-supported security patterns.

Interview line:

```text
Most Java security failures are not exotic JVM problems. They are unsafe deserialization,
bad dependency hygiene, weak secret handling, injection, and missing validation.
```

---

## 21. SecureRandom

Bad:

```java
Random random = new Random();
```

Why:

```text
Random is predictable and not for security tokens.
```

Better:

```java
import java.security.SecureRandom;
import java.util.Base64;

class TokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    static String token() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

Use SecureRandom for:

- Session tokens
- Password reset tokens
- API keys
- Nonces
- Cryptographic salts

---

## 22. Hashing vs Encryption

Hashing:

```text
One-way transformation.
```

Encryption:

```text
Two-way transformation with key.
```

Password storage:

```text
Use password hashing algorithms like bcrypt, scrypt, Argon2, or PBKDF2 with salt.
Do not use plain SHA-256 alone for passwords.
```

Encryption:

```text
Use vetted libraries and standard algorithms. Do not invent your own crypto.
```

Strong answer:

```text
Passwords should be hashed with a slow salted password-hashing algorithm. Sensitive data
that must be recovered should be encrypted with managed keys.
```

---

## 23. TLS And Certificates

Java services commonly use:

- Truststore: certificates trusted by client/server.
- Keystore: private key and certificate for identity.
- TLS handshake: verifies identity and negotiates secure channel.

Common failures:

- Certificate expired.
- Missing CA in truststore.
- Hostname mismatch.
- Wrong keystore password.
- TLS version/cipher mismatch.

Interview line:

```text
TLS issues are often truststore, hostname, expiry, or environment configuration problems,
not application logic bugs.
```

---

## 24. Supply-Chain Hygiene

Production dependency practices:

- Pin versions.
- Use dependency lockfiles where available.
- Scan CVEs.
- Remove unused dependencies.
- Avoid random abandoned libraries.
- Check transitive dependency tree.
- Use internal artifact repositories if company requires.
- Keep base images patched.

Strong answer:

```text
Dependency management is a security and reliability responsibility. I inspect dependency
trees, align versions, scan vulnerabilities, and avoid unnecessary libraries.
```

---

## 25. Mini Program: JUnit + Boundary Testing

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingValidatorTest {
    @Test
    void rejectsCheckoutBeforeCheckin() {
        BookingValidator validator = new BookingValidator();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("2026-06-20", "2026-06-19")
        );

        assertEquals("checkout must be after checkin", exception.getMessage());
    }
}
```

Why it matters:

```text
Strong Java tests cover boundary and failure behavior, not just happy paths.
```

---

## 26. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Works in IDE but not terminal | Classpath/build mismatch | Run build from command line/CI |
| Ignoring dependency tree | Hidden conflicts | Inspect Maven/Gradle dependencies |
| Using latest dependency blindly | Breakage/security surprises | Pin, test, review changelog |
| Writing naive benchmarks | JIT/GC noise misleads | Use JMH |
| Mocking everything | Brittle tests | Mix unit and integration tests |
| Deserializing untrusted data | Security risk | Use safe formats and validation |
| Using Random for tokens | Predictable | Use SecureRandom |
| Logging secrets | Data exposure | Redact/mask |
| Native Image without compatibility check | Runtime failures | Test reflection/resources/proxies |
| Overusing modules in legacy app | Migration friction | Adopt JPMS intentionally |

---

## 27. FAANG-Level Question

Question:

> A Java service works locally but fails in production with NoClassDefFoundError. How do you debug?

Strong answer:

```text
I would first identify the missing class and whether it belongs to our code, a direct
dependency, or a transitive dependency. Then I would compare compile-time and runtime
classpath, inspect the Maven or Gradle dependency tree, check packaging exclusions, container
image contents, and version conflicts. If the class exists but initialization failed, I would
look for the earlier root-cause exception in logs. The fix may be adding the missing runtime
dependency, aligning versions, fixing shading/packaging, or correcting deployment artifacts.
```

---

## 28. Rapid Revision

Must-say lines:

```text
Classpath is legacy class/resource lookup; module path adds explicit module boundaries.
```

```text
Maven and Gradle solve build lifecycle and dependency management.
```

```text
Dependency conflicts are debugged with dependency trees and version alignment.
```

```text
JUnit tests behavior; Mockito isolates dependencies; Testcontainers tests real integrations.
```

```text
Use JMH for Java microbenchmarks because JVM warm-up and JIT can fool naive timing.
```

```text
GraalVM Native Image improves startup and memory but needs compatibility checks for dynamic features.
```

```text
Use SecureRandom for security tokens and never deserialize untrusted data.
```

---

## 29. Official Source Notes

Use official sources when refreshing:

- Java specifications and API docs: `https://docs.oracle.com/javase/specs/`
- OpenJDK JMH: `https://openjdk.org/projects/code-tools/jmh/`
- JUnit User Guide: `https://docs.junit.org/`
- GraalVM Native Image docs: `https://www.graalvm.org/latest/reference-manual/native-image/`
- JDK GA/EA status: `https://jdk.java.net/`

Interview safety line:

```text
Platform topics change with tool versions. I explain the principles, then verify exact
syntax, plugin version, and runtime behavior from official docs or the project build.
```
