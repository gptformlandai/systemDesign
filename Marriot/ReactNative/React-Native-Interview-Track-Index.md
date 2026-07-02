# React Native Interview Track Index

This folder is the React Native track for mobile application interviews, MAANG-level frontend/mobile engineering, and production app architecture.

Audience:
- You know JavaScript or TypeScript basics.
- You want React Native notes split like the Python track: starter, intermediate, senior, and scenario practice.
- You want one organized learning path that teaches app building, native internals, performance, release engineering, and interview communication.

Track size: **49 learning files, 9 groups**, plus this index and one topic coverage checklist.

Current baseline checked on July 2, 2026:
- React Native docs show version `0.86`.
- The official React Native docs recommend starting most new apps with a React Native framework, commonly Expo, while still supporting bare React Native for unusual native constraints.
- Expo's current production path emphasizes Expo Go for learning, development builds for real app development, config plugins/CNG for native configuration, and EAS for build/submit/update workflows.
- Always verify exact package versions before creating a new production app.

Goal:
- Build React Native from fundamentals to production-grade mobile architecture.
- Make every topic interview-ready using mental model, definition, internals, code, traps, production judgment, scenario answer, and revision notes.
- Cover both Expo-style product development and bare/native internals because senior interviews can test both.

---

## How To Read These Notes

Read in order if React Native is new to you. If you already build apps, jump to Group 3 and Group 4 for internals, performance, release, security, and production debugging.

Use the checklist as your progress tracker:

| File | Purpose |
|---|---|
| `React-Native-Topic-Coverage-Checklist.md` | Beginner-to-pro coverage map and final mastery gates |

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

## 0. Setup And Tooling Path (1 file)

Start here if you are new to mobile tooling.

| # | File | What It Builds |
|---:|---|---|
| 1 | `00-Setup-And-Tooling/React-Native-Developer-Environment-Expo-First-Gold-Sheet.md` | Expo-first setup, real device strategy, Expo Go vs development builds, app config, env config, troubleshooting |

Setup target:
- You can create and run a new React Native app.
- You understand why modern RN docs recommend a framework for most new apps.
- You know when Expo Go is enough and when a development build is required.
- You can document a reproducible team setup.

---

## 1. Starter Path (6 files)

Read these first. They build React Native intuition without assuming native app experience.

| # | File | What It Builds |
|---:|---|---|
| 2 | `01-Starter-Path/React-Native-Core-Foundations-Master-Sheet.md` | What React Native is, Expo vs bare, app startup, native views, Metro, Hermes, basic mental model |
| 3 | `01-Starter-Path/React-Native-Components-Props-State-Hooks-Gold-Sheet.md` | Core components, props, state, effects, callbacks, controlled inputs, render behavior |
| 4 | `01-Starter-Path/React-Native-Styling-Flexbox-Responsive-UI-Gold-Sheet.md` | StyleSheet, Flexbox, SafeArea, keyboard handling, responsive layouts, platform styles |
| 5 | `01-Starter-Path/React-Native-Web-Developer-Bridge-Gold-Sheet.md` | Web to RN mapping: DOM vs native views, CSS vs StyleSheet, text inheritance, storage, events, animations |
| 6 | `01-Starter-Path/React-Native-Hooks-Deep-Dive-Gold-Sheet.md` | Hook rules, state batching, effects, cleanup, refs, callbacks, memoization, reducer, custom hooks |
| 7 | `01-Starter-Path/React-Native-Error-Handling-Error-Boundaries-Gold-Sheet.md` | Error layers, Error Boundary class pattern, async errors, global handlers, Sentry, retry with backoff |

Starter target:
- You can explain React Native without calling it a WebView.
- You can build screens using core native components.
- You can manage state, effects, inputs, and event handlers safely.
- You can style mobile screens with Flexbox, safe areas, and platform awareness.
- You understand every major React hook and can explain pitfalls under pressure.

