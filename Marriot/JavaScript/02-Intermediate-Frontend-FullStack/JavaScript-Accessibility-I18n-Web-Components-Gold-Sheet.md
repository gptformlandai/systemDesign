# JavaScript Accessibility, Internationalization, And Web Components Gold Sheet

> Track: JavaScript Interview Track - Intermediate Frontend / Full-Stack  
> Goal: build JavaScript UI behavior that works for real users, real languages, and reusable browser-native components.

---

## 1. Intuition

Frontend JavaScript is not finished when it works with a mouse in English on your laptop.
Production UI must work with keyboard, screen readers, different locales, text expansion,
right-to-left layouts, and sometimes framework-free reusable components.

---

## 2. Definition

- Accessibility: making UI usable by people with different input, vision, hearing, motion,
  and cognitive needs.
- Internationalization: designing code so it can support multiple languages, locales,
  number/date/currency formats, and text directions.
- Web Components: browser-native reusable components built from Custom Elements, Shadow DOM,
  templates, slots, and lifecycle callbacks.

---

## 3. Why It Exists

These topics exist because:

- divs with click handlers are not buttons
- dynamic UI can lose focus or hide updates from screen readers
- English string concatenation breaks in other languages
- dates, numbers, names, and currencies are locale-sensitive
- reusable UI sometimes must work across frameworks
- Shadow DOM can isolate styles but complicates testing, accessibility, and theming

---

## 4. Accessibility Core

Accessibility decision order:

1. Use semantic HTML first.
2. Use native controls when possible.
3. Add labels, names, roles, and states.
4. Support keyboard interaction.
5. Manage focus for dialogs, menus, and route changes.
6. Announce dynamic updates when needed.
7. Test with keyboard, automated checks, and at least one screen reader path for critical flows.

Bad:

```html
<div onclick="submitForm()">Submit</div>
```

Better:

```html
<button type="submit">Submit</button>
```

JavaScript rule:

```text
If JavaScript creates interaction, JavaScript also owns focus, keyboard behavior, state,
cleanup, and announcements.
```

---

## 5. Focus Management

Common focus rules:

- opening a modal moves focus into the modal
- closing a modal returns focus to the triggering control
- focus should not escape a modal while it is open
- route changes should place focus somewhere meaningful
- error summary should be focusable when validation fails
- removed DOM nodes should not leave focus lost

Modal sketch:

```js
function openDialog(dialog, trigger) {
  dialog.showModal();
  dialog.querySelector("button, [href], input, select, textarea")?.focus();

  dialog.addEventListener("close", () => {
    trigger.focus();
  }, { once: true });
}
```

---

## 6. ARIA Judgment

ARIA helps when semantic HTML is not enough. ARIA does not magically make a component
interactive.

Useful examples:

| Need | Tool |
|---|---|
| label an icon button | `aria-label` |
| mark current nav item | `aria-current="page"` |
| announce loading | `aria-busy="true"` or live region |
| custom combobox | WAI-ARIA combobox pattern |
| expanded disclosure | `aria-expanded` |

Trap:

```text
role="button" still requires keyboard behavior. A native button already has it.
```

---

## 7. Internationalization

i18n design rules:

- never concatenate translated strings with variables in fixed order
- use message templates and placeholders
- use `Intl.NumberFormat` for numbers/currency
- use `Intl.DateTimeFormat` for dates/times
- support plural rules
- design for text expansion
- support right-to-left layout when needed
- lazy-load locale bundles when large
- do not put user-facing strings deep inside business logic

Example:

```js
const price = new Intl.NumberFormat("de-DE", {
  style: "currency",
  currency: "EUR"
}).format(1299.5);

const date = new Intl.DateTimeFormat("en-IN", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Asia/Kolkata"
}).format(new Date());
```

---

## 8. Web Components

Web Components pieces:

| Piece | Purpose |
|---|---|
| Custom Elements | define new HTML elements |
| Shadow DOM | encapsulate DOM and styles |
| Templates | reusable inert markup |
| Slots | allow external content projection |
| Attributes/properties | configure component |
| Lifecycle callbacks | connect, disconnect, attribute changes |

Minimal Custom Element:

```js
class BookingBadge extends HTMLElement {
  static observedAttributes = ["status"];

  connectedCallback() {
    this.render();
  }

  attributeChangedCallback() {
    this.render();
  }

  render() {
    const status = this.getAttribute("status") ?? "unknown";
    this.textContent = `Booking: ${status}`;
  }
}

customElements.define("booking-badge", BookingBadge);
```

Shadow DOM sketch:

```js
class PriceCard extends HTMLElement {
  connectedCallback() {
    const root = this.attachShadow({ mode: "open" });
    root.innerHTML = `
      <style>
        strong { color: #0f766e; }
      </style>
      <strong><slot></slot></strong>
    `;
  }
}

customElements.define("price-card", PriceCard);
```

---

## 9. When To Use Web Components

Use Web Components when:

- component must work across React, Vue, Angular, and plain HTML
- design-system component needs framework neutrality
- browser-native encapsulation is valuable
- embedding widgets into third-party pages

Avoid or be careful when:

- your app is already entirely framework-owned
- accessibility semantics are hard to implement correctly
- server rendering/hydration support is required
- form participation is complex
- styling/theme integration is more important than encapsulation

---

## 10. Failure Modes

| Failure | User Observes | Root Cause | Fix |
|---|---|---|---|
| Keyboard cannot open menu | Broken non-mouse UX | div click handler | Native button or keyboard handlers |
| Screen reader misses update | No announcement | dynamic DOM change without live region | ARIA live region |
| Focus disappears | User gets stuck | removed focused node | move focus intentionally |
| Date wrong for user | Confusing locale/timezone | manual formatting | `Intl` APIs |
| Long text breaks layout | clipped UI | English-only design | flexible layout and text expansion |
| Component not themeable | design mismatch | Shadow DOM isolation | CSS custom properties/parts |

---

## 11. Practical Question

> Build an autocomplete component for a global booking site. What JavaScript concerns
> matter beyond filtering results?

---

## 12. Strong Answer

I would treat autocomplete as an accessibility and internationalization problem, not only a
filtering problem. It needs keyboard navigation, focus management, screen-reader semantics
using the correct combobox/listbox pattern, async race protection, loading state, and escape
behavior. For global users, I would use locale-aware matching where needed, avoid hardcoded
string order, and design for text expansion. If this component must work across frameworks,
I might consider a Web Component, but I would verify form behavior, theming, testing, and
screen-reader support before choosing Shadow DOM.

---

## 13. Revision Notes

- One-line summary: production UI must preserve semantics, focus, keyboard access, locale
  correctness, and reusable component boundaries.
- Three keywords: semantic, focus, locale.
- One interview trap: ARIA does not replace native behavior.
- One memory trick: mouse, keyboard, reader, locale, framework.

