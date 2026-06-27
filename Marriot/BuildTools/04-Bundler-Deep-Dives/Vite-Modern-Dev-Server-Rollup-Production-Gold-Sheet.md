# Vite: Modern Dev Server and Production Bundling Gold Sheet

> Topic: native ES modules, on-demand serving, esbuild usage, and production bundling.

---

## 1. Intuition

Vite became popular because it stopped treating development like production. Instead of building the whole app before the browser can start, it lets the browser request ES modules and transforms files on demand.

Beginner version:

> Vite is fast in dev because it serves only what the browser asks for.

---

## 2. Definition

- Definition: Vite is a modern frontend build tool that uses native ES module serving in development and optimized bundling for production.
- Category: Dev-server-first build tool.
- Core idea: Fast dev feedback, optimized production output.

---

## 3. Vite Development Pipeline

```txt
npm run dev
   |
   v
Vite starts dev server
   |
   v
Browser requests index.html
   |
   v
Browser requests ESM imports
   |
   v
Vite transforms requested modules on demand
   |
   v
HMR updates changed modules
```

Key idea:

```txt
Webpack-style old dev
  -> build graph first
  -> serve bundle

Vite-style modern dev
  -> serve app immediately
  -> transform modules as requested
```

---

## 4. Native ES Modules

Browser can load:

```html
<script type="module" src="/src/main.tsx"></script>
```

The browser sees imports and requests them:

```ts
import App from './App';
import './style.css';
```

Vite intercepts those requests and transforms TypeScript, JSX, CSS, and other supported imports.

---

## 5. Dependency Pre-Bundling

Dependencies in `node_modules` can contain many small modules or CommonJS packages. Vite optimizes dependencies so dev server loading stays fast and compatible.

Pipeline:

```txt
scan imports
  -> identify dependencies
  -> convert incompatible formats when needed
  -> pre-bundle dependency graph
  -> serve optimized dependency module
```

Why:

- Convert CommonJS/UMD dependencies to ESM-compatible dev modules.
- Avoid the browser making hundreds of requests for deeply nested dependency files.
- Improve cold start and repeated dev loads.

Ecosystem note:

> The classic Vite mental model is native ESM dev plus esbuild-powered speed plus Rollup production bundling. Newer Vite versions may evolve internal implementation details, but the architectural lesson remains: separate fast dev serving from optimized production output.

---

## 6. Vite Production Build

```txt
npm run build
   |
   v
analyze full graph
   |
   v
bundle modules
   |
   v
tree shake
   |
   v
split chunks
   |
   v
minify
   |
   v
emit dist assets
```

Production build still matters because users should not load unoptimized dev modules.

---

## 7. Vite Config Example

```ts
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: true,
    rollupOptions: {
      output: {
        manualChunks: {
          charts: ['recharts'],
        },
      },
    },
  },
});
```

This config:

- Enables React transforms.
- Emits production sourcemaps.
- Splits chart library code into a named chunk.

---

## 8. HMR In Vite

```txt
file changed
  -> Vite invalidates module
  -> transform changed module
  -> send WebSocket update
  -> browser imports updated module
  -> React Fast Refresh applies update
```

Vite's HMR feels fast because the update often touches only a small module instead of rebuilding a full app bundle.

---

## 9. React, Next.js, React Native Notes

### React Web

Vite is a strong default for new React SPA projects.

### Next.js

Do not replace Next.js with Vite for standard Next.js apps. Next has its own server rendering, routing, compiler, and bundling needs.

### React Native

React Native uses Metro. Vite can matter for React Native Web, Storybook, or web companion apps, but not as the default native bundler.

---

## 10. Real-World Example

Internal admin app:

- Many developers.
- Fast component iteration needed.
- Modern browser baseline.
- Mostly client-side routes.

Vite is a strong fit:

```txt
fast dev startup
  -> productive iteration

production build
  -> optimized dist assets

plugins
  -> React, SVGR, env handling, testing integration
```

---

## 11. Common Mistakes

### Mistake: Thinking Vite skips bundling entirely

- Why wrong: Vite serves modules during dev but still bundles for production.
- Better approach: distinguish dev architecture from production build.

### Mistake: Assuming all Node packages work in browser dev

