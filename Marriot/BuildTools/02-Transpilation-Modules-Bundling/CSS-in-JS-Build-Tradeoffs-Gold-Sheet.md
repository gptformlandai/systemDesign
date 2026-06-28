# CSS-in-JS vs CSS Modules vs Vanilla Extract — Build Trade-offs Gold Sheet

> **Track**: Frontend Build Tools and Bundling Mastery Track  
> **File**: Phase 2 — Transpilation, Modules, and Bundling  
> **Read after**: CSS-PostCSS-Processing-Build-Pipeline-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Comes Up |
|---|---|---|
| Runtime vs. compile-time CSS generation | ★★★★★ | The central trade-off in all CSS-in-JS interviews |
| styled-components / emotion critical render path cost | ★★★★☆ | LCP impact, SSR serialization cost, hydration |
| Zero-runtime CSS-in-JS: Vanilla Extract, linaria | ★★★★☆ | Modern teams abandoning runtime CSS-in-JS |
| CSS Modules — when and why | ★★★★☆ | Default recommendation at MAANG scale |
| Streaming SSR compatibility | ★★★☆☆ | React 18 streaming breaks runtime CSS-in-JS |
| Bundle size contribution | ★★★☆☆ | Interviewers test awareness of library weight |
| Style extraction and critical CSS | ★★★☆☆ | Performance design at scale |

---

## 2. Intuition

```txt
Plain CSS file
  -> separate file
  -> no JS coupling
  -> global scope problem

CSS Modules
  -> bundler transforms class names to scoped hashes
  -> no runtime overhead
  -> no dynamic styles from props

CSS-in-JS (runtime, e.g. styled-components, emotion)
  -> JS generates styles at runtime
  -> dynamic props work naturally
  -> COSTS: runtime JS execution, larger bundle, SSR complications

Zero-runtime CSS-in-JS (e.g. Vanilla Extract, linaria)
  -> styles written in JS/TS syntax with full type safety
  -> CSS extracted at BUILD TIME, no runtime cost
  -> trade-off: no truly dynamic styles — only static values or CSS custom properties
```

The key question every MAANG interviewer probes: **"When does CSS generation happen: build time or runtime?"**

---

## 3. Mental Model — The Three Camps

```txt
Camp 1 — Build-time only (CSS Modules, Tailwind, plain CSS)
  ✅ Zero JS runtime cost
  ✅ Works with any SSR strategy including streaming
  ❌ Limited or no dynamic styles from props

Camp 2 — Runtime CSS-in-JS (styled-components, emotion)
  ✅ Full dynamic styles from JS props
  ✅ Developer ergonomics, colocation
  ❌ Runtime JS for style generation
  ❌ Incompatible with React 18 streaming SSR
  ❌ Adds 10–45 kB to bundle

Camp 3 — Zero-runtime CSS-in-JS (Vanilla Extract, linaria, Panda CSS)
  ✅ Full TypeScript type safety, theme tokens, variants
  ✅ CSS extracted at build time — no runtime cost
  ✅ Works with streaming SSR
  ❌ No truly dynamic styles — must use CSS custom properties
  ❌ Build-time complexity (Babel/SWC plugin required)
```

---

## 4. CSS Modules — The Baseline

### How It Works

CSS Modules are processed by the bundler (webpack/Vite). Class names are scoped to the file using a hash.

```css
/* Button.module.css */
.button {
  padding: 8px 16px;
  background: blue;
  color: white;
  border-radius: 4px;
}

.button:hover {
  background: darkblue;
}

.destructive {
  background: red;
}
```

```tsx
// Button.tsx
import styles from './Button.module.css';

interface Props {
  variant?: 'default' | 'destructive';
}

export function Button({ variant = 'default', children }: Props) {
  return (
    <button className={`${styles.button} ${variant === 'destructive' ? styles.destructive : ''}`}>
      {children}
    </button>
  );
}
```

### What the Bundler Does

```txt
Input:    .button { color: white; }
Output:   .Button_button__abc12 { color: white; }

The class name is hashed per-file:
  .{filename}_{localName}__{hash}
```

The `import styles` object maps local class names to the hashed output:
```ts
styles.button === "Button_button__abc12"
```

