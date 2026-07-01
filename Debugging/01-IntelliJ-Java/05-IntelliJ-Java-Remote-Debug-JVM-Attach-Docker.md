# 05. IntelliJ Java: Remote Debug, JVM Attach, Docker, Kubernetes

## Goal

Attach IntelliJ to a running JVM process outside the IDE — on the same machine, inside Docker, or in a Kubernetes pod.

---

## JDWP — How Remote Debug Works

Java Debug Wire Protocol (JDWP) is the agent that lives inside the JVM and accepts debugger connections.

```text
JVM (target process)
  ├── JDWP agent (enabled via -agentlib flag)
  ├── Opens a TCP port (e.g., 5005)
  └── Waits for or connects to a debugger

IntelliJ (debugger)
  ├── TCP connection to JVM JDWP port
  └── Sends debug commands, receives stack/variable data
```

---

## JDWP Startup Flags

### Full JDWP Flag (Java 9+)

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

### Flag Breakdown

| Parameter | Value | Meaning |
|---|---|---|
| transport | dt_socket | Use TCP/IP socket (always use this) |
| server | y | JVM listens for debugger (not connects to) |
| suspend | n | Don't wait for debugger before starting |
| suspend | y | Wait for debugger before executing main() |
| address | *:5005 | Listen on all interfaces on port 5005 |
| address | 127.0.0.1:5005 | Listen on localhost only (safer for dev) |

### Older Java Syntax (Java 8)

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
# Port only (no host) — listens on 0.0.0.0 by default
```

---

## Spring Boot Debug Run

### Gradle

```bash
./gradlew bootRun --debug-jvm
# Starts on port 5005, suspend=y by default (waits for debugger)
```

### Maven

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### IntelliJ Spring Boot Run Configuration

```text
Edit Configurations -> Spring Boot config
VM options: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
-> Or just use "Debug" mode in the run config (IntelliJ adds JDWP automatically)
```

---

## IntelliJ Remote JVM Debug Configuration

```text
Run -> Edit Configurations -> + -> Remote JVM Debug

Name:             DockerOrderService
Host:             localhost
Port:             5005
Command line args for remote JVM:
  (IntelliJ fills this in automatically based on your JDK version)

Debugger mode: Attach to remote JVM
Use module classpath: order-service
```

After connecting, all breakpoints set in your source code are active in the remote process.

---

## Docker Remote Debug

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/order-service.jar .

EXPOSE 8080 5005

ENTRYPOINT ["java", \
  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
  "-jar", "order-service.jar"]
```

### docker-compose.yml

```yaml
version: "3.8"
services:
  order-service:
    build: .
    ports:
      - "8080:8080"
      - "5005:5005"   # <- expose debug port to host
    environment:
      SPRING_PROFILES_ACTIVE: dev
```

### Debug Steps

```bash
# 1. Start container.
docker-compose up order-service

# 2. Verify JDWP is listening.
docker exec -it <container-id> sh -c "ss -tlnp | grep 5005"
# or from host:
nc -z localhost 5005 && echo "debug port open"

# 3. In IntelliJ: connect Remote JVM Debug config to localhost:5005.
```

---

## Kubernetes Remote Debug

### Method 1: Port-Forward To Pod

```bash
# Find the pod.
kubectl get pods -n default -l app=order-service

# Forward debug port.
kubectl port-forward pod/order-service-abc123 5005:5005 -n default

# IntelliJ: connect Remote JVM Debug to localhost:5005.
```

### Method 2: JDWP In K8s Deployment

```yaml
# deployment.yaml - add to container spec.
containers:
  - name: order-service
    image: order-service:latest
    ports:
      - containerPort: 8080
      - containerPort: 5005      # expose debug port
    env:
      - name: JAVA_TOOL_OPTIONS
        value: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

### Security Note For Production

Never expose JDWP on production clusters. JDWP provides full control over the JVM:
- Read any variable
- Change any value
- Call any method
- Load arbitrary code

For production investigation, use `jstack` (thread dumps) and `jmap` (heap dumps) without JDWP.

---

## Attaching To A Local JVM Process

```bash
# List all running JVMs.
jps -l
# Output:
# 12345  com.example.orders.OrderServiceApplication
# 12346  org.gradle.launcher.daemon.bootstrap.GradleDaemon

# In IntelliJ: connect to PID directly.
# Run -> Attach to Process -> select PID 12345
# (No JDWP flag needed if same machine, same user)
```

---

## Troubleshooting Remote Debug

| Problem | Cause | Fix |
|---|---|---|
| Connection refused | JDWP not enabled or wrong port | Verify -agentlib flag in process startup |
| Connection refused in Docker | Debug port not exposed | Add -p 5005:5005 to docker run |
| Breakpoints not hitting | Source mismatch | Ensure IntelliJ source matches running JAR exactly |
| suspend=y and process hangs | Waiting for debugger | Connect IntelliJ or change suspend=n |
| JDWP address rejected (Java 9+) | Need address=*:5005 | Add * to address to listen on all interfaces |

---

## Interview Sound Bite

Remote Java debug uses JDWP: add `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005` to JVM startup, then attach IntelliJ with a Remote JVM Debug configuration pointing to the host and port. For Docker: expose port 5005 with `-p 5005:5005`. For Kubernetes: `kubectl port-forward` tunnels the debug port to localhost without exposing it externally. `suspend=y` pauses the JVM until the debugger connects — useful for debugging startup code. Never enable JDWP in production; use jstack and jmap for non-interactive analysis.
