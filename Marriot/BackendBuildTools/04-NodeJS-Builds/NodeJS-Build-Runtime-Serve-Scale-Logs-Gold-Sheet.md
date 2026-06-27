# Node.js Build, Runtime, Serve, Scale, Logs Gold Sheet

> Topic: TypeScript builds, Node runtime, serving APIs, scaling, and startup logs.

---

## 1. Intuition

A Node backend build usually prepares JavaScript that the Node runtime can execute. The runtime starts one or more processes, loads config, connects to dependencies, binds a port, and serves requests through an event loop.

Beginner version:

> Node builds prepare the code; Node runtime executes it and serves traffic.

---

## 2. Definition

- Definition: A Node.js backend pipeline installs dependencies, builds/transpiles source, runs tests, packages output, starts a Node process, and scales with processes or replicas.
- Category: JavaScript/TypeScript backend runtime pipeline.
- Core idea: build output plus runtime process plus event-loop-aware scaling.

---

## 3. Build Pipeline

```txt
src/server.ts
   |
   v
dependency install
   |
   v
typecheck
   |
   v
transpile TypeScript
   |
   v
test + coverage
   |
   v
dist/server.js
   |
   v
node dist/server.js
```

Typical scripts:

```json
{
  "scripts": {
    "typecheck": "tsc --noEmit",
    "build": "tsc -p tsconfig.json",
    "start": "node dist/server.js",
    "dev": "tsx watch src/server.ts",
    "test": "vitest run"
  }
}
```

---

## 4. Runtime Startup Flow

```txt
node dist/server.js
   |
   v
load environment variables
   |
   v
load modules
   |
   v
initialize logger/config
   |
   v
connect DB/cache/message broker
   |
   v
register routes/middleware
   |
   v
bind port
   |
   v
ready for traffic
```

Example startup logs:

```txt
INFO config loaded env=staging port=8080
INFO connecting postgres host=db.internal
INFO postgres connected poolSize=20
INFO redis connected
INFO routes registered count=42
INFO server listening on 0.0.0.0:8080
INFO readiness probe enabled path=/health/ready
```

---

## 5. Serve

Common Node backend frameworks:

- Express.
- Fastify.
- NestJS.
- Koa.

Minimal example:

```ts
import Fastify from 'fastify';

const app = Fastify({ logger: true });

app.get('/health/ready', async () => {
  return { status: 'ok' };
});

await app.listen({ host: '0.0.0.0', port: Number(process.env.PORT ?? 8080) });
```

Important production settings:

- bind to `0.0.0.0` in containers.
- expose readiness/liveness endpoints.
- use structured logs.
- handle unhandled rejections.
- graceful shutdown on `SIGTERM`.

---

## 6. Scale

Node is single-process by default.

Scaling options:

```txt
one process
   -> simple, limited to one event loop

cluster/workers
   -> multiple Node processes per machine

container replicas
   -> multiple pods/tasks across machines

message queue workers
   -> background job parallelism
```

Node is strong for:

- IO-heavy APIs.
- gateways.
- streaming.
- WebSockets.
- orchestration services.

Be careful with:

- CPU-heavy work.
- blocking synchronous operations.
- huge JSON parsing on event loop.
- excessive logging.

---

## 7. Graceful Shutdown

```ts
const shutdown = async (signal: string) => {
  console.log(`received ${signal}, shutting down`);
  await app.close();
  process.exit(0);
};

process.on('SIGTERM', () => void shutdown('SIGTERM'));
process.on('SIGINT', () => void shutdown('SIGINT'));
```

Why:

- Kubernetes sends `SIGTERM`.
- app should stop accepting new requests.
- existing requests should finish when possible.
- DB connections should close.

---

## 8. Reading Node Startup Logs

Read top-to-bottom:

```txt
1. Node version selected?
2. correct entrypoint?
3. env/config loaded?
4. modules imported successfully?
5. DB/cache/broker connected?
6. migrations run or skipped intentionally?
7. routes registered?
8. port bound?
9. health endpoint ready?
```

Common log symptoms:

| Log Symptom | Likely Stage |
|---|---|
| `Cannot find module` | build output/copy/install problem |
| `ERR_MODULE_NOT_FOUND` | ESM/CJS/module path problem |
| `EADDRINUSE` | port already used |
| `ECONNREFUSED` | dependency not reachable |
| app exits with code 0 | entrypoint completed without server staying alive |
| app hangs before listening | startup dependency or migration blocking |

---

## 9. Real-World Docker Pattern

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM deps AS build
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
CMD ["node", "dist/server.js"]
```

Why:

- build tools stay out of runtime image.
- production image has runtime dependencies only.
- output is explicit.

---

## 10. Common Mistakes

### Mistake: Starting TypeScript directly in production without a plan

- Why wrong: runtime depends on dev tooling.
- Better approach: build to JS or intentionally use a supported runtime strategy.

### Mistake: No graceful shutdown

- Why wrong: deployments can drop requests or corrupt work.
- Better approach: handle `SIGTERM` and close resources.

### Mistake: Blocking the event loop

- Why wrong: one slow CPU task can stall all requests in the process.
- Better approach: move CPU work to workers/services.

### Mistake: Missing startup health distinction

- Why wrong: traffic may hit the app before dependencies are ready.
- Better approach: separate liveness from readiness.

---

## 11. NestJS Build Pipeline

NestJS (enterprise TypeScript framework) has a structured build pipeline:

```bash
# Install
npm ci

