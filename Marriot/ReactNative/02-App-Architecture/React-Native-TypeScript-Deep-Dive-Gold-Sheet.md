# React Native TypeScript Deep Dive — Gold Sheet

> Track Module - Group 2: App Architecture
> Level: intermediate to senior | Mode: write fully typed React Native code that survives scale

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Typing navigation params — end-to-end | Very high | Most common TS pain point in RN |
| Discriminated unions for state machines | Very high | Clean async state, API response typing |
| Generic components with TypeScript | High | Reusable typed FlatList rows, form fields |
| `keyof`, `typeof`, mapped types | High | Dynamic prop typing |
| `React.ComponentProps` and extending native props | High | Building a design system |
| Type narrowing patterns | High | Handling `unknown` from network, events |
| Strict mode — what it enables | Medium | Interview signal for quality |
| Utility types in practice | High | `Partial`, `Required`, `Pick`, `Omit`, `Readonly` |

---

## 2. TypeScript Configuration for React Native

```json
// tsconfig.json — recommended strict setup
{
  "extends": "@react-native/typescript-config/tsconfig.json",
  "compilerOptions": {
    "strict": true,           // enables strictNullChecks, noImplicitAny, etc.
    "noUncheckedIndexedAccess": true,  // arr[0] returns T | undefined
    "exactOptionalPropertyTypes": true, // distinguishes undefined vs absent
    "paths": {
      "@components/*": ["src/components/*"],
      "@screens/*": ["src/screens/*"],
      "@hooks/*": ["src/hooks/*"],
      "@utils/*": ["src/utils/*"]
    }
  }
}
```

`strict: true` enables:
- `strictNullChecks` — `null` and `undefined` are not assignable without explicit handling
- `noImplicitAny` — every value must have a type
- `strictFunctionTypes` — contravariant function type checking
- `strictBindCallApply`
- `strictPropertyInitialization`
- `noImplicitThis`

Production rule: Always use `strict: true`. Teams that disable it accumulate type debt that becomes runtime crashes.

---

## 3. Typing Navigation Params — End-to-End

Navigation typing is the most-asked TypeScript topic in React Native interviews.

### Step 1: Define the param list

```tsx
// navigation/types.ts

export type RootStackParamList = {
  Home: undefined;                           // no params
  ProductDetail: {productId: string};        // required param
  Checkout: {items: CartItem[]; coupon?: string}; // optional param
  Profile: {userId: string; editing?: boolean};
  Modal: {title: string; message: string};
};

export type TabParamList = {
  HomeTab: undefined;
  SearchTab: {initialQuery?: string};
  CartTab: undefined;
  ProfileTab: undefined;
};
```

### Step 2: Type the navigator

```tsx
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import type {RootStackParamList} from './types';

const Stack = createNativeStackNavigator<RootStackParamList>();

function RootNavigator() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Home" component={HomeScreen} />
      <Stack.Screen name="ProductDetail" component={ProductDetailScreen} />
      <Stack.Screen name="Checkout" component={CheckoutScreen} />
    </Stack.Navigator>
  );
}
```

### Step 3: Type individual screen components

```tsx
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import type {RootStackParamList} from '../navigation/types';

// Screen receives typed route and navigation
type ProductDetailScreenProps = NativeStackScreenProps<RootStackParamList, 'ProductDetail'>;

export function ProductDetailScreen({route, navigation}: ProductDetailScreenProps) {
  const {productId} = route.params; // TypeScript knows this is string

  const handleCheckout = () => {
    navigation.navigate('Checkout', {
      items: cartItems,
      // coupon is optional — TypeScript allows omitting it
    });
  };
}
```

### Step 4: useNavigation and useRoute hooks — typed

```tsx
// Typed navigation hook
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {RootStackParamList} from '../navigation/types';

type HomeNavigation = NativeStackNavigationProp<RootStackParamList, 'Home'>;

function SomeNestedComponent() {
  const navigation = useNavigation<HomeNavigation>();
  // TypeScript knows every valid navigate() call
  navigation.navigate('ProductDetail', {productId: '123'}); // valid
  navigation.navigate('Home'); // valid — no params needed
  // navigation.navigate('ProductDetail'); // TypeScript error — missing productId
}

// Typed route hook
import {useRoute} from '@react-navigation/native';
import type {RouteProp} from '@react-navigation/native';

type ProductRoute = RouteProp<RootStackParamList, 'ProductDetail'>;

function ProductBreadcrumb() {
  const route = useRoute<ProductRoute>();
  return <Text>{route.params.productId}</Text>; // typed
}
```

---

## 4. Discriminated Unions — The Most Powerful Pattern

Discriminated unions use a shared `type` (or `kind`) field to create a type that TypeScript can narrow precisely.

### Async state machine

