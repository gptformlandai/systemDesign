# 01. gRPC Mental Model: Proto, Service, Stubs, HTTP/2

## Core Idea

gRPC is typed remote procedure call infrastructure.

```text
.proto file -> generated client/server stubs -> channel -> HTTP/2 stream -> server handler -> status/trailers
```

The `.proto` is the contract. Generated stubs make the contract usable in application code. HTTP/2 carries the actual call. Status codes, deadlines, metadata, and trailers make the result operationally visible.

---

## What gRPC Adds

| Problem | gRPC Capability |
|---|---|
| Teams hand-write client SDKs | code generation from proto contracts |
| JSON payloads are large and loosely typed | protobuf binary messages |
| Internal service calls need time budgets | deadlines and cancellation |
| Errors vary per service | canonical status codes |
| Services need streaming | unary, server streaming, client streaming, bidi streaming |
| Polyglot teams need shared APIs | language-neutral schema and generated stubs |

---

## RPC Lifecycle

```text
client constructs request message
client stub serializes protobuf
channel selects connection/subchannel
HTTP/2 stream is opened
metadata is sent as headers
server receives request and invokes handler
handler returns response or error
status and trailers finish the call
client receives message, status, and metadata
```

Important production detail: a gRPC call can fail before application logic runs. DNS, TLS, load balancing, connection pools, flow control, deadlines, proxy settings, and message-size limits can all fail the request.

---

## Proto Service Example

```proto
syntax = "proto3";

package orders.v1;

service OrderService {
  rpc GetOrder(GetOrderRequest) returns (GetOrderResponse);
}

message GetOrderRequest {
  string order_id = 1;
}

message GetOrderResponse {
  string order_id = 1;
  string status = 2;
}
```

Read it as:

- `package orders.v1` creates a versioned API namespace.
- `service OrderService` defines RPC methods.
- `GetOrder` is a unary RPC.
- Request and response messages are stable wire contracts.
- Field numbers, not names, are the durable wire identity.

---

## gRPC vs REST Short Answer

Use gRPC when internal services need typed contracts, generated clients, low-latency binary payloads, streaming, and deadline-aware RPC.

Use REST/HTTP+JSON when public browser-first APIs, human-readable payloads, cache-friendly resources, and broad tool compatibility matter more.

Strong systems often use both: gRPC internally and REST/GraphQL/gateway APIs externally.

---

## Common Beginner Mistakes

| Mistake | Why It Hurts |
|---|---|
| Treating proto as only serialization | misses service contracts, evolution, and governance |
| Ignoring deadlines | callers can hang, queues build, incidents cascade |
| Returning generic errors | clients cannot retry, alert, or recover correctly |
| Breaking field numbers | old and new clients decode messages incorrectly |
| Forgetting proxy behavior | HTTP/2, keepalive, max message size, and timeouts vary |

---

## Interview Sound Bite

gRPC is a typed RPC framework. The proto defines the service and message contract, codegen creates client and server stubs, HTTP/2 carries multiplexed calls, and production correctness depends on deadlines, status codes, metadata, schema evolution, load balancing, security, and observability.

---

## Practice

1. Pick any internal service and write a one-method proto for it.
2. Identify the request, response, status codes, deadline, and auth metadata.
3. Explain which failures happen before the handler and which happen inside the handler.