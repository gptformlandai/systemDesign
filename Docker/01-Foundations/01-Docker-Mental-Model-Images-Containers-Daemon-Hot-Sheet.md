# Docker Mental Model: Images, Containers, Daemon, Registry - Hot Sheet

> Track File #1 of 40 - Group 01: Foundations
> For: beginners and interviews | Level: beginner | Mode: mental model

## 1. Core Idea

Docker packages an application and its runtime dependencies into an image, then runs that image as one or more containers.

```text
Dockerfile -> image -> container -> logs/network/volume/runtime state
```

An image is a template. A container is a running or stopped instance of that image.

---

## 2. Main Objects

| Object | Meaning |
|---|---|
| Docker client | CLI/API you use: `docker ...` |
| Docker daemon | background service that builds/runs/manages containers |
| Dockerfile | recipe for building an image |
| image | immutable layered artifact with app and dependencies |
| container | runtime instance of an image with config and writable layer |
| registry | storage/distribution system for images |
| volume | persistent data managed by Docker |
| network | container communication boundary |

---

## 3. Essential Commands

```bash
docker version
docker info
docker images
docker ps -a
docker pull nginx:alpine
docker run --rm nginx:alpine nginx -v
docker inspect CONTAINER_OR_IMAGE
```

---

## 4. Production Significance

Docker standardizes packaging and runtime behavior, but production quality still depends on:

- small and secure images
- non-root users
- pinned versions or digests
- health checks
- resource limits
- safe secrets handling
- logs and observability
- image scanning and provenance

---

## 5. Interview Summary

```text
Docker uses a client-daemon model to build images from Dockerfiles and run containers from those images. Images are immutable layered artifacts, while containers are runtime instances with configuration, writable state, networks, and mounts. I debug Docker by identifying the object involved: image, container, network, volume, registry, or daemon.
```

---

## 6. Revision Notes

- One-line summary: Docker turns build recipes into images and images into containers.
- Three keywords: image, container, daemon.
- One trap: thinking a container is a VM instead of an isolated Linux process created from an image.