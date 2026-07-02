# React Native Platform-Specific Patterns — Gold Sheet

> Track Module - Group 3: Native Device & Internals
> Level: intermediate to senior | Mode: write code that handles iOS and Android differences correctly

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| `Platform.OS` and `Platform.select` | Very high | Daily usage — every senior RN dev must know this |
| Platform-specific file extensions | High | Clean way to split large platform divergences |
| iOS vs Android navigation differences | High | Back behavior, header styles, transitions |
| Safe area handling for notch/status bar | Very high | Broken UI without this |
| Status bar — color and style | High | Looks unprofessional without correct handling |
| Keyboard behavior differences | High | iOS and Android keyboard handling differs |
| Typography and font differences | Medium | System fonts differ per platform |
| Shadow and elevation differences | High | CSS shadow does not work — must use platform split |

---

## 2. Platform Detection — Three Approaches

### Approach 1: Platform.OS inline check

```tsx
import {Platform} from 'react-native';

// Simple conditional
const isIOS = Platform.OS === 'ios';
const isAndroid = Platform.OS === 'android';

const headerHeight = Platform.OS === 'ios' ? 44 : 56;
```

### Approach 2: Platform.select — the idiomatic way

`Platform.select` picks the right value based on the current OS. More readable than ternary chains when you have multiple keys:

```tsx
const styles = StyleSheet.create({
  header: {
    height: Platform.select({
      ios: 44,
      android: 56,
      default: 50,  // fallback for web, Windows, macOS
    }),
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 2},
        shadowOpacity: 0.15,
        shadowRadius: 4,
      },
      android: {
        elevation: 4,
      },
    }),
  },
});
```

```tsx
// Platform.select with component choice
const BackIcon = Platform.select({
  ios: () => <ChevronLeftIcon />,
  android: () => <ArrowLeftIcon />,
})!;
```

### Approach 3: Platform-specific file extensions

For large divergences where the component logic itself differs, use separate files:

```text
components/
  DatePicker.ios.tsx       ← iOS uses native date picker wheel
  DatePicker.android.tsx   ← Android uses material calendar picker
  DatePicker.tsx           ← Optional: shared types and default export

The importer writes:
  import DatePicker from './DatePicker';
Metro resolves to the correct platform file automatically.
```

```tsx
// DatePicker.ios.tsx
import DateTimePicker from '@react-native-community/datetimepicker';

export function DatePicker({value, onChange}: DatePickerProps) {
  return (
    <DateTimePicker
      value={value}
      mode="date"
      display="spinner"  // iOS wheel spinner
      onChange={(_, date) => date && onChange(date)}
    />
  );
}

// DatePicker.android.tsx
import DateTimePicker from '@react-native-community/datetimepicker';

export function DatePicker({value, onChange}: DatePickerProps) {
  return (
    <DateTimePicker
      value={value}
      mode="date"
      display="default"  // Android material dialog
      onChange={(_, date) => date && onChange(date)}
    />
  );
}
```

Rule: Use inline `Platform.select` for small differences (padding, shadow, height). Use `.ios.tsx`/`.android.tsx` files for component logic or API surface that differs significantly.

---

## 3. Safe Area — Handling Notch, Dynamic Island, Status Bar

Without safe area handling, content renders behind the notch, status bar, or home indicator:

```bash
npx expo install react-native-safe-area-context
```

```tsx
// App.tsx — wrap root in SafeAreaProvider
import {SafeAreaProvider} from 'react-native-safe-area-context';

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>...</NavigationContainer>
    </SafeAreaProvider>
  );
}
```

```tsx
// Using SafeAreaView on individual screens
import {SafeAreaView} from 'react-native-safe-area-context';

function HomeScreen() {
  return (
    <SafeAreaView style={{flex: 1}} edges={['top', 'bottom']}>
      <Text>Content is safe from notch and home indicator</Text>
    </SafeAreaView>
  );
}
```

```tsx
// Using insets directly for custom layouts
import {useSafeAreaInsets} from 'react-native-safe-area-context';

function CustomHeader() {
  const insets = useSafeAreaInsets();
  return (
    <View style={{
      paddingTop: insets.top + 8,  // status bar height + visual padding
      paddingLeft: insets.left,
      paddingRight: insets.right,
      backgroundColor: '#fff',
    }}>
      <Text style={{fontSize: 20, fontWeight: 'bold'}}>My App</Text>
    </View>
  );
}
```

Edges parameter:
```tsx
// Only apply top and bottom safe area — not sides (most phones have no side notch)
<SafeAreaView edges={['top', 'bottom']}>

// Only bottom — useful when you have a custom header that already handles top inset
<SafeAreaView edges={['bottom']}>
```

---

## 4. Status Bar

