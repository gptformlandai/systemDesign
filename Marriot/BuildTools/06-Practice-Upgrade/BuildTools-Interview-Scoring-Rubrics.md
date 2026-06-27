# Build Tools — Interview Scoring Rubrics — Gold Sheet

> 1-5 rubrics per topic + readiness gates for each role level

---

## Scoring Scale

| Score | Meaning |
|---|---|
| 1 | Blank / wildly incorrect |
| 2 | Vague / partially correct, missing key concepts |
| 3 | Correct core concepts, missing depth or tradeoffs |
| 4 | Full correct answer with tradeoffs or examples |
| 5 | Expert: correct + tradeoffs + real-world context + proactive follow-ups |

---

## Topic 1: Bundler Mental Model

**Question:** "Walk me through what a bundler does."

| Score | Response |
|---|---|
| 1 | "It compiles the code" (no specifics) |
| 2 | "It takes all the files and combines them" (correct direction, no depth) |
| 3 | Entry point → dependency graph → transform → output chunks. Mentions loaders or plugins. |
| 4 | Adds: tree shaking, code splitting, content hashing. Explains why each matters for production. |
| 5 | Adds: comparison of Webpack's ahead-of-time bundle vs Vite's native ESM approach. Mentions when bundling is not needed (Deno, CDN ESM). |

---

## Topic 2: Tree Shaking

**Question:** "Why does lodash not tree-shake?"

| Score | Response |
|---|---|
| 1 | "It's too big" |
| 2 | "Because it's CommonJS" (correct but unexplained) |
| 3 | CommonJS `module.exports` is a runtime assignment; bundler cannot statically analyze exports. |
| 4 | Adds: ESM `export` statements are static syntax → bundler knows at build time what's unused. Explains the fix (lodash-es). |
| 5 | Adds: `sideEffects: false` requirement, explains CSS sideEffects bug. Discusses `__esModule: true` interop. |

---

## Topic 3: HMR and React Fast Refresh

**Question:** "Why did HMR fall back to a full page reload when I saved a component file?"

| Score | Response |
|---|---|
| 1 | "That sometimes happens" |
| 2 | "There was probably an error" |
| 3 | Fast Refresh falls back when a runtime error occurs during re-render. Check browser console for the actual error. |
| 4 | Adds: Rules of Hooks violations are a common cause. Explains the full/module/component update tiers. |
| 5 | Adds: `import.meta.hot.accept` for manually controlling HMR for non-component modules. Explains how to add a dispose handler for cleanup. |

---

## Topic 4: Module Federation

**Question:** "Explain how Module Federation works."

| Score | Response |
|---|---|
| 1 | Never heard of it |
| 2 | "It shares code between different apps at runtime" (vague) |
| 3 | Shell loads `remoteEntry.js` from a remote app. Remote exposes components via `exposes`. Shell imports them dynamically. |
| 4 | Adds: `singleton: true` for React to prevent two instances. `publicPath` must be absolute. Error boundaries for remote failures. |
| 5 | Adds: dynamic remote loading (no build-time URL coupling). Module Federation v2 manifest approach. Tradeoff analysis vs monorepo vs npm publishing. |

---

## Topic 5: TypeScript Configuration

**Question:** "What is `moduleResolution` and when does it matter?"

| Score | Response |
|---|---|
| 1 | "It's a TypeScript setting" |
| 2 | "It's how TypeScript finds modules" |
| 3 | Controls how TypeScript resolves import paths. `Node` doesn't understand `exports` field; `Bundler`/`NodeNext` does. |
| 4 | Adds: distinction between `module` (emit syntax) and `moduleResolution` (resolution algorithm). Explains which packages break under `Node` (any that use `exports`). |
| 5 | Adds: `isolatedModules: true` requirement for bundlers. Project references and `composite: true`. When to use `NodeNext` vs `Bundler`. |

---

## Topic 6: Environment Variables

**Question:** "How do environment variables work in a Vite app and what must never reach the browser?"

| Score | Response |
|---|---|
| 1 | "You put them in .env" |
| 2 | "VITE_ prefix makes them available" |
| 3 | `VITE_` prefix exposes via `import.meta.env`. Non-prefixed vars stay in Node/build context only. Secrets must never be prefixed. |
| 4 | Adds: compile-time text substitution (not runtime). Different `.env` files per mode. `NODE_ENV` enables dead code elimination. |
| 5 | Adds: Zod validation pattern for env vars. Server-only imports. Security detection in CI (grep bundle for secrets). |

