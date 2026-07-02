# Docker Containerize Web App Scenario - Gold Sheet

> Track File #17 of 40 - Group 04: Scenario Practice
> For: backend interviews and practical Dockerization | Level: intermediate | Mode: app containerization

## 1. Scenario

```text
Containerize a backend or web application for local development and CI/CD.
```

Goal: produce a small, reproducible, secure image that runs the app with correct ports, env vars, health checks, and logs.

---

## 2. Design Steps

```text
runtime needs -> Dockerfile -> .dockerignore -> build -> run -> health check -> logs -> publish
```

Checklist:

- choose correct base image
- copy dependency files before source for cache
- use multi-stage build if compiled or bundled
- expose/document port
- run as non-root where possible
- log to stdout/stderr
- use env vars for runtime config
- avoid baking secrets

---

## 3. Commands

```bash
docker build -t app:local .
docker run --rm -p 8080:8080 --env APP_ENV=local app:local
docker logs CONTAINER
docker inspect CONTAINER
```

---

## 4. Common Mistakes

- copying entire repo before dependency install
- missing `.dockerignore`
- app binds to `127.0.0.1` instead of `0.0.0.0`
- secret copied into image
- root runtime user without reason
- missing health endpoint

---

## 5. Interview Summary

```text
To containerize an app, I identify runtime dependencies, write a cache-aware Dockerfile, use .dockerignore, avoid secrets, run as non-root, expose the right port, log to stdout/stderr, add health checks, and validate with docker build, run, logs, inspect, and curl.
```

---

## 6. Revision Notes

- One-line summary: Good containerization packages runtime behavior, not developer-machine assumptions.
- Three keywords: Dockerfile, port, health.
- One trap: app listens on localhost inside the container and is unreachable from host.