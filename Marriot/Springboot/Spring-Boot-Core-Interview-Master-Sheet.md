# Spring Boot Core Interview Master Sheet

Target: Marriott Tech Accelerator / Java Backend / Intervue round.

This sheet covers Spring Core + Spring Boot fundamentals that are repeatedly asked in interviews:
- IoC and DI
- `@Autowired` internals
- Constructor, setter, and field injection
- Bean creation and bean lifecycle
- Bean scopes and bean types
- Component scanning and stereotype annotations
- `ApplicationContext`, `BeanFactory`, auto-configuration
- Profiles and external configuration
- Circular dependencies
- AOP and proxies
- `@Transactional`
- Spring MVC request flow
- Exception handling, validation, filters/interceptors
- Hot interview questions, traps, and rapid revision

Goal:

```text
After reading this sheet, you should be able to explain how Spring creates objects,
wires dependencies, applies proxies, starts a Boot app, handles requests, and manages
transactions in clear interview language.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| IoC | Very high | Foundation of Spring |
| Dependency Injection | Very high | Everyday Spring development |
| Constructor vs setter vs field injection | Very high | Tests design maturity |
| `@Autowired` resolution | Very high | Common tricky area |
| Bean lifecycle | Very high | Core Spring understanding |
| Bean scopes | High | Singleton/prototype/request scope clarity |
| `ApplicationContext` vs `BeanFactory` | High | Container fundamentals |
| `@Component` vs `@Bean` | Very high | Bean creation methods |
| `@Component`, `@Service`, `@Repository`, `@Controller` | Very high | Stereotype annotations |
| `@SpringBootApplication` | Very high | Boot startup basics |
| Auto-configuration | Very high | Main Spring Boot magic |
| Starters | High | Dependency simplification |
| Profiles | High | Environment-specific config |
| `@Value` vs `@ConfigurationProperties` | High | Config binding |
| Circular dependency | High | Practical troubleshooting |
| AOP/proxy | Very high | Transactions, caching, security |
| `@Transactional` | Very high | Backend must-know |
| DispatcherServlet request flow | High | REST API internals |
| Filters vs Interceptors | Medium-high | Web pipeline |
| Validation and exception handling | High | API quality |
| Actuator | Medium | Production readiness |

---

## 2. Spring Core vs Spring Boot vs Spring MVC

### Spring Core

Spring Core provides the foundation:
- IoC container
- Dependency injection
- Bean lifecycle
- AOP
- Resource abstraction
- Events

### Spring MVC

Spring MVC is the web framework:
- `DispatcherServlet`
- Controllers
- Request mapping
- Model binding
- Validation
- Exception handling

### Spring Boot

Spring Boot makes Spring easier to use:
- Auto-configuration
- Embedded server
- Starter dependencies
- Externalized configuration
- Actuator
- Opinionated defaults

### Strong Interview Answer

```text
Spring Core provides IoC and dependency injection. Spring MVC provides the web layer for
handling HTTP requests. Spring Boot sits on top of Spring and reduces setup using
auto-configuration, starters, embedded servers, and production-ready features like Actuator.
```

### Common Trap

Wrong:

```text
Spring Boot replaced Spring.
```

Correct:

```text
Spring Boot does not replace Spring. It builds on Spring and makes configuration and
application startup easier.
```

---

## 3. IoC - Inversion Of Control

### Definition

IoC means object creation and dependency management are controlled by the Spring container, not manually by application code.

### Without IoC

```java
class BookingService {
    private final PaymentService paymentService = new PaymentService();
}
```

Problem:
- `BookingService` is tightly coupled to `PaymentService`.
- Hard to test.
- Hard to replace implementation.

### With IoC

```java
@Service
class BookingService {
    private final PaymentService paymentService;

    BookingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Spring creates both objects and injects dependency.

### Mental Model

Without IoC:

```text
Your code creates objects.
```

With IoC:

```text
Spring creates objects and gives them to your code.
```

### Strong Interview Answer

```text
Inversion of Control means the responsibility of creating and managing objects is inverted
from application code to the Spring container. Instead of a class creating its dependencies
using new, Spring creates beans and injects dependencies where needed.
```

### Why IoC Exists

- Loose coupling
- Easier testing
- Easier configuration
- Centralized lifecycle management
- Better separation of concerns

### Hot Questions

| Question | Strong Answer |
|---|---|
| What is IoC? | Container controls object creation and wiring |
| What problem does it solve? | Tight coupling and manual dependency management |
| Is DI same as IoC? | DI is one way to implement IoC |
| Who is the container? | Spring `ApplicationContext` / `BeanFactory` |

---

## 4. Dependency Injection

### Definition

Dependency Injection means required dependencies are provided from outside the class instead of the class creating them itself.

### Types Of DI

| Type | How |
|---|---|
| Constructor injection | Dependencies passed through constructor |
| Setter injection | Dependencies passed through setter methods |
| Field injection | Dependencies injected directly into fields |

---

## 5. Constructor Injection

### Example

```java
@Service
class BookingService {
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    BookingService(PaymentService paymentService,
                   NotificationService notificationService) {
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }
}
```

### Why Constructor Injection Is Preferred

- Makes dependencies explicit.
- Supports immutability with `final`.
- Object cannot be created in invalid state.
- Easier unit testing.
- Works better for required dependencies.
- Avoids hidden dependencies.

### Do We Need `@Autowired` On Constructor?

If there is only one constructor, Spring can inject it without `@Autowired`.

```java
@Service
class BookingService {
    private final PaymentService paymentService;

    BookingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Strong Interview Answer

```text
I prefer constructor injection for mandatory dependencies because it makes dependencies
explicit, allows final fields, improves testability, and ensures the object is fully
initialized when created.
```

---

## 6. Setter Injection

### Example

```java
@Service
class ReportService {
    private AuditService auditService;

