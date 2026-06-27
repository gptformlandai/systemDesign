# Backend Build Pipeline Overview Gold Sheet

> Topic: Build -> Package -> Runtime -> Serve -> Scale.

---

## 1. Intuition

A backend build pipeline is the factory that turns source code into a running service. The service is only production-ready after it has passed tests, been packaged into a versioned artifact, started in the correct runtime, exposed health, and proven it can handle traffic.

Beginner version:

> Backend builds create deployable services, not just compiled files.

---

## 2. Definition

- Definition: A backend build pipeline is the repeatable process that resolves dependencies, compiles/transpiles code, runs checks, packages artifacts, deploys them, and starts services in a runtime.
- Category: Software delivery and runtime engineering.
- Core idea: Make code reproducible, deployable, observable, and scalable.

---

## 3. Universal Pipeline

```txt
Source Code
   |
   v
Dependency Resolution
   |
   v
Compile / Transpile / Typecheck
   |
   v
Unit + Integration Tests
   |
   v
Quality Reports
   Sonar, JaCoCo, coverage, lint, security scans
   |
   v
Package Artifact
   JAR, WAR, wheel, npm package, dist folder, container image
   |
   v
Deploy / Promote
   dev -> test -> staging -> prod
   |
   v
Runtime
   JVM, Node.js, Python ASGI/WSGI, container runtime
   |
   v
Serve Traffic
   HTTP, gRPC, messaging, scheduled jobs
   |
   v
Scale + Observe
   replicas, workers, autoscaling, logs, metrics, traces
```

---

## 4. What Is A Build System?

A build system coordinates the work required to produce a usable artifact.

It answers:

- What source files matter?
- Which dependencies are needed?
- What order should tasks run in?
- What outputs should be produced?
- Can any work be skipped because it is already up-to-date?
- How do we make the result reproducible in CI?

Examples:

| Ecosystem | Build System |
|---|---|
| Java | Maven, Gradle |
| Node.js | npm scripts, Yarn, pnpm, TypeScript, bundlers |
| Python | pip/venv, Poetry, uv, setuptools, build backends |
| Containers | Docker/BuildKit |

---

## 5. What Is An Artifact?

An artifact is the output that moves through environments.

| Ecosystem | Artifact Examples |
|---|---|
| Java | `app.jar`, `app.war`, `sources.jar`, Docker image |
| Node.js | `dist/`, npm tarball, Docker image |
| Python | `.whl`, `.tar.gz`, Docker image |
| Platform | Helm chart, SBOM, test report, coverage report |

Strong artifact properties:

- Versioned.
- Reproducible.
- Traceable to commit SHA.
- Stored in an artifact repository.
- Promoted, not rebuilt, between environments when possible.

---

## 6. Build Time vs Runtime

Build time:

```txt
compile code
run tests
generate reports
package artifact
publish artifact
```

Runtime:

```txt
start process
load config
connect to dependencies
bind port
serve requests
emit logs/metrics
scale under traffic
```

Critical distinction:

> A successful build proves the artifact can be created. It does not prove the service can start, connect to dependencies, or serve production traffic.

---

## 7. Ecosystem Examples

### Java Spring Boot

```txt
mvn clean verify
   -> compile Java
   -> run tests
   -> create JAR
   -> run Sonar/JaCoCo
   -> java -jar app.jar
   -> embedded server binds port
```

### Node.js API

```txt
npm ci
   -> install locked dependencies
npm run build
   -> TypeScript -> JavaScript
npm test
   -> run tests
node dist/server.js
   -> start HTTP server
```

### Python FastAPI

```txt
uv sync
   -> create/sync environment
pytest
   -> run tests
uvicorn app.main:app
   -> ASGI server starts
   -> FastAPI handles requests
```

---

## 8. Real-World Polyglot Pipeline

```txt
Pull request
   |
   +-- Java service
   |     -> Gradle build -> JaCoCo -> Sonar -> JAR -> image
   |
   +-- Node API gateway
   |     -> pnpm install -> TypeScript build -> tests -> image
   |
   +-- Python AI service
   |     -> uv sync -> pytest -> wheel/image -> ASGI runtime
   |
   v
Integration test environment
   |
   v
Staging
   |
   v
Production rollout
```

---

## 9. Common Mistakes

### Mistake: Rebuilding separately for each environment

- Why wrong: Dev, staging, and prod may run different bits.
- Better approach: build once, promote the same artifact.

### Mistake: Treating dependency install as harmless

- Why wrong: dependency resolution can change transitive versions and introduce risk.
- Better approach: lock versions and use deterministic install commands in CI.

### Mistake: Considering a green build equal to a healthy service

- Why wrong: runtime config, DB, secrets, ports, migrations, and health checks can still fail.
- Better approach: add startup smoke tests and health checks.

### Mistake: Ignoring logs until production

- Why wrong: poor startup logs make incidents much harder.
- Better approach: design startup logs as a timeline.

---

## 10. Per-Ecosystem Comparison Table

