# React Error Handling and Error Boundaries — Gold Sheet

> Track File #28 of 40 · Group 3: React Architecture And Routing
> Level: beginner → MAANG | render errors, async errors, production patterns

---

## 1. Intuition

React has three distinct error zones — each requires a different handling strategy:

```text
Zone 1: Render errors       → Error Boundaries catch these
Zone 2: Async errors        → try/catch + state machine or React Query
Zone 3: Event handler errors → try/catch in the handler
```

The common mistake: assuming one mechanism covers all three zones. An Error Boundary does NOT catch errors in async code, event handlers, or server-side code.

---

## 2. Error Boundaries

Error Boundaries are class components that catch render errors from their children. As of React 18, they must still be class components (no hook equivalent exists yet — React 19 is exploring this).

### Full Production Error Boundary

```tsx
import React, { Component, ErrorInfo } from 'react';

type ErrorBoundaryProps = {
  children: React.ReactNode;
  fallback?: React.ReactNode | ((error: Error, reset: () => void) => React.ReactNode);
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
};

type ErrorBoundaryState = {
  hasError: boolean;
  error: Error | null;
};

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    // Update state so the next render shows the fallback UI
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Log to error tracking service
    this.props.onError?.(error, errorInfo);
    console.error('ErrorBoundary caught:', error, errorInfo.componentStack);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError && this.state.error) {
      const { fallback } = this.props;
      if (typeof fallback === 'function') {
        return fallback(this.state.error, this.handleReset);
      }
      return fallback ?? <DefaultErrorFallback error={this.state.error} onReset={this.handleReset} />;
    }
    return this.props.children;
  }
}

function DefaultErrorFallback({ error, onReset }: { error: Error; onReset: () => void }) {
  return (
    <div role="alert">
      <h2>Something went wrong</h2>
      <pre>{error.message}</pre>
      <button onClick={onReset}>Try Again</button>
    </div>
  );
}

export { ErrorBoundary };
```

### Using ErrorBoundary

```tsx
// Wrap the entire app for crash protection
function App() {
  return (
    <ErrorBoundary onError={(error, info) => reportToSentry(error, info)}>
      <Router />
    </ErrorBoundary>
  );
}

// Wrap individual page sections for graceful degradation
function Dashboard() {
  return (
    <div>
      <Header />  {/* If this crashes, entire page dies */}
      
      <ErrorBoundary fallback={<p>Chart failed to load</p>}>
        <RevenueChart />  {/* If this crashes, only chart shows error */}
      </ErrorBoundary>
      
      <ErrorBoundary
        fallback={(error, reset) => (
          <div>
            <p>User table error: {error.message}</p>
            <button onClick={reset}>Reload Table</button>
          </div>
        )}
      >
        <UsersTable />
      </ErrorBoundary>
    </div>
  );
}
```

### What Error Boundaries DO NOT Catch

```tsx
// 1. Errors in event handlers — use try/catch
function BrokenComponent() {
  const handleClick = () => {
    throw new Error('This is NOT caught by Error Boundary');
    // Error Boundaries only catch render-phase errors
  };
  return <button onClick={handleClick}>Click</button>;
}

// Fix: try/catch in the handler
const handleClick = () => {
  try {
    riskyOperation();
  } catch (error) {
    setError((error as Error).message);
  }
};

// 2. Errors in async code — NOT caught
useEffect(() => {
  fetchData().catch(error => {
    // This error does NOT propagate to Error Boundary
    // Must handle it with state
    setError(error);
  });
}, []);

// 3. Errors in Server Components — NOT caught (Next.js uses error.tsx instead)
// 4. Errors in the Error Boundary itself — NOT caught
```

---

## 3. Async Error Patterns

### State Machine Pattern for Async Errors

```tsx
type FetchState<T> =
  | { phase: 'idle' }
  | { phase: 'loading' }
  | { phase: 'success'; data: T }
  | { phase: 'error'; message: string };

function useProductData(id: string) {
  const [state, setState] = useState<FetchState<Product>>({ phase: 'idle' });

  useEffect(() => {
    let cancelled = false;
    setState({ phase: 'loading' });

    fetchProduct(id)
      .then(data => {
        if (!cancelled) setState({ phase: 'success', data });
      })
      .catch(err => {
        if (!cancelled) setState({ phase: 'error', message: err.message });
      });

    return () => { cancelled = true; };
  }, [id]);

  return state;
}

// Clean component — no conditional isLoading/isError boolean mess
function ProductPage({ id }: { id: string }) {
  const state = useProductData(id);

  switch (state.phase) {
    case 'idle':
    case 'loading': return <ProductSkeleton />;
    case 'error':   return <ErrorMessage message={state.message} />;
    case 'success': return <ProductDetail product={state.data} />;
  }
}
```

