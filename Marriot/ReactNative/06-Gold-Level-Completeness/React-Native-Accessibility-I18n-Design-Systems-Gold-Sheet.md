# React Native Accessibility, Internationalization, And Design Systems - Gold Sheet

> Track Module - Group 6: Gold-Level Completeness
> Level: inclusive product quality and global mobile readiness

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Accessibility labels/roles | Very high | Screen reader support |
| Touch target size | High | Motor accessibility and mobile UX |
| Dynamic text | High | Users change font size |
| Color contrast | Very high | Readability and compliance |
| Focus management | High | Modals, navigation, forms |
| RTL support | Medium-high | Global apps |
| Pluralization/date/currency | High | Correct localization |
| Design-system primitives | Very high | Accessibility at scale |

MAANG signal:
You build accessibility and localization into reusable components, not as a cleanup task before launch.

---

## 2. Mental Model

Accessibility is part of the app contract.

```text
Design token
  -> accessible primitive
  -> feature component
  -> screen
  -> screen reader / touch / visual user experience
```

If accessibility is missing from the primitive, every screen repeats the same bug.

---

## 3. Core Accessibility Props

Common props:
- `accessible`
- `accessibilityLabel`
- `accessibilityHint`
- `accessibilityRole`
- `accessibilityState`
- `accessibilityValue`
- `accessibilityActions`
- `onAccessibilityAction`

Example:

```tsx
<Pressable
  accessibilityRole="button"
  accessibilityLabel="Add backpack to cart"
  accessibilityHint="Adds this product to your shopping cart"
  accessibilityState={{disabled: isDisabled}}
  disabled={isDisabled}
  onPress={onAddToCart}
>
  <Text>Add to cart</Text>
</Pressable>
```

Rule:
Label describes what it is or does. Hint explains the result when useful.

---

## 4. Roles And States

Use roles for semantic meaning:
- `button`
- `link`
- `header`
- `image`
- `search`
- `tab`
- `switch`
- `adjustable`

Use states for current condition:
- disabled
- selected
- checked
- busy
- expanded

Example:

```tsx
<Pressable
  accessibilityRole="tab"
  accessibilityLabel="Orders"
  accessibilityState={{selected: activeTab === 'orders'}}
  onPress={() => setActiveTab('orders')}
>
  <Text>Orders</Text>
</Pressable>
```

---

## 5. Design-System Primitive Pattern

```tsx
type IconButtonProps = {
  label: string;
  icon: React.ReactNode;
  onPress: () => void;
  disabled?: boolean;
};

export function IconButton({label, icon, onPress, disabled}: IconButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{disabled}}
      disabled={disabled}
      hitSlop={8}
      onPress={onPress}
      style={[styles.button, disabled && styles.disabled]}
    >
      {icon}
    </Pressable>
  );
}
```

Benefit:
Icon-only buttons cannot ship without an accessible name because `label` is required.

---

## 6. Dynamic Text And Layout

Users may increase system font size.

Design for:
- wrapping text
- larger row heights
- buttons that can grow
- not clipping text
- scrollable forms
- avoiding fixed heights for text-heavy components

Bad:

```tsx
button: {
  height: 40,
}
```

Better:

```tsx
button: {
  minHeight: 44,
  paddingHorizontal: 16,
  paddingVertical: 10,
}
```

Rule:
Use `minHeight`, padding, and flexible layout instead of fixed text containers.

---

## 7. Focus And Modals

When opening a modal:
- announce meaningful title
- focus should move into modal
- screen reader should not wander into hidden background content where possible
- close button must be accessible
- hardware back/escape behavior should be clear

For destructive dialogs:
- state the consequence
- make cancel easy
- avoid ambiguous labels

Good labels:
- `Delete note`
- `Cancel`

Bad labels:
- `Yes`
- `No`

---

## 8. Color, Motion, And Touch

Checklist:
- contrast meets product/accessibility standard
- disabled state is not color-only
- error state includes icon/text, not only red border
- touch targets are large enough
- important gestures have non-gesture alternative
- respect reduced motion where possible
- do not rely only on haptics/sound

Interview answer:

```text
I avoid color-only meaning. Error states include text, selected states include
semantic state, and icon-only actions have labels. Design-system primitives make
these defaults automatic instead of relying on every feature engineer to remember.
```

---

## 9. Internationalization

I18n concerns:
- translation lookup
- pluralization
- interpolation
- date formatting
- number formatting
- currency formatting
- measurement units
- RTL layout
- locale-specific assets/legal copy

Bad:

```tsx
<Text>{count + ' items left'}</Text>
```

Better:

```tsx
<Text>{t('inventory.itemsLeft', {count})}</Text>
```

Why:
Plural rules differ across languages.

---

## 10. RTL Support

Right-to-left languages affect:
- layout direction
- icons/arrows
- gestures
- text alignment
- navigation transitions
- charts/timelines

Use logical concepts:
- start/end instead of left/right where possible
- test with RTL enabled
- avoid images with embedded English text
- mirror directional icons intentionally, not blindly

---

## 11. Testing Accessibility And I18n

Manual checks:
- VoiceOver on iOS.
- TalkBack on Android.
- keyboard/open forms.
- large text size.
- dark mode/high contrast if supported.
- RTL locale.
- long translated strings.

Automated checks:
- component tests for labels/roles.
- lint rules where available.
- snapshot or visual checks for key localized screens.
- E2E smoke for critical accessible flows.

Example:

```tsx
expect(screen.getByRole('button', {name: 'Add backpack to cart'})).toBeTruthy();
```

---

## 12. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Icon-only button without label | Screen reader cannot explain it | Require label prop |
| Fixed-height text containers | Dynamic text clips | Use minHeight/padding |
| Color-only errors | Not accessible | Add text/icon/state |
| Concatenated translations | Breaks grammar/plurals | Use i18n keys |
| No RTL testing | Layout surprises late | Test RTL early |
| Accessibility only at QA | Expensive and incomplete | Build into primitives |

---

## 13. Strong Interview Answer

Question:
How do you make React Native accessibility and i18n scale across a large app?

Strong answer:

```text
I put accessibility and localization into the design system. Buttons, inputs,
icon buttons, tabs, modals, and error states require labels, roles, states, and
flexible layout by default. I avoid fixed text heights, support dynamic text,
avoid color-only meaning, and test VoiceOver/TalkBack on critical flows. For i18n,
I use translation keys with pluralization and locale-aware date/currency formatting,
avoid string concatenation, and test long strings and RTL layouts before launch.
```

---

## 14. Revision Notes

- One-line summary: Accessibility and i18n should be default behavior in shared primitives.
- Three keywords: label, dynamic text, pluralization.
- One interview trap: Icon-only buttons still need accessible names.
- One memory trick: Global users break fixed text, concatenated strings, and left/right assumptions.

