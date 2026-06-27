# Build Tools — Tricky Scenario Questions — Gold Sheet

> Format: "What's wrong with this config?" + output prediction + diagnosis

---

## How to Use This File

For each scenario: read the config/code, predict the problem or output, then check the explanation. Cover the explanation with your hand or a card — don't peek before thinking.

---

## Webpack Scenarios

---

### WP-1 — The Missing publicPath

```javascript
// Remote micro-frontend webpack config
const { ModuleFederationPlugin } = require('webpack').container;

module.exports = {
  mode: 'production',
  plugins: [
    new ModuleFederationPlugin({
      name: 'checkout',
      filename: 'remoteEntry.js',
      exposes: { './CheckoutPage': './src/pages/CheckoutPage' },
      shared: { react: { singleton: true } },
    }),
  ],
};
```

**What is wrong?**

<details>
<summary>Answer</summary>

`publicPath` is missing. When the shell loads `remoteEntry.js`, subsequent chunk requests will use relative URLs resolved from the shell's domain — not the checkout app's domain. All async chunks from the remote will 404.

Fix:
```javascript
output: {
  publicPath: 'https://checkout.example.com/',  // absolute URL required
}
```

</details>

---

### WP-2 — The Duplicate React Trap

```javascript
// Both shell and checkout remote have this shared config:
shared: {
  react: { requiredVersion: '^18.0.0' },
  'react-dom': { requiredVersion: '^18.0.0' },
}
```

```
// Error in browser console:
// "Hooks can only be called inside of the body of a function component."
// Error appears only when CheckoutPage renders its own useState
```

**What is wrong?**

<details>
<summary>Answer</summary>

`singleton: true` is missing. Without it, Webpack creates two separate React instances — one in the shell and one in the remote. React hooks require a single React instance to work correctly. The shell's `useState` uses one React, the remote's component uses another.

Fix:
```javascript
react: { singleton: true, requiredVersion: '^18.0.0' }
```

</details>

---

### WP-3 — The Stale splitChunks

```javascript
// webpack.config.js
module.exports = {
  optimization: {
    splitChunks: {
      chunks: 'async',      // only async chunks
      minSize: 30000,
      cacheGroups: {
        vendor: {
          test: /node_modules/,
          name: 'vendors',
          chunks: 'all',    // override parent
        }
      }
    }
  }
};
```

**Predict the outcome:** The app deploys. Users clear cache. Users revisit two weeks later. A new React version was deployed. What do users download?

<details>
<summary>Answer</summary>

Users re-download everything. The `vendors` chunk is named `vendors` (static name, no hash). When React updates, the content changes but the filename stays `vendors.js`. The browser has `vendors.js` cached from two weeks ago and serves the old version.

Fix:
```javascript
vendor: {
  test: /node_modules/,
  name: false,          // or use contenthash in filename
  chunks: 'all',
}
// and in output:
filename: '[name].[contenthash].js',
chunkFilename: '[name].[contenthash].js',
```

</details>

---

### WP-4 — The Loader Order Gotcha

```javascript
module: {
  rules: [
    {
      test: /\.scss$/,
      use: [
        'css-loader',
        'sass-loader',
        'style-loader',   // WRONG ORDER
      ]
    }
  ]
}
```

**What happens?**

<details>
<summary>Answer</summary>

Webpack loaders execute **right to left**. This config runs `style-loader` first, then `sass-loader`, then `css-loader`. `style-loader` expects CSS strings as input, not SCSS. Build fails.

Fix (correct order — rightmost runs first):
```javascript
use: [
  'style-loader',   // 3. inject CSS into DOM
  'css-loader',     // 2. resolve @import and url()
  'sass-loader',    // 1. compile SCSS → CSS
]
```

</details>

---

### WP-5 — The DefinePlugin Type Error

```javascript
// webpack.config.js
new webpack.DefinePlugin({
  API_URL: 'https://api.example.com',
  DEBUG: true,
})

// src/app.js
console.log(API_URL);   // what does this output?
```

**What is wrong?**

<details>
<summary>Answer</summary>

`API_URL` is set to `https://api.example.com` (a string of JS code, not a JSON string). Webpack performs literal text substitution: `API_URL` in code becomes `https://api.example.com` — which is a syntax error (`:` is not valid in an expression).

