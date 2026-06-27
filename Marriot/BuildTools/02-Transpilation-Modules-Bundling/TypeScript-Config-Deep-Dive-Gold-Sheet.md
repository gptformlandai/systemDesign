# TypeScript Config Deep Dive — Gold Sheet

> Topic: tsconfig.json internals, compiler options, path aliases, composite projects, bundler integration

---

## 1. Intuition

`tsconfig.json` is the control panel for TypeScript. It tells the compiler what code to include, how strictly to check it, what JavaScript version to emit, how to resolve imports, and how to generate declaration files. Every bundler that uses TypeScript reads this file.

Beginner version:

> `tsconfig.json` is the settings file that tells TypeScript what your project is and how strictly to check it.

---

## 2. Definition

- Definition: `tsconfig.json` is a JSON configuration file that controls TypeScript's type checker, emitter, and module resolver.
- Category: Compiler configuration.
- Core idea: Separate type-checking from emitting; configure each phase independently.

---

## 3. Minimal Working Config

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "react-jsx",
    "outDir": "./dist",
    "rootDir": "./src",
    "declaration": true,
    "skipLibCheck": true
  },
  "include": ["src"],
  "exclude": ["node_modules", "dist"]
}
```

---

## 4. The Most Important Options

### `target` — What JavaScript to emit

```json
"target": "ES2020"
```

Controls which JavaScript features TypeScript compiles down vs passes through. `ES2020` supports optional chaining, nullish coalescing, `Promise.allSettled`. `ESNext` passes through everything — bundler or browser must support it.

```
ES5     → all modern syntax compiled down (Arrow fn → function(){})
ES2015  → classes, arrow fns, template literals native
ES2020  → optional chaining, nullish coalescing, BigInt native
ESNext  → no downcompilation at all
```

**Rule:** Match `target` to what your runtime supports. For Node.js 20+, use `ES2022+`. For browser apps with a bundler, set `target` to what Babel/SWC will handle — TypeScript only type-checks.

### `module` — Module system for emitted code

```json
"module": "ESNext"       // ESM imports/exports in output
"module": "CommonJS"     // require()/module.exports in output
"module": "NodeNext"     // Node.js hybrid ESM/CJS resolution
```

**For bundlers (Vite, Webpack, Rollup):** Use `"module": "ESNext"` — bundler handles module resolution and emitting. TypeScript only type-checks.

**For Node.js direct execution:** Use `"module": "NodeNext"` with `"moduleResolution": "NodeNext"`.

### `moduleResolution` — How imports are resolved

```json
"moduleResolution": "bundler"   // modern apps with Vite/webpack
"moduleResolution": "NodeNext"  // Node.js apps
"moduleResolution": "Node16"    // older Node.js
```

`"bundler"` is the correct option for any app using Vite, webpack, or Rollup. It allows `exports` field in `package.json`, supports path aliases, and matches how bundlers actually resolve modules.

### `strict` — The key safety flag

```json
"strict": true
```

Enables a bundle of strictness checks. Most important:

| Flag | What it catches |
|---|---|
| `strictNullChecks` | `null` and `undefined` must be explicitly handled |
| `strictFunctionTypes` | Function parameter contravariance |
| `strictPropertyInitialization` | Class properties must be initialized |
| `noImplicitAny` | Variables cannot have implicit `any` type |

**Always enable `strict: true`** in new projects. Disabling it to "fix" type errors is the wrong approach.

---

## 5. Path Aliases — Eliminate `../../..` Imports

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"],
      "@components/*": ["./src/components/*"],
      "@hooks/*": ["./src/hooks/*"],
      "@utils/*": ["./src/utils/*"]
    }
  }
}
```

```typescript
// Before: relative hell
import { Button } from '../../../components/ui/Button';
import { useAuth } from '../../hooks/useAuth';

// After: clean aliases
import { Button } from '@components/ui/Button';
import { useAuth } from '@hooks/useAuth';
```

**Critical:** TypeScript path aliases only affect type-checking. Bundlers must also be configured to resolve the same aliases.