### Build Pipeline

```txt
CSS Modules file
  -> CSS loader (webpack) or Vite's built-in transform
  -> Class names hashed and scoped
  -> CSS extracted to static file at production build
  -> JS module returns { button: "Button_button__abc12" }
```

**Production artifact:** a `.css` file with all scoped rules, loaded as a static asset. No runtime cost.

### When to Use CSS Modules

| Scenario | Use CSS Modules? |
|---|---|
| Large team with strict style isolation | ✅ Yes |
| Server Components in Next.js | ✅ Yes |
| React 18 streaming SSR | ✅ Yes |
| Fully dynamic styles from JS runtime values | ❌ Use inline styles or CSS custom properties instead |
| Design token integration | ✅ Yes (via CSS custom properties) |
| Existing Tailwind project | ❌ Mixed approach gets complex |

---

## 5. Runtime CSS-in-JS — styled-components and emotion

### How styled-components Works

```tsx
import styled from 'styled-components';

// Creates a React component with styles attached
const Button = styled.button<{ variant: 'primary' | 'danger' }>`
  padding: 8px 16px;
  background: ${(props) => props.variant === 'danger' ? 'red' : 'blue'};
  color: white;
  border-radius: 4px;
`;

// Usage — props drive styles at runtime
<Button variant="danger">Delete</Button>
```

### Runtime Mechanism

```txt
Component renders
  -> styled-components generates a CSS class name from a hash of the template literal
  -> Styles for this props combination inserted into a <style> tag in <head>
  -> Component receives the generated class name
  -> On props change, new class generated and injected

Every unique prop combination = a new CSS class injected into the DOM
```

This means **style generation happens during JavaScript execution on every render path where new styles are needed.**

### emotion — the Faster Sibling

```tsx
import { css } from '@emotion/react';

const buttonStyle = (variant: string) => css`
  padding: 8px 16px;
  background: ${variant === 'danger' ? 'red' : 'blue'};
  color: white;
`;

// Or the styled API (same as styled-components)
import styled from '@emotion/styled';
const Button = styled.button<{ variant: string }>`...`;
```

emotion is smaller (~7 kB gzip vs ~12 kB for styled-components) and slightly faster at runtime due to a different style injection strategy. Both share the same fundamental runtime cost.

### SSR with Runtime CSS-in-JS — The Complication

```txt
Server renders HTML
  -> styled-components needs to COLLECT all styles used in the render
  -> Inject them as a <style> tag in the HTML response
  -> Client hydrates and takes over

Problem with React 18 streaming (renderToPipeableStream):
  -> The server streams HTML in chunks as it renders
  -> styled-components cannot know which styles will be needed for chunks yet to render
  -> Styles must be collected for the FULL render before streaming can begin
  -> This breaks streaming entirely or requires a complex flush-per-chunk mechanism

React 18 streaming is incompatible with runtime CSS-in-JS as of 2024-2025.
```

**styled-components v6** and **emotion v12** added experimental streaming support but it remains fragile and is not recommended for production streaming SSR as of 2025.

### Bundle Cost

| Library | Bundle size (gzip) | Runtime CSS generation | SSR streaming |
|---|---|---|---|
| CSS Modules | 0 kB | None | ✅ Full support |
| Tailwind (JIT) | ~5–10 kB utility classes | None | ✅ Full support |
| emotion | ~7 kB | Yes — every render | ⚠️ Complex |
| styled-components | ~12 kB | Yes — every render | ⚠️ Complex |
| Vanilla Extract | 0 kB runtime | None (build time) | ✅ Full support |

---

## 6. Zero-Runtime CSS-in-JS — Vanilla Extract

### Philosophy

Write styles in TypeScript with full type safety, theme contracts, and variant systems — but emit pure CSS at build time. No runtime library in the browser.

### Basic Usage

```ts
// button.css.ts  — this file is NEVER bundled into the JS bundle
import { style, styleVariants } from '@vanilla-extract/css';

// Base styles
export const button = style({
  padding: '8px 16px',
  borderRadius: '4px',
  color: 'white',
  cursor: 'pointer',
});

// Variant styles — each is a separate CSS class
export const variants = styleVariants({
  primary: { background: 'blue' },
  danger:  { background: 'red' },
  ghost:   { background: 'transparent', color: 'blue', border: '1px solid blue' },
});
```

