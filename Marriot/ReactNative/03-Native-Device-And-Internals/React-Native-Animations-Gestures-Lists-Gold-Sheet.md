# React Native Animations, Gestures, And Lists - Gold Sheet

> Track Module - Group 3: Native Device And Internals
> Level: smooth interaction and list performance

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| FlatList | Very high | Most mobile apps render large lists |
| Virtualization | Very high | Memory and scroll performance |
| `keyExtractor` | Very high | Prevents row identity bugs |
| `getItemLayout` | High | Speeds fixed-height lists |
| Pull-to-refresh/pagination | High | Common app behavior |
| Gestures | High | Swipe, drag, sheet, carousel |
| Animated/Reanimated | High | Smooth UI and interview depth |
| JS thread vs UI thread animation | Very high | Senior performance signal |

MAANG signal:
You know when UI jank comes from rendering too much, doing too much JS work, or using the wrong animation driver.

---

## 2. Mental Model

Mobile interaction must feel immediate.

```text
Touch event -> gesture recognition -> state/animation update -> frame rendered
```

At 60 FPS, the app has about 16.67 ms per frame. Anything blocking JS or UI thread can cause visible stutter.

---

## 3. Lists: ScrollView vs FlatList

| Component | Use For | Avoid For |
|---|---|---|
| `ScrollView` | Small static content | Hundreds/thousands of rows |
| `FlatList` | Large flat lists | Highly custom nested layouts without care |
| `SectionList` | Grouped/sectioned lists | Simple lists |
| FlashList/other optimized libs | Very large/complex lists | Unmeasured premature dependency |

Rule:
If list size can grow, start with `FlatList`.

---

## 4. FlatList Template

```tsx
type Product = {
  id: string;
  name: string;
  priceCents: number;
};

const ProductRow = memo(function ProductRow({
  product,
  onPress,
}: {
  product: Product;
  onPress: (id: string) => void;
}) {
  return (
    <Pressable onPress={() => onPress(product.id)} style={styles.row}>
      <Text style={styles.name}>{product.name}</Text>
      <Text style={styles.price}>${(product.priceCents / 100).toFixed(2)}</Text>
    </Pressable>
  );
});

export function ProductList({products}: {products: Product[]}) {
  const handlePress = useCallback((id: string) => {
    navigateToProduct(id);
  }, []);

  const renderItem = useCallback(
    ({item}: {item: Product}) => (
      <ProductRow product={item} onPress={handlePress} />
    ),
    [handlePress],
  );

  return (
    <FlatList
      data={products}
      keyExtractor={item => item.id}
      renderItem={renderItem}
      initialNumToRender={12}
      windowSize={7}
      removeClippedSubviews
    />
  );
}
```

Production notes:
- Tune list props with measurement.
- Avoid heavy row render logic.
- Use stable keys.
- Keep row state outside row components if it must survive virtualization.
- Optimize image loading.

---

## 5. `getItemLayout`

Use when row height is fixed.

```tsx
const ROW_HEIGHT = 72;

<FlatList
  data={items}
  getItemLayout={(_, index) => ({
    length: ROW_HEIGHT,
    offset: ROW_HEIGHT * index,
    index,
  })}
  renderItem={renderItem}
/>;
```

Why it helps:
React Native can jump/scroll without measuring every previous item.

Trap:
Do not use wrong fixed heights for variable-height rows. It causes broken scroll positions.

---

## 6. Infinite Scroll And Refresh

```tsx
<FlatList
  data={items}
  renderItem={renderItem}
  keyExtractor={item => item.id}
  refreshing={isRefreshing}
  onRefresh={refresh}
  onEndReached={loadNextPage}
  onEndReachedThreshold={0.5}
  ListFooterComponent={isFetchingNextPage ? <Spinner /> : null}
/>;
```

Guardrails:
- Prevent duplicate page fetches.
- Handle end-of-list.
- Deduplicate items by ID.
- Preserve scroll position when prepending chat messages.
- Show retry for page failures.

---

## 7. Gestures

