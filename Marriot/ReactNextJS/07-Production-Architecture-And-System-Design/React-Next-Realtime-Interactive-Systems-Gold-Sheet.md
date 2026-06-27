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

---

## 9. WebSocket Production Patterns

### Complete WebSocket Client with Reconnect

```tsx
'use client';
import { useEffect, useRef, useState, useCallback } from 'react';

type WSStatus = 'connecting' | 'connected' | 'disconnected' | 'error';

function useWebSocket<T>(url: string, onMessage: (data: T) => void) {
  const wsRef = useRef<WebSocket | null>(null);
  const [status, setStatus] = useState<WSStatus>('disconnected');
  const reconnectTimeoutRef = useRef<NodeJS.Timeout>();
  const attemptRef = useRef(0);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;
    
    setStatus('connecting');
    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => {
      setStatus('connected');
      attemptRef.current = 0;
    };

    ws.onmessage = (event) => {
      try {
        onMessage(JSON.parse(event.data) as T);
      } catch {
        console.error('WS parse error');
      }
    };

    ws.onerror = () => setStatus('error');

    ws.onclose = () => {
      setStatus('disconnected');
      const delay = Math.min(1000 * 2 ** attemptRef.current, 30_000);  // exp backoff cap 30s
      attemptRef.current++;
      reconnectTimeoutRef.current = setTimeout(connect, delay);
    };
  }, [url, onMessage]);

  useEffect(() => {
    connect();
    return () => {
      clearTimeout(reconnectTimeoutRef.current);
      wsRef.current?.close();
    };
  }, [connect]);

  const send = useCallback((data: unknown) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data));
    }
  }, []);

  return { status, send };
}
```

### Next.js WebSocket — Use Separate Server

Next.js route handlers run in Edge or Node.js serverless functions that do not support persistent WebSocket connections. For production WebSocket, use:

1. A separate Node.js WebSocket server (Socket.io or ws)
2. Vercel's [`@vercel/node`](https://vercel.com/docs/functions) with WebSocket support
3. Cloud service: Ably, Pusher, Liveblocks, PartyKit

---

## 10. Server-Sent Events (SSE) — Simpler Alternative to WebSocket

SSE is one-way server → client streaming over HTTP. Simpler than WebSocket, works with regular HTTP/2, supported natively in Next.js route handlers.

```tsx
// app/api/events/route.ts — SSE stream
export async function GET() {
  const encoder = new TextEncoder();

  const stream = new ReadableStream({
    async start(controller) {
      function sendEvent(data: object) {
        controller.enqueue(encoder.encode(`data: ${JSON.stringify(data)}\n\n`));
      }

      // Subscribe to events
      const unsubscribe = eventBus.subscribe((event) => {
        sendEvent(event);
      });

      // Heartbeat to keep connection alive
      const heartbeat = setInterval(() => sendEvent({ type: 'ping' }), 30_000);

      // Cleanup when client disconnects
      await new Promise<void>((resolve) => {
        stream.cancel = () => {
          unsubscribe();
          clearInterval(heartbeat);
          resolve();
        };
      });
    },
  });

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
    },
  });
}

// Client-side EventSource
function useServerEvents(url: string, onEvent: (data: unknown) => void) {
  useEffect(() => {
    const es = new EventSource(url);
    es.onmessage = (e) => {
      const data = JSON.parse(e.data);
      if (data.type !== 'ping') onEvent(data);
    };
    return () => es.close();
  }, [url, onEvent]);
}
```

---

## 11. Optimistic UI — Production Implementation

```tsx
'use client';
import { useOptimistic, startTransition } from 'react';

type Comment = { id: string; text: string; authorId: string; pending?: boolean };

function CommentList({ initialComments, postId }: { initialComments: Comment[]; postId: string }) {
  const [comments, addOptimistic] = useOptimistic(
    initialComments,
    (state: Comment[], newComment: Comment) => [...state, newComment]
  );

  async function handleAddComment(formData: FormData) {
    const text = formData.get('text') as string;
    if (!text.trim()) return;

    const optimisticComment: Comment = {
      id: crypto.randomUUID(),
      text,
      authorId: 'me',
      pending: true,  // flag to show "sending..." style
    };

    startTransition(() => {
      addOptimistic(optimisticComment);
    });

    // Server Action — actual persist
    await addCommentAction(postId, text);
    // After action completes, React replaces optimistic state with server response
  }

  return (
    <>
      {comments.map(comment => (
        <div key={comment.id} style={{ opacity: comment.pending ? 0.6 : 1 }}>
          <p>{comment.text}</p>
          {comment.pending && <span>Sending...</span>}
        </div>
      ))}
      <form action={handleAddComment}>
        <input name="text" placeholder="Add comment" />
        <button type="submit">Send</button>
      </form>
    </>
  );
}
```

### When NOT to Use Optimistic UI

| Safe for optimistic | Unsafe for optimistic |
|---|---|
| Like/unlike (easy to reverse) | Payment submission |
| Add comment | Destructive deletes |
| Toggle feature flag | File transfer start |
| Wishlist toggle | Role/permission changes |

