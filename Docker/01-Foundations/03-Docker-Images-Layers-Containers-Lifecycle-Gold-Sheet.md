# Docker Images, Layers, Containers, and Lifecycle - Gold Sheet

> Track File #3 of 40 - Group 01: Foundations
> For: Docker internals basics | Level: beginner to intermediate | Mode: image/container lifecycle

## 1. Core Idea

Images are layered and immutable. Containers add runtime configuration and a writable layer.

```text
base layer + dependency layers + app layer = image
image + command/env/network/mounts/limits = container
```

---

## 2. Lifecycle States

| State | Meaning |
|---|---|
| created | container object exists but process not running |
| running | container main process is active |
| paused | process is paused |
| exited | main process ended |
| restarting | restart policy is trying to restart container |
| removed | container object deleted |

---

## 3. Commands

```bash
docker images
docker image inspect IMAGE
docker history IMAGE
docker ps
docker ps -a
docker run --name demo nginx:alpine
docker stop demo
docker start demo
docker rm demo
docker rmi IMAGE
```

---

## 4. Image vs Container Mistakes

| Mistake | Correction |
|---|---|
| deleting image while container exists | remove stopped containers first if image is unused |
| expecting container changes to update image | commit/build a new image intentionally |
| storing important data in writable layer | use volumes or external storage |
| using `latest` everywhere | use explicit tags/digests for production |

---

## 5. Production Failure Modes

- image tag points to new unexpected content
- container exits because PID 1 command finishes
- writable layer grows unexpectedly
- base image has vulnerabilities
- architecture mismatch between image and host

---

## 6. Interview Summary

```text
Docker images are immutable layered artifacts, and containers are runtime instances with writable state and configuration. I separate image problems from container problems by inspecting image history, container state, logs, command, environment, mounts, and network settings.
```

---

## 7. Revision Notes

- One-line summary: Images are build artifacts; containers are runtime instances.
- Three keywords: layer, image, lifecycle.
- One trap: relying on mutable container state instead of rebuilding images or using volumes.