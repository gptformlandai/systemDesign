# 09. VS Code: Complete Keyboard Shortcuts for Debugging

## Core Debug Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Start / Continue (F5)** | F5 | F5 |
| **Stop debugging** | Shift+F5 | Shift+F5 |
| **Restart debugging** | Cmd+Shift+F5 | Ctrl+Shift+F5 |
| **Pause execution** | F6 | F6 |
| **Step Over** | F10 | F10 |
| **Step Into** | F11 | F11 |
| **Step Out** | Shift+F11 | Shift+F11 |
| **Continue to cursor** | (right-click line → Run to Cursor) | |

---

## Breakpoint Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Toggle breakpoint** | F9 | F9 |
| **Conditional breakpoint** | right-click gutter → Add Conditional Breakpoint | |
| **Logpoint** | right-click gutter → Add Logpoint | |
| **Inline breakpoint** | Shift+F9 | Shift+F9 |
| **Enable all breakpoints** | (Breakpoints panel checkbox header) | |
| **Disable all breakpoints** | (Breakpoints panel checkbox header) | |
| **Remove all breakpoints** | Cmd+Shift+F9 | Ctrl+Shift+F9 |

---

## View Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| **Open Debug view** | Cmd+Shift+D | Ctrl+Shift+D |
| **Open Debug console** | Cmd+Shift+Y | Ctrl+Shift+Y |
| **Toggle Debug console** | Cmd+Shift+Y | Ctrl+Shift+Y |
| **Focus call stack** | (click in Run & Debug panel) | |
| **Focus variables** | (click in Run & Debug panel) | |

---

## Variable Inspection

| Action | macOS | Windows / Linux |
|---|---|---|
| **Hover to inspect** | hover mouse over variable | |
| **Peek value inline** | Cmd+K Cmd+I | Ctrl+K Ctrl+I |
| **Add to Watch** | right-click variable → Add to Watch | |
| **Copy value** | right-click variable → Copy Value | |
| **Set value** | double-click variable value in Variables panel | |
| **Evaluate in console** | type expression in Debug Console | |

---

## Debug Console

The Debug Console (Cmd+Shift+Y) is a REPL inside the paused process:

```javascript
// Node.js: evaluate any expression in the current scope.
req.body
user.email
orders.filter(o => o.status === 'PENDING').length
JSON.stringify(response, null, 2)

// Python: same concept.
request.POST
len(queryset)
type(obj).__name__
```

Different from the terminal: expressions run inside the paused process's scope and can access local variables.

---

## Watch Expressions

```text
Run & Debug panel -> WATCH section -> + button

Examples:
  req.body.userId
  orders.length
  error?.message
  process.env.NODE_ENV
  user?.role === 'ADMIN'

Watches update automatically on every step.
```

---

## Inline Values Display

VS Code shows current values inline in the editor when paused:

```text
Settings -> "debug.inlineValues": "on"
OR
"debug.inlineValues": "auto"  (only shows for variables with known types)
```

---

## Call Stack Navigation

```text
Run & Debug panel -> CALL STACK section

Click any frame to:
  -> Jump to that file and line in the editor
  -> Update the Variables panel to show locals of that frame

Works the same as IntelliJ Frames panel.
```

---

## Debug Console Input Mode

```text
When paused at a breakpoint, the Debug Console input field has two modes:
  > (prompt)    = evaluate expression
  ... (no prompt) = continue multiline input

Examples:
  > 2 + 2
    4
  > orders.find(o => o.id === 'ORD-001')
    { id: 'ORD-001', status: 'PENDING', total: 99.50 }
  > orders[0].status = 'CANCELLED'
    'CANCELLED'  (changes actual runtime state)
```

Changing state in the Debug Console modifies the live process — useful for testing fix hypotheses.

---

## JavaScript-Specific: Debugger Statement

```javascript
function processOrder(order) {
  if (order.total > 10000) {
    debugger;  // <- execution pauses here when DevTools or VS Code is attached
  }
  // ...
}
```

`debugger;` works as a hardcoded breakpoint. VS Code respects it. Remove before committing.

---

## Task + Debug Integration

VS Code can run a build task before launching the debugger:

```json
{
  "name": "TypeScript: Debug",
  "type": "node",
  "request": "launch",
  "program": "${workspaceFolder}/dist/index.js",
  "preLaunchTask": "tsc: build - tsconfig.json",
  "outFiles": ["${workspaceFolder}/dist/**/*.js"],
  "sourceMaps": true
}
```

`preLaunchTask` runs the TypeScript compiler before every debug session.

---

## Quick Reference Card

```text
The 7 shortcuts to memorize first:
  F5           = Start / Continue
  Shift+F5     = Stop
  F10          = Step Over
  F11          = Step Into
  Shift+F11    = Step Out
  F9           = Toggle Breakpoint
  Cmd+Shift+D  = Open Debug View

For inspection:
  Hover                   = show current value tooltip
  Cmd+K Cmd+I             = force show value tooltip
  Debug Console (Cmd+Shift+Y) = REPL in process scope
```

---

## Interview Sound Bite

VS Code debug shortcuts: F5 start/continue, F10 step over, F11 step into, Shift+F11 step out, F9 toggle breakpoint, Shift+F5 stop. The Debug Console (Cmd+Shift+Y) is a live REPL in the paused process scope — evaluate expressions, inspect variables, and even modify state. `debugger;` in JavaScript/TypeScript acts as a hardcoded breakpoint that VS Code respects. Inline values display (debug.inlineValues: on) shows current variable values directly in the source editor while paused, making it faster to read state without switching to the Variables panel.