| Dimension | Java (Spring Boot) | Node.js | Python (FastAPI) |
|---|---|---|---|
| Build tool | Maven / Gradle | npm / pnpm / Yarn | pip / Poetry / uv |
| Compile step | `javac` (Java → bytecode) | `tsc` (TS → JS) or bundler | None (interpreted) or `mypyc` |
| Runtime | JVM (JRE 21) | Node.js 22 LTS | CPython 3.12 |
| Artifact | Fat JAR (all deps inside) | `dist/` folder + `node_modules` | Wheel (.whl) or container |
| Container base | `eclipse-temurin:21-jre-alpine` | `node:22-alpine` | `python:3.12-slim` |
| Serve layer | Embedded Tomcat / Netty | Express / Fastify / NestJS | Uvicorn (ASGI) / Gunicorn |
| Health probe | Spring Actuator `/health` | Custom or `express-healthcheck` | FastAPI `/health` endpoint |
| Config injection | env vars → relaxed binding | `process.env` | `os.environ` / pydantic settings |
| Concurrency model | Thread-per-request (MVC) or reactive (Webflux) | Event loop + cluster | async/await (ASGI) or workers |

---

## 11. Spring Boot Fat JAR Internals

A Spring Boot fat JAR (produced by `spring-boot-maven-plugin`) is a self-contained executable JAR:

```txt
app.jar
├── BOOT-INF/
│   ├── classes/          ← compiled application classes
│   └── lib/              ← all dependency JARs (nested)
├── META-INF/
│   └── MANIFEST.MF       ← Main-Class: org.springframework.boot.loader.launch.JarLauncher
└── org/springframework/boot/loader/  ← Spring Boot Loader (bootstrap code)
```

The Spring Boot Loader is responsible for:
1. Reading the nested JAR layout (JDK cannot load nested JARs natively)
2. Creating a `ClassLoader` that reads from `BOOT-INF/lib/`
3. Delegating to your application's `main()` method

Why this matters: the fat JAR is ~30-200MB — it includes everything the app needs to start, making Docker images self-contained.

```bash
# Verify contents
jar tf app.jar | grep "BOOT-INF/lib" | head -5
# Check Main-Class
jar xf app.jar META-INF/MANIFEST.MF && cat META-INF/MANIFEST.MF
```

---

## 12. Container-Native JVM Tuning

The JVM historically read `/proc/meminfo` for total system RAM, ignoring container cgroup limits. Java 10+ reads cgroup limits correctly.

```bash
# Verify JVM sees container limits correctly
docker run --memory=512m eclipse-temurin:21-jre java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize
# Should show ~128MB (25% of 512m default), not host RAM

# Use percentage-based heap:
docker run --memory=1g eclipse-temurin:21-jre \
  java -XX:MaxRAMPercentage=75 -XshowSettings:all -version 2>&1 | grep -i heap
# Shows heap = 768MB (75% of 1g)
```

Recommended container JVM flags:
```bash
-XX:MaxRAMPercentage=75
-XX:InitialRAMPercentage=50
-XX:MaxMetaspaceSize=200m
-XX:+UseG1GC
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
```

---

## 13. Observability Pipeline

```txt
Application
     |
     +-- Logs → stdout → container log driver → Fluentd/Fluent Bit → Elasticsearch/Loki
     |
     +-- Metrics → /actuator/prometheus → Prometheus scrape → Grafana
     |
     +-- Traces → OpenTelemetry SDK → OTLP exporter → Jaeger / Tempo
     |
     +-- Health → /actuator/health → Kubernetes probes
```

Structured logging (JSON) is required for log aggregation:

```yaml
# application.yml — enable JSON logging
logging:
  structured:
    format:
      console: ecs   # Elastic Common Schema JSON (Spring Boot 3.4+)
```

Every log entry includes: `timestamp`, `level`, `traceId`, `spanId`, `serviceVersion`, `message`.

---

## 14. Interview Insight

Strong answer:

> I separate backend delivery into build, package, runtime, serve, and scale. Build resolves dependencies and validates the code. Package creates a versioned artifact. Runtime starts the artifact with config and dependencies. Serve exposes traffic through HTTP/gRPC/messaging. Scale adds replicas, workers, autoscaling, observability, and failure handling.

Follow-up trap:

> If the Docker image builds successfully, is the service production-ready?

Good answer:

> No. Image build only proves packaging. I still need startup validation, config/secrets, dependency connectivity, health endpoints, metrics, logs, resource limits, and deployment checks.

---

## 15. Revision Notes

- One-line summary: Backend delivery is source-to-artifact-to-running-service-to-scaled-system.
- Three keywords: artifact, runtime, health.
- One interview trap: build success is not runtime success.
- Memory trick: Build creates the thing; runtime proves it can live.

Strong answer:

> I separate backend delivery into build, package, runtime, serve, and scale. Build resolves dependencies and validates the code. Package creates a versioned artifact. Runtime starts the artifact with config and dependencies. Serve exposes traffic through HTTP/gRPC/messaging. Scale adds replicas, workers, autoscaling, observability, and failure handling.

Follow-up trap:

> If the Docker image builds successfully, is the service production-ready?

Good answer:

> No. Image build only proves packaging. I still need startup validation, config/secrets, dependency connectivity, health endpoints, metrics, logs, resource limits, and deployment checks.

---

## 11. Revision Notes

- One-line summary: Backend delivery is source-to-artifact-to-running-service-to-scaled-system.
- Three keywords: artifact, runtime, health.
- One interview trap: build success is not runtime success.
- Memory trick: Build creates the thing; runtime proves it can live.
