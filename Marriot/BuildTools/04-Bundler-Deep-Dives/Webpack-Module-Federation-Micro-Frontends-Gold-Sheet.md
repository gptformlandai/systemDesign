# Webpack Module Federation — Micro-Frontend Architecture — Gold Sheet

> Topic: Module Federation v2 architecture, shell/remote pattern, shared dependencies, runtime integration

---

## 1. Intuition

Module Federation lets multiple independently deployed applications share code at runtime — without build-time coupling. Team A owns the checkout app, Team B owns the product catalog. Each deploys on its own schedule. The shell application loads them dynamically at runtime, just like loading a library from a CDN — but it is your own team's code.

Beginner version:

> Module Federation lets separate apps share code at runtime, so different teams can deploy their features independently.

---

## 2. Definition

- Definition: Module Federation is a Webpack 5 feature that allows multiple separately deployed JavaScript applications to dynamically load modules from each other at runtime, with shared dependency management.
- Category: Micro-frontend architecture pattern.
- Core idea: Runtime module loading across deployment boundaries, not build-time bundling.

---

## 3. The Problem It Solves

```
Traditional monolithic frontend:
  Team A changes checkout
  Team B changes product catalog
  Team C changes navigation
  → Everyone waits for one big release → slow
  → One team's bug blocks everyone's deploy → brittle

Module Federation:
  Team A deploys checkout-app.example.com (independent)
  Team B deploys catalog-app.example.com  (independent)
  Shell app loads both at runtime         → fast, isolated
```

---

## 4. Core Concepts

| Term | Meaning |
|---|---|
| **Host** (Shell) | The main app that consumes remote modules |
| **Remote** | An independently deployed app that exposes modules |
| **Exposed module** | A component/function a remote makes available |
| **Shared module** | A dependency (like React) shared to avoid duplication |
| **Remote entry** | A JS file (`remoteEntry.js`) that a remote publishes, listing its exposed modules |

---

## 5. Minimal Module Federation Setup

### Remote App — exposes a component

```javascript
// webpack.config.js (remote: checkout-app)
const { ModuleFederationPlugin } = require('webpack').container;

module.exports = {
  mode: 'production',
  output: {
    publicPath: 'https://checkout.example.com/',  // important: absolute URL
    filename: '[name].[contenthash].js',
  },
  plugins: [
    new ModuleFederationPlugin({
      name: 'checkout',                          // unique name for this remote
      filename: 'remoteEntry.js',               // entry point consumed by shell
      exposes: {
        './CheckoutPage': './src/pages/CheckoutPage',
        './MiniCart': './src/components/MiniCart',
      },
      shared: {
        react: { singleton: true, requiredVersion: '^18.0.0' },
        'react-dom': { singleton: true, requiredVersion: '^18.0.0' },
      },
    }),
  ],
};
```

### Host App (Shell) — consumes the remote

```javascript
// webpack.config.js (shell)
const { ModuleFederationPlugin } = require('webpack').container;

module.exports = {
  plugins: [
    new ModuleFederationPlugin({
      name: 'shell',
      remotes: {
        checkout: 'checkout@https://checkout.example.com/remoteEntry.js',
        //        ^ name    ^ URL to remoteEntry.js
      },
      shared: {
        react: { singleton: true, requiredVersion: '^18.0.0' },
        'react-dom': { singleton: true, requiredVersion: '^18.0.0' },
      },
    }),
  ],
};
```

### Using the remote component in the shell

```typescript
// src/pages/Cart.tsx in shell app
import React, { Suspense, lazy } from 'react';

// Module Federation imports are always async — they load over the network
const RemoteCheckoutPage = lazy(() => import('checkout/CheckoutPage'));
const RemoteMiniCart = lazy(() => import('checkout/MiniCart'));

export function CartRoute() {
  return (
    <Suspense fallback={<div>Loading checkout...</div>}>
      <RemoteCheckoutPage />
    </Suspense>
  );
}
```

---

## 6. Shared Dependencies — Preventing Duplicate Bundles

Without shared config, both shell and remote would bundle React separately — each app would ship 45KB of React.

```javascript
// With shared config:
shared: {
  react: {
    singleton: true,      // only ONE instance of React across all remotes
    requiredVersion: '^18.0.0',
    eager: false,         // lazy-load shared dep (recommended)
  },
  'react-dom': {
    singleton: true,
    requiredVersion: '^18.0.0',
  },
  // Share a design system (but not singleton — each remote can have own version)
  '@company/design-system': {
    requiredVersion: '^2.0.0',
  },
}
```

**`singleton: true` behavior:**

```
Shell uses React 18.2.0
Remote uses React 18.3.0
→ With singleton: the higher version (18.3.0) is loaded once, shared by all
→ Without singleton: each module has its own React instance → hooks break
```

**When NOT to use singleton:** Non-framework libraries where version isolation is acceptable (date-fns, lodash).