---

## 2. App Architecture Path (6 files)

Use these after fundamentals. They teach how real apps are structured.

| # | File | What It Builds |
|---:|---|---|
| 8 | `02-App-Architecture/React-Native-Navigation-Routing-Deep-Linking-Gold-Sheet.md` | Stack, tabs, drawers, Expo Router, React Navigation, params, auth flows, deep links |
| 9 | `02-App-Architecture/React-Native-State-Management-Data-Fetching-Forms-Gold-Sheet.md` | Local state, server state, cache, Zustand deep dive, Redux vs Zustand matrix, RHF forms |
| 10 | `02-App-Architecture/React-Native-Project-Architecture-TypeScript-Gold-Sheet.md` | Feature folders, TypeScript, API clients, DTOs, domain models, dependency boundaries |
| 11 | `02-App-Architecture/React-Native-TypeScript-Deep-Dive-Gold-Sheet.md` | Strict mode, navigation typing end-to-end, discriminated unions, generics, Zod runtime validation |
| 12 | `02-App-Architecture/React-Native-TanStack-Query-Data-Fetching-Internals-Gold-Sheet.md` | useQuery, useMutation optimistic updates, useInfiniteQuery, AppState focus, offline persistence |
| 13 | `02-App-Architecture/React-Native-Modern-React-19-Compiler-Compatibility-Gold-Sheet.md` | React 19-era feature judgment, React Compiler, Suspense caution, RN vs web/framework compatibility |

Architecture target:
- You can design navigation for authenticated and unauthenticated users.
- You can choose the right state tool instead of putting everything in Redux.
- You can structure a production mobile repo so screens stay thin and business logic is testable.
- You can type navigation params, discriminated union states, and generic components end-to-end.
- You can manage server state with caching, refetch, and optimistic mutations.
- You can adopt modern React features without assuming DOM, Next.js, or browser APIs exist.

---

## 3. Native Device And Internals Path (8 files)

This is where React Native becomes mobile engineering rather than just React syntax.

| # | File | What It Builds |
|---:|---|---|
| 14 | `03-Native-Device-And-Internals/React-Native-Native-APIs-Permissions-Storage-Gold-Sheet.md` | Permissions, secure storage, local storage, files, camera, notifications, app lifecycle |
| 15 | `03-Native-Device-And-Internals/React-Native-New-Architecture-Fabric-TurboModules-JSI-Gold-Sheet.md` | JSI deep dive, Fabric render pipeline phases, TurboModules codegen, Hermes, bridgeless mode |
| 16 | `03-Native-Device-And-Internals/React-Native-Native-Modules-Codegen-Swift-Kotlin-Gold-Sheet.md` | Custom native modules, Codegen, TurboModule specs, Swift/Kotlin implementation shape, native events |
| 17 | `03-Native-Device-And-Internals/React-Native-Device-APIs-Camera-Maps-BLE-Payments-Gold-Sheet.md` | Camera, maps, location, BLE, NFC, files, sensors, payments, permission-safe device API design |
| 18 | `03-Native-Device-And-Internals/React-Native-Animations-Gestures-Lists-Gold-Sheet.md` | FlatList vs FlashList, virtualization, Reanimated worklets, Gesture Handler, swipe patterns |
| 19 | `03-Native-Device-And-Internals/React-Native-Push-Notifications-Background-Tasks-Gold-Sheet.md` | FCM vs APNs, app state behavior table, deep linking from tap, background fetch limits, silent push |
| 20 | `03-Native-Device-And-Internals/React-Native-Platform-Specific-Patterns-Gold-Sheet.md` | Platform.OS, .ios.tsx/.android.tsx, SafeAreaView, keyboard behavior, shadows, BackHandler, biometric |
| 21 | `03-Native-Device-And-Internals/React-Native-Custom-Hooks-Pattern-Library-Gold-Sheet.md` | Production hooks: useAsync, useFetch, usePaginatedList, useNetworkStatus, useKeyboardDimensions, useDebounce |

