# Build Tools — Active Recall Question Bank — Gold Sheet

> 30+ tiered questions for spaced repetition review. Cover the answer, think first, then reveal.

---

## Tier Legend

- 🟢 Foundation — Should answer in 30 seconds
- 🟡 Intermediate — Should answer in 1 minute
- 🔴 MAANG — Should answer in 2 minutes with examples

---

## Group 1: Core Concepts

---

### Q1 🟢

**What is the fundamental job of a bundler?**

<details><summary>Answer</summary>

A bundler traverses the dependency graph starting from one or more entry points, collects all imported modules, transforms them (transpile, minify), and outputs one or more optimized files that browsers can load efficiently.

</details>

---

### Q2 🟢

**What is the difference between a loader and a plugin in Webpack?**

<details><summary>Answer</summary>

- **Loader**: transforms a specific file type before it enters the module graph. Works at the file level (`rule.use: ['babel-loader']`).
- **Plugin**: hooks into Webpack's compilation lifecycle to perform broader operations (optimize chunks, inject HTML, define constants, upload source maps). Works at the build pipeline level.

Memory: Loaders transform files; Plugins transform the build.

</details>

---

### Q3 🟢

**What is tree shaking and what is required for it to work?**

<details><summary>Answer</summary>

Tree shaking is dead code elimination — the bundler detects exported symbols that are never imported and drops them from the bundle.

Requirements:
1. ESM `export` / `import` syntax (not CommonJS `module.exports`)
2. `"sideEffects": false` or an accurate sideEffects list in `package.json`
3. Production mode (minifiers do the final pruning)

</details>

---

### Q4 🟢

**What is HMR and how is it different from a full page reload?**

<details><summary>Answer</summary>

Hot Module Replacement (HMR) swaps changed modules in the running browser without reloading the page. The component state (form inputs, scroll position, authentication) is preserved. A full reload resets all browser state.

Vite uses native ESM + a WebSocket for HMR. Webpack uses its runtime to swap module references.

</details>

---

### Q5 🟢

**What does `NODE_ENV=production` do to a React app's bundle size?**

<details><summary>Answer</summary>

React checks `process.env.NODE_ENV` at runtime. When `production`, it skips development warnings, prop-type validation, and detailed error messages. Bundlers use `DefinePlugin` / `define` to replace `process.env.NODE_ENV` with the literal string `"production"`, allowing minifiers to dead-code-eliminate all the dev-only branches. This typically saves 60-70KB in the React bundle.

</details>

---

## Group 2: Module Systems

---

### Q6 🟡

**What is the difference between `module: "ESNext"` and `moduleResolution: "Bundler"` in tsconfig?**

<details><summary>Answer</summary>

- `module`: what syntax TypeScript **emits** (`import/export` vs `require/module.exports`).
- `moduleResolution`: how TypeScript **resolves** import paths when type-checking.

`moduleResolution: "Bundler"` understands the `exports` field in `package.json` — required for packages like `date-fns` v3 that use subpath exports. The older `"Node"` setting ignores `exports` and breaks.

</details>

---

### Q7 🟡

**What is the `exports` field in `package.json` and why does it matter for library authors?**

<details><summary>Answer</summary>

The `exports` field defines precise public entry points for a package. It:
1. Replaces `main` and `module` fields
2. Supports conditional exports (`import`, `require`, `types`, `browser`)
3. Hides internal files (unlisted paths cannot be imported)

```json
{
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs",
      "types": "./dist/index.d.ts"
    }
  }
}
```

Without it, consumers can import any file in the package directly, breaking encapsulation.

</details>

---

### Q8 🟡

**Why can't CommonJS modules be tree-shaken?**

<details><summary>Answer</summary>

CommonJS exports are a runtime assignment:
```javascript
module.exports = { formatDate, parseDate };  // evaluated at runtime
```

Bundlers cannot statically determine which properties will be used before executing the code. ESM `export` statements are static syntax that bundlers can analyze without running the code — they know at build time exactly which symbols are used.

</details>

---

### Q9 🟡

