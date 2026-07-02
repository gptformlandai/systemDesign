# Next.js Middleware and Edge Runtime — Gold Sheet

> Track Module - Group 5: Next.js App Router And Data
> Level: intermediate → senior | Middleware patterns, Edge vs Node, auth, A/B, localization

> Current Next.js 16 note: Middleware has been renamed/deprecated in favor of `proxy.ts`. This sheet remains useful for legacy projects and conceptual patterns, but modern answers should read `NextJS-Proxy-Runtime-Migration-Gold-Sheet.md` and say "Proxy" first.

---

## 1. Intuition

Middleware runs before any request reaches your pages, API routes, or server components. It is the right layer for cross-cutting concerns that apply to many routes.

```text
Request → Middleware → Route Handler / Page / Action
          ↕
    (can redirect, rewrite, modify headers, set cookies)
```

Because middleware runs at the Edge, it has near-zero latency and runs globally close to the user — making it ideal for tasks like redirects, auth checks, and localization.

---

## 2. Creating Middleware

```tsx
// middleware.ts — always in the project root (next to app/ or pages/)
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  // Middleware logic here
  return NextResponse.next();  // continue to the requested route
}

// Configure which routes middleware applies to
export const config = {
  matcher: [
    // Apply to all routes except:
    '/((?!_next/static|_next/image|favicon.ico|public/).*)',
    // Or be explicit about routes:
    '/dashboard/:path*',
    '/api/:path*',
  ],
};
```

**Middleware return values:**
```tsx
NextResponse.next()                         // continue — no change
NextResponse.redirect(new URL('/login', request.url))  // redirect to another URL
NextResponse.rewrite(new URL('/404', request.url))     // rewrite URL (client sees original URL)
NextResponse.json({ error: 'Unauthorized' }, { status: 401 })  // return response directly
```

---

## 3. Pattern 1: Authentication / Route Protection

```tsx
// middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { jwtVerify } from 'jose';  // Edge-compatible JWT library (not jsonwebtoken — uses Node.js APIs)

const JWT_SECRET = new TextEncoder().encode(process.env.JWT_SECRET!);

// Routes that require authentication
const PROTECTED_ROUTES = ['/dashboard', '/settings', '/api/private'];
// Routes only for unauthenticated users
const AUTH_ROUTES = ['/login', '/signup'];

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('auth-token')?.value;

  const isProtectedRoute = PROTECTED_ROUTES.some(route => pathname.startsWith(route));
  const isAuthRoute = AUTH_ROUTES.some(route => pathname.startsWith(route));

  // Verify JWT token (Edge-compatible)
  let isAuthenticated = false;
  if (token) {
    try {
      await jwtVerify(token, JWT_SECRET);
      isAuthenticated = true;
    } catch {
      // Token expired or invalid
    }
  }

  // Redirect unauthenticated users away from protected routes
  if (isProtectedRoute && !isAuthenticated) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('callbackUrl', pathname);  // remember where they were going
    return NextResponse.redirect(loginUrl);
  }

  // Redirect authenticated users away from auth routes
  if (isAuthRoute && isAuthenticated) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
```

---

## 4. Pattern 2: Localization / i18n Routing

```tsx
// middleware.ts — detect preferred locale and redirect
import { match } from '@formatjs/intl-localematcher';
import Negotiator from 'negotiator';

const SUPPORTED_LOCALES = ['en', 'fr', 'de', 'es'];
const DEFAULT_LOCALE = 'en';

function getPreferredLocale(request: NextRequest): string {
  const headers: Record<string, string> = {};
  request.headers.forEach((value, key) => { headers[key] = value; });

  const negotiator = new Negotiator({ headers });
  const languages = negotiator.languages();

  try {
    return match(languages, SUPPORTED_LOCALES, DEFAULT_LOCALE);
  } catch {
    return DEFAULT_LOCALE;
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Check if pathname already has a locale prefix
  const hasLocale = SUPPORTED_LOCALES.some(
    locale => pathname.startsWith(`/${locale}/`) || pathname === `/${locale}`
  );
  
  if (hasLocale) return NextResponse.next();

  // Redirect to locale-prefixed path
  const locale = getPreferredLocale(request);
  return NextResponse.redirect(new URL(`/${locale}${pathname}`, request.url));
}

export const config = {
  matcher: ['/((?!_next|api|favicon.ico).*)'],
};
```

