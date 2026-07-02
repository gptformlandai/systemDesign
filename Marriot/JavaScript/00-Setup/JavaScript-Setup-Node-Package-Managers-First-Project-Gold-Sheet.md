# JavaScript Setup, Node, Package Managers, And First Project Gold Sheet

> Track: JavaScript Interview Track - Setup Layer  
> Goal: take a beginner from zero setup to a clean, reproducible JavaScript project.

---

## 1. Intuition

JavaScript setup is the workshop. The language is the tool, Node.js is the local runtime,
the package manager brings dependencies, the lockfile freezes the dependency graph, and
scripts make development repeatable.

Beginner version:

- Install a supported Node.js version.
- Choose one package manager.
- Create `package.json`.
- Add lint, format, test, and start scripts.
- Commit the lockfile.
- Run the same commands locally and in CI.

---

## 2. Definition

- Definition: JavaScript project setup is the process of pinning the runtime, dependency
  manager, module format, scripts, quality tools, and test workflow for a project.
- Category: developer environment, build tooling, runtime setup.
- Core idea: a JavaScript project is reproducible only when Node version, package manager,
  lockfile, scripts, and environment expectations are explicit.

---

## 3. Why It Exists

Without setup discipline, JavaScript teams lose time to:

- different Node versions
- mixed npm/yarn/pnpm lockfiles
- dependencies that install locally but fail in CI
- ESM/CommonJS confusion
- missing test/lint commands
- hidden environment variables
- browser code accidentally importing Node-only modules
- production deploys built from non-reproducible installs

---

## 4. Reality

As of July 2, 2026, the official Node.js releases page lists Node.js v24 as LTS and v26
as Current. Production applications should use Active LTS or Maintenance LTS releases
unless the team has a specific reason and test coverage for Current.

Real teams usually standardize:

```text
Node LTS -> one package manager -> lockfile -> scripts -> lint/test/build -> CI -> deploy
```

Common choices:

| Area | Common Options |
|---|---|
| Runtime | Node.js LTS |
| Package manager | npm, pnpm, Yarn |
| Version pinning | `.nvmrc`, `.node-version`, Volta, asdf, Docker image |
| Package manager pinning | `packageManager` field plus Corepack |
| Type safety | TypeScript or JSDoc/type-checking |
| Testing | Vitest, Jest, node:test, Playwright |
| Lint/format | ESLint, Prettier, Biome |
| Build | Vite, Webpack, Rollup, esbuild, SWC, framework-owned tooling |

---

## 5. How It Works

1. Pick runtime target: browser, Node.js, edge, or library.
2. Pin Node version.
3. Choose package manager and commit the lockfile.
4. Decide module format: ESM, CommonJS, or dual package.
5. Add scripts: `dev`, `test`, `lint`, `format`, `build`, `typecheck`.
6. Add config validation for environment variables.
7. Add first source file and first test.
8. Run locally and in CI using the same package manager command.
9. Document how to start, test, and debug.

Failure path:

- `npm install` changes dependencies unexpectedly.
- `type: module` breaks CommonJS imports.
- frontend bundle includes secrets from env vars.
- CI uses Node 26 while production uses Node 24.
- package manager hoisting hides undeclared dependency.

Recovery path:

- pin runtime and package manager.
- use frozen installs: `npm ci`, `pnpm install --frozen-lockfile`, `yarn install --immutable`.
- add dependency checks.
- make module format explicit.
- validate env at startup.

---

## 6. What Problem It Solves

- Primary problem solved: local-to-CI-to-production drift.
- Secondary benefits: faster onboarding, safer dependency upgrades, easier debugging.
- Systems impact: fewer "works on my machine" failures and fewer release surprises.

---

## 7. When To Rely On It

Use this setup path when:

- starting any JavaScript, TypeScript, Node, frontend, or library project
- joining an existing repo
- preparing for machine-coding interviews
- creating a capstone or portfolio project
- standardizing a team template

