# GitHub Actions Frontend CI and Preview Deployments Gold Sheet

> Goal: design frontend pipelines for React, Angular, Vue, Next.js, Vite, Storybook, E2E tests, preview deployments, CDN releases, and frontend production safety.

---

## 0. How To Read This

Beginner focus:

- install dependencies
- lint
- typecheck
- test
- build

Intermediate focus:

- npm/yarn/pnpm caching
- Playwright/Cypress
- Storybook
- preview deployments
- bundle size
- Lighthouse

Senior focus:

- monorepo affected frontend builds
- CDN cache invalidation
- source maps and Sentry releases
- environment variables
- visual regression
- frontend rollback strategy

---

# Topic 1: Frontend CI and Preview Deployments

---

## 1. Intuition

Frontend CI protects user experience.

It asks:

```text
Does the code typecheck?
Does UI behavior work?
Does the app build?
Did bundle size explode?
Did visual UI regress?
Is accessibility still acceptable?
Can reviewers open a preview URL?
```

Beginner explanation:

A frontend GitHub Actions pipeline automatically checks lint, types, tests, and builds for UI code, then can publish a preview deployment for reviewers.

---

## 2. Definition

- Definition: Frontend CI/CD is automated validation and deployment of web applications, static assets, SSR apps, previews, and UI test artifacts.
- Category: CI/CD for web applications
- Core idea: catch UI, build, dependency, and deployment issues before users see them.

---

## 3. Why It Exists

Without frontend CI:

- broken TypeScript reaches main
- UI tests are skipped
- bundle size grows silently
- preview testing is manual
- CDN cache issues appear in production
- source maps are mishandled
- accessibility regressions slip through

Frontend CI gives consistent feedback and reviewable previews.

---

## 4. Reality

Frontend pipelines commonly include:

- React / Angular / Vue / Next.js / Vite builds
- `npm`, `yarn`, or `pnpm`
- lint
- typecheck
- unit/component tests
- Playwright/Cypress E2E
- Storybook build
- visual regression
- Lighthouse CI
- preview deployments
- Sentry release/source map upload
- CDN invalidation

Senior expectation:

You should understand the difference between build-time environment variables, runtime configuration, and secret exposure.

---

## 5. How It Works

### Part A: Standard Frontend PR Flow

```text
pull_request
-> checkout
-> setup Node
-> restore package manager cache
-> install with lockfile
-> lint
-> typecheck
-> unit/component tests
-> build
-> upload reports
-> optionally deploy preview
```

### Part B: Package Managers

| Tool | Install Command | Notes |
|---|---|---|
| npm | `npm ci` | strict lockfile install |
| yarn | `yarn install --immutable` | common in Yarn Berry |
| pnpm | `pnpm install --frozen-lockfile` | fast monorepo-friendly |

Use lockfile installs in CI. Do not use commands that silently modify dependencies.

### Part C: React / Vite CI

```yaml
name: Frontend CI

on:
  pull_request:
    paths:
      - "frontend/**"
      - ".github/workflows/frontend-ci.yml"

permissions:
  contents: read

jobs:
  verify:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json

      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm test -- --run
      - run: npm run build
```

### Part D: pnpm CI

```yaml
- uses: pnpm/action-setup@v4
  with:
    version: 9

- uses: actions/setup-node@v4
  with:
    node-version: "22"
    cache: pnpm
    cache-dependency-path: pnpm-lock.yaml

- run: pnpm install --frozen-lockfile
- run: pnpm lint
- run: pnpm test
- run: pnpm build
```

### Part E: Next.js CI

Next.js pipelines should check:

- TypeScript
- lint
- unit tests
- build
- environment variable assumptions
- SSR/runtime target
- source map policy

Example:

```yaml
- run: npm run build
  env:
    NEXT_TELEMETRY_DISABLED: "1"
```

Important:

Do not put secrets into public frontend environment variables. Variables exposed to browser bundles are visible to users.

### Part F: Unit and Component Tests

