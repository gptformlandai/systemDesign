# gRPC Mastery Track - Beginner To Pro Index

This folder is a complete gRPC mastery track for backend engineers, platform engineers, cloud engineers, SREs, and system design interviews.

It teaches gRPC as a production RPC architecture, not just generated client/server code.

```text
.proto contract -> generated stubs -> HTTP/2 transport -> RPC lifecycle -> deadlines/status/metadata -> production answer
```

Use this track if:

- You want beginner-to-pro gRPC confidence for service-to-service APIs, backend platforms, and interviews.
- You want to understand Protocol Buffers, service definitions, code generation, stubs, streaming, deadlines, errors, metadata, and HTTP/2 deeply.
- You want MAANG-level answers connecting gRPC to schema evolution, load balancing, retries, mTLS, observability, service mesh, and production incidents.
- You want hands-on labs, runbooks, portfolio projects, and scenario drills instead of reading-only notes.

---

## 1. Learning Style: Beginner To Pro Loop

Every topic should be learned with this loop:

```text
concept -> .proto contract -> generated code -> RPC execution -> transport behavior -> failure mode -> fix -> production scenario -> interview explanation
```

gRPC mastery is not memorizing `protoc`. It is understanding how a typed service contract becomes generated clients/servers, HTTP/2 requests, status codes, deadlines, metadata, and observable production behavior.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Foundations` | gRPC mental model, tooling, Protocol Buffers, RPC types |
| 2 | `02-Intermediate-Practical` | codegen, stubs, errors, deadlines, streaming, discovery, auth, testing |
| 3 | `03-Senior-Production` | schema governance, performance, resilience, security, observability, deployment, gateway/platform depth |
| 4 | `04-Scenario-Practice` | API design, deadline incidents, streaming issues, auth, schema evolution, production debugging |
| 5 | `05-Special-Interview-Rounds` | Q&A, command maps, anti-patterns, debugging traps |
| 6 | `06-Practice-Upgrade` | active recall, drills, mini projects, production readiness checklist |
| Lab | `grpc-mastery-lab` | proto examples, scripts, labs, projects, cheatsheets, interview prep, runbooks |

---

## 3. Foundations Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Foundations/01-gRPC-Mental-Model-Proto-Service-Stubs-HTTP2-Hot-Sheet.md](01-Foundations/01-gRPC-Mental-Model-Proto-Service-Stubs-HTTP2-Hot-Sheet.md) | gRPC mental model, proto contract, stubs, HTTP/2 |
| 2 | [01-Foundations/02-gRPC-Setup-Tooling-Protoc-Buf-gRPCurl-Server-Client-Gold-Sheet.md](01-Foundations/02-gRPC-Setup-Tooling-Protoc-Buf-gRPCurl-Server-Client-Gold-Sheet.md) | protoc, Buf, grpcurl, reflection, server/client workflow |
| 3 | [01-Foundations/03-gRPC-Protobuf-Schemas-Messages-Fields-Enums-Oneof-Gold-Sheet.md](01-Foundations/03-gRPC-Protobuf-Schemas-Messages-Fields-Enums-Oneof-Gold-Sheet.md) | messages, fields, tags, enums, oneof, reserved fields |
| 4 | [01-Foundations/04-gRPC-RPC-Types-Unary-Server-Client-Bidi-Streaming-Gold-Sheet.md](01-Foundations/04-gRPC-RPC-Types-Unary-Server-Client-Bidi-Streaming-Gold-Sheet.md) | unary, server streaming, client streaming, bidirectional streaming |

Foundation target:

- You can explain `.proto` vs generated code vs runtime transport.
- You can read and design basic service and message contracts.
- You can explain why gRPC is strong for typed service-to-service communication.

---

## 4. Intermediate Practical Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Practical/05-gRPC-Codegen-Stubs-Clients-Servers-Interceptors-Gold-Sheet.md](02-Intermediate-Practical/05-gRPC-Codegen-Stubs-Clients-Servers-Interceptors-Gold-Sheet.md) | generated stubs, servers, clients, interceptors |
| 6 | [02-Intermediate-Practical/06-gRPC-Status-Codes-Errors-Metadata-Deadlines-Cancellation-Gold-Sheet.md](02-Intermediate-Practical/06-gRPC-Status-Codes-Errors-Metadata-Deadlines-Cancellation-Gold-Sheet.md) | status codes, metadata, deadlines, cancellation, rich errors |
| 7 | [02-Intermediate-Practical/07-gRPC-Streaming-Flow-Control-Backpressure-Gold-Sheet.md](02-Intermediate-Practical/07-gRPC-Streaming-Flow-Control-Backpressure-Gold-Sheet.md) | streaming patterns, flow control, backpressure, cancellation |
| 8 | [02-Intermediate-Practical/08-gRPC-Service-Discovery-Load-Balancing-Name-Resolution-Gold-Sheet.md](02-Intermediate-Practical/08-gRPC-Service-Discovery-Load-Balancing-Name-Resolution-Gold-Sheet.md) | name resolution, client-side/server-side load balancing, health checks |
| 9 | [02-Intermediate-Practical/09-gRPC-Auth-mTLS-JWT-Per-Method-Authorization-Gold-Sheet.md](02-Intermediate-Practical/09-gRPC-Auth-mTLS-JWT-Per-Method-Authorization-Gold-Sheet.md) | TLS, mTLS, JWT metadata, per-method authz |
| 10 | [02-Intermediate-Practical/10-gRPC-Testing-Reflection-gRPCurl-Contract-Tests-Gold-Sheet.md](02-Intermediate-Practical/10-gRPC-Testing-Reflection-gRPCurl-Contract-Tests-Gold-Sheet.md) | grpcurl, reflection, contract tests, golden protos |

Practical target:

- You can generate stubs, implement clients/servers, debug status/deadline failures, test services, and reason about streaming and auth.

---

## 5. Senior Production Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-Production/11-gRPC-Proto-Evolution-Compatibility-Governance-MAANG-Sheet.md](03-Senior-Production/11-gRPC-Proto-Evolution-Compatibility-Governance-MAANG-Sheet.md) | schema evolution, field tags, reserved numbers, compatibility, Buf governance |
| 12 | [03-Senior-Production/12-gRPC-Performance-HTTP2-Flow-Control-Keepalive-Compression-MAANG-Sheet.md](03-Senior-Production/12-gRPC-Performance-HTTP2-Flow-Control-Keepalive-Compression-MAANG-Sheet.md) | HTTP/2 multiplexing, flow control, keepalive, compression, message size |
| 13 | [03-Senior-Production/13-gRPC-Resilience-Retries-Hedging-Deadlines-Idempotency-MAANG-Sheet.md](03-Senior-Production/13-gRPC-Resilience-Retries-Hedging-Deadlines-Idempotency-MAANG-Sheet.md) | deadlines, retries, hedging, idempotency, service config |
| 14 | [03-Senior-Production/14-gRPC-Observability-Metrics-Tracing-Logs-SLOs-Gold-Sheet.md](03-Senior-Production/14-gRPC-Observability-Metrics-Tracing-Logs-SLOs-Gold-Sheet.md) | RPC metrics, tracing, logs, status code SLOs |
| 15 | [03-Senior-Production/15-gRPC-Security-mTLS-Cert-Rotation-SPIFFE-Authz-MAANG-Sheet.md](03-Senior-Production/15-gRPC-Security-mTLS-Cert-Rotation-SPIFFE-Authz-MAANG-Sheet.md) | mTLS, cert rotation, SPIFFE/SPIRE, metadata safety, authz |
| 16 | [03-Senior-Production/16-gRPC-Deployment-Envoy-Service-Mesh-Kubernetes-Gateways-MAANG-Sheet.md](03-Senior-Production/16-gRPC-Deployment-Envoy-Service-Mesh-Kubernetes-Gateways-MAANG-Sheet.md) | Kubernetes, Envoy, service mesh, gateways, gRPC-Web, HTTP transcoding |
| 31 | [03-Senior-Production/31-gRPC-Pro-Gap-Fill-Health-WaitForReady-ServiceConfig-RichErrors-FieldMasks-MAANG-Sheet.md](03-Senior-Production/31-gRPC-Pro-Gap-Fill-Health-WaitForReady-ServiceConfig-RichErrors-FieldMasks-MAANG-Sheet.md) | health protocol, wait-for-ready, service config/xDS, rich errors, field masks, limits |
| 32 | [03-Senior-Production/32-gRPC-Protobuf-Editions-WellKnownTypes-JSON-Mapping-MAANG-Sheet.md](03-Senior-Production/32-gRPC-Protobuf-Editions-WellKnownTypes-JSON-Mapping-MAANG-Sheet.md) | Protobuf Editions, field presence, well-known types, JSON mapping, API shape |
| 33 | [03-Senior-Production/33-gRPC-Channel-Internals-ServiceConfig-xDS-Deep-Dive-MAANG-Sheet.md](03-Senior-Production/33-gRPC-Channel-Internals-ServiceConfig-xDS-Deep-Dive-MAANG-Sheet.md) | channel states, resolver, service config, LB policy, xDS, subchannel debugging |
| 34 | [03-Senior-Production/34-gRPC-Graceful-Shutdown-Draining-Kubernetes-Envoy-MAANG-Sheet.md](03-Senior-Production/34-gRPC-Graceful-Shutdown-Draining-Kubernetes-Envoy-MAANG-Sheet.md) | graceful shutdown, health drain, Kubernetes readiness, Envoy drain, long streams |
| 35 | [03-Senior-Production/35-gRPC-OpenTelemetry-Channelz-Production-Debugging-MAANG-Sheet.md](03-Senior-Production/35-gRPC-OpenTelemetry-Channelz-Production-Debugging-MAANG-Sheet.md) | OTel metrics, retry/attempt metrics, Channelz/admin debugging, RCA workflow |
| 36 | [03-Senior-Production/36-gRPC-Web-Gateway-Transcoding-External-API-Patterns-MAANG-Sheet.md](03-Senior-Production/36-gRPC-Web-Gateway-Transcoding-External-API-Patterns-MAANG-Sheet.md) | gRPC-Web, CORS, JSON transcoding, external API decision map |

Senior target:

- You can explain gRPC in production: schema governance, performance, resilience, health semantics, security, observability, channel behavior, graceful draining, deployment, and mesh/gateway tradeoffs.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/17-gRPC-Design-Order-Payment-Service-Scenario-Gold-Sheet.md](04-Scenario-Practice/17-gRPC-Design-Order-Payment-Service-Scenario-Gold-Sheet.md) | design typed service-to-service APIs |
| 18 | [04-Scenario-Practice/18-gRPC-Deadline-Exceeded-Latency-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/18-gRPC-Deadline-Exceeded-Latency-Debugging-Scenario-MAANG-Sheet.md) | deadline exceeded and latency debugging |
| 19 | [04-Scenario-Practice/19-gRPC-Streaming-Backpressure-Cancellation-Scenario-MAANG-Sheet.md](04-Scenario-Practice/19-gRPC-Streaming-Backpressure-Cancellation-Scenario-MAANG-Sheet.md) | streaming stalls, backpressure, cancellation |
| 20 | [04-Scenario-Practice/20-gRPC-Auth-mTLS-Cert-Failure-Scenario-Gold-Sheet.md](04-Scenario-Practice/20-gRPC-Auth-mTLS-Cert-Failure-Scenario-Gold-Sheet.md) | TLS/mTLS and auth failures |
| 21 | [04-Scenario-Practice/21-gRPC-Proto-Breaking-Change-Incident-Scenario-MAANG-Sheet.md](04-Scenario-Practice/21-gRPC-Proto-Breaking-Change-Incident-Scenario-MAANG-Sheet.md) | schema compatibility incident |
| 22 | [04-Scenario-Practice/22-gRPC-Load-Balancing-Service-Discovery-Incident-Scenario-MAANG-Sheet.md](04-Scenario-Practice/22-gRPC-Load-Balancing-Service-Discovery-Incident-Scenario-MAANG-Sheet.md) | discovery/LB/health incident |
| 23 | [04-Scenario-Practice/23-gRPC-Production-Incident-Debugging-Scenario-MAANG-Sheet.md](04-Scenario-Practice/23-gRPC-Production-Incident-Debugging-Scenario-MAANG-Sheet.md) | on-call gRPC incident response |

Scenario target:

- You can diagnose realistic gRPC issues with a repeatable evidence path.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/24-gRPC-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md](05-Special-Interview-Rounds/24-gRPC-Interview-QA-Beginner-To-Pro-MAANG-Sheet.md) | gRPC Q&A from beginner to MAANG |
| 25 | [05-Special-Interview-Rounds/25-gRPC-Commands-Protobuf-Cheat-Sheet-And-Decision-Map.md](05-Special-Interview-Rounds/25-gRPC-Commands-Protobuf-Cheat-Sheet-And-Decision-Map.md) | command/proto/RPC decision map |
| 26 | [05-Special-Interview-Rounds/26-gRPC-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md](05-Special-Interview-Rounds/26-gRPC-Anti-Patterns-Debugging-Traps-MAANG-Sheet.md) | unsafe gRPC/protobuf practices |

Special-round target:

- You can answer gRPC interviews and avoid common production mistakes.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 27 | [06-Practice-Upgrade/27-gRPC-Active-Recall-Question-Bank.md](06-Practice-Upgrade/27-gRPC-Active-Recall-Question-Bank.md) | recall prompts across beginner to pro topics |
| 28 | [06-Practice-Upgrade/28-gRPC-Hands-On-Exercises-And-RPC-Drills.md](06-Practice-Upgrade/28-gRPC-Hands-On-Exercises-And-RPC-Drills.md) | proto/RPC/debugging drills |
| 29 | [06-Practice-Upgrade/29-gRPC-Mini-Projects-Portfolio.md](06-Practice-Upgrade/29-gRPC-Mini-Projects-Portfolio.md) | portfolio-ready gRPC projects |
| 30 | [06-Practice-Upgrade/30-gRPC-Pro-Gap-Fill-Production-Readiness-Checklist.md](06-Practice-Upgrade/30-gRPC-Pro-Gap-Fill-Production-Readiness-Checklist.md) | senior readiness checklist and scoring rubric |
| 37 | [06-Practice-Upgrade/37-gRPC-Runnable-Java-Go-Lab-Guide.md](06-Practice-Upgrade/37-gRPC-Runnable-Java-Go-Lab-Guide.md) | runnable Java/Go service blueprint, grpcurl smoke tests, Docker, load/failure tests |

Practice target:

- You can design, implement, debug, secure, observe, and evolve production gRPC APIs.

---

## 9. gRPC Mastery Lab

Use the lab when you want practice instead of reading-only notes:

- [grpc-mastery-lab/README.md](grpc-mastery-lab/README.md)
- [grpc-mastery-lab/LEARNING_PATH.md](grpc-mastery-lab/LEARNING_PATH.md)

Lab target:

- You can inspect `.proto` contracts and generated-code boundaries.
- You can practice RPC debugging, deadlines, status codes, metadata, streaming, and schema evolution.
- You can use safe scripts and runbooks to reason through production-style gRPC failures.

---

## 10. Interview Answer Pattern

For gRPC debugging and interview answers, use this shape:

```text
1. Symptom:
   What exactly is failing: schema, codegen, connection, deadline, status, auth, streaming, load balancing, or server logic?