**What does `"type": "module"` in `package.json` change?**

<details><summary>Answer</summary>

Without `"type": "module"`: `.js` files are treated as CommonJS. ESM must use `.mjs` extension.
With `"type": "module"`: `.js` files are treated as ESM. CommonJS must use `.cjs` extension.

This affects Node.js resolution at runtime. Many build tools also use this field to determine how to process files. Warning: setting `"type": "module"` in a library can break consumers using CommonJS.

</details>

---

## Group 3: Transpilation

---

### Q10 🟡

**What is the difference between Babel and SWC?**

<details><summary>Answer</summary>

Both transpile modern JavaScript/TypeScript to an older target. The difference is implementation:
- **Babel**: JavaScript-based, plugin-based, highly configurable, extensible with custom transforms. 
- **SWC**: Rust-based, 20-70× faster, built-in support for the most common transforms.

Next.js 12+ replaced Babel with SWC for speed. SWC does not support arbitrary Babel plugins, so apps with custom Babel transforms may need to keep Babel.

</details>

---

### Q11 🟡

**What is `isolatedModules: true` in tsconfig and why do bundlers require it?**

<details><summary>Answer</summary>

`isolatedModules: true` restricts TypeScript to transforms that work per-file without cross-file type analysis. Bundlers (Babel, esbuild, SWC) transpile each file independently — they cannot see the full type information of the project.

The key restriction: you cannot re-export a type without `export type`. Without this constraint, bundlers cannot distinguish type-only exports from value exports and may fail to strip types correctly.

Required when using: Babel, esbuild, SWC, Vite, Next.js.

</details>

---

### Q12 🟡

**What does `tsconfig.json` `target` vs `lib` control?**

<details><summary>Answer</summary>

- `target`: what JavaScript **syntax** TypeScript compiles down to. `ES2020` means arrow functions are kept (supported since ES6), but `??` nullish coalescing is downleveled.
- `lib`: what global **types** are available in type checking. `lib: ["ES2020", "DOM"]` gives types for `Promise`, `Array.flatMap`, `fetch`, `window`, etc.

They are independent: you can target ES5 syntax but have type definitions for all DOM APIs.

</details>

---

## Group 4: Optimization

---

### Q13 🟡

**What is code splitting and what are the three main strategies?**

<details><summary>Answer</summary>

Code splitting divides the bundle into multiple files loaded on demand:

1. **Entry-point splitting**: separate bundle per page (Next.js does this automatically)
2. **Dynamic import**: `import('./Component')` creates a chunk loaded lazily
3. **Vendor splitting**: extract `node_modules` into a separate `vendors` chunk (stays cached across app updates)

Without splitting: one large bundle blocks initial render. With splitting: critical JS loads first, rest loads on demand.

</details>

---

### Q14 🟡

**What is the purpose of `contenthash` in output filenames?**

<details><summary>Answer</summary>

`contenthash` in `filename: '[name].[contenthash].js'` produces a filename that changes only when the file's content changes. This enables long-term browser caching (`Cache-Control: max-age=31536000, immutable`) — the browser caches `main.abc123.js` forever, and when the code changes, the filename becomes `main.def456.js` (a different URL), forcing a fresh download.

Without content hashing, you must use short cache durations, losing the performance benefit of caching.

</details>

---

### Q15 🔴

**Explain differential serving and when it matters.**

<details><summary>Answer</summary>

Differential serving delivers modern JS to modern browsers and transpiled JS to old browsers:

```html
<script type="module" src="app.modern.js"></script>        <!-- ES2020+, smaller -->
<script nomodule src="app.legacy.js"></script>              <!-- ES5, larger, only loaded by old browsers -->
```

Modern browsers understand `type="module"` and ignore `nomodule`. Old browsers ignore `type="module"` and load `nomodule`.

When it matters: the legacy bundle (targeting ES5) includes polyfills and transpilation overhead. If 95% of users have modern browsers, only 5% download the large bundle. For apps with global audiences and legacy device support, this reduces average bundle size by 15-30%.

