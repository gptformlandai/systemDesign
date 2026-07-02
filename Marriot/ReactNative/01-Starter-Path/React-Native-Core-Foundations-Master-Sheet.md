# React Native Core Foundations - Master Sheet

> Track Module - Group 1: Starter Path
> Level: beginner to interview-ready | Mode: understand what React Native really is

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| React Native vs React Web | Very high | Most beginners incorrectly assume DOM/CSS/WebView behavior |
| Native views | Very high | This is the core identity of React Native |
| Expo vs bare React Native | High | Modern app setup and production delivery depend on this decision |
| Metro bundler | Medium | Explains build/startup behavior and module resolution |
| Hermes | High | Common production JS engine and performance topic |
| App startup flow | High | Senior interviews ask what happens before the first screen |
| iOS vs Android differences | High | Mobile apps are not fully platform-neutral |
| New Architecture awareness | High | You need vocabulary: Fabric, TurboModules, JSI |

MAANG signal:
You can explain React Native as a native UI runtime controlled by React, not as a mobile website.

---

## 2. Intuition

React Native is like having React write instructions to native iOS and Android UI toolkits.

React Web:

```text
React component -> virtual DOM -> browser DOM -> pixels
```

React Native:

```text
React component -> React Native renderer -> native views -> pixels
```

The beginner sentence:
React Native lets you write React components in JavaScript or TypeScript, but the screen is made of real native views such as `UIView` on iOS and `View`/`TextView` on Android.

---

## 3. Definition

- Definition: React Native is a framework for building native mobile apps using React and JavaScript/TypeScript.
- Category: Cross-platform native mobile framework.
- Core idea: Share business logic and UI structure while rendering platform-native controls.

---

## 4. What React Native Is Not

React Native is not:
- A WebView wrapper.
- A CSS-in-browser runtime.
- A promise that every native difference disappears.
- A replacement for all native iOS/Android knowledge.

React Native is:
- React for app logic.
- Native host components for actual UI.
- A bridge/new architecture layer for JS-to-native communication.
- A cross-platform productivity tool with real mobile constraints.

---

## 5. Expo vs Bare React Native

### Expo

Expo is a React Native framework and toolchain.

Use Expo when:
- You are starting a new app.
- You want file-based routing, EAS builds, OTA updates, standard native modules, and fast setup.
- Your native requirements fit within Expo modules or config plugins.

### Bare React Native

Bare React Native gives direct ownership of `ios/` and `android/`.

Use bare when:
- You need custom native code that does not fit Expo's workflow.
- You are integrating React Native into an existing native app.
- Your organization already has deep iOS/Android build infrastructure.

Interview answer:

```text
For a new production app, I would usually start with Expo because it gives routing,
build tooling, native modules, and release workflows early. I would choose bare
React Native when the app has unusual native constraints, heavy custom SDK work,
or must be embedded into an existing native codebase.
```

---

## 6. App Startup Flow

High-level flow:

```text
1. User taps app icon.
2. Native app process starts.
3. Native entry point initializes React Native runtime.
4. JavaScript bundle loads through Metro in dev or from packaged bundle in release.
5. Hermes or another JS engine executes the bundle.
6. AppRegistry registers the root React component.
7. React builds the component tree.
8. React Native renderer creates shadow nodes and native host views.
9. First screen is mounted on the UI thread.
```

Code:

```tsx
import {AppRegistry} from 'react-native';
import App from './src/App';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);
```

What to say in interviews:

```text
The native shell starts first, then the JavaScript runtime loads the bundle.
React executes the root component and React Native translates the rendered tree
into native views. Performance during startup depends on native initialization,
bundle size, JS execution, image/font loading, and the work done before first paint.
```

---

## 7. Core Components

| React Native | Native Meaning | Web Analogy |
|---|---|---|
| `View` | Basic container | `div` |
| `Text` | Text node | `span` / text |
| `Image` | Native image view | `img` |
| `TextInput` | Native text field | `input` |
| `ScrollView` | Scroll container | scrollable div |
| `FlatList` | Virtualized list | windowed list |
| `Pressable` | Touch interaction wrapper | button-ish interaction |
| `Modal` | Native modal overlay | dialog |

