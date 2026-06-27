# React Native Runnable Mini Labs — Gold Sheet

> Track File #26 of 37 · Group 7: Practice Upgrade
> Level: all levels | Mode: build it yourself — reading is not enough

---

## 1. How to Use This Sheet

Each lab gives you a task, a hint, and the full solution. Work the task yourself first — even if you only get part of it. The struggle is what builds memory. Time yourself: foundation labs in 5 minutes, intermediate in 15, senior in 30.

Run these in an Expo Snack (snack.expo.dev) or a local Expo project.

---

## 2. Foundation Labs (Target: 5 minutes each)

### Lab F1: Controlled TextInput with character counter

**Task**: Build a bio input. Max 160 characters. Show "X/160" below. Remaining characters turn red when below 20.

```tsx
// Starter
import {View, TextInput, Text, StyleSheet} from 'react-native';

export default function BioInput() {
  // Your code here
}
```

**Solution**:
```tsx
import {View, TextInput, Text, StyleSheet, useState} from 'react';
import {useState} from 'react';

const MAX = 160;

export default function BioInput() {
  const [bio, setBio] = useState('');
  const remaining = MAX - bio.length;

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        value={bio}
        onChangeText={setBio}
        maxLength={MAX}
        multiline
        placeholder="Tell us about yourself"
        textAlignVertical="top"
      />
      <Text style={[styles.counter, remaining < 20 && styles.warning]}>
        {bio.length}/{MAX}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {padding: 16},
  input: {borderWidth: 1, borderColor: '#ccc', borderRadius: 8, height: 100, padding: 12},
  counter: {textAlign: 'right', marginTop: 4, color: '#666'},
  warning: {color: '#e53e3e'},
});
```

**What this practices**: controlled input, maxLength, conditional styling, character count.

---

### Lab F2: Toggle list with checkboxes

**Task**: Show a grocery list. Each item can be checked/unchecked. Checked items show with a strikethrough. A counter shows "X of N completed".

```tsx
const ITEMS = ['Milk', 'Eggs', 'Bread', 'Butter', 'Coffee'];
```

**Solution**:
```tsx
import {View, Text, Pressable, StyleSheet, useState} from 'react';
import {useState} from 'react';

const ITEMS = ['Milk', 'Eggs', 'Bread', 'Butter', 'Coffee'];

export default function GroceryList() {
  const [checked, setChecked] = useState<Set<string>>(new Set());

  const toggle = (item: string) => {
    setChecked(prev => {
      const next = new Set(prev);
      next.has(item) ? next.delete(item) : next.add(item);
      return next;
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.counter}>{checked.size} of {ITEMS.length} done</Text>
      {ITEMS.map(item => (
        <Pressable key={item} onPress={() => toggle(item)} style={styles.row}>
          <View style={[styles.checkbox, checked.has(item) && styles.checked]}>
            {checked.has(item) && <Text style={styles.tick}>✓</Text>}
          </View>
          <Text style={[styles.label, checked.has(item) && styles.done]}>{item}</Text>
        </Pressable>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {padding: 16},
  counter: {fontSize: 14, color: '#666', marginBottom: 12},
  row: {flexDirection: 'row', alignItems: 'center', paddingVertical: 12, gap: 12},
  checkbox: {width: 24, height: 24, borderWidth: 2, borderColor: '#007AFF', borderRadius: 4, justifyContent: 'center', alignItems: 'center'},
  checked: {backgroundColor: '#007AFF'},
  tick: {color: '#fff', fontWeight: 'bold'},
  label: {fontSize: 16},
  done: {textDecorationLine: 'line-through', color: '#999'},
});
```

---

### Lab F3: Fetch users from JSONPlaceholder

**Task**: Fetch `https://jsonplaceholder.typicode.com/users` on mount. Show loading spinner, then a list of names. Show error message if fetch fails.

**Solution**:
```tsx
import {View, Text, FlatList, ActivityIndicator, StyleSheet} from 'react-native';
import {useState, useEffect} from 'react';

type User = {id: number; name: string; email: string};

export default function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    fetch('https://jsonplaceholder.typicode.com/users')
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<User[]>;
      })
      .then(data => { if (!cancelled) setUsers(data); })
      .catch(err => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, []);

  if (loading) return <ActivityIndicator style={{flex: 1}} />;
  if (error) return <Text style={styles.error}>{error}</Text>;

  return (
    <FlatList
      data={users}
      keyExtractor={u => String(u.id)}
      renderItem={({item}) => (
        <View style={styles.row}>
          <Text style={styles.name}>{item.name}</Text>
          <Text style={styles.email}>{item.email}</Text>
        </View>
      )}
    />
  );
}

const styles = StyleSheet.create({
  error: {flex: 1, textAlign: 'center', marginTop: 50, color: 'red'},
  row: {padding: 16, borderBottomWidth: 1, borderColor: '#eee'},
  name: {fontWeight: 'bold', fontSize: 16},
  email: {color: '#666', marginTop: 4},
});
```

