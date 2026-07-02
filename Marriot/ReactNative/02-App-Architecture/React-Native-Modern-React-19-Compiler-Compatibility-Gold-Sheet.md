# React Native Modern React 19 And Compiler Compatibility - Gold Sheet

> Track Module - Group 2: App Architecture
> Level: modern React features applied safely to React Native

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| React vs React Native boundary | Very high | Not every React/web feature maps to mobile |
| React 19 compatibility | High | Modern projects need current React judgment |
| React Compiler | Medium-high | Changes memoization strategy when enabled |
| Error Boundary reality | Very high | No stable hook replacement |
| Suspense/async features | Medium-high | Useful, but framework support matters |
| Migration safety | High | RN upgrades move React, Hermes, Metro, native code together |

MAANG signal:
You can use modern React without assuming browser, Next.js, or server-rendering behavior exists in React Native.

---

## 2. Mental Model

React Native uses React for component semantics, but React Native owns the host environment.

```text
React:
  components, hooks, reconciliation, Suspense semantics, memoization model

React Native:
  host components, native renderer, Metro, Hermes, native modules,
  app lifecycle, device APIs, platform UI

Web/Next.js:
  DOM, browser APIs, server components, forms/actions, web routing
```

Rule:
Ask whether a feature belongs to React core, the browser DOM, a web framework, or React Native's renderer.

---

## 3. What Modern React Changes

React 19-era thinking affects React Native mostly through:
- concurrent rendering semantics
- Suspense behavior where supported
- `use` for supported async/context patterns
- improved ref handling patterns
- React Compiler ecosystem work
- stricter expectations around pure rendering

What does not automatically transfer:
- DOM form actions
- browser APIs
- Next.js server components
- web-specific routing/data APIs
- document/window/localStorage assumptions

---

## 4. Error Boundaries Reality

Production Error Boundaries are still class components.

```tsx
class ErrorBoundary extends React.Component<
  {children: React.ReactNode},
  {hasError: boolean}
> {
  state = {hasError: false};

  static getDerivedStateFromError() {
    return {hasError: true};
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    reportError(error, info);
  }

  render() {
    if (this.state.hasError) return <FallbackScreen />;
    return this.props.children;
  }
}
```

Interview answer:
There is no stable hook replacement for Error Boundaries. Async errors still need explicit try/catch, error state, promise rejection handling, or global reporting.

---

## 5. React Compiler Mental Model

React Compiler is an optimizing compiler for React code. Its goal is to reduce unnecessary re-renders by automatically applying memoization-like optimizations when code follows React's rules.

What it rewards:
- pure render logic
- stable component boundaries
- no mutation during render
- predictable hooks
- no hidden side effects

What it does not replace:
- state architecture
- list virtualization
- native performance profiling
- expensive image/video handling
- JS thread vs UI thread understanding
- release-build measurement

Senior rule:
Even with compiler support, measure performance on real devices and release builds.

---

## 6. Manual Memoization Strategy

Before compiler:

```tsx
const Row = React.memo(function Row({item, onPress}: Props) {
  return <Pressable onPress={() => onPress(item.id)} />;
});

const handlePress = useCallback((id: string) => {
  navigation.navigate('Details', {id});
}, [navigation]);
```

With compiler enabled and verified:
- remove only memoization that the compiler safely makes redundant
- keep semantic memoization where identity matters for third-party APIs
- keep list item optimization when profiling proves it matters
- do not blindly delete `useMemo`/`useCallback` across a mobile app

Migration approach:

```text
1. Enable on a small surface.
2. Run typecheck/lint/tests.
3. Test key flows on release builds.
4. Compare render/performance telemetry.
5. Expand only after confidence.
```

---

## 7. Suspense In React Native

Suspense can be useful for:
- lazy component boundaries
- data libraries that explicitly support Suspense
- app shell loading fallbacks
- deferring non-critical UI

Be cautious with:
- navigation transitions
- offline-first data
- permission prompts
- forms with unsaved state
- release-build behavior not tested on device

Do not convert every loading state to Suspense. Mobile UX often needs explicit cached, offline, retrying, and partial-content states.

---

## 8. Feature Compatibility Checklist

Before using a modern React feature in RN:

```text
[ ] Is this React core or web/framework-specific?
[ ] Does the installed React Native version support it?
[ ] Does Metro/Babel/tooling support it?
[ ] Does the target library support it in React Native?
[ ] Does it work on Hermes?
[ ] Does it work in release builds?
[ ] Does it preserve offline/loading/error UX?
[ ] Does it interact safely with navigation?
```

---

## 9. Upgrade Risk

React Native upgrades can involve:
- React version
- React Native renderer
- Hermes version
- Metro behavior
- native build tooling
- Gradle/CocoaPods changes
- third-party package compatibility

Upgrade policy:
- read RN release notes
- use upgrade helper/diff tooling where appropriate
- upgrade one minor at a time when possible
- run native builds
- run E2E smoke
- compare startup/performance/crash telemetry
- test OTA runtime boundaries

---

## 10. Strong Interview Answer

```text
I treat modern React features in React Native by first separating React core from
web or framework APIs. React Native gets React component semantics, but not the
DOM or Next.js runtime. Error Boundaries still use class components. React
Compiler can reduce manual memoization when the project tooling supports it, but
it does not replace profiling, list virtualization, or JS/UI thread knowledge. I
would enable modern features incrementally, test on Hermes release builds, and
watch crash and performance telemetry before broad rollout.
```

---

## 11. Revision Notes

- One-line summary: Modern React helps RN, but host-environment compatibility decides what is safe.
- Three keywords: host boundary, compiler, release test.
- One interview trap: Assuming Next.js or DOM features exist in React Native.
- Memory trick: React core travels; browser APIs do not.
