# Node.js Package Managers: npm, Yarn, pnpm Gold Sheet

> Topic: `package.json`, dependencies, lockfiles, scripts, workspaces, hoisting, and CI optimization.

---

## 1. Intuition

Node package managers do three major jobs: resolve dependency versions, place packages where Node/tooling can find them, and run project scripts.

Beginner version:

> `package.json` says what the project wants; the lockfile says exactly what it got.

---

## 2. Definition

- Definition: A Node.js package manager installs dependencies, resolves versions, manages lockfiles, and runs scripts for Node projects.
- Category: JavaScript/TypeScript backend build tooling.
- Core idea: dependency graph + scripts + reproducible install.

---

## 3. `package.json`

```json
{
  "name": "orders-api",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "tsx watch src/server.ts",
    "build": "tsc -p tsconfig.json",
    "start": "node dist/server.js",
    "test": "vitest run"
  },
  "dependencies": {
    "fastify": "^5.0.0"
  },
  "devDependencies": {
    "typescript": "^5.8.0",
    "vitest": "^3.0.0",
    "tsx": "^4.0.0"
  }
}
```

Fields:

- `dependencies`: needed at runtime.
- `devDependencies`: needed to build/test/develop.
- `scripts`: project commands.
- `type`: module mode, such as ESM.
- `engines`: expected Node/npm versions.
- `workspaces`: monorepo packages.

---

## 4. npm

Strength:

- Default package manager with Node.
- Works everywhere.
- Strong CI support with `npm ci`.

Common commands:

```bash
npm install
npm ci
npm run build
npm test
npm audit
npm explain express
```

`npm ci`:

- intended for CI.
- installs from lockfile.
- fails if lockfile and package manifest disagree.
- removes existing `node_modules`.

---

## 5. Yarn

Strength:

- Workspace workflows.
- Advanced features depending on version.
- Popular in many monorepos.

Common ideas:

- lockfile-driven install.
- workspace linking.
- stricter dependency behavior in some modes.

Use when:

- your organization standardizes on Yarn.
- workspace features and existing tooling fit well.

---

## 6. pnpm

Strength:

- disk-efficient package store.
- strict dependency linking.
- strong workspace support.

Mental model:

```txt
global content-addressed store
   |
   v
project node_modules uses links
   |
   v
dependencies are available only when declared
```

Why teams like it:

- faster installs in monorepos.
- less disk duplication.
- catches undeclared dependency usage better than loose hoisting setups.

Common commands:

```bash
pnpm install --frozen-lockfile
pnpm build
pnpm test
pnpm -r build
```

---

## 7. Lockfiles

| Tool | Lockfile |
|---|---|
| npm | `package-lock.json` |
| Yarn | `yarn.lock` |
| pnpm | `pnpm-lock.yaml` |

Application rule:

```txt
Commit lockfiles for deployable applications.
```

Library rule:

```txt
Lockfiles can help development, but published libraries should declare compatible ranges carefully.
```

---

## 8. Hoisting Issues

Hoisting means dependencies may be placed higher in the `node_modules` tree.

Problem:

```txt
package-a uses lodash without declaring it
root node_modules happens to contain lodash
local works
CI/prod fails after layout changes
```

Better:

```txt
every package declares what it imports
```

pnpm's stricter layout often reveals these mistakes earlier.

---

## 9. Workspaces

Monorepo:

```txt
repo/
  package.json
  packages/
    api/
    shared/
    worker/
```

Root `package.json`:

```json
{
  "private": true,
  "workspaces": [
    "packages/*"
  ]
}
```

Benefits:

- local package linking.
- single install command.
- shared scripts.
- coordinated dependency updates.

Risks:

- accidental cross-package imports.
- version drift.
- slow CI without task caching.
- hoisting differences.

---

## 10. CI Optimization

Good CI pattern:

```txt
pin Node version
restore package-manager cache
frozen install
build
test
coverage
package/containerize
```

Examples:

```bash
npm ci
npm run build
npm test
```

