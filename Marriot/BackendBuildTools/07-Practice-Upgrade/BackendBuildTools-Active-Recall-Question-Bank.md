# Backend Build Tools — Active Recall Question Bank — Gold Sheet

> Self-test: cover answers, state answer aloud, then check. Track which level trips you.

---

## Key

🟢 Foundation — must know cold
🟡 Intermediate — expected at mid-level interviews
🔴 MAANG — senior/staff level depth

---

## Group 1: Maven & Gradle Fundamentals

### 🟢 Q1
What command runs only unit tests in Maven, skipping integration tests and packaging?

<details><summary>Answer</summary>

```bash
mvn test
```
`test` phase includes: validate → initialize → generate-sources → process-sources → generate-resources → process-resources → compile → process-test-sources → test-compile → **test**

Does NOT include `package`. Use `mvn test -DskipITs` if integration tests are bound to `integration-test` phase.

</details>

---

### 🟢 Q2
What is the difference between `<dependencies>` and `<dependencyManagement>` in a Maven POM?

<details><summary>Answer</summary>

- `<dependencies>` — declares actual dependencies; they ARE on the classpath for this module.
- `<dependencyManagement>` — declares version/scope metadata only; does NOT add anything to the classpath. Child modules inherit the version/scope metadata but must still declare the dependency in `<dependencies>` to actually use it.

Use case: parent POM uses `<dependencyManagement>` to pin all versions for child modules. Child modules declare `<dependency>` without version — they inherit from parent.

</details>

---

### 🟡 Q3
What is a Maven BOM (Bill of Materials) and when would you import one?

<details><summary>Answer</summary>

A BOM is a POM that only contains `<dependencyManagement>` — no code, no output artifact. It defines a set of compatible dependency versions tested together.

Import via:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>   <!-- ← import scope: copies all entries into this POM's dependency management -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

Use when: adopting a framework family (Spring Boot, Spring Cloud, Quarkus) where versions must be compatible across many JARs.

</details>

---

### 🟡 Q4
What is Gradle's task DAG and why does it matter for build optimization?

<details><summary>Answer</summary>

Gradle builds a Directed Acyclic Graph of all tasks and their dependencies before executing anything. This allows Gradle to:
1. Skip tasks whose inputs/outputs haven't changed (incremental builds)
2. Run independent tasks in parallel (`--parallel`)
3. Use cached outputs from previous builds or remote cache

A task without declared `inputs` and `outputs` can never be `UP-TO-DATE` and will always run — breaking the DAG optimization.

</details>

---

### 🔴 Q5
Explain Gradle's configuration cache and its limitations.

<details><summary>Answer</summary>

Configuration cache serializes the configuration phase (task graph + task inputs) so subsequent builds can skip it entirely. Build scripts and plugins are not re-evaluated.

Enable:
```kotlin
// gradle.properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn   # during migration
```

Limitations:
- Plugins must be configuration-cache compatible (e.g. no task accessing `project.` at execution time)
- External process calls during configuration must be avoided
- Not compatible with all community plugins (check plugin release notes)
- First build after any task input/configuration change rebuilds the cache

</details>

---

## Group 2: Spring Boot & JVM

### 🟢 Q6
What is the Spring Boot property source priority order? Which overrides which?

<details><summary>Answer</summary>

Highest to lowest priority:
1. Command-line arguments (`--server.port=9090`)
2. Java System properties (`-Dserver.port=9090`)
3. OS environment variables (`SERVER_PORT=9090`)
4. Profile-specific config files (`application-prod.yml`)
5. Application config files (`application.yml`)
6. `@PropertySource` annotations
7. Default properties

Higher number overrides lower. Environment variables and command-line args beat config files — this is intentional for container-based config injection.

</details>

---

### 🟢 Q7
What is the difference between `-Xmx512m` and `-XX:MaxRAMPercentage=75`?

<details><summary>Answer</summary>

- `-Xmx512m`: Sets heap to exactly 512MB regardless of container size. Fails silently if container memory changes — you must manually update the flag.
- `-XX:MaxRAMPercentage=75`: Sets heap to 75% of the container's memory limit (read from cgroup). Container-aware. If you scale from 1Gi to 2Gi, heap automatically adjusts.

Use `-XX:MaxRAMPercentage=75` in containers — it's self-adjusting and avoids the OOM-killed scenario where total JVM memory exceeds the fixed limit.

</details>

---

### 🟡 Q8
Why does `@MockBean` in tests cause slow CI builds?

<details><summary>Answer</summary>

