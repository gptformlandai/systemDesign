# React Native Interview Track Index

This folder is the React Native track for mobile application interviews, MAANG-level frontend/mobile engineering, and production app architecture.

Audience:
- You know JavaScript or TypeScript basics.
- You want React Native notes split like the Python track: starter, intermediate, senior, and scenario practice.
- You want one organized learning path that teaches app building, native internals, performance, release engineering, and interview communication.

Track size: **37 files, 7 groups** — beginner to MAANG-ready.

Current baseline checked on June 27, 2026:
- React Native docs show version `0.86`.
- The official React Native docs recommend starting most new apps with a React Native framework, commonly Expo, while still supporting bare React Native for unusual native constraints.
- React Navigation docs used here follow the current 7.x examples.
- Always verify exact package versions before creating a new production app.

Goal:
- Build React Native from fundamentals to production-grade mobile architecture.
- Make every topic interview-ready using mental model, definition, internals, code, traps, production judgment, scenario answer, and revision notes.
- Cover both Expo-style product development and bare/native internals because senior interviews can test both.

---

## How To Read These Notes

Read in order if React Native is new to you. If you already build apps, jump to Group 3 and Group 4 for internals, performance, release, security, and production debugging.

The track follows this repeated pattern:

```text
Mental model
Definition
How it works
Code sample
Production judgment
Common traps
Interview answer
Revision notes
```

---

## Web Developer Bridge Pattern

Many React Native mistakes come from assuming the browser exists.

```text
Similar to React Web:
  Components, props, state, hooks, reconciliation, context, memoization.

Different in React Native:
  No DOM, no CSS cascade, no browser layout engine, no web storage by default,
  native views are created on iOS/Android, and performance depends on JS thread,
  UI thread, native modules, list virtualization, and device constraints.

React Native replacement:
  View, Text, Pressable, TextInput, Image, ScrollView, FlatList, StyleSheet,
  React Navigation or Expo Router, AsyncStorage/SecureStore, native modules,
  app lifecycle APIs, permissions, and platform-specific code.

Interview trap:
  Saying "React Native renders HTML on mobile." It does not. It renders native
  host views controlled by React through the React Native renderer.
```

---

## 1. Starter Path (6 files)

Read these first. They build React Native intuition without assuming native app experience.

| # | File | What It Builds |
|---:|---|---|
| 1 | `01-Starter-Path/React-Native-Core-Foundations-Master-Sheet.md` | What React Native is, Expo vs bare, app startup, native views, Metro, Hermes, basic mental model |
| 2 | `01-Starter-Path/React-Native-Components-Props-State-Hooks-Gold-Sheet.md` | Core components, props, state, effects, callbacks, controlled inputs, render behavior |
| 3 | `01-Starter-Path/React-Native-Styling-Flexbox-Responsive-UI-Gold-Sheet.md` | StyleSheet, Flexbox, SafeArea, keyboard handling, responsive layouts, platform styles |
| 4 | `01-Starter-Path/React-Native-Web-Developer-Bridge-Gold-Sheet.md` | Web→RN mapping: DOM vs native views, CSS vs StyleSheet, flex defaults, text inheritance, storage, events, animations, 7 traps |
| 5 | `01-Starter-Path/React-Native-Hooks-Deep-Dive-Gold-Sheet.md` | Hook rules, useState batching, useEffect 4 forms + cleanup + stale closure, useRef, useCallback, useMemo, useReducer, custom hooks |
| 6 | `01-Starter-Path/React-Native-Error-Handling-Error-Boundaries-Gold-Sheet.md` | 3 error layers, Error Boundary class pattern, async error state machine, global handler, Sentry, retry with backoff |

Starter target:
- You can explain React Native without calling it a WebView.
- You can build screens using core native components.
- You can manage state, effects, inputs, and event handlers safely.
- You can style mobile screens with Flexbox, safe areas, and platform awareness.
- You understand every major React hook and can explain pitfalls under pressure.

---

## 2. App Architecture Path (5 files)

Use these after fundamentals. They teach how real apps are structured.

