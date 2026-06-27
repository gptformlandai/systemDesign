# React TypeScript Deep Dive — Gold Sheet

> Track File #25 of 40 · Group 2: React Core And Hooks
> Level: beginner TypeScript + React → MAANG-level typed architecture

---

## 1. Intuition

TypeScript in React is not just about adding types. It is about making component APIs self-documenting, making impossible states unrepresentable, and catching entire categories of bugs before runtime.

```text
Untyped React: "What props does this component take?"
Typed React:   "The type signature tells you exactly — no guessing, no runtime surprises."
```

---

## 2. Component Props Typing

### Basic Props

```tsx
// Named function with explicit prop type
type UserCardProps = {
  name: string;
  email: string;
  role: 'admin' | 'user' | 'guest';   // union literal — only these three values
  avatarUrl?: string;                   // optional — can be undefined
  onRoleChange: (newRole: 'admin' | 'user' | 'guest') => void;
};

export function UserCard({name, email, role, avatarUrl, onRoleChange}: UserCardProps) {
  return (
    <div>
      <h2>{name}</h2>
      <p>{email}</p>
      <select value={role} onChange={e => onRoleChange(e.target.value as 'admin' | 'user' | 'guest')}>
        <option value="admin">Admin</option>
        <option value="user">User</option>
        <option value="guest">Guest</option>
      </select>
      {avatarUrl && <img src={avatarUrl} alt={name} />}
    </div>
  );
}
```

### Interface vs Type Alias

```tsx
// Both work for component props — choose one and stick to it
// Type alias: more flexible (supports unions, intersections, mapped types)
type ButtonProps = {
  label: string;
  onClick: () => void;
};

// Interface: extends other interfaces naturally
interface IconButtonProps extends ButtonProps {
  icon: React.ReactNode;
  iconPosition?: 'left' | 'right';
}

// Intersection type — combines two types
type AdminButtonProps = ButtonProps & { requiresConfirmation: boolean };
```

### Extending Native HTML Props

```tsx
// Common pattern: your component IS an HTML element with extra props
type InputFieldProps = React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
};

// This gives you all native input props (value, onChange, disabled, placeholder, etc.)
// PLUS your custom label and error props
export function InputField({label, error, ...inputProps}: InputFieldProps) {
  return (
    <div>
      <label>{label}</label>
      <input {...inputProps} aria-invalid={!!error} />
      {error && <p role="alert">{error}</p>}
    </div>
  );
}

// Usage: all native input props just work
<InputField
  label="Email"
  type="email"
  placeholder="you@example.com"
  value={email}
  onChange={e => setEmail(e.target.value)}
  disabled={isSubmitting}
  error={errors.email}
/>
```

---

## 3. useState Typing

```tsx
// Type is inferred from initial value — usually fine
const [count, setCount] = useState(0);          // number
const [name, setName] = useState('');           // string
const [open, setOpen] = useState(false);        // boolean

// Provide explicit type when initial value is null/undefined
const [user, setUser] = useState<User | null>(null);
const [items, setItems] = useState<Product[]>([]);

// Lazy initialization with explicit type
const [config, setConfig] = useState<AppConfig>(() => loadConfigFromStorage());

// Object state — always create new object on update
type FormState = { email: string; password: string };
const [form, setForm] = useState<FormState>({email: '', password: ''});

// Correct — spread creates new object, triggers re-render
setForm(prev => ({...prev, email: 'new@example.com'}));

// WRONG — mutates existing state, React may not detect update
form.email = 'new@example.com';
setForm(form);
```

---

## 4. useRef Typing

```tsx
// DOM ref — starts null, TypeScript knows it can be null
const inputRef = useRef<HTMLInputElement>(null);

function focusInput() {
  // Must check null — ref is null until component mounts
  inputRef.current?.focus();
  
  // If you are certain it is mounted, use non-null assertion (rare)
  inputRef.current!.focus();
}

// Mutable value ref — not a DOM ref, so start with the value
const countRef = useRef<number>(0);
countRef.current++;  // fine — no null check needed

// Timer ref pattern
const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

useEffect(() => {
  timerRef.current = setInterval(tick, 1000);
  return () => {
    if (timerRef.current) clearInterval(timerRef.current);
  };
}, []);
```

