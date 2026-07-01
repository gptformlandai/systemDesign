# Runbook: Health Checks, Wait-For-Ready, And Service Config

## Symptoms

- clients hang during startup or rollout
- `DEADLINE_EXCEEDED` rises after a discovery or mesh change
- `UNAVAILABLE` appears during deploy windows
- traffic reaches pods before the gRPC service can serve
- retry or wait behavior changes without application code changes

## Checks

1. Confirm the gRPC health service exists.
2. Compare liveness, readiness, and per-service serving state.
3. Check whether readiness flips before shutdown/drain.
4. Identify methods using `wait_for_ready`.
5. Confirm every wait-for-ready call has a deadline.
6. Inspect service config or xDS retry, hedging, timeout, and load-balancing policy.
7. Compare app deadlines with mesh/gateway route timeouts.
8. Check whether retries or queued calls amplified load during startup.

## Mitigations

- disable or narrow unsafe `wait_for_ready` behavior
- roll back service config/xDS policy change
- fix readiness so traffic only reaches serving instances
- align app deadline and mesh timeout policy
- reduce retry attempts or disable hedging for unsafe methods
- drain not-serving pods before termination

## Prevention

- method-level policy review for wait-for-ready and retries
- health-drain test in deployment pipeline
- canary for service config/xDS changes
- dashboard for health state, channel state, retries, deadlines, and status codes
- runbook ownership between application and platform teams

## Interview Sound Bite

Health checks decide where traffic should go; wait-for-ready decides whether a client waits for connectivity; service config and xDS decide client behavior at scale. If those policies are not governed together, rollout and discovery incidents become hard to explain.