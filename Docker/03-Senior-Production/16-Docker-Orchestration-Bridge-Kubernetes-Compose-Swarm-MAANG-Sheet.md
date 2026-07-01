# Docker Orchestration Bridge: Compose, Kubernetes, Swarm - MAANG Sheet

> Track File #16 of 30 - Group 03: Senior Production
> For: system design and platform interviews | Level: senior | Mode: orchestration boundaries

## 1. Core Idea

Docker runs containers. Orchestrators manage container fleets.

```text
single container -> Compose app -> orchestrated service -> Kubernetes workload
```

Docker knowledge is the foundation for Kubernetes, but Kubernetes adds scheduling, desired state, service discovery, rollout, autoscaling, and cluster operations.

---

## 2. Tool Boundaries

| Tool | Best Fit |
|---|---|
| Docker CLI | single-container build/run/debug |
| Docker Compose | local multi-container development and small demos |
| Docker Swarm | Docker-native clustering, less common now |
| Kubernetes | production orchestration at scale |
| managed container services | simpler deployment when full Kubernetes is unnecessary |

---

## 3. Concept Mapping

| Docker/Compose | Kubernetes Rough Equivalent |
|---|---|
| image | image |
| container | container in Pod |
| Compose service | Deployment/StatefulSet plus Service |
| named volume | PersistentVolumeClaim |
| network/service DNS | Kubernetes Service DNS |
| env vars/secrets | ConfigMap/Secret |
| healthcheck | liveness/readiness/startup probes |

---

## 4. Production Decision

Use plain Docker/Compose when:

- local development
- simple single-host deployment
- small internal tooling
- demos and learning

Use orchestration when:

- multi-host scheduling
- rolling deploys and rollbacks
- service discovery
- autoscaling
- self-healing
- secrets/config management at scale
- network policy and cluster operations

---

## 5. Failure Modes

- treating Compose as full production orchestration without HA plan
- assuming Docker health check equals Kubernetes readiness behavior
- ignoring image pull policy and tag mutability
- missing persistent storage and backup plan
- no resource requests/limits in orchestrator

---

## 6. Interview Summary

```text
Docker packages and runs containers, while orchestrators manage fleets. Compose is excellent for local multi-service workflows, but production at scale usually needs scheduling, service discovery, rollouts, health management, secrets/config, storage, and autoscaling from an orchestrator such as Kubernetes or a managed container platform.
```

---

## 7. Revision Notes

- One-line summary: Docker is container runtime workflow; orchestration manages fleets and desired state.
- Three keywords: Compose, Kubernetes, rollout.
- One trap: using Compose in production without addressing HA, rollout, secrets, storage, and monitoring.