---

## 5. useReducer Typing — State Machines

```tsx
// Full typed state machine for async operations
type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: string };

type AsyncAction<T> =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: T }
  | { type: 'FETCH_ERROR'; error: string }
  | { type: 'RESET' };

function asyncReducer<T>(state: AsyncState<T>, action: AsyncAction<T>): AsyncState<T> {
  switch (action.type) {
    case 'FETCH_START':  return { status: 'loading' };
    case 'FETCH_SUCCESS': return { status: 'success', data: action.payload };
    case 'FETCH_ERROR':  return { status: 'error', error: action.error };
    case 'RESET':        return { status: 'idle' };
    default: {
      // Exhaustiveness check — TypeScript errors if action type is not handled
      const _exhaustive: never = action;
      return state;
    }
  }
}

// Usage
function UserProfile({ userId }: { userId: string }) {
  const [state, dispatch] = useReducer(asyncReducer<User>, { status: 'idle' });
  
  useEffect(() => {
    dispatch({ type: 'FETCH_START' });
    fetchUser(userId)
      .then(data => dispatch({ type: 'FETCH_SUCCESS', payload: data }))
      .catch(err => dispatch({ type: 'FETCH_ERROR', error: err.message }));
  }, [userId]);

  if (state.status === 'loading') return <Spinner />;
  if (state.status === 'error') return <ErrorMessage message={state.error} />;
  if (state.status === 'success') return <UserCard user={state.data} />;
  return null;
}
```

---

## 6. Event Handler Types

```tsx
// All common React event handler types
function FormExample() {
  // onClick — works on any HTML element
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.disabled = true;  // TypeScript knows it is HTMLButtonElement
  };

  // onChange for inputs
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    const checked = event.target.checked;  // for checkboxes
    const files = event.target.files;       // for file inputs
  };

  // onChange for select
  const handleSelect = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const value = event.target.value;
  };

  // onSubmit — forms
  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
  };

  // onKeyDown
  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') submit();
    if (event.key === 'Escape') cancel();
  };

  // Drag events
  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
  };

  return <form onSubmit={handleSubmit}>...</form>;
}

// Short inline handler — TypeScript infers the event type
<input onChange={e => setValue(e.target.value)} />
```

---

## 7. Discriminated Unions — Making Impossible States Impossible

```tsx
// Anti-pattern: independent booleans that create impossible combinations
type BadLoadingState = {
  isLoading: boolean;
  isError: boolean;
  data: User | null;
  // isLoading=true AND isError=true is technically possible — but nonsensical
};

// Better: discriminated union — only valid states exist
type UserState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; user: User }
  | { status: 'error'; message: string };
// Can never be both loading and error simultaneously

// TypeScript narrows the type in each branch
function UserDisplay({ state }: { state: UserState }) {
  switch (state.status) {
    case 'loading':
      return <Spinner />;
    case 'error':
      return <p>{state.message}</p>;  // TypeScript knows message exists here
    case 'success':
      return <UserCard user={state.user} />;  // TypeScript knows user exists here
    default:
      return null;
  }
}
```

```tsx
// UI variant pattern — one component, many looks
type ButtonVariant =
  | { variant: 'primary'; label: string }
  | { variant: 'icon'; icon: React.ReactNode; ariaLabel: string }
  | { variant: 'danger'; label: string; confirmMessage: string };

function Button(props: ButtonVariant & { onClick: () => void }) {
  if (props.variant === 'icon') {
    return <button aria-label={props.ariaLabel} onClick={props.onClick}>{props.icon}</button>;
  }
  if (props.variant === 'danger') {
    return (
      <button onClick={() => confirm(props.confirmMessage) && props.onClick()}>
        {props.label}
      </button>
    );
  }
  return <button onClick={props.onClick}>{props.label}</button>;
}
```

