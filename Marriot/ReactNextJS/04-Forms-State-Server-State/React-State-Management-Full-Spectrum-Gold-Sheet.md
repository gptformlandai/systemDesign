# React State Management Full Spectrum - Gold Sheet

> Track File #7 of 24 - Group 4: Forms, Client State, And Server State
> Covers: local vs global state, Context API, Redux, Redux Toolkit, Zustand, Jotai, Recoil, decision tree

---

## 1. Intuition

State management is ownership management.

```text
Who owns this value?
Who can change it?
Who needs to read it?
Does it come from server or client?
Does it need persistence?
```

Do not choose a library before classifying the state.

---

## 2. State Categories

| Category | Examples | Good Tool |
|---|---|---|
| Local UI | modal open, tab, input | `useState`, `useReducer` |
| Shared UI | theme, sidebar state | Context, Zustand |
| Complex app state | workflow, editor state | reducer, Zustand, Redux Toolkit |
| Server state | users, products, feed | TanStack Query/SWR/RTK Query |
| URL state | filters, pagination | search params/router |
| Persistent state | preferences | localStorage/cookie/server |

---

## 3. Context API

Context passes values deeply without prop drilling.

```tsx
const ThemeContext = createContext<'light' | 'dark'>('light');

function useTheme() {
  return useContext(ThemeContext);
}
```

Good for:
- theme
- locale
- current user snapshot
- feature flags
- dependency injection

Limitations:
- provider value changes re-render consumers
- not a full state management library
- poor fit for high-frequency updates

---

## 4. Redux And Redux Toolkit

Redux exists to make state transitions explicit, predictable, inspectable, and testable.

Redux Toolkit is the standard modern way to write Redux.

```ts
const cartSlice = createSlice({
  name: 'cart',
  initialState: [] as CartItem[],
  reducers: {
    itemAdded(state, action: PayloadAction<CartItem>) {
      state.push(action.payload);
    },
    itemRemoved(state, action: PayloadAction<string>) {
      return state.filter(item => item.id !== action.payload);
    },
  },
});
```

Use Redux Toolkit when:
- many teams touch shared state
- complex workflows need predictable events
- devtools/time travel matter
- strict conventions matter

Avoid when:
- state is mostly server cache
- app is small
- local component state is enough

---

## 5. Zustand

Zustand is a lightweight store with less boilerplate.

```ts
type CartStore = {
  count: number;
  add: () => void;
};

export const useCartStore = create<CartStore>(set => ({
  count: 0,
  add: () => set(state => ({count: state.count + 1})),
}));
```

Good for:
- small/medium global state
- UI shell state
- feature-level stores
- less ceremony than Redux

Trade-off:
Less enforced architecture than Redux.

---

## 6. Jotai And Recoil

Jotai:
- atomic state model
- small independent atoms
- good for derived state graphs

```ts
const countAtom = atom(0);
const doubledAtom = atom(get => get(countAtom) * 2);
```

Recoil:
- also atom/selector mental model
- conceptual understanding still useful, though adoption varies by org

Use atom models when:
- state naturally decomposes into small atoms
- derived relationships matter
- you want localized subscriptions

---

## 7. Decision Tree

```text
Can it stay in one component?
  yes -> useState/useReducer

Needed by URL/back/share?
  yes -> URL search params/path

Server owns it?
  yes -> TanStack Query/SWR/RTK Query

Global but low-frequency?
  yes -> Context or Zustand

Complex cross-team workflow?
  yes -> Redux Toolkit

Atom-like derived graph?
  yes -> Jotai/Recoil-style model
```

---

## 8. Real-World Use Cases

- Theme: Context.
- Cart drawer UI open: Zustand or local layout state.
- Cart server contents: server-state cache or backend.
- Checkout workflow: reducer/Redux Toolkit.
- Admin filters: URL search params.
- Spreadsheet/editor cells: specialized store/atom model.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Putting all server data in Redux manually | Duplicates cache concerns | Use server-state library |
| Global state for local modal | Unnecessary coupling | Keep local |
| Context for high-frequency updates | Rerender storms | Store with selectors |
| State not reflected in URL | Filters not shareable | Use search params |
| Library shopping before classification | Overengineering | Classify ownership first |

---

## 10. Strong Interview Answer

Question:
How do you choose React state management?

Strong answer:

```text
I classify the state first. Local UI stays local. Shareable route state goes into
the URL. Server-owned data belongs in a server-state cache. Low-frequency global
settings can use Context. Lightweight app state can use Zustand. Complex shared
workflows with strict conventions are a good fit for Redux Toolkit. Atom-based
stores like Jotai fit derived state graphs. The mistake is treating Redux or any
single library as the default for all state.
```

---

## 11. Revision Notes

- One-line summary: State management starts with ownership, not libraries.
- Three keywords: local, server, URL.
- One interview trap: Context is not optimized global state by default.
- One memory trick: Server owns server state; URL owns shareable state; component owns local state.

