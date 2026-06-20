# JavaScript Testing Patterns Master Sheet

Target: JavaScript, TypeScript, frontend, Node.js, full-stack, platform, and MAANG interviews where you must explain test strategy, write reliable tests, choose the right test level, and prevent production regressions.

This sheet covers:
- Testing mental model
- Test pyramid and test trophy
- Unit, integration, component, E2E, contract, and smoke tests
- Jest and Vitest fundamentals
- Arrange-Act-Assert pattern
- Matchers and assertions
- Mocks, stubs, spies, fakes, and test doubles
- Fake timers and async testing
- DOM and component testing
- React Testing Library-style principles
- API and Node.js integration tests
- Supertest-style HTTP tests
- MSW/network mocking patterns
- Database testing and transaction cleanup
- Testcontainers-style integration tests
- Playwright E2E testing
- Contract tests and schema tests
- Accessibility, visual, and performance tests
- Flaky test diagnosis
- CI quality gates
- Production regression strategy
- Strong interview answers and MAANG scenarios

How to use this:
- Learn test intent before tool syntax.
- For every feature, ask what confidence is needed and what failure would hurt users.
- Prefer tests that verify behavior, not implementation details.
- Use the smallest test level that gives strong confidence.
- Make tests deterministic, readable, isolated, and fast enough for CI.

---

## 1. Testing In One Line

Definition:

```text
Testing is executable evidence that code behaves correctly under expected, edge, and failure conditions.
```

Interview line:

```text
Good tests are not just coverage numbers. They protect behavior, document intent, enable refactoring,
and catch regressions before users do.
```

Production mindset:

```text
A test suite is a safety system. It should be fast, trustworthy, maintainable, and aligned with
real business risk.
```

---

## 2. Testing Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Unit tests | Very high | Fast feedback for logic |
| Integration tests | Very high | Catch wiring and dependency bugs |
| Component tests | Very high | Frontend user behavior confidence |
| E2E tests | High | Validate critical workflows |
| Contract tests | High | Prevent frontend/backend breakage |
| Mocking strategy | Very high | Avoid brittle or fake confidence |
| Async tests | Very high | Common JS source of bugs |
| Fake timers | High | Debounce, retry, timeout, interval tests |
| API tests | Very high | Backend route behavior |
| Database tests | High | Real query/schema behavior |
| Accessibility tests | High | Production UI quality |
| Flaky test control | Very high | CI trust |
| Test data builders | High | Maintainable fixtures |
| Coverage | Medium-high | Useful signal, not the goal |
| Mutation testing | Medium | Strong test quality signal |
| Visual regression | Medium | UI layout confidence |
| Performance regression | Medium-high | Protect latency and bundles |
| Security tests | High | Abuse-case regression prevention |

---

## 3. Test Strategy Mental Model

Use this question chain:

```text
1. What behavior matters to the user or system?
2. What can break?
3. What level catches it fastest and most reliably?
4. What dependencies should be real vs mocked?
5. What edge/failure cases are worth locking down?
6. How will this run in CI without flakiness?
```

Strong line:

```text
I choose the smallest reliable test that proves the behavior. Unit tests are great for pure logic,
integration tests for boundaries, and E2E tests for critical user journeys.
```

---

## 4. Test Pyramid And Test Trophy

Classic test pyramid:

```text
Many unit tests
Fewer integration tests
Few E2E tests
```

Frontend test trophy idea:

```text
Static checks + unit tests + many integration/component tests + fewer E2E tests
```

Why not too many E2E tests:

- Slower.
- More flaky.
- Harder to debug.
- Expensive in CI.
- Often duplicate lower-level coverage.

Why not only unit tests:

- Miss wiring bugs.
- Miss real browser behavior.
- Miss database/query issues.
- Miss API contract mismatches.

Strong answer:

```text
I do not chase one test shape for everything. I balance speed and confidence: pure logic gets unit
tests, important boundaries get integration tests, and business-critical flows get E2E coverage.
```

---

## 5. Test Types Cheat Sheet

| Test Type | What It Proves | Example |
|---|---|---|
| Static check | Code is structurally valid | TypeScript, ESLint |
| Unit | One function/module behavior | price calculation |
| Component | UI behavior in isolation | search form interaction |
| Integration | Multiple modules with real boundary | API + DB repository |
| Contract | Producer/consumer agreement | OpenAPI response shape |
| E2E | Full user journey | login -> checkout |
| Smoke | App basically works after deploy | health + critical route |
| Regression | Specific bug stays fixed | stale response no longer wins |
| Security | Abuse case blocked | XSS string rendered safely |
| Performance | Latency/bundle budget protected | API p95 under threshold |

---

## 6. Test Naming

Good test name:

```text
returns validation error when email is missing
```

Weak test name:

```text
test user service
```

Patterns:

```javascript
it("returns validation error when email is missing", () => {});
it("does not submit twice while request is pending", async () => {});
it("ignores stale search responses", async () => {});
```

Strong answer:

```text
A test name should describe behavior and condition, so failure output tells the team what broke.
```

---

## 7. Arrange Act Assert

Pattern:

```text
Arrange -> set up inputs and dependencies
Act -> execute behavior
Assert -> verify observable result
```

Example:

```javascript
import { describe, expect, it } from "vitest";

function calculateTotal(items) {
    return items.reduce((sum, item) => sum + item.priceCents, 0);
}

describe("calculateTotal", () => {
    it("returns the sum of item prices in cents", () => {
        const items = [
            { priceCents: 1000 },
            { priceCents: 250 }
        ];

        const total = calculateTotal(items);

        expect(total).toBe(1250);
    });
});
```

Strong answer:

```text
Arrange-Act-Assert keeps tests readable and prevents assertions from being hidden inside setup.
```

---

## 8. What To Assert

Assert observable behavior:

- Return value.
- Rendered text or accessible role.
- HTTP status and response body.
- Database state.
- Event published.
- Log/metric only when meaningful.
- Error thrown for invalid input.

Avoid over-asserting:

- Private helper calls.
- Internal state not part of contract.
- Exact implementation sequence unless it matters.
- Full object snapshots when only fields matter.

