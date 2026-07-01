# PyCharm Debug Shortcuts Cheatsheet

## macOS

| Action | Shortcut |
|---|---|
| Start Debug | Ctrl+D |
| Stop Debug | Cmd+F2 |
| Resume Program | F9 |
| Step Over | F8 |
| Step Into | F7 |
| Step Into My Code | Alt+Shift+F7 |
| Step Out | Shift+F8 |
| Run To Cursor | Alt+F9 |
| Show Execution Point | Alt+F10 |
| Evaluate Expression | Alt+F8 |
| Quick Evaluate | Opt+Cmd+F8 |
| Toggle Breakpoint | Cmd+F8 |
| View All Breakpoints | Cmd+Shift+F8 |
| Set Variable Value | F2 (in Variables) |
| Debug Window | Cmd+5 |

## Windows / Linux

| Action | Shortcut |
|---|---|
| Start Debug | Shift+F9 |
| Stop Debug | Ctrl+F2 |
| Resume Program | F9 |
| Step Over | F8 |
| Step Into | F7 |
| Step Into My Code | Alt+Shift+F7 |
| Step Out | Shift+F8 |
| Run To Cursor | Alt+F9 |
| Show Execution Point | Alt+F10 |
| Evaluate Expression | Alt+F8 |
| Quick Evaluate | Ctrl+Alt+F8 |
| Toggle Breakpoint | Ctrl+F8 |
| View All Breakpoints | Ctrl+Shift+F8 |
| Set Variable Value | F2 (in Variables) |
| Debug Window | Alt+5 |

## Run Config Required Args

```text
Django:   --noreload --nothreading
FastAPI:  NO --reload
pytest:   -v -s (s = no output capture)
```

## Remote Debug (debugpy)

```bash
python -m debugpy --listen 0.0.0.0:5678 --wait-for-client app.py
```

PyCharm config:

```text
Run → Edit Configurations → + → Python Remote Debug
Host: localhost  Port: 5678
Path mappings: /local/path → /remote/path
```

## Python Builtins For Debug

```python
breakpoint()                    # Python 3.7+ pause (PyCharm respects it)
threading.enumerate()           # list all live threads
asyncio.all_tasks()             # list all active asyncio tasks
asyncio.current_task().get_name()  # name of current task
```

## Thread Suspension Modes

```text
All:    all threads pause when any breakpoint fires (default)
Thread: only the triggering thread pauses; others continue
          (essential for race condition investigation)
```

## GIL Quick Reference

```text
GIL allows:    one thread executing Python bytecode at a time
GIL releases:  during I/O, every ~5ms, during C extension calls
GIL prevents:  parallel CPU computation across threads
GIL does NOT prevent: race conditions (counter += 1 is not atomic)

Fix race conditions: threading.Lock() around shared state
```

## Top 7 To Memorize

```text
1. F9          = Resume
2. F8          = Step Over
3. F7          = Step Into
4. Shift+F8    = Step Out
5. Alt+F9      = Run To Cursor
6. Alt+F8      = Evaluate Expression
7. Ctrl+D      = Start Debug (macOS)
Bonus: Alt+Shift+F7 = Step Into My Code (skip libraries)
```