Gesture-heavy apps often use:
- React Native Gesture Handler.
- Reanimated.
- native stack transitions.
- bottom sheet libraries.

Why:
Gesture recognition and animations often need to run close to the UI thread to stay smooth.

Examples:
- swipe to delete
- bottom sheet
- draggable card
- pinch zoom
- carousel

Production concern:
Gesture interactions must not depend on slow JS work in the critical path.

---

## 8. Animations

Animation options:

| Tool | Best For |
|---|---|
| `Animated` | Basic animations, especially with native driver |
| `LayoutAnimation` | Simple layout transitions |
| Reanimated | Gesture-driven, interruptible, UI-thread-heavy animations |
| Native stack animations | Screen transitions |

Key distinction:
- JS-driven animation can freeze if JS thread is busy.
- Native/UI-thread animation can keep running even when JS is doing work.

Interview answer:

```text
For simple opacity/transform animations, I can use Animated with native driver.
For complex gesture-driven animations, I prefer Reanimated because work can run
on the UI side and avoid JS-thread stalls. I still profile because UI-thread work
can also become expensive.
```

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| `ScrollView` for a feed | Renders too much | Use `FlatList` |
| Index as key | Breaks identity on insert/reorder | Use stable item IDs |
| Row owns critical state | Virtualization can unmount it | Store state externally |
| Heavy inline row render | JS thread stalls | Memoize and split rows |
| Wrong `getItemLayout` | Scroll bugs | Use only for fixed size |
| JS-driven complex gestures | Jank under JS load | Use native/UI-thread-capable tools |

---

## 10. Strong Interview Answer

Question:
A React Native feed scrolls poorly. How do you debug it?

Strong answer:

```text
I first reproduce in a release build and check JS and UI FPS. Then I inspect the
list: is it ScrollView instead of FlatList, are keys stable, are rows memoized,
are images too large, is renderItem doing heavy work, and are pagination calls
duplicating? If row heights are fixed I consider getItemLayout. I also check for
console logging, expensive selectors, and re-renders caused by unstable props.
If animations or gestures are involved, I verify whether they depend on the JS
thread and move suitable work to native/UI-thread-capable animation tools.
```

---

## 11. Revision Notes

- One-line summary: Smooth mobile UI depends on virtualization and thread-aware interactions.
- Three keywords: FlatList, gestures, UI thread.
- One interview trap: Index keys are dangerous in changing lists.
- One memory trick: Feed equals virtualized rows plus stable identity plus cheap render.

---

## 12. Reanimated 3 Deep Dive — Worklets and Shared Values

### Why Reanimated Exists

React Native's built-in `Animated` API drives animations from the JavaScript thread by default. Any JS-thread activity (API calls, heavy renders, slow component) will cause JS-driven animations to stutter because the JS thread is occupied.

Reanimated 3 solves this by running animation logic on the UI thread using "worklets":

```text
Animated API (JS thread):
  animation state lives in JS
  every frame: JS calculates new value → serializes → posts to UI thread → UI renders
  if JS is busy → animation misses frames

Reanimated 3 (UI thread):
  animation logic compiled to C++ as "worklet"
  worklet runs directly on UI thread, no JS involvement per frame
  JS thread busy? Animation still runs at 60/120 FPS
```

### Shared Values

`useSharedValue` creates a value that exists in both JS and UI thread memory. Updates to it trigger UI-thread re-evaluation of animated styles — not React re-renders:

```tsx
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSpring,
  withRepeat,
  withSequence,
  Easing,
} from 'react-native-reanimated';

function PulsingButton() {
  const scale = useSharedValue(1);  // shared value — lives on UI thread
  
  // This function becomes a worklet — runs on UI thread, not JS thread
  const animatedStyle = useAnimatedStyle(() => {
    'worklet';  // explicit worklet marker (required in functions passed to Reanimated)
    return {
      transform: [{scale: scale.value}],
    };
  });
  
  const handlePress = () => {
    // withSpring runs entirely on UI thread
    scale.value = withSpring(0.9, {}, () => {
      'worklet';
      scale.value = withSpring(1);
    });
  };
  
  return (
    <Animated.View style={[styles.button, animatedStyle]}>
      <Pressable onPress={handlePress}>
        <Text>Press Me</Text>
      </Pressable>
    </Animated.View>
  );
}
```

