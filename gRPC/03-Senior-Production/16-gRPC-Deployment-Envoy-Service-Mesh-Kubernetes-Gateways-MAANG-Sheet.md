# 16. Deployment: Envoy, Service Mesh, Kubernetes, Gateways

## Goal

Deploy gRPC safely through real production infrastructure.

```text
client -> DNS/service discovery -> proxy/mesh -> HTTP/2 connection -> server pod -> handler/dependencies
```

---

## Kubernetes Checks

For gRPC services on Kubernetes, verify:

- service ports and target ports are correct
- port names/protocol hints support HTTP/2 where required
- readiness probes reflect serving ability
- EndpointSlices include healthy pods
- rolling deploys drain connections safely
- max request/message limits are configured
- service mesh sidecars understand gRPC traffic

---

## Envoy And Mesh Behavior

Envoy/service mesh can provide:

- mTLS
- retries and timeouts
- circuit breaking
- load balancing
- health checks
- access logs
- distributed tracing
- traffic splitting
- rate limiting

Risk: if application deadlines, mesh timeouts, and retry policies disagree, failures become confusing and can amplify load.

---

## Gateway Patterns

| Pattern | Use Case |
|---|---|
| gRPC internal only | service-to-service traffic |
| gRPC-Web | browser clients through a proxy/gateway |
| HTTP/JSON transcoding | expose REST-like API backed by gRPC |
| API gateway | auth/rate-limit/routing at edge |
| mesh ingress | controlled entry to mesh services |

---

## Proxy Timeout Alignment

Align:

- caller deadline
- client library timeout
- gateway timeout
- mesh route timeout
- load balancer idle timeout
- server max connection/stream age
- keepalive intervals

Mismatch example:

```text
client deadline 5s, Envoy route timeout 1s -> client sees early failure from proxy
```

---

## Deployment Incident Checklist

1. Can the client resolve the service name?
2. Are endpoints ready?
3. Does the path preserve HTTP/2?
4. Are TLS/mTLS identities valid?
5. Are route timeouts compatible with deadlines?
6. Are retries configured in both app and mesh?
7. Are connection draining and pod termination graceful?
8. Do metrics show one backend, all backends, or only one zone failing?

---

## Interview Sound Bite

Deploying gRPC is about more than running a server. I verify HTTP/2 support, service discovery, readiness, connection draining, proxy timeouts, mesh retry policy, mTLS, gateway mode, and observability across client, proxy, and server.