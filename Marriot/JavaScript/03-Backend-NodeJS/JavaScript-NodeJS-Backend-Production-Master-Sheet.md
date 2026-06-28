# JavaScript Node.js Backend Production Master Sheet

Target: JavaScript, Node.js, backend, full-stack, platform, and MAANG interviews where you must explain Node runtime behavior, build APIs, prevent event-loop blocking, handle production failures, and design reliable backend services.

This sheet covers:
- Node.js runtime mental model
- V8, libuv, event loop, and thread pool
- CommonJS and ES modules
- Package.json and dependency management
- HTTP servers and API design
- Express/Fastify-style middleware thinking
- Request parsing and validation
- Error handling and async boundaries
- Streams, buffers, and backpressure
- Filesystem and network IO
- EventEmitter patterns
- Timers and scheduling
- Worker threads and child processes
- Clustering and horizontal scaling
- Event-loop blocking diagnosis
- Memory leaks and heap debugging
- Database access and connection pools
- Timeouts, retries, cancellation, and idempotency
- Logging, metrics, tracing, and health checks
- Security basics for Node services
- Production deployment checklist
- Interview scenarios and strong answers

How to use this:
- Learn the mental model first: Node is single-threaded for JavaScript execution but not single-threaded for all work.
- Practice explaining where work runs: JS call stack, event loop, OS async IO, libuv thread pool, workers, external services.
- For every API design, ask: What can block? What can retry? What can leak? What can overload? What can be abused?
- Treat this as a production reasoning sheet, not just a syntax sheet.

---

## 1. Node.js In One Line

Definition:

```text
Node.js is a JavaScript runtime built on V8 that lets JavaScript run outside the browser, with
APIs for servers, files, streams, networking, processes, crypto, and operating system integration.
```

Interview line:

```text
Node is excellent for IO-heavy services because it uses non-blocking IO and an event loop, but
CPU-heavy JavaScript can block the event loop unless moved to workers, child processes, or external systems.
```

Production mindset:

```text
Node performance depends less on raw threads and more on keeping the event loop free, bounding
concurrency, respecting backpressure, and handling failures explicitly.
```

---

## 2. Backend Mental Model

A Node backend service usually does this loop:

```text
1. Accept request.
2. Parse and validate input.
3. Authenticate and authorize.
4. Call dependencies: database, cache, queue, external API, filesystem.
5. Transform response.
6. Log, trace, and emit metrics.
7. Return response or error.
```

A production request is not just function calls. It has:

- Latency budget.
- Timeout budget.
- Memory cost.
- Connection pool cost.
- Retry behavior.
- Auth context.
- Request ID / trace ID.
- Failure mode.
- Observability signal.

Strong line:

```text
For backend Node, I think in terms of request lifecycle, dependency calls, event-loop health,
resource limits, and failure boundaries.
```

---

## 3. Node Runtime Architecture

Main pieces:

| Piece | Role |
|---|---|
| V8 | Executes JavaScript and manages JS heap |
| Node core APIs | Provides fs, http, stream, crypto, net, timers, process, buffer |
| libuv | Event loop, async IO abstraction, thread pool |
| OS kernel | Sockets, file descriptors, scheduling, networking |
| C/C++ bindings | Bridge between JS APIs and native capabilities |
| npm ecosystem | Packages and tooling around Node |

Simplified flow:

```text
JavaScript code -> Node API -> native binding/libuv -> OS or thread pool -> callback/promise -> event loop -> JavaScript continuation
```

Important distinction:

```text
JavaScript execution is on the main thread by default. Some IO and native work can happen outside
that thread, but the callback that handles the result still runs on the main JS thread.
```

---

## 4. Node Is Not Fully Single-Threaded

Common misconception:

```text
Node is single-threaded.
```

Better answer:

```text
Node runs JavaScript on a main thread by default, but the runtime also uses the OS and libuv thread
pool for certain asynchronous operations. Node can also use worker_threads and child processes.
```

Examples:

| Work | Where It Happens |
|---|---|
| JS function execution | Main JS thread |
| Network socket readiness | OS event notification / libuv |
| Some filesystem calls | libuv thread pool |
| Some crypto/compression/DNS | libuv thread pool |
| Worker thread JS | Separate worker thread |
| Child process work | Separate process |

Strong answer:

```text
Node is single-threaded for normal JavaScript execution, but not for the whole runtime. The key
production concern is that CPU-heavy JS on the main thread blocks all requests handled by that process.
```

---

## 5. Event Loop Review For Backend

The event loop lets Node handle many concurrent IO operations without one thread per request.

Simplified phases:

```text
Timers -> pending callbacks -> idle/prepare -> poll -> check -> close callbacks
```

Microtasks:

```text
Promise callbacks and queueMicrotask run between macrotask boundaries. process.nextTick has its
own high-priority queue in Node and can starve the event loop if abused.
```

Example:

```javascript
console.log("start");

setTimeout(() => console.log("timeout"), 0);
setImmediate(() => console.log("immediate"));

Promise.resolve().then(() => console.log("promise"));
process.nextTick(() => console.log("nextTick"));

console.log("end");
```

Typical output shape:

```text
start
end
nextTick
promise
...timeout/immediate ordering depends on context...
```

Interview point:

```text
For backend systems, event-loop knowledge matters because one slow synchronous task can delay
unrelated requests, timers, incoming sockets, and resolved promises in the same process.
```

---

## 6. Event-Loop Blocking

Event-loop blocking means the main JS thread is busy and cannot process other callbacks.

Bad:

```javascript
import http from "node:http";

function slowCpuWork() {
    let total = 0;

    for (let index = 0; index < 5_000_000_000; index++) {
        total += index;
    }

    return total;
}

http.createServer((request, response) => {
    if (request.url === "/slow") {
        response.end(String(slowCpuWork()));
        return;
    }

    response.end("ok");
}).listen(3000);
```

Impact:

```text
While /slow runs, the process cannot respond quickly to /health, /login, /orders, timers, or
resolved IO callbacks.
```

Symptoms:

- High latency for all routes.
- Health checks fail under CPU work.
- Event-loop delay increases.
- CPU is high but throughput drops.
- P99 latency spikes.

Fix options:

- Move CPU work to worker_threads.
- Use child processes.
- Use a job queue and workers.
- Use database/search engine for heavy queries.
- Chunk work and yield if approximate/intermediate work is acceptable.
- Optimize algorithm and data structure.

Strong answer:

```text
Node can handle many concurrent IO requests, but synchronous CPU work blocks the event loop. I
would measure event-loop delay and move CPU-heavy work away from the main request thread.
```

---

## 7. Measuring Event-Loop Delay

Node provides `perf_hooks`.

```javascript
import { monitorEventLoopDelay } from "node:perf_hooks";

const histogram = monitorEventLoopDelay({ resolution: 20 });
histogram.enable();

setInterval(() => {
    const p99Ms = histogram.percentile(99) / 1_000_000;
    console.log({ eventLoopDelayP99Ms: Number(p99Ms.toFixed(2)) });
    histogram.reset();
}, 10_000);
```

Production use:

- Export event-loop lag as a metric.
- Alert when p95/p99 delay crosses threshold.
- Correlate with CPU, GC, request latency, and deployments.
- Use profiling to find blocking code.

Strong line:

```text
Event-loop delay is the backend Node equivalent of input responsiveness: it tells whether the
process can promptly handle callbacks and requests.
```

---

## 8. CommonJS Modules

CommonJS is the classic Node module system.

Export:

```javascript
function calculateTotal(items) {
    return items.reduce((sum, item) => sum + item.price, 0);
}

module.exports = { calculateTotal };
```

Import:

```javascript
const { calculateTotal } = require("./pricing");
```

Characteristics:

- Synchronous loading.
- `require` can be called conditionally.
- Modules are cached after first load.
- Uses `module.exports` and `exports`.
- Common in older Node projects and many packages.

Trap:

```javascript
exports = { calculateTotal }; // wrong for replacing export object
```

Better:

```javascript
module.exports = { calculateTotal };
```

Strong answer:

```text
CommonJS is synchronous and dynamic, which fit early Node server usage. It remains common, but
modern Node supports ESM as well.
```

---

## 9. ES Modules In Node

ES modules are the standard JavaScript module system.

Export:

```javascript
export function calculateTotal(items) {
    return items.reduce((sum, item) => sum + item.price, 0);
}
```

Import:

```javascript
import { calculateTotal } from "./pricing.js";
```

How Node decides ESM:

```json
{
  "type": "module"
}
```

Or file extensions:

```text
.mjs -> ESM
.cjs -> CommonJS
```

Key differences:

| Topic | CommonJS | ESM |
|---|---|---|
| Import syntax | `require` | `import` |
| Export syntax | `module.exports` | `export` |
| Loading | Synchronous style | Static, async-capable |
| Top-level await | No | Yes |
| `__dirname` | Available | Not directly available |
| Tree-shaking | Harder | Better for tooling |

ESM dirname replacement:

```javascript
import { fileURLToPath } from "node:url";
import { dirname } from "node:path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
```

Strong answer:

```text
ESM is the standard module system and supports static analysis and top-level await, but Node code
must handle interop carefully when mixing CommonJS and ESM packages.
```

---

## 10. Package.json Production Fields

Example:

```json
{
  "name": "booking-api",
  "version": "1.0.0",
  "type": "module",
  "main": "src/server.js",
  "scripts": {
    "start": "node src/server.js",
    "test": "vitest run",
    "lint": "eslint ."
  },
  "engines": {
    "node": ">=20"
  },
  "dependencies": {
    "express": "^4.18.3"
  },
  "devDependencies": {
    "vitest": "^2.0.0"
  }
}
```

Production notes:

- Pin supported Node version.
- Separate dependencies from devDependencies.
- Use lockfiles.
- Avoid running production with unknown Node major versions.
- Use `npm ci` in CI for reproducible installs.
- Audit dependencies but do not blindly upgrade without tests.

Strong answer:

```text
For production Node, package.json is part of the runtime contract: module type, entrypoint,
Node engine, scripts, and dependency boundaries all affect deploy behavior.
```

---

## 11. Dependency Hygiene

Production risk from dependencies:

- Supply-chain compromise.
- Vulnerable transitive package.
- Unmaintained package.
- Huge dependency for tiny utility.
- ESM/CommonJS incompatibility.
- License risk.
- Postinstall scripts.

Good habits:

```text
Use lockfiles, review package health, minimize dependencies, scan vulnerabilities, pin Node
versions, and update with tests.
```

Command examples:

```bash
npm ci
npm audit
npm outdated
npm ls package-name
```

Interview line:

```text
In Node, dependency management is a production security topic, not just a convenience topic.
```

---

## 12. Minimal HTTP Server

Node can create an HTTP server without frameworks.

```javascript
import http from "node:http";

const server = http.createServer((request, response) => {
    if (request.method === "GET" && request.url === "/health") {
        response.writeHead(200, { "Content-Type": "application/json" });
        response.end(JSON.stringify({ status: "ok" }));
        return;
    }

    response.writeHead(404, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ error: "not_found" }));
});

server.listen(3000, () => {
    console.log("server listening on port 3000");
});
```

What frameworks add:

- Routing.
- Middleware.
- Body parsing.
- Error handling conventions.
- Request/response helpers.
- Plugin ecosystems.
- Validation integration.

Strong answer:

```text
Frameworks make API development ergonomic, but understanding the raw Node HTTP server helps explain
request streams, response writing, headers, timeouts, and backpressure.
```

---

## 13. Request And Response Are Streams

In Node HTTP:

```text
Incoming request body is a readable stream.
Outgoing response is a writable stream.
```

Manual body parsing:

```javascript
async function readJsonBody(request, limitBytes = 1_000_000) {
    let size = 0;
    const chunks = [];

    for await (const chunk of request) {
        size += chunk.length;

        if (size > limitBytes) {
            throw new Error("payload_too_large");
        }

        chunks.push(chunk);
    }

    const raw = Buffer.concat(chunks).toString("utf8");
    return JSON.parse(raw);
}
```

Why limit matters:

```text
Without body size limits, attackers or buggy clients can force your server to allocate too much memory.
```

Strong answer:

```text
Request bodies are streams, so production APIs must enforce body limits, parse carefully, and avoid
assuming the full body is instantly available.
```

---

## 14. API Route Design

Good API routes are predictable.

Example resource routes:

```text
GET    /bookings
GET    /bookings/:id
POST   /bookings
PATCH  /bookings/:id
DELETE /bookings/:id
```

Guidelines:

- Use nouns for resources.
- Use HTTP methods for actions.
- Use status codes consistently.
- Validate input at boundaries.
- Return stable error shapes.
- Include pagination for lists.
- Avoid unbounded result sets.
- Make writes idempotent where required.

Status code examples:

| Code | Meaning |
|---:|---|
| 200 | Successful GET/PATCH |
| 201 | Resource created |
| 204 | Success with no body |
| 400 | Invalid request syntax/input |
| 401 | Not authenticated |
| 403 | Authenticated but not allowed |
| 404 | Resource not found |
| 409 | Conflict |
| 422 | Semantically invalid input |
| 429 | Rate limited |
| 500 | Unexpected server error |
| 503 | Service unavailable |

Strong answer:

```text
I design APIs around resources, explicit validation, stable error shapes, bounded lists, and
correct status codes so clients can handle failures predictably.
```

---

## 15. Middleware Mental Model

Middleware is a pipeline around request handling.

```text
request -> middleware 1 -> middleware 2 -> route handler -> error handler -> response
```

Typical middleware responsibilities:

- Request ID.
- Logging.
- CORS.
- Security headers.
- Body parsing.
- Authentication.
- Rate limiting.
- Validation.
- Error handling.

Express-style shape:

```javascript
function requestLogger(request, response, next) {
    const start = Date.now();

    response.on("finish", () => {
        console.log({
            method: request.method,
            path: request.path,
            status: response.statusCode,
            durationMs: Date.now() - start
        });
    });

    next();
}
```

Strong answer:

```text
Middleware is powerful for cross-cutting concerns, but order matters. Auth, parsing, rate limits,
and error handlers must be placed intentionally.
```

---

## 16. Async Error Handling Trap

Bad Express-style async handler:

```javascript
app.get("/bookings/:id", async (request, response) => {
    const booking = await loadBooking(request.params.id);
    response.json(booking);
});
```

Depending framework/version, rejected promises may need explicit forwarding.

Wrapper pattern:

```javascript
function asyncHandler(handler) {
    return function wrapped(request, response, next) {
        Promise.resolve(handler(request, response, next)).catch(next);
    };
}

app.get("/bookings/:id", asyncHandler(async (request, response) => {
    const booking = await loadBooking(request.params.id);
    response.json(booking);
}));
```

Central error handler:

```javascript
app.use((error, request, response, next) => {
    request.log?.error({ error }, "request failed");

    response.status(error.statusCode ?? 500).json({
        error: error.code ?? "internal_error",
        message: error.expose ? error.message : "Something went wrong"
    });
});
```

Strong answer:

```text
Async route errors need a reliable path to the central error handler. I avoid unhandled rejections
and keep error responses stable and non-leaky.
```

---

## 17. Stable Error Shape

Good error response:

```json
{
  "error": "validation_failed",
  "message": "Invalid request",
  "details": [
    { "field": "email", "message": "Email is required" }
  ],
  "requestId": "req_123"
}
```

Why it matters:

- Clients can show correct UI.
- Logs can be correlated with request ID.
- Tests can assert error codes.
- Internal stack traces are not leaked.
- Support teams can debug incidents.

Do not expose:

- Database credentials.
- SQL queries with sensitive data.
- Stack traces to users.
- Full dependency error objects.
- Tokens or secrets.

Strong answer:

```text
I return stable machine-readable error codes to clients and log detailed internal errors server-side
with request IDs.
```

---

## 18. Input Validation At The Boundary

Never trust incoming data.

Validation should happen at:

- HTTP body.
- Query params.
- Route params.
- Headers.
- Message queue payloads.
- Webhook payloads.
- Environment variables.

Plain validation example:

```javascript
function parseCreateBooking(input) {
    if (!input || typeof input !== "object") {
        throw badRequest("body must be an object");
    }

    if (typeof input.guestName !== "string" || input.guestName.trim() === "") {
        throw badRequest("guestName is required");
    }

    if (typeof input.roomId !== "string") {
        throw badRequest("roomId is required");
    }

    return {
        guestName: input.guestName.trim(),
        roomId: input.roomId
    };
}
```

