# React + Next.js LLD Machine Coding Design — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: requirements → approach → full implementation → review

---

## How to Use This File

1. Read the requirements.
2. Set a timer (listed for each problem).
3. Design and code it without looking at the solution.
4. Compare your solution to the reference.
5. Grade: Did you get the component architecture, state shape, and key edge cases?

---

## Design 1 — Infinite Scroll Product Feed (30 min)

### Requirements

- Fetch products page by page from `/api/products?page=N&limit=20`
- Display products in a grid
- Load more when the user scrolls to the bottom
- Show a loading skeleton while fetching
- Handle errors with a retry button
- Show "No more products" when all pages are loaded

### Approach

```text
State: items[], page, hasMore, loading, error
Trigger: IntersectionObserver on a sentinel div at the bottom
Data: custom usePagination hook
Rendering: ProductCard grid + sentinel + loading indicator
```

### Full Implementation

```tsx
// hooks/usePagination.ts
type PaginationResult<T> = {
  items: T[];
  hasMore: boolean;
  loading: boolean;
  error: Error | null;
  loadMore: () => void;
  reset: () => void;
};

function usePagination<T>(
  fetchPage: (page: number) => Promise<{ items: T[]; hasMore: boolean }>
): PaginationResult<T> {
  const [items, setItems] = useState<T[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const loadingRef = useRef(false);

  const loadMore = useCallback(async () => {
    if (loadingRef.current || !hasMore) return;
    loadingRef.current = true;
    setLoading(true);
    setError(null);
    try {
      const result = await fetchPage(page);
      setItems(prev => [...prev, ...result.items]);
      setHasMore(result.hasMore);
      setPage(p => p + 1);
    } catch (err) {
      setError(err as Error);
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [page, hasMore, fetchPage]);

  useEffect(() => { loadMore(); }, []);  // load first page

  const reset = useCallback(() => {
    setItems([]);
    setPage(1);
    setHasMore(true);
    setError(null);
  }, []);

  return { items, hasMore, loading, error, loadMore, reset };
}

// components/ProductFeed.tsx
'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

type Product = { id: string; name: string; price: number; imageUrl: string };

function ProductCard({ product }: { product: Product }) {
  return (
    <div className="product-card">
      <img src={product.imageUrl} alt={product.name} loading="lazy" />
      <h3>{product.name}</h3>
      <p>${product.price.toFixed(2)}</p>
    </div>
  );
}

function ProductSkeleton() {
  return (
    <div className="product-card skeleton">
      <div className="skeleton-image" />
      <div className="skeleton-text" />
      <div className="skeleton-text short" />
    </div>
  );
}

export function ProductFeed() {
  const fetchPage = useCallback(async (page: number) => {
    const res = await fetch(`/api/products?page=${page}&limit=20`);
    if (!res.ok) throw new Error('Failed to load products');
    return res.json();  // { items: Product[], hasMore: boolean }
  }, []);

  const { items, hasMore, loading, error, loadMore } = usePagination<Product>(fetchPage);

  // Intersection Observer — trigger loadMore when sentinel enters view
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!sentinelRef.current) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) loadMore();
      },
      { threshold: 0.1 },
    );
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [loadMore]);

  return (
    <div>
      <div className="product-grid">
        {items.map(product => <ProductCard key={product.id} product={product} />)}
        {loading && Array.from({ length: 4 }).map((_, i) => <ProductSkeleton key={i} />)}
      </div>

      {error && (
        <div className="error-banner">
          <p>Failed to load: {error.message}</p>
          <button onClick={loadMore}>Retry</button>
        </div>
      )}

      {!hasMore && !loading && <p className="end-message">You've seen all products</p>}

      {/* Invisible sentinel div — IntersectionObserver watches this */}
      {hasMore && !error && <div ref={sentinelRef} style={{ height: 1 }} />}
    </div>
  );
}
```

**Review checklist:**
- [ ] Custom hook separates data logic from UI
- [ ] Guard against double-loading (loadingRef)
- [ ] IntersectionObserver properly disconnected on cleanup
- [ ] Skeletons shown during loading
- [ ] Error state with retry
- [ ] "End" state shown when hasMore is false

---

## Design 2 — Multi-Step Form with Validation (30 min)

### Requirements

