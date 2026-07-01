# 10. VS Code: Node.js Debug — Express, Async, Event Loop

## Goal

Debug Node.js applications: Express middleware chains, async/await flows, Promise rejections, and the event loop — all in VS Code.

---

## Node.js Debug Flags

```bash
# Start with inspector (does not pause).
node --inspect src/server.js
# Opens inspector on 127.0.0.1:9229 by default.

# Start and pause until debugger connects.
node --inspect-brk src/server.js
# Pauses at the first line of code. Use to debug startup.

# Specify host and port (expose beyond localhost).
node --inspect=0.0.0.0:9229 src/server.js

# With npm start.
node --inspect node_modules/.bin/nodemon src/server.js
```

---

## VS Code Launch Config For Node.js

```json
{
  "name": "Node: Launch Server",
  "type": "node",
  "request": "launch",
  "program": "${workspaceFolder}/src/server.js",
  "args": [],
  "env": {
    "NODE_ENV": "development",
    "PORT": "3000"
  },
  "cwd": "${workspaceFolder}",
  "console": "integratedTerminal",
  "skipFiles": [
    "<node_internals>/**",
    "${workspaceFolder}/node_modules/**"
  ]
}
```

---

## Attaching To nodemon

```json
{
  "name": "Node: Attach To nodemon",
  "type": "node",
  "request": "attach",
  "restart": true,
  "port": 9229,
  "skipFiles": ["<node_internals>/**"]
}
```

`restart: true` tells VS Code to reconnect automatically when nodemon restarts the process.

Start nodemon with inspect:

```json
// package.json
{
  "scripts": {
    "debug": "nodemon --inspect=0.0.0.0:9229 src/server.js"
  }
}
```

---

## Debugging Express Middleware Chain

```javascript
// express-app.js
const express = require('express');
const app = express();

app.use(express.json());

// Middleware 1: Auth.
app.use((req, res, next) => {
  // <- set breakpoint here to inspect req.headers
  if (!req.headers.authorization) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  req.userId = parseToken(req.headers.authorization);
  next();  // <- step over next() to proceed to the next middleware
});

// Middleware 2: Logging.
app.use((req, res, next) => {
  // <- breakpoint here shows req.userId was set by previous middleware
  console.log(`${req.method} ${req.path} userId=${req.userId}`);
  next();
});

// Route handler.
app.post('/orders', async (req, res) => {
  // <- inspect req.body here
  const order = await orderService.create(req.body);
  res.json(order);
});
```

Debugging tip: set breakpoints on each `next()` call and the route handler to trace the full middleware execution.

---

## Debugging Async/Await

VS Code shows the async call stack — all frames up the async chain, not just the synchronous frames.

```javascript
async function processOrder(orderId) {
  const order = await fetchOrder(orderId);   // <- step into: enters fetchOrder
  const payment = await chargePayment(order); // <- step over: runs chargePayment, resumes here
  return await saveResult(order, payment);
}

async function fetchOrder(orderId) {
  // <- breakpoint here shows orderId value
  const response = await db.query('SELECT * FROM orders WHERE id = ?', [orderId]);
  return response.rows[0];
}
```

When paused inside `fetchOrder`, the async call stack shows:
```text
fetchOrder  (current frame)
processOrder (awaiting fetchOrder)
HTTP handler (awaiting processOrder)
```

Enable full async stack traces in launch.json:

```json
{
  "showAsyncStacks": true
}
```

---

## Source Maps For TypeScript

```json
{
  "name": "TypeScript: Node",
  "type": "node",
  "request": "launch",
  "runtimeArgs": ["-r", "ts-node/register"],
  "args": ["${workspaceFolder}/src/server.ts"],
  "sourceMaps": true,
  "outFiles": ["${workspaceFolder}/dist/**/*.js"],
  "skipFiles": [
    "<node_internals>/**",
    "${workspaceFolder}/node_modules/**"
  ]
}
```

With source maps, breakpoints on `.ts` files translate to the correct line in the compiled `.js`.

---

## Debugging Promise Rejection

### Enable "Uncaught Exceptions" Breakpoint

```text
Run & Debug panel -> BREAKPOINTS section
  [x] Caught Exceptions
  [x] Uncaught Exceptions  <- most important: stops at unhandled rejections
```

### Handling In Code

```javascript
// Without proper rejection handling:
async function handler(req, res) {
  const result = await dangerousOperation(); // if this rejects, where does it go?
  res.json(result);
}

// Proper rejection handling:
async function handler(req, res, next) {
  try {
    const result = await dangerousOperation();
    res.json(result);
  } catch (err) {
    next(err); // passes to Express error middleware
  }
}

// Express error handler (must have 4 params: err, req, res, next).
app.use((err, req, res, next) => {
  // <- set breakpoint here to catch all unhandled errors
  console.error(err.stack);
  res.status(500).json({ error: err.message });
});
```

### Global Unhandled Rejection Handler

```javascript
process.on('unhandledRejection', (reason, promise) => {
  // <- set breakpoint here
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});
```

---

## Debugging The debugger Statement

```javascript
// Place debugger; anywhere in Node.js code.
function calculateTotal(items) {
  debugger; // VS Code pauses here when attached
  return items.reduce((sum, item) => sum + item.price, 0);
}
```

Remove before production deployment.

---

## skipFiles — Avoid Stepping Into Internals

```json
"skipFiles": [
  "<node_internals>/**",           // Node.js core modules
  "${workspaceFolder}/node_modules/**", // all npm packages
  "${workspaceFolder}/dist/**"     // compiled output (step into source instead)
]
```

With `skipFiles`, Step Into skips over these files and lands on the next frame in your code.

---

## Conditional Breakpoints In VS Code

```text
Right-click gutter -> "Add Conditional Breakpoint"
Enter condition:
  req.body.userId === 'USR-999'
  order.total > 10000
  i === 9999
  err !== null
```

---

## Interview Sound Bite

Node.js debug uses the V8 Inspector Protocol (CDP). Launch with `--inspect` for a live attach or `--inspect-brk` to pause at startup. VS Code's async call stack shows the full await chain up through calling handlers — essential for async debugging. Enable source maps and `showAsyncStacks: true` in launch.json. For Express, set breakpoints on each `next()` call to trace middleware execution. For Promise rejections, enable the "Uncaught Exceptions" breakpoint category in the Breakpoints panel and add a global `process.on('unhandledRejection')` handler with a breakpoint.
