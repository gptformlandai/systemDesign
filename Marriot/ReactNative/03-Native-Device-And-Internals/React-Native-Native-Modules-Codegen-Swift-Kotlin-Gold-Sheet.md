# React Native Native Modules, Codegen, Swift, And Kotlin - Gold Sheet

> Track Module - Group 3: Native Device And Internals
> Level: senior mobile internals and library engineering

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| When to write native code | Very high | Prevents overengineering and bad package choices |
| TurboModule mental model | High | Core of the New Architecture native API story |
| Codegen | High | Type-safe JS/native boundary |
| Swift/Kotlin implementation shape | Medium-high | Shows real mobile depth |
| Native events | Medium | Required for sensors, connectivity, auth callbacks |
| Testing and release risk | High | Native bugs can break app startup |

MAANG signal:
You know when React Native stops being JavaScript and becomes platform engineering.

---

## 2. Mental Model

A native module exposes platform capability to JavaScript.

```text
JS/TS component or hook
  -> typed native module spec
  -> Codegen generated glue
  -> Kotlin/Java implementation on Android
  -> Swift/Objective-C/Objective-C++ implementation on iOS
  -> platform SDK or OS API
```

The best native module feels boring to app developers:
- typed API
- predictable errors
- stable threading
- no UI blocking
- clear permission model
- deterministic cleanup

---

## 3. When To Write A Native Module

Write one when:
- No maintained library exists.
- You need proprietary SDK integration.
- You need a platform API not exposed by Expo/RN libraries.
- You need high-performance native work outside the JS thread.
- You need a native view or native event stream.

Avoid one when:
- A maintained Expo/RN package already solves it.
- The behavior can be implemented in JS safely.
- The team cannot maintain iOS and Android code.
- The API is unstable and likely to change weekly.

Decision rule:
Native modules increase capability and maintenance cost at the same time.

---

## 4. TurboModule Shape

TypeScript spec:

```ts
// specs/NativeBattery.ts
import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {
  getBatteryLevel(): Promise<number>;
  isLowPowerModeEnabled(): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeBattery');
```

Consumer hook:

```ts
import {useEffect, useState} from 'react';
import NativeBattery from '../specs/NativeBattery';

export function useBatteryLevel() {
  const [level, setLevel] = useState<number | null>(null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let active = true;

    NativeBattery.getBatteryLevel()
      .then(value => {
        if (active) setLevel(value);
      })
      .catch(reason => {
        if (active) setError(reason instanceof Error ? reason : new Error(String(reason)));
      });

    return () => {
      active = false;
    };
  }, []);

  return {level, error};
}
```

---

## 5. Codegen Configuration

Typical package config:

```json
{
  "codegenConfig": {
    "name": "BatterySpec",
    "type": "modules",
    "jsSrcsDir": "specs",
    "android": {
      "javaPackageName": "com.company.battery"
    }
  }
}
```

Codegen rules:
- TurboModule spec files commonly start with `Native`.
- Fabric component specs commonly end with `NativeComponent`.
- Generated files should not be hand-edited.
- The native implementation must match the generated interface.

Android generation:

```bash
cd android
./gradlew generateCodegenArtifactsFromSchema
```

iOS generation is normally integrated into the build/pod workflow, with manual script options available for advanced debugging.

---

## 6. Android Kotlin Implementation Shape

```kotlin
package com.company.battery

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

class NativeBatteryModule(
  reactContext: ReactApplicationContext
) : NativeBatterySpec(reactContext) {

  override fun getName(): String = NAME

  override fun getBatteryLevel(promise: Promise) {
    try {
      val level = readBatteryLevel(reactApplicationContext)
      promise.resolve(level)
    } catch (error: Exception) {
      promise.reject("BATTERY_READ_FAILED", error)
    }
  }

  override fun isLowPowerModeEnabled(promise: Promise) {
    promise.resolve(false)
  }

  companion object {
    const val NAME = "NativeBattery"
  }
}
```

Production rules:
- Do heavy work off the main thread.
- Validate permissions before touching protected APIs.
- Return stable error codes.
- Avoid leaking `Activity` references.

---

