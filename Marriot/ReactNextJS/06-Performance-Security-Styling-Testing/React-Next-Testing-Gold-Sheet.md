# React + Next.js Testing - Gold Sheet

> Track File #16 of 24 - Group 6: Performance, Security, Styling, And Testing
> Covers: Jest, React Testing Library, integration testing, E2E with Cypress/Playwright

---

## 1. Intuition

Testing should prove behavior at the cheapest reliable level.

```text
static checks -> unit tests -> component tests -> integration tests -> E2E
```

Do not test implementation details when user-visible behavior is what matters.

---

## 2. Unit Testing

Use for pure logic:
- validators
- reducers
- mappers
- formatters
- cache key builders
- permission helpers

```ts
it('formats cents as dollars', () => {
  expect(formatPrice(1299)).toBe('$12.99');
});
```

Fast and stable.

---

## 3. React Testing Library

Test from user perspective.

```tsx
it('submits email', async () => {
  const onSubmit = vi.fn();
  render(<EmailForm onSubmit={onSubmit} />);

  await userEvent.type(screen.getByLabelText(/email/i), 'a@example.com');
  await userEvent.click(screen.getByRole('button', {name: /continue/i}));

  expect(onSubmit).toHaveBeenCalledWith('a@example.com');
});
```

Good queries:
- `getByRole`
- `getByLabelText`
- `getByText`
- `findBy...` for async UI

Avoid:
- testing component state directly
- brittle snapshots for large screens
- querying by class names

---

## 4. Integration Testing

Test feature behavior with providers and mocked network.

Example:

```text
ProductsPage
  query client provider
  mocked API response
  user filters
  list updates
  empty state appears
```

Use MSW or framework mocks to simulate API behavior.

---

## 5. Next.js Testing Notes

Test:
- server actions as server functions where possible
- route handlers with request objects
- client components with Testing Library
- pages through E2E for routing/rendering correctness

Mock:
- `next/navigation`
- `next/image` when needed
- environment variables
- server-only modules carefully

---

## 6. E2E Testing

Tools:
- Playwright
- Cypress

Best E2E flows:
- login/logout
- checkout
- critical dashboard action
- route protection
- deep link/bookmarkable route
- GenAI chat streaming smoke

Playwright example:

```ts
test('user can login', async ({page}) => {
  await page.goto('/login');
  await page.getByLabel('Email').fill('a@example.com');
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', {name: 'Login'}).click();
  await expect(page.getByText('Dashboard')).toBeVisible();
});
```

---

## 7. CI Quality Gates

Recommended:
- TypeScript
- lint
- unit tests
- component/integration tests
- E2E smoke on release/main
- build check
- bundle budget check for large apps
- accessibility checks where possible

---

## 8. Real-World Use Cases

- Reducer for checkout workflow: unit test.
- Login form: component test.
- Product search with API: integration test.
- Checkout purchase path: E2E.
- Route protection: E2E plus server authorization unit tests.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Only snapshots | Low behavior confidence | user-centric assertions |
| Mocking everything | Integration bugs escape | mock at boundaries |
| No E2E for critical flows | Release risk | small reliable E2E suite |
| Testing implementation details | Brittle | test behavior |
| CI skips build | Next build may fail | include build gate |

---

## 10. Strong Interview Answer

Question:
How do you test a React/Next.js app?

Strong answer:

```text
I use TypeScript and lint as the first gate. Unit tests cover pure logic like
reducers, validators, and mappers. React Testing Library covers components from
the user's perspective. Integration tests render feature screens with providers
and mocked APIs. E2E tests with Playwright or Cypress cover critical flows like
login, checkout, and route protection. In CI, I also run the Next build because
server/client boundaries and route conventions can fail at build time.
```

---

## 11. Revision Notes

- One-line summary: Test behavior cheaply, and reserve E2E for critical confidence.
- Three keywords: RTL, integration, E2E.
- One interview trap: A passing component test does not prove server route protection.
- One memory trick: Unit for logic, RTL for UI, E2E for journeys.

