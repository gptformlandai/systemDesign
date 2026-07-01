# 02. Datadog Agent: Install, Config, API Key, Host Tagging, DogStatsD

## Goal

Install and configure the Datadog Agent on any host, understand its configuration file, and send your first custom metric.

---

## Installation

### Linux (Debian/Ubuntu)

```bash
DD_API_KEY=your-api-key-here \
DD_SITE="datadoghq.com" \
bash -c "$(curl -L https://s3.amazonaws.com/dd-agent/scripts/install_script_agent7.sh)"
```

### Docker

```bash
docker run -d --name datadog-agent \
  -e DD_API_KEY=your-api-key-here \
  -e DD_SITE=datadoghq.com \
  -e DD_LOGS_ENABLED=true \
  -e DD_APM_ENABLED=true \
  -e DD_PROCESS_AGENT_ENABLED=true \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /proc/:/host/proc/:ro \
  -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
  gcr.io/datadoghq/agent:7
```

### Kubernetes (Helm)

```bash
helm repo add datadog https://helm.datadoghq.com
helm install datadog-agent datadog/datadog \
  --set datadog.apiKey=your-api-key-here \
  --set datadog.site=datadoghq.com \
  --set datadog.logs.enabled=true \
  --set datadog.apm.portEnabled=true \
  --set datadog.processAgent.enabled=true
```

---

## datadog.yaml: Core Configuration

```yaml
api_key: your-api-key-here
site: datadoghq.com

# Unified Service Tagging - set on agent.
tags:
  - env:production
  - team:platform
  - region:us-east-1

# Enable log collection.
logs_enabled: true

# Enable APM.
apm_config:
  enabled: true
  apm_non_local_traffic: true    # allow traces from other containers

# Enable process monitoring.
process_config:
  process_collection:
    enabled: true

# DogStatsD custom metrics.
dogstatsd_port: 8125
dogstatsd_non_local_traffic: true   # allow metrics from other containers
```

Config file location: `/etc/datadog-agent/datadog.yaml`

---

## Verifying Agent Status

```bash
# Check agent status.
datadog-agent status

# Key sections to check:
# - API key: valid
# - Logs Agent: running
# - APM Agent: running
# - Checks: all green

# Send a health check.
datadog-agent health

# Check agent version.
datadog-agent version
```

---

## Host Tags

Host tags applied to the agent appear on all metrics, logs, and traces from that host.

```yaml
# In datadog.yaml.
tags:
  - env:production
  - service:orders-service
  - version:1.2.3
  - region:us-east-1
  - datacenter:aws-us-east
  - team:backend
```

Tags set via environment variable (Docker/K8s preferred):

```bash
DD_TAGS="env:production service:orders-service version:1.2.3"
```

---

## DogStatsD: Custom Metrics

DogStatsD listens on UDP port 8125. Applications send custom metrics in StatsD format.

### Protocol Format

```text
metric.name:value|type|#tag1:value1,tag2:value2
```

### Metric Types

```bash
# Counter (c): track occurrences.
echo "orders.placed:1|c|#env:prod,service:orders" | nc -u -w1 localhost 8125

# Gauge (g): track current value.
echo "queue.depth:42|g|#env:prod,queue:orders" | nc -u -w1 localhost 8125

# Histogram (h): distribution of values.
echo "request.duration.ms:245|h|#env:prod,service:api" | nc -u -w1 localhost 8125

# Timer (ms): timing (Datadog treats as histogram).
echo "db.query.time:12|ms|#env:prod,db:postgres" | nc -u -w1 localhost 8125

# Distribution (d): global percentiles across agents.
echo "api.response.time:120|d|#env:prod,endpoint:/checkout" | nc -u -w1 localhost 8125

# Set (s): count unique values.
echo "active.users:user1001|s|#env:prod" | nc -u -w1 localhost 8125
```

### From Application Code (Python Example)

```python
from datadog import initialize, statsd

initialize(statsd_host="localhost", statsd_port=8125)

statsd.increment("orders.placed", tags=["env:production", "service:orders"])
statsd.gauge("queue.depth", 42, tags=["env:production"])
statsd.histogram("api.latency.ms", 245, tags=["env:production", "endpoint:/checkout"])
```

---

## Agent Integrations

Built-in integrations automatically collect metrics from common services:

```yaml
# /etc/datadog-agent/conf.d/postgres.d/conf.yaml
init_config:

instances:
  - host: localhost
    port: 5432
    username: datadog
    password: your-password
    dbname: myapp
    tags:
      - env:production
      - service:myapp-db
```

Common integrations: `postgres`, `mysql`, `redis`, `kafka`, `nginx`, `jmx`, `aws`, `kubernetes`.

---

## Interview Sound Bite

The Datadog Agent is a local daemon that collects all signal types and ships them to Datadog's intake endpoint via HTTPS using an API key. It runs a core metrics agent, APM trace collector (port 8126), log tailer, and DogStatsD listener (port 8125). Unified Service Tagging — setting `env`, `service`, and `version` on the agent, container, and tracer — is the prerequisite for cross-signal correlation in the UI.
