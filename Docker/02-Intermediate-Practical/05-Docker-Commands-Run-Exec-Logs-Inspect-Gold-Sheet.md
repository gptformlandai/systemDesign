# Docker Commands: run, exec, logs, inspect - Gold Sheet

> Track File #5 of 30 - Group 02: Intermediate Practical
> For: daily Docker usage | Level: intermediate | Mode: command fluency

## 1. Core Idea

Daily Docker work is observing and changing Docker objects safely.

```text
image/container/network/volume -> inspect -> logs/stats/exec -> fix or rebuild
```

---

## 2. Command Map

| Goal | Command |
|---|---|
| list running containers | `docker ps` |
| list all containers | `docker ps -a` |
| run container | `docker run` |
| run one-off command | `docker run --rm IMAGE command` |
| shell into running container | `docker exec -it CONTAINER sh` |
| read logs | `docker logs CONTAINER` |
| inspect metadata | `docker inspect CONTAINER` |
| see resource usage | `docker stats` |
| copy files | `docker cp` |
| cleanup stopped containers | `docker container prune` |

---

## 3. Debug Flow

```text
docker ps -a -> docker logs -> docker inspect -> docker exec -> docker stats -> image/Dockerfile fix
```

Useful commands:

```bash
docker ps -a
docker logs --tail 100 CONTAINER
docker inspect CONTAINER
docker exec -it CONTAINER sh
docker stats --no-stream
docker events --since 10m
```

---

## 4. Production Safety

- prefer logs/inspect before `exec` changes
- avoid mutating containers manually; fix image/config instead
- avoid `docker system prune -a` on shared hosts without review
- record container ID, image digest, command, env, mounts, and network

---

## 5. Failure Modes

- container exits immediately because command finishes
- app listens on wrong interface or port
- missing environment variable
- bind mount hides files from image
- manual change inside container disappears after recreation

---

## 6. Interview Summary

```text
For Docker debugging, I start with docker ps -a, logs, inspect, stats, and events. If needed, I exec into the container to inspect runtime state, but I avoid manual fixes inside containers and instead update image, config, mount, network, or environment.
```

---

## 7. Revision Notes

- One-line summary: Docker inspect and logs usually explain the first failure layer.
- Three keywords: logs, inspect, exec.
- One trap: changing a running container manually instead of fixing the build or runtime configuration.