| # | File | What It Builds |
|---:|---|---|
| 7 | `02-App-Architecture/React-Native-Navigation-Routing-Deep-Linking-Gold-Sheet.md` | Stack, tabs, drawers, Expo Router, React Navigation, params, auth flows, deep links |
| 8 | `02-App-Architecture/React-Native-State-Management-Data-Fetching-Forms-Gold-Sheet.md` | Local state, server state, cache, Zustand deep dive, Redux vs Zustand matrix, RHF forms |
| 9 | `02-App-Architecture/React-Native-Project-Architecture-TypeScript-Gold-Sheet.md` | Feature folders, TypeScript, API clients, DTOs, domain models, dependency boundaries |
| 10 | `02-App-Architecture/React-Native-TypeScript-Deep-Dive-Gold-Sheet.md` | Strict mode, navigation typing end-to-end, discriminated unions, generics, Zod runtime validation |
| 11 | `02-App-Architecture/React-Native-TanStack-Query-Data-Fetching-Internals-Gold-Sheet.md` | useQuery, useMutation optimistic updates, useInfiniteQuery, AppState focus, offline persistence |

Architecture target:
- You can design navigation for authenticated and unauthenticated users.
- You can choose the right state tool instead of putting everything in Redux.
- You can structure a production mobile repo so screens stay thin and business logic is testable.
- You can type navigation params, discriminated union states, and generic components end-to-end.
- You can manage server state with caching, refetch, and optimistic mutations.

---

## 3. Native Device And Internals Path (6 files)

This is where React Native becomes mobile engineering rather than just React syntax.

| # | File | What It Builds |
|---:|---|---|
| 12 | `03-Native-Device-And-Internals/React-Native-Native-APIs-Permissions-Storage-Gold-Sheet.md` | Permissions, secure storage, local storage, files, camera, notifications, app lifecycle |
| 13 | `03-Native-Device-And-Internals/React-Native-New-Architecture-Fabric-TurboModules-JSI-Gold-Sheet.md` | JSI deep dive, Fabric render pipeline phases, TurboModules codegen, Hermes AOT, bridgeless mode |
| 14 | `03-Native-Device-And-Internals/React-Native-Animations-Gestures-Lists-Gold-Sheet.md` | FlatList vs FlashList, virtualization, Reanimated worklets, Gesture Handler, swipe patterns |
| 15 | `03-Native-Device-And-Internals/React-Native-Push-Notifications-Background-Tasks-Gold-Sheet.md` | FCM vs APNs, app state behavior table, deep linking from tap, background fetch limits, silent push |
| 16 | `03-Native-Device-And-Internals/React-Native-Platform-Specific-Patterns-Gold-Sheet.md` | Platform.OS, .ios.tsx/.android.tsx, SafeAreaView, keyboard behavior, shadows, BackHandler, biometric |
| 17 | `03-Native-Device-And-Internals/React-Native-Custom-Hooks-Pattern-Library-Gold-Sheet.md` | 15 production hooks: useAsync, useFetch, usePaginatedList, useNetworkStatus, useKeyboardDimensions, useDebounce |

Native target:
- You can explain how JavaScript creates native UI (shadow tree → Yoga → host views).
- You can reason about JS thread vs UI thread.
- You can choose secure storage, local cache, permissions, and background behavior correctly.
- You can build fast lists, responsive gestures, and smooth animations using Reanimated 3.
- You can explain New Architecture (JSI, Fabric, TurboModules) end-to-end.

---

## 4. Senior / MAANG Path (5 files)

These are the pro sheets for production interviews.

| # | File | What It Builds |
|---:|---|---|
| 18 | `04-Senior-MAANG/React-Native-Performance-Memory-Debugging-MAANG-Master-Sheet.md` | 60 FPS, Hermes profiler workflow, FlatList complete playbook, memory leak patterns, startup optimization |
| 19 | `04-Senior-MAANG/React-Native-Testing-Quality-Gates-Gold-Sheet.md` | Type checking, unit tests, component tests, native mocks, E2E, Detox/Maestro, CI quality gates |
| 20 | `04-Senior-MAANG/React-Native-Security-Offline-Release-Observability-MAANG-Master-Sheet.md` | Token safety, PII, offline sync, retries, app releases, OTA updates, crash analytics, telemetry |
| 21 | `04-Senior-MAANG/React-Native-Production-App-Architecture-MAANG-Master-Sheet.md` | Feature flags, modularization, API contracts, accessibility, i18n, design systems, app system design |
| 22 | `04-Senior-MAANG/React-Native-GraphQL-Apollo-URQL-Gold-Sheet.md` | Apollo client, useQuery, useMutation optimistic update, subscriptions, fragment colocation, URQL trade-off |

