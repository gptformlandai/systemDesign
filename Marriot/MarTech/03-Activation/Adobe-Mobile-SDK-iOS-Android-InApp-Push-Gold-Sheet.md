# Adobe Mobile SDK — iOS, Android, In-App MarTech — Gold Sheet

> Topic: How Adobe's MarTech stack works inside a mobile app — the same Analytics, Target, and AEP functionality but optimized for native iOS and Android

---

## 1. Intuition

The Marriott Bonvoy app is not a website — it's a native iOS and Android app. It can't load alloy.js or use browser cookies. But it still needs to: track which screens users view, fire booking confirmation events to Analytics, show personalized content via Target, push users into AEP profiles, and trigger AJO push notifications. The Adobe Experience Platform Mobile SDK (formerly Adobe Mobile SDK v5) is the alloy.js equivalent for native apps — one SDK, all products.

Beginner version:

> Web pages use alloy.js (Web SDK). Mobile apps use the AEP Mobile SDK. Same idea — one library that connects your app to Analytics, Target, AEP, and AJO — just written for Swift (iOS) and Kotlin (Android) instead of JavaScript.

---

## 2. Definition

- **AEP Mobile SDK:** Adobe's native SDK for iOS (Swift/Obj-C) and Android (Kotlin/Java) that collects events, sends them to the Adobe Edge Network, and powers Analytics, Target, AEP, and AJO inside mobile apps.
- **Core extension:** The base SDK module all other extensions depend on — handles configuration, event hub, and lifecycle events.
- **Lifecycle extension:** Auto-tracks app opens, closes, crashes, session length, and device info.
- **Edge extension:** The mobile equivalent of Web SDK — sends XDM events to Adobe Edge Network via DataStream.

---

## 3. Architecture — How Mobile SDK Relates to Web SDK

```
WEB                                    MOBILE
─────────────────────────────────────────────────────────
alloy.js (browser JavaScript)     AEP Mobile SDK (native)
     │                                    │
     │                                    │
     ▼                                    ▼
Adobe Edge Network          Adobe Edge Network
     │                                    │
     ▼                                    ▼
DataStream routing           Same DataStream routing
  → Analytics                    → Analytics
  → Target                       → Target
  → AEP                          → AEP
  → Event Forwarding             → Event Forwarding

Key difference:
  Web:    cookies store ECID (s_ecid cookie)
  Mobile: ECID stored in local device storage (Keychain on iOS, SharedPreferences on Android)
```

---

## 4. SDK Setup — iOS (Swift)

```swift
// Package.swift or CocoaPods — add dependencies
// AEP Mobile SDK uses Swift Package Manager

// In AppDelegate or App init:
import AEPCore
import AEPEdge
import AEPEdgeIdentity
import AEPAnalytics
import AEPTarget
import AEPAssurance   // Debugging tool (Griffon)

@main
struct MarriottBonvoyApp: App {
    init() {
        // 1. Register all extensions
        MobileCore.registerExtensions([
            Edge.self,
            EdgeIdentity.self,
            Analytics.self,
            Target.self,
            AEPAssurance.self
        ]) {
            // 2. Configure the SDK with your DataStream / Launch property ID
            MobileCore.configureWith(appId: "your-launch-app-id")
            // Launch App ID = found in Tags → Mobile Property → Environments
            
            // Privacy status
            MobileCore.setPrivacyStatus(.optedIn)  // or .optedOut / .unknown
        }
    }
}
```

---

## 5. SDK Setup — Android (Kotlin)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.adobe.marketing.mobile:core:3.x.x")
    implementation("com.adobe.marketing.mobile:edge:3.x.x")
    implementation("com.adobe.marketing.mobile:edgeidentity:3.x.x")
    implementation("com.adobe.marketing.mobile:analytics:3.x.x")
    implementation("com.adobe.marketing.mobile:target:3.x.x")
    implementation("com.adobe.marketing.mobile:assurance:3.x.x")  // Debugging
}

// Application class:
class MarriottApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        MobileCore.setApplication(this)
        
        MobileCore.registerExtensions(
            listOf(Edge.EXTENSION, EdgeIdentity.EXTENSION,
                   Analytics.EXTENSION, Target.EXTENSION,
                   Assurance.EXTENSION)
        ) {
            MobileCore.configureWithAppID("your-launch-app-id")
            MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN)
        }
    }
}
```

---

## 6. Sending Events (XDM) from the App

Once the SDK is initialized, send events from anywhere in the app:

```swift
// iOS — Track screen view (equivalent of page view in web)
let xdmData: [String: Any] = [
    "eventType": "web.webpagedetails.pageViews",
    "web": [
        "webPageDetails": [
            "name": "hotel-detail",
            "URL": "marriottapp://hotels/NYCMQ"
        ]
    ]
]

