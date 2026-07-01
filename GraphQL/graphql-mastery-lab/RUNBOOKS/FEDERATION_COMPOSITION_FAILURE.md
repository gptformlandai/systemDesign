# Runbook: Federation Composition Failure

## Symptoms

- supergraph composition fails
- router deploy blocked
- operation fails after subgraph deploy

## Evidence

- composition error
- changed subgraph schema
- affected entity/type/field
- router query plan
- subgraph health
- owner/team change log

## Mitigate

- rollback subgraph schema
- publish previous known-good supergraph
- patch conflicting field or entity key
- pause rollout until composition passes

## Prevent

- composition checks in CI
- subgraph ownership rules
- stable entity keys
- query-plan review for risky fields