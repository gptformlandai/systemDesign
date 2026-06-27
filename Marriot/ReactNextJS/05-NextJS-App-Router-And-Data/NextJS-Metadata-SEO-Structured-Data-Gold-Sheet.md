# Next.js Metadata, SEO, and Structured Data — Gold Sheet

> Track File #31 of 40 · Group 5: Next.js App Router And Data
> Level: intermediate → senior | Metadata API, Open Graph, JSON-LD, sitemap, robots.txt

---

## 1. Intuition

SEO determines whether search engines can find and rank your pages. Next.js provides a type-safe Metadata API that generates `<head>` tags at build or request time — no more managing `<meta>` tags manually.

```text
Good SEO outcome:
  Search engine can crawl your page
  → Understands what the page is about
  → Shows a rich result (title, description, image)
  → Ranks it appropriately
  → Social shares show a preview card

Next.js role:
  Generate correct <meta>, <title>, <link rel="canonical">, Open Graph, and structured data tags
  Control what robots can index
  Provide machine-readable sitemaps
```

---

## 2. Static Metadata

Export a `metadata` constant from any `page.tsx` or `layout.tsx`:

```tsx
// app/page.tsx
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'My App — Dashboard',
  description: 'Manage your account and view analytics.',
  keywords: ['dashboard', 'analytics', 'management'],
  authors: [{ name: 'Acme Inc', url: 'https://acme.com' }],
  
  // Open Graph — controls social share card
  openGraph: {
    title: 'My App Dashboard',
    description: 'Manage your account and view analytics.',
    url: 'https://acme.com',
    siteName: 'Acme App',
    images: [
      {
        url: 'https://acme.com/og-image.png',
        width: 1200,
        height: 630,
        alt: 'Acme App Dashboard',
      },
    ],
    locale: 'en_US',
    type: 'website',
  },
  
  // Twitter Card
  twitter: {
    card: 'summary_large_image',
    title: 'My App Dashboard',
    description: 'Manage your account and view analytics.',
    creator: '@acmeinc',
    images: ['https://acme.com/og-image.png'],
  },
  
  // Canonical URL — prevents duplicate content issues
  alternates: {
    canonical: 'https://acme.com',
    languages: {
      'en-US': 'https://acme.com/en',
      'fr-FR': 'https://acme.com/fr',
    },
  },
  
  // Robots — control indexing
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-image-preview': 'large',
    },
  },
};
```

---

## 3. Dynamic Metadata with generateMetadata

For pages whose metadata depends on fetched data (product pages, blog posts):

```tsx
// app/products/[id]/page.tsx
import type { Metadata, ResolvingMetadata } from 'next';

type Props = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
};

// Next.js calls this at build time (SSG) or request time (SSR)
export async function generateMetadata(
  { params }: Props,
  parent: ResolvingMetadata,  // access parent layout's metadata
): Promise<Metadata> {
  const { id } = await params;
  const product = await fetchProduct(id);
  
  if (!product) {
    return { title: 'Product Not Found' };
  }

  // Access and extend parent Open Graph images
  const previousImages = (await parent).openGraph?.images ?? [];

  return {
    title: `${product.name} — Buy Online | Acme Store`,
    description: product.description.slice(0, 160),  // 160 chars max for meta description
    openGraph: {
      title: product.name,
      description: product.description,
      images: [
        { url: product.imageUrl, width: 1200, height: 630, alt: product.name },
        ...previousImages,
      ],
    },
    alternates: {
      canonical: `https://acme.com/products/${id}`,
    },
  };
}

export default async function ProductPage({ params }: Props) {
  const { id } = await params;
  const product = await fetchProduct(id);
  if (!product) notFound();
  return <ProductDetail product={product} />;
}
```

---

## 4. Title Templates

Avoid repeating " — Site Name" in every page. Use `template` in the root layout:

```tsx
// app/layout.tsx
export const metadata: Metadata = {
  title: {
    template: '%s — Acme Store',  // %s = child page title
    default: 'Acme Store',        // used if no child title is set
  },
  description: 'Shop the best products at Acme Store.',
};

// app/products/page.tsx
export const metadata: Metadata = {
  title: 'Products',  // renders as "Products — Acme Store"
};

// app/products/[id]/page.tsx
export async function generateMetadata(): Promise<Metadata> {
  return { title: product.name };  // renders as "iPhone 16 — Acme Store"
}
```

---

## 5. Structured Data (JSON-LD)

Structured data helps search engines understand your content deeply and enables rich results (star ratings, prices, breadcrumbs in search results):

```tsx
// app/products/[id]/page.tsx
import { Product } from '@/types';