Schema libraries:

```text
Zod, Joi, Yup, Valibot, Ajv, TypeBox, OpenAPI validators.
```

Strong answer:

```text
TypeScript does not validate runtime input. Node APIs need runtime validation at every external boundary.
```

---

## 19. Environment Configuration

Bad:

```javascript
const databaseUrl = process.env.DATABASE_URL;
connect(databaseUrl);
```

If missing, the service may fail later with confusing errors.

Better:

```javascript
function requireEnv(name) {
    const value = process.env[name];

    if (!value) {
        throw new Error(`Missing required env var: ${name}`);
    }

    return value;
}

export const config = {
    nodeEnv: process.env.NODE_ENV ?? "development",
    port: Number(process.env.PORT ?? 3000),
    databaseUrl: requireEnv("DATABASE_URL"),
    jwtSecret: requireEnv("JWT_SECRET")
};
```

Production rules:

- Validate env vars at startup.
- Fail fast if required config is missing.
- Do not log secrets.
- Keep config typed and centralized.
- Separate build-time config from runtime config.

Strong answer:

```text
I validate configuration on startup so misconfigured services fail fast before accepting traffic.
```

---

## 20. Buffers

Buffer is Node's binary data type.

```javascript
const buffer = Buffer.from("hello", "utf8");
console.log(buffer.toString("hex"));
console.log(buffer.toString("utf8"));
```

Used for:

- File data.
- Network packets.
- Binary protocols.
- Crypto operations.
- Streams.
- Image/file uploads.

Important:

```text
Buffers allocate memory outside normal JS object shapes and can contribute to process memory usage.
```

Safe allocation:

```javascript
const safe = Buffer.alloc(1024);
```

Unsafe allocation caution:

```javascript
const fastButUninitialized = Buffer.allocUnsafe(1024);
```

Strong answer:

```text
Buffer represents raw binary data in Node. I use it for files, streams, crypto, and network data,
and I avoid unsafe allocation unless I immediately overwrite the contents.
```

---

## 21. Streams

Streams process data piece by piece.

Types:

| Stream | Meaning |
|---|---|
| Readable | Source of data |
| Writable | Destination for data |
| Duplex | Both readable and writable |
| Transform | Modifies data while passing through |

Why streams matter:

```text
They avoid loading entire large files or responses into memory.
```

Bad large file response:

```javascript
import { readFile } from "node:fs/promises";

app.get("/download", async (request, response) => {
    const file = await readFile("large-report.csv");
    response.send(file);
});
```

Better:

```javascript
import { createReadStream } from "node:fs";

app.get("/download", (request, response) => {
    response.setHeader("Content-Type", "text/csv");
    createReadStream("large-report.csv").pipe(response);
});
```

Strong answer:

```text
Streams are essential for large payloads because they reduce memory pressure and support backpressure.
```

---

## 22. Backpressure

Backpressure means the consumer cannot keep up with the producer.

Example:

```text
Disk reads faster than network response can send to a slow client.
```

If ignored:

- Memory grows.
- Process can crash.
- Latency spikes.
- GC pressure increases.

Pipeline handles errors and backpressure:

```javascript
import { pipeline } from "node:stream/promises";
import { createReadStream } from "node:fs";
import { createGzip } from "node:zlib";

app.get("/report.gz", async (request, response, next) => {
    try {
        response.setHeader("Content-Encoding", "gzip");
        await pipeline(
            createReadStream("report.csv"),
            createGzip(),
            response
        );
    } catch (error) {
        next(error);
    }
});
```

Strong answer:

```text
Backpressure prevents producers from overwhelming consumers. In Node, streams and pipeline help
manage it safely.
```

---

## 23. Filesystem IO

Node has callback, promise, and stream filesystem APIs.

Promise API:

```javascript
import { readFile } from "node:fs/promises";

const config = JSON.parse(await readFile("config.json", "utf8"));
```

Stream API for large files:

```javascript
import { createReadStream } from "node:fs";

const stream = createReadStream("large.log", { encoding: "utf8" });

for await (const chunk of stream) {
    process.stdout.write(chunk);
}
```

Production cautions:

- Avoid synchronous fs calls in request path.
- Enforce upload limits.
- Sanitize file paths.
- Do not trust user-provided filenames.
- Stream large files.
- Handle errors and cleanup temp files.

Strong answer:

```text
Small startup reads can be fine, but request-path filesystem work should be async or streamed,
and user-controlled paths must be validated carefully.
```

---

## 24. Path Traversal Risk

Bad:

```javascript
app.get("/files/:name", (request, response) => {
    response.sendFile(`/app/uploads/${request.params.name}`);
});
```

Attack:

```text
GET /files/../../etc/passwd
```

Safer pattern:

```javascript
import path from "node:path";

const uploadRoot = path.resolve("/app/uploads");

function resolveUploadPath(fileName) {
    const candidate = path.resolve(uploadRoot, fileName);

    if (!candidate.startsWith(uploadRoot + path.sep)) {
        throw new Error("invalid_path");
    }

    return candidate;
}
```

Strong answer:

```text
Any file path derived from user input must be normalized and constrained to an allowed directory.
```

---

## 25. EventEmitter

EventEmitter is a common Node pattern.

```javascript
import { EventEmitter } from "node:events";

class BookingEvents extends EventEmitter {}

const events = new BookingEvents();

events.on("booking.created", booking => {
    console.log("send email", booking.id);
});

events.emit("booking.created", { id: "b1" });
```

Production cautions:

- Add error listeners.
- Avoid unbounded listener growth.
- Remove listeners on cleanup.
- Know sync vs async behavior.
- Do not use in-process events as durable queues.

Error event trap:

```javascript
events.on("error", error => {
    console.error(error);
});
```

Strong answer:

```text
EventEmitter is useful for in-process events, but it is not durable. For cross-service or reliable
async work, use a queue or event broker.
```

---

## 26. Timers In Production

Timers:

```javascript
setTimeout(() => runOnce(), 1000);
setInterval(() => refreshCache(), 60_000);
setImmediate(() => afterCurrentPollPhase());
```

Cautions:

- Timers are not exact under event-loop delay.
- setInterval can overlap async work.
- Timers must be cleared on shutdown.
- Do not use in-process timers as reliable schedulers across replicas.

Bad overlapping interval:

```javascript
setInterval(async () => {
    await refreshCache();
}, 1000);
```

If refresh takes 5 seconds, calls overlap.

Better:

```javascript
let stopped = false;

async function loop() {
    while (!stopped) {
        await refreshCache();
        await delay(1000);
    }
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
```

Strong answer:

```text
For recurring async work, I avoid overlapping setInterval and use controlled loops, queues, or
external schedulers depending on reliability needs.
```

---

## 27. AbortController For Cancellation

Node supports AbortController in many APIs.

```javascript
const controller = new AbortController();
const timeoutId = setTimeout(() => controller.abort(), 3000);

try {
    const response = await fetch("https://api.example.com/data", {
        signal: controller.signal
    });

    console.log(await response.json());
} finally {
    clearTimeout(timeoutId);
}
```

Reusable timeout helper:

```javascript
async function fetchWithTimeout(url, options = {}, timeoutMs = 3000) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
        return await fetch(url, {
            ...options,
            signal: controller.signal
        });
    } finally {
        clearTimeout(timeoutId);
    }
}
```

Strong answer:

```text
Cancellation is part of backend resource control. Timeouts without cancellation may leave work
running after the caller already gave up.
```

---

## 28. Timeouts

Every dependency call should have a timeout.

Examples:

- External HTTP calls.
- Database queries.
- Cache operations.
- Queue publishes.
- File operations where supported.
- Request body read.

Why:

```text
Without timeouts, one slow dependency can tie up sockets, memory, promises, and user requests indefinitely.
```

Layered timeout model:

```text
Client timeout > Gateway timeout > Service request timeout > Dependency timeout
```

Strong answer:

```text
Timeouts should be shorter at deeper dependency levels so the service has time to handle failure
and return a controlled response before the upstream caller gives up.
```

---

## 29. Retries

Retries help transient failures but can amplify outages.

Retry only when:

- Operation is idempotent or protected by idempotency key.
- Error is likely transient.
- Retry budget is bounded.
- Backoff and jitter are used.

Bad:

```javascript
while (true) {
    await callPaymentApi(payload);
}
```

Better:

```javascript
async function retry(operation, { attempts = 3, baseDelayMs = 100 } = {}) {
    let lastError;

    for (let attempt = 1; attempt <= attempts; attempt++) {
        try {
            return await operation();
        } catch (error) {
            lastError = error;

            if (attempt === attempts) {
                break;
            }

            const delayMs = baseDelayMs * 2 ** (attempt - 1) + Math.random() * 50;
            await delay(delayMs);
        }
    }

    throw lastError;
}
```

Strong answer:

```text
Retries need backoff, jitter, retry limits, and idempotency. Otherwise they can turn a small outage
into a retry storm.
```

---

## 30. Idempotency

Idempotency means repeating the same operation has the same effect as doing it once.

Safe/idempotent HTTP examples:

| Method | Usually Idempotent? | Note |
|---|---:|---|
| GET | Yes | Should not mutate server state |
| PUT | Yes | Replace resource with same representation |
| DELETE | Yes | Deleting already-deleted resource has same final state |
| POST | Not by default | Can be made safe with idempotency keys |

Payment/order protection:

```javascript
app.post("/payments", async (request, response) => {
    const key = request.header("Idempotency-Key");

    if (!key) {
        response.status(400).json({ error: "idempotency_key_required" });
        return;
    }

    const result = await createPaymentOnce(key, request.body);
    response.status(201).json(result);
});
```

Strong answer:

```text
For non-idempotent writes, especially payments and orders, retries must be protected by server-side
idempotency keys.
```

---

## 31. Unbounded Concurrency

Bad:

```javascript
await Promise.all(userIds.map(id => sendEmail(id)));
```

If `userIds` has 100,000 entries, this can overload the service, email provider, memory, and network.

Concurrency limiter:

```javascript
async function mapWithConcurrency(items, limit, mapper) {
    const results = new Array(items.length);
    let nextIndex = 0;

    async function worker() {
        while (nextIndex < items.length) {
            const index = nextIndex++;
            results[index] = await mapper(items[index], index);
        }
    }

    await Promise.all(
        Array.from({ length: Math.min(limit, items.length) }, () => worker())
    );

    return results;
}
```

Strong answer:

```text
Promise.all is dangerous for unbounded input. Production Node code should bound concurrency to
protect memory, downstream dependencies, and its own event loop.
```

---

## 32. Database Connection Pools

A pool reuses database connections.

Why pooling exists:

- Opening DB connections is expensive.
- Databases have connection limits.
- Reuse improves latency.
- Pool size controls concurrency pressure.

Bad pattern:

```javascript
app.get("/bookings", async (request, response) => {
    const client = await createNewDatabaseConnection();
    const rows = await client.query("select * from bookings");
    response.json(rows);
});
```

Better pattern:

```javascript
const pool = createPool({
    connectionString: config.databaseUrl,
    max: 10
});

app.get("/bookings", async (request, response) => {
    const rows = await pool.query("select * from bookings limit 100");
    response.json(rows);
});
```

Production cautions:

- Pool max should match database capacity.
- Release clients in finally.
- Set query timeouts.
- Avoid connection leaks.
- Use pagination.
- Monitor pool wait time.

Strong answer:

```text
A database pool is a backpressure mechanism. Too small causes queueing; too large can overload
the database.
```

---

## 33. SQL Injection Risk

Bad:

```javascript
const sql = `select * from users where email = '${request.query.email}'`;
await db.query(sql);
```

Safe parameterized query:

```javascript
await db.query(
    "select * from users where email = $1",
    [request.query.email]
);
```

Strong answer:

```text
Never concatenate untrusted input into SQL. Use parameterized queries or a safe query builder.
```

---

## 34. Caching In Node Services

Cache candidates:

- Read-heavy reference data.
- Expensive external API responses.
- Feature flags/config snapshots.
- Auth public keys/JWKS.
- Computed reports.

Cache risks:

- Stale data.
- Memory growth.
- Cache stampede.
- Tenant/user data leakage.
- Missing invalidation.

Simple TTL cache:

```javascript
class TtlCache {
    constructor() {
        this.items = new Map();
    }

    get(key) {
        const entry = this.items.get(key);

        if (!entry || entry.expiresAt <= Date.now()) {
            this.items.delete(key);
            return undefined;
        }

        return entry.value;
    }

    set(key, value, ttlMs) {
        this.items.set(key, {
            value,
            expiresAt: Date.now() + ttlMs
        });
    }
}
```

Strong answer:

```text
Caching improves latency and reduces load, but production caches need TTLs, invalidation strategy,
size bounds, and tenant-safe keys.
```

---

## 35. Worker Threads

Use worker_threads for CPU-heavy JavaScript inside the same process family.

Main thread:

```javascript
import { Worker } from "node:worker_threads";

function runWorker(input) {
    return new Promise((resolve, reject) => {
        const worker = new Worker(new URL("./worker.js", import.meta.url), {
            workerData: input
        });

        worker.once("message", resolve);
        worker.once("error", reject);
        worker.once("exit", code => {
            if (code !== 0) {
                reject(new Error(`worker stopped with code ${code}`));
            }
        });
    });
}
```

Worker:

```javascript
import { parentPort, workerData } from "node:worker_threads";

function expensiveCalculation(value) {
    let total = 0;

    for (let index = 0; index < value.iterations; index++) {
        total += index;
    }

    return total;
}

parentPort.postMessage(expensiveCalculation(workerData));
```

Use for:

- CPU-heavy parsing.
- Image processing.
- Compression if not using native async APIs.
- Cryptographic work if appropriate.
- Large data transformations.

Cautions:

- Serialization cost.
- Memory overhead.
- Worker pool management.
- Error handling.
- Timeouts/cancellation.

Strong answer:

```text
Worker threads are useful when CPU-heavy JavaScript would block the event loop, but they are not
a replacement for external job systems when work must be durable or distributed.
```

---

## 36. Child Processes

Child processes run separate OS processes.

```javascript
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

const { stdout } = await execFileAsync("node", ["--version"], {
    timeout: 3000
});

console.log(stdout);
```

Use cases:

- Running external binaries.
- Process isolation.
- Legacy scripts.
- CPU-heavy work with separate memory.

Security cautions:

- Avoid shell interpolation with untrusted input.
- Prefer `execFile` over shell `exec` when possible.
- Set timeouts.
- Limit output buffer.
- Validate arguments.

Strong answer:

```text
Child processes provide isolation but add process overhead. For untrusted arguments, avoid shell
string construction and use execFile/spawn with validated args.
```

---

## 37. Cluster And Horizontal Scaling

One Node process uses one main JS thread. To use multiple CPU cores, run multiple processes.

Options:

- Node cluster module.
- Process manager.
- Containers with multiple replicas.
- Kubernetes deployments.
- Load balancer across instances.

Production reality:

```text
Modern production often scales Node with multiple container replicas rather than manual cluster
logic inside the app.
```

Important:

- Do not store critical session state only in process memory.
- Use external session/cache/store if multiple replicas need shared state.
- Health checks and graceful shutdown matter.
- Sticky sessions may be needed for some WebSocket designs.

Strong answer:

```text
Node services scale horizontally by running multiple processes/replicas. This means application
state must be externalized or designed to tolerate per-process isolation.
```

---

## 38. Graceful Shutdown

Why:

```text
When a container/process receives SIGTERM, it should stop accepting new work, finish in-flight
requests if possible, close resources, and exit before the platform kills it.
```

Example:

```javascript
const server = app.listen(config.port);

async function shutdown(signal) {
    console.log({ signal }, "shutting down");

    server.close(async error => {
        if (error) {
            console.error(error);
            process.exit(1);
        }

        try {
            await pool.end();
            process.exit(0);
        } catch (closeError) {
            console.error(closeError);
            process.exit(1);
        }
    });

    setTimeout(() => {
        console.error("forced shutdown");
        process.exit(1);
    }, 30_000).unref();
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
```

Strong answer:

```text
Graceful shutdown protects users and data during deploys by draining requests and closing resources
before the process exits.
```

---

## 39. Health Checks

Types:

| Check | Meaning |
|---|---|
| Liveness | Is process alive? |
| Readiness | Can process receive traffic? |
| Startup | Has app finished initialization? |

Example:

```javascript
app.get("/health/live", (request, response) => {
    response.json({ status: "alive" });
});

app.get("/health/ready", async (request, response) => {
    const databaseOk = await checkDatabase();

    if (!databaseOk) {
        response.status(503).json({ status: "not_ready" });
        return;
    }

    response.json({ status: "ready" });
});
```

Caution:

```text
Readiness should not overload dependencies. Cache check results briefly if checks are expensive.
```

Strong answer:

```text
Liveness says whether to restart the process. Readiness says whether to send it traffic. They are
not the same.
```

---

## 40. Logging

Good structured log:

```javascript
logger.info({
    requestId,
    userId,
    route: request.route?.path,
    statusCode: response.statusCode,
    durationMs
}, "request completed");
```

Avoid:

- Tokens.
- Passwords.
- Full request bodies with PII.
- Secrets.
- Huge payloads.
- Console-only thinking in production.

Log levels:

| Level | Use |
|---|---|
| debug | Local/deep troubleshooting |
| info | Normal lifecycle events |
| warn | Unexpected but handled |
| error | Failure needing attention |
| fatal | Process cannot continue |

Strong answer:

```text
Production logs should be structured, correlated by request ID, and safe from secret/PII leakage.
```

---

## 41. Metrics

Useful Node service metrics:

- Request count by route/status.
- Request latency p50/p95/p99.
- Error rate.
- Event-loop delay.
- Heap used and RSS.
- GC pauses if available.
- Active handles/connections.
- Database pool usage/wait time.
- External dependency latency/errors.
- Queue depth/lag.

Strong answer:

```text
Metrics tell whether the service is healthy at scale. For Node, event-loop delay and memory are
especially important runtime signals.
```

---

## 42. Tracing

Tracing follows a request across services.

Trace context:

```text
request -> API gateway -> Node service -> database -> payment service -> queue
```

What to include:

- Trace ID.
- Span names.
- Route and method.
- Dependency call durations.
- Error status.
- Retry attempts.
- Tenant/user category when safe.

Strong answer:

```text
Logs explain what happened in one service. Traces explain where time and failure occurred across
a distributed request path.
```

---

## 43. Authentication And Authorization

Authentication:

```text
Who are you?
```

Authorization:

```text
What are you allowed to do?
```

Middleware shape:

```javascript
function requireUser(request, response, next) {
    const token = extractBearerToken(request.headers.authorization);

    if (!token) {
        response.status(401).json({ error: "unauthenticated" });
        return;
    }

    try {
        request.user = verifyToken(token);
        next();
    } catch (error) {
        response.status(401).json({ error: "invalid_token" });
    }
}

function requireRole(role) {
    return function roleMiddleware(request, response, next) {
        if (!request.user.roles.includes(role)) {
            response.status(403).json({ error: "forbidden" });
            return;
        }

        next();
    };
}
```

Strong answer:

```text
Authentication and authorization must be enforced on the server. Frontend checks are UX only.
```

---

## 44. JWT Production Cautions

JWT checks:

- Signature.
- Algorithm allowlist.
- Expiration.
- Issuer.
- Audience.
- Key rotation.
- Clock skew.
- Token revocation strategy if needed.

Trap:

```text
Decoding a JWT is not the same as verifying it.
```

Strong answer:

```text
For JWTs, I verify the signature and claims, restrict algorithms, handle key rotation, and keep
token lifetimes appropriate for the risk.
```

---

## 45. Rate Limiting

Rate limiting protects services from abuse and accidental overload.

Dimensions:

- IP address.
- User ID.
- API key.
- Tenant.
- Route/action.

Simple in-memory limiter:

```javascript
const buckets = new Map();

function rateLimit({ windowMs, max }) {
    return function middleware(request, response, next) {
        const key = request.ip;
        const now = Date.now();
        const bucket = buckets.get(key) ?? { count: 0, resetAt: now + windowMs };

        if (bucket.resetAt <= now) {
            bucket.count = 0;
            bucket.resetAt = now + windowMs;
        }

        bucket.count++;
        buckets.set(key, bucket);

        if (bucket.count > max) {
            response.status(429).json({ error: "rate_limited" });
            return;
        }

        next();
    };
}
```

Production caution:

```text
In-memory rate limits do not work correctly across multiple replicas. Use Redis or gateway-level
rate limiting for distributed systems.
```

Strong answer:

```text
Rate limiting should match the abuse model and deployment model. In-memory is okay for learning,
but distributed production rate limiting needs shared state or gateway support.
```

---

## 46. Security Headers

Common headers:

```text
Content-Security-Policy
Strict-Transport-Security
X-Content-Type-Options
X-Frame-Options or frame-ancestors in CSP
Referrer-Policy
Permissions-Policy
```

Express ecosystem often uses Helmet-style middleware.

Strong answer:

```text
Security headers reduce browser-side attack surface, but they complement server-side validation,
auth, output encoding, and dependency hygiene.
```

---

## 47. Prototype Pollution

Risk:

```text
Merging untrusted objects into normal objects can modify Object.prototype or unexpected inherited properties.
```

Dangerous input:

```json
{
  "__proto__": {
    "isAdmin": true
  }
}
```

Defenses:

- Validate input schema.
- Reject keys like `__proto__`, `constructor`, `prototype` when merging dynamic objects.
- Use safe merge utilities.
- Use objects without prototypes when appropriate.
- Keep dependencies patched.

Strong answer:

```text
Prototype pollution is a JavaScript-specific backend risk. I prevent it by validating input and
avoiding unsafe deep merges of untrusted objects.
```

---

## 48. Memory Model And Leaks

Memory areas to know:

- V8 heap.
- Native memory.
- Buffers/external memory.
- C++ addon memory.
- Stack.
- OS resources: sockets, file descriptors.

Leak patterns:

- Global arrays/maps that grow forever.
- Unbounded caches.
- Listeners never removed.
- Timers never cleared.
- Large closures retained.
- Request objects stored after request ends.
- Buffers retained accidentally.

Example leak:

```javascript
const requests = [];

app.use((request, response, next) => {
    requests.push(request);
    next();
});
```

Fix mindset:

```text
Store only necessary data, bound caches, cleanup resources, and use heap snapshots to inspect retainers.
```

Strong answer:

```text
In Node, memory leaks often come from retained references, unbounded caches, listeners, timers,
and buffers. I verify with heap snapshots and memory metrics over repeated traffic.
```

---

## 49. Process Memory Metrics

```javascript
setInterval(() => {
    const memory = process.memoryUsage();

    console.log({
        rssMb: toMb(memory.rss),
        heapUsedMb: toMb(memory.heapUsed),
        heapTotalMb: toMb(memory.heapTotal),
        externalMb: toMb(memory.external),
        arrayBuffersMb: toMb(memory.arrayBuffers)
    });
}, 30_000);

function toMb(bytes) {
    return Number((bytes / 1024 / 1024).toFixed(2));
}
```

Meaning:

| Metric | Meaning |
|---|---|
| rss | Total memory resident in RAM |
| heapUsed | V8 heap currently used |
| heapTotal | V8 heap allocated |
| external | Native/external memory tied to JS objects |
| arrayBuffers | ArrayBuffer/Buffer memory |

Strong answer:

```text
If heap is stable but RSS grows, I also investigate native memory, buffers, dependencies, and
container memory behavior.
```

---

## 50. Unhandled Rejections And Uncaught Exceptions

Bad:

```javascript
void doAsyncWork();
```

If it rejects, it may become unhandled.

Better:

```javascript
void doAsyncWork().catch(error => {
    logger.error({ error }, "background task failed");
});
```

Process handlers:

```javascript
process.on("unhandledRejection", reason => {
    logger.fatal({ reason }, "unhandled rejection");
    process.exit(1);
});

process.on("uncaughtException", error => {
    logger.fatal({ error }, "uncaught exception");
    process.exit(1);
});
```

Production note:

```text
After an uncaught exception, process state may be unsafe. Log, exit, and let the supervisor restart.
```

Strong answer:

```text
Unhandled errors should be treated as serious process-level failures. I log them, shut down safely,
and rely on orchestration to restart a clean process.
```

