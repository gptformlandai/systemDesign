# React Native Testing And Quality Gates - Gold Sheet

> Track File #11 of 20 - Group 4: Senior MAANG
> Level: testing strategy for production mobile teams

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| TypeScript/static analysis | Very high | First quality gate |
| Unit tests | Very high | Fast feedback for logic |
| Component tests | Very high | User-facing behavior |
| Mocking native modules | High | Common RN testing challenge |
| Integration tests | High | API/state/navigation interaction |
| E2E tests | High | Real-device confidence |
| Snapshot tests | Medium | Useful but often abused |
| CI quality gates | High | Senior production signal |

MAANG signal:
You test business logic, UI behavior, navigation flows, and device-critical paths without making every test slow and flaky.

---

## 2. Testing Pyramid For React Native

```text
Static checks:
  TypeScript, ESLint, formatting

Unit tests:
  pure functions, hooks, services, validators

Component tests:
  render behavior and user interactions

Integration tests:
  screen + store/query cache + mocked API + navigation

E2E tests:
  real app on simulator/device for critical flows
```

Rule:
Use the cheapest test that catches the bug reliably.

---

## 3. Static Analysis

Quality gates:
- TypeScript compile.
- ESLint.
- Prettier or formatter.
- dependency audit.
- unused exports/dead code checks where available.
- bundle size warnings for large apps.

Why:
Many mobile bugs are preventable before running the app: wrong route params, missing fields, invalid component props, unsafe optional access.

---

## 4. Unit Tests

Good unit-test targets:
- validators
- mappers from DTO to domain
- price/date formatting
- reducers
- retry/backoff logic
- offline queue logic
- analytics event builders

Example:

```ts
describe('toProduct', () => {
  it('maps API snake_case fields to app camelCase fields', () => {
    expect(toProduct({id: 'p1', name: 'Bag', price_cents: 1299})).toEqual({
      id: 'p1',
      name: 'Bag',
      priceCents: 1299,
    });
  });
});
```

---

## 5. Component Tests

Test from user perspective.

Good assertions:
- text visible
- button disabled/enabled
- error shown
- loading state shown
- callback fired after press
- accessibility label/role available

Avoid:
- asserting internal state
- testing implementation details
- giant snapshots

Example:

```tsx
it('submits email after user enters it', () => {
  const onSubmit = jest.fn();
  const screen = render(<EmailForm onSubmit={onSubmit} />);

  fireEvent.changeText(screen.getByPlaceholderText('Email'), 'a@example.com');
  fireEvent.press(screen.getByRole('button', {name: 'Continue'}));

  expect(onSubmit).toHaveBeenCalledWith('a@example.com');
});
```

---

## 6. Mocking Native Modules

Native modules may not exist in Node test environment.

Examples:
- camera
- secure storage
- push notifications
- device info
- native animation libraries

Pattern:

```ts
jest.mock('../secureTokenStore', () => ({
  tokenStore: {
    get: jest.fn(),
    set: jest.fn(),
    clear: jest.fn(),
  },
}));
```

Production judgment:
Mock at your app boundary, not deep inside third-party internals, when possible.

---

## 7. Integration Tests

Useful integration test:

```text
SearchScreen
  mocked API
  query client/store provider
  user types
  loading appears
  results appear
  empty state appears for no results
```

This catches:
- API hook wiring
- loading/error states
- render behavior
- stale result handling

Keep integration tests fewer and meaningful.

---

## 8. E2E Tests

Tools commonly used:
- Detox
- Maestro
- Appium

Best E2E candidates:
- login/logout
- onboarding
- checkout/payment happy path
- critical deep link
- push notification open
- offline mode for key feature
- app upgrade smoke test

Trade-offs:
- high confidence
- slower
- more flaky
- needs simulator/device infrastructure
- harder to debug

Run E2E:
- before release
- on nightly builds
- on critical PRs if infrastructure allows

---

## 9. Snapshot Tests

Snapshots can help for small stable components.

Avoid:
- huge screen snapshots
- blindly updating snapshots
- treating snapshots as proof of behavior

Better:
Prefer explicit assertions for user-visible behavior.

---

## 10. CI Quality Gates

Recommended PR gates:

```text
1. install dependencies with lockfile
2. TypeScript check
3. lint
4. unit tests
5. component tests
6. build Android debug/release candidate where feasible
7. build iOS on protected/release branches
8. E2E smoke on release branch
9. upload source maps for release builds
```

Mobile-specific gates:
- Android build.
- iOS build.
- signing validation.
- version/build number check.
- crash source maps.
- dependency license/security review.

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Only snapshot tests | Low behavior confidence | Test user actions |
| Testing internal state | Brittle | Test visible output |
| Mocking everything | Integration bugs escape | Use real modules where cheap |
| No E2E for checkout/login | Critical flow risk | Add small E2E smoke suite |
| CI only runs JS tests | Native build can break | Build platforms in CI |
| No source maps | Crash traces are unreadable | Upload per release |

---

## 12. Strong Interview Answer

Question:
What is your testing strategy for a React Native app?

Strong answer:

```text
I start with static checks: TypeScript and lint. Then I unit test pure business
logic, API mappers, validators, reducers, and offline/retry logic. For components,
I use user-centric tests that interact with text inputs and buttons instead of
asserting internal state. I add integration tests for screens with mocked APIs and
providers. For high-value flows like login, checkout, and critical deep links, I
use E2E tests on simulator/device. CI should also build iOS and Android and upload
source maps for release crash debugging.
```

---

## 13. Revision Notes

- One-line summary: Test logic cheaply, UI behavior realistically, and critical flows end-to-end.
- Three keywords: TypeScript, component tests, E2E.
- One interview trap: Snapshots are not behavior tests.
- One memory trick: Fast tests for breadth, E2E for critical confidence.

