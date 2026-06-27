# Build Tools — Production Debugging Case Studies — Gold Sheet

> Format: incident narrative → symptoms → investigation → root cause → fix → lesson

---

## How to Use This File

Read each case study as if it happened to your production app. After reading the symptoms, pause and think: what would your first three investigation steps be? Then read the investigation section to see the actual approach.

---

## Case Study 1: The Mystery 5MB Bundle

### Symptoms

- Production build completes successfully
- Chrome DevTools Network tab shows `main.[hash].js` at 4.8MB (gzipped: 1.6MB)
- Initial page load takes 8.2 seconds on a 4G connection
- Performance score: 34 (Lighthouse)
- Team added `moment` and `lodash` last sprint "just to be safe"

### Investigation

**Step 1: Analyze the bundle**

```bash
# Install bundle analyzer
npm install --save-dev webpack-bundle-analyzer

# Add to webpack config
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
plugins: [new BundleAnalyzerPlugin()]

npm run build
# Opens browser with treemap visualization
```

**Step 2: Read the treemap**

```
Main bundle breakdown:
  moment/          → 232KB (locales included)
  lodash/          → 72KB  (all utilities)
  src/             → 48KB
  react + react-dom → 130KB
```

**Step 3: Identify why full moment is included**

```javascript
// src/utils/dates.ts
import moment from 'moment';   // imports entire moment + all 160 locale files
```

### Root Cause

1. `import moment from 'moment'` pulls the full moment package including all locale files (160 locales ≈ 195KB)
2. `import _ from 'lodash'` pulls the entire lodash library (72KB)

### Fix

**Replace moment with date-fns (tree-shakeable):**

```bash
npm uninstall moment
npm install date-fns
```

```typescript
// Before: 232KB
import moment from 'moment';
const formatted = moment(date).format('MMM D, YYYY');

// After: ~2KB (only the format and parse functions imported)
import { format } from 'date-fns';
const formatted = format(date, 'MMM d, yyyy');
```

**Replace lodash with lodash-es or cherry-picked imports:**

```typescript
// Before: 72KB
import _ from 'lodash';
const unique = _.uniq(array);

// After: ~1KB
import { uniq } from 'lodash-es';  // tree-shakeable ESM version
```

**Result:** Bundle dropped from 4.8MB → 780KB. Lighthouse score: 34 → 78.

### Lesson

> Never install a utility library without checking its bundle size and whether it supports tree shaking. Use `bundlephobia.com` before installing. Libraries with ESM `exports` and no sideEffects are tree-shakeable; CommonJS libraries are not.

---

## Case Study 2: HMR Stopped Working

### Symptoms

- Developers report: saving a file no longer triggers fast updates in the browser
- Full page reload happens instead of HMR
- Happens in a Next.js 14 app after upgrading from Next.js 13
- Error in terminal: `[webpack-hmr] HMR update failed. Reloading page.`

### Investigation

**Step 1: Check the error in detail**

```
[Fast Refresh] had to do a full reload due to a runtime error.
Error: Hooks can only be called inside of the body of a function component.
    at Counter (Counter.tsx:12)
```

**Step 2: Check what changed in the component**

```typescript
// Counter.tsx — someone added a conditional hook
export function Counter() {
  const [count, setCount] = useState(0);
  
  if (count > 10) {
    const [doubled, setDoubled] = useState(count * 2);  // RULES OF HOOKS VIOLATION
    return <div>{doubled}</div>;
  }
  
  return <button onClick={() => setCount(c => c + 1)}>{count}</button>;
}
```

**Step 3: Confirm it's not a tooling issue**

```bash
# Check React Fast Refresh is configured
# In Next.js 14, this is built-in — no config needed
# Verify in next.config.js:
console.log(process.env.NEXT_DISABLE_REACT_FAST_REFRESH)  // should be undefined
```

### Root Cause

React Fast Refresh falls back to full reload when a component throws during re-render. The Rules of Hooks violation causes React to throw during the HMR update, triggering a full reload. The symptom (HMR not working) masked the actual bug (Rules of Hooks).

### Fix

