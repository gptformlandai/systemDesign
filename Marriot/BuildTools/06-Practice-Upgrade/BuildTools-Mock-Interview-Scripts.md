# Build Tools — Mock Interview Scripts — Gold Sheet

> Three timed interview rounds with expected coverage and evaluation checkpoints

---

## How to Use This File

Use with a study partner or self-practice with a timer. The interviewer reads the questions. The candidate answers verbally (no notes). After each round, check coverage against the expected points.

---

## Round 1: Junior → Mid-Level Screen (25 minutes)

**Context:** You are interviewing for a mid-level frontend role. The interviewer wants to verify solid fundamentals.

---

**[0:00] Q1 — Opener**

> "Walk me through what happens when you run `npm run build` in a Webpack project — from source files to what lands in the browser."

*Expected coverage (3+ of these):*
- [ ] Entry point scanning
- [ ] Dependency graph traversal
- [ ] Loaders transform non-JS files (CSS, images, TypeScript)
- [ ] Plugins manipulate the build pipeline (HtmlWebpackPlugin, MiniCssExtractPlugin)
- [ ] Output: chunked JS files with content hashes
- [ ] Source maps generated separately

---

**[4:00] Q2 — Practical Config**

> "Here's a failing webpack config — what's wrong?"

```javascript
module.exports = {
  module: {
    rules: [{
      test: /\.scss$/,
      use: ['style-loader', 'sass-loader', 'css-loader']
    }]
  }
};
```

*Expected coverage:*
- [ ] Loaders execute right-to-left (bottom-to-top)
- [ ] Correct order: sass-loader → css-loader → style-loader
- [ ] Bonus: explains what each loader does

---

**[7:00] Q3 — HMR**

> "What is Hot Module Replacement and how does it differ from a full page reload?"

*Expected coverage:*
- [ ] Module replacement without page reload
- [ ] State is preserved
- [ ] WebSocket communication
- [ ] Falls back to full reload when an error occurs
- [ ] Bonus: React Fast Refresh vs webpack-hmr

---

**[10:00] Q4 — Environment Variables**

> "How do you safely add an API base URL to a Vite app? What about a secret key?"

*Expected coverage:*
- [ ] `VITE_API_URL` prefix for public exposure
- [ ] `import.meta.env.VITE_API_URL` in code
- [ ] Secret keys MUST NOT be prefixed with VITE_
- [ ] Bonus: `.env.local` vs `.env.production`

---

**[14:00] Q5 — Tree Shaking**

> "Why does importing lodash like `import _ from 'lodash'` bundle the entire library?"

*Expected coverage:*
- [ ] CommonJS `module.exports` is runtime — cannot be statically analyzed
- [ ] Bundler cannot know which properties are used before execution
- [ ] Fix: `lodash-es` (ESM version) or cherry-pick: `import { uniq } from 'lodash-es'`
- [ ] Bonus: `sideEffects: false` requirement

---

**[18:00] Q6 — DevServer**

> "What is the core architectural difference between Vite's dev server and Webpack's?"

*Expected coverage:*
- [ ] Webpack: full bundle before serve (slow cold start)
- [ ] Vite: native ESM, browser requests individual files
- [ ] esbuild pre-bundles node_modules
- [ ] Vite dev is faster; production uses Rollup

---

**[22:00] Q7 — Closer**

> "If a colleague added a large library to the project and the bundle size jumped 200KB, what's your debugging approach?"

*Expected coverage:*
- [ ] Bundle analyzer (webpack-bundle-analyzer or Vite visualizer)
- [ ] Check bundlephobia for the library's size
- [ ] Look for tree-shaking opportunity or lighter alternative
- [ ] Bonus: dynamic import to lazy-load if not needed on initial render

---

**Round 1 Score (out of 7 questions):**

| Score | Signal |
|---|---|
| 7/7 | Ready for mid-level; proceed to Round 2 |
| 5-6/7 | One more review session on weak areas |
| < 5/7 | Review Group 01-03 gold sheets |

---

## Round 2: Mid → Senior Technical Deep Dive (35 minutes)

**Context:** Senior frontend / full-stack engineer role at a product company. Heavy system design flavor.

---

**[0:00] Q1 — Module Federation Concept**

> "Your company has 4 frontend teams. Each team deploys their feature on an independent schedule. How would you architect the frontend to support this?"

*Expected coverage:*
- [ ] Micro-frontend architecture
- [ ] Module Federation as the solution
- [ ] Shell / remote pattern
- [ ] Each team deploys a `remoteEntry.js`
- [ ] Shell loads remotes at runtime
- [ ] Bonus: shared React singleton, error boundaries

---

**[6:00] Q2 — Module Federation Technical Detail**

> "Walk me through the `ModuleFederationPlugin` config for the shell and a remote. What is `singleton: true` and why is it required for React?"

*Expected coverage:*
- [ ] `name`, `filename: 'remoteEntry.js'`, `exposes`, `remotes`
- [ ] `shared: { react: { singleton: true } }`
- [ ] singleton: true → one React instance across all remotes
- [ ] Without it: two React instances → hooks break
- [ ] Bonus: React uses module-level singleton for fiber tree

---

**[12:00] Q3 — Production Build Optimization**

> "Your Next.js app's Lighthouse performance score is 38. What's your systematic approach to improving it?"

*Expected coverage:*
- [ ] Bundle analysis (identify large dependencies)
- [ ] Code splitting (dynamic imports for large routes/components)
- [ ] Image optimization (next/image, WebP, lazy loading)
- [ ] Font optimization (next/font for build-time download)
- [ ] Caching strategy (content hashes, long TTL for static assets)
- [ ] Bonus: Core Web Vitals — LCP, FID, CLS targets

