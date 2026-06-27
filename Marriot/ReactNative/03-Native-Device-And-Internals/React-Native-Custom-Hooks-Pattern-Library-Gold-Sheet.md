# React Native Custom Hooks Pattern Library — Gold Sheet

> Track File #15 of 37 · Group 3: Native Device & Internals
> Level: intermediate to senior | Mode: 15 production-ready hooks you can use and explain in interviews

---

## 1. Why a Hook Pattern Library

Senior React Native engineers do not write the same async/event/subscription boilerplate repeatedly. They extract it into hooks that are:
- Independently testable
- Composable with other hooks
- Reusable across screens
- Named to express intent

This sheet is a library of 15 production hooks organized by category. Study the pattern, understand why it is structured the way it is, and be able to explain the design choices in an interview.

---

## 2. Data Fetching Hooks

### Hook 1: useAsync — generic async operation

```tsx
type AsyncState<T> =
  | {status: 'idle'}
  | {status: 'loading'}
  | {status: 'success'; data: T}
  | {status: 'error'; error: Error};

type UseAsyncReturn<T> = AsyncState<T> & {execute: () => void};

function useAsync<T>(asyncFn: () => Promise<T>, immediate = true): UseAsyncReturn<T> {
  const [state, setState] = useState<AsyncState<T>>({status: 'idle'});
  const mountedRef = useRef(true);

  const execute = useCallback(async () => {
    setState({status: 'loading'});
    try {
      const data = await asyncFn();
      if (mountedRef.current) setState({status: 'success', data});
    } catch (err) {
      if (mountedRef.current) {
        setState({status: 'error', error: err instanceof Error ? err : new Error(String(err))});
      }
    }
  }, [asyncFn]);

  useEffect(() => {
    mountedRef.current = true;
    if (immediate) execute();
    return () => { mountedRef.current = false; };
  }, [execute, immediate]);

  return {...state, execute};
}

// Usage
const {status, data: user, error, execute: retry} = useAsync(
  () => fetchUser(userId),
  true, // run immediately
);
```

### Hook 2: useFetch — typed fetch with abort

```tsx
function useFetch<T>(url: string, options?: RequestInit) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    fetch(url, {...options, signal: controller.signal})
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<T>;
      })
      .then(setData)
      .catch(err => {
        if (err.name !== 'AbortError') setError(err.message);
      })
      .finally(() => setLoading(false));

    return () => controller.abort(); // cancel on unmount or url change
  }, [url]);

  return {data, loading, error};
}
```

### Hook 3: usePaginatedList — local pagination state

```tsx
type PaginatedListState<T> = {
  items: T[];
  page: number;
  hasMore: boolean;
  loading: boolean;
  error: string | null;
};

function usePaginatedList<T>(
  fetchPage: (page: number) => Promise<{items: T[]; hasMore: boolean}>,
) {
  const [state, setState] = useState<PaginatedListState<T>>({
    items: [],
    page: 1,
    hasMore: true,
    loading: false,
    error: null,
  });

  const loadPage = useCallback(async (pageNum: number, reset = false) => {
    setState(prev => ({...prev, loading: true, error: null}));
    try {
      const {items, hasMore} = await fetchPage(pageNum);
      setState(prev => ({
        items: reset ? items : [...prev.items, ...items],
        page: pageNum,
        hasMore,
        loading: false,
        error: null,
      }));
    } catch (err) {
      setState(prev => ({
        ...prev,
        loading: false,
        error: err instanceof Error ? err.message : 'Failed',
      }));
    }
  }, [fetchPage]);

  useEffect(() => { loadPage(1, true); }, [loadPage]);

  const loadMore = useCallback(() => {
    if (!state.loading && state.hasMore) {
      loadPage(state.page + 1);
    }
  }, [state.loading, state.hasMore, state.page, loadPage]);

  const refresh = useCallback(() => loadPage(1, true), [loadPage]);

  return {...state, loadMore, refresh};
}
```

---