Strong line:

```text
Tests should fail when behavior breaks, not every time implementation changes.
```

---

## 9. Jest And Vitest Basics

Both provide:

- Test runner.
- Assertions.
- Mocks/spies.
- Fake timers.
- Watch mode.
- Coverage integration.

Vitest style:

```javascript
import { describe, expect, it, vi } from "vitest";

describe("math", () => {
    it("adds numbers", () => {
        expect(1 + 2).toBe(3);
    });
});
```

Jest style:

```javascript
describe("math", () => {
    test("adds numbers", () => {
        expect(1 + 2).toBe(3);
    });
});
```

Strong answer:

```text
Jest and Vitest solve similar problems. I choose based on project ecosystem, ESM support, speed,
framework integration, and existing team conventions.
```

---

## 10. Matchers

Common matchers:

```javascript
expect(value).toBe(1);                 // strict identity / primitive equality
expect(object).toEqual({ id: 1 });     // deep equality
expect(list).toHaveLength(2);
expect(text).toContain("hello");
expect(fn).toThrow("invalid");
expect(mock).toHaveBeenCalledWith(1);
```

Important distinction:

```javascript
expect({ a: 1 }).toBe({ a: 1 });    // fails, different references
expect({ a: 1 }).toEqual({ a: 1 }); // passes, same structure
```

Strong answer:

```text
Use matchers that express intent. toBe is for identity/primitives; toEqual is for structural equality.
```

---

## 11. Testing Pure Functions

Pure functions are easiest to test.

```javascript
function applyDiscount(priceCents, discountPercent) {
    if (discountPercent < 0 || discountPercent > 100) {
        throw new Error("invalid discount");
    }

    return Math.round(priceCents * (100 - discountPercent) / 100);
}
```

Tests:

```javascript
describe("applyDiscount", () => {
    it("applies percentage discount", () => {
        expect(applyDiscount(1000, 15)).toBe(850);
    });

    it("rejects negative discount", () => {
        expect(() => applyDiscount(1000, -1)).toThrow("invalid discount");
    });

    it("allows full discount", () => {
        expect(applyDiscount(1000, 100)).toBe(0);
    });
});
```

Strong answer:

```text
For pure business logic, unit tests should cover normal cases, boundaries, and invalid input.
```

---

## 12. Testing Edge Cases

Think in categories:

- Empty input.
- Null/undefined if allowed.
- Minimum value.
- Maximum value.
- One item.
- Many items.
- Duplicate items.
- Invalid type.
- Timezone/date boundary.
- Floating-point/money boundary.
- Concurrent/repeated action.

Example:

```javascript
describe("normalizeEmail", () => {
    it("trims and lowercases email", () => {
        expect(normalizeEmail("  A@Example.COM ")).toBe("a@example.com");
    });

    it("rejects empty email", () => {
        expect(() => normalizeEmail("   ")).toThrow("email required");
    });
});
```

---

## 13. Test Data Builders

Bad fixture:

```javascript
const user = {
    id: "u1",
    email: "a@example.com",
    role: "admin",
    createdAt: "2026-06-20T00:00:00Z",
    profile: { firstName: "A", lastName: "B" },
    settings: { theme: "dark" }
};
```

If every test copies this, changes become painful.

Builder:

```javascript
function buildUser(overrides = {}) {
    return {
        id: "u1",
        email: "user@example.com",
        role: "customer",
        tenantId: "t1",
        ...overrides
    };
}
```

Usage:

```javascript
const admin = buildUser({ role: "admin" });
```

Strong answer:

```text
Test data builders keep tests focused on the field that matters for the behavior under test.
```

---

## 14. Test Doubles Vocabulary

| Term | Meaning |
|---|---|
| Dummy | Passed but not used |
| Stub | Returns predefined data |
| Spy | Records calls |
| Mock | Pre-programmed expectation/behavior |
| Fake | Working simplified implementation |

Example stub:

```javascript
const userRepository = {
    findById: async () => ({ id: "u1", role: "admin" })
};
```

Example spy:

```javascript
const sendEmail = vi.fn();
await service.createBooking(input);
expect(sendEmail).toHaveBeenCalledWith(expect.objectContaining({ bookingId: "b1" }));
```

Strong answer:

```text
I use test doubles to isolate the behavior under test, but I avoid mocking so much that the test no longer resembles production.
```

---

## 15. Mocking Strategy

Mock when:

- Dependency is slow.
- Dependency is non-deterministic.
- Dependency costs money.
- Dependency is external/unavailable in CI.
- You need to force failure paths.

Do not mock when:

- The integration itself is what you need confidence in.
- The mock duplicates implementation details.
- A fake or local test dependency is more realistic.
- The boundary has frequent contract changes.

Strong answer:

```text
Mock external edges, not the behavior I am trying to prove. For critical integrations, I add contract or integration tests.
```

---

## 16. Spies Without Over-Coupling

Over-coupled:

```javascript
expect(repository.findById).toHaveBeenCalledTimes(1);
expect(repository.save).toHaveBeenCalledTimes(1);
expect(repository.publish).toHaveBeenCalledTimes(1);
```

Better when behavior matters:

```javascript
expect(result.status).toBe("CONFIRMED");
expect(events).toContainEqual({ type: "booking.confirmed", bookingId: "b1" });
```

But call assertions are useful when:

- Preventing duplicate email.
- Verifying idempotency.
- Ensuring no payment call on invalid input.
- Ensuring audit log/event is emitted.

Strong answer:

```text
I assert calls only when the call itself is part of the observable behavior or risk.
```

---

## 17. Async Testing Basics

Correct:

```javascript
it("loads user", async () => {
    const user = await loadUser("u1");
    expect(user.id).toBe("u1");
});
```

Wrong:

```javascript
it("loads user", () => {
    loadUser("u1").then(user => {
        expect(user.id).toBe("u1");
    });
});
```

The test may finish before assertion runs.

Testing rejection:

```javascript
await expect(loadUser("missing")).rejects.toThrow("not found");
```

Strong answer:

```text
Async tests must return or await the promise, otherwise assertions can run after the test already passed.
```

---

