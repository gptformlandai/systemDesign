# React Native Topic Coverage Checklist

> Track Coverage Map
> Purpose: verify beginner-to-pro mastery across the whole React Native track

---

## 1. How To Use This Checklist

Use this after each group:

```text
Learn:
  read the sheet and explain the mental model

Build:
  implement the code/lab pattern once

Debug:
  identify failure modes and production symptoms

Interview:
  answer the strong-answer prompt out loud
```

Mark a topic complete only when you can explain it without reading notes.

---

## 2. Beginner Foundation

| Topic | Covered In | Mastery Gate |
|---|---|---|
| React Native mental model | Core Foundations | Explain why RN is not a WebView |
| Expo-first setup | Developer Environment | Create and run app on real device |
| Core components | Components/Props/State/Hooks | Build screen with View/Text/TextInput/Pressable |
| Hooks | Hooks Deep Dive | Explain stale closure and cleanup |
| Styling/Flexbox | Styling/Flexbox | Build responsive safe-area layout |
| Web-to-RN differences | Web Developer Bridge | List 7 browser assumptions that fail |
| Error handling | Error Boundaries | Explain render vs async vs native errors |

---

## 3. App Architecture

| Topic | Covered In | Mastery Gate |
|---|---|---|
| Navigation | Navigation/Routing/Deep Linking | Design auth stack and deep link flow |
| State management | State/Data/Forms | Choose local/server/global state correctly |
| Forms | State/Data/Forms | Build validated form with keyboard behavior |
| TypeScript architecture | Project Architecture/TypeScript | Type DTOs, domain models, and boundaries |
| Advanced TypeScript | TypeScript Deep Dive | Use discriminated unions and typed routes |
| Server state | TanStack Query | Explain cache, stale time, invalidation, mutation |
| Modern React compatibility | Modern React 19/Compiler | Separate React core from web/framework APIs |

---

## 4. Native And Device Internals

| Topic | Covered In | Mastery Gate |
|---|---|---|
| Permissions/storage | Native APIs/Permissions/Storage | Pick correct storage for token/cache/file |
| New Architecture | Fabric/TurboModules/JSI | Explain JS -> shadow tree -> native view |
| Native modules | Native Modules/Codegen | Design a typed TurboModule API |
| Device APIs | Device APIs Deep Dive | Design camera/location/payment flow safely |
| Animations/lists | Animations/Gestures/Lists | Tune a slow FlatList/FlashList |
| Push/background | Push Notifications/Background Tasks | Explain foreground/background behavior |
| Platform-specific code | Platform Patterns | Choose Platform.OS vs file split |
| Custom hooks | Custom Hooks Library | Extract reusable async/network/keyboard hooks |

---

## 5. Senior / MAANG Readiness

| Topic | Covered In | Mastery Gate |
|---|---|---|
| Performance | Performance/Memory/Debugging | Profile slow screen and name JS/UI bottleneck |
| Testing | Testing/Quality Gates | Define unit/component/E2E quality gates |
| Visual/device testing | Visual Testing/Device Farms | Design screenshot, device-farm, release smoke, and performance gates |
| Security | Security/Offline/Release/Observability | Explain token, PII, deep-link risks |
| Auth security | Auth OAuth/PKCE/Passkeys | Explain PKCE, state/nonce, secure callback, token refresh |
| Offline sync | Offline Database/Sync Engine | Design durable queue, idempotency, conflicts |
| Observability | Mobile Observability/RUM/SLO | Define crash/startup/update dashboards |
| Production architecture | Production App Architecture | Explain feature flags, modules, ownership |
| GraphQL | GraphQL Apollo/URQL | Compare REST/TanStack Query vs GraphQL |

---

## 6. Gold-Level Completeness

| Topic | Covered In | Mastery Gate |
|---|---|---|
| Networking/realtime | Networking/API Clients/Realtime | Build auth refresh without refresh storm |
| Debugging | Debugging DevTools/Native/Release | Triage release-only crash |
| Build/release/CI | Build/Release/Upgrades/CICD | Explain binary vs OTA release |
| Modern Expo | Expo/EAS/CNG/Config Plugins | Design dev build and runtimeVersion strategy |
| Compliance | App Store Compliance | Build data inventory and permission review |
| Monorepo governance | Enterprise Monorepo | Define package/native dependency ownership |
| Accessibility/i18n/design | Accessibility/I18n/Design Systems | Build accessible global-ready component |

---

## 7. Practice And Capstone

| Topic | Covered In | Mastery Gate |
|---|---|---|
| Scenario practice | Scenario Bank | Answer 10 scenario prompts |
| Machine coding | Mini Labs/Runnable Labs | Complete timed hooks/API/offline labs |
| Debugging cases | Production Debugging Cases | Explain root cause and mitigation |
| Mock interviews | Mock Scripts | Complete 5 timed mock rounds |
| Rubrics | Scoring Rubrics | Score 4+ on senior gates |
| Roadmaps | 2-Week/4-Week Roadmaps | Finish chosen plan |
| Capstone | Production Capstone | Explain full app architecture end-to-end |

---

## 8. Final Mastery Gate

You are React Native PRO-ready when you can:

- create a production Expo-first app from scratch
- explain RN rendering and New Architecture at a high level
- build typed navigation, forms, API clients, and server-state flows
- choose local/global/server/offline state correctly
- design permission-safe device API flows
- optimize lists, startup, and heavy interactions
- implement offline queue with idempotency and conflict handling
- operate releases with EAS/native builds, OTA boundaries, and staged rollout
- define crash/startup/RUM/SLO dashboards
- pass app store privacy/compliance review basics
- answer scenario questions with trade-offs, failure modes, and alternatives

---

## 9. Last Revision Pass

Before an interview, explain these out loud:

1. Why React Native is not a WebView.
2. Expo Go vs development builds.
3. JS thread vs UI thread.
4. FlatList performance tuning.
5. Secure storage vs AsyncStorage.
6. Deep links and auth flows.
7. Push notifications in foreground/background.
8. OTA update boundaries.
9. Offline mutation queue with idempotency.
10. Mobile observability metrics and crash triage.
