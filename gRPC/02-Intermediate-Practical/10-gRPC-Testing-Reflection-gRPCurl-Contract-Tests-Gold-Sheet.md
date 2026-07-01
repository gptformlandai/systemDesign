# 10. Testing: Reflection, grpcurl, Contract Tests

## Goal

Test gRPC APIs at contract, implementation, and operational layers.

```text
proto lint -> breaking check -> generated code compile -> handler tests -> grpcurl smoke -> contract tests -> observability checks
```

---

## Test Layers

| Layer | What It Proves |
|---|---|
| proto lint | schema style and consistency |
| breaking-change check | old clients remain compatible |
| generated-code compile | codegen is reproducible and buildable |
| handler unit test | business mapping and status codes |
| in-process integration test | client/server behavior without network complexity |
| grpcurl smoke test | service is reachable and method works |
| contract test | service behavior matches agreed examples |
| load/stream test | deadlines, flow control, and resource behavior |

---

## grpcurl Examples

```bash
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 describe orders.v1.OrderService
grpcurl -plaintext \
  -H 'authorization: Bearer test-token' \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder
```

When reflection is disabled, pass proto files explicitly:

```bash
grpcurl -plaintext \
  -proto proto/orders/v1/order.proto \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder
```

---

## Contract Test Cases

For each method, capture:

- valid request and response
- invalid request and expected status
- unauthenticated request and expected status
- unauthorized request and expected status
- not found case
- deadline behavior for slow dependency
- retry/idempotency behavior if applicable
- metadata requirements

---

## Schema CI Gates

Minimum production gate:

```text
buf lint
buf breaking against main
generate stubs
compile generated code
run contract tests
publish versioned schema artifact
```

---

## Test Data Caution

Do not make contract examples depend on live production IDs. Use deterministic fixtures with realistic shapes and stable expected statuses.

---

## Interview Sound Bite

I test gRPC at multiple layers: proto linting and breaking checks for schema safety, generated-code compilation for toolchain safety, handler and integration tests for behavior, grpcurl for operational smoke tests, and contract tests for client/server compatibility.