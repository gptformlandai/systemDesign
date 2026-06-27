# React Native Web Developer Bridge — Gold Sheet

> Track File #4 of 37 · Group 1: Starter Path
> Audience: web/React developer entering mobile | Level: beginner to interview-ready
> Mode: map every web assumption to its React Native reality before touching any code

---

## 1. Why This File Exists First

Every React Native mistake made by a web developer comes from a wrong assumption carried over from the browser. This sheet maps every major web concept to its React Native equivalent, difference, or absence. Read this before anything else if you have built web apps.

The core rule:

```text
React Native does not run in a browser.
There is no DOM, no CSS cascade, no window object, no localStorage,
no service workers, no HTML, and no browser layout engine.

React Native runs JavaScript that talks to native iOS and Android UI toolkits
through the React Native renderer.
```

---

## 2. The Rendering Model — The Most Important Difference

### Web

```text
React component
  -> virtual DOM (JavaScript objects)
  -> real DOM (browser elements like <div>, <p>, <a>)
  -> browser layout engine (CSS box model)
  -> GPU pixels on screen
```

### React Native

```text
React component
  -> React Native renderer (shadow tree)
  -> native views (UIView on iOS, android.view.View on Android)
  -> platform layout engine (Yoga / Flexbox)
  -> GPU pixels on screen
```

Key insight:
There is no HTML. There is no browser. `<View>` is not a `<div>`. `<Text>` is not a `<p>`. They are instructions that create native platform controls. This is why React Native apps look and feel native — because they are.

Interview answer:
```text
React Native does not render HTML. The React components compile through the React
Native renderer into native views. On iOS, a View becomes a UIView. On Android it
becomes an android.view.View. The layout is computed by Yoga, a cross-platform
Flexbox engine. So a React Native app behaves exactly like a native iOS or Android
app because it IS a native app with React driving the component tree.
```

---

## 3. Complete Component Mapping: Web → React Native

| Web / React Web | React Native | Notes |
|---|---|---|
| `<div>` | `<View>` | No semantic meaning, Flexbox container |
| `<span>` | `<Text>` | All text MUST be inside `<Text>` — no inline text in Views |
| `<p>` | `<Text>` | Same as span — Text for all text |
| `<h1>` - `<h6>` | `<Text style={styles.heading}>` | No heading tags — you style it |
| `<button>` | `<Pressable>` or `<TouchableOpacity>` | Pressable is the modern choice |
| `<a>` | `<Pressable>` + `Linking.openURL()` | For external links use Linking |
| `<input type="text">` | `<TextInput>` | Has its own keyboard handling |
| `<input type="checkbox">` | Custom Pressable + state | No built-in checkbox |
| `<select>` | `<Picker>` (community) or modal | No native HTML select |
| `<img>` | `<Image>` | Requires explicit width/height |
| `<ul>/<li>` | `<FlatList>` or `<View>` mapped | FlatList for large lists |
| `<table>` | Custom `<View>` grid | No table element |
| `<form>` | `<View>` + state | No form element or form events |
| `<ScrollView>` | `<ScrollView>` | Close equivalent — but not the DOM scrollbar |
| `<Fragment>` | `<>` or `<React.Fragment>` | Same |
| `<Modal>` | `<Modal>` | Exists in React Native core |

Trap:
Text placed directly inside a `<View>` without a `<Text>` wrapper throws a runtime error on the device:
```text
Error: Text strings must be rendered within a <Text> component.
```

```tsx
// Wrong — will crash
<View>Hello world</View>

// Correct
<View>
  <Text>Hello world</Text>
</View>
```

---

## 4. Styling: CSS vs StyleSheet

### Web CSS

```css
.container {
  display: flex;
  flex-direction: row;
  background-color: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
```

### React Native StyleSheet

```tsx
import {StyleSheet} from 'react-native';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',      // default is 'column' in RN (not row like web)
    backgroundColor: '#fff',
    padding: 16,               // unitless number — always density-independent pixels
    borderRadius: 8,
    // No box-shadow — use elevation (Android) or shadow* props (iOS)
    elevation: 2,              // Android shadow
    shadowColor: '#000',       // iOS shadow
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
});
```

