# Docker Pro Gap Fill: PID 1, BuildKit, Multi-Platform, OCI, Disk Pressure - MAANG Sheet

> Gap-fill appendix - Group 03: Senior Production
> For: senior Docker/platform interviews | Level: pro | Mode: production internals

## 1. Why This Gap Fill Exists

Many Docker tracks cover build, run, network, and volumes. Senior interviews often probe the parts that break under production pressure:

- PID 1 and signal handling
- graceful shutdown
- BuildKit secrets and cache
- multi-platform image builds
- OCI/containerd/runtime boundaries
- overlay filesystem and daemon disk pressure
- prune safety

```text
Docker maturity = correct image + correct process behavior + correct artifact identity + correct cleanup discipline
```

---

## 2. PID 1 And Signal Handling

Inside a container, the main process runs as PID 1. PID 1 has special signal and child-process behavior on Linux.

| Problem | Symptom | Fix |
|---|---|---|
| shell wrapper ignores signals | slow or failed shutdown | use exec form `CMD`/`ENTRYPOINT` |
| child processes not reaped | zombie processes | use proper init or app process management |
| app ignores SIGTERM | force kill after timeout | implement graceful shutdown |
| long shutdown | deploys hang or drop requests | close listeners and drain work |

Prefer exec form:

```Dockerfile
CMD ["node", "server.js"]
```

Avoid shell form unless you understand signal forwarding:

```Dockerfile
CMD node server.js
```

Useful runtime option:

```bash
docker run --init IMAGE
```

---

## 3. BuildKit, Secrets, SSH, And Cache

BuildKit improves build performance and safer secret handling.

Useful patterns:

```bash
DOCKER_BUILDKIT=1 docker build -t app:local .
docker buildx build --platform linux/amd64,linux/arm64 -t registry/app:1.0.0 .
```

Secret mount pattern:

```Dockerfile
# syntax=docker/dockerfile:1.7
RUN --mount=type=secret,id=npm_token ./install-private-deps.sh
```

Why it matters:

- `ARG` and `ENV` can leak values through metadata/history or final config.
- BuildKit secret mounts are available only during the build step.
- cache mounts speed package manager workflows without shipping cache into runtime.

Cache example:

```Dockerfile
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
```

---

## 4. Multi-Platform Images

Apple Silicon, CI runners, and production nodes may use different CPU architectures.

```text
developer arm64 laptop -> CI amd64 runner -> prod amd64/arm64 cluster
```

Senior checks:

- What platform was built?
- What platform is deployed?
- Is the image a manifest list or single-architecture image?
- Are native dependencies compiled for the correct architecture?

Commands:

```bash
docker image inspect IMAGE --format '{{.Architecture}}/{{.Os}}'
docker buildx imagetools inspect IMAGE
docker manifest inspect IMAGE
```

Failure modes:

- `exec format error`
- dependency works locally but not in production
- slow emulated builds
- wrong image variant pulled by deployment platform

---

## 5. OCI, containerd, runc, And Overlay Filesystems

Docker is not one monolith in modern container stacks.

```text
Docker CLI -> Docker daemon -> containerd -> runc -> Linux kernel namespaces/cgroups
```

Important boundaries:

| Layer | Responsibility |
|---|---|
| Docker CLI | user command interface |
| Docker daemon | image/build/network/volume/container management |
| containerd | container lifecycle management |
| runc | OCI runtime that starts containers |
| Linux kernel | namespaces, cgroups, capabilities, filesystem behavior |
| overlay filesystem | image layers plus writable container layer |

Interview phrase:

```text
Docker packages a workflow around OCI images and Linux isolation. The container is still a host kernel process constrained by namespaces, cgroups, capabilities, mounts, and filesystem layers.
```

---

## 6. Disk Pressure And Prune Safety

Docker hosts can fail because images, stopped containers, volumes, and build cache consume disk.

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

Safe cleanup mindset:

- identify what consumes space
- preserve rollback images
- confirm stopped containers are disposable
- never remove volumes without backup/owner confirmation
- separate build cache cleanup from image/runtime cleanup

---

## 7. Senior Incident Flow

```text
symptom -> container state -> image identity -> process/signal behavior -> resource/disk pressure -> daemon/runtime evidence -> safe mitigation
```

Use this when:

- deployments hang during shutdown
- containers exit with unclear signal behavior
- CI builds leak secrets or are slow
- `exec format error` appears after release
- Docker host is out of disk
- cleanup risks deleting rollback images or volumes

---

## 8. Interview Summary

```text
At senior level, I look beyond docker run. I check whether PID 1 handles signals, builds use BuildKit secrets and cache safely, images match target platforms, Docker's OCI/runtime boundaries are understood, and disk cleanup preserves rollback images and persistent volumes.
```

---

## 9. Revision Notes

- One-line summary: Pro Docker work is process behavior, artifact identity, runtime boundaries, and cleanup discipline.
- Three keywords: PID1, BuildKit, platform.
- One trap: using `docker system prune -a` during an incident before preserving rollback images and volume ownership.