# 12. VS Code: Python Debug — Interpreter, Django, FastAPI

## Goal

Debug Python applications in VS Code using the debugpy extension — select interpreters, configure Django and FastAPI, debug pytest tests, and use justMyCode to control stepping scope.

---

## Setup: Python Extension And debugpy

```text
1. Install "Python" extension from Microsoft (ms-python.python).
2. Install "Pylance" for type checking and IntelliSense.
3. debugpy is installed automatically with the Python extension.
```

---

## Selecting The Python Interpreter

```text
Cmd+Shift+P -> Python: Select Interpreter
  -> Lists all Python installations:
     /usr/bin/python3
     /usr/local/bin/python3
     ~/.pyenv/shims/python
     venv/bin/python      <- select this for project with virtualenv
     .venv/bin/python

Or set in .vscode/settings.json:
```

```json
{
  "python.defaultInterpreterPath": "${workspaceFolder}/.venv/bin/python"
}
```

---

## Basic Python Launch Config

```json
{
  "name": "Python: Launch",
  "type": "debugpy",
  "request": "launch",
  "program": "${file}",
  "console": "integratedTerminal",
  "justMyCode": true,
  "env": {
    "PYTHONPATH": "${workspaceFolder}/src"
  }
}
```

`justMyCode: true` means Step Into only enters your own code — not the standard library or installed packages. Set to `false` when you need to step into a library.

---

## Django Debug Config

```json
{
  "name": "Django: runserver",
  "type": "debugpy",
  "request": "launch",
  "program": "${workspaceFolder}/manage.py",
  "args": [
    "runserver",
    "--noreload",
    "--nothreading"
  ],
  "django": true,
  "console": "integratedTerminal",
  "cwd": "${workspaceFolder}",
  "env": {
    "DJANGO_SETTINGS_MODULE": "myapp.settings.dev"
  },
  "justMyCode": true
}
```

Critical flags:
- `--noreload`: Django by default uses a file watcher that spawns a subprocess. That subprocess is not debugged. `--noreload` disables the watcher so only one process exists.
- `--nothreading`: makes Django single-threaded, so the debugger doesn't lose control to concurrent threads.

---

## FastAPI / Uvicorn Debug Config

```json
{
  "name": "FastAPI: uvicorn",
  "type": "debugpy",
  "request": "launch",
  "module": "uvicorn",
  "args": [
    "main:app",
    "--host", "0.0.0.0",
    "--port", "8000"
  ],
  "cwd": "${workspaceFolder}",
  "env": {
    "PYTHONDONTWRITEBYTECODE": "1"
  },
  "jinja": true,
  "justMyCode": false
}
```

Do NOT use `--reload` — it forks a new process that won't be connected to the debugger.

---

## Pytest Debug Config

```json
{
  "name": "Python: Pytest",
  "type": "debugpy",
  "request": "launch",
  "module": "pytest",
  "args": [
    "${workspaceFolder}/tests",
    "-v",
    "-s"
  ],
  "cwd": "${workspaceFolder}",
  "justMyCode": false,
  "env": {
    "PYTHONPATH": "${workspaceFolder}/src"
  }
}
```

`-s` disables output capture so `print()` and `breakpoint()` work. `justMyCode: false` allows stepping into the code under test without restriction.

### Debug A Single Test File

```json
{
  "name": "Python: Single Test File",
  "type": "debugpy",
  "request": "launch",
  "module": "pytest",
  "args": ["${file}", "-v", "-s"],
  "justMyCode": false
}
```

---

## Python builtin breakpoint()

Python 3.7+ has a built-in `breakpoint()` function:

```python
def process_order(order):
    if order['total'] > 10000:
        breakpoint()  # VS Code pauses here when debugpy is running
    return calculate_tax(order)
```

Unlike `import pdb; pdb.set_trace()`, `breakpoint()` integrates with VS Code's debugpy automatically.

---

## Inspecting Variables

```text
When paused at a breakpoint:
  Variables panel (left side):
    LOCALS: function-local variables
    GLOBALS: module-level variables
    SPECIAL VARIABLES: __builtins__, __file__, etc.

Click triangle next to any list/dict/object to expand it.
```

### Debug Console Evaluation

```python
# In the VS Code Debug Console when paused in Python:
order['items']
len(queryset)
type(response).__name__
[o for o in orders if o.status == 'PENDING']
df.describe()  # pandas DataFrame inspection
```

---

## Remote Python Debug (debugpy listen)

```python
# Add to your application for remote debugging.
import debugpy
debugpy.listen(("0.0.0.0", 5678))
print("Waiting for debugger to attach...")
debugpy.wait_for_client()  # pause until VS Code connects
```

VS Code attach config:

```json
{
  "name": "Python: Remote Attach",
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

## Common Django Debugging Targets

```python
# View function breakpoint.
def order_detail(request, order_id):
    # breakpoint: inspect request.user, order_id
    order = Order.objects.get(pk=order_id)
    # breakpoint: inspect order fields
    return render(request, 'orders/detail.html', {'order': order})

# Django middleware breakpoint.
class TimingMiddleware:
    def __call__(self, request):
        # breakpoint: inspect request before it hits the view
        response = self.get_response(request)
        # breakpoint: inspect response before it's sent
        return response

# Django signal breakpoint.
@receiver(post_save, sender=Order)
def order_saved(sender, instance, created, **kwargs):
    # breakpoint: runs after Order.save()
    if created:
        send_confirmation_email(instance)
```

---

## Interview Sound Bite

VS Code Python debugging uses debugpy, installed with the Python extension. Django requires `--noreload --nothreading` or the debugger attaches to the wrong process (the file-watching subprocess, not the server). FastAPI must not use `--reload` for the same reason. `justMyCode: true` limits stepping to your code only — set to `false` when you need to step into library internals. Python 3.7+ `breakpoint()` is cleaner than `pdb.set_trace()` and integrates automatically with VS Code. For remote debug: `debugpy.listen()` in the app, then attach from VS Code with path mappings for container or remote environments.
