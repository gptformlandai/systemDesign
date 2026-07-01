# Project 03: Secure Multi-Tenant GraphQL API

## Outcome

Design a GraphQL API that prevents cross-tenant and nested field data leaks.

## Deliverables

- auth context model
- field/object authorization policy
- tenant-scoped data-source filters
- request-scoped loader plan
- safe error contract
- negative test cases

## Acceptance Criteria

- nested fields enforce authorization
- list fields filter by tenant/user scope
- loader cache cannot cross users or tenants
- errors do not reveal sensitive object existence
- tests cover unauthorized nested selection

## Interview Proof

```text
I can secure GraphQL beyond the top-level endpoint by enforcing field/object auth and tenant scope throughout resolver execution.
```