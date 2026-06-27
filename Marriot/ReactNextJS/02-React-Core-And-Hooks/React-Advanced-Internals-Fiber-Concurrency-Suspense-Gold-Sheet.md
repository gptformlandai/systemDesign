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

---

## 12. Fiber Node Structure

Each React element in the tree corresponds to a **Fiber node** — a JavaScript object tracking component state, effects, and connections.

```
Fiber node (simplified):
{
  type: 'div' | FunctionComponent | ClassComponent,
  key: string | null,
  
  // Tree structure
  return: Fiber,       // parent
  child: Fiber,        // first child
  sibling: Fiber,      // next sibling
  
  // State and effects
  pendingProps: {},
  memoizedProps: {},
  memoizedState: Hook, // linked list of hooks
  updateQueue: Update[],
  flags: number,       // bitmask: Placement | Update | Deletion
  
  // Work tracking
  lanes: Lanes,        // priority of pending work
  alternate: Fiber,    // double buffer — current or work-in-progress
}
```

**Double buffering:** React maintains two trees — the current tree (committed to DOM) and the work-in-progress tree. React builds the work-in-progress tree incrementally. On commit, the trees swap.

---

## 13. The Render Phase Work Loop

```
Root of tree
    │
    ▼ beginWork(fiber)
    │  ← renders component, computes next children
    │  ← can be interrupted (concurrent mode)
    │
    ▼ completeWork(fiber)
    │  ← creates DOM nodes, collects effect flags
    │  ← bubbles up to parent
    │
    ▼ (recursion using sibling/return links, not the call stack)
```

```ts
// Simplified work loop — calls workUnitOfWork repeatedly
function workLoop(shouldYield: () => boolean) {
  while (workInProgress !== null && !shouldYield()) {
    workInProgress = performUnitOfWork(workInProgress);
  }
}

function performUnitOfWork(unitOfWork: Fiber): Fiber | null {
  const next = beginWork(unitOfWork);  // render the component
  if (next === null) {
    completeUnitOfWork(unitOfWork);   // no children left — complete up
  }
  return next;
}
```

**Why Fiber uses iteration instead of recursion:** Recursive call stacks cannot be interrupted. Iterating over a linked list of Fiber nodes can be paused at any `shouldYield()` check and resumed by storing `workInProgress`.

---

## 14. Concurrent Lanes — Priority Model

React 18's scheduler assigns **lanes** to update requests, determining processing order.

```
Lane Priority (high → low):
  SyncLane            ← flushSync, browser event handlers (urgent)
  InputContinuousLane ← continuous input (mouse move, scroll)
  DefaultLane         ← normal setState (deferred events)
  TransitionLane      ← startTransition (user-marked as non-urgent)
  IdleLane            ← background work
```

```tsx
// Assigning transition priority
const [isPending, startTransition] = useTransition();
startTransition(() => {
  setFilter(newValue);  // This update gets TransitionLane — can be interrupted
});

// SyncLane — cannot be interrupted, runs to completion
import { flushSync } from 'react-dom';
flushSync(() => setCount(c => c + 1));
```

**Practical implication for interviews:** `useTransition` tells React that the wrapped update has low priority (TransitionLane). If a high-priority update (user typing) arrives, React can abandon the in-progress TransitionLane render and start fresh with the new input value — keeping the UI responsive.

---

## 15. Suspense Internals — How it Works

```tsx
// When a component throws a Promise, React catches it at the nearest Suspense boundary
async function SuspendingComponent() {
  const data = await fetchData();  // in React 19, use() hook does this
  return <div>{data.name}</div>;
}

// Internally, React checks if thrown value is a Promise (thenable):
function beginWork(fiber) {
  try {
    return renderComponent(fiber);
  } catch (thrownValue) {
    if (typeof thrownValue.then === 'function') {
      // It's a Promise — attach a callback to retry when it resolves
      thrownValue.then(() => scheduleRerender(fiber));
      // Show the nearest Suspense fallback
      return findNearestSuspenseBoundary(fiber).fallback;
    }
    throw thrownValue;  // not a Promise — propagate as error
  }
}
```

**Suspense boundary in the tree:**
```
<Suspense fallback={<Spinner />}>      ← catches thrown Promises from subtree
  <UserProfile />                      ← might throw a Promise
</Suspense>
```

When `UserProfile` throws a Promise:
1. React shows `<Spinner />` fallback immediately
2. When Promise resolves, React re-renders `<UserProfile />`
3. If resolved data is now available, render completes → fallback replaced with content

---

## 16. Why Render Must Be Pure — Concurrent Mode Implication

In concurrent mode, React may:
- Render a component multiple times before committing (abandoned renders)
- Render components "out of order" based on priority
- Pause and resume mid-render

**Consequence:** Any side effect in the render function runs multiple times, at unpredictable times, possibly with stale data.

```tsx
// DANGEROUS: Modifying external state during render
let renderCount = 0;
function Counter() {
  renderCount++;  // BAD: incremented on abandoned renders too
  return <p>{renderCount}</p>;
}

// SAFE: Only read during render, mutate in effects
function Counter() {
  const [count, setCount] = useState(0);
  return <p>{count}</p>;  // pure — just reads and returns
}
```

**Strict Mode double-invoke exposes this:** Strict Mode intentionally renders twice to flush out render-phase side effects. If your component behaves differently on the second render, you have a purity violation.

---

## 17. React Scheduler — `MessageChannel` Trick

React's scheduler uses `MessageChannel` to schedule work between frames without blocking the browser.

```
Frame N: user interaction → React queues work
React posts a message to MessageChannel
Browser handles paints, layout, other events
Browser delivers the message → React processes the work queue
Frame N+1: React commits changes if done, or continues next frame
```

This is why React's concurrent work doesn't block 60fps scrolling — it yields between frames. The scheduler has a 5ms budget per task by default; if work exceeds 5ms, it yields and resumes in the next message loop iteration.

