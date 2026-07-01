# Runbook: Agent Not Reporting / Traces Not Appearing

## Symptom

- No metrics from a host in Infrastructure → Hosts
- No traces in APM → Services for an instrumented service
- Log stream empty for a service that should be logging

---

## Diagnosis Checklist

### Step 1: Is The Agent Running?

```bash
# Bare metal.
sudo systemctl status datadog-agent

# Docker.
docker ps | grep datadog-agent
docker exec datadog-agent datadog-agent status | head -30

# Kubernetes.
kubectl get pods -n datadog -l app=datadog-agent
kubectl exec -n datadog datadog-agent-XXXXX -- agent status | head -30
```

### Step 2: Is The API Key Valid?

```bash
docker exec datadog-agent datadog-agent status | grep -i "api key"
# Should show: API Keys status: API Key is valid
# If invalid: check DD_API_KEY env var
```

### Step 3: Can The Agent Reach Datadog?

```bash
docker exec datadog-agent datadog-agent status | grep -i "forwarder"
# Should show: Transactions flushed: [N]
# If 0 transactions: network connectivity issue

# Test connectivity.
docker exec datadog-agent curl -s https://intake.datadoghq.com/api/v1/validate \
  -H "DD-API-KEY: $DD_API_KEY" | head -50
```

### Step 4: Is APM Enabled?

```bash
docker exec datadog-agent datadog-agent status | grep -A5 "APM Agent"
# Should show: Status: Running  Port: 8126

# If APM not running, check:
# DD_APM_ENABLED=true (env var)
# apm_config.enabled: true (datadog.yaml)
```

### Step 5: Can The Application Reach The Agent?

```bash
# From the application container:
docker exec your-app-container curl -s http://datadog-agent:8126 || echo "Cannot reach APM port"

# Common issues:
# - DD_AGENT_HOST set to 'localhost' but app is in separate container
# - Agent not allowing non-local traffic: DD_APM_NON_LOCAL_TRAFFIC=true
```

### Step 6: Is The Tracer Library Initialized?

**Java:**

```bash
# Check JVM args to verify javaagent is present.
ps aux | grep javaagent
# Should show: -javaagent:/path/to/dd-java-agent.jar

# Check agent sent startup log.
docker logs your-java-app | grep -i "datadog\|dd-trace\|javaagent"
```

**Node.js:**

```bash
# Verify dd-trace is first require().
docker logs your-node-app | grep -i "datadog\|dd-trace"
# dd-trace prints startup log when initialized.
```

**Python:**

```bash
docker logs your-python-app | grep -i "ddtrace\|datadog"
# ddtrace prints startup info when active.
```

---

## Common Fixes

| Problem | Fix |
|---|---|
| API key invalid | Double-check DD_API_KEY value; no leading/trailing spaces |
| APM not enabled | Set DD_APM_ENABLED=true |
| Non-local traffic blocked | Set DD_APM_NON_LOCAL_TRAFFIC=true |
| Wrong agent host | Set DD_AGENT_HOST=datadog-agent (container name) |
| dd-trace initialized late | Move require('dd-trace').init() to first line |
| No logs from Java | Check DD_LOGS_INJECTION=true and JSON logging config |
| Sampling rate 0 | Set DD_TRACE_SAMPLE_RATE=1.0 for dev/test |
| Agent unreachable | Check Docker network; app and agent must be on same Docker network |
