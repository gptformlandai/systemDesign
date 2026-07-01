# 19. PyCharm: Remote Debug — Docker, SSH, pydevd

## Goal

Attach PyCharm to Python processes running in Docker containers, on remote SSH hosts, and using debugpy for any remote scenario.

---

## How Python Remote Debug Works

```text
Remote machine / Container
  -> Python process
  -> debugpy listening on a port (e.g., 5678)
  
PyCharm (local)
  -> TCP connection to debugpy port
  -> Source files resolved via path mappings
  -> Breakpoints translate to remote file paths
```

PyCharm uses pydevd (its internal debugger) for local sessions and can connect to debugpy (the VS Code-compatible protocol) for remote sessions.

---

## Method 1: debugpy In Application Code

```python
# app.py (add at startup)
import debugpy
import os

if os.getenv('PYDEVD_REMOTE_DEBUG') == '1':
    debugpy.listen(("0.0.0.0", 5678))
    print("[debug] Waiting for debugger on port 5678...")
    debugpy.wait_for_client()  # pause startup until debugger connects
```

Start the application:

```bash
PYDEVD_REMOTE_DEBUG=1 python app.py
```

---

## Method 2: debugpy As Entry Point

```bash
python -m debugpy --listen 0.0.0.0:5678 --wait-for-client app.py
# Starts app.py and waits for debugger to connect before executing any code.

# Without waiting (just expose the debug port, connect anytime):
python -m debugpy --listen 0.0.0.0:5678 app.py
```

---

## PyCharm Remote Debug Configuration

```text
Run -> Edit Configurations -> + -> Python Remote Debug

Name:      Docker Python Debug
Host:      localhost   (or remote IP)
Port:      5678

Path mappings:
  Local path:   /Users/you/project
  Remote path:  /app
```

Connect sequence:

```text
1. Start Python process with debugpy listening.
2. In PyCharm: click Debug on the Remote Debug config (not Run).
3. PyCharm connects to port 5678.
4. Breakpoints set in local source files now fire in the remote process.
```

---

## Docker: Python Debug

### Dockerfile

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt debugpy
COPY . .
EXPOSE 8000 5678
CMD ["python", "-m", "debugpy", "--listen", "0.0.0.0:5678", \
     "--wait-for-client", "app.py"]
```

### docker-compose.yml

```yaml
version: "3.8"
services:
  api:
    build: .
    ports:
      - "8000:8000"
      - "5678:5678"
    volumes:
      - ./src:/app/src   # mount source for path mapping alignment
    environment:
      PYTHONDONTWRITEBYTECODE: "1"
```

### PyCharm launch config

```text
Path mappings:
  Local:  ${PROJECT_ROOT}/src
  Remote: /app/src
```

---

## FastAPI In Docker Debug

```dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN pip install fastapi uvicorn debugpy
COPY . .
EXPOSE 8000 5678
CMD ["python", "-m", "debugpy", "--listen", "0.0.0.0:5678", \
     "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

Note: `--wait-for-client` makes the app wait for PyCharm before starting uvicorn. Omit if you want the app to start immediately and connect anytime.

---

## Django In Docker Debug

```bash
# Command in Dockerfile or docker-compose:
python -m debugpy --listen 0.0.0.0:5678 manage.py runserver 0.0.0.0:8000 --noreload --nothreading
```

---

## PyCharm SSH Interpreter

For remote Python running on a server:

```text
Settings (Cmd+,) -> Project -> Python Interpreter -> + -> SSH Interpreter

Host: user@remote-host.com
Port: 22
SSH auth: key pair or password

PyCharm:
  -> Deploys project files to remote path
  -> Runs debugger on remote Python
  -> Mirrors calls back to local IDE
```

This is deeper than just port forwarding — PyCharm manages the SSH tunnel and file sync.

---

## Path Mappings: Critical

Path mappings tell PyCharm how local source files correspond to remote paths.

Without correct mappings: breakpoints appear but never fire, or fire at wrong lines.

```text
Common mapping patterns:

Local:   /Users/aravind/project/src
Remote:  /app/src

Local:   /Users/aravind/project
Remote:  /home/ubuntu/app

Local:   C:\Users\aravind\project
Remote:  /app
```

---

## Troubleshooting

| Problem | Likely Cause | Fix |
|---|---|---|
| Connection refused | Port not exposed | Add -p 5678:5678 to docker run |
| Breakpoints never hit | Wrong path mapping | Verify localRoot vs remoteRoot |
| Process finishes before connect | No wait-for-client | Add `--wait-for-client` flag |
| Connection times out | Firewall on remote host | Open port 5678 in security group |
| Breakpoints hit wrong line | Stale .pyc files | Add `PYTHONDONTWRITEBYTECODE=1` |

---

## Interview Sound Bite

PyCharm remote debugging uses debugpy: run `python -m debugpy --listen 0.0.0.0:5678 app.py` on the remote side, then connect with a PyCharm Python Remote Debug configuration pointing to the host and port. For Docker: expose port 5678 in docker-compose and set path mappings from your local project root to the container's `/app` directory. `--wait-for-client` pauses the process until PyCharm connects — essential for debugging startup code. Path mappings are the critical piece that makes breakpoints in local `.py` files fire correctly in the remote process.
