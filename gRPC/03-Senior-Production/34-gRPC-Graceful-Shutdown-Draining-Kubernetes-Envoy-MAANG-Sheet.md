# 34. Graceful Shutdown, Draining, Kubernetes, Envoy, And Long-Lived Streams

## Goal

Deploy and terminate gRPC services without breaking in-flight unary calls, long-lived streams, or client load-balancing behavior.

```text
readiness false -> health NOT_SERVING -> stop accepting new RPCs -> drain streams -> GOAWAY/close -> terminate
```

---

## 1. Why gRPC Shutdown Is Special

gRPC commonly uses long-lived HTTP/2 connections. A rolling deploy is not just "stop old pod, start new pod."

Failure modes:

- Clients keep using an old HTTP/2 connection.
- New RPCs arrive after Kubernetes removed the pod from endpoints.
- Streams are cut during SIGTERM.
- Envoy route timeout is shorter than app drain window.
- Health check stays `SERVING` until the process exits.
- The client retries non-idempotent work after connection reset.

---

## 2. Shutdown Sequence

Recommended server behavior:

```text
1. Receive SIGTERM.
2. Flip gRPC health service to NOT_SERVING.
3. Flip Kubernetes readiness to false.
4. Stop accepting new RPCs where framework supports it.
5. Let in-flight unary RPCs finish within grace period.
6. For streams, send terminal event or cancellation guidance.
7. Close server gracefully.
8. Force close only after grace budget expires.
```

Kubernetes behavior:

```text
pod deletion -> preStop hook -> SIGTERM -> terminationGracePeriodSeconds -> SIGKILL
```

The app must fit its drain logic inside that budget.

---

## 3. Kubernetes Pattern

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-grpc
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  template:
    spec:
      terminationGracePeriodSeconds: 60
      containers:
        - name: app
          image: example/payment-grpc:1.0
          ports:
            - name: grpc
              containerPort: 8443
          readinessProbe:
            grpc:
              port: 8443
            initialDelaySeconds: 5
            periodSeconds: 5
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
```

Notes:

- `preStop sleep` is a blunt tool; app-level health/drain is better.
- Use `maxUnavailable: 0` for critical services.
- Use PDBs for voluntary disruptions.
- Readiness should reflect gRPC serving ability, not only process liveness.

---

## 4. Health Protocol During Drain

The gRPC health service should be service-aware:

```text
payments.v1.PaymentService -> SERVING
admin.v1.AdminService      -> NOT_SERVING
```

During shutdown:

```text
PaymentService: SERVING -> NOT_SERVING
Kubernetes readiness: true -> false
Load balancer: remove endpoint
Existing calls: drain or terminate by policy
```

Do not rely on TCP liveness alone. A process can be alive while the service is not ready for a specific RPC.

---

## 5. Envoy And Mesh Drain

Envoy/mesh must align with app behavior:

```text
drain timeout >= app drain time
route timeout >= expected RPC deadline where appropriate
idle timeout > expected stream idle period, or use keepalive carefully
max connection duration configured deliberately
```

Common mismatch:

```text
App terminationGracePeriodSeconds: 60s
Envoy drain timeout: 5s
Result: proxy closes connections before app finishes graceful drain
```

---

## 6. Long-Lived Stream Strategy

Streams need explicit protocol decisions:

| Decision | Example |
|---|---|
| server drain message | `SERVER_DRAINING` event before close |
| resume token | client reconnects from last seen offset |
| max stream age | reconnect every 30 minutes to redistribute load |
| heartbeat | detect half-open connections |
| cancellation behavior | client knows whether to retry/resume |

Do not run infinite streams with no resume, no max age, and no drain event.

---

## 7. GOAWAY Mental Model

HTTP/2 `GOAWAY` tells peers the endpoint is closing and helps stop new streams on that connection.

Interview-safe explanation:

```text
GOAWAY is transport-level drain signaling. It is not a business-level guarantee
that every in-flight RPC completed. Application health, deadlines, idempotency,
and stream resume still matter.
```

---

## 8. Rollout Test Plan

```text
1. Start continuous unary calls with deadlines.
2. Start a long-lived server stream.
3. Trigger rolling restart.
4. Verify readiness flips before termination.
5. Verify no new RPCs route to draining pod.
6. Verify unary calls either complete or fail with expected retry-safe status.
7. Verify stream receives drain/resume behavior.
8. Verify traffic redistributes across new pods.
```

Metrics to watch:

- RPC status by method and pod.
- active streams by pod.
- endpoint readiness transitions.
- connection count by backend.
- retry/hedge attempts.
- server shutdown duration.

---

## 9. Interview Scenario

> During rolling deploy, p99 latency spikes and some bidirectional streams reset. What is likely wrong?

Good answer:

```text
I would check whether shutdown is coordinated across Kubernetes, gRPC health,
and Envoy. If pods receive SIGTERM while still advertising SERVING/readiness,
clients can start new RPCs on draining pods. If Envoy drain timeout is shorter
than app drain, streams reset. For long-lived streams, I would require max stream
age, heartbeat, drain event, and resume token. The fix is to flip health to
NOT_SERVING, fail readiness, stop new RPCs, drain within termination grace, and
test rolling restarts under active unary and streaming load.
```

---

## Senior Sound Bite

Graceful gRPC rollout is a choreography: health state, Kubernetes readiness, proxy drain, HTTP/2 connection behavior, deadlines, idempotency, and stream resume all need to agree. If one layer lies, deploys become production incidents.

## Official Source Notes

- Health checking: <https://grpc.io/docs/guides/health-checking/>
- Core lifecycle: <https://grpc.io/docs/what-is-grpc/core-concepts/>
- Keepalive: <https://grpc.io/docs/guides/keepalive/>
- Kubernetes gRPC probes: <https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/>

