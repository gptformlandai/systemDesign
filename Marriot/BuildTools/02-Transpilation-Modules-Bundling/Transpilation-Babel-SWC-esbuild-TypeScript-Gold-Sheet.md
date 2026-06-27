# Transpilation: Babel, SWC, esbuild, TypeScript Gold Sheet

> Topic: JSX -> JavaScript, TypeScript -> JavaScript, browser compatibility, polyfills.

---

## 1. Intuition

Transpilation is translation between JavaScript-like languages or JavaScript versions. You write expressive modern code; the transpiler rewrites it into code your target runtime can parse.

Beginner version:

> Transpilation changes the shape of code without changing what the code is supposed to do.

---

## 2. Definition

- Definition: Transpilation converts source syntax from one version or dialect into another executable JavaScript form.
- Category: Compile-time transform.
- Core idea: Parse source -> create AST -> transform AST -> print output code.

---

## 3. Internal Flow

```txt
React / TS source
        |
        v
Parser
  creates AST
        |
        v
Transformer
  JSX transform
  TS type removal
  syntax lowering
        |
        v
Code generator
  emits JavaScript
        |
        v
Source map
  maps generated code back to original source
```

AST means Abstract Syntax Tree. It is a structured representation of code.

---

## 4. JSX To JavaScript

Input:

```tsx
export function Button() {
  return <button className="primary">Save</button>;
}
```

Possible modern React output shape:

```js
import { jsx as _jsx } from 'react/jsx-runtime';

export function Button() {
  return _jsx('button', {
    className: 'primary',
    children: 'Save',
  });
}
```

Older React output shape:

```js
export function Button() {
  return React.createElement(
    'button',
    { className: 'primary' },
    'Save'
  );
}
```

Interview point:

> JSX is syntax sugar. It must be converted because JavaScript engines do not understand JSX tags natively.

---

## 5. TypeScript Compilation

Input:

```ts
type User = {
  id: string;
  name: string;
};

export function formatUser(user: User): string {
  return `${user.id}: ${user.name}`;
}
```

Output:

```js
export function formatUser(user) {
  return `${user.id}: ${user.name}`;
}
```

The type information helps the developer and compiler, but it usually does not exist at runtime.

Important distinction:

| Tool | Type Checks? | Emits JS? |
|---|---:|---:|
| `tsc` | Yes | Yes |
| Babel TypeScript preset | No full type check | Yes |
| SWC TypeScript transform | No full type check | Yes |
| esbuild TypeScript transform | No full type check | Yes |

Common production pattern:

```txt
tsc --noEmit
  -> type checking

bundler transform
  -> fast JS output
```

---

## 6. Tool Comparison: Babel, SWC, And esbuild

| Tool | Written In | Strength | Common Use |
|---|---|---|---|
| Babel | JavaScript | Plugin ecosystem and syntax transforms | React apps, custom transforms, legacy compatibility |
| SWC | Rust | Fast compile/transforms | Next.js compiler, high-speed builds |
| esbuild | Go | Extremely fast transform/bundle/minify | Vite, tooling, fast builds |

Mental models:

- Babel: highly flexible transform framework.
- SWC: fast compiler platform.
- esbuild: speed-first build primitive.

---

## 7. Polyfills And Browser Compatibility

Transpilation changes syntax. Polyfills add missing runtime APIs.

Syntax example:

```js
const value = user?.profile?.name;
```

Can be transpiled into older JS.

Runtime API example:

```js
const result = await Promise.any(tasks);
```

If a browser does not implement `Promise.any`, syntax transpilation alone does not create that API. You need a polyfill.

```txt
Modern feature problem
        |
        +-- Syntax unsupported?
        |     -> transpile
        |
        +-- API missing?
              -> polyfill
```

---

## 8. Browser Targets

Build tools use target settings to decide what to transform.

Examples:

```json
{
  "browserslist": [
    "last 2 Chrome versions",
    "last 2 Safari versions",
    "not dead"
  ]
}
```

For a modern internal dashboard, you may target recent browsers and ship less transformed code.

For a public banking site, you may support more browsers and ship more compatibility code.

---

## 9. React, Next.js, React Native Notes

### React Web

Vite, Webpack, and Parcel transform JSX and TypeScript before execution.

### Next.js

Next.js uses a framework compiler pipeline, with SWC important in modern Next.js compilation. You usually do not configure Babel unless you need custom behavior.

### React Native

Metro transforms JavaScript for native targets and may use Babel transforms. The output runs in a JavaScript runtime such as Hermes.

React Native also needs platform-specific resolution:

```txt
Button.ios.tsx
Button.android.tsx
Button.tsx
```

The resolver decides which file is correct for the platform.

---

## 10. Real-World Example

You write:

```tsx
const total = cart.items?.reduce((sum, item) => sum + item.price, 0) ?? 0;

export function CartTotal() {
  return <Text>Total: {total}</Text>;
}
```

The pipeline may:

- Convert JSX to JavaScript.
- Remove TypeScript types if present.
- Lower optional chaining depending on target.
- Preserve modern syntax if target supports it.
- Generate a sourcemap so errors point back to `.tsx`.

---

## 11. Common Mistakes

### Mistake: Thinking TypeScript transform equals type safety

- Why wrong: Babel, SWC, and esbuild can strip types without checking them.
- Better approach: Run `tsc --noEmit` in CI.

### Mistake: Confusing transpilation with polyfilling

- Why wrong: Transpilation changes syntax; it does not invent runtime APIs.
- Better approach: Use target-aware polyfills when old environments require missing APIs.

### Mistake: Over-transpiling modern apps

- Why wrong: Too much legacy output increases bundle size and can reduce performance.
- Better approach: Choose browser targets based on real user analytics.

### Mistake: Custom Babel config in framework apps without need

- Why wrong: It can disable optimized framework compiler paths.
- Better approach: Use framework defaults unless a concrete transform requirement exists.

---

## 12. Trade-Offs

| Choice | Benefit | Cost |
|---|---|---|
| Babel | Maximum plugin flexibility | Slower than native compilers |
| SWC | Very fast framework-level transforms | Smaller plugin ecosystem than Babel |
| esbuild | Extremely fast transforms | Some transform semantics are intentionally simpler |
| Legacy browser support | More compatible app | Larger and slower output |
| Modern-only target | Smaller output | Excludes older environments |

---

## 13. Interview Insight

Strong answer:

> Transpilation parses source into an AST, transforms unsupported syntax like JSX or TypeScript, and emits JavaScript plus sourcemaps. It is different from bundling, which decides how modules are connected and emitted as chunks. It is also different from polyfilling, which adds missing runtime APIs.

Follow-up trap:

> If esbuild can compile TypeScript, why run `tsc`?

Good answer:

> esbuild can remove TypeScript syntax quickly, but it does not replace full TypeScript type checking. In serious projects, we commonly use esbuild or SWC for fast emit and `tsc --noEmit` for correctness.

---

## 14. Mini Pipeline

```txt
Button.tsx
  -> parse TSX
  -> remove type annotations
  -> transform JSX
  -> lower syntax based on target
  -> emit Button.js
  -> emit Button.js.map
```

---

## 15. Revision Notes

- One-line summary: Transpilation converts modern or extended syntax into executable JavaScript.
- Three keywords: AST, syntax, sourcemap.
- One interview trap: Type stripping is not the same as type checking.
- Memory trick: Transpiler is the translator; bundler is the packager.
