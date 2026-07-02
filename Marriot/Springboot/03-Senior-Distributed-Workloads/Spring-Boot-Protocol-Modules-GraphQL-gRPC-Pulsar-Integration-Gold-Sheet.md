# Spring Boot Protocol Modules: GraphQL, gRPC, Pulsar, Integration, And Realtime Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: know when REST is not the only interface and how modern Spring Boot supports other protocols.

---

## 1. Intuition

REST is a strong default, but not every conversation between systems is a REST conversation.
GraphQL lets clients ask for shaped data. gRPC gives fast contract-first RPC. Pulsar and
Kafka move durable events. Spring Integration connects enterprise systems. SSE/WebSocket
push data to clients. Good engineers choose the protocol that matches the traffic pattern.

---

## 2. Definition

- Definition: Protocol modules are Spring Boot integrations for serving or consuming APIs,
  streams, messages, and enterprise flows beyond classic REST controllers.
- Category: integration architecture.
- Core idea: choose protocols by coupling, latency, schema, client type, durability, and
  operational maturity.

---

## 3. Why It Exists

REST alone can be awkward when:

- frontend needs graph-shaped data from many resources
- internal services need low-latency binary RPC
- business events must survive service restarts
- enterprise systems speak files, JMS, FTP, mail, or polling adapters
- UI needs realtime updates
- consumers need schema evolution guarantees

---

## 4. Reality

| Need | Candidate |
|---|---|
| Public CRUD API | REST |
| Flexible frontend query shape | Spring GraphQL |
| Low-latency internal RPC | gRPC |
| Durable event stream | Kafka or Pulsar |
| Work queue / routing | RabbitMQ or JMS |
| Enterprise adapters | Spring Integration |
| Browser push | SSE or WebSocket |
| Reactive bidirectional messaging | RSocket |

---

## 5. How It Works

Protocol selection flow:

1. Identify caller type: browser, mobile, backend service, batch, external partner.
2. Identify interaction: request/response, streaming, event, command, query, file flow.
3. Identify delivery requirement: best effort, at least once, exactly once effect, durable.
4. Identify contract style: OpenAPI, GraphQL schema, Protobuf, Avro, JSON Schema.
5. Identify operational maturity: gateway support, observability, auth, retries, tooling.
6. Implement with the matching Spring Boot starter/module.
7. Add contract tests and backward compatibility rules.
8. Add metrics, tracing, rate limits, and failure handling.

Failure path:

- choose GraphQL for everything -> expensive resolvers and hidden N+1
- choose gRPC for public browser API -> client/proxy complexity
- choose messaging for synchronous user decision -> poor UX
- choose REST for high-volume internal streaming -> inefficient protocol fit

Recovery path:

- split read model or add DataLoader for GraphQL
- expose REST/BFF to browsers and gRPC internally
- use outbox/eventing for async side effects
- keep synchronous critical path small

---

## 6. What Problem It Solves

- Primary problem solved: mismatch between communication pattern and protocol.
- Secondary benefits: better performance, cleaner contracts, less frontend overfetching,
  durable async workflows.
- Systems impact: correct protocol boundaries reduce coupling and production surprises.

---

## 7. When To Rely On It

Use these modules when:

- GraphQL: clients need flexible query shape and schema-driven frontend collaboration.
- gRPC: internal service calls need strict protobuf contracts and low overhead.
- Pulsar/Kafka: events must be durable, replayable, and partitioned.
- Spring Integration: enterprise workflows involve adapters, channels, filters, routers.
- SSE/WebSocket: clients need realtime updates.

---

## 8. When Not To Use It

Avoid protocol novelty when REST is enough.

- Do not use GraphQL to hide poor service boundaries.
- Do not use gRPC if clients, gateways, or observability cannot support it.
- Do not use messaging to dodge transaction design.
- Do not use WebSocket when polling or SSE is enough.
- Do not introduce Pulsar/Kafka without operations ownership.

---

## 9. Pros And Cons