- 3-step checkout form: Shipping → Payment → Review
- Each step validates before proceeding to next
- User can go back to previous steps
- Show step progress indicator
- Submit on final step with loading state
- Persist state across steps (don't lose shipping when going to payment)

### Full Implementation

```tsx
'use client';

import { z } from 'zod';
import { useForm, FormProvider, useFormContext } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';

// Step schemas
const ShippingSchema = z.object({
  fullName: z.string().min(2),
  address: z.string().min(5),
  city: z.string().min(2),
  zipCode: z.string().regex(/^\d{5}$/),
  country: z.string().min(2),
});

const PaymentSchema = z.object({
  cardNumber: z.string().regex(/^\d{16}$/, 'Must be 16 digits'),
  expiry: z.string().regex(/^(0[1-9]|1[0-2])\/\d{2}$/),
  cvv: z.string().regex(/^\d{3,4}$/),
});

const CheckoutSchema = ShippingSchema.merge(PaymentSchema);
type CheckoutFormData = z.infer<typeof CheckoutSchema>;

const STEPS = ['Shipping', 'Payment', 'Review'] as const;

// Progress indicator
function StepProgress({ current }: { current: number }) {
  return (
    <div className="step-progress">
      {STEPS.map((step, i) => (
        <div key={step} className={`step ${i < current ? 'completed' : i === current ? 'active' : ''}`}>
          <span className="step-number">{i < current ? '✓' : i + 1}</span>
          <span className="step-label">{step}</span>
          {i < STEPS.length - 1 && <div className="step-connector" />}
        </div>
      ))}
    </div>
  );
}

// Step 1: Shipping
function ShippingStep({ onNext }: { onNext: () => void }) {
  const { register, handleSubmit, formState: { errors } } = useFormContext<CheckoutFormData>();

  return (
    <form onSubmit={handleSubmit(onNext)}>
      <h2>Shipping Address</h2>
      <input {...register('fullName')} placeholder="Full Name" />
      {errors.fullName && <p className="error">{errors.fullName.message}</p>}
      
      <input {...register('address')} placeholder="Street Address" />
      {errors.address && <p className="error">{errors.address.message}</p>}
      
      <input {...register('city')} placeholder="City" />
      <input {...register('zipCode')} placeholder="ZIP Code" />
      <input {...register('country')} placeholder="Country" />
      
      <button type="submit">Continue to Payment →</button>
    </form>
  );
}

// Step 2: Payment
function PaymentStep({ onNext, onBack }: { onNext: () => void; onBack: () => void }) {
  const { register, handleSubmit, formState: { errors } } = useFormContext<CheckoutFormData>();

  return (
    <form onSubmit={handleSubmit(onNext)}>
      <h2>Payment Details</h2>
      <input {...register('cardNumber')} placeholder="Card Number" maxLength={16} />
      {errors.cardNumber && <p className="error">{errors.cardNumber.message}</p>}
      
      <input {...register('expiry')} placeholder="MM/YY" />
      <input {...register('cvv')} placeholder="CVV" type="password" />
      
      <div className="button-row">
        <button type="button" onClick={onBack}>← Back</button>
        <button type="submit">Review Order →</button>
      </div>
    </form>
  );
}

// Step 3: Review
function ReviewStep({
  onBack, onSubmit, isSubmitting
}: {
  onBack: () => void;
  onSubmit: () => void;
  isSubmitting: boolean;
}) {
  const { getValues } = useFormContext<CheckoutFormData>();
  const values = getValues();

  return (
    <div>
      <h2>Review Your Order</h2>
      <section>
        <h3>Ship to</h3>
        <p>{values.fullName}</p>
        <p>{values.address}, {values.city} {values.zipCode}</p>
        <p>{values.country}</p>
      </section>
      <section>
        <h3>Payment</h3>
        <p>Card ending in {values.cardNumber.slice(-4)}</p>
      </section>
      <div className="button-row">
        <button type="button" onClick={onBack} disabled={isSubmitting}>← Back</button>
        <button onClick={onSubmit} disabled={isSubmitting}>
          {isSubmitting ? 'Placing Order...' : 'Place Order'}
        </button>
      </div>
    </div>
  );
}

// Main checkout form — manages step state
export function CheckoutForm() {
  const [currentStep, setCurrentStep] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const methods = useForm<CheckoutFormData>({
    resolver: zodResolver(CheckoutSchema),
    mode: 'onSubmit',
  });

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      const data = methods.getValues();
      await submitOrder(data);
      // redirect to confirmation
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormProvider {...methods}>
      <StepProgress current={currentStep} />
      
      {currentStep === 0 && <ShippingStep onNext={() => setCurrentStep(1)} />}
      {currentStep === 1 && (
        <PaymentStep
          onNext={() => setCurrentStep(2)}
          onBack={() => setCurrentStep(0)}
        />
      )}
      {currentStep === 2 && (
        <ReviewStep
          onBack={() => setCurrentStep(1)}
          onSubmit={handleSubmit}
          isSubmitting={isSubmitting}
        />
      )}
    </FormProvider>
  );
}
```

**Review checklist:**
- [ ] `FormProvider` shares form state across step components
- [ ] Each step validates only its own fields before proceeding
- [ ] State preserved when going back (react-hook-form preserves field values)
- [ ] Step progress shows completed/active/pending states
- [ ] Review step shows summary without re-fetching
- [ ] Submit button disabled while pending

---

## Design 3 — Real-Time Search with Filters (25 min)

### Requirements

- Search input with debounce (300ms)
- Category filter (multi-select)
- Price range filter (min/max)
- Sortable results (price asc/desc, name)
- URL state sync — shareable filter links
- Loading skeleton while fetching

### Full Implementation

```tsx
'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { useCallback, useTransition } from 'react';

// Sync state to URL search params
function useFilterState() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  const query = searchParams.get('q') ?? '';
  const categories = searchParams.getAll('category');
  const minPrice = Number(searchParams.get('minPrice') ?? 0);
  const maxPrice = Number(searchParams.get('maxPrice') ?? 10000);
  const sort = (searchParams.get('sort') ?? 'name') as 'name' | 'price_asc' | 'price_desc';

  const updateParams = useCallback((updates: Record<string, string | string[]>) => {
    const params = new URLSearchParams(searchParams.toString());
    
    Object.entries(updates).forEach(([key, value]) => {
      params.delete(key);
      if (Array.isArray(value)) {
        value.forEach(v => params.append(key, v));
      } else if (value) {
        params.set(key, value);
      }
    });

    startTransition(() => {
      router.replace(`${pathname}?${params.toString()}`);
    });
  }, [pathname, router, searchParams]);

  return { query, categories, minPrice, maxPrice, sort, isPending, updateParams };
}

export function ProductSearch() {
  const { query, categories, minPrice, maxPrice, sort, isPending, updateParams } = useFilterState();
  const debouncedQuery = useDebounce(query, 300);

  const { data: products, loading } = useFetch<Product[]>(
    debouncedQuery || categories.length || minPrice || maxPrice !== 10000
      ? `/api/products?q=${encodeURIComponent(debouncedQuery)}&categories=${categories.join(',')}&min=${minPrice}&max=${maxPrice}&sort=${sort}`
      : '/api/products/featured'
  );

  return (
    <div className="search-layout">
      <aside className="filters">
        <input
          value={query}
          onChange={e => updateParams({ q: e.target.value })}
          placeholder="Search products..."
        />

        <PriceRangeFilter
          min={minPrice}
          max={maxPrice}
          onChange={(min, max) => updateParams({ minPrice: String(min), maxPrice: String(max) })}
        />

        <CategoryFilter
          selected={categories}
          onChange={cats => updateParams({ category: cats })}
        />

        <select
          value={sort}
          onChange={e => updateParams({ sort: e.target.value })}
        >
          <option value="name">Name A-Z</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
        </select>
      </aside>

      <main>
        {(loading || isPending) && <ProductGrid>{Array.from({length: 12}).map((_, i) => <ProductSkeleton key={i} />)}</ProductGrid>}
        {!loading && !isPending && products && <ProductGrid>{products.map(p => <ProductCard key={p.id} product={p} />)}</ProductGrid>}
      </main>
    </div>
  );
}
```

**Review checklist:**
- [ ] URL sync — filters are shareable/bookmarkable
- [ ] `useTransition` — marks URL update as non-urgent (input stays responsive)
- [ ] Debounce on search query before API call
- [ ] Loading state shown during navigation and fetch

---

## Design 4 — Feature Flag Dashboard (Next.js, 20 min)

### Requirements

- Fetch feature flags from `/api/flags` with `{ name, enabled, rollout }` shape
- Toggle flags with optimistic update
- Show rollout percentage with live edit
- Admin-only — protected route

### Quick Implementation Pattern

```tsx
// This design tests: Server Component + Client Island, optimistic updates, Server Action

// app/admin/flags/page.tsx — Server Component fetches
import { redirect } from 'next/navigation';
export default async function FlagsPage() {
  const session = await getSession();
  if (!session?.isAdmin) redirect('/login');
  
  const flags = await getFlagsFromDB();
  return <FlagsDashboard initialFlags={flags} />;
}

// components/FlagsDashboard.tsx — Client Component manages interaction
'use client';

import { useOptimistic, startTransition } from 'react';
import { toggleFlag, updateRollout } from '@/actions/flags';

export function FlagsDashboard({ initialFlags }: { initialFlags: Flag[] }) {
  const [optimisticFlags, dispatch] = useOptimistic(
    initialFlags,
    (state: Flag[], update: { id: string; enabled?: boolean; rollout?: number }) =>
      state.map(f => f.id === update.id ? { ...f, ...update } : f)
  );

  return (
    <table>
      {optimisticFlags.map(flag => (
        <tr key={flag.id}>
          <td>{flag.name}</td>
          <td>
            <input
              type="checkbox"
              checked={flag.enabled}
              onChange={async () => {
                startTransition(() => dispatch({ id: flag.id, enabled: !flag.enabled }));
                await toggleFlag(flag.id, !flag.enabled);
              }}
            />
          </td>
          <td>
            <input
              type="range" min={0} max={100} value={flag.rollout}
              onChange={e => {
                startTransition(() => dispatch({ id: flag.id, rollout: Number(e.target.value) }));
              }}
              onMouseUp={e => updateRollout(flag.id, Number((e.target as HTMLInputElement).value))}
            />
            {flag.rollout}%
          </td>
        </tr>
      ))}
    </table>
  );
}
```

---

## Scoring Guide

For each design, score yourself:

| Score | Criteria |
|---|---|
| 5 | Complete working implementation, all checklist items, clean architecture |
| 4 | Works, minor gaps (missing edge case or cleanup) |
| 3 | Core logic correct, missing loading/error/cleanup |
| 2 | Partial — got state shape but missing event handling or hooks |
| 1 | Outline only — no working code |

Target: all designs at 4+ before your interview.
