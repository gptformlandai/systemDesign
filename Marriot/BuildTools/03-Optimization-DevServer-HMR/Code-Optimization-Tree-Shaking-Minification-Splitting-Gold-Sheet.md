# Code Optimization: Tree Shaking, Minification, Splitting Gold Sheet

> Topic: production optimization after the graph is known.

---

## 1. Intuition

Optimization is the packing stage before shipping. The build tool removes what is unused, compresses what remains, and splits code so users download the right amount at the right time.

Beginner version:

> Optimization makes the app smaller, faster to download, faster to parse, and easier to cache.

---

## 2. Definition

- Definition: Code optimization is the set of build-time techniques that reduce shipped code size and improve loading behavior.
- Category: Production build optimization.
- Core idea: Remove, compress, split, and cache.

---

## 3. Internal Flow

```txt
Dependency graph
      |
      v
Mark used exports
      |
      v
Remove dead code
      |
      v
Split chunks
      |
      v
Minify output
      |
      v
Hash filenames
      |
      v
Emit assets + sourcemaps
```

Optimization is usually heavier in production than in development because it takes more CPU time.

---

## 4. Tree Shaking

Tree shaking removes unused exports.

Input:

```ts
// math.ts
export function add(a: number, b: number) {
  return a + b;
}

export function debugMath() {
  console.log('debug');
}
```

```ts
import { add } from './math';

console.log(add(2, 3));
```

Output idea:

```js
function add(a, b) {
  return a + b;
}

console.log(add(2, 3));
```

`debugMath` can be removed if the bundler proves it is unused and safe to remove.

---

## 5. Side Effects

Tree shaking must preserve code that changes global state.

```ts
// polyfills.ts
import 'core-js/features/promise';
```

This import has side effects even if it exports nothing.

Risky package metadata:

```json
{
  "sideEffects": false
}
```

If a package marks everything as side-effect free but contains CSS imports or global setup files, required code may disappear.

---

## 6. Minification

Minification reduces output size.

Before:

```js
function calculateTotal(items) {
  const total = items.reduce((sum, item) => sum + item.price, 0);
  return total;
}
```

After:

```js
function calculateTotal(t){return t.reduce((t,r)=>t+r.price,0)}
```

Minifiers may:

- Remove whitespace.
- Shorten local variable names.
- Remove unreachable code.
- Inline constants.
- Drop debug branches when configured.

---

## 7. Code Splitting

Code splitting creates multiple chunks instead of one file.

```tsx
import { lazy, Suspense } from 'react';

const HeavyChart = lazy(() => import('./HeavyChart'));

export function Analytics() {
  return (
    <Suspense fallback={<p>Loading chart...</p>}>
      <HeavyChart />
    </Suspense>
  );
}
```

Pipeline:

```txt
Analytics route loaded
      |
      v
Initial chunk renders shell
      |
      v
Dynamic import requests chart chunk
      |
      v
Suspense fallback replaced by chart
```

---

## 8. Lazy Loading

Lazy loading delays loading code or assets until needed.

Good candidates:

- Admin panels.
- Large charting libraries.
- Rich text editors.
- Maps.
- Payment widgets.
- Modal workflows rarely opened.

Poor candidates:

- Above-the-fold UI.
- Shared layout components.
- Critical auth flow code.

---

## 9. React, Next.js, React Native Notes

### React Web

Use `React.lazy`, route-level splitting, dynamic imports, and bundle analysis.

### Next.js

Next.js automatically splits by routes and framework boundaries. You still need to avoid pulling heavy client-only dependencies into shared layouts.

Example:

```tsx
import dynamic from 'next/dynamic';

const Editor = dynamic(() => import('./Editor'), {
  loading: () => <p>Loading editor...</p>,
});
```

### React Native

React Native optimization is different because code is bundled for an app binary or loaded from a dev server. Metro supports RAM bundles and optimizations, but native app startup, Hermes bytecode, and package size matter more than browser waterfall behavior.

---

## 10. Tool Comparison Lens

| Tool | Optimization Style |
|---|---|
| Webpack | Highly configurable tree shaking, minification, and splitChunks |
| Vite | Fast dev path with optimized production bundling |
| Rollup | Strong ESM tree shaking and clean output |
| esbuild | Very fast minification and bundling |
| Parcel | Built-in optimization with low config |
| Metro | Native bundle and startup-focused optimization |
| Next.js tooling | Route-aware and server/client-aware optimization |

---

## 11. Real-World Example

SaaS analytics dashboard:

```txt
initial
  -> app shell, auth, dashboard summary

lazy chunks
  -> advanced reports
  -> chart editor
  -> export wizard
  -> admin audit logs

vendor
  -> React, router, design system
```

Why:

- Executives checking one KPI should not download the export wizard.
- Admin-only code should not slow regular users.
- Stable dependencies should be cacheable.