### Critical CSS Differences

| CSS (Web) | React Native | Difference |
|---|---|---|
| `flex-direction: row` | `flexDirection: 'column'` default | **RN defaults to column, not row** |
| `px`, `em`, `rem`, `%` units | Unitless number | Numbers are density-independent pixels (dp) |
| `display: flex` | Always flex | All Views are flex by default |
| `display: none` | `display: 'none'` | Works the same |
| `box-shadow` | `elevation` + `shadow*` | Split across platforms |
| CSS cascade | No cascade | Styles don't inherit to children (except inside Text) |
| `:hover`, `:focus` pseudo | None | No CSS pseudo-classes |
| `position: fixed` | None | No fixed positioning to viewport |
| `position: absolute` | `position: 'absolute'` | Relative to nearest parent, not viewport |
| `z-index` | `zIndex` | Works similarly |
| `border` shorthand | Individual props | `borderWidth`, `borderColor`, `borderStyle` |
| `overflow: hidden` | `overflow: 'hidden'` | Needed to clip child views |
| `width: 100%` | `flex: 1` or `width: '100%'` | Percentage strings work but flex is idiomatic |
| `min-height`, `max-width` | `minHeight`, `maxWidth` | Work the same |
| `object-fit: cover` | `resizeMode: 'cover'` on Image | Image-specific prop |
| CSS Grid | Not supported | Use nested Flexbox |
| CSS variables | Not supported natively | Use JS constants or theme context |
| Media queries | `Dimensions` API or `useWindowDimensions` | JS-based responsive logic |

The trap new developers fall into most:
```tsx
// Web developer writes this expecting horizontal layout
<View style={{flexDirection: 'row'}}>  // needed — RN defaults to column
```

---

## 5. Text Inheritance — No Cascade

In CSS, font properties inherit down the DOM tree. In React Native, styles do NOT cascade except inside nested `<Text>` components.

```tsx
// Web: this works — children inherit font size
<div style={{fontSize: 16}}>
  <p>This text is 16px</p>
</div>

// React Native: this does NOT work
<View style={{fontSize: 16}}> {/* fontSize on View is ignored */}
  <Text>This text has default size</Text>
</View>

// React Native: style each Text explicitly
<Text style={{fontSize: 16}}>This text is 16 dp</Text>
```

Exception — Text inside Text DOES inherit:
```tsx
<Text style={{color: 'blue', fontSize: 18}}>
  Normal text <Text style={{fontWeight: 'bold'}}>bold inherits blue color</Text>
</Text>
```

---

## 6. Layout: Flexbox in React Native vs CSS Flexbox

React Native uses Flexbox everywhere but with different defaults from CSS:

| Property | CSS Web default | React Native default |
|---|---|---|
| `flexDirection` | `row` | `column` |
| `alignContent` | `stretch` | `flex-start` |
| `flexShrink` | `1` | `0` |
| `position` | `static` | `relative` |

React Native does NOT support:
- `flex-wrap: wrap-reverse`
- `align-self: baseline`
- CSS Grid
- `float`
- `clear`
- `display: inline`
- `display: inline-flex`

Common pattern — full-screen layout:
```tsx
// Fill available space like flex: 1 in CSS but using RN idiom
<View style={{flex: 1}}>
  <View style={{flex: 0}}>   {/* header — natural height */}
    <Text>Header</Text>
  </View>
  <View style={{flex: 1}}>   {/* content — fills remaining space */}
    <ScrollView>...</ScrollView>
  </View>
</View>
```

---

## 7. Storage: localStorage vs AsyncStorage / SecureStore

| Web | React Native | Notes |
|---|---|---|
| `localStorage` | `@react-native-async-storage/async-storage` | Async — returns promises |
| `sessionStorage` | In-memory state (useState/Zustand) | No session storage concept |
| `IndexedDB` | SQLite via `expo-sqlite` or `react-native-sqlite-storage` | For relational local DB |
| `cookies` | No cookies | HTTP auth uses token headers |
| `document.cookie` | `expo-secure-store` or `react-native-keychain` | Secure device keychain |
| `Cache API` | MMKV (`react-native-mmkv`) for fast key-value | Much faster than AsyncStorage |

