# React + Next.js Styling, UI System, And Accessibility - Gold Sheet

> Track File #14 of 24 - Group 6: Performance, Security, Styling, And Testing
> Covers: CSS fundamentals, Flexbox, Grid, Tailwind, CSS Modules, styled-components, design systems, accessibility

---

## 1. Intuition

Styling is not decoration. It is layout, usability, consistency, accessibility, and maintainability.

```text
tokens -> primitives -> components -> pages -> product experience
```

---

## 2. CSS Fundamentals

Must know:
- cascade
- specificity
- inheritance
- box model
- positioning
- stacking context
- responsive units
- media queries

Box model:

```css
.card {
  box-sizing: border-box;
  padding: 16px;
  border: 1px solid #ddd;
}
```

Use `box-sizing: border-box` globally in most apps.

---

## 3. Flexbox And Grid

Flexbox:
- one-dimensional layout
- rows/columns
- nav bars, cards, form rows

Grid:
- two-dimensional layout
- dashboards, galleries, page shells

```css
.dashboard {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
```

---

## 4. Styling Options

| Tool | Good For | Trade-off |
|---|---|---|
| CSS Modules | scoped component CSS | file switching |
| Tailwind CSS | utility-first speed/consistency | class noise if undisciplined |
| styled-components | dynamic component styles | runtime/SSR setup concerns |
| global CSS | resets/tokens/base styles | avoid feature-specific globals |
| design system | consistency at scale | upfront investment |

---

## 5. Tailwind CSS

Good:
- fast prototyping
- design-token-like classes
- avoids naming debates
- good Next ecosystem support

Watch:
- giant unreadable class strings
- inconsistent one-off values
- business logic inside class composition

Example:

```tsx
<button className="rounded-md bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700">
  Save
</button>
```

---

## 6. CSS Modules

```tsx
import styles from './Button.module.css';

export function Button() {
  return <button className={styles.button}>Save</button>;
}
```

```css
.button {
  border-radius: 8px;
  padding: 8px 16px;
}
```

Benefit:
Scoped class names avoid accidental global collisions.

---

## 7. Design Systems

Design system includes:
- color tokens
- spacing scale
- typography
- buttons
- inputs
- dialogs
- tables
- empty/error/loading states
- accessibility defaults
- documentation and examples

Component API:

```tsx
type ButtonProps = {
  variant?: 'primary' | 'secondary' | 'danger';
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
};
```

Good design-system components prevent common mistakes by default.

---

## 8. Accessibility

Checklist:
- semantic HTML first
- labels for inputs
- keyboard navigation
- focus visible
- color contrast
- ARIA only when semantic HTML is insufficient
- error messages associated with fields
- skip links/landmarks for large pages
- reduced motion where appropriate

Good:

```tsx
<label htmlFor="email">Email</label>
<input id="email" name="email" type="email" />
```

Bad:

```tsx
<div onClick={submit}>Submit</div>
```

Better:

```tsx
<button type="submit">Submit</button>
```

---

## 9. Real-World Use Cases

- SaaS dashboard: grid layout, design-system tables/forms.
- Ecommerce: responsive product grid, image aspect ratios.
- Admin forms: accessible labels, errors, focus management.
- Marketing page: responsive typography and performance.
- Modal system: portal, focus trap, escape close.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Div buttons | Not keyboard/semantic | Use button |
| Color-only errors | Inaccessible | Add text/icons/aria |
| Global CSS for features | Style leakage | CSS modules/components |
| Tailwind arbitrary values everywhere | Design drift | use token scale |
| No focus states | Keyboard users blocked | visible focus |

---

## 11. Strong Interview Answer

Question:
How do you design styling for a large React app?

Strong answer:

```text
I start with CSS fundamentals and a design system: tokens, primitives, layout
patterns, accessibility defaults, and documented component APIs. CSS Modules,
Tailwind, or CSS-in-JS can all work, but the key is consistency and ownership.
For accessibility, I prefer semantic HTML first, correct labels, keyboard support,
visible focus, sufficient contrast, and reusable primitives that make the right
thing easy across the product.
```

---

## 12. Revision Notes

- One-line summary: Styling at scale needs layout fundamentals, tokens, components, and accessibility defaults.
- Three keywords: CSS, design system, a11y.
- One interview trap: ARIA does not replace semantic HTML.
- One memory trick: Good UI system makes good accessibility the default.

