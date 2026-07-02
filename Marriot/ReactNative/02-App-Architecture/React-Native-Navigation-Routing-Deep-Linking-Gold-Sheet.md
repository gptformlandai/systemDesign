# React Native Navigation, Routing, And Deep Linking - Gold Sheet

> Track Module - Group 2: App Architecture
> Level: production navigation design and interview-ready routing judgment

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Stack navigation | Very high | Most apps have screen-to-screen flow |
| Tabs and drawers | High | Common product navigation patterns |
| Auth navigation | Very high | Login/logout flow is a top interview scenario |
| Route params | Very high | Data passing and type safety |
| Deep linking | High | Push notifications, sharing, universal links |
| Navigation lifecycle | High | Refetching on focus, cleanup, analytics |
| Android back behavior | High | Platform-specific correctness |
| Expo Router vs React Navigation | Medium-high | Modern projects often use one or both concepts |

MAANG signal:
You can design navigation as application state, not just "go to screen" calls.

---

## 2. Mental Model

Navigation is a tree of navigators.

```text
Root
  AuthStack
    Login
    Register
  AppTabs
    HomeStack
      Home
      Details
    SearchStack
      Search
      Result
    AccountStack
      Account
      Settings
```

Each navigator owns a portion of route state. Screens receive route params and navigation actions.

---

## 3. Definition

- Definition: Navigation manages which screen is visible, how screens transition, and how route state maps to URLs/deep links/back behavior.
- Category: App shell architecture.
- Core idea: Make screen flow predictable, typed, recoverable, and platform-correct.

---

## 4. React Navigation vs Expo Router

### React Navigation

React Navigation is the common navigation library for React Native.

Good fit:
- Explicit navigator trees.
- Complex stack/tab/drawer combinations.
- Existing RN apps.
- Fine control over screen options and lifecycle.

### Expo Router

Expo Router uses file-based routing on top of React Navigation concepts.

Good fit:
- Expo apps.
- URL-like mental model.
- Teams that like Next.js-style file routing.
- Universal deep-link/web support.

Interview answer:

```text
React Navigation gives explicit navigator configuration. Expo Router gives a
file-based routing model and is convenient for Expo apps. Under the hood, both
still require understanding stacks, tabs, params, deep links, and mobile lifecycle.
```

---

## 5. Typed Stack Navigation

```tsx
import {createNativeStackNavigator} from '@react-navigation/native-stack';

export type RootStackParamList = {
  Home: undefined;
  ProductDetails: {productId: string};
  Checkout: {cartId: string; couponCode?: string};
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Home" component={HomeScreen} />
      <Stack.Screen name="ProductDetails" component={ProductDetailsScreen} />
      <Stack.Screen name="Checkout" component={CheckoutScreen} />
    </Stack.Navigator>
  );
}
```

Navigate:

```tsx
navigation.navigate('ProductDetails', {productId: product.id});
```

Trap:
Do not pass full product objects through route params. Pass stable IDs and fetch/read from cache.

---

## 6. Auth Navigation Pattern

Good auth flow:

```tsx
export function AppNavigator() {
  const {status} = useAuthSession();

  if (status === 'loading') {
    return <SplashScreen />;
  }

  return (
    <NavigationContainer>
      {status === 'authenticated' ? <AppTabs /> : <AuthStack />}
    </NavigationContainer>
  );
}
```

Why this is good:
- Auth state decides available routes.
- Logged-out users cannot navigate back into authenticated screens.
- Splash/loading prevents flicker while token restore runs.

Common mistake:
Calling `navigation.navigate('Home')` after login while leaving login screen in history. On Android back, user can return to login.

Better:
Switch navigator tree or reset navigation state.

---

## 7. Deep Linking

Deep linking maps an external URL or app link to navigation state.

Examples:

```text
myapp://product/123
https://example.com/product/123
```

Conceptual config:

```tsx
const linking = {
  prefixes: ['myapp://', 'https://example.com'],
  config: {
    screens: {
      Home: '',
      ProductDetails: 'product/:productId',
      Checkout: 'checkout/:cartId',
    },
  },
};

<NavigationContainer linking={linking}>
  <RootNavigator />
</NavigationContainer>;
```

Production concerns:
- Validate params.
- Handle auth-required links.
- Handle deleted/missing resources.
- Track link source for analytics.
- Avoid leaking secrets in URLs.
- Test cold start and warm app cases.

---

## 8. Screen Focus Lifecycle

Use focus events when work should happen only when the screen is visible.

```tsx
import {useFocusEffect} from '@react-navigation/native';
import {useCallback} from 'react';

function OrdersScreen() {
  useFocusEffect(
    useCallback(() => {
      refreshOrders();
      return () => {
        stopPollingOrders();
      };
    }, []),
  );

  return <OrdersList />;
}
```

Use cases:
- Refreshing stale data when returning to a screen.
- Starting/stopping polling.
- Screen analytics.
- Subscribing to local events.

Trap:
Do not refetch aggressively on every focus if the cache is fresh.

---

## 9. Android Back Button

Android users expect hardware/software back to work naturally.

Guidelines:
- Stack back should go to previous screen.
- Root tab back may exit app or return to first tab depending product rules.
- Confirmation screens should prevent accidental loss.
- Modals should close before exiting app.

Example:

```tsx
useFocusEffect(
  useCallback(() => {
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (hasUnsavedChanges) {
        showDiscardDialog();
        return true;
      }
      return false;
    });

    return () => subscription.remove();
  }, [hasUnsavedChanges]),
);
```

---

## 10. Navigation Design Trade-offs

| Choice | Pros | Cons |
|---|---|---|
| Native stack | Smooth platform-native transitions | Some customization constraints |
| JS stack | More JS-level control | More exposed to JS thread stalls |
| File-based routing | Easy convention and deep link mapping | Less explicit for complex flows |
| Explicit navigator tree | Very clear architecture | More setup code |
| Passing IDs | Stable and serializable | Requires cache/fetch lookup |
| Passing objects | Fast for prototypes | Stale, large, non-serializable route state |

---

## 11. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| One giant root stack | Becomes unmanageable | Compose auth/app/tabs/stacks |
| Passing large objects as params | Stale and non-serializable | Pass IDs |
| Ignoring auth-required deep links | Bad UX/security | Store intended link and continue after login |
| Refetching on every focus | Wastes battery/network | Use stale time/cache policy |
| Not testing Android back | Platform regression | Add scenario tests |
| Navigation from random service code | Tight coupling | Use events/state or navigation ref carefully |

---

## 12. Strong Interview Answer

Question:
How would you design navigation for a React Native ecommerce app?

Strong answer:

```text
I would structure it as a root auth switch. While restoring the session I show a
splash/loading state. Logged-out users see an AuthStack. Logged-in users see tabs
like Home, Search, Cart, and Account, with each tab owning its own stack. Product
details receive productId as a typed route param, not the whole product object.
Deep links map to product and checkout routes, but auth-required links are held
until login completes. I would test cold-start deep links, warm links, Android
back behavior, and logout reset so users cannot back-navigate into private screens.
```

---

## 13. Revision Notes

- One-line summary: Navigation is app state plus platform back/deep-link behavior.
- Three keywords: stack, params, deep links.
- One interview trap: Do not pass large objects through route params.
- One memory trick: Auth decides route tree; route params should be serializable IDs.

