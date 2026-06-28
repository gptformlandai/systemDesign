# Frontend Build Tools and Bundling Mastery Track

> Goal: master how React, Next.js, and React Native code becomes executable production software, from beginner intuition to MAANG-level interview and architecture clarity.
> 
> **Track size: 28 content files across 6 groups** (8 new topic files + 6 practice files added; CSS-in-JS Build Trade-offs added as new file)

---

## How To Use This Track

Read this track in order if you are new. If you already build apps, use it as a debugging and system design reference.

The mental model:

```txt
Your Code: React / TypeScript / CSS / Assets
        |
        v
Transpilation: Babel / SWC / esbuild / TypeScript
        |
        v
Dependency Graph Creation
        |
        v
Bundling or Module Serving
        |
        v
Optimization: tree shaking / minification / code splitting
        |
        v
Dev Server or Production Hosting
        |
        v
Browser / Node runtime / React Native JavaScript runtime
```

Old world:

```txt
Webpack dev server
  -> build a large graph before the browser can start
```

New world:

```txt
Vite / modern dev tools
  -> serve source modules on demand during dev
  -> optimize dependencies and production bundles separately
```

React Native world:

```txt
Metro
  -> resolve JS and assets
  -> transform platform-specific files
  -> serialize bundles for iOS, Android, and dev reload
```

Expo world:

```txt
Expo CLI
  -> starts Metro
  -> chooses Expo Go or development build
  -> serves JS bundle/assets to device
  -> uses EAS for native builds and compatible OTA updates
```

---

## Learning Path

### Phase 1: Pipeline Foundations

1. [Build Pipeline Overview](./01-Pipeline-Foundations/Build-Pipeline-Overview-Gold-Sheet.md)

You learn why build tools exist, what happens from source to served app, and why browsers cannot directly run everything developers write.

### Phase 2: Transpilation, Modules, and Bundling

2. [Transpilation: Babel, SWC, esbuild, TypeScript](./02-Transpilation-Modules-Bundling/Transpilation-Babel-SWC-esbuild-TypeScript-Gold-Sheet.md)
3. [Bundling Core: Dependency Graph, Entry Points, Chunking](./02-Transpilation-Modules-Bundling/Bundling-Core-Dependency-Graph-Chunking-Gold-Sheet.md)
4. [Module Systems: ES Modules, CommonJS, Tree Shaking](./02-Transpilation-Modules-Bundling/Module-Systems-ESM-CommonJS-Tree-Shaking-Gold-Sheet.md)
5. [TypeScript Config Deep Dive](./02-Transpilation-Modules-Bundling/TypeScript-Config-Deep-Dive-Gold-Sheet.md) ★ New
6. [CSS and PostCSS Processing Build Pipeline](./02-Transpilation-Modules-Bundling/CSS-PostCSS-Processing-Build-Pipeline-Gold-Sheet.md) ★ New
7. [CSS-in-JS vs CSS Modules vs Vanilla Extract — Build Trade-offs](./02-Transpilation-Modules-Bundling/CSS-in-JS-Build-Tradeoffs-Gold-Sheet.md) ★ New

You learn how modern syntax becomes compatible JavaScript, how imports form a graph, how module format affects optimization, how TypeScript config drives bundler compatibility, how CSS flows through the build pipeline, and when to choose runtime vs. zero-runtime vs. build-time CSS strategies.

### Phase 3: Optimization, Dev Server, and HMR

