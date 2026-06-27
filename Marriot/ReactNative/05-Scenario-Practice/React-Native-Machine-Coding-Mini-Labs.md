# React Native Machine Coding Mini Labs

> Track File #15 of 20 - Group 5: Scenario Practice
> Level: hands-on patterns for interviews and real apps

---

## Lab 1: Debounced Search Hook

Task:
Implement a reusable hook that returns a debounced value.

Solution:

```tsx
import {useEffect, useState} from 'react';

export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebounced(value);
    }, delayMs);

    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}
```

Interview explanation:

```text
This avoids firing a search request on every keystroke. The cleanup cancels the
previous timer whenever the value changes, so only the latest stable value emits.
```

Follow-up:
How would you cancel the previous network request too?

---

## Lab 2: Retry With Exponential Backoff

Task:
Implement retry for safe requests.

Solution:

```ts
type RetryOptions = {
  retries: number;
  baseDelayMs: number;
};

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function delayForAttempt(attempt: number, baseDelayMs: number) {
  const exponential = baseDelayMs * 2 ** attempt;
  const jitter = Math.floor(Math.random() * 200);
  return Math.min(exponential + jitter, 30_000);
}

export async function retry<T>(
  operation: () => Promise<T>,
  options: RetryOptions,
): Promise<T> {
  let lastError: unknown;

  for (let attempt = 0; attempt <= options.retries; attempt += 1) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;

      if (attempt === options.retries) {
        break;
      }

      await sleep(delayForAttempt(attempt, options.baseDelayMs));
    }
  }

  throw lastError;
}
```

Interview explanation:

```text
I only use this automatically for safe/idempotent operations. For payments or
state-changing mutations, retries need idempotency keys and backend support.
```

---

## Lab 3: Typed Navigation Params

Task:
Type a stack with product details route params.

Solution:

```tsx
import type {NativeStackScreenProps} from '@react-navigation/native-stack';

type RootStackParamList = {
  Home: undefined;
  ProductDetails: {productId: string};
};

type ProductDetailsProps = NativeStackScreenProps<
  RootStackParamList,
  'ProductDetails'
>;

export function ProductDetailsScreen({route}: ProductDetailsProps) {
  return <ProductDetails productId={route.params.productId} />;
}
```

Interview explanation:

```text
Route params should be serializable and typed. I pass IDs rather than large objects
so the destination screen can read from cache or fetch fresh data.
```

---

## Lab 4: Optimized FlatList Row

Task:
Implement a list with stable keys and memoized rows.

Solution:

```tsx
type Message = {
  id: string;
  author: string;
  body: string;
};

const MessageRow = memo(function MessageRow({message}: {message: Message}) {
  return (
    <View style={styles.row}>
      <Text style={styles.author}>{message.author}</Text>
      <Text>{message.body}</Text>
    </View>
  );
});

export function MessageList({messages}: {messages: Message[]}) {
  const renderItem = useCallback(
    ({item}: {item: Message}) => <MessageRow message={item} />,
    [],
  );

  return (
    <FlatList
      data={messages}
      keyExtractor={item => item.id}
      renderItem={renderItem}
    />
  );
}
```

Interview explanation:

```text
Stable item IDs preserve identity. Memoized rows and stable renderItem reduce
unnecessary JS work, but I would still profile before over-tuning props.
```

---

## Lab 5: Offline Mutation Queue

Task:
Model a safe offline queue item.

Solution:

```ts
type QueueStatus = 'pending' | 'syncing' | 'failed';

type OfflineQueueItem<TPayload> = {
  id: string;
  idempotencyKey: string;
  type: 'create_note' | 'update_note';
  payload: TPayload;
  status: QueueStatus;
  retryCount: number;
  createdAt: string;
};

export function createQueueItem<TPayload>(
  type: OfflineQueueItem<TPayload>['type'],
  payload: TPayload,
): OfflineQueueItem<TPayload> {
  const id = crypto.randomUUID();

  return {
    id,
    idempotencyKey: id,
    type,
    payload,
    status: 'pending',
    retryCount: 0,
    createdAt: new Date().toISOString(),
  };
}
```