```tsx
// Web
localStorage.setItem('token', 'abc123');
const token = localStorage.getItem('token');

// React Native — always async
import AsyncStorage from '@react-native-async-storage/async-storage';
await AsyncStorage.setItem('token', 'abc123');
const token = await AsyncStorage.getItem('token'); // string | null

// React Native — sensitive data like tokens (uses device keychain)
import * as SecureStore from 'expo-secure-store';
await SecureStore.setItemAsync('authToken', 'abc123');
const token = await SecureStore.getItemAsync('authToken');
```

Security trap:
Never store auth tokens in AsyncStorage — it is not encrypted. Use SecureStore or react-native-keychain for anything sensitive.

---

## 8. Navigation: React Router vs React Navigation / Expo Router

| Web (React Router) | React Native | Notes |
|---|---|---|
| `<BrowserRouter>` | `<NavigationContainer>` | Root navigation wrapper |
| `<Route path="/home">` | `Stack.Screen name="Home"` | Declarative screen config |
| `<Link to="/profile">` | `navigation.navigate('Profile')` | Imperative navigation |
| `useNavigate()` | `useNavigation()` | Hook for navigation actions |
| `useParams()` | `useRoute().params` or typed params | Route param extraction |
| `useLocation()` | `useNavigationState()` | Navigation state access |
| Browser back button | Android hardware back / iOS swipe back | Handled by navigator |
| URL bar / deep link | Deep link scheme or universal link | `Linking` API |
| `window.history` | Navigation stack in state | Not exposed as URL |
| Hash routing | None | No hash concept |
| `<Redirect>` | `navigation.replace()` | Replace without back stack |

```tsx
// Web
import {useNavigate, useParams} from 'react-router-dom';
const navigate = useNavigate();
const {id} = useParams();
navigate('/product/123');

// React Native — React Navigation
import {useNavigation, useRoute} from '@react-navigation/native';
type ProductScreenRouteProp = RouteProp<RootStackParamList, 'Product'>;
const navigation = useNavigation();
const route = useRoute<ProductScreenRouteProp>();
const {id} = route.params;
navigation.navigate('Product', {id: '123'});
```

---

## 9. Events: DOM Events vs RN Gesture System

| Web | React Native | Notes |
|---|---|---|
| `onClick` | `onPress` (Pressable) | `onClick` does not exist in RN |
| `onMouseEnter` | None by default | No hover on touchscreens |
| `onChange` on input | `onChangeText` (TextInput) | Returns string directly, not event |
| `onSubmit` on form | Custom handler on submit button | No form element |
| `onScroll` | `onScroll` (ScrollView) | Same name, different event shape |
| `onKeyDown` | `onKeyPress` (TextInput) | Limited keyboard events |
| `addEventListener` | Not applicable | No DOM event listeners |
| Event propagation | Limited — `stopPropagation` via gesture | Touch propagation is different |

```tsx
// Web
<input onChange={(e) => setValue(e.target.value)} />
<button onClick={handleSubmit}>Submit</button>

// React Native
<TextInput onChangeText={(text) => setValue(text)} />
<Pressable onPress={handleSubmit}>
  <Text>Submit</Text>
</Pressable>
```

---

## 10. Networking: fetch in Web vs React Native

The `fetch` API exists in React Native (Hermes implements it). The differences are operational:

| Concern | Web | React Native |
|---|---|---|
| `fetch` API | Available | Available — same API |
| CORS | Browser enforces it | No CORS — RN calls APIs directly |
| HTTPS enforcement | Browser setting | iOS `Info.plist` ATS, Android network security config |
| Cookies in requests | Auto-sent by browser | Must manage manually with headers |
| AbortController | Available | Available |
| FormData | Available | Available with caveats for file uploads |
| WebSocket | `new WebSocket(url)` | `new WebSocket(url)` — same API |
| Request caching | Browser HTTP cache | No automatic cache — use react-query or SWR |
| Base URL | Relative paths work | Must use absolute URLs (no window.location) |

