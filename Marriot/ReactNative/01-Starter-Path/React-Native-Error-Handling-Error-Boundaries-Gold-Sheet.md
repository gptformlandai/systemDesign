# React Native Error Handling & Error Boundaries — Gold Sheet

> Track File #6 of 37 · Group 1: Starter Path
> Level: beginner to interview-ready | Mode: handle errors safely in every layer

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Error Boundaries — what they catch and what they don't | Very high | One of the most asked React senior topics |
| async errors in useEffect — they bypass Error Boundaries | Very high | Common beginner trap that causes silent failures |
| try/catch in event handlers | High | Must handle network errors in mobile |
| Network error taxonomy | High | Production mobile apps need clear error categories |
| Graceful degradation vs crash | High | UX and interview signal |
| Global error handler setup | Medium | Required for production crash safety |
| Error state design patterns | High | Loading/error/empty state is asked in every mobile interview |

---

## 2. The Three Error Layers in React Native

```text
Layer 1: JavaScript render errors
  → Error thrown inside render, during component tree evaluation
  → Caught by: React Error Boundaries
  → Behavior without boundary: red screen in development, white/black screen in production

Layer 2: Async errors (in useEffect, event handlers, custom hooks)
  → Error thrown in setTimeout, fetch, async functions, callbacks
  → NOT caught by: Error Boundaries
  → Must handle with: try/catch + setState, .catch(), or Promise rejection handlers

Layer 3: Native / unhandled errors
  → Native exceptions, OOM, unhandled Promise rejections
  → Caught by: global error handlers (Sentry, ErrorUtils)
  → Result: app crash with native crash report
```

---

## 3. Error Boundaries — Full Mental Model

An Error Boundary is a class component that catches JavaScript errors in its child component tree during render, lifecycle methods, and constructor calls.

### Why Class Component?

Error Boundaries require two class lifecycle methods (`getDerivedStateFromError` and `componentDidCatch`) that have no hook equivalents. You always write them as class components. React 19 is working on `use(Promise)` and `useError` patterns, but class components remain the standard.

### The Standard Error Boundary

```tsx
import React from 'react';
import {View, Text, Pressable, StyleSheet} from 'react-native';

type Props = {
  children: React.ReactNode;
  fallback?: React.ReactNode;
  onError?: (error: Error, info: React.ErrorInfo) => void;
};

type State = {hasError: boolean; error: Error | null};

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = {hasError: false, error: null};

  // Called during render when child throws
  // Return new state — this is the only correct way to update state here
  static getDerivedStateFromError(error: Error): State {
    return {hasError: true, error};
  }

  // Called after render with error details — good for logging
  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, info.componentStack);
    this.props.onError?.(error, info);
    // Send to crash reporter: Sentry.captureException(error);
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback ?? (
          <View style={styles.container}>
            <Text style={styles.title}>Something went wrong</Text>
            <Text style={styles.message}>{this.state.error?.message}</Text>
            <Pressable
              style={styles.button}
              onPress={() => this.setState({hasError: false, error: null})}>
              <Text style={styles.buttonText}>Try again</Text>
            </Pressable>
          </View>
        )
      );
    }
    return this.props.children;
  }
}

const styles = StyleSheet.create({
  container: {flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24},
  title: {fontSize: 20, fontWeight: 'bold', marginBottom: 8},
  message: {fontSize: 14, color: '#666', textAlign: 'center', marginBottom: 24},
  button: {backgroundColor: '#007AFF', paddingHorizontal: 24, paddingVertical: 12, borderRadius: 8},
  buttonText: {color: '#fff', fontWeight: '600'},
});
```

### Using Error Boundaries

Granular boundaries give better UX than one global boundary:

```tsx
// Coarse — whole app shows error screen on any error
function App() {
  return (
    <ErrorBoundary>
      <NavigationContainer>
        <RootNavigator />
      </NavigationContainer>
    </ErrorBoundary>
  );
}

// Fine-grained — only the broken section shows error, rest of app works
function HomeScreen() {
  return (
    <View style={{flex: 1}}>
      <ErrorBoundary fallback={<Text>Failed to load recommendations</Text>}>
        <RecommendedProducts />
      </ErrorBoundary>
      <ErrorBoundary fallback={<Text>Failed to load promotions</Text>}>
        <PromoBanner />
      </ErrorBoundary>
      <CartSummary /> {/* No boundary — will bubble up if crashes */}
    </View>
  );
}
```

---

## 4. What Error Boundaries Do NOT Catch

This is the most important trap. Error Boundaries only catch errors during:
- Rendering
- Constructor
- Static lifecycle methods of child components

They do NOT catch:
- Errors in event handlers (`onPress`, `onChangeText`)
- Errors in `useEffect` and `useLayoutEffect` callbacks
- Errors in async code (`setTimeout`, `fetch`, `async/await`)
- Errors in the error boundary itself

