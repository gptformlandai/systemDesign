# Kubernetes Networking: Services, Ingress, and DNS Gold Sheet

> Track: K8s Interview Track — Phase 1: Foundations
> Goal: Understand how traffic flows inside and outside a Kubernetes cluster, from pod-to-pod communication to external traffic routing.

---

## 0. How To Read This

Beginner focus:
- ClusterIP, NodePort, LoadBalancer Service types
- What Ingress does and why you need it
- CoreDNS and how pods discover services

Intermediate focus:
- How kube-proxy implements services (iptables vs ipvs)
- Ingress controllers: NGINX, ALB, Traefik
- ExternalName and Headless services
- EndpointSlices

Senior / MAANG focus:
- kube-proxy ipvs mode vs iptables performance at scale
- Ingress vs Gateway API
- Traffic routing: session affinity, cross-zone awareness
- Multi-cluster service discovery
- Network Policy integration with service routing

---

# Topic 1: How Pod Networking Works

## 1. Pod Network Model

Kubernetes requires every pod to have a unique IP address and pods must be able to communicate with all other pods without NAT:

```text
Node 1 (10.0.1.10):
  pod-A  IP: 10.244.1.2
  pod-B  IP: 10.244.1.3

Node 2 (10.0.1.11):
  pod-C  IP: 10.244.2.2
  pod-D  IP: 10.244.2.3

pod-A (10.244.1.2) → pod-C (10.244.2.2):
  - Traffic goes through CNI plugin (Flannel, Calico, Cilium)
  - Each node has a virtual network bridge
  - Routing rules installed by CNI
  - No NAT required — pod-A sees pod-C at its real IP
```

CNI (Container Network Interface) plugins implement this:
- Flannel: simple overlay (VXLAN), limited features
- Calico: BGP routing or overlay, supports Network Policies
- Cilium: eBPF-based, high performance, deep observability, L7 Network Policies
- AWS VPC CNI: pods get real VPC IPs (no overlay overhead on EKS)

---

# Topic 2: Services

## 1. Why Services Exist

Pods are ephemeral. Their IP addresses change on restart. A Service provides a stable virtual IP (ClusterIP) that load-balances to matching pods:

```text
payment-service ClusterIP: 10.96.15.42

Pod-1 (v1): 10.244.1.5   all receive traffic via ClusterIP
Pod-2 (v1): 10.244.1.6
Pod-3 (v2): 10.244.2.3

Service selector: app=payment-service
kube-proxy watches Endpoints/EndpointSlices for this selector
Routes traffic to healthy pods (not terminating/failed)
```

## 2. Service Types

### ClusterIP (default)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: payment-service
  namespace: prod
spec:
  type: ClusterIP          # only accessible inside the cluster
  selector:
    app: payment-service
  ports:
    - protocol: TCP
      port: 80             # port exposed by the service
      targetPort: 8080     # port the container listens on
```

Use when: pod-to-pod communication within the cluster.

### NodePort

```yaml
spec:
  type: NodePort
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080    # valid range: 30000-32767
```

```text
External client → <any-node-ip>:30080 → Service → Pod

All nodes listen on nodePort. Traffic reaches service even if
the pod is on a different node.
```

Use when: dev/testing, or when running without a cloud load balancer. Not for production — exposes node IPs.

### LoadBalancer

```yaml
spec:
  type: LoadBalancer
  ports:
    - port: 443
      targetPort: 8443
```

```text
Cloud provider creates external load balancer:
  AWS: Network Load Balancer (NLB) or Classic ELB
  GCP: TCP/UDP Load Balancer
  Azure: Azure Load Balancer

External IP or DNS → Cloud LB → NodePort → Pod

Each LoadBalancer service creates one cloud LB = one public IP.
Expensive at scale (use Ingress instead for many HTTP services).
```

### Headless Service

```yaml
spec:
  clusterIP: None    # no virtual IP assigned
  selector:
    app: postgres
```

```text
DNS returns individual pod IPs instead of a single ClusterIP.
Used by StatefulSets for direct pod addressing.
Also used when the application handles load balancing itself (e.g., Kafka clients).

