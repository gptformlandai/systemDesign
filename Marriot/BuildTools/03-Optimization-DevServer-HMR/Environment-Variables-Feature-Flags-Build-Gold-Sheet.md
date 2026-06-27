# Environment Variables and Feature Flags in the Build Pipeline — Gold Sheet

> Topic: .env files, NODE_ENV, client vs server exposure, security rules, Vite/Next.js/webpack patterns

---

## 1. Intuition

Environment variables let you ship one codebase to many environments — development, staging, production — with different API URLs, feature flags, and secrets. The danger is accidentally shipping server-side secrets (API keys, database URLs) to the browser. Build tools have different systems for this, and understanding the boundary prevents security incidents.

Beginner version:

> Environment variables are a way to give your app different values depending on where it is running — and you must never put secrets in ones that reach the browser.

---

## 2. Definition

- Definition: Environment variables are named values injected into the build process or runtime, controlling behavior across environments without changing source code.
- Category: Build pipeline configuration.
- Core idea: Static replacement at build time vs runtime injection; client vs server exposure boundary.

---

## 3. How Build Tools Inject Environment Variables

### Static replacement (compile-time substitution)

Bundlers replace `process.env.VARIABLE_NAME` with the literal string value at build time. This is not a real runtime lookup — it is a text substitution.

```typescript
// Source code
if (process.env.NODE_ENV === 'production') {
  setupAnalytics();
}

// After build (Webpack/Vite define plugin)
if ("production" === 'production') {
  setupAnalytics();
}

// After minification (dead code eliminated)
setupAnalytics();
```

This is how `process.env.NODE_ENV === 'development'` dead code is tree-shaken from production builds.

---

## 4. Vite Environment Variables

Vite uses `import.meta.env` (ESM standard) instead of `process.env`.

```bash
# .env (loaded in all environments)
VITE_API_URL=https://api.example.com
VITE_FEATURE_DARK_MODE=true

# .env.development (only in dev)
VITE_API_URL=http://localhost:4000

# .env.production (only in production build)
VITE_API_URL=https://api.prod.example.com

# .env.local (local overrides — gitignored)
VITE_API_URL=http://localhost:5000
```

**The `VITE_` prefix rule:** Only variables prefixed with `VITE_` are exposed to client-side code. All others are server-side only and not accessible in the browser bundle.

```typescript
// In component code
const apiUrl = import.meta.env.VITE_API_URL;       // works — exposed
const secret = import.meta.env.DB_PASSWORD;         // undefined — not exposed
const isDev = import.meta.env.DEV;                  // Vite built-in boolean
const isProd = import.meta.env.PROD;                // Vite built-in boolean
const mode = import.meta.env.MODE;                  // 'development' | 'production' | custom
```

**TypeScript support:**

```typescript
// vite-env.d.ts (or src/env.d.ts)
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_FEATURE_DARK_MODE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

---

## 5. Next.js Environment Variables

Next.js uses `process.env` with a prefix convention.

```bash
# .env.local (gitignored)
DATABASE_URL=postgresql://localhost/mydb      # server-only
JWT_SECRET=supersecret                        # server-only

NEXT_PUBLIC_API_URL=https://api.example.com  # exposed to browser
NEXT_PUBLIC_ANALYTICS_ID=UA-12345            # exposed to browser
```

**The `NEXT_PUBLIC_` prefix rule:** Only variables prefixed with `NEXT_PUBLIC_` are inlined into the client bundle. Others are only accessible in Server Components, API routes, Server Actions, and middleware.

```typescript
// Server Component or API route — can access both
const dbUrl = process.env.DATABASE_URL;           // works
const apiUrl = process.env.NEXT_PUBLIC_API_URL;   // works

