# Reading Backend Startup Logs and Troubleshooting - Gold Sheet

> Goal: become able to open backend startup logs and quickly understand where the app is in the Build -> Package -> Runtime -> Serve -> Scale pipeline.

---

## 1. Intuition

Startup logs are the backend's flight recorder.

They tell you:

- what runtime started
- what configuration was loaded
- which dependencies connected
- whether the service bound a port
- whether the app is ready for traffic
- why it crashed before serving

Backend log reading is a senior skill because production failures often look like:

```text
container restarted
  -> app logs are short
  -> health probe failed
  -> service never became ready
```

The fastest engineer is usually the one who can map each log line to a lifecycle stage.

---

## 2. Universal Startup Timeline

```text
1. Process launch
2. Runtime loads
3. Application code imports
4. Configuration and environment loaded
5. Secrets loaded
6. Logging/telemetry initialized
7. Database/cache/message broker connections initialized
8. Migrations or schema checks run
9. HTTP server binds port
10. Health/readiness endpoints become available
11. First request is served
```

Troubleshooting map:

| Stops at | Suspect |
|---|---|
| process launch | command, entrypoint, image, permissions |
| runtime loads | Java/Node/Python version, missing executable |
| app import | missing dependency, wrong module path |
| config load | missing env var, invalid profile |
| dependency connection | DB, Redis, Kafka, network, credentials |
| port bind | port conflict, wrong host |
| readiness | startup too slow, dependency unhealthy, probe path wrong |
| first request | routing, auth, middleware, serialization |

---

## 3. Java and Spring Boot Logs

Typical startup:

```text
Starting BookingApplication using Java 21
No active profile set, falling back to default profile
Tomcat initialized with port 8080
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Started BookingApplication in 6.423 seconds
```

How to read it:

| Log line | Meaning |
|---|---|
| `using Java 21` | JVM version is visible |
| `No active profile set` | profile/env config may be default |
| `Tomcat initialized with port 8080` | embedded server selected port |
| `HikariPool Starting` | DB pool initialization |
| `Start completed` | DB pool connected |
| `Started BookingApplication` | Spring context loaded and server started |

Common failures:

```text
APPLICATION FAILED TO START
Port 8080 was already in use
```

Fix:

- change server port
- stop conflicting process
- check Kubernetes container command

```text
Failed to configure a DataSource: 'url' attribute is not specified
```

Fix:

- provide `SPRING_DATASOURCE_URL`
- check active profile
- check config map/secret injection

```text
FlywayException: Validate failed
```

Fix:

- inspect migration history
- avoid editing already-applied migration files
- create a new migration

Interview insight:

> In Spring Boot, "Started Application" is necessary but readiness should also consider DB, migrations, and actuator readiness state.

---

## 4. Maven Build Logs

Typical Maven output:

```text
[INFO] Scanning for projects...
[INFO] --------------------< com.company:booking-api >--------------------
[INFO] Building booking-api 1.2.0-SNAPSHOT
[INFO] --- maven-resources-plugin:resources ---
[INFO] --- maven-compiler-plugin:compile ---
[INFO] --- maven-surefire-plugin:test ---
[INFO] --- jacoco-maven-plugin:report ---
[INFO] --- maven-jar-plugin:jar ---
[INFO] BUILD SUCCESS
```

How to read it:

| Section | Meaning |
|---|---|
| `Scanning for projects` | Maven is reading POMs |
| `1.2.0-SNAPSHOT` | non-final development version |
| `resources` | copying config/resources |
| `compile` | Java compilation |
| `surefire:test` | unit tests |
| `jacoco:report` | coverage report generation |
| `jar` | artifact packaging |
| `BUILD SUCCESS` | lifecycle completed |

Common Maven failures:

| Error | Meaning | Fix |
|---|---|---|
| `Could not resolve dependencies` | repo/network/version issue | check repository, coordinates, credentials |
| `Compilation failure` | source compile error | inspect first compiler error |
| `There are test failures` | unit test failed | inspect surefire reports |
| `Non-resolvable parent POM` | parent not found | check parent version/repository |
| `401 Unauthorized` | repo credentials missing | fix settings.xml/CI secret |

