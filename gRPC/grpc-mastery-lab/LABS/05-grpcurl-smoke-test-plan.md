# Lab 05: grpcurl Smoke Test Plan

## Task

Write commands for a service running on `localhost:50051`.

Required checks:

- list services
- describe the target service
- call a unary method
- pass metadata
- call with local proto when reflection is disabled
- test an invalid request
- test wrong method name

## Template

```bash
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 describe package.v1.Service
grpcurl -plaintext -d '{}' localhost:50051 package.v1.Service/Method
```

## Done When

You can use grpcurl to prove reachability, method shape, and status behavior.