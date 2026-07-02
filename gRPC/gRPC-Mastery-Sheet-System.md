# gRPC Mastery Sheet System

gRPC mastery means designing typed RPC contracts, generating safe client/server code, operating HTTP/2-based calls under deadlines, and explaining production behavior clearly.

```text
.proto + generated stubs + channel + deadline + metadata + status + gateway + observability = production gRPC service
```

---

## 1. What gRPC Is

gRPC is a high-performance RPC framework that commonly uses Protocol Buffers for contracts and HTTP/2 for transport.

It is strong for service-to-service APIs where teams need typed contracts, generated clients/servers, efficient binary serialization, streaming, deadlines, and consistent error semantics.

---

## 2. Core Mental Model

```text
.proto defines package, messages, services, and methods
codegen creates client/server stubs
client calls method through a channel
HTTP/2 carries frames and multiplexed streams
server handler executes business logic
response returns message or status/error metadata
observability proves latency, status, and dependency behavior
```

---

## 3. Main Objects

| Object | Meaning |
|---|---|
| `.proto` | API contract file |
| message | protobuf data structure |
| field tag | stable numeric field identity on the wire |
| service | collection of RPC methods |
| method | unary or streaming RPC endpoint |
| stub | generated client/server interface |
| channel | client connection abstraction |
| metadata | key/value request or response metadata |
| status | canonical RPC result code and message |
| deadline | max time budget for the RPC |
| interceptor | middleware around client/server calls |
| service config | client policy for timeout/retry/LB/health behavior |
| xDS | dynamic control-plane APIs for routing/LB/security policy |
| gateway | bridge for gRPC-Web, JSON transcoding, auth/rate limits |

---

## 4. Why gRPC Exists

gRPC helps when services need efficient, typed, cross-language RPC.

It solves:

- weak service contracts
- hand-written client SDK drift
- inefficient JSON payloads for internal high-throughput calls
- inconsistent error/status semantics
- lack of deadline and cancellation discipline
- streaming needs in service-to-service systems

It introduces tradeoffs:

- browser support needs gRPC-Web or gateways
- protobuf evolution requires discipline
- debugging binary protocols needs tooling
- proxy/load-balancer behavior matters
- streaming introduces flow-control/backpressure complexity
- public APIs may need REST/JSON transcoding alongside gRPC
- channel, resolver, and service config behavior can surprise teams
- graceful shutdown must handle long-lived HTTP/2 connections and streams

---

## 5. Beginner To Pro Learning Loop

```text
.proto -> generated stubs -> client channel -> server method -> status/deadline/metadata -> trace/metric/log -> production fix
```

For every gRPC topic, ask:

1. What service contract is exposed?
2. What generated code is used?
3. What RPC type executes?
4. What deadline, metadata, and auth apply?
5. What status code is returned?
6. What can fail in transport, server, dependency, or proxy?
7. What evidence proves the failure?
8. What production guardrail prevents recurrence?

---

## 6. Senior Interview Framing

Strong gRPC answers connect:

- proto contract design
- code generation
- HTTP/2 transport
- deadlines and cancellation
- canonical status codes
- retries and idempotency
- load balancing and discovery
- mTLS/authz
- streaming and flow control
- observability
- schema evolution
- deployment through proxies/mesh/gateways
- Protobuf Editions, field presence, well-known types, and JSON mapping
- channel state, service config, xDS, Channelz/admin debugging
- graceful shutdown, draining, and gRPC-Web/transcoding patterns

Weak answers only describe protobuf syntax.

---

## 7. Fast Recall

```text
gRPC is typed RPC over HTTP/2.
.proto is the contract.
Generated stubs reduce client/server drift.
Deadlines and status codes make failure explicit.
Production readiness depends on schema governance, channel policy, retries, load balancing, mTLS, observability, gateway behavior, and proxy/mesh rollout safety.
```

---

## 8. Start Here

1. Open [gRPC-Mastery-Track-Index.md](gRPC-Mastery-Track-Index.md).
2. Complete `01-Foundations` in order.
3. Practice `02-Intermediate-Practical` with the lab.
4. Study `03-Senior-Production` before system design interviews.
5. Use scenarios and runbooks until the debugging flow becomes automatic.
