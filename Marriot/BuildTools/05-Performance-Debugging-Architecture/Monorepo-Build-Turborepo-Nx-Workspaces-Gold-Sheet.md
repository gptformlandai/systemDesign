# Monorepo Build: Turborepo, Nx, and Workspace Protocols — Gold Sheet

> Topic: monorepo build orchestration, task caching, affected builds, workspace package management

---

## 1. Intuition

A monorepo holds multiple packages (apps and libraries) in one repository. The challenge is: when you change one package, you should not have to rebuild all 50 others. Turborepo and Nx solve this by building a dependency graph of tasks and caching results — only rebuilding packages that actually changed or depend on what changed.

Beginner version:

> In a monorepo, build tools figure out what actually changed and only run tasks for those packages — not everything.

---

## 2. Definition

- Definition: Monorepo build orchestration tools (Turborepo, Nx) execute tasks across multiple packages in correct dependency order, caching results to skip redundant work.
- Category: Build pipeline orchestration.
- Core idea: Task graph + caching = fast incremental builds.

---

## 3. Monorepo Structure

```
my-company/
  apps/
    web/           ← Next.js marketing site
    admin/         ← Next.js admin dashboard
    api/           ← Node.js API server
  packages/
    ui/            ← Shared React component library
    config/        ← Shared ESLint, TypeScript configs
    db/            ← Prisma schema + client
    utils/         ← Shared TypeScript utilities
  package.json     ← workspace root
  turbo.json       ← Turborepo config
  pnpm-workspace.yaml
```

---

## 4. Workspace Protocols — pnpm, Yarn Berry, npm

Workspaces allow packages in the monorepo to reference each other without publishing to npm.

```yaml
# pnpm-workspace.yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

```json
// apps/web/package.json
{
  "dependencies": {
    "@company/ui": "workspace:*",     // pnpm workspace protocol
    "@company/utils": "workspace:^"   // allow minor version bumps
  }
}
```

```json
// packages/ui/package.json
{
  "name": "@company/ui",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.js"
    }
  }
}
```

**pnpm workspace benefits:** Strict symlink-based dependency isolation, no phantom dependencies (can't accidentally use an unlisted package), faster installs than npm workspaces.

---

## 5. Turborepo — Task Graph and Caching

```json
// turbo.json
{
  "$schema": "https://turbo.build/schema.json",
  "globalDependencies": [".env"],
  "tasks": {
    "build": {
      "dependsOn": ["^build"],          // run this package's deps' build FIRST
      "inputs": ["src/**", "package.json", "tsconfig.json"],
      "outputs": [".next/**", "dist/**"]
    },
    "test": {
      "dependsOn": ["build"],           // test runs after build
      "inputs": ["src/**", "**/*.test.ts"],
      "outputs": ["coverage/**"]
    },
    "lint": {
      "inputs": ["src/**", ".eslintrc.*"],
      "outputs": []
    },
    "dev": {
      "cache": false,                   // never cache dev server
      "persistent": true               // long-running process
    }
  }
}
```

**`dependsOn: ["^build"]`** — the `^` means "run the `build` task for all packages that this package depends on first". Turborepo figures out the order automatically.

### Running tasks

```bash
# Run build for all packages in correct dependency order
turbo build

# Run only affected packages (changed files)
turbo build --filter='...[HEAD~1]'

# Run for a specific package and its dependencies
turbo build --filter=web...

# Run for a specific package only (no dependencies)
turbo build --filter=@company/ui
```

---

## 6. Turborepo Caching — How It Works

```
Task inputs → hash → check cache → HIT: restore outputs / MISS: run task → store in cache
```

Turborepo hashes:
- The `inputs` files listed in `turbo.json`
- The task's dependencies' output hashes
- Environment variables listed in `globalEnv`

If the hash matches a previous run's hash, Turborepo replays the cached output without running the task.

```bash
# First run — cache miss
turbo build
  # builds @company/ui in 45s
  # builds web in 90s
  # Total: 2m15s
  # Cache saved

# Second run (nothing changed) — cache hit
turbo build
  # >>> FULL TURBO (all cached)
  # Total: 1.2s
```

### Remote caching — share cache across CI machines

```bash
# Link to Vercel remote cache (free with Vercel account)
npx turbo login
npx turbo link

# Now CI and local machines share the same cache:
# Developer builds → caches → CI gets cache hit
# CI builds → caches → next CI run is instant
```

---

## 7. Turborepo with Next.js — Common Patterns

```json
// turbo.json optimized for Next.js
{
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "inputs": [
        "src/**",
        "app/**",
        "public/**",
        "next.config.*",
        "tsconfig.json",
        "tailwind.config.*",
        "package.json"
      ],
      "outputs": [".next/**", "!.next/cache/**"]  // exclude Next.js internal cache
    },
    "type-check": {
      "dependsOn": ["^build"],
      "outputs": []
    }
  }
}
```

---

## 8. Nx — Build Orchestration with Deeper Integration

Nx is an alternative to Turborepo with more opinions and more features.

```bash
npx create-nx-workspace@latest my-org
cd my-org

