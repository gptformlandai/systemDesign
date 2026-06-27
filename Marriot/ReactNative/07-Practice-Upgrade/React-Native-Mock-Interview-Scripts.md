# React Native Mock Interview Scripts — Gold Sheet

> Track File #27 of 37 · Group 7: Practice Upgrade
> Level: all levels | Mode: timed mock rounds — simulate real interview pressure

---

## 1. How to Use This Sheet

Set a timer. Answer out loud as if the interviewer is in front of you. Do not look at notes during the timed portion. After each round, score your answers using the scoring rubric sheet.

**Format per round**: 5-6 questions, 25-35 minutes total. Follow-up questions are in italic.

---

## 2. Round 1: Foundations (25 minutes)

**Interviewer**: "Let's start with fundamentals. Tell me how React Native actually renders UI."

Hint: Start with the rendering model comparison to web. Cover: components → shadow tree → native views → Yoga layout.

---

**Interviewer**: "What is the New Architecture and why does it matter?"

Hint: JSI → synchronous bridge replacement. Fabric = new renderer. TurboModules = lazy native modules. Why: better animation performance, synchronous native calls, shared memory.

*Follow-up*: "Is it backwards compatible? How does the migration work?"

---

**Interviewer**: "Walk me through what happens when a user types in a TextInput."

Hint: User presses key → native keyboard event → native TextInput updates → JS receives `onChangeText` callback → React state update → re-render.

*Follow-up*: "Why does React Native default to `onChangeText` returning a string directly instead of a synthetic event?"

---

**Interviewer**: "Explain the difference between Expo managed workflow and bare React Native. When would you choose each?"

Hint: Expo managed = config plugins, EAS, OTA, no Xcode/Android Studio needed. Bare = full native access, custom native code, more control. Choose Expo for 95% of apps. Choose bare only when native module requirements exceed Expo's config plugin capabilities.

---

**Interviewer**: "How does StyleSheet.create work and why is it better than inline style objects?"

Hint: `StyleSheet.create` creates an immutable style registry at app init — each style gets an integer ID. In production, only the integer is passed to native instead of a JavaScript object on every render. Inline style objects are new JavaScript object references on every render — more garbage collection, slightly more memory.

---

## 3. Round 2: Hooks and State (30 minutes)

**Interviewer**: "What is the dependency array in useEffect and what happens if you get it wrong?"

---

**Interviewer**: "I'm seeing a bug where a counter stops incrementing after the first click. Here's the code:"

```tsx
useEffect(() => {
  const id = setInterval(() => setCount(count + 1), 1000);
  return () => clearInterval(id);
}, []);
```

"What's wrong and how would you fix it?"

Hint: Stale closure. count is captured as 0 at mount. Fix: `setCount(prev => prev + 1)`.

*Follow-up*: "If I add `count` to the dependency array instead, what happens?"

---

**Interviewer**: "When should you use useReducer instead of useState?"

Hint: Complex state where multiple values transition together (loading/error/data). State machine behavior. When the update logic is testable independently. When multiple setState calls need to be atomic.

---

**Interviewer**: "Explain the re-render problem with React Context and how you'd solve it."

Hint: All consumers re-render on any context value change. Solutions: split contexts by update frequency, memoize context value with useMemo, use Zustand/Redux for high-frequency state.

---

**Interviewer**: "Design a `useAsync` hook. What should it return and how should it handle cleanup?"

Hint: Returns `{status: 'idle'|'loading'|'success'|'error', data, error, execute}`. Uses `cancelled` flag in cleanup to prevent setState after unmount. `useCallback` on execute for stable reference.

---

## 4. Round 3: Architecture and Data (35 minutes)

**Interviewer**: "Walk me through how you would design the data layer for a social feed screen."

Expected coverage: TanStack Query for server state, infinite scroll with `useInfiniteQuery`, optimistic likes with `useMutation`, FlatList virtualization, pull-to-refresh. Mention what NOT to put in Redux (server data).

*Follow-up*: "How does TanStack Query handle the case where two different screens both read the same user profile?"

Hint: Normalized cache — same query key = same cache entry. Both screens share the same cache. Mutation to user profile invalidates the key and both screens refresh.

---

**Interviewer**: "Describe your approach to navigation architecture for a large app with auth, tabs, modals, and deep links."

Expected coverage:
- Root navigator: AuthStack vs MainStack (conditional on auth state)
- Tab navigator inside MainStack
- Modal stack for full-screen modals (separate from tab navigator for clean presentation)
- Deep link configuration mapped to navigator routes
- Navigation ref for notification taps outside component tree
- TypeScript param list for all navigators

---

**Interviewer**: "What is the difference between optimistic UI and normal mutation handling? When is each appropriate?"