### Common Animation Patterns

```tsx
// 1. Timing animation (linear or eased movement)
const opacity = useSharedValue(0);
opacity.value = withTiming(1, {duration: 300, easing: Easing.ease});

// 2. Spring animation (physics-based bounce)
const translateY = useSharedValue(-100);
translateY.value = withSpring(0, {damping: 15, stiffness: 100});

// 3. Sequence of animations
const x = useSharedValue(0);
x.value = withSequence(
  withTiming(100, {duration: 200}),
  withTiming(-100, {duration: 200}),
  withTiming(0, {duration: 200}),
);

// 4. Repeat animation
const rotation = useSharedValue(0);
rotation.value = withRepeat(withTiming(360, {duration: 1000}), -1);  // -1 = infinite

const spinStyle = useAnimatedStyle(() => ({
  transform: [{rotate: `${rotation.value}deg`}],
}));

// 5. Interpolation — map one range to another
import {interpolate, Extrapolation} from 'react-native-reanimated';
const scrollY = useSharedValue(0);

const headerStyle = useAnimatedStyle(() => {
  'worklet';
  const headerHeight = interpolate(
    scrollY.value,
    [0, 100],           // input range
    [80, 50],           // output range
    Extrapolation.CLAMP, // do not extrapolate beyond output range
  );
  return {height: headerHeight};
});
```

---

## 13. Gesture Handler — Production Patterns

### Gesture Basics

```tsx
import {
  GestureDetector,
  Gesture,
  GestureHandlerRootView,
} from 'react-native-gesture-handler';
import Animated, {useSharedValue, useAnimatedStyle} from 'react-native-reanimated';

// MUST wrap your entire app
function App() {
  return (
    <GestureHandlerRootView style={{flex: 1}}>
      <AppNavigator />
    </GestureHandlerRootView>
  );
}
```

### Draggable Card

```tsx
function DraggableCard() {
  const offsetX = useSharedValue(0);
  const offsetY = useSharedValue(0);
  const savedX = useSharedValue(0);
  const savedY = useSharedValue(0);

  const panGesture = Gesture.Pan()
    .onUpdate((event) => {
      'worklet';
      offsetX.value = savedX.value + event.translationX;
      offsetY.value = savedY.value + event.translationY;
    })
    .onEnd(() => {
      'worklet';
      savedX.value = offsetX.value;
      savedY.value = offsetY.value;
    });

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      {translateX: offsetX.value},
      {translateY: offsetY.value},
    ],
  }));

  return (
    <GestureDetector gesture={panGesture}>
      <Animated.View style={[styles.card, animatedStyle]} />
    </GestureDetector>
  );
}
```

### Swipe-to-Dismiss Pattern

```tsx
import {runOnJS} from 'react-native-reanimated';

function SwipeToDismiss({onDismiss, children}: {onDismiss: () => void; children: React.ReactNode}) {
  const translateX = useSharedValue(0);
  const SCREEN_WIDTH = Dimensions.get('window').width;
  const SWIPE_THRESHOLD = SCREEN_WIDTH * 0.4;

  const panGesture = Gesture.Pan()
    .onUpdate((event) => {
      'worklet';
      translateX.value = event.translationX;
    })
    .onEnd((event) => {
      'worklet';
      if (Math.abs(event.translationX) > SWIPE_THRESHOLD) {
        // runOnJS bridges UI thread back to JS thread for React state updates
        const direction = event.translationX > 0 ? SCREEN_WIDTH : -SCREEN_WIDTH;
        translateX.value = withTiming(direction, {duration: 200}, () => {
          runOnJS(onDismiss)();  // call JS function from UI thread worklet
        });
      } else {
        // Not far enough — spring back
        translateX.value = withSpring(0);
      }
    });

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{translateX: translateX.value}],
  }));

  return (
    <GestureDetector gesture={panGesture}>
      <Animated.View style={animatedStyle}>{children}</Animated.View>
    </GestureDetector>
  );
}
```

