# Lab 03: Map Status Codes

## Task

Map each failure to a canonical gRPC status.

| Failure | Status | Client Behavior |
|---|---|---|
| malformed item id | | |
| item not found | | |
| caller has no tenant access | | |
| missing auth token | | |
| reservation conflict | | |
| inventory database unavailable | | |
| caller deadline expired | | |
| server panic | | |

## Discussion

For each retryable-looking status, decide whether the method is idempotent.

## Done When

You can defend each status code and client recovery path.