---

**[18:00] Q4 — TypeScript Config**

> "A new team member added `"moduleResolution": "Node"` to the tsconfig and now TypeScript can't find `date-fns/format`. What's happening?"

*Expected coverage:*
- [ ] `Node` resolution doesn't understand `exports` field
- [ ] `date-fns` v3 uses subpath exports
- [ ] Fix: `moduleResolution: "Bundler"` or `"NodeNext"`
- [ ] Bonus: difference between `module` and `moduleResolution`

---

**[22:00] Q5 — Monorepo Build**

> "Your monorepo CI takes 12 minutes. You've installed Turborepo but cache hit rate is 0%. What's wrong?"

*Expected coverage:*
- [ ] Turborepo default: hash ALL files (includes generated files that change every run)
- [ ] Fix: explicit `inputs` field listing only source files
- [ ] Remote caching (Vercel) needed for CI machines
- [ ] Bonus: `dependsOn: ["^build"]` for correct build order

---

**[28:00] Q6 — Source Maps**

> "Sentry is showing minified stack traces in production. How do you fix this without exposing your source code to users?"

*Expected coverage:*
- [ ] `devtool: 'hidden-source-map'` generates maps without linking them
- [ ] Upload maps to Sentry via CI
- [ ] Delete maps from deploy artifact
- [ ] Bonus: `SENTRY_AUTH_TOKEN` env var in CI

---

**[32:00] Q7 — Trade-off Question**

> "A team wants to migrate from Create React App to Vite. What risks and migration steps would you outline?"

*Expected coverage:*
- [ ] CRA uses webpack; Vite uses Rollup (prod) + ESM (dev)
- [ ] `process.env.REACT_APP_` → `import.meta.env.VITE_`
- [ ] `index.html` moves to project root and references `<script type="module">`
- [ ] CommonJS node_modules may need `optimizeDeps.include`
- [ ] Risks: custom webpack plugins not available in Vite, CJS-only deps
- [ ] Bonus: `CRACO` or `vite-cra-compat` migration paths

---

**Round 2 Score (out of 7 questions):**

| Score | Signal |
|---|---|
| 7/7 | Strong senior signal; proceed to Round 3 for staff/principal prep |
| 5-6/7 | Mid-senior level; targeted review on gaps |
| < 5/7 | Deep review Groups 04-05 before interviewing |

---

## Round 3: Staff/Principal System Design (45 minutes)

**Context:** Staff engineer or tech lead role. Open-ended problems, architectural judgment, tradeoffs.

---

**[0:00] Q1 — Open Design**

> "Design the build pipeline for a new e-commerce platform with 5 teams, expected 500K monthly active users, and a monorepo codebase. Walk me through your architecture and tool choices."

*Expected coverage:*
- [ ] Tool choices: Next.js for web, Turborepo for monorepo orchestration
- [ ] Shared packages: UI library, TypeScript configs, ESLint configs
- [ ] Independent team deployment model (with or without Module Federation)
- [ ] CI pipeline: type-check + lint + test + build in parallel; Turborepo cache
- [ ] Performance: code splitting, image optimization, CDN
- [ ] Monitoring: bundle budgets in CI, error tracking with source maps

*Strong signal:* Candidate mentions tradeoffs (Module Federation complexity vs monorepo), discusses team autonomy vs shared version coordination, raises operational concerns.

---

**[15:00] Q2 — Library Ecosystem**

> "Your design system team wants to publish `@company/ui` as an npm package for internal consumption. What does the full publishing pipeline look like?"

*Expected coverage:*
- [ ] tsup for dual CJS + ESM build
- [ ] `package.json` `exports` field with `import`, `require`, `types`
- [ ] `sideEffects: ["**/*.css"]` — keep CSS, tree-shake JS
- [ ] `peerDependencies: { react, react-dom }` — avoid duplicate React
- [ ] `publint` validation before publish
- [ ] Versioning: semver + changeset automation
- [ ] Bonus: `workspace:` protocol in monorepo, CI automated release

---

**[25:00] Q3 — Incident Walk-through**

> "Tell me about a time a production build problem caused an incident. Walk me through how you diagnosed and fixed it."

*Evaluation:*
This is open-ended. If the candidate lacks a real example, they may use one of the case studies from this track. Look for:
- [ ] Systematic approach (symptoms → investigation → root cause → fix)
- [ ] Use of tooling (bundle analyzer, git bisect, CI logs)
- [ ] Lesson learned and prevention added
- [ ] Blameless post-mortem framing

---

**[35:00] Q4 — Future Evolution**

> "Turbopack is now production-ready for Next.js. How would you evaluate migrating from Webpack to Turbopack in an existing production app?"

*Expected coverage:*
- [ ] Incremental model vs Webpack's file-level cache
- [ ] Current state: Turbopack in prod for Next.js dev server only (as of 2025)
- [ ] Migration risk: custom Webpack plugins won't work in Turbopack
- [ ] Evaluation approach: canary flag + measurement on cold start and HMR times
- [ ] Decision criteria: team size, custom plugin usage, actual measured improvement
- [ ] Bonus: distinguish hype from production readiness

---

**Round 3 Score:**

| Signal | Indicator |
|---|---|
| Strong | Candidate drives the design, anticipates tradeoffs, references actual metrics |
| Adequate | Covers core concepts but needs prompting for tradeoffs |
| Weak | Cannot design end-to-end pipeline without heavy guidance |

---

## Revision Notes

- One-line summary: Three timed rounds from junior screen to staff design; self-score after each round.
- Practice cadence: Do Round 1 after first pass through materials; Round 2 after second pass; Round 3 when you feel interview-ready.
- Partner tip: Record yourself for Round 3. Watch it back and score your own answers.
