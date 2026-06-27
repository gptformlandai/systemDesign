# React + Next.js Real-World Projects - Gold Sheet

> Track File #23 of 24 - Group 8: Projects And Interview Prep
> Covers: SaaS dashboard, ecommerce app, GenAI frontend, design decisions

---

## 1. Project 1: SaaS Dashboard Architecture

### Requirements

- login/session
- dashboard shell
- tenant/account switching
- charts and tables
- filters in URL
- role-based access
- notifications
- audit-friendly actions

### Architecture

```text
Next App Router
  app/(dashboard)/layout.tsx
  server session check
  sidebar/header layout
  feature modules
  client widgets for charts/tables
  server actions for mutations
  TanStack Query for interactive widgets
```

### Design Decisions

- SSR layout checks auth and tenant access.
- URL search params own filters and pagination.
- Server Components fetch stable initial data.
- Client Components own charts, table interactions, and optimistic low-risk actions.
- Feature flags gate new dashboard modules.
- Error boundaries isolate broken widgets.

### Mistakes To Avoid

- client-only auth checks
- filters only in local state
- giant dashboard client component
- no loading/error states per widget
- no tenant authorization on server

### Interview Answer

```text
For a SaaS dashboard, I use a server-rendered authenticated shell and client
islands for highly interactive widgets. Filters live in URL params, data fetching
uses server components for initial data and query cache for interactive refresh.
Authorization is enforced on the server per tenant. Observability tracks widget
load time, API failures, and release version.
```

---

## 2. Project 2: Ecommerce App

### Requirements

- public catalog
- product detail pages
- search/filter
- cart
- auth
- checkout
- payment status
- admin catalog updates

### Architecture

```text
Catalog/product pages:
  ISR + CDN + image optimization

Cart:
  client drawer for fast UX
  server/session source of truth

Checkout:
  server-side validation
  payment provider integration
  idempotency keys
  no optimistic payment success
```

### Design Decisions

- Product pages use ISR for SEO and cost.
- Catalog update triggers tag/path revalidation.
- Cart uses client UI but reconciles with server.
- Checkout uses Server Action or route handler with auth and idempotency.
- Product images use `next/image`.

### Mistakes To Avoid

- public caching personalized cart
- optimistic payment success
- missing product image sizes
- no cache invalidation after price update
- trusting client-calculated totals

### Interview Answer

```text
For ecommerce, public catalog and product pages are strong ISR candidates because
they need SEO, speed, and controlled freshness. The cart can feel instant on the
client, but the server/session remains source of truth. Checkout validates prices,
inventory, user, and payment server-side with idempotency. Cache invalidation is
tag-based after catalog changes.
```

---

## 3. Project 3: GenAI Frontend

### Requirements

- chat UI
- streaming responses
- prompt input
- conversation history
- auth and quotas
- cancellation
- retry
- safety/error messages

### Architecture

```text
Next page shell
  authenticated route
  chat client component
  server route/action calls model gateway
  streaming response to client
  conversation persistence
  observability for latency/errors/tokens
```

### Design Decisions

- Chat surface is a Client Component because it handles input, streaming tokens, scroll, and cancellation.
- Server route or action protects API keys and enforces quotas.
- Streaming improves perceived latency.
- Conversation persistence happens server-side.
- UI supports retry, stop generation, partial response, and error recovery.

### Mistakes To Avoid

- exposing LLM API key in browser
- no cancellation path
- blocking UI until full answer completes
- no rate limit/quota
- no trace ID for debugging slow generations
- treating partial streamed text as final without state

### Minimal Streaming UI Shape

```tsx
'use client';

function ChatBox() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [pending, setPending] = useState(false);

  async function send(prompt: string) {
    setPending(true);
    const response = await fetch('/api/chat', {
      method: 'POST',
      body: JSON.stringify({prompt}),
    });

    // In production, read the stream and append chunks safely.
    const text = await response.text();
    setMessages(current => [...current, {role: 'assistant', content: text}]);
    setPending(false);
  }

  return <ChatView messages={messages} pending={pending} onSend={send} />;
}
```

### Interview Answer

```text
For a GenAI chat frontend, the UI is client-side because it manages prompt input,
streamed output, stop/retry, and scroll behavior. The model call goes through a
server route or BFF so API keys, quotas, safety checks, and logging stay server-side.
I stream tokens to reduce perceived latency, persist conversation state on the
server, and track request IDs, latency, token usage, and model errors for debugging.
```

---

## 4. Cross-Project Design Decisions

| Concern | SaaS Dashboard | Ecommerce | GenAI |
|---|---|---|---|
| Rendering | SSR shell + client widgets | ISR catalog + server checkout | SSR shell + client chat |
| Auth | server session/tenant | auth for cart/checkout | auth/quota |
| Server state | query widgets | catalog/cart/payment | conversation/history |
| Realtime | notifications optional | inventory/payment status | streaming required |
| Cache | private dashboard | public ISR + private cart | no public prompt cache |
| Risk | tenant data leak | payment correctness | API key/quota/safety |

---

## 5. Revision Notes

- One-line summary: Project design connects rendering, data, cache, auth, UX, and failure recovery.
- Three keywords: dashboard, ecommerce, GenAI.
- One interview trap: Never expose server/provider secrets in the browser.
- One memory trick: Public content can cache; private/critical actions need server truth.

