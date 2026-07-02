# React Native New Architecture, Fabric, TurboModules, And JSI - Gold Sheet

> Track Module - Group 3: Native Device And Internals
> Level: senior internals and interview architecture vocabulary

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Old bridge vs New Architecture | High | Common senior interview topic |
| JSI | High | Explains modern JS-native calls |
| Fabric | High | New renderer |
| TurboModules | High | Modern native module system |
| Codegen | Medium-high | Type-safe native boundary |
| Render/commit/mount pipeline | High | Explains UI updates |
| Threading model | High | Performance and correctness |
| Hermes | Medium-high | Runtime and startup discussion |

MAANG signal:
You can explain internals without pretending every app developer writes native modules daily.

---

## 2. Mental Model

Classic simplified model:

```text
JS thread -> serialized bridge messages -> native side
```

Modern simplified model:

```text
JS runtime + JSI
  -> direct interface to C++/native capabilities
  -> Fabric renderer for UI
  -> TurboModules for native modules
  -> Codegen for typed contracts
```

The main idea:
React Native moved from a mostly asynchronous serialized bridge model toward a more direct, typed, synchronous-capable architecture.

---

## 3. Key Terms

| Term | Meaning |
|---|---|
| JSI | JavaScript Interface, a C++ API that lets JS runtimes interact with native/C++ objects |
| Fabric | React Native's newer rendering system |
| TurboModule | New native module system with lazy loading and typed interfaces |
| Codegen | Generates native binding code from typed JS/TS specs |
| Shadow Tree | Cross-platform tree representing layout/UI before host views |
| Host View | Actual platform view, such as iOS `UIView` or Android `View` |
| Hermes | JavaScript engine optimized for React Native |

---

## 4. Render Pipeline

React Native's renderer can be understood in three phases:

```text
1. Render
   React executes components and creates React elements.
   The renderer creates shadow nodes.

2. Commit
   The new shadow tree is committed.
   Layout is calculated, often using Yoga.

3. Mount
   The committed shadow tree is turned into host view mutations.
   Native views are created/updated/deleted on the UI thread.
```

Interview answer:

```text
React components do not directly draw pixels. They produce a React tree, which
React Native translates into a shadow tree. After layout and commit, React Native
mounts mutations to real platform views. This distinction matters for performance
because heavy JS work, layout work, and UI-thread work can all become bottlenecks.
```

---

## 5. Threads

Useful mental model:

```text
JS thread:
  React render logic, business logic, many event handlers, API callbacks.

UI/main thread:
  Native UI mounting, touch processing, native animations, platform rendering.

Background/native threads:
  Layout, image decoding, networking, native module work depending implementation.
```

Performance implication:
- If JS thread is blocked, touch handlers, JS animations, and renders may lag.
- If UI thread is blocked, native rendering/gestures may stutter.
- Native-driven animations can remain smooth even when JS is busy.

---

## 6. Old Bridge vs New Architecture

| Area | Old Bridge Model | New Architecture Direction |
|---|---|---|
| Communication | Serialized async messages | JSI-based direct interfaces |
| Native modules | Eager-ish bridge modules | TurboModules, lazy, typed |
| UI renderer | Legacy renderer | Fabric |
| Type safety | Mostly manual | Codegen from specs |
| Sync calls | Limited/awkward | More possible with JSI |
| Performance | Bridge serialization overhead | Lower overhead in many paths |

Caution:
Do not oversell. New Architecture improves foundations, but bad app code can still block the JS thread, over-render lists, or leak memory.

---

## 7. TurboModule Conceptual Spec

```ts
import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {
  getDeviceRiskScore(userId: string): Promise<number>;
  getInstallSource(): string;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RiskDeviceModule');
```

What this represents:
- JavaScript declares a typed native module contract.
- Codegen can generate native binding scaffolding.
- Native implementation fulfills the contract.

Interview point:
Most app developers consume libraries that hide this. Senior engineers should understand it when debugging native dependencies or building platform modules.

---

## 8. Native Module Design Judgment

Build a native module when:
- OS capability has no good JS-only equivalent.
- Performance requires native/C++ work.
- A vendor SDK is native-only.
- You need platform integration, background service, Bluetooth, NFC, etc.

Avoid a native module when:
- JavaScript can do it cheaply.
- A maintained community/Expo module exists.
- The native API would create unnecessary app-store/release complexity.

