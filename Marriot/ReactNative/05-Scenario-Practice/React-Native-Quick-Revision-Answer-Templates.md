# React Native Quick Revision And Answer Templates

> Track File #16 of 20 - Group 5: Scenario Practice
> Level: final revision before interviews

---

## 1. React Native In 30 Seconds

```text
React Native lets us build native iOS and Android apps using React and
JavaScript/TypeScript. It is not a WebView. React components describe UI, and
React Native maps that tree to native host views. Production quality depends on
thread-aware performance, navigation, native permissions, secure storage, release
discipline, and platform-specific testing.
```

---

## 2. Expo vs Bare In 30 Seconds

```text
For a new app, I usually start with Expo because it gives a production framework:
routing, native modules, builds, updates, and tooling. I choose bare React Native
when I need unusual native control, custom SDK integration, or integration into an
existing native app. The trade-off is velocity and standard tooling versus native
ownership and flexibility.
```

---

## 3. FlatList In 30 Seconds

```text
FlatList is a virtualized list for large data. It renders a window of rows instead
of everything, which protects memory and scroll performance. I use stable keys,
memoized rows, stable renderItem, pagination guards, image optimization, and
getItemLayout for fixed-height rows. I avoid index keys and row-local state that
must survive virtualization.
```

---

## 4. Navigation In 30 Seconds

```text
I model navigation as a tree: auth stack for logged-out users, app tabs/stacks
for logged-in users, and typed params between screens. Auth state should switch
the available route tree so users cannot back-navigate into private or login
screens incorrectly. Deep links should be validated, auth-aware, and tested for
cold and warm app launches.
```

---

## 5. State Management In 30 Seconds

```text
I classify state before choosing tools. Local UI state stays local. Server data
belongs in a query/cache layer. Auth, theme, and feature flags can live in a small
global store or context. Secrets go into secure storage. Forms use local state for
simple cases and a form library/schema validation for complex flows.
```

---

## 6. Performance In 60 Seconds

```text
React Native performance starts with the frame budget: about 16.67 ms for 60 FPS.
I debug in release builds and separate JS FPS from UI FPS. JS drops point to heavy
rendering, expensive calculations, bad list rows, logging, or state churn. UI drops
point to native drawing, images, shadows, layout, or animation work. For startup,
I reduce bundle size, defer non-critical initialization, avoid blocking storage
reads, and render a useful shell quickly. Every fix should be measured before and
after.
```

---

## 7. New Architecture In 60 Seconds

```text
The New Architecture modernizes React Native internals. JSI provides a lower-level
interface between JavaScript runtimes and native/C++ objects. Fabric is the newer
renderer that handles the render, commit, and mount pipeline for native UI.
TurboModules are the newer native module system with typed specs and Codegen.
These improve native integration foundations, but they do not remove the need to
write efficient React code and profile JS/UI thread bottlenecks.
```

---

## 8. Security In 60 Seconds

```text
Mobile apps run on devices we do not control, so the backend is the real trust
boundary. I store tokens in secure storage, keep true secrets on the backend,
avoid logging PII or tokens, validate deep links, and use HTTPS. Client-side role
checks are only UX; backend authorization must enforce access. For release, I
upload source maps, monitor crash-free users, and use staged rollout and kill
switches for risky features.
```

---

## 9. Testing In 60 Seconds

```text
My React Native testing strategy starts with TypeScript and lint. I unit test pure
logic like validators, DTO mappers, reducers, retry logic, and offline queues. I
component-test user-visible behavior with React Native Testing Library. I use
integration tests for screens with providers and mocked APIs. For critical flows
like login, checkout, and deep links, I add E2E tests with tools like Detox,
Maestro, or Appium. CI should also build iOS and Android because JS tests alone
do not prove native builds work.
```

---

## 10. Networking In 60 Seconds

```text
I avoid scattering fetch calls across screens. I use a central API client for
base URL, auth headers, request IDs, timeout, cancellation, error mapping, and
safe telemetry. Server state goes through a query/cache layer. Token refresh uses
a single-flight refresh to avoid request storms, and retries are allowed only for
safe or idempotent operations. For realtime, I design WebSocket reconnect,
heartbeat, token refresh, and missed-event recovery.
```

