# Kubernetes Operators and Custom Resource Definitions Gold Sheet

> Track: K8s Interview Track — Phase 6: Advanced Patterns
> Goal: Understand how to extend Kubernetes with custom resources and controllers — the foundation of every K8s platform product (databases, ML, service mesh).

---

## 0. How To Read This

Beginner focus:
- What CRDs are and why they exist
- What an Operator does differently from a Deployment
- Examples of popular Operators

Intermediate focus:
- Writing a simple CRD
- Controller reconciliation loop
- Finalizers for cleanup on delete

Senior / MAANG focus:
- Operator-SDK and Kubebuilder frameworks
- Status subresource and conditions
- Webhook validation/mutation for CRDs
- Operator multi-tenancy patterns
- Production Operator best practices (leader election, metrics, RBAC)

---

# Topic 1: Why Operators Exist

## 1. The Problem Helm/Deployments Can't Solve

```text
Helm installs a PostgreSQL cluster.
But then:
  - How do you handle failover when the primary goes down?
  - How do you add a replica and have it automatically join the cluster?
  - How do you trigger a coordinated rolling upgrade that respects Postgres's protocol?
  - How do you take backups and restore them?

Answer: human operator knowledge encoded as automation.

An Operator:
  - Defines custom K8s resources (PostgresCluster, KafkaTopic, etc.)
  - Runs a controller that watches those resources
  - Takes expert-level actions automatically (failover, scaling, backup)
```

## 2. Operator vs Deployment

| Feature | Deployment | Operator |
|---|---|---|
| Manages | Stateless pods | Complex stateful systems |
| Knowledge | Generic (just restart pods) | Domain-specific (knows Postgres, Kafka, etc.) |
| Actions | Rolling update, scale | Failover, backup, upgrade, user management |
| API | Built-in (apps/v1) | Custom (CRD) |

## 3. Popular Operators in Production

```text
Databases:
  Zalando PostgreSQL Operator        — PostgreSQL HA clusters
  CockroachDB Operator               — CockroachDB clusters
  MongoDB Community Operator         — MongoDB replica sets
  CloudNativePG                      — PostgreSQL on K8s
  Vitess Operator                    — MySQL sharding (used at YouTube)

Messaging:
  Strimzi Kafka Operator             — Kafka clusters, topics, users
  RabbitMQ Cluster Operator          — RabbitMQ HA

Observability:
  Prometheus Operator                — Prometheus, Alertmanager, ServiceMonitor
  Elastic Cloud on K8s (ECK)        — Elasticsearch, Kibana, APM

ML:
  Kubeflow                           — ML pipelines, training jobs
  KServe                             — ML model serving

Service Mesh:
  Istio Operator                     — Istio control plane
  
Storage:
  Rook                               — Ceph storage clusters
```

---

# Topic 2: Custom Resource Definitions (CRDs)

## 1. What a CRD Does

A CRD extends the Kubernetes API with a new resource type:

```text
Built-in: apiVersion: apps/v1, Kind: Deployment
Custom:   apiVersion: payments.mycompany.com/v1, Kind: PaymentProcessor
```

After CRD is applied:
```bash
kubectl get paymentprocessors -n prod
kubectl apply -f my-payment-processor.yaml
kubectl describe paymentprocessor main-processor -n prod
```

## 2. CRD Spec

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: paymentprocessors.payments.mycompany.com
spec:
  group: payments.mycompany.com
  versions:
    - name: v1
      served: true
      storage: true          # stored in etcd with this version
      schema:
        openAPIV3Schema:
          type: object
          properties:
            spec:
              type: object
              required: [replicas, region]
              properties:
                replicas:
                  type: integer
                  minimum: 1
                  maximum: 10
                region:
                  type: string
                  enum: ["us-east-1", "us-west-2", "eu-west-1"]
                paymentMethods:
                  type: array
                  items:
                    type: string
            status:
              type: object
              properties:
                phase:
                  type: string
                readyReplicas:
                  type: integer
      subresources:
        status: {}           # enables status subresource (separate update endpoint)
      additionalPrinterColumns:
        - name: Replicas
          type: integer
          jsonPath: .spec.replicas
        - name: Phase
          type: string
          jsonPath: .status.phase
        - name: Age
          type: date
          jsonPath: .metadata.creationTimestamp
  scope: Namespaced          # or Cluster
  names:
    plural: paymentprocessors
    singular: paymentprocessor
    kind: PaymentProcessor
    shortNames: ["pp"]
