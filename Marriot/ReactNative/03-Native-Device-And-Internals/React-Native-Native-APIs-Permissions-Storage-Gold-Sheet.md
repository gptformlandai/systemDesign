# React Native Native APIs, Permissions, And Storage - Gold Sheet

> Track Module - Group 3: Native Device And Internals
> Level: mobile capability design and production safety

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Runtime permissions | Very high | Camera, location, photos, notifications |
| Secure storage | Very high | Auth/session safety |
| AsyncStorage/MMKV | High | Local cache/preferences |
| App lifecycle | High | Foreground/background behavior |
| Push notifications | High | Real product apps rely on them |
| File/image handling | Medium-high | Uploads, avatars, media apps |
| Platform-specific capability | High | iOS and Android APIs differ |
| Background work | High | Constrained and commonly misunderstood |

MAANG signal:
You treat native APIs as privacy-sensitive, failure-prone, platform-specific capabilities.

---

## 2. Mental Model

Native APIs are not just functions. They are contracts with the operating system.

```text
JavaScript call
  -> native module
  -> OS permission / capability
  -> result or denial
  -> app fallback UI
```

Every native API needs:
- permission strategy
- denied state
- unavailable state
- platform-specific behavior
- user education
- privacy-safe telemetry
- graceful fallback

---

## 3. Permissions

Common permission types:
- camera
- microphone
- location foreground/background
- photo library/media
- contacts
- notifications
- Bluetooth/NFC
- motion sensors

Permission states are not binary.

```text
unknown -> requestable -> granted
                    -> denied
                    -> blocked / do not ask again
                    -> limited access
                    -> unavailable
```

Production rule:
Ask for permission when the user is about to use the feature, not at app launch unless the product absolutely requires it.

---

## 4. Permission Flow

```tsx
type PermissionStatus = 'granted' | 'denied' | 'blocked' | 'unavailable';

async function openCameraFlow() {
  const status = await cameraPermission.getStatus();

  if (status === 'granted') {
    return camera.open();
  }

  if (status === 'blocked') {
    showOpenSettingsDialog();
    return;
  }

  const nextStatus = await cameraPermission.request();

  if (nextStatus === 'granted') {
    return camera.open();
  }

  showCameraDeniedState();
}
```

Interview point:
Users can deny permissions. Production apps must still be usable or explain the missing capability clearly.

---

## 5. Storage Types

| Storage | Use For | Do Not Use For |
|---|---|---|
| SecureStore / Keychain / Keystore | auth tokens, refresh tokens, sensitive values | large cache |
| AsyncStorage | non-secret preferences, small cache | secrets, high-performance large data |
| MMKV | fast key-value local state | secrets unless encrypted and reviewed |
| SQLite | structured offline data | simple one-off preferences |
| File system | images, documents, downloads | token/session storage |
| In-memory store | current runtime state | data that must survive restart |

Rule:
If the value grants account access, treat it as sensitive.

---

## 6. Token Storage Pattern

```ts
type TokenStore = {
  getAccessToken(): Promise<string | null>;
  setAccessToken(token: string): Promise<void>;
  clear(): Promise<void>;
};

export function createAuthHeaders(token: string | null) {
  return token ? {Authorization: `Bearer ${token}`} : {};
}
```

Production concerns:
- Prefer short-lived access tokens.
- Store refresh tokens only in secure storage.
- Clear tokens on logout.
- Clear tokens after server-side revocation.
- Never log tokens.
- Avoid putting tokens in deep links.

---

## 7. App Lifecycle

Apps move between states:

```text
active -> inactive -> background -> active
```

Use cases:
- pause expensive polling in background
- refresh stale data on foreground
- lock sensitive screens after timeout
- flush analytics
- resume downloads carefully

Example:

```tsx
useEffect(() => {
  const subscription = AppState.addEventListener('change', state => {
    if (state === 'active') {
      refreshSessionIfNeeded();
    }
  });

  return () => subscription.remove();
}, []);
```

Trap:
Do not assume background JavaScript can run freely. iOS and Android restrict background work heavily.

---

## 8. Push Notifications

Notification flow:

```text
1. Ask permission at meaningful moment.
2. Register device with platform push service.
3. Send push token to backend.
4. Backend sends notification through APNs/FCM.
5. App handles foreground/background/tapped notification.
6. Navigation resolves deep link safely.
```

Production concerns:
- Tokens rotate.
- Users disable notifications.
- Notification tap may cold-start the app.
- Payloads should not contain sensitive PII.
- Deep-linked resources may require auth.
- Analytics should distinguish delivered/opened/actioned when possible.

---

## 9. File And Image Handling

Common tasks:
- pick image
- capture image
- resize/compress
- upload file
- download document
- cache images

Production rules:
- Compress before upload when quality allows.
- Use signed upload URLs for large media.
- Show progress for long uploads.
- Retry idempotently.
- Handle permission denial.
- Strip or protect sensitive metadata where relevant.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Asking all permissions on first launch | Scary and low conversion | Ask in context |
| Storing tokens in normal storage | Security risk | Use secure storage |
| Assuming background JS always runs | OS may suspend app | Use platform background APIs |
| No denied-permission UI | Feature feels broken | Explain and offer settings path |
| Logging notification payloads | PII risk | Redact/sanitize logs |
| Uploading original huge images | Slow, costly, failure-prone | Compress and show progress |

---

## 11. Strong Interview Answer

Question:
How would you handle camera permission in a React Native app?

Strong answer:

```text
I would request camera permission only when the user chooses a camera action.
First I check the current status. If granted, I open the camera. If denied, I show
a clear fallback. If blocked, I show an explanation and a button to app settings.
I track the funnel without logging sensitive content. For uploads, I compress the
image, show progress, use a signed upload URL if files are large, and handle retry
or cancellation safely.
```

---

## 12. Revision Notes

- One-line summary: Native APIs need permissions, platform handling, and graceful fallback.
- Three keywords: permission, secure storage, lifecycle.
- One interview trap: Background JS is not unlimited.
- One memory trick: OS capability means user consent plus platform constraints.