## 18. Testing Error Paths

Good tests include failure behavior.

```javascript
it("returns validation error when guestName is missing", async () => {
    const response = await request(app)
        .post("/bookings")
        .send({ roomId: "r1" });

    expect(response.status).toBe(400);
    expect(response.body).toEqual(expect.objectContaining({
        error: "validation_failed"
    }));
});
```

Failure paths to test:

- Invalid input.
- Unauthorized user.
- Forbidden user.
- Missing resource.
- Conflict/idempotency conflict.
- Dependency timeout.
- External API failure.
- Duplicate request.
- Empty result.

Strong answer:

```text
Production bugs often live in failure paths, so I test not only success but invalid input, auth,
dependency failure, and retry/idempotency behavior.
```

---

## 19. Fake Timers

Use for:

- Debounce.
- Throttle.
- Retry backoff.
- setTimeout.
- setInterval.
- Expiring cache.
- Token refresh.

Example debounce test:

```javascript
import { describe, expect, it, vi } from "vitest";

function debounce(fn, delayMs) {
    let timeoutId;

    return (...args) => {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn(...args), delayMs);
    };
}

describe("debounce", () => {
    it("runs once after the delay", () => {
        vi.useFakeTimers();
        const fn = vi.fn();
        const debounced = debounce(fn, 300);

        debounced("a");
        debounced("ab");
        debounced("abc");

        vi.advanceTimersByTime(299);
        expect(fn).not.toHaveBeenCalled();

        vi.advanceTimersByTime(1);
        expect(fn).toHaveBeenCalledWith("abc");

        vi.useRealTimers();
    });
});
```

Strong answer:

```text
Fake timers make time-based behavior deterministic and fast, but tests must restore real timers to avoid cross-test pollution.
```

---

## 20. Testing Retry With Backoff

Example:

```javascript
async function retry(operation, { attempts = 3, delayMs = 100 } = {}) {
    let lastError;

    for (let attempt = 1; attempt <= attempts; attempt++) {
        try {
            return await operation();
        } catch (error) {
            lastError = error;

            if (attempt < attempts) {
                await new Promise(resolve => setTimeout(resolve, delayMs));
            }
        }
    }

    throw lastError;
}
```

Test:

```javascript
it("retries until operation succeeds", async () => {
    vi.useFakeTimers();
    const operation = vi.fn()
        .mockRejectedValueOnce(new Error("temporary"))
        .mockResolvedValueOnce("ok");

    const promise = retry(operation, { attempts: 2, delayMs: 1000 });

    await vi.advanceTimersByTimeAsync(1000);

    await expect(promise).resolves.toBe("ok");
    expect(operation).toHaveBeenCalledTimes(2);

    vi.useRealTimers();
});
```

Strong answer:

```text
Retry tests should prove attempt count, delay behavior, success after transient failure, and final failure after budget is exhausted.
```

---

## 21. Testing Abort And Timeout

Function:

```javascript
async function fetchWithTimeout(fetchFn, url, timeoutMs) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    try {
        return await fetchFn(url, { signal: controller.signal });
    } finally {
        clearTimeout(timeoutId);
    }
}
```

Test idea:

```javascript
it("aborts fetch after timeout", async () => {
    vi.useFakeTimers();

    const fetchFn = vi.fn((url, options) => new Promise((resolve, reject) => {
        options.signal.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
    }));

    const promise = fetchWithTimeout(fetchFn, "/api", 1000);

    await vi.advanceTimersByTimeAsync(1000);

    await expect(promise).rejects.toThrow("Aborted");
    vi.useRealTimers();
});
```

Strong answer:

```text
Timeout tests should verify the underlying work is cancelled where possible, not only that the caller stops waiting.
```

---

## 22. Date And Time Tests

Bad:

```javascript
expect(formatToday()).toBe("2026-06-20");
```

This fails tomorrow.

Better inject clock:

```javascript
function createReportName(clock = () => new Date()) {
    return `report-${clock().toISOString().slice(0, 10)}.csv`;
}

it("uses current date in report name", () => {
    const clock = () => new Date("2026-06-20T12:00:00Z");
    expect(createReportName(clock)).toBe("report-2026-06-20.csv");
});
```

Strong answer:

```text
Time-dependent code should use injected clocks or fake timers so tests are deterministic.
```

---

## 23. Testing DOM Behavior

Prefer user-visible behavior.

Example with Testing Library style:

```javascript
render(<LoginForm onSubmit={onSubmit} />);

await user.type(screen.getByLabelText(/email/i), "a@example.com");
await user.type(screen.getByLabelText(/password/i), "secret");
await user.click(screen.getByRole("button", { name: /sign in/i }));

expect(onSubmit).toHaveBeenCalledWith({
    email: "a@example.com",
    password: "secret"
});
```

Good queries:

- `getByRole`
- `getByLabelText`
- `getByText` when appropriate
- `findBy...` for async UI

Avoid first:

- Querying by CSS class.
- Querying implementation-specific DOM structure.
- Testing private component state.

Strong answer:

```text
Component tests should interact like a user: roles, labels, text, and visible outcomes instead of internal state.
```

---

## 24. Testing Loading, Error, Empty, Success

Async UI state machine:

```text
idle -> loading -> success
idle -> loading -> error
success -> refreshing -> success/error
```

Test cases:

```javascript
it("shows empty state when no results are returned", async () => {
    server.use(searchHandlerReturning([]));

    render(<SearchPage />);
    await user.type(screen.getByRole("searchbox"), "zzzz");

    expect(await screen.findByText(/no results/i)).toBeInTheDocument();
});
```

Strong answer:

```text
For async UI, I test loading, success, empty, and error states separately because users experience them differently.
```

---

## 25. Testing Stale Search Responses

Problem:

```text
Older slow response overwrites newer search results.
```

Test idea:

```javascript
it("renders only the latest search response", async () => {
    const search = createControlledSearchMock();
    render(<SearchBox search={search} />);

    await user.type(screen.getByRole("searchbox"), "ja");
    await user.type(screen.getByRole("searchbox"), "va");

    search.resolveSecond([{ label: "java" }]);
    expect(await screen.findByText("java")).toBeInTheDocument();

    search.resolveFirst([{ label: "javascript" }]);
    expect(screen.queryByText("javascript")).not.toBeInTheDocument();
});
```