---

## 5. Pattern 3: A/B Testing

```tsx
// middleware.ts — assign users to A/B test variants
export function middleware(request: NextRequest) {
  // Check for existing variant assignment
  const existingVariant = request.cookies.get('ab-variant')?.value;
  
  // Assign variant if not yet assigned (50/50 split)
  const variant = existingVariant ?? (Math.random() < 0.5 ? 'control' : 'treatment');
  
  // Rewrite to variant-specific page
  const url = request.nextUrl.clone();
  if (request.nextUrl.pathname === '/checkout') {
    url.pathname = variant === 'treatment' ? '/checkout-v2' : '/checkout';
  }
  
  const response = NextResponse.rewrite(url);
  
  // Persist variant in cookie (1-day expiry)
  if (!existingVariant) {
    response.cookies.set('ab-variant', variant, { 
      maxAge: 60 * 60 * 24,
      httpOnly: false,  // allow client JS to read for analytics
    });
  }

  return response;
}
```

---

## 6. Pattern 4: Rate Limiting

Edge middleware can implement simple rate limiting using cookies or headers:

```tsx
import { LRUCache } from 'lru-cache';  // Edge-compatible

const rateLimitCache = new LRUCache<string, number[]>({
  max: 10_000,  // max unique IPs to track
  ttl: 60_000,  // 1 minute window
});

function getRateLimitKey(request: NextRequest): string {
  return request.headers.get('x-forwarded-for') ?? request.ip ?? 'unknown';
}

export function middleware(request: NextRequest) {
  if (!request.nextUrl.pathname.startsWith('/api/')) {
    return NextResponse.next();
  }

  const key = getRateLimitKey(request);
  const now = Date.now();
  const windowStart = now - 60_000;

  const timestamps = rateLimitCache.get(key) ?? [];
  const recentRequests = timestamps.filter(t => t > windowStart);
  recentRequests.push(now);
  rateLimitCache.set(key, recentRequests);

  const remaining = Math.max(0, 100 - recentRequests.length);  // 100 req/min limit

  const response = remaining > 0
    ? NextResponse.next()
    : NextResponse.json({ error: 'Rate limit exceeded' }, { status: 429 });

  response.headers.set('X-RateLimit-Remaining', String(remaining));
  response.headers.set('X-RateLimit-Reset', String(Math.ceil(now / 1000) + 60));

  return response;
}
```

---

## 7. Edge Runtime vs Node.js Runtime

| Feature | Edge Runtime | Node.js Runtime |
|---|---|---|
| Location | CDN edge nodes globally | Single-region server |
| Cold start | Near-zero | Slow (100ms-1s) |
| Latency | Closest to user | Single region round-trip |
| Available APIs | Web Platform APIs (fetch, crypto, TextEncoder) | Full Node.js (fs, net, child_process, Buffer) |
| Memory limit | 128MB | 1GB+ |
| Execution limit | 30 seconds | 60+ seconds (configurable) |
| npm packages | Must use Edge-compatible packages | Any npm package |
| Database access | Via HTTP (Neon, PlanetScale HTTP mode) | Direct TCP/socket connection |

### Choosing Edge vs Node.js

```tsx
// Route handler — default is Node.js runtime
export async function GET(request: Request) { ... }

// Opt into Edge runtime
export const runtime = 'edge';

export async function GET(request: Request) { ... }  // now runs at the edge
```

**Use Edge runtime when:**
- Middleware (Edge-only — cannot change)
- Auth checks that just verify JWTs
- Geo-routing, redirects, header manipulation
- AI streaming responses (lower latency)
- Simple personalizations without heavy compute

**Use Node.js runtime when:**
- Database queries via Prisma (requires TCP connection)
- File system operations
- Libraries that use Node.js APIs (sharp for images, pdf generation)
- Heavy computation
- Webhooks that need full request processing

