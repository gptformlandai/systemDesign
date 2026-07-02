# 25. gRPC Commands, Protobuf Cheat Sheet, Decision Map

## grpcurl

```bash
# List services through reflection.
grpcurl -plaintext localhost:50051 list

# Describe a service.
grpcurl -plaintext localhost:50051 describe orders.v1.OrderService

# Call unary RPC.
grpcurl -plaintext \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder

# Call with metadata.
grpcurl -plaintext \
  -H 'authorization: Bearer test-token' \
  -H 'x-request-id: local-test-1' \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder

# Call without reflection by providing proto.
grpcurl -plaintext \
  -proto proto/orders/v1/order.proto \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder
```

---

## Buf

```bash
buf lint
buf breaking --against '.git#branch=main'
buf generate
buf format -w proto
```

---

## protoc Pattern

```bash
protoc --proto_path=. \
  --go_out=. \
  --go-grpc_out=. \
  proto/orders/v1/order.proto
```

Language plugins differ, but the shape is always: include proto path, select output plugin, pass proto files.

---

## grpcui

```bash
# Browser UI for a local/dev service with reflection enabled.
grpcui -plaintext localhost:50051

# Use explicit proto when reflection is disabled.
grpcui -plaintext \
  -proto proto/orders/v1/order.proto \
  localhost:50051
```

Use only against local, dev, or protected environments.

---

## Load And Failure Tests

```bash
# Example shape with ghz.
ghz \
  --insecure \
  --proto proto/payments/v1/payment.proto \
  --call payments.v1.PaymentService.GetPayment \
  -d '{"payment_id":"pay_123"}' \
  -c 20 \
  -n 1000 \
  localhost:50051
```

Measure:

- p95/p99 latency
- status distribution
- retry amplification
- message sizes
- active streams
- slow-consumer behavior

---

## Channel And Gateway Debug Checks

```bash
# DNS target check.
dig payment.prod.svc.cluster.local

# TLS/ALPN check.
openssl s_client -connect api.example.com:443 -alpn h2

# Kubernetes endpoint check.
kubectl get endpointslices -n prod -l kubernetes.io/service-name=payment

# Envoy admin examples, if access is allowed.
curl localhost:15000/clusters
curl localhost:15000/config_dump
```

If Channelz/admin endpoints are enabled, protect them and use them to inspect channel, subchannel, socket, and server state.

---

## Proto Rules

| Need | Rule |
|---|---|
| delete a field | reserve number and name |
| add data | use new field number |
| default enum value | use `*_UNSPECIFIED = 0` |
| incompatible rewrite | create new package/version |
| side-effect method | include idempotency key if retryable |
| large response | paginate, stream, or use field masks |

---

## Status Decision Map

| Situation | Status |
|---|---|
| bad request shape | `INVALID_ARGUMENT` |
| missing resource | `NOT_FOUND` |
| duplicate create | `ALREADY_EXISTS` |
| failed state precondition | `FAILED_PRECONDITION` |
| concurrency conflict | `ABORTED` |
| no/invalid identity | `UNAUTHENTICATED` |
| valid identity lacks access | `PERMISSION_DENIED` |
| capacity/quota exhausted | `RESOURCE_EXHAUSTED` |
| transient unavailable service | `UNAVAILABLE` |
| time budget expired | `DEADLINE_EXCEEDED` |
| unexpected server bug | `INTERNAL` |

---

## RPC Type Decision Map

| Need | RPC Type |
|---|---|
| one request, one response | unary |
| one request, many responses | server streaming |
| many chunks, one result | client streaming |
| ongoing conversation | bidirectional streaming |
| public browser API | gRPC-Web or REST/JSON gateway |
| long-running operation | start operation + poll/get status |

---

## Debugging Decision Map

| Symptom | First Checks |
|---|---|
| cannot connect | DNS, port, TLS/plaintext, proxy, server listening |
| `UNIMPLEMENTED` | package/service/method mismatch, server missing registration |
| `UNAVAILABLE` | endpoints, health, LB, TLS, deployment |
| `DEADLINE_EXCEEDED` | deadline, route timeout, server trace, dependencies, retries |
| streaming stalls | slow consumer, flow control, cancellation, proxy idle timeout |
| wrong data with `OK` | proto compatibility, field numbers, generated code version |
| browser cannot read status | CORS exposed headers, grpc-web filter, gateway error mapping |
| traffic sticks to one backend | channel reuse, LB policy, subchannel state, connection drain |
| rollout breaks streams | health drain, GOAWAY, stream resume, proxy idle/drain timeout |

---

## One-Minute Interview Summary

Use grpcurl/grpcui for reachability and method calls, Buf for lint/breaking/generation, protoc or language plugins for generated code, ghz or a client harness for load tests, canonical status codes for failure contracts, and proto evolution rules to keep old and new clients safe.