    @Autowired
    public void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }
}
```

### When To Use

- Optional dependencies.
- Reconfigurable dependencies.
- Legacy code.
- Avoiding constructor with too many optional dependencies.

### Drawbacks

- Object can exist in partially initialized state.
- Dependencies are less obvious.
- Cannot use `final`.

### Strong Interview Answer

```text
Setter injection is useful for optional dependencies, but for required dependencies I prefer
constructor injection because it prevents partially initialized objects.
```

---

## 7. Field Injection

### Example

```java
@Service
class BookingService {
    @Autowired
    private PaymentService paymentService;
}
```

### Why It Is Common

- Very concise.
- Easy to write.
- Common in old examples/tutorials.

### Why It Is Not Preferred

- Hidden dependencies.
- Harder to unit test without Spring/reflection.
- Cannot make field `final`.
- Object can be constructed without required dependencies.
- Encourages too many dependencies.

### Strong Interview Answer

```text
Field injection works, but I avoid it in production code for required dependencies because
it hides dependencies and makes unit testing harder. Constructor injection is generally the
better default.
```

### Interview Trap

If interviewer asks:

```text
Which injection is best?
```

Do not say:

```text
Field injection because it is easy.
```

Say:

```text
Constructor injection for mandatory dependencies, setter injection for optional dependencies,
and field injection only rarely or in tests/legacy code.
```

---

## 8. How `@Autowired` Works

### What `@Autowired` Does

`@Autowired` tells Spring:

```text
Find a bean from the container and inject it here.
```

It can be used on:
- Constructor
- Setter
- Field
- Method

### Resolution Steps

When Spring sees a dependency:

```text
1. Identify required type.
2. Search ApplicationContext for beans of that type.
3. If exactly one bean exists, inject it.
4. If multiple beans exist, check @Primary.
5. If @Qualifier is present, use qualifier.
6. If still ambiguous, try bean name matching.
7. If no bean found and dependency is required, fail startup.
```

### Example: Single Bean

```java
interface PaymentService {
    void pay();
}

@Service
class CardPaymentService implements PaymentService {
    public void pay() {
        System.out.println("Card payment");
    }
}

@Service
class BookingService {
    private final PaymentService paymentService;

    BookingService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Spring finds one `PaymentService` implementation and injects it.

### Multiple Beans Problem

```java
@Service
class CardPaymentService implements PaymentService {
}

@Service
class UpiPaymentService implements PaymentService {
}
```

This fails:

```java
BookingService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

Error:

```text
NoUniqueBeanDefinitionException
```

### Fix 1: `@Primary`

```java
@Primary
@Service
class CardPaymentService implements PaymentService {
}
```

### Fix 2: `@Qualifier`

```java
@Service
class BookingService {
    private final PaymentService paymentService;