```tsx
type AsyncState<T> =
  | {status: 'idle'}
  | {status: 'loading'}
  | {status: 'success'; data: T}
  | {status: 'error'; error: string};

// Narrowing — TypeScript knows exactly which fields exist
function renderUser(state: AsyncState<User>) {
  switch (state.status) {
    case 'idle': return <Text>Not started</Text>;
    case 'loading': return <ActivityIndicator />;
    case 'success': return <Text>{state.data.name}</Text>; // .data exists here
    case 'error': return <Text>{state.error}</Text>;       // .error exists here
  }
}
```

### API response envelope

```tsx
type ApiResponse<T> =
  | {ok: true; data: T}
  | {ok: false; error: {code: string; message: string}};

async function getUser(id: string): Promise<ApiResponse<User>> {
  try {
    const res = await fetch(`/users/${id}`);
    if (!res.ok) return {ok: false, error: {code: `HTTP_${res.status}`, message: res.statusText}};
    return {ok: true, data: await res.json()};
  } catch {
    return {ok: false, error: {code: 'NETWORK', message: 'No connection'}};
  }
}

// Caller
const result = await getUser('123');
if (result.ok) {
  console.log(result.data.name); // TypeScript knows .data exists
} else {
  console.error(result.error.code); // TypeScript knows .error exists
}
```

### UI component variants

```tsx
type ButtonVariant =
  | {variant: 'primary'; label: string; onPress: () => void}
  | {variant: 'destructive'; label: string; onPress: () => void; confirmMessage: string}
  | {variant: 'loading'; label: string}; // no onPress — disabled while loading

function AppButton(props: ButtonVariant) {
  if (props.variant === 'loading') {
    return <ActivityIndicator />; // TypeScript knows no onPress here
  }
  if (props.variant === 'destructive') {
    // TypeScript knows confirmMessage exists here
    return <Pressable onPress={() => confirm(props.confirmMessage, props.onPress)}>...</Pressable>;
  }
  return <Pressable onPress={props.onPress}><Text>{props.label}</Text></Pressable>;
}
```

---

## 5. Generic Components

### Typed FlatList wrapper

```tsx
import {FlatList, type FlatListProps} from 'react-native';

type TypedListProps<T> = {
  data: T[];
  renderItem: (item: T, index: number) => React.ReactElement;
  keyExtractor: (item: T) => string;
  onEndReached?: () => void;
  ListEmptyComponent?: React.ReactElement;
};

function TypedList<T>({
  data,
  renderItem,
  keyExtractor,
  onEndReached,
  ListEmptyComponent,
}: TypedListProps<T>) {
  return (
    <FlatList<T>
      data={data}
      renderItem={({item, index}) => renderItem(item, index)}
      keyExtractor={keyExtractor}
      onEndReached={onEndReached}
      ListEmptyComponent={ListEmptyComponent}
    />
  );
}

// Usage — TypeScript infers T = Product
<TypedList
  data={products}
  renderItem={product => <ProductCard product={product} />}
  keyExtractor={p => p.id}
/>
```

### Generic form field

```tsx
type SelectFieldProps<T extends string | number> = {
  label: string;
  value: T;
  options: Array<{label: string; value: T}>;
  onChange: (value: T) => void;
};

function SelectField<T extends string | number>({
  label, value, options, onChange,
}: SelectFieldProps<T>) {
  return (
    <View>
      <Text>{label}</Text>
      {options.map(opt => (
        <Pressable
          key={String(opt.value)}
          onPress={() => onChange(opt.value)}
          style={opt.value === value ? styles.selected : styles.option}>
          <Text>{opt.label}</Text>
        </Pressable>
      ))}
    </View>
  );
}
```

---

## 6. Extending Native Component Props

When building a design system, extend native component props to add your own:

```tsx
import {TextInput, type TextInputProps, View, Text} from 'react-native';

type AppTextInputProps = TextInputProps & {
  label: string;
  error?: string;
  required?: boolean;
};

function AppTextInput({label, error, required, style, ...rest}: AppTextInputProps) {
  return (
    <View>
      <Text style={styles.label}>
        {label}{required && <Text style={styles.required}> *</Text>}
      </Text>
      <TextInput
        style={[styles.input, error ? styles.inputError : null, style]}
        {...rest}  // passes all TextInput props (placeholder, value, onChangeText, etc.)
      />
      {error && <Text style={styles.errorText}>{error}</Text>}
    </View>
  );
}
```

`React.ComponentProps<typeof TextInput>` is equivalent to `TextInputProps` and works for any component:
```tsx
type PressableStyleProp = React.ComponentProps<typeof Pressable>['style'];
```

---

## 7. Utility Types in Practice

### Partial — for update payloads

```tsx
type User = {id: string; name: string; email: string; role: 'admin' | 'user'};

// Update only the fields that changed
type UpdateUserPayload = Partial<Omit<User, 'id'>>;
// = {name?: string; email?: string; role?: 'admin' | 'user'}

async function updateUser(id: string, payload: UpdateUserPayload) {
  return fetch(`/users/${id}`, {method: 'PATCH', body: JSON.stringify(payload)});
}
```

### Pick and Omit — API shape contracts

