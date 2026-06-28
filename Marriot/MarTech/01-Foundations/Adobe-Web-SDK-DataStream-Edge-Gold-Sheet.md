# Adobe Web SDK (alloy.js) and DataStreams — Gold Sheet

> Topic: The modern Adobe data collection standard — one library, one request, all Adobe products — replacing at.js + AppMeasurement

---

## 1. Intuition

Before Web SDK, Marriott's page loaded THREE separate Adobe libraries: `AppMeasurement.js` (for Analytics), `at.js` (for Target), and `VisitorAPI.js` (for the shared ECID). Each library made its own network request to its own endpoint. Web SDK (alloy.js) replaces all three — one library, one network request to Adobe's Edge Network, which fans it out to Analytics, Target, and AEP simultaneously.

Beginner version:

> Old way: three libraries loading separately, three network calls. New way: one library (alloy.js), one network call to Adobe's Edge, which delivers data to Analytics, Target, and AEP at once — faster and simpler.

---

## 2. Definition

- **Adobe Web SDK (alloy.js):** A single JavaScript library that collects and sends data to all Adobe Experience Cloud products via the Adobe Edge Network.
- **DataStream:** A server-side configuration on Adobe's Edge that defines which products receive the data (Analytics, Target, AEP) and how it maps.
- **Edge Network:** Adobe's globally distributed server infrastructure that receives Web SDK events and routes them to the right products.
- **XDM (Experience Data Model):** The schema format Web SDK uses for all event payloads — same as AEP's schema language.

---

## 3. Legacy Architecture vs Web SDK Architecture

### Legacy (still common — you'll see both in production)

```
Browser
  ├── AppMeasurement.js (Analytics library)
  │     → GET /b/ss/marriott.global/... (beacon to Analytics Edge servers)
  │
  ├── at.js (Target library)
  │     → POST edge.adobedc.net/... (Target decision request)
  │
  └── VisitorAPI.js (ECID / Identity library)
        → GET dpm.demdex.net/... (third-party cookie for cross-domain ID)

Problems:
  - 3 libraries loading (each 50-100KB) → slow
  - 3 separate network requests
  - At.js blocks rendering to prevent flicker (adds 100-300ms)
  - AppMeasurement doesn't natively support AEP
```

### Web SDK (Modern — Adobe's direction for 2024+)

```
Browser
  └── alloy.js (single library ~50KB)
        → POST edge.adobedc.net/ee/v2/interact?datastreamId=...
              (ONE request to Adobe Edge Network)

Adobe Edge Network (server-side routing):
  ├── → Adobe Analytics report suite (via DataStream mapping)
  ├── → Adobe Target (decision + personalization response)
  ├── → AEP Profile (event ingested, profile updated)
  └── → AEP Segmentation (streaming segment evaluation)

Benefits:
  - 1 library, 1 network request
  - Server-side rendering of Target decisions (no client-side flicker risk)
  - Native XDM schema → direct AEP ingestion without mapping
  - Supports Web, mobile (iOS/Android SDK), server-side (Edge SDK)
```

---

## 4. DataStream Configuration

A DataStream is Adobe's server-side routing config. You create it in Adobe Experience Platform UI:

```
DataStream: "Marriott Production"
  ID: abc123-def456-...  ← referenced in alloy() configure call

  Services enabled:
  ┌─────────────────────────────────────────────────────┐
  │ Adobe Analytics                                      │
  │   Report Suite ID: marriott.global                  │
  │   Enable: ✓                                         │
  └─────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────┐
  │ Adobe Target                                         │
  │   Property Token: abc-123 (optional)                │
  │   Enable: ✓                                         │
  └─────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────┐
  │ Adobe Experience Platform                            │
  │   Sandbox: prod                                      │
  │   Event Dataset: Marriott Web Events                │
  │   Profile Dataset: Marriott Profiles                │
  │   Enable: ✓                                         │
  └─────────────────────────────────────────────────────┘
```

You can have MULTIPLE DataStreams for different environments:
- `Marriott Development DataStream` → dev Analytics RS + dev AEP sandbox
- `Marriott Staging DataStream` → staging RS + staging AEP
- `Marriott Production DataStream` → production RS + production AEP

---

## 5. Web SDK JavaScript API

### Initialization (runs once on page load)