---

## Topic 7: Code Splitting

**Question:** "How would you reduce initial bundle size for a large admin dashboard?"

| Score | Response |
|---|---|
| 1 | "Use less code" |
| 2 | "Code splitting" (no implementation detail) |
| 3 | `React.lazy()` + dynamic `import()` for admin routes. Suspense boundary. Only critical path JS in initial bundle. |
| 4 | Adds: vendor chunk separation (React stays cached across deploys). `prefetch`/`preload` hints for predictable navigation. Bundle analyzer to identify large chunks. |
| 5 | Adds: differential serving for further size reduction. HTTP/2 push implications. Module Federation for separately deployed admin app. |

---

## Topic 8: Build Performance

**Question:** "Your CI build takes 15 minutes. How do you improve it?"

| Score | Response |
|---|---|
| 1 | "Use a faster computer" |
| 2 | "Cache the node_modules" |
| 3 | Turborepo with explicit inputs + remote caching. Only run affected packages. Parallelize independent tasks. |
| 4 | Adds: TypeScript incremental builds (`tsBuildInfoFile`). SWC over Babel. Split type-checking from bundling. Cache `.next/cache` in CI. |
| 5 | Adds: `turbo.json` `inputs` field details (why default is wrong). pnpm store caching. Turbopack for dev server (not yet production). |

---

## Topic 9: Source Maps

**Question:** "How do you enable readable stack traces in Sentry without exposing source code to users?"

| Score | Response |
|---|---|
| 1 | "Upload the source files to Sentry" |
| 2 | "Use source maps" |
| 3 | `devtool: 'hidden-source-map'` generates maps without linking them in the bundle. Upload maps to Sentry via CI. |
| 4 | Adds: delete maps from deploy artifact after upload. `SENTRY_AUTH_TOKEN` in CI. Explains why `source-map` is wrong for production. |
| 5 | Adds: `sentry/cli sourcemaps inject` for more accurate mapping. Debug IDs for upload matching. Multipart upload for large source maps. |

---

## Topic 10: Library Publishing

**Question:** "How do you publish a TypeScript library that supports both CJS and ESM consumers?"

| Score | Response |
|---|---|
| 1 | "Use npm publish" |
| 2 | "Build it and publish" |
| 3 | `tsup` for dual CJS + ESM build. `exports` field with `import` and `require` conditions. Include type declarations. |
| 4 | Adds: `sideEffects: ["**/*.css"]`. `peerDependencies` for React. `publint` validation. `workspace:` for monorepo development. |
| 5 | Adds: Changeset automation for versioning. Full pre-publish checklist. Explains the `main` vs `module` vs `exports` precedence rules. Consumer compatibility matrix. |

---

## Readiness Gates

### Junior Level Ready
All of the following at score 3+:
- [ ] Bundler Mental Model
- [ ] HMR basics
- [ ] Environment Variables
- [ ] Code Splitting concept

### Mid-Level Ready
All of the following at score 3+, majority at 4+:
- [ ] Bundler Mental Model
- [ ] Tree Shaking
- [ ] TypeScript Configuration
- [ ] Code Splitting
- [ ] HMR and React Fast Refresh
- [ ] Build Performance (basics)

### Senior Level Ready
All topics at score 4+:
- [ ] All mid-level topics
- [ ] Module Federation
- [ ] Source Maps
- [ ] Library Publishing

### Staff/Principal Level Ready
All topics at score 4+, majority at 5:
- [ ] All senior topics
- [ ] Can design full build pipeline from scratch
- [ ] Can evaluate tool tradeoffs with production context
- [ ] Can articulate when NOT to use a technology

---

## Self-Assessment Tracking

Complete after each mock interview round:

| Topic | Round 1 Score | Round 2 Score | Round 3 Score |
|---|---|---|---|
| Bundler Mental Model | | | |
| Tree Shaking | | | |
| HMR / Fast Refresh | | | |
| Module Federation | | | |
| TypeScript Config | | | |
| Environment Variables | | | |
| Code Splitting | | | |
| Build Performance | | | |
| Source Maps | | | |
| Library Publishing | | | |
| **Average** | | | |

---

## Revision Notes

- One-line summary: Score yourself 1-5 on 10 topics across three interview rounds to identify gaps.
- Target: all topics at 4+ for senior readiness; majority at 5 for staff.
- Action: any topic below 3 → reread its gold sheet + do 3 active recall questions before next mock.
