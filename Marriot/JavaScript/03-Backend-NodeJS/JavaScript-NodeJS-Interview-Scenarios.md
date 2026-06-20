# JavaScript Node.js Interview Scenarios

Target: Node.js, backend JavaScript, full-stack, platform, and MAANG interviews where you must debug production symptoms, explain root causes, and propose reliable fixes under pressure.

This sheet covers:
- Scenario answer framework
- API latency and p99 spikes
- Event-loop blocking
- CPU-heavy work
- Huge JSON payloads
- Streams and backpressure
- Memory leaks
- Connection pool exhaustion
- Retry storms
- Idempotency failures
- Queue and worker incidents
- Webhook reliability
- File upload and export failures
- WebSocket scaling issues
- CORS, auth, JWT, and rate limit incidents
- Dependency and npm supply-chain risks
- Graceful shutdown and deployment bugs
- Observability-driven debugging
- Strong spoken interview answers
- MAANG-style production scenario drills

How to use this:
- Read each scenario as an interviewer prompt.
- Answer using: symptom, evidence, root cause, fix, and prevention.
- Practice saying the strong answer out loud in 60-90 seconds.
- For every solution, mention trade-offs and production guardrails.

---

## 1. Node Scenario Answer Framework

Use this for almost every backend scenario:

```text
1. Clarify user impact and scope.
2. Identify likely causes.
3. Explain what production evidence you would check.
4. Isolate the root cause.
5. Apply immediate mitigation.
6. Apply durable fix.
7. Add prevention: tests, limits, metrics, alerts, runbooks.
```

Short version:

```text
Impact -> evidence -> cause -> mitigation -> durable fix -> prevention.
```

Strong line:

```text
For Node incidents, I check event-loop delay, CPU, memory/RSS, request latency by route,
dependency latency, error rate, connection pools, queue depth, and recent deploys before guessing.
```

---

## 2. Scenario Priority Meter

| Scenario | Priority | What It Tests |
|---|---:|---|
| All routes slow | Very high | Event-loop blocking / CPU / process health |
| One route slow | Very high | Dependency or query isolation |
| Health check slow | Very high | Process-level blocking |
| Memory grows all day | Very high | Heap/RSS leaks and retainers |
| Large export crashes service | Very high | Streaming, backpressure, async jobs |
| Payment duplicated after retry | Very high | Idempotency and timeout semantics |
| Retry storm during outage | Very high | Backoff, jitter, circuit breakers |
| Promise.all overload | Very high | Bounded concurrency |
| DB pool exhausted | Very high | Connection leaks and pool sizing |
| Queue retries forever | High | Poison messages and DLQ |
| Webhook processed twice | High | Idempotent consumers |
| Upload freezes server | High | Streaming and object storage |
| WebSocket does not scale | High | Multi-replica state |
| Async errors disappear | High | Error middleware and unhandled rejections |
| Logs leak secrets | High | Safe observability |
| JWT accepted incorrectly | High | Verification vs decoding |
| Slow startup or bad deploy | Medium-high | config validation and readiness |
| In-memory cache grows | High | TTL and max size |
| Compression hurts latency | Medium | CPU/bandwidth trade-off |
| Dependency breaks production | High | package hygiene and lockfiles |

---

## 3. Debugging Evidence Map

| Symptom | First Evidence To Check |
|---|---|
| All endpoints slow | event-loop delay, CPU, GC, recent deploy |
| Only one endpoint slow | route latency, DB query, dependency timing |
| Memory climbs | heapUsed, RSS, external, heap snapshots |
| Process restarts | container OOM, exit code, fatal logs |
| API storm | outbound request count, retry metrics, queue depth |
| DB timeout | pool wait, active connections, slow queries |
| Large response failure | payload size, memory, stream/backpressure logs |
| Duplicate writes | idempotency key logs, retries, timeout timeline |
| Queue backlog | publish rate, consume rate, failure rate, DLQ |
| WebSocket disconnects | connection count, heartbeat, load balancer idle timeout |
| Auth bug | token verification, claims, clock skew, key rotation |
| CORS issue | Origin, preflight, credentials, response headers |

Interview line:

```text
I map the symptom to the layer: process, route, dependency, data, network, queue, or client contract.
Then I use metrics and traces to avoid debugging by intuition only.
```

---

## 4. Scenario: All Routes Suddenly Slow

Prompt:

```text
A Node API has high p99 latency across every endpoint, including /health.
```

Likely causes:

- Event-loop blocking.
- CPU-heavy synchronous work.
- Huge JSON parsing/stringifying.
- Garbage collection pressure.
- Container CPU throttling.
- Synchronous filesystem call in request path.
- Massive logging or serialization.
- Recent deploy introduced blocking code.

Evidence:

```text
Check event-loop delay, CPU usage, heap/GC, route latency, recent deploy, active handles, and CPU profile.
```

Immediate mitigation:

- Roll back recent deploy if correlated.
- Scale out replicas if CPU-bound and stateless.
- Disable expensive feature flag.
- Shed load or rate limit heavy route.

Durable fix:

- Move CPU-heavy work to workers/job system.
- Stream large payloads.
- Paginate large responses.
- Add event-loop delay alerts.
- Add load tests around expensive route.

Strong answer:

```text
Because even health checks are slow, I would suspect process-level event-loop blocking or CPU/GC
pressure rather than one downstream dependency. I would check event-loop delay and CPU profiles,
then isolate recent code that performs synchronous CPU-heavy work, huge JSON operations, sync fs,
or excessive serialization.
```

---

## 5. Scenario: One Endpoint Is Slow

Prompt:

```text
Only GET /reports is slow. Other routes are normal.
```

Likely causes:

- Slow database query.
- Missing index.
- Large unbounded result set.
- External API dependency.
- Huge JSON response.
- Route-specific CPU transformation.
- N+1 queries.
- No cache for expensive read.

Evidence:

- Trace spans for route.
- DB query plan and duration.
- Response payload size.
- Number of DB/external calls per request.
- Route-specific CPU profile.
- Pagination parameters.

Bad pattern:

```javascript
app.get("/reports", async (request, response) => {
    const rows = await db.query("select * from bookings");
    const enriched = await Promise.all(rows.map(row => enrich(row)));
    response.json(enriched);
});
```

Fix direction:

```text
Bound results, add indexes, reduce N+1 calls, limit concurrency, cache if safe, and move large
exports to background jobs.
```

Strong answer:

```text
If one route is slow, I isolate that route's dependency calls and payload size. I would look for
unbounded queries, missing indexes, N+1 calls, huge JSON serialization, or route-specific CPU work.
```

---

## 6. Scenario: Event Loop Blocked By Regex

Prompt:

```text
A login endpoint sometimes freezes the Node process when given unusual input.
```

Possible cause:

```text
Catastrophic backtracking in a vulnerable regular expression.
```

Bad:

```javascript
const pattern = /^([a-zA-Z]+)+$/;

function validateName(value) {
    return pattern.test(value);
}
```