```tsx
// Button.tsx
import { button, variants } from './button.css';

interface Props {
  variant?: keyof typeof variants;
  children: React.ReactNode;
}

export function Button({ variant = 'primary', children }: Props) {
  return (
    <button className={`${button} ${variants[variant]}`}>
      {children}
    </button>
  );
}
```

### Build Pipeline

```txt
button.css.ts (TypeScript file)
  -> Bundler plugin (Vite/webpack plugin for Vanilla Extract)
  -> Executes the .css.ts file at BUILD TIME in Node.js
  -> Collects all style() calls
  -> Generates hashed CSS class names
  -> Emits a static .css file (button.css.ts.vanilla.css)
  -> JS side receives only the class name strings

No style library ships to the browser.
No style generation at runtime.
No SSR complications.
```

### Theme System with Contracts

```ts
// theme.css.ts
import { createGlobalTheme } from '@vanilla-extract/css';

// Declares a theme contract — TypeScript enforces all tokens
export const vars = createGlobalTheme(':root', {
  color: {
    primary:    '#3b82f6',
    danger:     '#ef4444',
    background: '#ffffff',
    text:       '#111827',
  },
  space: {
    sm: '8px',
    md: '16px',
    lg: '24px',
  },
  font: {
    body: 'Inter, sans-serif',
    size: {
      base: '16px',
      lg:   '18px',
    },
  },
});
```

```ts
// button.css.ts — consuming theme tokens
import { style } from '@vanilla-extract/css';
import { vars } from './theme.css';

export const button = style({
  padding: `${vars.space.sm} ${vars.space.md}`,
  fontFamily: vars.font.body,
  fontSize: vars.font.size.base,
  color: vars.color.text,
  background: vars.color.primary,
  // TypeScript error if vars.color.typo — token contracts are enforced
});
```

### The Dynamic Styles Limitation

```ts
// ❌ This is NOT possible in Vanilla Extract — no runtime values
export const dynamicButton = style({
  background: someRuntimeVariable,   // Build-time only — no JS values allowed
});

// ✅ Solution 1: Pre-declare all variants as separate classes
export const variants = styleVariants({
  blue:  { background: 'blue' },
  red:   { background: 'red' },
  green: { background: 'green' },
});

// ✅ Solution 2: Use CSS custom properties for truly dynamic values
export const dynamicColor = style({
  background: 'var(--button-bg)',   // CSS custom property set at runtime
});

// In component:
<button style={{ '--button-bg': userColor } as React.CSSProperties} className={dynamicColor}>
```

---

## 7. linaria — Zero-Runtime Alternative to styled-components Syntax

linaria lets you write styled-components syntax but extracts CSS at build time:

```ts
import { styled } from '@linaria/react';

const Button = styled.button<{ variant: 'primary' | 'danger' }>`
  padding: 8px 16px;
  /* ❌ Cannot use JS runtime values in the template */
  /* ✅ Can use static values and CSS custom properties */
  background: var(--button-bg);
`;

// At build time, linaria statically analyzes the template literal,
// extracts the CSS, and emits a static CSS file.
// Runtime: only the className string remains in the JS bundle.
```

**Limitation:** Template literals with runtime expressions (props callbacks like `${props => props.color}`) cannot be statically extracted — linaria falls back to runtime for those or throws a build error. This is the key difference from styled-components.

---

## 8. Panda CSS — The Modern Zero-Runtime Option

Panda CSS (2023+) targets Next.js App Router and Server Components:

```tsx
import { css } from '../styled-system/css';

// css() returns a string of hashed class names — generated at build time
function Button({ variant }: { variant: 'primary' | 'danger' }) {
  return (
    <button
      className={css({
        padding: '2 4',
        background: variant === 'primary' ? 'blue.500' : 'red.500',
        // Panda statically analyzes and generates all variant combinations
        // at build time — no runtime style generation
      })}
    >
      Click
    </button>
  );
}
```