DNS for headless + StatefulSet:
  postgres-0.postgres-headless.prod.svc.cluster.local → 10.244.1.5
  postgres-1.postgres-headless.prod.svc.cluster.local → 10.244.1.6
```

### ExternalName

```yaml
spec:
  type: ExternalName
  externalName: my-database.rds.amazonaws.com
```

```text
Maps a Kubernetes service name to an external DNS name.
No proxying — just a CNAME.
Use to reference external services (RDS, ElastiCache) using in-cluster DNS.
```

## 3. How kube-proxy Implements Services

kube-proxy runs on every node and maintains network rules:

**iptables mode (default):**
```text
Traffic to ClusterIP:Port → iptables DNAT → random pod IP
  - O(n) rule lookup — performance degrades with many services
  - Random selection (no session affinity by default)
  - Rules updated atomically on endpoint change
```

**ipvs mode (recommended at scale):**
```text
Traffic to ClusterIP:Port → ipvs load balancing → pod IP
  - O(1) lookup using hash tables
  - Multiple algorithms: rr, lc, dh, sh, sed, nq
  - Better performance at 1000+ services
  - Enable: kube-proxy --proxy-mode=ipvs
```

## 4. EndpointSlices

Modern replacement for Endpoints (default since K8s 1.21):

```text
Old: one Endpoints object per Service (can grow huge with many pods)
New: multiple EndpointSlice objects (default 100 pods per slice)

Benefits:
  - Smaller updates (only affected slices updated)
  - Faster propagation to kube-proxy
  - Zone-aware traffic routing (topology hints)
```

Zone-aware routing (TopologyAwareHints):
```yaml
metadata:
  annotations:
    service.kubernetes.io/topology-mode: auto
```
Routes traffic to endpoints in the same availability zone when possible.

## 5. Session Affinity

```yaml
spec:
  sessionAffinity: ClientIP    # route same client IP to same pod
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800    # 3 hours
```

Use for stateful HTTP sessions (carts, auth tokens).

## 6. Service Traps

| Trap | Reality |
|---|---|
| "NodePort is load balanced" | Traffic hits one node IP; if that node is gone, the port is unavailable on that IP |
| "LoadBalancer replaces Ingress" | LoadBalancer = L4; Ingress = L7 (host/path routing, TLS termination) |
| "ClusterIP is accessible from outside" | No — only from within the cluster |
| "Pods are always in Endpoints" | Only Running pods with passing readiness probes are in EndpointSlices |

---

# Topic 3: Ingress

## 1. Intuition

A single LoadBalancer service costs money and only handles one hostname. Ingress provides L7 routing (HTTP/HTTPS) for many services through one load balancer:

```text
Client → ALB/NGINX (Ingress Controller) → Route by host/path → Service → Pod

ingress rule: payment.myapp.com         → payment-service:80
ingress rule: orders.myapp.com          → orders-service:80
ingress rule: myapp.com/api/users       → users-service:80
ingress rule: myapp.com/api/payments    → payment-service:80

All handled by ONE Ingress Controller (one cloud LB).
```

## 2. Ingress Resource

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  namespace: prod
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx           # which Ingress Controller handles this
  tls:
    - hosts:
        - payment.myapp.com
        - orders.myapp.com
      secretName: myapp-tls-cert    # TLS cert stored in K8s Secret
  rules:
    - host: payment.myapp.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: payment-service
                port:
                  number: 80
    - host: orders.myapp.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: orders-service
                port:
                  number: 80
```

## 3. Ingress Controllers

```text
NGINX Ingress Controller:
  - Most popular; self-hosted
  - Rich annotation support (rate limiting, rewrite, auth)
  - Runs as a Deployment + LoadBalancer Service

AWS ALB Ingress Controller (AWS Load Balancer Controller):
  - Creates real ALB per Ingress resource (or shared ALB with IngressGroup)
  - Integrates with ACM for TLS, WAF, Shield, Cognito auth
  - Targets can be pods or instance mode

Traefik:
  - Dynamic configuration via K8s CRDs
  - Built-in dashboard, middleware, rate limiting

Istio Gateway:
  - Part of Istio service mesh
  - Use Gateway + VirtualService instead of Ingress
```

