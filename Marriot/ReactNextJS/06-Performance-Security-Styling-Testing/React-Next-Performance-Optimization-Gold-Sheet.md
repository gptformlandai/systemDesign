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