Production checklist:
- typed interface
- platform parity or documented platform gaps
- permission handling
- thread safety
- cancellation where relevant
- error mapping
- test plan
- upgrade ownership

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Saying New Architecture removes all performance issues | App code can still be slow | Profile real bottlenecks |
| Treating JSI as a normal JS API | It is native runtime integration | Explain it as JS/native interface |
| Ignoring thread ownership | Race/stutter/crash risk | Know JS vs UI vs native work |
| Writing native modules for everything | Maintenance burden | Use native only when justified |
| No typed native contract | Runtime crashes | Use typed specs/codegen |

---

## 10. Strong Interview Answer

Question:
What are Fabric, TurboModules, and JSI?

Strong answer:

```text
JSI is the lower-level interface that lets JavaScript runtimes interact with
C++/native objects more directly than the old serialized bridge. Fabric is the
new React Native renderer that manages the render, commit, and mount pipeline
for native UI. TurboModules are the newer native module system, often using typed
specs and Codegen, with lazy loading and a cleaner boundary. Together they improve
the architecture for performance, type safety, and native integration, but we
still need to write efficient React code and profile JS/UI thread bottlenecks.
```

---

## 11. Revision Notes

- One-line summary: New Architecture modernizes JS-native communication and rendering.
- Three keywords: JSI, Fabric, TurboModules.
- One interview trap: New Architecture does not magically fix bad renders.
- One memory trick: JSI talks, Fabric renders, TurboModules expose native capability.

---

## 12. JSI Deep Dive — How It Actually Works

### The Old Bridge Problem

The old React Native bridge worked by serializing all communication between JavaScript and native as JSON strings sent asynchronously across a message queue:

```text
Old Bridge Flow:
  JS calls NativeModules.CameraModule.takePicture(options)
  → Serialize to JSON string
  → Post to message queue
  → Native side picks up message
  → Deserialize JSON
  → Execute native code
  → Serialize result to JSON
  → Post result to JS queue
  → Deserialize result in JS
  → Execute callback

Latency: typically 1-5ms per round trip
Limitation: cannot be synchronous — all calls are async
```

### JSI Architecture

JSI (JavaScript Interface) is a C++ header-only library that provides a C++ API for JavaScript runtimes. It allows native C++ objects to be exposed directly to JavaScript as JavaScript objects:

```text
JSI Flow:
  JS holds a reference to a C++ host object
  → Direct method call (no serialization)
  → C++ function executes synchronously or schedules async
  → Return value directly available to JS
  → No JSON, no message queue

Latency: near-zero for synchronous calls
Capability: synchronous reads, shared memory, direct callbacks
```

```cpp
// C++ side — a simple JSI host object
class NativeCounter : public jsi::HostObject {
public:
  int count = 0;
  
  jsi::Value get(jsi::Runtime& rt, const jsi::PropNameID& name) override {
    if (name.utf8(rt) == "increment") {
      return jsi::Function::createFromHostFunction(rt, name, 0,
        [this](jsi::Runtime& rt, const jsi::Value& thisVal, const jsi::Value* args, size_t count) {
          this->count++;
          return jsi::Value(this->count);
        });
    }
    return jsi::Value::undefined();
  }
};

// JS side — calls C++ directly
const counter = global.__nativeCounter;
counter.increment(); // synchronous C++ call — no bridge
```

JSI is what powers:
- Reanimated 2/3 — worklets running on UI thread with shared values
- React Native Vision Camera — frame processors running in C++
- MMKV — synchronous key-value storage

---

## 13. Fabric Renderer — Render Pipeline Internals

Fabric is the new React Native renderer. It replaces the Legacy Renderer and is built on JSI.

### The Three Phases in Detail

```text
Phase 1: RENDER (JavaScript Thread)
  React runs the component tree and calls render functions
  React Native creates ReactShadowNode objects (C++) for each host component
  These form the "React tree" — still JavaScript logic controlling what should exist
  
  Example: <View style={{flex: 1}}><Text>Hello</Text></View>
  Creates: ViewShadowNode{flex:1} → TextShadowNode{content:"Hello"}

Phase 2: COMMIT (JavaScript Thread → Background Thread)
  The new React tree is committed — this is an atomic snapshot
  Yoga layout algorithm runs on the committed tree (C++ — very fast)
  Each node gets calculated position, width, height
  Tree diff is computed: what changed from the previous committed tree?
  Result: a ChangeSet — a list of view mutations (create, update, delete, re-order)

Phase 3: MOUNT (UI Thread)
  The ChangeSet is applied on the UI thread
  Native views (UIView / android.view.View) are created/updated/deleted
  The visible screen reflects the new tree
```

