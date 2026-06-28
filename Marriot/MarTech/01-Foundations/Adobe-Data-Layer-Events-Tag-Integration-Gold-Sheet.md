# Adobe Data Layer — Events, Schema, Tag Integration — Gold Sheet

> Topic: adobeDataLayer[] — the client-side contract between engineering and marketing

---

## 1. Intuition

The Adobe Data Layer is a JavaScript array living on the browser's `window` object. Engineering pushes structured event objects into it. Adobe Tags listens to those pushes and reacts — sending data to Analytics, triggering Target, etc. It's the contract that lets marketing move fast without requiring engineering changes.

Beginner version:

> The data layer is like a shared whiteboard between engineering (who writes) and marketing (who reads and reacts).

---

## 2. Definition

- **Definition:** The Adobe Client Data Layer (ACDL) is a standardized JavaScript array (`window.adobeDataLayer`) that decouples data production (engineering) from data consumption (marketing tools/tags).
- **Category:** Client-side data architecture.
- **Core idea:** Push events with structured context; Tags listens and acts — no tight coupling.

---

## 3. How It Works Mechanically

```javascript
// 1. Page initializes the data layer (usually in <head>)
window.adobeDataLayer = window.adobeDataLayer || [];

// 2. ACDL library loads (adobe-client-data-layer npm package)
// It wraps the plain array with an event emitter

// 3. Engineering code pushes events
window.adobeDataLayer.push({
  event: "page:loaded",
  page: {
    name: "hotel-detail",
    category: "accommodation",
    language: "en_US",
    environment: "production"
  },
  user: {
    loginStatus: "authenticated",
    loyaltyTier: "gold"
  }
});

// 4. Adobe Tags listens to the "page:loaded" event
// Rule: IF event == "page:loaded" THEN fire Adobe Analytics page view beacon
```

The ACDL is a **persistent state + event bus**:
- It accumulates state (the full merged object)
- It emits named events for each push
- Tags can read current state at any time OR react to specific events

---

## 4. Data Layer Schema — Real-World Fields

The log you saw on Marriott's site:

```javascript
{
  device_language_preferred: 'en_US',   // navigator.language — browser language setting
  env_is_prod: 'true',                  // hardcoded by dev team to distinguish prod vs staging
  browser_akamai_loc_state: 'tg',       // Akamai geo-IP → Telangana state code
  previous_page: 'www.marriott.com/default.mi', // referrer or previous page tracker
  browser_akamai_loc_country: 'IN',     // Akamai geo-IP → India
}
```

A full production data layer object on a hotel search page:

```javascript
window.adobeDataLayer.push({
  event: "search:results",

  // Page context
  page: {
    name: "hotel-search-results",
    type: "search",
    category: "accommodation",
    locale: "en_US",
    environment: "production",
    version: "2.4.1"
  },

  // Search context
  search: {
    destination: "New York",
    checkIn: "2026-07-15",
    checkOut: "2026-07-18",
    adults: 2,
    children: 0,
    resultsCount: 47,
    filters: ["pool", "free-breakfast"]
  },

  // User context
  user: {
    loginStatus: "authenticated",
    loyaltyId: "HASH_NOT_RAW_PII",   // hashed — never raw PII
    loyaltyTier: "titanium",
    memberSince: "2019"
  },

  // Device/browser context (often filled from Akamai headers)
  device: {
    type: "desktop",
    os: "macOS",
    browser: "Chrome",
    languagePreferred: "en_US"
  },

  // Geo context (from Akamai CDN geo-IP detection)
  geo: {
    country: "IN",
    state: "tg",
    city: "Hyderabad"
  }
});
```

---

## 5. Event Naming Conventions

Adobe recommends a `category:action` pattern:

```javascript
// Page events
{ event: "page:loaded" }
{ event: "page:error" }

// Search events
{ event: "search:started" }
{ event: "search:results" }
{ event: "search:no-results" }

// Product / hotel events
{ event: "hotel:viewed" }
{ event: "hotel:availability-checked" }
{ event: "hotel:rate-selected" }

// Commerce / booking events
{ event: "booking:started" }
{ event: "booking:form-completed" }
{ event: "booking:confirmed" }
{ event: "booking:cancelled" }

// User events
{ event: "user:login" }
{ event: "user:logout" }
{ event: "user:registered" }
```

---

## 6. Adobe Tags Listening to the Data Layer

In Adobe Tags (Launch), you create a **Data Layer event listener**:

```
Rule: "Track hotel view"
  WHEN: Custom Code — adobeDataLayer event == "hotel:viewed"
  IF: (no conditions)
  DO: Send Adobe Analytics beacon
      Set eVar5 = %data layer: hotel.name%
      Set event3 = 1 (hotel view event)
```

