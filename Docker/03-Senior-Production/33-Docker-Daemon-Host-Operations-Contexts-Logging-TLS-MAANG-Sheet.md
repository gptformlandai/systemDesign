# Docker Daemon and Host Operations: Contexts, Logging, TLS - MAANG Sheet

> Track File #33 of 40 - Group 03: Senior Production
> For: platform, SRE, DevOps, and senior backend interviews | Level: senior | Mode: Docker host operations

## 1. Intuition

The Docker CLI is the steering wheel. The Docker daemon is the engine.

Most beginners debug the container. Senior engineers also debug the host that creates, starts, networks, stores, logs, and kills containers.

```text
docker CLI -> context -> daemon socket/API -> containerd/runtime -> namespaces/cgroups/storage/network
```

---

## 2. Definition

- Definition: The Docker daemon is the long-running service that manages Docker objects and exposes the Docker Engine API.
- Category: host-level container platform control plane.
- Core idea: every `docker` command affects whichever daemon your current Docker context points to.

---

## 3. Why It Exists

Containers need privileged host work: namespaces, cgroups, networking, storage mounts, image pulls, and process supervision. The daemon centralizes that work behind an API.

Without daemon awareness, a senior engineer can misdiagnose:

- wrong machine or wrong context
- remote daemon security exposure
- log disk pressure
- Docker Desktop VM limits
- daemon restart impact
- image pull failures caused by proxy or registry configuration

---

## 4. Host Operations Map

| Area | What To Know |
|---|---|
| context | which daemon the CLI targets |
| socket | local Unix socket or remote TCP endpoint |
| `daemon.json` | host-level defaults for logging, registry, live restore, storage, metrics, proxies |
| logging drivers | where container stdout/stderr goes |
| live restore | keep containers running during daemon downtime in supported cases |
| proxy | daemon pulls/builds may need different proxy config than shell commands |
| registry mirrors | reduce pull latency and rate-limit pain |
| rootless/rootful | different privilege and feature boundaries |
| Desktop VM | macOS/Windows containers run through a Linux VM layer |

---

## 5. How It Works

1. CLI resolves the active context.
2. CLI sends API request over socket, SSH, or TCP/TLS.
3. Daemon validates request and policy.
4. Daemon interacts with image store, network drivers, volume drivers, and runtime.
5. Runtime starts or stops the container process.
6. Logs stream through configured driver.
7. Daemon persists metadata under Docker's data root.
8. Systemd or Docker Desktop supervises daemon lifecycle.

Failure path:

```text
wrong context -> correct command hits wrong daemon -> "missing image/container/network" confusion
```

Recovery path:

```bash
docker context ls
docker context show
docker info
docker version
```

---

## 6. Command Map

```bash
docker context ls
docker context show
docker context use default
docker context inspect default

docker version
docker info
docker system df
docker system events

docker inspect CONTAINER --format '{{.HostConfig.LogConfig.Type}}'
docker logs --tail 100 CONTAINER

journalctl -u docker --since "1 hour ago"
systemctl status docker
```

Remote context example:

```bash
docker context create prod-host --docker "host=ssh://docker-admin@example.com"
docker --context prod-host ps
```

Prefer SSH contexts for many admin workflows. Exposing the daemon over TCP requires explicit TLS protection and tight network access.

---

## 7. `daemon.json` Example

```json
{
  "log-driver": "local",
  "log-opts": {
    "max-size": "10m",
    "max-file": "5"
  },
  "live-restore": true,
  "registry-mirrors": ["https://mirror.example.internal"],
  "features": {
    "buildkit": true
  },
  "metrics-addr": "127.0.0.1:9323",
  "experimental": false
}
```

Notes:

- `json-file` without rotation can fill disks.
- daemon config changes usually require daemon reload or restart.
- do not change production daemon defaults casually; it affects every container on that host.

---

## 8. Logging Drivers

| Driver | Typical Use |
|---|---|
| `local` | safer local default with rotation support |
| `json-file` | common default, easy `docker logs`, but needs rotation |
| `journald` | Linux hosts using systemd/journal |
| `syslog` | legacy centralized logging |
| `fluentd`, `gelf`, `splunk`, cloud drivers | centralized logging pipelines |

Interview maturity:

```text
I choose logging drivers intentionally, verify whether `docker logs` still works, and configure rotation before production traffic.
```

---

## 9. Remote Daemon Security

The Docker API is powerful enough to start privileged containers, mount host paths, and control workloads. Treat daemon access like root-equivalent infrastructure access.

Do:

- prefer SSH contexts or mutually authenticated TLS
- restrict network access to daemon endpoints
- avoid unauthenticated TCP sockets
- audit group membership for `docker`
- never mount `/var/run/docker.sock` into untrusted containers

Do not:

- expose `tcp://0.0.0.0:2375`
- let CI jobs mount the host socket unless the job is fully trusted
- assume "inside a container" makes socket access safe

---

## 10. Failure Modes

| Symptom | Host-Level Cause | Evidence |
|---|---|---|
| command shows no containers | wrong context | `docker context show` |
| container logs missing | remote logging driver or rotation | `docker inspect` log config |
| disk fills | image/cache/log/volume growth | `docker system df -v`, data root usage |
| pulls fail in CI | daemon proxy or registry auth missing | daemon logs, `docker info` |
| restart kills management API | daemon restart without planning | systemd logs, live restore config |
| "works on Linux, slow on Mac" | Desktop VM/file sharing boundary | Desktop settings and bind mount paths |

---

## 11. Scenario

- Product / system: shared CI runner fleet building Docker images.
- Why daemon operations matter: build cache, registry mirrors, log rotation, and daemon proxy settings determine reliability and speed.
- What would go wrong without it: disk pressure, rate limits, leaked daemon sockets, and flaky builds that appear to be app failures.

---

## 12. Practical Question

> Production builds are randomly failing with "no space left on device" and container logs are huge. How do you debug and prevent recurrence?

---

## 13. Strong Answer

I would first confirm the target daemon with `docker context show` and `docker info`, then measure Docker disk usage with `docker system df -v` and host filesystem usage. I would separate image/cache/volume/log growth, inspect the logging driver and rotation settings, prune only safe build cache or stopped objects, and protect named volumes. Prevention would include log rotation, BuildKit cache retention policy, registry cache strategy, scheduled observability, and a runbook that avoids destructive blanket prune commands.

---

## 14. Revision Notes

- One-line summary: Senior Docker debugging includes the daemon, context, socket, logs, storage root, and host lifecycle.
- Three keywords: context, socket, logging.
- One interview trap: debugging the wrong daemon because the active Docker context changed.
- One memory trick: before any serious incident, ask "which daemon am I talking to?"
