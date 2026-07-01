# X03. Cross-IDE Remote Attach: Docker, SSH, Ports, Running Processes

> Bridge module for attaching a local IDE to a process that is already running.

---

## 1. Core Idea

Remote debugging is a three-part contract:

```text
target process exposes a debug protocol
network path reaches that debug port
IDE maps local source code to remote runtime code
```

If any one part is wrong, breakpoints will not bind or the debugger will not connect.

---

## 2. Protocol Map

| Language | Runtime Protocol | Common Port | IDE |
|---|---|---:|---|
| Java | JDWP | 5005 | IntelliJ |
| Node.js | V8 Inspector / Chrome DevTools Protocol | 9229 | VS Code |
| Python | debugpy / pydevd | 5678 | VS Code / PyCharm |

---

## 3. Java Remote Attach

Start JVM with JDWP:

```bash
java \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar app.jar
```

Use `suspend=y` when you need to debug startup:

```bash
java \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 \
  -jar app.jar
```

IntelliJ attach checklist:

```text
Run -> Edit Configurations -> Remote JVM Debug
Host: localhost or remote host
Port: 5005
Module classpath: project module
Source code matches deployed artifact
```

Common failures:

- port not exposed
- app listening only inside container
- wrong code version
- firewall or tunnel issue
- `suspend=y` used accidentally and app appears stuck

---

## 4. Node.js Remote Attach

Start Node with inspector:

```bash
node --inspect=0.0.0.0:9229 server.js
```

Break at startup:

```bash
node --inspect-brk=0.0.0.0:9229 server.js
```

VS Code attach pattern:

```json
{
  "type": "node",
  "request": "attach",
  "name": "Attach Node",
  "address": "localhost",
  "port": 9229,
  "localRoot": "${workspaceFolder}",
  "remoteRoot": "/app",
  "skipFiles": ["<node_internals>/**"]
}
```

Common failures:

- app started without `--inspect`
- inspector bound to `127.0.0.1` inside container
- wrong `localRoot` / `remoteRoot`
- source maps missing for TypeScript
- process restarted by nodemon and debugger detached

---

## 5. Python Remote Attach

Start app with debugpy:

```bash
python -m debugpy --listen 0.0.0.0:5678 app.py
```

Wait for debugger before running:

```bash
python -m debugpy --listen 0.0.0.0:5678 --wait-for-client app.py
```

VS Code attach pattern:

```json
{
  "name": "Attach Python",
  "type": "debugpy",
  "request": "attach",
  "connect": {
    "host": "localhost",
    "port": 5678
  },
  "pathMappings": [
    {
      "localRoot": "${workspaceFolder}",
      "remoteRoot": "/app"
    }
  ]
}
```

PyCharm attach pattern:

```text
Run -> Edit Configurations -> Python Debug Server
Set IDE host/port
Install/use pydevd-pycharm in remote environment
Add path mappings between local project and remote /app
```

Common failures:

- wrong Python interpreter in container
- reload process not attached
- missing path mapping
- debug port not forwarded
- remote source differs from local source

---

## 6. Docker Port And Path Checklist

Docker Compose examples:

```yaml
services:
  java-api:
    ports:
      - "5005:5005"
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

  node-api:
    command: node --inspect=0.0.0.0:9229 server.js
    ports:
      - "9229:9229"

  python-api:
    command: python -m debugpy --listen 0.0.0.0:5678 app.py
    ports:
      - "5678:5678"
```

Checklist:

- container exposes debug port
- host maps debug port
- debugger connects to host port
- runtime binds to `0.0.0.0` inside container
- local source path maps to container source path
- container actually runs the code you are editing

---

## 7. SSH Tunnel Pattern

When the process is on a remote VM:

```bash
ssh -L 5005:localhost:5005 user@remote-host
ssh -L 9229:localhost:9229 user@remote-host
ssh -L 5678:localhost:5678 user@remote-host
```

Then attach IDE to:

```text
Host: localhost
Port: forwarded local port
```

Security rule:

```text
Do not expose debug ports publicly. Treat debug ports like privileged control channels.
```

---

## 8. Remote Attach Failure Flow

```text
1. Is the target process running?
2. Is the debug protocol enabled?
3. Is the debug port listening?
4. Is the port reachable from local machine?
5. Is the IDE using attach, not launch?
6. Are local and remote paths mapped?
7. Does deployed code match local source?
8. Are breakpoints bound?
```

Evidence commands:

```bash
ps aux
lsof -i :5005
lsof -i :9229
lsof -i :5678
docker ps
docker port CONTAINER
docker logs CONTAINER --tail 100
```

---

## 9. Strong Debugging Answer

```text
For remote debugging, I verify the process exposes the correct debug protocol, the port is reachable through Docker or SSH, and the IDE maps local files to remote paths. I avoid public debug ports, use attach mode for running processes, and confirm the source version matches the deployed artifact before trusting breakpoints.
```

---

## 10. Revision Notes

- One-line summary: Remote attach requires protocol, network path, and source mapping.
- Three keywords: protocol, port, path.
- One trap: attaching successfully but debugging the wrong source version.
- Memory trick: `P-P-P` = Protocol, Port, Path.
