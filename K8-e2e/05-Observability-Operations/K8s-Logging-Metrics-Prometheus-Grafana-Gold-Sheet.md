# Kubernetes Observability: Logging, Metrics, and Prometheus/Grafana Gold Sheet

> Track: K8s Interview Track — Phase 5: Observability and Operations
> Goal: Build full observability for Kubernetes workloads — logs, metrics, traces — and answer "what's broken and why" in production within minutes.

---

## 0. How To Read This

Beginner focus:
- kubectl logs basics
- How stdout/stderr logging works in K8s
- What Prometheus and Grafana do

Intermediate focus:
- Fluent Bit log pipeline: collection → parsing → forwarding
- Prometheus scrape config and ServiceMonitor
- Grafana dashboards and alerting
- Custom application metrics (Micrometer, OpenTelemetry)

Senior / MAANG focus:
- High-cardinality metric explosions
- Prometheus remote write for long-term storage (Thanos/Mimir/Cortex)
- Distributed tracing: OpenTelemetry, Tempo, Jaeger
- SLO burn-rate alerting and RED/USE dashboards
- Kubernetes Events as observability signal
- Cost-effective observability at scale

---

# Topic 1: Logging Architecture

## 1. How K8s Handles Logs

```text
Container writes to STDOUT/STDERR (required — not to local files)
  ↓
Container runtime (containerd) captures stdout/stderr
  ↓
Logs stored on node: /var/log/pods/{namespace}_{pod}_{uid}/{container}/
  ↓
kubectl logs reads from this path via kubelet API
  ↓
Log rotation: kubelet rotates logs (default: 10MB, 5 files)
  ↓ (optional)
Node-level log agent (Fluent Bit DaemonSet) tails log files
  ↓
Forwards to central log store (CloudWatch, Elasticsearch, Loki, Splunk)
```

## 2. kubectl Logging Commands

```bash
# View logs for a pod
kubectl logs payment-service-abc123 -n prod

# Follow (tail -f equivalent)
kubectl logs payment-service-abc123 -n prod -f

# Previous container instance (after crash)
kubectl logs payment-service-abc123 -n prod --previous

# Specific container in multi-container pod
kubectl logs payment-service-abc123 -n prod -c envoy-proxy

# All pods matching label selector (aggregate)
kubectl logs -l app=payment-service -n prod --all-containers=true

# Time-bounded logs
kubectl logs payment-service-abc123 -n prod --since=1h
kubectl logs payment-service-abc123 -n prod --since-time="2024-01-15T10:00:00Z"

# Last N lines
kubectl logs payment-service-abc123 -n prod --tail=100
```

## 3. Fluent Bit — Log Collection Pipeline

Fluent Bit runs as a DaemonSet on every node:

```yaml
# DaemonSet (simplified)
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: kube-system
spec:
  template:
    spec:
      serviceAccountName: fluent-bit
      tolerations:
        - operator: Exists    # run on all nodes including control plane
      containers:
        - name: fluent-bit
          image: fluent/fluent-bit:2.2
          volumeMounts:
            - name: varlog
              mountPath: /var/log
            - name: varlogcontainers
              mountPath: /var/log/containers
              readOnly: true
            - name: varlogpods
              mountPath: /var/log/pods
              readOnly: true
            - name: config
              mountPath: /fluent-bit/etc
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: varlogcontainers
          hostPath:
            path: /var/log/containers
        - name: varlogpods
          hostPath:
            path: /var/log/pods
        - name: config
          configMap:
            name: fluent-bit-config
```

Note:
```text
Modern clusters usually use containerd and expose container logs through
/var/log/containers and /var/log/pods. The legacy
/var/lib/docker/containers path only applies to Docker-runtime clusters.
```

Fluent Bit config:
```ini
[SERVICE]
    Flush        5
    Log_Level    info

[INPUT]
    Name         tail
    Path         /var/log/containers/*.log
    Parser       docker
    Tag          kube.*
    Refresh_Interval 5

[FILTER]
    Name         kubernetes
    Match        kube.*
    Kube_URL     https://kubernetes.default.svc:443
    Kube_CA_File /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    Kube_Token_File /var/run/secrets/kubernetes.io/serviceaccount/token
    Merge_Log    On    # merge JSON log lines with K8s metadata

[OUTPUT]
    Name         cloudwatch_logs
    Match        kube.*
    region       us-east-1
    log_group_name /eks/prod/application
    log_stream_prefix payment-service-
    auto_create_group On
```

