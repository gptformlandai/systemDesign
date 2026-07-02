# React Native LLD & Machine Coding Design Patterns — Gold Sheet

> Track Module - Group 5: Scenario Practice
> Level: senior / MAANG | Mode: design complete features from scratch in 30-45 minutes

---

## 1. How to Use This Sheet

Machine coding rounds ask you to build a working feature in 30-45 minutes. The evaluation criteria:
- Does it work?
- Is the code clean and organized?
- Are edge cases handled?
- Is the data flow clear?
- Can you explain your decisions?

Each design in this sheet follows the structure:
1. Problem statement
2. Component/hook breakdown
3. Data types
4. Complete implementation
5. Edge cases handled
6. What to explain to the interviewer

---

## 2. Design 1: Offline-Capable Shopping Cart

**Problem**: Design a shopping cart that works offline. Items added offline must sync when the user comes back online.

### Component Breakdown

```text
CartScreen
  ├── CartItemList (FlatList of CartItem components)
  ├── CartSummary (total, item count)
  ├── CheckoutButton (disabled when offline or cart empty)
  └── OfflineBanner (shown when not connected)

Hooks:
  useCart() — CRUD operations + persistence
  useNetworkStatus() — online/offline detection
  useOfflineQueue() — queue mutations while offline, sync when online
```

### Data Types

```tsx
type CartItem = {
  id: string;
  productId: string;
  name: string;
  price: number;
  quantity: number;
  imageUrl: string;
};

type Cart = {
  items: CartItem[];
  lastSyncedAt: number | null;
};

type QueuedMutation =
  | {type: 'ADD'; item: CartItem}
  | {type: 'REMOVE'; itemId: string}
  | {type: 'UPDATE_QTY'; itemId: string; quantity: number};
```

### useCart Implementation

```tsx
import AsyncStorage from '@react-native-async-storage/async-storage';

const CART_KEY = '@cart';

function useCart() {
  const [cart, setCart] = useState<Cart>({items: [], lastSyncedAt: null});
  const [loading, setLoading] = useState(true);
  const {isOnline} = useNetworkStatus();
  const {enqueue, queue} = useOfflineQueue();

  // Load persisted cart on mount
  useEffect(() => {
    AsyncStorage.getItem(CART_KEY)
      .then(stored => {
        if (stored) setCart(JSON.parse(stored));
      })
      .finally(() => setLoading(false));
  }, []);

  // Persist cart on every change
  useEffect(() => {
    AsyncStorage.setItem(CART_KEY, JSON.stringify(cart));
  }, [cart]);

  // Sync offline queue when coming back online
  useEffect(() => {
    if (isOnline && queue.length > 0) {
      syncQueue(queue);
    }
  }, [isOnline, queue]);

  const addItem = useCallback((product: Product) => {
    const cartItem: CartItem = {
      id: `${product.id}-${Date.now()}`,
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
      imageUrl: product.imageUrl,
    };

    setCart(prev => {
      const existing = prev.items.find(i => i.productId === product.id);
      if (existing) {
        return {
          ...prev,
          items: prev.items.map(i =>
            i.productId === product.id
              ? {...i, quantity: i.quantity + 1}
              : i,
          ),
        };
      }
      return {...prev, items: [...prev.items, cartItem]};
    });

    if (!isOnline) {
      enqueue({type: 'ADD', item: cartItem});
    } else {
      // Fire and forget — cart is already updated locally
      api.post('/cart/items', {productId: product.id}).catch(() => {
        enqueue({type: 'ADD', item: cartItem}); // failed — queue for retry
      });
    }
  }, [isOnline, enqueue]);

  const removeItem = useCallback((itemId: string) => {
    setCart(prev => ({...prev, items: prev.items.filter(i => i.id !== itemId)}));
    if (!isOnline) enqueue({type: 'REMOVE', itemId});
    else api.delete(`/cart/items/${itemId}`).catch(() => enqueue({type: 'REMOVE', itemId}));
  }, [isOnline, enqueue]);

  const total = cart.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

  return {cart, loading, total, itemCount, addItem, removeItem};
}
```

### CartScreen