8. [Code Optimization: Tree Shaking, Minification, Splitting](./03-Optimization-DevServer-HMR/Code-Optimization-Tree-Shaking-Minification-Splitting-Gold-Sheet.md)
9. [Dev Server: npm run dev, File Watching, HMR Pipeline](./03-Optimization-DevServer-HMR/Dev-Server-File-Watching-HMR-Pipeline-Gold-Sheet.md)
10. [HMR: Hot Reload Internals and State Preservation](./03-Optimization-DevServer-HMR/HMR-Hot-Reload-Internals-State-Preservation-Gold-Sheet.md)
11. [Asset Handling: Images, Fonts, SVG](./03-Optimization-DevServer-HMR/Asset-Handling-Images-Fonts-SVG-Gold-Sheet.md) ★ New
12. [Environment Variables and Feature Flags Build](./03-Optimization-DevServer-HMR/Environment-Variables-Feature-Flags-Build-Gold-Sheet.md) ★ New

You learn how development stays fast, how hot updates are delivered, how assets flow through the pipeline, and how environment variables are securely injected at build time.

### Phase 4: Bundler Deep Dives

13. [Webpack Deep Dive](./04-Bundler-Deep-Dives/Webpack-Deep-Dive-Loaders-Plugins-DevServer-Gold-Sheet.md)
14. [Vite Modern Dev Server and Production Bundling](./04-Bundler-Deep-Dives/Vite-Modern-Dev-Server-Rollup-Production-Gold-Sheet.md)
15. [Parcel Zero-Config Pipeline](./04-Bundler-Deep-Dives/Parcel-Zero-Config-Auto-Optimization-Gold-Sheet.md)
16. [Modern Tools: esbuild, Rollup, Turbopack, Metro](./04-Bundler-Deep-Dives/Modern-Build-Tools-esbuild-Rollup-Turbopack-Metro-Gold-Sheet.md)
17. [Library Publishing Pipeline: tsup and Rollup](./04-Bundler-Deep-Dives/Library-Publishing-Pipeline-tsup-Rollup-Gold-Sheet.md) ★ New
18. [Webpack Module Federation and Micro-Frontends](./04-Bundler-Deep-Dives/Webpack-Module-Federation-Micro-Frontends-Gold-Sheet.md) ★ New

You learn the philosophy and internal pipeline of the major tools, how to publish tree-shakeable libraries, and how to architect micro-frontend systems with Module Federation.

### Phase 5: Performance, Debugging, and Architecture

19. [Build Performance Pipeline](./05-Performance-Debugging-Architecture/Build-Performance-Pipeline-Cold-Start-Hot-Updates-Gold-Sheet.md)
20. [Debugging Builds: Source Maps and Bundle Issues](./05-Performance-Debugging-Architecture/Debugging-Builds-Source-Maps-Bundle-Issues-Gold-Sheet.md)
21. [Real-World Pipeline Design and CI/CD](./05-Performance-Debugging-Architecture/Real-World-Build-Pipeline-Design-CICD-Gold-Sheet.md)
22. [Monorepo Build: Turborepo, Nx, and Workspaces](./05-Performance-Debugging-Architecture/Monorepo-Build-Turborepo-Nx-Workspaces-Gold-Sheet.md) ★ New

You learn how to choose tools, debug production build failures, design scalable CI/CD pipelines, and orchestrate monorepo builds efficiently.

### Phase 6: Practice Upgrade

23. [Tricky Scenario Questions](./06-Practice-Upgrade/BuildTools-Tricky-Scenario-Questions-Gold-Sheet.md) ★ New
24. [Production Debugging Case Studies](./06-Practice-Upgrade/BuildTools-Production-Debugging-Case-Studies-Gold-Sheet.md) ★ New
25. [Active Recall Question Bank](./06-Practice-Upgrade/BuildTools-Active-Recall-Question-Bank.md) ★ New
26. [Mock Interview Scripts](./06-Practice-Upgrade/BuildTools-Mock-Interview-Scripts.md) ★ New
27. [Interview Scoring Rubrics](./06-Practice-Upgrade/BuildTools-Interview-Scoring-Rubrics.md) ★ New
28. [Mastery Roadmap](./06-Practice-Upgrade/BuildTools-Mastery-Roadmap.md) ★ New

