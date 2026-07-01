# Lab 03: Compose Multi-Service Workflow

## Goal

Use Compose to define, validate, run, observe, and clean up a local service.

## Commands

```bash
cd ../EXAMPLES/hello-web
docker compose config
docker compose up -d --build
docker compose ps
docker compose logs --tail=80
curl http://localhost:8080/health
```

Cleanup:

```bash
docker compose down
```

## Observe

- generated Compose config
- service name
- image name
- health status
- published port
- security options

## Failure Drill

Change the host port from `8080:8080` to `8081:8080`, rerun Compose, and explain why the host URL changes while the container port stays the same.

## Interview Takeaway

```text
Compose is a developer workflow for multi-container definitions. I validate with docker compose config, observe service state with ps/logs, and treat Compose networking, env vars, volumes, and health checks as explicit runtime config.
```