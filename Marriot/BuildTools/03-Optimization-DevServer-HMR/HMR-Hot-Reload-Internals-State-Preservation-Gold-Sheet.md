# HMR: Hot Reload Internals and State Preservation Gold Sheet

> Topic: full reload vs module replacement, HMR internals, and state preservation issues.

---

## 1. Intuition

HMR is like changing one part of a running machine without switching the whole machine off. Instead of refreshing the page, the dev server sends just the changed module and asks the app to accept it.

Beginner version:

> HMR updates changed code in the running app without a full reload when it is safe.

---

## 2. Definition

- Definition: Hot Module Replacement updates changed modules in a running application while attempting to preserve runtime state.
- Category: Development-time runtime patching.
- Core idea: Replace the smallest safe piece of code.

---

## 3. Full Reload vs HMR

| Behavior | What Happens | State |
|---|---|---|
| Full reload | Browser reloads the whole page | Lost |
| HMR | Runtime swaps changed module | Often preserved |
| React Fast Refresh | React-aware HMR for components | Preserved when component signatures are compatible |

Full reload:

```txt
save file
  -> rebuild
  -> browser reloads
  -> app starts from scratch
```

HMR:

```txt
save file
  -> rebuild changed module
  -> send update
  -> runtime replaces module
  -> app re-renders affected area
```

---

## 4. How HMR Works Internally

```txt
Source file changed
       |
       v
File watcher event
       |
       v
Dev server invalidates module
       |
       v
Transform updated module
       |
       v
Find HMR boundary
       |
       v
Send update over WebSocket
       |
       v
Client runtime imports updated module
       |
       v
Accept update or reload page
```

An HMR boundary is a module that can safely accept an update.

---

## 5. HMR Boundary Example

Conceptual HMR API:

```js
import { render } from './renderApp';

render();

if (import.meta.hot) {
  import.meta.hot.accept('./renderApp', () => {
    render();
  });
}
```

In React apps, framework tooling usually wires this for you. You rarely write it manually unless building custom tooling.

---

## 6. React Fast Refresh

React Fast Refresh is React-aware hot reloading.

It tries to preserve state when:

- The file exports React components.
- Hook order remains compatible.
- Component identity can be tracked.

It may reset state when:

- You change hook order.
- You switch component type meaningfully.
- The file exports non-component values used outside React boundaries.
- The update affects module initialization logic.

---

## 7. State Preservation Problems

Example:

```tsx
export function Counter() {
  const [count, setCount] = useState(0);

  return <button onClick={() => setCount(count + 1)}>{count}</button>;
}
```

Changing button text usually preserves state.

But changing hook order can force reset or error:

```tsx
export function Counter({ enabled }: { enabled: boolean }) {
  if (enabled) {
    const [count, setCount] = useState(0);
    return <button onClick={() => setCount(count + 1)}>{count}</button>;
  }

  return null;
}
```

This also violates React hook rules.

---

## 8. CSS HMR

CSS updates are often easier:

```txt
save CSS
  -> dev server transforms CSS
  -> browser updates style tag/link
  -> no component state reset
```

That is why style changes often feel instant.

---

## 9. React, Next.js, React Native Notes

### React Web

Vite and Webpack dev servers use an HMR client runtime to update modules. React Fast Refresh handles component updates.

### Next.js

Next.js coordinates Fast Refresh with route compilation, server/client boundaries, and framework runtime state.

### React Native

Fast Refresh is part of the React Native dev experience through Metro. It updates JavaScript on the device while preserving React state when safe.

---

## 10. Tool Comparison Lens

| Tool | HMR / Refresh Model |
|---|---|
| Webpack | HMR runtime plus framework integrations |
| Vite | Native ESM update protocol with HMR client |
| Parcel | Built-in HMR for supported asset types |
| Next.js | Fast Refresh integrated with framework routing and boundaries |
| Metro | React Native Fast Refresh on device/emulator |

---

## 11. Real-World Example

You are editing a profile screen:

```txt
change ProfileCard.tsx style
  -> HMR update
  -> form input state remains

change auth provider setup
  -> update may affect root module
  -> full reload likely
```

