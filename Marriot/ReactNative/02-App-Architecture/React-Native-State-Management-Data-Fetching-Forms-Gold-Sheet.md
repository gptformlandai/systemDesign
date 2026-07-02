# React Native State Management, Data Fetching, And Forms - Gold Sheet

> Track Module - Group 2: App Architecture
> Level: production state choices and mobile data UX

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Local vs global state | Very high | Prevents over-engineering |
| Server state | Very high | Most app data comes from APIs |
| React Query/SWR style cache | High | Modern production pattern |
| Redux/Zustand/Context | High | Interviewers ask when to use what |
| Forms and validation | Very high | Auth, checkout, onboarding |
| Optimistic updates | High | Smooth UX under network latency |
| Offline/poor network | High | Mobile-specific reality |
| Request cancellation/races | High | Common bug with search and navigation |

MAANG signal:
You classify state before choosing a library.

---

## 2. Mental Model

Not all state is the same.

```text
UI state:
  modal open, selected tab, input text

Server state:
  products, profile, orders, feed data

Global client state:
  auth session, feature flags, app theme

Persistent state:
  tokens, drafts, offline queue, preferences

Derived state:
  filtered list, totals, validation result
```

The right tool depends on the state category.

---

## 3. State Tool Decision Table

| State Type | Good Tool | Avoid |
|---|---|---|
| Local UI | `useState`, `useReducer` | Global store |
| Shared app state | Context, Zustand, Redux Toolkit | Prop drilling too far |
| Server cache | TanStack Query/SWR/Apollo/Relay | Manually duplicating API cache |
| Complex forms | React Hook Form/Formik + schema validation | Hundreds of `useState`s |
| Secure persisted state | SecureStore/Keychain/Keystore wrapper | AsyncStorage for secrets |
| Non-secret preferences | AsyncStorage/MMKV | Server round trips |

Interview line:

```text
I do not start with Redux. I first classify the state: local UI, server cache,
global app state, persistent state, or derived state. Then I choose the smallest
tool that gives correctness and maintainability.
```

---

## 4. Data Fetching Hook Pattern

Conceptual example with a query library:

```tsx
type Product = {
  id: string;
  name: string;
  priceCents: number;
};

async function fetchProducts(query: string): Promise<Product[]> {
  const response = await api.get(`/products?query=${encodeURIComponent(query)}`);
  return response.data;
}

export function useProducts(query: string) {
  return useQuery({
    queryKey: ['products', query],
    queryFn: () => fetchProducts(query),
    staleTime: 30_000,
    enabled: query.trim().length > 0,
  });
}
```

Screen:

```tsx
function SearchScreen() {
  const [query, setQuery] = useState('');
  const products = useProducts(query);

  if (products.isLoading) {
    return <LoadingState />;
  }

  if (products.isError) {
    return <ErrorState onRetry={products.refetch} />;
  }

  return (
    <ProductList
      products={products.data ?? []}
      query={query}
      onQueryChange={setQuery}
    />
  );
}
```

Why this is good:
- The screen stays declarative.
- Loading/error/success states are explicit.
- Cache behavior is centralized.
- Refetch and stale-time policy are testable.

---

## 5. Request Race Scenario

Problem:
User types `a`, then `ap`, then `app`. The `a` request returns last and overwrites the newest result.

Solutions:
- Use query keys so each query has separate cache identity.
- Cancel stale requests when supported.
- Track request IDs.
- Ignore responses that do not match the latest query.
- Debounce user input.

Debounce hook:

```tsx
import {useEffect, useState} from 'react';

export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return debounced;
}
```

---

## 6. Forms

Small form:
- `useState` is fine.

Medium/large form:
- Use a form library.
- Use schema validation.
- Keep server errors separate from client validation.
- Disable submit while submitting.
- Handle keyboard and accessibility.

Manual example:

```tsx
type LoginFormState = {
  email: string;
  password: string;
};

function validateLogin(form: LoginFormState) {
  return {
    email: form.email.includes('@') ? undefined : 'Enter a valid email.',
    password: form.password.length >= 8 ? undefined : 'Use at least 8 characters.',
  };
}
```

Production form states:

```text
idle -> editing -> validating -> submitting -> success
                                  -> field_error
                                  -> server_error
```

Interview point:
Form UX is not just validation. It includes keyboard, disabled states, accessibility labels, server errors, retries, and preserving input on failure.

---

## 7. Optimistic Updates

