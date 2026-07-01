# Lab 01: Docker Environment Snapshot

## Goal

Learn how to inspect the Docker environment before debugging containers.

## Commands

```bash
../SCRIPTS/01-docker-snapshot.sh
docker version
docker info
docker context ls
docker system df
```

## Observe

- Docker client and server versions
- current context
- storage driver
- cgroup driver and version
- disk usage from images, containers, volumes, and build cache

## Failure Drill

Answer these questions without guessing:

1. Is the Docker daemon reachable?
2. Which context is active?
3. Which storage driver is used?
4. How much space do images and build cache consume?
5. Are there old stopped containers?

## Interview Takeaway

```text
Before Docker debugging, I verify the daemon, context, storage, cgroup configuration, object inventory, and disk usage. This avoids chasing app problems when the runtime itself is unhealthy.
```