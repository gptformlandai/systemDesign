# Real-World Build Pipeline Design and CI/CD Gold Sheet

> Topic: choosing bundlers, large-scale app build strategy, and CI/CD integration.

---

## 1. Intuition

In real companies, build tooling is an architecture decision. It affects developer speed, release safety, app performance, CI cost, debugging, and team independence.

Beginner version:

> The best build pipeline is the one that fits the product, runtime, team, and release process.

---

## 2. Definition

- Definition: Build pipeline design is the architecture of tools, configs, caching, checks, and deployment steps that turn source code into verified runtime artifacts.
- Category: Frontend platform architecture.
- Core idea: Make builds fast, correct, reproducible, observable, and deployable.

---

## 3. Decision Framework

```txt
What are we building?
    |
    +-- React SPA?
    |      -> Vite, Webpack, Parcel depending constraints
    |
    +-- Next.js app?
    |      -> Next framework build system
    |
    +-- React Native app?
    |      -> Metro
    |
    +-- Shared library?
    |      -> Rollup / esbuild-based package tool
    |
    +-- Enterprise micro-frontend?
           -> Webpack or framework-supported federation strategy
```

---

## 4. Choosing A Bundler

| Project | Strong Default | Why |
|---|---|---|
| New React SPA | Vite | Fast dev, modern defaults |
| Legacy enterprise React | Webpack | Existing ecosystem and custom config |
| Small low-config app | Parcel | Minimal setup |
| Next.js SaaS | Next.js tooling | Routing, SSR, RSC, server/client boundaries |
| React Native mobile app | Metro | Native platform resolution |
| Component library | Rollup or esbuild wrapper | Clean package outputs |
| Tooling package | esbuild | Speed and simple API |

---

## 5. Large-Scale App Strategy

Large apps need more than a bundler:

```txt
package manager
  -> workspace strategy
  -> build cache
  -> type checking
  -> linting
  -> tests
  -> bundle analysis
  -> production build
  -> artifact upload
  -> deployment
  -> monitoring
```

Architecture practices:

- Use feature-based code organization.
- Keep shared packages dependency-light.
- Prevent duplicate React versions.
- Avoid importing server-only code into client packages.
- Add bundle budgets.
- Cache dependency install and build outputs.
- Separate type checking from fast transforms when useful.
- Run production preview smoke tests.

---

## 6. CI/CD Pipeline

```txt
Pull request
   |
   v
Install dependencies
   |
   v
Restore caches
   |
   v
Type check
   |
   v
Lint
   |
   v
Unit/integration tests
   |
   v
Build production output
   |
   v
Bundle budget check
   |
   v
Upload sourcemaps privately
   |
   v
Deploy preview
   |
   v
E2E smoke test
   |
   v
Promote to production
```

---

## 7. Example CI Script Shape

```yaml
name: frontend-ci

on:
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: npm
      - run: npm ci
      - run: npm run typecheck
      - run: npm run lint
      - run: npm test
      - run: npm run build
      - run: npm run bundle:check
```

Adapt the commands to the actual package manager and repo structure.

---

## 8. Environment Variables

Build-time env:

```txt
compiled into bundle or used during build
```

Runtime env:

```txt
read by server or platform at request/start time
```

Common mistake:

```txt
Changing an env var after static build but expecting already-built browser JS to change.
```

For browser bundles, public env values are often embedded during build and are not secret.

---

## 9. Build Artifacts

React SPA artifacts:

```txt
dist/
  index.html
  assets/app.[hash].js
  assets/app.[hash].css
```

Next.js artifacts:

```txt
.next/
  server output
  static chunks
  route manifests
```

React Native artifacts:

```txt
iOS/Android app build
JS bundle or Hermes bytecode
assets
source maps for crash reporting
```

Library artifacts:

```txt
dist/
  index.mjs
  index.cjs
  index.d.ts
```

---

## 10. Real-World Architectures

### SaaS Dashboard

```txt
Vite or Next.js
  -> route-based chunks
  -> bundle budget
  -> sourcemap upload
  -> preview deployments
```

Decision:

- Use Next.js if SEO, SSR, auth at server boundary, or server actions matter.
- Use Vite SPA if internal dashboard is mostly client-rendered and API-driven.

### E-Commerce App

```txt
Next.js
  -> SSR/SSG for product pages
  -> dynamic chunks for cart/checkout
  -> CDN caching
  -> private sourcemaps
```

Decision:

- Optimize product page load and SEO.
- Keep payment widgets client-only when required.

### React Native App

```txt
Metro
  -> platform-specific module resolution
  -> Hermes build
  -> native app CI
  -> sourcemap upload to crash tool
```

Decision:

- Tune Metro config for monorepo packages.
- Watch app startup and device performance.

### Design System

```txt
Rollup
  -> ESM + CJS outputs
  -> preserve types
  -> externalize React
  -> publish package
```

Decision:

- Do not bundle React into the library.
- Ensure consumers can tree shake components.

---

## 11. System Design Discussion

Prompt:

> Design the frontend build pipeline for a large SaaS product used by 200 engineers across multiple teams.

Strong answer structure:

