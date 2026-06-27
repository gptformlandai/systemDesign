# React + Next.js Production Architecture - Gold Sheet

> Track File #19 of 24 - Group 7: Production Architecture And System Design
> Covers: folder structure, feature-based architecture, component design patterns, monorepo, Turborepo

---

## 1. Intuition

Production architecture controls change velocity.

```text
features own product behavior
shared packages own reusable primitives
core owns infrastructure boundaries
app router owns composition
```

The goal is not clever folders. The goal is fewer accidental couplings.

---

## 2. Feature-Based Structure

```text
src/
  app/
    layout.tsx
    page.tsx
    dashboard/
  features/
    auth/
      components/
      actions/
      queries/
      types.ts
    products/
      components/
      data/
      ui/
      types.ts
  shared/
    ui/
    hooks/
    utils/
  core/
    env/
    logging/
    auth/
    api/
```

Rule:
Feature code can import shared/core. Shared/core should not import feature code.

---

## 3. Component Design Patterns

Component layers:

```text
page/route -> feature container -> feature component -> shared UI primitive
```

Good component API:
- clear props
- controlled/uncontrolled option when useful
- accessible defaults
- stable event names
- no hidden global side effects

---

## 4. Monorepo And Turborepo

Monorepo can contain:

```text
apps/
  web/
  admin/
packages/
  ui/
  config/
  eslint-config/
  types/
  analytics/
```

Benefits:
- shared design system
- shared config
- atomic changes
- consistent tooling
- package-level ownership

Costs:
- build orchestration
- dependency boundaries
- versioning/release complexity
- CI caching needs

Turborepo helps with task pipelines and caching.

---

## 5. Boundaries

Good boundaries:
- UI primitives do not import app data.
- Server-only code stays out of client bundles.
- Feature modules expose narrow APIs.
- Data access is centralized.
- Environment reads are validated in one place.

Bad boundary:

```text
shared/Button imports auth store
```

Better:

```text
feature/AuthButton composes shared/Button
```

---

## 6. Real-World Use Cases

- SaaS: shared UI package across dashboard and admin.
- Ecommerce: product, cart, checkout feature modules.
- GenAI app: chat feature isolated from billing/auth.
- Enterprise monorepo: web app, docs, component library, shared config.

---

## 7. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Folders by file type only | features scattered | feature ownership |
| Shared folder as dumping ground | no ownership | promote only stable reusable code |
| Client imports server module | build/runtime failure | enforce server/client boundaries |
| UI package imports business code | tight coupling | compose in feature layer |
| Monorepo without boundaries | big ball of code | package ownership and lint rules |

---

## 8. Strong Interview Answer

Question:
How do you structure a large React/Next.js app?

Strong answer:

```text
I use feature-based architecture with a small core layer and a shared design
system. App Router files compose routes, layouts, and boundaries. Feature modules
own product behavior, data functions, server actions, and feature-specific UI.
Shared UI primitives stay business-agnostic and accessible. Core owns env,
logging, auth/session helpers, and API infrastructure. In a monorepo, Turborepo
can cache builds and tests, but package boundaries and ownership matter more than
the tool.
```

---

## 9. Revision Notes

- One-line summary: Production architecture is dependency direction and ownership.
- Three keywords: features, shared, monorepo.
- One interview trap: Monorepo does not automatically mean good architecture.
- One memory trick: Shared code must be more stable than feature code.

