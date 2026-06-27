# React Native MAANG Interview Scenario Bank

> Track File #14 of 20 - Group 5: Scenario Practice
> Level: spoken interview answers with follow-up depth

---

## How To Use This File

For each scenario:
1. State the likely root causes.
2. Ask what evidence you would collect.
3. Explain your fix order.
4. Mention trade-offs.
5. End with prevention.

---

## Scenario 1: Feed Screen Scrolls Poorly

Question:

```text
Users report that the home feed stutters while scrolling. How do you debug and fix it?
```

Strong answer:

```text
I first reproduce on a realistic device in a release build. I check whether JS FPS
or UI FPS is dropping. Then I inspect whether the feed uses FlatList instead of
ScrollView, stable keys, memoized row components, stable renderItem, tuned window
settings, and optimized image sizes. I also check if rows do expensive formatting
or network calls during render. If images are the issue, I request thumbnails and
cache them. After each fix, I re-measure scroll FPS and memory.
```

Follow-ups:
- What if only JS FPS drops?
- What if only UI FPS drops?
- How would you handle video autoplay?
- How would you prevent regression?

---

## Scenario 2: Search Results Show Old Data

Question:

```text
User types quickly in search. Sometimes old results replace newer results.
```

Strong answer:

```text
This is likely a request race. A request for an earlier query may return after
the newer query. I would debounce input, key each query by search term in the
server-state cache, cancel stale requests when supported, or ignore responses that
do not match the latest query. I would also show loading and empty states based
on the active query only.
```

Follow-ups:
- How do you test it?
- What is the role of AbortController?
- Where should debounce live?

---

## Scenario 3: App Freezes During Login

Question:

```text
After login, the app freezes for two seconds before showing home.
```

Strong answer:

```text
I would profile startup after successful login. Likely causes include synchronous
secure storage reads, too many providers initializing, large cache hydration,
analytics/config blocking navigation, or heavy home screen render. I would show
the app shell first, move non-critical initialization after first paint, parallelize
needed requests, and avoid doing large transformations on the JS thread during
the transition.
```

Follow-ups:
- What should block home screen?
- How do you prevent auth flicker?
- How do you measure this?

---

## Scenario 4: User Can Back-Navigate To Login After Login

Question:

```text
On Android, after login, pressing back returns to the login screen.
```

Strong answer:

```text
The auth flow likely navigated to Home without resetting or replacing the route
tree. I would make auth state decide the navigator tree: unauthenticated users
see AuthStack and authenticated users see AppTabs. When auth changes, the login
stack is removed, so Android back cannot return to it.
```

Follow-ups:
- How should logout work?
- How do deep links work when logged out?
- How do you test Android back behavior?

---

## Scenario 5: Payment Button Was Tapped Twice

Question:

```text
Some users are charged twice after tapping pay twice on a slow network.
```

Strong answer:

```text
The client should disable the pay button while submitting, but the real fix must
be server-side idempotency. The app should generate or receive an idempotency key
for the payment attempt, send it with the request, show a pending state, and never
show success until the server confirms. Retries should reuse the same key so the
backend can return the original result instead of creating a duplicate charge.
```

Follow-ups:
- Should payment be optimistic?
- What happens if app closes mid-payment?
- How do you recover unknown payment status?

---

## Scenario 6: Push Notification Opens Wrong Screen

Question:

```text
A push notification for an order sometimes opens Home instead of OrderDetails.
```

Strong answer:

```text
I would inspect cold-start and warm-app notification handling separately. The app
may be processing the notification before navigation/auth restore is ready. I
would normalize push payloads into a deep-link intent, validate params, wait for
navigation and auth state to be ready, then navigate. If the route requires auth,
I store the intended destination and continue after login.
```

Follow-ups:
- What if the order no longer exists?
- What fields should the push payload contain?
- What should not be in the payload?

---

## Scenario 7: Memory Grows After Visiting Image Gallery

Question:

```text
Memory usage keeps growing after users open and close an image gallery.
```

Strong answer:

```text
I would profile memory and inspect whether large images, listeners, timers, or
navigation-retained screens are being held. Image galleries often retain decoded
images or base64 data in JS memory. I would avoid storing base64 in state, request
proper image sizes, clear listeners/timers, limit cache size, and verify screens
are unmounted or reset when expected.
```

Follow-ups:
- What is a native memory leak vs JS leak?
- How would you reproduce reliably?
- How do navigation stacks retain screens?