# Build: TypeScript → JavaScript (uses tsc under the hood, output to dist/)
npm run build
# equivalent to: nest build

# Start production
node dist/main.js
# or: nest start (not for production — uses ts-node-dev which is slow)
```

```json
// package.json scripts (NestJS standard)
{
  "scripts": {
    "build": "nest build",
    "start": "node dist/main",
    "start:prod": "node dist/main",
    "test": "jest",
    "test:cov": "jest --coverage"
  }
}
```

NestJS `dist/` structure:
```
dist/
├── main.js          ← application entry point
├── app.module.js
├── controllers/
└── services/
```

---

## 12. Node.js Clustering for Multi-Core Utilization

Node.js is single-threaded per process. To use all CPU cores:

```javascript
// cluster.js — fork one worker per CPU core
const cluster = require('cluster');
const os = require('os');

if (cluster.isPrimary) {
  const numCPUs = os.cpus().length;
  console.log(`Primary ${process.pid} starting ${numCPUs} workers`);

  for (let i = 0; i < numCPUs; i++) {
    cluster.fork();
  }

  cluster.on('exit', (worker, code, signal) => {
    console.log(`Worker ${worker.process.pid} died — restarting`);
    cluster.fork();  // auto-restart dead workers
  });
} else {
  require('./dist/main');  // each worker runs the full app
  console.log(`Worker ${process.pid} started`);
}
```

Kubernetes alternative: instead of cluster, run one pod per CPU core as separate containers — simpler, easier to observe individually.

---

## 13. PM2 Process Manager

PM2 manages Node.js processes in production (most useful outside containers):

```yaml
# ecosystem.config.yml
module.exports = {
  apps: [{
    name: 'payments-api',
    script: 'dist/main.js',
    instances: 'max',          // fork one per CPU core
    exec_mode: 'cluster',
    max_memory_restart: '500M',
    env_production: {
      NODE_ENV: 'production',
      PORT: 8080
    },
    error_file: '/var/log/payments-api-error.log',
    out_file: '/var/log/payments-api-out.log',
    log_date_format: 'YYYY-MM-DD HH:mm:ss Z'
  }]
};
```

```bash
pm2 start ecosystem.config.yml --env production
pm2 status
pm2 logs payments-api
pm2 reload payments-api   # zero-downtime reload
```

In containers: prefer single-process (`node dist/main.js`) — Kubernetes handles restarts and scaling. PM2 adds complexity inside containers.

---

## 14. Memory Limit and Worker Threads

```bash
# Increase V8 heap for memory-heavy Node.js processes
node --max-old-space-size=2048 dist/main.js   # 2GB heap

# In package.json:
"start:prod": "node --max-old-space-size=2048 dist/main.js"
```

Worker Threads for CPU-bound work (e.g., PDF generation, heavy computation):

```javascript
// main.js — offload CPU-bound work to Worker Thread
const { Worker } = require('worker_threads');

function runReportInThread(reportData) {
  return new Promise((resolve, reject) => {
    const worker = new Worker('./report-worker.js', {
      workerData: reportData
    });
    worker.on('message', resolve);
    worker.on('error', reject);
    worker.on('exit', (code) => {
      if (code !== 0) reject(new Error(`Worker exited with code ${code}`));
    });
  });
}
```

```javascript
// report-worker.js
const { workerData, parentPort } = require('worker_threads');
// CPU-heavy work runs here without blocking event loop
const result = generateReport(workerData);
parentPort.postMessage(result);
```

Rule: use Worker Threads for CPU-bound tasks; never block the event loop with synchronous computation.

---

## 15. Interview Insight

Strong answer:

> For a Node backend, I install with a lockfile, typecheck, transpile TypeScript to JavaScript, run tests and coverage, then run `node dist/server.js` in production. Runtime startup should log config loading, dependency connections, route registration, port bind, and readiness. Scaling is usually process/replica based because each Node process has one main event loop.

Follow-up trap:

> Why did the container start and then immediately stop?

Good answer:

> The entrypoint likely finished instead of keeping a server process alive, or crashed during startup. I would inspect logs for module import errors, missing env vars, failed dependency connections, and whether the HTTP server actually called `listen`.

---

## 16. Revision Notes

- One-line summary: Node backend delivery is locked install, build output, runtime process, health, and event-loop-aware scaling.
- Three keywords: dist, listen, event loop.
- One interview trap: a successful `npm run build` does not prove the server can start.
- Memory trick: Build makes `dist`; runtime must keep listening.

Strong answer:

> For a Node backend, I install with a lockfile, typecheck, transpile TypeScript to JavaScript, run tests and coverage, then run `node dist/server.js` in production. Runtime startup should log config loading, dependency connections, route registration, port bind, and readiness. Scaling is usually process/replica based because each Node process has one main event loop.

Follow-up trap:

> Why did the container start and then immediately stop?

Good answer:

> The entrypoint likely finished instead of keeping a server process alive, or crashed during startup. I would inspect logs for module import errors, missing env vars, failed dependency connections, and whether the HTTP server actually called `listen`.

---

## 12. Revision Notes

- One-line summary: Node backend delivery is locked install, build output, runtime process, health, and event-loop-aware scaling.
- Three keywords: dist, listen, event loop.
- One interview trap: a successful `npm run build` does not prove the server can start.
- Memory trick: Build makes `dist`; runtime must keep listening.
