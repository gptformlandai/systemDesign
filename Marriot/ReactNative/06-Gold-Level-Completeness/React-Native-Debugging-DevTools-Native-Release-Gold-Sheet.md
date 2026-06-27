# React Native Debugging, DevTools, Native Logs, And Release Builds - Gold Sheet

> Track File #18 of 20 - Group 6: Gold-Level Completeness
> Level: practical debugging workflow for real teams

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| React Native DevTools | High | Modern JS/component debugging |
| Dev Menu | Medium-high | Daily development workflow |
| LogBox | Medium | Warnings/errors during development |
| Performance Monitor | High | Quick JS/UI FPS signal |
| Xcode/Android Studio logs | High | Native issues require native evidence |
| Release-build debugging | Very high | Many bugs only happen in release |
| Source maps | Very high | Crash stack traces need symbolication |
| Native crash triage | High | Senior mobile debugging signal |

MAANG signal:
You can debug across JavaScript, React render logic, native platform logs, release builds, and crash reporting.

---

## 2. Debugging Map

```text
JavaScript bug:
  React Native DevTools, console, breakpoints, component inspector

Render/state bug:
  React DevTools profiler, component tree, state/props inspection

Native module bug:
  Xcode logs, Android Studio Logcat, native stack trace

Performance bug:
  release build, JS/UI FPS, profiler, native instruments

Release-only bug:
  local release build, source maps, native symbols, build config comparison

Production bug:
  crash report, app version, device model, OS, logs, traces, reproduction path
```

---

## 3. Dev Menu And DevTools

Dev Menu gives quick access to development features:
- reload
- open DevTools
- performance monitor
- app-specific dev options if configured

React Native DevTools helps inspect:
- console logs
- JavaScript runtime
- component tree
- props/state
- profiler

Important:
Debugging tools such as Dev Menu, LogBox, and React Native DevTools are development tools and are disabled in release builds.

---

## 4. LogBox

LogBox shows development warnings and errors.

Use it to:
- notice deprecations
- catch runtime warnings
- detect syntax/fatal errors
- triage noisy third-party warnings

Do not:
- ignore all logs permanently
- hide warnings you do not understand
- treat LogBox absence as proof the app is production-safe

Rule:
If you suppress a warning, add a short reason in code review.

---

## 5. Performance Monitor

Performance Monitor gives a quick guide for:
- JS FPS
- UI FPS

Use it for first-pass diagnosis:
- JS FPS low: inspect render, JS work, logs, list rows, selectors.
- UI FPS low: inspect native drawing, shadows, images, layout, animations.

Production rule:
Use native profiling tools and release builds for accurate performance decisions.

---

## 6. Native Logs

Android:

```text
Android Studio Logcat
adb logcat
Gradle build logs
ANR traces
Play Console crash/ANR reports
```

iOS:

```text
Xcode console
Device logs
Instruments
Crash reports
App Store Connect diagnostics
```

Use native logs when:
- app crashes before JS starts
- native module fails
- permissions behave unexpectedly
- build/signing fails
- release build differs from dev
- app is killed by OS

---

## 7. Release-Build Debugging

Release builds differ from debug builds:
- dev menu disabled
- JS is bundled locally
- minification may apply
- environment config differs
- Android shrinking/obfuscation may apply
- native signing/provisioning changes
- logs may be stripped or routed differently

Release debug workflow:

```text
1. Reproduce with local release build.
2. Capture native logs.
3. Confirm environment config.
4. Confirm bundled JS/assets exist.
5. Check source maps/native symbols.
6. Compare debug vs release dependencies and flags.
7. Bisect recent changes if needed.
```

---

## 8. Source Maps And Symbolication

Why source maps matter:
Minified JavaScript stack traces are hard to read without source maps.

Release checklist:
- generate source maps
- upload source maps to crash reporting provider
- tag source maps by app version/build/channel
- upload native symbols where required
- verify a test crash resolves to readable stack frames

Interview answer:

```text
For production crashes, I need app version, build number, source maps, native
symbols, device model, OS version, and breadcrumbs. Without source maps and
symbols, crash reports often point to minified or native addresses instead of
actionable code.
```

---

## 9. Debugging Common Incidents

### White Screen On Launch

Check:
- JS bundle failed to load.
- root component registration mismatch.
- fatal JS exception before first screen.
- native module init failure.
- bad environment config.
- app waiting forever for auth/config.

### Works In Debug, Fails In Release

Check:
- minification assumptions
- missing ProGuard/R8 keep rules
- wrong release API URL
- signing/capability differences
- missing bundled assets
- feature flag environment mismatch
- native permission/config missing

### Native Module Undefined

Check:
- package installed
- pods installed
- app rebuilt after native dependency change
- autolinking success
- platform support
- new architecture compatibility
- Expo config plugin/dev client requirement

---

## 10. Debugging Discipline

Good debugging loop:

```text
symptom -> reproduction -> evidence -> hypothesis -> smallest change -> verify -> prevent
```

Bad loop:

```text
symptom -> random dependency upgrade -> cache clear -> random workaround
```

Cache clearing can help local environment issues, but it is not a root-cause analysis.

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Only debugging in dev mode | Release can behave differently | Reproduce release build |
| No source maps | Production JS stacks unreadable | Upload per release |
| Ignoring native logs | Misses platform root cause | Use Xcode/Logcat |
| Suppressing LogBox blindly | Hides real issues | Explain and track suppressions |
| Assuming JS crash only | Native init can fail first | Check native stack/logs |
| "Fixed" by clearing cache only | Root cause unknown | Find durable cause |

---

## 12. Strong Interview Answer

Question:
A React Native app crashes only in production. How do you debug it?

Strong answer:

```text
I start from the crash report with app version, build number, device model, OS,
breadcrumbs, source-mapped JS stack, and native symbols. Then I reproduce locally
with a release build, not debug. I check native logs through Xcode or Logcat,
verify environment config, bundled assets, native dependency setup, and whether
minification or Android shrinker rules changed behavior. Once fixed, I add a
regression test or release checklist item and verify source maps work for future
crashes.
```

---

## 13. Revision Notes

- One-line summary: Debug RN across JS, React, native logs, release config, and crash tooling.
- Three keywords: DevTools, Logcat/Xcode, source maps.
- One interview trap: Dev tools are disabled in release builds.
- One memory trick: Release-only crash means compare runtime, bundle, config, and native build.