Panda CSS uses static analysis to extract all possible class combinations at build time — even conditional object props — making it compatible with React Server Components.

---

## 9. Decision Matrix — Choosing the Right Approach

| Requirement | Best Choice | Reason |
|---|---|---|
| New Next.js App Router project (2025+) | CSS Modules or Vanilla Extract or Tailwind | Server Components + streaming compatible |
| Legacy Next.js Pages Router with SSR | CSS Modules | Safe, no SSR complications |
| Strict design token enforcement at scale | Vanilla Extract | TypeScript contract, zero runtime |
| Component library publishing (React, Storybook) | Vanilla Extract or CSS Modules | No runtime dep, consumers can tree-shake |
| Complex theme switching (light/dark/brand) | Vanilla Extract + CSS custom properties | Build-time classes + runtime token swaps |
| Quick prototyping, dev speed priority | styled-components / emotion | Best DX, fast iteration |
| React Native | StyleSheet API or Tamagui | No CSS in RN; Tamagui = zero-runtime |
| Migrating a large codebase away from styled-components | linaria (if template syntax stays) or Vanilla Extract (if team rewrites) | Different migration effort |

**MAANG interview default answer:** For new production React projects using Next.js App Router or any streaming SSR, the recommendation is zero-runtime or build-time CSS (CSS Modules, Vanilla Extract, Tailwind). Reserve runtime CSS-in-JS for legacy codebases where migration cost outweighs the streaming compatibility gain.

---

## 10. Build-Time Mechanics — What the Bundler Does

### CSS Modules Pipeline (Vite)

```txt
import styles from './Button.module.css'
     |
     v
Vite CSS Module transform
     |
     v
Class names hashed: .button -> .Button_button__abc12
     |
     v
CSS injected into the page via <style> in dev
     |
     v (production build)
CSS extracted into static .css file via rollup-plugin-css
     |
     v
JS module exports: { button: "Button_button__abc12" }
```

### Vanilla Extract Pipeline (Vite)

```txt
import { button, variants } from './button.css'
     |
     v
@vanilla-extract/vite-plugin intercepts .css.ts files
     |
     v
Executes the .css.ts file in Node.js (build environment, not browser)
     |
     v
Collects all style() calls → generates CSS rules with hashed class names
     |
     v
Emits static .vanilla.css file (included in CSS bundle)
     |
     v
JS side receives only { button: "_abc12", variants: { primary: "_def34", ... } }
```

### styled-components Pipeline (webpack)

```txt
const Button = styled.button`color: red;`
     |
     v
babel-plugin-styled-components (optional) adds display names in dev
     |
     v
At RENDER TIME (browser or server):
  styled-components generates a class name hash from the template literal
  Inserts <style>.sc-abc12 { color: red; }</style> into <head>
  Returns className "sc-abc12" to the component
     |
     v
Bundle includes styled-components runtime library (~12 kB gzip)
```

---

## 11. Common Interview Traps

| Trap | Wrong assumption | Correct answer |
|---|---|---|
| "styled-components is zero-cost because CSS is static" | CSS is static, but the GENERATION is runtime JS | styled-components generates and injects CSS at render time — runtime cost exists |
| "CSS Modules means CSS is in JavaScript" | CSS Modules are processed by the bundler — no JS runtime | CSS Modules produce static CSS files with scoped class names — no runtime style generation |
| "Vanilla Extract is just styled-components with TypeScript" | Both use TS but fundamentally different | Vanilla Extract runs at build time; styled-components at runtime |
| "Emotion is faster, so there's no reason not to use it in Next.js 13+" | Speed is relative; compatibility is not | Runtime CSS-in-JS (including emotion) is incompatible with React 18 streaming SSR |
| "You can just add `suppressHydrationWarning` to fix SSR style flicker" | Suppresses the warning, not the underlying mismatch | Style flicker is a render pipeline problem; suppressing the warning hides symptoms |

---

## 12. Migration Patterns

### Migrating from styled-components to CSS Modules