```javascript
// alloy.js must be loaded first (via <script> tag or Tags extension)
alloy("configure", {
  datastreamId: "abc123-def456-789abc",   // Your DataStream ID
  orgId: "ABC123DEF@AdobeOrg",            // Adobe IMS Organization ID
  
  // Identity config
  idMigrationEnabled: true,     // Read existing s_ecid cookie (migration from legacy)
  thirdPartyCookiesEnabled: false, // Disable demdex.net 3rd party cookie (privacy)
  
  // Personalization config
  prehidingStyle: "#hero-banner { visibility: hidden; }",  // Anti-flicker for Target
  
  // Debug (dev only)
  debugEnabled: true,
  
  // Consent (IAB TCF or Adobe Consent Standard)
  defaultConsent: "pending"   // Wait for user consent before sending data
});
```

### Sending Events

```javascript
// Page view event
alloy("sendEvent", {
  xdm: {
    eventType: "web.webpagedetails.pageViews",
    web: {
      webPageDetails: {
        name: "hotel-detail",
        URL: window.location.href,
        pageViews: { value: 1 }
      },
      webReferrer: {
        URL: document.referrer
      }
    },
    identityMap: {
      loyaltyId: [{
        id: "MR-GOLD-789",
        authenticatedState: "authenticated",
        primary: false
      }]
    }
  },
  // Non-XDM data (goes to Analytics via DataStream mapping)
  data: {
    __adobe: {
      analytics: {
        eVar5: "Marriott Marquis Times Square",
        events: "event1"
      }
    }
  }
});

// Commerce / booking event
alloy("sendEvent", {
  xdm: {
    eventType: "commerce.purchases",
    commerce: {
      purchases: { value: 1 },
      order: {
        currencyCode: "USD",
        priceTotal: 450.00,
        purchaseID: "B-98765"
      }
    },
    productListItems: [{
      SKU: "NYCMQ-DELUXE-KING",
      name: "Marriott Marquis Times Square - Deluxe King",
      priceTotal: 450.00,
      quantity: 1
    }]
  }
});
```

### Getting Personalization Decisions from Target

```javascript
// Request Target decisions alongside the page view event
alloy("sendEvent", {
  renderDecisions: true,   // Tell Web SDK to automatically apply Target DOM changes
  xdm: {
    eventType: "web.webpagedetails.pageViews",
    web: { webPageDetails: { name: "home" } }
  }
}).then(({ decisions }) => {
  // Decisions returned but not auto-rendered (if renderDecisions: false)
  decisions.forEach(decision => {
    console.log("Scope:", decision.scope);         // "hero-banner"
    console.log("Content:", decision.items[0].data.content);  // HTML/JSON
  });
});
```

---

## 6. Web SDK in Adobe Tags (No-Code Setup)

When using Tags (the recommended approach), you don't write `alloy()` calls manually. The **Adobe Experience Platform Web SDK extension** in Tags handles it:

```
Tags Property: "Marriott.com - Production"
Extensions:
  └── Adobe Experience Platform Web SDK
        DataStream: Marriott Production DataStream
        Identity namespace: ECID (auto-managed)

Rules configured:
  Rule: "Page View"
    Event: Adobe Client Data Layer - Data Pushed - "page:loaded"
    Action: Adobe Experience Platform Web SDK - Send event
              Type: web.webpagedetails.pageViews
              XDM object: (mapped from data elements)
              
  Rule: "Booking Confirmed"
    Event: Adobe Client Data Layer - Data Pushed - "booking:confirmed"
    Action: Adobe Experience Platform Web SDK - Send event
              Type: commerce.purchases
              XDM object: (commerce fields mapped from data layer)
```

The Web SDK extension auto-manages:
- ECID generation and persistence
- Identity stitching across sessions
- Anti-flicker snippet for Target
- Consent state management

---

## 7. XDM Field Mapping for Analytics

The DataStream automatically maps XDM fields to Analytics variables. You can also pass legacy variables in the `data.__adobe.analytics` object:

```
XDM field                              → Analytics variable (auto-mapped)
──────────────────────────────────────────────────────────────────────────
web.webPageDetails.name                → pageName
web.webPageDetails.pageViews           → Page Views metric
commerce.purchases                     → Purchase event (event1)
commerce.order.priceTotal              → Revenue metric
identityMap.ECID                       → Visitor ID

Manual mapping in DataStream rules:
data.__adobe.analytics.eVar5           → eVar5
data.__adobe.analytics.events          → events
```

