# React + Next.js Design System, Accessibility, I18n, And Visual Testing - Gold Sheet

> Track File #34 - Group 6: Performance, Security, Styling, And Testing
> Level: intermediate -> staff frontend | Design tokens, component APIs, accessibility, internationalization, RTL, Storybook, and visual regression

---

## 1. Intuition

A design system is not a prettier button folder. It is a product interface contract.

```text
Tokens -> primitives -> composed components -> product screens -> tests/governance
```

Accessibility and internationalization are not polish. They are correctness requirements for real users.

---

## 2. Definition

- Definition: A design system is a reusable set of visual tokens, components, interaction patterns, documentation, and quality gates.
- Category: Frontend architecture / UI platform.
- Core idea: Build common UI once, make it accessible, themeable, testable, and hard to misuse.

---

## 3. Token Model

Tokens describe design decisions without tying them to a specific component.

```css
:root {
  --color-bg: #ffffff;
  --color-fg: #111827;
  --color-primary: #2563eb;
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --radius-sm: 0.25rem;
  --radius-md: 0.5rem;
}

[data-theme='dark'] {
  --color-bg: #111827;
  --color-fg: #f9fafb;
}
```

Token categories:
- color;
- typography;
- spacing;
- radius;
- shadow;
- motion;
- z-index;
- breakpoints;
- semantic states.

Prefer semantic tokens:

```text
--color-danger-bg
--color-success-fg
--color-surface-muted
```

over raw tokens in product code:

```text
--red-600
--blue-100
```

---

## 4. Component API Principles

Good component APIs are:
- small;
- predictable;
- accessible by default;
- composable;
- hard to misuse;
- escape-hatch friendly.

Example:

```tsx
type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
};

export function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      data-variant={variant}
      data-size={size}
    >
      {isLoading ? 'Loading...' : children}
    </button>
  );
}
```

Avoid:
- `any` props;
- styling-only booleans like `isBlue`, `isRounded`, `isFancy`;
- inaccessible div-buttons;
- forcing product screens to reimplement focus/error states.

---

## 5. Headless Components

Headless components own behavior, not final styling.

Good candidates:
- dialog;
- menu;
- combobox;
- tabs;
- accordion;
- tooltip;
- popover;
- date picker.

Use battle-tested libraries when behavior is complex:
- Radix UI;
- React Aria;
- Headless UI;
- Ariakit.

Why:
Keyboard and screen-reader behavior is harder than it looks.

---

## 6. Accessibility Checklist

Every interactive component should answer:

```text
[ ] Can keyboard users reach it?
[ ] Is focus visible?
[ ] Does Enter/Space work where expected?
[ ] Does Escape close overlays?
[ ] Is focus trapped in modal dialogs?
[ ] Is focus restored after close?
[ ] Is the accessible name correct?
[ ] Are errors announced?
[ ] Does color contrast pass?
[ ] Does zoom/reflow work at 200%?
```

Common ARIA rule:
Use semantic HTML first. Add ARIA only when HTML cannot express the behavior.

Good:

```tsx
<button type="button">Save</button>
```

Bad:

```tsx
<div role="button" onClick={save}>Save</div>
```

---

## 7. Dialog Pattern

Expected behavior:
- opens with focus inside dialog;
- traps focus;
- closes on Escape if allowed;
- restores focus to trigger;
- background is inert or unavailable to screen readers;
- has title and optional description.

Example with a headless dialog primitive:

```tsx
<Dialog open={open} onOpenChange={setOpen}>
  <DialogTrigger asChild>
    <Button>Delete</Button>
  </DialogTrigger>
  <DialogContent>
    <DialogTitle>Delete project?</DialogTitle>
    <DialogDescription>
      This action cannot be undone.
    </DialogDescription>
    <Button variant="danger" onClick={confirmDelete}>
      Delete
    </Button>
  </DialogContent>
</Dialog>
```

---

## 8. Forms And Error A11y

```tsx
<label htmlFor="email">Email</label>
<input
  id="email"
  name="email"
  type="email"
  aria-invalid={Boolean(error)}
  aria-describedby={error ? 'email-error' : undefined}
/>
{error ? (
  <p id="email-error" role="alert">
    {error}
  </p>
) : null}
```