---

## Scenario 8: Offline Notes Sync Creates Duplicates

Question:

```text
Users create notes offline. After reconnecting, duplicate notes appear.
```

Strong answer:

```text
The offline queue likely lacks idempotency or conflict handling. Each offline
mutation should have a stable client-generated ID or idempotency key. When syncing,
the backend should upsert or return the existing note for the same key. The client
should reconcile local pending records with server records and mark them synced
instead of appending blindly.
```

Follow-ups:
- How do you handle edit conflicts?
- What if sync partially fails?
- How do you show pending state?

---

## Scenario 9: App Crashes Only In Release Build

Question:

```text
The app works in development but crashes in release.
```

Strong answer:

```text
I would collect crash logs with source maps and native symbols. Differences may
come from minification, Hermes bytecode, missing native config, ProGuard/R8 rules,
environment config, stripped permissions, or code that depends on dev-only behavior.
I would reproduce with a local release build, check native logs, verify source
map upload, and compare build-time config between dev and release.
```

Follow-ups:
- Why are source maps important?
- What release-only Android issues are common?
- How do you add CI guardrails?

---

## Scenario 10: Design System Button Is Inaccessible

Question:

```text
Screen reader users cannot understand a custom icon-only button.
```

Strong answer:

```text
The component likely lacks accessibility role and label. I would fix this in the
design-system primitive so every usage benefits. Icon-only buttons need an
accessibilityLabel that describes the action, correct role, adequate touch target,
focus behavior in modals, and visible state that is not color-only.
```

Follow-ups:
- How do you test accessibility?
- What about dynamic font sizes?
- Why fix the primitive instead of one screen?

---

## Scenario 11: Token Refresh Storm

Question:

```text
After access tokens expire, the app sends dozens of refresh requests and the API slows down.
```

Strong answer:

```text
The app likely refreshes independently for every 401. I would implement a
single-flight refresh flow: the first 401 starts refresh, other requests wait for
that promise, then retry once with the new token. If refresh fails, clear the
session and send the user to login. I would also add telemetry for refresh count,
refresh failure rate, and repeated 401 loops.
```

Follow-ups:
- How do you avoid infinite retry?
- Where should refresh token be stored?
- What happens if logout occurs while refresh is in flight?

---

## Scenario 12: WebSocket Misses Events After Reconnect

Question:

```text
A live order-tracking screen misses status updates when the device loses signal and reconnects.
```

Strong answer:

```text
The WebSocket reconnect flow probably resubscribes without catching up missed
events. I would include sequence numbers or event IDs, persist the last received
event ID, reconnect with backoff, resubscribe, and request missed events from the
backend. The UI should show reconnecting/stale state and recover through an HTTP
refresh if the gap cannot be replayed.
```

Follow-ups:
- How do you handle app background?
- How do you authenticate socket reconnect?
- When would polling be better than WebSockets?

---

## Scenario 13: OTA Update Breaks Older App Version

Question:

```text
An OTA update works on the latest binary but crashes users on an older binary.
```

Strong answer:

```text
The OTA update likely assumed native code, assets, storage schema, or permissions
that do not exist in the older binary. OTA updates must be scoped by runtime or
binary compatibility. I would stop rollout, roll back the update, segment update
channels by native version, and add a compatibility gate before publishing future
updates. Native changes require a store binary release.
```

Follow-ups:
- What changes are safe for OTA?
- How do you design update channels?
- What telemetry identifies the affected binary?

---

## Scenario 14: German Translation Breaks Checkout Layout

Question:

```text
Checkout looks fine in English, but German translations overflow buttons and hide prices.
```

Strong answer:

```text
The layout was probably designed around fixed English text. I would remove fixed
text heights, allow wrapping or flexible width, use minHeight with padding, and
test long localized strings. For buttons, the label can wrap or the layout can
adapt by stacking actions. I would also add visual checks for supported locales
and keep translation keys out of string concatenation.
```

Follow-ups:
- How do plural rules affect this?
- How do you test RTL?
- How do you handle legal text expansion?

---

## Rapid Answer Framework

Use this for any scenario:

```text
1. Classify the issue: performance, state, navigation, native API, network, release, security.
2. Reproduce in the right environment.
3. Collect evidence.
4. Identify smallest likely bottleneck or bug.
5. Fix with production trade-offs.
6. Add regression protection.
```
