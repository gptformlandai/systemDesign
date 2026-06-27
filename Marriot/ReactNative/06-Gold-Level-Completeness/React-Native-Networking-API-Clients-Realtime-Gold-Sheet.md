# React Native Networking, API Clients, And Realtime - Gold Sheet

> Track File #17 of 20 - Group 6: Gold-Level Completeness
> Level: production mobile networking and interview-ready reliability

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Fetch and async networking | Very high | Every real app talks to backend services |
| API client abstraction | Very high | Prevents duplicated auth/error/retry code |
| Timeout and cancellation | Very high | Mobile networks are unreliable |
| Auth refresh | Very high | Common production failure source |
| HTTPS / cleartext restrictions | High | iOS and Android security policies matter |
| WebSockets/realtime | High | Chat, live orders, notifications, collaboration |
| Upload/download | Medium-high | Media-heavy mobile apps need this |
| Offline-aware networking | High | Mobile devices move through bad networks constantly |

MAANG signal:
You treat networking as a reliability layer, not just a `fetch()` call from a component.

---

## 2. Mental Model

Mobile networking has more failure modes than desktop web.

```text
User action
  -> API client
  -> auth/session layer
  -> timeout/cancellation/retry policy
  -> network transport
  -> backend
  -> response mapping
  -> cache/update UI
  -> telemetry
```

Production mobile networking must answer:
- What if the request is slow?
- What if the app backgrounds?
- What if the token expires?
- What if the user navigates away?
- What if the same mutation is retried?
- What if the device goes offline mid-upload?
- What if the backend returns an unknown response shape?

---

## 3. Fetch In React Native

React Native supports the Fetch API and asynchronous request handling.

Basic shape:

```ts
async function getProfile(userId: string): Promise<UserProfile> {
  const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Profile request failed: ${response.status}`);
  }

  return response.json() as Promise<UserProfile>;
}
```

Do not scatter this across screens. Wrap it.

---

## 4. Production API Client

```ts
type ApiRequestOptions = {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  signal?: AbortSignal;
  idempotencyKey?: string;
};

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const token = await tokenStore.getAccessToken();

  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    method: options.method ?? 'GET',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(token ? {Authorization: `Bearer ${token}`} : {}),
      ...(options.idempotencyKey
        ? {'Idempotency-Key': options.idempotencyKey}
        : {}),
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });

  if (response.status === 401) {
    throw new AuthExpiredError();
  }

  if (!response.ok) {
    throw await mapApiError(response);
  }

  return response.json() as Promise<T>;
}
```

What belongs in the client:
- base URL
- auth header injection
- request IDs/correlation IDs
- error mapping
- timeout/cancellation support
- idempotency header support
- safe logging/telemetry

What does not belong:
- screen-specific UI logic
- navigation logic
- feature-specific business decisions

---

## 5. Timeout And Cancellation

Why it matters:
`fetch` can hang longer than the user experience allows, and stale requests can update screens after the user has moved on.

Pattern:

```ts
export async function withTimeout<T>(
  operation: (signal: AbortSignal) => Promise<T>,
  timeoutMs: number,
): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await operation(controller.signal);
  } finally {
    clearTimeout(timeout);
  }
}
```

Use:

```ts
const profile = await withTimeout(
  signal => apiRequest<UserProfile>('/me', {signal}),
  10_000,
);
```

Interview point:
Cancellation is not just optimization. It prevents stale UI writes, wasted battery, and duplicate work.

---

## 6. Auth Refresh Flow

Naive flow:

```text
request -> 401 -> logout
```

Better flow:

```text
request -> 401
  -> single-flight refresh token request
  -> retry original request once
  -> if refresh fails, clear session and show login
