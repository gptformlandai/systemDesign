# Build Performance Pipeline: Cold Start, Hot Updates, Runtime Cost Gold Sheet

> Topic: cold start vs hot updates, build time vs runtime performance, and bundling trade-offs.

---

## 1. Intuition

Build performance has two audiences: developers and users. Developers care how fast the project starts and updates. Users care how fast the app loads, runs, and responds.

Beginner version:

> A fast dev build is for the team. A fast production bundle is for the user. They are related, but not the same.

---

## 2. Definition

- Definition: Build performance is the efficiency of the toolchain in starting, rebuilding, optimizing, and delivering application code.
- Category: Developer experience and production performance.
- Core idea: Optimize the right phase for the right audience.

---

## 3. Performance Pipeline

```txt
Developer changes file
       |
       v
Cold start cost
  config + graph + dependency optimization
       |
       v
Hot update cost
  changed module + affected dependents
       |
       v
Production build cost
  full graph + optimize + emit
       |
       v
Runtime cost
  download + parse + compile + execute
```

---

## 4. Cold Start

Cold start is the time from starting the dev server to usable app.

Expensive work:

- Loading config.
- Scanning dependencies.
- Building an initial graph.
- Pre-bundling dependencies.
- Starting compilers.
- Warming caches.

Old-style prebuild dev:

```txt
start dev server
  -> build whole app
  -> browser can load
```

On-demand dev:

```txt
start dev server
  -> browser can load shell
  -> transform modules as requested
```

---

## 5. Hot Update

Hot update is the time from saving a file to seeing the change.

```txt
save file
  -> watcher event
  -> invalidate cache
  -> transform changed module
  -> find affected graph area
  -> send HMR payload
  -> runtime applies update
```

Good hot update systems avoid rebuilding unaffected modules.

---

## 6. Production Build Time

Production builds do more work:

- Full graph traversal.
- Dead-code elimination.
- Minification.
- Chunk planning.
- CSS extraction.
- Image processing.
- Sourcemap generation.
- Asset hashing.

That is why production builds can be slower than dev updates.

---

## 7. Runtime Performance

Runtime performance includes:

- Network transfer.
- Decompression.
- JavaScript parse and compile.
- JavaScript execution.
- Rendering.
- Hydration for SSR apps.
- Main-thread contention.

Build decisions affect runtime:

| Build Choice | Runtime Impact |
|---|---|
| Smaller initial chunk | Faster startup |
| Too many chunks | Network waterfalls |
| Heavy polyfills | More parse and execution |
| Poor tree shaking | More unused code |
| Bad sourcemap policy | Debugging or security risk |

---

## 8. Metrics To Watch

Developer metrics:

- Dev server startup time.
- Time to first page load in dev.
- Hot update time.
- Production build time.
- CI build time.
- Cache hit rate.

User metrics:

- JavaScript transfer size.
- Parsed JavaScript size.
- LCP.
- INP.
- CLS.
- Time to interactive.
- Route transition latency.

---

## 9. React, Next.js, React Native Notes

### React Web

Watch initial bundle size, route chunk size, and heavy client dependencies.

### Next.js

Watch server/client boundary mistakes. Accidentally marking large areas as client components can increase shipped JavaScript.

### React Native

Watch app startup, JS bundle size, Hermes bytecode size, Metro transform time, and device performance. Browser-style chunk waterfalls are less central than native startup and bridge/runtime cost.

---

## 10. Tool Comparison Lens

| Tool | Performance Strength | Watch Carefully |
|---|---|---|
| Vite | Fast dev startup and hot updates | Production chunk strategy |
| Webpack | Deep optimization control | Cold start and config complexity |
| esbuild | Raw transform/minify speed | Ecosystem fit |
| Rollup | Library output quality | App dev experience without surrounding tooling |
| Turbopack | Incremental framework builds | Compatibility with project needs |
| Metro | React Native platform workflow | Monorepo watching and native startup |

---

## 11. Real-World Example

Problem:

> A dashboard takes 45 seconds to start in dev and ships 2 MB of JavaScript.

Investigation:

```txt
dev startup
  -> dependency scanning slow
  -> monorepo packages not cached

bundle size
  -> charting library in initial route
  -> date library includes all locales
```

Fix:

- Configure workspace caching.
- Lazy-load advanced charts.
- Replace or configure large date dependency.
- Add bundle size budget in CI.

---

## 12. Common Mistakes

### Mistake: Optimizing dev speed while ignoring production payload

- Why wrong: Developers feel faster, but users suffer.
- Better approach: track both dev and runtime metrics.

