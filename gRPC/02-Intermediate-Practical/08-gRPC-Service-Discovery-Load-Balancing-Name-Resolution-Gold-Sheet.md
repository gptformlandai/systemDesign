# 08. Service Discovery, Load Balancing, Name Resolution

## Goal

Understand how a gRPC client finds and selects a server.

```text
target name -> resolver -> addresses -> load-balancing policy -> subchannel -> RPC attempt
```

---

## Why gRPC Load Balancing Is Different

gRPC uses long-lived HTTP/2 connections. If a client opens one connection through a basic L4 or L7 balancer, many multiplexed RPCs may stick to the same backend.

This makes load balancing strategy important.

---

## Patterns

| Pattern | How It Works | Tradeoff |
|---|---|---|
| client-side LB | client resolves multiple backends and picks subchannels | efficient, but clients need resolver/LB config |
| proxy LB | client connects to Envoy/NLB/mesh proxy | centralized, but proxy behavior matters |
| DNS round-robin | resolver returns multiple IPs | simple, but updates and stickiness can be weak |
| service mesh | sidecar or ambient mesh handles discovery/LB/mTLS | operational control, added complexity |

---

## Health Checking

gRPC has a standard health checking protocol. Health checks help clients and load balancers avoid unhealthy backends.

Good health checks distinguish:

- process is alive
- gRPC server is accepting calls
- dependencies are available enough for serving
- specific service is healthy

---

## Common LB Policies

| Policy | Use Case |
|---|---|
| pick_first | simple target, one preferred connection |
| round_robin | spread calls across resolved backends |
| xDS policies | Envoy/service mesh managed routing |

Exact policies vary by language/runtime.

---

## Debugging `UNAVAILABLE`

Ask:

1. Did name resolution return addresses?
2. Are backends healthy and listening?
3. Is TLS/plaintext configured correctly?
4. Is the channel in transient failure?
5. Are keepalive settings compatible with the proxy?
6. Did a deployment remove all healthy endpoints?
7. Is the LB policy picking an unhealthy subchannel?

---

## Kubernetes Notes

In Kubernetes, check:

- Service and EndpointSlice membership
- readiness probes
- port names and protocol handling
- ingress/gateway HTTP/2 support
- mesh sidecar config
- DNS resolution from client namespace
- connection draining during rolling deploys

---

## Interview Sound Bite

Because gRPC uses long-lived HTTP/2 connections, load balancing is not just one request per connection. Production systems need deliberate name resolution, health checking, subchannel/LB policy, connection draining, and proxy or mesh configuration.