---

## 12. Common Mistakes

### Mistake: Importing heavy dependencies at the top level

- Why wrong: Top-level static imports usually enter the initial graph.
- Better approach: Use dynamic imports for heavy optional features.

### Mistake: Assuming tree shaking removes everything unused

- Why wrong: Side effects, CommonJS, dynamic imports, and package metadata can block it.
- Better approach: Verify with bundle analyzer.

### Mistake: Splitting every component

- Why wrong: Too many chunks can cause request overhead and poor UX.
- Better approach: Split at route, feature, and heavy dependency boundaries.

### Mistake: Ignoring parse and execution time

- Why wrong: Small compressed files can still be expensive to parse and execute.
- Better approach: Measure JavaScript execution cost, not only transfer size.

---

## 13. Trade-Offs

| Optimization | Gain | Cost |
|---|---|---|
| Tree shaking | Less unused code | Requires static analyzable modules |
| Minification | Smaller payload | Harder debugging without maps |
| Code splitting | Better first load | More loading states |
| Lazy loading | Less upfront work | Interaction may wait for code |
| Aggressive vendor splitting | Better long-term cache | More complex chunk graph |

---

## 14. Interview Insight

Strong answer:

> Optimization happens after the build tool understands the graph. It marks used exports, removes safe dead code, creates chunks for loading boundaries, minifies output, and emits hashed files for caching. The trade-off is build time and complexity versus runtime performance.

Follow-up trap:

> Our bundle is gzipped, so why care about JavaScript size?

Good answer:

> Compression reduces network transfer, but the browser still has to decompress, parse, compile, and execute JavaScript. Large JavaScript can hurt main-thread responsiveness even when compressed size looks acceptable.

---

## 15. Differential Serving

```html
<!-- serve modern JS to modern browsers, legacy to old browsers -->
<script type="module" src="app.modern.[hash].js"></script>    <!-- ES2020+ -->
<script nomodule src="app.legacy.[hash].js"></script>          <!-- ES5 + polyfills -->
```

Modern browsers: download `app.modern.js` (smaller — no polyfills, minimal transpilation).
IE11/old browsers: download `app.legacy.js` (larger — ES5, polyfills included).

**With Vite (`@vitejs/plugin-legacy`):**

```javascript
// vite.config.ts
import legacy from '@vitejs/plugin-legacy';

export default defineConfig({
  plugins: [
    legacy({
      targets: ['defaults', 'not IE 11'],
      additionalLegacyPolyfills: ['regenerator-runtime/runtime'],
    }),
  ],
});
```

**Bundle size impact:** Modern bundle is typically 15-30% smaller than a legacy bundle. For apps with mostly modern-browser users, this directly improves LCP.

---

## 16. HTTP/2 and Chunk Strategy

HTTP/1.1: each request occupies a connection slot. More chunks = more blocking.
HTTP/2: multiplexed — all requests run over one connection in parallel.

**HTTP/2 changes the chunk calculus:**
- HTTP/1.1: fewer, larger chunks preferred (reduce request overhead)
- HTTP/2: more, granular chunks can improve cache efficiency (only invalidate changed chunk)

**Practical implication with Webpack:**

```javascript
// HTTP/2 project — finer chunks for better cache granularity
splitChunks: {
  maxInitialRequests: 30,     // allow many parallel initial requests
  maxAsyncRequests: 30,
  minSize: 10000,             // allow smaller chunks (10KB vs 30KB default)
  cacheGroups: {
    reactVendors: { test: /react/, name: 'react', chunks: 'all', priority: 30 },
    routerVendors: { test: /react-router/, name: 'router', chunks: 'all', priority: 20 },
    // Each stays cached independently when the other updates
  }
}
```

---

## 17. `sideEffects: false` — The Full Explanation

```json
// package.json
{
  "sideEffects": false
}
// means: every JS file in this package is a pure module —
// importing it without using any export is safe to remove entirely
```

**Without `sideEffects: false`:**
```
import { Button } from '@company/ui';
// Bundler must include ALL code from ui/index.ts (might re-export many things)
// because any of them could have side effects
```

**With `sideEffects: false`:**
```
import { Button } from '@company/ui';
// Bundler only includes Button.ts and its deps — everything else is dropped
```

**The CSS trap:**
```json
// WRONG — breaks CSS imports:
{ "sideEffects": false }

// CORRECT:
{ "sideEffects": ["**/*.css", "**/*.scss", "./src/polyfills.js"] }
```

CSS imports (`import './button.css'`) are side effects — they modify styles when imported even if they export nothing.

---

## 18. Revision Notes

- One-line summary: Production optimization removes unused code, compresses output, and splits loading work.
- Three keywords: shake, minify, split.
- One interview trap: Small transfer size does not guarantee fast execution.
- Memory trick: Production build is packing for travel, not writing code.