---

## 8. ECID — The Universal Visitor Identity

ECID (Experience Cloud ID) is the shared visitor identifier across all Adobe products.

```
First time a visitor lands on marriott.com:

1. alloy.js loads
2. Web SDK generates a new ECID: 12345678901234567890 (30-digit number)
3. Stores it in first-party cookie: s_ecid=MCMID|12345678901234567890
   Domain: .marriott.com (first-party — survives ITP/browser restrictions)
4. Sends ECID in every event to Adobe Edge
5. Analytics, Target, AEP all receive same ECID → same visitor across all products

Second visit (same browser):
1. alloy.js loads
2. Web SDK reads s_ecid cookie → same ECID: 12345678901234567890
3. Same visitor recognized across all Adobe products

User logs in:
1. alloy.js sendEvent includes loyaltyId in identityMap
2. AEP identity graph links: ECID ↔ loyaltyId
3. This person's profile is now unified across anonymous + authenticated visits
```

**Cookie storage:**

| Cookie | Domain | Purpose | Expiry |
|---|---|---|---|
| `s_ecid` | `.marriott.com` (1st party) | Stores ECID for Web SDK | 2 years |
| `AMCV_...` | `.marriott.com` (1st party) | Legacy VisitorAPI cookie | 2 years |
| `demdex` | `.demdex.net` (3rd party) | Cross-domain ID sync (legacy) | 180 days |

Third-party cookies (demdex.net) are blocked by Safari (ITP), Firefox, and increasingly Chrome. Web SDK uses first-party cookies (`s_ecid`) to avoid this — which is why migrating from legacy to Web SDK improves identity accuracy.

---

## 9. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Loading alloy.js after at.js and AppMeasurement.js | Both legacy and new SDK run simultaneously — double counting Analytics hits | Remove legacy libraries from the page when switching to Web SDK |
| Not configuring DataStream environments | Dev traffic goes to production Analytics report suite | Create separate DataStreams for dev/staging/prod; configure in Tags environments |
| Sending PII in XDM fields | AEP and Analytics store raw PII — GDPR violation | Hash identifiers; never send raw email/phone; use hashed identityMap values |
| Setting `renderDecisions: false` without handling decisions | Target decisions are returned but never applied to the DOM | Either set `renderDecisions: true` or manually apply `decision.items` content |
| Forgetting `idMigrationEnabled: true` during legacy→Web SDK migration | Existing visitors get new ECID → lose their history and segment membership | Keep `idMigrationEnabled: true` during transition period |

---

## 10. Interview Insight

Strong answer:

> Web SDK (alloy.js) replaces three legacy Adobe libraries — AppMeasurement, at.js, and VisitorAPI — with a single library that makes one network call to Adobe's Edge Network. The Edge Network uses a DataStream configuration to route that call to Analytics, Target, and AEP simultaneously on the server side. Events are sent as XDM payloads — the same schema AEP uses natively, so there's no mapping overhead. ECID is stored in a first-party cookie (s_ecid) so it survives browser ITP restrictions that kill third-party cookies. The migration from legacy to Web SDK requires keeping `idMigrationEnabled: true` so returning visitors keep their ECID and don't lose profile continuity.

Follow-up trap:

> Why would you still see both `AppMeasurement.js` AND `alloy.js` on the same page?

Good answer:

> This is a migration state — the team is in the process of switching from legacy to Web SDK but hasn't fully cut over yet. Running both simultaneously causes double-counting in Analytics (every page view fires two beacons). The correct approach is to run them in parallel only briefly during validation, then remove the legacy libraries. In Tags, you can use a rule condition to fire only the Web SDK rule OR the legacy rule, not both, during the cutover period.

---

## 11. Revision Notes

- One-line summary: Web SDK (alloy.js) is the single modern Adobe library that replaces at.js + AppMeasurement — one request to Edge Network, DataStream routes to Analytics/Target/AEP, ECID in first-party cookie.
- Three keywords: DataStream, Edge Network, XDM.
- One interview trap: running both Web SDK and legacy AppMeasurement simultaneously doubles Analytics beacon counts.
- Memory trick: Web SDK = one request to Edge; DataStream = the config that tells Edge where to send it.