In practice, using the ACDL extension in Tags:

```
Extension: Adobe Client Data Layer
Event type: Data Pushed
Specific Event: hotel:viewed
```

This triggers the rule every time a `hotel:viewed` event is pushed, without polling.

---

## 7. Reading from the Data Layer

```javascript
// Get current state at any point
const currentState = window.adobeDataLayer.getState();
console.log(currentState.user.loyaltyTier); // "gold"

// Get state of a specific path
const geoState = window.adobeDataLayer.getState("geo");

// Subscribe to future events
window.adobeDataLayer.addEventListener("hotel:viewed", (event) => {
  console.log("Hotel viewed:", event.detail.data.hotel.name);
});
```

In Adobe Tags, **Data Elements** read from the data layer:

```
Data Element: "Hotel Name"
Type: Data Layer computed state
Path: hotel.name

Data Element: "User Loyalty Tier"
Path: user.loyaltyTier

Data Element: "Page Environment"
Path: page.environment
```

Tags rules then reference these data elements using `%Hotel Name%` syntax.

---

## 8. Web SDK (alloy.js) vs Legacy (AppMeasurement + at.js)

| | Legacy stack | Modern Web SDK |
|---|---|---|
| Library | AppMeasurement.js (Analytics) + at.js (Target) | alloy.js (single library) |
| Data layer | adobeDataLayer + DTM/Launch | adobeDataLayer + Launch |
| Network | Separate beacons for each product | Single XHR to Adobe Edge Network |
| Data format | Proprietary Analytics vars | XDM (Experience Data Model — JSON Schema) |
| AEP integration | Complex connector required | Native — data goes directly to AEP |

**Migration direction:** Companies are migrating from Legacy to Web SDK for better performance (one request vs many) and AEP integration.

---

## 9. Data Layer Anti-Patterns

```javascript
// ❌ WRONG: push raw PII into data layer
window.adobeDataLayer.push({
  user: {
    email: "john.doe@example.com",   // never do this
    creditCard: "4111111111111111"   // never do this
  }
});

// ✅ RIGHT: push hashed IDs and non-PII attributes
window.adobeDataLayer.push({
  user: {
    hashedEmail: sha256("john.doe@example.com"),
    loyaltyTier: "gold",
    loginStatus: "authenticated"
  }
});
```

```javascript
// ❌ WRONG: push on every scroll pixel
document.addEventListener('scroll', () => {
  window.adobeDataLayer.push({ event: "scroll:progress", px: window.scrollY });
});
// Results in thousands of events per session

// ✅ RIGHT: push at meaningful milestones only
const milestones = [25, 50, 75, 90];
milestones.forEach(pct => {
  // push only when user crosses each threshold
});
```

---

## 10. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Pushing PII (email, name, CC) into data layer | GDPR/CCPA violation; data visible in browser console | Hash sensitive fields; push only attributes |
| No event naming convention | Tags rules become inconsistent across teams | Define schema doc: `category:action` pattern, enforce with TypeScript types |
| Pushing data layer before ACDL library loads | Events dropped — library not listening yet | Initialize `window.adobeDataLayer = []` before library; ACDL replays queued events |
| Over-pushing events (every keypress, scroll px) | Analytics hit limits exceeded, cost spikes | Push on meaningful user intent milestones only |
| Not versioning the data layer schema | Schema changes break Tags rules silently | Version the schema (v1, v2) and coordinate data layer + Tags releases |

---

## 11. Interview Insight

Strong answer:

> The Adobe Data Layer is a client-side JavaScript contract. Engineering pushes structured events — page loads, hotel views, booking completions — into `adobeDataLayer`. Adobe Tags listens to those push events using the ACDL extension and fires rules in response — sending Analytics beacons, triggering Target experiences, or feeding the AEP Edge Network. The key value is decoupling: once engineering adds data layer events, marketing can add or change tracking without engineering deploys.

Follow-up trap:

> What happens to data layer events pushed before Adobe Tags loads?

Good answer:

> The ACDL is initialized as a plain array first — `window.adobeDataLayer = window.adobeDataLayer || []`. Events pushed before the ACDL library loads are queued in that array. When the library loads, it processes the queue — no events are lost. This is the "replay" behavior built into the ACDL spec.

---

## 12. Revision Notes

- One-line summary: The data layer is the shared event bus between engineering (pushes events) and marketing tools (listens and acts).
- Three keywords: push, listen, decouple.
- One interview trap: Data pushed before library loads is queued and replayed — events are not lost.
- Memory trick: Data layer is the API contract; Tags is the API consumer; Analytics/Target are the backends.
