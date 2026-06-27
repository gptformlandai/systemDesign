# React Native Security, Offline, Release, And Observability - MAANG Master Sheet

> Track File #12 of 20 - Group 4: Senior MAANG
> Level: production readiness beyond UI coding

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Token security | Very high | Auth safety |
| PII handling | Very high | Compliance and user trust |
| Offline sync | High | Mobile network reality |
| Timeouts/retries | High | Reliability under bad networks |
| Release channels | High | Mobile delivery is slower than web |
| OTA updates | Medium-high | Powerful but risky |
| Crash reporting | Very high | Production debugging |
| Analytics/telemetry | High | Product and reliability feedback |

MAANG signal:
You understand that mobile production is a distributed system running on unreliable devices and networks.

---

## 2. Security Mental Model

Mobile apps run on devices you do not control.

Assume:
- App binary can be inspected.
- Local storage can be attacked on compromised devices.
- Network can be unreliable or hostile.
- Logs can leak.
- Deep links can be abused.
- Users may be on old app versions for months.

Rule:
The backend must enforce security. The mobile app improves UX and reduces exposure, but it is not the trust boundary.

---

## 3. Token And Secret Safety

Do:
- Store auth tokens in platform secure storage.
- Use short-lived access tokens.
- Rotate refresh tokens if backend supports it.
- Clear tokens on logout/revocation.
- Use HTTPS.
- Redact auth headers from logs.
- Keep true secrets on the backend.

Do not:
- Put API secrets in app config.
- Store tokens in normal AsyncStorage.
- Put tokens in URLs/deep links.
- Log full request/response bodies with PII.
- Trust client-side role checks.

Interview answer:

```text
Anything bundled into a mobile app should be treated as public. I store user
tokens in secure storage, but true service secrets stay on the backend. The backend
must enforce authorization because the client can be inspected or modified.
```

---

## 4. PII And Logging

PII examples:
- name
- email
- phone
- address
- precise location
- payment metadata
- health data
- IDs that identify a user

Logging rules:
- Use correlation IDs.
- Redact tokens and sensitive fields.
- Avoid full payload logs.
- Sample high-volume events.
- Separate analytics from debugging logs.
- Honor privacy and consent requirements.

Bad:

```ts
logger.info('login response', response);
```

Better:

```ts
logger.info('login completed', {
  status: response.status,
  requestId,
});
```

---

## 5. Offline Strategy

Offline app states:

```text
online
offline_with_cache
offline_no_cache
sync_pending
sync_failed
conflict_detected
```

Patterns:
- Read-through cache for content.
- Offline mutation queue for safe actions.
- Idempotency keys for queued mutations.
- Conflict detection with versions/timestamps.
- Retry with exponential backoff.
- User-visible pending state.

Example queue item:

```ts
type OfflineMutation = {
  idempotencyKey: string;
  type: 'save_note' | 'like_post';
  payload: unknown;
  createdAt: string;
  retryCount: number;
};
```

Avoid offline queues for:
- payments without backend-designed idempotency
- legal submissions
- irreversible destructive actions
- actions requiring real-time inventory/availability

---

## 6. Network Reliability

Production API client should support:
- timeout
- retry for safe idempotent requests
- no retry for unsafe mutations unless idempotent
- cancellation on screen unmount/search changes
- auth refresh flow
- standardized error mapping
- telemetry for latency/failures

Backoff:

```ts
function backoffMs(attempt: number) {
  const base = Math.min(1000 * 2 ** attempt, 30_000);
  const jitter = Math.floor(Math.random() * 250);
  return base + jitter;
}
```

---

## 7. Release Engineering

Mobile release differs from web:
- App store review can delay releases.
- Users may not update immediately.
- Native binary changes require store release.
- OTA updates can update JS/assets but should not violate native/runtime compatibility.
- Rollbacks are slower than server rollbacks.

Release checklist:
- version/build number bumped
- changelog/release notes
- source maps uploaded
- crash-free threshold monitored
- feature flags ready
- staged rollout plan
- backend compatibility verified
- old app versions still supported or blocked intentionally

---

## 8. OTA Updates

OTA updates are useful for:
- JS bug fixes
- copy changes
- small UI changes
- feature-flagged JS behavior

Be careful with:
- native module changes
- schema/storage migrations
- navigation state changes
- incompatible JS/native assumptions
- emergency rollback plan

Interview answer:

```text
OTA updates are powerful, but I treat them as updates within the compatibility
boundary of the installed native binary. If a change requires native code, new
permissions, or native dependency changes, it needs an app-store binary release.
```

---

## 9. Observability

Mobile observability includes:
- crash reports
- non-fatal errors
- source maps
- app start time
- screen load time
- API latency/error rate
- JS/UI FPS on critical screens if available
- memory warnings
- ANR/app-not-responding signals on Android
- release version/build/channel
- device model/OS/app version

Useful event shape:

```ts
type MobileTelemetryEvent = {
  name: string;
  timestamp: string;
  appVersion: string;
  buildNumber: string;
  platform: 'ios' | 'android';
  deviceClass?: 'low' | 'mid' | 'high';
  requestId?: string;
  properties: Record<string, unknown>;
};
```

Rule:
Telemetry should help debug without collecting unnecessary sensitive data.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| App contains real API secret | Binary can be inspected | Keep secrets on backend |
| No source maps | Crashes unreadable | Upload per release |
| OTA native-incompatible change | Runtime crash risk | Respect binary compatibility |
| Retrying all mutations | Duplicate side effects | Retry only idempotent/safe actions |
| No old-version API support | Older apps break | Version APIs or maintain compatibility |
| Logging PII | Compliance/security issue | Redact and minimize |

---

## 11. Strong Interview Answer

Question:
How do you make a React Native app production-ready?

Strong answer:

```text
I treat mobile as an unreliable distributed client. Security starts with backend
authorization, secure token storage, HTTPS, and PII-safe logs. Reliability requires
timeouts, cancellation, safe retries, idempotency, and offline cache/queue design
where appropriate. Release readiness means app-store builds, source maps, staged
rollout, feature flags, OTA compatibility rules, and monitoring crash-free users,
startup time, API failures, and screen performance by app version and device class.
```

---

## 12. Revision Notes

- One-line summary: Production RN needs security, offline resilience, release discipline, and telemetry.
- Three keywords: tokens, OTA, crash reports.
- One interview trap: Mobile app config is not secret.
- One memory trick: Mobile release is slower than web release, so feature flags and compatibility matter.

