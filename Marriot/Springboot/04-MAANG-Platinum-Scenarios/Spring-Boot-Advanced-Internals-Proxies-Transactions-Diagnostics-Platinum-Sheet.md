# Spring Boot Advanced Internals Proxies Transactions Diagnostics Platinum Sheet

> Track: Spring Boot Interview Track - MAANG Platinum Scenarios  
> Goal: deepen Spring internals, proxy behavior, transaction traps, and diagnostics beyond normal annotation usage.

Read after the Internals AutoConfiguration Proxies Transactions platinum sheet.

---

## 1. Platinum Internals Mental Model

Spring Boot applications run on the Spring container.

Key phases:

```text
read configuration -> register bean definitions -> post-process definitions -> instantiate
beans -> post-process beans -> wrap proxies -> publish events -> start web server/runners
```

Strong answer:

```text
Spring behavior comes from bean definitions, post-processors, auto-configuration conditions,
and proxies. Most tricky bugs happen when code assumes annotations are magic instead of
proxy-based runtime behavior.
```

---

## 2. BeanDefinition vs Bean Instance

BeanDefinition:

```text
metadata describing how to create a bean
```

Bean instance:

```text
actual object created from the definition
```

Why it matters:

- BeanFactoryPostProcessor changes definitions before beans exist
- BeanPostProcessor changes/wraps bean instances
- proxies are usually created around bean instances

Strong answer:

```text
Spring first builds metadata, then creates objects. Some extension points work before object
creation, others after creation.
```

---

## 3. BeanFactoryPostProcessor

Runs before bean instances are created.

Use cases:

- modify bean definitions
- property placeholder resolution
- configuration class processing

Interview line:

```text
BeanFactoryPostProcessor works at metadata level. It should not depend on normal application
beans because those beans may not exist yet.
```

---

## 4. BeanPostProcessor

Runs around bean initialization.

Use cases:

- create proxies
- process annotations
- inject behavior
- wrap infrastructure beans

Examples:

- AOP proxy creation
- `@Autowired` processing
- lifecycle annotation processing

Strong answer:

```text
BeanPostProcessors are why annotations can trigger runtime behavior. They inspect or wrap
beans during container startup.
```

---

## 5. Auto-Configuration Diagnostics

Debug tools:

- `--debug`
- ConditionEvaluationReport
- Actuator `/actuator/conditions`
- Actuator `/actuator/beans`
- Actuator `/actuator/configprops`
- dependency tree
- logs for auto-config classes

Diagnosis example:

```text
Why did my custom DataSource not get used?
```

Check:

- custom bean type/name
- conditional-on-missing-bean behavior
- profile/property activation
- classpath dependencies
- auto-config ordering

Strong answer:

```text
When auto-configuration surprises me, I inspect the condition report before guessing. It
shows which auto-configurations matched or did not match and why.
```

---

## 6. Proxy Types

| Proxy | Used When |
|---|---|
| JDK dynamic proxy | interface-based proxy |
| CGLIB proxy | class-based proxy |

Implications:

- final classes/methods can be problematic for class proxying
- self-invocation bypasses proxy
- private methods are not advised
- proxy only intercepts calls going through proxy reference

Strong answer:

```text
AOP advice applies when the call goes through the Spring proxy. A method calling another
method on `this` bypasses proxy advice.
```

---

## 7. Proxy Stacking

A bean may have multiple cross-cutting concerns:

- transaction
- security
- caching
- async
- metrics/tracing

Question:

```text
Which advice runs first?
```

Answer:

```text
It depends on advisor ordering. In complex cases, inspect proxy/advisor order or keep designs
simple enough that order is not surprising.
```

Practical trap:

```text
@Async and @Transactional together can move work to another thread where transaction context
is not what the developer expected.
```

---

## 8. Transaction Propagation

Common propagation modes:

| Mode | Meaning |
|---|---|
| REQUIRED | join existing or create new transaction |
| REQUIRES_NEW | suspend existing, create independent transaction |
| SUPPORTS | join if exists, otherwise non-transactional |
| MANDATORY | fail if no existing transaction |
| NOT_SUPPORTED | suspend existing and run non-transactional |
| NEVER | fail if transaction exists |
| NESTED | savepoint-based nested transaction when supported |

Strong answer:

```text
REQUIRES_NEW is not just a stronger REQUIRED. It creates an independent transaction and can
commit even if the outer transaction later rolls back.
```

---

## 9. Transaction Isolation

Isolation controls visibility between concurrent transactions.

| Isolation | Protects Against |
|---|---|
| READ_COMMITTED | dirty reads |
| REPEATABLE_READ | non-repeatable reads in many DBs |
| SERIALIZABLE | strongest, lower concurrency |

Interview maturity:

```text
Isolation behavior depends on the database. I verify the actual database semantics, not only
Spring enum names.
```

---

## 10. Rollback Rules

Default Spring rollback:

```text
RuntimeException and Error trigger rollback.
Checked exceptions do not unless configured.
```

Example:

```java
@Transactional(rollbackFor = PaymentException.class)
void capturePayment() throws PaymentException { }
```

Trap:

```text
Catching an exception and not rethrowing may cause transaction to commit unless rollback is
marked explicitly.
```

---

## 11. Flush vs Commit

JPA flush:

```text
synchronizes persistence context changes to DB SQL statements
```

Commit:

```text
commits transaction durability
```

Trap:

```text
SQL may execute before commit due to flush, but the transaction can still roll back.
```

Strong answer:

```text
Flush is not commit. It sends SQL to the database within the current transaction.
```

---

## 12. Lazy Loading And Transactions

Lazy loading needs an open persistence context.

Common failure:

```text
LazyInitializationException outside transaction/session
```

Better approaches:

- fetch join when needed
- DTO projection
- entity graph
- service-layer transaction boundary
- avoid Open Session In View as a default crutch

Strong answer:

```text
I design query shape intentionally instead of relying on lazy loading during JSON serialization.
```

---

## 13. Transaction + External Call Trap

Bad pattern:

```java
@Transactional
void confirmBooking() {
    bookingRepository.markConfirmed(id);
    paymentClient.capture(paymentId);
}
```

Risks:

- long DB transaction
- lock held during network call
- timeout ambiguity
- partial workflow confusion

Better:

```text
persist pending state -> commit -> outbox/saga -> call payment idempotently -> update final state
```

---

## 14. Diagnosing Missing Bean

Checklist:

1. Is class in component scan path?
2. Is profile active?
3. Is conditional property enabled?
4. Is dependency on classpath?
5. Is bean name/type mismatched?
6. Did custom auto-config back off?
7. Did test slice exclude the bean?

Strong answer:

```text
For missing beans, I check scan path, profile, conditions, classpath, bean type/name, and test
slice boundaries before changing application code.
```

---

## 15. Diagnosing Transaction Not Applied

Checklist:

1. Is method public and called through Spring proxy?
2. Is bean managed by Spring?
3. Is method final/private?
4. Is self-invocation happening?
5. Is transaction manager configured?
6. Is exception type rollback-enabled?
7. Is work moved to async thread?
8. Are logs showing begin/commit/rollback?

---

## 16. Strong Closing Answer

```text
For advanced Spring internals, I reason from the container lifecycle: bean definitions,
post-processors, auto-configuration conditions, bean creation, proxies, and advisor order.
For transaction bugs, I check whether the call goes through the proxy, which propagation and
isolation apply, what exception triggers rollback, and whether external calls or async work
are crossing transaction boundaries incorrectly.
```
