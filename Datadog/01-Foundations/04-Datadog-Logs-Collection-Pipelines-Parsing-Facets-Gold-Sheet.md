# 04. Datadog Logs: Collection, Pipelines, Parsing, Facets, Archives

## Goal

Collect logs from any source, parse them into structured facets, and manage log volume with exclusion filters and archives.

---

## Log Collection Sources

### File Tailing

```yaml
# /etc/datadog-agent/conf.d/java.d/conf.yaml
logs:
  - type: file
    path: /var/log/myapp/*.log
    service: orders-service
    source: java
    tags:
      - env:production
      - version:1.2.3
```

### Docker Container Logs

```yaml
# In datadog.yaml.
logs_config:
  container_collect_all: true
```

Or per-container via Docker labels:

```bash
docker run \
  -l "com.datadoghq.ad.logs=[{\"source\":\"java\",\"service\":\"orders-service\"}]" \
  my-app:1.2.3
```

### Kubernetes Pod Logs

Use annotations on the pod spec:

```yaml
# In pod template spec.
annotations:
  ad.datadoghq.com/orders-service.logs: |
    [{"source":"java","service":"orders-service"}]
```

### Lambda / CloudWatch Logs

Use the Datadog Forwarder Lambda function to ship CloudWatch log groups to Datadog automatically.

---

## Log Levels And Standard Fields

Datadog looks for standard fields. Use JSON logging to get them automatically:

```json
{
  "timestamp": "2024-01-15T10:23:45.123Z",
  "level": "ERROR",
  "message": "Order processing failed",
  "service": "orders-service",
  "env": "production",
  "version": "1.2.3",
  "dd.trace_id": "8423012345678901234",
  "dd.span_id": "1234567890123456789",
  "order_id": "ORD-99001",
  "user_id": "USR-12345",
  "error.type": "java.lang.NullPointerException",
  "error.message": "Cannot invoke method on null reference",
  "error.stack": "at com.example.OrderService.process(OrderService.java:45)..."
}
```

---

## Log Processing Pipelines

Pipelines parse raw log text into structured attributes. Pipeline → Processors applied in order.

### Grok Parser

Parse logs that are not JSON:

```text
# Pattern for common Java log format.
# Input:
# 2024-01-15 10:23:45.123 ERROR [orders-service] OrderService - Order failed for ORD-99001

# Grok pattern:
%{date("yyyy-MM-dd HH:mm:ss.SSS"):date} %{word:level} \[%{notSpace:service}\] %{notSpace:logger} - %{data:message}
```

Built-in Grok patterns:

```text
%{word}         - one word (no whitespace)
%{notSpace}     - characters until whitespace
%{data}         - any characters
%{integer}      - integer value
%{number}       - decimal number
%{date()}       - date with specified format
%{uuid}         - UUID pattern
%{ipv4}         - IPv4 address
%{url}          - URL
```

### Other Processors

| Processor | Purpose |
|---|---|
| Attribute Remapper | rename/copy an attribute to a standard name |
| Status Remapper | map a field value to Datadog log status (INFO/WARN/ERROR) |
| Date Remapper | set the official log timestamp from a field |
| URL Parser | parse URL into host, path, query parameters |
| User-Agent Parser | parse user agent string into browser/OS |
| Lookup Processor | enrich with static lookup table (e.g., service code -> service name) |
| Arithmetic Processor | compute derived fields (e.g., latency_ms from start/end epoch) |

---

## Log Facets

Facets are indexed attributes you can filter and aggregate on:

- String facets: filter logs by exact value (`service:orders-service`)
- Measure facets: numeric aggregation (`avg(duration_ms)`, `sum(bytes_sent)`)

Create a facet from any attribute in the Log Explorer by clicking the attribute and selecting "Create facet".

Facets appear in the left sidebar for filtering. Only faceted attributes support aggregations.

---

## Log Search Query Syntax

```text
# Free text search.
"order processing failed"

# Exact attribute match.
service:orders-service level:ERROR

# Range (measure facets).
duration_ms:[100 TO 500]

# Wildcard.
service:order*

# Boolean.
service:orders-service AND level:ERROR
service:orders-service OR service:payments-service
NOT level:DEBUG

# Tag search.
env:production AND @error.type:NullPointerException

# Time range (from URL or query).
@timestamp:[2024-01-15T10:00:00 TO 2024-01-15T11:00:00]
```

---

## Log Exclusion Filters

Reduce log ingestion cost by excluding verbose or low-value logs:

```text
# In Datadog UI: Logs -> Configuration -> Indexes -> Edit -> Exclusion Filters

# Exclude DEBUG and TRACE logs in production (usually 60-70% of log volume).
Filter: level:(DEBUG OR TRACE)
Exclusion rate: 100%

# Exclude health check endpoint logs.
Filter: @http.url_details.path:/health
Exclusion rate: 100%

# Sample INFO logs (keep 10%, discard 90%).
Filter: level:INFO
Exclusion rate: 90%
```

---

## Log Archives

Rehydratable archives: send all logs (including excluded ones) to S3/GCS/Azure Blob for long-term storage and rehydration.

```text
Logs -> Configuration -> Archives -> Add an Archive
  - Name: prod-logs-archive
  - Destination: S3 bucket (requires IAM role)
  - Prefix: datadog/production/
  - Filter: env:production
```

Rehydration: Load archived logs back into Datadog for analysis of a past time window.

---

## Interview Sound Bite

Datadog log collection supports file tailing, Docker/Kubernetes container stdout, Lambda/CloudWatch forwarding, and HTTP ingestion. Log pipelines use processors (Grok parsers, attribute remappers, status remappers) to extract structured attributes. Facets make attributes filterable and aggregatable. Exclusion filters reduce ingestion cost by dropping debug/trace logs or sampling high-volume low-value events. Archives to S3/GCS preserve all logs for compliance and rehydration.
