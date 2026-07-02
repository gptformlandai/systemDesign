# React Native Device APIs: Camera, Maps, BLE, NFC, Files, And Payments - Gold Sheet

> Track Module - Group 3: Native Device And Internals
> Level: production mobile capability design

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Permission-first design | Very high | Mobile features fail without privacy-safe prompts |
| Camera/media pipeline | High | Common app feature with memory/performance risk |
| Location/maps/geofencing | High | Common, privacy-sensitive, battery-sensitive |
| Files/document picker | Medium-high | Uploads, enterprise workflows, receipts, claims |
| BLE/NFC/sensors | Medium | Shows real native-device depth |
| Payments/IAP | Medium-high | High compliance and failure cost |

MAANG signal:
You design device features as unreliable, privacy-sensitive platform capabilities, not as simple JavaScript calls.

---

## 2. Mental Model

Every device API has five layers:

```text
User intent
  -> permission and privacy consent
  -> native platform capability
  -> JS wrapper/hook/service
  -> product fallback and telemetry
```

The app must handle:
- unsupported device
- denied permission
- temporarily unavailable hardware
- background restrictions
- OS version differences
- vendor/OEM behavior
- memory and battery pressure

---

## 3. Capability Matrix

| Domain | Examples | Main Risks |
|---|---|---|
| Camera/media | capture, gallery, crop, compression, upload | permission, memory, EXIF/PII, upload retry |
| Location/maps | current location, tracking, geofence, routes | privacy, battery, background policy |
| BLE | device scan/connect/read/write | Android permission split, connection reliability |
| NFC | tap cards/tags | device support, foreground session limits |
| Files | document picker, local cache, upload/download | storage scope, large files, cleanup |
| Contacts/calendar | import/invite/schedule | high privacy sensitivity |
| Share sheet | share text/files/deep links | app availability, content leakage |
| Sensors | accelerometer, gyroscope, health-like signals | sampling rate, battery, permission |
| Payments/IAP | Apple/Google IAP, wallets, checkout SDKs | compliance, idempotency, receipt validation |

---

## 4. Permission State Machine

```text
unknown
  -> can_ask
  -> granted
  -> denied_can_ask_again
  -> denied_permanently
  -> restricted_by_parental_control_or_policy
  -> unavailable_on_device
```

Do not ask immediately on app launch unless the app cannot function without it.

Better flow:

```text
1. User taps feature.
2. App explains value in product language.
3. App requests OS permission.
4. App handles grant/deny/restricted.
5. App provides a fallback or settings path.
```

---

## 5. Permission Hook Pattern

```ts
type PermissionStatus =
  | 'unknown'
  | 'granted'
  | 'denied'
  | 'blocked'
  | 'unavailable';

type PermissionResult = {
  status: PermissionStatus;
  canAskAgain: boolean;
};

export function mapPermission(result: {
  granted: boolean;
  canAskAgain?: boolean;
  status?: string;
}): PermissionResult {
  if (result.granted) return {status: 'granted', canAskAgain: false};
  if (result.status === 'undetermined') return {status: 'unknown', canAskAgain: true};
  if (result.canAskAgain === false) return {status: 'blocked', canAskAgain: false};
  return {status: 'denied', canAskAgain: true};
}
```

Production rule:
Map platform-specific permission shapes into a product-level state machine.

---

## 6. Camera And Media

Design concerns:
- camera permission
- photo library permission
- image size and compression
- memory usage
- upload retry/cancel
- EXIF metadata stripping
- background upload behavior
- offline upload queue

Recommended flow:

```text
capture/select
  -> validate file type and size
  -> optionally resize/compress
  -> strip sensitive metadata if not needed
  -> store temporary local file
  -> upload with progress and cancellation
  -> delete temp file after success or expiry
```

Common trap:
Uploading original 12 MB photos directly from the UI thread path creates slow screens, memory pressure, and failed uploads on poor networks.

---

## 7. Location, Maps, And Geofencing

Location questions before implementation:
- Is approximate location enough?
- Is background location truly required?
- How often do we need updates?
- Can the backend infer or cache location?
- What should happen when permission is denied?
- How do we explain battery impact?

Foreground location:
Good for maps, nearby stores, delivery quote, check-in.

Background location:
Use only for strong product need. It increases app review scrutiny, battery cost, and privacy risk.

Geofencing:
Use coarse business rules and server validation when possible. Device geofences are best-effort, not a financial-grade truth source.

---

## 8. BLE, NFC, And Sensors

BLE flow:

```text
check support
  -> request permissions
  -> scan with timeout
  -> filter devices
  -> connect
  -> discover services
  -> read/write/subscribe
  -> reconnect/cleanup
```

BLE traps:
- scans drain battery
- Android permissions changed across OS versions
- devices disconnect frequently
- app background behavior is limited
- user may need Bluetooth/location enabled

NFC flow:

```text
check support
  -> start foreground session
  -> read tag or card
  -> validate payload
  -> end session
```

Sensor rules:
- sample at the lowest acceptable rate
- throttle events before JS
- stop listeners on unmount/background
- never treat sensor data as perfectly accurate

---

## 9. Files And Documents

Use cases:
- upload receipts
- import PDFs
- export reports
- cache media
- open external files

Design rules:
- Ask for the narrowest access.
- Store only what is needed.
- Validate MIME type and extension.
- Enforce max file size before upload.
- Clean temp files.
- Support retry and cancellation.

Large file upload checklist:
- progress UI
- cancellation
- resume or retry
- idempotency key
- background/foreground behavior
- server-side virus/type validation

---

## 10. Payments And In-App Purchases

Payments are not just UI:

```text
client intent
  -> native payment sheet or IAP purchase
  -> receipt/token
  -> backend verification
  -> idempotent order/subscription fulfillment
  -> audit trail and rollback/refund path
```

Rules:
- Never trust the client as payment authority.
- Verify receipts or payment tokens on the backend.
- Use idempotency keys for order creation.
- Handle pending, cancelled, failed, refunded, and restored states.
- Keep app store policies in mind for digital goods.

Interview trap:
Saying "after payment success in the app, unlock the feature" without backend receipt validation.

---

## 11. Package Choice Framework

Evaluate:
- maintenance activity
- Expo compatibility
- New Architecture compatibility
- iOS and Android parity
- permission documentation
- release-build examples
- native dependency footprint
- app store compliance notes
- ability to test/mocks

Prefer:
- official Expo modules for Expo apps
- mature community packages
- wrappers around official native SDKs
- small APIs with clear platform behavior

---

## 12. Observability For Device Features

Track:
- permission prompt shown
- permission granted/denied/blocked
- hardware unsupported
- capture/select success
- upload started/progress/success/failure
- location lookup latency
- BLE scan/connect failure reason
- payment cancelled/failed/verified

Never log:
- raw location unless necessary and consented
- payment card data
- tokens
- contact lists
- full file contents

---

## 13. Strong Interview Answer

```text
For device APIs I start with the user intent and permission model, not the library.
I map platform permissions into product states, provide a denial fallback, and
test on real devices because camera, location, BLE, NFC, files, and payments all
depend on hardware and OS behavior. For sensitive domains I minimize data, avoid
logging PII, validate on the backend, and instrument success, denial, unavailable,
and failure states so production support can diagnose issues.
```

---

## 14. Revision Notes

- One-line summary: Device APIs are privacy-sensitive native capabilities with unreliable availability.
- Three keywords: permission, fallback, real device.
- One interview trap: Treating hardware APIs like normal JS utilities.
- Memory trick: Intent -> permission -> hardware -> wrapper -> fallback.
