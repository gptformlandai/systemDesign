# React + Next.js Developer Environment - Gold Sheet

> Track Module - Group 0: Setup And Tooling
> Level: beginner -> production-ready | First-mile setup, local workflow, scripts, TypeScript, linting, debugging, and project conventions

---

## 1. Intuition

A React/Next.js app is not only components. It is a development system:

```text
Node runtime + package manager + framework + TypeScript + linting + local server
+ environment variables + folder conventions + debugging tools
```

Beginners often lose time because the first-mile setup is fuzzy. Senior engineers make setup boring, repeatable, and easy to diagnose.

---

## 2. Definition

- Definition: A developer environment is the local and CI setup that lets engineers create, run, debug, test, and build the app consistently.
- Category: Tooling / project foundation.
- Core idea: Same commands, same Node version, same project shape, same quality gates on every machine.

---

## 3. Why It Exists

Without a standard setup:
- one developer uses a different Node version and hits build-only errors;
- another forgets environment variables and debugs phantom bugs;
- lint/type/test checks drift between local and CI;
- onboarding becomes folklore instead of a checklist.

Next.js gives strong defaults, but teams still need conventions.

---

## 4. Current Baseline

Modern Next.js App Router projects should assume:
- Node.js `20.9+` for current Next.js 16 projects.
- TypeScript by default.
- App Router by default.
- Turbopack as the default local bundler.
- ESLint or Biome as an explicit quality gate.
- Import alias such as `@/*`.
- Environment variables split into server-only and explicitly public values.

Recommended create command:

```bash
pnpm create next-app@latest my-app
```

Common prompt choices:

```text
TypeScript: Yes
Linter: ESLint or Biome
React Compiler: Yes for new apps if team is ready to adopt it
Tailwind CSS: Yes if the team wants utility-first styling
src directory: Yes for larger apps
App Router: Yes
Import alias: @/*
```

---

## 5. Project Structure

A production-friendly structure:

```text
my-app/
  app/
    layout.tsx
    page.tsx
    error.tsx
    not-found.tsx
    loading.tsx
    api/
  components/
    ui/
    features/
  features/
    checkout/
      components/
      actions.ts
      queries.ts
      schema.ts
      types.ts
  lib/
    auth.ts
    env.ts
    http.ts
    logger.ts
  data/
    users.ts
    products.ts
  public/
  tests/
  next.config.ts
  package.json
  tsconfig.json
```

Rule of thumb:
- `app/` owns routes, layouts, metadata, loading/error boundaries, and route handlers.
- `components/ui/` owns reusable design-system primitives.
- `features/` owns product behavior by domain.
- `lib/` owns shared infrastructure helpers.
- `data/` owns server-only data access and DTO shaping.

---

## 6. Package Manager Decision

| Tool | Strong Fit | Watch Out |
|---|---|---|
| `pnpm` | Monorepos, fast installs, strict dependency graph | Some older packages assume hoisting |
| `npm` | Universal default | Slower in large workspaces |
| `yarn` | Existing Yarn workspaces | Version differences matter |
| `bun` | Fast local workflow | Ecosystem parity should be verified for enterprise apps |

Interview answer:
Use the package manager already standardized by the organization. For greenfield monorepos, `pnpm` is usually a strong default because it is fast and strict.

---

## 7. Required Scripts

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "eslint",
    "lint:fix": "eslint --fix",
    "typecheck": "tsc --noEmit",
    "test": "vitest run",
    "test:watch": "vitest",
    "e2e": "playwright test",
    "format": "prettier --check .",
    "format:fix": "prettier --write ."
  }
}
```

Minimum CI gate:

```bash
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

For user-facing flows, add:

```bash
pnpm e2e
```

---

## 8. Environment Variables

Mental model:

```text
No prefix       -> server-only by default
NEXT_PUBLIC_    -> exposed to browser bundle
```

Example:

```env
DATABASE_URL=postgres://internal
AUTH_SECRET=server-secret
NEXT_PUBLIC_ANALYTICS_ID=public-id
```

Do not put secrets behind `NEXT_PUBLIC_`.

Create a typed environment module:

```ts
// lib/env.ts
import 'server-only';
import { z } from 'zod';

const schema = z.object({
  DATABASE_URL: z.string().url(),
  AUTH_SECRET: z.string().min(32),
});

export const env = schema.parse({
  DATABASE_URL: process.env.DATABASE_URL,
  AUTH_SECRET: process.env.AUTH_SECRET,
});
```

For public values:

```ts
// lib/public-env.ts
import { z } from 'zod';

const schema = z.object({
  NEXT_PUBLIC_ANALYTICS_ID: z.string().optional(),
});

export const publicEnv = schema.parse({
  NEXT_PUBLIC_ANALYTICS_ID: process.env.NEXT_PUBLIC_ANALYTICS_ID,
});
```

---

## 9. TypeScript Settings

Production default:

```json
{
  "compilerOptions": {
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitOverride": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./*"]
    }
  }
}
```

Practical rule:
If TypeScript is quiet, the type system is probably too loose.

---

## 10. Debugging Setup

Useful local checks:

```bash
pnpm dev
pnpm build
pnpm typecheck
pnpm lint
```

Debugging categories:

| Symptom | First Check |
|---|---|
| Works in dev, fails in build | Server/client boundary, env vars, dynamic imports |
| Hydration warning | Browser-only value in initial render |
| Slow route | Waterfall, uncached fetch, large client bundle |
| Secret in browser | `NEXT_PUBLIC_`, serialized props, Client Component import |
| Route not found | App Router file convention |
| Type works locally, fails in CI | Node/package manager lockfile mismatch |

---

## 11. DevTools You Should Know

Browser:
- Elements panel for DOM/CSS.
- Network panel for request waterfalls, cache headers, payload size.
- Performance panel for long tasks and main-thread blocking.
- Lighthouse for coarse performance and accessibility.

React:
- React DevTools Components tab.
- Profiler for re-render cost.
- Strict Mode warnings.

Next.js:
- Build output.
- Route-level rendering logs.
- Bundle analyzer.
- `instrumentation.ts` for server telemetry.

---

## 12. Beginner Workflow

Daily loop:

```text
1. Start dev server.
2. Change one component or route.
3. Check browser behavior.
4. Check terminal warnings.
5. Run typecheck/lint before committing.
6. Add a test for important behavior.
```

For any bug:

```text
Can I reproduce it?
Is it client-only, server-only, or boundary-related?
Is it data, render, cache, route, or CSS?
Can I write a small failing test or fixture?
```

---

## 13. Senior Workflow

Before merging:
- The route has a clear rendering strategy.
- Data access has explicit cache policy.
- Secrets stay on the server.
- User-facing errors have fallbacks.
- Important interactions have tests.
- Bundle impact is understood.
- Observability exists for the failure path.

Pull request checklist:

```text
[ ] No secret leaves the server.
[ ] Loading, empty, error, and success states exist.
[ ] Accessibility path works from keyboard.
[ ] Route can be refreshed directly.
[ ] Build passes locally.
[ ] New server action validates and authorizes.
[ ] Caching and revalidation are intentional.
```

---

## 14. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Starting without TypeScript | Bugs move to runtime | Use TS from day one |
| Putting all code in `app/` | Routes become tangled with business logic | Move domain logic into `features/` or `data/` |
| Reading secrets in Client Components | Secret leakage risk | Use server modules and DTOs |
| Trusting dev mode only | Production build behaves differently | Run `next build` often |
| Skipping lockfiles | Non-reproducible installs | Commit lockfile |
| No env validation | Late runtime crashes | Validate env at startup |

---

## 15. Practical Question

> You are onboarding a team to a new Next.js app. What setup choices would you standardize before feature work starts?

---

## 16. Strong Answer

```text
I would standardize Node version, package manager, TypeScript strictness, App
Router, lint/type/test/build scripts, import aliases, env validation, and a
feature-based folder structure. I would also define server/client boundary rules,
cache policy conventions, and a PR checklist for loading/error/accessibility
states. This prevents setup drift and lets engineers focus on product behavior
instead of local environment issues.
```

---

## 17. Revision Notes

- One-line summary: A strong React/Next.js app starts with repeatable tooling, not only components.
- Three keywords: Node, scripts, boundaries.
- One interview trap: Do not expose secrets with `NEXT_PUBLIC_`.
- One memory trick: If CI cannot run it, the team does not own it yet.

