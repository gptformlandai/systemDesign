# 37. Runnable gRPC Java And Go Lab Guide

## Purpose

This lab closes the gap between reading gRPC notes and proving you can build, run, test, and debug a real service.

Target outcome:

```text
proto -> generated Java/Go code -> server -> client -> grpcurl -> tests -> Docker -> production checklist
```

This guide is intentionally tool-agnostic enough to run in most environments. Use it as the file-by-file blueprint for a portfolio-ready repo.

---

## 1. Service Contract

Create:

```text
proto/payments/v1/payment.proto
```

```proto
syntax = "proto3";

package payments.v1;

option go_package = "github.com/example/grpc-lab/gen/go/payments/v1;paymentspb";
option java_multiple_files = true;
option java_package = "com.example.payments.v1";
option java_outer_classname = "PaymentProto";

import "google/protobuf/timestamp.proto";

service PaymentService {
  rpc GetPayment(GetPaymentRequest) returns (Payment);
  rpc CapturePayment(CapturePaymentRequest) returns (CapturePaymentResponse);
  rpc WatchPayments(WatchPaymentsRequest) returns (stream PaymentEvent);
}

message GetPaymentRequest {
  string payment_id = 1;
}

message CapturePaymentRequest {
  string payment_id = 1;
  int64 amount_cents = 2;
  string idempotency_key = 3;
}

message CapturePaymentResponse {
  Payment payment = 1;
  bool duplicate_request = 2;
}

message WatchPaymentsRequest {
  string merchant_id = 1;
  string resume_token = 2;
}

message Payment {
  string payment_id = 1;
  int64 amount_cents = 2;
  PaymentStatus status = 3;
  google.protobuf.Timestamp created_at = 4;
}

message PaymentEvent {
  string event_id = 1;
  string resume_token = 2;
  Payment payment = 3;
}

enum PaymentStatus {
  PAYMENT_STATUS_UNSPECIFIED = 0;
  PAYMENT_STATUS_AUTHORIZED = 1;
  PAYMENT_STATUS_CAPTURED = 2;
  PAYMENT_STATUS_FAILED = 3;
}
```

Why this contract is useful:

- unary read: `GetPayment`
- side-effecting unary write: `CapturePayment`
- server streaming: `WatchPayments`
- idempotency key
- enum zero value
- well-known type
- package/version/options for generated code

---

## 2. Buf Setup

Create:

```text
buf.yaml
buf.gen.yaml
```

`buf.yaml`:

```yaml
version: v2
modules:
  - path: proto
lint:
  use:
    - STANDARD
breaking:
  use:
    - FILE
```

`buf.gen.yaml`:

```yaml
version: v2
plugins:
  - remote: buf.build/protocolbuffers/go
    out: gen/go
    opt: paths=source_relative
  - remote: buf.build/grpc/go
    out: gen/go
    opt: paths=source_relative
  - remote: buf.build/protocolbuffers/java
    out: gen/java
  - remote: buf.build/grpc/java
    out: gen/java
```

Commands:

```bash
buf lint
buf breaking --against '.git#branch=main'
buf generate
```

If remote plugins are not allowed in your environment, replace them with local `protoc` plugins.

---

## 3. Go Service Shape

Suggested tree:

```text
go-service/
  go.mod
  cmd/server/main.go
  cmd/client/main.go
  internal/payments/service.go
  internal/payments/service_test.go
```

Server responsibilities:

```text
1. Validate request.
2. Map invalid input to INVALID_ARGUMENT.
3. Enforce idempotency key for CapturePayment.
4. Respect context cancellation/deadline.
5. Return NOT_FOUND for missing payment.
6. Stream events with bounded loop and context cancellation.
7. Register health and reflection in local/dev mode.
```

Go handler skeleton:

```go
func (s *Server) CapturePayment(ctx context.Context, req *pb.CapturePaymentRequest) (*pb.CapturePaymentResponse, error) {
    if req.GetPaymentId() == "" || req.GetAmountCents() <= 0 {
        return nil, status.Error(codes.InvalidArgument, "payment_id and positive amount_cents are required")
    }
    if req.GetIdempotencyKey() == "" {
        return nil, status.Error(codes.InvalidArgument, "idempotency_key is required")
    }
    if err := ctx.Err(); err != nil {
        return nil, status.Error(codes.Canceled, "caller canceled before capture")
    }
    return s.captureWithIdempotency(req)
}
```

Go test checklist:

- invalid input returns `INVALID_ARGUMENT`
- missing idempotency key returns `INVALID_ARGUMENT`
- duplicate capture returns same result with `duplicate_request=true`
- not found returns `NOT_FOUND`
- deadline/cancellation stops work
- stream exits when client cancels

---

## 4. Java Service Shape

Suggested tree:

```text
java-service/
  build.gradle
  src/main/java/com/example/payments/PaymentServer.java
  src/main/java/com/example/payments/PaymentServiceImpl.java
  src/main/java/com/example/payments/PaymentClient.java
  src/test/java/com/example/payments/PaymentServiceImplTest.java
```

Java handler responsibilities:

```text
1. Use generated `PaymentServiceGrpc.PaymentServiceImplBase`.
2. Return errors with `Status.INVALID_ARGUMENT.asRuntimeException()`.
3. Use deadlines from client calls.
4. Add server interceptor for request logging and metrics.
5. Register health/reflection in local/dev profile.
6. Shut down server gracefully.
```

Java handler skeleton:

```java
public void getPayment(GetPaymentRequest request, StreamObserver<Payment> responseObserver) {
    if (request.getPaymentId().isBlank()) {
        responseObserver.onError(
            Status.INVALID_ARGUMENT
                .withDescription("payment_id is required")
                .asRuntimeException()
        );
        return;
    }

    Payment payment = repository.find(request.getPaymentId())
        .orElseThrow(() -> Status.NOT_FOUND
            .withDescription("payment not found")
            .asRuntimeException());

    responseObserver.onNext(payment);
    responseObserver.onCompleted();
}
```

Java test checklist:

- in-process server test for unary method
- interceptor adds trace/request id
- deadline test with slow fake dependency
- stream cancellation test
- reflection disabled in prod profile

---

## 5. grpcurl Smoke Tests

Run server on `localhost:50051`.

List services:

```bash
grpcurl -plaintext localhost:50051 list
```

Describe service:

```bash
grpcurl -plaintext localhost:50051 describe payments.v1.PaymentService
```

Call unary:

```bash
grpcurl -plaintext \
  -d '{"paymentId":"pay_123"}' \
  localhost:50051 payments.v1.PaymentService/GetPayment
```

Call side-effecting method:

```bash
grpcurl -plaintext \
  -H 'x-request-id: local-001' \
  -d '{"paymentId":"pay_123","amountCents":1299,"idempotencyKey":"idem_001"}' \
  localhost:50051 payments.v1.PaymentService/CapturePayment
```

Call stream:

```bash
grpcurl -plaintext \
  -d '{"merchantId":"m_123"}' \
  localhost:50051 payments.v1.PaymentService/WatchPayments
```

No reflection mode:

```bash
grpcurl -plaintext \
  -proto proto/payments/v1/payment.proto \
  -d '{"paymentId":"pay_123"}' \
  localhost:50051 payments.v1.PaymentService/GetPayment
```

---

## 6. Docker And Compose Shape

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/payment-service.jar app.jar
EXPOSE 50051
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Compose:

```yaml
services:
  payment-grpc:
    build: .
    ports:
      - "50051:50051"
    environment:
      GRPC_REFLECTION_ENABLED: "true"
      GRPC_HEALTH_ENABLED: "true"
```

Production additions:

- TLS/mTLS certs mounted from secret.
- reflection disabled or protected.
- health service enabled.
- OpenTelemetry exporter configured.
- max message size documented.
- graceful shutdown signal tested.

---

## 7. Load And Failure Tests

Use any available gRPC load tool, such as `ghz`, k6 with gRPC support, or a small custom client.

Test cases:

```text
Unary baseline:
  100 RPS GetPayment for 5 minutes.

Side effect:
  CapturePayment with idempotency keys and forced client retry.

Deadline:
  client deadline 100 ms, fake dependency sleeps 200 ms.

Streaming:
  100 watchers, 10 intentionally slow consumers.

Shutdown:
  run load while restarting server.
```

Pass criteria:

- p95/p99 within target.
- errors are expected status codes.
- retry amplification stays bounded.
- stream memory does not grow unbounded.
- shutdown does not create large `UNAVAILABLE` spike.

---

## 8. Portfolio Evidence

Your final repo should prove:

- `buf lint`, `buf breaking`, and `buf generate` run.
- Java or Go server runs locally.
- client call has deadline.
- grpcurl smoke tests are documented.
- contract tests cover status codes.
- streaming cancellation is tested.
- Docker image runs.
- README explains deadline, status, idempotency, health, reflection, and shutdown.

---

## 9. Interview Walkthrough

Explain the lab in this order:

```text
1. Contract: package, service, methods, messages, enum, field numbers.
2. Generation: Buf/protoc creates stubs.
3. Runtime: server validates, maps errors, observes cancellation.
4. Client: sets deadline and metadata.
5. Debug: grpcurl, reflection, status codes.
6. Production: health, shutdown, idempotency, observability, mTLS.
7. Evolution: lint, breaking checks, reserved fields.
```

If you can do that clearly, you are no longer just "aware of gRPC"; you can operate it.