| Protocol | Pros | Cons |
|---|---|---|
| REST | simple, cacheable, universal | over/under-fetching, many round trips |
| GraphQL | flexible client query, typed schema | resolver N+1, caching and auth complexity |
| gRPC | fast, typed, streaming | browser/proxy/tooling complexity |
| Kafka/Pulsar | durable event streams | eventual consistency, ops complexity |
| Spring Integration | rich adapters and EIP patterns | flow complexity if overused |
| WebSocket | bidirectional realtime | connection scaling and state |
| SSE | simple server push | one-way only |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- REST optimizes simplicity.
- GraphQL optimizes client flexibility.
- gRPC optimizes internal contract and latency.
- Messaging optimizes durability and decoupling.
- Realtime protocols optimize freshness but add connection management.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| No schema compatibility policy | Breaks consumers | Define backward compatibility |
| GraphQL resolver calls DB per field | N+1 at API layer | DataLoader/batching/projections |
| Retrying non-idempotent RPC blindly | Duplicate side effects | Idempotency keys and retry policy |
| Treating event publish as transaction | DB commit can succeed while publish fails | Outbox pattern |
| No trace propagation | Debugging becomes hard | W3C trace headers/metadata |
| Public gRPC without support | Client adoption pain | REST/BFF externally, gRPC internally |

---

## 11. Key Numbers

Approximate reasoning points:

- REST JSON is human-readable but larger than Protobuf.
- gRPC works well for low-latency internal calls but needs HTTP/2 support.
- GraphQL query depth and field count should be limited.
- Event streams are usually at-least-once at the business effect level.
- SSE is one-way server-to-client over HTTP.
- WebSocket keeps long-lived connections, so instance memory and load balancing matter.

---

## 12. Failure Modes

| Failure | User Observes | Root Cause | Mitigation |
|---|---|---|---|
| GraphQL p99 spike | Slow frontend | Resolver N+1 | DataLoader, projection, limits |
| gRPC unavailable | Internal calls fail | HTTP/2/proxy issue | Gateway/proxy support and fallback |
| Duplicate event effect | Double email/payment | At-least-once delivery | Idempotent consumer |
| Lost event | Missing side effect | Publish outside DB transaction | Outbox |
| WebSocket scale issue | Disconnects | Sticky/session/connection load | Dedicated realtime scaling |
| Contract break | Client errors | Schema incompatible change | Contract tests and versioning |

---

## 13. Scenario

- Product/system: hotel booking platform.
- Why this concept fits: REST handles booking commands, GraphQL can power search UI,
  gRPC can serve internal pricing calls, Kafka/Pulsar can publish booking events, SSE can
  push booking status updates.
- What would go wrong without it: one protocol would be stretched across mismatched use cases.

---

## 14. Code Sample

GraphQL controller sketch:

```java
package com.example.booking.api;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
class HotelGraphQlController {

    private final HotelSearchService searchService;

    HotelGraphQlController(HotelSearchService searchService) {
        this.searchService = searchService;
    }

    @QueryMapping
    List<HotelResult> hotels(@Argument String city, @Argument int guests) {
        return searchService.search(city, guests);
    }
}
```

gRPC and Pulsar configuration is usually contract-first and generated/configured through
their respective Spring Boot modules. The interview point is not memorizing annotations;
it is choosing the right communication model and failure handling.

---

## 15. Mini Program / Simulation

```python
def choose_protocol(caller, interaction, durability):
    if caller == "browser" and interaction == "flexible-query":
        return "GraphQL through BFF"
    if caller == "service" and interaction == "low-latency-rpc":
        return "gRPC"
    if durability == "durable-event":
        return "Kafka or Pulsar"
    if interaction == "server-push":
        return "SSE or WebSocket"
    return "REST"


cases = [
    ("browser", "flexible-query", "none"),
    ("service", "low-latency-rpc", "none"),
    ("service", "event", "durable-event"),
]

for case in cases:
    print(case, "=>", choose_protocol(*case))
```

---

## 16. Practical Question

> A hotel platform has a browser UI, pricing service, booking events, and live booking
> status updates. Which protocols would you use and why?

---

## 17. Strong Answer

I would keep booking commands as REST because they are resource-oriented and need clear
HTTP semantics, validation, idempotency, and ProblemDetail errors. For flexible hotel
search UI I may use GraphQL through a BFF, but I would control query depth and resolver
batching. For internal low-latency pricing calls I would consider gRPC if the platform
supports HTTP/2, tracing, retries, and protobuf contracts. For booking-created and payment
events I would use Kafka or Pulsar with outbox and idempotent consumers. For live booking
status, SSE is enough if the server only pushes updates; WebSocket only if the client also
needs bidirectional realtime interaction.

---

## 18. Revision Notes

- One-line summary: REST is a default, not a law; choose protocol by interaction pattern.
- Three keywords: schema, durability, caller.
- One interview trap: GraphQL solves client shape, not database or authorization design.
- One memory trick: REST for resources, gRPC for RPC, events for durability, SSE for push.

