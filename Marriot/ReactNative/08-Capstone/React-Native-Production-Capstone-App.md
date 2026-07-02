# React Native Production Capstone App

> Track Module - Group 8: Production Capstone
> Level: final beginner-to-pro integration project

---

## 1. Capstone Goal

Build one production-style React Native app that proves you can connect the whole track:

```text
React fundamentals
  -> navigation
  -> API layer
  -> auth
  -> device APIs
  -> offline sync
  -> push/background
  -> release
  -> observability
  -> testing
  -> accessibility
```

Recommended product:
Field service task app.

Why:
It naturally includes auth, lists, maps/location, camera uploads, offline work, sync conflict, push notifications, and operational dashboards.

---

## 2. Product Requirements

User can:
- sign in and sign out
- view assigned tasks
- search and filter tasks
- open task details
- mark task started/completed
- add notes while offline
- capture/upload a photo
- see sync pending/synced/failed states
- receive push notification for new urgent task
- open task from deep link/notification
- use the app with accessible labels and dynamic text

Admin/backend simulation can be mocked.

---

## 3. Required Architecture

```text
app shell:
  navigation, auth guard, deep links

features:
  tasks, task details, notes, uploads, settings

services:
  api client, auth session, offline queue, telemetry, feature flags

storage:
  secure token storage, local task database, file cache

platform:
  permissions, camera, location, push notification registration
```

Expected folders:

```text
src/
  app/
  features/tasks/
  features/uploads/
  services/api/
  services/auth/
  services/offline/
  services/telemetry/
  storage/
  platform/
  components/
  theme/
```

---

## 4. Technical Requirements

Use:
- TypeScript strict mode.
- React Navigation or Expo Router.
- TanStack Query or equivalent for server state.
- Zustand/Context only where appropriate.
- React Hook Form or controlled form strategy.
- Secure storage for tokens.
- Local durable storage for offline task/notes state.
- Development build, not Expo Go, if custom native capability is needed.
- EAS-style release thinking even if not actually submitting to stores.

Do not:
- put all state in Redux by default
- call APIs directly from components everywhere
- store tokens in normal AsyncStorage
- hide offline failures
- log PII
- ship untyped navigation params

---

## 5. Offline Requirements

Offline mode must support:
- cached task list
- cached task details
- creating local note
- updating task status locally
- pending sync banner/badge
- durable mutation queue
- retry with backoff
- idempotency key per mutation
- conflict state when backend version changed

Conflict example:

```text
Device edits task notes at version 4.
Server already has version 6.
App shows "Needs review" and lets user keep local, accept server, or merge.
```

---

## 6. Device API Requirements

At least two:
- camera/photo upload
- location check-in
- push notification deep link
- file/document upload
- biometric unlock

For each:
- permission state machine
- denial fallback
- real-device test notes
- telemetry events
- privacy-safe logging

---

## 7. Observability Requirements

Track:
- app start
- login success/failure
- task list ready time
- task detail ready time
- offline queue length
- sync success/failure
- photo upload success/failure
- permission denied
- crash/fatal JS error path
- app version/build/update metadata

Dashboard questions:
- Did the latest release increase crash rate?
- Are uploads failing on one OS version?
- Is offline queue age growing?
- Is startup slower on low-memory Android devices?

---

## 8. Testing Requirements

Minimum:
- TypeScript passes.
- Lint passes.
- API mapping unit tests.
- offline queue unit tests.
- navigation/auth guard test.
- task list component test.
- one E2E smoke for login -> task list -> detail.
- accessibility labels for critical controls.

Advanced:
- screenshot/visual regression for key screens.
- device-farm smoke on at least one iOS and one Android target.
- release-build smoke test.

---

## 9. Release Requirements

Prepare:
- app version/build number policy
- dev/preview/prod environment config
- OTA vs binary decision table
- source map upload plan
- native symbol upload plan
- privacy/data inventory
- permission strings
- app store metadata draft
- staged rollout and rollback plan

Even if the capstone is local-only, write the release plan as if it will go to TestFlight and Google Play internal testing.

---

## 10. Milestones

### Milestone 1 - Foundation

Deliver:
- app created
- navigation shell
- auth mock
- task list/detail from mock API
- typed routes

Pass:
User can sign in and view task details.

### Milestone 2 - Production Data

Deliver:
- API client boundary
- TanStack Query cache
- loading/error/empty states
- forms and validation

Pass:
Network failures show useful recovery states.

### Milestone 3 - Offline

Deliver:
- local task DB/cache
- offline mutation queue
- sync status UI
- idempotent replay simulation

Pass:
User can edit offline and see sync complete after reconnect.

### Milestone 4 - Device

Deliver:
- camera/location/push or equivalent
- permission flow
- fallback path

Pass:
Permission denial does not break the app.

### Milestone 5 - Quality And Release

Deliver:
- tests
- telemetry
- release checklist
- privacy inventory
- performance pass

Pass:
You can explain the app architecture in a senior interview.

---

## 11. Scoring Rubric

| Score | Meaning |
|---:|---|
| 1 | Screens work only in happy path |
| 2 | Handles loading/error and typed navigation |
| 3 | Has clean architecture, tests, and API boundary |
| 4 | Offline, device APIs, telemetry, and release plan are solid |
| 5 | Senior-level: conflict handling, observability, compliance, performance, and rollout are complete |

---

## 12. Final Interview Prompt

> You built this React Native field service app. Explain the architecture, offline sync design, device API handling, release strategy, and the top three production risks.

Strong answer outline:

```text
1. App architecture and module boundaries.
2. Navigation/auth/deep link flow.
3. Server state and local database read path.
4. Offline mutation queue with idempotency.
5. Device permissions and fallbacks.
6. Observability and SLOs.
7. Release/OTA/runtimeVersion strategy.
8. Security and privacy considerations.
9. Trade-offs and future improvements.
```

---

## 13. Revision Notes

- One-line summary: The capstone proves React Native mastery by integrating product, native, offline, release, and operations.
- Three keywords: integrate, operate, explain.
- One interview trap: Building screens without production behaviors.
- Memory trick: If it cannot survive offline, release, and debugging, it is not pro-level yet.
