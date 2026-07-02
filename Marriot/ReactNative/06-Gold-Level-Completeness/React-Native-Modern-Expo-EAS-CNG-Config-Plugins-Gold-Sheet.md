# React Native Modern Expo, EAS, CNG, And Config Plugins - Gold Sheet

> Track Module - Group 6: Gold-Level Completeness
> Level: modern Expo production workflow

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Expo as React Native framework | Very high | Recommended path for most new apps |
| Development builds | Very high | Real production workflow beyond Expo Go |
| Continuous Native Generation | High | Keeps native folders generated from config |
| Config plugins | High | Customizes native projects without manual drift |
| EAS Build/Submit/Update | Very high | Production delivery path |
| runtimeVersion/channels | High | Prevents OTA/native mismatch |

MAANG signal:
You know Expo is not just a beginner tool. It can be a serious production delivery system when used with development builds, config plugins, and EAS discipline.

---

## 2. Mental Model

Modern Expo flow:

```text
app.config.ts
  -> config plugins
  -> generated native projects through CNG/prebuild
  -> development build for daily work
  -> EAS Build for signed binaries
  -> EAS Submit for store upload
  -> EAS Update for compatible JS/assets updates
```

Key idea:
The app config becomes the source of truth for native identity and many native modifications.

---

## 3. Expo Go vs Development Build

Expo Go:
- useful for learning and quick checks
- shared runtime
- limited to included native modules
- not representative of custom native dependencies

Development build:
- your app id and native runtime
- includes your selected native modules
- supports config plugins
- closer to release behavior
- correct default for production teams

Strong answer:
Expo Go is for fast learning. Development builds are for real app development.

---

## 4. Continuous Native Generation

Continuous Native Generation means native folders can be generated from config and dependencies instead of treated as hand-maintained source for every change.

Flow:

```bash
npx expo prebuild
npx expo run:ios
npx expo run:android
```

Use CNG when:
- native configuration can be expressed through app config and plugins
- the team wants reproducible native projects
- manual native folder drift would slow upgrades

Be cautious when:
- native teams heavily customize Xcode/Gradle
- the app embeds complex existing native screens
- a compliance process requires manual native project ownership

---

## 5. Config Plugins

Config plugins automate native modifications.

They can:
- add permissions
- edit Info.plist
- edit AndroidManifest.xml
- configure entitlements
- add URL schemes
- configure native SDKs
- add build properties

Example:

```ts
import type {ExpoConfig} from 'expo/config';

const config: ExpoConfig = {
  name: 'MasteryApp',
  slug: 'mastery-app',
  scheme: 'mastery',
  plugins: [
    'expo-router',
    'expo-secure-store',
    [
      'expo-location',
      {
        locationWhenInUsePermission: 'Allow MasteryApp to show nearby jobs.',
      },
    ],
  ],
};

export default config;
```

Rule:
Prefer config plugins over manual native edits when the change is repeatable and supported.

---

## 6. EAS Build Profiles

Example `eas.json`:

```json
{
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "channel": "development"
    },
    "preview": {
      "distribution": "internal",
      "channel": "preview"
    },
    "production": {
      "channel": "production",
      "autoIncrement": true
    }
  },
  "submit": {
    "production": {}
  }
}
```

Profiles should encode:
- distribution type
- channel
- env variables/secrets
- app variant
- signing behavior
- resource class if needed

---

## 7. EAS Build And Submit

Build:

```bash
eas build --profile production --platform ios
eas build --profile production --platform android
eas build --profile production --platform all
```

Submit:

```bash
eas submit --platform ios
eas submit --platform android
```

Release gate:
- build number unique
- source maps uploaded
- native symbols uploaded
- store metadata ready
- privacy declarations reviewed
- smoke tested on physical devices
- staged rollout plan ready

---

## 8. EAS Update

OTA update can change:
- JavaScript logic
- UI layout
- copy
- assets bundled through update
- feature flag behavior

OTA update cannot safely change:
- native modules
- permissions
- entitlements
- app icons/splash native config
- native SDK versions
- runtime-incompatible assumptions

Command:

```bash
eas update --channel production --message "Fix order history empty state"
```

Rule:
An OTA update must be compatible with every binary on that channel and runtime version.

---

## 9. runtimeVersion Strategy

Use `runtimeVersion` to group binaries that can safely run the same update.

Common policies:

```text
appVersion:
  runtime changes when app version changes

nativeVersion:
  runtime changes when native version changes

fingerprint:
  runtime derived from native/runtime inputs
```

Senior rule:
When native dependencies or config change, force a new runtime boundary so old binaries do not receive incompatible JS.

---

## 10. Workflow Automation

Recommended automation:

```text
Pull request:
  typecheck, lint, test, expo-doctor

Merge to main:
  preview build or update for QA

Release tag:
  production EAS Build
  source maps/symbols upload
  EAS Submit
  metadata push
  staged rollout

Hotfix:
  decide OTA vs binary
  publish to narrow channel or rollout
  monitor crash-free sessions
```

---

## 11. When To Leave Expo Workflow

Consider bare or custom native ownership when:
- integrating into a large existing native app
- native teams require direct ownership of Xcode/Gradle
- config plugins cannot express required changes
- custom native SDKs require unusual build steps
- enterprise signing/security constraints cannot fit EAS

Even then, Expo modules and EAS may still be usable depending on the architecture.

---

## 12. Strong Interview Answer

```text
In a modern React Native app I would start Expo-first, but I would not rely on
Expo Go for production development. I would use development builds, app.config.ts
as native configuration source of truth, config plugins for repeatable native
changes, and EAS Build/Submit/Update for delivery. I would protect OTA updates
with runtimeVersion and channels so JavaScript is never shipped to a binary that
lacks the required native code or permissions.
```

---

## 13. Revision Notes

- One-line summary: Modern Expo is app config plus dev builds plus EAS delivery.
- Three keywords: dev build, config plugin, runtimeVersion.
- One interview trap: Saying "Expo means no native code."
- Memory trick: Config creates native, EAS ships native, Update ships compatible JS.
