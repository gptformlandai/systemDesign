# Next.js Proxy, Runtime, And Middleware Migration - Gold Sheet

> Track File #24 - Group 5: Next.js App Router And Data
> Level: intermediate -> senior | `proxy.ts`, redirects, rewrites, auth gates, CORS, runtime choices, and migration from Middleware

---

## 1. Intuition

Proxy is a request boundary in front of your app.

```text
Incoming request -> proxy.ts -> route/page/action/handler
```

It can redirect, rewrite, set cookies, set headers, short-circuit a response, or let the request continue.

The important senior-level point:
Proxy is powerful, so it should be used sparingly. Prefer route-level logic when route-level logic is enough.

---

## 2. Definition

- Definition: Next.js Proxy is the file convention that runs code before a matched request reaches application routes.
- Category: Request routing / edge or server boundary.
- Core idea: Centralize cross-cutting request decisions that must happen before rendering.

---

## 3. Middleware To Proxy Migration

Current Next.js 16 direction:

```text
middleware.ts -> proxy.ts
export function middleware() -> export function proxy()
```

Migration command:

```bash
npx @next/codemod@canary middleware-to-proxy .
```

Manual migration:

```diff
- // middleware.ts
+ // proxy.ts
  import { NextResponse } from 'next/server';
  import type { NextRequest } from 'next/server';

- export function middleware(request: NextRequest) {
+ export function proxy(request: NextRequest) {
    return NextResponse.next();
  }
```

Why the rename matters:
- "Middleware" sounded like Express middleware and encouraged overuse.
- "Proxy" makes the network-boundary behavior clearer.
- In Next.js 16, Middleware is deprecated and renamed to Proxy.
- Proxy defaults to the Node.js runtime.

---

