# React + Next.js Deployment And Infrastructure - Gold Sheet

> Track File #18 of 24 - Group 7: Production Architecture And System Design
> Covers: Vercel deployment, CI/CD, environment variables, Edge functions

---

## 1. Intuition

Deployment is how code becomes a reliable user-facing system.

```text
commit -> CI -> build -> test -> deploy -> observe -> rollback
```

Next.js deployment also depends on runtime choice: Node, Edge, static, serverless, or self-hosted.

---

## 2. Vercel Deployment

Vercel is a natural Next.js platform:
- preview deployments per PR
- production deployments
- serverless/edge support
- image optimization
- analytics and Web Vitals
- environment variable management
- rollback support

Trade-off:
Strong platform integration, but platform-specific features can create portability concerns.

---

## 3. CI/CD Basics

Pipeline:

```text
pull request:
  install
  typecheck
  lint
  unit/component tests
  build
  preview deploy

main/release:
  integration/E2E smoke
  production deploy
  monitor metrics
  rollback if needed
```

Quality gates:
- TypeScript
- lint
- tests
- build
- bundle budget
- Playwright smoke
- env validation

---

## 4. Environment Variables

Rules:
- Server-only secrets must not be exposed to client.
- `NEXT_PUBLIC_` variables are bundled for browser use.
- Validate env at startup/build.
- Separate dev/stage/prod.

Example:

```ts
export const serverEnv = {
  DATABASE_URL: required(process.env.DATABASE_URL),
  AUTH_SECRET: required(process.env.AUTH_SECRET),
};

export const clientEnv = {
  NEXT_PUBLIC_APP_ENV: required(process.env.NEXT_PUBLIC_APP_ENV),
};
```

Trap:
Anything exposed to browser is public.

---

## 5. Edge Functions

Edge runtime runs close to users.

Good for:
- auth redirects
- personalization headers
- A/B assignment
- lightweight geolocation logic
- low-latency middleware-like decisions

Avoid for:
- heavy CPU
- unsupported Node APIs
- long-running tasks
- database drivers that require Node runtime

---

## 6. Self-Hosting

Consider:
- Node server runtime
- Docker
- CDN/proxy
- image optimization support
- cache handler strategy
- logs/traces
- process scaling
- zero-downtime deploys

Trade-off:
More control, more operational responsibility.

---

## 7. Real-World Use Cases

- SaaS app: preview deployments for product review.
- Ecommerce: staged rollout plus cache revalidation.
- Multi-region public pages: CDN/edge.
- Internal admin: self-hosted behind corporate network.
- Experiment: Edge assignment cookie.

---

## 8. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Secret in `NEXT_PUBLIC_` | Exposed to browser | server-only env |
| CI skips build | Next errors missed | run production build |
| No preview env parity | bugs appear in prod | staged env config |
| Heavy work at edge | runtime limits | Node/server job |
| No rollback plan | slow incident recovery | keep previous deployment |

---

## 9. Strong Interview Answer

Question:
How would you deploy a Next.js app?

Strong answer:

```text
I would run CI with typecheck, lint, tests, and a production build. For Vercel,
I would use preview deployments for PRs and production deployments from main or
release branches. Environment variables are separated by environment, and only
`NEXT_PUBLIC_` values are exposed to the browser. I choose Node, Edge, static, or
serverless per route based on runtime needs. After deployment, I monitor Web
Vitals, errors, logs, and business metrics, with rollback ready.
```

---

## 10. Revision Notes

- One-line summary: Deployment is build correctness plus runtime choice plus observability.
- Three keywords: CI, env, edge.
- One interview trap: `NEXT_PUBLIC_` secrets are not secret.
- One memory trick: Preview before prod, observe after prod, rollback when needed.