Hint: Optimistic = update UI immediately, rollback if server fails. Normal = wait for server response, then update UI. Optimistic for: likes, cart, follow, read receipts — low-stakes, fast feedback. Normal for: payment, irreversible actions, complex server computation.

---

**Interviewer**: "A junior engineer on your team asks: 'Should I store all app state in Redux?' How do you respond?"

Expected answer: No. Distinguish server state (TanStack Query), UI state (useState/useReducer), global persistent state (Zustand for cart, auth token), and derived state (useMemo). Redux is appropriate for complex shared global state with many cross-cutting concerns. Most modern apps use TanStack Query + Zustand + local state.

---

## 5. Round 4: Performance and Debugging (30 minutes)

**Interviewer**: "Users report the app is slow on older Android devices. Walk me through your investigation process."

Expected steps:
1. Reproduce on same device class
2. Enable Perf Monitor overlay (FPS numbers)
3. If JS FPS drops: React Native DevTools Profiler to find expensive render/computation
4. If UI FPS drops: check gesture/animation running on JS thread vs UI thread
5. Check bundle size
6. Check list virtualization — getItemLayout, React.memo on rows, keyExtractor stability

---

**Interviewer**: "Explain what causes a memory leak in React Native and give two examples of how to fix one."

Hint: (1) useEffect subscription without cleanup — fix: return cleanup function. (2) useState on unmounted component — fix: cancelled flag or AbortController. Demonstrate the pattern.

---

**Interviewer**: "A FlatList has 1000 items and is jank-free on iOS but janky on Android. Same code. Why?"

Hint: Android has lower JS thread priority by default. Possible causes: renderItem is doing expensive work (filter/sort), inline function props causing row re-renders (React.memo not working), image decoding blocking JS thread (use react-native-fast-image), `getItemLayout` not provided (forces layout measurement on every render).

---

**Interviewer**: "What tools do you use to debug a crash that happens only in production release builds, not debug builds?"

Expected: Sentry + source maps (symbolicated crash reports), `__DEV__` flag differences, release-mode Metro minification exposing edge cases, ProGuard on Android stripping class names. Steps: reproduce on release build locally, check Sentry crash trace, symbolicate native crash with dSYM (iOS) or mapping file (Android).

---

## 6. Round 5: Senior / System Design (40 minutes)

**Interviewer**: "Design a mobile app notifications system. Tell me the architecture from server push to user tap to screen navigation."

Expected coverage:
- Server → FCM/APNs → device
- Three app states: foreground (listener), background (OS), killed (getLastNotificationResponseAsync)
- Push token registration and sync to server
- Notification routing based on data payload type
- Navigation ref for navigation outside component tree
- Badge count management
- Silent push for background data refresh

---

**Interviewer**: "How would you approach making a React Native app work offline?"

Expected coverage:
- Distinguish what needs to work offline (reads vs writes)
- React Query / TanStack Query with AsyncStorage persistence for cache
- Offline mutation queue: queue writes, sync when online
- Optimistic updates for immediate feedback
- Conflict resolution: last-write-wins vs server-wins
- Network status detection (NetInfo)
- UX: offline banners, disabled actions, pending indicators

---

**Interviewer**: "Walk me through your process for shipping a major feature to 10 million users with zero downtime."

Expected coverage:
- Feature flags (enable for 1% → 5% → 25% → 100%)
- Staged rollout in App Store / Play Store
- OTA update for JS-only fixes
- Sentry monitoring + alerting on crash rate spike
- Rollback plan: halt staged rollout, revert feature flag, emergency OTA if needed
- A/B testing: measure metrics before full rollout
- Analytics: funnel events to measure conversion impact

---

## 7. After Each Round — Self-Debrief

```text
1. What questions did I answer confidently?
2. Where did I hesitate or lose structure?
3. Did I give concrete examples or stay abstract?
4. Did I mention trade-offs or just describe one approach?
5. Did I connect my answer to production impact?
6. What would I look up before my next interview session?
```

The goal is not perfection on the first attempt. The goal is to identify gaps, study the relevant sheet, and score better on the next attempt. One mock round per day for 2 weeks produces significant fluency gains.

---

## 8. Revision Notes

- Round 1: Core mental model, rendering, Expo vs bare, StyleSheet
- Round 2: Hooks — stale closures, useReducer, context re-render trap
- Round 3: Data architecture — TanStack Query, navigation design, optimistic UI
- Round 4: Debugging and performance — profiling tools, memory leaks, production crashes
- Round 5: Senior system design — notifications, offline, feature rollout
- Always structure answers: mental model → mechanism → trade-offs → production judgment
- Concrete examples beat abstract descriptions every time
- Mentioning what you would NOT do shows senior engineering judgment
