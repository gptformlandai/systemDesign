# Kubernetes Service Mesh: Istio and Linkerd Gold Sheet

> Track: K8s Interview Track — Phase 6: Advanced Patterns
> Goal: Understand what a service mesh adds to Kubernetes networking — mTLS, traffic management, observability — and when the complexity is worth it.

---

## 0. How To Read This

Beginner focus:
- What problem a service mesh solves
- Sidecar proxy model (Envoy)
- mTLS between services

Intermediate focus:
- Traffic management: retries, timeouts, circuit breakers, load balancing
- Istio VirtualService and DestinationRule
- Observability: automatic telemetry without code changes
- Linkerd vs Istio tradeoffs

Senior / MAANG focus:
- Istio vs ambient mesh (sidecar-less)
- Multi-cluster service mesh (east-west gateway)
- Service mesh at scale: performance overhead, tuning
- Service mesh for compliance: FIPS mTLS, audit logging
- mTLS with SPIFFE/SPIRE identity

---

# Topic 1: The Problem Service Mesh Solves

## 1. Without Service Mesh

```text
Service A → Service B:
  - Which protocol? HTTP/1.1? gRPC? TLS?
  - Is traffic encrypted between services?
  - What happens when Service B is slow? Timeout? Retry?
  - How do I route 10% to v2 of Service B (canary)?
  - How do I observe latency between these two services?

All these questions require:
  - Application code changes for each service
  - Each team implements their own retry/timeout logic
  - No consistent TLS between microservices
```

## 2. With Service Mesh

```text
Service mesh handles all network concerns at infrastructure level:
  - mTLS: automatic encryption between every service pair
  - Retries: automatically retry failed requests (configurable)
  - Timeouts: apply per-route timeout without code changes
  - Circuit breakers: stop sending to failing services
  - Traffic splitting: send 10% to v2 declaratively (YAML config)
  - Observability: traces, metrics, service graph — no code instrumentation
  - RBAC: which services can talk to which (zero-trust)

Application code becomes simpler (no retry/circuit-breaker libraries).
Network policy is centrally managed and consistent.
```

---

# Topic 2: Architecture — Sidecar Model

## 1. Envoy Sidecar

```text
Every pod gets an Envoy sidecar injected automatically:

Pod (before mesh):
  [container: payment-service]

Pod (in mesh):
  [container: payment-service] + [sidecar: envoy-proxy]

Envoy intercepts ALL traffic:
  Inbound:  Internet → Envoy (port 15001) → payment-service (port 8080)
  Outbound: payment-service → Envoy → target service

Envoy sees all traffic → can apply policies, collect metrics, trace.
```

## 2. Control Plane vs Data Plane

```text
Data Plane (Envoy sidecars):
  - Actual traffic routing
  - TLS encryption/decryption
  - Metrics collection
  - One per pod

Control Plane (Istiod):
  - Pushes config to all Envoy sidecars via xDS API
  - Issues mTLS certificates (SPIFFE/SVID)
  - Service discovery
  - One cluster-level deployment
```

## 3. Automatic Sidecar Injection

```yaml
# Enable injection for namespace
apiVersion: v1
kind: Namespace
metadata:
  name: prod
  labels:
    istio-injection: enabled    # all pods in this namespace get sidecar

# Or per-pod:
spec:
  template:
    metadata:
      annotations:
        sidecar.istio.io/inject: "true"
```

---

# Topic 3: Mutual TLS (mTLS)

## 1. What mTLS Provides

```text
Standard TLS: client verifies server identity
mTLS: BOTH sides verify each other's identity

In Istio:
  - Every service gets a SPIFFE certificate (spiffe://cluster.local/ns/prod/sa/payment-service-sa)
  - Certificate = workload identity
  - All service-to-service traffic is encrypted AND mutually authenticated
  - Even if an attacker is inside the cluster, they can't impersonate a service

Certificate management:
  - Istiod acts as CA
  - Envoy sidecars receive certs via xDS
  - Certs rotate automatically (default 24h)
```

## 2. PeerAuthentication (mTLS Policy)

```yaml
# Strict mTLS for entire namespace
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: prod
spec:
  mtls:
    mode: STRICT    # reject non-mTLS connections

# PERMISSIVE: accept both mTLS and plain text (for migration)
# DISABLE: no mTLS (not recommended)
```

```yaml
# Port-level mTLS exception (health checks from kubelet can't do mTLS)
spec:
  mtls:
    mode: STRICT
  portLevelMtls:
    8080:
      mode: PERMISSIVE    # allow kubelet health check probes
```

---

# Topic 4: Traffic Management

## 1. VirtualService

Defines routing rules (like a programmable L7 load balancer):

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-service
  namespace: prod
spec:
  hosts:
    - payment-service              # the K8s service name
  http:
    # Canary: route 10% to v2
    - match:
        - headers:
            x-canary-user:
              exact: "true"         # header-based routing
      route:
        - destination:
            host: payment-service
            subset: v2
    
    - route:
        - destination:
            host: payment-service
            subset: v1
          weight: 90
        - destination:
            host: payment-service
            subset: v2
          weight: 10
      
      # Retry configuration
      retries:
        attempts: 3
        perTryTimeout: 2s
        retryOn: gateway-error,connect-failure,retriable-4xx
      
      # Timeout
      timeout: 5s
      
      # Fault injection (chaos testing)
      fault:
        delay:
          fixedDelay: 100ms
          percentage:
            value: 5              # delay 5% of requests