## 3. Device and Network Hooks

### Hook 4: useNetworkStatus — online/offline detection

```tsx
import NetInfo from '@react-native-community/netinfo';

type NetworkStatus = {
  isOnline: boolean;
  type: string | null; // 'wifi', 'cellular', 'none', etc.
};

function useNetworkStatus(): NetworkStatus {
  const [status, setStatus] = useState<NetworkStatus>({isOnline: true, type: null});

  useEffect(() => {
    // Get initial state
    NetInfo.fetch().then(state => {
      setStatus({isOnline: state.isConnected ?? true, type: state.type});
    });

    // Subscribe to changes
    const unsubscribe = NetInfo.addEventListener(state => {
      setStatus({isOnline: state.isConnected ?? true, type: state.type});
    });

    return unsubscribe;
  }, []);

  return status;
}

// Usage — show offline banner
const {isOnline} = useNetworkStatus();
{!isOnline && <OfflineBanner />}
```

### Hook 5: useAppState — foreground/background lifecycle

```tsx
type AppStateStatus = 'active' | 'background' | 'inactive';

function useAppState(): AppStateStatus {
  const [appState, setAppState] = useState<AppStateStatus>(
    AppState.currentState as AppStateStatus,
  );

  useEffect(() => {
    const sub = AppState.addEventListener('change', state => {
      setAppState(state as AppStateStatus);
    });
    return () => sub.remove();
  }, []);

  return appState;
}

// Variant: fire callbacks on state transitions
function useAppStateCallbacks({
  onForeground,
  onBackground,
}: {onForeground?: () => void; onBackground?: () => void}) {
  const prevState = useRef(AppState.currentState);

  useEffect(() => {
    const sub = AppState.addEventListener('change', nextState => {
      if (prevState.current !== 'active' && nextState === 'active') {
        onForeground?.();
      } else if (prevState.current === 'active' && nextState !== 'active') {
        onBackground?.();
      }
      prevState.current = nextState;
    });
    return () => sub.remove();
  }, [onForeground, onBackground]);
}
```

### Hook 6: useLocation — GPS coordinates with permission

```tsx
import * as Location from 'expo-location';

type LocationState =
  | {status: 'idle'}
  | {status: 'requesting'}
  | {status: 'denied'; message: string}
  | {status: 'granted'; coords: {latitude: number; longitude: number}};

function useLocation(): [LocationState, () => void] {
  const [state, setState] = useState<LocationState>({status: 'idle'});

  const requestLocation = useCallback(async () => {
    setState({status: 'requesting'});

    const {status} = await Location.requestForegroundPermissionsAsync();
    if (status !== 'granted') {
      setState({status: 'denied', message: 'Location permission denied'});
      return;
    }

    try {
      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      setState({
        status: 'granted',
        coords: {
          latitude: location.coords.latitude,
          longitude: location.coords.longitude,
        },
      });
    } catch {
      setState({status: 'denied', message: 'Could not get location'});
    }
  }, []);

  return [state, requestLocation];
}
```

---

## 4. UI Behavior Hooks

### Hook 7: useKeyboardDimensions — keyboard height for layout

```tsx
import {Keyboard, KeyboardEvent, Platform} from 'react-native';

type KeyboardDimensions = {height: number; visible: boolean};

function useKeyboardDimensions(): KeyboardDimensions {
  const [dimensions, setDimensions] = useState<KeyboardDimensions>({
    height: 0,
    visible: false,
  });

  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';

    const show = Keyboard.addListener(showEvent, (e: KeyboardEvent) => {
      setDimensions({height: e.endCoordinates.height, visible: true});
    });

    const hide = Keyboard.addListener(hideEvent, () => {
      setDimensions({height: 0, visible: false});
    });

    return () => {
      show.remove();
      hide.remove();
    };
  }, []);

  return dimensions;
}
```

### Hook 8: useDebounce — delay a rapidly changing value

