# Kafka Kubernetes And Strimzi Operations Gold Sheet

> Track: Kafka Interview Track - Senior / Platform Operations
> Goal: understand when and how Kafka is operated on Kubernetes using Strimzi and Kubernetes-native guardrails.

---

## 1. Why This Sheet Exists

Kafka on Kubernetes is common in platform interviews.

The hard part is not "can Kafka run in a pod?"

The hard part is:

- stable storage
- broker identity
- rolling upgrades
- listener configuration
- topic and user management
- safe node draining
- replica placement
- monitoring
- disaster recovery

Strimzi is a common operator-based way to manage Kafka on Kubernetes.

---

## 2. Mental Model

Kafka needs stable identity and durable storage.

Kubernetes provides:

```text
StatefulSet / operator-managed pods
  -> stable pod identity
  -> persistent volume claims
  -> services/listeners
  -> rolling operations
```

Strimzi adds:

```text
Kafka custom resource
KafkaTopic custom resource
KafkaUser custom resource
Cluster Operator
Topic Operator
User Operator
Cruise Control integration
MirrorMaker / Connect resources
```

---

## 3. Why Strimzi

Strimzi helps with:
- deploying Kafka clusters
- configuring listeners
- managing TLS/certs
- rolling upgrades
- creating topics
- creating users and ACLs
- managing Kafka Connect
- managing MirrorMaker
- monitoring integration
- balancing with Cruise Control

Strong interview sentence:
Running Kafka directly with plain StatefulSets is possible, but operator knowledge reduces operational mistakes during upgrades, scaling, user management, and maintenance.

---

## 4. Core Custom Resources

| Resource | Purpose |
|---|---|
| `Kafka` | defines Kafka cluster |
| `KafkaTopic` | declarative topic config |
| `KafkaUser` | declarative user/auth/ACLs/quotas |
| `KafkaConnect` | Connect cluster |
| `KafkaConnector` | connector config if enabled |
| `KafkaMirrorMaker2` | cross-cluster replication |

GitOps pattern:
Kafka platform config should be version-controlled and reviewed like infrastructure code.

---

## 5. Example Topic Resource

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: orders-created-v1
  labels:
    strimzi.io/cluster: prod-kafka
spec:
  topicName: orders.created.v1
  partitions: 12
  replicas: 3
  config:
    retention.ms: 604800000
    min.insync.replicas: 2
```

Rules:
- topic name standards still matter
- partitions and retention need ownership review
- increasing partitions can affect key ordering assumptions

---

## 6. Example User Resource

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaUser
metadata:
  name: order-service
  labels:
    strimzi.io/cluster: prod-kafka
spec:
  authentication:
    type: tls
  authorization:
    type: simple
    acls:
      - resource:
          type: topic
          name: orders.created.v1
          patternType: literal
        operations:
          - Write
          - Describe
      - resource:
          type: group
          name: order-service-
          patternType: prefix
        operations:
          - Read
```

Security rule:
Do not replace topic/ACL design with tenant fields in payloads.

---

## 7. Storage And Scheduling

Kafka storage questions:
- Which storage class?
- Is volume expansion supported?
- What IOPS/throughput?
- What happens on node loss?
- Are volumes zone-bound?
- How are backups/DR handled?

Scheduling questions:
- Are brokers spread across zones?
- Are controllers isolated if needed?
- Are anti-affinity rules configured?
- Is rack awareness mapped to zones?
- Can pods be drained safely?

Trap:
Persistent volumes keep broker data, but they do not replace Kafka replication or DR.

---

## 8. Rolling Upgrades

Safe upgrade checklist:

```text
1. Read Kafka and Strimzi version compatibility.
2. Check cluster health before upgrade.
3. Confirm ISR healthy and no offline partitions.
4. Pause large reassignments and nonessential admin jobs.
5. Roll one broker/controller at a time through operator.
6. Monitor under-replicated partitions, controller changes, request latency.
7. Validate clients and Connect/Streams apps after upgrade.
```

Do not upgrade during a live replication or disk incident unless the upgrade fixes that incident and platform owner approves.

---

## 9. Node Draining

Kubernetes node drain can hurt Kafka if it evicts too many brokers or replicas at once.

Good drain behavior:
- one broker at a time
- ISR stays healthy
- leader movement controlled
- PodDisruptionBudgets respected
- operator/drain cleaner coordinates rolling movement

Watch:
- under-replicated partitions
- offline partitions
- leader election churn
- produce/fetch latency

---

## 10. Cruise Control And Rebalancing

Cruise Control can help rebalance Kafka workloads.

Use cases:
- broker disk imbalance
- leader imbalance
- adding/removing brokers
- rack/zone rebalancing

Rules:
- throttle movement
- run proposals before execution
- avoid during incidents
- monitor network/disk pressure
- review proposal impact

Interview line:
Rebalancing fixes placement problems but creates load while it runs.

---

## 11. Kafka Connect And MirrorMaker On Kubernetes

Strimzi can manage:
- Connect clusters
- connector configs
- MirrorMaker 2 replication

Operational concerns:
- connector plugin images
- secrets and credentials
- task scaling
- DLQ topics
- source/target ACLs
- offset replication
- worker pod disruption

Do not treat Connect workers as stateless web pods. Their configs, offsets, and target side effects matter.

---

## 12. Monitoring

Kubernetes layer:
- pod restarts
- CPU/memory
- PVC usage
- node pressure
- pending pods
- PDB violations

Kafka layer:
- under-replicated partitions
- offline partitions
- controller health
- request latency
- broker disk
- consumer lag
- auth failures

Operator layer:
- reconciliation errors
- failed rolling update
- CR status conditions
- certificate renewal

---

## 13. Failure Modes

| Failure | Symptom | Mitigation |
|---|---|---|
| PVC full | broker fails or stalls | expand volume, reduce retention, add capacity |
| bad drain | under-replicated/offline partitions | coordinated drain, PDB, operator support |
| wrong listener config | clients cannot connect | validate internal/external listener setup |
| rolling upgrade stuck | pods fail readiness | inspect CR status and broker logs |
| topic CR drift | unexpected topic config | GitOps review and operator status |
| user cert expiry | auth failures | certificate monitoring and rotation |

---

## 14. Strong Interview Answer

```text
For Kafka on Kubernetes I prefer an operator such as Strimzi because Kafka needs
stable identity, durable storage, careful rolling operations, topic/user
management, listener configuration, and safe node maintenance. I would manage
Kafka, KafkaTopic, and KafkaUser resources through GitOps, spread brokers across
zones, monitor PVCs and Kafka health together, and treat upgrades, drains, and
Cruise Control rebalancing as controlled platform operations.
```

---

## 15. Revision Notes

- One-line summary: Kafka on Kubernetes is storage, identity, operators, and safe rolling operations.
- Three keywords: Strimzi, PVC, rolling.
- One interview trap: Treating Kafka brokers like stateless deployments.
- Memory trick: Broker pod identity plus durable volume plus operator reconciliation.

---

## 16. Official Source Notes

- Strimzi overview: https://strimzi.io/docs/operators/latest/overview.html
- Apache Kafka KRaft docs: https://kafka.apache.org/43/operations/kraft/