### Why Fabric Is Better

```text
Legacy Renderer:
  - Phase 2 (layout) happened asynchronously on the JS thread
  - Phase 3 (mount) was also asynchronous — UI updates could lag
  - No concurrent rendering support

Fabric Renderer:
  - Phase 2 runs in background thread (not blocking JS)
  - Phase 3 is synchronous — committed tree becomes visible atomically
  - Supports concurrent React features (Suspense, transitions)
  - Enables synchronous native layout measurements from JS (via JSI)
  - Better performance for animated layout changes
```

### The Shadow Tree and Yoga

Every host component (`<View>`, `<Text>`, `<Image>`, etc.) has a shadow node. The shadow tree mirrors the React component tree but exists in C++/native memory.

```text
React Tree (JS objects):
  React Element: {type: 'View', props: {style: {flex: 1}}}
    React Element: {type: 'Text', props: {children: 'Hello'}}

Shadow Tree (C++ objects — Fabric):
  ViewShadowNode: {yogaNode, style: {flex: 1}, children: [...]}
    TextShadowNode: {yogaNode, text: 'Hello', measuredSize: {w:50, h:20}}

Host Views (Native — iOS UIKit / Android View):
  UIView: {frame: CGRect(0, 0, 375, 812)}
    UILabel: {frame: CGRect(0, 0, 50, 20), text: 'Hello'}
```

Yoga is the cross-platform C++ Flexbox layout engine that calculates the `frame` (position + size) for every shadow node. The same algorithm runs on iOS and Android — ensuring consistent layouts.

---

## 14. TurboModules — Lazy Loading and Typed Contracts

### Old Native Modules vs TurboModules

```text
Old Native Module:
  - Registered eagerly at startup (all modules loaded when app starts)
  - No type safety at the JS-native boundary
  - Async-only communication via the bridge
  - Could not be called synchronously

TurboModule:
  - Loaded lazily — only when first used (faster startup)
  - Type-safe — Codegen generates typed bindings from a TypeScript spec
  - Can be called synchronously if needed (via JSI)
  - Better error messages when contract is violated
```

### Codegen in Practice

Codegen is a tool that reads your TypeScript module spec and generates the native (Objective-C/Java/Kotlin/C++) binding code. It creates a type-safe "contract" at the native boundary.

```typescript
// NativeNetworkModule.ts — the TypeScript spec
import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {
  // Codegen reads these signatures and generates native stubs
  fetchData(url: string, options: {method: string; headers: {[key: string]: string}}): Promise<string>;
  cancelRequest(requestId: string): void;
  readonly getMaxConnections: () => number; // synchronous — returns value, not Promise
}

export default TurboModuleRegistry.getEnforcing<Spec>('NetworkModule');
```

```text
Codegen generates from this spec:
  iOS: RCTNativeNetworkModuleSpec.h (Objective-C protocol)
  Android: NativeNetworkModuleSpec.java (Java abstract class)

Native implementation must conform to the generated protocol/class.
At the JS-native boundary, types are validated — wrong argument type = clear error.
```

---

## 15. Enabling New Architecture in Your App

```tsx
// For Expo apps — app.json or app.config.ts
{
  "expo": {
    "newArchEnabled": true
  }
}

// For bare React Native apps — android/gradle.properties
newArchEnabled=true

// iOS — Podfile
# Already enabled by default in React Native 0.76+
```

### Compatibility Considerations

Not all native libraries support New Architecture immediately:

```text
Check library compatibility:
  - reactnative.directory — filter by "New Architecture" support
  - Library's GitHub — look for "New Architecture" or "Fabric" in README

Migration path for incompatible libraries:
  1. Use the Interop Layer — New Architecture has a backward compatibility layer
     that allows Old Architecture native modules to work with limited functionality
  2. Find an Expo-compatible alternative
  3. Contribute New Architecture support to the library
  4. Wait for the maintainer to add support
```

### The Interop Layer

React Native ships with an interop layer that allows old native modules to work in a New Architecture app. Old modules that have not been migrated use the interop layer transparently. Performance may not be optimal through the interop layer but functionality is maintained.

```text
Legacy module behavior in New Architecture:
  Communication still goes through the serialization layer
  No JSI benefits for these modules
  But app does not break — migration can be gradual
```

---

## 16. Hermes — JavaScript Engine Deep Dive

