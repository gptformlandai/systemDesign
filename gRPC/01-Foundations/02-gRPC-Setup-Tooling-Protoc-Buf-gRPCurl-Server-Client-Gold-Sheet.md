# 02. gRPC Setup And Tooling: protoc, Buf, grpcurl, Server, Client

## Goal

Learn the tools around a gRPC workflow.

```text
write proto -> lint/breaking check -> generate stubs -> run server -> inspect with grpcurl -> call from client
```

---

## Tool Map

| Tool | Purpose |
|---|---|
| `protoc` | protobuf compiler |
| language plugin | generates language-specific code from proto files |
| Buf | proto linting, breaking-change checks, module/dependency management |
| grpcurl | command-line gRPC client, similar spirit to curl |
| reflection | lets tools discover services at runtime |
| Evans | interactive gRPC client shell |
| BloomRPC/Postman | GUI gRPC exploration tools |

---

## Minimal Workflow

```bash
# Compile proto with a language plugin.
protoc --proto_path=. --go_out=. --go-grpc_out=. proto/orders/v1/order.proto

# List services when reflection is enabled.
grpcurl -plaintext localhost:50051 list

# Describe one service.
grpcurl -plaintext localhost:50051 describe orders.v1.OrderService

# Call unary method.
grpcurl -plaintext \
  -d '{"order_id":"o-123"}' \
  localhost:50051 orders.v1.OrderService/GetOrder
```

The exact codegen flags change by language, but the workflow stays the same.

---

## Reflection

Server reflection allows clients like `grpcurl` to discover services and messages without local proto files.

Use reflection for internal development and controlled operations. For hardened production environments, decide intentionally whether reflection should be enabled, restricted, or disabled.

---

## Buf Workflow

```bash
buf lint
buf breaking --against '.git#branch=main'
buf generate
```

Buf is valuable because schema safety needs automation. Humans miss field-number reuse, missing package conventions, inconsistent naming, and breaking changes.

---

## Setup Checklist

- Compiler installed: `protoc` or Buf-managed generation.
- Language plugins installed and versioned.
- Proto files grouped by package/version.
- Generated code path is deterministic.
- Lint and breaking checks run in CI.
- Local server exposes a known port.
- Reflection is available for local debugging.
- grpcurl command examples exist for common methods.

---

## Debugging Setup Problems

| Symptom | Likely Cause | Check |
|---|---|---|
| generated imports fail | wrong output path or package option | inspect generated package/module paths |
| grpcurl cannot list services | reflection disabled or wrong port | verify server logs and reflection config |
| `UNIMPLEMENTED` | wrong fully qualified service/method | use `grpcurl describe` |
| TLS handshake fails | plaintext vs TLS mismatch | compare client flags and server config |
| schema mismatch | stale generated code | regenerate and check CI artifacts |

---

## Interview Sound Bite

A serious gRPC workflow does not stop at `protoc`. It includes proto linting, breaking-change checks, deterministic code generation, reflection or proto-based debugging, grpcurl examples, and CI enforcement so clients and servers do not drift.