Risk:

```text
Certain crafted inputs can cause regex evaluation to take a very long time, blocking the event loop.
```

Fixes:

- Use safer regex patterns.
- Limit input length before regex.
- Use proven validators.
- Add timeouts at request/gateway level.
- Fuzz/test validation inputs.

Strong answer:

```text
Regex can be CPU-heavy in JavaScript. I would check event-loop delay and CPU profiles, limit input
length, replace unsafe patterns, and add tests for malicious inputs.
```

---

## 7. Scenario: Huge JSON Body Freezes API

Prompt:

```text
A client sends a huge JSON body and the Node service becomes slow or crashes.
```

Root causes:

- No body size limit.
- JSON.parse is synchronous.
- Entire body buffered in memory.
- Validation traverses huge object.

Bad:

```javascript
app.use(express.json()); // default may not match your risk profile
```

Better:

```javascript
app.use(express.json({ limit: "1mb" }));
```

Manual idea:

```javascript
async function readJsonBody(request, limitBytes) {
    let size = 0;
    const chunks = [];

    for await (const chunk of request) {
        size += chunk.length;

        if (size > limitBytes) {
            throw new Error("payload_too_large");
        }

        chunks.push(chunk);
    }

    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}
```

Strong answer:

```text
Large JSON payloads are dangerous because buffering and JSON.parse can consume memory and block
the event loop. I would enforce body limits, validate content type, stream large uploads, and move
large data transfer to file/object-storage flows.
```

---

## 8. Scenario: Large Export Runs Out Of Memory

Prompt:

```text
Users export a million rows. The Node process gets OOM killed.
```

Bad pattern:

```javascript
app.get("/export", async (request, response) => {
    const rows = await db.query("select * from bookings");
    const csv = rows.map(toCsvLine).join("\n");
    response.send(csv);
});
```

Problems:

- Loads all rows into memory.
- Builds huge string in memory.
- Blocks event loop during transformation.
- Response to slow client increases memory pressure.

Better design:

```text
For very large exports, create an async export job, stream DB rows to object storage, then return
a download link when ready.
```

Streaming shape:

```javascript
import { pipeline } from "node:stream/promises";

app.get("/export", async (request, response, next) => {
    try {
        response.setHeader("Content-Type", "text/csv");
        await pipeline(
            createBookingRowStream(),
            createCsvTransformStream(),
            response
        );
    } catch (error) {
        next(error);
    }
});
```

Strong answer:

```text
The fix is not more memory. I would avoid materializing the whole export, use streaming with
backpressure, or move very large exports to background jobs and object storage.
```

---

## 9. Scenario: Slow Client Causes Memory Growth

Prompt:

```text
Downloads work in testing, but in production memory grows when clients have slow connections.
```

Root cause:

```text
The service writes faster than the client can consume, and backpressure is ignored.
```

Bad:

```javascript
readable.on("data", chunk => {
    response.write(chunk);
});
```

Better:

```javascript
import { pipeline } from "node:stream/promises";

await pipeline(readable, response);
```

Strong answer:

```text
Slow consumers create backpressure. I would use stream pipeline or proper write/drain handling so
Node does not buffer unbounded data in memory.
```

---

## 10. Scenario: Promise.all Overloads Downstream

Prompt:

```text
An endpoint receives 10,000 IDs and calls an external service for all of them using Promise.all.
The service times out and the dependency rate-limits us.
```

Bad:

```javascript
const results = await Promise.all(ids.map(id => fetchDetails(id)));
```

Root cause:

```text
Promise.all starts all work immediately. With unbounded input, it creates unbounded concurrency.
```

Fix:

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
Promise.all is safe only for bounded groups. For user-sized input, I would limit concurrency,
add pagination or batching, enforce request limits, and coordinate with downstream rate limits.
```

---

## 11. Scenario: Retry Storm During Provider Outage

Prompt:

```text
A payment provider is slow. Node services retry aggressively and make the outage worse.
```

Bad:

```javascript
async function callProvider(payload) {
    try {
        return await provider.charge(payload);
    } catch {
        return callProvider(payload);
    }
}
```

Problems:

- Infinite retry.
- No backoff.
- No jitter.
- No timeout.
- Duplicate side-effect risk.
- Amplifies downstream outage.

Better:

```javascript
async function retryWithBackoff(operation, options = {}) {
    const { attempts = 3, baseDelayMs = 100 } = options;
    let lastError;

    for (let attempt = 1; attempt <= attempts; attempt++) {
        try {
            return await operation();
        } catch (error) {
            lastError = error;

            if (attempt === attempts) {
                break;
            }

            const delayMs = baseDelayMs * 2 ** (attempt - 1) + Math.random() * 100;
            await delay(delayMs);
        }
    }

    throw lastError;
}
```

Strong answer:

```text
Retries should be bounded and polite. I would use timeouts, exponential backoff, jitter, retry
budgets, circuit breaking or load shedding, and idempotency keys for side-effecting operations.
```

---

## 12. Scenario: Duplicate Payment After Timeout

Prompt:

```text
Client times out waiting for POST /payments, retries, and the user is charged twice.
```

Root cause:

```text
Timeout does not mean the original request failed. It may have completed after the client gave up.
```

Bad:

```javascript
app.post("/payments", async (request, response) => {
    const payment = await chargeCard(request.body);
    response.status(201).json(payment);
});
```

Fix with idempotency key:

```javascript
app.post("/payments", async (request, response) => {
    const key = request.header("Idempotency-Key");

    if (!key) {
        response.status(400).json({ error: "idempotency_key_required" });
        return;
    }

    const result = await createPaymentOnce({
        idempotencyKey: key,
        payload: request.body
    });

    response.status(201).json(result);
});
```

Strong answer:

```text
For non-idempotent writes, retries must be protected server-side. I would require an idempotency
key, store the first result for that key, and return the same result for duplicate attempts.
```

---

## 13. Scenario: Database Pool Exhausted

Prompt:

```text
API latency spikes and logs show database pool timeout.
```

Likely causes:

- Connections not released.
- Queries too slow.
- Pool too small for normal workload.
- Pool too large and database overloaded.
- Long transactions.
- Unbounded request concurrency.
- Missing query timeout.

Bad leak:

```javascript
app.get("/bookings", async (request, response) => {
    const client = await pool.connect();
    const result = await client.query("select * from bookings");
    response.json(result.rows);
    // forgot client.release()
});
```

Fix:

```javascript
app.get("/bookings", async (request, response, next) => {
    const client = await pool.connect();

    try {
        const result = await client.query("select * from bookings limit 100");
        response.json(result.rows);
    } catch (error) {
        next(error);
    } finally {
        client.release();
    }
});
```

Strong answer:

```text
I would inspect pool active/idle/wait metrics, slow queries, and connection release paths. Pool
exhaustion is either too much demand, slow work, or leaked connections.
```

---

## 14. Scenario: Database Gets Overloaded After Scaling Node

Prompt:

```text
We increased Node replicas from 5 to 50. The database now struggles.
```

Root cause:

```text
Each Node replica has its own connection pool. Total possible DB connections increased massively.
```

Example:

```text
50 replicas * pool max 20 = 1000 possible database connections
```

Fixes:

- Reduce per-replica pool size.
- Use connection proxy/pooler if appropriate.
- Add read replicas for read-heavy traffic.
- Cache safe reads.
- Optimize queries.
- Apply backpressure/rate limits.
- Scale database intentionally.

Strong answer:

```text
Scaling stateless Node replicas can overload stateful dependencies. I would calculate total pool
capacity across replicas and tune it against database limits.
```

---

## 15. Scenario: N+1 Query Problem

Prompt:

```text
GET /orders is slow. It loads orders, then queries user and items for each order.
```

Bad:

```javascript
const orders = await db.query("select * from orders limit 100");