```tsx
// Only expose what the UI needs
type UserCardProps = Pick<User, 'id' | 'name'>;

// Remove fields that should not be in the request body
type CreateUserPayload = Omit<User, 'id'>;
```

### Record — dictionaries

```tsx
type ProductById = Record<string, Product>;
type StatusMap = Record<'loading' | 'success' | 'error', string>;
```

### Required — enforce all fields present

```tsx
type DraftUser = Partial<User>;
// Before saving, ensure all fields are filled
function validateAndSave(draft: DraftUser): asserts draft is Required<User> {
  if (!draft.name || !draft.email) throw new Error('All fields required');
}
```

### Readonly — immutable data

```tsx
type ImmutableUser = Readonly<User>;
// user.name = 'change'; // TypeScript error

type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P];
};
```

---

## 8. Type Narrowing Patterns

### Handling unknown from network responses

```tsx
function isUser(val: unknown): val is User {
  return (
    typeof val === 'object' &&
    val !== null &&
    'id' in val &&
    'name' in val &&
    typeof (val as any).id === 'string' &&
    typeof (val as any).name === 'string'
  );
}

const raw: unknown = await response.json();
if (isUser(raw)) {
  setUser(raw); // TypeScript knows raw is User here
} else {
  throw new Error('Invalid user shape from API');
}
```

### Error narrowing

```tsx
// TypeScript catches have type unknown — always narrow
try {
  await fetchUser(id);
} catch (err) {
  // err is unknown — must narrow before accessing properties
  if (err instanceof Error) {
    setError(err.message);
  } else if (typeof err === 'string') {
    setError(err);
  } else {
    setError('An unknown error occurred');
  }
}
```

### Exhaustive checking — the switch trap

```tsx
type Shape = 'circle' | 'square' | 'triangle';

function area(shape: Shape, size: number): number {
  switch (shape) {
    case 'circle': return Math.PI * size ** 2;
    case 'square': return size ** 2;
    case 'triangle': return (Math.sqrt(3) / 4) * size ** 2;
    default: {
      // If a new shape is added but this switch is not updated,
      // TypeScript will error here because shape is never — not assignable to never
      const _exhaustive: never = shape;
      throw new Error(`Unknown shape: ${_exhaustive}`);
    }
  }
}
```

---

## 9. Common TypeScript Traps in React Native

### Trap 1: Trusting `any` from JSON responses

```tsx
// Wrong — destroys type safety
const data = await response.json() as any;
setUser(data.user);

// Correct — validate shape
const raw: unknown = await response.json();
// Use Zod or manual type guard to validate
const user = UserSchema.parse(raw); // Zod throws if shape is wrong
```

### Trap 2: Optional chaining that hides bugs

```tsx
// May hide undefined navigation params
const name = route.params?.user?.name ?? '';

// Better — assert params exist if the screen always requires them
// TypeScript should have caught this at the navigate() call site
const {user} = route.params; // Should never be undefined if navigation is typed
```

### Trap 3: `as` casting without validation

```tsx
// Dangerous — forces a type that may not match at runtime
const product = data as Product;

// Better — use a type guard or Zod schema
```

### Trap 4: Missing `undefined` handling with optional params

```tsx
// route.params could be undefined if navigated to without params
type ProfileParams = {userId?: string};

function ProfileScreen({route}: ProfileScreenProps) {
  const userId = route.params?.userId;
  if (!userId) return <Text>No user</Text>;
  // TypeScript now knows userId is string
}
```

---

## 10. Zod for Runtime Validation (Bridge to TypeScript)

TypeScript only validates at compile time. Use Zod to validate runtime data from APIs:

```tsx
import {z} from 'zod';

const UserSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1),
  email: z.string().email(),
  role: z.enum(['admin', 'user']),
  createdAt: z.string().datetime(),
});

type User = z.infer<typeof UserSchema>; // TypeScript type derived from schema

async function fetchUser(id: string): Promise<User> {
  const res = await fetch(`/users/${id}`);
  const raw = await res.json();
  return UserSchema.parse(raw); // throws ZodError if shape is wrong
}
```

`z.infer<typeof Schema>` is the pattern — define the schema once, derive the TypeScript type from it. The schema is the single source of truth.

---

## 11. Revision Notes

- Type navigation params end-to-end: `RootStackParamList` → `NativeStackScreenProps` → `useNavigation<>()` → `useRoute<>()`
- Discriminated unions are the clean pattern for async state, API envelopes, and component variants
- Extend native props with `&` — never redefine what TypeScript already knows
- Utility types: `Partial` for updates, `Pick/Omit` for API shapes, `Record` for maps
- `err` in catch blocks is `unknown` — always narrow before reading `.message`
- Use exhaustive switches with `never` to get TypeScript errors when new union members are added
- Never use `as` to cast without first validating the shape at runtime
- Use Zod for runtime validation — derive the TypeScript type from the schema with `z.infer`
- `strict: true` in tsconfig is non-negotiable for production codebases