```tsx
function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}

// Usage in search
const [query, setQuery] = useState('');
const debouncedQuery = useDebounce(query, 400);
useEffect(() => {
  if (debouncedQuery) search(debouncedQuery);
}, [debouncedQuery]);
```

### Hook 9: useThrottle — limit execution frequency

```tsx
function useThrottle<T>(value: T, intervalMs: number): T {
  const [throttled, setThrottled] = useState<T>(value);
  const lastRan = useRef(Date.now());

  useEffect(() => {
    const elapsed = Date.now() - lastRan.current;
    if (elapsed >= intervalMs) {
      setThrottled(value);
      lastRan.current = Date.now();
    } else {
      const timer = setTimeout(() => {
        setThrottled(value);
        lastRan.current = Date.now();
      }, intervalMs - elapsed);
      return () => clearTimeout(timer);
    }
  }, [value, intervalMs]);

  return throttled;
}
```

### Hook 10: usePrevious — access previous render's value

```tsx
function usePrevious<T>(value: T): T | undefined {
  const ref = useRef<T>();
  useEffect(() => {
    ref.current = value;
  });
  return ref.current; // returns value from previous render
}

// Usage — animate when count changes direction
const prevCount = usePrevious(count);
const isIncreasing = count > (prevCount ?? count);
```

---

## 5. Form and Input Hooks

### Hook 11: useFormField — single field with validation

```tsx
type FieldConfig<T> = {
  initialValue: T;
  validate: (value: T) => string | null; // null = valid
};

type FieldState<T> = {
  value: T;
  error: string | null;
  touched: boolean;
  onChange: (value: T) => void;
  onBlur: () => void;
  reset: () => void;
};

function useFormField<T>({initialValue, validate}: FieldConfig<T>): FieldState<T> {
  const [value, setValue] = useState<T>(initialValue);
  const [touched, setTouched] = useState(false);

  const error = touched ? validate(value) : null;

  return {
    value,
    error,
    touched,
    onChange: setValue,
    onBlur: () => setTouched(true),
    reset: () => { setValue(initialValue); setTouched(false); },
  };
}

// Usage
const email = useFormField({
  initialValue: '',
  validate: v => {
    if (!v) return 'Email is required';
    if (!v.includes('@')) return 'Invalid email';
    return null;
  },
});

<TextInput
  value={email.value}
  onChangeText={email.onChange}
  onBlur={email.onBlur}
/>
{email.error && <Text style={styles.error}>{email.error}</Text>}
```

### Hook 12: useToggle — boolean state with convenience methods

```tsx
function useToggle(initial = false): [boolean, {toggle: () => void; on: () => void; off: () => void}] {
  const [value, setValue] = useState(initial);
  const toggle = useCallback(() => setValue(v => !v), []);
  const on = useCallback(() => setValue(true), []);
  const off = useCallback(() => setValue(false), []);
  return [value, {toggle, on, off}];
}

// Usage
const [isModalOpen, modal] = useToggle();
<Pressable onPress={modal.toggle}><Text>Open Modal</Text></Pressable>
<Modal visible={isModalOpen} onRequestClose={modal.off}>...</Modal>
```

---

## 6. Performance and Scroll Hooks

### Hook 13: useScrollToTop — scroll FlatList to top on tab press

```tsx
import {useScrollToTop} from '@react-navigation/native';
import {useRef} from 'react';
import {FlatList} from 'react-native';

function useScrollableList<T>() {
  const ref = useRef<FlatList<T>>(null);
  useScrollToTop(ref); // React Navigation hook — scrolls to top when tab is pressed

  return ref;
}

// Usage
const listRef = useScrollableList<Product>();
<FlatList ref={listRef} data={products} ... />
```

### Hook 14: useScrollPosition — track FlatList scroll offset

