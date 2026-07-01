# 08. VS Code: Debugger Setup, launch.json Configuration

## Goal

Set up VS Code debugging for Node.js, JavaScript, TypeScript, and Python using `.vscode/launch.json`. Understand attach vs launch, compound configurations, and all key fields.

---

## Creating launch.json

```text
1. Open the Debug view: Cmd+Shift+D (macOS) / Ctrl+Shift+D (Windows)
2. Click "create a launch.json file"
3. VS Code detects project type and generates a template.
4. File is saved to .vscode/launch.json in the workspace root.
```

---

## launch.json Structure

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Node: Launch Server",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/src/server.js",
      "args": ["--port", "3000"],
      "env": {
        "NODE_ENV": "development",
        "PORT": "3000"
      },
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal"
    }
  ]
}
```

---

## Key Fields Explained

| Field | Values | Meaning |
|---|---|---|
| `type` | `node`, `chrome`, `python`, `debugpy`, `pwa-node` | Debugger type |
| `request` | `launch` or `attach` | Start a new process or attach to existing |
| `program` | file path | Entry point to launch |
| `args` | string array | Command-line args to the program |
| `env` | object | Environment variables for the process |
| `envFile` | `.env` file path | Load env vars from file |
| `cwd` | directory path | Working directory |
| `port` | number | Port for attach mode |
| `processId` | number or `${command:PickProcess}` | Process to attach to |
| `skipFiles` | glob array | Skip these files during step-into |
| `outFiles` | glob array | Where to find compiled JS (for TypeScript) |
| `sourceMaps` | boolean | Enable source map support |
| `console` | `integratedTerminal`, `externalTerminal`, `internalConsole` | Where stdout goes |

---

## Node.js: Launch Configuration

```json
{
  "name": "Node: Launch",
  "type": "node",
  "request": "launch",
  "program": "${workspaceFolder}/src/index.js",
  "args": [],
  "env": {
    "NODE_ENV": "development"
  },
  "envFile": "${workspaceFolder}/.env.development",
  "cwd": "${workspaceFolder}",
  "console": "integratedTerminal",
  "skipFiles": [
    "<node_internals>/**",
    "${workspaceFolder}/node_modules/**"
  ]
}
```

---

## Node.js: Attach To Running Process

```json
{
  "name": "Node: Attach",
  "type": "node",
  "request": "attach",
  "port": 9229,
  "localRoot": "${workspaceFolder}",
  "remoteRoot": "/app",
  "skipFiles": ["<node_internals>/**"]
}
```

Start node with:

```bash
node --inspect=0.0.0.0:9229 src/server.js
# or to pause until debugger connects:
node --inspect-brk=0.0.0.0:9229 src/server.js
```

---

## TypeScript: Launch With Source Maps

```json
{
  "name": "TypeScript: Launch",
  "type": "node",
  "request": "launch",
  "runtimeArgs": ["-r", "ts-node/register"],
  "args": ["${workspaceFolder}/src/index.ts"],
  "cwd": "${workspaceFolder}",
  "env": {
    "NODE_ENV": "development",
    "TS_NODE_PROJECT": "${workspaceFolder}/tsconfig.json"
  },
  "sourceMaps": true,
  "outFiles": ["${workspaceFolder}/dist/**/*.js"]
}
```

---

## Python: Launch Configuration

```json
{
  "name": "Python: Launch",
  "type": "debugpy",
  "request": "launch",
  "program": "${workspaceFolder}/src/app.py",
  "args": [],
  "env": {
    "PYTHONPATH": "${workspaceFolder}/src"
  },
  "cwd": "${workspaceFolder}",
  "console": "integratedTerminal",
  "justMyCode": true
}
```

---

## Python: Django Debug Config

```json
{
  "name": "Django: runserver (debug)",
  "type": "debugpy",
  "request": "launch",
  "program": "${workspaceFolder}/manage.py",
  "args": ["runserver", "--noreload", "--nothreading"],
  "django": true,
  "cwd": "${workspaceFolder}",
  "env": {
    "DJANGO_SETTINGS_MODULE": "myapp.settings.dev"
  }
}
```

`--noreload` prevents Django from launching a second process (which breaks the debugger).

---

## Python: FastAPI / Uvicorn Debug Config

```json
{
  "name": "FastAPI: uvicorn (debug)",
  "type": "debugpy",
  "request": "launch",
  "module": "uvicorn",
  "args": ["main:app", "--host", "0.0.0.0", "--port", "8000"],
  "cwd": "${workspaceFolder}",
  "env": {
    "PYTHONDONTWRITEBYTECODE": "1"
  },
  "jinja": true
}
```

Do NOT use `--reload` — it breaks the debugger.

---

## Compound Configuration (Multi-Process Debug)

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Backend: Node",
      "type": "node",
      "request": "launch",
      "program": "${workspaceFolder}/backend/server.js"
    },
    {
      "name": "Frontend: Chrome",
      "type": "chrome",
      "request": "launch",
      "url": "http://localhost:3000",
      "webRoot": "${workspaceFolder}/frontend/src"
    }
  ],
  "compounds": [
    {
      "name": "Full Stack Debug",
      "configurations": ["Backend: Node", "Frontend: Chrome"],
      "stopAll": true
    }
  ]
}
```

Start both backend and frontend debuggers with a single launch.

---

## Attach To Process By PID

```json
{
  "name": "Attach: By PID",
  "type": "node",
  "request": "attach",
  "processId": "${command:PickProcess}"
}
```

VS Code shows a process picker when you start this config.

---

## Variable Substitutions

| Variable | Value |
|---|---|
| `${workspaceFolder}` | Root folder of the workspace |
| `${file}` | Currently open file |
| `${fileBasename}` | Filename without path |
| `${fileBasenameNoExtension}` | Filename without extension |
| `${env:VAR}` | Value of environment variable |
| `${command:PickProcess}` | Interactive process picker |
| `${input:variableName}` | Prompt user for input at launch time |

---

## Interview Sound Bite

VS Code debugging is configured via `.vscode/launch.json`. The two key modes are `launch` (VS Code starts the process) and `attach` (VS Code connects to a running process). `skipFiles` prevents stepping into node internals and node_modules. For TypeScript, enable `sourceMaps` and set `outFiles` to the compiled output. Django must be launched with `--noreload --nothreading` so only one process runs and the debugger can attach to it. Compound configurations allow debugging multiple services (Node backend + React frontend) with a single launch command.
