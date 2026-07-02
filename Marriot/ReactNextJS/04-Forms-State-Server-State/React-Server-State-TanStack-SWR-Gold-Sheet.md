# React Server State, TanStack Query, And SWR - Gold Sheet

> Track Module - Group 4: Forms, Client State, And Server State
> Covers: server state vs client state, TanStack Query caching/background refetch/mutations, SWR basics, API consistency problems

---

## 1. Intuition

Server state is borrowed data.

Client state:
- owned by browser/app
- synchronous
- directly mutable by UI

Server state:
- owned by backend
- async
- cached
- can become stale
- shared across users/devices

---

## 2. Why Server-State Libraries Exist

Without a server-state library, teams rebuild:
- loading state
- error state
- retries
- caching
- deduplication
- background refetch
- pagination
- optimistic updates
- invalidation
- stale data policy

TanStack Query and SWR solve these recurring problems.

---

## 3. TanStack Query Basics

```tsx
function useProduct(productId: string) {
  return useQuery({
    queryKey: ['product', productId],
    queryFn: () => fetchProduct(productId),
    staleTime: 60_000,
  });
}
```

Query key:
- identifies cached data
- should include all inputs
- controls invalidation/refetch

States:
- loading/pending
- error
- success
- stale
- fetching in background

---

## 4. Background Refetching

Server data can be shown from cache while being refreshed.

Benefits:
- fast UI
- eventually fresh data
- fewer loading spinners

Trade-off:
UI may briefly show stale data. That is acceptable for many products but not all.

Examples where stale cache is fine:
- product description
- user profile display
- dashboard widgets

Examples needing stronger freshness:
- payment status
- inventory reservation
- trading price

---

## 5. Mutations

```tsx
const mutation = useMutation({
  mutationFn: updateProfile,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['profile']});
  },
});
```

Mutation flow:

```text
user action -> mutation -> pending UI -> success/error -> invalidate or update cache
```

Optimistic update:

```tsx
const likePost = useMutation({
  mutationFn: sendLike,
  onMutate: async postId => {
    await queryClient.cancelQueries({queryKey: ['feed']});
    const previous = queryClient.getQueryData(['feed']);
    queryClient.setQueryData(['feed'], optimisticLike(postId));
    return {previous};
  },
  onError: (_error, _postId, context) => {
    queryClient.setQueryData(['feed'], context?.previous);
  },
  onSettled: () => {
    queryClient.invalidateQueries({queryKey: ['feed']});
  },
});
```

Use optimistic updates only when rollback is safe.

---

## 6. SWR Basics

SWR stands for stale-while-revalidate.

```tsx
function Profile() {
  const {data, error, isLoading} = useSWR('/api/profile', fetcher);

  if (isLoading) return <Spinner />;
  if (error) return <ErrorState />;

  return <h1>{data.name}</h1>;
}
```

Good for:
- simple data fetching
- cache-first UX
- small/medium apps
- Next.js/Vercel ecosystem familiarity

TanStack Query tends to offer broader mutation/cache workflow control.

---

## 7. API Data Consistency Problems

Problems:
- stale cache after mutation
- duplicate requests
- race conditions
- optimistic update conflict
- pagination duplication
- partial failure
- offline edits
- server truth differs from client assumption

Mitigations:
- consistent query keys
- invalidation after mutations
- server-generated versions/timestamps
- idempotency keys
- conflict resolution
- normalized cache where needed
- refetch on focus/reconnect when appropriate

---

## 8. Real-World Use Cases

- Ecommerce catalog: cached queries with ISR/Next server rendering for public pages.
- User profile: query cache plus invalidation on edit.
- Feed: infinite query, optimistic likes.
- Admin table: query key includes filters, sorting, pagination.
- Payment: avoid optimistic success, poll or subscribe to server-confirmed status.

---

## 8b. Infinite Queries (Pagination)

```tsx
import {useInfiniteQuery} from '@tanstack/react-query';

function BookingFeed() {
  const {data, fetchNextPage, hasNextPage, isFetchingNextPage} =
    useInfiniteQuery({
      queryKey: ['bookings'],
      queryFn: ({pageParam}) => fetchBookings({cursor: pageParam}),
      initialPageParam: undefined,
      getNextPageParam: lastPage => lastPage.nextCursor ?? undefined,
    });

  const bookings = data?.pages.flatMap(page => page.items) ?? [];

  return (
    <>
      {bookings.map(b => <BookingCard key={b.id} booking={b} />)}
      {hasNextPage && (
        <button
          onClick={() => fetchNextPage()}
          disabled={isFetchingNextPage}
        >
          {isFetchingNextPage ? 'Loading…' : 'Load more'}
        </button>
      )}
    </>
  );
}
```