```tsx
// This throws inside render — ErrorBoundary DOES catch this
function BrokenComponent({data}: {data: any}) {
  return <Text>{data.nonExistentProperty.nested}</Text>; // TypeError caught
}

// This throws in an event handler — ErrorBoundary does NOT catch this
function SearchBar() {
  const handlePress = () => {
    throw new Error('boom'); // NOT caught by ErrorBoundary — must try/catch here
  };
  return <Pressable onPress={handlePress}><Text>Search</Text></Pressable>;
}

// This throws in useEffect — ErrorBoundary does NOT catch this
function DataLoader() {
  useEffect(() => {
    throw new Error('async boom'); // NOT caught — must try/catch inside useEffect
  }, []);
}
```

---

## 5. Async Error Handling Patterns

### Pattern 1: try/catch inside useEffect

```tsx
function UserProfile({userId}: {userId: string}) {
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchUser(userId);
        if (!cancelled) setUser(data);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Unknown error');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [userId]);
}
```

### Pattern 2: Error state machine with useReducer

```tsx
type State =
  | {status: 'idle'}
  | {status: 'loading'}
  | {status: 'success'; data: User}
  | {status: 'error'; message: string};

type Action =
  | {type: 'LOAD'}
  | {type: 'SUCCESS'; data: User}
  | {type: 'ERROR'; message: string}
  | {type: 'RESET'};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'LOAD': return {status: 'loading'};
    case 'SUCCESS': return {status: 'success', data: action.data};
    case 'ERROR': return {status: 'error', message: action.message};
    case 'RESET': return {status: 'idle'};
    default: return state;
  }
}

// In component
const [state, dispatch] = useReducer(reducer, {status: 'idle'});

const load = useCallback(async () => {
  dispatch({type: 'LOAD'});
  try {
    const data = await fetchUser(userId);
    dispatch({type: 'SUCCESS', data});
  } catch (err) {
    dispatch({type: 'ERROR', message: err instanceof Error ? err.message : 'Failed'});
  }
}, [userId]);
```

### Pattern 3: Network error taxonomy

```tsx
type AppError =
  | {kind: 'network'; message: string}       // No internet
  | {kind: 'server'; status: number; message: string}  // 4xx, 5xx
  | {kind: 'parse'; message: string}          // Bad JSON
  | {kind: 'unknown'; message: string};

async function safeFetch<T>(url: string, options?: RequestInit): Promise<T> {
  let response: Response;

  try {
    response = await fetch(url, options);
  } catch (err) {
    // Network error — no connection, DNS failure, timeout
    throw {kind: 'network', message: 'No internet connection'} as AppError;
  }

  if (!response.ok) {
    throw {
      kind: 'server',
      status: response.status,
      message: `Server error: ${response.status}`,
    } as AppError;
  }

  try {
    return await response.json() as T;
  } catch {
    throw {kind: 'parse', message: 'Invalid response format'} as AppError;
  }
}

// In component — handle specific error types
const handleError = (err: AppError) => {
  switch (err.kind) {
    case 'network':
      showToast('Check your internet connection');
      break;
    case 'server':
      if (err.status === 401) logout();
      else if (err.status >= 500) showToast('Server error — try again later');
      break;
    case 'parse':
      logError(err); // silent — user does not need to know
      break;
  }
};
```

---

## 6. Loading / Error / Empty State Pattern

Every screen that fetches data needs four states. This is a standard interview answer:

```tsx
function ProductListScreen() {
  const {data, loading, error, refetch} = useProducts();

  // 1. Loading state
  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#007AFF" />
        <Text style={styles.hint}>Loading products...</Text>
      </View>
    );
  }

  // 2. Error state — with recovery action
  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorIcon}>⚠️</Text>
        <Text style={styles.errorTitle}>Could not load products</Text>
        <Text style={styles.errorMessage}>{error}</Text>
        <Pressable style={styles.retryButton} onPress={refetch}>
          <Text style={styles.retryText}>Try Again</Text>
        </Pressable>
      </View>
    );
  }

  // 3. Empty state — data loaded but nothing to show
  if (data.length === 0) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyIcon}>📦</Text>
        <Text style={styles.emptyTitle}>No products found</Text>
        <Text style={styles.emptyMessage}>Try a different search</Text>
      </View>
    );
  }

  // 4. Success state
  return (
    <FlatList
      data={data}
      keyExtractor={item => item.id}
      renderItem={({item}) => <ProductCard product={item} />}
    />
  );
}
```

---

## 7. Global Error Handler for Unhandled Rejections

In production, unhandled Promise rejections do not crash the app visibly — they silently fail. Set up global handlers:

```tsx
// In your app entry point (App.tsx or index.ts)
import {ErrorUtils} from 'react-native';

// Catch any unhandled JS error
const originalGlobalHandler = ErrorUtils.getGlobalHandler();
ErrorUtils.setGlobalHandler((error: Error, isFatal: boolean) => {
  console.error('Global error caught:', error, 'fatal:', isFatal);
  // Report to Sentry: Sentry.captureException(error);
  if (isFatal) {
    // Show a user-friendly crash screen or restart the app
  }
  originalGlobalHandler(error, isFatal);
});

// Catch unhandled Promise rejections
const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
  console.error('Unhandled promise rejection:', event.reason);
  // Sentry.captureException(event.reason);
};
```

With Sentry (production standard):
```tsx
import * as Sentry from '@sentry/react-native';

Sentry.init({
  dsn: 'your-dsn',
  environment: __DEV__ ? 'development' : 'production',
  tracesSampleRate: 0.2,
});

// Wrap root component
export default Sentry.wrap(App);
```

---

## 8. Retry Pattern with Exponential Backoff

Network errors in mobile are common (flaky connections, tunnels, subways). Always build retry:

```tsx
type RetryConfig = {
  maxAttempts: number;
  baseDelayMs: number;
  retryOn?: (error: unknown) => boolean;
};

async function withRetry<T>(
  fn: () => Promise<T>,
  config: RetryConfig,
): Promise<T> {
  const {maxAttempts, baseDelayMs, retryOn = () => true} = config;

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (err) {
      const isLast = attempt === maxAttempts - 1;
      const shouldRetry = retryOn(err);

      if (isLast || !shouldRetry) throw err;

      const delay = baseDelayMs * 2 ** attempt + Math.random() * 200;
      await new Promise(resolve => setTimeout(resolve, Math.min(delay, 30_000)));
    }
  }
  throw new Error('Unreachable');
}

// Usage — retry network errors but not auth errors
const user = await withRetry(() => fetchUser(id), {
  maxAttempts: 3,
  baseDelayMs: 1000,
  retryOn: err => {
    if (err && typeof err === 'object' && 'kind' in err) {
      return (err as AppError).kind === 'network'; // retry network, not server
    }
    return true;
  },
});
```

---

## 9. Common Error Handling Traps

### Trap 1: Swallowing errors silently

```tsx
// Wrong — error is eaten, user sees nothing, developer sees nothing
try {
  await saveOrder(order);
} catch {}

// Correct — always log at minimum, ideally show feedback
try {
  await saveOrder(order);
} catch (err) {
  logError(err); // to Sentry / analytics
  showToast('Could not save order — please try again');
}
```

### Trap 2: Mixing async errors with Error Boundaries

```tsx
// Wrong — thinking ErrorBoundary catches this
useEffect(() => {
  fetchData(); // unhandled promise — ErrorBoundary cannot catch this
}, []);

// Correct — handle async errors in state
useEffect(() => {
  fetchData()
    .then(setData)
    .catch(err => setError(err.message));
}, []);
```

### Trap 3: Not resetting error state on retry

```tsx
// Wrong — error state persists even after successful retry
const retry = () => {
  fetchData().then(setData); // forgot to clear error
};

// Correct
const retry = () => {
  setError(null);
  setLoading(true);
  fetchData()
    .then(setData)
    .catch(err => setError(err.message))
    .finally(() => setLoading(false));
};
```

### Trap 4: Generic error messages in production

```tsx
// Wrong — tells user nothing
<Text>Error</Text>

// Correct — actionable message with recovery path
<Text>We couldn't load your orders. Check your connection and try again.</Text>
<Pressable onPress={retry}><Text>Retry</Text></Pressable>
```

---

## 10. Interview Answer Template

```text
Q: How do you handle errors in a React Native app?

A: I think about three layers.

First, rendering errors in the component tree are caught by Error Boundaries — class
components that implement getDerivedStateFromError and componentDidCatch. I use
granular boundaries so one broken section does not crash the whole screen. The
boundary shows a fallback UI with a recovery action.

Second, async errors in useEffect, event handlers, and custom hooks are NOT caught
by Error Boundaries, so I always wrap async logic in try/catch and store the error
in state. I categorize errors — network vs server vs parse — and show different
messages or recovery actions for each.

Third, for production I set up a global handler with Sentry to catch unhandled
rejections and fatal JS errors, symbolicated against source maps so I can diagnose
them by version and device.

For every screen that fetches data, I implement four states: loading, error, empty,
and success — never showing a blank screen to the user.
```

---

## 11. Revision Notes

- Error Boundaries only catch render-time errors — not async, not event handlers
- Always put try/catch inside useEffect for async code
- Use `cancelled` ref to prevent setState after unmount
- Categorize errors — network, server, parse — for better UX
- Every fetch screen needs 4 states: loading, error, empty, success
- Use functional update form `setState(prev => ...)` when new state depends on old
- Log all caught errors in production — never silently swallow
- Retry with exponential backoff for transient network errors
- Global error handler (Sentry) is required for production crash visibility
