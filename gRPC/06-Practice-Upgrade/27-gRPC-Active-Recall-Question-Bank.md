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

---

## Scenario Recall

1. A client sees `UNAVAILABLE`. What are your first seven checks?
2. A client sees `DEADLINE_EXCEEDED`. How do you prove where time was spent?
3. A stream stalls and memory grows. What evidence do you collect?
4. Old clients receive wrong data with `OK`. What likely happened?
5. After cert rotation, some clients fail. What do you check?
6. During rolling deploy, traffic sticks to one backend. What do you check?
7. A payment capture times out. Why should the caller not blindly retry without idempotency?

---

## MAANG-Style Prompts

1. Design a gRPC API for payment authorization at high scale.
2. Explain how you would run gRPC behind Envoy in Kubernetes.
3. Create a proto evolution policy for a company with 200 services.
4. Debug a p99 latency regression in one gRPC method.
5. Explain a safe retry and deadline policy for internal RPCs.
6. Compare gRPC, REST, and GraphQL for internal and external APIs.
7. Design observability for a fleet of gRPC services.

---

## Scoring

- Beginner: answer foundations clearly.
- Intermediate: map statuses, deadlines, metadata, and testing.
- Senior: discuss evolution, resilience, security, load balancing, observability.
- Staff-level: explain tradeoffs, failure modes, rollout safety, and governance.