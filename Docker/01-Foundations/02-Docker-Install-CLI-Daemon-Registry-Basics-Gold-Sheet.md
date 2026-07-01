# Docker Install, CLI, Daemon, and Registry Basics - Gold Sheet

> Track File #2 of 30 - Group 01: Foundations
> For: setup and first commands | Level: beginner | Mode: Docker environment

## 1. Core Idea

The Docker CLI talks to the Docker daemon. If the daemon is unavailable, commands that manage images and containers fail.

```text
docker CLI -> Docker API socket -> Docker daemon -> container runtime/image store
```

---

## 2. Environment Checks

```bash
docker version
docker info
docker context ls
docker system df
docker login REGISTRY
```

What to check:

- client version
- server/daemon version
- current Docker context
- storage driver
- rootless/rootful mode
- registry login status

---

## 3. Docker Desktop vs Docker Engine

| Environment | Notes |
|---|---|
| Docker Desktop | common on macOS/Windows, runs Linux containers through a VM |
| Docker Engine | native Linux daemon/server environment |
| remote context | CLI targets remote daemon/context |
| rootless Docker | daemon runs without root privileges, with limitations |

---

## 4. Registry Basics

Images are usually named like:

```text
registry.example.com/team/app:1.2.3
```

Parts:

- registry host
- namespace/project
- repository/image name
- tag
- digest

---

## 5. Failure Modes

- Docker daemon not running
- CLI connected to wrong context
- registry auth expired
- image pull blocked by network/proxy
- architecture mismatch, such as arm64 vs amd64
- Docker Desktop VM out of disk or memory

---

## 6. Interview Summary

```text
Docker commands usually talk to a daemon through an API socket or context. I first check docker version, docker info, current context, daemon health, registry authentication, and platform architecture when Docker commands fail unexpectedly.
```

---

## 7. Revision Notes

- One-line summary: Docker CLI success depends on daemon, context, registry, and platform.
- Three keywords: daemon, context, registry.
- One trap: debugging the wrong Docker context or remote daemon.