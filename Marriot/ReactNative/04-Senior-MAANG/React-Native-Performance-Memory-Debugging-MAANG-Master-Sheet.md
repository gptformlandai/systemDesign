# React Native Performance, Memory, And Debugging - MAANG Master Sheet

> Track Module - Group 4: Senior MAANG
> Level: production performance diagnosis and interview depth

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| 60 FPS / 16.67 ms frame budget | Very high | Core mobile performance metric |
| JS FPS vs UI FPS | Very high | Identifies bottleneck class |
| Release-build profiling | Very high | Dev mode lies about performance |
| FlatList performance | Very high | Common real app issue |
| Startup time | High | First impression and retention |
| Memory leaks | High | Crashes, slowdowns, app kills |
| Image performance | High | Heavy mobile resource |
| Bundle size | Medium-high | Startup and download impact |
| Logging overhead | Medium-high | `console.log` can hurt release performance |

MAANG signal:
You diagnose using measurements: FPS, traces, profiles, heap, network waterfall, logs, crash reports, and release builds.

---

## 2. Mental Model

React Native performance has multiple bottleneck zones.

```text
Startup:
  native init + bundle load + JS execution + first render

Interaction:
  touch -> JS work -> render -> commit -> native mount -> frame

Scrolling:
  list virtualization + row render cost + image decode/cache + UI thread drawing

Memory:
  JS objects + native views + images + caches + retained listeners/timers
```

You need to locate the bottleneck before fixing it.

---

## 3. Key Numbers

| Number | Meaning |
|---|---|
| 60 FPS | Standard smoothness target |
| 16.67 ms | Time budget per frame at 60 FPS |
| 100 ms | Human-perceived instant-ish interaction threshold |
| 1 second | Noticeable wait; needs feedback |
| 3 seconds+ | High abandonment risk for startup/loading |

Use approximate ranges in interviews. Exact targets depend on device class, product, and market.

---

## 4. JS FPS vs UI FPS

JS thread bottleneck symptoms:
- touch delay
- JS-driven animation freeze
- delayed state updates
- navigation callbacks delayed
- heavy render or calculation

UI thread bottleneck symptoms:
- native animation/rendering stutter
- expensive shadows/overdraw
- image resizing/compositing issues
- layout/mount pressure

Debug line:

```text
If JS FPS drops but UI FPS is fine, I look for JS render/computation/list work.
If UI FPS drops, I look for native drawing/layout/image/shadow/animation work.
If both drop, I inspect cross-thread pressure and overall screen complexity.
```

---

## 5. Performance Debugging Flow

```text
1. Reproduce on target device class.
2. Confirm release build, not dev mode.
3. Identify symptom: startup, scroll, tap delay, animation, memory, network.
4. Compare JS FPS and UI FPS.
5. Capture profiler/trace/heap/network data.
6. Find the hottest path.
7. Apply smallest fix.
8. Re-measure.
9. Add regression guard if possible.
```

Do not start by sprinkling `useMemo` everywhere.

---

## 6. Slow Screen Checklist

Check:
- Is the screen doing too much before first paint?
- Are API calls waterfalling instead of parallelizing?
- Are large images loaded uncompressed?
- Is data transformation happening inside render?
- Are child components re-rendering unnecessarily?
- Are expensive selectors recalculating?
- Is logging active in release?
- Is a large list using `ScrollView`?
- Are native modules blocking the wrong thread?
- Is the app waiting for non-critical config before rendering?

Pattern:
Render shell first, then progressively load non-critical content.

---

## 7. FlatList Performance Checklist

Use:
- stable `keyExtractor`
- memoized row component
- stable `renderItem`
- `getItemLayout` for fixed rows
- tuned `initialNumToRender`, `windowSize`, `maxToRenderPerBatch`
- image thumbnail sizes
- pagination guards
- `ListEmptyComponent`, `ListFooterComponent`

Avoid:
- inline heavy row functions
- index keys
- row-level network calls
- large base64 images in state
- nested scrolls without care
- expensive date/price formatting during every row render

---

## 8. Startup Performance

Startup stages:

```text
native process start
native dependency init
JS engine init
bundle load
JS module evaluation
root render
initial API/cache work
first interactive screen
```

Improve by:
- reducing bundle size
- deferring non-critical work
- lazy-loading heavy screens
- avoiding large synchronous storage reads at startup
- showing cached shell quickly
- optimizing fonts/images
- avoiding expensive provider initialization

Trap:
Too many root providers can make startup slower if each one does synchronous work.

---

## 9. Memory Leaks

Common leak sources:
- event listeners not removed
- timers not cleared
- subscriptions not cleaned up
- retained closures holding large data
- unbounded caches
- large images retained in memory
- navigation stacks keeping heavy screens alive
- native module references not released

Bad:

```tsx
useEffect(() => {
  const id = setInterval(refresh, 5000);
}, []);
```

Good:

```tsx
useEffect(() => {
  const id = setInterval(refresh, 5000);
  return () => clearInterval(id);
}, []);
```

---

## 10. Image Performance

Problems:
- original image too large
- repeated downloads
- no caching strategy
- resizing via layout instead of requesting correct size
- animating width/height instead of transform
- base64 images in JS memory

Better:
- request thumbnails from backend/CDN
- use proper cache headers
- use progressive placeholders
- compress uploads
- prefer transform animations for scale
- profile memory on image-heavy screens

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Profiling dev builds | Dev mode adds overhead | Test release builds |
| Optimizing without measuring | May fix wrong thing | Find JS/UI/network/memory bottleneck |
| Overusing `useMemo` | Adds complexity | Use where expensive or identity-sensitive |
| `console.log` in production bundle | JS thread overhead and leaks data | Strip/sanitize logs |
| Large images in state | Memory pressure | Use URLs/files/cache |
| Ignoring low-end Android | Users feel worst performance | Test representative devices |

---

## 12. Strong Interview Answer

Question:
How do you debug dropped frames in React Native?

Strong answer:

```text
I first reproduce in a release build on a realistic device. Then I check whether
JS FPS, UI FPS, or both are dropping. If JS FPS drops, I inspect render cost,
list rows, selectors, synchronous work, logging, and API callback bursts. If UI
FPS drops, I inspect native drawing, image sizes, shadows, layout, and animations.
For lists I verify virtualization, stable keys, memoized rows, image thumbnails,
and pagination. I make one change at a time and re-measure so the fix is evidence-based.
```

---

## 13. Revision Notes

- One-line summary: Measure the bottleneck before optimizing.
- Three keywords: JS FPS, UI FPS, release build.
- One interview trap: Dev-mode performance is not production performance.
- One memory trick: Slow RN app equals too much JS, too much UI work, too much memory, or slow network.

---

## 14. Profiling Workflow — Step by Step

### Rule Zero: Always Profile on a Release Build

Dev builds run with:
- Extra error checking and validation
- Remote JS debugging overhead (if enabled)
- No Hermes AOT optimization (using interpreter mode)
- Bundle not minified

Release build profiling is the only number that matters. A component that renders in 8ms in release can take 80ms in dev mode.

```bash
# Build release for Android profiling
cd android && ./gradlew assembleRelease

# Run release build on connected device
npx react-native run-android --mode=release

# iOS release build
npx react-native run-ios --configuration Release
```

---

### Hermes CPU Profiler — Detailed Workflow

```text
Step 1: Enable Hermes
  android/gradle.properties: hermesEnabled=true (default in 0.70+)
  iOS: automatically uses Hermes in 0.70+

Step 2: Start profiling
  Shake device on the simulator/device
  → "Performance" → "Start Sampling Profiler"
  OR in Flipper: Hermes Debugger → Profiler

Step 3: Reproduce the slow interaction
  Navigate to the slow screen
  Trigger the interaction (scroll, open drawer, fetch, etc.)
  Keep interaction under 30 seconds for clean trace

Step 4: Stop and export
  Shake → "Stop Sampling Profiler"
  Downloads a .cpuprofile JSON file

Step 5: Analyze in Chrome
  Open Chrome → DevTools → Performance tab
  Drag .cpuprofile into Chrome DevTools
  Look for: wide bars (expensive functions), hot stack frames
  Key targets: render functions, useEffect callbacks, selector calculations
```

