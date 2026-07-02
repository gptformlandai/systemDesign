# React Native Visual Testing, Device Farms, And Performance Gates - Gold Sheet

> Track Module - Group 4: Senior / MAANG Path
> Level: production quality gates across devices

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Visual regression | High | UI can break without logic failing |
| Storybook/component catalog | Medium-high | Scales design-system validation |
| Device farms | High | Mobile fragmentation is real |
| Release-build smoke tests | Very high | Debug builds hide performance/release issues |
| Accessibility automation | High | Prevents regressions in critical flows |
| Performance regression gates | High | Keeps startup/list/navigation regressions out |

MAANG signal:
You know mobile quality is not only unit tests. It is UI, devices, performance, accessibility, and release-build confidence.

---

## 2. Mental Model

Testing layers:

```text
static:
  TypeScript, lint, dependency policy

unit:
  pure functions, API mappers, sync queues

component:
  screen states, forms, accessibility labels

visual:
  screenshots, Storybook, design-system variants

E2E:
  real app flows through navigation and storage

device/release:
  real OS/device matrix, release build, performance gates
```

Each layer catches a different class of bug.

---

## 3. Visual Regression

Visual tests catch:
- clipped text
- RTL layout breaks
- dark mode issues
- dynamic text overflow
- missing assets
- bad loading/empty/error states
- accidental design-system changes

Screenshot matrix:

```text
platform:
  iOS, Android

theme:
  light, dark

font:
  normal, large accessibility text

locale:
  English, long translated strings, RTL

state:
  loading, empty, error, populated, offline
```

Do not screenshot only happy-path populated screens.

---

## 4. Storybook / Component Catalog

Use a component catalog for:
- buttons
- inputs
- cards/list rows
- banners/toasts
- dialogs
- skeletons
- empty/error states
- design-system tokens

Story states:

```text
Default
Pressed
Disabled
Loading
Error
Long text
RTL
Large font
Dark mode
```

Benefit:
Design-system review happens before a full app flow is built.

---

## 5. Device Farms

Use device farms for:
- supported OS matrix
- low-memory Android devices
- OEM-specific Android behavior
- release-build smoke tests
- permission/device API flows
- app startup/performance checks

Options commonly used:
- Firebase Test Lab
- BrowserStack/App Automate
- AWS Device Farm
- internal device lab

Matrix design:

```text
Tier 1:
  latest iOS, latest Android, one low-end Android

Tier 2:
  oldest supported iOS, oldest supported Android, tablet if supported

Tier 3:
  OEM/problem devices selected from crash telemetry
```

---

## 6. Release-Build Smoke

Smoke flow:

```text
install release build
launch app
login
open main list
open detail
perform critical mutation
background/foreground app
force close/reopen
logout
```

For device APIs:
- permission grant path
- permission denial path
- settings/manual recovery path

Release-only issues:
- minification
- missing source maps
- native symbol problems
- ProGuard/R8 issues
- bundle loading
- signing/capability mismatch
- network security config differences

---

## 7. Performance Gates

Track in CI or pre-release:
- cold start time
- warm start time
- first meaningful screen
- critical screen ready time
- list scroll health
- memory after long session
- bundle size
- app size
- JS error rate in smoke

Example thresholds:

```text
cold start p75 must not regress by more than 10 percent
task list ready p75 must stay below 1500 ms on tier-1 devices
app binary size must not grow by more than agreed budget
```

Use relative and absolute gates. Some regressions are small percentages but bad UX.

---

## 8. Accessibility Automation

Automate:
- accessibility labels on critical controls
- tap target size where possible
- screen reader traversal for core flows
- dynamic text rendering
- color contrast in design review
- focus after navigation/dialogs

Manual checks still matter:
- VoiceOver/TalkBack on real device
- keyboard and switch control if supported
- long translation and RTL review

---

## 9. CI Strategy

Pull request:

```text
typecheck
lint
unit tests
component tests
changed visual snapshots for design-system components
```

Nightly:

```text
E2E on device farm
visual matrix
performance smoke
accessibility smoke
```

Release candidate:

```text
signed release build
device farm matrix
critical flow E2E
startup/list performance gates
source maps/symbols verified
```

---

## 10. Flake Management

Flaky tests are production debt.

Reduce flake by:
- stable test IDs
- deterministic mock data
- waiting on user-visible state
- avoiding arbitrary sleeps
- isolating network dependencies
- clearing app storage between runs
- retrying only at the job level with tracking

Rule:
Do not ignore flaky mobile tests forever. Quarantine with owner and expiry date.

---

## 11. Strong Interview Answer

```text
For React Native quality gates I would combine static checks, unit/component
tests, visual regression, E2E, release-build smoke, and device-farm coverage.
Visual tests protect layout states like dark mode, RTL, large text, and empty or
error states. Device farms protect us from OS and hardware fragmentation. Before
release I would gate startup, key screen readiness, crash-free smoke, source maps,
and native symbols, then monitor production telemetry after staged rollout.
```

---

## 12. Revision Notes

- One-line summary: Senior RN quality includes visuals, devices, release builds, accessibility, and performance.
- Three keywords: screenshots, device farm, release smoke.
- One interview trap: Saying Jest alone gives mobile release confidence.
- Memory trick: Logic tests say it works; device tests say it survives.
