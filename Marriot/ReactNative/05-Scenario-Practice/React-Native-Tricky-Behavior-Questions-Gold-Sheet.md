# React Native Tricky Behavior & Output Questions — Gold Sheet

> Track Module - Group 5: Scenario Practice
> Level: intermediate to MAANG | Mode: answer from behavior prediction, not guesswork

---

## 1. How to Use This Sheet

These questions test whether you understand React Native mechanics deeply enough to predict behavior before running code. The same format appears in senior interviews — "What happens when...?" or "What does this code do?".

For each question:
1. Cover the answer section and reason through it
2. Check your reasoning against the answer
3. Mark: ✅ got it cold | ⚠️ partially right | ❌ wrong

---

## 2. State and Re-render Behavior

### Q1: What does this log?

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  const handlePress = () => {
    setCount(count + 1);
    setCount(count + 1);
    setCount(count + 1);
    console.log(count);
  };

  return <Pressable onPress={handlePress}><Text>{count}</Text></Pressable>;
}
```

**What logs on first press? What does the text show after?**

Answer:
```text
Logs: 0

count is captured by closure at 0 when handlePress was created.
All three setCount(count + 1) calls are setCount(0 + 1) = setCount(1).
React batches all setState calls in the same event handler.
After the event: count becomes 1 (not 3).

Text shows: 1

To get 3, use functional update: setCount(prev => prev + 1)
Each call then receives the latest prev value.
```

---

### Q2: Will this component re-render?

```tsx
function UserCard() {
  const [user, setUser] = useState({name: 'Alice', age: 30});

  const updateAge = () => {
    user.age = 31; // direct mutation
    setUser(user); // same reference
  };

  return (
    <Pressable onPress={updateAge}>
      <Text>{user.name}: {user.age}</Text>
    </Pressable>
  );
}
```

**Will the UI update after pressing?**

Answer:
```text
No. React uses Object.is(previousState, nextState) to decide whether to re-render.
user.age = 31 mutates the existing object. setUser(user) passes the same object reference.
Object.is(user, user) is true — same reference — React skips re-render.

The age IS 31 internally (mutation happened) but the screen stays at 30.
This is one of the most dangerous bugs because it looks like state management is broken,
but the real cause is mutation.

Fix: setUser(prev => ({...prev, age: 31}))
```

---

### Q3: What happens when this runs?

```tsx
function App() {
  const [items, setItems] = useState(['a', 'b', 'c']);

  const addItem = () => {
    setItems(items.push('d')); // what does Array.push return?
  };
}
```

**What does `items.push('d')` return? What does setItems receive?**

Answer:
```text
Array.push returns the new length of the array, not the array.
items.push('d') returns 4 (the new length).
setItems(4) sets items to the number 4.

Now items is the number 4 — any code trying to map over items will throw.
The original items array IS mutated (has 'd' added) but the state is now 4.

Double bug:
1. Mutation of state array
2. setItems receives a number instead of an array

Fix:
setItems(prev => [...prev, 'd'])
```

---

### Q4: What is the render count?

```tsx
const Context = React.createContext({count: 0, name: 'test'});

function DisplayCount() {
  const {count} = useContext(Context);
  return <Text>{count}</Text>;
}

function Parent() {
  const [count, setCount] = useState(0);
  const [name, setName] = useState('test');

  return (
    <Context.Provider value={{count, name}}>
      <DisplayCount />
      <Pressable onPress={() => setName('new name')}>
        <Text>Change Name</Text>
      </Pressable>
    </Context.Provider>
  );
}
```

**Does DisplayCount re-render when name changes?**

Answer:
```text
Yes — DisplayCount re-renders even though it only uses count.

Why: The context value is {count, name}. When name changes, Parent re-renders
and creates a new object {count: 0, name: 'new name'}. The context value is a
new object reference. React.useContext compares context values by reference.
New reference = all consumers re-render, even DisplayCount which only reads count.

Fix: Memoize the context value
const value = useMemo(() => ({count, name}), [count, name]);
// Now DisplayCount still re-renders when name changes because the memoized value
// IS a new reference when name changes.