---

## 14. FlashList vs FlatList — The Modern Choice

### FlashList (by Shopify)

FlashList is a drop-in FlatList replacement that is significantly faster for most use cases:

```tsx
// FlatList — standard
<FlatList
  data={products}
  renderItem={({item}) => <ProductCard product={item} />}
  keyExtractor={item => item.id}
  getItemLayout={(_, index) => ({length: 80, offset: 80 * index, index})}
/>

// FlashList — drop-in replacement
import {FlashList} from '@shopify/flash-list';
<FlashList
  data={products}
  renderItem={({item}) => <ProductCard product={item} />}
  keyExtractor={item => item.id}
  estimatedItemSize={80}   // replaces getItemLayout — estimates help FlashList pre-allocate
/>
```

### FlatList vs FlashList Trade-offs

| Dimension | FlatList | FlashList |
|---|---|---|
| View recycling | Creates and destroys views | Recycles view instances (like RecyclerView) |
| Memory usage | Higher for large lists | 5-10× lower for large lists |
| Scroll performance | Good with optimization | Excellent out of the box |
| Setup | `getItemLayout` for best perf | `estimatedItemSize` sufficient |
| Drop-in compatibility | — | 90%+ API compatibility |
| Variable height rows | Handles automatically | Needs `overrideItemLayout` callback |
| Part of RN core | Yes — no extra dep | Requires `@shopify/flash-list` |

### When to Use What

```text
FlatList:
  - Simple lists with < 100 items
  - No extra dependency preferred
  - Highly variable row heights (FlatList handles this better out of the box)

FlashList:
  - Long lists (200+ items)
  - Infinite scroll feeds
  - Performance is critical (product listing, social feed, search results)
  - Lists that users scroll quickly
```

---

## 15. SectionList and FlatList Edge Cases

```tsx
// SectionList — for categorized lists with headers
<SectionList
  sections={[
    {title: 'Fruits', data: ['Apple', 'Banana']},
    {title: 'Vegetables', data: ['Carrot', 'Broccoli']},
  ]}
  renderItem={({item}) => <Text>{item}</Text>}
  renderSectionHeader={({section: {title}}) => (
    <Text style={styles.sectionHeader}>{title}</Text>
  )}
  keyExtractor={(item, index) => item + index}
  stickySectionHeadersEnabled={true}  // headers stick on iOS by default, Android needs this
/>

// Bi-directional scroll (chat list) — inverted FlatList
<FlatList
  data={messages}
  renderItem={renderMessage}
  inverted={true}               // newest messages at bottom, renders inverted
  keyExtractor={m => m.id}
  // For chat: prepend new messages (push to start of array)
  // inverted + reverse array order = natural chat experience
/>

// Horizontal FlatList (story tray)
<FlatList
  data={stories}
  renderItem={renderStoryAvatar}
  horizontal={true}
  showsHorizontalScrollIndicator={false}
  snapToInterval={STORY_WIDTH + 12}  // snap to each story
  decelerationRate="fast"
/>
```

---

## 16. Interview Answer Upgrade

**Q: How do you build a smooth, high-performance scrollable feed in React Native?**

```text
I'd use FlashList from Shopify for its view recycling, which avoids the mount/unmount
cost of FlatList for long lists. For each row component I'd apply React.memo with proper
keyExtractor returning stable unique IDs (never index). The renderItem would be wrapped
in useCallback to prevent new function references. I'd size images to thumbnail resolution
at the CDN level and use a caching image library like FastImage.

For animations within rows — like a like button press or swipe reveal — I'd use
Reanimated 3 with useSharedValue and useAnimatedStyle to keep animation work on the
UI thread, so a slow network call or re-render cannot interfere with gesture smoothness.

If the list has variable row heights and complex gestures, I'd also add
GestureHandlerRootView at the app root to ensure gesture handling is performed
natively rather than through the JS thread.

The final check is always a release build profile — I verify JS FPS and UI FPS
under realistic scroll speeds and data volumes before calling the list "done."
```