---

## 7. Dynamic Remote Loading — No Build-Time URL Dependency

```typescript
// Shell does not need to know the remote URL at build time
// It can be fetched from a service registry at runtime

async function loadRemote(scope: string, module: string) {
  // Load the remote entry dynamically
  await __webpack_init_sharing__('default');
  const container = window[scope];  // set by remoteEntry.js
  await container.init(__webpack_share_scopes__.default);
  const factory = await container.get(module);
  return factory();
}

// Runtime remote config (from an API)
const remoteConfig = await fetch('/api/micro-frontend-registry').then(r => r.json());
// { checkout: 'https://checkout.example.com/remoteEntry.js' }
```

---

## 8. Error Boundaries for Remote Failures

```typescript
// A remote being down should not crash the entire shell
import { ErrorBoundary } from 'react-error-boundary';

function RemoteCheckout() {
  return (
    <ErrorBoundary
      fallback={<div>Checkout is temporarily unavailable. <a href="/checkout-legacy">Use legacy checkout</a></div>}
    >
      <Suspense fallback={<CheckoutSkeleton />}>
        <LazyRemoteCheckoutPage />
      </Suspense>
    </ErrorBoundary>
  );
}
```

---

## 9. Module Federation vs Alternatives

| Approach | Coupling | Deploy Independence | Complexity |
|---|---|---|---|
| Module Federation | Runtime | Full | High |
| npm library | Build-time | No (republish required) | Low |
| `<iframe>` embedding | Runtime | Full | Medium (postMessage) |
| Single-SPA | Runtime (router) | Full | High |
| Vite + `import()` CDN | Runtime | Full | Medium |

**When Module Federation is worth the complexity:**
- Multiple teams with separate deploy pipelines
- Features change often and independently
- Shared dependency versions must be synchronized

**When it's not worth it:**
- Small team with one repo
- Features rarely need independent deployment
- Added build complexity and debugging difficulty exceed the benefit

---

## 10. Webpack Module Federation v2 (2024+)

Module Federation v2 (`@module-federation/enhanced`) adds:
- **Manifest JSON** — remote describes its capabilities; host reads the manifest at runtime
- **TypeScript support** — type stubs generated for remote modules
- **Retry and fallback** — built-in resilience for failed remote loads
- **Shared state** — shared Zustand/Redux store across remotes

```bash
npm install @module-federation/enhanced
```

```javascript
// webpack.config.js with MF v2
const { ModuleFederationPlugin } = require('@module-federation/enhanced/webpack');

module.exports = {
  plugins: [
    new ModuleFederationPlugin({
      name: 'checkout',
      exposes: { './CheckoutPage': './src/pages/CheckoutPage' },
      shared: { react: { singleton: true } },
      manifest: true,  // generate manifest.json for runtime discovery
    }),
  ],
};
```

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| No `publicPath` on remote | remoteEntry.js loads but chunks fail (wrong relative paths) | Always set absolute `publicPath` on remotes |
| `singleton: false` for React | Two React instances — hooks break across remotes | `singleton: true` for React and React DOM |
| Loading remote without Suspense | Blank screen or crash during async load | Wrap in `<Suspense>` |
| No error boundary for remote | One remote failure crashes entire shell | Wrap each remote in `<ErrorBoundary>` |
| Different React major versions across shell and remote | `singleton` can't reconcile incompatible versions | Coordinate major version upgrades across teams |
| Hardcoding remote URLs in webpack config | Cannot change URLs without rebuilding shell | Use runtime dynamic remote loading from a config API |

---

## 12. Interview Insight

Strong answer:

> Module Federation is Webpack 5's solution for micro-frontend architectures. Each team deploys an independent application that exposes modules via a `remoteEntry.js` file. The shell application loads these at runtime using dynamic imports, wrapping them in Suspense and Error Boundaries. The key technical challenge is shared dependencies: React must be configured as `singleton: true` so there is exactly one instance across all remotes — otherwise React hooks break because each module has its own state. The tradeoff is significant operational complexity (runtime failures, version coordination, debugging across deployment boundaries) so it's only justified when teams genuinely need independent deployment cycles.

Follow-up trap:

> Why does React need `singleton: true` but `date-fns` does not?

Good answer:

> React uses module-level singletons for its internal state — hooks, context, and the fiber tree all live in one React instance. If two instances exist, `useState` in one instance doesn't see state from the other. `date-fns` is a pure utility library with no global state — multiple versions can coexist without conflict.

---

## 13. Revision Notes

- One-line summary: Module Federation loads independently deployed modules at runtime, with shared singleton React to prevent hook breakage.
- Three keywords: remote, singleton, runtime-loading.
- One interview trap: Forgetting `publicPath` breaks remote chunk loading.
- Memory trick: Each remote is a separately deployed shop; the shell is the mall that hosts them all.
