# React Advanced Internals, Fiber, Concurrency, Suspense - Gold Sheet

> Track File #4 of 24 - Group 2: React Core And Hooks
> Covers: Fiber, concurrent rendering, batching, Suspense, lazy loading, error boundaries, portals

---

## 1. Intuition

React Fiber is the internal architecture that lets React split rendering work, pause it, resume it, prioritize it, and commit only completed work.

Simple mental model:

```text
urgent updates: typing, clicks
less urgent updates: filtering big list, route transition
```

Concurrent rendering helps React keep the UI responsive by preparing work without immediately blocking every interaction.

---

## 2. Fiber Architecture

High-level only:
- A Fiber is React's internal unit of work for a component.
- React can traverse the Fiber tree to compute updates.
- Work can be prioritized.
- Render work can be interrupted before commit.
- Commit phase is not partially applied. React commits completed changes.

Interview safety:
You do not need to explain private implementation fields. Explain scheduling, prioritization, and interruptible rendering.

---

## 3. Concurrent Rendering

Concurrent rendering means React can prepare multiple UI versions and prioritize urgent work.

Examples:
- Keep typing responsive while filtering list.
- Show pending route transition.
- Defer non-urgent UI updates.

Useful APIs:

```tsx
const [isPending, startTransition] = useTransition();

function onSearch(nextQuery: string) {
  setInput(nextQuery);
  startTransition(() => {
    setQuery(nextQuery);
  });
}
```

Trade-off:
Concurrent rendering makes render purity more important. Impure renders may run more than once or be abandoned.

---

## 4. Batching Updates

React batches multiple state updates to reduce renders.

```tsx
function handleClick() {
  setCount(current => current + 1);
  setOpen(true);
  setStatus('saved');
}
```

Instead of committing after every setter, React can batch and commit once.

Interview insight:
State setters do not synchronously change variables in the current render. They queue work for a future render.

---

## 5. Suspense And Lazy Loading

`React.lazy` loads component code on demand.

```tsx
const SettingsPage = lazy(() => import('./SettingsPage'));

export function App() {
  return (
    <Suspense fallback={<Spinner />}>
      <SettingsPage />
    </Suspense>
  );
}
```

Suspense lets React show fallback UI while something is not ready:
- code split component
- framework-supported data loading
- server component streaming boundaries

Next.js uses Suspense heavily for streaming and route segment loading.

---

## 6. Error Boundaries

Error boundaries catch render-time errors below them.

Class example:

```tsx
class ErrorBoundary extends React.Component<
  {children: React.ReactNode},
  {hasError: boolean}
> {
  state = {hasError: false};

  static getDerivedStateFromError() {
    return {hasError: true};
  }

  componentDidCatch(error: Error) {
    reportError(error);
  }

  render() {
    if (this.state.hasError) {
      return <Fallback />;
    }
    return this.props.children;
  }
}
```

Next.js App Router also provides route-level `error.tsx` boundaries.

---

## 7. Portals

Portals render children into a different DOM node while preserving React parent relationship.

```tsx
import {createPortal} from 'react-dom';

export function Modal({children}: {children: React.ReactNode}) {
  return createPortal(
    <div role="dialog" aria-modal="true">{children}</div>,
    document.body,
  );
}
```

Use for:
- modals
- popovers
- tooltips
- global overlays

Trap:
DOM placement changes, but React event bubbling follows the React tree.

---

## 8. Real-World Use Cases

- Suspense boundary around slow dashboard widget.
- Lazy route chunks for admin pages.
- Error boundary around payment form.
- Portal for modal/dialog system.
- Transition for expensive filtering or route state.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Side effects during render | Concurrent render may restart | Keep render pure |
| One giant Suspense boundary | Whole page fallback | Use meaningful boundaries |
| No error boundaries | One render crash can blank app | Add route/widget boundaries |
| Lazy loading everything | Too many waterfalls | Split at route/heavy feature level |
| Misunderstanding portals | DOM tree differs from React tree | Handle focus/aria carefully |

---

## 10. Strong Interview Answer

Question:
Explain Fiber and concurrent rendering at a high level.

Strong answer:

```text
Fiber is React's internal architecture for representing component work as units
that can be scheduled and prioritized. With concurrent rendering, React can prepare
updates without immediately blocking urgent interactions, and it can abandon
unfinished render work before committing. This is why render functions must stay
pure. The commit phase applies completed changes to the DOM, while Suspense,
transitions, and lazy loading help control what users see while work is pending.
```

---

## 11. Revision Notes

- One-line summary: Fiber lets React schedule and prioritize UI work.
- Three keywords: scheduling, Suspense, boundaries.
- One interview trap: Concurrent rendering does not mean parallel DOM mutation.
- One memory trick: Render work can be interrupted; commit is where UI changes become real.

