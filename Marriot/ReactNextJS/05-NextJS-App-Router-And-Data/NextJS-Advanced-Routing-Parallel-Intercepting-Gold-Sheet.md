# Next.js Advanced Routing — Parallel Routes, Intercepting Routes, Route Handlers Gold Sheet

> Track File #32 of 40 · Group 5: Next.js App Router And Data
> Level: intermediate → senior | Parallel routes, intercepting routes, groups, route handlers

---

## 1. Intuition

The App Router uses a file-system routing model where folder names define URL segments. Advanced patterns — route groups, parallel routes, intercepting routes — unlock UI architectures that are impossible with traditional page-per-file routing.

```text
Traditional routing: one URL = one page component
Advanced App Router:
  Parallel routes: one URL = multiple independent page slots
  Intercepting routes: same URL = different UI depending on how you arrived
  Route groups: organize code without affecting URLs
```

---

## 2. Route Groups — Organize Without Affecting URLs

Route groups use `(folderName)` syntax — the folder is ignored in the URL:

```text
app/
  (marketing)/           ← NOT in URL
    layout.tsx           ← marketing layout (no header/footer overhead)
    page.tsx             → /
    about/
      page.tsx           → /about
    pricing/
      page.tsx           → /pricing
  (app)/                 ← NOT in URL
    layout.tsx           ← authenticated app layout (sidebar, auth check)
    dashboard/
      page.tsx           → /dashboard
    settings/
      page.tsx           → /settings
```

**Why it matters:** different sections of the same app can have completely different layouts without nesting or shared layout overhead.

```tsx
// (marketing)/layout.tsx — minimal layout for public pages
export default function MarketingLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <MarketingHeader />
      <main>{children}</main>
      <MarketingFooter />
    </>
  );
}

// (app)/layout.tsx — authenticated app layout
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <main>{children}</main>
    </div>
  );
}
```

---

## 3. Dynamic Routes — Catch-All and Optional Catch-All

```text
app/
  blog/
    [slug]/page.tsx          → /blog/my-post (single dynamic segment)
    [...slug]/page.tsx        → /blog/2024/01/my-post (catch-all — 1+ segments)
    [[...slug]]/page.tsx     → /blog OR /blog/2024/01/my-post (optional catch-all — 0+ segments)
```

```tsx
// [...slug]/page.tsx — matches /docs/getting-started/installation
type DocsPageProps = {
  params: Promise<{ slug: string[] }>;  // slug is an array of segments
};

export default async function DocsPage({ params }: DocsPageProps) {
  const { slug } = await params;
  // slug = ['getting-started', 'installation'] for /docs/getting-started/installation
  const doc = await getDoc(slug.join('/'));
  return <DocContent doc={doc} />;
}

// [[...slug]]/page.tsx — matches /docs AND /docs/anything/here
export default async function DocsPage({ params }: { params: Promise<{ slug?: string[] }> }) {
  const { slug } = await params;
  // slug = undefined for /docs
  // slug = ['getting-started'] for /docs/getting-started
  const doc = await getDoc(slug?.join('/') ?? 'index');
  return <DocContent doc={doc} />;
}
```

---

## 4. Parallel Routes — Multiple Pages in One Layout

Parallel routes let you render multiple pages simultaneously in the same layout. Each "slot" (`@slotName`) is an independent route.

```text
app/
  @team/
    page.tsx          ← team slot content
    settings/
      page.tsx        ← team/settings slot content
  @analytics/
    page.tsx          ← analytics slot content
    revenue/
      page.tsx        ← analytics/revenue slot content
  layout.tsx          ← receives @team and @analytics as props
  page.tsx
```

```tsx
// layout.tsx — receives each slot as a prop
export default function DashboardLayout({
  children,
  team,
  analytics,
}: {
  children: React.ReactNode;
  team: React.ReactNode;
  analytics: React.ReactNode;
}) {
  return (
    <div className="dashboard">
      <div className="main">{children}</div>
      <div className="sidebar-team">{team}</div>
      <div className="sidebar-analytics">{analytics}</div>
    </div>
  );
}
```

