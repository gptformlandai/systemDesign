# 13. Datadog Kubernetes: Container Monitoring, Cluster Agent, Autodiscovery

## Goal

Deploy and configure Datadog on Kubernetes for full cluster visibility: pod health, node resources, container metrics, and APM trace collection from all pods.

---

## Kubernetes Deployment Architecture

```text
Kubernetes Cluster
  ├── DaemonSet: datadog-agent
  │     (one pod per node, collects node metrics, pod logs, container metrics)
  ├── Deployment: datadog-cluster-agent
  │     (one pod per cluster, external metrics, admission controller)
  └── Your workload pods
        (dd-java-agent / dd-trace via auto-injection or volume mount)
```

---

## Helm Install (Recommended)

```bash
helm repo add datadog https://helm.datadoghq.com
helm repo update

helm install datadog datadog/datadog \
  --namespace datadog \
  --create-namespace \
  --set datadog.apiKey=your-api-key \
  --set datadog.clusterName=prod-cluster-us-east \
  --set datadog.site=datadoghq.com \
  --set datadog.apm.portEnabled=true \
  --set datadog.logs.enabled=true \
  --set datadog.logs.containerCollectAll=true \
  --set datadog.processAgent.enabled=true \
  --set datadog.networkMonitoring.enabled=true \
  --set clusterAgent.enabled=true \
  --set clusterAgent.metricsProvider.enabled=true \
  --set datadog.profiling.enabled=true
```

---

## DaemonSet Agent Configuration

```yaml
# values.yaml (Helm)
datadog:
  apiKey: your-api-key
  clusterName: prod-cluster
  site: datadoghq.com
  
  tags:
    - env:production
    - region:us-east-1

  logs:
    enabled: true
    containerCollectAll: true
    autoMultiLineDetection: true

  apm:
    portEnabled: true           # opens port 8126 on node
    socketEnabled: true         # UDS socket at /var/run/datadog/apm.socket

  processAgent:
    enabled: true
    processCollection: true

  networkMonitoring:
    enabled: true

  kubelet:
    tlsVerify: false           # required for some cluster setups
```

---

## Cluster Agent

The Cluster Agent runs as a single Deployment (not DaemonSet) and:

- Aggregates cluster-level metrics (less API server load)
- Provides Kubernetes External Metrics API (for HPA on Datadog metrics)
- Runs the Admission Controller for APM auto-injection
- Collects Kubernetes events

```yaml
clusterAgent:
  enabled: true
  replicas: 2                  # HA setup
  
  metricsProvider:
    enabled: true              # enables Datadog metrics as HPA target
    
  admissionController:
    enabled: true              # auto-inject dd-java-agent via webhook
    mutateUnlabelled: false    # only inject if pod has opt-in label
```

---

## APM Auto-Injection Via Admission Controller

The Admission Controller mutating webhook automatically injects the Datadog APM tracer into pods that have the opt-in annotation:

```yaml
# Add to pod template spec annotations.
metadata:
  labels:
    admission.datadoghq.com/enabled: "true"
  annotations:
    admission.datadoghq.com/java-lib.version: "latest"
```

The webhook:

1. Detects the label at pod creation.
2. Downloads the correct tracer init container.
3. Mounts the tracer into the pod.
4. Sets DD_SERVICE, DD_ENV, DD_VERSION automatically from pod labels.

---

## Autodiscovery: Per-Pod Log And Check Config

Autodiscovery allows per-container monitoring configuration via annotations.

### Log Collection Annotation

```yaml
# Pod template annotations.
annotations:
  ad.datadoghq.com/orders-service.logs: |
    [{
      "source": "java",
      "service": "orders-service",
      "log_processing_rules": [{
        "type": "multi_line",
        "name": "java_multiline",
        "pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}"
      }]
    }]
```

### Custom Check Annotation (Integration Check)

```yaml
annotations:
  ad.datadoghq.com/redis.check_names: '["redisdb"]'
  ad.datadoghq.com/redis.init_configs: '[{}]'
  ad.datadoghq.com/redis.instances: |
    [{"host": "%%host%%", "port": "6379", "password": "my-password"}]
```

`%%host%%` and `%%port%%` are template variables resolved to the container's IP and port automatically.

---

## Live Containers View

Infrastructure → Containers (Live Containers):

- Real-time list of all running containers across the cluster
- CPU, memory, network I/O per container
- Filter by namespace, pod name, image, status
- Click container → logs, process list, network connections, APM spans

---

## Key Kubernetes Metrics

```text
# Pod health.
kubernetes.pods.running{cluster_name:prod-cluster,namespace:production}
kubernetes.containers.restarts{namespace:production,pod_name:orders-*}

# Node resources.
kubernetes.cpu.requests.total{cluster_name:prod-cluster}
kubernetes.memory.requests.total{cluster_name:prod-cluster}
kubernetes.node.cpu.usage{cluster_name:prod-cluster} by {node}

# Container resources.
container.cpu.usage{kube_namespace:production,kube_deployment:orders-service}
container.memory.usage{kube_namespace:production}
container.net.rcvd_bytes{kube_namespace:production}

# OOMKill detection.
kubernetes.containers.last_seen_state{reason:OOMKilled}
```

---

## Kubernetes Monitor Examples

```text
# Alert on container restart spike.
Monitor: sum(last_5m):sum:kubernetes.containers.restarts{namespace:production}
         .rollup(max) > 5

# Alert on pod unschedulable.
Monitor: sum(last_5m):sum:kubernetes.pods.status.phase{phase:Pending,namespace:production} > 3

# Alert on HPA scaling limit hit.
Monitor: sum(last_10m):kubernetes.hpa.spec.max_replicas{namespace:production}
         - kubernetes.hpa.status.current_replicas{namespace:production} == 0
```

---

## Unified Service Tagging On Kubernetes

Apply UST labels to Deployment pod spec:

```yaml
spec:
  template:
    metadata:
      labels:
        tags.datadoghq.com/env: production
        tags.datadoghq.com/service: orders-service
        tags.datadoghq.com/version: "1.2.3"
    spec:
      containers:
        - name: orders-service
          env:
            - name: DD_ENV
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['tags.datadoghq.com/env']
            - name: DD_SERVICE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['tags.datadoghq.com/service']
            - name: DD_VERSION
              valueFrom:
                fieldRef:
                  fieldPath: metadata.labels['tags.datadoghq.com/version']
```

---

## Interview Sound Bite

Datadog on Kubernetes uses a DaemonSet node agent for per-node metrics/logs/traces and a Cluster Agent Deployment for cluster-level aggregation and the external metrics HPA provider. Autodiscovery via pod annotations configures per-container log collection and integration checks dynamically. The Admission Controller auto-injects APM tracers into annotated pods. Unified Service Tagging via pod labels propagates env/service/version to all signals for cross-correlation.