Fix:
```javascript
new webpack.DefinePlugin({
  API_URL: JSON.stringify('https://api.example.com'),  // '"https://api.example.com"'
  DEBUG: JSON.stringify(true),
})
```

</details>

---

## Vite Scenarios

---

### VT-1 — The VITE_ Prefix Rule

```typescript
// .env
REACT_APP_API_URL=https://api.example.com
SECRET_KEY=super-secret

// src/api.ts
const apiUrl = import.meta.env.REACT_APP_API_URL;
console.log(apiUrl);   // what is this?
```

**What is the output?**

<details>
<summary>Answer</summary>

`undefined`. Vite only exposes environment variables prefixed with `VITE_` to client-side code. `REACT_APP_API_URL` is a Create React App convention — Vite ignores it.

Fix:
```
# .env
VITE_API_URL=https://api.example.com
```
```typescript
const apiUrl = import.meta.env.VITE_API_URL;
```

Note: `SECRET_KEY` correctly remains undefined in the client — it should never be exposed.

</details>

---

### VT-2 — The CommonJS Plugin Problem

```javascript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
});

// src/app.ts
import moment from 'moment';  // moment uses CommonJS

// Error: "moment is not a function"
// Error only appears in development, not production
```

**What is the problem and why does it only appear in dev?**

<details>
<summary>Answer</summary>

Vite's dev server uses native ESM — it does not bundle or transform CommonJS modules during development unless they are pre-bundled by esbuild. `moment` uses CommonJS `require()` which browsers cannot handle natively.

Vite pre-bundles `node_modules` with esbuild, but the error suggests something went wrong (stale cache, or a sub-dependency issue).

Fix: Clear Vite's pre-bundle cache:
```bash
rm -rf node_modules/.vite
```
If persistent, add to `optimizeDeps.include`:
```javascript
optimizeDeps: {
  include: ['moment'],
}
```
Production works because Rollup handles CJS-to-ESM conversion during the build.

</details>

---

### VT-3 — The Dynamic Import Alias

```javascript
// vite.config.ts
resolve: {
  alias: {
    '@': path.resolve(__dirname, './src'),
  }
}

// src/utils/loader.ts
const modulePath = '@/features/' + featureName + '/index.ts';
const module = await import(modulePath);  // does this work?
```

**What happens?**

<details>
<summary>Answer</summary>

This fails at runtime. Vite (and Rollup) resolve aliases **statically at build time**. A dynamic import with a runtime string concatenation using an alias cannot be resolved — Vite cannot know which files to include in the bundle.

Fix 1: Use a static glob pattern that Vite can analyze:
```javascript
// Vite handles this: static prefix + dynamic suffix
const module = await import(`../features/${featureName}/index.ts`);
// Rollup includes all matched files
```

Fix 2: Use `import.meta.glob` for explicit dynamic loading:
```typescript
const modules = import.meta.glob('../features/*/index.ts');
const module = await modules[`../features/${featureName}/index.ts`]();
```

</details>

---

## TypeScript / tsconfig Scenarios

---

### TS-1 — The moduleResolution Mismatch

```json
// tsconfig.json
{
  "compilerOptions": {
    "module": "ESNext",
    "moduleResolution": "Node"   // ← old resolution algorithm
  }
}
```

```typescript
// src/utils.ts — a package with an exports field
import { formatDate } from 'date-fns/format';
// Error: "Cannot find module 'date-fns/format'"
// But it works fine when running the built code
```

**What is the problem?**

<details>
<summary>Answer</summary>

`moduleResolution: "Node"` does not understand the `exports` field in `package.json`. `date-fns` v3 uses `package.json exports` to expose subpaths like `date-fns/format`. TypeScript's old Node resolution algorithm ignores `exports` and looks for a literal file `date-fns/format.js` — which doesn't exist.

Fix:
```json
{
  "compilerOptions": {
    "module": "ESNext",
    "moduleResolution": "Bundler"   // or "Node16" / "NodeNext"
  }
}
```

</details>

---

### TS-2 — The isolatedModules Constraint

