# 32. Datadog Universal Service Monitoring: Code-Free Service Discovery

## Goal

Understand Universal Service Monitoring (USM), when to use it, how it differs from APM, and how it helps discover services that are not yet instrumented.

---

## Mental Model

APM is like putting a camera inside the application code.

USM is like watching network traffic from the side of the road.

```text
APM -> code-level traces, spans, errors, flame graphs
USM -> service-to-service traffic, request rate, errors, latency, dependencies
```

USM gives fast visibility without code changes. APM gives deeper visibility after instrumentation.

---

## Why It Exists

Large environments often contain:

- Legacy services with no tracing library.
- Third-party services.
- Services owned by teams that are slow to instrument.
- Containers where code changes require long release cycles.
- Newly discovered internal services.

USM gives an observability baseline before full APM rollout.

---

## What USM Captures

| Capability | USM |
|---|---|
| Service discovery | Yes |
| Request rate | Yes |
| Error rate | Yes |
| Latency | Yes |
| HTTP dependencies | Yes |
| Service map visibility | Yes |
| Code-level spans | No |
| Function/method-level flame graph | No |
| DB query text | No |
| Custom span tags | No |

---

## USM Data Flow

```text
Application traffic
  -> Datadog Agent observes network/system-level telemetry
  -> USM detects service, endpoint, and dependency behavior
  -> Datadog creates service-level metrics
  -> Service appears in Service Catalog and Service Map
  -> Team can add monitors, dashboards, and ownership
```

USM depends heavily on consistent service naming and unified tags.

---

## Key Metric Family

USM produces service-level HTTP metrics such as:

```text
universal.http.server.request.count
universal.http.server.request.duration
universal.http.server.request.errors
universal.http.client.request.count
universal.http.client.request.duration
```

Use these for basic health monitors when APM is not available.

---

## USM vs APM Decision Table

| Situation | Use USM | Use APM |
|---|---:|---:|
| Need instant service inventory | Yes | Later |
| Cannot change code | Yes | No |
| Need endpoint latency/error rate | Yes | Yes |
| Need slow method or DB query root cause | No | Yes |
| Need distributed trace flame graph | No | Yes |
| Need service map before instrumentation | Yes | Later |
| Need custom business spans | No | Yes |

Best practice: use USM to discover and prioritize, then use APM for critical services.

---

## Rollout Strategy

```text
Phase 1: Enable USM in production clusters and hosts.
Phase 2: Identify all discovered services with no owner.
Phase 3: Add Software Catalog ownership metadata.
Phase 4: Add monitors using universal HTTP metrics.
Phase 5: Select top critical services for full APM instrumentation.
Phase 6: Use APM for deep debugging and USM for broad coverage.
```

---

## Kubernetes Use Case

```text
Problem:
  80 services run in Kubernetes, but only 20 have APM.

Approach:
  - Enable Datadog Agent with USM.
  - Discover all HTTP services and service-to-service dependencies.
  - Use pod labels to apply env/service/version/team tags.
  - Import services into Software Catalog.
  - Prioritize APM rollout for tier-0 and tier-1 services.
```

---

## Monitor Examples

### Error Rate Monitor

```text
sum:universal.http.server.request.errors{service:orders-service,env:prod}.as_count()
/
sum:universal.http.server.request.count{service:orders-service,env:prod}.as_count()
> 0.02
```

### Latency Monitor

```text
p95:universal.http.server.request.duration{service:orders-service,env:prod}
> 0.5
```

Exact query syntax can vary by metric type and account configuration. The key idea is that USM gives enough request, error, and latency data to create health alerts before APM exists.

---

## Failure Modes

| Failure | Symptom | Fix |
|---|---|---|
| Missing unified tags | Services appear with messy names | Standardize `service`, `env`, `version` |
| Sidecar/proxy hides service identity | Service map is confusing | Add explicit labels and service metadata |
| Non-HTTP protocol unsupported or partial | Missing endpoint detail | Add APM or protocol-specific integration |
| No ownership metadata | Catalog has orphan services | Add entity definitions |
| Treating USM as full tracing | Root cause remains shallow | Instrument critical code paths with APM |

---

## Practical Question

> You inherited a Kubernetes platform with 150 services and almost no tracing. Leadership wants service dependency visibility in one week. What do you do?

---

## Strong Answer

I would enable USM first because it gives service discovery and request/error/latency visibility without waiting for code changes. Then I would enforce unified service tags through Kubernetes labels and admission controls so services appear consistently in Service Map and Software Catalog.

Next, I would use Software Catalog to assign owners and tier the services. For tier-0 and tier-1 services, I would schedule full APM instrumentation because USM cannot show method-level spans, DB statements, or custom business context. USM is the fast discovery layer; APM is the deep diagnostic layer.

---

## Interview Sound Bite

USM is the quickest way to get service-level visibility without code changes. It is excellent for discovery, dependency mapping, and baseline RED metrics, but it does not replace APM for flame graphs, custom spans, and root-cause debugging.