Tools: Vite with `@vitejs/plugin-legacy`, or a custom webpack config with two targets.

</details>

---

### Q16 🔴

**What is `sideEffects: false` and what is the risk of getting it wrong?**

<details><summary>Answer</summary>

`"sideEffects": false` in `package.json` tells bundlers that no file in the package modifies global state when imported. This allows the bundler to tree-shake entire files when none of their exports are used.

Risk: CSS imports are side effects — they modify the DOM's styles when imported, even if they export nothing. If a component imports `./button.css` and `sideEffects: false` is set, the CSS is dropped from the bundle.

Correct config:
```json
{
  "sideEffects": ["**/*.css", "**/*.scss", "./src/polyfills.js"]
}
```

This keeps CSS files and explicit polyfills while allowing JS tree-shaking.

</details>

---

## Group 5: Dev Server and HMR

---

### Q17 🟢

**What is the fundamental difference between Vite's dev server and Webpack's dev server?**

<details><summary>Answer</summary>

- **Webpack**: bundles all modules together before serving. Cold start time scales with project size.
- **Vite**: serves source files as native ESM modules. The browser requests only the files it needs. esbuild pre-bundles `node_modules`. Cold start time is nearly constant regardless of project size.

The tradeoff: Vite's dev server serves raw modules, so HMR invalidation is more precise. But browsers make many small requests instead of one bundle.

</details>

---

### Q18 🟡

**What triggers a React Fast Refresh full reload vs an HMR hot swap?**

<details><summary>Answer</summary>

React Fast Refresh hot-swaps the component and preserves state when:
- The component is a named function export (not anonymous)
- No Rules of Hooks violation occurred
- The component rendered successfully

Falls back to full page reload when:
- A runtime error occurred during the re-render
- The component boundary changed (e.g., hooks count changed)
- The module has non-component exports that changed

Falls back to module replacement (state lost, no page reload) when:
- The component has state that is incompatible with the new version

</details>

---

### Q19 🟡

**What is `import.meta.hot` in Vite and when would you use it?**

<details><summary>Answer</summary>

`import.meta.hot` is Vite's HMR API. It allows a module to participate in HMR by defining what should happen when it or its dependencies update.

```typescript
if (import.meta.hot) {
  import.meta.hot.accept('./data-store.ts', (newModule) => {
    // Replace the in-memory store with the new version
    store.replaceReducer(newModule.reducer);
  });
  import.meta.hot.dispose(() => {
    // Clean up timers, subscriptions before module unloads
    clearInterval(pollingInterval);
  });
}
```

Common uses: state management stores, WebSocket connections, global intervals that need cleanup between HMR updates.

</details>

---

## Group 6: Bundler Comparison

---

### Q20 🟡

**When would you choose Vite over Webpack for a new project in 2025?**

<details><summary>Answer</summary>

Choose Vite when:
- Building a React/Vue/Svelte SPA or library
- Fast development experience is a priority
- Starting fresh (no legacy webpack plugins required)

Choose Webpack when:
- Module Federation micro-frontend architecture
- Deeply customized webpack plugin ecosystem required
- Migrating an existing CRA or webpack app (migration cost outweighs Vite benefits)
- Next.js handles the choice — it uses Webpack with optional SWC, not Vite

In 2025, most new React projects use Vite. Most Next.js/Remix projects stay with their framework's built-in bundler (Webpack or Turbopack).

</details>

---

### Q21 🟡

**What is esbuild's key limitation that prevents it from replacing Webpack entirely?**

<details><summary>Answer</summary>

esbuild is extremely fast (written in Go, parallelized) but has limited plugin ecosystem and does not support all Webpack features:
- No Module Federation
- No advanced code splitting strategies (`splitChunks` config)
- Limited CSS handling (no CSS Modules natively)
- Plugin API exists but is less powerful than Webpack's

esbuild is used as a **transformer** inside Vite (for dev server pre-bundling and TypeScript stripping) and Vitest — but Vite uses Rollup for production builds to get full plugin ecosystem support.

</details>

