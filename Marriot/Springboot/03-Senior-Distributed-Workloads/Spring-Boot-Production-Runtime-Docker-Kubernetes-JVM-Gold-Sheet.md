# Spring Boot Production Runtime Docker Kubernetes JVM Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: connect Spring Boot application design to Docker, Kubernetes, JVM container behavior, deployment safety, and runtime operations.

Read after Observability and Spring Cloud.

---

## 1. Runtime Mental Model

A production Spring Boot service is more than a jar.

```text
Spring Boot app -> JVM -> container image -> pod -> service/ingress -> metrics/logs/traces
```

Production readiness includes:

- container image
- external config/secrets
- health probes
- graceful shutdown
- resource requests/limits
- JVM memory sizing
- logs/metrics/traces
- rollout and rollback strategy

Strong answer:

```text
Spring Boot gives application features, but production reliability depends on how the JVM,
container, Kubernetes, health probes, resources, and deployment strategy are configured.
```

---

## 2. Docker Image Basics

Good image practices:

- small base image
- non-root user
- layered jar or buildpacks
- no secrets baked into image
- reproducible build
- clear startup command
- vulnerability scanning

Spring Boot layered jar:

```text
dependencies
spring-boot-loader
snapshot-dependencies
application
```

Why:

```text
Layering lets Docker reuse dependency layers when only application code changes.
```

---

## 3. Buildpacks

Spring Boot supports building OCI images with buildpacks.

Example:

```text
mvn spring-boot:build-image
```

Benefits:

- sane JVM defaults
- layered image
- dependency analysis
- no custom Dockerfile needed for many apps

Trade-off:

```text
Buildpacks are convenient, but teams still need to understand runtime memory, security, and
image scanning.
```

---

## 4. JVM In Containers

Modern JVMs are container-aware, but sizing still matters.

Important settings:

- heap as percent of container memory
- metaspace
- thread stack memory
- direct buffers
- native memory
- GC choice

Trap:

```text
If container memory limit is 512 MiB and heap is too large, native memory/thread stacks can
still cause OOMKilled.
```

Strong answer:

```text
I size container memory for heap plus non-heap JVM memory, threads, direct buffers, and
native overhead, not only Xmx.
```

---

## 5. CPU Limits And Throttling

CPU throttling can cause p99 spikes.

Symptoms:

- high p99 latency
- CPU usage average looks acceptable
- throttling metrics high
- request queues grow

Mitigation:

- right-size CPU requests/limits
- monitor throttling
- avoid overly tight CPU limits for latency-sensitive services
- load test under realistic concurrency

---

## 6. Kubernetes Probes For Spring Boot

Spring Boot Actuator supports health groups.

Example:

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
```

Probe meaning:

| Probe | Purpose |
|---|---|
| startup | app is still booting |
| readiness | can receive traffic |
| liveness | process should be restarted if broken |

Strong answer:

```text
Readiness controls traffic. Liveness controls restart. I avoid putting temporary dependency
failures into liveness because it can cause restart storms.
```

---

## 7. Graceful Shutdown

Spring Boot graceful shutdown:

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Shutdown flow:

```text
SIGTERM -> readiness false -> stop new requests -> finish in-flight work -> close resources
```

For consumers:

```text
Stop polling, finish or safely abandon in-flight records, commit only successful work, then
close broker connection.
```

---

## 8. Deployment Strategies

| Strategy | Use |
|---|---|
| rolling update | default, simple |
| blue-green | quick switch/rollback |
| canary | gradual exposure with metrics |
| feature flag | decouple deploy from release |

Canary metrics:

- 5xx by version
- p95/p99 by version
- JVM memory/GC by version
- DB pool wait by version
- business success rate
- logs/errors by version

Strong answer:

```text
Canary is useful only if metrics are separated by version. Otherwise a bad version can hide
inside aggregate averages.
```

---

## 9. Config And Secrets

Spring Boot config sources:

- environment variables
- config files
- Kubernetes ConfigMap
- Kubernetes Secret
- Vault/secret manager
- Spring Cloud Config

Rules:

- no secrets in git or image
- validate required config at startup
- audit config changes
- canary risky config
- support rollback

Strong answer:

```text
Configuration changes can break production like code changes. I validate, audit, canary, and
roll back config safely.
```

---

## 10. Hikari And DB Pool Runtime

HikariCP is common default pool.

Monitor:

- active connections
- idle connections
- pending threads
- acquisition time
- timeout count
- max pool size

Trap:

```text
Increasing pool size can make the database worse if the DB is already saturated.
```

Strong answer:

```text
I tune Hikari based on DB capacity, query latency, transaction duration, and request
concurrency. Pool size is not a magic throughput knob.
```

---

## 11. JVM Diagnostics In Production

Useful tools/signals:

- thread dump
- heap dump
- JFR recording
- GC logs
- Micrometer JVM metrics
- Actuator metrics/threaddump/heapdump with secure exposure
- container restart/OOM events

Use cases:

| Symptom | Diagnostic |
|---|---|
| high CPU | thread dump, JFR, profiling |
| memory leak | heap dump, memory trend, GC logs |
| stuck threads | thread dump |
| p99 spikes | traces, GC, throttling, pool wait |
| OOMKilled | container events + memory analysis |

---

## 12. Startup Performance

Startup matters for:

- autoscaling
- canary rollout
- pod replacement
- serverless/native image use cases

Levers:

- reduce classpath bloat
- avoid slow startup network calls
- lazy initialization only when safe
- AOT/native image if constraints justify it
- correct startup probe

Trade-off:

```text
Lazy initialization can improve startup but move failures to first request.
```

---

## 13. Logging In Containers

Best practice:

```text
Log to stdout/stderr as structured logs. Let platform collect logs.
```

Include:

- timestamp
- level
- service name
- version
- trace id
- correlation id
- error code
- safe message

Avoid:

- full tokens
- secrets
- PII/payment data
- huge payloads by default

---

## 14. Runtime Incident Checklist

When Spring Boot service is unhealthy:

1. Check recent deploy/config/secret changes.
2. Check readiness/liveness failures.
3. Check pod restarts and OOMKilled.
4. Check CPU throttling and memory.
5. Check JVM metrics and GC.
6. Check DB pool and downstream latency.
7. Check logs by version.
8. Roll back or shift traffic if customer impact is active.

---

## 15. Strong Closing Answer

```text
For production Spring Boot runtime, I package the app safely, externalize config and secrets,
use Actuator probes correctly, enable graceful shutdown, size JVM/container resources with
heap and non-heap memory in mind, monitor Hikari/GC/throttling, and deploy with canary or
rollback metrics tied to user impact.
```
