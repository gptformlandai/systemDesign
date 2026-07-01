# Docker Performance, Resource Limits, cgroups, and Health Checks - MAANG Sheet

> Track File #12 of 30 - Group 03: Senior Production
> For: production container interviews | Level: senior | Mode: resources and reliability

## 1. Core Idea

Containers are Linux processes controlled by cgroups and namespaces. Docker resource settings become kernel-enforced runtime limits.

```text
container process -> cgroup limits -> CPU/memory/I/O behavior -> health/restart policy
```

---

## 2. Resource Controls

| Resource | Docker Option | Risk |
|---|---|---|
| memory | `--memory` | OOM kill if too low |
| CPU quota | `--cpus` | throttling and latency spikes |
| CPU shares | `--cpu-shares` | relative weight under contention |
| process count | `--pids-limit` | fork/thread failures |
| file descriptors | `--ulimit nofile=...` | connection/file open failures |
| restart | `--restart` | restart loops if root cause remains |

---

## 3. Commands

```bash
docker stats
docker inspect CONTAINER --format '{{json .HostConfig}}'
docker events --since 10m
docker logs CONTAINER --tail 100
docker inspect CONTAINER --format '{{.State.OOMKilled}}'
```

---

## 4. Health Checks

Dockerfile example:

```Dockerfile
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/health || exit 1
```

Health checks should be:

- fast
- deterministic
- dependency-aware but not too expensive
- meaningful for traffic readiness

---

## 5. Failure Modes

- memory limit too low causes OOM kills
- CPU throttling hides behind host CPU averages
- health check flaps because it is too strict
- restart policy causes endless crash loop
- no resource limit lets one container starve the host

---

## 6. Interview Summary

```text
Docker containers are Linux processes governed by cgroups, so I set memory, CPU, PID, and ulimit controls intentionally. I monitor docker stats, OOMKilled state, logs, events, and health checks, then tune limits based on real workload behavior.
```

---

## 7. Revision Notes

- One-line summary: Docker resource behavior is cgroup behavior.
- Three keywords: cgroup, limit, healthcheck.
- One trap: checking only host CPU while the container is throttled by its own CPU quota.