Snapshot clue:

```text
Downloading ... booking-common/1.2.0-SNAPSHOT/maven-metadata.xml
```

Maven is checking metadata to locate the latest timestamped snapshot.

---

## 5. Gradle Build Logs

Typical Gradle output:

```text
> Task :compileJava
> Task :processResources
> Task :classes
> Task :test
> Task :jacocoTestReport
> Task :bootJar
BUILD SUCCESSFUL in 18s
7 actionable tasks: 5 executed, 2 up-to-date
```

How to read it:

| Gradle status | Meaning |
|---|---|
| `executed` | task ran |
| `UP-TO-DATE` | inputs/outputs unchanged |
| `FROM-CACHE` | task output restored from cache |
| `NO-SOURCE` | task has no matching source |
| `SKIPPED` | task did not run due to condition |
| `FAILED` | task failed |

Common Gradle failures:

| Error | Meaning | Fix |
|---|---|---|
| dependency not found | repository/version issue | check repositories and version |
| task not found | wrong task name or plugin missing | run `gradle tasks` |
| daemon disappeared | memory or JVM issue | inspect daemon logs, tune memory |
| cache miss everywhere | undeclared inputs/outputs | fix task configuration |

Interview insight:

> Gradle logs are task-graph logs. Maven logs are lifecycle-phase logs.

---

## 6. Node.js Startup Logs

Typical backend startup:

```text
> booking-api@1.0.0 start
> node dist/server.js

config loaded: env=production port=3000
database connected
routes registered
server listening on 0.0.0.0:3000
```

How to read it:

| Log line | Meaning |
|---|---|
| package script | `npm start` executed a command |
| `node dist/server.js` | built output is being used |
| config loaded | env vars were read |
| database connected | dependency initialized |
| routes registered | app bootstrapped |
| listening | port bind succeeded |

Common Node failures:

```text
Error: Cannot find module '/app/dist/server.js'
```

Fix:

- build step did not run
- Docker copied wrong directory
- `main`/start script points to wrong file

```text
EADDRINUSE: address already in use :::3000
```

Fix:

- port conflict
- duplicate server start
- wrong process manager configuration

```text
ECONNREFUSED 10.0.0.4:5432
```

Fix:

- database unreachable
- wrong host/port
- service started before DB readiness
- network policy or security group issue

```text
ERR_REQUIRE_ESM
```

Fix:

- CommonJS/ESM mismatch
- check `"type": "module"`
- update import/require usage

---

## 7. npm Install and Build Logs

Typical CI:

```text
npm ci
added 842 packages, and audited 843 packages in 19s
found 0 vulnerabilities

npm run build
> tsc -p tsconfig.json
```

How to read it:

| Log | Meaning |
|---|---|
| `npm ci` | clean install from lockfile |
| `added packages` | dependency installation succeeded |
| `audited` | vulnerability metadata checked |
| `tsc` | TypeScript compilation |
| `vite build` / `next build` | framework build pipeline |

Common failures:

| Error | Meaning | Fix |
|---|---|---|
| `package-lock.json out of sync` | lockfile stale | run install locally and commit lockfile |
| `npm ERR! peer dep` | dependency compatibility conflict | align versions |
| `tsc` errors | type errors | fix first meaningful type error |
| `JavaScript heap out of memory` | build too large | increase memory, split build, inspect bundle |

---

## 8. Python and FastAPI Startup Logs

Typical Uvicorn startup:

```text
INFO:     Started server process [1]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8000
```

How to read it:

| Log line | Meaning |
|---|---|
| `Started server process` | process is alive |
| `Waiting for application startup` | lifespan startup running |
| `Application startup complete` | startup hooks finished |
| `Uvicorn running` | host and port bound |

Common Python failures:

```text
ModuleNotFoundError: No module named 'app'
```

Fix:

- wrong working directory
- wrong import path
- missing package marker
- Docker did not copy source

```text
Error loading ASGI app. Attribute "app" not found
```

Fix:

- `app.main:app` points to a missing variable
- app object has different name