Optimistic update means update UI before server confirms.

Good for:
- Like/unlike.
- Save bookmark.
- Toggle preference.
- Add item to local list.

Risky for:
- Payments.
- Inventory reservation.
- Legal or financial submission.

Flow:

```text
1. Snapshot current cache.
2. Apply optimistic change.
3. Send mutation.
4. On success, reconcile with server response.
5. On failure, rollback or show recovery UI.
```

Interview answer:

```text
I use optimistic updates only when rollback is safe and the user benefit is clear.
For payment or inventory flows, I prefer explicit pending states and server-confirmed
transitions because false success is worse than waiting.
```

---

## 8. Offline And Poor Network

Mobile networks fail often.

Design states:
- No connection.
- Slow connection.
- Request timeout.
- Partial data from cache.
- Retryable failure.
- Non-retryable failure.
- Offline queue pending.
- Conflict on sync.

Offline strategies:
- Cache read-only content.
- Queue safe mutations.
- Use idempotency keys.
- Sync when network returns.
- Show pending state.
- Resolve conflicts with server timestamps/versioning or user choice.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Putting API responses directly in Redux manually | Duplicates server-cache logic | Use a server-state cache |
| No loading/error/empty states | Bad UX | Model every state |
| Overfetching on focus | Wastes battery/data | Use stale-time policy |
| Optimistic payments | Risky false success | Wait for server confirmation |
| No cancellation/debounce for search | Race conditions and API waste | Debounce and key requests |
| Storing secrets in AsyncStorage | Not secure enough | Use secure storage |

---

## 10. Strong Interview Answer

Question:
How do you choose state management in React Native?

Strong answer:

```text
I first classify the state. Local UI state stays in components or reducers.
Server state belongs in a query/cache layer because it needs stale time, retries,
loading states, and invalidation. Auth session, theme, and feature flags can live
in a small global store or context. Secrets must use secure storage, not normal
async storage. For forms, I choose local state for small forms and a form library
with schema validation for complex flows. This keeps state ownership clear and
prevents turning the whole app into one global store.
```

---

## 11. Revision Notes

- One-line summary: Classify state before choosing a tool.
- Three keywords: local, server, persistent.
- One interview trap: Redux is not the default answer for all state.
- One memory trick: Server state needs cache rules; UI state needs locality.

---

## 12. Zustand Deep Dive — Production Patterns

Zustand is the current industry-standard choice for global client state in React Native apps. It replaced Redux in most new production apps because it has zero boilerplate and does not require Provider wrapping.

### Core Pattern

```typescript
import {create} from 'zustand';
import {persist, createJSONStorage} from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Type the entire store upfront
interface CartState {
  // State
  items: CartItem[];
  couponCode: string | null;
  
  // Derived (computed)
  totalPrice: () => number;
  itemCount: () => number;
  
  // Actions
  addItem: (product: Product, quantity?: number) => void;
  removeItem: (productId: string) => void;
  updateQuantity: (productId: string, quantity: number) => void;
  applyCoupon: (code: string) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      couponCode: null,
      
      // Computed getters — use get() to access current state
      totalPrice: () => {
        return get().items.reduce((sum, item) => sum + item.price * item.quantity, 0);
      },
      itemCount: () => get().items.reduce((count, item) => count + item.quantity, 0),
      
      addItem: (product, quantity = 1) => {
        set(state => {
          const existing = state.items.find(i => i.id === product.id);
          if (existing) {
            return {
              items: state.items.map(i =>
                i.id === product.id ? {...i, quantity: i.quantity + quantity} : i
              ),
            };
          }
          return {items: [...state.items, {...product, quantity}]};
        });
      },
      
      removeItem: (productId) => {
        set(state => ({items: state.items.filter(i => i.id !== productId)}));
      },
      
      updateQuantity: (productId, quantity) => {
        set(state => ({
          items: quantity === 0
            ? state.items.filter(i => i.id !== productId)
            : state.items.map(i => i.id === productId ? {...i, quantity} : i),
        }));
      },
      
      applyCoupon: (code) => set({couponCode: code}),
      clearCart: () => set({items: [], couponCode: null}),
    }),
    {
      name: 'cart-storage',                            // AsyncStorage key
      storage: createJSONStorage(() => AsyncStorage),   // persist to device
      partialize: (state) => ({                         // only persist these fields
        items: state.items,
        couponCode: state.couponCode,
      }),
      // actions (functions) are automatically excluded — they cannot be serialized
    }
  )
);
```