2. Contract:
   Which .proto package/service/method/message is involved?

3. Call Path:
   Which client, channel, resolver/LB policy, server method, and dependency execute?

4. Evidence:
   Which status code, deadline, trace span, metric, log, metadata, or grpcurl result proves the state?

5. Cause:
   What changed or mismatched?

6. Mitigation:
   What safe action restores service?

7. Prevention:
   What proto rule, deadline policy, retry config, auth policy, health check, test, or runbook prevents recurrence?
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Foundation files 1-4.
2. Practical files 5-10.
3. Scenario files 17-23.
4. Cheat sheet, exercises, and interview Q&A.

### 5-Week Pro Path

1. Week 1: gRPC mental model, Protocol Buffers, tooling, RPC types.
2. Week 2: codegen, status/errors, deadlines, streaming, discovery, auth, testing.
3. Week 3: proto governance, performance, resilience, security, observability, deployment.
4. Week 4: protobuf editions, channel/xDS, graceful shutdown, OTel/Channelz, gateways.
5. Week 5: runnable Java/Go lab, production scenarios, runbooks, mini projects, interview practice.

### Production Operator Path

1. Learn status/deadline/metadata/debugging workflow.
2. Practice deadline, streaming, mTLS, proto evolution, and discovery incidents.
3. Add observability, retries, load balancing, and schema governance controls.
4. Write RCA notes from each scenario.

