# React Native Active Recall Question Bank — Gold Sheet

> Track Module - Group 7: Practice Upgrade
> Level: all levels | Mode: answer from memory — no notes, no hints

---

## 1. How to Use This Sheet

**Rules:**
1. Cover the answer section below each question.
2. Answer out loud or write on paper first.
3. Only uncover after you have committed to an answer.
4. Mark: ✅ (got it cold) | ⚠️ (partial) | ❌ (blank or wrong)
5. Return to ❌ the next day; ⚠️ in 3 days.
6. A topic is not mastered until you score ✅ three times in a row.

**Difficulty tiers:**
- 🟢 Foundation — must answer without hesitation
- 🟡 Intermediate — answer with some thought
- 🔴 MAANG — explain mechanics and trade-offs cold

---

## 2. Foundations: React Native Core

### 2-A Foundations

🟢 **Q1:** What is the difference between React Native and a WebView app?

> Answer: React Native renders actual native platform views (UIView on iOS, android.view.View on Android). A WebView app wraps a browser inside a native container and renders HTML/CSS. React Native performance and look/feel is native because the views ARE native, not web content inside a browser shell.

🟢 **Q2:** What are the three core primitives in React Native that replace `<div>`, `<span>`, and `<button>`?

> Answer: `<View>` replaces `<div>` as a generic container. `<Text>` is the only component that can render text (all text must be wrapped in Text). `<Pressable>` (modern) or `<TouchableOpacity>` replace `<button>` for pressable elements.

🟢 **Q3:** What is Metro?

> Answer: Metro is the JavaScript bundler for React Native. It resolves modules, transforms TypeScript/JSX, creates the JS bundle that runs on the device, and powers Fast Refresh during development. It is the equivalent of webpack or Vite for React Native.

🟢 **Q4:** What is Hermes and why does it matter?

> Answer: Hermes is Meta's JavaScript engine optimized for React Native on mobile. It pre-compiles JavaScript to bytecode at build time (ahead-of-time compilation), resulting in faster startup, lower memory usage, and better battery efficiency compared to V8 or JavaScriptCore. It is enabled by default in React Native.

🟢 **Q5:** What is Expo and when should you use it vs bare React Native?

> Answer: Expo is a framework and toolchain built on top of React Native. It provides a managed workflow with pre-configured native modules, EAS for cloud builds, Expo Router for file-based navigation, and OTA updates. Use Expo for most new apps. Use bare React Native only when you need custom native code that Expo config plugins cannot handle, or when the native module you need is not in the Expo SDK.

🟢 **Q6:** What does `SafeAreaView` do and why is it required?

> Answer: SafeAreaView adds padding to keep content away from the notch, Dynamic Island (iPhone), status bar, and home indicator. Without it, content renders behind these system UI elements. Always use `SafeAreaView` from `react-native-safe-area-context` (not the built-in one — it is deprecated and less accurate).

---

### 2-B Intermediate

🟡 **Q7:** What is the difference between `isLoading` and `isFetching` in TanStack Query?

> Answer: `isLoading` is true only on the very first load when there is no cached data and a request is in-flight. `isFetching` is true any time a request is in-flight, including background refetches when cached data is shown. Use `isLoading` to show a full-page spinner; use `isFetching` to show a subtle refresh indicator.

🟡 **Q8:** Why is `flexDirection: 'column'` the default in React Native but `flex-direction: row` is the default in CSS?

> Answer: React Native chose column as the default because mobile screens are portrait-oriented. Content typically stacks vertically. Web pages are landscape with horizontal content flow as the default. This is the most common layout bug for web developers switching to React Native.

🟡 **Q9:** What is the difference between `useRef` returning a stable mutable container vs `useState`?

> Answer: `useState` triggers a re-render when updated. `useRef` does not. Use `useState` for values the UI needs to display. Use `useRef` for values you need to read/write without triggering re-renders: timer IDs, isMounted flags, previous values, direct references to native views.

🟡 **Q10:** Why must you never put sensitive tokens in `AsyncStorage`?

> Answer: AsyncStorage is unencrypted. On a rooted/jailbroken device, anyone can read the storage file. Sensitive data like auth tokens, API keys, and PII must go in `expo-secure-store` or `react-native-keychain`, which use the iOS Keychain and Android Keystore — hardware-backed encrypted storage.

🟡 **Q11:** What is stale-while-revalidate and how does TanStack Query implement it?

> Answer: Show the user cached (potentially stale) data immediately, then fetch fresh data in the background and update when it arrives. In TanStack Query, `staleTime` controls how long data is considered fresh. After `staleTime` expires, the data is stale. On next access, the stale data is shown immediately while a background request fetches fresh data. The UI updates when fresh data arrives. Zero loading flash.

🟡 **Q12:** What does an Error Boundary catch and what does it NOT catch?

