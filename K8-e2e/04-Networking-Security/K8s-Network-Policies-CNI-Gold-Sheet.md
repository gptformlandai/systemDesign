# Kubernetes Network Policies and CNI Gold Sheet

> Track: K8s Interview Track — Phase 4: Networking and Security
> Goal: Implement zero-trust networking inside Kubernetes — control which pods can talk to which, from basic deny-all to microsegmentation.

---

## 0. How To Read This

Beginner focus:
- What Network Policies are and why they matter
- Default allow-all behavior (no policies = everything can talk)
- Basic ingress/egress rules

Intermediate focus:
- Default deny patterns (namespace isolation)
- Namespace and pod selectors in policy rules
- ipBlock rules for external traffic
- CNI requirements for Network Policies

Senior / MAANG focus:
- Cilium L7 Network Policies (HTTP method/path-based)
- Policy audit mode vs enforce mode
- Network Policy at scale (eBPF vs iptables)
- Multi-tenant cluster network segmentation
- Network Policy debugging and observability

---

# Topic 1: Default Behavior and Why Policies Matter

## 1. Default: All Pods Can Talk To All Pods

```text
By default, Kubernetes has NO network isolation:
  pod-A in namespace payments can reach:
    pod-B in namespace payments
    pod-C in namespace orders
    pod-D in kube-system
    anything on the Internet (if node has internet access)

This is a security risk in multi-team clusters.
Network Policies restrict communication to only what is declared.
```

## 2. CNI Requirement

**Network Policies are enforced by the CNI plugin.**
If your CNI doesn't support Network Policies, the API accepts the policy object but nothing is enforced:

| CNI Plugin | Network Policy Support |
|---|---|
| Flannel | ❌ No (use Calico on top) |
| Calico | ✅ Full L3/L4 support |
| Cilium | ✅ Full L3/L4 + L7 (HTTP, gRPC, Kafka) |
| AWS VPC CNI | ✅ With Network Policy controller add-on |
| Weave Net | ✅ Full support |
| Antrea | ✅ Full support |

On EKS: enable the VPC CNI network policy controller or use Calico/Cilium as overlay.

---

# Topic 2: Network Policy Structure

## 1. How a NetworkPolicy Works

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-netpol
  namespace: prod                # policies are namespaced
spec:
  podSelector:                   # which pods this policy applies to
    matchLabels:
      app: payment-service
  policyTypes:
    - Ingress                    # control incoming traffic
    - Egress                     # control outgoing traffic
  ingress:
    - from:                      # allowed sources (OR between list items)
        - podSelector:           # pods in same namespace with this label
            matchLabels:
              app: api-gateway
        - namespaceSelector:     # pods in namespaces with this label
            matchLabels:
              team: frontend
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:                        # allowed destinations
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432
    - to:                        # allow DNS
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
```

## 2. Selector Semantics (AND vs OR)

```text
WITHIN a single `- from:` or `- to:` list item:
  podSelector + namespaceSelector are ANDed:
  "pods in namespace matching NS selector AND pod matching pod selector"

BETWEEN list items in the `from:` array:
  Items are ORed:
  "source 1 OR source 2 OR source 3"

Example — AND:
  - from:
    - namespaceSelector:        # these two selectors are inside ONE list item = AND
        matchLabels:
          env: prod
      podSelector:              # pod must be in prod namespace AND have app=api-gateway
        matchLabels:
          app: api-gateway

Example — OR:
  - from:
    - namespaceSelector:        # list item 1
        matchLabels:
          env: prod
    - podSelector:              # list item 2 (separate dash = OR)
        matchLabels:
          app: monitoring
```

---

# Topic 3: Default Deny Patterns

## 1. Deny All Ingress (Namespace Isolation)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-ingress
  namespace: prod
spec:
  podSelector: {}     # applies to ALL pods in namespace
  policyTypes:
    - Ingress
  # No ingress rules = deny all ingress
```

After applying: nothing can reach any pod in `prod` until specific allow rules are added.

## 2. Deny All Egress

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-egress
  namespace: prod
spec:
  podSelector: {}
  policyTypes:
    - Egress
  # No egress rules = deny all egress
```

## 3. Deny All Ingress and Egress (Maximum Isolation)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: prod
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

## 4. Allow All Within Namespace

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-same-namespace
  namespace: prod
spec:
  podSelector: {}
  ingress:
    - from:
        - podSelector: {}    # any pod in same namespace
  egress:
    - to:
        - podSelector: {}    # any pod in same namespace
```

---

# Topic 4: Common Production Network Policies

## 1. Allow Only Frontend → Backend

```yaml
# Applied in backend namespace
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend-to-backend
  namespace: backend
spec:
  podSelector:
    matchLabels:
      tier: backend
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: frontend
          podSelector:
            matchLabels:
              tier: frontend
      ports:
        - protocol: TCP
          port: 8080
  policyTypes:
    - Ingress
```

## 2. Allow Backend → Database with DNS

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-egress
  namespace: prod
