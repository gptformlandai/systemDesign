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