```tsx
function CartScreen() {
  const {cart, loading, total, itemCount, removeItem} = useCart();
  const {isOnline} = useNetworkStatus();

  if (loading) return <ActivityIndicator />;

  return (
    <SafeAreaView style={{flex: 1}}>
      {!isOnline && (
        <View style={styles.offlineBanner}>
          <Text style={styles.offlineText}>You are offline — changes will sync when connected</Text>
        </View>
      )}
      <FlatList
        data={cart.items}
        keyExtractor={item => item.id}
        renderItem={({item}) => (
          <CartItemRow item={item} onRemove={() => removeItem(item.id)} />
        )}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text>Your cart is empty</Text>
          </View>
        }
      />
      <View style={styles.summary}>
        <Text style={styles.total}>Total: ${total.toFixed(2)} ({itemCount} items)</Text>
        <Pressable
          style={[styles.checkoutBtn, (!isOnline || itemCount === 0) && styles.disabled]}
          disabled={!isOnline || itemCount === 0}
          onPress={handleCheckout}>
          <Text style={styles.checkoutText}>
            {!isOnline ? 'Offline — cannot checkout' : 'Checkout'}
          </Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}
```

**Edge cases to mention**: duplicate item → increment quantity; offline mutation queue; checkout disabled when offline; empty state; loading state.

---

## 3. Design 2: Real-Time Chat Screen

**Problem**: Design a chat screen that shows messages, sends new messages, and receives real-time updates via WebSocket.

### Component Breakdown

```text
ChatScreen
  ├── MessageList (inverted FlatList for chat order)
  ├── TypingIndicator (when other user is typing)
  ├── MessageInputBar (text input + send button)
  └── ConnectionStatusBar (when WebSocket disconnects)

Hooks:
  useChatMessages(conversationId) — message list + WebSocket subscription
  useMessageInput() — draft text + send action
```

### Data Types

```tsx
type Message = {
  id: string;
  conversationId: string;
  senderId: string;
  text: string;
  sentAt: number; // unix timestamp
  status: 'sending' | 'sent' | 'failed';
};

type TypingEvent = {
  userId: string;
  conversationId: string;
};
```

### useChatMessages Implementation