Strong answer:

```text
Race-condition tests should control promise resolution order so the old response resolves after the new response.
```

---

## 26. Network Mocking With MSW

MSW intercepts network requests at a realistic boundary.

Example concept:

```javascript
import { http, HttpResponse } from "msw";

export const handlers = [
    http.get("/api/bookings", () => {
        return HttpResponse.json([
            { id: "b1", guestName: "Ava" }
        ]);
    })
];
```

Why useful:

- Tests actual fetch/client behavior.
- Avoids mocking every API client method.
- Works for browser-like and Node-like tests.
- Can simulate latency/errors.

Strong answer:

```text
I like network-level mocks for frontend integration tests because they keep the component close to production while still deterministic.
```

---

## 27. Testing Fetch Error Semantics

Important:

```text
fetch resolves for HTTP 404/500. It rejects for network errors or aborts.
```

Test:

```javascript
it("throws for HTTP 500", async () => {
    const fetchFn = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: async () => ({ error: "server_error" })
    });

    await expect(fetchJson(fetchFn, "/api")).rejects.toThrow("HTTP 500");
});
```

Strong answer:

```text
I test HTTP error handling explicitly because fetch does not reject on 4xx or 5xx by default.
```

---

## 28. Node API Integration Tests

Use real app wiring where possible.

Supertest-style example:

```javascript
import request from "supertest";

it("creates booking", async () => {
    const response = await request(app)
        .post("/bookings")
        .send({ guestName: "Ava", roomId: "r1" });

    expect(response.status).toBe(201);
    expect(response.body).toEqual(expect.objectContaining({
        id: expect.any(String),
        guestName: "Ava",
        roomId: "r1"
    }));
});
```

What it proves:

- Routing.
- Middleware.
- Body parsing.
- Validation.
- Error handler.
- Response shape.

Strong answer:

```text
API integration tests catch wiring issues that unit tests miss: middleware order, parsing, status codes, and error shapes.
```

---

## 29. Testing Auth And Authorization

Cases:

- Missing token -> 401.
- Invalid token -> 401.
- Valid user lacking permission -> 403.
- Valid user accessing another tenant -> 404/403 by policy.
- Admin allowed -> 200/204.

Example:

```javascript
it("rejects cross-tenant booking access", async () => {
    const booking = await seedBooking({ tenantId: "t2" });
    const token = createToken({ userId: "u1", tenantId: "t1" });

    const response = await request(app)
        .get(`/bookings/${booking.id}`)
        .set("Authorization", `Bearer ${token}`);

    expect(response.status).toBe(404);
});
```

Strong answer:

```text
Security-sensitive tests should prove the API rejects unauthorized access, not only that happy-path access works.
```

---

## 30. Testing Idempotency

Scenario:

```text
Same idempotency key should not create duplicate payment/order.
```

Test:

```javascript
it("returns same result for repeated idempotency key", async () => {
    const key = "idem-123";
    const payload = { amountCents: 1000 };

    const first = await request(app)
        .post("/payments")
        .set("Idempotency-Key", key)
        .send(payload);

    const second = await request(app)
        .post("/payments")
        .set("Idempotency-Key", key)
        .send(payload);

    expect(first.status).toBe(201);
    expect(second.status).toBe(201);
    expect(second.body.id).toBe(first.body.id);
});
```

Also test:

```text
Same key with different payload -> 409 conflict.
```

Strong answer:

```text
Idempotency tests prove retry safety for non-idempotent writes like payments, bookings, and orders.
```

---

## 31. Database Testing Strategies

Options:

| Strategy | Pros | Cons |
|---|---|---|
| Mock repository | Fast | Misses query/schema bugs |
| In-memory fake | Fast and useful | May not match real DB semantics |
| Local test DB | Realistic | Slower setup |
| Testcontainers | Very realistic | Requires Docker/CI support |
| Shared test DB | Easy start | Isolation/flakiness risk |

Strong answer:

```text
For important persistence behavior, I prefer tests against a real database engine because mocks miss schema, SQL, migration, and transaction issues.
```

---

## 32. Database Cleanup Patterns

Common patterns:

- Transaction per test and rollback.
- Truncate tables after each test.
- Unique schema/database per test worker.
- Seed known data before each test.
- Use factories/builders.

Transaction pattern idea:

```javascript
beforeEach(async () => {
    transaction = await db.beginTransaction();
});

afterEach(async () => {
    await transaction.rollback();
});
```

Caution:

```text
Transaction rollback can be hard if the app uses its own connection pool outside the test transaction.
```

Strong answer:

```text
Database tests must isolate data. Flaky DB tests often come from shared state and parallel test interference.
```

---

## 33. Testing Migrations

Migration tests prove schema changes work.

Test ideas:

- Apply migrations from empty database.
- Apply migrations to representative old schema/data.
- Rollback if your process supports rollback.
- Verify indexes/constraints exist.
- Verify data backfill correctness.

Strong answer:

```text
Migrations are production code. I test that they apply cleanly and preserve or transform data correctly.
```

---

## 34. Contract Tests

Contract tests prevent producer/consumer mismatch.

Examples:

- Frontend expects `guestName`, backend returns `guest_name`.
- API error shape changes unexpectedly.
- Enum adds unknown value.
- Required field disappears.

Approaches:

- OpenAPI schema validation.
- Consumer-driven contracts.
- Generated TypeScript types.
- Runtime response validation in tests.

Strong answer:

```text
Contract tests are valuable when independent teams or services change APIs. They catch breaking changes before deployment.
```

---

## 35. Schema Validation Tests

Example with a conceptual schema validator:

```javascript
it("matches booking response schema", async () => {
    const response = await request(app).get("/bookings/b1");

    expect(response.status).toBe(200);
    expect(validateBookingResponse(response.body)).toEqual({ valid: true });
});
```

Strong answer:

```text
Schema tests verify the shape clients depend on, not just that the endpoint returns 200.
```

---

