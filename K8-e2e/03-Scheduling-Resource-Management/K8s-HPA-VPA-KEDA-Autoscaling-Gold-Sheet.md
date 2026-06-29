# Kubernetes Autoscaling: HPA, VPA, and KEDA Gold Sheet

> Track: K8s Interview Track — Phase 3: Scheduling and Resource Management
> Goal: Master horizontal and vertical pod autoscaling, custom metrics scaling, and event-driven scale-to-zero with KEDA.

---

## 0. How To Read This

Beginner focus:
- HPA based on CPU/memory
- What VPA does differently from HPA
- When to use each

Intermediate focus:
- HPA with custom metrics (Prometheus, application metrics)
- KEDA for event-driven autoscaling
- Scale-to-zero patterns
- HPA and VPA conflict

Senior / MAANG focus:
- HPA with KEDA + Prometheus at scale
- Predictive scaling patterns
- Cluster Autoscaler integration: pod scaling → node scaling chain
- Scale-down lag and cooldown tuning
- Multi-dimensional scaling (CPU + custom metric simultaneously)

---

# Topic 1: Horizontal Pod Autoscaler (HPA)

## 1. Intuition

HPA increases/decreases the number of pod replicas based on observed metrics:

```text
Target: keep average CPU at 50% per pod

Current: 3 pods, average CPU = 90%
Action:  scale UP to 6 pods (90/50 * 3 = 5.4, rounded up to 6)

Current: 6 pods, average CPU = 15%
Action:  scale DOWN to 2 pods (15/50 * 6 = 1.8, rounded up to 2)
```

## 2. HPA Spec (CPU-based)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-service-hpa
  namespace: prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60   # target 60% average CPU across pods
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 70   # target 70% average memory
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60    # wait 60s before scaling up again
      policies:
        - type: Pods
          value: 4                       # add at most 4 pods per step
          periodSeconds: 60
        - type: Percent
          value: 100                     # OR double the count per step
          periodSeconds: 60
      selectPolicy: Max                  # take the maximum result
    scaleDown:
      stabilizationWindowSeconds: 300   # wait 5 min before scaling down
      policies:
        - type: Pods
          value: 2                       # remove at most 2 pods per step
          periodSeconds: 60
```

## 3. How HPA Calculates Scaling

```text
desiredReplicas = ceil[currentReplicas * (currentMetricValue / desiredMetricValue)]

Example:
  currentReplicas = 3
  currentCPU = 90%
  targetCPU = 60%

  desiredReplicas = ceil[3 * (90/60)] = ceil[4.5] = 5

After scale up to 5 pods:
  Load distributed: 90% total across 3 → now 5 pods
  Expected CPU = 90 * 3 / 5 = 54% ≈ 60% target ✓
```

## 4. HPA Prerequisites

HPA requires `metrics-server` to be running in the cluster:
```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

Metrics server collects CPU and memory from kubelet summary API.

## 5. HPA with Custom Metrics (Prometheus Adapter)

```yaml
metrics:
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second    # Prometheus metric exposed by app
      target:
        type: AverageValue
        averageValue: 100                 # target 100 RPS per pod

  - type: Object
    object:
      metric:
        name: requests-per-second
      describedObject:
        apiVersion: networking.k8s.io/v1
        kind: Ingress
        name: payment-service-ingress
      target:
        type: Value
        value: 10k    # total 10,000 requests/sec
```

Requires: prometheus-adapter configured to expose Prometheus metrics to the K8s custom metrics API.

---

# Topic 2: Vertical Pod Autoscaler (VPA)

## 1. What VPA Does

VPA adjusts CPU and memory requests/limits based on actual usage history:

```text
HPA: scale OUT (more pods)
VPA: scale UP (bigger pods)

HPA is better for stateless services.
VPA is better for stateful services or when you don't know right resource sizes.
```

## 2. VPA Spec

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: payment-service-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  updatePolicy:
    updateMode: "Auto"    # Off | Initial | Recreate | Auto
  resourcePolicy:
    containerPolicies:
      - containerName: payment-service
        minAllowed:
          cpu: "100m"
          memory: "128Mi"
        maxAllowed:
          cpu: "4"
          memory: "8Gi"
        controlledResources: ["cpu", "memory"]
        controlledValues: RequestsAndLimits  # or RequestsOnly