Senior target:
- You can debug slow screens using evidence, not guesses.
- You can describe mobile security and release risk with maturity.
- You can design a large mobile app used by millions of users.
- You can explain trade-offs across UX, performance, native capability, app store constraints, and team velocity.
- You can make architectural decisions between REST + TanStack Query vs GraphQL + Apollo.

---

## 5. Scenario Practice Path (6 files)

Use these after the concept sheets. They convert notes into interview fluency.

| # | File | What It Builds |
|---:|---|---|
| 23 | `05-Scenario-Practice/React-Native-MAANG-Interview-Scenario-Bank.md` | Scenario questions with strong answers and follow-ups |
| 24 | `05-Scenario-Practice/React-Native-Machine-Coding-Mini-Labs.md` | Hands-on hooks, retry, forms, list optimization, offline queue, navigation typing |
| 25 | `05-Scenario-Practice/React-Native-Quick-Revision-Answer-Templates.md` | 30-second answers, traps, key numbers, final readiness checklist |
| 26 | `05-Scenario-Practice/React-Native-Tricky-Behavior-Questions-Gold-Sheet.md` | 15 code-snippet questions: state batching, stale closure, Array.push trap, React.memo failures, fetch not throwing |
| 27 | `05-Scenario-Practice/React-Native-LLD-Machine-Coding-Design-Gold-Sheet.md` | 4 full LLD designs: offline cart, real-time chat, photo gallery, feature flag system |
| 28 | `05-Scenario-Practice/React-Native-Production-Debugging-Case-Studies-Gold-Sheet.md` | 5 narrative case studies: frozen Android, memory leak, iOS HTTPS block, navigation state loss, 7-second launch |

Scenario target:
- You can answer under pressure.
- You can implement common mobile patterns quickly.
- You can connect React Native mechanics to real production incidents.
- You can sound senior without overcomplicating every answer.
- You can spot React behavior traps in code snippets without running the code.

---

## 6. Gold-Level Completeness Path (4 files)

These close the gaps that usually appear after a first React Native track: production networking, practical debugging, release ownership, upgrades, accessibility, and global product readiness.

| # | File | What It Builds |
|---:|---|---|
| 29 | `06-Gold-Level-Completeness/React-Native-Networking-API-Clients-Realtime-Gold-Sheet.md` | API clients, timeouts, cancellation, auth refresh, HTTPS, WebSockets, uploads, error taxonomy |
| 30 | `06-Gold-Level-Completeness/React-Native-Debugging-DevTools-Native-Release-Gold-Sheet.md` | DevTools, LogBox, native logs, release-build debugging, source maps, crash triage |
| 31 | `06-Gold-Level-Completeness/React-Native-Build-Release-Upgrades-CICD-Gold-Sheet.md` | EAS/native builds, iOS/Android release, app signing, OTA boundaries, upgrades, CI/CD |
| 32 | `06-Gold-Level-Completeness/React-Native-Accessibility-I18n-Design-Systems-Gold-Sheet.md` | Accessibility props, dynamic text, focus, RTL, pluralization, global design-system primitives |

Gold completeness target:
- You can design a real mobile networking layer instead of scattering `fetch`.
- You can debug JS, native, release-only, and production crash issues.
- You can own app-store release, OTA, versioning, upgrades, and CI/CD conversations.
- You can make accessibility and i18n scale through reusable primitives.

---

## 7. Practice Upgrade (5 files)

Active recall, labs, mock interviews, scoring rubrics, and study plans. Use these throughout — not just at the end.

| # | File | What It Builds |
|---:|---|---|
| 33 | `07-Practice-Upgrade/React-Native-Active-Recall-Question-Bank.md` | 29 questions across foundations/hooks/navigation/performance/security — cover-and-answer format |
| 34 | `07-Practice-Upgrade/React-Native-Runnable-Mini-Labs.md` | 7 timed labs: 3 foundation (5 min), 2 intermediate (15 min), 2 senior (30 min) with full solutions |
| 35 | `07-Practice-Upgrade/React-Native-Mock-Interview-Scripts.md` | 5 timed mock rounds (25-40 min each) — Foundation, Hooks, Architecture, Performance, Senior System Design |
| 36 | `07-Practice-Upgrade/React-Native-Interview-Scoring-Rubrics.md` | 9 rubrics on 1-5 scale — readiness gates for mid-level, senior, and MAANG |
| 37 | `07-Practice-Upgrade/React-Native-2-Week-4-Week-Mastery-Roadmaps.md` | Daily schedule for 2-week sprint and 4-week deep mastery — file-by-file study plan |