---

## 8. Generic Components

```tsx
// Generic list — works with any data type
type ListProps<T> = {
  items: T[];
  renderItem: (item: T, index: number) => React.ReactNode;
  keyExtractor: (item: T) => string;
  emptyMessage?: string;
};

function List<T>({ items, renderItem, keyExtractor, emptyMessage = 'No items' }: ListProps<T>) {
  if (items.length === 0) return <p>{emptyMessage}</p>;
  return (
    <ul>
      {items.map((item, i) => (
        <li key={keyExtractor(item)}>{renderItem(item, i)}</li>
      ))}
    </ul>
  );
}

// Usage — TypeScript infers T from the items array
<List
  items={products}
  keyExtractor={p => p.id}
  renderItem={p => <ProductCard product={p} />}
/>

// Generic select component
type SelectProps<T extends string | number> = {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
};

function Select<T extends string | number>({ options, value, onChange }: SelectProps<T>) {
  return (
    <select value={String(value)} onChange={e => onChange(e.target.value as T)}>
      {options.map(opt => (
        <option key={String(opt.value)} value={String(opt.value)}>{opt.label}</option>
      ))}
    </select>
  );
}
```

---

## 9. Utility Types in Practice

```tsx
type User = {
  id: string;
  name: string;
  email: string;
  role: 'admin' | 'user';
  passwordHash: string;
  createdAt: Date;
};

// Partial — all fields optional (useful for update payloads)
type UserUpdatePayload = Partial<User>;
// { id?: string; name?: string; ... }

// Pick — select specific fields (useful for display types)
type UserDisplay = Pick<User, 'id' | 'name' | 'email'>;

// Omit — exclude specific fields (remove sensitive data)
type PublicUser = Omit<User, 'passwordHash'>;

// Required — all fields mandatory (override optional)
type RequiredConfig = Required<AppConfig>;

// Record — typed dictionary
type RolePermissions = Record<'admin' | 'user' | 'guest', string[]>;
const permissions: RolePermissions = {
  admin: ['read', 'write', 'delete'],
  user: ['read', 'write'],
  guest: ['read'],
};

// ReturnType — extract return type of a function
const getUser = async () => ({ id: '1', name: 'Alice' });
type UserReturnType = Awaited<ReturnType<typeof getUser>>;
// { id: string; name: string }
```

---

## 10. forwardRef Typing

```tsx
// forwardRef — pass a ref from parent to child DOM element
type InputProps = {
  label: string;
  error?: string;
} & React.InputHTMLAttributes<HTMLInputElement>;

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, ...props }, ref) => {
    return (
      <div>
        <label>{label}</label>
        <input ref={ref} {...props} aria-invalid={!!error} />
        {error && <p>{error}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';  // important for React DevTools

// Parent can now use a ref to focus the input
function LoginForm() {
  const emailRef = useRef<HTMLInputElement>(null);

  return (
    <form>
      <Input ref={emailRef} label="Email" type="email" />
      <button type="button" onClick={() => emailRef.current?.focus()}>
        Focus Email
      </button>
    </form>
  );
}
```

---

## 11. Context API Typing

```tsx
type Theme = 'light' | 'dark';

type ThemeContextValue = {
  theme: Theme;
  toggleTheme: () => void;
};

// Create context with undefined default — forces proper Provider usage
const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>('light');
  
  const toggleTheme = useCallback(() => {
    setTheme(t => t === 'light' ? 'dark' : 'light');
  }, []);
  
  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

// Custom hook with null check — never exposes undefined to callers
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
```

---

## 12. Type Narrowing Patterns