```

Important:
- Only one refresh should run at a time.
- Pending requests should wait for refresh result.
- Retry only once to avoid infinite loops.
- Refresh tokens belong in secure storage.
- Never log tokens.

Strong answer:

```text
For token refresh, I use a single-flight refresh mechanism. The first 401 starts
a refresh, other requests await it, and then retry once with the new token. If
refresh fails, I clear session state and route to login. This prevents refresh
storms and avoids infinite retry loops.
```

---

## 7. HTTPS, Cleartext, And Native Security

Mobile networking is not exactly browser networking.

Key points:
- Use HTTPS in production.
- iOS App Transport Security expects secure connections unless exceptions are configured.
- Modern Android blocks cleartext traffic by default unless explicitly allowed.
- Native apps do not use the browser CORS security model in the same way.
- Certificate pinning can improve protection for high-risk apps, but adds operational risk during certificate rotation.

Interview maturity:
Do not disable transport security globally just to make local development easier. Use scoped development configuration.

---

## 8. WebSockets And Realtime

Use WebSockets for:
- chat
- live order status
- collaborative editing
- trading/price updates
- multiplayer/live presence

Realtime lifecycle:

```text
connect
  -> authenticate
  -> subscribe
  -> receive messages
  -> heartbeat/ping
  -> reconnect with backoff
  -> resubscribe
  -> catch up missed events
```

Common production requirements:
- reconnect with jitter
- app background handling
- token refresh while connected
- sequence numbers or event IDs
- missed event recovery
- server-side fanout limits
- battery/network awareness

WebSocket skeleton:

```ts
function connectOrdersSocket(token: string) {
  const socket = new WebSocket(`${config.wsBaseUrl}/orders?token=${token}`);

  socket.onopen = () => {
    socket.send(JSON.stringify({type: 'subscribe', channel: 'orders'}));
  };

  socket.onmessage = event => {
    const message = JSON.parse(event.data);
    orderEventStore.apply(message);
  };

  socket.onerror = error => {
    logger.warn('orders socket error', {message: error.message});
  };

  socket.onclose = event => {
    scheduleReconnect({code: event.code, reason: event.reason});
  };

  return socket;
}
```

Security note:
Avoid putting long-lived tokens in URLs when possible. Prefer short-lived socket auth tokens or an authenticated handshake.

---

## 9. Uploads And Downloads

Media upload flow:

```text
pick/capture file
  -> validate type/size
  -> compress/resize if needed
  -> request signed upload URL
  -> upload with progress
  -> notify backend
  -> show final state
```

Production concerns:
- large files need progress UI
- app background can interrupt upload
- retries require idempotency
- uploads should be cancelable
- avoid base64 in JS memory for large files
- use thumbnails/previews
- strip sensitive metadata when needed

---

## 10. Error Taxonomy

Use error categories, not random strings.

```ts
type ApiErrorKind =
  | 'network_unavailable'
  | 'timeout'
  | 'unauthorized'
  | 'forbidden'
  | 'not_found'
  | 'validation'
  | 'rate_limited'
  | 'server'
  | 'unknown';
```

Why:
- UI can show correct message.
- Telemetry can group failures.
- Retry logic can be safe.
- Tests can assert behavior.

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Calling `fetch` directly in every screen | Duplicates auth/error logic | Central API client |
| No timeout | User waits forever | Abort and show retry |
| Retrying unsafe mutations | Duplicate side effects | Use idempotency or do not retry |
| Refresh token storm | Backend load and race bugs | Single-flight refresh |
| WebSocket without catch-up | Missed events after reconnect | Use event IDs/resync |
| Logging full network payloads | PII/token leak | Redact and sample |

---

## 12. Strong Interview Answer

Question:
How would you design networking for a React Native app?

Strong answer:

```text
I centralize networking behind an API client that injects auth headers, handles
timeouts, cancellation, request IDs, error mapping, and safe telemetry. Server
state is consumed through a query/cache layer, while mutations define retry and
idempotency rules explicitly. For auth refresh, I use a single-flight refresh and
retry the original request once. For realtime, I design WebSocket reconnect,
heartbeat, token refresh, and missed-event recovery. I avoid logging PII and never
store or expose long-lived secrets in the mobile app.
```

---

## 13. Revision Notes

- One-line summary: Mobile networking needs API boundaries, cancellation, auth refresh, retry discipline, and realtime recovery.
- Three keywords: timeout, idempotency, single-flight refresh.
- One interview trap: Native apps do not follow browser CORS in the same way.
- One memory trick: Every request needs a policy: auth, timeout, retry, cache, telemetry.