```

## 3. Custom Resource (Instance of CRD)

```yaml
apiVersion: payments.mycompany.com/v1
kind: PaymentProcessor
metadata:
  name: main-processor
  namespace: prod
spec:
  replicas: 3
  region: us-east-1
  paymentMethods:
    - credit-card
    - bank-transfer
    - crypto
```

---

# Topic 3: Operator Controller Pattern

## 1. The Reconciliation Loop

Every controller runs the same pattern:

```go
func (r *PaymentProcessorReconciler) Reconcile(ctx context.Context, req reconcile.Request) (reconcile.Result, error) {
    // 1. Fetch the custom resource
    processor := &paymentsv1.PaymentProcessor{}
    if err := r.Get(ctx, req.NamespacedName, processor); err != nil {
        return reconcile.Result{}, client.IgnoreNotFound(err)
    }

    // 2. Observe actual state (what exists in cluster)
    deployment := &appsv1.Deployment{}
    err := r.Get(ctx, types.NamespacedName{
        Name:      processor.Name + "-deployment",
        Namespace: processor.Namespace,
    }, deployment)

    // 3. Compare desired (spec) vs actual
    if apierrors.IsNotFound(err) {
        // Create deployment if missing
        newDeployment := r.buildDeployment(processor)
        return reconcile.Result{}, r.Create(ctx, newDeployment)
    }

    // 4. Update if different
    if deployment.Spec.Replicas != &processor.Spec.Replicas {
        deployment.Spec.Replicas = &processor.Spec.Replicas
        return reconcile.Result{}, r.Update(ctx, deployment)
    }

    // 5. Update status
    processor.Status.Phase = "Running"
    processor.Status.ReadyReplicas = deployment.Status.ReadyReplicas
    r.Status().Update(ctx, processor)

    return reconcile.Result{}, nil
}
```

## 2. Operator Build Frameworks

```text
Kubebuilder (CNCF):
  - Go-based framework for writing controllers
  - Generates CRD YAML, RBAC, Dockerfile, Makefile
  - Uses controller-runtime library

Operator-SDK (Red Hat):
  - Supports Go, Ansible, Helm
  - Can wrap an existing Helm chart as an Operator
  - Integrates with OperatorHub

Metacontroller (simple):
  - Write controller logic in any language via webhooks
  - Kubernetes calls your HTTP endpoint with current and desired state

kopf (Python):
  - Python-based Operator framework
  - Use for: simple operators, Python shop
```

## 3. Creating an Operator with Operator-SDK

```bash
# Initialize
operator-sdk init --domain=mycompany.com --repo=github.com/myorg/payment-operator

# Create API (CRD + Controller)
operator-sdk create api \
  --group=payments \
  --version=v1 \
  --kind=PaymentProcessor \
  --resource --controller

# Generate CRD manifests from Go type annotations
make generate manifests

# Run locally against cluster
make run

# Build and deploy
make docker-build IMG=my-registry/payment-operator:v1
make docker-push IMG=my-registry/payment-operator:v1
make deploy IMG=my-registry/payment-operator:v1
```

---

# Topic 4: Finalizers

Finalizers allow operators to perform cleanup before a custom resource is deleted:

```go
const myFinalizer = "payments.mycompany.com/finalizer"

func (r *Reconciler) Reconcile(ctx context.Context, req reconcile.Request) (reconcile.Result, error) {
    processor := &paymentsv1.PaymentProcessor{}
    r.Get(ctx, req.NamespacedName, processor)

    // Check if being deleted
    if !processor.DeletionTimestamp.IsZero() {
        if controllerutil.ContainsFinalizer(processor, myFinalizer) {
            // Run cleanup: drain payment queue, notify external system
            if err := r.cleanup(ctx, processor); err != nil {
                return reconcile.Result{}, err
            }
            // Remove finalizer so K8s can delete the object
            controllerutil.RemoveFinalizer(processor, myFinalizer)
            r.Update(ctx, processor)
        }
        return reconcile.Result{}, nil
    }

    // Add finalizer on first reconcile
    if !controllerutil.ContainsFinalizer(processor, myFinalizer) {
        controllerutil.AddFinalizer(processor, myFinalizer)
        r.Update(ctx, processor)
    }
    // ... rest of reconciliation
}
```

```text
Without finalizer:
  kubectl delete paymentprocessor main-processor
  → Object deleted immediately → orphaned cloud resources

