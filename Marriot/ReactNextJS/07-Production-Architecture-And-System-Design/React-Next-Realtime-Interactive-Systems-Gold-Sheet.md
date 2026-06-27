# React + Next.js Realtime And Interactive Systems - Gold Sheet

> Track File #17 of 24 - Group 7: Production Architecture And System Design
> Covers: WebSockets, polling vs realtime updates, optimistic UI

---

## 1. Intuition

Realtime systems keep UI synchronized with changing server state.

```text
polling: ask repeatedly
WebSocket: server pushes over persistent connection
optimistic UI: update before confirmation, then reconcile
```

The right choice depends on freshness, scale, complexity, and failure handling.

---

## 2. Polling

```tsx
useQuery({
  queryKey: ['order', orderId],
  queryFn: () => fetchOrder(orderId),
  refetchInterval: 5000,
});
```

Use polling when:
- updates are infrequent
- slight delay is acceptable
- simple infrastructure is preferred
- connection count must stay low

Trade-off:
Polling wastes requests when nothing changes and is less instant.

---

## 3. WebSockets

Use WebSockets when:
- chat
- live dashboard
- collaborative editing
- live order tracking
- notifications/presence

Lifecycle:

```text
connect -> authenticate -> subscribe -> receive -> heartbeat -> reconnect -> resync
```

Client skeleton:

```ts
const socket = new WebSocket(url);

socket.onmessage = event => {
  const message = JSON.parse(event.data);
  applyServerEvent(message);
};

socket.onclose = () => {
  scheduleReconnect();
};
```

Production needs:
- reconnect backoff
- heartbeat
- auth refresh
- missed-event recovery
- visibility/background handling
- server fanout limits

---

## 4. Optimistic UI

Optimistic UI updates before server confirmation.

Good for:
- likes
- toggles
- bookmarks
- low-risk comments with pending state

Bad for:
- payments
- irreversible actions
- inventory reservation
- legal submissions

Flow:

```text
snapshot cache -> apply optimistic change -> send mutation
  -> success: reconcile
  -> failure: rollback/show error
```

---

## 5. Real-World Use Cases

- Chat UI: WebSocket or SSE, message pending state, retry.
- Order tracking: polling may be enough; WebSocket if high freshness.
- GenAI streaming: streaming response chunks into UI.
- Stock ticker: WebSocket with sequence handling.
- Likes: optimistic update with rollback.

---

## 6. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| WebSockets for everything | Operational complexity | Poll if freshness allows |
| No reconnect strategy | UI silently stale | backoff/resubscribe/resync |
| Optimistic payment success | Serious correctness risk | wait for server confirmation |
| No event ordering | Out-of-order state | sequence/event IDs |
| Polling too frequently | backend load | adaptive interval or push |

---

## 7. Strong Interview Answer

Question:
Polling vs WebSockets: how do you choose?

Strong answer:

```text
I use polling when updates are infrequent, seconds of delay are acceptable, and
simple infrastructure is valuable. I use WebSockets when the product needs low
latency server push, such as chat, presence, or live collaboration. WebSockets
need reconnect, heartbeat, auth refresh, missed-event recovery, and backpressure.
Optimistic UI is separate: it improves perceived speed for safe reversible actions,
but I avoid it for payments and irreversible workflows.
```

---

## 8. Revision Notes

- One-line summary: Realtime is freshness plus failure recovery.
- Three keywords: polling, WebSocket, optimistic.
- One interview trap: Optimistic UI is not safe for every mutation.
- One memory trick: Push for instant, poll for simple, optimistic for reversible.

