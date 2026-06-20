# Spring Boot Internals AutoConfiguration Proxies Transactions Platinum Sheet

Target: intermediate, senior, and MAANG-level Spring Boot interviews.

This sheet explains what happens behind the annotations: startup, auto-configuration, bean
creation, proxying, AOP, and transaction behavior.

---

## 0. Why This Sheet Exists

Starter candidates say:

```text
I used @SpringBootApplication and @Autowired.
```

Senior candidates say:

```text
Spring Boot starts an ApplicationContext, registers bean definitions, applies conditional
auto-configuration from the classpath/properties, creates beans, wraps eligible beans with
proxies, and uses those proxies for cross-cutting behavior such as transactions, security,
caching, and async execution.
```

---

# 1. Spring Boot Startup Flow

High-level flow:

```text
main()
  -> SpringApplication.run()
  -> prepare environment
  -> create ApplicationContext
  -> load bean definitions
  -> apply auto-configuration
  -> run BeanFactoryPostProcessors
  -> create singleton beans
  -> apply BeanPostProcessors
  -> start embedded server
  -> publish application events
  -> run runners
```

Interview answer:

```text
Spring Boot is not magic. It is convention plus conditional configuration over the Spring
container.
```

---

# 2. `@SpringBootApplication`

It combines:

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

Meaning:

| Annotation | Role |
|---|---|
| `@SpringBootConfiguration` | marks primary configuration class |
| `@EnableAutoConfiguration` | imports Boot auto-configurations |
| `@ComponentScan` | scans components from current package downward |

Common trap:

```text
Putting main class in a nested package can accidentally exclude controllers/services from
component scanning.
```

---

# 3. Auto-Configuration

Auto-configuration creates default beans based on:

- classpath
- existing beans
- properties
- web application type
- environment

Examples:

| Condition | Result |
|---|---|
| web starter present | embedded web server and MVC/WebFlux setup |
| DataSource properties present | DataSource bean |
| JPA present | EntityManagerFactory and transaction manager |
| Actuator present | management endpoints |

Important:

```text
Auto-configuration backs off when you define your own bean of the expected type/name.
```

Debugging:

- run with `--debug`
- inspect condition evaluation report
- use Actuator `conditions` endpoint when exposed safely

---

# 4. Conditional Annotations

Common Boot conditions:

| Condition | Meaning |
|---|---|
| `@ConditionalOnClass` | class exists on classpath |
| `@ConditionalOnMissingBean` | bean is absent, so default can be created |
| `@ConditionalOnBean` | bean exists |
| `@ConditionalOnProperty` | property has expected value |
| `@ConditionalOnWebApplication` | app is web app |

Example:

```java
@Configuration
class CacheConfig {
    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    CacheClient cacheClient() {
        return new CacheClient();
    }
}
```

Strong answer:

```text
When a bean is missing, I check classpath, properties, existing beans, package scanning, and
the condition report.
```

---

# 5. Bean Lifecycle

Simplified singleton lifecycle:

```text
instantiate
  -> populate dependencies
  -> aware callbacks
  -> BeanPostProcessor before init
  -> @PostConstruct / InitializingBean
  -> BeanPostProcessor after init
  -> ready for use
  -> @PreDestroy / DisposableBean on shutdown
```

Why it matters:

- proxies are often applied by BeanPostProcessors
- lifecycle hooks can run before app is fully ready
- heavy work in startup can delay readiness
- circular dependencies fail during creation

---

# 6. Dependency Injection Resolution

When Spring sees:

```java
public OrderService(PaymentClient paymentClient) {
    this.paymentClient = paymentClient;
}
```

It resolves by:

1. type
2. qualifier
3. primary bean
4. parameter/field name fallback in some cases

If multiple beans exist:

```java
@Primary
@Bean
PaymentClient defaultPaymentClient() { ... }

@Bean
@Qualifier("mockPaymentClient")
PaymentClient mockPaymentClient() { ... }
```

Best practice:

```text
Prefer constructor injection because dependencies become explicit, immutable, and testable.
```

---

# 7. Proxies

Spring often adds behavior by wrapping your bean in a proxy.

Used by:

- `@Transactional`
- `@Cacheable`
- `@Async`
- `@Scheduled` infrastructure
- method security
- AOP aspects

Mental model:

```text
caller -> proxy -> advice/interceptor -> target method
```

If call bypasses proxy, advice does not run.

---

# 8. JDK Proxy vs CGLIB/Class Proxy

| Proxy Type | Works With | Notes |
|---|---|---|
| JDK dynamic proxy | interfaces | proxy implements interface |
| CGLIB/class proxy | concrete classes | subclass-style proxy |

Common traps:

- final classes/methods are hard to proxy with class-based proxying
- private methods are not advised
- self-invocation bypasses proxy
- internal method call does not trigger `@Transactional`, `@Cacheable`, or `@Async`

---

# 9. Self-Invocation Trap

Wrong:

```java
@Service
class BookingService {
    public void checkout() {
        reserveInventory(); // internal call bypasses proxy
    }

    @Transactional
    public void reserveInventory() {
        // transaction may not start if called internally
    }
}
```

Better:

```java
@Service
class BookingFacade {
    private final InventoryService inventoryService;

    BookingFacade(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public void checkout() {
        inventoryService.reserveInventory();
    }
}

@Service
class InventoryService {
    @Transactional
    public void reserveInventory() {
        // goes through proxy
    }
}
```

Strong answer:

```text
Spring AOP is proxy-based in normal Boot apps, so cross-cutting annotations apply when the
method is invoked through the proxy, not through self-invocation.
```

---

# 10. Transaction Internals

When a proxied `@Transactional` method is called:

```text
proxy receives call
  -> transaction interceptor checks metadata
  -> transaction manager begins or joins transaction
  -> target method runs
  -> commit if successful
  -> rollback if rollback rule matches exception
```

Important defaults:

- unchecked exceptions roll back by default
- checked exceptions do not roll back by default unless configured
- transactions are usually applied at service layer
- long external calls inside transactions are dangerous

Example:

```java
@Transactional(rollbackFor = IOException.class)
public void importBookings() throws IOException {
    // checked exception now triggers rollback
}
```

---

# 11. Transaction Propagation Quick Map

| Propagation | Meaning |
|---|---|
| `REQUIRED` | join existing transaction or create one |
| `REQUIRES_NEW` | suspend existing and create new |
| `MANDATORY` | fail if no transaction exists |
| `SUPPORTS` | join if present, otherwise no transaction |
| `NOT_SUPPORTED` | suspend transaction |
| `NEVER` | fail if transaction exists |
| `NESTED` | savepoint-based nested transaction if supported |

Interview trap:

```text
REQUIRES_NEW commits independently. That can be useful for audit logs but dangerous if you
expect it to roll back with the parent transaction.
```

---

# 12. Transaction Isolation Quick Map

| Isolation | Protects Against |
|---|---|
| READ_UNCOMMITTED | weak; dirty reads may be allowed depending database |
| READ_COMMITTED | dirty reads prevented |
| REPEATABLE_READ | repeated row reads stable in many databases |
| SERIALIZABLE | strongest, behaves close to serial execution |

Backend answer:

```text
I do not raise isolation blindly. I first use proper constraints, row locks, optimistic
locking, and short transactions. Higher isolation can reduce concurrency.
```

---

# 13. `@Async` Internals

`@Async` also depends on proxy behavior.

Wrong:

```java
public void placeOrder() {
    sendEmailAsync(); // self-invocation, async may not happen
}

@Async
public void sendEmailAsync() { ... }
```

Better:

- move async method to another bean
- configure executor
- propagate security/MDC context if needed
- handle exceptions

Production rule:

```text
Never rely on default async executor for serious production workloads. Name and size it.
```

---

# 14. `@Cacheable` Internals

`@Cacheable` checks cache before invoking target method.

Flow:

```text
proxy -> compute key -> check cache -> hit returns value -> miss calls target -> store result
```

Traps:

- self-invocation
- weak cache keys
- caching mutable objects
- no eviction plan
- cache stampede
- local cache in multi-instance app

---

# 15. Application Events Internals

Spring events are in-process by default.

Use for:

- decoupling inside one application
- post-commit local actions with `@TransactionalEventListener`
- simple lifecycle hooks

Do not use as replacement for Kafka/RabbitMQ when:

- another service must consume it
- delivery must survive process crash
- replay is needed
- horizontal scale matters

---

# 16. Native Image And AOT Awareness

Modern Spring Boot supports Ahead-of-Time processing and GraalVM native image workflows.

Interview awareness:

```text
Native images can improve startup time and memory profile, but reflection, dynamic proxies,
resource loading, and runtime classpath scanning need special care. I would choose it when
cold start and memory are important, not as a default for every service.
```

---

# 17. Debugging Missing Bean

Symptom:

```text
NoSuchBeanDefinitionException
```

Checklist:

1. Is class annotated or declared with `@Bean`?
2. Is package under component scan root?
3. Is profile active?
4. Did condition fail?
5. Is dependency missing from classpath?
6. Is bean excluded by auto-configuration?
7. Is there a test slice that intentionally loads fewer beans?

---

# 18. Debugging Transaction Not Working

Checklist:

1. Is method public?
2. Is it called through Spring proxy?
3. Is annotation on correct bean?
4. Is transaction manager configured?
5. Is exception unchecked or rollback configured?
6. Is method doing external calls inside transaction?
7. Is database auto-commit behavior understood?

---

# 19. Interview Question

> Explain how Spring Boot auto-configuration and `@Transactional` work internally.

Strong answer:

```text
Spring Boot starts a Spring ApplicationContext and applies auto-configuration based on
classpath, properties, existing beans, and environment conditions. Auto-configuration is
non-invasive: if I define my own bean, Boot backs off in many cases. For @Transactional,
Spring usually creates a proxy around the bean. Calls go through the proxy, the transaction
interceptor starts or joins a transaction, invokes the target method, then commits or rolls
back based on the exception rules. The main traps are self-invocation, private/final methods,
checked exceptions not rolling back by default, and keeping transactions open across slow
external calls.
```

---

# 20. Final Rapid Revision

```text
Boot = Spring container + auto-configuration + production conventions.
Auto-config = classpath + properties + missing beans + conditions.
Proxy = caller -> proxy -> advice -> target.
Self-invocation bypasses proxy.
@Transactional default rollback = runtime exceptions.
Checked exception rollback needs rollbackFor.
Do not keep DB transactions open during remote calls.
Debug missing beans with condition report, profiles, classpath, package scan.
```

---

# 21. Official Source Notes

- Spring Boot reference: https://docs.spring.io/spring-boot/reference/index.html
- Spring Boot auto-configuration: https://docs.spring.io/spring-boot/reference/using/auto-configuration.html
- Spring Framework declarative transactions: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- Spring Boot native images: https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html
