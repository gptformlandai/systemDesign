# 05. Codegen, Stubs, Clients, Servers, Interceptors

## Goal

Understand how a proto contract becomes runnable application code.

```text
.proto -> compiler/plugin -> generated messages + client stub + server interface -> application implementation
```

---

## Generated Code Responsibilities

| Generated Piece | Responsibility |
|---|---|
| message classes/types | serialize, deserialize, validate basic structure |
| client stub | exposes typed RPC methods to callers |
| server interface/base | defines methods the server must implement |
| service descriptors | runtime method metadata used by frameworks/tools |

Generated code should be reproducible. Keep generation versioned and automated so local builds, CI, and release artifacts agree.

---

## Server Implementation Pattern

```text
generated service interface
-> application handler
-> validate request
-> call domain/service layer
-> map domain result to protobuf response
-> map failures to canonical status codes
```

Avoid putting heavy business logic in generated-code adapters. Keep generated stubs as boundary glue.

---

## Client Pattern

```text
create channel
create stub
attach metadata/auth
set deadline
call method
handle response/status
record metrics/traces
```

The deadline belongs close to the caller's business operation. A default library timeout is not enough for production correctness.

---

## Interceptors

Interceptors are middleware around RPC calls.

Common server interceptors:

- authentication
- authorization
- request logging
- metrics
- tracing
- panic/exception mapping
- rate limiting
- validation

Common client interceptors:

- auth metadata injection
- tracing propagation
- retry policy integration
- metrics
- deadline enforcement
- request logging with redaction

---

## Interceptor Caution

Do not hide business behavior in interceptors. They are best for cross-cutting policy. If a method has unique domain rules, implement them in the handler or domain layer.

---

## Failure Mapping

| Domain Failure | gRPC Status Candidate |
|---|---|
| invalid input | `INVALID_ARGUMENT` |
| missing resource | `NOT_FOUND` |
| permission denied | `PERMISSION_DENIED` |
| unauthenticated caller | `UNAUTHENTICATED` |
| optimistic lock conflict | `ABORTED` |
| dependency unavailable | `UNAVAILABLE` |
| operation exceeded deadline | `DEADLINE_EXCEEDED` |
| unexpected server bug | `INTERNAL` |

---

## Interview Sound Bite

Generated stubs reduce contract drift, but production quality comes from the surrounding adapter: request validation, deadline handling, metadata/auth, observability, error mapping, and keeping business logic out of generated-code plumbing.