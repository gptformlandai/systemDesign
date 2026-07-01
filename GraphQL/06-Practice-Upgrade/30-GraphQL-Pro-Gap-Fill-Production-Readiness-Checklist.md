# GraphQL Pro Gap-Fill Production Readiness Checklist

> Track File #30 of 30 - Group 06: Practice Upgrade
> For: final review | Level: pro | Mode: readiness checklist

## 1. Schema Readiness

- schema models domain concepts
- naming is consistent
- nullability is intentional
- high-cardinality lists are paginated
- fields have owners
- deprecations include reason and migration path
- breaking-change checks run in CI

## 2. Resolver Readiness

- resolvers are thin and testable
- data access goes through services/data sources
- context includes identity, request ID, and loaders
- DataLoader is request-scoped
- resolver errors use stable codes
- slow fields are traced

## 3. Security Readiness

- authentication is enforced
- field/object authorization exists
- tenant scope is applied near data access
- introspection policy is intentional
- depth/complexity limits exist
- persisted queries or safelisting are considered
- rate limits are operation-aware

## 4. Performance Readiness

- max page size is enforced
- N+1 is tested and monitored
- complexity/depth thresholds exist
- operation names/hashes are logged
- resolver latency and data-source call counts are measured
- expensive fields have ownership and SLO awareness

## 5. Client Readiness

- operations are named
- generated types are used where practical
- normalized cache identity is stable
- error and partial-data behavior is handled
- schema changes are coordinated with client usage

## 6. Federation Readiness

- subgraph ownership is documented
- entity keys are stable
- composition checks block unsafe deploys
- router and subgraph metrics are visible
- fallback/rollback plan exists

## 7. Final Self-Test

You are ready for pro-level GraphQL interviews when you can answer:

```text
How is this schema designed, evolved, authorized, resolved, batched, cached, observed, limited, federated, and debugged under production pressure?
```