## 4. Loki — K8s-Native Log Aggregation

```text
Loki (Grafana Labs):
  - Stores log streams indexed by labels (not full-text index)
  - Labels: namespace, pod, container, node
  - Much cheaper than Elasticsearch for logs
  - Queried via LogQL (similar to PromQL)
  - Integration: Promtail (agent) or Fluent Bit (with Loki output)

LogQL example:
  {namespace="prod", app="payment-service"} | json | level="error"
  {namespace="prod"} |= "OOMKilled" | count_over_time([5m])
```

---

# Topic 2: Metrics with Prometheus

## 1. How Prometheus Works

```text
Prometheus scrapes metrics via HTTP:

Prometheus → GET http://payment-service:9090/metrics
            every 15 seconds (configurable)
            ← returns metrics in text format:
                http_requests_total{method="POST",status="200"} 1234
                jvm_memory_used_bytes{area="heap"} 104857600

Prometheus stores in time-series database (TSDB)
Users query via PromQL
Grafana visualizes PromQL queries
Alertmanager sends alerts when rules fire
```

## 2. kube-prometheus-stack (Helm Chart)

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  -n monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.retention=30d \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.storageClassName=gp3 \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi
```

Installs:
- Prometheus (metrics collection)
- Alertmanager (alert routing)
- Grafana (visualization)
- kube-state-metrics (K8s object state metrics)
- node-exporter DaemonSet (node hardware/OS metrics)
- Pre-built dashboards for K8s

## 3. ServiceMonitor — Scraping Application Metrics

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: payment-service-monitor
  namespace: prod
  labels:
    release: kube-prometheus-stack    # must match Prometheus selector
spec:
  selector:
    matchLabels:
      app: payment-service
  namespaceSelector:
    matchNames: ["prod"]
  endpoints:
    - port: metrics                    # named port in the Service
      path: /metrics
      interval: 15s
      scrapeTimeout: 10s
```

```yaml
# Service must expose metrics port
apiVersion: v1
kind: Service
metadata:
  name: payment-service
  labels:
    app: payment-service
spec:
  ports:
    - name: http
      port: 80
      targetPort: 8080
    - name: metrics        # named port referenced by ServiceMonitor
      port: 9090
      targetPort: 9090
```

## 4. Key Prometheus Metrics for K8s

```text
Pod / Container metrics (from kubelet):
  container_cpu_usage_seconds_total
  container_memory_usage_bytes
  container_memory_working_set_bytes      (more accurate than usage_bytes)
  container_oom_events_total              (OOM kills)
  container_cpu_cfs_throttled_periods_total (CPU throttling)

Kubernetes object metrics (from kube-state-metrics):
  kube_deployment_status_replicas_ready
  kube_pod_status_phase{phase="Pending"}
  kube_pod_container_status_restarts_total  (crash loops)
  kube_node_status_condition{condition="Ready"}
  kube_persistentvolumeclaim_status_phase

Node metrics (from node-exporter):
  node_memory_MemAvailable_bytes
  node_filesystem_avail_bytes
  node_cpu_seconds_total
  node_network_receive_bytes_total
```

## 5. PromQL Examples

```promql
# CPU usage % per pod
rate(container_cpu_usage_seconds_total{container!=""}[5m]) * 100

# Memory usage % vs limit
container_memory_working_set_bytes /
container_spec_memory_limit_bytes * 100

# Pods in CrashLoopBackOff
kube_pod_container_status_waiting_reason{reason="CrashLoopBackOff"}

# Pods pending for > 5 minutes
kube_pod_status_phase{phase="Pending"}
  and
(time() - kube_pod_start_time) > 300

# HTTP error rate (requires app to expose http_requests_total with status label)
rate(http_requests_total{status=~"5.."}[5m]) /
rate(http_requests_total[5m]) * 100

# HPA not able to scale (maxReplicas hit)
kube_horizontalpodautoscaler_status_current_replicas ==
kube_horizontalpodautoscaler_spec_max_replicas
```

## 6. PrometheusRule (Alerting)

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: payment-service-alerts
  namespace: prod
