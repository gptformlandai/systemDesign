# 28. Scenario: Node.js Async Promise Leak — VS Code Debug Walkthrough

## Scenario Description

An Express API server has an intermittent issue: some requests return 500 errors with the message "Cannot set headers after they are sent to the client." Other requests hang for 30 seconds and then the client gets a connection timeout. The logs show unhandled promise rejections but no clear stack trace pointing to the cause.

---

## Setup: Reproduce The Bug

```javascript
// server.js
const express = require('express');
const app = express();

app.use(express.json());

// Simulated async order service.
async function fetchOrderFromDB(orderId) {
    // Simulate DB query (200ms).
    await new Promise(resolve => setTimeout(resolve, 200));
    
    if (orderId === 'ORD-FAIL') {
        throw new Error('Order not found in database');
    }
    
    return { id: orderId, status: 'PENDING', total: 99.50 };
}

// Simulated notification service.
async function sendNotification(order) {
    await new Promise(resolve => setTimeout(resolve, 100));
    // BUG: this throws for certain order statuses.
    if (order.status !== 'CONFIRMED') {
        throw new Error('Notification service: cannot notify for non-confirmed orders');
    }
    return true;
}

// === THE BUGGY ROUTE ===
app.get('/orders/:id', async (req, res) => {
    const { id } = req.params;
    
    // Step 1: Fetch order.
    const order = await fetchOrderFromDB(id);
    
    // BUG 1: fire-and-forget without await and without .catch()
    // If sendNotification throws, nobody handles it.
    sendNotification(order);  // <- missing await AND missing .catch()
    
    // Step 2: Send response.
    res.json(order);
    
    // BUG 2: if fetchOrderFromDB throws (ORD-FAIL), async errors are not
    // passed to Express error handler — res.json never called, request hangs.
});

// === THE FIX ===
app.get('/orders/:id/fixed', async (req, res, next) => {
    try {
        const { id } = req.params;
        const order = await fetchOrderFromDB(id);
        
        // Await notification but don't fail the request if it errors.
        await sendNotification(order).catch(err => {
            console.warn('Notification failed (non-critical):', err.message);
        });
        
        res.json(order);
    } catch (err) {
        next(err);  // pass to Express error handler
    }
});

// Express error handler (4 parameters).
app.use((err, req, res, next) => {
    console.error('Request failed:', err.stack);
    res.status(500).json({ error: err.message });
});

app.listen(3000, () => console.log('Server running on port 3000'));
```

---

## Step 1: See The Errors

```bash
node server.js

# Make requests:
curl http://localhost:3000/orders/ORD-001  # returns 200 OK but...
curl http://localhost:3000/orders/ORD-FAIL  # hangs for 30s, then connection reset
```

Console output:

```text
(node:12345) UnhandledPromiseRejectionWarning: Error: Notification service: cannot notify for non-confirmed orders
    at sendNotification (server.js:17)
```

But no line indicating WHERE it was called from — the async stack is missing.

---

## Step 2: VS Code Debug — Enable Uncaught Exceptions

```text
VS Code Run & Debug panel -> BREAKPOINTS section:
  [x] Uncaught Exceptions
  [x] Caught Exceptions
```

---

## Step 3: Add process.on Handlers With Breakpoints

```javascript
// Add at top of server.js for debugging.
process.on('unhandledRejection', (reason, promise) => {
    // <- SET BREAKPOINT HERE
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

process.on('uncaughtException', (err) => {
    // <- SET BREAKPOINT HERE
    console.error('Uncaught Exception:', err);
    process.exit(1);
});
```

Launch the server in VS Code debug mode (F5).

---

## Step 4: Trigger The Bug And Hit The Breakpoint

```bash
curl http://localhost:3000/orders/ORD-001
```

VS Code pauses at `process.on('unhandledRejection')`.

### Inspect In Variables Panel

```text
Variables:
  reason  -> Error: 'Notification service: cannot notify for non-confirmed orders'
             .stack: 
               "Error: Notification service: cannot notify...\n
                at sendNotification (server.js:17:15)\n
                at /orders/:id (server.js:28:5)\n  <- THE CALLER LINE
                at Layer.handle [as handle_request] (express/lib/router/layer.js:95:5)"
  promise -> Promise { <rejected> }
```

The `.stack` in the Variables panel shows the full async stack trace — line 28 is the fire-and-forget `sendNotification(order)` call without `.catch()`.

---

## Step 5: Set Breakpoint On Line 28

```text
Set breakpoint on line:  sendNotification(order);  // the fire-and-forget line
```

Trigger again:

```text
VS Code pauses at: sendNotification(order);
Variables:
  order  -> { id: 'ORD-001', status: 'PENDING', total: 99.50 }
```

Step Over → sendNotification starts running but the result is discarded. No `.catch()` means the rejection bubbles to `unhandledRejection`.

---

## Step 6: Debug The Hanging Request (ORD-FAIL)

```bash
curl http://localhost:3000/orders/ORD-FAIL &
```

No response. Set breakpoint inside the route handler and check the async call stack.

The `fetchOrderFromDB` throws, but there is no `try/catch` in the route handler and `next(err)` is never called. Express gets no signal to close the response, so it waits until the client times out.

---

## Step 7: Apply The Fix And Verify

```text
Use the /orders/:id/fixed route instead.

Test:
  curl http://localhost:3000/orders/ORD-001/fixed
  -> 200 OK, returns order, console shows: "Notification failed (non-critical): ..."
  
  curl http://localhost:3000/orders/ORD-FAIL/fixed
  -> 500 {"error":"Order not found in database"}  (immediately, no hang)
```

---

## Key Takeaways

```text
Promise leak checklist:
  1. Console shows UnhandledPromiseRejectionWarning -> find the fire-and-forget.
  2. Request hangs indefinitely -> async error not passed to next(err).
  3. VS Code: enable Uncaught Exceptions breakpoint category.
  4. Add process.on('unhandledRejection') handler with breakpoint.
  5. Read reason.stack in Variables panel: shows the full async call chain.
  6. Fix: always await async calls, always add .catch() for non-critical ones,
     always wrap Express async routes in try/catch with next(err).

Express async pattern (wrap every async route):
  const asyncHandler = fn => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
  app.get('/orders/:id', asyncHandler(async (req, res) => { ... }));
```

---

## Interview Sound Bite

Node.js async bugs show as `UnhandledPromiseRejectionWarning` — a fire-and-forget Promise with no `.catch()`. VS Code's "Uncaught Exceptions" breakpoint category catches these at the throw site. The async stack trace in `reason.stack` shows the full call chain from the throw site back to the caller. Hanging Express requests are caused by async errors that never call `next(err)` — the response is never closed. The fix pattern: wrap all async Express routes in `try/catch` with `next(err)` in the catch block, and always add `.catch()` to fire-and-forget Promises.
