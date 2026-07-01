# VS Code Debug Shortcuts Cheatsheet

## macOS

| Action | Shortcut |
|---|---|
| Start / Continue | F5 |
| Stop Debug | Shift+F5 |
| Restart Debug | Cmd+Shift+F5 |
| Pause | F6 |
| Step Over | F10 |
| Step Into | F11 |
| Step Out | Shift+F11 |
| Toggle Breakpoint | F9 |
| Remove All Breakpoints | Cmd+Shift+F9 |
| Open Debug View | Cmd+Shift+D |
| Open Debug Console | Cmd+Shift+Y |
| Hover Inspect | Cmd+K Cmd+I |

## Windows / Linux

| Action | Shortcut |
|---|---|
| Start / Continue | F5 |
| Stop Debug | Shift+F5 |
| Restart Debug | Ctrl+Shift+F5 |
| Pause | F6 |
| Step Over | F10 |
| Step Into | F11 |
| Step Out | Shift+F11 |
| Toggle Breakpoint | F9 |
| Remove All Breakpoints | Ctrl+Shift+F9 |
| Open Debug View | Ctrl+Shift+D |
| Open Debug Console | Ctrl+Shift+Y |

## Node.js Debug Flags

```bash
node --inspect=9229 server.js          # attach anytime
node --inspect-brk=9229 server.js      # pause before first line
```

## Python Debug Flags

```bash
python -m debugpy --listen 0.0.0.0:5678 --wait-for-client app.py
```

## launch.json Key Fields

```text
type:    "node" | "chrome" | "debugpy"
request: "launch" | "attach"
skipFiles: ["<node_internals>/**", "${workspaceFolder}/node_modules/**"]
showAsyncStacks: true
restart: true    (for nodemon attach)
```

## Breakpoint Types In VS Code

```text
Line:        click gutter or F9
Conditional: right-click gutter → Add Conditional Breakpoint
Logpoint:    right-click gutter → Add Logpoint  (use {variable} syntax)
Inline:      Shift+F9
```

## Top 7 To Memorize

```text
1. F5           = Start / Continue
2. F10          = Step Over
3. F11          = Step Into
4. Shift+F11    = Step Out
5. F9           = Toggle Breakpoint
6. Shift+F5     = Stop
7. Cmd+Shift+Y  = Debug Console (REPL)
```

## Debug Console Examples (Node.js)

```javascript
req.body               // inspect request body
orders.length          // count items
error?.message         // optional chain inspection
JSON.stringify(obj, null, 2)  // pretty print
```

## Django / FastAPI Required Args

```text
Django:  --noreload --nothreading
FastAPI: no --reload
```
