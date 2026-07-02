# Docker Advanced Storage: Overlay2, Tmpfs, Volumes - MAANG Sheet

> Track File #38 of 40 - Group 03: Senior Production
> For: senior debugging, SRE, and platform interviews | Level: senior | Mode: container storage

## 1. Intuition

Docker storage has two worlds:

```text
image/container filesystem for disposable runtime state
volumes/bind mounts/external stores for persistent state
```

Senior storage maturity is knowing which bytes are disposable, which bytes are durable, and which cleanup command can destroy them.

---

## 2. Definition

- Definition: Docker storage is the combination of image layers, writable container layers, storage drivers, volumes, bind mounts, tmpfs mounts, and external storage integrations.
- Category: container runtime persistence.
- Core idea: container writable layers are not a database strategy; durable data must be explicitly mounted, backed up, and owned.

---

## 3. Why It Exists

Containers need fast disposable filesystems for app runtime and reliable mounts for state.

Without clear storage boundaries:

- container deletion loses data
- logs and writable layers fill disks
- bind mounts behave differently across platforms
- UID/GID mismatches break apps
- `down -v` or prune commands delete critical data

---

## 4. Storage Map

| Storage Type | Lifetime | Strong Fit |
|---|---|---|
| image layer | immutable, shared | packaged app and dependencies |
| writable container layer | container lifetime | ephemeral runtime changes |
| named volume | independent of container | local persistent state |
| bind mount | host path lifetime | source code/dev config |
| tmpfs | memory-backed, container lifetime | temp files, secrets, scratch space |
| external storage | platform lifetime | production databases, object stores, network volumes |

---

## 5. Overlay2 Mental Model

Overlay-style storage presents multiple image layers as one merged filesystem and adds a writable upper layer for each running container.

```text
lower image layers + upper writable layer -> merged container view
```

Important implications:

- writing to a file from an image layer can copy data into the writable layer
- large writes in container layer can create disk pressure
- deleting a file from an image layer in a later layer does not necessarily remove bytes from earlier layers
- production apps should not depend on container writable layer for durable state

---

## 6. Mount Types

### Named Volume

```bash
docker volume create pg_data
docker run --rm \
  --mount type=volume,src=pg_data,dst=/var/lib/postgresql/data \
  postgres:16
```

### Bind Mount

```bash
docker run --rm \
  --mount type=bind,src="$PWD",dst=/workspace,readonly \
  alpine ls /workspace
```

### Tmpfs

```bash
docker run --rm \
  --read-only \
  --tmpfs /tmp:rw,noexec,nosuid,size=64m \
  my-api:local
```

---

## 7. Compose Storage Example

```yaml
services:
  db:
    image: postgres:16
    volumes:
      - pg_data:/var/lib/postgresql/data

  api:
    image: app/api:local
    read_only: true
    tmpfs:
      - /tmp:size=64m,noexec,nosuid
    volumes:
      - type: volume
        source: app_cache
        target: /app/cache

volumes:
  pg_data:
  app_cache:
```

---

## 8. Backup and Restore Pattern

Named volume backup example:

```bash
docker run --rm \
  -v pg_data:/data:ro \
  -v "$PWD/backups":/backup \
  alpine tar czf /backup/pg_data.tgz -C /data .
```

Restore example:

```bash
docker volume create pg_data_restore
docker run --rm \
  -v pg_data_restore:/data \
  -v "$PWD/backups":/backup:ro \
  alpine sh -c "cd /data && tar xzf /backup/pg_data.tgz"
```

Production note:

For real databases, prefer database-native backups and restore drills. Filesystem volume backups are useful for local labs, simple services, or cold backup workflows.

---

## 9. Ownership and Permissions

The container process UID must be able to read/write mounted paths.

Checklist:

- know the UID/GID used by the image
- avoid `chmod 777` as a default fix
- use `COPY --chown` at build time
- initialize named volumes with correct ownership
- document host path ownership for bind mounts
- consider user namespace remapping effects

Debug:

```bash
docker exec CONTAINER id
docker exec CONTAINER stat -c '%u:%g %a %n' /path
docker volume inspect VOLUME
```

---

## 10. SELinux and Bind Mounts

On SELinux-enabled hosts, bind mounts may need proper labels. Common symptoms look like ordinary permission denied errors even when Unix ownership seems correct.

Senior debugging posture:

```text
Check Unix permissions, container user, mount type, and host security labels before changing app code.
```

---

## 11. Docker Desktop Storage Boundary

On macOS and Windows, Docker Desktop runs Linux containers inside a VM. Bind-mounted source code crosses a host-to-VM filesystem boundary.

Consequences:

- file watching may be slower
- metadata operations can be expensive
- native dependencies may not match host architecture
- Desktop resource limits can cap disk, CPU, or memory

Use named volumes for dependency directories like `node_modules` when host/container platform mismatch hurts performance.

---

## 12. Safe Cleanup Decision Map

| Command | Risk |
|---|---|
| `docker container prune` | removes stopped containers |
| `docker image prune` | removes dangling images |
| `docker builder prune` | removes build cache |
| `docker volume prune` | can delete unused persistent data |
| `docker system prune -a --volumes` | broad destructive cleanup |

Rule:

```text
Never run volume prune or `down -v` until you know which data must survive.
```

---

## 13. Failure Modes

| Symptom | Likely Cause | Fix |
|---|---|---|
| data lost after container recreate | data was in writable layer | use named volume/external storage |
| permission denied | UID/GID or SELinux mismatch | inspect user, ownership, labels |
| disk fills | logs, build cache, writable layers, volumes | classify with `docker system df -v` |
| app slow on Mac/Windows | bind mount file sharing overhead | named volumes, watch sync, reduce metadata churn |
| read-only app crashes | app writes `/tmp` or cache | add tmpfs or explicit writable volume |
| backup useless | no restore drill | practice restore and document RPO/RTO |

---

## 14. Scenario

- Product / system: local Postgres and application cache for a developer platform.
- Why advanced storage fits: DB must persist, cache can be recreated, source bind mount should not own dependencies.
- What would go wrong without it: accidental data deletion, permission drift, and slow development loops.

---

## 15. Strong Answer

I separate immutable image layers, disposable container writable layers, and durable state. Databases use named volumes locally and managed/external storage in production. Runtime containers should be read-only where practical with tmpfs for temp paths. I debug ownership with UID/GID and mount inspection, account for SELinux and Desktop VM boundaries, and write backup/restore procedures before relying on a volume. Cleanup commands must classify image, cache, container, log, and volume usage before deleting anything.

---

## 16. Revision Notes

- One-line summary: Docker storage mastery means knowing what is disposable, durable, mounted, backed up, and safe to prune.
- Three keywords: overlay2, volume, tmpfs.
- One interview trap: treating container writable layers as persistent storage.
- One memory trick: "image is packaged, layer is disposable, volume is deliberate."