Rules:
- label every input;
- connect errors with `aria-describedby`;
- use `role="alert"` or live regions for async validation;
- never rely on color alone.

---

## 9. Internationalization

Internationalization means building UI so localization is possible.

Localization means providing translated content.

Next.js i18n concerns:
- locale-aware routing;
- translated messages;
- date/time/number formatting;
- metadata per locale;
- RTL layouts;
- font fallback;
- localized validation messages;
- canonical and alternate URLs.

Locale route shape:

```text
app/[locale]/layout.tsx
app/[locale]/page.tsx
app/[locale]/products/page.tsx
```

Validate locale:

```ts
const locales = ['en', 'es', 'ar'] as const;
type Locale = (typeof locales)[number];

export function isLocale(value: string): value is Locale {
  return locales.includes(value as Locale);
}
```

---

## 10. RTL Readiness

Use logical CSS properties:

```css
.card {
  padding-inline: 1rem;
  margin-block: 1rem;
  border-inline-start: 4px solid var(--color-primary);
}
```

Avoid directional CSS unless necessary:

```css
/* weaker */
margin-left: 1rem;
border-left: 4px solid blue;
```

Set direction at document or layout level:

```tsx
export default function LocaleLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: { locale: string };
}) {
  const dir = params.locale === 'ar' ? 'rtl' : 'ltr';

  return (
    <html lang={params.locale} dir={dir}>
      <body>{children}</body>
    </html>
  );
}
```

---

## 11. Storybook Strategy

Storybook is useful when:
- many teams reuse components;
- design review happens before product integration;
- visual regression matters;
- components have many states.

Story categories:

```text
Button
  Primary
  Secondary
  Danger
  Loading
  Disabled
  Long Label
  High Contrast
  RTL
```

Each component should document:
- purpose;
- props;
- accessibility notes;
- do/don't examples;
- loading/empty/error states;
- keyboard behavior for complex widgets.

---

## 12. Visual Regression

Tools:
- Playwright screenshots;
- Chromatic;
- Percy;
- Loki;
- Happo.

Test what users see:
- component variants;
- responsive breakpoints;
- locale expansion;
- dark/high-contrast themes;
- error states;
- loading skeletons;
- modals/menus/tooltips.

Example Playwright screenshot:

```ts
import { test, expect } from '@playwright/test';

test('checkout summary visual state', async ({ page }) => {
  await page.goto('/checkout');
  await expect(page).toHaveScreenshot('checkout-summary.png', {
    fullPage: true,
  });
});
```

---

## 13. Governance

Without governance, design systems become another pile of components.

Rules:
- product teams can request variants, not invent random ones;
- tokens are versioned;
- breaking visual changes need migration notes;
- accessibility regressions block release;
- Storybook/visual tests run in CI for shared UI;
- deprecated components have replacement paths.

Quality gates:

```text
lint -> typecheck -> unit tests -> accessibility tests -> visual tests -> e2e smoke
```

---

## 14. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Treating design system as CSS only | Behavior and a11y drift | Own component behavior |
| Using ARIA instead of HTML | More bugs | Semantic HTML first |
| No RTL testing | Layout breaks for RTL languages | Use logical properties |
| No visual regression | UI breaks silently | Screenshot critical states |
| Token names tied to colors | Rebrand becomes expensive | Use semantic tokens |
| Docs without examples | Teams misuse components | Storybook with real states |

---

## 15. Practical Question

> You are building a shared React component library for multiple product teams. What would you include before calling it production-ready?

---

## 16. Strong Answer

```text
I would define semantic design tokens, accessible headless or primitive
components, clear component APIs, Storybook examples for all states, keyboard
and screen-reader behavior, RTL and localization checks, and visual regression
tests. I would also add governance: versioning, deprecation paths, accessibility
gates, and rules for adding variants. The goal is not just consistent visuals,
but consistent behavior and safe reuse across teams.
```

---

## 17. Revision Notes

- One-line summary: A design system is a tested UI contract, not a button folder.
- Three keywords: tokens, a11y, visual tests.
- One interview trap: ARIA does not replace semantic HTML.
- One memory trick: Design system quality = visuals + behavior + governance.