```txt
1. Identify pure-static components (no prop-driven styles)
   → Replace with CSS Modules first (lowest risk)

2. For prop-driven styles, convert to data-attributes:
   // Before: styled.button<{ active: boolean }>`color: ${p => p.active ? 'blue' : 'gray'};`
   // After:  .button[data-active="true"] { color: blue; }
   //         .button[data-active="false"] { color: gray; }
   <button data-active={String(active)} className={styles.button}>

3. For theme tokens, migrate to CSS custom properties:
   // Before: color: ${theme.colors.primary};
   // After:  color: var(--color-primary);

4. Test SSR output for style flicker after each component migration.
```

### Migrating from styled-components to Vanilla Extract

```txt
1. Run both systems in parallel during migration (different files)
2. Start with shared tokens/theme — create theme.css.ts first
3. Migrate leaf components (buttons, inputs) before compound layouts
4. Use styleVariants() to replace prop-conditional template literals
5. Replace dynamic prop callbacks with CSS custom properties
6. Remove styled-components entirely after all components migrated
```

---

## 13. Server Components Compatibility Summary

| Approach | React Server Components | Streaming SSR | Client-only | Notes |
|---|---|---|---|---|
| CSS Modules | ✅ | ✅ | No | Best default for App Router |
| Tailwind | ✅ | ✅ | No | Zero runtime |
| Vanilla Extract | ✅ | ✅ | No | Build-time extraction |
| Panda CSS | ✅ | ✅ | No | App Router native |
| linaria | ✅ (static only) | ✅ | No | Static template literals only |
| emotion (v12) | ❌ | ⚠️ Experimental | Yes (needs 'use client') | Not recommended in App Router |
| styled-components (v6) | ❌ | ⚠️ Experimental | Yes (needs 'use client') | Not recommended in App Router |

---

## 14. Hot Interview Q&A

**Q: What is the core trade-off with runtime CSS-in-JS like styled-components?**  
A: Runtime CSS-in-JS generates and injects styles during JavaScript execution — on the server during SSR, and in the browser during render. The benefits are dynamic styles from props and great developer ergonomics. The costs are bundle size (~12 kB for styled-components), runtime CPU for style generation, and incompatibility with React 18's streaming SSR because styles cannot be collected for partial HTML chunks that haven't rendered yet.

**Q: Why is runtime CSS-in-JS incompatible with React 18 streaming?**  
A: Streaming SSR sends HTML to the browser in chunks as components render. Runtime CSS-in-JS needs to collect all styles used in a render and inject them as `<style>` tags before the HTML flushes. With streaming, the entire render hasn't completed when HTML starts flushing — so style collection is either delayed (breaking streaming) or incomplete (causing flash of unstyled content on hydration).

**Q: What does "zero-runtime" mean in Vanilla Extract?**  
A: It means the style generation code runs at BUILD TIME in Node.js, not at RUNTIME in the browser. The `.css.ts` file is executed during the build, CSS is extracted to a static file, and the browser receives only class name strings. No Vanilla Extract code ships to the browser.

**Q: When would you still choose styled-components in 2025?**  
A: For legacy codebases with many components already written in styled-components, where migrating all of them would cost more than the streaming SSR benefit provides. Also for pure client-rendered SPAs (no SSR) where streaming compatibility is not a concern. For new projects, zero-runtime or CSS Modules is the default recommendation.

**Q: How do you handle truly dynamic styles (user-provided colors) in zero-runtime CSS-in-JS?**  
A: Use CSS custom properties. Declare a CSS variable name in the Vanilla Extract style, set its value via inline styles in the component, and let the browser resolve it. This gives runtime dynamism without generating new CSS rules at runtime:
```tsx
const dynamicText = style({ color: 'var(--user-color)' });
<span className={dynamicText} style={{ '--user-color': user.brandColor } as React.CSSProperties}>
```

---

## 15. Revision Notes

- One-line summary: CSS-in-JS trade-offs are build-time vs. runtime generation, with streaming SSR compatibility as the deciding factor for modern Next.js.
- Three keywords: runtime, zero-runtime, streaming.
- One interview trap: Assuming styled-components is "free" because CSS is cached — the generation cost is real and the SSR complication is a hard blocker.
- Memory trick: "If it runs in the browser to make CSS, it can't stream."
