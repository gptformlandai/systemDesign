# React Native TanStack Query (React Query) — Data Fetching Internals — Gold Sheet

> Track File #11 of 37 · Group 2: App Architecture
> Level: intermediate to senior | Mode: master server-state management for mobile

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Why TanStack Query vs plain fetch/useState | Very high | Proves you understand server-state vs client-state |
| Cache invalidation and stale-while-revalidate | Very high | Core to the mental model |
| `useQuery` — all options that matter | Very high | Daily usage patterns |
| `useMutation` — optimistic updates | High | MAANG-level pattern |
| Pagination with `useInfiniteQuery` | High | FlatList + infinite scroll pattern |
| Prefetching and query waterfalls | High | Performance interview topic |
| Offline support — React Query + AsyncStorage | High | Mobile-specific topic |
| Error handling and retries | High | Production robustness |

---

## 2. Mental Model — Why TanStack Query Exists

### The problem with plain fetch + useState

```tsx
// Every screen duplicates this pattern
const [data, setData] = useState(null);
const [loading, setLoading] = useState(false);
const [error, setError] = useState(null);

useEffect(() => {
  setLoading(true);
  fetch('/products').then(r => r.json()).then(setData).catch(setError).finally(() => setLoading(false));
}, []);
```

Problems this causes:
1. Duplicate requests — two screens mounting at the same time both fetch `/products`
2. No caching — navigating back to a screen refetches everything
3. Stale data — user sees old data with no refresh
4. Race conditions — fast navigation can deliver results out of order
5. Background refresh — no way to silently refresh when app comes to foreground
6. No shared state — two components using the same data have separate copies

### Server state vs client state

```text
Client state:          what user has done locally
  → useState, useReducer, Zustand, Redux
  → Examples: modal open/closed, selected tab, draft form text

Server state:          what exists on the server that we cache locally
  → TanStack Query, SWR
  → Examples: products list, user profile, order history
  → It is stale by definition — it may change on the server at any time
```

TanStack Query manages server state: caching, background refetching, deduplication, loading/error state, pagination, mutations, and optimistic updates.

---

## 3. Setup

```tsx
// App.tsx
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,   // data is fresh for 5 minutes — no refetch
      gcTime: 10 * 60 * 1000,     // keep in cache 10 minutes after last subscriber
      retry: 2,                    // retry failed requests twice
      retryDelay: attempt => Math.min(1000 * 2 ** attempt, 30_000),
      refetchOnWindowFocus: false, // mobile apps use AppState instead
      refetchOnMount: true,        // refetch when component mounts if stale
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <NavigationContainer>
        <RootNavigator />
      </NavigationContainer>
    </QueryClientProvider>
  );
}
```

---

## 4. useQuery — Full Mental Model

```tsx
import {useQuery} from '@tanstack/react-query';

// Query key: unique identifier for this data + its dependencies
// Query function: async function that fetches the data
const {data, isLoading, isFetching, isError, error, refetch} = useQuery({
  queryKey: ['products', categoryId],  // array — categoryId is a dependency
  queryFn: () => fetchProducts(categoryId),
  staleTime: 2 * 60 * 1000,   // override default for this query
  enabled: !!categoryId,        // only run when categoryId exists
  select: data => data.items,  // transform data before returning to component
  placeholderData: keepPreviousData, // show previous data while fetching new
});
```

### Query key design — critical

The query key must uniquely identify the data:

```tsx
// Good query key patterns
['user', userId]                          // user by ID
['products']                              // all products
['products', {category: 'shoes', sort: 'price'}]  // filtered products
['user', userId, 'orders']               // user's orders
['user', userId, 'orders', {page: 1}]   // paginated orders

// Bad — same key for different data
['data']   // too generic — collides
```

### isLoading vs isFetching

```text
isLoading: true only when there is no cached data AND a request is in flight
           (first load)

isFetching: true whenever a request is in flight — includes background refetches
            (use this to show a subtle refresh indicator)

isStale: true when the data is older than staleTime — may be refetched soon
```

```tsx
function ProductList({categoryId}: {categoryId: string}) {
  const {data, isLoading, isFetching, isError} = useQuery({
    queryKey: ['products', categoryId],
    queryFn: () => fetchProducts(categoryId),
  });

  if (isLoading) return <ActivityIndicator />; // first load — no cached data
  if (isError) return <ErrorScreen />;

  return (
    <>
      {isFetching && <Text style={styles.refreshing}>Refreshing...</Text>}
      <FlatList data={data} ... />
    </>
  );
}
```

---

## 5. Dependent Queries — Preventing Waterfalls

Query waterfalls happen when Query B starts only after Query A finishes, sequentially:

```tsx
// Waterfall — user loads, then orders load — two sequential round trips
const {data: user} = useQuery({queryKey: ['user', userId], queryFn: () => fetchUser(userId)});
const {data: orders} = useQuery({
  queryKey: ['orders', user?.id],
  queryFn: () => fetchOrders(user!.id),
  enabled: !!user, // waits for user — sequential!
});
```