```tsx
// Works in both Web and React Native
const response = await fetch('https://api.example.com/users', {
  method: 'POST',
  headers: {'Content-Type': 'application/json', Authorization: `Bearer ${token}`},
  body: JSON.stringify(payload),
  signal: abortController.signal,
});

if (!response.ok) throw new Error(`HTTP ${response.status}`);
const data = await response.json();
```

No CORS trap:
If an API rejects from a browser due to CORS, it may still work in React Native because CORS is a browser security feature, not a server-to-server or native app restriction.

---

## 11. Animations: CSS Animations vs Reanimated

| Web | React Native | Notes |
|---|---|---|
| CSS `transition` | `Animated` or `Reanimated` | No CSS transitions |
| CSS `@keyframes` | `Animated.sequence` or worklets | No keyframe CSS |
| CSS `transform` | `style.transform` array | Same concept, different syntax |
| CSS `opacity` | `Animated.Value` or `useSharedValue` | Animated values, not CSS |
| `requestAnimationFrame` | Available | Can be used directly |
| CSS `will-change` | Run on UI thread via Reanimated | Architecture difference |

```tsx
// Web CSS
.button { transition: opacity 0.3s; }
.button:hover { opacity: 0.8; }

// React Native — Reanimated (runs on UI thread, 60 FPS)
import Animated, {useSharedValue, useAnimatedStyle, withTiming} from 'react-native-reanimated';

const opacity = useSharedValue(1);
const animatedStyle = useAnimatedStyle(() => ({opacity: opacity.value}));

// Trigger
opacity.value = withTiming(0.8, {duration: 300});

<Animated.View style={[styles.button, animatedStyle]}>
  <Text>Press me</Text>
</Animated.View>
```

---

## 12. Global Objects: window / document / navigator

| Web global | React Native | Notes |
|---|---|---|
| `window` | Does not exist | No window object |
| `document` | Does not exist | No DOM |
| `navigator.userAgent` | `Platform.OS` + `Platform.Version` | Use RN Platform API |
| `navigator.geolocation` | `expo-location` or `react-native-geolocation-service` | Requires permission |
| `navigator.clipboard` | `@react-native-clipboard/clipboard` | Community module |
| `window.location` | No equivalent | Navigation state in Navigator |
| `window.history` | Navigation stack | Managed by React Navigation |
| `window.open()` | `Linking.openURL()` | For external URLs |
| `window.innerWidth` | `Dimensions.get('window').width` | Requires listening for changes |
| `window.screen` | `Dimensions.get('screen')` | Physical screen dimensions |
| `window.alert()` | `Alert.alert()` | Native dialog, not browser alert |
| `window.confirm()` | `Alert.alert()` with buttons | Native confirm dialog |
| `window.scroll()` | `scrollRef.current.scrollTo()` | Via ref on ScrollView |
| `window.localStorage` | AsyncStorage | See storage section above |
| `window.sessionStorage` | In-memory state | No session concept |
| `window.matchMedia()` | `useWindowDimensions()` | Responsive breakpoints via JS |

```tsx
// Web
if (window.innerWidth < 768) { ... }

// React Native
import {useWindowDimensions} from 'react-native';
const {width} = useWindowDimensions();
if (width < 400) { ... }
```

---

## 13. Browser APIs That Simply Do Not Exist

These web APIs have no direct equivalent in React Native. You must use community modules or native modules:

| Web API | Status in RN | Alternative |
|---|---|---|
| Service Workers | None | Background fetch via `react-native-background-fetch` |
| Web Workers | None | Run heavy work in native module or Hermes worklet |
| `crypto.subtle` | Limited | `react-native-quick-crypto` |
| WebGL | None | `expo-gl` or Three.js with Expo |
| Canvas API | None | `react-native-canvas` or `react-native-svg` |
| `FileReader` | None | `react-native-fs` or `expo-file-system` |
| `Blob` | Partial | Some operations work, file uploads need workarounds |
| `history.pushState` | None | React Navigation manages stack |
| `document.title` | None | App name set in Info.plist/AndroidManifest |
| `<iframe>` | `<WebView>` | Renders a web page inside a native view |
| `performance.now()` | Available | Works in Hermes |
| `console.*` | Available | Works, shows in Metro logs |
| `setTimeout`/`setInterval` | Available | Work the same |
| `Promise` | Available | Full Promises work |
| `fetch` | Available | Works the same |