```

## 2. DestinationRule

Defines traffic policies for a destination (like circuit breakers, load balancing):

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payment-service
  namespace: prod
spec:
  host: payment-service
  trafficPolicy:
    loadBalancer:
      simple: LEAST_CONN          # or ROUND_ROBIN, RANDOM, PASSTHROUGH
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:              # circuit breaker
      consecutive5xxErrors: 5      # eject after 5 consecutive errors
      interval: 10s                # check every 10s
      baseEjectionTime: 30s        # eject for 30s minimum
      maxEjectionPercent: 50       # eject at most 50% of endpoints
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```

## 3. Gateway (External Traffic)

```yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: prod-gateway
  namespace: istio-system
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 443
        name: https
        protocol: HTTPS
      tls:
        mode: SIMPLE
        credentialName: myapp-tls
      hosts:
        - "*.myapp.com"

---
# VirtualService to route external traffic
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: payment-external
spec:
  hosts:
    - payment.myapp.com
  gateways:
    - prod-gateway              # reference the Gateway
    - mesh                      # also apply in-cluster
  http:
    - route:
        - destination:
            host: payment-service
            port:
              number: 80
```

---

# Topic 5: Istio Authorization Policy (Zero-Trust)

```yaml
# Deny all by default
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: prod
spec:
  {}    # empty = deny all

---
# Allow specific service-to-service access
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-api-gateway-to-payment
  namespace: prod
spec:
  selector:
    matchLabels:
      app: payment-service
  action: ALLOW
  rules:
    - from:
        - source:
            principals:
              # SPIFFE identity of api-gateway service
              - "cluster.local/ns/prod/sa/api-gateway-sa"
      to:
        - operation:
            methods: ["POST", "GET"]
            paths: ["/api/payments*"]
```

This is identity-based (mTLS certificate), not IP-based. Much stronger than Network Policies.

---

# Topic 6: Istio Observability

Istio provides telemetry without any code changes:

```text
Distributed traces:
  All requests get a trace ID propagated via headers (x-request-id, b3)
  Jaeger/Tempo receives traces from Envoy sidecars
  See full request path across all services

Metrics:
  Envoy exposes: request count, latency (p50/p95/p99), error rate per service pair
  Scraped by Prometheus
  Istio dashboard in Grafana (Kiali for service topology)

Service graph:
  Kiali: visual map of all services and their traffic
  See error rates, latency, traffic volume on live graph
```

---

# Topic 7: Linkerd (Alternative to Istio)

```text
Linkerd philosophy:
  - Simplicity first (no envoy, uses lighter Linkerd-proxy/linkerd2-proxy)
  - Less configuration overhead
  - Faster: Rust-based proxy (lower latency than Envoy)

Linkerd provides:
  ✅ mTLS (automatic, zero config)
  ✅ L7 observability (golden signals per route)
  ✅ Traffic splits (canary) via SMI (Service Mesh Interface)
  ❌ No fault injection
  ❌ No advanced L7 routing (no header-based routing in base)
  ❌ No Authorization Policies (only mTLS identity)

Comparison:
  Istio: feature-rich, complex, Envoy-based
  Linkerd: simple, fast, fewer features, Rust-based

Choose Linkerd: you need mTLS + observability with minimal ops overhead
Choose Istio: you need advanced traffic management, RBAC, multi-cluster
```

---

# Topic 8: Ambient Mesh (Istio Sidecar-less)

```text
Problem with sidecar model:
  - Every pod gets an Envoy sidecar (CPU/memory overhead)
  - Envoy versioning tied to app pod restart
  - Complex for large pod counts

Istio Ambient Mesh (1.21+):
  - No sidecar per pod
  - ztunnel: per-node DaemonSet handles L4 mTLS and telemetry
  - waypoint proxy: namespace-level proxy for L7 traffic management

Benefits:
  - Reduced resource overhead (~30-50% less)
  - Independent upgrade of mesh from application
  - Simpler pod spec (no injection)

Status: beta in Istio 1.21 (2024)
```

---

# Topic 9: Interview Traps

| Trap | Reality |
|---|---|
| "Service mesh replaces Network Policies" | They complement each other. Network Policy: IP-based, L3/L4. Mesh: identity-based, L7. Use both. |
| "Istio is free" | Resource cost: Envoy sidecar per pod adds ~50-100ms cold start, 100-200MB memory per pod |
| "mTLS means no need for app-level auth" | mTLS authenticates service identity; user auth (JWT) is still app responsibility |
| "Linkerd supports all Istio features" | Linkerd is simpler; no Istio-style L7 routing or AuthorizationPolicy |
| "Circuit breaker in DestinationRule replaces app-level circuit breaker" | Mesh circuit breaker is connection/request level; app-level is for business logic (payment API down) |

---

# Topic 10: Revision Notes

- Service mesh: mTLS, retries, timeouts, circuit breakers, traffic splitting, automatic telemetry
- Sidecar model: Envoy injected per pod; intercepts all in/out traffic
- Istiod: control plane; distributes config via xDS; issues SPIFFE certs
- PeerAuthentication: mTLS mode per namespace/port (STRICT/PERMISSIVE/DISABLE)
- VirtualService: routing rules (canary weights, header routing, retries, timeouts, fault injection)
- DestinationRule: circuit breaker (outlierDetection), connection pools, subsets, load balancing
- AuthorizationPolicy: SPIFFE-identity-based allow/deny (stronger than Network Policy)
- Linkerd: simpler, Rust proxy, less features; good for mTLS + observability without complexity
- Ambient mesh: sidecar-less Istio (ztunnel DaemonSet); lower overhead

## Official Source Notes

- Istio: <https://istio.io/latest/docs/>
- Linkerd: <https://linkerd.io/docs/>
- SPIFFE: <https://spiffe.io/>
- Istio Ambient: <https://istio.io/latest/docs/ambient/>
- Envoy: <https://www.envoyproxy.io/docs>
