# 01. IntelliJ IDEA: Debugger Setup, Breakpoints, Debug Configuration

## Goal

Configure and launch the IntelliJ debugger for any Java project, understand the debug toolbar, and use all basic breakpoint types.

---

## Creating A Run/Debug Configuration

```text
Run -> Edit Configurations -> + -> Application

Name:          OrderServiceDebug
Main class:    com.example.orders.OrderServiceApplication
VM options:    -Xmx512m -Dspring.profiles.active=dev
Program args:  --server.port=8080
Working dir:   $MODULE_WORKING_DIR$
JRE:           17 (project JDK)
```

For Spring Boot projects: use the Spring Boot run config type instead of Application — it adds live reload and health check integration.

```text
Run -> Edit Configurations -> + -> Spring Boot
  Main class: com.example.orders.OrderServiceApplication
  Active profiles: dev
  Override parameters: (add any property overrides)
```

---

## Debug Toolbar Buttons

```text
[Rerun]  [Stop]  [Resume]  [Pause]  [Step Over]  [Step Into]  [Step Out]  [Run To Cursor]
```

| Button | Action | Shortcut (macOS) | Shortcut (Win/Linux) |
|---|---|---|---|
| Resume Program | Continue to next breakpoint | F9 | F9 |
| Step Over | Execute current line, stay in same method | F8 | F8 |
| Step Into | Enter the method being called | F7 | F7 |
| Force Step Into | Step into even JDK/library code | Alt+Shift+F7 | Alt+Shift+F7 |
| Step Out | Run to end of current method, return to caller | Shift+F8 | Shift+F8 |
| Run To Cursor | Run until the line where cursor is | Alt+F9 | Alt+F9 |
| Evaluate Expression | Open expression evaluator | Alt+F8 | Alt+F8 |
| Show Execution Point | Jump back to current line | Alt+F10 | Alt+F10 |

---

## Setting Breakpoints

### Line Breakpoint

```text
Click in the left gutter at the line number.
Red circle appears.
When execution reaches that line, it pauses BEFORE executing it.
```

### Conditional Breakpoint

```text
Right-click on the red breakpoint circle -> "Edit Breakpoint"

Condition field: orderId.equals("ORD-99001")
            or: price > 100.0 && quantity > 5
            or: user.getRole() == Role.ADMIN

Result: breakpoint only fires when condition is true.
        All other hits are passed through silently.
```

### Exception Breakpoint

```text
Run -> View Breakpoints (Cmd+Shift+F8) -> + -> Java Exception Breakpoints

Exception class: java.lang.NullPointerException
  Caught exceptions: checked (pauses even when caught by try/catch)
  Uncaught exceptions: checked (pauses before propagating up the stack)

This catches the NPE at the exact line it is thrown, not where it is caught.
```

### Method Breakpoint

```text
Click the gutter at the method declaration line (method name, not first code line).
IntelliJ shows a diamond-shaped breakpoint.
Pauses on entry AND/OR exit of that method.

Entry: see arguments as the method is called
Exit: see return value before returning to caller
```

### Field Watchpoint

```text
Right-click a field in the source -> "Add Field Watchpoint"
OR click gutter next to field declaration.

Options:
  Field access (read): pause when the field is READ
  Field modification (write): pause when the field is WRITTEN

Use case: find out where a field gets set to an unexpected value.
```

### Log Breakpoint (Non-Suspending)

```text
Right-click breakpoint -> Edit -> uncheck "Suspend"
Enable: "Evaluate and log" -> check
Expression: "Order created: " + orderId + " total=" + total

Result: prints to the Debug console without pausing execution.
Use for: tracing execution flow without stopping, like println debugging but cleaner.
```

---

## Debug Tool Window Panels

```text
Debugger tool window (bottom):
  ├── Frames    - call stack of current thread
  ├── Variables - local variables and 'this' fields at current frame
  ├── Watches   - pinned expressions that update on every step
  └── Threads   - list of all JVM threads (key for multithreading debug)

Console panel - stdout/stderr output
```

### Variables Panel

```text
Click triangle next to an object to expand its fields.
Right-click a variable -> "Set Value" to change it at runtime (without restarting).
Right-click -> "Add to Watches" to pin it in the Watches panel.
Right-click -> "Copy Value" to copy current value as string.
```

### Watches Panel

```text
Add watch: click + in Watches panel or press the watch shortcut.
Expression examples:
  user.getEmail()
  orderList.size()
  ((OrderService) service).pendingOrders
  order.getTotal() > 100

Watches re-evaluate on every step and breakpoint hit.
```

---

## Smart Step Into

```text
When multiple method calls are on one line:
  processOrder(validate(request), enrichOrder(request))
  
F7 (Step Into) shows a menu with arrows: choose WHICH method call to enter.
  -> validate()
  -> enrichOrder()
  -> processOrder()

This prevents entering all three when you only want one.
```

---

## Drop Frame (Rewind Execution)

```text
In the Frames panel: right-click any frame -> "Drop Frame"
  -> Execution returns to the entry of that method
  -> Variables reset to method entry state
  -> Use to re-run a method and observe it again

Limitation: side effects (DB writes, HTTP calls) are NOT reversed.
Use only for pure computation methods.
```

---

## Interview Sound Bite

IntelliJ debugger starts via a Run/Debug Configuration that defines the main class, JVM args, and working directory. The debug toolbar provides Step Over (F8), Step Into (F7), Step Out (Shift+F8), and Resume (F9). Conditional breakpoints filter on runtime expressions to avoid pausing on every hit. Exception breakpoints catch exceptions at the throw site. Field watchpoints identify unexpected state mutations. Log breakpoints add trace output without suspending — useful when suspending would change timing-dependent behavior.