---

### Q22 🔴

**Explain Turbopack's incremental model and how it differs from Webpack 5's cache.**

<details><summary>Answer</summary>

**Webpack 5 persistent cache**: serializes the entire module graph to disk after the first build. Subsequent builds deserialize the graph, check which files changed, and rebuild only changed modules. The cache is file-based and invalidated by file timestamps/hashes.

**Turbopack incremental model**: uses a function-level memoization engine (Turbo engine). Every computation (parse file, resolve module, transform) is a pure function with tracked inputs. If inputs haven't changed, the output is reused. This operates at a finer granularity than Webpack — it can memoize individual AST transformations, not just whole files.

Result: Turbopack's granular memoization enables faster HMR for large codebases because only the minimal affected computation path is re-executed. However, as of 2025, Turbopack is in production only for Next.js dev server — production bundling is still Webpack.

</details>

---

## Group 7: Library and Monorepo

---

### Q23 🟡

**What is the difference between `dependencies`, `devDependencies`, and `peerDependencies` in a published library?**

<details><summary>Answer</summary>

- `dependencies`: bundled into or required at runtime by the library. Installed automatically when the library is installed.
- `devDependencies`: only needed to build/test the library. NOT installed by consumers. (TypeScript, jest, tsup go here.)
- `peerDependencies`: must be provided by the consumer's app. The library uses them but does not bundle them. Example: `react` and `react-dom` — if the library bundled React, the consumer's app would have two React instances, breaking hooks.

Rule: frameworks (React, Vue, Angular) and large utilities (lodash) should be peerDependencies in libraries.

</details>

---

### Q24 🟡

**What is `dependsOn: ["^build"]` in `turbo.json` and what does the `^` mean?**

<details><summary>Answer</summary>

`dependsOn` controls task ordering in Turborepo. 
- `"^build"`: run the `build` task for all packages that this package depends on (via `package.json` dependencies) **before** running this package's build.
- Without `^` (e.g., `"build"`): run this same package's `build` task before this task (useful for `test` depending on `build`).

The `^` means "upstream dependencies". This ensures `@company/ui` is built before `@company/web`, which imports from it.

</details>

---

### Q25 🔴

**Describe the full publishing pipeline for a dual CJS+ESM library.**

<details><summary>Answer</summary>

1. Source code in TypeScript in `src/`
2. Build with `tsup` or `rollup`:
   - CJS: `dist/index.js` + `dist/index.d.ts`
   - ESM: `dist/index.mjs` + `dist/index.d.mts`
3. `package.json` exports field:
```json
{
  "exports": {
    ".": {
      "import": { "types": "./dist/index.d.mts", "default": "./dist/index.mjs" },
      "require": { "types": "./dist/index.d.ts", "default": "./dist/index.js" }
    }
  },
  "sideEffects": false
}
```
4. Validate with `publint` to catch config errors
5. `npm publish --access public`

The result: Webpack/Rollup/Vite consumers get the ESM build (tree-shakeable). Node.js CommonJS consumers get the CJS build. TypeScript consumers get types for both.

</details>

---

## Group 8: Environment and Security

---

### Q26 🟢

**What is the Vite `VITE_` prefix rule and why does it exist?**

<details><summary>Answer</summary>

Vite only exposes environment variables with the `VITE_` prefix to client-side code via `import.meta.env`. Variables without this prefix are only available in `vite.config.ts` and server-side Node.js code.

The rule exists to prevent accidental exposure of secrets. Without this safeguard, a developer might add `DATABASE_URL=postgres://...` to `.env` and accidentally reference it in client code, sending the database password to every browser.

</details>

---

### Q27 🟡

**What is the compile-time vs runtime distinction for environment variables?**

<details><summary>Answer</summary>

**Compile-time (Vite `import.meta.env`, Webpack `DefinePlugin`)**: Values are baked into the bundle as literal strings during the build. The bundle cannot be changed after deployment — deploying to a new environment requires a rebuild.