    BookingService(@Qualifier("upiPaymentService") PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

### Fix 3: Inject All Implementations

```java
@Service
class PaymentRouter {
    private final Map<String, PaymentService> paymentServices;

    PaymentRouter(Map<String, PaymentService> paymentServices) {
        this.paymentServices = paymentServices;
    }
}
```

Spring injects beans by bean name:

```text
cardPaymentService -> CardPaymentService
upiPaymentService  -> UpiPaymentService
```

### Optional Dependency

```java
@Autowired(required = false)
private AuditService auditService;
```

Better:

```java
BookingService(Optional<AuditService> auditService) {
    this.auditService = auditService;
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Does autowiring happen by type or name? | Primarily by type, then qualifiers/name help resolve ambiguity |
| What if two beans of same type exist? | Use `@Primary` or `@Qualifier` |
| What if no bean exists? | Startup fails unless dependency is optional |
| Can we inject List/Map of beans? | Yes |
| Is `@Autowired` required on single constructor? | No |

---

## 9. Bean Basics

### What Is A Bean?

A bean is an object managed by the Spring container.

Spring controls:
- Creation
- Dependency injection
- Lifecycle callbacks
- Scope
- Destruction
- Proxy wrapping if needed

### Strong Interview Answer

```text
A Spring bean is an object created and managed by the Spring IoC container. The container
creates it, injects dependencies, applies lifecycle callbacks and post-processors, and
manages its scope.
```

### Ways To Create Beans

| Method | Example |
|---|---|
| Stereotype annotation | `@Component`, `@Service`, `@Repository`, `@Controller` |
| Java config | `@Bean` inside `@Configuration` |
| XML config | Old style |
| Programmatic registration | Advanced/framework use |
| Auto-configuration | Spring Boot registers based on classpath/conditions |

### `@Component`

Generic Spring-managed component.

```java
@Component
class TokenGenerator {
}
```

### `@Service`

Business/service layer stereotype.

```java
@Service
class BookingService {
}
```

### `@Repository`

Persistence layer stereotype.

```java
@Repository
class BookingRepository {
}
```

Extra behavior:

```text
@Repository can participate in persistence exception translation.
```

### `@Controller`

Spring MVC controller returning views.

```java
@Controller
class PageController {
}
```

### `@RestController`

Combination of:

```text
@Controller + @ResponseBody
```

Used for REST APIs.

```java
@RestController
class BookingController {
}
```

### `@Bean`

Used when you cannot or should not annotate the class directly.

Example:

```java
@Configuration
class AppConfig {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

Use `@Bean` for:
- Third-party classes
- Library objects
- Configuration objects
- Custom construction logic

### `@Component` vs `@Bean`

| `@Component` | `@Bean` |
|---|---|
| Class-level annotation | Method-level annotation |
| Spring detects via component scan | Explicitly declared in config |
| Best for your own classes | Best for third-party/custom objects |

### Hot Questions

| Question | Strong Answer |
|---|---|
| What is bean? | Object managed by Spring container |
| `@Component` vs `@Service`? | Same registration behavior, different semantic layer meaning |
| `@Repository` special? | Persistence exception translation |
| `@RestController` means? | `@Controller + @ResponseBody` |
| `@Bean` vs `@Component`? | Explicit factory method vs component scanning |

---

## 10. ApplicationContext And BeanFactory

### BeanFactory

Basic IoC container.

Provides:
- Bean creation
- Dependency injection
- Bean lookup

### ApplicationContext

Advanced container built on BeanFactory.

Adds:
- Internationalization
- Events
- Resource loading
- Environment abstraction
- Annotation support
- AOP integration
- Web application features

### Strong Interview Answer

```text
BeanFactory is the basic IoC container. ApplicationContext extends it with enterprise
features like event publishing, resource loading, environment support, and annotation-based
configuration. In Spring Boot applications, we normally work with ApplicationContext.
```

### Bean Lookup

```java
ApplicationContext context = SpringApplication.run(Application.class, args);
BookingService service = context.getBean(BookingService.class);
```

### Interview Trap

Wrong:

```text
ApplicationContext and BeanFactory are unrelated.
```

Correct:

```text
ApplicationContext is a richer container built on top of BeanFactory capabilities.
```

---

## 11. Bean Scopes

### Common Scopes

| Scope | Meaning |
|---|---|
| singleton | One bean instance per Spring container |
| prototype | New instance each time requested from container |
| request | One instance per HTTP request |
| session | One instance per HTTP session |
| application | One instance per ServletContext |
| websocket | One instance per WebSocket session |

### Singleton Scope

Default scope.

```java
@Service
class BookingService {
}
```

Spring creates one instance per application context.

### Prototype Scope

```java
@Component
@Scope("prototype")
class ReportBuilder {
}
```

Spring creates a new instance every time the bean is requested from container.

### Singleton With Prototype Dependency Trap

```java
@Service
class SingletonService {
    private final PrototypeBean prototypeBean;

    SingletonService(PrototypeBean prototypeBean) {
        this.prototypeBean = prototypeBean;
    }
}
```

Problem:

```text
PrototypeBean is injected once when SingletonService is created. It does not become new
on every method call.
```

Fix options:
- Inject `ObjectProvider<PrototypeBean>`
- Use lookup method injection
- Redesign if possible

Example:

```java
@Service
class SingletonService {
    private final ObjectProvider<PrototypeBean> provider;

    SingletonService(ObjectProvider<PrototypeBean> provider) {
        this.provider = provider;
    }

    void process() {
        PrototypeBean bean = provider.getObject();
    }
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Default Spring bean scope? | singleton |
| Spring singleton means? | One instance per Spring container |
| Prototype lifecycle managed fully? | Spring creates/injects, but destruction callback is not managed like singleton |
| Request scope? | One bean per HTTP request |
| Prototype inside singleton issue? | Injected once unless provider/proxy used |

---

## 12. Bean Lifecycle

### High-Level Lifecycle

```text
1. Bean definition discovered
2. Bean instantiated
3. Dependencies populated
4. Aware callbacks
5. BeanPostProcessor before initialization
6. Initialization callbacks
7. BeanPostProcessor after initialization
8. Bean ready to use
9. Destruction callbacks when context closes
```

### Detailed Lifecycle

```text
Class scanning / @Bean registration
    -> BeanDefinition created
    -> Instantiate object
    -> Populate dependencies
    -> BeanNameAware / BeanFactoryAware / ApplicationContextAware
    -> BeanPostProcessor#postProcessBeforeInitialization
    -> @PostConstruct
    -> InitializingBean#afterPropertiesSet
    -> custom initMethod
    -> BeanPostProcessor#postProcessAfterInitialization
    -> Bean ready
    -> @PreDestroy
    -> DisposableBean#destroy
    -> custom destroyMethod
```

### Example

```java
@Component
class LifecycleDemo {
    public LifecycleDemo() {
        System.out.println("Constructor");
    }

    @PostConstruct
    public void init() {
        System.out.println("PostConstruct");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("PreDestroy");
    }
}
```

### BeanPostProcessor

Allows custom logic before/after initialization.

Spring uses BeanPostProcessors for many features:
- `@Autowired`
- AOP proxy creation
- `@PostConstruct`
- Validation

### Important Proxy Point

AOP proxies are often created in:

```text
postProcessAfterInitialization
```

Meaning:

```text
The object you inject may be a proxy wrapping the real bean.
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| When dependencies injected? | After instantiation, before initialization callbacks |
| `@PostConstruct` when runs? | After dependency injection |
| `@PreDestroy` when runs? | Before bean destruction/context close |
| What is BeanPostProcessor? | Hook to modify/wrap beans during lifecycle |
| Where are proxies created? | Often by BeanPostProcessor after initialization |

---

## 13. Bean Definitions And Component Scanning

### What Is BeanDefinition?

BeanDefinition is metadata about a bean.

It contains:
- Bean class
- Scope
- Constructor args
- Properties
- Lazy/eager info
- Init/destroy methods

Spring first collects bean definitions, then creates beans.

### Component Scanning

```java
@SpringBootApplication
public class Application {
}
```

By default, Spring Boot scans from the package of the main application class downward.

Example:

```text
com.company.app.Application
com.company.app.controller.BookingController
com.company.app.service.BookingService
```

These are scanned.

But:

```text
com.company.common.SomeService
```

may not be scanned if outside base package.

### Custom Component Scan

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.company.app", "com.company.common"})
public class Application {
}
```

### Hot Question

```text
Why is my @Service not getting injected?
```

Possible answers:
- Class not under component scan package.
- Missing stereotype annotation.
- Bean condition not matched.
- Multiple beans causing ambiguity.
- Profile not active.
- Class is not public/constructable in expected way.

---

## 14. `@Configuration` And `@Bean`

### `@Configuration`

Marks a class as a source of bean definitions.

```java
@Configuration
class AppConfig {
    @Bean
    PaymentClient paymentClient() {
        return new PaymentClient();
    }
}
```

### Full Mode vs Lite Mode

`@Configuration` classes are proxied by Spring to preserve singleton behavior for `@Bean` method calls.

Example:

```java
@Configuration
class AppConfig {
    @Bean
    ServiceA serviceA() {
        return new ServiceA(serviceB());
    }

    @Bean
    ServiceB serviceB() {
        return new ServiceB();
    }
}
```

Even though `serviceB()` is called directly, Spring proxy ensures singleton bean is returned.

### `proxyBeanMethods = false`

```java
@Configuration(proxyBeanMethods = false)
class AppConfig {
}
```

This avoids proxying and can improve startup performance.

Use when:

```text
@Bean methods do not call each other directly.
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why `@Configuration`? | Declares bean definitions |
| Why proxy config class? | To preserve singleton semantics for inter-bean method calls |
| When use `@Bean`? | Third-party/custom object creation |
| `proxyBeanMethods=false`? | Avoid proxy if no inter-bean method calls |

---

## 15. `@SpringBootApplication`

### What It Contains

`@SpringBootApplication` is a convenience annotation combining:

```text
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan
```

### Example

```java
@SpringBootApplication
public class BookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
```

### What Happens On Startup

```text
1. main method calls SpringApplication.run
2. ApplicationContext is created
3. Environment and properties are loaded
4. Component scanning finds beans
5. Auto-configuration registers conditional beans
6. Beans are created and dependencies injected
7. Embedded server starts
8. Application is ready to handle requests
```

### Hot Question

```text
What does @SpringBootApplication do?
```

Answer:

```text
It marks the main configuration class, enables auto-configuration, and triggers component
scanning from the package where the main class is located.
```

---

## 16. Spring Boot Auto-Configuration

### What Is Auto-Configuration?

Auto-configuration automatically configures beans based on:
- Classpath dependencies
- Existing beans
- Properties
- Conditions

Example:

If Spring Boot sees:

```text
spring-boot-starter-web
```

it configures:
- Embedded Tomcat
- DispatcherServlet
- JSON converter
- MVC infrastructure

### Conditional Annotations

| Annotation | Meaning |
|---|---|
| `@ConditionalOnClass` | Configure if class exists |
| `@ConditionalOnMissingBean` | Configure if bean not already defined |
| `@ConditionalOnProperty` | Configure based on property |
| `@ConditionalOnBean` | Configure if bean exists |
| `@ConditionalOnWebApplication` | Configure for web app |

### Example

```java
@Bean
@ConditionalOnMissingBean
ObjectMapper objectMapper() {
    return new ObjectMapper();
}
```

Meaning:

```text
Create ObjectMapper only if user has not already provided one.
```

### Override Auto-Configuration

Usually define your own bean.

```java
@Bean
ObjectMapper objectMapper() {
    return new ObjectMapper()
        .findAndRegisterModules();
}
```

### Strong Interview Answer

```text
Spring Boot auto-configuration creates sensible default beans based on classpath, properties,
and conditions. It backs off when the application defines its own bean, commonly using
conditions like @ConditionalOnMissingBean.
```

### Starters

Starter dependencies bundle common dependencies.

Examples:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`

Starter means:

```text
Dependency package, not code generator.
```

---

## 17. External Configuration

### Common Config Files

```text
application.properties
application.yml
application-dev.yml
application-prod.yml
```

### Example YAML

```yaml
server:
  port: 8081

booking:
  cancellation-window-hours: 24
```

### `@Value`

```java
@Value("${booking.cancellation-window-hours}")
private int cancellationWindowHours;
```

Good for:
- Simple single values.

### `@ConfigurationProperties`

```java
@ConfigurationProperties(prefix = "booking")
public class BookingProperties {
    private int cancellationWindowHours;
    private String defaultCurrency;

    public int getCancellationWindowHours() {
        return cancellationWindowHours;
    }

    public void setCancellationWindowHours(int cancellationWindowHours) {
        this.cancellationWindowHours = cancellationWindowHours;
    }
}
```

Enable:

```java
@EnableConfigurationProperties(BookingProperties.class)
```

or make it a component depending setup.

### `@Value` vs `@ConfigurationProperties`

| `@Value` | `@ConfigurationProperties` |
|---|---|
| Single value | Grouped config |
| Good for simple cases | Good for structured config |
| SpEL support | Type-safe binding |
| Scattered if overused | Centralized config class |

### Strong Interview Answer

```text
For one-off values, @Value is fine. For related configuration properties, I prefer
@ConfigurationProperties because it is type-safe, grouped, easier to validate, and easier
to test.
```

---

## 18. Profiles

### What Are Profiles?

Profiles let you load environment-specific beans or properties.

Examples:
- dev
- test
- stage
- prod

### Activate Profile

```properties
spring.profiles.active=dev
```

Command line:

```text
--spring.profiles.active=prod
```

### Profile-Specific Files

```text
application-dev.yml
application-prod.yml
```

### `@Profile`

```java
@Bean
@Profile("dev")
PaymentClient mockPaymentClient() {
    return new MockPaymentClient();
}
```

```java
@Bean
@Profile("prod")
PaymentClient realPaymentClient() {
    return new RealPaymentClient();
}
```

### Hot Questions

| Question | Strong Answer |
|---|---|
| Why profiles? | Environment-specific config/beans |
| How activate? | `spring.profiles.active` |
| Can beans be profile-specific? | Yes, with `@Profile` |
| What if no profile active? | Default profile is used |

---

## 19. Circular Dependencies

### What Is Circular Dependency?

```text
Bean A depends on Bean B.
Bean B depends on Bean A.
```

Example:

```java
@Service
class ServiceA {
    ServiceA(ServiceB serviceB) {
    }
}

@Service
class ServiceB {
    ServiceB(ServiceA serviceA) {
    }
}
```

Constructor injection circular dependency fails.

### Why It Is Bad

- Indicates poor design.
- Services are too tightly coupled.
- Hard to test.
- Startup failure.

### Possible Fixes

Best fixes:
- Redesign responsibilities.
- Extract shared logic into third service.
- Use event-based communication.
- Use mediator/orchestrator service.

Technical workarounds:
- Setter injection
- `@Lazy`
- `ObjectProvider`

Example with `@Lazy`:

```java
ServiceA(@Lazy ServiceB serviceB) {
    this.serviceB = serviceB;
}
```

### Strong Interview Answer

```text
I treat circular dependency as a design smell. The best fix is usually to refactor the
responsibilities or introduce a third service. @Lazy or setter injection can break the cycle,
but they are workarounds, not my first choice.
```

---

## 20. Lazy vs Eager Initialization

### Default Behavior

Singleton beans are usually eagerly created during application startup.

### Lazy Bean

```java
@Lazy
@Service
class HeavyReportService {
}
```

Created only when first requested.

### Global Lazy Initialization

```properties
spring.main.lazy-initialization=true
```

### Trade-Off

| Eager | Lazy |
|---|---|
| Slower startup | Faster startup |
| Fails fast | Errors appear later |
| Ready at startup | First request may be slower |

### Strong Answer

```text
Eager initialization catches wiring problems at startup. Lazy initialization can improve
startup time but may move failures to runtime, so I use it carefully.
```

---

## 21. AOP And Proxies

### What Is AOP?

AOP means Aspect-Oriented Programming.

It is used for cross-cutting concerns:
- Transactions
- Logging
- Security
- Caching
- Metrics
- Auditing

### Core Terms

| Term | Meaning |
|---|---|
| Aspect | Cross-cutting module |
| Advice | Code to run before/after/around method |
| Join point | Point in execution, usually method call |
| Pointcut | Expression selecting join points |
| Weaving | Applying aspect to target |
| Proxy | Object wrapping target bean |

### Proxy Example

```text
Controller -> Proxy -> Real Service
```

Proxy can:
- Start transaction
- Call real method
- Commit or rollback

### JDK Dynamic Proxy vs CGLIB

| JDK Dynamic Proxy | CGLIB |
|---|---|
| Interface-based | Class subclass-based |
| Requires interface | Can proxy concrete class |
| Built into JDK | Bytecode subclassing |

### Self-Invocation Trap

```java
@Service
class BookingService {
    public void outer() {
        inner();
    }

    @Transactional
    public void inner() {
        // transaction may not apply if called internally
    }
}
```

Why?

```text
The internal call does not go through the Spring proxy.
```

### Strong Interview Answer

```text
Spring AOP is proxy-based. Features like @Transactional and @Cacheable work when method
calls go through the Spring proxy. Internal self-invocation can bypass the proxy, so the
annotation may not apply.
```

---

## 22. `@Transactional`

### What It Does

`@Transactional` tells Spring to run a method inside a database transaction.

```java
@Transactional
public void createBooking() {
    reserveRoom();
    collectPayment();
    saveBooking();
}
```

### What Happens Internally

```text
1. Caller invokes proxy method.
2. Proxy opens transaction.
3. Real method executes.
4. If success, transaction commits.
5. If exception requiring rollback, transaction rolls back.
```

### Default Rollback Rule

By default, Spring rolls back on:
- RuntimeException
- Error

It does not roll back by default on checked exceptions unless configured.

Example:

```java
@Transactional(rollbackFor = Exception.class)
public void process() throws Exception {
}
```

### Propagation

| Propagation | Meaning |
|---|---|
| REQUIRED | Join existing transaction or create new one |
| REQUIRES_NEW | Suspend existing and create new transaction |
| SUPPORTS | Use transaction if exists |
| MANDATORY | Must have existing transaction |
| NEVER | Fail if transaction exists |
| NOT_SUPPORTED | Run without transaction |
| NESTED | Nested transaction with savepoint if supported |

Most common:

```text
REQUIRED
```

### Isolation

| Isolation | Meaning |
|---|---|
| READ_COMMITTED | Prevent dirty reads |
| REPEATABLE_READ | Prevent dirty and non-repeatable reads |
| SERIALIZABLE | Strongest isolation |

### `readOnly = true`

```java
@Transactional(readOnly = true)
public Booking getBooking(Long id) {
}
```

Purpose:
- Hint to transaction manager/provider.
- Can optimize read-only operations.
- Communicates intent.

### Common Traps

| Trap | Explanation |
|---|---|
| Private transactional method | Proxy cannot intercept private method normally |
| Self-invocation | Internal call bypasses proxy |
| Checked exception rollback | Not rollback by default |
| Method not public | Proxy interception may not work as expected |
| Calling external API inside transaction | Keeps DB transaction open too long |

### Strong Interview Answer

```text
@Transactional is implemented using Spring AOP proxies. The proxy starts and completes the
transaction around the method call. By default, rollback happens for unchecked exceptions.
Self-invocation and private methods are common traps because they may not go through the proxy.
```

---

## 23. Spring MVC Request Flow

### High-Level Flow

```text
Client
  -> Filter chain
  -> DispatcherServlet
  -> HandlerMapping
  -> HandlerAdapter
  -> Controller method
  -> Service
  -> Repository
  -> Response body conversion
  -> Client
```

### DispatcherServlet

Front controller for Spring MVC.

It receives HTTP requests and dispatches them to appropriate controller methods.

### Controller Example

```java
@RestController
@RequestMapping("/bookings")
class BookingController {
    private final BookingService bookingService;

    BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{id}")
    BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }
}
```

### Common REST Annotations

| Annotation | Use |
|---|---|
| `@RestController` | REST controller |
| `@RequestMapping` | Base/general mapping |
| `@GetMapping` | GET endpoint |
| `@PostMapping` | POST endpoint |
| `@PutMapping` | PUT endpoint |
| `@PatchMapping` | PATCH endpoint |
| `@DeleteMapping` | DELETE endpoint |
| `@PathVariable` | URL path value |
| `@RequestParam` | Query parameter |
| `@RequestBody` | JSON body binding |
| `@ResponseStatus` | Set response status |

### Message Conversion

Spring uses `HttpMessageConverter`.

For JSON, commonly Jackson converts:

```text
Java object <-> JSON
```

### Strong Interview Answer

```text
In Spring MVC, DispatcherServlet acts as the front controller. It receives the request,
uses HandlerMapping to find the controller method, invokes it through HandlerAdapter,
then uses message converters like Jackson to convert the response object into JSON.
```

---

## 24. Filter vs Interceptor vs ControllerAdvice

### Filter

Servlet-level.

Runs before request reaches Spring MVC.

Use for:
- Authentication token extraction
- CORS
- Logging
- Request/response wrapping

### Interceptor

Spring MVC-level.

Runs before/after controller invocation.

Use for:
- Request logging
- Authorization checks
- Locale/theme
- Handler-specific logic

### ControllerAdvice

Global controller support.

Use for:
- Exception handling
- Global model attributes
- Response body advice

### Comparison

| Feature | Filter | Interceptor | ControllerAdvice |
|---|---|---|---|
| Layer | Servlet | Spring MVC | Spring MVC exception/binding layer |
| Before DispatcherServlet? | Yes | No |
| Knows controller handler? | No | Yes |
| Common use | Security/logging/CORS | MVC-specific pre/post logic | Global exception handling |

### Interceptor Example

```java
class RequestLoggingInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        System.out.println(request.getRequestURI());
        return true;
    }
}
```

---

## 25. Validation

### Request DTO

```java
class CreateBookingRequest {
    @NotBlank
    private String hotelId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;
}
```

### Controller

```java
@PostMapping
public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
    return bookingService.create(request);
}
```

### Common Validation Annotations

| Annotation | Meaning |
|---|---|
| `@NotNull` | Must not be null |
| `@NotBlank` | String not null and not blank |
| `@NotEmpty` | Collection/string not empty |
| `@Min` | Minimum numeric value |
| `@Max` | Maximum numeric value |
| `@Size` | Size range |
| `@Email` | Valid email format |
| `@Pattern` | Regex match |
| `@Valid` | Trigger nested validation |

### `@NotNull` vs `@NotEmpty` vs `@NotBlank`

| Annotation | Works On | Meaning |
|---|---|---|
| `@NotNull` | Any object | Not null |
| `@NotEmpty` | String/Collection/Array | Not null and size > 0 |
| `@NotBlank` | String | Not null and contains non-whitespace |

### Strong Interview Answer

```text
I validate request DTOs using Bean Validation annotations and @Valid in controller methods.
For business validations that require database or domain checks, I keep that logic in the
service/domain layer.
```

---

## 26. Exception Handling

### Local Exception Handler

```java
@ExceptionHandler(BookingNotFoundException.class)
public ResponseEntity<String> handle(BookingNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
}
```

### Global Exception Handler

```java
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BookingNotFoundException.class)
    ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("BOOKING_NOT_FOUND", ex.getMessage()));
    }
}
```

### Error Response DTO

```java
record ErrorResponse(String code, String message) {
}
```

### Strong Interview Answer

```text
I use @RestControllerAdvice for centralized API exception handling. It keeps controllers
clean and ensures consistent error response structure across endpoints.
```

---

## 27. Spring Data Repository Basics

### Repository Example

```java
interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
}
```

Spring Data generates implementation at runtime.

### Common Repository Interfaces

| Interface | Use |
|---|---|
| Repository | Marker |
| CrudRepository | Basic CRUD |
| PagingAndSortingRepository | Paging/sorting |
| JpaRepository | JPA-specific plus batch/flush methods |

### Derived Query Methods

```java
List<Booking> findByStatusAndCustomerId(String status, Long customerId);
```

### `@Query`

```java
@Query("select b from Booking b where b.customer.id = :customerId")
List<Booking> findBookings(@Param("customerId") Long customerId);
```

### Hot Question

```text
Spring Data creates repository implementations using proxies. We define the interface, and
Spring generates runtime implementation based on method names, @Query, and repository metadata.
```

---

## 28. Actuator

### What Is Actuator?

Spring Boot Actuator provides production-ready endpoints.

Common endpoints:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`
- `/actuator/env`
- `/actuator/beans`

