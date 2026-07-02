# React Native Styling, Flexbox, And Responsive UI - Gold Sheet

> Track Module - Group 1: Starter Path
> Level: mobile layout foundations and interview-ready styling judgment

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| `StyleSheet.create` | Very high | Standard style organization |
| Flexbox | Very high | Main layout model |
| RN vs CSS differences | Very high | Common web-to-mobile trap |
| Safe areas | High | Avoids notch/status bar overlap |
| Keyboard handling | High | Critical for auth/forms/chat |
| Responsive design | High | Phones, tablets, orientation, text size |
| Platform-specific styles | Medium | Needed for polished native feel |
| Accessibility-aware layout | High | MAANG mobile apps must support inclusive UX |

MAANG signal:
You design screens that survive small phones, big phones, tablets, dynamic text, keyboards, safe areas, and platform differences.

---

## 2. Mental Model

React Native styles look like CSS, but they are JavaScript objects interpreted by the native renderer.

Important differences from web:
- No CSS cascade.
- No class selectors.
- No DOM.
- No media queries by default.
- Default `flexDirection` is `column`, not `row`.
- Numeric sizes are density-independent points, not CSS pixels.
- Text styling is inherited only through nested `Text`, not through arbitrary `View` trees.

---

## 3. StyleSheet Basics

```tsx
const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#ffffff',
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
  },
});
```

Why use `StyleSheet.create`:
- Organizes style names.
- Catches some style typos through TypeScript/editor tooling.
- Keeps render code cleaner.
- Makes style reuse easier.

Inline styles are fine for small dynamic values:

```tsx
<View style={[styles.badge, {backgroundColor: statusColor}]} />
```

---

## 4. Flexbox In React Native

Common layout properties:

| Property | Meaning |
|---|---|
| `flex` | How much space the element takes |
| `flexDirection` | Main axis: `column` default, or `row` |
| `justifyContent` | Alignment on main axis |
| `alignItems` | Alignment on cross axis |
| `gap` | Space between children in modern RN |
| `padding` | Inner spacing |
| `margin` | Outer spacing |

Example:

```tsx
<View style={styles.row}>
  <Text style={styles.name}>Jane Doe</Text>
  <Text style={styles.status}>Active</Text>
</View>
```

```tsx
const styles = StyleSheet.create({
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: 16,
  },
  name: {
    flex: 1,
    fontSize: 16,
    fontWeight: '600',
  },
  status: {
    marginLeft: 12,
  },
});
```

Trap:
If text can be long, give it `flex: 1` and maybe `numberOfLines`.

---

## 5. Safe Area

Mobile devices have notches, rounded corners, status bars, and home indicators.

Use safe area handling for screens:

```tsx
import {SafeAreaView} from 'react-native-safe-area-context';

export function AccountScreen() {
  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <AccountContent />
    </SafeAreaView>
  );
}
```

Production rule:
Use `react-native-safe-area-context` in real apps, especially with navigation.

---

## 6. Keyboard Handling

Forms must handle the keyboard.

Common tools:
- `KeyboardAvoidingView`
- `ScrollView` with `keyboardShouldPersistTaps`
- platform-specific behavior
- avoiding fixed bottom buttons that get covered

Example:

```tsx
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
} from 'react-native';

export function LoginScreen() {
  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.container}
    >
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <LoginForm />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}
```

Interview answer:

```text
For forms, I test with the keyboard open on both platforms. I usually combine
safe area handling, KeyboardAvoidingView, and a scroll container so fields and
submit buttons remain reachable on small screens.
```

---

## 7. Responsive UI

React Native responsive design is less about breakpoints and more about constraints.

Good practices:
- Use `flex` layouts instead of fixed heights.
- Use `maxWidth` for tablet content.
- Use `numberOfLines` for long text.
- Use `useWindowDimensions` for orientation/tablet decisions.
- Support dynamic font sizes unless product has a strong reason not to.
- Test small Android devices, large iPhones, and tablets if supported.

Example:

```tsx
import {useWindowDimensions, View} from 'react-native';

export function ResponsiveShell({children}: {children: React.ReactNode}) {
  const {width} = useWindowDimensions();
  const isTablet = width >= 768;

  return (
    <View style={[styles.shell, isTablet && styles.tabletShell]}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  shell: {
    flex: 1,
    padding: 16,
  },
  tabletShell: {
    alignSelf: 'center',
    maxWidth: 720,
    width: '100%',
  },
});
```

---

## 8. Platform-Specific Styling

```tsx
import {Platform, StyleSheet} from 'react-native';

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    ...Platform.select({
      ios: {
        shadowColor: '#000000',
        shadowOpacity: 0.12,
        shadowRadius: 12,
        shadowOffset: {width: 0, height: 4},
      },
      android: {
        elevation: 4,
      },
    }),
  },
});
```

Use this sparingly:
- Prefer shared design tokens.
- Use platform differences only when native polish requires it.

---

## 9. Design System Thinking

In large apps, avoid random one-off styles.

Use:
- color tokens
- typography tokens
- spacing scale
- reusable components
- accessible contrast
- dark mode support
- predictable touch target sizes

Example:

```tsx
export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
};

export const colors = {
  background: '#ffffff',
  text: '#0f172a',
  mutedText: '#64748b',
  primary: '#2563eb',
};
```

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Copying CSS directly from web | RN style support differs | Use RN-supported style props |
| Fixed widths everywhere | Breaks on devices | Use flex and max constraints |
| Ignoring safe area | Content overlaps system UI | Use safe-area context |
| Ignoring keyboard | Forms become unusable | Test keyboard states |
| Long text without constraints | Layout breaks | Use flex, wrapping, numberOfLines |
| Tiny touch targets | Bad accessibility and UX | Use at least around 44x44 pt targets |

---

## 11. Strong Interview Answer

Question:
How is styling different in React Native compared with web React?

Strong answer:

```text
React Native styling uses JavaScript objects, not CSS files or browser cascade.
The layout model is Flexbox, but the default direction is column and styles map
to native layout systems. There is no DOM or CSS media query model by default,
so I design with flex constraints, safe areas, keyboard handling, platform checks,
and useWindowDimensions. In production I prefer design tokens and reusable
components so the app remains consistent across devices and accessibility settings.
```

---

## 12. Revision Notes

- One-line summary: React Native styles are JS objects that drive native layout.
- Three keywords: Flexbox, safe area, keyboard.
- One interview trap: RN Flexbox defaults to `column`, not web's usual row mental model.
- One memory trick: Mobile layout must survive notches, keyboards, and small screens.

