# React Native GraphQL — Apollo Client & URQL — Gold Sheet

> Track Module - Group 4: Senior / MAANG Path
> Level: senior | Mode: understand GraphQL in the mobile context and make the right client choice

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| GraphQL vs REST — when to choose which | Very high | Design question staple |
| Apollo Client setup and cache | High | Most widely used GraphQL client |
| Query, Mutation, Subscription — all three | Very high | Core GraphQL operations |
| Fragment colocation with components | High | Senior GraphQL pattern |
| Optimistic UI with mutations | High | Same concept as REST but cache-centric |
| Normalized cache — how Apollo InMemoryCache works | High | Cache hit/miss determines performance |
| URQL vs Apollo — trade-offs | Medium | Shows you understand the ecosystem |
| Subscriptions with WebSocket on mobile | Medium | Real-time mobile use case |

---

## 2. Mental Model — Why GraphQL on Mobile

```text
REST problems in mobile:
  - Over-fetching: GET /users/123 returns 30 fields, mobile needs 5
  - Under-fetching: screen needs user + orders + addresses — 3 REST calls = 3 round trips
  - Versioning: backend evolves, mobile clients get stale endpoints
  
GraphQL solution:
  - Client declares exactly what it needs
  - Single request returns exactly those fields
  - Strongly typed schema = codegen for TypeScript types
  - Subscriptions = real-time over WebSocket with same query language
```

```graphql
# REST: 3 round trips
GET /users/123
GET /users/123/orders
GET /users/123/addresses

# GraphQL: 1 round trip
query GetUserProfile($userId: ID!) {
  user(id: $userId) {
    id
    name
    email
    orders(first: 5) {
      id
      status
      total
    }
    addresses {
      street
      city
    }
  }
}
```

---

## 3. Apollo Client Setup for React Native

```bash
npx expo install @apollo/client graphql
```

```tsx
// apollo/client.ts
import {ApolloClient, InMemoryCache, createHttpLink, from} from '@apollo/client';
import {setContext} from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import {getAuthToken} from '../services/auth';

const httpLink = createHttpLink({
  uri: 'https://api.example.com/graphql',
});

// Auth header injection
const authLink = setContext(async (_, {headers}) => {
  const token = await getAuthToken();
  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : '',
    },
  };
});

// Error handling link
const errorLink = onError(({graphQLErrors, networkError}) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({message, extensions}) => {
      if (extensions?.code === 'UNAUTHENTICATED') {
        // Token expired — redirect to login
        navigationRef.navigate('Login');
      }
      console.error('GraphQL error:', message);
    });
  }
  if (networkError) {
    console.error('Network error:', networkError);
  }
});

export const apolloClient = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          products: {
            keyArgs: ['category'],  // cache separate per category
            merge(existing = [], incoming) {
              return [...existing, ...incoming]; // for pagination
            },
          },
        },
      },
    },
  }),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network', // show cached, then update from server
    },
  },
});

// App.tsx
import {ApolloProvider} from '@apollo/client';

<ApolloProvider client={apolloClient}>
  <NavigationContainer>...</NavigationContainer>
</ApolloProvider>
```

---

## 4. Queries — Reading Data

### Basic query with loading/error

```tsx
import {useQuery, gql} from '@apollo/client';

const GET_USER = gql`
  query GetUser($id: ID!) {
    user(id: $id) {
      id
      name
      email
      avatar
    }
  }
`;

type GetUserData = {user: {id: string; name: string; email: string; avatar: string}};
type GetUserVariables = {id: string};

function UserProfile({userId}: {userId: string}) {
  const {data, loading, error, refetch} = useQuery<GetUserData, GetUserVariables>(
    GET_USER,
    {
      variables: {id: userId},
      fetchPolicy: 'cache-and-network',
    },
  );

  if (loading && !data) return <ActivityIndicator />;
  if (error) return <ErrorScreen message={error.message} onRetry={refetch} />;
  if (!data) return null;

  const {user} = data;
  return (
    <View>
      <Text>{user.name}</Text>
      <Text>{user.email}</Text>
    </View>
  );
}
```

### fetchPolicy options

| Policy | Behavior | Use When |
|---|---|---|
| `cache-first` (default) | Use cache if available, skip network | Static data, rare updates |
| `cache-and-network` | Show cache immediately, also fetch from network | Lists, feeds — avoid stale flash |
| `network-only` | Always fetch, skip cache entirely | Critical real-time data |
| `cache-only` | Only read from cache, throw if missing | Offline mode |
| `no-cache` | Always fetch, do not write to cache | Sensitive data |

---

## 5. Mutations — Writing Data

```tsx
import {useMutation, gql} from '@apollo/client';

const ADD_TO_CART = gql`
  mutation AddToCart($productId: ID!, $quantity: Int!) {
    addToCart(productId: $productId, quantity: $quantity) {
      id
      items {
        id
        product {
          id
          name
          price
        }
        quantity
      }
      total
    }
  }
