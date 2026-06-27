# Docker BuildKit, Multi-Stage Builds, Image Scanning, and Distroless — Gold Sheet

> Topic: BuildKit features, multi-stage optimization, layer caching, distroless images, Trivy scanning, SBOM

---

## 1. Intuition

A container image is the artifact you deploy — it must be minimal, fast to build, secure, and reproducible. BuildKit is the modern Docker build engine that makes this achievable through parallel builds, build secrets, layer caching, and multi-platform support. Image scanning catches vulnerabilities before they reach production.

Beginner version:

> Multi-stage builds keep your final image small by discarding build tools. BuildKit makes builds faster with parallelism and caching.

---

## 2. Definition

- Definition: Container image engineering is the practice of building minimal, cacheable, reproducible, and secure Docker images using BuildKit features, multi-stage patterns, and automated vulnerability scanning.
- Category: Container/artifact engineering.
- Core idea: Build-time tools must not ship in runtime images.

---

## 3. Multi-Stage Build Patterns

### Java Spring Boot

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies separately from source
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B   # download deps (cached layer)

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime stage — minimal JRE, no build tools
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the fat JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
```

### Python FastAPI

```dockerfile
FROM python:3.12-slim AS build
WORKDIR /app

# Install uv for fast dependency resolution
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project   # install deps only

COPY app ./app

# Runtime stage
FROM python:3.12-slim AS runtime
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

COPY --from=build /app/.venv ./.venv
COPY --from=build /app/app ./app

ENV PATH="/app/.venv/bin:$PATH"
EXPOSE 8080
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080", "--workers", "4"]
```

### Node.js TypeScript API

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS build
COPY tsconfig.json .
COPY src ./src
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist

EXPOSE 8080
CMD ["node", "dist/server.js"]
```

---

## 4. BuildKit Features

Enable BuildKit (default in Docker 23+):

```bash
DOCKER_BUILDKIT=1 docker build .
# or in daemon config: "features": {"buildkit": true}
```

### Build secrets (do not bake credentials into layers)

```dockerfile
# Do NOT do this — secret baked into image layer:
RUN curl -H "Authorization: Bearer $TOKEN" https://private-registry.example.com/

# DO this with BuildKit secret mount:
RUN --mount=type=secret,id=registry_token \
    curl -H "Authorization: Bearer $(cat /run/secrets/registry_token)" \
    https://private-registry.example.com/
```

```bash
docker build --secret id=registry_token,env=REGISTRY_TOKEN .
```

The secret never appears in any image layer.

### SSH forwarding (clone private repos during build)

```dockerfile
RUN --mount=type=ssh git clone git@github.com:company/private-lib.git
```

```bash
docker build --ssh default=$SSH_AUTH_SOCK .
```

### Cache mounts (persist package manager cache across builds)

```dockerfile
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen
```

This dramatically speeds up rebuilds — the package manager cache survives between builds on the same machine/CI runner.

---

## 5. `.dockerignore` Best Practices

```
# .dockerignore — exclude everything not needed for build
.git
.gitignore
.env*
*.md
target/
dist/
node_modules/
__pycache__/
.pytest_cache/
.coverage
htmlcov/
*.log
.DS_Store
.idea/
.vscode/
```

Without `.dockerignore`, every file in the build context is sent to the Docker daemon — including `node_modules/` (potentially GBs) even if multi-stage builds don't use it.

---

## 6. Distroless Images

Distroless images contain only the application runtime — no shell, no package manager, no utilities.

```dockerfile
# Java with distroless
FROM gcr.io/distroless/java21-debian12:nonroot AS runtime
COPY --from=build /app/target/app.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["/usr/bin/java", "-XX:MaxRAMPercentage=75", "-jar", "/app.jar"]
```

**Benefits:** Attack surface reduced — no `sh`, no `apt`, no `curl` for attackers to use.
**Tradeoff:** No shell for debugging; use `kubectl exec` with ephemeral debug containers or `docker run --entrypoint sh image:debug-variant`.

---

## 7. Image Vulnerability Scanning — Trivy

```bash
# Scan an image for CVEs
trivy image payments-service:1.2.3

# Output:
# payments-service:1.2.3 (debian 12.8)
# CRITICAL: 2
# HIGH: 7
# MEDIUM: 14
# ...

# Scan and fail CI if CRITICAL or HIGH vulns found
trivy image --exit-code 1 --severity CRITICAL,HIGH payments-service:1.2.3

# Scan in CI (GitHub Actions)
- name: Scan image
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: payments-service:${{ github.sha }}
    severity: CRITICAL,HIGH
    exit-code: 1
```