**Key feature:** Each slot loads independently. If `@analytics` is slow, the rest of the layout shows immediately — Suspense applies per slot.

**default.tsx — required for unmatched slots:**

```tsx
// @team/default.tsx
// Shown when the URL doesn't match any @team route
export default function TeamDefault() {
  return <p>Select a team member</p>;
}
```

---

## 5. Intercepting Routes — Modals and Overlays

Intercepting routes let you display content from one route while keeping the URL context. The canonical use case is photo/post modals.

```text
(..) notation:
  (.) — intercepts routes one level above the current segment
  (..) — intercepts routes from two levels above
  (...) — intercepts routes from the root

app/
  feed/
    page.tsx                          → /feed (list of photos)
    (..)photo/[id]/page.tsx          ← intercepts /photo/[id] when navigating from /feed
  photo/
    [id]/
      page.tsx                        → /photo/123 (full page when accessed directly)
```

```tsx
// app/feed/(..)photo/[id]/page.tsx
// This renders as a MODAL when navigating from /feed, but the URL becomes /photo/123
export default async function PhotoModal({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const photo = await getPhoto(id);

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <img src={photo.url} alt={photo.title} />
        <p>{photo.description}</p>
        <Link href="/feed">Close</Link>
      </div>
    </div>
  );
}

// app/photo/[id]/page.tsx  
// This renders the full page when accessed directly (URL shared, page refresh)
export default async function PhotoPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const photo = await getPhoto(id);
  return <FullPhotoView photo={photo} />;
}
```

**How this works:**
- User browses `/feed` → clicks a photo → URL changes to `/photo/123` → App Router detects the intercept → renders `PhotoModal` as an overlay in the feed layout
- User refreshes at `/photo/123`, shares the URL, or navigates directly → App Router renders `PhotoPage` as a full page

This is exactly how Instagram, Pinterest, and Twitter photo modals work.

---

## 6. Combining Parallel + Intercepting — Modal Pattern

The full production pattern:

```text
app/
  @modal/
    (.)product/[id]/
      page.tsx        ← modal version of product page
    default.tsx       ← null (no modal by default)
  product/
    [id]/
      page.tsx        ← full product page
  layout.tsx          ← renders @modal slot conditionally
  page.tsx            ← shop listing page
```

```tsx
// app/layout.tsx
export default function ShopLayout({
  children,
  modal,
}: {
  children: React.ReactNode;
  modal: React.ReactNode;
}) {
  return (
    <>
      {children}
      {modal}  {/* renders ProductModal when @modal slot is active */}
    </>
  );
}

// app/@modal/default.tsx
export default function ModalDefault() {
  return null;  // nothing in modal slot by default
}

// app/@modal/(.)product/[id]/page.tsx
'use client';
import { useRouter } from 'next/navigation';

export default function ProductModal({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  
  return (
    <div className="modal-overlay" onClick={() => router.back()}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <Suspense fallback={<Skeleton />}>
          <ProductModalContent id={(use(params)).id} />
        </Suspense>
        <button onClick={() => router.back()}>×</button>
      </div>
    </div>
  );
}
```

---

## 7. Route Handlers — API Routes in App Router

Route Handlers replace `pages/api/` routes. They use standard Web APIs:

```tsx
// app/api/products/route.ts
import { NextRequest, NextResponse } from 'next/server';

// GET /api/products?category=electronics
export async function GET(request: NextRequest) {
  const { searchParams } = request.nextUrl;
  const category = searchParams.get('category');

  const products = await db.product.findMany({
    where: category ? { category } : undefined,
    take: 20,
  });

  return NextResponse.json(products, {
    headers: {
      'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=300',
    },
  });
}

// POST /api/products
export async function POST(request: NextRequest) {
  const body = await request.json();
  const result = CreateProductSchema.safeParse(body);
  
  if (!result.success) {
    return NextResponse.json({ error: result.error.flatten() }, { status: 422 });
  }

  const product = await db.product.create({ data: result.data });
  return NextResponse.json(product, { status: 201 });
}
```