---

## 51. Background Jobs

Do not run durable business work only in request memory.

Bad:

```javascript
app.post("/bookings", async (request, response) => {
    const booking = await createBooking(request.body);

    sendEmail(booking); // fire and forget, can be lost on crash

    response.status(201).json(booking);
});
```

Better production shape:

```javascript
app.post("/bookings", async (request, response) => {
    const booking = await createBooking(request.body);
    await queue.publish("booking.created", { bookingId: booking.id });
    response.status(201).json(booking);
});
```

Worker:

```javascript
queue.consume("booking.created", async message => {
    await sendBookingEmail(message.bookingId);
});
```

Strong answer:

```text
For durable side effects, I use queues or transactional outbox patterns instead of best-effort
in-process fire-and-forget work.
```

---

## 52. Queue And Backpressure Thinking

Queues help when:

- Work is slow.
- Work can be asynchronous.
- Bursts need smoothing.
- Retry is needed.
- Worker scaling should be independent.

Queue risks:

- Duplicate delivery.
- Poison messages.
- Unbounded backlog.
- Retry storms.
- Ordering assumptions.
- Visibility timeout issues.

Strong answer:

```text
Queue consumers should be idempotent because most production queues are at-least-once delivery.
```

---

## 53. Webhooks

Webhook endpoint production checklist:

- Verify signature.
- Read raw body if signature requires it.
- Enforce timestamp tolerance.
- Idempotency by event ID.
- Fast acknowledgement.
- Queue heavy work.
- Log event ID and provider.
- Protect against replay.

Shape:

```javascript
app.post("/webhooks/payment", rawBodyMiddleware, async (request, response) => {
    const event = verifyWebhookSignature(request.rawBody, request.headers);

    if (await alreadyProcessed(event.id)) {
        response.status(200).json({ received: true });
        return;
    }

    await saveEvent(event);
    await queue.publish("payment.webhook", event);

    response.status(200).json({ received: true });
});
```

Strong answer:

```text
Webhook handlers must verify authenticity, deduplicate by event ID, and process idempotently
because providers may retry events.
```

---

## 54. File Upload API

Production concerns:

- Size limits.
- Content type validation.
- Streaming upload.
- Virus/malware scanning.
- Object storage instead of app disk.
- Temporary file cleanup.
- Auth and ownership.
- Rate limits.

Bad:

```text
Load entire multi-GB file into memory in the Node process.
```

Better design:

```text
Client -> pre-signed object-storage URL -> object storage -> scan/process async -> app stores metadata
```

Strong answer:

```text
For large uploads, I prefer direct-to-object-storage or streaming approaches so the Node API does
not become a memory bottleneck.
```

---

## 55. WebSocket Backend Concerns

WebSocket services need:

- Authentication at connection time.
- Heartbeats/ping-pong.
- Backpressure handling.
- Reconnect strategy.
- Connection limits.
- Horizontal scaling strategy.
- Pub/sub for multi-replica broadcasting.
- Cleanup on close.

Strong answer:

```text
WebSockets make connection state a production concern. In multi-replica Node deployments, broadcast
and presence usually need shared infrastructure like Redis pub/sub or a message broker.
```

---

## 56. Compression

Compression reduces bandwidth but costs CPU.

Use for:

- Text responses.
- JSON payloads.
- HTML/CSS/JS when not already compressed by CDN.

Avoid or be careful for:

- Already compressed images/videos.
- Very small payloads.
- CPU-constrained services.
- Sensitive responses with compression side-channel risk in specific contexts.

Strong answer:

```text
Compression is a bandwidth/CPU trade-off. I usually let CDN/gateway handle static compression and
measure CPU impact for dynamic API compression.
```

---

## 57. JSON Performance And Payload Size

Issues:

- Large JSON parse blocks event loop.
- Large JSON stringify blocks event loop.
- Huge response bodies increase latency and memory.
- Nested payloads are expensive for clients too.

Mitigations:

- Paginate.
- Select only needed fields.
- Stream when appropriate.
- Compress when beneficial.
- Use binary formats only with clear need.
- Move huge exports to async jobs/files.

Strong answer:

```text
JSON parse/stringify is synchronous JavaScript work. Very large payloads can block the event loop,
so I keep API payloads bounded and use streaming or async export jobs for large data.
```

---

## 58. API Pagination

Bad:

```text
GET /bookings returns all bookings forever.
```

Offset pagination:

```text
GET /bookings?limit=50&offset=100
```

Cursor pagination:

```text
GET /bookings?limit=50&cursor=abc
```

Cursor benefits:

- Better for large datasets.
- More stable under inserts/deletes.
- Often more efficient with indexed ordering.

Strong answer:

```text
Every list endpoint should be bounded. Cursor pagination is often better for large or changing datasets.
```

---

## 59. Production API Checklist

Before shipping a Node API route, ask:

- Is input validated?
- Is auth required and checked?
- Are list results bounded?
- Are dependency calls timed out?
- Are writes idempotent if retries can happen?
- Are errors stable and non-leaky?
- Are logs correlated with request ID?
- Are metrics/traces emitted?
- Is concurrency bounded?
- Is memory usage safe?
- Are downstream failures handled?
- Are tests covering success and failure paths?

---

## 60. Mini Program: Production-Style HTTP API Without Framework

```javascript
import http from "node:http";
import { randomUUID } from "node:crypto";

const bookings = new Map();

const server = http.createServer(async (request, response) => {
    const requestId = request.headers["x-request-id"] ?? randomUUID();
    response.setHeader("X-Request-Id", requestId);
    response.setHeader("Content-Type", "application/json");

    try {
        if (request.method === "GET" && request.url === "/health") {
            send(response, 200, { status: "ok" });
            return;
        }

        if (request.method === "POST" && request.url === "/bookings") {
            const body = await readJsonBody(request);
            const input = parseCreateBooking(body);
            const id = randomUUID();
            const booking = { id, ...input, status: "CREATED" };
            bookings.set(id, booking);
            send(response, 201, booking);
            return;
        }

        send(response, 404, { error: "not_found", requestId });
    } catch (error) {
        const status = error.statusCode ?? 500;
        send(response, status, {
            error: error.code ?? "internal_error",
            message: status >= 500 ? "Something went wrong" : error.message,
            requestId
        });
    }
});

server.listen(3000);

function send(response, statusCode, payload) {
    response.writeHead(statusCode);
    response.end(JSON.stringify(payload));
}

async function readJsonBody(request, limitBytes = 1_000_000) {
    let size = 0;
    const chunks = [];

    for await (const chunk of request) {
        size += chunk.length;

        if (size > limitBytes) {
            throw httpError(413, "payload_too_large", "Payload too large");
        }

        chunks.push(chunk);
    }

    try {
        return JSON.parse(Buffer.concat(chunks).toString("utf8"));
    } catch {
        throw httpError(400, "invalid_json", "Invalid JSON");
    }
}

function parseCreateBooking(input) {
    if (!input || typeof input !== "object") {
        throw httpError(400, "invalid_body", "Body must be an object");
    }

    if (typeof input.guestName !== "string" || input.guestName.trim() === "") {
        throw httpError(400, "invalid_guest_name", "guestName is required");
    }

    if (typeof input.roomId !== "string" || input.roomId.trim() === "") {
        throw httpError(400, "invalid_room_id", "roomId is required");
    }

    return {
        guestName: input.guestName.trim(),
        roomId: input.roomId.trim()
    };
}

function httpError(statusCode, code, message) {
    const error = new Error(message);
    error.statusCode = statusCode;
    error.code = code;
    return error;
}
```

Why this is interview-strong:

- Uses raw Node HTTP to show fundamentals.
- Adds request ID.
- Parses request body as stream.
- Enforces body size limit.
- Validates input.
- Returns stable errors.
- Avoids leaking stack traces.

---

## 61. Mini Program: Timeout, Retry, And Idempotency Client

```javascript
async function postJsonWithPolicy(url, body, options = {}) {
    const {
        timeoutMs = 3000,
        attempts = 3,
        idempotencyKey = crypto.randomUUID()
    } = options;

    return retry(async () => {
        const response = await fetchWithTimeout(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Idempotency-Key": idempotencyKey
            },
            body: JSON.stringify(body)
        }, timeoutMs);

        if (response.status === 429 || response.status >= 500) {
            throw new Error(`retryable HTTP ${response.status}`);
        }

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return response.json();
    }, { attempts });
}
```

