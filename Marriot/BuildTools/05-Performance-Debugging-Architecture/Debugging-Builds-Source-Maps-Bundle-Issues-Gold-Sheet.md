# Debugging Builds: Source Maps and Bundle Issues Gold Sheet

> Topic: source maps, bundle debugging, and common build pipeline errors.

---

## 1. Intuition

Build debugging is detective work across the pipeline. The bug may be in source code, transformation, resolution, bundling, optimization, or deployment.

Beginner version:

> When a build breaks, ask which pipeline stage broke.

---

## 2. Definition

- Definition: Build debugging is the process of locating failures across transform, module resolution, optimization, and runtime output.
- Category: Production engineering and developer tooling.
- Core idea: Map symptoms back to pipeline stages.

---

## 3. Debugging Pipeline

```txt
Error symptom
    |
    v
Which phase?
    |
    +-- install/dependency
    +-- transpilation
    +-- module resolution
    +-- bundling/chunking
    +-- optimization/minification
    +-- runtime/deployment
```

---

## 4. Source Maps

Minified production code is hard to read.

```js
function a(n){return n.map(n=>n.id)}
```

Source maps connect generated output back to original source.

```txt
dist/app.abc123.js:1:2311
   |
   v
src/features/users/selectors.ts:14:9
```

Why source maps matter:

- Debug production errors.
- Connect Sentry errors to source files.
- Improve stack traces.
- Investigate minification-only issues.

Risk:

- Public source maps may expose source code.
- Private upload to monitoring tools is often safer for sensitive apps.

---

## 5. Common Error Types

### Transpilation Error

```txt
Unexpected token '<'
```

Likely causes:

- JSX file not passed through React transform.
- Wrong extension.
- Missing loader/plugin.

### Module Resolution Error

```txt
Cannot resolve '@/components/Button'
```

Likely causes:

- Alias configured in TypeScript but not bundler.
- Missing file extension support.
- Wrong monorepo package path.

### Node API In Browser

```txt
Module not found: fs
```

Likely causes:

- Client bundle imports server-only code.
- Package expects Node runtime.
- Incorrect package condition selected.

### Duplicate React

```txt
Invalid hook call
```

Possible cause:

- Two React copies in the bundle or workspace.

### Minification-Only Bug

```txt
works in dev, fails in prod
```

Likely causes:

- Dead code elimination removed side-effectful code.
- Environment variable differs.
- Name mangling interacts with unsafe code.
- Race hidden by dev timing.

---

## 6. Debugging Checklist

```txt
1. Reproduce locally.
2. Run production build.
3. Run production preview/server.
4. Check exact error phase.
5. Inspect transformed output if needed.
6. Check aliases and package resolution.
7. Check duplicate dependencies.
8. Check environment variables.
9. Check source maps.
10. Use bundle analyzer.
```

Commands vary by project:

```bash
npm run build
npm run preview
npm ls react
```

---

## 7. Bundle Analyzer Thinking

Use analyzer output to answer:

- Which chunk is too large?
- Which dependency pulled it in?
- Is there duplicate code?
- Are locales or icons imported too broadly?
- Did a server-only package enter the client bundle?
- Is a dependency included in the initial route unnecessarily?

Bad import:

```ts
import * as Icons from 'huge-icon-pack';
```

Better:

```ts
import { SearchIcon } from 'huge-icon-pack/search';
```

Exact import paths depend on the package.

---

## 8. React, Next.js, React Native Notes

### React Web

Debug with production build preview, source maps, and bundle analysis.

### Next.js

Check whether an error is server-side, client-side, route-specific, or caused by a Server Component / Client Component boundary.

Common issue:

```txt
Window is not defined
```

Meaning:

```txt
browser-only code ran during server rendering
```

### React Native

Check Metro resolver errors, platform-specific file selection, asset paths, native module linking, and Hermes stack traces.

Common issue:

```txt
Unable to resolve module ./Button
```

Possible cause:

```txt
file exists as Button.web.tsx but native build needs Button.ios.tsx, Button.android.tsx, or Button.tsx
```

---

## 9. Tool Comparison Lens

| Tool | Debug Focus |
|---|---|
| Webpack | Loader/plugin order, aliases, splitChunks, duplicate deps |
| Vite | dependency optimization, ESM/CJS compatibility, plugin transforms |
| Parcel | inferred transforms, target config, generated assets |
| Next.js | server/client boundary, route output, SSR/runtime mismatch |
| Metro | platform resolution, watchFolders, assets, native modules |
| Rollup/esbuild | package output format, externals, tree shaking |

---

## 10. Real-World Example

Symptom:

> App works in dev, but production checkout page crashes.

Investigation:

```txt
prod stack trace
  -> minified file
  -> use source map
  -> points to paymentWidget.ts
  -> widget imported at top level
  -> accesses window during SSR
```

Fix:

- Load widget only on the client.
- Dynamic import the widget.
- Add SSR guard.
- Add production build test in CI.

Example:

```tsx
import dynamic from 'next/dynamic';

const PaymentWidget = dynamic(() => import('./PaymentWidget'), {
  ssr: false,
});
```

---

## 11. Common Mistakes

### Mistake: Debugging production bugs only in dev mode

- Why wrong: Dev and prod builds differ.
- Better approach: reproduce against production build output.

### Mistake: Uploading public source maps without thinking

- Why wrong: Source code may be exposed.
- Better approach: upload private source maps to monitoring tools when appropriate.

### Mistake: Fixing alias in `tsconfig` only

- Why wrong: TypeScript can understand an alias while the bundler cannot.
- Better approach: align TypeScript, bundler, test runner, and IDE aliases.

### Mistake: Ignoring lockfile changes

- Why wrong: Transitive dependency updates can change module format or exports.
- Better approach: inspect dependency diffs during build failures.

---

## 12. Trade-Offs

| Debug Choice | Benefit | Cost |
|---|---|---|
| Production sourcemaps | Better debugging | Possible source exposure |
| Private sourcemap upload | Safer observability | Extra CI setup |
| Bundle analyzer in CI | Catches regressions | Needs thresholds and maintenance |
| Strict dependency dedupe | Avoids duplicate React | Can constrain package versions |
| Clear build caches | Removes stale state | Slower next build |

---

## 13. Interview Insight

Strong answer:

> I debug build issues by mapping the symptom to a pipeline phase. Parse errors point to transforms, missing modules to resolution, large output to chunk analysis, production-only bugs to minification or environment differences, and runtime stack traces to source maps. I avoid assuming dev behavior matches production.

Follow-up trap:

> Why does `tsconfig` path alias work in the editor but fail in the app?

Good answer:

> The editor and TypeScript know the alias, but the bundler also needs matching resolver configuration. Test runners and lint tools may need it too.

---

## 14. Revision Notes

- One-line summary: Build debugging means identifying the failing pipeline stage.
- Three keywords: phase, map, reproduce.
- One interview trap: Dev reproduction is not enough for production build bugs.
- Memory trick: Follow the error backward through the factory line.
