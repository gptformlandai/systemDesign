# Kubernetes OpenTelemetry, SLOs, and Production Observability Gold Sheet

> Track: K8s Interview Track - Phase 5: Observability and Operations Plus
> Goal: Design observability that catches user-impacting failures, controls telemetry cost, and supports fast incident response in large Kubernetes platforms.

---

## 0. How To Read This

Beginner focus:
- Logs explain what happened.
- Metrics show trends and alerts.
- Traces show request path across services.

Intermediate focus:
- OpenTelemetry standardizes traces, metrics, and logs.
- SLOs turn telemetry into reliability targets.
- Prometheus, Loki, Tempo, Thanos, Mimir, and CloudWatch solve different pieces.

Senior / MAANG focus:
- Alert on symptoms users feel, not every technical fluctuation.
- Control cardinality and retention costs.
- Design multi-cluster, HA observability with clear ownership and runbooks.

---

# Topic 1: Observability Mental Model

```text
Logs:
  Discrete facts. Good for debugging one request or one pod.

Metrics:
  Numeric time series. Good for alerts, dashboards, capacity, and SLOs.

Traces:
  Causal path. Good for distributed latency and dependency debugging.

Events:
  Kubernetes control-plane signals. Good for scheduling, image pull, probe, and node issues.
```

Golden rule:
```text
You do not have observability because data exists.
You have observability when an on-call engineer can answer:
  "What is broken, who is affected, why, and what should I do next?"
```

---

# Topic 2: OpenTelemetry Collector Patterns

## 1. Agent Mode

```text
Pod or node sends telemetry to local Collector agent.
Agent batches, enriches, samples, and forwards to backend.
```

Good for:
- Reducing app-side exporter complexity.
- Adding Kubernetes metadata.
- Local buffering and retry.

## 2. Gateway Mode

```text
Apps/agents send to shared Collector Deployment.
Gateway handles auth, routing, sampling, and backend fanout.
```

Good for:
- Centralized policy.
- Multi-backend routing.
- Tail-based sampling.

## 3. Combined Pattern

```text
App -> node Collector DaemonSet -> regional Collector gateway -> backend
```

This is common in larger clusters because it separates local enrichment from central governance.

## 4. Collector Pipeline Example

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
  k8sattributes:
    auth_type: serviceAccount
  batch:
    timeout: 5s
    send_batch_size: 8192

exporters:
  otlp/tempo:
    endpoint: tempo.monitoring.svc:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, k8sattributes, batch]
      exporters: [otlp/tempo]
```

---

# Topic 3: RED, USE, and Golden Signals

## 1. RED For Services

```text
Rate:
  Requests per second.

Errors:
  Failed requests per second or error percentage.

Duration:
  Latency distribution, usually p50/p95/p99.
```

Best for microservices, APIs, and user-facing workloads.

## 2. USE For Infrastructure

```text
Utilization:
  How busy is the resource?

Saturation:
  How much queued work exists?

Errors:
  Hardware, kernel, filesystem, network, or runtime errors.
```

Best for nodes, disks, network interfaces, queues, and storage.

## 3. Kubernetes Golden Signals

| Layer | Signals |
|---|---|
| Workload | request rate, error rate, latency, restarts, readiness |
| Pod | CPU/memory, OOMKilled, throttling, filesystem usage |
| Node | allocatable vs requested, pressure, disk, network, kubelet health |
| Control plane | API latency, etcd latency, workqueue depth, admission failures |
| Platform | deploy success, rollback rate, alert volume, cost per namespace |

---

# Topic 4: SLOs and Burn-Rate Alerts

## 1. SLO Terms

```text
SLI:
  Measured signal, such as "successful HTTP requests / total HTTP requests".

SLO:
  Target, such as "99.9% monthly successful requests".

Error budget:
  Allowed failure: 0.1% for a 99.9% SLO.
