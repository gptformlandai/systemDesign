# 13. VS Code: Node.js Async, Concurrency, Worker Threads Debug

## Goal

Debug Node.js concurrency: worker threads, cluster processes, async stack traces, EventEmitter listener leaks, and unhandled Promise rejections.

---

## Node.js Concurrency Model

```text
Single-threaded main event loop:
  One call stack, one at a time.
  Concurrent I/O via libuv thread pool (file system, DNS, crypto).
  Timer callbacks, I/O callbacks, Promise microtasks queued and drained.

True parallelism:
  Worker Threads: separate V8 instance + event loop per worker.
  Child Processes: separate OS processes with IPC.
  Cluster: multiple OS processes sharing the same port.
```

---

## Async Stack Traces

By default, Node.js stack traces in async code are short — they only show the current async frame, not how you got there.

Enable long async stack traces:

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

Or via environment:

```bash
node --stack-trace-limit=50 src/server.js
```

### Async Stack Example

Without `showAsyncStacks`:
```text
Error: Order not found
    at OrderService.get (OrderService.js:45)
```

With `showAsyncStacks`:
```text
Error: Order not found
    at OrderService.get (OrderService.js:45)
    at async processRequest (handler.js:22)    <- async frame
    at async Express.router (router.js:8)       <- async frame
```

---

## Worker Threads Debug

Worker threads run a separate V8 engine with an independent debugger.

### Starting Worker With Debug Port

```javascript
// main.js
const { Worker } = require('worker_threads');

const worker = new Worker('./worker.js', {
  workerData: { jobId: 'JOB-001' },
  execArgv: ['--inspect=9230']  // worker listens on port 9230
});

worker.on('message', (result) => {
  console.log('Worker result:', result);
});
```

```javascript
// worker.js
const { workerData, parentPort } = require('worker_threads');

// Runs in a separate V8 instance.
// Set breakpoints here — attach VS Code to port 9230.
function processJob(jobId) {
  // <- breakpoint for worker inspection
  return { result: `processed ${jobId}` };
}

parentPort.postMessage(processJob(workerData.jobId));
```

### VS Code: Attach To Worker Thread

```json
{
  "name": "Node: Attach Worker",
  "type": "node",
  "request": "attach",
  "port": 9230,
  "skipFiles": ["<node_internals>/**"]
}
```

Start with the compound config to debug both main and worker simultaneously:

```json
"compounds": [
  {
    "name": "Node: Main + Worker",
    "configurations": ["Node: Launch Main", "Node: Attach Worker"]
  }
]
```

---

## Cluster Module Debug

Cluster spawns multiple worker processes sharing a port:

```javascript
// cluster-server.js
const cluster = require('cluster');
const http = require('http');

if (cluster.isPrimary) {
  // Primary process — fork workers.
  cluster.fork();
  cluster.fork();
} else {
  // Worker process — runs the HTTP server.
  http.createServer((req, res) => {
    // <- breakpoints here hit in one of the worker processes
    res.end(`Worker ${process.pid} handling request`);
  }).listen(3000);
}
```

To debug a cluster worker:

```bash
# Fork with different inspect ports per worker.
NODE_OPTIONS='--inspect=9229' node cluster-server.js
# Each worker claims a different port (9229, 9230, ...) automatically.
```

Attach to the specific worker port you want to debug.

---

## EventEmitter Listener Leak Detection

```javascript
const EventEmitter = require('events');
const emitter = new EventEmitter();

// Default max listeners: 10.
// Adding more than 10 for the same event prints a warning.

// Set custom limit.
emitter.setMaxListeners(20);

// Find how many listeners are attached.
console.log(emitter.listenerCount('data'));  // <- watch this in debug console
console.log(emitter.listeners('data'));       // <- inspect the actual listener functions
```

### Detecting Leaks

```javascript
// In VS Code Debug Console when paused:
emitter.eventNames()        // list all event names with listeners
emitter.listenerCount('data')  // count of 'data' listeners
emitter.listeners('data')      // array of actual listener functions

// Add breakpoint in EventEmitter.addListener to catch every registration:
// In launch.json:
// "justMyCode": false  and set breakpoint in node internals
```

---

## Unhandled Promise Rejections

```javascript
// Catch all unhandled rejections.
process.on('unhandledRejection', (reason, promise) => {
  // <- set breakpoint here in VS Code
  // reason: the error that caused the rejection
  // promise: the rejected Promise
  console.error('Unhandled rejection:', reason);
});

// Catch all uncaught exceptions.
process.on('uncaughtException', (err) => {
  // <- set breakpoint here
  console.error('Uncaught exception:', err);
  process.exit(1); // mandatory after uncaughtException
});
```

### VS Code Breakpoint Categories

```text
Run & Debug panel -> BREAKPOINTS section:
  [x] Caught Exceptions    (pauses inside try/catch for rejected promises)
  [x] Uncaught Exceptions  (pauses at unhandled rejection before process crash)
```

---

## Async Execution Order: Common Confusion

```javascript
async function example() {
  console.log('1 - sync');                         // runs first
  
  Promise.resolve().then(() => {
    console.log('3 - microtask (Promise)');         // runs third
  });
  
  setImmediate(() => {
    console.log('4 - setImmediate (check phase)');  // runs fourth
  });
  
  setTimeout(() => {
    console.log('5 - setTimeout (timers phase)');   // runs fifth (or after setImmediate)
  }, 0);
  
  process.nextTick(() => {
    console.log('2 - nextTick (before I/O)');       // runs second
  });
  
  console.log('1b - sync continued');               // still sync, runs after '1'
}
```

Set a breakpoint on each log and step through to verify execution order.

---

## --experimental-vm-modules Debug

For ESM worker debugging:

```bash
node --experimental-vm-modules --inspect=9229 server.mjs
```

---

## Interview Sound Bite

Node.js concurrency debug challenges: Worker Threads each have a separate V8 inspector — attach VS Code to the worker's debug port separately from the main process. Cluster workers also get separate debug ports (incrementing from 9229). Use `showAsyncStacks: true` in launch.json to see the full async call chain instead of just the current frame. EventEmitter listener leaks are detected by checking `listenerCount()` in the Debug Console — more than 10 listeners per event triggers a Node.js warning. `process.on('unhandledRejection')` with a breakpoint is the most reliable way to catch async errors that don't have explicit `.catch()`.