```tsx
function useChatMessages(conversationId: string) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isTyping, setIsTyping] = useState(false);
  const [wsStatus, setWsStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const wsRef = useRef<WebSocket | null>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Load initial messages
  useEffect(() => {
    fetchMessages(conversationId).then(setMessages).catch(console.error);
  }, [conversationId]);

  // WebSocket connection
  useEffect(() => {
    const ws = new WebSocket(`wss://api.example.com/chat/${conversationId}`);
    wsRef.current = ws;

    ws.onopen = () => setWsStatus('connected');
    ws.onclose = () => setWsStatus('disconnected');
    ws.onerror = () => setWsStatus('disconnected');

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'message') {
        setMessages(prev => {
          // Deduplicate — server may echo our own sent messages
          if (prev.some(m => m.id === data.message.id)) return prev;
          return [data.message, ...prev]; // prepend — list is inverted
        });
      } else if (data.type === 'typing_start') {
        setIsTyping(true);
        // Auto-clear typing indicator after 3 seconds
        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
        typingTimeoutRef.current = setTimeout(() => setIsTyping(false), 3000);
      } else if (data.type === 'typing_stop') {
        setIsTyping(false);
      }
    };

    return () => {
      ws.close();
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    };
  }, [conversationId]);

  const sendMessage = useCallback(async (text: string) => {
    const tempId = `temp-${Date.now()}`;
    const optimisticMessage: Message = {
      id: tempId,
      conversationId,
      senderId: currentUser.id,
      text,
      sentAt: Date.now(),
      status: 'sending',
    };

    // Optimistic — add immediately with 'sending' status
    setMessages(prev => [optimisticMessage, ...prev]);

    try {
      const saved = await api.post<Message>('/messages', {conversationId, text});
      // Replace temp message with server-confirmed message
      setMessages(prev => prev.map(m => m.id === tempId ? {...saved, status: 'sent'} : m));
    } catch {
      setMessages(prev => prev.map(m => m.id === tempId ? {...m, status: 'failed'} : m));
    }
  }, [conversationId]);

  return {messages, isTyping, wsStatus, sendMessage};
}
```

### ChatScreen

```tsx
function ChatScreen({route}: ChatScreenProps) {
  const {conversationId} = route.params;
  const {messages, isTyping, wsStatus, sendMessage} = useChatMessages(conversationId);
  const [draft, setDraft] = useState('');
  const listRef = useRef<FlatList<Message>>(null);

  const handleSend = useCallback(() => {
    if (!draft.trim()) return;
    sendMessage(draft.trim());
    setDraft('');
  }, [draft, sendMessage]);

  return (
    <SafeAreaView style={{flex: 1}} edges={['bottom']}>
      {wsStatus === 'disconnected' && (
        <View style={styles.disconnectedBanner}>
          <Text>Reconnecting...</Text>
        </View>
      )}
      <FlatList
        ref={listRef}
        data={messages}
        inverted               // newest messages at bottom
        keyExtractor={m => m.id}
        renderItem={({item}) => (
          <MessageBubble
            message={item}
            isOwn={item.senderId === currentUser.id}
            onRetry={item.status === 'failed' ? () => sendMessage(item.text) : undefined}
          />
        )}
        ListHeaderComponent={isTyping ? <TypingIndicator /> : null}
      />
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
        <View style={styles.inputBar}>
          <TextInput
            style={styles.input}
            value={draft}
            onChangeText={setDraft}
            placeholder="Type a message..."
            multiline
            maxLength={2000}
          />
          <Pressable
            style={[styles.sendBtn, !draft.trim() && styles.sendBtnDisabled]}
            onPress={handleSend}
            disabled={!draft.trim()}>
            <Text>Send</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
```

**Edge cases to mention**: WebSocket disconnect/reconnect, deduplication of server-echoed messages, typing indicator timeout, optimistic message with failed state + retry, inverted FlatList for chat order, keyboard avoiding for input bar.

---

## 4. Design 3: Photo Gallery with Infinite Scroll and Caching

**Problem**: Design a photo gallery that loads lazily, supports infinite scroll, and caches images for offline viewing.

```tsx
type Photo = {
  id: string;
  uri: string;
  width: number;
  height: number;
  caption: string;
};

function usePhotoGallery(albumId: string) {
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const loadPhotos = useCallback(async (afterCursor: string | null, reset = false) => {
    if (loading) return;
    setLoading(true);

    try {
      const {items, nextCursor} = await fetchPhotos(albumId, {after: afterCursor, limit: 20});
      setPhotos(prev => reset ? items : [...prev, ...items]);
      setCursor(nextCursor);
      setHasMore(nextCursor !== null);
    } catch (err) {
      console.error('Failed to load photos:', err);
    } finally {
      setLoading(false);
    }
  }, [albumId, loading]);

  useEffect(() => { loadPhotos(null, true); }, [albumId]);

  const loadMore = useCallback(() => {
    if (!loading && hasMore) loadPhotos(cursor);
  }, [loading, hasMore, cursor, loadPhotos]);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    await loadPhotos(null, true);
    setRefreshing(false);
  }, [loadPhotos]);

  return {photos, loading, hasMore, refreshing, loadMore, refresh};
}

