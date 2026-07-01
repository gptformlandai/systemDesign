# Generated Code Boundary

Generated gRPC code should be treated as boundary glue.

## Generated Code Owns

- message serialization/deserialization
- typed request/response structures
- client stub methods
- server interface/base types
- service descriptors

## Application Code Owns

- request validation
- deadline and cancellation behavior
- auth and authorization decisions
- business logic
- dependency calls
- status-code mapping
- metrics, tracing, and logging
- idempotency and retry safety

## Review Prompt

For any generated stub integration, ask:

1. Is business logic outside generated code?
2. Are request validation and status codes explicit?
3. Is the caller setting a deadline?
4. Is metadata redacted in logs?
5. Are traces and metrics tagged by method and status?