function ProductStructuredData({ product }: { product: Product }) {
  const structuredData = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.name,
    image: product.imageUrl,
    description: product.description,
    sku: product.sku,
    brand: {
      '@type': 'Brand',
      name: product.brand,
    },
    offers: {
      '@type': 'Offer',
      url: `https://acme.com/products/${product.id}`,
      priceCurrency: 'USD',
      price: product.price,
      availability: product.inStock
        ? 'https://schema.org/InStock'
        : 'https://schema.org/OutOfStock',
      seller: { '@type': 'Organization', name: 'Acme Store' },
    },
    aggregateRating: product.reviewCount > 0 ? {
      '@type': 'AggregateRating',
      ratingValue: product.averageRating,
      reviewCount: product.reviewCount,
    } : undefined,
  };

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
    />
  );
}

export default async function ProductPage({ params }: Props) {
  const product = await fetchProduct((await params).id);
  return (
    <>
      <ProductStructuredData product={product} />
      <ProductDetail product={product} />
    </>
  );
}
```

```tsx
// Breadcrumb structured data
const breadcrumbData = {
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: [
    { '@type': 'ListItem', position: 1, name: 'Home', item: 'https://acme.com' },
    { '@type': 'ListItem', position: 2, name: 'Products', item: 'https://acme.com/products' },
    { '@type': 'ListItem', position: 3, name: product.name },
  ],
};

// Article/Blog structured data
const articleData = {
  '@context': 'https://schema.org',
  '@type': 'Article',
  headline: post.title,
  datePublished: post.publishedAt,
  dateModified: post.updatedAt,
  author: { '@type': 'Person', name: post.author.name },
  image: post.heroImage,
  publisher: {
    '@type': 'Organization',
    name: 'Acme Blog',
    logo: { '@type': 'ImageObject', url: 'https://acme.com/logo.png' },
  },
};
```

---

## 6. Sitemap

Next.js generates XML sitemaps from a `sitemap.ts` file:

```tsx
// app/sitemap.ts
import type { MetadataRoute } from 'next';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  // Fetch all dynamic routes
  const products = await getAllProducts();
  const posts = await getAllPosts();

  const productUrls = products.map(product => ({
    url: `https://acme.com/products/${product.id}`,
    lastModified: product.updatedAt,
    changeFrequency: 'weekly' as const,
    priority: 0.8,
  }));

  const postUrls = posts.map(post => ({
    url: `https://acme.com/blog/${post.slug}`,
    lastModified: post.publishedAt,
    changeFrequency: 'monthly' as const,
    priority: 0.6,
  }));

  return [
    // Static routes
    { url: 'https://acme.com', lastModified: new Date(), changeFrequency: 'daily', priority: 1 },
    { url: 'https://acme.com/products', lastModified: new Date(), changeFrequency: 'daily', priority: 0.9 },
    { url: 'https://acme.com/blog', lastModified: new Date(), changeFrequency: 'weekly', priority: 0.7 },
    // Dynamic routes
    ...productUrls,
    ...postUrls,
  ];
}
```

For very large sites (100k+ pages), use multiple sitemaps:
```tsx
// app/sitemap/[id]/route.ts — generate chunked sitemaps
// app/sitemap.ts — return sitemap index pointing to chunks
```

---

## 7. Robots.txt

```tsx
// app/robots.ts
import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',         // all crawlers
        allow: '/',
        disallow: ['/api/', '/admin/', '/private/', '/_next/'],
      },
      {
        userAgent: 'Googlebot',
        allow: '/',
        disallow: '/admin/',
      },
    ],
    sitemap: 'https://acme.com/sitemap.xml',
    host: 'https://acme.com',
  };
}
```

---

## 8. Social Media Preview Testing

```text
Test your Open Graph and Twitter Card meta tags:

Facebook/LinkedIn OG debugger: https://developers.facebook.com/tools/debug/
Twitter Card Validator: https://cards-dev.twitter.com/validator
Google Rich Results Test: https://search.google.com/test/rich-results

Common issues:
  - Image must be > 200x200 pixels (ideal: 1200x630)
  - Image URL must be absolute (https://), not relative
  - Title max ~70 characters, description max ~200 characters
  - og:url must match canonical URL exactly
```

---

## 9. Dynamic OG Images (Next.js ImageResponse)

```tsx
// app/og/route.tsx — generate dynamic social images
import { ImageResponse } from 'next/og';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const title = searchParams.get('title') ?? 'Default Title';
  const author = searchParams.get('author') ?? 'Acme';

  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'linear-gradient(135deg, #0f172a, #1e40af)',
          padding: 60,
        }}
      >
        <h1 style={{ color: 'white', fontSize: 60, fontWeight: 700, textAlign: 'center' }}>
          {title}
        </h1>
        <p style={{ color: '#94a3b8', fontSize: 30 }}>{author}</p>
      </div>
    ),
    { width: 1200, height: 630 },
  );
}

