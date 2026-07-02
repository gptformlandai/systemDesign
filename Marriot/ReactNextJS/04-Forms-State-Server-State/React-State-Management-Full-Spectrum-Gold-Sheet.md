# React State Management Full Spectrum - Gold Sheet

> Track Module - Group 4: Forms, Client State, And Server State
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

---

## 12. Zustand — Internals and Selector Optimization

### Zustand Store Internals

Zustand uses a closure to store state outside React. Subscribers are notified when state changes.

```tsx
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';

type CartStore = {
  items: CartItem[];
  total: number;
  addItem: (item: CartItem) => void;
  removeItem: (id: string) => void;
  clearCart: () => void;
};

const useCartStore = create<CartStore>()(
  devtools(
    persist(
      (set, get) => ({
        items: [],
        total: 0,

        addItem: (item) => set((state) => {
          const existing = state.items.find(i => i.id === item.id);
          if (existing) {
            return {
              items: state.items.map(i => i.id === item.id ? { ...i, qty: i.qty + 1 } : i),
              total: state.total + item.price,
            };
          }
          return { items: [...state.items, { ...item, qty: 1 }], total: state.total + item.price };
        }),

        removeItem: (id) => set((state) => {
          const item = state.items.find(i => i.id === id);
          return {
            items: state.items.filter(i => i.id !== id),
            total: state.total - (item ? item.price * item.qty : 0),
          };
        }),

        clearCart: () => set({ items: [], total: 0 }),
      }),
      { name: 'cart-storage' }  // persists to localStorage
    )
  )
);
```

### Selector Optimization — Preventing Unnecessary Re-renders

```tsx
// BAD: subscribes to entire store — re-renders on ANY store change
function CartBadge() {
  const store = useCartStore();  // full store subscription
  return <span>{store.items.length}</span>;
}

// GOOD: selector subscribes only to items.length
function CartBadge() {
  const count = useCartStore(state => state.items.length);
  // Re-renders ONLY when items.length changes (Zustand does shallow compare)
  return <span>{count}</span>;
}

// GOOD: multiple selectors with shallow — one re-render if either changes
import { useShallow } from 'zustand/react/shallow';
function CartSummary() {
  const { count, total } = useCartStore(
    useShallow(state => ({ count: state.items.length, total: state.total }))
  );
  return <p>{count} items — ${total}</p>;
}
```

---

## 13. Redux Toolkit — When and How

**When RTK makes sense:**
- Team > 5 engineers, complex shared state with many interaction paths
- Need strict conventions, action replay, time-travel debugging
- Backend team already thinks in actions/events (CQRS-style)

```tsx
// store/userSlice.ts
import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';

type UserState = { user: User | null; status: 'idle' | 'loading' | 'success' | 'error'; error: string | null };

export const fetchUser = createAsyncThunk('user/fetch', async (id: string) => {
  const res = await fetch(`/api/users/${id}`);
  if (!res.ok) throw new Error('Failed to fetch');
  return res.json() as Promise<User>;
});

const userSlice = createSlice({
  name: 'user',
  initialState: { user: null, status: 'idle', error: null } as UserState,
  reducers: {
    logout: (state) => { state.user = null; state.status = 'idle'; },
    updateName: (state, action: PayloadAction<string>) => {
      if (state.user) state.user.name = action.payload;  // Immer allows mutation syntax
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUser.pending, (state) => { state.status = 'loading'; })
      .addCase(fetchUser.fulfilled, (state, action) => { state.status = 'success'; state.user = action.payload; })
      .addCase(fetchUser.rejected, (state, action) => { state.status = 'error'; state.error = action.error.message ?? null; });
  },
});
```

### RTK Query — Replaces Manual Data Fetching

```tsx
// store/api.ts
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export const api = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['Product', 'Cart'],
  endpoints: (builder) => ({
    getProduct: builder.query<Product, string>({
      query: (id) => `/products/${id}`,
      providesTags: (result, error, id) => [{ type: 'Product', id }],
    }),
    updateProduct: builder.mutation<Product, Partial<Product> & { id: string }>({
      query: ({ id, ...body }) => ({ url: `/products/${id}`, method: 'PATCH', body }),
      invalidatesTags: (result, error, { id }) => [{ type: 'Product', id }],  // auto-revalidate
    }),
  }),
});

// Usage in component — loading/error/data handled automatically
const { data: product, isLoading } = api.useGetProductQuery(productId);
const [updateProduct] = api.useUpdateProductMutation();
```

---

## 14. Jotai — Atomic State

Jotai atoms are the smallest unit of state. Derived atoms recompute automatically.

```tsx
import { atom, useAtom, useAtomValue } from 'jotai';

// Primitive atoms
const countAtom = atom(0);
const nameAtom = atom('');

// Derived atom — recomputes when countAtom changes
const doubleCountAtom = atom(get => get(countAtom) * 2);

// Async derived atom
const userAtom = atom(async (get) => {
  const id = get(userIdAtom);
  const res = await fetch(`/api/users/${id}`);
  return res.json();
});

// In component
function Counter() {
  const [count, setCount] = useAtom(countAtom);
  const double = useAtomValue(doubleCountAtom);  // read-only
  return <button onClick={() => setCount(c => c + 1)}>{count} (×2 = {double})</button>;
}
```

**Jotai vs Zustand:**
- Jotai: fine-grained atoms, natural for derived/computed state, great for feature-local state
- Zustand: whole-store model, better for cross-feature state with clear actions

---

## 15. Context — When It's Fine and When It Hurts

```tsx
// FINE: Static or rarely-changing values
const ThemeContext = createContext<'light' | 'dark'>('light');
// All consumers re-render only when the provider's value changes

// PROBLEM: Frequent updates
const NotificationsContext = createContext<Notification[]>([]);
// Every new notification → ALL consumers of this context re-render
// Fix: Separate context for the notification list from the notification actions

// PATTERN: Split value context from dispatch context
const CartStateContext = createContext<CartState | undefined>(undefined);
const CartDispatchContext = createContext<CartDispatch | undefined>(undefined);
// Components that only dispatch never re-render on state changes
```

**Rule:** If more than 10 components subscribe to a context that updates frequently, consider Zustand.

