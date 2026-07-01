# GraphQL Production Guardrails

## Required Guardrails

- named operations
- request IDs and operation hashes in logs
- field/object authorization
- tenant-scoped data-source access
- max page sizes
- complexity/depth limits
- request timeouts/deadlines
- request-scoped DataLoader
- resolver tracing
- stable error codes
- schema diff checks
- deprecation policy

## Strong Guardrails

- persisted operations
- schema registry
- operation usage telemetry
- operation-aware rate limiting
- field ownership metadata
- federation composition checks
- client code generation

## Incident Rule

```text
Scope by operation name/hash and field path before changing schema, resolver code, or limits.
```