```typescript
// vite.config.ts — mirror the same aliases
import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
    },
  },
});

// webpack.config.js
module.exports = {
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
};

// Next.js — tsconfig paths are automatically picked up
// Add to next.config.ts only if custom resolution needed
```

---

## 6. JSX Configuration

```json
"jsx": "react-jsx"       // React 17+ — no need to import React
"jsx": "react"           // React 16 — requires import React from 'react'
"jsx": "preserve"        // pass JSX through — bundler handles it (Vite/webpack+Babel)
"jsx": "react-native"    // JSX passed through as-is for Metro
```

For modern React apps with a bundler: use `"jsx": "preserve"` and let Babel/SWC/esbuild handle JSX transformation. TypeScript only type-checks.

For type-only libraries or strict tsc compilation: use `"jsx": "react-jsx"`.

---

## 7. Declaration Files

```json
"declaration": true,          // emit .d.ts files
"declarationDir": "./types",  // put them in ./types/ folder
"declarationMap": true,       // source map for .d.ts files (go-to-definition works)
"emitDeclarationOnly": true   // only emit .d.ts — no JS output (bundler emits JS)
```

**Library authoring pattern:**

```json
{
  "compilerOptions": {
    "declaration": true,
    "declarationMap": true,
    "emitDeclarationOnly": true,
    "outDir": "./dist"
  }
}
```

```
tsc (emits .d.ts) + tsup/rollup (emits .js/.mjs/.cjs)
→ consumers get types + runtime code
```

---

## 8. Incremental Builds

```json
"incremental": true,
"tsBuildInfoFile": "./.tsbuildinfo"
```

TypeScript stores a cache of the last type-check result. On subsequent runs, it only re-checks changed files. Dramatically speeds up CI and watch mode.

```
First run:   tsc --incremental   → 8.2s, creates .tsbuildinfo
Second run:  tsc --incremental   → 1.4s (cache hit)
```

---

## 9. Project References — Composite Projects

For monorepos, project references allow TypeScript to build and type-check packages independently with dependency tracking.

```
packages/
  core/
    tsconfig.json     ← composite: true
  ui/
    tsconfig.json     ← composite: true, references core
  app/
    tsconfig.json     ← references ui and core
```

```json
// packages/core/tsconfig.json
{
  "compilerOptions": {
    "composite": true,
    "declaration": true,
    "outDir": "./dist"
  }
}

// packages/app/tsconfig.json
{
  "references": [
    { "path": "../core" },
    { "path": "../ui" }
  ]
}
```

```bash
# Build all referenced projects in correct dependency order
tsc --build
# Or shorthand:
tsc -b
```

**Benefits:**
- TypeScript validates cross-package imports
- Only rebuilds packages that changed (incremental)
- Correct build ordering is enforced

---

## 10. Common tsconfig.json Options Table

| Option | Purpose | Typical Value |
|---|---|---|
| `strict` | Enable all strictness checks | `true` |
| `noUnusedLocals` | Error on unused variables | `true` (in CI) |
| `noUnusedParameters` | Error on unused function params | `true` (in CI) |
| `noImplicitReturns` | All code paths must return | `true` |
| `noFallthroughCasesInSwitch` | Switch must not fall through | `true` |
| `exactOptionalPropertyTypes` | `undefined` ≠ absent property | `true` (strict apps) |
| `isolatedModules` | Each file must be independently transpilable | `true` (required for esbuild/SWC) |
| `verbatimModuleSyntax` | Enforce explicit `import type` for types | `true` (modern) |
| `skipLibCheck` | Skip type-checking `.d.ts` in `node_modules` | `true` |
| `resolveJsonModule` | Allow `import config from './config.json'` | `true` |
| `esModuleInterop` | Allow default import of CJS modules | `true` |
| `allowSyntheticDefaultImports` | Allow default import without `esModuleInterop` side effects | `true` |
| `forceConsistentCasingInFileNames` | Error on case mismatches in imports | `true` |

---

