# Docker Pro Gap Fill: PID 1, BuildKit, Multi-Platform, OCI, Disk Pressure - MAANG Deep Dive

> Track appendix #31 - Group 03: Senior Production
> For: senior Docker/platform interviews | Level: pro | Mode: production internals

This appendix fills the senior-level gaps that usually separate "I know Docker commands" from "I can reason about containers in production."

```text
Docker maturity = image quality + process behavior + artifact identity + runtime isolation + cleanup discipline
```

---

## 1. Intuition

Docker is like shipping an application inside a standardized box. Beginner Docker teaches you how to pack and open the box. Senior Docker teaches you what happens when the box is loaded onto different machines, stopped during deployment, inspected during an incident, scanned by security, and cleaned up under disk pressure.

The simplest mental model:

- The image is the artifact.
- The container is a Linux process with isolation and limits.
- The registry identifies what was shipped.
- The runtime and host decide how it behaves under failure.

If you can explain image identity, PID 1 behavior, BuildKit secrets, platform architecture, runtime boundaries, and prune safety, your Docker answer becomes production-grade.

---

## 2. Definition

- Definition: This gap-fill covers Docker production internals that affect correctness, security, portability, operability, and incident recovery.
- Category: Container runtime, build system, artifact supply chain, Linux process isolation, platform operations.
- Core idea: A reliable container system needs correct process shutdown, safe builds, reproducible image identity, compatible CPU architecture, clear runtime boundaries, and safe disk cleanup.

---

## 3. Why It Exists

Most Docker learning paths focus on:

- `docker run`
- Dockerfiles
- ports
- volumes
- Compose
- basic debugging

That is necessary, but not enough for senior interviews or production systems.

These gaps exist because production failures often happen outside the happy path:

- Deployments hang because PID 1 does not forward `SIGTERM`.
- Containers get killed because the app ignores graceful shutdown.
- Secrets leak because teams use `ARG` or `ENV` during builds.
- Images work on an arm64 laptop but fail on amd64 production nodes.
- Rollbacks fail because mutable tags were reused.
- Cleanup deletes volumes or rollback images during an incident.
- Engineers blame Docker when the real issue is kernel, cgroup, filesystem, registry, or orchestration behavior.

Without these concepts, Docker knowledge stays command-level instead of architecture-level.

---

## 4. Reality

This knowledge shows up in:

- backend services deployed by Kubernetes, ECS, Nomad, or Docker Compose
- CI/CD systems that build and promote images
- platform teams managing registries, base images, and build pipelines
- SRE/on-call teams debugging restarts, OOMs, disk pressure, and bad deploys
- security teams enforcing image scanning, non-root users, SBOMs, and secret hygiene
- developer platforms supporting both arm64 developer laptops and amd64 production fleets

Real systems that rely on these ideas:

- microservice platforms
- internal developer platforms
- containerized API services
- batch workers and queue consumers
- ML/model-serving containers
- CI runner fleets
- preview environments
- edge or multi-architecture deployments

---

## 5. How It Works

### Flow A: Container Startup And PID 1

1. Docker creates container configuration from the image plus runtime flags.
2. The runtime starts the configured command as the container's main process.
3. That process becomes PID 1 inside the container namespace.
4. Docker tracks the container lifecycle through that PID.
5. On stop, Docker sends `SIGTERM` to PID 1.
6. If the process does not exit before the timeout, Docker sends `SIGKILL`.
7. The container exits with a status code that can reveal signal or OOM behavior.

Important states:

```text
created -> running -> stopping -> exited
```

Failure path:

- shell wrapper becomes PID 1
- shell does not forward signals correctly
- app never receives `SIGTERM`
- Docker eventually sends `SIGKILL`
- requests are dropped or data is not flushed

Recovery path:

- use exec-form `CMD` or `ENTRYPOINT`
- implement signal handling in the application
- use `docker run --init` or a minimal init when child reaping is needed
- tune shutdown timeouts intentionally

Preferred exec form:

```Dockerfile
CMD ["node", "server.js"]
```

Risky shell form unless you understand forwarding:

```Dockerfile
CMD node server.js
```

Useful runtime option:

```bash
docker run --init IMAGE
```

### Flow B: BuildKit, Secrets, And Cache

1. Docker sends the build context to the builder.
2. BuildKit evaluates Dockerfile instructions as a dependency graph.
3. Stable layers and cache mounts are reused when inputs have not changed.
4. Secret mounts are made available only to specific build steps.
5. Final image layers are produced without including BuildKit secret mounts.
6. Image metadata, history, and layers are pushed to a registry.

Secret-safe pattern:

```Dockerfile
# syntax=docker/dockerfile:1.7
RUN --mount=type=secret,id=npm_token ./install-private-deps.sh
```

Cache pattern:

```Dockerfile
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
```

Why it matters:

- `ARG` and `ENV` can leak values through image metadata, history, logs, or final config.
- BuildKit secret mounts avoid baking secrets into image layers.
- cache mounts speed builds without shipping package caches in runtime images.

### Flow C: Multi-Platform Images

1. A developer builds or pulls an image on a local CPU architecture.
2. CI may build on another architecture.
3. Production nodes may be amd64, arm64, or mixed.
4. A multi-platform manifest points each platform to the correct image digest.
5. The runtime selects the compatible image for the target platform.

```text
developer arm64 laptop -> CI amd64 runner -> prod amd64/arm64 nodes
```

Senior checks:

```bash
docker image inspect IMAGE --format '{{.Architecture}}/{{.Os}}'
docker buildx imagetools inspect IMAGE
docker manifest inspect IMAGE
```

Failure path:

- image is built for the wrong architecture
- deployment node pulls incompatible binary layers
- container fails with `exec format error`

Recovery path:

- build a multi-platform manifest
- pin deployment to a known supported platform
- ensure native dependencies are compiled per architecture

### Flow D: OCI, containerd, runc, And Linux Isolation

Docker is not one single magic box.

```text
Docker CLI -> Docker daemon -> containerd -> runc -> Linux kernel namespaces/cgroups
```

| Layer | Responsibility |
|---|---|
| Docker CLI | user command interface |
| Docker daemon | image, build, network, volume, container management |
| containerd | lower-level container lifecycle management |
| runc | OCI runtime that starts containers |
| Linux kernel | namespaces, cgroups, capabilities, mounts, signals |
| overlay filesystem | image layers plus writable container layer |

Interview phrase:

```text
Docker packages a workflow around OCI images and Linux isolation. The container is still a host kernel process constrained by namespaces, cgroups, capabilities, mounts, and filesystem layers.
```

### Flow E: Disk Pressure And Prune Safety

1. Docker accumulates images, stopped containers, build cache, volumes, logs, and writable layers.
2. Builds, pulls, or container starts begin failing.
3. Engineers inspect disk consumers before deleting anything.
4. Disposable objects are removed first.
5. Rollback images and persistent volumes are preserved unless explicitly approved.

Safe inspection:

```bash
docker system df
docker system df -v
docker builder du
docker ps -a
docker images
docker volume ls
```

Dangerous cleanup if used blindly:

```bash
docker system prune -a
docker volume prune
docker compose down -v
```

---

## 6. What Problem It Solves

- Primary problem solved: makes Docker workloads safe, portable, debuggable, and recoverable under production conditions.
- Secondary benefits: better deployment behavior, faster builds, fewer secret leaks, safer rollbacks, clearer RCA, better platform compatibility.
- Systems impact: improves reliability, security posture, developer velocity, incident response, and release confidence.

---

## 7. When to Rely on It

Use this mental model when you hear these interview or production keywords:

- container restarts
- graceful shutdown
- stuck deployment
- `SIGTERM` or `SIGKILL`
- OOMKilled
- `exec format error`
- slow Docker builds
- image too large
- leaked secret
- mutable tag rollback issue
- Docker host disk full
- `docker system prune`
- image digest
- SBOM or image scan
- Docker vs containerd vs Kubernetes
- arm64 laptop, amd64 production

This is a strong fit when:

- the system runs containers in production
- CI/CD builds and promotes Docker images
- security requires supply-chain controls
- incidents require evidence before mitigation
- multiple CPU architectures exist
- rollback safety matters
- stateful volumes exist

---

## 8. When Not to Use It

Do not over-apply this depth when:

- the task is a tiny local-only prototype
- the container is disposable and never deployed
- a managed serverless platform hides the container runtime details
- a simple local Compose stack is enough for learning
- the bottleneck is application logic, not container behavior

What to use instead:

- For a beginner exercise, start with image, container, port, volume, logs.
- For local development, use Compose and simple Dockerfiles.
- For production orchestration, discuss Kubernetes/ECS/Nomad once Docker boundaries are clear.
- For application performance, profile the app before blaming container overhead.

Senior signal:

