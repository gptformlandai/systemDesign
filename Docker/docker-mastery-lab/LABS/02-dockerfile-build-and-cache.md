# Lab 02: Dockerfile Build And Cache

## Goal

Build the example app and observe Dockerfile layer/cache behavior.

## Setup

```bash
cd ../EXAMPLES/hello-web
docker build -t docker-mastery-hello-web:lab .
docker run --rm -p 8080:8080 docker-mastery-hello-web:lab
```

In another terminal:

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

## Cache Drill

1. Rebuild without changing files.
2. Change `src/server.py` response text.
3. Rebuild and note which steps are cached.
4. Change `.dockerignore` and rebuild.
5. Run `docker history docker-mastery-hello-web:lab`.

## Observe

- build context size
- cached vs rebuilt layers
- final image user
- health check definition
- port binding behavior

## Interview Takeaway

```text
Docker build performance depends on build context and layer ordering. A good Dockerfile keeps dependency layers stable, uses .dockerignore, and separates build-time concerns from runtime behavior.
```