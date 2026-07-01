# Runbook: GraphQL Schema Breaking Change

## Symptoms

- clients fail after schema deploy
- validation errors spike
- generated types fail in client build

## Evidence

- schema diff
- affected field/type
- operation usage
- client versions
- deploy timestamp
- deprecation history

## Mitigate

- restore field or previous schema
- add compatibility alias field
- rollback resolver semantic change
- coordinate client migration

## Prevent

- breaking-change CI checks
- schema registry
- usage telemetry before removal
- deprecation policy