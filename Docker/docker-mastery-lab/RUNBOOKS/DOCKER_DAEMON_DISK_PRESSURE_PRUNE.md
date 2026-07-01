# Runbook: Docker Daemon Disk Pressure And Prune Safety

## Symptoms

- image builds fail with no space left
- containers fail to start
- pulls fail midway
- Docker daemon becomes slow or unhealthy
- host disk is near full

## Evidence Commands

```bash
docker system df
docker system df -v
docker builder du
docker ps -a
docker images
docker volume ls
```

Optional lab helper:

```bash
../SCRIPTS/07-docker-disk-pressure-safe.sh
```

## Check

- build cache size
- stopped containers
- dangling images
- unused old images
- named volumes and ownership
- rollback images that must be retained
- registry availability if local images are removed

## Unsafe Actions

Avoid these until ownership and blast radius are clear:

```bash
docker system prune -a
docker volume prune
docker compose down -v
```

## Safer Mitigation

1. Capture current disk usage and release metadata.
2. Confirm rollback images are available in registry or retained locally.
3. Remove known-disposable stopped containers.
4. Remove dangling images if safe.
5. Clean build cache with an agreed policy.
6. Do not delete volumes unless owner confirms backup or disposability.

## Prevention

- alert on Docker host disk usage
- enforce build-cache retention policy
- keep rollback images in registry
- label important images/containers/volumes
- document cleanup commands and owners
- separate CI build hosts from long-lived runtime hosts when possible

## Interview Summary

```text
For Docker disk pressure, I inspect system df, build cache, stopped containers, images, and volumes before deleting anything. I preserve rollback images and never prune volumes during an incident without backup and ownership confirmation.
```