Prevent waterfalls when possible:

```tsx
// Option 1: Fetch both in parallel if you can derive the ID without a server round-trip
const [userQuery, ordersQuery] = [
  useQuery({queryKey: ['user', userId], queryFn: () => fetchUser(userId)}),
  useQuery({queryKey: ['orders', userId], queryFn: () => fetchOrders(userId)}),
];

// Option 2: Server endpoint that returns both (BFF pattern)
const {data} = useQuery({
  queryKey: ['userWithOrders', userId],
  queryFn: () => fetchUserWithOrders(userId), // single API call
});

// Option 3: Prefetch on navigate — start loading before screen mounts
navigation.navigate('Profile', {userId});
queryClient.prefetchQuery({
  queryKey: ['user', userId],
  queryFn: () => fetchUser(userId),
});
```

---

## 6. useMutation — Writes and Optimistic Updates

```tsx
import {useMutation, useQueryClient} from '@tanstack/react-query';

const queryClient = useQueryClient();

const addToCart = useMutation({
  mutationFn: (item: CartItem) => postCartItem(item),

  // Optimistic update — update UI before server confirms
  onMutate: async (newItem) => {
    // Cancel any outgoing refetches so they don't overwrite optimistic update
    await queryClient.cancelQueries({queryKey: ['cart']});

    // Snapshot previous value for rollback
    const previousCart = queryClient.getQueryData<CartItem[]>(['cart']);

    // Optimistically update cache
    queryClient.setQueryData<CartItem[]>(['cart'], old => [...(old ?? []), newItem]);

    return {previousCart}; // returned context used in onError
  },

  onError: (err, newItem, context) => {
    // Rollback to previous value on failure
    if (context?.previousCart) {
      queryClient.setQueryData(['cart'], context.previousCart);
    }
    showToast('Could not add to cart — please try again');
  },

  onSuccess: () => {
    // Invalidate cart query — will refetch from server to get final truth
    queryClient.invalidateQueries({queryKey: ['cart']});
    showToast('Added to cart');
  },
});

// Usage
<Pressable onPress={() => addToCart.mutate({id: product.id, qty: 1})}>
  <Text>{addToCart.isPending ? 'Adding...' : 'Add to Cart'}</Text>
</Pressable>
```

---

## 7. useInfiniteQuery — FlatList Pagination

```tsx
import {useInfiniteQuery} from '@tanstack/react-query';

type ProductPage = {items: Product[]; nextCursor: string | null};

const {
  data,
  fetchNextPage,
  hasNextPage,
  isFetchingNextPage,
  isLoading,
} = useInfiniteQuery({
  queryKey: ['products', filters],
  queryFn: ({pageParam}) => fetchProducts({cursor: pageParam, ...filters}),
  initialPageParam: null as string | null,
  getNextPageParam: lastPage => lastPage.nextCursor, // null = no more pages
});

// Flatten pages for FlatList
const products = data?.pages.flatMap(page => page.items) ?? [];

<FlatList
  data={products}
  keyExtractor={item => item.id}
  renderItem={({item}) => <ProductCard product={item} />}
  onEndReached={() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }}
  onEndReachedThreshold={0.5}
  ListFooterComponent={
    isFetchingNextPage ? <ActivityIndicator /> : null
  }
/>
```

---

## 8. Cache Invalidation Strategies

```tsx
const queryClient = useQueryClient();

// 1. Invalidate by key — marks as stale, refetches if there is an active subscriber
queryClient.invalidateQueries({queryKey: ['products']});

// 2. Invalidate all queries with prefix
queryClient.invalidateQueries({queryKey: ['user', userId]}); // all user queries for this ID

// 3. Set data directly — optimistic or from mutation response
queryClient.setQueryData(['user', userId], updatedUser);

// 4. Remove from cache entirely
queryClient.removeQueries({queryKey: ['tempData']});

// 5. Prefetch — populate cache before user navigates
await queryClient.prefetchQuery({
  queryKey: ['product', productId],
  queryFn: () => fetchProduct(productId),
  staleTime: 5 * 60 * 1000,
});
```

---

## 9. Mobile-Specific: Refetch on App Foreground

React Query defaults `refetchOnWindowFocus` to true for web (window focus events). In React Native, the equivalent is refetching when the app returns from background:

```tsx
import {useEffect} from 'react';
import {AppState} from 'react-native';
import {useQueryClient, focusManager} from '@tanstack/react-query';

// Set up AppState-based focus management — do this once at app root
function useAppStateFocus() {
  useEffect(() => {
    const sub = AppState.addEventListener('change', status => {
      if (status === 'active') {
        focusManager.setFocused(true);  // tells React Query the app is focused
      } else {
        focusManager.setFocused(false);
      }
    });
    return () => sub.remove();
  }, []);
}

// In App.tsx root component
function App() {
  useAppStateFocus();
  return <QueryClientProvider client={queryClient}>...</QueryClientProvider>;
}
```

---

## 10. Offline Support — Persisting the Cache