## 4. Gateway API (Next-Gen Ingress)

Kubernetes Gateway API is the modern replacement for Ingress:

```yaml
# Gateway (where traffic enters)
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: prod-gateway
spec:
  gatewayClassName: nginx
  listeners:
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        mode: Terminate
        certificateRefs:
          - name: myapp-tls

---
# HTTPRoute (routing rules)
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: payment-route
spec:
  parentRefs:
    - name: prod-gateway
  hostnames:
    - payment.myapp.com
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /api/payments
      backendRefs:
        - name: payment-service
          port: 80
          weight: 90
        - name: payment-service-canary
          port: 80
          weight: 10    # 10% canary traffic
```

Gateway API advantages over Ingress:
- Role-based: platform team owns Gateway; dev teams own HTTPRoute
- Expressible: native canary, header-based routing, traffic mirroring
- Extensible: no annotation hacks

---

# Topic 3b: cert-manager — Automated TLS Certificate Management

## 1. What cert-manager Does

```text
cert-manager is a Kubernetes controller that:
  - Automatically provisions TLS certificates (Let's Encrypt, Vault, AWS PCA)
  - Renews certs before expiry (30-day buffer by default)
  - Stores certs as Kubernetes Secrets
  - Integrates with Ingress and Gateway API (annotation-based or CRD-based)

Without cert-manager: manual cert renewal → expired certs → outages.
With cert-manager: fully automated lifecycle.
```

## 2. Key CRDs

```text
Issuer      — issues certs for one namespace
ClusterIssuer — issues certs cluster-wide (most common)
Certificate — declares what cert you want
CertificateRequest — internal (created by cert-manager, not manually)
```

## 3. ClusterIssuer (Let's Encrypt)

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ops-team@mycompany.com
    privateKeySecretRef:
      name: letsencrypt-prod-account-key    # stores ACME account key
    solvers:
      - http01:
          ingress:
            ingressClassName: nginx   # use NGINX Ingress for HTTP-01 challenge
      - dns01:                        # for wildcard certs
          route53:
            region: us-east-1
            hostedZoneID: Z1234567890
```

## 4. Ingress Integration (annotation-driven)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: payment-ingress
  annotations:
    # Tell cert-manager to issue a cert for this Ingress
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - payment.myapp.com
      secretName: payment-tls-cert    # cert-manager creates this Secret
  rules:
    - host: payment.myapp.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: payment-service
                port:
                  number: 80

# cert-manager sees the annotation → creates Certificate CRD → issues cert → stores in Secret
```

## 5. Explicit Certificate Resource

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: payment-tls
  namespace: prod
spec:
  secretName: payment-tls-cert       # K8s Secret where cert will be stored
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  commonName: payment.myapp.com
  dnsNames:
    - payment.myapp.com
    - api.payment.myapp.com
  duration: 2160h    # 90 days (Let's Encrypt max)
  renewBefore: 720h  # renew 30 days before expiry
```

## 6. Debugging cert-manager

```bash
# Check certificate status
kubectl get certificate -n prod
kubectl describe certificate payment-tls -n prod
# Look for: Ready=True, not-before, not-after

# Check CertificateRequest
kubectl get certificaterequest -n prod

# Check Orders (ACME challenge status)
kubectl get order -n prod
kubectl describe order payment-tls-xxx -n prod
# Look for: state=valid (succeeded) or state=pending (challenge not yet solved)

# Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager -f

# Force renewal (add annotation)
kubectl annotate certificate payment-tls \
  cert-manager.io/force-renewal=true -n prod