## 11. `isolatedModules` — Why Bundlers Require It

```json
"isolatedModules": true
```

Bundlers like esbuild and SWC transform one file at a time — they cannot see the whole project. `isolatedModules` tells TypeScript to error when you write code that requires cross-file knowledge to transform:

```typescript
// BAD: is 'Foo' a type or a value? esbuild can't tell without checking other files
export { Foo } from './foo';

// GOOD: explicit type export — esbuild/SWC knows to strip this
export type { Foo } from './foo';
import type { Foo } from './foo';
```

**Rule:** Always use `import type` / `export type` for type-only imports when `isolatedModules: true`. Vite, Next.js, and Create React App all require this.

---

## 12. tsconfig for Different Project Types

### React App (Vite)
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "isolatedModules": true,
    "noEmit": true,
    "baseUrl": ".",
    "paths": { "@/*": ["./src/*"] }
  },
  "include": ["src"]
}
```

**Note:** `"noEmit": true` — Vite handles emitting. TypeScript only type-checks.

### Next.js App
```json
{
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["dom", "dom.iterable", "esnext"],
    "module": "ESNext",
    "moduleResolution": "bundler",
    "jsx": "preserve",
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "plugins": [{ "name": "next" }],
    "paths": { "@/*": ["./src/*"] }
  }
}
```

**Note:** `"plugins": [{ "name": "next" }]` enables Next.js-specific type checking (e.g., warning about client-only code in Server Components).

### Node.js API
```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "outDir": "./dist",
    "declaration": true,
    "sourceMap": true
  },
  "include": ["src"],
  "exclude": ["node_modules", "dist", "**/*.test.ts"]
}
```

### npm Library
```json
{
  "compilerOptions": {
    "target": "ES2018",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "declaration": true,
    "declarationMap": true,
    "emitDeclarationOnly": true,
    "strict": true,
    "isolatedModules": true
  }
}
```

---

## 13. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| `"module": "CommonJS"` with Vite/webpack | Bundler can't tree-shake CJS | Use `"module": "ESNext"` |
| `"moduleResolution": "Node"` in modern apps | Path aliases, `exports` field, `.ts` extensions silently fail | Use `"moduleResolution": "bundler"` |
| Missing `isolatedModules` for esbuild/SWC | Works locally (TypeScript project context), fails in CI | Set `"isolatedModules": true` |
| Path aliases in tsconfig but not in bundler | Type-checking passes; runtime module not found | Mirror aliases in `vite.config.ts` or `webpack.config.js` |
| `"strict": false` | `null` safety and implicit `any` errors hidden | Enable strict, fix errors properly |
| Not using `import type` | esbuild/SWC strips type-only imports at wrong time | Always `import type` for types when `isolatedModules: true` |
| `skipLibCheck: false` | Type errors from `@types/*` packages you don't own | Set `skipLibCheck: true` for apps |

---

## 14. Interview Insight

Strong answer:

> `tsconfig.json` controls three separate concerns: what TypeScript checks, what it emits, and how it resolves modules. In a bundled app, TypeScript usually only type-checks — `noEmit: true` — while the bundler handles the actual transformation. The most impactful options are `strict` for type safety, `moduleResolution: bundler` for correct module resolution with modern tools, `isolatedModules` for compatibility with single-file transformers like esbuild and SWC, and `paths` for clean import aliases — which must be mirrored in the bundler config to actually work at runtime.

Follow-up trap:

> Path aliases work in VS Code but not at runtime. Why?

Good answer:

> TypeScript aliases are compile-time only. The bundler resolves modules at build time independently. Both must be configured with the same alias mappings for it to work end-to-end.

---

## 15. Revision Notes

- One-line summary: `tsconfig.json` controls type checking, module resolution, and emitting — usually with a bundler doing the actual emitting.
- Three keywords: strict, moduleResolution, isolatedModules.
- One interview trap: Path aliases in tsconfig do not affect runtime without bundler config.
- Memory trick: TypeScript checks the code; the bundler ships it.
