# 22. Scenario: Load Balancing And Service Discovery Incident

## Incident

During a rolling deployment, some clients see `UNAVAILABLE` and others send almost all traffic to one backend pod.

---

## First Hypotheses

- DNS/resolver returned stale or incomplete endpoints.
- Client is using `pick_first` and one long-lived HTTP/2 connection.
- Readiness removed endpoints too late or too early.
- Connection draining is missing during pod termination.
- Service mesh route or cluster config is stale.
- A proxy is not preserving HTTP/2 behavior.

---

## Evidence Path

| Evidence | Why It Matters |
|---|---|
| client channel state | shows connectivity and subchannel health |
| resolver output | confirms addresses known to client |
| EndpointSlice | shows Kubernetes ready endpoints |
| backend request distribution | proves imbalance scope |
| pod lifecycle events | confirms deploy/drain timing |
| Envoy cluster health | shows proxy view of endpoints |
| status by client version | isolates bad LB policy or resolver config |

---

## Mitigations

- switch to appropriate round-robin or xDS policy where supported
- use health checking to avoid unhealthy endpoints
- fix readiness probes
- add graceful shutdown and connection draining
- align mesh/proxy endpoint discovery
- restart or refresh clients with stale resolver state if necessary

---

## Kubernetes Deployment Guardrails

- readiness only true after gRPC server can serve
- preStop/drain period long enough for streams/calls
- terminationGracePeriodSeconds sized for method latency
- rolling update maxUnavailable/maxSurge reviewed
- metrics show traffic by pod/version/zone

---

## Interview Sound Bite

gRPC load balancing must account for long-lived HTTP/2 connections. I check resolver output, subchannel policy, health checks, EndpointSlices, proxy cluster health, connection draining, and backend distribution to distinguish discovery failure from LB policy or rollout behavior.