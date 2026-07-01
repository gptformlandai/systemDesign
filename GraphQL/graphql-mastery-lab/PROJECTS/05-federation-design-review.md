# Project 05: Federation Design Review

## Outcome

Design a federated GraphQL ownership model for multiple teams.

## Deliverables

- subgraph ownership map
- entity key design
- field ownership table
- router query-plan review checklist
- composition gate proposal
- incident rollback plan

## Acceptance Criteria

- each type/field has an owner
- entity keys are stable
- composition checks block incompatible deploys
- router and subgraph metrics are defined
- incident plan restores a known-good supergraph

## Interview Proof

```text
I can explain federation as distributed schema ownership plus runtime query planning, not just schema merging.
```