> Answer: Catches: render-time errors in the component tree, errors in class component lifecycle methods, errors in constructors. Does NOT catch: async errors (useEffect, event handlers, setTimeout, fetch). Async errors must be handled with try/catch and stored in state.

---

### 2-C MAANG

🔴 **Q13:** Explain the React Native New Architecture — what are Fabric, TurboModules, and JSI?

> Answer: JSI (JavaScript Interface) is a C++ layer that lets JavaScript directly call native code without the old asynchronous bridge. This enables synchronous communication and shared memory between JS and native. Fabric is the new renderer built on JSI — it manages the native view tree synchronously, enabling direct view manipulation from JS thread and better animation performance. TurboModules are the new way to write native modules on JSI — they are lazy (loaded on demand) and typed. Together they replace the old bridge-based async architecture and enable smoother animations, better Reanimated performance, and faster native module calls.

🔴 **Q14:** Walk through what happens when a React Native app launches — from binary execution to first screen render.

> Answer: Native app binary starts → native runtime initializes (iOS UIKit / Android Activity) → Hermes engine loads and parses the JS bundle (bytecode on Hermes, parsing on JSC) → React root component initializes → React tree renders → Shadow tree is created (Yoga calculates layout) → Native views are created on UI thread → First frame is painted → useEffect hooks run → data fetching begins. The key threads: JS thread (React logic), UI thread (native view updates), and background threads (network, async tasks). Performance problems at startup usually come from large bundle parse time (Hermes AOT solves this) or synchronous work blocking JS thread before first render.

🔴 **Q15:** A user reports that the app shows a black screen in production but the same build works in TestFlight. What is your debugging plan?

> Answer: First, check Sentry for JavaScript errors on the affected user's device and app version — a crash during initialization shows as a black screen. Check if the issue correlates with a specific device model or OS version. If a native crash, pull Xcode device crash logs (symbolicating with the release dSYM). Check for network errors: the app may be making a blocking call before first render. Verify the JS bundle is not corrupted in the OTA update. Check if any conditional code runs differently on user's locale or time zone. Reproduce by testing on the same OS version and device model. The black screen is almost always: uncaught JS error during initialization, native crash, or network blocking first render.

---

## 3. Hooks

🟢 **Q16:** What are the two rules of hooks?

> Answer: 1. Only call hooks at the top level — never inside conditions, loops, or nested functions. 2. Only call hooks from React function components or other custom hooks. These rules protect React's internal hook call-order tracking.

🟢 **Q17:** What is the mutable default argument equivalent for React Native? Show the bug and fix.

> Answer: The equivalent is the stale closure in useEffect with an empty dependency array. Bug: `useEffect(() => { setInterval(() => setCount(count + 1), 1000); }, [])` — count is always 0 in the closure. Fix: `setCount(prev => prev + 1)` uses functional update and does not need count in the closure.

🟡 **Q18:** When should you use `useCallback` and when is it a waste?

> Answer: Useful when: the function is passed as a prop to a `React.memo`-wrapped child (stable reference prevents unnecessary re-renders), or the function is used in a `useEffect` dependency array. Wasteful when: passed to non-memoized components (they re-render anyway), or wrapping trivial one-liners (overhead exceeds benefit). Most premature `useCallback` usage does nothing.

🟡 **Q19:** What is the cleanup function in `useEffect` and what are three examples where it is required?

> Answer: The function returned from useEffect runs before the component unmounts OR before the effect re-runs. Required for: (1) setInterval / setTimeout — call clearInterval/clearTimeout, (2) WebSocket / event subscriptions — call ws.close() / subscription.remove(), (3) network requests — call abortController.abort() to cancel in-flight requests when component unmounts.

🔴 **Q20:** A component has `useEffect(() => { subscribeToSocket(); }, [userId])`. The user navigates rapidly through profile pages. What race condition could occur and how do you fix it?

> Answer: If the user navigates to userId='A', then quickly to userId='B', two subscriptions start: first for A, then for B. If subscription A resolves last, it sets state from A's data while the component is showing B's screen. Fix: use a cancelled flag returned from cleanup. `let cancelled = false; subscribeToSocket(userId, (data) => { if (!cancelled) setState(data); }); return () => { cancelled = true; unsubscribe(); }`. The cleanup from the first effect (A) sets cancelled=true before the second effect (B) runs.

---

## 4. Navigation

🟢 **Q21:** What is the difference between `navigation.navigate()` and `navigation.push()`?

> Answer: `navigate()` goes to an existing screen in the stack if it exists — popping anything on top of it. `push()` always adds a new instance of the screen on top of the stack. Use `push()` when you need the current screen to stay (e.g., ProductDetail → another ProductDetail, or Checkout → ProductDetail without losing Checkout).

🟡 **Q22:** How do you handle the Android hardware back button in React Native?