```tsx
import {StatusBar} from 'expo-status-bar';

// Light content (white icons) for dark backgrounds
<StatusBar style="light" />

// Dark content (dark icons) for light backgrounds
<StatusBar style="dark" />

// Transparent status bar — content renders behind it
<StatusBar style="light" translucent backgroundColor="transparent" />

// Per-screen status bar — React Navigation recommended pattern
function ProductScreen() {
  return (
    <>
      <StatusBar style="dark" />
      <SafeAreaView>...</SafeAreaView>
    </>
  );
}
```

Platform differences:
- iOS: status bar content color is set per-screen — background is always transparent
- Android: status bar background color can be set; translucent mode requires `backgroundColor="transparent"`

---

## 5. Keyboard Behavior

iOS and Android handle keyboards differently by default:

```tsx
import {KeyboardAvoidingView, Platform} from 'react-native';

// The behavior prop must differ by platform
function LoginScreen() {
  return (
    <KeyboardAvoidingView
      style={{flex: 1}}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      <View style={styles.content}>
        <TextInput placeholder="Email" />
        <TextInput placeholder="Password" secureTextEntry />
        <Pressable style={styles.button}><Text>Login</Text></Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}
```

`behavior` values:
- `'padding'`: adds padding to the bottom of the container — works well on iOS
- `'height'`: reduces the height of the container — sometimes needed on Android
- `'position'`: moves the content up absolutely — advanced use case

Better alternative for complex forms:
```tsx
import {KeyboardAwareScrollView} from 'react-native-keyboard-aware-scroll-view';
// or
import {KeyboardAwareScrollView} from 'react-native-keyboard-controller';

function SignupScreen() {
  return (
    <KeyboardAwareScrollView
      enableOnAndroid={true}
      extraScrollHeight={20}>
      {/* Long form content */}
    </KeyboardAwareScrollView>
  );
}
```

---

## 6. Shadow and Elevation

CSS `box-shadow` does not exist in React Native. Platform-specific shadow:

```tsx
const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    // iOS shadow
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: {width: 0, height: 2},
        shadowOpacity: 0.12,
        shadowRadius: 8,
      },
      android: {
        elevation: 4,  // Android material elevation — creates shadow
      },
    }),
  },
});
```

Important:
- On Android, `elevation` also affects z-order stacking (higher elevation appears on top)
- On iOS, `shadowColor`, `shadowOffset`, `shadowOpacity`, `shadowRadius` together define the shadow
- The `overflow: 'hidden'` on a parent clips shadow on iOS — avoid it if shadow is needed

---

## 7. Navigation Header and Back Behavior

| Behavior | iOS | Android |
|---|---|---|
| Back navigation | Swipe from left edge | Hardware back button + optional gesture |
| Default header title position | Center | Left-aligned (material design) |
| Header back icon | `<` arrow with previous screen name | `←` arrow, no label |
| Modal presentation | Slide up from bottom | Same or slide from right |

```tsx
// React Navigation — customize per platform
<Stack.Screen
  name="ProductDetail"
  options={{
    title: 'Product Details',
    headerBackTitle: Platform.OS === 'ios' ? 'Back' : undefined, // iOS only
    headerTitleAlign: Platform.OS === 'ios' ? 'center' : 'left',
    animation: Platform.OS === 'ios' ? 'default' : 'slide_from_right',
  }}
/>
```

Android hardware back button handling:
```tsx
import {BackHandler} from 'react-native';

useEffect(() => {
  const handler = BackHandler.addEventListener('hardwareBackPress', () => {
    if (isModalOpen) {
      closeModal();
      return true; // consumed — prevents default back behavior
    }
    return false; // not consumed — default back behavior (navigate back)
  });
  return () => handler.remove();
}, [isModalOpen]);
```

---

## 8. Typography and System Fonts

React Native uses the system font by default. System fonts differ:
- iOS: San Francisco (SF Pro)
- Android: Roboto

```tsx
const styles = StyleSheet.create({
  // Uses system font automatically
  body: {
    fontSize: 16,
    lineHeight: 24,
    color: '#1a1a1a',
  },
  // Use font family with Platform.select for system fonts
  monospace: {
    fontFamily: Platform.select({
      ios: 'Courier New',
      android: 'monospace',
    }),
    fontSize: 14,
  },
});
```

Custom fonts with Expo:
```tsx
// 1. Place fonts in assets/fonts/
// 2. Load with expo-font
import {useFonts} from 'expo-font';
import {SplashScreen} from 'expo-router';

export function RootLayout() {
  const [loaded] = useFonts({
    'Inter-Regular': require('../assets/fonts/Inter-Regular.ttf'),
    'Inter-Bold': require('../assets/fonts/Inter-Bold.ttf'),
  });

  useEffect(() => {
    if (loaded) SplashScreen.hideAsync();
  }, [loaded]);

  if (!loaded) return null;
  return <Slot />;
}
```