Combine React Query with AsyncStorage or MMKV to persist cache across app restarts:

```tsx
import {QueryClient} from '@tanstack/react-query';
import {createSyncStoragePersister} from '@tanstack/query-sync-storage-persister';
import {persistQueryClient} from '@tanstack/react-query-persist-client';
import AsyncStorage from '@react-native-async-storage/async-storage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {gcTime: 24 * 60 * 60 * 1000}, // keep cache 24 hours
  },
});

const asyncStoragePersister = createSyncStoragePersister({
  storage: {
    getItem: key => AsyncStorage.getItem(key),      // wrapped for sync interface
    setItem: (key, value) => AsyncStorage.setItem(key, value),
    removeItem: key => AsyncStorage.removeItem(key),
  },
});

persistQueryClient({
  queryClient,
  persister: asyncStoragePersister,
  maxAge: 24 * 60 * 60 * 1000, // cache valid for 24 hours
});
```

With offline persistence:
- App shows cached data immediately on launch (no loading flash)
- Stale queries refetch in background when online
- App works fully offline for reads

---

## 11. Query Key Factory Pattern — Scale Without Key Collisions

As apps grow, ad-hoc query key strings become unmaintainable:

```tsx
// keys.ts — centralized query key factory
export const queryKeys = {
  products: {
    all: ['products'] as const,
    list: (filters: ProductFilters) => ['products', 'list', filters] as const,
    detail: (id: string) => ['products', 'detail', id] as const,
  },
  user: {
    all: ['user'] as const,
    detail: (id: string) => ['user', id] as const,
    orders: (id: string) => ['user', id, 'orders'] as const,
    cart: (id: string) => ['user', id, 'cart'] as const,
  },
} as const;

// Usage
useQuery({queryKey: queryKeys.products.detail(productId), ...});
// Invalidating all product queries
queryClient.invalidateQueries({queryKey: queryKeys.products.all});
```

---

## 12. Common Traps

### Trap 1: Using state for server data

```tsx
// Wrong — manual loading/error/data management
const [products, setProducts] = useState([]);
useEffect(() => { fetch('/products').then(r => r.json()).then(setProducts); }, []);

// Correct — let React Query manage server state
const {data: products} = useQuery({queryKey: ['products'], queryFn: fetchProducts});
```

### Trap 2: Forgetting enabled flag for dependent queries

```tsx
// Wrong — throws if userId is undefined
useQuery({queryKey: ['user', userId], queryFn: () => fetchUser(userId!)});

// Correct
useQuery({
  queryKey: ['user', userId],
  queryFn: () => fetchUser(userId!),
  enabled: !!userId,
});
```

### Trap 3: Mutating cache data directly

```tsx
// Wrong — mutates the cached object
const user = queryClient.getQueryData<User>(['user', id]);
user!.name = 'New Name'; // mutation — React Query does not know about this change

// Correct — set new data
queryClient.setQueryData(['user', id], (old: User) => ({...old, name: 'New Name'}));
```

### Trap 4: Not handling isFetching separately from isLoading

```tsx
// Wrong — shows full loading spinner on every background refetch
if (isLoading || isFetching) return <ActivityIndicator />;

// Correct — only show full spinner on first load
if (isLoading) return <ActivityIndicator />;
// Show subtle indicator for background refetches
{isFetching && <SmallRefreshIndicator />}
```

---

## 13. Interview Answer Template

```text
Q: How do you manage server data in React Native?

A: I use TanStack Query (React Query) for all server state. The key insight is that
server data is fundamentally different from UI state — it is stale by definition,
can be shared by multiple components, and needs background refresh.

React Query handles caching, deduplication, stale-while-revalidate, loading and
error state, pagination, and mutations with optimistic updates — all out of the box.

For mobile, I configure refetchOnWindowFocus to use AppState so queries refresh when
the user returns to the app. For offline support, I persist the cache to AsyncStorage
so users see cached data instantly on launch. I use useInfiniteQuery with FlatList
for paginated lists. For mutations like adding to cart, I implement optimistic
updates — update the cache immediately, roll back if the server fails.

The pattern I follow is: query keys are arrays that include all dependencies, query
functions are pure async functions, and invalidation happens after mutations to keep
the cache synchronized with the server.
```

---

## 14. Revision Notes

- TanStack Query owns server state; Zustand/useState owns UI state — never mix
- Query key must include all variables the fetch depends on
- `isLoading` = first fetch with no cache; `isFetching` = any in-flight request
- Optimistic update: cancelQueries → snapshot → setQueryData → return snapshot → rollback in onError → invalidate in onSuccess
- `enabled: !!dependency` prevents queries from running with undefined params
- `useInfiniteQuery` + `getNextPageParam` + FlatList `onEndReached` = infinite scroll
- Persist cache to AsyncStorage for offline-first behavior
- Refetch on AppState change replaces `refetchOnWindowFocus` for mobile
- Query key factory prevents collision and enables targeted invalidation
- `gcTime` controls when unused data is garbage collected; `staleTime` controls when data is considered fresh
