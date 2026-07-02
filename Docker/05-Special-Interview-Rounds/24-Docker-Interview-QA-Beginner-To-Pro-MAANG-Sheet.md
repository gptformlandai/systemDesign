# Docker Interview Q&A: Beginner To Pro - MAANG Sheet

> Track File #24 of 40 - Group 05: Special Interview Rounds
> For: Docker interviews | Level: beginner to senior | Mode: direct Q&A

## 1. What is Docker?

Docker is a platform for building, packaging, distributing, and running applications as containers from images.

## 2. Image vs container?

An image is an immutable layered artifact. A container is a runtime instance of an image with config, process state, network, mounts, and writable layer.

## 3. Dockerfile vs image?

A Dockerfile is the recipe. An image is the built artifact from that recipe and build context.

## 4. What is build context?

The set of files sent to the Docker builder. `COPY` can only access files inside the context unless advanced build features are used.

## 5. Why use `.dockerignore`?

To reduce build context size and prevent copying secrets, dependencies, build outputs, and irrelevant files into the image.

## 6. What are layers?

Image filesystem changes created by Dockerfile instructions. Layer ordering affects cache reuse and image size.

## 7. Why multi-stage builds?

To use build tools in one stage and copy only runtime artifacts into a smaller, cleaner final image.

## 8. How do Docker ports work?

The container listens on a container port. Publishing maps a host port to that container port.

## 9. Why does localhost cause Docker confusion?

`localhost` inside a container means the container itself, not the host or another container.

## 10. Volumes vs bind mounts?

Volumes are Docker-managed persistent storage. Bind mounts map a host path into the container and can cause host coupling and permission issues.

## 11. How do you debug a container that exits?

Use `docker ps -a`, `docker logs`, `docker inspect`, exit code, command/entrypoint, env, mounts, permissions, and OOMKilled state.

## 12. How do you secure Docker containers?

Use non-root users, minimal images, no secrets in images, scan images, avoid privileged mode, drop capabilities, control Docker daemon access, and pin/record digests.

## 13. Tag vs digest?

A tag is a mutable human-readable pointer. A digest is immutable content identity.

## 14. What makes a Docker answer senior-level?

It covers image construction, runtime config, Linux isolation, resource limits, security, registry identity, observability, CI/CD, rollback, and production failure modes.

## 15. What is the first thing you check before a serious Docker incident?

The active daemon/context. `docker context show`, `docker info`, and `docker version` prevent debugging the wrong host or Desktop VM.

## 16. Why is mounting `/var/run/docker.sock` risky?

The Docker socket lets a container control the Docker daemon. A process with socket access can often start privileged containers or mount host paths, so treat it like root-equivalent infrastructure access.

## 17. What is the difference between tag, digest, SBOM, provenance, and signature?

A tag is a human-friendly pointer. A digest is exact content identity. An SBOM lists dependencies. Provenance describes build source and process. A signature or attestation proves an identity approved or produced a specific artifact.

## 18. What is Buildx used for?

Buildx manages advanced BuildKit builds: builders, drivers, multi-platform images, cache exporters/importers, secret mounts, SSH mounts, SBOM/provenance, and Bake-based multi-target builds.

## 19. How do Compose profiles help?

Profiles keep optional services such as admin UIs, debug tools, profilers, seeders, and load-test tools out of the default app while preserving them in the same Compose model.

## 20. What is the env precedence trap in Compose?

`.env` is mainly an interpolation source. Final container env can come from CLI `-e`, interpolated values, `environment`, `env_file`, or Dockerfile `ENV`, with precedence deciding the winner.

## 21. When would you use `--read-only` and tmpfs?

Use `--read-only` to prevent persistent writes to the root filesystem, then add tmpfs or explicit volumes only for paths the app truly needs to write, such as `/tmp` or cache directories.

## 22. Why can Docker behave differently on macOS/Windows than Linux?

Docker Desktop usually runs Linux containers inside a VM. File sharing, networking, CPU/memory/disk limits, architecture, and host access can differ from native Linux Docker Engine.

## 23. Does Kubernetes run Docker?

Modern Kubernetes commonly runs containers through containerd or CRI-O via the CRI. Docker may still be used to build images or for local development, but production runtime must be checked explicitly.