```typescript
// Counter.tsx — fix the Rules of Hooks violation
export function Counter() {
  const [count, setCount] = useState(0);
  const [doubled, setDoubled] = useState(0);  // always call at top level
  
  useEffect(() => {
    if (count > 10) setDoubled(count * 2);
  }, [count]);
  
  if (count > 10) {
    return <div>{doubled}</div>;
  }
  
  return <button onClick={() => setCount(c => c + 1)}>{count}</button>;
}
```

### Lesson

> When HMR falls back to full reload, the root cause is usually a runtime error during re-render — not a bundler issue. Always check the browser console and terminal for error details before investigating build config. Add `eslint-plugin-react-hooks` to catch Rules of Hooks violations at lint time.

---

## Case Study 3: Source Maps Broken in Production

### Symptoms

- Sentry error reports show minified stack traces: `n.e(t).then(n.bind(n, 3547))` instead of readable code
- Error occurred in `CheckoutPage.tsx` line 47, but Sentry shows `main.js` line 1 column 847
- Was working last month; broke after a webpack config change

### Investigation

**Step 1: Check if source maps are being generated**

```bash
ls -la dist/
# main.[hash].js        4.2MB
# main.[hash].js.map    → NOT PRESENT
```

**Step 2: Find the webpack config change**

```bash
git diff HEAD~5 webpack.config.js
```

```diff
-  devtool: 'source-map',
+  devtool: false,
```

Someone disabled source maps to speed up the build. But the team did not realize source maps are consumed by Sentry at upload time, not served to users.

**Step 3: Check Sentry upload**

```bash
# Sentry source map upload in CI:
npx @sentry/cli sourcemaps upload ./dist --url-prefix '~/'
# Error: no .map files found in ./dist
```

### Root Cause

`devtool: false` disables source map generation. Without `.map` files, Sentry cannot translate minified stack traces back to original source.

### Fix

```javascript
// webpack.config.js
module.exports = {
  devtool: process.env.NODE_ENV === 'production' 
    ? 'hidden-source-map'    // generates .map file but doesn't link it in JS (users can't access)
    : 'eval-source-map',     // fast source maps in dev
};
```

**`hidden-source-map`** generates the `.map` file without adding `//# sourceMappingURL=` comment to the bundle — so browsers/users cannot access it, but Sentry can upload it.

```yaml
# CI step: upload source maps to Sentry, then delete them
- name: Upload source maps
  run: npx @sentry/cli sourcemaps upload ./dist

- name: Delete source maps from deploy artifact
  run: find ./dist -name "*.map" -delete
```

### Lesson

> Always use `hidden-source-map` in production, not `false`. Upload maps to your error tracker and delete them from the deploy artifact. Source maps should never be publicly accessible — they expose your original source code.

---

## Case Study 4: The 12-Minute CI Build

### Symptoms

- GitHub Actions CI pipeline takes 12+ minutes per run
- Every PR triggers a full rebuild of all 23 packages
- Team: 8 engineers, 15+ PRs per day = 3+ hours of CI compute
- Turborepo is installed but cache hit rate: 0%

### Investigation

**Step 1: Check Turborepo cache hit rate**

```bash
turbo build --summarize
# cache status: MISS for all 23 packages
# every single time
```

**Step 2: Check turbo.json inputs**

```json
// turbo.json (problematic)
{
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": [".next/**", "dist/**"]
      // ← NO inputs field
    }
  }
}
```

**Step 3: Understand the default inputs behavior**

Without an `inputs` field, Turborepo defaults to hashing **all files in the package** including `.gitignore`d files, lock files, and anything that changes between runs — like `node_modules/.cache`.

**Step 4: Check for non-deterministic inputs**

```bash
# Check what Turborepo is hashing
turbo build --dry-run --verbosity 2 | grep "hash"
# Shows: node_modules/.cache/babel-loader/ is included in hash
# This changes every run even if source didn't change
```

### Root Cause

1. No `inputs` field → Turborepo hashes everything → Babel loader cache files change every run → hash never matches → cache always misses
2. Remote caching not configured → each CI runner starts from scratch

### Fix

**Step 1: Add explicit inputs to turbo.json**