## 36. Playwright E2E Tests

E2E test example:

```javascript
import { expect, test } from "@playwright/test";

test("user can search and open booking", async ({ page }) => {
    await page.goto("/bookings");

    await page.getByRole("searchbox", { name: /search/i }).fill("Ava");
    await page.getByRole("link", { name: /Ava/ }).click();

    await expect(page.getByRole("heading", { name: /booking details/i })).toBeVisible();
});
```

Good E2E candidates:

- Login.
- Checkout/payment happy path in test mode.
- Search and details flow.
- Admin critical workflow.
- File upload if business critical.

Strong answer:

```text
E2E tests should cover critical user journeys, not every edge case. Too many E2E tests slow CI and increase flakiness.
```

---

## 37. Playwright Best Practices

Prefer:

- `getByRole`
- `getByLabel`
- `getByText` when user-visible
- Auto-waiting assertions
- Isolated test data
- Trace/video on failure
- Test IDs only when semantic selectors are not enough

Avoid:

- Arbitrary sleeps.
- Brittle CSS selectors.
- Sharing logged-in state unsafely.
- Depending on test order.
- Using production third-party services directly.

Bad:

```javascript
await page.waitForTimeout(5000);
```

Better:

```javascript
await expect(page.getByText("Saved")).toBeVisible();
```

Strong answer:

```text
Reliable E2E tests wait for observable conditions, not arbitrary time.
```

---

## 38. Accessibility Tests

Automated checks can catch:

- Missing labels.
- Color contrast issues.
- Invalid ARIA.
- Missing alt text.
- Heading/order problems.

But manual testing is still needed for:

- Keyboard flow.
- Screen reader experience.
- Focus management.
- Usability of custom widgets.

Example E2E checks:

```javascript
await page.getByRole("button", { name: /save/i }).focus();
await page.keyboard.press("Enter");
await expect(page.getByText("Saved")).toBeVisible();
```

Strong answer:

```text
Accessibility testing combines automated scans with keyboard and screen-reader-oriented behavior tests.
```

---

## 39. Visual Regression Tests

Visual tests catch:

- Layout shifts.
- Broken CSS.
- Missing images.
- Unexpected spacing.
- Theme regressions.
- Responsive breakage.

Cautions:

- Can be noisy.
- Needs stable data and fonts.
- Should focus on important screens/components.
- Review diffs carefully.

Strong answer:

```text
Visual regression tests are useful for UI risk, but they need stable environments and should not replace behavioral tests.
```

---

## 40. Performance Regression Tests

Examples:

- API response under latency budget.
- Bundle size budget.
- Lighthouse/INP checks.
- Event-loop delay under load.
- Query count budget.
- Render time for large list.

Simple assertion idea:

```javascript
it("does not execute N+1 queries", async () => {
    const queryCounter = startQueryCounter();

    await request(app).get("/orders?limit=50");

    expect(queryCounter.count()).toBeLessThanOrEqual(3);
});
```

Strong answer:

```text
Performance tests should guard known risks with budgets: query count, payload size, bundle size, and critical route latency.
```

---

## 41. Security Regression Tests

Examples:

- XSS payload rendered as text.
- Cross-tenant ID returns 404/403.
- SQL injection string does not bypass auth/search.
- Missing CSRF token rejected.
- Invalid JWT rejected.
- Path traversal blocked.
- Oversized body rejected.
- Webhook bad signature rejected.

Example:

```javascript
it("renders comment text without executing HTML", () => {
    render(<Comment body={'<img src=x onerror="alert(1)">'} />);

    expect(screen.getByText(/onerror/)).toBeInTheDocument();
    expect(document.querySelector("img")).toBeNull();
});
```

Strong answer:

```text
Security tests should encode known abuse cases so a future refactor does not reopen the vulnerability.
```

---

## 42. Snapshot Tests

Snapshot tests store expected output.

Useful for:

- Stable serialized output.
- Generated config.
- AST transforms.
- Small stable components.

Risky for:

- Large UI trees.
- Frequently changing markup.
- Values with dates/random IDs.
- Replacing real assertions.

Bad habit:

```text
Update snapshots without reviewing what changed.
```

Strong answer:

```text
Snapshots are useful when output is stable and reviewable. Large snapshots often create noise instead of confidence.
```

---

## 43. Coverage

Coverage tells what code ran, not whether behavior was asserted.

Types:

- Statement coverage.
- Branch coverage.
- Function coverage.
- Line coverage.

Use coverage to find:

- Untested branches.
- Missing failure paths.
- Dead zones in critical modules.

Do not use coverage as:

```text
The only measure of test quality.
```

Strong answer:

```text
Coverage is a useful signal, but high coverage with weak assertions can still miss real bugs.
```

---

## 44. Mutation Testing

Mutation testing changes code and checks whether tests fail.

Example mutation:

```text
>= becomes >
true becomes false
+ becomes -
```

If tests still pass, they may be weak.

Use for:

- Critical business logic.
- Payment/pricing rules.
- Authorization rules.
- Complex algorithms.

Strong answer:

```text
Mutation testing measures whether tests actually detect behavior changes, not just whether they execute code.
```

---

## 45. Flaky Tests

Flaky tests pass/fail without code changes.

Common causes:

- Real time sleeps.
- Shared mutable state.
- Test order dependency.
- Network dependency.
- Random data collisions.
- Unawaited promises.
- Race conditions.
- Parallel database tests.
- Timezone/locale differences.
- Animations/transitions.

Strong answer:

```text
A flaky test is a broken test or a real race exposed by tests. Either way, it deserves root-cause fixing, not blind retries forever.
```

---

## 46. Fixing Flaky Tests

Checklist:

- Replace sleeps with condition waits.
- Await all async work.
- Isolate database/test data.
- Use unique IDs.
- Reset mocks between tests.
- Restore timers.
- Freeze time or inject clock.
- Avoid test order dependency.
- Mock external network.
- Capture trace/video/logs on failure.

Strong answer:

```text
I fix flakiness by removing nondeterminism: time, network, shared state, ordering, and unawaited async work.
```

---

## 47. Test Isolation

Each test should be able to run:

- Alone.
- In any order.
- Repeatedly.
- In parallel if test type supports it.

Bad:

```javascript
it("creates user", async () => { /* creates u1 */ });
it("updates user", async () => { /* assumes u1 exists */ });
```

Better:

```javascript
it("updates user", async () => {
    const user = await seedUser();
    await updateUser(user.id, { name: "Ava" });
    expect(await loadUser(user.id)).toEqual(expect.objectContaining({ name: "Ava" }));
});
```

Strong answer:

```text
Tests should create their own required state so they do not depend on previous tests.
```

---

## 48. Mock Cleanup

Common cleanup:

```javascript
afterEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
});
```

Differences:

| API | Meaning |
|---|---|
| clear mocks | Clear call history |
| reset mocks | Reset implementation too |
| restore mocks | Restore original spied method |

Strong answer:

```text
Mock and timer state can leak between tests, so cleanup is part of test correctness.
```

---

## 49. Testing Modules With Side Effects

Side effects at import time make tests harder.

Bad:

```javascript
const connection = connectDatabase(process.env.DATABASE_URL);
export { connection };
```

Better:

```javascript
export function createDatabaseConnection(config) {
    return connectDatabase(config.databaseUrl);
}
```

Strong answer:

```text
Modules that do real work at import time are harder to test. I prefer factories and explicit startup wiring.
```

---

## 50. Dependency Injection For Testability

Service:

```javascript
function createBookingService({ bookingRepository, emailQueue, clock }) {
    return {
        async createBooking(input) {
            const booking = await bookingRepository.insert({
                ...input,
                createdAt: clock().toISOString()
            });

            await emailQueue.publish({ type: "booking.created", bookingId: booking.id });
            return booking;
        }
    };
}
```

Test:

```javascript
it("publishes booking event", async () => {
    const emailQueue = { publish: vi.fn() };
    const service = createBookingService({
        bookingRepository: { insert: async value => ({ id: "b1", ...value }) },
        emailQueue,
        clock: () => new Date("2026-06-20T00:00:00Z")
    });

    await service.createBooking({ guestName: "Ava" });

    expect(emailQueue.publish).toHaveBeenCalledWith({
        type: "booking.created",
        bookingId: "b1"
    });
});
```

Strong answer:

```text
Dependency injection makes tests simpler by replacing real boundaries with controlled doubles without changing production behavior.
```

---

## 51. Testing Event Emitters

Example:

```javascript
it("emits booking.created", () => {
    const events = new EventEmitter();
    const listener = vi.fn();

    events.on("booking.created", listener);

    createBooking({ events, id: "b1" });

    expect(listener).toHaveBeenCalledWith({ id: "b1" });
});
```

For async events:

```javascript
await new Promise(resolve => events.once("done", resolve));
```

Strong answer:

```text
Event tests should verify the event contract and cleanup listeners to avoid leaks between tests.
```

---

## 52. Testing Queues And Workers

Worker behavior to test:

- Valid message processed.
- Invalid message rejected or sent to DLQ.
- Handler is idempotent.
- Retryable error is retried.
- Non-retryable error is not retried forever.
- Acknowledgement happens after success.
- Message is not acked before durable side effect.

Example:

```javascript
it("does not send duplicate email for same booking", async () => {
    await handleBookingCreated({ bookingId: "b1" });
    await handleBookingCreated({ bookingId: "b1" });

    expect(emailProvider.send).toHaveBeenCalledTimes(1);
});
```

Strong answer:

```text
Queue tests should assume at-least-once delivery and prove idempotency.
```

---

## 53. Testing Webhooks

Cases:

- Valid signature accepted.
- Invalid signature rejected.
- Old timestamp rejected.
- Duplicate event ignored.
- Event enqueued quickly.
- Heavy work not done in request path.

Example:

```javascript
it("rejects webhook with invalid signature", async () => {
    const response = await request(app)
        .post("/webhooks/payment")
        .set("X-Signature", "bad")
        .send({ id: "evt_1" });

    expect(response.status).toBe(401);
});
```

Strong answer:

```text
Webhook tests should verify authenticity, replay protection, idempotency, and fast acknowledgement.
```

---

## 54. Testing File Uploads

Test:

- Valid upload accepted.
- Oversized file rejected.
- Wrong type rejected.
- Path traversal filename neutralized.
- Empty file handled.
- Ownership enforced on download.
- Malware scan flow triggered if applicable.

Example:

```javascript
it("rejects oversized upload", async () => {
    const response = await request(app)
        .post("/uploads")
        .attach("file", Buffer.alloc(11 * 1024 * 1024), "large.pdf");

    expect(response.status).toBe(413);
});
```

Strong answer:

```text
Upload tests should cover resource limits and security, not only the happy path.
```

---

## 55. Testing WebSockets

Test:

- Connection authenticated.
- Unauthorized connection rejected.
- Message broadcast reaches intended clients.
- User cannot subscribe to another tenant.
- Heartbeat closes stale connections.
- Cleanup occurs on disconnect.

Strong answer:

```text
WebSocket tests should prove authorization, message routing, and cleanup because connection state is long-lived.
```

---

## 56. Testing Streams

Test stream output without loading huge fixtures.

```javascript
import { Readable } from "node:stream";

it("transforms rows to csv", async () => {
    const input = Readable.from([
        { id: "b1", guestName: "Ava" },
        { id: "b2", guestName: "Mia" }
    ]);

    const chunks = [];

    for await (const chunk of input.pipe(createCsvTransform())) {
        chunks.push(chunk);
    }

    expect(chunks.join("")).toContain("Ava");
});
```

Strong answer:

```text
Stream tests should verify output, error handling, and backpressure-sensitive behavior without requiring huge files.
```

---

## 57. Testing Logging And Metrics

Usually avoid over-testing logs, but test when:

- Audit log is a requirement.
- Security event must be logged.
- Metric drives alerting/SLO.
- Request ID propagation is required.

Example:

```javascript
it("logs request id on failure", async () => {
    await handler(requestWithId("req_123"), response);

    expect(logger.error).toHaveBeenCalledWith(
        expect.objectContaining({ requestId: "req_123" }),
        expect.any(String)
    );
});
```