### Example Config

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

### Strong Interview Answer

```text
Actuator exposes operational endpoints for health checks, metrics, info, and diagnostics.
In production, I expose only required endpoints and secure sensitive ones.
```

---

## 29. Common Spring Boot Annotations

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | Main Boot app annotation |
| `@Component` | Generic bean |
| `@Service` | Service layer bean |
| `@Repository` | Persistence layer bean |
| `@Controller` | MVC controller |
| `@RestController` | REST controller |
| `@Autowired` | Dependency injection |
| `@Qualifier` | Choose specific bean |
| `@Primary` | Preferred bean |
| `@Bean` | Declare bean from method |
| `@Configuration` | Configuration class |
| `@Value` | Inject property value |
| `@ConfigurationProperties` | Bind grouped properties |
| `@Profile` | Profile-specific bean/config |
| `@ConditionalOnProperty` | Conditional bean based on property |
| `@Transactional` | Transaction boundary |
| `@ControllerAdvice` | Global MVC advice |
| `@ExceptionHandler` | Exception mapping |
| `@Valid` | Trigger validation |
| `@RequestBody` | Bind request body |
| `@PathVariable` | Bind path variable |
| `@RequestParam` | Bind query param |

---

## 30. Hot Interview Questions And Answers

### Q1. What is IoC?