### Error Taxonomy for API Calls

```tsx
class NetworkError extends Error { constructor(msg: string) { super(msg); this.name = 'NetworkError'; } }
class ApiError extends Error {
  constructor(public statusCode: number, msg: string) { super(msg); this.name = 'ApiError'; }
}
class ValidationError extends Error { constructor(public fields: Record<string, string>) { super('Validation failed'); this.name = 'ValidationError'; } }

async function apiRequest<T>(url: string, options?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(url, options);
  } catch {
    throw new NetworkError('Network unavailable');
  }

  if (!res.ok) {
    if (res.status === 422) {
      const body = await res.json();
      throw new ValidationError(body.errors);
    }
    if (res.status === 401) throw new ApiError(401, 'Unauthorized');
    if (res.status === 404) throw new ApiError(404, 'Not found');
    throw new ApiError(res.status, `API error ${res.status}`);
  }

  return res.json() as Promise<T>;
}

// Handle different error types differently in UI
async function handleSave() {
  try {
    await apiRequest('/api/posts', { method: 'POST', body: JSON.stringify(formData) });
  } catch (error) {
    if (error instanceof ValidationError) {
      setFieldErrors(error.fields);  // show inline validation errors
    } else if (error instanceof NetworkError) {
      setGlobalError('No internet connection. Changes saved locally.');
    } else if (error instanceof ApiError && error.statusCode === 401) {
      redirectToLogin();
    } else {
      setGlobalError('Something went wrong. Please try again.');
    }
  }
}
```

---

## 4. Next.js Error Handling — error.tsx

In Next.js App Router, `error.tsx` is the file-based Error Boundary for a route segment:

```tsx
// app/dashboard/error.tsx
'use client';  // error.tsx MUST be a Client Component

import { useEffect } from 'react';

type ErrorPageProps = {
  error: Error & { digest?: string };  // digest is a server-side error hash
  reset: () => void;                    // re-render the route segment
};

export default function DashboardError({ error, reset }: ErrorPageProps) {
  useEffect(() => {
    reportToSentry(error);
  }, [error]);

  return (
    <div>
      <h2>Dashboard failed to load</h2>
      <p>{error.message}</p>
      {error.digest && <code>Error ID: {error.digest}</code>}
      <button onClick={reset}>Retry</button>
    </div>
  );
}

// app/dashboard/not-found.tsx — for notFound() calls
export default function DashboardNotFound() {
  return <div><h2>Dashboard not found</h2></div>;
}

// app/global-error.tsx — catches errors in the root layout itself
'use client';
export default function GlobalError({ error, reset }: ErrorPageProps) {
  return (
    <html><body>
      <h1>Application Error</h1>
      <button onClick={reset}>Reload</button>
    </body></html>
  );
}
```

---

## 5. React Query / TanStack Query Error Handling

TanStack Query provides built-in error state — no need for manual try/catch in components:

```tsx
import { useQuery, useMutation } from '@tanstack/react-query';

function ProductPage({ id }: { id: string }) {
  const { data, error, isLoading, isError, refetch } = useQuery({
    queryKey: ['product', id],
    queryFn: () => fetchProduct(id),
    retry: 2,                          // retry failed requests twice before showing error
    retryDelay: attempt => Math.min(1000 * 2 ** attempt, 30000),  // exponential backoff
  });

  if (isLoading) return <Skeleton />;
  if (isError) return (
    <div>
      <p>Failed to load: {(error as Error).message}</p>
      <button onClick={() => refetch()}>Retry</button>
    </div>
  );
  return <ProductDetail product={data} />;
}

// Global error handler for all queries
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      throwOnError: false,  // return error in state — don't propagate to Error Boundary
    },
  },
  queryCache: new QueryCache({
    onError: (error) => {
      if (isUnauthorized(error)) redirectToLogin();
      toast.error(`Query failed: ${error.message}`);
    },
  }),
});
```

