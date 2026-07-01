# Datadog Mastery Lab

This lab contains working examples, setup scripts, hands-on labs, portfolio projects, cheatsheets, interview preparation materials, and production runbooks for the Datadog Mastery Track.

---

## Lab Structure

```text
datadog-mastery-lab/
  README.md                     (this file)
  LEARNING_PATH.md              (week-by-week learning guide)
  
  EXAMPLES/
    java-spring-boot/           (Spring Boot with dd-java-agent)
    nodejs-express/             (Express.js with dd-trace)
    python-fastapi/             (FastAPI with ddtrace)
    opentelemetry/              (OTel SDK examples for all languages)
  
  SCRIPTS/
    agent-status-check.sh       (verify agent health and data flow)
    metric-explorer.sh          (query and validate custom metrics)
    trace-sampler-config.sh     (configure sampling rules)
    log-pipeline-test.sh        (test log parsing pipelines)
    slo-burn-calculator.sh      (calculate burn rate from CLI)
    incident-evidence-template.sh (generate incident evidence snapshot)
  
  LABS/
    lab-01-agent-setup.md       (install agent and send first metric)
    lab-02-java-apm.md          (instrument Java app with dd-java-agent)
    lab-03-nodejs-apm.md        (instrument Node.js app with dd-trace)
    lab-04-python-apm.md        (instrument Python app with ddtrace)
    lab-05-otel-java.md         (OTel Java SDK with OTLP to Datadog)
    lab-06-log-correlation.md   (inject trace IDs into Java/Python/Node.js logs)
    lab-07-dashboard.md         (build full-stack APM + RUM dashboard)
    lab-08-monitors-slos.md     (create monitors, SLOs, burn rate alerts)
    lab-09-kubernetes.md        (deploy agent on Kubernetes with Helm)
    lab-10-incident-simulation.md (simulate production incidents)
  
  PROJECTS/
    project-01-java-full-stack.md
    project-02-nodejs-python-microservices.md
    project-03-otel-java-demo.md
    project-04-kubernetes-monitoring.md
    project-05-slo-dashboard.md
  
  CHEATSHEETS/
    agent-commands-cheatsheet.md
    metric-query-syntax-cheatsheet.md
    log-search-syntax-cheatsheet.md
    apm-trace-search-cheatsheet.md
    kubernetes-metrics-cheatsheet.md
    slo-math-cheatsheet.md
  
  INTERVIEW_PREP/
    common-datadog-interview-questions.md
    system-design-observability-patterns.md
    whiteboard-scenarios.md
    vocabulary-definitions.md
    tell-me-about-a-time-observability.md
  
  RUNBOOKS/
    runbook-high-latency.md
    runbook-error-spike.md
    runbook-oom-kubernetes.md
    runbook-slow-db-queries.md
    runbook-alert-fatigue-review.md
    runbook-slo-breach-response.md
    runbook-broken-traces.md
    runbook-agent-not-reporting.md
```

---

## Quick Start

### Prerequisites

- Docker installed
- A Datadog account (free trial available)
- Your Datadog API key and App key

### Start The Lab Environment

```bash
# Set your API key (do NOT commit this to git).
export DD_API_KEY=your-api-key-here

# Start Datadog Agent.
docker run -d --name datadog-agent \
  -e DD_API_KEY=$DD_API_KEY \
  -e DD_SITE=datadoghq.com \
  -e DD_APM_ENABLED=true \
  -e DD_APM_NON_LOCAL_TRAFFIC=true \
  -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC=true \
  -e DD_LOGS_ENABLED=true \
  -p 8125:8125/udp \
  -p 8126:8126/tcp \
  gcr.io/datadoghq/agent:7

# Verify agent is healthy.
docker exec datadog-agent datadog-agent status | grep -E "Agent|Forwarder|API"
```

### Verify The Lab Is Working

```bash
# Send a test metric.
echo "datadog.lab.test:1|c|#env:dev,service:lab" | nc -u -w1 localhost 8125

# Send a test log.
curl -X POST "https://http-intake.logs.datadoghq.com/api/v2/logs" \
  -H "Content-Type: application/json" \
  -H "DD-API-KEY: $DD_API_KEY" \
  -d '[{"ddsource":"lab","ddtags":"env:dev,service:lab","message":"Datadog lab test log"}]'
```

---

## Getting Help

Each lab file contains step-by-step instructions, expected outcomes, and troubleshooting notes.

If traces are not appearing, check [SCRIPTS/agent-status-check.sh](SCRIPTS/agent-status-check.sh).

If metrics are not appearing, check [CHEATSHEETS/metric-query-syntax-cheatsheet.md](CHEATSHEETS/metric-query-syntax-cheatsheet.md).

For incident scenarios, use [RUNBOOKS/](RUNBOOKS/) as reference for each scenario type.
