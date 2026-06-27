# Backend Build Tools — Production Debugging Case Studies — Gold Sheet

> Format: incident narrative → symptoms → investigation → root cause → fix → lesson

---

## Case Study 1: The Spring Boot App That OOM-Killed Every 4 Hours

### Symptoms
- Spring Boot pod restarts every 3-5 hours in Kubernetes
- `kubectl describe pod` shows `OOMKilled` as the reason
- Heap usage in metrics looks healthy (under 512MB limit)
- Team says "we set -Xmx512m which matches the container limit of 512Mi"

### Investigation

**Step 1: Check actual container memory**
```bash
kubectl top pod payments-service-abc123
# NAME                        CPU    MEMORY
# payments-service-abc123     120m   780Mi   ← already over 512Mi limit
```

**Step 2: Understand total JVM memory**
```bash
kubectl exec -it payments-service-abc123 -- java -XX:+PrintFlagsFinal -version | grep -i maxheap
# MaxHeapSize = 536870912  (512MB)

# But total resident set size (RSS) includes more than heap:
# Heap: 512MB
# Metaspace: ~180MB (many Spring AOP proxies = many classes)
# Thread stacks: 200 threads × 1MB = 200MB
# JVM overhead: ~80MB
# Total: ~972MB >> 512Mi limit
```

**Step 3: Check Metaspace**
```bash
kubectl exec -it payments-service-abc123 -- jstat -gcmetacapacity 1 5
# Metaspace grows to 160-200MB and stays there
```

### Root Cause

Container limit = 512Mi. JVM heap = 512MB. Metaspace + thread stacks + overhead = ~400MB extra. Total = ~912MB → OOM killed.

### Fix

```yaml
# deployment.yaml — increase limit to accommodate all JVM memory
resources:
  requests:
    memory: "1Gi"
  limits:
    memory: "1Gi"
```

```bash
# Use percentage-based heap + explicit Metaspace cap
JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=200m -XX:+ExitOnOutOfMemoryError"
# Heap = 60% of 1Gi = 614MB; Metaspace capped; room for stacks/overhead
```

### Lesson

> The JVM's total memory is NOT just the heap. Container limits must account for heap + Metaspace + thread stacks + direct memory. Use `-XX:MaxRAMPercentage` not `-Xmx` in containers — the JVM reads cgroup limits and sets heap as a percentage of container memory, automatically leaving room for non-heap usage.

---

## Case Study 2: The Flyway Migration That Broke the Rolling Deployment

### Symptoms
- v2.1.0 deployed to production at 14:00
- Error rate spikes to 45% for 4 minutes
- Error: `ERROR: column "status" does not exist`
- Error only on some pods, not all
- Pods running v2.0.0 are throwing errors; pods that updated to v2.1.0 are fine

### Investigation

**Step 1: Check what Flyway ran**
```sql
SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 5;
-- V12 | rename status to order_status | 2026-06-28 14:00:02
```

**Step 2: Check what the old app queries**
```java
// v2.0.0 OrderRepository:
@Query("SELECT o FROM Order o WHERE o.status = :status")
```

**Step 3: Check the migration**
```sql
-- V12__rename_status_column.sql
ALTER TABLE orders RENAME COLUMN status TO order_status;
```

### Root Cause

Kubernetes rolling update runs both v2.0.0 and v2.1.0 pods simultaneously for ~4 minutes. Flyway runs V12 immediately when the first v2.1.0 pod starts — renaming `status` to `order_status`. All remaining v2.0.0 pods that still query `status` immediately fail.

### Fix

**Immediate (hotfix):** Roll back by renaming column back (requires another migration):
```sql
-- V12a__rollback_rename.sql
ALTER TABLE orders RENAME COLUMN order_status TO status;
```
Then redeploy v2.0.0 with this hotfix migration.

**Correct approach — expand and contract:**

V12 (deploy with v2.1.0, keep old column):
```sql
ALTER TABLE orders ADD COLUMN order_status VARCHAR(20);
UPDATE orders SET order_status = status;
```

v2.1.0 code: reads `order_status`, writes both `status` and `order_status` (backward compatible).

V13 (deploy with v2.2.0, after v2.0.0 fully retired):
```sql
ALTER TABLE orders DROP COLUMN status;
```

### Lesson

> Column renames and drops are always breaking changes during rolling deployments. The expand-and-contract pattern is mandatory: add the new column, backfill data, deploy new code, then drop the old column only after all old pods are gone.

---

## Case Study 3: The 15-Minute Maven Build in CI

### Symptoms
- Maven build takes 14-17 minutes in GitHub Actions
- Locally, same build takes 3 minutes
- `test` phase alone takes 11 minutes in CI
- Team has 280 integration tests using `@SpringBootTest`

### Investigation

**Step 1: Measure phase timings**
```bash
mvn -B clean verify -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep "\[INFO\] BUILD\|seconds"
# [INFO] BUILD SUCCESS
# BUILD 14m 32s
```

**Step 2: Check test output**
```bash
# Every @SpringBootTest class shows:
# INFO 12345 --- [main] o.s.t.c.cache.DefaultCacheAwareContextLoaderDelegate: 
# Loading ApplicationContext for test class...
# Starting application context... (8.2 seconds)
# ... test runs ...
# INFO 12345 --- [main] o.s.t.c.cache.DefaultCacheAwareContextLoaderDelegate: 
# Loading ApplicationContext for test class...   ← context loaded AGAIN
```

**Step 3: Identify cause**
```java
// Problem: each @SpringBootTest class loads a fresh context
@SpringBootTest
@MockBean(ExternalPaymentClient.class)  // ← @MockBean invalidates context cache
class OrderServiceTest { ... }

@SpringBootTest
@MockBean(NotificationService.class)    // ← different @MockBean = different context
class InventoryServiceTest { ... }
```

