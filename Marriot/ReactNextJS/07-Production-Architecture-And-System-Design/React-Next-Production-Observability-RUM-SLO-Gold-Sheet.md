# React + Next.js Production Observability, RUM, And SLOs - Gold Sheet

> Track File #43 - Group 7: Production Architecture And System Design
> Level: senior -> architect | Web Vitals, RUM, traces, logs, release health, SLOs, privacy, and incident debugging

---

## 1. Intuition

Observability answers:

```text
Is the app healthy for real users?
If not, who is affected, where, since when, and why?
```

Frontend observability is not just console errors. It connects browser experience to server work, releases, caches, and backend dependencies.

---

## 2. Definition

- Definition: Frontend observability is the collection and analysis of user-experience, error, log, metric, and trace data from browser to server.
- Category: Production engineering.
- Core idea: Measure real user impact, not only local performance.

---

## 3. Signal Types

| Signal | What It Answers |
|---|---|
| RUM | What real users experience |
| Web Vitals | Loading, interaction, layout stability |
| Errors | What broke |
| Logs | What happened |
| Traces | Where time went across services |
| Metrics | How often and how severe |
| Session replay | What the user saw, with privacy controls |
| Release health | Did the new deploy make things worse |

---

## 4. Web Vitals Targets

Core targets:

| Metric | Good Target | Meaning |
|---|---:|---|
| LCP | <= 2.5s | Main content load |
| INP | <= 200ms | Interaction responsiveness |
| CLS | <= 0.1 | Layout stability |
| TTFB | lower is better | Server/CDN responsiveness |

Use field data for decisions. Lab data is useful for debugging, but real user data decides priority.

---

## 5. Next.js Web Vitals Hook

```tsx
'use client';

import { useReportWebVitals } from 'next/web-vitals';

export function WebVitalsReporter() {
  useReportWebVitals(metric => {
    navigator.sendBeacon(
      '/api/rum',
      JSON.stringify({
        name: metric.name,
        value: metric.value,
        id: metric.id,
        rating: metric.rating,
        navigationType: metric.navigationType,
      }),
    );
  });

  return null;
}
```

Mount once near the app shell:

```tsx
// app/layout.tsx
import { WebVitalsReporter } from '@/components/web-vitals-reporter';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <WebVitalsReporter />
        {children}
      </body>
    </html>
  );
}
```

---

## 6. RUM Event Shape

Capture enough context to debug without collecting sensitive data.

```ts
type RumEvent = {
  app: string;
  release: string;
  route: string;
  metric: 'LCP' | 'INP' | 'CLS' | 'TTFB' | 'FCP';
  value: number;
  rating: 'good' | 'needs-improvement' | 'poor';
  deviceType: 'desktop' | 'mobile' | 'tablet';
  connection?: string;
  country?: string;
  browser?: string;
  sampled: boolean;
};
```

Do not collect:
- passwords;
- tokens;
- full names unless necessary;
- raw addresses;
- credit card data;
- health/financial data;
- unmasked replay of sensitive forms.

---

## 7. Tracing With OpenTelemetry

Next.js supports server instrumentation through `instrumentation.ts`.

```ts
// instrumentation.ts
import { registerOTel } from '@vercel/otel';

export function register() {
  registerOTel({ serviceName: 'next-storefront' });
}
```

For manual spans:

```ts
import { trace } from '@opentelemetry/api';

export async function fetchProduct(productId: string) {
  return trace
    .getTracer('next-storefront')
    .startActiveSpan('fetchProduct', async span => {
      try {
        span.setAttribute('product.id', productId);
        return await getProductFromApi(productId);
      } finally {
        span.end();
      }
    });
}
```

Trace goal:

```text
Browser route -> Next.js request -> data fetch -> backend service -> database
```

When a route is slow, the trace should show where.

---

## 8. Error Monitoring

Capture:
- unhandled browser errors;
- unhandled promise rejections;
- React error boundary failures;
- route handler exceptions;
- Server Action failures;
- API dependency failures;
- hydration errors;
- chunk load errors.

Add context:

```text
release
route
user/tenant hash
feature flag state
browser/device
network
build id
request id / trace id
```

Avoid:
- logging full payloads;
- storing secrets;
- noisy expected validation errors as critical incidents.

