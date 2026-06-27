# Backend Build Tools — Tricky Scenario Questions — Gold Sheet

> Format: broken config / symptom → diagnose before reading the answer

---

## How to Use

Cover the answer section. Read the scenario. Think through the root cause and fix. Then reveal.

---

## Java / Maven / Gradle Scenarios

---

### J-1 — The Snapshot That Won't Update

```xml
<!-- service-b/pom.xml -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>shared-lib</artifactId>
    <version>2.1.0-SNAPSHOT</version>
</dependency>
```

**Symptom:** Developer Alice publishes a new snapshot of `shared-lib`. Developer Bob runs `mvn clean install` but his service-b still uses the old snapshot from last week.

**What is the problem?**

<details><summary>Answer</summary>

Maven caches snapshots in `~/.m2/repository` with an `updatePolicy` that defaults to `daily`. Bob's local cache has a snapshot from last week and Maven sees no need to re-download it.

Fix options:
1. Force update: `mvn clean install -U` (the `-U` flag forces snapshot update check)
2. Change repository policy in `settings.xml`:
```xml
<repository>
  <id>snapshots</id>
  <releases><enabled>false</enabled></releases>
  <snapshots>
    <enabled>true</enabled>
    <updatePolicy>always</updatePolicy>   <!-- check every time -->
  </snapshots>
</repository>
```
3. Delete local cache: `rm -rf ~/.m2/repository/com/example/shared-lib/`

Root cause: Snapshot update policies exist to prevent excessive network calls, but `daily` means a stale snapshot sits in local cache all day.

</details>

---

### J-2 — The Gradle Task That Won't Cache

```kotlin
// build.gradle.kts
tasks.register("generateConfig") {
    doLast {
        val outputFile = file("$buildDir/generated/config.json")
        outputFile.writeText("""{"version": "${project.version}", "built": "${java.time.Instant.now()}"}""")
    }
}
```

**Symptom:** The `generateConfig` task always runs even when nothing changed. The team added it to speed up CI but it's never `UP-TO-DATE`.

**What is wrong?**

<details><summary>Answer</summary>

The task has no declared inputs or outputs, AND it includes `java.time.Instant.now()` — a hidden time-based input that changes every run.

Two fixes needed:
1. Declare inputs and outputs so Gradle can skip/cache the task:
```kotlin
tasks.register("generateConfig") {
    val version = project.version.toString()
    inputs.property("version", version)
    val outputFile = layout.buildDirectory.file("generated/config.json")
    outputs.file(outputFile)

    doLast {
        outputFile.get().asFile.writeText("""{"version": "$version"}""")
        // Remove Instant.now() — it's a non-deterministic input
    }
}
```
2. Remove the timestamp — or make it an explicit input property if needed. Non-deterministic tasks cannot be cached.

</details>

---

### J-3 — The JVM OOM Kill

```yaml
# Kubernetes deployment
resources:
  requests:
    memory: "512Mi"
  limits:
    memory: "512Mi"
```

```bash
# Java startup command
JAVA_OPTS="-Xmx512m -Xms256m"
java $JAVA_OPTS -jar app.jar
```

**Symptom:** Pod is OOM-killed repeatedly. The team thinks the heap is within the 512Mi limit.

**What is wrong?**

<details><summary>Answer</summary>

The total JVM memory is NOT just the heap. The pod is using:
```
Heap: 512MB (-Xmx)
Metaspace: ~150MB (default unlimited)
Thread stacks: 200 threads × 1MB = 200MB
Direct/NIO memory: ~100MB
JVM overhead: ~50MB
Total: ~1012MB >> 512Mi limit → OOM killed by kernel
```

Fix: Container limit must account for all JVM memory, or use percentage-based allocation:
```yaml
resources:
  limits:
    memory: "1Gi"   # allow room for non-heap
```
```bash
JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:MaxMetaspaceSize=200m -XX:+ExitOnOutOfMemoryError"
# JVM allocates 768MB heap, pod stays within 1Gi limit
```

</details>

---

### J-4 — The Scope Trap

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
    <scope>test</scope>   <!-- ← problem -->