Spring's `TestContext` caches ApplicationContexts. The cache key includes the set of `@MockBean` declarations. Each unique combination of mocked beans creates a separate context.

If 50 test classes each `@MockBean` a slightly different combination, Spring creates 50 separate contexts instead of reusing one. Each context startup takes 5-10 seconds → build time explodes.

Fix: consolidate mocks into a shared `@TestConfiguration` or use test slices (`@WebMvcTest`, `@DataJpaTest`) that don't need a full context.

</details>

---

### 🟡 Q9
What does Spring Boot Actuator's liveness vs readiness probe signal?

<details><summary>Answer</summary>

- **Liveness** (`/actuator/health/liveness`): Is the app alive / not deadlocked? If liveness fails, Kubernetes restarts the pod.
- **Readiness** (`/actuator/health/readiness`): Is the app ready to receive traffic? If readiness fails, the pod is removed from the load balancer but NOT restarted.

Used together:
- Liveness fails → something is catastrophically wrong, restart helps
- Readiness fails → app is starting up, warming cache, or temporarily overloaded; don't send traffic but don't restart

```yaml
# Kubernetes pod spec
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30   # wait for startup
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
```

</details>

---

### 🔴 Q10
Walk through how ZGC differs from G1GC and when you would choose ZGC in production.

<details><summary>Answer</summary>

G1GC: Generational, concurrent marking with short STW pauses. Typical pause: 10-200ms. Good general purpose GC. Default in Java 9+.

ZGC: Near-zero pause time (<1ms typical). Concurrent compaction — almost all GC work happens concurrently with application threads. Available since Java 15 (production), generational ZGC since Java 21.

Choose ZGC when:
- Application has strict P99 latency requirements (<5ms)
- Large heaps (10GB+) where G1GC pauses scale with heap size
- Services that cannot tolerate 100-200ms GC pauses (trading, payments, gaming)

Cost: ZGC uses more CPU for concurrent work. Under memory pressure it may not reclaim fast enough.

Choose G1GC when: latency tolerance is moderate, CPU budget is constrained, Java < 21.

</details>

---

## Group 3: Docker & Containers

### 🟢 Q11
What is a multi-stage Docker build and why does it matter?

<details><summary>Answer</summary>

A Dockerfile with multiple `FROM` statements. Each stage produces a filesystem snapshot; later stages can `COPY --from=<stage>` specific artifacts.

Why: build-time tools (JDK, Maven, Node.js dev tools, compilers) must NOT be in the runtime image. They add hundreds of MB, increase attack surface (more tools for attackers to use), and slow image pulls.

Example: Java needs JDK to compile but only JRE to run → build stage uses JDK, runtime stage uses JRE.

</details>

---

### 🟢 Q12
Why should containers NOT run as root?

<details><summary>Answer</summary>

If a container vulnerability is exploited and the process is running as root (UID 0), the attacker has root inside the container and potential paths to host escape (especially in poorly-configured environments without seccomp/AppArmor). Non-root processes have limited capabilities even if compromised.

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

Many Kubernetes security policies (PodSecurityPolicy deprecated, OPA Gatekeeper, Pod Security Admission) require non-root users.

</details>

---

### 🟡 Q13
How does `--mount=type=secret` differ from `ARG` for build-time credentials?

<details><summary>Answer</summary>

`ARG` values appear in layer metadata — visible via `docker history --no-trunc`. Even if you `unset` in the same `RUN`, the command string that used the value is stored in the layer.

`--mount=type=secret` mounts a file at `/run/secrets/<id>` only during that specific `RUN` command. The secret never becomes part of any layer. After the `RUN` completes, the secret is gone.

```dockerfile
RUN --mount=type=secret,id=npm_token \
    npm config set //registry.company.com/:_authToken=$(cat /run/secrets/npm_token) \
    && npm ci \
    && npm config delete //registry.company.com/:_authToken
```

</details>

---

### 🔴 Q14
How do you implement layer caching in CI where runners are ephemeral?

<details><summary>Answer</summary>

Ephemeral runners have no local Docker cache between runs. Options:

1. **Registry cache (recommended):** Push/pull cache from a container registry:
```bash
docker build \
  --cache-from type=registry,ref=ghcr.io/org/app:cache \
  --cache-to type=registry,ref=ghcr.io/org/app:cache,mode=max \
  -t ghcr.io/org/app:$SHA .
```