### Root Cause

Every `@MockBean` on a different set of beans causes Spring Test to create a new ApplicationContext. 280 integration test classes × 8.2s context startup = 38 minutes of context startups (partially parallelized to 15 minutes).

### Fix

**Option 1: Consolidate mocks into shared test configuration**
```java
// src/test/java/com/example/TestConfig.java
@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public ExternalPaymentClient mockPaymentClient() { return Mockito.mock(ExternalPaymentClient.class); }

    @Bean
    @Primary
    public NotificationService mockNotificationService() { return Mockito.mock(NotificationService.class); }
}

// All tests use the same context:
@SpringBootTest
@Import(TestConfig.class)
class OrderServiceTest { ... }   // same context as InventoryServiceTest
```

**Option 2: Use test slices instead of full context**
```java
// Controller tests → @WebMvcTest (loads only web layer ~1s)
// Repository tests → @DataJpaTest + Testcontainers
// Service tests → plain Mockito, no Spring context
```

**Result:** Build time dropped from 15 minutes to 4.5 minutes.

### Lesson

> `@MockBean` is convenient but expensive — each unique combination of mocked beans creates a separate Spring ApplicationContext. Consolidate mocks into shared test configurations or, better, use the right test slice for each layer. Controller tests don't need a full context; repository tests don't need controllers.

---

## Case Study 4: Sonar Shows 0% Coverage After Successful Tests

### Symptoms
- `mvn clean verify` runs successfully — all tests pass
- SonarQube shows 0% coverage on new code
- Sonar quality gate fails: "Coverage on new code is 0%, minimum is 80%"
- CI fails before deployment

### Investigation

**Step 1: Check CI pipeline order**
```yaml
# Problematic CI:
- run: mvn clean test           # tests pass ✓
- run: mvn sonar:sonar          # Sonar runs ✗ (no coverage report yet)
- run: mvn jacoco:report        # report generated AFTER Sonar — too late
```

**Step 2: Check if coverage report exists**
```bash
ls target/site/jacoco/
# ls: cannot access 'target/site/jacoco/': No such file or directory
```

**Step 3: Check POM configuration**
```xml
<!-- Missing: jacoco:prepare-agent is not bound to any phase -->
<!-- JaCoCo agent was never activated during test phase -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
    <!-- No <executions> block! -->
</plugin>
```

### Root Cause

Two problems:
1. JaCoCo agent not configured — no coverage data collected during test execution
2. CI pipeline runs Sonar before JaCoCo generates the XML report

### Fix

**Fix POM:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>  <!-- activate agent before tests -->
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>         <!-- generate XML after tests -->
        </execution>
    </executions>
</plugin>
```

**Fix CI pipeline:**
```yaml
- run: mvn -B clean verify                        # tests + JaCoCo report generated
- run: mvn -B sonar:sonar -Dsonar.token=$TOKEN    # Sonar runs AFTER coverage exists
```

### Lesson

> Coverage must be generated before Sonar analysis. The correct order is always: (1) prepare JaCoCo agent, (2) run tests, (3) generate coverage XML, (4) run Sonar scanner. Use `mvn clean verify` which runs all phases in order — tests + verify + JaCoCo report — before running Sonar.

---

## Case Study 5: Python Dependency Works Locally, Fails in Docker

### Symptoms
- FastAPI app works perfectly with `uv run uvicorn app.main:app`
- Docker build succeeds
- Container starts but crashes with: `ModuleNotFoundError: No module named 'cryptography'`
- `cryptography` is not in `pyproject.toml` dependencies

### Investigation

**Step 1: Check local environment**
```bash
uv pip list | grep cryptography
# cryptography     42.0.8

uv pip show cryptography
# Required-by: passlib, python-jose
```

**Step 2: Check pyproject.toml**
```toml
[project]
dependencies = [
    "fastapi>=0.116.0",
    "passlib[bcrypt]>=1.7.4",    # passlib declares cryptography as optional dep
    "python-jose[cryptography]>=3.3.0",   # ← [cryptography] extra SHOULD pull it in
]
```

**Step 3: Check uv.lock**
```
# uv.lock includes cryptography — but why does Docker fail?
```

**Step 4: Check Dockerfile**
```dockerfile
FROM python:3.12-slim
COPY pyproject.toml uv.lock ./
RUN pip install -r requirements.txt   # ← PROBLEM: requirements.txt doesn't have extras
```

### Root Cause

The Dockerfile uses `pip install -r requirements.txt` but `requirements.txt` was generated without the `[cryptography]` extras syntax. `python-jose` was installed without its optional cryptography backend. Locally, the dev used `uv sync` which correctly handles extras from `pyproject.toml` and the lockfile.

### Fix

```dockerfile
FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev   # uses lockfile + respects extras
COPY app ./app
ENV PATH="/app/.venv/bin:$PATH"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
```

### Lesson

> Use the same dependency tool in Docker as in development. If you use `uv` locally, use `uv sync --frozen` in Docker — not `pip install -r requirements.txt` from a generated file that may lose extras information. The lockfile was correct; the build tooling change was the problem.

---

## Revision Notes

- One-line summary: Production incidents trace to: JVM non-heap memory, rolling-deploy schema incompatibility, context cache invalidation, coverage ordering, and tooling inconsistency.
- Five case mnemonics: OOM-Heap-Illusion, Rename-Breaks-Rolling, MockBean-Context-Flood, Coverage-Before-Sonar, Same-Tool-Both-Sides.
- One interview trap: OOM killed ≠ heap overflow — check Metaspace and thread stacks too.