Interview note:

```text
Retries around POST are safe only if the server honors the idempotency key.
```

---

## 62. Mini Program: Stream Large File Safely

```javascript
import { createReadStream } from "node:fs";
import { pipeline } from "node:stream/promises";

async function sendFile({ filePath, response, contentType }) {
    response.setHeader("Content-Type", contentType);

    await pipeline(
        createReadStream(filePath),
        response
    );
}
```

Why strong:

- Does not load entire file into memory.
- Uses pipeline for backpressure and error handling.
- Works with slow clients better than readFile-send.

---

## 63. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| CPU-heavy code in route | Blocks all callbacks in process | Worker/job/external system |
| Promise.all on unbounded input | Memory and dependency overload | Concurrency limit |
| No request body limit | Memory DoS risk | Enforce size limits |
| Trusting TypeScript at runtime | External input still unsafe | Runtime validation |
| No dependency timeouts | Requests hang indefinitely | Timeouts and cancellation |
| Blind retries | Retry storm/duplicate writes | Backoff, jitter, idempotency |
| Fire-and-forget durable work | Lost on crash | Queue/outbox |
| In-memory sessions in multi-replica app | Users lose state or sticky dependency | External session store |
| Unbounded cache | Memory leak | TTL and max size |
| Missing graceful shutdown | Dropped requests during deploy | Drain and close resources |
| Logging secrets | Security incident | Redact and log safe context |
| Missing async error forwarding | Unhandled rejection | async wrapper/error middleware |
| readFile for huge files | Memory spike | Streams/pipeline |
| No DB pool limits | DB overload | Tune pool and monitor wait time |
| CORS/security as afterthought | Browser/API risk | Configure deliberately |
| In-process scheduler for critical jobs | Lost/duplicated work across replicas | External scheduler/queue |

---

## 64. Strong Interview Answers

### Why is Node good for IO-heavy services?

```text
Node uses an event loop and non-blocking IO, so one process can manage many concurrent network
requests without one thread per connection. It is strong when most time is spent waiting on IO,
but CPU-heavy JavaScript must be moved off the main thread or it blocks the event loop.
```

### What blocks the Node event loop?

```text
Long synchronous JavaScript, huge JSON parse/stringify, sync filesystem calls, expensive regex,
large loops, CPU-heavy crypto/compression in the wrong path, and unbounded work in request handlers.
```

### How do you handle errors in a Node API?

```text
I validate input at the boundary, throw typed/domain errors, forward async errors to central
middleware, return stable error codes to clients, log internal details with request IDs, and avoid
leaking stack traces or secrets.
```

### How do streams help production APIs?

```text
Streams process data incrementally instead of loading everything into memory. They are useful for
large files, uploads, downloads, compression, and proxies, and they support backpressure so slow
consumers do not overwhelm memory.
```

### When do you use worker threads?

```text
I use worker threads for CPU-heavy JavaScript that would otherwise block the event loop. For durable
or distributed background work, I use queues and separate workers instead.
```

### How do you make retries safe?

```text
Retries need bounded attempts, exponential backoff, jitter, timeouts, and idempotency. For POST
operations like payments or orders, server-side idempotency keys are required.
```

---

## 65. MAANG Scenario 1: API Latency Spike

> A Node API suddenly has high p99 latency across all routes, including health checks. CPU is high.

Strong answer:

```text
Because all routes are slow, I suspect event-loop blocking or process-level resource exhaustion,
not one database query only. I would check event-loop delay, CPU profile, recent deploys, route
latency breakdown, heap/GC, and dependency metrics.

If CPU-heavy JavaScript is found in a request path, I would move it to worker threads, child
processes, or an async job. If the issue is huge JSON payloads, I would paginate, stream, or move
exports to background jobs. I would add event-loop delay alerts to catch this earlier.
```

---

## 66. MAANG Scenario 2: File Export Crashes Service

> Users export large reports and the Node process runs out of memory.

Strong answer:

```text
I would check whether the service loads the entire dataset or file into memory. For large exports,
I would stream database rows to a file/response with backpressure, or generate the export in a
background job and upload it to object storage.

I would add pagination or export limits, monitor memory/RSS, and avoid synchronous JSON/CSV
construction for huge datasets in the request path.
```

---

## 67. MAANG Scenario 3: Duplicate Orders After Timeout

> Client times out calling a Node order API, retries, and duplicate orders are created.

Strong answer:

```text
A timeout does not prove the first request failed. The server may have completed the order after
the client disconnected. For non-idempotent writes, I would require an idempotency key, store the
first result for that key, and return the same result on retry.

I would also set clear client/server timeouts, avoid blind retries for unsafe operations, and add
observability around duplicate prevention.
```

---

## 68. MAANG Scenario 4: Memory Grows All Day

> A Node service memory graph grows until the container restarts every few hours.

Strong answer:

```text
I would compare heapUsed, RSS, external memory, request rate, and GC behavior. Then I would take
heap snapshots over time and inspect retained objects.

Common causes are unbounded caches, arrays/maps retaining request data, listeners not removed,
timers, sockets, buffers, or dependency leaks. The fix is to remove the retaining reference, bound
caches, cleanup resources, and add memory regression monitoring.
```

---

## 69. MAANG Scenario 5: Downstream Outage Causes Retry Storm

> A payment provider is slow. Node services retry aggressively and make the outage worse.

Strong answer:

```text
I would add timeouts, bounded retries with exponential backoff and jitter, circuit breaking or
load shedding where appropriate, and idempotency keys for payment operations. I would also respect
provider 429/Retry-After responses and cap total retry budgets.

At system level, retries should be coordinated with queues, rate limits, and observability so the
service does not amplify downstream failure.
```

---

## 70. Rapid Revision

- Node runs JavaScript on the main thread by default.
- Node is not single-threaded for all work; libuv, OS async IO, thread pool, workers, and child processes exist.
- IO-heavy services fit Node well.
- CPU-heavy JavaScript blocks the event loop.
- Event-loop delay is a key production metric.
- `process.nextTick` can starve the event loop if abused.
- CommonJS uses `require` and `module.exports`.
- ESM uses `import` and `export`.
- Mixing ESM and CommonJS needs care.
- Request and response bodies are streams.
- Large files should be streamed, not loaded fully into memory.
- Backpressure protects memory when consumers are slow.
- Always enforce request body size limits.
- Validate runtime input at every external boundary.
- TypeScript does not validate network payloads at runtime.
- Async route errors need central handling.
- Stable error shapes help clients and tests.
- Do not leak stack traces or secrets to users.
- Every dependency call should have a timeout.
- Retries require backoff, jitter, limits, and idempotency.
- POST is not idempotent by default.
- Promise.all on unbounded input can overload everything.
- Database pools are backpressure and capacity controls.
- Parameterized queries prevent SQL injection.
- Caches need TTL, max size, and safe keys.
- Worker threads help CPU-heavy JavaScript.
- Child processes isolate work but require safe argument handling.
- Multiple Node replicas require externalized shared state.
- Graceful shutdown drains requests during deploys.
- Liveness and readiness are different.
- Logs should be structured and correlated by request ID.
- Metrics should include latency, error rate, event-loop delay, memory, and dependency health.
- Tracing shows where distributed requests spend time.
- Authentication proves identity; authorization checks permission.
- Decoding JWT is not verification.
- In-memory rate limits do not work across replicas.
- Prototype pollution is a JavaScript backend risk.
- Memory leaks often come from retained references, unbounded caches, listeners, timers, and buffers.
- Fire-and-forget work can be lost on crash.
- Durable async work belongs in queues/outbox patterns.
- Webhooks need signature verification and idempotency.
- Large uploads should stream or go direct to object storage.
- WebSockets need heartbeat, cleanup, backpressure, and multi-replica design.
- Huge JSON parse/stringify can block the event loop.
- List APIs must be bounded with pagination.

---

## 72. AsyncLocalStorage — Trace Context Propagation

`AsyncLocalStorage` provides request-scoped storage that flows automatically through async operations without passing values explicitly through function arguments.

### Why It Matters

