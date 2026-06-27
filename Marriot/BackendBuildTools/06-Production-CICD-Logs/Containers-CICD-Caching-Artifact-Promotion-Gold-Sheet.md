# Containers, CI/CD, Caching, Artifact Promotion - Gold Sheet

> Goal: understand how backend code moves from source control to a safe, repeatable production deployment.

---

## 1. Intuition

CI/CD is a factory line for software.

```text
Source code
  -> verify
  -> package
  -> scan
  -> version
  -> publish artifact
  -> deploy same artifact
  -> observe
```

The professional rule:

> Build once, promote the same artifact through environments.

Do not rebuild separately for dev, staging, and production unless you want environment-specific bugs.

---

## 2. Definition

- Definition: CI/CD is the automated pipeline that validates code, creates versioned artifacts, and deploys them safely.
- Category: Delivery architecture.
- Core idea: make releases repeatable, observable, reversible, and auditable.

---

## 3. Universal Backend Pipeline

```text
Developer push
  -> checkout
  -> setup toolchains
  -> restore dependency caches
  -> install dependencies in frozen mode
  -> compile/transpile
  -> unit tests
  -> integration tests
  -> coverage report
  -> static analysis
  -> package artifact
  -> build container image
  -> vulnerability scan
  -> publish artifact
  -> deploy
  -> smoke test
  -> monitor
```

Mapped to ecosystems:

| Stage | Java | Node.js | Python |
|---|---|---|---|
| Install | Maven/Gradle dependency resolution | `npm ci`, `pnpm install --frozen-lockfile` | `uv sync --frozen`, `pip install -r` |
| Build | `mvn package`, `gradle build` | `npm run build` | `uv build`, `python -m build` |
| Test | Surefire/JUnit | Jest/Vitest | pytest |
| Coverage | JaCoCo | c8/nyc/Istanbul | coverage.py/pytest-cov |
| Static analysis | Sonar, Checkstyle, SpotBugs | ESLint, Sonar | Ruff, mypy, Sonar |
| Artifact | JAR/WAR | dist package/image | wheel/image |
| Runtime | JVM | Node runtime | Python ASGI/WSGI |

---

## 4. Build Once, Promote Many

Bad release pattern:

```text
build for dev
  -> deploy dev
build again for staging
  -> deploy staging
build again for prod
  -> deploy prod
```

Problems:

- dependency drift
- image drift
- timestamps and generated artifacts differ
- staging did not test the exact production artifact

Better:

```text
commit SHA
  -> build image once
  -> tag image with SHA
  -> deploy same digest to dev
  -> promote same digest to staging
  -> promote same digest to prod
```

Example:

```text
registry.company.com/booking-api:git-8f31c2a
sha256:9b7...
```

Interview insight:

> Tags are human-friendly. Digests are immutable proof of the exact image.

---

## 5. Artifact Types

| Artifact | Ecosystem | Example | Deployment use |
|---|---|---|---|
| JAR | Java | `booking-api-1.4.2.jar` | JVM service |
| WAR | Java | `booking-web.war` | servlet container |
| npm package | Node | `@org/common-lib` | shared library |
| dist folder | Node | `dist/` | app runtime |
| wheel | Python | `booking_api-0.1.0.whl` | Python package |
| Docker image | all | `booking-api:sha` | container runtime |
| Helm chart | Kubernetes | `booking-api-1.4.2.tgz` | deployment template |

Artifact metadata should include:

- commit SHA
- version
- build timestamp
- branch/tag
- build number
- dependency report
- test report
- scan result

---

## 6. Docker Build Strategy

### Java multi-stage example

```dockerfile
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/target/booking-api.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Node multi-stage example

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS builder
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=builder /app/dist ./dist
CMD ["node", "dist/server.js"]
```

### Python multi-stage example

```dockerfile
FROM python:3.12-slim AS builder
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --frozen --no-dev
COPY app ./app

FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /app/.venv /app/.venv
COPY app ./app
ENV PATH="/app/.venv/bin:$PATH"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Key pattern:

```text
copy dependency manifests first
  -> install dependencies
  -> copy source
  -> build app
```

This preserves Docker layer cache when only source changes.

---

## 7. Dependency Caching in CI

| Ecosystem | Cache target | Notes |
|---|---|---|
| Maven | `~/.m2/repository` | key by `pom.xml` hash |
| Gradle | Gradle user home/build cache | key by wrapper and dependency metadata |
| npm | npm cache | prefer `npm ci` for reproducibility |
| pnpm | pnpm store | excellent monorepo cache behavior |
| Yarn | Yarn cache | depends on Yarn version |
| uv | uv global cache | fast sync and package reuse |
| pip | pip cache | useful, but lock discipline still matters |

Bad cache key:

```text
cache-key: backend
```

Better cache key:

```text
cache-key: java-maven-${hashFiles('**/pom.xml')}
```

Why:

- dependency cache should refresh when dependency manifests change
- source changes should not invalidate dependency downloads

---

## 8. Quality Gates

Typical quality pipeline:

```text
compile
  -> unit tests
  -> integration tests
  -> coverage XML
  -> static analysis
  -> quality gate
  -> package
