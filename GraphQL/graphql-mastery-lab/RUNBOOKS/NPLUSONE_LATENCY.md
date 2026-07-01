# Runbook: GraphQL N+1 Latency

## Symptoms

- one operation becomes slow with larger lists
- data-source call count grows with returned objects
- nested field dominates resolver traces

## Evidence

- operation name/hash
- resolver path latency
- data-source call count
- page size and variables
- recent field/fragment change

## Mitigate

- add request-scoped batching
- cap page size
- optimize batch data-source call
- hide expensive field behind explicit action
- rollback expensive client operation if needed

## Prevent

- resolver call-count tests
- DataLoader standards
- complexity limits
- slow-field dashboards