Native target:
- You can explain how JavaScript creates native UI.
- You can reason about JS thread vs UI thread.
- You can choose secure storage, local cache, permissions, and background behavior correctly.
- You can design device API flows with real permission, fallback, and privacy handling.
- You can explain New Architecture, Codegen, Fabric, TurboModules, and native module trade-offs.

---

## 4. Senior / MAANG Path (9 files)

These are the pro sheets for production interviews.

| # | File | What It Builds |
|---:|---|---|
| 22 | `04-Senior-MAANG/React-Native-Performance-Memory-Debugging-MAANG-Master-Sheet.md` | 60 FPS, Hermes profiler workflow, FlatList playbook, memory leak patterns, startup optimization |
| 23 | `04-Senior-MAANG/React-Native-Testing-Quality-Gates-Gold-Sheet.md` | Type checking, unit tests, component tests, native mocks, E2E, Detox/Maestro, CI quality gates |
| 24 | `04-Senior-MAANG/React-Native-Visual-Testing-Device-Farms-Performance-Gates-Gold-Sheet.md` | Visual regression, Storybook/component catalog, device farms, release smoke, accessibility and performance gates |
| 25 | `04-Senior-MAANG/React-Native-Security-Offline-Release-Observability-MAANG-Master-Sheet.md` | Token safety, PII, offline sync, retries, app releases, OTA updates, crash analytics, telemetry |
| 26 | `04-Senior-MAANG/React-Native-Auth-OAuth-PKCE-Passkeys-Deep-Link-Security-Gold-Sheet.md` | OAuth/OIDC with PKCE, state/nonce, deep link callback security, passkeys, biometrics, token refresh |
| 27 | `04-Senior-MAANG/React-Native-Offline-First-Database-Sync-Engine-Gold-Sheet.md` | Local database selection, durable mutation queue, idempotency, conflict resolution, migrations, sync cursors |
| 28 | `04-Senior-MAANG/React-Native-Mobile-Observability-RUM-SLO-Gold-Sheet.md` | Crash-free sessions, startup p95, RUM, OTA attribution, dashboards, privacy-safe telemetry |
| 29 | `04-Senior-MAANG/React-Native-Production-App-Architecture-MAANG-Master-Sheet.md` | Feature flags, modularization, API contracts, accessibility, i18n, design systems, app system design |
| 30 | `04-Senior-MAANG/React-Native-GraphQL-Apollo-URQL-Gold-Sheet.md` | Apollo client, useQuery, useMutation optimistic update, subscriptions, fragment colocation, URQL trade-offs |

Senior target:
- You can debug slow screens using evidence, not guesses.
- You can describe mobile security and release risk with maturity.
- You can design a large mobile app used by millions of users.
- You can build offline-first flows with idempotency, conflicts, tombstones, and sync observability.
- You can define reliability metrics and operate a production mobile app after release.
- You can defend auth callback security, token storage, release-quality testing, and device fragmentation strategy.

---

## 5. Scenario Practice Path (6 files)

Use these after the concept sheets. They convert notes into interview fluency.

| # | File | What It Builds |
|---:|---|---|
| 31 | `05-Scenario-Practice/React-Native-MAANG-Interview-Scenario-Bank.md` | Scenario questions with strong answers and follow-ups |
| 32 | `05-Scenario-Practice/React-Native-Machine-Coding-Mini-Labs.md` | Hands-on hooks, retry, forms, list optimization, offline queue, navigation typing |
| 33 | `05-Scenario-Practice/React-Native-Quick-Revision-Answer-Templates.md` | 30-second answers, traps, key numbers, final readiness checklist |
| 34 | `05-Scenario-Practice/React-Native-Tricky-Behavior-Questions-Gold-Sheet.md` | Code-snippet questions: batching, stale closure, mutation traps, memo failures, fetch behavior |
| 35 | `05-Scenario-Practice/React-Native-LLD-Machine-Coding-Design-Gold-Sheet.md` | Full LLD designs: offline cart, real-time chat, photo gallery, feature flag system |
| 36 | `05-Scenario-Practice/React-Native-Production-Debugging-Case-Studies-Gold-Sheet.md` | Case studies: frozen Android, memory leak, HTTPS block, navigation state loss, slow launch |