Better fix: Split contexts
const CountContext = React.createContext(0);
const NameContext = React.createContext('test');
// DisplayCount consumes CountContext only — now isolated from name changes
```

---

## 3. useEffect Behavior

### Q5: How many times does the effect run?

```tsx
function Timer() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const id = setInterval(() => {
      setCount(count + 1);
      console.log('tick', count);
    }, 1000);
    return () => clearInterval(id);
  }, []); // empty deps

  return <Text>{count}</Text>;
}
```

**What does `count` log after 5 seconds?**

Answer:
```text
Logs: tick 0, tick 0, tick 0, tick 0, tick 0

The effect runs once (empty deps array). count is captured as 0 in the closure
at mount time. The interval fires every second with count always reading 0.
setCount(count + 1) = setCount(0 + 1) = setCount(1) every tick.
So count jumps to 1 on the first tick and then STAYS at 1 — setCount(1) is
called every second but nothing changes.

Screen shows: 1 forever

Fix: setCount(prev => prev + 1)
This uses functional update — does not need count in closure.
With this fix, count increments correctly each second.
```

---

### Q6: When does cleanup run?

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    console.log('effect run', count);
    return () => console.log('cleanup', count);
  }, [count]);
}
```

**What logs on mount and on first increment?**

Answer:
```text
On mount:        "effect run 0"
On first press:  "cleanup 0"   (cleanup from previous effect — with old count)
                 "effect run 1" (new effect — with new count)

React cleanup order:
1. Component renders with new state
2. Screen paints
3. Previous effect cleanup runs (with old captured values)
4. New effect runs (with new captured values)

This is why cleanup logs count=0 even after the state changes to 1.
```

---

### Q7: Memory leak or not?

```tsx
function UserAvatar({userId}: {userId: string}) {
  const [avatar, setAvatar] = useState<string | null>(null);

  useEffect(() => {
    fetchAvatar(userId).then(url => {
      setAvatar(url); // is this safe?
    });
  }, [userId]);

  return avatar ? <Image source={{uri: avatar}} /> : <View />;
}
```

**If the user navigates away before fetchAvatar resolves, what happens?**

Answer:
```text
Memory leak / React warning.

If the component unmounts while fetchAvatar is still in-flight, the .then
callback still runs after unmount. It calls setAvatar on an unmounted component.

Result: React logs a warning:
"Can't perform a React state update on an unmounted component."
And the state update is a no-op, but the promise callback still executes,
which is wasted work and technically a memory consideration.

Fix: use a cancelled flag or AbortController
useEffect(() => {
  let cancelled = false;
  fetchAvatar(userId).then(url => {
    if (!cancelled) setAvatar(url);
  });
  return () => { cancelled = true; };
}, [userId]);
```

---

## 4. FlatList and List Behavior

### Q8: Why is this FlatList slow?

```tsx
function ProductList({products}: {products: Product[]}) {
  return (
    <FlatList
      data={products}
      renderItem={({item}) => (
        <ProductCard
          product={item}
          onPress={() => navigate('Detail', {id: item.id})}
        />
      )}
    />
  );
}

const ProductCard = React.memo(({product, onPress}: ProductCardProps) => {
  return <Pressable onPress={onPress}><Text>{product.name}</Text></Pressable>;
});
```

**ProductCard is wrapped in React.memo — will it prevent re-renders?**

Answer:
```text
No — React.memo will NOT prevent re-renders here.

The problem: () => navigate('Detail', {id: item.id}) is an anonymous inline
function inside renderItem. It is a new function reference on every ProductList
render.

React.memo does a shallow comparison of props. product likely has the same
reference (if products array items are stable), but onPress is a new function
each time ProductList renders.

React.memo sees: prevOnPress !== nextOnPress → re-render.

Fix: use useCallback
const handlePress = useCallback((id: string) => {
  navigate('Detail', {id});
}, [navigate]);

// In renderItem:
<ProductCard product={item} onPress={() => handlePress(item.id)} />

// Still an inline arrow — better:
// Pass a stable handler per item using item ID as part of the key
// or use the pattern where ProductCard handles navigation internally
```

---

### Q9: What is the key trap here?

```tsx
function MessageList({messages}: {messages: Message[]}) {
  return (
    <FlatList
      data={messages}
      keyExtractor={(item, index) => String(index)}
      renderItem={({item}) => <MessageRow message={item} />}
    />
  );
}
```

