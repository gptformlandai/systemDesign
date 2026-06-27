# Module Systems: ES Modules, CommonJS, Tree Shaking Gold Sheet

> Topic: import/export vs require, and why module format affects bundling.

---

## 1. Intuition

Module systems are the rules for how files share code. Bundlers need predictable rules to know what code is used, what can be removed, and how modules should load.

Beginner version:

> Modules are how one file asks for code from another file.

---

## 2. Definition

- Definition: A module system defines how code declares exports and imports dependencies.
- Category: JavaScript runtime and build-time loading model.
- Core idea: Static module structure enables stronger optimization.

---

## 3. ES Modules

ES Modules use `import` and `export`.

```ts
// math.ts
export function add(a: number, b: number) {
  return a + b;
}

export function subtract(a: number, b: number) {
  return a - b;
}
```

```ts
// app.ts
import { add } from './math';

console.log(add(1, 2));
```

Important properties:

- Static imports are analyzable before running code.
- Exports are live bindings.
- Works in modern browsers and Node.js.
- Enables tree shaking more naturally.

---

## 4. CommonJS

CommonJS uses `require` and `module.exports`.

```js
// math.cjs
function add(a, b) {
  return a + b;
}

function subtract(a, b) {
  return a - b;
}

module.exports = { add, subtract };
```

```js
// app.cjs
const { add } = require('./math.cjs');

console.log(add(1, 2));
```

Important properties:

- Historically common in Node.js.
- `require` can be dynamic.
- Harder for bundlers to statically analyze.
- Tree shaking is less reliable.

---

## 5. Static vs Dynamic Analysis

Easy to analyze:

```ts
import { Button } from './Button';
```

Harder to analyze:

```js
const moduleName = process.env.WIDGET;
const widget = require(`./widgets/${moduleName}`);
```

Bundler question:

```txt
Can I know at build time which module is imported?
```

If yes, optimization is easier. If no, the bundler may include more code or create a runtime loader context.

---

## 6. Internal Flow

```txt
Read module
   |
   v
Parse imports and exports
   |
   v
Classify module format
   |
   +-- ESM
   |     -> static dependency edges
   |     -> tree-shaking candidates
   |
   +-- CommonJS
         -> wrapper or conversion
         -> conservative inclusion
```

---

## 7. Impact On Tree Shaking

Tree shaking removes unused exports.

```ts
import { add } from './math';
```

If only `add` is used, a bundler may remove `subtract`.

But if a CommonJS module exports a dynamic object, the bundler may not safely know which properties matter.

```js
module.exports = createExportsDynamically();
```

Result:

- ESM improves static analysis.
- CommonJS often needs conversion or conservative bundling.
- Package metadata matters.

---

## 8. Package Metadata

`package.json` can expose multiple entry styles:

```json
{
  "name": "example-lib",
  "main": "./dist/index.cjs",
  "module": "./dist/index.mjs",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs"
    }
  },
  "sideEffects": false
}
```

Meaning:

- `main`: older CommonJS entry convention.
- `module`: ESM entry convention used by bundlers.
- `exports`: modern controlled package entry map.
- `sideEffects`: tells bundlers whether unused modules can be removed safely.

---

## 9. React, Next.js, React Native Notes

### React Web

Modern apps prefer ESM because it supports better optimization and browser-native dev serving.

### Next.js

Next.js must handle server and client dependencies. A module may be safe on the server but unsafe in the browser if it imports Node-only APIs.

### React Native

Metro resolves packages differently from browser bundlers and pays attention to React Native-specific fields and platform extensions.

Potential issue:

```txt
Package has browser build but no react-native-compatible build.
```

This can break native apps even if the package works in a web React app.

---

## 10. Real-World Example

You import one utility from a large package:

```ts
import { debounce } from 'utility-kit';
```

If `utility-kit` ships ESM and correct side-effect metadata:

```txt
bundle may include only debounce and its dependencies
```

If it ships only CommonJS:

```txt
bundle may include much more of utility-kit
```

This can be the difference between a small route chunk and a bloated vendor chunk.

---

## 11. Common Mistakes

### Mistake: Mixing ESM and CommonJS without understanding boundaries

- Why wrong: You can hit default export interop bugs and larger bundles.
- Better approach: Know package output format and test the production build.

### Mistake: Dynamic require in frontend code

- Why wrong: Bundlers cannot statically know what to include.
- Better approach: Use explicit imports or controlled dynamic imports.

### Mistake: Incorrect `sideEffects: false`

- Why wrong: Required CSS or initialization files may be removed.
- Better approach: Mark side-effect files explicitly.

Example:

```json
{
  "sideEffects": [
    "*.css",
    "./src/polyfills.ts"
  ]
}
```

### Mistake: Importing Node modules into client code

- Why wrong: Browsers and React Native runtimes do not provide Node APIs by default.
- Better approach: Keep server-only code out of client bundles.

---

## 12. Tool Comparison

| Tool | Module Strength |
|---|---|
| Webpack | Handles many formats and legacy patterns |
| Vite | Uses native ESM in dev and optimizes dependencies |
| Rollup | ESM-first and excellent for tree shaking |
| esbuild | Fast ESM/CJS handling |
| Metro | React Native package and platform resolution |

---

## 13. Interview Insight

Strong answer:

> ES Modules are statically analyzable because imports and exports are declared at the top level. That makes dependency graphs, code splitting, and tree shaking easier. CommonJS is flexible and historically important, but dynamic `require` patterns make bundlers more conservative.

Follow-up trap:

> Why did Vite care so much about native ES modules?

Good answer:

> Native ESM lets the browser participate in module loading during development, so the dev server can transform and serve modules on demand instead of building the entire application before startup.

---

## 14. Revision Notes

- One-line summary: Static ESM structure gives bundlers better optimization power.
- Three keywords: import, require, side effects.
- One interview trap: CommonJS can work but may reduce tree-shaking precision.
- Memory trick: ESM gives the bundler a map; dynamic CommonJS gives it a mystery.
