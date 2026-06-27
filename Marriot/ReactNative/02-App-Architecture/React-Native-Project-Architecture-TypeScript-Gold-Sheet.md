# React Native Project Architecture And TypeScript - Gold Sheet

> Track File #6 of 20 - Group 2: App Architecture
> Level: scalable codebase structure for production teams

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Feature-based structure | High | Keeps large apps maintainable |
| TypeScript route/API types | Very high | Prevents common app bugs |
| API client boundaries | Very high | Network logic should not leak into UI |
| DTO vs domain model | High | Backend contracts differ from UI needs |
| Design system | High | Large apps need consistency |
| Environment config | High | Dev/stage/prod correctness |
| Dependency direction | High | Senior architecture signal |
| Monorepo/modules | Medium | Common in large companies |

MAANG signal:
You make screens thin and isolate API, domain, storage, and UI concerns.

---

## 2. Recommended Feature-Based Layout

```text
src/
  app/
    App.tsx
    navigation/
    providers/
  core/
    api/
    config/
    logging/
    storage/
    telemetry/
  design-system/
    components/
    tokens/
  features/
    auth/
      api/
      components/
      hooks/
      screens/
      types.ts
    products/
      api/
      components/
      hooks/
      screens/
      types.ts
    checkout/
      api/
      components/
      hooks/
      screens/
      types.ts
  shared/
    components/
    hooks/
    utils/
```

Dependency direction:

```text
screens -> feature hooks/components -> feature api/domain -> core api/storage
design-system -> no business imports
core -> no feature imports
```

Avoid:
- `utils/` becoming a junk drawer.
- API calls directly inside presentational components.
- Domain logic importing navigation objects.
- Circular dependencies between features.

---

## 3. Screen-Hook-Component Pattern

```text
ProductDetailsScreen
  useProductDetails(productId)
  ProductDetailsHeader
  ProductPrice
  AddToCartButton
```

Screen:

```tsx
export function ProductDetailsScreen({route}: ProductDetailsScreenProps) {
  const {productId} = route.params;
  const viewModel = useProductDetails(productId);

  return <ProductDetailsView {...viewModel} />;
}
```

Hook:

```tsx
export function useProductDetails(productId: string) {
  const product = useProduct(productId);
  const addToCart = useAddToCartMutation();

  return {
    product: product.data,
    isLoading: product.isLoading,
    error: product.error,
    onAddToCart: () => addToCart.mutate({productId}),
  };
}
```

Why this works:
- Screen handles routing.
- Hook handles behavior/data.
- View handles rendering.
- Business behavior is testable without mounting the whole navigator.

---

## 4. API Client Boundary

Core client:

```ts
type RequestOptions = {
  signal?: AbortSignal;
  headers?: Record<string, string>;
};

export async function getJson<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const response = await fetch(`${config.apiBaseUrl}${path}`, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
    signal: options.signal,
  });

  if (!response.ok) {
    throw await mapApiError(response);
  }

  return response.json() as Promise<T>;
}
```

Feature API:

```ts
type ProductDto = {
  id: string;
  name: string;
  price_cents: number;
};

export type Product = {
  id: string;
  name: string;
  priceCents: number;
};

function toProduct(dto: ProductDto): Product {
  return {
    id: dto.id,
    name: dto.name,
    priceCents: dto.price_cents,
  };
}

export async function getProduct(productId: string): Promise<Product> {
  const dto = await getJson<ProductDto>(`/products/${productId}`);
  return toProduct(dto);
}
```

Why map DTOs:
- Backend naming may not match UI naming.
- API can expose fields the app should not depend on.
- Mapping gives a stable domain shape.
- Tests can catch contract drift.

---

## 5. TypeScript Practices

Use TypeScript for:
- Route params.
- API responses.
- Component props.
- Form models.
- Feature flags.
- Analytics events.
- Native module interfaces.

Avoid:
- `any` as an escape hatch.
- Optional fields everywhere.
- Duplicating similar types without clear reason.
- Trusting API response types without runtime validation when data is untrusted.

Example event typing:

```ts
type AnalyticsEvent =
  | {name: 'product_viewed'; productId: string}
  | {name: 'checkout_started'; cartId: string; itemCount: number}
  | {name: 'login_failed'; reason: 'invalid_credentials' | 'network'};

export function track(event: AnalyticsEvent) {
  telemetry.track(event.name, event);
}
```

Benefit:
You cannot accidentally send `checkout_started` without `cartId`.

---

## 6. Environment Configuration

Common environments:

```text
development
staging
production
```

Config values:
- API base URL.
- Sentry/crash DSN.
- analytics key.
- feature flag environment.
- OAuth client IDs.
- build channel.

Rules:
- Do not hardcode production endpoints in feature code.
- Do not put secrets in mobile config. Anything shipped to the app can be extracted.
- Use build-time config for public values.
- Fetch sensitive capability from backend after auth.

Interview answer:

```text
Mobile environment config should be treated as public. I use it for environment
selection and public identifiers, but never for true secrets. The backend owns
secrets and issues scoped tokens or signed URLs when needed.
```

---

## 7. Design System Architecture

Design system includes:
- tokens: color, spacing, typography
- primitives: Button, TextField, Card, Icon
- composition components: EmptyState, ErrorState, LoadingState
- accessibility defaults
- dark mode support

Example:

```tsx
type ButtonProps = {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
};

export function Button({label, onPress, variant = 'primary', disabled}: ButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={[styles.base, styles[variant], disabled && styles.disabled]}
    >
      <Text style={styles.label}>{label}</Text>
    </Pressable>
  );
}
```

Production benefit:
Product changes happen in one component instead of hundreds of screens.

---

## 8. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Screen owns API + validation + UI + analytics | Untestable and hard to optimize | Split screen/hook/view/service |
| Backend DTO used everywhere | UI becomes coupled to API shape | Map DTO to app model |
| `any` for route params | Runtime navigation bugs | Typed param lists |
| Global `utils` dumping ground | No ownership | Feature/shared/core boundaries |
| Secrets in app config | Mobile apps are inspectable | Keep secrets on backend |
| No design system | Visual inconsistency | Build tokens/primitives |

---

## 9. Strong Interview Answer

Question:
How would you structure a large React Native app?

Strong answer:

```text
I prefer a feature-based structure with shared core infrastructure and a design
system. Screens are thin and mostly handle routing. Feature hooks own data fetching,
mutations, and view-model behavior. API modules map backend DTOs into app models.
Core modules own cross-cutting concerns like API clients, storage, telemetry, and
configuration. TypeScript covers route params, API contracts, component props,
analytics events, and native module interfaces. This keeps dependencies clear and
lets teams work independently without turning the app into a single shared state blob.
```

---

## 10. Revision Notes

- One-line summary: Large RN apps need feature boundaries, typed contracts, and thin screens.
- Three keywords: feature folders, TypeScript, DTO mapping.
- One interview trap: Mobile config values are not secrets.
- One memory trick: Screen coordinates, hook behaves, view renders, service talks outside.

