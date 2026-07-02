# React + Next.js Runnable Mini Labs — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: requirements → starter scaffold → reference solution → self-grading checklist

---

## How to Use

1. Open a new CodeSandbox or local CRA/Vite project
2. Copy the starter scaffold
3. Implement the requirements from scratch before looking at the solution
4. Grade yourself with the checklist

Tools needed: create-react-app, Vite + React, or CodeSandbox.io
For Next.js labs: `npx create-next-app@latest lab --ts --app`

---

## Foundation Labs (5 minutes each)

---

### F1 — Counter with Input and Character Limit

**Goal:** Controlled inputs, conditional rendering, derived state

**Requirements:**
- Text input for a username
- Character limit: 20 characters
- Character counter showing `X / 20`
- Counter turns red when > 15 characters
- Clear button that resets the input
- "Display name" area below that shows what you typed (or "Nothing typed" if empty)

**Starter scaffold:**
```tsx
export function F1Counter() {
  // TODO: add state

  return (
    <div>
      <h2>Username Input</h2>
      <input /* TODO */ />
      <p /* TODO: red class when near limit */>? / 20</p>
      <button /* TODO: clear */>Clear</button>
      <p>{/* TODO: display or fallback */}</p>
    </div>
  );
}
```

**Reference solution:**
```tsx
export function F1Counter() {
  const [name, setName] = useState('');
  const isNearLimit = name.length > 15;
  const isAtLimit = name.length >= 20;

  return (
    <div>
      <h2>Username Input</h2>
      <input
        value={name}
        onChange={e => setName(e.target.value)}
        maxLength={20}
        placeholder="Enter username"
      />
      <p style={{ color: isNearLimit ? 'red' : 'inherit' }}>
        {name.length} / 20
      </p>
      <button onClick={() => setName('')}>Clear</button>
      <p>{name || 'Nothing typed'}</p>
    </div>
  );
}
```

**Checklist:**
- [ ] Input is controlled (value + onChange)
- [ ] Counter updates in real-time
- [ ] Red color when > 15
- [ ] Clear button resets to empty
- [ ] Fallback text shown when empty

---

### F2 — Fetch User List from JSONPlaceholder

**Goal:** useEffect for data fetching, loading and error states

**Requirements:**
- Fetch from `https://jsonplaceholder.typicode.com/users`
- Show a loading spinner/text while fetching
- Show error message if fetch fails
- Show a list of user names and emails when loaded

**Starter scaffold:**
```tsx
export function F2UserList() {
  // TODO: state for users, loading, error

  useEffect(() => {
    // TODO: fetch users
  }, []);

  // TODO: conditional rendering
}
```

**Reference solution:**
```tsx
type User = { id: number; name: string; email: string };

export function F2UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;  // prevent state update after unmount
    
    fetch('https://jsonplaceholder.typicode.com/users')
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(data => {
        if (!cancelled) setUsers(data);
      })
      .catch(err => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, []);

  if (loading) return <p>Loading users...</p>;
  if (error) return <p style={{ color: 'red' }}>Error: {error}</p>;

  return (
    <ul>
      {users.map(user => (
        <li key={user.id}>
          <strong>{user.name}</strong> — {user.email}
        </li>
      ))}
    </ul>
  );
}
```

**Checklist:**
- [ ] Loading state shown on initial render
- [ ] Users displayed after fetch
- [ ] Error handled and shown
- [ ] Cleanup flag prevents state update after unmount
- [ ] Key on each list item

---

### F3 — Toggle Filter List

**Goal:** Multiple state values, filtered derived data

**Requirements:**
- List of 8 fruits: apple, banana, cherry, date, elderberry, fig, grape, honeydew
- "Show only tropical" toggle button (banana, date, fig, honeydew are tropical)
- Filter persists as you toggle
- Show a count of visible items: "Showing X of 8"

