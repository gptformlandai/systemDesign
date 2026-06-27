# React Core Fundamentals - Gold Sheet

> Track File #2 of 24 - Group 2: React Core And Hooks
> Covers: JSX, components, props, state, function vs class components, lifecycle, reconciliation, keys, controlled/uncontrolled components

---

## 1. Intuition

React is a UI description engine.

```text
state + props -> render output -> React compares -> DOM commit
```

You do not manually update the DOM for every change. You describe what UI should look like for the current data.

---

## 2. JSX

JSX is syntax that lets you write UI-like markup inside JavaScript/TypeScript.

```tsx
type UserCardProps = {
  name: string;
  role: string;
};

export function UserCard({name, role}: UserCardProps) {
  return (
    <article>
      <h2>{name}</h2>
      <p>{role}</p>
    </article>
  );
}
```

Rules:
- Use `className`, not `class`.
- Return one parent element or fragment.
- Use `{}` for JavaScript expressions.
- Components must be pure during render.

---

## 3. Components, Props, And State

Props:
- Inputs from parent.
- Read-only inside child.
- Define component API.

State:
- Local memory for a component.
- Updating state queues a render.
- Should be minimal and derived values should often be calculated, not stored.

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button onClick={() => setCount(count + 1)}>
      Count: {count}
    </button>
  );
}
```

---

## 4. Functional vs Class Components

Modern React uses function components and hooks.

Class lifecycle mapping:

| Class Lifecycle | Function/Hooks Equivalent |
|---|---|
| `componentDidMount` | `useEffect(..., [])` with caveats |
| `componentDidUpdate` | `useEffect(..., [deps])` |
| `componentWillUnmount` | cleanup returned from `useEffect` |
| instance fields | `useRef` or state |
| error boundary | class component or framework boundary |

Interview point:
Do not say `useEffect` is exactly lifecycle. Effects synchronize with external systems after render. Many lifecycle-style effects are unnecessary if state can be derived during render.

---

## 5. React Render Lifecycle

React's public mental model:

```text
trigger -> render -> commit -> browser paint
```

Trigger:
- initial mount
- state update
- parent render
- context change
- external store update

Render:
- React calls components.
- JSX output is calculated.
- Render must be pure.

Commit:
- React applies DOM changes.
- Refs update.
- Effects run after commit.

---

## 6. Reconciliation And Keys

Reconciliation is React's process for comparing previous and next render output.

Keys tell React item identity in lists.

Good:

```tsx
{todos.map(todo => (
  <TodoRow key={todo.id} todo={todo} />
))}
```

Bad:

```tsx
{todos.map((todo, index) => (
  <TodoRow key={index} todo={todo} />
))}
```

Why index keys are dangerous:
- Inserting/removing/reordering changes identity.
- Local row state can attach to the wrong item.
- Inputs can appear to "move" values.

Use index only for static, never-reordered lists.

---

## 7. Controlled vs Uncontrolled Components

Controlled:
- React state owns input value.
- Easier validation and conditional UI.
- More renders for every keystroke.

```tsx
function ControlledEmail() {
  const [email, setEmail] = useState('');

  return (
    <input
      value={email}
      onChange={event => setEmail(event.target.value)}
    />
  );
}
```

Uncontrolled:
- DOM owns current value.
- Read through ref on submit.
- Useful for large/simple forms and file inputs.

```tsx
function UncontrolledEmail() {
  const emailRef = useRef<HTMLInputElement>(null);

  function submit() {
    console.log(emailRef.current?.value);
  }

  return <input ref={emailRef} />;
}
```

---

## 8. Real-World Use Cases

- Controlled form: search box that filters results live.
- Uncontrolled form: large survey managed by React Hook Form.
- Keyed list: ecommerce cart rows.
- Class component: legacy codebase or error boundary.
- Function component: default modern React component style.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Mutating state directly | React may not detect expected update | Create new object/array |
| Side effects in render | Render must be pure | Use event handlers/effects |
| Index keys in dynamic list | Wrong identity | Use stable IDs |
| Storing derived state | Duplication gets stale | Derive during render or memoize |
| Treating props as mutable | Child should not modify parent data directly | Emit events/callbacks |

---

## 10. Strong Interview Answer

Question:
How does React update the UI?

Strong answer:

```text
React updates UI through trigger, render, and commit. A state or prop change
triggers a render. React calls components as pure functions to calculate the next
UI description. Then it reconciles previous and next output and commits only the
necessary DOM changes. Keys are critical for list identity, and bad keys can cause
state to attach to the wrong row.
```

---

## 11. Revision Notes

- One-line summary: React renders pure UI descriptions from props and state.
- Three keywords: JSX, state, reconciliation.
- One interview trap: `useEffect` is not a perfect lifecycle replacement.
- One memory trick: Keys answer "which item is this across renders?"

---

## 12. Batching and flushSync

### Automatic Batching (React 18+)

Before React 18: only state updates inside React event handlers were batched.
After React 18: all state updates are batched automatically, including inside `setTimeout`, Promises, and native event handlers.

```tsx
// React 18 — all 3 updates batched into 1 re-render
setTimeout(() => {
  setA(1);
  setB(2);
  setC(3);
  // React renders ONCE after the timeout, not 3 times
}, 100);