---

## 9. Haptic Feedback — iOS Taptic Engine

iOS has rich haptic feedback. Android vibrator API is simpler:

```tsx
import * as Haptics from 'expo-haptics';

// Light tap — button press feedback
const handlePress = async () => {
  if (Platform.OS === 'ios') {
    await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  }
  doAction();
};

// Success confirmation
await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);

// Error — failure feedback
await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
```

---

## 10. Biometric Authentication (FaceID / TouchID / Fingerprint)

```tsx
import * as LocalAuthentication from 'expo-local-authentication';

async function authenticateWithBiometric(): Promise<boolean> {
  // Check if hardware is available
  const hasHardware = await LocalAuthentication.hasHardwareAsync();
  if (!hasHardware) return false;

  // Check if biometrics are enrolled
  const isEnrolled = await LocalAuthentication.isEnrolledAsync();
  if (!isEnrolled) {
    Alert.alert(
      'No Biometrics',
      'Please set up Face ID or Touch ID in your device settings.',
    );
    return false;
  }

  // Available types
  const types = await LocalAuthentication.supportedAuthenticationTypesAsync();
  const isFaceID = types.includes(LocalAuthentication.AuthenticationType.FACIAL_RECOGNITION);

  // Prompt biometric dialog
  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: isFaceID ? 'Authenticate with Face ID' : 'Authenticate with Touch ID',
    fallbackLabel: 'Use Passcode',
    cancelLabel: 'Cancel',
    disableDeviceFallback: false, // allow PIN/passcode fallback
  });

  return result.success;
}

// iOS: add NSFaceIDUsageDescription to Info.plist
// "NSFaceIDUsageDescription": "We use Face ID to secure your account"
```

---

## 11. Linking — Opening External URLs and Deep Links

```tsx
import {Linking} from 'react-native';

// Open URL in default browser
await Linking.openURL('https://example.com');

// Open another app (deep link)
await Linking.openURL('tel:+1234567890');   // phone call
await Linking.openURL('mailto:hello@example.com'); // email
await Linking.openURL('maps:?q=Coffee+Shop'); // maps (iOS)
await Linking.openURL('geo:37.7749,-122.4194'); // maps (Android)

// Check if URL can be opened before trying
const canOpen = await Linking.canOpenURL('instagram://user?username=example');
if (canOpen) {
  await Linking.openURL('instagram://user?username=example');
} else {
  await Linking.openURL('https://instagram.com/example'); // fallback to web
}

// Open app settings
await Linking.openSettings(); // iOS: Settings > Your App; Android: App Info
```

---

## 12. Platform Version Checking

```tsx
import {Platform} from 'react-native';

// iOS version check
if (Platform.OS === 'ios' && parseInt(Platform.Version as string, 10) >= 16) {
  // iOS 16+ specific behavior
}

// Android API level check
if (Platform.OS === 'android' && Platform.Version >= 33) {
  // Android 13+ (API 33) — needs POST_NOTIFICATIONS permission
}
```

---

## 13. Production Pattern — Platform Abstraction Layer

For large apps, abstract platform differences behind a consistent interface:

```tsx
// utils/platform.ts
import {Platform, Dimensions} from 'react-native';

export const isIOS = Platform.OS === 'ios';
export const isAndroid = Platform.OS === 'android';

export function shadow(elevation: number) {
  return Platform.select({
    ios: {
      shadowColor: '#000',
      shadowOffset: {width: 0, height: elevation / 2},
      shadowOpacity: 0.1 + elevation * 0.02,
      shadowRadius: elevation,
    },
    android: {elevation},
  });
}

export function hapticFeedback(style: 'light' | 'medium' | 'heavy' | 'success' | 'error') {
  if (!isIOS) return; // Android vibration handled separately
  // call expo-haptics
}
```

---

## 14. Revision Notes

- `Platform.OS` for inline checks; `Platform.select` for multiple platform values; `.ios.tsx`/`.android.tsx` for large divergences
- Always use `react-native-safe-area-context` — never hardcode status bar heights
- `KeyboardAvoidingView` behavior must be `'padding'` on iOS and `'height'` on Android
- Shadows: use `elevation` on Android, `shadow*` props on iOS — they are mutually exclusive
- Android hardware back button needs `BackHandler.addEventListener` for modals and drawers
- iOS requires `NSFaceIDUsageDescription` in `Info.plist` for biometric auth
- Check `Linking.canOpenURL` before attempting deep links to other apps
- Status bar style (light/dark) should be set per-screen
- System fonts differ — SF Pro on iOS, Roboto on Android — always load custom fonts explicitly
- Test platform-specific code on both platforms — iOS simulator and Android emulator