Common tools:

- Jest
- Vitest
- React Testing Library
- Angular TestBed
- Vue Test Utils

Run on PR:

```yaml
- run: npm test -- --coverage
```

Upload coverage:

```yaml
- uses: actions/upload-artifact@v4
  if: always()
  with:
    name: frontend-coverage
    path: frontend/coverage/
```

### Part G: Playwright E2E

```yaml
jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
        working-directory: frontend
      - run: npx playwright install --with-deps
        working-directory: frontend
      - run: npm run e2e
        working-directory: frontend
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

### Part H: Cypress E2E

Cypress can run against:

- local app server
- deployed preview URL
- test environment

Key idea:

Run E2E where it gives confidence without blocking every tiny UI change for too long.

### Part I: Storybook and Visual Regression

Storybook CI:

```yaml
- run: npm run build-storybook
```

Use visual regression for:

- design systems
- shared component libraries
- critical UI flows

Common tool:

- Chromatic
- Percy
- Playwright screenshot comparisons

### Part J: Accessibility and Lighthouse

Frontend quality gates can include:

- axe accessibility tests
- Lighthouse CI
- performance budget
- bundle size budget

Use budgets carefully:

- too strict = noisy
- too loose = useless

### Part K: Preview Deployments

Preview deployment flow:

```text
PR opened
-> build frontend
-> deploy preview
-> comment preview URL
-> run smoke/E2E against preview
-> cleanup on PR close
```

Targets:

- Vercel
- Netlify
- Firebase Hosting
- S3 + CloudFront
- Kubernetes preview namespace

### Part L: Static Site Deployment

Static assets need:

- content-hashed filenames
- long cache headers for immutable assets
- short/no cache for HTML shell
- CDN invalidation or versioned paths

Trap:

If `index.html` is cached too long, users may load old JS references.

### Part M: Source Maps and Error Tracking

Source maps are useful for production debugging, but risky if exposed publicly without intention.

Production flow:

```text
build release
upload source maps to Sentry
hide or restrict public source maps
deploy assets
associate release with commit SHA
```

### Part N: Frontend Rollback

Rollback options:

- redeploy previous build artifact
- switch CDN origin/path
- revert release
- disable feature flag
- roll back only frontend if API contract remains compatible

Senior point:

Frontend rollback can be fast only if artifacts are versioned and old backend contracts still work.

---

## 6. What Problem It Solves

- Primary problem solved: prevent broken UI builds, behavior regressions, and unsafe frontend deployments
- Secondary benefits: faster review, preview validation, visual confidence, performance monitoring
- Systems impact: protects user experience and release velocity

---

## 7. When To Rely On It

Use frontend CI for:

- every UI PR
- design system changes
- dependency updates
- preview review
- release candidate validation
- accessibility/performance gates

Interviewer keywords:

- React CI
- preview deployment
- Playwright
- Storybook
- bundle size
- CDN invalidation
- source maps
- frontend rollback

---

## 8. When Not To Overload It

Avoid running every expensive browser test on every PR if:

- feedback becomes too slow
- tests are flaky
- most changes are low risk

Layer tests:

```text
PR: lint, typecheck, unit, build
selected PRs: preview + smoke
main: E2E and visual regression
nightly: full browser matrix and Lighthouse
release: production-like smoke
```

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Catches UI/build issues early | Browser tests can be slow |
| Preview URLs improve review | Preview env cleanup needed |
| Visual regression protects design systems | Snapshot noise can be high |
| Bundle checks protect performance | Budgets need tuning |
| Source maps improve debugging | Source map exposure risk |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More E2E:
  More confidence, slower PR feedback.
- Preview per PR:
  Better review, higher infra cost.
- Strict visual tests:
  Better UI safety, more false positives.
- CDN caching:
  Better performance, more invalidation complexity.

### Common Mistakes

- Mistake: "Store frontend secrets in build env."
  Why it is wrong: browser-exposed variables are visible to users.
  Better approach: keep secrets server-side and expose only public config.

- Mistake: "Cache `node_modules` blindly."
  Why it is wrong: package-manager caches are usually safer and more portable.
  Better approach: use `setup-node` cache or pnpm store cache.

- Mistake: "Deploy preview but never clean it up."
  Why it is wrong: cost and stale environments grow.
  Better approach: cleanup on PR close.

- Mistake: "Invalidate entire CDN on every deploy."
  Why it is wrong: expensive and unnecessary with content-hashed assets.
  Better approach: immutable asset names plus targeted HTML invalidation.

---

## 11. Key Numbers

Useful targets:

- PR frontend checks: ideally under 10 minutes
- Build artifacts: version by commit SHA
- Static assets: content-hashed for long cache
- HTML shell: short cache or invalidated
- Preview deployments: cleaned up after PR close
- Full browser matrix: usually scheduled or release-gated

---

## 12. Failure Modes

### Build Works Locally But Fails In CI

Causes:

- missing lockfile update
- different Node version
- case-sensitive path issue
- env variable missing

Fix:

- pin Node version
- use lockfile install
- validate required env
- run build in CI

### Preview Leaks Secret

Cause:

- secret embedded in frontend bundle

Fix:

- rotate secret
- remove from build env
- move secret to backend
- audit build artifacts

### CDN Serves Old JS

Cause:

- bad cache headers
- stale `index.html`
- non-hashed asset names

Fix:

- content-hashed files
- invalidate HTML
- versioned deploy path

### Visual Regression Noise

Cause:

- unstable screenshots
- dynamic content
- font loading differences

Fix:

- mock dynamic data
- stabilize viewport/fonts
- set review workflow for snapshots

---

## 13. Scenario

- Product / system: React customer dashboard
- Why this concept fits: UI changes need lint/type/test/build, preview URL, and visual review before merge
- What would go wrong without it: broken bundles, layout regressions, and stale CDN assets could reach customers

---

## 14. Code Sample

Frontend CI with Playwright artifact:

```yaml
name: Frontend CI

