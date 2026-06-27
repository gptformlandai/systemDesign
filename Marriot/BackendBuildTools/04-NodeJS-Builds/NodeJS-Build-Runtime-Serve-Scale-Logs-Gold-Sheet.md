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

## 11. Interview Insight

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