```typescript
// src/types.ts
export type UserRole = 'admin' | 'user' | 'guest';

// src/index.ts
import { UserRole } from './types';  // works fine with tsc
// But Babel / esbuild / SWC error: "Re-export of a type requires isolatedModules"
```

**Why does tsc succeed but the bundler fails?**

<details>
<summary>Answer</summary>

Bundlers (Babel, esbuild, SWC) transpile each file independently — they cannot perform cross-file type analysis. Without `isolatedModules: true`, TypeScript allows importing a type and re-exporting it, but bundlers don't know whether `UserRole` is a type or a value and can't safely strip it.

Fix: Enable `isolatedModules: true` (required when using bundlers) and use type-only imports:
```typescript
import type { UserRole } from './types';  // explicit type import
// or export directly:
export type { UserRole } from './types';
```

With `isolatedModules: true`, TypeScript itself will warn about this pattern before the bundler rejects it.

</details>

---

### TS-3 — The Path Alias Build Hole

```json
// tsconfig.json
{
  "compilerOptions": {
    "paths": { "@components/*": ["./src/components/*"] }
  }
}
```

```typescript
// app.ts — TypeScript is happy, no errors
import { Button } from '@components/Button';
```

```bash
# tsup build output:
# Error: Cannot find module '@components/Button'
```

**Why does TypeScript succeed but the build fail?**

<details>
<summary>Answer</summary>

TypeScript's `paths` only teaches the TypeScript compiler how to resolve types — it does NOT transform import paths in the output. The compiled JavaScript still contains `import { Button } from '@components/Button'`, which Node.js and bundlers cannot resolve.

Fix for library builds with tsup:
```javascript
// tsup.config.ts
import { defineConfig } from 'tsup';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig({
  entry: ['src/index.ts'],
  plugins: [tsconfigPaths()],  // applies path rewriting at build time
});
```

Fix for apps in Vite:
```javascript
// vite.config.ts — Vite handles this automatically via resolve.alias:
resolve: {
  alias: { '@components': path.resolve(__dirname, './src/components') },
}
// AND tsconfig.json paths must mirror this alias
```

</details>

---

## Build Output Prediction Scenarios

---

### BO-1 — Predict the sideEffects Behavior

```json
// packages/ui/package.json
{
  "sideEffects": false
}
```

```typescript
// packages/ui/src/index.ts
export { Button } from './Button';
export { Modal } from './Modal';

// Button.ts
import './button.css';   // side effect!
export const Button = ...;
```

**Predict what happens when the app only imports Button:**

<details>
<summary>Answer</summary>

`button.css` is dropped from the bundle. `sideEffects: false` tells the bundler that no file in this package has side effects — so importing `Button.ts` but not using anything from `button.css` is "safe to tree-shake." The CSS import is treated as dead code.

This is a real bug in many design systems. Fix:
```json
{
  "sideEffects": ["**/*.css", "**/*.scss"]
}
```
This tells bundlers: "JS files have no side effects, but CSS files do — keep them."

</details>

---

### BO-2 — The CommonJS Export Pattern

```javascript
// lib/utils.js
module.exports = {
  formatDate: (d) => d.toISOString(),
  parseDate: (s) => new Date(s),
};

// app.js (ESM, bundled with Rollup)
import { formatDate } from './lib/utils.js';
```

**Will tree shaking remove `parseDate`?**

<details>
<summary>Answer</summary>

No. CommonJS `module.exports` is a runtime assignment — the bundler cannot statically analyze which properties will be used. The entire `module.exports` object is included.

Tree shaking only works with static ESM `export` statements. To enable tree shaking:
```javascript
// lib/utils.js — convert to ESM
export const formatDate = (d) => d.toISOString();
export const parseDate = (s) => new Date(s);
```
Now Rollup/Webpack can detect that `parseDate` is never imported and drop it.

</details>

---

## Revision Notes

- One-line summary: Config mistakes have predictable consequences — trace the data flow to diagnose.
- Three keywords: loader-order, singleton, sideEffects.
- One interview trap: Webpack loader execution order is right-to-left, not left-to-right.
- Memory trick: For loaders, read the `use` array like a pipeline that reads bottom-to-top.