```tsx
// app/api/products/[id]/route.ts — dynamic route handler
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const product = await db.product.findUnique({ where: { id } });
  
  if (!product) {
    return NextResponse.json({ error: 'Not found' }, { status: 404 });
  }
  
  return NextResponse.json(product);
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const session = await getSession(request);
  if (!session) return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  
  const { id } = await params;
  await db.product.delete({ where: { id } });
  
  return new NextResponse(null, { status: 204 });
}
```

---

## 8. Streaming with Route Handlers

```tsx
// app/api/ai/chat/route.ts — streaming AI response
import { openai } from '@ai-sdk/openai';
import { streamText } from 'ai';

export async function POST(request: NextRequest) {
  const { messages } = await request.json();

  const result = streamText({
    model: openai('gpt-4o'),
    messages,
  });

  return result.toDataStreamResponse();  // streams tokens as they arrive
}

// Manual streaming with ReadableStream
export async function GET() {
  const encoder = new TextEncoder();
  
  const stream = new ReadableStream({
    async start(controller) {
      for (let i = 0; i < 10; i++) {
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ count: i })}\n\n`));
        await new Promise(r => setTimeout(r, 100));
      }
      controller.close();
    },
  });

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
    },
  });
}
```

---

## 9. Common Routing Mistakes

| Mistake | Why Wrong | Fix |
|---|---|---|
| Missing `default.tsx` for parallel routes | "Children" pages break on direct access | Always add `default.tsx` to each slot |
| Intercepting route without full-page fallback | Sharing URL shows intercepted version | Always implement the actual route too |
| Route group expecting shared layout | `(group)` layouts are independent | Each group has its own layout — can't share between groups without nesting |
| Not awaiting `params` | Next.js 15: params/searchParams are Promises | `const { id } = await params` |
| Using `export default` for route handlers | Route handlers use named exports | Export `GET`, `POST`, `PUT`, `DELETE` etc. |

---

## 10. Strong Interview Answer

**Q: Explain parallel routes and intercepting routes in Next.js.**

```text
Parallel routes let you render multiple independent page components in the same
layout simultaneously. Each slot is defined with @slotName and passed as a prop
to the layout. They load independently — one slow slot does not block the others.
They are ideal for dashboard layouts where sidebar sections are independent of
the main content.

Intercepting routes let you display a route's content in a different context
depending on how the user arrived. The classic example is a photo modal: clicking
a photo from a list shows it as an overlay while the URL changes to /photo/123.
If you share that URL or refresh, you see the full photo page. The same URL
renders different UI based on navigation context. This is achieved with (..) or
(.) prefix notation in the file structure.

Combining both patterns gives you Instagram-style modals: a @modal parallel slot
that shows null by default, and an intercepting route that fills that slot when
you click on a post from the feed.
```

---

## 11. Revision Notes

- Route groups `(name)`: organize code, separate layouts, do NOT affect URLs
- Catch-all `[...slug]`: array of segments, requires at least 1; optional `[[...slug]]` allows 0
- Parallel routes `@slotName`: multiple pages in one layout, each loads independently — always add `default.tsx`
- Intercepting routes `(.)`, `(..)`: same URL shows different UI based on navigation context vs direct access
- Route handlers: named exports `GET`, `POST`, `DELETE` — use `NextRequest`/`NextResponse`
- Next.js 15: `params` and `searchParams` are now `Promise<{...}>` — must be awaited
- `(.)` intercepts same level; `(..)` intercepts one level up; `(...)` intercepts from app root