**What problem does using `index` as key cause?**

Answer:
```text
Incorrect reconciliation when items are added to the beginning or middle.

Keys help React identify which items changed between renders.
Using index means:
  - Original: [A(key=0), B(key=1), C(key=2)]
  - After prepend D: [D(key=0), A(key=1), B(key=2), C(key=3)]

React sees key=0 changed from A to D — updates A's component with D's data.
React sees key=3 appeared — creates new component for C.
React does NOT see that A, B, C simply shifted — it re-renders all of them.

For chat apps with messages prepended: this causes items to flicker,
animations to reset, and unread state to be lost.

Fix: Use a stable unique ID
keyExtractor={item => item.id}  // message ID from server
```

---

## 5. TypeScript and Props Traps

### Q10: Is this TypeScript-safe?

```tsx
type ButtonProps = {
  label: string;
  onPress: () => void;
  disabled?: boolean;
};

function Button({label, onPress, disabled = false}: ButtonProps) {
  return (
    <Pressable onPress={onPress} disabled={disabled}>
      <Text>{label}</Text>
    </Pressable>
  );
}

// Usage
<Button label="Submit" onPress={undefined} />
```

**Does TypeScript catch this error?**

Answer:
```text
Yes — TypeScript catches this.

onPress is typed as () => void (non-optional). Passing undefined is a type error.

However: if TypeScript strict mode is NOT enabled (strict: false in tsconfig),
this might silently pass. With strict: true, it is caught.

Runtime consequence without TypeScript: Pressable's onPress is undefined.
Pressing the button calls undefined() — TypeError crash.

Fix: either make onPress optional:
  onPress?: () => void
or ensure callers always provide it (TypeScript enforces this with strict mode).
```

---

## 6. Navigation Traps

### Q11: What happens to unsaved form data?

```tsx
function EditProfileScreen() {
  const [name, setName] = useState('Alice');

  return (
    <View>
      <TextInput value={name} onChangeText={setName} />
      <Pressable onPress={() => navigation.navigate('Home')}>
        <Text>Go Home</Text>
      </Pressable>
    </View>
  );
}
```

**If the user navigates home and comes back, is `name` preserved?**

Answer:
```text
It depends on the navigation action.

navigate('Home') pushes Home onto the stack — EditProfile is NOT destroyed.
When user presses back, EditProfile is still mounted with name = latest value.
This is PRESERVED.

But: navigation.navigate('Home') when Home is already in the stack may go back
to the existing Home instance — and EditProfile IS popped (destroyed).

If the screen is fully unmounted, useState is reset to 'Alice' on re-mount.

Production fix for important form data: persist to AsyncStorage or Zustand
on every change so it survives navigation:
useEffect(() => { AsyncStorage.setItem('editName', name); }, [name]);
```

---

## 7. Async and Promise Traps

### Q12: What order do these log?

```tsx
useEffect(() => {
  console.log('1 - effect start');

  Promise.resolve().then(() => console.log('2 - microtask'));

  setTimeout(() => console.log('3 - macrotask'), 0);

  console.log('4 - effect end');
}, []);
```

**What is the log order?**

Answer:
```text
1 - effect start
4 - effect end
2 - microtask
3 - macrotask

JavaScript event loop:
  - Synchronous code runs first: 1, then 4
  - Microtask queue (Promise.resolve().then) runs before next macrotask: 2
  - Macrotask (setTimeout 0) runs last: 3

This matters in React Native for understanding when state updates triggered
inside async operations (Promises) take effect relative to synchronous code.
```

---

### Q13: What does this catch?

```tsx
async function loadUser() {
  try {
    const response = await fetch('/users/me');
    const user = await response.json();
    return user;
  } catch (err) {
    console.log('caught:', err);
  }
}
```

**Server returns HTTP 500. Does catch() run?**

Answer:
```text
No — catch() does NOT run for HTTP 500.

fetch() only rejects (throws) on network errors — no connection, DNS failure, timeout.
HTTP 4xx and 5xx responses DO resolve — fetch succeeded in delivering the request
and receiving a response. response.ok will be false.

Response.json() will also resolve for a 500 with a JSON body.

The function silently returns the parsed error body (or undefined if response.json
fails on a non-JSON 500 body).

Fix: Always check response.ok
const response = await fetch('/users/me');
if (!response.ok) throw new Error(`HTTP ${response.status}`);
const user = await response.json();
```