---

## 6. Global Error Handler

For errors that slip past everything:

```tsx
// _app.tsx or app root — catch truly unhandled errors
useEffect(() => {
  const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
    console.error('Unhandled promise rejection:', event.reason);
    reportToSentry(event.reason);
  };

  const handleError = (event: ErrorEvent) => {
    console.error('Unhandled error:', event.error);
    reportToSentry(event.error);
  };

  window.addEventListener('unhandledrejection', handleUnhandledRejection);
  window.addEventListener('error', handleError);

  return () => {
    window.removeEventListener('unhandledrejection', handleUnhandledRejection);
    window.removeEventListener('error', handleError);
  };
}, []);
```

---

## 7. Retry with Exponential Backoff

```tsx
async function fetchWithRetry<T>(
  fn: () => Promise<T>,
  maxAttempts = 3,
  baseDelayMs = 500,
): Promise<T> {
  let lastError: Error;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      if (attempt === maxAttempts) break;
      
      const delay = baseDelayMs * 2 ** (attempt - 1);  // 500, 1000, 2000
      const jitter = Math.random() * 200;               // add randomness to avoid thundering herd
      await new Promise(resolve => setTimeout(resolve, delay + jitter));
    }
  }

  throw lastError!;
}
```

---

## 8. Error Boundary with react-error-boundary Library

The `react-error-boundary` npm package is the community standard. It avoids writing class components:

```tsx
import { ErrorBoundary, useErrorBoundary } from 'react-error-boundary';

function ErrorFallback({ error, resetErrorBoundary }: { error: Error; resetErrorBoundary: () => void }) {
  return (
    <div role="alert">
      <p>Error: {error.message}</p>
      <button onClick={resetErrorBoundary}>Reset</button>
    </div>
  );
}

// Wrapping
<ErrorBoundary
  FallbackComponent={ErrorFallback}
  onError={(error, info) => reportToSentry(error, info)}
  onReset={() => queryClient.clear()}
>
  <Dashboard />
</ErrorBoundary>

// Triggering from inside a component — useErrorBoundary
function DataComponent() {
  const { showBoundary } = useErrorBoundary();
  
  async function load() {
    try {
      await fetchData();
    } catch (error) {
      // Push the async error into the nearest Error Boundary
      showBoundary(error);
    }
  }
  // ...
}
```

---

## 9. Common Mistakes

| Mistake | Why Wrong | Fix |
|---|---|---|
| One Error Boundary at app root | Entire app crashes on one widget error | Wrap individual sections too |
| Expecting EB to catch async errors | Doesn't work — different error zone | State machine or `showBoundary` |
| No `onReset` logic | Reset renders same broken state | Clear cache/state on reset |
| Swallowing errors silently | Invisible failures in production | Always log to error service |
| `catch (e) {}` empty | Error disappears silently | At minimum, log: `console.error(e)` |
| Using `error.message` for user display | May contain internal/technical info | Map error types to user-friendly messages |

---

## 10. Strong Interview Answer

**Q: How do you handle errors in a React application?**

```text
I handle errors across three layers. For render errors, I use Error Boundaries —
class components that catch throws during rendering and show a fallback UI.
I place them at both the app level for crash protection and around individual
sections so one broken widget does not kill the whole page. For async errors —
data fetching, API calls — I use discriminated union state machines or TanStack
Query, which has built-in error/retry/refetch state. For event handler errors,
I use try/catch inside the handler and update state to show the error. In
Next.js, I also use error.tsx for route-segment-level error boundaries. The key
production principle is that all errors should be logged to an observability
service like Sentry before they are shown to users.
```

---

## 11. Revision Notes

- Error Boundaries only catch: render errors and errors in lifecycle methods of descendants
- Error Boundaries do NOT catch: async code, event handlers, errors in Server Components, errors in the boundary itself
- Next.js `error.tsx` must be `'use client'` — it IS an Error Boundary
- `react-error-boundary`'s `showBoundary` pushes async errors into the nearest boundary
- State machine (idle/loading/success/error) beats `isLoading + isError + data` booleans
- TanStack Query: `throwOnError: true` in defaultOptions makes queries propagate to Error Boundaries
- Always log to Sentry in `componentDidCatch` or `onError` before rendering fallback
