# Docker Volumes, Bind Mounts, and Persistence - Gold Sheet

> Track File #7 of 30 - Group 02: Intermediate Practical
> For: stateful container practice | Level: intermediate | Mode: storage and mounts

## 1. Core Idea

Container writable layers are disposable. Persistent state should live in volumes, bind mounts, or external services.

```text
container filesystem = image layers + writable layer + mounts
```

---

## 2. Storage Types

| Type | Use | Risk |
|---|---|---|
| named volume | Docker-managed persistent data | ownership/backup visibility |
| bind mount | host path mounted into container | host coupling, permission mismatch |
| tmpfs | in-memory temporary data | lost on restart |
| external storage | database/object store/network storage | network/credential dependency |

---

## 3. Commands

```bash
docker volume ls
docker volume inspect VOLUME
docker run -v app-data:/data IMAGE
docker run -v "$PWD":/app IMAGE
docker inspect CONTAINER --format '{{json .Mounts}}'
```

---

## 4. Ownership And Permissions

Mounts expose Linux permission issues:

```text
container user UID/GID + host path owner/mode = allow or deny
```

Common checks:

```bash
docker exec CONTAINER id
docker exec CONTAINER ls -la /data
ls -la ./host-path
```

---

## 5. Failure Modes

- data lost because it was written to writable layer
- bind mount hides files copied during image build
- app cannot write because UID/GID mismatch
- volume removed during cleanup
- database container used as production database without backup strategy

---

## 6. Interview Summary

```text
For Docker persistence, I avoid storing important data only in the container writable layer. I use named volumes, bind mounts, or external storage depending on the workflow, then verify ownership, backup, restore, and cleanup behavior.
```

---

## 7. Revision Notes

- One-line summary: Containers are disposable; data needs an intentional home.
- Three keywords: volume, mount, UID.
- One trap: bind mounting over a directory and hiding files that were baked into the image.