---

## 9. SLOs

SLO means Service Level Objective: the reliability target you commit to.

Frontend SLO examples:

```text
99.5% of product pages have LCP <= 2.5s over 7 days.
99.9% of checkout page views do not hit a JS fatal error.
99% of add-to-cart interactions complete within 500ms.
95% of searches render first results within 1s.
```

Error budget:

```text
Allowed bad events = total events * (1 - SLO)
```

Why SLOs matter:
- prevents arguing from anecdotes;
- balances speed and reliability;
- gives release stop/go criteria;
- makes incidents measurable.

---

## 10. Dashboards

Minimum dashboard:
- traffic by route;
- error rate by route/release/browser;
- LCP/INP/CLS p75 and p95;
- Server Action failure rate;
- API dependency latency;
- cache hit/miss ratio;
- release comparison;
- top slow pages;
- top user-impacting errors.

Useful breakdowns:
- country/region;
- device class;
- browser version;
- authenticated vs anonymous;
- tenant/customer tier;
- network type;
- experiment variant.

---

## 11. Alerting

Good alert:

```text
Checkout fatal JS error rate > 1% for 5 minutes on latest release.
```

Bad alert:

```text
Any console error happened.
```

Alert on user impact:
- checkout broken;
- login broken;
- LCP regression;
- Server Action failures;
- 5xx route handler spike;
- bundle chunk load failures after deploy.

---

## 12. Release Health

Every deploy should answer:

```text
Did errors increase?
Did Web Vitals regress?
Did conversion-critical interactions slow down?
Are failures isolated to browser/device/region?
Can we roll back quickly?
```

Release practices:
- tag errors with build id;
- keep source maps private and upload to provider;
- monitor first 30 minutes after deploy;
- compare against previous release;
- use canary/gradual rollout for risky changes.

---

## 13. Privacy And Compliance

Rules:
- mask sensitive form fields in replay;
- hash user identifiers when possible;
- sample sessions;
- define retention limits;
- avoid sending request bodies by default;
- treat logs as production data;
- restrict access to observability tools.

Privacy failure example:

```text
Session replay captures password reset token in URL.
```

Fix:
- redact query params;
- avoid secrets in URLs;
- mask token-like values;
- alert on known sensitive keys.

---

## 14. Debugging Playbook

Problem: "LCP regressed after release."

```text
1. Confirm field regression by route and device.
2. Compare release versions.
3. Check server TTFB and cache hit ratio.
4. Check image/font changes.
5. Check bundle size and Client Component expansion.
6. Check backend dependency latency.
7. Reproduce in lab with same device/network.
8. Roll back or feature-flag if error budget is burning.
9. Add regression test or budget.
```

Problem: "Hydration errors spiked."

```text
1. Filter by release and route.
2. Look for Date, random, locale, browser-only APIs in render.
3. Check server/client feature flag mismatch.
4. Check invalid HTML nesting.
5. Add targeted error boundary and test.
```

---

## 15. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Only using lab Lighthouse | Misses real users | Add RUM |
| Logging PII | Compliance risk | Redact and sample |
| Alerts without route/release | Hard to debug | Tag every event |
| No source maps | Stack traces useless | Upload privately |
| No SLOs | No priority model | Define user-impact targets |
| Measuring averages only | Hides tail pain | Use p75/p95/p99 |

---

## 16. Practical Question

> You own a Next.js ecommerce app. What would you instrument before Black Friday?

---

## 17. Strong Answer

```text
I would instrument RUM for Core Web Vitals by route, JS errors by release,
checkout and add-to-cart success rates, Server Action failures, route handler
latency, API dependency traces, and cache hit ratios. I would define SLOs for
product page LCP, checkout fatal errors, and add-to-cart latency, then alert on
burning error budget rather than every noisy error. I would also upload private
source maps, mask replay data, and make release rollback or feature flags ready.
```

---

## 18. Revision Notes

- One-line summary: Frontend observability connects user experience to releases, routes, traces, and business-critical flows.
- Three keywords: RUM, SLO, trace.
- One interview trap: Lab performance is not enough; field data decides real impact.
- One memory trick: Healthy UI = fast, correct, measured, debuggable.

