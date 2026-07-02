# Docker Production Capstone: Secure Build, Compose, Registry, Runbook

> Track File #40 of 40 - Group 06: Practice Upgrade
> For: final Docker mastery proof | Level: pro | Mode: capstone project

## 1. Goal

Build a production-style Docker workflow for one small backend service and prove that you can build, run, secure, scan, promote, debug, and recover it.

The final answer should feel like this:

```text
source -> Dockerfile -> BuildKit build -> SBOM/provenance -> scan -> registry digest -> Compose runtime -> incident runbook
```

---

## 2. What You Build

Create a small API service with one dependency service.

Suggested stack:

- API: Java/Spring Boot, Node/Express, Python/FastAPI, or Go
- dependency: Postgres or Redis
- optional profile: Adminer, Redis Commander, or debug shell
- registry: local registry or company registry sandbox
- CI simulation: local script or GitHub Actions-style command list

---

## 3. Required Deliverables

| Deliverable | Must Prove |
|---|---|
| Dockerfile | small image, cache-aware layers, non-root user |
| `.dockerignore` | secrets and heavy files excluded |
| Compose file | api + dependency + health checks + named volume |
| profile | optional debug/admin service does not start by default |
| Buildx command | reproducible image build with cache plan |
| SBOM/provenance | metadata generated or documented |
| scan result | vulnerability review with policy decision |
| registry tag/digest | exact artifact identity recorded |
| hardening run command | non-root, read-only, caps, tmpfs where practical |
| runbook | startup, network, volume, registry, disk pressure, rollback |

---

## 4. Step 1: Dockerfile

Minimum standards:

- use dependency lock files
- copy lock files before app source
- use multi-stage build when language benefits
- create non-root user
- set explicit working directory
- expose only documented port
- run one main process
- do not store secrets in image layers

Example skeleton:

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies || true
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean bootJar

FROM eclipse-temurin:21-jre
RUN useradd -r -u 10001 app
WORKDIR /app
COPY --from=build --chown=app:app /src/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Adapt the stack to your chosen language.

---

## 5. Step 2: Compose Runtime

Your Compose file must include:

- project name
- API service
- database/cache service
- named volume for state
- service DNS usage
- health checks
- optional profile
- safe env handling
- clear ports

Example requirements:

```yaml
services:
  api:
    build:
      context: .
      target: runtime
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]

  adminer:
    image: adminer
    profiles: ["debug"]

volumes:
  db_data:
```

Run:

```bash
docker compose config
docker compose up -d
docker compose ps
docker compose logs -f api
docker compose --profile debug up -d adminer
```

---

## 6. Step 3: Build and Metadata

Build with Buildx:

```bash
docker buildx create --name capstone-builder --driver docker-container --use
docker buildx inspect --bootstrap

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --tag registry.local/app/api:git-${GIT_SHA:-local} \
  --cache-to type=local,dest=.buildx-cache,mode=max \
  --cache-from type=local,src=.buildx-cache \
  --sbom=true \
  --provenance=true \
  --push .
```

If you cannot push, document why and use a local single-platform build:

```bash
docker buildx build --load -t app/api:local .
```

---

## 7. Step 4: Scan and Policy

Run one scanner available in your environment:

```bash
docker scout cves app/api:local
docker scout recommendations app/api:local
```

Policy decision template:

```text
Image:
Digest:
Scanner:
Critical:
High:
Fix available:
Decision: pass / fail / exception
Exception owner:
Expiry date:
Reason:
```

Passing means risk is reviewed, not that CVE count is zero.

---

## 8. Step 5: Hardening Proof

Run or document the hardened runtime:

```bash
docker run --rm \
  --user 10001:10001 \
  --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=64m \
  --cap-drop ALL \
  --security-opt no-new-privileges:true \
  --pids-limit 256 \
  --memory 512m \
  --cpus 1.0 \
  -p 8080:8080 \
  app/api:local
```

If the app fails, record the exact missing write path or capability and fix it explicitly.

---

## 9. Step 6: Registry and Promotion

Record:

```text
Commit SHA:
Image tag:
Image digest:
SBOM/provenance location:
Scan result:
Promoted to staging:
Promoted to production:
Rollback digest:
```

Promotion rule:

```text
Build once, scan once, promote the same digest.
```

---

## 10. Step 7: Incident Runbook

Your runbook must include commands for:

### Startup Failure

```bash
docker compose ps
docker compose logs api
docker inspect CONTAINER
```

### Network Failure

```bash
docker compose exec api getent hosts db
docker compose exec api ss -lntp
docker network inspect NETWORK
```

### Volume / Permission Failure

```bash
docker compose exec api id
docker compose exec api stat -c '%u:%g %a %n' /app /tmp
docker volume inspect VOLUME
```

### Registry Failure

```bash
docker pull IMAGE
docker buildx imagetools inspect IMAGE
docker login REGISTRY
```

### Disk Pressure

```bash
docker system df -v
docker buildx du
docker builder prune
```

Write which prune commands are safe and which require approval.

---

## 11. Final Interview Prompt

> Walk me through how your Dockerized service is built, secured, shipped, run, observed, debugged, and rolled back.

---

## 12. Strong Answer Shape

1. I build a cache-aware, multi-stage image with no secrets and a non-root runtime user.
2. I run the app locally through Compose with explicit services, health checks, volumes, profiles, and rendered config validation.
3. I build with Buildx/BuildKit, use cache mounts, and generate SBOM/provenance where supported.
4. I scan the image, review risk with policy, and promote the exact digest.
5. I run with least privilege: non-root, reduced capabilities, read-only filesystem, tmpfs for temp writes, and resource limits.
6. I observe logs, health, resource usage, events, and daemon disk usage.
7. I debug by proving the object and layer: image, container, network, volume, daemon, registry, or app.
8. I roll back by redeploying the previously recorded digest.

---

## 13. Scoring Rubric

| Score | Meaning |
|---:|---|
| 1 | can run a container but cannot explain image/runtime boundaries |
| 2 | has Dockerfile and Compose but weak security or debugging |
| 3 | has cache-aware build, Compose health, volume safety, and basic runbooks |
| 4 | has Buildx, scan, digest promotion, hardening, and incident response |
| 5 | can defend trade-offs, platform boundaries, registry policy, and recovery under interview pressure |

---

## 14. Revision Notes

- One-line summary: This capstone proves Docker as a production workflow, not a command list.
- Three keywords: build, digest, runbook.
- One interview trap: showing a working container without artifact identity or recovery plan.
- One memory trick: "build it, trust it, run it, break it, recover it."
