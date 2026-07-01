# Project 04: Schema Governance Pipeline

## Outcome

Create a governance plan for safe schema evolution.

## Deliverables

- schema diff process
- breaking-change rule list
- deprecation policy
- operation usage tracking plan
- owner review checklist
- client migration template

## Acceptance Criteria

- additive changes are separated from breaking changes
- deprecated fields include reason and migration path
- schema checks run before deploy
- client usage is checked before removal
- owners approve risky changes

## Interview Proof

```text
I can evolve a GraphQL schema safely using compatibility checks, deprecation, telemetry, and ownership review.
```