With finalizer:
  kubectl delete paymentprocessor main-processor
  → DeletionTimestamp set, object NOT deleted
  → Controller runs cleanup (delete cloud resources, drain queues)
  → Controller removes finalizer
  → Object deleted from etcd
```

---

# Topic 5: Webhook Validation for CRDs

```go
// Validation webhook prevents bad CRD specs from being applied
func (r *PaymentProcessor) ValidateCreate() error {
    if r.Spec.Replicas < 1 {
        return fmt.Errorf("replicas must be >= 1")
    }
    if r.Spec.Region == "" {
        return fmt.Errorf("region is required")
    }
    return nil
}

func (r *PaymentProcessor) ValidateUpdate(old runtime.Object) error {
    oldProcessor := old.(*PaymentProcessor)
    if r.Spec.Region != oldProcessor.Spec.Region {
        return fmt.Errorf("region is immutable after creation")
    }
    return nil
}
```

## Status Conditions Pattern

```go
// Update status with structured conditions (like Deployment conditions)
meta.SetStatusCondition(&processor.Status.Conditions, metav1.Condition{
    Type:               "Ready",
    Status:             metav1.ConditionTrue,
    Reason:             "AllReplicasRunning",
    Message:            "All 3 replicas are running",
    LastTransitionTime: metav1.Now(),
})
```

---

# Topic 6: Operator Best Practices

## 1. Production Checklist

```text
Leader election:
  Only one controller instance is active at a time.
  Multiple replicas: only leader reconciles; others are standby.
  
  mgr, _ := ctrl.NewManager(ctrl.GetConfigOrDie(), ctrl.Options{
    LeaderElection:          true,
    LeaderElectionID:        "payment-operator-leader",
  })

Metrics:
  Expose Prometheus metrics for reconciliation count, errors, latency.
  controller-runtime provides default metrics endpoint.

RBAC:
  Operator needs minimal permissions to read/write the resources it manages.
  Use RBAC annotations in Kubebuilder:
  //+kubebuilder:rbac:groups=payments.mycompany.com,resources=paymentprocessors,verbs=get;list;watch;create;update;patch;delete

Idempotency:
  Reconcile can be called multiple times for the same event.
  Always check current state before acting.
  Use server-side apply or dry-run to detect drift.

Error handling:
  Return error → controller re-queues immediately
  Return Result{RequeueAfter: 5*time.Second} → re-queue after delay
  Return nil → no re-queue (until next watch event)
```

## 2. CRD Versioning

```text
Support multiple versions for backward compatibility:
  v1alpha1 → v1beta1 → v1 (stable)

Conversion webhook:
  Converts between versions when stored version differs from requested version.
  Required for multiple versions with different schemas.
```

---

# Topic 7: Revision Notes

- Operator = CRD (custom resource type) + Controller (reconciliation loop) + domain knowledge
- CRD: extends K8s API; stored in etcd; RBAC applies; schema validation via openAPIV3Schema
- Controller: watches resources; reconciles desired (spec) vs actual state; runs in loop
- Finalizers: prevent deletion until cleanup runs; add on first reconcile, remove after cleanup
- Kubebuilder / Operator-SDK: frameworks to generate boilerplate for K8s controllers
- Leader election: one active controller replica at a time; prevents concurrent reconciliation conflicts
- Validation webhook: reject bad CRD specs at admission time
- Status subresource: separate update endpoint for status (prevents spec conflicts)

## Official Source Notes

- CRDs: <https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/>
- Operator pattern: <https://kubernetes.io/docs/concepts/extend-kubernetes/operator/>
- Kubebuilder: <https://book.kubebuilder.io/>
- Operator-SDK: <https://sdk.operatorframework.io/>
- controller-runtime: <https://pkg.go.dev/sigs.k8s.io/controller-runtime>
