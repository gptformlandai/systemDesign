# React Native Mobile Observability, RUM, And SLOs - Gold Sheet

> Track Module - Group 4: Senior / MAANG Path
> Level: production reliability and operational excellence

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Crash-free sessions | Very high | Primary mobile reliability metric |
| Startup performance | Very high | Direct UX and retention impact |
| JS errors vs native crashes | High | Different debugging pipelines |
| OTA/update attribution | High | Needed for safe rollout |
| Mobile RUM | High | Measures real user experience |
| Privacy-safe telemetry | Very high | Prevents compliance and trust failures |

MAANG signal:
You can operate a mobile app after it ships, not just build screens before release.

---

## 2. Mental Model

Mobile observability answers:

```text
Who is affected?
What changed?
Which app version/build/update?
Which device/OS/network?
Which screen/flow?
Is the user experience broken or just slower?
Can we rollback, disable, or hotfix safely?
```

Mobile is harder than web because:
- users stay on old binaries
- OTA updates create multiple JS versions per binary
- devices vary wildly
- logs are not easy to retrieve
- native crashes may happen before JS starts
- app stores slow full binary rollback

---

## 3. Observability Pillars

| Pillar | Examples |
|---|---|
| Crashes | native crash, fatal JS exception, unhandled promise rejection |
| Errors | API error, validation failure, permission denial, sync failure |
| RUM | startup, screen load, interaction latency, list scroll health |
| Traces | request correlation, navigation flow, backend trace id |
| Logs | structured breadcrumbs, release diagnostics, sampled debug events |
| Product analytics | funnel, activation, conversion, retention |

Keep reliability telemetry separate from product analytics when privacy, sampling, and alerting needs differ.

---

## 4. Required Event Attributes

Every production event should include:

```text
app.version
app.buildNumber
app.runtimeVersion
ota.updateId
ota.channel
platform.os
platform.version
device.model
device.memoryClass
network.type
screen.name
session.id
user.anonymousId or hashedId
environment
```

Never include:
- access tokens
- refresh tokens
- raw passwords
- full request bodies
- payment card data
- unnecessary precise location
- contact lists
- medical/financial PII unless specifically designed and approved

---

## 5. Core Mobile SLOs

Example SLOs:

| SLO | Target |
|---|---|
| Crash-free sessions | >= 99.8 percent |
| App cold start p75 | <= 2.5 seconds on supported devices |
| App cold start p95 | <= 5 seconds |
| Time to first render p75 | <= 1.5 seconds |
| Time to interactive p75 | <= 3 seconds |
| API request success | >= 99 percent for critical endpoints |
| Offline queue drain | 95 percent within 5 minutes after reconnect |
| ANR rate Android | below defined release threshold |

Use product-specific targets. A banking app, game, chat app, and warehouse app have different tolerance.

---

## 6. Startup Instrumentation

Startup phases:

```text
process start
  -> native app launch
  -> JS runtime initialized
  -> JS bundle loaded
  -> root component mounted
  -> navigation ready
  -> first meaningful screen rendered
  -> app interactive
```

Record timestamps:

```ts
const marks: Record<string, number> = {};

export function mark(name: string) {
  marks[name] = Date.now();
}

export function measure(start: string, end: string) {
  if (!marks[start] || !marks[end]) return null;
  return marks[end] - marks[start];
}

mark('js_start');

// later, when navigation is ready
mark('navigation_ready');
```

Production version:
Use your observability SDK, native startup hooks, and platform-specific timestamps where available.

---

## 7. Screen And Interaction RUM

Track:
- screen mount to first data
- screen mount to first meaningful paint
- button tap to response
- search input to results
- navigation transition duration
- list scroll dropped-frame signals
- image load failures

Example event:

```ts
analytics.track('screen_ready', {
  screen: 'OrderHistory',
  durationMs: 842,
  source: 'cache_then_network',
  itemCount: 20,
});
```

Rules:
- Use stable screen names.
- Avoid raw route params containing PII.
- Bucket high-cardinality values.
- Sample noisy events.

---

## 8. Crash And Error Triage

Triage order:

```text
1. Is crash-free session rate below threshold?
2. Did it start after a binary release or OTA update?
3. Which app version/build/runtime/update is affected?
4. Which device/OS/platform is clustered?
5. Is there a feature flag or remote config kill switch?
6. Can we rollback OTA, disable feature, or submit hotfix?
```

Required crash context:
- source maps for JS
- dSYM for iOS
- ProGuard/R8 mapping for Android
- breadcrumbs
- release version/build
- OTA update id/channel/runtime
- user impact count

---

## 9. OTA Observability

Every OTA update should be attributable.

Track:
- update id
- channel
- runtime version
- rollout percentage
- first launch after update
- crash rate by update id
- JS error rate by update id
- update download failure
- rollback status

Rule:
Never evaluate an OTA release only by "published successfully." Evaluate by adoption, crash-free sessions, critical flow success, and rollback readiness.

---

## 10. Offline And Sync Observability

Track:
- queue length
- oldest pending mutation age
- sync attempts
- sync success/failure
- conflict count
- local database migration duration
- storage size
- data corruption reports

Dashboards should split by:
- app version
- build
- network type
- country/region if allowed
- OS version
- low-memory device class

---

## 11. Privacy And Sampling

Privacy rules:
- collect the minimum useful signal
- hash or anonymize user identifiers where possible
- redact sensitive fields at the logger boundary
- avoid event names that expose private behavior
- document retention
- respect consent and regional policy

Sampling:
- always capture crashes
- heavily sample verbose logs
- sample high-volume UI events
- keep critical flow metrics unsampled or carefully sampled

---

## 12. Alerting

Good alerts:
- crash-free sessions below threshold after release
- startup p95 regression above threshold
- API failure spike in critical flow
- offline queue age above threshold
- OTA update crash spike
- payment verification failure spike

Bad alerts:
- every single JS warning
- raw event volume changes without context
- alerts that do not identify owner, impact, or action

---

## 13. Strong Interview Answer

```text
For React Native observability I track native crashes, fatal JS errors, startup,
screen readiness, API reliability, offline sync, and OTA attribution. Every event
includes app version, build number, runtime version, update id, platform, device,
network, and screen. I upload source maps and native symbols for every release,
set SLOs like crash-free sessions and cold-start p95, and alert on release
regressions. I also redact PII and sample noisy telemetry so observability helps
debug production without becoming a privacy problem.
```

---

## 14. Revision Notes

- One-line summary: Mobile observability connects user impact to app version, device, network, and update.
- Three keywords: crash-free, startup, update attribution.
- One interview trap: Saying "we use Sentry" without defining metrics, ownership, or rollback.
- Memory trick: Who, what changed, which build, which device, can we rollback?
