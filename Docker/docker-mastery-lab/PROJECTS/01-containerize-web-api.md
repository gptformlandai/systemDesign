# Project 01: Containerize A Web API

## Outcome

Build a production-shaped Docker image for a web API.

## Deliverables

- Dockerfile
- `.dockerignore`
- local build command
- local run command
- health check
- non-root runtime user
- image size note
- README explaining env vars and ports

## Acceptance Criteria

- `docker build` succeeds from a clean checkout
- app listens on `0.0.0.0`
- logs go to stdout/stderr
- health endpoint works through published host port
- no secrets exist in image history

## Interview Proof

```text
I can explain build context, layers, port publishing, runtime config, non-root execution, health checks, and logs.
```