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

## 12. Interview Insight

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