let experienceEvent = ExperienceEvent(xdm: xdmData, data: [
    "_marriott": [
        "hotel": [
            "code": "NYCMQ",
            "name": "Marriott Marquis Times Square",
            "city": "New York"
        ]
    ]
])

Edge.sendEvent(experienceEvent: experienceEvent)
```

```kotlin
// Android — Track booking confirmation
val xdmData = mapOf(
    "eventType" to "commerce.purchases",
    "commerce" to mapOf(
        "purchases" to mapOf("value" to 1),
        "order" to mapOf(
            "currencyCode" to "USD",
            "priceTotal" to 450.00,
            "purchaseID" to "B-98765"
        )
    ),
    "productListItems" to listOf(mapOf(
        "SKU" to "NYCMQ-DELUXE-KING",
        "name" to "Marriott Marquis Times Square - Deluxe King",
        "priceTotal" to 450.00,
        "quantity" to 1
    ))
)

val event = ExperienceEvent.Builder()
    .setXdmSchema(xdmData)
    .build()

Edge.sendEvent(event) { handles ->
    // handles contain any personalization decisions returned
}
```

---

## 7. Lifecycle Tracking — Auto-Captured App Events

The Lifecycle extension auto-tracks events without any code in the app:

```
App Install (first open):
  Lifecycle event → Analytics hit with:
    a.InstallEvent = 1
    a.OSVersion = "iOS 18.2"
    a.DeviceName = "iPhone 16 Pro"
    a.AppVersion = "8.4.1"

App Launch:
  Lifecycle event every app open →
    a.LaunchEvent = 1
    a.PrevSessionLength = 340 (seconds of last session)
    a.DaysSinceFirstUse = 45
    a.DaysSinceLastUse = 3

App Crash (unexpected close):
  On next launch, SDK detects abnormal termination →
    a.CrashEvent = 1
  (useful for monitoring app stability in Analytics)

Session:
  Foreground = session start
  Background > 5 minutes = session end (configurable timeout)
```

---

## 8. Push Notifications — AJO + Mobile SDK

AJO sends push notifications through the Mobile SDK. The SDK bridges AJO with Apple Push Notification Service (APNS) and Firebase Cloud Messaging (FCM):

```swift
// iOS — Register device token with AEP after APNS grants permission
func application(_ application: UIApplication,
                 didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    // Pass APNS token to AEP SDK — this links the device to the ECID/profile
    MobileCore.setPushIdentifier(deviceToken)
}

// iOS — Handle incoming push notification from AJO
func userNotificationCenter(_ center: UNUserNotificationCenter,
                             didReceive response: UNNotificationResponse,
                             withCompletionHandler handler: @escaping () -> Void) {
    // Report notification interaction back to AJO for journey analytics
    let userInfo = response.notification.request.content.userInfo
    MobileCore.track(action: "push_opened", data: [
        "a.push.id": userInfo["adb_msg_id"] as? String ?? ""
    ])
    handler()
}
```

```kotlin
// Android — Register FCM token with AEP
class MarriottFirebaseService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        MobileCore.setPushIdentifier(token)  // Links device to AEP profile
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        // AEP SDK handles rendering of AJO push messages automatically
        // if you use the Messaging extension
    }
}
```

**AJO then:**
1. Knows the APNS/FCM token linked to a profile (via `setPushIdentifier`)
2. When a journey reaches a push action → sends to APNS/FCM with that token
3. Delivery status (sent, delivered, opened) flows back to AJO for journey reporting

---

## 9. In-App Messages — AJO Messaging Extension

In-app messages display while the user is inside the app — no push permission needed:

```swift
// Enable AJO in-app messaging (add to registration):
import AEPMessaging

MobileCore.registerExtensions([
    Edge.self, EdgeIdentity.self,
    Messaging.self      // ← AJO in-app messages
]) { ... }

// In-app messages are triggered automatically when:
//   - User opens specific screen (trigger: "screen_name == hotel-detail")
//   - User views X hotels without booking (trigger: "hotel_views >= 3 AND no_booking")
//   - AJO journey reaches an in-app message node
//   - SDK evaluates trigger rules locally (no network call needed for evaluation)
```

Example in-app message triggered by AJO:

```
User opens hotel detail screen for the 3rd time this session
  → Mobile SDK evaluates local trigger rules
  → Condition: screen = "hotel-detail" AND session_hotel_views >= 3
  → AJO in-app message appears:
     ┌─────────────────────────────────────┐
     │  Still deciding?                    │
     │  Book today and earn 2,000          │
     │  bonus Bonvoy points                │
     │                                     │
     │  [Book Now]    [Remind Me Later]    │
     └─────────────────────────────────────┘
  → User taps "Book Now" → deep link to booking flow
  → Tap event tracked back to AJO → journey continues
```

---

## 10. Identity in Mobile — No Cookies

Mobile apps don't have browser cookies. ECID is stored in device storage:

```
First app open:
  SDK generates ECID: 12345678901234567890
  Stored in: iOS Keychain / Android SharedPreferences
  Persists across: app restarts, OS updates
  Deleted when: app is uninstalled

