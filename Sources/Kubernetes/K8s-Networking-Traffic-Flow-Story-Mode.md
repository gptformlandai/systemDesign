# Kubernetes Networking and Traffic Flow Through Story Mode

> You have a React frontend and a Spring Boot backend. On your laptop, they talk through localhost and life is simple. In Kubernetes, the app still does the same business work, but the traffic flow becomes far more structured. This guide explains Services, Ingress, Egress, DNS, Network Policies, and how all of them relate to a Spring Boot application in production.

---

# Table of Contents

1. [How Networking Works on Your Laptop](#1-how-networking-works-on-your-laptop)
2. [What Changes in Kubernetes](#2-what-changes-in-kubernetes)
3. [The Big Mental Model: Ingress, East-West, Egress](#3-the-big-mental-model-ingress-east-west-egress)
4. [The Foundation: Pod Networking](#4-the-foundation-pod-networking)
5. [Services - Stable Identity for Moving Pods](#5-services---stable-identity-for-moving-pods)
6. [The Four Service Types and When to Use Them](#6-the-four-service-types-and-when-to-use-them)
7. [How kube-proxy and Endpoints Make Services Work](#7-how-kube-proxy-and-endpoints-make-services-work)
8. [DNS and CoreDNS - How Spring Boot Finds Other Services](#8-dns-and-coredns---how-spring-boot-finds-other-services)
9. [Ingress - How External Traffic Enters the App](#9-ingress---how-external-traffic-enters-the-app)
10. [Egress - How Your Spring Boot App Calls the Outside World](#10-egress---how-your-spring-boot-app-calls-the-outside-world)
11. [Network Policies - Who Can Talk to Whom](#11-network-policies---who-can-talk-to-whom)
12. [The Relationship Between Services, Ingress, and Egress](#12-the-relationship-between-services-ingress-and-egress)
13. [Spring Boot App Traffic Stories](#13-spring-boot-app-traffic-stories)
14. [Spring Boot Configuration Examples](#14-spring-boot-configuration-examples)
15. [Common Mistakes and Troubleshooting](#15-common-mistakes-and-troubleshooting)
16. [Interview-Ready Answers](#16-interview-ready-answers)
17. [Quick Revision Sheet](#17-quick-revision-sheet)

---

# 1. How Networking Works on Your Laptop

On your laptop, your app usually looks like this:

```text
Browser
  ↓
React app on localhost:3000
  ↓
Spring Boot API on localhost:8080
  ↓
PostgreSQL on localhost:5432
```

Maybe your React app calls:

```text
http://localhost:8080/api/orders
```

And your Spring Boot app calls:

```text
jdbc:postgresql://localhost:5432/appdb
```

That works because:

- everything is on one machine
- IP addressing is trivial
- service discovery does not exist because you already know every host and port
- incoming and outgoing traffic all look the same from the app's point of view

Production Kubernetes is different.

---

# 2. What Changes in Kubernetes

In Kubernetes:

- your backend runs in Pods
- Pods can die and come back
- Pod IPs change
- multiple replicas exist
- traffic enters from outside the cluster
- traffic moves between services inside the cluster
- traffic leaves the cluster to call external systems

So networking now has three different questions:

```text
1. How does external traffic enter the cluster?
2. How do services find each other inside the cluster?
3. How does a Pod call systems outside the cluster?
```

Those map to:

```text
External entry     → Ingress
Internal routing   → Services + DNS
External outbound  → Egress
```

---

# 3. The Big Mental Model: Ingress, East-West, Egress

This is the most important networking picture to remember.

```text
North-South traffic = traffic entering or leaving the cluster boundary
East-West traffic   = traffic between services inside the cluster
```

For your Spring Boot app:

```text
Internet user → Ingress → Service → Spring Boot Pod      = Ingress path
Spring Boot Pod → user-service / payment-service         = East-West path
Spring Boot Pod → Stripe / Twilio / external DB / SaaS   = Egress path
```

Another way to visualize it:

```text
                INGRESS
Internet  ------------------>  Ingress Controller  --> Service --> Pod

                EAST-WEST
Pod A     ------------------>  Service B          --> Pod B

                EGRESS
Pod A     ------------------>  External API / Internet / Managed DB
```

Critical clarification:

```text
Ingress is a Kubernetes resource.
Service is a Kubernetes resource.
Egress is NOT a single Kubernetes resource type.
Egress is a traffic direction and is usually controlled using Network Policies,
cloud networking, NAT, firewalls, or service mesh gateways.
```

That distinction matters in interviews.

---

# 4. The Foundation: Pod Networking

Before Services and Ingress make sense, you need the base model.

## 4.1 The Three Core Rules

Kubernetes networking assumes:

```text
1. Every Pod gets its own IP address.
2. Every Pod can reach every other Pod by IP by default.
3. No NAT is needed for Pod-to-Pod traffic inside the cluster.
```

Real-life analogy:

```text
Every Pod is like a person with their own direct phone number.
They do not need to share one office phone.
```

## 4.2 CNI - The Actual Network Engine

Kubernetes itself does not implement Pod networking directly.
That is done by the **CNI plugin**.

Common CNIs:

- Calico
- Cilium
- Flannel
- Weave

Why this matters:

- CNI gives Pods IPs
- CNI connects Pods across nodes
- some CNIs enforce Network Policies

Important interview detail:

```text
Network Policies only work if the CNI supports them.
```

---

# 5. Services - Stable Identity for Moving Pods

## 5.1 The Story

Your Spring Boot Deployment has 3 Pods.

```text
order-service-abc123   → 10.244.1.5
order-service-def456   → 10.244.2.8
order-service-ghi789   → 10.244.3.3
```

Then one Pod dies and is recreated:

```text
order-service-new111   → 10.244.4.2
```

Problem:

```text
How will the frontend or another service keep track of these changing Pod IPs?
```

Answer: **Service**.

## 5.2 Real-Life Analogy

```text
Pods are hotel guests. They can change rooms.
Service is the hotel reception number.

You do not try to memorize every guest's room number.
You call reception and let it route you correctly.
```

## 5.3 What a Service Really Gives You

A Service provides:

- a stable virtual IP
- a stable DNS name
- load balancing across matching Pods

For example:

```text
order-service.default.svc.cluster.local
```

or, within the same namespace, just:

```text
http://order-service
```

## 5.4 Spring Boot View of a Service

Your Spring Boot app does not care which Pod it is calling.
It should call the Service name.

Example:

```properties
user.service.base-url=http://user-service:8080
```

From Java code:

```java
String url = userServiceBaseUrl + "/api/users/" + userId;
```

That is the correct Kubernetes pattern.

---

# 6. The Four Service Types and When to Use Them

## 6.1 ClusterIP

This is the default and the most common.

```text
Used for internal communication only.
Frontend Pod → backend Service
Backend Pod  → database Service
Backend Pod  → Redis Service
```

For most Spring Boot microservice calls, this is the normal choice.

Example:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

## 6.2 NodePort

```text
Exposes the Service on a port on every node.
Useful for quick testing.
Usually not preferred for production internet entry.
```

Think of it as:

```text
any-node-ip:nodePort → Service → Pod
```

## 6.3 LoadBalancer

```text
Creates a cloud load balancer in EKS/GKE/AKS.
Used when you want external traffic to reach one Service directly.
```

This is good for simple cases, but if you have many HTTP services, Ingress is usually more efficient.

## 6.4 Headless Service

```text
No ClusterIP.
DNS returns Pod IPs directly.
Mainly used with StatefulSets.
```

This matters for databases like Kafka, PostgreSQL clusters, or systems where each Pod has a stable identity.

## 6.5 The Interview Summary

```text
ClusterIP   = internal service-to-service traffic
NodePort    = simple external access via node port
LoadBalancer= direct cloud LB for one service
Headless    = direct Pod discovery, mainly for stateful apps
```

---

# 7. How kube-proxy and Endpoints Make Services Work

## 7.1 What Many People Miss

A Service is not a process.
It is an abstraction.

The actual routing happens because Kubernetes maintains endpoint lists and installs network rules on nodes.

## 7.2 The Flow

```text
Service selector matches Pods
      ↓
Kubernetes builds endpoint list for that Service
      ↓
kube-proxy programs iptables or IPVS rules
      ↓
traffic to the Service virtual IP gets forwarded to one of the Pods
```

So when Spring Boot calls:

```text
http://payment-service
```

it does not magically call one Pod.
It hits the Service VIP, and kube-proxy forwards it to a healthy backend Pod.

## 7.3 Readiness and Endpoints

This is critical:

```text
If a Pod fails readiness probe, it is removed from Service endpoints.
The Pod may still be running, but it receives no traffic.
```

This is why readiness is fundamentally a networking concept, not just a health-check concept.

---

# 8. DNS and CoreDNS - How Spring Boot Finds Other Services

## 8.1 The Story

Your Spring Boot service needs to call:

- `user-service`
- `inventory-service`
- `redis-service`

How does it resolve those names?

Answer: **CoreDNS**.

## 8.2 Real-Life Analogy

```text
CoreDNS is the cluster phone directory.
Instead of remembering IP addresses, services ask the directory for names.
```

## 8.3 Common DNS Patterns

Within same namespace:

```text
http://order-service
```

Cross namespace:

```text
http://order-service.payments
```

Full DNS form:

```text
order-service.payments.svc.cluster.local
```

## 8.4 Why This Matters for Spring Boot

Your config should point to Kubernetes service names, not Pod IPs.

Examples:

```properties
spring.datasource.url=jdbc:postgresql://postgres-service:5432/appdb
redis.host=redis-service
inventory.base-url=http://inventory-service:8080
```

That is the clean production-friendly setup.

## 8.5 Common DNS Failure Pattern

If service resolution fails, check:

- Service name correct?
- same namespace or cross-namespace?
- CoreDNS healthy?
- typo in config?

---

# 9. Ingress - How External Traffic Enters the App

## 9.1 The Story

You now want users on the internet to reach the app.

You have:

- frontend Service
- order Service
- user Service

You do not want one cloud load balancer per service.

So you introduce **Ingress**.

## 9.2 Real-Life Analogy

```text
Ingress is the receptionist at the office entrance.

If a visitor asks for /api/orders, send them to the backend.
If a visitor asks for /, send them to the frontend.
```

## 9.3 What Ingress Actually Does

Ingress is an HTTP routing rule.
It routes based on:

- host
- path
- sometimes TLS termination and other L7 features depending on the controller

Important:

```text
Ingress does not send traffic directly to Pods.
Ingress routes to Services.
Services route to Pods.
```

That relationship is core.

## 9.4 You Also Need an Ingress Controller

Ingress resource alone is just configuration.
You need an engine to implement it.

Common choices:

- NGINX Ingress Controller
- Traefik
- AWS Load Balancer Controller

In cloud environments, the Ingress Controller itself is often exposed through a Service of type `LoadBalancer`.

That means the practical chain is often:

```text
Internet
  ↓
Cloud Load Balancer
  ↓
Ingress Controller
  ↓
Service
  ↓
Pod
```

## 9.5 Ingress Example for React + Spring Boot

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
spec:
  rules:
    - host: app.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: frontend-service
                port:
                  number: 80
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: backend-service
                port:
                  number: 80
```

Here the frontend and backend are both reachable under one hostname.

## 9.6 Why Ingress Matters for Spring Boot

Without Ingress, your browser may call backend separately via another public endpoint.
With Ingress, you can often keep one domain and route neatly.

That helps with:

- simpler DNS
- simpler TLS
- simpler browser routing
- better production architecture

---

# 10. Egress - How Your Spring Boot App Calls the Outside World

## 10.1 The Story

Your Spring Boot app needs to call:

- Stripe payment API
- Twilio SMS API
- external OAuth provider
- managed database outside the cluster
- S3 or other cloud APIs

That outbound traffic is **Egress**.

## 10.2 The Important Clarification

Unlike Ingress, Kubernetes does not have a built-in resource called `Egress` for normal app routing.

Egress means:

```text
traffic leaving a Pod toward destinations outside its allowed internal scope
```

It is controlled through combinations of:

- Network Policies with egress rules
- cloud NAT gateways or firewalls
- proxies
- service mesh egress gateways
- corporate network controls

## 10.3 Real-Life Analogy

```text
Ingress is who can enter your office building.
Egress is where employees are allowed to go outside the building.
```

## 10.4 Typical Spring Boot Egress Examples

```text
backend Pod → api.stripe.com:443
backend Pod → sms-provider.example.com:443
backend Pod → external-postgres.company.net:5432
```

## 10.5 Egress and Security

If you do nothing, many clusters allow broad outbound access by default.

In mature production systems, teams often restrict it:

- allow only required destinations
- deny unnecessary outbound internet access
- force traffic through proxy or egress gateway

This is where Network Policies matter.

---

# 11. Network Policies - Who Can Talk to Whom

## 11.1 The Story

Your React frontend should call the backend.
But some random Pod should not call the backend.

Your backend should call Stripe.
But it should not freely talk to every Pod and every external endpoint.

That is the job of **NetworkPolicy**.

## 11.2 Ingress Rules in NetworkPolicy

These control who may send traffic **into** selected Pods.

Example:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-allow-frontend
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: frontend
      ports:
        - protocol: TCP
          port: 8080
```

Meaning:

```text
frontend Pods may call backend Pods on 8080
others may not
```

## 11.3 Egress Rules in NetworkPolicy

These control where selected Pods may send traffic **outward**.

Example:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-egress-policy
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
    - Egress
  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: data
      ports:
        - protocol: TCP
          port: 5432
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
      ports:
        - protocol: TCP
          port: 443
```

Meaning:

```text
backend may talk to:
  - DB in the data namespace on 5432
  - external HTTPS destinations on 443
```

## 11.4 Important Interview Clarification

```text
Ingress resource and NetworkPolicy ingress are not the same thing.

Ingress resource:
  external HTTP routing into Services

NetworkPolicy ingress:
  firewall rule controlling which Pods can send traffic to selected Pods
```

That difference is extremely important.

---

# 12. The Relationship Between Services, Ingress, and Egress

This is the exact relationship you asked for.

## 12.1 The Clean Model

```text
Ingress handles incoming HTTP traffic from outside the cluster.
Service handles stable internal routing to Pods.
Egress handles outbound traffic from Pods to the outside world.
```

## 12.2 They Are Not Peers in the Same Layer

They solve different parts of the path:

```text
Internet request path:
  Client → Ingress → Service → Pod

Internal request path:
  Pod A → Service B → Pod B

Outbound request path:
  Pod A → external destination
```

## 12.3 The Most Important Relationship Statement

```text
Ingress usually depends on Services.
Services do not depend on Ingress.
Egress is separate from both and describes outbound traffic behavior from Pods.
```

## 12.4 Another Useful Mental Model

```text
Ingress = front door
Service = internal reception desk
Egress = exit gate
```

---

# 13. Spring Boot App Traffic Stories

## 13.1 Story 1: Browser Reaches the Spring Boot API

You open:

```text
https://app.example.com/api/orders
```

Traffic flow:

```text
Browser
  ↓
DNS resolves app.example.com
  ↓
Cloud Load Balancer / Ingress Controller entry
  ↓
Ingress rule matches /api
  ↓
backend-service
  ↓
one healthy backend Pod
  ↓
Spring Boot handles request
```

Why Service is needed here:

- Ingress routes to Service, not directly to Pods
- Service hides Pod churn
- readiness ensures only healthy Pods receive traffic

## 13.2 Story 2: Spring Boot Calls Another Internal Service

Order service needs user profile details.

Traffic flow:

```text
order-service Pod
  ↓
HTTP call to http://user-service:8080/api/users/123
  ↓
CoreDNS resolves user-service
  ↓
user-service ClusterIP
  ↓
kube-proxy forwards to one user-service Pod
  ↓
response comes back
```

This is classic east-west traffic.

## 13.3 Story 3: Spring Boot Calls Stripe

Order service processes payment.

Traffic flow:

```text
order-service Pod
  ↓
HTTPS call to api.stripe.com
  ↓
egress path from cluster/network
  ↓
Stripe API
  ↓
response comes back
```

This is not handled by a Kubernetes Service.
It is outbound egress.

## 13.4 Story 4: Frontend to Backend in One Domain

You want:

```text
https://app.example.com/      → frontend
https://app.example.com/api   → backend
```

Ingress makes that clean.

This is often the simplest browser-friendly architecture.

---

# 14. Spring Boot Configuration Examples

## 14.1 Internal Service-to-Service URLs

```properties
user.service.base-url=http://user-service:8080
inventory.service.base-url=http://inventory-service:8080
spring.datasource.url=jdbc:postgresql://postgres-service:5432/appdb
```

## 14.2 Cross-Namespace Example

```properties
payment.service.base-url=http://payment-service.payments:8080
```

## 14.3 External Egress Example

```properties
stripe.base-url=https://api.stripe.com
sms.base-url=https://api.twilio.com
```

## 14.4 Spring Boot Code Example

```java
@Service
public class PaymentClient {

    @Value("${stripe.base-url}")
    private String stripeBaseUrl;

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String charge(Object request) {
        return restTemplate.postForObject(stripeBaseUrl + "/v1/charges", request, String.class);
    }
}
```

## 14.5 CORS Reminder for Browser Traffic

If frontend and backend are served from different origins, browser CORS rules apply.

If both are behind the same Ingress host, you can often reduce CORS complexity significantly.

For example:

```text
app.example.com       → frontend
app.example.com/api   → backend
```

This is cleaner than:

```text
frontend.example.com
api.example.com
```

though both are valid architectures.

---

# 15. Common Mistakes and Troubleshooting

## 15.1 "I used Pod IPs in config"

```text
Wrong: Pod IPs are ephemeral.
Correct: use Service DNS names.
```

## 15.2 "My Service exists but traffic is not reaching Pods"

Check:

```text
1. Does Service selector match Pod labels?
2. Are Pods ready?
3. Does kubectl get endpoints <service> show Pod IPs?
```

## 15.3 "Ingress exists but nothing works"

Check:

- Is an Ingress Controller installed?
- Is DNS pointing to the controller or load balancer?
- Are backend Service names and ports correct?
- Is TLS configured correctly if using HTTPS?

## 15.4 "Egress to external API is failing"

Check:

- DNS resolution working?
- outbound firewall/NAT rules okay?
- NetworkPolicy egress blocking it?
- proxy required in enterprise environment?

## 15.5 "Internal service call fails"

Check:

```text
1. Service name correct?
2. Namespace correct?
3. Target container listening on expected port?
4. Readiness passing?
5. NetworkPolicy blocking traffic?
```

## 15.6 Useful Commands

```bash
kubectl get svc
kubectl get endpoints
kubectl get ingress
kubectl get networkpolicy
kubectl describe svc backend-service
kubectl describe ingress app-ingress
kubectl exec -it <pod> -- curl http://user-service:8080/actuator/health
kubectl exec -it <pod> -- nslookup user-service
```

---

# 16. Interview-Ready Answers

## 16.1 "What is a Service in Kubernetes?"

```text
A Service is a stable network abstraction in front of a dynamic set of Pods. It provides a fixed virtual IP and DNS name and load balances traffic across healthy Pods selected by labels.
```

## 16.2 "How is Ingress different from Service?"

```text
Service gives internal stable access to Pods. Ingress handles external HTTP entry into the cluster and routes requests to Services based on host and path. Ingress typically sits in front of Services, not Pods directly.
```

## 16.3 "What is Egress in Kubernetes?"

```text
Egress is outbound traffic from Pods to destinations outside their current scope, such as external APIs or the internet. Unlike Ingress, Egress is not usually represented by a single built-in routing resource. It is controlled using Network Policies, cloud networking, proxies, or service mesh gateways.
```

## 16.4 "How does Spring Boot call another service in Kubernetes?"

```text
Spring Boot should call the target Service name, not Pod IPs. CoreDNS resolves the Service DNS name, kube-proxy forwards traffic to one of the healthy Pods behind that Service, and readiness probes ensure only ready Pods receive traffic.
```

## 16.5 "What is the difference between Ingress resource and NetworkPolicy ingress?"

```text
Ingress resource is for external HTTP routing into Services. NetworkPolicy ingress is a Pod-level firewall rule that controls which sources may send traffic into selected Pods. They solve completely different problems.
```

## 16.6 "Why does readiness matter for networking?"

```text
Because readiness determines whether a Pod is included in a Service's endpoint list. If a Pod is not ready, it is removed from traffic routing even if the container is still running.
```

---

# 17. Quick Revision Sheet

## One-Line Mapping

```text
Pod            = one running app instance with its own IP
Service        = stable name and load balancing for Pods
ClusterIP      = internal-only Service
NodePort       = Service exposed on every node port
LoadBalancer   = cloud LB in front of one Service
Headless       = direct Pod discovery, no ClusterIP
CoreDNS        = cluster DNS resolver
Ingress        = external HTTP routing to Services
Egress         = outbound traffic from Pods to outside destinations
NetworkPolicy  = Pod-level ingress/egress firewall rules
kube-proxy     = programs node rules so Services forward traffic correctly
```

## The Relationship in One Picture

```text
External user → Ingress → Service → Pod
Pod A → Service B → Pod B
Pod A → external API = Egress
```

## Gold Standard Answer

```text
In Kubernetes, I think about traffic in three directions. External client traffic enters through Ingress, which routes HTTP requests to Services. Services provide stable discovery and load balancing in front of dynamic Pods. Internal service-to-service communication happens through ClusterIP Services and CoreDNS. Outbound traffic from Pods to third-party APIs or internet destinations is Egress, which is controlled separately using Network Policies and infrastructure-level networking controls. For Spring Boot applications, the correct pattern is to configure internal dependencies using Service DNS names and treat readiness as part of networking because only ready Pods should receive traffic.
```