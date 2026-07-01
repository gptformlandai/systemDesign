# 24. Node.js: Event Loop, Worker Threads, Cluster Debug

## Goal

Understand and debug the Node.js event loop phases, worker thread parallelism, cluster multi-process debugging, and async task execution order.

---

## Event Loop Phases

Node.js processes events in phases, in a fixed order:

```text
┌──────────────────────────┐
│  timers                  │  setTimeout, setInterval callbacks
│  pending callbacks       │  I/O error callbacks deferred from last iteration
│  idle, prepare           │  internal use
│  poll                    │  retrieve new I/O events; execute I/O callbacks
│  check                   │  setImmediate callbacks
│  close callbacks         │  socket.on('close', ...) callbacks
└──────────────────────────┘
           └─► loops back to timers ─►
```

---

## Microtask Queue: Runs Between Phases

process.nextTick and Promise microtasks run between every phase transition:

```text
Phase ends
  -> drain process.nextTick queue completely
  -> drain Promise microtask queue completely
  -> enter next phase
```

This means nextTick/Promise.then always run before the next event loop phase.

---

## Execution Order: Live Example

```javascript
console.log('1 sync');

process.nextTick(() => console.log('3 nextTick'));

Promise.resolve().then(() => console.log('4 microtask'));

setImmediate(() => console.log('5 setImmediate (check phase)'));

setTimeout(() => console.log('6 setTimeout (timers phase)'), 0);

console.log('2 sync continued');

// Output order:
// 1 sync
// 2 sync continued
// 3 nextTick          <- nextTick drains before microtasks
// 4 microtask         <- Promise.then after nextTick
// 5 setImmediate      <- check phase
// 6 setTimeout        <- timers phase (same or next iteration)
```

### Debug This In VS Code

```javascript
// Set breakpoints on each callback.
// Use step-over to observe execution jumping between phases.
// The Debug Console shows the output in order.
```

---

## Blocking The Event Loop

```javascript
// BAD: synchronous CPU work blocks the event loop.
app.get('/orders', (req, res) => {
    const data = JSON.parse(largeJsonString);  // 500ms — blocks all requests
    res.json(data);
});

// GOOD: offload to worker thread.
const { Worker } = require('worker_threads');

app.get('/orders', (req, res) => {
    const worker = new Worker('./parseWorker.js', {
        workerData: { json: largeJsonString }
    });
    worker.once('message', (data) => res.json(data));
    worker.once('error', (err) => res.status(500).json({ error: err.message }));
});
```

---

## Worker Threads Deep Dive

Each worker thread has its own:
- V8 engine instance
- Event loop
- Debugger instance (separate CDP port)

```javascript
// main.js
const { Worker, workerData, isMainThread, parentPort } = require('worker_threads');

if (isMainThread) {
    // Main thread: start workers.
    const worker = new Worker(__filename, {
        workerData: { jobId: 'JOB-001' },
        execArgv: ['--inspect=9230']   // worker debug port
    });
    
    worker.on('message', (result) => {
        console.log('Worker result:', result);
    });
    
    worker.on('error', (err) => {
        console.error('Worker error:', err);
    });
} else {
    // Worker thread: this branch runs in the worker.
    const result = processJob(workerData.jobId);  // <- breakpoint here (attach to port 9230)
    parentPort.postMessage(result);
}
```

---

## MessageChannel For Worker Communication

```javascript
const { MessageChannel, Worker } = require('worker_threads');
const { port1, port2 } = new MessageChannel();

const worker = new Worker('./worker.js', {
    workerData: { port: port2 },
    transferList: [port2]
});

// Main thread sends via port1.
port1.postMessage({ command: 'process', data: [1, 2, 3] });

// Receive from worker via port1.
port1.on('message', (result) => {
    // <- breakpoint: inspect result from worker
    console.log('Result:', result);
});
```

---

## Cluster Debug

```javascript
const cluster = require('cluster');
const http = require('http');
const os = require('os');

if (cluster.isPrimary) {
    const cpuCount = os.cpus().length;
    console.log(`Primary ${process.pid}: forking ${cpuCount} workers`);
    
    for (let i = 0; i < cpuCount; i++) {
        cluster.fork();
    }
    
    cluster.on('exit', (worker, code) => {
        console.log(`Worker ${worker.process.pid} exited (${code}). Restarting...`);
        cluster.fork();
    });
} else {
    // Worker process — runs the server.
    http.createServer((req, res) => {
        // breakpoint here hits in ONE of the worker processes
        res.end(`Worker PID: ${process.pid}`);
    }).listen(3000);
    
    console.log(`Worker ${process.pid} started`);
}
```

### Debugging Cluster Workers

```bash
# Each worker automatically gets a debug port.
# Run primary with inspect, workers get incrementing ports.
node --inspect=9229 cluster-server.js
# Primary: port 9229
# Worker 1: port 9230
# Worker 2: port 9231
# ...

# Attach VS Code to a specific worker port.
```

```json
// launch.json
{
  "name": "Attach Cluster Worker",
  "type": "node",
  "request": "attach",
  "port": 9230   // target specific worker
}
```

---

## --inspect-brk For Startup Debug

```bash
# Pause before first line — useful for debugging startup/initialization.
node --inspect-brk=9229 server.js
# VS Code must connect before any code runs.
```

---

## Async Stack Traces In VS Code

```json
// launch.json
{
  "name": "Node: Debug",
  "type": "node",
  "request": "launch",
  "program": "${workspaceFolder}/src/server.js",
  "showAsyncStacks": true
}
```

---

## Common Event Loop Bugs

```javascript
// Bug 1: nextTick recursion starves I/O.
function recursiveTick() {
    process.nextTick(recursiveTick);  // fills nextTick queue infinitely
    // I/O callbacks never run because nextTick is always draining.
}

// Bug 2: unhandled Promise rejection kills process (Node 15+).
async function riskyOp() {
    throw new Error('failure');
}
riskyOp();  // no await, no .catch() -> unhandled rejection

// Bug 3: setImmediate vs setTimeout order is non-deterministic outside I/O callbacks.
// Inside an I/O callback: setImmediate always runs before setTimeout.
// Outside: order depends on OS timer precision.
```

---

## Interview Sound Bite

The Node.js event loop has six phases: timers → pending callbacks → idle/prepare → poll → check → close. `process.nextTick` and Promise microtasks drain between every phase — they always run before the next phase. Worker threads each have a separate V8 and debugger; attach VS Code to the worker's debug port (e.g., 9230) separately from the main thread (9229). Cluster workers also get sequential debug ports when the primary is started with `--inspect`. Blocking the event loop (synchronous CPU work) is the most common Node.js performance bug — offload to worker threads.
