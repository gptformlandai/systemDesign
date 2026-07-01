# Hello Web Docker Example

This is a tiny Python HTTP service used by the Docker Mastery lab.

## Build

```bash
docker build -t docker-mastery-hello-web:local .
```

## Run With Docker

```bash
docker run --rm -p 8080:8080 --env APP_MESSAGE="Docker mastery lab" docker-mastery-hello-web:local
```

Check it:

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

## Run With Compose

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=80
docker compose down
```

## What This Example Demonstrates

- app binds to `0.0.0.0`
- logs go to stdout
- runtime config comes from env vars
- image runs as non-root user
- health check is defined
- Compose drops capabilities and uses read-only filesystem for the service

## Intentional Limits

This example is intentionally small. It is for Docker behavior practice, not web framework design.