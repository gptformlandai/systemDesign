# Docker Interview Q&A: Beginner To Pro - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
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