```bash
pnpm install --frozen-lockfile
pnpm -r build
pnpm -r test
```

Cache:

- package manager cache.
- build tool cache.
- TypeScript incremental cache if safe.
- test cache if deterministic.

---

## 11. Common Mistakes

### Mistake: Using `npm install` in CI without lockfile discipline

- Why wrong: it can update lockfiles or resolve differently.
- Better approach: use `npm ci`.

### Mistake: Runtime dependency in `devDependencies`

- Why wrong: production install may omit it.
- Better approach: dependencies needed by running app go in `dependencies`.

### Mistake: Relying on hoisted undeclared dependencies

- Why wrong: layout changes break runtime.
- Better approach: declare every imported package.

### Mistake: Mixing package managers

- Why wrong: multiple lockfiles create ambiguity.
- Better approach: choose one package manager per repo and enforce it.

---

## 12. Yarn Berry (Yarn 2+) — PnP Mode

Yarn Berry's Plug'n'Play eliminates `node_modules` entirely:

```bash
# Enable Yarn Berry in an existing project
corepack enable
yarn set version stable

# .yarnrc.yml
nodeLinker: pnp   # Plug'n'Play — no node_modules
```

### How PnP Works

```txt
Traditional node_modules resolution:
  import("lodash")
    -> Node.js walks up directories looking for node_modules/lodash
    -> Finds it (or doesn't) based on filesystem layout
    -> Hoisting determines which version is found

Yarn Berry PnP:
  import("lodash")
    -> Node.js patched at startup with Yarn's require hook
    -> .pnp.cjs file consulted: { "lodash@4.17.21": ".yarn/cache/lodash-4.17.21.zip" }
    -> Resolved directly — no filesystem walk
    -> Package is read from the zip archive
```

- Dependencies stored in `.yarn/cache/` as zip archives
- A `.pnp.cjs` file generated — maps package names to zip locations
- Node.js patched at startup to resolve from zip files instead of `node_modules`

### Benefits vs Tradeoffs

```
Benefits:
  - Faster installs: no I/O write to node_modules (thousands of small files)
  - Strict resolution: phantom dependencies are impossible (undeclared = unresolved)
  - Smaller CI cache: cache one zip per package, not thousands of files
  - Zero-install: commit .yarn/cache/ to git — no npm install needed in CI

Tradeoffs:
  - Some packages incompatible with PnP (C++ native addons, scripts that walk node_modules)
  - IDE setup requires Yarn SDKs: yarn dlx @yarnpkg/sdks vscode
  - TypeScript, ESLint, Prettier need SDK wrappers to find their own modules
  - Harder to debug resolution issues (less familiar error messages)
  - Not all frameworks officially support it (CRA had issues; Vite works fine)
```

### Yarn nodeLinker Options (not just PnP)

```yaml
# .yarnrc.yml

# Option 1: Plug'n'Play (default in Berry)
nodeLinker: pnp

# Option 2: node-modules (classic behavior, with Yarn Berry tooling improvements)
nodeLinker: node-modules

# Option 3: pnpm (uses pnpm-style symlinks without needing pnpm)
nodeLinker: pnpm
```

For teams not ready for PnP, `nodeLinker: node-modules` gets Yarn Berry's speed improvements (parallel installs, deduplication) while keeping familiar resolution behavior.

### IDE Setup (required for PnP)

```bash
# Generate IDE integration files (do this after enabling PnP)
yarn dlx @yarnpkg/sdks vscode   # generates .yarn/sdks/ and .vscode/settings.json

# Required .vscode/settings.json entries generated:
# "typescript.tsdk": ".yarn/sdks/typescript/lib"
# "eslint.nodePath": ".yarn/sdks"

# Without this, VS Code uses its own TypeScript and cannot find packages in zip files
```

### Migrating from npm/Yarn v1 to Yarn Berry