// Client Component — only NEXT_PUBLIC_ available
'use client';
const apiUrl = process.env.NEXT_PUBLIC_API_URL;   // works — inlined at build time
const dbUrl = process.env.DATABASE_URL;           // undefined — never exposed to client
```

**Loading order (each level overrides the previous):**

```
.env
.env.local
.env.[NODE_ENV]           (.env.development, .env.production, .env.test)
.env.[NODE_ENV].local     (.env.development.local)
```

---

## 6. Webpack — DefinePlugin

```javascript
// webpack.config.js
const webpack = require('webpack');
const dotenv = require('dotenv');

const env = dotenv.config().parsed;

module.exports = {
  plugins: [
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
      'process.env.API_URL': JSON.stringify(env.API_URL),
      // Only include variables that should reach the browser
    }),
  ],
};
```

**Security:** Never do `...Object.fromEntries(Object.entries(env).map(...))` — that exposes everything including secrets.

---

## 7. Security Rules — What Must Never Reach the Browser

```bash
# NEVER put these in VITE_* or NEXT_PUBLIC_*:
DATABASE_URL=postgresql://...      # database credentials
JWT_SECRET=...                     # signing key
STRIPE_SECRET_KEY=sk_live_...      # payment API secret
AWS_SECRET_ACCESS_KEY=...          # cloud credentials
SMTP_PASSWORD=...                  # email server password
```

```bash
# Safe for browser (public by nature):
VITE_API_URL=https://api.example.com
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_...  # publishable key only
NEXT_PUBLIC_ANALYTICS_ID=G-12345
NEXT_PUBLIC_APP_NAME=MyApp
```

**How secrets leak:** A developer copies a `.env` example, prefixes everything with `VITE_` or `NEXT_PUBLIC_`, and pushes to production. Bundle analysis or the browser DevTools network tab reveals the secrets in the JS bundle.

**Detection:** Run `strings dist/assets/*.js | grep -i "secret\|password\|key\|token"` in CI as a security gate.

---

## 8. NODE_ENV — The Universal Flag

`NODE_ENV` is the most important environment variable in the JavaScript ecosystem. Set by bundlers automatically based on the build command.

| Command | NODE_ENV |
|---|---|
| `vite` / `webpack serve` | `development` |
| `vite build` / `webpack --mode production` | `production` |
| `jest` | `test` |

**Impact on builds:**

```javascript
// React itself uses NODE_ENV to remove development warnings:
if (process.env.NODE_ENV !== 'production') {
  // entire React DevTools and warnings tree — dead code eliminated in production
}

// Libraries like redux-toolkit check NODE_ENV:
const isDev = process.env.NODE_ENV !== 'production';
if (isDev) {
  // extra checks, stack traces, etc.
}
```

**Never manually set NODE_ENV=development in production deployments.** This disables React's production optimizations and ships debug code.

---

## 9. Custom Modes (Vite)

Beyond `development` and `production`, Vite supports custom modes:

```bash
# Build for staging
vite build --mode staging

# .env.staging
VITE_API_URL=https://api.staging.example.com
VITE_FEATURE_BETA=true
```

```typescript
const mode = import.meta.env.MODE;  // 'staging'
const isProd = import.meta.env.PROD; // true (only false in dev mode)
```

---

## 10. Feature Flags — Build-Time vs Runtime

### Build-time flags (via env vars) — zero runtime cost

```typescript
// Dead code eliminated at build time
const ENABLE_NEW_CHECKOUT = import.meta.env.VITE_ENABLE_NEW_CHECKOUT === 'true';

function CheckoutPage() {
  if (ENABLE_NEW_CHECKOUT) {
    return <NewCheckout />;  // in prod bundle if flag=true
  }
  return <LegacyCheckout />; // dead code eliminated if flag=true
}
```

Pros: zero runtime overhead, no feature flag service needed.
Cons: requires a new build to change the flag — cannot toggle without redeploying.

### Runtime flags (via API) — flexible but with cost

```typescript
// Fetch from a feature flag service (LaunchDarkly, Unleash, etc.)
function useFeatureFlag(flagName: string): boolean {
  const [enabled, setEnabled] = useState(false);
  useEffect(() => {
    flagService.getFlag(flagName).then(setEnabled);
  }, [flagName]);
  return enabled;
}
```

Pros: toggle without redeployment, gradual rollout, A/B testing.
Cons: network request, loading state, service dependency.

### Hybrid pattern (Next.js middleware + edge config)

```typescript
// middleware.ts — evaluated at the edge, no client JS needed
import { NextRequest, NextResponse } from 'next/server';

export function middleware(req: NextRequest) {
  const isNewCheckout = process.env.FEATURE_NEW_CHECKOUT === 'true';
  if (isNewCheckout && req.nextUrl.pathname === '/checkout') {
    return NextResponse.rewrite(new URL('/checkout-v2', req.url));
  }
}
```

---

## 11. Validation — Never Trust Unset Env Vars

```typescript
// lib/env.ts — validate required env vars at startup
import { z } from 'zod';

const envSchema = z.object({
  VITE_API_URL: z.string().url(),
  VITE_APP_NAME: z.string().min(1),
  // Optional with default:
  VITE_ANALYTICS_ENABLED: z.string().default('false'),
});

export const env = envSchema.parse(import.meta.env);
// If any required variable is missing → throws at startup (not at runtime in a user flow)

// Usage
import { env } from '@/lib/env';
const apiUrl = env.VITE_API_URL;  // fully typed, validated
```

**For Next.js (server-side):**

```typescript
// lib/server-env.ts (server-only file)
import { z } from 'zod';

const serverEnvSchema = z.object({
  DATABASE_URL: z.string(),
  JWT_SECRET: z.string().min(32),
  STRIPE_SECRET_KEY: z.string().startsWith('sk_'),
});

export const serverEnv = serverEnvSchema.parse(process.env);
```

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Putting secrets in `VITE_*` or `NEXT_PUBLIC_*` | Secret ships in browser bundle | Move to server-only env vars |
| Not gitignoring `.env.local` | Secrets committed to repo | Add `.env.local`, `.env.*.local` to `.gitignore` |
| Committing `.env` with real values | Environment-specific values break other developers | Only commit `.env.example` with placeholder values |
| `process.env.VAR` in Next.js Client Component | `undefined` at runtime — variable not exposed | Use `NEXT_PUBLIC_` prefix |
| Not validating env vars | App runs with silent `undefined` for hours | Validate with Zod at startup |
| `NODE_ENV=development` in production Docker | Disables React/library production optimizations | Never set NODE_ENV manually in production — let the build tool set it |

---

## 13. Interview Insight

Strong answer:

> Environment variables in build tools work through static replacement — the bundler replaces `process.env.VAR` with the literal string value at build time. The critical security rule is the exposure prefix: only `VITE_*` variables reach Vite's client bundle, and only `NEXT_PUBLIC_*` variables reach Next.js's client bundle. Everything else is server-side only. The most dangerous mistake is accidentally prefixing database credentials or API secrets with these prefixes, which ships them in the JavaScript bundle where anyone can extract them from DevTools.

Follow-up trap:

> If build tools replace `process.env.NODE_ENV` statically, what happens if you read `process.env.NODE_ENV` dynamically?

Good answer:

> Vite replaces only static references. A dynamic access like `process.env['NODE_' + 'ENV']` would fail — Vite can't statically replace that pattern. You'd get a runtime error unless you use `import.meta.env.MODE` instead, which Vite handles at a higher level.

---

## 14. Revision Notes

- One-line summary: Env vars are statically replaced at build time; client-exposed vars are gated by prefix (`VITE_*`, `NEXT_PUBLIC_*`).
- Three keywords: prefix, static-replacement, server-only.
- One interview trap: Env vars are not runtime lookups — they are compile-time text substitution.
- Memory trick: Public prefix = public information — never put secrets there.
