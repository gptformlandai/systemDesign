# Project 02: N+1 Detection And DataLoader Fix

## Outcome

Prove and fix a GraphQL N+1 resolver problem.

## Deliverables

- slow nested resolver example
- resolver trace or call-count output
- request-scoped batching fix
- before/after call count
- explanation of auth/cache scope

## Acceptance Criteria

- naive implementation shows fanout
- batched implementation reduces data-source calls
- loader is request-scoped
- loader key respects tenant/auth scope
- metrics prove improvement

## Interview Proof

```text
I can use resolver traces and data-source call counts to prove N+1, then fix it with request-scoped batching.
```