Practice infrastructure: diagnose broken configs, walk through production incidents, do spaced repetition, time yourself on mock interviews, self-score with rubrics, and follow a structured 7- or 14-day mastery plan.

---

## What This Track Covers

| Area | Covered |
|---|---|
| Browser rendering constraints | Yes |
| Transpilation | Yes |
| Babel, SWC, esbuild | Yes |
| TypeScript and JSX | Yes |
| Polyfills and browser compatibility | Yes |
| Bundling and dependency graphs | Yes |
| Entry points and chunks | Yes |
| ES Modules and CommonJS | Yes |
| Tree shaking and minification | Yes |
| Code splitting and lazy loading | Yes |
| Dev server internals | Yes |
| File watching | Yes |
| HMR internals | Yes |
| React Fast Refresh internals | Yes |
| Webpack | Yes |
| Vite | Yes |
| Parcel | Yes |
| esbuild | Yes |
| Rollup | Yes |
| Turbopack | Yes |
| Metro for React Native | Yes |
| Expo CLI, Expo Go, development builds, EAS | Yes |
| TypeScript tsconfig deep dive | Yes |
| CSS and PostCSS pipeline | Yes |
| Asset handling (images, fonts, SVG) | Yes |
| Environment variables and feature flags | Yes |
| Library publishing (tsup, dual CJS/ESM) | Yes |
| Module Federation micro-frontends | Yes |
| Monorepo build (Turborepo, Nx) | Yes |
| Reading frontend startup logs | Yes |
| Source maps | Yes |
| CI/CD build design + bundle budgets | Yes |
| Practice scenarios and case studies | Yes |
| Mock interviews and scoring rubrics | Yes |
| MAANG interview framing | Yes |

---

## Interview Communication Formula

When asked about build tools, answer in this shape:

1. State the pipeline.
2. Explain what happens differently in development and production.
3. Mention dependency graph creation.
4. Mention transforms: JSX, TypeScript, CSS, assets.
5. Mention optimization: tree shaking, minification, splitting.
6. Mention the tool philosophy: Webpack builds a graph, Vite serves ESM on demand, Metro targets native platforms.
7. Mention failure modes: sourcemaps, duplicate dependencies, bad polyfills, large vendor chunks, HMR state bugs.

Strong interview sentence:

> A bundler is not just a file combiner. It is a compiler pipeline that resolves imports, transforms syntax and assets, builds a dependency graph, creates optimized chunks, and serves either fast development modules or production-ready bundles.

---

## Current Ecosystem Notes

- React web apps commonly use Vite, Webpack, Parcel, or framework-owned tooling.
- Next.js uses its own compiler and build pipeline, with SWC and Turbopack as important parts of the modern Next.js story.
- React Native uses Metro as the default JavaScript bundler.
- Expo apps use Expo CLI to orchestrate the workflow, but Metro still handles the React Native JavaScript and asset bundle.
- Library authors often use Rollup, esbuild-based tools, or tsup-like wrappers because package output requirements differ from app output requirements.

---

## Official References Used For Alignment

- Webpack concepts: https://webpack.js.org/concepts/
- Vite guide: https://vite.dev/guide/
- Parcel docs: https://parceljs.org/docs/
- esbuild docs: https://esbuild.github.io/
- Rollup docs: https://rollupjs.org/introduction/
- Next.js Turbopack docs: https://nextjs.org/docs/app/api-reference/turbopack
- Babel docs: https://babeljs.io/docs/
- SWC docs: https://swc.rs/docs/getting-started
- Metro concepts: https://metrobundler.dev/docs/concepts/
- React Native Metro docs: https://reactnative.dev/docs/metro
- Expo CLI docs: https://docs.expo.dev/more/expo-cli/
- Expo development builds docs: https://docs.expo.dev/develop/development-builds/introduction/
- Expo Metro docs: https://docs.expo.dev/guides/customizing-metro/
- Expo debugging tools docs: https://docs.expo.dev/debugging/tools/
