# Runbook: Volume Permission Or Data Loss

## Symptoms

- permission denied on write
- data disappears after container recreation
- app starts with empty database/cache directory

## Evidence Commands

```bash
docker inspect CONTAINER --format '{{json .Mounts}}'
docker volume ls
docker volume inspect VOLUME
docker exec CONTAINER id
docker exec CONTAINER ls -la /data
```

## Check

- writable layer vs named volume vs bind mount
- `docker compose down -v` history
- host path exists and has expected contents
- UID/GID mismatch
- bind mount hiding image files
- backup availability

## Mitigate

- restore from backup
- fix ownership or runtime user
- recreate container with correct mount
- stop destructive cleanup automation

## Prevent

- document stateful paths
- use named volumes or external storage for persistence
- create backup/restore process
- warn clearly before `down -v`