Interview explanation:

```text
The idempotency key prevents duplicate server effects when the app retries after
network loss or process restart.
```

Note:
In React Native, `crypto.randomUUID` availability depends on runtime/polyfills. In production, use a vetted UUID library or platform-supported generator.

---

## Lab 6: Form Validation Function

Task:
Create a pure validation function for testability.

Solution:

```ts
type SignupForm = {
  email: string;
  password: string;
};

type SignupErrors = Partial<Record<keyof SignupForm, string>>;

export function validateSignup(form: SignupForm): SignupErrors {
  const errors: SignupErrors = {};

  if (!form.email.includes('@')) {
    errors.email = 'Enter a valid email.';
  }

  if (form.password.length < 8) {
    errors.password = 'Use at least 8 characters.';
  }

  return errors;
}
```

Test:

```ts
it('requires valid email and password length', () => {
  expect(validateSignup({email: 'bad', password: '123'})).toEqual({
    email: 'Enter a valid email.',
    password: 'Use at least 8 characters.',
  });
});
```

Interview explanation:

```text
Keeping validation pure makes it easy to unit test. The screen can focus on input,
keyboard, accessibility, server errors, and submission state.
```

---

## Lab 7: Single-Flight Token Refresh

Task:
Ensure many failed requests share one refresh operation.

Solution:

```ts
let refreshPromise: Promise<string> | null = null;

async function refreshAccessTokenOnce(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken()
      .then(token => {
        tokenStore.setAccessToken(token);
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

export async function requestWithAuthRetry<T>(
  request: (token: string | null) => Promise<T>,
): Promise<T> {
  const token = await tokenStore.getAccessToken();

  try {
    return await request(token);
  } catch (error) {
    if (!(error instanceof AuthExpiredError)) {
      throw error;
    }

    const newToken = await refreshAccessTokenOnce();
    return request(newToken);
  }
}
```

Interview explanation:

```text
Single-flight refresh prevents every request that receives a 401 from starting
its own refresh call. I still retry only once and clear session if refresh fails.
```

---

## Lab 8: Accessible Icon Button Primitive

Task:
Create an icon-only button that cannot ship without an accessible label.

Solution:

```tsx
type IconButtonProps = {
  label: string;
  icon: React.ReactNode;
  onPress: () => void;
  disabled?: boolean;
};

export function IconButton({label, icon, onPress, disabled}: IconButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{disabled}}
      disabled={disabled}
      hitSlop={8}
      onPress={onPress}
      style={[styles.iconButton, disabled && styles.disabled]}
    >
      {icon}
    </Pressable>
  );
}
```

Interview explanation:

```text
Making the label required at the primitive level scales accessibility across the
app. Every feature that uses the button gets the screen reader behavior by default.
```

---

## Lab 9: Request Error Mapper

Task:
Map raw HTTP failures into UI-friendly categories.

Solution:

```ts
type ApiErrorKind =
  | 'timeout'
  | 'unauthorized'
  | 'forbidden'
  | 'not_found'
  | 'validation'
  | 'rate_limited'
  | 'server'
  | 'unknown';

export function mapStatusToErrorKind(status: number): ApiErrorKind {
  if (status === 401) return 'unauthorized';
  if (status === 403) return 'forbidden';
  if (status === 404) return 'not_found';
  if (status === 422) return 'validation';
  if (status === 429) return 'rate_limited';
  if (status >= 500) return 'server';
  return 'unknown';
}
```

Interview explanation:

```text
Error taxonomy lets UI, retry policy, and telemetry behave consistently. A 429
should not be handled the same way as a 401 or a validation error.
```

---

## Mini Lab Checklist

Practice implementing:
- debounced search
- retry with backoff
- typed route params
- optimized FlatList
- offline queue with idempotency key
- form validation
- auth navigator switch
- permission request flow
- app state listener cleanup
- analytics event typing
- single-flight token refresh
- accessible icon button primitive
- HTTP error taxonomy