for (const order of orders.rows) {
    order.user = await loadUser(order.user_id);
    order.items = await loadItems(order.id);
}
```

Problem:

```text
1 query becomes 201 queries for 100 orders.
```

Fixes:

- Use joins when appropriate.
- Batch load by IDs.
- Use DataLoader-style batching.
- Return only needed fields.
- Cache stable reference data.

Strong answer:

```text
I would trace query count per request. N+1 often hides behind clean code but destroys latency at scale.
```

---

## 16. Scenario: Memory Grows Until Container Restarts

Prompt:

```text
Node service memory grows for hours until Kubernetes restarts the pod.
```

Check:

- `heapUsed` vs RSS.
- External memory / buffers.
- Heap snapshots over time.
- Recent deploys.
- Cache sizes.
- Listener counts.
- Timer/subscription cleanup.

Common leaks:

```javascript
const cache = new Map();

app.get("/profile/:id", async (request, response) => {
    const profile = await loadProfile(request.params.id);
    cache.set(request.params.id, profile);
    response.json(profile);
});
```

No TTL or max size.

Fix:

```text
Use bounded LRU/TTL caches, remove retained references, cleanup listeners/timers, and verify with
heap snapshots.
```

Strong answer:

```text
I would compare heap and RSS. If heap grows, I inspect heap snapshots for retainers. If RSS grows
while heap is stable, I investigate buffers, native memory, dependencies, and container behavior.
```

---

## 17. Scenario: EventEmitter Memory Leak Warning

Prompt:

```text
Logs show MaxListenersExceededWarning.
```

Likely cause:

```text
The service adds listeners repeatedly and does not remove them.
```

Bad:

```javascript
app.get("/events", (request, response) => {
    eventBus.on("booking.created", event => {
        response.write(JSON.stringify(event));
    });
});
```

Problems:

- Listener added per request.
- Listener may remain after client disconnects.
- Captures response object.

Fix:

```javascript
app.get("/events", (request, response) => {
    function handleEvent(event) {
        response.write(JSON.stringify(event));
    }

    eventBus.on("booking.created", handleEvent);

    request.on("close", () => {
        eventBus.off("booking.created", handleEvent);
    });
});
```

Strong answer:

```text
MaxListenersExceededWarning is often a leak signal. I would find where listeners are added per
request or per lifecycle and ensure cleanup on close/unmount/shutdown.
```

---

## 18. Scenario: In-Memory Cache Serves Wrong Tenant Data

Prompt:

```text
A multi-tenant API occasionally returns cached data from another tenant.
```

Root cause:

```text
Cache key does not include tenant/user/security context.
```

Bad:

```javascript
cache.set(`booking:${bookingId}`, booking);
```

Better:

```javascript
cache.set(`tenant:${tenantId}:booking:${bookingId}`, booking);
```

Also consider:

- Auth scope.
- Locale.
- Feature flag variant.
- Query params.
- User permissions.

Strong answer:

```text
Cache keys must include every dimension that changes the response. In multi-tenant systems, tenant
identity is mandatory to avoid data leakage.
```

---

## 19. Scenario: In-Memory Cache Breaks After Horizontal Scaling

Prompt:

```text
Feature flags or sessions work on one instance but behave inconsistently after scaling to many pods.
```

Root cause:

```text
Each Node process has separate memory. In-memory state is not shared across replicas.
```

Fixes:

- External cache/store like Redis.
- Database-backed session store.
- Gateway or flag service.
- Sticky sessions only when justified.
- Design for stateless app replicas.

Strong answer:

```text
In-memory state is per process. Once Node scales horizontally, shared state must move to a shared
store or the design must tolerate per-replica independence.
```

---

## 20. Scenario: Graceful Shutdown Drops Requests

Prompt:

```text
During deployment, users see failed requests even though readiness checks exist.
```

Likely causes:

- Process exits immediately on SIGTERM.
- Server still accepts traffic while shutting down.
- Load balancer sends traffic before readiness flips.
- DB connections close before in-flight requests finish.
- Shutdown grace period too short.

Graceful shape:

```javascript
process.on("SIGTERM", async () => {
    ready = false;

    server.close(async () => {
        await pool.end();
        process.exit(0);
    });

    setTimeout(() => process.exit(1), 30_000).unref();
});
```

Strong answer:

```text
Graceful shutdown should mark the instance not ready, stop accepting new requests, let in-flight
requests finish within a deadline, close resources, and then exit.
```

---

## 21. Scenario: Readiness Check Causes Outage

Prompt:

```text
Readiness endpoint checks database every second from every pod. During DB slowness, checks add load.
```

Problem:

```text
Health checks can become dependency load amplifiers.
```

Better:

- Separate liveness and readiness.
- Cache expensive readiness result briefly.
- Check lightweight dependency state.
- Avoid deep checks on every probe.
- Tune probe intervals and timeouts.

Strong answer:

```text
Readiness should protect traffic routing without overloading dependencies. I would avoid expensive
probe logic and cache dependency health briefly.
```

---

## 22. Scenario: Queue Backlog Grows

Prompt:

```text
A worker queue backlog grows and jobs are delayed by hours.
```

Possible causes:

- Publish rate exceeds consume rate.
- Workers are failing and retrying.
- Poison messages block progress.
- Downstream dependency slow.
- Worker concurrency too low.
- Job processing is CPU-bound.
- Queue visibility timeout misconfigured.

Evidence:

- Queue depth and age of oldest message.
- Success/failure rate.
- Retry count.
- DLQ size.
- Worker CPU/memory.
- Downstream latency.

Strong answer:

```text
I would compare enqueue rate, dequeue rate, processing latency, and failure rate. Then I would
scale workers, fix poison messages, tune retries, and protect downstream dependencies.
```

---

## 23. Scenario: Poison Message Retries Forever

Prompt:

```text
One malformed queue message keeps failing and retrying indefinitely.
```

Fixes:

- Validate message schema.
- Limit retry attempts.
- Send to dead-letter queue.
- Add error reason and message ID to logs.
- Build replay tooling.
- Make handlers idempotent.

Bad:

```javascript
queue.consume("booking.created", async message => {
    await sendEmail(message.email); // throws forever if email missing
});
```

Better:

```javascript
queue.consume("booking.created", async message => {
    const parsed = parseBookingCreated(message);
    await sendEmail(parsed.email);
});
```

Strong answer:

```text
Production queues need retry limits and dead-letter queues. A poison message should be isolated,
not allowed to block or burn the system forever.
```

---

## 24. Scenario: Webhook Processed Twice

Prompt:

```text
Payment provider sends the same webhook twice. The app ships two orders.
```

Root cause:

```text
Webhook delivery is often at-least-once. Duplicate events are normal.
```

Fix:

```javascript
app.post("/webhooks/payment", async (request, response) => {
    const event = verifyProviderSignature(request);

    if (await wasProcessed(event.id)) {
        response.status(200).json({ received: true });
        return;
    }

    await markProcessedAndHandle(event);
    response.status(200).json({ received: true });
});
```

Strong answer:

```text
Webhook consumers must be idempotent. I deduplicate by provider event ID, verify signature, and
make the business side effect safe to repeat.
```

---

## 25. Scenario: Webhook Signature Verification Fails

Prompt:

```text
Webhook verification fails only in production after adding JSON body parser middleware.
```

Likely cause:

```text
The provider signs the raw request body, but middleware parsed or modified it before verification.
```

Fix:

```text
Capture raw body for that route before JSON parsing, then verify signature against raw bytes.
```

Strong answer:

```text
Many webhook signatures require the exact raw body. I would ensure route-specific raw body parsing
runs before generic JSON parsing and verify timestamp to prevent replay.
```

---

## 26. Scenario: File Upload Blocks API

Prompt:

```text
Large file uploads make normal API requests slow.
```

Likely causes:

- Uploads buffered in app memory.
- CPU-heavy processing in request path.
- Same pods handle uploads and latency-sensitive APIs.
- No upload size limits.
- Disk temp files fill node storage.

Better architecture:

```text
Client -> pre-signed object-storage URL -> storage -> async scan/process worker -> metadata API
```

Strong answer:

```text
I would avoid routing huge file bytes through the Node API when possible. Direct-to-object-storage
plus async processing is usually more scalable and reliable.
```

---

## 27. Scenario: Uploaded File Path Traversal

Prompt:

```text
A file download endpoint takes a filename parameter. Security reports path traversal.
```

Bad:

```javascript
response.sendFile(`/uploads/${request.query.file}`);
```

Fix:

```javascript
import path from "node:path";