Scenario target:
- You can answer under pressure.
- You can implement common mobile patterns quickly.
- You can connect React Native mechanics to real production incidents.
- You can spot React behavior traps in code snippets without running the code.

---

## 6. Gold-Level Completeness Path (7 files)

These close advanced production gaps: networking, debugging, release, modern Expo, compliance, monorepos, accessibility, and global product readiness.

| # | File | What It Builds |
|---:|---|---|
| 37 | `06-Gold-Level-Completeness/React-Native-Networking-API-Clients-Realtime-Gold-Sheet.md` | API clients, timeouts, cancellation, auth refresh, HTTPS, WebSockets, uploads, error taxonomy |
| 38 | `06-Gold-Level-Completeness/React-Native-Debugging-DevTools-Native-Release-Gold-Sheet.md` | DevTools, LogBox, native logs, release-build debugging, source maps, crash triage |
| 39 | `06-Gold-Level-Completeness/React-Native-Build-Release-Upgrades-CICD-Gold-Sheet.md` | EAS/native builds, iOS/Android release, app signing, OTA boundaries, upgrades, CI/CD |
| 40 | `06-Gold-Level-Completeness/React-Native-Modern-Expo-EAS-CNG-Config-Plugins-Gold-Sheet.md` | Expo framework workflow, development builds, CNG/prebuild, config plugins, EAS Build/Submit/Update |
| 41 | `06-Gold-Level-Completeness/React-Native-App-Store-Compliance-Privacy-Metadata-Gold-Sheet.md` | App Store privacy, Google Play Data Safety, SDK inventory, permission copy, metadata, rejection playbook |
| 42 | `06-Gold-Level-Completeness/React-Native-Enterprise-Monorepo-Release-Governance-Gold-Sheet.md` | Workspaces, Metro, shared packages, native dependency governance, release trains, ownership |
| 43 | `06-Gold-Level-Completeness/React-Native-Accessibility-I18n-Design-Systems-Gold-Sheet.md` | Accessibility props, dynamic text, focus, RTL, pluralization, global design-system primitives |

Gold completeness target:
- You can design a real mobile networking layer instead of scattering `fetch`.
- You can debug JS, native, release-only, and production crash issues.
- You can own app-store release, OTA, versioning, upgrades, and CI/CD conversations.
- You can use modern Expo correctly instead of treating Expo as only a beginner tool.
- You can handle app store compliance, privacy metadata, and enterprise governance.

---

## 7. Practice Upgrade (5 files)

Active recall, labs, mock interviews, scoring rubrics, and study plans. Use these throughout, not just at the end.

| # | File | What It Builds |
|---:|---|---|
| 44 | `07-Practice-Upgrade/React-Native-Active-Recall-Question-Bank.md` | Questions across foundations/hooks/navigation/performance/security using cover-and-answer format |
| 45 | `07-Practice-Upgrade/React-Native-Runnable-Mini-Labs.md` | Timed labs with full solutions |
| 46 | `07-Practice-Upgrade/React-Native-Mock-Interview-Scripts.md` | Timed mock rounds: Foundation, Hooks, Architecture, Performance, Senior System Design |
| 47 | `07-Practice-Upgrade/React-Native-Interview-Scoring-Rubrics.md` | Rubrics on 1-5 scale and readiness gates |
| 48 | `07-Practice-Upgrade/React-Native-2-Week-4-Week-Mastery-Roadmaps.md` | Daily schedule for 2-week sprint and 4-week deep mastery |

