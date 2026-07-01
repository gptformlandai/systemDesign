# Agent Commands Cheatsheet

## Bare Metal / systemd

```bash
# Status.
sudo systemctl status datadog-agent
datadog-agent status

# Restart.
sudo systemctl restart datadog-agent

# Config check.
datadog-agent configcheck

# Check agent version.
datadog-agent version

# Run a specific integration check.
datadog-agent check postgres

# Diagnose.
datadog-agent diagnose

# Send a flare to Datadog support.
datadog-agent flare
```

## Docker

```bash
# Status check.
docker exec datadog-agent datadog-agent status

# Config check.
docker exec datadog-agent datadog-agent configcheck

# Stream agent logs.
docker logs -f datadog-agent

# Exec into agent.
docker exec -it datadog-agent bash

# Check APM agent.
docker exec datadog-agent datadog-agent status | grep -A10 "APM"

# Check logs agent.
docker exec datadog-agent datadog-agent status | grep -A10 "Logs Agent"
```

## Kubernetes

```bash
# List agent pods.
kubectl get pods -n datadog -l app=datadog-agent

# Agent status.
kubectl exec -n datadog datadog-agent-XXXXX -- agent status

# Stream agent logs.
kubectl logs -n datadog datadog-agent-XXXXX -f

# Cluster agent status.
kubectl exec -n datadog datadog-cluster-agent-XXXXX -- agent status

# Check autodiscovery.
kubectl exec -n datadog datadog-agent-XXXXX -- agent status | grep -A5 "Autodiscovery"
```

## Test Commands

```bash
# Send test metric via DogStatsD.
echo "test.metric:1|c|#env:dev,service:test" | nc -u -w1 localhost 8125

# Send test log via HTTP API.
curl -X POST "https://http-intake.logs.datadoghq.com/api/v2/logs" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: $DD_API_KEY" \
  -d '[{"ddsource":"test","ddtags":"env:dev","message":"Test log from cheatsheet"}]'

# Verify API key.
curl "https://api.datadoghq.com/api/v1/validate" \
  -H "DD-API-KEY: $DD_API_KEY"
```

## Key Config File Locations

```text
Config:         /etc/datadog-agent/datadog.yaml
Integration:    /etc/datadog-agent/conf.d/<integration>.d/conf.yaml
Logs:           /var/log/datadog/agent.log
Temp files:     /tmp/datadog-agent/
```
