# Docker Observability: Logs, Events, Inspect, and Debugging - Gold Sheet

> Track File #14 of 40 - Group 03: Senior Production
> For: Docker incident response | Level: senior | Mode: evidence gathering

## 1. Core Idea

Docker observability starts with container state, logs, runtime config, resource usage, and daemon events.

```text
container state + logs + inspect + stats + events + host evidence = incident picture
```

---

## 2. Evidence Sources

| Source | What It Shows |
|---|---|
| `docker ps -a` | container state and exit status |
| `docker logs` | stdout/stderr from main process |
| `docker inspect` | config, env, mounts, networks, health, state |
| `docker stats` | resource usage |
| `docker events` | lifecycle and daemon events |
| host logs | kernel OOM, daemon issues, storage/network failures |

---

## 3. Debug Commands

```bash
docker ps -a
docker logs --tail 100 CONTAINER
docker inspect CONTAINER
docker stats --no-stream
docker events --since 10m
docker exec -it CONTAINER sh
```

---

## 4. Crash Loop Flow

```text
ps -a -> exit code -> logs -> inspect command/env/mounts -> resource/OOM -> rebuild or config fix
```

Useful inspect fields:

- `State.ExitCode`
- `State.OOMKilled`
- `Config.Cmd`
- `Config.Entrypoint`
- `Config.Env`
- `Mounts`
- `NetworkSettings`

---

## 5. Failure Modes

- logs missing because app writes only to file inside container
- container exits before you can exec into it
- restart policy hides original crash frequency
- health check reports unhealthy but app still partially serves traffic
- daemon disk usage breaks container operations

---

## 6. Interview Summary

```text
For Docker observability, I gather container state, logs, inspect output, stats, events, and host evidence. I prefer stdout/stderr logging, meaningful health checks, and immutable rebuilds over manual mutation inside a container.
```

---

## 7. Revision Notes

- One-line summary: Docker incident evidence is state, logs, inspect, stats, events, and host signals.
- Three keywords: logs, inspect, events.
- One trap: relying on `docker exec` when the container exits immediately; use logs and inspect first.