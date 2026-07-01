# Runbook: High CPU, Memory, Or OOM

## Symptoms

- latency spikes
- container restarts
- `OOMKilled=true`
- CPU throttling
- host resource pressure

## Evidence Commands

```bash
docker stats --no-stream
docker inspect CONTAINER --format '{{.State.OOMKilled}} {{.HostConfig.Memory}} {{.HostConfig.NanoCpus}}'
docker logs CONTAINER --tail 200
docker events --since 30m
```

## Check

- memory limit vs actual working set
- CPU quota and throttling symptoms
- app memory leak or traffic spike
- host resource pressure
- restart policy hiding repeated failures
- recent deployment or config change

## Mitigate

- rollback if tied to release
- scale healthy replicas
- tune limit only with evidence
- reduce traffic or disable expensive feature
- capture heap/profile evidence if available

## Prevent

- set resource limits intentionally
- load test container limits
- alert on restart count and OOMKilled
- keep runtime metrics and logs accessible