on:
  pull_request:
    paths:
      - "frontend/**"

permissions:
  contents: read

concurrency:
  group: frontend-pr-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  verify:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm test -- --run
      - run: npm run build

  e2e:
    runs-on: ubuntu-latest
    needs: verify
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run e2e
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

---

## 15. Mini Program / Simulation

Frontend quality gate layering:

```python
checks = [
    ("lint", "PR"),
    ("typecheck", "PR"),
    ("unit", "PR"),
    ("build", "PR"),
    ("preview smoke", "important PR"),
    ("full browser matrix", "nightly"),
    ("visual regression", "design-system PR"),
]

for check, when in checks:
    print(f"{check}: {when}")
```

---

## 16. Practical Question

> Design GitHub Actions for a frontend application with PR previews and production CDN deployment.

---

## 17. Strong Answer

I would run fast PR checks first: install with lockfile, lint, typecheck, unit tests, and production build. For important UI changes, I would deploy a preview environment and comment the preview URL on the PR. E2E smoke tests could run against that preview.

For production, I would build immutable assets with content hashes, upload them to the hosting/CDN target, invalidate or short-cache the HTML shell, and keep the previous artifact available for rollback. Secrets would not be embedded into the frontend bundle. Source maps would be uploaded to error tracking and not casually exposed publicly.

For a monorepo, I would use path filters or affected-build tooling so only touched apps run.

---

## 18. Revision Notes

- One-line summary: Frontend CI protects build correctness, UI behavior, performance, previews, and CDN delivery.
- Three keywords: typecheck, preview, CDN
- One interview trap: frontend build-time variables can leak into browser bundles.
- One memory trick: lint/type/test/build protects code; preview protects review; CDN rules protect users.

---

## 19. Official Source Notes

- Node setup action: <https://github.com/actions/setup-node>
- Artifact storage: <https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts>
- Dependency caching: <https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows>
- Deployments and environments: <https://docs.github.com/en/actions/deployment>