Practice target:
- You can recall answers without looking at notes.
- You have built every lab pattern from scratch at least once.
- You have completed all 5 mock interview rounds and scored rubrics.
- You know which topics need more study before your interview.
- You have a daily study plan and are following it.

---

## Interview Answer Pattern

Use this structure for most React Native answers:

1. Give a crisp definition.
2. Explain the mobile-specific mental model.
3. Describe the runtime path: React component -> shadow tree -> native view.
4. Mention JS thread vs UI thread when performance matters.
5. Give a TypeScript example.
6. Mention the common trap.
7. Close with production judgment and trade-offs.

Example:

```text
FlatList is React Native's virtualized list component for large scrollable data.
It renders a window of items instead of every row, which protects memory and keeps
scrolling smooth. The trade-off is that internal row state can be lost when rows
leave the render window, so row state should live in item data or external state.
For senior production work, I tune keys, getItemLayout, batch size, memoized rows,
image loading, and pagination based on measured JS/UI FPS in a release build.
```

---

## What A MAANG-Level React Native Engineer Should Master

### Product App Fundamentals

- React fundamentals: components, props, state, effects, memoization.
- React Native core components.
- TypeScript-first component APIs.
- Navigation and deep linking.
- Forms, validation, and keyboard behavior.
- API calls, loading states, error states, retries.
- Offline and poor-network UX.

### Native Mobile Awareness

- iOS vs Android lifecycle differences.
- Permissions and privacy prompts.
- Secure storage vs normal storage.
- Push notifications and background constraints.
- App state changes: foreground, background, inactive.
- Native dependency installation and linking.
- New Architecture concepts: Fabric, TurboModules, JSI, Codegen.

### Networking And Realtime

- API client boundaries.
- Fetch, request headers, response/error mapping.
- Timeouts and cancellation.
- Auth refresh without refresh storms.
- Idempotency for retries and offline mutations.
- HTTPS, cleartext restrictions, and certificate operational risk.
- WebSocket reconnect, heartbeat, token refresh, and missed-event recovery.
- Upload/download progress and cancellation.

### Performance

- 60 FPS means about 16.67 ms per frame.
- JS thread vs UI thread frame rate.
- List virtualization.
- Image loading and caching.
- Avoiding heavy renders.
- Avoiding expensive work during gestures and navigation transitions.
- Startup time and bundle size.
- Release-build profiling.

### Debugging And Tooling

- React Native DevTools.
- LogBox and when not to suppress warnings.
- Xcode and Android Studio native logs.
- Release-build reproduction.
- Source maps and native symbolication.
- Crash triage by app version, build, device, OS, and breadcrumbs.

### Production Engineering

- Crash reporting and source maps.
- Structured client telemetry.
- Feature flags and staged rollout.
- App store release process.
- OTA update risk boundaries.
- API version compatibility.
- Security: tokens, PII, deep links, certificates, dependency hygiene.
- Accessibility and internationalization.
- CI/CD for iOS and Android.
- App signing, TestFlight, Play tracks, staged rollout, and rollback planning.
- React Native upgrades, native dependency hygiene, and New Architecture compatibility checks.

---

## Official Docs Checked

- React Native Introduction: https://reactnative.dev/docs/getting-started
- React Native Environment Setup: https://reactnative.dev/docs/environment-setup
- React Native Navigation: https://reactnative.dev/docs/navigation
- React Native Networking: https://reactnative.dev/docs/network
- React Native Accessibility: https://reactnative.dev/docs/accessibility
- React Native AppState: https://reactnative.dev/docs/appstate
- React Native Debugging: https://reactnative.dev/docs/debugging
- React Native Performance: https://reactnative.dev/docs/performance
- React Native Render Pipeline: https://reactnative.dev/architecture/render-pipeline
- React Native Testing: https://reactnative.dev/docs/testing-overview
- React Native iOS Publishing: https://reactnative.dev/docs/publishing-to-app-store
- React Native Android Publishing: https://reactnative.dev/docs/signed-apk-android
- React Native Upgrading: https://reactnative.dev/docs/upgrading
- React Native Metro: https://reactnative.dev/docs/metro
- React Navigation: https://reactnavigation.org/docs/getting-started/
- Expo Router: https://docs.expo.dev/router/introduction/
- Expo Application Services: https://docs.expo.dev/eas/
