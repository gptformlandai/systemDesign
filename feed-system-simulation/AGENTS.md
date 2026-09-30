# Agent instructions — Feed System Simulation

This file gives the Cursor agent persistent context about this repository.

## Project summary

Spring Boot (WebFlux) + Kafka simulation of social feed distribution:

- **Push** — fanout-on-write into `feed_inbox`
- **Pull** — fanout-on-read from followed timelines
- **Hybrid** — push for normal users, pull for celebrities

## Key paths

| Area | Path |
|------|------|
| API controllers | `src/main/java/com/systemdesign/feed/controller/` |
| Fanout / feed logic | `src/main/java/com/systemdesign/feed/service/` |
| Kafka config | `src/main/java/com/systemdesign/feed/config/KafkaConfig.java` |
| Schema | `src/main/resources/schema.sql` |
| Architecture notes | `README.md`, `implementation_plan.md` |

## Common commands

```bash
# Start dependencies (Postgres, Kafka)
docker compose up -d

# Run the app
mvn spring-boot:run

# Run tests
mvn test

# Build
mvn package -DskipTests
```

## Conventions for agents

- Prefer minimal, focused diffs — this is a learning/simulation project, not production.
- Keep reactive (WebFlux) patterns when touching controllers or services.
- Do not commit secrets or change `application.yml` credentials without asking.
- When changing feed strategy behavior, update `README.md` if the API or semantics change.
- Run `mvn test` after non-trivial Java changes when feasible.

## When to use project skills / subagents

- **run-simulation** skill — starting the stack, hitting simulation endpoints, comparing PUSH/PULL/HYBRID metrics.
- **feed-architect** subagent — deep architecture questions or trade-off analysis across the three feed models.
