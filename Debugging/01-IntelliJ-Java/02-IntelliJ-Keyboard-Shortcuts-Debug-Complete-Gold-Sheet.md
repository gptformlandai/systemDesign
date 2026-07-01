# 02. IntelliJ IDEA: Complete Keyboard Shortcuts for Debugging (Java)

## Core Navigation Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Start Debug** | Ctrl+D | Shift+F9 |
| **Stop Debug** | Cmd+F2 | Ctrl+F2 |
| **Resume Program** | F9 | F9 |
| **Pause Program** | — | — (click Pause button) |
| **Step Over** | F8 | F8 |
| **Step Into** | F7 | F7 |
| **Force Step Into** | Alt+Shift+F7 | Alt+Shift+F7 |
| **Step Out** | Shift+F8 | Shift+F8 |
| **Run To Cursor** | Alt+F9 | Alt+F9 |
| **Show Execution Point** | Alt+F10 | Alt+F10 |

---

## Breakpoint Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Toggle Line Breakpoint** | Cmd+F8 | Ctrl+F8 |
| **View All Breakpoints** | Cmd+Shift+F8 | Ctrl+Shift+F8 |
| **Edit Breakpoint** (at cursor) | Cmd+Shift+F8 (on line) | Ctrl+Shift+F8 (on line) |
| **Disable All Breakpoints** | (from View Breakpoints panel) | |
| **Mute All Breakpoints** | click mute icon in debug toolbar | |

---

## Expression And Inspection

| Action | macOS | Windows / Linux |
|---|---|---|
| **Evaluate Expression** | Alt+F8 | Alt+F8 |
| **Quick Evaluate** (cursor on var) | Opt+Cmd+F8 | Ctrl+Alt+F8 |
| **Add To Watches** | (right-click var → Add Watch) | |
| **Inspect Variable** | — hover or click triangle | |
| **Set Variable Value** | F2 (in Variables panel) | F2 |
| **Copy Variable Value** | Ctrl+C (in Variables panel) | Ctrl+C |

---

## Frames And Call Stack

| Action | macOS | Windows / Linux |
|---|---|---|
| **Select previous frame** | Alt+↑ | Alt+↑ |
| **Select next frame** | Alt+↓ | Alt+↓ |
| **Copy stack trace** | (right-click in Frames panel) | |
| **Drop frame (rewind)** | (right-click frame → Drop Frame) | |
| **Jump to source** | Enter on frame | Enter on frame |

---

## Thread Control

| Action | macOS | Windows / Linux |
|---|---|---|
| **View threads** | (Threads tab in Debug window) | |
| **Suspend thread** | (right-click thread → Suspend) | |
| **Resume thread** | (right-click thread → Resume) | |
| **Freeze thread** | (right-click → Freeze) | |
| **Dump thread** | (right-click → Dump) | |

---

## Debug Window Navigation

| Action | macOS | Windows / Linux |
|---|---|---|
| **Open/focus Debug window** | Cmd+5 | Alt+5 |
| **Focus Variables tab** | (click tab) | |
| **Focus Frames tab** | (click tab) | |
| **Focus Console tab** | (click tab) | |
| **Clear Console** | Cmd+K | Ctrl+K |
| **Close Debug tab** | Cmd+W | Ctrl+W |

---

## Smart Step Into And Custom Actions

| Action | macOS | Windows / Linux |
|---|---|---|
| **Smart Step Into** | Shift+F7 | Shift+F7 |
| (shows which call to enter on a chained line) | | |

---

## IntelliJ Run Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Run current config** | Ctrl+R | Shift+F10 |
| **Debug current config** | Ctrl+D | Shift+F9 |
| **Run context** (at cursor) | Ctrl+Shift+R | Ctrl+Shift+F10 |
| **Debug context** (at cursor) | Ctrl+Shift+D | Ctrl+Shift+F9 |
| **Edit Run Configuration** | (top right dropdown → Edit) | |

---

## Quick Reference Card (Memorize These 10 First)

```text
Priority 1 (use every debug session):
  F9  = Resume (continue to next breakpoint)
  F8  = Step Over (next line, same method)
  F7  = Step Into (enter called method)
  Shift+F8 = Step Out (finish method, return to caller)
  Alt+F9   = Run To Cursor (run until cursor line)
  Alt+F8   = Evaluate Expression (test any expression)
  Cmd+F8   = Toggle Breakpoint

Priority 2 (use for concurrency and deep investigation):
  Cmd+Shift+F8 = View All Breakpoints (add conditions here)
  F2 (in Variables) = Set Value (change variable at runtime)
  Shift+F7 = Smart Step Into (choose which call to enter)
```

---

## Evaluate Expression: Power Patterns

```text
# Call a method on any visible object.
orderService.getOrder("ORD-001")

# Cast and inspect.
((SpringOrderService) orderService).cache.size()

# Java stream evaluation.
orderList.stream().filter(o -> o.getTotal() > 100).count()

# String formatting.
String.format("Order %s has total %,.2f", order.getId(), order.getTotal())

# Modify state.
order.setStatus(OrderStatus.CANCELLED)
# Note: this actually changes the state in the running process.
```

---

## Interview Sound Bite

The 10 most important IntelliJ debug shortcuts: F9 resume, F8 step over, F7 step into, Shift+F8 step out, Alt+F9 run to cursor, Alt+F8 evaluate expression, Cmd+F8 toggle breakpoint, Cmd+Shift+F8 view/edit all breakpoints. Evaluate Expression (Alt+F8) is the most underused: you can call any method, evaluate streams, and even modify state without changing source code. Smart Step Into (Shift+F7) resolves the ambiguity of multi-call expressions on one line.