### Mistake: Optimizing bundle size only by gzip

- Why wrong: JavaScript parse and execution still cost CPU.
- Better approach: inspect uncompressed and parsed JS size too.

### Mistake: Manual chunking without measurement

- Why wrong: It can create cache and request problems.
- Better approach: use analyzer output and real performance traces.

### Mistake: Treating CI cache misses as random

- Why wrong: Cache key design controls build repeatability.
- Better approach: cache package manager and build tool outputs intentionally.

---

## 13. Trade-Offs

| Goal | Improves | Can Hurt |
|---|---|---|
| Fast cold start | Developer onboarding | Less initial graph validation |
| Fast HMR | Iteration speed | Complex invalidation logic |
| Aggressive minification | User payload | Build time and debug readability |
| More code splitting | Initial load | Request waterfalls |
| Broad browser support | Compatibility | Bundle size |
| CI caching | Build speed | Cache correctness |

---

## 14. Interview Insight

Strong answer:

> Build performance must be split into dev feedback performance and production runtime performance. Dev speed is about cold start, caching, file watching, and HMR invalidation. Production performance is about graph optimization, chunk strategy, minification, and how much JavaScript the user downloads, parses, and executes.

Follow-up trap:

> The production build got slower after adding optimization. Is that bad?

Good answer:

> Not automatically. A slower build may be acceptable if it significantly improves user-facing payload and runtime performance. The trade-off depends on CI cost, release frequency, and user impact.

---

## 15. Webpack 5 Filesystem Cache Config

```javascript
// webpack.config.js — production cache config
const path = require('path');

module.exports = {
  cache: {
    type: 'filesystem',
    cacheDirectory: path.resolve(__dirname, '.webpack-cache'),
    buildDependencies: {
      config: [
        __filename,                           // webpack.config.js itself
        path.resolve(__dirname, 'babel.config.json'),
        path.resolve(__dirname, '.browserslistrc'),
      ],
    },
    // Invalidate cache when any of these env variables change:
    version: [
      process.env.NODE_ENV,
      process.env.npm_package_version,
    ].join('-'),
  },
};
```

**CI usage:**

```yaml
# .github/workflows/ci.yml
- name: Cache webpack build
  uses: actions/cache@v4
  with:
    path: .webpack-cache
    key: webpack-${{ runner.os }}-${{ hashFiles('package-lock.json', 'webpack.config.js') }}
    restore-keys: webpack-${{ runner.os }}-
```

---

## 16. Vite Dependency Pre-optimization

Vite pre-bundles `node_modules` with esbuild on first startup. The result is stored in `node_modules/.vite/deps/`.

```javascript
// vite.config.ts — control pre-optimization
export default defineConfig({
  optimizeDeps: {
    include: [
      'moment',            // force pre-bundle (CJS-only packages)
      'lodash/debounce',  // pre-bundle specific subpath
    ],
    exclude: [
      '@company/ui',       // don't pre-bundle (monorepo source package)
    ],
    esbuildOptions: {
      target: 'es2020',
    },
  },
});
```

**When to add to `include`:**
- Package is CommonJS and causes "not a function" errors in dev
- Package has many tiny internal modules causing 100s of requests
- After clearing `.vite` cache still produces errors

**Clearing Vite's dep cache:**
```bash
rm -rf node_modules/.vite
npx vite --force   # or pass --force flag to force re-optimization
```

---

## 17. pnpm Store and Content-Addressable Caching

pnpm stores packages once globally in `~/.pnpm-store` and symlinks them into `node_modules`. This means:

1. `npm install` with 50 packages: downloads 50 × N MB
2. `pnpm install` with 50 packages: downloads each unique package once, symlinks

**CI benefit:**

```yaml
# Cache pnpm store between CI runs
- name: Setup pnpm
  uses: pnpm/action-setup@v3
  with:
    version: 9

- name: Get pnpm store directory
  id: pnpm-cache
  run: echo "STORE_PATH=$(pnpm store path)" >> $GITHUB_OUTPUT

- name: Cache pnpm store
  uses: actions/cache@v4
  with:
    path: ${{ steps.pnpm-cache.outputs.STORE_PATH }}
    key: pnpm-store-${{ hashFiles('**/pnpm-lock.yaml') }}
    restore-keys: pnpm-store-
```

---

## 18. Revision Notes

- One-line summary: Build performance has developer-time and user-time dimensions.
- Three keywords: cold, hot, runtime.
- One interview trap: Faster builds do not always mean faster apps.
- Memory trick: Dev speed helps builders; bundle speed helps users.
