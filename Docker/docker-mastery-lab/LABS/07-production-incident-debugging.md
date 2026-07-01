# Lab 07: Production Incident Debugging

## Goal

Practice evidence-first Docker incident response.

## Scenario

```text
An app container is restarting and users are seeing errors.
```

## Evidence Commands

```bash
docker ps -a
docker logs CONTAINER --tail 200
docker inspect CONTAINER
docker stats --no-stream
docker events --since 30m
docker inspect CONTAINER --format '{{.State.ExitCode}} {{.State.OOMKilled}}'
```

## Debug Questions

1. Which image tag or digest is running?
2. Did the container exit or fail health checks?
3. Was it OOMKilled?
4. Did env, command, entrypoint, mounts, or ports change?
5. Did the host run out of disk, memory, or file descriptors?
6. Is rollback available?

## Mitigation Options

- rollback to previous digest
- restore missing config or secret
- fix bad mount or permission
- scale healthy containers
- tune memory only if evidence supports it
- rebuild and redeploy a corrected image

## RCA Template

```text
Impact:
Trigger:
Detection:
Evidence:
Mitigation:
Prevention:
```

## Interview Takeaway

```text
Senior Docker incident response scopes impact, preserves evidence, checks image identity, logs, inspect output, stats, events, OOM state, host signals, and then mitigates with rollback, config fix, scaling, or resource correction.
```