</dependency>
```

**Symptom:** All tests pass. App starts locally with `mvn spring-boot:run`. But the Docker container crashes with `Unable to create connection: No suitable driver found for jdbc:postgresql://...`

**What is wrong?**

<details><summary>Answer</summary>

The PostgreSQL JDBC driver is scoped as `test` — it is excluded from the compiled, packaged JAR. It works locally because the driver is on the classpath during local development (Maven adds test-scoped deps to IDE classpath), but is not included in the production JAR.

Fix:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
    <scope>runtime</scope>   <!-- runtime scope: in JAR, not needed to compile -->
</dependency>
```

Rule: JDBC drivers are `runtime` scope — needed at runtime but not to compile application code.

</details>

---

## Spring Boot / Configuration Scenarios

---

### SB-1 — The Profile That Didn't Activate

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/payments
```

```dockerfile
ENV SPRING_PROFILES_ACTIVE=production
```

**Symptom:** The production pod connects to the wrong database. It's using `localhost:5432` from `application.yml`.

**What is wrong?**

<details><summary>Answer</summary>

The profile name mismatch. The file is `application-prod.yml` (profile name: `prod`) but the environment variable activates `production`. Spring Boot looks for `application-production.yml`, which doesn't exist, so it falls through to the base `application.yml` with `localhost:5432`.

Fix:
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod   # must match the file suffix
```
Or rename the file:
```
application-production.yml        # matches SPRING_PROFILES_ACTIVE=production
```

This is an easy mistake and a common interview trap.

</details>

---

### SB-2 — Actuator Exposes Secrets

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"   # expose all endpoints
```

**Symptom:** Security audit finds that `curl https://api.example.com/actuator/env` returns database passwords and API keys in plaintext.

**What is wrong and how do you fix it?**

<details><summary>Answer</summary>

`include: "*"` exposes all actuator endpoints including `/actuator/env` which shows all resolved properties — including secrets injected as environment variables.

Fix:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus   # explicit allowlist only
      base-path: /actuator
  server:
    port: 8081                           # serve on internal management port not exposed publicly
  endpoint:
    env:
      enabled: false                     # disable env endpoint entirely
    health:
      show-details: when-authorized      # require auth for details
```

Also: serve actuator on a separate internal port not exposed in the public Kubernetes Service/Ingress.

</details>

---

## Docker / Container Scenarios

---

### D-1 — The Cache-Busting Dockerfile

```dockerfile
FROM node:22-alpine
WORKDIR /app
COPY . .                      # ← problem
RUN npm ci
RUN npm run build
CMD ["node", "dist/server.js"]
```

**Symptom:** Every build takes 3-4 minutes even when only one TypeScript file changed. The team expected `npm ci` to be cached.

**What is wrong?**

<details><summary>Answer</summary>

`COPY . .` copies ALL project files before `npm ci`. Any source file change (even changing a comment in a `.ts` file) invalidates this layer, which cascades to invalidate the `npm ci` layer — causing all dependencies to be re-downloaded every time.

Fix: copy manifests first, install, then copy source:
```dockerfile
FROM node:22-alpine
WORKDIR /app

COPY package.json package-lock.json ./   # only invalidate if dependencies change
RUN npm ci                               # cached unless package files change

COPY tsconfig.json .
COPY src ./src                           # source changes don't bust the deps cache
RUN npm run build
CMD ["node", "dist/server.js"]
```

</details>

---

### D-2 — The Secret in the History

```dockerfile
FROM python:3.12-slim
ARG PRIVATE_PYPI_TOKEN
RUN pip install --extra-index-url https://oauth2:$PRIVATE_PYPI_TOKEN@pypi.company.com/simple mylib==1.0.0
```

**Symptom:** Security scan flags that private registry credentials are visible in `docker history` output.

**What is wrong?**

<details><summary>Answer</summary>

`ARG` values appear in the image layer and are visible via `docker history --no-trunc`. Even if the `ARG` is only used in a `RUN` command, the command string (including the token value interpolated) is stored in layer metadata.

Fix with BuildKit secret mount:
```dockerfile
# syntax=docker/dockerfile:1
FROM python:3.12-slim
RUN --mount=type=secret,id=pypi_token \
    pip install --extra-index-url \
    "https://oauth2:$(cat /run/secrets/pypi_token)@pypi.company.com/simple" \
    mylib==1.0.0