```

Java example:

```bash
mvn -B clean verify
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

Node example:

```bash
npm ci
npm run lint
npm test -- --coverage
npm run build
```

Python example:

```bash
uv sync --frozen
uv run ruff check .
uv run pytest --cov=app --cov-report=xml
uv build
```

Quality gates should check:

- tests pass
- coverage report exists
- critical vulnerabilities fail the build
- code smells stay below threshold
- no high-severity static analysis issues
- artifact is generated

Mistake:

- Running Sonar before coverage reports exist, then wondering why coverage is 0 percent.

---

## 9. Environment Variables and Configuration

Build-time configuration:

- version
- build profile
- compile flags
- source map generation
- feature compilation

Runtime configuration:

- DB URL
- API keys
- port
- log level
- feature flags
- downstream service URLs

Rule:

> Secrets belong in runtime secret management, not in source code or baked into images.

Bad:

```dockerfile
ENV DATABASE_PASSWORD=my-secret
```

Better:

```text
Kubernetes Secret / cloud secret manager
  -> injected as env var or mounted file
  -> app reads at startup
```

Interview insight:

> A container image should be environment-neutral when possible. The same image should run in staging and production with different runtime configuration.

---

## 10. Release Strategies

| Strategy | How it works | Best for | Risk |
|---|---|---|---|
| Rolling | replace instances gradually | normal services | bad release can affect some users |
| Blue/green | switch traffic between two environments | fast rollback | higher infra cost |
| Canary | send small percent first | high-risk services | needs metrics and routing control |
| Feature flags | deploy code separately from release | progressive rollout | flag complexity |

Backend decision:

- use rolling for low-risk normal releases
- use canary for major behavior changes
- use blue/green when rollback speed matters
- use feature flags when business behavior needs gradual exposure

---

## 11. Observability in Deployment

A deployment is not complete when the command succeeds. It is complete when the service is healthy under real traffic.

Watch:

- startup success
- readiness probe
- error rate
- p95/p99 latency
- CPU/memory
- DB pool saturation
- queue lag
- downstream failures
- log error patterns

Smoke test:

```bash
curl -fsS https://api.example.com/health
curl -fsS https://api.example.com/ready
```

Better smoke test:

- validates route wiring
- validates auth if required
- validates a safe read path
- validates dependency connection

---

## 12. Real-World Polyglot Pipeline

System:

```text
React/Next.js frontend
Java order service
Node notification service
Python FastAPI recommendation service
PostgreSQL
Kafka
Redis
```

Pipeline:

```text
monorepo or multi-repo
  -> detect changed services
  -> run ecosystem-specific build
  -> generate reports
  -> build images
  -> scan images
  -> publish images
  -> deploy services independently
```

Important architecture choice:

> Each service should own its build, test, package, and runtime contract, but the platform should standardize logs, metrics, health checks, and artifact promotion.

---

## 13. Common Mistakes

| Mistake | Why it hurts | Better approach |
|---|---|---|
| Rebuilding per environment | staging and prod differ | build once, promote artifact |
| Using latest tags | mutable and unsafe | use commit SHA and image digest |
| Installing dependencies at container startup | slow and fragile | install during image build |
| Ignoring test reports | failures become invisible | publish reports as CI artifacts |
| Bad cache keys | stale or ineffective cache | key by lockfiles/manifests |
| Baking secrets into image | security risk | inject at runtime |
| No rollback plan | outage lasts longer | keep prior artifact and deploy strategy |

---

## 14. Interview Questions

### Question

> Design a backend CI/CD pipeline for Java, Node, and Python services.

Strong answer:

1. I would standardize the stages: checkout, setup toolchain, restore cache, frozen dependency install, build, test, coverage, static analysis, package, scan, publish, deploy, smoke test.
2. Java would use Maven or Gradle with JaCoCo and Sonar.
3. Node would use lockfile-based installs such as `npm ci` or frozen pnpm installs.
4. Python would use `pyproject.toml` plus a lockfile with uv or another project manager.
5. Each service would produce an immutable artifact or Docker image tagged with commit SHA.
6. The same artifact would be promoted across environments.
7. Deployments would be monitored through logs, metrics, health checks, and rollback readiness.

### Question

> Why should dependencies not be installed at runtime?

Strong answer:

> Runtime dependency installation makes startup slower and non-deterministic. Package registries can be unavailable, dependency versions can drift, and failures happen after deployment. Dependencies should be resolved and installed during build, then the built artifact should be promoted.

---

## 15. Revision Notes

- One-line summary: CI/CD turns source code into a tested, scanned, immutable artifact that can be promoted safely.
- Three keywords: immutable artifact, frozen install, promotion.
- One interview trap: treating deployment success as proof of health.
- One memory trick: build once, run many.