```json
{
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "inputs": [
        "src/**",
        "app/**",
        "public/**",
        "next.config.*",
        "tsconfig.json",
        "package.json",
        "tailwind.config.*"
      ],
      "outputs": [".next/**", "!.next/cache/**", "dist/**"]
    }
  }
}
```

**Step 2: Configure remote caching**

```yaml
# .github/workflows/ci.yml
- name: Build
  run: turbo build
  env:
    TURBO_TOKEN: ${{ secrets.TURBO_TOKEN }}    # Vercel remote cache token
    TURBO_TEAM: ${{ vars.TURBO_TEAM }}         # Vercel team name
```

**Result:** Cache hit rate went to 87%. Average CI time dropped from 12 minutes to 2.3 minutes.

### Lesson

> Always define explicit `inputs` in `turbo.json`. The default (hash everything) includes generated files that change per run, defeating the cache. Remote caching is essential for CI — local cache doesn't help CI runners that start fresh.

---

## Case Study 5: Metro Resolution Failure in React Native

### Symptoms

- React Native Metro bundler fails to start
- Error: `Unable to resolve module @company/ui from /app/screens/HomeScreen.tsx`
- Works fine on developer A's machine, fails on developer B's machine and in CI
- `@company/ui` exists in `node_modules/@company/ui`

### Investigation

**Step 1: Verify the module exists**

```bash
ls node_modules/@company/ui/
# dist/
# package.json
# src/
```

**Step 2: Check package.json main/exports**

```json
// node_modules/@company/ui/package.json
{
  "name": "@company/ui",
  "main": "dist/index.cjs",
  "module": "dist/index.mjs",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs"
    }
  }
}
```

**Step 3: Check Metro config**

```javascript
// metro.config.js — missing resolver config
const { getDefaultConfig } = require('expo/metro-config');
const config = getDefaultConfig(__dirname);
module.exports = config;
```

**Step 4: Find root cause by comparing machines**

Developer A has `dist/index.cjs` in their `node_modules`. Developer B's install is missing it — the package's `prepare` script (which runs `tsup build`) failed silently.

```json
// @company/ui/package.json
{
  "scripts": {
    "prepare": "tsup src/index.ts"  // ← runs on npm install
  }
}
```

The `prepare` script requires `tsup` as a devDependency — but in CI, `NODE_ENV=production` causes npm to skip devDependency installs, so `tsup` is not present and the `prepare` script fails silently.

### Root Cause

`@company/ui`'s `prepare` script uses `tsup` to build the `dist/` folder. In CI with `NODE_ENV=production`, devDependencies are skipped, `tsup` is absent, `prepare` fails silently, and `dist/` is empty. Metro cannot resolve the main entry point.

### Fix

**Option 1: Publish pre-built dist to npm (recommended for shared packages)**

```bash
cd packages/ui
npm run build          # builds dist/ locally
npm publish            # publish the pre-built dist/
```

Remove `prepare` script — the package is published with dist already included.

**Option 2: Force devDependency install in CI**

```yaml
# .github/workflows/ci.yml
- name: Install dependencies
  run: npm install --include=dev  # override NODE_ENV production behavior
```

**Option 3: Add Metro resolution fallback**

```javascript
// metro.config.js
module.exports = {
  resolver: {
    sourceExts: ['js', 'jsx', 'ts', 'tsx', 'cjs', 'mjs'],
    resolverMainFields: ['react-native', 'browser', 'main'],
  },
};
```

### Lesson

> Never rely on `prepare` scripts for packages consumed by Metro. Metro has a different resolver than Node.js and webpack — it does not understand `exports` field by default, and `prepare` scripts are fragile in CI. Publish packages with pre-built dist/ folders.

---

## Revision Notes

- One-line summary: Every production build incident has a traceable root cause — bundle size (import analysis), HMR failures (runtime errors), source maps (devtool config), slow CI (cache inputs), Metro failures (prepare scripts).
- Three keywords: bundle-analyzer, cache-inputs, hidden-source-map.
- One interview trap: `devtool: false` looks like it speeds up builds, but it breaks error monitoring.
- Memory trick: The 5 cases — Fat Bundle, HMR Lie, Dark Maps, Cache Ghost, Metro Ghost.