---

## 14. Debugging Tools: Browser DevTools vs React Native DevTools

| Web DevTools | React Native Equivalent | Notes |
|---|---|---|
| Elements panel (DOM) | React Native DevTools component tree | Shows component hierarchy |
| Console | Metro terminal + in-app LogBox | `console.log` appears in Metro |
| Network panel | React Native DevTools network | Or use Reactotron/Charles Proxy |
| Sources / breakpoints | React Native debugger or VS Code | Hermes debugger |
| Performance profiler | React Native DevTools Profiler | JS thread frame rate |
| Memory profiler | Hermes heap snapshot | Via React Native DevTools |
| `debugger` statement | Works | Pauses JS execution |
| React DevTools | Built into React Native DevTools | Same extension concept |
| Lighthouse | N/A | Use Xcode Instruments / Android Profiler for device metrics |

Opening DevTools:
- Shake device or press `Cmd+D` (iOS sim) / `Cmd+M` (Android emulator)
- Or `j` in the Metro terminal to open the JS debugger

---

## 15. Build System: npm build vs Metro + Hermes

| Web | React Native | Notes |
|---|---|---|
| Webpack / Vite bundler | Metro bundler | RN-specific JS bundler |
| `npm run build` → static files | Metro → JS bundle → native app | Very different output |
| Hot Module Replacement | Fast Refresh | Similar UX, different implementation |
| `node_modules` | Same | Same package system |
| `import` / `require` | Same | Module resolution via Metro |
| Tree shaking | Limited in Metro | Not as aggressive as webpack |
| Code splitting | Limited | Manual splitting via dynamic imports |
| Environment variables | `babel-plugin-transform-inline-env-vars` or `expo-constants` | No `process.env.REACT_APP_*` by default |
| `public/` folder | `assets/` folder | Static assets in app bundle |
| `.env` files | `.env` via `react-native-dotenv` or Expo | Different loading mechanism |
| V8 engine (Chrome) | Hermes (Meta's JS engine) | Optimized for mobile: faster startup, less memory |
| Source maps | Generated for release | Required for crash symbolication |

---

## 16. App Lifecycle vs Page Lifecycle

Web has no real app lifecycle. React Native has a full native app lifecycle:

```text
Web page:
  load -> user uses page -> user closes tab (page unloads)

React Native app:
  launch -> splash screen -> JS bundle loads -> first render
         -> foreground (active) -> user presses home button
         -> background (inactive) -> app may be suspended/killed by OS
         -> user returns -> foreground again
```

```tsx
import {AppState} from 'react-native';
import {useEffect, useRef} from 'react';

export function useAppStateChange(onChange: (state: string) => void) {
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    const sub = AppState.addEventListener('change', nextState => {
      if (appState.current !== nextState) {
        onChange(nextState);  // 'active', 'background', 'inactive'
        appState.current = nextState;
      }
    });
    return () => sub.remove();
  }, [onChange]);
}
```

This matters for:
- Pausing/resuming timers and WebSocket connections
- Re-fetching stale data when user returns to app
- Stopping camera/audio when app goes to background
- Clearing sensitive data from memory

---

## 17. Platform Differences: iOS vs Android

Unlike web (one browser target), React Native targets two very different platforms:

| Area | iOS | Android |
|---|---|---|
| Font rendering | San Francisco system font | Roboto system font |
| Back navigation | Swipe from left edge | Hardware back button or gesture |
| Status bar | Notch / Dynamic Island | Variable — depends on manufacturer |
| Safe area | `useSafeAreaInsets()` | `useSafeAreaInsets()` |
| Permissions | Permission dialog per use | Manifest + runtime dialog |
| Push notifications | APNs (Apple Push Notification service) | FCM (Firebase Cloud Messaging) |
| Keychain / secure storage | iOS Keychain | Android Keystore |
| Background execution | Very limited | More flexible |
| App signing | Certificates + provisioning profiles | Keystore file |
| App store review | Apple App Store review (slower) | Google Play review (faster) |
| Status bar color | Controlled via `expo-status-bar` | Controlled via `expo-status-bar` |

```tsx
import {Platform} from 'react-native';

const hitSlop = Platform.select({
  ios: {top: 10, bottom: 10, left: 10, right: 10},
  android: {top: 12, bottom: 12, left: 12, right: 12},
});

const headerHeight = Platform.OS === 'ios' ? 44 : 56;
```

---

## 18. Interview Traps for Web Developers Entering React Native

### Trap 1: Assuming CSS works
```tsx
// Wrong — CSS classes don't work
<View className="container flex-row">

// Correct — StyleSheet objects
<View style={styles.container}>
```

### Trap 2: Putting text outside Text
```tsx
// Crashes on device
<View>Hello</View>

// Correct
<View><Text>Hello</Text></View>
```

### Trap 3: Using onClick instead of onPress
```tsx
// Does nothing in React Native
<View onClick={handler}>

// Correct
<Pressable onPress={handler}>
```

### Trap 4: Expecting window/document to exist
```tsx
// Crashes — window is undefined
if (window.innerWidth > 400) { }

// Correct
const {width} = useWindowDimensions();
```

### Trap 5: Using localStorage synchronously
```tsx
// Does not exist
localStorage.setItem('key', 'value');

// Correct — always async
await AsyncStorage.setItem('key', value);
```

### Trap 6: Expecting flex-direction row by default
```tsx
// Web default — this would be horizontal
<div style={{display: 'flex'}}>

// React Native default is COLUMN — must explicitly set row
<View style={{flexDirection: 'row'}}>
```

### Trap 7: Forgetting SafeAreaView for notch/status bar
```tsx
// Content hides behind notch on iPhone
<View style={{flex: 1}}>

// Correct — use safe area
import {SafeAreaView} from 'react-native-safe-area-context';
<SafeAreaView style={{flex: 1}}>
```

---

## 19. Mental Model Summary

```text
Web Developer                   →    React Native
--------------------------------------------------------------
<div>                           →    <View>
<span>/<p>/<h1>                 →    <Text> (only text container)
<button>                        →    <Pressable>
<input>                         →    <TextInput>
<img>                           →    <Image> (needs explicit size)
<ul><li>                        →    <FlatList> (for long lists)
CSS class + cascade             →    StyleSheet.create (no cascade)
flex-direction: row (default)   →    flexDirection: 'column' (default)
px/em/rem units                 →    unitless number (density pixels)
localStorage                    →    AsyncStorage (async, unencrypted)
session storage                 →    useState / Zustand
secure token storage            →    SecureStore / Keychain
React Router                    →    React Navigation / Expo Router
onClick                         →    onPress
onChange (e.target.value)       →    onChangeText (receives string)
window                          →    Platform + Dimensions + AppState
DevTools Elements               →    React Native DevTools tree
CSS animations                  →    Reanimated (UI thread)
Webpack/Vite                    →    Metro
V8 / SpiderMonkey               →    Hermes
One platform (browser)          →    Two platforms (iOS + Android)
CORS                            →    Not applicable
```

---

## 20. Revision Notes

- RN renders native views, not HTML — this is the #1 thing to get right
- All text must be in `<Text>` — no exceptions
- `flexDirection` defaults to `column` — always explicit for row layouts
- No CSS cascade — style every component individually
- No `window`, no `document`, no `localStorage` — all replaced with RN APIs
- `onClick` → `onPress`, `onChange` → `onChangeText`
- Storage is async — `await` everything with AsyncStorage/SecureStore
- Tokens go in SecureStore, never AsyncStorage
- iOS and Android are two different platforms with real differences — write for both
- Metro is the bundler, Hermes is the JS engine, Yoga is the layout engine