- Why wrong: Some packages rely on Node APIs.
- Better approach: use browser-compatible packages or server boundaries.

### Mistake: Overusing manual chunks

- Why wrong: Manual chunking can create stale or inefficient chunk graphs.
- Better approach: let defaults work first, then tune based on bundle analysis.

### Mistake: Expecting Vite to solve framework concerns

- Why wrong: Next.js SSR/RSC and React Native platform bundling need their own tooling.
- Better approach: choose Vite for the right app shape.

---

## 12. Trade-Offs

| Vite Choice | Gain | Cost |
|---|---|---|
| Native ESM dev | Fast startup | Modern dev browser assumptions |
| Dependency optimization | Faster dev requests | Cache invalidation complexity |
| Production bundling | Optimized user output | Separate build step |
| Plugin ecosystem | Flexible integration | Plugin compatibility must be checked |

---

## 13. Interview Insight

Strong answer:

> Vite is fast because it separates development and production. In dev, it uses native ES modules and transforms source on demand. Dependencies are optimized for browser loading. In production, it still performs a full optimized bundle with tree shaking, minification, code splitting, and asset hashing.

Follow-up trap:

> If Vite serves ESM directly, why optimize dependencies?

Good answer:

> Some dependencies are CommonJS or contain many tiny internal modules. Pre-bundling makes them ESM-compatible for the browser and reduces request overhead during development.

---

## 14. Full vite.config.ts Patterns

```typescript
// vite.config.ts — comprehensive config reference
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');  // load all env (not just VITE_)

  return {
    plugins: [
      react(),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        '@components': path.resolve(__dirname, './src/components'),
      },
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/api/, ''),
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: true,
      rollupOptions: {
        output: {
          manualChunks: {
            'react-vendor': ['react', 'react-dom'],
            'router': ['react-router-dom'],
          },
        },
      },
    },
    define: {
      // inject non-VITE_ variables to client (careful — only public values)
      __APP_VERSION__: JSON.stringify(process.env.npm_package_version),
    },
  };
});
```

---

## 15. Library Mode

Build a reusable library instead of an app:

```typescript
// vite.config.ts — library mode
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: path.resolve(__dirname, 'src/index.ts'),
      name: 'MyLib',                    // global var name for UMD builds
      formats: ['es', 'cjs', 'umd'],
      fileName: (format) => `my-lib.${format}.js`,
    },
    rollupOptions: {
      external: ['react', 'react-dom'],  // don't bundle — consumer provides them
      output: {
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
        },
      },
    },
  },
});
```

---

## 16. SSR Mode

```typescript
// vite.config.ts for SSR
export default defineConfig({
  build: {
    ssr: true,                          // build for Node.js runtime
    rollupOptions: {
      input: 'src/entry-server.tsx',
    },
  },
});
```

```typescript
// vite.ssrLoadModule API (for custom server like Express)
const vite = await createServer({ server: { middlewareMode: true } });
const module = await vite.ssrLoadModule('/src/entry-server.tsx');
```

---

## 17. Plugin Authoring Basics

```typescript
// Custom Vite plugin
import type { Plugin } from 'vite';

function myPlugin(): Plugin {
  return {
    name: 'my-plugin',
    // Transform hook: runs on every file import
    transform(code, id) {
      if (!id.endsWith('.myext')) return null;
      return {
        code: code.replace('__VERSION__', '1.0.0'),
        map: null,
      };
    },
    // Configure dev server hook
    configureServer(server) {
      server.middlewares.use('/custom-path', (req, res) => {
        res.end('custom response');
      });
    },
    // Build output manipulation
    generateBundle(options, bundle) {
      for (const [fileName, chunk] of Object.entries(bundle)) {
        if (chunk.type === 'chunk') {
          console.log(`Chunk: ${fileName} (${chunk.code.length} bytes)`);
        }
      }
    },
  };
}
```

---

## 18. Revision Notes

- One-line summary: Vite serves ESM on demand in dev and emits optimized production bundles.
- Three keywords: ESM, on-demand, optimize.
- One interview trap: Vite is not production-unbundled by default.
- Memory trick: Vite opens the restaurant before cooking the entire menu.
