# React Native Enterprise Monorepo And Release Governance - Gold Sheet

> Track Module - Group 6: Gold-Level Completeness
> Level: large-team React Native ownership

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Monorepo boundaries | High | Large teams share code without chaos |
| Metro/workspace behavior | High | RN resolution differs from web bundlers |
| Shared design system | High | Prevents inconsistent product UI |
| Native dependency governance | Very high | One bad native package can break all apps |
| Release trains | High | Mobile rollback is slower than web |
| Code ownership | Medium-high | Scales review and accountability |

MAANG signal:
You can run React Native as a platform inside an organization, not just as one app repo.

---

## 2. Mental Model

Enterprise mobile has two systems:

```text
Product app system:
  screens, features, business flows, API clients

Platform system:
  shared packages, design system, native dependency policy,
  release process, observability, CI/CD, ownership
```

Monorepo success depends on clear boundaries, not only tooling.

---

## 3. Common Monorepo Shape

```text
apps/
  consumer-mobile/
  driver-mobile/
packages/
  ui/
  theme/
  api-client/
  auth/
  telemetry/
  feature-flags/
  eslint-config/
  tsconfig/
  test-utils/
```

Rules:
- App packages own app identity, navigation shell, release config.
- Feature packages own business modules.
- Platform packages own cross-cutting tools.
- UI package owns primitives, not every screen.
- Native modules must be approved centrally.

---

## 4. Package Boundaries

Good dependency direction:

```text
app -> feature -> domain/api -> platform utilities
app -> ui/theme
feature -> ui/theme
```

Bad:

```text
ui -> app
api-client -> screens
shared package -> app-specific navigation
feature A -> feature B internals
```

Use public exports:

```ts
// packages/auth/src/index.ts
export {AuthProvider} from './AuthProvider';
export {useAuthSession} from './useAuthSession';
export type {AuthSession} from './types';
```

Do not import from another package's private files.

---

## 5. Metro And Workspaces

React Native bundling must resolve:
- source files outside the app folder
- single React version
- single React Native version
- platform-specific files
- assets from shared packages

Modern Expo handles common monorepo setups better than older versions, but teams still need rules.

Checklist:
- one package manager
- one lockfile
- one React version
- one React Native version per app runtime
- avoid duplicate native module installs
- verify shared package transpilation
- test Metro after moving packages

Customize Metro only when the default setup cannot resolve workspace needs.

---

## 6. Native Dependency Governance

Native dependencies are high-risk because they affect:
- app startup
- store builds
- permissions
- privacy declarations
- app size
- New Architecture compatibility
- OTA compatibility
- iOS/Android parity

Approval checklist:

```text
[ ] Maintained package
[ ] Expo/New Architecture compatibility checked
[ ] iOS and Android behavior tested
[ ] Permission/privacy impact documented
[ ] App size impact checked
[ ] Release-build smoke tested
[ ] Rollback plan exists
[ ] Owner assigned
```

Rule:
JavaScript packages can be owned by feature teams. Native packages need platform review.

---

## 7. Design System Governance

Design-system package should include:
- theme tokens
- spacing scale
- color modes
- typography scale
- accessible primitives
- form controls
- loading/error/empty states
- icons
- test helpers

Avoid:
- business logic in UI primitives
- app-specific copy in shared components
- breaking visual changes without migration plan
- inaccessible custom controls

Versioning:
Use changesets or another release-note mechanism so app teams know what changed.

---

## 8. Release Trains

Release train model:

```text
main:
  ongoing development

release/x.y:
  stabilization, QA, store submission

hotfix/x.y.z:
  critical fix only
```

Mobile release needs:
- app version/build number
- runtimeVersion/channel mapping
- feature flag state
- backend compatibility window
- source maps/symbols
- staged rollout plan
- rollback/kill switch path

Release ownership:
- release captain
- QA owner
- platform owner
- backend compatibility owner
- product signoff

---

## 9. CI For Monorepos

Pull request checks:

```text
affected package calculation
typecheck affected packages
lint affected packages
unit/component tests
app-level smoke if mobile package changed
native build if native dependency changed
dependency policy check
```

Release checks:

```text
clean install from lockfile
Expo doctor or RN doctor
iOS release build
Android release build
E2E smoke
source maps/symbol upload
metadata validation
```

---

## 10. Code Ownership

Use ownership for:
- app shell
- navigation
- auth/session
- telemetry/logging
- design system
- native dependencies
- release config
- critical flows

Review policy:
- feature changes reviewed by feature owner
- shared package changes reviewed by platform owner
- native dependency changes reviewed by mobile platform
- release pipeline changes reviewed by release owner

---

## 11. Failure Modes

| Failure | Cause | Prevention |
|---|---|---|
| Duplicate React | workspace resolution issue | dependency constraints |
| App build breaks after package upgrade | native dependency drift | platform approval and native CI |
| OTA crashes old binary | runtime/channel mismatch | runtimeVersion discipline |
| Design system blocks teams | too centralized | clear extension points |
| Monorepo CI too slow | all checks always run | affected checks plus release gates |
| Store rejection | metadata/privacy not owned | compliance checklist |

---

## 12. Strong Interview Answer

```text
For enterprise React Native I separate product app ownership from mobile platform
ownership. Apps own navigation, app identity, and release config; shared packages
own UI primitives, API clients, auth, telemetry, and feature flags. I keep strict
package boundaries, one lockfile, one React/RN version policy per app runtime, and
platform review for native dependencies. Releases run on trains with source maps,
symbols, runtimeVersion/channel discipline, staged rollout, and clear owners.
```

---

## 13. Revision Notes

- One-line summary: Enterprise RN needs boundaries, governance, and release discipline.
- Three keywords: monorepo, native governance, release train.
- One interview trap: Treating monorepo as only a folder structure.
- Memory trick: Apps own product; platform owns shared risk.