Strong answer:

```text
I test observability when it is part of the production contract, such as audit logging or critical metrics.
```

---

## 58. Testing Environment Config

Test startup config validation:

```javascript
it("throws when DATABASE_URL is missing", () => {
    expect(() => loadConfig({})).toThrow("DATABASE_URL");
});

it("loads valid config", () => {
    expect(loadConfig({ DATABASE_URL: "postgres://test", PORT: "3000" })).toEqual({
        databaseUrl: "postgres://test",
        port: 3000
    });
});
```

Strong answer:

```text
Config validation tests prevent services from accepting traffic with missing required environment variables.
```

---

## 59. Testing CLI Scripts

For scripts:

- Extract logic into testable functions.
- Test argument parsing.
- Test dry-run mode.
- Mock filesystem/process boundaries.
- Use temp directories for integration tests.

Strong answer:

```text
CLI scripts should not be untested just because they are scripts. Extract logic and test file/process boundaries carefully.
```

---

## 60. CI Test Pipeline

Typical JS CI pipeline:

```text
install with npm ci
lint
format check
type check
unit tests
integration tests
build
E2E/smoke tests
coverage upload
security/dependency scan
```

Strong answer:

```text
CI should catch cheap deterministic failures early and run slower confidence checks later or in parallel.
```

---

## 61. Test Speed Strategy

Keep feedback fast:

- Unit tests in watch mode locally.
- Integration tests in CI and pre-push for touched areas.
- E2E tests for critical flows.
- Parallelize isolated tests.
- Avoid unnecessary real network calls.
- Reuse expensive setup safely.
- Split slow suites.

Strong answer:

```text
A slow test suite becomes ignored. I keep most tests fast and reserve slower tests for boundaries and critical workflows.
```

---

## 62. CI Flake Policy

Bad policy:

```text
Rerun failed tests until green and ignore.
```

Better:

- Quarantine only with owner and deadline.
- Capture traces/logs on failure.
- Track flake rate.
- Fix root cause.
- Do not let flaky tests block all work forever, but do not normalize them.

Strong answer:

```text
Retries can reduce noise, but they should not hide flaky tests. Flakes need ownership and root-cause fixes.
```

---

## 63. What Not To Test

Usually avoid testing:

- Framework internals.
- Third-party library behavior.
- Private implementation details.
- Trivial getters/setters.
- Exact CSS class names unless they are contract.
- Large snapshots without intent.

But test wrappers/config if:

- You add logic around library.
- Configuration is critical.
- The integration is risky.

Strong answer:

```text
I do not test the framework. I test my behavior and my integration choices.
```

---

## 64. Mini Program: Testable Booking Service

Service:

```javascript
export function createBookingService({ repository, eventBus, clock }) {
    return {
        async createBooking(input) {
            if (!input.guestName?.trim()) {
                throw new Error("guestName required");
            }

            const booking = await repository.insert({
                guestName: input.guestName.trim(),
                roomId: input.roomId,
                status: "CREATED",
                createdAt: clock().toISOString()
            });

            eventBus.emit("booking.created", { bookingId: booking.id });
            return booking;
        }
    };
}
```

Test:

```javascript
it("creates booking and emits event", async () => {
    const events = [];
    const service = createBookingService({
        repository: {
            insert: async value => ({ id: "b1", ...value })
        },
        eventBus: {
            emit: (type, payload) => events.push({ type, payload })
        },
        clock: () => new Date("2026-06-20T00:00:00Z")
    });

    const booking = await service.createBooking({
        guestName: " Ava ",
        roomId: "r1"
    });

    expect(booking).toEqual(expect.objectContaining({
        id: "b1",
        guestName: "Ava",
        status: "CREATED"
    }));

    expect(events).toContainEqual({
        type: "booking.created",
        payload: { bookingId: "b1" }
    });
});
```

Why strong:

- Dependency injection.
- Deterministic clock.
- Observable behavior.
- No real database needed for unit test.

---

## 65. Mini Program: API Test Checklist In Code

```javascript
describe("POST /bookings", () => {
    it("creates booking for valid input", async () => {});
    it("returns 400 for invalid input", async () => {});
    it("returns 401 without auth", async () => {});
    it("returns 403 without permission", async () => {});
    it("does not create duplicate booking for same idempotency key", async () => {});
    it("returns stable error shape", async () => {});
});
```

Interview note:

```text
A good API test suite covers success, validation, auth, authorization, idempotency, and error shape.
```

---

## 66. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Testing implementation details | Brittle tests | Test observable behavior |
| Too many mocks | False confidence | Mock external edges only |
| No integration tests | Wiring bugs escape | Add boundary tests |
| Too many E2E tests | Slow/flaky CI | Cover critical flows only |
| Not awaiting async work | False passes | Return/await promises |
| Real sleeps | Flaky and slow | Wait for condition/fake timers |
| Shared test data | Order dependency | Seed per test |
| No failure-path tests | Production bugs escape | Test invalid/error cases |
| Snapshot everything | Noise | Use focused assertions |
| Coverage worship | Weak assertions hidden | Use behavior and risk focus |
| No auth negative tests | Security regressions | Test 401/403/cross-tenant |
| No contract tests | Client breakage | Validate schemas/contracts |
| Ignoring flaky tests | CI loses trust | Fix root cause |
| Testing only happy path | Low confidence | Include edge and abuse cases |
| Not restoring mocks/timers | Cross-test pollution | Cleanup after each test |
| Depending on current time | Date flakes | Inject/freeze clock |

---

## 67. Strong Interview Answers

### What is your testing strategy for a JS app?

```text
I combine static checks, unit tests for pure logic, integration/component tests for boundaries and
user behavior, contract tests for API agreements, and a small number of E2E tests for critical flows.
I optimize for reliable confidence, not just coverage percentage.
```

### How do you decide what to mock?

```text
I mock slow, expensive, nondeterministic, or external dependencies. I avoid mocking the behavior I
am trying to prove. For critical boundaries like DB queries or API contracts, I add integration or contract tests.
```

### How do you test async JavaScript?