```text
Problem: A request arrives at the Node.js server. You need:
  - requestId (for tracing)
  - userId (for authorization logging)
  - correlationId (for distributed tracing)

Passing these through every function call is impractical.
ThreadLocal equivalent does not exist in single-threaded JavaScript.
Solution: AsyncLocalStorage — persists context across await, callbacks, and async boundaries.
```

### Basic Usage

```javascript
import { AsyncLocalStorage } from 'node:async_hooks';

// Create a single shared storage instance (module-level singleton)
export const requestContext = new AsyncLocalStorage();

// Middleware: establish context for the duration of the request
app.use((req, res, next) => {
    const context = {
        requestId: req.headers['x-request-id'] || crypto.randomUUID(),
        userId: req.auth?.userId,
        correlationId: req.headers['x-correlation-id'] || crypto.randomUUID()
    };

    // All async operations spawned from this callback inherit this store
    requestContext.run(context, () => {
        next();
    });
});

// Any function anywhere in the call graph can access the context
export function getCurrentRequestId() {
    return requestContext.getStore()?.requestId;
}

export function log(message, extra = {}) {
    const context = requestContext.getStore() ?? {};
    console.log(JSON.stringify({
        timestamp: new Date().toISOString(),
        level: 'INFO',
        requestId: context.requestId,
        correlationId: context.correlationId,
        userId: context.userId,
        message,
        ...extra
    }));
}
```

### Propagation Through Async Code

```javascript
// requestContext flows automatically through all async boundaries
async function handleCreateBooking(req, res) {
    log('Creating booking'); // requestId available, not passed explicitly

    const booking = await bookingService.create(req.body);
    // bookingService.create → calls paymentService → calls database
    // requestId available in ALL of these without passing it

    log('Booking created', { bookingId: booking.id });
    res.json(booking);
}

async function bookingService_create(data) {
    log('Booking service: creating'); // requestId still available

    const payment = await paymentService.charge(data.amount);
    // Still available in paymentService

    return saveBooking({ ...data, paymentId: payment.id });
}
```

### Propagation Through Worker Threads

```javascript
import { Worker, workerData } from 'node:worker_threads';

// Context does NOT propagate automatically to Worker threads
// Must pass explicitly via workerData
const context = requestContext.getStore();

const worker = new Worker('./heavy-worker.js', {
    workerData: {
        context: { requestId: context?.requestId },
        taskData: heavyData
    }
});
```

### Propagation to Outgoing HTTP Requests

```javascript
// HTTP client interceptor that adds correlation headers from context
import axios from 'axios';

const httpClient = axios.create();

httpClient.interceptors.request.use(config => {
    const context = requestContext.getStore();

    if (context?.correlationId) {
        config.headers['X-Correlation-Id'] = context.correlationId;
    }
    if (context?.requestId) {
        config.headers['X-Request-Id'] = context.requestId;
    }

    return config;
});
```

### Production Pattern: OpenTelemetry Integration

Most APM libraries (OpenTelemetry, Datadog, New Relic) use `AsyncLocalStorage` internally to propagate trace context:

```javascript
import { context, trace } from '@opentelemetry/api';

// The active span is automatically propagated via AsyncLocalStorage
app.use((req, res, next) => {
    const tracer = trace.getTracer('booking-service');
    const span = tracer.startSpan('http.request', {
        attributes: {
            'http.method': req.method,
            'http.route': req.path
        }
    });

    // Runs subsequent middleware inside the span's context
    context.with(trace.setSpan(context.active(), span), () => {
        res.on('finish', () => span.end());
        next();
    });
});
```

### Interview Line

```text
AsyncLocalStorage is the Node.js equivalent of thread-local storage. I use it to store
request-scoped context — request ID, correlation ID, user ID, tenant ID — once at the
middleware level. All async operations in that request's lifetime automatically inherit
the context without requiring explicit parameter passing. This is the foundation for
structured logging, distributed tracing, and multi-tenant context in production Node.js services.
```

---

## 73. diagnostics_channel — Modern Observability API

`diagnostics_channel` is a publish-subscribe API for emitting structured diagnostic events from Node.js internals and application code.

### Core Concepts

```javascript
import diagnostics_channel from 'node:diagnostics_channel';

// Subscribe to a named channel
const channel = diagnostics_channel.channel('booking:created');

channel.subscribe(data => {
    console.log('Booking created diagnostic:', data);
    metrics.increment('bookings.created', { status: data.status });
});

// Publish to the channel
function createBooking(data) {
    const booking = saveToDatabase(data);

    // Publish to any subscribers — only called if there are subscribers (no-op otherwise)
    channel.publish({ bookingId: booking.id, status: booking.status });

    return booking;
}
```

### Built-in Node.js Channels

Node.js core uses `diagnostics_channel` internally. Subscribing gives you structured events:

```javascript
import diagnostics_channel from 'node:diagnostics_channel';

// HTTP client events
const httpClientChannel = diagnostics_channel.channel('http.client.request.start');
httpClientChannel.subscribe(data => {
    // data contains: request details, hostname, path, method
    performanceTracker.startRequest(data.request[Symbol.for('id')]);
});

// undici (Node.js built-in HTTP client) channels
diagnostics_channel.channel('undici:request:create').subscribe(data => {
    log('Outgoing request', {
        url: data.request.origin + data.request.path,
        method: data.request.method
    });
});
```

### Custom Application Channels

```javascript
// Define channels in a shared module
export const channels = {
    bookingCreated: diagnostics_channel.channel('myapp:booking:created'),
    bookingFailed: diagnostics_channel.channel('myapp:booking:failed'),
    paymentCharged: diagnostics_channel.channel('myapp:payment:charged')
};

// In service code
function processBooking(data) {
    try {
        const booking = createBooking(data);
        channels.bookingCreated.publish({ bookingId: booking.id, amount: data.amount });
        return booking;
    } catch (error) {
        channels.bookingFailed.publish({ error: error.message, data });
        throw error;
    }
}

// In observability layer (separate concern)
channels.bookingCreated.subscribe(data => {
    metrics.increment('booking.created.count');
    metrics.histogram('booking.amount', data.amount);
    log('Booking created', data);
});

channels.bookingFailed.subscribe(data => {
    metrics.increment('booking.failed.count');
    errorTracker.capture(data);
});
```

### `hasSubscribers` — Zero Overhead When Unused

```javascript
// Only construct expensive diagnostic data if someone is listening
if (diagnostics_channel.channel('myapp:query:slow').hasSubscribers) {
    channel.publish({
        query: sql,
        duration: Date.now() - startTime,
        params: sanitize(params) // Expensive operation only when needed
    });
}
```

### Interview Line

```text
diagnostics_channel provides a built-in pub-sub mechanism for structured diagnostic events.
It has zero overhead when no subscribers are attached (hasSubscribers check). I use it to
decouple instrumentation from business logic: services publish events to named channels,
and observability code subscribes. This is how OpenTelemetry auto-instrumentation hooks into
undici, HTTP, and database operations in Node.js without patching libraries.
```

---

## 74. Official Source Notes

Use these sources when refreshing Node.js backend knowledge:

- Node.js docs: `https://nodejs.org/docs/latest/api/`
- Node.js event loop guide: `https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick`
- Node.js fs docs: `https://nodejs.org/docs/latest/api/fs.html`
- Node.js stream docs: `https://nodejs.org/docs/latest/api/stream.html`
- Node.js worker_threads docs: `https://nodejs.org/docs/latest/api/worker_threads.html`
- Node.js child_process docs: `https://nodejs.org/docs/latest/api/child_process.html`
- Node.js perf_hooks docs: `https://nodejs.org/docs/latest/api/perf_hooks.html`
- Node.js buffer docs: `https://nodejs.org/docs/latest/api/buffer.html`
- Node.js process docs: `https://nodejs.org/docs/latest/api/process.html`
- Express docs: `https://expressjs.com/`
- Fastify docs: `https://fastify.dev/`
- OWASP API Security Top 10: `https://owasp.org/API-Security/`
- npm docs: `https://docs.npmjs.com/`

Interview safety line:

```text
For Node.js backend interviews, I connect runtime mechanics to production behavior: event-loop
health, bounded concurrency, streams/backpressure, validation, timeouts, retries, idempotency,
observability, and graceful failure.
```