2. **GitHub Actions cache:**
```yaml
- uses: docker/build-push-action@v6
  with:
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

3. **Persistent runner (self-hosted):** Runners keep local Docker cache between jobs.

`mode=max` caches all layers including intermediate stages, not just the final stage.

</details>

---

## Group 4: Flyway / Database

### 🟢 Q15
What naming convention does Flyway use for migration files, and why?

<details><summary>Answer</summary>

Format: `{prefix}{version}__{description}.sql`
- `V` prefix: versioned migration (runs once, never modified)
- `R` prefix: repeatable migration (re-runs when checksum changes)
- `U` prefix: undo migration (requires Flyway Pro)

Double underscore `__` separates version from description.

Examples:
```
V1__create_users_table.sql
V2__add_email_index.sql
R__create_reporting_view.sql
```

Flyway checksums every applied migration. If you modify an applied `V` migration, Flyway will fail with `Validate failed: Migrations have failed validation` on the next startup.

</details>

---

### 🟡 Q16
How do you add a NOT NULL column to a large production table without downtime?

<details><summary>Answer</summary>

Naive approach (causes downtime or long lock):
```sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL DEFAULT 'us-east';
```
On a large table this locks the entire table while it backfills the default.

Zero-downtime approach:
```sql
-- Step 1: Add nullable column (immediate, no lock)
ALTER TABLE orders ADD COLUMN region VARCHAR(10);

-- Step 2: Backfill in batches (no table lock)
UPDATE orders SET region = 'us-east' WHERE region IS NULL AND id BETWEEN 1 AND 100000;
-- Repeat in batches

-- Step 3: Add NOT NULL constraint using new PostgreSQL syntax (minimal lock)
ALTER TABLE orders ALTER COLUMN region SET DEFAULT 'us-east';
ALTER TABLE orders ALTER COLUMN region SET NOT NULL;
-- PostgreSQL 12+ verifies constraint without full rewrite if values exist
```

</details>

---

### 🔴 Q17
What does `flyway.baseline-on-migrate=true` do and when is it appropriate?

<details><summary>Answer</summary>

When Flyway starts on a database that already has schema (no `flyway_schema_history` table), it normally fails — it doesn't know what migrations were already applied.

`baseline-on-migrate=true`: If no history table exists, Flyway creates it and inserts a `baseline` record at the `baselineVersion` (default: `1`). All existing migrations at or below that version are skipped; only newer migrations run.

Use when: introducing Flyway to an existing database that was manually managed. Set `baselineVersion` to one below the first new migration you want to run.

Risk: If set permanently on a service, it silently ignores a missing history table instead of failing — hides schema drift bugs. Turn it off after initial baseline.

</details>

---

## Group 5: Python & Testing

### 🟢 Q18
What is the difference between a pytest `fixture` with scope `function` vs `session`?

<details><summary>Answer</summary>

- `scope="function"` (default): fixture is created fresh for each test function, torn down after each test. Isolates tests completely.
- `scope="session"`: fixture is created once for the entire test session (all tests), torn down at the end.

Use `session` scope for expensive resources:
```python
@pytest.fixture(scope="session")
def postgres_container():
    with PostgresContainer("postgres:16") as pg:
        yield pg   # shared across all tests — starts once, stops once
```

Using `function` scope for a container means starting/stopping Docker for every test — very slow.

</details>

---

### 🟡 Q19
Where should you patch in `unittest.mock.patch` — at the definition or at the usage site?

<details><summary>Answer</summary>

Always at the **usage site** — the module that imports and uses the name.

```python
# utils/email.py:
def send_email(to): ...

# services/user_service.py:
from utils.email import send_email   ← this is where the name 'send_email' lives in user_service

# test: patch at usage site:
with patch("services.user_service.send_email") as mock:
    # correct — patches the name in user_service's namespace

# NOT at definition:
with patch("utils.email.send_email") as mock:
    # wrong — user_service already has its own reference to the original function
```

</details>

---

### 🔴 Q20
How do you test an async FastAPI route that makes an HTTP call to an external service?

<details><summary>Answer</summary>

```python
# app/routes/weather.py
import httpx

async def get_weather(city: str) -> dict:
    async with httpx.AsyncClient() as client:
        resp = await client.get(f"https://weather-api.example.com/v1/{city}")
        return resp.json()
```

```python
# tests/test_weather.py
import pytest
from httpx import AsyncClient, ASGITransport, Response
from unittest.mock import patch, AsyncMock
from app.main import app

