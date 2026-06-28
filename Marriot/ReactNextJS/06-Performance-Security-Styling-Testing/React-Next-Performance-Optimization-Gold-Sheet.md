# React + Next.js Performance Optimization - Gold Sheet

> Track File #15 of 24 - Group 6: Performance, Security, Styling, And Testing
> Covers: code splitting, lazy loading, tree shaking, bundle analysis, image optimization, Web Vitals

---

## 1. Intuition

Performance is user-perceived speed plus system efficiency.

```text
network -> server -> HTML -> CSS/JS -> hydration -> interaction -> runtime updates
```

Optimize the bottleneck you measured.

---

## 2. Web Vitals

| Metric | Meaning |
|---|---|
| LCP | Largest Contentful Paint, loading performance |
| CLS | Cumulative Layout Shift, visual stability |
| INP | Interaction to Next Paint, interaction responsiveness |
| FCP | First Contentful Paint |
| TTFB | Time to First Byte |

Note:
FID has historically appeared in interview lists, but INP is the modern Core Web Vitals responsiveness metric. Know both terms.

---

## 3. Code Splitting And Lazy Loading

Split at:
- route boundaries
- heavy widgets
- admin-only tools
- chart/editor libraries

```tsx
const Chart = dynamic(() => import('./Chart'), {
  loading: () => <ChartSkeleton />,
});
```

Trade-off:
Code splitting reduces initial JS but can create later loading waterfalls.

---

## 4. Tree Shaking

Tree shaking removes unused exports when bundler and package format allow it.

Good:

```ts
import {format} from 'date-fns';
```

Risky:

```ts
import _ from 'lodash';
```

Production check:
Use bundle analyzer to verify assumptions.

---

## 5. Bundle Analysis

Look for:
- huge dependencies
- duplicate versions
- client-only packages imported into server paths
- unnecessary `use client` boundaries
- chart/editor libraries in main bundle
- icon library imports that pull too much

Next.js app insight:
Pushing client boundaries down can reduce shipped JavaScript.

---

## 6. Image Optimization

Use:
- correct dimensions
- responsive sizes
- modern formats
- lazy loading below fold
- priority for hero/LCP image
- CDN image resizing

Next example:

```tsx
import Image from 'next/image';

<Image
  src="/hero.jpg"
  alt="Product dashboard"
  width={1200}
  height={630}
  priority
/>;
```

Avoid:
- unbounded image dimensions
- layout shift from missing sizes
- shipping massive original images

---

## 7. Runtime React Performance

Optimize:
- avoid unnecessary state high in tree
- memoize expensive calculations
- virtualize huge lists
- split components by update frequency
- avoid expensive effects
- use transitions for non-urgent updates

Do not:
- `memo` everything blindly
- use effects for derived state
- optimize before measuring

### Why Components Re-render

A component re-renders when:
1. Its own state changes
2. A parent re-renders (unless wrapped with `React.memo`)
3. A context it subscribes to changes
4. A key prop changes

```tsx
// React.memo — skip re-render if props are shallowly equal
const ProductCard = React.memo(function ProductCard({product}: Props) {
  return <div>{product.name}</div>;
});

// useMemo — memoize expensive computation
const sortedItems = useMemo(
  () => items.sort((a, b) => a.price - b.price),
  [items]
);

// useCallback — stable function reference for memoized children
const handleSelect = useCallback((id: string) => {
  setSelectedId(id);
}, []); // stable — no dependencies change
```

Trap: `React.memo` does a shallow comparison. Object/array props created inline defeat it:

```tsx
// WRONG — new object every parent render defeats memo
<ProductCard style={{margin: 0}} />

// RIGHT — stable reference
const style = useMemo(() => ({margin: 0}), []);
<ProductCard style={style} />
```

### List Virtualization

Only render rows visible in the viewport:

