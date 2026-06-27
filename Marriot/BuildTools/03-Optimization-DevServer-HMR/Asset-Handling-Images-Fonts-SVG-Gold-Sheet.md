# Asset Handling: Images, Fonts, SVG, and Static Files — Gold Sheet

> Topic: how non-JS, non-CSS assets flow through bundlers

---

## 1. Intuition

When you write `import logo from './logo.svg'` in React, the bundler has to decide: copy this file, inline it as a data URL, or transform it into a React component. Each bundler has its own asset pipeline, and understanding it prevents "image not found" bugs in production.

Beginner version:

> Bundlers transform image, font, and SVG imports into URLs or data URIs so the browser can load them from the right path.

---

## 2. Definition

- Definition: Asset handling is the build-time process of resolving, transforming, copying, and hashing non-code files (images, fonts, SVG, JSON, WASM) so they are correctly referenced in the output bundle.
- Category: Build pipeline asset transformation.
- Core idea: Every import — including images — goes through the dependency graph.

---

## 3. The Asset Processing Decision Tree

```
import logo from './logo.svg'
        |
        v
Is there a plugin/loader for this file type? YES →
        |
        v
Is the file small enough to inline?
  YES → base64 data URL embedded in JS/CSS bundle
  NO  → copy to output dir, replace import with hashed URL
        |
        v
Output: '/assets/logo.abc123.svg'
```

---

## 4. Vite Asset Handling

Vite uses Rollup's asset pipeline in production and handles assets natively in dev.

### Image imports

```typescript
// Import as URL string
import logoUrl from './assets/logo.png';
// logoUrl = '/assets/logo.abc123.png' (production)
// logoUrl = '/src/assets/logo.png' (dev — no hashing)

function Header() {
  return <img src={logoUrl} alt="Logo" />;
}
```

### Inline threshold — small files become data URIs

```typescript
// vite.config.ts
export default defineConfig({
  build: {
    assetsInlineLimit: 4096,  // default: 4KB — files smaller than this are inlined as base64
  },
});
```

```
logo-small.png (2KB) → data:image/png;base64,iVBOR... (inlined in JS)
logo-large.png (50KB) → /assets/logo-large.abc123.png (separate file)
```

### Raw file content

```typescript
import shaderSource from './shader.glsl?raw';  // import as string
```

### URL without processing

```typescript
import workerUrl from './heavy-worker.js?url';  // just the URL, no bundling
```

### Public folder — bypasses the build pipeline

Files in `public/` are copied to the root of `dist/` as-is. No hashing. No processing. Reference with absolute paths:

```typescript
// public/robots.txt → dist/robots.txt
// public/favicon.ico → dist/favicon.ico

// Reference from code (NOT via import — just use the path):
<link rel="icon" href="/favicon.ico" />
```

**When to use `public/`:** Files that need stable, unhashed URLs (robots.txt, favicon, site verification files).

---

## 5. Webpack Asset Handling (Webpack 5+)

Webpack 5 introduced **Asset Modules** — no more `file-loader`, `url-loader`, or `raw-loader` needed.

```javascript
// webpack.config.js
module.exports = {
  module: {
    rules: [
      {
        test: /\.(png|jpg|jpeg|gif|webp)$/i,
        type: 'asset',              // auto: inline if < 8KB, resource if larger
      },
      {
        test: /\.(png|jpg|jpeg|gif|webp)$/i,
        type: 'asset/resource',     // always copy to output, emit URL
      },
      {
        test: /\.(png|jpg|jpeg|gif|webp)$/i,
        type: 'asset/inline',       // always base64 inline
      },
      {
        test: /\.(woff|woff2|eot|ttf|otf)$/i,
        type: 'asset/resource',
      },
      {
        test: /\.txt$/,
        type: 'asset/source',       // import as string (like ?raw in Vite)
      },
    ],
  },
  output: {
    assetModuleFilename: 'assets/[name].[contenthash][ext]',
  },
};
```

Asset type reference:

| Type | Behavior |
|---|---|
| `asset/resource` | Emits a file, import resolves to URL |
| `asset/inline` | Base64 data URI |
| `asset/source` | Import resolves to file content string |
| `asset` | Auto: inline if < `maxSize` (default 8KB), else resource |

---

## 6. SVG Handling — Three Patterns

SVG needs special handling because it can be used as an image URL, inlined as HTML, or imported as a React component.

### Pattern 1: SVG as URL (both Vite and Webpack)

```typescript
import iconUrl from './icon.svg';
// iconUrl = '/assets/icon.abc123.svg'
<img src={iconUrl} alt="icon" />
```

### Pattern 2: SVG as React Component (SVGR)

```bash
npm install -D @svgr/webpack  # webpack
npm install -D vite-plugin-svgr  # vite
```

```typescript
// Vite setup
import svgr from 'vite-plugin-svgr';
export default defineConfig({ plugins: [react(), svgr()] });

// Import as React component
import { ReactComponent as Logo } from './logo.svg';
// OR with vite-plugin-svgr default config:
import Logo from './logo.svg?react';

function Header() {
  return <Logo className="logo" aria-label="Company Logo" />;
}
```