1. Clarify app type: SPA, SSR, hybrid, mobile, or library.
2. Pick framework-aligned tooling.
3. Separate dev speed from production optimization.
4. Define monorepo and package boundaries.
5. Add typecheck, lint, test, build, and bundle-budget gates.
6. Add build caching.
7. Add preview deployments.
8. Upload source maps privately.
9. Monitor runtime metrics and build metrics.
10. Define ownership for build config.

---

## 12. Common Mistakes

### Mistake: Migrating bundlers for trend reasons

- Why wrong: Migration cost and plugin gaps may exceed benefits.
- Better approach: tie migration to measurable pain.

### Mistake: No bundle budget

- Why wrong: Bundle size regresses slowly and silently.
- Better approach: enforce budgets in CI.

### Mistake: One shared package imports everything

- Why wrong: Shared packages can accidentally drag huge dependencies into many chunks.
- Better approach: keep shared utilities modular and dependency-light.

### Mistake: No production-like preview

- Why wrong: Build-only bugs escape to production.
- Better approach: deploy preview and run smoke tests.

### Mistake: Treating mobile and web bundles the same

- Why wrong: React Native has platform-specific resolution, native modules, and app startup constraints.
- Better approach: design Metro and native CI separately.

---

## 13. Trade-Offs

| Decision | Gain | Cost |
|---|---|---|
| Framework defaults | Stability and support | Less custom control |
| Custom build pipeline | Tailored behavior | Maintenance burden |
| Monorepo build cache | Faster CI | Cache invalidation complexity |
| Bundle budgets | Prevent regressions | Needs tuning |
| Private sourcemaps | Better debugging | Extra deploy step |
| Preview deployments | Safer releases | Higher infra cost |

---

## 14. Interview Insight

Strong answer:

> I would design the pipeline around the runtime first. React SPA, Next.js, React Native, and libraries have different output needs. Then I would optimize developer speed through caching and HMR, production quality through chunking and bundle budgets, and release safety through CI checks, preview deployments, sourcemap upload, and monitoring.

Follow-up trap:

> Should every company migrate from Webpack to Vite?

Good answer:

> No. Vite is excellent for many modern SPAs, but a mature Webpack app may depend on Module Federation, custom loaders, or framework integrations. Migration should be justified by measurable developer speed or maintenance gains.

---

## 15. Bundle Budget Enforcement in CI

Prevent bundle regressions by failing CI when bundle size exceeds a threshold.

**Using `bundlesize`:**

```bash
npm install --save-dev bundlesize
```

```json
// package.json
{
  "bundlesize": [
    { "path": "./dist/main.*.js",     "maxSize": "200 kB" },
    { "path": "./dist/vendor.*.js",   "maxSize": "300 kB" },
    { "path": "./dist/main.*.css",    "maxSize": "50 kB" }
  ],
  "scripts": {
    "size": "bundlesize"
  }
}
```

```yaml
# .github/workflows/ci.yml
- name: Build
  run: npm run build

- name: Bundle size check
  run: npm run size
  # Fails CI if any bundle exceeds maxSize
```

**Using Next.js `@next/bundle-analyzer` with size limits:**

```javascript
// scripts/check-bundle-size.js
const { readFileSync } = require('fs');
const path = require('path');

const stats = JSON.parse(readFileSync('.next/build-manifest.json', 'utf8'));
const MAX_PAGE_KB = 150;

Object.entries(stats.pages).forEach(([page, files]) => {
  const totalSize = files.reduce((sum, f) => {
    try {
      return sum + readFileSync(path.join('.next', f)).length;
    } catch { return sum; }
  }, 0);
  const kbs = totalSize / 1024;
  if (kbs > MAX_PAGE_KB) {
    console.error(`Page ${page}: ${kbs.toFixed(0)}KB exceeds ${MAX_PAGE_KB}KB limit`);
    process.exit(1);
  }
});
```

---

## 16. GitHub Actions Build Pipeline — Complete Pattern

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: pnpm/action-setup@v3
        with:
          version: 9

      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'pnpm'

      - name: Install
        run: pnpm install --frozen-lockfile

      # Run type-check and lint in parallel with build
      - name: Type check
        run: pnpm tsc --noEmit
        
      - name: Lint
        run: pnpm eslint src --ext .ts,.tsx

      - name: Test
        run: pnpm vitest run --coverage

      - name: Build
        run: pnpm build
        env:
          NODE_ENV: production

      - name: Bundle size check
        run: pnpm bundlesize

      - name: Upload source maps to Sentry
        run: npx @sentry/cli sourcemaps upload ./dist
        env:
          SENTRY_AUTH_TOKEN: ${{ secrets.SENTRY_AUTH_TOKEN }}
          SENTRY_ORG: ${{ vars.SENTRY_ORG }}

      - name: Remove source maps from artifact
        run: find ./dist -name "*.map" -delete

      - name: Deploy
        run: pnpm deploy:production
        if: github.ref == 'refs/heads/main'
```

---

## 17. Revision Notes

- One-line summary: Real build architecture starts from runtime and product constraints.
- Three keywords: runtime, cache, deploy.
- One interview trap: Tool choice without constraints is shallow.
- Memory trick: Choose the pipeline by the artifact you need to ship.