```text
IoC means Spring controls object creation and dependency management instead of application
code manually creating dependencies with new.
```

### Q2. What is DI?

```text
Dependency Injection means dependencies are provided from outside the class, usually by
Spring, instead of the class creating them itself.
```

### Q3. IoC vs DI?

```text
IoC is the principle where control is moved to the container. DI is the common technique
Spring uses to implement IoC.
```

### Q4. Which injection type do you prefer?

```text
Constructor injection for mandatory dependencies because it makes dependencies explicit,
supports final fields, improves testability, and prevents partially initialized objects.
```

### Q5. Why avoid field injection?

```text
It hides dependencies, makes unit testing harder, prevents final fields, and allows objects
to be constructed without required dependencies.
```

### Q6. How does `@Autowired` resolve dependencies?

```text
It resolves primarily by type. If multiple beans match, Spring uses @Primary, @Qualifier,
or bean name to resolve ambiguity. If no bean is found for a required dependency, startup fails.
```

### Q7. What is a Spring bean?

```text
A Spring bean is an object created and managed by the Spring IoC container, including
dependency injection, lifecycle callbacks, scope, and possible proxy wrapping.
```

### Q8. `@Component` vs `@Bean`?

```text
@Component is class-level and discovered by component scanning. @Bean is method-level inside
configuration and is used for explicit bean creation, often for third-party classes.
```

