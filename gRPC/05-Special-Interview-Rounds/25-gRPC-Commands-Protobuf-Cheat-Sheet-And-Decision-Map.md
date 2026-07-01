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

---

## One-Minute Interview Summary

Use grpcurl for reachability and method calls, Buf for lint/breaking/generation, protoc or language plugins for generated code, canonical status codes for failure contracts, and proto evolution rules to keep old and new clients safe.