spec:
  groups:
    - name: payment-service
      rules:
        - alert: PodCrashLooping
          expr: rate(kube_pod_container_status_restarts_total{namespace="prod"}[5m]) > 0
          for: 5m
          labels:
            severity: critical
            team: payments
          annotations:
            summary: "Pod {{ $labels.pod }} is crash looping"
            description: "Container {{ $labels.container }} has restarted {{ $value }} times in 5m"

        - alert: HighMemoryUsage
          expr: |
            container_memory_working_set_bytes /
            container_spec_memory_limit_bytes > 0.9
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Container {{ $labels.container }} using >90% memory limit"
```

---

# Topic 3: Distributed Tracing

## 1. OpenTelemetry (OTel)

```text
OpenTelemetry: vendor-neutral observability framework.

Components:
  SDK:        app instruments code, creates spans and traces
  Collector:  receives, processes, exports telemetry (deployed as sidecar or DaemonSet)
  Exporters:  Jaeger, Tempo, Zipkin, OTLP

Trace = end-to-end request across services
Span = one operation within a trace (one service call)
```

OTel Collector as DaemonSet:
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

processors:
  batch:
    timeout: 1s

exporters:
  otlp:
    endpoint: tempo.monitoring:4317  # Grafana Tempo

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp]
```

## 2. Grafana Stack (Unified Observability)

```text
Prometheus → metrics
Loki       → logs
Tempo      → traces

Grafana:
  Single UI for all three
  Unified search: click on log line → linked trace → linked metrics
  This is the "LGTM stack" (Loki + Grafana + Tempo + Mimir)
```

---

# Topic 4: Kubernetes Events

Events are low-cost, fast-path observability signals:

```bash
kubectl get events -n prod --sort-by='.lastTimestamp'
kubectl get events -n prod --field-selector=reason=OOMKilling
kubectl get events -n prod --watch
```

Important event reasons:
| Reason | Meaning |
|---|---|
| `Scheduled` | Pod assigned to a node |
| `Pulled` | Container image pulled |
| `Started` | Container started |
| `Failed` | Container failed |
| `BackOff` | Restart back-off (CrashLoopBackOff) |
| `OOMKilling` | Container OOM killed |
| `FailedScheduling` | No node found (resource/affinity issue) |
| `Evicted` | Pod evicted (disk/memory pressure) |
| `NodeNotReady` | Node condition changed |

Events are stored in etcd with 1-hour TTL by default. Use event-exporter to persist to Prometheus or a log system.

---

# Topic 5: Observability Best Practices

## 1. Golden Signals (Google SRE)

For each service monitor:
```text
Latency:   p50, p95, p99 request duration (histogram)
Traffic:   requests per second
Errors:    error rate (4xx, 5xx, circuit breaks)
Saturation: CPU %, memory %, queue depth, thread pool usage
```

## 2. Alerting Principles

```text
Alert on user impact, not symptoms:
  ❌ "CPU > 80%" (not necessarily a problem)
  ✅ "Error rate > 1% for 5 minutes" (users are affected)

Alert page-worthy items only to on-call:
  ✅ Page: SLO breach, payment failures, cluster node down
  ✅ Ticket: Warning trends, gradual memory growth
  ❌ Page: CPU spikes that auto-recover in 1 minute
```

## 3. Revision Notes

- Logs: containers write to stdout/stderr; stored on node; Fluent Bit DaemonSet ships to central store
- Prometheus: scrapes `/metrics` endpoint; stores time-series; queried via PromQL
- ServiceMonitor: Prometheus Operator CRD that defines scrape targets
- kube-state-metrics: exposes K8s object state as Prometheus metrics (pod phase, restart count)
- node-exporter: exposes node OS/hardware metrics
- OOMKilled: detected via `kube_pod_container_status_restarts_total` + Events
- OpenTelemetry: vendor-neutral tracing; Collector + SDK + Tempo/Jaeger backend
- Loki: label-indexed logs (not full-text), cheap storage, queried via LogQL
- Events: kubectl get events; short TTL; use event-exporter to persist

## 4. Official Source Notes

- Logging: <https://kubernetes.io/docs/concepts/cluster-administration/logging/>
- Prometheus: <https://prometheus.io/docs/introduction/overview/>
- kube-prometheus-stack: <https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack>
- OpenTelemetry: <https://opentelemetry.io/docs/>
- Grafana Loki: <https://grafana.com/docs/loki/>

See also:
- Advanced production observability: `K8s-OpenTelemetry-SLOs-Production-Observability-Gold-Sheet.md`
