# Spring Boot Configuration, Profiles, and Actuator — Gold Sheet

> Topic: application.yml, @ConfigurationProperties, profiles, externalized config, and Actuator

---

## 1. Intuition

Spring Boot applications need different behavior in different environments: local dev connects to a local DB, staging connects to a staging DB, production connects to the real one. Spring Boot's configuration system lets you express all of this in one codebase — the runtime environment determines which values are active.

Beginner version:

> Spring Boot reads config from files, environment variables, and command-line arguments — later sources override earlier ones.

---

## 2. Definition

- Definition: Spring Boot's externalized configuration system resolves property values from a priority-ordered set of sources and injects them into beans at startup.
- Category: Runtime configuration and environment management.
- Core idea: Same artifact, different config — drives different behavior per environment.

---

## 3. Configuration Property Sources — Priority Order

Spring Boot evaluates property sources from highest to lowest priority:

```
1. Command-line arguments           --server.port=9090
2. OS environment variables         SERVER_PORT=9090
3. application-{profile}.properties/yml
4. application.properties/yml
5. @PropertySource annotations
6. Default values in @Value/@ConfigurationProperties
```

Higher number in the list = lower priority. OS env vars override `application.yml`.

This is how Kubernetes injects secrets: as environment variables that override the application config file.

---

## 4. `application.yml` Structure

```yaml
# application.yml — base config (all environments)
spring:
  application:
    name: payments-service
  datasource:
    url: jdbc:postgresql://localhost:5432/payments
    username: ${DB_USERNAME}           # resolved from env var
    password: ${DB_PASSWORD:default}   # env var with fallback default
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate               # never auto-create in production
    open-in-view: false               # disable OSIV — prevent lazy load traps

server:
  port: 8080
  shutdown: graceful                  # wait for in-flight requests on SIGTERM

logging:
  level:
    com.example: INFO
    org.hibernate.SQL: DEBUG          # only for dev/debug
```

---

## 5. Profile-Specific Configuration

```yaml
# application-local.yml — activated when SPRING_PROFILES_ACTIVE=local
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payments_local
  jpa:
    hibernate:
      ddl-auto: create-drop           # recreate schema on startup in local only

logging:
  level:
    org.hibernate.SQL: DEBUG
```

```yaml
# application-prod.yml — activated when SPRING_PROFILES_ACTIVE=prod
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/payments_prod
  jpa:
    hibernate:
      ddl-auto: validate

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus   # restricted in prod
```

**Activating profiles:**

```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable (preferred in containers)
export SPRING_PROFILES_ACTIVE=prod

# Multiple profiles
SPRING_PROFILES_ACTIVE=prod,metrics
```

---

## 6. `@ConfigurationProperties` — Type-Safe Configuration

```java
// Define a configuration class
@ConfigurationProperties(prefix = "payments")
@Validated
public record PaymentsConfig(
    @NotNull String gatewayUrl,
    @Min(1) @Max(100) int maxRetries,
    @DurationUnit(ChronoUnit.MILLIS) Duration timeout
) {}
```

```yaml
# application.yml
payments:
  gateway-url: https://gateway.example.com/v1
  max-retries: 3
  timeout: 5000ms
```

```java
// Enable in main class or configuration
@SpringBootApplication
@EnableConfigurationProperties(PaymentsConfig.class)
public class PaymentsApplication { ... }

// Inject and use
@Service
public class PaymentGatewayClient {
    private final PaymentsConfig config;

    public PaymentGatewayClient(PaymentsConfig config) {
        this.config = config;
    }
}
```

**Why prefer `@ConfigurationProperties` over `@Value`:**
- Type-safe (compile error on wrong type)
- Validated with Bean Validation
- IDE autocomplete with metadata processor
- Easier to test (just construct the record)
- Groups related config together

---

## 7. Profile-Based Bean Activation

```java
// Bean only created when "local" profile is active
@Configuration
@Profile("local")
public class LocalDatabaseConfig {
    @Bean
    public DataSource dataSource() {
        // embedded H2 for local dev
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}

// Bean active in prod AND staging
@Component
@Profile({"prod", "staging"})
public class SentryErrorReporter { ... }

// Bean active in any profile EXCEPT test
@Component
@Profile("!test")
public class ExternalNotificationService { ... }
```

---

## 8. Spring Boot Actuator