// Reference in generateMetadata
export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const post = await fetchPost((await params).slug);
  return {
    openGraph: {
      images: [{
        url: `/og?title=${encodeURIComponent(post.title)}&author=${encodeURIComponent(post.author)}`,
        width: 1200,
        height: 630,
      }],
    },
  };
}
```

---

## 10. Core Web Vitals — SEO Impact

Google uses Core Web Vitals as a ranking signal. Key metrics:

| Metric | Measures | Good | Needs Work | Poor |
|---|---|---|---|---|
| LCP (Largest Contentful Paint) | Loading speed | < 2.5s | 2.5-4s | > 4s |
| INP (Interaction to Next Paint) | Responsiveness | < 200ms | 200-500ms | > 500ms |
| CLS (Cumulative Layout Shift) | Visual stability | < 0.1 | 0.1-0.25 | > 0.25 |

```tsx
// Common LCP improvement: prioritize above-the-fold image
import Image from 'next/image';

// Add priority to hero image — tells Next.js to preload it
<Image
  src="/hero.jpg"
  alt="Hero image"
  width={1200}
  height={600}
  priority  // adds <link rel="preload"> in <head>
/>

// CLS improvement: always specify image dimensions
// Without dimensions: image loads → layout shifts
// With dimensions: browser reserves space before image loads

// INP improvement: avoid heavy synchronous JS on interaction
// Use startTransition for non-urgent state updates
// Debounce expensive computations
```

---

## 11. Common Mistakes

| Mistake | Impact | Fix |
|---|---|---|
| Relative URLs in og:image | Social preview broken | Always use absolute URLs |
| Same title on all pages | Poor SEO differentiation | Unique, descriptive title per page |
| No canonical URL | Duplicate content penalty | Set `alternates.canonical` |
| Blocking robots from JS files | Googlebot cannot render | Only block truly private paths |
| generateMetadata making redundant DB call | Double fetch | Deduplicate with React `cache()` |
| `dangerouslySetInnerHTML` with user data in JSON-LD | XSS risk | Sanitize or use `JSON.stringify` with a replacer |

```tsx
// Deduplicate DB calls with React cache()
import { cache } from 'react';

const getProduct = cache(async (id: string) => {
  return db.product.findUnique({ where: { id } });
});

// Called once in generateMetadata, once in the page component —
// React cache() ensures only one DB query happens
export async function generateMetadata({ params }: Props) {
  const product = await getProduct((await params).id);  // cached
  return { title: product?.name };
}

export default async function ProductPage({ params }: Props) {
  const product = await getProduct((await params).id);  // returns cached result
  return <ProductDetail product={product} />;
}
```

---

## 12. Strong Interview Answer

**Q: How do you implement SEO in a Next.js app?**

```text
Next.js provides a Metadata API that generates head tags at build or request time.
I export a static `metadata` object for pages with fixed SEO, and use
`generateMetadata()` for dynamic pages like products or blog posts where title and
description depend on fetched data. I use the title template feature in the root
layout to avoid repeating the site name on every page.

For social sharing, I populate the openGraph and twitter fields with absolute image
URLs at 1200x630. For SEO-heavy pages like product listings, I add JSON-LD
structured data for rich results in Google.

I also generate sitemap.ts and robots.ts files so crawlers have a machine-readable
view of the site structure. The key performance aspect is using `priority` on LCP
images and correct image dimensions to avoid layout shift, since Core Web Vitals
affect search ranking directly.
```

---

## 13. Revision Notes

- Static metadata: `export const metadata: Metadata = {...}` in `page.tsx` or `layout.tsx`
- Dynamic metadata: `export async function generateMetadata({ params }): Promise<Metadata>`
- Title template: `{ template: '%s — Site Name', default: 'Site Name' }` in root layout
- Open Graph image: must be absolute URL (https://), 1200×630, < 8MB
- JSON-LD: `<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }} />`
- `sitemap.ts` exports an async function returning array of `{url, lastModified, changeFrequency, priority}`
- `robots.ts` exports `robots()` returning `{rules, sitemap}`
- Use `React.cache()` to deduplicate DB calls between `generateMetadata` and the page component
- `ImageResponse` in `/og/route.tsx` — generate dynamic social images with JSX
