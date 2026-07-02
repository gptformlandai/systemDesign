# React Native Push Notifications & Background Tasks — Gold Sheet

> Track Module - Group 3: Native Device & Internals
> Level: intermediate to senior | Mode: understand the full notification and background execution model

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| FCM vs APNs — what they are and why both needed | Very high | Foundation of all mobile push |
| Notification types: foreground, background, quit state | Very high | Behavior is different in each state |
| Permissions flow — iOS vs Android differences | High | Must request correctly or notifications are blocked |
| Deep link from notification tap | High | Critical for good notification UX |
| Background execution limits — iOS especially | High | Key mobile constraint MAANG interviewers probe |
| Local vs remote notifications | Medium | Knows the difference |
| Silent / data-only notifications | High | Background refresh without visible notification |
| Expo vs bare push setup | Medium | Setup context matters |

---

## 2. Mental Model — How Push Notifications Work

```text
Your Server
  → sends push payload to FCM (Android) or APNs (iOS)
  → FCM/APNs routes to the correct device using a device token
  → OS receives the notification
  → Delivers to app depending on app state (foreground/background/killed)
```

Why two services (FCM and APNs)?
- Apple controls all iOS push delivery through APNs. There is no alternative.
- Google controls Android push delivery through FCM.
- FCM can also wrap APNs — sending one payload to FCM with APNs credentials handles both.
- Expo Push Service is a hosted proxy that abstracts both FCM and APNs behind one unified API.

```text
Device Token:
  A unique token issued by APNs (iOS) or FCM (Android) that identifies
  a specific app installation on a specific device.
  
  When to refresh: after reinstall, app update, user signs out/in,
  OS clears tokens (rare). Always send the latest token to your server.
```

---

## 3. Expo Notifications Setup (Recommended for Most Apps)

```bash
npx expo install expo-notifications expo-device expo-constants
```

```tsx
// notifications/setup.ts
import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import Constants from 'expo-constants';
import {Platform} from 'react-native';

// Configure how notifications appear when app is in foreground
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,    // show banner
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

export async function registerForPushNotifications(): Promise<string | null> {
  // Push notifications only work on real devices
  if (!Device.isDevice) {
    console.warn('Push notifications require a physical device');
    return null;
  }

  // Android requires a notification channel
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'Default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
      lightColor: '#FF231F7C',
    });
  }

  // Request permission
  const {status: existingStatus} = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;

  if (existingStatus !== 'granted') {
    const {status} = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }

  if (finalStatus !== 'granted') {
    return null; // user denied — do not show a second prompt
  }

  // Get Expo push token (for Expo Push Service)
  const token = await Notifications.getExpoPushTokenAsync({
    projectId: Constants.expoConfig?.extra?.eas?.projectId,
  });

  return token.data; // send this to your backend
}
```

---

## 4. Notification Behavior by App State

This is the most important operational concept:

| App State | Notification Delivery | `onNotificationReceived` fires? | User sees notification? |
|---|---|---|---|
| Foreground (active) | Delivered to app directly | Yes | Only if `shouldShowAlert: true` in handler |
| Background | OS shows notification | No (not in JS) | Yes — system shows it |
| Killed (quit state) | OS shows notification | No | Yes — system shows it |

```tsx
// Handling all three scenarios

// 1. Foreground — notification arrives while app is open
const foregroundSubscription = Notifications.addNotificationReceivedListener(notification => {
  console.log('Foreground notification:', notification);
  // Show in-app toast/banner since system banner may not show
  showInAppNotification(notification.request.content);
});

// 2. Background/Quit tap — user taps the notification to open app
const responseSubscription = Notifications.addNotificationResponseReceivedListener(response => {
  const data = response.notification.request.content.data;
  // Deep link into the relevant screen
  if (data.type === 'order_update') {
    navigationRef.navigate('OrderDetail', {orderId: data.orderId});
  } else if (data.type === 'message') {
    navigationRef.navigate('Chat', {chatId: data.chatId});
  }
});

// 3. App launched from tapped notification (killed state)
useEffect(() => {
  Notifications.getLastNotificationResponseAsync().then(response => {
    if (response) {
      const data = response.notification.request.content.data;
      handleNotificationNavigation(data);
    }
  });
}, []);

// Cleanup on unmount
useEffect(() => {
  return () => {
    foregroundSubscription.remove();
    responseSubscription.remove();
  };
}, []);
```

---

## 5. Deep Linking from Notifications

Notification tap should navigate the user to the relevant content, not just open the app home screen:

