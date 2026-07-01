# Runbook: GraphQL Transport Status And Client Failure

## Symptoms

- client treats HTTP 200 as success despite GraphQL errors
- CDN caches wrong GraphQL response
- persisted operation not found
- validation errors spike after client release
- mutation retries create duplicates

## Evidence

- HTTP status
- GraphQL `errors` and `data`
- error path and `extensions.code`
- operation name/hash
- variables and client version
- persisted operation registry status
- cache key and relevant headers
- mutation idempotency key

## Check

- does client inspect GraphQL errors?
- does cache key include operation, variables, and auth/public scope?
- is operation registered/persisted?
- did schema/codegen drift from client operations?
- can mutation be safely retried?

## Mitigate

- patch client error handling
- purge unsafe CDN/server cache entry
- register missing persisted operation
- rollback schema/client mismatch
- dedupe mutation by idempotency key
- disable cache for private operation until key is fixed

## Prevent

- contract tests for active operations
- generated types in CI
- persisted operation deployment gate
- transport/error handling test cases
- idempotency policy for retryable mutations
- operation-aware cache review

## Interview Summary

```text
For GraphQL transport/client failures, I inspect both HTTP and GraphQL errors, operation identity, variables, cache key, persisted registry state, client version, and mutation idempotency before changing schema or resolvers.
```