```tsx
// typeof narrowing
function processId(id: string | number) {
  if (typeof id === 'string') {
    return id.toUpperCase();  // TypeScript knows: string here
  }
  return id.toFixed(2);       // TypeScript knows: number here
}

// instanceof narrowing
function handleError(error: unknown) {
  if (error instanceof Error) {
    console.error(error.message);  // TypeScript knows: Error here
  } else if (typeof error === 'string') {
    console.error(error);
  } else {
    console.error('Unknown error');
  }
}

// in narrowing — check if property exists
type Admin = { role: 'admin'; permissions: string[] };
type RegularUser = { role: 'user'; name: string };

function processUser(user: Admin | RegularUser) {
  if ('permissions' in user) {
    // TypeScript knows: Admin here
    console.log(user.permissions);
  }
}

// Type guard function
function isProduct(item: unknown): item is Product {
  return (
    typeof item === 'object' &&
    item !== null &&
    'id' in item &&
    'name' in item &&
    'price' in item
  );
}

// Zod for runtime validation + type inference
import {z} from 'zod';

const ProductSchema = z.object({
  id: z.string(),
  name: z.string().min(1),
  price: z.number().positive(),
  category: z.enum(['electronics', 'clothing', 'books']),
});

type Product = z.infer<typeof ProductSchema>;  // type inferred from schema

async function fetchProduct(id: string): Promise<Product> {
  const res = await fetch(`/api/products/${id}`);
  const data = await res.json();
  return ProductSchema.parse(data);  // throws if API response doesn't match schema
}
```

---

## 13. Common TypeScript Traps in React

| Trap | Example | Fix |
|---|---|---|
| `any` from JSON parse | `const data = JSON.parse(res)` → `any` | Use Zod or cast to a validated type |
| `as` casting without validation | `data as User` — crashes if wrong | Validate with Zod first, then infer type |
| Missing `undefined` in optional props | `props.label.toUpperCase()` | Check `props.label?.toUpperCase()` |
| Event handler typing too broad | `(e: any) => ...` | Use specific `React.MouseEvent<HTMLButtonElement>` |
| Stale closure with non-ref state | Timer reads initial value forever | Use `useRef` or functional update |
| Inferring array as readonly | `const arr = ['a', 'b']` → `string[]` not `['a','b']` | Use `as const` for literal types |
| Generic component arrow function JSX conflict | `<T>` parsed as JSX tag | Use `<T,>` or `function` keyword |

```tsx
// Generic arrow function JSX conflict — use trailing comma
// WRONG (TypeScript parses <T> as JSX)
const identity = <T>(value: T): T => value;

// CORRECT option 1: trailing comma
const identity = <T,>(value: T): T => value;

// CORRECT option 2: function keyword
function identity<T>(value: T): T { return value; }

// as const — preserve literal types
const ROUTES = ['home', 'about', 'contact'] as const;
type Route = typeof ROUTES[number];  // 'home' | 'about' | 'contact'
```

---

## 14. Strong Interview Answer

**Q: How do you use TypeScript to make React components robust?**

```text
I type component props explicitly so the API is self-documenting. I extend native
HTML element types with intersection types so custom components still accept all
native attributes. For complex state, I use discriminated unions rather than
independent booleans — this makes impossible states unrepresentable at the type level.
For event handlers, I use React's specific event types (MouseEvent<HTMLButtonElement>)
rather than any. I validate API responses with Zod so the runtime type matches the
compile-time type. For generics, I use them when a component's logic is the same
regardless of the data shape — typed lists, select components, async wrappers.
The key principle is: TypeScript should catch real bugs, not just add noise.
```

---

## 15. Revision Notes

- Extend `React.InputHTMLAttributes<HTMLInputElement>` to get all native props for free
- Discriminated unions beat `isLoading/isError/data` boolean combos — impossible states become type errors
- Use `Omit<User, 'passwordHash'>` to strip sensitive fields at the type level
- `forwardRef<HTMLInputElement, Props>` — first generic is ref type, second is props type
- Zod: define schema once, infer TypeScript type, validate at runtime — three benefits from one declaration
- Arrow function generic: `<T,>` with trailing comma or use `function` keyword to avoid JSX conflict
- `useReducer` state machines: model each valid state as a union branch — `never` exhaustiveness check ensures all cases handled
