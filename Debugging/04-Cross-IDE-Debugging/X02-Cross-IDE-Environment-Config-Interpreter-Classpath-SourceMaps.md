# X02. Cross-IDE Environment Debugging: Config, Interpreter, Classpath, Source Maps

> Bridge module for IDE/runtime mismatches.
> Use this when code works in terminal but fails in the IDE, or works in one IDE but not another.

---

## 1. Core Idea

Many "debugger bugs" are really environment bugs.

```text
same code + different runtime config = different behavior
```

Before stepping through code, confirm the process you are debugging is the process you think it is:

- correct working directory
- correct Java JDK / Node version / Python interpreter
- correct env vars
- correct command-line args
- correct classpath/module path
- correct source maps
- correct port and profile
- correct container/remote process

---

## 2. Universal Environment Checklist

| Check | Java / IntelliJ | Node/JS / VS Code | Python / PyCharm or VS Code |
|---|---|---|---|
| Runtime | Project SDK / JDK | `node --version` | interpreter / venv / conda |
| Dependencies | Maven/Gradle classpath | `node_modules`, lockfile | pip/poetry/uv deps |
| Working dir | module/project root | workspace folder | project root |
| Env vars | VM options / env table | `env` in launch config | env table / `.env` |
| Args | Program args | `args` in launch config | script parameters |
| Profile | Spring profile | `NODE_ENV` | settings module / app env |
| Source mapping | compiled classes | source maps | source roots |
| Attach port | JDWP 5005 | inspector 9229 | debugpy 5678 |

---

## 3. Java Environment Debugging

Common IntelliJ mismatch:

```text
Terminal uses JDK 21.
IntelliJ run config uses JDK 17.
Bug appears only in IDE.
```

Check:

```text
File -> Project Structure -> Project SDK
Run -> Edit Configurations -> JRE
Maven/Gradle JVM settings
VM options
Program arguments
Working directory
Active Spring profile
```

Useful runtime prints:

```java
System.out.println(System.getProperty("java.version"));
System.out.println(System.getProperty("user.dir"));
System.out.println(System.getenv("SPRING_PROFILES_ACTIVE"));
System.out.println(Thread.currentThread().getName());
```

Classpath symptoms:

- class not found in IDE only
- test passes in Maven but fails in IntelliJ
- generated sources not recognized
- annotation processors not running
- wrong resource file loaded

Fix direction:

- reload Maven/Gradle project
- mark generated sources correctly
- enable annotation processing
- align project SDK and build tool JVM
- compare IDE command line with terminal command

---

## 4. Node.js / Frontend Environment Debugging

Common VS Code mismatch:

```text
Terminal runs npm script with env loader.
VS Code launch config runs node directly.
App misses env vars or transpilation.
```

Check:

```text
.vscode/launch.json
runtimeExecutable
runtimeArgs
program
cwd
env
envFile
sourceMaps
outFiles
skipFiles
```

Node runtime probes:

```js
console.log(process.version);
console.log(process.cwd());
console.log(process.env.NODE_ENV);
console.log(process.argv);
```

Frontend/browser debug symptoms:

- breakpoint is gray/unbound
- breakpoint hits transpiled code, not TypeScript source
- React component line numbers do not match
- code works in browser but not VS Code attach

Fix direction:

- enable source maps in bundler
- set `webRoot`
- set correct `outFiles`
- check dev server URL
- ensure browser launched by debugger matches the app instance
- avoid debugging stale built assets

---

## 5. Python Environment Debugging

Common mismatch:

```text
Terminal uses project venv.
IDE uses system Python.
Package exists in terminal but import fails in debugger.
```

Check:

```text
selected interpreter
virtualenv/conda environment
working directory
PYTHONPATH
environment variables
module vs script execution
pytest configuration
framework run config
```

Runtime probes:

```python
import os
import sys
import threading

print(sys.executable)
print(sys.version)
print(os.getcwd())
print(sys.path)
print(threading.current_thread().name)
```

Python-specific traps:

- running a package file as a script breaks relative imports
- missing `.env` file in IDE config
- pytest uses different working directory
- Django settings module not configured
- FastAPI reload starts a child process not attached to debugger

Fix direction:

- select the project venv explicitly
- use module mode when needed
- add source root correctly
- set `DJANGO_SETTINGS_MODULE`
- debug without reload first, then add reload after config works

---

## 6. Remote And Container Env Mismatches

Remote debugging adds another layer:

```text
local source path != container source path
local env != container env
local port != container port
```

Check:

- container image tag/digest
- command actually running inside container
- port forwarding
- path mappings
- mounted source code vs copied image code
- env vars inside the remote process
- debug server bound to `0.0.0.0` when needed

Evidence commands:

```bash
docker ps
docker inspect CONTAINER
docker exec CONTAINER env
docker exec CONTAINER pwd
docker exec CONTAINER ps aux
```

---

## 7. Debug Config Review Template

```text
Symptom:
Expected runtime:
Actual runtime:
IDE:
Language:
Command used by terminal:
Command used by IDE:
Working directory:
Env vars:
Args:
Interpreter/JDK/Node version:
Source mapping:
Remote/container path:
Fix:
Verification:
```

---

## 8. Strong Debugging Answer

```text
Before stepping into the code, I verify the debug process. I compare the IDE run configuration with the terminal command: runtime version, working directory, env vars, args, profiles, dependencies, source maps, and remote path mappings. If the bug only happens in the IDE, I assume environment drift until proven otherwise. Once the process identity matches, then I debug code logic.
```

---

## 9. Revision Notes

- One-line summary: Many debugger problems are runtime configuration mismatches.
- Three keywords: runtime, env, cwd.
- One trap: debugging logic before confirming the IDE is running the same process as terminal.
- Memory trick: `R-C-E-A` = Runtime, CWD, Env, Args.