Important:
You cannot put raw text directly inside `View`. Text must be inside `Text`.

Bad:

```tsx
<View>Hello</View>
```

Good:

```tsx
<View>
  <Text>Hello</Text>
</View>
```

---

## 8. Hello Screen

```tsx
import {SafeAreaView, StyleSheet, Text, View} from 'react-native';

export default function HomeScreen() {
  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <Text style={styles.title}>React Native</Text>
        <Text style={styles.subtitle}>Native apps with React thinking.</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    fontSize: 32,
    fontWeight: '700',
  },
  subtitle: {
    marginTop: 8,
    fontSize: 16,
    color: '#475569',
  },
});
```

What this teaches:
- `SafeAreaView` protects content from notches and system UI.
- `View` is the layout container.
- `Text` owns text rendering.
- `StyleSheet.create` organizes styles.
- `flex: 1` fills available space.

---

## 9. Metro, Bundles, And Hermes

### Metro

Metro is the JavaScript bundler commonly used by React Native.

It:
- Resolves imports.
- Transforms TypeScript/JavaScript.
- Serves the dev bundle.
- Produces release bundles.

### Hermes

Hermes is a JavaScript engine optimized for React Native.

It helps with:
- Startup performance.
- Memory usage.
- Bytecode execution.
- Debuggability in modern React Native tooling.

Interview answer:

```text
Metro prepares the JavaScript bundle. Hermes executes that JavaScript bundle on
device. The bundle contains the React Native app logic, while native platform code
hosts the app shell and renders native views.
```

---

## 10. Platform Differences

React Native shares code, but platform differences still matter.

Examples:
- Android has a hardware/software back button concept.
- iOS has notches, safe areas, and different permission wording.
- Android permissions can vary by OS version.
- Push notification behavior differs by platform.
- Text rendering and font metrics differ.
- Native modules may support one platform before another.

Platform-specific file names:

```text
Button.ios.tsx
Button.android.tsx
```

React Native automatically resolves the correct file by platform.

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Calling RN a WebView | It misrepresents the runtime | Say RN renders native views |
| Using `ScrollView` for huge lists | It renders too much content | Use `FlatList` or `SectionList` |
| Putting all state in Redux | Overkill and causes noisy architecture | Separate UI state, server state, and global app state |
| Ignoring release builds | Dev mode is slower and noisier | Profile performance in release mode |
| Assuming iOS and Android behave the same | Native behavior differs | Test both platforms |
| Logging secrets or PII | Mobile logs can leak sensitive data | Use structured safe logging |

---

## 12. Strong Interview Answer

Question:
What is React Native and how does it work?

Strong answer:

```text
React Native is a framework for building native iOS and Android apps using React.
Unlike a WebView framework, it does not render HTML. React components describe
the UI, and the React Native renderer maps that component tree to native host
views. The app runs JavaScript in a JS engine such as Hermes, while native code
owns the platform shell and the UI thread. For production work, I pay attention
to app startup, JS thread load, native module boundaries, platform differences,
and release-build performance.
```

---

## 13. Revision Notes

- One-line summary: React Native is React controlling native mobile views.
- Three keywords: native views, JS runtime, platform constraints.
- One interview trap: Do not say React Native renders DOM.
- One memory trick: React Web ends at browser DOM; React Native ends at iOS/Android views.

---

## 14. The Full App Startup Flow — Step by Step

Understanding what happens from tap to first render is a senior-level signal. Here is the complete sequence for a Hermes + Expo app:

```text
1. User taps app icon
   → OS creates application process
   → iOS: loads AppDelegate, initializes UIWindow
   → Android: loads MainActivity, initializes ReactRootView

2. Native initialization (50-200ms)
   → Native modules registered (Camera, Location, Notifications, etc.)
   → TurboModule registry prepared (New Architecture)
   → Fabric renderer initialized (New Architecture)
   → JavaScript engine (Hermes) loaded into memory

3. Bundle load (50-300ms depending on bundle size)
   → main.jsbundle (or index.android.bundle) read from app package
   → With Hermes: bytecode already compiled at build time — skip parse step
   → Without Hermes: parse + compile JS source at runtime (adds 200-600ms)

4. Module evaluation (50-200ms)
   → All import/require statements execute
   → React, React Navigation, Zustand, etc. modules initialize
   → This is why large bundles cause slow startups

5. App component first render (50-150ms)
   → React evaluates App component tree
   → Navigation stack initialized
   → Initial screen component rendered
   → Shadow tree built and layout calculated

6. Mount to native views (16-50ms)
   → Shadow tree mutations applied to iOS/Android native views
   → First frame visible to user

7. Hydration / async initialization (variable)
   → AsyncStorage reads for auth state
   → Initial API calls fire
   → Auth state check → redirect to login or home
```

### Startup Optimization Targets

```text
Phases 1-2 (native init): Not much you can do — minimize native modules registered
Phase 3 (bundle load): Keep bundle small, remove unused dependencies
Phase 4 (module eval): Use dynamic imports / lazy loading for non-critical screens  
Phase 5 (first render): Simplify App component, defer non-critical useEffects
Phase 7 (hydration): Cache auth state, use splash screen to hide loading
```

---

## 15. Metro Bundler — Deep Dive

Metro is React Native's JavaScript bundler (like Webpack but purpose-built for mobile).

### What Metro Does

```text
1. RESOLUTION: Given an import, find the actual file
   import {Button} from './components/Button'
   → Look for Button.tsx, Button.ts, Button.js, Button/index.ts, etc.
   → Check node_modules if not local
   → Platform-specific: Button.ios.tsx takes priority over Button.tsx on iOS

2. TRANSFORMATION: Convert each file
   → TypeScript → JavaScript (via Babel)
   → JSX → React.createElement calls
   → New syntax → compatible ES5 (for older Hermes versions)
   → Apply any configured Babel plugins

3. SERIALIZATION: Bundle all modules
   → Wrap each module in a factory function with a module ID
   → Produce one or more .bundle files
   → Generate source maps for debugging
```

### Metro Config

```typescript
// metro.config.js — common production configurations
const {getDefaultConfig} = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// Add file extensions for resolution
config.resolver.assetExts.push('lottie', 'caf');

// Support monorepo — add sibling package sources
config.watchFolders = [path.resolve(__dirname, '../shared')];

// Alias resolution — clean imports instead of ../../utils
config.resolver.extraNodeModules = {
  '@components': path.resolve(__dirname, 'src/components'),
  '@utils': path.resolve(__dirname, 'src/utils'),
  '@hooks': path.resolve(__dirname, 'src/hooks'),
};

module.exports = config;
```

### Metro Dev Server vs Production Bundle

```text
Development (Metro dev server running):
  - JS runs from Metro server over USB/LAN
  - Hot Module Replacement: changed module injected without full reload
  - Fast Refresh: component state preserved on save
  - Source maps loaded automatically for debugging
  - No Hermes AOT — slower execution

Production (bundled):
  - Bundle baked into app binary at build time
  - Hermes pre-compiles to bytecode at build time
  - No dev server connection needed
  - Source maps optional (needed for Sentry symbolication)
```

---

## 16. Hermes Engine — What Every Developer Should Know

### Why Hermes for Mobile

```text
V8 and SpiderMonkey were designed for long-running browser/server sessions:
  - Optimize code over time with JIT compilation
  - Trade startup time for long-run throughput
  - Large memory footprint acceptable (desktop/server RAM is plentiful)

Mobile reality:
  - Sessions last 30 seconds to 5 minutes on average
  - JIT warmup cost matters more than peak throughput
  - RAM is shared with many apps — iOS/Android will kill your app if it uses too much
  - Startup time directly impacts user retention (every 100ms adds ~1% bounce)

Hermes is designed for this:
  - AOT (ahead-of-time) compilation at build time → no startup parse/compile
  - Conservative memory allocator → lower peak heap usage
  - No JIT → predictable, consistent performance (no "hot" vs "cold" path inconsistency)
```

### Enabling Hermes

```text
React Native 0.64+: Hermes on Android by default
React Native 0.70+: Hermes on iOS by default
React Native 0.76+: Old JS engine (JSC) not officially supported

To verify Hermes is running:
```