Hermes is Meta's purpose-built JavaScript engine for React Native. Understanding it is a MAANG senior signal.

### AOT Compilation

```text
Traditional JS engines (V8, JSC):
  1. Parse JavaScript source text
  2. Build AST (Abstract Syntax Tree)
  3. Compile to bytecode
  4. JIT compile hot code paths
  Steps 1-3 happen at RUNTIME — adds to startup time

Hermes:
  1. Parse JavaScript source text (at BUILD TIME on CI/developer machine)
  2. Build AST (at BUILD TIME)
  3. Compile to Hermes bytecode (.hbc file) (at BUILD TIME)
  4. App bundle ships as pre-compiled bytecode
  
  At runtime: only load + execute bytecode — no parse, no compile
  Result: faster startup, lower memory (no AST kept in memory)
```

### Impact Numbers (typical production apps)

```text
Without Hermes (JSCore):
  Bundle load + parse + compile: 1200-2500ms on mid-range Android
  
With Hermes:
  Bundle load + bytecode execute: 400-800ms on mid-range Android
  
Memory reduction: typically 10-50MB lower JavaScript heap usage
  (no JIT compiler code cache, leaner runtime)
```

### Hermes Debugging

```text
Hermes CPU profiling:
  In development: shake device → Performance → Start Sampling Profiler
  Download .cpuprofile → Open in Chrome DevTools Performance tab
  Shows: JS execution hot spots, render times, hook overhead

Hermes heap snapshot:
  In development: React Native DevTools → Memory → Take Snapshot
  Shows: all JS objects in heap, reference chains, retained size
  Use to find: closures holding large objects, accumulating arrays/maps
```

---

## 17. Bridgeless Mode (React Native 0.73+)

React Native 0.73+ introduced "Bridgeless Mode" as the final step of the New Architecture:

```text
Old Architecture:
  JS ←→ Bridge (JSON message queue) ←→ Native

New Architecture without Bridgeless:
  JS ←→ JSI (direct) ←→ Native (for TurboModules/Fabric)
  JS ←→ Bridge (still exists) ←→ Some legacy paths

New Architecture + Bridgeless:
  JS ←→ JSI only ←→ Native (bridge completely removed)
  All communication goes through JSI
  No JSON serialization anywhere in the hot path
```

Enable in React Native 0.76+:
```tsx
// android/gradle.properties
bridgelessEnabled=true
// iOS: automatically enabled with New Architecture in 0.76+
```

---

## 18. Interview Answer Upgrade: New Architecture Explained to a Senior Engineer

```text
Q: Explain the React Native New Architecture end-to-end.

A: The New Architecture has three main components built on a shared foundation.

The foundation is JSI — JavaScript Interface — a C++ layer that lets JavaScript
hold direct references to C++ objects and call their methods without JSON
serialization. This enables synchronous communication and shared memory between
JavaScript and native.

On top of JSI, Fabric is the new renderer. It replaces the old async rendering
pipeline with a three-phase synchronous pipeline: render (component tree to
shadow nodes), commit (Yoga layout calculation in background thread), and mount
(atomic application of view mutations to native UI on the main thread). This
enables concurrent React features and eliminates the layout lag of the old renderer.

TurboModules are native modules built on JSI with typed specifications defined in
TypeScript. Codegen reads these specs and generates native binding code,
creating a type-safe contract at the JS-native boundary. TurboModules load lazily
rather than all at startup.

The practical benefits are: faster startup (TurboModules lazy loading), better
animation performance (Reanimated 3 runs directly on UI thread via JSI worklets),
synchronous layout measurements, and support for React concurrent features.

The important caveat is that New Architecture does not fix poorly written React
code — expensive renders, missing memoization, and large lists without
virtualization still need to be addressed in application code.
```

---

## 19. Updated Revision Notes

- JSI = C++ layer enabling direct JS-to-native calls without JSON serialization
- Fabric = three-phase renderer: render (JS thread) → commit (layout on background thread) → mount (UI thread)
- TurboModules = lazy-loaded, typed native modules via JSI + Codegen
- Codegen generates native binding stubs from TypeScript spec files
- Hermes pre-compiles JS at build time → faster startup, lower memory
- Shadow tree mirrors React tree in C++ — Yoga calculates layout
- New Architecture 0.76+ ships with Bridgeless Mode by default
- Interop Layer allows old modules to work in New Architecture apps without full migration
- Best interview answer: acknowledge JSI foundation → Fabric renderer → TurboModules → practical benefits → caveat that app code quality still matters