---

## 8. Performance and Rendering Traps

### Q14: Is this useMemo useful?

```tsx
function CartTotal({price, quantity}: {price: number; quantity: number}) {
  const total = useMemo(() => price * quantity, [price, quantity]);
  return <Text>${total.toFixed(2)}</Text>;
}
```

**Is useMemo providing a benefit here?**

Answer:
```text
No. This is a premature optimization anti-pattern.

price * quantity is O(1) — a single multiplication. The cost is negligible.
useMemo itself has overhead: storing the previous value, comparing dependencies,
and returning the cached value. For a trivial computation, the useMemo overhead
exceeds the computation cost.

useMemo is useful for:
  - Expensive computations (sorting 10,000 items, complex filtering)
  - Creating new array/object references that would trigger re-renders in children
  - Returning a stable reference for dependency arrays in other hooks

For simple arithmetic, just compute it:
const total = price * quantity;
```

---

### Q15: Why does this FlatList re-render all rows?

```tsx
function ProductList() {
  const [cart, setCart] = useState<string[]>([]);

  const addToCart = (id: string) => {
    setCart(prev => [...prev, id]);
  };

  return (
    <FlatList
      data={products}
      extraData={cart}  // passed to detect cart changes
      renderItem={({item}) => (
        <ProductRow
          product={item}
          inCart={cart.includes(item.id)}
          onAddToCart={addToCart}
        />
      )}
    />
  );
}
```

**When one item is added to cart, how many rows re-render?**

Answer:
```text
All rows re-render.

Reasons:
1. cart changes → ProductList re-renders → renderItem is a new function → all rows
2. onAddToCart is a new function reference on every ProductList render
3. ProductRow is not wrapped in React.memo

Even with React.memo on ProductRow:
  - inCart is stable for most rows (they are NOT in cart)
  - BUT onAddToCart is a new reference on every render → React.memo fails

Complete fix:
  1. Wrap ProductRow in React.memo
  2. useCallback on addToCart
  3. Pass only the product ID to the row, let the row compute inCart from context/store

Or use FlatList's built-in optimization: pass stable data, stable keyExtractor,
memoized renderItem, and React.memo rows.
```

---

## 9. Quick Reference — Key Rules to Internalize

```text
useState batching:     All setState in one event handler are batched — only one re-render
useState identity:     Object.is comparison — same reference = no re-render
Mutation trap:         Mutating state object and calling setState with same ref = no re-render
useEffect deps:        Empty array = once on mount; missing dep = stale closure bug
Cleanup timing:        Cleanup runs BEFORE next effect, with old captured values
fetch HTTP errors:     fetch does NOT throw on 4xx/5xx — always check response.ok
Array.push return:     Returns new length, not the array
React.memo and fns:    React.memo fails if any prop function is a new reference
index as key:          Causes incorrect reconciliation on insert/delete/reorder
Context re-render:     ALL consumers re-render on context value change, even unused fields
useMemo cost:          Only useful for expensive computations or stable reference needs
setInterval + state:   Empty deps captures initial value — use functional update or include dep
Promise vs HTTP err:   Network error = throws; HTTP error = resolves with !ok response
```

---

## 10. Revision Notes

- Batch setState: three `setCount(count + 1)` calls in one handler all use the same captured `count` → only increment once; use `prev =>` form to increment properly
- Object mutation + setState: changing a field on an existing object and passing it to setState does nothing because the reference is the same
- `useEffect` closure: deps array controls what values the closure captures — missing dep = stale value
- `fetch` resolves on HTTP errors — always check `response.ok` before reading body
- `Array.push` returns the new length — never pass it to setState
- Context consumers all re-render on any value change — split contexts or memoize values
- `React.memo` requires stable prop references — inline functions break it; use `useCallback`
- `keyExtractor` must use stable unique IDs — index as key causes reconciliation bugs
- `useMemo` on simple arithmetic is overhead, not optimization
- Cleanup in `useEffect` runs with the old captured values before the new effect runs
