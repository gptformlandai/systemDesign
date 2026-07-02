# React Native Production Debugging Case Studies — Gold Sheet

> Track Module - Group 5: Scenario Practice
> Level: senior / MAANG | Mode: think like an on-call engineer, not a feature developer

---

## 1. How to Use This Sheet

Each case study follows a narrative arc:
1. Incident: what the symptom is
2. Investigation: how you narrow it down
3. Root cause: the actual bug
4. Fix: the code change
5. Prevention: what you add to prevent recurrence
6. Interview answer: the 60-second spoken version

Read each case, then close the sheet and reconstruct the debugging flow from memory.

---

## 2. Case Study 1: App Freezes on Slow Devices During List Scroll

**Incident**:
QA reports the product list screen freezes for 2-3 seconds on older Android devices when scrolling fast. The issue does not appear on newer iPhones.

**Investigation**:

Step 1 — Reproduce on the same device class.
```text
Old Android device (2018, 2GB RAM). iOS 15 Pro: no issue.
Conclusion: not a universal bug — device-specific.
```

Step 2 — Profile using React Native DevTools Profiler.
```text
Open DevTools → Profiler → record while scrolling.
Observation: JS thread frame rate drops to 8 FPS during scroll.
UI thread is fine (native scroll is smooth).
JS thread is doing something expensive.
```

Step 3 — Find the expensive JS work during scroll.
```text
The renderItem function calls getProductRecommendations(item.id) synchronously
on every render of each ProductRow.
getProductRecommendations does an O(n²) search through 500 items.
On a fast device this is fast enough to miss. On a slow device: ~50ms per row.
```

```tsx
// Bug: expensive computation in render
const ProductRow = React.memo(({item}: {item: Product}) => {
  // THIS runs on every render of this row
  const recommendations = getProductRecommendations(item.id); // O(n²) — 50ms on slow device
  return <View>...</View>;
});
```

**Root Cause**: Synchronous expensive computation inside `renderItem` blocking the JS thread during scroll.

**Fix**:

```tsx
// Option A: Memoize the expensive computation
const ProductRow = React.memo(({item}: {item: Product}) => {
  const recommendations = useMemo(
    () => getProductRecommendations(item.id),
    [item.id], // only recompute when item ID changes
  );
  return <View>...</View>;
});

// Option B (better): Pre-compute before rendering using selector
// Compute once in the parent, pass as prop
const ProductList = () => {
  const {products} = useProducts();
  const recommendationMap = useMemo(
    () => Object.fromEntries(products.map(p => [p.id, getProductRecommendations(p.id)])),
    [products],
  );

  return (
    <FlatList
      data={products}
      renderItem={({item}) => (
        <ProductRow item={item} recommendations={recommendationMap[item.id] ?? []} />
      )}
    />
  );
};
```

**Prevention**:
- Add React Native Perf Monitor to staging builds (FPS overlay)
- Set up Flipper Performance plugin for CI profiling
- Code review checklist: no expensive sync operations in renderItem

**Interview answer**:
```text
We had a freeze on slow Android devices during list scroll. I profiled it with
React Native DevTools and found the JS thread dropping to 8 FPS while the
UI thread was fine. Profiler showed expensive work in renderItem — an O(n²)
recommendation calculation running synchronously on every row render.
On fast devices it was invisible; on 2GB RAM Android it was 50ms per row.
The fix was to pre-compute the recommendation map outside FlatList using
useMemo, keyed on the product list. After the fix, JS thread stayed at 60 FPS
on the same slow device. We added a performance regression test that asserts
renderItem never calls functions above O(n).
```

---

## 3. Case Study 2: Memory Leak — App Crashes After 10 Minutes

**Incident**:
A chat screen crashes with an OOM (out of memory) error on iOS after about 10 minutes of use. Memory grows steadily in Instruments.

**Investigation**:

Step 1 — Open Xcode Instruments → Leaks tool during 10-minute session.
```text
Memory grows 3MB per minute. Leaks shows WebSocket event listener objects
accumulating — not being released.
```

