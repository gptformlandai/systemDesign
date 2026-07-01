# 06. IntelliJ Java: Spring Boot HotSwap, LiveReload, Actuator Debug

## Goal

Debug Spring Boot applications with fast iteration — hot swap class changes without restarting, use actuator endpoints to inspect live state, and debug profile-specific configurations.

---

## Spring Boot Debug Run (Basic)

```text
IntelliJ Run/Debug Configuration -> Spring Boot
  Click "Debug" (bug icon) not "Run" (play icon)
  -> Starts with the IntelliJ-managed JDWP agent automatically
  -> All breakpoints in source are immediately active
```

Or from terminal with Maven/Gradle:

```bash
./gradlew bootRun --debug-jvm        # waits for debugger on port 5005
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

---

## HotSwap — Change Code Without Restarting

### Built-in JVM HotSwap (Limited)

When paused in the debugger, IntelliJ can swap changed method bodies into the running JVM:

```text
1. App is running in debug mode.
2. Edit a method body (change logic, not signature).
3. Build (Cmd+F9 or Build -> Recompile File).
4. IntelliJ prompt: "Reload Changed Classes?"
5. Click Yes.
6. The running JVM now executes the new method body.
```

Limitations of built-in HotSwap:
- Can only change method bodies (no new methods, fields, or classes)
- Cannot change class structure, annotations, or Spring bean wiring

### Spring DevTools — Automatic Restart

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

```gradle
// build.gradle
developmentOnly("org.springframework.boot:spring-boot-devtools")
```

Behavior:

```text
1. You save a file (or IntelliJ recompiles it).
2. DevTools detects the change in the classpath.
3. Spring Boot context restarts (fast restart, not full JVM restart).
4. Application is live again in ~1-2 seconds instead of 10-30 seconds.
```

DevTools uses two classloaders:
- Base classloader: JDK + third-party libraries (does not reload)
- Restart classloader: your application code (reloads on change)

The result is much faster than a cold start.

### Excluding Paths From DevTools Restart

```properties
# application.properties
spring.devtools.restart.exclude=static/**,public/**,templates/**
```

---

## DCEVM + HotSwapAgent (Full HotSwap)

For full hot swap including new methods and fields:

```bash
# 1. Install DCEVM patch for your JDK.
# https://github.com/TravaOpenJDK/trava-jdk-11-dcevm

# 2. Add HotSwapAgent jar.
-javaagent:/path/to/hotswap-agent.jar

# 3. JVM args:
-XX:+AllowEnhancedClassRedefinition \
-javaagent:/path/to/hotswap-agent.jar
```

HotSwapAgent supports reloading:
- New and modified methods
- New and modified fields
- Spring bean configuration changes
- Mapper and template changes (Thymeleaf, etc.)

---

## Spring Boot Actuator Debug Endpoints

Actuator provides live introspection of a running Spring Boot application.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
```

### Key Debug Endpoints

```bash
# Health check with component details.
curl http://localhost:8080/actuator/health

# List all beans in the Spring context (huge output).
curl http://localhost:8080/actuator/beans | python3 -m json.tool | grep "bean_name"

# Current environment properties (shows active profile, property sources).
curl http://localhost:8080/actuator/env

# Thread dump.
curl http://localhost:8080/actuator/threaddump

# Heap dump (saves .hprof file or streams binary).
curl http://localhost:8080/actuator/heapdump -o heap.hprof

# Request mappings (which URL maps to which controller method).
curl http://localhost:8080/actuator/mappings

# Metrics.
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Logger levels (read and change at runtime).
curl http://localhost:8080/actuator/loggers/com.example.orders
# Response: {"configuredLevel":null,"effectiveLevel":"INFO"}

# Change log level at runtime (POST).
curl -X POST http://localhost:8080/actuator/loggers/com.example.orders \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel":"DEBUG"}'
```

The log level change at runtime is extremely useful in production — temporarily enable DEBUG logging for one package without restarting.

---

## Profile-Specific Debug Configuration

```properties
# application-dev.properties
logging.level.com.example.orders=DEBUG
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
management.endpoints.web.exposure.include=*
```

```bash
# Activate dev profile.
java -jar order-service.jar --spring.profiles.active=dev
```

In IntelliJ Run Configuration:

```text
Spring Boot config -> Active profiles: dev
OR
VM options: -Dspring.profiles.active=dev
```

---

## Debugging Conditional Beans

```java
@Configuration
public class FeatureConfig {
    
    @Bean
    @ConditionalOnProperty(name = "feature.new-pricing", havingValue = "true")
    public PricingService newPricingService() {
        return new NewPricingService();
    }
    
    @Bean
    @ConditionalOnMissingBean(PricingService.class)
    public PricingService legacyPricingService() {
        return new LegacyPricingService();
    }
}
```

Debug which bean is active:

```bash
# Check which beans are in context.
curl http://localhost:8080/actuator/beans | grep -i pricingservice

# Or check conditions report.
curl http://localhost:8080/actuator/conditions
# Shows: which @Conditional evaluated to true/false and why.
```

In IntelliJ, set a breakpoint inside both `newPricingService()` and `legacyPricingService()` to see which one is called during startup.

---

## DevTools LiveReload For Frontend Changes

```properties
# DevTools includes a LiveReload server.
spring.devtools.livereload.enabled=true
# Browser extension at livereload.com auto-reloads page when server restarts.
```

---

## Interview Sound Bite

Spring Boot debug run starts the JDWP agent automatically. Built-in JVM HotSwap reloads changed method bodies while the debugger is attached, without restart. Spring DevTools adds a faster restart classloader that restarts the Spring context (not the JVM) in ~1-2 seconds when classpath changes. Actuator endpoints are essential for live production debugging: `/actuator/threaddump` shows all JVM threads, `/actuator/heapdump` captures heap for analysis, and `/actuator/loggers` changes log levels at runtime without restart. `/actuator/conditions` explains why a @ConditionalOnProperty bean was included or excluded.