The closer a change is to the root runtime setup, the more likely a full reload becomes.

---

## 12. Common Mistakes

### Mistake: Depending on HMR state as proof the app works

- Why wrong: HMR can preserve state that would be reset on real navigation or reload.
- Better approach: manually reload and test real startup flows.

### Mistake: Exporting mixed component and non-component values from the same file

- Why wrong: It can confuse React Fast Refresh boundaries.
- Better approach: keep component modules clean when possible.

### Mistake: Ignoring module-level side effects

- Why wrong: HMR may rerun initialization code.
- Better approach: make setup idempotent or isolate it.

### Mistake: Treating HMR bugs as production bugs immediately

- Why wrong: HMR runtime is development-only.
- Better approach: reproduce in production build before over-fixing.

---

## 13. Trade-Offs

| HMR Design | Benefit | Cost |
|---|---|---|
| Preserve state | Faster UI iteration | Can hide startup bugs |
| Full reload fallback | Correctness | Slower feedback |
| Fine-grained updates | Less disruption | More boundary logic |
| React-aware refresh | Better component editing | Depends on component signature rules |

---

## 14. Interview Insight

Strong answer:

> HMR is a dev-time protocol between the file watcher, dev server, and browser runtime. When a module changes, the server transforms it, finds affected graph boundaries, sends an update over a socket, and the client runtime either accepts the replacement or reloads the page. React Fast Refresh adds React-specific state preservation rules.

Follow-up trap:

> Why did my state reset after a hot update?

Good answer:

> The update likely crossed an unsafe boundary, changed a component signature, changed hook order, or affected module initialization. HMR preserves state only when the runtime can prove the update is safe enough.

---

## 15. React Fast Refresh Internals

React Fast Refresh (shipped in React 17+, used by Vite and Next.js) is React-aware HMR:

**How it works:**
1. Babel/SWC transform wraps every component export with a `register()` call
2. On file change, the new component is registered with the same key
3. React compares old and new component — if it's a "safe" update, it re-renders preserving state

```typescript
// React Fast Refresh transform output (simplified)
// Original:
export function Counter() { ... }

// After Fast Refresh transform:
import { register } from 'react-refresh/runtime';
export function Counter() { ... }
register(Counter, 'Counter');  // key = component name in source
```

**Update tiers:**
1. **Hot swap (state preserved)**: Component code changed, hook order same, no runtime error
2. **Module replacement (state lost, no page reload)**: Incompatible changes detected
3. **Full page reload**: Runtime error during re-render

**The "safe" update check:** If the signature of hooks changed (different count, different hook calls), Fast Refresh drops component state for that component and re-renders with initial state. This prevents stale state from invalid hook assumptions.

---

## 16. Vite HMR API (`import.meta.hot`)

```typescript
// A Redux store that supports HMR cleanup
import { configureStore } from '@reduxjs/toolkit';
import rootReducer from './rootReducer';

export const store = configureStore({ reducer: rootReducer });

// HMR for the store — replace reducer without losing state
if (import.meta.hot) {
  import.meta.hot.accept('./rootReducer', (newModule) => {
    if (newModule) {
      store.replaceReducer(newModule.default);
    }
  });

  // Cleanup when this module is replaced (called before new version loads)
  import.meta.hot.dispose((data) => {
    // Save state across HMR boundary
    data.savedState = store.getState();
  });
}

// Restore state from previous HMR version
if (import.meta.hot?.data?.savedState) {
  store.dispatch({ type: 'RESTORE_STATE', payload: import.meta.hot.data.savedState });
}
```

**When to use the Vite HMR API:**
- Redux/Zustand stores that should survive module hot-swaps
- WebSocket connections that should close and reconnect on file change
- Long-polling intervals that should be cleared between HMR updates

---

## 17. Revision Notes

- One-line summary: HMR replaces changed modules at runtime during development.
- Three keywords: boundary, socket, refresh.
- One interview trap: Preserved HMR state can hide real reload behavior.
- Memory trick: HMR patches a running app, reload restarts it.
