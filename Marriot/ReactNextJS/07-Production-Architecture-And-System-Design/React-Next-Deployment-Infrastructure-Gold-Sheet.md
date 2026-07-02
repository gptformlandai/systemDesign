# React + Next.js Deployment And Infrastructure - Gold Sheet

> Track Module - Group 7: Production Architecture And System Design
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

### Docker Multi-Stage Build for Next.js

```dockerfile
# Stage 1: dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# Stage 2: build
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Stage 3: production runner (standalone output)
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
```

`next.config.ts` required option:

```ts
const nextConfig = {
  output: 'standalone', // copies only required node_modules
};
```

`standalone` output produces a minimal `server.js` with all dependencies. No need to copy full `node_modules`.

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: web
          image: myapp:v1.2.3
          ports:
            - containerPort: 3000
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: app-secrets
                  key: database_url
          readinessProbe:
            httpGet:
              path: /api/health
              port: 3000
            initialDelaySeconds: 5
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

---

## 7. Monitoring and Observability

### Sentry Integration

```ts
// sentry.client.config.ts
import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  tracesSampleRate: 0.1,  // 10% of transactions
  replaysOnErrorSampleRate: 1.0,
  environment: process.env.NEXT_PUBLIC_APP_ENV,
});
```

Sentry captures:
- unhandled errors (client + server)
- source-map-resolved stack traces
- performance transactions
- session replays for error context

### OpenTelemetry for Next.js

```ts
// instrumentation.ts (Next.js 14+)
export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs') {
    const {NodeSDK} = await import('@opentelemetry/sdk-node');
    const {OTLPTraceExporter} = await import(
      '@opentelemetry/exporter-trace-otlp-http'
    );

    const sdk = new NodeSDK({
      traceExporter: new OTLPTraceExporter({
        url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
      }),
    });

    sdk.start();
  }
}
```

Next.js has native OpenTelemetry instrumentation for:
- incoming requests
- `fetch` calls
- route rendering
- database queries (via library spans)

---

## 8. Blue-Green and Canary Deployments

### Blue-Green

```text
Production → Blue (v1.0)
Deploy Green (v1.1) → test → switch traffic → Blue becomes standby
Rollback = switch back to Blue (instant)
```

Use for:
- guaranteed zero-downtime switches
- easy rollback

### Canary

```text
Production → 95% v1.0, 5% v1.1
Monitor metrics/errors → ramp to 20%, 50%, 100% → or rollback
```

Next.js canary with Vercel feature flags or custom middleware:

```ts
// middleware.ts — route 10% of users to v2
export function middleware(request: NextRequest) {
  const isCanary = Math.random() < 0.1;
  const url = request.nextUrl.clone();

  if (isCanary) {
    const response = NextResponse.rewrite(url);
    response.cookies.set('canary', 'true', {httpOnly: true});
    return response;
  }
}
```

### Bundle Size Budget in CI

```bash
# package.json script
"bundlesize": "bundlesize"

# bundlesize.config.js
module.exports = [
  {path: '.next/static/chunks/pages/*.js', maxSize: '150 kB'},
  {path: '.next/static/chunks/app/*.js', maxSize: '200 kB'},
];
```

Or with `next build` output checking:

```ts
// In CI: fail if any JS chunk exceeds threshold
const stats = JSON.parse(fs.readFileSync('.next/build-manifest.json', 'utf8'));
// parse and assert
```

---

## 9. Real-World Use Cases

- SaaS app: preview deployments for product review.
- Ecommerce: staged rollout plus cache revalidation on product updates.
- Multi-region public pages: CDN/edge.
- Internal admin: self-hosted Docker on Kubernetes behind corporate network.
- Experiment: canary middleware cookie for 10% traffic split.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Secret in `NEXT_PUBLIC_` | Exposed to browser | server-only env |
| CI skips build | Next errors missed | run production build |
| No preview env parity | bugs appear in prod | staged env config |
| Heavy work at edge | runtime limits | Node/server job |
| No rollback plan | slow incident recovery | keep previous deployment |
| Docker without `standalone` output | huge image | `output: 'standalone'` in next.config |
| No Sentry source maps | unreadable crash traces | upload source maps in CI |

---

## 11. Strong Interview Answer

Question:
How would you deploy a Next.js app in production?

Strong answer:

```text
I run CI with typecheck, lint, tests, and a production Next build. For Vercel I
use preview deployments for PR review and production from the main branch. For
self-hosting I use Docker with a multi-stage build and Next's standalone output
mode, which produces a minimal image with only the required dependencies. I deploy
to Kubernetes with a readiness probe on /api/health, set memory/CPU limits, and
inject secrets from Kubernetes secrets rather than baking them in. I integrate
Sentry with source map upload for crash traces, and configure OpenTelemetry via
next's instrumentation.ts for distributed tracing. For risky releases I use canary
deployment — routing 10% of traffic first — with automatic rollback if error rate
exceeds threshold. Only NEXT_PUBLIC_ values go to the browser; server secrets stay
in server env only.
```

---

## 12. Revision Notes

- One-line summary: Deployment is build correctness, runtime choice, container config, and observability.
- Three keywords: standalone, canary, Sentry.
- One interview trap: `NEXT_PUBLIC_` secrets are not secret — they go into the browser bundle.
- One memory trick: Build → Container → Canary → Observe → Rollback.