User logs in (loyalty ID):
  MobileCore.updateIdentities(IdentityMap("loyaltyId", "MR-GOLD-789", .authenticated))
  → AEP identity graph links: ECID ↔ loyaltyId

Cross-device recognition:
  Same user on web (ECID aaaa from browser cookie) and app (ECID bbbb from device storage)
  Both share loyaltyId = "MR-GOLD-789" when authenticated
  AEP identity graph: ECID-aaaa ↔ loyaltyId ↔ ECID-bbbb → one profile
```

---

## 11. Adobe Target on Mobile (In-App Personalization)

Target delivers personalized content inside the app — different offers, layouts, or content for different user segments:

```swift
// Request Target personalization before rendering a screen
let request = TargetRequest(
    mboxName: "home-hero-offer",       // Location name configured in Target
    defaultContent: "default-offer",   // Fallback if Target is unreachable
    targetParameters: TargetParameters(
        profileParameters: ["loyaltyTier": "titanium"],
        order: nil,
        product: nil
    )
) { content in
    // content = what Target returned (could be JSON, HTML snippet, or offer code)
    self.renderOffer(content)
}

Target.retrieveLocationContent([request])
```

---

## 12. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Not calling `setPushIdentifier` after getting APNS/FCM token | AJO cannot send push to this device — profile has no push token | Call `MobileCore.setPushIdentifier` in the token registration callback |
| Forgetting to set privacy status | SDK defaults to "unknown" — collects no data until consent is confirmed | Set `MobilePrivacyStatus.OPT_IN` after user accepts consent; `OPT_OUT` if they decline |
| Sending raw loyalty ID (not hashed) in identity map | PII in edge events — GDPR risk | Use hashed values for email/phone; loyalty IDs are typically non-PII (opaque IDs) |
| Not handling app backgrounding in event tracking | Background-to-foreground transitions miscounted as new sessions | Use Lifecycle extension defaults; customize `lifecycleSessionTimeout` if needed |
| Not testing with Assurance (Griffon) before release | Tracking bugs discovered after app store release — can't hot-fix native apps | Always QA with Assurance connected before every release |

---

## 13. Adobe Experience Platform Assurance (Griffon)

Assurance is the mobile debugging tool — the mobile equivalent of the AEP Debugger browser extension:

```
1. Connect app to Assurance:
   In App: scan QR code from Assurance session → SDK connects to Assurance session

2. In Assurance UI, you see in real time:
   ├── Every SDK event fired (with full XDM payload)
   ├── Every Edge request and response
   ├── Analytics hits (what variables were set)
   ├── Target decisions returned
   ├── AJO in-app message triggers evaluated
   └── Lifecycle events

3. Debugging workflow:
   Fire event in app → see it appear in Assurance UI in real time
   Verify XDM fields are correct → verify Edge response contains Target decisions
   No need for a proxy or device certificate
```

---

## 14. Interview Insight

Strong answer:

> The AEP Mobile SDK is the native equivalent of Web SDK (alloy.js) for iOS and Android apps. It uses the same DataStream and Edge Network — so the same Analytics report suite, the same AEP datasets, and the same AJO journeys work for both web and app. The key mobile differences: ECID is stored in device Keychain/SharedPreferences instead of a browser cookie; push notification tokens are registered via `setPushIdentifier` to link devices to AEP profiles; in-app messages are evaluated locally against trigger rules without a network call. For debugging, Assurance (Griffon) replaces the browser's AEP Debugger extension — you connect via QR code and see every SDK event in real time.

Follow-up trap:

> If a user uses both the Marriott website and the Marriott app on the same phone, are they recognized as the same person?

Good answer:

> On an anonymous basis, no — they have different ECIDs (one in the browser's s_ecid cookie, one in the app's Keychain). But the moment they log in on either surface, the loyalty ID links them. The AEP identity graph sees: browser ECID ↔ loyaltyId (from web login) and app ECID ↔ loyaltyId (from app login). Both ECIDs are now linked through loyaltyId — they resolve to one unified profile. All their web and app behavior is stitched into a single journey that AJO and CJA can use.

---

## 15. Revision Notes

- One-line summary: AEP Mobile SDK (Swift/Kotlin) is the native app equivalent of Web SDK — same DataStream, same Edge Network, same Analytics/Target/AEP/AJO — with ECID in device storage instead of cookies, and push via APNS/FCM.
- Three keywords: Lifecycle extension, setPushIdentifier, Assurance (Griffon).
- One interview trap: anonymous web and app ECIDs are different — they only stitch when the user authenticates on both and loyaltyId is present on both surfaces.
- Memory trick: Mobile SDK = Web SDK but native. Same Edge, same DataStream, different ID storage (Keychain vs cookie). Assurance = Griffon = the Debugger extension for apps.