# Generate apps and libraries
npx nx generate @nx/react:app my-app
npx nx generate @nx/react:lib my-lib

# Run affected tasks (only packages changed since main branch)
npx nx affected --target=build
npx nx affected --target=test

# Graph the task dependencies
npx nx graph
```

**Nx-specific features vs Turborepo:**

| Feature | Turborepo | Nx |
|---|---|---|
| Task graph + caching | Yes | Yes |
| Remote caching | Vercel (free) | Nx Cloud (paid tier) |
| Code generation | No | Yes (`nx generate`) |
| Plugin ecosystem | Minimal | Large (React, Next.js, Node, .NET) |
| Affected calculations | File-level | Semantic (understands what changed) |
| Project graph visualization | No | Yes (`nx graph`) |
| Learning curve | Low | Medium |

**When to choose Turborepo:** Simple pipelines, team already on Vercel, minimal config philosophy.
**When to choose Nx:** Large enterprise teams, want generators + plugins + visualization, need very fine-grained affected calculations.

---

## 9. Shared Config Packages Pattern

```
packages/
  config/
    eslint/
      package.json    { "name": "@company/eslint-config" }
      index.js        ← shared ESLint config
    typescript/
      package.json    { "name": "@company/tsconfig" }
      base.json       ← base tsconfig
      nextjs.json     ← Next.js-specific tsconfig
    tailwind/
      package.json    { "name": "@company/tailwind-config" }
      index.js        ← shared Tailwind config
```

```json
// apps/web/tsconfig.json
{
  "extends": "@company/tsconfig/nextjs.json",
  "compilerOptions": {
    "paths": { "@/*": ["./src/*"] }
  }
}
```

```javascript
// apps/web/.eslintrc.js
module.exports = {
  extends: ['@company/eslint-config'],
  rules: {
    // app-specific overrides
  },
};
```

---

## 10. Package Manager Comparison in Monorepos

| Package Manager | Monorepo Support | Speed | Phantom Deps |
|---|---|---|---|
| npm workspaces | Yes | Slowest | Allowed (hoisting) |
| Yarn v1 workspaces | Yes | Medium | Allowed (hoisting) |
| Yarn Berry (v2+) | Yes | Fast | Strict (PnP) |
| pnpm workspaces | Yes | Fastest | Not allowed (symlinks) |

**pnpm is the recommended choice** for monorepos in 2025:
- Fastest installs (content-addressable store)
- Strictest dependency isolation (symlink per package, no phantom dependencies)
- `workspace:` protocol for cross-package references
- Works seamlessly with Turborepo

---

## 11. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Not listing all inputs in `turbo.json` | Cache hit even when a file changed → stale output | List all inputs: src, config, env files |
| Caching the dev server task | `turbo dev` replays old terminal output | Set `"cache": false` for dev tasks |
| Missing `^build` in `dependsOn` | Build runs before dependencies are built → type errors | Always use `"dependsOn": ["^build"]` for packages with cross-package imports |
| Shared configs not published with `exports` | Apps can't resolve the config package | Add `exports` and `main` to shared config `package.json` |
| Circular dependencies | Turborepo / pnpm error | Restructure — UI can't import from apps |
| Not using `workspace:` protocol | Cross-package changes require npm publish | Use `workspace:*` for local references |

---

## 12. Interview Insight

Strong answer:

> Turborepo and Nx solve the same core problem: in a monorepo with dozens of packages, you should only rebuild what changed. Both work by building a task dependency graph from your package.json dependencies, then caching task outputs keyed by a hash of the task's inputs. On a cache hit, they replay the previous output without running the task — making CI effectively instant for unchanged packages. The key configuration is `dependsOn: ["^build"]`, which tells Turborepo to run all transitive package builds before building the current package. Remote caching extends this to CI machines, so one developer's build benefits all subsequent CI runs.

Follow-up trap:

> What's the difference between `dependsOn: ["build"]` and `dependsOn: ["^build"]`?

Good answer:

> Without the caret, `"build"` means "this package's own build task must run first" — which is circular and not what you want. With the caret, `"^build"` means "run the `build` task for all packages that this package depends on in `package.json`". That's the correct dependency order: build libraries before building the apps that use them.

---

## 13. Revision Notes

- One-line summary: Monorepo orchestration tools run tasks in dependency order and cache results to skip unchanged packages.
- Three keywords: task-graph, cache, affected.
- One interview trap: `dependsOn: ["build"]` vs `["^build"]` — the caret means "upstream deps first."
- Memory trick: The caret (`^`) points up to dependencies — run them first.