```tsx
// navigation/ref.ts — navigation ref usable outside components
import {createNavigationContainerRef} from '@react-navigation/native';
import type {RootStackParamList} from './types';

export const navigationRef = createNavigationContainerRef<RootStackParamList>();

export function navigateFromNotification(data: NotificationData) {
  if (!navigationRef.isReady()) return;

  switch (data.type) {
    case 'order_update':
      navigationRef.navigate('OrderDetail', {orderId: data.orderId});
      break;
    case 'promotion':
      navigationRef.navigate('ProductDetail', {productId: data.productId});
      break;
    case 'message':
      navigationRef.navigate('ChatScreen', {conversationId: data.conversationId});
      break;
    default:
      navigationRef.navigate('Home');
  }
}
```

---

## 6. Local Notifications — Scheduling Without a Server

Use local notifications for reminders, timers, and offline scenarios:

```tsx
import * as Notifications from 'expo-notifications';

// Schedule a notification for a future time
async function scheduleReminder(task: Task) {
  const id = await Notifications.scheduleNotificationAsync({
    content: {
      title: 'Task Reminder',
      body: `Time to work on: ${task.title}`,
      data: {taskId: task.id, type: 'task_reminder'},
      sound: true,
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.DATE,
      date: task.dueDate,
    },
  });
  return id; // save this ID to cancel later
}

// Cancel a scheduled notification
await Notifications.cancelScheduledNotificationAsync(notificationId);

// Cancel all scheduled notifications
await Notifications.cancelAllScheduledNotificationsAsync();

// List pending notifications
const pending = await Notifications.getAllScheduledNotificationsAsync();
```

---

## 7. Badge Count Management

```tsx
// Set badge count (number on app icon)
await Notifications.setBadgeCountAsync(5);

// Clear badge on open
useEffect(() => {
  const sub = AppState.addEventListener('change', state => {
    if (state === 'active') {
      Notifications.setBadgeCountAsync(0); // clear badge when app opens
    }
  });
  return () => sub.remove();
}, []);
```

---

## 8. Background Execution — The Mobile Constraint

### The fundamental rule

Mobile operating systems aggressively kill background apps to preserve battery.
Background JavaScript execution is severely limited on both iOS and Android.

```text
iOS Background Execution:
  - Normal background: about 30 seconds after app goes to background
  - Background fetch: OS decides when to wake app — not developer-controlled
  - Background download: NSURLSession can finish large downloads in background
  - Push silent notifications: ~30 seconds of execution triggered by silent push
  - Background audio/navigation/voip: special background modes in Info.plist

Android Background Execution:
  - More permissive than iOS
  - Foreground service: persistent notification, can run indefinitely
  - WorkManager: deferred background work, battery-optimized scheduling
  - Doze mode / App Standby: restricts network and CPU in deep sleep
  - Background services: restricted in Android 8+
```

### Background fetch with expo-background-fetch

```tsx
import * as BackgroundFetch from 'expo-background-fetch';
import * as TaskManager from 'expo-task-manager';

const BACKGROUND_FETCH_TASK = 'background-fetch-task';

// Define the task — this runs in the background
TaskManager.defineTask(BACKGROUND_FETCH_TASK, async () => {
  try {
    const newData = await fetchLatestData();
    await AsyncStorage.setItem('latestData', JSON.stringify(newData));
    return BackgroundFetch.BackgroundFetchResult.NewData;
  } catch {
    return BackgroundFetch.BackgroundFetchResult.Failed;
  }
});

// Register the task
async function registerBackgroundFetch() {
  const status = await BackgroundFetch.getStatusAsync();
  if (status === BackgroundFetch.BackgroundFetchStatus.Restricted ||
      status === BackgroundFetch.BackgroundFetchStatus.Denied) {
    return; // OS does not permit background fetch
  }

  await BackgroundFetch.registerTaskAsync(BACKGROUND_FETCH_TASK, {
    minimumInterval: 15 * 60, // minimum 15 minutes — OS may delay further
    stopOnTerminate: false,    // continue after app is killed (Android)
    startOnBoot: true,         // restart on device reboot (Android)
  });
}
```

Important: iOS does NOT guarantee background fetch runs at your requested interval. The OS learns app usage patterns and schedules background fetch based on when the user typically opens the app.

### Silent push notifications for background data refresh

A silent notification wakes the app briefly to fetch new data without showing any visible notification to the user:

```tsx
// Silent notification payload (from your server)
// iOS: content-available: 1, no alert/badge/sound
// Android: data-only, no notification object

TaskManager.defineTask('silent-push-task', async ({data, error}) => {
  if (error) return;
  // Fetch fresh data and update local cache
  try {
    const latest = await fetchLatestMessages();
    await AsyncStorage.setItem('messages', JSON.stringify(latest));
    // Optionally show a local notification if there are new messages
    if (latest.unreadCount > 0) {
      await Notifications.scheduleNotificationAsync({
        content: {title: `${latest.unreadCount} new messages`, body: ''},
        trigger: null, // immediate
      });
    }
  } catch {}
});
```

