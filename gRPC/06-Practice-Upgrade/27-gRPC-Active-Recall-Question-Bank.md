# 27. gRPC Active Recall Question Bank

Use these questions without looking at notes first.

---

## Foundations

1. What are the roles of `.proto`, message, service, method, stub, and channel?
2. Why are protobuf field numbers more important than field names on the wire?
3. Why should enum zero usually be `UNSPECIFIED`?
4. What are unary, server streaming, client streaming, and bidirectional streaming RPCs?
5. Why does gRPC commonly use HTTP/2?

---

## Practical

1. What does generated code provide and what should application code still own?
2. Where should deadlines be set?
3. What is metadata used for?
4. How do interceptors help?
5. How do you call a method with grpcurl?
6. What does server reflection enable?
7. What status code should invalid input return?
8. What status code should missing authentication return?
9. What status code should transient backend failure return?
10. How should cancellation affect server work?

---

## Production

1. How do you evolve protobuf safely?
2. When do you create a new proto package version?
3. Why can gRPC load balancing be tricky?
4. How do mTLS and JWT metadata differ?
5. How do you align app deadlines and proxy route timeouts?
6. What metrics are required for gRPC observability?
7. What makes retries safe or unsafe?
8. When is hedging appropriate?
9. What should a streaming API document?
10. Why can keepalive settings cause incidents?
11. What is the difference between proto3 syntax and Protobuf Editions?
12. When does field presence matter?
13. Why are well-known types better than ad hoc strings for timestamps?
14. What can break when a binary-safe proto change is exposed through JSON mapping?
15. When should you use `google.protobuf.Any`?
16. Why is `Struct` risky for core business data?
17. What is the purpose of `FieldMask` in reads and updates?
18. What is the difference between pagination and server streaming?
19. When should a long-running RPC become an operation resource instead?
20. Why should batch RPCs have per-item status or clear all-or-nothing semantics?

---

## Channel, Service Config, And xDS

1. What are the main channel states?
2. Why is creating a new channel per RPC usually a bad idea?
3. What does `dns:///` mean in a gRPC target URI?
4. What does `xds:///` imply about where client behavior comes from?
5. What is a subchannel?
6. What is the difference between `pick_first` and `round_robin`?
7. Why can HTTP/2 connection reuse cause load imbalance?
8. What belongs in service config?
9. What retry fields should appear in a service config?
10. How does a hedging policy differ from a retry policy?
11. Why must hedging have a total deadline?
12. What evidence proves an xDS update was NACKed?
13. Why should xDS/service config changes be canaried?
14. How do you debug clients stuck in `TRANSIENT_FAILURE`?
15. What metrics show retry amplification?

---

## Graceful Shutdown And Draining

1. What should a gRPC server do after receiving SIGTERM?
2. Why should health flip to `NOT_SERVING` before process exit?
3. How does Kubernetes readiness interact with gRPC health?
4. What is the role of `terminationGracePeriodSeconds`?
5. Why is a `preStop sleep` not a full graceful shutdown strategy?
6. What is HTTP/2 GOAWAY used for?
7. Why can long-lived streams break during rolling deploys?
8. What is a stream resume token?
9. Why should streams have heartbeat and max age?
10. Which metrics prove a rollout drained correctly?

---

## Observability And Debugging

1. What is the difference between client call duration and client attempt duration?
2. Why are per-attempt metrics important when retries or hedging exist?
3. What labels should gRPC metrics include?
4. Which labels should never be used in high-cardinality metrics?
5. What is Channelz used for?
6. Why should Channelz/admin endpoints be protected?
7. If clients see `UNAVAILABLE` but server metrics are flat, where do you look?
8. How do you prove whether latency came from proxy, server, dependency, or retry delay?
9. What trace spans should exist for a production gRPC call?
10. What should a gRPC RCA include?

---

## Gateways And External APIs

1. Why can't browser JavaScript usually call native gRPC directly?
2. When is gRPC-Web a good fit?
3. What does Envoy's grpc-web filter do?
4. Which CORS headers matter for gRPC-Web?
5. Why must `grpc-status` and `grpc-message` be exposed to browser clients?
6. When is REST/JSON transcoding better than gRPC-Web?
7. What proto annotation shape maps an RPC to an HTTP path?
8. How should gRPC status codes map to HTTP status codes?
9. What JSON mapping issues appear with int64, enums, and default values?
10. When would you choose a BFF or GraphQL instead of gRPC-Web?

---

## Runnable Implementation

1. What files should exist in a runnable Java or Go gRPC lab?
2. What should the shared `payment.proto` demonstrate?
3. Why does `CapturePayment` require an idempotency key?
4. What should the client deadline prove?
5. What should a grpcurl smoke test prove?
6. Which tests prove cancellation is handled correctly?
7. Why should reflection be enabled locally but reviewed for production?
8. What should Docker/compose prove for a gRPC service?
9. What load test catches slow-consumer streaming problems?
10. How do you present a runnable gRPC project in an interview?

---

## Scenario Recall

1. A client sees `UNAVAILABLE`. What are your first seven checks?
2. A client sees `DEADLINE_EXCEEDED`. How do you prove where time was spent?
3. A stream stalls and memory grows. What evidence do you collect?
4. Old clients receive wrong data with `OK`. What likely happened?
5. After cert rotation, some clients fail. What do you check?
6. During rolling deploy, traffic sticks to one backend. What do you check?
7. A payment capture times out. Why should the caller not blindly retry without idempotency?
8. During rolling deploy, streams reset. What evidence do you collect?
9. A browser client cannot read `grpc-status`. What gateway setting do you check?
10. A JSON client breaks after a proto field rename. Why can this happen?
11. Retry attempts doubled traffic during an outage. What policy failed?
12. xDS pushed a new route and only one region failed. What do you inspect?
13. A channel stays `READY`, but traffic goes to one backend. What do you investigate?
14. A side-effecting method was hedged. What is the incident risk?
15. A server logs `OK`, but client sees deadline exceeded. How can both be true?

---

## MAANG-Style Prompts

1. Design a gRPC API for payment authorization at high scale.
2. Explain how you would run gRPC behind Envoy in Kubernetes.
3. Create a proto evolution policy for a company with 200 services.
4. Debug a p99 latency regression in one gRPC method.
5. Explain a safe retry and deadline policy for internal RPCs.
6. Compare gRPC, REST, and GraphQL for internal and external APIs.
7. Design observability for a fleet of gRPC services.
8. Design a browser-facing API backed by internal gRPC services.
9. Design a safe rollout strategy for service config and xDS changes.
10. Design graceful shutdown for bidirectional streaming RPCs on Kubernetes.
11. Build a runnable Java/Go gRPC lab and explain the production add-ons.
12. Create a Protobuf Editions adoption policy for a large company.

---

## Scoring

- Beginner: answer foundations clearly.
- Intermediate: map statuses, deadlines, metadata, and testing.
- Senior: discuss evolution, resilience, security, load balancing, observability.
- Staff-level: explain tradeoffs, failure modes, rollout safety, and governance.
- Principal-level: connect contract design, generated SDKs, channel policy, mesh/gateway behavior, observability, and operational ownership.
