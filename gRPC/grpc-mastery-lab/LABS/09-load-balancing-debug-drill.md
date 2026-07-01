# Lab 09: Load Balancing Debug Drill

## Incident

After a rollout, 80% of traffic goes to one backend pod and some clients see `UNAVAILABLE`.

## Evidence To Fill

| Evidence | Result |
|---|---|
| client resolver output | |
| channel/subchannel state | |
| Kubernetes EndpointSlice | |
| backend request distribution | |
| readiness probe events | |
| Envoy/mesh cluster health | |
| deployment timeline | |

## Done When

You can distinguish DNS, resolver, LB policy, readiness, and connection-draining causes.