---

## 3. Intermediate Labs (Target: 15 minutes each)

### Lab I1: Search with debounce + loading state

**Task**: Text input that searches JSONPlaceholder posts (`https://jsonplaceholder.typicode.com/posts?title_like=QUERY`). Debounce the request by 500ms. Show loading spinner while fetching. Do not fire on empty query.

**Solution**:
```tsx
import {View, TextInput, FlatList, Text, ActivityIndicator, StyleSheet} from 'react-native';
import {useState, useEffect} from 'react';

type Post = {id: number; title: string};

function useDebounce<T>(value: T, ms: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return debounced;
}

export default function PostSearch() {
  const [query, setQuery] = useState('');
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(false);
  const debouncedQuery = useDebounce(query, 500);

  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setPosts([]);
      return;
    }

    let cancelled = false;
    setLoading(true);

    fetch(`https://jsonplaceholder.typicode.com/posts?title_like=${encodeURIComponent(debouncedQuery)}`)
      .then(r => r.json())
      .then((data: Post[]) => { if (!cancelled) setPosts(data.slice(0, 20)); })
      .catch(console.error)
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [debouncedQuery]);

  return (
    <View style={{flex: 1, padding: 16}}>
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder="Search posts..."
        style={styles.input}
        clearButtonMode="while-editing"
      />
      {loading && <ActivityIndicator style={{marginTop: 20}} />}
      {!loading && posts.length === 0 && debouncedQuery.trim() && (
        <Text style={styles.empty}>No posts found</Text>
      )}
      <FlatList
        data={posts}
        keyExtractor={p => String(p.id)}
        renderItem={({item}) => <Text style={styles.row}>{item.title}</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  input: {borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 12, marginBottom: 8},
  empty: {textAlign: 'center', marginTop: 20, color: '#999'},
  row: {paddingVertical: 12, borderBottomWidth: 1, borderColor: '#eee'},
});
```

---

### Lab I2: useReducer form with validation

**Task**: Build a login form (email + password) using `useReducer`. Show inline validation on blur. Disable submit while loading. Show success message on submit.

**Solution**:
```tsx
import {View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet} from 'react-native';
import {useReducer} from 'react';

type Field = {value: string; error: string | null; touched: boolean};
type State = {email: Field; password: Field; submitting: boolean; success: boolean};

const initial: State = {
  email: {value: '', error: null, touched: false},
  password: {value: '', error: null, touched: false},
  submitting: false,
  success: false,
};

type Action =
  | {type: 'SET_EMAIL'; value: string}
  | {type: 'BLUR_EMAIL'}
  | {type: 'SET_PASSWORD'; value: string}
  | {type: 'BLUR_PASSWORD'}
  | {type: 'SUBMITTING'}
  | {type: 'SUCCESS'};

function validate(field: 'email' | 'password', value: string): string | null {
  if (field === 'email') {
    if (!value) return 'Email is required';
    if (!value.includes('@')) return 'Invalid email';
  }
  if (field === 'password') {
    if (!value) return 'Password is required';
    if (value.length < 8) return 'Min 8 characters';
  }
  return null;
}

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'SET_EMAIL':
      return {...state, email: {...state.email, value: action.value}};
    case 'BLUR_EMAIL':
      return {...state, email: {...state.email, touched: true, error: validate('email', state.email.value)}};
    case 'SET_PASSWORD':
      return {...state, password: {...state.password, value: action.value}};
    case 'BLUR_PASSWORD':
      return {...state, password: {...state.password, touched: true, error: validate('password', state.password.value)}};
    case 'SUBMITTING':
      return {...state, submitting: true};
    case 'SUCCESS':
      return {...state, submitting: false, success: true};
    default:
      return state;
  }
}