Step 2 — Look at the WebSocket subscription code.
```tsx
// Bug: no cleanup — event listener accumulates on every re-render
function useChatSocket(conversationId: string) {
  const [messages, setMessages] = useState<Message[]>([]);

  useEffect(() => {
    const ws = new WebSocket(`wss://api.example.com/chat/${conversationId}`);
    ws.onmessage = (e) => {
      setMessages(prev => [JSON.parse(e.data), ...prev]);
    };
    // NO return cleanup function — ws is never closed
  }, [conversationId]); // re-runs when conversationId changes

  return messages;
}
```

Step 3 — Confirm by adding logging.
```text
Added console.log on WebSocket creation and close.
On each navigation into ChatScreen, a new WebSocket opens.
On navigation out, the previous one is never closed.
10 minutes = multiple chat conversations = 8 open WebSockets all receiving events
and calling setState on unmounted components.
```

**Root Cause**: Missing `useEffect` cleanup — WebSocket connections accumulate and never close.

**Fix**:

```tsx
function useChatSocket(conversationId: string) {
  const [messages, setMessages] = useState<Message[]>([]);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    const ws = new WebSocket(`wss://api.example.com/chat/${conversationId}`);

    ws.onmessage = (e) => {
      if (mountedRef.current) {
        setMessages(prev => [JSON.parse(e.data), ...prev]);
      }
    };

    // CRITICAL: cleanup closes the WebSocket when component unmounts
    // or when conversationId changes
    return () => {
      mountedRef.current = false;
      ws.close();
    };
  }, [conversationId]);

  return messages;
}
```

**Prevention**:
- Mandate useEffect cleanup review in PR checklist
- Add ESLint rule: `react-hooks/exhaustive-deps` (catches missing deps/cleanup)
- Instruments memory profiling step in release candidate testing

**Interview answer**:
```text
Our chat screen had a memory leak causing OOM crashes after 10 minutes.
Xcode Instruments showed WebSocket objects accumulating without release.
The bug was a missing return cleanup function in useEffect — every time the user
opened a conversation, a new WebSocket opened. On navigation away, the socket was
never closed. After 8 chat sessions, 8 sockets were receiving messages and trying
to setState on unmounted components. The fix was adding the return cleanup:
ws.close() and a mountedRef to guard setState after unmount. After the fix,
memory was flat for a 30-minute session. We added a linting rule that warns on
useEffect blocks with subscriptions and no cleanup, and added a memory profiling
step to our release checklist.
```

---

## 4. Case Study 3: Network Requests Fail in Production But Not Development

**Incident**:
API calls to `api.example.com` work fine on development builds but fail silently in TestFlight and production builds on iOS.

**Investigation**:

Step 1 — Check the error in production logs (Sentry).
```text
Error: Network request failed
URL: http://api.example.com/products   ← NOTE: http, not https
```

Step 2 — Check iOS App Transport Security (ATS).
```text
iOS 9+ enforces HTTPS for all network requests by default.
Development builds often have NSAllowsArbitraryLoads = true in Debug Info.plist.
Production build does not have this exception.
HTTP requests to api.example.com are blocked by ATS in production.
```

Step 3 — Verify in the network layer code.
```tsx
// Bug: hardcoded http in API base URL
const BASE_URL = 'http://api.example.com'; // development works with NSAllowsArbitraryLoads
```

**Root Cause**: API base URL using HTTP instead of HTTPS. Development builds had ATS disabled; production did not.

**Fix**:

```tsx
// Fix 1: Use HTTPS everywhere
const BASE_URL = 'https://api.example.com';