const root = path.resolve("/uploads");

function safePath(name) {
    const candidate = path.resolve(root, name);

    if (!candidate.startsWith(root + path.sep)) {
        throw new Error("invalid_path");
    }

    return candidate;
}
```

Strong answer:

```text
I never trust user-controlled file paths. I normalize the path, constrain it to an allowed root,
and prefer storing files by server-generated IDs instead of raw filenames.
```

---

## 28. Scenario: WebSocket Broadcast Missing Users

Prompt:

```text
A WebSocket broadcast reaches only some users after scaling to multiple Node replicas.
```

Root cause:

```text
Each replica only knows about its own in-memory WebSocket connections.
```

Fixes:

- Use Redis pub/sub or message broker for cross-replica broadcast.
- Store presence externally if needed.
- Use sticky sessions only when appropriate.
- Design reconnection behavior.
- Add heartbeat cleanup.

Strong answer:

```text
In multi-replica Node, WebSocket connection state is local to each process. Broadcast needs shared
pub/sub or a gateway layer so all replicas receive the event.
```

---

## 29. Scenario: WebSocket Reconnect Storm

Prompt:

```text
After a deploy, all clients reconnect immediately and overload the service.
```

Fixes:

- Client exponential backoff with jitter.
- Server connection rate limits.
- Rolling deploys.
- Graceful shutdown close codes.
- Load balancer timeout tuning.
- Queue or drop non-critical messages under pressure.

Strong answer:

```text
Reconnect storms are thundering herds. I would use backoff with jitter on clients, graceful deploys,
connection limits, and metrics around connection churn.
```

---

## 30. Scenario: Async Route Error Is Not Caught

Prompt:

```text
A rejected promise in an Express route causes unhandled rejection instead of normal error response.
```

Bad:

```javascript
app.get("/users/:id", async (request, response) => {
    const user = await loadUser(request.params.id);
    response.json(user);
});
```

Wrapper:

```javascript
function asyncHandler(handler) {
    return (request, response, next) => {
        Promise.resolve(handler(request, response, next)).catch(next);
    };
}
```

Use:

```javascript
app.get("/users/:id", asyncHandler(async (request, response) => {
    const user = await loadUser(request.params.id);
    response.json(user);
}));
```

Strong answer:

```text
Async errors need a reliable path to central error handling. Depending on framework/version, I use
an async wrapper or built-in promise-aware handlers and avoid unhandled rejections.
```

---

## 31. Scenario: Process Crashes On Uncaught Exception

Prompt:

```text
A rare uncaught exception crashes the Node process.
```

Correct mindset:

```text
After uncaught exception, process state may be unsafe. Log, drain if possible, exit, and let
supervisor restart.
```

Handler:

```javascript
process.on("uncaughtException", error => {
    logger.fatal({ error }, "uncaught exception");
    process.exit(1);
});
```

Strong answer:

```text
I do not try to continue indefinitely after an uncaught exception. I log it, shut down safely, and
restart a clean process while fixing the root cause.
```

---

## 32. Scenario: Background Email Lost

Prompt:

```text
API creates a booking, fires sendEmail in the background, returns success, but emails are sometimes lost.
```

Bad:

```javascript
app.post("/bookings", async (request, response) => {
    const booking = await createBooking(request.body);
    void sendEmail(booking);
    response.status(201).json(booking);
});
```

Problems:

- Process may crash before email sends.
- Promise rejection may be unhandled.
- No retry.
- No audit trail.

Better:

```javascript
app.post("/bookings", async (request, response) => {
    const booking = await createBooking(request.body);
    await queue.publish("booking.created", { bookingId: booking.id });
    response.status(201).json(booking);
});
```

Strong answer:

```text
Durable side effects should not rely on in-process fire-and-forget promises. I would publish a
queue event or use a transactional outbox so work survives process crashes.
```

---

## 33. Scenario: Transaction Commits But Queue Publish Fails

Prompt:

```text
Order is saved in DB, but publishing order.created event fails. Downstream never runs.
```

Root issue:

```text
Database write and queue publish are not atomic.
```

Fix pattern:

```text
Transactional outbox: write business row and outbox event in same DB transaction. A separate worker
publishes outbox events and marks them sent.
```

Strong answer:

```text
When DB state and messages must stay consistent, I use the transactional outbox pattern instead
of hoping the queue publish succeeds after commit.
```

---

## 34. Scenario: Rate Limiter Fails Across Replicas

Prompt:

```text
Rate limiting works locally but users can exceed limits in production with many pods.
```

Root cause:

```text
Each pod has its own in-memory counter.
```

Fixes:

- Redis-backed distributed limiter.
- API gateway limiter.
- Load balancer/WAF rules.
- Limit by user/API key/tenant, not only IP.
- Add route-specific limits.

Strong answer:

```text
In-memory rate limiting is per process. For multi-replica production, I use shared state or gateway
rate limiting and choose keys based on the abuse model.
```

---

## 35. Scenario: Auth Middleware Trusts Decoded JWT

Prompt:

```text
Code decodes JWT payload and trusts the user role without verifying signature.
```

Bad:

```javascript
const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url"));
request.user = payload;
```

Problem:

```text
Anyone can create a fake payload. Decoding is not verification.
```

Correct checks:

- Verify signature.
- Allowlist algorithm.
- Check issuer.
- Check audience.
- Check expiration.
- Handle key rotation.

Strong answer:

```text
JWT decoding only reads claims. It does not prove authenticity. I verify signature and claims before
trusting identity or permissions.
```

---

## 36. Scenario: JWT Key Rotation Breaks Login

Prompt:

```text
After identity provider rotates signing keys, API rejects valid tokens.
```

Likely causes:

- JWKS cache never refreshes.
- Unknown `kid` not handled.
- Clock skew too strict.
- Wrong issuer/audience config.

Fixes:

- Cache JWKS with TTL.
- Refresh on unknown key ID.
- Respect cache headers.
- Alert on verification failures.
- Validate issuer/audience.

Strong answer:

```text
JWT verification needs key rotation support. I cache JWKS carefully and refresh when an unknown
kid appears, while still validating issuer, audience, and expiration.
```

---

## 37. Scenario: CORS Fails With Cookies

Prompt:

```text
Browser does not send auth cookies to Node API, but Postman works.
```

Likely causes:

- Frontend missing `credentials: "include"`.
- Server missing `Access-Control-Allow-Credentials: true`.
- Server uses wildcard origin with credentials.
- Cookie SameSite/Secure mismatch.
- Domain/path mismatch.

Server response must not be wildcard for credentials:

```text
Access-Control-Allow-Origin: https://app.example.com
Access-Control-Allow-Credentials: true
```

Strong answer:

```text
CORS with cookies requires browser fetch credentials, explicit allowed origin, allow-credentials
header, and correct cookie attributes. Postman does not prove browser CORS is configured.
```

---

## 38. Scenario: Logs Leak Tokens

Prompt:

```text
Production logs contain Authorization headers and request bodies.
```

Risks:

- Token theft.
- PII leakage.
- Compliance incident.
- Vendor log exposure.

Bad:

```javascript
logger.info({ headers: request.headers, body: request.body }, "request");
```

Better:

```javascript
logger.info({
    requestId: request.id,
    method: request.method,
    path: request.path,
    userId: request.user?.id,
    statusCode: response.statusCode
}, "request completed");
```

Strong answer:

```text
Logs should be useful and safe. I log request IDs, route, status, latency, and safe identifiers,
while redacting tokens, secrets, and unnecessary PII.
```

---

## 39. Scenario: Source Maps Expose Source Code

Prompt:

```text
Production Node build publishes source maps publicly or logs full source paths.
```

Risk depends on environment, but concerns include:

- Exposing source internals.
- Revealing paths/config names.
- Making exploit research easier.

Balanced answer:

```text
Source maps are useful for debugging but should be controlled. For backend services, I would keep
source maps available to observability/error tooling without exposing them publicly.
```

---

## 40. Scenario: npm Dependency Breaks Production

Prompt:

```text
A patch dependency release breaks the service during deployment.
```

Likely causes:

- No lockfile or lockfile ignored.
- Using `npm install` instead of `npm ci` in CI.
- Floating versions.
- Transitive package changed.
- Missing integration tests.

Fixes:

- Commit lockfile.
- Use `npm ci`.
- Pin Node version.
- Add dependency update automation with CI.
- Use canary deploys.
- Monitor runtime errors after release.

Strong answer:

```text
Node dependency versions are part of the production artifact. I use lockfiles, npm ci, pinned Node
versions, tests, and gradual rollout to reduce dependency surprise.
```

---

## 41. Scenario: Prototype Pollution Attack

Prompt:

```text
Security report shows an API can set __proto__ through JSON input.
```

Risk:

```text
Unsafe deep merge can modify prototypes or create unexpected inherited properties.
```

Bad:

```javascript
function merge(target, source) {
    for (const key in source) {
        if (typeof source[key] === "object") {
            target[key] = target[key] ?? {};
            merge(target[key], source[key]);
        } else {
            target[key] = source[key];
        }
    }
}
```

Defenses:

- Schema validation.
- Reject `__proto__`, `constructor`, `prototype` keys.
- Avoid unsafe deep merge.
- Use patched libraries.
- Use null-prototype objects when appropriate.

Strong answer:

```text
Prototype pollution is a JavaScript-specific backend risk. I prevent it by validating input and
avoiding unsafe recursive merges of untrusted objects.
```

---

## 42. Scenario: SSRF Through URL Fetcher

Prompt:

```text
API accepts a URL and fetches it to generate a preview. Security flags SSRF.
```

Bad:

```javascript
app.post("/preview", async (request, response) => {
    const result = await fetch(request.body.url);
    response.send(await result.text());
});
```

Attack examples:

```text
http://localhost:8080/admin
http://169.254.169.254/latest/meta-data
```

Defenses:

- Allowlist domains when possible.
- Reject private/internal IP ranges.
- Resolve DNS carefully and guard redirects.
- Set timeouts and size limits.
- Do not send internal credentials.
- Isolate fetcher network permissions.

Strong answer:

```text
Server-side URL fetching can become SSRF. I would allowlist destinations or block private networks,
handle redirects carefully, and run fetchers with restricted network access.
```

---

## 43. Scenario: Compression Makes API Slower

Prompt:

```text
After enabling gzip for all responses, CPU increases and latency worsens.
```

Root cause:

```text
Compression saves bandwidth but costs CPU. Compressing small or already compressed payloads can be wasteful.
```

Fixes:

- Compress only above size threshold.
- Avoid compressing images/videos.
- Let CDN/gateway handle static compression.
- Measure CPU and latency impact.
- Consider brotli/gzip levels carefully.

Strong answer:

```text
Compression is a trade-off. I would measure payload size, bandwidth, CPU, and latency, then apply
compression selectively instead of blindly compressing everything.
```

---

## 44. Scenario: Slow Startup Causes Failed Deploy

Prompt:

```text
New pods fail readiness because Node app takes too long to initialize.
```

Possible causes:

- Heavy startup DB migrations.
- Loading huge config/cache synchronously.
- Network calls during boot.
- Dependency not ready.
- Probe timeout too short.
- Top-level await blocks startup.

Fixes:

- Move migrations out of app startup.
- Lazy load non-critical caches.
- Fail fast for required config.
- Tune startup probes.
- Make readiness true only after required resources are ready.

Strong answer:

```text
Startup should initialize only what is required to safely serve traffic. Expensive one-off work
belongs in jobs or deploy steps, not every app process boot.
```

---

## 45. Scenario: Environment Variable Missing In Production

Prompt:

```text
Service starts but fails requests because DATABASE_URL is undefined.
```

Bad:

```javascript
const databaseUrl = process.env.DATABASE_URL;
```

Better:

```javascript
function requireEnv(name) {
    const value = process.env[name];

    if (!value) {
        throw new Error(`Missing env var: ${name}`);
    }

    return value;
}
```

Strong answer:

```text
I validate required configuration at startup and fail fast before accepting traffic. Runtime config
errors should not appear first as random request failures.
```

---

## 46. Scenario: Clock Skew Breaks Tokens

Prompt:

```text
Some valid JWTs are rejected as not yet valid or expired across pods.
```

Likely causes:

- Clock skew between servers.
- Too-strict `nbf` or `exp` checks.
- Bad NTP sync.
- Long request time with near-expiry token.

Fixes:

- Ensure time sync.
- Allow small clock tolerance.
- Use reasonable token lifetimes.
- Monitor auth failure categories.

Strong answer:

```text
Token validation depends on time. I would verify server clock sync and allow small clock tolerance
while still enforcing expiration and issuer/audience checks.
```

---

## 47. Scenario: External API Hangs Requests

Prompt:

```text
When a vendor API is slow, Node requests hang until gateway timeout.
```

Root cause:

```text
No dependency timeout or cancellation.
```

Fix:

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
Every dependency call needs a timeout shorter than the caller's timeout, so the service can fail
fast, free resources, and return a controlled response.
```

