---
name: run-simulation
description: Starts the feed simulation stack, runs simulation endpoints, and compares PUSH/PULL/HYBRID metrics. Use when the user wants to run the app locally, execute a simulation, or benchmark feed strategies.
---

# Run feed simulation

## Prerequisites

- Docker running
- Java 17+ and Maven on PATH

## Workflow

1. Start infrastructure:

```bash
docker compose up -d
```

2. Run the application:

```bash
mvn spring-boot:run
```

3. Trigger simulation (default port 8080):

```bash
# Seed users/follows and run a simulation scenario
curl -s -X POST "http://localhost:8080/api/simulation/run?users=100&posts=50"
```

4. Compare feed strategies for a user:

```bash
curl -s "http://localhost:8080/api/feed/{userId}?strategy=PUSH"
curl -s "http://localhost:8080/api/feed/{userId}?strategy=PULL"
curl -s "http://localhost:8080/api/feed/{userId}?strategy=HYBRID"
```

5. Check metrics if exposed via `SimulationController` / `MetricsTracker`.

## Troubleshooting

- **Kafka connection errors** — wait for containers to be healthy; check `docker compose ps`.
- **Port in use** — stop other Spring apps or change `server.port` in `application.yml`.
- **DB schema** — schema is in `src/main/resources/schema.sql`; Postgres is initialized via compose.