## 4. Basic Proxy

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname === '/') {
    return NextResponse.redirect(new URL('/home', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
```

Return options:

```ts
NextResponse.next();
NextResponse.redirect(new URL('/login', request.url));
NextResponse.rewrite(new URL('/maintenance', request.url));
NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
```

---

## 5. When To Use Proxy

Strong fit:
- global redirects;
- locale negotiation;
- A/B assignment before route render;
- auth gate before protected sections;
- request header normalization;
- CORS preflight for broad route groups;
- coarse bot/device routing;
- legacy URL migration.

Weak fit:
- complex business logic;
- database-heavy authorization;
- rendering data decisions;
- mutations;
- per-component feature flags;
- work that belongs in route handlers or Server Components.

Rule:
Proxy should decide where the request goes, not own the product domain.

---

## 6. Auth Gate Pattern

Proxy can do a coarse auth check, but final authorization must still happen in the route/action/data layer.

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const protectedPrefixes = ['/dashboard', '/settings', '/admin'];

export function proxy(request: NextRequest) {
  const pathname = request.nextUrl.pathname;
  const isProtected = protectedPrefixes.some(prefix => pathname.startsWith(prefix));

  if (!isProtected) {
    return NextResponse.next();
  }

  const session = request.cookies.get('session')?.value;

  if (!session) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/dashboard/:path*', '/settings/:path*', '/admin/:path*'],
};
```

Do not rely on this alone:

```ts
'use server';

export async function deleteUser(userId: string) {
  const session = await requireSession();
  await requireAdmin(session.user.id);
  await deleteUserById(userId);
}
```

Proxy prevents obvious navigation. Server-side authorization prevents real attacks.

---

## 7. Localization Pattern

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const locales = ['en', 'es', 'fr'] as const;
const defaultLocale = 'en';

function pathnameHasLocale(pathname: string) {
  return locales.some(locale => pathname === `/${locale}` || pathname.startsWith(`/${locale}/`));
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathnameHasLocale(pathname)) {
    return NextResponse.next();
  }

  const acceptLanguage = request.headers.get('accept-language') ?? '';
  const preferredLocale = acceptLanguage.startsWith('es') ? 'es' : defaultLocale;

  return NextResponse.redirect(new URL(`/${preferredLocale}${pathname}`, request.url));
}

export const config = {
  matcher: ['/((?!_next|api|favicon.ico).*)'],
};
```

Production additions:
- use a proper language negotiation library;
- respect user-selected locale cookie;
- avoid redirect loops;
- keep canonical metadata and hreflang correct.

---

## 8. A/B Assignment Pattern

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function proxy(request: NextRequest) {
  if (request.nextUrl.pathname !== '/checkout') {
    return NextResponse.next();
  }

  const existing = request.cookies.get('checkout_variant')?.value;
  const variant = existing ?? (Math.random() < 0.5 ? 'control' : 'fast');

  const url = request.nextUrl.clone();
  url.pathname = variant === 'fast' ? '/checkout-fast' : '/checkout';

  const response = NextResponse.rewrite(url);

  if (!existing) {
    response.cookies.set('checkout_variant', variant, {
      httpOnly: true,
      sameSite: 'lax',
      secure: true,
      maxAge: 60 * 60 * 24 * 7,
    });
  }

  return response;
}
```

Watch out:
- random assignment must be stable;
- analytics must record assignment;
- SEO routes should avoid accidental duplicate indexing.

---

## 9. CORS Pattern

```ts
// proxy.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const allowedOrigins = new Set(['https://app.example.com', 'https://admin.example.com']);

export function proxy(request: NextRequest) {
  const origin = request.headers.get('origin') ?? '';
  const isAllowed = allowedOrigins.has(origin);

  if (request.method === 'OPTIONS') {
    return NextResponse.json(
      {},
      {
        headers: {
          ...(isAllowed ? { 'Access-Control-Allow-Origin': origin } : {}),
          'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        },
      },
    );
  }

  const response = NextResponse.next();

  if (isAllowed) {
    response.headers.set('Access-Control-Allow-Origin', origin);
  }

  return response;
}

export const config = {
  matcher: ['/api/:path*'],
};
```

---

## 10. Runtime Choice

| Runtime | Good For | Watch Out |
|---|---|---|
| Node.js | Most apps, auth libraries, crypto, DB clients | Slower cold starts than edge in some environments |
| Edge | Lightweight geo/redirect/header work near users | Limited APIs and package compatibility |

In Next.js 16, Proxy defaults to Node.js runtime. Choose Edge intentionally when the code is small, dependency-light, and latency-sensitive.

---

## 11. RSC Requests And Rewrites

Proxy touches both HTML and RSC navigation requests. If you rewrite manually with `fetch`, you can break internal RSC request details.

Safer:

```ts
return NextResponse.rewrite(new URL('/new-route', request.url));
```

Riskier:

```ts
return fetch(new URL('/new-route', request.url));
```

Use framework primitives unless you have a very specific reason not to.

---

## 12. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Keeping `middleware.ts` in Next 16 material | Teaches deprecated convention | Teach `proxy.ts` first |
| Putting DB-heavy logic in Proxy | Adds latency and runtime coupling | Do coarse checks only |
| Trusting Proxy as full authz | Attackers can call actions/routes directly | Re-authorize in server code |
| Huge matcher | Runs on too many requests | Narrow matcher |
| Setting request headers as response headers | Leaks internal data | Use `NextResponse.next({ request: { headers } })` correctly |
| Random A/B every request | Bad experiment data | Persist assignment |

---

## 13. Practical Question

> Your Next.js app has locale redirects, protected dashboard routes, and API CORS. Would you use Proxy?

---

## 14. Strong Answer

```text
I would use Proxy for lightweight request-boundary concerns: locale detection,
coarse dashboard redirects, and broad API CORS handling. I would keep matchers
narrow and avoid domain-heavy database logic there. For auth, Proxy can redirect
unauthenticated users, but route handlers, Server Components, and Server Actions
must still re-check authentication and authorization. In Next.js 16 I would use
proxy.ts, not middleware.ts, and choose Node or Edge runtime based on dependency
compatibility and latency needs.
```

---

## 15. Revision Notes

- One-line summary: Proxy is the modern request-boundary convention that replaces Middleware in Next.js 16.
- Three keywords: `proxy.ts`, matcher, request boundary.
- One interview trap: Proxy auth is not enough; actions and data access still need authorization.
- One memory trick: Proxy routes the request; server code owns the decision.

