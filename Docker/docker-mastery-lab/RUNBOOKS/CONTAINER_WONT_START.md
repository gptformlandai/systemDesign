# Runbook: Container Won't Start

## Symptoms

- container exits immediately
- restart count increases
- service never becomes healthy

## Evidence Commands

```bash
docker ps -a
docker logs CONTAINER --tail 200
docker inspect CONTAINER
docker inspect CONTAINER --format '{{.State.ExitCode}} {{.State.OOMKilled}}'
```

## Check

- command and entrypoint
- required env vars
- file permissions
- missing files from build context
- bind mount hiding files
- dependency readiness
- OOMKilled state

## Mitigate

- restore missing config
- fix command or entrypoint
- correct permissions in image
- rebuild image
- rollback to previous digest

## Prevent

- add smoke test in CI
- validate required env vars on startup
- add health checks
- document run command and config