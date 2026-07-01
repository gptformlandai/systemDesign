# GraphQL Senior Answer Patterns

## Pattern 1: Explain A Concept

```text
definition -> schema object -> operation behavior -> resolver/data-source behavior -> failure mode -> production guardrail
```

## Pattern 2: Debug An Incident

```text
scope -> operation name/hash -> field path -> resolver trace -> data-source calls -> auth/cache scope -> mitigation -> prevention
```

## Pattern 3: Design A GraphQL API

```text
domain model -> SDL -> operations -> resolver map -> auth -> pagination -> performance -> observability -> evolution
```

## Pattern 4: Security Answer

```text
identity -> field/object authorization -> tenant-scoped data access -> scoped loaders/cache -> complexity/rate limits -> safe errors
```

## Pattern 5: Federation Answer

```text
subgraph ownership -> entity keys -> composition checks -> router query plans -> subgraph SLOs -> rollback plan
```