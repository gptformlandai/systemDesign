# 15. PyCharm: Debugger Setup, Breakpoints, Debug Config

## Goal

Configure and launch the PyCharm debugger for Python applications — Run/Debug configurations, debug toolbar, all breakpoint types, Variables/Watches/Frames panels.

---

## Creating A Run/Debug Configuration

```text
Run -> Edit Configurations -> + -> Python

Name:          OrderService Debug
Script path:   /path/to/project/src/main.py
Parameters:    --host 0.0.0.0 --port 8000
Environment:   PYTHONDONTWRITEBYTECODE=1;PYTHONPATH=/path/to/project/src
Working dir:   /path/to/project
Python interpreter: Project Default (3.11)
```

For Django:

```text
+ -> Django server
  
Name:          Django Dev
Working dir:   /path/to/project
  Additional options: --noreload --nothreading
  Custom run command: (blank, uses manage.py runserver)
```

---

## Debug Toolbar Buttons

| Button | Action | Shortcut (macOS) | Shortcut (Win/Linux) |
|---|---|---|---|
| Resume Program | Continue to next breakpoint | F9 | F9 |
| Step Over | Execute current line | F8 | F8 |
| Step Into | Enter the called function | F7 | F7 |
| Step Into My Code | Like Step Into but skips library code | Alt+Shift+F7 | Alt+Shift+F7 |
| Step Out | Finish current function, return to caller | Shift+F8 | Shift+F8 |
| Run To Cursor | Run until cursor line | Alt+F9 | Alt+F9 |
| Evaluate Expression | Open evaluator | Alt+F8 | Alt+F8 |
| Stop | Terminate debug session | Cmd+F2 | Ctrl+F2 |

---

## Setting Breakpoints

### Line Breakpoint

```text
Click in the left gutter next to the line number.
Red circle appears.
Pauses BEFORE executing that line.
```

### Conditional Breakpoint

```text
Right-click the red circle -> "Edit Breakpoint..."

Condition: order_id == "ORD-99001"
        or total > 10000
        or request.user.is_staff

The breakpoint only fires when the condition is True.
```

### Exception Breakpoint

```text
Run -> View Breakpoints (Cmd+Shift+F8)
  -> Python Exception Breakpoints -> +
  
Exception class: AttributeError
               : KeyError
               : requests.exceptions.HTTPError

Options:
  On raise:   pause when exception is raised (before any try/except handles it)
  On handle:  pause when it's caught inside except
```

### Log Breakpoint (Non-Suspending)

```text
Right-click gutter -> Add Breakpoint (or existing breakpoint)
  -> More options
  -> Uncheck "Suspend"
  -> "Log message": f"Processing order {order_id} total={total}"
  -> Logs to the Debug Console without pausing.
```

---

## Debug Tool Window Panels

```text
Debugger tool window (bottom):
  ├── Frames    - call stack of current thread
  ├── Variables - local and global variables at current frame
  ├── Watches   - pinned expressions (updated on every step)
  └── Console   - stdout/stderr, REPL for evaluation

Threads panel:
  (accessible via Frames dropdown when multiple threads active)
```

### Variables Panel

```python
# When paused in a function:
LOCALS:
  order_id  = 'ORD-001'
  total     = 9500.00
  items     = [list: 3 elements]  <- click triangle to expand
  self      = <OrderService object>  <- expand to see all attributes

GLOBALS:
  __name__ = '__main__'
  DB_URL   = 'postgresql://...'
```

Right-click a variable:
- "Set Value" — change at runtime
- "Add to Watches" — pin expression
- "Copy Value" — copy to clipboard
- "View as..." — render as hex, binary, etc.

### Watches Panel

```python
# Add expressions that update on every step.
len(order_list)
order.status in ('PENDING', 'PROCESSING')
request.POST.get('user_id')
self._cache.get(order_id)
```

---

## Python 3.7+ builtin breakpoint()

```python
def process_order(order):
    if order['total'] > 10000:
        breakpoint()   # PyCharm pauses here — no import needed
    return calculate_tax(order)
```

PyCharm respects `breakpoint()` in debug mode. Remove before committing.

---

## Evaluate Expression (Alt+F8)

```python
# In the Evaluate Expression dialog when paused:

# Call any function visible in scope.
orderService.get_order('ORD-001')

# Test a condition.
len(pending_orders) > 100

# Inspect dict/list comprehension.
[o for o in orders if o['status'] == 'FAILED']

# Modify state at runtime.
order['status'] = 'CANCELLED'  # actually changes the live object

# Import and use a module.
import json; json.dumps(order, indent=2)
```

---

## Smart Step Into

When a line has multiple function calls, PyCharm shows a popup asking which call to enter:

```python
result = process(validate(request), enrich(request))
# F7 (Step Into) -> popup: choose validate() or enrich() or process()
```

---

## PyCharm Debugger vs pdb

| Feature | PyCharm | pdb |
|---|---|---|
| GUI variable inspection | Yes | No |
| Conditional breakpoints | Yes (no code change) | Requires code change |
| Async stack trace | Yes | Limited |
| Watches | Yes | No |
| Remote debug | Yes (via pydevd/debugpy) | Yes (via pdb.connect) |
| Thread inspection | Yes | Limited |

---

## Interview Sound Bite

PyCharm's debugger starts with a Run/Debug Configuration that defines the script, interpreter, args, and environment. The debug toolbar: F8 step over, F7 step into, Shift+F8 step out, F9 resume, Alt+F9 run to cursor, Alt+F8 evaluate. Conditional breakpoints filter on runtime Python expressions. Exception breakpoints catch exceptions at the `raise` site, not the `except` site — this is where the actual bug is. The Evaluate Expression dialog can call any in-scope function, inspect complex objects, and even mutate state, making it a live Python REPL inside the debugged process.