> Answer: Use `BackHandler.addEventListener('hardwareBackPress', callback)` in useEffect. The callback returns `true` to consume the event (prevent default back) or `false` to allow default back behavior. Always remove the listener in the cleanup function. Use this for modals, drawers, or multi-step flows where back should do something custom.

🟡 **Q23:** What is deep linking and how does it work in React Native?

> Answer: Deep linking is launching the app and navigating to a specific screen from an external source (notification, browser, another app). It uses URL schemes (`myapp://`) or Universal Links / App Links (`https://myapp.com/product/123`). React Navigation's `Linking` configuration maps URLs to screen routes. The app registers URL handlers in Info.plist (iOS) and AndroidManifest.xml (Android). `Linking.getInitialURL()` handles cold starts; the navigation event listener handles foreground opens.

---

## 5. Performance

🟢 **Q24:** What is the difference between the JS thread and the UI thread in React Native?

> Answer: JS thread: runs your React component logic, business logic, and state updates. UI thread (main thread): runs native view updates, gestures, and animations. They are separate. Expensive work on the JS thread does not block scrolling or animations if those run on the UI thread (Reanimated runs on UI thread). Expensive work on JS thread DOES slow state updates and cause janky navigation transitions.

🟡 **Q25:** What is list virtualization and why does FlatList use it?

> Answer: Virtualization renders only the items currently visible on screen (plus a buffer window) instead of all items at once. If a list has 1000 items, rendering all 1000 creates 1000 native views consuming significant memory. FlatList renders maybe 20-30 items (the visible window), recycles the native views as the user scrolls. This keeps memory constant regardless of list length. The trade-off: items leaving the window unmount their component state.

🔴 **Q26:** A FlatList with 500 items scrolls smoothly at first but slows down after 2 minutes of use. What are the possible causes and investigation steps?

> Answer: Possible causes: (1) Memory accumulation — images not being released from memory as rows leave the window; use a better image caching library (react-native-fast-image). (2) State accumulation in the parent — if each row press adds to an array in parent state without cleanup. (3) No `React.memo` on row component — every parent state change re-renders all visible rows. (4) Expensive computation in renderItem not memoized. Investigation: React Native DevTools Profiler to see if JS thread is the bottleneck. Hermes heap snapshot to identify objects growing in memory. Check that row components are memoized. Profile a single renderItem execution time.

---

## 6. Security and Production

🟡 **Q27:** What are the three most important security rules for React Native production apps?

> Answer: (1) Store sensitive data (tokens, PII) in SecureStore/Keychain — never AsyncStorage. (2) Use HTTPS for all network calls — no HTTP exceptions in production. (3) Validate and sanitize deep link parameters before using them — deep links are externally controlled and can be forged.

🟡 **Q28:** What is OTA update and what are its limits?

> Answer: OTA (Over-The-Air) update pushes a new JavaScript bundle to devices without requiring an App Store/Play Store update. Expo EAS Update provides this. It can update: JavaScript logic, component layouts, business rules, API endpoints. It CANNOT update: native code, native modules, app permissions, app icon/splash, or anything in the native binary. Use OTA for bug fixes and UI changes. Use full app store update for native changes.

🔴 **Q29:** Describe a React Native release process from code complete to App Store production.

> Answer: (1) Feature freeze + full regression on physical devices (iOS/Android). (2) Run E2E tests (Detox/Maestro) on CI. (3) Update version number (semver) and build number. (4) Build production binaries: `eas build --platform all --profile production`. (5) Submit to TestFlight + internal Play Store track — test with ~10 internal testers. (6) Submit to external TestFlight + Play Store open testing — broader validation. (7) App Store/Play Store review submission. (8) After approval: staged rollout — 10% → 25% → 50% → 100% over 48-72 hours. (9) Monitor Sentry for crash spikes. (10) If spike detected: halt rollout, push OTA fix if JS-only, or submit emergency binary fix.

---

## 7. Quick Recall — One-line Answers

Fill these in without notes:

1. Default `flexDirection` in React Native: **column**
2. Component that catches render errors: **Error Boundary (class component)**
3. Encrypted storage for tokens on iOS/Android: **SecureStore / Keychain**
4. TanStack Query hook for read operations: **useQuery**
5. TanStack Query hook for write operations: **useMutation**
6. Hook to get safe area padding values: **useSafeAreaInsets**
7. React Native's JS engine: **Hermes**
8. React Native's bundler: **Metro**
9. React Native's layout engine: **Yoga (Flexbox)**
10. How to handle Android back button: **BackHandler.addEventListener**
11. What Error Boundaries do NOT catch: **async errors (useEffect, events, setTimeout)**
12. When to use `push()` vs `navigate()`: **push() when you need current screen to stay in stack**
13. How to prevent setState after unmount: **cancelled flag or AbortController cleanup**
14. What `fetch()` throws on: **network errors only — NOT on 4xx/5xx**
15. `isLoading` vs `isFetching`: **isLoading = first load no cache; isFetching = any active request**
