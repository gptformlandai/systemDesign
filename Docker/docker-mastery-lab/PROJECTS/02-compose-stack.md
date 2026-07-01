# Project 02: Compose Multi-Service Stack

## Outcome

Create a local multi-service app using Docker Compose.

## Deliverables

- `compose.yaml`
- app service
- database or cache service
- named volume for state
- user-defined network
- health checks
- `.env.example`

## Acceptance Criteria

- `docker compose config` succeeds
- `docker compose up -d` starts all services
- app connects by service name, not hardcoded container IP
- state survives container recreation
- cleanup steps distinguish `down` from `down -v`

## Interview Proof

```text
I can explain Compose services, network DNS, volumes, env vars, health checks, and why depends_on is not full readiness.
```