---

## 12. Readiness Gate

You are gRPC interview-ready when you can do all of this without notes:

- Explain `.proto`, message, service, method, stub, channel, interceptor, metadata, deadline, status, and HTTP/2.
- Design protobuf messages with stable field tags, enums, oneof, reserved numbers/names, and compatibility rules.
- Explain unary, server streaming, client streaming, and bidirectional streaming tradeoffs.
- Generate clients/servers and debug with reflection, grpcurl, and contract tests.
- Explain deadlines, cancellation, retries, hedging, idempotency, and status-code semantics.
- Explain load balancing, name resolution, health checks, service discovery, and service mesh behavior.
- Explain security: TLS, mTLS, JWT metadata, SPIFFE/SPIRE, cert rotation, and per-method authorization.
- Explain observability with RPC metrics, traces, logs, status-code SLOs, and dependency evidence.
- Explain production deployment with Kubernetes, Envoy, gateways, gRPC-Web, HTTP transcoding, and proxy timeouts.
- Explain `wait_for_ready`, service config/xDS policy, structured rich errors, field masks, and operational limit policies.
- Explain Protobuf Editions, field presence, well-known types, JSON mapping, and external API compatibility.
- Explain channel states, subchannels, resolver behavior, xDS evidence, and retry/hedge attempt metrics.
- Explain graceful shutdown, health drain, Kubernetes readiness, Envoy drain, GOAWAY, and long-stream resume.
- Build or walk through a runnable Java/Go gRPC service with Buf, grpcurl, tests, Docker, health, reflection, and deadlines.
- Handle production gRPC incidents with evidence, mitigation, and prevention.

---

## 13. Current Source Anchors

- gRPC core concepts: <https://grpc.io/docs/what-is-grpc/core-concepts/>
- gRPC guides: <https://grpc.io/docs/guides/>
- gRPC service config: <https://grpc.io/docs/guides/service-config/>
- gRPC OpenTelemetry metrics: <https://grpc.io/docs/guides/opentelemetry-metrics/>
- gRPC request hedging: <https://grpc.io/docs/guides/request-hedging/>
- Protobuf Editions: <https://protobuf.dev/programming-guides/editions/>
- Protobuf best practices: <https://protobuf.dev/best-practices/dos-donts/>
- Buf docs: <https://buf.build/docs/>