// Fix 2: Make environment-specific and fail fast in development if wrong protocol
const BASE_URL = process.env.API_BASE_URL;
if (!BASE_URL?.startsWith('https://') && !__DEV__) {
  throw new Error('API_BASE_URL must use HTTPS in production');
}
```

**Prevention**:
- Always use HTTPS — no HTTP exceptions in production
- Add URL validation in CI: `grep -r 'http://' src/api` fails build if found
- Enable NSAllowsArbitraryLoads only explicitly for local dev, never committed

**Interview answer**:
```text
Production API calls were failing silently while development worked fine.
Sentry logs showed 'Network request failed' on an http:// URL. The cause was
iOS App Transport Security — iOS blocks non-HTTPS requests by default. Our dev
Info.plist had NSAllowsArbitraryLoads disabled for testing but production builds
didn't have the exception. The API base URL was hardcoded with http instead of https.
The fix was changing to https and adding a startup assertion that throws if
API_BASE_URL doesn't use HTTPS in non-dev builds. We also added a CI step that
greps source files for http:// API URLs and fails the build if any are found.
```

---

## 5. Case Study 4: State Loss on Navigation — Form Clears Unexpectedly

**Incident**:
Users report that their checkout form (shipping address) resets when they navigate to a product detail page and come back.

**Investigation**:

Step 1 — Reproduce the navigation flow.
```text
CheckoutScreen → navigate to ProductDetail (from a recommendation) → back
Observation: CheckoutScreen remounts — form state is gone.
```

Step 2 — Check the navigation action being used.
```tsx
// Bug: using navigate() instead of understanding the stack
navigation.navigate('ProductDetail', {productId}); 
// When ProductDetail is already on the stack, navigate() pops back to it
// rather than pushing a new screen — this pops CheckoutScreen off the stack
```

Step 3 — Check React Navigation stack configuration.
```text
The navigator is a Stack and ProductDetail is below Checkout in some flows.
navigate('ProductDetail') goes BACK to the existing ProductDetail instance,
popping Checkout off the stack. CheckoutScreen unmounts → useState resets.
```

**Root Cause**: `navigation.navigate()` can pop the current screen when the destination already exists in the stack. Local `useState` is lost on unmount.

**Fix**:

```tsx
// Fix 1: Use navigation.push() to always add a new screen instance
navigation.push('ProductDetail', {productId}); // always pushes, never pops

// Fix 2 (better for important form state): persist form state to survive navigation
// Use Zustand, Redux, or AsyncStorage
const useCheckoutStore = create<CheckoutState>(set => ({
  shippingAddress: null,
  setShippingAddress: (address) => set({shippingAddress: address}),
}));

// Form reads from and writes to the store — survives full unmount
function CheckoutForm() {
  const {shippingAddress, setShippingAddress} = useCheckoutStore();
  // Even if screen unmounts, state is in Zustand — persists across navigation
}
```

**Prevention**:
- Use Zustand/Redux for any multi-step form or wizard state that spans navigation
- Document when to use `push()` vs `navigate()` in team navigation conventions

**Interview answer**:
```text
Checkout form state was being lost when users navigated to a product detail screen and back.
The root cause was two things: the navigation action popped CheckoutScreen off the stack because
ProductDetail was already in the navigation history — navigate() goes to an existing screen
instance, which pops everything on top. And the form state was in local useState, so unmounting
the screen wiped it.
The fix was twofold: use navigation.push() for recommendation links from checkout so Checkout
stays mounted, AND move form state to Zustand so it survives even a full unmount.
We documented a navigation convention: use push() when you need to guarantee the current screen
stays in the stack. We added a Zustand store for multi-step checkout state.
```

---

## 6. Case Study 5: App Takes 7 Seconds to Show First Screen

**Incident**:
App launch to first meaningful paint takes 7 seconds on mid-range Android. Competitor apps launch in 1-2 seconds.

**Investigation**:

Step 1 — Profile with Android Profiler + React Native Startup Profiler.
```text
Startup phases:
  Native init: 200ms (acceptable)
  JS bundle load (Hermes): 2100ms ← slow
  JS execution: 1800ms ← slow
  First render: 900ms
  Data fetch for home screen: 2000ms ← slow