export default function LoginForm() {
  const [state, dispatch] = useReducer(reducer, initial);
  const canSubmit = !state.submitting &&
    !validate('email', state.email.value) &&
    !validate('password', state.password.value);

  const handleSubmit = async () => {
    dispatch({type: 'SUBMITTING'});
    await new Promise(r => setTimeout(r, 1500)); // simulate API call
    dispatch({type: 'SUCCESS'});
  };

  if (state.success) return <Text style={styles.success}>Login successful!</Text>;

  return (
    <View style={styles.container}>
      <TextInput
        style={[styles.input, state.email.error && styles.inputError]}
        value={state.email.value}
        onChangeText={v => dispatch({type: 'SET_EMAIL', value: v})}
        onBlur={() => dispatch({type: 'BLUR_EMAIL'})}
        placeholder="Email"
        autoCapitalize="none"
        keyboardType="email-address"
      />
      {state.email.error && <Text style={styles.error}>{state.email.error}</Text>}

      <TextInput
        style={[styles.input, state.password.error && styles.inputError]}
        value={state.password.value}
        onChangeText={v => dispatch({type: 'SET_PASSWORD', value: v})}
        onBlur={() => dispatch({type: 'BLUR_PASSWORD'})}
        placeholder="Password"
        secureTextEntry
      />
      {state.password.error && <Text style={styles.error}>{state.password.error}</Text>}

      <Pressable
        style={[styles.button, !canSubmit && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={!canSubmit}>
        {state.submitting ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Login</Text>}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {padding: 24},
  input: {borderWidth: 1, borderColor: '#ddd', borderRadius: 8, padding: 14, marginBottom: 4, fontSize: 16},
  inputError: {borderColor: '#e53e3e'},
  error: {color: '#e53e3e', fontSize: 13, marginBottom: 12},
  button: {backgroundColor: '#007AFF', padding: 16, borderRadius: 8, alignItems: 'center', marginTop: 8},
  buttonDisabled: {opacity: 0.5},
  buttonText: {color: '#fff', fontWeight: '600', fontSize: 16},
  success: {padding: 24, fontSize: 18, textAlign: 'center'},
});
```

---

## 4. Senior Labs (Target: 30 minutes each)

### Lab S1: Offline-aware task list with sync queue

**Task**: Build a task list. Tasks can be added and completed. All changes must persist to AsyncStorage. Additions made while offline must sync to a fake API (`fetch('/tasks', {method: 'POST'})`) when the app comes back online. Show an offline banner.

**Key requirements**:
- Add task: instant local update + persist + API call (or queue if offline)
- Complete task: toggle locally + persist
- Offline banner with pending count
- On reconnect: drain the queue

**Approach to take**:
```text
State: tasks array (AsyncStorage) + syncQueue (AsyncStorage)
Hooks: useNetworkStatus + useEffect watching isOnline + queue drain
AsyncStorage keys: '@tasks' and '@syncQueue'
Queue item shape: {id: string, type: 'ADD' | 'COMPLETE', payload: any}
```

Implement this yourself. The reference solution is 120 lines. You should produce it in 30 minutes.

---

### Lab S2: Custom hook — usePaginatedSearch

**Task**: Build `usePaginatedSearch(searchFn, pageSize)` that:
- Takes an async search function and page size
- Returns: `{results, loading, error, query, setQuery, loadMore, hasMore, refresh}`
- Debounces the query by 400ms
- Resets results when query changes
- Appends results on loadMore
- Can be refreshed (reset + refetch page 1)

**Approach**:
```text
Internal state: results[], page, hasMore, loading, error, query (raw), debouncedQuery
useEffect on debouncedQuery change: reset results, fetch page 1
loadMore: fetch next page, append
refresh: reset + fetch page 1 again
```

Implement without looking at the Custom Hooks sheet. This tests whether you can build composable hook infrastructure from scratch.

---

## 5. Self-Grading for Labs

After each lab, ask yourself:

```text
✅ Did it work on the first try?
✅ Did I handle the empty state?
✅ Did I handle the error state?
✅ Did I clean up all subscriptions / timers / requests?
✅ Is TypeScript fully typed (no `any`)?
✅ Are functions stable (useCallback where needed)?
✅ Could I explain every line to an interviewer?
```

One ❌ = revisit the relevant concept sheet and redo the lab.

---

## 6. Revision Notes

- Foundation labs: controlled input, list rendering, basic fetch with loading/error
- Intermediate labs: debounce hook, useReducer for form state with validation
- Senior labs: offline sync queue, custom paginated search hook
- The goal is to build these from memory in the given time — not to reference the solution
- Each lab is a mini version of something you will encounter in a real interview
- Struggling is expected and valuable — the struggle is where learning happens
