# Node.js Async Debug Example

## What This Demonstrates

An Express server with two async bugs:
1. Fire-and-forget Promise with no `.catch()` — causes `UnhandledPromiseRejection`
2. Async error not forwarded to Express error handler — causes request to hang

## Files

- `server.js` — the buggy server
- `package.json` — dependencies

## Setup

```bash
npm install
```

## Run The Buggy Server

```bash
node server.js
```

## Trigger The Bugs

In another terminal:

```bash
# Triggers UnhandledPromiseRejection (fire-and-forget notification fails)
curl http://localhost:3000/orders/ORD-001

# Triggers hanging request (fetchOrder throws, no error handler)
curl --max-time 5 http://localhost:3000/orders/ORD-FAIL
# Times out after 5 seconds
```

## Debug With VS Code

1. Open this folder in VS Code.
2. In Run & Debug panel, enable "Uncaught Exceptions" breakpoint.
3. Launch with the included `launch.json`.
4. Trigger the bugs — VS Code pauses at `process.on('unhandledRejection')`.
5. Inspect `reason.stack` in the Variables panel.

## See The Fix

```bash
# Use the fixed route:
curl http://localhost:3000/orders/ORD-001/fixed
curl http://localhost:3000/orders/ORD-FAIL/fixed
```