### Reading the Flame Chart

```text
Flame chart interpretation:
  Wide bar = function ran for a long time (expensive)
  Tall stack = deep call chain (many function calls)
  Color grouping: yellow = JavaScript, gray = native/system

Look for:
  - Wide bars in your component's render function → too much work in render
  - Wide bars in map/filter/reduce → expensive derivations not memoized
  - Wide bars in useEffect → heavy async work blocking JS thread
  - Repeated thin bars for same function → called too many times (re-render issue)
```

---

## 15. FlatList Optimization — The Complete Playbook

FlatList is the most common performance bottleneck in production React Native apps. Every senior engineer should know this deeply.

### The Virtualization Mental Model

```text
ScrollView: renders ALL items immediately, keeps them in memory
FlatList: renders only visible items + small overflow (windowSize), recycles others

Without FlatList:
  200 items × 100ms render = 20 seconds to mount
  200 items in memory = likely crash on low-end devices

With FlatList default config:
  Visible items + windowSize buffer rendered
  Off-screen items unmounted (mount cost on scroll)
```

### Every Performance Prop Explained

```tsx
<FlatList
  data={items}
  renderItem={renderItem}
  keyExtractor={item => item.id}          // stable key — must be unique
  
  // Memory and rendering window
  windowSize={5}                           // renders 5 viewport-heights worth of items
                                           // default 21 — reduce for lower memory, increase for less blank on fast scroll
  maxToRenderPerBatch={10}                 // items rendered per JS batch
                                           // reduce to unblock JS thread faster, increase for smoother initial load
  initialNumToRender={10}                  // items rendered on first paint
                                           // should fill visible area — not more
  updateCellsBatchingPeriod={50}           // milliseconds between batch renders
  
  // Layout optimization (critical for fixed-height rows)
  getItemLayout={(data, index) => ({
    length: ITEM_HEIGHT,                   // fixed row height
    offset: ITEM_HEIGHT * index,           // row's position in list
    index,
  })}
  // Without getItemLayout: Yoga must measure each row — expensive
  // With getItemLayout: positions are pre-calculated — scrollToIndex works instantly

  // Viewport maintenance
  removeClippedSubviews={true}            // detach off-screen views from window hierarchy
                                           // reduces GPU overdraw — useful for long lists
                                           // can cause blank flashes on Android — test carefully

  // Interaction priority
  disableVirtualization={false}           // never set to true in production
/>
```

### Row Component Optimization

```tsx
// BAD — new function reference every render → React.memo useless
const BadList = () => {
  const [cart, setCart] = useState([]);
  return (
    <FlatList
      renderItem={({item}) => (  // inline arrow function — new ref every parent render
        <ProductRow item={item} onAdd={() => setCart(c => [...c, item])} />
      )}
    />
  );
};

// GOOD — stable references with useCallback
const GoodList = () => {
  const [cart, setCart] = useState([]);
  
  const handleAdd = useCallback((item: Product) => {
    setCart(c => [...c, item]);
  }, []);  // empty deps — setCart is stable
  
  const renderItem = useCallback(({item}: {item: Product}) => (
    <ProductRow item={item} onAdd={handleAdd} />
  ), [handleAdd]);  // handleAdd is stable → renderItem is stable
  
  return <FlatList renderItem={renderItem} />;
};

// ProductRow uses React.memo — only re-renders when item or onAdd change
const ProductRow = React.memo(({item, onAdd}: {item: Product; onAdd: (item: Product) => void}) => {
  return (
    <Pressable onPress={() => onAdd(item)}>
      <Text>{item.name}</Text>
    </Pressable>
  );
});
```

### Image Optimization in Lists