```

## 2. Example PromQL

Availability SLI:
```promql
sum(rate(http_requests_total{job="payment",code!~"5.."}[5m]))
/
sum(rate(http_requests_total{job="payment"}[5m]))
```

Latency SLI:
```promql
histogram_quantile(
  0.95,
  sum by (le) (rate(http_request_duration_seconds_bucket{job="payment"}[5m]))
)
```

## 3. Burn-Rate Alert Shape

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: payment-slo-alerts
  namespace: monitoring
spec:
  groups:
    - name: payment-slo
      rules:
        - alert: PaymentFastBurn
          expr: |
            (
              1 -
              (
                sum(rate(http_requests_total{job="payment",code!~"5.."}[5m]))
                /
                sum(rate(http_requests_total{job="payment"}[5m]))
              )
            ) > (14.4 * 0.001)
          for: 5m
          labels:
            severity: page
          annotations:
            summary: "Payment service is burning error budget quickly"
```

The constants vary by SLO window and alerting strategy. The interview point is to alert on budget burn, not raw CPU spikes.

---

# Topic 5: Prometheus at Scale

## 1. HA Pattern

```text
Two Prometheus replicas scrape the same targets.
Both remote-write to long-term storage.
Alertmanager deduplicates alerts.
```

## 2. Long-Term Storage Options

| Backend | Why Use It |
|---|---|
| Thanos | Global query, object storage, HA deduplication |
| Mimir | Multi-tenant scalable metrics backend |
| Cortex | Older CNCF multi-tenant metrics backend |
| AMP | AWS-managed Prometheus-compatible backend |

## 3. Cardinality Budget

Bad label:
```text
user_id, request_id, trace_id, email, full URL with IDs
```

Good label:
```text
service, route template, method, status_code, namespace, cluster
```

Cardinality incident pattern:
```text
New label with unbounded values -> millions of time series -> Prometheus memory
spike -> slow queries -> missed scrapes -> blind on-call.
```

Prevention:
- Metric naming review in PRs.
- Drop high-cardinality labels at Collector or Prometheus relabeling.
- Per-namespace metrics budget.
- Alert on active series count and scrape sample ingestion rate.

---

# Topic 6: Incident Workflow

```text
1. Start from SLO alert: who is affected?
2. Check recent deploys and config changes.
3. Look at service RED metrics.
4. Follow one bad trace.
5. Check pod events, restarts, readiness, and resource throttling.
6. Check dependencies: database, queue, cache, external API.
7. Mitigate: rollback, scale, shed load, fail over, or disable feature flag.
8. After incident: tune alert, add dashboard panel, update runbook.
```

Kubernetes-specific debug commands:
```bash
kubectl get events -A --sort-by=.lastTimestamp
kubectl top pods -A --containers
kubectl get --raw /readyz?verbose
kubectl get endpointslices -n prod -l kubernetes.io/service-name=payment
```

---

# Topic 7: Interview Scenario

> Your cluster has Prometheus and Grafana, but incidents still take 45 minutes to diagnose. What would you improve?

Strong answer:
```text
I would start by checking whether alerts map to user impact. If pages are based
on CPU or pod restarts alone, on-call has to infer business impact. I would add
SLOs for key services, RED dashboards per service, USE dashboards for nodes and
storage, and trace correlation from logs to traces. I would also control metric
cardinality, define ownership labels, persist Kubernetes events, and create
runbooks linked from alerts. For scale, I would run Prometheus HA with remote
write to Thanos, Mimir, Cortex, or a managed backend.
```

---

# Topic 8: Revision Notes

- Observability answers "what, who, why, what next".
- RED is best for services; USE is best for infrastructure.
- SLO burn-rate alerts reduce noisy symptom-based paging.
- OpenTelemetry Collector can run as agent, gateway, or both.
- Prometheus at scale needs HA, remote storage, and cardinality control.
- Kubernetes events are high-value signals but short-lived unless exported.

## Official Source Notes

- Kubernetes observability: <https://kubernetes.io/docs/concepts/cluster-administration/system-metrics/>
- OpenTelemetry Collector: <https://opentelemetry.io/docs/collector/>
- Prometheus alerting: <https://prometheus.io/docs/practices/alerting/>
- Thanos: <https://thanos.io/>
- Grafana Mimir: <https://grafana.com/oss/mimir/>

