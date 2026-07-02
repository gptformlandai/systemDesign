# Docker Compose Advanced: Profiles, Overrides, Secrets, Watch - MAANG Sheet

> Track File #32 of 40 - Group 03: Senior Production
> For: senior local-platform and developer-experience interviews | Level: senior | Mode: Compose operations

## 1. Intuition

Compose is the local control plane for a multi-service app.

Beginner Compose answers "how do I run web plus database?" Senior Compose answers:

```text
Which services run in each mode, where does config come from, how is readiness proven, and what can destroy data?
```

---

## 2. Definition

- Definition: Docker Compose is a declarative application model for services, networks, volumes, configs, secrets, builds, and development workflows.
- Category: local orchestration and developer platform tooling.
- Core idea: a `compose.yaml` file turns many container commands into one repeatable project lifecycle.

---

## 3. Why It Exists

Without Compose, every developer and CI job must remember long `docker run` commands, network names, env vars, ports, volumes, and startup order.

Compose exists to make multi-container state repeatable:

```text
manual commands -> drifting local environments
compose project -> versioned topology and lifecycle
```

---

## 4. Advanced Compose Concepts

| Concept | Senior Meaning |
|---|---|
| project name | namespace prefix for containers, networks, and volumes |
| profiles | optional service groups for debug, admin, migration, load-test, or tools |
| multiple files | base plus local, test, CI, or observability overrides |
| env precedence | final value depends on CLI, shell, `.env`, `environment`, `env_file`, and image `ENV` |
| secrets/configs | file-backed sensitive or operational config, not image-baked values |
| health conditions | readiness gating when dependency startup order is not enough |
| `develop.watch` | source sync/rebuild workflow for local development |
| `config` | rendered truth after interpolation and file merging |

---

## 5. How It Works

1. Compose chooses a project name from `-p`, `COMPOSE_PROJECT_NAME`, top-level `name`, directory, or project directory.
2. Compose loads one or more files from `-f` flags or default names.
3. Variables are interpolated from shell and env files.
4. Files are merged into one application model.
5. Profiles decide which optional services join the graph.
6. Compose creates networks and volumes.
7. Compose starts services in dependency order.
8. Health checks report readiness, but only configured conditions can gate dependent services.
9. Logs, exec, restart, down, and watch operate inside the project namespace.

---

## 6. Command Map

```bash
docker compose config
docker compose -p payments-dev config
docker compose --profile debug up -d
docker compose --profile "*" config
docker compose -f compose.yaml -f compose.local.yaml up -d
docker compose up --watch
docker compose watch
docker compose ps --format json
docker compose logs -f api
docker compose exec api sh
docker compose down
docker compose down -v
```

Use `docker compose config` before blaming Docker. It shows the rendered model Compose is actually using.

---

## 7. Strong Compose Pattern

```yaml
name: payments

services:
  api:
    build:
      context: .
      target: runtime
    ports:
      - "8080:8080"
    env_file:
      - .env.local
    environment:
      DATABASE_URL: postgres://app:app@db:5432/app
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/health"]
      interval: 10s
      timeout: 3s
      retries: 5
    develop:
      watch:
        - action: sync
          path: ./src
          target: /app/src
        - action: rebuild
          path: ./package-lock.json

  db:
    image: postgres:16
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
      POSTGRES_DB: app
    secrets:
      - db_password
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d app"]
      interval: 5s
      timeout: 3s
      retries: 10

  adminer:
    image: adminer
    profiles: ["debug"]
    ports:
      - "8081:8080"

secrets:
  db_password:
    file: ./secrets/db_password.txt

volumes:
  db_data:
```

---

## 8. Env Precedence Mental Model

Highest to lowest practical precedence:

1. CLI overrides such as `docker compose run -e KEY=value`.
2. Interpolated values from shell or env files used by `environment` / `env_file`.
3. `environment` values in Compose.
4. `env_file` values in Compose.
5. Dockerfile `ENV` defaults.

Interview trap:

```text
.env is not automatically "the container env". It is mainly an interpolation source unless referenced into service environment.
```

---

## 9. When To Rely On It

Use advanced Compose when:

- a feature needs multiple services locally
- tests need realistic dependencies
- developers need optional debug/admin tools
- CI needs a disposable app topology
- a team needs one repeatable local environment
- you want migration or seed jobs as explicit one-off services

---

## 10. When Not To Use It

Do not treat Compose as your full production orchestrator for high-availability systems.

Use Kubernetes, ECS, Nomad, or another orchestrator when you need:

- multi-host scheduling
- autoscaling
- rolling deployments with service discovery
- node health and rescheduling
- mature secret/config policy
- production ingress and traffic shifting

---

## 11. Trade-offs

| Gain | Cost |
|---|---|
| repeatable local systems | YAML merge and env precedence can confuse teams |
| easy service networking | not the same as production networking |
| optional profiles | profile sprawl can hide required dependencies |
| fast dev with watch | requires correct file ownership and ignore rules |
| simple CI dependency setup | weak substitute for production orchestrator semantics |

---

## 12. Failure Modes

| Symptom | Likely Cause | Fix |
|---|---|---|
| works on one machine only | different project name, env file, platform, or bind mount behavior | compare `docker compose config` |
| app starts before database | `depends_on` without health condition | add DB healthcheck and condition |
| debug tool always starts | forgot `profiles` | assign optional tool to profile |
| data disappears | `docker compose down -v` removed named volume | restore backup, document cleanup rules |
| env value is unexpected | precedence mismatch | inspect rendered config and container env |
| watch does nothing | service uses `image` only or container user cannot write target | use `build`, correct `COPY --chown`, verify paths |

---

## 13. Scenario

- Product / system: local payment platform with API, worker, Postgres, Redis, and optional observability.
- Why Compose fits: all developers need the same topology, but only some need debug UI and metrics.
- What would go wrong without it: manual startup order, env drift, broken DNS names, accidental volume deletion, and inconsistent CI setup.

---

## 14. Practical Question

> You are standardizing local development for a microservice that needs Postgres, Redis, a migration job, and optional admin tools. How would you structure Compose?

---

## 15. Strong Answer

I would keep core services unprofiled so `docker compose up` starts the normal app. I would put admin tools, seeders, profilers, and load-test utilities behind profiles. I would use named volumes for state, health checks for DB/Redis readiness, and `depends_on` conditions only where startup truly depends on readiness. I would validate with `docker compose config` in CI, avoid real secrets in files committed to Git, and document that `down -v` is destructive.

---

## 16. Revision Notes

- One-line summary: Senior Compose is about project lifecycle, rendered config, optional service modes, readiness, and data safety.
- Three keywords: profiles, config, readiness.
- One interview trap: assuming `depends_on` alone proves a dependency is ready.
- One memory trick: always ask "which project, which file, which profile, which env source?"