function PhotoGallery({albumId}: {albumId: string}) {
  const {photos, loading, refreshing, loadMore, refresh} = usePhotoGallery(albumId);
  const numColumns = 3;
  const itemSize = Dimensions.get('window').width / numColumns;

  const renderPhoto = useCallback(({item}: {item: Photo}) => (
    <Pressable
      style={{width: itemSize, height: itemSize}}
      onPress={() => navigation.navigate('PhotoDetail', {photoId: item.id})}>
      <Image
        source={{uri: item.uri}}
        style={{width: itemSize, height: itemSize}}
        resizeMode="cover"
      />
    </Pressable>
  ), [itemSize]);

  return (
    <FlatList
      data={photos}
      numColumns={numColumns}
      keyExtractor={p => p.id}
      renderItem={renderPhoto}
      onEndReached={loadMore}
      onEndReachedThreshold={0.3}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}
      ListFooterComponent={loading ? <ActivityIndicator /> : null}
      getItemLayout={(_, index) => ({
        length: itemSize,
        offset: itemSize * Math.floor(index / numColumns),
        index,
      })}
      initialNumToRender={12}
      maxToRenderPerBatch={12}
      windowSize={5}
    />
  );
}
```

**Edge cases to mention**: `getItemLayout` for performance (fixed-size grid), `numColumns` calculation from screen width, refresh control, footer spinner, `windowSize` tuning for memory.

---

## 5. Design 4: Feature Flag System

**Problem**: Design a feature flag system that fetches flags from a server, caches them locally, and makes them available throughout the app with zero performance impact on components.

```tsx
type FeatureFlags = {
  newCheckoutFlow: boolean;
  darkModeEnabled: boolean;
  maxCartItems: number;
  promotionBanner: {enabled: boolean; message: string};
};

const defaultFlags: FeatureFlags = {
  newCheckoutFlow: false,
  darkModeEnabled: false,
  maxCartItems: 50,
  promotionBanner: {enabled: false, message: ''},
};

const FlagContext = React.createContext<FeatureFlags>(defaultFlags);

export function FeatureFlagProvider({children}: {children: React.ReactNode}) {
  const [flags, setFlags] = useState<FeatureFlags>(defaultFlags);

  useEffect(() => {
    // Load cached flags immediately for zero-latency usage
    AsyncStorage.getItem('@feature_flags').then(cached => {
      if (cached) setFlags(JSON.parse(cached));
    });

    // Fetch fresh flags in background
    fetch('/feature-flags')
      .then(r => r.json())
      .then((freshFlags: FeatureFlags) => {
        setFlags(freshFlags);
        AsyncStorage.setItem('@feature_flags', JSON.stringify(freshFlags));
      })
      .catch(console.error);
  }, []);

  return <FlagContext.Provider value={flags}>{children}</FlagContext.Provider>;
}

export function useFeatureFlag<K extends keyof FeatureFlags>(flag: K): FeatureFlags[K] {
  const flags = useContext(FlagContext);
  return flags[flag];
}

// Usage
function CheckoutButton() {
  const newCheckout = useFeatureFlag('newCheckoutFlow');
  return newCheckout ? <NewCheckoutButton /> : <LegacyCheckoutButton />;
}
```

**Edge cases to mention**: stale cache shown immediately (no loading state for flags), background refresh, typed flags with TypeScript generics, default values for all flags so app never crashes on undefined.

---

## 6. Interview Communication Pattern

For any machine coding round, structure your answer before writing code:

```text
Step 1 — Clarify (2 minutes)
  "Before I start coding, let me clarify:
   - What devices and network conditions should I handle?
   - Is real-time data required or is polling acceptable?
   - Are there any performance constraints (number of items)?
   - Should I use a specific state library or plain React hooks?"

Step 2 — Design (3 minutes, verbal)
  "My approach: I'll separate the screen into [components].
   I'll put the business logic in [hooks] so it is testable.
   The data flow is: [explain].
   I'll handle these edge cases: [list]."

Step 3 — Code (25 minutes)
  Start with types → hooks → component → edge cases

Step 4 — Review (5 minutes)
  Walk through edge cases you handled.
  Mention what you would add with more time:
  "With more time, I'd add unit tests for the hooks,
   skeleton loading screens, and error retry telemetry."
```

---

## 7. Revision Notes

- Machine coding = types first → hook → component → edge cases
- Always separate business logic into hooks — screens should contain zero logic
- Offline patterns: optimistic local update + queue for sync + rollback on failure
- Real-time: WebSocket in useEffect with full cleanup + reconnect logic
- Inverted FlatList for chat (newest at bottom with reversed data)
- Feature flags: cache first → background refresh → context for zero re-render overhead
- Always handle: loading state, error state, empty state, offline state, optimistic state
- `getItemLayout` on FlatList is mandatory for performance when item height is known
- useCallback on all functions passed to FlatList renderItem to prevent unnecessary row re-renders
- Typing indicator: reset timeout on each typing event — clear on explicit stop or after 3 seconds