**What Trivy scans:**
- OS packages (Debian, Alpine, Red Hat)
- Application dependencies (Java JAR, npm `node_modules`, Python wheels)
- Misconfigurations in Dockerfiles and Kubernetes manifests
- Secrets accidentally embedded in images

---

## 8. SBOM Generation (Syft + Grype)

Software Bill of Materials — a complete inventory of what's in your image.

```bash
# Generate SBOM for an image
syft payments-service:1.2.3 -o spdx-json > sbom.spdx.json

# Scan SBOM for vulnerabilities
grype sbom:sbom.spdx.json

# Generate during Docker build (Docker 24+ BuildKit)
docker build --sbom=true -t payments-service:1.2.3 .
```

SBOM is increasingly required by:
- US Executive Order on Cybersecurity (2021)
- EU Cyber Resilience Act
- Enterprise security/compliance programs

---

## 9. Image Labeling for Traceability

```dockerfile
ARG VERSION
ARG GIT_COMMIT
ARG BUILD_DATE

LABEL org.opencontainers.image.version="$VERSION"
LABEL org.opencontainers.image.revision="$GIT_COMMIT"
LABEL org.opencontainers.image.created="$BUILD_DATE"
LABEL org.opencontainers.image.source="https://github.com/company/payments-service"
LABEL org.opencontainers.image.title="payments-service"
```

```bash
docker build \
  --build-arg VERSION=1.2.3 \
  --build-arg GIT_COMMIT=$(git rev-parse HEAD) \
  --build-arg BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
  -t payments-service:1.2.3 .
```

---

## 10. Layer Caching Strategy

```
Most stable layers → FIRST (cached most of the time)
Least stable layers → LAST (rebuilt most often)

COPY pom.xml .
RUN ./mvnw dependency:go-offline   ← cached until pom.xml changes
COPY src ./src                      ← source changes frequently
RUN ./mvnw package                  ← only runs when src changes
```

**Cache invalidation is triggered by:** any change in a COPY source, or any change in a preceding layer.

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Single-stage build | Build tools (JDK, Maven, Node dev deps) in runtime image — image 5-10× larger | Multi-stage build |
| `COPY . .` before dependency install | Every source change invalidates deps cache | Copy manifests first, run install, then copy source |
| Running as root in container | Privilege escalation if exploited | Create non-root user; `USER appuser` |
| No `.dockerignore` | Entire build context sent to daemon (slow, may leak files) | Add comprehensive `.dockerignore` |
| Baking secrets with `ENV` or `ARG` | Secret visible in image layers (`docker history`) | Use `--mount=type=secret` |
| Never scanning images | CVEs accumulate invisibly | Trivy scan in CI on every build |

---

## 12. Interview Insight

Strong answer:

> I use multi-stage Docker builds to separate the build environment from the runtime image — the final stage contains only what the app needs to run, which for a Spring Boot service means a JRE (not a JDK) and the fat JAR. I put dependency resolution before source copy so the layer cache isn't invalidated on every source change. BuildKit's `--mount=type=secret` handles credentials needed during build without baking them into image layers. For security, I scan every image with Trivy in CI before pushing, configured to fail if CRITICAL or HIGH CVEs are found. For traceability, OCI labels connect the image to the git commit and build date.

Follow-up trap:

> Why does your CI build always miss the layer cache even though nothing changed?

Good answer:

> Most likely the build context includes files that change on every run — timestamps, CI-generated files, or artifacts in a directory that isn't in `.dockerignore`. The `COPY . .` layer gets invalidated because Docker sees a context diff. Fix: review `.dockerignore` to exclude anything that changes without affecting the build, and use `--cache-from` with a registry-stored cache for CI runners that don't share local state.

---

## 13. Revision Notes

- One-line summary: Multi-stage builds separate build from runtime; BuildKit secrets keep credentials out of layers; Trivy catches CVEs before deployment.
- Three keywords: multi-stage, distroless, scan.
- One interview trap: `COPY . .` before dependency install breaks layer caching.
- Memory trick: Smallest possible image = smallest possible attack surface + fastest pull time.