**Reference solution:**
```tsx
const ALL_FRUITS = ['apple', 'banana', 'cherry', 'date', 'elderberry', 'fig', 'grape', 'honeydew'];
const TROPICAL = new Set(['banana', 'date', 'fig', 'honeydew']);

export function F3FilterList() {
  const [showTropical, setShowTropical] = useState(false);
  const visible = showTropical ? ALL_FRUITS.filter(f => TROPICAL.has(f)) : ALL_FRUITS;

  return (
    <div>
      <button onClick={() => setShowTropical(p => !p)}>
        {showTropical ? 'Show All' : 'Show Only Tropical'}
      </button>
      <p>Showing {visible.length} of {ALL_FRUITS.length}</p>
      <ul>
        {visible.map(fruit => <li key={fruit}>{fruit}</li>)}
      </ul>
    </div>
  );
}
```

---

## Intermediate Labs (15 minutes each)

---

### I1 — Search with Debounce and Loading Skeleton

**Goal:** Custom hooks, debounce, skeleton loading states

**Requirements:**
- Search input
- Query debounced by 300ms — API only called after user stops typing
- Fetch results from `https://jsonplaceholder.typicode.com/posts?q=<query>`
- Show skeleton cards while loading (not a spinner — placeholder cards)
- Show "No results" if array empty
- Show "Type to search" on initial empty state

**Key concepts to practice:** `useDebounce`, controlled input, conditional rendering

**Reference solution:**
```tsx
function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

function PostSkeleton() {
  return (
    <div style={{ padding: 16, borderRadius: 8, background: '#eee', marginBottom: 8 }}>
      <div style={{ height: 16, width: '60%', background: '#ddd', borderRadius: 4 }} />
      <div style={{ height: 12, width: '80%', background: '#ddd', borderRadius: 4, marginTop: 8 }} />
    </div>
  );
}

export function I1Search() {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);
  const [posts, setPosts] = useState<{ id: number; title: string; body: string }[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!debouncedQuery) { setPosts([]); return; }
    
    setLoading(true);
    fetch(`https://jsonplaceholder.typicode.com/posts?q=${encodeURIComponent(debouncedQuery)}`)
      .then(r => r.json())
      .then(setPosts)
      .finally(() => setLoading(false));
  }, [debouncedQuery]);

  return (
    <div>
      <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search posts..." />
      
      {!query && <p>Type to search</p>}
      
      {loading && Array.from({ length: 3 }).map((_, i) => <PostSkeleton key={i} />)}
      
      {!loading && query && posts.length === 0 && <p>No results</p>}
      
      {!loading && posts.map(post => (
        <div key={post.id} style={{ padding: 16, border: '1px solid #eee', marginBottom: 8 }}>
          <h3>{post.title}</h3>
          <p>{post.body.slice(0, 100)}...</p>
        </div>
      ))}
    </div>
  );
}
```

**Checklist:**
- [ ] Custom `useDebounce` hook in its own function
- [ ] API not called on every keystroke — only after 300ms pause
- [ ] Skeleton cards (not spinner) during loading
- [ ] "Type to search" initial state
- [ ] "No results" when empty

---

### I2 — useReducer Form with Validation

**Goal:** Complex state with useReducer, inline validation

**Requirements:**
- Sign-up form: name (min 2 chars), email (valid format), password (min 8 chars, 1 uppercase)
- Validation on blur (not on every keystroke — too noisy)
- Submit button disabled if any errors
- Show success message after submit
- Clear form on success

**Key concepts to practice:** `useReducer`, form state as a state machine, derived "valid" state

**Reference solution:**
```tsx
type Field = { value: string; error: string; touched: boolean };
type FormState = { name: Field; email: Field; password: Field; submitted: boolean };
type Action =
  | { type: 'CHANGE'; field: keyof Omit<FormState, 'submitted'>; value: string }
  | { type: 'BLUR'; field: keyof Omit<FormState, 'submitted'> }
  | { type: 'SUBMIT' }
  | { type: 'RESET' };

