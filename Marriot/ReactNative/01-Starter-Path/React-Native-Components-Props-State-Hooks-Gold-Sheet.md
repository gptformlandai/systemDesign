# React Native Components, Props, State, And Hooks - Gold Sheet

> Track Module - Group 1: Starter Path
> Level: React fundamentals applied to mobile screens

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Functional components | Very high | Standard modern React Native style |
| Props vs state | Very high | Core render model |
| `useState` | Very high | Local UI state |
| `useEffect` | Very high | Fetching, subscriptions, lifecycle bugs |
| `useMemo` / `useCallback` | High | Avoiding unnecessary work in lists and child components |
| Controlled inputs | High | Forms, search, auth screens |
| Stale closures | High | Common bug in async handlers and effects |
| Component boundaries | High | Senior codebases depend on clean separation |

MAANG signal:
You can explain not just what hooks do, but how they affect render cost, stale state, and user experience.

---

## 2. Mental Model

A React Native screen is a pure description of UI for the current state.

```text
props + state -> render output -> native UI update
```

When state changes:

```text
setState -> component re-renders -> React calculates changes -> RN updates native views
```

Render should be cheap. Heavy computation, network calls, timers, subscriptions, and storage reads belong outside the direct render path.

---

## 3. Props vs State

Props:
- Passed from parent to child.
- Read-only from the child's perspective.
- Used to configure reusable components.

State:
- Owned by a component or store.
- Changes over time.
- Causes re-render when updated.

Example:

```tsx
type StatCardProps = {
  label: string;
  value: string;
  isHighlighted?: boolean;
};

export function StatCard({label, value, isHighlighted = false}: StatCardProps) {
  return (
    <View style={[styles.card, isHighlighted && styles.highlighted]}>
      <Text style={styles.label}>{label}</Text>
      <Text style={styles.value}>{value}</Text>
    </View>
  );
}
```

Production judgment:
Use props for reusable UI contracts. Use state only for data that actually changes inside the component.

---

## 4. Core Hooks

### `useState`

Use for local UI state:

```tsx
const [query, setQuery] = useState('');
```

Good for:
- Text input values.
- Toggle state.
- Selected tab.
- Modal open/close.

Bad for:
- Server cache shared across screens.
- Authentication state used by the whole app.
- Data that must survive app restart.

### `useEffect`

Use for side effects:

```tsx
useEffect(() => {
  const subscription = AppState.addEventListener('change', setAppState);
  return () => subscription.remove();
}, []);
```

Good for:
- Subscriptions.
- Timers.
- Logging screen view.
- Fetching when a dependency changes.

Trap:
Do not put side effects directly in render.

### `useMemo`

Use to memoize expensive derived values:

```tsx
const visibleItems = useMemo(() => {
  return items.filter(item => item.name.includes(query));
}, [items, query]);
```

Trap:
Do not use `useMemo` everywhere. It has overhead and can make code harder to read.

### `useCallback`

Use to preserve function identity when passing callbacks to memoized children:

```tsx
const handlePress = useCallback((id: string) => {
  navigation.navigate('Details', {id});
}, [navigation]);
```

Very useful with:
- `FlatList.renderItem`
- memoized row components
- expensive child trees

---

## 5. Controlled TextInput

```tsx
import {useState} from 'react';
import {Pressable, StyleSheet, Text, TextInput, View} from 'react-native';

type SearchBoxProps = {
  onSearch: (query: string) => void;
};

export function SearchBox({onSearch}: SearchBoxProps) {
  const [query, setQuery] = useState('');

  function submit() {
    const trimmed = query.trim();
    if (trimmed.length > 0) {
      onSearch(trimmed);
    }
  }

  return (
    <View style={styles.container}>
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder="Search"
        autoCapitalize="none"
        returnKeyType="search"
        onSubmitEditing={submit}
        style={styles.input}
      />
      <Pressable onPress={submit} style={styles.button}>
        <Text style={styles.buttonText}>Search</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 12,
  },
  input: {
    borderColor: '#cbd5e1',
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  button: {
    alignItems: 'center',
    backgroundColor: '#2563eb',
    borderRadius: 8,
    paddingVertical: 12,
  },
  buttonText: {
    color: '#ffffff',
    fontWeight: '700',
  },
});
```

Interview point:
`TextInput` is usually controlled through `value` and `onChangeText`. For large forms, move validation and submission logic into a form hook or form library.

---

## 6. Render Behavior

When a parent renders:
- The parent function executes again.
- Child component elements are recreated.
- React decides what actually changed.
- Memoized children can skip work if props are stable.

Common optimization tools:
- `React.memo`
- `useMemo`
- `useCallback`
- stable keys
- moving state closer to where it is needed
- splitting large components

Bad pattern:

```tsx
<FlatList
  data={items}
  renderItem={({item}) => <ProductRow product={item} onPress={() => buy(item.id)} />}
/>
```

Problem:
New function instances are created frequently, and row rendering can become expensive.

Better:

```tsx
const handleBuy = useCallback((id: string) => {
  buy(id);
}, [buy]);

const renderItem = useCallback(({item}: {item: Product}) => {
  return <ProductRow product={item} onPress={handleBuy} />;
}, [handleBuy]);

<FlatList data={items} renderItem={renderItem} keyExtractor={item => item.id} />;
```

---

## 7. Stale Closure Trap

Bad:

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  function incrementLater() {
    setTimeout(() => {
      setCount(count + 1);
    }, 1000);
  }
}
```

If `count` changes before the timeout runs, the callback may use an old value.

Better:

```tsx
setTimeout(() => {
  setCount(current => current + 1);
}, 1000);
```

Interview answer:

```text
Hooks close over values from the render in which they were created. For async
updates that depend on previous state, I use functional state updates or refs to
avoid stale values.
```

---

## 8. Component Boundary Pattern

Bad screen:

```tsx
function ProductScreen() {
  // fetch data
  // transform data
  // handle filters
  // render header
  // render list
  // render modal
  // handle analytics
  // handle checkout
}
```

Better:

```text
ProductScreen
  useProducts()
  ProductHeader
  ProductFilters
  ProductList
  CheckoutBanner
```

Rule:
Screens coordinate. Components render. Hooks own reusable behavior. Services own external IO.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Fetching in render | Repeats on every render | Fetch in effect or data hook |
| Missing effect dependencies | Creates stale data bugs | Include dependencies or restructure |
| Overusing global state | Makes simple UI hard to reason about | Prefer local state first |
| Mutating arrays directly | React may not detect meaningful changes | Create new arrays/objects |
| Inline heavy calculations | Blocks JS thread during render | Memoize or precompute |
| Huge screen components | Hard to test and optimize | Split by responsibility |

---

## 10. Strong Interview Answer

Question:
How do props, state, and hooks affect React Native performance?

Strong answer:

```text
Props configure components and state represents values that change over time.
When state changes, React re-renders the affected component tree and React Native
eventually updates native views. The key production concern is keeping render work
cheap because it runs on the JavaScript thread. I avoid side effects in render,
split large components, memoize expensive derived values, keep callbacks stable
for lists, and use functional updates to avoid stale closure bugs.
```

---

## 11. Revision Notes

- One-line summary: State changes drive renders; renders must stay cheap.
- Three keywords: props, state, hooks.
- One interview trap: `useCallback` is not magic; it only helps when identity matters.
- One memory trick: Screens coordinate, components render, hooks reuse behavior.

