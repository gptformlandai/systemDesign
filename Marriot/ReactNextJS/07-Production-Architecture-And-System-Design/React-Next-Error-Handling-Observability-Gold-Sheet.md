# React + Next.js Error Handling And Observability - Gold Sheet

> Track File #20 of 24 - Group 7: Production Architecture And System Design
> Covers: logging, monitoring, Sentry, error boundaries, API failures

---

## 1. Intuition

Production users experience failures before engineers see stack traces.

Observability answers:

```text
what failed?
for whom?
where?
how often?
which release?
what changed?
```

---

## 2. Error Boundaries

React error boundaries catch render errors below them.

Next App Router provides segment-level error files:

```tsx
'use client';

export default function Error({
  error,
  reset,
}: {
  error: Error;
  reset: () => void;
}) {
  return (
    <section>
      <h2>Something went wrong</h2>
      <button onClick={reset}>Try again</button>
    </section>
  );
}
```

Use boundaries around:
- route segments
- expensive widgets
- third-party integrations
- payment/checkout surfaces

---

## 3. API Failure Handling

Model failures:
- loading
- empty
- unauthorized
- forbidden
- validation error
- rate limited
- server error
- network timeout
- retrying

Do not show "Something went wrong" for every failure.

---

## 4. Logging Strategy

Client logs:
- user action breadcrumbs
- route changes
- feature flags
- non-fatal errors
- performance markers

Server logs:
- request ID
- user/tenant ID if safe
- route/action
- latency
- status
- error class

Avoid:
- tokens
- passwords
- raw PII
- full payloads by default

---

## 5. Monitoring With Sentry-Style Tools

Track:
- exceptions
- source maps
- release version
- environment
- user/session when privacy allows
- breadcrumbs
- performance spans
- Web Vitals

Production rule:
Upload source maps for readable client stack traces.

---

## 6. Instrumentation

Next.js supports instrumentation entry points for server observability patterns.

Use:
- OpenTelemetry traces
- request correlation IDs
- external API spans
- database spans
- error reporting setup

Goal:
Trace user request from browser to Next server to backend services.

---

## 7. Real-World Use Cases

- Checkout error: boundary plus report with cart ID/request ID.
- Dashboard widget failure: local fallback, rest of page works.
- API timeout: retry button and telemetry.
- Production hydration error: capture release, route, user agent.
- Slow page: Web Vitals and server trace.

---

## 8. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| One root error boundary only | whole app fallback | segment/widget boundaries |
| Logging sensitive data | security risk | redact |
| No source maps | unreadable stacks | upload per release |
| No request correlation | hard debugging | request IDs/traces |
| Same UI for all errors | bad UX | error taxonomy |

---

## 9. Strong Interview Answer

Question:
How do you handle errors and observability in React/Next.js?

Strong answer:

```text
I use layered error handling. Server code validates and returns typed failures.
Client UI models loading, empty, validation, auth, rate-limit, network, and server
errors separately. React/Next error boundaries isolate route or widget crashes.
For observability, I capture client exceptions with source maps, server logs with
request IDs, traces across backend calls, Web Vitals, and release metadata. Logs
must be useful but redacted.
```

---

## 10. Revision Notes

- One-line summary: Errors need user recovery and engineer evidence.
- Three keywords: boundary, logs, traces.
- One interview trap: Error boundaries do not catch every async event handler error automatically.
- One memory trick: User sees fallback; engineer sees correlated evidence.

---

## 11. Next.js Error File Conventions

```
app/
  error.tsx           ← catches errors in the same segment and children
  not-found.tsx       ← rendered by notFound() call
  global-error.tsx    ← catches errors in the root layout (must include <html><body>)
  products/
    error.tsx         ← catches errors only in the products segment
    not-found.tsx     ← rendered by notFound() in the products segment
```

```tsx
// app/error.tsx — must be a Client Component
'use client';
import { useEffect } from 'react';

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log to external service
    captureException(error, { digest: error.digest });
  }, [error]);

  return (
    <div>
      <h2>Something went wrong</h2>
      <p>Reference: {error.digest}</p>  {/* safe to show — digest is a hash, not the message */}
      <button onClick={reset}>Try again</button>
    </div>
  );
}
```

**`error.digest`:** Next.js generates a short hash for each server error, correlates it to the server logs. Show the digest to users so they can report it — never show the raw error.message from server errors to users.

---

## 12. Sentry Integration Pattern

```tsx
// app/global-error.tsx
'use client';
import * as Sentry from '@sentry/nextjs';

export default function GlobalError({ error, reset }: { error: Error; reset: () => void }) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html><body>
      <h2>Critical error</h2>
      <button onClick={reset}>Reload</button>
    </body></html>
  );
}

// instrumentation.ts (Next.js) — initializes Sentry on server startup
export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs') {
    await import('./sentry.server.config');
  }
  if (process.env.NEXT_RUNTIME === 'edge') {
    await import('./sentry.edge.config');
  }
}
```

---

## 13. Retry with Exponential Backoff

```tsx
async function fetchWithRetry<T>(
  url: string,
  options?: RequestInit,
  maxRetries = 3
): Promise<T> {
  let lastError: Error;
  
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const res = await fetch(url, options);
      if (res.status === 429 || res.status >= 500) {
        // Retryable error
        const delay = Math.pow(2, attempt) * 1000 + Math.random() * 1000;  // jitter
        await new Promise(r => setTimeout(r, delay));
        lastError = new Error(`HTTP ${res.status}`);
        continue;
      }
      if (!res.ok) throw new Error(`HTTP ${res.status}`);  // non-retryable
      return res.json();
    } catch (err) {
      lastError = err as Error;
      if (attempt < maxRetries - 1) {
        const delay = Math.pow(2, attempt) * 1000;
        await new Promise(r => setTimeout(r, delay));
      }
    }
  }
  
  throw lastError!;
}
```

---

## 14. Web Vitals Monitoring

```tsx
// app/layout.tsx — report Core Web Vitals to analytics
'use client';
import { useReportWebVitals } from 'next/web-vitals';

export function WebVitalsReporter() {
  useReportWebVitals((metric) => {
    // Send to your analytics endpoint
    fetch('/api/vitals', {
      method: 'POST',
      body: JSON.stringify({
        name: metric.name,
        value: metric.value,
        rating: metric.rating,  // 'good' | 'needs-improvement' | 'poor'
        id: metric.id,
        url: window.location.href,
      }),
    });
  });
  return null;
}

// Alert thresholds (use in your monitoring dashboard):
// LCP > 4s = poor — page load is broken
// INP > 500ms = poor — UI feels frozen
// CLS > 0.25 = poor — layout is unstable
```