@pytest.mark.asyncio
async def test_get_weather():
    mock_response = Response(200, json={"temp": 22, "city": "London"})

    with patch("app.routes.weather.httpx.AsyncClient") as MockClient:
        mock_instance = AsyncMock()
        mock_instance.get.return_value = mock_response
        MockClient.return_value.__aenter__.return_value = mock_instance

        async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
            response = await ac.get("/weather/London")

    assert response.status_code == 200
    assert response.json()["city"] == "London"
```

Key: `AsyncMock` for async methods, patch the httpx client at the usage module, use `ASGITransport` instead of spinning up a real server.

</details>

---

## Group 6: Node.js Builds

### 🟢 Q21
What is the difference between `npm install` and `npm ci`?

<details><summary>Answer</summary>

- `npm install`: resolves dependencies, may update `package-lock.json`, installs devDependencies. For development.
- `npm ci`: reads `package-lock.json` exactly, fails if lockfile is missing or inconsistent with `package.json`, deletes `node_modules` before installing. Faster, deterministic, for CI.

Rule: CI pipelines always use `npm ci`. Developers use `npm install` to add or update packages.

</details>

---

### 🟡 Q22
What happens to devDependencies when `NODE_ENV=production` with `npm ci`?

<details><summary>Answer</summary>

`npm ci` with `NODE_ENV=production` skips installing `devDependencies` (equivalent to `--omit=dev`). This is intentional for production images — test frameworks, type definitions, and build tools are not needed at runtime.

Risk: any runtime dependency accidentally placed in `devDependencies` will be missing in production, causing a module-not-found error. Check by diffing `dependencies` and `devDependencies` sections.

</details>

---

## Group 7: Supply Chain & Security

### 🟡 Q23
What is dependency confusion and how does it work?

<details><summary>Answer</summary>

An attacker publishes a malicious package to npmjs.com (or PyPI) with the same name as a private internal package. If the package manager checks the public registry first, it may download the malicious package — even though your private registry has the real one — because the attacker published a higher version number.

Protection:
- Use scoped packages: `@company/shared-lib` instead of `shared-lib`
- Configure `npmrc` to resolve `@company` scope only from private registry
- Use `--registry` for private packages
- Verify checksums / use Sigstore supply chain signing

</details>

---

### 🔴 Q24
What is an SBOM and when is it legally required?

<details><summary>Answer</summary>

SBOM (Software Bill of Materials): a machine-readable inventory of all components, versions, and licenses in a software artifact.

Formats: SPDX (ISO standard), CycloneDX.

When required:
- US Executive Order 14028 (May 2021): Federal software suppliers must provide SBOMs
- EU Cyber Resilience Act: Products with digital elements sold in EU require SBOMs
- DoD and US federal contractor requirements

Generate:
```bash
syft payments-service:1.2.3 -o spdx-json > sbom.spdx.json
docker build --sbom=true -t payments-service:1.2.3 .
```

Also used for: vulnerability tracking, license compliance, incident response (quickly find "do we use log4j?").

</details>

---

## Rapid-Fire Review (30 seconds each)

| # | Question |
|---|---|
| RF-1 | How do you force Maven to re-download snapshots? |
| RF-2 | What scope does a JDBC driver use? |
| RF-3 | Which JVM flag sets heap as % of container memory? |
| RF-4 | What file must Flyway NOT modify after applying? |
| RF-5 | Which pytest scope starts container once per session? |
| RF-6 | What kills the Docker layer cache in a single-stage Node Dockerfile? |
| RF-7 | How do you expose actuator only on health + prometheus? |
| RF-8 | What's the CI command for deterministic Node installs? |
| RF-9 | What is the CORRECT order: tests → coverage report → Sonar scan? |
| RF-10 | How do you add a NOT NULL column without locking a large table? |

<details><summary>Rapid-Fire Answers</summary>

1. `mvn clean install -U`
2. `runtime`
3. `-XX:MaxRAMPercentage=75`
4. Applied versioned migrations (`V` prefix files)
5. `scope="session"`
6. `COPY . .` before `npm ci`
7. `management.endpoints.web.exposure.include: health,prometheus`
8. `npm ci`
9. Yes — always in that order: tests, then coverage report, then Sonar
10. Add nullable, backfill in batches, then add NOT NULL constraint

</details>

---

## Revision Notes

- One-line summary: 24 questions across all 7 BackendBuildTools topic groups, tiered 🟢→🟡→🔴.
- Foundation must-know: scope rules, `-XX:MaxRAMPercentage`, profile name matching, patch-at-usage-site.
- MAANG extras: ZGC vs G1GC, configuration cache limits, registry layer caching in CI, SBOM legal requirements.