---

## 11. Debugging In 60 Seconds

```text
I debug React Native across layers. For JS and render issues, I use React Native
DevTools, component inspection, logs, and profiling. For native issues, I use
Xcode logs, Android Studio Logcat, crash reports, and native symbols. For
release-only issues, I reproduce with a release build, verify environment config,
bundled assets, minification/shrinker behavior, source maps, and native dependency
setup. Cache clearing is not a root-cause analysis.
```

---

## 12. Build And Release In 60 Seconds

```text
React Native releases have two paths: native binary releases and OTA JS/asset
updates. Native modules, permissions, entitlements, and dependency changes need
an app-store binary. OTA is safe only when compatible with installed binaries.
CI should run TS, lint, tests, native builds, E2E smoke for critical flows, upload
source maps/symbols, and release through TestFlight, Play tracks, EAS, or native
pipelines with staged rollout and crash monitoring.
```

---

## 13. Accessibility And I18n In 60 Seconds

```text
I make accessibility and localization part of the design system. Buttons, inputs,
tabs, modals, and icon buttons should require labels, roles, states, flexible
layout, and dynamic text support. I avoid color-only meaning and fixed text
heights. For i18n, I use translation keys, pluralization, locale-aware
date/currency formatting, and test long strings plus RTL layouts.
```

---

## 14. Key Numbers

| Number | Use In Interview |
|---|---|
| 60 FPS | Smooth UI target |
| 16.67 ms | Frame budget at 60 FPS |
| 100 ms | Interaction feels immediate-ish |
| 1 second | User notices delay; show feedback |
| 3 seconds+ | High frustration/abandonment risk |
| 44 x 44 pt approx | Common minimum touch target guidance |

---

## 15. High-Frequency Traps

| Trap | Correct Answer |
|---|---|
| "React Native renders HTML" | It renders native host views |
| "Use ScrollView for feed" | Use FlatList/SectionList for growing lists |
| "Put all state in Redux" | Classify state first |
| "Store tokens in AsyncStorage" | Use secure storage |
| "Profile in dev mode" | Use release builds |
| "Pass whole objects in navigation params" | Pass serializable IDs |
| "OTA can update anything" | OTA must stay compatible with installed native binary |
| "Client secrets are safe" | Mobile app contents are inspectable |
| "Snapshots prove UI works" | Prefer user-centric assertions |
| "New Architecture fixes all jank" | Bad JS/UI work can still cause jank |
| "Retry every failed request" | Retry only safe/idempotent operations |
| "Clear cache fixed it" | Find the durable root cause |
| "OTA can ship native modules" | Native changes need binary release |
| "Icon-only buttons are self-explanatory" | They still need accessible labels |

---

## 16. Final Readiness Checklist

You are interview-ready when you can explain:
- React Native vs React Web.
- Expo vs bare.
- Core components and styling differences.
- Props, state, hooks, stale closures.
- Navigation, auth flow, Android back, deep links.
- Local state vs server state vs persistent state.
- Permissions and secure storage.
- FlatList virtualization and row optimization.
- JS thread vs UI thread.
- New Architecture vocabulary.
- Startup, memory, image, and list performance.
- Testing pyramid and E2E trade-offs.
- Release engineering and OTA constraints.
- Offline queue and idempotency.
- API clients, timeout, cancellation, auth refresh, and WebSocket recovery.
- DevTools, LogBox, Xcode/Logcat, source maps, and release-build debugging.
- iOS archive, Android AAB/signing, versioning, staged rollout, and RN upgrades.
- Accessibility and i18n through reusable design-system primitives.
- Production app architecture for millions of users.

---

## 17. Last-Minute Strong Close

Use this when an interviewer asks for senior production judgment:

```text
In React Native, I try to keep the JavaScript side declarative and cheap, put
native capabilities behind typed wrappers, classify state before choosing tools,
profile in release builds, centralize networking policy, design accessible
primitives, and release assuming old app versions will exist for a while. That
is the difference between building screens and operating a production mobile app.
```