---

## 48. Scenario: Circuit Breaker Needed

Prompt:

```text
A dependency is failing. Continued calls waste resources and increase latency.
```

Circuit breaker idea:

```text
closed -> calls allowed
open -> fail fast temporarily
half-open -> test limited calls
```

Use when:

- Dependency failure is widespread.
- Calls are expensive.
- Fast fallback is better than waiting.
- You need time for dependency recovery.

Caution:

```text
Circuit breakers add state and tuning complexity. They should be backed by metrics and sensible fallbacks.
```

Strong answer:

```text
A circuit breaker protects the service when a dependency is unhealthy by failing fast instead of
letting every request wait and pile up.
```

---

## 49. Scenario: Request ID Missing Across Logs

Prompt:

```text
Production logs cannot correlate all lines for one request.
```

Fix:

```javascript
import { randomUUID } from "node:crypto";

function requestIdMiddleware(request, response, next) {
    request.id = request.header("X-Request-Id") ?? randomUUID();
    response.setHeader("X-Request-Id", request.id);
    next();
}
```

Strong answer:

```text
Every request should have a correlation ID propagated through logs, responses, and downstream
calls so incidents can be traced end to end.
```

---

## 50. Scenario: Traces Show Time In Unknown Gap

Prompt:

```text
Distributed tracing shows missing spans around a slow external API call.
```