```javascript
// Webpack — handle both URL and component patterns
{
  test: /\.svg$/i,
  issuer: /\.[jt]sx?$/,
  use: ['@svgr/webpack'],         // SVG → React component when imported from JS/TS
}
```

**Why SVG as component:** You can style parts of the SVG with CSS, animate paths, and apply `currentColor` inheritance from parent text color. Cannot do this with `<img src>`.

### Pattern 3: Inline SVG in CSS

```css
.icon {
  background-image: url('./icon.svg');  /* inlined or emitted as URL by CSS processor */
}
```

---

## 7. Font Handling

### Self-hosted fonts (recommended for performance)

```css
/* In CSS — bundler copies the font file and replaces the path */
@font-face {
  font-family: 'Inter';
  src: url('./fonts/Inter-Regular.woff2') format('woff2');
  font-weight: 400;
  font-display: swap;  /* prevents FOIT — text renders in fallback font while Inter loads */
}
```

Webpack and Vite automatically process the `url()` inside CSS — the font file is copied to output and the path is replaced with the hashed URL.

### Next.js built-in font optimization

```typescript
// app/layout.tsx — Next.js downloads font at build time, self-hosts it
import { Inter } from 'next/font/google';

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html className={inter.variable}>
      <body>{children}</body>
    </html>
  );
}
```

**What `next/font` does at build time:**
1. Downloads the font files from Google Fonts during `next build`
2. Serves them from your own domain (no external request to `fonts.googleapis.com`)
3. Adds `<link rel="preload">` automatically
4. Zero layout shift with `size-adjust` CSS

---

## 8. JSON and WASM Imports

```typescript
// JSON — supported natively by Vite and Webpack 5
import config from './config.json';
console.log(config.apiUrl);  // fully typed if tsconfig has "resolveJsonModule": true

// WASM — Vite 4+
// vite.config.ts
import wasmPlugin from 'vite-plugin-wasm';
export default defineConfig({ plugins: [wasmPlugin()] });

import init from './lib.wasm?init';
const wasmInstance = await init();
```

---

## 9. Asset Output and Caching Strategy

```javascript
// Vite — configure asset output filenames
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        assetFileNames: 'assets/[name].[hash][extname]',  // images, fonts, etc.
        chunkFileNames: 'assets/[name].[hash].js',
        entryFileNames: 'assets/[name].[hash].js',
      },
    },
  },
});

// Webpack — configure output filenames
output: {
  filename: '[name].[contenthash].js',
  assetModuleFilename: 'assets/[name].[contenthash][ext]',
  path: path.resolve(__dirname, 'dist'),
}
```

Content hashing enables **long-term caching**:
```
logo.abc123.png → CDN caches for 1 year
When logo changes → hash changes → new URL → cache busted automatically
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Importing images from `public/` with `import` | Works in dev, fails in prod or gets double-processed | Use absolute path `/image.png` for public assets |
| SVG imported as URL but expected as component | Renders as `<img>`, can't style internals | Use SVGR (`?react` in Vite or `@svgr/webpack`) |
| No `font-display: swap` on custom fonts | Flash of invisible text (FOIT) during font load | Always set `font-display: swap` |
| Large image not optimized | Poor LCP performance | Use `next/image` or add an image optimization loader |
| Dynamic asset paths | Bundler can't resolve at build time | Use static import or `new URL('./assets/'+name, import.meta.url)` |
| Forgetting `resolveJsonModule` in tsconfig | `import config from './config.json'` throws TS error | Add `"resolveJsonModule": true` to tsconfig |

### Dynamic asset pattern (Vite)

```typescript
// For truly dynamic asset paths, use import.meta.url
function getImageUrl(name: string) {
  return new URL(`./assets/${name}.png`, import.meta.url).href;
}
// Vite can statically analyze this pattern and include matched assets
```

---

## 11. Interview Insight

Strong answer:

> Asset handling in bundlers works by running non-JS files through the dependency graph. Images and fonts below a size threshold are inlined as base64 data URIs; larger ones are copied to the output directory with content-hashed filenames for long-term caching. SVG is a special case — it can be imported as a URL for an `<img>` tag or transformed into a React component using SVGR, which lets you style SVG internals with CSS. The public folder bypasses the pipeline for files that need stable, unhashed URLs.

Follow-up trap:

> Why are small images sometimes embedded in the JS bundle?

Good answer:

> Inlining small images as base64 eliminates an extra HTTP request. For tiny icons under 4–8KB, the cost of a separate network round trip exceeds the cost of a slightly larger JS file. For larger images, a separate cacheable file is more efficient.

---

## 12. Revision Notes

- One-line summary: Bundlers hash, copy, or inline assets — the choice depends on file size and use case.
- Three keywords: inline, hash, public.
- One interview trap: Public folder assets bypass the build pipeline — no hashing.
- Memory trick: Small asset = inline; large asset = separate file with content hash.