```bash
# Step 1: Ensure corepack is enabled
corepack enable

# Step 2: Set Yarn Berry version in the project
yarn set version stable   # or: yarn set version 4.x

# Step 3: Start with node-modules linker for safety
# .yarnrc.yml
# nodeLinker: node-modules

# Step 4: Run install and verify all packages work
yarn install

# Step 5: (optional, later) migrate to PnP if desired
# Change nodeLinker to pnp
# Fix incompatible packages with patches or fallbacks
# Install IDE SDKs
```

---

## 13. Corepack — Pinning Package Manager Version

Corepack (bundled with Node.js 16+) manages package manager versions per project:

```json
// package.json
{
  "packageManager": "pnpm@9.15.4+sha512.abc123..."
}
```

```bash
# Enable Corepack globally
corepack enable

# Run pnpm — Corepack auto-installs the pinned version if not present
pnpm install
```

**Why this matters:** Without `packageManager` pinning, different developers/CI runners may use different pnpm/yarn versions with different behavior. The `packageManager` field is the Node.js-native way to enforce a specific version.

```bash
# In CI, always enable Corepack first:
- run: corepack enable
- run: pnpm install --frozen-lockfile
```

---

## 14. pnpm Workspace Catalogs

pnpm workspaces + catalogs pin shared dependencies across all packages in a monorepo:

```yaml
# pnpm-workspace.yaml
packages:
  - "packages/*"
  - "apps/*"

catalog:
  react: ^18.3.0
  typescript: ^5.7.2
  "@types/node": ^22.0.0
```

```json
// packages/ui/package.json
{
  "dependencies": {
    "react": "catalog:"   // resolves to react@^18.3.0 from catalog
  }
}
```

**Benefit:** When you want to upgrade React across 20 packages, change one line in `pnpm-workspace.yaml` instead of 20 `package.json` files.

---

## 15. pnpm Workspace Protocol and Strict Isolation

### `workspace:*` Protocol

The `workspace:*` protocol tells pnpm to link a local workspace package instead of downloading from the registry. This is how packages in a monorepo reference each other:

```yaml
# pnpm-workspace.yaml
packages:
  - "packages/*"
  - "apps/*"
```

```json
// apps/payments-ui/package.json
{
  "name": "@company/payments-ui",
  "dependencies": {
    "@company/ui-components": "workspace:*",   // link to packages/ui-components locally
    "@company/auth-lib":       "workspace:^",   // link local, publish with ^ range
    "@company/utils":          "workspace:~"    // link local, publish with ~ range
  }
}
```

```txt
workspace:*  — when publishing to npm, replaced with exact current version (e.g., "1.2.3")
workspace:^  — when publishing, replaced with ^1.2.3 (allows minor bumps)
workspace:~  — when publishing, replaced with ~1.2.3 (allows patch bumps)

During development (inside the monorepo), ALL workspace: variants link locally —
they all resolve to the local package directory via symlink.
```

### How pnpm Strict Isolation Works

pnpm's dependency layout is fundamentally different from npm's hoisting:

```txt
npm hoisting (loose):
  node_modules/
    react/           <- shared at root (hoisted)
    lodash/          <- hoisted even if not declared by root
    payments-ui/
      node_modules/  <- only things that can't be hoisted
    ui-components/

pnpm strict symlinks:
  node_modules/
    .pnpm/
      react@18.3.0/node_modules/react/    <- actual files here
      lodash@4.17.21/node_modules/lodash/
    react -> .pnpm/react@18.3.0/.../react  <- symlink
    @company/
      ui-components -> ../../packages/ui-components  <- workspace symlink

  packages/payments-ui/node_modules/
    react -> ../../../node_modules/.pnpm/react@18.3.0/.../react  <- can only access declared deps
    lodash   <- ERROR if not in payments-ui's own package.json
```

**Key consequence:** A package can only import modules it has explicitly declared in its own `package.json`. Using a hoisted undeclared transitive dependency (phantom dependency) causes an `ERR_MODULE_NOT_FOUND` error immediately — **not** silently at runtime in production after a dependency tree change.

