# Docker Volume, Permission, and Data Loss Scenario - Gold Sheet

> Track File #20 of 40 - Group 04: Scenario Practice
> For: Docker storage debugging | Level: intermediate | Mode: volumes and persistence

## 1. Scenario

```text
A containerized app cannot write data, or data disappears after container recreation.
```

Goal: identify whether data was in the writable layer, named volume, bind mount, or external service.

---

## 2. Debug Flow

```text
inspect mounts -> container user -> path ownership -> volume lifecycle -> backup/restore plan
```

Commands:

```bash
docker inspect CONTAINER --format '{{json .Mounts}}'
docker volume ls
docker volume inspect VOLUME
docker exec CONTAINER id
docker exec CONTAINER ls -la /data
ls -la ./host-path
```

---

## 3. Common Causes

- data written to container writable layer
- `docker compose down -v` removed volume
- bind mount path wrong or empty
- UID/GID mismatch prevents writes
- bind mount hides files from image
- no backup for stateful volume

---

## 4. Mitigation

- move persistent data to named volume or external datastore
- fix ownership or run user
- restore from backup if available
- stop using `down -v` casually
- document volume backup/restore path

---

## 5. Interview Summary

```text
For Docker data issues, I inspect mounts, distinguish writable layer from volumes and bind mounts, check container user and host path ownership, avoid destructive volume cleanup, and require backup/restore planning for stateful data.
```

---

## 6. Revision Notes

- One-line summary: Container state is disposable unless stored in an intentional persistent layer.
- Three keywords: volume, mount, ownership.
- One trap: running `docker compose down -v` and deleting state unintentionally.