Likely cause:

```text
The service does not instrument dependency calls or context propagation is broken.
```

Fixes:

- Instrument HTTP clients and DB clients.
- Propagate trace headers.
- Name spans consistently.
- Add error attributes.
- Sample intelligently.

Strong answer:

```text
Tracing is only useful if dependency boundaries are instrumented. I would add spans around DB,
HTTP, cache, and queue calls and propagate trace context downstream.
```

---

## 51. Scenario: Metrics Cardinality Explosion

Prompt:

```text
Metrics backend becomes expensive after adding userId and raw URL labels.
```

Root cause:

```text
High-cardinality labels create too many time series.
```

Bad:

```text
http_request_duration{path="/users/123456/orders", userId="u123"}
```

Better:

```text
http_request_duration{route="/users/:id/orders", method="GET", status="200"}
```

Strong answer:

```text
Metrics labels should be bounded. I avoid user IDs, raw URLs, request IDs, and unbounded values as
metric labels, using logs/traces for high-cardinality debugging.
```

---

## 52. Scenario: Worker Thread Pool Saturated

Prompt:

```text
CPU-heavy jobs moved to worker threads, but latency is still high under load.
```

Likely causes:

- Too many worker jobs queued.
- Worker count too high causing CPU contention.
- Serialization overhead too large.
- Job should be async/offline instead of request path.
- No timeout/cancellation.

Fixes:

- Use bounded worker pool.
- Queue jobs with limits.
- Move large jobs to background workers.
- Transfer ArrayBuffers when appropriate.
- Monitor queue wait time and job duration.

Strong answer:

```text
Worker threads prevent main event-loop blocking, but CPU is still finite. I would bound the worker
pool and monitor worker queue wait time, not create unlimited workers.
```

---

## 53. Scenario: Child Process Command Injection

Prompt:

```text
API calls a shell command with user input and security reports command injection.
```

Bad:

```javascript
import { exec } from "node:child_process";

exec(`convert ${request.body.fileName} output.png`);
```

Better:

```javascript
import { execFile } from "node:child_process";

execFile("convert", [safeInputFile, "output.png"], {
    timeout: 5000
});
```

Strong answer:

```text
I avoid shell string construction with untrusted input. I use execFile or spawn with validated
arguments, timeouts, and resource limits.
```

---

## 54. Scenario: Serverless Node Cold Start

Prompt:

```text
A Node function has slow first request latency.
```

Likely causes:

- Large dependency graph.
- Heavy top-level initialization.
- DB connection setup.
- Bundle too large.
- VPC/network setup.
- Cold start after idle.

Fixes:

- Reduce bundle and dependencies.
- Lazy load non-critical modules.
- Reuse clients across invocations.
- Keep initialization minimal.
- Provision concurrency if needed.

Strong answer:

```text
Cold start latency comes from runtime startup, code loading, and initialization. I would reduce
bundle size, avoid heavy top-level work, and reuse connections where the platform allows.
```

---

## 55. Scenario: Timezone Bug In Backend Dates

Prompt:

```text
Bookings created for one date appear as previous day for some users.
```

Cause:

```text
Date-only business values are converted through UTC/local Date objects without explicit semantics.
```

Bad:

```javascript
const checkIn = new Date(request.body.checkInDate);
```

Better:

```javascript
const checkInDate = request.body.checkInDate; // YYYY-MM-DD date-only string after validation
```

Strong answer:

```text
For date-only values, I avoid accidental timezone conversion. I store and validate them as calendar
dates, and use explicit timezone rules when a timestamp is required.
```

---

## 56. Scenario: Money Precision Bug

Prompt:

```text
Order totals sometimes show 0.30000000000000004 or wrong cents.
```

Cause:

```text
JavaScript Number is floating-point and not safe for decimal money arithmetic.
```

Bad:

```javascript
const total = 0.1 + 0.2;
```

Better:

```javascript
const totalCents = items.reduce((sum, item) => sum + item.priceCents, 0);
```

Strong answer:

```text
For money, I use integer minor units like cents or a decimal-safe library, and I keep rounding
rules explicit at business boundaries.
```

---

## 57. Scenario: API Returns Too Much Data

Prompt:

```text
Mobile clients are slow because API response includes huge nested objects they do not use.
```

Fixes:

- Return only needed fields.
- Add pagination.
- Avoid deep over-fetching.
- Split endpoints by use case if needed.
- Use GraphQL/field selection only if it fits team maturity.
- Compress carefully.

Strong answer:

```text
Backend performance is also client performance. I would reduce payload size, bound lists, avoid
unnecessary nested data, and measure response size and parse cost.
```

---

## 58. Scenario: Static Assets Served By Node API

Prompt:

```text
Node API serves static assets and API latency worsens during traffic spikes.
```

Better design:

```text
Static assets -> CDN/object storage
Dynamic API -> Node service
```

Why:

- CDN handles caching and edge delivery.
- Node CPU/event loop stays for dynamic work.
- Better compression and range support.
- Lower latency globally.

Strong answer:

```text
For production, static assets usually belong behind a CDN or object storage, not the same Node
process handling latency-sensitive APIs.
```

---

## 59. Scenario: API Versioning Breaks Clients

Prompt:

```text
Backend changes response field names and older frontend clients break.
```

Fixes:

- Additive changes by default.
- Version breaking changes.
- Contract tests.
- OpenAPI/GraphQL schema discipline.
- Deprecation windows.
- Consumer-driven contracts.

Strong answer:

```text
APIs are contracts. I avoid breaking response shapes without versioning or migration, and I use
contract tests to catch compatibility issues.
```

---

## 60. Mini Program: Scenario-Ready API Handler

```javascript
function createRoute(handler) {
    return async function route(request, response, next) {
        const start = Date.now();

        try {
            await handler(request, response);
        } catch (error) {
            next(error);
        } finally {
            request.log?.info({
                requestId: request.id,
                method: request.method,
                route: request.route?.path,
                statusCode: response.statusCode,
                durationMs: Date.now() - start
            }, "request completed");
        }
    };
}
```

Why this is strong:

- Routes async errors to central handler.
- Logs request completion.
- Keeps handler code clean.
- Encourages consistent observability.

---

## 61. Mini Program: Idempotency Store Shape

```javascript
class InMemoryIdempotencyStore {
    constructor() {
        this.records = new Map();
    }

    async runOnce(key, operation) {
        const existing = this.records.get(key);

        if (existing) {
            return existing;
        }

        const result = await operation();
        this.records.set(key, result);
        return result;
    }
}
```

Production note:

```text
This in-memory version is only for learning. Production needs durable shared storage and atomic
claim/result behavior across replicas.
```

---

## 62. Mini Program: Bounded Queue Worker

```javascript
async function processJobs({ queue, handler, concurrency }) {
    let stopped = false;

    async function worker() {
        while (!stopped) {
            const job = await queue.take();

            try {
                await handler(job);
                await queue.ack(job);
            } catch (error) {
                await queue.fail(job, error);
            }
        }
    }

    const workers = Array.from({ length: concurrency }, () => worker());

    return {
        stop: async () => {
            stopped = true;
            await Promise.allSettled(workers);
        }
    };
}
```

Interview note:

```text
Real workers need graceful shutdown, visibility timeout handling, retry limits, DLQ, idempotency,
and metrics for queue depth and job age.
```

---

## 63. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Debugging by guessing | Wastes time | Start from metrics/traces/logs |
| Ignoring event-loop delay | Misses Node-specific bottleneck | Monitor event-loop lag |
| Treating timeout as failure | Can duplicate side effects | Use idempotency |
| Unbounded Promise.all | Overloads memory/dependencies | Limit concurrency |
| Loading huge files into memory | OOM risk | Stream or background job |
| Ignoring backpressure | Memory growth | Use pipeline/streams |
| No body size limit | Memory DoS | Enforce parser limits |
| Fire-and-forget durable work | Lost side effects | Queue/outbox |
| Retrying forever | Retry storm | Bounded backoff with jitter |
| In-memory state across pods | Inconsistent production behavior | Shared store/stateless design |
| Missing graceful shutdown | Dropped requests | Drain and close resources |
| Trusting decoded JWT | Auth bypass | Verify signature and claims |
| Logging secrets | Security incident | Redact and log safe context |
| Unsafe shell exec | Command injection | execFile/spawn with safe args |
| Unsafe URL fetch | SSRF | Allowlist/block private networks |
| Unsafe deep merge | Prototype pollution | Validate and safe merge |
| High-cardinality metrics | Metrics cost/explosion | Use bounded labels |
| Missing contract tests | Client breakage | Validate API compatibility |

---

## 64. Strong Interview Answers

### How do you debug Node p99 latency?

```text
I first determine whether the slowdown is global or route-specific. If all routes, including health,
are slow, I check event-loop delay, CPU, GC, memory, and recent deploys. If one route is slow, I
trace its DB/external calls, payload size, and route-specific CPU work.
```

### How do you prevent event-loop blocking?

```text
I avoid long synchronous work in request paths, keep JSON payloads bounded, stream large data,
move CPU-heavy work to worker threads or job workers, and monitor event-loop delay in production.
```

### How do you make retries safe?

```text
Retries need timeouts, bounded attempts, exponential backoff, jitter, and idempotency for side
effects. I do not blindly retry payments or orders unless the server uses idempotency keys.
```

### How do you debug memory leaks?

```text
I compare heapUsed, RSS, external memory, and GC behavior. Then I take heap snapshots over time
and look for retainers like unbounded caches, listeners, timers, request objects, buffers, and closures.
```

### How do you handle large exports?

```text
I avoid loading the entire export into memory. I either stream with backpressure or move the export
to an async job that writes to object storage and returns a download link.
```

### How do you design reliable background work?

```text
I avoid in-process fire-and-forget for durable work. I use queues or transactional outbox patterns,
make consumers idempotent, configure retries and DLQs, and monitor queue depth and job age.
```

---

## 65. MAANG Scenario 1: Global Latency Spike

> Your Node service has p99 latency above 10 seconds for every route. The database looks normal.

Strong answer:

```text
If the database is normal and all routes are slow, I would suspect event-loop blocking, CPU, GC,
or process-level resource exhaustion. I would check event-loop delay, CPU profiles, heap/RSS,
GC pauses, recent deploys, and whether health checks are also delayed.

The immediate mitigation could be rollback, disabling an expensive feature, or scaling replicas.
The durable fix depends on root cause: move CPU work to workers, bound payloads, stream large data,
remove sync filesystem/crypto work, or fix memory pressure. I would add event-loop delay alerts.
```