```tsx
function useScrollPosition() {
  const [scrollY, setScrollY] = useState(0);
  const [isScrolledDown, setIsScrolledDown] = useState(false);

  const onScroll = useCallback((event: {nativeEvent: {contentOffset: {y: number}}}) => {
    const y = event.nativeEvent.contentOffset.y;
    setScrollY(y);
    setIsScrolledDown(y > 100);
  }, []);

  return {scrollY, isScrolledDown, onScroll};
}

// Usage — show floating back-to-top button
const {isScrolledDown, onScroll} = useScrollPosition();

<FlatList onScroll={onScroll} scrollEventThrottle={16} ... />
{isScrolledDown && (
  <Pressable style={styles.backToTop} onPress={scrollToTop}>
    <Text>↑</Text>
  </Pressable>
)}
```

---

## 7. Storage Hooks

### Hook 15: useAsyncStorage — typed AsyncStorage wrapper

```tsx
import AsyncStorage from '@react-native-async-storage/async-storage';

function useAsyncStorage<T>(key: string, defaultValue: T) {
  const [value, setValue] = useState<T>(defaultValue);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    AsyncStorage.getItem(key)
      .then(stored => {
        if (stored !== null) {
          setValue(JSON.parse(stored) as T);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [key]);

  const setStored = useCallback(async (newValue: T) => {
    setValue(newValue);
    try {
      await AsyncStorage.setItem(key, JSON.stringify(newValue));
    } catch (err) {
      console.error('AsyncStorage write failed:', err);
      setValue(value => value); // revert optimistic update on failure? optional
    }
  }, [key]);

  const removeStored = useCallback(async () => {
    setValue(defaultValue);
    await AsyncStorage.removeItem(key);
  }, [key, defaultValue]);

  return {value, loading, setValue: setStored, remove: removeStored};
}

// Usage — persist theme preference
const {value: theme, setValue: setTheme} = useAsyncStorage<'light' | 'dark'>('theme', 'light');
```

---

## 8. Interview Pattern for Custom Hooks

When an interviewer asks you to design a feature, the senior signal is to immediately reach for custom hooks:

```text
Interview: "Design a search screen with debounced API calls and offline handling."

Senior answer structure:
1. useDebounce(query, 400) — debounce the input
2. useNetworkStatus() — detect offline
3. useQuery from TanStack Query with the debounced query key — data + cache
4. usePaginatedList — if results are paginated
5. useScrollableList — FlatList with tab-press scroll-to-top
6. Compose them all in the screen component — zero business logic in the screen

The screen component becomes a composition of hooks.
Each hook is independently testable.
```

---

## 9. Design Rules for Good Custom Hooks

```text
Rule 1: One responsibility
  A hook should do one thing. useFormField manages one field. Do not build
  a useEntireForm that manages 10 fields in one hook.

Rule 2: Return what the consumer needs — nothing more
  If the consumer only needs a boolean, return a boolean. Do not expose
  internal state that does not affect the consumer.

Rule 3: Cleanup everything
  Every subscription, timer, and request started in a hook must be cleaned up
  in the useEffect return function. No exceptions.

Rule 4: Stable function references
  Functions returned from hooks should be wrapped in useCallback so consumers
  can safely include them in their own dependency arrays.

Rule 5: TypeScript-first
  All hooks should be fully typed. Use generics when the return type
  depends on input parameters.

Rule 6: Test independently
  Good hooks can be tested with @testing-library/react-hooks without
  mounting any screen component.
```

---

## 10. Revision Notes

- Custom hooks = extracted stateful logic with full React hook capabilities
- `mountedRef` pattern prevents setState after unmount — use it in any async hook
- `useCallback` on returned functions ensures stable references for consumers
- Cleanup everything: subscriptions (`sub.remove()`), timers (`clearTimeout`), requests (`controller.abort()`)
- Discriminated union return types make consuming hooks type-safe and exhaustive
- Compose hooks in screen components — screens should contain zero business logic
- Each hook in this library has a single testable contract
- `useDebounce` and `useThrottle` are the two most commonly asked custom hooks in interviews
- `usePrevious` is a classic interview custom hook question — explain why `useRef` is needed (no re-render)
- `useFormField` with validation logic reduces form screens to pure composition
