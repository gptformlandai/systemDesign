# Spring Boot Install, Initializr, Maven, Gradle, And First App Gold Sheet

> Track: Spring Boot Interview Track - Setup Layer  
> Goal: take a beginner from zero local setup to a clean, testable first Spring Boot service.

---

## 1. Intuition

Spring Boot setup is the workshop before the engineering starts. The JDK is the engine,
Maven or Gradle is the assembly line, Spring Initializr creates the blueprint, and the
first controller proves the machine can actually run.

Beginner version:

- Install a supported JDK.
- Generate a project with Spring Initializr.
- Run it with the wrapper command.
- Add one endpoint, one test, and one configuration file.

---

## 2. Definition

- Definition: Spring Boot project setup is the process of choosing a supported Java and
  Spring Boot version, generating a standard project layout, managing dependencies with
  Maven or Gradle, and running a minimal application locally and in tests.
- Category: Developer environment, build tooling, application bootstrap.
- Core idea: repeatable setup removes tool drift and lets every developer and CI runner
  build the same application the same way.

---

## 3. Why It Exists

Without a repeatable setup, teams lose time to:

- different local Java versions
- dependency version conflicts
- Maven or Gradle version drift
- unclear starter choices
- tests that pass in one IDE but fail in CI
- production bugs caused by missing drivers, wrong profiles, or package layout mistakes

Spring Boot exists partly to make the starting point boring: embedded server, opinionated
defaults, auto-configuration, dependency management, and production-ready hooks.

---

## 4. Reality

Real teams use this setup flow for:

- REST APIs
- backend-for-frontend services
- batch jobs
- Kafka consumers
- internal platform services
- cloud-native microservices
- monoliths that need clean modular boundaries

Common company baseline:

```text
JDK pinned -> wrapper pinned -> Spring Boot version pinned -> CI uses wrapper -> tests run
```

---

## 5. How It Works

1. Pick Java and Spring Boot versions.
2. Generate a project from Spring Initializr.
3. Choose Maven or Gradle.
4. Add starters instead of manually listing every Spring dependency.
5. Run through `./mvnw spring-boot:run` or `./gradlew bootRun`.
6. Spring Boot starts the `ApplicationContext`.
7. Auto-configuration detects classpath and properties.
8. Embedded Tomcat/Jetty/Netty starts if it is a web app.
9. Tests run against the smallest useful Spring test slice.
10. CI uses the wrapper, not the developer's globally installed build tool.

Failure path:

- wrong Java version -> build or startup fails
- missing starter -> bean or class not found
- wrong package layout -> components are not scanned
- wrong profile -> app connects to the wrong database
- missing runtime dependency -> executable JAR starts locally but fails in container

Recovery path:

- check `java -version`
- run wrapper command from repo root
- check `pom.xml` or `build.gradle`
- check `@SpringBootApplication` package root
- inspect startup logs and condition report
- run focused tests

---

## 6. What Problem It Solves

- Primary problem solved: repeatable creation and execution of Spring Boot apps.
- Secondary benefits: predictable dependency versions, local-to-CI parity, faster onboarding.
- Systems impact: fewer environment bugs, safer upgrades, cleaner production builds.

---

## 7. When To Rely On It

Use this setup path when:

- starting any new Spring Boot service
- onboarding into an existing Spring Boot repo
- preparing for interviews where you may need to sketch a project from scratch
- building a portfolio reference project
- standardizing CI for a Java backend team

Interviewer triggers:

- "How do you start a new Spring Boot project?"
- "What is a starter?"
- "How do you keep builds reproducible?"
- "Why use the Maven/Gradle wrapper?"
- "How do you structure packages?"

---

## 8. When Not To Use It

Avoid blindly generating a default app when:

- your company has a platform template with security, logging, and CI already baked in
- the app is a library, not a bootable service
- the workload is a tiny CLI where Spring Boot startup is unnecessary
- the team needs a non-JVM runtime for latency, deployment, or ecosystem reasons

Use instead:

- internal service template
- plain Maven/Gradle Java library
- Spring Shell or Picocli for CLI workloads
- another runtime if the platform standard requires it

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Fast project generation | Easy to add too many starters |
| Managed dependency versions | Version upgrades still require testing |
| Embedded runtime | Larger artifact than a minimal Java app |
| Wrapper improves reproducibility | Wrapper files must be committed and reviewed |
| Standard layout helps teams | Beginners may treat auto-config as magic |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Gain: speed, convention, dependency alignment.
- Give up: some low-level control compared with hand-wiring the runtime.
- Latency: Boot startup is acceptable for services, but may be heavy for tiny tools.
- Complexity: simpler application setup, but hidden auto-configuration must be understood.
- Cost: build and runtime image size depend on dependency discipline.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Using globally installed Maven in CI | CI may differ from local builds | Use `./mvnw` |
| Putting application class in a nested package | Component scanning misses sibling packages | Put it at the root package |
| Adding both Web MVC and WebFlux accidentally | Runtime model becomes confusing | Choose intentionally |
| Exposing entities from the first controller | API becomes coupled to database shape | Use DTOs |
| Starting with `@SpringBootTest` for every test | Slow, brittle test suite | Use unit, slice, and integration tests |
| Committing secrets in `application.yml` | Credential leak | Use env vars, secret managers, or local ignored files |

---

## 11. Key Numbers

Current platform facts to know:

- Modern Spring Boot requires a supported Java baseline; Spring Boot 4.1.0 requires Java 17+.
- Maven wrapper is commonly committed as `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/...`.
- Gradle wrapper is commonly committed as `gradlew`, `gradlew.bat`, and `gradle/wrapper/...`.
- A small Boot REST API can usually start locally in seconds, but startup depends on classpath,
  database connections, migrations, and test containers.
- For a beginner project, keep dependencies under control: web, validation, actuator, test,
  and one data/security starter at a time.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| Build fails before compile | Maven/Gradle error | Wrong JDK or wrapper | Pin JDK and wrapper |
| App starts but endpoint 404s | No route found | Controller outside scan root | Fix package layout |
| App fails at startup | Bean missing | Missing starter or condition mismatch | Check condition report |
| Tests are slow | CI timeout | Too many full context tests | Use slices |
| Local profile hits prod DB | Dangerous data access | Misconfigured profile/secrets | Use safe local defaults |
| Container fails | Classpath/runtime missing | Wrong dependency scope | Verify executable artifact |

---

## 13. Scenario

- Product/system: hotel booking service.
- Why this concept fits: every future lesson needs a clean service skeleton with REST,
  persistence, validation, tests, and Actuator.
- What would go wrong without it: the learner spends time fighting tools instead of learning
  Spring behavior, and interview answers stay abstract.

---

## 14. First Maven Project Shape

Typical generated layout:

```text
booking-service/
  pom.xml
  mvnw
  .mvn/wrapper/
  src/main/java/com/example/booking/BookingApplication.java
  src/main/java/com/example/booking/api/
  src/main/java/com/example/booking/service/
  src/main/java/com/example/booking/repository/
  src/main/resources/application.yml
  src/test/java/com/example/booking/BookingApplicationTests.java
```

Minimal `pom.xml` shape:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>booking-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <java.version>21</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Note: Spring Boot starter artifact names can change across major versions. Use Spring
Initializr or the official dependency metadata when generating a real project.

---

## 15. First Controller And Test

```java
package com.example.booking.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
class BookingController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse create(@RequestBody CreateBookingRequest request) {
        return new BookingResponse("booking-123", request.hotelId());
    }

    record CreateBookingRequest(@NotBlank String hotelId) {
    }

    record BookingResponse(String bookingId, String hotelId) {
    }
}
```

```java
package com.example.booking.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createsBooking() throws Exception {
        mvc.perform(post("/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hotelId\":\"H100\"}"))
            .andExpect(status().isCreated());
    }
}
```

---

## 16. Practical Question

> You joined a team with no Spring Boot template. How would you create the first service
> so local development, tests, and CI stay reproducible?

---

## 17. Strong Answer

I would start with Spring Initializr or the company template, choose the current approved
Spring Boot and Java baseline, and commit the Maven or Gradle wrapper so CI and developers
use the same build tool version. I would keep dependencies minimal: web, validation,
actuator, test, and only the data/security starters we need. The main application class
goes in the root package so component scanning works naturally. I would add a thin
controller, DTOs, a service boundary, `application.yml`, one slice test, and a CI command
that runs the wrapper `verify` task. I would avoid secrets in config files and use profiles
only for environment differences, not for business logic.

---

## 18. Revision Notes

- One-line summary: a good Spring Boot setup pins Java, wrapper, Boot version, package
  layout, minimal starters, config, and tests.
- Three keywords: wrapper, starter, package root.
- One interview trap: Spring Boot auto-configuration is helpful, but you still must know
  what is on the classpath and which conditions matched.
- One memory trick: JDK starts Java, wrapper builds it, Initializr shapes it, Boot runs it.