```text
I always await or return promises, test both resolve and reject paths, use fake timers for time-based
logic, and control promise resolution order for race conditions.
```

### How do you reduce flaky tests?

```text
I remove nondeterminism: arbitrary sleeps, shared state, real network calls, time dependency,
unawaited promises, and order dependency. E2E tests should wait for observable UI conditions.
```

### How do you test a Node API?

```text
I test route behavior through HTTP where possible: status codes, response body, validation, auth,
authorization, idempotency, error shape, and persistence effects. Critical database behavior should
use a real test database or realistic integration setup.
```

### How do you test frontend components?

```text
I test user-visible behavior using roles, labels, and text. I avoid asserting private state or CSS
structure unless that is the contract. I cover loading, error, empty, and success states for async UI.
```

---

## 68. MAANG Scenario 1: Flaky Checkout E2E

> Checkout E2E fails randomly in CI but passes locally.

Strong answer:

```text
I would inspect traces, screenshots, network logs, and timing around the failure. Common causes are
arbitrary sleeps, shared test data, real third-party dependencies, animations, unawaited async work,
or tests depending on execution order.

I would replace sleeps with condition-based waits, isolate data per test, mock or sandbox external
payment dependencies, and keep only critical checkout coverage in E2E while moving edge cases to
lower-level tests.
```

---

## 69. MAANG Scenario 2: High Coverage, Many Bugs

> Team has 90 percent coverage but still ships regressions.

Strong answer:

```text
Coverage only says code executed, not that important behavior was asserted. I would review whether
tests assert outcomes, cover failure paths, and exercise real integration boundaries.

I would add risk-based tests around recent incidents, mutation testing for critical logic, contract
tests for API shape, and integration tests where mocks hide wiring bugs.
```

---

## 70. MAANG Scenario 3: Frontend Breaks After Backend Change

> Backend renames guest_name to guestName and frontend silently breaks.

Strong answer:

```text
This is an API contract failure. I would add contract tests or schema validation so response shape
changes fail in CI. Generated types from OpenAPI/GraphQL can help, but runtime tests should still
validate real responses.

On the frontend, I would also validate boundary data or use typed API clients so contract drift is
caught earlier.
```

---

## 71. MAANG Scenario 4: Payment Retry Bug

> Duplicate charges happen after client retry. What tests would you add?

Strong answer:

```text
I would add API integration tests for idempotency: same key and same payload returns the same
payment result, same key with different payload returns conflict, and concurrent duplicate requests
cannot both create payments.

I would also add unit tests around retry policy so unsafe writes are not retried without idempotency,
and contract tests to ensure clients send idempotency keys.
```

---

## 72. MAANG Scenario 5: Memory Leak Regression

> A Node memory leak was fixed. How do you prevent it from coming back?

Strong answer:

```text
I would add a focused regression test for the retaining behavior if possible, such as ensuring
listeners are removed on disconnect or cache size stays bounded. For process-level leaks, I would
add load or soak tests in CI/nightly and production metrics for heap, RSS, external memory, and
listener counts.

Not every memory leak is easy to catch with a unit test, so monitoring and regression load tests
are part of the prevention strategy.
```

---

## 73. Rapid Revision

- Tests are executable evidence of behavior.
- Coverage is a signal, not the goal.
- Use the smallest reliable test level.
- Unit tests are best for pure logic.
- Integration tests catch wiring and boundary bugs.
- E2E tests should cover critical user journeys.
- Component tests should use user-visible queries.
- Avoid testing implementation details.
- Arrange-Act-Assert improves readability.
- Test names should describe behavior.
- Builders keep fixtures maintainable.
- Mock external edges, not the behavior being proven.
- Too many mocks create false confidence.
- Async tests must await or return promises.
- Test rejection paths with rejects.toThrow.
- Fake timers help test debounce, timeout, retry, and intervals.
- Restore real timers after fake timer tests.
- Inject clocks for date/time behavior.
- Race-condition tests should control promise resolution order.
- fetch does not reject on HTTP 4xx/5xx.
- API tests should cover status, body, validation, auth, authorization, and errors.
- Security tests should include negative and abuse cases.
- Database tests need data isolation.
- Test migrations because migrations are production code.
- Contract tests prevent API shape breakage.
- Playwright tests should avoid arbitrary sleeps.
- Accessibility tests include keyboard/focus behavior.
- Visual tests catch layout regressions but can be noisy.
- Performance tests should guard budgets.
- Snapshot tests should be small and reviewable.
- Mutation testing checks whether tests detect code changes.
- Flaky tests destroy CI trust.
- Test isolation means tests run alone, in any order, repeatedly.
- Clean up mocks, timers, listeners, and test data.
- Avoid import-time side effects for testability.
- Dependency injection makes boundaries controllable.
- Queue consumers need idempotency tests.
- Webhooks need signature, duplicate, and replay tests.
- Upload tests should check size, type, auth, and path behavior.
- CI should run lint, type checks, tests, build, and security scans.

---

## 74. Official Source Notes

Use these sources when refreshing JavaScript testing knowledge:

- Jest docs: `https://jestjs.io/docs/getting-started`
- Vitest docs: `https://vitest.dev/guide/`
- Testing Library docs: `https://testing-library.com/docs/`
- React Testing Library docs: `https://testing-library.com/docs/react-testing-library/intro/`
- Playwright docs: `https://playwright.dev/docs/intro`
- MSW docs: `https://mswjs.io/docs/`
- Supertest repo: `https://github.com/ladjs/supertest`
- Testcontainers for Node.js: `https://node.testcontainers.org/`
- OpenAPI Initiative: `https://www.openapis.org/`
- Pact contract testing: `https://docs.pact.io/`
- Istanbul/nyc coverage: `https://istanbul.js.org/`
- Stryker mutation testing: `https://stryker-mutator.io/`
- web.dev testing guidance: `https://web.dev/learn/testing/`

Interview safety line:

```text
For JavaScript testing interviews, I focus on confidence and risk: unit tests for logic, integration
for boundaries, component tests for user behavior, contract tests for API stability, E2E for critical
journeys, and CI practices that keep the suite fast and trustworthy.
```