Actuator exposes operational endpoints over HTTP (or JMX).

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,loggers,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized    # prod: hide details from anonymous
      probes:
        enabled: true                 # enables /actuator/health/liveness and /readiness
  server:
    port: 8081                        # serve actuator on separate port (security)
```

**Key Actuator endpoints:**

| Endpoint | URL | Purpose |
|---|---|---|
| Health | `/actuator/health` | Overall UP/DOWN + component details |
| Liveness | `/actuator/health/liveness` | Is the process alive? (Kubernetes liveness probe) |
| Readiness | `/actuator/health/readiness` | Is the app ready for traffic? (K8s readiness probe) |
| Info | `/actuator/info` | App version, git commit, build info |
| Metrics | `/actuator/metrics` | Micrometer metrics |
| Prometheus | `/actuator/prometheus` | Prometheus-scrape format |
| Env | `/actuator/env` | Resolved property sources (sensitive — restrict in prod) |
| Loggers | `/actuator/loggers` | Dynamic log level changes at runtime |

---

## 9. Kubernetes Probes with Actuator

```yaml
# Kubernetes deployment manifest
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

**Liveness vs Readiness:**
- **Liveness**: restart the pod if this fails (app is broken and cannot recover)
- **Readiness**: stop sending traffic if this fails (app is temporarily unavailable — e.g., warming up, dependency outage)

Spring Boot sets readiness to `REFUSING_TRAFFIC` during startup and switches to `ACCEPTING_TRAFFIC` when the ApplicationContext is fully initialized.

---

## 10. Externalized Config for Kubernetes

```yaml
# Kubernetes ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: payments-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SPRING_DATASOURCE_URL: "jdbc:postgresql://db-svc:5432/payments"

# Kubernetes Secret (base64-encoded)
apiVersion: v1
kind: Secret
metadata:
  name: payments-secrets
data:
  DB_USERNAME: <base64>
  DB_PASSWORD: <base64>
```

```yaml
# deployment.yaml — inject as environment variables
env:
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: payments-config
        key: SPRING_PROFILES_ACTIVE
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: payments-secrets
        key: DB_PASSWORD
```

Spring Boot's property binding converts `SPRING_DATASOURCE_URL` → `spring.datasource.url` automatically (relaxed binding).

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Hardcoding secrets in `application.yml` | Secrets in source control | Use `${SECRET_NAME}` env var references |
| `ddl-auto: create-drop` in production | Database schema wiped on deploy | Set to `validate` in prod; use Flyway for migrations |
| Exposing all Actuator endpoints in prod | Security risk (env reveals secrets) | Restrict `include` and run Actuator on internal port |
| Using `@Value("${property}")` for complex config | Brittle, no validation | Use `@ConfigurationProperties` |
| Profiles not set in container | Wrong environment config runs | Always set `SPRING_PROFILES_ACTIVE` in deployment |

---

## 12. Interview Insight

Strong answer:

> Spring Boot's configuration system reads from multiple sources with a defined priority — OS environment variables override `application-{profile}.yml` which overrides `application.yml`. I use `@ConfigurationProperties` for type-safe, validated config grouped by domain. Profile-specific yml files (`application-prod.yml`) handle environment differences like DB URLs and pool sizes. Actuator exposes health, readiness, and metrics endpoints — in Kubernetes I wire the health probes to `/actuator/health/liveness` and `/actuator/health/readiness` on a management port not exposed to internet traffic.

Follow-up trap:

> What is the difference between the liveness and readiness probes and how does Spring Boot handle each?

Good answer:

> Liveness asks "is the JVM alive" — if it fails, Kubernetes restarts the pod. Readiness asks "is the app ready to serve traffic" — if it fails, the pod is removed from the Service endpoints but not restarted. Spring Boot automatically manages readiness state: it's `REFUSING_TRAFFIC` during startup and during graceful shutdown, switching to `ACCEPTING_TRAFFIC` only when the ApplicationContext is fully initialized. This prevents traffic from hitting a pod that hasn't finished connecting to its dependencies.

---

## 13. Revision Notes

- One-line summary: Spring Boot config flows from external sources → profiles → defaults; Actuator exposes operational endpoints for health, metrics, and runtime management.
- Three keywords: profile, relaxed-binding, actuator.
- One interview trap: `@Value` vs `@ConfigurationProperties` — prefer the latter for groups of related config.
- Memory trick: Profiles are costumes — same app, different behavior per environment.