### Q9. `@Service` vs `@Component`?

```text
Both register beans, but @Service communicates service-layer/business meaning.
```

### Q10. `@Repository` special behavior?

```text
It marks persistence layer components and can participate in exception translation from
persistence exceptions to Spring's DataAccessException hierarchy.
```

### Q11. What is default bean scope?

```text
Singleton. One bean instance per Spring application context.
```

### Q12. Spring singleton vs Java singleton?

```text
Spring singleton is one bean per Spring container. Java singleton usually means one instance
per classloader/JVM design.
```

### Q13. Explain bean lifecycle.

```text
Spring discovers bean definitions, instantiates beans, injects dependencies, calls aware
callbacks, applies BeanPostProcessors, runs initialization callbacks like @PostConstruct,
possibly creates proxies, then destroys beans using @PreDestroy when context closes.
```

### Q14. What is BeanPostProcessor?

```text
It is an extension hook that lets Spring modify or wrap beans before and after initialization.
Many framework features like AOP proxies rely on bean post-processing.
```

### Q15. What does `@SpringBootApplication` include?

```text
@SpringBootConfiguration, @EnableAutoConfiguration, and @ComponentScan.
```

### Q16. What is auto-configuration?

```text
Spring Boot automatically configures beans based on classpath, properties, existing beans,
and conditional annotations. It backs off when user-defined beans are present.
```