```tsx
import {useVirtualizer} from '@tanstack/react-virtual';

function VirtualList({items}: {items: Item[]}) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 60,
  });

  return (
    <div ref={parentRef} style={{height: '600px', overflow: 'auto'}}>
      <div style={{height: `${virtualizer.getTotalSize()}px`, position: 'relative'}}>
        {virtualizer.getVirtualItems().map(virtualItem => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              transform: `translateY(${virtualItem.start}px)`,
              height: `${virtualItem.size}px`,
            }}
          >
            <ItemRow item={items[virtualItem.index]} />
          </div>
        ))}
      </div>
    </div>
  );
}
```

Use `@tanstack/react-virtual` or `react-window` for 1000+ row tables and infinite feeds.

---

## 7b. Font Optimization with next/font

`next/font` downloads fonts at build time, hosts them self-served, and injects optimal `<link>` preloads:

```ts
// app/layout.tsx
import {Inter, Roboto_Mono} from 'next/font/google';

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
});

const robotoMono = Roboto_Mono({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-roboto-mono',
});

export default function RootLayout({children}: {children: React.ReactNode}) {
  return (
    <html lang="en" className={`${inter.variable} ${robotoMono.variable}`}>
      <body>{children}</body>
    </html>
  );
}
```

Benefits:
- Zero layout shift (CLS=0 with `display: swap`)
- Privacy (no requests to Google Fonts at runtime)
- Automatic `size-adjust` for fallback font
- No extra network requests

Local font:

```ts
import localFont from 'next/font/local';

const brandFont = localFont({
  src: '../public/fonts/brand.woff2',
  variable: '--font-brand',
});
```

---

## 7c. INP — Reducing Interaction Latency

INP (Interaction to Next Paint) measures the worst interaction delay across a session.

Common causes:
- Long main-thread tasks blocking the event loop
- Synchronous state updates causing large reconciliation
- Heavy `onClick` handlers without yielding

Fixes:

```tsx
// Use useTransition to keep the input responsive while filtering
const [isPending, startTransition] = useTransition();
const [query, setQuery] = useState('');
const [filtered, setFiltered] = useState(allItems);

function handleInput(value: string) {
  setQuery(value); // urgent — update input immediately
  startTransition(() => {
    setFiltered(allItems.filter(i => i.name.includes(value))); // non-urgent
  });
}
```

Yield to browser between large tasks:

```ts
async function processLargeDataset(items: Item[]) {
  const results: ProcessedItem[] = [];

  for (let i = 0; i < items.length; i++) {
    results.push(processItem(items[i]));

    // Yield every 50 items so the browser can handle events
    if (i % 50 === 0) {
      await new Promise(resolve => setTimeout(resolve, 0));
    }
  }

  return results;
}
```

---

## 8. Real-World Use Cases

- Landing page LCP: optimize hero image, fonts, CSS, TTFB.
- Dashboard INP: reduce JS work in filters and charts.
- Ecommerce CLS: set image dimensions and reserve skeleton space.
- Admin table: virtualize rows and debounce filters.
- App Router bundle: move interactive child to client, keep parent server.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Optimizing React before images/network | Wrong bottleneck | Measure Web Vitals |
| Client component too high | Ships extra JS | Push boundary down |
| Missing image dimensions | CLS | reserve size |
| Lazy loading LCP image | Slower LCP | priority/preload hero |
| Importing huge libraries casually | Bundle bloat | analyze and replace |

---

## 10. Strong Interview Answer

Question:
How do you improve performance in a React/Next.js app?

Strong answer:

```text
I start with metrics: LCP for loading, CLS for layout stability, INP/FID for
responsiveness, plus bundle size and server latency. In Next.js, I reduce shipped
JavaScript by using Server Components and pushing `use client` boundaries down.
I optimize images, fonts, caching, code splitting, and route-level loading. For
runtime React issues, I profile render cost, move state down, memoize only where
it matters, virtualize large lists, and avoid expensive effects.
```

---

## 11. Revision Notes

- One-line summary: Performance work starts with Web Vitals and measured bottlenecks.
- Three keywords: LCP, bundle, hydration.
- One interview trap: Lazy loading the hero image can hurt LCP.
- One memory trick: Less JS, stable layout, optimized images, measured renders.