```

## 7. Interview Traps — cert-manager

| Trap | Reality |
|---|---|
| "cert-manager only works with Let's Encrypt" | Supports ACME (LE, ZeroSSL), Vault, AWS PCA, self-signed, CA issuers |
| "HTTP-01 challenge works for wildcard certs" | HTTP-01 cannot issue wildcards. Use DNS-01 for `*.myapp.com` |
| "cert-manager stores the private key in a ConfigMap" | No — stored in a K8s Secret of type `kubernetes.io/tls` |
| "Cert expiry won't happen because cert-manager renews" | cert-manager must have permission (RBAC) and issuer must be reachable at renewal time |

---

# Topic 4: DNS in Kubernetes

## 1. CoreDNS

CoreDNS runs as a Deployment in `kube-system` and handles DNS for all pods:

```text
Every pod has /etc/resolv.conf:
  nameserver 10.96.0.10    (CoreDNS ClusterIP)
  search prod.svc.cluster.local svc.cluster.local cluster.local
  ndots: 5
```

## 2. DNS Record Formats

```text
Service DNS:
  {service}.{namespace}.svc.cluster.local
  payment-service.prod.svc.cluster.local

Pod DNS (if pod hostname configured):
  {pod-ip-dashes}.{namespace}.pod.cluster.local
  10-244-1-5.prod.pod.cluster.local

StatefulSet pod DNS:
  {pod-name}.{headless-service}.{namespace}.svc.cluster.local
  postgres-0.postgres-headless.prod.svc.cluster.local
```

## 3. Search Domain Short Names

Because of the `search` domains in resolv.conf, short names resolve:

```text
payment-service          → payment-service.prod.svc.cluster.local
payment-service.prod     → payment-service.prod.svc.cluster.local
payment-service.prod.svc → payment-service.prod.svc.cluster.local

Cross-namespace:
  From orders namespace:
    payment-service.prod → resolves correctly
    payment-service      → resolves in orders namespace (not found!)
```

## 4. CoreDNS Customization

```text
Add custom DNS entries (e.g., for external RDS):
  ConfigMap: coredns (in kube-system)

  # Forward all .corp.internal queries to internal DNS server
  corp.internal:53 {
    forward . 10.0.0.53
  }
```

## 5. DNS Traps

| Trap | Reality |
|---|---|
| "payment-service resolves from any namespace" | Short name resolves only within same namespace |
| "CoreDNS failure kills running pods" | DNS failure breaks new connections, not existing ones |
| "Pods always see latest Service changes" | DNS TTL is 5 seconds; may hit old pods briefly |
| "All DNS goes to CoreDNS" | Yes, all cluster DNS goes to CoreDNS; external DNS forwarded upstream |

---

# Topic 5: Network Summary

```text
Traffic flow (external to pod):
  Internet → Cloud LB (LoadBalancer Service or ALB Ingress)
           → Node IP:NodePort
           → kube-proxy routes to pod IP (via iptables/ipvs)
           → Container port

Traffic flow (pod to pod, same node):
  pod-A:8080 → veth pair → bridge → veth pair → pod-B:8080

Traffic flow (pod to pod, cross-node):
  pod-A → veth → bridge → CNI overlay/routing → node-B → bridge → veth → pod-B

Traffic flow (pod to service):
  pod → Service ClusterIP:port → kube-proxy iptables/ipvs → pod IP:port
```

## 6. Revision Notes

- Service = stable VIP that load-balances to matching pods
- ClusterIP: cluster-internal only; NodePort: external via node IPs; LoadBalancer: cloud LB; Headless: direct pod DNS
- kube-proxy implements Service routing via iptables (default) or ipvs (better at scale)
- Ingress = L7 routing; one LB for many HTTP/HTTPS services
- Gateway API = modern Ingress replacement; role-based, traffic splitting native
- CoreDNS: `{service}.{namespace}.svc.cluster.local`; short names only resolve in same namespace
- StatefulSet pod DNS: `{pod-name}.{headless-svc}.{namespace}.svc.cluster.local`

## 7. Official Source Notes

- Services: <https://kubernetes.io/docs/concepts/services-networking/service/>
- Ingress: <https://kubernetes.io/docs/concepts/services-networking/ingress/>
- Gateway API: <https://kubernetes.io/docs/concepts/services-networking/gateway/>
- DNS: <https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/>
- CNI: <https://kubernetes.io/docs/concepts/extend-kubernetes/compute-storage-net/network-plugins/>