---

## 9. Permissions — iOS vs Android Differences

### iOS Permission Flow

```tsx
// iOS: must explicitly request notification permission
// Best practice: ask at a relevant moment, not on app first launch

// Check current status without showing a prompt
const {status} = await Notifications.getPermissionsAsync();
// 'undetermined' | 'granted' | 'denied'

// Show the system permission dialog (iOS — only works once if denied)
const {status: newStatus} = await Notifications.requestPermissionsAsync({
  ios: {
    allowAlert: true,
    allowBadge: true,
    allowSound: true,
    allowProvisional: false, // provisional = silent delivery to notification center
  },
});

if (newStatus === 'denied') {
  // Cannot re-prompt — must tell user to go to Settings
  Alert.alert(
    'Notifications Disabled',
    'Enable notifications in Settings to receive order updates.',
    [{text: 'Open Settings', onPress: () => Linking.openSettings()}],
  );
}
```

### Android Permission Flow

```tsx
// Android 13+ (API 33+) requires POST_NOTIFICATIONS permission
import {PermissionsAndroid, Platform} from 'react-native';

async function requestAndroidNotificationPermission() {
  if (Platform.OS !== 'android' || Platform.Version < 33) return true;

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}
```

---

## 10. Production Pattern — Full Notification Service

```tsx
// services/NotificationService.ts
class NotificationService {
  private foregroundSub: Notifications.Subscription | null = null;
  private responseSub: Notifications.Subscription | null = null;

  async initialize(onNavigate: (data: NotificationData) => void) {
    // Register for push notifications
    const token = await registerForPushNotifications();
    if (token) await this.syncTokenToServer(token);

    // Handle foreground notifications
    this.foregroundSub = Notifications.addNotificationReceivedListener(n => {
      this.handleForegroundNotification(n);
    });

    // Handle taps
    this.responseSub = Notifications.addNotificationResponseReceivedListener(r => {
      onNavigate(r.notification.request.content.data as NotificationData);
    });

    // Handle cold start from notification
    const lastResponse = await Notifications.getLastNotificationResponseAsync();
    if (lastResponse) {
      onNavigate(lastResponse.notification.request.content.data as NotificationData);
    }
  }

  private handleForegroundNotification(notification: Notifications.Notification) {
    // Show in-app toast/snackbar since system banner behavior varies
    Toast.show(notification.request.content.body ?? '');
  }

  private async syncTokenToServer(token: string) {
    try {
      await api.post('/devices/register', {token, platform: Platform.OS});
    } catch (err) {
      console.error('Failed to register push token:', err);
    }
  }

  destroy() {
    this.foregroundSub?.remove();
    this.responseSub?.remove();
  }
}

export const notificationService = new NotificationService();
```

---

## 11. Common Traps

### Trap 1: Testing on simulator

Push notifications don't work on simulators. Always test on physical devices for notification behavior. Local notifications work on simulators but remote push does not.

### Trap 2: Assuming background JavaScript runs freely

```text
Wrong assumption: "I'll just setInterval to sync data in background."

Reality: The JavaScript thread is paused when the app goes to background.
setInterval, fetch, and other async operations stop running.
Use background fetch tasks or silent push for background work.
```

### Trap 3: Not handling the killed state

Most developers handle foreground and background but forget the killed-state scenario where the user taps a notification to launch the app from scratch. Always call `getLastNotificationResponseAsync` on mount.

### Trap 4: Token not refreshed after reinstall

Always fetch a fresh push token on app launch and compare it with the last saved token. If different, update your server. Sending to an old token results in silent delivery failure.

---

## 12. Revision Notes

- iOS uses APNs, Android uses FCM — both needed for cross-platform push
- Three app states: foreground (listener fires), background (OS shows, no JS), killed (getLastNotificationResponse on launch)
- Always use `getLastNotificationResponseAsync` on app mount to handle cold start from notification tap
- iOS permission can only be shown once — if denied, direct user to Settings
- Android 13+ requires `POST_NOTIFICATIONS` permission at runtime
- Background JavaScript execution is limited — use `expo-background-fetch` or silent push for background work
- Silent (data-only) push wakes the app for ~30 seconds to fetch data
- Always sync the push token to your server after registration and after any token refresh
- Badge count should be cleared when user opens the app
- Test all notification states on physical devices — simulators do not support remote push