`;

function ProductCard({product}: {product: Product}) {
  const [addToCart, {loading}] = useMutation(ADD_TO_CART, {
    variables: {productId: product.id, quantity: 1},

    // Optimistic UI — update cache before server responds
    optimisticResponse: {
      addToCart: {
        __typename: 'Cart',
        id: 'current-cart',
        items: [], // simplified — real implementation would include existing items
        total: 0,
      },
    },

    // After mutation, refetch the cart to get accurate data
    refetchQueries: [{query: GET_CART}],

    onCompleted: () => showToast('Added to cart'),
    onError: err => showToast(`Failed: ${err.message}`),
  });

  return (
    <Pressable
      onPress={() => addToCart()}
      disabled={loading}>
      <Text>{loading ? 'Adding...' : 'Add to Cart'}</Text>
    </Pressable>
  );
}
```

### Manual cache update — more precise than refetchQueries

```tsx
const [removeFromCart] = useMutation(REMOVE_FROM_CART, {
  update(cache, {data}) {
    // Read current cart from cache
    const existing = cache.readQuery<GetCartData>({query: GET_CART});
    if (!existing) return;

    // Write filtered cart back to cache
    cache.writeQuery({
      query: GET_CART,
      data: {
        cart: {
          ...existing.cart,
          items: existing.cart.items.filter(
            item => item.id !== data.removeFromCart.removedItemId,
          ),
        },
      },
    });
  },
});
```

---

## 6. Subscriptions — Real-Time Data

Subscriptions use WebSocket to push data from server to client:

```tsx
import {useSubscription, gql} from '@apollo/client';
import {GraphQLWsLink} from '@apollo/client/link/subscriptions';
import {createClient} from 'graphql-ws';

// Setup WebSocket link alongside HTTP link (split by operation type)
import {split, HttpLink} from '@apollo/client';
import {getMainDefinition} from '@apollo/client/utilities';

const wsLink = new GraphQLWsLink(
  createClient({
    url: 'wss://api.example.com/graphql',
    connectionParams: async () => ({
      authorization: `Bearer ${await getAuthToken()}`,
    }),
  }),
);

const splitLink = split(
  ({query}) => {
    const def = getMainDefinition(query);
    return def.kind === 'OperationDefinition' && def.operation === 'subscription';
  },
  wsLink,
  httpLink,
);

// Using subscription in a component
const ORDER_UPDATED = gql`
  subscription OrderUpdated($orderId: ID!) {
    orderUpdated(orderId: $orderId) {
      id
      status
      updatedAt
    }
  }
`;

function OrderTracker({orderId}: {orderId: string}) {
  const {data, loading} = useSubscription(ORDER_UPDATED, {
    variables: {orderId},
  });

  const status = data?.orderUpdated?.status ?? 'Loading...';

  return (
    <View>
      <Text>Order Status: {status}</Text>
      {loading && <Text style={styles.small}>Connecting...</Text>}
    </View>
  );
}
```

---

## 7. Fragment Colocation — The Senior Pattern

Fragments let components declare exactly what data they need. The parent query assembles fragments:

```tsx
// components/ProductCard.tsx — component owns its data requirements
export const PRODUCT_CARD_FRAGMENT = gql`
  fragment ProductCardFields on Product {
    id
    name
    price
    thumbnail
    inStock
  }
`;

function ProductCard({product}: {product: ProductCardFields}) {
  return (
    <View style={styles.card}>
      <Image source={{uri: product.thumbnail}} style={styles.image} />
      <Text style={styles.name}>{product.name}</Text>
      <Text style={styles.price}>${product.price}</Text>
      {!product.inStock && <Text style={styles.soldOut}>Sold out</Text>}
    </View>
  );
}
```

```tsx
// screens/ProductListScreen.tsx — composes fragments into one query
const GET_PRODUCTS = gql`
  ${PRODUCT_CARD_FRAGMENT}
  query GetProducts($category: String!) {
    products(category: $category) {
      ...ProductCardFields
    }
  }
`;

function ProductListScreen({category}: {category: string}) {
  const {data} = useQuery(GET_PRODUCTS, {variables: {category}});
  return (
    <FlatList
      data={data?.products}
      renderItem={({item}) => <ProductCard product={item} />}
      keyExtractor={p => p.id}
    />
  );
}
```

Benefits of fragment colocation:
1. If `ProductCard` needs a new field, it updates its own fragment — no other file changes
2. The query is always exactly what the UI needs — zero over-fetching
3. Types can be generated from fragments for zero-effort TypeScript

---

## 8. Apollo Cache — InMemoryCache Internals

Apollo normalizes the cache by `__typename` + `id`. This means the same object is stored once regardless of how many queries fetched it:

```text
Cache normalization example:

Query 1 returns:
  user { id: "123", name: "Alice" }

Query 2 returns:
  order { id: "456", user { id: "123", name: "Alice Updated" } }

Apollo cache stores:
  User:123 → {id: "123", name: "Alice Updated"}  (single record, updated)
  Order:456 → {id: "456", user: REF(User:123)}   (reference, not duplicate)

When user "123" is updated anywhere, all components reading it re-render automatically.
```

