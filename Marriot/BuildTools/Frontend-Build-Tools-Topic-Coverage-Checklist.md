# Frontend Build Tools Coverage Checklist

Use this checklist to confirm that the BuildTools track covers the requested learning surface.

---

## Mandatory Topic Coverage

| # | Topic | File |
|---|---|---|
| 1 | Build pipeline overview | [Build Pipeline Overview](./01-Pipeline-Foundations/Build-Pipeline-Overview-Gold-Sheet.md) |
| 2 | Transpilation | [Transpilation](./02-Transpilation-Modules-Bundling/Transpilation-Babel-SWC-esbuild-TypeScript-Gold-Sheet.md) |
| 3 | Bundling core | [Bundling Core](./02-Transpilation-Modules-Bundling/Bundling-Core-Dependency-Graph-Chunking-Gold-Sheet.md) |
| 4 | Module systems | [Module Systems](./02-Transpilation-Modules-Bundling/Module-Systems-ESM-CommonJS-Tree-Shaking-Gold-Sheet.md) |
| 5 | Code optimization | [Code Optimization](./03-Optimization-DevServer-HMR/Code-Optimization-Tree-Shaking-Minification-Splitting-Gold-Sheet.md) |
| 6 | CSS-in-JS vs CSS Modules vs Vanilla Extract | [CSS-in-JS Build Trade-offs](./02-Transpilation-Modules-Bundling/CSS-in-JS-Build-Tradeoffs-Gold-Sheet.md) |
| 7 | Dev server | [Dev Server](./03-Optimization-DevServer-HMR/Dev-Server-File-Watching-HMR-Pipeline-Gold-Sheet.md) |
| 8 | HMR | [HMR](./03-Optimization-DevServer-HMR/HMR-Hot-Reload-Internals-State-Preservation-Gold-Sheet.md) |
| 9 | Webpack | [Webpack Deep Dive](./04-Bundler-Deep-Dives/Webpack-Deep-Dive-Loaders-Plugins-DevServer-Gold-Sheet.md) |
| 10 | Vite | [Vite Modern Dev Server](./04-Bundler-Deep-Dives/Vite-Modern-Dev-Server-Rollup-Production-Gold-Sheet.md) |
| 11 | Parcel | [Parcel Zero-Config Pipeline](./04-Bundler-Deep-Dives/Parcel-Zero-Config-Auto-Optimization-Gold-Sheet.md) |
| 12 | Modern build tools | [Modern Tools](./04-Bundler-Deep-Dives/Modern-Build-Tools-esbuild-Rollup-Turbopack-Metro-Gold-Sheet.md) |
| 13 | Performance pipeline | [Build Performance Pipeline](./05-Performance-Debugging-Architecture/Build-Performance-Pipeline-Cold-Start-Hot-Updates-Gold-Sheet.md) |
| 14 | Debugging builds | [Debugging Builds](./05-Performance-Debugging-Architecture/Debugging-Builds-Source-Maps-Bundle-Issues-Gold-Sheet.md) |
| 15 | Real-world pipeline design | [Pipeline Design](./05-Performance-Debugging-Architecture/Real-World-Build-Pipeline-Design-CICD-Gold-Sheet.md) |

---

## Required Learning Quality

| Requirement | Status |
|---|---|
| Internal flow explanations | Covered in every module |
| Pipeline diagrams | Covered in every module |
| Tool comparisons | Covered across comparison tables and architecture sections |
| Real-world examples | Covered in every module |
| Beginner intuition | Covered in every module |
| Developer mistakes | Covered in every module |
| Interview insights | Covered in every module |
| React web relevance | Covered |
| Next.js relevance | Covered |
| React Native relevance | Covered, especially through Metro |
| Expo relevance | Covered through Expo CLI, Expo Go, development builds, EAS, and Expo+Metro flow |
| Reading startup logs | Covered for React web, Next.js, Expo, and React Native |
| MAANG system design framing | Covered |

---

## Beginner To Pro Progression

1. Beginner: understand why browsers need transformed code.
2. Junior: understand JSX, TypeScript, CSS, and asset transforms.
3. Mid-level: understand dependency graphs, chunks, source maps, and HMR.
4. Senior: choose tools based on product constraints.
5. MAANG-ready: explain build architecture, trade-offs, failure modes, and performance decisions clearly.

---

## Fast Revision Map

```txt
Syntax problem?
  -> Transpilation

Import problem?
  -> Module systems and resolver

Large bundle?
  -> Code splitting, tree shaking, vendor chunk analysis

Slow npm run dev?
  -> Dev server, dependency pre-bundling, file watching, monorepo config

Hot reload not preserving state?
  -> HMR boundary design

Production-only bug?
  -> Minification, tree shaking, env variables, source maps

React Native bundle issue?
  -> Metro resolver, platform extensions, assets, watchFolders

Expo app starts but screen is blank?
  -> read Expo CLI logs: server started, device connected, bundle requested, bundle completed, JS/runtime logs appeared

Next.js build issue?
  -> Server/client boundary, framework compiler, route-level chunks, cache behavior
```