### Q17. What is a starter?

```text
A starter is a dependency bundle that brings common libraries and auto-configuration support
for a specific feature, like web, JPA, validation, or actuator.
```

### Q18. How do profiles work?

```text
Profiles activate environment-specific properties and beans. They are commonly used for
dev, test, stage, and prod configurations.
```

### Q19. `@Value` vs `@ConfigurationProperties`?

```text
@Value is good for simple single values. @ConfigurationProperties is better for grouped,
type-safe, validated configuration.
```

### Q20. What is circular dependency?

```text
Circular dependency occurs when two or more beans depend on each other. Constructor-based
circular dependencies fail and usually indicate a design smell.
```

### Q21. How to fix circular dependency?

```text
Best fix is refactoring responsibilities or extracting a third service. Workarounds include
@Lazy, setter injection, or ObjectProvider, but those are not my first choice.
```

### Q22. What is Spring AOP?

```text
AOP handles cross-cutting concerns like transactions, logging, security, and caching by
using proxies around target beans.
```

### Q23. JDK proxy vs CGLIB?

```text
JDK dynamic proxy is interface-based. CGLIB creates subclass-based proxies for concrete classes.
```

### Q24. Why does self-invocation break `@Transactional`?

```text
Because Spring AOP is proxy-based. A method call inside the same class does not go through
the proxy, so transactional advice may not run.
```

