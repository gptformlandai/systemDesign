# Status Code Map Cheatsheet

| Status | Use |
|---|---|
| `OK` | success |
| `INVALID_ARGUMENT` | request is invalid independent of state |
| `NOT_FOUND` | resource does not exist |
| `ALREADY_EXISTS` | create conflict |
| `FAILED_PRECONDITION` | current state blocks operation |
| `ABORTED` | concurrency conflict |
| `RESOURCE_EXHAUSTED` | quota, rate, or capacity limit |
| `UNAUTHENTICATED` | identity missing or invalid |
| `PERMISSION_DENIED` | identity valid but not allowed |
| `UNAVAILABLE` | transient service/connectivity failure |
| `DEADLINE_EXCEEDED` | time budget expired |
| `INTERNAL` | unexpected server bug |
| `UNIMPLEMENTED` | method not implemented or wrong method path |

## Retry Reminder

Only retry when the status is retryable and the method is safe or idempotent.