// In a Promise resolution
fetchData().then(() => {
  setLoading(false);
  setData(result);
  // Batched into 1 render in React 18
});
```

### flushSync — Opting Out of Batching

`flushSync` forces React to process state updates synchronously — the DOM is updated before `flushSync` returns. Use rarely — mostly for third-party integrations that read the DOM immediately after state changes.

```tsx
import { flushSync } from 'react-dom';

function handleClick() {
  flushSync(() => {
    setCount(c => c + 1);
    // DOM updated HERE before flushSync returns
  });
  
  // This reads the updated DOM — required for scroll-to-bottom patterns
  bottomRef.current?.scrollIntoView();
}
```

**When to use:** Scroll-to-bottom after appending a message, integrating with non-React DOM measurement libraries.
**When NOT to use:** Most state updates — batching is a performance optimization.

---

## 13. forwardRef and useImperativeHandle

### forwardRef — Passing Refs Through Components

```tsx
// React 18 and below: must use forwardRef
const Input = React.forwardRef<HTMLInputElement, InputProps>((props, ref) => {
  return <input ref={ref} {...props} />;
});

// React 19: ref is just a prop — no forwardRef needed
function Input({ ref, ...props }: InputProps & { ref?: React.Ref<HTMLInputElement> }) {
  return <input ref={ref} {...props} />;
}

// Usage
function Form() {
  const inputRef = useRef<HTMLInputElement>(null);
  return (
    <>
      <Input ref={inputRef} placeholder="Name" />
      <button onClick={() => inputRef.current?.focus()}>Focus Input</button>
    </>
  );
}
```

### useImperativeHandle — Expose Custom Interface

```tsx
interface VideoPlayerRef {
  play: () => void;
  pause: () => void;
  seek: (time: number) => void;
}

const VideoPlayer = React.forwardRef<VideoPlayerRef, VideoPlayerProps>((props, ref) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useImperativeHandle(ref, () => ({
    play: () => videoRef.current?.play(),
    pause: () => videoRef.current?.pause(),
    seek: (time) => { if (videoRef.current) videoRef.current.currentTime = time; },
  }));

  return <video ref={videoRef} src={props.src} />;
});

// Parent only has access to play/pause/seek — not the raw DOM element
function App() {
  const playerRef = useRef<VideoPlayerRef>(null);
  return (
    <>
      <VideoPlayer ref={playerRef} src="/movie.mp4" />
      <button onClick={() => playerRef.current?.play()}>Play</button>
    </>
  );
}
```

---

## 14. Strict Mode Behaviors

`React.StrictMode` adds development-only checks. It does not affect production.

**What Strict Mode does:**
1. Double-invokes components, reducers, and initializers — to detect side effects in render
2. Double-invokes effects (mounts → unmounts → mounts) — to detect missing cleanup
3. Warns about deprecated lifecycle methods and legacy patterns

```tsx
// app/layout.tsx — Next.js enables Strict Mode by default
// next.config.ts — to disable:
const nextConfig = {
  reactStrictMode: false,  // not recommended
};