```tsx
import {HermesInternal} from 'react-native';

const isHermes = () => !!HermesInternal;
console.log('Running Hermes:', isHermes());  // should be true in production
```

---

## 17. Expo Ecosystem — Choosing the Right Setup

### Expo Managed vs Bare Workflow vs Expo Router

```text
Expo Managed Workflow (recommended for most apps):
  - Managed by Expo: builds, updates, native module management
  - Use expo install to add modules — Expo handles native compatibility
  - app.json / app.config.ts for all native configuration
  - No Xcode or Android Studio required for most development
  - Limitation: Expo Go only includes the Expo SDK runtime; custom native modules
    require a development build, config plugin, or generated native project path
  
  Best for: teams without native mobile expertise, B2B apps, rapid prototyping

Expo with CNG (Continuous Native Generation — previously "prebuild"):
  - You write app.config.ts, Expo generates /android and /ios directories
  - Use config plugins to customize native code without writing native code
  - Run npx expo prebuild to generate native projects when needed
  - Full control over native layer while keeping Expo tooling
  
  Best for: teams that want Expo tools but need custom native modules

Bare React Native:
  - Full native code in /android and /ios directories
  - You manage all native dependencies and upgrades
  - Use npx react-native upgrade to upgrade
  - Maximum flexibility — can write any native code
  
  Best for: apps with heavy native requirements or existing native codebases

Expo Router (file-based navigation):
  - Routing based on file structure (like Next.js App Router)
  - Automatic deep linking based on file paths
  - Universal apps (React Native + Web) from same codebase
  - Recommended for new Expo projects
```

### Expo Go vs Development Client

```text
Expo Go:
  - Prebuilt app on App Store/Play Store — scan QR to run your app inside it
  - No native code compilation required
  - Best for learning and quick checks
  - Limitation: only supports the native modules bundled into Expo Go
  
Development Client (npx expo run:ios / run:android):
  - Builds a custom dev client app that includes your specific native dependencies
  - Requires Xcode or Android Studio
  - Supports any native module
  - Better performance — your app, not a shared container
  
EAS Build (cloud builds):
  - Build iOS and Android in the cloud — no Xcode/Android Studio required locally
  - Free tier available
  - Fastest path to TestFlight and Google Play internal testing

Modern Expo interview answer:
Do not say "eject" as the default. Most production Expo apps move from Expo Go
to development builds, config plugins, CNG/prebuild, and EAS before choosing full
bare ownership.
```

---

## 18. React Native vs Other Mobile Approaches

```text
React Native:
  - One JS codebase runs on iOS + Android
  - Components compile to real native views
  - Good performance for business apps, e-commerce, fintech
  - Large ecosystem, shared web developer skills
  - Gap: cannot use every native capability without native modules

Flutter:
  - Uses Dart language
  - Draws its own widgets with Skia/Impeller — no native views
  - More consistent cross-platform pixel-perfect UI
  - Better performance on animation-heavy UIs (because no native view mapping)
  - Larger performance overhead for apps that primarily display text/lists

Native (Swift/Kotlin):
  - Best performance, full platform capability access
  - Requires separate iOS and Android teams
  - No shared code between platforms
  - Fastest integration of OS-level features (ARKit, etc.)

React Native Web (react-native-web):
  - Share components between mobile and web
  - Components render to DOM on web, native views on mobile
  - Good for simple component sharing — complex layouts still need platform work
```

---

## 19. Updated Revision Notes

- React Native: React components → shadow tree (C++) → Yoga layout → native views (iOS UIView / Android View)
- Never say it renders HTML or DOM — that is the most common interview mistake
- Metro bundler: resolves imports, transforms TypeScript/JSX, produces .bundle
- Hermes: pre-compiles JS to bytecode at build time → faster startup, lower memory than V8/JSC
- Expo Managed: no native code written, Expo manages native dependencies
- Expo CNG: app.config.ts + config plugins → Expo generates native projects
- Bare React Native: full native code ownership, maximum flexibility
- App startup flow: native init → Hermes load → bundle load → module eval → first render → hydration