### Component Usage — Granular Subscriptions

```tsx
// Subscribe to only what you need — components re-render only when that selector changes
function CartBadge() {
  const itemCount = useCartStore(state => state.itemCount());  // re-renders only when count changes
  if (itemCount === 0) return null;
  return <View style={styles.badge}><Text>{itemCount}</Text></View>;
}

function CartTotal() {
  const totalPrice = useCartStore(state => state.totalPrice());
  return <Text>${totalPrice.toFixed(2)}</Text>;
}

function ProductCard({product}: {product: Product}) {
  const addItem = useCartStore(state => state.addItem);  // action — stable reference, never changes
  return (
    <Pressable onPress={() => addItem(product)}>
      <Text>Add to Cart</Text>
    </Pressable>
  );
}
```

### Zustand DevTools (for debugging)

```typescript
import {devtools} from 'zustand/middleware';

// Wrap your store with devtools middleware to see state changes in Flipper/ReactDevTools
export const useCartStore = create<CartState>()(
  devtools(
    persist(/* ...same as above... */, persistConfig),
    {name: 'CartStore', enabled: __DEV__}  // only enabled in development
  )
);
```

---

## 13. Redux Toolkit vs Zustand Decision Matrix

| Dimension | Zustand | Redux Toolkit |
|---|---|---|
| Boilerplate | Near-zero | Low (RTK reduced it significantly) |
| Bundle size | 1KB | ~50KB (Redux + RTK + Immer) |
| DevTools | Zustand middleware | Redux DevTools (excellent) |
| Time-travel debugging | No | Yes |
| Team familiarity | Modern React teams | Large/enterprise teams (still common) |
| Async actions | Plain async functions | RTK Query (powerful) / createAsyncThunk |
| Middleware | Basic | Full middleware pipeline |
| State normalization | Manual | @reduxjs/toolkit (createEntityAdapter) |
| Server state | Not built-in | RTK Query (cache, refetch, mutations) |

**Pragmatic guidance for interviews:**

```text
Choose Zustand when:
  - Greenfield app with modern React patterns
  - Small-to-medium team that knows React hooks well
  - Server state is handled by TanStack Query (Zustand handles only client state)
  - Simplicity and bundle size matter

Choose Redux Toolkit when:
  - Existing Redux codebase (migration cost vs benefit)
  - Large team that needs strong conventions and DevTools
  - Complex state normalization (RTK's createEntityAdapter)
  - App uses RTK Query for server state as well (single store handles everything)
  - Time-travel debugging is a requirement (e.g., complex transaction flows)
```

---

## 14. React Hook Form — Production Pattern

For complex forms in React Native, `react-hook-form` is the industry standard. It is more performant than Formik because it avoids re-rendering the entire form on every keystroke.

### Why RHF is Better than Formik for Mobile

```text
Formik:
  - Stores all field values in React state
  - Every keystroke → setState → all form fields re-render
  - 10-field form → 10 re-renders per keystroke
  - On Android TextInput with complex validation: visible lag

React Hook Form:
  - Uses uncontrolled inputs with refs by default
  - Only re-renders the field that changed, and only when needed
  - useController bridges RHF and controlled RN components
  - Re-renders entire form only on submit or validation state change
```

### Complete Form Pattern with Zod

```tsx
import {useForm, Controller} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {TextInput, Text, View, Pressable} from 'react-native';

// 1. Define schema with Zod
const registrationSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  confirmPassword: z.string(),
  phoneNumber: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid phone number').optional(),
}).refine(data => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

type RegistrationForm = z.infer<typeof registrationSchema>;

// 2. Form component
function RegistrationForm() {
  const {
    control,
    handleSubmit,
    formState: {errors, isSubmitting, isDirty},
    watch,
    reset,
  } = useForm<RegistrationForm>({
    resolver: zodResolver(registrationSchema),
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
    },
    mode: 'onBlur',   // validate when user leaves a field (not on every keystroke)
  });

  const onSubmit = async (data: RegistrationForm) => {
    try {
      await registerUser(data.email, data.password, data.phoneNumber);
      reset();  // clear form on success
    } catch (error) {
      // handle server errors
    }
  };

  return (
    <View>
      <Controller
        control={control}
        name="email"
        render={({field: {onChange, onBlur, value}}) => (
          <TextInput
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            keyboardType="email-address"
            autoCapitalize="none"
            placeholder="Email address"
          />
        )}
      />
      {errors.email && <Text style={styles.error}>{errors.email.message}</Text>}

      <Controller
        control={control}
        name="password"
        render={({field: {onChange, onBlur, value}}) => (
          <TextInput
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            secureTextEntry
            placeholder="Password"
          />
        )}
      />
      {errors.password && <Text style={styles.error}>{errors.password.message}</Text>}

      <Controller
        control={control}
        name="confirmPassword"
        render={({field: {onChange, onBlur, value}}) => (
          <TextInput
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            secureTextEntry
            placeholder="Confirm password"
          />
        )}
      />
      {errors.confirmPassword && <Text style={styles.error}>{errors.confirmPassword.message}</Text>}

      <Pressable
        onPress={handleSubmit(onSubmit)}
        disabled={isSubmitting || !isDirty}
      >
        <Text>{isSubmitting ? 'Creating Account...' : 'Create Account'}</Text>
      </Pressable>
    </View>
  );
}
```