### Common pnpm Workspace Commands

```bash
# Install all packages across all workspaces
pnpm install

# Add a dep to a specific workspace
pnpm add react --filter @company/payments-ui

# Add a workspace package as a dep to another workspace
pnpm add @company/ui-components --filter @company/payments-ui --workspace

# Run a script in a specific workspace
pnpm run build --filter @company/payments-ui

# Run in all workspaces (parallel)
pnpm run build --recursive          # or: pnpm -r run build

# Run only in changed workspaces (since last git commit)
pnpm run test --filter "...[HEAD]"

# Execute in workspaces in dependency order (respects package deps)
pnpm run build --recursive --sort

# CI: frozen install
pnpm install --frozen-lockfile
```

### Phantom Dependency Example — Why pnpm Catches Production Bugs Earlier

```json
// packages/service-a/package.json — only declares express
{
  "dependencies": {
    "express": "^4.18.0"
  }
}
```

```js
// packages/service-a/src/index.js
const lodash = require('lodash');   // NOT declared in package.json!
```

```txt
npm behavior:
  lodash is hoisted to root node_modules because service-b uses it
  → service-a's require('lodash') WORKS locally
  → After service-b stops using lodash, hoisting removes it
  → service-a breaks in production (or CI after clean install)

pnpm behavior:
  service-a's node_modules only contains symlinks to DECLARED packages
  → require('lodash') in service-a throws immediately in development
  → Bug caught before it reaches CI or production
```

---

## 17. npm Workspaces

npm native monorepo support (since npm v7):

```json
// root package.json
{
  "name": "my-monorepo",
  "workspaces": [
    "packages/*",
    "apps/*"
  ]
}
```

```bash
# Install all packages
npm install

# Run command in specific workspace
npm run build -w @company/payments-ui

# Run in all workspaces
npm run test --workspaces

# Filter workspaces
npm run build --workspace packages/shared-api --workspace packages/auth-lib
```

**Hoisting:** npm hoists shared dependencies to root `node_modules`. This means `packages/ui/node_modules/` is nearly empty — React is in `node_modules/` at root. Risk: phantom dependencies can still be imported accidentally.

---

## 18. Interview Insight

Strong answer:

> Node package managers resolve dependency graphs, create lockfiles, manage `node_modules`, and run scripts. npm is the default, Yarn is common in workspace-heavy environments, and pnpm is attractive for monorepos because of disk efficiency and stricter dependency linking. In CI, I use frozen installs and pin Node/package-manager versions.

Follow-up trap:

> Why does the app work locally but fail after `npm ci --omit=dev`?

Good answer:

> A package needed at runtime was probably placed in `devDependencies`, or the build output depends on a tool/package that is not included in the runtime image. I would inspect imports, production install flags, and the Docker multi-stage copy.

---

## 19. Revision Notes

- One-line summary: Node package managers resolve, lock, install, and run scripts.
- Three keywords: manifest, lockfile, scripts.
- One interview trap: `dependencies` vs `devDependencies` affects production runtime.
- Memory trick: Manifest is the wish list; lockfile is the receipt.

Strong answer:

> Node package managers resolve dependency graphs, create lockfiles, manage `node_modules`, and run scripts. npm is the default, Yarn is common in workspace-heavy environments, and pnpm is attractive for monorepos because of disk efficiency and stricter dependency linking. In CI, I use frozen installs and pin Node/package-manager versions.

Follow-up trap:

> Why does the app work locally but fail after `npm ci --omit=dev`?

Good answer:

> A package needed at runtime was probably placed in `devDependencies`, or the build output depends on a tool/package that is not included in the runtime image. I would inspect imports, production install flags, and the Docker multi-stage copy.

---

## 13. Revision Notes

- One-line summary: Node package managers resolve, lock, install, and run scripts.
- Three keywords: manifest, lockfile, scripts.
- One interview trap: `dependencies` vs `devDependencies` affects production runtime.
- Memory trick: Manifest is the wish list; lockfile is the receipt.