---

## 8c. Select — Transform Data in the Query

`select` transforms data without changing what is cached. Different components can subscribe to different shapes of the same query:

```tsx
// Raw query — cached as full product list
const useProducts = () =>
  useQuery({queryKey: ['products'], queryFn: fetchProducts});

// Subscriber A — only needs names
const useProductNames = () =>
  useQuery({
    queryKey: ['products'],
    queryFn: fetchProducts,
    select: products => products.map(p => p.name),
  });

// Subscriber B — only needs in-stock items
const useInStockProducts = () =>
  useQuery({
    queryKey: ['products'],
    queryFn: fetchProducts,
    select: products => products.filter(p => p.inStock),
  });
```

`select` runs on every render if not memoized — wrap with `useCallback` for expensive transforms.

---

## 8d. placeholderData — Smooth Pagination

`keepPreviousData` is replaced by `placeholderData: keepPreviousData` in TanStack Query v5:

```tsx
import {keepPreviousData} from '@tanstack/react-query';

function ProductTable({page}: {page: number}) {
  const {data, isFetching} = useQuery({
    queryKey: ['products', page],
    queryFn: () => fetchProducts(page),
    placeholderData: keepPreviousData, // shows previous page while loading next
  });

  return (
    <>
      {isFetching && <div>Updating…</div>}
      {data?.items.map(p => <ProductRow key={p.id} product={p} />)}
    </>
  );
}
```

Result: no content flash on page changes — previous data stays visible while the next page loads.

---

## 8e. Dependent Queries — enabled

Run a query only when a prerequisite value is available:

```tsx
function BookingDetails({userId}: {userId: string | undefined}) {
  const userQuery = useQuery({
    queryKey: ['user', userId],
    queryFn: () => fetchUser(userId!),
    enabled: !!userId,
  });

  const bookingsQuery = useQuery({
    queryKey: ['bookings', userQuery.data?.id],
    queryFn: () => fetchBookings(userQuery.data!.id),
    enabled: !!userQuery.data?.id, // waits for user to load
  });
}
```

---

## 8f. Parallel Queries — useQueries

```tsx
function MultiRoomDetails({roomIds}: {roomIds: string[]}) {
  const roomQueries = useQueries({
    queries: roomIds.map(id => ({
      queryKey: ['room', id],
      queryFn: () => fetchRoom(id),
    })),
  });

  const isLoading = roomQueries.some(q => q.isPending);
  const rooms = roomQueries
    .map(q => q.data)
    .filter(Boolean) as Room[];
}
```

---

## 8g. Prefetching in Server Components (Next.js)

Hydrate the query cache on the server so the client has data immediately:

```tsx
// app/products/page.tsx (Server Component)
import {dehydrate, HydrationBoundary, QueryClient} from '@tanstack/react-query';

export default async function ProductsPage() {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: ['products'],
    queryFn: fetchProducts,
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <ProductList />
    </HydrationBoundary>
  );
}
```

`ProductList` is a Client Component that calls `useQuery(['products'])` — the cache is already populated on first render.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Missing query input in key | Cache collisions | Include all variables |
| Optimistic update for irreversible action | False success risk | Wait for server confirmation |
| Manual Redux cache for API data | Reinventing query cache | Use server-state library |
| Invalidating everything | Wasteful refetch | Target query keys/tags |
| Ignoring races | Old response overwrites new | Key/cancel/ignore stale responses |

---

## 10. Strong Interview Answer

Question:
What is server state and how do you manage it?

Strong answer:

```text
Server state is data owned by the backend and cached in the UI. It is async,
shared, and can become stale, unlike local client state. I use TanStack Query or
SWR to manage caching, stale time, background refetch, deduplication, retries,
mutations, optimistic updates, and invalidation. The key design decisions are
query-key structure, freshness policy, mutation rollback, and consistency after
server writes.
```

---

## 11. Revision Notes

- One-line summary: Server state is cached backend truth, not normal local state.
- Three keywords: query key, stale time, invalidation.
- One interview trap: Do not put API cache manually into Redux by default.
- One memory trick: Client state is owned; server state is borrowed.