```

VPA update modes:
| Mode | Behavior |
|---|---|
| `Off` | Recommends only (view via `kubectl get vpa`) |
| `Initial` | Sets resources when pod is created; no updates to running pods |
| `Recreate` | Evicts pods to apply new resources (causes restarts) |
| `Auto` | Currently same as Recreate |

## 3. HPA and VPA Conflict

```text
Problem: Both HPA and VPA target CPU:
  VPA increases CPU request on pods
  HPA sees lower CPU utilization (denominator increased)
  HPA scales DOWN replicas
  Fewer pods → CPU spikes → VPA increases again → loop

Solution: Use HPA and VPA on DIFFERENT metrics:
  HPA: scale on custom metrics (RPS, queue depth, etc.)
  VPA: manage CPU/memory requests in Off or Initial mode

Or: Only use HPA for stateless, only VPA for stateful.
```

---

# Topic 3: KEDA (Kubernetes Event-Driven Autoscaling)

## 1. Intuition

HPA scales based on resource utilization. KEDA scales based on events:

```text
KEDA sources:
  - SQS queue depth > 100 → scale up
  - Kafka consumer lag > 1000 → scale up
  - RabbitMQ queue > 50 → scale up
  - Azure Service Bus queue → scale up
  - Prometheus metric query → scale up
  - Cron schedule → scale up
  - Scale to ZERO when no events (saves cost)
```

## 2. KEDA Architecture

```text
KEDA components:
  ScaledObject: defines what to scale and what metric to watch
  ScaledJob: for batch processing (scales K8s Jobs instead of Deployments)
  TriggerAuthentication: stores credentials for external metric sources (SQS, etc.)

KEDA replaces HPA in the cluster. Under the hood, KEDA creates HPA objects.
```

## 3. KEDA ScaledObject (SQS Example)

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: payment-worker-scaledobject
  namespace: prod
spec:
  scaleTargetRef:
    name: payment-worker          # Deployment to scale
  pollingInterval: 15             # check metrics every 15 seconds
  cooldownPeriod: 60              # wait 60s after last event before scale to zero
  idleReplicaCount: 0             # scale to zero when no events
  minReplicaCount: 0              # minimum (0 = scale to zero)
  maxReplicaCount: 50             # maximum replicas
  triggers:
    - type: aws-sqs-queue
      authenticationRef:
        name: payment-sqs-auth    # TriggerAuthentication for AWS credentials
      metadata:
        queueURL: https://sqs.us-east-1.amazonaws.com/123456/payment-queue
        queueLength: "10"         # target: 10 messages per pod
        awsRegion: us-east-1
```

## 4. KEDA TriggerAuthentication

```yaml
apiVersion: keda.sh/v1alpha1
kind: TriggerAuthentication
metadata:
  name: payment-sqs-auth
  namespace: prod
spec:
  podIdentity:
    provider: aws    # use IRSA (IAM Roles for Service Accounts)
```

KEDA + IRSA: KEDA pods assume AWS IAM role via IRSA. No static credentials.

## 5. KEDA ScaledObject (Kafka Example)

```yaml
triggers:
  - type: apache-kafka
    metadata:
      bootstrapServers: kafka-0.kafka.prod.svc:9092
      consumerGroup: payment-workers
      topic: payment-events
      lagThreshold: "1000"        # scale when consumer lag > 1000 messages
      offsetResetPolicy: latest
```

## 6. KEDA ScaledJob (Batch Processing)

For one-shot batch jobs (not long-running workers):

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledJob
metadata:
  name: report-generator
spec:
  jobTargetRef:
    template:
      spec:
        containers:
          - name: reporter
            image: reporter:v2
        restartPolicy: Never
  triggers:
    - type: aws-sqs-queue
      metadata:
        queueURL: https://sqs.us-east-1.amazonaws.com/123456/reports-queue
        queueLength: "1"    # 1 job per SQS message
  minReplicaCount: 0
  maxReplicaCount: 20
