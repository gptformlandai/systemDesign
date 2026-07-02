# Project 06: Runnable Java And Go gRPC Services

## Build

Create one runnable implementation of `payments.v1.PaymentService` in Java or Go, then optionally implement the second language for polyglot practice.

Required:

- shared `payment.proto`
- Buf lint and generation config
- unary `GetPayment`
- side-effecting `CapturePayment` with idempotency key
- server-streaming `WatchPayments`
- client with deadline
- grpcurl smoke-test commands
- health service
- reflection only for local/dev

## Production Add-Ons

- TLS or local mTLS simulation
- interceptor for auth, metrics, tracing, and safe logging
- graceful shutdown test
- stream cancellation test
- retry/idempotency test
- Dockerfile and compose file
- README with status-code map

## Done When

You can run:

```bash
buf lint
buf generate
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext -d '{"paymentId":"pay_123"}' localhost:50051 payments.v1.PaymentService/GetPayment
```

And you can explain:

- why `CapturePayment` needs an idempotency key
- how deadlines stop resource leaks
- why stream cancellation must be handled
- when reflection should be disabled
- how proto evolution is protected

## Portfolio Evidence

Include:

- file tree
- run commands
- test results
- grpcurl output
- one failure demo for `INVALID_ARGUMENT`
- one failure demo for `DEADLINE_EXCEEDED` or `CANCELLED`
- production readiness checklist result