```text
I would not introduce complex supply-chain or runtime controls for a throwaway prototype, but I would require them before production promotion.
```

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Better graceful shutdown and fewer dropped requests | Requires app teams to understand signals and lifecycle |
| Safer builds with BuildKit secrets and cache mounts | Build pipelines become more sophisticated |
| Multi-platform images reduce laptop-to-prod surprises | Native dependencies and emulation can complicate builds |
| Digest-based promotion improves reproducibility | More metadata must be tracked in releases |
| Runtime-boundary knowledge improves debugging | Requires some Linux internals understanding |
| Prune safety protects rollback images and data | Cleanup becomes slower and more deliberate |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Latency: graceful shutdown may keep containers alive longer during deploys, but avoids dropped in-flight work.
- Throughput: resource limits prevent host starvation but can throttle a busy container.
- Consistency: digest promotion improves release consistency, while mutable tags are easier but risk drift.
- Cost: build cache and retained rollback images consume storage, but reduce build time and recovery risk.
- Complexity: BuildKit, SBOMs, scans, and multi-platform manifests add pipeline complexity but improve production confidence.
- Availability: safer cleanup and rollback metadata reduce incident blast radius.

### Common Mistakes

- Mistake: using shell-form `CMD` and assuming signals reach the application.
- Why it is wrong: PID 1 may be a shell that does not forward or reap correctly.
- Better approach: use exec-form commands and test `docker stop`.

- Mistake: passing secrets through `ARG` or `ENV`.
- Why it is wrong: values may appear in image history, config, cache, or logs.
- Better approach: use BuildKit secret mounts or runtime secret stores.

- Mistake: deploying mutable tags such as `latest`.
- Why it is wrong: rollback and RCA cannot prove exactly what binary ran.
- Better approach: promote immutable tags or digests and record release metadata.

- Mistake: ignoring platform architecture.
- Why it is wrong: arm64 and amd64 images may not be interchangeable, especially with native dependencies.
- Better approach: inspect image architecture and build multi-platform manifests when needed.

- Mistake: running `docker system prune -a` during a disk incident.
- Why it is wrong: it can remove rollback images and make recovery harder.
- Better approach: inspect disk usage, preserve rollback artifacts, and clean known-disposable objects first.

---

## 11. Key Numbers

Use these as reasoning anchors, not universal laws:

- Docker stop behavior: sends `SIGTERM`, then `SIGKILL` after a timeout. Common default is about 10 seconds unless overridden.
- Kubernetes graceful termination default: commonly 30 seconds via `terminationGracePeriodSeconds`.
- Exit code 137: often means process received `SIGKILL`; commonly seen with OOM or forced kill.
- Exit code 143: often means process exited after `SIGTERM`.
- Health check interval: local examples often use 10-30 seconds; production values are tuned to SLO and startup behavior.
- Image size: smaller images usually improve pull speed and reduce vulnerability surface; exact target depends on language/runtime.
- Replicas for safe rollout: production services usually need more than one healthy instance before rolling updates are safe.
- Registry retention: keep enough previous digests for rollback; many teams retain at least the last few successful releases.
- Build context size: keep it small with `.dockerignore`; huge contexts slow builds and leak unnecessary files.
- Disk pressure threshold: alert before full disk; many operations teams alert around 80-90 percent usage depending on host role.

---

## 12. Failure Modes

| Failure | What User Observes | Evidence | Recovery |
|---|---|---|---|
| App ignores `SIGTERM` | deploys hang or requests drop | `docker stop`, exit code, app logs | implement graceful shutdown, exec-form command |
| Zombie child processes | slow degradation or process table pressure | `ps` inside container, host metrics | use init, reap children, fix supervisor |
| Secret leaked in image | security incident | `docker history`, image config, scanner | rotate secret, rebuild, use BuildKit secrets |
| Wrong architecture image | container fails at startup | `exec format error`, image inspect | rebuild for target platform or manifest list |
| Mutable tag drift | rollback runs unexpected version | digest mismatch, registry history | deploy immutable tag/digest |
| OOMKilled | restart loop, 5xx errors | `.State.OOMKilled`, exit 137, stats | tune memory, fix leak, right-size limits |
| Host disk full | builds/pulls/startups fail | `docker system df`, host disk metrics | safe cleanup, retain rollback and volumes |
| Volume deleted | data loss | volume list, backups, audit logs | restore backup, improve ownership policy |
| Docker socket mounted | host compromise risk | container mounts inspect | remove socket, use scoped builder/runner |
| Read-only filesystem missing tmpfs | app cannot write temp files | app logs, mount config | add tmpfs for temp paths or configure app |

---

## 13. Scenario

- Product / system: A payment API runs in containers across a Kubernetes or ECS-backed production platform.
- Why this concept fits: The service needs graceful deploys, fast rollbacks, secure image builds, immutable release identity, architecture-compatible images, and incident-safe cleanup.
- What would go wrong without it:
  - shell-form `CMD` prevents graceful shutdown and drops in-flight payments
  - a build token leaks into an image layer
  - an arm64 image gets promoted to amd64 production nodes
  - rollback fails because `latest` was overwritten
  - an engineer deletes rollback images during disk pressure
  - RCA cannot prove which digest produced the incident

