# 24. gRPC Interview Q&A: Beginner To Pro

## Beginner Questions

### What is gRPC?

gRPC is a typed RPC framework that commonly uses Protocol Buffers for service contracts and HTTP/2 for transport. It generates clients and servers from `.proto` files and supports unary and streaming RPCs.

### What is a `.proto` file?

A `.proto` file defines protobuf messages and gRPC services. It is the API contract shared by clients and servers.

### What is a stub?

A stub is generated code that lets a client call a remote method as a typed local method, or lets a server implement a generated service interface.

---

## Intermediate Questions

### Why does gRPC use HTTP/2?

HTTP/2 supports multiplexed streams over long-lived connections, binary framing, header compression, and streaming. This fits efficient service-to-service RPC.

### What are deadlines?

A deadline is the maximum time budget for an RPC. Callers should set deadlines, servers should observe cancellation, and downstream calls should receive bounded sub-budgets.

### How do you debug `UNAVAILABLE`?

Check name resolution, endpoint health, channel/subchannel state, TLS/plaintext mismatch, proxy/mesh routing, server availability, and deployment changes.

### How do you debug `DEADLINE_EXCEEDED`?

Compare client and server traces, check whether the handler ran, inspect proxy route timeouts, dependency latency, retries, payload size, and recent deployments.

---

## Senior Questions

### How do you evolve protobuf safely?

Add fields with new numbers, reserve deleted numbers/names, avoid changing field meaning or type incompatibly, treat enum zero as unspecified, and enforce lint/breaking checks in CI.

### How do retries work safely?

Retries require a caller deadline, bounded attempts, backoff, retryable status codes, and idempotent methods. Side-effecting calls need idempotency keys or should not be retried automatically.

### How do you secure gRPC?

Use TLS/mTLS for transport and workload identity, validate JWT/OAuth metadata where needed, enforce per-method/resource authorization, rotate certs automatically, and redact sensitive metadata from logs.

### How do you operate gRPC behind Envoy or a service mesh?

Verify HTTP/2 handling, route timeouts, retries, health checks, circuit breaking, mTLS, access logs, tracing, connection draining, and alignment between app deadlines and mesh policy.

---

## System Design Questions

### When would you choose gRPC over REST?

Use gRPC for internal typed service-to-service communication, low-latency binary payloads, generated clients, streaming, deadlines, and polyglot backends. Use REST/JSON for public APIs, browser-native access, and broad human/tool compatibility.

### How would you design a payment API with gRPC?

Use versioned proto packages, unary methods for authorize/capture/get, idempotency keys for side effects, canonical status codes, deadlines, mTLS, per-method authz, contract tests, and reconciliation for timeout ambiguity.

### How would you support browser clients?

Use gRPC-Web through a gateway/proxy or expose REST/JSON transcoding. Native browser gRPC is limited because browsers do not expose full HTTP/2 framing APIs to JavaScript.

---

## Strong Closing Answer

gRPC is not just protobuf. Production gRPC requires contract governance, generated-code hygiene, deadlines, canonical status mapping, retries with idempotency, load balancing for long-lived HTTP/2 connections, mTLS/authz, observability by method/status/latency, and operational runbooks for proxy and schema failures.