```tsx
// Objects must have an id (or a custom keyField) for normalization
new InMemoryCache({
  typePolicies: {
    Product: {
      keyFields: ['sku'],       // use 'sku' instead of 'id' as cache key
    },
    User: {
      keyFields: ['email'],     // email is the unique identifier
    },
    CartItem: {
      keyFields: false,         // not normalized — embedded in parent only
    },
  },
});
```

---

## 9. URQL — Lighter Alternative to Apollo

URQL is a lighter GraphQL client that is more tree-shakeable and has a simpler setup:

```tsx
import {createClient, cacheExchange, fetchExchange, Provider} from 'urql';

const client = createClient({
  url: 'https://api.example.com/graphql',
  exchanges: [cacheExchange, fetchExchange],
  fetchOptions: async () => {
    const token = await getAuthToken();
    return {headers: {authorization: token ? `Bearer ${token}` : ''}};
  },
});

// App.tsx
<Provider value={client}>
  <NavigationContainer>...</NavigationContainer>
</Provider>

// Component
import {useQuery} from 'urql';
const [{data, fetching, error}] = useQuery({query: GET_USER, variables: {id: userId}});
```

### Apollo vs URQL Trade-offs

| | Apollo Client | URQL |
|---|---|---|
| Bundle size | ~47 KB gzipped | ~17 KB gzipped |
| Cache | Normalized InMemoryCache | Document-based + Normalized Exchange |
| Ecosystem | Very large (Apollo Studio, subscriptions, codegen) | Growing |
| Complexity | High — powerful but steep learning curve | Lower — simpler mental model |
| Offline support | Apollo Persisted Queries, cache persistence | `@urql/exchange-graphcache` |
| When to choose | Large enterprise apps, complex caching needs | Smaller apps, performance-sensitive bundles |

Interview answer on trade-off:
```text
For a large e-commerce app with complex data relationships and a team already using
Apollo Studio for query tracing, I would choose Apollo — the normalized cache and
ecosystem justify the complexity. For a smaller app or one where bundle size matters
(React Native on slower devices), URQL is a solid choice with a simpler mental model.
```

---

## 10. GraphQL Code Generator — Zero-Effort TypeScript

```bash
npm install -D @graphql-codegen/cli @graphql-codegen/typescript @graphql-codegen/typescript-operations @graphql-codegen/typescript-react-apollo
```

```yaml
# codegen.yml
schema: 'https://api.example.com/graphql'
documents: 'src/**/*.tsx'
generates:
  src/generated/graphql.ts:
    plugins:
      - typescript
      - typescript-operations
      - typescript-react-apollo
    config:
      withHooks: true
```

```bash
npx graphql-codegen
```

Result: TypeScript types and typed hooks generated from your schema and queries:
```tsx
// Auto-generated — strongly typed
import {useGetUserQuery} from '../generated/graphql';

function UserProfile({userId}: {userId: string}) {
  const {data, loading, error} = useGetUserQuery({variables: {id: userId}});
  // data is fully typed — no manual type annotations needed
}
```

---

## 11. Common GraphQL Traps in React Native

### Trap 1: Forgetting `__typename` in optimistic response

Apollo uses `__typename` + `id` to normalize. Optimistic responses must include `__typename`:
```tsx
optimisticResponse: {
  addToCart: {
    __typename: 'Cart',  // must include this
    id: 'cart-1',
    ...
  }
}
```

### Trap 2: N+1 on the client — fragment not colocated

```tsx
// Instead of one query that returns everything, calling refetch per item
items.forEach(item => refetchItemDetails(item.id)); // N network calls!

// Correct — one query with fragments
const {data} = useQuery(GET_ALL_WITH_DETAILS, {variables: {ids: items.map(i => i.id)}});
```

### Trap 3: Not handling subscription reconnect

WebSocket connections drop on mobile (background, airplane mode). Always configure reconnect:
```tsx
createClient({
  url: 'wss://api.example.com/graphql',
  retryAttempts: Infinity,    // keep retrying
  shouldRetry: () => true,    // always retry
  retryWait: (retries) => new Promise(res => setTimeout(res, 2 ** retries * 1000)),
});
```

---

## 12. Revision Notes

- GraphQL solves mobile over-fetching and under-fetching with a single typed query
- Apollo Client setup: HTTP link + auth link + error link → `from([...])` → ApolloClient
- `fetchPolicy: 'cache-and-network'` is the mobile-friendly default — show cache, then update
- Mutations: `optimisticResponse` for instant UX, `update` callback for manual cache writes, `refetchQueries` for safety
- Subscriptions use WebSocket — set up reconnect with retry for mobile network interruptions
- Fragment colocation: each component owns its data requirements — parent query assembles fragments
- InMemoryCache normalizes by `__typename + id` — same object updated anywhere propagates everywhere
- Apollo vs URQL: Apollo for large apps with complex caching; URQL for smaller/bundle-sensitive apps
- Use GraphQL Code Generator to derive TypeScript types from schema — zero manual type maintenance
- Always include `__typename` in optimistic responses for correct cache normalization