spec:
  podSelector:
    matchLabels:
      app: payment-service
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432
    - to:
        - podSelector:
            matchLabels:
              app: redis
      ports:
        - protocol: TCP
          port: 6379
    - to:                         # DNS (always needed if denying egress)
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
  policyTypes:
    - Egress
```

## 3. Allow Prometheus to Scrape All Pods

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-prometheus-scrape
  namespace: prod
spec:
  podSelector: {}    # all pods
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: monitoring
          podSelector:
            matchLabels:
              app: prometheus
      ports:
        - protocol: TCP
          port: 9090
  policyTypes:
    - Ingress
```

## 4. External Traffic via ipBlock

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-external-api
  namespace: prod
spec:
  podSelector:
    matchLabels:
      app: payment-service
  egress:
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0        # allow all external traffic
            except:
              - 10.0.0.0/8         # but not internal VPC
              - 172.16.0.0/12
              - 192.168.0.0/16
      ports:
        - protocol: TCP
          port: 443
  policyTypes:
    - Egress
```

---

# Topic 5: Cilium L7 Network Policies (Advanced)

Cilium supports HTTP-level policies (beyond L3/L4):

```yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: payment-l7-policy
  namespace: prod
spec:
  endpointSelector:
    matchLabels:
      app: payment-service
  ingress:
    - fromEndpoints:
        - matchLabels:
            app: api-gateway
      toPorts:
        - ports:
            - port: "8080"
              protocol: TCP
          rules:
            http:
              - method: POST        # only allow POST requests
                path: /api/payments
              - method: GET
                path: /api/payments/[0-9]+
```

Cilium L7 policies can also enforce:
- gRPC method names
- Kafka topic access
- DNS FQDN allowlists

---

# Topic 6: Network Policy Best Practices

## 1. Zero-Trust Namespace Template

Apply in every namespace:

```bash
# Step 1: deny all
kubectl apply -f default-deny-all.yaml

# Step 2: allow necessary communications
kubectl apply -f allow-dns.yaml           # DNS for all pods
kubectl apply -f allow-health-probes.yaml  # kubelet health checks
kubectl apply -f app-specific-rules.yaml   # application traffic
kubectl apply -f allow-prometheus.yaml     # monitoring scraping
```

## 2. Common Mistakes

| Mistake | Fix |
|---|---|
| Deny egress without DNS allow | Pods can't resolve DNS; everything breaks |
| AND vs OR in selectors (confused) | Test with `kubectl get netpol -o yaml` and dry-run |
| Forgetting kubelet health probe access | Liveness/readiness probes fail; pods restart |
| Using Flannel (no Network Policy enforcement) | Switch to Calico/Cilium |
| Applying to wrong namespace | Network Policies are namespaced; check `metadata.namespace` |

## 3. Kubelet Health Probe Exception

When denying all ingress, kubelet health probes still need to reach pods:

```text
Liveness and readiness probes are initiated by the kubelet (node) to the pod.
In most CNI implementations, kubelet probe traffic bypasses Network Policies
because it comes from the node's IP (not a pod).

BUT: with Calico strict mode or Cilium, you may need to explicitly allow:
  - Source: node CIDR
  - Port: containerPort used for probe
```

---

# Topic 7: Debugging Network Policies

```bash
# Check if policies exist in namespace
kubectl get networkpolicies -n prod

# Describe a policy to see rules
kubectl describe networkpolicy payment-service-netpol -n prod

# Test connectivity between pods
kubectl exec -n prod pod/payment-service-abc -- \
  wget -qO- --timeout=2 http://postgres.prod:5432 || echo "BLOCKED"

# Cilium: connectivity check
cilium connectivity test

# Calico: check policy hit counts
calicoctl get networkpolicy -o wide

# Temporary debug: add allow-all policy to isolate issue
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: debug-allow-all
  namespace: prod
spec:
  podSelector: {}
  ingress:
  - {}
  egress:
  - {}
  policyTypes:
  - Ingress
  - Egress
EOF
# Test, then delete the debug policy
kubectl delete networkpolicy debug-allow-all -n prod
```

---

# Topic 8: Revision Notes

- Network Policies are enforced by CNI (Calico, Cilium, VPC CNI); Flannel doesn't support them
- Default: no policies = all traffic allowed; any policy on a pod = default deny for that type
- `podSelector: {}` = all pods in namespace
- List items in `from`/`to` = ORed; selectors within one item = ANDed
- `policyTypes: Ingress` + empty `ingress:` = deny all ingress
- Always allow DNS egress (UDP/TCP 53 to kube-system) when denying egress
- ipBlock: for external IP ranges; `except` to exclude subnets
- Cilium L7: policy at HTTP/gRPC/Kafka level; beyond standard K8s NetworkPolicy
- Debugging: test with `kubectl exec` + wget/curl; use Cilium/Calico CLI tools

## Official Source Notes

- Network Policies: <https://kubernetes.io/docs/concepts/services-networking/network-policies/>
- Calico: <https://docs.projectcalico.org/security/kubernetes-network-policy>
- Cilium: <https://docs.cilium.io/en/stable/security/policy/>
