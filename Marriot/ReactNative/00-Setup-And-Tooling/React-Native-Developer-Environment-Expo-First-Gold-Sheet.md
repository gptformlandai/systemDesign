# React Native Developer Environment And Expo-First Setup - Gold Sheet

> Track Module - Group 0: Setup And Tooling
> Level: beginner-friendly setup to production-ready local workflow

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Expo-first project creation | Very high | Current recommended path for most new React Native apps |
| Real device vs simulator | High | Many bugs only appear on real hardware |
| Expo Go vs development build | Very high | Senior teams must know when Expo Go is too limited |
| Environment variables | High | Prevents leaking secrets and mixing staging/prod |
| Native toolchain basics | Medium-high | Needed for custom native modules and release debugging |
| Troubleshooting Metro/native builds | High | Common day-to-day mobile engineering skill |

MAANG signal:
You can onboard a new engineer and produce a reproducible mobile dev setup without turning the app into a snowflake machine project.

---

## 2. Mental Model

React Native development has three layers:

```text
JavaScript layer:
  React components, TypeScript, Metro bundler, navigation, state, API clients

React Native framework/tooling layer:
  Expo or bare RN CLI, dev server, development builds, config plugins, EAS

Native platform layer:
  Android SDK, iOS SDK, Xcode, Android Studio, Gradle, CocoaPods, signing
```

Beginner trap:
Thinking "React Native setup" means only installing npm packages. Real setup also includes device runtime, native permissions, build profiles, environment config, and release-safe defaults.

---

## 3. Recommended Starting Path

Use Expo for most new apps:

```bash
npx create-expo-app@latest
cd <app-name>
npx expo start
```

Why:
- It gives a production-grade React Native framework.
- It provides routing, native module conventions, app config, and build/update tooling.
- It lets you delay native complexity until you actually need it.

Use bare React Native only when:
- You are integrating into an existing native app.
- You have deep custom native platform constraints.
- Your company already owns mature Xcode/Gradle infrastructure.
- Expo config plugins/development builds cannot support the native requirement.

---

## 4. Local Machine Checklist

Minimum:
- Node LTS version pinned by the project.
- One package manager chosen and locked: npm, pnpm, yarn, or bun.
- Git and a clean lockfile workflow.
- Expo CLI through `npx`, not a stale global install.
- A physical iOS or Android device for realistic testing.

For Android:
- Android Studio.
- Android SDK platform installed.
- Android emulator or real Android device.
- `ANDROID_HOME` or Android SDK path configured when native builds are needed.

For iOS on macOS:
- Xcode.
- Xcode command line tools.
- CocoaPods when using native iOS builds.
- Simulator runtime or physical iPhone.

Production habit:
Document the supported versions in the repo. Do not depend on "whatever works on my laptop."

---

## 5. Device Strategy

| Target | Best For | Limitations |
|---|---|---|
| Expo Go | Learning, demos, quick UI checks | Cannot include custom native modules |
| Development build | Real app development | Requires a native build once dependencies change |
| iOS Simulator | Fast iOS UI loop | Not real camera, push, Bluetooth, performance, biometric behavior |
| Android Emulator | Fast Android loop | Performance and OEM behavior may differ |
| Real device | Permissions, camera, push, biometrics, performance | Slightly slower iteration |

Rule:
Use Expo Go for learning. Use development builds for serious production projects.

---

## 6. Expo Go vs Development Build

Expo Go:

```text
Shared client app installed from the store.
Runs many standard Expo SDK APIs.
Great for learning and simple prototypes.
Cannot include arbitrary custom native code.
```

Development build:

```text
Custom debug version of your own app.
Includes your exact native modules, plugins, app id, permissions, and runtime.
Used by professional Expo teams before store release.
```

Typical commands:

```bash
npx expo install expo-dev-client
npx expo run:ios
npx expo run:android
```

Cloud build option:

```bash
eas build --profile development --platform ios
eas build --profile development --platform android
```

---

## 7. Project Baseline

Recommended starter conventions:

```text
src/
  app/ or screens/
  features/
  components/
  services/
  api/
  hooks/
  storage/
  theme/
  test/

app.config.ts
eas.json
tsconfig.json
eslint.config.js
```

Baseline quality gates:

```bash
npx tsc --noEmit
npm run lint
npm test
npx expo-doctor
```

Use equivalent commands for the package manager chosen by the repo.

---

## 8. App Config

`app.config.ts` is where Expo apps describe native identity and configuration.

```ts
import type {ExpoConfig} from 'expo/config';

const config: ExpoConfig = {
  name: 'MasteryApp',
  slug: 'mastery-app',
  scheme: 'mastery',
  ios: {
    bundleIdentifier: 'com.company.mastery',
    supportsTablet: true,
  },
  android: {
    package: 'com.company.mastery',
    adaptiveIcon: {
      foregroundImage: './assets/adaptive-icon.png',
      backgroundColor: '#ffffff',
    },
  },
  plugins: [
    'expo-router',
    'expo-secure-store',
  ],
  extra: {
    apiBaseUrl: process.env.EXPO_PUBLIC_API_BASE_URL,
  },
};

export default config;
```

Rules:
- Only expose client-safe values through public env variables.
- Never put server secrets in app config.
- Treat anything in a mobile binary as inspectable.

---

## 9. Environment Config

Common environment split:

```text
local:
  local dev server, mock auth, verbose logs

development:
  shared development API, development build, dev telemetry

staging:
  production-like backend, release candidate, store/internal test track

production:
  public backend, production telemetry, strict logging
```

Runtime config should answer:
- Which API base URL?
- Which feature flag environment?
- Which update channel?
- Which analytics/crash project?
- Which app variant or bundle id?

Interview trap:
Do not use a single app binary that can freely switch to any backend in production unless security, compliance, and support teams explicitly approve it.

---

## 10. Metro And Cache Troubleshooting

Common fixes:

```bash
npx expo start --clear
watchman watch-del-all
rm -rf node_modules
npm install
```

Use cache clearing when:
- Metro serves stale code.
- Asset changes do not appear.
- Native module installation changed.
- A package was upgraded and generated files look inconsistent.

Do not use cache clearing as diagnosis replacement. Always identify whether the failure is JS, Metro, package manager, Gradle, CocoaPods, signing, or simulator state.

---

## 11. Common Setup Failures

| Symptom | Likely Cause | Fix Direction |
|---|---|---|
| App cannot connect to dev server | Device/network mismatch | Same Wi-Fi, tunnel mode, correct host |
| Native module not found | Missing development build | Rebuild dev client |
| Works in Expo Go, fails in dev build | Native config mismatch | Check plugins, permissions, prebuild output |
| Android build fails | SDK/Gradle/JDK mismatch | Check Android Studio and project versions |
| iOS build fails | Pods/signing/Xcode mismatch | `pod install`, Xcode version, signing |
| Release works differently | Dev-only tooling hidden | Test release profile on real device |

---

## 12. Beginner Setup Lab

Build this in 45 minutes:

1. Create a new Expo app.
2. Add TypeScript strict mode.
3. Add one screen with `TextInput`, `Pressable`, and `FlatList`.
4. Add one environment variable for API base URL.
5. Run on one simulator/emulator.
6. Run on one real device.
7. Convert to a development build.
8. Document the exact commands used.

Pass condition:
Another engineer can clone the repo, run the commands, and reach the same app state.

---

## 13. Strong Interview Answer

```text
For most new React Native apps I would start with Expo because it gives a
production-grade framework, routing, native module conventions, and EAS for build,
submit, and updates. I would use Expo Go only for learning or quick prototypes.
For production work I would create a development build so the app includes its
actual native modules, app config, permissions, and runtime. I would document Node,
package manager, iOS/Android tooling, app config, environment variables, and the
quality gates so the setup is reproducible across the team.
```

---

## 14. Revision Notes

- One-line summary: React Native setup is a reproducible mobile runtime workflow, not just an npm install.
- Three keywords: Expo-first, development build, app config.
- One interview trap: Saying Expo Go is enough for production native-module development.
- Memory trick: Expo Go is the playground; development build is your real app.