```text
Address already in use
```

Fix:

- duplicate process
- wrong port
- reloader running in production command

---

## 9. Docker and Kubernetes Logs

Docker:

```bash
docker logs <container>
docker inspect <container>
```

Kubernetes:

```bash
kubectl logs deploy/booking-api
kubectl logs pod/booking-api-abc123 --previous
kubectl describe pod booking-api-abc123
kubectl get events --sort-by=.metadata.creationTimestamp
```

What to check:

| Signal | Meaning |
|---|---|
| `ImagePullBackOff` | image cannot be pulled |
| `CrashLoopBackOff` | process repeatedly exits |
| readiness probe failed | app not ready for traffic |
| liveness probe failed | platform restarts container |
| OOMKilled | memory limit exceeded |
| exit code 1 | app error |
| exit code 137 | killed, often memory or termination |

CrashLoop debug order:

```text
1. kubectl logs --previous
2. kubectl describe pod
3. check events
4. check env/config/secret mounts
5. check command and args
6. check readiness/liveness probes
7. check resource limits
```

---

## 10. Structured Logging

Good production log:

```json
{
  "level": "info",
  "service": "booking-api",
  "env": "prod",
  "traceId": "8f3a",
  "message": "order created",
  "orderId": "ord_123",
  "durationMs": 42
}
```

Why structured logs matter:

- searchable by field
- easier alerting
- correlation across services
- works with log aggregation tools

Avoid:

```text
Something went wrong
```

Prefer:

```text
payment authorization failed provider=stripe code=card_declined orderId=ord_123
```

Do not log:

- passwords
- tokens
- raw credit card values
- personal data unless explicitly approved and protected

---

## 11. Troubleshooting Decision Tree

```text
Is the container running?
  no -> image, command, crash, permissions
  yes
    Is the process listening on the expected port?
      no -> app command, port config, startup failure
      yes
        Is readiness passing?
          no -> dependency, probe path, startup timeout
          yes
            Are requests failing?
              yes -> route, auth, DB, downstream, serialization
              no -> inspect latency and saturation
```

Fast first checks:

- exact command used to start app
- runtime version
- active profile/environment
- port
- dependency connection
- readiness endpoint
- recent deploy/change
- first error line, not last stack trace line

---

## 12. Real-World Scenario

Problem:

> A Python FastAPI service deploys successfully but receives no traffic.

Logs:

```text
INFO:     Started server process [1]
INFO:     Waiting for application startup.
```

Kubernetes events:

```text
Readiness probe failed: connection refused
```

Likely diagnosis:

- app is stuck in lifespan startup
- port has not started serving
- readiness cannot connect

Next checks:

- DB connection timeout
- secret manager call
- migration lock
- blocking network call
- startup timeout settings

Fix:

- add timeouts
- log each startup dependency
- make non-critical dependency lazy or degraded
- ensure readiness only passes after critical dependencies load

---

## 13. Interview Questions

### Question

> A service is in CrashLoopBackOff. How do you debug it?

Strong answer:

1. I check previous container logs because the current container may have restarted.
2. I describe the pod to inspect events, probes, exit codes, image pulls, env vars, and mounts.
3. I identify whether the failure is before process start, during app import, during dependency initialization, or after port binding.
4. I check recent deployment changes and configuration differences.
5. I fix the root cause and add better startup logs or health checks if needed.

### Question

> What do startup logs tell you in a backend system?

Strong answer:

> They show the lifecycle stage: runtime launch, app import, configuration, dependency initialization, server binding, and readiness. Reading them lets me distinguish packaging errors, configuration errors, dependency failures, and serving issues quickly.

### Question

> Why is "server listening" not always enough?

Strong answer:

> A process can bind a port before all dependencies are healthy. For production traffic, I care about readiness, critical dependency state, and whether the app can serve a meaningful request.

---

## 14. Revision Notes

- One-line summary: Startup logs show where the backend is in its lifecycle and where failure begins.
- Three keywords: import, dependency, readiness.
- One interview trap: debugging only the last stack trace line instead of the first root error.
- One memory trick: process alive is not the same as service ready.