```tsx
// Large unoptimized images crash list performance
// BAD: loading full-resolution images in thumbnails
<Image source={{uri: product.imageUrl}} style={{width: 80, height: 80}} />
// Decodes a 4K image into an 80×80 box — wastes memory and CPU

// GOOD: request thumbnail-sized images from your CDN
const thumbnailUrl = `${product.imageUrl}?w=160&h=160&fit=crop`;
// or use a CDN transform like Cloudinary/imgix with proper sizing

// BETTER: use FastImage (community library) for:
// - Disk caching (images load instantly on second scroll)
// - HTTP cache headers respected
// - Priority loading (visible items > off-screen)
import FastImage from 'react-native-fast-image';
<FastImage
  source={{uri: thumbnailUrl, priority: FastImage.priority.normal}}
  style={{width: 80, height: 80}}
  resizeMode={FastImage.resizeMode.cover}
/>
```

---

## 16. Memory Leak Patterns and Fixes

### The Three Most Common Leak Patterns

**Pattern 1: Missing useEffect Cleanup**

```tsx
// LEAK: WebSocket not closed when component unmounts
function ChatScreen() {
  const [messages, setMessages] = useState([]);
  
  useEffect(() => {
    const ws = new WebSocket('wss://api.example.com/chat');
    ws.onmessage = (event) => {
      setMessages(m => [...m, JSON.parse(event.data)]);
      // If component unmounts, ws still fires, trying to setMessages on unmounted component
    };
    // NO cleanup! ws.close() never called
  }, []);
}

// FIX: return cleanup function
useEffect(() => {
  const ws = new WebSocket('wss://api.example.com/chat');
  let mounted = true;
  
  ws.onmessage = (event) => {
    if (!mounted) return;  // guard against updates after unmount
    setMessages(m => [...m, JSON.parse(event.data)]);
  };
  
  return () => {
    mounted = false;
    ws.close();
  };
}, []);
```

**Pattern 2: Timer Accumulation**

```tsx
// LEAK: setInterval not cleared — adds a new interval every render
function PollComponent({pollInterval}: {pollInterval: number}) {
  const [data, setData] = useState(null);
  
  useEffect(() => {
    const id = setInterval(fetchData, pollInterval);
    // Missing: return () => clearInterval(id);
    // If pollInterval changes → new interval added, old never removed
    // After 10 navigation cycles → 10 intervals polling simultaneously
  }, [pollInterval]);  // runs again when pollInterval changes
}

// FIX
useEffect(() => {
  const id = setInterval(fetchData, pollInterval);
  return () => clearInterval(id);
}, [pollInterval]);
```

**Pattern 3: EventEmitter Subscription Accumulation**

```tsx
// LEAK: AppState listener never removed
function NetworkAwareComponent() {
  useEffect(() => {
    const sub = AppState.addEventListener('change', handleAppStateChange);
    // No cleanup — each mount adds another listener
    // After screen focus/blur 10 times → 10 handlers firing
  }, []);
  
  // FIX
  useEffect(() => {
    const sub = AppState.addEventListener('change', handleAppStateChange);
    return () => sub.remove();  // EventEmitter subscriptions have .remove()
  }, []);
}
```

### Detecting Memory Leaks

```text
Method 1: Hermes Heap Snapshot
  React Native DevTools → Memory → Take Heap Snapshot
  Navigate to the screen → interact → navigate back
  Take another snapshot
  Compare: are objects from the screen being retained?
  Look for: closures, arrays, component instances that should be GC'd

Method 2: Flipper Memory Plugin
  Flipper → Memory → Record allocation timeline
  Reproduce the suspected leak
  Look for: growing JS heap that never drops after navigation

Method 3: Manual Pattern Check
  Code review checklist:
  □ Every useEffect with side effects has a cleanup return
  □ Every setInterval/setTimeout has clearInterval/clearTimeout in cleanup
  □ Every EventEmitter.addEventListener has .remove() in cleanup
  □ Every WebSocket/EventSource is closed in cleanup
  □ Every external store subscription is unsubscribed in cleanup
```

---

## 17. App Startup Time Optimization

### Startup Time Breakdown