```

## 7. Scale to Zero Pattern

```text
KEDA scale to zero:
  idleReplicaCount: 0  (set replicas to 0 when no messages)
  minReplicaCount: 0

Cost savings:
  Payment workers at 3AM: 0 pods running
  First message arrives in SQS: KEDA scales from 0 to 1
  First scale-from-zero latency: ~15-30 seconds (pollingInterval + pod startup)

Not suitable for:
  - Services that must respond in <1 second (use minReplicaCount: 1)
  - Services with slow startup (Java, initial connection pools)

Suitable for:
  - Async batch processors
  - Nightly report generators
  - Image processing pipelines
```

---

# Topic 4: Cluster-Level Autoscaling Integration

## 1. Pod Scaling → Node Scaling Chain

```text
HPA/KEDA detects metric threshold
  → scales Deployment (adds pods)
    → new pods are Pending (no room on nodes)
      → Cluster Autoscaler detects Pending pods
        → adds new nodes
          → pods schedule on new nodes

Same chain for scale down:
  HPA scales down replicas
    → nodes become underutilized
      → Cluster Autoscaler removes empty/underutilized nodes
```

## 2. Karpenter Integration with KEDA

```text
Karpenter is faster than Cluster Autoscaler:
  Pending pod detected → EC2 instance provisioned directly via API (30-60s)

KEDA + Karpenter:
  SQS spike → KEDA adds pods → pods Pending → Karpenter provisions instances
  Entire scale-out: ~60-90 seconds from message spike to processing

vs traditional:
  SQS spike → KEDA adds pods → pods Pending → ASG scaled → new node registers → pods scheduled
  Entire scale-out: ~4-5 minutes
```

---

# Topic 5: Interview Scenarios

**Scenario 1: Payment service latency spikes at 9 AM**
```text
Diagnosis:
  Traffic ramps 10x at business open
  HPA triggers at 60% CPU but stabilization window delays scale-up

Fix:
  1. Reduce scaleUp stabilizationWindowSeconds to 0 for fast ramp
  2. Increase maxReplicas to handle burst
  3. Use predictive scaling: KEDA cron trigger at 8:45 AM
     → pre-warm 5 minimum replicas before traffic hits

KEDA cron trigger:
triggers:
  - type: cron
    metadata:
      timezone: "America/New_York"
      start: 45 8 * * 1-5    # weekdays 8:45 AM
      end: 0 9 * * 1-5       # weekdays 9:00 AM
      desiredReplicas: "10"  # pre-warm to 10 replicas
```

**Scenario 2: SQS queue growing unboundedly at peak**
```text
Diagnosis:
  maxReplicaCount too low
  Or pod startup time too slow (takes 2 min to warm up)

Fix:
  1. Increase maxReplicaCount
  2. Optimize startup time (lazy loading, connection pooling)
  3. Use minReplicaCount: 2 instead of 0 (avoid cold start delay)
  4. Increase pollingInterval: 5 for faster response
```

## 6. Revision Notes

- HPA: scales replicas based on CPU/memory/custom metrics; requires metrics-server
- HPA formula: `desiredReplicas = ceil[current * (currentMetric / targetMetric)]`
- HPA behavior: `scaleUp.stabilizationWindowSeconds` and `scaleDown.stabilizationWindowSeconds` control thrashing
- VPA: adjusts requests/limits based on history; restarts pods (updateMode)
- HPA + VPA conflict: only use together if they target different metrics
- KEDA: event-driven scaling; scale-to-zero; extends HPA with external trigger support
- KEDA triggers: SQS, Kafka, RabbitMQ, Prometheus, Cron, and 60+ more
- ScaledJob: for batch jobs (creates K8s Jobs per event); ScaledObject for Deployments
- Karpenter: provision exact node type per pod requirement; faster than Cluster Autoscaler

## 7. Official Source Notes

- HPA: <https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/>
- VPA: <https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler>
- KEDA: <https://keda.sh/docs/>
- metrics-server: <https://github.com/kubernetes-sigs/metrics-server>