## 7. iOS Swift Implementation Shape

Swift is commonly exposed through Objective-C/Objective-C++ bridging in React Native native modules.

```swift
import Foundation
import UIKit

@objc(NativeBattery)
class NativeBattery: NSObject {
  @objc
  func getBatteryLevel(
    _ resolve: RCTPromiseResolveBlock,
    rejecter reject: RCTPromiseRejectBlock
  ) {
    UIDevice.current.isBatteryMonitoringEnabled = true
    let value = UIDevice.current.batteryLevel

    if value < 0 {
      reject("BATTERY_READ_FAILED", "Battery level unavailable", nil)
      return
    }

    resolve(Double(value))
  }
}
```

Senior note:
Exact iOS boilerplate differs by React Native version, New Architecture setup, and whether the module is app-local or packaged as a library. The stable concept is the same: typed JS spec, generated glue, native implementation, and consistent exported name.

---

## 8. Native Events

Use events when the native side pushes data over time:
- Bluetooth scan results
- network changes
- auth SDK callbacks
- location updates
- media playback state
- sensor data

Event design:

```ts
type BatteryEvent = {
  level: number;
  lowPowerMode: boolean;
  timestamp: number;
};
```

Rules:
- Provide `start` and `stop` semantics.
- Remove listeners on unmount.
- Backpressure high-frequency events.
- Do not emit sensitive data without consent.

---

## 9. Fabric Native Component Use Cases

Use a native view when:
- The UI must be implemented by a platform SDK.
- Rendering through JS would be too slow.
- You need a map, camera preview, video player, ad SDK, or payment widget.

Fabric component mental model:

```text
TypeScript native component spec
  -> Codegen props/events
  -> native view manager
  -> native platform view
  -> React props update native state
```

Interview trap:
A TurboModule exposes functions. A Fabric component exposes a native view.

---

## 10. Expo Integration

For Expo apps:
- Prefer existing Expo modules first.
- Use development builds for custom native modules.
- Use config plugins to modify native configuration.
- Use Continuous Native Generation/prebuild when native folders are generated from app config.

Config plugin responsibilities:
- add permissions
- add iOS entitlements
- edit Android manifest
- edit Info.plist
- add native SDK config

Do not tell a team to eject as the first answer. Modern Expo often supports custom native code through development builds and config plugins.

---

## 11. Testing Checklist

Unit level:
- JS wrapper handles success/error.
- Hook cleans up listeners.
- Error codes map to user-safe messages.

Native level:
- Android implementation tested on API versions supported by the app.
- iOS implementation tested on supported iOS versions.
- Permission denied path tested.
- App startup tested with module installed.

Release level:
- Debug and release builds both work.
- Source maps and native symbols uploaded.
- Crash reporting includes module version.
- OTA updates do not call APIs missing from older binaries.

---

## 12. Failure Modes

| Failure | User Impact | Mitigation |
|---|---|---|
| Native module missing | App crash on import/startup | Guard imports, rebuild binary, release gate |
| Thread violation | UI freezes or crashes | Move work off main thread where appropriate |
| Permission denied | Feature unavailable | Explain and degrade gracefully |
| SDK callback after unmount | Memory leak or setState warning | Listener cleanup |
| Android-only behavior | iOS parity bug | Platform contract tests |
| OTA/native mismatch | Production crash | runtimeVersion/channel discipline |

---

## 13. Strong Interview Answer

```text
I write a native module only when a maintained library or Expo module cannot meet
the requirement. In the New Architecture, I define a typed TypeScript spec, let
Codegen create the JS/native glue, and implement the platform behavior in Kotlin
and Swift or Objective-C. I keep the API small, promise-based for one-shot work,
event-based for streams, and I treat permissions, threading, cleanup, and release
compatibility as part of the design. For Expo apps I use development builds and
config plugins instead of treating eject as the default path.
```

---

## 14. Revision Notes

- One-line summary: Native modules are typed bridges from JS to platform APIs.
- Three keywords: TurboModule, Codegen, config plugin.
- One interview trap: Saying every custom native need requires ejecting from Expo.
- Memory trick: Functions are TurboModules; views are Fabric components.