---

## 8. Modifying Request/Response Headers

```tsx
export function middleware(request: NextRequest) {
  const response = NextResponse.next({
    request: {
      // Add request headers — visible to route handlers and Server Components
      headers: new Headers({
        ...Object.fromEntries(request.headers.entries()),
        'x-user-id': getUserIdFromToken(request) ?? '',
        'x-request-id': crypto.randomUUID(),
      }),
    },
  });

  // Add response headers — visible to the browser
  response.headers.set('X-Content-Type-Options', 'nosniff');
  response.headers.set('X-Frame-Options', 'SAMEORIGIN');
  response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');

  return response;
}

// Reading x-user-id in a Server Component (set by middleware)
import { headers } from 'next/headers';

async function UserInfo() {
  const headersList = await headers();
  const userId = headersList.get('x-user-id');
  // ...
}
```

---

## 9. Middleware Execution Order and Matcher Patterns

```tsx
// Matcher patterns
export const config = {
  matcher: [
    // Match all routes
    '/:path*',
    
    // Match routes starting with /dashboard
    '/dashboard/:path*',
    
    // Exclude static files and Next.js internals (most common pattern)
    '/((?!_next/static|_next/image|favicon.ico|.*\\.png$|.*\\.jpg$).*)',
    
    // Multiple matchers — middleware runs if ANY match
    ['/dashboard/:path*', '/api/private/:path*'],
    
    // Conditional match with has (match only requests with specific header/cookie)
    {
      source: '/dashboard/:path*',
      has: [{ type: 'cookie', key: 'auth-token' }],
    },
  ],
};
```

---

## 10. Common Mistakes

| Mistake | Why Wrong | Fix |
|---|---|---|
| Using `jsonwebtoken` in middleware | Depends on Node.js crypto — not Edge-compatible | Use `jose` library |
| Verifying JWTs with DB lookup in middleware | Too slow — defeats edge advantage | Stateless JWT only in middleware, DB session check in route handler |
| Matching too broadly | Middleware runs on static files too | Exclude `_next/static`, `_next/image`, `favicon.ico` |
| Not setting `callbackUrl` on redirect | User loses their destination | Always preserve intended destination in redirect |
| Heavy computation in middleware | Slow, expensive at edge | Keep middleware fast — defer heavy work to route handlers |
| Using `process.env` for secrets in edge | Not all env vars available at edge | Use `process.env.NEXT_PUBLIC_*` for public or mark vars as server-only |

---

## 11. Strong Interview Answer

**Q: What is Next.js middleware and when do you use it?**

```text
Middleware is code that runs before every matched request, at the Edge — meaning it
runs in CDN nodes globally, close to users, with near-zero latency. It can redirect,
rewrite URLs, modify request or response headers, and set cookies.

I use middleware for cross-cutting concerns that apply to many routes: authentication
redirects (send unauthenticated users to /login, redirect logged-in users away from
/login), locale detection and routing, A/B test variant assignment, and lightweight
rate limiting.

The important constraint is that middleware runs on the Edge runtime, not Node.js.
This means no Prisma, no TCP database connections, no Node.js-only libraries. Auth
should use stateless JWT verification with Edge-compatible libraries like `jose`,
not session lookups. Heavy work stays in route handlers and Server Components.
```

---

## 12. Revision Notes

- `middleware.ts` file in root — the only location; always export `config.matcher`
- Edge runtime: Web Platform APIs only — no Node.js APIs, no Prisma, no TCP
- Use `jose` for JWT (Edge-compatible), NOT `jsonwebtoken` (Node.js only)
- `NextResponse.next()` = continue; `NextResponse.redirect()` = new URL; `NextResponse.rewrite()` = proxy to different URL, client sees original
- Middleware cannot read response bodies — only request/response metadata
- Headers set in middleware via `request: { headers: ... }` are visible to Server Components via `headers()` function
- `export const runtime = 'edge'` on route handlers opts them into Edge; middleware is always Edge