```text
Total startup = sum of these phases:
  1. Native init:      app binary load, native modules init            (50-300ms)
  2. JS engine start:  Hermes load, bytecode parse                     (50-200ms)
  3. Bundle load:      read .jsbundle from disk                        (50-500ms)
  4. JS execution:     module evaluation, dependency graph init        (100-500ms)
  5. First render:     component tree creation + navigation init       (50-200ms)
  6. Hydration:        AsyncStorage reads, auth check, initial API     (50-500ms+)

Target:
  Low-end Android: < 2 seconds to interactive (time-to-interactive)
  Mid-range: < 1 second
  High-end: < 500ms
```

### Optimization Strategies

```tsx
// 1. Lazy-load non-critical screens
import React, {lazy, Suspense} from 'react';

// Instead of: import ProfileScreen from './ProfileScreen';
// These execute immediately when the module is evaluated

const ProfileScreen = lazy(() => import('./ProfileScreen'));  
// Module loads only when ProfileScreen is first rendered

// 2. Defer non-critical module initialization
// BAD: import moment from 'moment';  — 329KB parsed on startup
// GOOD: 
const formatDate = (date: Date) => {
  // Dynamic import — module loads only when this function is first called
  return new Intl.DateTimeFormat('en-US').format(date);  // use built-in instead
};

// 3. Defer non-critical useEffect work
function AppRoot() {
  useEffect(() => {
    // Don't do this at startup — it runs before first render is visible
    initAnalytics();
    loadPersistedState();
    prefetchUserProfile();
  }, []);
  
  // Better — let first frame render, then do non-critical initialization
  useEffect(() => {
    const timer = setTimeout(() => {
      initAnalytics();
      loadPersistedState();
    }, 0);  // yield to the event loop — first render happens first
    return () => clearTimeout(timer);
  }, []);
}

// 4. Cache navigation state
// React Navigation can restore its state from AsyncStorage
// Avoids re-running navigation logic on every launch
const navState = await AsyncStorage.getItem('NAV_STATE');
const initialState = navState ? JSON.parse(navState) : undefined;
<NavigationContainer initialState={initialState} onStateChange={saveState}>
```

---

## 18. Bundle Size Optimization

```bash
# Analyze bundle composition
npx react-native bundle --dev false --platform android \
  --bundle-output /tmp/main.bundle \
  --assets-dest /tmp/assets

# Use source-map-explorer to visualize
npx source-map-explorer /tmp/main.bundle

# Common large dependencies to audit:
# moment.js: 329KB (use date-fns or Intl.DateTimeFormat instead)
# lodash: 70KB full (use lodash-es with tree shaking, or native methods)
# @expo/vector-icons: 4MB (use react-native-svg + specific icon files)
```

```tsx
// Tree shaking — import only what you use
// BAD: imports entire lodash
import _ from 'lodash';
_.debounce(fn, 300);

// GOOD: imports only debounce
import debounce from 'lodash/debounce';
debounce(fn, 300);

// Better: write it yourself (90 characters vs 70KB)
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(handler);
  }, [value, delay]);
  return debouncedValue;
}
```

---

## 19. The MAANG Performance Interview Answer Framework

When asked any React Native performance question, structure your answer in 5 parts:

```text
1. MEASURE first: "Before I optimize, I would profile on a release build on a realistic
   device. DevMode numbers are misleading. I'd use the Hermes Profiler and check
   both JS FPS and UI FPS to identify which thread is the bottleneck."

2. IDENTIFY the class of problem:
   "If JS FPS is dropping, the problem is in JS work: renders, selectors, API callbacks,
    logging, or synchronous heavy computation.
   If UI FPS is dropping, it's native: shadows, overdraw, image compositing, layout thrash."

3. APPLY targeted fixes based on evidence:
   "For lists: stable keys, React.memo on rows, getItemLayout, useCallback for renderItem,
    image thumbnails not full resolution, reduce windowSize if memory is the issue.
   For re-renders: check that useMemo/useCallback dependencies are correct, move state down
    to leaf components, consider splitting contexts."

4. VERIFY: "I'd re-measure after one change at a time to confirm the improvement is real."

5. CAVEAT: "New Architecture and Hermes handle the infrastructure, but application-level
   issues like missing memoization or large lists still require explicit optimization."
```