Practice target:
- You can recall answers without looking at notes.
- You have built every lab pattern from scratch at least once.
- You have completed mock interviews and scored the rubrics.
- You know which topics need more study before your interview.

---

## 8. Production Capstone Path (1 file)

Finish here after the core and senior paths.

| # | File | What It Builds |
|---:|---|---|
| 49 | `08-Capstone/React-Native-Production-Capstone-App.md` | Integrated production app spec with auth, navigation, offline sync, device APIs, observability, release, testing, compliance |

Capstone target:
- You can integrate the full track into one production-style app.
- You can explain architecture, trade-offs, failure modes, release strategy, and observability.
- You can prove mastery through a build, not only notes.

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
- Modern React feature compatibility in a React Native host environment.

### Native Mobile Awareness

- iOS vs Android lifecycle differences.
- Permissions and privacy prompts.
- Secure storage vs normal storage.
- Push notifications and background constraints.
- Device APIs: camera, location, files, BLE, NFC, sensors, payments.
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

### Offline And Local Data

- Local database selection.
- Mutation queue design.
- Sync cursor and tombstones.
- Conflict resolution.
- Schema migration.
- Queue observability.
- OTA/native compatibility for storage changes.

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
- Crash triage by app version, build, device, OS, runtime, update id, and breadcrumbs.

### Production Engineering

- Crash reporting and source maps.
- Structured client telemetry and mobile RUM.
- OAuth/OIDC with PKCE, callback validation, token refresh, passkeys, and biometric trade-offs.
- Feature flags and staged rollout.
- Visual regression, device-farm testing, release-build smoke, and performance gates.
- App store release process.
- OTA update risk boundaries.
- API version compatibility.
- Security: tokens, PII, deep links, certificates, dependency hygiene.
- Accessibility and internationalization.
- App store privacy, Play Data Safety, SDK inventory, and metadata.
- CI/CD for iOS and Android.
- App signing, TestFlight, Play tracks, staged rollout, and rollback planning.
- React Native upgrades, native dependency hygiene, and New Architecture compatibility checks.
- Monorepo ownership, release trains, and mobile platform governance.

---

## Official Docs Checked

- React Native Introduction: https://reactnative.dev/docs/getting-started
- React Native Environment Setup: https://reactnative.dev/docs/environment-setup
- React Native Debugging: https://reactnative.dev/docs/debugging
- React Native Performance: https://reactnative.dev/docs/performance
- React Native Releases: https://reactnative.dev/docs/releases
- React Native Codegen: https://reactnative.dev/docs/the-new-architecture/using-codegen
- React Native Turbo Native Modules: https://reactnative.dev/docs/turbo-native-modules-introduction
- React Native Testing: https://reactnative.dev/docs/testing-overview
- React Native iOS Publishing: https://reactnative.dev/docs/publishing-to-app-store
- React Native Android Publishing: https://reactnative.dev/docs/signed-apk-android
- Expo Environment Setup: https://docs.expo.dev/get-started/set-up-your-environment/
- Expo App Config: https://docs.expo.dev/workflow/configuration/
- Expo Config Plugins: https://docs.expo.dev/config-plugins/introduction/
- Expo EAS Build: https://docs.expo.dev/deploy/build-project/
- Expo EAS Submit: https://docs.expo.dev/deploy/submit-to-app-stores/
- Expo OTA Updates: https://docs.expo.dev/deploy/send-over-the-air-updates/
- Expo Monitoring: https://docs.expo.dev/monitoring/services/
- Expo App Store Metadata: https://docs.expo.dev/deploy/app-stores-metadata/
- Expo Monorepos: https://docs.expo.dev/guides/monorepos/
- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Apple Privacy Manifest Files: https://developer.apple.com/documentation/bundleresources/privacy-manifest-files
- React Compiler: https://react.dev/learn/react-compiler