### Q25. Default rollback behavior of `@Transactional`?

```text
By default, Spring rolls back on unchecked exceptions and Error, not checked exceptions
unless rollbackFor is configured.
```

### Q26. What is DispatcherServlet?

```text
DispatcherServlet is the front controller in Spring MVC. It receives requests, finds the
right controller method, invokes it, and handles response rendering/conversion.
```

### Q27. Filter vs Interceptor?

```text
Filter is Servlet-level and runs before DispatcherServlet. Interceptor is Spring MVC-level
and has access to handler/controller information.
```

### Q28. How do you handle global exceptions?

```text
Use @RestControllerAdvice with @ExceptionHandler methods to return consistent API error
responses.
```

### Q29. What is Actuator?

```text
Actuator provides production-ready endpoints like health, metrics, info, and diagnostics.
Sensitive endpoints should be secured in production.
```

### Q30. How does Spring Data repository work?

```text
Spring creates proxy implementations for repository interfaces at runtime based on metadata,
method names, and @Query definitions.
```

---

## 31. Common Interview Traps

| Trap | Correct Answer |
|---|---|
| Field injection is best because easiest | Constructor injection is preferred for required dependencies |
| `@Autowired` is always by name | Primarily by type, name/qualifier resolves ambiguity |
| Spring singleton is JVM singleton | It is singleton per Spring container |
| Prototype bean inside singleton gives new instance every call | No, injected once unless provider/proxy used |
| `@PostConstruct` runs before dependency injection | It runs after dependency injection |
| `@Transactional` works on private methods | Usually no, proxy cannot intercept private method |
| Self-invocation applies AOP | No, it bypasses proxy |
| Checked exceptions rollback by default | No, configure `rollbackFor` |
| Auto-configuration means no control | You can override by defining your own beans/properties |
| `@Service`, `@Component`, `@Repository` are identical in meaning | Registration similar, but semantic roles differ; repository has exception translation |
| Lazy initialization always better | It can move startup failures to runtime |
| Exposing all actuator endpoints is fine | Sensitive endpoints must be secured |

---

## 32. One-Hour Spring Boot Revision Plan

### First 15 Minutes: Core Container

Revise:
- IoC
- DI
- Constructor/setter/field injection
- `@Autowired`
- `@Primary`, `@Qualifier`

Must say:

```text
Constructor injection is my default for mandatory dependencies because it is explicit,
testable, and supports immutability.
```

### Next 15 Minutes: Beans

Revise:
- Bean definition
- Bean scopes
- Bean lifecycle
- `@Component` vs `@Bean`
- `ApplicationContext`

Must say:

```text
A bean is an object managed by Spring, including creation, dependency injection, lifecycle,
scope, and post-processing.
```

### Next 15 Minutes: Boot Magic

Revise:
- `@SpringBootApplication`
- Auto-configuration
- Starters
- Profiles
- Configuration properties

Must say:

```text
Auto-configuration creates default beans based on classpath, properties, and conditions,
and backs off when user-defined beans exist.
```

### Final 15 Minutes: Practical Runtime

Revise:
- AOP/proxy
- `@Transactional`
- MVC request flow
- Filters/interceptors
- Validation
- Exception handling

Must say:

```text
Spring AOP is proxy-based, so features like @Transactional apply when calls go through
the proxy. Self-invocation is a common trap.
```

---

## 33. Final Rapid Revision Sheet

| Need | Spring Concept |
|---|---|
| Object creation controlled by container | IoC |
| Dependencies supplied from outside | DI |
| Preferred required dependency style | Constructor injection |
| Resolve multiple beans | `@Primary`, `@Qualifier` |
| Create third-party bean | `@Bean` |
| Register own class automatically | `@Component` / `@Service` |
| Default scope | singleton |
| New object every lookup | prototype |
| Startup convenience annotation | `@SpringBootApplication` |
| Boot default bean setup | Auto-configuration |
| Environment config | Profiles |
| Type-safe grouped config | `@ConfigurationProperties` |
| Cross-cutting logic | AOP |
| Transaction management | `@Transactional` |
| Request front controller | DispatcherServlet |
| Global exception handling | `@RestControllerAdvice` |
| Request validation | `@Valid` |
| Production endpoints | Actuator |

---

## 34. Strong Closing Answer

If interviewer asks:

```text
How strong are you in Spring Boot fundamentals?
```

Say:

```text
I am comfortable with Spring Core and Spring Boot fundamentals: IoC, dependency injection,
bean creation, scopes, lifecycle, component scanning, auto-configuration, profiles, and
configuration properties. In application code, I prefer constructor injection, keep beans
focused and testable, and understand how Spring applies proxies for AOP features like
transactions and caching. I also understand the MVC request flow through DispatcherServlet,
validation, global exception handling, and production features like Actuator.
```

This is a strong product-company answer because it connects theory to real backend behavior.

