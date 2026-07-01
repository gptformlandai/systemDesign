# IntelliJ IDEA Debug Shortcuts Cheatsheet

## macOS

| Action | Shortcut |
|---|---|
| Start Debug | Ctrl+D |
| Stop Debug | Cmd+F2 |
| Resume Program | F9 |
| Step Over | F8 |
| Step Into | F7 |
| Force Step Into | Alt+Shift+F7 |
| Step Out | Shift+F8 |
| Run To Cursor | Alt+F9 |
| Show Execution Point | Alt+F10 |
| Evaluate Expression | Alt+F8 |
| Quick Evaluate | Opt+Cmd+F8 |
| Toggle Breakpoint | Cmd+F8 |
| View All Breakpoints | Cmd+Shift+F8 |
| Set Variable Value | F2 (in Variables) |
| Smart Step Into | Shift+F7 |
| Debug Window | Cmd+5 |

## Windows / Linux

| Action | Shortcut |
|---|---|
| Start Debug | Shift+F9 |
| Stop Debug | Ctrl+F2 |
| Resume Program | F9 |
| Step Over | F8 |
| Step Into | F7 |
| Force Step Into | Alt+Shift+F7 |
| Step Out | Shift+F8 |
| Run To Cursor | Alt+F9 |
| Show Execution Point | Alt+F10 |
| Evaluate Expression | Alt+F8 |
| Quick Evaluate | Ctrl+Alt+F8 |
| Toggle Breakpoint | Ctrl+F8 |
| View All Breakpoints | Ctrl+Shift+F8 |
| Set Variable Value | F2 (in Variables) |
| Smart Step Into | Shift+F7 |
| Debug Window | Alt+5 |

## Breakpoint Types Quick Reference

```text
Line:        click gutter
Conditional: right-click breakpoint → Edit Breakpoint → Condition field
Exception:   Cmd+Shift+F8 → + → Java Exception Breakpoints
Method:      click gutter at method signature (diamond icon)
Watchpoint:  right-click field → Add Field Watchpoint
Log:         right-click breakpoint → uncheck Suspend + enable Evaluate and log
Hit count:   right-click breakpoint → More → Pass count
```

## Top 10 To Memorize

```text
1. F9  = Resume
2. F8  = Step Over
3. F7  = Step Into
4. Shift+F8  = Step Out
5. Alt+F9    = Run To Cursor
6. Alt+F8    = Evaluate Expression
7. Cmd+F8    = Toggle Breakpoint
8. Cmd+Shift+F8 = View/Edit All Breakpoints (conditions here)
9. F2 in Variables = Set Value
10. Shift+F7 = Smart Step Into (choose which call to enter)
```

## JDWP Remote Debug Flag

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

## Thread State Reference

```text
RUNNABLE     = executing
BLOCKED      = waiting for synchronized lock (DEADLOCK CANDIDATE)
WAITING      = indefinitely waiting (wait(), park(), join())
TIMED_WAITING = waiting with timeout
TERMINATED   = finished
```
