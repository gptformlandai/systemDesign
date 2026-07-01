# Docker Compose: Services, Environment, Dependencies - Gold Sheet

> Track File #9 of 30 - Group 02: Intermediate Practical
> For: multi-container local systems | Level: intermediate | Mode: Compose workflow

## 1. Core Idea

Docker Compose describes multi-container applications as services, networks, volumes, and configuration.

```text
compose.yaml -> services + networks + volumes -> docker compose up
```

---

## 2. Minimal Example

```yaml
services:
  web:
    image: nginx:alpine
    ports:
      - "8080:80"
```

Commands:

```bash
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
```

---

## 3. Compose Concepts

| Concept | Meaning |
|---|---|
| service | container template managed by Compose |
| project | namespace for service/network/volume names |
| default network | services can reach each other by service name |
| environment | runtime variables for containers |
| depends_on | startup ordering, not full readiness by itself |
| healthcheck | command to report container health |

---

## 4. Production-Like Checks

```bash
docker compose config
docker compose ps
docker compose logs SERVICE
docker compose exec SERVICE sh
docker compose down -v  # removes named volumes, use carefully
```

---

## 5. Failure Modes

- `depends_on` starts database before it is ready
- `.env` values differ between developer machines and CI
- `down -v` removes important volumes
- service names work inside Compose network but not from host
- bind mount hides files from image

---

## 6. Interview Summary

```text
Docker Compose is useful for local and small multi-container workflows. I treat services, networks, volumes, env vars, health checks, and dependencies explicitly. I do not confuse startup order with readiness, and I validate Compose config before relying on it.
```

---

## 7. Revision Notes

- One-line summary: Compose is a declarative local multi-container workflow.
- Three keywords: service, network, volume.
- One trap: assuming `depends_on` means the dependency is ready to accept traffic.