```
```bash
docker build --secret id=pypi_token,env=PRIVATE_PYPI_TOKEN .
```
The secret never appears in any layer or `docker history` output.

</details>

---

## Node.js Scenarios

---

### N-1 — Works Locally, Fails in Docker

```dockerfile
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci                       # uses --omit=dev implicitly when NODE_ENV=production
COPY dist ./dist
CMD ["node", "dist/server.js"]
```

**Symptom:** Build works. Container starts but immediately crashes with `Cannot find module '@nestjs/platform-express'`.

**What is wrong?**

<details><summary>Answer</summary>

`npm ci` with `NODE_ENV=production` is equivalent to `npm ci --omit=dev` — it skips installing devDependencies. If `@nestjs/platform-express` was placed in `devDependencies` by mistake (common with NestJS scaffolding), it is not installed in the production image.

Fix:
```json
// package.json — move to dependencies
{
  "dependencies": {
    "@nestjs/platform-express": "^10.0.0"
  }
}
```

Rule: anything imported by production code must be in `dependencies`, not `devDependencies`.

</details>

---

## Python Scenarios

---

### P-1 — The Pytest That Patches the Wrong Thing

```python
# app/services/email_service.py
from app.clients.smtp_client import SmtpClient   # imports the class

class EmailService:
    def send_welcome(self, email: str) -> None:
        client = SmtpClient()
        client.send(email, "Welcome!")
```

```python
# tests/test_email_service.py
from unittest.mock import patch

def test_send_welcome_email():
    with patch("app.clients.smtp_client.SmtpClient") as mock:  # ← problem
        mock.return_value.send.return_value = None
        service = EmailService()
        service.send_welcome("user@example.com")
        mock.return_value.send.assert_called_once()   # FAILS
```

**What is wrong?**

<details><summary>Answer</summary>

The patch targets `app.clients.smtp_client.SmtpClient` — the definition site. But `email_service.py` already imported `SmtpClient` at module load time. The name `SmtpClient` in `email_service.py`'s local namespace still points to the original class.

Fix: patch at the usage site:
```python
with patch("app.services.email_service.SmtpClient") as mock:
    # now patches the name as it exists in the module using it
```

The rule: **patch where the name is used, not where it is defined**.

</details>

---

## Flyway / Database Scenarios

---

### F-1 — The Migration That Breaks Rolling Deployment

```sql
-- V5__rename_status_column.sql
ALTER TABLE orders RENAME COLUMN status TO order_status;
```

**Symptom:** Deployment of v2.0 app succeeds. But during the 3-minute rolling update, some pods still running v1.9 start throwing `column "status" does not exist` errors.

**What is wrong?**

<details><summary>Answer</summary>

Renaming a column is a breaking change for any pod version that still uses the old column name. During a rolling update, old and new pods run simultaneously.

Zero-downtime fix using expand-and-contract:

**Step 1 — V5 (expand): add new column, keep old one**
```sql
ALTER TABLE orders ADD COLUMN order_status VARCHAR(20);
UPDATE orders SET order_status = status WHERE order_status IS NULL;
```

**Step 2 — Deploy v2.0** (reads/writes both `status` and `order_status` or handles both)

**Step 3 — V6 (contract): drop old column after all old pods retired**
```sql
ALTER TABLE orders DROP COLUMN status;
```

Never rename or drop columns during a rolling update — always expand first, then contract in a subsequent release.

</details>

---

## Revision Notes

- One-line summary: Config mistakes have consistent root causes — trace the symptom to the pipeline phase.
- Three keywords: scope, profile-name, expand-contract.
- One interview trap: Maven snapshot cache uses `-U` to force refresh; default is `daily`.
- Memory trick: For DB migrations, add before you remove — expand then contract.
