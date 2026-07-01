# 15. Datadog RUM and Synthetic Monitoring: Frontend Performance, Core Web Vitals

## Goal

Instrument frontend applications with Real User Monitoring (RUM), measure Core Web Vitals, configure synthetic tests for proactive monitoring, and understand apdex scores.

---

## What Is RUM

Real User Monitoring (RUM) collects performance and behavior data from actual user browsers or mobile apps. It measures what real users experience, not what lab tests predict.

```text
User visits page
  -> RUM SDK in browser captures:
      - Page load timing
      - Core Web Vitals (LCP, FID, CLS)
      - JavaScript errors
      - Network requests (XHR/fetch)
      - User interactions (clicks, form submits)
      - Session replay recording (optional)
  -> Data sent to Datadog RUM endpoint
  -> Viewable in RUM Explorer and dashboards
```

---

## Browser RUM SDK Setup

```html
<!-- Option 1: CDN (add to <head>) -->
<script
  src="https://www.datadoghq-browser-agent.com/us1/v5/datadog-rum.js"
  type="text/javascript">
</script>
<script>
  window.DD_RUM && window.DD_RUM.init({
    clientToken: 'pubXXXXXXXXXXXXXX',     // public client token (safe for browser)
    applicationId: 'xxxxxxxx-xxxx-xxxx',   // RUM application ID
    site: 'datadoghq.com',
    service: 'orders-frontend',
    env: 'production',
    version: '1.2.3',
    sessionSampleRate: 100,               // track 100% of sessions
    sessionReplaySampleRate: 20,          // record 20% of sessions
    trackInteractions: true,
    trackResources: true,
    trackLongTasks: true,
    defaultPrivacyLevel: 'mask-user-input', // mask form inputs in session replay
  })
</script>
```

```javascript
// Option 2: npm package.
// npm install @datadog/browser-rum

import { datadogRum } from '@datadog/browser-rum'

datadogRum.init({
  applicationId: 'xxxxxxxx-xxxx-xxxx',
  clientToken: 'pubXXXXXXXXXXXXXX',
  site: 'datadoghq.com',
  service: 'orders-frontend',
  env: 'production',
  version: '1.2.3',
  sessionSampleRate: 100,
  sessionReplaySampleRate: 20,
  trackInteractions: true,
  trackResources: true,
  trackLongTasks: true,
  defaultPrivacyLevel: 'mask-user-input',
})
```

---

## Core Web Vitals

| Metric | What It Measures | Good | Needs Work | Poor |
|---|---|---|---|---|
| LCP (Largest Contentful Paint) | How fast main content loads | < 2.5s | 2.5-4s | > 4s |
| FID (First Input Delay) | Responsiveness to first interaction | < 100ms | 100-300ms | > 300ms |
| CLS (Cumulative Layout Shift) | Visual stability (layout jumping) | < 0.1 | 0.1-0.25 | > 0.25 |
| INP (Interaction to Next Paint) | Responsiveness to all interactions | < 200ms | 200-500ms | > 500ms |
| FCP (First Contentful Paint) | Time until first content painted | < 1.8s | 1.8-3s | > 3s |
| TTFB (Time To First Byte) | Server response time | < 800ms | 800ms-1.8s | > 1.8s |

---

## Apdex Score

Apdex quantifies user satisfaction as a number between 0 and 1.

```text
T = apdex threshold (satisfaction target)

Satisfied:  response time <= T
Tolerating: response time <= 4T
Frustrated: response time >  4T

Apdex = (Satisfied + (Tolerating / 2)) / Total

Example (T = 500ms):
  100 requests: 70 satisfied (<=500ms), 20 tolerating (<=2000ms), 10 frustrated (>2000ms)
  Apdex = (70 + 20/2) / 100 = (70 + 10) / 100 = 0.8
```

Apdex 1.0 = perfect. 0.7+ = acceptable. Below 0.5 = poor user experience.

Configure apdex threshold in APM service settings: Services → orders-service → Edit Apdex Threshold.

---

## Session Replay

Session replay records full browser sessions (video-like playback) for debugging:

```text
RUM -> Session Replay
  - Click a session
  - See pixel-perfect recording of what the user saw
  - Click-heatmap, rage clicks, error moments highlighted
  - Network waterfall alongside the replay
```

Privacy controls:

```javascript
datadogRum.init({
  defaultPrivacyLevel: 'mask-user-input',  // hides all form fields
  // Options: 'allow' | 'mask' | 'mask-user-input'
})
```

---

## Synthetic Monitoring

Synthetic tests simulate user actions to proactively detect issues before real users are affected.

### API Test (Simple Uptime Check)

```text
Synthetics -> New Test -> API Test

Request:
  Method: GET
  URL: https://api.example.com/health
  
Assertions:
  - Status code is 200
  - Response body contains {"status":"ok"}
  - Response time < 500ms
  - SSL certificate expires in > 30 days

Locations: AWS us-east-1, EU London, AP Tokyo
Schedule: every 1 minute
Alert on: failure from 2 of 3 locations
```

### Browser Test (Full User Flow)

```text
Synthetics -> New Test -> Browser Test

Starting URL: https://app.example.com

Steps (recorded or scripted):
  1. Load page (assert: "Sign In" button visible)
  2. Click "Sign In"
  3. Type email: testuser@example.com
  4. Type password: (from secret variable)
  5. Click "Submit"
  6. Assert: "Welcome back" text visible
  7. Click "Place Order"
  8. Assert: order confirmation number present

Assertions at each step. Failure = alert.
Schedule: every 5 minutes
Locations: AWS us-east-1, EU Paris
```

### Multi-Step API Test (Workflow Test)

```text
Steps:
  1. POST /auth/login -> extract token from response
  2. GET /orders (using token) -> assert 200
  3. POST /orders (using token) -> assert 201, extract orderId
  4. GET /orders/{orderId} -> assert status is "PENDING"
```

---

## Private Locations

For testing internal services not exposed to the internet:

```bash
# Deploy private location agent inside your network.
docker run -d --rm \
  -e DATADOG_API_KEY=your-api-key \
  -e DATADOG_ACCESS_KEY=your-access-key \
  datadog/synthetics-private-location-worker
```

---

## RUM + APM Trace Correlation

When both RUM and APM are active:

1. Browser request includes `x-datadog-trace-id` header automatically.
2. Backend APM picks up the trace.
3. In RUM, click a slow XHR request → "View APM trace" link appears.
4. Jump directly to the backend flame graph for that specific user request.

---

## Interview Sound Bite

RUM SDK instruments the browser to capture Core Web Vitals (LCP, FID, CLS), page load timing, JS errors, and user interactions from real users. Session replay provides video-like debugging. Synthetic tests proactively simulate API calls and browser user flows on a schedule from multiple global locations, alerting before users are affected. Apdex quantifies user satisfaction as a 0-1 score using a T-threshold. RUM and APM traces are correlated via trace headers, enabling click-through from a browser request to the server-side flame graph.