// What you'll see in development: effects run twice
useEffect(() => {
  const ws = new WebSocket(url);  // opens, closes, opens — cleanup MUST work
  return () => ws.close();
}, []);
```

**Why the double-invoke is valuable:** If your component breaks when mounted twice, you have missing cleanup. Strict Mode makes this visible in development before it becomes a production bug.

---

## 15. Component Composition Patterns

### Children as Configuration (Inversion of Control)

```tsx
// Instead of accepting many props to configure layout:
// <Card title="..." body="..." footer="..." image="..." />

// Accept children — caller controls layout:
function Card({ children }: { children: React.ReactNode }) {
  return <div className="card">{children}</div>;
}
Card.Header = ({ children }: { children: React.ReactNode }) => <div className="card-header">{children}</div>;
Card.Body = ({ children }: { children: React.ReactNode }) => <div className="card-body">{children}</div>;
Card.Footer = ({ children }: { children: React.ReactNode }) => <div className="card-footer">{children}</div>;

// Usage — flexible, no prop explosion
<Card>
  <Card.Header><h2>Title</h2><Badge>New</Badge></Card.Header>
  <Card.Body><p>Content here</p></Card.Body>
  <Card.Footer><button>Action</button></Card.Footer>
</Card>
```

### Render Props

```tsx
// Component provides data, caller decides rendering
function MouseTracker({ render }: { render: (pos: { x: number; y: number }) => React.ReactNode }) {
  const [pos, setPos] = useState({ x: 0, y: 0 });
  return (
    <div onMouseMove={e => setPos({ x: e.clientX, y: e.clientY })}>
      {render(pos)}
    </div>
  );
}

// Usage
<MouseTracker render={pos => <p>Mouse at {pos.x}, {pos.y}</p>} />

// Modern alternative: custom hook (usually preferred)
function useMousePosition() {
  const [pos, setPos] = useState({ x: 0, y: 0 });
  useEffect(() => {
    const handler = (e: MouseEvent) => setPos({ x: e.clientX, y: e.clientY });
    window.addEventListener('mousemove', handler);
    return () => window.removeEventListener('mousemove', handler);
  }, []);
  return pos;
}
```

### HOC (Higher-Order Component) — When Still Useful

```tsx
// Mostly replaced by hooks, but still seen for cross-cutting concerns
function withAuth<P extends object>(Component: React.ComponentType<P>) {
  return function AuthGuard(props: P) {
    const { user, loading } = useAuth();
    if (loading) return <Spinner />;
    if (!user) return <Navigate to="/login" />;
    return <Component {...props} />;
  };
}
const ProtectedDashboard = withAuth(Dashboard);
```

---

## 16. React 19 Changes Summary

| Feature | Change |
|---|---|
| `ref` as prop | `forwardRef` no longer needed — pass `ref` as a regular prop |
| `use()` hook | Read promises and context inside components, even in conditions |
| `useActionState` | Renamed from `useFormState`, works with Server Actions |
| `useOptimistic` | Stable API for optimistic UI without external libraries |
| `useFormStatus` | Read parent form's pending state from child components |
| Error handling | `onCaughtError`, `onUncaughtError`, `onRecoverableError` callbacks on `createRoot` |
| Compiler | React Compiler (opt-in) auto-memoizes — `useMemo`/`useCallback` become less necessary |

---

## 17. Common Mistakes Table

| Mistake | Consequence | Fix |
|---|---|---|
| Mutating state directly (`arr.push()`) | No re-render triggered | Return new array: `[...arr, item]` |
| Setting state in render | Infinite loop | Move to event handler or useEffect |
| Deriving state from props in useState | State goes stale when props update | Derive in render body or use useEffect to sync |
| Not unique key in list | Reconciler confusion, wrong component updates | Use stable data ID, never index |
| `forwardRef` in React 18 without typing | TypeScript errors | `React.forwardRef<ElementType, PropsType>` |
| Direct DOM manipulation without ref | React's tree diverges from actual DOM | Only touch DOM through refs |

