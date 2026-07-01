# 16. PyCharm: Complete Keyboard Shortcuts for Debugging (Python)

## Core Navigation Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Start Debug** | Ctrl+D | Shift+F9 |
| **Stop Debug** | Cmd+F2 | Ctrl+F2 |
| **Resume Program** | F9 | F9 |
| **Step Over** | F8 | F8 |
| **Step Into** | F7 | F7 |
| **Step Into My Code** | Alt+Shift+F7 | Alt+Shift+F7 |
| **Step Out** | Shift+F8 | Shift+F8 |
| **Run To Cursor** | Alt+F9 | Alt+F9 |
| **Show Execution Point** | Alt+F10 | Alt+F10 |

---

## Breakpoint Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Toggle Breakpoint** | Cmd+F8 | Ctrl+F8 |
| **View All Breakpoints** | Cmd+Shift+F8 | Ctrl+Shift+F8 |
| **Edit Breakpoint** | Cmd+Shift+F8 on line | Ctrl+Shift+F8 on line |
| **Disable/Enable Breakpoint** | right-click -> disable | right-click -> disable |
| **Mute All Breakpoints** | mute icon in debug toolbar | |

---

## Evaluation And Inspection

| Action | macOS | Windows / Linux |
|---|---|---|
| **Evaluate Expression** | Alt+F8 | Alt+F8 |
| **Quick Evaluate** | Opt+Cmd+F8 | Ctrl+Alt+F8 |
| **Add to Watches** | right-click → Add to Watches | |
| **Inspect Variable** | hover or click triangle | |
| **Set Variable Value** | F2 in Variables panel | F2 |
| **Copy Variable Value** | Ctrl+C in Variables panel | Ctrl+C |

---

## Frames And Call Stack

| Action | macOS | Windows / Linux |
|---|---|---|
| **Select previous frame** | Alt+↑ | Alt+↑ |
| **Select next frame** | Alt+↓ | Alt+↓ |
| **Copy stack trace** | right-click Frames → Copy Stack | |
| **Jump to source** | Enter on frame | Enter on frame |

---

## Thread Panel

| Action | macOS | Windows / Linux |
|---|---|---|
| **Switch active thread** | click thread in Frames dropdown | |
| **View thread state** | Frames panel header dropdown | |

---

## Debug Window Navigation

| Action | macOS | Windows / Linux |
|---|---|---|
| **Open/focus Debug window** | Cmd+5 | Alt+5 |
| **Clear console** | Cmd+K | Ctrl+K |

---

## Run Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Run current config** | Ctrl+R | Shift+F10 |
| **Debug current config** | Ctrl+D | Shift+F9 |
| **Run context** (cursor) | Ctrl+Shift+R | Ctrl+Shift+F10 |
| **Debug context** (cursor) | Ctrl+Shift+D | Ctrl+Shift+F9 |
| **Edit Run Config** | Edit Configurations dialog | |

---

## Quick Reference Card

```text
Priority 1 (every session):
  F9         = Resume
  F8         = Step Over
  F7         = Step Into
  Shift+F8   = Step Out
  Alt+F9     = Run To Cursor
  Alt+F8     = Evaluate Expression
  Cmd+F8     = Toggle Breakpoint

Priority 2 (deep investigation):
  Cmd+Shift+F8 = View All Breakpoints (add conditions here)
  F2 (Variables panel) = Set Value
  Alt+Shift+F7 = Step Into My Code (skip standard library)
```

---

## Evaluate Expression: Power Patterns

```python
# Call any in-scope function.
self.get_order('ORD-001')

# List comprehension on live data.
[o for o in orders if o.status == 'FAILED']

# Dict inspection.
{k: v for k, v in request.POST.items() if 'password' not in k}

# Check type.
type(response).__name__

# Pandas inspection (data science).
df[df['status'] == 'PENDING'].describe()

# Modify state.
order.status = 'CANCELLED'  # actually changes the live object

# Import and inspect.
import json; print(json.dumps(vars(order), default=str, indent=2))
```

---

## PyCharm Debugger Keyboard Cheat Sheet (Printable)

```text
+------------------------+-------------------+-------------------+
| Action                 | macOS             | Windows/Linux     |
+------------------------+-------------------+-------------------+
| Start Debug            | Ctrl+D            | Shift+F9          |
| Stop                   | Cmd+F2            | Ctrl+F2           |
| Resume                 | F9                | F9                |
| Step Over              | F8                | F8                |
| Step Into              | F7                | F7                |
| Step Into My Code      | Alt+Shift+F7      | Alt+Shift+F7      |
| Step Out               | Shift+F8          | Shift+F8          |
| Run To Cursor          | Alt+F9            | Alt+F9            |
| Evaluate               | Alt+F8            | Alt+F8            |
| Toggle Breakpoint      | Cmd+F8            | Ctrl+F8           |
| All Breakpoints        | Cmd+Shift+F8      | Ctrl+Shift+F8     |
| Set Variable Value     | F2 (in Variables) | F2                |
| Debug Window           | Cmd+5             | Alt+5             |
+------------------------+-------------------+-------------------+
```

---

## Step Into My Code vs Step Into

```text
Step Into (F7):
  Enters ANY function call, including:
    - Python standard library (os.path, json, datetime)
    - Third-party packages (requests, SQLAlchemy, etc.)
    - Your code

Step Into My Code (Alt+Shift+F7):
  Only enters functions in your project.
  Skips stdlib and packages.
  Same as VS Code's "justMyCode: true" but per-step.
```

---

## Interview Sound Bite

PyCharm debug shortcuts mirror IntelliJ: F9 resume, F8 step over, F7 step into, Shift+F8 step out, Alt+F9 run to cursor, Alt+F8 evaluate, Cmd+F8 toggle breakpoint. Step Into My Code (Alt+Shift+F7) is the PyCharm-specific shortcut that skips standard library and third-party packages — crucial for staying in your code while debugging. Evaluate Expression (Alt+F8) is a full Python REPL in the paused process: you can run list comprehensions, call methods, inspect DataFrames, and modify runtime state.