**Runtime (Node.js `process.env`, K8s ConfigMaps, server-rendered env injection)**: Values are read when the server or script starts. The same bundle can run in dev/staging/prod with different configs.

For browser apps, environment variables are always effectively compile-time (the browser cannot read server env vars). This is why `NEXT_PUBLIC_API_URL` must be set at build time.

</details>

---

### Q28 🔴

**What should never reach the browser bundle, and how would you detect a leak?**

<details><summary>Answer</summary>

Should never reach browser:
- Database connection strings
- JWT secrets / signing keys
- API keys with write access
- Internal service URLs / credentials
- Private key material

Detection methods:
1. **Grep the bundle**: `grep -r "SECRET" dist/` after each build
2. **CI check**: `cat dist/main.js | grep -i "secret\|password\|private_key" && exit 1`
3. **Bundle analyzer**: visualize large string literals in chunks
4. **Automated secret scanning**: GitGuardian, GitHub secret scanning on the built artifacts

Prevention:
- Vite: never-prefix secrets with `VITE_`
- Next.js: never-prefix public secrets with `NEXT_PUBLIC_`
- Server-only packages: use `server-only` npm package — imports it in server code to prevent accidental client import
- Zod env validation: validate env at startup, distinguish public vs private

</details>

---

## Group 9: Performance and Debugging

---

### Q29 🟡

**What is the webpack-bundle-analyzer and what three things should you look for?**

<details><summary>Answer</summary>

`webpack-bundle-analyzer` renders an interactive treemap of your bundle, sized by byte weight.

Three things to look for:
1. **Duplicate packages**: the same library appears twice under different paths (different versions of lodash, two React copies in Module Federation)
2. **Unexpectedly large libraries**: moment, lodash full build, AWS SDK full import
3. **Files that should be lazy-loaded**: full admin pages included in the initial bundle when only authenticated users ever reach them

</details>

---

### Q30 🟡

**What is `devtool: 'hidden-source-map'` and when should it be used?**

<details><summary>Answer</summary>

`hidden-source-map` generates `.map` files but does NOT add the `//# sourceMappingURL=` comment to the bundle. This means:
- Browsers cannot automatically find the source maps (no public exposure)
- Error tracking tools (Sentry, Datadog) can upload and use the maps

Use case: production builds where you want readable stack traces in error monitoring but don't want your source code accessible to users via browser DevTools.

Alternative approaches: `source-map` (fully accessible, never use in production), `eval-source-map` (dev only, fast but not accurate), `false` (no maps, breaks error monitoring).

</details>

---

### Q31 🔴

**A developer reports their Next.js build takes 4 minutes locally. Walk through your optimization approach.**

<details><summary>Answer</summary>

**Step 1: Identify the slow phase**

```bash
# Time each phase
time next build 2>&1 | grep -E "Compiled|Linting|Type|Generating"
```

**Step 2: Check TypeScript type-checking**

TypeScript full type check is slow. Separate type-checking from bundling:
```json
// next.config.js
{ typescript: { ignoreBuildErrors: false } }  // keep type safety
// Run type-check separately in CI:
// "type-check": "tsc --noEmit"
```

**Step 3: Check bundle size and code splitting**

```bash
ANALYZE=true next build  # with @next/bundle-analyzer
```

Large pages with no dynamic imports = slow bundling.

**Step 4: Enable SWC (if not already)**

Next.js 12+ uses SWC by default. If there's a `babel.config.js`, it overrides SWC.
```bash
# Remove babel.config.js if it only has basic React transforms
# SWC is 17x faster than Babel
```

**Step 5: Enable incremental TypeScript**

```json
{
  "incremental": true,
  "tsBuildInfoFile": ".next/cache/.tsbuildinfo"
}
```

**Step 6: Ensure filesystem cache persisted across builds**

In CI, cache `.next/cache` between runs.

</details>

---

## Revision Notes

- One-line summary: Active recall on 31 questions across all build tools topics; spaced repetition required.
- Progress tracking: mark date next to each question as you review.
- Review cadence: Day 1 → Day 3 → Day 7 → Day 14 → Day 30.