Total: ~7000ms
```

Step 2 — Analyze bundle size.
```bash
npx react-native bundle --platform android --dev false --entry-file index.js --bundle-output bundle.js
wc -c bundle.js
# 8.4 MB — very large
```

Step 3 — Identify bundle bloat.
```bash
npx react-native-bundle-visualizer
# Large modules: moment.js (2.1 MB), lodash (1.4 MB), 3 icon libraries (800 KB each)
```

Step 4 — Analyze JS execution time.
```text
App.tsx synchronously imports 40 screens at the top level.
All screens initialize their data fetching logic on import — even screens user hasn't visited.
```

**Root Cause**: Oversized JS bundle + synchronous initialization of all screens at startup.

**Fix**:

```tsx
// Fix 1: Replace moment.js with date-fns (tree-shakeable) or dayjs
// moment.js is 2.1MB; dayjs is 2KB
import dayjs from 'dayjs';

// Fix 2: Replace full lodash with individual lodash functions
import debounce from 'lodash/debounce'; // 2KB vs 1.4MB

// Fix 3: Use a single icon library with SVG instead of three image-based ones
import {Icon} from '@expo/vector-icons'; // one icon set

// Fix 4: Lazy-load non-critical screens
const ProductDetailScreen = React.lazy(() => import('./screens/ProductDetail'));
// Or with React Navigation:
const Stack = createNativeStackNavigator();
// Screens are already lazy by default in React Navigation

// Fix 5: Prefetch home screen data before JS bundle finishes loading
// In native splash screen layer, start the API call as early as possible
// Expo SplashScreen.preventAutoHideAsync() while data loads

// Fix 6: Use Hermes inline requires (enabled by default in RN 0.71+)
// babel.config.js
module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [['react-native-reanimated/plugin'], ['babel-plugin-transform-inline-environment-variables']],
};
```

**Result**: After removing moment/lodash bloat, bundle drops from 8.4MB to 3.1MB. Startup time: 7 seconds → 2.4 seconds.

**Prevention**:
- Bundle size budget: CI fails if bundle exceeds 5MB
- Import cost linting: import-cost VS Code plugin
- Startup time threshold: Detox/Maestro E2E test asserting first screen visible within 3 seconds

**Interview answer**:
```text
Our app took 7 seconds to launch on mid-range Android. I profiled with Android Studio Profiler
and found three problems: bundle was 8.4MB because we had moment.js, full lodash, and three
separate icon libraries all bundled; JS execution was initializing all 40 screens at startup;
and the home screen data fetch was not starting until after the bundle parsed.
The fix: replaced moment with dayjs (2KB), replaced lodash with individual functions, consolidated
to one icon library. Bundle dropped from 8.4MB to 3.1MB. We also added React.lazy for non-critical
screens and overlapped data fetching with bundle loading using SplashScreen. Startup time went
from 7 seconds to 2.4 seconds. We added a CI bundle size gate (fail if > 5MB) and a Maestro E2E
test that asserts the home screen is visible within 3 seconds on a mid-range device.
```

---

## 7. Common Debugging Patterns Reference

```text
Symptom                          → First Tool To Use
---------------------------------------------------------------------
JS thread slow (jank)            → React Native DevTools Profiler
Memory leak                      → Xcode Instruments (iOS) / Android Profiler
Network failures in production   → Sentry + check ATS / network security config
State unexpectedly lost          → Check navigation action type + component lifecycle
Slow startup                     → React Native bundle visualizer + Android profiler
Crash only in release build      → React Native DevTools + Sentry symbolication
Notifications not arriving       → Check push token + APN/FCM dashboard
WebSocket drops                  → Add reconnect + check app state foreground/background
Image flickering                 → Check key prop + check Image caching config
FlatList blank rows              → Check getItemLayout miscalculation
```

---

## 8. Revision Notes

- Always profile before optimizing — measure first, then fix the right thing
- Memory leaks in React Native: missing useEffect cleanup is the #1 cause
- ATS on iOS blocks HTTP in production builds — always use HTTPS
- `navigation.navigate()` can pop screens — use `push()` when forward navigation is intended
- Form state across navigation belongs in Zustand/Redux, not local useState
- Bundle bloat: moment.js, full lodash, multiple icon libraries are the most common culprits
- Startup optimization: bundle size + lazy imports + overlapping data fetch with bundle load
- Production debugging requires: Sentry for JS errors, Xcode Instruments for memory, React Native DevTools for render performance
- The interview answer pattern: symptom → investigation tool → root cause → code fix → prevention mechanism