Interviewer triggers:

- "How do you start a JavaScript project?"
- "npm install vs npm ci?"
- "Why commit lockfiles?"
- "What is packageManager?"
- "ESM vs CommonJS?"
- "How do you prevent dependency drift?"

---

## 8. When Not To Overcomplicate

Do not add a full toolchain for a tiny script:

- one-off script: Node LTS plus one `.js` file may be enough
- browser demo: plain HTML and JS may be enough
- library: build output and package metadata matter more than a dev server
- framework app: use framework defaults before custom build config

Gold rule:

```text
Start simple, but make the runtime, install command, and test command unambiguous.
```

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Reproducible installs | More config files |
| Fast onboarding | Tooling can distract from fundamentals |
| CI parity | Package manager choice can become team debate |
| Safer dependency hygiene | Lockfile conflicts need care |
| Clear scripts | Bad scripts can hide complexity |

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Mixing npm, Yarn, and pnpm lockfiles | Dependency graph becomes unclear | Pick one manager |
| Using `npm install` in CI casually | Can mutate dependency resolution | Use `npm ci` |
| Not pinning Node | Runtime bugs vary by machine | Use `.nvmrc`, Volta, Docker, or CI image |
| No `type` decision | ESM/CJS imports fail mysteriously | Make module format explicit |
| No env validation | Service starts with missing config | Validate env at startup |
| No first test | Refactors have no safety net | Add one unit or integration test immediately |

---

## 11. First Project Shape

Minimal Node/TypeScript project:

```text
booking-js/
  package.json
  package-lock.json | pnpm-lock.yaml | yarn.lock
  src/
    index.ts
  test/
    index.test.ts
  tsconfig.json
  README.md
```

Example `package.json`:

```json
{
  "name": "booking-js",
  "version": "0.1.0",
  "type": "module",
  "packageManager": "pnpm@9.15.0",
  "scripts": {
    "dev": "tsx watch src/index.ts",
    "build": "tsc -p tsconfig.json",
    "typecheck": "tsc --noEmit",
    "test": "vitest run",
    "lint": "eslint .",
    "format": "prettier --write ."
  },
  "engines": {
    "node": ">=24 <27"
  }
}
```

The exact versions should follow your project policy. The important interview point is
pinning and reproducibility, not memorizing one package manager.

---

## 12. Code Sample

```ts
// src/index.ts
export function formatBooking(id: string, guestName: string): string {
  if (!id.trim()) throw new Error("Booking id is required");
  if (!guestName.trim()) throw new Error("Guest name is required");
  return `${id}:${guestName}`;
}
```

```ts
// test/index.test.ts
import { describe, expect, it } from "vitest";
import { formatBooking } from "../src/index.js";

describe("formatBooking", () => {
  it("formats a booking label", () => {
    expect(formatBooking("B1", "Ava")).toBe("B1:Ava");
  });
});
```

---

## 13. Practical Question

> You joined a JavaScript team with inconsistent local installs and flaky CI. How would
> you make the setup reproducible?

---

## 14. Strong Answer

I would first standardize the Node version and one package manager, then commit the lockfile
and use frozen installs in CI. I would add the `packageManager` field, a documented install
command, and scripts for `dev`, `test`, `lint`, `typecheck`, and `build`. I would make the
module format explicit with `type: module` or CommonJS conventions, validate environment
variables at startup, and add one minimal test so setup problems are caught immediately.
For frontend projects, I would also clarify which environment variables are safe to expose
to the browser and rely on the framework/build tool defaults before custom config.

---

## 15. Revision Notes

- One-line summary: a JavaScript project is production-ready only when runtime, package
  manager, lockfile, module format, scripts, and env config are explicit.
- Three keywords: Node, lockfile, scripts.
- One interview trap: `npm install` and `npm ci` are not the same in CI.
- One memory trick: pin it, install it, script it, test it.