const validate = {
  name: (v: string) => v.length < 2 ? 'Min 2 characters' : '',
  email: (v: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? '' : 'Invalid email',
  password: (v: string) => {
    if (v.length < 8) return 'Min 8 characters';
    if (!/[A-Z]/.test(v)) return 'Must contain uppercase';
    return '';
  },
};

const initial: FormState = {
  name: { value: '', error: '', touched: false },
  email: { value: '', error: '', touched: false },
  password: { value: '', error: '', touched: false },
  submitted: false,
};

function formReducer(state: FormState, action: Action): FormState {
  switch (action.type) {
    case 'CHANGE':
      return { ...state, [action.field]: { ...state[action.field], value: action.value } };
    case 'BLUR':
      return {
        ...state,
        [action.field]: {
          ...state[action.field],
          touched: true,
          error: validate[action.field](state[action.field].value),
        },
      };
    case 'SUBMIT':
      return { ...state, submitted: true };
    case 'RESET':
      return initial;
    default:
      return state;
  }
}

export function I2Form() {
  const [state, dispatch] = useReducer(formReducer, initial);

  const isValid = ['name', 'email', 'password'].every(
    f => !validate[f as keyof typeof validate]((state[f as keyof typeof validate] as Field).value)
  );

  if (state.submitted) {
    return (
      <div>
        <p>Welcome, {state.name.value}!</p>
        <button onClick={() => dispatch({ type: 'RESET' })}>Sign up another</button>
      </div>
    );
  }

  return (
    <form onSubmit={e => { e.preventDefault(); if (isValid) dispatch({ type: 'SUBMIT' }); }}>
      {(['name', 'email', 'password'] as const).map(field => (
        <div key={field}>
          <input
            value={state[field].value}
            onChange={e => dispatch({ type: 'CHANGE', field, value: e.target.value })}
            onBlur={() => dispatch({ type: 'BLUR', field })}
            placeholder={field.charAt(0).toUpperCase() + field.slice(1)}
            type={field === 'password' ? 'password' : 'text'}
          />
          {state[field].touched && state[field].error && <p style={{ color: 'red' }}>{state[field].error}</p>}
        </div>
      ))}
      <button type="submit" disabled={!isValid}>Sign Up</button>
    </form>
  );
}
```

---

## Senior Labs (30 minutes each)

---

### S1 — Infinite Scroll with IntersectionObserver

**Goal:** IntersectionObserver, cursor/page-based pagination, cleanup

**Requirements:**
- Fetch from JSONPlaceholder: `GET /posts?_page=N&_limit=10`
- Render posts in a list
- When user scrolls to the bottom sentinel div, load the next page
- Show "Loading more..." at the bottom while fetching
- Show "All posts loaded" when page > 10 (JSONPlaceholder has 100 posts / 10 per page)
- Guard against double-loading (don't fire 2 requests if user bounces on the sentinel)

**Note:** JSONPlaceholder returns all posts — in a real API you'd check `hasMore` from the response. Treat page > 10 as the end.

**Reference solution:** See `React-NextJS-LLD-Machine-Coding-Design-Gold-Sheet.md` Design 1 — same pattern, simpler API.

**Checklist:**
- [ ] IntersectionObserver on sentinel div
- [ ] Observer disconnected on cleanup
- [ ] Double-fetch guard (loading ref or check)
- [ ] "All posts loaded" terminal state
- [ ] Stable keys (post.id)

---

### S2 — Next.js Product Page (Server Component + Client Island)

**Goal:** App Router, Server/Client split, useOptimistic for wishlist

**Requirements:**
- `app/products/[id]/page.tsx` — Server Component — fetch product by ID
- `components/ProductDetails.tsx` — Client Component — handles "Add to Cart" button
- `components/WishlistButton.tsx` — Client Component — toggle wishlist with `useOptimistic`
- Product details rendered on server (SEO-friendly)
- Add to Cart shows a count badge that updates immediately
- Wishlist toggling uses optimistic update — responds instantly before server confirms

**Starter structure:**
```
app/
  products/
    [id]/
      page.tsx          ← Server Component
components/
  ProductDetails.tsx    ← 'use client'
  WishlistButton.tsx    ← 'use client' + useOptimistic
actions/
  cart.ts              ← 'use server'
  wishlist.ts          ← 'use server'
```

**Key concept:** The page.tsx passes server-fetched data as props to client islands. Client islands handle all interactive behavior.

**Checklist:**
- [ ] `page.tsx` is async — data fetched on server, no `'use client'`
- [ ] `WishlistButton` uses `useOptimistic` for instant UI feedback
- [ ] Server actions in separate `actions/` files
- [ ] No client/server boundary violation (no import of server actions into server components directly)
- [ ] Proper TypeScript types for props
