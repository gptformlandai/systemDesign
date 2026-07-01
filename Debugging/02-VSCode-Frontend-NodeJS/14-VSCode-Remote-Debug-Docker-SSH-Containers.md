# 14. VS Code: Remote Debug — Docker, SSH, Dev Containers

## Goal

Attach VS Code to processes running inside Docker containers, on remote machines via SSH, and in dev containers — for Node.js, Python, and general inspection.

---

## Remote Debug Architecture

```text
Local machine (VS Code)
  |
  |  Debug protocol (CDP for Node, DAP for Python via debugpy)
  |
Remote host / Container
  -> Running process with debug port open
```

VS Code handles the protocol translation and source file mapping.

---

## Node.js Debug In Docker

### Dockerfile Setup

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .
EXPOSE 3000 9229
CMD ["node", "--inspect=0.0.0.0:9229", "src/server.js"]
```

### docker-compose.yml

```yaml
version: "3.8"
services:
  api:
    build: .
    ports:
      - "3000:3000"
      - "9229:9229"   # <- debug port
    volumes:
      - ./src:/app/src  # <- live source mount for breakpoint alignment
    environment:
      NODE_ENV: development
```

### VS Code launch.json

```json
{
  "name": "Docker: Attach Node",
  "type": "node",
  "request": "attach",
  "port": 9229,
  "address": "localhost",
  "localRoot": "${workspaceFolder}/src",
  "remoteRoot": "/app/src",
  "skipFiles": ["<node_internals>/**"],
  "restart": true
}
```

`localRoot`/`remoteRoot` mapping tells VS Code how to translate container file paths to local source paths.

---

## Python Debug In Docker

### Dockerfile With debugpy

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt debugpy
COPY . .
EXPOSE 8000 5678
CMD ["python", "-m", "debugpy", "--listen", "0.0.0.0:5678", \
     "--wait-for-client", "-m", "uvicorn", "main:app", \
     "--host", "0.0.0.0", "--port", "8000"]
```

### Without Modifying Entrypoint

Add to your Python app at startup:

```python
import debugpy
import os

if os.getenv('DEBUG_MODE') == '1':
    debugpy.listen(("0.0.0.0", 5678))
    debugpy.wait_for_client()  # optional: pause until debugger connects
```

### docker-compose.yml

```yaml
services:
  api:
    build: .
    ports:
      - "8000:8000"
      - "5678:5678"  # debug port
    volumes:
      - ./src:/app/src
    environment:
      DEBUG_MODE: "1"
```

### VS Code launch.json

```json
{
  "name": "Docker: Attach Python",
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
  ],
  "justMyCode": false
}
```

---

## Attach By Process ID (docker exec)

```bash
# Find Node.js PID in container.
docker exec -it <container-id> sh -c "ps aux | grep node"

# Output:
#  PID  TTY  STAT  TIME  COMMAND
#   12  ?    Sl    0:05  node --inspect=0.0.0.0:9229 src/server.js
```

```json
{
  "name": "Attach by PID",
  "type": "node",
  "request": "attach",
  "processId": "${command:PickProcess}"
}
```

---

## Remote SSH Debug

### Install Remote-SSH Extension

```text
Extensions (Cmd+Shift+X) -> search "Remote - SSH" -> Install
```

### Connect To Remote Host

```text
Cmd+Shift+P -> Remote-SSH: Connect to Host
-> ssh user@remote-host
-> VS Code opens a new window connected to the remote machine
-> Folder opened on remote filesystem
-> Extensions run on remote machine
-> Breakpoints work with remote files
```

### launch.json On Remote Host

After Remote-SSH connection, the `.vscode/launch.json` runs configurations on the remote machine:

```json
{
  "name": "Remote: Node Launch",
  "type": "node",
  "request": "launch",
  "program": "${workspaceFolder}/src/server.js"
}
```

This starts the process on the remote machine and attaches VS Code's debugger to it.

---

## Dev Container Debug

### .devcontainer/devcontainer.json

```json
{
  "name": "Node.js Dev Container",
  "image": "mcr.microsoft.com/devcontainers/javascript-node:18",
  "forwardPorts": [3000, 9229],
  "postCreateCommand": "npm install",
  "customizations": {
    "vscode": {
      "extensions": [
        "ms-vscode.js-debug-nightly"
      ]
    }
  }
}
```

### Using launch.json Inside Dev Container

```text
1. Cmd+Shift+P -> "Dev Containers: Reopen in Container"
2. VS Code reopens with files mounted inside the container.
3. .vscode/launch.json runs configurations inside the container.
4. Port forwarding makes container ports accessible on localhost.
```

The `.vscode/launch.json` is the same as a local launch config — port forwarding handles the networking.

---

## Troubleshooting Remote Debug

| Problem | Cause | Fix |
|---|---|---|
| Connection refused on port 9229 | Port not exposed in Docker | Add `-p 9229:9229` |
| Breakpoints not hit | localRoot/remoteRoot mismatch | Match container WORKDIR to remoteRoot |
| Grey/unverified breakpoints | Source file paths don't match | Check pathMappings or localRoot/remoteRoot |
| Process exits before attach | `--wait-for-client` needed | Add `debugpy.wait_for_client()` |
| Wrong PID attached | Multiple Node processes | Use `--inspect-brk` so target process pauses |

---

## Interview Sound Bite

VS Code remote debug works by forwarding the debug port (9229 for Node, 5678 for Python). For Docker: expose the port in docker-compose and add `localRoot`/`remoteRoot` path mappings so VS Code can map container file paths back to your local source. For Python: use `debugpy.listen()` in the app and the `debugpy` attach config in VS Code with `pathMappings`. Remote-SSH extension runs VS Code's backend on the remote machine — extensions, terminal, and debugger all execute remotely, making it transparent. Dev containers forward ports automatically and run launch configs inside the container.
