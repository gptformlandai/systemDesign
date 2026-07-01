# GraphQL Error And Observability Map

## Error Map

| Error | Evidence | Common Fix |
|---|---|---|
| parse | invalid document | fix query syntax |
| validation | field/type mismatch | align operation with schema |
| authn | no/invalid identity | refresh token/session |
| authz | forbidden field/object | fix policy or caller access |
| null bubbling | non-null field failed | revisit resolver/nullability/upstream |
| upstream | data source error | retry, fallback, rollback, circuit breaker |
| complexity | query too expensive | reduce query or adjust policy |

## Observability Fields

- operation name
- operation hash
- client name/version
- field path
- resolver latency
- data-source call count
- error code
- error path
- complexity/depth
- tenant/user scope without sensitive data

## Senior One-Liner

```text
GraphQL observability must move from one `/graphql` endpoint to operation and resolver evidence.
```