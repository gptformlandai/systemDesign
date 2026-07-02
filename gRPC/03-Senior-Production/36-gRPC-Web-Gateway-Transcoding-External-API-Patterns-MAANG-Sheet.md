# 36. gRPC-Web, Gateways, JSON Transcoding, And External API Patterns

## Goal

Know when native gRPC is the right external API, when browsers need gRPC-Web, and when REST/JSON transcoding or another API style is cleaner.

```text
internal gRPC + browser/gateway constraints + auth/CORS + JSON mapping + streaming limits = safe external API design
```

---

## 1. Why Browsers Need A Gateway

Native browser JavaScript does not expose full HTTP/2 framing control needed for normal gRPC clients.

Common external patterns:

| Pattern | Use |
|---|---|
| Native gRPC | service-to-service, mobile/native clients, controlled infrastructure |
| gRPC-Web | browser clients through Envoy or compatible gateway |
| REST/JSON transcoding | public HTTP APIs, broad tooling, simple browser use |
| GraphQL | flexible client-selected graphs, frontend aggregation |
| Connect-style protocols | mixed gRPC/HTTP/JSON compatibility, depending on ecosystem |

Interview stance:

```text
Use native gRPC for internal service contracts. For browser-first public APIs,
choose gRPC-Web or REST/JSON gateway intentionally, not accidentally.
```

---

## 2. gRPC-Web Trade-Offs

gRPC-Web gives browser clients a gRPC-like API through a proxy.

Strengths:

- generated clients
- typed protobuf messages
- consistent backend gRPC service
- works with browser HTTP APIs through gateway translation

Constraints:

- proxy/gateway is required
- CORS must be configured
- metadata maps to HTTP headers
- auth headers must be explicitly allowed
- streaming support is more limited than native gRPC
- browser devtools show gateway HTTP traffic, not raw native gRPC frames

---

## 3. Envoy gRPC-Web Shape

Conceptual Envoy route:

```yaml
http_filters:
  - name: envoy.filters.http.grpc_web
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.filters.http.grpc_web.v3.GrpcWeb
  - name: envoy.filters.http.cors
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.filters.http.cors.v3.Cors
  - name: envoy.filters.http.router
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router

route_config:
  virtual_hosts:
    - name: api
      domains: ["api.example.com"]
      cors:
        allow_origin_string_match:
          - exact: "https://app.example.com"
        allow_methods: "POST, OPTIONS"
        allow_headers: "content-type,x-grpc-web,authorization,x-user-agent,grpc-timeout"
        expose_headers: "grpc-status,grpc-message,grpc-status-details-bin"
      routes:
        - match:
            prefix: "/payments.v1.PaymentService/"
          route:
            cluster: payment-grpc
            timeout: 2s
```

Production rules:

- Keep CORS allowlist tight.
- Expose gRPC status headers intentionally.
- Do not log `authorization` or sensitive metadata.
- Align gateway route timeout with client deadlines.
- Test browser auth, preflight, and error detail behavior.

---

## 4. JSON Transcoding

JSON transcoding exposes REST-like HTTP endpoints backed by gRPC methods.

Example proto annotation shape:

```proto
import "google/api/annotations.proto";

service PaymentService {
  rpc GetPayment(GetPaymentRequest) returns (Payment) {
    option (google.api.http) = {
      get: "/v1/payments/{payment_id}"
    };
  }

  rpc CapturePayment(CapturePaymentRequest) returns (CapturePaymentResponse) {
    option (google.api.http) = {
      post: "/v1/payments/{payment_id}:capture"
      body: "*"
    };
  }
}
```

Use when:

- public clients expect REST/JSON
- browser tooling matters
- caching/resource URLs matter
- external API lifecycle differs from internal service lifecycle

Watch for:

- JSON field naming
- int64/string mapping
- enum string mapping
- HTTP status vs gRPC status mapping
- request/response body shape
- error detail exposure

---

## 5. Gateway Error Mapping

Native gRPC clients receive canonical gRPC status codes.

External HTTP clients often expect HTTP status codes.

| gRPC | HTTP-ish mapping |
|---|---|
| `INVALID_ARGUMENT` | 400 |
| `UNAUTHENTICATED` | 401 |
| `PERMISSION_DENIED` | 403 |
| `NOT_FOUND` | 404 |
| `ALREADY_EXISTS` | 409 |
| `FAILED_PRECONDITION` | 400 or 409 |
| `RESOURCE_EXHAUSTED` | 429 |
| `UNAVAILABLE` | 503 |
| `DEADLINE_EXCEEDED` | 504 |
| `INTERNAL` | 500 |

Document this mapping. Do not let each gateway invent its own.

---

## 6. External API Decision Map

| Requirement | Better Choice |
|---|---|
| internal polyglot services | native gRPC |
| browser frontend with generated protobuf client | gRPC-Web |
| public API with broad client tooling | REST/JSON transcoding |
| frontend needs flexible graph shape | GraphQL or BFF |
| mobile client with strong typing and streaming | native gRPC if platform support is mature |
| server push/live updates in browser | WebSocket/SSE or carefully tested gRPC-Web stream |

---

## 7. Security Checklist

```text
Auth:
  [ ] browser auth token allowed through CORS preflight
  [ ] metadata/header names documented
  [ ] credentials not exposed to logs

CORS:
  [ ] explicit allowed origins
  [ ] explicit allowed headers
  [ ] explicit exposed gRPC status headers

Gateway:
  [ ] route timeout aligns with client deadline
  [ ] request size and metadata size capped
  [ ] error details redacted for external clients
  [ ] rate limits and WAF rules applied at edge
```

---

## 8. Interview Scenario

> A frontend team wants to call an internal payment gRPC service directly from React. What do you recommend?

Good answer:

```text
I would not expose the internal native gRPC service directly to the browser.
Browsers need gRPC-Web or a JSON/REST gateway because they do not expose full
native gRPC HTTP/2 semantics to JavaScript. If the frontend benefits from typed
protobuf clients, I would put Envoy or an API gateway in front with grpc-web,
CORS, auth header allowlists, timeout alignment, and error mapping. If this is
a public API, I may prefer REST/JSON transcoding or a BFF so we can control API
shape, auth, rate limits, and backward compatibility separately from internal
service contracts.
```

---

## Senior Sound Bite

External gRPC design is a gateway decision. Native gRPC is excellent inside controlled infrastructure, but browsers and public APIs need deliberate gRPC-Web, transcoding, auth/CORS, timeout, error mapping, and compatibility policy.

## Official Source Notes

- gRPC-Web basics: <https://grpc.io/docs/platforms/web/basics/>
- Envoy gRPC-Web filter: <https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_filters/grpc_web_filter>
- Envoy JSON transcoder: <https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_filters/grpc_json_transcoder_filter>
- Protobuf JSON mapping: <https://protobuf.dev/programming-guides/proto3/#json>

