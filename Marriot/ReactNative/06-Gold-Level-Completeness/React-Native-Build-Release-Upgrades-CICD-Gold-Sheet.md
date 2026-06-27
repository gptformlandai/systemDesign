# React Native Build, Release, Upgrades, And CI/CD - Gold Sheet

> Track File #19 of 20 - Group 6: Gold-Level Completeness
> Level: mobile delivery ownership for senior engineers

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Expo EAS vs native builds | High | Modern RN delivery decision |
| iOS release/archive | High | App Store delivery knowledge |
| Android AAB/signing | High | Play Store delivery knowledge |
| Version/build numbers | Very high | Release tracking and upgrades |
| CI/CD | Very high | Team-scale delivery |
| OTA update boundaries | High | Fast fixes with compatibility risk |
| RN upgrades | High | Ongoing maintenance reality |
| Native dependency hygiene | High | Build failures and security risk |

MAANG signal:
You understand the difference between shipping JavaScript and shipping a native binary.

---

## 2. Mental Model

React Native delivery has two artifact types:

```text
Native binary:
  iOS archive / Android AAB or APK
  includes native code, permissions, entitlements, bundled JS/assets
  distributed through app stores or internal distribution

OTA update:
  JS bundle/assets update compatible with installed native binary
  distributed through update service
  cannot add missing native code or permissions
```

This distinction controls release risk.

---

## 3. Expo EAS vs Bare Native Release

### Expo EAS

Good for:
- managed Expo apps
- cloud builds
- credential management
- app store submission
- OTA updates
- consistent team workflow

### Bare Native Release

Good for:
- custom native build pipelines
- existing iOS/Android teams
- unusual signing/security constraints
- deep native customization
- enterprise-specific distribution

Interview answer:

```text
Expo EAS standardizes build, submit, and update workflows. Bare native release
gives maximum control over Xcode, Gradle, signing, and native customization. I
choose based on native complexity, compliance constraints, and team ownership.
```

---

## 4. iOS Release Essentials

Know these terms:
- Bundle Identifier
- provisioning profile
- signing certificate
- App Store Connect
- TestFlight
- Archive
- Release scheme
- entitlements/capabilities

Release flow:

```text
1. Set version and build number.
2. Ensure signing/capabilities are correct.
3. Build with Release configuration.
4. Archive in Xcode or CI.
5. Upload to App Store Connect.
6. Test through TestFlight.
7. Submit for review.
8. Monitor rollout/crashes.
```

Important:
Release builds disable development tooling and bundle JavaScript locally.

---

## 5. Android Release Essentials

Know these terms:
- applicationId
- versionCode
- versionName
- upload key
- app signing by Google Play
- AAB
- APK
- Gradle build types
- ProGuard/R8

Release flow:

```text
1. Set versionName and versionCode.
2. Configure signing securely.
3. Build release AAB.
4. Test release build on device.
5. Upload to Play Console.
6. Use internal/closed/open tracks.
7. Stage rollout.
8. Monitor crash/ANR metrics.
```

Security note:
Do not commit keystore passwords or release signing secrets.

---

## 6. Versioning Strategy

Track:
- marketing version: `1.8.0`
- build number/version code: monotonically increasing
- OTA channel/update group
- backend API compatibility range
- minimum supported app version

Example policy:

```text
1.8.0 build 184:
  native binary release
  supports API v3
  OTA channel production
  minimum backend-compatible app 1.6.0
```

Production value:
Every crash, telemetry event, API error, and feature flag evaluation should include app version and build.

---

## 7. CI/CD Pipeline

Recommended pipeline:

```text
Pull request:
  install with lockfile
  TypeScript
  lint
  unit/component tests
  affected package checks

Merge to main:
  build Android debug/release candidate
  build iOS if runner available
  upload artifacts

Release branch/tag:
  build signed iOS/Android
  run E2E smoke
  upload source maps/symbols
  submit to store/internal track
  create release notes
  monitor rollout
```

Quality gates:
- lockfile integrity
- native build success
- source map upload
- environment config verification
- version/build number uniqueness
- E2E smoke for critical flows

---

## 8. OTA Update Rules

Safe candidates:
- JS bug fix
- copy/text changes
- small UI behavior
- feature flag adjustment
- non-native asset update

Unsafe candidates:
- new native module
- new permission/entitlement
- native dependency change
- storage migration without compatibility
- code assuming a newer native binary

Golden rule:
OTA must be compatible with every native binary allowed to receive it.

---

## 9. React Native Upgrades

Upgrade work includes:
- React Native version change
- React version compatibility
- Gradle/Android plugin changes
- Xcode/iOS deployment changes
- CocoaPods updates
- native dependency compatibility
- New Architecture compatibility
- Hermes/runtime changes
- test/build pipeline updates

Upgrade strategy:

```text
1. Read release notes and breaking changes.
2. Create upgrade branch.
3. Upgrade one layer at a time.
4. Fix native build issues first.
5. Run JS tests and app smoke.
6. Run E2E critical flows.
7. Test release builds on both platforms.
8. Roll out gradually.
```

Interview maturity:
Plan RN upgrades as engineering work, not casual package bumps.

---

## 10. Native Dependency Hygiene

Checklist:
- actively maintained
- supports current RN version
- supports New Architecture if needed
- supports both platforms or documented one-platform behavior
- has Expo config plugin if using Expo prebuild
- has acceptable license
- does not add excessive permissions
- does not bloat app size unnecessarily
- has fallback/replacement plan

Avoid:
- adding native libraries for tiny JS-solvable problems
- ignoring transitive dependency risk
- upgrading many native dependencies in one PR without isolation

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Treating mobile release like web deploy | Store review and slow user updates | Use staged rollout and compatibility |
| OTA changes native assumptions | Crashes old binaries | Enforce binary compatibility |
| No release build testing | Debug hides release issues | Test signed release builds |
| Committing signing secrets | Credential leak | Use secure CI secrets/credential store |
| Random RN upgrades | Breaks native build | Planned upgrade branch |
| No version in telemetry | Hard to isolate bad release | Include version/build everywhere |

---

## 12. Strong Interview Answer

Question:
How do you safely release a React Native app?

Strong answer:

```text
I separate native binary releases from OTA updates. Native changes, permissions,
entitlements, and dependency changes require a store binary release. JS-only fixes
can use OTA if they remain compatible with installed binaries. CI runs TypeScript,
lint, tests, native builds, and E2E smoke for critical flows. For release, I upload
source maps and native symbols, submit through TestFlight/Play tracks or EAS, use
staged rollout, monitor crash-free users and ANRs, and keep backend APIs compatible
with older app versions.
```

---

## 13. Revision Notes

- One-line summary: Mobile delivery is native binary release plus compatible OTA updates plus staged monitoring.
- Three keywords: AAB/archive, source maps, compatibility.
- One interview trap: OTA cannot add missing native code.
- One memory trick: Web deploy updates everyone; mobile release meets old clients for weeks or months.

