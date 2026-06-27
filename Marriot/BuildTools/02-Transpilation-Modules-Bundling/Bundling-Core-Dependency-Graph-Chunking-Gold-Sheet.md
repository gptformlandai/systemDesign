# Bundling Core: Dependency Graph, Entry Points, Chunking Gold Sheet

> Topic: what bundling is, how dependency graphs work, and how output chunks are created.

---

## 1. Intuition

Bundling starts with one file and follows every import until it understands the whole app. Then it decides how to package the code into files the runtime can load efficiently.

Beginner version:

> A bundler follows imports like a map, then packs related code into deliverable files.

---

## 2. Definition

- Definition: Bundling is the process of resolving modules into a dependency graph and emitting one or more executable output files.
- Category: Build graph and packaging step.
- Core idea: Entry points create graphs; graphs become chunks.

---

## 3. What Is A Dependency Graph?

```txt
main.tsx
  |
  +-- App.tsx
  |     |
  |     +-- routes/Dashboard.tsx
  |     |     |
  |     |     +-- charts.ts
  |     |
  |     +-- routes/Settings.tsx
  |
  +-- global.css
```

Graph language:

- Node: a module such as `App.tsx`.
- Edge: an import relationship.
- Entry point: where graph traversal begins.
- Chunk: emitted output group.
- Asset: non-JS output such as CSS, image, font, or source map.

---

## 4. Internal Flow

```txt
Entry point
   |
   v
Resolve import specifiers
   |
   v
Load source files
   |
   v
Transform each module
   |
   v
Record imports and exports
   |
   v
Create dependency graph
   |
   v
Plan chunks
   |
   v
Emit JS, CSS, assets, maps
```

Resolution example:

```ts
import { formatPrice } from '@/lib/money';
```

Resolver checks:

- Alias `@`.
- File extensions.
- `package.json` fields.
- Platform-specific files for React Native.
- Browser/server conditions.

---

## 5. Entry Points

An entry point is the root of a build.

React SPA:

```txt
src/main.tsx
```

Multi-page app:

```txt
src/home.tsx
src/pricing.tsx
src/admin.tsx
```

Library:

```txt
src/index.ts
src/react.ts
src/node.ts
```

Next.js:

```txt
app/page.tsx
app/dashboard/page.tsx
app/api/search/route.ts
```

React Native:

```txt
index.js
```

---

## 6. Chunking Strategies

### Single Bundle

```txt
app.js
```

Good for tiny apps. Bad for large apps because users download everything at once.

### Route-Based Chunks

```txt
home.chunk.js
dashboard.chunk.js
settings.chunk.js
```

Good for apps where routes are natural loading boundaries.

### Vendor Chunk

```txt
vendor.react.js
vendor.charting.js
app.js
```

Good when dependencies change less often than app code.

### Dynamic Import Chunk

```tsx
const AdminPanel = lazy(() => import('./AdminPanel'));
```

The bundler creates a separate chunk for `AdminPanel`.

### Framework Route Chunks

Next.js and similar frameworks create chunks based on routes, layouts, server/client boundaries, and runtime requirements.

---

## 7. Code Example: Dynamic Import

```tsx
import { Suspense, lazy } from 'react';

const ReportsPage = lazy(() => import('./ReportsPage'));

export function App() {
  return (
    <Suspense fallback={<p>Loading reports...</p>}>
      <ReportsPage />
    </Suspense>
  );
}
```

Pipeline:

```txt
App.tsx
  -> static imports stay in initial chunk
  -> import('./ReportsPage') becomes async chunk boundary
  -> browser loads ReportsPage only when needed
```

---

## 8. React, Next.js, React Native Differences

| Runtime | Bundling Goal | Important Detail |
|---|---|---|
| React SPA | Browser bundles/chunks | Optimize first load and cache behavior |
| Next.js | Server and client output | Respect Server Component and Client Component boundaries |
| React Native | Native JS bundle/assets | Platform-specific resolution matters |
| Library package | Reusable package files | Preserve module formats and peer dependencies |

React Native Metro mental model:

```txt
entry file
  -> resolve platform modules
  -> transform modules
  -> serialize bundle
```

---

## 9. Tool Comparison Lens

| Tool | Graph Style | Chunking Mental Model |
|---|---|---|
| Webpack | Full configurable graph | Entry, async imports, splitChunks |
| Vite | ESM dev graph plus production bundle graph | On-demand dev modules, optimized prod chunks |
| Parcel | Inferred asset graph | Automatic packaging and splitting |
| Rollup | ESM-first graph | Strong library-oriented chunking |
| Metro | React Native module graph | Platform-aware bundle serialization |
| Next.js tooling | Route and server/client graph | Framework-created route and runtime chunks |

---

## 10. What Bundlers Must Handle

- JavaScript and TypeScript modules.
- CSS imports and extraction.
- Images, fonts, and static assets.
- JSON imports.
- Web workers.
- Dynamic imports.
- Package exports and conditional exports.
- Environment-specific code.
- Source map generation.

---

## 11. Real-World Example

E-commerce app:

```txt
initial chunk
  -> React, app shell, home route

product chunk
  -> product page components

cart chunk
  -> cart and checkout flow

admin chunk
  -> admin-only dashboard

vendor chunk
  -> stable dependency code
```

Why this matters:

- First-time visitors should not download admin dashboard code.
- Repeat visitors benefit from cached vendor code.
- Product pages can load only product-specific logic.

---

## 12. Common Mistakes

### Mistake: One giant bundle for a growing app

- Why wrong: Users pay initial download and parse cost for code they may never use.
- Better approach: Split by route and heavy features.

### Mistake: Too many tiny chunks

- Why wrong: Network overhead and waterfall requests can hurt performance.
- Better approach: Split at meaningful boundaries.

### Mistake: Bundling duplicate dependencies

- Why wrong: Multiple versions of React or design-system packages increase size and can break hooks.
- Better approach: Audit dependency graph and package manager resolution.

### Mistake: Ignoring side effects

- Why wrong: Incorrect side effect metadata can remove required code or keep unused code.
- Better approach: Understand package `sideEffects` and tree-shaking rules.

---

## 13. Trade-Offs

| Strategy | Gain | Cost |
|---|---|---|
| Single bundle | Simple loading | Poor scale |
| Route chunks | Better first load | More chunk management |
| Vendor chunks | Better long-term caching | Vendor chunk can become huge |
| Dynamic imports | Load heavy code only when needed | Loading states and error paths needed |
| Manual chunk config | Control | Can become fragile |

---

## 14. Interview Insight

Strong answer:

> Bundling begins with entry points. The tool resolves every static and dynamic import into a dependency graph, transforms modules, then emits chunks. Chunking strategy is a performance decision: fewer chunks reduce coordination overhead, while more targeted chunks reduce unused initial code.

Follow-up trap:

> Is code splitting always good?

Good answer:

> No. Code splitting helps when it avoids loading code that is not needed immediately. Too much splitting can create network waterfalls, extra loading states, and cache fragmentation.

---

## 15. Revision Notes

- One-line summary: Bundling turns an import graph into runtime-loadable chunks.
- Three keywords: entry, graph, chunk.
- One interview trap: More chunks are not automatically better.
- Memory trick: Imports are roads; the bundler builds the delivery map.
