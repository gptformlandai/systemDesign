# Library Publishing Pipeline — tsup, Rollup, CJS+ESM Dual Output — Gold Sheet

> Topic: building npm packages with correct dual-format output, the exports field, and tree-shaking-friendly output

---

## 1. Intuition

Building an app and building a library are different. An app bundles everything into one chunk for the browser. A library must output separate, tree-shakeable files that consumers can import — without bundling the consumer's dependencies. Getting it wrong means consumers get giant bundles, broken imports, or no TypeScript types.

Beginner version:

> A library build must output clean JavaScript that any app bundler can tree-shake, without including the library's own node_modules.

---

## 2. Definition

- Definition: A library publishing pipeline transforms TypeScript source into multiple output formats (ESM, CJS) with declaration files, correctly configured `package.json` exports, and no bundled peer dependencies.
- Category: Library toolchain.
- Core idea: Output is consumed by other bundlers — optimize for flexibility, not for direct browser loading.

---

## 3. App Build vs Library Build

| Concern | App Build | Library Build |
|---|---|---|
| Entry points | One or a few | One per public export |
| Output format | Browser ESM (+ optional CJS) | ESM + CJS both |
| `node_modules` bundled | YES — ship everything | NO — mark as external |
| CSS extraction | YES | Usually separate or consumers handle |
| Content hashing | YES (cache busting) | NO (stable filenames) |
| Source maps | Optional | YES (debugging in consumer's stack traces) |
| TypeScript types | Not needed | Required — ship `.d.ts` files |
| Minification | YES | Optional (consumer minifies) |

---

## 4. tsup — The Fastest Library Build Tool

`tsup` is built on esbuild and is the de facto standard for TypeScript library builds.

```bash
npm install -D tsup
```

```typescript
// tsup.config.ts
import { defineConfig } from 'tsup';

export default defineConfig({
  entry: ['src/index.ts'],           // library entry point
  format: ['cjs', 'esm'],            // output both CommonJS and ESM
  dts: true,                         // generate .d.ts declaration files
  sourcemap: true,
  clean: true,                       // clean dist/ before build
  splitting: false,                  // no chunk splitting for libraries
  treeshake: true,
  minify: false,                     // consumers minify — don't double-minify
  external: ['react', 'react-dom'],  // don't bundle peer deps
});
```

```bash
tsup  # produces:
# dist/index.js       (CommonJS)
# dist/index.mjs      (ESM)
# dist/index.d.ts     (TypeScript declarations)
# dist/index.d.mts    (ESM TypeScript declarations)
# dist/index.js.map   (source maps)
```

---

## 5. The `package.json` exports Field

The `exports` field is the modern way to define what consumers get when they `import` or `require` your package. It replaces `main` and `module` (though you should keep those for backwards compatibility).

```json
{
  "name": "my-ui-library",
  "version": "1.0.0",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",         // ESM consumers
      "require": "./dist/index.js",          // CJS consumers
      "types": "./dist/index.d.ts"           // TypeScript
    },
    "./button": {
      "import": "./dist/button.mjs",
      "require": "./dist/button.js",
      "types": "./dist/button.d.ts"
    }
  },
  "main": "./dist/index.js",           // fallback for old Node.js / bundlers
  "module": "./dist/index.mjs",        // non-standard but many bundlers read this
  "types": "./dist/index.d.ts",        // TypeScript fallback
  "files": ["dist"],                   // only publish dist/ to npm
  "sideEffects": false,                // tell bundlers: safe to tree-shake this package
  "peerDependencies": {
    "react": ">=18.0.0"
  },
  "devDependencies": {
    "react": "^18.0.0",
    "tsup": "^8.0.0",
    "typescript": "^5.0.0"
  }
}
```

**`sideEffects: false`** is critical for tree-shaking. It tells the consumer's bundler that importing from this package doesn't cause side effects — unused exports can be safely removed.

If your library has CSS imports as side effects:

```json
"sideEffects": ["./dist/styles.css", "**/*.css"]
```

---

## 6. Multiple Entry Points — Subpath Exports

Large libraries export specific subpaths to enable partial imports:

```typescript
// tsup.config.ts
export default defineConfig({
  entry: {
    index: 'src/index.ts',
    button: 'src/components/button/index.ts',
    input: 'src/components/input/index.ts',
    utils: 'src/utils/index.ts',
  },
  format: ['cjs', 'esm'],
  dts: true,
});
```

```json
// package.json
{
  "exports": {
    ".": { "import": "./dist/index.mjs", "require": "./dist/index.js", "types": "./dist/index.d.ts" },
    "./button": { "import": "./dist/button.mjs", "require": "./dist/button.js", "types": "./dist/button.d.ts" },
    "./input": { "import": "./dist/input.mjs", "require": "./dist/input.js", "types": "./dist/input.d.ts" }
  }
}
```

```typescript
// Consumer — imports only what they need
import { Button } from 'my-ui-library/button';
// → only button code is pulled in, not the entire library
```

---

## 7. Rollup — Still the Best for Library Flexibility

Rollup is preferred when you need fine-grained control over the output: multiple formats from one config, complex externals, or custom plugins.

```javascript
// rollup.config.js
import typescript from '@rollup/plugin-typescript';
import { nodeResolve } from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';

export default [
  // ESM build
  {
    input: 'src/index.ts',
    output: {
      file: 'dist/index.mjs',
      format: 'esm',
      sourcemap: true,
    },
    external: ['react', 'react-dom', /^react\//],  // don't bundle React
    plugins: [
      nodeResolve(),
      commonjs(),
      typescript({ tsconfig: './tsconfig.build.json' }),
    ],
  },
  // CJS build
  {
    input: 'src/index.ts',
    output: {
      file: 'dist/index.js',
      format: 'cjs',
      sourcemap: true,
      exports: 'named',
    },
    external: ['react', 'react-dom'],
    plugins: [nodeResolve(), commonjs(), typescript()],
  },
];
```

---

## 8. Peer Dependencies vs Dependencies

```json
{
  "dependencies": {
    "date-fns": "^3.0.0"    // bundled INTO your library output
  },
  "peerDependencies": {
    "react": ">=18.0.0"      // NOT bundled — consumer must provide this
  },
  "peerDependenciesMeta": {
    "react": { "optional": false }
  }
}
```

**Rule:**
- `dependencies`: utilities your library bundles (lodash, date-fns sub-functions)
- `peerDependencies`: frameworks/runtimes the consumer already has (React, Vue, Next.js)

**Why peer deps matter for bundle size:** If you put `react` in `dependencies`, it gets bundled into your library output. The consumer's app then has two copies of React — one from their `node_modules`, one from your bundle. This causes the "two React instances" bug in hooks.

---

## 9. Declaration File Generation

```json
// tsconfig.build.json (separate from tsconfig.json)
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "declaration": true,
    "declarationMap": true,
    "emitDeclarationOnly": true,
    "outDir": "./dist"
  },
  "exclude": ["**/*.test.ts", "**/*.stories.tsx"]
}
```

With `tsup`, `dts: true` handles this automatically using an internal `tsc` invocation.

---

## 10. Checklist Before Publishing to npm

```bash
# 1. Build
npm run build

# 2. Verify outputs exist
ls dist/
# index.js, index.mjs, index.d.ts, index.d.mts, *.map

# 3. Verify exports resolve correctly
node -e "require('./dist/index.js')"
node --input-type=module --eval "import './dist/index.mjs'"

# 4. Check what will be published
npm pack --dry-run

# 5. Verify tree-shaking with publint
npx publint

# 6. Verify peer deps are external (not bundled)
grep -l "react" dist/*.js | head -5
# Should NOT find React source code in your dist
```

**`publint`** is a lint tool that validates your `package.json` exports, files, and format settings:

```bash
npx publint
# Reports: exports path missing, types not exported, etc.
```

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| `react` in `dependencies` instead of `peerDependencies` | Two React instances in consumer — hooks break | Move to `peerDependencies` |
| No `sideEffects: false` | Consumer bundler can't tree-shake library | Add `"sideEffects": false` (or list actual side-effecting files) |
| No `exports` field | Only `main` is used — subpath imports fail | Add `exports` with import/require/types conditions |
| Bundling `node_modules` into library | Library grows 10x, consumer gets duplicates | Mark all peer deps as `external` |
| Missing declaration maps (`declarationMap`) | Go-to-definition in consumer points to `.d.ts`, not source | Enable `declarationMap: true` |
| Publishing `src/` instead of `dist/` | Consumers get TypeScript source, must run their own tsc | Use `"files": ["dist"]` in package.json |
| CJS-only output in 2025+ | ESM-only apps (Vite default) may have issues | Always dual-publish CJS + ESM |

---

## 12. Interview Insight

Strong answer:

> Library builds differ from app builds in three key ways. First, they never bundle peer dependencies — React, Vue, or other frameworks must be marked as external so the consumer's bundler doesn't end up with two copies. Second, they output both CJS and ESM so any consumer can use the library regardless of their module system. Third, they export TypeScript declaration files so consumers get full type checking. The `exports` field in `package.json` is the modern way to declare all of this — it maps import paths to specific file formats and type definitions for each environment.

Follow-up trap:

> What breaks if a library doesn't mark React as external?

Good answer:

> The library bundles its own copy of React into its output. When a consumer installs the library, they have React in their `node_modules` and React inside the library bundle. React's hooks rely on module singletons — if two different React instances exist, `useState` and context don't share state, causing "invalid hook call" errors.

---

## 13. Revision Notes

- One-line summary: Library builds output CJS + ESM with types and no bundled peer deps.
- Three keywords: external, exports field, dual-format.
- One interview trap: Peer deps in `dependencies` cause two React instances.
- Memory trick: Library output is an ingredient, not a meal — don't cook the consumer's React.
