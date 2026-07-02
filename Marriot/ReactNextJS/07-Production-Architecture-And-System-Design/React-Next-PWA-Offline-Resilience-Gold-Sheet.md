# React + Next.js PWA, Offline, And Client Resilience - Gold Sheet

> Track Module - Group 7: Production Architecture And System Design
> Level: intermediate -> senior | Progressive Web Apps, service workers, offline UX, sync queues, storage, and resilient client behavior

---

## 1. Intuition

The network is a dependency, not a guarantee.

Client resilience asks:

```text
What does the user see when network, backend, cache, or JavaScript loading fails?
```

A PWA is one way to improve resilience, installability, and offline behavior.

---

## 2. Definition

- Definition: A Progressive Web App uses web platform features such as manifests, service workers, caching, and installability to behave more like a resilient app.
- Category: Frontend production architecture.
- Core idea: Design useful behavior for weak networks, offline states, and repeated visits.

---

## 3. PWA Building Blocks

| Building Block | Purpose |
|---|---|
| Web app manifest | App name, icons, display mode, install metadata |
| Service worker | Intercepts network requests and handles caching |
| Cache Storage | Stores responses for offline/reuse |
| IndexedDB | Stores structured client data |
| Background sync | Retry deferred work when connection returns |
| Push notifications | Re-engagement, if user permits |
| Offline UI | Clear user feedback and recovery |

---

## 4. Manifest

```json
{
  "name": "Acme Store",
  "short_name": "Acme",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#111827",
  "icons": [
    {
      "src": "/icons/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

In Next.js:

```ts
// app/manifest.ts
import type { MetadataRoute } from 'next';

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: 'Acme Store',
    short_name: 'Acme',
    start_url: '/',
    display: 'standalone',
    background_color: '#ffffff',
    theme_color: '#111827',
    icons: [
      { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
      { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
    ],
  };
}
```

---

## 5. Service Worker Strategy

Cache strategies:

| Strategy | Use For | Risk |
|---|---|---|
| Cache first | Versioned static assets | Stale assets if not versioned |
| Network first | Fresh API/content | Slow when network is bad |
| Stale while revalidate | News/catalog/docs | Temporary stale data |
| Network only | payments/auth mutations | No offline support |
| Cache only | pre-bundled offline shell | Missing updates |

Do not cache sensitive personalized API responses casually.

---

## 6. Offline UX

Good offline UI:
- says what is unavailable;
- preserves user input;
- shows last synced time;
- lets the user retry;
- avoids fake success for critical actions;
- distinguishes "saved locally" from "saved on server."

Bad offline UI:
- infinite spinner;
- generic crash page;
- silently drops form data;
- marks payment/order success before server confirmation.

---

## 7. Mutation Queue

For non-critical offline mutations:

```ts
type QueuedMutation = {
  id: string;
  type: 'NOTE_CREATE' | 'PROFILE_DRAFT_SAVE';
  payload: unknown;
  createdAt: number;
  retryCount: number;
};
```

Queue flow:

```text
User submits -> validate locally -> store mutation -> optimistic UI
-> sync when online -> mark confirmed or failed -> reconcile UI
```

Do not queue:
- payments;
- irreversible admin actions;
- security changes;
- operations requiring strict ordering unless you have conflict handling.

---

## 8. Conflict Handling

Common strategies:

| Strategy | Good For | Trade-off |
|---|---|---|
| Last write wins | Simple preferences | Can lose data |
| Server wins | Critical source of truth | User edits may be discarded |
| Client merge | Notes/forms | Merge complexity |
| Manual conflict UI | Documents/collaboration | More UI work |
| CRDT/OT | Real-time collaboration | High complexity |

Interview phrase:
Offline is not just caching. It creates distributed state on the client.

---

## 9. Storage Choices

| Storage | Use For | Avoid |
|---|---|---|
| Memory | Temporary UI state | Persistence |
| `localStorage` | Small non-sensitive preferences | Tokens, large data |
| IndexedDB | Offline drafts, queues, structured data | Simple flags |
| Cache Storage | HTTP responses/assets | Private secrets |
| Cookies | Session identifiers | Large client data |

---

## 10. Resilient Fetch Pattern

```ts
export async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);

  try {
    const response = await fetch(url, {
      ...init,
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`Request failed: ${response.status}`);
    }

    return (await response.json()) as T;
  } finally {
    clearTimeout(timeout);
  }
}
```

Add:
- retry with backoff for idempotent requests;
- no retry for non-idempotent mutations unless safely deduplicated;
- `AbortController` for abandoned UI;
- clear error states.

---

## 11. Next.js Considerations

Watch out:
- service workers run in the browser, not on the server;
- RSC payloads and app shell behavior require careful cache testing;
- static assets are easier to cache than dynamic RSC responses;
- auth and personalized data require conservative caching;
- route transitions should handle offline failures gracefully.

Practical approach:
- start with installability and static asset caching;
- add offline page;
- add offline draft persistence for forms;
- add mutation queue only for safe operations;
- test update behavior after deploy.

---

## 12. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Caching everything | Privacy and staleness bugs | Cache by data class |
| Fake offline success for payments | Business/security risk | Require server confirmation |
| No update strategy | Users run stale app shell | Version service worker |
| Ignoring conflict resolution | Data loss | Define merge policy |
| Storing tokens in localStorage | XSS theft risk | Prefer secure httpOnly cookies |
| No offline testing | Works only in demos | Use browser offline mode and e2e |

---

## 13. Practical Question

> You are building a field-service app where users may lose network while filling inspection forms. How would you design offline support?

---

## 14. Strong Answer

```text
I would not start by caching everything. I would cache the app shell and static
assets, store inspection drafts in IndexedDB, clearly show offline/last-synced
state, and queue only safe form submissions with idempotency keys. When the
network returns, the client syncs queued drafts, handles conflicts explicitly,
and marks records confirmed only after the server accepts them. Authentication,
payments, and irreversible admin actions would remain server-confirmed.
```

---

## 15. Revision Notes

- One-line summary: Offline support turns the browser into a small distributed system.
- Three keywords: service worker, IndexedDB, sync.
- One interview trap: Do not treat offline caching as safe for personalized or critical mutations.
- One memory trick: Offline design = store, sync, reconcile, explain.