Production-ready design:

```text
source commit
-> BuildKit build with secret mounts and cache
-> unit/integration tests
-> image scan and SBOM
-> push immutable tag and digest
-> deploy by digest
-> health and readiness checks
-> graceful shutdown on SIGTERM
-> rollback to previous digest
-> retain artifacts and evidence for RCA
```

---

## 14. Code Sample

This Python sample demonstrates application-level graceful shutdown. The exact language does not matter; the pattern does.

```python
import signal
import time

running = True


def handle_sigterm(signum, frame):
    global running
    print("received SIGTERM, draining work")
    running = False


signal.signal(signal.SIGTERM, handle_sigterm)
signal.signal(signal.SIGINT, handle_sigterm)

print("service started")

while running:
    print("processing request batch")
    time.sleep(2)

print("closing listeners")
time.sleep(1)
print("flushing metrics")
time.sleep(1)
print("shutdown complete")
```

Dockerfile pattern:

```Dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY app.py .
USER 10001
CMD ["python", "app.py"]
```

The important part is the exec-form `CMD`. The Python process becomes PID 1 and receives `SIGTERM` directly.

---

## 15. Mini Program / Simulation

This small simulation shows why shutdown timeouts matter. Run it locally, then send `Ctrl+C` or terminate the process.

```python
import os
import signal
import time

shutdown_started_at = None
grace_period_seconds = 5
running = True


def terminate(signum, frame):
    global running, shutdown_started_at
    shutdown_started_at = time.time()
    running = False
    print(f"pid={os.getpid()} got signal={signum}; starting graceful drain")


signal.signal(signal.SIGTERM, terminate)
signal.signal(signal.SIGINT, terminate)

print(f"worker pid={os.getpid()} started")

while running:
    print("working...")
    time.sleep(1)

for step in range(grace_period_seconds):
    print(f"draining step {step + 1}/{grace_period_seconds}")
    time.sleep(1)

elapsed = time.time() - shutdown_started_at
print(f"exited cleanly after {elapsed:.1f}s")
```

Interview interpretation:

```text
If the platform gives this process less time than its drain duration, it will be killed before cleanup. Senior Docker design aligns app drain time, Docker stop timeout, orchestrator termination grace period, and load balancer draining.
```

---

## 16. Practical Question

> You are designing a Docker-based CI/CD and runtime flow for a high-traffic payments API. Builds must be secure, images must run on both arm64 developer machines and amd64 production nodes, deployments must roll back safely, and containers must not drop in-flight requests during rollout. How would you use Docker's senior production features, and what trade-offs would you consider?

---

## 17. Strong Answer

1. I would use Docker, but I would treat the image as a production artifact, not just a local package.
2. I would build with BuildKit so secrets are mounted only during build steps, dependency caches speed builds, and the final runtime image stays small.
3. I would produce immutable image tags and record the digest in release metadata. Production would deploy by digest or immutable tag, not `latest`.
4. I would support multi-platform builds if developer and production architectures differ, and I would verify manifests before promotion.
5. I would make the container process handle `SIGTERM`, use exec-form `CMD`, and test graceful shutdown with `docker stop`.
6. I would run as non-root, avoid privileged mode, minimize capabilities, scan images, and avoid mounting the Docker socket into runtime containers.
7. I would configure health checks, logs to stdout/stderr, resource limits, and clear rollback steps.
8. During incidents, I would gather evidence from logs, inspect output, stats, events, OOM state, image digest, and host disk usage before mitigation.
9. The trade-off is more CI/CD and operational complexity, but the payoff is safer deploys, reproducible rollbacks, fewer secret leaks, and clearer RCA.
10. If this were only a local prototype, I would keep the Dockerfile simple and add these controls before production promotion.

---

## 18. Revision Notes

- One-line summary: Pro Docker work is process behavior, safe builds, artifact identity, runtime boundaries, platform compatibility, and cleanup discipline.
- Three keywords: PID 1, BuildKit, digest.
- One interview trap: saying "a container is like a lightweight VM" and stopping there; senior answers mention host kernel processes, namespaces, cgroups, signals, image digests, and runtime evidence.
- One memory trick: `P-B-M-O-D` = Process, Build, Manifest, OCI, Disk.

Fast recall:

```text
PID 1 handles shutdown.
BuildKit protects build secrets.
Manifests solve platform mismatch.
OCI explains runtime boundaries.
Disk cleanup must preserve rollback and data.
```