### Form Library Comparison

| Dimension | React Hook Form | Formik |
|---|---|---|
| Re-renders on input | Uncontrolled — minimal | Controlled — all fields re-render |
| Bundle size | 9KB | 15KB |
| Native TextInput compat | Via Controller | Via handleChange + handleBlur |
| Validation | Any (Zod, Yup, manual) | Any (Yup most common) |
| TypeScript support | Excellent | Good |
| Field arrays | useFieldArray (built-in) | FieldArray (built-in) |
| Community | Large, growing | Large, mature |
| Recommendation | New projects | Existing Formik codebases |

---

## 15. Context API — When It Makes Sense vs When It Does Not

```typescript
// Context IS the right tool for:
//   - Auth state (user, token, login/logout) — read by many components, rarely changes
//   - Theme (dark/light mode) — rarely changes
//   - Feature flags — rarely changes
//   - I18n / locale — changes at most once per session

// Context IS NOT the right tool for:
//   - Shopping cart — changes frequently, would cause many re-renders
//   - Form state — use RHF
//   - Server data — use TanStack Query
//   - Anything that changes more than once per user session

// Optimized AuthContext pattern
const AuthContext = createContext<AuthState | null>(null);

// Separate context for actions to prevent unnecessary re-renders
// (components reading only actions won't re-render when auth state changes)
const AuthActionsContext = createContext<AuthActions | null>(null);

export function AuthProvider({children}: {children: React.ReactNode}) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  
  // Memoize actions to prevent reference changes on re-render
  const actions = useMemo(() => ({
    login: async (email: string, password: string) => { /* ... */ },
    logout: async () => { /* ... */ },
    refreshToken: async () => { /* ... */ },
  }), []);  // empty deps — functions are stable
  
  const state = useMemo(() => ({user, loading}), [user, loading]);
  
  return (
    <AuthContext.Provider value={state}>
      <AuthActionsContext.Provider value={actions}>
        {children}
      </AuthActionsContext.Provider>
    </AuthContext.Provider>
  );
}

// Typed hooks — never call useContext directly in components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};

export const useAuthActions = () => {
  const ctx = useContext(AuthActionsContext);
  if (!ctx) throw new Error('useAuthActions must be used within AuthProvider');
  return ctx;
};
```

---

## 16. Interview Answer Upgrade: "How Do You Manage State in a React Native App?"

```text
I start by classifying state into three categories: server state, local UI state,
and global client state.

Server state — data that lives on the backend and needs caching, background refresh,
and invalidation — goes to TanStack Query. It handles stale-while-revalidate, refetch
on focus, pagination, and mutations with optimistic updates out of the box.

Local UI state — which modal is open, which tab is selected, an individual form field —
stays in useState or useReducer in the component that needs it. Moving this to a
global store is over-engineering.

Global client state — the shopping cart, user preferences, feature flags — goes to
Zustand because it has near-zero boilerplate, does not require Provider wrapping,
and integrates with AsyncStorage persistence via its middleware. I use granular
selectors to prevent unnecessary re-renders.

Auth state and theme go to React Context because they change infrequently and benefit
from the Provider pattern where every deeply-nested component can access them.

I explicitly avoid Redux unless the team already has it or the codebase requires
time-travel debugging or RTK Query for server state. In new projects, TanStack Query
plus Zustand plus Context covers every use case with less code and better performance.
```