---

## 66. MAANG Scenario 2: Checkout Duplicate Charge

> A checkout API sometimes charges users twice after network timeouts.

Strong answer:

```text
A client timeout does not prove the server-side charge failed. The first request may have completed
after the client gave up. For payment APIs, I would require idempotency keys and persist the first
result for that key so retries return the same outcome.

I would also use clear dependency timeouts, avoid blind retries, log request and idempotency IDs,
and reconcile with the payment provider. The frontend can disable buttons, but the backend must
provide the real duplicate-charge protection.
```

---

## 67. MAANG Scenario 3: Queue Backlog Incident

> Booking confirmation emails are delayed by six hours. Queue depth keeps growing.

Strong answer:

```text
I would compare publish rate, consume rate, processing latency, failure rate, retry count, and age
of oldest message. Then I would inspect worker logs and downstream email provider latency.

If workers are failing on poison messages, I would add schema validation and DLQ handling. If
consume capacity is too low, I would scale workers carefully while respecting provider rate limits.
All consumers should be idempotent because retries and duplicate deliveries are expected.
```

---

## 68. MAANG Scenario 4: Multi-Replica WebSocket Bug

> After scaling from one Node pod to ten, real-time notifications reach only some users.

Strong answer:

```text
Each Node process only knows about the WebSocket connections attached to it. With ten pods, a
broadcast emitted in one pod reaches only that pod's clients unless there is shared pub/sub.

I would introduce Redis pub/sub or a message broker for cross-replica broadcasts, add heartbeat
cleanup, handle reconnect with jitter, and monitor connection count by pod. Sticky sessions may
help connection affinity but do not solve global broadcast by themselves.
```

---

## 69. MAANG Scenario 5: Memory Leak Under Traffic

> A Node API's memory grows steadily and restarts every few hours.

Strong answer:

```text
I would first identify whether heapUsed, RSS, or external memory is growing. Heap growth points to
retained JS objects; RSS/external growth may involve buffers, native modules, or unmanaged resources.

Then I would take heap snapshots over time and look for retainers such as unbounded caches,
request objects stored globally, listeners not removed, timers, closures, or large buffers. The
fix is to remove the retaining reference, add TTL/max size, cleanup resources, and add memory metrics.
```

---

## 70. MAANG Scenario 6: Dependency Outage Amplified By Retries

> A downstream service is slow. Your Node service retries and causes more traffic than normal.

Strong answer:

```text
This is retry amplification. I would add dependency timeouts, bounded retries with exponential
backoff and jitter, retry budgets, and circuit breaking or load shedding when the dependency is
unhealthy. For non-idempotent operations, retries require idempotency keys.

I would also respect 429 and Retry-After headers, monitor retry counts, and coordinate behavior
with queues so retries do not pile up and overload the dependency further.
```

---

## 71. Rapid Revision

- Start scenario answers with impact and scope.
- If all routes are slow, suspect process-level issues.
- If health checks are slow, suspect event-loop blocking or CPU/GC pressure.
- If one route is slow, inspect route dependencies, query count, payload size, and CPU work.
- Event-loop delay is a critical Node production metric.
- CPU-heavy JavaScript blocks unrelated requests in the same process.
- Regex can block the event loop if vulnerable to catastrophic backtracking.
- Large JSON parse/stringify is synchronous and can block.
- Body size limits protect memory and CPU.
- Large exports should stream or run as async jobs.
- Backpressure matters with slow clients.
- Promise.all over unbounded input is dangerous.
- Retries need backoff, jitter, limits, and idempotency.
- Timeout does not mean side effect failed.
- Payments/orders need server-side idempotency.
- DB pool exhaustion means slow work, leaked connections, or too much demand.
- Scaling Node replicas multiplies database pool capacity.
- N+1 queries hide behind innocent loops.
- Memory leaks require heap/RSS/external analysis.
- EventEmitter warnings often signal listener leaks.
- Cache keys must include tenant/security context.
- In-memory state is per process and breaks across replicas.
- Graceful shutdown drains requests before exit.
- Readiness checks should not overload dependencies.
- Queue backlog needs publish/consume/failure analysis.
- Poison messages need retry limits and DLQ.
- Webhook delivery is at least once; consumers must be idempotent.
- Webhook signatures may require raw body.
- Large uploads should stream or go direct to object storage.
- User file paths must be constrained to safe roots.
- WebSocket broadcasts across replicas need pub/sub.
- Async route errors need central error handling.
- Uncaught exceptions should usually terminate the process after logging.
- Durable background work needs queues or outbox.
- DB write plus queue publish consistency needs outbox pattern.
- Distributed rate limiting needs shared state or gateway support.
- JWT decoding is not verification.
- JWT key rotation needs JWKS refresh logic.
- CORS with cookies needs credentials and explicit origin.
- Logs must not leak tokens or PII.
- Lockfiles and npm ci reduce dependency surprises.
- Prototype pollution comes from unsafe object merging.
- SSRF comes from server-side fetching of untrusted URLs.
- Compression is CPU vs bandwidth trade-off.
- Startup work should be minimal and readiness-aware.
- Env vars should be validated at startup.
- Clock skew affects token validation.
- Circuit breakers fail fast during dependency outages.
- Request IDs connect logs, traces, and support debugging.
- Metrics labels must be bounded.
- Worker threads still need bounded pools.
- Shell commands need safe args, not string interpolation.
- Money should use integer minor units or decimal-safe math.
- API responses should be bounded and contract-safe.

---

## 72. Official Source Notes

Use these sources when refreshing Node.js scenario knowledge:

- Node.js docs: `https://nodejs.org/docs/latest/api/`
- Node.js event loop guide: `https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick`
- Node.js diagnostics guide: `https://nodejs.org/en/learn/diagnostics`
- Node.js stream docs: `https://nodejs.org/docs/latest/api/stream.html`
- Node.js worker_threads docs: `https://nodejs.org/docs/latest/api/worker_threads.html`
- Node.js child_process docs: `https://nodejs.org/docs/latest/api/child_process.html`
- Node.js perf_hooks docs: `https://nodejs.org/docs/latest/api/perf_hooks.html`
- Express error handling: `https://expressjs.com/en/guide/error-handling.html`
- Fastify docs: `https://fastify.dev/`
- OWASP API Security Top 10: `https://owasp.org/API-Security/`
- OWASP SSRF: `https://owasp.org/www-community/attacks/Server_Side_Request_Forgery`
- OWASP Prototype Pollution prevention: `https://cheatsheetseries.owasp.org/cheatsheets/Prototype_Pollution_Prevention_Cheat_Sheet.html`
- npm docs: `https://docs.npmjs.com/`

Interview safety line:

```text
For Node.js interview scenarios, I connect symptoms to runtime mechanics and production controls:
event-loop health, bounded concurrency, backpressure, validation, timeouts, retries, idempotency,
queues, observability, and graceful failure.
```